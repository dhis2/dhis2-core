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
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.booleanArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.textArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toObjectStyleJson;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamptz;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.truncate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.RelationshipItem;

/**
 * Flushes staged {@link Relationship} inserts via three JDBC statements -- a multi-row INSERT into
 * {@code relationship} with {@code from/to_relationshipitemid} left NULL, a multi-row INSERT into
 * {@code relationshipitem} for the (from + to) items, and an unnest UPDATE on {@code relationship}
 * to set the from/to FKs. The three-step shape breaks the circular, non-deferrable FK between the
 * two tables. Ids come from {@code relationship_sequence} and {@code relationshipitem_sequence}.
 *
 * <p>Relationship is INSERT-only in tracker imports ({@code RelationshipPersister.convert} returns
 * null on UPDATE strategy), so there is no update list and no L1 detach/refresh.
 */
final class RelationshipWriter {

  // Constant-text INSERT ... SELECT unnest(...) so pgjdbc's prepared-statement cache engages
  // regardless of row count. from/to_relationshipitemid are written as NULL on this parent INSERT
  // and filled in by updateFromTo after the items are inserted.
  private static final String INSERT_SQL =
      "insert into relationship ("
          + "relationshipid, uid, code, created, lastupdated, lastupdatedby,"
          + " style, relationshiptypeid, from_relationshipitemid, to_relationshipitemid,"
          + " key, inverted_key, deleted, createdatclient)"
          + " select relationshipid, uid, code, created, lastupdated, lastupdatedby,"
          + " style::jsonb, relationshiptypeid, null, null,"
          + " key, inverted_key, deleted, createdatclient"
          + " from ( select"
          + " unnest(?::bigint[]) as relationshipid,"
          + " unnest(?::text[]) as uid,"
          + " unnest(?::text[]) as code,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::bigint[]) as lastupdatedby,"
          + " unnest(?::text[]) as style,"
          + " unnest(?::bigint[]) as relationshiptypeid,"
          + " unnest(?::text[]) as key,"
          + " unnest(?::text[]) as inverted_key,"
          + " unnest(?::boolean[]) as deleted,"
          + " unnest(?::timestamptz[]) as createdatclient"
          + " ) v";

  private static final String ITEM_INSERT_SQL =
      "insert into relationshipitem ("
          + "relationshipitemid, relationshipid,"
          + " trackedentityid, enrollmentid, trackereventid, singleeventid)"
          + " select relationshipitemid, relationshipid,"
          + " trackedentityid, enrollmentid, trackereventid, singleeventid"
          + " from ( select"
          + " unnest(?::bigint[]) as relationshipitemid,"
          + " unnest(?::bigint[]) as relationshipid,"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as enrollmentid,"
          + " unnest(?::bigint[]) as trackereventid,"
          + " unnest(?::bigint[]) as singleeventid"
          + " ) v";

  private static final String UPDATE_FROM_TO_SQL =
      "update relationship r set"
          + " from_relationshipitemid = v.from_id,"
          + " to_relationshipitemid = v.to_id"
          + " from ( select"
          + " unnest(?::bigint[]) as relationshipid,"
          + " unnest(?::bigint[]) as from_id,"
          + " unnest(?::bigint[]) as to_id"
          + " ) v where r.relationshipid = v.relationshipid";

  private final List<Relationship> inserts = new ArrayList<>();

  /**
   * Relationships are never updated -- {@link AbstractTrackerPersister} ignores update payloads.
   */
  void stageInsert(Relationship relationship) {
    inserts.add(relationship);
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
    insertRelationships(conn);
    insertRelationshipItems(conn);
    updateFromTo(conn);
  }

