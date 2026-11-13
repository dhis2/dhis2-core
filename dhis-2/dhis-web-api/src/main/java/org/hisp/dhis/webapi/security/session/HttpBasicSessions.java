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
import java.util.List;
import org.springframework.security.core.session.SessionInformation;

/**
 * Knows which sessions were created by an HTTP Basic login, so that they are left out of the
 * maximum number of sessions per user that an interactive login (form, OIDC) enforces.
 *
 * <p>Such sessions are registered so that they can be expired, but a client that sends credentials
 * without keeping the cookie gets a new one on every request; counting them would make the next
 * interactive login of the same user expire that user's other sessions.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface HttpBasicSessions {

  /** Marks the session of the given request as created by an HTTP Basic login. */
  void markSession(HttpServletRequest request);

  /**
   * Returns the given sessions of the principal without the ones created by an HTTP Basic login, as
   * a list that can be modified.
   */
  List<SessionInformation> withoutHttpBasicSessions(
      Object principal, List<SessionInformation> sessions);
}
