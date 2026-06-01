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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackerEvent;

/**
 * Accumulates entity-level writes staged during the persist loop and applies them at a single flush
 * point. Mirrors {@link ChangeLogAccumulator} and shares the same mark/rollback contract for
 * per-entity error isolation in non-atomic mode.
 *
 * <p>Scope:
 *
 * <ul>
 *   <li>TrackedEntity inserts are flushed via a multi-row JDBC INSERT against {@code trackedentity}
 *       -- ids are pre-allocated by {@link AbstractTrackerPersister} from {@code
 *       trackedentityinstance_sequence} in one round-trip.
 *   <li>TrackedEntity updates are flushed via a single JDBC unnest UPDATE against {@code
 *       trackedentity}, writing only the columns mutated by {@code TrackerObjectsMapper.map} on the
 *       update branch.
 *   <li>All other entity types (Enrollment, TrackerEvent, SingleEvent, Relationship) still delegate
 *       to {@link EntityManager}; their Phase 6 JDBC replacements follow.
 *   <li>TEAVs continue to delegate to {@link EntityManager} until their Phase 6 turn.
 * </ul>
 *
 * <p>Top-level entities are flushed before TEAVs so that ids are set (and rows present in the DB or
 * in the Hibernate persistence context) before any TEAV that references them. Within top-level
 * entities the order (TE inserts, TE updates, Enrollment, TrackerEvent, SingleEvent, Relationship)
 * matches the persister call order enforced by {@code DefaultTrackerBundleService.commit()} and is
 * FK-safe.
 *
 * <p>Each {@link AbstractTrackerPersister#persist} call creates its own batch, so in practice only
 * one top-level entity type list is populated per batch (the persister's own type) alongside any
 * TEAVs staged by its attribute-handling code.
 */
class EntityWriteBatch {

  /** Cap on rows per multi-row INSERT statement, to keep query text manageable. */
  private static final int INSERT_BATCH_SIZE = 128;

  /** Cap on rows per unnest UPDATE statement, to bound peak array memory per chunk. */
  private static final int UPDATE_BATCH_SIZE = 128;

  private static final int TRACKED_ENTITY_SRID = 4326;

  private static final String TRACKED_ENTITY_INSERT_PREFIX =
      "insert into trackedentity ("
          + "trackedentityid, uid, created, lastupdated,"
          + " createdatclient, lastupdatedatclient,"
          + " inactive, deleted, lastsynchronized, potentialduplicate,"
          + " organisationunitid, trackedentitytypeid,"
          + " geometry, createdbyuserinfo, lastupdatedbyuserinfo) values ";

  private static final String TRACKED_ENTITY_INSERT_ROW =
      "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ST_GeomFromText(?, "
          + TRACKED_ENTITY_SRID
          + "), ?::jsonb, ?::jsonb)";

  // Columns mutated by TrackerObjectsMapper.map() on the UPDATE branch (see lines 90-104). Insert-
  // only columns (uid, created, createdbyuserinfo) and columns owned by other code paths
  // (deleted, lastsynchronized -- the latter is bulk-updated by
  // DefaultTrackerBundleService.postCommit) are deliberately excluded.
  private static final String TRACKED_ENTITY_UPDATE_SQL =
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
          + TRACKED_ENTITY_SRID
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

  private final List<TrackedEntity> teInserts = new ArrayList<>();
  private final List<TrackedEntity> teUpdates = new ArrayList<>();

  private final List<Enrollment> enrollmentInserts = new ArrayList<>();
  private final List<Enrollment> enrollmentUpdates = new ArrayList<>();

  private final List<TrackerEvent> trackerEventInserts = new ArrayList<>();
  private final List<TrackerEvent> trackerEventUpdates = new ArrayList<>();

  private final List<SingleEvent> singleEventInserts = new ArrayList<>();
  private final List<SingleEvent> singleEventUpdates = new ArrayList<>();

  private final List<Relationship> relationshipInserts = new ArrayList<>();

  private final List<TrackedEntityAttributeValue> teavInserts = new ArrayList<>();
  private final List<TrackedEntityAttributeValue> teavUpdates = new ArrayList<>();
  private final List<TrackedEntityAttributeValue> teavDeletes = new ArrayList<>();

