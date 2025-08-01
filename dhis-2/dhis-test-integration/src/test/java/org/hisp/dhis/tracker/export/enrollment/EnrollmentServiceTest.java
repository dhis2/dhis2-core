/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourBefore;
import static org.hisp.dhis.tracker.TrackerTestUtils.uids;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.test.utils.RelationshipUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.export.relationship.RelationshipFields;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventFields;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EnrollmentServiceTest extends PostgresIntegrationTestBase {

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired protected UserService _userService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private final Date occurredDate = new Date();

  private User admin;

  private User userWithOrgUnitZ;

  private User authorizedUser;

  private Program programA;

  private ProgramStage programStageA;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Enrollment enrollmentChildA;

  private Enrollment enrollmentGrandchildA;

  private TrackerEvent eventA;

  private TrackedEntity trackedEntityA;

  private TrackedEntityType trackedEntityTypeA;

  private TrackedEntityAttribute trackedEntityAttributeA;

  private RelationshipType relationshipTypeA;

  private Relationship relationshipA;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitChildA;

  private User user;

  @BeforeEach
  void setUp() {
    admin = getAdminUser();

    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);
    OrganisationUnit orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitB, false);
    OrganisationUnit orgUnitC = createOrganisationUnit('C');
    manager.save(orgUnitC, false);
    orgUnitChildA = createOrganisationUnit('D', orgUnitA);
    manager.save(orgUnitChildA, false);
    OrganisationUnit orgUnitGrandchildA = createOrganisationUnit('E', orgUnitChildA);
    manager.save(orgUnitGrandchildA, false);
    OrganisationUnit orgUnitZ = createOrganisationUnit('Z');
    manager.save(orgUnitZ, false);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "F_EXPORT_DATA");
    user.setTeiSearchOrganisationUnits(Set.of(orgUnitA, orgUnitB, orgUnitC));
    userWithOrgUnitZ = createUserWithAuth("userWithoutOrgUnit");
    userWithOrgUnitZ.setTeiSearchOrganisationUnits(Set.of(orgUnitZ));
    authorizedUser =
        createAndAddUser(
            false,
            "test user",
            Set.of(orgUnitA),
            Set.of(orgUnitA),
            "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS");

    trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeA.getSharing().setOwner(user);
    manager.save(trackedEntityTypeA, false);

    trackedEntityA = createTrackedEntity(orgUnitA, trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    TrackedEntity trackedEntityB = createTrackedEntity(orgUnitB, trackedEntityTypeA);
    manager.save(trackedEntityB, false);

    TrackedEntity trackedEntityC = createTrackedEntity(orgUnitC, trackedEntityTypeA);
    manager.save(trackedEntityC, false);

    TrackedEntity trackedEntityChildA = createTrackedEntity(orgUnitChildA, trackedEntityTypeA);
    manager.save(trackedEntityChildA, false);

    TrackedEntity trackedEntityGrandchildA =
        createTrackedEntity(orgUnitGrandchildA, trackedEntityTypeA);
    manager.save(trackedEntityGrandchildA, false);

    programA = createProgram('A', new HashSet<>(), orgUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programA.setTrackedEntityType(trackedEntityTypeA);
    programA.getSharing().setOwner(admin);
    programA.getSharing().setPublicAccess(AccessStringHelper.READ_ONLY);
    manager.save(programA, false);

    Program programB = createProgram('B', new HashSet<>(), orgUnitB);
    programB.setProgramType(ProgramType.WITH_REGISTRATION);
    programB.setTrackedEntityType(trackedEntityTypeA);
    programB.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(programB, false);

    trackedEntityAttributeA = createTrackedEntityAttribute('A');
    trackedEntityAttributeA.getSharing().setOwner(admin);
    manager.save(trackedEntityAttributeA, false);
    TrackedEntityAttributeValue trackedEntityAttributeValueA = new TrackedEntityAttributeValue();
    trackedEntityAttributeValueA.setAttribute(trackedEntityAttributeA);
    trackedEntityAttributeValueA.setTrackedEntity(trackedEntityA);
    trackedEntityAttributeValueA.setValue("12");
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(trackedEntityAttributeValueA);
    trackedEntityA.setTrackedEntityAttributeValues(Set.of(trackedEntityAttributeValueA));
    manager.update(trackedEntityA);

    programA
        .getProgramAttributes()
        .add(createProgramTrackedEntityAttribute(programA, trackedEntityAttributeA));
    manager.update(programA);

    programB
        .getProgramAttributes()
        .add(createProgramTrackedEntityAttribute(programB, trackedEntityAttributeA));
    manager.update(programB);

    programStageA = createProgramStage('A', programA);
    manager.save(programStageA, false);
    ProgramStage inaccessibleProgramStage = createProgramStage('B', programA);
    inaccessibleProgramStage.getSharing().setOwner(admin);
    inaccessibleProgramStage.setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(inaccessibleProgramStage, false);
    programA.setProgramStages(Set.of(programStageA, inaccessibleProgramStage));
    manager.save(programA, false);
    programB.setProgramStages(Set.of(programStageA, inaccessibleProgramStage));
    manager.save(programB, false);

    relationshipTypeA = createRelationshipType('A');
    relationshipTypeA
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeA.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeA.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    relationshipTypeA.getToConstraint().setProgram(programA);
    relationshipTypeA.getSharing().setOwner(user);
    manager.save(relationshipTypeA, false);

    enrollmentA = createEnrollment(programA, trackedEntityA, orgUnitA);
    manager.save(enrollmentA, false);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityA, programA, orgUnitA);

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

    eventA = createEvent(programStageA, enrollmentA, orgUnitA);
    eventA.setOccurredDate(occurredDate);
    manager.save(eventA);
    enrollmentA.setEvents(Set.of(eventA));
    enrollmentA.setRelationshipItems(Set.of(to));
    manager.update(enrollmentA);

    enrollmentB = createEnrollment(programB, trackedEntityB, orgUnitB);
    manager.save(enrollmentB);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programB, orgUnitB);

    enrollmentChildA = createEnrollment(programA, trackedEntityChildA, orgUnitChildA);
    manager.save(enrollmentChildA);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityChildA, programA, orgUnitChildA);

    enrollmentGrandchildA =
        createEnrollment(programA, trackedEntityGrandchildA, orgUnitGrandchildA);
    manager.save(enrollmentGrandchildA);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityGrandchildA, programA, orgUnitGrandchildA);

    manager.flush();
    manager.clear();

    injectSecurityContextUser(user);
  }

  @Test
  void shouldGetEnrollmentWhenUserHasWriteAccessToProgramAndAccessToOrgUnit()
      throws NotFoundException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    manager.updateNoAcl(programA);

    Enrollment enrollment =
        enrollmentService.getEnrollment(UID.of(enrollmentA), EnrollmentFields.none());

    assertNotNull(enrollment);
    assertEquals(enrollmentA.getUid(), enrollment.getUid());
  }

  @Test
  void shouldGetEnrollmentWhenUserHasReadAccessToProgramAndAccessToOrgUnit()
      throws NotFoundException {
    programA.getSharing().setPublicAccess(AccessStringHelper.READ_ONLY);
    manager.updateNoAcl(programA);

    Enrollment enrollment = enrollmentService.getEnrollment(UID.of(enrollmentA));

    assertNotNull(enrollment);
    assertEquals(enrollmentA.getUid(), enrollment.getUid());
  }

  @Test
  void shouldGetEnrollmentWithEventsWhenUserHasAccessToEvent() throws NotFoundException {
    EnrollmentFields fields =
        EnrollmentFields.builder().includeEvents(TrackerEventFields.all()).build();

    Enrollment enrollment = enrollmentService.getEnrollment(UID.of(enrollmentA), fields);

    assertNotNull(enrollment);
    assertContainsOnly(
        List.of(eventA.getUid()),
        enrollment.getEvents().stream().map(TrackerEvent::getUid).toList());
  }

  @Test
  void shouldGetEnrollmentWithoutEventsWhenUserHasNoAccessToProgramStage()
      throws NotFoundException {
    programStageA.getSharing().setOwner(admin);
    programStageA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(programStageA);

    EnrollmentFields fields =
        EnrollmentFields.builder().includeEvents(TrackerEventFields.all()).build();

    Enrollment enrollment = enrollmentService.getEnrollment(UID.of(enrollmentA), fields);

    assertNotNull(enrollment);
    assertIsEmpty(enrollment.getEvents());
  }

  @Test
  void shouldGetEnrollmentWithRelationshipsWhenUserHasAccessToThem() throws NotFoundException {
    EnrollmentFields fields =
        EnrollmentFields.builder().includeRelationships(RelationshipFields.all()).build();

    Enrollment enrollment = enrollmentService.getEnrollment(UID.of(enrollmentA), fields);

    assertNotNull(enrollment);
    assertContainsOnly(Set.of(relationshipA.getUid()), relationshipUids(enrollment));
  }

  @Test
  void shouldGetEnrollmentWithoutRelationshipsWhenUserHasAccessToThem() throws NotFoundException {
    injectSecurityContextUser(admin);
    relationshipTypeA.getSharing().setOwner(admin);
    relationshipTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.update(relationshipTypeA);
    manager.flush();
    manager.clear();
    injectSecurityContextUser(user);

    EnrollmentFields fields =
        EnrollmentFields.builder().includeRelationships(RelationshipFields.all()).build();

    Enrollment enrollment = enrollmentService.getEnrollment(UID.of(enrollmentA), fields);

    assertNotNull(enrollment);
    assertIsEmpty(enrollment.getRelationshipItems());
  }

  @Test
  void shouldGetEnrollmentWithAttributesWhenUserHasAccessToThem() throws NotFoundException {
    EnrollmentFields fields = EnrollmentFields.builder().includeAttributes().build();

    Enrollment enrollment = enrollmentService.getEnrollment(UID.of(enrollmentA), fields);

    assertNotNull(enrollment);
    assertContainsOnly(List.of(trackedEntityAttributeA.getUid()), attributeUids(enrollment));
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasNoAccessToProgramsTrackedEntityType() {
    trackedEntityTypeA.getSharing().setOwner(admin);
    trackedEntityTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(trackedEntityTypeA);
    manager.flush();
    manager.clear();

    assertFalse(enrollmentService.findEnrollment(UID.of(enrollmentA)).isPresent());
  }

  @Test
  void shouldFailGettingEnrollmentWhenDoesNotExist() {
    trackedEntityTypeA.getSharing().setOwner(admin);
    trackedEntityTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(trackedEntityTypeA);

    UID nonExistentUid = UID.generate();

    assertFalse(enrollmentService.findEnrollment(nonExistentUid).isPresent());
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasReadAccessToProgramButNoAccessToOrgUnit() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ);
    manager.updateNoAcl(programA);

    injectSecurityContextUser(userWithOrgUnitZ);

    assertFalse(enrollmentService.findEnrollment(UID.of(enrollmentA)).isPresent());
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasReadWriteAccessToProgramButNoAccessToOrgUnit() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DATA_READ_WRITE);
    manager.updateNoAcl(programA);

    injectSecurityContextUser(userWithOrgUnitZ);

    assertFalse(enrollmentService.findEnrollment(UID.of(enrollmentA)).isPresent());
  }

  @Test
  void shouldFailGettingEnrollmentWhenUserHasNoAccessToProgramButAccessToOrgUnit() {
    programA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.updateNoAcl(programA);
    manager.flush();
    manager.clear();

    assertFalse(enrollmentService.findEnrollment(UID.of(enrollmentA)).isPresent());
  }

  @Test
  void shouldGetEnrollmentsWhenUserHasReadAccessToProgramAndSearchScopeAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);

    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .program(programA)
            .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(
        List.of(enrollmentA.getUid(), enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()),
        uids(enrollments));
  }

  @Test
  void shouldGetEnrollmentsWhenUserHasReadAccessToProgramAndNoOrgUnitNorOrgUnitModeSpecified()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);

    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder().program(programA).orgUnitMode(ACCESSIBLE).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(
        List.of(enrollmentA.getUid(), enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()),
        uids(enrollments));
  }

  @Test
  void shouldGetEnrollmentsInCaptureScopeIfOrgUnitModeCapture()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);

    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder().orgUnitMode(CAPTURE).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(
        List.of(enrollmentA.getUid(), enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()),
        uids(enrollments));
  }

  @Test
  void shouldGetEnrollmentWhenEnrollmentsAndOtherParamsAreSpecified()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);

    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .program(programA)
            .enrollments(enrollmentA)
            .orgUnitMode(ACCESSIBLE)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(List.of(enrollmentA.getUid()), uids(enrollments));
  }

  @Test
  void shouldGetEnrollmentsByTrackedEntityWhenUserHasAccessToTrackedEntityType()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.READ_ONLY);
    manager.updateNoAcl(programA);

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnits(trackedEntityA.getOrganisationUnit())
            .orgUnitMode(SELECTED)
            .trackedEntity(trackedEntityA)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(params);

    assertNotNull(enrollments);
    assertContainsOnly(List.of(enrollmentA.getUid()), uids(enrollments));
  }

  @Test
  void shouldReturnEnrollmentIfEnrollmentWasUpdatedBeforePassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    Date oneHourBeforeLastUpdated = oneHourBefore(enrollmentA.getLastUpdated());

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .lastUpdated(oneHourBeforeLastUpdated)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);

    assertContainsOnly(List.of(enrollmentA), enrollments);
  }

  @Test
  void shouldReturnEmptyIfEnrollmentWasUpdatedAfterPassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    Date oneHourAfterLastUpdated = oneHourAfter(enrollmentA.getLastUpdated());

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .lastUpdated(oneHourAfterLastUpdated)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);

    assertIsEmpty(enrollments);
  }

  @Test
  void shouldReturnEnrollmentIfEnrollmentStartedBeforePassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    Date oneHourBeforeEnrollmentDate = oneHourBefore(enrollmentA.getEnrollmentDate());

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programA)
            .programStartDate(oneHourBeforeEnrollmentDate)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);

    assertContainsOnly(List.of(enrollmentA), enrollments);
  }

  @Test
  void shouldReturnEmptyIfEnrollmentStartedAfterPassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    Date oneHourAfterEnrollmentDate = oneHourAfter(enrollmentA.getEnrollmentDate());

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programA)
            .programStartDate(oneHourAfterEnrollmentDate)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);

    assertIsEmpty(enrollments);
  }

  @Test
  void shouldReturnEnrollmentIfEnrollmentEndedAfterPassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    Date oneHourAfterEnrollmentDate = oneHourAfter(enrollmentA.getEnrollmentDate());

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programA)
            .programEndDate(oneHourAfterEnrollmentDate)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);

    assertContainsOnly(List.of(enrollmentA), enrollments);
  }

  @Test
  void shouldReturnEmptyIfEnrollmentEndedBeforePassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    Date oneHourBeforeEnrollmentDate = oneHourBefore(enrollmentA.getEnrollmentDate());

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programA)
            .programEndDate(oneHourBeforeEnrollmentDate)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);

    assertIsEmpty(enrollments);
  }

  @Test
  void shouldReturnAllAccessibleEnrollmentsInTheSystemWhenModeAllAndUserAuthorized()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(authorizedUser);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ALL).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);
    assertContainsOnly(
        List.of(enrollmentA.getUid(), enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()),
        uids(enrollments));
  }

  @Test
  void shouldFailWhenOrgUnitModeAllAndUserNotAuthorized() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ALL).build();

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class, () -> enrollmentService.findEnrollments(operationParams));
    assertEquals(
        "User is not authorized to query across all organisation units", exception.getMessage());
  }

  @Test
  void shouldFailWhenUserCanSearchEverywhereModeDescendantsAndOrgUnitNotInSearchScope() {
    injectSecurityContextUser(userWithOrgUnitZ);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(DESCENDANTS).orgUnits(orgUnitA).build();

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class, () -> enrollmentService.findEnrollments(operationParams));
    assertEquals(
        String.format("Organisation unit is not part of the search scope: %s", orgUnitA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldReturnAllEnrollmentsWhenOrgUnitModeAllAndUserAuthorized()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(admin);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ALL).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);
    assertContainsOnly(
        List.of(enrollmentA, enrollmentB, enrollmentChildA, enrollmentGrandchildA), enrollments);
  }

  @Test
  void shouldReturnAllDescendantsOfSelectedOrgUnitWhenOrgUnitModeDescendants()
      throws ForbiddenException, BadRequestException {

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnits(orgUnitA).orgUnitMode(DESCENDANTS).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);
    assertContainsOnly(
        List.of(enrollmentA.getUid(), enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()),
        uids(enrollments));
  }

  @Test
  void shouldReturnChildrenOfRootOrgUnitWhenOrgUnitModeChildren()
      throws ForbiddenException, BadRequestException {

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnits(orgUnitA).orgUnitMode(CHILDREN).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);
    assertContainsOnly(List.of(enrollmentA.getUid(), enrollmentChildA.getUid()), uids(enrollments));
  }

  @Test
  void shouldReturnChildrenOfRequestedOrgUnitWhenOrgUnitModeChildren()
      throws ForbiddenException, BadRequestException {

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnits(orgUnitChildA).orgUnitMode(CHILDREN).build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);
    assertContainsOnly(
        List.of(enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()), uids(enrollments));
  }

  @Test
  void
      shouldReturnAllChildrenOfRequestedOrgUnitsWhenOrgUnitModeChildrenAndMultipleOrgUnitsRequested()
          throws ForbiddenException, BadRequestException {

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnitA, orgUnitChildA)
            .orgUnitMode(CHILDREN)
            .build();

    List<Enrollment> enrollments = enrollmentService.findEnrollments(operationParams);
    assertContainsOnly(
        List.of(enrollmentA.getUid(), enrollmentChildA.getUid(), enrollmentGrandchildA.getUid()),
        uids(enrollments));
  }

  @Test
  void shouldReturnEmptyListWhenRequestingEnrollmentsAndTheyAreNotAccessible()
      throws ForbiddenException {
    injectSecurityContextUser(admin);
    programA.getSharing().setPublicAccess("rw------");
    manager.update(programA);
    manager.flush();
    manager.clear();

    injectSecurityContextUser(authorizedUser);
    List<Enrollment> enrollments =
        enrollmentService.findEnrollments(UID.of(enrollmentA, enrollmentB));
    assertIsEmpty(enrollments);
  }

  @Test
  void shouldNotDeleteNoteWhenDeletingEnrollment() {
    Note note = new Note();
    note.setCreator(CodeGenerator.generateUid());
    note.setNoteText("text");
    manager.save(note);
    enrollmentA.getNotes().add(note);

    manager.save(enrollmentA);

    assertTrue(enrollmentService.findEnrollment(UID.of(enrollmentA)).isPresent());

    manager.delete(enrollmentA);
    manager.flush();
    manager.clear();

    assertFalse(enrollmentService.findEnrollment(UID.of(enrollmentA)).isPresent());
    assertTrue(manager.exists(Note.class, note.getUid()));
  }

  @Test
  void shouldReturnEnrollmentWithGeometry() {
    GeometryFactory geometryFactory = new GeometryFactory();
    Point point = geometryFactory.createPoint(new Coordinate(13.4050, 52.5200));

    enrollmentA.setGeometry(point);
    manager.update(enrollmentA);
    manager.flush();
    manager.clear();

    Optional<Enrollment> enrollment = enrollmentService.findEnrollment(UID.of(enrollmentA));

    assertTrue(enrollment.isPresent());
    assertEquals(enrollmentA.getGeometry(), enrollment.get().getGeometry());
  }

  private static List<String> attributeUids(Enrollment enrollment) {
    return enrollment.getTrackedEntity().getTrackedEntityAttributeValues().stream()
        .map(v -> v.getAttribute().getUid())
        .toList();
  }

  private static Set<String> relationshipUids(Enrollment enrollment) {
    return enrollment.getRelationshipItems().stream()
        .map(r -> r.getRelationship().getUid())
        .collect(Collectors.toSet());
  }
}
