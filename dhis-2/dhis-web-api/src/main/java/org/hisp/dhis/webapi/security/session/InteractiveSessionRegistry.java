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

import java.util.List;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;

/**
 * The {@link SessionRegistry} that {@link ConcurrentSessionControlAuthenticationStrategy} counts
 * sessions in: the sessions of a principal are listed without the ones created by an HTTP Basic
 * login (see {@link HttpBasicSessions}), so only interactive logins count toward the maximum number
 * of sessions per user. Everything else goes to the registry as it is, so sessions are still
 * registered, refreshed and found by id, and the filter that expires sessions can use this view
 * too.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class InteractiveSessionRegistry implements SessionRegistry {

  private final SessionRegistry sessionRegistry;

  private final HttpBasicSessions httpBasicSessions;

  public InteractiveSessionRegistry(
      SessionRegistry sessionRegistry, HttpBasicSessions httpBasicSessions) {
    this.sessionRegistry = sessionRegistry;
    this.httpBasicSessions = httpBasicSessions;
  }

  @Override
  public List<SessionInformation> getAllSessions(Object principal, boolean includeExpiredSessions) {
    return httpBasicSessions.withoutHttpBasicSessions(
        principal, sessionRegistry.getAllSessions(principal, includeExpiredSessions));
  }

  @Override
  public List<Object> getAllPrincipals() {
    return sessionRegistry.getAllPrincipals();
  }

  @Override
  public SessionInformation getSessionInformation(String sessionId) {
    return sessionRegistry.getSessionInformation(sessionId);
  }

  @Override
  public void refreshLastRequest(String sessionId) {
    sessionRegistry.refreshLastRequest(sessionId);
  }

  @Override
  public void registerNewSession(String sessionId, Object principal) {
    sessionRegistry.registerNewSession(sessionId, principal);
  }

  @Override
  public void removeSessionInformation(String sessionId) {
    sessionRegistry.removeSessionInformation(sessionId);
  }
}
