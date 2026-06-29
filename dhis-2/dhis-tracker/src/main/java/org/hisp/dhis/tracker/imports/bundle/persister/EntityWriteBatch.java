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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
import org.hisp.dhis.tracker.model.TrackerEvent;

/**
 * Accumulates entity-level writes staged during the persist loop and applies them at a single flush
 * point. Mirrors {@link ChangeLogAccumulator} and shares the same mark/rollback contract for
 * per-entity error isolation in non-atomic mode.
 *
 * <p>This class is a thin composite: the per-table SQL, binding and flush mechanics live in the
 * dedicated {@code *Writer} classes ({@link TrackedEntityWriter}, {@link EnrollmentWriter}, {@link
 * TrackerEventWriter}, {@link SingleEventWriter}, {@link RelationshipWriter}, {@link TeavWriter}),
 * with shared helpers in {@link JdbcBatchSupport} and the notes cascade in {@link
 * NoteCascadeWriter}. This class owns only the staging delegation, the composite mark/rollback and
 * the FK-safe flush order. Scope of each writer:
 *
 * <ul>
 *   <li>TrackedEntity -- multi-row INSERT + unnest UPDATE on {@code trackedentity} ({@code
 *       trackedentity_sequence}).
 *   <li>Enrollment -- multi-row INSERT + unnest UPDATE on {@code enrollment} ({@code
 *       enrollment_sequence}) plus the notes cascade.
 *   <li>TrackerEvent / SingleEvent -- multi-row INSERT + unnest UPDATE on {@code trackerevent} /
 *       {@code singleevent} ({@code eventdatavalues} jsonb keyed by dataElement uid) plus notes.
 *   <li>Relationship -- INSERT-only three-step (parent INSERT with NULL from/to, item INSERT,
 *       unnest UPDATE to set the from/to FKs) breaking the circular, non-deferrable FK.
 *   <li>TrackedEntityAttributeValue -- multi-row INSERT, unnest UPDATE and unnest DELETE on the
 *       composite ({@code trackedentityid}, {@code trackedentityattributeid}) PK.
 * </ul>
 *
 * <p>All reads and writes in the import path go through JDBC, bypassing the Hibernate persistence
 * context. Two consequences:
 *
 * <ul>
 *   <li>L1 cache: the import itself never reads these entities back through Hibernate after the
 *       writes, so nothing reconciles the persistence context at flush time. This is safe for
 *       transaction-scoped sessions, but it is NOT a guarantee that the session is empty: preheat
 *       strategies leave managed originals in the persistence context (DetachUtils maps detached
 *       copies, it does not evict), so code holding a session open across the import transaction
 *       (e.g. an SMS listener whose class-level {@code @Transactional} spans preheat, commit and
 *       post-import reads) would observe stale pre-import state through that session.
 *   <li>Auditing: the entity types written here are {@code @Auditable} (TEAV at scope TRACKER), and
 *       JDBC writes skip Hibernate's Post{Insert,Update,Delete}AuditListener, so no audit events
 *       are emitted for them. Deliberate trade-off: TRACKER-scope auditing is off by default, and
 *       attribute-value history -- the sensitive payload -- is covered by the {@code
 *       trackedentitychangelog} rows written by {@link ChangeLogAccumulator}.
 * </ul>
 *
 * <p>Top-level entities are flushed before TEAVs so that ids are set and rows are present in the DB
 * before any TEAV that references them. Within top-level entities the order (TrackedEntity,
 * Enrollment, TrackerEvent, SingleEvent, Relationship) matches the persister call order enforced by
 * {@code DefaultTrackerBundleService.commit()} and is FK-safe.
 *
 * <p>Each {@link AbstractTrackerPersister#persist} call creates its own batch, so in practice only
 * one top-level entity type list is populated per batch (the persister's own type) alongside any
 * TEAVs staged by its attribute-handling code.
 */
class EntityWriteBatch {

  private final TrackedEntityWriter trackedEntityWriter;
  private final EnrollmentWriter enrollmentWriter;
  private final TrackerEventWriter trackerEventWriter;
  private final SingleEventWriter singleEventWriter;
  private final RelationshipWriter relationshipWriter = new RelationshipWriter();
  private final TrackedEntityProgramOwnerWriter ownershipWriter =
      new TrackedEntityProgramOwnerWriter();
  private final TeavWriter teavWriter = new TeavWriter();

  EntityWriteBatch(ObjectMapper objectMapper) {
    UserInfoJsonCache userInfo = new UserInfoJsonCache(objectMapper);
    this.trackedEntityWriter = new TrackedEntityWriter(userInfo);
    this.enrollmentWriter = new EnrollmentWriter(userInfo);
    this.trackerEventWriter = new TrackerEventWriter(userInfo);
    this.singleEventWriter = new SingleEventWriter(userInfo);
  }

