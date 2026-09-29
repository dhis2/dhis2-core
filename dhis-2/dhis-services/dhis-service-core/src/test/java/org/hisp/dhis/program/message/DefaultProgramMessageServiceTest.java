/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.program.message;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.email.EmailMessageBatchCreatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.sms.SmsMessageBatchCreatorService;
import org.hisp.dhis.trackedentity.ApiTrackedEntityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultProgramMessageService#sendMessages(List)}, focusing on how delivery
 * channels are resolved against the recipient's available contact details.
 */
@ExtendWith(MockitoExtension.class)
class DefaultProgramMessageServiceTest {

  private static final String OU_UID = "OrgUnitUid1";

  private static final String OU_EMAIL = "orgunit@test.org";

  private static final String OU_PHONE = "4712345678";

  @Mock private IdentifiableObjectManager manager;

  @Mock private ProgramMessageStore programMessageStore;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private OutboundMessageBatchService messageBatchService;

  @Mock private AclService aclService;

  @Mock private ProgramMessageOperationParamMapper operationParamMapper;

  @Mock private ApiTrackedEntityAuditService trackedEntityAuditService;

  @Captor private ArgumentCaptor<List<OutboundMessageBatch>> batchCaptor;

  private DefaultProgramMessageService service;

  @BeforeEach
  void setUp() {
    EmailDeliveryChannelStrategy emailStrategy = new EmailDeliveryChannelStrategy();
    emailStrategy.organisationUnitService = organisationUnitService;
    SmsDeliveryChannelStrategy smsStrategy = new SmsDeliveryChannelStrategy();
    smsStrategy.organisationUnitService = organisationUnitService;

    service =
        new DefaultProgramMessageService(
            manager,
            programMessageStore,
            organisationUnitService,
            messageBatchService,
            List.of(emailStrategy, smsStrategy),
            List.of(new SmsMessageBatchCreatorService(), new EmailMessageBatchCreatorService()),
            aclService,
            operationParamMapper,
            trackedEntityAuditService);
  }

  @Test
  void shouldSendEmailAndDiscardSmsWhenOrgUnitContactHasEmailButNoPhoneNumber() {
    OrganisationUnit orgUnit = orgUnitContact(OU_EMAIL, null);
    ProgramMessage message =
        orgUnitContactMessage(orgUnit, DeliveryChannel.SMS, DeliveryChannel.EMAIL);

    when(organisationUnitService.getOrganisationUnit(OU_UID)).thenReturn(orgUnit);
    when(messageBatchService.sendBatches(anyList())).thenReturn(List.of());

    service.sendMessages(new ArrayList<>(List.of(message)));

    // The channel stays configured on the message: whether a recipient could actually be
    // resolved for it is decided when the OutboundMessage is built for that channel, not by
    // removing the channel upfront. This differs from a tracked entity recipient, whose missing
    // attribute still removes the channel (see DefaultProgramMessageService
    // #setAttributesBasedOnStrategy).
    assertContainsOnly(
        Set.of(DeliveryChannel.SMS, DeliveryChannel.EMAIL), message.getDeliveryChannels());

    verify(messageBatchService).sendBatches(batchCaptor.capture());
    List<OutboundMessageBatch> batches = batchCaptor.getValue();

    // The org unit has no phone number, so no SMS OutboundMessage is ever built for it and no SMS
    // batch is created at all -- it's discarded, not sent as a batch that then fails.
    assertEquals(1, batches.size(), "only the EMAIL batch should be created");
    OutboundMessageBatch emailBatch = getBatch(batches, DeliveryChannel.EMAIL);
    assertEquals(1, emailBatch.getMessages().size());
    assertContainsOnly(Set.of(OU_EMAIL), emailBatch.getMessages().get(0).getRecipients());
  }

