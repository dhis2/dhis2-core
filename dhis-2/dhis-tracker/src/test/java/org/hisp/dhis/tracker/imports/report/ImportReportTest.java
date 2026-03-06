/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.report;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.test.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class ImportReportTest {

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @Test
  void testImportCalculatesIgnoredValues() {
    // Create Bundle report for te and enrollment
    PersistenceReport persistenceReport =
        new PersistenceReport(
            createTypeReport(TrackerType.TRACKED_ENTITY, 5, 3, 0),
            createTypeReport(TrackerType.ENROLLMENT, 3, 3, 0),
            createTypeReport(TrackerType.EVENT, 0, 0, 0),
            createTypeReport(TrackerType.EVENT, 0, 0, 0),
            createTypeReport(TrackerType.RELATIONSHIP, 0, 0, 0));
    // Create validation report with 3 objects
    ValidationReport validationReport = ValidationReport.emptyReport();
    validationReport.addErrors(rnd.objects(Error.class, 3).toList());
    // Create payload map
    Map<TrackerType, Integer> originalPayload = new HashMap<>();
    originalPayload.put(TrackerType.TRACKED_ENTITY, 10);
    originalPayload.put(TrackerType.ENROLLMENT, 8);
    // Method under test
    ImportReport rep =
        ImportReport.withImportCompleted(
            Status.OK, persistenceReport, validationReport, originalPayload);
    assertThat(rep.getStats().getCreated(), is(8));
    assertThat(rep.getStats().getUpdated(), is(6));
    assertThat(rep.getStats().getIgnored(), is(3));
    assertThat(rep.getStats().getDeleted(), is(0));
    assertThat(getBundleReportStats(rep, TrackerType.TRACKED_ENTITY).getCreated(), is(5));
    assertThat(getBundleReportStats(rep, TrackerType.TRACKED_ENTITY).getUpdated(), is(3));
    assertThat(getBundleReportStats(rep, TrackerType.TRACKED_ENTITY).getDeleted(), is(0));
    assertThat(getBundleReportStats(rep, TrackerType.TRACKED_ENTITY).getIgnored(), is(2));
    assertThat(getBundleReportStats(rep, TrackerType.ENROLLMENT).getCreated(), is(3));
    assertThat(getBundleReportStats(rep, TrackerType.ENROLLMENT).getUpdated(), is(3));
    assertThat(getBundleReportStats(rep, TrackerType.ENROLLMENT).getDeleted(), is(0));
    assertThat(getBundleReportStats(rep, TrackerType.ENROLLMENT).getIgnored(), is(2));
  }

  private Stats getBundleReportStats(ImportReport importReport, TrackerType type) {
    return importReport.getPersistenceReport().getTypeReportMap().get(type).getStats();
  }

  private TrackerTypeReport createTypeReport(
      TrackerType type, int created, int updated, int deleted) {
    final Stats teStats = new Stats();
    teStats.setCreated(created);
    teStats.setUpdated(updated);
    teStats.setDeleted(deleted);
    return new TrackerTypeReport(type, teStats, new ArrayList<>(), new ArrayList<>());
  }
}
