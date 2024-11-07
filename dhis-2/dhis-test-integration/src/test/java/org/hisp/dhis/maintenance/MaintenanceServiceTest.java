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
package org.hisp.dhis.maintenance;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
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
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.test.utils.RelationshipUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.TrackedEntityDataValueChangeLog;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
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
    programService.updateProgram(program);
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntity);
    trackedEntityB = createTrackedEntity(organisationUnit);
    trackedEntityB.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntityB);
    trackedEntityWithAssociations = createTrackedEntity('T', organisationUnit);
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
    enrollmentWithTeAssociation.setUid("UID-B");
    enrollmentWithTeAssociation.setOrganisationUnit(organisationUnit);
    manager.save(trackedEntityWithAssociations);
    manager.save(enrollmentWithTeAssociation);
    manager.save(enrollment);
    event = new Event(enrollment, stageA);
    event.setUid("PSUID-B");
    event.setOrganisationUnit(organisationUnit);
    event.setEnrollment(enrollment);
    event.setOccurredDate(new Date());
    event.setAttributeOptionCombo(coA);
    Event eventWithTeAssociation = new Event(enrollmentWithTeAssociation, stageA);
    eventWithTeAssociation.setUid("PSUID-C");
    eventWithTeAssociation.setOrganisationUnit(organisationUnit);
    eventWithTeAssociation.setEnrollment(enrollmentWithTeAssociation);
    eventWithTeAssociation.setOccurredDate(new Date());
    eventWithTeAssociation.setAttributeOptionCombo(coA);
    manager.save(eventWithTeAssociation);
    relationshipType = createPersonToPersonRelationshipType('A', program, trackedEntityType, false);
    relationshipTypeService.addRelationshipType(relationshipType);
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
    assertNotNull(manager.get(TrackedEntity.class, trackedEntity.getId()));
    assertNotNull(getRelationship(r.getId()));
    manager.delete(trackedEntity);
    assertNull(manager.get(TrackedEntity.class, trackedEntity.getId()));
    manager.delete(r);
    manager.delete(enrollment);
    assertNull(getRelationship(r.getId()));
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
    assertNotNull(manager.get(Enrollment.class, enrollment.getUid()));

    manager.delete(enrollment);
    assertNull(manager.get(Enrollment.class, enrollment.getUid()));
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
    long idA = event.getId();
    programMessageService.saveProgramMessage(message);
    assertNotNull(getEvent(idA));
    manager.delete(event);
    assertNull(getEvent(idA));
    assertTrue(eventExistsIncludingDeleted(event.getUid()));

    maintenanceService.deleteSoftDeletedEvents();

    assertFalse(eventExistsIncludingDeleted(event.getUid()));
  }

  @Test
  void testDeleteSoftDeletedTrackedEntityAProgramMessage()
      throws ForbiddenException, NotFoundException, BadRequestException {
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
    assertNotNull(trackedEntityService.getTrackedEntity(UID.of(trackedEntityB)));
    manager.delete(trackedEntityB);
    assertThrows(
        NotFoundException.class,
        () -> trackedEntityService.getTrackedEntity(UID.of(trackedEntityB)));
    assertTrue(trackedEntityExistsIncludingDeleted(trackedEntityB.getUid()));

    maintenanceService.deleteSoftDeletedTrackedEntities();

    assertFalse(trackedEntityExistsIncludingDeleted(trackedEntityB.getUid()));
  }

  @Test
  void testDeleteSoftDeletedEnrollmentLinkedToATrackedEntityDataValueAudit()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = createDataElement('A');
    dataElementService.addDataElement(dataElement);
    Event eventA = new Event(enrollment, program.getProgramStageByStage(1));
    eventA.setScheduledDate(enrollmentDate);
    eventA.setUid("UID-A");
    eventA.setAttributeOptionCombo(coA);
    manager.save(eventA);
    TrackedEntityDataValueChangeLog trackedEntityDataValueChangeLog =
        new TrackedEntityDataValueChangeLog(
            dataElement, eventA, "value", "modifiedBy", false, ChangeLogType.UPDATE);
    eventChangeLogService.addTrackedEntityDataValueChangeLog(trackedEntityDataValueChangeLog);
    manager.save(enrollment);
    assertNotNull(manager.get(Enrollment.class, enrollment.getUid()));
    manager.delete(enrollment);
    assertNull(manager.get(Enrollment.class, enrollment.getUid()));
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
    manager.save(eventA);
    long idA = eventA.getId();
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
    assertNotNull(getEvent(idA));
    assertNotNull(getRelationship(r.getId()));
    manager.delete(eventA);
    assertNull(getEvent(idA));
    manager.delete(r);
    assertNull(getRelationship(r.getId()));
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
    assertNotNull(manager.get(Enrollment.class, enrollment.getId()));
    assertNotNull(getRelationship(r.getId()));
    manager.delete(enrollment);
    assertNull(manager.get(Enrollment.class, enrollment.getId()));
    manager.delete(r);
    assertNull(getRelationship(r.getId()));
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
    assertNull(manager.get(TrackedEntity.class, trackedEntityWithAssociations.getId()));
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
    assertNotNull(getRelationship(relationship.getUid()));

    manager.delete(relationship);
    assertNull(getRelationship(relationship.getUid()));
    assertTrue(relationshipExistsIncludingDeleted(relationship.getUid()));

    maintenanceService.deleteSoftDeletedRelationships();
    assertFalse(relationshipExistsIncludingDeleted(relationship.getUid()));
  }

  private Event getEvent(long id) {
    return manager.get(Event.class, id);
  }

  private Relationship getRelationship(String uid) {
    return manager.get(Relationship.class, uid);
  }

  private Relationship getRelationship(long id) {
    return manager.get(Relationship.class, id);
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

    return !enrollmentService.getEnrollments(params).isEmpty();
  }
}
