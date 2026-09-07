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
package org.hisp.dhis.webapi.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.security.session.SessionStatisticsProvider.SessionGauges;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

/**
 * Tests the servlet container session lifecycle counters, see {@link SessionStatisticsProvider}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Stian Sandvold
 */
class SessionStatisticsProviderTest {

  private SessionStatisticsProvider provider;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.<SessionGauges>createDataSummarySessionGaugesCache())
        .thenReturn((Cache<SessionGauges>) mock(Cache.class));
    provider = new SessionStatisticsProvider(mock(UserService.class), cacheProvider);
  }

  @Test
  void httpSessionsCountsCreatedMinusDestroyed() {
    MockHttpSession first = new MockHttpSession();
    MockHttpSession second = new MockHttpSession();

    provider.onSessionCreated(new HttpSessionCreatedEvent(first));
    provider.onSessionCreated(new HttpSessionCreatedEvent(second));
    assertEquals(2, provider.getHttpSessions());

    provider.onSessionDestroyed(new HttpSessionDestroyedEvent(first));
    assertEquals(1, provider.getHttpSessions());
    assertEquals(2, provider.getSessionsCreatedTotal());
    assertEquals(1, provider.getSessionsDestroyedTotal());
  }

  @Test
  void httpSessionsNeverGoesNegative() {
    provider.onSessionDestroyed(new HttpSessionDestroyedEvent(new MockHttpSession()));

    assertEquals(0, provider.getHttpSessions());
    assertEquals(1, provider.getSessionsDestroyedTotal());
  }
}
