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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.note.Note;
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
 *   <li>Enrollment inserts are flushed via a multi-row JDBC INSERT against {@code enrollment} --
 *       ids are pre-allocated by {@link AbstractTrackerPersister} from {@code
 *       programinstance_sequence}.
 *   <li>Enrollment updates are flushed via a single JDBC unnest UPDATE against {@code enrollment},
 *       writing the columns mutated by {@code TrackerObjectsMapper.map} on the update branch.
 *   <li>TrackerEvent inserts are flushed via a multi-row JDBC INSERT against {@code trackerevent}
 *       -- ids are pre-allocated from {@code trackerevent_sequence}. The {@code eventdatavalues}
 *       jsonb column is serialized as a JSON object keyed by dataElement uid, matching {@code
 *       JsonEventDataValueSetBinaryType}.
 *   <li>TrackerEvent updates are flushed via a JDBC unnest UPDATE against {@code trackerevent}.
 *   <li>Any new {@code note}s on inserted or updated enrollments / tracker events are
 *       cascade-inserted in further multi-row INSERTs (into {@code note} plus the matching {@code
 *       enrollment_notes} / {@code trackerevent_notes} join), replacing Hibernate's
 *       {@code @OneToMany(cascade=ALL)} behaviour. Notes are append-only on UPDATE.
 *   <li>All remaining entity types (SingleEvent, Relationship) still delegate to {@link
 *       EntityManager}; their Phase 6 JDBC replacements follow.
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

  /** Cap on rows per multi-row INSERT/UPDATE statement, to bound peak array memory per chunk. */
  private static final int BATCH_SIZE = 128;

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

  private static final String ENROLLMENT_INSERT_PREFIX =
      "insert into enrollment ("
          + "enrollmentid, uid, created, lastupdated,"
          + " createdatclient, lastupdatedatclient,"
          + " createdbyuserinfo, lastupdatedbyuserinfo,"
          + " enrollmentdate, occurreddate, completeddate, completedby,"
          + " status, followup, deleted,"
          + " trackedentityid, programid, organisationunitid, attributeoptioncomboid,"
          + " geometry) values ";

  private static final String ENROLLMENT_INSERT_ROW =
      "(?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
          + "ST_GeomFromText(?, "
          + TRACKED_ENTITY_SRID
          + "))";

  // Columns mutated by TrackerObjectsMapper.map(Enrollment) outside the new-entity branch (see
  // lines 124-180). Insert-only columns (uid, created, createdbyuserinfo) and columns owned by
  // other code paths (deleted) are deliberately excluded. status, completeddate, completedby are
  // included even though the mapper only touches them on a status change -- writing the unchanged
  // values back is a no-op against the existing row.
  private static final String ENROLLMENT_UPDATE_SQL =
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
          + TRACKED_ENTITY_SRID
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

  private static final String TRACKER_EVENT_INSERT_PREFIX =
      "insert into trackerevent ("
          + "eventid, uid, created, lastupdated,"
          + " createdatclient, lastupdatedatclient,"
          + " createdbyuserinfo, lastupdatedbyuserinfo,"
          + " status, occurreddate, scheduleddate, completeddate, completedby,"
          + " deleted, lastsynchronized,"
          + " enrollmentid, programstageid, organisationunitid, attributeoptioncomboid,"
          + " assigneduserid, eventdatavalues, geometry) values ";

  private static final String TRACKER_EVENT_INSERT_ROW =
      "(?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, "
          + "ST_GeomFromText(?, "
          + TRACKED_ENTITY_SRID
          + "))";

  // Columns mutated by TrackerObjectsMapper.map(TrackerEvent) outside the new-entity branch
  // (see TrackerObjectsMapper.java:199-251). Insert-only columns (uid, created,
  // createdbyuserinfo, deleted) and columns owned by other code paths (lastsynchronized) are
  // excluded.
  private static final String TRACKER_EVENT_UPDATE_SQL =
      "update trackerevent ev set"
          + " lastupdated = v.lastupdated,"
          + " createdatclient = v.createdatclient,"
          + " lastupdatedatclient = v.lastupdatedatclient,"
          + " lastupdatedbyuserinfo = v.lastupdatedbyuserinfo::jsonb,"
          + " enrollmentid = v.enrollmentid,"
          + " programstageid = v.programstageid,"
          + " organisationunitid = v.organisationunitid,"
          + " attributeoptioncomboid = v.attributeoptioncomboid,"
          + " status = v.status,"
          + " occurreddate = v.occurreddate,"
          + " scheduleddate = v.scheduleddate,"
          + " completeddate = v.completeddate,"
          + " completedby = v.completedby,"
          + " assigneduserid = v.assigneduserid,"
          + " eventdatavalues = v.eventdatavalues::jsonb,"
          + " geometry = case when v.geometry is null then null"
          + " else ST_GeomFromText(v.geometry, "
          + TRACKED_ENTITY_SRID
          + ") end"
          + " from ( select"
          + " unnest(?::bigint[]) as eventid,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::timestamptz[]) as createdatclient,"
          + " unnest(?::timestamptz[]) as lastupdatedatclient,"
          + " unnest(?::text[]) as lastupdatedbyuserinfo,"
          + " unnest(?::bigint[]) as enrollmentid,"
          + " unnest(?::bigint[]) as programstageid,"
          + " unnest(?::bigint[]) as organisationunitid,"
          + " unnest(?::bigint[]) as attributeoptioncomboid,"
          + " unnest(?::text[]) as status,"
          + " unnest(?::timestamptz[]) as occurreddate,"
          + " unnest(?::timestamptz[]) as scheduleddate,"
          + " unnest(?::timestamptz[]) as completeddate,"
          + " unnest(?::text[]) as completedby,"
          + " unnest(?::bigint[]) as assigneduserid,"
          + " unnest(?::text[]) as eventdatavalues,"
          + " unnest(?::text[]) as geometry"
          + " ) v where ev.eventid = v.eventid";

  // Cascade-INSERT targets for entities that carry notes (Enrollment, TrackerEvent). See
  // Enrollment.java:192-199 and TrackerEvent.hbm.xml:65-70.
  private static final String NOTE_INSERT_PREFIX =
      "insert into note (noteid, uid, created, lastupdatedby, notetext) values ";
  private static final String NOTE_INSERT_ROW = "(?, ?, ?, ?, ?)";

  private static final String ENROLLMENT_NOTES_INSERT_PREFIX =
      "insert into enrollment_notes (enrollmentid, noteid, sort_order) values ";
  private static final String ENROLLMENT_NOTES_INSERT_ROW = "(?, ?, ?)";

  private static final String TRACKER_EVENT_NOTES_INSERT_PREFIX =
      "insert into trackerevent_notes (eventid, noteid, sort_order) values ";
  private static final String TRACKER_EVENT_NOTES_INSERT_ROW = "(?, ?, ?)";

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
   * Applies all staged writes. TrackedEntity and Enrollment writes go through JDBC on {@code conn}
   * (including cascade INSERTs into {@code note} / {@code enrollment_notes}); TrackerEvent,
   * SingleEvent, Relationship and TEAV writes still delegate to {@code entityManager}. The
   * connection must be the one bound to the current Spring-managed transaction so that the JDBC
   * statements and any subsequent Hibernate flush execute under the same commit.
   */
  void flush(EntityManager entityManager, Connection conn) throws SQLException {
    if (isEmpty()) {
      return;
    }

    insertTrackedEntities(conn);
    updateTrackedEntities(entityManager, conn);
    insertEnrollments(conn);
    updateEnrollments(entityManager, conn);
    insertEnrollmentNotes(conn);
    insertTrackerEvents(conn);
    updateTrackerEvents(conn);
    insertTrackerEventNotes(conn);
    refreshUpdatedTrackerEvents(entityManager);
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
    for (int from = 0; from < teUpdates.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, teUpdates.size());
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
    for (int from = 0; from < teInserts.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, teInserts.size());
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

  private void insertEnrollments(Connection conn) throws SQLException {
    if (enrollmentInserts.isEmpty()) {
      return;
    }
    for (int from = 0; from < enrollmentInserts.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, enrollmentInserts.size());
      List<Enrollment> chunk = enrollmentInserts.subList(from, to);
      String sql =
          buildMultiRowInsertSql(ENROLLMENT_INSERT_PREFIX, ENROLLMENT_INSERT_ROW, chunk.size());
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int p = 1;
        for (Enrollment e : chunk) {
          p = bindEnrollmentRow(ps, p, e);
        }
        ps.executeUpdate();
      }
    }
  }

  private int bindEnrollmentRow(PreparedStatement ps, int p, Enrollment e) throws SQLException {
    if (e.getId() == 0) {
      throw new SQLException(
          "Enrollment "
              + e.getUid()
              + " has no pre-allocated id; AbstractTrackerPersister"
              + " must pre-allocate ids when sequenceName() is non-null.");
    }
    ps.setLong(p++, e.getId());
    ps.setString(p++, e.getUid());
    ps.setTimestamp(p++, toTimestamp(e.getCreated()));
    ps.setTimestamp(p++, toTimestamp(e.getLastUpdated()));
    setNullableTimestamp(ps, p++, e.getCreatedAtClient());
    setNullableTimestamp(ps, p++, e.getLastUpdatedAtClient());
    ps.setString(p++, toJson(e.getCreatedByUserInfo()));
    ps.setString(p++, toJson(e.getLastUpdatedByUserInfo()));
    ps.setTimestamp(p++, toTimestamp(e.getEnrollmentDate()));
    setNullableTimestamp(ps, p++, e.getOccurredDate());
    setNullableTimestamp(ps, p++, e.getCompletedDate());
    if (e.getCompletedBy() != null) {
      ps.setString(p++, e.getCompletedBy());
    } else {
      ps.setNull(p++, Types.VARCHAR);
    }
    ps.setString(p++, e.getStatus().name());
    if (e.getFollowup() != null) {
      ps.setBoolean(p++, e.getFollowup());
    } else {
      ps.setNull(p++, Types.BOOLEAN);
    }
    ps.setBoolean(p++, e.isDeleted());
    ps.setLong(p++, e.getTrackedEntity().getId());
    ps.setLong(p++, e.getProgram().getId());
    ps.setLong(p++, e.getOrganisationUnit().getId());
    ps.setLong(p++, e.getAttributeOptionCombo().getId());
    if (e.getGeometry() != null) {
      ps.setString(p++, e.getGeometry().toText());
    } else {
      ps.setNull(p++, Types.VARCHAR);
    }
    return p;
  }

  private void updateEnrollments(EntityManager entityManager, Connection conn) throws SQLException {
    if (enrollmentUpdates.isEmpty()) {
      return;
    }
    for (int from = 0; from < enrollmentUpdates.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, enrollmentUpdates.size());
      List<Enrollment> chunk = enrollmentUpdates.subList(from, to);
      int n = chunk.size();

      Long[] ids = new Long[n];
      Timestamp[] lastUpdated = new Timestamp[n];
      Timestamp[] createdAtClient = new Timestamp[n];
      Timestamp[] lastUpdatedAtClient = new Timestamp[n];
      String[] lastUpdatedByUserInfo = new String[n];
      Long[] trackedEntityIds = new Long[n];
      Long[] programIds = new Long[n];
      Long[] organisationUnitIds = new Long[n];
      Long[] attributeOptionComboIds = new Long[n];
      Timestamp[] enrollmentDate = new Timestamp[n];
      Timestamp[] occurredDate = new Timestamp[n];
      Timestamp[] completedDate = new Timestamp[n];
      String[] completedBy = new String[n];
      String[] status = new String[n];
      Boolean[] followup = new Boolean[n];
      String[] geometry = new String[n];

      for (int i = 0; i < n; i++) {
        Enrollment e = chunk.get(i);
        ids[i] = e.getId();
        lastUpdated[i] = toTimestamp(e.getLastUpdated());
        createdAtClient[i] = toTimestamp(e.getCreatedAtClient());
        lastUpdatedAtClient[i] = toTimestamp(e.getLastUpdatedAtClient());
        lastUpdatedByUserInfo[i] = toJson(e.getLastUpdatedByUserInfo());
        trackedEntityIds[i] = e.getTrackedEntity().getId();
        programIds[i] = e.getProgram().getId();
        organisationUnitIds[i] = e.getOrganisationUnit().getId();
        attributeOptionComboIds[i] = e.getAttributeOptionCombo().getId();
        enrollmentDate[i] = toTimestamp(e.getEnrollmentDate());
        occurredDate[i] = toTimestamp(e.getOccurredDate());
        completedDate[i] = toTimestamp(e.getCompletedDate());
        completedBy[i] = e.getCompletedBy();
        status[i] = e.getStatus().name();
        followup[i] = e.getFollowup();
        geometry[i] = e.getGeometry() != null ? e.getGeometry().toText() : null;
      }

      try (PreparedStatement ps = conn.prepareStatement(ENROLLMENT_UPDATE_SQL)) {
        ps.setArray(1, conn.createArrayOf("bigint", ids));
        ps.setArray(2, conn.createArrayOf("timestamptz", lastUpdated));
        ps.setArray(3, conn.createArrayOf("timestamptz", createdAtClient));
        ps.setArray(4, conn.createArrayOf("timestamptz", lastUpdatedAtClient));
        ps.setArray(5, conn.createArrayOf("text", lastUpdatedByUserInfo));
        ps.setArray(6, conn.createArrayOf("bigint", trackedEntityIds));
        ps.setArray(7, conn.createArrayOf("bigint", programIds));
        ps.setArray(8, conn.createArrayOf("bigint", organisationUnitIds));
        ps.setArray(9, conn.createArrayOf("bigint", attributeOptionComboIds));
        ps.setArray(10, conn.createArrayOf("timestamptz", enrollmentDate));
        ps.setArray(11, conn.createArrayOf("timestamptz", occurredDate));
        ps.setArray(12, conn.createArrayOf("timestamptz", completedDate));
        ps.setArray(13, conn.createArrayOf("text", completedBy));
        ps.setArray(14, conn.createArrayOf("text", status));
        ps.setArray(15, conn.createArrayOf("boolean", followup));
        ps.setArray(16, conn.createArrayOf("text", geometry));
        ps.executeUpdate();
      }
    }
    // Same L1 staleness fix as updateTrackedEntities -- the JDBC UPDATE bypasses Hibernate, so the
    // preheat-loaded managed Enrollment in the persistence context still carries the pre-update
    // state. Detach it so any subsequent JPQL read reloads the fresh DB row.
    for (Enrollment e : enrollmentUpdates) {
      Enrollment managed = entityManager.find(Enrollment.class, e.getId());
      if (managed != null) {
        entityManager.detach(managed);
      }
    }
  }

  private void insertTrackerEvents(Connection conn) throws SQLException {
    if (trackerEventInserts.isEmpty()) {
      return;
    }
    for (int from = 0; from < trackerEventInserts.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, trackerEventInserts.size());
      List<TrackerEvent> chunk = trackerEventInserts.subList(from, to);
      String sql =
          buildMultiRowInsertSql(
              TRACKER_EVENT_INSERT_PREFIX, TRACKER_EVENT_INSERT_ROW, chunk.size());
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int p = 1;
        for (TrackerEvent e : chunk) {
          p = bindTrackerEventRow(ps, p, e);
        }
        ps.executeUpdate();
      }
    }
  }

  private int bindTrackerEventRow(PreparedStatement ps, int p, TrackerEvent e) throws SQLException {
    if (e.getId() == 0) {
      throw new SQLException(
          "TrackerEvent "
              + e.getUid()
              + " has no pre-allocated id; AbstractTrackerPersister"
              + " must pre-allocate ids when sequenceName() is non-null.");
    }
    ps.setLong(p++, e.getId());
    ps.setString(p++, e.getUid());
    ps.setTimestamp(p++, toTimestamp(e.getCreated()));
    ps.setTimestamp(p++, toTimestamp(e.getLastUpdated()));
    setNullableTimestamp(ps, p++, e.getCreatedAtClient());
    setNullableTimestamp(ps, p++, e.getLastUpdatedAtClient());
    ps.setString(p++, toJson(e.getCreatedByUserInfo()));
    ps.setString(p++, toJson(e.getLastUpdatedByUserInfo()));
    ps.setString(p++, e.getStatus().name());
    setNullableTimestamp(ps, p++, e.getOccurredDate());
    setNullableTimestamp(ps, p++, e.getScheduledDate());
    setNullableTimestamp(ps, p++, e.getCompletedDate());
    if (e.getCompletedBy() != null) {
      ps.setString(p++, e.getCompletedBy());
    } else {
      ps.setNull(p++, Types.VARCHAR);
    }
    ps.setBoolean(p++, e.isDeleted());
    setNullableTimestamp(ps, p++, e.getLastSynchronized());
    ps.setLong(p++, e.getEnrollment().getId());
    ps.setLong(p++, e.getProgramStage().getId());
    ps.setLong(p++, e.getOrganisationUnit().getId());
    ps.setLong(p++, e.getAttributeOptionCombo().getId());
    if (e.getAssignedUser() != null) {
      ps.setLong(p++, e.getAssignedUser().getId());
    } else {
      ps.setNull(p++, Types.BIGINT);
    }
    ps.setString(p++, toEventDataValuesJson(e.getEventDataValues()));
    if (e.getGeometry() != null) {
      ps.setString(p++, e.getGeometry().toText());
    } else {
      ps.setNull(p++, Types.VARCHAR);
    }
    return p;
  }

  private void updateTrackerEvents(Connection conn) throws SQLException {
    if (trackerEventUpdates.isEmpty()) {
      return;
    }
    for (int from = 0; from < trackerEventUpdates.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, trackerEventUpdates.size());
      List<TrackerEvent> chunk = trackerEventUpdates.subList(from, to);
      int n = chunk.size();

      Long[] ids = new Long[n];
      Timestamp[] lastUpdated = new Timestamp[n];
      Timestamp[] createdAtClient = new Timestamp[n];
      Timestamp[] lastUpdatedAtClient = new Timestamp[n];
      String[] lastUpdatedByUserInfo = new String[n];
      Long[] enrollmentIds = new Long[n];
      Long[] programStageIds = new Long[n];
      Long[] organisationUnitIds = new Long[n];
      Long[] attributeOptionComboIds = new Long[n];
      String[] status = new String[n];
      Timestamp[] occurredDate = new Timestamp[n];
      Timestamp[] scheduledDate = new Timestamp[n];
      Timestamp[] completedDate = new Timestamp[n];
      String[] completedBy = new String[n];
      Long[] assignedUserIds = new Long[n];
      String[] eventDataValues = new String[n];
      String[] geometry = new String[n];

      for (int i = 0; i < n; i++) {
        TrackerEvent e = chunk.get(i);
        ids[i] = e.getId();
        lastUpdated[i] = toTimestamp(e.getLastUpdated());
        createdAtClient[i] = toTimestamp(e.getCreatedAtClient());
        lastUpdatedAtClient[i] = toTimestamp(e.getLastUpdatedAtClient());
        lastUpdatedByUserInfo[i] = toJson(e.getLastUpdatedByUserInfo());
        enrollmentIds[i] = e.getEnrollment().getId();
        programStageIds[i] = e.getProgramStage().getId();
        organisationUnitIds[i] = e.getOrganisationUnit().getId();
        attributeOptionComboIds[i] = e.getAttributeOptionCombo().getId();
        status[i] = e.getStatus().name();
        occurredDate[i] = toTimestamp(e.getOccurredDate());
        scheduledDate[i] = toTimestamp(e.getScheduledDate());
        completedDate[i] = toTimestamp(e.getCompletedDate());
        completedBy[i] = e.getCompletedBy();
        assignedUserIds[i] = e.getAssignedUser() != null ? e.getAssignedUser().getId() : null;
        eventDataValues[i] = toEventDataValuesJson(e.getEventDataValues());
        geometry[i] = e.getGeometry() != null ? e.getGeometry().toText() : null;
      }

      try (PreparedStatement ps = conn.prepareStatement(TRACKER_EVENT_UPDATE_SQL)) {
        ps.setArray(1, conn.createArrayOf("bigint", ids));
        ps.setArray(2, conn.createArrayOf("timestamptz", lastUpdated));
        ps.setArray(3, conn.createArrayOf("timestamptz", createdAtClient));
        ps.setArray(4, conn.createArrayOf("timestamptz", lastUpdatedAtClient));
        ps.setArray(5, conn.createArrayOf("text", lastUpdatedByUserInfo));
        ps.setArray(6, conn.createArrayOf("bigint", enrollmentIds));
        ps.setArray(7, conn.createArrayOf("bigint", programStageIds));
        ps.setArray(8, conn.createArrayOf("bigint", organisationUnitIds));
        ps.setArray(9, conn.createArrayOf("bigint", attributeOptionComboIds));
        ps.setArray(10, conn.createArrayOf("text", status));
        ps.setArray(11, conn.createArrayOf("timestamptz", occurredDate));
        ps.setArray(12, conn.createArrayOf("timestamptz", scheduledDate));
        ps.setArray(13, conn.createArrayOf("timestamptz", completedDate));
        ps.setArray(14, conn.createArrayOf("text", completedBy));
        ps.setArray(15, conn.createArrayOf("bigint", assignedUserIds));
        ps.setArray(16, conn.createArrayOf("text", eventDataValues));
        ps.setArray(17, conn.createArrayOf("text", geometry));
        ps.executeUpdate();
      }
    }
    // L1 staleness is fixed by refreshUpdatedTrackerEvents() called from flush() AFTER
    // insertTrackerEventNotes so the refreshed entity sees the just-written notes join rows.
  }

  /**
   * Reload the L1-cached TrackerEvent entities affected by {@link #updateTrackerEvents} from DB.
   * Must run AFTER {@link #insertTrackerEventNotes} so the refreshed entity's eager-fetched notes
   * collection picks up the newly-appended notes. Using {@code em.refresh} (instead of {@code
   * em.detach}) mirrors the side effect of the pre-migration {@code em.merge(detachedDto)} path
   * that updated the L1-managed instance in place -- callers (and tests like {@code
   * TrackerEventSMSTest.shouldUpdateEvent}) that hold a reference to the entity and compare it to a
   * freshly-read row rely on the local reference seeing the new state.
   */
  private void refreshUpdatedTrackerEvents(EntityManager entityManager) {
    for (TrackerEvent e : trackerEventUpdates) {
      TrackerEvent managed = entityManager.find(TrackerEvent.class, e.getId());
      if (managed != null) {
        entityManager.refresh(managed);
      }
    }
  }

  /** Shared shape between {@code enrollment_notes} and {@code trackerevent_notes} cascade rows. */
  private interface NoteToInsert {
    Note note();
  }

  /**
   * Cascade-insert any notes attached to enrollments that were just inserted by {@link
   * #insertEnrollments} or appended to existing enrollments by {@link #updateEnrollments}. Mirrors
   * what Hibernate did via {@code @OneToMany(cascade=ALL)} on {@code Enrollment.notes} (see {@code
   * Enrollment.java:192-199}): allocate ids for the new {@code note} rows from {@code
   * note_sequence}, insert the {@code note} rows, then insert the join rows in {@code
   * enrollment_notes} with {@code sort_order} taken from the list position (1-based, per
   * {@code @ListIndexBase(1)}). On INSERT every Note is new; on UPDATE only Notes with {@code id ==
   * 0} are new (existing ones were loaded by the preheat).
   */
  private record EnrollmentNoteToInsert(long enrollmentId, Note note, int sortOrder)
      implements NoteToInsert {}

  private void insertEnrollmentNotes(Connection conn) throws SQLException {
    List<EnrollmentNoteToInsert> newNotes = collectNewEnrollmentNotes();
    if (newNotes.isEmpty()) {
      return;
    }

    long[] noteIds = allocateIds(conn, "note_sequence", newNotes.size());
    for (int i = 0; i < newNotes.size(); i++) {
      newNotes.get(i).note().setId(noteIds[i]);
    }

    insertNoteRows(conn, newNotes);
    insertEnrollmentNotesJoinRows(conn, newNotes);
  }

  /**
   * For each enrollment being inserted or updated, collect the {@link Note}s that are new (no DB
   * id) along with their 1-based sort order, which is the position in the {@code getNotes()} list.
   * For inserts every note is new; for updates the preheat-loaded list already contains existing
   * notes with non-zero ids, so we skip those.
   */
  private List<EnrollmentNoteToInsert> collectNewEnrollmentNotes() {
    List<EnrollmentNoteToInsert> newNotes = new ArrayList<>();
    collectNewNotesFrom(enrollmentInserts, newNotes);
    collectNewNotesFrom(enrollmentUpdates, newNotes);
    return newNotes;
  }

  private static void collectNewNotesFrom(
      List<Enrollment> enrollments, List<EnrollmentNoteToInsert> out) {
    for (Enrollment e : enrollments) {
      if (e.getNotes() == null) {
        continue;
      }
      List<Note> notes = e.getNotes();
      for (int i = 0; i < notes.size(); i++) {
        Note n = notes.get(i);
        if (n.getId() == 0) {
          out.add(new EnrollmentNoteToInsert(e.getId(), n, i + 1));
        }
      }
    }
  }

  private void insertNoteRows(Connection conn, List<? extends NoteToInsert> newNotes)
      throws SQLException {
    for (int from = 0; from < newNotes.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, newNotes.size());
      List<? extends NoteToInsert> chunk = newNotes.subList(from, to);
      String sql = buildMultiRowInsertSql(NOTE_INSERT_PREFIX, NOTE_INSERT_ROW, chunk.size());
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int p = 1;
        for (NoteToInsert item : chunk) {
          Note n = item.note();
          ps.setLong(p++, n.getId());
          ps.setString(p++, n.getUid());
          ps.setTimestamp(p++, toTimestamp(n.getCreated()));
          if (n.getLastUpdatedBy() != null) {
            ps.setLong(p++, n.getLastUpdatedBy().getId());
          } else {
            ps.setNull(p++, Types.BIGINT);
          }
          if (n.getNoteText() != null) {
            ps.setString(p++, n.getNoteText());
          } else {
            ps.setNull(p++, Types.VARCHAR);
          }
        }
        ps.executeUpdate();
      }
    }
  }

  private void insertEnrollmentNotesJoinRows(Connection conn, List<EnrollmentNoteToInsert> newNotes)
      throws SQLException {
    for (int from = 0; from < newNotes.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, newNotes.size());
      List<EnrollmentNoteToInsert> chunk = newNotes.subList(from, to);
      String sql =
          buildMultiRowInsertSql(
              ENROLLMENT_NOTES_INSERT_PREFIX, ENROLLMENT_NOTES_INSERT_ROW, chunk.size());
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int p = 1;
        for (EnrollmentNoteToInsert item : chunk) {
          ps.setLong(p++, item.enrollmentId());
          ps.setLong(p++, item.note().getId());
          ps.setInt(p++, item.sortOrder());
        }
        ps.executeUpdate();
      }
    }
  }

  /** Parallel of {@link EnrollmentNoteToInsert} for the {@code trackerevent_notes} join table. */
  private record TrackerEventNoteToInsert(long eventId, Note note, int sortOrder)
      implements NoteToInsert {}

  private void insertTrackerEventNotes(Connection conn) throws SQLException {
    List<TrackerEventNoteToInsert> newNotes = collectNewTrackerEventNotes();
    if (newNotes.isEmpty()) {
      return;
    }

    long[] noteIds = allocateIds(conn, "note_sequence", newNotes.size());
    for (int i = 0; i < newNotes.size(); i++) {
      newNotes.get(i).note().setId(noteIds[i]);
    }

    insertNoteRows(conn, newNotes);
    insertTrackerEventNotesJoinRows(conn, newNotes);
  }

  private List<TrackerEventNoteToInsert> collectNewTrackerEventNotes() {
    List<TrackerEventNoteToInsert> newNotes = new ArrayList<>();
    collectNewTrackerEventNotesFrom(trackerEventInserts, newNotes);
    collectNewTrackerEventNotesFrom(trackerEventUpdates, newNotes);
    return newNotes;
  }

  private static void collectNewTrackerEventNotesFrom(
      List<TrackerEvent> events, List<TrackerEventNoteToInsert> out) {
    for (TrackerEvent e : events) {
      if (e.getNotes() == null) {
        continue;
      }
      List<Note> notes = e.getNotes();
      for (int i = 0; i < notes.size(); i++) {
        Note n = notes.get(i);
        if (n.getId() == 0) {
          out.add(new TrackerEventNoteToInsert(e.getId(), n, i + 1));
        }
      }
    }
  }

  private void insertTrackerEventNotesJoinRows(
      Connection conn, List<TrackerEventNoteToInsert> newNotes) throws SQLException {
    for (int from = 0; from < newNotes.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, newNotes.size());
      List<TrackerEventNoteToInsert> chunk = newNotes.subList(from, to);
      String sql =
          buildMultiRowInsertSql(
              TRACKER_EVENT_NOTES_INSERT_PREFIX, TRACKER_EVENT_NOTES_INSERT_ROW, chunk.size());
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int p = 1;
        for (TrackerEventNoteToInsert item : chunk) {
          ps.setLong(p++, item.eventId());
          ps.setLong(p++, item.note().getId());
          ps.setInt(p++, item.sortOrder());
        }
        ps.executeUpdate();
      }
    }
  }

  /**
   * Fetches {@code count} ids from {@code sequenceName} in a single round-trip. The sequence name
   * is interpolated into the SQL (not a bind parameter) because PostgreSQL's {@code nextval} takes
   * a {@code regclass}; the value is always a static literal controlled by us. Mirrors the helper
   * in {@code AbstractTrackerPersister}.
   */
  private static long[] allocateIds(Connection conn, String sequenceName, int count)
      throws SQLException {
    long[] ids = new long[count];
    String sql = "select nextval('" + sequenceName + "') from generate_series(1, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, count);
      try (ResultSet rs = ps.executeQuery()) {
        int i = 0;
        while (rs.next()) {
          ids[i++] = rs.getLong(1);
        }
        if (i != count) {
          throw new SQLException(
              "Allocated " + i + " ids from " + sequenceName + ", expected " + count);
        }
      }
    }
    return ids;
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

  // Must mirror JsonEventDataValueSetBinaryType exactly: same MAPPER (configured with
  // IgnoreJsonPropertyWriteOnlyAccessJacksonAnnotationIntrospector, NON_NULL inclusion, etc.) and
  // same per-value writer. Using a different ObjectMapper (e.g. the Spring-injected default) drops
  // fields like UserInfoSnapshot.id that the JDBC reader requires.
  private static final ObjectWriter EVENT_DATA_VALUE_WRITER =
      JsonBinaryType.MAPPER.writerFor(EventDataValue.class);

  /**
   * Serializes the EventDataValues set as a JSON object keyed by {@code dataElement} uid, matching
   * the on-disk shape produced by {@code JsonEventDataValueSetBinaryType}. An empty or null set is
   * serialized as {@code "{}"} to match the column's NOT NULL default.
   */
  private String toEventDataValuesJson(Set<EventDataValue> values) throws SQLException {
    try {
      StringWriter sw = new StringWriter();
      try (JsonGenerator gen = JsonBinaryType.MAPPER.getFactory().createGenerator(sw)) {
        gen.writeStartObject();
        if (values != null) {
          for (EventDataValue edv : values) {
            gen.writeFieldName(edv.getDataElement());
            EVENT_DATA_VALUE_WRITER.writeValue(gen, edv);
          }
        }
        gen.writeEndObject();
      }
      return sw.toString();
    } catch (IOException e) {
      throw new SQLException("Failed to serialize EventDataValues to JSON", e);
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
