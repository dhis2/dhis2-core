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
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamptz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hisp.dhis.tracker.model.Enrollment;

/**
 * Flushes staged {@link Enrollment} writes: a multi-row JDBC INSERT against {@code enrollment} (ids
 * pre-allocated from {@code enrollment_sequence}), a single unnest UPDATE, and a cascade insert of
 * any new notes into {@code note} + {@code enrollment_notes}.
 */
final class EnrollmentWriter extends UpsertTableWriter<Enrollment> {

  // Constant-text INSERT ... SELECT unnest(...) so pgjdbc's prepared-statement cache engages
  // regardless of row count. INSERT column order, the SELECT projection and the unnest aliases are
  // kept in lockstep.
  private static final String INSERT_SQL =
      "insert into enrollment ("
          + "enrollmentid, uid, created, lastupdated, createdatclient, lastupdatedatclient,"
          + " createdbyuserinfo, lastupdatedbyuserinfo, enrollmentdate, occurreddate, completeddate,"
          + " completedby, status, followup, deleted,"
          + " trackedentityid, programid, organisationunitid, attributeoptioncomboid, geometry)"
          + " select enrollmentid, uid, created, lastupdated, createdatclient, lastupdatedatclient,"
          + " createdbyuserinfo::jsonb, lastupdatedbyuserinfo::jsonb, enrollmentdate, occurreddate,"
          + " completeddate, completedby, status, followup, deleted,"
          + " trackedentityid, programid, organisationunitid, attributeoptioncomboid,"
          + " case when geometry is null then null else ST_GeomFromText(geometry, "
          + SRID
          + ") end"
          + " from ( select"
          + " unnest(?::bigint[]) as enrollmentid,"
          + " unnest(?::text[]) as uid,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::text[]) as createdbyuserinfo,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo,"
          + " unnest(?::timestamptz[]) as enrollmentdate,"
          + " unnest(?::timestamptz[]) as occurreddate,"
          + " unnest(?::timestamptz[]) as completeddate,"
          + " unnest(?::text[]) as completedby,"
          + " unnest(?::text[]) as status,"
          + " unnest(?::boolean[]) as followup,"
          + " unnest(?::boolean[]) as deleted,"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as programid,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as attributeoptioncomboid,"
          + " unnest(?::text[]) as geometry"
          + " ) v";

  // Columns mutated by TrackerObjectsMapper.map(Enrollment) outside the new-entity branch. Insert-
  // only columns (uid, created, createdbyuserinfo) and columns owned by other code paths (deleted)
  // are deliberately excluded. status, completeddate, completedby are included even though the
  // mapper only touches them on a status change -- writing the unchanged values back is a no-op
  // against the existing row.
  private static final String UPDATE_SQL =
      "update enrollment e set"
          + " lastupdated = v.lastupdated,"
          + " createdatclient = v.createdatclient,"
          + " lastupdatedatclient = v.lastupdatedatclient,"
          + " lastupdatedbyuserinfo = v.lastupdatedbyuserinfo::jsonb,"
          + " trackedentityid = v.trackedentityid,"
          + " programid = v.programid,"
          + " organisationunitid = v.organisationunitid,"
          + " attributeoptioncomboid = v.attributeoptioncomboid,"
          + " enrollmentdate = v.enrollmentdate,"
          + " occurreddate = v.occurreddate,"
          + " completeddate = v.completeddate,"
          + " completedby = v.completedby,"
          + " status = v.status,"
          + " followup = v.followup,"
          + " geometry = case when v.geometry is null then null"
          + " else ST_GeomFromText(v.geometry, "
          + SRID
          + ") end"
          + " from ( select"
          + " unnest(?::bigint[]) as enrollmentid,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo,"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as programid,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as attributeoptioncomboid,"
          + " unnest(?::timestamptz[]) as enrollmentdate,"
          + " unnest(?::timestamptz[]) as occurreddate,"
          + " unnest(?::timestamptz[]) as completeddate,"
          + " unnest(?::text[]) as completedby,"
          + " unnest(?::text[]) as status,"
          + " unnest(?::boolean[]) as followup,"
          + " unnest(?::text[]) as geometry"
          + " ) v where e.enrollmentid = v.enrollmentid";

  private final UserInfoJsonCache userInfo;
  private final NoteCascadeWriter notes = new NoteCascadeWriter("enrollment_notes", "enrollmentid");

  EnrollmentWriter(UserInfoJsonCache userInfo) {
    this.userInfo = userInfo;
  }

  @Override
  void flush(Connection conn) throws SQLException {
    insert(conn);
    update(conn);
    notes.cascade(conn, inserts, updates, Enrollment::getId, Enrollment::getNotes);
  }

