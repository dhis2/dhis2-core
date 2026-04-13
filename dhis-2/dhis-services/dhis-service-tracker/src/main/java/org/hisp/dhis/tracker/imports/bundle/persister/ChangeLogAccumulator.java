/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.bundle.persister;

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
import javax.persistence.EntityManager;
import org.hibernate.Session;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.jasypt.encryption.pbe.PBEStringEncryptor;

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

  private final boolean enabled;

  // trackedentityattributevalueaudit columns:
  // trackedentityid, trackedentityattributeid, value, encryptedvalue, created, modifiedby,
  // audittype
  private static final String TE_AUDIT_TUPLE = "(?,?,?,?,?,?,?)";
  private static final String TE_AUDIT_VALUES = multiRowValues(TE_AUDIT_TUPLE, MAX_ROWS_PER_INSERT);
  private static final String INSERT_TE_AUDIT =
      "insert into trackedentityattributevalueaudit"
          + " (trackedentityid, trackedentityattributeid, value, encryptedvalue,"
          + " created, modifiedby, audittype) values ";

  // trackedentitydatavalueaudit columns:
  // eventid, dataelementid, value, providedelsewhere, created, modifiedby, audittype
  private static final String EVENT_AUDIT_TUPLE = "(?,?,?,?,?,?,?)";
  private static final String EVENT_AUDIT_VALUES =
      multiRowValues(EVENT_AUDIT_TUPLE, MAX_ROWS_PER_INSERT);
  private static final String INSERT_EVENT_AUDIT =
      "insert into trackedentitydatavalueaudit"
          + " (eventid, dataelementid, value, providedelsewhere,"
          + " created, modifiedby, audittype) values ";

  private final PBEStringEncryptor encryptor;
  private final Date created = new Date();
  private final List<TeAttributeAudit> teAudits = new ArrayList<>();
  private final List<EventDataValueAudit> eventAudits = new ArrayList<>();

  ChangeLogAccumulator(boolean enabled, PBEStringEncryptor encryptor) {
    this.enabled = enabled;
    this.encryptor = encryptor;
  }

  void addTrackedEntityAttributeValueAudit(
      @Nonnull TrackedEntity trackedEntity,
      @Nonnull TrackedEntityAttribute attribute,
      @CheckForNull String value,
      @Nonnull ChangeLogType type,
      @Nonnull String username) {
    if (!enabled) return;
    teAudits.add(new TeAttributeAudit(trackedEntity, attribute, value, type, username));
  }

  void addEventDataValueAudit(
      @Nonnull Event event,
      @Nonnull DataElement dataElement,
      @CheckForNull String value,
      boolean providedElsewhere,
      @Nonnull ChangeLogType type,
      @Nonnull String username) {
    if (!enabled) return;
    eventAudits.add(
        new EventDataValueAudit(event, dataElement, value, providedElsewhere, type, username));
  }

  Mark mark() {
    return new Mark(teAudits.size(), eventAudits.size());
  }

  void rollbackTo(Mark mark) {
    truncate(teAudits, mark.teSize);
    truncate(eventAudits, mark.eventSize);
  }

  void flushAll(EntityManager entityManager) {
    if (teAudits.isEmpty() && eventAudits.isEmpty()) {
      return;
    }

    Session session = entityManager.unwrap(Session.class);
    session.doWork(this::insertAll);
    teAudits.clear();
    eventAudits.clear();
  }

  private void insertAll(Connection connection) throws SQLException {
    Timestamp timestamp = new Timestamp(created.getTime());
    insertTeAudits(connection, timestamp);
    insertEventAudits(connection, timestamp);
  }

  private void insertTeAudits(Connection connection, Timestamp timestamp) throws SQLException {
    for (int offset = 0; offset < teAudits.size(); offset += MAX_ROWS_PER_INSERT) {
      int end = Math.min(offset + MAX_ROWS_PER_INSERT, teAudits.size());
      int batchSize = end - offset;
      String values =
          batchSize == MAX_ROWS_PER_INSERT
              ? TE_AUDIT_VALUES
              : multiRowValues(TE_AUDIT_TUPLE, batchSize);
      String sql = INSERT_TE_AUDIT + values;

      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        int idx = 1;
        for (int i = offset; i < end; i++) {
          TeAttributeAudit a = teAudits.get(i);
          ps.setLong(idx++, a.trackedEntity.getId());
          ps.setLong(idx++, a.attribute.getId());
          if (a.attribute.getConfidential()) {
            ps.setNull(idx++, Types.VARCHAR); // value (plain) = null
            if (a.value != null) {
              ps.setString(idx++, encryptor.encrypt(a.value)); // encryptedvalue
            } else {
              ps.setNull(idx++, Types.VARCHAR);
            }
          } else {
            ps.setString(idx++, a.value); // value (plain)
            ps.setNull(idx++, Types.VARCHAR); // encryptedvalue = null
          }
          ps.setTimestamp(idx++, timestamp);
          ps.setString(idx++, a.username);
          ps.setString(idx++, a.type.name());
        }
        ps.executeUpdate();
      }
    }
  }

  private void insertEventAudits(Connection connection, Timestamp timestamp) throws SQLException {
    for (int offset = 0; offset < eventAudits.size(); offset += MAX_ROWS_PER_INSERT) {
      int end = Math.min(offset + MAX_ROWS_PER_INSERT, eventAudits.size());
      int batchSize = end - offset;
      String values =
          batchSize == MAX_ROWS_PER_INSERT
              ? EVENT_AUDIT_VALUES
              : multiRowValues(EVENT_AUDIT_TUPLE, batchSize);
      String sql = INSERT_EVENT_AUDIT + values;

      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        int idx = 1;
        for (int i = offset; i < end; i++) {
          EventDataValueAudit a = eventAudits.get(i);
          ps.setLong(idx++, a.event.getId());
          ps.setLong(idx++, a.dataElement.getId());
          ps.setString(idx++, a.value);
          ps.setBoolean(idx++, a.providedElsewhere);
          ps.setTimestamp(idx++, timestamp);
          ps.setString(idx++, a.username);
          ps.setString(idx++, a.type.name());
        }
        ps.executeUpdate();
      }
    }
  }

  private static String multiRowValues(String tuple, int count) {
    return (tuple + ",").repeat(count - 1) + tuple;
  }

  private static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  record Mark(int teSize, int eventSize) {}

  private record TeAttributeAudit(
      TrackedEntity trackedEntity,
      TrackedEntityAttribute attribute,
      String value,
      ChangeLogType type,
      String username) {}

  private record EventDataValueAudit(
      Event event,
      DataElement dataElement,
      String value,
      boolean providedElsewhere,
      ChangeLogType type,
      String username) {}
}
