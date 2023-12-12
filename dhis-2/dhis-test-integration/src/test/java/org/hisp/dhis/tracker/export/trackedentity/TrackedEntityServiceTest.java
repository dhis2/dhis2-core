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

import static java.util.Collections.emptySet;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourBefore;
import static org.hisp.dhis.tracker.TrackerTestUtils.twoHoursAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.twoHoursBefore;
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
import java.util.Map;
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
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.RelationshipUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.note.Note;
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

  @Autowired protected UserService _userService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private EventService eventService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  @Autowired private CurrentUserService currentUserService;

  private User user;

  private User userWithSearchInAllAuthority;

  private User superuser;

  private User authorizedUser;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private OrganisationUnit orgUnitChildA;

  private TrackedEntityAttribute teaA;

  private TrackedEntityAttribute teaC;

  private TrackedEntityType trackedEntityTypeA;

  private Program programA;

  private Program programB;

  private Program programC;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Event eventA;

  private Event eventB;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntity trackedEntityChildA;

  private TrackedEntity trackedEntityGrandchildA;

  private Note note;

  private CategoryOptionCombo defaultCategoryOptionCombo;

  private Relationship relationshipA;

  private Relationship relationshipB;

  private Relationship relationshipC;

  private static List<String> uids(
      Collection<? extends BaseIdentifiableObject> identifiableObjects) {
    return identifiableObjects.stream()
        .map(BaseIdentifiableObject::getUid)
        .collect(Collectors.toList());
  }

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;

    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);
    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitB, false);
    OrganisationUnit orgUnitC = createOrganisationUnit('C');
    manager.save(orgUnitC, false);
    orgUnitChildA = createOrganisationUnit("childA");
    orgUnitChildA.setParent(orgUnitA);
    manager.save(orgUnitChildA);
    orgUnitA.setChildren(Set.of(orgUnitChildA));
    manager.update(orgUnitA);
    OrganisationUnit orgUnitGrandchildA = createOrganisationUnit("grandchildA");
    orgUnitGrandchildA.setParent(orgUnitChildA);
    manager.save(orgUnitGrandchildA);
    orgUnitChildA.setChildren(Set.of(orgUnitGrandchildA));
    manager.update(orgUnitChildA);

    superuser = preCreateInjectAdminUser();
    superuser.setOrganisationUnits(Set.of(orgUnitA, orgUnitB));
    manager.save(superuser);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "F_EXPORT_DATA");
    user.setTeiSearchOrganisationUnits(Set.of(orgUnitA, orgUnitB, orgUnitC));

    authorizedUser =
        createAndAddUser(
            false,
            "authorizedUser",
            emptySet(),
            emptySet(),
            "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS");

    userWithSearchInAllAuthority =
        createAndAddUser(
            false,
            "userSearchInAll",
            Set.of(orgUnitA),
            Set.of(orgUnitA),
            "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS");
    userWithSearchInAllAuthority.setTeiSearchOrganisationUnits(
        Set.of(orgUnitA, orgUnitB, orgUnitC));

    teaA = createTrackedEntityAttribute('A', ValueType.TEXT);
    teaA.setUnique(true);
    manager.save(teaA, false);
    TrackedEntityAttribute teaB = createTrackedEntityAttribute('B', ValueType.TEXT);
    manager.save(teaB, false);
    teaC = createTrackedEntityAttribute('C', ValueType.TEXT);
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
    programA.setMinAttributesRequiredToSearch(0);
    manager.save(programA, false);
    ProgramStage programStageA1 = createProgramStage(programA);
    programStageA1.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStageA1, false);
    ProgramStage programStageA2 = createProgramStage(programA);
    programStageA2.setPublicAccess(AccessStringHelper.FULL);
    manager.save(programStageA2, false);
    programA.setProgramStages(
        Stream.of(programStageA1, programStageA2).collect(Collectors.toCollection(HashSet::new)));
    programA.getSharing().setOwner(superuser);
    programA.getSharing().setPublicAccess(AccessStringHelper.FULL);
    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        new ProgramTrackedEntityAttribute(programA, teaC);
    programTrackedEntityAttribute.setSearchable(true);
    programA.getProgramAttributes().add(programTrackedEntityAttribute);
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
    programB.getSharing().setOwner(superuser);
    programB.getSharing().setPublicAccess(AccessStringHelper.FULL);
    manager.update(programB);

    programC = createProgram('C', new HashSet<>(), orgUnitC);
    programC.setProgramType(ProgramType.WITH_REGISTRATION);
    programC.setTrackedEntityType(trackedEntityTypeA);
    programC.setCategoryCombo(defaultCategoryCombo);
    programC.setAccessLevel(AccessLevel.PROTECTED);
    programC.getSharing().setPublicAccess(AccessStringHelper.READ);
    manager.save(programC, false);

    programB
        .getProgramAttributes()
        .addAll(
            List.of(
                new ProgramTrackedEntityAttribute(programB, teaA),
                new ProgramTrackedEntityAttribute(programB, teaD),
                new ProgramTrackedEntityAttribute(programB, teaE)));
    manager.update(programB);

    trackedEntityA = createTrackedEntity(orgUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    trackedEntityChildA = createTrackedEntity(orgUnitChildA);
    trackedEntityChildA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityChildA, false);

    trackedEntityGrandchildA = createTrackedEntity(orgUnitGrandchildA);
    trackedEntityGrandchildA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityGrandchildA, false);

    enrollmentA =
        enrollmentService.enrollTrackedEntity(
            trackedEntityA, programA, new Date(), new Date(), orgUnitA);
    eventA = new Event();
    eventA.setEnrollment(enrollmentA);
    eventA.setProgramStage(programStageA1);
    eventA.setOrganisationUnit(orgUnitA);
    eventA.setAttributeOptionCombo(defaultCategoryOptionCombo);
    eventA.setOccurredDate(parseDate("2021-05-27T12:05:00.000"));
    eventA.setScheduledDate(parseDate("2021-02-27T12:05:00.000"));
    eventA.setCompletedDate(parseDate("2021-02-27T11:05:00.000"));
    eventA.setCompletedBy("herb");
    eventA.setAssignedUser(user);
    note = new Note("note1", "ant");
    note.setUid(CodeGenerator.generateUid());
    note.setCreated(new Date());
    note.setLastUpdated(new Date());
    eventA.setNotes(List.of(note));
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
        new TrackedEntityAttributeValue(teaA, trackedEntityChildA, "A"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaA, trackedEntityGrandchildA, "A"));
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
            .build();

    trackedEntityTypeA.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    trackedEntityTypeA.getSharing().setOwner(superuser);
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
            .orgUnitMode(DESCENDANTS)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .user(user)
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityB, trackedEntityChildA, trackedEntityGrandchildA),
        trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIncludingAllAttributesEnrollmentsEventsRelationshipsOwners()
      throws ForbiddenException, NotFoundException, BadRequestException {
    // this was declared as "remove ownership"; unclear to me how this is removing ownership
    trackerOwnershipManager.assignOwnership(trackedEntityA, programB, orgUnitB, true, true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityParams(TrackedEntityParams.TRUE)
            .user(user)
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
  void shouldReturnTrackedEntityIncludeSpecificProtectedProgram()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programUid(programB.getUid())
            .user(user)
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
            .orgUnitMode(SELECTED)
            .programUid(programA.getUid())
            .user(user)
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
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .filters(Map.of(teaA.getUid(), List.of(new QueryFilter(QueryOperator.EQ, "M'M"))))
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfGivenFilterMatches()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .filters(Map.of(teaA.getUid(), List.of(new QueryFilter(QueryOperator.EQ, "A"))))
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfTEWasUpdatedAfterPassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeLastUpdated = oneHourBefore(trackedEntityA.getLastUpdated());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedStartDate(oneHourBeforeLastUpdated)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfTEWasUpdatedBeforePassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterLastUpdated = oneHourAfter(trackedEntityA.getLastUpdated());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedStartDate(oneHourAfterLastUpdated)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfTEWasUpdatedBeforePassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterLastUpdated = oneHourAfter(trackedEntityA.getLastUpdated());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedEndDate(oneHourAfterLastUpdated)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfTEWasUpdatedAfterPassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeLastUpdated = oneHourBefore(trackedEntityA.getLastUpdated());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedEndDate(oneHourBeforeLastUpdated)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfTEWasEnrolledAfterPassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeEnrollmentDate = oneHourBefore(enrollmentA.getEnrollmentDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programEnrollmentStartDate(oneHourBeforeEnrollmentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfTEWasEnrolledBeforePassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterEnrollmentDate = oneHourAfter(enrollmentA.getEnrollmentDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programEnrollmentStartDate(oneHourAfterEnrollmentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfTEWasEnrolledBeforePassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterEnrollmentDate = oneHourAfter(enrollmentA.getEnrollmentDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programEnrollmentEndDate(oneHourAfterEnrollmentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfTEWasEnrolledAfterPassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeEnrollmentDate = oneHourBefore(enrollmentA.getEnrollmentDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programEnrollmentEndDate(oneHourBeforeEnrollmentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfEnrollmentOccurredAfterPassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeIncidentDate = oneHourBefore(enrollmentA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programIncidentStartDate(oneHourBeforeIncidentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfEnrollmentOccurredBeforePassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterIncidentDate = oneHourAfter(enrollmentA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programIncidentStartDate(oneHourAfterIncidentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfEnrollmentOccurredBeforePassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterIncidentDate = oneHourAfter(enrollmentA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programIncidentEndDate(oneHourAfterIncidentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfEnrollmentOccurredPassedDateAndTime()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeIncidentDate = oneHourBefore(enrollmentA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .programIncidentEndDate(oneHourBeforeIncidentDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfEventOccurredBetweenPassedDateAndTimes()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeOccurredDate = oneHourBefore(eventA.getOccurredDate());
    Date oneHourAfterOccurredDate = oneHourAfter(eventA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .eventStatus(EventStatus.ACTIVE)
            .eventStartDate(oneHourBeforeOccurredDate)
            .eventEndDate(oneHourAfterOccurredDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfEventOccurredBeforePassedDateAndTimes()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourAfterOccurredDate = oneHourAfter(eventA.getOccurredDate());
    Date twoHoursAfterOccurredDate = twoHoursAfter(eventA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .eventStatus(EventStatus.ACTIVE)
            .eventStartDate(oneHourAfterOccurredDate)
            .eventEndDate(twoHoursAfterOccurredDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnEmptyIfEventOccurredAfterPassedDateAndTimes()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Date oneHourBeforeOccurredDate = oneHourBefore(eventA.getOccurredDate());
    Date twoHoursBeforeOccurredDate = twoHoursBefore(eventA.getOccurredDate());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(programA.getUid())
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .eventStatus(EventStatus.ACTIVE)
            .eventStartDate(twoHoursBeforeOccurredDate)
            .eventEndDate(oneHourBeforeOccurredDate)
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnEmptyCollectionIfGivenFilterDoesNotMatch()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .filters(Map.of(teaA.getUid(), List.of(new QueryFilter(QueryOperator.EQ, "Z"))))
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntitiesIfTheyHaveGivenAttributeFilteredUsingOnlyUID()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid(), orgUnitB.getUid()))
            .orgUnitMode(DESCENDANTS)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .filters(Map.of(teaA.getUid(), List.of()))
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityChildA, trackedEntityGrandchildA), trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityWithLastUpdatedParameter()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedStartDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            .lastUpdatedEndDate(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)))
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);

    // Update last updated start date to today
    operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .lastUpdatedStartDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .lastUpdatedEndDate(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)))
            .user(user)
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
  @Disabled("IncludeDeleted param is not working when TE has a deleted relationship")
  void shouldIncludeDeletedEnrollmentAndEvents()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
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
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .user(user)
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
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertIsEmpty(trackedEntities.get(0).getEnrollments());
  }

  @Test
  void shouldReturnTrackedEntityWithEventsAndNotesGivenTheyShouldBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, true, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .user(user)
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
    assertContainsOnly(Set.of(note), events.stream().findFirst().get().getNotes());
  }

  @Test
  void shouldReturnTrackedEntityWithoutEventsGivenTheyShouldNotBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(
            false, TrackedEntityEnrollmentParams.TRUE.withIncludeEvents(false), false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityParams(params)
            .user(user)
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
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .user(user)
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
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityParams(params)
            .user(user)
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
        () -> checkDate(currentTime, enrollment.getOccurredDate()),
        () -> assertNull(enrollment.getStoredBy()));
  }

  @Test
  void shouldReturnEventMappedCorrectly()
      throws ForbiddenException, NotFoundException, BadRequestException {
    final Date currentTime = new Date();
    TrackedEntityParams params =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .trackedEntityParams(params)
            .user(user)
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
        () -> checkDate(eventA.getScheduledDate(), event.getScheduledDate()),
        () -> checkDate(currentTime, event.getCreatedAtClient()),
        () -> checkDate(currentTime, event.getLastUpdatedAtClient()),
        () -> checkDate(eventA.getCompletedDate(), event.getCompletedDate()),
        () -> assertEquals(eventA.getCompletedBy(), event.getCompletedBy()));
  }

  @Test
  void shouldReturnTrackedEntityWithRelationshipsTei2Tei()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(true, TrackedEntityEnrollmentParams.FALSE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .user(user)
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
        new TrackedEntityParams(true, TrackedEntityEnrollmentParams.FALSE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .user(user)
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
        new TrackedEntityParams(true, TrackedEntityEnrollmentParams.TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of(trackedEntityA.getUid()))
            .trackedEntityParams(params)
            .user(user)
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

  @Test
  void shouldReturnAllEntitiesWhenSuperuserAndNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContext(superuser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .user(superuser)
            .programUid(programA.getUid())
            .filters(Map.of(teaC.getUid(), List.of(new QueryFilter(QueryOperator.LIKE, "C"))))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnAllEntitiesByTrackedEntityTypeMatchingFilterWhenAuthorizedUserNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContext(authorizedUser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .user(authorizedUser)
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .filters(Map.of(teaA.getUid(), List.of(new QueryFilter(QueryOperator.LIKE, "A"))))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityChildA, trackedEntityGrandchildA), trackedEntities);
  }

  @Test
  void shouldReturnAllEntitiesEnrolledInProgramMatchingFilterWhenAuthorizedUserNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContext(authorizedUser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .user(authorizedUser)
            .programUid(programB.getUid())
            .filters(Map.of(teaA.getUid(), List.of(new QueryFilter(QueryOperator.LIKE, "A"))))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldFailWhenModeAllUserCanSearchEverywhereButNotSuperuserAndNoAccessToProgram() {
    injectSecurityContext(userWithSearchInAllAuthority);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .programUid(programC.getUid())
            .user(userWithSearchInAllAuthority)
            .build();

    ForbiddenException ex =
        assertThrows(
            ForbiddenException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));

    assertContains(
        String.format(
            "Current user is not authorized to read data from selected program:  %s",
            programC.getUid()),
        ex.getMessage());
  }

  @Test
  void shouldReturnChildrenOfRootOrgUnitWhenOrgUnitModeChildren()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(CHILDREN)
            .organisationUnits(Set.of(orgUnitA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(
        Set.of(trackedEntityA.getUid(), trackedEntityChildA.getUid()), uids(trackedEntities));
  }

  @Test
  void shouldReturnChildrenOfRequestedOrgUnitWhenOrgUnitModeChildren()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(CHILDREN)
            .organisationUnits(Set.of(orgUnitChildA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(
        Set.of(trackedEntityChildA.getUid(), trackedEntityGrandchildA.getUid()),
        uids(trackedEntities));
  }

  @Test
  void
      shouldReturnAllChildrenOfRequestedOrgUnitsWhenOrgUnitModeChildrenAndMultipleOrgUnitsRequested()
          throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(CHILDREN)
            .organisationUnits(Set.of(orgUnitA.getUid(), orgUnitChildA.getUid()))
            .trackedEntityTypeUid(trackedEntityTypeA.getUid())
            .user(user)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(
        Set.of(
            trackedEntityA.getUid(),
            trackedEntityChildA.getUid(),
            trackedEntityGrandchildA.getUid()),
        uids(trackedEntities));
  }

  @Test
  void shouldReturnAllEntitiesWhenSuperuserAndModeAll()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContext(superuser);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .programUid(programA.getUid())
            .user(superuser)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(Set.of(trackedEntityA.getUid()), uids(trackedEntities));
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
