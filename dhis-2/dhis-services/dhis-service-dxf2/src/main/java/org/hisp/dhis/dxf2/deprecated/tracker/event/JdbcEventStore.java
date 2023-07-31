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
package org.hisp.dhis.dxf2.deprecated.tracker.event;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.ValueType.NUMERIC_TYPES;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.AbstractEventService.STATIC_EVENT_COLUMNS;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ATTRIBUTE_OPTION_COMBO_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_COMPLETED_BY_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_COMPLETED_DATE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_CREATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_CREATED_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_DELETED;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_DUE_DATE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ENROLLMENT_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_EXECUTION_DATE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_GEOMETRY;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_LAST_UPDATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_LAST_UPDATED_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ORG_UNIT_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_ORG_UNIT_NAME;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_PROGRAM_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_PROGRAM_STAGE_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_STATUS_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams.EVENT_STORED_BY_ID;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.eventDataValuesToJson;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.jsonToUserInfo;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.userInfoToJson;
import static org.hisp.dhis.system.util.SqlUtils.castToNumber;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.addDays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.deprecated.tracker.report.EventRow;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Attribute;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.EventQuery;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jdbc.BatchPreparedStatementSetterWithKeyHolder;
import org.hisp.dhis.jdbc.JdbcUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository("org.hisp.dhis.dxf2.events.event.EventStore")
@RequiredArgsConstructor
public class JdbcEventStore implements EventStore {
  private static final String RELATIONSHIP_IDS_QUERY =
      " left join (select ri.eventid as ri_psi_id, json_agg(ri.relationshipid) as psi_rl FROM relationshipitem ri"
          + " GROUP by ri_psi_id)  as fgh on fgh.ri_psi_id=event.psi_id ";

  private static final String PSI_EVENT_COMMENT_QUERY =
      "select psic.eventid    as psic_id,"
          + " psinote.trackedentitycommentid as psinote_id,"
          + " psinote.commenttext            as psinote_value,"
          + " psinote.created                as psinote_storeddate,"
          + " psinote.creator                as psinote_storedby,"
          + " psinote.uid                    as psinote_uid,"
          + " psinote.lastupdated            as psinote_lastupdated,"
          + " userinfo.userinfoid            as usernote_id,"
          + " userinfo.code                  as usernote_code,"
          + " userinfo.uid                   as usernote_uid,"
          + " userinfo.username              as usernote_username,"
          + " userinfo.firstname             as userinfo_firstname,"
          + " userinfo.surname               as userinfo_surname"
          + " from eventcomments psic"
          + " inner join trackedentitycomment psinote"
          + " on psic.trackedentitycommentid = psinote.trackedentitycommentid"
          + " left join userinfo on psinote.lastupdatedby = userinfo.userinfoid ";

  private static final String PSI_STATUS = "psi_status";

  private static final String PSI_STATUS_EQ = " psi.status = ";

  private static final String PSI_LASTUPDATED_GT = " psi.lastupdated >= ";

  private static final String DOT_NAME = ".name)";

  private static final String SPACE = " ";

  private static final String EQUALS = " = ";

  private static final String AND = " AND ";

  private static final Map<String, String> QUERY_PARAM_COL_MAP =
      ImmutableMap.<String, String>builder()
          .put(EVENT_ID, "psi_uid")
          .put(EVENT_PROGRAM_ID, "p_uid")
          .put(EVENT_PROGRAM_STAGE_ID, "ps_uid")
          .put(EVENT_ENROLLMENT_ID, "pi_uid")
          .put("enrollmentStatus", "pi_status")
          .put("enrolledAt", "pi_enrollmentdate")
          .put(EVENT_ORG_UNIT_ID, "ou_uid")
          .put(EVENT_ORG_UNIT_NAME, "ou_name")
          .put("trackedEntityInstance", "tei_uid")
          .put("occurredAt", "psi_executiondate")
          .put(EVENT_EXECUTION_DATE_ID, "psi_executiondate")
          .put("followup", "pi_followup")
          .put(EVENT_STATUS_ID, PSI_STATUS)
          .put(EVENT_DUE_DATE_ID, "psi_duedate")
          .put(EVENT_STORED_BY_ID, "psi_storedby")
          .put(EVENT_LAST_UPDATED_BY_USER_INFO_ID, "psi_lastupdatedbyuserinfo")
          .put(EVENT_CREATED_BY_USER_INFO_ID, "psi_createdbyuserinfo")
          .put(EVENT_CREATED_ID, "psi_created")
          .put(EVENT_LAST_UPDATED_ID, "psi_lastupdated")
          .put(EVENT_COMPLETED_BY_ID, "psi_completedby")
          .put(EVENT_ATTRIBUTE_OPTION_COMBO_ID, "psi_aoc")
          .put(EVENT_COMPLETED_DATE_ID, "psi_completeddate")
          .put(EVENT_DELETED, "psi_deleted")
          .put("assignedUser", "user_assigned_username")
          .put("assignedUserDisplayName", "user_assigned_name")
          .build();

  private static final Map<String, String> COLUMNS_ALIAS_MAP =
      ImmutableMap.<String, String>builder()
          .put(EventQuery.COLUMNS.UID.getQueryElement().useInSelect(), EVENT_ID)
          .put(EventQuery.COLUMNS.CREATED.getQueryElement().useInSelect(), EVENT_CREATED_ID)
          .put(EventQuery.COLUMNS.UPDATED.getQueryElement().useInSelect(), EVENT_LAST_UPDATED_ID)
          .put(EventQuery.COLUMNS.STOREDBY.getQueryElement().useInSelect(), EVENT_STORED_BY_ID)
          .put("psi.createdbyuserinfo", EVENT_CREATED_BY_USER_INFO_ID)
          .put("psi.lastupdatedbyuserinfo", EVENT_LAST_UPDATED_BY_USER_INFO_ID)
          .put(
              EventQuery.COLUMNS.COMPLETEDBY.getQueryElement().useInSelect(), EVENT_COMPLETED_BY_ID)
          .put(
              EventQuery.COLUMNS.COMPLETEDDATE.getQueryElement().useInSelect(),
              EVENT_COMPLETED_DATE_ID)
          .put(EventQuery.COLUMNS.DUE_DATE.getQueryElement().useInSelect(), EVENT_DUE_DATE_ID)
          .put(
              EventQuery.COLUMNS.EXECUTION_DATE.getQueryElement().useInSelect(),
              EVENT_EXECUTION_DATE_ID)
          .put("ou.uid", EVENT_ORG_UNIT_ID)
          .put("ou.name", EVENT_ORG_UNIT_NAME)
          .put(EventQuery.COLUMNS.STATUS.getQueryElement().useInSelect(), EVENT_STATUS_ID)
          .put("pi.uid", EVENT_ENROLLMENT_ID)
          .put("ps.uid", EVENT_PROGRAM_STAGE_ID)
          .put("p.uid", EVENT_PROGRAM_ID)
          .put("coc.uid", EVENT_ATTRIBUTE_OPTION_COMBO_ID)
          .put(EventQuery.COLUMNS.DELETED.getQueryElement().useInSelect(), EVENT_DELETED)
          .put("psi.geometry", EVENT_GEOMETRY)
          .build();

  // SQL QUERIES

  private static final List<String> INSERT_COLUMNS =
      List.of(
          EventQuery.COLUMNS.ID.getColumnName(), // nextval
          "enrollmentid", // 1
          "programstageid", // 2
          EventQuery.COLUMNS.DUE_DATE.getColumnName(), // 3
          EventQuery.COLUMNS.EXECUTION_DATE.getColumnName(), // 4
          "organisationunitid", // 5
          EventQuery.COLUMNS.STATUS.getColumnName(), // 6
          EventQuery.COLUMNS.COMPLETEDDATE.getColumnName(), // 7
          EventQuery.COLUMNS.UID.getColumnName(), // 8
          EventQuery.COLUMNS.CREATED.getColumnName(), // 9
          EventQuery.COLUMNS.UPDATED.getColumnName(), // 10
          "attributeoptioncomboid", // 11
          EventQuery.COLUMNS.STOREDBY.getColumnName(), // 12
          "createdbyuserinfo", // 13
          "lastupdatedbyuserinfo", // 14
          EventQuery.COLUMNS.COMPLETEDBY.getColumnName(), // 15
          EventQuery.COLUMNS.DELETED.getColumnName(), // 16
          "code", // 17
          EventQuery.COLUMNS.CREATEDCLIENT.getColumnName(), // 18
          EventQuery.COLUMNS.UPDATEDCLIENT.getColumnName(), // 19
          EventQuery.COLUMNS.GEOMETRY.getColumnName(), // 20
          "assigneduserid", // 21
          "eventdatavalues"); // 22

  private static final String INSERT_EVENT_SQL;

  private static final List<String> UPDATE_COLUMNS =
      List.of(
          "enrollmentid", // 1
          "programstageid", // 2
          EventQuery.COLUMNS.DUE_DATE.getColumnName(), // 3
          EventQuery.COLUMNS.EXECUTION_DATE.getColumnName(), // 4
          "organisationunitid", // 5
          EventQuery.COLUMNS.STATUS.getColumnName(), // 6
          EventQuery.COLUMNS.COMPLETEDDATE.getColumnName(), // 7
          EventQuery.COLUMNS.UPDATED.getColumnName(), // 8
          "attributeoptioncomboid", // 9
          EventQuery.COLUMNS.STOREDBY.getColumnName(), // 10
          "lastupdatedbyuserinfo", // 11
          EventQuery.COLUMNS.COMPLETEDBY.getColumnName(), // 12
          EventQuery.COLUMNS.DELETED.getColumnName(), // 13
          "code", // 14
          EventQuery.COLUMNS.UPDATEDCLIENT.getColumnName(), // 15
          EventQuery.COLUMNS.GEOMETRY.getColumnName(), // 16
          "assigneduserid", // 17
          "eventdatavalues", // 18
          EventQuery.COLUMNS.UID.getColumnName()); // 19

