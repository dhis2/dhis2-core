/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.program;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.message.ProgramMessageStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Transactional
class ProgramMessageStoreTest extends PostgresIntegrationTestBase {
  private static final String MSISDN = "4740332255";
  private static final String MESSAGE_TEXT = "Hi";

  @Autowired private ProgramMessageStore programMessageStore;

  @Autowired private OrganisationUnitService orgUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private IdentifiableObjectManager manager;

  private Enrollment enrollmentA;

  private final ProgramMessageStatus messageStatus = ProgramMessageStatus.SENT;

  private final Set<DeliveryChannel> channels = Set.of(DeliveryChannel.SMS);

  private ProgramMessageQueryParams params;

  private TrackerEvent eventA;

  private SingleEvent singleEvent;

  private ProgramMessage programMessageA;
  private ProgramMessage programMessageB;

  private final String notificationTemplate = CodeGenerator.generateUid();

  @BeforeEach
  void setUp() {
    OrganisationUnit orgUnitA = createOrganisationUnit('A');
    OrganisationUnit orgUnitB = createOrganisationUnit('B');
    orgUnitService.addOrganisationUnit(orgUnitA);
    orgUnitService.addOrganisationUnit(orgUnitB);

    // Initialize Program and Program Stage
    Program programA = createProgram('A', new HashSet<>(), orgUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programService.addProgram(programA);

    Program programB = createProgram('B', new HashSet<>(), orgUnitA);
    programB.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programService.addProgram(programB);

    ProgramStage stageA = new ProgramStage("StageA", programA);
    programStageService.saveProgramStage(stageA);

    ProgramStage stageB = new ProgramStage("StageB", programB);
    programStageService.saveProgramStage(stageB);

    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programA.setProgramStages(programStages);
    programService.updateProgram(programA);

    // Initialize Tracked Entities, Enrollment and Event
    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntityB = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityB);

    enrollmentA = new Enrollment(new Date(), new Date(), trackedEntityB, programA);
    enrollmentA.setUid(CodeGenerator.generateUid());
    enrollmentA.setOrganisationUnit(orgUnitA);

    eventA = createEvent(stageA, enrollmentA, orgUnitA);
    eventA.setScheduledDate(new Date());
    eventA.setUid(CodeGenerator.generateUid());

    singleEvent = createSingleEvent(stageB, orgUnitB);
    singleEvent.setUid(CodeGenerator.generateUid());

    TrackedEntity trackedEntityA = createTrackedEntity(orgUnitA, trackedEntityType);
    manager.save(trackedEntityA);

    ProgramMessageRecipients recipientsA = createRecipients(orgUnitA, trackedEntityA);
    ProgramMessageRecipients recipientsB = createRecipients(orgUnitA, trackedEntityA);

    programMessageA = createProgramMessage(MESSAGE_TEXT, recipientsA);
    programMessageB = createProgramMessage(MESSAGE_TEXT, recipientsB);

    params = ProgramMessageQueryParams.builder().build();
  }

  @Test
  void shouldReturnProgramMessageById() {
    programMessageStore.save(programMessageA);

    ProgramMessage retrievedProgramMessage = programMessageStore.get(programMessageA.getId());

    assertNotNull(retrievedProgramMessage, "The retrieved program message should not be null");
    assertEquals(
        programMessageA,
        retrievedProgramMessage,
        "The retrieved program message should match the saved program message");
  }

  @Test
  void shouldGetProgramMessages() {
    programMessageStore.save(programMessageA);
    programMessageStore.save(programMessageB);

    assertContainsOnly(List.of(programMessageA, programMessageB), programMessageStore.getAll());
  }

  @Test
  void shouldDeleteProgramMessage() {
    programMessageStore.save(programMessageA);

    programMessageStore.delete(programMessageA);

    assertNull(
        programMessageStore.get(programMessageA.getId()),
        "The program message should be null after deletion");
  }

  @Test
  void testGetProgramMessageByEnrollment() {
    manager.save(enrollmentA);
    programMessageA.setEnrollment(enrollmentA);
    programMessageB.setEnrollment(enrollmentA);
    programMessageStore.save(programMessageA);
    programMessageStore.save(programMessageB);
    params.setEnrollment(enrollmentA);

    List<ProgramMessage> programMessages = programMessageStore.getProgramMessages(params);

    assertContainsOnly(List.of(programMessageA, programMessageB), programMessages);
    assertEquals(
        channels,
        programMessages.get(0).getDeliveryChannels(),
        "Delivery channels should match for each program message");
    assertEquals(
        enrollmentA,
        programMessages.get(0).getEnrollment(),
        "Enrollment should match for each program message");
  }

