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
import java.util.List;
import org.hibernate.Session;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLog;

/**
 * Collects changelog entries during the persist phase and batch-inserts them via JDBC at the end.
 * This avoids Hibernate's per-entity sequence round-trips and enables JDBC batching, grouping all
 * changelog INSERTs separately from entity INSERTs/UPDATEs.
 */
class ChangeLogAccumulator {
  private static final String INSERT_TE_CHANGELOG =
      """
      insert into trackedentitychangelog
        (trackedentityid, trackedentityattributeid, previousvalue, currentvalue,
         changelogtype, created, createdby)
      values (?, ?, ?, ?, ?, ?, ?)""";

  private static final String INSERT_TRACKER_EVENT_CHANGELOG =
      """
      insert into trackereventchangelog
        (eventid, dataelementid, eventfield, previousvalue, currentvalue,
         changelogtype, created, createdby)
      values (?, ?, ?, ?, ?, ?, ?, ?)""";

  private static final String INSERT_SINGLE_EVENT_CHANGELOG =
      """
      insert into singleeventchangelog
        (eventid, dataelementid, eventfield, previousvalue, currentvalue,
         changelogtype, created, createdby)
      values (?, ?, ?, ?, ?, ?, ?, ?)""";

  private final List<TrackedEntityChangeLog> teChangeLogs = new ArrayList<>();
  private final List<TrackerEventChangeLog> trackerEventChangeLogs = new ArrayList<>();
  private final List<SingleEventChangeLog> singleEventChangeLogs = new ArrayList<>();

  void addTrackedEntityChangeLog(TrackedEntityChangeLog changeLog) {
    teChangeLogs.add(changeLog);
  }

  void addTrackerEventChangeLog(TrackerEventChangeLog changeLog) {
    trackerEventChangeLogs.add(changeLog);
  }

  void addSingleEventChangeLog(SingleEventChangeLog changeLog) {
    singleEventChangeLogs.add(changeLog);
  }

  Mark mark() {
    return new Mark(
        teChangeLogs.size(), trackerEventChangeLogs.size(), singleEventChangeLogs.size());
  }

  void rollbackToMark(Mark mark) {
    truncate(teChangeLogs, mark.teSize);
    truncate(trackerEventChangeLogs, mark.trackerEventSize);
    truncate(singleEventChangeLogs, mark.singleEventSize);
  }

  void flushAll(EntityManager entityManager) {
    if (teChangeLogs.isEmpty()
        && trackerEventChangeLogs.isEmpty()
        && singleEventChangeLogs.isEmpty()) {
      return;
    }

    Session session = entityManager.unwrap(Session.class);
    session.doWork(this::insertAll);
    teChangeLogs.clear();
    trackerEventChangeLogs.clear();
    singleEventChangeLogs.clear();
  }

  private void insertAll(Connection connection) throws SQLException {
    if (!teChangeLogs.isEmpty()) {
      try (PreparedStatement ps = connection.prepareStatement(INSERT_TE_CHANGELOG)) {
        for (TrackedEntityChangeLog cl : teChangeLogs) {
          ps.setLong(1, cl.getTrackedEntity().getId());
          ps.setLong(2, cl.getTrackedEntityAttribute().getId());
          ps.setString(3, cl.getPreviousValue());
          ps.setString(4, cl.getCurrentValue());
          ps.setString(5, cl.getChangeLogType().name());
          ps.setTimestamp(6, new Timestamp(cl.getCreated().getTime()));
          ps.setString(7, cl.getCreatedByUsername());
          ps.addBatch();
        }
        ps.executeBatch();
      }
    }

    if (!trackerEventChangeLogs.isEmpty()) {
      try (PreparedStatement ps = connection.prepareStatement(INSERT_TRACKER_EVENT_CHANGELOG)) {
        for (TrackerEventChangeLog cl : trackerEventChangeLogs) {
          ps.setLong(1, cl.getEvent().getId());
          setNullableLong(ps, 2, cl.getDataElement());
          ps.setString(3, cl.getEventField());
          ps.setString(4, cl.getPreviousValue());
          ps.setString(5, cl.getCurrentValue());
          ps.setString(6, cl.getChangeLogType().name());
          ps.setTimestamp(7, new Timestamp(cl.getCreated().getTime()));
          ps.setString(8, cl.getCreatedBy());
          ps.addBatch();
        }
        ps.executeBatch();
      }
    }

    if (!singleEventChangeLogs.isEmpty()) {
      try (PreparedStatement ps = connection.prepareStatement(INSERT_SINGLE_EVENT_CHANGELOG)) {
        for (SingleEventChangeLog cl : singleEventChangeLogs) {
          ps.setLong(1, cl.getEvent().getId());
          setNullableLong(ps, 2, cl.getDataElement());
          ps.setString(3, cl.getEventField());
          ps.setString(4, cl.getPreviousValue());
          ps.setString(5, cl.getCurrentValue());
          ps.setString(6, cl.getChangeLogType().name());
          ps.setTimestamp(7, new Timestamp(cl.getCreated().getTime()));
          ps.setString(8, cl.getCreatedBy());
          ps.addBatch();
        }
        ps.executeBatch();
      }
    }
  }

  private static void setNullableLong(
      PreparedStatement ps, int index, org.hisp.dhis.common.IdentifiableObject obj)
      throws SQLException {
    if (obj != null) {
      ps.setLong(index, obj.getId());
    } else {
      ps.setNull(index, Types.BIGINT);
    }
  }

  private static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  record Mark(int teSize, int trackerEventSize, int singleEventSize) {}
}
