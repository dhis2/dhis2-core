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
package org.hisp.dhis.tracker.deduplication;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

  private static final String TE_A_UID = "TvctPPhpD8u";

  private static final String TE_B_UID = "D9PbzJY8bJO";

  @InjectMocks private DefaultDeduplicationService deduplicationService;

  @Mock private TrackedEntity trackedEntityA;

  @Mock private TrackedEntity trackedEntityB;

  @Mock private Enrollment enrollmentA;

  @Mock private Enrollment enrollmentB;

  @Mock private UserService userService;

  @Mock private DeduplicationHelper deduplicationHelper;

  @Mock private TrackerObjectDeletionService trackerObjectDeletionService;

  @Mock private HibernatePotentialDuplicateStore potentialDuplicateStore;

  private DeduplicationMergeParams deduplicationMergeParams;

  private static final String SEX_UID = CodeGenerator.generateUid();

  private static final String SEX_NAME = "sex";

  private static final String FIRST_NAME_UID = CodeGenerator.generateUid();

  private static final String FIRST_NAME = "firstName";

  private static final String TEAV_SEX = "Male";

  private static final String TEAV_SEX_FIRST_NAME = "John";

  @BeforeEach
  void setUp() throws ForbiddenException, NotFoundException {
    PotentialDuplicate potentialDuplicate = new PotentialDuplicate(UID.generate(), UID.generate());
    deduplicationMergeParams =
        DeduplicationMergeParams.builder()
            .potentialDuplicate(potentialDuplicate)
            .original(trackedEntityA)
            .duplicate(trackedEntityB)
            .mergeObject(MergeObject.builder().build())
            .build();
    TrackedEntityType trackedEntityPerson = new TrackedEntityType();
    trackedEntityPerson.setName("Person");
    trackedEntityPerson.setUid(CodeGenerator.generateUid());
    when(trackedEntityA.getUid()).thenReturn(TE_A_UID);
    when(trackedEntityB.getUid()).thenReturn(TE_B_UID);
    when(trackedEntityA.getTrackedEntityType()).thenReturn(trackedEntityPerson);
    when(trackedEntityB.getTrackedEntityType()).thenReturn(trackedEntityPerson);
    when(deduplicationHelper.getUserAccessErrors(any(), any(), any())).thenReturn(null);
    setUpPrograms();
    setAttributeValues();
  }

  private void setUpPrograms() {
    when(trackedEntityA.getEnrollments())
        .thenReturn(new HashSet<>(Collections.singletonList(enrollmentA)));
    when(trackedEntityB.getEnrollments())
        .thenReturn(new HashSet<>(Collections.singletonList(enrollmentB)));
    Program programA = new Program();
    programA.setUid(CodeGenerator.generateUid());
    programA.setDescription("programADescr");
    programA.setName("programAName");
    Program programB = new Program();
    programB.setUid(CodeGenerator.generateUid());
    programB.setDescription("programBDescr");
    programB.setName("programBName");
    when(enrollmentA.getProgram()).thenReturn(programA);
    when(enrollmentB.getProgram()).thenReturn(programB);
  }

  private void setAttributeValues() {
    TrackedEntityAttributeValue sexAttributeValueA =
        getTrackedEntityAttributeValue(SEX_UID, SEX_NAME, trackedEntityA);
    sexAttributeValueA.setValue(TEAV_SEX);
    TrackedEntityAttributeValue nameAttributeValueA =
        getTrackedEntityAttributeValue(FIRST_NAME_UID, FIRST_NAME, trackedEntityA);
    nameAttributeValueA.setValue(TEAV_SEX_FIRST_NAME);
    TrackedEntityAttributeValue sexAttributeValueB =
        getTrackedEntityAttributeValue(SEX_UID, SEX_NAME, trackedEntityB);
    sexAttributeValueB.setValue(TEAV_SEX);
    TrackedEntityAttributeValue nameAttributeValueB =
        getTrackedEntityAttributeValue(FIRST_NAME_UID, FIRST_NAME, trackedEntityB);
    nameAttributeValueB.setValue(TEAV_SEX_FIRST_NAME);
    when(trackedEntityA.getTrackedEntityAttributeValues())
        .thenReturn(new HashSet<>(Arrays.asList(sexAttributeValueA, nameAttributeValueA)));
    when(trackedEntityB.getTrackedEntityAttributeValues())
        .thenReturn(new HashSet<>(Arrays.asList(sexAttributeValueB, nameAttributeValueB)));
  }

  @Test
  void shouldBeAutoMergeable()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    SystemUser actingUser = new SystemUser();
    injectSecurityContextNoSettings(actingUser);

    MergeObject mergeObject = MergeObject.builder().build();
    when(deduplicationHelper.generateMergeObject(trackedEntityA, trackedEntityB))
        .thenReturn(mergeObject);
    deduplicationService.autoMerge(deduplicationMergeParams);
    verify(deduplicationHelper).getUserAccessErrors(trackedEntityA, trackedEntityB, mergeObject);
    verify(deduplicationHelper).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore)
        .moveTrackedEntityAttributeValues(
            trackedEntityA, trackedEntityB, mergeObject.getTrackedEntityAttributes());
    verify(potentialDuplicateStore)
        .moveRelationships(trackedEntityA, trackedEntityB, mergeObject.getRelationships());
    verify(trackerObjectDeletionService).deleteTrackedEntities(List.of(UID.of(trackedEntityB)));
    verify(potentialDuplicateStore)
        .update(argThat(t -> t.getStatus().equals(DeduplicationStatus.MERGED)));
    verify(potentialDuplicateStore).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldNotBeAutoMergeableDifferentTrackedEntityType()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    String uidOther = "uidOther";
    TrackedEntityType trackedEntityOther = new TrackedEntityType();
    trackedEntityOther.setName("Other");
    trackedEntityOther.setUid(uidOther);
    when(trackedEntityB.getTrackedEntityType()).thenReturn(trackedEntityOther);
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore, times(0)).update(any());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldNotBeAutoMergeableSameEnrollment()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    Program program = new Program();
    program.setUid("programUid");
    when(enrollmentA.getProgram()).thenReturn(program);
    when(enrollmentB.getProgram()).thenReturn(program);
    when(trackedEntityA.getEnrollments()).thenReturn(Sets.newHashSet(enrollmentA));
    when(trackedEntityB.getEnrollments()).thenReturn(Sets.newHashSet(enrollmentB));
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore, times(0)).update(any());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldNotBeAutoMergeableDeletedTrackedEntity()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    when(trackedEntityA.isDeleted()).thenReturn(true);
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    when(trackedEntityA.isDeleted()).thenReturn(false);
    when(trackedEntityB.isDeleted()).thenReturn(true);
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore, times(0)).update(any());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldNotBeAutoMergeableWithSameProgram()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    Program program = new Program();
    program.setUid("progrUid");
    program.setDescription("programDescr");
    program.setName("programName");
    when(enrollmentA.getProgram()).thenReturn(program);
    when(enrollmentB.getProgram()).thenReturn(program);
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore, times(0)).update(any());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldNotBeAutoMergeableDifferentAttributeValues()
      throws PotentialDuplicateConflictException, PotentialDuplicateForbiddenException {
    TrackedEntityAttributeValue sexAttributeValueB =
        getTrackedEntityAttributeValue(SEX_UID, SEX_NAME, trackedEntityB);
    sexAttributeValueB.setValue(TEAV_SEX);
    TrackedEntityAttributeValue nameAttributeValueB =
        getTrackedEntityAttributeValue(FIRST_NAME_UID, FIRST_NAME, trackedEntityB);
    nameAttributeValueB.setValue("Jimmy");
    when(trackedEntityB.getTrackedEntityAttributeValues())
        .thenReturn(new HashSet<>(Arrays.asList(sexAttributeValueB, nameAttributeValueB)));
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore, times(0)).update(any());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldNotBeAutoMergeableNoUserAccess()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    MergeObject mergeObject = MergeObject.builder().build();
    when(deduplicationHelper.generateMergeObject(trackedEntityA, trackedEntityB))
        .thenReturn(mergeObject);
    when(deduplicationHelper.getUserAccessErrors(trackedEntityA, trackedEntityB, mergeObject))
        .thenReturn("error");
    assertThrows(
        PotentialDuplicateForbiddenException.class,
        () -> deduplicationService.autoMerge(deduplicationMergeParams));
    verify(deduplicationHelper).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(deduplicationHelper).getUserAccessErrors(trackedEntityA, trackedEntityB, mergeObject);
    verify(potentialDuplicateStore, times(0)).update(any());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldtBeAutoMergeableAttributeValuesIsEmpty()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          NotFoundException,
          ForbiddenException {
    SystemUser actingUser = new SystemUser();
    injectSecurityContextNoSettings(actingUser);

    when(trackedEntityB.getTrackedEntityAttributeValues()).thenReturn(new HashSet<>());
    MergeObject mergeObject = MergeObject.builder().build();
    when(deduplicationHelper.generateMergeObject(trackedEntityA, trackedEntityB))
        .thenReturn(mergeObject);
    deduplicationService.autoMerge(deduplicationMergeParams);
    verify(deduplicationHelper).getUserAccessErrors(trackedEntityA, trackedEntityB, mergeObject);
    verify(deduplicationHelper).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(potentialDuplicateStore)
        .moveTrackedEntityAttributeValues(
            trackedEntityA, trackedEntityB, mergeObject.getTrackedEntityAttributes());
    verify(potentialDuplicateStore)
        .moveRelationships(trackedEntityA, trackedEntityB, mergeObject.getRelationships());
    verify(trackerObjectDeletionService).deleteTrackedEntities(List.of(UID.of(trackedEntityB)));
    verify(potentialDuplicateStore).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldBeManualMergeable()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          NotFoundException,
          ForbiddenException {
    SystemUser actingUser = new SystemUser();
    injectSecurityContextNoSettings(actingUser);

    deduplicationService.manualMerge(deduplicationMergeParams);
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(deduplicationHelper)
        .getUserAccessErrors(
            trackedEntityA, trackedEntityB, deduplicationMergeParams.getMergeObject());
    verify(potentialDuplicateStore)
        .moveTrackedEntityAttributeValues(
            trackedEntityA,
            trackedEntityB,
            deduplicationMergeParams.getMergeObject().getTrackedEntityAttributes());
    verify(potentialDuplicateStore)
        .moveRelationships(
            trackedEntityA,
            trackedEntityB,
            deduplicationMergeParams.getMergeObject().getRelationships());
    verify(trackerObjectDeletionService).deleteTrackedEntities(List.of(UID.of(trackedEntityB)));
    verify(potentialDuplicateStore).auditMerge(deduplicationMergeParams);
  }

  @Test
  void shouldThrowManualMergeableHasInvalidReference()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    when(deduplicationHelper.getInvalidReferenceErrors(deduplicationMergeParams))
        .thenReturn("Error");
    assertThrows(
        PotentialDuplicateConflictException.class,
        () -> deduplicationService.manualMerge(deduplicationMergeParams));
    verify(deduplicationHelper, times(1)).getInvalidReferenceErrors(deduplicationMergeParams);
    verify(deduplicationHelper, times(0)).generateMergeObject(trackedEntityA, trackedEntityB);
    verify(deduplicationHelper, times(0))
        .getUserAccessErrors(
            trackedEntityA, trackedEntityB, deduplicationMergeParams.getMergeObject());
    verify(potentialDuplicateStore, times(0)).auditMerge(deduplicationMergeParams);
  }

  private TrackedEntityAttributeValue getTrackedEntityAttributeValue(
      String uid, String name, TrackedEntity trackedEntity) {
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setTrackedEntity(trackedEntity);
    TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
    trackedEntityAttribute.setUid(uid);
    trackedEntityAttribute.setName(name);
    attributeValue.setAttribute(trackedEntityAttribute);
    return attributeValue;
  }
}
