/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static java.lang.String.format;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sms.SmsMessageSender;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.SmsSubmissionWriter;
import org.hisp.dhis.smscompression.models.DeleteSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsMetadata;
import org.hisp.dhis.test.message.FakeMessageSender;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests tracker SMS to delete an event implemented via {@link
 * org.hisp.dhis.tracker.imports.sms.DeleteEventSMSListener}. It also tests parts of {@link
 * org.hisp.dhis.webapi.controller.sms.SmsInboundController} and other SMS classes in the SMS class
 * hierarchy.
 */
@ContextConfiguration(classes = {TrackerDeleteEventSMSTest.Config.class})
class TrackerDeleteEventSMSTest extends PostgresControllerIntegrationTestBase {
  private static final int SMS_COMPRESSION_VERSION = 2;

  static class Config {
    @Primary
    @Bean
    public SmsMessageSender smsMessageSender() {
      return new FakeMessageSender();
    }
  }

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  @Autowired private EventService eventService;

  @Autowired private IncomingSmsService incomingSmsService;

  @Autowired private FakeMessageSender smsMessageSender;

  private CategoryOptionCombo coc;

  private OrganisationUnit orgUnit;

  private Program program;

  private ProgramStage programStage;

  private User user;

  private TrackedEntityType trackedEntityType;

  private Event event;

  @BeforeEach
  void setUp() {
    coc = categoryService.getDefaultCategoryOptionCombo();

    orgUnit = createOrganisationUnit('A');

    user = createUserWithAuth("tester", Authorities.toStringArray(Authorities.F_MOBILE_SETTINGS));
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    user.setPhoneNumber("7654321");
    userService.updateUser(user);

    orgUnit.getSharing().setOwner(user);
    manager.save(orgUnit, false);

    trackedEntityType = trackedEntityTypeAccessible();

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(user);
    program.getSharing().addUserAccess(fullAccess(user));
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    DataElement de = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    de.getSharing().setOwner(user);
    manager.save(de, false);

    programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(user);
    programStage.getSharing().addUserAccess(fullAccess(user));
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(programStage, de, 1, false);
    programStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElement));
    manager.save(programStage, false);

    EventDataValue dv = new EventDataValue();
    dv.setDataElement(de.getUid());
    dv.setStoredBy("user");
    dv.setValue("value");

    event = event(enrollment(trackedEntity()));
  }

  @AfterEach
  void afterEach() {
    smsMessageSender.clearMessages();
  }

  @Test
  void shouldDeleteEvent() throws SmsCompressionException {
    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 1;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(event.getUid());

    SmsSubmissionWriter smsSubmissionWriter = new SmsSubmissionWriter(new SmsMetadata());
    byte[] compressedText = smsSubmissionWriter.compress(submission, SMS_COMPRESSION_VERSION);
    String text = Base64.getEncoder().encodeToString(compressedText);
    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
                    """
        {
        "text": "%s",
        "originator": "%s"
        }
        """,
                    text, originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    String expectedText = submissionId + ":" + SmsResponse.SUCCESS;
    OutboundMessage expectedMessage = new OutboundMessage(null, expectedText, Set.of(originator));
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> assertNotNull(sms.getReceivedDate()),
        () -> assertEquals(sms.getReceivedDate(), sms.getSentDate()),
        () -> assertEquals("default", sms.getGatewayId()),
        () ->
            assertThrows(
                NotFoundException.class, () -> eventService.getEvent(UID.of(event.getUid()))),
        () -> assertContainsOnly(List.of(expectedMessage), smsMessageSender.getAllMessages()));
  }

  @Test
  void shouldDeleteEventViaRequestParameters() throws SmsCompressionException {
    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 1;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(event.getUid());

    SmsSubmissionWriter smsSubmissionWriter = new SmsSubmissionWriter(new SmsMetadata());
    byte[] compressedText = smsSubmissionWriter.compress(submission, SMS_COMPRESSION_VERSION);
    String text = Base64.getEncoder().encodeToString(compressedText);
    String originator = user.getPhoneNumber();
    LocalDateTime receivedTime = LocalDateTime.of(2024, 9, 2, 10, 15, 30);

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound?message={message}&originator={originator}&receivedTime={receivedTime}",
                text,
                originator,
                receivedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    String expectedText = submissionId + ":" + SmsResponse.SUCCESS;
    OutboundMessage expectedMessage = new OutboundMessage(null, expectedText, Set.of(originator));
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> assertNotNull(sms.getReceivedDate()),
        () -> assertNotEquals(sms.getReceivedDate(), sms.getSentDate()),
        () ->
            assertEquals(
                receivedTime,
                sms.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
        () -> assertEquals("Unknown", sms.getGatewayId()),
        () ->
            assertThrows(
                NotFoundException.class, () -> eventService.getEvent(UID.of(event.getUid()))),
        () -> assertContainsOnly(List.of(expectedMessage), smsMessageSender.getAllMessages()));
  }

  @Test
  void shouldFailDeletingNonExistingEvent() throws SmsCompressionException {
    UID uid = UID.of(CodeGenerator.generateUid());
    assertThrows(NotFoundException.class, () -> eventService.getEvent(uid));

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 2;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setEvent(uid.getValue());

    SmsSubmissionWriter smsSubmissionWriter = new SmsSubmissionWriter(new SmsMetadata());
    byte[] compressedText = smsSubmissionWriter.compress(submission, SMS_COMPRESSION_VERSION);
    String text = Base64.getEncoder().encodeToString(compressedText);
    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
                    """
    {
    "text": "%s",
    "originator": "%s"
    }
    """,
                    text, originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(response);
    String expectedText = submissionId + ":" + SmsResponse.INVALID_EVENT.set(uid.getValue());
    OutboundMessage expectedMessage = new OutboundMessage(null, expectedText, Set.of(originator));
    assertAll(
        () -> assertEquals(SmsMessageStatus.FAILED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> assertContainsOnly(List.of(expectedMessage), smsMessageSender.getAllMessages()));
  }

  private IncomingSms getSms(JsonWebMessage response) {
    assertStartsWith("Received SMS: ", response.getMessage());

    String smsId = response.getMessage().replaceFirst("^Received SMS: ", "");
    IncomingSms sms = incomingSmsService.get(Long.parseLong(smsId));
    assertNotNull(sms, "failed to find SMS in DB with id " + smsId);
    return sms;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().setOwner(user);
    type.getSharing().addUserAccess(fullAccess(user));
    manager.save(type, false);
    return type;
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(OrganisationUnit orgUnit) {
    return trackedEntity(orgUnit, trackedEntityType);
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(user);
    return te;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    manager.save(enrollment);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit(), coc);
    event.setAutoFields();
    manager.save(event);
    return event;
  }

  private TrackedEntityType trackedEntityType(char uniqueChar) {
    TrackedEntityType type = createTrackedEntityType(uniqueChar);
    type.getSharing().setOwner(user);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private UserAccess fullAccess(User user) {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }
}
