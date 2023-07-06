/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackerImporterTest {
  @InjectMocks DefaultTrackerImporter defaultTrackerImporter;

  @Mock TrackerAsyncImporter asyncImporter;

  @Mock TrackerSyncImporter syncImporter;

  @Mock ContextService contextService;

  @Test
  void shouldImportAsync() {
    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder()
            .contextService(contextService)
            .userUid("userUid")
            .uid("uid")
            .trackerBundleParams(TrackerBundleParams.builder().build())
            .isAsync(true)
            .build();

    defaultTrackerImporter.importTracker(trackerImportRequest);

    verify(asyncImporter).importTracker(any(), any(), eq("uid"));
    verify(syncImporter, times(0)).importTracker(any(), any());
  }

  @Test
  void shouldImportSync() {
    TrackerImportRequest trackerImportRequest =
        TrackerImportRequest.builder()
            .contextService(contextService)
            .userUid("userUid")
            .uid("uid")
            .trackerBundleParams(TrackerBundleParams.builder().build())
            .trackerBundleReportMode(TrackerBundleReportMode.ERRORS)
            .build();

    defaultTrackerImporter.importTracker(trackerImportRequest);

    verify(asyncImporter, times(0)).importTracker(any(), any(), any());
    verify(syncImporter).importTracker(any(), eq(TrackerBundleReportMode.ERRORS));
  }
}
