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
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.assertSmsResponse;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.encodeSms;
import static org.hisp.dhis.webapi.controller.tracker.imports.SmsTestUtils.getSms;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.RelationshipSmsSubmission;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.relationship.RelationshipFields;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests tracker SMS to create a relationship implemented via {@link
 * org.hisp.dhis.tracker.imports.sms.RelationshipSMSListener}.
 */
@Transactional
class TrackerCreateRelationshipSMSTest extends PostgresControllerIntegrationTestBase {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CategoryService categoryService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private IncomingSmsService incomingSmsService;

  @Autowired private MessageSender smsMessageSender;

  private CategoryOptionCombo coc;

  private OrganisationUnit orgUnit;

  private Program program;

  private ProgramStage programStage;

  private User user;

  private TrackedEntityType trackedEntityType;

  private TrackerEvent event1;
  private TrackerEvent event2;
  private RelationshipType relType;

  @BeforeEach
  void setUp() {
    smsMessageSender.clearMessages();

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
    programStage.setProgramStageDataElements(Set.of(programStageDataElement));
    manager.save(programStage, false);

    event1 = event(enrollment(trackedEntity()));
    event2 = event(enrollment(trackedEntity()));

    relType = relationshipType();
  }

  @AfterEach
  void afterEach() {
    smsMessageSender.clearMessages();
  }

  @Test
  void shouldCreateRelationship() throws SmsCompressionException {
    RelationshipSmsSubmission submission = new RelationshipSmsSubmission();
    int submissionId = 1;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    String relationshipUid = CodeGenerator.generateUid();
    submission.setRelationship(relationshipUid);
    submission.setRelationshipType(relType.getUid());
    submission.setFrom(event1.getUid());
    submission.setTo(event2.getUid());

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
    assertSmsResponse(submissionId + ":" + SmsResponse.SUCCESS, originator, smsMessageSender);
    assertAll(
        () -> assertEquals(SmsMessageStatus.PROCESSED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()),
        () -> {
          Relationship relationship =
              relationshipService.getRelationship(
                  UID.of(relationshipUid), RelationshipFields.all());
          assertAll(
              () -> assertEquals(relationshipUid, relationship.getUid()),
              () -> assertEquals(event1, relationship.getFrom().getEvent()),
              () -> assertEquals(event2, relationship.getTo().getEvent()));
        });
  }

  @Test
  void shouldFailCreatingRelationshipIfUserHasNoDataWriteAccessToRelationshipType()
      throws SmsCompressionException {
    relType.getSharing().setUserAccesses(Set.of());
    manager.save(relType, false);

    RelationshipSmsSubmission submission = new RelationshipSmsSubmission();
    int submissionId = 2;
    submission.setSubmissionId(submissionId);
    submission.setUserId(user.getUid());
    String relationshipUid = CodeGenerator.generateUid();
    submission.setRelationship(relationshipUid);
    submission.setRelationshipType(relType.getUid());
    submission.setFrom(event1.getUid());
    submission.setTo(event2.getUid());

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
    assertSmsResponse(submissionId + ":" + SmsResponse.UNKNOWN_ERROR, originator, smsMessageSender);
    assertAll(
        () -> assertEquals(SmsMessageStatus.FAILED, sms.getStatus()),
        () -> assertTrue(sms.isParsed()),
        () -> assertEquals(originator, sms.getOriginator()),
        () -> assertEquals(user, sms.getCreatedBy()));
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
    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
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

  private TrackerEvent event(Enrollment enrollment) {
    TrackerEvent event = new TrackerEvent();
    event.setEnrollment(enrollment);
    event.setProgramStage(programStage);
    event.setOrganisationUnit(enrollment.getOrganisationUnit());
    event.setAttributeOptionCombo(coc);
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

  private RelationshipType relationshipType() {
    RelationshipType type = createRelationshipType('A');
    type.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    type.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    type.getSharing().setOwner(user);
    type.getSharing().addUserAccess(fullAccess(user));
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(type, false);
    return type;
  }
}
