/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static java.lang.String.format;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.assertEqualUids;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.assertSmsResponse;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.encodeSms;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.getSms;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.DeleteSmsSubmission;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SimpleEventSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.TrackerEventSmsSubmission;
import org.hisp.dhis.test.message.DefaultFakeMessageSender;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.function.Executable;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Tests tracker compression and command based SMS
 *
 * <ul>
 *   <li>to delete an event via a compressed SMS of type {@link DeleteSmsSubmission} implemented by
 *       {@link org.hisp.dhis.tracker.imports.sms.DeleteEventSMSListener}
 *   <li>to create an event in a tracker program via a compressed SMS of type {@link
 *       TrackerEventSmsSubmission} implemented by {@link
 *       org.hisp.dhis.tracker.imports.sms.TrackerEventSMSListener}
 *   <li>to create an event in an event program via a compressed SMS of type {@link
 *       SimpleEventSmsSubmission} implemented by {@link
 *       org.hisp.dhis.tracker.imports.sms.SimpleEventSMSListener}
 *   <li>to enroll a tracked entity and create an event in a tracker program via an SMS command of
 *       type {@code ParserType.PROGRAM_STAGE_DATAENTRY_PARSER} implemented by {@link
 *       org.hisp.dhis.tracker.imports.sms.ProgramStageDataEntrySMSListener}
 *   <li>to create an event in an event program via an SMS command of type {@code
 *       ParserType.EVENT_REGISTRATION_PARSER} implemented by {@link
 *       org.hisp.dhis.tracker.imports.sms.SingleEventListener}
 * </ul>
 *
 * It also tests parts of {@link org.hisp.dhis.webapi.controller.sms.SmsInboundController} and other
 * SMS classes in the SMS class hierarchy.
 *
 * <p>This test is non-transactional so we can test the code fetching tracked entities in {@link
 * org.hisp.dhis.tracker.imports.sms.ProgramStageDataEntrySMSListener} which runs in another thread
 * and would not see the test setup otherwise.
 */
@TestInstance(Lifecycle.PER_CLASS)
class TrackerEventSMSTest extends PostgresControllerIntegrationTestBase {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackerEventService trackerEventService;

  @Autowired private SingleEventService singleEventService;

  @Autowired private IncomingSmsService incomingSmsService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired
  @Qualifier("smsMessageSender")
  private DefaultFakeMessageSender smsMessageSender;

  private CategoryOptionCombo coc;

  private OrganisationUnit orgUnit;

  private Program trackerProgram;
  private ProgramStage trackerProgramStage;

  // two users with different phone numbers are needed to test ProgramStageDataEntrySMSListener
  private User user1;
  private User user2;

  private TrackedEntityType trackedEntityType;
  private TrackedEntityAttribute teaA;

  private DataElement de;

  private Program eventProgram;
  private ProgramStage eventProgramStage;

  @BeforeAll
  void setUp() {
    smsMessageSender.clearMessages();

    coc = categoryService.getDefaultCategoryOptionCombo();

    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit, false);

    user1 =
        createUserWithAuth(
            "tester1", Authorities.toStringArray(Authorities.F_MOBILE_SETTINGS, Authorities.ALL));
    user1.addOrganisationUnit(orgUnit);
    user1.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    user1.setPhoneNumber("7654321");
    userService.updateUser(user1);

    user2 =
        createUserWithAuth(
            "tester2", Authorities.toStringArray(Authorities.F_MOBILE_SETTINGS, Authorities.ALL));
    user2.addOrganisationUnit(orgUnit);
    user2.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    user2.setPhoneNumber("1234567");
    userService.updateUser(user2);

    orgUnit.getSharing().setOwner(user1);
    manager.save(orgUnit, false);

    trackedEntityType = trackedEntityTypeAccessible();

