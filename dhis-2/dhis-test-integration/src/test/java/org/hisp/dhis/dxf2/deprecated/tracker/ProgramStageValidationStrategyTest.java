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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David Katuscak
 */
class ProgramStageValidationStrategyTest extends TransactionalIntegrationTest {
  @Autowired private org.hisp.dhis.dxf2.deprecated.tracker.event.EventService eventService;

  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private UserService _userService;

  @Autowired protected CurrentUserService currentUserService;

  @Autowired private EventService programStageInstanceService;

  private TrackedEntityInstance trackedEntityInstanceMaleA;

  private OrganisationUnit organisationUnitA;

  private DataValue dataValueAMissing;

  private DataValue dataValueBMissing;

  private DataValue dataValueCMissing;

  private DataValue dataValueA;

  private DataValue dataValueB;

  private DataValue dataValueC;

  private Program programA;

  private ProgramStage programStageA;

  @Override
  protected void setUpTest() {
    final int testYear = Calendar.getInstance().get(Calendar.YEAR) - 1;
    userService = _userService;
    createUserAndInjectSecurityContext(
        false,
        "F_TRACKED_ENTITY_DATAVALUE_ADD",
        "F_TRACKED_ENTITY_DATAVALUE_DELETE",
        "F_UNCOMPLETE_EVENT",
        "F_PROGRAMSTAGE_ADD",
        "F_PROGRAMSTAGE_DELETE",
        "F_PROGRAM_PUBLIC_ADD",
        "F_PROGRAM_PRIVATE_ADD",
        "F_PROGRAM_DELETE",
        "F_TRACKED_ENTITY_ADD",
        "F_TRACKED_ENTITY_UPDATE",
        "F_TRACKED_ENTITY_DELETE",
        "F_DATAELEMENT_PUBLIC_ADD",
        "F_DATAELEMENT_PRIVATE_ADD",
        "F_DATAELEMENT_DELETE",
        "F_CATEGORY_COMBO_PUBLIC_ADD",
        "F_CATEGORY_COMBO_PRIVATE_ADD",
        "F_CATEGORY_COMBO_DELETE");
    User currentUser = currentUserService.getCurrentUser();
    UserAccess userAccess1 = new UserAccess(currentUser, "rwrw----");
    UserAccess userAccess2 = new UserAccess(currentUser, "rwrw----");
    UserAccess userAccess3 = new UserAccess(currentUser, "rwrw----");
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitA.addUser(currentUser);
    organisationUnitA.getSharing().addUserAccess(userAccess1);
    currentUser.getTeiSearchOrganisationUnits().add(organisationUnitA);
    manager.save(organisationUnitA, false);
    userService.updateUser(currentUser);
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.getSharing().addUserAccess(userAccess1);
    manager.save(trackedEntityType, false);
    TrackedEntity maleA = createTrackedEntity(organisationUnitA);
    maleA.setTrackedEntityType(trackedEntityType);
    maleA.getSharing().addUserAccess(userAccess1);
    maleA.setCreatedBy(currentUser);
    manager.save(maleA, false);
    trackedEntityInstanceMaleA = trackedEntityInstanceService.getTrackedEntityInstance(maleA);
    DataElement dataElementA = createDataElement('A');
    dataElementA.setValueType(ValueType.INTEGER);
    dataElementA.getSharing().addUserAccess(userAccess1);
    manager.save(dataElementA, false);
    DataElement dataElementB = createDataElement('B');
    dataElementB.setValueType(ValueType.TEXT);
    dataElementB.getSharing().addUserAccess(userAccess2);
    manager.save(dataElementB, false);
    DataElement dataElementC = createDataElement('C');
    dataElementC.setValueType(ValueType.INTEGER);
    dataElementC.getSharing().addUserAccess(userAccess3);
    manager.save(dataElementC, false);
    programStageA = createProgramStage('A', 0);
    programStageA.setValidationStrategy(ValidationStrategy.ON_COMPLETE);
    programStageA.getSharing().addUserAccess(userAccess1);
    manager.save(programStageA, false);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programA.getSharing().addUserAccess(userAccess1);
    manager.save(programA, false);
    // Create a compulsory PSDE
    ProgramStageDataElement programStageDataElementA = new ProgramStageDataElement();
    programStageDataElementA.setDataElement(dataElementA);
    programStageDataElementA.setProgramStage(programStageA);
    programStageDataElementA.setCompulsory(true);
    programStageDataElementA.getSharing().addUserAccess(userAccess1);
    manager.save(programStageDataElementA, false);
    // Create a compulsory PSDE
    ProgramStageDataElement programStageDataElementB = new ProgramStageDataElement();
    programStageDataElementB.setDataElement(dataElementB);
    programStageDataElementB.setProgramStage(programStageA);
    programStageDataElementB.setCompulsory(true);
    programStageDataElementB.getSharing().addUserAccess(userAccess1);
    manager.save(programStageDataElementB, false);
    // Create a NON-compulsory PSDE
    ProgramStageDataElement programStageDataElementC = new ProgramStageDataElement();
    programStageDataElementC.setDataElement(dataElementC);
    programStageDataElementC.setProgramStage(programStageA);
    programStageDataElementC.setCompulsory(false);
    programStageDataElementC.getSharing().addUserAccess(userAccess1);
    manager.save(programStageDataElementC, false);
    // Assign all 3 created PSDEs to created ProgramStage programStageA and
    // to
    // created Program programA
    programStageA.getProgramStageDataElements().add(programStageDataElementA);
    programStageA.getProgramStageDataElements().add(programStageDataElementB);
    programStageA.getProgramStageDataElements().add(programStageDataElementC);
    programStageA.setProgram(programA);
    programA.getProgramStages().add(programStageA);
    manager.update(programStageA);
    manager.update(programA);
    Enrollment enrollment = new Enrollment();
    enrollment.setProgram(programA);
    enrollment.setIncidentDate(new Date());
    enrollment.setEnrollmentDate(new Date());
    enrollment.setTrackedEntity(maleA);
    enrollment.getSharing().addUserAccess(userAccess1);
    maleA.getEnrollments().add(enrollment);
    manager.save(enrollment, false);
    manager.update(maleA);
    Period periodA = createPeriod(testYear + "03");
    periodA.getSharing().addUserAccess(userAccess1);
    manager.save(periodA, false);
    CategoryCombo categoryComboA = createCategoryCombo('A');
    CategoryOptionCombo categoryOptionComboA = createCategoryOptionCombo('A');
    categoryOptionComboA.setCategoryCombo(categoryComboA);
    categoryComboA.getSharing().addUserAccess(userAccess1);
    categoryOptionComboA.getSharing().addUserAccess(userAccess1);
    manager.save(categoryComboA, false);
    manager.save(categoryOptionComboA, false);
    dataValueAMissing = new DataValue(dataElementA.getUid(), "");
    dataValueBMissing = new DataValue(dataElementB.getUid(), "");
    dataValueCMissing = new DataValue(dataElementC.getUid(), "");
    dataValueA = new DataValue(dataElementA.getUid(), "42");
    dataValueB = new DataValue(dataElementB.getUid(), "Ford Prefect");
    dataValueC = new DataValue(dataElementC.getUid(), "84");
  }

