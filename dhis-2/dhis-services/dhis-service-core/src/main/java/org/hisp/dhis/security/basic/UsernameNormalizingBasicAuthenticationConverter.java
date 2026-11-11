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
package org.hisp.dhis.security.basic;

import jakarta.servlet.http.HttpServletRequest;
import org.hisp.dhis.user.Usernames;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;

/**
 * HTTP Basic converter that strips whitespace at the edges of the username (see {@link
 * Usernames#stripEdgeWhitespace(String)}) before the token is authenticated. The password is passed
 * on unchanged.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class UsernameNormalizingBasicAuthenticationConverter extends BasicAuthenticationConverter {

  public UsernameNormalizingBasicAuthenticationConverter(
      AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
    super(authenticationDetailsSource);
  }

  @Override
  public UsernamePasswordAuthenticationToken convert(HttpServletRequest request) {
    UsernamePasswordAuthenticationToken token = super.convert(request);
    if (token == null) {
      return null;
    }
    String username = Usernames.stripEdgeWhitespace(token.getName());
    if (username.equals(token.getName())) {
      return token;
    }
    UsernamePasswordAuthenticationToken normalized =
        UsernamePasswordAuthenticationToken.unauthenticated(username, token.getCredentials());
    normalized.setDetails(token.getDetails());
    return normalized;
  }
}
