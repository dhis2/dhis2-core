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
package org.hisp.dhis.tracker.deduplication;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeduplicationServiceMergeIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private DeduplicationService deduplicationService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramService programService;

  @Autowired private IdentifiableObjectManager manager;

  @Test
  void shouldManualMergeWithAuthorityAll()
      throws PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          NotFoundException {
    OrganisationUnit ou = createOrganisationUnit("OU_A");
    organisationUnitService.addOrganisationUnit(ou);
    User user =
        createUser(new HashSet<>(Collections.singletonList(ou)), Authorities.ALL.toString());
    injectSecurityContextUser(user);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    TrackedEntity original = createTrackedEntity(ou);
    TrackedEntity duplicate = createTrackedEntity(ou);
    original.setTrackedEntityType(trackedEntityType);
    duplicate.setTrackedEntityType(trackedEntityType);
    manager.save(original);
    manager.save(duplicate);
    Program program = createProgram('A');
    Program program1 = createProgram('B');
    programService.addProgram(program);
    programService.addProgram(program1);
    Enrollment enrollment1 = createEnrollment(program, original, ou);
    Enrollment enrollment2 = createEnrollment(program1, duplicate, ou);
    manager.save(enrollment1);
    manager.save(enrollment2);
    original.getEnrollments().add(enrollment1);
    duplicate.getEnrollments().add(enrollment2);
    manager.update(original);
    manager.update(duplicate);
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(UID.of(original), UID.of(duplicate));
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
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
        deduplicationService.getPotentialDuplicateByUid(UID.of(potentialDuplicate)).getStatus());
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
    OrganisationUnit ou = createOrganisationUnit("OU_A");
    organisationUnitService.addOrganisationUnit(ou);
    User user = createAndAddUser(true, "userB", ou, "F_TRACKED_ENTITY_MERGE");
    injectSecurityContextUser(user);
    Sharing sharing = getUserSharing(user, AccessStringHelper.FULL);
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntityType.setSharing(sharing);
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);
    TrackedEntity original = createTrackedEntity(ou);
    TrackedEntity duplicate = createTrackedEntity(ou);
    original.setTrackedEntityType(trackedEntityType);
    duplicate.setTrackedEntityType(trackedEntityType);
    manager.save(original);
    manager.save(duplicate);
    Program program = createProgram('A');
    Program program1 = createProgram('B');
    programService.addProgram(program);
    programService.addProgram(program1);
    program.setSharing(sharing);
    program1.setSharing(sharing);
    Enrollment enrollment1 = createEnrollment(program, original, ou);
    Enrollment enrollment2 = createEnrollment(program1, duplicate, ou);
    manager.save(enrollment1);
    manager.save(enrollment2);
    manager.update(enrollment1);
    manager.update(enrollment2);
    original.getEnrollments().add(enrollment1);
    duplicate.getEnrollments().add(enrollment2);
    manager.update(original);
    manager.update(duplicate);
    PotentialDuplicate potentialDuplicate =
        new PotentialDuplicate(UID.of(original), UID.of(duplicate));
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
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
        deduplicationService.getPotentialDuplicateByUid(UID.of(potentialDuplicate)).getStatus());
    assertTrue(
        requireNonNull(manager.get(TrackedEntity.class, original.getUid()))
                .getLastUpdated()
                .getTime()
            > lastUpdatedOriginal.getTime());
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
}
