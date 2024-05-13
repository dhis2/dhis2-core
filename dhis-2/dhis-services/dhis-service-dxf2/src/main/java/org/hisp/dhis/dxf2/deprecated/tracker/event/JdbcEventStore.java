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
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.eventDataValuesToJson;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.userInfoToJson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.EventQuery;
import org.hisp.dhis.jdbc.BatchPreparedStatementSetterWithKeyHolder;
import org.hisp.dhis.jdbc.JdbcUtils;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository("org.hisp.dhis.dxf2.events.event.EventStore")
@RequiredArgsConstructor
public class JdbcEventStore implements EventStore {

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

  /**
   * Updates Tracked Entity Instance after an event update. In order to prevent deadlocks, SELECT
   * ... FOR UPDATE SKIP LOCKED is used before the actual UPDATE statement. This prevents deadlocks
   * when Postgres tries to update the same TEI.
   */
  private static final String UPDATE_TEI_SQL =
      "select * from trackedentity where uid in (:teiUids) for update skip locked;"
          + "update trackedentity set lastupdated = :lastUpdated, lastupdatedby = :lastUpdatedBy where uid in (:teiUids)";

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

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Qualifier("dataValueJsonMapper")
  private final ObjectMapper jsonMapper;

  private final SkipLockedProvider skipLockedProvider;

  // -------------------------------------------------------------------------
  // EventStore implementation
  // -------------------------------------------------------------------------

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
          log.warn("PSI failed to update and will be ignored: '{}'", events.get(i).getUid(), e);
        }
      }

      jdbcTemplate.batchUpdate(UPDATE_EVENT_SQL, parameters);
    } catch (DataAccessException e) {
      log.error("Error updating events", e);
      throw e;
    }

    return events;
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
              log.warn("PSI failed to persist and will be ignored: '{}'", event.getUid(), e);
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
    ps.setTimestamp(3, JdbcEventSupport.toTimestamp(event.getScheduledDate()));
    ps.setTimestamp(4, JdbcEventSupport.toTimestamp(event.getOccurredDate()));
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
            JdbcEventSupport.toTimestamp(event.getScheduledDate()))
        .addValue(
            EventQuery.COLUMNS.EXECUTION_DATE.getColumnName(),
            JdbcEventSupport.toTimestamp(event.getOccurredDate()))
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

  /** Sort the list of {@see Event} by UID */
  private List<Event> sort(List<Event> batch) {
    return batch.stream()
        .sorted(Comparator.comparing(org.hisp.dhis.program.Event::getUid))
        .collect(toList());
  }
}