  /**
   * Inserts the parent {@code relationship} rows with from/to FKs left NULL (filled in by {@link
   * #updateFromTo} once the item rows exist, since neither FK constraint is deferrable).
   */
  private void insertRelationships(Connection conn) throws SQLException {
    forEachChunk(
        inserts,
        chunk -> {
          for (Relationship r : chunk) {
            requirePreallocatedId(r);
          }
          try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, Relationship::getId));
            ps.setArray(p++, textArray(conn, chunk, Relationship::getUid));
            ps.setArray(p++, textArray(conn, chunk, Relationship::getCode));
            ps.setArray(p++, textArray(conn, chunk, r -> toTimestamptz(r.getCreated())));
            ps.setArray(p++, textArray(conn, chunk, r -> toTimestamptz(r.getLastUpdated())));
            ps.setArray(p++, bigintArray(conn, chunk, RelationshipWriter::lastUpdatedById));
            ps.setArray(p++, textArray(conn, chunk, r -> toObjectStyleJson(r.getStyle())));
            ps.setArray(p++, bigintArray(conn, chunk, r -> r.getRelationshipType().getId()));
            ps.setArray(p++, textArray(conn, chunk, Relationship::getKey));
            ps.setArray(p++, textArray(conn, chunk, Relationship::getInvertedKey));
            ps.setArray(p++, booleanArray(conn, chunk, Relationship::isDeleted));
            ps.setArray(p++, textArray(conn, chunk, r -> toTimestamptz(r.getCreatedAtClient())));
            ps.executeUpdate();
          }
        });
  }

  private static Long lastUpdatedById(Relationship r) {
    return r.getLastUpdatedBy() != null ? r.getLastUpdatedBy().getId() : null;
  }

  private static void requirePreallocatedId(Relationship r) throws SQLException {
    if (r.getId() == 0) {
      throw new SQLException(
          "Relationship "
              + r.getUid()
              + " has no pre-allocated id; AbstractTrackerPersister"
              + " must pre-allocate ids when sequenceName() is non-null.");
    }
  }

  /** Flattened from/to item with its pre-allocated id and parent relationship id. */
  private record ItemRow(long itemId, long relationshipId, RelationshipItem item) {}

  /**
   * Inserts the {@code relationshipitem} rows. Each Relationship has exactly two items (from + to);
   * we allocate {@code 2 * relationships.size()} ids from {@code relationshipitem_sequence} in one
   * round-trip, assign them to the in-memory items (so {@link #updateFromTo} can read them back),
   * then flatten the from/to items into a single multi-row INSERT.
   */
  private void insertRelationshipItems(Connection conn) throws SQLException {
    if (inserts.isEmpty()) {
      return;
    }
    long[] itemIds = allocateIds(conn, "relationshipitem_sequence", 2 * inserts.size());
    int cursor = 0;
    for (Relationship r : inserts) {
      r.getFrom().setId((int) itemIds[cursor++]);
      r.getTo().setId((int) itemIds[cursor++]);
    }

    List<ItemRow> rows = new ArrayList<>(2 * inserts.size());
    for (Relationship r : inserts) {
      rows.add(new ItemRow(r.getFrom().getId(), r.getId(), r.getFrom()));
      rows.add(new ItemRow(r.getTo().getId(), r.getId(), r.getTo()));
    }

    // For PROGRAM_STAGE_INSTANCE endpoints the mapper sets BOTH trackerEvent and singleEvent from
    // preheat (TrackerObjectsMapper.java:350-353,366-369), but only the matching one resolves to
    // non-null because a given UID exists in exactly one of the two event tables.
    forEachChunk(
        rows,
        chunk -> {
          try (PreparedStatement ps = conn.prepareStatement(ITEM_INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, ItemRow::itemId));
            ps.setArray(p++, bigintArray(conn, chunk, ItemRow::relationshipId));
            ps.setArray(
                p++, bigintArray(conn, chunk, row -> itemRefId(row.item().getTrackedEntity())));
            ps.setArray(
                p++, bigintArray(conn, chunk, row -> itemRefId(row.item().getEnrollment())));
            ps.setArray(
                p++, bigintArray(conn, chunk, row -> itemRefId(row.item().getTrackerEvent())));
            ps.setArray(
                p++, bigintArray(conn, chunk, row -> itemRefId(row.item().getSingleEvent())));
            ps.executeUpdate();
          }
        });
  }

  private static Long itemRefId(org.hisp.dhis.common.IdentifiableObject ref) {
    return ref != null ? ref.getId() : null;
  }

  /**
   * Sets {@code relationship.from_relationshipitemid} and {@code to_relationshipitemid} once the
   * item rows have been inserted by {@link #insertRelationshipItems}.
   */
  private void updateFromTo(Connection conn) throws SQLException {
    forEachChunk(
        inserts,
        chunk -> {
          int n = chunk.size();

          Long[] ids = new Long[n];
          Long[] fromIds = new Long[n];
          Long[] toIds = new Long[n];

          for (int i = 0; i < n; i++) {
            Relationship r = chunk.get(i);
            ids[i] = r.getId();
            fromIds[i] = (long) r.getFrom().getId();
            toIds[i] = (long) r.getTo().getId();
          }

          try (PreparedStatement ps = conn.prepareStatement(UPDATE_FROM_TO_SQL)) {
            ps.setArray(1, conn.createArrayOf("bigint", ids));
            ps.setArray(2, conn.createArrayOf("bigint", fromIds));
            ps.setArray(3, conn.createArrayOf("bigint", toIds));
            ps.executeUpdate();
          }
        });
  }
}
