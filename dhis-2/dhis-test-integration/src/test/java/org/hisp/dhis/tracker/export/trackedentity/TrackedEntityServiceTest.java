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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityServiceTest extends IntegrationTestBase {
  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired protected UserService _userService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private EventService eventService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  @Autowired private CurrentUserService currentUserService;

  private User user;

  private User admin;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private TrackedEntityAttribute teaA;

  private TrackedEntityType trackedEntityTypeA;

  private Program programA;

  private Program programB;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Event eventA;

  private Event eventB;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntityComment note1;

  private CategoryOptionCombo defaultCategoryOptionCombo;

  private Relationship relationshipA;

  private Relationship relationshipB;

  private Relationship relationshipC;

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

    teaA = createTrackedEntityAttribute('A', ValueType.TEXT);
    manager.save(teaA, false);
    TrackedEntityAttribute teaB = createTrackedEntityAttribute('B', ValueType.TEXT);
    manager.save(teaB, false);
    TrackedEntityAttribute teaC = createTrackedEntityAttribute('C', ValueType.TEXT);
    manager.save(teaC, false);
    TrackedEntityAttribute teaD = createTrackedEntityAttribute('D', ValueType.TEXT);
    manager.save(teaD, false);
    TrackedEntityAttribute teaE = createTrackedEntityAttribute('E', ValueType.TEXT);
    manager.save(teaE, false);

    trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeA
        .getTrackedEntityTypeAttributes()
        .addAll(
            List.of(
                new TrackedEntityTypeAttribute(trackedEntityTypeA, teaA),
                new TrackedEntityTypeAttribute(trackedEntityTypeA, teaB)));
    trackedEntityTypeA.getSharing().setOwner(user);
    trackedEntityTypeA.setPublicAccess(AccessStringHelper.FULL);
    manager.save(trackedEntityTypeA, false);

    CategoryCombo defaultCategoryCombo = manager.getByName(CategoryCombo.class, "default");
    assertNotNull(defaultCategoryCombo);
    defaultCategoryOptionCombo = manager.getByName(CategoryOptionCombo.class, "default");
    assertNotNull(defaultCategoryOptionCombo);

    programA = createProgram('A', new HashSet<>(), orgUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programA.setTrackedEntityType(trackedEntityTypeA);
    programA.setCategoryCombo(defaultCategoryCombo);
    manager.save(programA, false);
    ProgramStage programStageA1 = createProgramStage(programA);
    programStageA1.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStageA1, false);
    ProgramStage programStageA2 = createProgramStage(programA);
    programStageA2.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStageA2, false);
    programA.setProgramStages(
        Stream.of(programStageA1, programStageA2).collect(Collectors.toCollection(HashSet::new)));
    programA.getSharing().setOwner(admin);
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    programA.getProgramAttributes().add(new ProgramTrackedEntityAttribute(programA, teaC));
    manager.update(programA);

    User currentUser = currentUserService.getCurrentUser();

    programB = createProgram('B', new HashSet<>(), orgUnitA);
    programB.setProgramType(ProgramType.WITH_REGISTRATION);
    programB.setTrackedEntityType(trackedEntityTypeA);
    programB.setCategoryCombo(defaultCategoryCombo);
    programB.setAccessLevel(AccessLevel.PROTECTED);
    programB
        .getSharing()
        .addUserAccess(
            new org.hisp.dhis.user.sharing.UserAccess(currentUser, AccessStringHelper.FULL));
    manager.save(programB, false);
    ProgramStage programStageB1 = createProgramStage(programB);
    programStageB1.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStageB1, false);
    ProgramStage programStageB2 = createProgramStage(programB);
    programStageB2.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStageB2, false);
    programB.setProgramStages(
        Stream.of(programStageB1, programStageB2).collect(Collectors.toCollection(HashSet::new)));
    programB.getSharing().setOwner(admin);
    programB.getSharing().setPublicAccess(AccessStringHelper.FULL);
    manager.update(programB);

    programB
        .getProgramAttributes()
        .addAll(
            List.of(
                new ProgramTrackedEntityAttribute(programB, teaD),
                new ProgramTrackedEntityAttribute(programB, teaE)));
    manager.update(programB);

    trackedEntityA = createTrackedEntity(orgUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    enrollmentA =
        enrollmentService.enrollTrackedEntity(
            trackedEntityA, programA, new Date(), new Date(), orgUnitA);
    eventA = new Event();
    eventA.setEnrollment(enrollmentA);
    eventA.setProgramStage(programStageA1);
    eventA.setOrganisationUnit(orgUnitA);
    eventA.setAttributeOptionCombo(defaultCategoryOptionCombo);
    eventA.setDueDate(parseDate("2021-02-27T12:05:00.000"));
    eventA.setCompletedDate(parseDate("2021-02-27T11:05:00.000"));
    eventA.setCompletedBy("herb");
    eventA.setAssignedUser(user);
    note1 = new TrackedEntityComment("note1", "ant");
    note1.setUid(CodeGenerator.generateUid());
    note1.setCreated(new Date());
    note1.setLastUpdated(new Date());
    eventA.setComments(List.of(note1));
    manager.save(eventA, false);
    enrollmentA.setEvents(Set.of(eventA));
    enrollmentA.setFollowup(true);
    manager.save(enrollmentA, false);

    enrollmentB =
        enrollmentService.enrollTrackedEntity(
            trackedEntityA, programB, new Date(), new Date(), orgUnitA);
    eventB = new Event();
    eventB.setEnrollment(enrollmentB);
    eventB.setProgramStage(programStageB1);
    eventB.setOrganisationUnit(orgUnitA);
    eventB.setAttributeOptionCombo(defaultCategoryOptionCombo);
    manager.save(eventB, false);
    enrollmentB.setEvents(Set.of(eventB));
    manager.save(enrollmentB, false);

    trackedEntityB = createTrackedEntity(orgUnitB);
    trackedEntityB.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityB, false);

    TrackedEntity trackedEntityC = createTrackedEntity(orgUnitC);
    trackedEntityC.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityC, false);

    trackerOwnershipManager.assignOwnership(trackedEntityA, programA, orgUnitA, true, true);
    trackerOwnershipManager.assignOwnership(trackedEntityA, programB, orgUnitA, true, true);

    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaA, trackedEntityA, "A"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaB, trackedEntityA, "B"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaC, trackedEntityA, "C"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaE, trackedEntityA, "E"));

    RelationshipType relationshipTypeA = createRelationshipType('A');
    relationshipTypeA
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeA.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeA
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeA.getToConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeA.getSharing().setOwner(user);
    manager.save(relationshipTypeA, false);

    relationshipA = new Relationship();
    relationshipA.setUid(CodeGenerator.generateUid());
    relationshipA.setRelationshipType(relationshipTypeA);
    RelationshipItem fromA = new RelationshipItem();
    fromA.setTrackedEntity(trackedEntityA);
    fromA.setRelationship(relationshipA);
    relationshipA.setFrom(fromA);
    RelationshipItem toA = new RelationshipItem();
    toA.setTrackedEntity(trackedEntityB);
    toA.setRelationship(relationshipA);
    relationshipA.setTo(toA);
    relationshipA.setKey(RelationshipUtils.generateRelationshipKey(relationshipA));
    relationshipA.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationshipA));
    manager.save(relationshipA, false);

    RelationshipType relationshipTypeB = createRelationshipType('B');
    relationshipTypeB
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeB.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeB.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    relationshipTypeB.getToConstraint().setProgram(programA);
    relationshipTypeB.getSharing().setOwner(user);
    manager.save(relationshipTypeB, false);

    relationshipB = new Relationship();
    relationshipB.setUid(CodeGenerator.generateUid());
    relationshipB.setRelationshipType(relationshipTypeB);
    RelationshipItem fromB = new RelationshipItem();
    fromB.setTrackedEntity(trackedEntityA);
    fromB.setRelationship(relationshipB);
    relationshipB.setFrom(fromB);
    RelationshipItem toB = new RelationshipItem();
    toB.setEnrollment(enrollmentA);
    toB.setRelationship(relationshipB);
    relationshipB.setTo(toB);
    relationshipB.setKey(RelationshipUtils.generateRelationshipKey(relationshipB));
    relationshipB.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationshipB));
    manager.save(relationshipB, false);

    RelationshipType relationshipTypeC = createRelationshipType('C');
    relationshipTypeC
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeC.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeC
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    relationshipTypeC.getToConstraint().setProgramStage(programStageA1);
    relationshipTypeC.getSharing().setOwner(user);
    manager.save(relationshipTypeC, false);

    relationshipC = new Relationship();
    relationshipC.setUid(CodeGenerator.generateUid());
    relationshipC.setRelationshipType(relationshipTypeC);
    RelationshipItem fromC = new RelationshipItem();
    fromC.setTrackedEntity(trackedEntityA);
    fromC.setRelationship(relationshipC);
    relationshipC.setFrom(fromC);
    RelationshipItem toC = new RelationshipItem();
    toC.setEvent(eventA);
    toC.setRelationship(relationshipC);
    relationshipC.setTo(toC);
    relationshipC.setKey(RelationshipUtils.generateRelationshipKey(relationshipC));
    relationshipC.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationshipC));
    manager.save(relationshipC, false);

    injectSecurityContext(user);
  }

  @Test
  void shouldReturnEmptyCollectionGivenUserHasNoAccessToTrackedEntityType() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .build();

    trackedEntityTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    trackedEntityTypeA.getSharing().setOwner(admin);
    manager.updateNoAcl(trackedEntityA);

    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));
    assertContains(
        "racked entity type is specified but does not exist: " + trackedEntityTypeA.getUid(),
        ex.getMessage());
  }

  @Test
  void shouldReturnTrackedEntitiesGivenUserHasDataReadAccessToTrackedEntityType()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid(), orgUnitB.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA, trackedEntityB), trackedEntities);
  }

  @Test
  void shouldIncludeAllAttributesOfGivenTrackedEntity()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .includeAllAttributes(true)
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertContainsOnly(
        Set.of("A", "B", "C", "E"),
        attributeNames(trackedEntities.get(0).getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldReturnTrackedEntityIncludingAllAttributesEnrollmentsEventsRelationshipsOwners()
      throws ForbiddenException, NotFoundException, BadRequestException {
    // this was declared as "remove ownership"; unclear to me how this is removing ownership
    trackerOwnershipManager.assignOwnership(trackedEntityA, programB, orgUnitB, true, true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .trackedEntityParams(TrackedEntityParams.TRUE)
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA.getUid()), uids(trackedEntities));
    assertContainsOnly(Set.of(enrollmentA.getUid()), uids(trackedEntities.get(0).getEnrollments()));

    assertAll(
        () -> assertEquals(2, trackedEntities.get(0).getProgramOwners().size()),
        () ->
            assertContainsOnly(
                Set.of(trackedEntityA.getUid()),
                trackedEntities.get(0).getProgramOwners().stream()
                    .map(po -> po.getTrackedEntity().getUid())
                    .collect(Collectors.toSet())),
        () ->
            assertContainsOnly(
                Set.of(orgUnitA.getUid(), orgUnitB.getUid()),
                trackedEntities.get(0).getProgramOwners().stream()
                    .map(po -> po.getOrganisationUnit().getUid())
                    .collect(Collectors.toSet())),
        () ->
            assertContainsOnly(
                Set.of(programA.getUid(), programB.getUid()),
                trackedEntities.get(0).getProgramOwners().stream()
                    .map(po -> po.getProgram().getUid())
                    .collect(Collectors.toSet())));
  }

  @Test
  void shouldReturnTrackedEntityIncludeAllAttributesInProtectedProgramNoAccess()
      throws ForbiddenException, NotFoundException, BadRequestException {
    // this was declared as "remove ownership"; unclear to me how this is removing ownership
    trackerOwnershipManager.assignOwnership(trackedEntityA, programB, orgUnitB, true, true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertContainsOnly(
        Set.of("A", "B", "C"),
        attributeNames(trackedEntities.get(0).getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldReturnTrackedEntityIncludeSpecificProtectedProgram()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .programUid(programB.getUid())
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertContainsOnly(
        Set.of("A", "B", "E"),
        attributeNames(trackedEntities.get(0).getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldTrackedEntityIncludeSpecificOpenProgram()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .programUid(programA.getUid())
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertContainsOnly(
        Set.of("A", "B", "C"),
        attributeNames(trackedEntities.get(0).getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldReturnEmptyCollectionGivenSingleQuoteInAttributeSearchInput()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .filters(teaA.getUid() + ":eq:M'M")
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityWithLastUpdatedParameter()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedStartDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            .lastUpdatedEndDate(new Date())
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);

    // Update last updated start date to today
    operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedStartDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .lastUpdatedEndDate(new Date())
            .build();

    assertIsEmpty(trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  @Disabled("12098 This test is not working")
  void shouldReturnTrackedEntityWithEventFilters()
      throws ForbiddenException, NotFoundException, BadRequestException {

    TrackedEntityOperationParams.TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .assignedUserQueryParam(new AssignedUserQueryParam(null, user, null))
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .programUid(programA.getUid())
            .eventStartDate(Date.from(Instant.now().minus(10, ChronoUnit.DAYS)))
            .eventEndDate(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)));

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(builder.eventStatus(EventStatus.COMPLETED).build());
    assertEquals(4, trackedEntities.size());
    // Update status to active
    final List<TrackedEntity> limitedTrackedEntities =
        trackedEntityService.getTrackedEntities(builder.eventStatus(EventStatus.ACTIVE).build());
    assertIsEmpty(limitedTrackedEntities);
    // Update status to overdue
    final List<TrackedEntity> limitedTrackedEntities2 =
        trackedEntityService.getTrackedEntities(builder.eventStatus(EventStatus.OVERDUE).build());
    assertIsEmpty(limitedTrackedEntities2);
    // Update status to schedule
    final List<TrackedEntity> limitedTrackedEntities3 =
        trackedEntityService.getTrackedEntities(builder.eventStatus(EventStatus.OVERDUE).build());
    assertIsEmpty(limitedTrackedEntities3);
    // Update status to schedule
    final List<TrackedEntity> limitedTrackedEntities4 =
        trackedEntityService.getTrackedEntities(builder.eventStatus(EventStatus.SCHEDULE).build());
    assertIsEmpty(limitedTrackedEntities4);
    // Update status to visited
    final List<TrackedEntity> limitedTrackedEntities5 =
        trackedEntityService.getTrackedEntities(builder.eventStatus(EventStatus.VISITED).build());
    assertIsEmpty(limitedTrackedEntities5);
  }

  @Test
  void shouldIncludeDeletedEnrollmentAndEvents()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeDeleted(true)
            .trackedEntityParams(TrackedEntityParams.TRUE)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    TrackedEntity trackedEntity = trackedEntities.get(0);
    Set<String> deletedEnrollments =
        trackedEntity.getEnrollments().stream()
            .filter(Enrollment::isDeleted)
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertIsEmpty(deletedEnrollments);
    Set<String> deletedEvents =
        trackedEntity.getEnrollments().stream()
            .flatMap(enrollment -> enrollment.getEvents().stream())
            .filter(Event::isDeleted)
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertIsEmpty(deletedEvents);

    enrollmentService.deleteEnrollment(enrollmentA);
    eventService.deleteEvent(eventA);

    trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    trackedEntity = trackedEntities.get(0);

    assertContainsOnly(
        Set.of(enrollmentA.getUid(), enrollmentB.getUid()), uids(trackedEntity.getEnrollments()));
    deletedEnrollments =
        trackedEntity.getEnrollments().stream()
            .filter(Enrollment::isDeleted)
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(enrollmentA.getUid()), deletedEnrollments);

    Set<Event> events =
        trackedEntity.getEnrollments().stream()
            .flatMap(e -> e.getEvents().stream())
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(eventA.getUid(), eventB.getUid()), uids(events));
    deletedEvents =
        events.stream()
            .filter(Event::isDeleted)
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(eventA.getUid()), deletedEvents);

    operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeDeleted(false)
            .trackedEntityParams(TrackedEntityParams.TRUE)
            .build();
    trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    trackedEntity = trackedEntities.get(0);
    assertContainsOnly(Set.of(enrollmentB.getUid()), uids(trackedEntity.getEnrollments()));
    events =
        trackedEntity.getEnrollments().stream()
            .flatMap(e -> e.getEvents().stream())
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(eventB.getUid()), uids(events));
  }

  @Test
  void shouldReturnTrackedEntityAndEnrollmentsGivenTheyShouldBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, false, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .includeAllAttributes(true)
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA.getUid()), uids(trackedEntities));
    assertContainsOnly(
        Set.of(enrollmentA.getUid(), enrollmentB.getUid()),
        uids(trackedEntities.get(0).getEnrollments()));
    // ensure that EnrollmentAggregate is called and attaches the enrollments attributes (program
    // attributes)
    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentA =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(this.enrollmentA.getUid()))
            .findFirst();
    assertContainsOnly(
        Set.of("C"),
        attributeNames(enrollmentA.get().getTrackedEntity().getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldReturnTrackedEntityWithoutEnrollmentsGivenTheyShouldNotBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .includeAllAttributes(true)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertIsEmpty(trackedEntities.get(0).getEnrollments());
  }

  @Test
  void shouldReturnTrackedEntityWithEventsAndNotesGivenTheyShouldBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, true, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .includeAllAttributes(true)
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertContainsOnly(
        Set.of(enrollmentA.getUid(), enrollmentB.getUid()),
        uids(trackedEntities.get(0).getEnrollments()));
    // ensure that EventAggregate is called and attaches the events with notes
    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentA =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(this.enrollmentA.getUid()))
            .findFirst();
    Set<Event> events = enrollmentA.get().getEvents();
    assertContainsOnly(Set.of(eventA), events);
    assertContainsOnly(Set.of(note1), events.stream().findFirst().get().getComments());
  }

  @Test
  void shouldReturnTrackedEntityWithoutEventsGivenTheyShouldNotBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(
            false,
            TrackedEntityEnrollmentParams.TRUE.withIncludeEvents(false),
            false,
            false,
            false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA.getUid()), uids(trackedEntities));
    assertContainsOnly(
        Set.of(enrollmentA.getUid(), enrollmentB.getUid()),
        uids(trackedEntities.get(0).getEnrollments()));
    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentA =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(this.enrollmentA.getUid()))
            .findFirst();
    assertIsEmpty(enrollmentA.get().getEvents());
  }

  @Test
  void shouldReturnTrackedEntityMappedCorrectly()
      throws ForbiddenException, NotFoundException, BadRequestException {
    final Date currentTime = new Date();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    assertAll(
        () -> assertEquals(trackedEntityA.getUid(), trackedEntity.getUid()),
        () ->
            assertEquals(
                trackedEntity.getTrackedEntityType().getUid(), trackedEntityTypeA.getUid()),
        () -> assertEquals(orgUnitA.getUid(), trackedEntity.getOrganisationUnit().getUid()),
        () -> assertFalse(trackedEntity.isInactive()),
        () -> assertFalse(trackedEntity.isDeleted()),
        () -> checkDate(currentTime, trackedEntity.getCreated()),
        () -> checkDate(currentTime, trackedEntity.getCreatedAtClient()),
        () -> checkDate(currentTime, trackedEntity.getLastUpdatedAtClient()),
        () -> checkDate(currentTime, trackedEntity.getLastUpdated()),
        // get stored by is always null
        () -> assertNull(trackedEntity.getStoredBy()));
  }

  @Test
  void shouldReturnEnrollmentMappedCorrectly()
      throws ForbiddenException, NotFoundException, BadRequestException {
    final Date currentTime = new Date();
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, false, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentOpt =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(enrollmentA.getUid()))
            .findFirst();
    assertTrue(enrollmentOpt.isPresent());
    Enrollment enrollment = enrollmentOpt.get();
    assertAll(
        () -> assertEquals(enrollmentA.getId(), enrollment.getId()),
        () -> assertEquals(trackedEntityA.getUid(), enrollment.getTrackedEntity().getUid()),
        () ->
            assertEquals(
                trackedEntityA.getTrackedEntityType().getUid(),
                enrollment.getTrackedEntity().getTrackedEntityType().getUid()),
        () -> assertEquals(orgUnitA.getUid(), enrollment.getOrganisationUnit().getUid()),
        () -> assertEquals(orgUnitA.getName(), enrollment.getOrganisationUnit().getName()),
        () -> assertEquals(programA.getUid(), enrollment.getProgram().getUid()),
        () -> assertEquals(ProgramStatus.ACTIVE, enrollment.getStatus()),
        () -> assertFalse(enrollment.isDeleted()),
        () -> assertTrue(enrollment.getFollowup()),
        () -> checkDate(currentTime, enrollment.getCreated()),
        () -> checkDate(currentTime, enrollment.getCreatedAtClient()),
        () -> checkDate(currentTime, enrollment.getLastUpdated()),
        () -> checkDate(currentTime, enrollment.getLastUpdatedAtClient()),
        () -> checkDate(currentTime, enrollment.getEnrollmentDate()),
        () -> checkDate(currentTime, enrollment.getIncidentDate()),
        () -> assertNull(enrollment.getStoredBy()));
  }

  @Test
  void shouldReturnEventMappedCorrectly()
      throws ForbiddenException, NotFoundException, BadRequestException {
    final Date currentTime = new Date();
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, false, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .includeAllAttributes(true)
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentOpt =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(enrollmentA.getUid()))
            .findFirst();
    assertTrue(enrollmentOpt.isPresent());
    Enrollment enrollment = enrollmentOpt.get();
    Optional<Event> eventOpt = enrollment.getEvents().stream().findFirst();
    assertTrue(eventOpt.isPresent());
    Event event = eventOpt.get();
    assertAll(
        () -> assertEquals(eventA.getId(), event.getId()),
        () -> assertEquals(eventA.getUid(), event.getUid()),
        () -> assertEquals(EventStatus.ACTIVE, event.getStatus()),
        () -> assertEquals(orgUnitA.getUid(), event.getOrganisationUnit().getUid()),
        () -> assertEquals(orgUnitA.getName(), event.getOrganisationUnit().getName()),
        () -> assertEquals(enrollmentA.getUid(), event.getEnrollment().getUid()),
        () -> assertEquals(programA.getUid(), event.getEnrollment().getProgram().getUid()),
        () -> assertEquals(ProgramStatus.ACTIVE, event.getEnrollment().getStatus()),
        () ->
            assertEquals(
                trackedEntityA.getUid(), event.getEnrollment().getTrackedEntity().getUid()),
        () -> assertEquals(eventA.getProgramStage().getUid(), event.getProgramStage().getUid()),
        () ->
            assertEquals(
                defaultCategoryOptionCombo.getUid(), event.getAttributeOptionCombo().getUid()),
        () -> assertFalse(event.isDeleted()),
        () -> assertTrue(event.getEnrollment().getFollowup()),
        () -> assertEquals(user, event.getAssignedUser()),
        () -> checkDate(currentTime, event.getCreated()),
        () -> checkDate(currentTime, event.getLastUpdated()),
        () -> checkDate(eventA.getDueDate(), event.getDueDate()),
        () -> checkDate(currentTime, event.getCreatedAtClient()),
        () -> checkDate(currentTime, event.getLastUpdatedAtClient()),
        () -> checkDate(eventA.getCompletedDate(), event.getCompletedDate()),
        () -> assertEquals(eventA.getCompletedBy(), event.getCompletedBy()));
  }

  @Test
  void shouldReturnTrackedEntityWithRelationshipsTei2Tei()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(true, TrackedEntityEnrollmentParams.FALSE, false, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipA.getUid()))
            .findFirst();
    assertTrue(relOpt.isPresent());
    Relationship actual = relOpt.get().getRelationship();
    assertAll(
        () -> assertEquals(trackedEntityA.getUid(), actual.getFrom().getTrackedEntity().getUid()),
        () -> assertEquals(trackedEntityB.getUid(), actual.getTo().getTrackedEntity().getUid()));
  }

  @Test
  void returnTrackedEntityRelationshipsWithTei2Enrollment()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(true, TrackedEntityEnrollmentParams.FALSE, false, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipB.getUid()))
            .findFirst();
    assertTrue(relOpt.isPresent());
    Relationship actual = relOpt.get().getRelationship();
    assertAll(
        () -> assertEquals(trackedEntityA.getUid(), actual.getFrom().getTrackedEntity().getUid()),
        () -> assertEquals(enrollmentA.getUid(), actual.getTo().getEnrollment().getUid()));
  }

  @Test
  void shouldReturnTrackedEntityRelationshipsWithTei2Event()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(true, TrackedEntityEnrollmentParams.TRUE, false, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipC.getUid()))
            .findFirst();
    assertTrue(relOpt.isPresent());
    Relationship actual = relOpt.get().getRelationship();
    assertAll(
        () -> assertEquals(trackedEntityA.getUid(), actual.getFrom().getTrackedEntity().getUid()),
        () -> assertEquals(eventA.getUid(), actual.getTo().getEvent().getUid()));
  }

  private static List<String> uids(Collection<? extends BaseIdentifiableObject> trackedEntities) {
    return trackedEntities.stream()
        .map(BaseIdentifiableObject::getUid)
        .collect(Collectors.toList());
  }

  private Set<String> attributeNames(final Collection<TrackedEntityAttributeValue> attributes) {
    // depends on createTrackedEntityAttribute() prefixing with "Attribute"
    return attributes.stream()
        .map(a -> StringUtils.removeStart(a.getAttribute().getName(), "Attribute"))
        .collect(Collectors.toSet());
  }

  protected ProgramStage createProgramStage(Program program) {
    ProgramStage programStage = createProgramStage('1', program);
    programStage.setUid(CodeGenerator.generateUid());
    programStage.setRepeatable(true);
    programStage.setEnableUserAssignment(true);
    programStage.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStage, false);
    return programStage;
  }

  private void checkDate(Date currentTime, Date date) {
    final long interval = currentTime.getTime() - date.getTime();
    long second = 1000;
    assertTrue(
        Math.abs(interval) < second,
        "Timestamp is higher than expected interval. Expecting: "
            + (long) 1000
            + " got: "
            + interval);
  }
}
