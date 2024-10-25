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
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourBefore;
import static org.hisp.dhis.tracker.TrackerTestUtils.twoHoursAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.twoHoursBefore;
import static org.hisp.dhis.tracker.export.trackedentity.TrackedEntityEnrollmentParams.FALSE;
import static org.hisp.dhis.tracker.export.trackedentity.TrackedEntityEnrollmentParams.TRUE;
import static org.hisp.dhis.util.DateUtils.parseDate;
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
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
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
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams.TrackedEntityOperationParamsBuilder;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Luciano Fiandesio
 */
class TrackedEntityServiceTest extends PostgresIntegrationTestBase {

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  private User user;

  private User userWithSearchInAllAuthority;

  private User superuser;

  private User authorizedUser;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private OrganisationUnit orgUnitChildA;

  private TrackedEntityAttribute teaA;

  private TrackedEntityAttribute teaC;

  private TrackedEntityAttributeValue trackedEntityAttributeValueA;

  private TrackedEntityAttributeValue trackedEntityAttributeValueB;

  private TrackedEntityAttributeValue trackedEntityAttributeValueC;

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

  private Relationship relationshipD;

  private Relationship relationshipE;

  private static List<String> uids(
      Collection<? extends BaseIdentifiableObject> identifiableObjects) {
    return identifiableObjects.stream().map(BaseIdentifiableObject::getUid).toList();
  }

  @BeforeEach
  void setUp() {
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

    superuser = getAdminUser();
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

    User currentUser = getCurrentUser();

    programB = createProgram('B', new HashSet<>(), orgUnitA);
    programB.setProgramType(ProgramType.WITH_REGISTRATION);
    programB.setTrackedEntityType(trackedEntityTypeA);
    programB.setCategoryCombo(defaultCategoryCombo);
    programB.setAccessLevel(AccessLevel.PROTECTED);
    programB.getSharing().addUserAccess(new UserAccess(currentUser, AccessStringHelper.FULL));
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

    trackedEntityAttributeValueA = new TrackedEntityAttributeValue(teaA, trackedEntityA, "A");
    trackedEntityAttributeValueB = new TrackedEntityAttributeValue(teaB, trackedEntityA, "B");
    trackedEntityAttributeValueC = new TrackedEntityAttributeValue(teaC, trackedEntityA, "C");

    trackedEntityA = createTrackedEntity(orgUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    trackedEntityA.setTrackedEntityAttributeValues(
        Set.of(
            trackedEntityAttributeValueA,
            trackedEntityAttributeValueB,
            trackedEntityAttributeValueC));
    manager.save(trackedEntityA, false);

    trackedEntityChildA = createTrackedEntity(orgUnitChildA);
    trackedEntityChildA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityChildA, false);

    trackedEntityGrandchildA = createTrackedEntity(orgUnitGrandchildA);
    trackedEntityGrandchildA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityGrandchildA, false);

    enrollmentA = createEnrollment(programA, trackedEntityA, orgUnitA);
    manager.save(enrollmentA);
    trackedEntityA.getEnrollments().add(enrollmentA);
    manager.update(trackedEntityA);

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
    note.setUid(generateUid());
    note.setCreated(new Date());
    note.setLastUpdated(new Date());
    eventA.setNotes(List.of(note));
    manager.save(eventA, false);
    enrollmentA.setEvents(Set.of(eventA));
    enrollmentA.setFollowup(true);
    manager.save(enrollmentA, false);

    enrollmentB = createEnrollment(programB, trackedEntityA, orgUnitA);
    manager.save(enrollmentB);
    trackedEntityA.getEnrollments().add(enrollmentB);
    manager.update(trackedEntityA);

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

    Enrollment enrollmentC = createEnrollment(programB, trackedEntityB, orgUnitB);
    manager.save(enrollmentC);
    trackedEntityA.getEnrollments().add(enrollmentC);
    manager.update(trackedEntityB);

