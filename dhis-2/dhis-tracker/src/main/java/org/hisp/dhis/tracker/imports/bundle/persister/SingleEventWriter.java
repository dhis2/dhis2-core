/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.SRID;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.bigintArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.booleanArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.geometryText;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.textArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toEventDataValuesJson;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamptz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hisp.dhis.tracker.model.SingleEvent;

/**
 * Flushes staged {@link SingleEvent} writes. Mirrors {@link TrackerEventWriter} but writes to
 * {@code singleevent} (no {@code enrollmentid}, no {@code scheduleddate}); ids are pre-allocated
 * from {@code singleevent_sequence}. New notes cascade into {@code note} + {@code
 * singleevent_notes}.
 */
final class SingleEventWriter extends UpsertTableWriter<SingleEvent> {

  // Constant-text INSERT ... SELECT unnest(...) so pgjdbc's prepared-statement cache engages
  // regardless of row count. INSERT column order, the SELECT projection and the unnest aliases are
  // kept in lockstep.
  private static final String INSERT_SQL =
      "insert into singleevent ("
          + "eventid, uid, created, lastupdated, createdatclient, lastupdatedatclient,"
          + " createdbyuserinfo, lastupdatedbyuserinfo, status, occurreddate, completeddate,"
          + " completedby, deleted, lastsynchronized,"
          + " programstageid, organisationunitid, attributeoptioncomboid,"
          + " assigneduserid, eventdatavalues, geometry)"
          + " select eventid, uid, created, lastupdated, createdatclient, lastupdatedatclient,"
          + " createdbyuserinfo::jsonb, lastupdatedbyuserinfo::jsonb, status, occurreddate,"
          + " completeddate, completedby, deleted, lastsynchronized,"
          + " programstageid, organisationunitid, attributeoptioncomboid,"
          + " assigneduserid, eventdatavalues::jsonb,"
          + " case when geometry is null then null else ST_GeomFromText(geometry, "
          + SRID
          + ") end"
          + " from ( select"
          + " unnest(?::bigint[]) as eventid,"
          + " unnest(?::text[]) as uid,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::text[]) as createdbyuserinfo,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo,"
          + " unnest(?::text[]) as status,"
          + " unnest(?::timestamptz[]) as occurreddate,"
          + " unnest(?::timestamptz[]) as completeddate,"
          + " unnest(?::text[]) as completedby,"
          + " unnest(?::boolean[]) as deleted,"
          + " unnest(?::timestamptz[]) as lastsynchronized,"
          + " unnest(?::bigint[]) as programstageid,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as attributeoptioncomboid,"
          + " unnest(?::bigint[]) as assigneduserid,"
          + " unnest(?::text[]) as eventdatavalues,"
          + " unnest(?::text[]) as geometry"
          + " ) v";

  // Columns mutated by TrackerObjectsMapper.mapSingleEvent outside the new-entity branch. Insert-
  // only columns (uid, created, createdbyuserinfo, deleted) and columns owned by other code paths
  // (lastsynchronized) are excluded.
  private static final String UPDATE_SQL =
      "update singleevent ev set"
          + " lastupdated = v.lastupdated,"
          + " createdatclient = v.createdatclient,"
          + " lastupdatedatclient = v.lastupdatedatclient,"
          + " lastupdatedbyuserinfo = v.lastupdatedbyuserinfo::jsonb,"
          + " programstageid = v.programstageid,"
          + " organisationunitid = v.organisationunitid,"
          + " attributeoptioncomboid = v.attributeoptioncomboid,"
          + " status = v.status,"
          + " occurreddate = v.occurreddate,"
          + " completeddate = v.completeddate,"
          + " completedby = v.completedby,"
          + " assigneduserid = v.assigneduserid,"
          + " eventdatavalues = v.eventdatavalues::jsonb,"
          + " geometry = case when v.geometry is null then null"
          + " else ST_GeomFromText(v.geometry, "
          + SRID
          + ") end"
          + " from ( select"
          + " unnest(?::bigint[]) as eventid,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo,"
          + " unnest(?::bigint[]) as programstageid,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as attributeoptioncomboid,"
          + " unnest(?::text[]) as status,"
          + " unnest(?::timestamptz[]) as occurreddate,"
          + " unnest(?::timestamptz[]) as completeddate,"
          + " unnest(?::text[]) as completedby,"
          + " unnest(?::bigint[]) as assigneduserid,"
          + " unnest(?::text[]) as eventdatavalues,"
          + " unnest(?::text[]) as geometry"
          + " ) v where ev.eventid = v.eventid";

  private final UserInfoJsonCache userInfo;
  private final NoteCascadeWriter notes = new NoteCascadeWriter("singleevent_notes", "eventid");

  SingleEventWriter(UserInfoJsonCache userInfo) {
    this.userInfo = userInfo;
  }

  @Override
  void flush(Connection conn) throws SQLException {
    insert(conn);
    update(conn);
    notes.cascade(conn, inserts, updates, SingleEvent::getId, SingleEvent::getNotes);
  }

