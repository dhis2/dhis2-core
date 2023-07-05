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
package org.hisp.dhis.message;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.user.User;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A {@link MessageSender} used in test setup that pretends to send messages and that gives access
 * to the messages "send" to an email using {@link #getMessagesByEmail(String)}.
 *
 * @author Jan Bernitt
 */
public class FakeMessageSender implements MessageSender {
  private final Map<String, List<OutboundMessage>> sendMessagesByRecipient = new HashMap<>();

  public List<OutboundMessage> getMessagesByEmail(String recipient) {
    return unmodifiableList(sendMessagesByRecipient.getOrDefault(recipient, emptyList()));
  }

  @Override
  public OutboundMessageResponse sendMessage(
      String subject,
      String text,
      String footer,
      User sender,
      Set<User> recipients,
      boolean forceSend) {
    return sendMessage(
        subject,
        text + (footer == null ? "" : "\n[" + footer + "]"),
        recipients.stream().map(User::getEmail).collect(toSet()));
  }

  @Override
  public Future<OutboundMessageResponse> sendMessageAsync(
      String subject,
      String text,
      String footer,
      User sender,
      Set<User> recipients,
      boolean forceSend) {
    return completedFuture(sendMessage(subject, text, footer, sender, recipients, forceSend));
  }

  @Override
  public OutboundMessageResponse sendMessage(String subject, String text, Set<String> recipients) {
    OutboundMessage message = new OutboundMessage(subject, text, recipients);
    for (String recipient : recipients) {
      sendMessagesByRecipient.computeIfAbsent(recipient, key -> new ArrayList<>()).add(message);
    }
    OutboundMessageResponse response = new OutboundMessageResponse();
    response.setOk(true);
    response.setAsync(false);
    response.setDescription(subject + ":" + text);
    response.setResponseObject(EmailResponse.SENT);
    return response;
  }

  @Override
  public OutboundMessageResponse sendMessage(String subject, String text, String recipient) {
    return sendMessage(subject, text, singleton(recipient));
  }

  @Override
  public OutboundMessageResponseSummary sendMessageBatch(OutboundMessageBatch batch) {
    for (OutboundMessage msg : batch.getMessages()) {
      sendMessage(msg.getSubject(), msg.getText(), msg.getRecipients());
    }
    OutboundMessageResponseSummary summary = new OutboundMessageResponseSummary();
    int n = batch.getMessages().size();
    summary.setSent(n);
    summary.setTotal(n);
    summary.setBatchStatus(OutboundMessageBatchStatus.COMPLETED);
    summary.setChannel(batch.getDeliveryChannel());
    return summary;
  }

  @Override
  public ListenableFuture<OutboundMessageResponseSummary> sendMessageBatchAsync(
      OutboundMessageBatch batch) {
    return new AsyncResult<>(sendMessageBatch(batch));
  }

  @Override
  public boolean isConfigured() {
    return true;
  }
}
