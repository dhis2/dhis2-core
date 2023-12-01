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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class TrackedEntityRegistrationListenerTest extends DhisConvenienceTest {
  private static final String TEI_REGISTRATION_COMMAND = "tei";

  private static final String ATTRIBUTE_VALUE = "TEST";

  private static final String SMS_TEXT = TEI_REGISTRATION_COMMAND + " " + "attr=sample";

  private static final String ORIGINATOR = "47400000";

  private static final String SUCCESS_MESSAGE = "Command has been processed successfully";

  @Mock private EnrollmentService enrollmentService;

  @Mock private CategoryService dataElementCategoryService;

  @Mock private EventService eventService;

  @Mock private UserService userService;

  @Mock private CurrentUserService currentUserService;

  @Mock private IncomingSmsService incomingSmsService;

  @Mock private MessageSender smsSender;

  @Mock private SMSCommandService smsCommandService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private ProgramService programService;

  private TrackedEntityRegistrationSMSListener subject;

  private TrackedEntityType trackedEntityType;

  private TrackedEntity trackedEntity;

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  private ProgramTrackedEntityAttribute programTrackedEntityAttribute;

  private Program program;

  private OrganisationUnit organisationUnit;

  private User user;

  private SMSCommand teiRegistrationCommand;

  private SMSCode smsCode;

  private IncomingSms incomingSms;

  private IncomingSms updatedIncomingSms;

  private OutboundMessageResponse response = new OutboundMessageResponse();

  private String message = "";

  @BeforeEach
  public void initTest() {
    subject =
        new TrackedEntityRegistrationSMSListener(
            programService,
            enrollmentService,
            dataElementCategoryService,
            eventService,
            userService,
            currentUserService,
            incomingSmsService,
            smsSender,
            smsCommandService,
            trackedEntityTypeService,
            trackedEntityService);

    setUpInstances();

    // Mock for smsCommandService
    when(smsCommandService.getSMSCommand(anyString(), any())).thenReturn(teiRegistrationCommand);

    // Mock for userService
    when(userService.getUser(anyString())).thenReturn(user);

    // Mock for smsSender
    when(smsSender.isConfigured()).thenReturn(true);

    when(smsSender.sendMessage(any(), any(), anyString()))
        .thenAnswer(
            invocation -> {
              message = (String) invocation.getArguments()[1];
              return response;
            });

    when(programService.hasOrgUnit(program, organisationUnit)).thenReturn(false);
  }

  @Test
  void testTeiRegistration() {
    // Mock for trackedEntityService
    when(trackedEntityService.createTrackedEntity(any(), any())).thenReturn(1L);
    when(trackedEntityService.getTrackedEntity(anyLong())).thenReturn(trackedEntity);
    when(programService.hasOrgUnit(program, organisationUnit)).thenReturn(true);

    // Mock for incomingSmsService
    doAnswer(
            invocation -> {
              updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
              return updatedIncomingSms;
            })
        .when(incomingSmsService)
        .update(any());

    subject.receive(incomingSms);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testIfProgramHasNoOu() {
    Program programA = createProgram('P');

    teiRegistrationCommand.setProgram(programA);

    assertThrows(SMSParserException.class, () -> subject.receive(incomingSms));

    verify(trackedEntityTypeService, never()).getTrackedEntityByName(anyString());
  }

  private void setUpInstances() {
    trackedEntityType = createTrackedEntityType('T');
    organisationUnit = createOrganisationUnit('O');
    program = createProgram('P');

    user = makeUser("U");
    user.setPhoneNumber(ORIGINATOR);
    user.setOrganisationUnits(Sets.newHashSet(organisationUnit));

    programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(program, trackedEntityAttribute);
    trackedEntityAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);
    program.getProgramAttributes().add(programTrackedEntityAttribute);
    program.getOrganisationUnits().add(organisationUnit);
    program.setTrackedEntityType(trackedEntityType);

    trackedEntity = createTrackedEntity(organisationUnit);
    trackedEntity.getTrackedEntityAttributeValues().add(trackedEntityAttributeValue);
    trackedEntity.setOrganisationUnit(organisationUnit);

    trackedEntityAttributeValue =
        createTrackedEntityAttributeValue('A', trackedEntity, trackedEntityAttribute);
    trackedEntityAttributeValue.setValue(ATTRIBUTE_VALUE);

    smsCode = new SMSCode();
    smsCode.setCode("attr");
    smsCode.setTrackedEntityAttribute(trackedEntityAttribute);

    teiRegistrationCommand = new SMSCommand();
    teiRegistrationCommand.setName(TEI_REGISTRATION_COMMAND);
    teiRegistrationCommand.setParserType(ParserType.TRACKED_ENTITY_REGISTRATION_PARSER);
    teiRegistrationCommand.setProgram(program);
    teiRegistrationCommand.setCodes(Sets.newHashSet(smsCode));

    incomingSms = new IncomingSms();
    incomingSms.setText(SMS_TEXT);
    incomingSms.setOriginator(ORIGINATOR);
    incomingSms.setCreatedBy(user);
  }
}
