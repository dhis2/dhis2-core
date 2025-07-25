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
package org.hisp.dhis.tracker.export.relationship;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
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
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipServiceTest extends PostgresIntegrationTestBase {

  @Autowired protected UserService _userService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private TrackedEntity teA;

  private TrackedEntity teB;

  private TrackedEntity inaccessibleTe;

  private TrackerEvent eventA;

  private TrackerEvent inaccessibleEvent;

  private final RelationshipType teToTeType = createRelationshipType('A');

  private final RelationshipType teToEnType = createRelationshipType('B');

  private final RelationshipType teToEvType = createRelationshipType('C');

  private final RelationshipType teToInaccessibleTeType = createRelationshipType('D');

  private final RelationshipType teToInaccessibleEnType = createRelationshipType('E');

  private final RelationshipType eventToEventType = createRelationshipType('F');

  private Enrollment enrollmentA;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private User user;

  private ProgramStage programStage;

  private TrackedEntityType trackedEntityType;

  @BeforeAll
  void setUp() {
    Date enrollmentDate = new Date();

    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);

    orgUnitB = createOrganisationUnit('B');
    manager.save(orgUnitB, false);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA));

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType, false);

    TrackedEntityType inaccessibleTrackedEntityType = createTrackedEntityType('B');
    inaccessibleTrackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(inaccessibleTrackedEntityType, false);

    teA = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(teA, false);

    teB = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(teB, false);

    inaccessibleTe = createTrackedEntity(orgUnitA, inaccessibleTrackedEntityType);
    manager.save(inaccessibleTe, false);

    Program program = createProgram('A', new HashSet<>(), orgUnitA);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);
    programStage = createProgramStage('A', program);
    manager.save(programStage, false);
    ProgramStage inaccessibleProgramStage = createProgramStage('B', program);
    inaccessibleProgramStage.setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(inaccessibleProgramStage, false);
    program.setProgramStages(Set.of(programStage, inaccessibleProgramStage));
    manager.save(program, false);

    enrollmentA = createEnrollment(program, teA, orgUnitA);
    manager.save(enrollmentA);
    teA.getEnrollments().add(enrollmentA);
    manager.update(teA);

    eventA = createEvent(programStage, enrollmentA, orgUnitA);
    eventA.setOccurredDate(enrollmentDate);
    manager.save(eventA);

    Enrollment enrollmentB = createEnrollment(program, teB, orgUnitA);
    manager.save(enrollmentB);
    teA.getEnrollments().add(enrollmentB);
    manager.update(teA);
    inaccessibleEvent = createEvent(inaccessibleProgramStage, enrollmentB, orgUnitA);
    inaccessibleEvent.setOccurredDate(enrollmentDate);
    manager.save(inaccessibleEvent);

    teToTeType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToTeType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToTeType.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToTeType.getToConstraint().setTrackedEntityType(trackedEntityType);
    manager.save(teToTeType, false);

    teToInaccessibleTeType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToInaccessibleTeType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToInaccessibleTeType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToInaccessibleTeType.getToConstraint().setTrackedEntityType(inaccessibleTrackedEntityType);
    manager.save(teToInaccessibleTeType, false);

    teToEnType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToEnType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToEnType.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    teToEnType.getToConstraint().setProgram(program);
    manager.save(teToEnType, false);

    teToInaccessibleEnType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToInaccessibleEnType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToInaccessibleEnType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    teToInaccessibleEnType.getToConstraint().setProgram(program);
    teToInaccessibleEnType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(teToInaccessibleEnType, false);

    teToEvType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToEvType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToEvType.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    teToEvType.getToConstraint().setProgramStage(programStage);
    manager.save(teToEvType, false);

    eventToEventType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    eventToEventType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    eventToEventType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    eventToEventType.getToConstraint().setProgramStage(programStage);
    manager.save(eventToEventType, false);

    injectSecurityContextUser(user);
  }

  @Test
  void shouldNotReturnRelationshipByTrackedEntityIfUserHasNoAccessToTrackedEntityType()
      throws ForbiddenException, NotFoundException, BadRequestException {

    Relationship accessible = relationship(teA, teB);
    relationship(teA, inaccessibleTe, teToInaccessibleTeType);

    RelationshipOperationParams operationParams = RelationshipOperationParams.builder(teA).build();

    List<Relationship> relationships = relationshipService.findRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()), relationships.stream().map(Relationship::getUid).toList());
  }

  @Test
  void shouldNotReturnRelationshipByEnrollmentIfUserHasNoAccessToRelationshipType()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Relationship accessible = relationship(teA, enrollmentA);
    relationship(teB, enrollmentA, teToInaccessibleEnType);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder(enrollmentA).build();

    List<Relationship> relationships = relationshipService.findRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()), relationships.stream().map(Relationship::getUid).toList());
  }

  @Test
  void shouldNotReturnRelationshipByEventIfUserHasNoAccessToProgramStage()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Relationship accessible = relationship(teA, eventA);
    relationship(eventA, inaccessibleEvent);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder(eventA).build();

    List<Relationship> relationships = relationshipService.findRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()), relationships.stream().map(Relationship::getUid).toList());
  }

  @Test
  void shouldNotReturnRelationshipWhenTeIsTransferredAndUserHasNoAccessToAtLeastOneProgram()
      throws BadRequestException {
    injectAdminIntoSecurityContext();

    TrackedEntityType trackedEntityType = createTrackedEntityType('X');
    manager.save(trackedEntityType, false);

    Program program = protectedProgram('P', trackedEntityType, orgUnitA);
    program.getSharing().setOwner(user); // set metadata access to the program
    program.setProgramStages(Set.of(programStage));
    program.setOrganisationUnits(Set.of(orgUnitA, orgUnitB));
    manager.save(program, false);

    TrackedEntity trackedEntityFrom = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityFrom);

    manager.save(createEnrollment(program, trackedEntityFrom, orgUnitA));

    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntityFrom, program, orgUnitA);

    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner(
        trackedEntityFrom, program, orgUnitB);

    TrackedEntity trackedEntityTo = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityTo);

    relationship(trackedEntityFrom, trackedEntityTo);

    injectSecurityContextUser(user);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder(trackedEntityFrom).build();

    assertThrows(
        ForbiddenException.class,
        () -> relationshipService.findRelationships(operationParams),
        "User should not have access to a relationship in case of ownership transfer of a tracked entity");
  }

  @Test
  void shouldExcludeRelationshipWhenProgramIsProtectedAndUserHasNoAccess()
      throws ForbiddenException, NotFoundException, BadRequestException {
    injectAdminIntoSecurityContext();

    TrackedEntity trackedEntityFrom = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityFrom);

    TrackedEntity trackedEntityTo = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityTo);

    Program inaccessibleProgram = protectedProgram('P', trackedEntityType, orgUnitB);
    manager.save(inaccessibleProgram, false);

    TrackedEntity notAccessibleTe = createTrackedEntity(orgUnitB, trackedEntityType);
    manager.save(notAccessibleTe);

    injectSecurityContextUser(user);

    Relationship accessible = relationship(trackedEntityFrom, trackedEntityTo);
    relationship(trackedEntityFrom, notAccessibleTe);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder(trackedEntityFrom).build();

    List<Relationship> relationships = relationshipService.findRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()), relationships.stream().map(Relationship::getUid).toList());
  }

  @Test
  void shouldNotReturnRelationshipWhenUserHasNoMetadataAccessToProgram() {
    User admin = getAdminUser();
    injectSecurityContextUser(admin);

    TrackedEntityType trackedEntityType = createTrackedEntityType('Y');
    manager.save(trackedEntityType, false);

    Program program = createProgram('Y', new HashSet<>(), orgUnitA);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);
    program.getSharing().setOwner(admin);
    program.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    program.setProgramStages(Set.of(programStage));
    manager.save(program, false);

    TrackedEntity trackedEntityFrom = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityFrom);

    TrackedEntity trackedEntityTo = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityTo);

    relationship(trackedEntityFrom, trackedEntityTo);

    injectSecurityContextUser(user);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder(trackedEntityFrom).build();

    assertThrows(
        ForbiddenException.class,
        () -> relationshipService.findRelationships(operationParams),
        "User should not have access to a relationship in case of missing metadata access to at least one program");
  }

  @Test
  void shouldNotReturnRelationshipWhenUserHasNoDataReadAccessToProgram() {
    User admin = getAdminUser();
    injectSecurityContextUser(admin);

    TrackedEntityType trackedEntityType = createTrackedEntityType('Y');
    manager.save(trackedEntityType, false);

    Program program = createProgram('Y', new HashSet<>(), orgUnitA);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);
    program.getSharing().setPublicAccess(AccessStringHelper.READ_WRITE);
    program.setProgramStages(Set.of(programStage));
    manager.save(program, false);

    TrackedEntity trackedEntityFrom = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityFrom);

    TrackedEntity trackedEntityTo = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityTo);

    relationship(trackedEntityFrom, trackedEntityTo);

    injectSecurityContextUser(user);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder(trackedEntityFrom).build();

    assertThrows(
        ForbiddenException.class,
        () -> relationshipService.findRelationships(operationParams),
        "User should not have access to a relationship in case of missing data read access to at least one program");
  }

  private Program protectedProgram(
      char p, TrackedEntityType trackedEntityType, OrganisationUnit unit) {
    Program program = createProgram(p, new HashSet<>(), unit);
    program.setTrackedEntityType(trackedEntityType);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setAccessLevel(AccessLevel.PROTECTED);
    return program;
  }

  private Relationship relationship(TrackedEntity from, TrackedEntity to) {
    return relationship(from, to, teToTeType, new Date());
  }

  private Relationship relationship(TrackedEntity from, TrackedEntity to, RelationshipType type) {
    return relationship(from, to, type, new Date());
  }

  private Relationship relationship(
      TrackedEntity from, TrackedEntity to, RelationshipType type, Date createdAtClient) {
    Relationship relationship = new Relationship();
    relationship.setUid(CodeGenerator.generateUid());
    relationship.setRelationshipType(type);
    relationship.setFrom(item(from));
    relationship.setTo(item(to));
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));
    relationship.setCreatedAtClient(createdAtClient);
    manager.save(relationship);

    return relationship;
  }

  private Relationship relationship(TrackedEntity from, Enrollment to) {
    return relationship(from, to, teToEvType);
  }

  private Relationship relationship(TrackedEntity from, Enrollment to, RelationshipType type) {
    Relationship relationship = new Relationship();
    relationship.setUid(CodeGenerator.generateUid());
    relationship.setRelationshipType(type);
    relationship.setFrom(item(from));
    relationship.setTo(item(to));
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    manager.save(relationship);

    return relationship;
  }

  private Relationship relationship(TrackedEntity from, TrackerEvent to) {
    return relationship(from, to, teToEvType, new Date());
  }

  private Relationship relationship(
      TrackedEntity from, TrackerEvent to, RelationshipType type, Date createdAtClient) {
    Relationship relationship = new Relationship();
    relationship.setUid(CodeGenerator.generateUid());
    relationship.setRelationshipType(type);
    relationship.setFrom(item(from));
    relationship.setTo(item(to));
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));
    relationship.setCreatedAtClient(createdAtClient);

    manager.save(relationship);

    return relationship;
  }

  private void relationship(TrackerEvent from, TrackerEvent to) {
    relationship(from, to, eventToEventType);
  }

  private void relationship(TrackerEvent from, TrackerEvent to, RelationshipType type) {
    Relationship relationship = new Relationship();
    relationship.setUid(CodeGenerator.generateUid());
    relationship.setRelationshipType(type);
    relationship.setFrom(item(from));
    relationship.setTo(item(to));
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    manager.save(relationship);
  }

  private RelationshipItem item(TrackedEntity from) {
    RelationshipItem relationshipItem = new RelationshipItem();
    relationshipItem.setTrackedEntity(from);
    return relationshipItem;
  }

  private RelationshipItem item(Enrollment from) {
    RelationshipItem relationshipItem = new RelationshipItem();
    relationshipItem.setEnrollment(from);
    return relationshipItem;
  }

  private RelationshipItem item(TrackerEvent from) {
    RelationshipItem relationshipItem = new RelationshipItem();
    relationshipItem.setEvent(from);
    return relationshipItem;
  }
}
