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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageOperationParams;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramMessageServiceTest extends PostgresIntegrationTestBase {
  private String TEXT = "Hi";
  private String MSISDN = "4742312555";
  private String SUBJECT = "subjectText";

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private Enrollment enrollment;
  private TrackerEvent trackerEvent;
  private SingleEvent singleEvent;

  private ProgramMessageStatus messageStatus = ProgramMessageStatus.OUTBOUND;

  private ProgramMessage programMessageA;
  private ProgramMessage programMessageB;
  private ProgramMessage programMessageC;
  private ProgramMessage programMessageD;

  private ProgramMessageRecipients recipientA;
  private ProgramMessageRecipients recipientB;
  private ProgramMessageRecipients recipientC;
  private ProgramMessageRecipients recipientD;

  @Autowired private TestSetup testSetup;
  @Autowired private ProgramMessageService programMessageService;
  @Autowired private OrganisationUnitService orgUnitService;
  @Autowired private IdentifiableObjectManager manager;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
    testSetup.importTrackerData();
    enrollment = manager.get(Enrollment.class, "nxP7UnKhomJ");
    trackerEvent = manager.get(TrackerEvent.class, "pTzf9KYMk72");
    singleEvent = manager.get(SingleEvent.class, "QRYjLTiJTrA");

    ouA = createOrganisationUnit('A');
    ouA.setPhoneNumber(MSISDN);
    ouB = createOrganisationUnit('B');
    orgUnitService.addOrganisationUnit(ouA);
    orgUnitService.addOrganisationUnit(ouB);

    Set<String> phoneNumberListA = new HashSet<>();
    phoneNumberListA.add(MSISDN);

    recipientA = new ProgramMessageRecipients();
    recipientA.setPhoneNumbers(phoneNumberListA);
    recipientB = new ProgramMessageRecipients();
    recipientB.setPhoneNumbers(phoneNumberListA);
    recipientC = new ProgramMessageRecipients();
    recipientC.setPhoneNumbers(phoneNumberListA);
    recipientD = new ProgramMessageRecipients();
    recipientD.setPhoneNumbers(phoneNumberListA);

    programMessageA =
        createProgramMessage(TEXT, SUBJECT, recipientA, messageStatus, Set.of(DeliveryChannel.SMS));
    programMessageA.setEnrollment(enrollment);
    programMessageA.setStoreCopy(false);
    programMessageB =
        createProgramMessage(TEXT, SUBJECT, recipientB, messageStatus, Set.of(DeliveryChannel.SMS));
    programMessageB.setTrackerEvent(trackerEvent);
    programMessageC =
        createProgramMessage(TEXT, SUBJECT, recipientC, messageStatus, Set.of(DeliveryChannel.SMS));
    programMessageC.setSingleEvent(singleEvent);
    programMessageD =
        createProgramMessage(TEXT, SUBJECT, recipientD, messageStatus, Set.of(DeliveryChannel.SMS));

    programMessageService.saveProgramMessage(programMessageA);
    programMessageService.saveProgramMessage(programMessageB);
    programMessageService.saveProgramMessage(programMessageC);
    programMessageService.saveProgramMessage(programMessageD);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void shouldDeleteProgramMessage() {
    programMessageService.deleteProgramMessage(
        programMessageService.getProgramMessage(programMessageA.getUid()));
    ProgramMessage deletedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getId());

    assertNull(deletedProgramMessage, "The program message should be null after deletion");
  }

  @Test
  void shouldReturnAllSavedProgramMessages() {
    List<ProgramMessage> programMessages = programMessageService.getAllProgramMessages();

    assertContainsOnly(
        List.of(programMessageA, programMessageB, programMessageC, programMessageD),
        programMessages);
  }

  @Test
  void shouldReturnProgramMessageById() {
    ProgramMessage retrievedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getId());

    assertEqualProgramMessages(retrievedProgramMessage, programMessageA);
  }

  @Test
  void shouldReturnProgramMessageByUid() {
    ProgramMessage retrievedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getUid());

    assertEqualProgramMessages(retrievedProgramMessage, programMessageA);
  }

  @Test
  void shouldFailToRetrieveProgramMessagesWhenEnrollmentIsNotFound() {
    ProgramMessageOperationParams programMessageOperationParams =
        ProgramMessageOperationParams.builder().enrollment(UID.generate()).build();
    assertThrows(
        NotFoundException.class,
        () -> programMessageService.getProgramMessages(programMessageOperationParams));
  }

  @Test
  void shouldFailToRetrieveProgramMessagesWhenEventIsNotFound() {
    ProgramMessageOperationParams programMessageOperationParams =
        ProgramMessageOperationParams.builder().event(UID.generate()).build();
    assertThrows(
        NotFoundException.class,
        () -> programMessageService.getProgramMessages(programMessageOperationParams));
  }

  @Test
  void shouldReturnProgramMessagesForEnrollment() throws NotFoundException {
    ProgramMessageOperationParams programMessageOperationParams =
        ProgramMessageOperationParams.builder().enrollment(UID.of(enrollment)).build();
    List<ProgramMessage> programMessages =
        programMessageService.getProgramMessages(programMessageOperationParams);

    assertContainsOnly(List.of(programMessageA), programMessages);
    assertEquals(
        Set.of(DeliveryChannel.SMS),
        programMessages.get(0).getDeliveryChannels(),
        "The delivery channels should match the expected channels");
  }

  @Test
  void shouldReturnProgramMessagesForTrackerEvent() throws NotFoundException {
    ProgramMessageOperationParams programMessageOperationParams =
        ProgramMessageOperationParams.builder().event(UID.of(trackerEvent)).build();
    List<ProgramMessage> programMessages =
        programMessageService.getProgramMessages(programMessageOperationParams);

    assertContainsOnly(List.of(programMessageB), programMessages);
    assertEquals(
        Set.of(DeliveryChannel.SMS),
        programMessages.get(0).getDeliveryChannels(),
        "The delivery channels should match the expected channels");
  }

  @Test
  void shouldReturnProgramMessagesForSingleEvent() throws NotFoundException {
    ProgramMessageOperationParams programMessageOperationParams =
        ProgramMessageOperationParams.builder().event(UID.of(singleEvent)).build();
    List<ProgramMessage> programMessages =
        programMessageService.getProgramMessages(programMessageOperationParams);

    assertContainsOnly(List.of(programMessageC), programMessages);
    assertEquals(
        Set.of(DeliveryChannel.SMS),
        programMessages.get(0).getDeliveryChannels(),
        "The delivery channels should match the expected channels");
  }

  @Test
  void shouldSaveProgramMessage() {
    assertEquals(programMessageService.getProgramMessage(programMessageA.getId()), programMessageA);
  }

  @Test
  void shouldUpdateProgramMessage() {
    ProgramMessage programMessage =
        programMessageService.getProgramMessage(programMessageA.getId());
    programMessage.setText("hello");

    programMessageService.updateProgramMessage(programMessage);
    ProgramMessage updatedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getId());

    assertNotNull(updatedProgramMessage, "The updated program message should not be null");
    assertEquals(
        "hello",
        updatedProgramMessage.getText(),
        "The text of the updated program message should be 'hello'");
  }

  private void assertEqualProgramMessages(
      ProgramMessage retrievedProgramMessage, ProgramMessage actual) {
    assertNotNull(retrievedProgramMessage, "The retrieved program message should not be null");
    assertEquals(
        actual,
        retrievedProgramMessage,
        "The retrieved program message should match the saved program message");
  }
}