  private void insert(Connection conn) throws SQLException {
    forEachChunk(
        inserts,
        chunk -> {
          for (Enrollment e : chunk) {
            requirePreallocatedId(e);
          }
          try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, Enrollment::getId));
            ps.setArray(p++, textArray(conn, chunk, Enrollment::getUid));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getCreated())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getLastUpdated())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getCreatedAtClient())));
            ps.setArray(
                p++, textArray(conn, chunk, e -> toTimestamptz(e.getLastUpdatedAtClient())));
            ps.setArray(
                p++, textArray(conn, chunk, e -> userInfo.toJson(e.getCreatedByUserInfo())));
            ps.setArray(
                p++, textArray(conn, chunk, e -> userInfo.toJson(e.getLastUpdatedByUserInfo())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getEnrollmentDate())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getOccurredDate())));
            ps.setArray(p++, textArray(conn, chunk, e -> toTimestamptz(e.getCompletedDate())));
            ps.setArray(p++, textArray(conn, chunk, Enrollment::getCompletedBy));
            ps.setArray(p++, textArray(conn, chunk, e -> e.getStatus().name()));
            ps.setArray(p++, booleanArray(conn, chunk, Enrollment::getFollowup));
            ps.setArray(p++, booleanArray(conn, chunk, Enrollment::isDeleted));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getTrackedEntity().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getProgram().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getOrganisationUnit().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, e -> e.getAttributeOptionCombo().getId()));
            ps.setArray(p++, textArray(conn, chunk, e -> geometryText(e.getGeometry())));
            ps.executeUpdate();
          }
        });
  }

  private static void requirePreallocatedId(Enrollment e) throws SQLException {
    if (e.getId() == 0) {
      throw new SQLException(
          "Enrollment "
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
          Long[] trackedEntityIds = new Long[n];
          Long[] programIds = new Long[n];
          Long[] organisationUnitIds = new Long[n];
          Long[] attributeOptionComboIds = new Long[n];
          String[] enrollmentDate = new String[n];
          String[] occurredDate = new String[n];
          String[] completedDate = new String[n];
          String[] completedBy = new String[n];
          String[] status = new String[n];
          Boolean[] followup = new Boolean[n];
          String[] geometry = new String[n];

          for (int i = 0; i < n; i++) {
            Enrollment e = chunk.get(i);
            ids[i] = e.getId();
            lastUpdated[i] = toTimestamptz(e.getLastUpdated());
            createdAtClient[i] = toTimestamptz(e.getCreatedAtClient());
            lastUpdatedAtClient[i] = toTimestamptz(e.getLastUpdatedAtClient());
            lastUpdatedByUserInfo[i] = userInfo.toJson(e.getLastUpdatedByUserInfo());
            trackedEntityIds[i] = e.getTrackedEntity().getId();
            programIds[i] = e.getProgram().getId();
            organisationUnitIds[i] = e.getOrganisationUnit().getId();
            attributeOptionComboIds[i] = e.getAttributeOptionCombo().getId();
            enrollmentDate[i] = toTimestamptz(e.getEnrollmentDate());
            occurredDate[i] = toTimestamptz(e.getOccurredDate());
            completedDate[i] = toTimestamptz(e.getCompletedDate());
            completedBy[i] = e.getCompletedBy();
            status[i] = e.getStatus().name();
            followup[i] = e.getFollowup();
            geometry[i] = e.getGeometry() != null ? e.getGeometry().toText() : null;
          }

          try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setArray(1, conn.createArrayOf("bigint", ids));
            ps.setArray(2, conn.createArrayOf("text", lastUpdated));
            ps.setArray(3, conn.createArrayOf("text", createdAtClient));
            ps.setArray(4, conn.createArrayOf("text", lastUpdatedAtClient));
            ps.setArray(5, conn.createArrayOf("text", lastUpdatedByUserInfo));
            ps.setArray(6, conn.createArrayOf("bigint", trackedEntityIds));
            ps.setArray(7, conn.createArrayOf("bigint", programIds));
            ps.setArray(8, conn.createArrayOf("bigint", organisationUnitIds));
            ps.setArray(9, conn.createArrayOf("bigint", attributeOptionComboIds));
            ps.setArray(10, conn.createArrayOf("text", enrollmentDate));
            ps.setArray(11, conn.createArrayOf("text", occurredDate));
            ps.setArray(12, conn.createArrayOf("text", completedDate));
            ps.setArray(13, conn.createArrayOf("text", completedBy));
            ps.setArray(14, conn.createArrayOf("text", status));
            ps.setArray(15, conn.createArrayOf("boolean", followup));
            ps.setArray(16, conn.createArrayOf("text", geometry));
            ps.executeUpdate();
          }
        });
  }
}
