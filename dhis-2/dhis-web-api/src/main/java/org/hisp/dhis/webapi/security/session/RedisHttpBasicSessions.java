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

import static java.util.stream.Collectors.toCollection;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * {@link HttpBasicSessions} for sessions held by Spring Session (Redis): marks an HTTP Basic
 * session with a session attribute, so that every node sees the mark, and reads the marks of a
 * principal's sessions with one lookup by principal name.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class RedisHttpBasicSessions implements HttpBasicSessions {

  static final String HTTP_BASIC_ATTRIBUTE = HttpBasicSessions.class.getName() + ".HTTP_BASIC";

  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  public RedisHttpBasicSessions(
      FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  @Override
  public void markSession(HttpServletRequest request) {
    request.getSession().setAttribute(HTTP_BASIC_ATTRIBUTE, Boolean.TRUE);
  }

  @Override
  public List<SessionInformation> withoutHttpBasicSessions(
      Object principal, List<SessionInformation> sessions) {
    if (sessions.isEmpty()) {
      return new ArrayList<>(sessions);
    }
    Map<String, ? extends Session> sessionsById =
        sessionRepository.findByPrincipalName(name(principal));
    return sessions.stream()
        .filter(session -> !isHttpBasic(sessionsById.get(session.getSessionId())))
        .collect(toCollection(ArrayList::new));
  }

  private static boolean isHttpBasic(Session session) {
    return session != null && Boolean.TRUE.equals(session.getAttribute(HTTP_BASIC_ATTRIBUTE));
  }

  /** The name {@code SpringSessionBackedSessionRegistry} looks the sessions up by. */
  private static String name(Object principal) {
    if (principal instanceof UserDetails userDetails) {
      return userDetails.getUsername();
    }
    if (principal instanceof AuthenticatedPrincipal authenticatedPrincipal) {
      return authenticatedPrincipal.getName();
    }
    if (principal instanceof Principal namedPrincipal) {
      return namedPrincipal.getName();
    }
    return principal == null ? "" : principal.toString();
  }
}
