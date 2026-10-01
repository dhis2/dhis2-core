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
package org.hisp.dhis.outboundmessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.message.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultOutboundMessageBatchService}. Filtering out batches that have no
 * deliverable recipient at all happens upstream now (in the {@code MessageBatchCreatorService}s),
 * so this class only has to worry about dispatching whatever batches it is actually given.
 */
@ExtendWith(MockitoExtension.class)
class DefaultOutboundMessageBatchServiceTest {

  @Mock private MessageSender emailSender;

  @Mock private MessageSender smsSender;

  private DefaultOutboundMessageBatchService service;

  @BeforeEach
  void setUp() {
    service = new DefaultOutboundMessageBatchService();
    service.setMessageSenders(
        Map.of(
            DeliveryChannel.EMAIL, emailSender,
            DeliveryChannel.SMS, smsSender));
  }

  @Test
  void shouldSendBatchWhenSenderIsConfigured() {
    when(emailSender.isConfigured()).thenReturn(true);
    when(emailSender.sendMessageBatch(any()))
        .thenReturn(
            new OutboundMessageResponseSummary(
                "ok", DeliveryChannel.EMAIL, OutboundMessageBatchStatus.COMPLETED));

    OutboundMessageBatch emailBatch =
        new OutboundMessageBatch(
            List.of(new OutboundMessage("subj", "text", Set.of("orgunit@test.org"))),
            DeliveryChannel.EMAIL);

    List<OutboundMessageResponseSummary> results = service.sendBatches(List.of(emailBatch));

    assertEquals(1, results.size());
    assertEquals(DeliveryChannel.EMAIL, results.get(0).getChannel());
    assertEquals(OutboundMessageBatchStatus.COMPLETED, results.get(0).getBatchStatus());
    verify(emailSender, times(1)).sendMessageBatch(any());

    assertTrue(new BatchResponseStatus(results).isOk());
  }

  @Test
  void shouldReportFailureWhenSenderNotConfigured() {
    when(smsSender.isConfigured()).thenReturn(false);

    OutboundMessageBatch smsBatch =
        new OutboundMessageBatch(
            List.of(new OutboundMessage("subj", "text", Set.of("4712345678"))),
            DeliveryChannel.SMS);

    List<OutboundMessageResponseSummary> results = service.sendBatches(List.of(smsBatch));

    assertEquals(1, results.size());
    assertEquals(OutboundMessageBatchStatus.FAILED, results.get(0).getBatchStatus());
    verify(smsSender, never()).sendMessageBatch(any());
  }

  @Test
  void shouldReportFailureWhenNoSenderIsRegisteredForChannel() {
    OutboundMessageBatch smsBatch =
        new OutboundMessageBatch(
            List.of(new OutboundMessage("subj", "text", Set.of("4712345678"))),
            DeliveryChannel.EMAIL);
    service.setMessageSenders(Map.of());

    List<OutboundMessageResponseSummary> results = service.sendBatches(List.of(smsBatch));

    assertEquals(1, results.size());
    assertEquals(OutboundMessageBatchStatus.FAILED, results.get(0).getBatchStatus());
  }
}