  @Test
  void shouldGetProgramMessageByTrackerEvent() {
    manager.save(enrollmentA);
    manager.save(eventA);
    programMessageA.setTrackerEvent(eventA);
    programMessageB.setTrackerEvent(eventA);
    programMessageStore.save(programMessageA);
    programMessageStore.save(programMessageB);
    params.setTrackerEvent(eventA);

    List<ProgramMessage> programMessages = programMessageStore.getProgramMessages(params);

    assertNotNull(programMessages);
    assertContainsOnly(List.of(programMessageA, programMessageB), programMessages);
    assertEquals(
        channels,
        programMessages.get(0).getDeliveryChannels(),
        "Delivery channels should match for each program message");
    assertEquals(
        eventA,
        programMessages.get(0).getTrackerEvent(),
        "Event should match for each program message");
  }

  @Test
  void shouldGetProgramMessageBySingleEvent() {
    manager.save(singleEvent);
    programMessageA.setSingleEvent(singleEvent);
    programMessageB.setSingleEvent(singleEvent);
    programMessageStore.save(programMessageA);
    programMessageStore.save(programMessageB);
    params.setSingleEvent(singleEvent);

    List<ProgramMessage> programMessages = programMessageStore.getProgramMessages(params);

    assertNotNull(programMessages);
    assertContainsOnly(List.of(programMessageA, programMessageB), programMessages);
    assertEquals(
        channels,
        programMessages.get(0).getDeliveryChannels(),
        "Delivery channels should match for each program message");
    assertEquals(
        singleEvent,
        programMessages.get(0).getSingleEvent(),
        "Event should match for each program message");
  }

  @Test
  void shouldGetProgramMessageByMessageStatus() {
    programMessageStore.save(programMessageA);
    programMessageStore.save(programMessageB);
    params.setMessageStatus(messageStatus);

    List<ProgramMessage> programMessages = programMessageStore.getProgramMessages(params);

    assertContainsOnly(List.of(programMessageA, programMessageB), programMessages);
    assertEquals(
        channels,
        programMessages.get(0).getDeliveryChannels(),
        "Delivery channels should match for each program message");
    assertEquals(
        messageStatus,
        programMessages.get(0).getMessageStatus(),
        "ProgramMessageStatus should match for each program message");
  }

  @Test
  void shouldGetProgramMessageByEnrollmentAndStatus() {
    manager.save(enrollmentA);
    programMessageA.setEnrollment(enrollmentA);
    programMessageB.setEnrollment(enrollmentA);
    programMessageStore.save(programMessageA);
    programMessageStore.save(programMessageB);
    params.setEnrollment(enrollmentA);
    params.setMessageStatus(messageStatus);

    List<ProgramMessage> programMessages = programMessageStore.getProgramMessages(params);

    assertContainsOnly(List.of(programMessageA, programMessageB), programMessages);
    assertEquals(
        channels,
        programMessages.get(0).getDeliveryChannels(),
        "Delivery channels should match for each program message");
    assertEquals(
        enrollmentA,
        programMessages.get(0).getEnrollment(),
        "Enrollment should match for each program message");
    assertEquals(
        messageStatus,
        programMessages.get(0).getMessageStatus(),
        "ProgramMessageStatus should match for each program message");
  }

  private ProgramMessageRecipients createRecipients(
      OrganisationUnit orgUnit, TrackedEntity trackedEntity) {
    ProgramMessageRecipients recipients = new ProgramMessageRecipients();
    recipients.setOrganisationUnit(orgUnit);
    recipients.setTrackedEntity(trackedEntity);
    Set<String> phoneNumberList = new HashSet<>();
    phoneNumberList.add(MSISDN);
    recipients.setPhoneNumbers(phoneNumberList);
    return recipients;
  }

  private ProgramMessage createProgramMessage(String text, ProgramMessageRecipients recipients) {
    return ProgramMessage.builder()
        .subject(text)
        .text(text)
        .recipients(recipients)
        .messageStatus(messageStatus)
        .deliveryChannels(channels)
        .notificationTemplate(notificationTemplate)
        .build();
  }
}
