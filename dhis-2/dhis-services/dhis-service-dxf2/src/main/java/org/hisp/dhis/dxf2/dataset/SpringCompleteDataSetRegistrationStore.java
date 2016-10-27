package org.hisp.dhis.dxf2.dataset;

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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class SpringCompleteDataSetRegistrationStore
    implements CompleteDataSetRegistrationStore
{
    private static final Log log = LogFactory.getLog( SpringCompleteDataSetRegistrationStore.class );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //--------------------------------------------------------------------------
    // CompleteDataSetRegistrationStore implementation
    //--------------------------------------------------------------------------

    @Override
    public void writeCompleteDataSetRegistrationsXml( ExportParams params, OutputStream outputStream )
    {

    }

    @Override
    public void writeCompleteDataSetRegistrationsJson( ExportParams params, OutputStream outputStream )
    {
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void write( String query, ExportParams params, final CompleteDataSetRegistrations items )
    {
        final Calendar calendar = PeriodType.getCalendar();

        jdbcTemplate.query( query, rs -> {
            CompleteDataSetRegistration cdsr = new CompleteDataSetRegistration();
            cdsr.open();

            cdsr.setPeriod( toIsoDate( rs.getString( Param.PERIOD_TYPE.name ), rs.getDate( Param.PERIOD_START.name ), calendar ) );
            cdsr.setDataSet( rs.getString( Param.DATA_SET.name ) );
            cdsr.setOrganisationUnit( rs.getString( Param.ORG_UNIT.name ) );
            cdsr.setAttributeOptionCombo( rs.getString( Param.ATTR_OPT_COMBO.name ) );
            cdsr.setDate( rs.getString( Param.DATE.name ) );
            cdsr.setStoredBy( rs.getString( Param.STORED_BY.name ) );

            cdsr.close();
        } );
    }

    private static String createQuery( ExportParams params )
    {
        IdSchemes idSchemes = params.getOutputIdSchemes() != null ? params.getOutputIdSchemes() : new IdSchemes();

        ImmutableMap.Builder<String, String> namedParamsBuilder = ImmutableMap.<String, String>builder()
            .put( "dsScheme", idSchemes.getDataSetIdScheme().getIdentifiableString().toLowerCase() )
            .put( "ouScheme", idSchemes.getOrgUnitIdScheme().getIdentifiableString().toLowerCase() )
            .put( "aocScheme", idSchemes.getAttributeOptionComboIdScheme().getIdentifiableString().toLowerCase() );

        String sql = // language=SQL
            "SELECT ds.${dsScheme} AS dsid, pe.startdate AS pestart, pt.name AS ptname, " +
            "ou.${ouScheme} AS ouid, aoc.${aocScheme} AS aocid, cdsr.storedby AS storedby, cdsr.date AS created " +
            "FROM completedatasetregistration cdsr " +
            "INNER JOIN dataset ds ON (cdsr.datasetid=ds.datasetid) " +
            "INNER JOIN period pe ON (cdsr.periodid=pe.periodid) " +
            "INNER JOIN organisationunit ou ON (cdsr.sourceid=ou.organisationunitid) " +
            "INNER JOIN categoryoptioncombo aoc ON (cdsr.attributeoptioncomboid = aoc.categoryoptioncomboid) ";

        sql += createOrgUnitGroupJoin( params );
        sql += createDataSetClause( params, namedParamsBuilder );
        sql += createOrgUnitClause( params, namedParamsBuilder );
        sql += createPeriodClause( params, namedParamsBuilder );
        sql += createCreatedClause( params, namedParamsBuilder );
        sql += createLimitClause( params, namedParamsBuilder );

        sql = injectNamedParams( sql, namedParamsBuilder.build() );

        log.info( "Get CompleteDataSetRegistrations SQL: " + sql );

        return sql;
    }

    private static String createOrgUnitGroupJoin( ExportParams params )
    {
        if ( params.hasOrganisationUnitGroups() )
        {
            // language=SQL
            return " LEFT JOIN orgunitgroupmembers ougm on (ou.organisationunitid=ougm.organisationunitid) ";
        }

        return "";
    }

    private static String createDataSetClause( ExportParams params, ImmutableMap.Builder<String, String> namedParamsBuilder )
    {
        namedParamsBuilder.put( "dataSets", commaDelimitedIds( params.getDataSets() ) );
        return " WHERE cdsr.datasetid in ($dataSets) ";
    }

    private static String createOrgUnitClause( ExportParams params, ImmutableMap.Builder<String, String> namedParamsBuilder )
    {
        if ( params.isIncludeChildren() )
        {
            String clause = " AND ( ";

            clause += params.getOrganisationUnits().stream()
                .map( OrganisationUnit::getPath )
                .collect( Collectors.joining( " ", "ou.path LIKE '", "%' OR" ) );

            return TextUtils.removeLastOr( clause ) + " ) ";
        }
        else
        {
            String clause = " AND ( ";

            if ( params.hasOrganisationUnits() )
            {
                namedParamsBuilder.put( "orgUnits", commaDelimitedIds( params.getOrganisationUnits() ) );
                clause += " cdsr.sourceid IN ( ${orgUnits} ) ";

                if ( params.hasOrganisationUnitGroups() )
                {
                    clause += " OR ";
                }
            }

            if ( params.hasOrganisationUnitGroups() )
            {
                namedParamsBuilder.put( "orgUnitGroups", commaDelimitedIds( params.getOrganisationUnitGroups() ) );
                clause += " ougm.orgunitgroupid in ( ${orgUnitGroups} )";
            }

            return clause + " ) ";
        }
    }

    private static String createPeriodClause( ExportParams params, ImmutableMap.Builder<String, String> namedParamsBuilder )
    {
        if ( params.hasStartEndDate() )
        {
            namedParamsBuilder
                .put( "startDate", DateUtils.getMediumDateString( params.getStartDate() ) )
                .put( "endDate", DateUtils.getMediumDateString( params.getEndDate() ) );

            // language=SQL
            return " AND ( pe.startdate >= '${startDate}' AND pe.enddate <= '${endDate}' ) ";
        }

        else if ( params.hasPeriods() )
        {
            namedParamsBuilder
                .put( "periods", commaDelimitedIds( params.getPeriods() ) );

            // language=SQL
            return " AND dv.periodid in ( ${periods} ) ";
        }

        return "";
    }

    private static String createCreatedClause( ExportParams params, ImmutableMap.Builder<String, String> namedParamsBuilder )
    {
        if ( params.hasCreated() )
        {
            namedParamsBuilder.put( "created", DateUtils.getLongGmtDateString( params.getCreated() ) );

            return " AND cdsr.date >= '${created}' ";
        }
        else if ( params.hasCreatedDuration() )
        {
            namedParamsBuilder.put( "createdDuration",
                DateUtils.getLongGmtDateString( DateUtils.nowMinusDuration( params.getCreatedDuration() ) ) );

            return " AND cdsr.date >= '${createdDuration}' ";
        }
        else
        {
            return "";
        }
    }

    private static String createLimitClause( ExportParams params, ImmutableMap.Builder<String, String> namedParamsBuilder )
    {
        if ( params.hasLimit() )
        {
            namedParamsBuilder.put( "limit", params.getLimit().toString() );

            // language=SQL
            return " LIMIT ${limit} ";
        }

        return "";
    }

    private static String commaDelimitedIds( Collection<? extends IdentifiableObject> idObjects )
    {
        return TextUtils.getCommaDelimitedString( IdentifiableObjectUtils.getIdentifiers( idObjects ) );
    }

    private static String injectNamedParams( String sql, Map<String, String> valueMap )
    {
        return new StrSubstitutor( valueMap, "${", "}" ).replace( sql );
    }

    private static String toIsoDate( String periodName, Date start, final Calendar calendar )
    {
        return PeriodType.getPeriodTypeByName( periodName ).createPeriod( start, calendar ).getIsoDate();
    }

    private enum Param {
        PERIOD_TYPE( "ptname" ),
        DATA_SET( "dsid" ),
        ORG_UNIT( "ouid" ),
        ATTR_OPT_COMBO( "aocid" ),
        DATE( "created" ),
        STORED_BY( "storedby"),
        PERIOD_START( "pe_start" ),
        PERIOD_END( "pe_end" );

        final String name;

        Param( String name )
        {
            this.name = name;
        }
    }
}