  /*
   * #########################################################################
   * #### #################################
   * #########################################################################
   * #### #################################
   * #########################################################################
   * #### ################################# Following tests test
   * creation/update of complete Event (Basically what /events endpoint does)
   * #########################################################################
   * #### #################################
   * #########################################################################
   * #### #################################
   * #########################################################################
   * #### #################################
   */
  /*
   * ####################################################### Tests with
   * ValidationStrategy.ON_UPDATE_AND_INSERT
   * #######################################################
   */
  @Test
  void missingCompulsoryDataElementWithValidationOnUpdateShouldFailTest() {
    validationOnInsertUpdate(programStageA);
    assertInvalidImport(addEvent(createDefaultEvent(dataValueA, dataValueBMissing, dataValueC)));
  }

  @Test
  void correctCompulsoryDataElementsWithValidationOnUpdateShouldPassTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueB, dataValueC);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  @Test
  void missingCompulsoryDataElementAndCompletedEventWithValidationOnUpdateShouldFailTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueBMissing, dataValueC);
    event.setStatus(EventStatus.COMPLETED);
    assertInvalidImport(addEvent(event));
  }

  @Test
  void correctCompulsoryDataElementAndCompletedEventWithValidationOnUpdateShouldPassTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueB, dataValueC);
    event.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  /*
   * ####################################################### Tests with
   * ValidationStrategy.ON_COMPLETE
   * #######################################################
   */
  @Test
  void missingCompulsoryDataElementWithValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueBMissing, dataValueC);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueC));
  }

  @Test
  void correctCompulsoryDataElementsWithValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueB, dataValueC);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  @Test
  void missingCompulsoryDataElementAndCompletedEventWithValidationOnCompleteShouldFailTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueBMissing, dataValueC);
    event.setStatus(EventStatus.COMPLETED);
    assertInvalidImport(addEvent(event));
  }

  @Test
  void correctCompulsoryDataElementAndCompletedEventWithValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueB, dataValueC);
    event.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  /*
   * #########################################################################
   * #### ##########################################
   * #########################################################################
   * #### ##########################################
   * #########################################################################
   * #### ########################################## Following tests test
   * update of 1 specific data element (Basically what
   * /events/{uid}/{dataElementUid} endpoint does)
   * #########################################################################
   * #### ##########################################
   * #########################################################################
   * #### ##########################################
   * #########################################################################
   * #### ##########################################
   */
  /*
   * ####################################################### Tests with
   * ValidationStrategy.ON_UPDATE_AND_INSERT
   * #######################################################
   */
  @Test
  void compulsoryDataElementWithEmptyValueAndValidationOnUpdateShouldFailTest() {
    validationOnInsertUpdate(programStageA);
    // Create event having 3 Data Values
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    // Single value update -> should pass -> because data values are fetched
    // from DB
    // and merged
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueBMissing);
    // FIXME
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    // this
    // should
    // fail
    // because
    // 'dataValueB'
    // is
    // mandatory
    // assertDataValuesOnPsi( event.getEvent(), checkDataValue( dataValueA),
    // checkDataValue( dataValueB ), checkDataValue( dataValueC ) );
    // NOT a single value update -> should fail -> because data values are
    // NOT
    // fetched from DB and so NOT merged
    updatedEvent = createDefaultEvent(event.getUid(), dataValueBMissing);
    assertInvalidImport(updateEvent(updatedEvent));
  }

  @Test
  void correctCompulsoryDataElementAndValidationOnUpdateShouldPassTest() {
    validationOnInsertUpdate(programStageA);
    // Create event
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    // Update Data Value value
    dataValueB.setValue("new value");
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueB);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  @Test
  void
      correctCompulsoryDataElementButOtherCompulsoryMissingInDBAndValidationOnUpdateShouldFailTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueAMissing, dataValueB, dataValueC);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueB), checkDataValue(dataValueC));
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueB);
    assertInvalidImport(updateEventWithSingleValueUpdate(updatedEvent));
  }

  @Test
  void emptyNonCompulsoryDataElementAndValidationOnUpdateShouldPassTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    // DataValueC is not mandatory - so ok to remove
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueCMissing);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueB));
  }

  @Test
  void compulsoryDataElementWithEmptyValueCompletedEventAndValidationOnUpdateShouldFailTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    // Single value update -> should pass -> because data values are fetched
    // from DB
    // and merged
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueBMissing);
    updatedEvent.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueC));
    // NOT a single value update -> should fail -> because data values are
    // NOT
    // fetched from DB and so NOT merged
    assertInvalidImport(updateEvent(updatedEvent));
  }

  @Test
  void correctCompulsoryDataElementWithCompletedEventAndValidationOnUpdateShouldPassTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    dataValueB.setValue("new value");
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueB);
    updatedEvent.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  @Test
  void emptyNonCompulsoryDataElementWithCompletedEventAndValidationOnUpdateShouldPassTest() {
    validationOnInsertUpdate(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueCMissing);
    updatedEvent.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueB));
  }

  /*
   * ####################################################### Tests with
   * ValidationStrategy.ON_COMPLETE
   * #######################################################
   */
  @Test
  void compulsoryDataElementWithEmptyValueAndValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueBMissing);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueC));
  }

  @Test
  void correctCompulsoryDataElementAndValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    dataValueB.setValue("new value");
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueB);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  @Test
  void emptyNonCompulsoryDataElementAndValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueCMissing);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueB));
  }

  @Test
  void compulsoryDataElementWithEmptyValueCompletedEventAndValidationOnCompleteShouldFailTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    // Single value update -> should pass -> because data values are fetched
    // from DB
    // and merged
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueBMissing);
    updatedEvent.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueC));
    // NOT a single value update -> should fail -> because data values are
    // NOT
    // fetched from DB and so NOT merged
    assertInvalidImport(updateEvent(updatedEvent));
  }

  @Test
  void correctCompulsoryDataElementWithCompletedEventAndValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueB);
    updatedEvent.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
  }

  @Test
  void emptyNonCompulsoryDataElementWithCompletedEventAndValidationOnCompleteShouldPassTest() {
    validationOnComplete(programStageA);
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = addDefaultEvent();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event updatedEvent =
        createDefaultEvent(event.getUid(), dataValueCMissing);
    updatedEvent.setStatus(EventStatus.COMPLETED);
    assertSuccessfulImport(updateEventWithSingleValueUpdate(updatedEvent));
    assertDataValuesOnPsi(event.getEvent(), checkDataValue(dataValueA), checkDataValue(dataValueB));
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.event.Event createEvent(
      String program, String programStage, String orgUnit, String person) {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
    event.setProgram(program);
    event.setProgramStage(programStage);
    event.setOrgUnit(orgUnit);
    event.setTrackedEntityInstance(person);
    event.setEventDate("2013-01-01");
    return event;
  }

  private ImportSummary addEvent(org.hisp.dhis.dxf2.deprecated.tracker.event.Event event) {
    return eventService.addEvent(event, null, false);
  }

  private ImportSummary updateEventWithSingleValueUpdate(
      org.hisp.dhis.dxf2.deprecated.tracker.event.Event event) {
    return eventService.updateEvent(event, true, null, false);
  }

  private ImportSummary updateEvent(org.hisp.dhis.dxf2.deprecated.tracker.event.Event event) {
    return eventService.updateEvent(event, false, null, false);
  }

  @Autowired private SessionFactory sessionFactory;

  private Event getPsi(String event) {
    sessionFactory.getCurrentSession().clear();
    return programStageInstanceService.getEvent(event);
  }

  private void assertDataValuesOnPsi(String event, DataValueAsserter... dataValues) {
    final Event psi = getPsi(event);
    assertEquals(dataValues.length, psi.getEventDataValues().size(), print(psi, dataValues));
    for (DataValueAsserter dataValue : dataValues) {
      assertThat(
          psi.getEventDataValues(),
          hasItem(
              allOf(
                  Matchers.<EventDataValue>hasProperty("value", is(dataValue.getValue())),
                  Matchers.<EventDataValue>hasProperty(
                      "dataElement", is(dataValue.getDataElement())))));
    }
  }

  private String print(Event psi, DataValueAsserter... dataValues) {
    StringBuilder sb = new StringBuilder();
    sb.append("PSI on database has the following Data Values: \n");
    psi.getEventDataValues()
        .forEach(
            e ->
                sb.append(e.getDataElement())
                    .append(" - value: ")
                    .append(e.getValue())
                    .append("\n"));
    sb.append("---------------\n");
    sb.append("Expecting: \n");
    Stream.of(dataValues).forEach(d -> sb.append(d.getDataElement()).append("\n"));
    return sb.toString();
  }

  private void assertSuccessfulImport(ImportSummary importSummary) {
    assertEquals(importSummary.getStatus(), ImportStatus.SUCCESS);
  }

  private void assertInvalidImport(ImportSummary importSummary) {
    assertEquals(importSummary.getStatus(), ImportStatus.ERROR);
  }

  private DataValueAsserter checkDataValue(DataValue dataValue) {
    return DataValueAsserter.builder()
        .value(dataValue.getValue())
        .dataElement(dataValue.getDataElement())
        .build();
  }

  private void validationOnInsertUpdate(ProgramStage programStage) {
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    manager.update(programStage);
  }

  private void validationOnComplete(ProgramStage programStage) {
    programStage.setValidationStrategy(ValidationStrategy.ON_COMPLETE);
    manager.update(programStage);
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.event.Event createDefaultEvent(
      DataValue... dataValues) {
    final String uid = CodeGenerator.generateUid();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance());
    event.getDataValues().addAll(Arrays.asList(dataValues));
    event.setUid(uid);
    event.setEvent(uid);
    return event;
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.event.Event createDefaultEvent(
      String uid, DataValue... dataValues) {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createEvent(
            programA.getUid(),
            programStageA.getUid(),
            organisationUnitA.getUid(),
            trackedEntityInstanceMaleA.getTrackedEntityInstance());
    event.getDataValues().addAll(Arrays.asList(dataValues));
    event.setUid(uid);
    event.setEvent(uid);
    return event;
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.event.Event addDefaultEvent() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        createDefaultEvent(dataValueA, dataValueB, dataValueC);
    assertSuccessfulImport(addEvent(event));
    assertDataValuesOnPsi(
        event.getEvent(),
        checkDataValue(dataValueA),
        checkDataValue(dataValueB),
        checkDataValue(dataValueC));
    return event;
  }
}
