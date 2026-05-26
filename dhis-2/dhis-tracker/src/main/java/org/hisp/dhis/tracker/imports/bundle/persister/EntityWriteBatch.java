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
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;

/**
 * Accumulates entity-level writes staged during the persist loop and applies them at a single flush
 * point. Mirrors {@link ChangeLogAccumulator} and shares the same mark/rollback contract for
 * per-entity error isolation in non-atomic mode.
 *
 * <p>Phase 3 scope: TEAV inserts/updates/deletes only. The flush implementation currently delegates
 * back to {@link EntityManager} so that Hibernate continues to drive persistence while the staging
 * shape is in place. Phase 6 replaces {@link #flush(EntityManager)} with a connection-based
 * implementation that emits multi-row INSERT / unnest UPDATE / tuple DELETE statements. Top-level
 * entities (TrackedEntity, Enrollment, events, relationships) are added by Phase 4.
 */
class EntityWriteBatch {

  private final List<TrackedEntityAttributeValue> teavInserts = new ArrayList<>();
  private final List<TrackedEntityAttributeValue> teavUpdates = new ArrayList<>();
  private final List<TrackedEntityAttributeValue> teavDeletes = new ArrayList<>();

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
    return new Mark(teavInserts.size(), teavUpdates.size(), teavDeletes.size());
  }

  void rollbackTo(Mark mark) {
    truncate(teavInserts, mark.teavInsertSize);
    truncate(teavUpdates, mark.teavUpdateSize);
    truncate(teavDeletes, mark.teavDeleteSize);
  }

  /**
   * Applies all staged writes through the provided {@link EntityManager}. Temporary delegating
   * implementation; Phase 6 swaps this for JDBC multi-row statements that take a {@link
   * java.sql.Connection}.
   */
  void flush(EntityManager entityManager) {
    if (teavInserts.isEmpty() && teavUpdates.isEmpty() && teavDeletes.isEmpty()) {
      return;
    }

    for (TrackedEntityAttributeValue v : teavInserts) {
      entityManager.persist(v);
    }
    for (TrackedEntityAttributeValue v : teavUpdates) {
      entityManager.merge(v);
    }
    for (TrackedEntityAttributeValue v : teavDeletes) {
      entityManager.remove(entityManager.contains(v) ? v : entityManager.merge(v));
    }

    teavInserts.clear();
    teavUpdates.clear();
    teavDeletes.clear();
  }

  private static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  record Mark(int teavInsertSize, int teavUpdateSize, int teavDeleteSize) {}
}
