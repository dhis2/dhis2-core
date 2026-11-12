/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.mvc.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserContextInterceptorTest {

  private final UserContextInterceptor interceptor = new UserContextInterceptor();

  @AfterEach
  void tearDown() {
    ThreadUserSettings.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void settingsAreClearedWhenALaterInterceptorEndsTheRequest() throws Exception {
    UserDetails user = mock(UserDetails.class);
    when(user.getUsername()).thenReturn("locale-user");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dataElements");
    request.setParameter("locale", "fr");
    MockHttpServletResponse response = new MockHttpServletResponse();

    interceptor.preHandle(request, response, new Object());
    assertEquals("fr", UserSettings.getCurrentSettings().getUserDbLocale().toString());

    // A later interceptor answered the request itself (an ETag 304): Spring then calls
    // afterCompletion, not postHandle, on the interceptors before it.
    interceptor.afterCompletion(request, response, new Object(), null);

    assertNotEquals("fr", UserSettings.getCurrentSettings().getUserDbLocale().toString());
  }
}
