package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import net.sf.jasperreports.engine.JRDataSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a two-dimensional grid of Object-typed values organized in rows
 * and columns.
 *
 * @author Lars Helge Overland
 */
public interface Grid
    extends JRDataSource
{
    /**
     * Returns the grid title.
     */
    String getTitle();

    /**
     * Sets the grid title.
     */
    Grid setTitle( String title );

    /**
     * Returns the grid subtitle.
     */
    String getSubtitle();

    /**
     * Sets the grid subtitle.
     */
    Grid setSubtitle( String subtitle );

    /**
     * Returns the grid table.
     */
    String getTable();

    /**
     * Sets the grid table.
     */
    Grid setTable( String table );

    /**
     * Returns all header values.
     */
    List<GridHeader> getHeaders();

    /**
     * Returns map of meta-data.
     */
    Map<String, Object> getMetaData();

    /**
     * Sets map of meta-data.
     */
    Grid setMetaData( Map<String, Object> metaData );

    /**
     * Adds a key-value pair to meta-data.
     */
    Grid addMetaData( String key, Object value );

    /**
     * Returns a map of internal meta-data.
     */
    Map<String, Object> getInternalMetaData();

    /**
     * Sets a map of internal meta-data.
     */
    Grid setInternalMetaData( Map<String, Object> internalMetaData );
    
    /**
     * Returns all visible headers, ie. headers which are not hidden.
     */
    List<GridHeader> getVisibleHeaders();

    /**
     * Returns the index of the header with the given name.
     *
     * @param name the name of the grid header.
     */
    int getIndexOfHeader( String name );

    /**
     * Adds a header.
     *
     * @param header the grid header.
     */
    Grid addHeader( GridHeader header );
    
    /**
     * Adds a header value at the given column index.
     *
     * @param headerIndex the index to insert the grid header at.
     * @param header the grid header.
     */
    Grid addHeader( int headerIndex, GridHeader header );

    /**
     * Adds a list of headers.
     * 
     * @param headerIndex the index to insert the first grid header at.
     * @param headers list of headers.
     */
    Grid addHeaders( int headerIndex, List<GridHeader> headers );

    /**
     * Adds a number of empty values to the Grid.
     *
     * @param number the number of columns to add.
     */
    Grid addEmptyHeaders( int number );
    
    /**
     * Returns the current height / number of rows in the grid.
     */
    int getHeight();

    /**
     * Returns the current width / number of columns in the grid.
     */
    int getWidth();

    /**
     * Returns the current width / number of visible columns in the grid.
     */
    int getVisibleWidth();

    /**
     * Adds a new row the the grid and moves the cursor accordingly.
     */
    Grid addRow();

    /**
     * Adds all rows of the given grid to this grid.
     *
     * @param grid the grid to add to this grid.
     */
    Grid addRows( Grid grid );

    /**
     * Adds the value to the end of the current row.
     *
     * @param value the value to add.
     */
    Grid addValue( Object value );

    /**
     * Adds values in the given array to the end of the current row in the
     * specified order.
     *
     * @param values the values to add.
     */
    Grid addValues( Object[] values );

    /**
     * Adds the given values to the end of the current row in the specified 
     * order.
     *
     * @param values the values to add.
     */
    Grid addValuesVar( Object... values );

    /**
     * Adds values in the given array to the end of the current row in the
     * specified order.
     *
     * @param values the values to add.
     */
    Grid addValuesAsList( List<Object> values );

    /**
     * Adds an empty value to the Grid at the current row.
     */
    Grid addEmptyValue();

    /**
     * Adds a number of empty values to the Grid at the current row.
     *
     * @param number the number of empty values to add.
     */
    Grid addEmptyValues( int number );

    /**
     * Adds a number of null values to the Grid at the current row.
     *
     * @param number the number of null values to add.
     */
    Grid addNullValues( int number );

    /**
     * Returns the row with the given index.
     *
     * @param rowIndex the index of the row.
     */
    List<Object> getRow( int rowIndex );

    /**
     * Returns all rows.
     */
    List<List<Object>> getRows();

    /**
     * Returns all visible rows, ie. rows with a corresponding header that is
     * not hidden.
     */
    List<List<Object>> getVisibleRows();

    /**
     * Returns the column with the given index.
     *
     * @param columnIndex the index of the column.
     */
    List<Object> getColumn( int columnIndex );

    /**
     * Return the value at the given row index and the given column index.
     *
     * @param rowIndex    the row index.
     * @param columnIndex the column index.
     * @return the column value.
     * @throws IllegalArgumentException if the grid does not contain the requested row / column.
     */
    Object getValue( int rowIndex, int columnIndex );

    /**
     * Adds a new column at the end of the grid.
     *
     * @param columnValues the column values to add.
     * @throws IllegalStateException if the columnValues has different length
     *                               than the rows in grid, or if the grid rows are not of the same length.
     */
    Grid addColumn( List<Object> columnValues );

    /**
     * Adds a new column at the end of the grid.
     *
     * @param columnIndex  the index at where to insert the column.
     * @param columnValues the column values to add.
     * @throws IllegalStateException if the columnValues has different length
     *                               than the rows in grid, or if the grid rows are not of the same length.
     */
    Grid addColumn( int columnIndex, List<Object> columnValues );

    /**
     * Adds and populates the given number of columns before the given reference
     * column index. The given value map is to populate each row, where they key
     * is matched against the values in the reference column, and the list of values
     * is used to populate the row values left to right. Where there is no match,
     * null values are inserted.
     * 
     * @param referenceColumnIndex the reference column index.
     * @param valueMap the map of values to list of values.
     * @param newColumns the number of new columns to add.
     */
    Grid addAndPopulateColumnsBefore( int referenceColumnIndex, Map<Object, List<?>> valueMap, int newColumns );

    /**
     * Removes the header and column at the given index.
     *
     * @param columnIndex the column index.
     */
    Grid removeColumn( int columnIndex );
    
    /**
     * Removes from the grid columns with corresponding headers which only contain 
     * null values.
     */
    Grid removeEmptyColumns();
    
    /**
     * Indicates whether the column with the given index only contains null values.
     * 
     * @param columnIndex the column index.
     */
    boolean columnIsEmpty( int columnIndex );

    /**
     * Removes the header and the column at the index of the given header if it
     * exists.
     *
     * @param header the grid header.
     */
    Grid removeColumn( GridHeader header );

    /**
     * Removes the current row from the grid.
     */
    Grid removeCurrentWriteRow();

    /**
     * Indicates whether meta data exists and contains the given key.
     *
     * @param key the meta data key.
     */
    boolean hasMetaDataKey( String key );

    /**
     * Indicates whether the internal meta data exists and contains the given key.
     *
     * @param key the internal meta data key.
     */
    boolean hasInternalMetaDataKey( String key );

    /**
     * Limits the grid from top by the given argument number.
     *
     * @param limit the top limit, must be greater than zero to have an effect.
     */
    Grid limitGrid( int limit );

    /**
     * Limits the grid by the given start and end position.
     *
     * @param startPos the start position.
     * @param endPos   the end position.
     */
    Grid limitGrid( int startPos, int endPos );

    /**
     * Sorts the grid ascending on the column at the given columnIndex.
     *
     * @param columnIndex the column index, starting on 1.
     * @param order       a negative value indicates ascending order, a positive value
     *                    indicates descending order, zero value indicates no sorting.
     */
    Grid sortGrid( int columnIndex, int order );

    /**
     * Adds a regression column to the grid. Column must hold numeric data.
     *
     * @param columnIndex the index of the base column.
     * @param addHeader   indicates whether to add a grid header for the regression column.
     */
    Grid addRegressionColumn( int columnIndex, boolean addHeader );

    /**
     * Adds columns with regression values to the given grid.
     *
     * @param startColumnIndex the index of the first data column.
     * @param numberOfColumns  the number of data columns.
     */
    Grid addRegressionToGrid( int startColumnIndex, int numberOfColumns );

    /**
     * Adds a cumulative column to the grid. Column must hold numeric data.
     *
     * @param columnIndex the index of the base column.
     * @param addHeader   indicates whether to add a grid header for the regression column.
     */
    Grid addCumulativeColumn( int columnIndex, boolean addHeader );

    /**
     * Adds columns with cumulative values to the given grid.
     *
     * @param startColumnIndex the index of the first data column.
     * @param numberOfColumns  the number of data columns.
     */
    Grid addCumulativesToGrid( int startColumnIndex, int numberOfColumns );

    /**
     * Substitutes the grid header names and the grid values for meta type columns
     * based on the given mapping. Values are left unchanged when there is not match
     * in the given meta data map.
     *
     * @param metaDataMap meta data map of keys and value substitutions.
     */
    Grid substituteMetaData( Map<?, ?> metaDataMap );

    /**
     * Substitutes the grid values based on the given mapping. Substitutes values
     * per row based on the given source column index and target column index.
     * Values are left unchanged when there is not match in the given meta data
     * map.
     *
     * @param sourceColumnIndex the index of the column to read values.
     * @param targetColumnIndex the index of the column to substitute values.
     * @param metaDataMap       meta-data map of keys and substitutions.
     */
    Grid substituteMetaData( int sourceColumnIndex, int targetColumnIndex, Map<?, ?> metaDataMap );

    /**
     * Returns indexes of the meta grid headers.
     */
    List<Integer> getMetaColumnIndexes();

    /**
     * Returns the unique set of values from the grid column with the given name.
     * The name refers to the name of the grid header of the column.
     *
     * @param columnName name of the column grid header.
     */
    Set<Object> getUniqueValues( String columnName );

    /**
     * Returns a map of each row in the grid.
     *
     * @param valueIndex   the index of the column to use as map values.
     * @param keySeparator the separator to use to concatenate the map key.
     */
    <T> Map<String, T> getAsMap( int valueIndex, String keySeparator );

    /**
     * Adds a set of headers based on the column names of the given SQL result set.
     *
     * @param rs the result set.
     */
    Grid addHeaders( ResultSet rs );

    /**
     * Adds a set of headers based on the column names of the given SQL row set.
     *
     * @param rs the result set.
     */
    Grid addHeaders( SqlRowSet rs );

    /**
     * Moves the cursor the next row and adds values for each column of the given
     * SQL result set.
     *
     * @param rs the result set.
     */
    Grid addRows( ResultSet rs );

    /**
     * Moves the cursor the next row and adds values for each column of the given
     * SQL row set.
     *
     * @param rs the row set.
     */
    Grid addRows( SqlRowSet rs );
}
