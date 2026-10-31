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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionIdChangedEvent;
import org.springframework.security.core.session.SessionInformation;

/**
 * {@link HttpBasicSessions} for sessions held by the servlet container: keeps the ids of the HTTP
 * Basic sessions in memory, and forgets an id when its session is destroyed.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class InMemoryHttpBasicSessions implements HttpBasicSessions {

  private final Set<String> sessionIds = ConcurrentHashMap.newKeySet();

  @Override
  public void markSession(HttpServletRequest request) {
    sessionIds.add(request.getSession().getId());
  }

  @Override
  public List<SessionInformation> withoutHttpBasicSessions(
      Object principal, List<SessionInformation> sessions) {
    return sessions.stream()
        .filter(session -> !sessionIds.contains(session.getSessionId()))
        .collect(toCollection(ArrayList::new));
  }

  @EventListener
  public void sessionDestroyed(SessionDestroyedEvent event) {
    sessionIds.remove(event.getId());
  }

  @EventListener
  public void sessionIdChanged(SessionIdChangedEvent event) {
    if (sessionIds.remove(event.getOldSessionId())) {
      sessionIds.add(event.getNewSessionId());
    }
  }
}
