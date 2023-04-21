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
package org.hisp.dhis.tracker.export.event;

import static java.util.Map.entry;
import static org.hisp.dhis.common.ValueType.NUMERIC_TYPES;
import static org.hisp.dhis.system.util.SqlUtils.castToNumber;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_ATTRIBUTE_OPTION_COMBO_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_COMPLETED_AT_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_COMPLETED_BY_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_CREATED_AT_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_CREATED_BY_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_DELETED;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_ENROLLMENT_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_GEOMETRY;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_OCCURRED_AT_DATE_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_ORG_UNIT_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_ORG_UNIT_NAME;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_PROGRAM_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_PROGRAM_STAGE_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_SCHEDULE_AT_DATE_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_STATUS_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_STORED_BY_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_UPDATED_AT_ID;
import static org.hisp.dhis.tracker.export.event.EventSearchParams.EVENT_UPDATED_BY;
import static org.hisp.dhis.util.DateUtils.addDays;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository( "org.hisp.dhis.tracker.export.event.EventStore" )
@RequiredArgsConstructor
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
        " userinfo.userinfoid            as usernote_id," +
        " userinfo.code                  as usernote_code," +
        " userinfo.uid                   as usernote_uid," +
        " userinfo.username              as usernote_username," +
        " userinfo.firstname             as userinfo_firstname," +
        " userinfo.surname               as userinfo_surname" +
        " from programstageinstancecomments psic" +
        " inner join trackedentitycomment psinote" +
        " on psic.trackedentitycommentid = psinote.trackedentitycommentid" +
        " left join userinfo on psinote.lastupdatedby = userinfo.userinfoid ";

    private static final String PSI_STATUS = "psi_status";

    private static final String PSI_STATUS_EQ = " psi.status = ";

    private static final String PSI_LASTUPDATED_GT = " psi.lastupdated >= ";

    private static final String DOT_NAME = ".name)";

    private static final String SPACE = " ";

    private static final String EQUALS = " = ";

    private static final String AND = " AND ";

    public static final Map<String, String> QUERY_PARAM_COL_MAP = Map.ofEntries(
        entry( EVENT_ID, "psi_uid" ),
        entry( EVENT_PROGRAM_ID, "p_uid" ),
        entry( EVENT_PROGRAM_STAGE_ID, "ps_uid" ),
        entry( EVENT_ENROLLMENT_ID, "pi_uid" ),
        entry( "enrollmentStatus", "pi_status" ),
        entry( "enrolledAt", "pi_enrollmentdate" ),
        entry( EVENT_ORG_UNIT_ID, "ou_uid" ),
        entry( EVENT_ORG_UNIT_NAME, "ou_name" ),
        entry( "trackedEntity", "tei_uid" ),
        entry( EVENT_OCCURRED_AT_DATE_ID, "psi_executiondate" ),
        entry( "followup", "pi_followup" ),
        entry( EVENT_STATUS_ID, PSI_STATUS ),
        entry( EVENT_SCHEDULE_AT_DATE_ID, "psi_duedate" ),
        entry( EVENT_STORED_BY_ID, "psi_storedby" ),
        entry( EVENT_UPDATED_BY, "psi_lastupdatedbyuserinfo" ),
        entry( EVENT_CREATED_BY_ID, "psi_createdbyuserinfo" ),
        entry( EVENT_CREATED_AT_ID, "psi_created" ),
        entry( EVENT_UPDATED_AT_ID, "psi_lastupdated" ),
        entry( EVENT_COMPLETED_BY_ID, "psi_completedby" ),
        entry( EVENT_ATTRIBUTE_OPTION_COMBO_ID, "psi_aoc" ),
        entry( EVENT_COMPLETED_AT_ID, "psi_completeddate" ),
        entry( EVENT_DELETED, "psi_deleted" ),
        entry( "assignedUser", "user_assigned_username" ),
        entry( "assignedUserDisplayName", "user_assigned_name" ) );

    private static final Map<String, String> COLUMNS_ALIAS_MAP = ImmutableMap.<String, String> builder()
        .put( EventQuery.COLUMNS.UID.getQueryElement().useInSelect(), EVENT_ID )
        .put( EventQuery.COLUMNS.CREATED.getQueryElement().useInSelect(), EVENT_CREATED_AT_ID )
        .put( EventQuery.COLUMNS.UPDATED.getQueryElement().useInSelect(), EVENT_UPDATED_AT_ID )
        .put( EventQuery.COLUMNS.STOREDBY.getQueryElement().useInSelect(), EVENT_STORED_BY_ID )
        .put( "psi.createdbyuserinfo", EVENT_CREATED_BY_ID )
        .put( "psi.lastupdatedbyuserinfo", EVENT_UPDATED_BY )
        .put( EventQuery.COLUMNS.COMPLETEDBY.getQueryElement().useInSelect(), EVENT_COMPLETED_BY_ID )
        .put( EventQuery.COLUMNS.COMPLETEDDATE.getQueryElement().useInSelect(),
            EVENT_COMPLETED_AT_ID )
        .put( EventQuery.COLUMNS.DUE_DATE.getQueryElement().useInSelect(), EVENT_SCHEDULE_AT_DATE_ID )
        .put( EventQuery.COLUMNS.EXECUTION_DATE.getQueryElement().useInSelect(),
            EVENT_OCCURRED_AT_DATE_ID )
        .put( "ou.uid", EVENT_ORG_UNIT_ID )
        .put( "ou.name", EVENT_ORG_UNIT_NAME )
        .put( EventQuery.COLUMNS.STATUS.getQueryElement().useInSelect(), EVENT_STATUS_ID )
        .put( "pi.uid", EVENT_ENROLLMENT_ID )
        .put( "ps.uid", EVENT_PROGRAM_STAGE_ID )
        .put( "p.uid", EVENT_PROGRAM_ID )
        .put( "coc.uid", EVENT_ATTRIBUTE_OPTION_COMBO_ID )
        .put( EventQuery.COLUMNS.DELETED.getQueryElement().useInSelect(), EVENT_DELETED )
        .put( "psi.geometry", EVENT_GEOMETRY )
        .build();

    public static final List<String> STATIC_EVENT_COLUMNS = Arrays.asList( EVENT_ID,
        EVENT_ENROLLMENT_ID,
        EVENT_CREATED_AT_ID, EVENT_CREATED_BY_ID,
        EVENT_UPDATED_AT_ID, EVENT_UPDATED_BY,
        EVENT_STORED_BY_ID, EVENT_COMPLETED_BY_ID,
        EVENT_COMPLETED_AT_ID, EVENT_OCCURRED_AT_DATE_ID,
        EVENT_SCHEDULE_AT_DATE_ID,
        EVENT_ORG_UNIT_ID, EVENT_ORG_UNIT_NAME, EVENT_STATUS_ID,
        EVENT_PROGRAM_STAGE_ID, EVENT_PROGRAM_ID,
        EVENT_ATTRIBUTE_OPTION_COMBO_ID, EVENT_DELETED,
        EVENT_GEOMETRY );

    private static final String PATH_LIKE = "path LIKE";

    private static final String PATH_EQ = "path =";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    // Cannot use DefaultRenderService mapper. Does not work properly -
    // DHIS2-6102
    private static final ObjectReader eventDataValueJsonReader = JsonBinaryType.MAPPER
        .readerFor( new TypeReference<Map<String, EventDataValue>>()
        {
        } );

    private final StatementBuilder statementBuilder;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Qualifier( "dataValueJsonMapper" )
    private final ObjectMapper jsonMapper;

    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager manager;

    private final RelationshipStore relationshipStore;

    // -------------------------------------------------------------------------
    // EventStore implementation
    // -------------------------------------------------------------------------

    @Override
    public List<ProgramStageInstance> getEvents( EventSearchParams params,
        Map<String, Set<String>> psdesWithSkipSyncTrue )
    {
        User user = currentUserService.getCurrentUser();

        setAccessiblePrograms( user, params );

        List<ProgramStageInstance> events = new ArrayList<>();
        List<Long> relationshipIds = new ArrayList<>();

        final Gson gson = new Gson();

        final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

        String sql = buildSql( params, mapSqlParameterSource, user );

        return jdbcTemplate.query( sql, mapSqlParameterSource, resultSet -> {

            log.debug( "Event query SQL: " + sql );

            Set<String> notes = new HashSet<>();

            while ( resultSet.next() )
            {
                if ( resultSet.getString( "psi_uid" ) == null )
                {
                    continue;
                }

                String psiUid = resultSet.getString( "psi_uid" );

                validateIdentifiersPresence( resultSet, params.getIdSchemes() );

                ProgramStageInstance event = new ProgramStageInstance();

                if ( !params.isSkipEventId() )
                {
                    event.setUid( psiUid );
                }

                TrackedEntityInstance tei = new TrackedEntityInstance();
                tei.setUid( resultSet.getString( "tei_uid" ) );
                event.setStatus( EventStatus.valueOf( resultSet.getString( PSI_STATUS ) ) );
                ProgramType programType = ProgramType.fromValue( resultSet.getString( "p_type" ) );
                Program program = new Program();
                program.setUid( resultSet.getString( "p_identifier" ) );
                program.setProgramType( programType );
                ProgramInstance pi = new ProgramInstance();
                pi.setUid( resultSet.getString( "pi_uid" ) );
                pi.setProgram( program );
                pi.setEntityInstance( tei );
                OrganisationUnit ou = new OrganisationUnit();
                ou.setUid( resultSet.getString( "ou_uid" ) );
                ou.setName( resultSet.getString( "ou_name" ) );
                ProgramStage ps = new ProgramStage();
                ps.setUid( resultSet.getString( "ps_identifier" ) );
                event.setDeleted( resultSet.getBoolean( "psi_deleted" ) );

                pi.setStatus( ProgramStatus.valueOf( resultSet.getString( "pi_status" ) ) );
                pi.setFollowup( resultSet.getBoolean( "pi_followup" ) );
                event.setProgramInstance( pi );
                event.setProgramStage( ps );
                event.setOrganisationUnit( ou );
                CategoryOptionCombo coc = new CategoryOptionCombo();
                coc.setUid( resultSet.getString( "coc_identifier" ) );

                Set<CategoryOption> options = Arrays.stream( resultSet.getString( "co_uids" ).split( ";" ) )
                    .map( optionUid -> {
                        CategoryOption option = new CategoryOption();
                        option.setUid( optionUid );
                        return option;
                    } )
                    .collect( Collectors.toSet() );
                coc.setCategoryOptions( options );

                event.setAttributeOptionCombo( coc );

                event.setStoredBy( resultSet.getString( "psi_storedby" ) );
                event.setDueDate( resultSet.getDate( "psi_duedate" ) );
                event.setExecutionDate( resultSet.getDate( "psi_executiondate" ) );
                event.setCreated( resultSet.getDate( "psi_created" ) );
                event.setCreatedByUserInfo(
                    EventUtils.jsonToUserInfo( resultSet.getString( "psi_createdbyuserinfo" ), jsonMapper ) );
                event.setLastUpdated( resultSet.getDate( "psi_lastupdated" ) );
                event.setLastUpdatedByUserInfo(
                    EventUtils.jsonToUserInfo( resultSet.getString( "psi_lastupdatedbyuserinfo" ), jsonMapper ) );

                event.setCompletedBy( resultSet.getString( "psi_completedby" ) );
                event.setCompletedDate( resultSet.getDate( "psi_completeddate" ) );

                if ( resultSet.getObject( "psi_geometry" ) != null )
                {
                    try
                    {
                        Geometry geom = new WKTReader().read( resultSet.getString( "psi_geometry" ) );

                        event.setGeometry( geom );
                    }
                    catch ( ParseException e )
                    {
                        log.error( "Unable to read geometry for event '" + event.getUid() + "': ", e );
                    }
                }

                if ( resultSet.getObject( "user_assigned" ) != null )
                {
                    User eventUser = new User();
                    eventUser.setUid( resultSet.getString( "user_assigned" ) );
                    eventUser.setUsername( resultSet.getString( "user_assigned_username" ) );
                    eventUser.setName( resultSet.getString( "user_assigned_name" ) );
                    eventUser.setFirstName( resultSet.getString( "user_assigned_first_name" ) );
                    eventUser.setSurname( resultSet.getString( "user_assigned_surname" ) );
                    event.setAssignedUser( eventUser );
                }

                events.add( event );

                if ( !StringUtils.isEmpty( resultSet.getString( "psi_eventdatavalues" ) ) )
                {
                    Set<EventDataValue> eventDataValues = convertEventDataValueJsonIntoSet(
                        resultSet.getString( "psi_eventdatavalues" ) );

                    event.getEventDataValues().addAll( eventDataValues );
                }

                if ( resultSet.getString( "psinote_value" ) != null
                    && !notes.contains( resultSet.getString( "psinote_id" ) ) )
                {
                    TrackedEntityComment note = new TrackedEntityComment();
                    note.setUid( resultSet.getString( "psinote_uid" ) );
                    note.setCommentText( resultSet.getString( "psinote_value" ) );
                    note.setCreated( resultSet.getDate( "psinote_storeddate" ) );
                    note.setCreator( resultSet.getString( "psinote_storedby" ) );

                    if ( resultSet.getObject( "usernoteupdated_id" ) != null )
                    {
                        User userNote = new User();
                        userNote.setId( resultSet.getLong( "usernoteupdated_id" ) );
                        userNote.setCode( resultSet.getString( "usernoteupdated_code" ) );
                        userNote.setUid( resultSet.getString( "usernoteupdated_uid" ) );
                        userNote.setUsername( resultSet.getString( "usernoteupdated_username" ) );
                        userNote.setFirstName( resultSet.getString( "usernoteupdated_firstname" ) );
                        userNote.setSurname( resultSet.getString( "usernoteupdated_surname" ) );
                        note.setLastUpdatedBy( userNote );
                    }

                    note.setLastUpdated( resultSet.getDate( "psinote_lastupdated" ) );

                    event.getComments().add( note );
                    notes.add( resultSet.getString( "psinote_id" ) );
                }

                if ( params.isIncludeRelationships() && resultSet.getObject( "psi_rl" ) != null )
                {
                    PGobject pGobject = (PGobject) resultSet.getObject( "psi_rl" );

                    if ( pGobject != null )
                    {
                        String value = pGobject.getValue();

                        relationshipIds.addAll( Lists.newArrayList( gson.fromJson( value, Long[].class ) ) );
                    }
                }
            }

            List<Relationship> relationships = relationshipStore.getById( relationshipIds );

            Multimap<String, RelationshipItem> map = LinkedListMultimap.create();

            for ( Relationship relationship : relationships )
            {
                if ( relationship.getFrom().getProgramStageInstance() != null )
                {
                    map.put( relationship.getFrom().getProgramStageInstance().getUid(), relationship.getFrom() );
                }
                if ( relationship.getTo().getProgramStageInstance() != null )
                {
                    map.put( relationship.getTo().getProgramStageInstance().getUid(), relationship.getTo() );
                }
            }

            if ( !map.isEmpty() )
            {
                events.forEach( e -> e.getRelationshipItems().addAll( map.get( e.getUid() ) ) );
            }

            return events;
        } );

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

    private void validateIdentifiersPresence( ResultSet rowSet, IdSchemes idSchemes )
        throws SQLException
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

        if ( StringUtils.isEmpty( rowSet.getString( "coc_identifier" ) ) )
        {
            throw new IllegalStateException(
                String.format( "CategoryOptionCombo %s does not have a value assigned for idScheme %s",
                    rowSet.getString( "coc_uid" ), idSchemes.getCategoryOptionComboIdScheme().name() ) );
        }
    }

    private String getEventSelectIdentifiersByIdScheme( EventSearchParams params )
    {
        IdSchemes idSchemes = params.getIdSchemes();

        StringBuilder sqlBuilder = new StringBuilder();

        String ouTableName = getOuTableName( params );

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getOrgUnitIdScheme(),
            ouTableName + ".uid as ou_identifier, ",
            ouTableName + ".attributevalues #>> '{%s, value}' as ou_identifier, ",
            ouTableName + ".code as ou_identifier, " ) );

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getProgramIdScheme(),
            "p.uid as p_identifier, ",
            "p.attributevalues #>> '{%s, value}' as p_identifier, ",
            "p.code as p_identifier, " ) );

        sqlBuilder.append( getIdSqlBasedOnIdScheme( idSchemes.getProgramStageIdScheme(),
            "ps.uid as ps_identifier, ",
            "ps.attributevalues #>> '{%s, value}' as ps_identifier, ",
            "ps.code as ps_identifier, " ) );

        sqlBuilder
            .append( getIdSqlBasedOnIdScheme( idSchemes.getCategoryOptionComboIdScheme(),
                "coc.uid as coc_identifier, ",
                "coc.attributevalues #>> '{%s, value}' as coc_identifier, ",
                "coc.code as coc_identifier, " ) );

        return sqlBuilder.toString();
    }

    @Override
    public int getEventCount( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();
        setAccessiblePrograms( user, params );

        String sql;

        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

        if ( params.hasFilters() )
        {
            sql = buildGridSql( user, params, mapSqlParameterSource );
        }
        else
        {
            sql = getEventSelectQuery( params, mapSqlParameterSource, user );
        }

        sql = sql.replaceFirst( "select .*? from", "select count(*) from" );

        sql = sql.replaceFirst( "order .*? (desc|asc)", "" );

        sql = sql.replaceFirst( "limit \\d+ offset \\d+", "" );

        log.debug( "Event query count SQL: " + sql );

        return jdbcTemplate.queryForObject( sql, mapSqlParameterSource, Integer.class );
    }

    private String buildGridSql( User user, EventSearchParams params, MapSqlParameterSource mapSqlParameterSource )
    {
        SqlHelper hlp = new SqlHelper();

        StringBuilder selectBuilder = new StringBuilder().append( "select " )
            .append( COLUMNS_ALIAS_MAP.entrySet()
                .stream()
                .map( col -> col.getKey() + " as " + col.getValue() )
                .collect( Collectors.joining( ", " ) ) );

        return selectBuilder.append(
            getFromWhereClause( user, params, dataElementAndFiltersSql( params, mapSqlParameterSource, hlp,
                selectBuilder ), mapSqlParameterSource, hlp ) )
            .append( getGridOrderQuery( params ) )
            .append( getEventPagingQuery( params ) ).toString();
    }

    /**
     * Query is based on three sub queries on event, data value and comment,
     * which are joined using program stage instance id. The purpose of the
     * separate queries is to be able to page properly on events.
     */
    private String buildSql( EventSearchParams params, MapSqlParameterSource mapSqlParameterSource,
        User user )
    {
        StringBuilder sqlBuilder = new StringBuilder().append( "select * from (" );

        sqlBuilder.append( getEventSelectQuery( params, mapSqlParameterSource, user ) );

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

    /**
     * Generates a single INNER JOIN for each attribute we are searching on. We
     * can search by a range of operators. All searching is using lower() since
     * attribute values are case-insensitive.
     *
     * @param attributes
     * @param filterItems
     */
    private void joinAttributeValueWithoutQueryParameter( StringBuilder attributes, List<QueryItem> filterItems )
    {
        for ( QueryItem queryItem : filterItems )
        {
            String teaValueCol = statementBuilder.columnQuote( queryItem.getItemId() );
            String teaCol = statementBuilder.columnQuote( queryItem.getItemId() + "ATT" );

            attributes
                .append( " INNER JOIN trackedentityattributevalue " )
                .append( teaValueCol )
                .append( " ON " )
                .append( teaValueCol + ".trackedentityinstanceid" )
                .append( " = TEI.trackedentityinstanceid " )
                .append( " INNER JOIN trackedentityattribute " )
                .append( teaCol )
                .append( " ON " )
                .append( teaValueCol + ".trackedentityattributeid" )
                .append( EQUALS )
                .append( teaCol + ".trackedentityattributeid" )
                .append( AND )
                .append( teaCol + ".UID" )
                .append( EQUALS )
                .append( statementBuilder.encode( queryItem.getItem().getUid(), true ) );

            attributes.append( getAttributeFilterQuery( queryItem, teaCol, teaValueCol ) );
        }
    }

    private String getAttributeFilterQuery( QueryItem queryItem, String teaCol, String teaValueCol )
    {
        StringBuilder query = new StringBuilder();

        if ( !queryItem.getFilters().isEmpty() )
        {
            query.append( AND );

            // In SQL the order of expressions linked by AND is not
            // guaranteed.
            // So when casting to number we need to be sure that the value
            // to cast is really a number.
            if ( queryItem.isNumeric() )
            {
                query
                    .append( " CASE WHEN " )
                    .append( lower( teaCol + ".valueType" ) )
                    .append( " in (" )
                    .append( NUMERIC_TYPES.stream()
                        .map( Enum::name )
                        .map( StringUtils::lowerCase )
                        .map( SqlUtils::singleQuote )
                        .collect( Collectors.joining( "," ) ) )
                    .append( ")" )
                    .append( " THEN " );
            }

            List<String> filterStrings = new ArrayList<>();
            for ( QueryFilter filter : queryItem.getFilters() )
            {
                StringBuilder filterString = new StringBuilder();
                final String queryCol = queryItem.isNumeric() ? castToNumber( teaValueCol + ".value" )
                    : lower( teaValueCol + ".value" );
                final Object encodedFilter = queryItem.isNumeric() ? Double.valueOf( filter.getFilter() )
                    : StringUtils.lowerCase( filter.getSqlFilter( filter.getFilter() ) );
                filterString
                    .append( queryCol )
                    .append( SPACE )
                    .append( filter.getSqlOperator() )
                    .append( SPACE )
                    .append( encodedFilter );
                filterStrings.add( filterString.toString() );
            }
            query.append( String.join( AND, filterStrings ) );

            if ( queryItem.isNumeric() )
            {
                query.append( " END " );
            }
        }

        return query.toString();
    }

    private String getEventSelectQuery( EventSearchParams params, MapSqlParameterSource mapSqlParameterSource,
        User user )
    {
        SqlHelper hlp = new SqlHelper();

        StringBuilder selectBuilder = new StringBuilder().append( "select " )
            .append( getEventSelectIdentifiersByIdScheme( params ) )
            .append( " psi.uid as psi_uid, " )
            .append( "ou.uid as ou_uid, p.uid as p_uid, ps.uid as ps_uid, " )
            .append(
                "psi.programstageinstanceid as psi_id, psi.status as psi_status, psi.executiondate as psi_executiondate, " )
            .append(
                "psi.eventdatavalues as psi_eventdatavalues, psi.duedate as psi_duedate, psi.completedby as psi_completedby, psi.storedby as psi_storedby, " )
            .append(
                "psi.created as psi_created, psi.createdbyuserinfo as psi_createdbyuserinfo, psi.lastupdated as psi_lastupdated, psi.lastupdatedbyuserinfo as psi_lastupdatedbyuserinfo, " )
            .append( "psi.completeddate as psi_completeddate, psi.deleted as psi_deleted, " )
            .append(
                "ST_AsText( psi.geometry ) as psi_geometry, au.uid as user_assigned, (au.firstName || ' ' || au.surName) as user_assigned_name," )
            .append( "au.firstName as user_assigned_first_name, au.surName as user_assigned_surname, " )
            .append( "au.username as user_assigned_username," )
            .append( "coc.uid as coc_uid, " )
            .append( "coc_agg.co_uids AS co_uids, " )
            .append( "coc_agg.co_count AS option_size, " );

        for ( OrderParam orderParam : params.getAttributeOrders() )
        {
            selectBuilder.append( quote( orderParam.getField() ) )
                .append( ".value AS " )
                .append( orderParam.getField() )
                .append( "_value, " );
        }

        return selectBuilder.append(
            "pi.uid as pi_uid, pi.status as pi_status, pi.followup as pi_followup, pi.enrollmentdate as pi_enrollmentdate, pi.incidentdate as pi_incidentdate, " )
            .append( "p.type as p_type, ps.uid as ps_uid, ou.name as ou_name, " )
            .append(
                "tei.trackedentityinstanceid as tei_id, tei.uid as tei_uid, teiou.uid as tei_ou, teiou.name as tei_ou_name, tei.created as tei_created, tei.inactive as tei_inactive " )
            .append( getFromWhereClause( params, mapSqlParameterSource, user, hlp,
                dataElementAndFiltersSql( params, mapSqlParameterSource, hlp,
                    selectBuilder ) ) )
            .toString();
    }

    private boolean checkForOwnership( EventSearchParams params )
    {
        return Optional.ofNullable( params.getProgram() )
            .filter( p -> Objects.nonNull( p.getProgramType() ) && p.getProgramType() == ProgramType.WITH_REGISTRATION )
            .isPresent();
    }

    private String getOuTableName( EventSearchParams params )
    {
        return checkForOwnership( params ) ? " psiou" : " ou";
    }

    private StringBuilder getFromWhereClause( EventSearchParams params, MapSqlParameterSource mapSqlParameterSource,
        User user, SqlHelper hlp, StringBuilder dataElementAndFiltersSql )
    {
        StringBuilder fromBuilder = new StringBuilder( " from programstageinstance psi " )
            .append( "inner join programinstance pi on pi.programinstanceid=psi.programinstanceid " )
            .append( "inner join program p on p.programid=pi.programid " )
            .append( "inner join programstage ps on ps.programstageid=psi.programstageid " );

        if ( checkForOwnership( params ) )
        {
            fromBuilder.append(
                "left join trackedentityprogramowner po on (pi.trackedentityinstanceid=po.trackedentityinstanceid) " )
                .append(
                    "inner join organisationunit psiou on (coalesce(po.organisationunitid, psi.organisationunitid)=psiou.organisationunitid) " )
                .append( "inner join organisationunit ou on (psi.organisationunitid=ou.organisationunitid) " );
        }
        else
        {
            fromBuilder.append( "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid " );
        }

        fromBuilder
            .append( "left join trackedentityinstance tei on tei.trackedentityinstanceid=pi.trackedentityinstanceid " )
            .append( "left join organisationunit teiou on (tei.organisationunitid=teiou.organisationunitid) " )
            .append( "left join userinfo au on (psi.assigneduserid=au.userinfoid) " );

        if ( !params.getFilterAttributes().isEmpty() )
        {
            joinAttributeValueWithoutQueryParameter( fromBuilder, params.getFilterAttributes() );
        }

        fromBuilder.append( getCategoryOptionComboQuery( user ) );

        fromBuilder.append( dataElementAndFiltersSql );

        if ( params.getTrackedEntity() != null )
        {
            mapSqlParameterSource.addValue( "trackedentityinstanceid", params.getTrackedEntity().getId() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " tei.trackedentityinstanceid= " )
                .append( ":trackedentityinstanceid" )
                .append( " " );
        }

        if ( params.getProgram() != null )
        {
            mapSqlParameterSource.addValue( "programid", params.getProgram()
                .getId() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " p.programid = " )
                .append( ":programid" )
                .append( " " );
        }

        if ( params.getProgramStage() != null )
        {
            mapSqlParameterSource.addValue( "programstageid", params.getProgramStage()
                .getId() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " ps.programstageid = " )
                .append( ":programstageid" )
                .append( " " );
        }

        if ( params.getProgramStatus() != null )
        {
            mapSqlParameterSource.addValue( "program_status", params.getProgramStatus()
                .name() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " pi.status = " )
                .append( ":program_status " );
        }

        if ( params.getEnrollmentEnrolledBefore() != null )
        {
            mapSqlParameterSource.addValue( "enrollmentEnrolledBefore", params.getEnrollmentEnrolledBefore(),
                Types.TIMESTAMP );
            fromBuilder
                .append( hlp.whereAnd() )
                .append( " (pi.enrollmentdate <= :enrollmentEnrolledBefore ) " );
        }

        if ( params.getEnrollmentEnrolledAfter() != null )
        {
            mapSqlParameterSource.addValue( "enrollmentEnrolledAfter", params.getEnrollmentEnrolledAfter(),
                Types.TIMESTAMP );
            fromBuilder
                .append( hlp.whereAnd() )
                .append( " (pi.enrollmentdate >= :enrollmentEnrolledAfter ) " );
        }

        if ( params.getEnrollmentOccurredBefore() != null )
        {
            mapSqlParameterSource.addValue( "enrollmentOccurredBefore", params.getEnrollmentOccurredBefore(),
                Types.TIMESTAMP );
            fromBuilder
                .append( hlp.whereAnd() )
                .append( " (pi.incidentdate <= :enrollmentOccurredBefore ) " );
        }

        if ( params.getEnrollmentOccurredAfter() != null )
        {
            mapSqlParameterSource.addValue( "enrollmentOccurredAfter", params.getEnrollmentOccurredAfter(),
                Types.TIMESTAMP );
            fromBuilder
                .append( hlp.whereAnd() )
                .append( " (pi.incidentdate >= :enrollmentOccurredAfter ) " );
        }

        if ( params.getScheduleAtStartDate() != null )
        {
            mapSqlParameterSource.addValue( "startDueDate", params.getScheduleAtStartDate(), Types.TIMESTAMP );

            fromBuilder
                .append( hlp.whereAnd() )
                .append( " (psi.duedate is not null and psi.duedate >= :startDueDate ) " );
        }

        if ( params.getScheduleAtEndDate() != null )
        {
            mapSqlParameterSource.addValue( "endDueDate", params.getScheduleAtEndDate(), Types.TIMESTAMP );

            fromBuilder
                .append( hlp.whereAnd() )
                .append( " (psi.duedate is not null and psi.duedate <= :endDueDate ) " );
        }

        if ( params.getFollowUp() != null )
        {
            fromBuilder.append( hlp.whereAnd() )
                .append( " pi.followup is " )
                .append( Boolean.TRUE.equals( params.getFollowUp() ) ? "true" : "false" )
                .append( " " );
        }

        fromBuilder.append( addLastUpdatedFilters( params, mapSqlParameterSource, hlp, true ) );

        // Comparing milliseconds instead of always creating new Date( 0 );
        if ( params.getSkipChangedBefore() != null && params.getSkipChangedBefore()
            .getTime() > 0 )
        {
            mapSqlParameterSource.addValue( "skipChangedBefore", params.getSkipChangedBefore(), Types.TIMESTAMP );

            fromBuilder.append( hlp.whereAnd() )
                .append( PSI_LASTUPDATED_GT )
                .append( ":skipChangedBefore" )
                .append( " " );
        }

        if ( params.getCategoryOptionCombo() != null )
        {
            mapSqlParameterSource.addValue( "attributeoptioncomboid", params.getCategoryOptionCombo()
                .getId() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " psi.attributeoptioncomboid = " )
                .append( ":attributeoptioncomboid" )
                .append( " " );
        }

        String orgUnitSql = getOrgUnitSql( user, params, getOuTableName( params ) );

        if ( orgUnitSql != null )
        {
            fromBuilder.append( hlp.whereAnd() )
                .append( " (" )
                .append( orgUnitSql )
                .append( ") " );
        }

        if ( params.getStartDate() != null )
        {
            mapSqlParameterSource.addValue( "startDate", params.getStartDate(), Types.DATE );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (psi.executiondate >= " )
                .append( ":startDate" )
                .append( " or (psi.executiondate is null and psi.duedate >= " )
                .append( ":startDate" )
                .append( " )) " );
        }

        if ( params.getEndDate() != null )
        {
            mapSqlParameterSource.addValue( "endDate", addDays( params.getEndDate(), 1 ), Types.DATE );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (psi.executiondate < " )
                .append( ":endDate" )
                .append( " or (psi.executiondate is null and psi.duedate < " )
                .append( ":endDate" )
                .append( " )) " );
        }

        if ( params.getProgramType() != null )
        {
            mapSqlParameterSource.addValue( "programType", params.getProgramType()
                .name() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " p.type = " )
                .append( ":programType" )
                .append( " " );
        }

        fromBuilder.append( eventStatusSql( params, mapSqlParameterSource, hlp ) );

        if ( params.getEvents() != null && !params.getEvents()
            .isEmpty() && !params.hasFilters() )
        {
            mapSqlParameterSource.addValue( "psi_uid", params.getEvents() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (psi.uid in (" )
                .append( ":psi_uid" )
                .append( ")) " );
        }

        if ( params.getAssignedUserQueryParam().hasAssignedUsers() )
        {
            mapSqlParameterSource.addValue( "au_uid", params.getAssignedUserQueryParam().getAssignedUsers() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (au.uid in (" )
                .append( ":au_uid" )
                .append( ")) " );
        }

        if ( AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode() )
        {
            fromBuilder.append( hlp.whereAnd() )
                .append( " (au.uid is null) " );
        }

        if ( AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode() )
        {
            fromBuilder.append( hlp.whereAnd() )
                .append( " (au.uid is not null) " );
        }

        if ( !params.isIncludeDeleted() )
        {
            fromBuilder.append( hlp.whereAnd() )
                .append( " psi.deleted is false " );
        }

        if ( params.hasSecurityFilter() )
        {
            mapSqlParameterSource.addValue( "program_uid", params.getAccessiblePrograms()
                .isEmpty() ? null : params.getAccessiblePrograms() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (p.uid in (" )
                .append( ":program_uid" )
                .append( ")) " );

            mapSqlParameterSource.addValue( "programstage_uid", params.getAccessibleProgramStages()
                .isEmpty() ? null : params.getAccessibleProgramStages() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (ps.uid in (" )
                .append( ":programstage_uid" )
                .append( ")) " );
        }

        if ( params.isSynchronizationQuery() )
        {
            fromBuilder.append( hlp.whereAnd() )
                .append( " psi.lastupdated > psi.lastsynchronized " );
        }

        if ( !CollectionUtils.isEmpty( params.getProgramInstances() ) )
        {
            mapSqlParameterSource.addValue( "programinstance_uid", params.getProgramInstances() );

            fromBuilder.append( hlp.whereAnd() )
                .append( " (pi.uid in (:programinstance_uid)) " );
        }

        return fromBuilder;
    }

    /**
     * For dataElement params, restriction is set in inner join. For query
     * params, restriction is set in where clause.
     */
    private StringBuilder dataElementAndFiltersSql( EventSearchParams params,
        MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp, StringBuilder selectBuilder )
    {
        int filterCount = 0;

        StringBuilder optionValueJoinBuilder = new StringBuilder();
        StringBuilder optionValueConditionBuilder = new StringBuilder();
        StringBuilder eventDataValuesWhereSql = new StringBuilder();
        Set<String> joinedColumns = new HashSet<>();

        for ( QueryItem item : params.getDataElementsAndFilters() )
        {
            ++filterCount;

            final String itemId = item.getItemId();

            final String dataValueValueSql = "psi.eventdatavalues #>> '{" + itemId + ", value}'";

            selectBuilder.append( ", " )
                .append( item.isNumeric() ? castToNumber( dataValueValueSql ) : lower( dataValueValueSql ) )
                .append( " as " )
                .append( itemId );

            String optValueTableAs = "opt_" + filterCount;

            if ( !joinedColumns.contains( itemId ) && item.hasOptionSet() && item.hasFilter() )
            {
                String optSetBind = "optset_" + filterCount;

                mapSqlParameterSource.addValue( optSetBind, item.getOptionSet()
                    .getId() );

                optionValueJoinBuilder.append( "inner join optionvalue as " )
                    .append( optValueTableAs )
                    .append( " on lower(" )
                    .append( optValueTableAs )
                    .append( ".code) = " )
                    .append( "lower(" )
                    .append( dataValueValueSql )
                    .append( ") and " )
                    .append( optValueTableAs )
                    .append( ".optionsetid = " )
                    .append( ":" )
                    .append( optSetBind )
                    .append( " " );

                joinedColumns.add( itemId );
            }

            if ( item.hasFilter() )
            {
                for ( QueryFilter filter : item.getFilters() )
                {
                    ++filterCount;

                    final String queryCol = item.isNumeric() ? castToNumber( dataValueValueSql )
                        : lower( dataValueValueSql );

                    String bindParameter = "parameter_" + filterCount;
                    int itemType = item.isNumeric() ? Types.NUMERIC : Types.VARCHAR;

                    if ( !item.hasOptionSet() )
                    {
                        eventDataValuesWhereSql.append( hlp.whereAnd() );

                        if ( QueryOperator.IN.getValue()
                            .equalsIgnoreCase( filter.getSqlOperator() ) )
                        {
                            mapSqlParameterSource.addValue( bindParameter,
                                QueryFilter.getFilterItems( StringUtils.lowerCase( filter.getFilter() ) ), itemType );

                            eventDataValuesWhereSql.append( inCondition( filter, bindParameter, queryCol ) );
                        }
                        else
                        {
                            mapSqlParameterSource.addValue( bindParameter,
                                StringUtils.lowerCase( filter.getSqlBindFilter() ), itemType );

                            eventDataValuesWhereSql.append( " " )
                                .append( queryCol )
                                .append( " " )
                                .append( filter.getSqlOperator() )
                                .append( " " )
                                .append( ":" )
                                .append( bindParameter )
                                .append( " " );
                        }
                    }
                    else
                    {
                        if ( QueryOperator.IN.getValue()
                            .equalsIgnoreCase( filter.getSqlOperator() ) )
                        {
                            mapSqlParameterSource.addValue( bindParameter,
                                QueryFilter.getFilterItems( StringUtils.lowerCase( filter.getFilter() ) ), itemType );

                            optionValueConditionBuilder.append( " and " );
                            optionValueConditionBuilder.append( inCondition( filter, bindParameter, queryCol ) );
                        }
                        else
                        {
                            mapSqlParameterSource.addValue( bindParameter,
                                StringUtils.lowerCase( filter.getSqlBindFilter() ), itemType );

                            optionValueConditionBuilder.append( "and lower(" )
                                .append( optValueTableAs )
                                .append( DOT_NAME )
                                .append( " " )
                                .append( filter.getSqlOperator() )
                                .append( " " )
                                .append( ":" )
                                .append( bindParameter )
                                .append( " " );
                        }
                    }
                }
            }
        }

        return optionValueJoinBuilder.append( optionValueConditionBuilder )
            .append( eventDataValuesWhereSql )
            .append( " " );
    }

    private String inCondition( QueryFilter filter, String boundParameter, String queryCol )
    {
        return new StringBuilder().append( " " )
            .append( queryCol )
            .append( " " )
            .append( filter.getSqlOperator() )
            .append( " " )
            .append( "(" )
            .append( ":" )
            .append( boundParameter )
            .append( ") " )
            .toString();
    }

    private String getFromWhereClause( User user, EventSearchParams params, StringBuilder dataElementAndFiltersSql,
        MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp )
    {
        StringBuilder sqlBuilder = new StringBuilder().append(
            " from programstageinstance psi "
                + "inner join programinstance pi on pi.programinstanceid = psi.programinstanceid "
                + "inner join program p on p.programid = pi.programid "
                + "inner join programstage ps on ps.programstageid = psi.programstageid "
                + "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = psi.attributeoptioncomboid "
                + "left join userinfo au on (psi.assigneduserid=au.userinfoid) " );

        if ( checkForOwnership( params ) )
        {
            sqlBuilder.append(
                "left join trackedentityprogramowner po on (pi.trackedentityinstanceid=po.trackedentityinstanceid) " )
                .append(
                    "inner join organisationunit psiou on (coalesce(po.organisationunitid, psi.organisationunitid)=psiou.organisationunitid) " )
                .append( "left join organisationunit ou on (psi.organisationunitid=ou.organisationunitid) " );
        }
        else
        {
            sqlBuilder.append( "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid " );
        }

        sqlBuilder.append( dataElementAndFiltersSql );

        String orgUnitSql = getOrgUnitSql( user, params, getOuTableName( params ) );

        if ( orgUnitSql != null )
        {
            sqlBuilder.append( hlp.whereAnd() ).append( orgUnitSql + " " );
        }

        if ( params.getProgramStage() != null )
        {
            mapSqlParameterSource.addValue( "programstageid", params.getProgramStage().getId() );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " ps.programstageid = " )
                .append( ":programstageid" )
                .append( " " );
        }

        if ( params.getCategoryOptionCombo() != null )
        {
            mapSqlParameterSource.addValue( "attributeoptioncomboid", params.getCategoryOptionCombo()
                .getId() );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " psi.attributeoptioncomboid = " )
                .append( ":attributeoptioncomboid" )
                .append( " " );
        }

        if ( params.getStartDate() != null )
        {
            mapSqlParameterSource.addValue( "startDate", params.getStartDate(), Types.DATE );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " (psi.executiondate >= " )
                .append( ":startDate" )
                .append( " or (psi.executiondate is null and psi.duedate >= " )
                .append( ":startDate" )
                .append( " )) " );
        }

        if ( params.getEndDate() != null )
        {
            mapSqlParameterSource.addValue( "endDate", addDays( params.getEndDate(), 1 ), Types.DATE );

            sqlBuilder.append( hlp.whereAnd() ).append( " (psi.executiondate < " )
                .append( ":endDate " )
                .append( " or (psi.executiondate is null and psi.duedate < " )
                .append( ":endDate" ).append( " )) " );
        }

        sqlBuilder.append( addLastUpdatedFilters( params, mapSqlParameterSource, hlp, false ) );

        if ( params.isSynchronizationQuery() )
        {
            sqlBuilder.append( hlp.whereAnd() )
                .append( " psi.lastupdated > psi.lastsynchronized " );
        }

        // Comparing milliseconds instead of always creating new Date( 0 )

        if ( params.getSkipChangedBefore() != null && params.getSkipChangedBefore()
            .getTime() > 0 )
        {
            mapSqlParameterSource.addValue( "skipChangedBefore", params.getSkipChangedBefore(), Types.TIMESTAMP );

            sqlBuilder.append( hlp.whereAnd() )
                .append( PSI_LASTUPDATED_GT )
                .append( ":skipChangedBefore " );
        }

        if ( params.getScheduleAtStartDate() != null )
        {
            mapSqlParameterSource.addValue( "startDueDate", params.getScheduleAtStartDate(), Types.DATE );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " psi.duedate is not null and psi.duedate >= " )
                .append( ":dueDate" )
                .append( " " );
        }

        if ( params.getScheduleAtEndDate() != null )
        {
            mapSqlParameterSource.addValue( "endDueDate", params.getScheduleAtEndDate(), Types.DATE );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " psi.duedate is not null and psi.duedate <= " )
                .append( ":endDueDate" )
                .append( " " );
        }

        if ( !params.isIncludeDeleted() )
        {
            sqlBuilder.append( hlp.whereAnd() )
                .append( " psi.deleted is false " );
        }

        if ( !CollectionUtils.isEmpty( params.getProgramInstances() ) )
        {
            mapSqlParameterSource.addValue( "programinstance_uid", params.getProgramInstances() );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " (pi.uid in (:programinstance_uid)) " );
        }

        sqlBuilder.append( eventStatusSql( params, mapSqlParameterSource, hlp ) );

        if ( params.getEvents() != null && !params.getEvents()
            .isEmpty() && !params.hasFilters() )
        {
            mapSqlParameterSource.addValue( "psi_uid", params.getEvents() );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " (psi.uid in (" )
                .append( ":psi_uid" )
                .append( ")) " );
        }

        if ( params.getAssignedUserQueryParam().hasAssignedUsers() )
        {
            mapSqlParameterSource.addValue( "au_uid", params.getAssignedUserQueryParam().getAssignedUsers() );

            sqlBuilder.append( hlp.whereAnd() )
                .append( " (au.uid in (" )
                .append( ":au_uid" )
                .append( ")) " );
        }

        if ( AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode() )
        {
            sqlBuilder.append( hlp.whereAnd() )
                .append( " (au.uid is null) " );
        }

        if ( AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode() )
        {
            sqlBuilder.append( hlp.whereAnd() )
                .append( " (au.uid is not null) " );
        }

        return sqlBuilder.toString();
    }

    private String eventStatusSql( EventSearchParams params, MapSqlParameterSource mapSqlParameterSource,
        SqlHelper hlp )
    {
        StringBuilder stringBuilder = new StringBuilder();

        if ( params.getEventStatus() != null )
        {
            if ( params.getEventStatus() == EventStatus.VISITED )
            {
                mapSqlParameterSource.addValue( PSI_STATUS, EventStatus.ACTIVE.name() );

                stringBuilder.append( hlp.whereAnd() )
                    .append( PSI_STATUS_EQ )
                    .append( ":" + PSI_STATUS )
                    .append( " and psi.executiondate is not null " );
            }
            else if ( params.getEventStatus() == EventStatus.OVERDUE )
            {
                mapSqlParameterSource.addValue( PSI_STATUS, EventStatus.SCHEDULE.name() );

                stringBuilder.append( hlp.whereAnd() )
                    .append( " date(now()) > date(psi.duedate) and psi.status = " )
                    .append( ":" + PSI_STATUS )
                    .append( " " );
            }
            else
            {
                mapSqlParameterSource.addValue( PSI_STATUS, params.getEventStatus()
                    .name() );

                stringBuilder.append( hlp.whereAnd() )
                    .append( PSI_STATUS_EQ )
                    .append( ":" + PSI_STATUS )
                    .append( " " );
            }
        }

        return stringBuilder.toString();
    }

    private String addLastUpdatedFilters( EventSearchParams params, MapSqlParameterSource mapSqlParameterSource,
        SqlHelper hlp, boolean useDateAfterEndDate )
    {
        StringBuilder sqlBuilder = new StringBuilder();

        if ( params.hasUpdatedAtDuration() )
        {
            mapSqlParameterSource.addValue( "lastUpdated", DateUtils.offSetDateTimeFrom(
                DateUtils.nowMinusDuration( params.getUpdatedAtDuration() ) ), Types.TIMESTAMP_WITH_TIMEZONE );

            sqlBuilder.append( hlp.whereAnd() )
                .append( PSI_LASTUPDATED_GT )
                .append( ":lastUpdated" )
                .append( " " );
        }
        else
        {
            if ( params.hasUpdatedAtStartDate() )
            {
                mapSqlParameterSource.addValue( "lastUpdatedStart", params.getUpdatedAtStartDate(), Types.TIMESTAMP );

                sqlBuilder.append( hlp.whereAnd() )
                    .append( PSI_LASTUPDATED_GT )
                    .append( ":lastUpdatedStart" )
                    .append( " " );
            }

            if ( params.hasUpdatedAtEndDate() )
            {
                if ( useDateAfterEndDate )
                {
                    mapSqlParameterSource.addValue( "lastUpdatedEnd", addDays( params.getUpdatedAtEndDate(), 1 ),
                        Types.TIMESTAMP );

                    sqlBuilder.append( hlp.whereAnd() )
                        .append( " psi.lastupdated < " )
                        .append( ":lastUpdatedEnd" )
                        .append( " " );
                }
                else
                {
                    mapSqlParameterSource.addValue( "lastUpdatedEnd", params.getUpdatedAtEndDate(), Types.TIMESTAMP );

                    sqlBuilder.append( hlp.whereAnd() )
                        .append( " psi.lastupdated <= " )
                        .append( ":lastUpdatedEnd" )
                        .append( " " );
                }
            }
        }

        return sqlBuilder.toString();
    }

    /**
     * Returns the joins and sub-queries needed to fulfill all the needs
     * regarding category option combo and category options. Category option
     * combos (COC) are composed of category options (CO), one per category of
     * the COCs category combination (CC).
     *
     * Important constraints leading to this query:
     * <ul>
     * <li>While COCs are pre-computed and can be seen as a de-normalization of
     * the possible permutations the COs in a COC are stored in a normalized
     * way. The final event should have its attributeCategoryOptions field
     * populated with a semicolon separated string of its COCs COs. We thus need
     * to aggregate these COs for each event.</li>
     * <li>COCs should be returned in the user specified idScheme. So in order
     * to have access to uid, code, name, attributes we need another join as all
     * of these fields cannot be added to the above aggregation. IdSchemes
     * SELECT are handled in {@link #getEventSelectIdentifiersByIdScheme}.</li>
     * <li>A user must have access to all COs of the events COC to have access
     * to an event.</li>
     * </ul>
     */
    private String getCategoryOptionComboQuery( User user )
    {
        String joinCondition = "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = psi.attributeoptioncomboid "
            +
            " inner join (select coc.categoryoptioncomboid as id," +
            " string_agg(co.uid, ';') as co_uids, count(co.categoryoptionid) as co_count" +
            " from categoryoptioncombo coc " +
            " inner join categoryoptioncombos_categoryoptions cocco on coc.categoryoptioncomboid = cocco.categoryoptioncomboid"
            +
            " inner join dataelementcategoryoption co on cocco.categoryoptionid = co.categoryoptionid" +
            " group by coc.categoryoptioncomboid ";

        if ( !isSuper( user ) )
        {
            joinCondition = joinCondition + " having bool_and(case when "
                + JpaQueryUtils.generateSQlQueryForSharingCheck( "co.sharing", user, AclService.LIKE_READ_DATA )
                + " then true else false end) = True ";
        }

        return joinCondition + ") as coc_agg on coc_agg.id = psi.attributeoptioncomboid ";
    }

    private String getEventPagingQuery( final EventSearchParams params )
    {
        final StringBuilder sqlBuilder = new StringBuilder().append( " " );
        int pageSize = params.getPageSizeWithDefault();

        // When the clients choose to not show the total of pages.
        if ( !params.isTotalPages() )
        {
            // Get pageSize + 1, so we are able to know if there is another
            // page available. It adds one additional element into the list,
            // as consequence. The caller needs to remove the last element.
            pageSize++;
        }

        if ( !params.isSkipPaging() )
        {
            sqlBuilder.append( "limit " )
                .append( pageSize )
                .append( " offset " )
                .append( params.getOffset() )
                .append( " " );
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

        for ( OrderParam order : params.getOrders() )
        {
            if ( QUERY_PARAM_COL_MAP.containsKey( order.getField() ) )
            {
                String orderText = QUERY_PARAM_COL_MAP.get( order.getField() );
                orderText += " " + (order.getDirection().isAscending() ? "asc" : "desc");
                orderFields.add( orderText );
            }
            else if ( params.getAttributeOrders().contains( order ) )
            {
                orderFields.add( order.getField() + "_value " + order.getDirection() );
            }
            else if ( params.getGridOrders().contains( order ) )
            {
                orderFields.add( getDataElementsOrder( params.getDataElements(), order ) );
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

    private String getDataElementsOrder( Set<QueryItem> dataElements, OrderParam order )
    {
        for ( QueryItem item : dataElements )
        {
            if ( order.getField().equals( item.getItemId() ) )
            {
                return order.getField() + " " + order.getDirection();
            }
        }

        return "";
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

    private void setAccessiblePrograms( User user, EventSearchParams params )
    {
        if ( !isSuper( user ) )
        {
            params.setAccessiblePrograms(
                manager.getDataReadAll( Program.class )
                    .stream()
                    .map( Program::getUid )
                    .collect( Collectors.toSet() ) );

            params.setAccessibleProgramStages( manager.getDataReadAll( ProgramStage.class )
                .stream()
                .map( ProgramStage::getUid )
                .collect( Collectors.toSet() ) );
        }
    }

    private String getOrgUnitSql( User user, EventSearchParams params, String ouTable )
    {
        OrganisationUnitSelectionMode orgUnitSelectionMode = params.getOrgUnitSelectionMode();

        if ( orgUnitSelectionMode == null )
        {
            if ( params.getOrgUnit() != null )
            {
                return getSelectedOrgUnitsPath( params, ouTable );
            }

            return getAccessibleOrgUnitsPath( params, user, ouTable );
        }

        return switch (orgUnitSelectionMode) {
            case ALL -> null;
            case CHILDREN -> getChildrenOrgUnitsPath(params, ouTable);
            case DESCENDANTS -> getDescendantOrgUnitsPath(params, ouTable);
            case CAPTURE -> getCaptureOrgUnitsPath(user, ouTable);
            case SELECTED -> getSelectedOrgUnitsPath(params, ouTable);
            default -> getAccessibleOrgUnitsPath(params, user, ouTable);
        };
    }

    private String getChildrenOrgUnitsPath( EventSearchParams params, String ouTable )
    {
        return ouTable + "." + PATH_LIKE + " '" + params.getOrgUnit().getPath() + "%' " + " and " + ouTable + "."
            + "hierarchylevel = " + (params.getOrgUnit().getLevel() + 1);
    }

    private String getSelectedOrgUnitsPath( EventSearchParams params, String ouTable )
    {
        return ouTable + "." + PATH_EQ + " '" + params.getOrgUnit().getPath() + "' ";
    }

    private String getDescendantOrgUnitsPath( EventSearchParams params, String ouTable )
    {
        return ouTable + "." + PATH_LIKE + " '" + params.getOrgUnit().getPath() + "%' ";
    }

    private String getCaptureOrgUnitsPath( User user, String ouTable )
    {
        if ( user == null )
        {
            return null;
        }

        List<String> orgUnitPaths = new ArrayList<>();

        for ( OrganisationUnit organisationUnit : user.getOrganisationUnits().stream().toList() )
        {
            orgUnitPaths.add( ouTable + "." + PATH_LIKE + " '" + organisationUnit.getPath() + "%' " );
        }

        return String.join( " OR ", orgUnitPaths );
    }

    private String getAccessibleOrgUnitsPath( EventSearchParams params, User user, String ouTable )
    {
        if ( user == null )
        {
            return null;
        }

        List<String> orgUnitPaths = new ArrayList<>();

        List<OrganisationUnit> organisationUnits = new ArrayList<>( user.getTeiSearchOrganisationUnitsWithFallback() );

        if ( params.getProgram() == null || params.getProgram().isClosed() || params.getProgram().isProtected() )
        {
            organisationUnits = new ArrayList<>( user.getOrganisationUnits() );
        }

        if ( organisationUnits.isEmpty() )
        {
            return null;
        }

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            orgUnitPaths.add( ouTable + "." + PATH_LIKE + " '" + organisationUnit.getPath() + "%' " );
        }

        return String.join( " OR ", orgUnitPaths );
    }
}
