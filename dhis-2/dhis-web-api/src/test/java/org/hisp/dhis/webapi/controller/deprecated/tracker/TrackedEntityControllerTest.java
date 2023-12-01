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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.deprecated.tracker.imports.impl.TrackedEntityInstanceAsyncStrategyImpl;
import org.hisp.dhis.webapi.controller.deprecated.tracker.imports.impl.TrackedEntityInstanceStrategyImpl;
import org.hisp.dhis.webapi.controller.deprecated.tracker.imports.impl.TrackedEntityInstanceSyncStrategyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class TrackedEntityControllerTest {

  private MockMvc mockMvc;

  @Mock private CurrentUserService currentUserService;

  @Mock private TrackedEntityInstanceAsyncStrategyImpl trackedEntityInstanceAsyncStrategy;

  @Mock private TrackedEntityInstanceSyncStrategyImpl trackedEntityInstanceSyncStrategy;

  @Mock private User user;

  @Mock private TrackedEntityService instanceService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private TrackedEntity trackedEntity;

  private static final String ENDPOINT = TrackedEntityInstanceController.RESOURCE_PATH;

  @BeforeEach
  public void setUp() throws BadRequestException, IOException {
    final TrackedEntityInstanceController controller =
        new TrackedEntityInstanceController(
            mock(TrackedEntityInstanceService.class),
            instanceService,
            null,
            null,
            null,
            currentUserService,
            null,
            trackerAccessManager,
            null,
            null,
            new TrackedEntityInstanceStrategyImpl(
                trackedEntityInstanceSyncStrategy, trackedEntityInstanceAsyncStrategy));

    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(user.getUid()).thenReturn("userId");
  }

  @Test
  void shouldCallSyncStrategy() throws Exception {

    when(trackedEntityInstanceSyncStrategy.mergeOrDeleteTrackedEntityInstances(any()))
        .thenReturn(new ImportSummaries());

    mockMvc
        .perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk())
        .andReturn();

    verify(trackedEntityInstanceSyncStrategy, times(1)).mergeOrDeleteTrackedEntityInstances(any());
    verify(trackedEntityInstanceAsyncStrategy, times(0)).mergeOrDeleteTrackedEntityInstances(any());
  }

  @Test
  void shouldCallAsyncStrategy() throws Exception {
    mockMvc
        .perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .param("async", "true")
                .content("{}"))
        .andExpect(status().isOk())
        .andReturn();

    verify(trackedEntityInstanceSyncStrategy, times(0)).mergeOrDeleteTrackedEntityInstances(any());
    verify(trackedEntityInstanceAsyncStrategy, times(1)).mergeOrDeleteTrackedEntityInstances(any());
  }
}
