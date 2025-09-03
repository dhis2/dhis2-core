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
package org.hisp.dhis.maintenance;

import static org.hisp.dhis.changelog.ChangeLogType.UPDATE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.audit.AuditQuery;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditService;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.test.utils.RelationshipUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Enrico Colasante
 */
class MaintenanceServiceTest extends PostgresIntegrationTestBase {
  @Autowired private EnrollmentService enrollmentService;

  @Autowired private EventService eventService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private ProgramMessageService programMessageService;

  @Autowired private EventChangeLogService eventChangeLogService;

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private RelationshipTypeService relationshipTypeService;

  @Autowired private MaintenanceService maintenanceService;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private AuditService auditService;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private Date enrollmentDate;

  private CategoryOptionCombo coA;

  private Program program;

  private OrganisationUnit organisationUnit;

  private Enrollment enrollment;

  private Event event;

  private TrackedEntity trackedEntity;

  private TrackedEntity trackedEntityB;

  private TrackedEntity trackedEntityWithAssociations;

  private RelationshipType relationshipType;

  @BeforeEach
  void setUp() {
    coA = categoryService.getDefaultCategoryOptionCombo();
    organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    program = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(program);
    ProgramStage stageA = createProgramStage('A', program);
    stageA.setSortOrder(1);
    programStageService.saveProgramStage(stageA);
    ProgramStage stageB = createProgramStage('B', program);
    stageB.setSortOrder(2);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    program.setProgramStages(programStages);
    program.setTrackedEntityType(trackedEntityType);
    programService.updateProgram(program);
    trackedEntity = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(trackedEntity);
    trackedEntityB = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(trackedEntityB);
    trackedEntityWithAssociations = createTrackedEntity('T', organisationUnit, trackedEntityType);
    DateTime testDate1 = DateTime.now();
    testDate1.withTimeAtStartOfDay();
    testDate1 = testDate1.minusDays(70);
    Date occurredDate = testDate1.toDate();
    DateTime testDate2 = DateTime.now();
    testDate2.withTimeAtStartOfDay();
    enrollmentDate = testDate2.toDate();
    enrollment = new Enrollment(enrollmentDate, occurredDate, trackedEntity, program);
    enrollment.setUid(CodeGenerator.generateUid());
    enrollment.setOrganisationUnit(organisationUnit);
    Enrollment enrollmentWithTeAssociation =
        new Enrollment(enrollmentDate, occurredDate, trackedEntityWithAssociations, program);
    enrollmentWithTeAssociation.setUid(UID.generate().getValue());
    enrollmentWithTeAssociation.setOrganisationUnit(organisationUnit);
    manager.save(trackedEntityWithAssociations);
    manager.save(enrollmentWithTeAssociation);
    manager.save(enrollment);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntity, program, organisationUnit);
    event = new Event(enrollment, stageA);
    event.setUid(UID.generate().getValue());
    event.setOrganisationUnit(organisationUnit);
    event.setEnrollment(enrollment);
    event.setOccurredDate(new Date());
    event.setAttributeOptionCombo(coA);
    Event eventWithTeAssociation = new Event(enrollmentWithTeAssociation, stageA);
    eventWithTeAssociation.setUid(UID.generate().getValue());
    eventWithTeAssociation.setOrganisationUnit(organisationUnit);
    eventWithTeAssociation.setEnrollment(enrollmentWithTeAssociation);
    eventWithTeAssociation.setOccurredDate(new Date());
    eventWithTeAssociation.setAttributeOptionCombo(coA);
    manager.save(eventWithTeAssociation);
    relationshipType = createPersonToPersonRelationshipType('A', program, trackedEntityType, false);
    relationshipTypeService.addRelationshipType(relationshipType);
    User superUser =
        createAndAddUser(
            true,
            "username",
            Set.of(organisationUnit),
            Set.of(organisationUnit),
            Authorities.ALL.toString());
    injectSecurityContextUser(superUser);
  }

  @Test
  void testDeleteSoftDeletedTrackedEntityLinkedToARelationshipItem() {
    RelationshipType rType = createRelationshipType('A');
    rType.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    rType.getFromConstraint().setProgram(program);
    rType.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    rType.getFromConstraint().setTrackedEntityType(trackedEntity.getTrackedEntityType());
    relationshipTypeService.addRelationshipType(rType);
    Relationship r = new Relationship();
    RelationshipItem rItem1 = new RelationshipItem();
    rItem1.setEnrollment(enrollment);
    RelationshipItem rItem2 = new RelationshipItem();
    rItem2.setTrackedEntity(trackedEntity);
    r.setFrom(rItem1);
    r.setTo(rItem2);
    r.setRelationshipType(rType);
    r.setKey(RelationshipUtils.generateRelationshipKey(r));
    r.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(r));
    manager.save(r);
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(trackedEntity)).isPresent());
    assertTrue(relationshipService.findRelationship(UID.of(r)).isPresent());
    manager.delete(trackedEntity);
    assertFalse(trackedEntityService.findTrackedEntity(UID.of(trackedEntity)).isPresent());
    manager.delete(r);
    manager.delete(enrollment);
    assertFalse(relationshipService.findRelationship(UID.of(r)).isPresent());
    assertTrue(trackedEntityExistsIncludingDeleted(trackedEntity.getUid()));
    assertTrue(relationshipExistsIncludingDeleted(r.getUid()));

    maintenanceService.deleteSoftDeletedEnrollments();
    maintenanceService.deleteSoftDeletedTrackedEntities();

    assertFalse(trackedEntityExistsIncludingDeleted(trackedEntity.getUid()));
    assertFalse(relationshipExistsIncludingDeleted(r.getUid()));
  }

  @Test
  void testDeleteSoftDeletedEnrollmentWithAProgramMessage()
      throws ForbiddenException, BadRequestException {
    ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
    programMessageRecipients.setEmailAddresses(Sets.newHashSet("testemail"));
    programMessageRecipients.setPhoneNumbers(Sets.newHashSet("testphone"));
    programMessageRecipients.setOrganisationUnit(organisationUnit);
    programMessageRecipients.setTrackedEntity(trackedEntity);
    ProgramMessage message =
        ProgramMessage.builder()
            .subject("subject")
            .text("text")
            .recipients(programMessageRecipients)
            .deliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL))
            .enrollment(enrollment)
            .build();
    manager.save(enrollment);
    programMessageService.saveProgramMessage(message);
    assertTrue(enrollmentService.findEnrollment(UID.of(enrollment)).isPresent());

    manager.delete(enrollment);
    assertFalse(enrollmentService.findEnrollment(UID.of(enrollment)).isPresent());
    assertTrue(enrollmentExistsIncludingDeleted(enrollment));

    maintenanceService.deleteSoftDeletedEnrollments();

    assertFalse(enrollmentExistsIncludingDeleted(enrollment));
  }

  @Test
  void testDeleteSoftDeletedEventsWithAProgramMessage() {
    ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
    programMessageRecipients.setEmailAddresses(Sets.newHashSet("testemail"));
    programMessageRecipients.setPhoneNumbers(Sets.newHashSet("testphone"));
    programMessageRecipients.setOrganisationUnit(organisationUnit);
    ProgramMessage message =
        ProgramMessage.builder()
            .subject("subject")
            .text("text")
            .recipients(programMessageRecipients)
            .deliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL))
            .event(event)
            .build();
    manager.save(event);
    UID idA = UID.of(event);
    programMessageService.saveProgramMessage(message);
    assertTrue(eventService.findEvent(idA).isPresent());
    manager.delete(event);
    assertFalse(eventService.findEvent(idA).isPresent());
    assertTrue(eventExistsIncludingDeleted(event.getUid()));

    maintenanceService.deleteSoftDeletedEvents();

    assertFalse(eventExistsIncludingDeleted(event.getUid()));
  }

  @Test
  void testDeleteSoftDeletedTrackedEntityAProgramMessage() {
    ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
    programMessageRecipients.setEmailAddresses(Sets.newHashSet("testemail"));
    programMessageRecipients.setPhoneNumbers(Sets.newHashSet("testphone"));
    programMessageRecipients.setOrganisationUnit(organisationUnit);
    programMessageRecipients.setTrackedEntity(trackedEntityB);
    ProgramMessage message =
        ProgramMessage.builder()
            .subject("subject")
            .text("text")
            .recipients(programMessageRecipients)
            .deliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL))
            .build();
    manager.save(trackedEntityB);
    programMessageService.saveProgramMessage(message);
    assertTrue(trackedEntityService.findTrackedEntity(UID.of(trackedEntityB)).isPresent());
    manager.delete(trackedEntityB);
    assertFalse(trackedEntityService.findTrackedEntity(UID.of(trackedEntityB)).isPresent());
    assertTrue(trackedEntityExistsIncludingDeleted(trackedEntityB.getUid()));

    maintenanceService.deleteSoftDeletedTrackedEntities();

    assertFalse(trackedEntityExistsIncludingDeleted(trackedEntityB.getUid()));
  }

  @Test
  void testDeleteSoftDeletedEnrollmentLinkedToAnEventDataValueChangeLog()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = createDataElement('A');
    dataElementService.addDataElement(dataElement);
    Event eventA = new Event(enrollment, program.getProgramStageByStage(1));
    eventA.setScheduledDate(enrollmentDate);
    eventA.setUid("UID-A");
    eventA.setAttributeOptionCombo(coA);
    eventA.setOrganisationUnit(organisationUnit);
    manager.save(eventA);
    eventChangeLogService.addEventChangeLog(
        eventA, dataElement, "", "value", UPDATE, getCurrentUsername());
    manager.save(enrollment);
    assertTrue(enrollmentService.findEnrollment(UID.of(enrollment)).isPresent());
    manager.delete(enrollment);
    assertFalse(enrollmentService.findEnrollment(UID.of(enrollment)).isPresent());
    assertTrue(enrollmentExistsIncludingDeleted(enrollment));

    maintenanceService.deleteSoftDeletedEnrollments();

    assertFalse(enrollmentExistsIncludingDeleted(enrollment));
  }

  @Test
  void testDeleteSoftDeletedEventLinkedToARelationshipItem() {
    RelationshipType rType = createRelationshipType('A');
    rType.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    rType.getFromConstraint().setProgram(program);
    rType.getFromConstraint().setProgramStage(program.getProgramStageByStage(1));
    rType.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    rType.getFromConstraint().setTrackedEntityType(trackedEntity.getTrackedEntityType());
    relationshipTypeService.addRelationshipType(rType);
    Event eventA = new Event(enrollment, program.getProgramStageByStage(1));
    eventA.setScheduledDate(enrollmentDate);
    eventA.setUid(UID.generate().getValue());
    eventA.setAttributeOptionCombo(coA);
    eventA.setOrganisationUnit(organisationUnit);
    manager.save(eventA);
    UID idA = UID.of(eventA);
    Relationship r = new Relationship();
    RelationshipItem rItem1 = new RelationshipItem();
    rItem1.setEvent(eventA);
    RelationshipItem rItem2 = new RelationshipItem();
    rItem2.setTrackedEntity(trackedEntity);
    r.setFrom(rItem1);
    r.setTo(rItem2);
    r.setRelationshipType(rType);
    r.setKey(RelationshipUtils.generateRelationshipKey(r));
    r.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(r));
    manager.save(r);
    assertTrue(eventService.findEvent(idA).isPresent());
    assertTrue(relationshipService.findRelationship(UID.of(r)).isPresent());
    manager.delete(eventA);
    assertFalse(eventService.findEvent(idA).isPresent());
    manager.delete(r);
    assertFalse(relationshipService.findRelationship(UID.of(r)).isPresent());
    assertTrue(eventExistsIncludingDeleted(eventA.getUid()));
    assertTrue(relationshipExistsIncludingDeleted(r.getUid()));

    maintenanceService.deleteSoftDeletedEvents();

    assertFalse(eventExistsIncludingDeleted(eventA.getUid()));
    assertFalse(relationshipExistsIncludingDeleted(r.getUid()));
  }

  @Test
  void testDeleteSoftDeletedEnrollmentLinkedToARelationshipItem()
      throws ForbiddenException, BadRequestException {
    RelationshipType rType = createRelationshipType('A');
    rType.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    rType.getFromConstraint().setProgram(program);
    rType.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    rType.getFromConstraint().setTrackedEntityType(trackedEntity.getTrackedEntityType());
    relationshipTypeService.addRelationshipType(rType);
    Relationship r = new Relationship();
    RelationshipItem rItem1 = new RelationshipItem();
    rItem1.setEnrollment(enrollment);
    RelationshipItem rItem2 = new RelationshipItem();
    rItem2.setTrackedEntity(trackedEntity);
    r.setFrom(rItem1);
    r.setTo(rItem2);
    r.setRelationshipType(rType);
    r.setKey(RelationshipUtils.generateRelationshipKey(r));
    r.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(r));
    manager.save(r);
    assertTrue(enrollmentService.findEnrollment(UID.of(enrollment)).isPresent());
    assertTrue(relationshipService.findRelationship(UID.of(r)).isPresent());
    manager.delete(enrollment);
    assertFalse(enrollmentService.findEnrollment(UID.of(enrollment)).isPresent());
    manager.delete(r);
    assertFalse(relationshipService.findRelationship(UID.of(r)).isPresent());
    assertTrue(enrollmentExistsIncludingDeleted(enrollment));
    assertTrue(relationshipExistsIncludingDeleted(r.getUid()));

    maintenanceService.deleteSoftDeletedEnrollments();

    assertFalse(enrollmentExistsIncludingDeleted(enrollment));
    assertFalse(relationshipExistsIncludingDeleted(r.getUid()));
  }

  @Test
  @Disabled("until we can inject dhis.conf property overrides")
  void testAuditEntryForDeletionOfSoftDeletedTrackedEntity() {
    manager.delete(trackedEntityWithAssociations);
    assertFalse(
        trackedEntityService.findTrackedEntity(UID.of(trackedEntityWithAssociations)).isPresent());
    assertTrue(trackedEntityExistsIncludingDeleted(trackedEntityWithAssociations.getUid()));
    maintenanceService.deleteSoftDeletedTrackedEntities();
    List<Audit> audits =
        auditService.getAudits(
            AuditQuery.builder()
                .auditType(Sets.newHashSet(AuditType.DELETE))
                .auditScope(Sets.newHashSet(AuditScope.TRACKER))
                .build());
    assertFalse(audits.isEmpty());
    assertEquals(
        1,
        audits.stream()
            .filter(a -> a.getKlass().equals("org.hisp.dhis.program.Enrollment"))
            .count());
    assertEquals(
        1, audits.stream().filter(a -> a.getKlass().equals("org.hisp.dhis.program.Event")).count());
    assertEquals(
        1,
        audits.stream()
            .filter(a -> a.getKlass().equals("org.hisp.dhis.trackedentity.TrackedEntity"))
            .count());
    audits.forEach(a -> assertEquals(AuditType.DELETE, a.getAuditType()));
  }

  @Test
  void testDeleteSoftDeletedRelationship() {
    Relationship relationship =
        createTeToTeRelationship(trackedEntity, trackedEntityB, relationshipType);
    manager.save(relationship);
    assertTrue(relationshipService.findRelationship(UID.of(relationship)).isPresent());

    manager.delete(relationship);
    assertFalse(relationshipService.findRelationship(UID.of(relationship)).isPresent());
    assertTrue(relationshipExistsIncludingDeleted(relationship.getUid()));

    maintenanceService.deleteSoftDeletedRelationships();
    assertFalse(relationshipExistsIncludingDeleted(relationship.getUid()));
  }

  private boolean trackedEntityExistsIncludingDeleted(String uid) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "select exists(select 1 from trackedentity where uid=?)", Boolean.class, uid));
  }

  private boolean eventExistsIncludingDeleted(String uid) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "select exists(select 1 from event where uid=?)", Boolean.class, uid));
  }

  private boolean relationshipExistsIncludingDeleted(String uid) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "select exists(select 1 from relationship where uid=?)", Boolean.class, uid));
  }

  private boolean enrollmentExistsIncludingDeleted(Enrollment enrollment)
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .enrollments(enrollment)
            .orgUnitMode(ALL)
            .includeDeleted(true)
            .build();

    return !enrollmentService.findEnrollments(params).isEmpty();
  }
}
