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
package org.hisp.dhis.dxf2.events.event;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringEscapeUtils.escapeSql;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.splitToArray;
import static org.hisp.dhis.dxf2.events.event.AbstractEventService.STATIC_EVENT_COLUMNS;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ATTRIBUTE_OPTION_COMBO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_COMPLETED_BY_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_COMPLETED_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_CREATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_CREATED_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_DELETED;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_DUE_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ENROLLMENT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_EXECUTION_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_GEOMETRY;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_LAST_UPDATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_LAST_UPDATED_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ORG_UNIT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ORG_UNIT_NAME;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_PROGRAM_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_PROGRAM_STAGE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_STATUS_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_STORED_BY_ID;
import static org.hisp.dhis.dxf2.events.event.EventUtils.eventDataValuesToJson;
import static org.hisp.dhis.dxf2.events.event.EventUtils.jsonToUserInfo;
import static org.hisp.dhis.dxf2.events.event.EventUtils.userInfoToJson;
import static org.hisp.dhis.system.util.SqlUtils.castToNumber;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.util.DateUtils.getDateAfterAddition;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jdbc.BatchPreparedStatementSetterWithKeyHolder;
import org.hisp.dhis.jdbc.JdbcUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository( "org.hisp.dhis.dxf2.events.event.EventStore" )
public class JdbcEventStore implements EventStore
{
    private static final String RELATIONSHIP_IDS_QUERY = " left join (select ri.programstageinstanceid as ri_psi_id, json_agg(ri.relationshipid) as psi_rl FROM relationshipitem ri"
        + " GROUP by ri_psi_id)  as fgh on fgh.ri_psi_id=event.psi_id ";

    private static final String PSI_EVENT_COMMENT_QUERY = "select psic.programstageinstanceid    as psic_id," +
        " psinote.trackedentitycommentid as psinote_id," +
        " psinote.commenttext            as psinote_value," +
        " psinote.created                as psinote_storeddate," +
        " psinote.creator                as psinote_storedby," +
        " psinote.uid                    as psinote_uid," +
        " psinote.lastupdated            as psinote_lastupdated," +
        " usernote.userid                as usernote_id," +
        " usernote.code                  as usernote_code," +
        " usernote.uid                   as usernote_uid," +
        " usernote.username              as usernote_username," +
        " userinfo.firstname             as userinfo_firstname," +
        " userinfo.surname               as userinfo_surname" +
        " from programstageinstancecomments psic" +
        " inner join trackedentitycomment psinote" +
        " on psic.trackedentitycommentid = psinote.trackedentitycommentid" +
        " left join users usernote on psinote.lastupdatedby = usernote.userid" +
        " left join userinfo on usernote.userid = userinfo.userinfoid";

    private static final String PSI_STATUS_EQ = " psi.status = '";

    private static final String PSI_LASTUPDATED_GT = " psi.lastupdated >= '";

    private static final String DOT_NAME = ".name)";

    private static final Map<String, String> QUERY_PARAM_COL_MAP = ImmutableMap.<String, String> builder()
        .put( "event", "psi_uid" ).put( "program", "p_uid" ).put( "programStage", "ps_uid" )
        .put( "enrollment", "pi_uid" ).put( "enrollmentStatus", "pi_status" ).put( "orgUnit", "ou_uid" )
        .put( "orgUnitName", "ou_name" ).put( "trackedEntityInstance", "tei_uid" )
        .put( "eventDate", "psi_executiondate" ).put( "followup", "pi_followup" ).put( "status", "psi_status" )
        .put( "dueDate", "psi_duedate" ).put( "storedBy", "psi_storedby" )
        .put( "lastUpdatedByUserInfo", "psi_lastupdatedbyuserinfo" ).put( "createdByUserInfo", "psi_createdbyuserinfo" )
        .put( "created", "psi_created" )
        .put( "lastUpdated", "psi_lastupdated" ).put( "completedBy", "psi_completedby" )
        .put( "attributeOptionCombo", "psi_aoc" ).put( "completedDate", "psi_completeddate" )
        .put( "deleted", "psi_deleted" ).put( "assignedUser", "user_assigned_username" )
        .put( "assignedUserDisplayName", "user_assigned_name" ).build();

    // SQL QUERIES

    private final static String INSERT_EVENT_SQL = "insert into programstageinstance (" +
    // @formatter:off
        "programstageinstanceid, " +    // 0
        "programinstanceid, " +         // 1
        "programstageid, " +            // 2
        "duedate, " +                   // 3
        "executiondate, " +             // 4
        "organisationunitid, " +        // 5
        "status, " +                    // 6
        "completeddate, " +             // 7
        "uid, " +                       // 8
        "created, " +                   // 9
        "lastupdated, " +               // 10
        "attributeoptioncomboid, " +    // 11
        "storedby, " +                  // 12
        "createdbyuserinfo, " +         // 13
        "lastupdatedbyuserinfo, " +     // 14
        "completedby, " +               // 15
        "deleted, " +                   // 16
        "code, " +                      // 17
        "createdatclient, " +           // 18
        "lastupdatedatclient, " +       // 19
        "geometry, " +                  // 20
        "assigneduserid, " +            // 21
        "eventdatavalues) " +           // 22
        // @formatter:on
        "values ( nextval('programstageinstance_sequence'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

    private final static String UPDATE_EVENT_SQL = "update programstageinstance set " +
    // @formatter:off
        "programinstanceid = ?, " +         // 1
        "programstageid = ?, " +            // 2
        "duedate = ?, " +                   // 3
        "executiondate = ?, " +             // 4
        "organisationunitid = ?, " +        // 5
        "status = ?, " +                    // 6
        "completeddate = ?, " +             // 7
        "lastupdated = ?, " +               // 8
        "attributeoptioncomboid = ?, " +    // 9
        "storedby = ?, " +                  // 10
        "lastupdatedbyuserinfo = ?, " +     // 11
        "completedby = ?, " +               // 12
        "deleted = ?, " +                   // 13
        "code = ?, " +                      // 14
        "createdatclient = ?, " +           // 15
        "lastupdatedatclient = ?, " +       // 16
        "geometry = ?, " +                  // 17
        "assigneduserid = ?, " +            // 18
        "eventdatavalues = ? " +            // 19
        "where uid = ?;";                   // 20
    // @formatter:on

    /**
     * Updates Tracked Entity Instance after an event update. In order to
     * prevent deadlocks, SELECT ... FOR UPDATE SKIP LOCKED is used before the
     * actual UPDATE statement. This prevents deadlocks when Postgres tries to
     * update the same TEI.
     */
    private static final String UPDATE_TEI_SQL = "SELECT * FROM trackedentityinstance where uid in (%s) FOR UPDATE %s;"
        +
        "update trackedentityinstance set lastupdated = %s, lastupdatedby = %s where uid in (%s)";

    private static final String NULL = "null";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    // Cannot use DefaultRenderService mapper. Does not work properly -
    // DHIS2-6102
    private static final ObjectReader eventDataValueJsonReader = JsonEventDataValueSetBinaryType.MAPPER
        .readerFor( new TypeReference<Map<String, EventDataValue>>()
        {
        } );

    private final StatementBuilder statementBuilder;

    private final JdbcTemplate jdbcTemplate;

    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager manager;

    private final ObjectMapper jsonMapper;

    private final Environment env;

    private final org.hisp.dhis.dxf2.events.trackedentity.store.EventStore eventStore;

    public JdbcEventStore( StatementBuilder statementBuilder, JdbcTemplate jdbcTemplate,
        @Qualifier( "dataValueJsonMapper" ) ObjectMapper jsonMapper,
        CurrentUserService currentUserService, IdentifiableObjectManager identifiableObjectManager, Environment env,
        org.hisp.dhis.dxf2.events.trackedentity.store.EventStore eventStore )
    {
        checkNotNull( statementBuilder );
        checkNotNull( jdbcTemplate );
        checkNotNull( currentUserService );
        checkNotNull( identifiableObjectManager );
        checkNotNull( jsonMapper );
        checkNotNull( env );
        checkNotNull( eventStore );

        this.statementBuilder = statementBuilder;
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.manager = identifiableObjectManager;
        this.jsonMapper = jsonMapper;
        this.env = env;
        this.eventStore = eventStore;

    }

    // -------------------------------------------------------------------------
    // EventStore implementation
    // -------------------------------------------------------------------------

