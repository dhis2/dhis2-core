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
package org.hisp.dhis.webapi.controller.deduplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.Collections;
import org.hisp.dhis.deduplication.DeduplicationMergeParams;
import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.MergeObject;
import org.hisp.dhis.deduplication.MergeStrategy;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.deduplication.PotentialDuplicateForbiddenException;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DeduplicationControllerMvcTest {

  private static final String ENDPOINT = "/" + "potentialDuplicates";

  private MockMvc mockMvc;

  @Mock private DeduplicationService deduplicationService;

  @Mock private TrackedEntityInstanceService trackedEntityInstanceService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private CurrentUserService currentUserService;

  @Mock private FieldFilterService fieldFilterService;

  @Mock private ContextService contextService;

  @Mock private TrackedEntityInstance trackedEntityInstanceA;

  @Mock private TrackedEntityInstance trackedEntityInstanceB;

  @InjectMocks private DeduplicationController deduplicationController;

  private DeduplicationMergeParams deduplicationMergeParams;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String teiA = "trackedentA";

  private static final String teiB = "trackedentB";

  @BeforeEach
  void setUp() {
    deduplicationMergeParams =
        DeduplicationMergeParams.builder()
            .potentialDuplicate(new PotentialDuplicate(teiA, teiB))
            .original(trackedEntityInstanceA)
            .duplicate(trackedEntityInstanceB)
            .mergeObject(MergeObject.builder().build())
            .build();
    mockMvc =
        MockMvcBuilders.standaloneSetup(deduplicationController)
            .setControllerAdvice(new CrudControllerAdvice())
            .build();
    lenient()
        .when(trackedEntityInstanceService.getTrackedEntityInstance(teiA))
        .thenReturn(trackedEntityInstanceA);
    lenient()
        .when(trackedEntityInstanceService.getTrackedEntityInstance(teiB))
        .thenReturn(trackedEntityInstanceB);
    lenient()
        .when(trackerAccessManager.canRead(any(), any(TrackedEntityInstance.class)))
        .thenReturn(Lists.newArrayList());
  }

  @Test
  void shouldPostPotentialDuplicate() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    mockMvc
        .perform(
            post(ENDPOINT)
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void shouldThrowPostPotentialDuplicateMissingTei() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, null);
    mockMvc
        .perform(
            post(ENDPOINT)
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof BadRequestException));
  }

  @Test
  void shouldThrowUpdatePotentialDuplicateAlreadyMerged() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    potentialDuplicate.setStatus(DeduplicationStatus.MERGED);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    mockMvc
        .perform(
            put(ENDPOINT + "/" + uid)
                .param("status", DeduplicationStatus.INVALID.name())
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof BadRequestException));
    verify(deduplicationService).getPotentialDuplicateByUid(uid);
  }

  @Test
  void shouldThrowUpdatePotentialDuplicateToMergedStatus() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    mockMvc
        .perform(
            put(ENDPOINT + "/" + uid)
                .param("status", DeduplicationStatus.MERGED.name())
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof BadRequestException));
    verify(deduplicationService).getPotentialDuplicateByUid(uid);
  }

  @Test
  void shouldUpdatePotentialDuplicate() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    mockMvc
        .perform(
            put(ENDPOINT + "/" + uid)
                .param("status", DeduplicationStatus.INVALID.name())
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    ArgumentCaptor<PotentialDuplicate> potentialDuplicateArgumentCaptor =
        ArgumentCaptor.forClass(PotentialDuplicate.class);
    verify(deduplicationService).getPotentialDuplicateByUid(uid);
    verify(deduplicationService)
        .updatePotentialDuplicate(potentialDuplicateArgumentCaptor.capture());
    assertEquals(
        DeduplicationStatus.INVALID, potentialDuplicateArgumentCaptor.getValue().getStatus());
  }

  @Test
  void shouldGetAllPotentialDuplicateNoPaging() throws Exception {
    when(deduplicationService.getAllPotentialDuplicatesBy(any()))
        .thenReturn(Collections.singletonList(new PotentialDuplicate(teiA, teiB)));
    mockMvc
        .perform(
            get(ENDPOINT)
                .param("teis", teiA)
                .param("skipPaging", "true")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
    verify(deduplicationService).getAllPotentialDuplicatesBy(any());
  }

  @Test
  void shouldGetPotentialDuplicateById() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    String uid = "uid";
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    mockMvc
        .perform(
            get(ENDPOINT + "/" + uid)
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
    verify(deduplicationService).getPotentialDuplicateByUid(uid);
  }

  @Test
  void shouldThrowMissingPotentialDuplicate() throws Exception {
    String uid = "uid";
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(null);
    mockMvc
        .perform(
            get(ENDPOINT + "/" + uid)
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof NotFoundException));
  }

  @Test
  void shouldMergePotentialDuplicate() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    MergeObject mergeObject = MergeObject.builder().build();
    mockMvc
        .perform(
            post(ENDPOINT + "/" + uid + "/merge")
                .content(objectMapper.writeValueAsString(mergeObject))
                .param("mergeStrategy", MergeStrategy.AUTO.name())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    verify(deduplicationService).autoMerge(deduplicationMergeParams);
    verify(deduplicationService, times(0)).manualMerge(deduplicationMergeParams);
  }

  @Test
  void shouldManualMergePotentialDuplicate() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    MergeObject mergeObject = MergeObject.builder().build();
    mockMvc
        .perform(
            post(ENDPOINT + "/" + uid + "/merge")
                .content(objectMapper.writeValueAsString(mergeObject))
                .param("mergeStrategy", MergeStrategy.MANUAL.name())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    verify(deduplicationService, times(0)).autoMerge(deduplicationMergeParams);
    verify(deduplicationService).manualMerge(deduplicationMergeParams);
  }

  @Test
  void shouldThrowAutoMergeForbiddenException() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    doThrow(new PotentialDuplicateForbiddenException("Forbidden"))
        .when(deduplicationService)
        .autoMerge(deduplicationMergeParams);
    MergeObject mergeObject = MergeObject.builder().build();
    mockMvc
        .perform(
            post(ENDPOINT + "/" + uid + "/merge")
                .content(objectMapper.writeValueAsString(mergeObject))
                .param("mergeStrategy", MergeStrategy.AUTO.name())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result ->
                assertTrue(
                    result.getResolvedException() instanceof PotentialDuplicateForbiddenException));
    verify(deduplicationService).autoMerge(deduplicationMergeParams);
    verify(deduplicationService, times(0)).manualMerge(deduplicationMergeParams);
  }

  @Test
  void shouldThrowAutoMergeConflictException() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(teiA, teiB);
    when(deduplicationService.getPotentialDuplicateByUid(uid)).thenReturn(potentialDuplicate);
    doThrow(new PotentialDuplicateConflictException("Conflict"))
        .when(deduplicationService)
        .autoMerge(deduplicationMergeParams);
    MergeObject mergeObject = MergeObject.builder().build();
    mockMvc
        .perform(
            post(ENDPOINT + "/" + uid + "/merge")
                .content(objectMapper.writeValueAsString(mergeObject))
                .param("mergeStrategy", MergeStrategy.AUTO.name())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result ->
                assertTrue(
                    result.getResolvedException() instanceof PotentialDuplicateConflictException));
    verify(deduplicationService).autoMerge(deduplicationMergeParams);
    verify(deduplicationService, times(0)).manualMerge(deduplicationMergeParams);
  }
}
