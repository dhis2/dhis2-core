/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.system.grid;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.feedback.ErrorCode.E7230;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.PerformanceMetrics;
import org.hisp.dhis.common.Reference;
import org.hisp.dhis.common.adapter.JacksonRowDataSerializer;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Iterables;

/**
 * @author Lars Helge Overland
 */
public class ListGrid
    implements Grid, Serializable
{
    private static final String REGRESSION_SUFFIX = "_regression";

    private static final String CUMULATIVE_SUFFIX = "_cumulative";

    /**
     * The title of the grid.
     */
    private String title;

    /**
     * The subtitle of the grid.
     */
    private String subtitle;

    /**
     * The name of a potential corresponding table.
     */
    private String table;

    /**
     * A List which represents the column headers of the grid.
     */
    private List<GridHeader> headers;

    /**
     * A Map which can hold arbitrary meta-data.
     */
    private Map<String, Object> metaData;

    /**
     * An Object which can hold execution plans and related data.
     */
    private PerformanceMetrics performanceMetrics;

    /**
     * A Map which can hold internal arbitrary meta data. Will not be
     * serialized.
     */
    private Map<String, Object> internalMetaData;

    /**
     * A two dimensional List which simulates a grid where the first list
     * represents all rows and the second represents a single row with columns.
     */
    private List<List<Object>> grid;

    /**
     * References.
     */
    private List<Reference> refs;

    /**
     * Indicating the current row in the grid for writing data.
     */
    private int currentRowWriteIndex = -1;

    /**
     * Indicating the current row in the grid for reading data.
     */
    private int currentRowReadIndex = -1;

    /**
     * Represents a mapping between column names and the index of the column in
     * the grid.
     */
    private Map<String, Integer> columnIndexMap = new HashMap<>();

    private boolean lastDataRow;

    /**
     * Default constructor.
     */
    public ListGrid()
    {
        this.headers = new ArrayList<>();
        this.metaData = new HashMap<>();
        this.internalMetaData = new HashMap<>();
        this.grid = new ArrayList<>();
    }

    /**
     * @param metaData meta data.
     * @param internalMetaData internal meta data.
     */
    public ListGrid( Map<String, Object> metaData, Map<String, Object> internalMetaData )
    {
        this.headers = new ArrayList<>();
        this.metaData = metaData;
        this.internalMetaData = internalMetaData;
        this.grid = new ArrayList<>();
    }

    // ---------------------------------------------------------------------
    // Public methods
    // ---------------------------------------------------------------------

    @Override
    @JsonProperty
    public String getTitle()
    {
        return title;
    }

    @Override
    public Grid setTitle( String title )
    {
        this.title = title;

        return this;
    }

    @Override
    @JsonProperty
    public String getSubtitle()
    {
        return subtitle;
    }

    @Override
    public Grid setSubtitle( String subtitle )
    {
        this.subtitle = subtitle;

        return this;
    }

    @Override
    @JsonProperty
    public String getTable()
    {
        return table;
    }

    @Override
    public Grid setTable( String table )
    {
        this.table = table;

        return this;
    }

    @Override
    public Grid addHeader( GridHeader header )
    {
        headers.add( header );

        updateColumnIndexMap();

        return this;
    }

    @Override
    public Grid addHeader( int headerIndex, GridHeader header )
    {
        headers.add( headerIndex, header );

        updateColumnIndexMap();

        return this;
    }

    @Override
    public Grid addHeaders( int headerIndex, List<GridHeader> gridHeaders )
    {
        if ( gridHeaders == null || gridHeaders.isEmpty() )
        {
            return this;
        }

        for ( int i = gridHeaders.size() - 1; i >= 0; i-- )
        {
            headers.add( headerIndex, gridHeaders.get( i ) );
        }

        updateColumnIndexMap();

        return this;
    }

    @Override
    public Grid addEmptyHeaders( int number )
    {
        for ( int i = 0; i < number; i++ )
        {
            headers.add( new GridHeader( "", false, false ) );
        }

        updateColumnIndexMap();

        return this;
    }

    @Override
    public Grid replaceHeaders( List<GridHeader> gridHeaders )
    {
        if ( gridHeaders == null || gridHeaders.isEmpty() )
        {
            return this;
        }

        headers.clear();
        headers.addAll( gridHeaders );

        updateColumnIndexMap();

        return this;
    }

    @Override
    @JsonProperty
    public List<GridHeader> getHeaders()
    {
        return headers;
    }

    @Override
    public List<GridHeader> getVisibleHeaders()
    {
        return headers.stream()
            .filter( h -> !h.isHidden() )
            .collect( Collectors.toList() );
    }

    @Override
    public List<GridHeader> getMetadataHeaders()
    {
        return headers.stream()
            .filter( GridHeader::isMeta )
            .collect( Collectors.toList() );
    }

    @Override
    public int getIndexOfHeader( String name )
    {
        return headers.indexOf( new GridHeader( name ) );
    }

    @Override
    public boolean headerExists( String name )
    {
        return getIndexOfHeader( name ) != -1;
    }

    @Override
    @JsonProperty
    public int getHeight()
    {
        return grid != null && grid.size() > 0 ? grid.size() : 0;
    }

    @Override
    @JsonProperty
    public int getWidth()
    {
        verifyGridState();

        return grid != null && grid.size() > 0 ? grid.get( 0 ).size() : 0;
    }

    @Override
    @JsonProperty
    public int getHeaderWidth()
    {
        return headers.size();
    }

    @Override
    @JsonProperty
    public Map<String, Object> getMetaData()
    {
        return metaData;
    }

    @Override
    public Grid setMetaData( Map<String, Object> metaData )
    {
        this.metaData = metaData;
        return this;
    }

    @Override
    public Grid addMetaData( String key, Object value )
    {
        this.metaData.put( key, value );
        return this;
    }

    @Override
    @JsonIgnore
    public Map<String, Object> getInternalMetaData()
    {
        return internalMetaData;
    }

    @Override
    public Grid setInternalMetaData( Map<String, Object> internalMetaData )
    {
        this.internalMetaData = internalMetaData;
        return this;
    }

    @Override
    @JsonProperty
    public PerformanceMetrics getPerformanceMetrics()
    {
        return performanceMetrics;
    }

    @Override
    public int getVisibleWidth()
    {
        verifyGridState();

        return grid != null && grid.size() > 0 ? getVisibleRows().get( 0 ).size() : 0;
    }

    @Override
    public Grid addRow()
    {
        grid.add( new ArrayList<>() );

        currentRowWriteIndex++;

        return this;
    }

    @Override
    public Grid addRows( Grid grid )
    {
        List<List<Object>> rows = grid.getRows();

        for ( List<Object> row : rows )
        {
            this.grid.add( row );

            currentRowWriteIndex++;
        }

        return this;
    }

    @Override
    public Grid addValue( Object value )
    {
        grid.get( currentRowWriteIndex ).add( value );

        return this;
    }

    @Override
    public Grid addValues( Object[] values )
    {
        List<Object> row = grid.get( currentRowWriteIndex );

        for ( Object value : values )
        {
            row.add( value );
        }

        return this;
    }

    @Override
    public Grid addValuesVar( Object... values )
    {
        return addValues( values );
    }

    @Override
    public Grid addValuesAsList( List<Object> values )
    {
        return addValues( values.toArray() );
    }

    @Override
    public Grid addEmptyValue()
    {
        addValue( StringUtils.EMPTY );

        return this;
    }

    @Override
    public Grid addEmptyValues( int number )
    {
        for ( int i = 0; i < number; i++ )
        {
            addEmptyValue();
        }

        return this;
    }

    @Override
    public Grid addNullValues( int number )
    {
        for ( int i = 0; i < number; i++ )
        {
            addValue( null );
        }

        return this;
    }

    @Override
    public List<Object> getRow( int rowIndex )
    {
        return grid.get( rowIndex );
    }

    @Override
    @JsonProperty
    @JsonSerialize( using = JacksonRowDataSerializer.class )
    public List<List<Object>> getRows()
    {
        return grid;
    }

    @Override
    @JsonProperty
    public List<Reference> getRefs()
    {
        return refs;
    }

    @Override
    public List<List<Object>> getVisibleRows()
    {
        verifyGridState();

        List<List<Object>> tempGrid = new ArrayList<>();

        if ( headers != null && headers.size() > 0 )
        {
            for ( List<Object> row : grid )
            {
                List<Object> tempRow = new ArrayList<>();

                for ( int i = 0; i < row.size(); i++ )
                {
                    if ( !headers.get( i ).isHidden() )
                    {
                        tempRow.add( row.get( i ) );
                    }
                }

                tempGrid.add( tempRow );
            }
        }

        return tempGrid;
    }

    @Override
    public List<Object> getColumn( int columnIndex )
    {
        List<Object> column = new ArrayList<>();

        for ( List<Object> row : grid )
        {
            column.add( row.get( columnIndex ) );
        }

        return column;
    }

    @Override
    public Object getValue( int rowIndex, int columnIndex )
    {
        if ( grid.size() < rowIndex || grid.get( rowIndex ) == null || grid.get( rowIndex ).size() < columnIndex )
        {
            throw new IllegalArgumentException( "Grid does not contain the requested row / column" );
        }

        return grid.get( rowIndex ).get( columnIndex );
    }

    @Override
    public Grid addColumn( List<Object> columnValues )
    {
        verifyGridState();

        int currentRowIndex = 0;
        int currentColumnIndex = 0;

        if ( grid.size() != columnValues.size() )
        {
            throw new IllegalStateException( "Number of column values (" + columnValues.size()
                + ") is not equal to number of rows (" + grid.size() + ")" );
        }

        for ( int i = 0; i < grid.size(); i++ )
        {
            grid.get( currentRowIndex++ ).add( columnValues.get( currentColumnIndex++ ) );
        }

        return this;
    }

    @Override
    public Grid addColumn( int columnIndex, List<Object> columnValues )
    {
        verifyGridState();

        int currentRowIndex = 0;
        int currentColumnIndex = 0;

        if ( grid.size() != columnValues.size() )
        {
            throw new IllegalStateException( "Number of column values (" + columnValues.size()
                + ") is not equal to number of rows (" + grid.size() + ")" );
        }

        for ( int i = 0; i < grid.size(); i++ )
        {
            grid.get( currentRowIndex++ ).add( columnIndex, columnValues.get( currentColumnIndex++ ) );
        }

        return this;
    }

    @Override
    public Grid addAndPopulateColumnsBefore( int referenceColumnIndex, Map<Object, List<?>> valueMap, int newColumns )
    {
        Validate.inclusiveBetween( 0, getWidth() - 1, referenceColumnIndex );
        Validate.notNull( valueMap );
        verifyGridState();

        for ( List<Object> row : grid )
        {
            Object refVal = row.get( referenceColumnIndex );
            List<?> list = valueMap.get( refVal );

            for ( int i = 0; i < newColumns; i++ )
            {
                Object value = list == null ? null : Iterables.get( list, i, null );
                int index = referenceColumnIndex + i;
                row.add( index, value );
            }
        }

        return this;
    }

    @Override
    public Grid removeEmptyColumns()
    {
        if ( getWidth() == 0 )
        {
            return this;
        }

        int lastCol = getWidth() - 1;

        for ( int i = lastCol; i >= 0; i-- )
        {
            if ( columnIsEmpty( i ) )
            {
                removeColumn( i );
            }
        }

        return this;
    }

    @Override
    public boolean columnIsEmpty( int columnIndex )
    {
        verifyGridState();

        for ( List<Object> row : grid )
        {
            Object val = row.get( columnIndex );

            if ( val != null )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public Grid removeColumn( int columnIndex )
    {
        verifyGridState();

        if ( headers.size() > 0 )
        {
            headers.remove( columnIndex );
        }

        for ( List<Object> row : grid )
        {
            row.remove( columnIndex );
        }

        updateColumnIndexMap();

        return this;
    }

    @Override
    public Grid removeColumn( GridHeader header )
    {
        int index = headers.indexOf( header );

        if ( index != -1 )
        {
            removeColumn( index );
        }

        return this;
    }

    @Override
    public Grid removeCurrentWriteRow()
    {
        grid.remove( currentRowWriteIndex );

        currentRowWriteIndex--;

        return this;
    }

    @Override
    public boolean hasMetaDataKey( String key )
    {
        return metaData != null && metaData.containsKey( key );
    }

    @Override
    public boolean hasInternalMetaDataKey( String key )
    {
        return internalMetaData != null && internalMetaData.containsKey( key );
    }

    @Override
    public Grid limitGrid( int limit )
    {
        if ( limit < 0 )
        {
            throw new IllegalStateException( "Illegal limit: " + limit );
        }

        if ( limit > 0 && limit <= getHeight() )
        {
            grid = grid.subList( 0, limit );
        }

        return this;
    }

    @Override
    public Grid limitGrid( int startPos, int endPos )
    {
        if ( startPos < 0 || endPos < startPos || endPos > getHeight() )
        {
            throw new IllegalStateException(
                "Illegal start / end pos: " + startPos + ", " + endPos + ", " + getHeight() );
        }

        grid = grid.subList( startPos, endPos );

        return this;
    }

    @Override
    public Grid sortGrid( int columnIndex, int order )
    {
        if ( order == 0 )
        {
            return this; // No sorting
        }

        columnIndex--;

        if ( columnIndex < 0 || columnIndex >= getWidth() )
        {
            throw new IllegalArgumentException( "Column index out of bounds: " + columnIndex );
        }

        Collections.sort( grid, new GridRowComparator( columnIndex, order ) );

        return this;
    }

    @Override
    public Grid addRegressionColumn( int columnIndex, boolean addHeader )
    {
        verifyGridState();

        SimpleRegression regression = new SimpleRegression();

        List<Object> column = getColumn( columnIndex );

        int index = 0;

        for ( Object value : column )
        {
            // 0 omitted from regression

            if ( value != null && !MathUtils.isEqual( Double.parseDouble( String.valueOf( value ) ), 0d ) )
            {
                regression.addData( index++, Double.parseDouble( String.valueOf( value ) ) );
            }
        }

        List<Object> regressionColumn = new ArrayList<>();

        for ( int i = 0; i < column.size(); i++ )
        {
            final double predicted = regression.predict( i );

            // Enough values must exist for regression

            if ( !Double.isNaN( predicted ) )
            {
                regressionColumn.add( Precision.round( predicted, 1 ) );
            }
            else
            {
                regressionColumn.add( null );
            }
        }

        addColumn( regressionColumn );

        if ( addHeader && columnIndex < headers.size() )
        {
            GridHeader header = headers.get( columnIndex );

            if ( header != null )
            {
                GridHeader regressionHeader = new GridHeader(
                    header.getName() + REGRESSION_SUFFIX,
                    header.getColumn() + REGRESSION_SUFFIX,
                    header.getValueType(),
                    header.isHidden(),
                    header.isMeta() );

                addHeader( regressionHeader );
            }
        }

        return this;
    }

    @Override
    public Grid addRegressionToGrid( int startColumnIndex, int numberOfColumns )
    {
        for ( int i = 0; i < numberOfColumns; i++ )
        {
            int columnIndex = i + startColumnIndex;

            this.addRegressionColumn( columnIndex, true );
        }

        return this;
    }

    @Override
    public Grid addCumulativeColumn( int columnIndex, boolean addHeader )
    {
        verifyGridState();

        List<Object> column = getColumn( columnIndex );

        List<Object> cumulativeColumn = new ArrayList<>();

        double sum = 0d;

        for ( Object value : column )
        {
            double number = value != null ? Double.parseDouble( String.valueOf( value ) ) : 0d;

            sum += number;

            cumulativeColumn.add( sum );
        }

        addColumn( cumulativeColumn );

        if ( addHeader && columnIndex < headers.size() )
        {
            GridHeader header = headers.get( columnIndex );

            if ( header != null )
            {
                GridHeader regressionHeader = new GridHeader(
                    header.getName() + CUMULATIVE_SUFFIX,
                    header.getColumn() + CUMULATIVE_SUFFIX,
                    header.getValueType(),
                    header.isHidden(),
                    header.isMeta() );

                addHeader( regressionHeader );
            }
        }

        return this;
    }

    @Override
    public Grid addCumulativesToGrid( int startColumnIndex, int numberOfColumns )
    {
        for ( int i = 0; i < numberOfColumns; i++ )
        {
            int columnIndex = i + startColumnIndex;

            this.addCumulativeColumn( columnIndex, true );
        }

        return this;
    }

    @Override
    public Grid substituteMetaData( Map<? extends Object, ? extends Object> metaDataMap )
    {
        if ( metaDataMap == null || headers == null || headers.isEmpty() )
        {
            return this;
        }

        for ( int colIndex = 0; colIndex < headers.size(); colIndex++ )
        {
            GridHeader header = headers.get( colIndex );

            // Header

            Object headerMetaName = metaDataMap.get( header.getName() );

            if ( headerMetaName != null )
            {
                header.setName( String.valueOf( headerMetaName ) );
            }

            if ( header.isMeta() )
            {
                // Column cells

                substituteMetaData( colIndex, colIndex, metaDataMap );
            }
        }

        return this;
    }

    @Override
    public Grid substituteMetaData( int sourceColumnIndex, int targetColumnIndex,
        Map<? extends Object, ? extends Object> metaDataMap )
    {
        if ( metaDataMap == null )
        {
            return this;
        }

        List<Object> sourceColumn = getColumn( sourceColumnIndex );

        for ( int rowIndex = 0; rowIndex < sourceColumn.size(); rowIndex++ )
        {
            Object sourceValue = sourceColumn.get( rowIndex );

            Object metaValue = metaDataMap.get( sourceValue );

            if ( metaValue != null )
            {
                grid.get( rowIndex ).set( targetColumnIndex, metaValue );
            }
        }

        return this;
    }

    @Override
    public List<Integer> getMetaColumnIndexes()
    {
        List<Integer> indexes = new ArrayList<>();

        for ( int i = 0; i < headers.size(); i++ )
        {
            GridHeader header = headers.get( i );

            if ( header != null && header.isMeta() )
            {
                indexes.add( i );
            }
        }

        return indexes;
    }

    @Override
    public Set<Object> getUniqueValues( String columnName )
    {
        int columnIndex = getIndexOfHeader( columnName );

        Set<Object> values = new HashSet<>();

        if ( columnIndex != -1 )
        {
            List<Object> column = getColumn( columnIndex );
            values.addAll( column );
        }

        return values;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T> Map<String, T> getAsMap( int valueIndex, String keySeparator )
    {
        Map<String, T> map = new HashMap<>();

        for ( List<Object> row : grid )
        {
            List<Object> metaDataRow = new ArrayList<>( row );

            metaDataRow.remove( valueIndex );

            String key = StringUtils.join( metaDataRow, keySeparator );

            T value = (T) row.get( valueIndex );

            map.put( key, value );
        }

        return map;
    }

    // -------------------------------------------------------------------------
    // JRDataSource implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean next()
        throws JRException
    {
        boolean next = ++currentRowReadIndex < getHeight();

        if ( !next )
        {
            currentRowReadIndex = -1; // Reset and return false
        }

        return next;
    }

    @Override
    public Object getFieldValue( JRField field )
        throws JRException
    {
        Integer index = columnIndexMap.get( field.getName() );

        return index != null ? getRow( currentRowReadIndex ).get( index ) : null;
    }

    // -------------------------------------------------------------------------
    // SQL utility methods
    // -------------------------------------------------------------------------

    @Override
    public Grid addHeaders( ResultSet rs )
    {
        try
        {
            ResultSetMetaData rsmd = rs.getMetaData();

            int columnNo = rsmd.getColumnCount();

            for ( int i = 1; i <= columnNo; i++ )
            {
                addHeader( new GridHeader( rsmd.getColumnLabel( i ), false, false ) );
            }
        }
        catch ( SQLException ex )
        {
            throw new RuntimeException( ex );
        }

        return this;
    }

    @Override
    public Grid addHeaders( SqlRowSet rs )
    {
        SqlRowSetMetaData rsmd = rs.getMetaData();

        int columnNo = rsmd.getColumnCount();

        for ( int i = 1; i <= columnNo; i++ )
        {
            addHeader( new GridHeader( rsmd.getColumnLabel( i ), false, false ) );
        }

        return this;
    }

    @Override
    public Grid addRows( ResultSet rs )
    {
        try
        {
            int cols = rs.getMetaData().getColumnCount();

            while ( rs.next() )
            {
                addRow();

                for ( int i = 1; i <= cols; i++ )
                {
                    addValue( rs.getObject( i ) );
                }
            }
        }
        catch ( SQLException ex )
        {
            throw new RuntimeException( ex );
        }

        return this;
    }

    @Override
    public Grid addRows( SqlRowSet rs, int maxLimit )
    {
        int cols = rs.getMetaData().getColumnCount();

        while ( rs.next() )
        {
            addRow();

            for ( int i = 1; i <= cols; i++ )
            {
                addValue( rs.getObject( i ) );

                if ( maxLimit > 0 && i > maxLimit )
                {
                    throw new IllegalStateException(
                        "Number of rows produced by query is larger than the max limit: " + maxLimit );
                }
            }
        }

        return this;
    }

    @Override
    public Grid addPerformanceMetrics( List<ExecutionPlan> plans )
    {
        if ( plans.isEmpty() )
        {
            return this;
        }

        double total = plans.stream()
            .map( ExecutionPlan::getTimeInMillis )
            .reduce( 0.0, Double::sum );

        performanceMetrics = new PerformanceMetrics();
        performanceMetrics.setTotalTimeInMillis( Precision.round( total, 3 ) );
        performanceMetrics.setExecutionPlans( plans );

        return this;
    }

    @Override
    public Grid addReference( Reference reference )
    {
        if ( refs == null )
        {
            refs = new ArrayList<>();
        }

        refs.add( reference );

        return this;
    }

    @Override
    public Grid addRows( SqlRowSet rs )
    {
        return addRows( rs, -1 );
    }

    @Override
    public void retainColumns( Set<String> headers )
    {
        final List<String> exclusions = getHeaders().stream().map( GridHeader::getName ).collect( toList() );
        exclusions.removeAll( headers );

        for ( final String headerToExclude : exclusions )
        {
            final int headerIndex = getIndexOfHeader( headerToExclude );
            final boolean hasHeader = headerIndex != -1;

            if ( hasHeader )
            {
                removeColumn( getHeaders().get( headerIndex ) );
            }
        }
    }

    @Override
    public List<Integer> repositionHeaders( List<String> headers )
    {
        verifyGridState();

        final List<String> headerNames = mapToList( getHeaders(), GridHeader::getName );
        final List<GridHeader> orderedHeaders = new ArrayList<>();
        final List<Integer> columnIndexes = new ArrayList<>();

        for ( String header : headers )
        {
            if ( headerNames.contains( header ) )
            {
                int headerIndex = getIndexOfHeader( header );
                orderedHeaders.add( getHeaders().get( headerIndex ) );
                columnIndexes.add( headerIndex );
            }
            else
            {
                throw new IllegalQueryException( new ErrorMessage( E7230, header ) );
            }
        }

        replaceHeaders( orderedHeaders );

        return columnIndexes;
    }

    @Override
    public void repositionColumns( List<Integer> columnIndexes )
    {
        verifyGridState();

        List<List<Object>> rows = getRows();

        for ( List<Object> row : rows )
        {
            List<Object> orderedValues = new ArrayList<>();

            for ( int i = 0; i < row.size(); i++ )
            {
                orderedValues.add( row.get( columnIndexes.get( i ) ) );
            }

            row.clear();
            row.addAll( orderedValues );
        }
    }

    @Override
    public boolean hasLastDataRow()
    {
        return lastDataRow;
    }

    @Override
    public void setLastDataRow( boolean lastDataRow )
    {
        this.lastDataRow = lastDataRow;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Verifies that all grid rows are of the same length.
     */
    private void verifyGridState()
    {
        Integer rowLength = null;

        int rowPos = 0;

        for ( List<Object> row : grid )
        {
            if ( rowLength != null && rowLength != row.size() )
            {
                throw new IllegalStateException( "Grid rows do not have the same number of cells, previous: "
                    + rowLength + ", this: " + row.size() + ", at row: " + rowPos );
            }

            rowPos++;
            rowLength = row.size();
        }
    }

    /**
     * Updates the mapping between header columns and grid indexes. This method
     * should be invoked whenever the columns are manipulated.
     */
    private void updateColumnIndexMap()
    {
        columnIndexMap.clear();

        for ( int i = 0; i < headers.size(); i++ )
        {
            columnIndexMap.put( headers.get( i ).getColumn(), i );
        }
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder( "[\n" );

        if ( headers != null && headers.size() > 0 )
        {
            List<String> headerNames = new ArrayList<>();

            for ( GridHeader header : headers )
            {
                headerNames.add( header.getName() );
            }

            builder.append( headerNames ).append( "\n" );
        }

        for ( List<Object> row : grid )
        {
            builder.append( row ).append( "\n" );
        }

        return builder.append( "]" ).toString();
    }

    // -------------------------------------------------------------------------
    // Comparator
    // -------------------------------------------------------------------------

    public static class GridRowComparator
        implements Comparator<List<Object>>
    {
        private int columnIndex;

        private int order;

        protected GridRowComparator( int columnIndex, int order )
        {
            this.columnIndex = columnIndex;
            this.order = order;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public int compare( List<Object> list1, List<Object> list2 )
        {
            boolean list1Invalid = list1 == null || list1.get( columnIndex ) == null
                || !(list1.get( columnIndex ) instanceof Comparable<?>);
            boolean list2Invalid = list2 == null || list2.get( columnIndex ) == null
                || !(list2.get( columnIndex ) instanceof Comparable<?>);

            if ( list1Invalid && list2Invalid )
            {
                return 0;
            }
            else if ( list1Invalid )
            {
                return order > 0 ? 1 : -1;
            }
            else if ( list2Invalid )
            {
                return order > 0 ? -1 : 1;
            }

            final Comparable<Object> value1 = (Comparable<Object>) list1.get( columnIndex );
            final Comparable<Object> value2 = (Comparable<Object>) list2.get( columnIndex );

            return order > 0 ? value2.compareTo( value1 ) : value1.compareTo( value2 );
        }
    }
}