    @Override
    public List<Event> getEvents( EventSearchParams params, List<OrganisationUnit> organisationUnits,
        Map<String, Set<String>> psdesWithSkipSyncTrue )
    {
        User user = currentUserService.getCurrentUser();

        setAccessiblePrograms( user, params );

        Map<String, Event> eventUidToEventMap = new HashMap<>( params.getPageSizeWithDefault() );
        List<Event> events = new ArrayList<>();
        List<Long> relationshipIds = new ArrayList<>();

        final Gson gson = new Gson();

        String sql = buildSql( params, organisationUnits, user );
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        log.debug( "Event query SQL: " + sql );

        Set<String> notes = new HashSet<>();

        while ( rowSet.next() )
        {
            if ( rowSet.getString( "psi_uid" ) == null
                || (params.getCategoryOptionCombo() == null && !isSuper( user ) && !userHasAccess( rowSet )) )
            {
                continue;
            }

            String psiUid = rowSet.getString( "psi_uid" );

            Event event;

            if ( !eventUidToEventMap.containsKey( psiUid ) )
            {
                validateIdentifiersPresence( rowSet, params.getIdSchemes(), true );

                event = new Event();
                eventUidToEventMap.put( psiUid, event );

                if ( !params.isSkipEventId() )
                {
                    event.setUid( psiUid );
                    event.setEvent( psiUid );
                }

                event.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );
                event.setStatus( EventStatus.valueOf( rowSet.getString( "psi_status" ) ) );

                ProgramType programType = ProgramType.fromValue( rowSet.getString( "p_type" ) );

                event.setProgram( rowSet.getString( "p_identifier" ) );
                event.setProgramType( programType );
                event.setProgramStage( rowSet.getString( "ps_identifier" ) );
                event.setOrgUnit( rowSet.getString( "ou_identifier" ) );
                event.setDeleted( rowSet.getBoolean( "psi_deleted" ) );

                if ( programType != ProgramType.WITHOUT_REGISTRATION )
                {
                    event.setEnrollment( rowSet.getString( "pi_uid" ) );
                    event.setEnrollmentStatus( EnrollmentStatus
                        .fromProgramStatus( ProgramStatus.valueOf( rowSet.getString( "pi_status" ) ) ) );
                    event.setFollowup( rowSet.getBoolean( "pi_followup" ) );
                }

                if ( params.getCategoryOptionCombo() == null && !isSuper( user ) )
                {
                    event.setOptionSize( rowSet.getInt( "option_size" ) );
                }

                event.setAttributeOptionCombo( rowSet.getString( "coc_identifier" ) );
                event.setAttributeCategoryOptions( rowSet.getString( "deco_uid" ) );
                event.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );

                event.setStoredBy( rowSet.getString( "psi_storedby" ) );
                event.setOrgUnitName( rowSet.getString( "ou_name" ) );
                event.setDueDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_duedate" ) ) );
                event.setEventDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_executiondate" ) ) );
                event.setCreated( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_created" ) ) );
                event.setCreatedByUserInfo( jsonToUserInfo( rowSet.getString( "psi_createdbyuserinfo" ), jsonMapper ) );
                event.setLastUpdated( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_lastupdated" ) ) );
                event.setLastUpdatedByUserInfo(
                    jsonToUserInfo( rowSet.getString( "psi_lastupdatedbyuserinfo" ), jsonMapper ) );

                event.setCompletedBy( rowSet.getString( "psi_completedby" ) );
                event.setCompletedDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psi_completeddate" ) ) );

                if ( rowSet.getObject( "psi_geometry" ) != null )
                {
                    try
                    {
                        Geometry geom = new WKTReader().read( rowSet.getString( "psi_geometry" ) );

                        event.setGeometry( geom );
                    }
                    catch ( ParseException e )
                    {
                        log.error( "Unable to read geometry for event '" + event.getUid() + "': ", e );
                    }
                }

                if ( rowSet.getObject( "user_assigned" ) != null )
                {
                    event.setAssignedUser( rowSet.getString( "user_assigned" ) );
                    event.setAssignedUserUsername( rowSet.getString( "user_assigned_username" ) );
                    event.setAssignedUserDisplayName( rowSet.getString( "user_assigned_name" ) );
                }

                events.add( event );
            }
            else
            {
                event = eventUidToEventMap.get( psiUid );
                String attributeCategoryCombination = event.getAttributeCategoryOptions();
                String currentAttributeCategoryCombination = rowSet.getString( "deco_uid" );

                if ( !attributeCategoryCombination.contains( currentAttributeCategoryCombination ) )
                {
                    event.setAttributeCategoryOptions(
                        attributeCategoryCombination + ";" + currentAttributeCategoryCombination );
                }
            }

            if ( !StringUtils.isEmpty( rowSet.getString( "psi_eventdatavalues" ) ) )
            {
                Set<EventDataValue> eventDataValues = convertEventDataValueJsonIntoSet(
                    rowSet.getString( "psi_eventdatavalues" ) );

                for ( EventDataValue dv : eventDataValues )
                {
                    DataValue dataValue = convertEventDataValueIntoDtoDataValue( dv );

                    if ( params.isSynchronizationQuery() )
                    {
                        if ( psdesWithSkipSyncTrue.containsKey( rowSet.getString( "ps_uid" ) ) && psdesWithSkipSyncTrue
                            .get( rowSet.getString( "ps_uid" ) ).contains( dv.getDataElement() ) )
                        {
                            dataValue.setSkipSynchronization( true );
                        }
                        else
                        {
                            dataValue.setSkipSynchronization( false );
                        }
                    }

                    event.getDataValues().add( dataValue );
                }
            }

            if ( rowSet.getString( "psinote_value" ) != null && !notes.contains( rowSet.getString( "psinote_id" ) ) )
            {
                Note note = new Note();
                note.setNote( rowSet.getString( "psinote_uid" ) );
                note.setValue( rowSet.getString( "psinote_value" ) );
                note.setStoredDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psinote_storeddate" ) ) );
                note.setStoredBy( rowSet.getString( "psinote_storedby" ) );

                if ( rowSet.getObject( "usernote_id" ) != null )
                {

                    note.setLastUpdatedBy(
                        UserInfoSnapshot.of(
                            rowSet.getLong( "usernote_id" ),
                            rowSet.getString( "usernote_code" ),
                            rowSet.getString( "usernote_uid" ),
                            rowSet.getString( "usernote_username" ),
                            rowSet.getString( "userinfo_firstname" ),
                            rowSet.getString( "userinfo_surname" ) ) );
                }

                note.setLastUpdated( rowSet.getDate( "psinote_lastupdated" ) );

                event.getNotes().add( note );
                notes.add( rowSet.getString( "psinote_id" ) );
            }

            if ( params.isIncludeRelationships() )
            {
                if ( rowSet.getObject( "psi_rl" ) != null )
                {
                    PGobject pGobject = (PGobject) rowSet.getObject( "psi_rl" );

                    if ( pGobject != null )
                    {
                        String value = pGobject.getValue();

                        relationshipIds.addAll( Lists.newArrayList( gson.fromJson( value, Long[].class ) ) );
                    }
                }
            }
        }

        final Multimap<String, Relationship> map = eventStore
            .getRelationshipsByIds( relationshipIds );

        if ( !map.isEmpty() )
        {
            events.forEach( e -> e.getRelationships().addAll( map.get( e.getEvent() ) ) );
        }

        IdSchemes idSchemes = ObjectUtils.firstNonNull( params.getIdSchemes(), new IdSchemes() );
        IdScheme dataElementIdScheme = idSchemes.getDataElementIdScheme();

        if ( dataElementIdScheme != IdScheme.ID && dataElementIdScheme != IdScheme.UID )
        {
            CachingMap<String, String> dataElementUidToIdentifierCache = new CachingMap<>();

            List<Collection<DataValue>> dataValuesList = events.stream().map( Event::getDataValues )
                .collect( Collectors.toList() );
            populateCache( dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache );
            convertDataValuesIdentifiers( dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache );
        }

        if ( params.getCategoryOptionCombo() == null && !isSuper( user ) )
        {
            return events.stream().filter( ev -> ev.getAttributeCategoryOptions() != null
                && splitToArray( ev.getAttributeCategoryOptions(), TextUtils.SEMICOLON ).size() == ev.getOptionSize() )
                .collect( Collectors.toList() );
        }

        return events;
    }

    @Override
    public List<ProgramStageInstance> saveEvents( List<ProgramStageInstance> events )
    {
        try
        {
            return saveAllEvents( events );
        }
        catch ( Exception e )
        {
            log.error( "An error occurred saving a batch", e );
            throw e;
        }
    }

