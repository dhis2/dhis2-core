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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Saves the security context of an HTTP Basic login in the HTTP session, like {@link
 * HttpSessionSecurityContextRepository}, and also registers that session in the {@link
 * SessionRegistry}.
 *
 * <p>Since the context is already in the session when the {@code SessionManagementFilter} runs,
 * that filter skips its session authentication strategies, so without this the session would stay
 * unknown to an in-memory registry. Sessions the registry does not know are never expired when the
 * roles or user groups of the user change, and keep the user details from login.
 *
 * <p>The session is also marked in {@link HttpBasicSessions}, so that it does not count toward the
 * maximum number of sessions per user: neither on this request nor at the user's next interactive
 * login. An API client that sends credentials without keeping the cookie, and thereby gets a new
 * session on every request, does not log the same user out elsewhere. With Redis, the registry
 * finds sessions by principal name and registering is a no-op.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class SessionRegisteringSecurityContextRepository implements SecurityContextRepository {

  private final HttpSessionSecurityContextRepository delegate =
      new HttpSessionSecurityContextRepository();

  private final RegisterSessionAuthenticationStrategy registerSession;

  private final HttpBasicSessions httpBasicSessions;

  public SessionRegisteringSecurityContextRepository(
      SessionRegistry sessionRegistry, HttpBasicSessions httpBasicSessions) {
    this.registerSession = new RegisterSessionAuthenticationStrategy(sessionRegistry);
    this.httpBasicSessions = httpBasicSessions;
  }

  @Override
  @SuppressWarnings("deprecation")
  public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
    return delegate.loadContext(requestResponseHolder);
  }

  @Override
  public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
    return delegate.loadDeferredContext(request);
  }

  @Override
  public void saveContext(
      SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    delegate.saveContext(context, request, response);
    Authentication authentication = context.getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      registerSession.onAuthentication(authentication, request, response);
      httpBasicSessions.markSession(request);
    }
  }

  @Override
  public boolean containsContext(HttpServletRequest request) {
    return delegate.containsContext(request);
  }
}
