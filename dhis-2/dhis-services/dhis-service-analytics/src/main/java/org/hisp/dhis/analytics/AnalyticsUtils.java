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
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ReflectionUtils;

import com.google.common.collect.Maps;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtils
{
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

        sql = TextUtils.removeLastOr( sql ) + ") limit 100000;";
        
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
     * concatenated by {@link DimensionalObject.DIMENSION_SEP} and the value is 
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
}
