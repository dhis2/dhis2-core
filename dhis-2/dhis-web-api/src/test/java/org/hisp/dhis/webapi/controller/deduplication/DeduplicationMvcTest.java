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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.deduplication.DeduplicationMergeParams;
import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.MergeObject;
import org.hisp.dhis.deduplication.MergeStrategy;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.deduplication.PotentialDuplicateForbiddenException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
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
class DeduplicationMvcTest {
  private static final String ENDPOINT = "/" + "potentialDuplicates";

  private MockMvc mockMvc;

  @Mock private DeduplicationService deduplicationService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private CurrentUserService currentUserService;

  @Mock private FieldFilterService fieldFilterService;

  @Mock private ContextService contextService;

  @Mock private TrackedEntity trackedEntityA;

  @Mock private TrackedEntity trackedEntityB;

  @InjectMocks private DeduplicationController deduplicationController;

  private DeduplicationMergeParams deduplicationMergeParams;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String original = CodeGenerator.generateUid();

  private static final String duplicate = CodeGenerator.generateUid();

  @BeforeEach
  void setUp() {
    deduplicationMergeParams =
        DeduplicationMergeParams.builder()
            .potentialDuplicate(new PotentialDuplicate(original, duplicate))
            .original(trackedEntityA)
            .duplicate(trackedEntityB)
            .mergeObject(MergeObject.builder().build())
            .build();
    mockMvc =
        MockMvcBuilders.standaloneSetup(deduplicationController)
            .setControllerAdvice(new CrudControllerAdvice())
            .build();
  }

  @Test
  void shouldPostPotentialDuplicateWhenTeisExistAndUserHasAccess() throws Exception {
    when(trackerAccessManager.canRead(any(User.class), any(TrackedEntity.class)))
        .thenReturn(Collections.emptyList());
    when(currentUserService.getCurrentUser()).thenReturn(new User());
    when(trackedEntityService.getTrackedEntity(original)).thenReturn(trackedEntityA);
    when(trackedEntityService.getTrackedEntity(duplicate)).thenReturn(trackedEntityB);

    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
    mockMvc
        .perform(
            post(ENDPOINT)
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"));
    verify(deduplicationService).addPotentialDuplicate(any());
  }

  @Test
  void shouldThrowForbiddenExceptionExceptionWhenPostAndUserHasNoTeiAccess() throws Exception {
    when(trackerAccessManager.canRead(any(User.class), any(TrackedEntity.class)))
        .thenReturn(List.of("error"));
    when(currentUserService.getCurrentUser()).thenReturn(new User());
    when(trackedEntityService.getTrackedEntity(original)).thenReturn(trackedEntityA);

    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
    mockMvc
        .perform(
            post(ENDPOINT)
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ForbiddenException));
    verify(deduplicationService, times(0)).addPotentialDuplicate(any());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenPostAndTeiDoNotExists() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, null);
    mockMvc
        .perform(
            post(ENDPOINT)
                .content(objectMapper.writeValueAsString(potentialDuplicate))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof BadRequestException));
    verify(deduplicationService, times(0)).addPotentialDuplicate(any());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenPutAndPotentialDuplicateIsAlreadyMerged()
      throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldThrowBadRequestExceptionWhenPutPotentialDuplicateToMergedStatus() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldUpdatePotentialDuplicateWhenPotentialDuplicateExists() throws Exception {
    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldGetPotentialDuplicateByIdWhenPotentialDuplicateExists() throws Exception {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldThrowNotFoundExceptionWhenPotentialDuplicateNotExists() throws Exception {
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
    verify(deduplicationService).getPotentialDuplicateByUid(uid);
  }

  @Test
  void shouldAutoMergePotentialDuplicateWhenUserHasAccessAndMergeIsOk() throws Exception {
    when(trackedEntityService.getTrackedEntity(original)).thenReturn(trackedEntityA);
    when(trackedEntityService.getTrackedEntity(duplicate)).thenReturn(trackedEntityB);

    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldManualMergePotentialDuplicateWhenUserHasAccessAndMergeIsOk() throws Exception {
    when(trackedEntityService.getTrackedEntity(original)).thenReturn(trackedEntityA);
    when(trackedEntityService.getTrackedEntity(duplicate)).thenReturn(trackedEntityB);

    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldThrowForbiddenExceptionWhenAutoMergingIsForbidden() throws Exception {
    when(trackedEntityService.getTrackedEntity(original)).thenReturn(trackedEntityA);
    when(trackedEntityService.getTrackedEntity(duplicate)).thenReturn(trackedEntityB);

    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
  void shouldThrowConflictExceptionWhenAutoMergeHasConflicts() throws Exception {
    when(trackedEntityService.getTrackedEntity(original)).thenReturn(trackedEntityA);
    when(trackedEntityService.getTrackedEntity(duplicate)).thenReturn(trackedEntityB);

    String uid = "uid";
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(original, duplicate);
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
