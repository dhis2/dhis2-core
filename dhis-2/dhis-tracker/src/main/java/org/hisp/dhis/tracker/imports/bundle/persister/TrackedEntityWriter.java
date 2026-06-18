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
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.buildMultiRowInsertSql;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toJson;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamp;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamptz;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.hisp.dhis.tracker.model.TrackedEntity;

/**
 * Flushes staged {@link TrackedEntity} writes: a multi-row JDBC INSERT against {@code
 * trackedentity} (ids pre-allocated from {@code trackedentity_sequence}) and a single unnest UPDATE
 * writing only the columns mutated by {@code TrackerObjectsMapper.map} on the update branch.
 */
final class TrackedEntityWriter extends UpsertTableWriter<TrackedEntity> {

  private static final String INSERT_PREFIX =
      "insert into trackedentity ("
          + "trackedentityid, uid, created, lastupdated,"
          + " createdatclient, lastupdatedatclient,"
          + " inactive, deleted, lastsynchronized, potentialduplicate,"
          + " organisationunitid, trackedentitytypeid,"
          + " geometry, createdbyuserinfo, lastupdatedbyuserinfo) values ";

  private static final String INSERT_ROW =
      "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ST_GeomFromText(?, " + SRID + "), ?::jsonb, ?::jsonb)";

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

  private final ObjectMapper objectMapper;

  TrackedEntityWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
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
          String sql = buildMultiRowInsertSql(INSERT_PREFIX, INSERT_ROW, chunk.size());
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int p = 1;
            for (TrackedEntity te : chunk) {
              p = bindRow(ps, p, te);
            }
            ps.executeUpdate();
          }
        });
  }

  private int bindRow(PreparedStatement ps, int p, TrackedEntity te) throws SQLException {
    if (te.getId() == 0) {
      throw new SQLException(
          "TrackedEntity "
              + te.getUid()
              + " has no pre-allocated id; AbstractTrackerPersister"
              + " must pre-allocate ids when sequenceName() is non-null.");
    }
    ps.setLong(p++, te.getId());
    ps.setString(p++, te.getUid());
    ps.setTimestamp(p++, toTimestamp(te.getCreated()));
    ps.setTimestamp(p++, toTimestamp(te.getLastUpdated()));
    JdbcBatchSupport.setNullableTimestamp(ps, p++, te.getCreatedAtClient());
    JdbcBatchSupport.setNullableTimestamp(ps, p++, te.getLastUpdatedAtClient());
    ps.setBoolean(p++, te.isInactive());
    ps.setBoolean(p++, te.isDeleted());
    ps.setTimestamp(p++, toTimestamp(te.getLastSynchronized()));
    ps.setBoolean(p++, te.isPotentialDuplicate());
    ps.setLong(p++, te.getOrganisationUnit().getId());
    ps.setLong(p++, te.getTrackedEntityType().getId());
    if (te.getGeometry() != null) {
      ps.setString(p++, te.getGeometry().toText());
    } else {
      ps.setNull(p++, Types.VARCHAR);
    }
    ps.setString(p++, toJson(objectMapper, te.getCreatedByUserInfo()));
    ps.setString(p++, toJson(objectMapper, te.getLastUpdatedByUserInfo()));
    return p;
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
            lastUpdatedByUserInfo[i] = toJson(objectMapper, te.getLastUpdatedByUserInfo());
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
