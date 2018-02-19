package org.hisp.dhis.dataanalysis.jdbc;

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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.collection.PaginatedList;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataanalysis.DataAnalysisMeasures;
import org.hisp.dhis.dataanalysis.DataAnalysisStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.objectmapper.DeflatedDataValueNameMinMaxRowMapper;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Lars Helge Overland
 * @author Halvdan Hoem Grelland
 */
public class JdbcDataAnalysisStore
    implements DataAnalysisStore
{
    private static final Log log = LogFactory.getLog( JdbcDataAnalysisStore.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    /**
     * Read only JDBC template.
     */
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // OutlierAnalysisStore implementation
    // -------------------------------------------------------------------------

    @Override
    public List<DataAnalysisMeasures> getDataAnalysisMeasures( DataElement dataElement,
        Collection<DataElementCategoryOptionCombo> categoryOptionCombos,
        Collection<String> parentPaths, Date from )
    {
        List<DataAnalysisMeasures> measures = new ArrayList<>();

        if ( categoryOptionCombos.isEmpty() || parentPaths.isEmpty() )
        {
            return measures;
        }

        String catOptionComboIds = TextUtils.getCommaDelimitedString( getIdentifiers( categoryOptionCombos ) );

        String matchPaths = "(";
        for ( String path : parentPaths )
        {
            matchPaths += "ou.path like '" + path + "%' or ";
        }
        matchPaths = TextUtils.removeLastOr( matchPaths ) + ") ";

        String sql =
            "select dv.sourceid, dv.categoryoptioncomboid, " +
                "avg( cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) ) as average, " +
                "stddev_pop( cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) ) as standarddeviation " +
                "from datavalue dv " +
                "join organisationunit ou on ou.organisationunitid = dv.sourceid " +
                "join period pe on dv.periodid = pe.periodid " +
                "where dv.dataelementid = " + dataElement.getId() + " " +
                "and dv.categoryoptioncomboid in (" + catOptionComboIds + ") " +
                "and pe.startdate >= '" + DateUtils.getMediumDateString( from ) + "' " +
                "and " + matchPaths +
                "and dv.deleted is false " +
                "group by dv.sourceid, dv.categoryoptioncomboid";

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        while ( rowSet.next() )
        {
            int orgUnitId = rowSet.getInt( 1 );
            int categoryOptionComboId = rowSet.getInt( 2 );
            double average = rowSet.getDouble( 3 );
            double standardDeviation = rowSet.getDouble( 4 );

            if ( standardDeviation != 0.0 )
            {
                measures.add( new DataAnalysisMeasures( orgUnitId, categoryOptionComboId, average, standardDeviation ) );
            }
        }

        return measures;
    }
    
    @Override
    public List<DeflatedDataValue> getMinMaxViolations( Collection<DataElement> dataElements, Collection<DataElementCategoryOptionCombo> categoryOptionCombos,
        Collection<Period> periods, Collection<OrganisationUnit> parents, int limit )
    {
        if ( dataElements.isEmpty() || categoryOptionCombos.isEmpty() || periods.isEmpty() || parents.isEmpty() )
        {
            return new ArrayList<>();
        }
        
        String dataElementIds = getCommaDelimitedString( getIdentifiers( dataElements ) );
        String periodIds = getCommaDelimitedString( getIdentifiers( periods ) );
        String categoryOptionComboIds = getCommaDelimitedString( getIdentifiers( categoryOptionCombos ) );
                
        String sql = 
            "select dv.dataelementid, dv.periodid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, dv.storedby, dv.lastupdated, " +
            "dv.created, dv.comment, dv.followup, ou.name as sourcename, de.name as dataelementname, " +
            "pt.name as periodtypename, pe.startdate, pe.enddate, coc.name as categoryoptioncomboname, mm.minimumvalue, mm.maximumvalue " +
            "from datavalue dv " +
            "join minmaxdataelement mm on ( dv.dataelementid = mm.dataelementid and dv.categoryoptioncomboid = mm.categoryoptioncomboid and dv.sourceid = mm.sourceid ) " +
            "join dataelement de on dv.dataelementid = de.dataelementid " +
            "join period pe on dv.periodid = pe.periodid " +
            "join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "where dv.dataelementid in (" + dataElementIds + ") " +
            "and dv.categoryoptioncomboid in (" + categoryOptionComboIds + ") " +
            "and dv.periodid in (" + periodIds + ") " + 
            "and ( " +
                "cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) < mm.minimumvalue " +
                "or cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) > mm.maximumvalue ) " +
            "and (";

        for ( OrganisationUnit parent : parents )
        {
            sql += "ou.path like '" + parent.getPath() + "%' or ";
        }
        
        sql = TextUtils.removeLastOr( sql ) + ") ";
        sql += "and dv.deleted is false ";
        
        sql += statementBuilder.limitRecord( 0, limit );
        
        return jdbcTemplate.query( sql, new DeflatedDataValueNameMinMaxRowMapper( null, null ) );
    }
    
    @Override
    public List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, Map<Integer, Integer> lowerBoundMap, Map<Integer, Integer> upperBoundMap )
    {
        if ( lowerBoundMap == null || lowerBoundMap.isEmpty() || periods.isEmpty() )
        {
            return new ArrayList<>();
        }
        
        //TODO parallel processes?
                
        List<List<Integer>> organisationUnitPages = new PaginatedList<>( lowerBoundMap.keySet() ).setPageSize( 1000 ).getPages();
        
        log.debug( "No of pages: " + organisationUnitPages.size() );
        
        List<DeflatedDataValue> dataValues = new ArrayList<>();
        
        for ( List<Integer> unitPage : organisationUnitPages )
        {
            dataValues.addAll( getDeflatedDataValues( dataElement, categoryOptionCombo, periods, unitPage, lowerBoundMap, upperBoundMap ) );
        }
        
        return dataValues;
    }
    
    private List<DeflatedDataValue> getDeflatedDataValues( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo,
        Collection<Period> periods, List<Integer> organisationUnits, Map<Integer, Integer> lowerBoundMap, Map<Integer, Integer> upperBoundMap )
    {
        String periodIds = TextUtils.getCommaDelimitedString( getIdentifiers( periods ) );
        
        String sql = 
            "select dv.dataelementid, dv.periodid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, dv.storedby, dv.lastupdated, " +
            "dv.created, dv.comment, dv.followup, ou.name as sourcename, " +
            "'" + dataElement.getName() + "' as dataelementname, pt.name as periodtypename, pe.startdate, pe.enddate, " + 
            "'" + categoryOptionCombo.getName() + "' as categoryoptioncomboname " +
            "from datavalue dv " +
            "join period pe on dv.periodid = pe.periodid " +
            "join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "where dv.dataelementid = " + dataElement.getId() + " " +
            "and dv.categoryoptioncomboid = " + categoryOptionCombo.getId() + " " +
            "and dv.periodid in (" + periodIds + ") and ( ";
        
        for ( Integer orgUnitUid : organisationUnits )
        {
            sql += "( dv.sourceid = " + orgUnitUid + " " +
                "and ( cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) < " + lowerBoundMap.get( orgUnitUid ) + " " +
                "or cast( dv.value as " + statementBuilder.getDoubleColumnType() + " ) > " + upperBoundMap.get( orgUnitUid ) + " ) ) or ";
        }
        
        sql = TextUtils.removeLastOr( sql ) + " ) ";
        sql += "and dv.deleted is false ";
        
        return jdbcTemplate.query( sql, new DeflatedDataValueNameMinMaxRowMapper( lowerBoundMap, upperBoundMap ) );
    }

    @Override
    public List<DeflatedDataValue> getFollowupDataValues( OrganisationUnit organisationUnit, int limit )
    {
        final String sql =
            "select dv.dataelementid, dv.periodid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, dv.value, " +
            "dv.storedby, dv.lastupdated, dv.created, dv.comment, dv.followup, mm.minimumvalue, mm.maximumvalue, de.name AS dataelementname, " +
            "pe.startdate, pe.enddate, pt.name AS periodtypename, ou.name AS sourcename, cc.name AS categoryoptioncomboname " +
            "from datavalue dv " +
            "left join minmaxdataelement mm on (dv.sourceid = mm.sourceid and dv.dataelementid = mm.dataelementid and dv.categoryoptioncomboid = mm.categoryoptioncomboid) " +
            "join dataelement de on dv.dataelementid = de.dataelementid " +
            "join period pe on dv.periodid = pe.periodid " +
            "join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "join organisationunit ou on ou.organisationunitid = dv.sourceid " +
            "join categoryoptioncombo cc on dv.categoryoptioncomboid = cc.categoryoptioncomboid " +
            "where ou.path like '%" + organisationUnit.getUid() + "%' " +
            "and dv.followup = true " +
            "and dv.deleted is false " +
            statementBuilder.limitRecord( 0, limit );
        
        return jdbcTemplate.query( sql, new DeflatedDataValueNameMinMaxRowMapper() );
    }
}
