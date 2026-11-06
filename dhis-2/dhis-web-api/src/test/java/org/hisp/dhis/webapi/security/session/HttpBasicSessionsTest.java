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
package org.hisp.dhis.webapi.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.security.web.session.HttpSessionIdChangedEvent;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

/**
 * Tests that both {@link HttpBasicSessions} implementations leave exactly the marked sessions out,
 * and that the in-memory one forgets sessions that no longer exist.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class HttpBasicSessionsTest {

  private static final UserDetails ALICE = User.withUsername("alice").password("").build();

  @Test
  void inMemoryLeavesOutMarkedSessions() {
    InMemoryHttpBasicSessions basicSessions = new InMemoryHttpBasicSessions();
    basicSessions.markSession(requestOn(new MockHttpSession(null, "basic")));

    assertEquals(
        List.of("form"),
        ids(basicSessions.withoutHttpBasicSessions(ALICE, sessions("basic", "form"))));
  }

  @Test
  void inMemoryForgetsDestroyedSessions() {
    InMemoryHttpBasicSessions basicSessions = new InMemoryHttpBasicSessions();
    MockHttpSession session = new MockHttpSession(null, "basic");
    basicSessions.markSession(requestOn(session));

    basicSessions.sessionDestroyed(new HttpSessionDestroyedEvent(session));

    assertEquals(
        List.of("basic"), ids(basicSessions.withoutHttpBasicSessions(ALICE, sessions("basic"))));
  }

  @Test
  void inMemoryKeepsTheMarkWhenTheSessionIdChanges() {
    InMemoryHttpBasicSessions basicSessions = new InMemoryHttpBasicSessions();
    basicSessions.markSession(requestOn(new MockHttpSession(null, "old")));

    basicSessions.sessionIdChanged(
        new HttpSessionIdChangedEvent(new MockHttpSession(null, "new"), "old"));

    assertEquals(
        List.of("old"), ids(basicSessions.withoutHttpBasicSessions(ALICE, sessions("old", "new"))));
  }

  @Test
  void redisLeavesOutSessionsWithTheMarkAttribute() {
    MapSession basic = new MapSession("basic");
    basic.setAttribute(RedisHttpBasicSessions.HTTP_BASIC_ATTRIBUTE, Boolean.TRUE);
    MapSession form = new MapSession("form");
    RedisHttpBasicSessions basicSessions =
        new RedisHttpBasicSessions(repositoryWith(Map.of("basic", basic, "form", form)));

    assertEquals(
        List.of("form", "gone"),
        ids(basicSessions.withoutHttpBasicSessions(ALICE, sessions("basic", "form", "gone"))));
  }

  @Test
  void redisMarksTheSessionWithAnAttribute() {
    MockHttpSession session = new MockHttpSession(null, "basic");

    new RedisHttpBasicSessions(repositoryWith(Map.of())).markSession(requestOn(session));

    assertEquals(Boolean.TRUE, session.getAttribute(RedisHttpBasicSessions.HTTP_BASIC_ATTRIBUTE));
  }

  @SuppressWarnings("unchecked")
  private static FindByIndexNameSessionRepository<Session> repositoryWith(
      Map<String, ? extends Session> sessionsById) {
    FindByIndexNameSessionRepository<Session> repository =
        mock(FindByIndexNameSessionRepository.class);
    when(repository.findByPrincipalName("alice")).thenReturn((Map<String, Session>) sessionsById);
    return repository;
  }

  private static MockHttpServletRequest requestOn(MockHttpSession session) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    return request;
  }

  private static List<SessionInformation> sessions(String... ids) {
    return Arrays.stream(ids).map(id -> new SessionInformation(ALICE, id, new Date())).toList();
  }

  private static List<String> ids(List<SessionInformation> sessions) {
    return sessions.stream().map(SessionInformation::getSessionId).toList();
  }
}
