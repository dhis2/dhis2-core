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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityServiceTest extends PostgresIntegrationTestBase {
  private static final String TE_A_UID = uidWithPrefix('A');
  private static final String TE_B_UID = uidWithPrefix('B');
  private static final String TE_C_UID = uidWithPrefix('C');
  private static final String TE_D_UID = uidWithPrefix('D');
  private static final String ENROLLMENT_A_UID = UID.of(CodeGenerator.generateUid()).getValue();
  private static final String EVENT_A_UID = UID.of(CodeGenerator.generateUid()).getValue();

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  private Event event;

  private Enrollment enrollment;

  private Program program;

  private TrackedEntity trackedEntityA1;

  private TrackedEntity trackedEntityB1;

  private TrackedEntity trackedEntityC1;

  private TrackedEntity trackedEntityD1;

  private OrganisationUnit organisationUnit;

  private TrackedEntityType trackedEntityType;

  private TrackedEntityAttribute trackedEntityAttribute;

  @BeforeEach
  void setUp() {
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntityType.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    manager.update(trackedEntityType);

    TrackedEntityAttribute attrD = createTrackedEntityAttribute('D');
    TrackedEntityAttribute attrE = createTrackedEntityAttribute('E');
    TrackedEntityAttribute filtF = createTrackedEntityAttribute('F');
    TrackedEntityAttribute filtG = createTrackedEntityAttribute('G');
    trackedEntityAttribute = createTrackedEntityAttribute('H');

    trackedEntityAttributeService.addTrackedEntityAttribute(attrD);
    trackedEntityAttributeService.addTrackedEntityAttribute(attrE);
    trackedEntityAttributeService.addTrackedEntityAttribute(filtF);
    trackedEntityAttributeService.addTrackedEntityAttribute(filtG);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    attributeService.addTrackedEntityAttribute(createTrackedEntityAttribute('A'));
    trackedEntityA1 = createTrackedEntity(organisationUnit);
    trackedEntityB1 = createTrackedEntity(organisationUnit);
    trackedEntityC1 = createTrackedEntity(organisationUnit);
    trackedEntityD1 = createTrackedEntity(organisationUnit);
    trackedEntityA1.setUid(TE_A_UID);
    trackedEntityB1.setUid(TE_B_UID);
    trackedEntityC1.setUid(TE_C_UID);
    trackedEntityD1.setUid(TE_D_UID);
    trackedEntityA1.setTrackedEntityType(trackedEntityType);
    program = createProgram('A', new HashSet<>(), organisationUnit);
    program.setTrackedEntityType(trackedEntityType);
    programService.addProgram(program);
    program.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    manager.update(program);
    ProgramStage stageA = createProgramStage('A', program);
    stageA.setSortOrder(1);
    programStageService.saveProgramStage(stageA);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    program.setProgramStages(programStages);
    programService.updateProgram(program);
    DateTime enrollmentDate = DateTime.now();
    enrollmentDate.withTimeAtStartOfDay();
    enrollmentDate = enrollmentDate.minusDays(70);
    DateTime incidentDate = DateTime.now();
    incidentDate.withTimeAtStartOfDay();
    enrollment =
        new Enrollment(enrollmentDate.toDate(), incidentDate.toDate(), trackedEntityA1, program);
    enrollment.setUid(ENROLLMENT_A_UID);
    enrollment.setOrganisationUnit(organisationUnit);
    event = createEvent(stageA, enrollment, organisationUnit);
    event.setUid(EVENT_A_UID);

    attributeService.addTrackedEntityAttribute(attrD);
    attributeService.addTrackedEntityAttribute(attrE);
    attributeService.addTrackedEntityAttribute(filtF);
    attributeService.addTrackedEntityAttribute(filtG);

    User user = createUserWithAuth("testUser");
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnit));
    userService.addUser(user);
    injectSecurityContextUser(user);
  }

  @Test
  void testDeleteTrackedEntityAndLinkedEnrollmentsAndEvents() throws NotFoundException {
    manager.save(trackedEntityA1);
    manager.save(enrollment);
    manager.save(event);
    long eventIdA = event.getId();
    enrollment.setEvents(Set.of(event));
    trackedEntityA1.setEnrollments(Set.of(enrollment));
    manager.update(enrollment);
    manager.update(trackedEntityA1);
    TrackedEntity trackedEntityA = manager.get(TrackedEntity.class, trackedEntityA1.getUid());
    Enrollment psA = manager.get(Enrollment.class, enrollment.getUid());
    Event eventA = manager.get(Event.class, eventIdA);
    assertNotNull(trackedEntityA);
    assertNotNull(psA);
    assertNotNull(eventA);
    trackerObjectDeletionService.deleteTrackedEntities(List.of(trackedEntityA.getUid()));
    assertNull(manager.get(TrackedEntity.class, trackedEntityA.getUid()));
    assertNull(manager.get(Enrollment.class, enrollment.getUid()));
    assertNull(manager.get(Event.class, eventIdA));
  }

  @Test
  void shouldOrderEntitiesByCreatedInAscOrder() {
    injectAdminIntoSecurityContext();

    trackedEntityA1.setCreated(DateTime.now().plusDays(1).toDate());
    trackedEntityB1.setCreated(DateTime.now().toDate());
    trackedEntityC1.setCreated(DateTime.now().minusDays(1).toDate());
    trackedEntityD1.setCreated(DateTime.now().plusDays(2).toDate());
    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("createdAt", SortDirection.ASC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityC1.getId(),
            trackedEntityB1.getId(),
            trackedEntityA1.getId(),
            trackedEntityD1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByCreatedInDescOrder() {
    injectAdminIntoSecurityContext();

    trackedEntityA1.setCreated(DateTime.now().plusDays(1).toDate());
    trackedEntityB1.setCreated(DateTime.now().toDate());
    trackedEntityC1.setCreated(DateTime.now().minusDays(1).toDate());
    trackedEntityD1.setCreated(DateTime.now().plusDays(2).toDate());
    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("createdAt", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityD1.getId(),
            trackedEntityA1.getId(),
            trackedEntityB1.getId(),
            trackedEntityC1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByCreatedAtInAscOrder() {
    injectAdminIntoSecurityContext();

    trackedEntityA1.setCreated(DateTime.now().plusDays(1).toDate());
    trackedEntityB1.setCreated(DateTime.now().toDate());
    trackedEntityC1.setCreated(DateTime.now().minusDays(1).toDate());
    trackedEntityD1.setCreated(DateTime.now().plusDays(2).toDate());
    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("createdAt", SortDirection.ASC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityC1.getId(),
            trackedEntityB1.getId(),
            trackedEntityA1.getId(),
            trackedEntityD1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByCreatedAtInDescOrder() {
    injectAdminIntoSecurityContext();

    DateTime now = DateTime.now();

    trackedEntityD1.setCreated(now.plusDays(2).toDate());
    trackedEntityA1.setCreated(now.plusDays(1).toDate());
    trackedEntityB1.setCreated(now.toDate());
    trackedEntityC1.setCreated(now.minusDays(1).toDate());

    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("createdAt", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityD1.getId(),
            trackedEntityA1.getId(),
            trackedEntityB1.getId(),
            trackedEntityC1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByUpdatedAtInAscOrder() throws InterruptedException {
    injectAdminIntoSecurityContext();

    addEntityInstances();
    // lastupdated is automatically set by the store; update entities in a certain order and
    //   expect
    // that to be returned
    manager.update(trackedEntityD1);
    Thread.sleep(1000);
    manager.update(trackedEntityB1);
    Thread.sleep(1000);
    manager.update(trackedEntityC1);
    Thread.sleep(1000);
    manager.update(trackedEntityA1);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("updatedAt", SortDirection.ASC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityD1.getId(),
            trackedEntityB1.getId(),
            trackedEntityC1.getId(),
            trackedEntityA1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByUpdatedAtInDescOrder() throws InterruptedException {
    injectAdminIntoSecurityContext();

    addEntityInstances();
    // lastupdated is automatically set by the store; update entities in a certain order and
    //   expect
    // that to be returned
    manager.update(trackedEntityD1);
    Thread.sleep(1000);
    manager.update(trackedEntityB1);
    Thread.sleep(1000);
    manager.update(trackedEntityC1);
    Thread.sleep(1000);
    manager.update(trackedEntityA1);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("updatedAt", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityA1.getId(),
            trackedEntityC1.getId(),
            trackedEntityB1.getId(),
            trackedEntityD1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByTrackedEntityUidInDescOrder() {
    injectAdminIntoSecurityContext();

    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("trackedEntity", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityD1.getId(),
            trackedEntityC1.getId(),
            trackedEntityB1.getId(),
            trackedEntityA1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByUpdatedAtClientInDescOrder() {
    injectAdminIntoSecurityContext();

    trackedEntityA1.setLastUpdatedAtClient(DateTime.now().plusDays(1).toDate());
    trackedEntityB1.setLastUpdatedAtClient(DateTime.now().toDate());
    trackedEntityC1.setLastUpdatedAtClient(DateTime.now().minusDays(1).toDate());
    trackedEntityD1.setLastUpdatedAtClient(DateTime.now().plusDays(2).toDate());
    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("updatedAtClient", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityD1.getId(),
            trackedEntityA1.getId(),
            trackedEntityB1.getId(),
            trackedEntityC1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByEnrolledAtDateInDescOrder() {
    injectAdminIntoSecurityContext();

    addEntityInstances();
    manager.save(enrollment);
    addEnrollment(trackedEntityB1, DateTime.now().plusDays(2).toDate(), 'B');
    addEnrollment(trackedEntityC1, DateTime.now().minusDays(2).toDate(), 'C');
    addEnrollment(trackedEntityD1, DateTime.now().plusDays(1).toDate(), 'D');

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam("enrolledAt", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityB1.getId(),
            trackedEntityD1.getId(),
            trackedEntityC1.getId(),
            trackedEntityA1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldSortEntitiesAndKeepOrderOfParamsWhenMultipleStaticFieldsSupplied() {
    injectAdminIntoSecurityContext();
    trackedEntityA1.setInactive(true);
    trackedEntityB1.setInactive(true);
    trackedEntityC1.setInactive(false);
    trackedEntityD1.setInactive(false);
    addEntityInstances();

    manager.save(enrollment);
    addEnrollment(trackedEntityB1, DateTime.now().plusDays(2).toDate(), 'B');
    addEnrollment(trackedEntityC1, DateTime.now().minusDays(2).toDate(), 'C');
    addEnrollment(trackedEntityD1, DateTime.now().plusDays(1).toDate(), 'D');

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(
        List.of(
            new OrderParam("inactive", SortDirection.DESC),
            new OrderParam("enrolledAt", SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityB1.getId(),
            trackedEntityA1.getId(),
            trackedEntityD1.getId(),
            trackedEntityC1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderEntitiesByDefaultUsingTrackedEntityIdInAscOrderWhenNoOrderParamProvided() {
    injectAdminIntoSecurityContext();
    addEntityInstances();

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setOrgUnits(Set.of(organisationUnit));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityA1.getId(),
            trackedEntityB1.getId(),
            trackedEntityC1.getId(),
            trackedEntityD1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldOrderByNonStaticFieldWhenNonStaticFieldProvided() {
    injectAdminIntoSecurityContext();
    trackedEntityAttribute.setDisplayInListNoProgram(true);
    attributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    setUpEntityAndAttributeValue(trackedEntityA1, "3-Attribute Value A1");
    setUpEntityAndAttributeValue(trackedEntityB1, "1-Attribute Value B1");
    setUpEntityAndAttributeValue(trackedEntityC1, "4-Attribute Value C1");
    setUpEntityAndAttributeValue(trackedEntityD1, "2-Attribute Value D1");

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam(trackedEntityAttribute.getUid(), SortDirection.ASC)));
    params.setAttributes(List.of(new QueryItem(trackedEntityAttribute)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityB1.getId(),
            trackedEntityD1.getId(),
            trackedEntityA1.getId(),
            trackedEntityC1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldSortEntitiesAndKeepOrderOfParamsWhenStaticAndNonStaticFieldsSupplied() {
    injectAdminIntoSecurityContext();
    trackedEntityAttribute.setDisplayInListNoProgram(true);
    attributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    trackedEntityA1.setInactive(true);
    trackedEntityB1.setInactive(false);
    trackedEntityC1.setInactive(true);
    trackedEntityD1.setInactive(false);

    setUpEntityAndAttributeValue(trackedEntityA1, "2-Attribute Value");
    setUpEntityAndAttributeValue(trackedEntityB1, "2-Attribute Value");
    setUpEntityAndAttributeValue(trackedEntityC1, "1-Attribute Value");
    setUpEntityAndAttributeValue(trackedEntityD1, "1-Attribute Value");

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(
        List.of(
            new OrderParam(trackedEntityAttribute.getUid(), SortDirection.DESC),
            new OrderParam("inactive", SortDirection.ASC)));
    params.setAttributes(List.of(new QueryItem(trackedEntityAttribute)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityB1.getId(),
            trackedEntityA1.getId(),
            trackedEntityD1.getId(),
            trackedEntityC1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldSortEntitiesByAttributeDescendingWhenAttributeDescendingProvided() {
    injectAdminIntoSecurityContext();

    TrackedEntityAttribute tea = createTrackedEntityAttribute();

    addEntityInstances();

    createTrackedEntityAttribute(trackedEntityA1, tea, "A");
    createTrackedEntityAttribute(trackedEntityB1, tea, "D");
    createTrackedEntityAttribute(trackedEntityC1, tea, "C");
    createTrackedEntityAttribute(trackedEntityD1, tea, "B");

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam(tea.getUid(), SortDirection.DESC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityB1.getId(),
            trackedEntityC1.getId(),
            trackedEntityD1.getId(),
            trackedEntityA1.getId()),
        trackedEntityIdList);
  }

  @Test
  void shouldSortEntitiesByAttributeAscendingWhenAttributeAscendingProvided() {
    injectAdminIntoSecurityContext();

    TrackedEntityAttribute tea = createTrackedEntityAttribute();

    addEntityInstances();

    createTrackedEntityAttribute(trackedEntityA1, tea, "A");
    createTrackedEntityAttribute(trackedEntityB1, tea, "D");
    createTrackedEntityAttribute(trackedEntityC1, tea, "C");
    createTrackedEntityAttribute(trackedEntityD1, tea, "B");

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();

    params.setOrgUnits(Set.of(organisationUnit));
    params.setOrders(List.of(new OrderParam(tea.getUid(), SortDirection.ASC)));

    List<Long> trackedEntityIdList = trackedEntityService.getTrackedEntityIds(params, true, true);

    assertEquals(
        List.of(
            trackedEntityA1.getId(),
            trackedEntityD1.getId(),
            trackedEntityC1.getId(),
            trackedEntityB1.getId()),
        trackedEntityIdList);
  }

  private void addEnrollment(TrackedEntity trackedEntity, Date enrollmentDate, char programStage) {
    ProgramStage stage = createProgramStage(programStage, program);
    stage.setSortOrder(1);
    programStageService.saveProgramStage(stage);

    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stage);
    program.setProgramStages(programStages);
    programService.updateProgram(program);

    enrollment = new Enrollment(enrollmentDate, DateTime.now().toDate(), trackedEntity, program);
    enrollment.setUid(uidWithPrefix(programStage));
    enrollment.setOrganisationUnit(organisationUnit);
    event = new Event(enrollment, stage);
    enrollment.setUid(uidWithPrefix(programStage));
    enrollment.setOrganisationUnit(organisationUnit);

    manager.save(enrollment);
  }

  private void addEntityInstances() {
    trackedEntityA1.setTrackedEntityType(trackedEntityType);
    trackedEntityB1.setTrackedEntityType(trackedEntityType);
    trackedEntityC1.setTrackedEntityType(trackedEntityType);
    trackedEntityD1.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);
    manager.save(trackedEntityC1);
    manager.save(trackedEntityD1);
  }

  private void setUpEntityAndAttributeValue(TrackedEntity trackedEntity, String attributeValue) {
    trackedEntity.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntity);

    TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
    trackedEntityAttributeValue.setAttribute(trackedEntityAttribute);
    trackedEntityAttributeValue.setTrackedEntity(trackedEntity);
    trackedEntityAttributeValue.setValue(attributeValue);
    attributeValueService.addTrackedEntityAttributeValue(trackedEntityAttributeValue);
  }

  private TrackedEntityAttribute createTrackedEntityAttribute() {
    TrackedEntityAttribute tea = createTrackedEntityAttribute('X');
    attributeService.addTrackedEntityAttribute(tea);

    return tea;
  }

  private void createTrackedEntityAttribute(
      TrackedEntity trackedEntity, TrackedEntityAttribute attribute, String value) {
    TrackedEntityAttributeValue trackedEntityAttributeValueA1 = new TrackedEntityAttributeValue();

    trackedEntityAttributeValueA1.setAttribute(attribute);
    trackedEntityAttributeValueA1.setTrackedEntity(trackedEntity);
    trackedEntityAttributeValueA1.setValue(value);

    attributeValueService.addTrackedEntityAttributeValue(trackedEntityAttributeValueA1);
  }

  private static String uidWithPrefix(char prefix) {
    String value = prefix + CodeGenerator.generateUid().substring(0, 10);
    return UID.of(value).getValue();
  }
}
