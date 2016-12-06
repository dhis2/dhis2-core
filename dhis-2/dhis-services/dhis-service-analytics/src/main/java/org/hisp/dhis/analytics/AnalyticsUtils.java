package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.common.DataDimensionItem.DATA_DIMENSION_TYPE_CLASS_MAP;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DataDimensionalItemObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtils
{
    private static final String KEY_AGG_VALUE = "[aggregated]";
    
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
     * value is rounded to the given decimals. Otherwise, default rounding is
     * used.
     * 
     * @param params the query parameters.
     * @param decimals the number of decimals.
     * @param value the value.
     * @return a double.
     */
    public static Double getRoundedValue( DataQueryParams params, Integer decimals, Double value )
    {
        if ( value == null || params.isSkipRounding() )
        {
            return value;
        }
        else if ( decimals != null && decimals > 0 )
        {
            return MathUtils.getRounded( value, decimals );
        }
        else
        {
            return MathUtils.getRounded( value );
        }
    }
    
    /**
     * Rounds a value. If the given parameters has skip rounding, the value is
     * returned unchanged. If the given number is null or not of class Double,
     * the value is returned unchanged. Otherwise, default rounding is used.
     *  
     * @param params the query parameters.
     * @param value the value.
     * @return a value.
     */
    public static Object getRoundedValueObject( DataQueryParams params, Object value )
    {
        if ( value == null || params.isSkipRounding() || !Double.class.equals( value.getClass() ) )
        {
            return value;
        }
        
        return MathUtils.getRounded( (Double) value );
    }
    
    /**
     * Converts the data and option combo identifiers to an operand identifier,
     * i.e. "deuid-cocuid" to "deuid.cocuid".
     * 
     * @param valueMap the value map to convert.
     * @return a value map.
     */
    public static <T> Map<String, T> convertDxToOperand( Map<String, T> valueMap )
    {
        Map<String, T> map = Maps.newHashMap();
        
        for ( Entry<String, T> entry : valueMap.entrySet() )
        {
            map.put( entry.getKey().replaceFirst( DimensionalObject.DIMENSION_SEP, 
                DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP ), entry.getValue() );
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
     * concatenated by DimensionalObject.DIMENSION_SEP and the value is
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
        
        Assert.isTrue( dxInx >= 0 );
        Assert.isTrue( peInx >= 0 );
        Assert.isTrue( ouInx >= 0 );
        Assert.isTrue( coInx >= 0 );
        Assert.isTrue( aoInx >= 0 );
        Assert.isTrue( vlInx >= 0 );
                
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
        
        Assert.isTrue( dxInx >= 0 );
        Assert.isTrue( vlInx >= 0 );
        
        for ( List<Object> row : grid.getRows() )
        {
            String dx = String.valueOf( row.get( dxInx ) );
            
            Assert.notNull( dx );
            
            DimensionalItemObject item = dimItemObjectMap.get( dx );

            Assert.notNull( item );
            
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

        grid.addHeader( vlInx, new GridHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID, ATTRIBUTEOPTIONCOMBO_DIM_ID, String.class.getName(), false, true ) );
        grid.addHeader( vlInx, new GridHeader( CATEGORYOPTIONCOMBO_DIM_ID, CATEGORYOPTIONCOMBO_DIM_ID, String.class.getName(), false, true ) );

        grid.addColumn( vlInx, aocCol );
        grid.addColumn( vlInx, cocCol );
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
            List<DimensionalItemObject> items = new ArrayList<>( dimension.getItems() );

            for ( DimensionalItemObject object : items )
            {
                if ( DimensionType.PERIOD.equals( dimension.getDimensionType() ) && !calendar.isIso8601() )
                {
                    Period period = (Period) object;
                    DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
                    map.put( period.getPeriodType().getIsoDate( dateTimeUnit ), period.getDisplayName() );
                }
                else
                {
                    map.put( object.getDimensionItem(), object.getDisplayProperty( params.getDisplayProperty() ) );
                }

                if ( DimensionType.ORGANISATION_UNIT.equals( dimension.getDimensionType() ) && params.isHierarchyMeta() )
                {
                    OrganisationUnit unit = (OrganisationUnit) object;

                    map.putAll( NameableObjectUtils.getUidDisplayPropertyMap( unit.getAncestors(), params.getDisplayProperty() ) );
                }
            }

            map.put( dimension.getDimension(), dimension.getDisplayProperty( params.getDisplayProperty() ) );
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

        if ( des != null && !des.isEmpty() )
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
}
