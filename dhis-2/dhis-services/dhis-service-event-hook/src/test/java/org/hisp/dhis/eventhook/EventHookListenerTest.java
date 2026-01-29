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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.cache.TestCache;
import org.hisp.dhis.user.AuthenticationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.jdbc.core.JdbcTemplate;

class EventHookListenerTest {

  private final ObjectMapper objectMapper = JacksonObjectMapperConfig.staticJsonMapper();
  private final AuthenticationService mockAuthenticationService =
      new AuthenticationService() {
        @Override
        public void obtainAuthentication(String userId) {}

        @Override
        public void obtainAuthentication(UserDetails userDetails) {}

        @Override
        public void obtainSystemAuthentication() {}

        @Override
        public void clearAuthentication() {}
      };

  private final FieldFilterService fieldFilterService = mock(FieldFilterService.class);

  @Test
  void testOnPreCommitPersistsEvent() throws Exception {
    CacheProvider mockCacheProvider = mock(CacheProvider.class);
    EventHook eventHook = createMockEventHook();
    EventHookStore eventHookStore = mock(EventHookStore.class);
    when(eventHookStore.getAll()).thenReturn(List.of(eventHook));

    when(mockCacheProvider.<EventHookTargets>createEventHookTargetsCache())
        .thenReturn(new TestCache<>());

    EventHookService eventHookService =
        new EventHookService(
            null,
            null,
            eventHookStore,
            objectMapper,
            new EventHookSecretManager(null),
            mockCacheProvider,
            null);
    eventHookService.postConstruct();

    JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
    when(mockJdbcTemplate.update(anyString(), anyString())).thenReturn(1);
    EventHookListener eventHookListener =
        new EventHookListener(
            objectMapper,
            fieldFilterService,
            mockAuthenticationService,
            mockJdbcTemplate,
            eventHookService);

    OrganisationUnit organisationUnit = new OrganisationUnit();
    eventHookListener.onPreCommit(EventUtils.metadataCreate(organisationUnit));

    Collection<Invocation> invocations = mockingDetails(mockJdbcTemplate).getInvocations();
    String eventAsString = invocations.iterator().next().getArgument(1);
    Map<String, Object> event = objectMapper.readValue(eventAsString, Map.class);

    assertEquals("metadata.organisationUnit." + organisationUnit.getUid(), event.get("path"));
    assertEquals("create", (((Map<String, Object>) event.get("meta")).get("op")));
    assertEquals(organisationUnit.getUid(), ((Map<String, Object>) event.get("object")).get("id"));
  }

  private EventHook createMockEventHook() {
    Source source = new Source();
    source.setPath("metadata.");

    User user = new User();
    user.setUid(CodeGenerator.generateUid());

    WebhookTarget webhookTarget = new WebhookTarget();
    webhookTarget.setUrl("http://stub");

    EventHook eventHook = new EventHook();
    eventHook.setUser(user);
    eventHook.setTargets(List.of(webhookTarget));
    eventHook.setUid(CodeGenerator.generateUid());
    eventHook.setSource(source);

    return eventHook;
  }
}
