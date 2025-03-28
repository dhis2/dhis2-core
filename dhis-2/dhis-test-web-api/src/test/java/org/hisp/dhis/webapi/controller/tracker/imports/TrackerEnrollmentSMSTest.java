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
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.assertEqualUids;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.assertSmsResponse;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.encodeSms;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.getSms;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
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
import org.hisp.dhis.smscompression.SmsConsts.SmsEnrollmentStatus;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.EnrollmentSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsAttributeValue;
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
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Tests tracker compression and command based SMS
 *
 * <ul>
 *   <li>to enroll an existing or new tracked entity via a compressed SMS of type {@link
 *       org.hisp.dhis.smscompression.models.EnrollmentSmsSubmission} implemented by {@link
 *       org.hisp.dhis.tracker.imports.sms.EnrollmentSMSListener}
 *   <li>to create and enroll a tracked entity via an SMS command of type {@code
 *       ParserType.TRACKED_ENTITY_REGISTRATION_PARSER} implemented by {@link
 *       org.hisp.dhis.tracker.imports.sms.TrackedEntityRegistrationSMSListener}
 * </ul>
 */
class TrackerEnrollmentSMSTest extends PostgresControllerIntegrationTestBase {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private IncomingSmsService incomingSmsService;

  @Autowired TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired
  @Qualifier("smsMessageSender")
  private DefaultFakeMessageSender messageSender;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  private OrganisationUnit orgUnit;

  private Program trackerProgram;
  private ProgramStage trackerProgramStage;

  private User user;

  private TrackedEntityType trackedEntityType;
  private TrackedEntityAttribute teaA;
  private TrackedEntityAttribute teaB;
  private TrackedEntityAttribute teaC;

  @BeforeEach
  void setUp() {
    messageSender.clearMessages();

    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit, false);

    user =
        createUserWithAuth(
            "tester", Authorities.toStringArray(Authorities.F_MOBILE_SETTINGS, Authorities.ALL));
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    user.setPhoneNumber("7654321");
    userService.updateUser(user);

    orgUnit.getSharing().setOwner(user);
    manager.save(orgUnit, false);

    trackedEntityType = trackedEntityTypeAccessible();

    teaA = createTrackedEntityAttribute('A', ValueType.TEXT);
    teaA.setConfidential(false);
    teaA.getSharing().setOwner(user);
    teaA.getSharing().addUserAccess(fullAccess(user));
    manager.save(teaA, false);

    // this TEA will be a tracked entity type attribute and also a program attribute
    teaB = createTrackedEntityAttribute('B', ValueType.TEXT);
    teaB.getSharing().setOwner(user);
    teaB.getSharing().addUserAccess(fullAccess(user));
    teaB.setConfidential(false);
    manager.save(teaB, false);

    // this TEA will only be a program attribute
    teaC = createTrackedEntityAttribute('C', ValueType.TEXT);
    teaC.getSharing().setOwner(user);
    teaC.getSharing().addUserAccess(fullAccess(user));
    teaC.setConfidential(false);
    manager.save(teaC, false);

    trackedEntityType
        .getTrackedEntityTypeAttributes()
        .addAll(
            List.of(
                new TrackedEntityTypeAttribute(trackedEntityType, teaA),
                new TrackedEntityTypeAttribute(trackedEntityType, teaB)));
    manager.save(trackedEntityType, false);

    trackerProgram = createProgram('A');
    trackerProgram.addOrganisationUnit(orgUnit);
    trackerProgram.getSharing().setOwner(user);
    trackerProgram.getSharing().addUserAccess(fullAccess(user));
    trackerProgram.setTrackedEntityType(trackedEntityType);
    trackerProgram.setProgramType(ProgramType.WITH_REGISTRATION);
    trackerProgram.setProgramAttributes(
        List.of(
            createProgramTrackedEntityAttribute(trackerProgram, teaB),
            createProgramTrackedEntityAttribute(trackerProgram, teaC)));
    manager.save(trackerProgram, false);

    DataElement de = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    de.getSharing().setOwner(user);
    manager.save(de, false);

