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
package org.hisp.dhis.sms.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsConsts.SmsEnrollmentStatus;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.models.EnrollmentSmsSubmission;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SmsAttributeValue;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsEvent;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentSMSListenerTest extends CompressionSMSListenerTest {

  @Mock private UserService userService;

  @Mock private IncomingSmsService incomingSmsService;

  @Mock private MessageSender smsSender;

  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private ProgramStageService programStageService;

  @Mock private ProgramStageInstanceService programStageInstanceService;

  // Needed for this test

  @Mock private TrackedEntityInstanceService teiService;

  @Mock private ProgramInstanceService programInstanceService;

  @Mock private TrackedEntityAttributeValueService attributeValueService;

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  EnrollmentSMSListener subject;

  // Needed for all

  private User user;

  private OutboundMessageResponse response = new OutboundMessageResponse();

  private IncomingSms updatedIncomingSms;

  private String message = "";

  // Needed for this test

  private IncomingSms incomingSmsEnrollmentNoEvents;

  private IncomingSms incomingSmsEnrollmentWithEvents;

  private IncomingSms incomingSmsEnrollmentWithNulls;

  private IncomingSms incomingSmsEnrollmentNoAttribs;

  private IncomingSms incomingSmsEnrollmentEventWithNulls;

  private IncomingSms incomingSmsEnrollmentEventNoValues;

  private OrganisationUnit organisationUnit;

  private Program program;

  private ProgramStage programStage;

  private ProgramInstance programInstance;

  private ProgramStageInstance programStageInstance;

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  private TrackedEntityType trackedEntityType;

  private TrackedEntityInstance trackedEntityInstance;

  private CategoryOptionCombo categoryOptionCombo;

  private DataElement dataElement;

  @BeforeEach
  public void initTest() throws SmsCompressionException {
    subject =
        new EnrollmentSMSListener(
            incomingSmsService,
            smsSender,
            userService,
            trackedEntityTypeService,
            trackedEntityAttributeService,
            programService,
            organisationUnitService,
            categoryService,
            dataElementService,
            programStageService,
            programStageInstanceService,
            attributeValueService,
            teiService,
            programInstanceService,
            identifiableObjectManager);

    setUpInstances();

    when(userService.getUser(anyString())).thenReturn(user);
    when(smsSender.isConfigured()).thenReturn(true);
    when(smsSender.sendMessage(any(), any(), anyString()))
        .thenAnswer(
            invocation -> {
              message = (String) invocation.getArguments()[1];
              return response;
            });

    when(organisationUnitService.getOrganisationUnit(anyString())).thenReturn(organisationUnit);
    when(programService.getProgram(anyString())).thenReturn(program);
    when(trackedEntityTypeService.getTrackedEntityType(anyString())).thenReturn(trackedEntityType);
    when(programInstanceService.enrollTrackedEntityInstance(
            any(), any(), any(), any(), any(), any()))
        .thenReturn(programInstance);
    when(programService.hasOrgUnit(any(Program.class), any(OrganisationUnit.class)))
        .thenReturn(true);

    doAnswer(
            invocation -> {
              updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
              return updatedIncomingSms;
            })
        .when(incomingSmsService)
        .update(any());
  }

  @Test
  void testEnrollmentNoEvents() {
    when(trackedEntityAttributeService.getTrackedEntityAttribute(anyString()))
        .thenReturn(trackedEntityAttribute);

    subject.receive(incomingSmsEnrollmentNoEvents);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testEnrollmentWithEvents() {
    when(dataElementService.getDataElement(anyString())).thenReturn(dataElement);
    when(categoryService.getCategoryOptionCombo(anyString())).thenReturn(categoryOptionCombo);
    when(programStageService.getProgramStage(anyString())).thenReturn(programStage);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(anyString()))
        .thenReturn(trackedEntityAttribute);

    subject.receive(incomingSmsEnrollmentWithEvents);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testEnrollmentWithEventsRepeat() {
    when(categoryService.getCategoryOptionCombo(anyString())).thenReturn(categoryOptionCombo);
    when(dataElementService.getDataElement(anyString())).thenReturn(dataElement);
    when(programStageService.getProgramStage(anyString())).thenReturn(programStage);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(anyString()))
        .thenReturn(trackedEntityAttribute);

    subject.receive(incomingSmsEnrollmentWithEvents);
    subject.receive(incomingSmsEnrollmentWithEvents);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(2)).update(any());
  }

  @Test
  void testEnrollmentWithNulls() {
    when(trackedEntityAttributeService.getTrackedEntityAttribute(anyString()))
        .thenReturn(trackedEntityAttribute);

    subject.receive(incomingSmsEnrollmentWithNulls);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testEnrollmentNoAttribs() {
    subject.receive(incomingSmsEnrollmentNoAttribs);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(NOATTRIBS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testEnrollmentEventWithNulls() {
    when(categoryService.getCategoryOptionCombo(anyString())).thenReturn(categoryOptionCombo);
    when(dataElementService.getDataElement(anyString())).thenReturn(dataElement);
    when(programStageService.getProgramStage(anyString())).thenReturn(programStage);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(anyString()))
        .thenReturn(trackedEntityAttribute);

    subject.receive(incomingSmsEnrollmentEventWithNulls);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  // For now there's no warning if an event within the event
  // list has no values. This might be changed in the future.
  @Test
  void testEnrollmentEventNoValues() {
    when(categoryService.getCategoryOptionCombo(anyString())).thenReturn(categoryOptionCombo);
    when(programStageService.getProgramStage(anyString())).thenReturn(programStage);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(anyString()))
        .thenReturn(trackedEntityAttribute);

    subject.receive(incomingSmsEnrollmentEventNoValues);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  private void setUpInstances() throws SmsCompressionException {
    trackedEntityType = createTrackedEntityType('T');
    organisationUnit = createOrganisationUnit('O');
    program = createProgram('P');
    programStage = createProgramStage('S', program);

    user = createUser('U');
    user.setPhoneNumber(ORIGINATOR);
    user.setOrganisationUnits(Sets.newHashSet(organisationUnit));

    trackedEntityAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);
    final ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(program, trackedEntityAttribute);
    program.getProgramAttributes().add(programTrackedEntityAttribute);
    program.getOrganisationUnits().add(organisationUnit);
    program.setTrackedEntityType(trackedEntityType);
    HashSet<ProgramStage> stages = new HashSet<>();
    stages.add(programStage);
    program.setProgramStages(stages);

    programInstance = new ProgramInstance();
    programInstance.setAutoFields();
    programInstance.setProgram(program);

    programStageInstance = new ProgramStageInstance();
    programStageInstance.setAutoFields();

    trackedEntityInstance = createTrackedEntityInstance(organisationUnit);
    trackedEntityInstance.getTrackedEntityAttributeValues().add(trackedEntityAttributeValue);
    trackedEntityInstance.setOrganisationUnit(organisationUnit);

    trackedEntityAttributeValue =
        createTrackedEntityAttributeValue('A', trackedEntityInstance, trackedEntityAttribute);
    trackedEntityAttributeValue.setValue(ATTRIBUTE_VALUE);

    categoryOptionCombo = createCategoryOptionCombo('C');
    dataElement = createDataElement('D');

    incomingSmsEnrollmentNoEvents = createSMSFromSubmission(createEnrollmentSubmissionNoEvents());
    incomingSmsEnrollmentWithEvents =
        createSMSFromSubmission(createEnrollmentSubmissionWithEvents());
    incomingSmsEnrollmentWithNulls = createSMSFromSubmission(createEnrollmentSubmissionWithNulls());
    incomingSmsEnrollmentNoAttribs = createSMSFromSubmission(createEnrollmentSubmissionNoAttribs());
    incomingSmsEnrollmentEventWithNulls =
        createSMSFromSubmission(createEnrollmentSubmissionEventWithNulls());
    incomingSmsEnrollmentEventNoValues =
        createSMSFromSubmission(createEnrollmentSubmissionEventNoValues());
  }

  private EnrollmentSmsSubmission createEnrollmentSubmissionNoEvents() {
    EnrollmentSmsSubmission subm = new EnrollmentSmsSubmission();

    subm.setUserId(user.getUid());
    subm.setOrgUnit(organisationUnit.getUid());
    subm.setTrackerProgram(program.getUid());
    subm.setTrackedEntityType(trackedEntityType.getUid());
    subm.setTrackedEntityInstance(trackedEntityInstance.getUid());
    subm.setEnrollment(programInstance.getUid());
    subm.setEnrollmentDate(new Date());
    subm.setIncidentDate(new Date());
    subm.setEnrollmentStatus(SmsEnrollmentStatus.ACTIVE);
    subm.setCoordinates(new GeoPoint(59.9399586f, 10.7195609f));
    ArrayList<SmsAttributeValue> values = new ArrayList<>();
    values.add(new SmsAttributeValue(trackedEntityAttribute.getUid(), ATTRIBUTE_VALUE));
    subm.setValues(values);
    subm.setSubmissionId(1);

    return subm;
  }

  private EnrollmentSmsSubmission createEnrollmentSubmissionWithEvents() {
    EnrollmentSmsSubmission subm = createEnrollmentSubmissionNoEvents();

    ArrayList<SmsEvent> events = new ArrayList<>();
    events.add(createEvent());
    subm.setEvents(events);
    return subm;
  }

  private SmsEvent createEvent() {
    SmsEvent event = new SmsEvent();
    event.setOrgUnit(organisationUnit.getUid());
    event.setProgramStage(programStage.getUid());
    event.setAttributeOptionCombo(categoryOptionCombo.getUid());
    event.setEvent(programStageInstance.getUid());
    event.setEventStatus(SmsEventStatus.COMPLETED);
    event.setEventDate(new Date());
    event.setDueDate(new Date());
    event.setCoordinates(new GeoPoint(59.9399586f, 10.7195609f));
    ArrayList<SmsDataValue> eventValues = new ArrayList<>();
    eventValues.add(new SmsDataValue(categoryOptionCombo.getUid(), dataElement.getUid(), "10"));
    event.setValues(eventValues);

    return event;
  }

  private EnrollmentSmsSubmission createEnrollmentSubmissionWithNulls() {
    EnrollmentSmsSubmission subm = createEnrollmentSubmissionNoEvents();
    subm.setEnrollmentDate(null);
    subm.setIncidentDate(null);
    subm.setCoordinates(null);
    subm.setEvents(null);

    return subm;
  }

  private EnrollmentSmsSubmission createEnrollmentSubmissionNoAttribs() {
    EnrollmentSmsSubmission subm = createEnrollmentSubmissionNoEvents();
    subm.setValues(null);

    return subm;
  }

  private EnrollmentSmsSubmission createEnrollmentSubmissionEventWithNulls() {
    EnrollmentSmsSubmission subm = createEnrollmentSubmissionNoEvents();
    SmsEvent event = createEvent();
    event.setEventDate(null);
    event.setDueDate(null);
    event.setCoordinates(null);
    ArrayList<SmsEvent> events = new ArrayList<>();
    events.add(event);
    subm.setEvents(events);

    return subm;
  }

  private EnrollmentSmsSubmission createEnrollmentSubmissionEventNoValues() {
    EnrollmentSmsSubmission subm = createEnrollmentSubmissionNoEvents();
    SmsEvent event = createEvent();
    event.setValues(null);
    ArrayList<SmsEvent> events = new ArrayList<>();
    events.add(event);
    subm.setEvents(events);

    return subm;
  }
}
