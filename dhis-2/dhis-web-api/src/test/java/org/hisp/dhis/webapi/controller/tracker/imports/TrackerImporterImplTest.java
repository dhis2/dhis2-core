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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.job.TrackerMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackerImporterImplTest {
  @InjectMocks TrackerAsyncImporter asyncImporter;

  @InjectMocks TrackerSyncImporter syncImporter;

  @Mock TrackerImportService trackerImportService;

  @Mock MessageManager messageManager;

  @Test
  void shouldCreateReportSync() {
    TrackerImportParams params =
        TrackerImportParams.builder()
            .jobConfiguration(new JobConfiguration("", JobType.TRACKER_IMPORT_JOB, "userId", false))
            .build();

    syncImporter.importTracker(params, TrackerBundleReportMode.FULL);

    verify(trackerImportService).importTracker(params);
    verify(trackerImportService).buildImportReport(any(), eq(TrackerBundleReportMode.FULL));
  }

  @Test
  void shouldSendMessageToQueueAsync() {
    ArgumentCaptor<String> queueNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<TrackerMessage> trackerMessageCaptor =
        ArgumentCaptor.forClass(TrackerMessage.class);

    doNothing()
        .when(messageManager)
        .sendQueue(queueNameCaptor.capture(), trackerMessageCaptor.capture());

    TrackerImportParams params =
        TrackerImportParams.builder()
            .jobConfiguration(new JobConfiguration("", JobType.TRACKER_IMPORT_JOB, "userId", true))
            .build();

    asyncImporter.importTracker(params, null, "");

    verify(trackerImportService, times(0)).importTracker(any());
    verify(messageManager).sendQueue(any(), any());
    assertEquals(Topics.TRACKER_IMPORT_JOB_TOPIC_NAME, queueNameCaptor.getValue());
    assertEquals(params, trackerMessageCaptor.getValue().getTrackerImportParams());
  }
}