  private static final String UPDATE_EVENT_SQL;

  private static final String PATH_LIKE = "path LIKE";

  private static final String PATH_EQ = "path =";

  /**
   * Updates Tracked Entity Instance after an event update. In order to prevent deadlocks, SELECT
   * ... FOR UPDATE SKIP LOCKED is used before the actual UPDATE statement. This prevents deadlocks
   * when Postgres tries to update the same TEI.
   */
  private static final String UPDATE_TEI_SQL =
      "SELECT * FROM trackedentity WHERE uid IN (:teiUids) FOR UPDATE SKIP LOCKED;"
          + "UPDATE trackedentity SET lastupdated = :lastUpdated, lastupdatedby = :lastUpdatedBy WHERE uid IN (:teiUids)";

  static {
    INSERT_EVENT_SQL =
        "insert into event ("
            + String.join(",", INSERT_COLUMNS)
            + ") "
            + "values ( nextval('programstageinstance_sequence'), "
            + INSERT_COLUMNS.stream().skip(1L).map(column -> "?").collect(Collectors.joining(","))
            + ")";

    UPDATE_EVENT_SQL =
        "update event set "
            + UPDATE_COLUMNS.stream()
                .map(column -> column + " = :" + column)
                .limit(UPDATE_COLUMNS.size() - 1L)
                .collect(Collectors.joining(","))
            + " where uid = :uid;";
  }

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  // Cannot use DefaultRenderService mapper. Does not work properly -
  // DHIS2-6102
  private static final ObjectReader eventDataValueJsonReader =
      JsonBinaryType.MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});

  private final StatementBuilder statementBuilder;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Qualifier("dataValueJsonMapper")
  private final ObjectMapper jsonMapper;

  private final CurrentUserService currentUserService;

  private final IdentifiableObjectManager manager;

  private final org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.EventStore eventStore;

  private final SkipLockedProvider skipLockedProvider;

  // -------------------------------------------------------------------------
  // EventStore implementation
  // -------------------------------------------------------------------------

  @Override
  public List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> getEvents(
      EventSearchParams params, Map<String, Set<String>> psdesWithSkipSyncTrue) {
    User user = currentUserService.getCurrentUser();

    setAccessiblePrograms(user, params);

    List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events = new ArrayList<>();
    List<Long> relationshipIds = new ArrayList<>();

    final Gson gson = new Gson();

    final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    String sql = buildSql(params, mapSqlParameterSource, user);

    return jdbcTemplate.query(
        sql,
        mapSqlParameterSource,
        resultSet -> {
          log.debug("Event query SQL: " + sql);

          Set<String> notes = new HashSet<>();

          while (resultSet.next()) {
            if (resultSet.getString("psi_uid") == null) {
              continue;
            }

            String psiUid = resultSet.getString("psi_uid");

            validateIdentifiersPresence(resultSet, params.getIdSchemes(), true);

            org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
                new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();

            if (!params.isSkipEventId()) {
              event.setUid(psiUid);
              event.setEvent(psiUid);
            }

            event.setTrackedEntityInstance(resultSet.getString("tei_uid"));
            event.setStatus(EventStatus.valueOf(resultSet.getString(PSI_STATUS)));

            ProgramType programType = ProgramType.fromValue(resultSet.getString("p_type"));

            event.setProgram(resultSet.getString("p_identifier"));
            event.setProgramType(programType);
            event.setProgramStage(resultSet.getString("ps_identifier"));
            event.setOrgUnit(resultSet.getString("ou_uid"));
            event.setDeleted(resultSet.getBoolean("psi_deleted"));

            if (programType != ProgramType.WITHOUT_REGISTRATION) {
              event.setEnrollment(resultSet.getString("pi_uid"));
              event.setEnrollmentStatus(
                  EnrollmentStatus.fromProgramStatus(
                      ProgramStatus.valueOf(resultSet.getString("pi_status"))));
              event.setFollowup(resultSet.getBoolean("pi_followup"));
            }

            event.setAttributeOptionCombo(resultSet.getString("coc_identifier"));
            event.setAttributeCategoryOptions(resultSet.getString("co_uids"));
            event.setOptionSize(resultSet.getInt("option_size"));

            event.setTrackedEntityInstance(resultSet.getString("tei_uid"));

            event.setStoredBy(resultSet.getString("psi_storedby"));
            event.setOrgUnitName(resultSet.getString("ou_name"));
            event.setDueDate(DateUtils.getIso8601NoTz(resultSet.getTimestamp("psi_duedate")));
            event.setEventDate(
                DateUtils.getIso8601NoTz(resultSet.getTimestamp("psi_executiondate")));
            event.setCreated(DateUtils.getIso8601NoTz(resultSet.getTimestamp("psi_created")));
            event.setCreatedByUserInfo(
                jsonToUserInfo(resultSet.getString("psi_createdbyuserinfo"), jsonMapper));
            event.setLastUpdated(
                DateUtils.getIso8601NoTz(resultSet.getTimestamp("psi_lastupdated")));
            event.setLastUpdatedByUserInfo(
                jsonToUserInfo(resultSet.getString("psi_lastupdatedbyuserinfo"), jsonMapper));

            event.setCompletedBy(resultSet.getString("psi_completedby"));
            event.setCompletedDate(
                DateUtils.getIso8601NoTz(resultSet.getTimestamp("psi_completeddate")));

            if (resultSet.getObject("psi_geometry") != null) {
              try {
                Geometry geom = new WKTReader().read(resultSet.getString("psi_geometry"));

                event.setGeometry(geom);
              } catch (ParseException e) {
                log.error("Unable to read geometry for event '" + event.getUid() + "': ", e);
              }
            }

            if (resultSet.getObject("user_assigned") != null) {
              event.setAssignedUser(resultSet.getString("user_assigned"));
              event.setAssignedUserUsername(resultSet.getString("user_assigned_username"));
              event.setAssignedUserDisplayName(resultSet.getString("user_assigned_name"));
              event.setAssignedUserFirstName(resultSet.getString("user_assigned_first_name"));
              event.setAssignedUserSurname(resultSet.getString("user_assigned_surname"));
            }

            events.add(event);

            if (!StringUtils.isEmpty(resultSet.getString("psi_eventdatavalues"))) {
              Set<EventDataValue> eventDataValues =
                  convertEventDataValueJsonIntoSet(resultSet.getString("psi_eventdatavalues"));

              for (EventDataValue dv : eventDataValues) {
                DataValue dataValue = convertEventDataValueIntoDtoDataValue(dv);

                if (params.isSynchronizationQuery()) {
                  dataValue.setSkipSynchronization(
                      psdesWithSkipSyncTrue.containsKey(resultSet.getString("ps_uid"))
                          && psdesWithSkipSyncTrue
                              .get(resultSet.getString("ps_uid"))
                              .contains(dv.getDataElement()));
                }

                event.getDataValues().add(dataValue);
              }
            }

            if (resultSet.getString("psinote_value") != null
                && !notes.contains(resultSet.getString("psinote_id"))) {
              Note note = new Note();
              note.setNote(resultSet.getString("psinote_uid"));
              note.setValue(resultSet.getString("psinote_value"));
              note.setStoredDate(DateUtils.getIso8601NoTz(resultSet.getDate("psinote_storeddate")));
              note.setStoredBy(resultSet.getString("psinote_storedby"));

              if (resultSet.getObject("usernote_id") != null) {

                note.setLastUpdatedBy(
                    UserInfoSnapshot.of(
                        resultSet.getLong("usernote_id"),
                        resultSet.getString("usernote_code"),
                        resultSet.getString("usernote_uid"),
                        resultSet.getString("usernote_username"),
                        resultSet.getString("userinfo_firstname"),
                        resultSet.getString("userinfo_surname")));
              }

              note.setLastUpdated(resultSet.getDate("psinote_lastupdated"));

              event.getNotes().add(note);
              notes.add(resultSet.getString("psinote_id"));
            }

            if (params.isIncludeRelationships() && resultSet.getObject("psi_rl") != null) {
              PGobject pGobject = (PGobject) resultSet.getObject("psi_rl");

              if (pGobject != null) {
                String value = pGobject.getValue();

                relationshipIds.addAll(Lists.newArrayList(gson.fromJson(value, Long[].class)));
              }
            }
          }

          final Multimap<String, Relationship> map =
              eventStore.getRelationshipsByIds(relationshipIds, params);

          if (!map.isEmpty()) {
            events.forEach(e -> e.getRelationships().addAll(map.get(e.getEvent())));
          }

          IdSchemes idSchemes = ObjectUtils.firstNonNull(params.getIdSchemes(), new IdSchemes());
          IdScheme dataElementIdScheme = idSchemes.getDataElementIdScheme();

          if (dataElementIdScheme != IdScheme.ID && dataElementIdScheme != IdScheme.UID) {
            CachingMap<String, String> dataElementUidToIdentifierCache = new CachingMap<>();

            List<Collection<DataValue>> dataValuesList =
                events.stream()
                    .map(org.hisp.dhis.dxf2.deprecated.tracker.event.Event::getDataValues)
                    .collect(Collectors.toList());
            populateCache(dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache);
            convertDataValuesIdentifiers(
                dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache);
          }

          return events;
        });
  }

  @Override
  public List<Event> saveEvents(List<org.hisp.dhis.program.Event> events) {
    try {
      return saveAllEvents(events);
    } catch (Exception e) {
      log.error("An error occurred saving a batch", e);
      throw e;
    }
  }

  @Override
  public List<org.hisp.dhis.program.Event> updateEvents(List<Event> events) {
    try {
      SqlParameterSource[] parameters = new SqlParameterSource[events.size()];

      for (int i = 0; i < events.size(); i++) {
        try {
          parameters[i] = getSqlParametersForUpdate(events.get(i));
        } catch (SQLException | JsonProcessingException e) {
          log.warn(
              "PSI failed to update and will be ignored. PSI UID: " + events.get(i).getUid(),
              events.get(i).getUid(),
              e);
        }
      }

      jdbcTemplate.batchUpdate(UPDATE_EVENT_SQL, parameters);
    } catch (DataAccessException e) {
      log.error("Error updating events", e);
      throw e;
    }

    return events;
  }

  @Override
  public List<Map<String, String>> getEventsGrid(EventSearchParams params) {
    User user = currentUserService.getCurrentUser();

    setAccessiblePrograms(user, params);

    final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    String sql = buildGridSql(params, mapSqlParameterSource);

    return jdbcTemplate.query(
        sql,
        mapSqlParameterSource,
        rowSet -> {
          log.debug("Event query SQL: " + sql);

          List<Map<String, String>> list = new ArrayList<>();

          while (rowSet.next()) {
            final Map<String, String> map = new HashMap<>();

            for (String col : STATIC_EVENT_COLUMNS) {
              map.put(col, rowSet.getString(col));
            }

            for (QueryItem item : params.getDataElements()) {
              map.put(item.getItemId(), rowSet.getString(item.getItemId()));
            }

            list.add(map);
          }

          return list;
        });
  }

  @Override
  public List<EventRow> getEventRows(EventSearchParams params) {
    User user = currentUserService.getCurrentUser();

    setAccessiblePrograms(user, params);

    List<EventRow> eventRows = new ArrayList<>();

    final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    String sql = buildSql(params, mapSqlParameterSource, user);

    return jdbcTemplate.query(
        sql,
        mapSqlParameterSource,
        resultSet -> {
          log.debug("Event query SQL: " + sql);

          EventRow eventRow = new EventRow();

          eventRow.setEvent("not_valid");

          Set<String> notes = new HashSet<>();

          Map<String, List<DataValue>> processedDataValues = new HashMap<>();

          while (resultSet.next()) {
            if (resultSet.getString("psi_uid") == null) {
              continue;
            }

            if (eventRow.getUid() == null
                || !eventRow.getUid().equals(resultSet.getString("psi_uid"))) {
              validateIdentifiersPresence(resultSet, params.getIdSchemes(), false);

              eventRow = new EventRow();

              eventRow.setUid(resultSet.getString("psi_uid"));

              eventRow.setEvent(resultSet.getString("psi_uid"));
              eventRow.setTrackedEntityInstance(resultSet.getString("tei_uid"));
              eventRow.setTrackedEntityInstanceOrgUnit(resultSet.getString("tei_ou"));
              eventRow.setTrackedEntityInstanceOrgUnitName(resultSet.getString("tei_ou_name"));
              eventRow.setTrackedEntityInstanceCreated(resultSet.getString("tei_created"));
              eventRow.setTrackedEntityInstanceInactive(resultSet.getBoolean("tei_inactive"));
              eventRow.setDeleted(resultSet.getBoolean("psi_deleted"));

              eventRow.setProgram(resultSet.getString("p_identifier"));
              eventRow.setProgramStage(resultSet.getString("ps_identifier"));
              eventRow.setOrgUnit(resultSet.getString("ou_uid"));

              ProgramType programType = ProgramType.fromValue(resultSet.getString("p_type"));

              if (programType == ProgramType.WITH_REGISTRATION) {
                eventRow.setEnrollment(resultSet.getString("pi_uid"));
                eventRow.setFollowup(resultSet.getBoolean("pi_followup"));
              }

              eventRow.setTrackedEntityInstance(resultSet.getString("tei_uid"));
              eventRow.setOrgUnitName(resultSet.getString("ou_name"));
              eventRow.setDueDate(DateUtils.getIso8601NoTz(resultSet.getDate("psi_duedate")));
              eventRow.setEventDate(
                  DateUtils.getIso8601NoTz(resultSet.getDate("psi_executiondate")));

              eventRows.add(eventRow);
            }

            if (resultSet.getString("pav_value") != null && resultSet.getString("ta_uid") != null) {
              String valueType = resultSet.getString("ta_valuetype");

              Attribute attribute = new Attribute();
              attribute.setCreated(DateUtils.getIso8601NoTz(resultSet.getDate("pav_created")));
              attribute.setLastUpdated(
                  DateUtils.getIso8601NoTz(resultSet.getDate("pav_lastupdated")));
              attribute.setValue(resultSet.getString("pav_value"));
              attribute.setDisplayName(resultSet.getString("ta_name"));
              attribute.setValueType(
                  valueType != null ? ValueType.valueOf(valueType.toUpperCase()) : null);
              attribute.setAttribute(resultSet.getString("ta_uid"));

              eventRow.getAttributes().add(attribute);
            }

            if (!StringUtils.isEmpty(resultSet.getString("psi_eventdatavalues"))
                && !processedDataValues.containsKey(resultSet.getString("psi_uid"))) {
              List<DataValue> dataValues = new ArrayList<>();
              Set<EventDataValue> eventDataValues =
                  convertEventDataValueJsonIntoSet(resultSet.getString("psi_eventdatavalues"));

              for (EventDataValue dv : eventDataValues) {
                dataValues.add(convertEventDataValueIntoDtoDataValue(dv));
              }
              processedDataValues.put(resultSet.getString("psi_uid"), dataValues);
            }

            if (resultSet.getString("psinote_value") != null
                && !notes.contains(resultSet.getString("psinote_id"))) {
              Note note = new Note();
              note.setNote(resultSet.getString("psinote_uid"));
              note.setValue(resultSet.getString("psinote_value"));
              note.setStoredDate(DateUtils.getIso8601NoTz(resultSet.getDate("psinote_storeddate")));
              note.setStoredBy(resultSet.getString("psinote_storedby"));

              eventRow.getNotes().add(note);
              notes.add(resultSet.getString("psinote_id"));
            }
          }
          eventRows.forEach(e -> e.setDataValues(processedDataValues.get(e.getUid())));

          IdSchemes idSchemes = ObjectUtils.firstNonNull(params.getIdSchemes(), new IdSchemes());
          IdScheme dataElementIdScheme = idSchemes.getDataElementIdScheme();

          if (dataElementIdScheme != IdScheme.ID && dataElementIdScheme != IdScheme.UID) {
            CachingMap<String, String> dataElementUidToIdentifierCache = new CachingMap<>();

            List<Collection<DataValue>> dataValuesList =
                eventRows.stream().map(EventRow::getDataValues).collect(Collectors.toList());
            populateCache(dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache);
            convertDataValuesIdentifiers(
                dataElementIdScheme, dataValuesList, dataElementUidToIdentifierCache);
          }

          return eventRows;
        });
  }

  private String getIdSqlBasedOnIdScheme(
      IdScheme idScheme, String uidSql, String attributeSql, String codeSql) {
    if (idScheme == IdScheme.ID || idScheme == IdScheme.UID) {
      return uidSql;
    } else if (idScheme.isAttribute()) {
      return String.format(attributeSql, idScheme.getAttribute());
    } else {
      return codeSql;
    }
  }

  private void validateIdentifiersPresence(
      ResultSet rowSet, IdSchemes idSchemes, boolean validateCategoryOptionCombo)
      throws SQLException {

    if (StringUtils.isEmpty(rowSet.getString("p_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "Program %s does not have a value assigned for idScheme %s",
              rowSet.getString("p_uid"), idSchemes.getProgramIdScheme().name()));
    }

    if (StringUtils.isEmpty(rowSet.getString("ps_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "ProgramStage %s does not have a value assigned for idScheme %s",
              rowSet.getString("ps_uid"), idSchemes.getProgramStageIdScheme().name()));
    }

    if (StringUtils.isEmpty(rowSet.getString("ou_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "OrgUnit %s does not have a value assigned for idScheme %s",
              rowSet.getString("ou_uid"), idSchemes.getOrgUnitIdScheme().name()));
    }

    if (validateCategoryOptionCombo && StringUtils.isEmpty(rowSet.getString("coc_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "CategoryOptionCombo %s does not have a value assigned for idScheme %s",
              rowSet.getString("coc_uid"), idSchemes.getCategoryOptionComboIdScheme().name()));
    }
  }

  private String getEventSelectIdentifiersByIdScheme(EventSearchParams params) {
    IdSchemes idSchemes = params.getIdSchemes();

    StringBuilder sqlBuilder = new StringBuilder();

    String ouTableName = getOuTableName(params);

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getOrgUnitIdScheme(),
            ouTableName + ".uid as ou_identifier, ",
            ouTableName + ".attributevalues #>> '{%s, value}' as ou_identifier, ",
            ouTableName + ".code as ou_identifier, "));

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getProgramIdScheme(),
            "p.uid as p_identifier, ",
            "p.attributevalues #>> '{%s, value}' as p_identifier, ",
            "p.code as p_identifier, "));

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getProgramStageIdScheme(),
            "ps.uid as ps_identifier, ",
            "ps.attributevalues #>> '{%s, value}' as ps_identifier, ",
            "ps.code as ps_identifier, "));

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getCategoryOptionComboIdScheme(),
            "coc.uid as coc_identifier, ",
            "coc.attributevalues #>> '{%s, value}' as coc_identifier, ",
            "coc.code as coc_identifier, "));

    return sqlBuilder.toString();
  }

  @Override
  public int getEventCount(EventSearchParams params) {
    User user = currentUserService.getCurrentUser();
    setAccessiblePrograms(user, params);

    String sql;

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    if (params.hasFilters()) {
      sql = buildGridSql(params, mapSqlParameterSource);
    } else {
      sql = getEventSelectQuery(params, mapSqlParameterSource, user);
    }

    sql = sql.replaceFirst("select .*? from", "select count(*) from");

    sql = sql.replaceFirst("order .*? (desc|asc)", "");

    sql = sql.replaceFirst("limit \\d+ offset \\d+", "");

    log.debug("Event query count SQL: " + sql);

    return jdbcTemplate.queryForObject(sql, mapSqlParameterSource, Integer.class);
  }

  private DataValue convertEventDataValueIntoDtoDataValue(EventDataValue eventDataValue) {
    DataValue dataValue = new DataValue();
    dataValue.setCreated(DateUtils.getIso8601NoTz(eventDataValue.getCreated()));
    dataValue.setCreatedByUserInfo(eventDataValue.getCreatedByUserInfo());
    dataValue.setLastUpdated(DateUtils.getIso8601NoTz(eventDataValue.getLastUpdated()));
    dataValue.setLastUpdatedByUserInfo(eventDataValue.getLastUpdatedByUserInfo());
    dataValue.setDataElement(eventDataValue.getDataElement());
    dataValue.setValue(eventDataValue.getValue());
    dataValue.setProvidedElsewhere(eventDataValue.getProvidedElsewhere());
    dataValue.setStoredBy(eventDataValue.getStoredBy());

    return dataValue;
  }

  private String buildGridSql(
      EventSearchParams params, MapSqlParameterSource mapSqlParameterSource) {
    SqlHelper hlp = new SqlHelper();

    StringBuilder selectBuilder =
        new StringBuilder()
            .append("select ")
            .append(
                COLUMNS_ALIAS_MAP.entrySet().stream()
                    .map(col -> col.getKey() + " as " + col.getValue())
                    .collect(Collectors.joining(", ")));

    return selectBuilder
        .append(
            getFromWhereClause(
                params,
                dataElementAndFiltersSql(params, mapSqlParameterSource, hlp, selectBuilder),
                mapSqlParameterSource,
                hlp))
        .append(getGridOrderQuery(params))
        .append(getEventPagingQuery(params))
        .toString();
  }

  /**
   * Query is based on three sub queries on event, data value and comment, which are joined using
   * program stage instance id. The purpose of the separate queries is to be able to page properly
   * on events.
   */
  private String buildSql(
      EventSearchParams params, MapSqlParameterSource mapSqlParameterSource, User user) {
    StringBuilder sqlBuilder = new StringBuilder().append("select * from (");

    sqlBuilder.append(getEventSelectQuery(params, mapSqlParameterSource, user));

    sqlBuilder.append(getOrderQuery(params));

    sqlBuilder.append(getEventPagingQuery(params));

    sqlBuilder.append(") as event left join (");

    if (params.isIncludeAttributes()) {
      sqlBuilder.append(getAttributeValueQuery());

      sqlBuilder.append(") as att on event.tei_id=att.pav_id left join (");
    }

    sqlBuilder.append(PSI_EVENT_COMMENT_QUERY);

    sqlBuilder.append(") as cm on event.psi_id=cm.psic_id ");

    if (params.isIncludeRelationships()) {
      sqlBuilder.append(RELATIONSHIP_IDS_QUERY);
    }

    sqlBuilder.append(getOrderQuery(params));

    return sqlBuilder.toString();
  }

  /**
   * Generates a single INNER JOIN for each attribute we are searching on. We can search by a range
   * of operators. All searching is using lower() since attribute values are case-insensitive.
   *
   * @param attributes
   * @param filterItems
   */
  private void joinAttributeValueWithoutQueryParameter(
      StringBuilder attributes, List<QueryItem> filterItems) {
    for (QueryItem queryItem : filterItems) {
      String teaValueCol = statementBuilder.columnQuote(queryItem.getItemId());
      String teaCol = statementBuilder.columnQuote(queryItem.getItemId() + "ATT");

      attributes
          .append(" INNER JOIN trackedentityattributevalue ")
          .append(teaValueCol)
          .append(" ON ")
          .append(teaValueCol + ".trackedentityid")
          .append(" = TEI.trackedentityid ")
          .append(" INNER JOIN trackedentityattribute ")
          .append(teaCol)
          .append(" ON ")
          .append(teaValueCol + ".trackedentityattributeid")
          .append(EQUALS)
          .append(teaCol + ".trackedentityattributeid")
          .append(AND)
          .append(teaCol + ".UID")
          .append(EQUALS)
          .append(statementBuilder.encode(queryItem.getItem().getUid(), true));

      attributes.append(getAttributeFilterQuery(queryItem, teaCol, teaValueCol));
    }
  }

  private String getAttributeFilterQuery(QueryItem queryItem, String teaCol, String teaValueCol) {
    StringBuilder query = new StringBuilder();

    if (!queryItem.getFilters().isEmpty()) {
      query.append(AND);

      // In SQL the order of expressions linked by AND is not
      // guaranteed.
      // So when casting to number we need to be sure that the value
      // to cast is really a number.
      if (queryItem.isNumeric()) {
        query
            .append(" CASE WHEN ")
            .append(lower(teaCol + ".valueType"))
            .append(" in (")
            .append(
                NUMERIC_TYPES.stream()
                    .map(Enum::name)
                    .map(StringUtils::lowerCase)
                    .map(SqlUtils::singleQuote)
                    .collect(Collectors.joining(",")))
            .append(")")
            .append(" THEN ");
      }

      List<String> filterStrings = new ArrayList<>();
      for (QueryFilter filter : queryItem.getFilters()) {
        StringBuilder filterString = new StringBuilder();
        final String queryCol =
            queryItem.isNumeric()
                ? castToNumber(teaValueCol + ".value")
                : lower(teaValueCol + ".value");
        final Object encodedFilter =
            queryItem.isNumeric()
                ? Double.valueOf(filter.getFilter())
                : StringUtils.lowerCase(filter.getSqlFilter(filter.getFilter()));
        filterString
            .append(queryCol)
            .append(SPACE)
            .append(filter.getSqlOperator())
            .append(SPACE)
            .append(encodedFilter);
        filterStrings.add(filterString.toString());
      }
      query.append(String.join(AND, filterStrings));

      if (queryItem.isNumeric()) {
        query.append(" END ");
      }
    }

    return query.toString();
  }

  private String getEventSelectQuery(
      EventSearchParams params, MapSqlParameterSource mapSqlParameterSource, User user) {
    SqlHelper hlp = new SqlHelper();

    StringBuilder selectBuilder =
        new StringBuilder()
            .append("select ")
            .append(getEventSelectIdentifiersByIdScheme(params))
            .append(" psi.uid as psi_uid, ")
            .append("ou.uid as ou_uid, p.uid as p_uid, ps.uid as ps_uid, ")
            .append(
                "psi.eventid as psi_id, psi.status as psi_status, psi.executiondate as psi_executiondate, ")
            .append(
                "psi.eventdatavalues as psi_eventdatavalues, psi.duedate as psi_duedate, psi.completedby as psi_completedby, psi.storedby as psi_storedby, ")
            .append(
                "psi.created as psi_created, psi.createdbyuserinfo as psi_createdbyuserinfo, psi.lastupdated as psi_lastupdated, psi.lastupdatedbyuserinfo as psi_lastupdatedbyuserinfo, ")
            .append("psi.completeddate as psi_completeddate, psi.deleted as psi_deleted, ")
            .append(
                "ST_AsText( psi.geometry ) as psi_geometry, au.uid as user_assigned, (au.firstName || ' ' || au.surName) as user_assigned_name,")
            .append(
                "au.firstName as user_assigned_first_name, au.surName as user_assigned_surname, ")
            .append("au.username as user_assigned_username,")
            .append("coc.uid as coc_uid, ")
            .append("coc_agg.co_uids AS co_uids, ")
            .append("coc_agg.co_count AS option_size, ");

    for (OrderParam orderParam : params.getAttributeOrders()) {
      selectBuilder
          .append(quote(orderParam.getField()))
          .append(".value AS ")
          .append(orderParam.getField())
          .append("_value, ");
    }

    return selectBuilder
        .append(
            "pi.uid as pi_uid, pi.status as pi_status, pi.followup as pi_followup, pi.enrollmentdate as pi_enrollmentdate, pi.incidentdate as pi_incidentdate, ")
        .append("p.type as p_type, ps.uid as ps_uid, ou.name as ou_name, ")
        .append(
            "tei.trackedentityid as tei_id, tei.uid as tei_uid, teiou.uid as tei_ou, teiou.name as tei_ou_name, tei.created as tei_created, tei.inactive as tei_inactive ")
        .append(
            getFromWhereClause(
                params,
                mapSqlParameterSource,
                user,
                hlp,
                dataElementAndFiltersSql(params, mapSqlParameterSource, hlp, selectBuilder)))
        .toString();
  }

  private boolean checkForOwnership(EventSearchParams params) {
    return Optional.ofNullable(params.getProgram())
        .filter(
            p ->
                Objects.nonNull(p.getProgramType())
                    && p.getProgramType() == ProgramType.WITH_REGISTRATION)
        .isPresent();
  }

  private String getOuTableName(EventSearchParams params) {
    return checkForOwnership(params) ? " psiou" : " ou";
  }

  private StringBuilder getFromWhereClause(
      EventSearchParams params,
      MapSqlParameterSource mapSqlParameterSource,
      User user,
      SqlHelper hlp,
      StringBuilder dataElementAndFiltersSql) {
    StringBuilder fromBuilder =
        new StringBuilder(" from event psi ")
            .append("inner join enrollment pi on pi.enrollmentid=psi.enrollmentid ")
            .append("inner join program p on p.programid=pi.programid ")
            .append("inner join programstage ps on ps.programstageid=psi.programstageid ");

    if (checkForOwnership(params)) {
      fromBuilder
          .append(
              "left join trackedentityprogramowner po on (pi.trackedentityid=po.trackedentityid) ")
          .append(
              "inner join organisationunit psiou on (coalesce(po.organisationunitid, psi.organisationunitid)=psiou.organisationunitid) ")
          .append(
              "inner join organisationunit ou on (psi.organisationunitid=ou.organisationunitid) ");
    } else {
      fromBuilder.append(
          "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid ");
    }

    fromBuilder
        .append("left join trackedentity tei on tei.trackedentityid=pi.trackedentityid ")
        .append(
            "left join organisationunit teiou on (tei.organisationunitid=teiou.organisationunitid) ")
        .append("left join userinfo au on (psi.assigneduserid=au.userinfoid) ");

    if (!params.getFilterAttributes().isEmpty()) {
      joinAttributeValueWithoutQueryParameter(fromBuilder, params.getFilterAttributes());
    }

    fromBuilder.append(getCategoryOptionComboQuery(user));

    fromBuilder.append(dataElementAndFiltersSql);

    if (params.getTrackedEntity() != null) {
      mapSqlParameterSource.addValue("trackedentityid", params.getTrackedEntity().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" tei.trackedentityid= ")
          .append(":trackedentityid")
          .append(" ");
    }

    if (params.getProgram() != null) {
      mapSqlParameterSource.addValue("programid", params.getProgram().getId());

      fromBuilder.append(hlp.whereAnd()).append(" p.programid = ").append(":programid").append(" ");
    }

    if (params.getProgramStage() != null) {
      mapSqlParameterSource.addValue("programstageid", params.getProgramStage().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" ps.programstageid = ")
          .append(":programstageid")
          .append(" ");
    }

    if (params.getProgramStatus() != null) {
      mapSqlParameterSource.addValue("program_status", params.getProgramStatus().name());

      fromBuilder.append(hlp.whereAnd()).append(" pi.status = ").append(":program_status ");
    }

    if (params.getEnrollmentEnrolledBefore() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentEnrolledBefore", params.getEnrollmentEnrolledBefore(), Types.TIMESTAMP);
      fromBuilder
          .append(hlp.whereAnd())
          .append(" (pi.enrollmentdate <= :enrollmentEnrolledBefore ) ");
    }

    if (params.getEnrollmentEnrolledAfter() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentEnrolledAfter", params.getEnrollmentEnrolledAfter(), Types.TIMESTAMP);
      fromBuilder
          .append(hlp.whereAnd())
          .append(" (pi.enrollmentdate >= :enrollmentEnrolledAfter ) ");
    }

    if (params.getEnrollmentOccurredBefore() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentOccurredBefore", params.getEnrollmentOccurredBefore(), Types.TIMESTAMP);
      fromBuilder
          .append(hlp.whereAnd())
          .append(" (pi.incidentdate <= :enrollmentOccurredBefore ) ");
    }

    if (params.getEnrollmentOccurredAfter() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentOccurredAfter", params.getEnrollmentOccurredAfter(), Types.TIMESTAMP);
      fromBuilder.append(hlp.whereAnd()).append(" (pi.incidentdate >= :enrollmentOccurredAfter ) ");
    }

    if (params.getDueDateStart() != null) {
      mapSqlParameterSource.addValue("startDueDate", params.getDueDateStart(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (psi.duedate is not null and psi.duedate >= :startDueDate ) ");
    }

    if (params.getDueDateEnd() != null) {
      mapSqlParameterSource.addValue("endDueDate", params.getDueDateEnd(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (psi.duedate is not null and psi.duedate <= :endDueDate ) ");
    }

    if (params.getFollowUp() != null) {
      fromBuilder
          .append(hlp.whereAnd())
          .append(" pi.followup is ")
          .append(Boolean.TRUE.equals(params.getFollowUp()) ? "true" : "false")
          .append(" ");
    }

    fromBuilder.append(addLastUpdatedFilters(params, mapSqlParameterSource, hlp, true));

    // Comparing milliseconds instead of always creating new Date( 0 );
    if (params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0) {
      mapSqlParameterSource.addValue(
          "skipChangedBefore", params.getSkipChangedBefore(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(PSI_LASTUPDATED_GT)
          .append(":skipChangedBefore")
          .append(" ");
    }

    if (params.getCategoryOptionCombo() != null) {
      mapSqlParameterSource.addValue(
          "attributeoptioncomboid", params.getCategoryOptionCombo().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" psi.attributeoptioncomboid = ")
          .append(":attributeoptioncomboid")
          .append(" ");
    }

    String orgUnitSql = getOrgUnitSql(params, getOuTableName(params));

    if (!Strings.isNullOrEmpty(orgUnitSql)) {
      fromBuilder.append(hlp.whereAnd()).append(" (").append(orgUnitSql).append(") ");
    }

    if (params.getStartDate() != null) {
      mapSqlParameterSource.addValue("startDate", params.getStartDate(), Types.DATE);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (psi.executiondate >= ")
          .append(":startDate")
          .append(" or (psi.executiondate is null and psi.duedate >= ")
          .append(":startDate")
          .append(" )) ");
    }

    if (params.getEndDate() != null) {
      mapSqlParameterSource.addValue("endDate", addDays(params.getEndDate(), 1), Types.DATE);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (psi.executiondate < ")
          .append(":endDate")
          .append(" or (psi.executiondate is null and psi.duedate < ")
          .append(":endDate")
          .append(" )) ");
    }

    if (params.getProgramType() != null) {
      mapSqlParameterSource.addValue("programType", params.getProgramType().name());

      fromBuilder.append(hlp.whereAnd()).append(" p.type = ").append(":programType").append(" ");
    }

    fromBuilder.append(eventStatusSql(params, mapSqlParameterSource, hlp));

    if (params.getEvents() != null && !params.getEvents().isEmpty() && !params.hasFilters()) {
      mapSqlParameterSource.addValue("psi_uid", params.getEvents());

      fromBuilder.append(hlp.whereAnd()).append(" (psi.uid in (").append(":psi_uid").append(")) ");
    }

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      mapSqlParameterSource.addValue(
          "au_uid", params.getAssignedUserQueryParam().getAssignedUsers());

      fromBuilder.append(hlp.whereAnd()).append(" (au.uid in (").append(":au_uid").append(")) ");
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      fromBuilder.append(hlp.whereAnd()).append(" (au.uid is null) ");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      fromBuilder.append(hlp.whereAnd()).append(" (au.uid is not null) ");
    }

    if (!params.isIncludeDeleted()) {
      fromBuilder.append(hlp.whereAnd()).append(" psi.deleted is false ");
    }

    if (params.hasSecurityFilter()) {
      mapSqlParameterSource.addValue(
          "program_uid",
          params.getAccessiblePrograms().isEmpty() ? null : params.getAccessiblePrograms());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (p.uid in (")
          .append(":program_uid")
          .append(")) ");

      mapSqlParameterSource.addValue(
          "programstage_uid",
          params.getAccessibleProgramStages().isEmpty()
              ? null
              : params.getAccessibleProgramStages());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ps.uid in (")
          .append(":programstage_uid")
          .append(")) ");
    }

    if (params.isSynchronizationQuery()) {
      fromBuilder.append(hlp.whereAnd()).append(" psi.lastupdated > psi.lastsynchronized ");
    }

    if (!CollectionUtils.isEmpty(params.getProgramInstances())) {
      mapSqlParameterSource.addValue("programinstance_uid", params.getProgramInstances());

      fromBuilder.append(hlp.whereAnd()).append(" (pi.uid in (:programinstance_uid)) ");
    }

    return fromBuilder;
  }

  /**
   * For dataElement params, restriction is set in inner join. For query params, restriction is set
   * in where clause.
   */
  private StringBuilder dataElementAndFiltersSql(
      EventSearchParams params,
      MapSqlParameterSource mapSqlParameterSource,
      SqlHelper hlp,
      StringBuilder selectBuilder) {
    int filterCount = 0;

    StringBuilder optionValueJoinBuilder = new StringBuilder();
    StringBuilder optionValueConditionBuilder = new StringBuilder();
    StringBuilder eventDataValuesWhereSql = new StringBuilder();
    Set<String> joinedColumns = new HashSet<>();

    for (QueryItem item : params.getDataElementsAndFilters()) {
      ++filterCount;

      final String itemId = item.getItemId();

      final String dataValueValueSql = "psi.eventdatavalues #>> '{" + itemId + ", value}'";

      selectBuilder
          .append(", ")
          .append(item.isNumeric() ? castToNumber(dataValueValueSql) : lower(dataValueValueSql))
          .append(" as ")
          .append(itemId);

      String optValueTableAs = "opt_" + filterCount;

      if (!joinedColumns.contains(itemId) && item.hasOptionSet() && item.hasFilter()) {
        String optSetBind = "optset_" + filterCount;

        mapSqlParameterSource.addValue(optSetBind, item.getOptionSet().getId());

        optionValueJoinBuilder
            .append("inner join optionvalue as ")
            .append(optValueTableAs)
            .append(" on lower(")
            .append(optValueTableAs)
            .append(".code) = ")
            .append("lower(")
            .append(dataValueValueSql)
            .append(") and ")
            .append(optValueTableAs)
            .append(".optionsetid = ")
            .append(":")
            .append(optSetBind)
            .append(" ");

        joinedColumns.add(itemId);
      }

      if (item.hasFilter()) {
        for (QueryFilter filter : item.getFilters()) {
          ++filterCount;

          final String queryCol =
              item.isNumeric() ? castToNumber(dataValueValueSql) : lower(dataValueValueSql);

          String bindParameter = "parameter_" + filterCount;
          int itemType = item.isNumeric() ? Types.NUMERIC : Types.VARCHAR;

          if (!item.hasOptionSet()) {
            eventDataValuesWhereSql.append(hlp.whereAnd());

            if (QueryOperator.IN.getValue().equalsIgnoreCase(filter.getSqlOperator())) {
              mapSqlParameterSource.addValue(
                  bindParameter,
                  QueryFilter.getFilterItems(StringUtils.lowerCase(filter.getFilter())),
                  itemType);

              eventDataValuesWhereSql.append(inCondition(filter, bindParameter, queryCol));
            } else {
              mapSqlParameterSource.addValue(
                  bindParameter, StringUtils.lowerCase(filter.getSqlBindFilter()), itemType);

              eventDataValuesWhereSql
                  .append(" ")
                  .append(queryCol)
                  .append(" ")
                  .append(filter.getSqlOperator())
                  .append(" ")
                  .append(":")
                  .append(bindParameter)
                  .append(" ");
            }
          } else {
            if (QueryOperator.IN.getValue().equalsIgnoreCase(filter.getSqlOperator())) {
              mapSqlParameterSource.addValue(
                  bindParameter,
                  QueryFilter.getFilterItems(StringUtils.lowerCase(filter.getFilter())),
                  itemType);

              optionValueConditionBuilder.append(" and ");
              optionValueConditionBuilder.append(inCondition(filter, bindParameter, queryCol));
            } else {
              mapSqlParameterSource.addValue(
                  bindParameter, StringUtils.lowerCase(filter.getSqlBindFilter()), itemType);

              optionValueConditionBuilder
                  .append("and lower(")
                  .append(optValueTableAs)
                  .append(DOT_NAME)
                  .append(" ")
                  .append(filter.getSqlOperator())
                  .append(" ")
                  .append(":")
                  .append(bindParameter)
                  .append(" ");
            }
          }
        }
      }
    }

    return optionValueJoinBuilder
        .append(optionValueConditionBuilder)
        .append(eventDataValuesWhereSql)
        .append(" ");
  }

  private String inCondition(QueryFilter filter, String boundParameter, String queryCol) {
    return new StringBuilder()
        .append(" ")
        .append(queryCol)
        .append(" ")
        .append(filter.getSqlOperator())
        .append(" ")
        .append("(")
        .append(":")
        .append(boundParameter)
        .append(") ")
        .toString();
  }

  private String getFromWhereClause(
      EventSearchParams params,
      StringBuilder dataElementAndFiltersSql,
      MapSqlParameterSource mapSqlParameterSource,
      SqlHelper hlp) {
    StringBuilder sqlBuilder =
        new StringBuilder()
            .append(
                " from event psi "
                    + "inner join enrollment pi on pi.enrollmentid = psi.enrollmentid "
                    + "inner join program p on p.programid = pi.programid "
                    + "inner join programstage ps on ps.programstageid = psi.programstageid "
                    + "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = psi.attributeoptioncomboid "
                    + "left join userinfo au on (psi.assigneduserid=au.userinfoid) ");

    if (checkForOwnership(params)) {
      sqlBuilder
          .append(
              "left join trackedentityprogramowner po on (pi.trackedentityid=po.trackedentityid) ")
          .append(
              "inner join organisationunit psiou on (coalesce(po.organisationunitid, psi.organisationunitid)=psiou.organisationunitid) ")
          .append(
              "left join organisationunit ou on (psi.organisationunitid=ou.organisationunitid) ");
    } else {
      sqlBuilder.append(
          "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid ");
    }

    sqlBuilder.append(dataElementAndFiltersSql);

    String orgUnitSql = getOrgUnitSql(params, getOuTableName(params));

    if (orgUnitSql != null) {
      sqlBuilder.append(hlp.whereAnd()).append(orgUnitSql + " ");
    }

    if (params.getProgramStage() != null) {
      mapSqlParameterSource.addValue("programstageid", params.getProgramStage().getId());

      sqlBuilder
          .append(hlp.whereAnd())
          .append(" ps.programstageid = ")
          .append(":programstageid")
          .append(" ");
    }

    if (params.getCategoryOptionCombo() != null) {
      mapSqlParameterSource.addValue(
          "attributeoptioncomboid", params.getCategoryOptionCombo().getId());

      sqlBuilder
          .append(hlp.whereAnd())
          .append(" psi.attributeoptioncomboid = ")
          .append(":attributeoptioncomboid")
          .append(" ");
    }

    if (params.getStartDate() != null) {
      mapSqlParameterSource.addValue("startDate", params.getStartDate(), Types.DATE);

      sqlBuilder
          .append(hlp.whereAnd())
          .append(" (psi.executiondate >= ")
          .append(":startDate")
          .append(" or (psi.executiondate is null and psi.duedate >= ")
          .append(":startDate")
          .append(" )) ");
    }

    if (params.getEndDate() != null) {
      mapSqlParameterSource.addValue("endDate", addDays(params.getEndDate(), 1), Types.DATE);

      sqlBuilder
          .append(hlp.whereAnd())
          .append(" (psi.executiondate < ")
          .append(":endDate ")
          .append(" or (psi.executiondate is null and psi.duedate < ")
          .append(":endDate")
          .append(" )) ");
    }

    sqlBuilder.append(addLastUpdatedFilters(params, mapSqlParameterSource, hlp, false));

    if (params.isSynchronizationQuery()) {
      sqlBuilder.append(hlp.whereAnd()).append(" psi.lastupdated > psi.lastsynchronized ");
    }

    // Comparing milliseconds instead of always creating new Date( 0 )

    if (params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0) {
      mapSqlParameterSource.addValue(
          "skipChangedBefore", params.getSkipChangedBefore(), Types.TIMESTAMP);

      sqlBuilder.append(hlp.whereAnd()).append(PSI_LASTUPDATED_GT).append(":skipChangedBefore ");
    }

    if (params.getDueDateStart() != null) {
      mapSqlParameterSource.addValue("startDueDate", params.getDueDateStart(), Types.DATE);

      sqlBuilder
          .append(hlp.whereAnd())
          .append(" psi.duedate is not null and psi.duedate >= ")
          .append(":dueDate")
          .append(" ");
    }

    if (params.getDueDateEnd() != null) {
      mapSqlParameterSource.addValue("endDueDate", params.getDueDateEnd(), Types.DATE);

      sqlBuilder
          .append(hlp.whereAnd())
          .append(" psi.duedate is not null and psi.duedate <= ")
          .append(":endDueDate")
          .append(" ");
    }

    if (!params.isIncludeDeleted()) {
      sqlBuilder.append(hlp.whereAnd()).append(" psi.deleted is false ");
    }

    if (!CollectionUtils.isEmpty(params.getProgramInstances())) {
      mapSqlParameterSource.addValue("programinstance_uid", params.getProgramInstances());

      sqlBuilder.append(hlp.whereAnd()).append(" (pi.uid in (:programinstance_uid)) ");
    }

    sqlBuilder.append(eventStatusSql(params, mapSqlParameterSource, hlp));

    if (params.getEvents() != null && !params.getEvents().isEmpty() && !params.hasFilters()) {
      mapSqlParameterSource.addValue("psi_uid", params.getEvents());

      sqlBuilder.append(hlp.whereAnd()).append(" (psi.uid in (").append(":psi_uid").append(")) ");
    }

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      mapSqlParameterSource.addValue(
          "au_uid", params.getAssignedUserQueryParam().getAssignedUsers());

      sqlBuilder.append(hlp.whereAnd()).append(" (au.uid in (").append(":au_uid").append(")) ");
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      sqlBuilder.append(hlp.whereAnd()).append(" (au.uid is null) ");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      sqlBuilder.append(hlp.whereAnd()).append(" (au.uid is not null) ");
    }

    return sqlBuilder.toString();
  }

  private String eventStatusSql(
      EventSearchParams params, MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp) {
    StringBuilder stringBuilder = new StringBuilder();

    if (params.getEventStatus() != null) {
      if (params.getEventStatus() == EventStatus.VISITED) {
        mapSqlParameterSource.addValue(PSI_STATUS, EventStatus.ACTIVE.name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(PSI_STATUS_EQ)
            .append(":" + PSI_STATUS)
            .append(" and psi.executiondate is not null ");
      } else if (params.getEventStatus() == EventStatus.OVERDUE) {
        mapSqlParameterSource.addValue(PSI_STATUS, EventStatus.SCHEDULE.name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(" date(now()) > date(psi.duedate) and psi.status = ")
            .append(":" + PSI_STATUS)
            .append(" ");
      } else {
        mapSqlParameterSource.addValue(PSI_STATUS, params.getEventStatus().name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(PSI_STATUS_EQ)
            .append(":" + PSI_STATUS)
            .append(" ");
      }
    }

    return stringBuilder.toString();
  }

  private String addLastUpdatedFilters(
      EventSearchParams params,
      MapSqlParameterSource mapSqlParameterSource,
      SqlHelper hlp,
      boolean useDateAfterEndDate) {
    StringBuilder sqlBuilder = new StringBuilder();

    if (params.hasLastUpdatedDuration()) {
      mapSqlParameterSource.addValue(
          "lastUpdated",
          DateUtils.offSetDateTimeFrom(DateUtils.nowMinusDuration(params.getLastUpdatedDuration())),
          Types.TIMESTAMP_WITH_TIMEZONE);

      sqlBuilder
          .append(hlp.whereAnd())
          .append(PSI_LASTUPDATED_GT)
          .append(":lastUpdated")
          .append(" ");
    } else {
      if (params.hasLastUpdatedStartDate()) {
        mapSqlParameterSource.addValue(
            "lastUpdatedStart", params.getLastUpdatedStartDate(), Types.TIMESTAMP);

        sqlBuilder
            .append(hlp.whereAnd())
            .append(PSI_LASTUPDATED_GT)
            .append(":lastUpdatedStart")
            .append(" ");
      }

      if (params.hasLastUpdatedEndDate()) {
        if (useDateAfterEndDate) {
          mapSqlParameterSource.addValue(
              "lastUpdatedEnd", addDays(params.getLastUpdatedEndDate(), 1), Types.TIMESTAMP);

          sqlBuilder
              .append(hlp.whereAnd())
              .append(" psi.lastupdated < ")
              .append(":lastUpdatedEnd")
              .append(" ");
        } else {
          mapSqlParameterSource.addValue(
              "lastUpdatedEnd", params.getLastUpdatedEndDate(), Types.TIMESTAMP);

          sqlBuilder
              .append(hlp.whereAnd())
              .append(" psi.lastupdated <= ")
              .append(":lastUpdatedEnd")
              .append(" ");
        }
      }
    }

    return sqlBuilder.toString();
  }

  /**
   * Returns the joins and sub-queries needed to fulfill all the needs regarding category option
   * combo and category options. Category option combos (COC) are composed of category options (CO),
   * one per category of the COCs category combination (CC).
   *
   * <p>Important constraints leading to this query:
   *
   * <ul>
   *   <li>While COCs are pre-computed and can be seen as a de-normalization of the possible
   *       permutations the COs in a COC are stored in a normalized way. The final event should have
   *       its attributeCategoryOptions field populated with a semicolon separated string of its
   *       COCs COs. We thus need to aggregate these COs for each event.
   *   <li>COCs should be returned in the user specified idScheme. So in order to have access to
   *       uid, code, name, attributes we need another join as all of these fields cannot be added
   *       to the above aggregation. IdSchemes SELECT are handled in {@link
   *       #getEventSelectIdentifiersByIdScheme}.
   *   <li>A user must have access to all COs of the events COC to have access to an event.
   * </ul>
   */
  private String getCategoryOptionComboQuery(User user) {
    String joinCondition =
        "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = psi.attributeoptioncomboid "
            + " inner join (select coc.categoryoptioncomboid as id,"
            + " string_agg(co.uid, ';') as co_uids, count(co.categoryoptionid) as co_count"
            + " from categoryoptioncombo coc "
            + " inner join categoryoptioncombos_categoryoptions cocco on coc.categoryoptioncomboid = cocco.categoryoptioncomboid"
            + " inner join dataelementcategoryoption co on cocco.categoryoptionid = co.categoryoptionid"
            + " group by coc.categoryoptioncomboid ";

    if (!isSuper(user)) {
      joinCondition =
          joinCondition
              + " having bool_and(case when "
              + JpaQueryUtils.generateSQlQueryForSharingCheck(
                  "co.sharing", user, AclService.LIKE_READ_DATA)
              + " then true else false end) = True ";
    }

    return joinCondition + ") as coc_agg on coc_agg.id = psi.attributeoptioncomboid ";
  }

  private String getEventPagingQuery(final EventSearchParams params) {
    final StringBuilder sqlBuilder = new StringBuilder().append(" ");
    int pageSize = params.getPageSizeWithDefault();

    // When the clients choose to not show the total of pages.
    if (!params.isTotalPages()) {
      // Get pageSize + 1, so we are able to know if there is another
      // page available. It adds one additional element into the list,
      // as consequence. The caller needs to remove the last element.
      pageSize++;
    }

    if (!params.isSkipPaging()) {
      sqlBuilder
          .append("limit ")
          .append(pageSize)
          .append(" offset ")
          .append(params.getOffset())
          .append(" ");
    }

    return sqlBuilder.toString();
  }

  private String getGridOrderQuery(EventSearchParams params) {

    if (params.getGridOrders() != null
        && params.getDataElements() != null
        && !params.getDataElements().isEmpty()
        && STATIC_EVENT_COLUMNS != null
        && !STATIC_EVENT_COLUMNS.isEmpty()) {
      List<String> orderFields = new ArrayList<>();

      for (OrderParam order : params.getGridOrders()) {
        if (STATIC_EVENT_COLUMNS.contains(order.getField())) {
          orderFields.add(order.getField() + " " + order.getDirection());
        } else {
          Set<QueryItem> queryItems = params.getDataElements();

          for (QueryItem item : queryItems) {
            if (order.getField().equals(item.getItemId())) {
              orderFields.add(order.getField() + " " + order.getDirection());
              break;
            }
          }
        }
      }

      if (!orderFields.isEmpty()) {
        return "order by " + StringUtils.join(orderFields, ',');
      }
    }

    return "order by lastUpdated desc ";
  }

  private String getOrderQuery(EventSearchParams params) {
    ArrayList<String> orderFields = new ArrayList<>();

    for (OrderParam order : params.getOrders()) {
      if (QUERY_PARAM_COL_MAP.containsKey(order.getField())) {
        String orderText = QUERY_PARAM_COL_MAP.get(order.getField());
        orderText += " " + (order.getDirection().isAscending() ? "asc" : "desc");
        orderFields.add(orderText);
      } else if (params.getAttributeOrders().contains(order)) {
        orderFields.add(order.getField() + "_value " + order.getDirection());
      } else if (params.getGridOrders().contains(order)) {
        orderFields.add(getDataElementsOrder(params.getDataElements(), order));
      }
    }

    if (!orderFields.isEmpty()) {
      return "order by " + StringUtils.join(orderFields, ',') + " ";
    } else {
      return "order by psi_lastupdated desc ";
    }
  }

  private String getDataElementsOrder(Set<QueryItem> dataElements, OrderParam order) {
    for (QueryItem item : dataElements) {
      if (order.getField().equals(item.getItemId())) {
        return order.getField() + " " + order.getDirection();
      }
    }

    return "";
  }

  private String getAttributeValueQuery() {
    return "select pav.trackedentityid as pav_id, pav.created as pav_created, pav.lastupdated as pav_lastupdated, "
        + "pav.value as pav_value, ta.uid as ta_uid, ta.name as ta_name, ta.valuetype as ta_valuetype "
        + "from trackedentityattributevalue pav "
        + "inner join trackedentityattribute ta on pav.trackedentityattributeid=ta.trackedentityattributeid ";
  }

  private boolean isSuper(User user) {
    return user == null || user.isSuper();
  }

  /**
   * Saves a list of {@see Event} using JDBC batch update.
   *
   * <p>Note that this method is using JdbcTemplate to execute the batch operation, therefore it's
   * able to participate in any Spring-initiated transaction
   *
   * @param batch the list of {@see Event}
   * @return the list of created {@see Event} with primary keys assigned
   */
  private List<Event> saveAllEvents(List<org.hisp.dhis.program.Event> batch) {

    JdbcUtils.batchUpdateWithKeyHolder(
        jdbcTemplate.getJdbcTemplate(),
        INSERT_EVENT_SQL,
        new BatchPreparedStatementSetterWithKeyHolder<>(sort(batch)) {
          @Override
          protected void setValues(PreparedStatement ps, Event event) {
            try {
              bindEventParamsForInsert(ps, event);
            } catch (JsonProcessingException | SQLException e) {
              log.warn(
                  "PSI failed to persist and will be ignored. PSI UID: " + event.getUid(),
                  event.getUid(),
                  e);
            }
          }

          @Override
          protected void setPrimaryKey(Map<String, Object> primaryKey, Event event) {
            event.setId((Long) primaryKey.get("eventid"));
          }
        });

    /*
     * Extract the primary keys from the created objects
     */
    List<Long> eventIds =
        batch.stream().map(IdentifiableObject::getId).collect(Collectors.toList());

    /*
     * Assign the generated event PKs to the batch.
     *
     * If the generate event PKs size doesn't match the batch size, one or
     * more PSI were not persisted. Run an additional query to fetch the
     * persisted PSI and return only the PSI from the batch which are
     * persisted.
     *
     */
    if (eventIds.size() != batch.size()) {
      /* a Map where [key] -> PSI UID , [value] -> PSI ID */
      Map<String, Long> persisted =
          jdbcTemplate
              .queryForList(
                  "SELECT uid, eventid from event where eventid in ( "
                      + Joiner.on(";").join(eventIds)
                      + ")",
                  Maps.newHashMap())
              .stream()
              .collect(Collectors.toMap(s -> (String) s.get("uid"), s -> (Long) s.get("eventid")));

      return batch.stream()
          .filter(psi -> persisted.containsKey(psi.getUid()))
          .peek(psi -> psi.setId(persisted.get(psi.getUid())))
          .collect(Collectors.toList());
    } else {
      for (int i = 0; i < eventIds.size(); i++) {
        batch.get(i).setId(eventIds.get(i));
      }
      return batch;
    }
  }

  @Override
  public void updateTrackedEntityInstances(List<String> teiUids, User user) {
    if (teiUids.isEmpty()) {
      return;
    }

    String sql = UPDATE_TEI_SQL;

    if (skipLockedProvider.getSkipLocked().isEmpty()) {
      sql = sql.replace("SKIP LOCKED", "");
    }

    jdbcTemplate.execute(
        sql,
        new MapSqlParameterSource()
            .addValue("teiUids", teiUids)
            .addValue("lastUpdated", new Timestamp(System.currentTimeMillis()))
            .addValue("lastUpdatedBy", (user != null ? user.getId() : null)),
        PreparedStatement::execute);
  }

  private void bindEventParamsForInsert(PreparedStatement ps, Event event)
      throws SQLException, JsonProcessingException {
    ps.setLong(1, event.getEnrollment().getId());
    ps.setLong(2, event.getProgramStage().getId());
    ps.setTimestamp(3, JdbcEventSupport.toTimestamp(event.getDueDate()));
    ps.setTimestamp(4, JdbcEventSupport.toTimestamp(event.getExecutionDate()));
    ps.setLong(5, event.getOrganisationUnit().getId());
    ps.setString(6, event.getStatus().toString());
    ps.setTimestamp(7, JdbcEventSupport.toTimestamp(event.getCompletedDate()));
    ps.setString(8, event.getUid());
    ps.setTimestamp(9, JdbcEventSupport.toTimestamp(new Date()));
    ps.setTimestamp(10, JdbcEventSupport.toTimestamp(new Date()));
    ps.setLong(11, event.getAttributeOptionCombo().getId());
    ps.setString(12, event.getStoredBy());
    ps.setObject(13, userInfoToJson(event.getCreatedByUserInfo(), jsonMapper));
    ps.setObject(14, userInfoToJson(event.getLastUpdatedByUserInfo(), jsonMapper));
    ps.setString(15, event.getCompletedBy());
    ps.setBoolean(16, false);
    ps.setString(17, event.getCode());
    ps.setTimestamp(18, JdbcEventSupport.toTimestamp(event.getCreatedAtClient()));
    ps.setTimestamp(19, JdbcEventSupport.toTimestamp(event.getLastUpdatedAtClient()));
    ps.setObject(20, JdbcEventSupport.toGeometry(event.getGeometry()));
    if (event.getAssignedUser() != null) {
      ps.setLong(21, event.getAssignedUser().getId());
    } else {
      ps.setObject(21, null);
    }
    ps.setObject(22, eventDataValuesToJson(event.getEventDataValues(), this.jsonMapper));
  }

  private MapSqlParameterSource getSqlParametersForUpdate(org.hisp.dhis.program.Event event)
      throws SQLException, JsonProcessingException {
    return new MapSqlParameterSource()
        .addValue("enrollmentid", event.getEnrollment().getId())
        .addValue("programstageid", event.getProgramStage().getId())
        .addValue(
            EventQuery.COLUMNS.DUE_DATE.getColumnName(),
            JdbcEventSupport.toTimestamp(event.getDueDate()))
        .addValue(
            EventQuery.COLUMNS.EXECUTION_DATE.getColumnName(),
            JdbcEventSupport.toTimestamp(event.getExecutionDate()))
        .addValue("organisationunitid", event.getOrganisationUnit().getId())
        .addValue(EventQuery.COLUMNS.STATUS.getColumnName(), event.getStatus().toString())
        .addValue(
            EventQuery.COLUMNS.COMPLETEDDATE.getColumnName(),
            JdbcEventSupport.toTimestamp(event.getCompletedDate()))
        .addValue(
            EventQuery.COLUMNS.UPDATED.getColumnName(), JdbcEventSupport.toTimestamp(new Date()))
        .addValue("attributeoptioncomboid", event.getAttributeOptionCombo().getId())
        .addValue(EventQuery.COLUMNS.STOREDBY.getColumnName(), event.getStoredBy())
        .addValue(
            "lastupdatedbyuserinfo", userInfoToJson(event.getLastUpdatedByUserInfo(), jsonMapper))
        .addValue(EventQuery.COLUMNS.COMPLETEDBY.getColumnName(), event.getCompletedBy())
        .addValue(EventQuery.COLUMNS.DELETED.getColumnName(), event.isDeleted())
        .addValue("code", event.getCode())
        .addValue(
            EventQuery.COLUMNS.UPDATEDCLIENT.getColumnName(),
            JdbcEventSupport.toTimestamp(event.getLastUpdatedAtClient()))
        .addValue(
            EventQuery.COLUMNS.GEOMETRY.getColumnName(),
            JdbcEventSupport.toGeometry(event.getGeometry()))
        .addValue(
            "assigneduserid",
            (event.getAssignedUser() != null ? event.getAssignedUser().getId() : null))
        .addValue("eventdatavalues", eventDataValuesToJson(event.getEventDataValues(), jsonMapper))
        .addValue(EventQuery.COLUMNS.UID.getColumnName(), event.getUid());
  }

  private Set<EventDataValue> convertEventDataValueJsonIntoSet(String jsonString) {
    try {
      Map<String, EventDataValue> data = eventDataValueJsonReader.readValue(jsonString);
      return JsonEventDataValueSetBinaryType.convertEventDataValuesMapIntoSet(data);
    } catch (IOException e) {
      log.error("Parsing EventDataValues json string failed. String value: " + jsonString);
      throw new IllegalArgumentException(e);
    }
  }

  private void convertDataValuesIdentifiers(
      IdScheme idScheme,
      List<Collection<DataValue>> dataValuesList,
      CachingMap<String, String> dataElementUidToIdentifierCache) {
    for (Collection<DataValue> dataValues : dataValuesList) {
      for (DataValue dv : dataValues) {
        String deUid = dv.getDataElement();
        String deIdentifier = dataElementUidToIdentifierCache.get(deUid);

        if (StringUtils.isEmpty(deIdentifier)) {
          throw new IllegalStateException(
              "DataElement: "
                  + deUid
                  + " does not have a value assigned for idScheme "
                  + idScheme.name());
        }

        dv.setDataElement(deIdentifier);
      }
    }
  }

  private void populateCache(
      IdScheme idScheme,
      List<Collection<DataValue>> dataValuesList,
      CachingMap<String, String> dataElementUidToIdentifierCache) {
    Set<String> deUids = new HashSet<>();

    for (Collection<DataValue> dataValues : dataValuesList) {
      for (DataValue dv : dataValues) {
        deUids.add(dv.getDataElement());
      }
    }

    if (!idScheme.isAttribute()) {
      List<DataElement> dataElements = manager.getByUid(DataElement.class, deUids);
      dataElements.forEach(de -> dataElementUidToIdentifierCache.put(de.getUid(), de.getCode()));
    } else {
      if (!deUids.isEmpty()) {
        String sql =
            "select de.uid, de.attributevalues #>> :attributeValuePath::text[] as value "
                + "from dataelement de where de.uid in (:dataElementUids) "
                + "and de.attributevalues ?? :attributeUid";

        SqlRowSet deRowSet =
            jdbcTemplate.queryForRowSet(
                sql,
                new MapSqlParameterSource()
                    .addValue("attributeValuePath", "{" + idScheme.getAttribute() + ",value}")
                    .addValue("attributeUid", idScheme.getAttribute())
                    .addValue("dataElementUids", deUids));

        while (deRowSet.next()) {
          dataElementUidToIdentifierCache.put(
              deRowSet.getString("uid"), deRowSet.getString("value"));
        }
      }
    }
  }

  @Override
  public void delete(final List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events) {
    if (isNotEmpty(events)) {
      final List<String> psiUids =
          events.stream()
              .map(org.hisp.dhis.dxf2.deprecated.tracker.event.Event::getEvent)
              .collect(toList());

      jdbcTemplate.update(
          "UPDATE event SET deleted = true where uid in (:uids)",
          new MapSqlParameterSource().addValue("uids", psiUids));
    }
  }

  private void setAccessiblePrograms(User user, EventSearchParams params) {
    if (!isSuper(user)) {
      params.setAccessiblePrograms(
          manager.getDataReadAll(Program.class).stream()
              .map(Program::getUid)
              .collect(Collectors.toSet()));

      params.setAccessibleProgramStages(
          manager.getDataReadAll(ProgramStage.class).stream()
              .map(ProgramStage::getUid)
              .collect(Collectors.toSet()));
    }
  }

  /** Sort the list of {@see Event} by UID */
  private List<Event> sort(List<Event> batch) {
    return batch.stream()
        .sorted(Comparator.comparing(org.hisp.dhis.program.Event::getUid))
        .collect(toList());
  }

  private String getOrgUnitSql(EventSearchParams params, String ouTable) {
    return switch (params.getOrgUnitSelectionMode()) {
      case SELECTED -> getSelectedOrgUnitPath(params.getAccessibleOrgUnits(), ouTable);
      case CHILDREN -> getChildrenOrgUnitsPath(params.getAccessibleOrgUnits(), ouTable);
      case ALL -> null;
      default -> getOrgUnitsPath(params.getAccessibleOrgUnits(), ouTable);
    };
  }

  private String getChildrenOrgUnitsPath(List<OrganisationUnit> orgUnits, String ouTable) {
    StringJoiner orgUnitSqlJoiner = new StringJoiner(" or ");

    for (OrganisationUnit orgUnit : orgUnits) {
      orgUnitSqlJoiner.add(
          ouTable
              + "."
              + PATH_LIKE
              + " '%"
              + orgUnit.getPath()
              + "%' "
              + " and "
              + ouTable
              + "."
              + "hierarchylevel = "
              + orgUnit.getLevel());
    }

    return orgUnitSqlJoiner.toString();
  }

  private String getSelectedOrgUnitPath(List<OrganisationUnit> orgUnits, String ouTable) {
    return orgUnits.isEmpty()
        ? null
        : ouTable + "." + PATH_EQ + " '" + orgUnits.get(0).getPath() + "' ";
  }

  private String getOrgUnitsPath(List<OrganisationUnit> orgUnits, String ouTable) {
    StringJoiner orgUnitSqlJoiner = new StringJoiner(" or ");

    for (OrganisationUnit orgUnit : orgUnits) {
      orgUnitSqlJoiner.add(ouTable + "." + PATH_LIKE + " '%" + orgUnit.getPath() + "%' ");
    }

    return orgUnitSqlJoiner.toString();
  }
}
