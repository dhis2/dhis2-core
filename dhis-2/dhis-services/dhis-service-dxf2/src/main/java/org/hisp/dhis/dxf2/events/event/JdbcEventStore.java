package org.hisp.dhis.dxf2.events.event;

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.*;
import static org.hisp.dhis.dxf2.events.event.AbstractEventService.STATIC_EVENT_COLUMNS;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.*;
import static org.hisp.dhis.system.util.DateUtils.getDateAfterAddition;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class JdbcEventStore
    implements EventStore
{
    private static final Log log = LogFactory.getLog( JdbcEventStore.class );

    private static final Map<String, String> QUERY_PARAM_COL_MAP = ImmutableMap.<String, String> builder()
        .put( "event", "psi_uid" ).put( "program", "p_uid" ).put( "programStage", "ps_uid" )
        .put( "enrollment", "pi_uid" ).put( "enrollmentStatus", "pi_status" ).put( "orgUnit", "ou_uid" )
        .put( "orgUnitName", "ou_name" ).put( "trackedEntityInstance", "tei_uid" )
        .put( "eventDate", "psi_executiondate" ).put( "followup", "pi_followup" ).put( "status", "psi_status" )
        .put( "dueDate", "psi_duedate" ).put( "storedBy", "psi_storedby" ).put( "created", "psi_created" )
        .put( "lastUpdated", "psi_lastupdated" ).put( "completedBy", "psi_completedby" )
        .put( "attributeOptionCombo", "psi_aoc" ).put( "completedDate", "psi_completeddate" )
        .put( "deleted", "psi_deleted" ).build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private StatementBuilder statementBuilder;

    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // -------------------------------------------------------------------------
    // EventStore implementation
    // -------------------------------------------------------------------------

    @Override
    public List<Event> getEvents( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        List<Event> events = new ArrayList<>();

        String sql = buildSql( params, organisationUnits );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        log.debug( "Event query SQL: " + sql );

        Event event = new Event();

        event.setEvent( "not_valid" );

        Set<String> notes = new HashSet<>();

        IdSchemes idSchemes = ObjectUtils.firstNonNull( params.getIdSchemes(), new IdSchemes() );

        while ( rowSet.next() )
        {
            if ( rowSet.getString( "psi_uid" ) == null )
            {
                continue;
            }

            if ( !event.getEvent().equals( rowSet.getString( "psi_uid" ) ) )
            {
                event = new Event();

                event.setEvent( rowSet.getString( "psi_uid" ) );
                event.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );
                event.setStatus( EventStatus.valueOf( rowSet.getString( "psi_status" ) ) );

                event.setProgram( IdSchemes.getValue( rowSet.getString( "p_uid" ), rowSet.getString( "p_code" ),
                    idSchemes.getProgramIdScheme() ) );
                event.setProgramStage( IdSchemes.getValue( rowSet.getString( "ps_uid" ), rowSet.getString( "ps_code" ),
                    idSchemes.getProgramStageIdScheme() ) );
                event.setOrgUnit( IdSchemes.getValue( rowSet.getString( "ou_uid" ), rowSet.getString( "ou_code" ),
                    idSchemes.getOrgUnitIdScheme() ) );
                event.setDeleted( rowSet.getBoolean( "psi_deleted" ) );

                ProgramType programType = ProgramType.fromValue( rowSet.getString( "p_type" ) );

                if ( programType != ProgramType.WITHOUT_REGISTRATION )
                {
                    event.setEnrollment( rowSet.getString( "pi_uid" ) );
                    event.setEnrollmentStatus( EnrollmentStatus
                        .fromProgramStatus( ProgramStatus.valueOf( rowSet.getString( "pi_status" ) ) ) );
                    event.setFollowup( rowSet.getBoolean( "pi_followup" ) );
                }

                event.setAttributeOptionCombo( rowSet.getString( "coc_categoryoptioncombouid" ) );
                event.setAttributeCategoryOptions( rowSet.getString( "deco_uid" ) );
                event.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );

                event.setStoredBy( rowSet.getString( "psi_storedby" ) );
                event.setOrgUnitName( rowSet.getString( "ou_name" ) );
                event.setDueDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_duedate" ) ) );
                event.setEventDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_executiondate" ) ) );
                event.setCreated( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_created" ) ) );
                event.setLastUpdated( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_lastupdated" ) ) );

                event.setCompletedBy( rowSet.getString( "psi_completedby" ) );
                event.setCompletedDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_completeddate" ) ) );

                Double longitude = rowSet.getDouble( "psi_longitude" );
                Double latitude = rowSet.getDouble( "psi_latitude" );

                if ( longitude != null && latitude != null )
                {
                    Coordinate coordinate = new Coordinate( longitude, latitude );

                    try
                    {
                        List<Double> list = OBJECT_MAPPER.readValue( coordinate.getCoordinateString(),
                            new TypeReference<List<Double>>()
                            {
                            } );

                        coordinate.setLongitude( list.get( 0 ) );
                        coordinate.setLatitude( list.get( 1 ) );
                    }
                    catch ( IOException ignored )
                    {
                    }

                    if ( coordinate.isValid() )
                    {
                        event.setCoordinate( coordinate );
                    }
                }
                
                boolean deleted = rowSet.getBoolean( "psi_deleted" );
                
                if ( deleted )
                {
                    event.setDeleted( deleted );
                }

                events.add( event );
            }
            else
            {
                String attributeCategoryCombination = event.getAttributeCategoryOptions();
                String currentAttributeCategoryCombination = rowSet.getString( "deco_uid" );

                if ( !attributeCategoryCombination.contains( currentAttributeCategoryCombination ) )
                {
                    event.setAttributeCategoryOptions(
                        attributeCategoryCombination + ";" + currentAttributeCategoryCombination );
                }
            }

            if ( rowSet.getString( "pdv_value" ) != null && rowSet.getString( "de_uid" ) != null
                && isNewDataValue( rowSet, event.getDataValues() ) )
            {
                DataValue dataValue = new DataValue();
                dataValue.setCreated( DateUtils.getIso8601NoTz( rowSet.getDate( "pdv_created" ) ) );
                dataValue.setLastUpdated( DateUtils.getIso8601NoTz( rowSet.getDate( "pdv_lastupdated" ) ) );
                dataValue.setValue( rowSet.getString( "pdv_value" ) );
                dataValue.setProvidedElsewhere( rowSet.getBoolean( "pdv_providedelsewhere" ) );
                dataValue.setDataElement( IdSchemes.getValue( rowSet.getString( "de_uid" ),
                    rowSet.getString( "de_code" ), idSchemes.getDataElementIdScheme() ) );

                dataValue.setStoredBy( rowSet.getString( "pdv_storedby" ) );

                event.getDataValues().add( dataValue );
            }

            if ( rowSet.getString( "psinote_value" ) != null && !notes.contains( rowSet.getString( "psinote_id" ) ) )
            {
                Note note = new Note();
                note.setValue( rowSet.getString( "psinote_value" ) );
                note.setStoredDate( rowSet.getString( "psinote_storeddate" ) );
                note.setStoredBy( rowSet.getString( "psinote_storedby" ) );

                event.getNotes().add( note );
                notes.add( rowSet.getString( "psinote_id" ) );
            }
        }

        return events;
    }

    @Override
    public List<Map<String, String>> getEventsGrid( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        SqlHelper hlp = new SqlHelper();

        // ---------------------------------------------------------------------
        // Select clause
        // ---------------------------------------------------------------------

        String sql = "select psi.uid as " + EVENT_ID + ", " + "psi.created as " + EVENT_CREATED_ID + ", "
            + "psi.lastupdated as " + EVENT_LAST_UPDATED_ID + ", " + "psi.storedby as " + EVENT_STORED_BY_ID + ", "
            + "psi.completedby as " + EVENT_COMPLETED_BY_ID + ", " + "psi.completeddate as " + EVENT_COMPLETED_DATE_ID
            + ", " + "psi.duedate as " + EVENT_DUE_DATE_ID + ", " + "psi.executiondate as " + EVENT_EXECUTION_DATE_ID
            + ", " + "ou.uid as " + EVENT_ORG_UNIT_ID + ", " + "ou.name as " + EVENT_ORG_UNIT_NAME + ", "
            + "psi.status as " + EVENT_STATUS_ID + ", " + "psi.longitude as " + EVENT_LONGITUDE_ID + ", "
            + "psi.latitude as " + EVENT_LATITUDE_ID + ", " + "ps.uid as " + EVENT_PROGRAM_STAGE_ID + ", " + "p.uid as "
            + EVENT_PROGRAM_ID + ", " + "coc.uid as " + EVENT_ATTRIBUTE_OPTION_COMBO_ID + ", " + "psi.deleted as " + EVENT_DELETED + ", ";

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            String col = statementBuilder.columnQuote( item.getItemId() );

            sql += item.isNumeric() ? "CAST( " + col + ".value AS NUMERIC ) as " : col + ".value as ";

            sql += col + ", ";
        }

        sql = removeLastComma( sql ) + " ";

        // ---------------------------------------------------------------------
        // From and where clause
        // ---------------------------------------------------------------------

        sql += getFromWhereClause( params, hlp, organisationUnits );

        // ---------------------------------------------------------------------
        // Order clause
        // ---------------------------------------------------------------------

        sql += getGridOrderQuery( params );

        // ---------------------------------------------------------------------
        // Paging clause
        // ---------------------------------------------------------------------

        sql += getEventPagingQuery( params );

        // ---------------------------------------------------------------------
        // Query
        // ---------------------------------------------------------------------

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        log.debug( "Event query SQL: " + sql );
        
        List<Map<String, String>> list = new ArrayList<>();

        while ( rowSet.next() )
        {
            final Map<String, String> map = new HashMap<>();

            for ( String col : STATIC_EVENT_COLUMNS )
            {
                map.put( col, rowSet.getString( col ) );
            }

            for ( QueryItem item : params.getDataElements() )
            {
                map.put( item.getItemId(), rowSet.getString( item.getItemId() ) );
            }

            list.add( map );
        }

        return list;
    }

    @Override
    public List<EventRow> getEventRows( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        List<EventRow> eventRows = new ArrayList<>();

        String sql = buildSql( params, organisationUnits );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        log.debug( "Event query SQL: " + sql );

        EventRow eventRow = new EventRow();

        eventRow.setEvent( "not_valid" );

        Set<String> notes = new HashSet<>();

        IdSchemes idSchemes = ObjectUtils.firstNonNull( params.getIdSchemes(), new IdSchemes() );

        while ( rowSet.next() )
        {
            if ( rowSet.getString( "psi_uid" ) == null )
            {
                continue;
            }

            if ( !eventRow.getEvent().equals( rowSet.getString( "psi_uid" ) ) )
            {
                eventRow = new EventRow();

                eventRow.setEvent( rowSet.getString( "psi_uid" ) );
                eventRow.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );
                eventRow.setTrackedEntityInstanceOrgUnit( rowSet.getString( "tei_ou" ) );
                eventRow.setTrackedEntityInstanceOrgUnitName( rowSet.getString( "tei_ou_name" ) );
                eventRow.setTrackedEntityInstanceCreated( rowSet.getString( "tei_created" ) );
                eventRow.setTrackedEntityInstanceInactive( rowSet.getBoolean( "tei_inactive" ) );
                eventRow.setDeleted( rowSet.getBoolean( "psi_deleted" ) );


                eventRow.setProgram( IdSchemes.getValue( rowSet.getString( "p_uid" ), rowSet.getString( "p_code" ),
                    idSchemes.getProgramIdScheme() ) );
                eventRow.setProgramStage( IdSchemes.getValue( rowSet.getString( "ps_uid" ),
                    rowSet.getString( "ps_code" ), idSchemes.getProgramStageIdScheme() ) );
                eventRow.setOrgUnit( IdSchemes.getValue( rowSet.getString( "ou_uid" ), rowSet.getString( "ou_code" ),
                    idSchemes.getOrgUnitIdScheme() ) );

                ProgramType programType = ProgramType.fromValue( rowSet.getString( "p_type" ) );
                if ( programType == ProgramType.WITHOUT_REGISTRATION )
                {
                    eventRow.setEnrollment( rowSet.getString( "pi_uid" ) );
                    eventRow.setFollowup( rowSet.getBoolean( "pi_followup" ) );
                }

                eventRow.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );
                eventRow.setOrgUnitName( rowSet.getString( "ou_name" ) );
                eventRow.setDueDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_duedate" ) ) );
                eventRow.setEventDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_executiondate" ) ) );

                eventRows.add( eventRow );
            }

            if ( rowSet.getString( "pav_value" ) != null && rowSet.getString( "ta_uid" ) != null )
            {
                String valueType = rowSet.getString( "ta_valuetype" );

                Attribute attribute = new Attribute();
                attribute.setCreated( DateUtils.getIso8601NoTz( rowSet.getDate( "pav_created" ) ) );
                attribute.setLastUpdated( DateUtils.getIso8601NoTz( rowSet.getDate( "pav_lastupdated" ) ) );
                attribute.setValue( rowSet.getString( "pav_value" ) );
                attribute.setDisplayName( rowSet.getString( "ta_name" ) );
                attribute.setValueType( valueType != null ? ValueType.valueOf( valueType.toUpperCase() ) : null );
                attribute.setAttribute( rowSet.getString( "ta_uid" ) );

                eventRow.getAttributes().add( attribute );
            }

            if ( rowSet.getString( "pdv_value" ) != null && rowSet.getString( "de_uid" ) != null )
            {
                DataValue dataValue = new DataValue();
                dataValue.setCreated( DateUtils.getIso8601NoTz( rowSet.getDate( "pdv_created" ) ) );
                dataValue.setLastUpdated( DateUtils.getIso8601NoTz( rowSet.getDate( "pdv_lastupdated" ) ) );
                dataValue.setValue( rowSet.getString( "pdv_value" ) );
                dataValue.setProvidedElsewhere( rowSet.getBoolean( "pdv_providedelsewhere" ) );
                dataValue.setDataElement( IdSchemes.getValue( rowSet.getString( "de_uid" ),
                    rowSet.getString( "de_code" ), idSchemes.getDataElementIdScheme() ) );

                dataValue.setStoredBy( rowSet.getString( "pdv_storedby" ) );

                eventRow.getDataValues().add( dataValue );
            }

            if ( rowSet.getString( "psinote_value" ) != null && !notes.contains( rowSet.getString( "psinote_id" ) ) )
            {
                Note note = new Note();
                note.setValue( rowSet.getString( "psinote_value" ) );
                note.setStoredDate( rowSet.getString( "psinote_storeddate" ) );
                note.setStoredBy( rowSet.getString( "psinote_storedby" ) );

                eventRow.getNotes().add( note );
                notes.add( rowSet.getString( "psinote_id" ) );
            }
        }

        return eventRows;
    }

    @Override
    public int getEventCount( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        String sql = getEventSelectQuery( params, organisationUnits );

        sql = sql.replaceFirst( "select .*? from", "select count(*) from" );

        log.debug( "Event query count SQL: " + sql );

        return jdbcTemplate.queryForObject( sql, Integer.class );
    }

    private boolean isNewDataValue( SqlRowSet rowSet, List<DataValue> dataValues )
    {
        String currentDataElementUid = rowSet.getString( "de_uid" );

        for ( DataValue dv : dataValues )
        {
            if ( dv.getDataElement().equals( currentDataElementUid ) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Query is based on three sub queries on event, data value and comment,
     * which are joined using program stage instance id. The purpose of the
     * separate queries is to be able to page properly on events.
     */
    private String buildSql( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        String sql = "select * from (";

        sql += getEventSelectQuery( params, organisationUnits );

        sql += getOrderQuery( params.getOrders() );

        sql += getEventPagingQuery( params );

        sql += ") as event left join (";

        if ( params.isIncludeAttributes() )
        {
            sql += getAttributeValueQuery();

            sql += ") as att on event.tei_id=att.pav_id left join (";
        }

        sql += getDataValueQuery();

        sql += ") as dv on event.psi_id=dv.pdv_id left join (";

        sql += getCommentQuery();

        sql += ") as cm on event.psi_id=cm.psic_id ";

        sql += getOrderQuery( params.getOrders() );

        return sql;
    }

    private String getEventSelectQuery( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        List<Integer> orgUnitIds = getIdentifiers( organisationUnits );

        SqlHelper hlp = new SqlHelper();

        String sql = "select psi.programstageinstanceid as psi_id, psi.uid as psi_uid, psi.status as psi_status, psi.executiondate as psi_executiondate, "
            + "psi.duedate as psi_duedate, psi.completedby as psi_completedby, psi.storedby as psi_storedby, psi.longitude as psi_longitude, " 
            + "psi.latitude as psi_latitude, psi.created as psi_created, psi.lastupdated as psi_lastupdated, psi.completeddate as psi_completeddate, psi.deleted as psi_deleted, "
            + "coc.code AS coc_categoryoptioncombocode, coc.uid AS coc_categoryoptioncombouid, cocco.categoryoptionid AS cocco_categoryoptionid, "
            + "deco.uid AS deco_uid, pi.uid as pi_uid, pi.status as pi_status, pi.followup as pi_followup, p.uid as p_uid, p.code as p_code, "
            + "p.type as p_type, ps.uid as ps_uid, ps.code as ps_code, ps.capturecoordinates as ps_capturecoordinates, "
            + "ou.uid as ou_uid, ou.code as ou_code, ou.name as ou_name, "
            + "tei.trackedentityinstanceid as tei_id, tei.uid as tei_uid, teiou.uid as tei_ou, teiou.name as tei_ou_name, tei.created as tei_created, tei.inactive as tei_inactive, "
            + "psi.deleted as psi_deleted "
            + "from programstageinstance psi "
            + "inner join programinstance pi on pi.programinstanceid=psi.programinstanceid "
            + "inner join program p on p.programid=pi.programid "
            + "inner join programstage ps on ps.programstageid=psi.programstageid "
            + "INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid=psi.attributeoptioncomboid "
            + "INNER JOIN categoryoptioncombos_categoryoptions cocco ON psi.attributeoptioncomboid=cocco.categoryoptioncomboid "
            + "INNER JOIN dataelementcategoryoption deco ON cocco.categoryoptionid=deco.categoryoptionid "
            + "left join trackedentityinstance tei on tei.trackedentityinstanceid=pi.trackedentityinstanceid "
            + "left join organisationunit ou on (psi.organisationunitid=ou.organisationunitid) "
            + "left join organisationunit teiou on (tei.organisationunitid=teiou.organisationunitid) ";

        if ( params.getTrackedEntityInstance() != null )
        {
            sql += hlp.whereAnd() + " tei.trackedentityinstanceid=" + params.getTrackedEntityInstance().getId() + " ";
        }

        if ( params.getProgram() != null )
        {
            sql += hlp.whereAnd() + " p.programid = " + params.getProgram().getId() + " ";
        }

        if ( params.getProgramStage() != null )
        {
            sql += hlp.whereAnd() + " ps.programstageid = " + params.getProgramStage().getId() + " ";
        }

        if ( params.getProgramStatus() != null )
        {
            sql += hlp.whereAnd() + " pi.status = '" + params.getProgramStatus() + "' ";
        }

        if ( params.getFollowUp() != null )
        {
            sql += hlp.whereAnd() + " pi.followup is " + (params.getFollowUp() ? "true" : "false") + " ";
        }

        if ( params.getLastUpdatedStartDate() != null )
        {
            sql += hlp.whereAnd() + " psi.lastupdated >= '"
                + DateUtils.getLongDateString( params.getLastUpdatedStartDate() ) + "' ";
        }

        if ( params.getLastUpdatedEndDate() != null )
        {
            Date dateAfterEndDate = getDateAfterAddition( params.getLastUpdatedEndDate(), 1 );
            sql += hlp.whereAnd() + " psi.lastupdated < '" + DateUtils.getLongDateString( dateAfterEndDate ) + "' ";
        }

        if ( params.getCategoryOptionCombo() != null )
        {
            sql += hlp.whereAnd() + " psi.attributeoptioncomboid = " + params.getCategoryOptionCombo().getId() + " ";
        }

        if ( orgUnitIds != null && !orgUnitIds.isEmpty() )
        {
            sql += hlp.whereAnd() + " psi.organisationunitid in (" + getCommaDelimitedString( orgUnitIds ) + ") ";
        }

        if ( params.getStartDate() != null )
        {
            sql += hlp.whereAnd() + " (psi.executiondate >= '" + getMediumDateString( params.getStartDate() ) + "' "
                + "or (psi.executiondate is null and psi.duedate >= '" + getMediumDateString( params.getStartDate() )
                + "')) ";
        }

        if ( params.getEndDate() != null )
        {
            Date dateAfterEndDate = getDateAfterAddition( params.getEndDate(), 1 );
            sql += hlp.whereAnd() + " (psi.executiondate < '" + getMediumDateString( dateAfterEndDate ) + "' "
                + "or (psi.executiondate is null and psi.duedate < '" + getMediumDateString( dateAfterEndDate )
                + "')) ";
        }

        if ( params.getProgramType() != null )
        {
            sql += hlp.whereAnd() + " p.type = '" + params.getProgramType() + "' ";
        }

        if ( params.getEventStatus() != null )
        {
            if ( params.getEventStatus() == EventStatus.VISITED )
            {
                sql += hlp.whereAnd() + " psi.status = '" + EventStatus.ACTIVE.name()
                    + "' and psi.executiondate is not null ";
            }
            else if ( params.getEventStatus() == EventStatus.OVERDUE )
            {
                sql += hlp.whereAnd() + " date(now()) > date(psi.duedate) and psi.status = '"
                    + EventStatus.SCHEDULE.name() + "' ";
            }
            else
            {
                sql += hlp.whereAnd() + " psi.status = '" + params.getEventStatus().name() + "' ";
            }
        }

        if ( params.getEvents() != null && !params.getEvents().isEmpty() && !params.hasFilters() )
        {
            sql += hlp.whereAnd() + " (psi.uid in (" + getQuotedCommaDelimitedString( params.getEvents() ) + ")) ";
        }

        if ( !params.isIncludeDeleted() )
        {
            sql += hlp.whereAnd() + " psi.deleted is false ";
        }

        return sql;
    }

    /**
     * From, join and where clause. For dataElement params, restriction is set
     * in inner join. For query params, restriction is set in where clause.
     */
    private String getFromWhereClause( EventSearchParams params, SqlHelper hlp,
        List<OrganisationUnit> organisationUnits )
    {
        String sql = "from programstageinstance psi "
            + "inner join programinstance pi on pi.programinstanceid = psi.programinstanceid "
            + "inner join program p on p.programid = pi.programid "
            + "inner join programstage ps on ps.programstageid = psi.programstageid "
            + "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = psi.attributeoptioncomboid "
            + "inner join organisationunit ou on psi.organisationunitid = ou.organisationunitid ";

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            final String col = statementBuilder.columnQuote( item.getItemId() );

            final String joinClause = item.hasFilter() ? "inner join" : "left join";

            sql += joinClause + " " + "trackedentitydatavalue as " + col + " " + "on " + col
                + ".programstageinstanceid = psi.programstageinstanceid " + "and " + col + ".dataelementid = "
                + item.getItem().getId() + " ";

            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    final String encodedFilter = statementBuilder.encode( filter.getFilter(), false );

                    final String queryCol = item.isNumeric() ? " CAST( " + (col + ".value AS NUMERIC)")
                        : "lower(" + col + ".value)";

                    sql += "and " + queryCol + " " + filter.getSqlOperator() + " "
                        + StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) + " ";
                }
            }
        }

        if ( organisationUnits != null && !organisationUnits.isEmpty() )
        {
            sql += hlp.whereAnd() + " psi.organisationunitid in ("
                + getCommaDelimitedString( getIdentifiers( organisationUnits ) ) + ") ";
        }

        if ( params.getProgramStage() != null )
        {
            sql += hlp.whereAnd() + " ps.programstageid = " + params.getProgramStage().getId() + " ";
        }
        
        if ( params.getCategoryOptionCombo() != null )
        {
            sql += hlp.whereAnd() + " psi.attributeoptioncomboid = " + params.getCategoryOptionCombo().getId() + " ";
        }

        if ( params.getStartDate() != null )
        {
            sql += hlp.whereAnd() + " (psi.executiondate >= '" + getMediumDateString( params.getStartDate() ) + "' "
                + "or (psi.executiondate is null and psi.duedate >= '" + getMediumDateString( params.getStartDate() )
                + "')) ";
        }

        if ( params.getEndDate() != null )
        {
            sql += hlp.whereAnd() + " (psi.executiondate <= '" + getMediumDateString( params.getEndDate() ) + "' "
                + "or (psi.executiondate is null and psi.duedate <= '" + getMediumDateString( params.getEndDate() )
                + "')) ";
        }

        if ( params.getLastUpdatedStartDate() != null )
        {
            sql += hlp.whereAnd() + " psi.lastupdated >= '"
                + DateUtils.getLongDateString( params.getLastUpdatedStartDate() ) + "' ";
        }

        if ( params.getLastUpdatedEndDate() != null )
        {
            sql += hlp.whereAnd() + " psi.lastupdated <= '"
                + DateUtils.getLongDateString( params.getLastUpdatedEndDate() ) + "' ";
        }
        
        if ( params.getDueDateStart() != null )
        {
            sql += hlp.whereAnd() + " psi.duedate is not null and psi.duedate >= '"
                + DateUtils.getLongDateString( params.getDueDateStart() ) + "' ";
        }

        if ( params.getDueDateEnd() != null )
        {
            sql += hlp.whereAnd() + " psi.duedate is not null and psi.duedate <= '"
                + DateUtils.getLongDateString( params.getDueDateEnd() ) + "' ";
        }

        if ( !params.isIncludeDeleted() )
        {
            sql += hlp.whereAnd() + " psi.deleted is false ";
        }

        if ( params.getEventStatus() != null )
        {
            if ( params.getEventStatus() == EventStatus.VISITED )
            {
                sql += hlp.whereAnd() + " psi.status = '" + EventStatus.ACTIVE.name()
                    + "' and psi.executiondate is not null ";
            }
            else if ( params.getEventStatus() == EventStatus.OVERDUE )
            {
                sql += hlp.whereAnd() + " date(now()) > date(psi.duedate) and psi.status = '"
                    + EventStatus.SCHEDULE.name() + "' ";
            }
            else
            {
                sql += hlp.whereAnd() + " psi.status = '" + params.getEventStatus().name() + "' ";
            }
        }

        return sql;
    }

    private String getEventPagingQuery( EventSearchParams params )
    {
        String sql = " ";

        if ( params.isPaging() )
        {
            sql += "limit " + params.getPageSizeWithDefault() + " offset " + params.getOffset() + " ";
        }

        return sql;
    }

    private String getDataValueQuery()
    {
        String sql = "select pdv.programstageinstanceid as pdv_id, pdv.created as pdv_created, pdv.lastupdated as pdv_lastupdated, "
            + "pdv.value as pdv_value, pdv.storedby as pdv_storedby, pdv.providedelsewhere as pdv_providedelsewhere, "
            + "de.uid as de_uid, de.code as de_code " + "from trackedentitydatavalue pdv "
            + "inner join dataelement de on pdv.dataelementid=de.dataelementid ";

        return sql;
    }

    private String getCommentQuery()
    {
        String sql = "select psic.programstageinstanceid as psic_id, psinote.trackedentitycommentid as psinote_id, psinote.commenttext as psinote_value, "
            + "psinote.createddate as psinote_storeddate, psinote.creator as psinote_storedby "
            + "from programstageinstancecomments psic "
            + "inner join trackedentitycomment psinote on psic.trackedentitycommentid=psinote.trackedentitycommentid ";

        return sql;
    }

    private String getGridOrderQuery( EventSearchParams params )
    {

        if ( params.getGridOrders() != null && params.getDataElements() != null && !params.getDataElements().isEmpty()
            && STATIC_EVENT_COLUMNS != null && !STATIC_EVENT_COLUMNS.isEmpty() )
        {
            ArrayList<String> orderFields = new ArrayList<String>();

            for ( String order : params.getGridOrders() )
            {
                String[] prop = order.split( ":" );

                if ( prop.length == 2 && (prop[1].equals( "desc" ) || prop[1].equals( "asc" )) )
                {
                    if ( STATIC_EVENT_COLUMNS.contains( prop[0] ) )
                    {
                        orderFields.add( prop[0] + " " + prop[1] );
                    }
                    else
                    {
                        Iterator<QueryItem> itermIterator = params.getDataElements().iterator();

                        while ( itermIterator.hasNext() )
                        {
                            QueryItem item = itermIterator.next();

                            if ( prop[0].equals( item.getItemId() ) )
                            {
                                orderFields.add( statementBuilder.columnQuote( prop[0] ) + " " + prop[1] );
                                break;
                            }
                        }
                    }
                }

            }

            if ( !orderFields.isEmpty() )
            {
                return "order by " + StringUtils.join( orderFields, ',' );
            }
        }

        return "order by lastUpdated desc ";
    }

    private String getOrderQuery( List<Order> orders )
    {
        if ( orders != null )
        {
            ArrayList<String> orderFields = new ArrayList<String>();

            for ( Order order : orders )
            {
                if ( QUERY_PARAM_COL_MAP.containsKey( order.getProperty().getName() ) )
                {
                    String orderText = QUERY_PARAM_COL_MAP.get( order.getProperty().getName() );
                    orderText += order.isAscending() ? " asc" : " desc";
                    orderFields.add( orderText );
                }
            }

            if ( !orderFields.isEmpty() )
            {
                return "order by " + StringUtils.join( orderFields, ',' ) + " ";
            }
        }

        return "order by psi_lastupdated desc ";
    }

    private String getAttributeValueQuery()
    {
        String sql = "select pav.trackedentityinstanceid as pav_id, pav.created as pav_created, pav.lastupdated as pav_lastupdated, "
            + "pav.value as pav_value, ta.uid as ta_uid, ta.name as ta_name, ta.valuetype as ta_valuetype "
            + "from trackedentityattributevalue pav "
            + "inner join trackedentityattribute ta on pav.trackedentityattributeid=ta.trackedentityattributeid ";

        return sql;
    }
}
