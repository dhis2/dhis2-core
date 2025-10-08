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

import static java.util.Objects.requireNonNull;
import static org.hisp.dhis.changelog.ChangeLogType.CREATE;
import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;
import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLogService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationServiceMergeIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private DeduplicationService deduplicationService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramService programService;

  @Autowired private TrackedEntityChangeLogService trackedEntityChangeLogService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private PotentialDuplicate potentialDuplicate;

  private TrackedEntity original;

  private TrackedEntity duplicate;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private Program program1;

  @BeforeEach
  void setUp() throws PotentialDuplicateConflictException {
    orgUnit = createOrganisationUnit("OU_A");
    organisationUnitService.addOrganisationUnit(orgUnit);
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setAllowChangeLog(true);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    original = createTrackedEntity(orgUnit, trackedEntityType);
    duplicate = createTrackedEntity(orgUnit, trackedEntityType);
    manager.save(original);
    manager.save(duplicate);
    program = createProgram('A');
    program.setTrackedEntityType(trackedEntityType);
    program.setAllowChangeLog(true);
    program1 = createProgram('B');
    program1.setTrackedEntityType(trackedEntityType);
    programService.addProgram(program);
    programService.addProgram(program1);
    Enrollment enrollment1 = createEnrollment(program, original, orgUnit);
    Enrollment enrollment2 = createEnrollment(program1, duplicate, orgUnit);
    manager.save(enrollment1);
    manager.save(enrollment2);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(original, program, orgUnit);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(duplicate, program1, orgUnit);
    original.getEnrollments().add(enrollment1);
    duplicate.getEnrollments().add(enrollment2);
    manager.update(original);
    manager.update(duplicate);
    potentialDuplicate = new PotentialDuplicate(UID.of(original), UID.of(duplicate));
    deduplicationService.addPotentialDuplicate(potentialDuplicate);

    User user = createUser(new HashSet<>(Collections.singletonList(orgUnit)), ALL.toString());
    injectSecurityContextUser(user);
  }

  @Test
  void shouldManualMergeWithAuthorityAll()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    DeduplicationMergeParams deduplicationMergeParams =
        DeduplicationMergeParams.builder()
            .potentialDuplicate(potentialDuplicate)
            .original(original)
            .duplicate(duplicate)
            .build();
    Date lastUpdatedOriginal =
        requireNonNull(manager.get(TrackedEntity.class, original.getUid())).getLastUpdated();

    deduplicationService.autoMerge(deduplicationMergeParams);

    assertEquals(
        DeduplicationStatus.MERGED,
        deduplicationService.getPotentialDuplicate(UID.of(potentialDuplicate)).getStatus());
    assertTrue(
        requireNonNull(manager.get(TrackedEntity.class, original.getUid()))
                .getLastUpdated()
                .getTime()
            > lastUpdatedOriginal.getTime());
  }

  @Test
  void shouldManualMergeWithUserGroupOfProgram()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    User user = createAndAddUser(true, "userB", orgUnit, "F_TRACKED_ENTITY_MERGE");
    injectSecurityContextUser(user);
    Sharing sharing = getUserSharing(user, AccessStringHelper.FULL);
    trackedEntityType.setSharing(sharing);
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);
    program.setSharing(sharing);
    program1.setSharing(sharing);
    programService.updateProgram(program);
    programService.updateProgram(program1);
    DeduplicationMergeParams deduplicationMergeParams =
        DeduplicationMergeParams.builder()
            .potentialDuplicate(potentialDuplicate)
            .original(original)
            .duplicate(duplicate)
            .build();
    Date lastUpdatedOriginal =
        requireNonNull(manager.get(TrackedEntity.class, original.getUid())).getLastUpdated();

    deduplicationService.autoMerge(deduplicationMergeParams);

    assertEquals(
        DeduplicationStatus.MERGED,
        deduplicationService.getPotentialDuplicate(UID.of(potentialDuplicate)).getStatus());
    assertTrue(
        requireNonNull(manager.get(TrackedEntity.class, original.getUid()))
                .getLastUpdated()
                .getTime()
            > lastUpdatedOriginal.getTime());
  }

  @Test
  void shouldAddCreateChangeLogWhenAttributeDoesNotExistInOriginalTrackedEntity()
      throws PotentialDuplicateConflictException,
          ForbiddenException,
          PotentialDuplicateForbiddenException,
          NotFoundException,
          BadRequestException {
    TrackedEntityAttribute trackedEntityAttribute = createAndPersistTrackedEntityAttribute();
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        createAndPersistTrackedEntityAttributeValue("value", duplicate, trackedEntityAttribute);
    addTrackedEntityAttributeValue(trackedEntityAttributeValue, duplicate);
    MergeObject mergeObject = createMergeObject(trackedEntityAttribute);
    DeduplicationMergeParams deduplicationParams =
        createDeduplicationParams(original, duplicate, mergeObject, potentialDuplicate);
    deduplicationService.manualMerge(deduplicationParams);

    assertEquals(
        DeduplicationStatus.MERGED,
        deduplicationService.getPotentialDuplicate(UID.of(potentialDuplicate)).getStatus());

    List<TrackedEntityChangeLog> trackedEntityChangeLogs =
        trackedEntityChangeLogService
            .getTrackedEntityChangeLog(
                UID.of(original.getUid()),
                null,
                TrackedEntityChangeLogOperationParams.builder().build(),
                PageParams.of(1, 50, false))
            .getItems();
    assertChangeLogCreate(trackedEntityChangeLogs);
  }

  @Test
  void shouldAddUpdateChangeLogWhenAttributeExistsInBothEntities()
      throws PotentialDuplicateConflictException,
          ForbiddenException,
          PotentialDuplicateForbiddenException,
          NotFoundException,
          BadRequestException {
    TrackedEntityAttribute trackedEntityAttribute = createAndPersistTrackedEntityAttribute();
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        createAndPersistTrackedEntityAttributeValue("value", original, trackedEntityAttribute);
    addTrackedEntityAttributeValue(trackedEntityAttributeValue, original);
    addTrackedEntityAttributeValue(trackedEntityAttributeValue, duplicate);
    MergeObject mergeObject = createMergeObject(trackedEntityAttribute);
    DeduplicationMergeParams deduplicationParams =
        createDeduplicationParams(original, duplicate, mergeObject, potentialDuplicate);
    deduplicationService.manualMerge(deduplicationParams);

    assertEquals(
        DeduplicationStatus.MERGED,
        deduplicationService.getPotentialDuplicate(UID.of(potentialDuplicate)).getStatus());
    List<TrackedEntityChangeLog> trackedEntityChangeLogs =
        trackedEntityChangeLogService
            .getTrackedEntityChangeLog(
                UID.of(original.getUid()),
                null,
                TrackedEntityChangeLogOperationParams.builder().build(),
                PageParams.of(1, 50, false))
            .getItems();
    assertChangeLogUpdate(trackedEntityChangeLogs, "value");
  }

  @Test
  void shouldDeleteDuplicatedTrackedEntityChangeLogsWhenMerged()
      throws PotentialDuplicateConflictException,
          ForbiddenException,
          PotentialDuplicateForbiddenException,
          NotFoundException,
          BadRequestException {
    TrackedEntityAttribute trackedEntityAttribute = createAndPersistTrackedEntityAttribute();
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        createAndPersistTrackedEntityAttributeValue("value", original, trackedEntityAttribute);
    addTrackedEntityAttributeValue(trackedEntityAttributeValue, original);
    addTrackedEntityAttributeValue(trackedEntityAttributeValue, duplicate);
    trackedEntityChangeLogService.addTrackedEntityChangeLog(
        duplicate,
        trackedEntityAttribute,
        "previous value",
        "current value",
        UPDATE,
        getCurrentUser().getUsername());
    MergeObject mergeObject = createMergeObject(trackedEntityAttribute);
    DeduplicationMergeParams deduplicationParams =
        createDeduplicationParams(original, duplicate, mergeObject, potentialDuplicate);
    deduplicationService.manualMerge(deduplicationParams);

    assertEquals(
        DeduplicationStatus.MERGED,
        deduplicationService.getPotentialDuplicate(UID.of(potentialDuplicate)).getStatus());

    List<TrackedEntityChangeLog> trackedEntityChangeLogs =
        trackedEntityChangeLogService
            .getTrackedEntityChangeLog(
                UID.of(original.getUid()),
                null,
                TrackedEntityChangeLogOperationParams.builder().build(),
                PageParams.of(1, 50, false))
            .getItems()
            .stream()
            .filter(cl -> cl.getTrackedEntity().getUid().equals(duplicate.getUid()))
            .toList();
    assertIsEmpty(trackedEntityChangeLogs);
  }

  private Sharing getUserSharing(User user, String accessStringHelper) {
    UserGroup userGroup = new UserGroup();
    userGroup.setName("UserGroupA");
    user.getGroups().add(userGroup);
    Map<String, UserAccess> userSharing = new HashMap<>();
    userSharing.put(user.getUid(), new UserAccess(user, AccessStringHelper.DEFAULT));
    Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
    userGroupSharing.put(userGroup.getUid(), new UserGroupAccess(userGroup, accessStringHelper));
    return Sharing.builder()
        .external(false)
        .publicAccess(AccessStringHelper.DEFAULT)
        .owner("testOwner")
        .userGroups(userGroupSharing)
        .users(userSharing)
        .build();
  }

  private User createUser(HashSet<OrganisationUnit> ou, String... authorities) {
    User user = createUserWithAuth("testUser", authorities);
    user.setOrganisationUnits(ou);
    return user;
  }

  private TrackedEntityAttribute createAndPersistTrackedEntityAttribute() {
    TrackedEntityAttribute trackedEntityAttribute =
        new TrackedEntityAttribute("TEA", "", ValueType.TEXT, false, false);
    trackedEntityAttribute.setShortName("TEA");
    trackedEntityAttribute.setAggregationType(AggregationType.AVERAGE);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    return trackedEntityAttribute;
  }

  private TrackedEntityAttributeValue createAndPersistTrackedEntityAttributeValue(
      String value, TrackedEntity trackedEntity, TrackedEntityAttribute trackedEntityAttribute) {
    TrackedEntityTypeAttribute trackedEntityTypeAttribute =
        new TrackedEntityTypeAttribute(trackedEntityType, trackedEntityAttribute);
    trackedEntityType.getTrackedEntityTypeAttributes().add(trackedEntityTypeAttribute);
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);
    TrackedEntityAttributeValue trackedEntityAttributeValue =
        new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity);
    trackedEntityAttributeValue.setValue(value);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(trackedEntityAttributeValue);

    return trackedEntityAttributeValue;
  }

  private DeduplicationMergeParams createDeduplicationParams(
      TrackedEntity original,
      TrackedEntity duplicate,
      MergeObject mergeObject,
      PotentialDuplicate potentialDuplicate) {
    return DeduplicationMergeParams.builder()
        .original(original)
        .duplicate(duplicate)
        .mergeObject(mergeObject)
        .potentialDuplicate(potentialDuplicate)
        .build();
  }

  private MergeObject createMergeObject(TrackedEntityAttribute trackedEntityAttribute) {
    return MergeObject.builder()
        .trackedEntityAttributes(Set.of(UID.of(trackedEntityAttribute.getUid())))
        .build();
  }

  private void addTrackedEntityAttributeValue(
      TrackedEntityAttributeValue trackedEntityAttributeValue, TrackedEntity trackedEntity) {
    trackedEntity.getTrackedEntityAttributeValues().add(trackedEntityAttributeValue);
    manager.update(trackedEntity);
  }

  private void assertChangeLogCreate(List<TrackedEntityChangeLog> trackedEntityChangeLogs) {
    assertNotEmpty(trackedEntityChangeLogs);
    assertEquals(original.getUid(), trackedEntityChangeLogs.get(0).getTrackedEntity().getUid());
    assertEquals("value", trackedEntityChangeLogs.get(0).getCurrentValue());
    assertEquals(CREATE, trackedEntityChangeLogs.get(0).getChangeLogType());
  }

  private void assertChangeLogUpdate(
      List<TrackedEntityChangeLog> trackedEntityChangeLogs, String expectedValue) {
    Assertions.assertAll(
        () -> assertNotEmpty(trackedEntityChangeLogs),
        () ->
            assertEquals(
                original.getUid(), trackedEntityChangeLogs.get(0).getTrackedEntity().getUid()),
        () -> assertEquals(expectedValue, trackedEntityChangeLogs.get(0).getCurrentValue()),
        () -> assertEquals(expectedValue, trackedEntityChangeLogs.get(0).getPreviousValue()),
        () -> assertEquals(UPDATE, trackedEntityChangeLogs.get(0).getChangeLogType()));
  }
}
