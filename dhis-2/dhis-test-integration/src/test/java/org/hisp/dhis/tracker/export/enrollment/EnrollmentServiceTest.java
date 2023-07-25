/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.tracker.Assertions.assertSlimPager;
import static org.hisp.dhis.tracker.TrackerTestUtils.uids;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams.EnrollmentOperationParamsBuilder;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentServiceTest extends TransactionalIntegrationTest {
  @Autowired private org.hisp.dhis.tracker.export.enrollment.EnrollmentService enrollmentService;

  @Autowired protected UserService _userService;

  @Autowired private EnrollmentService programInstanceService;

  @Autowired private IdentifiableObjectManager manager;

  private User admin;

  private User user;

  private User userWithoutOrgUnit;

  private Program programA;

  private ProgramStage programStageA;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Event eventA;

  private TrackedEntity trackedEntityA;

  private TrackedEntityType trackedEntityTypeA;

  private TrackedEntityAttribute trackedEntityAttributeA;

  private RelationshipType relationshipTypeA;

  private Relationship relationshipA;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;
    admin = preCreateInjectAdminUser();

    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);
    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitB, false);
    OrganisationUnit orgUnitC = createOrganisationUnit('C');
    manager.save(orgUnitC, false);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "F_EXPORT_DATA");
    user.setTeiSearchOrganisationUnits(Set.of(orgUnitA, orgUnitB, orgUnitC));
    userWithoutOrgUnit = createUserWithAuth("userWithoutOrgUnit");

    trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeA.getSharing().setOwner(user);
    manager.save(trackedEntityTypeA, false);

    trackedEntityA = createTrackedEntity(orgUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    TrackedEntity trackedEntityB = createTrackedEntity(orgUnitB);
    trackedEntityB.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityB, false);

    TrackedEntity trackedEntityC = createTrackedEntity(orgUnitC);
    trackedEntityC.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityC, false);

    programA = createProgram('A', new HashSet<>(), orgUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programA.setTrackedEntityType(trackedEntityTypeA);
    programA.getSharing().setOwner(admin);
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.save(programA, false);

    trackedEntityAttributeA = createTrackedEntityAttribute('A');
    trackedEntityAttributeA.getSharing().setOwner(admin);
    manager.save(trackedEntityAttributeA, false);
    TrackedEntityAttributeValue trackedEntityAttributeValueA = new TrackedEntityAttributeValue();
    trackedEntityAttributeValueA.setAttribute(trackedEntityAttributeA);
    trackedEntityAttributeValueA.setTrackedEntity(trackedEntityA);
    trackedEntityAttributeValueA.setValue("12");
    trackedEntityA.setTrackedEntityAttributeValues(Set.of(trackedEntityAttributeValueA));
    manager.update(trackedEntityA);
    programA.setProgramAttributes(
        List.of(createProgramTrackedEntityAttribute(programA, trackedEntityAttributeA)));
    manager.update(programA);

    programStageA = createProgramStage('A', programA);
    manager.save(programStageA, false);
    ProgramStage inaccessibleProgramStage = createProgramStage('B', programA);
    inaccessibleProgramStage.getSharing().setOwner(admin);
    inaccessibleProgramStage.setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(inaccessibleProgramStage, false);
    programA.setProgramStages(Set.of(programStageA, inaccessibleProgramStage));
    manager.save(programA, false);

    relationshipTypeA = createRelationshipType('A');
    relationshipTypeA
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeA.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeA.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    relationshipTypeA.getToConstraint().setProgram(programA);
    relationshipTypeA.getSharing().setOwner(user);
    manager.save(relationshipTypeA, false);

    relationshipA = new Relationship();
    relationshipA.setUid(CodeGenerator.generateUid());
    relationshipA.setRelationshipType(relationshipTypeA);
    RelationshipItem from = new RelationshipItem();
    from.setTrackedEntity(trackedEntityA);
    from.setRelationship(relationshipA);
    relationshipA.setFrom(from);
    RelationshipItem to = new RelationshipItem();
    to.setEnrollment(enrollmentA);
    to.setRelationship(relationshipA);
    relationshipA.setTo(to);
    relationshipA.setKey(RelationshipUtils.generateRelationshipKey(relationshipA));
    relationshipA.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationshipA));
    manager.save(relationshipA, false);

    enrollmentA =
        programInstanceService.enrollTrackedEntity(
            trackedEntityA, programA, new Date(), new Date(), orgUnitA);
    eventA = new Event();
    eventA.setEnrollment(enrollmentA);
    eventA.setProgramStage(programStageA);
    eventA.setOrganisationUnit(orgUnitA);
    manager.save(eventA, false);
    enrollmentA.setEvents(Set.of(eventA));
    enrollmentA.setRelationshipItems(Set.of(from, to));
    manager.save(enrollmentA, false);

    enrollmentB =
        programInstanceService.enrollTrackedEntity(
            trackedEntityB, programA, new Date(), new Date(), orgUnitB);

    injectSecurityContext(user);
  }

  @Test
  void shouldGetEnrollmentWhenUserHasReadWriteAccessToProgramAndAccessToOrgUnit()
      throws ForbiddenException, NotFoundException {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ_WRITE);
    manager.updateNoAcl(programA);

    Enrollment enrollment =
        enrollmentService.getEnrollment(enrollmentA.getUid(), EnrollmentParams.FALSE, false);

    assertNotNull(enrollment);
    assertEquals(enrollmentA.getUid(), enrollment.getUid());
  }

  @Test
  void shouldGetEnrollmentWhenUserHasReadAccessToProgramAndAccessToOrgUnit()
      throws ForbiddenException, NotFoundException {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.updateNoAcl(programA);

    Enrollment enrollment =
        enrollmentService.getEnrollment(enrollmentA.getUid(), EnrollmentParams.FALSE, false);

    assertNotNull(enrollment);
    assertEquals(enrollmentA.getUid(), enrollment.getUid());
  }

  @Test
  void shouldGetEnrollmentWithEventsWhenUserHasAccessToEvent()
      throws ForbiddenException, NotFoundException {
    EnrollmentParams params = EnrollmentParams.FALSE;
    params = params.withEnrollmentEventsParams(EnrollmentEventsParams.TRUE);

    Enrollment enrollment = enrollmentService.getEnrollment(enrollmentA.getUid(), params, false);

    assertNotNull(enrollment);
    assertContainsOnly(
        List.of(eventA.getUid()),
        enrollment.getEvents().stream().map(Event::getUid).collect(Collectors.toList()));
  }

  @Test
  void shouldGetEnrollmentWithoutEventsWhenUserHasNoAccessToProgramStage()
      throws ForbiddenException, NotFoundException {
    programStageA.getSharing().setOwner(admin);
    programStageA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(programStageA);

    EnrollmentParams params = EnrollmentParams.FALSE;
    params = params.withIncludeEvents(true);

    Enrollment enrollment = enrollmentService.getEnrollment(enrollmentA.getUid(), params, false);

    assertNotNull(enrollment);
    assertIsEmpty(enrollment.getEvents());
  }

  @Test
  void shouldGetEnrollmentWithRelationshipsWhenUserHasAccessToThem()
      throws ForbiddenException, NotFoundException {
    EnrollmentParams params = EnrollmentParams.FALSE;
    params = params.withIncludeRelationships(true);

    Enrollment enrollment = enrollmentService.getEnrollment(enrollmentA.getUid(), params, false);

    assertNotNull(enrollment);
    assertContainsOnly(Set.of(relationshipA.getUid()), relationshipUids(enrollment));
  }

  @Test
  void shouldGetEnrollmentWithoutRelationshipsWhenUserHasAccessToThem()
      throws ForbiddenException, NotFoundException {
    relationshipTypeA.getSharing().setOwner(admin);
    relationshipTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);

    EnrollmentParams params = EnrollmentParams.FALSE;
    params = params.withIncludeRelationships(true);

    Enrollment enrollment = enrollmentService.getEnrollment(enrollmentA.getUid(), params, false);

    assertNotNull(enrollment);
    assertIsEmpty(enrollment.getRelationshipItems());
  }

  @Test
  void shouldGetEnrollmentWithAttributesWhenUserHasAccessToThem()
      throws ForbiddenException, NotFoundException {
    EnrollmentParams params = EnrollmentParams.FALSE;
    params = params.withIncludeAttributes(true);

    Enrollment enrollment = enrollmentService.getEnrollment(enrollmentA.getUid(), params, false);

    assertNotNull(enrollment);
    assertContainsOnly(List.of(trackedEntityAttributeA.getUid()), attributeUids(enrollment));
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasNoAccessToProgramsTrackedEntityType() {
    trackedEntityTypeA.getSharing().setOwner(admin);
    trackedEntityTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(trackedEntityTypeA);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                enrollmentService.getEnrollment(
                    enrollmentA.getUid(), EnrollmentParams.FALSE, false));
    assertContains("access to tracked entity type", exception.getMessage());
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasReadAccessToProgramButNoAccessToOrgUnit() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.updateNoAcl(programA);

    injectSecurityContext(userWithoutOrgUnit);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                enrollmentService.getEnrollment(
                    enrollmentA.getUid(), EnrollmentParams.FALSE, false));
    assertContains("OWNERSHIP_ACCESS_DENIED", exception.getMessage());
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasReadWriteAccessToProgramButNoAccessToOrgUnit() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ_WRITE);
    manager.updateNoAcl(programA);

    injectSecurityContext(userWithoutOrgUnit);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                enrollmentService.getEnrollment(
                    enrollmentA.getUid(), EnrollmentParams.FALSE, false));
    assertContains("OWNERSHIP_ACCESS_DENIED", exception.getMessage());
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasNoAccessToProgramButAccessToOrgUnit() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(programA);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                enrollmentService.getEnrollment(
                    enrollmentA.getUid(), EnrollmentParams.FALSE, false));
    assertContains("access to program", exception.getMessage());
  }

  @Test
  void shouldGetEnrollmentsWhenUserHasReadAccessToProgramAndSearchScopeAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);

    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .programUid(programA.getUid())
            .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
            .build();

    Enrollments enrollments = enrollmentService.getEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(List.of(enrollmentA.getUid(), enrollmentB.getUid()), toUid(enrollments));
  }

  @Test
  void shouldGetEnrollmentsByTrackedEntityWhenUserHasAccessToTrackedEntityType()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(trackedEntityA.getOrganisationUnit().getUid()))
            .trackedEntityUid(trackedEntityA.getUid())
            .build();

    Enrollments enrollments = enrollmentService.getEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(List.of(enrollmentA.getUid()), toUid(enrollments));
  }

  @Test
  void shouldFailGettingEnrollmentsByTrackedEntityWhenUserHasNoAccessToTrackedEntityType() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.updateNoAcl(programA);

    trackedEntityTypeA.getSharing().setOwner(admin);
    trackedEntityTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(trackedEntityTypeA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(trackedEntityA.getOrganisationUnit().getUid()))
            .trackedEntityUid(trackedEntityA.getUid())
            .build();

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> enrollmentService.getEnrollments(params));
    assertContains("access to tracked entity type", exception.getMessage());
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {

    EnrollmentOperationParamsBuilder builder =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(orgUnitA.getUid(), orgUnitB.getUid()))
            .order(List.of(new OrderParam("created", SortDirection.ASC)));

    EnrollmentOperationParams params = builder.page(1).pageSize(1).build();

    Enrollments firstPage = enrollmentService.getEnrollments(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage.getPager()),
        () -> assertEquals(List.of(enrollmentA.getUid()), uids(firstPage.getEnrollments())));

    params = builder.page(2).pageSize(1).build();

    Enrollments secondPage = enrollmentService.getEnrollments(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage.getPager()),
        () -> assertEquals(List.of(enrollmentB.getUid()), uids(secondPage.getEnrollments())));

    params = builder.page(3).pageSize(1).build();

    assertIsEmpty(uids(enrollmentService.getEnrollments(params).getEnrollments()));
  }

  private static List<String> toUid(Enrollments enrollments) {
    return enrollments.getEnrollments().stream()
        .map(Enrollment::getUid)
        .collect(Collectors.toList());
  }

  private static List<String> attributeUids(Enrollment enrollment) {
    return enrollment.getTrackedEntity().getTrackedEntityAttributeValues().stream()
        .map(v -> v.getAttribute().getUid())
        .collect(Collectors.toList());
  }

  private static Set<String> relationshipUids(Enrollment enrollment) {
    return enrollment.getRelationshipItems().stream()
        .map(r -> r.getRelationship().getUid())
        .collect(Collectors.toSet());
  }
}