  void stageInsert(TrackedEntity trackedEntity) {
    trackedEntityWriter.stageInsert(trackedEntity);
  }

  void stageUpdate(TrackedEntity trackedEntity) {
    trackedEntityWriter.stageUpdate(trackedEntity);
  }

  void stageInsert(Enrollment enrollment) {
    enrollmentWriter.stageInsert(enrollment);
  }

  void stageUpdate(Enrollment enrollment) {
    enrollmentWriter.stageUpdate(enrollment);
  }

  void stageInsert(TrackerEvent event) {
    trackerEventWriter.stageInsert(event);
  }

  void stageUpdate(TrackerEvent event) {
    trackerEventWriter.stageUpdate(event);
  }

  void stageInsert(SingleEvent event) {
    singleEventWriter.stageInsert(event);
  }

  void stageUpdate(SingleEvent event) {
    singleEventWriter.stageUpdate(event);
  }

  /**
   * Relationships are never updated -- {@link AbstractTrackerPersister} ignores update payloads.
   */
  void stageInsert(Relationship relationship) {
    relationshipWriter.stageInsert(relationship);
  }

  /**
   * Stages a program-ownership row for a newly created enrollment. INSERT-only; {@link
   * EnrollmentPersister} guards against duplicates before staging.
   */
  void stageOwnershipInsert(TrackedEntityProgramOwner owner) {
    ownershipWriter.stageInsert(owner);
  }

  void stageTeavInsert(TrackedEntityAttributeValue value) {
    teavWriter.stageInsert(value);
  }

  void stageTeavUpdate(TrackedEntityAttributeValue value) {
    teavWriter.stageUpdate(value);
  }

  void stageTeavDelete(TrackedEntityAttributeValue value) {
    teavWriter.stageDelete(value);
  }

  Mark mark() {
    return new Mark(
        trackedEntityWriter.mark(),
        enrollmentWriter.mark(),
        trackerEventWriter.mark(),
        singleEventWriter.mark(),
        relationshipWriter.mark(),
        ownershipWriter.mark(),
        teavWriter.mark());
  }

  void rollbackTo(Mark mark) {
    trackedEntityWriter.rollbackTo(mark.te());
    enrollmentWriter.rollbackTo(mark.enrollment());
    trackerEventWriter.rollbackTo(mark.trackerEvent());
    singleEventWriter.rollbackTo(mark.singleEvent());
    relationshipWriter.rollbackTo(mark.relationshipInserts());
    ownershipWriter.rollbackTo(mark.ownershipInserts());
    teavWriter.rollbackTo(mark.teav());
  }

  /**
   * Applies all staged writes via JDBC on {@code conn} (including cascade INSERTs into {@code note}
   * and the per-entity notes join tables). The connection must be the one bound to the current
   * Spring-managed transaction so the JDBC statements execute under the same commit. The writes
   * bypass the Hibernate persistence context and audit listeners -- see the class javadoc for the
   * scope of that trade-off.
   *
   * <p>Writers run in FK-safe order: TrackedEntity -> Enrollment -> program ownership ->
   * TrackerEvent -> SingleEvent -> Relationship -> TEAV. Each writer applies its own inserts before
   * updates before notes. Program ownership is flushed after Enrollment (it references the tracked
   * entity, program and org unit, all already present).
   */
  void flush(Connection conn) throws SQLException {
    if (isEmpty()) {
      return;
    }

    trackedEntityWriter.flush(conn);
    enrollmentWriter.flush(conn);
    ownershipWriter.flush(conn);
    trackerEventWriter.flush(conn);
    singleEventWriter.flush(conn);
    relationshipWriter.flush(conn);
    teavWriter.flush(conn);

    trackedEntityWriter.clear();
    enrollmentWriter.clear();
    ownershipWriter.clear();
    trackerEventWriter.clear();
    singleEventWriter.clear();
    relationshipWriter.clear();
    teavWriter.clear();
  }

  private boolean isEmpty() {
    return trackedEntityWriter.isEmpty()
        && enrollmentWriter.isEmpty()
        && ownershipWriter.isEmpty()
        && trackerEventWriter.isEmpty()
        && singleEventWriter.isEmpty()
        && relationshipWriter.isEmpty()
        && teavWriter.isEmpty();
  }

  record Mark(
      UpsertTableWriter.Mark te,
      UpsertTableWriter.Mark enrollment,
      UpsertTableWriter.Mark trackerEvent,
      UpsertTableWriter.Mark singleEvent,
      int relationshipInserts,
      int ownershipInserts,
      TeavWriter.Mark teav) {}
}
