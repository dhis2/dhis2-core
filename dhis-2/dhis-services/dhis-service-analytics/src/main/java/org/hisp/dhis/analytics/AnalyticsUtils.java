package org.hisp.dhis.analytics;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.Map.Entry;

import static org.hisp.dhis.common.DataDimensionItem.DATA_DIMENSION_TYPE_CLASS_MAP;
import static org.hisp.dhis.common.DimensionalObject.*;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtils
{
    private static final int DECIMALS_NO_ROUNDING = 10;

    private static final String KEY_AGG_VALUE = "[aggregated]";
    
    /**
     * Returns an SQL statement for retrieving raw data values for
     * an aggregate query.
     * 
     * @param params the data query parameters.
     * @return an SQL statement.
     */
    public static String getDebugDataSql( DataQueryParams params )
    {
        List<DimensionalItemObject> dataElements = new ArrayList<>( NameableObjectUtils.getCopyNullSafe( params.getDataElements() ) );
        dataElements.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterDataElements() ) );
        
        List<DimensionalItemObject> periods = new ArrayList<>( NameableObjectUtils.getCopyNullSafe( params.getPeriods() ) );
        periods.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterPeriods() ) );
        
        List<DimensionalItemObject> orgUnits = new ArrayList<>( NameableObjectUtils.getCopyNullSafe( params.getOrganisationUnits() ) );
        orgUnits.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterOrganisationUnits() ) );
        
        if ( dataElements.isEmpty() || periods.isEmpty() || orgUnits.isEmpty() )
        {
            throw new IllegalQueryException( "Query must contain at least one data element, one period and one organisation unit" );
        }
        
        String sql = 
            "select de.name as de_name, de.uid as de_uid, de.dataelementid as de_id, pe.startdate as start_date, pe.enddate as end_date, pt.name as pt_name, " +
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
            "where dv.dataelementid in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( dataElements ), "," ) + ") " +
            "and (";
        
        for ( DimensionalItemObject period : periods )
        {
            Period pe = (Period) period;
            sql += "(pe.startdate >= '" + getMediumDateString( pe.getStartDate() ) + "' and pe.enddate <= '" + getMediumDateString( pe.getEndDate() ) + "') or ";
        }
        
        sql = TextUtils.removeLastOr( sql ) + ") and (";
        
        for ( DimensionalItemObject orgUnit : orgUnits )
        {
            OrganisationUnit ou = (OrganisationUnit) orgUnit;
            int level = ou.getLevel();
            sql += "(dv.sourceid in (select organisationunitid from _orgunitstructure where idlevel" + level + " = " + ou.getId() + ")) or ";            
        }

        sql = TextUtils.removeLastOr( sql ) + ") ";
        sql += 
            "and dv.deleted is false " +
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
    public static List<DimensionalItemObject> getByDataDimensionItemType( DataDimensionItemType itemType, List<DimensionalItemObject> dataDimensionOptions )
    {
        List<DimensionalItemObject> list = new ArrayList<>();
        
        for ( DimensionalItemObject object : dataDimensionOptions )
        {
            Class<?> clazz = ReflectionUtils.getRealClass( object.getClass() );
            
            if ( clazz.equals( DATA_DIMENSION_TYPE_CLASS_MAP.get( itemType ) ) )
            {
                list.add( object );
            }
        }
        
        return list;
    }
    
    /**
     * Rounds a value. If the given parameters has skip rounding, the value is
     * returned unchanged. If the given number of decimals is specified, the
     * value is rounded to the given decimals. If skip rounding is specified
     * in the given data query parameters, 10 decimals is used. Otherwise,
     * default rounding is used.
     * 
     * @param params the query parameters.
     * @param decimals the number of decimals.
     * @param value the value.
     * @return a double.
     */
    public static Double getRoundedValue( DataQueryParams params, Integer decimals, Double value )
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
        else
        {
            return MathUtils.getRounded( value );
        }
    }
    
    /**
     * Rounds a value. If the given parameters has skip rounding, the value is
     * returned unchanged. If the given number is null or not of class Double,
     * the value is returned unchanged. If skip rounding is specified in the
     * given data query parameters, 10 decimals is used. Otherwise, default
     * rounding is used.
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
        
        return MathUtils.getRounded( (Double) value );
    }
    
    /**
     * Converts the data and option combo identifiers to an operand identifier,
     * i.e. {@code deuid-cocuid} to {@code deuid.cocuid}. For {@link DataElementOperand.TotalType#AOC_ONLY}
     * a {@link ExpressionService#SYMBOL_WILDCARD} symbol will be inserted after the data
     * item.
     * 
     * @param valueMap the value map to convert.
     * @param propertyCount the number of properties to collapse into operand key.
     * @return a value map.
     */
    public static <T> Map<String, T> convertDxToOperand( Map<String, T> valueMap, TotalType totalType )
    {
        Map<String, T> map = Maps.newHashMap();
        
        for ( Entry<String, T> entry : valueMap.entrySet() )
        {
            List<String> items = Lists.newArrayList( entry.getKey().split( DimensionalObject.DIMENSION_SEP ) );
            List<String> operands = Lists.newArrayList( items.subList( 0, totalType.getPropertyCount() + 1 ) );
            List<String> dimensions = Lists.newArrayList( items.subList( totalType.getPropertyCount() + 1, items.size() ) );
            
            // Add wild card in place of category option combination
            
            if ( TotalType.AOC_ONLY == totalType )
            {
                operands.add( 1, SYMBOL_WILDCARD );
            }
            
            String operand = StringUtils.join( operands, DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP );
            String dimension = StringUtils.join( dimensions, DimensionalObject.DIMENSION_SEP );
            dimension = !dimension.isEmpty() ? ( DimensionalObject.DIMENSION_SEP + dimension ) : StringUtils.EMPTY;
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
            final Object value = entry.getValue();

            if ( value != null && Double.class.equals( value.getClass() ) )
            {
                typedMap.put( entry.getKey(), (Double) entry.getValue() );
            }
        }

        return typedMap;
    }

    /**
     * Generates a mapping where the key represents the dimensional item identifiers
     * concatenated by {@link DimensionalObject#DIMENSION_SEP} and the value is
     * the corresponding aggregated data value based on the given grid. Assumes 
     * that the value column is the last column in the grid. 
     *
     * @param grid the grid.
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
     * @param params the data query parameters.
     * @param grid the grid.
     * @return a data value set.
     */
    public static DataValueSet getDataValueSetFromGrid( DataQueryParams params, Grid grid )
    {
        int dxInx = grid.getIndexOfHeader( DATA_X_DIM_ID );
        int peInx = grid.getIndexOfHeader( PERIOD_DIM_ID );
        int ouInx = grid.getIndexOfHeader( ORGUNIT_DIM_ID );
        int coInx = grid.getIndexOfHeader( CATEGORYOPTIONCOMBO_DIM_ID );
        int aoInx = grid.getIndexOfHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID );
        int vlInx = grid.getWidth() - 1;
        
        Assert.isTrue( dxInx >= 0, "Data dimension index must be greater than or equal to zero" );
        Assert.isTrue( peInx >= 0, "Period dimension index must be greater than or equal to zero" );
        Assert.isTrue( ouInx >= 0, "Org unit dimension index must be greater than or equal to zero" );
        Assert.isTrue( coInx >= 0, "Category option combo dimension index must be greater than or equal to zero" );
        Assert.isTrue( aoInx >= 0, "Attribute option combo dimension index must be greater than or equal to zero" );
        Assert.isTrue( vlInx >= 0, "Value index must be greater than or equal to zero" );
                
        String created = DateUtils.getMediumDateString();
        
        DataValueSet dvs = new DataValueSet();
        
        Set<String> primaryKeys = Sets.newHashSet();
        
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
     * Prepares the given grid to be converted to a data value set.
     * 
     * <ul>
     * <li>Converts data values from double to integer based on the
     * associated data item if required.</li>
     * <li>Adds a category option combo and a attribute option combo
     * column to the grid based on the aggregated export properties
     * of the associated data item.</li>
     * <li>For data element operand data items, the operand identifier
     * is split and the data element identifier is used for the data
     * dimension column and the category option combo identifier is
     * used for the category option combo column.</li>
     * </ul>
     * 
     * @param params the data query parameters.
     * @param grid the grid.
     */
    public static void handleGridForDataValueSet( DataQueryParams params, Grid grid )
    {
        Map<String, DimensionalItemObject> dimItemObjectMap = AnalyticsUtils.getDimensionalItemObjectMap( params );
        
        List<Object> cocCol = Lists.newArrayList();
        List<Object> aocCol = Lists.newArrayList();
        
        int dxInx = grid.getIndexOfHeader( DATA_X_DIM_ID );
        int vlInx = grid.getWidth() - 1;
        
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

        grid.addHeader( vlInx, new GridHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID, ATTRIBUTEOPTIONCOMBO_DIM_ID, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( vlInx, new GridHeader( CATEGORYOPTIONCOMBO_DIM_ID, CATEGORYOPTIONCOMBO_DIM_ID, ValueType.TEXT, String.class.getName(), false, true ) )
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
     * @return an object, double or integer depending on the given arguments.
     */
    public static Object getIntegerOrValue( Object value, DimensionalItemObject item )
    {
        boolean doubleValue = item != null && value != null && ( value instanceof Double );
        
        if ( doubleValue )
        {
            if ( DimensionItemType.DATA_ELEMENT == item.getDimensionItemType() && ((DataElement) item).getValueType().isInteger() )
            {
                value = ((Double) value).intValue();
            }
            else if ( DimensionItemType.DATA_ELEMENT_OPERAND == item.getDimensionItemType() && ((DataElementOperand) item).getDataElement().getValueType().isInteger() )
            {
                value = ((Double) value).intValue();
            }
            else if ( DimensionItemType.INDICATOR == item.getDimensionItemType() && ((Indicator) item).hasZeroDecimals() )
            {
                value = ((Double) value).intValue();
            }
        }
        
        return value;        
    }
    
    /**
     * Returns a mapping between dimension item identifiers and dimensional
     * item object for the given query.
     *
     * @param params the data query parameters.
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
     * @param params the data query parameters.
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

                if ( DimensionType.ORGANISATION_UNIT.equals( dimension.getDimensionType() ) && params.isHierarchyMeta() )
                {
                    OrganisationUnit unit = (OrganisationUnit) item;

                    map.putAll( NameableObjectUtils.getUidDisplayPropertyMap( unit.getAncestors(), params.getDisplayProperty() ) );
                }
            }

            map.put( dimension.getDimension(), dimension.getDisplayProperty( params.getDisplayProperty() ) );
        }

        return map;
    }

    /**
     * Returns a mapping between identifiers and meta data items for the given query.
     *
     * @param params the data query parameters.
     * @return a mapping between identifiers and meta data items.
     */
    public static Map<String, MetadataItem> getDimensionMetadataItemMap( DataQueryParams params )
    {
        List<DimensionalObject> dimensions = params.getDimensionsAndFilters();

        Map<String, MetadataItem> map = new HashMap<>();

        Calendar calendar = PeriodType.getCalendar();

        for ( DimensionalObject dimension : dimensions )
        {
            for ( DimensionalItemObject item : dimension.getItems() )
            {
                if ( DimensionType.PERIOD == dimension.getDimensionType() && !calendar.isIso8601() )
                {
                    Period period = (Period) item;
                    DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
                    map.put( period.getPeriodType().getIsoDate( dateTimeUnit ), new MetadataItem( period.getDisplayName() ) );
                }
                else
                {
                    String legendSet = item.hasLegendSet() ? item.getLegendSet().getUid() : null;
                    map.put( item.getDimensionItem(), new MetadataItem( item.getDisplayProperty( params.getDisplayProperty() ), legendSet ) );
                }

                if ( DimensionType.ORGANISATION_UNIT == dimension.getDimensionType() && params.isHierarchyMeta() )
                {
                    OrganisationUnit unit = (OrganisationUnit) item;
                    
                    for ( OrganisationUnit ancestor : unit.getAncestors() )
                    {
                        map.put( ancestor.getUid(), new MetadataItem( ancestor.getDisplayProperty( params.getDisplayProperty() ) ) );
                    }
                }
                
                if ( DimensionItemType.DATA_ELEMENT == item.getDimensionItemType() )
                {
                    DataElement dataElement = (DataElement) item;
                    
                    for ( DataElementCategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
                    {
                        map.put( coc.getUid(), new MetadataItem( coc.getDisplayProperty( params.getDisplayProperty() ) ) );
                    }
                }
            }

            map.put( dimension.getDimension(), new MetadataItem( dimension.getDisplayProperty( params.getDisplayProperty() ) ) );
        }

        return map;
    }

    /**
     * Returns a mapping between the category option combo identifiers and names
     * for the given query.
     *
     * @param params the data query parameters.
     * @returns a mapping between identifiers and names.
     */
    public static Map<String, String> getCocNameMap( DataQueryParams params )
    {
        Map<String, String> metaData = new HashMap<>();

        List<DimensionalItemObject> des = params.getAllDataElements();

        if ( !des.isEmpty() )
        {
            for ( DimensionalItemObject de : des )
            {
                DataElement dataElement = (DataElement) de;
                
                for ( DataElementCategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
                {
                    metaData.put( coc.getUid(), coc.getName() );
                }
            }
        }

        return metaData;
    }
    
    /**
     * Returns a mapping of identifiers and names for the given event query.
     * 
     * @param params the event query.
     * @return a mapping of identifiers and names for the given event query.
     */
    public static Map<String, String> getUidNameMap( EventQueryParams params )
    {
        Map<String, String> map = new HashMap<>();

        Program program = params.getProgram();
        ProgramStage stage = params.getProgramStage();

        map.put( program.getUid(), program.getDisplayProperty( params.getDisplayProperty() ) );

        if ( stage != null )
        {
            map.put( stage.getUid(), stage.getName() );
        }
        else
        {
            for ( ProgramStage st : program.getProgramStages() )
            {
                map.put( st.getUid(), st.getName() );
            }
        }

        if ( params.hasValueDimension() )
        {
            map.put( params.getValue().getUid(), params.getValue().getDisplayProperty( params.getDisplayProperty() ) );
        }
        
        map.putAll( getUidDisplayPropertyMap( params.getItems(), params.getDisplayProperty() ) );
        map.putAll( getUidDisplayPropertyMap( params.getItemFilters(), params.getDisplayProperty() ) );
        map.putAll( getUidDisplayPropertyMap( params.getDimensions(), params.isHierarchyMeta(), params.getDisplayProperty() ) );
        map.putAll( getUidDisplayPropertyMap( params.getFilters(), params.isHierarchyMeta(), params.getDisplayProperty() ) );
        map.putAll( IdentifiableObjectUtils.getUidNameMap( params.getLegends() ) );
        
        return map;
    }
    /**
     * Returns a mapping between identifiers and display properties for the given 
     * list of query items.
     * 
     * @param queryItems the list of query items.
     * @param displayProperty the display property to use.
     * @return a mapping between identifiers and display properties.
     */
    public static Map<String, String> getUidDisplayPropertyMap( List<QueryItem> queryItems, DisplayProperty displayProperty )
    {
        Map<String, String> map = new HashMap<>();
        
        for ( QueryItem item : queryItems )
        {
            map.put( item.getItem().getUid(), item.getItem().getDisplayProperty( displayProperty ) );
        }
        
        return map;
    }

    /**
     * Returns a mapping between identifiers and display properties for the given 
     * list of dimensions.
     * 
     * @param dimensions the dimensions.
     * @param hierarchyMeta indicates whether to include meta data about the
     *        organisation unit hierarchy.
     * @return a mapping between identifiers and display properties.
     */
    public static Map<String, String> getUidDisplayPropertyMap( List<DimensionalObject> dimensions, boolean hierarchyMeta, DisplayProperty displayProperty )
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
     * Returns true if the given period occurs less than maxYears before the current date.
     * 
     * @param period periods to check
     * @param maxYears amount of years back to check
     * @return false if maxYears is 0 or period occurs earlier than maxYears years since now.
     */
    public static boolean periodIsOutsideApprovalMaxYears( Period period, Integer maxYears )
    {
        if ( maxYears == 0 )
        {
            return false;
        }

        java.util.Calendar periodDate = java.util.Calendar.getInstance();
        java.util.Calendar now = java.util.Calendar.getInstance();

        periodDate.setTime( period.getStartDate() );

        return ( now.get( java.util.Calendar.YEAR ) - periodDate.get( java.util.Calendar.YEAR ) ) >= maxYears;
    }
}
