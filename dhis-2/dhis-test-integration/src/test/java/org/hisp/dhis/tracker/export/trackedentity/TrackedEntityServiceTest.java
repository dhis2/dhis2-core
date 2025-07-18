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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.tracker.Assertions.assertNotes;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.oneHourBefore;
import static org.hisp.dhis.tracker.TrackerTestUtils.twoHoursAfter;
import static org.hisp.dhis.tracker.TrackerTestUtils.twoHoursBefore;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
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
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentFields;
import org.hisp.dhis.tracker.export.relationship.RelationshipFields;
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

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private User user;

  private User userWithSearchInAllAuthority;

  private User superuser;

  private User authorizedUser;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private OrganisationUnit orgUnitChildA;

  private TrackedEntityAttribute teaA;

  private TrackedEntityAttribute teaB;

  private TrackedEntityAttribute teaC;

  private TrackedEntityAttributeValue tetavA;

  private TrackedEntityAttributeValue tetavB;

  private TrackedEntityAttributeValue pteavC;

  private TrackedEntityType trackedEntityTypeA;

  private Program programA;

  private Program programB;

  private Program programC;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private ProgramStage programStageA1;

  private TrackerEvent eventA;

  private TrackerEvent eventB;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntity trackedEntityChildA;

  private TrackedEntity trackedEntityGrandchildA;

  private CategoryOptionCombo defaultCategoryOptionCombo;

  private Relationship relationshipA;

  private Relationship relationshipB;

  private Relationship relationshipC;

  private Relationship relationshipD;

  private Relationship relationshipE;

  private static List<String> uids(Collection<? extends IdentifiableObject> identifiableObjects) {
    return identifiableObjects.stream().map(IdentifiableObject::getUid).toList();
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
    teaB = createTrackedEntityAttribute('B', ValueType.TEXT);
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
    trackedEntityTypeA.setMinAttributesRequiredToSearch(0);
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
    programStageA1 = createProgramStage(programA);
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
    programB.setAccessLevel(PROTECTED);
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
    programC.setAccessLevel(CLOSED);
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

    tetavA = new TrackedEntityAttributeValue(teaA, trackedEntityA, "A");
    tetavB = new TrackedEntityAttributeValue(teaB, trackedEntityA, "B");
    pteavC = new TrackedEntityAttributeValue(teaC, trackedEntityA, "C");

    trackedEntityA = createTrackedEntity(orgUnitA, trackedEntityTypeA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    trackedEntityA.setTrackedEntityAttributeValues(Set.of(tetavA, tetavB, pteavC));

    manager.save(trackedEntityA, false);

    trackedEntityChildA = createTrackedEntity(orgUnitChildA, trackedEntityTypeA);
    manager.save(trackedEntityChildA, false);

    trackedEntityGrandchildA = createTrackedEntity(orgUnitGrandchildA, trackedEntityTypeA);
    manager.save(trackedEntityGrandchildA, false);

    enrollmentA = createEnrollment(programA, trackedEntityA, orgUnitA);
    manager.save(enrollmentA);
    trackedEntityA.getEnrollments().add(enrollmentA);
    manager.update(trackedEntityA);

    eventA = new TrackerEvent();
    eventA.setEnrollment(enrollmentA);
    eventA.setProgramStage(programStageA1);
    eventA.setOrganisationUnit(orgUnitA);
    eventA.setAttributeOptionCombo(defaultCategoryOptionCombo);
    eventA.setOccurredDate(parseDate("2021-05-27T12:05:00.000"));
    eventA.setScheduledDate(parseDate("2021-02-27T12:05:00.000"));
    eventA.setCompletedDate(parseDate("2021-02-27T11:05:00.000"));
    eventA.setCompletedBy("herb");
    eventA.setAssignedUser(user);
    Note note = new Note("note1", "ant");
    note.setUid(generateUid());
    note.setCreated(new Date());
    note.setLastUpdated(new Date());
    eventA.setNotes(List.of(note));
    manager.save(eventA, false);
    enrollmentA.setEvents(Set.of(eventA));
    enrollmentA.setFollowup(true);
    manager.save(enrollmentA, false);

    enrollmentB = createEnrollment(programB, trackedEntityA, orgUnitB);
    manager.save(enrollmentB);
    trackedEntityA.getEnrollments().add(enrollmentB);
    manager.update(trackedEntityA);

    eventB = new TrackerEvent();
    eventB.setEnrollment(enrollmentB);
    eventB.setProgramStage(programStageB1);
    eventB.setOrganisationUnit(orgUnitA);
    eventB.setAttributeOptionCombo(defaultCategoryOptionCombo);
    manager.save(eventB, false);
    enrollmentB.setEvents(Set.of(eventB));
    manager.save(enrollmentB, false);

    trackedEntityB = createTrackedEntity(orgUnitB, trackedEntityTypeA);
    manager.save(trackedEntityB, false);

    Enrollment enrollmentC = createEnrollment(programB, trackedEntityB, orgUnitB);
    manager.save(enrollmentC);
    trackedEntityA.getEnrollments().add(enrollmentC);
    manager.update(trackedEntityB);

    TrackerEvent eventC = new TrackerEvent();
    eventC.setEnrollment(enrollmentC);
    eventC.setProgramStage(programStageB1);
    eventC.setOrganisationUnit(orgUnitB);
    eventC.setAttributeOptionCombo(defaultCategoryOptionCombo);
    manager.save(eventC, false);
    enrollmentC.setEvents(Set.of(eventC));
    manager.save(enrollmentC, false);

    TrackedEntity trackedEntityC = createTrackedEntity(orgUnitC, trackedEntityTypeA);
    manager.save(trackedEntityC, false);

    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityA, programA, orgUnitA);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityA, programB, orgUnitA);

    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaA, trackedEntityA, "A"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaA, trackedEntityChildA, "CA"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(teaA, trackedEntityGrandchildA, "GCA"));
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

    makeTrackedEntityTypeDataInaccessible(trackedEntityTypeA);
    trackedEntityTypeA.getSharing().setOwner(superuser);
    manager.updateNoAcl(trackedEntityA);

    ForbiddenException ex =
        assertThrows(
            ForbiddenException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams));
    assertContains(
        "User is not authorized to read data from selected tracked entity type: "
            + trackedEntityTypeA.getUid(),
        ex.getMessage());
  }

  @Test
  void shouldReturnEmptyCollectionWhenUserHasNoAccessToProgramTrackedEntityType() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .program(programA)
            .build();

    makeTrackedEntityTypeDataInaccessible(trackedEntityTypeA);
    manager.updateNoAcl(trackedEntityA);

    ForbiddenException ex =
        assertThrows(
            ForbiddenException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams));
    assertStartsWith(
        "User is not authorized to read data from selected program's tracked entity type",
        ex.getMessage());
  }

  @Test
  void
      shouldReturnEmptyCollectionWhenRequestingCollectionTrackedEntitiesAndProgramSpecifiedIsNotAccessible() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .program(programA)
            .build();

    injectSecurityContextUser(superuser);
    makeProgramMetadataInaccessible(programA);
    manager.updateNoAcl(programA);
    injectSecurityContextUser(user);

    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () -> trackedEntityService.findTrackedEntities(operationParams));
    assertStartsWith("Program is specified but does not exist", ex.getMessage());
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
        trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityB, trackedEntityChildA, trackedEntityGrandchildA),
        trackedEntities);
  }

  @Test
  void shouldReturnTrackedEntitiesWithCorrectOrgUnitPropertiesMappedWhenTrackedEntityAccessible()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitChildA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityChildA), trackedEntities);
    assertEquals(
        trackedEntityChildA.getOrganisationUnit().getUid(),
        trackedEntities.get(0).getOrganisationUnit().getUid());
    assertEquals(
        trackedEntityChildA.getOrganisationUnit().getCode(),
        trackedEntities.get(0).getOrganisationUnit().getCode());
    assertEquals(
        trackedEntityChildA.getOrganisationUnit().getName(),
        trackedEntities.get(0).getOrganisationUnit().getName());
    assertEquals(
        trackedEntityChildA.getOrganisationUnit().getStoredPath(),
        trackedEntities.get(0).getOrganisationUnit().getStoredPath());
  }

  @Test
  void shouldReturnTrackedEntityIncludingAllAttributesEnrollmentsEventsRelationshipsOwners()
      throws ForbiddenException, NotFoundException, BadRequestException {
    // this was declared as "remove ownership"; unclear to me how this is removing ownership
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntityA, programB, orgUnitB);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .fields(TrackedEntityFields.all())
            .build();

    TrackedEntity te =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityFields.all());
    assertContainsOnly(Set.of(enrollmentA.getUid()), uids(te.getEnrollments()));

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.findTrackedEntities(operationParams);

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
  void shouldReturnTrackedEntityIncludingAllEnrollments()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .fields(TrackedEntityFields.all())
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA.getUid()), uids(trackedEntities));
    assertContainsOnly(
        Set.of(enrollmentA.getUid(), enrollmentB.getUid()),
        uids(trackedEntities.get(0).getEnrollments()));
    assertEquals(
        orgUnitA.getUid(),
        trackedEntities.get(0).getEnrollments().stream()
            .filter(e -> e.getUid().equals(enrollmentA.getUid()))
            .findFirst()
            .get()
            .getOrganisationUnit()
            .getUid());
    assertEquals(
        orgUnitB.getUid(),
        trackedEntities.get(0).getEnrollments().stream()
            .filter(e -> e.getUid().equals(enrollmentB.getUid()))
            .findFirst()
            .get()
            .getOrganisationUnit()
            .getUid());
  }

  @Test
  void shouldReturnTrackedEntityIncludeSpecificProtectedProgram()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .program(programB)
            .fields(TrackedEntityFields.builder().includeAttributes().build())
            .build();

    final List<TrackedEntity> trackedEntities =
        trackedEntityService.findTrackedEntities(operationParams);

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
            .fields(TrackedEntityFields.builder().includeAttributes().build())
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities, TrackedEntity::getUid);
    assertContainsOnly(
        Set.of("A", "B", "C"),
        attributeNames(trackedEntities.get(0).getTrackedEntityAttributeValues()));
  }

  @Test
  void shouldReturnEnrollmentsFromSpecifiedProgramWhenRequestingSingleTrackedEntity()
      throws ForbiddenException, NotFoundException {
    Enrollment enrollmentProgramB = createEnrollment(programB, trackedEntityA, orgUnitA);
    manager.save(enrollmentProgramB);
    trackedEntityA.getEnrollments().add(enrollmentProgramB);
    manager.update(trackedEntityA);

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityFields.all());

    assertContainsOnly(Set.of(enrollmentA), trackedEntity.getEnrollments(), Enrollment::getUid);
  }

  @Test
  void shouldReturnAllEnrollmentsWhenRequestingSingleTrackedEntityAndNoProgramSpecified()
      throws ForbiddenException, NotFoundException {
    Enrollment enrollmentProgramB = createEnrollment(programB, trackedEntityA, orgUnitA);
    manager.save(enrollmentProgramB);
    trackedEntityA.getEnrollments().add(enrollmentProgramB);
    manager.update(trackedEntityA);

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), null, TrackedEntityFields.all());

    assertContainsOnly(
        Set.of(enrollmentA, enrollmentB, enrollmentProgramB),
        trackedEntity.getEnrollments(),
        Enrollment::getUid);
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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertIsEmpty(trackedEntities);
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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    assertIsEmpty(trackedEntityService.findTrackedEntities(operationParams));
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
        trackedEntityService.findTrackedEntities(
            builder.eventStatus(EventStatus.COMPLETED).build());
    assertEquals(4, trackedEntities.size());
    // Update status to active
    final List<TrackedEntity> limitedTrackedEntities =
        trackedEntityService.findTrackedEntities(builder.eventStatus(EventStatus.ACTIVE).build());
    assertIsEmpty(limitedTrackedEntities);
    // Update status to overdue
    final List<TrackedEntity> limitedTrackedEntities2 =
        trackedEntityService.findTrackedEntities(builder.eventStatus(EventStatus.OVERDUE).build());
    assertIsEmpty(limitedTrackedEntities2);
    // Update status to schedule
    final List<TrackedEntity> limitedTrackedEntities3 =
        trackedEntityService.findTrackedEntities(builder.eventStatus(EventStatus.OVERDUE).build());
    assertIsEmpty(limitedTrackedEntities3);
    // Update status to schedule
    final List<TrackedEntity> limitedTrackedEntities4 =
        trackedEntityService.findTrackedEntities(builder.eventStatus(EventStatus.SCHEDULE).build());
    assertIsEmpty(limitedTrackedEntities4);
    // Update status to visited
    final List<TrackedEntity> limitedTrackedEntities5 =
        trackedEntityService.findTrackedEntities(builder.eventStatus(EventStatus.VISITED).build());
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
            .fields(TrackedEntityFields.all())
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    TrackedEntity trackedEntity = trackedEntities.get(0);
    Set<String> deletedEnrollments =
        trackedEntity.getEnrollments().stream()
            .filter(Enrollment::isDeleted)
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertIsEmpty(deletedEnrollments);
    Set<String> deletedEvents =
        trackedEntity.getEnrollments().stream()
            .flatMap(enrollment -> enrollment.getEvents().stream())
            .filter(TrackerEvent::isDeleted)
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertIsEmpty(deletedEvents);

    manager.delete(enrollmentA);
    manager.delete(eventA);

    trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    trackedEntity = trackedEntities.get(0);

    assertContainsOnly(
        Set.of(enrollmentA.getUid(), enrollmentB.getUid()), uids(trackedEntity.getEnrollments()));
    deletedEnrollments =
        trackedEntity.getEnrollments().stream()
            .filter(Enrollment::isDeleted)
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(enrollmentA.getUid()), deletedEnrollments);

    Set<TrackerEvent> events =
        trackedEntity.getEnrollments().stream()
            .flatMap(e -> e.getEvents().stream())
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(eventA.getUid(), eventB.getUid()), uids(events));
    deletedEvents =
        events.stream()
            .filter(TrackerEvent::isDeleted)
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());
    assertContainsOnly(Set.of(eventA.getUid()), deletedEvents);

    operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .trackedEntityType(trackedEntityTypeA)
            .includeDeleted(false)
            .fields(TrackedEntityFields.all())
            .build();
    trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntities(trackedEntityA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
    assertIsEmpty(trackedEntities.get(0).getEnrollments());
  }

  @Test
  void shouldReturnTrackedEntityWithEventsAndNotesGivenTheyShouldBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntities(trackedEntityA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities, UidObject::getUid);
    assertContainsOnly(
        Set.of(enrollmentA, enrollmentB),
        trackedEntities.get(0).getEnrollments(),
        UidObject::getUid);
    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentA =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(this.enrollmentA.getUid()))
            .findFirst();
    Set<TrackerEvent> events = enrollmentA.get().getEvents();
    assertContainsOnly(Set.of(eventA), events, UidObject::getUid);
    assertNotes(eventA.getNotes(), events.stream().findFirst().get().getNotes());
  }

  @Test
  void shouldReturnTrackedEntityWithoutEventsGivenTheyShouldNotBeIncluded()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityFields fields =
        TrackedEntityFields.builder()
            .includeEnrollments(
                EnrollmentFields.builder()
                    .includeAttributes()
                    .includeRelationships(RelationshipFields.all())
                    .build())
            .build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
  void shouldReturnTrackedEntityWithoutEventWhenProgramStageNotAccessible()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectAdminIntoSecurityContext();
    programStageA1.setSharing(Sharing.builder().publicAccess("--------").build());
    manager.update(programStageA1);
    injectSecurityContextUser(user);
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .trackedEntities(trackedEntityA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentOpt =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(enrollmentA.getUid()))
            .findFirst();
    assertTrue(enrollmentOpt.isPresent());
    Enrollment enrollment = enrollmentOpt.get();
    assertAll(
        () -> assertEquals(trackedEntityA.getUid(), enrollment.getTrackedEntity().getUid()),
        () -> assertEquals(trackedEntityA.getUid(), enrollment.getTrackedEntity().getUid()),
        () -> assertEquals(orgUnitA.getUid(), enrollment.getOrganisationUnit().getUid()),
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
  void shouldReturnTrackedEntityWithActiveEnrollments()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .enrollmentStatus(EnrollmentStatus.ACTIVE)
            .program(programA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertNotEmpty(trackedEntities);
    trackedEntities.forEach(
        te -> {
          AtomicBoolean hasEnrollment = new AtomicBoolean();
          te.getEnrollments()
              .forEach(
                  enrollment -> {
                    if (EnrollmentStatus.ACTIVE == enrollment.getStatus()) {
                      hasEnrollment.set(true);
                    }
                  });
          assertTrue(
              hasEnrollment.get(),
              "test expects each tracked entity to have at least one enrollment");
        });
  }

  @Test
  void shouldReturnTrackedEntityWithEnrollmentsMarkedForFollowUp()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .followUp(true)
            .program(programA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertNotEmpty(trackedEntities);
    trackedEntities.forEach(
        te -> {
          AtomicBoolean hasEnrollment = new AtomicBoolean();
          te.getEnrollments()
              .forEach(
                  enrollment -> {
                    if (Boolean.TRUE.equals(enrollment.getFollowup())) {
                      hasEnrollment.set(true);
                    }
                  });
          assertTrue(
              hasEnrollment.get(),
              "test expects each tracked entity to have at least one enrollment");
        });
  }

  @Test
  void shouldReturnEventMappedCorrectly()
      throws ForbiddenException, NotFoundException, BadRequestException {
    final Date currentTime = new Date();
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeEnrollments(EnrollmentFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityTypeA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    List<Enrollment> enrollments = new ArrayList<>(trackedEntities.get(0).getEnrollments());
    Optional<Enrollment> enrollmentOpt =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(enrollmentA.getUid()))
            .findFirst();
    assertTrue(enrollmentOpt.isPresent());
    Enrollment enrollment = enrollmentOpt.get();
    Optional<TrackerEvent> eventOpt = enrollment.getEvents().stream().findFirst();
    assertTrue(eventOpt.isPresent());
    TrackerEvent event = eventOpt.get();
    assertAll(
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
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeRelationships(RelationshipFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntities(trackedEntityA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntityA, programA, inaccessibleOrgUnit);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitB, trackedEntityB);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programA, inaccessibleOrgUnit);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programB, inaccessibleOrgUnit);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);

    injectSecurityContextUser(user);
    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programB, inaccessibleOrgUnit);
    TrackedEntityOperationParams operationParams = createOperationParams(orgUnitA, trackedEntityA);
    injectSecurityContextUser(user);

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    TrackedEntityFields fields =
        TrackedEntityFields.builder().includeRelationships(RelationshipFields.all()).build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntities(trackedEntityA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
    TrackedEntityFields fields =
        TrackedEntityFields.builder()
            .includeRelationships(RelationshipFields.all())
            .includeEnrollments(EnrollmentFields.all())
            .build();
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnitA)
            .orgUnitMode(SELECTED)
            .trackedEntities(trackedEntityA)
            .fields(fields)
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

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
            .organisationUnits(orgUnitA, orgUnitB, orgUnitChildA)
            .orgUnitMode(DESCENDANTS)
            .program(programA)
            .filterBy(UID.of(teaC), List.of(new QueryFilter(QueryOperator.LIKE, "C")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);
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
            .filterBy(UID.of(teaA), List.of(new QueryFilter(QueryOperator.LIKE, "A")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityChildA, trackedEntityGrandchildA), trackedEntities);
  }

  @Test
  void
      shouldReturnAllNonPaginatedEntitiesEnrolledInProgramMatchingFilterWhenAuthorizedUserNotInSearchScope()
          throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(authorizedUser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .program(programB)
            .filterBy(UID.of(teaA), List.of(new QueryFilter(QueryOperator.LIKE, "A")))
            .build();

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA), trackedEntities);
  }

  @Test
  void
      shouldReturnAllPaginatedEntitiesEnrolledInProgramMatchingFilterWhenAuthorizedUserNotInSearchScope()
          throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(authorizedUser);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ALL)
            .program(programB)
            .filterBy(UID.of(teaA), List.of(new QueryFilter(QueryOperator.LIKE, "A")))
            .build();

    List<TrackedEntity> trackedEntities =
        trackedEntityService
            .findTrackedEntities(operationParams, PageParams.of(1, 10, false))
            .getItems();

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
            () -> trackedEntityService.findTrackedEntities(operationParams));

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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);
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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);
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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);
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

    List<TrackedEntity> trackedEntities = trackedEntityService.findTrackedEntities(operationParams);
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
                    UID.of(trackedEntityA), UID.of(programUid), TrackedEntityFields.all()));
    assertEquals(
        String.format("Program with id %s could not be found.", programUid),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndProvidedTEDoesNotExist() {
    String notExistentTE = generateUid();
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(notExistentTE), UID.of(programA), TrackedEntityFields.all()));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", notExistentTE),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoDataAccessToProvidedProgram() {
    injectAdminIntoSecurityContext();
    Program inaccessibleProgram = createProgram('U', new HashSet<>(), orgUnitA);
    manager.save(inaccessibleProgram, false);
    makeProgramDataInaccessible(inaccessibleProgram);
    manager.update(inaccessibleProgram);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA),
                    UID.of(inaccessibleProgram),
                    TrackedEntityFields.all()));
    assertContains(
        String.format("User has no access to program: %s", inaccessibleProgram.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndTETNotAccessible() {
    TrackedEntityType inaccessibleTrackedEntityType = createTrackedEntityType('U');
    inaccessibleTrackedEntityType.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.save(inaccessibleTrackedEntityType, false);
    TrackedEntity trackedEntity = createTrackedEntity(orgUnitA, inaccessibleTrackedEntityType);
    manager.save(trackedEntity);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntity), UID.of(programA), TrackedEntityFields.all()));
    assertContains(
        String.format("TrackedEntity with id %s could not be found.", trackedEntity.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldReturnEmptyResultIfUserHasNoAccessToAnyTrackerProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(getAdminUser());
    makeProgramDataInaccessible(programA);
    makeProgramDataInaccessible(programB);
    makeProgramDataInaccessible(programC);
    injectSecurityContextUser(user);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().trackedEntityType(trackedEntityTypeA).build();

    assertIsEmpty(trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void shouldReturnEmptyPageIfUserHasNoAccessToAnyTrackerProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(getAdminUser());
    makeProgramDataInaccessible(programA);
    makeProgramDataInaccessible(programB);
    makeProgramDataInaccessible(programC);
    injectSecurityContextUser(user);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().trackedEntityType(trackedEntityTypeA).build();

    Page<TrackedEntity> trackedEntities =
        trackedEntityService.findTrackedEntities(operationParams, PageParams.of(1, 3, false));

    assertIsEmpty(trackedEntities.getItems());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoMetadataAccessToAnyProgram() {
    injectAdminIntoSecurityContext();
    makeProgramMetadataInaccessible(programA);
    makeProgramMetadataInaccessible(programB);
    makeProgramMetadataInaccessible(programC);

    injectSecurityContextUser(user);
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityFields.all()));
    assertContains(
        String.format("TrackedEntity with id %s could not be found.", trackedEntityA.getUid()),
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
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityFields.all()));

    assertContains(
        String.format("TrackedEntity with id %s could not be found.", trackedEntityA.getUid()),
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
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityFields.all()));

    assertContains(
        String.format("TrackedEntity with id %s could not be found.", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoDataAccessToAnyProgram() {
    injectAdminIntoSecurityContext();
    makeProgramDataInaccessible(programA);
    makeProgramDataInaccessible(programB);
    makeProgramDataInaccessible(programC);

    injectSecurityContextUser(user);
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityFields.all()));

    assertContains(
        String.format("TrackedEntity with id %s could not be found.", trackedEntityA.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleTEAndNoAccessToAnyTET() {
    injectAdminIntoSecurityContext();
    trackedEntityTypeA.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.update(trackedEntityA);

    injectSecurityContextUser(user);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA), null, TrackedEntityFields.all()));

    assertEquals("User has no access to any Tracked Entity Type", exception.getMessage());
  }

  @Test
  void shouldReturnProgramAttributesWhenSingleTERequestedAndProgramSpecified()
      throws ForbiddenException, NotFoundException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityFields.all());

    assertContainsOnly(
        Set.of(tetavA, tetavB, pteavC),
        trackedEntity.getTrackedEntityAttributeValues(),
        TrackedEntityAttributeValue::getValue);
  }

  @Test
  void shouldReturnTrackedEntityTypeAttributesOnlyWhenSingleTERequestedAndNoProgramSpecified()
      throws ForbiddenException, NotFoundException {
    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), null, TrackedEntityFields.all());

    assertContainsOnly(
        Set.of(tetavA, tetavB),
        trackedEntity.getTrackedEntityAttributeValues(),
        TrackedEntityAttributeValue::getValue);
  }

  @Test
  void
      shouldFindTrackedEntityWhenCaptureScopeIndependentFromSearchScopeAndCaptureScopeOrgUnitRequested()
          throws ForbiddenException, NotFoundException {
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
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityFields.all()));
  }

  @Test
  void shouldReturnTrackedEntitiesInCaptureScopeWhenOrgUnitModeCapture()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(CAPTURE)
            .trackedEntityType(trackedEntityTypeA)
            .build();

    assertContainsOnly(
        List.of(trackedEntityA, trackedEntityChildA, trackedEntityGrandchildA),
        trackedEntityService.findTrackedEntities(params));
  }

  @Test
  void shouldFindTrackedEntityWithEventsWhenEventRequestedAndAccessible()
      throws ForbiddenException, NotFoundException {
    injectAdminIntoSecurityContext();
    User testUser = createAndAddUser(false, "testUser", emptySet(), emptySet(), "F_EXPORT_DATA");
    testUser.setOrganisationUnits(Set.of(orgUnitA));
    manager.update(testUser);
    injectSecurityContext(UserDetails.fromUser(testUser));

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityFields.all());

    assertEquals(trackedEntityA.getUid(), trackedEntity.getUid());
    assertContainsOnly(Set.of(enrollmentA.getUid()), uids(trackedEntity.getEnrollments()));
    List<Enrollment> enrollments = new ArrayList<>(trackedEntity.getEnrollments());
    Optional<Enrollment> enrollmentA =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(this.enrollmentA.getUid()))
            .findFirst();
    Set<TrackerEvent> events = enrollmentA.get().getEvents();
    assertContainsOnly(Set.of(eventA.getUid()), uids(events));
  }

  @Test
  void shouldFindTrackedEntityWithoutEventsWhenEventRequestedButNotAccessible()
      throws ForbiddenException, NotFoundException {
    injectAdminIntoSecurityContext();
    programStageA1.setSharing(Sharing.builder().publicAccess("--------").build());
    manager.update(programStageA1);
    User testUser = createAndAddUser(false, "testUser", emptySet(), emptySet(), "F_EXPORT_DATA");
    testUser.setOrganisationUnits(Set.of(orgUnitA));
    manager.update(testUser);
    injectSecurityContext(UserDetails.fromUser(testUser));

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA), UID.of(programA), TrackedEntityFields.all());

    assertEquals(trackedEntityA.getUid(), trackedEntity.getUid());
    assertContainsOnly(Set.of(enrollmentA.getUid()), uids(trackedEntity.getEnrollments()));
    List<Enrollment> enrollments = new ArrayList<>(trackedEntity.getEnrollments());
    Optional<Enrollment> enrollmentA =
        enrollments.stream()
            .filter(enrollment -> enrollment.getUid().equals(this.enrollmentA.getUid()))
            .findFirst();
    assertIsEmpty(enrollmentA.get().getEvents());
  }

  @Test
  void shouldFailWhenRequestingSingleTEEnrolledInOpenProgramWhenUserNotInSearchScope() {
    user.setTeiSearchOrganisationUnits(Set.of(orgUnitB));
    user.setOrganisationUnits(Set.of(orgUnitB));
    injectSecurityContextUser(user);

    assertThrows(
        NotFoundException.class,
        () ->
            trackedEntityService.getTrackedEntity(
                UID.of(trackedEntityA.getUid()),
                UID.of(programA.getUid()),
                TrackedEntityFields.none()));
  }

  @Test
  void shouldGetTrackedEntityWhenRequestingSingleTEEnrolledInProtectedProgramAndUserInCaptureScope()
      throws ForbiddenException, NotFoundException {
    user.setTeiSearchOrganisationUnits(Set.of());
    user.setOrganisationUnits(Set.of(orgUnitB));
    injectSecurityContextUser(user);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programB, orgUnitB);

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityB.getUid()), UID.of(programB.getUid()), TrackedEntityFields.none());

    assertEquals(trackedEntityB.getUid(), trackedEntity.getUid());
  }

  @Test
  void
      shouldFailWhenRequestingSingleTEEnrolledInProtectedProgramWhenUserInSearchScopeButNotInCaptureScope() {
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programB, orgUnitB);

    assertThrows(
        NotFoundException.class,
        () ->
            trackedEntityService.getTrackedEntity(
                UID.of(trackedEntityB.getUid()),
                UID.of(programB.getUid()),
                TrackedEntityFields.none()));
  }

  @Test
  void shouldGetTrackedEntityWhenRequestingSingleTEEnrolledInClosedAndUserInCaptureScope()
      throws ForbiddenException, NotFoundException {
    injectAdminIntoSecurityContext();
    makeProgramDataAndMetadataAccessible(programC);
    Enrollment enrollment = createEnrollment(programC, trackedEntityB, orgUnitB);
    manager.save(enrollment);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programC, orgUnitB);
    user.setTeiSearchOrganisationUnits(Set.of());
    user.setOrganisationUnits(Set.of(orgUnitB));
    injectSecurityContextUser(user);

    TrackedEntity trackedEntity =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityB.getUid()), UID.of(programC.getUid()), TrackedEntityFields.none());

    assertEquals(trackedEntityB.getUid(), trackedEntity.getUid());
  }

  @Test
  void
      shouldFailWhenRequestingSingleTEEnrolledInClosedProgramWhenUserInSearchScopeButNotInCaptureScope() {
    injectAdminIntoSecurityContext();
    makeProgramDataAndMetadataAccessible(programC);
    Enrollment enrollment = createEnrollment(programC, trackedEntityB, orgUnitB);
    manager.save(enrollment);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityB, programC, orgUnitB);
    injectSecurityContextUser(user);

    assertThrows(
        NotFoundException.class,
        () ->
            trackedEntityService.getTrackedEntity(
                UID.of(trackedEntityB.getUid()),
                UID.of(programC.getUid()),
                TrackedEntityFields.none()));
  }

  @Test
  void
      shouldReturnEmptyListWhenRequestingCollectionPaginatedTEWithoutProgramParamAndUserCantEnrollTEAnywhere()
          throws BadRequestException, ForbiddenException, NotFoundException {
    injectAdminIntoSecurityContext();
    programB.setAccessLevel(CLOSED);
    manager.save(programB, false);
    makeProgramMetadataInaccessible(programA);
    makeProgramMetadataInaccessible(programC);
    injectSecurityContextUser(user);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().trackedEntities(trackedEntityB).build();

    assertIsEmpty(
        trackedEntityService
            .findTrackedEntities(operationParams, PageParams.of(1, 10, false))
            .getItems());
  }

  @Test
  void
      shouldReturnEmptyListWhenRequestingCollectionNonPaginatedTEWithoutProgramParamAndUserCantEnrollTEAnywhere()
          throws BadRequestException, ForbiddenException, NotFoundException {
    injectAdminIntoSecurityContext();
    programB.setAccessLevel(CLOSED);
    manager.save(programB, false);
    makeProgramMetadataInaccessible(programA);
    makeProgramMetadataInaccessible(programC);
    injectSecurityContextUser(user);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().trackedEntities(trackedEntityB).build();

    assertIsEmpty(trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoAccessToTETOfRequestedTE()
      throws ForbiddenException, BadRequestException, NotFoundException {
    makeTrackedEntityTypeDataInaccessible(trackedEntityTypeA);
    TrackedEntityType trackedEntityType = createTrackedEntityType('B');
    manager.save(trackedEntityType, false);
    makeTrackedEntityTypeDataAndMetadataAccessible(trackedEntityType);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().trackedEntities(trackedEntityB).build();

    assertIsEmpty(trackedEntityService.findTrackedEntities(operationParams));
  }

  @Test
  void shouldFailWhenRequestingSingleTEWithoutProgramAndUserCantEnrollTEAnywhere() {
    injectAdminIntoSecurityContext();
    programB.setAccessLevel(CLOSED);
    manager.save(programB, false);
    makeProgramMetadataInaccessible(programA);
    makeProgramMetadataInaccessible(programC);
    injectSecurityContextUser(user);

    assertThrows(
        NotFoundException.class,
        () -> trackedEntityService.getTrackedEntity(UID.of(trackedEntityB.getUid())));
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

  private void makeProgramDataAndMetadataAccessible(Program program) {
    program.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    manager.update(program);
  }

  private void makeProgramDataInaccessible(Program program) {
    program.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.update(program);
  }

  private void makeProgramMetadataInaccessible(Program program) {
    program.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.update(program);
  }

  private void makeTrackedEntityTypeDataInaccessible(TrackedEntityType trackedEntityType) {
    injectSecurityContextUser(superuser);
    trackedEntityType.setSharing(Sharing.builder().publicAccess("rw------").build());
    manager.update(trackedEntityType);
    injectSecurityContextUser(user);
  }

  private void makeTrackedEntityTypeDataAndMetadataAccessible(TrackedEntityType trackedEntityType) {
    injectSecurityContextUser(superuser);
    trackedEntityType.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    manager.update(trackedEntityType);
    injectSecurityContextUser(user);
  }

  private TrackedEntityOperationParams createOperationParams(
      OrganisationUnit orgUnit, TrackedEntity trackedEntity) {
    return TrackedEntityOperationParams.builder()
        .organisationUnits(orgUnit)
        .orgUnitMode(SELECTED)
        .trackedEntities(trackedEntity)
        .fields(
            TrackedEntityFields.builder()
                .includeRelationships(RelationshipFields.all())
                .includeEnrollments(EnrollmentFields.all())
                .build())
        .build();
  }
}
