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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityEnrollmentParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class TrackerOwnershipManagerTest extends IntegrationTestBase {

  @Autowired private TrackerOwnershipManager trackerOwnershipAccessManager;

  @Autowired private UserService _userService;

  @Autowired private TrackedEntityService entityInstanceService;

  @Autowired
  private org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  private TrackedEntity entityInstanceA1;

  private TrackedEntity entityInstanceB1;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private Program programB;

  private User userA;

  private User userB;

  @Override
  protected void setUpTest() throws Exception {
    //    userService = _userService;
    //    preCreateInjectAdminUser();

    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);

    entityInstanceA1 = createTrackedEntity(organisationUnitA);
    entityInstanceB1 = createTrackedEntity(organisationUnitB);
    entityInstanceService.addTrackedEntity(entityInstanceA1);
    entityInstanceService.addTrackedEntity(entityInstanceB1);

    programA = createProgram('A');
    programA.setAccessLevel(AccessLevel.PROTECTED);
    programService.addProgram(programA);
    programA.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    programService.updateProgram(programA);
    programB = createProgram('B');
    programB.setAccessLevel(AccessLevel.CLOSED);
    programService.addProgram(programB);

    userA = createUserWithAuth("userA");
    userA.addOrganisationUnit(organisationUnitA);
    userService.updateUser(userA);
    userB = createUserWithAuth("userB");
    userB.addOrganisationUnit(organisationUnitB);
    userService.updateUser(userB);
  }

  @Test
  void testAssignOwnership() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceB1, programA));
    trackerOwnershipAccessManager.assignOwnership(
        entityInstanceA1, programA, organisationUnitB, false, true);
    assertFalse(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
  }

  @Test
  void testGrantTemporaryOwnershipWithAudit() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    trackerOwnershipAccessManager.grantTemporaryOwnership(
        entityInstanceA1, programA, userB, "testing reason");
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
  }

  @Test
  void shouldNotHaveAccessToEnrollmentWithUserAWhenTransferredToAnotherOrgUnit() {
    trackerOwnershipAccessManager.assignOwnership(
        entityInstanceA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(
        entityInstanceA1, programA, organisationUnitB, false, true);

    injectSecurityContextUser(userA);
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    entityInstanceA1.getUid(), programA.getUid(), params, false));
    assertEquals("OWNERSHIP_ACCESS_DENIED", exception.getMessage());
  }

  @Test
  void shouldHaveAccessToEnrollmentWithUserBWhenTransferredToOwnOrgUnit()
      throws ForbiddenException, NotFoundException {
    trackerOwnershipAccessManager.assignOwnership(
        entityInstanceA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(
        entityInstanceA1, programA, organisationUnitB, false, true);

    injectSecurityContextUser(userB);
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);
    assertEquals(
        entityInstanceA1,
        trackedEntityService.getTrackedEntity(
            entityInstanceA1.getUid(), programA.getUid(), params, false));
  }

  @Test
  void shouldHaveAccessToTrackedEntityWhenProgramNotProvidedAndRegisteringOrgUnitInScope()
      throws ForbiddenException, NotFoundException {
    injectSecurityContextUser(userA);
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);

    assertEquals(
        entityInstanceA1,
        trackedEntityService.getTrackedEntity(entityInstanceA1.getUid(), params, false));
  }

  @Test
  void shouldNotHaveAccessToTrackedEntityWhenProgramNotProvidedAndRegisteringOrgUnitNotInScope() {
    injectSecurityContextUser(userB);
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () -> trackedEntityService.getTrackedEntity(entityInstanceA1.getUid(), params, false));
    assertEquals(
        String.format(
            "[User has no read access to organisation unit: %s]", organisationUnitA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndUserInCaptureScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userA, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programA));
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndHasTemporaryAccess() {
    trackerOwnershipAccessManager.grantTemporaryOwnership(
        entityInstanceA1, programA, userB, "test protected program");
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programA));
  }

  @Test
  void shouldNotHaveAccessWhenProgramProtectedAndUserNotInCaptureScopeNorHasTemporaryAccess() {
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    assertFalse(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programA));
  }

  @Test
  void shouldHaveAccessWhenProgramClosedAndUserInCaptureScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceB1, programB));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceB1.getUid(), entityInstanceB1.getOrganisationUnit(), programB));
  }

  @Test
  void shouldNotHaveAccessWhenProgramClosedAndUserHasTemporaryAccess() {
    trackerOwnershipAccessManager.grantTemporaryOwnership(
        entityInstanceA1, programB, userB, "test closed program");
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programB));
    assertFalse(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programB));
  }
}
