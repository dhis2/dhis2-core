/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.eventhook.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import reactor.core.publisher.Flux;

class WebhookReactiveHandlerTest {

  private static GenericContainer<?> consumerMockServerContainer;
  private MockServerClient consumerMockServerClient;

  @BeforeAll
  static void beforeAll() {
    consumerMockServerContainer =
        new GenericContainer<>("mockserver/mockserver")
            .waitingFor(new HttpWaitStrategy().forStatusCode(404))
            .withExposedPorts(1080);
    consumerMockServerContainer.start();
  }

  @BeforeEach
  void beforeEach() {
    consumerMockServerClient =
        new MockServerClient("localhost", consumerMockServerContainer.getFirstMappedPort());
  }

  @AfterEach
  void afterEach() {
    consumerMockServerClient.reset();
  }

  @Test
  void testAcceptWhenOutboxMessageBatchDeliveryPartiallySuccessful() throws Exception {
    WebhookTarget webhookTarget = new WebhookTarget();
    webhookTarget.setUrl(
        "http://localhost:" + consumerMockServerContainer.getFirstMappedPort().toString());

    WebhookReactiveHandler webhookReactiveHandler = new WebhookReactiveHandler(null, webhookTarget);
    List<Map<String, Object>> outboxMessages =
        List.of(
            Map.of("payload", "success"),
            Map.of("payload", "error 1"),
            Map.of("payload", "success"),
            Map.of("payload", "error 2"));
    CountDownLatch errorCountDownLatch = new CountDownLatch(1);
    CountDownLatch successCountDownLatch = new CountDownLatch(1);
    CountDownLatch onCompleteCountDownLatch = new CountDownLatch(1);

    consumerMockServerClient
        .when(request().withPath("/").withBody("error 1"))
        .respond(org.mockserver.model.HttpResponse.response().withStatusCode(500));

    consumerMockServerClient
        .when(request().withPath("/").withBody("error 2"))
        .respond(org.mockserver.model.HttpResponse.response().withStatusCode(500));

    consumerMockServerClient
        .when(request().withPath("/").withBody("success"))
        .respond(org.mockserver.model.HttpResponse.response().withStatusCode(200));

    webhookReactiveHandler.accept(
        Flux.fromIterable(outboxMessages),
        new ReactiveHandlerCallback() {
          @Override
          public void onError(Map<String, Object> outboxMessageCause) {
            assertEquals("error 1", outboxMessageCause.get("payload"));
            errorCountDownLatch.countDown();
          }

          @Override
          public void onSuccess(Map<String, Object> lastSuccessfulOutboxMessage) {
            successCountDownLatch.countDown();
          }

          @Override
          public void onComplete() {
            onCompleteCountDownLatch.countDown();
          }
        });

    onCompleteCountDownLatch.await();
    assertEquals(0, errorCountDownLatch.getCount());
    assertEquals(1, successCountDownLatch.getCount());
  }

  @Test
  void testAcceptWhenOutboxMessageBatchDeliveryFullySuccessfully() throws Exception {
    WebhookTarget webhookTarget = new WebhookTarget();
    webhookTarget.setUrl(
        "http://localhost:" + consumerMockServerContainer.getFirstMappedPort().toString());

    WebhookReactiveHandler webhookReactiveHandler = new WebhookReactiveHandler(null, webhookTarget);
    List<Map<String, Object>> outboxMessages =
        List.of(
            Map.of("payload", "success 1"),
            Map.of("payload", "success 2"),
            Map.of("payload", "success 3"));
    CountDownLatch errorCountDownLatch = new CountDownLatch(1);
    CountDownLatch successCountDownLatch = new CountDownLatch(1);
    CountDownLatch onCompleteCountDownLatch = new CountDownLatch(1);

    consumerMockServerClient
        .when(request().withPath("/"))
        .respond(org.mockserver.model.HttpResponse.response().withStatusCode(200));

    webhookReactiveHandler.accept(
        Flux.fromIterable(outboxMessages),
        new ReactiveHandlerCallback() {
          @Override
          public void onError(Map<String, Object> outboxMessageCause) {
            errorCountDownLatch.countDown();
          }

          @Override
          public void onSuccess(Map<String, Object> lastSuccessfulOutboxMessage) {
            assertEquals("success 3", lastSuccessfulOutboxMessage.get("payload"));
            successCountDownLatch.countDown();
          }

          @Override
          public void onComplete() {
            onCompleteCountDownLatch.countDown();
          }
        });

    onCompleteCountDownLatch.await();
    assertEquals(1, errorCountDownLatch.getCount());
    assertEquals(0, successCountDownLatch.getCount());
  }
}
