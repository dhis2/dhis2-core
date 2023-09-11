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
package org.hisp.dhis.dxf2.deprecated.tracker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.HashSet;
import org.hamcrest.CoreMatchers;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class RegistrationMultiEventsServiceTest extends TransactionalIntegrationTest {

  @Autowired private EventService eventService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private UserService _userService;

  private TrackedEntity maleA;

  private TrackedEntity maleB;

  private TrackedEntity femaleA;

  private TrackedEntity femaleB;

  private TrackedEntityInstance trackedEntityInstanceMaleA;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private ProgramStage programStageA;

  private ProgramStage programStageB;

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B');
    manager.save(organisationUnitA);
    manager.save(organisationUnitB);
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    maleA = createTrackedEntity(organisationUnitA);
    maleB = createTrackedEntity(organisationUnitB);
    femaleA = createTrackedEntity(organisationUnitA);
    femaleB = createTrackedEntity(organisationUnitB);
    maleA.setTrackedEntityType(trackedEntityType);
    maleB.setTrackedEntityType(trackedEntityType);
    femaleA.setTrackedEntityType(trackedEntityType);
    femaleB.setTrackedEntityType(trackedEntityType);
    manager.save(maleA);
    manager.save(maleB);
    manager.save(femaleA);
    manager.save(femaleB);
    trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance(maleA);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementA.setValueType(ValueType.INTEGER);
    dataElementB.setValueType(ValueType.INTEGER);
    manager.save(dataElementA);
    manager.save(dataElementB);
    programStageA = createProgramStage('A', 0);
    programStageB = createProgramStage('B', 0);
    programStageB.setRepeatable(true);
    manager.save(programStageA);
    manager.save(programStageB);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    manager.save(programA);
    ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementA);
    programStageDataElement.setProgramStage(programStageA);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageA.getProgramStageDataElements().add(programStageDataElement);
    programStageA.setProgram(programA);
    programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementB);
    programStageDataElement.setProgramStage(programStageB);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageB.getProgramStageDataElements().add(programStageDataElement);
    programStageB.setProgram(programA);
    programStageB.setMinDaysFromStart(2);
    programA.getProgramStages().add(programStageA);
    programA.getProgramStages().add(programStageB);
    manager.update(programStageA);
    manager.update(programStageB);
    manager.update(programA);
    createUserAndInjectSecurityContext(true);
  }

  @Test
  void testSaveWithoutProgramStageShouldFail() {
    Event event =
        createEvent(
            programA.getUid(),
            null,
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    ImportSummary importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.ERROR, importSummary.getStatus());
    assertThat(
        importSummary.getDescription(),
        CoreMatchers.containsString("does not point to a valid programStage"));
  }

  @Test
  void testSaveWithoutEnrollmentShouldFail() {
    Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    ImportSummary importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.ERROR, importSummary.getStatus());
    assertThat(
        importSummary.getDescription(), CoreMatchers.containsString("is not enrolled in program"));
  }

  @Test
  void testSaveRepeatableStageWithoutEventIdShouldCreateNewEvent() {
    Enrollment enrollment =
        createEnrollment(programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance());
    ImportSummary importSummary = enrollmentService.addEnrollment(enrollment, null, null);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    event =
        createEvent(
            programA.getUid(),
            programStageB.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementB.getUid());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    EventSearchParams params = new EventSearchParams();
    params.setProgram(programA);
    params.setOrgUnit(organisationUnitA);
    params.setOrgUnitSelectionMode(OrganisationUnitSelectionMode.SELECTED);
    assertEquals(2, eventService.getEvents(params).getEvents().size());
    event =
        createEvent(
            programA.getUid(),
            programStageB.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementB.getUid());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    assertEquals(3, eventService.getEvents(params).getEvents().size());
  }

  @Test
  void testDeleteEnrollmentWithEvents() {
    Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    eventService.addEvent(event, null, false);
    Enrollment enrollment =
        createEnrollment(programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance());
    enrollment.setEvents(Lists.newArrayList(event));
    ImportSummary importSummary = enrollmentService.addEnrollment(enrollment, null, null);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    TrackedEntityInstance tei =
        trackedEntityInstanceService.getTrackedEntityInstance(maleA.getUid());
    Enrollment retrievedEnrlollment =
        enrollmentService.getEnrollment(
            tei.getEnrollments().get(0).getEnrollment(), EnrollmentParams.FALSE);
    EventSearchParams params = new EventSearchParams();
    params.setProgram(programA);
    params.setOrgUnit(organisationUnitA);
    params.setOrgUnitSelectionMode(OrganisationUnitSelectionMode.SELECTED);
    Event retrievedEvent = enrollment.getEvents().get(0);
    assertNotNull(retrievedEnrlollment);
    assertNotNull(retrievedEvent);
    enrollmentService.deleteEnrollment(retrievedEnrlollment.getEnrollment());
    assertNull(
        enrollmentService.getEnrollment(
            tei.getEnrollments().get(0).getEnrollment(), EnrollmentParams.FALSE.FALSE));
    assertEquals(1, eventService.getEvents(params).getEvents().size());
  }

  @Test
  void testSaveRepeatableStageWithEventIdShouldNotCreateAdditionalEvents() {
    ImportOptions importOptions = new ImportOptions();
    importOptions.setImportStrategy(ImportStrategy.CREATE);
    Enrollment enrollment =
        createEnrollment(programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance());
    ImportSummary importSummary = enrollmentService.addEnrollment(enrollment, null, null);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    event =
        createEvent(
            programA.getUid(),
            programStageB.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementB.getUid());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    EventSearchParams params = new EventSearchParams();
    params.setProgram(programA);
    params.setOrgUnit(organisationUnitA);
    params.setOrgUnitSelectionMode(OrganisationUnitSelectionMode.SELECTED);
    assertEquals(2, eventService.getEvents(params).getEvents().size());
    event =
        createEvent(
            programA.getUid(),
            programStageB.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementB.getUid());
    event.setEvent(importSummary.getReference());
    importSummary = eventService.addEvent(event, importOptions, false);
    assertEquals(ImportStatus.ERROR, importSummary.getStatus());
    assertEquals(2, eventService.getEvents(params).getEvents().size());
    event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    importSummary = eventService.addEvent(event, importOptions, false);
    assertEquals(ImportStatus.ERROR, importSummary.getStatus());
    assertEquals(2, eventService.getEvents(params).getEvents().size());
  }

  @Test
  void testSaveEventToCompletedEnrollment() {
    ImportOptions importOptions = new ImportOptions();
    importOptions.setImportStrategy(ImportStrategy.CREATE_AND_UPDATE);
    EventSearchParams params = new EventSearchParams();
    params.setProgram(programA);
    params.setOrgUnit(organisationUnitA);
    params.setOrgUnitSelectionMode(OrganisationUnitSelectionMode.SELECTED);
    Enrollment enrollment =
        createEnrollment(programA.getUid(), trackedEntityInstanceMaleA.getTrackedEntityInstance());
    enrollment.setEnrollmentDate(new DateTime(2019, 1, 1, 0, 0, 0, 0).toDate());
    enrollment.setIncidentDate(new DateTime(2019, 1, 1, 0, 0, 0, 0).toDate());
    ImportSummary importSummary = enrollmentService.addEnrollment(enrollment, null, null);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    enrollment =
        enrollmentService.getEnrollment(importSummary.getReference(), EnrollmentParams.FALSE);
    Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementA.getUid());
    event.setEnrollment(enrollment.getEnrollment());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    enrollment.setCompletedDate(new DateTime(2019, 8, 20, 0, 0, 0, 0).toDate());
    enrollmentService.updateEnrollment(enrollment, null);
    importSummary = enrollmentService.updateEnrollment(enrollment, null);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    enrollment =
        enrollmentService.getEnrollment(enrollment.getEnrollment(), EnrollmentParams.FALSE);
    assertEquals(EnrollmentStatus.COMPLETED, enrollment.getStatus());
    event =
        createEvent(
            programA.getUid(),
            programStageB.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementB.getUid());
    event.setEnrollment(enrollment.getEnrollment());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    assertEquals(2, eventService.getEvents(params).getEvents().size());
    enrollmentService.incompleteEnrollment(enrollment.getEnrollment());
    enrollment =
        enrollmentService.getEnrollment(enrollment.getEnrollment(), EnrollmentParams.FALSE);
    assertEquals(EnrollmentStatus.ACTIVE, enrollment.getStatus());
    event =
        createEvent(
            programA.getUid(),
            programStageB.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance(),
            dataElementB.getUid());
    event.setEnrollment(enrollment.getEnrollment());
    importSummary = eventService.addEvent(event, null, false);
    assertEquals(ImportStatus.SUCCESS, importSummary.getStatus());
    assertEquals(3, eventService.getEvents(params).getEvents().size());
  }

  // -------------------------------------------------------------------------
  // Supportive tests
  // -------------------------------------------------------------------------
  private Enrollment createEnrollment(String program, String person) {
    Enrollment enrollment = new Enrollment();
    enrollment.setOrgUnit(organisationUnitA.getUid());
    enrollment.setProgram(program);
    enrollment.setTrackedEntityInstance(person);
    enrollment.setEnrollmentDate(new Date());
    enrollment.setIncidentDate(new Date());
    return enrollment;
  }

  private Event createEvent(
      String program, String programStage, String orgUnit, String person, String dataElement) {
    Event event = new Event();
    event.setUid(CodeGenerator.generateUid());
    event.setEvent(event.getUid());
    event.setProgram(program);
    event.setProgramStage(programStage);
    event.setOrgUnit(orgUnit);
    event.setTrackedEntityInstance(person);
    event.setEventDate("2013-01-01");
    event.getDataValues().add(new DataValue(dataElement, "10"));
    return event;
  }
}
