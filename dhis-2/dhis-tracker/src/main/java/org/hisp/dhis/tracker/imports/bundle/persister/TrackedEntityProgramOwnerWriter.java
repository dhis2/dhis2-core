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

import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.allocateIds;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.bigintArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.textArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamptz;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.truncate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;

/**
 * Flushes staged {@link TrackedEntityProgramOwner} rows via a single batched, multi-row JDBC INSERT
 * into {@code trackedentityprogramowner}, replacing the per-new-enrollment Hibernate {@code save()}
 * in {@code EnrollmentPersister.persistOwnership} (one {@code nextval} round-trip + one single-row
 * INSERT each).
 *
 * <p>Ids are pre-allocated from {@code trackedentityprogramowner_sequence} (added in {@code
 * V2_44_14}), the dedicated sequence the entity's {@code <generator class="sequence"/>} mapping now
 * draws from. Both this JDBC import path and the remaining Hibernate insert paths for this table
 * (ownership create-or-update and transfer) share that one sequence, so they cannot collide.
 *
 * <p>Ownership is INSERT-only here; the persister guards against duplicates against the preheat
 * before staging, and the {@code (trackedentityid, programid)} unique key backstops it.
 */
final class TrackedEntityProgramOwnerWriter {

  // Constant-text INSERT ... SELECT unnest(...) so pgjdbc's prepared-statement cache engages
  // regardless of row count.
  private static final String INSERT_SQL =
      "insert into trackedentityprogramowner ("
          + "trackedentityprogramownerid, trackedentityid, programid,"
          + " created, lastupdated, organisationunitid, createdby)"
          + " select trackedentityprogramownerid, trackedentityid, programid,"
          + " created, lastupdated, organisationunitid, createdby"
          + " from ( select"
          + " unnest(?::bigint[]) as trackedentityprogramownerid,"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as programid,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::text[]) as createdby"
          + " ) v";

  private final List<TrackedEntityProgramOwner> inserts = new ArrayList<>();

  void stageInsert(TrackedEntityProgramOwner owner) {
    inserts.add(owner);
  }

  int mark() {
    return inserts.size();
  }

  void rollbackTo(int mark) {
    truncate(inserts, mark);
  }

  void clear() {
    inserts.clear();
  }

  boolean isEmpty() {
    return inserts.isEmpty();
  }

  void flush(Connection conn) throws SQLException {
    if (inserts.isEmpty()) {
      return;
    }
    long[] ids = allocateIds(conn, "trackedentityprogramowner_sequence", inserts.size());
    int cursor = 0;
    for (TrackedEntityProgramOwner owner : inserts) {
      owner.setId((int) ids[cursor++]);
    }

    forEachChunk(
        inserts,
        chunk -> {
          try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, o -> (long) o.getId()));
            ps.setArray(p++, bigintArray(conn, chunk, o -> o.getTrackedEntity().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, o -> o.getProgram().getId()));
            ps.setArray(p++, textArray(conn, chunk, o -> toTimestamptz(o.getCreated())));
            ps.setArray(p++, textArray(conn, chunk, o -> toTimestamptz(o.getLastUpdated())));
            ps.setArray(p++, bigintArray(conn, chunk, o -> o.getOrganisationUnit().getId()));
            ps.setArray(p++, textArray(conn, chunk, TrackedEntityProgramOwner::getCreatedBy));
            ps.executeUpdate();
          }
        });
  }
}
