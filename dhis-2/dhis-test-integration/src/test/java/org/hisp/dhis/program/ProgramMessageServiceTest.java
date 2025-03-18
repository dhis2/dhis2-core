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

import com.google.common.collect.Sets;
import java.util.Date;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Transactional
class ProgramMessageServiceTest extends PostgresIntegrationTestBase {
  private String TEXT = "Hi";
  private String MSISDN = "4742312555";
  private String SUBJECT = "subjectText";

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private Enrollment enrollmentA;

  private ProgramMessageStatus messageStatus = ProgramMessageStatus.OUTBOUND;

  private ProgramMessageOperationParams params;

  private ProgramMessage programMessageA;
  private ProgramMessage programMessageB;
  private ProgramMessage programMessageC;

  private ProgramMessageRecipients recipientA;
  private ProgramMessageRecipients recipientB;
  private ProgramMessageRecipients recipientC;

  @Autowired private ProgramMessageService programMessageService;
  @Autowired private OrganisationUnitService orgUnitService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private ProgramService programService;

  @BeforeEach
  void setUp() {
    ouA = createOrganisationUnit('A');
    ouA.setPhoneNumber(MSISDN);
    ouB = createOrganisationUnit('B');
    orgUnitService.addOrganisationUnit(ouA);
    orgUnitService.addOrganisationUnit(ouB);
    Program program = createProgram('A');
    program.setAutoFields();
    program.setOrganisationUnits(Sets.newHashSet(ouA, ouB));
    program.setName("programA");
    program.setShortName("programAshortname");
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programService.addProgram(program);
    enrollmentA = new Enrollment();
    enrollmentA.setProgram(program);
    enrollmentA.setOrganisationUnit(ouA);
    enrollmentA.setName("enrollmentA");
    enrollmentA.setEnrollmentDate(new Date());
    enrollmentA.setAutoFields();
    manager.save(enrollmentA);

    Set<String> phoneNumberListA = new HashSet<>();
    phoneNumberListA.add(MSISDN);

    recipientA = new ProgramMessageRecipients();
    recipientA.setPhoneNumbers(phoneNumberListA);
    recipientB = new ProgramMessageRecipients();
    recipientB.setPhoneNumbers(phoneNumberListA);
    recipientC = new ProgramMessageRecipients();
    recipientC.setPhoneNumbers(phoneNumberListA);

    programMessageA =
        createProgramMessage(TEXT, SUBJECT, recipientA, messageStatus, Set.of(DeliveryChannel.SMS));
    programMessageA.setEnrollment(enrollmentA);
    programMessageA.setStoreCopy(false);
    programMessageB =
        createProgramMessage(TEXT, SUBJECT, recipientB, messageStatus, Set.of(DeliveryChannel.SMS));
    programMessageB.setEnrollment(enrollmentA);
    programMessageC =
        createProgramMessage(TEXT, SUBJECT, recipientC, messageStatus, Set.of(DeliveryChannel.SMS));

    params =
        ProgramMessageOperationParams.builder()
            .ou(Set.of())
            .enrollment(UID.of(enrollmentA))
            .messageStatus(messageStatus)
            .build();
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void shouldDeleteProgramMessage() {
    programMessageService.saveProgramMessage(programMessageA);

    programMessageService.deleteProgramMessage(programMessageA);
    ProgramMessage deletedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getId());

    assertNull(deletedProgramMessage, "The program message should be null after deletion");
  }

  @Test
  void shouldReturnAllSavedProgramMessages() {
    programMessageService.saveProgramMessage(programMessageA);
    programMessageService.saveProgramMessage(programMessageB);
    programMessageService.saveProgramMessage(programMessageC);

    List<ProgramMessage> programMessages = programMessageService.getAllProgramMessages();

    assertContainsOnly(List.of(programMessageA, programMessageB, programMessageC), programMessages);
  }

  @Test
  void shouldReturnProgramMessageById() {
    programMessageService.saveProgramMessage(programMessageA);

    ProgramMessage retrievedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getId());

    assertEqualProgramMessages(retrievedProgramMessage, programMessageA);
  }

  @Test
  void shouldReturnProgramMessageByUid() {
    programMessageService.saveProgramMessage(programMessageA);

    ProgramMessage retrievedProgramMessage =
        programMessageService.getProgramMessage(programMessageA.getUid());

    assertEqualProgramMessages(retrievedProgramMessage, programMessageA);
  }

  @Test
  void shouldReturnProgramMessagesByQuery() throws NotFoundException {
    programMessageService.saveProgramMessage(programMessageA);
    programMessageService.saveProgramMessage(programMessageB);

    List<ProgramMessage> programMessages = programMessageService.getProgramMessages(params);

    assertContainsOnly(List.of(programMessageA, programMessageB), programMessages);
    assertEquals(
        Set.of(DeliveryChannel.SMS),
        programMessages.get(0).getDeliveryChannels(),
        "The delivery channels should match the expected channels");
  }

  @Test
  void shouldSaveProgramMessage() {
    programMessageService.saveProgramMessage(programMessageA);
    assertEquals(programMessageService.getProgramMessage(programMessageA.getId()), programMessageA);
  }

  @Test
  void shouldUpdateProgramMessage() {
    programMessageService.saveProgramMessage(programMessageA);
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