    teaA = createTrackedEntityAttribute('A', ValueType.PHONE_NUMBER);
    teaA.setConfidential(false);
    teaA.getSharing().setOwner(user1);
    teaA.getSharing().addUserAccess(fullAccess(user1));
    manager.save(teaA, false);

    TrackedEntityTypeAttribute teat = new TrackedEntityTypeAttribute(trackedEntityType, teaA);
    manager.save(teat, false);
    trackedEntityType.getTrackedEntityTypeAttributes().add(teat);
    manager.save(trackedEntityType, false);

    trackerProgram = createProgram('A');
    trackerProgram.addOrganisationUnit(orgUnit);
    trackerProgram.getSharing().setOwner(user1);
    trackerProgram.getSharing().addUserAccess(fullAccess(user1));
    trackerProgram.setTrackedEntityType(trackedEntityType);
    trackerProgram.setProgramType(ProgramType.WITH_REGISTRATION);
    manager.save(trackerProgram, false);

    de = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    de.getSharing().setOwner(user1);
    manager.save(de, false);

    trackerProgramStage = createProgramStage('A', trackerProgram);
    trackerProgramStage.setFeatureType(FeatureType.POINT);
    trackerProgramStage.getSharing().setOwner(user1);
    trackerProgramStage.getSharing().addUserAccess(fullAccess(user1));
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(trackerProgramStage, de, 1, false);
    trackerProgramStage.setProgramStageDataElements(Set.of(programStageDataElementA));
    manager.save(trackerProgramStage, false);
    trackerProgram.getProgramStages().add(trackerProgramStage);
    manager.save(trackerProgram, false);

    eventProgram = createProgram('B');
    eventProgram.addOrganisationUnit(orgUnit);
    eventProgram.getSharing().setOwner(user1);
    eventProgram.getSharing().addUserAccess(fullAccess(user1));
    eventProgram.setTrackedEntityType(trackedEntityType);
    eventProgram.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    manager.save(eventProgram, false);

    eventProgramStage = createProgramStage('B', eventProgram);
    eventProgramStage.setFeatureType(FeatureType.POINT);
    eventProgramStage.getSharing().setOwner(user1);
    eventProgramStage.getSharing().addUserAccess(fullAccess(user1));
    ProgramStageDataElement programStageDataElementB =
        createProgramStageDataElement(eventProgramStage, de, 1, false);
    eventProgramStage.setProgramStageDataElements(Set.of(programStageDataElementB));
    manager.save(eventProgramStage, false);
    eventProgram.getProgramStages().add(eventProgramStage);
    manager.save(eventProgram, false);

