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
package org.hisp.dhis.webapi.controller.tracker.deduplication;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
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
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.deduplication.DeduplicationMergeParams;
import org.hisp.dhis.tracker.deduplication.DeduplicationService;
import org.hisp.dhis.tracker.deduplication.DeduplicationStatus;
import org.hisp.dhis.tracker.deduplication.MergeObject;
import org.hisp.dhis.tracker.deduplication.MergeStrategy;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicate;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateForbiddenException;
import org.hisp.dhis.tracker.deprecated.audit.TrackedEntityAuditService;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.webapi.controller.CrudControllerAdvice;
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
  private static final String ENDPOINT = "/api/" + "potentialDuplicates";

  private MockMvc mockMvc;

  @Mock private DeduplicationService deduplicationService;

  @Mock private IdentifiableObjectManager manager;

  @Mock private TrackedEntityAuditService trackedEntityAuditService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private TrackedEntity trackedEntityA;

  @Mock private TrackedEntity trackedEntityB;

  @InjectMocks private DeduplicationController deduplicationController;

  private DeduplicationMergeParams deduplicationMergeParams;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String original = CodeGenerator.generateUid();

  private static final String duplicate = CodeGenerator.generateUid();

  @BeforeEach
  void setUp() {
    injectSecurityContextNoSettings(new SystemUser());
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
    when(trackerAccessManager.canRead(any(), any(TrackedEntity.class)))
        .thenReturn(Collections.emptyList());

    when(manager.get(TrackedEntity.class, original)).thenReturn(trackedEntityA);
    when(manager.get(TrackedEntity.class, duplicate)).thenReturn(trackedEntityB);

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
    when(trackerAccessManager.canRead(any(), any(TrackedEntity.class)))
        .thenReturn(List.of("error"));

    when(manager.get(TrackedEntity.class, original)).thenReturn(trackedEntityA);

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
    when(manager.get(TrackedEntity.class, original)).thenReturn(trackedEntityA);
    when(manager.get(TrackedEntity.class, duplicate)).thenReturn(trackedEntityB);

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
    when(manager.get(TrackedEntity.class, original)).thenReturn(trackedEntityA);
    when(manager.get(TrackedEntity.class, duplicate)).thenReturn(trackedEntityB);

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
    when(manager.get(TrackedEntity.class, original)).thenReturn(trackedEntityA);
    when(manager.get(TrackedEntity.class, duplicate)).thenReturn(trackedEntityB);

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
    when(manager.get(TrackedEntity.class, original)).thenReturn(trackedEntityA);
    when(manager.get(TrackedEntity.class, duplicate)).thenReturn(trackedEntityB);

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
