package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import static org.hisp.dhis.common.DataDimensionItem.DATA_DIMENSION_TYPE_DOMAIN_MAP;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ReflectionUtils;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtils
{
    public static String getDebugDataSql( DataQueryParams params )
    {
        List<NameableObject> dataElements = new ArrayList<>( NameableObjectUtils.getCopyNullSafe( params.getDataElements() ) );
        dataElements.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterDataElements() ) );
        
        List<NameableObject> periods = new ArrayList<>( NameableObjectUtils.getCopyNullSafe( params.getPeriods() ) );
        periods.addAll( NameableObjectUtils.getCopyNullSafe( params.getFilterPeriods() ) );
        
        List<NameableObject> orgUnits = new ArrayList<>( NameableObjectUtils.getCopyNullSafe( params.getOrganisationUnits() ) );
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
        
        for ( NameableObject period : periods )
        {
            Period pe = (Period) period;
            sql += "(pe.startdate >= '" + getMediumDateString( pe.getStartDate() ) + "' and pe.enddate <= '" + getMediumDateString( pe.getEndDate() ) + "') or ";
        }
        
        sql = TextUtils.removeLastOr( sql ) + ") and (";
        
        for ( NameableObject orgUnit : orgUnits )
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
    public static List<NameableObject> getByDataDimensionType( DataDimensionItemType itemType, List<NameableObject> dataDimensionOptions )
    {
        List<NameableObject> list = new ArrayList<>();
        
        for ( NameableObject object : dataDimensionOptions )
        {
            Class<?> clazz = ReflectionUtils.getRealClass( object.getClass() );
            
            if ( !clazz.equals( DATA_DIMENSION_TYPE_CLASS_MAP.get( itemType ) ) )
            {
                continue;
            }
            
            if ( clazz.equals( DataElement.class ) && !( ((DataElement) object).getDomainType().equals( DATA_DIMENSION_TYPE_DOMAIN_MAP.get( itemType ) ) ) )
            {
                continue;
            }
            
            list.add( object );
        }
        
        return list;
    }
    
    /**
     * Returns a value. If the given parameters has skip rounding, the value is
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
        if ( params.isSkipRounding() )
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
}
