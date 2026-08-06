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
package org.hisp.dhis.webapi.controller.tracker.sync;

import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.alreadyDeletedOrSucceededUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.blockingFailedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.failedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.successfullyProcessedUids;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.junit.jupiter.api.Test;

class TrackerSyncReportUtilsTest {

  private static final UID SUCCEEDED = UID.of("Uid1234567A");
  private static final UID ALREADY_DELETED = UID.of("Uid1234567B");
  private static final UID GENUINELY_FAILED = UID.of("Uid1234567C");

  @Test
  void shouldReturnOnlyEntitiesWithoutErrorsAsSuccessfullyProcessed() {
    ImportReport report = deleteReportFor(TrackerType.RELATIONSHIP);

    Set<UID> synced = successfullyProcessedUids(report, TrackerType.RELATIONSHIP);

    assertEquals(Set.of(SUCCEEDED), synced);
  }

  @Test
  void shouldReturnAllEntitiesWithErrorsAsFailed() {
    ImportReport report = deleteReportFor(TrackerType.RELATIONSHIP);

    Set<UID> failed = failedUids(report, TrackerType.RELATIONSHIP);

    assertEquals(Set.of(ALREADY_DELETED, GENUINELY_FAILED), failed);
  }

  @Test
  void shouldExcludeAlreadyDeletedFromBlockingFailedUids() {
    ImportReport report = deleteReportFor(TrackerType.RELATIONSHIP);

    Set<UID> blocking = blockingFailedUids(report, TrackerType.RELATIONSHIP);

    assertEquals(Set.of(GENUINELY_FAILED), blocking);
  }

  @Test
  void shouldTreatAlreadyDeletedAsSyncedButNotGenuineFailure() {
    ImportReport report = deleteReportFor(TrackerType.RELATIONSHIP);

    Set<UID> synced = alreadyDeletedOrSucceededUids(report, TrackerType.RELATIONSHIP);

    assertEquals(Set.of(SUCCEEDED, ALREADY_DELETED), synced);
  }

  /**
   * A DELETE report for {@code type} with one entity that succeeded, one whose only error is
   * "already deleted" (E4017), and one that genuinely failed for an unrelated reason.
   */
  private ImportReport deleteReportFor(TrackerType type) {
    Entity succeeded = new Entity(type, SUCCEEDED);

    Entity alreadyDeleted = new Entity(type, ALREADY_DELETED);
    alreadyDeleted
        .getErrorReports()
        .add(
            new Error(
                "Relationship already deleted", "E4017", type.name(), ALREADY_DELETED, List.of()));

    Entity genuinelyFailed = new Entity(type, GENUINELY_FAILED);
    genuinelyFailed
        .getErrorReports()
        .add(new Error("Some other error", "E1000", type.name(), GENUINELY_FAILED, List.of()));

    TrackerTypeReport typeReport = new TrackerTypeReport(type);
    typeReport.setEntityReport(
        new ArrayList<>(List.of(succeeded, alreadyDeleted, genuinelyFailed)));

    TrackerTypeReport empty = new TrackerTypeReport(type);
    PersistenceReport persistenceReport =
        type == TrackerType.TRACKED_ENTITY
            ? new PersistenceReport(typeReport, empty, empty, empty, empty)
            : type == TrackerType.ENROLLMENT
                ? new PersistenceReport(empty, typeReport, empty, empty, empty)
                : type == TrackerType.EVENT
                    ? new PersistenceReport(empty, empty, typeReport, empty, empty)
                    : new PersistenceReport(empty, empty, empty, empty, typeReport);

    return ImportReport.builder().status(Status.ERROR).persistenceReport(persistenceReport).build();
  }
}
