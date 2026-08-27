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
import org.hisp.dhis.tracker.model.TrackedEntity;

/**
 * Flushes staged {@link TrackedEntity} writes: a multi-row JDBC INSERT against {@code
 * trackedentity} (ids pre-allocated from {@code trackedentity_sequence}) and a single unnest UPDATE
 * writing only the columns mutated by {@code TrackerObjectsMapper.map} on the update branch.
 */
final class TrackedEntityWriter extends UpsertTableWriter<TrackedEntity> {

  // Constant-text INSERT ... SELECT unnest(...): one cacheable statement regardless of row count,
  // so pgjdbc's server-side prepared-statement cache engages (a multi-row VALUES list has distinct
  // text per row count). INSERT column order, the SELECT projection and the unnest aliases below
  // are kept in lockstep.
  private static final String INSERT_SQL =
      "insert into trackedentity ("
          + "trackedentityid, uid, created, lastupdated, createdatclient, lastupdatedatclient,"
          + " inactive, deleted, lastsynchronized, potentialduplicate,"
          + " organisationunitid, trackedentitytypeid, geometry, createdbyuserinfo,"
          + " lastupdatedbyuserinfo)"
          + " select trackedentityid, uid, created, lastupdated, createdatclient,"
          + " lastupdatedatclient, inactive, deleted, lastsynchronized, potentialduplicate,"
          + " organisationunitid, trackedentitytypeid,"
          + " case when geometry is null then null else ST_GeomFromText(geometry, "
          + SRID
          + ") end,"
          + " createdbyuserinfo::jsonb, lastupdatedbyuserinfo::jsonb"
          + " from ( select"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::text[]) as uid,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::boolean[]) as inactive,"
          + " unnest(?::boolean[]) as deleted,"
          + " unnest(?::timestamptz[]) as lastsynchronized,"
          + " unnest(?::boolean[]) as potentialduplicate,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as trackedentitytypeid,"
          + " unnest(?::text[]) as geometry,"
          + " unnest(?::text[]) as createdbyuserinfo,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo"
          + " ) v";

  // Columns mutated by TrackerObjectsMapper.map() on the UPDATE branch. Insert-only columns (uid,
  // created, createdbyuserinfo) and columns owned by other code paths (deleted, lastsynchronized --
  // the latter is bulk-updated by DefaultTrackerBundleService.postCommit) are deliberately
  // excluded.
  private static final String UPDATE_SQL =
      "update trackedentity te set"
          + " lastupdated = v.lastupdated,"
          + " createdatclient = v.createdatclient,"
          + " lastupdatedatclient = v.lastupdatedatclient,"
          + " organisationunitid = v.organisationunitid,"
          + " trackedentitytypeid = v.trackedentitytypeid,"
          + " potentialduplicate = v.potentialduplicate,"
          + " inactive = v.inactive,"
          + " lastupdatedbyuserinfo = v.lastupdatedbyuserinfo::jsonb,"
          + " geometry = case when v.geometry is null then null"
          + " else ST_GeomFromText(v.geometry, "
          + SRID
          + ") end"
          + " from ( select"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as trackedentitytypeid,"
          + " unnest(?::boolean[]) as potentialduplicate,"
          + " unnest(?::boolean[]) as inactive,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo,"
          + " unnest(?::text[]) as geometry"
          + " ) v where te.trackedentityid = v.trackedentityid";

  private final UserInfoJsonCache userInfo;

  TrackedEntityWriter(UserInfoJsonCache userInfo) {
    this.userInfo = userInfo;
  }

  @Override
  void flush(Connection conn) throws SQLException {
    insert(conn);
    update(conn);
  }

  private void insert(Connection conn) throws SQLException {
    forEachChunk(
        inserts,
        chunk -> {
          for (TrackedEntity te : chunk) {
            requirePreallocatedId(te);
          }
          try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, TrackedEntity::getId));
            ps.setArray(p++, textArray(conn, chunk, TrackedEntity::getUid));
            ps.setArray(p++, textArray(conn, chunk, te -> toTimestamptz(te.getCreated())));
            ps.setArray(p++, textArray(conn, chunk, te -> toTimestamptz(te.getLastUpdated())));
            ps.setArray(p++, textArray(conn, chunk, te -> toTimestamptz(te.getCreatedAtClient())));
            ps.setArray(
                p++, textArray(conn, chunk, te -> toTimestamptz(te.getLastUpdatedAtClient())));
            ps.setArray(p++, booleanArray(conn, chunk, TrackedEntity::isInactive));
            ps.setArray(p++, booleanArray(conn, chunk, TrackedEntity::isDeleted));
            ps.setArray(p++, textArray(conn, chunk, te -> toTimestamptz(te.getLastSynchronized())));
            ps.setArray(p++, booleanArray(conn, chunk, TrackedEntity::isPotentialDuplicate));
            ps.setArray(p++, bigintArray(conn, chunk, te -> te.getOrganisationUnit().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, te -> te.getTrackedEntityType().getId()));
            ps.setArray(p++, textArray(conn, chunk, te -> geometryText(te.getGeometry())));
            ps.setArray(
                p++, textArray(conn, chunk, te -> userInfo.toJson(te.getCreatedByUserInfo())));
            ps.setArray(
                p++, textArray(conn, chunk, te -> userInfo.toJson(te.getLastUpdatedByUserInfo())));
            ps.executeUpdate();
          }
        });
  }

  private static void requirePreallocatedId(TrackedEntity te) throws SQLException {
    if (te.getId() == 0) {
      throw new SQLException(
          "TrackedEntity "
              + te.getUid()
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
          Long[] organisationUnitIds = new Long[n];
          Long[] trackedEntityTypeIds = new Long[n];
          Boolean[] potentialDuplicate = new Boolean[n];
          Boolean[] inactive = new Boolean[n];
          String[] lastUpdatedByUserInfo = new String[n];
          String[] geometry = new String[n];

          for (int i = 0; i < n; i++) {
            TrackedEntity te = chunk.get(i);
            ids[i] = te.getId();
            lastUpdated[i] = toTimestamptz(te.getLastUpdated());
            createdAtClient[i] = toTimestamptz(te.getCreatedAtClient());
            lastUpdatedAtClient[i] = toTimestamptz(te.getLastUpdatedAtClient());
            organisationUnitIds[i] = te.getOrganisationUnit().getId();
            trackedEntityTypeIds[i] = te.getTrackedEntityType().getId();
            potentialDuplicate[i] = te.isPotentialDuplicate();
            inactive[i] = te.isInactive();
            lastUpdatedByUserInfo[i] = userInfo.toJson(te.getLastUpdatedByUserInfo());
            geometry[i] = te.getGeometry() != null ? te.getGeometry().toText() : null;
          }

          try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setArray(1, conn.createArrayOf("bigint", ids));
            ps.setArray(2, conn.createArrayOf("text", lastUpdated));
            ps.setArray(3, conn.createArrayOf("text", createdAtClient));
            ps.setArray(4, conn.createArrayOf("text", lastUpdatedAtClient));
            ps.setArray(5, conn.createArrayOf("bigint", organisationUnitIds));
            ps.setArray(6, conn.createArrayOf("bigint", trackedEntityTypeIds));
            ps.setArray(7, conn.createArrayOf("boolean", potentialDuplicate));
            ps.setArray(8, conn.createArrayOf("boolean", inactive));
            ps.setArray(9, conn.createArrayOf("text", lastUpdatedByUserInfo));
            ps.setArray(10, conn.createArrayOf("text", geometry));
            ps.executeUpdate();
          }
        });
  }
}
