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
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.buildMultiRowInsertSql;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.setNullableTimestamp;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toObjectStyleJson;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamp;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.truncate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
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

  private static final String INSERT_PREFIX =
      "insert into relationship ("
          + "relationshipid, uid, code, created, lastupdated, lastupdatedby,"
          + " style, relationshiptypeid,"
          + " from_relationshipitemid, to_relationshipitemid,"
          + " key, inverted_key, deleted, createdatclient) values ";

  // from/to_relationshipitemid are written as NULL on the parent INSERT and filled in by
  // updateFromTo after the items are inserted.
  private static final String INSERT_ROW =
      "(?, ?, ?, ?, ?, ?, ?::jsonb, ?, NULL, NULL, ?, ?, ?, ?)";

  private static final String ITEM_INSERT_PREFIX =
      "insert into relationshipitem ("
          + "relationshipitemid, relationshipid,"
          + " trackedentityid, enrollmentid, trackereventid, singleeventid) values ";
  private static final String ITEM_INSERT_ROW = "(?, ?, ?, ?, ?, ?)";

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
          String sql = buildMultiRowInsertSql(INSERT_PREFIX, INSERT_ROW, chunk.size());
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int p = 1;
            for (Relationship r : chunk) {
              p = bindRow(ps, p, r);
            }
            ps.executeUpdate();
          }
        });
  }

  private int bindRow(PreparedStatement ps, int p, Relationship r) throws SQLException {
    if (r.getId() == 0) {
      throw new SQLException(
          "Relationship "
              + r.getUid()
              + " has no pre-allocated id; AbstractTrackerPersister"
              + " must pre-allocate ids when sequenceName() is non-null.");
    }
    ps.setLong(p++, r.getId());
    ps.setString(p++, r.getUid());
    if (r.getCode() != null) {
      ps.setString(p++, r.getCode());
    } else {
      ps.setNull(p++, Types.VARCHAR);
    }
    ps.setTimestamp(p++, toTimestamp(r.getCreated()));
    ps.setTimestamp(p++, toTimestamp(r.getLastUpdated()));
    if (r.getLastUpdatedBy() != null) {
      ps.setLong(p++, r.getLastUpdatedBy().getId());
    } else {
      ps.setNull(p++, Types.BIGINT);
    }
    ps.setString(p++, toObjectStyleJson(r.getStyle()));
    ps.setLong(p++, r.getRelationshipType().getId());
    ps.setString(p++, r.getKey());
    ps.setString(p++, r.getInvertedKey());
    ps.setBoolean(p++, r.isDeleted());
    setNullableTimestamp(ps, p++, r.getCreatedAtClient());
    return p;
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

    forEachChunk(
        rows,
        chunk -> {
          String sql = buildMultiRowInsertSql(ITEM_INSERT_PREFIX, ITEM_INSERT_ROW, chunk.size());
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int p = 1;
            for (ItemRow row : chunk) {
              p = bindItemRow(ps, p, row.itemId(), row.relationshipId(), row.item());
            }
            ps.executeUpdate();
          }
        });
  }

  private int bindItemRow(
      PreparedStatement ps, int p, long itemId, long relationshipId, RelationshipItem item)
      throws SQLException {
    ps.setLong(p++, itemId);
    ps.setLong(p++, relationshipId);
    if (item.getTrackedEntity() != null) {
      ps.setLong(p++, item.getTrackedEntity().getId());
    } else {
      ps.setNull(p++, Types.BIGINT);
    }
    if (item.getEnrollment() != null) {
      ps.setLong(p++, item.getEnrollment().getId());
    } else {
      ps.setNull(p++, Types.BIGINT);
    }
    // For PROGRAM_STAGE_INSTANCE endpoints the mapper sets BOTH trackerEvent and singleEvent from
    // preheat (TrackerObjectsMapper.java:350-353,366-369), but only the matching one resolves to
    // non-null because a given UID exists in exactly one of the two event tables.
    if (item.getTrackerEvent() != null) {
      ps.setLong(p++, item.getTrackerEvent().getId());
    } else {
      ps.setNull(p++, Types.BIGINT);
    }
    if (item.getSingleEvent() != null) {
      ps.setLong(p++, item.getSingleEvent().getId());
    } else {
      ps.setNull(p++, Types.BIGINT);
    }
    return p;
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