    @Override
    public List<ProgramStageInstance> updateEvents( List<ProgramStageInstance> programStageInstances )
    {
        try
        {
            jdbcTemplate.batchUpdate( UPDATE_EVENT_SQL, sort( programStageInstances ), programStageInstances.size(),
                ( ps, programStageInstance ) -> {
                    try
                    {
                        bindEventParamsForUpdate( ps, programStageInstance );
                    }
                    catch ( JsonProcessingException | SQLException e )
                    {
                        log.warn( "PSI failed to update and will be ignored. PSI UID: " + programStageInstance.getUid(),
                            programStageInstance.getUid(), e );
                    }
                } );
        }
        catch ( DataAccessException e )
        {
            log.error( "Error updating events", e );
            throw e;
        }

        return programStageInstances;
    }

    @Override
    public List<Map<String, String>> getEventsGrid( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        User user = currentUserService.getCurrentUser();

        setAccessiblePrograms( user, params );

        String sql = buildGridSql( params, organisationUnits );

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
        User user = currentUserService.getCurrentUser();

        setAccessiblePrograms( user, params );

        List<EventRow> eventRows = new ArrayList<>();

        String sql = buildSql( params, organisationUnits, user );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        log.debug( "Event query SQL: " + sql );

        EventRow eventRow = new EventRow();

        eventRow.setEvent( "not_valid" );

        Set<String> notes = new HashSet<>();

        Map<String, List<DataValue>> processedDataValues = new HashMap<>();

        while ( rowSet.next() )
        {
            if ( rowSet.getString( "psi_uid" ) == null
                || (params.getCategoryOptionCombo() == null && !isSuper( user ) && !userHasAccess( rowSet )) )
            {
                continue;
            }

            if ( eventRow.getUid() == null || !eventRow.getUid().equals( rowSet.getString( "psi_uid" ) ) )
            {
                validateIdentifiersPresence( rowSet, params.getIdSchemes(), false );

                eventRow = new EventRow();

                eventRow.setUid( rowSet.getString( "psi_uid" ) );

                eventRow.setEvent( rowSet.getString( "psi_uid" ) );
                eventRow.setTrackedEntityInstance( rowSet.getString( "tei_uid" ) );
                eventRow.setTrackedEntityInstanceOrgUnit( rowSet.getString( "tei_ou" ) );
                eventRow.setTrackedEntityInstanceOrgUnitName( rowSet.getString( "tei_ou_name" ) );
                eventRow.setTrackedEntityInstanceCreated( rowSet.getString( "tei_created" ) );
                eventRow.setTrackedEntityInstanceInactive( rowSet.getBoolean( "tei_inactive" ) );
                eventRow.setDeleted( rowSet.getBoolean( "psi_deleted" ) );

                eventRow.setProgram( rowSet.getString( "p_identifier" ) );
                eventRow.setProgramStage( rowSet.getString( "ps_identifier" ) );
                eventRow.setOrgUnit( rowSet.getString( "ou_identifier" ) );

                ProgramType programType = ProgramType.fromValue( rowSet.getString( "p_type" ) );

                if ( programType == ProgramType.WITH_REGISTRATION )
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

            if ( !StringUtils.isEmpty( rowSet.getString( "psi_eventdatavalues" ) )
                && !processedDataValues.containsKey( rowSet.getString( "psi_uid" ) ) )
            {
                List<DataValue> dataValues = new ArrayList<>();
                Set<EventDataValue> eventDataValues = convertEventDataValueJsonIntoSet(
                    rowSet.getString( "psi_eventdatavalues" ) );

                for ( EventDataValue dv : eventDataValues )
                {
                    dataValues.add( convertEventDataValueIntoDtoDataValue( dv ) );
                }
                processedDataValues.put( rowSet.getString( "psi_uid" ), dataValues );
            }

            if ( rowSet.getString( "psinote_value" ) != null && !notes.contains( rowSet.getString( "psinote_id" ) ) )
            {
                Note note = new Note();
                note.setNote( rowSet.getString( "psinote_uid" ) );
                note.setValue( rowSet.getString( "psinote_value" ) );
                note.setStoredDate( DateUtils.getIso8601NoTz( rowSet.getDate( "psinote_storeddate" ) ) );
                note.setStoredBy( rowSet.getString( "psinote_storedby" ) );

                eventRow.getNotes().add( note );
                notes.add( rowSet.getString( "psinote_id" ) );
            }
        }
        eventRows.forEach( e -> e.setDataValues( processedDataValues.get( e.getUid() ) ) );

        IdSchemes idSchemes = ObjectUtils.firstNonNull( params.getIdSchemes(), new IdSchemes() );
        IdScheme dataElementIdScheme = idSchemes.getDataElementIdScheme();

        if ( dataElementIdScheme != IdScheme.ID && dataElementIdScheme != IdScheme.UID )
        {
            CachingMap<String, String> dataElementUidToIdentifierCache = new CachingMap<>();

            List<Collection<DataValue>> dataValuesList = eventRows.stream().map( EventRow::getDataValues )
                .collect( Collectors.toList() );
            populateCache( dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache );
            convertDataValuesIdentifiers( dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache );
        }

        return eventRows;
    }

    private String getIdSqlBasedOnIdScheme( IdScheme idScheme, String uidSql, String attributeSql, String codeSql )
    {
        if ( idScheme == IdScheme.ID || idScheme == IdScheme.UID )
        {
            return uidSql;
        }
        else if ( idScheme.isAttribute() )
        {
            return String.format( attributeSql, idScheme.getAttribute() );
        }
        else
        {
            return codeSql;
        }
    }

    private void validateIdentifiersPresence( SqlRowSet rowSet, IdSchemes idSchemes,
        boolean validateCategoryOptionCombo )
    {
        if ( StringUtils.isEmpty( rowSet.getString( "p_identifier" ) ) )
        {
            throw new IllegalStateException( String.format( "Program %s does not have a value assigned for idScheme %s",
                rowSet.getString( "p_uid" ), idSchemes.getProgramIdScheme().name() ) );
        }

        if ( StringUtils.isEmpty( rowSet.getString( "ps_identifier" ) ) )
        {
            throw new IllegalStateException(
                String.format( "ProgramStage %s does not have a value assigned for idScheme %s",
                    rowSet.getString( "ps_uid" ), idSchemes.getProgramStageIdScheme().name() ) );
        }

        if ( StringUtils.isEmpty( rowSet.getString( "ou_identifier" ) ) )
        {
            throw new IllegalStateException( String.format( "OrgUnit %s does not have a value assigned for idScheme %s",
                rowSet.getString( "ou_uid" ), idSchemes.getOrgUnitIdScheme().name() ) );
        }

        if ( validateCategoryOptionCombo && StringUtils.isEmpty( rowSet.getString( "coc_identifier" ) ) )
        {
            throw new IllegalStateException(
                String.format( "CategoryOptionCombo %s does not have a value assigned for idScheme %s",
                    rowSet.getString( "coc_uid" ), idSchemes.getCategoryOptionComboIdScheme().name() ) );
        }
    }

    private String getEventSelectIdentifiersByIdScheme( IdSchemes idSchemes )
    {
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getOrgUnitIdScheme(),
            "ou.uid as ou_identifier, ",
            "ou.attributevalues #>> '{%s, value}' as ou_identifier, ",
            "ou.code as ou_identifier, " ) );

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getProgramIdScheme(),
            "p.uid as p_identifier, ",
            "p.attributevalues #>> '{%s, value}' as p_identifier, ",
            "p.code as p_identifier, " ) );

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getProgramStageIdScheme(),
            "ps.uid as ps_identifier, ",
            "ps.attributevalues #>> '{%s, value}' as ps_identifier, ",
            "ps.code as ps_identifier, " ) );

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getCategoryOptionComboIdScheme(),
            "coc.uid as coc_identifier, ",
            "coc.attributevalues #>> '{%s, value}' as coc_identifier, ",
            "coc.code as coc_identifier, " ) );

        return sqlBuilder.toString();
    }

    @Override
    public int getEventCount( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        User user = currentUserService.getCurrentUser();
        setAccessiblePrograms( user, params );

        String sql;

        if ( params.hasFilters() )
        {
            sql = buildGridSql( params, organisationUnits );
        }
        else
        {
            sql = getEventSelectQuery( params, organisationUnits, user );
        }

        sql = sql.replaceFirst( "select .*? from", "select count(*) from" );

        sql = sql.replaceFirst( "order .*? (desc|asc)", "" );

        sql = sql.replaceFirst( "limit \\d+ offset \\d+", "" );

        log.debug( "Event query count SQL: " + sql );

        return jdbcTemplate.queryForObject( sql, Integer.class );
    }

    private DataValue convertEventDataValueIntoDtoDataValue( EventDataValue eventDataValue )
    {
        DataValue dataValue = new DataValue();
        dataValue.setCreated( DateUtils.getIso8601NoTz( eventDataValue.getCreated() ) );
        dataValue.setCreatedByUserInfo( eventDataValue.getCreatedByUserInfo() );
        dataValue.setLastUpdated( DateUtils.getIso8601NoTz( eventDataValue.getLastUpdated() ) );
        dataValue.setLastUpdatedByUserInfo( eventDataValue.getLastUpdatedByUserInfo() );
        dataValue.setDataElement( eventDataValue.getDataElement() );
        dataValue.setValue( eventDataValue.getValue() );
        dataValue.setProvidedElsewhere( eventDataValue.getProvidedElsewhere() );
        dataValue.setStoredBy( eventDataValue.getStoredBy() );

        return dataValue;
    }

    private String buildGridSql( EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        SqlHelper hlp = new SqlHelper();

        // ---------------------------------------------------------------------
        // Select clause
        // ---------------------------------------------------------------------

        StringBuilder sqlBuilder = new StringBuilder().append( "select psi.uid as " + EVENT_ID + ", "
            + "psi.created as " + EVENT_CREATED_ID + ", "
            + "psi.lastupdated as " + EVENT_LAST_UPDATED_ID + ", " + "psi.storedby as " + EVENT_STORED_BY_ID + ", "
            + "psi.createdbyuserinfo as " + EVENT_CREATED_BY_USER_INFO_ID + ", " + "psi.lastupdatedbyuserinfo as "
            + EVENT_LAST_UPDATED_BY_USER_INFO_ID + ", "
            + "psi.completedby as " + EVENT_COMPLETED_BY_ID + ", " + "psi.completeddate as " + EVENT_COMPLETED_DATE_ID
            + ", "
            + "psi.duedate as " + EVENT_DUE_DATE_ID + ", " + "psi.executiondate as " + EVENT_EXECUTION_DATE_ID + ", "
            + "ou.uid as " + EVENT_ORG_UNIT_ID + ", " + "ou.name as " + EVENT_ORG_UNIT_NAME + ", "
            + "psi.status as " + EVENT_STATUS_ID + ", "
            + "pi.uid as " + EVENT_ENROLLMENT_ID + ", "
            + "ps.uid as " + EVENT_PROGRAM_STAGE_ID + ", " + "p.uid as "
            + EVENT_PROGRAM_ID + ", " + "coc.uid as " + EVENT_ATTRIBUTE_OPTION_COMBO_ID + ", " + "psi.deleted as "
            + EVENT_DELETED + ", "
            + "psi.geometry as " + EVENT_GEOMETRY + ", " );

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            final String col = item.getItemId();
            final String dataValueValueSql = "psi.eventdatavalues #>> '{" + col + ", value}'";

            String queryCol = item.isNumeric() ? castToNumber( dataValueValueSql ) : dataValueValueSql;
            queryCol += " as " + col + ", ";

            sqlBuilder.append( queryCol );
        }

        String intermediateSql = sqlBuilder.toString();
        sqlBuilder = new StringBuilder().append( removeLastComma( intermediateSql ) ).append( " " );

        // ---------------------------------------------------------------------
        // From and where clause
        // ---------------------------------------------------------------------

        sqlBuilder.append( getFromWhereClause( params, hlp, organisationUnits ) );

        // ---------------------------------------------------------------------
        // Order clause
        // ---------------------------------------------------------------------

        sqlBuilder.append( getGridOrderQuery( params ) );

        // ---------------------------------------------------------------------
        // Paging clause
        // ---------------------------------------------------------------------

        sqlBuilder.append( getEventPagingQuery( params ) );

        return sqlBuilder.toString();
    }

    /**
     * Query is based on three sub queries on event, data value and comment,
     * which are joined using program stage instance id. The purpose of the
     * separate queries is to be able to page properly on events.
     */
    private String buildSql( EventSearchParams params, List<OrganisationUnit> organisationUnits, User user )
    {
        StringBuilder sqlBuilder = new StringBuilder().append( "select * from (" );

        sqlBuilder.append( getEventSelectQuery( params, organisationUnits, user ) );

        sqlBuilder.append( getOrderQuery( params ) );

        sqlBuilder.append( getEventPagingQuery( params ) );

        sqlBuilder.append( ") as event left join (" );

        if ( params.isIncludeAttributes() )
        {
            sqlBuilder.append( getAttributeValueQuery() );

            sqlBuilder.append( ") as att on event.tei_id=att.pav_id left join (" );
        }

        sqlBuilder.append( PSI_EVENT_COMMENT_QUERY );

        sqlBuilder.append( ") as cm on event.psi_id=cm.psic_id " );

        if ( params.isIncludeRelationships() )
        {
            sqlBuilder.append( RELATIONSHIP_IDS_QUERY );
        }

        sqlBuilder.append( getOrderQuery( params ) );

        return sqlBuilder.toString();
    }

    private String getEventSelectQuery( EventSearchParams params, List<OrganisationUnit> organisationUnits, User user )
    {
        SqlHelper hlp = new SqlHelper();

        StringBuilder sqlBuilder = new StringBuilder().append( "select "
            + getEventSelectIdentifiersByIdScheme( params.getIdSchemes() )
            + " psi.uid as psi_uid, "
            + "ou.uid as ou_uid, p.uid as p_uid, ps.uid as ps_uid, coc.uid as coc_uid, "
            + "psi.programstageinstanceid as psi_id, psi.status as psi_status, psi.executiondate as psi_executiondate, "
            + "psi.eventdatavalues as psi_eventdatavalues, psi.duedate as psi_duedate, psi.completedby as psi_completedby, psi.storedby as psi_storedby, "
            + "psi.created as psi_created, psi.createdbyuserinfo as psi_createdbyuserinfo, psi.lastupdated as psi_lastupdated, psi.lastupdatedbyuserinfo as psi_lastupdatedbyuserinfo, "
            + "psi.completeddate as psi_completeddate, psi.deleted as psi_deleted, "
            + "ST_AsText( psi.geometry ) as psi_geometry, au.uid as user_assigned, (au.firstName || ' ' || au.surName) as user_assigned_name,"
            + "auc.username as user_assigned_username, cocco.categoryoptionid AS cocco_categoryoptionid, deco.uid AS deco_uid, " );

        if ( (params.getCategoryOptionCombo() == null || params.getCategoryOptionCombo().isDefault())
            && !isSuper( user ) )
        {
            sqlBuilder.append(
                "decoa.can_access AS decoa_can_access, cocount.option_size AS option_size, " );
        }

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            final String col = item.getItemId();
            final String dataValueValueSql = "psi.eventdatavalues #>> '{" + col + ", value}'";

            String queryCol = " " + (item.isNumeric() ? castToNumber( dataValueValueSql ) : lower( dataValueValueSql ));
            queryCol += " as " + col + ", ";

            sqlBuilder.append( queryCol );
        }

        sqlBuilder.append( "pi.uid as pi_uid, pi.status as pi_status, pi.followup as pi_followup, "
            + "p.type as p_type, ps.uid as ps_uid, ou.name as ou_name, "
            + "tei.trackedentityinstanceid as tei_id, tei.uid as tei_uid, teiou.uid as tei_ou, teiou.name as tei_ou_name, tei.created as tei_created, tei.inactive as tei_inactive "
            + "from programstageinstance psi "
            + "inner join programinstance pi on pi.programinstanceid=psi.programinstanceid "
            + "inner join program p on p.programid=pi.programid "
            + "inner join programstage ps on ps.programstageid=psi.programstageid "
            + "inner join categoryoptioncombo coc on coc.categoryoptioncomboid=psi.attributeoptioncomboid "
            + "inner join categoryoptioncombos_categoryoptions cocco on psi.attributeoptioncomboid=cocco.categoryoptioncomboid "
            + "inner join dataelementcategoryoption deco on cocco.categoryoptionid=deco.categoryoptionid "
            + "left join trackedentityprogramowner po on (pi.trackedentityinstanceid=po.trackedentityinstanceid) "
            + "inner join organisationunit ou on (coalesce(po.organisationunitid, psi.organisationunitid)=ou.organisationunitid) "
            + "left join trackedentityinstance tei on tei.trackedentityinstanceid=pi.trackedentityinstanceid "
            + "left join organisationunit teiou on (tei.organisationunitid=teiou.organisationunitid) "
            + "left join users auc on (psi.assigneduserid=auc.userid) "
            + "left join userinfo au on (auc.userid=au.userinfoid) " );

        Set<String> joinedColumns = new HashSet<>();

        String eventDataValuesWhereSql = "";

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            final String col = item.getItemId();
            final String optCol = item.getItemId() + "opt";
            final String dataValueValueSql = "psi.eventdatavalues #>> '{" + col + ", value}'";

            if ( !joinedColumns.contains( col ) )
            {
                if ( item.hasOptionSet() && item.hasFilter() )
                {
                    sqlBuilder.append( "inner join optionvalue as " + optCol + " on lower(" + optCol + ".code) = " +
                        "lower(" + dataValueValueSql + ") and " + optCol + ".optionsetid = "
                        + item.getOptionSet().getId() + " " );
                }

                joinedColumns.add( col );
            }

            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    final String encodedFilter = statementBuilder.encode( filter.getFilter(), false );

                    final String queryCol = item.isNumeric() ? " CAST( " + dataValueValueSql + " AS NUMERIC)"
                        : "lower( " + dataValueValueSql + " )";

                    if ( !item.hasOptionSet() )
                    {
                        if ( !eventDataValuesWhereSql.isEmpty() )
                        {
                            eventDataValuesWhereSql += " and ";
                        }

                        if ( QueryOperator.LIKE.getValue().equalsIgnoreCase( filter.getSqlOperator() ) )
                        {
                            eventDataValuesWhereSql += " " + queryCol + " " + filter.getSqlOperator() + " "
                                + StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) + " ";
                        }
                        else
                        {
                            eventDataValuesWhereSql += " " + queryCol + " " + filter.getSqlOperator() + " "
                                + StringUtils.lowerCase(
                                    item.isNumeric() ? encodedFilter : filter.getSqlFilter( encodedFilter ) )
                                + " ";
                        }
                    }
                    else if ( QueryOperator.IN.getValue().equalsIgnoreCase( filter.getSqlOperator() ) )
                    {
                        sqlBuilder.append( "and " + queryCol + " " + filter.getSqlOperator() + " "
                            + StringUtils.lowerCase(
                                item.isNumeric() ? encodedFilter : filter.getSqlFilter( encodedFilter ) )
                            + " " );
                    }
                    else if ( QueryOperator.LIKE.getValue().equalsIgnoreCase( filter.getSqlOperator() ) )
                    {
                        sqlBuilder.append( "and lower(" + optCol + DOT_NAME + " " + filter.getSqlOperator() + " "
                            + StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) + " " );
                    }
                    else
                    {
                        sqlBuilder.append( "and lower(" + optCol + DOT_NAME + " " + filter.getSqlOperator() + " "
                            + StringUtils.lowerCase(
                                item.isNumeric() ? encodedFilter : filter.getSqlFilter( encodedFilter ) )
                            + " " );
                    }
                }
            }
        }

        if ( (params.getCategoryOptionCombo() == null || params.getCategoryOptionCombo().isDefault())
            && !isSuper( user ) )
        {
            sqlBuilder.append( getCategoryOptionSharingForUser( user ) );
        }

        if ( !eventDataValuesWhereSql.isEmpty() )
        {
            sqlBuilder.append( hlp.whereAnd() + eventDataValuesWhereSql + " " );
        }

        if ( params.getTrackedEntityInstance() != null )
        {
            sqlBuilder.append(
                hlp.whereAnd() + " tei.trackedentityinstanceid=" + params.getTrackedEntityInstance().getId() + " " );
        }

        if ( params.getProgram() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " p.programid = " ).append( params.getProgram().getId() )
                .append( " " );
        }

        if ( params.getProgramStage() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " ps.programstageid = " )
                .append( params.getProgramStage().getId() ).append( " " );
        }

        if ( params.getProgramStatus() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " pi.status = '" ).append( params.getProgramStatus() )
                .append( "' " );
        }

        if ( params.getFollowUp() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " pi.followup is " )
                .append( params.getFollowUp() ? "true" : "false" ).append( " " );
        }

        sqlBuilder.append( addLastUpdatedFilters( params, hlp, true ) );

        // Comparing milliseconds instead of always creating new Date( 0 );
        if ( params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0 )
        {
            String skipChangedBefore = DateUtils.getLongDateString( params.getSkipChangedBefore() );
            sqlBuilder.append( hlp.whereAnd() ).append( PSI_LASTUPDATED_GT ).append( skipChangedBefore ).append( "' " );
        }

        if ( params.getCategoryOptionCombo() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.attributeoptioncomboid = " )
                .append( params.getCategoryOptionCombo().getId() ).append( " " );
        }

        if ( !CollectionUtils.isEmpty( organisationUnits ) || params.getOrgUnit() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( getOrgUnitSql( hlp, params, organisationUnits ) );
        }

        if ( params.getStartDate() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.executiondate >= '" )
                .append( getMediumDateString( params.getStartDate() ) ).append( "' " )
                .append( "or (psi.executiondate is null and psi.duedate >= '" )
                .append( getMediumDateString( params.getStartDate() ) ).append( "')) " );
        }

        if ( params.getEndDate() != null )
        {
            Date dateAfterEndDate = getDateAfterAddition( params.getEndDate(), 1 );
            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.executiondate < '" )
                .append( getMediumDateString( dateAfterEndDate ) ).append( "' " )
                .append( "or (psi.executiondate is null and psi.duedate < '" )
                .append( getMediumDateString( dateAfterEndDate ) ).append( "')) " );
        }

        if ( params.getProgramType() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " p.type = '" ).append( params.getProgramType() )
                .append( "' " );
        }

        if ( params.getEventStatus() != null )
        {
            if ( params.getEventStatus() == EventStatus.VISITED )
            {
                sqlBuilder.append( hlp.whereAnd() ).append( PSI_STATUS_EQ ).append( EventStatus.ACTIVE.name() )
                    .append( "' and psi.executiondate is not null " );
            }
            else if ( params.getEventStatus() == EventStatus.OVERDUE )
            {
                sqlBuilder.append( hlp.whereAnd() ).append( " date(now()) > date(psi.duedate) and psi.status = '" )
                    .append( EventStatus.SCHEDULE.name() ).append( "' " );
            }
            else
            {
                sqlBuilder.append( hlp.whereAnd() ).append( PSI_STATUS_EQ ).append( params.getEventStatus().name() )
                    .append( "' " );
            }
        }

        if ( params.getEvents() != null && !params.getEvents().isEmpty() && !params.hasFilters() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.uid in (" )
                .append( getQuotedCommaDelimitedString( params.getEvents() ) ).append( ")) " );
        }

        if ( params.hasAssignedUsers() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (au.uid in (" )
                .append( getQuotedCommaDelimitedString( params.getAssignedUsers() ) ).append( ")) " );
        }

        if ( params.isIncludeOnlyUnassignedEvents() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (au.uid is null) " );
        }

        if ( params.isIncludeOnlyAssignedEvents() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (au.uid is not null) " );
        }

        if ( !params.isIncludeDeleted() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.deleted is false " );
        }

        if ( params.hasSecurityFilter() )
        {
            sqlBuilder.append( hlp.whereAnd() + " (p.uid in ("
                + getQuotedCommaDelimitedString( params.getAccessiblePrograms() ) + ")) " );
            sqlBuilder.append( hlp.whereAnd() + " (ps.uid in ("
                + getQuotedCommaDelimitedString( params.getAccessibleProgramStages() ) + ")) " );
        }

        if ( params.isSynchronizationQuery() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.lastupdated > psi.lastsynchronized " );
        }

        if ( !CollectionUtils.isEmpty( params.getProgramInstances() ) )
        {
            sqlBuilder.append( hlp.whereAnd() )
                .append( " (pi.uid in (" + getQuotedCommaDelimitedString( params.getProgramInstances() ) + "))" );
        }

        return sqlBuilder.toString();
    }

    /**
     * From, join and where clause. For dataElement params, restriction is set
     * in inner join. For query params, restriction is set in where clause.
     */
    private String getFromWhereClause( EventSearchParams params, SqlHelper hlp,
        List<OrganisationUnit> organisationUnits )
    {
        StringBuilder sqlBuilder = new StringBuilder().append( "from programstageinstance psi "
            + "inner join programinstance pi on pi.programinstanceid = psi.programinstanceid "
            + "inner join program p on p.programid = pi.programid "
            + "inner join programstage ps on ps.programstageid = psi.programstageid "
            + "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = psi.attributeoptioncomboid "
            + "left join trackedentityprogramowner po on (pi.trackedentityinstanceid=po.trackedentityinstanceid) "
            + "inner join organisationunit ou on (coalesce(po.organisationunitid, psi.organisationunitid)=ou.organisationunitid) "
            + "left join users auc on (psi.assigneduserid=auc.userid) "
            + "left join userinfo au on (auc.userid=au.userinfoid) " );

        Set<String> joinedColumns = new HashSet<>();

        String eventDataValuesWhereSql = "";

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            final String col = item.getItemId();
            final String optCol = item.getItemId() + "opt";
            final String dataValueValueSql = "psi.eventdatavalues #>> '{" + col + ", value}'";

            if ( !joinedColumns.contains( col ) )
            {
                if ( item.hasOptionSet() && item.hasFilter() )
                {
                    sqlBuilder.append( "inner join optionvalue as " + optCol + " on lower(" + optCol + ".code) = " +
                        "lower(" + dataValueValueSql + ") and " + optCol + ".optionsetid = "
                        + item.getOptionSet().getId() + " " );
                }

                joinedColumns.add( col );
            }

            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    final String encodedFilter = statementBuilder.encode( filter.getFilter(), false );

                    final String queryCol = " " + (item.isNumeric() ? castToNumber( dataValueValueSql )
                        : lower( dataValueValueSql ));

                    if ( !item.hasOptionSet() )
                    {
                        if ( !eventDataValuesWhereSql.isEmpty() )
                        {
                            eventDataValuesWhereSql += " and ";
                        }

                        eventDataValuesWhereSql += " " + queryCol + " " + filter.getSqlOperator() + " "
                            + StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) + " ";
                    }
                    else if ( QueryOperator.IN.getValue().equalsIgnoreCase( filter.getSqlOperator() ) )
                    {
                        sqlBuilder.append( "and " ).append( queryCol ).append( " " ).append( filter.getSqlOperator() )
                            .append( " " ).append( StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) )
                            .append( " " );
                    }
                    else
                    {
                        sqlBuilder.append( "and lower( " ).append( optCol ).append( DOT_NAME ).append( " " )
                            .append( filter.getSqlOperator() ).append( " " )
                            .append( StringUtils.lowerCase( filter.getSqlFilter( encodedFilter ) ) ).append( " " );
                    }
                }
            }
        }

        if ( !eventDataValuesWhereSql.isEmpty() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( eventDataValuesWhereSql ).append( " " );
        }

        if ( !organisationUnits.isEmpty() || params.getOrgUnit() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( getOrgUnitSql( hlp, params, organisationUnits ) );
        }

        if ( params.getProgramStage() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " ps.programstageid = " )
                .append( params.getProgramStage().getId() ).append( " " );
        }

        if ( params.getCategoryOptionCombo() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.attributeoptioncomboid = " )
                .append( params.getCategoryOptionCombo().getId() ).append( " " );
        }

        if ( params.getStartDate() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.executiondate >= '" )
                .append( getMediumDateString( params.getStartDate() ) ).append( "' " )
                .append( "or (psi.executiondate is null and psi.duedate >= '" )
                .append( getMediumDateString( params.getStartDate() ) ).append( "')) " );
        }

        if ( params.getEndDate() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.executiondate <= '" )
                .append( getMediumDateString( params.getEndDate() ) ).append( "' " )
                .append( "or (psi.executiondate is null and psi.duedate <= '" )
                .append( getMediumDateString( params.getEndDate() ) ).append( "')) " );
        }

        sqlBuilder.append( addLastUpdatedFilters( params, hlp, false ) );

        if ( params.isSynchronizationQuery() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.lastupdated > psi.lastsynchronized " );
        }

        // Comparing milliseconds instead of always creating new Date( 0 )

        if ( params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0 )
        {
            String skipChangedBefore = DateUtils.getLongDateString( params.getSkipChangedBefore() );
            sqlBuilder.append( hlp.whereAnd() ).append( PSI_LASTUPDATED_GT ).append( skipChangedBefore ).append( "' " );
        }

        if ( params.getDueDateStart() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.duedate is not null and psi.duedate >= '" )
                .append( DateUtils.getLongDateString( params.getDueDateStart() ) ).append( "' " );
        }

        if ( params.getDueDateEnd() != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.duedate is not null and psi.duedate <= '" )
                .append( DateUtils.getLongDateString( params.getDueDateEnd() ) ).append( "' " );
        }

        if ( !params.isIncludeDeleted() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " psi.deleted is false " );
        }

        if ( params.getEventStatus() != null )
        {
            if ( params.getEventStatus() == EventStatus.VISITED )
            {
                sqlBuilder.append( hlp.whereAnd() ).append( PSI_STATUS_EQ ).append( EventStatus.ACTIVE.name() )
                    .append( "' and psi.executiondate is not null " );
            }
            else if ( params.getEventStatus() == EventStatus.OVERDUE )
            {
                sqlBuilder.append( hlp.whereAnd() ).append( " date(now()) > date(psi.duedate) and psi.status = '" )
                    .append( EventStatus.SCHEDULE.name() ).append( "' " );
            }
            else
            {
                sqlBuilder.append( hlp.whereAnd() ).append( PSI_STATUS_EQ ).append( params.getEventStatus().name() )
                    .append( "' " );
            }
        }

        if ( params.getEvents() != null && !params.getEvents().isEmpty() && !params.hasFilters() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.uid in (" )
                .append( getQuotedCommaDelimitedString( params.getEvents() ) ).append( ")) " );
        }

        if ( params.hasAssignedUsers() )
        {
            sqlBuilder.append(
                hlp.whereAnd() + " (au.uid in (" + getQuotedCommaDelimitedString( params.getAssignedUsers() ) + ")) " );
        }

        if ( params.isIncludeOnlyUnassignedEvents() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (au.uid is null) " );
        }

        if ( params.isIncludeOnlyAssignedEvents() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( " (au.uid is not null) " );
        }

        return sqlBuilder.toString();
    }

    private String addLastUpdatedFilters( EventSearchParams params, SqlHelper hlp, boolean useDateAfterEndDate )
    {
        StringBuilder sqlBuilder = new StringBuilder();

        if ( params.hasLastUpdatedDuration() )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( PSI_LASTUPDATED_GT )
                .append( getLongGmtDateString( DateUtils.nowMinusDuration( params.getLastUpdatedDuration() ) ) )
                .append( "' " );
        }
        else
        {
            if ( params.hasLastUpdatedStartDate() )
            {
                sqlBuilder.append( hlp.whereAnd() ).append( PSI_LASTUPDATED_GT )
                    .append( DateUtils.getLongDateString( params.getLastUpdatedStartDate() ) ).append( "' " );
            }

            if ( params.hasLastUpdatedEndDate() )
            {
                if ( useDateAfterEndDate )
                {
                    Date dateAfterEndDate = getDateAfterAddition( params.getLastUpdatedEndDate(), 1 );
                    sqlBuilder.append( hlp.whereAnd() ).append( " psi.lastupdated < '" )
                        .append( DateUtils.getLongDateString( dateAfterEndDate ) ).append( "' " );
                }
                else
                {
                    sqlBuilder.append( hlp.whereAnd() ).append( " psi.lastupdated <= '" )
                        .append( DateUtils.getLongDateString( params.getLastUpdatedEndDate() ) ).append( "' " );
                }
            }
        }

        return sqlBuilder.toString();
    }

    private String getCategoryOptionSharingForUser( User user )
    {
        StringBuilder sqlBuilder = new StringBuilder().append( " left join ( " );

        sqlBuilder.append( "select categoryoptioncomboid, count(categoryoptioncomboid) as option_size "
            + "from categoryoptioncombos_categoryoptions group by categoryoptioncomboid) "
            + "as cocount on coc.categoryoptioncomboid = cocount.categoryoptioncomboid "
            + "left join ("
            + "select deco.categoryoptionid as deco_id, deco.uid as deco_uid , "
            + "( select ( " + JpaQueryUtils.generateSQlQueryForSharingCheck( "deco.sharing",
                user, AclService.LIKE_READ_DATA )
            + " ) ) as can_access "
            + "from dataelementcategoryoption deco " );

        sqlBuilder.append( " ) as decoa on cocco.categoryoptionid = decoa.deco_id " );

        return sqlBuilder.toString();
    }

    private String getEventPagingQuery( EventSearchParams params )
    {
        StringBuilder sqlBuilder = new StringBuilder().append( " " );

        if ( !params.isSkipPaging() )
        {
            sqlBuilder.append( "limit " ).append( params.getPageSizeWithDefault() ).append( " offset " )
                .append( params.getOffset() ).append( " " );
        }

        return sqlBuilder.toString();
    }

    private String getGridOrderQuery( EventSearchParams params )
    {

        if ( params.getGridOrders() != null && params.getDataElements() != null && !params.getDataElements().isEmpty()
            && STATIC_EVENT_COLUMNS != null && !STATIC_EVENT_COLUMNS.isEmpty() )
        {
            List<String> orderFields = new ArrayList<>();

            for ( OrderParam order : params.getGridOrders() )
            {
                if ( STATIC_EVENT_COLUMNS.contains( order.getField() ) )
                {
                    orderFields.add( order.getField() + " " + order.getDirection() );
                }
                else
                {
                    Set<QueryItem> queryItems = params.getDataElements();

                    for ( QueryItem item : queryItems )
                    {
                        if ( order.getField().equals( item.getItemId() ) )
                        {
                            orderFields.add( order.getField() + " " + order.getDirection() );
                            break;
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

    private String getOrderQuery( EventSearchParams params )
    {
        ArrayList<String> orderFields = new ArrayList<>();

        if ( params.getGridOrders() != null )
        {
            for ( OrderParam order : params.getGridOrders() )
            {

                Set<QueryItem> items = params.getDataElements();

                for ( QueryItem item : items )
                {
                    if ( order.getField().equals( item.getItemId() ) )
                    {
                        orderFields.add( order.getField() + " " + order.getDirection() );
                        break;
                    }
                }
            }
        }

        if ( params.getOrders() != null )
        {
            for ( OrderParam order : params.getOrders() )
            {
                if ( QUERY_PARAM_COL_MAP.containsKey( order.getField() ) )
                {
                    String orderText = QUERY_PARAM_COL_MAP.get( order.getField() );
                    orderText += " " + (order.getDirection().isAscending() ? "asc" : "desc");
                    orderFields.add( orderText );
                }
            }
        }

        if ( !orderFields.isEmpty() )
        {
            return "order by " + StringUtils.join( orderFields, ',' ) + " ";
        }
        else
        {
            return "order by psi_lastupdated desc ";
        }
    }

    private String getAttributeValueQuery()
    {
        return "select pav.trackedentityinstanceid as pav_id, pav.created as pav_created, pav.lastupdated as pav_lastupdated, "
            + "pav.value as pav_value, ta.uid as ta_uid, ta.name as ta_name, ta.valuetype as ta_valuetype "
            + "from trackedentityattributevalue pav "
            + "inner join trackedentityattribute ta on pav.trackedentityattributeid=ta.trackedentityattributeid ";
    }

    private boolean isSuper( User user )
    {
        return user == null || user.isSuper();
    }

    /**
     * Saves a list of {@see ProgramStageInstance} using JDBC batch update.
     *
     * Note that this method is using JdbcTemplate to execute the batch
     * operation, therefore it's able to participate in any Spring-initiated
     * transaction
     *
     * @param batch the list of {@see ProgramStageInstance}
     * @return the list of created {@see ProgramStageInstance} with primary keys
     *         assigned
     *
     */
    private List<ProgramStageInstance> saveAllEvents( List<ProgramStageInstance> batch )
    {
        JdbcUtils.batchUpdateWithKeyHolder( jdbcTemplate, INSERT_EVENT_SQL,
            new BatchPreparedStatementSetterWithKeyHolder<ProgramStageInstance>( sort( batch ) )
            {
                @Override
                protected void setValues( PreparedStatement ps, ProgramStageInstance event )
                {
                    try
                    {
                        bindEventParamsForInsert( ps, event );
                    }
                    catch ( JsonProcessingException | SQLException e )
                    {
                        log.warn( "PSI failed to persist and will be ignored. PSI UID: " + event.getUid(),
                            event.getUid(), e );
                    }
                }

                @Override
                protected void setPrimaryKey( Map<String, Object> primaryKey, ProgramStageInstance event )
                {
                    event.setId( (Long) primaryKey.get( "programstageinstanceid" ) );
                }

            } );

        /*
         * Extract the primary keys from the created objects
         */
        List<Long> eventIds = batch.stream().map( BaseIdentifiableObject::getId ).collect( Collectors.toList() );

        /*
         * Assign the generated event PKs to the batch.
         *
         * If the generate event PKs size doesn't match the batch size, one or
         * more PSI were not persisted. Run an additional query to fetch the
         * persisted PSI and return only the PSI from the batch which are
         * persisted.
         *
         */
        if ( eventIds.size() != batch.size() )
        {
            /* a Map where [key] -> PSI UID , [value] -> PSI ID */
            Map<String, Long> persisted = jdbcTemplate
                .queryForList(
                    "SELECT uid, programstageinstanceid from programstageinstance where programstageinstanceid in ( "
                        + Joiner.on( ";" ).join( eventIds ) + ")" )
                .stream().collect(
                    Collectors.toMap( s -> (String) s.get( "uid" ), s -> (Long) s.get( "programstageinstanceid" ) ) );

            // @formatter:off
            return batch.stream()
                .filter( psi -> persisted.containsKey( psi.getUid() ) )
                .peek( psi -> psi.setId( persisted.get( psi.getUid() ) ) )
                .collect( Collectors.toList() );
            // @formatter:on
        }
        else
        {
            for ( int i = 0; i < eventIds.size(); i++ )
            {
                batch.get( i ).setId( eventIds.get( i ) );
            }
            return batch;
        }
    }

    @Override
    public void updateTrackedEntityInstances( List<String> teiUids, User user )
    {
        Optional.ofNullable( teiUids ).filter( s -> !s.isEmpty() )
            .ifPresent( teis -> updateTrackedEntityInstances( teis.stream()
                .sorted() // make sure the list is sorted, to prevent
                          // deadlocks
                .map( s -> "'" + s + "'" )
                .collect( Collectors.joining( ", " ) ), user ) );
    }

    private void updateTrackedEntityInstances( String teisInCondition, User user )
    {
        try
        {
            Timestamp timestamp = new Timestamp( System.currentTimeMillis() );

            String sql = String.format( UPDATE_TEI_SQL, teisInCondition, getSkipLocked(), "'" + timestamp + "'",
                user != null ? user.getId() : NULL, teisInCondition );

            jdbcTemplate.execute( sql );
        }
        catch ( DataAccessException e )
        {
            log.error( "An error occurred updating one or more Tracked Entity Instances", e );
            throw e;
        }
    }

    /**
     * Awful hack required for the H2-based tests to pass. H2 does not support
     * the "SKIP LOCKED" clause, therefore we need to remove it from the SQL
     * statement when executing the H2 tests.
     *
     * @return a SQL String containing Skip Locked statement
     */
    private String getSkipLocked()
    {
        return SystemUtils.isTestRun( env.getActiveProfiles() ) ? "" : "SKIP LOCKED";
    }

    private void bindEventParamsForInsert( PreparedStatement ps, ProgramStageInstance event )
        throws SQLException,
        JsonProcessingException
    {
        // @formatter:off
        ps.setLong(         1, event.getProgramInstance().getId() );
        ps.setLong(         2, event.getProgramStage().getId() );
        ps.setTimestamp(    3, JdbcEventSupport.toTimestamp( event.getDueDate() ) );
        ps.setTimestamp(    4, JdbcEventSupport.toTimestamp( event.getExecutionDate() ) );
        ps.setLong(         5, event.getOrganisationUnit().getId() );
        ps.setString(       6, event.getStatus().toString() );
        ps.setTimestamp(    7, JdbcEventSupport.toTimestamp( event.getCompletedDate() ) );
        ps.setString(       8, event.getUid() );
        ps.setTimestamp(    9, JdbcEventSupport.toTimestamp( new Date() ) );
        ps.setTimestamp(    10, JdbcEventSupport.toTimestamp( new Date() ) );
        ps.setLong(         11, event.getAttributeOptionCombo().getId() );
        ps.setString(       12, event.getStoredBy() );
        ps.setObject(       13, userInfoToJson( event.getCreatedByUserInfo(), jsonMapper ) );
        ps.setObject(       14, userInfoToJson( event.getLastUpdatedByUserInfo(), jsonMapper ) );
        ps.setString(       15, event.getCompletedBy() );
        ps.setBoolean(      16, false );
        ps.setString(       17, event.getCode() );
        ps.setTimestamp(    18, JdbcEventSupport.toTimestamp( event.getCreatedAtClient() ) );
        ps.setTimestamp(    19, JdbcEventSupport.toTimestamp( event.getLastUpdatedAtClient() ) );
        ps.setObject(       20, JdbcEventSupport.toGeometry( event.getGeometry() )  );
        if ( event.getAssignedUser() != null )
        {
            ps.setLong(     21, event.getAssignedUser().getId() );
        }
        else
        {
            ps.setObject(   21, null );
        }
        ps.setObject(       22, eventDataValuesToJson( event.getEventDataValues(), this.jsonMapper ) );
        // @formatter:on
    }

    private void bindEventParamsForUpdate( PreparedStatement ps, ProgramStageInstance programStageInstance )
        throws SQLException,
        JsonProcessingException
    {

        ps.setLong( 1, programStageInstance.getProgramInstance().getId() );
        ps.setLong( 2, programStageInstance.getProgramStage().getId() );
        ps.setTimestamp( 3, new Timestamp( programStageInstance.getDueDate().getTime() ) );
        if ( programStageInstance.getExecutionDate() != null )
        {
            ps.setTimestamp( 4, new Timestamp( programStageInstance.getExecutionDate().getTime() ) );
        }
        else
        {
            ps.setObject( 4, null );
        }
        ps.setLong( 5, programStageInstance.getOrganisationUnit().getId() );
        ps.setString( 6, programStageInstance.getStatus().toString() );
        ps.setTimestamp( 7, JdbcEventSupport.toTimestamp( programStageInstance.getCompletedDate() ) );
        ps.setTimestamp( 8, JdbcEventSupport.toTimestamp( new Date() ) );
        ps.setLong( 9, programStageInstance.getAttributeOptionCombo().getId() );
        ps.setString( 10, programStageInstance.getStoredBy() );
        ps.setObject( 11, userInfoToJson( programStageInstance.getLastUpdatedByUserInfo(), jsonMapper ) );
        ps.setString( 12, programStageInstance.getCompletedBy() );
        ps.setBoolean( 13, programStageInstance.isDeleted() );
        ps.setString( 14, programStageInstance.getCode() );
        ps.setTimestamp( 15, JdbcEventSupport.toTimestamp( programStageInstance.getCreatedAtClient() ) );
        ps.setTimestamp( 16, JdbcEventSupport.toTimestamp( programStageInstance.getLastUpdatedAtClient() ) );
        ps.setObject( 17, JdbcEventSupport.toGeometry( programStageInstance.getGeometry() ) );

        if ( programStageInstance.getAssignedUser() != null )
        {
            ps.setLong( 18, programStageInstance.getAssignedUser().getId() );
        }
        else
        {
            ps.setObject( 18, null );
        }

        ps.setObject( 19, eventDataValuesToJson( programStageInstance.getEventDataValues(), this.jsonMapper ) );
        ps.setString( 20, programStageInstance.getUid() );
    }

    private boolean userHasAccess( SqlRowSet rowSet )
    {
        if ( rowSet.wasNull() )
        {
            return true;
        }

        return rowSet.getBoolean( "decoa_can_access" );
    }

    private Set<EventDataValue> convertEventDataValueJsonIntoSet( String jsonString )
    {
        try
        {
            Map<String, EventDataValue> data = eventDataValueJsonReader.readValue( jsonString );
            return JsonEventDataValueSetBinaryType.convertEventDataValuesMapIntoSet( data );
        }
        catch ( IOException e )
        {
            log.error( "Parsing EventDataValues json string failed. String value: " + jsonString );
            throw new IllegalArgumentException( e );
        }
    }

    private void convertDataValuesIdentifiers( IdScheme idScheme, List<Collection<DataValue>> dataValuesList,
        CachingMap<String, String> dataElementUidToIdentifierCache )
    {
        for ( Collection<DataValue> dataValues : dataValuesList )
        {
            for ( DataValue dv : dataValues )
            {
                String deUid = dv.getDataElement();
                String deIdentifier = dataElementUidToIdentifierCache.get( deUid );

                if ( StringUtils.isEmpty( deIdentifier ) )
                {
                    throw new IllegalStateException(
                        "DataElement: " + deUid + " does not have a value assigned for idScheme " + idScheme.name() );
                }

                dv.setDataElement( deIdentifier );
            }
        }
    }

    private void populateCache( IdScheme idScheme, List<Collection<DataValue>> dataValuesList,
        CachingMap<String, String> dataElementUidToIdentifierCache )
    {
        Set<String> deUids = new HashSet<>();

        for ( Collection<DataValue> dataValues : dataValuesList )
        {
            for ( DataValue dv : dataValues )
            {
                deUids.add( dv.getDataElement() );
            }
        }

        if ( !idScheme.isAttribute() )
        {
            List<DataElement> dataElements = manager.getByUid( DataElement.class, deUids );
            dataElements.forEach( de -> dataElementUidToIdentifierCache.put( de.getUid(), de.getCode() ) );
        }
        else
        {
            if ( !deUids.isEmpty() )
            {
                String dataElementsUidsSqlString = getQuotedCommaDelimitedString( deUids );

                String deSql = "select de.uid, de.attributevalues #>> '{" + escapeSql( idScheme.getAttribute() )
                    + ", value}' as value from dataelement de where de.uid in (" + dataElementsUidsSqlString + ") "
                    + "and de.attributevalues ? '" + escapeSql( idScheme.getAttribute() ) + "'";

                SqlRowSet deRowSet = jdbcTemplate.queryForRowSet( deSql );

                while ( deRowSet.next() )
                {
                    dataElementUidToIdentifierCache.put( deRowSet.getString( "uid" ), deRowSet.getString( "value" ) );
                }
            }
        }
    }

    @Override
    public void delete( final List<Event> events )
    {
        if ( isNotEmpty( events ) )
        {
            final List<String> psiUids = events.stream().map( event -> "'" + event.getEvent() + "'" )
                .collect( toList() );
            final String uids = Joiner.on( "," ).join( psiUids );

            jdbcTemplate.execute( "UPDATE programstageinstance SET deleted = true where uid in ( " + uids + ")" );
        }
    }

    private void setAccessiblePrograms( User user, EventSearchParams params )
    {
        if ( !isSuper( user ) )
        {
            params.setAccessiblePrograms(
                manager.getDataReadAll( Program.class ).stream().map( Program::getUid ).collect( Collectors.toSet() ) );

            params.setAccessibleProgramStages( manager.getDataReadAll( ProgramStage.class ).stream()
                .map( ProgramStage::getUid ).collect( Collectors.toSet() ) );
        }
    }

    /**
     * Sort the list of {@see ProgramStageInstance} by UID
     */
    private List<ProgramStageInstance> sort( List<ProgramStageInstance> batch )
    {
        return batch.stream().sorted( Comparator.comparing( ProgramStageInstance::getUid ) ).collect( toList() );
    }

    private String getOrgUnitSql( SqlHelper hlp, EventSearchParams params, List<OrganisationUnit> organisationUnits )
    {
        StringBuilder orgUnitSql = new StringBuilder();

        if ( params.getOrgUnit() != null && !params.isPathOrganisationUnitMode() )
        {
            orgUnitSql.append( " ou.organisationunitid = " + params.getOrgUnit().getId() + " " );
        }
        else
        {
            SqlHelper orHlp = new SqlHelper( true );
            String path = "ou.path LIKE '";
            for ( OrganisationUnit organisationUnit : organisationUnits )
            {
                if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.DESCENDANTS ) )
                {
                    orgUnitSql.append( orHlp.or() ).append( path )
                        .append( organisationUnit.getPath() ).append( "%' " )
                        .append( hlp.whereAnd() ).append( " ou.hierarchylevel > " + organisationUnit.getLevel() );
                }
                else if ( params.isOrganisationUnitMode( OrganisationUnitSelectionMode.CHILDREN ) )
                {
                    orgUnitSql.append( orHlp.or() ).append( path )
                        .append( organisationUnit.getPath() ).append( "%' " )
                        .append( hlp.whereAnd() ).append( " ou.hierarchylevel = " + (organisationUnit.getLevel() + 1) );
                }
                else
                {
                    orgUnitSql.append( orHlp.or() ).append( path )
                        .append( organisationUnit.getPath() ).append( "%' " );
                }
            }

            if ( !organisationUnits.isEmpty() )
            {
                orgUnitSql.insert( 0, " (" );
                orgUnitSql.append( ") " );

                if ( params.isPathOrganisationUnitMode() )
                {
                    orgUnitSql.insert( 0, " (" );
                    orgUnitSql.append( orHlp.or() )
                        .append( " (ou.organisationunitid = " + params.getOrgUnit().getId() + ")) " );
                }
            }
        }

        return orgUnitSql.toString();
    }
}
