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
package org.hisp.dhis.analytics.util;

import static org.hisp.dhis.common.DataDimensionItem.DATA_DIMENSION_TYPE_CLASS_MAP;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.QUERY_MODS_ID_SEPARATOR;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.system.util.MathUtils.getRounded;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.springframework.util.Assert.isTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DataDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RegexUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.FinancialPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.DateUtils;
import org.joda.time.DateTime;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtils
{
    private static final int DECIMALS_NO_ROUNDING = 10;

    private static final String KEY_AGG_VALUE = "[aggregated]";

    private static final Pattern OU_LEVEL_PATTERN = Pattern.compile( DataQueryParams.PREFIX_ORG_UNIT_LEVEL + "(\\d+)" );

    public static final String ERR_MSG_TABLE_NOT_EXISTING = "Query failed, likely because the requested analytics table does not exist";

    /**
     * Returns an SQL statement for retrieving raw data values for an aggregate
     * query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a SQL statement.
     */
    public static String getDebugDataSql( DataQueryParams params )
    {
        List<DimensionalItemObject> dataElements = new ArrayList<>(
            NameableObjectUtils.getCopyNullSafe( params.getDataElements() ) );
        dataElements.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterDataElements() ) );

        List<DimensionalItemObject> periods = new ArrayList<>(
            NameableObjectUtils.getCopyNullSafe( params.getPeriods() ) );
        periods.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterPeriods() ) );

        List<DimensionalItemObject> orgUnits = new ArrayList<>(
            NameableObjectUtils.getCopyNullSafe( params.getOrganisationUnits() ) );
        orgUnits.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterOrganisationUnits() ) );

        if ( dataElements.isEmpty() || periods.isEmpty() || orgUnits.isEmpty() )
        {
            throw new IllegalQueryException( ErrorCode.E7400 );
        }

        String sql = "select de.name as de_name, de.uid as de_uid, de.dataelementid as de_id, " +
            "pe.startdate as start_date, pe.enddate as end_date, pt.name as pt_name, " +
            "ou.name as ou_name, ou.uid as ou_uid, ou.organisationunitid as ou_id, " +
            "coc.name as coc_name, coc.uid as coc_uid, coc.categoryoptioncomboid as coc_id, " +
            "aoc.name as aoc_name, aoc.uid as aoc_uid, aoc.categoryoptioncomboid as aoc_id, dv.value as datavalue " +
            "from datavalue dv " +
            "inner join dataelement de on dv.dataelementid = de.dataelementid " +
            "inner join period pe on dv.periodid = pe.periodid " +
            "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid " +
            "where dv.dataelementid in (" +
            StringUtils.join( IdentifiableObjectUtils.getIdentifiers( dataElements ), "," ) + ") " +
            "and (";

        for ( DimensionalItemObject period : periods )
        {
            Period pe = (Period) period;
            sql += "(pe.startdate >= '" + getMediumDateString( pe.getStartDate() ) + "' and pe.enddate <= '"
                + getMediumDateString( pe.getEndDate() ) + "') or ";
        }

        sql = TextUtils.removeLastOr( sql ) + ") and (";

        for ( DimensionalItemObject orgUnit : orgUnits )
        {
            OrganisationUnit ou = (OrganisationUnit) orgUnit;
            int level = ou.getLevel();
            sql += "(dv.sourceid in (select organisationunitid from _orgunitstructure where idlevel" + level + " = "
                + ou.getId() + ")) or ";
        }

        sql = TextUtils.removeLastOr( sql ) + ") ";
        sql += "and dv.deleted is false " +
            "limit 100000";

        return sql;
    }

    /**
     * Returns a list of data dimension options which match the given data
     * dimension item type.
     *
     * @param itemType the data dimension item type.
     * @param dataDimensionOptions the data dimension options.
     * @return list of nameable objects.
     */
    public static List<DimensionalItemObject> getByDataDimensionItemType( DataDimensionItemType itemType,
        List<DimensionalItemObject> dataDimensionOptions )
    {
        List<DimensionalItemObject> list = new ArrayList<>();

        for ( DimensionalItemObject object : dataDimensionOptions )
        {
            Class<?> clazz = HibernateProxyUtils.getRealClass( object );

            if ( clazz.equals( DATA_DIMENSION_TYPE_CLASS_MAP.get( itemType ) ) )
            {
                list.add( object );
            }
        }

        return list;
    }

    /**
     * Rounds a value. If the given parameters has skip rounding, the value is
     * rounded to {@link AnalyticsUtils#DECIMALS_NO_ROUNDING}. decimals. If the
     * given number of decimals is specified, the value is rounded to the given
     * decimals. Otherwise, default rounding is used. If 0 decimals is
     * explicitly specified, this method returns a long value. Otherwise, a
     * double value is returned.
     *
     * @param params the query parameters.
     * @param decimals the number of decimals.
     * @param value the value.
     * @return a double.
     */
    public static Number getRoundedValue( DataQueryParams params, Integer decimals, Double value )
    {
        if ( value == null )
        {
            return value;
        }
        else if ( params.isSkipRounding() )
        {
            return Precision.round( value, DECIMALS_NO_ROUNDING );
        }
        else if ( decimals != null && decimals > 0 )
        {
            return Precision.round( value, decimals );
        }
        else if ( decimals != null && decimals == 0 )
        {
            return Math.round( value );
        }
        else
        {
            return getRounded( value );
        }
    }

    /**
     * Rounds a value. If the given parameters has skip rounding, the value is
     * returned unchanged. If the given number is null or not of type Double,
     * the value is returned unchanged. If skip rounding is specified in the
     * given {@link DataQueryParams}, 10 decimals is used. Otherwise, default
     * rounding is used.
     *
     * If the given value is of type Double and ends with one or more decimal
     * "0" (ie.: "125.0", "2355.000", etc.) a long value will be returned,
     * forcing the removal of all decimal "0". The resulting value for the
     * previous example would be, respectively, "125" and "2355".
     *
     * @param params the query parameters.
     * @param value the value.
     * @return a value.
     */
    public static Object getRoundedValueObject( DataQueryParams params, Object value )
    {
        if ( value == null || !Double.class.equals( value.getClass() ) )
        {
            return value;
        }
        else if ( params.isSkipRounding() )
        {
            return Precision.round( (Double) value, DECIMALS_NO_ROUNDING );
        }

        Double rounded = getRounded( (Double) value );

        if ( endsWithZeroAsDecimal( rounded ) )
        {
            return rounded.longValue();
        }

        return rounded;
    }

    /**
     * This method simply checks if the given double value (positive or
     * negative) has one or more "0" as decimal digits. ie.:
     *
     * <pre>
     * {@code
     * 25.0 -> true
     * -232.0000 -> true
     * 133.25 -> false
     * -1045.00000001 -> false
     * }</code>
     *
     * @param value
     * @return true if the value has "0" as decimal digits, false otherwise
     */
    public static boolean endsWithZeroAsDecimal( double value )
    {
        return ((value * 10) % 10 == 0);
    }

    /**
     * Returns the database column type based on the given value type. For
     * boolean values, 1 means true, 0 means false and null means no value.
     *
     * @param valueType the value type to represent as database column type.
     * @param spatialSupport indicates whether spatial data types are enabled.
     * @return the {@link ColumnDataType}.
     */
    public static ColumnDataType getColumnType( ValueType valueType, boolean spatialSupport )
    {
        if ( valueType.isDecimal() )
        {
            return ColumnDataType.DOUBLE;
        }
        else if ( valueType.isInteger() )
        {
            return ColumnDataType.BIGINT;
        }
        else if ( valueType.isBoolean() )
        {
            return ColumnDataType.INTEGER;
        }
        else if ( valueType.isDate() )
        {
            return ColumnDataType.TIMESTAMP;
        }
        else if ( valueType.isGeo() && spatialSupport )
        {
            return ColumnDataType.GEOMETRY_POINT; // TODO consider GEOMETRY
        }
        else
        {
            return ColumnDataType.TEXT;
        }
    }

    /**
     * Converts the data and option combo identifiers to an operand identifier,
     * i.e. {@code deuid-cocuid} to {@code deuid.cocuid}. For
     * {@link TotalType#AOC_ONLY} a {@link ExpressionService#SYMBOL_WILDCARD}
     * symbol will be inserted after the data item.
     * <p>
     * Also transfers a queryModsId to the end of the operand identifier, i.e.
     * {@code deuid_queryModsId-cocuid} to {@code deuid.cocuid_queryModsId}
     *
     * @param valueMap the value map to convert.
     * @param totalType the {@link TotalType}.
     * @return a value map.
     */
    public static <T> Map<String, T> convertDxToOperand( Map<String, T> valueMap, TotalType totalType )
    {
        Map<String, T> map = Maps.newHashMap();

        for ( Entry<String, T> entry : valueMap.entrySet() )
        {
            List<String> items = Lists.newArrayList( entry.getKey().split( DimensionalObject.DIMENSION_SEP ) );
            List<String> operands = Lists.newArrayList( items.subList( 0, totalType.getPropertyCount() + 1 ) );
            List<String> dimensions = Lists
                .newArrayList( items.subList( totalType.getPropertyCount() + 1, items.size() ) );

            // Add wild card in place of category option combination

            if ( TotalType.AOC_ONLY == totalType )
            {
                operands.add( 1, SYMBOL_WILDCARD );
            }

            // If the DataElement has a queryModsId, move it to end of operand

            List<String> queryModsSplit = Lists.newArrayList( operands.get( 0 ).split( QUERY_MODS_ID_SEPARATOR ) );
            if ( queryModsSplit.size() > 1 )
            {
                operands.set( 0, queryModsSplit.get( 0 ) );
                int lastOp = operands.size() - 1;
                operands.set( lastOp, operands.get( lastOp ) + QUERY_MODS_ID_SEPARATOR + queryModsSplit.get( 1 ) );
            }

            String operand = StringUtils.join( operands, DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP );
            String dimension = StringUtils.join( dimensions, DimensionalObject.DIMENSION_SEP );
            dimension = !dimension.isEmpty() ? (DimensionalObject.DIMENSION_SEP + dimension) : StringUtils.EMPTY;
            String key = operand + dimension;

            map.put( key, entry.getValue() );
        }

        return map;
    }

    /**
     * Converts a String, Object map into a specific String, Double map.
     *
     * @param map the map to convert.
     * @return a mapping between string and double values.
     */
    public static Map<String, Double> getDoubleMap( Map<String, Object> map )
    {
        Map<String, Double> typedMap = new HashMap<>();

        for ( Map.Entry<String, Object> entry : map.entrySet() )
        {
            Object value = entry.getValue();

            if ( value != null && Double.class.equals( value.getClass() ) )
            {
                typedMap.put( entry.getKey(), (Double) entry.getValue() );
            }
        }

        return typedMap;
    }

    /**
     * Generates a mapping where the key represents the dimensional item
     * identifiers concatenated by {@link DimensionalObject#DIMENSION_SEP} and
     * the value is the corresponding aggregated data value based on the given
     * grid. Assumes that the value column is the last column in the grid.
     *
     * @param grid the {@link Grid}.
     * @return a mapping between item identifiers and aggregated values.
     */
    public static Map<String, Object> getAggregatedDataValueMapping( Grid grid )
    {
        Map<String, Object> map = new HashMap<>();

        int metaCols = grid.getWidth() - 1;
        int valueIndex = grid.getWidth() - 1;

        for ( List<Object> row : grid.getRows() )
        {
            StringBuilder key = new StringBuilder();

            for ( int index = 0; index < metaCols; index++ )
            {
                key.append( row.get( index ) ).append( DIMENSION_SEP );
            }

            key.deleteCharAt( key.length() - 1 );

            Object value = row.get( valueIndex );

            map.put( key.toString(), value );
        }

        return map;
    }

    /**
     * Generates a data value set based on the given grid with aggregated data.
     * Sets the created and last updated fields to the current date.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the {@link Grid}.
     * @return a {@link DataValueSet}.
     */
    public static DataValueSet getDataValueSet( DataQueryParams params, Grid grid )
    {
        validateGridForDataValueSet( grid );

        int dxInx = grid.getIndexOfHeader( DATA_X_DIM_ID );
        int peInx = grid.getIndexOfHeader( PERIOD_DIM_ID );
        int ouInx = grid.getIndexOfHeader( ORGUNIT_DIM_ID );
        int coInx = grid.getIndexOfHeader( CATEGORYOPTIONCOMBO_DIM_ID );
        int aoInx = grid.getIndexOfHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID );
        int vlInx = grid.getHeaderWidth() - 1;

        Set<String> primaryKeys = new HashSet<>();

        String created = DateUtils.getMediumDateString();

        DataValueSet dvs = new DataValueSet();

        for ( List<Object> row : grid.getRows() )
        {
            DataValue dv = new DataValue();

            Object coc = row.get( coInx );
            Object aoc = row.get( aoInx );

            dv.setDataElement( String.valueOf( row.get( dxInx ) ) );
            dv.setPeriod( String.valueOf( row.get( peInx ) ) );
            dv.setOrgUnit( String.valueOf( row.get( ouInx ) ) );
            dv.setCategoryOptionCombo( coc != null ? String.valueOf( coc ) : null );
            dv.setAttributeOptionCombo( aoc != null ? String.valueOf( aoc ) : null );
            dv.setValue( String.valueOf( row.get( vlInx ) ) );
            dv.setComment( KEY_AGG_VALUE );
            dv.setStoredBy( KEY_AGG_VALUE );
            dv.setCreated( created );
            dv.setLastUpdated( created );

            if ( !params.isDuplicatesOnly() || !primaryKeys.add( dv.getPrimaryKey() ) )
            {
                dvs.getDataValues().add( dv );
            }
        }

        return dvs;
    }

    /**
     * Generates a data value set as a grid based on the given grid with
     * aggregated data. Sets the created and last updated fields to the current
     * date.
     *
     * @param grid the {@link Grid}.
     * @return a data value set.
     */
    public static Grid getDataValueSetAsGrid( Grid grid )
    {
        validateGridForDataValueSet( grid );

        int dxInx = grid.getIndexOfHeader( DATA_X_DIM_ID );
        int peInx = grid.getIndexOfHeader( PERIOD_DIM_ID );
        int ouInx = grid.getIndexOfHeader( ORGUNIT_DIM_ID );
        int coInx = grid.getIndexOfHeader( CATEGORYOPTIONCOMBO_DIM_ID );
        int aoInx = grid.getIndexOfHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID );
        int vlInx = grid.getHeaderWidth() - 1;

        String created = DateUtils.getMediumDateString();

        Grid dvs = new ListGrid();

        dvs.addHeader( new GridHeader( "data_element", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "period", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "organisation_unit", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "category_option_combo", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "attribute_option_combo", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "value", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "stored_by", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "created", ValueType.DATETIME ) );
        dvs.addHeader( new GridHeader( "last_updated", ValueType.DATETIME ) );
        dvs.addHeader( new GridHeader( "comment", ValueType.TEXT ) );
        dvs.addHeader( new GridHeader( "follow_up", ValueType.BOOLEAN ) );

        for ( List<Object> row : grid.getRows() )
        {
            List<Object> objects = new ArrayList<>();

            objects.add( row.get( dxInx ) );
            objects.add( row.get( peInx ) );
            objects.add( row.get( ouInx ) );
            objects.add( row.get( coInx ) );
            objects.add( row.get( aoInx ) );
            objects.add( row.get( vlInx ) );
            objects.add( KEY_AGG_VALUE );
            objects.add( created );
            objects.add( created );
            objects.add( KEY_AGG_VALUE );
            objects.add( false );

            dvs.addRow().addValuesAsList( objects );
        }

        return dvs;
    }

    /**
     * Validates that the required headers for a data value set exist.
     *
     * @param grid the {@link Grid}.
     * @throws IllegalArgumentException if validation fails.
     */
    private static void validateGridForDataValueSet( Grid grid )
    {
        isTrue( grid.headerExists( DATA_X_DIM_ID ), "Data header does not exist" );
        isTrue( grid.headerExists( PERIOD_DIM_ID ), "Period header does not exist" );
        isTrue( grid.headerExists( ORGUNIT_DIM_ID ), "Org unit header does not exist" );
        isTrue( grid.headerExists( CATEGORYOPTIONCOMBO_DIM_ID ), "Category option combo header does not exist" );
        isTrue( grid.headerExists( ATTRIBUTEOPTIONCOMBO_DIM_ID ), "Attribute option combo header does not exist" );
    }

    /**
     * Prepares the given grid to be converted to a data value set.
     *
     * <ul>
     * <li>Converts data values from double to integer based on the associated
     * data item if required.</li>
     * <li>Adds a category option combo and a attribute option combo column to
     * the grid based on the aggregated export properties of the associated data
     * item.</li>
     * <li>For data element operand data items, the operand identifier is split
     * and the data element identifier is used for the data dimension column and
     * the category option combo identifier is used for the category option
     * combo column.</li>
     * </ul>
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    public static void handleGridForDataValueSet( DataQueryParams params, Grid grid )
    {
        Map<String, DimensionalItemObject> dimItemObjectMap = AnalyticsUtils.getDimensionalItemObjectMap( params );

        List<Object> cocCol = Lists.newArrayList();
        List<Object> aocCol = Lists.newArrayList();

        int dxInx = grid.getIndexOfHeader( DATA_X_DIM_ID );
        int vlInx = grid.getHeaderWidth() - 1;

        Assert.isTrue( dxInx >= 0, "Data dimension index must be greater than or equal to zero" );
        Assert.isTrue( vlInx >= 0, "Value index must be greater than or equal to zero" );

        for ( List<Object> row : grid.getRows() )
        {
            String dx = String.valueOf( row.get( dxInx ) );

            Assert.notNull( dx, "Data dimension item cannot be null" );

            DimensionalItemObject item = dimItemObjectMap.get( dx );

            Assert.notNull( item, "Dimensional item cannot be null" );

            Object value = AnalyticsUtils.getIntegerOrValue( row.get( vlInx ), item );

            row.set( vlInx, value );

            String coc = null, aoc = null;

            if ( DataDimensionalItemObject.class.isAssignableFrom( item.getClass() ) )
            {
                DataDimensionalItemObject dataItem = (DataDimensionalItemObject) item;
                coc = dataItem.getAggregateExportCategoryOptionCombo();
                aoc = dataItem.getAggregateExportAttributeOptionCombo();
            }
            else if ( DataElementOperand.class.isAssignableFrom( item.getClass() ) )
            {
                row.set( dxInx, DimensionalObjectUtils.getFirstIdentifer( dx ) );
                coc = DimensionalObjectUtils.getSecondIdentifer( dx );
            }

            cocCol.add( coc );
            aocCol.add( aoc );
        }

        grid.addHeader( vlInx, new GridHeader(
            ATTRIBUTEOPTIONCOMBO_DIM_ID, ATTRIBUTEOPTIONCOMBO_DIM_ID, ValueType.TEXT, false, true ) )
            .addHeader( vlInx, new GridHeader(
                CATEGORYOPTIONCOMBO_DIM_ID, CATEGORYOPTIONCOMBO_DIM_ID, ValueType.TEXT, false, true ) )
            .addColumn( vlInx, aocCol )
            .addColumn( vlInx, cocCol );
    }

    /**
     * Handles conversion of double values to integer. A value is converted to
     * integer if it is a double, and if either the dimensional item object is
     * associated with a data element of value type integer, or associated with
     * an indicator with zero decimals in aggregated output.
     *
     * @param value the value.
     * @param item the dimensional item object.
     * @return an double or integer depending on the given arguments.
     */
    public static Object getIntegerOrValue( Object value, DimensionalItemObject item )
    {
        boolean doubleValue = item != null && value != null && (value instanceof Double);

        if ( doubleValue )
        {
            if ( DimensionItemType.DATA_ELEMENT == item.getDimensionItemType()
                && ((DataElement) item).getValueType().isInteger() )
            {
                value = ((Double) value).intValue();
            }
            else if ( DimensionItemType.DATA_ELEMENT_OPERAND == item.getDimensionItemType()
                && ((DataElementOperand) item).getDataElement().getValueType().isInteger() )
            {
                value = ((Double) value).intValue();
            }
            else if ( DimensionItemType.INDICATOR == item.getDimensionItemType()
                && ((Indicator) item).hasZeroDecimals() )
            {
                value = ((Double) value).intValue();
            }
            else if ( DimensionItemType.PROGRAM_INDICATOR == item.getDimensionItemType()
                && ((ProgramIndicator) item).hasZeroDecimals() )
            {
                value = ((Double) value).intValue();
            }
        }

        return value;
    }

    /**
     * Returns a mapping between dimension item identifiers and dimensional item
     * object for the given query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between identifiers and names.
     */
    public static Map<String, DimensionalItemObject> getDimensionalItemObjectMap( DataQueryParams params )
    {
        List<DimensionalObject> dimensions = params.getDimensionsAndFilters();

        Map<String, DimensionalItemObject> map = new HashMap<>();

        for ( DimensionalObject dimension : dimensions )
        {
            dimension.getItems().stream().forEach( i -> map.put( i.getDimensionItem(), i ) );
        }

        return map;
    }

    /**
     * Returns a mapping between identifiers and names for the given query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between identifiers and names.
     */
    public static Map<String, String> getDimensionItemNameMap( DataQueryParams params )
    {
        List<DimensionalObject> dimensions = params.getDimensionsAndFilters();

        Map<String, String> map = new HashMap<>();

        Calendar calendar = PeriodType.getCalendar();

        for ( DimensionalObject dimension : dimensions )
        {
            for ( DimensionalItemObject item : dimension.getItems() )
            {
                if ( DimensionType.PERIOD.equals( dimension.getDimensionType() ) && !calendar.isIso8601() )
                {
                    Period period = (Period) item;
                    DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );

                    map.put( period.getPeriodType().getIsoDate( dateTimeUnit ), period.getDisplayName() );
                }
                else
                {
                    map.put( item.getDimensionItem(), item.getDisplayProperty( params.getDisplayProperty() ) );
                }

                if ( DimensionType.ORGANISATION_UNIT.equals( dimension.getDimensionType() )
                    && params.isHierarchyMeta() )
                {
                    OrganisationUnit unit = (OrganisationUnit) item;

                    map.putAll( NameableObjectUtils.getUidDisplayPropertyMap( unit.getAncestors(),
                        params.getDisplayProperty() ) );
                }
            }

            map.put( dimension.getDimension(), dimension.getDisplayProperty( params.getDisplayProperty() ) );
        }

        return map;
    }

    /**
     * Returns a mapping between identifiers and meta data items for the given
     * query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between identifiers and meta data items.
     */
    public static Map<String, MetadataItem> getDimensionMetadataItemMap( DataQueryParams params )
    {
        List<DimensionalObject> dimensions = params.getDimensionsAndFilters();

        Map<String, MetadataItem> map = new HashMap<>();

        Calendar calendar = PeriodType.getCalendar();

        boolean includeMetadataDetails = params.isIncludeMetadataDetails();

        for ( DimensionalObject dimension : dimensions )
        {
            for ( DimensionalItemObject item : dimension.getItems() )
            {
                if ( DimensionType.PERIOD == dimension.getDimensionType() && !calendar.isIso8601() )
                {
                    Period period = (Period) item;
                    DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
                    String isoDate = period.getPeriodType().getIsoDate( dateTimeUnit );
                    map.put( isoDate,
                        new MetadataItem( period.getDisplayName(), includeMetadataDetails ? period : null ) );
                }
                else
                {
                    map.put( item.getDimensionItem(),
                        new MetadataItem( item.getDisplayProperty( params.getDisplayProperty() ),
                            includeMetadataDetails ? item : null ) );
                }

                if ( DimensionType.ORGANISATION_UNIT == dimension.getDimensionType() && params.isHierarchyMeta() )
                {
                    OrganisationUnit unit = (OrganisationUnit) item;

                    for ( OrganisationUnit ancestor : unit.getAncestors() )
                    {
                        map.put( ancestor.getUid(),
                            new MetadataItem( ancestor.getDisplayProperty( params.getDisplayProperty() ),
                                includeMetadataDetails ? ancestor : null ) );
                    }
                }

                if ( DimensionItemType.DATA_ELEMENT == item.getDimensionItemType() )
                {
                    DataElement dataElement = (DataElement) item;

                    for ( CategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
                    {
                        map.put( coc.getUid(), new MetadataItem( coc.getDisplayProperty( params.getDisplayProperty() ),
                            includeMetadataDetails ? coc : null ) );
                    }
                }
            }

            map.put( dimension.getDimension(),
                new MetadataItem( dimension.getDisplayProperty( params.getDisplayProperty() ),
                    includeMetadataDetails ? dimension : null ) );

            if ( dimension.getDimensionItemKeywords() != null )
            {
                // if there is includeMetadataDetails flag set to true
                // MetaDataItem is put into the map
                // with all existing information.
                // DimensionItemKeyword can use the same key and overwrite the
                // value with the less information (DimensionItemKeyword can
                // contain only key, uid, code and name ).
                // The key/value should be included only if absent.
                dimension.getDimensionItemKeywords().getKeywords()
                    .forEach( b -> map.putIfAbsent( b.getKey(), b.getMetadataItem() ) );
            }

        }

        Program program = params.getProgram();
        ProgramStage stage = params.getProgramStage();

        if ( ObjectUtils.allNotNull( program ) )
        {
            map.put( program.getUid(), new MetadataItem( program.getDisplayProperty( params.getDisplayProperty() ),
                includeMetadataDetails ? program : null ) );

            if ( stage != null )
            {
                map.put( stage.getUid(),
                    new MetadataItem( stage.getDisplayProperty( params.getDisplayProperty() ),
                        includeMetadataDetails ? stage : null ) );
            }
            else
            {
                for ( ProgramStage ps : program.getProgramStages() )
                {
                    map.put( ps.getUid(), new MetadataItem( ps.getDisplayProperty( params.getDisplayProperty() ),
                        includeMetadataDetails ? ps : null ) );
                }
            }
        }

        return map;
    }

    /**
     * Returns a mapping between the category option combo identifiers and names
     * for the given query.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between identifiers and names.
     */
    public static Map<String, String> getCocNameMap( DataQueryParams params )
    {
        Map<String, String> metaData = new HashMap<>();

        List<DimensionalItemObject> des = params.getAllDataElements();

        for ( DimensionalItemObject de : des )
        {
            DataElement dataElement = (DataElement) de;

            for ( CategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
            {
                metaData.put( coc.getUid(), coc.getName() );
            }
        }

        return metaData;
    }

    /**
     * Returns a mapping between identifiers and display properties for the
     * given list of query items.
     *
     * @param queryItems the list of {@link QueryItem}.
     * @param displayProperty the {@link DisplayProperty} to use.
     * @return a mapping between identifiers and display properties.
     */
    public static Map<String, String> getUidDisplayPropertyMap( List<QueryItem> queryItems,
        DisplayProperty displayProperty )
    {
        Map<String, String> map = new HashMap<>();

        for ( QueryItem item : queryItems )
        {
            map.put( item.getItem().getUid(), item.getItem().getDisplayProperty( displayProperty ) );
        }

        return map;
    }

    /**
     * Returns a mapping between identifiers and display properties for the
     * given list of dimensions.
     *
     * @param dimensions the list of {@link DimensionalObject}.
     * @param hierarchyMeta indicates whether to include meta data about the
     *        organisation unit hierarchy.
     * @return a mapping between identifiers and display properties.
     */
    public static Map<String, String> getUidDisplayPropertyMap( List<DimensionalObject> dimensions,
        boolean hierarchyMeta, DisplayProperty displayProperty )
    {
        Map<String, String> map = new HashMap<>();

        for ( DimensionalObject dimension : dimensions )
        {
            boolean hierarchy = hierarchyMeta && DimensionType.ORGANISATION_UNIT.equals( dimension.getDimensionType() );

            for ( DimensionalItemObject object : dimension.getItems() )
            {
                Set<DimensionalItemObject> objects = Sets.newHashSet( object );

                if ( hierarchy )
                {
                    OrganisationUnit unit = (OrganisationUnit) object;

                    objects.addAll( unit.getAncestors() );
                }

                map.putAll( NameableObjectUtils.getUidDisplayPropertyMap( objects, displayProperty ) );
            }

            map.put( dimension.getDimension(), dimension.getDisplayProperty( displayProperty ) );
        }

        return map;
    }

    /**
     * Returns true if the given period occurs less than maxYears before the
     * current date.
     *
     * @param year the year to check.
     * @param maxYears amount of years back to check
     * @return false if maxYears is 0 or period occurs earlier than maxYears
     *         years since now.
     */
    public static boolean periodIsOutsideApprovalMaxYears( Integer year, Integer maxYears )
    {
        if ( maxYears == 0 )
        {
            return false;
        }

        int currentYear = new DateTime().getYear();

        return (currentYear - year) >= maxYears;
    }

    /**
     * Returns the level from the given org unit level dimension name. Returns
     * -1 if the level could not be determined.
     *
     * @param dimensionName the given org unit level dimension name.
     * @return the org unit level, or -1.
     */
    public static int getLevelFromOrgUnitDimensionName( String dimensionName )
    {
        Set<String> matches = RegexUtils.getMatches( OU_LEVEL_PATTERN, dimensionName, 1 );

        return matches.size() == 1 ? Integer.valueOf( matches.iterator().next() ) : -1;
    }

    /**
     * Indicates whether table layout is specified.
     *
     * @param columns the list of column dimensions.
     * @param rows the list of row dimensions.
     * @return true or false.
     */
    public static boolean isTableLayout( List<String> columns, List<String> rows )
    {
        return (columns != null && !columns.isEmpty()) || (rows != null && !rows.isEmpty());
    }

    /**
     * Calculates the weighted arithmetic mean between two yearly values, based
     * on the given factor as the month.
     *
     * @param year1Value the value for the first year.
     * @param year2Value the value for the second year.
     * @param factor a month value, zero represents January.
     * @return the weighted average of the two values.
     */
    public static Double calculateYearlyWeightedAverage( Double year1Value, Double year2Value, Double factor )
    {
        return Precision.round( (year1Value * ((12 - factor) / 12)) + (year2Value * (factor / 12)),
            DECIMALS_NO_ROUNDING );
    }

    /**
     * Returns the base month of the year for the period type, zero-based.
     *
     * @param periodType the period type.
     * @return the base month.
     */
    public static Double getBaseMonth( PeriodType periodType )
    {
        if ( periodType instanceof FinancialPeriodType )
        {
            return (double) ((FinancialPeriodType) periodType).getBaseMonth();
        }

        return 0D;
    }

    /**
     * Throws an {@link IllegalQueryException} using the given
     * {@link ErrorCode}.
     *
     * @param errorCode the {@link ErrorCode}.
     * @param args the arguments to provide to the error code message.
     */
    public static void throwIllegalQueryEx( ErrorCode errorCode, Object... args )
    {
        throw new IllegalQueryException( new ErrorMessage( errorCode, args ) );
    }

    /**
     * Checks of the given ISO period string matches at least one period in the
     * given list.
     *
     * @param period the ISO period string.
     * @param periods a list of {@link DimensionalItemObject} of type period.
     * @return true if the period exists in the given list.
     */
    public static boolean isPeriodInPeriods( String period, List<DimensionalItemObject> periods )
    {
        return periods.stream().map( d -> (Period) d ).map( Period::getIsoDate )
            .anyMatch( date -> date.equals( period ) );
    }

    /**
     * Filters a list of {@link DimensionalItemObject} and returns one or more
     * {@link DimensionalItemObject} matching the given identifier.
     *
     * @param dimensionIdentifier the identifier to match.
     * @param items the list of {@link DimensionalItemObject} to filter.
     * @return a list containing the {@link DimensionalItemObject} matching the
     *         given identifier.
     */
    public static List<DimensionalItemObject> findDimensionalItems( String dimensionIdentifier,
        List<DimensionalItemObject> items )
    {
        return items.stream()
            .filter( dio -> dio.getDimensionItem() != null &&
                dio.getDimensionItemWithQueryModsId().equals( dimensionIdentifier ) )
            .collect( Collectors.toList() );
    }

    /**
     * Check if the given grid row contains a valid period ISO string.
     *
     * @param row the grid row represented as a list of objects.
     * @param periodIndex the index at which the period is located.
     * @return true if the rows contains a valid period ISO string at the given
     *         index.
     */
    public static boolean hasPeriod( List<Object> row, int periodIndex )
    {
        return periodIndex < row.size() && row.get( periodIndex ) instanceof String
            && PeriodType.getPeriodFromIsoString( (String) row.get( periodIndex ) ) != null;
    }
}
