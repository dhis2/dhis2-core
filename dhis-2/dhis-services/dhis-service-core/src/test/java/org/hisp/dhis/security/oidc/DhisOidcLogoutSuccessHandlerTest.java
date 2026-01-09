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

import static org.hisp.dhis.external.conf.ConfigurationKey.HTTP_CLEAR_SITE_DATA;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_OAUTH2_LOGIN_ENABLED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jason P. Pickering
 */
@ExtendWith(MockitoExtension.class)
class DhisOidcLogoutSuccessHandlerTest {

  @Mock private DhisConfigurationProvider config;
  @Mock private DhisOidcProviderRepository dhisOidcProviderRepository;
  @Mock private UserService userService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private DhisOidcLogoutSuccessHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DhisOidcLogoutSuccessHandler(config, dhisOidcProviderRepository, userService);
    when(config.isEnabled(OIDC_OAUTH2_LOGIN_ENABLED)).thenReturn(false);
    handler.init();
  }

  @Test
  void setsClearSiteDataHeaderWhenOn() throws Exception {
    when(config.getProperty(HTTP_CLEAR_SITE_DATA)).thenReturn("on");

    handler.onLogoutSuccess(request, response, null);

    verify(response).setHeader("Clear-Site-Data", "\"cache\", \"storage\"");
  }

  @Test
  void setsClearSiteDataHeaderFromExplicitValue() throws Exception {
    when(config.getProperty(HTTP_CLEAR_SITE_DATA)).thenReturn("\"cookies\"");

    handler.onLogoutSuccess(request, response, null);

    verify(response).setHeader("Clear-Site-Data", "\"cookies\"");
  }

  @Test
  void doesNotSetClearSiteDataHeaderWhenMissing() throws Exception {
    when(config.getProperty(HTTP_CLEAR_SITE_DATA)).thenReturn("");

    handler.onLogoutSuccess(request, response, null);

    verify(response, never()).setHeader(eq("Clear-Site-Data"), anyString());
  }
}