  EntityWriteBatch(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  void stageInsert(TrackedEntity trackedEntity) {
    teInserts.add(trackedEntity);
  }

  void stageUpdate(TrackedEntity trackedEntity) {
    teUpdates.add(trackedEntity);
  }

  void stageInsert(Enrollment enrollment) {
    enrollmentInserts.add(enrollment);
  }

  void stageUpdate(Enrollment enrollment) {
    enrollmentUpdates.add(enrollment);
  }

  void stageInsert(TrackerEvent event) {
    trackerEventInserts.add(event);
  }

  void stageUpdate(TrackerEvent event) {
    trackerEventUpdates.add(event);
  }

  void stageInsert(SingleEvent event) {
    singleEventInserts.add(event);
  }

  void stageUpdate(SingleEvent event) {
    singleEventUpdates.add(event);
  }

  /**
   * Relationships are never updated -- {@link AbstractTrackerPersister} ignores update payloads.
   */
  void stageInsert(Relationship relationship) {
    relationshipInserts.add(relationship);
  }

  void stageTeavInsert(TrackedEntityAttributeValue value) {
    teavInserts.add(value);
  }

  void stageTeavUpdate(TrackedEntityAttributeValue value) {
    teavUpdates.add(value);
  }

  void stageTeavDelete(TrackedEntityAttributeValue value) {
    teavDeletes.add(value);
  }

  /**
   * TEAVs already staged for insert or update against the given TrackedEntity. Used by the
   * attribute-handling code to detect when the same logical TEAV (composite key {@code te + attr})
   * is being processed twice within one persister run -- e.g. two enrollments under the same TE
   * each carrying the same attribute value -- so it can fold the second occurrence into the first
   * staged instance instead of producing a duplicate {@code em.persist} that would throw {@code
   * EntityExistsException}.
   */
  Stream<TrackedEntityAttributeValue> stagedFor(TrackedEntity trackedEntity) {
    return Stream.concat(teavInserts.stream(), teavUpdates.stream())
        .filter(v -> v.getTrackedEntity() == trackedEntity);
  }

  /**
   * Whether the given TrackedEntity is being inserted in this batch (no DB row yet). Used by the
   * attribute-handling code to skip the "load existing TEAVs" JPQL query, which would always return
   * empty for a fresh insert.
   */
  boolean isStagedAsInsert(TrackedEntity trackedEntity) {
    return teInserts.contains(trackedEntity);
  }

  Mark mark() {
    return new Mark(
        new Mark.EntityCounts(teInserts.size(), teUpdates.size()),
        new Mark.EntityCounts(enrollmentInserts.size(), enrollmentUpdates.size()),
        new Mark.EntityCounts(trackerEventInserts.size(), trackerEventUpdates.size()),
        new Mark.EntityCounts(singleEventInserts.size(), singleEventUpdates.size()),
        relationshipInserts.size(),
        new Mark.TeavCounts(teavInserts.size(), teavUpdates.size(), teavDeletes.size()));
  }

  void rollbackTo(Mark mark) {
    truncate(teInserts, mark.te().inserts());
    truncate(teUpdates, mark.te().updates());
    truncate(enrollmentInserts, mark.enrollment().inserts());
    truncate(enrollmentUpdates, mark.enrollment().updates());
    truncate(trackerEventInserts, mark.trackerEvent().inserts());
    truncate(trackerEventUpdates, mark.trackerEvent().updates());
    truncate(singleEventInserts, mark.singleEvent().inserts());
    truncate(singleEventUpdates, mark.singleEvent().updates());
    truncate(relationshipInserts, mark.relationshipInserts());
    truncate(teavInserts, mark.teav().inserts());
    truncate(teavUpdates, mark.teav().updates());
    truncate(teavDeletes, mark.teav().deletes());
  }

  /**
   * Applies all staged writes. TrackedEntity inserts and updates go through JDBC on {@code conn};
   * everything else still delegates to {@code entityManager}. The connection must be the one bound
   * to the current Spring-managed transaction so that the JDBC statements and any subsequent
   * Hibernate flush execute under the same commit.
   */
  void flush(EntityManager entityManager, Connection conn) throws SQLException {
    if (isEmpty()) {
      return;
    }

    insertTrackedEntities(conn);
    updateTrackedEntities(entityManager, conn);
    persistAll(entityManager, enrollmentInserts);
    mergeAll(entityManager, enrollmentUpdates);
    persistAll(entityManager, trackerEventInserts);
    mergeAll(entityManager, trackerEventUpdates);
    persistAll(entityManager, singleEventInserts);
    mergeAll(entityManager, singleEventUpdates);
    persistAll(entityManager, relationshipInserts);

    for (TrackedEntityAttributeValue v : teavInserts) {
      entityManager.persist(v);
    }
    for (TrackedEntityAttributeValue v : teavUpdates) {
      entityManager.merge(v);
    }
    for (TrackedEntityAttributeValue v : teavDeletes) {
      entityManager.remove(entityManager.contains(v) ? v : entityManager.merge(v));
    }

    teInserts.clear();
    teUpdates.clear();
    enrollmentInserts.clear();
    enrollmentUpdates.clear();
    trackerEventInserts.clear();
    trackerEventUpdates.clear();
    singleEventInserts.clear();
    singleEventUpdates.clear();
    relationshipInserts.clear();
    teavInserts.clear();
    teavUpdates.clear();
    teavDeletes.clear();
  }

  private void updateTrackedEntities(EntityManager entityManager, Connection conn)
      throws SQLException {
    if (teUpdates.isEmpty()) {
      return;
    }
    for (int from = 0; from < teUpdates.size(); from += UPDATE_BATCH_SIZE) {
      int to = Math.min(from + UPDATE_BATCH_SIZE, teUpdates.size());
      List<TrackedEntity> chunk = teUpdates.subList(from, to);
      int n = chunk.size();

      Long[] ids = new Long[n];
      Timestamp[] lastUpdated = new Timestamp[n];
      Timestamp[] createdAtClient = new Timestamp[n];
      Timestamp[] lastUpdatedAtClient = new Timestamp[n];
      Long[] organisationUnitIds = new Long[n];
      Long[] trackedEntityTypeIds = new Long[n];
      Boolean[] potentialDuplicate = new Boolean[n];
      Boolean[] inactive = new Boolean[n];
      String[] lastUpdatedByUserInfo = new String[n];
      String[] geometry = new String[n];

      for (int i = 0; i < n; i++) {
        TrackedEntity te = chunk.get(i);
        ids[i] = te.getId();
        lastUpdated[i] = toTimestamp(te.getLastUpdated());
        createdAtClient[i] = toTimestamp(te.getCreatedAtClient());
        lastUpdatedAtClient[i] = toTimestamp(te.getLastUpdatedAtClient());
        organisationUnitIds[i] = te.getOrganisationUnit().getId();
        trackedEntityTypeIds[i] = te.getTrackedEntityType().getId();
        potentialDuplicate[i] = te.isPotentialDuplicate();
        inactive[i] = te.isInactive();
        lastUpdatedByUserInfo[i] = toJson(te.getLastUpdatedByUserInfo());
        geometry[i] = te.getGeometry() != null ? te.getGeometry().toText() : null;
      }

      try (PreparedStatement ps = conn.prepareStatement(TRACKED_ENTITY_UPDATE_SQL)) {
        ps.setArray(1, conn.createArrayOf("bigint", ids));
        ps.setArray(2, conn.createArrayOf("timestamptz", lastUpdated));
        ps.setArray(3, conn.createArrayOf("timestamptz", createdAtClient));
        ps.setArray(4, conn.createArrayOf("timestamptz", lastUpdatedAtClient));
        ps.setArray(5, conn.createArrayOf("bigint", organisationUnitIds));
        ps.setArray(6, conn.createArrayOf("bigint", trackedEntityTypeIds));
        ps.setArray(7, conn.createArrayOf("boolean", potentialDuplicate));
        ps.setArray(8, conn.createArrayOf("boolean", inactive));
        ps.setArray(9, conn.createArrayOf("text", lastUpdatedByUserInfo));
        ps.setArray(10, conn.createArrayOf("text", geometry));
        ps.executeUpdate();
      }
    }
    // The TrackedEntity entities passed in here are detached copies from the preheat. The
    // Hibernate persistence context separately holds the entities loaded by the preheat's queries
    // (M_managed), and they still carry their pre-update state. Detach them so any subsequent
    // JPQL read in this session reloads the fresh DB row instead of returning the stale L1
    // instance.
    for (TrackedEntity te : teUpdates) {
      TrackedEntity managed = entityManager.find(TrackedEntity.class, te.getId());
      if (managed != null) {
        entityManager.detach(managed);
      }
    }
  }

  private void insertTrackedEntities(Connection conn) throws SQLException {
    if (teInserts.isEmpty()) {
      return;
    }
    for (int from = 0; from < teInserts.size(); from += INSERT_BATCH_SIZE) {
      int to = Math.min(from + INSERT_BATCH_SIZE, teInserts.size());
      List<TrackedEntity> chunk = teInserts.subList(from, to);
      String sql =
          buildMultiRowInsertSql(
              TRACKED_ENTITY_INSERT_PREFIX, TRACKED_ENTITY_INSERT_ROW, chunk.size());
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int p = 1;
        for (TrackedEntity te : chunk) {
          p = bindTrackedEntityRow(ps, p, te);
        }
        ps.executeUpdate();
      }
    }
  }

