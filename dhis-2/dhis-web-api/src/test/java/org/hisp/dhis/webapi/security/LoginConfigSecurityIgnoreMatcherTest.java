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
package org.hisp.dhis.webapi.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Guards the DHIS2-21909 security-ignore pattern for loginConfig.
 *
 * <p>Production config uses web.ignoring() with an Ant mid-pattern under /api so the Spring
 * Security filter chain (including Basic auth) does not run for login bootstrap.
 *
 * @author Morten Svanæs
 */
class LoginConfigSecurityIgnoreMatcherTest {

  private static final AntPathRequestMatcher LOGIN_CONFIG =
      new AntPathRequestMatcher("/api/**/loginConfig");

  @ParameterizedTest
  @ValueSource(strings = {"/api/loginConfig", "/api/40/loginConfig", "/api/41/loginConfig"})
  void matchesVersionedAndUnversionedLoginConfig(String path) {
    assertTrue(LOGIN_CONFIG.matches(request(path)), path);
  }

  @Test
  void doesNotMatchOtherAuthEndpoints() {
    assertFalse(LOGIN_CONFIG.matches(request("/api/auth/login")));
    assertFalse(LOGIN_CONFIG.matches(request("/api/40/auth/login")));
    assertFalse(LOGIN_CONFIG.matches(request("/api/me")));
  }

  private static MockHttpServletRequest request(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    request.setPathInfo(null);
    return request;
  }
}
