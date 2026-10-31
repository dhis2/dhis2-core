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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy;

/**
 * Handles a request whose session has been expired, for example because the roles or user groups of
 * the user changed. The session is already logged out at this point.
 *
 * <p>A request that carries its own credentials in the {@code Authorization} header (HTTP Basic, an
 * API token or a bearer token) continues down the filter chain, so it authenticates again with the
 * current user details on a new session. Without this, an API client that sends credentials with
 * every request and also keeps the session cookie would fail once with 401 after each such change.
 * Any other request is redirected to the given URL.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class ReauthenticatingExpiredSessionStrategy implements SessionInformationExpiredStrategy {

  private final SessionInformationExpiredStrategy redirect;

  public ReauthenticatingExpiredSessionStrategy(String expiredUrl) {
    this.redirect = new SimpleRedirectSessionInformationExpiredStrategy(expiredUrl);
  }

  @Override
  public void onExpiredSessionDetected(SessionInformationExpiredEvent event)
      throws IOException, ServletException {
    HttpServletRequest request = event.getRequest();
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
      event.getFilterChain().doFilter(request, event.getResponse());
    } else {
      redirect.onExpiredSessionDetected(event);
    }
  }
}