  @Test
  void shouldDiscardBothChannelsWhenOrgUnitContactHasNeitherEmailNorPhoneNumber() {
    OrganisationUnit orgUnit = orgUnitContact(null, null);
    ProgramMessage message =
        orgUnitContactMessage(orgUnit, DeliveryChannel.SMS, DeliveryChannel.EMAIL);

    when(organisationUnitService.getOrganisationUnit(OU_UID)).thenReturn(orgUnit);
    when(messageBatchService.sendBatches(anyList())).thenReturn(List.of());

    service.sendMessages(new ArrayList<>(List.of(message)));

    // Same outcome as before this behavior was reworked: with no deliverable contact detail at
    // all, no batch is created for either channel -- nothing is attempted and no FAILED batch is
    // reported, rather than the whole send being reported as failed.
    verify(messageBatchService).sendBatches(batchCaptor.capture());
    List<OutboundMessageBatch> batches = batchCaptor.getValue();
    assertEquals(0, batches.size(), "no batch should be created for either channel");
  }

  @Test
  void shouldDiscardBothChannelsWhenOrgUnitContactHasBlankEmailAndPhoneNumber() {
    // A blank (non-null, empty/whitespace) contact detail must be treated the same as a missing
    // one, not added as a recipient.
    OrganisationUnit orgUnit = orgUnitContact("", " ");
    ProgramMessage message =
        orgUnitContactMessage(orgUnit, DeliveryChannel.SMS, DeliveryChannel.EMAIL);

    when(organisationUnitService.getOrganisationUnit(OU_UID)).thenReturn(orgUnit);
    when(messageBatchService.sendBatches(anyList())).thenReturn(List.of());

    service.sendMessages(new ArrayList<>(List.of(message)));

    verify(messageBatchService).sendBatches(batchCaptor.capture());
    List<OutboundMessageBatch> batches = batchCaptor.getValue();
    assertEquals(0, batches.size(), "no batch should be created for either channel");
  }

  @Test
  void shouldSendBothChannelsWhenOrgUnitContactHasEmailAndPhoneNumber() {
    OrganisationUnit orgUnit = orgUnitContact(OU_EMAIL, OU_PHONE);
    ProgramMessage message =
        orgUnitContactMessage(orgUnit, DeliveryChannel.SMS, DeliveryChannel.EMAIL);

    when(organisationUnitService.getOrganisationUnit(OU_UID)).thenReturn(orgUnit);
    when(messageBatchService.sendBatches(anyList())).thenReturn(List.of());

    service.sendMessages(new ArrayList<>(List.of(message)));

    assertContainsOnly(
        Set.of(DeliveryChannel.SMS, DeliveryChannel.EMAIL), message.getDeliveryChannels());

    verify(messageBatchService).sendBatches(batchCaptor.capture());
    List<OutboundMessageBatch> batches = batchCaptor.getValue();
    assertEquals(2, batches.size(), "both the SMS and EMAIL batches should be sent");
  }

  private static OutboundMessageBatch getBatch(
      List<OutboundMessageBatch> batches, DeliveryChannel channel) {
    return batches.stream()
        .filter(b -> b.getDeliveryChannel() == channel)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No batch found for channel " + channel));
  }

  private static OrganisationUnit orgUnitContact(String email, String phoneNumber) {
    OrganisationUnit orgUnit = new OrganisationUnit("Contact");
    orgUnit.setUid(OU_UID);
    orgUnit.setEmail(email);
    orgUnit.setPhoneNumber(phoneNumber);
    return orgUnit;
  }

  private static ProgramMessage orgUnitContactMessage(
      OrganisationUnit orgUnit, DeliveryChannel... channels) {
    ProgramMessageRecipients recipients = new ProgramMessageRecipients();
    recipients.setOrganisationUnit(orgUnit);

    ProgramMessage message = new ProgramMessage();
    message.setSubject("subject");
    message.setText("text");
    message.setRecipients(recipients);
    message.setDeliveryChannels(new HashSet<>(Set.of(channels)));
    return message;
  }
}
