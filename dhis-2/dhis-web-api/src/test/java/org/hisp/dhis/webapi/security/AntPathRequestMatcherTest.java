/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Behavioural coverage for the Security 7 drop-in matcher used by {@code
 * DhisWebApiWebSecurityConfig}. Mid-pattern double-star rules are the reason this class exists
 * instead of {@code PathPatternRequestMatcher}.
 *
 * @author Morten Svanæs
 */
class AntPathRequestMatcherTest {

  @Test
  void matchAllPatternMatchesAnyPath() {
    AntPathRequestMatcher matcher = new AntPathRequestMatcher("/**");
    assertTrue(matcher.matches(request("/")));
    assertTrue(matcher.matches(request("/api/me")));
    assertTrue(matcher.matches(request("/anything/else")));
  }

  @ParameterizedTest
  @CsvSource({
    // literal + prefix patterns
    "/api/ping, /api/ping, true",
    "/api/ping, /api/system/ping, false",
    "/api/system/ping, /api/system/ping, true",
    "/favicon.ico, /favicon.ico, true",
    "/oauth2/authorize, /oauth2/authorize, true",
    "/oauth2/token, /oauth2/token, true",
    "/oauth2/**, /oauth2/authorize, true",
    "/oauth2/**, /oauth2/code/google, true",
    "/oauth2/**, /api/oauth2/token, false",
    "/api/apps/login/**, /api/apps/login/index.html, true",
    "/login/**, /login/index.html, true",
    "/login.html, /login.html, true",
    "/static/**, /static/css/app.css, true",
    "/external-static/**, /external-static/file.png, true",
    // mid-pattern versioned API rules
    "/api/**/loginConfig, /api/loginConfig, true",
    "/api/**/loginConfig, /api/40/loginConfig, true",
    "/api/**/loginConfig, /api/41/loginConfig, true",
    "/api/**/loginConfig, /api/40/auth/login, false",
    "/api/**/auth/login, /api/auth/login, true",
    "/api/**/auth/login, /api/40/auth/login, true",
    "/api/**/publicKeys/**, /api/publicKeys/keys, true",
    "/api/**/publicKeys/**, /api/40/publicKeys/jwk, true",
    "/api/**/staticContent/**, /api/staticContent/logo_front.png, true",
    "/api/**/staticContent/**, /api/40/staticContent/logo_front.png, true",
    "/api/**/files/style/external, /api/files/style/external, true",
    "/api/**/files/style/external, /api/40/files/style/external, true",
    "/api/**/locales/ui, /api/locales/ui, true",
    "/api/**/auth/forgotPassword, /api/40/auth/forgotPassword, true",
    "/api/**/auth/passwordReset, /api/40/auth/passwordReset, true",
    "/api/**/auth/registration, /api/40/auth/registration, true",
    "/api/**/auth/invite, /api/40/auth/invite, true",
    "/api/**/auth/updatePassword, /api/40/auth/updatePassword, true",
    "/api/**/authentication/login, /api/40/authentication/login, true",
    "/api/**/account/recovery, /api/40/account/recovery, true",
    "/api/**/account/restore, /api/40/account/restore, true",
    "/api/**/account, /api/40/account, true",
    "/api/**/account, /api/40/account/extra, false",
  })
  void patternMatching(String pattern, String path, boolean expected) {
    AntPathRequestMatcher matcher = new AntPathRequestMatcher(pattern);
    if (expected) {
      assertTrue(matcher.matches(request(path)), path + " should match " + pattern);
    } else {
      assertFalse(matcher.matches(request(path)), path + " should not match " + pattern);
    }
  }

  @Test
  void matchesServletPathPlusPathInfoLikeSpringMatcher() {
    AntPathRequestMatcher matcher = new AntPathRequestMatcher("/api/**/loginConfig");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/40/loginConfig");
    request.setServletPath("/api");
    request.setPathInfo("/40/loginConfig");
    assertTrue(matcher.matches(request));
  }

  @Test
  void caseSensitiveMatching() {
    AntPathRequestMatcher matcher = new AntPathRequestMatcher("/api/ping");
    assertFalse(matcher.matches(request("/API/ping")));
  }

  private static MockHttpServletRequest request(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    request.setPathInfo(null);
    return request;
  }
}
