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
package org.hisp.dhis.tracker.acl;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.acl.TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class TrackerAccessManagerTest extends PostgresIntegrationTestBase {

  @Autowired private TrackerAccessManager trackerAccessManager;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  private TrackedEntity trackedEntityA;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private Program programA;

  private ProgramStage programStageA;

  private ProgramStage programStageB;

  private TrackedEntityType trackedEntityType;

  private TrackerEvent eventA;

  private TrackerEvent eventB;

  @BeforeEach
  void setUp() {
    CategoryOptionCombo coA = categoryService.getDefaultCategoryOptionCombo();
    orgUnitA = createOrganisationUnit('A');
    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitA);
    manager.save(orgUnitB);
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementA.setValueType(ValueType.INTEGER);
    dataElementB.setValueType(ValueType.INTEGER);
    manager.save(dataElementA);
    manager.save(dataElementB);
    programStageA = createProgramStage('A', 0);
    programStageB = createProgramStage('B', 0);
    programStageB.setRepeatable(true);
    manager.save(programStageA);
    manager.save(programStageB);
    programA = createProgram('A', new HashSet<>(), orgUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programA.setAccessLevel(AccessLevel.PROTECTED);
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.addOrganisationUnit(orgUnitB);
    programA.setTrackedEntityType(trackedEntityType);
    manager.save(programA);
    ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementA);
    programStageDataElement.setProgramStage(programStageA);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageA.getProgramStageDataElements().add(programStageDataElement);
    programStageA.setProgram(programA);
    programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementB);
    programStageDataElement.setProgramStage(programStageB);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageB.getProgramStageDataElements().add(programStageDataElement);
    programStageB.setProgram(programA);
    programStageB.setMinDaysFromStart(2);
    programA.setProgramStages(Set.of(programStageA, programStageB));
    manager.update(programStageA);
    manager.update(programStageB);
    manager.update(programA);
    trackedEntityA = createTrackedEntity(orgUnitA, trackedEntityType);
    TrackedEntity trackedEntityB = createTrackedEntity(orgUnitB, trackedEntityType);
    TrackedEntity femaleA = createTrackedEntity(orgUnitA, trackedEntityType);
    TrackedEntity femaleB = createTrackedEntity(orgUnitB, trackedEntityType);

    manager.save(trackedEntityA);
    manager.save(trackedEntityB);
    manager.save(femaleA);
    manager.save(femaleB);

    Enrollment enrollmentA = createEnrollment(programA, trackedEntityA, orgUnitA);
    manager.save(enrollmentA);
    trackedEntityA.getEnrollments().add(enrollmentA);
    manager.update(trackedEntityA);

    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityA, programA, orgUnitA);

    eventA = new TrackerEvent();
    eventA.setEnrollment(enrollmentA);
    eventA.setProgramStage(programStageA);
    eventA.setOrganisationUnit(orgUnitA);
    eventA.setStatus(EventStatus.COMPLETED);
    eventA.setOccurredDate(new Date());
    eventA.setAttributeOptionCombo(coA);
    manager.save(eventA, false);

    eventB = new TrackerEvent();
    eventB.setEnrollment(enrollmentA);
    eventB.setProgramStage(programStageB);
    eventB.setOrganisationUnit(orgUnitB);
    eventB.setStatus(EventStatus.SCHEDULE);
    eventB.setScheduledDate(DateUtils.addDays(new Date(), 10));
    eventB.setAttributeOptionCombo(coA);
    manager.save(eventB, false);

    User adminUser = getAdminUser();
    adminUser.setTeiSearchOrganisationUnits(Set.of(orgUnitA));
    adminUser.setOrganisationUnits(Set.of(orgUnitA));
    injectSecurityContextUser(adminUser);
  }

  @Test
  void checkAccessPermissionForTeWhenTeOuInCaptureScope()
      throws ForbiddenException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    manager.update(programA);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitA));
    UserDetails userDetails = UserDetails.fromUser(user);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);
    TrackedEntity te = trackedEntityService.getTrackedEntity(UID.of(trackedEntityA));
    // Can read te
    assertNoErrors(trackerAccessManager.canRead(userDetails, te));
    // can write te
    assertNoErrors(trackerAccessManager.canUpdateAndDelete(userDetails, te));
  }

  @Test
  void checkAccessPermissionForTeWhenTeOuInSearchScope()
      throws ForbiddenException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setAccessLevel(AccessLevel.OPEN);
    manager.update(programA);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitB));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);
    TrackedEntity te = trackedEntityService.getTrackedEntity(UID.of(trackedEntityA));
    // Can Read
    assertNoErrors(trackerAccessManager.canRead(userDetails, te));
    // Can write
    assertNoErrors(trackerAccessManager.canUpdateAndDelete(userDetails, te));
  }

  @Test
  void checkAccessPermissionForTeWhenTeOuOutsideSearchScope()
      throws ForbiddenException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setAccessLevel(AccessLevel.OPEN);
    manager.update(programA);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);
    TrackedEntity te = trackedEntityService.getTrackedEntity(UID.of(trackedEntityA));
    // Cannot Read
    assertHasError(trackerAccessManager.canRead(userDetails, te), OWNERSHIP_ACCESS_DENIED);
    // Cannot write
    assertHasError(
        trackerAccessManager.canUpdateAndDelete(userDetails, te), OWNERSHIP_ACCESS_DENIED);
  }

  @Test
  void checkAccessPermissionForTeWhenNoWriteAccessToTet() {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setAccessLevel(AccessLevel.OPEN);
    manager.update(programA);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitB));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.update(trackedEntityType);

    assertNoErrors(trackerAccessManager.canRead(userDetails, trackedEntityA));
    assertHasError(
        trackerAccessManager.canUpdateAndDelete(userDetails, trackedEntityA),
        "User has no data write access to tracked entity type");
  }

  @Test
  void checkAccessPermissionForEnrollmentInClosedProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    manager.update(programA);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitA));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, trackedEntityA.getUid());
    assertNotNull(trackedEntity);
    Enrollment enrollment = trackedEntity.getEnrollments().iterator().next();
    // Can create enrollment
    assertNoErrors(trackerAccessManager.canCreate(userDetails, enrollment, false));
    // Can update enrollment
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, enrollment, false));
    // Cannot delete enrollment
    assertNoErrors(trackerAccessManager.canDelete(userDetails, enrollment, false));
    // Can read enrollment
    assertNoErrors(trackerAccessManager.canRead(userDetails, enrollment, false));
    // Cannot create enrollment if enrollmentOU is outside capture scope
    // even if user is owner.
    enrollment.setOrganisationUnit(orgUnitB);
    assertHasError(
        trackerAccessManager.canCreate(userDetails, enrollment, false),
        "User has no create access to organisation unit:");
    enrollment.setOrganisationUnit(orgUnitA);
    // Transferring ownership to orgUnitB. user is no longer owner
    trackerOwnershipManager.transferOwnership(trackedEntity, UID.of(programA), UID.of(orgUnitB));
    // Cannot create enrollment if not owner
    assertHasError(
        trackerAccessManager.canCreate(userDetails, enrollment, false), "OWNERSHIP_ACCESS_DENIED");
    // Cannot update enrollment if not owner
    assertHasError(
        trackerAccessManager.canUpdate(userDetails, enrollment, false), "OWNERSHIP_ACCESS_DENIED");
    // Cannot delete enrollment if not owner
    assertHasError(
        trackerAccessManager.canDelete(userDetails, enrollment, false), "OWNERSHIP_ACCESS_DENIED");
    // Cannot read enrollment if not owner
    assertHasError(
        trackerAccessManager.canRead(userDetails, enrollment, false), "OWNERSHIP_ACCESS_DENIED");
  }

  @Test
  void checkAccessPermissionForEnrollmentWhenOrgUnitIsNull() {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    manager.update(programA);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitA));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, trackedEntityA.getUid());
    assertNotNull(trackedEntity);
    Enrollment enrollment = trackedEntity.getEnrollments().iterator().next();
    enrollment.setOrganisationUnit(null);
    // Can create enrollment
    assertNoErrors(trackerAccessManager.canCreate(userDetails, enrollment, false));
    // Can update enrollment
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, enrollment, false));
    // Cannot delete enrollment
    assertNoErrors(trackerAccessManager.canDelete(userDetails, enrollment, false));
    // Can read enrollment
    assertNoErrors(trackerAccessManager.canRead(userDetails, enrollment, false));
  }

  @Test
  void checkAccessPermissionForEnrollmentInOpenProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setAccessLevel(AccessLevel.OPEN);
    manager.update(programA);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitB));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, trackedEntityA.getUid());
    assertNotNull(trackedEntity);
    Enrollment enrollment = trackedEntity.getEnrollments().iterator().next();
    // Cannot create enrollment if enrollmentOU falls outside capture scope
    assertHasError(trackerAccessManager.canCreate(userDetails, enrollment, false));
    // Can update enrollment if ownerOU falls inside search scope
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, enrollment, false));
    // Can delete enrollment if ownerOU falls inside search scope
    assertNoErrors(trackerAccessManager.canDelete(userDetails, enrollment, false));
    // Can read enrollment if ownerOU falls inside search scope
    assertNoErrors(trackerAccessManager.canRead(userDetails, enrollment, false));
    // Transferring ownership to orgUnitB. user is now owner
    trackerOwnershipManager.transferOwnership(trackedEntity, UID.of(programA), UID.of(orgUnitB));
    // Cannot create enrollment if enrollmentOU falls outside capture scope,
    // even if user is owner
    assertHasError(
        trackerAccessManager.canCreate(userDetails, enrollment, false),
        "User has no create access to organisation unit:");
    // Can update enrollment
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, enrollment, false));
    // Can delete enrollment
    assertNoErrors(trackerAccessManager.canDelete(userDetails, enrollment, false));
    // Can read enrollment
    assertNoErrors(trackerAccessManager.canRead(userDetails, enrollment, false));
    // Transferring ownership to orgUnitB. user is now owner
    User adminUser = getAdminUser();
    adminUser.setOrganisationUnits(Set.of(orgUnitB));
    injectSecurityContextUser(adminUser);
    trackerOwnershipManager.transferOwnership(trackedEntity, UID.of(programA), UID.of(orgUnitA));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    // Cannot create enrollment if enrollment OU is outside capture scope
    assertHasError(
        trackerAccessManager.canCreate(userDetails, enrollment, false),
        "User has no create access to organisation unit:");
    // Can update enrollment if ownerOU is in search scope
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, enrollment, false));
    // Can delete enrollment if ownerOU is in search scope
    assertNoErrors(trackerAccessManager.canDelete(userDetails, enrollment, false));
    // Can read enrollment if ownerOU is in search scope
    assertNoErrors(trackerAccessManager.canRead(userDetails, enrollment, false));
  }

  @Test
  void checkAccessPermissionsForEventInClosedProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programStageA.setPublicAccess(AccessStringHelper.FULL);
    programStageB.setPublicAccess(AccessStringHelper.FULL);
    manager.update(programStageA);
    manager.update(programStageB);
    manager.update(programA);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitA));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);

    // Can create scheduled events outside capture scope if user is owner
    assertNoErrors(trackerAccessManager.canCreate(userDetails, eventB, false));
    // Cannot create regular events outside capture scope even if user is
    // owner
    eventB.setStatus(EventStatus.ACTIVE);
    assertHasError(
        trackerAccessManager.canCreate(userDetails, eventB, false),
        "User has no create access to organisation unit:");
    // Can read events if user is owner irrespective of eventOU
    assertNoErrors(trackerAccessManager.canRead(userDetails, eventB, false));
    // Can update events if user is owner irrespective of eventOU
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, eventB, false));
    // Can delete events if user is owner irrespective of eventOU
    assertNoErrors(trackerAccessManager.canDelete(userDetails, eventB, false));
    trackerOwnershipManager.transferOwnership(trackedEntityA, UID.of(programA), UID.of(orgUnitB));
    // Cannot create events anywhere if user is not owner
    assertHasErrors(2, trackerAccessManager.canCreate(userDetails, eventB, false));
    // Cannot read events if user is not owner (OwnerOU falls into capture
    // scope)
    assertHasError(
        trackerAccessManager.canRead(userDetails, eventB, false), "OWNERSHIP_ACCESS_DENIED");
    // Cannot update events if user is not owner (OwnerOU falls into capture
    // scope)
    assertHasError(
        trackerAccessManager.canUpdate(userDetails, eventB, false), "OWNERSHIP_ACCESS_DENIED");
    // Cannot delete events if user is not owner (OwnerOU falls into capture
    // scope)
    assertHasError(
        trackerAccessManager.canDelete(userDetails, eventB, false), "OWNERSHIP_ACCESS_DENIED");
  }

  @Test
  void checkAccessPermissionsForEventInOpenProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setAccessLevel(AccessLevel.OPEN);
    programStageA.setPublicAccess(AccessStringHelper.FULL);
    programStageB.setPublicAccess(AccessStringHelper.FULL);
    manager.update(programStageA);
    manager.update(programStageB);
    manager.update(programA);
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnitB));
    user.setTeiSearchOrganisationUnits(Sets.newHashSet(orgUnitA, orgUnitB));
    UserDetails userDetails = UserDetails.fromUser(user);
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    manager.update(trackedEntityType);

    // Cannot create events with event ou outside capture scope
    assertHasError(
        trackerAccessManager.canCreate(userDetails, eventA, false),
        "User has no create access to organisation unit:");
    // Can read events if ownerOu falls into users search scope
    assertNoErrors(trackerAccessManager.canRead(userDetails, eventA, false));
    // Can update events if ownerOu falls into users search scope
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, eventA, false));
    // Can delete events if ownerOu falls into users search scope
    assertNoErrors(trackerAccessManager.canDelete(userDetails, eventA, false));
    trackerOwnershipManager.transferOwnership(trackedEntityA, UID.of(programA), UID.of(orgUnitB));
    // Cannot create events with eventOu outside capture scope, even if
    // ownerOu is
    // also in capture scope
    assertHasError(
        trackerAccessManager.canCreate(userDetails, eventA, false),
        "User has no create access to organisation unit:");
    // Can read events if ownerOu falls into users capture scope
    assertNoErrors(trackerAccessManager.canRead(userDetails, eventA, false));
    // Can update events if ownerOu falls into users capture scope
    assertNoErrors(trackerAccessManager.canUpdate(userDetails, eventA, false));
    // Can delete events if ownerOu falls into users capture scope
    assertNoErrors(trackerAccessManager.canDelete(userDetails, eventA, false));
  }

  private void assertNoErrors(List<String> errors) {
    assertIsEmpty(errors);
  }

  private void assertHasError(List<String> errors, String error) {
    assertFalse(errors.isEmpty(), "error not found since there are no errors");
    assertAll(
        () ->
            assertEquals(
                1,
                errors.size(),
                String.format(
                    "mismatch in number of expected error(s), want 1, got %d: %s",
                    errors.size(), errors)),
        () ->
            assertTrue(
                errors.stream().anyMatch(err -> err.contains(error)),
                String.format("error '%s' not found in error(s) %s", error, errors)));
  }

  private void assertHasError(List<String> errors) {
    assertEquals(1, errors.size());
  }

  private void assertHasErrors(int errorNumber, List<String> errors) {
    assertEquals(errorNumber, errors.size());
  }
}
