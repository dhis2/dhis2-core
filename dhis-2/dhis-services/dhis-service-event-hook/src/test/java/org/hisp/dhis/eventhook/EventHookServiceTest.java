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
package org.hisp.dhis.eventhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.test.cache.TestCache;
import org.junit.jupiter.api.Test;

class EventHookServiceTest extends TestBase {

  @Test
  void testReloadRefreshesEventHookTargets() throws Exception {
    EventHook barEventHook = new EventHook();
    barEventHook.setUid(CodeGenerator.generateUid());
    WebhookTarget barWebhookTarget = new WebhookTarget();
    barWebhookTarget.setUrl("bar");
    barEventHook.setTargets(List.of(barWebhookTarget));

    EventHook fooEventHook = new EventHook();
    fooEventHook.setUid(CodeGenerator.generateUid());
    WebhookTarget fooWebhookTarget = new WebhookTarget();
    fooWebhookTarget.setUrl("foo");
    WebhookTarget quuzWebhookTarget = new WebhookTarget();
    quuzWebhookTarget.setUrl("quuz");
    fooEventHook.setTargets(List.of(fooWebhookTarget, quuzWebhookTarget));

    EventHookStore mockEventHookStore = mock(EventHookStore.class);
    when(mockEventHookStore.getAll())
        .thenReturn(List.of())
        .thenReturn(List.of(barEventHook, fooEventHook));

    CacheProvider mockCacheProvider = mock(CacheProvider.class);
    when(mockCacheProvider.<EventHookTargets>createEventHookTargetsCache())
        .thenReturn(new TestCache<>());

    EventHookService eventHookService = newEventHookService(mockEventHookStore, mockCacheProvider);
    eventHookService.postConstruct();
    eventHookService.reload();

    List<EventHookTargets> eventHookTargets =
        eventHookService.getEventHookTargets().stream()
            .sorted((o1, o2) -> o1.getEventHook().equals(barEventHook) ? -1 : 1)
            .toList();
    assertEquals(
        barEventHook.getUID().getValue(),
        eventHookTargets.get(0).getEventHook().getUID().getValue());
    assertEquals(1, eventHookTargets.get(0).getTargets().size());

    assertEquals(
        fooEventHook.getUID().getValue(),
        eventHookTargets.get(1).getEventHook().getUID().getValue());
    assertEquals(2, eventHookTargets.get(1).getTargets().size());
  }

  @Test
  void testReloadDoesNotCacheDisabledEventHooks() throws Exception {
    EventHook barEventHook = new EventHook();
    barEventHook.setDisabled(true);
    barEventHook.setUid(CodeGenerator.generateUid());
    WebhookTarget barWebhookTarget = new WebhookTarget();
    barWebhookTarget.setUrl("bar");
    barEventHook.setTargets(List.of(barWebhookTarget));

    EventHook fooEventHook = new EventHook();
    fooEventHook.setUid(CodeGenerator.generateUid());
    WebhookTarget fooWebhookTarget = new WebhookTarget();
    fooWebhookTarget.setUrl("foo");
    fooEventHook.setTargets(List.of(fooWebhookTarget));

    EventHookStore mockEventHookStore = mock(EventHookStore.class);
    when(mockEventHookStore.getAll())
        .thenReturn(List.of())
        .thenReturn(List.of(barEventHook, fooEventHook));

    CacheProvider mockCacheProvider = mock(CacheProvider.class);
    when(mockCacheProvider.<EventHookTargets>createEventHookTargetsCache())
        .thenReturn(new TestCache<>());

    EventHookService eventHookService = newEventHookService(mockEventHookStore, mockCacheProvider);
    eventHookService.postConstruct();
    eventHookService.reload();

    List<EventHookTargets> eventHookTargets = eventHookService.getEventHookTargets();
    assertEquals(1, eventHookTargets.size());
    assertEquals(fooEventHook.getUID(), eventHookTargets.get(0).getEventHook().getUID());
  }

  private EventHookService newEventHookService(
      EventHookStore eventHookStore, CacheProvider cacheProvider) {
    return new EventHookService(
        null,
        null,
        eventHookStore,
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
        new EventHookSecretManager(null) {
          @Override
          public void decrypt(EventHook eventHook) {}
        },
        cacheProvider,
        null);
  }
}
