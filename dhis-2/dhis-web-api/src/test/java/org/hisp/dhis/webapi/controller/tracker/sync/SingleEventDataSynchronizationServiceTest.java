/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Stats;
import org.hisp.dhis.tracker.imports.report.Status;
import org.junit.jupiter.api.Test;

class SingleEventDataSynchronizationServiceTest {

  @Test
  void shouldMapOkStatusToSuccess() {
    ImportReport report = ImportReport.builder().status(Status.OK).build();

    ImportSummary summary = SingleEventDataSynchronizationService.toImportSummary(report);

    assertEquals(ImportStatus.SUCCESS, summary.getStatus());
  }

  @Test
  void shouldMapWarningStatusToWarning() {
    ImportReport report = ImportReport.builder().status(Status.WARNING).build();

    ImportSummary summary = SingleEventDataSynchronizationService.toImportSummary(report);

    assertEquals(ImportStatus.WARNING, summary.getStatus());
  }

  @Test
  void shouldMapErrorStatusToError() {
    ImportReport report = ImportReport.builder().status(Status.ERROR).build();

    ImportSummary summary = SingleEventDataSynchronizationService.toImportSummary(report);

    assertEquals(ImportStatus.ERROR, summary.getStatus());
  }

  @Test
  void shouldMapStats() {
    Stats stats = Stats.builder().created(1).updated(2).deleted(3).ignored(4).build();
    ImportReport report = ImportReport.builder().status(Status.OK).stats(stats).build();

    ImportSummary summary = SingleEventDataSynchronizationService.toImportSummary(report);

    assertEquals(1, summary.getImportCount().getImported());
    assertEquals(2, summary.getImportCount().getUpdated());
    assertEquals(3, summary.getImportCount().getDeleted());
    assertEquals(4, summary.getImportCount().getIgnored());
  }

  @Test
  void shouldLeaveImportCountZeroedWhenStatsAbsent() {
    ImportReport report = ImportReport.builder().status(Status.OK).build();

    ImportSummary summary = SingleEventDataSynchronizationService.toImportSummary(report);

    assertEquals(0, summary.getImportCount().getImported());
    assertEquals(0, summary.getImportCount().getUpdated());
    assertEquals(0, summary.getImportCount().getDeleted());
    assertEquals(0, summary.getImportCount().getIgnored());
  }
}