  private int bindTrackedEntityRow(PreparedStatement ps, int p, TrackedEntity te)
      throws SQLException {
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
    setNullableTimestamp(ps, p++, te.getCreatedAtClient());
    setNullableTimestamp(ps, p++, te.getLastUpdatedAtClient());
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
    ps.setString(p++, toJson(te.getCreatedByUserInfo()));
    ps.setString(p++, toJson(te.getLastUpdatedByUserInfo()));
    return p;
  }

  private static String buildMultiRowInsertSql(
      String prefix, String rowPlaceholders, int rowCount) {
    StringBuilder sb =
        new StringBuilder(prefix.length() + rowPlaceholders.length() * rowCount + 16);
    sb.append(prefix);
    for (int i = 0; i < rowCount; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(rowPlaceholders);
    }
    return sb.toString();
  }

  private static Timestamp toTimestamp(Date date) {
    return date != null ? new Timestamp(date.getTime()) : null;
  }

  private static void setNullableTimestamp(PreparedStatement ps, int index, Date date)
      throws SQLException {
    if (date != null) {
      ps.setTimestamp(index, new Timestamp(date.getTime()));
    } else {
      ps.setNull(index, Types.TIMESTAMP);
    }
  }

  private String toJson(UserInfoSnapshot info) throws SQLException {
    if (info == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(info);
    } catch (JsonProcessingException e) {
      throw new SQLException("Failed to serialize UserInfoSnapshot to JSON", e);
    }
  }

  private boolean isEmpty() {
    return teInserts.isEmpty()
        && teUpdates.isEmpty()
        && enrollmentInserts.isEmpty()
        && enrollmentUpdates.isEmpty()
        && trackerEventInserts.isEmpty()
        && trackerEventUpdates.isEmpty()
        && singleEventInserts.isEmpty()
        && singleEventUpdates.isEmpty()
        && relationshipInserts.isEmpty()
        && teavInserts.isEmpty()
        && teavUpdates.isEmpty()
        && teavDeletes.isEmpty();
  }

  private static <T> void persistAll(EntityManager entityManager, List<T> entities) {
    for (T entity : entities) {
      entityManager.persist(entity);
    }
  }

  private static <T> void mergeAll(EntityManager entityManager, List<T> entities) {
    for (T entity : entities) {
      entityManager.merge(entity);
    }
  }

  private static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  record Mark(
      EntityCounts te,
      EntityCounts enrollment,
      EntityCounts trackerEvent,
      EntityCounts singleEvent,
      int relationshipInserts,
      TeavCounts teav) {

    record EntityCounts(int inserts, int updates) {}

    record TeavCounts(int inserts, int updates, int deletes) {}
  }
}
