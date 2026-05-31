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

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
 * <p>Current scope: inserts and updates for TrackedEntity, Enrollment, TrackerEvent, SingleEvent;
 * inserts only for Relationship (updates are rejected upstream by {@link
 * AbstractTrackerPersister}); inserts, updates, and deletes for TEAVs. The flush implementation
 * delegates back to {@link EntityManager} so that Hibernate continues to drive persistence while
 * the staging shape is in place. Phase 6 replaces {@link #flush(EntityManager)} with a
 * connection-based implementation that emits multi-row INSERT / unnest UPDATE / tuple DELETE
 * statements.
 *
 * <p>Each {@link AbstractTrackerPersister#persist} call creates its own batch, so in practice only
 * one top-level entity type list is populated per batch (the persister's own type) alongside any
 * TEAVs staged by its attribute-handling code.
 */
class EntityWriteBatch {

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
   * Applies all staged writes through the provided {@link EntityManager}. Temporary delegating
   * implementation; Phase 6 swaps this for JDBC multi-row statements that take a {@link
   * java.sql.Connection}.
   *
   * <p>Top-level entities are flushed before TEAVs so that Hibernate assigns ids (and inserts the
   * rows, when {@code em.flush()} runs) before any TEAV that references them. The top-level order
   * (TE, Enrollment, TrackerEvent, SingleEvent, Relationship) matches the persister call order
   * enforced by {@code DefaultTrackerBundleService.commit()} and is FK-safe.
   */
  void flush(EntityManager entityManager) {
    if (isEmpty()) {
      return;
    }

    persistAll(entityManager, teInserts);
    mergeAll(entityManager, teUpdates);
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
