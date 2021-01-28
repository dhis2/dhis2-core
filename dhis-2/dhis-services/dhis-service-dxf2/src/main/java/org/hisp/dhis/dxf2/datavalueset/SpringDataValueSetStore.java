/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.CsvUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.staxwax.factory.XMLFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Repository( "org.hisp.dhis.dxf2.datavalueset.DataValueSetStore" )
public class SpringDataValueSetStore
    implements DataValueSetStore
{
    private CurrentUserService currentUserService;

    private final JdbcTemplate jdbcTemplate;

    public SpringDataValueSetStore( CurrentUserService currentUserService, JdbcTemplate jdbcTemplate )
    {
        checkNotNull( currentUserService );
        checkNotNull( jdbcTemplate );

        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Use only for testing.
     */
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // --------------------------------------------------------------------------
    // DataValueSetStore implementation
    // --------------------------------------------------------------------------

    @Override
    public void writeDataValueSetXml( DataExportParams params, Date completeDate, OutputStream out )
    {
        DataValueSet dataValueSet = new StreamingXmlDataValueSet( XMLFactory.getXMLWriter( out ) );

        String sql = getDataValueSql( params );

        writeDataValueSet( sql, params, completeDate, dataValueSet );
    }

    @Override
    public void writeDataValueSetJson( DataExportParams params, Date completeDate, OutputStream out )
    {
        DataValueSet dataValueSet = new StreamingJsonDataValueSet( out );

        String sql = getDataValueSql( params );

        writeDataValueSet( sql, params, completeDate, dataValueSet );
    }

    @Override
    public void writeDataValueSetCsv( DataExportParams params, Date completeDate, Writer writer )
    {
        DataValueSet dataValueSet = new StreamingCsvDataValueSet( CsvUtils.getWriter( writer ) );

        String sql = getDataValueSql( params );

        writeDataValueSet( sql, params, completeDate, dataValueSet );
    }

    @Override
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes )
    {
        DataValueSet dataValueSet = new StreamingJsonDataValueSet( outputStream );

        final String sql = buildDataValueSql( lastUpdated, idSchemes );

        writeDataValueSet( sql, new DataExportParams(), null, dataValueSet );
    }

    @Override
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes, int pageSize,
        int page )
    {
        DataValueSet dataValueSet = new StreamingJsonDataValueSet( outputStream );

        final int offset = (page - 1) * pageSize;
        final String sql = buildDataValueSql( lastUpdated, idSchemes ) +
            "order by pe.startdate asc, dv.created asc, deid asc limit " + pageSize + " offset " + offset;

        writeDataValueSet( sql, new DataExportParams(), null, dataValueSet );
    }

    private String buildDataValueSql( Date lastUpdated, IdSchemes idSchemes )
    {
        String deScheme = idSchemes.getDataElementIdScheme().getIdentifiableString().toLowerCase();
        String ouScheme = idSchemes.getOrgUnitIdScheme().getIdentifiableString().toLowerCase();
        String ocScheme = idSchemes.getCategoryOptionComboIdScheme().getIdentifiableString().toLowerCase();

        final String sql = "select de." + deScheme + " as deid, pe.startdate as pestart, pt.name as ptname, ou."
            + ouScheme + " as ouid, " +
            "coc." + ocScheme + " as cocid, aoc." + ocScheme + " as aocid, " +
            "dv.value, dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup, dv.deleted " +
            "from datavalue dv " +
            "join dataelement de on (dv.dataelementid=de.dataelementid) " +
            "join period pe on (dv.periodid=pe.periodid) " +
            "join periodtype pt on (pe.periodtypeid=pt.periodtypeid) " +
            "join organisationunit ou on (dv.sourceid=ou.organisationunitid) " +
            "join categoryoptioncombo coc on (dv.categoryoptioncomboid=coc.categoryoptioncomboid) " +
            "join categoryoptioncombo aoc on (dv.attributeoptioncomboid=aoc.categoryoptioncomboid) " +
            "where dv.lastupdated >= '" + DateUtils.getLongDateString( lastUpdated ) + "' ";

        return sql;
    }

    private void writeDataValueSet( String sql, DataExportParams params, Date completeDate,
        final DataValueSet dataValueSet )
    {
        if ( params.isSingleDataValueSet() )
        {
            IdSchemes idScheme = params.getOutputIdSchemes() != null ? params.getOutputIdSchemes() : new IdSchemes();
            IdScheme ouScheme = idScheme.getOrgUnitIdScheme();
            IdScheme dataSetScheme = idScheme.getDataSetIdScheme();

            dataValueSet.setDataSet( params.getFirstDataSet().getPropertyValue( dataSetScheme ) );
            dataValueSet.setCompleteDate( getLongGmtDateString( completeDate ) );
            dataValueSet.setPeriod( params.getFirstPeriod().getIsoDate() );
            dataValueSet.setOrgUnit( params.getFirstOrganisationUnit().getPropertyValue( ouScheme ) );
        }

        final Calendar calendar = PeriodType.getCalendar();

        jdbcTemplate.query( sql, new RowCallbackHandler()
        {
            @Override
            public void processRow( ResultSet rs )
                throws SQLException
            {
                DataValue dataValue = dataValueSet.getDataValueInstance();
                PeriodType pt = PeriodType.getPeriodTypeByName( rs.getString( "ptname" ) );
                boolean deleted = rs.getBoolean( "deleted" );

                dataValue.setDataElement( rs.getString( "deid" ) );
                dataValue.setPeriod( pt.createPeriod( rs.getDate( "pestart" ), calendar ).getIsoDate() );
                dataValue.setOrgUnit( rs.getString( "ouid" ) );
                dataValue.setCategoryOptionCombo( rs.getString( "cocid" ) );
                dataValue.setAttributeOptionCombo( rs.getString( "aocid" ) );
                dataValue.setValue( rs.getString( "value" ) );
                dataValue.setStoredBy( rs.getString( "storedby" ) );
                dataValue.setCreated( getLongGmtDateString( rs.getTimestamp( "created" ) ) );
                dataValue.setLastUpdated( getLongGmtDateString( rs.getTimestamp( "lastupdated" ) ) );
                dataValue.setComment( rs.getString( "comment" ) );
                dataValue.setFollowup( rs.getBoolean( "followup" ) );

                if ( deleted )
                {
                    dataValue.setDeleted( deleted );
                }

                dataValue.close();
            }
        } );

        dataValueSet.close();
    }

    // --------------------------------------------------------------------------
    // Supportive methods
    // --------------------------------------------------------------------------

    private String getDataValueSql( DataExportParams params )
    {
        Preconditions.checkArgument( !params.getAllDataElements().isEmpty() );

        User user = currentUserService.getCurrentUser();

        IdSchemes idScheme = params.getOutputIdSchemes() != null ? params.getOutputIdSchemes() : new IdSchemes();

        String deScheme = idScheme.getDataElementIdScheme().getIdentifiableString().toLowerCase();
        String ouScheme = idScheme.getOrgUnitIdScheme().getIdentifiableString().toLowerCase();
        String cocScheme = idScheme.getCategoryOptionComboIdScheme().getIdentifiableString().toLowerCase();

        String dataElements = getCommaDelimitedString( getIdentifiers( params.getAllDataElements() ) );
        String orgUnits = getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) );
        String orgUnitGroups = getCommaDelimitedString( getIdentifiers( params.getOrganisationUnitGroups() ) );

        // ----------------------------------------------------------------------
        // Identifier schemes
        // ----------------------------------------------------------------------

        String deSql = idScheme.getDataElementIdScheme().isAttribute()
            ? "de.attributevalues #>> '{\"" + idScheme.getDataElementIdScheme().getAttribute()
                + "\", \"value\" }'  as deid"
            : "de." + deScheme + " as deid";

        String ouSql = idScheme.getOrgUnitIdScheme().isAttribute()
            ? "ou.attributevalues #>> '{\"" + idScheme.getOrgUnitIdScheme().getAttribute() + "\", \"value\" }'  as ouid"
            : "ou." + ouScheme + " as ouid";

        String cocSql = idScheme.getCategoryOptionComboIdScheme().isAttribute()
            ? "coc.attributevalues #>> '{\"" + idScheme.getCategoryOptionComboIdScheme().getAttribute()
                + "\", \"value\" }'  as cocid"
            : "coc." + cocScheme + " as cocid";

        String aocSql = idScheme.getCategoryOptionComboIdScheme().isAttribute()
            ? "aoc.attributevalues #>> '{\"" + idScheme.getCategoryOptionComboIdScheme().getAttribute()
                + "\", \"value\" }'  as aocid"
            : "aoc." + cocScheme + " as aocid";

        // ----------------------------------------------------------------------
        // Data values
        // ----------------------------------------------------------------------

        String sql = "select " + deSql + ", pe.startdate as pestart, pt.name as ptname, " + ouSql + ", " + cocSql + ", "
            + aocSql + ", " +
            "dv.value, dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup, dv.deleted " +
            "from datavalue dv " +
            "inner join dataelement de on (dv.dataelementid=de.dataelementid) " +
            "inner join period pe on (dv.periodid=pe.periodid) " +
            "inner join periodtype pt on (pe.periodtypeid=pt.periodtypeid) " +
            "inner join organisationunit ou on (dv.sourceid=ou.organisationunitid) " +
            "inner join categoryoptioncombo coc on (dv.categoryoptioncomboid=coc.categoryoptioncomboid) " +
            "inner join categoryoptioncombo aoc on (dv.attributeoptioncomboid=aoc.categoryoptioncomboid) ";

        // ----------------------------------------------------------------------
        // Filters
        // ----------------------------------------------------------------------

        if ( params.hasOrganisationUnitGroups() )
        {
            sql += "left join orgunitgroupmembers ougm on (ou.organisationunitid=ougm.organisationunitid) ";
        }

        sql += "where de.dataelementid in (" + dataElements + ") ";

        if ( params.isIncludeChildren() )
        {
            sql += "and (";

            for ( OrganisationUnit parent : params.getOrganisationUnits() )
            {
                sql += "ou.path like '" + parent.getPath() + "%' or ";
            }

            sql = TextUtils.removeLastOr( sql ) + ") ";
        }
        else
        {
            sql += "and (";

            if ( params.hasOrganisationUnits() )
            {
                sql += "dv.sourceid in (" + orgUnits + ") ";
            }

            if ( params.hasOrganisationUnits() && params.hasOrganisationUnitGroups() )
            {
                sql += "or ";
            }

            if ( params.hasOrganisationUnitGroups() )
            {
                sql += "ougm.orgunitgroupid in (" + orgUnitGroups + ") ";
            }

            sql += ") ";
        }

        if ( !params.isIncludeDeleted() )
        {
            sql += "and dv.deleted is false ";
        }

        if ( params.hasStartEndDate() )
        {
            sql += "and (pe.startdate >= '" + getMediumDateString( params.getStartDate() ) + "' and pe.enddate <= '"
                + getMediumDateString( params.getEndDate() ) + "') ";
        }
        else if ( params.hasPeriods() )
        {
            sql += "and dv.periodid in (" + getCommaDelimitedString( getIdentifiers( params.getPeriods() ) ) + ") ";
        }

        if ( params.hasAttributeOptionCombos() )
        {
            sql += "and dv.attributeoptioncomboid in ("
                + getCommaDelimitedString( getIdentifiers( params.getAttributeOptionCombos() ) ) + ") ";
        }

        if ( params.hasLastUpdated() )
        {
            sql += "and dv.lastupdated >= '" + getLongGmtDateString( params.getLastUpdated() ) + "' ";
        }
        else if ( params.hasLastUpdatedDuration() )
        {
            sql += "and dv.lastupdated >= '"
                + getLongGmtDateString( DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) ) + "' ";
        }

        if ( user != null && !user.isSuper() )
        {
            sql += getAttributeOptionComboClause( user );
        }

        if ( params.hasLimit() )
        {
            sql += "limit " + params.getLimit();
        }

        log.debug( "Get data value set SQL: " + sql );

        return sql;
    }

    /**
     * Returns an attribute option combo filter SQL clause. The filter enforces
     * that only attribute option combinations which the given user has access
     * to are returned.
     *
     * @param user the user.
     * @return an SQL filter clause.
     */
    private String getAttributeOptionComboClause( User user )
    {
        List<String> groupsIds = user.getGroups().stream().map( g -> g.getUid() )
            .collect( Collectors.toList() );

        String groupAccessCheck = groupsIds.size() > 0
            ? " or ( " + JsonbFunctions.HAS_USER_GROUP_IDS + "( co.sharing, '{" + String.join( ",", groupsIds )
                + "}' ) = true " +
                "and " + JsonbFunctions.CHECK_USER_GROUPS_ACCESS + "( co.sharing, '__r%', '{"
                + String.join( ",", groupsIds ) + "}' ) = true ) )"
            : "";

        String userAccessCheck = " or ( " + JsonbFunctions.HAS_USER_ID + "( co.sharing, '" + user.getUid()
            + "' ) = true " +
            " and " + JsonbFunctions.CHECK_USER_ACCESS + "( co.sharing, '__r%', '" + user.getUid() + "' ) = true ) )";

        return "and dv.attributeoptioncomboid not in (" +
            "select distinct(cocco.categoryoptioncomboid) " +
            "from categoryoptioncombos_categoryoptions as cocco " +
            // Get inaccessible category options
            "where cocco.categoryoptionid not in ( " +
            "select co.categoryoptionid " +
            "from dataelementcategoryoption co  " +
            " where "
            + JpaQueryUtils.generateSQlQueryForSharingCheck( "co.sharing", user, AclService.LIKE_READ_DATA ) + ") )";
    }
}