  private void insert(Connection conn) throws SQLException {
    forEachChunk(
        inserts,
        chunk -> {
          for (SingleEvent e : chunk) {
            requirePreallocatedId(e);
          }
          try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, SingleEvent::getId));
            ps.setArray(p++, textArray(conn, chunk, SingleEvent::getUid));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getCreated())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getLastUpdated())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getCreatedAtClient())));
            ps.setArray(
                p++, textArray(conn, chunk, e -> toTimestamptz(e.getLastUpdatedAtClient())));
            ps.setArray(
                p++, textArray(conn, chunk, e -> userInfo.toJson(e.getCreatedByUserInfo())));
            ps.setArray(
                p++, textArray(conn, chunk, e -> userInfo.toJson(e.getLastUpdatedByUserInfo())));
            ps.setArray(p++, textArray(conn, chunk, e -> e.getStatus().name()));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getOccurredDate())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getCompletedDate())));
            ps.setArray(p++, textArray(conn, chunk, SingleEvent::getCompletedBy));
            ps.setArray(p++, booleanArray(conn, chunk, SingleEvent::isDeleted));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getLastSynchronized())));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getProgramStage().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getOrganisationUnit().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getAttributeOptionCombo().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, SingleEventWriter::assignedUserId));
            ps.setArray(
                p++, textArray(conn, chunk, e -> toEventDataValuesJson(e.getEventDataValues())));
            ps.setArray(p++, textArray(conn, chunk, e -> geometryText(e.getGeometry())));
            ps.executeUpdate();
          }
        });
  }

  private static Long assignedUserId(SingleEvent e) {
    return e.getAssignedUser() != null ? e.getAssignedUser().getId() : null;
  }

  private static void requirePreallocatedId(SingleEvent e) throws SQLException {
    if (e.getId() == 0) {
      throw new SQLException(
          "SingleEvent "
              + e.getUid()
              + " has no pre-allocated id; AbstractTrackerPersister"
              + " must pre-allocate ids when sequenceName() is non-null.");
    }
  }

  private void update(Connection conn) throws SQLException {
    forEachChunk(
        updates,
        chunk -> {
          int n = chunk.size();

          Long[] ids = new Long[n];
          String[] lastUpdated = new String[n];
          String[] createdAtClient = new String[n];
          String[] lastUpdatedAtClient = new String[n];
          String[] lastUpdatedByUserInfo = new String[n];
          Long[] programStageIds = new Long[n];
          Long[] organisationUnitIds = new Long[n];
          Long[] attributeOptionComboIds = new Long[n];
          String[] status = new String[n];
          String[] occurredDate = new String[n];
          String[] completedDate = new String[n];
          String[] completedBy = new String[n];
          Long[] assignedUserIds = new Long[n];
          String[] eventDataValues = new String[n];
          String[] geometry = new String[n];

          for (int i = 0; i < n; i++) {
            SingleEvent e = chunk.get(i);
            ids[i] = e.getId();
            lastUpdated[i] = toTimestamptz(e.getLastUpdated());
            createdAtClient[i] = toTimestamptz(e.getCreatedAtClient());
            lastUpdatedAtClient[i] = toTimestamptz(e.getLastUpdatedAtClient());
            lastUpdatedByUserInfo[i] = userInfo.toJson(e.getLastUpdatedByUserInfo());
            programStageIds[i] = e.getProgramStage().getId();
            organisationUnitIds[i] = e.getOrganisationUnit().getId();
            attributeOptionComboIds[i] = e.getAttributeOptionCombo().getId();
            status[i] = e.getStatus().name();
            occurredDate[i] = toTimestamptz(e.getOccurredDate());
            completedDate[i] = toTimestamptz(e.getCompletedDate());
            completedBy[i] = e.getCompletedBy();
            assignedUserIds[i] = e.getAssignedUser() != null ? e.getAssignedUser().getId() : null;
            eventDataValues[i] = toEventDataValuesJson(e.getEventDataValues());
            geometry[i] = e.getGeometry() != null ? e.getGeometry().toText() : null;
          }

          try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setArray(1, conn.createArrayOf("bigint", ids));
            ps.setArray(2, conn.createArrayOf("text", lastUpdated));
            ps.setArray(3, conn.createArrayOf("text", createdAtClient));
            ps.setArray(4, conn.createArrayOf("text", lastUpdatedAtClient));
            ps.setArray(5, conn.createArrayOf("text", lastUpdatedByUserInfo));
            ps.setArray(6, conn.createArrayOf("bigint", programStageIds));
            ps.setArray(7, conn.createArrayOf("bigint", organisationUnitIds));
            ps.setArray(8, conn.createArrayOf("bigint", attributeOptionComboIds));
            ps.setArray(9, conn.createArrayOf("text", status));
            ps.setArray(10, conn.createArrayOf("text", occurredDate));
            ps.setArray(11, conn.createArrayOf("text", completedDate));
            ps.setArray(12, conn.createArrayOf("text", completedBy));
            ps.setArray(13, conn.createArrayOf("bigint", assignedUserIds));
            ps.setArray(14, conn.createArrayOf("text", eventDataValues));
            ps.setArray(15, conn.createArrayOf("text", geometry));
            ps.executeUpdate();
          }
        });
  }
}
