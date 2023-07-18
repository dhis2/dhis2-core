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
import static org.mockito.Mockito.lenient;
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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SimpleEventSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleEventSMSListenerTest extends CompressionSMSListenerTest {

  @Mock private UserService userService;

  @Mock private IncomingSmsService incomingSmsService;

  @Mock private MessageSender smsSender;

  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private ProgramStageInstanceService programStageInstanceService;

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  private User user;

  private OutboundMessageResponse response = new OutboundMessageResponse();

  private IncomingSms updatedIncomingSms;

  private String message = "";

  // Needed for this test

  @Mock private ProgramInstanceService programInstanceService;

  private SimpleEventSMSListener subject;

  private IncomingSms incomingSmsSimpleEvent;

  private IncomingSms incomingSmsSimpleEventWithNulls;

  private IncomingSms incomingSmsSimpleEventNoValues;

  private OrganisationUnit organisationUnit;

  private CategoryOptionCombo categoryOptionCombo;

  private DataElement dataElement;

  private Program program;

  private ProgramStageInstance programStageInstance;

  @BeforeEach
  public void initTest() throws SmsCompressionException {
    subject =
        new SimpleEventSMSListener(
            incomingSmsService,
            smsSender,
            userService,
            trackedEntityTypeService,
            trackedEntityAttributeService,
            programService,
            organisationUnitService,
            categoryService,
            dataElementService,
            programStageInstanceService,
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
    lenient().when(dataElementService.getDataElement(anyString())).thenReturn(dataElement);
    when(categoryService.getCategoryOptionCombo(anyString())).thenReturn(categoryOptionCombo);

    doAnswer(
            invocation -> {
              updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
              return updatedIncomingSms;
            })
        .when(incomingSmsService)
        .update(any());

    when(programService.hasOrgUnit(any(Program.class), any(OrganisationUnit.class)))
        .thenReturn(true);
  }

  @Test
  void testSimpleEvent() {
    subject.receive(incomingSmsSimpleEvent);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testSimpleEventRepeat() {
    subject.receive(incomingSmsSimpleEvent);
    subject.receive(incomingSmsSimpleEvent);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(2)).update(any());
  }

  @Test
  void testSimpleEventWithNulls() {
    subject.receive(incomingSmsSimpleEventWithNulls);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testSimpleEventNoValues() {
    subject.receive(incomingSmsSimpleEventNoValues);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(NOVALUES_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  private void setUpInstances() throws SmsCompressionException {
    organisationUnit = createOrganisationUnit('O');
    program = createProgram('P');
    ProgramStage programStage = createProgramStage('S', program);

    user = makeUser("U");
    user.setPhoneNumber(ORIGINATOR);
    user.setOrganisationUnits(Sets.newHashSet(organisationUnit));

    categoryOptionCombo = createCategoryOptionCombo('C');
    dataElement = createDataElement('D');

    program.getOrganisationUnits().add(organisationUnit);
    HashSet<ProgramStage> stages = new HashSet<>();
    stages.add(programStage);
    program.setProgramStages(stages);

    programStageInstance = new ProgramStageInstance();
    programStageInstance.setAutoFields();

    incomingSmsSimpleEvent = createSMSFromSubmission(createSimpleEventSubmission());
    incomingSmsSimpleEventWithNulls =
        createSMSFromSubmission(createSimpleEventSubmissionWithNulls());
    incomingSmsSimpleEventNoValues = createSMSFromSubmission(createSimpleEventSubmissionNoValues());
  }

  private SimpleEventSmsSubmission createSimpleEventSubmission() {
    SimpleEventSmsSubmission subm = new SimpleEventSmsSubmission();

    subm.setUserId(user.getUid());
    subm.setOrgUnit(organisationUnit.getUid());
    subm.setEventProgram(program.getUid());
    subm.setAttributeOptionCombo(categoryOptionCombo.getUid());
    subm.setEvent(programStageInstance.getUid());
    subm.setEventStatus(SmsEventStatus.COMPLETED);
    subm.setEventDate(new Date());
    subm.setDueDate(new Date());
    subm.setCoordinates(new GeoPoint(59.9399586f, 10.7195609f));

    ArrayList<SmsDataValue> values = new ArrayList<>();
    values.add(new SmsDataValue(categoryOptionCombo.getUid(), dataElement.getUid(), "true"));
    subm.setValues(values);
    subm.setSubmissionId(1);

    return subm;
  }

  private SimpleEventSmsSubmission createSimpleEventSubmissionWithNulls() {
    SimpleEventSmsSubmission subm = createSimpleEventSubmission();
    subm.setEventDate(null);
    subm.setDueDate(null);
    subm.setCoordinates(null);

    return subm;
  }

  private SimpleEventSmsSubmission createSimpleEventSubmissionNoValues() {
    SimpleEventSmsSubmission subm = createSimpleEventSubmission();
    subm.setValues(null);

    return subm;
  }
}