    trackerProgramStage = createProgramStage('A', trackerProgram);
    trackerProgramStage.setFeatureType(FeatureType.POINT);
    trackerProgramStage.getSharing().setOwner(user);
    trackerProgramStage.getSharing().addUserAccess(fullAccess(user));
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(trackerProgramStage, de, 1, false);
    trackerProgramStage.setProgramStageDataElements(Set.of(programStageDataElement));
    manager.save(trackerProgramStage, false);
  }

  @AfterEach
  void afterEach() {
    messageSender.clearMessages();
  }

  @Test
  void shouldCreateTrackedEntityAndEnrollIt()
      throws SmsCompressionException, ForbiddenException, NotFoundException {
    EnrollmentSmsSubmission submission = new EnrollmentSmsSubmission();
    int submissionId = 1;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setOrgUnit(orgUnit.getUid());
    submission.setTrackerProgram(trackerProgram.getUid());
    submission.setTrackedEntityInstance(CodeGenerator.generateUid());
    submission.setTrackedEntityType(trackedEntityType.getUid());
    UID enrollmentUid = UID.generate();
    submission.setEnrollment(enrollmentUid.getValue());
    submission.setEnrollmentDate(DateUtils.getDate(2024, 9, 2, 10, 15));
    submission.setIncidentDate(DateUtils.getDate(2024, 9, 3, 16, 23));
    submission.setEnrollmentStatus(SmsEnrollmentStatus.COMPLETED);
    submission.setValues(
        List.of(
            new SmsAttributeValue(teaA.getUid(), "TrackedEntityTypeAttributeValue"),
            new SmsAttributeValue(teaC.getUid(), "ProgramAttributeValue")));

    String text = encodeSms(submission);
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

    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () ->
            assertSmsResponse(submissionId + ":" + SmsResponse.SUCCESS, originator, messageSender));
    assertTrue(enrollmentService.findEnrollment(enrollmentUid).isPresent());
    Enrollment actual = enrollmentService.getEnrollment(enrollmentUid);
    assertAll(
        "created enrollment",
        () -> assertEquals(enrollmentUid.getValue(), actual.getUid()),
        () -> assertEqualUids(submission.getTrackedEntityInstance(), actual.getTrackedEntity()));
    assertDoesNotThrow(
        () ->
            trackedEntityService.getTrackedEntity(
                UID.of(submission.getTrackedEntityInstance().getUid()),
                UID.of(submission.getTrackerProgram().getUid()),
                TrackedEntityParams.FALSE));
    TrackedEntity actualTe =
        trackedEntityService.getTrackedEntity(
            UID.of(submission.getTrackedEntityInstance().getUid()),
            UID.of(submission.getTrackerProgram().getUid()),
            TrackedEntityParams.FALSE.withIncludeAttributes(true));
    assertAll(
        "created tracked entity with tracked entity attribute values",
        () -> assertEqualUids(submission.getTrackedEntityInstance(), actualTe),
        () -> {
          Map<String, String> actualTeav =
              actualTe.getTrackedEntityAttributeValues().stream()
                  .collect(
                      Collectors.toMap(
                          teav -> teav.getAttribute().getUid(),
                          TrackedEntityAttributeValue::getValue));
          assertEquals(
              Map.of(
                  teaA.getUid(),
                  "TrackedEntityTypeAttributeValue",
                  teaC.getUid(),
                  "ProgramAttributeValue"),
              actualTeav);
        });
  }

  @Test
  void shouldEnrollExistingTrackedEntityAndAddUpdateAndDeleteAttributes()
      throws SmsCompressionException, ForbiddenException, NotFoundException {
    TrackedEntity trackedEntity = trackedEntity();
    // add two tracked entity type value to the TE (one will be updated, the other deleted)
    TrackedEntityAttributeValue teavA = createTrackedEntityAttributeValue('A', trackedEntity, teaA);
    attributeValueService.addTrackedEntityAttributeValue(teavA);
    trackedEntity.getTrackedEntityAttributeValues().add(teavA);

    TrackedEntityAttributeValue teavB = createTrackedEntityAttributeValue('B', trackedEntity, teaB);
    attributeValueService.addTrackedEntityAttributeValue(teavB);
    trackedEntity.getTrackedEntityAttributeValues().add(teavB);

    manager.save(trackedEntity, false);
    Enrollment enrollment = enrollment(trackedEntity);

    switchContextToUser(user);

    assertEquals(
        EnrollmentStatus.ACTIVE,
        enrollment.getStatus(),
        "enrollment status should be updated from active to completed");

    EnrollmentSmsSubmission submission = new EnrollmentSmsSubmission();
    int submissionId = 2;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    submission.setOrgUnit(orgUnit.getUid());
    submission.setTrackerProgram(trackerProgram.getUid());
    submission.setTrackedEntityInstance(trackedEntity.getUid());
    submission.setTrackedEntityType(trackedEntityType.getUid());
    submission.setEnrollment(enrollment.getUid());
    submission.setEnrollmentDate(enrollment.getEnrollmentDate());
    submission.setIncidentDate(enrollment.getOccurredDate());
    submission.setEnrollmentStatus(SmsEnrollmentStatus.COMPLETED);
    submission.setValues(
        List.of(
            new SmsAttributeValue(teaA.getUid(), "AttributeAUpdated"),
            new SmsAttributeValue(teaC.getUid(), "AttributeCAdded")));

    String text = encodeSms(submission);
    String originator = user.getPhoneNumber();

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

    manager.clear();
    IncomingSms sms = getSms(incomingSmsService, response);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () ->
            assertSmsResponse(submissionId + ":" + SmsResponse.SUCCESS, originator, messageSender));
    Enrollment actual =
        enrollmentService.getEnrollment(
            UID.of(enrollment), EnrollmentParams.FALSE.withIncludeAttributes(true));
    assertAll(
        "update enrollment and program attributes",
        () -> assertEqualUids(submission.getTrackedEntityInstance(), actual.getTrackedEntity()));
    assertDoesNotThrow(
        () ->
            trackedEntityService.getTrackedEntity(
                UID.of(submission.getTrackedEntityInstance().getUid()),
                UID.of(submission.getTrackerProgram().getUid()),
                TrackedEntityParams.FALSE));
    TrackedEntity actualTe =
        trackedEntityService.getTrackedEntity(
            UID.of(submission.getTrackedEntityInstance().getUid()),
            UID.of(submission.getTrackerProgram().getUid()),
            TrackedEntityParams.FALSE.withIncludeAttributes(true));
    assertAll(
        "update tracked entity with tracked entity attribute values",
        () -> assertEqualUids(submission.getTrackedEntityInstance(), actualTe),
        () -> {
          Map<String, String> actualTeav =
              actualTe.getTrackedEntityAttributeValues().stream()
                  .collect(
                      Collectors.toMap(
                          teav -> teav.getAttribute().getUid(),
                          TrackedEntityAttributeValue::getValue));
          assertEquals(
              Map.of(teaA.getUid(), "AttributeAUpdated", teaC.getUid(), "AttributeCAdded"),
              actualTeav);
        });
  }

  @Test
  void shouldCreateTrackedEntityAndEnrollItViaTrackedEntityRegistrationParserCommand()
      throws ForbiddenException, NotFoundException, BadRequestException {
    SMSCommand command = new SMSCommand();
    command.setName("register");
    command.setParserType(ParserType.TRACKED_ENTITY_REGISTRATION_PARSER);
    command.setProgram(trackerProgram);
    command.setProgramStage(trackerProgramStage);
    SMSCode code1 = new SMSCode();
    code1.setCode("a");
    code1.setTrackedEntityAttribute(teaA);
    SMSCode code2 = new SMSCode();
    code2.setCode("c");
    code2.setTrackedEntityAttribute(teaC);
    command.setCodes(Set.of(code1, code2));
    manager.save(command);

    String originator = user.getPhoneNumber();

    switchContextToUser(user);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                format(
"""
{
"text": "register a=hello|c=there|x=codeIsNotFoundOnCommand",
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
        () -> assertEquals(user, smsResponse.getCreatedBy()),
        () ->
            assertSmsResponse(
                "Command has been processed successfully", originator, messageSender));

    List<Enrollment> enrollments =
        enrollmentService.findEnrollments(
            EnrollmentOperationParams.builder()
                .program(trackerProgram)
                .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
                .build());
    assertHasSize(1, enrollments);
    Enrollment actualEnrollment = enrollments.get(0);
    assertAll(
        "created enrollment",
        () -> assertEqualUids(orgUnit, actualEnrollment.getOrganisationUnit()),
        () -> assertEqualUids(trackerProgram, actualEnrollment.getProgram()),
        () -> assertEquals(EnrollmentStatus.ACTIVE, actualEnrollment.getStatus()));

    TrackedEntity trackedEntity = actualEnrollment.getTrackedEntity();
    assertNotNull(trackedEntity);
    assertDoesNotThrow(
        () ->
            trackedEntityService.getTrackedEntity(
                UID.of(trackedEntity), UID.of(trackerProgram), TrackedEntityParams.FALSE));
    TrackedEntity actualTe =
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntity),
            UID.of(trackerProgram),
            TrackedEntityParams.FALSE.withIncludeAttributes(true));
    assertAll(
        "created tracked entity with tracked entity attribute values",
        () -> {
          Map<String, String> actualTeav =
              actualTe.getTrackedEntityAttributeValues().stream()
                  .collect(
                      Collectors.toMap(
                          teav -> teav.getAttribute().getUid(),
                          TrackedEntityAttributeValue::getValue));
          assertEquals(Map.of(teaA.getUid(), "hello", teaC.getUid(), "there"), actualTeav);
        });
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().setOwner(user);
    type.getSharing().addUserAccess(fullAccess(user));
    manager.save(type, false);
    return type;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(trackerProgram, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    manager.save(enrollment);
    te.getEnrollments().add(enrollment);
    manager.save(te);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(te, trackerProgram, orgUnit);
    return enrollment;
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
    te.getSharing().setOwner(user);
    return te;
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
