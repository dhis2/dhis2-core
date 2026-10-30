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
package org.hisp.dhis.security.oidc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

/**
 * Unit tests for {@link DhisOidcLogoutSuccessHandler} verifying that the redirect_uri parameter is
 * validated against the device enrollment redirect allowlist from system settings.
 */
@ExtendWith(MockitoExtension.class)
class DhisOidcLogoutSuccessHandlerTest {

  @Mock private DhisConfigurationProvider config;
  @Mock private DhisOidcProviderRepository dhisOidcProviderRepository;
  @Mock private UserService userService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private SystemSettings systemSettings;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private Authentication authentication;

  private DhisOidcLogoutSuccessHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new DhisOidcLogoutSuccessHandler(
            config, dhisOidcProviderRepository, userService, systemSettingsService);
    handler.init();
  }

  private void stubAllowlist(String allowlist) {
    when(systemSettingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getDeviceEnrollmentRedirectAllowlist()).thenReturn(allowlist);
  }

  @Test
  @DisplayName("Allowed redirect URI results in redirect to that URI")
  void testAllowedRedirectUri() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("dhis2oauth://oauth");
    stubAllowlist("dhis2oauth://oauth");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response).sendRedirect("dhis2oauth://oauth");
  }

  @Test
  @DisplayName("Allowed redirect URI with wildcard glob pattern matches")
  void testAllowedRedirectUriWithGlob() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("dhis2oauth://oauth/callback");
    stubAllowlist("dhis2oauth://oauth/*");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response).sendRedirect("dhis2oauth://oauth/callback");
  }

  @Test
  @DisplayName("Disallowed redirect URI does not redirect to that URI")
  void testDisallowedRedirectUri() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("https://evil.com");
    stubAllowlist("dhis2oauth://oauth");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response, never()).sendRedirect("https://evil.com");
  }

  @Test
  @DisplayName("No redirect_uri parameter does not consult allowlist")
  void testNoRedirectUriParameter() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn(null);

    handler.onLogoutSuccess(request, response, authentication);

    verifyNoInteractions(systemSettingsService);
  }

  @Test
  @DisplayName("Empty redirect_uri parameter does not consult allowlist")
  void testBlankRedirectUriParameter() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("");

    handler.onLogoutSuccess(request, response, authentication);

    verifyNoInteractions(systemSettingsService);
  }

  @Test
  @DisplayName("Multiple allowlist entries - matching second entry succeeds")
  void testMultipleAllowlistEntries() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("myapp://callback");
    stubAllowlist("dhis2oauth://oauth, myapp://callback");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response).sendRedirect("myapp://callback");
  }

  @Test
  @DisplayName("Redirect URI not matching any allowlist entry is rejected")
  void testRedirectUriNotMatchingAnyEntry() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("https://attacker.com/steal");
    stubAllowlist("dhis2oauth://oauth, myapp://callback");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response, never()).sendRedirect("https://attacker.com/steal");
  }
}
