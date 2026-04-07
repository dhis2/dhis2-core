/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.bundle.persister;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.event.EventChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;

/**
 * Collects changelog entries during the persist phase and inserts them via multi-row INSERT at the
 * end. This avoids Hibernate's per-entity sequence round-trips and groups all changelog INSERTs
 * separately from entity INSERTs/UPDATEs. Multi-row INSERT is processed by PostgreSQL as a single
 * statement (one parse, one plan, one WAL entry batch) instead of individual per-row executes.
 */
class ChangeLogAccumulator {

  /**
   * Maximum rows per multi-row INSERT statement. pgjdbc found that performance does not improve
   * much beyond 128 rows per multi-valued INSERT. The hard upper limit is {@link Short#MAX_VALUE}
   * (32,767) bind parameters per statement, which gives ~4,000 rows with 8 columns. 128 rows is
   * well within that.
   *
   * @see <a
   *     href="https://github.com/pgjdbc/pgjdbc/blob/c44a2a99/pgjdbc/src/main/java/org/postgresql/jdbc/PgPreparedStatement.java#L1815-L1821">PgPreparedStatement.java#L1815-L1821</a>
   */
  private static final int MAX_ROWS_PER_INSERT = 128;

  private static final String TE_CHANGELOG_TUPLE = "(?,?,?,?,?,?,?)";
  private static final String TE_CHANGELOG_VALUES =
      multiRowValues(TE_CHANGELOG_TUPLE, MAX_ROWS_PER_INSERT);
  private static final String INSERT_TE_CHANGELOG =
      "insert into trackedentitychangelog"
          + " (trackedentityid, trackedentityattributeid, previousvalue, currentvalue,"
          + " changelogtype, created, createdby) values ";

  private static final String EVENT_CHANGELOG_TUPLE = "(?,?,?,?,?,?,?,?)";
  private static final String EVENT_CHANGELOG_VALUES =
      multiRowValues(EVENT_CHANGELOG_TUPLE, MAX_ROWS_PER_INSERT);
  private static final String INSERT_EVENT_CHANGELOG =
      "insert into eventchangelog"
          + " (eventid, dataelementid, eventfield, previousvalue, currentvalue,"
          + " changelogtype, created, createdby) values ";

  private final Date created = new Date();
  private final List<TrackedEntityChangeLog> teChangeLogs = new ArrayList<>();
  private final List<EventChangeLog> eventChangeLogs = new ArrayList<>();

  void addTrackedEntityChangeLog(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull TrackedEntityAttribute attribute,
      @CheckForNull String previousValue,
      @CheckForNull String currentValue,
      @Nonnull ChangeLogType type,
      @Nonnull String username) {
    teChangeLogs.add(
        new TrackedEntityChangeLog(
            trackedEntity, attribute, previousValue, currentValue, type, created, username));
  }

  void addEventChangeLog(
      @Nonnull Event event,
      @Nonnull DataElement dataElement,
      @CheckForNull String previousValue,
      @CheckForNull String currentValue,
      @Nonnull ChangeLogType type,
      @Nonnull String username) {
    eventChangeLogs.add(
        new EventChangeLog(
            event, dataElement, null, previousValue, currentValue, type, created, username));
  }

  void addEventFieldChangeLog(
      @Nonnull Event event,
      @Nonnull String eventField,
      @CheckForNull String previousValue,
      @CheckForNull String currentValue,
      @Nonnull ChangeLogType type,
      @Nonnull String username) {
    eventChangeLogs.add(
        new EventChangeLog(
            event, null, eventField, previousValue, currentValue, type, created, username));
  }

  Mark mark() {
    return new Mark(teChangeLogs.size(), eventChangeLogs.size());
  }

  void rollbackTo(Mark mark) {
    truncate(teChangeLogs, mark.teSize);
    truncate(eventChangeLogs, mark.eventSize);
  }

  void flushAll(EntityManager entityManager) {
    if (teChangeLogs.isEmpty() && eventChangeLogs.isEmpty()) {
      return;
    }

    Session session = entityManager.unwrap(Session.class);
    session.doWork(this::insertAll);
    teChangeLogs.clear();
    eventChangeLogs.clear();
  }

  private void insertAll(Connection connection) throws SQLException {
    Timestamp timestamp = new Timestamp(created.getTime());
    insertTeChangeLogs(connection, timestamp);
    insertEventChangeLogs(connection, timestamp);
  }

  private void insertTeChangeLogs(Connection connection, Timestamp timestamp) throws SQLException {
    for (int offset = 0; offset < teChangeLogs.size(); offset += MAX_ROWS_PER_INSERT) {
      int end = Math.min(offset + MAX_ROWS_PER_INSERT, teChangeLogs.size());
      int batchSize = end - offset;
      String values =
          batchSize == MAX_ROWS_PER_INSERT
              ? TE_CHANGELOG_VALUES
              : multiRowValues(TE_CHANGELOG_TUPLE, batchSize);
      String sql = INSERT_TE_CHANGELOG + values;

      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        int idx = 1;
        for (int i = offset; i < end; i++) {
          TrackedEntityChangeLog cl = teChangeLogs.get(i);
          ps.setLong(idx++, cl.getTrackedEntity().getId());
          ps.setLong(idx++, cl.getTrackedEntityAttribute().getId());
          ps.setString(idx++, cl.getPreviousValue());
          ps.setString(idx++, cl.getCurrentValue());
          ps.setString(idx++, cl.getChangeLogType().name());
          ps.setTimestamp(idx++, timestamp);
          ps.setString(idx++, cl.getCreatedByUsername());
        }
        ps.executeUpdate();
      }
    }
  }

  private void insertEventChangeLogs(Connection connection, Timestamp timestamp)
      throws SQLException {
    for (int offset = 0; offset < eventChangeLogs.size(); offset += MAX_ROWS_PER_INSERT) {
      int end = Math.min(offset + MAX_ROWS_PER_INSERT, eventChangeLogs.size());
      int batchSize = end - offset;
      String values =
          batchSize == MAX_ROWS_PER_INSERT
              ? EVENT_CHANGELOG_VALUES
              : multiRowValues(EVENT_CHANGELOG_TUPLE, batchSize);
      String sql = INSERT_EVENT_CHANGELOG + values;

      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        int idx = 1;
        for (int i = offset; i < end; i++) {
          EventChangeLog cl = eventChangeLogs.get(i);
          ps.setLong(idx++, cl.getEvent().getId());
          setNullableLong(ps, idx++, cl.getDataElement());
          ps.setString(idx++, cl.getEventField());
          ps.setString(idx++, cl.getPreviousValue());
          ps.setString(idx++, cl.getCurrentValue());
          ps.setString(idx++, cl.getChangeLogType().name());
          ps.setTimestamp(idx++, timestamp);
          ps.setString(idx++, cl.getCreatedByUsername());
        }
        ps.executeUpdate();
      }
    }
  }

  private static String multiRowValues(String tuple, int count) {
    return (tuple + ",").repeat(count - 1) + tuple;
  }

  private static void setNullableLong(PreparedStatement ps, int index, Object obj)
      throws SQLException {
    if (obj instanceof org.hisp.dhis.common.IdentifiableObject io) {
      ps.setLong(index, io.getId());
    } else {
      ps.setNull(index, Types.BIGINT);
    }
  }

  private static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  record Mark(int teSize, int eventSize) {}
}
