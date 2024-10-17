/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.service;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntityInstanceSupportServiceTest {

  @Mock private ProgramService programService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private CurrentUserService currentUserService;

  @InjectMocks private TrackedEntityInstanceSupportService trackedEntityInstanceSupportService;

  private TrackedEntityInstance trackedEntity;

  private Program program;

  private User user;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  private TrackedEntityAttribute tea;

  @BeforeEach
  public void setUpTest() {
    TrackedEntityType trackedEntityType = new TrackedEntityType("TET", "desc");
    trackedEntityType.setAutoFields();
    trackedEntity = new TrackedEntityInstance();
    trackedEntity.setAutoFields();
    trackedEntity.setTrackedEntityType(trackedEntityType);
    program = new Program("A");
    program.setUid("A");
    program.setTrackedEntityType(trackedEntityType);
    user = new User();

    trackedEntity = new TrackedEntityInstance();
    trackedEntity.setUid("TeUid12345");
    trackedEntity.setTrackedEntityType(trackedEntityType);
    tea = new TrackedEntityAttribute();
    tea.setUid("TeaUid12345");
    tea.setUnique(true);
    tea.setValueType(ValueType.TEXT);
    tea.setOrgunitScope(false);

    trackedEntityAttributeValue =
        new TrackedEntityAttributeValue(tea, trackedEntity, "attribute value");
    trackedEntity.setTrackedEntityAttributeValues(Set.of(trackedEntityAttributeValue));

    TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
    trackedEntityTypeAttribute.setTrackedEntityAttribute(tea);
    trackedEntityType.setTrackedEntityTypeAttributes(List.of(trackedEntityTypeAttribute));
    trackedEntity.setTrackedEntityType(trackedEntityType);

    when(currentUserService.getCurrentUser()).thenReturn(user);
  }

  @Test
  void shouldWorkWhenGettingAttributeValueIfIsProgramAttribute()
      throws NotFoundException, IllegalAccessException {
    when(instanceService.getTrackedEntityInstance(trackedEntity.getUid()))
        .thenReturn(trackedEntity);
    when(programService.getProgram(program.getUid())).thenReturn(program);
    when(trackerAccessManager.canRead(user, trackedEntity, program, false)).thenReturn(emptyList());
    when(attributeService.getProgramAttributes(program)).thenReturn(Set.of(tea));

    assertEquals(
        trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
            trackedEntity.getUid(), tea.getUid(), program.getUid()),
        trackedEntityAttributeValue);
  }

  @Test
  void shouldFailWhenGettingAttributeValueIfProgramDoesNotExist() {
    when(instanceService.getTrackedEntityInstance(trackedEntity.getUid()))
        .thenReturn(trackedEntity);

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
                    trackedEntity.getUid(), tea.getUid(), program.getUid()));
    assertEquals("Program not found for ID " + program.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenGettingAttributeValueIfTeProgramNotAccessible() {
    when(instanceService.getTrackedEntityInstance(trackedEntity.getUid()))
        .thenReturn(trackedEntity);
    when(programService.getProgram(program.getUid())).thenReturn(program);
    when(trackerAccessManager.canRead(user, trackedEntity, program, false))
        .thenReturn(List.of("error"));

    Exception exception =
        assertThrows(
            IllegalAccessException.class,
            () ->
                trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
                    trackedEntity.getUid(), tea.getUid(), program.getUid()));
    assertEquals(
        "You're not authorized to access the TrackedEntity with id: " + trackedEntity.getUid(),
        exception.getMessage());
  }

  @Test
  void shouldWorkWhenGettingAttributeValueIfIsTrackedEntityTypeAttribute()
      throws NotFoundException, IllegalAccessException {
    when(instanceService.getTrackedEntityInstance(trackedEntity.getUid()))
        .thenReturn(trackedEntity);
    when(trackerAccessManager.canRead(user, trackedEntity)).thenReturn(emptyList());
    when(attributeService.getTrackedEntityTypeAttributes(trackedEntity.getTrackedEntityType()))
        .thenReturn(Set.of(tea));

    assertEquals(
        trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
            trackedEntity.getUid(), tea.getUid(), null),
        trackedEntityAttributeValue);
  }

  @Test
  void shouldFailWhenGettingAttributeValueIfTrackedEntityDoesNotExist() {
    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
                    trackedEntity.getUid(), tea.getUid(), null));
    assertEquals(
        "Tracked entity not found for ID " + trackedEntity.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenGettingAttributeIfAttributeNotFound() {
    when(instanceService.getTrackedEntityInstance(trackedEntity.getUid()))
        .thenReturn(trackedEntity);
    when(trackerAccessManager.canRead(user, trackedEntity)).thenReturn(emptyList());

    Exception exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityInstanceSupportService.getTrackedEntityAttributeValue(
                    trackedEntity.getUid(), tea.getUid(), null));
    assertEquals("Attribute not found for ID " + tea.getUid(), exception.getMessage());
  }
}