    Event eventC = new Event();
    eventC.setEnrollment(enrollmentC);
    eventC.setProgramStage(programStageB1);
    eventC.setOrganisationUnit(orgUnitB);
    eventC.setAttributeOptionCombo(defaultCategoryOptionCombo);
    manager.save(eventC, false);
    enrollmentC.setEvents(Set.of(eventC));
    manager.save(enrollmentC, false);

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
    relationshipA.setUid(generateUid());
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
    relationshipB.setUid(generateUid());
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
    relationshipC.setUid(generateUid());
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

    RelationshipType relationshipTypeD = createRelationshipType('D');
    relationshipTypeD
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeD.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeD.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    relationshipTypeD.getToConstraint().setProgram(programB);
    relationshipTypeD.getSharing().setOwner(user);
    manager.save(relationshipTypeD, false);

    relationshipD = new Relationship();
    relationshipD.setUid(generateUid());
    relationshipD.setRelationshipType(relationshipTypeD);
    RelationshipItem fromD = new RelationshipItem();
    fromD.setTrackedEntity(trackedEntityA);
    fromD.setRelationship(relationshipD);
    relationshipD.setFrom(fromD);
    RelationshipItem toD = new RelationshipItem();
    toD.setEnrollment(enrollmentC);
    toD.setRelationship(relationshipD);
    relationshipD.setTo(toD);
    relationshipD.setKey(RelationshipUtils.generateRelationshipKey(relationshipD));
    relationshipD.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationshipD));
    manager.save(relationshipD, false);

    RelationshipType relationshipTypeE = createRelationshipType('E');
    relationshipTypeE
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipTypeE.getFromConstraint().setTrackedEntityType(trackedEntityTypeA);
    relationshipTypeE
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    relationshipTypeE.getToConstraint().setProgram(programB);
    relationshipTypeE.getSharing().setOwner(user);
    manager.save(relationshipTypeE, false);

    relationshipE = new Relationship();
    relationshipE.setUid(generateUid());
    relationshipE.setRelationshipType(relationshipTypeD);
    RelationshipItem fromE = new RelationshipItem();
    fromE.setTrackedEntity(trackedEntityA);
    fromE.setRelationship(relationshipE);
    relationshipE.setFrom(fromE);
    RelationshipItem toE = new RelationshipItem();
    toE.setEvent(eventC);
    toE.setRelationship(relationshipE);
    relationshipE.setTo(toE);
    relationshipE.setKey(RelationshipUtils.generateRelationshipKey(relationshipE));
    relationshipE.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationshipE));
    manager.save(relationshipE, false);

    injectSecurityContextUser(user);
  }

  @Test
  void shouldReturnEmptyCollectionGivenUserHasNoAccessToTrackedEntityType() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .trackedEntityType(trackedEntityTypeA)
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
            .organisationUnits(orgUnitA, orgUnitB)
            .orgUnitMode(DESCENDANTS)
            .trackedEntityType(trackedEntityTypeA)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntityParams(TrackedEntityParams.TRUE)
            .build();

    TrackedEntity te =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityParams.TRUE);
    assertEquals(1, te.getEnrollments().size());
    assertEquals(enrollmentA.getUid(), te.getEnrollments().stream().findFirst().get().getUid());

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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programB)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programA)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertContainsOnly(
        Set.of("A", "B", "C"),
        attributeNames(trackedEntities.get(0).getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldReturnEnrollmentsFromSpecifiedProgramWhenRequestingSingleTrackedEntity()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Enrollment enrollmentProgramB = createEnrollment(programB, trackedEntityA, orgUnitA);
    manager.save(enrollmentProgramB);
    trackedEntityA.getEnrollments().add(enrollmentProgramB);
    manager.update(trackedEntityA);

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityParams.TRUE);

    assertContainsOnly(Set.of(enrollmentA), trackedEntity.getEnrollments());
  }

  @Test
  void shouldReturnAllEnrollmentsWhenRequestingSingleTrackedEntityAndNoProgramSpecified()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Enrollment enrollmentProgramB = createEnrollment(programB, trackedEntityA, orgUnitA);
    manager.save(enrollmentProgramB);
    trackedEntityA.getEnrollments().add(enrollmentProgramB);
    manager.update(trackedEntityA);

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), null, TrackedEntityParams.TRUE);

    assertContainsOnly(
        Set.of(enrollmentA, enrollmentB, enrollmentProgramB), trackedEntity.getEnrollments());
  }

  @Test
  void shouldReturnEmptyCollectionGivenSingleQuoteInAttributeSearchInput()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .filter(teaA, List.of(new QueryFilter(QueryOperator.EQ, "M'M")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntityIfGivenFilterMatches()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .filter(teaA, List.of(new QueryFilter(QueryOperator.EQ, "A")))
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .lastUpdatedStartDate(oneHourBeforeLastUpdated)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .lastUpdatedStartDate(oneHourAfterLastUpdated)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .lastUpdatedEndDate(oneHourAfterLastUpdated)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .lastUpdatedEndDate(oneHourBeforeLastUpdated)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programEnrollmentStartDate(oneHourBeforeEnrollmentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programEnrollmentStartDate(oneHourAfterEnrollmentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programEnrollmentEndDate(oneHourAfterEnrollmentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programEnrollmentEndDate(oneHourBeforeEnrollmentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programIncidentStartDate(oneHourBeforeIncidentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programIncidentStartDate(oneHourAfterIncidentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programIncidentEndDate(oneHourAfterIncidentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .programIncidentEndDate(oneHourBeforeIncidentDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .eventStatus(EventStatus.ACTIVE)
            .eventStartDate(oneHourBeforeOccurredDate)
            .eventEndDate(oneHourAfterOccurredDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .eventStatus(EventStatus.ACTIVE)
            .eventStartDate(oneHourAfterOccurredDate)
            .eventEndDate(twoHoursAfterOccurredDate)
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
            .program(programA)
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .eventStatus(EventStatus.ACTIVE)
            .eventStartDate(twoHoursBeforeOccurredDate)
            .eventEndDate(oneHourBeforeOccurredDate)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnEmptyCollectionIfGivenFilterDoesNotMatch()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .filter(teaA, List.of(new QueryFilter(QueryOperator.EQ, "Z")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntitiesIfTheyHaveGivenAttributeFilteredUsingOnlyUID()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA, orgUnitB)
            .orgUnitMode(DESCENDANTS)
            .trackedEntityType(trackedEntityTypeA)
            .filter(teaA, List.of())
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .lastUpdatedStartDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
            .lastUpdatedEndDate(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);

    // Update last updated start date to today
    operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .lastUpdatedStartDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .lastUpdatedEndDate(Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)))
            .build();

    assertIsEmpty(trackedEntityService.getTrackedEntities(operationParams));
  }

  @Test
  @Disabled("12098 This test is not working")
  void shouldReturnTrackedEntityWithEventFilters()
      throws ForbiddenException, NotFoundException, BadRequestException {

    TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .assignedUserQueryParam(new AssignedUserQueryParam(null, null, UID.of(user)))
            .organisationUnits(orgUnitA)
            .program(programA)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
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

    manager.delete(enrollmentA);
    manager.delete(eventA);

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
            .organisationUnits(orgUnitA)
            .trackedEntityType(trackedEntityTypeA)
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
    TrackedEntityParams params = new TrackedEntityParams(false, TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntities(trackedEntityA)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntities(trackedEntityA)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertIsEmpty(trackedEntities.get(0).getEnrollments());
  }

  @Test
  void shouldReturnTrackedEntityWithEventsAndNotesGivenTheyShouldBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params = new TrackedEntityParams(false, TRUE, true, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntities(trackedEntityA)
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
    assertContainsOnly(Set.of(note), events.stream().findFirst().get().getNotes());
  }

  @Test
  void shouldReturnTrackedEntityWithoutEventsGivenTheyShouldNotBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params =
        new TrackedEntityParams(false, TRUE.withIncludeEvents(false), false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
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
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
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
    TrackedEntityParams params = new TrackedEntityParams(false, TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
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
        () -> assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus()),
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
    TrackedEntityParams params = new TrackedEntityParams(false, TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
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
        () -> assertEquals(EnrollmentStatus.ACTIVE, event.getEnrollment().getStatus()),
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
  void shouldReturnTrackedEntityWithRelationshipsTe2Te()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params = new TrackedEntityParams(true, FALSE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntities(trackedEntityA)
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
  void shouldNotReturnTrackedEntityRelationshipWhenTEFromItemNotAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);
    OrganisationUnit inaccessibleOrgUnit = createOrganisationUnit('D');
    manager.save(inaccessibleOrgUnit);
    makeProgramMetadataInaccessible(programB);
    makeProgramMetadataInaccessible(programC);
    trackerOwnershipManager.assignOwnership(
        trackedEntityA, programA, inaccessibleOrgUnit, true, true);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitB, trackedEntityB);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipA.getUid()))
            .findFirst();
    assertTrue(relOpt.isEmpty());
  }

  @Test
  void shouldNotReturnTrackedEntityRelationshipWhenTEToItemNotAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);
    OrganisationUnit inaccessibleOrgUnit = createOrganisationUnit('D');
    manager.save(inaccessibleOrgUnit);
    makeProgramMetadataInaccessible(programB);
    makeProgramMetadataInaccessible(programC);
    trackerOwnershipManager.assignOwnership(
        trackedEntityB, programA, inaccessibleOrgUnit, true, true);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipA.getUid()))
            .findFirst();
    assertTrue(relOpt.isEmpty());
  }

  @Test
  void shouldReturnTrackedEntityRelationshipWhenEnrollmentItemAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);
    makeProgramMetadataInaccessible(programC);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipD.getUid()))
            .findFirst();
    assertTrue(relOpt.isEmpty());
  }

  @Test
  void shouldNotReturnTrackedEntityRelationshipWhenEnrollmentItemNotAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);
    OrganisationUnit inaccessibleOrgUnit = createOrganisationUnit('D');
    manager.save(inaccessibleOrgUnit);
    makeProgramMetadataInaccessible(programC);
    trackerOwnershipManager.assignOwnership(
        trackedEntityB, programB, inaccessibleOrgUnit, true, true);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipD.getUid()))
            .findFirst();
    assertTrue(relOpt.isEmpty());
  }

  @Test
  void shouldReturnTrackedEntityRelationshipWhenEventItemAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);
    makeProgramMetadataInaccessible(programC);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipE.getUid()))
            .findFirst();
    assertTrue(relOpt.isEmpty());
  }

  @Test
  void shouldReturnTrackedEntityRelationshipWhenEventItemNotAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);
    OrganisationUnit inaccessibleOrgUnit = createOrganisationUnit('D');
    manager.save(inaccessibleOrgUnit);
    makeProgramMetadataInaccessible(programC);
    trackerOwnershipManager.assignOwnership(
        trackedEntityB, programB, inaccessibleOrgUnit, true, true);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    TrackedEntity trackedEntity = trackedEntities.get(0);
    Optional<RelationshipItem> relOpt =
        trackedEntity.getRelationshipItems().stream()
            .filter(i -> i.getRelationship().getUid().equals(relationshipE.getUid()))
            .findFirst();
    assertTrue(relOpt.isEmpty());
  }

  @Test
  void returnTrackedEntityRelationshipsWithTe2Enrollment()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params = new TrackedEntityParams(true, FALSE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntities(trackedEntityA)
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
  void shouldReturnTrackedEntityRelationshipsWithTe2Event()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityParams params = new TrackedEntityParams(true, TRUE, false, false);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntities(trackedEntityA)
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

  @Test
  void shouldReturnAllEntitiesWhenSuperuserAndNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(superuser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .program(programA)
            .filter(teaC, List.of(new QueryFilter(QueryOperator.LIKE, "C")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldReturnAllEntitiesByTrackedEntityTypeMatchingFilterWhenAuthorizedUserNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(authorizedUser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .trackedEntityType(trackedEntityTypeA)
            .filter(teaA, List.of(new QueryFilter(QueryOperator.LIKE, "A")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityChildA, trackedEntityGrandchildA), trackedEntities);
  }

  @Test
  void shouldReturnAllEntitiesEnrolledInProgramMatchingFilterWhenAuthorizedUserNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(authorizedUser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .program(programB)
            .filter(teaA, List.of(new QueryFilter(QueryOperator.LIKE, "A")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void shouldFailWhenModeAllUserCanSearchEverywhereButNotSuperuserAndNoAccessToProgram() {
    injectSecurityContextUser(userWithSearchInAllAuthority);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().orgUnitMode(ALL).program(programC).build();

    ForbiddenException ex =
        assertThrows(
            ForbiddenException.class,
            () -> trackedEntityService.getTrackedEntities(operationParams));

    assertContains(
        String.format("User has no access to program: %s", programC.getUid()), ex.getMessage());
  }

  @Test
  void shouldReturnChildrenOfRootOrgUnitWhenOrgUnitModeChildren()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(CHILDREN)
            .organisationUnits(orgUnitA)
            .trackedEntityType(trackedEntityTypeA)
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
            .organisationUnits(orgUnitChildA)
            .trackedEntityType(trackedEntityTypeA)
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
            .organisationUnits(orgUnitA, orgUnitChildA)
            .trackedEntityType(trackedEntityTypeA)
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
    injectSecurityContextUser(superuser);
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().orgUnitMode(ALL).program(programA).build();

    List<TrackedEntity> trackedEntities = trackedEntityService.getTrackedEntities(operationParams);
    assertContainsOnly(Set.of(trackedEntityA.getUid()), uids(trackedEntities));
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndProvidedProgramDoesNotExist() {
    String programUid = "madeUpPrUid";
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), UID.of(programUid), TrackedEntityParams.TRUE));
    assertEquals(
        String.format("Program with id %s could not be found.", programUid),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndProvidedTEDoesNotExist() {
    String notExistentTE = CodeGenerator.generateUid();
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(notExistentTE), UID.of(programA), TrackedEntityParams.TRUE));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", notExistentTE),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoDataAccessToProvidedProgram() {
    injectAdminIntoSecurityContext();
    Program inaccessibleProgram = createProgram('U', new HashSet<>(), orgUnitA);
    manager.save(inaccessibleProgram, false);
    makeProgramMetadataAccessibleOnly(inaccessibleProgram);
    manager.update(inaccessibleProgram);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), UID.of(inaccessibleProgram), TrackedEntityParams.TRUE));
    assertContains(
        String.format("User has no data read access to program: %s", inaccessibleProgram.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndTETNotAccessible() {
    TrackedEntityType inaccessibleTrackedEntityType = createTrackedEntityType('U');
    inaccessibleTrackedEntityType.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.save(inaccessibleTrackedEntityType, false);
    TrackedEntity trackedEntity = createTrackedEntity(orgUnitA);
    trackedEntity.setTrackedEntityType(inaccessibleTrackedEntityType);
    manager.save(trackedEntity);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntity), UID.of(programA), TrackedEntityParams.TRUE));
    assertContains(
        String.format(
            "User has no data read access to tracked entity type: %s",
            inaccessibleTrackedEntityType.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoMetadataAccessToAnyProgram() {
    injectAdminIntoSecurityContext();
    makeProgramMetadataInaccessible(programA);
    makeProgramMetadataInaccessible(programB);
    makeProgramMetadataInaccessible(programC);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityParams.TRUE));
    assertContains(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndOnlyEventProgramAccessible() {
    injectAdminIntoSecurityContext();
    makeProgramMetadataInaccessible(programA);
    makeProgramMetadataInaccessible(programB);
    makeProgramMetadataInaccessible(programC);
    Program eventProgram = createProgramWithoutRegistration('E');
    manager.save(eventProgram, false);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityParams.TRUE));

    assertContains(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndTETDoesNotMatchAnyProgram() {
    injectAdminIntoSecurityContext();
    TrackedEntityType trackedEntityType = createTrackedEntityType('T');
    manager.save(trackedEntityType, false);
    programA.setTrackedEntityType(trackedEntityType);
    manager.update(programA);
    programB.setTrackedEntityType(trackedEntityType);
    manager.update(programB);
    programC.setTrackedEntityType(trackedEntityType);
    manager.update(programC);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityParams.TRUE));

    assertContains(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoDataAccessToAnyProgram() {
    injectAdminIntoSecurityContext();
    makeProgramMetadataAccessibleOnly(programA);
    makeProgramMetadataAccessibleOnly(programB);
    makeProgramMetadataAccessibleOnly(programC);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityParams.TRUE));

    assertContains(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoAccessToTET() {
    injectAdminIntoSecurityContext();
    trackedEntityTypeA.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.update(trackedEntityA);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityParams.TRUE));

    assertEquals(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldReturnProgramAttributesWhenSingleTERequestedAndProgramSpecified()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityParams.TRUE);

    assertContainsOnly(
        Set.of(
            trackedEntityAttributeValueA,
            trackedEntityAttributeValueB,
            trackedEntityAttributeValueC),
        trackedEntity.getTrackedEntityAttributeValues());
  }

  @Test
  void shouldReturnTrackedEntityTypeAttributesWhenSingleTERequestedAndNoProgramSpecified()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), null, TrackedEntityParams.TRUE);

    assertContainsOnly(
        Set.of(trackedEntityAttributeValueA, trackedEntityAttributeValueB),
        trackedEntity.getTrackedEntityAttributeValues());
  }

  @Test
  void
      shouldFindTrackedEntityWhenCaptureScopeIndependentFromSearchScopeAndCaptureScopeOrgUnitRequested()
          throws ForbiddenException, NotFoundException, BadRequestException {
    injectAdminIntoSecurityContext();
    programA.setAccessLevel(AccessLevel.OPEN);
    manager.update(programA);

    User testUser = createAndAddUser(false, "testUser", emptySet(), emptySet(), "F_EXPORT_DATA");
    testUser.setOrganisationUnits(Set.of(orgUnitA));
    testUser.setTeiSearchOrganisationUnits(Set.of(orgUnitB));
    manager.update(testUser);
    injectSecurityContext(UserDetails.fromUser(testUser));

    assertEquals(
        trackedEntityA,
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityParams.TRUE));
  }

  private Set<String> attributeNames(final Collection<TrackedEntityAttributeValue> attributes) {
    // depends on createTrackedEntityAttribute() prefixing with "Attribute"
    return attributes.stream()
        .map(a -> StringUtils.removeStart(a.getAttribute().getName(), "Attribute"))
        .collect(Collectors.toSet());
  }

  protected ProgramStage createProgramStage(Program program) {
    ProgramStage programStage = createProgramStage('1', program);
    programStage.setUid(generateUid());
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

  private void makeProgramMetadataAccessibleOnly(Program program) {
    program.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.update(program);
  }

  private void makeProgramMetadataInaccessible(Program program) {
    program.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.update(program);
  }

  private TrackedEntityOperationParams createOperationParams(
      OrganisationUnit orgUnit, TrackedEntity trackedEntity) {
    return TrackedEntityOperationParams.builder()
        .organisationUnits(orgUnit)
        .orgUnitMode(SELECTED)
        .trackedEntities(trackedEntity)
        .trackedEntityParams(new TrackedEntityParams(true, TRUE, false, false))
        .build();
  }
}