    // create default enrollment for event program
    manager.save(createEnrollment(eventProgram, null, orgUnit));
  }

  @AfterEach
  void afterEach() {
    smsMessageSender.clearMessages();
  }

  @Test
  void shouldDeleteEvent() throws SmsCompressionException {
    Event event = event(enrollment(trackedEntity()));

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 1;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user1.getUid());
    submission.setEvent(event.getUid());

    String text = encodeSms(submission);
    String originator = user1.getPhoneNumber();

    switchContextToUser(user1);

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

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user1, sms.getCreatedBy()),
        () -> assertNotNull(sms.getReceivedDate()),
        () -> assertEquals(sms.getReceivedDate(), sms.getSentDate()),
        () -> assertEquals("default", sms.getGatewayId()),
        () ->
            assertSmsResponse(
                submissionId + ":" + SmsResponse.SUCCESS, originator, smsMessageSender));
    assertFalse(trackerEventService.findEvent(UID.of(event.getUid())).isPresent());
  }

  @Test
  void shouldDeleteEventViaRequestParameters() throws SmsCompressionException {
    Event event = event(enrollment(trackedEntity()));

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 2;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user1.getUid());
    submission.setEvent(event.getUid());

    String text = encodeSms(submission);
    String originator = user1.getPhoneNumber();
    LocalDateTime receivedTime = LocalDateTime.of(2024, 9, 2, 10, 15, 30);

    switchContextToUser(user1);

    JsonWebMessage response =
        POST(
                "/sms/inbound?message={message}&originator={originator}&receivedTime={receivedTime}",
                text,
                originator,
                receivedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user1, sms.getCreatedBy()),
        () -> assertNotNull(sms.getReceivedDate()),
        () -> assertNotEquals(sms.getReceivedDate(), sms.getSentDate()),
        () ->
            assertEquals(
                receivedTime,
                sms.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()),
        () -> assertEquals("Unknown", sms.getGatewayId()),
        () ->
            assertSmsResponse(
                submissionId + ":" + SmsResponse.SUCCESS, originator, smsMessageSender));
    assertFalse(trackerEventService.findEvent(UID.of(event.getUid())).isPresent());
  }

  @Test
  void shouldFailDeletingNonExistingEvent() throws SmsCompressionException {
    UID uid = UID.generate();

    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    int submissionId = 3;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user1.getUid());
    submission.setEvent(uid.getValue());

    String text = encodeSms(submission);
    String originator = user1.getPhoneNumber();

    switchContextToUser(user1);
    assertFalse(trackerEventService.findEvent(uid).isPresent());

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

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.FAILED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user1, sms.getCreatedBy()),
        () ->
            assertSmsResponse(
                submissionId + ":" + SmsResponse.INVALID_EVENT.set(uid.getValue()),
                originator,
                smsMessageSender));
  }

  @Test
  void shouldCreateEvent() throws SmsCompressionException, NotFoundException {
    Enrollment enrollment = enrollment(trackedEntity());

    TrackerEventSmsSubmission submission = new TrackerEventSmsSubmission();
    int submissionId = 4;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user1.getUid());
    String eventUid = CodeGenerator.generateUid();
    submission.setEvent(eventUid);
    submission.setOrgUnit(orgUnit.getUid());
    submission.setProgramStage(trackerProgramStage.getUid());
    submission.setEnrollment(enrollment.getUid());
    submission.setAttributeOptionCombo(coc.getUid());
    submission.setEventStatus(SmsEventStatus.COMPLETED);
    submission.setEventDate(DateUtils.getDate(2024, 9, 2, 10, 15));
    submission.setDueDate(DateUtils.getDate(2024, 9, 3, 16, 23));
    submission.setCoordinates(new GeoPoint(48.8575f, 2.3514f));

    String text = encodeSms(submission);
    String originator = user1.getPhoneNumber();

    switchContextToUser(user1);

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

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user1, sms.getCreatedBy()),
        () ->
            assertSmsResponse(
                submissionId + ":" + SmsResponse.SUCCESS, originator, smsMessageSender));
    assertTrue(trackerEventService.findEvent(UID.of(eventUid)).isPresent());
    Event actual = trackerEventService.getEvent(UID.of(eventUid));
    assertAll(
        "created event",
        () -> assertEquals(eventUid, actual.getUid()),
        () -> assertEqualUids(submission.getEnrollment(), actual.getEnrollment()),
        () -> assertEqualUids(submission.getOrgUnit(), actual.getOrganisationUnit()),
        () -> assertEqualUids(submission.getProgramStage(), actual.getProgramStage()),
        () ->
            assertEqualUids(submission.getAttributeOptionCombo(), actual.getAttributeOptionCombo()),
        () -> assertEquals(user1.getUsername(), actual.getStoredBy()),
        () -> assertEquals(submission.getEventDate(), actual.getOccurredDate()),
        () -> assertEquals(submission.getDueDate(), actual.getScheduledDate()),
        () -> assertEquals(EventStatus.COMPLETED, actual.getStatus()),
        () -> assertEquals(user1.getUsername(), actual.getCompletedBy()),
        () -> assertNotNull(actual.getCompletedDate()),
        () -> assertGeometry(submission.getCoordinates(), actual.getGeometry()));
  }

  @Test
  void shouldUpdateEvent() throws SmsCompressionException, NotFoundException {
    Enrollment enrollment = enrollment(trackedEntity());
    Event event = event(enrollment);

    TrackerEventSmsSubmission submission = new TrackerEventSmsSubmission();
    int submissionId = 5;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user1.getUid());
    submission.setEvent(event.getUid());
    submission.setOrgUnit(event.getOrganisationUnit().getUid());
    submission.setProgramStage(event.getProgramStage().getUid());
    submission.setEnrollment(enrollment.getUid());
    submission.setAttributeOptionCombo(event.getAttributeOptionCombo().getUid());
    submission.setEventStatus(SmsEventStatus.COMPLETED);
    submission.setEventDate(event.getOccurredDate());
    submission.setDueDate(event.getScheduledDate());
    // The coc has to be set so the sms-compression library can encode the data value. Not sure why
    // that is necessary though.
    submission.setValues(List.of(new SmsDataValue(coc.getUid(), de.getUid(), "hello")));

    String text = encodeSms(submission);
    String originator = user1.getPhoneNumber();

    switchContextToUser(user1);

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

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user1, sms.getCreatedBy()),
        () ->
            assertSmsResponse(
                submissionId + ":" + SmsResponse.SUCCESS, originator, smsMessageSender));
    assertTrue(trackerEventService.findEvent(UID.of(event)).isPresent());
    Event actual = trackerEventService.getEvent(UID.of(event.getUid()));
    assertAll(
        "updated event",
        () -> assertEqualUids(submission.getEnrollment(), actual.getEnrollment()),
        () -> assertEqualUids(submission.getOrgUnit(), actual.getOrganisationUnit()),
        () -> assertEqualUids(submission.getProgramStage(), actual.getProgramStage()),
        () ->
            assertEqualUids(submission.getAttributeOptionCombo(), actual.getAttributeOptionCombo()),
        () -> assertNull(actual.getStoredBy()),
        () -> assertEquals(event.getOccurredDate(), actual.getOccurredDate()),
        () -> assertEquals(event.getScheduledDate(), actual.getScheduledDate()),
        () -> assertEquals(EventStatus.COMPLETED, actual.getStatus()),
        () -> assertEquals(user1.getUsername(), actual.getCompletedBy()),
        () -> assertNotNull(actual.getCompletedDate()),
        () -> {
          EventDataValue expected = new EventDataValue(de.getUid(), "hello");
          expected.setStoredBy(user1.getUsername());
          assertDataValues(Set.of(expected), actual.getEventDataValues());
        },
        () -> assertNull(actual.getGeometry()));
  }

  @Test
  void shouldCreateEventInEventProgram() throws SmsCompressionException, NotFoundException {
    SimpleEventSmsSubmission submission = new SimpleEventSmsSubmission();
    int submissionId = 6;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user1.getUid());
    submission.setOrgUnit(orgUnit.getUid());
    submission.setEventProgram(eventProgram.getUid());
    submission.setEventStatus(SmsEventStatus.ACTIVE);
    submission.setAttributeOptionCombo(coc.getUid());
    String eventUid = CodeGenerator.generateUid();
    submission.setEvent(eventUid);
    submission.setEventDate(DateUtils.getDate(2024, 9, 2, 10, 15));
    submission.setDueDate(DateUtils.getDate(2024, 9, 3, 16, 23));
    submission.setCoordinates(new GeoPoint(48.8575f, 2.3514f));
    // The coc has to be set so the sms-compression library can encode the data value. Not sure why
    // that is necessary though.
    submission.setValues(List.of(new SmsDataValue(coc.getUid(), de.getUid(), "hello")));

    String text = encodeSms(submission);
    String originator = user1.getPhoneNumber();

    switchContextToUser(user1);

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

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user1, sms.getCreatedBy()),
        () ->
            assertSmsResponse(
                submissionId + ":" + SmsResponse.SUCCESS, originator, smsMessageSender));
    assertTrue(singleEventService.findEvent(UID.of(eventUid)).isPresent());
    Event actual = singleEventService.getEvent(UID.of(eventUid));
    assertAll(
        "created event",
        () -> assertEquals(eventUid, actual.getUid()),
        () -> assertEqualUids(submission.getEventProgram(), actual.getEnrollment().getProgram()),
        () -> assertEqualUids(submission.getOrgUnit(), actual.getOrganisationUnit()),
        () ->
            assertEqualUids(submission.getAttributeOptionCombo(), actual.getAttributeOptionCombo()),
        () -> assertEquals(user1.getUsername(), actual.getStoredBy()),
        () -> assertEquals(submission.getEventDate(), actual.getOccurredDate()),
        () -> assertEquals(EventStatus.ACTIVE, actual.getStatus()),
        () -> assertEquals(user1.getUsername(), actual.getStoredBy()),
        () -> assertNull(actual.getCompletedDate()),
        () -> assertGeometry(submission.getCoordinates(), actual.getGeometry()),
        () -> {
          EventDataValue expected = new EventDataValue(de.getUid(), "hello");
          expected.setStoredBy(user1.getUsername());
          assertDataValues(Set.of(expected), actual.getEventDataValues());
        });
  }

  @Test
  void shouldCreateEventInEventProgramViaEventRegistrationParserCommand()
      throws ForbiddenException, BadRequestException {
    SMSCommand command = new SMSCommand();
    command.setName("visit");
    command.setParserType(ParserType.EVENT_REGISTRATION_PARSER);
    command.setProgram(eventProgram);
    command.setProgramStage(eventProgramStage);
    SMSCode code1 = new SMSCode();
    code1.setCode("a");
    code1.setDataElement(de);
    command.setCodes(Set.of(code1));
    manager.save(command);

    String originator = user1.getPhoneNumber();

    switchContextToUser(user1);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
"""
{
"text": "visit a=hello",
"originator": "%s"
}
""",
                    originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms smsResponse = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, smsResponse.getStatus()),
        () -> assertTrue(smsResponse.isParsed()),
        () -> assertEquals(originator, smsResponse.getOriginator()),
        () -> assertEquals(user1, smsResponse.getCreatedBy()),
        () ->
            assertSmsResponse(
                "Command has been processed successfully", originator, smsMessageSender));

    List<Event> events =
        singleEventService.findEvents(
            SingleEventOperationParams.builder().program(eventProgram).build());
    assertHasSize(1, events);
    Event actual = events.get(0);
    assertAll(
        "created event",
        () -> assertEqualUids(orgUnit, actual.getOrganisationUnit()),
        () -> assertEqualUids(eventProgram, actual.getEnrollment().getProgram()),
        () -> assertEqualUids(eventProgramStage, actual.getProgramStage()),
        () -> assertEquals(user1.getUsername(), actual.getStoredBy()),
        () -> assertEquals(EventStatus.ACTIVE, actual.getStatus()),
        () -> assertEquals(user1.getUsername(), actual.getStoredBy()),
        () -> {
          EventDataValue expected = new EventDataValue(de.getUid(), "hello");
          expected.setStoredBy(user1.getUsername());
          assertDataValues(Set.of(expected), actual.getEventDataValues());
        });
  }

  @Test
  void shouldCreateEventAndEnrollmentInTrackerProgramViaProgramStageDataEntryCommand()
      throws ForbiddenException, BadRequestException {
    SMSCommand command = new SMSCommand();
    command.setName("birth");
    command.setParserType(ParserType.PROGRAM_STAGE_DATAENTRY_PARSER);
    command.setProgram(trackerProgram);
    command.setProgramStage(trackerProgramStage);
    SMSCode code1 = new SMSCode();
    code1.setCode("a");
    code1.setDataElement(de);
    command.setCodes(Set.of(code1));
    manager.save(command);

    String originator = user1.getPhoneNumber();
    TrackedEntity trackedEntity = trackedEntity(originator);

    switchContextToUser(user1);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
"""
{
"text": "birth a=hello",
"originator": "%s"
}
""",
                    originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms smsResponse = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, smsResponse.getStatus()),
        () -> assertTrue(smsResponse.isParsed()),
        () -> assertEquals(originator, smsResponse.getOriginator()),
        () -> assertEquals(user1, smsResponse.getCreatedBy()),
        () ->
            assertSmsResponse(
                "Command has been processed successfully", originator, smsMessageSender));

    List<Enrollment> enrollments =
        enrollmentService.findEnrollments(
            EnrollmentOperationParams.builder()
                .trackedEntity(trackedEntity)
                .program(trackerProgram)
                .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
                .build());
    assertHasSize(1, enrollments);
    Enrollment actualEnrollment = enrollments.get(0);
    assertAll(
        "created enrollment",
        () -> assertEqualUids(trackedEntity, actualEnrollment.getTrackedEntity()),
        () -> assertEqualUids(orgUnit, actualEnrollment.getOrganisationUnit()),
        () -> assertEqualUids(trackerProgram, actualEnrollment.getProgram()),
        () -> assertEquals(EnrollmentStatus.ACTIVE, actualEnrollment.getStatus()));

    List<Event> events =
        trackerEventService.findEvents(
            TrackerEventOperationParams.builder()
                .trackedEntity(trackedEntity)
                .program(trackerProgram)
                .build());
    assertHasSize(1, events);
    Event actualEvent = events.get(0);
    assertAll(
        "created event",
        () -> assertEqualUids(orgUnit, actualEvent.getOrganisationUnit()),
        () -> assertEqualUids(trackerProgram, actualEvent.getEnrollment().getProgram()),
        () -> assertEqualUids(trackerProgramStage, actualEvent.getProgramStage()),
        () -> assertEqualUids(actualEnrollment, actualEvent.getEnrollment()),
        () -> assertEquals(user1.getUsername(), actualEvent.getStoredBy()),
        () -> assertEquals(EventStatus.ACTIVE, actualEvent.getStatus()),
        () -> assertEquals(user1.getUsername(), actualEvent.getStoredBy()),
        () -> {
          EventDataValue expected = new EventDataValue(de.getUid(), "hello");
          expected.setStoredBy(user1.getUsername());
          assertDataValues(Set.of(expected), actualEvent.getEventDataValues());
        });
  }

  @Test
  void shouldCreateEventInExistingEnrollmentInTrackerProgramViaProgramStageDataEntryCommand()
      throws ForbiddenException, BadRequestException {
    SMSCommand command = new SMSCommand();
    command.setName("birth");
    command.setParserType(ParserType.PROGRAM_STAGE_DATAENTRY_PARSER);
    command.setProgram(trackerProgram);
    command.setProgramStage(trackerProgramStage);
    SMSCode code1 = new SMSCode();
    code1.setCode("a");
    code1.setDataElement(de);
    command.setCodes(Set.of(code1));
    manager.save(command);

    String originator = user2.getPhoneNumber();
    TrackedEntity trackedEntity = trackedEntity(originator);
    Enrollment enrollment = enrollment(trackedEntity);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        enrollment.getTrackedEntity(),
        enrollment.getProgram(),
        enrollment.getTrackedEntity().getOrganisationUnit());

    switchContextToUser(user2);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
