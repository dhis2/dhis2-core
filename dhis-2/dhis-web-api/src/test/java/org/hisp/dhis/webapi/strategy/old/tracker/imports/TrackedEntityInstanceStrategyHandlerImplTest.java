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
package org.hisp.dhis.webapi.strategy.old.tracker.imports;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.trackedentity.ImportTrackedEntitiesTask;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceAsyncStrategyImpl;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.impl.TrackedEntityInstanceSyncStrategyImpl;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.request.TrackerEntityInstanceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class TrackedEntityInstanceStrategyHandlerImplTest {
  @InjectMocks private TrackedEntityInstanceAsyncStrategyImpl trackedEntityInstanceAsyncStrategy;

  @InjectMocks private TrackedEntityInstanceSyncStrategyImpl trackedEntityInstanceSyncStrategy;

  @Mock private TrackedEntityInstanceService trackedEntityInstanceService;

  @Mock private AsyncTaskExecutor taskExecutor;

  @Mock private TrackedEntityInstance trackedEntityInstance;

  @Mock private InputStream inputStream;

  @Mock private ImportOptions importOptions;

  @Mock private JobConfiguration jobConfiguration;

  @Captor private ArgumentCaptor<ImportTrackedEntitiesTask> trackedEntitiesTaskArgumentCaptor;

  private static List<TrackedEntityInstance> trackedEntityInstanceList;

  @BeforeEach
  public void setUp() {
    trackedEntityInstanceList = Collections.singletonList(trackedEntityInstance);
  }

  @Test
  void shouldCallSyncTrackedEntityJsonSyncStrategy() throws IOException, BadRequestException {
    when(trackedEntityInstanceService.getTrackedEntityInstancesJson(any()))
        .thenReturn(trackedEntityInstanceList);

    TrackerEntityInstanceRequest trackerEntityInstanceRequest =
        TrackerEntityInstanceRequest.builder()
            .mediaType(MediaType.APPLICATION_JSON_VALUE)
            .importOptions(importOptions)
            .inputStream(inputStream)
            .build();

    trackedEntityInstanceSyncStrategy.mergeOrDeleteTrackedEntityInstances(
        trackerEntityInstanceRequest);

    verify(trackedEntityInstanceService, times(1))
        .mergeOrDeleteTrackedEntityInstances(trackedEntityInstanceList, importOptions, null);
    verify(trackedEntityInstanceService, times(1)).getTrackedEntityInstancesJson(inputStream);
  }

  @Test
  void shouldCallSyncTrackedEntityXmlSyncStrategy() throws BadRequestException, IOException {
    when(trackedEntityInstanceService.getTrackedEntityInstancesXml(any()))
        .thenReturn(trackedEntityInstanceList);

    TrackerEntityInstanceRequest trackerEntityInstanceRequest =
        TrackerEntityInstanceRequest.builder()
            .mediaType(MediaType.APPLICATION_XML_VALUE)
            .importOptions(importOptions)
            .inputStream(inputStream)
            .build();

    trackedEntityInstanceSyncStrategy.mergeOrDeleteTrackedEntityInstances(
        trackerEntityInstanceRequest);

    verify(trackedEntityInstanceService, times(1))
        .mergeOrDeleteTrackedEntityInstances(trackedEntityInstanceList, importOptions, null);
    verify(trackedEntityInstanceService, times(1)).getTrackedEntityInstancesXml(inputStream);
  }

  @Test
  void shouldCallAsyncTrackedEntityJsonAsyncStrategy() throws BadRequestException, IOException {
    when(trackedEntityInstanceService.getTrackedEntityInstancesJson(any()))
        .thenReturn(trackedEntityInstanceList);

    TrackerEntityInstanceRequest trackerEntityInstanceRequest =
        TrackerEntityInstanceRequest.builder()
            .mediaType(MediaType.APPLICATION_JSON_VALUE)
            .importOptions(importOptions)
            .jobConfiguration(jobConfiguration)
            .inputStream(inputStream)
            .build();

    trackedEntityInstanceAsyncStrategy.mergeOrDeleteTrackedEntityInstances(
        trackerEntityInstanceRequest);

    verify(trackedEntityInstanceService, times(1)).getTrackedEntityInstancesJson(inputStream);
    verify(taskExecutor, times(1)).executeTask(trackedEntitiesTaskArgumentCaptor.capture());
  }

  @Test
  void shouldCallAsyncTrackedEntityXmlAsyncStrategy() throws IOException, BadRequestException {
    when(trackedEntityInstanceService.getTrackedEntityInstancesJson(any()))
        .thenReturn(trackedEntityInstanceList);

    TrackerEntityInstanceRequest trackerEntityInstanceRequest =
        TrackerEntityInstanceRequest.builder()
            .mediaType(MediaType.APPLICATION_XML_VALUE)
            .importOptions(importOptions)
            .jobConfiguration(jobConfiguration)
            .inputStream(inputStream)
            .build();

    trackedEntityInstanceAsyncStrategy.mergeOrDeleteTrackedEntityInstances(
        trackerEntityInstanceRequest);

    verify(trackedEntityInstanceService, times(1)).getTrackedEntityInstancesXml(inputStream);
    verify(taskExecutor, times(1)).executeTask(trackedEntitiesTaskArgumentCaptor.capture());
  }

  @Test
  void shouldThrowMediaTypeNotAllowed() throws IOException {
    when(trackedEntityInstanceService.getTrackedEntityInstancesJson(any()))
        .thenReturn(trackedEntityInstanceList);

    TrackerEntityInstanceRequest trackerEntityInstanceRequest =
        TrackerEntityInstanceRequest.builder()
            .mediaType(MediaType.APPLICATION_PDF_VALUE)
            .importOptions(importOptions)
            .jobConfiguration(jobConfiguration)
            .inputStream(inputStream)
            .build();

    assertThrows(
        BadRequestException.class,
        () ->
            trackedEntityInstanceAsyncStrategy.mergeOrDeleteTrackedEntityInstances(
                trackerEntityInstanceRequest));
  }
}
