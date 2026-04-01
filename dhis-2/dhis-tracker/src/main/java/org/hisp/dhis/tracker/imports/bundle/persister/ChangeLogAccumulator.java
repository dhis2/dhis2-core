/*
 * Copyright (c) 2004-2024, University of Oslo
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
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLog;

/**
 * Collects changelog entries during the persist phase and flushes them all at the end. This allows
 * Hibernate JDBC batching to group entity INSERTs/UPDATEs separately from changelog INSERTs,
 * instead of interleaving them per entity.
 */
class ChangeLogAccumulator {
  private final List<TrackedEntityChangeLog> teChangeLogs = new ArrayList<>();
  private final List<TrackerEventChangeLog> trackerEventChangeLogs = new ArrayList<>();
  private final List<SingleEventChangeLog> singleEventChangeLogs = new ArrayList<>();

  void addTrackedEntityChangeLog(TrackedEntityChangeLog changeLog) {
    teChangeLogs.add(changeLog);
  }

  void addTrackerEventChangeLog(TrackerEventChangeLog changeLog) {
    trackerEventChangeLogs.add(changeLog);
  }

  void addSingleEventChangeLog(SingleEventChangeLog changeLog) {
    singleEventChangeLogs.add(changeLog);
  }

  Mark mark() {
    return new Mark(
        teChangeLogs.size(), trackerEventChangeLogs.size(), singleEventChangeLogs.size());
  }

  void rollbackToMark(Mark mark) {
    truncate(teChangeLogs, mark.teSize);
    truncate(trackerEventChangeLogs, mark.trackerEventSize);
    truncate(singleEventChangeLogs, mark.singleEventSize);
  }

  void flushAll(EntityManager entityManager) {
    teChangeLogs.forEach(entityManager::persist);
    trackerEventChangeLogs.forEach(entityManager::persist);
    singleEventChangeLogs.forEach(entityManager::persist);
    teChangeLogs.clear();
    trackerEventChangeLogs.clear();
    singleEventChangeLogs.clear();
  }

  private static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  record Mark(int teSize, int trackerEventSize, int singleEventSize) {}
}
