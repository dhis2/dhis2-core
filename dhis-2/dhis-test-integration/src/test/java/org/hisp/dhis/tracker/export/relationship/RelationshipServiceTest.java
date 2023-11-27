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
package org.hisp.dhis.tracker.export.relationship;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.RelationshipUtils;
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
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired protected UserService _userService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private IdentifiableObjectManager manager;

  private TrackedEntity teA;

  private TrackedEntity teB;

  private TrackedEntity inaccessibleTe;

  private Event eventA;

  private Event inaccessiblePsi;

  private final RelationshipType teToTeType = createRelationshipType('A');

  private final RelationshipType teToEnType = createRelationshipType('B');

  private final RelationshipType teToEvType = createRelationshipType('C');

  private final RelationshipType teToInaccessibleTeType = createRelationshipType('D');

  private final RelationshipType teToInaccessibleEnType = createRelationshipType('E');

  private final RelationshipType eventToEventType = createRelationshipType('F');

  private Enrollment enrollmentA;

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;
    User admin = preCreateInjectAdminUser();

    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit, false);

    User user = createAndAddUser(false, "user", Set.of(orgUnit), Set.of(orgUnit), "F_EXPORT_DATA");

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.getSharing().setOwner(user);
    manager.save(trackedEntityType, false);

    TrackedEntityType inaccessibleTrackedEntityType = createTrackedEntityType('B');
    inaccessibleTrackedEntityType.getSharing().setOwner(admin);
    inaccessibleTrackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(inaccessibleTrackedEntityType, false);

    teA = createTrackedEntity(orgUnit);
    teA.setTrackedEntityType(trackedEntityType);
    manager.save(teA, false);

    teB = createTrackedEntity(orgUnit);
    teB.setTrackedEntityType(trackedEntityType);
    manager.save(teB, false);

    inaccessibleTe = createTrackedEntity(orgUnit);
    inaccessibleTe.setTrackedEntityType(inaccessibleTrackedEntityType);
    manager.save(inaccessibleTe, false);

    Program program = createProgram('A', new HashSet<>(), orgUnit);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.getSharing().setOwner(user);
    manager.save(program, false);
    ProgramStage programStage = createProgramStage('A', program);
    manager.save(programStage, false);
    ProgramStage inaccessibleProgramStage = createProgramStage('B', program);
    inaccessibleProgramStage.getSharing().setOwner(admin);
    inaccessibleProgramStage.setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(inaccessibleProgramStage, false);
    program.setProgramStages(Set.of(programStage, inaccessibleProgramStage));
    manager.save(program, false);

    enrollmentA =
        enrollmentService.enrollTrackedEntity(teA, program, new Date(), new Date(), orgUnit);
    eventA = new Event();
    eventA.setEnrollment(enrollmentA);
    eventA.setProgramStage(programStage);
    eventA.setOrganisationUnit(orgUnit);
    manager.save(eventA, false);

    Enrollment enrollmentB =
        enrollmentService.enrollTrackedEntity(teB, program, new Date(), new Date(), orgUnit);
    inaccessiblePsi = new Event();
    inaccessiblePsi.setEnrollment(enrollmentB);
    inaccessiblePsi.setProgramStage(inaccessibleProgramStage);
    inaccessiblePsi.setOrganisationUnit(orgUnit);
    manager.save(inaccessiblePsi, false);

    teToTeType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToTeType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToTeType.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToTeType.getToConstraint().setTrackedEntityType(trackedEntityType);
    teToTeType.getSharing().setOwner(user);
    manager.save(teToTeType, false);

    teToInaccessibleTeType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToInaccessibleTeType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToInaccessibleTeType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToInaccessibleTeType.getToConstraint().setTrackedEntityType(inaccessibleTrackedEntityType);
    teToInaccessibleTeType.getSharing().setOwner(user);
    manager.save(teToInaccessibleTeType, false);

    teToEnType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToEnType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToEnType.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    teToEnType.getToConstraint().setProgram(program);
    teToEnType.getSharing().setOwner(user);
    manager.save(teToEnType, false);

    teToInaccessibleEnType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToInaccessibleEnType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToInaccessibleEnType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    teToInaccessibleEnType.getToConstraint().setProgram(program);
    teToInaccessibleEnType.getSharing().setOwner(admin);
    teToInaccessibleEnType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(teToInaccessibleEnType, false);

    teToEvType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    teToEvType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    teToEvType.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    teToEvType.getToConstraint().setProgramStage(programStage);
    teToEvType.getSharing().setOwner(user);
    manager.save(teToEvType, false);

    eventToEventType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    eventToEventType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    eventToEventType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    eventToEventType.getToConstraint().setProgramStage(programStage);
    eventToEventType.getSharing().setOwner(user);
    manager.save(eventToEventType, false);

    injectSecurityContextUser(user);
  }

  @Test
  void shouldNotReturnRelationshipByTrackedEntityIfUserHasNoAccessToTrackedEntityType()
      throws ForbiddenException, NotFoundException {
    Relationship accessible = relationship(teA, teB);
    relationship(teA, inaccessibleTe, teToInaccessibleTeType);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder().type(TRACKED_ENTITY).identifier(teA.getUid()).build();

    List<Relationship> relationships = relationshipService.getRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()),
        relationships.stream().map(Relationship::getUid).collect(Collectors.toList()));
  }

  @Test
  void shouldNotReturnRelationshipByEnrollmentIfUserHasNoAccessToRelationshipType()
      throws ForbiddenException, NotFoundException {
    Relationship accessible = relationship(teA, enrollmentA);
    relationship(teB, enrollmentA, teToInaccessibleEnType);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder()
            .type(ENROLLMENT)
            .identifier(enrollmentA.getUid())
            .build();

    List<Relationship> relationships = relationshipService.getRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()),
        relationships.stream().map(Relationship::getUid).collect(Collectors.toList()));
  }

  @Test
  void shouldNotReturnRelationshipByEventIfUserHasNoAccessToProgramStage()
      throws ForbiddenException, NotFoundException {
    Relationship accessible = relationship(teA, eventA);
    relationship(eventA, inaccessiblePsi);

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder().type(EVENT).identifier(eventA.getUid()).build();

    List<Relationship> relationships = relationshipService.getRelationships(operationParams);

    assertContainsOnly(
        List.of(accessible.getUid()),
        relationships.stream().map(Relationship::getUid).collect(Collectors.toList()));
  }

  @Test
  void shouldOrderRelationshipsByUpdatedAtClientInDescOrder()
      throws ForbiddenException, NotFoundException {
    Relationship relationshipA = relationship(teA, teB, DateTime.now().toDate());
    Relationship relationshipB = relationship(teA, eventA, DateTime.now().minusDays(1).toDate());

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder()
            .type(TRACKED_ENTITY)
            .identifier(teA.getUid())
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();
    List<String> relationshipIds =
        relationshipService.getRelationships(operationParams).stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toList());

    assertEquals(List.of(relationshipA.getUid(), relationshipB.getUid()), relationshipIds);
  }

  @Test
  void shouldOrderRelationshipsByUpdatedAtClientInAscOrder()
      throws ForbiddenException, NotFoundException {
    Relationship relationshipA = relationship(teA, teB, DateTime.now().toDate());
    Relationship relationshipB = relationship(teA, eventA, DateTime.now().minusDays(1).toDate());

    RelationshipOperationParams operationParams =
        RelationshipOperationParams.builder()
            .type(TRACKED_ENTITY)
            .identifier(teA.getUid())
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();
    List<String> relationshipIds =
        relationshipService.getRelationships(operationParams).stream()
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toList());

    assertEquals(List.of(relationshipB.getUid(), relationshipA.getUid()), relationshipIds);
  }

  private Relationship relationship(TrackedEntity from, TrackedEntity to) {
    return relationship(from, to, teToTeType, new Date());
  }

  private Relationship relationship(TrackedEntity from, TrackedEntity to, Date createdAtClient) {
    return relationship(from, to, teToTeType, createdAtClient);
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

  private Relationship relationship(TrackedEntity from, Event to, Date createdAtClient) {
    return relationship(from, to, teToEvType, createdAtClient);
  }

  private Relationship relationship(TrackedEntity from, Event to) {
    return relationship(from, to, teToEvType, new Date());
  }

  private Relationship relationship(
      TrackedEntity from, Event to, RelationshipType type, Date createdAtClient) {
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

  private void relationship(Event from, Event to) {
    relationship(from, to, eventToEventType);
  }

  private void relationship(Event from, Event to, RelationshipType type) {
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

  private RelationshipItem item(Event from) {
    RelationshipItem relationshipItem = new RelationshipItem();
    relationshipItem.setEvent(from);
    return relationshipItem;
  }
}