"""
{
"text": "birth a=hello",
"originator": "%s"
}
""",
                    originator))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    IncomingSms smsResponse = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, smsResponse.getStatus()),
        () -> assertTrue(smsResponse.isParsed()),
        () -> assertEquals(originator, smsResponse.getOriginator()),
        () -> assertEquals(user2, smsResponse.getCreatedBy()),
        () ->
            assertSmsResponse(
                "Command has been processed successfully", originator, smsMessageSender));

    List<Event> events =
        trackerEventService.findEvents(
            TrackerEventOperationParams.builder()
                .trackedEntity(trackedEntity)
                .program(trackerProgram)
                .build());
    assertHasSize(1, events);
    Event actual = events.get(0);
    assertAll(
        "created event",
        () -> assertEqualUids(orgUnit, actual.getOrganisationUnit()),
        () -> assertEqualUids(trackerProgram, actual.getEnrollment().getProgram()),
        () -> assertEqualUids(trackerProgramStage, actual.getProgramStage()),
        () -> assertEqualUids(enrollment, actual.getEnrollment()),
        () -> assertEquals(user2.getUsername(), actual.getStoredBy()),
        () -> assertEquals(EventStatus.ACTIVE, actual.getStatus()),
        () -> assertEquals(user2.getUsername(), actual.getStoredBy()),
        () -> {
          EventDataValue expected = new EventDataValue(de.getUid(), "hello");
          expected.setStoredBy(user2.getUsername());
          assertDataValues(Set.of(expected), actual.getEventDataValues());
        });
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().setOwner(user1);
    type.getSharing().addUserAccess(fullAccess(user1));
    manager.save(type, false);
    return type;
  }

  private TrackedEntity trackedEntity(String phoneNumber) {
    TrackedEntity trackedEntity = trackedEntity();
    TrackedEntityAttributeValue teavA = createTrackedEntityAttributeValue('A', trackedEntity, teaA);
    teavA.setValue(phoneNumber);
    attributeValueService.addTrackedEntityAttributeValue(teavA);
    trackedEntity.getTrackedEntityAttributeValues().add(teavA);
    manager.save(trackedEntity, false);
    return trackedEntity;
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
    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(user1);
    return te;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(trackerProgram, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    manager.save(enrollment);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, trackerProgramStage, enrollment.getOrganisationUnit(), coc);
    event.setOccurredDate(new Date());
    event.setAutoFields();
    manager.save(event);
    return event;
  }

  private TrackedEntityType trackedEntityType(char uniqueChar) {
    TrackedEntityType type = createTrackedEntityType(uniqueChar);
    type.getSharing().setOwner(user1);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private UserAccess fullAccess(User user) {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private static void assertGeometry(GeoPoint expected, Geometry actual) {
    assertAll(
        "assert geometry",
        () -> assertEquals("Point", actual.getGeometryType()),
        () ->
            assertEquals(
                expected.getLongitude(),
                actual.getCoordinate().x,
                0.000000000000001d,
                "mismatch in longitude"),
        () ->
            assertEquals(
                expected.getLatitude(),
                actual.getCoordinate().y,
                0.000000000000001d,
                "mismatch in latitude"));
  }

  private static void assertDataValues(Set<EventDataValue> expected, Set<EventDataValue> actual) {
    // The current EventDataValues.equals implementation does not take the value/storedBy into
    // account
    // it does check the data element. So we first assert we have a data value for every data
    // element we expect.
    // We then assert on fields that are not covered by the equals implementation.
    assertContainsOnly(expected, actual);
    assertAll(
        "assert data values", expected.stream().map(e -> assertDataValue(e, actual)).toList());
  }

  private static Executable assertDataValue(EventDataValue expected, Set<EventDataValue> actuals) {
    return () -> {
      EventDataValue actual =
          actuals.stream()
              .filter(dv -> dv.getDataElement().equals(expected.getDataElement()))
              .findFirst()
              .get();
      assertAll(
          "assert data value " + expected.getDataElement(),
          () -> assertEquals(expected.getDataElement(), actual.getDataElement()),
          () -> assertEquals(expected.getValue(), actual.getValue()),
          () -> assertEquals(expected.getStoredBy(), actual.getStoredBy()));
    };
  }
}
