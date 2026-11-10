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

import static org.hisp.dhis.webapi.security.session.UserDetailsRefreshFilter.CHANGE_TOKEN_ATTRIBUTE;
import static org.hisp.dhis.webapi.security.session.UserDetailsRefreshFilter.REFRESHED_AT_ATTRIBUTE;

import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import javax.annotation.CheckForNull;

/**
 * The refresh baseline of a principal: the change token it was last refreshed for and the time it
 * was last rebuilt. While a session impersonates, the baseline of the impersonating user is kept in
 * the {@link UserDetailsRefreshFilter#SOURCE_REFRESH_STATE_ATTRIBUTE} attribute.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public record PrincipalRefreshState(
    @CheckForNull String changeToken, @CheckForNull Long refreshedAt) implements Serializable {

  /** Makes this the baseline of the session principal. */
  public void apply(HttpSession session) {
    session.setAttribute(CHANGE_TOKEN_ATTRIBUTE, changeToken);
    session.setAttribute(REFRESHED_AT_ATTRIBUTE, refreshedAt);
  }
}
