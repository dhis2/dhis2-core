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
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityServiceTest extends IntegrationTestBase {
  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private EventService eventService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private IdentifiableObjectManager manager;

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

  private User superUser;

  @Override
  public void setUpTest() {
    //    super.userService = _userService;

    this.superUser = getAdminUser();

    trackedEntityType = createTrackedEntityType('A');
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
    trackedEntityA1.setUid("UID-A1");
    trackedEntityB1.setUid("UID-B1");
    trackedEntityC1.setUid("UID-C1");
    trackedEntityD1.setUid("UID-D1");
    program = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(program);
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
    enrollment.setUid("UID-A");
    enrollment.setOrganisationUnit(organisationUnit);
    event = new Event(enrollment, stageA);
    enrollment.setUid("UID-PSI-A");
    enrollment.setOrganisationUnit(organisationUnit);

    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    attributeService.addTrackedEntityAttribute(attrD);
    attributeService.addTrackedEntityAttribute(attrE);
    attributeService.addTrackedEntityAttribute(filtF);
    attributeService.addTrackedEntityAttribute(filtG);

    User user = createUserWithAuth("testUser");
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnit));
    injectSecurityContextUser(user);
  }

  @Test
  void testSaveTrackedEntity() {
    long idA = trackedEntityService.addTrackedEntity(trackedEntityA1);
    long idB = trackedEntityService.addTrackedEntity(trackedEntityB1);
    assertNotNull(trackedEntityService.getTrackedEntity(idA));
    assertNotNull(trackedEntityService.getTrackedEntity(idB));
  }

  @Test
  void testDeleteTrackedEntity() {
    long idA = trackedEntityService.addTrackedEntity(trackedEntityA1);
    long idB = trackedEntityService.addTrackedEntity(trackedEntityB1);
    TrackedEntity trackedEntityA = trackedEntityService.getTrackedEntity(idA);
    TrackedEntity trackedEntityB = trackedEntityService.getTrackedEntity(idB);
    assertNotNull(trackedEntityA);
    assertNotNull(trackedEntityB);
    trackedEntityService.deleteTrackedEntity(trackedEntityA1);
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityA.getUid()));
    assertNotNull(trackedEntityService.getTrackedEntity(trackedEntityB.getUid()));
    trackedEntityService.deleteTrackedEntity(trackedEntityB1);
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityA.getUid()));
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityB.getUid()));
  }

  @Test
  void testDeleteTrackedEntityAndLinkedEnrollmentsAndEvents() {
    long idA = trackedEntityService.addTrackedEntity(trackedEntityA1);
    long psIdA = enrollmentService.addEnrollment(enrollment);
    long eventIdA = eventService.addEvent(event);
    enrollment.setEvents(Set.of(event));
    trackedEntityA1.setEnrollments(Set.of(enrollment));
    enrollmentService.updateEnrollment(enrollment);
    trackedEntityService.updateTrackedEntity(trackedEntityA1);
    TrackedEntity trackedEntityA = trackedEntityService.getTrackedEntity(idA);
    Enrollment psA = enrollmentService.getEnrollment(psIdA);
    Event eventA = manager.get(Event.class, eventIdA);
    assertNotNull(trackedEntityA);
    assertNotNull(psA);
    assertNotNull(eventA);
    trackedEntityService.deleteTrackedEntity(trackedEntityA1);
    assertNull(trackedEntityService.getTrackedEntity(trackedEntityA.getUid()));
    assertNull(enrollmentService.getEnrollment(psIdA));
    assertNull(manager.get(Event.class, eventIdA));
  }

  @Test
  void testUpdateTrackedEntity() {
    long idA = trackedEntityService.addTrackedEntity(trackedEntityA1);
    assertNotNull(trackedEntityService.getTrackedEntity(idA));
    trackedEntityA1.setName("B");
    trackedEntityService.updateTrackedEntity(trackedEntityA1);
    assertEquals("B", trackedEntityService.getTrackedEntity(idA).getName());
  }

  @Test
  void testGetTrackedEntityById() {
    long idA = trackedEntityService.addTrackedEntity(trackedEntityA1);
    long idB = trackedEntityService.addTrackedEntity(trackedEntityB1);
    assertEquals(trackedEntityA1, trackedEntityService.getTrackedEntity(idA));
    assertEquals(trackedEntityB1, trackedEntityService.getTrackedEntity(idB));
  }

  @Test
  void testGetTrackedEntityByUid() {
    trackedEntityA1.setUid("A1");
    trackedEntityB1.setUid("B1");
    trackedEntityService.addTrackedEntity(trackedEntityA1);
    trackedEntityService.addTrackedEntity(trackedEntityB1);
    assertEquals(trackedEntityA1, trackedEntityService.getTrackedEntity("A1"));
    assertEquals(trackedEntityB1, trackedEntityService.getTrackedEntity("B1"));
  }

  @Test
  void testStoredByColumnForTrackedEntity() {
    trackedEntityA1.setStoredBy("test");
    trackedEntityService.addTrackedEntity(trackedEntityA1);
    TrackedEntity trackedEntity = trackedEntityService.getTrackedEntity(trackedEntityA1.getUid());
    assertEquals("test", trackedEntity.getStoredBy());
  }

  @Test
  void shouldOrderEntitiesByCreatedInAscOrder() {
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

    addEntityInstances();
    // lastupdated is automatically set by the store; update entities in a certain order and
    //   expect
    // that to be returned
    trackedEntityService.updateTrackedEntity(trackedEntityD1);
    Thread.sleep(1000);
    trackedEntityService.updateTrackedEntity(trackedEntityB1);
    Thread.sleep(1000);
    trackedEntityService.updateTrackedEntity(trackedEntityC1);
    Thread.sleep(1000);
    trackedEntityService.updateTrackedEntity(trackedEntityA1);

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
    injectSecurityContextUser(superUser);

    addEntityInstances();
    // lastupdated is automatically set by the store; update entities in a certain order and
    //   expect
    // that to be returned
    trackedEntityService.updateTrackedEntity(trackedEntityD1);
    Thread.sleep(1000);
    trackedEntityService.updateTrackedEntity(trackedEntityB1);
    Thread.sleep(1000);
    trackedEntityService.updateTrackedEntity(trackedEntityC1);
    Thread.sleep(1000);
    trackedEntityService.updateTrackedEntity(trackedEntityA1);

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
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

    addEntityInstances();
    enrollmentService.addEnrollment(enrollment);
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
    injectSecurityContextUser(superUser);
    trackedEntityA1.setInactive(true);
    trackedEntityB1.setInactive(true);
    trackedEntityC1.setInactive(false);
    trackedEntityD1.setInactive(false);
    addEntityInstances();

    enrollmentService.addEnrollment(enrollment);
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
    injectSecurityContextUser(superUser);
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
    injectSecurityContextUser(superUser);
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
    injectSecurityContextUser(superUser);
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
    injectSecurityContextUser(superUser);

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
    injectSecurityContextUser(superUser);

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
    enrollment.setUid("UID-" + programStage);
    enrollment.setOrganisationUnit(organisationUnit);
    event = new Event(enrollment, stage);
    enrollment.setUid("UID-PSI-" + programStage);
    enrollment.setOrganisationUnit(organisationUnit);

    enrollmentService.addEnrollment(enrollment);
  }

  private void addEntityInstances() {
    trackedEntityA1.setTrackedEntityType(trackedEntityType);
    trackedEntityB1.setTrackedEntityType(trackedEntityType);
    trackedEntityC1.setTrackedEntityType(trackedEntityType);
    trackedEntityD1.setTrackedEntityType(trackedEntityType);
    trackedEntityService.addTrackedEntity(trackedEntityA1);
    trackedEntityService.addTrackedEntity(trackedEntityB1);
    trackedEntityService.addTrackedEntity(trackedEntityC1);
    trackedEntityService.addTrackedEntity(trackedEntityD1);
  }

  private void setUpEntityAndAttributeValue(TrackedEntity trackedEntity, String attributeValue) {
    trackedEntity.setTrackedEntityType(trackedEntityType);
    trackedEntityService.addTrackedEntity(trackedEntity);

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
}
