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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Properties;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.config.H2DhisConfigurationProvider;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.webapi.security.CsrfTokenModeRemovedTest.CsrfKeyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.ContextConfiguration;

/**
 * Checks that the removed {@code http.security.csrf.enabled} setting has no effect: a {@code
 * dhis.conf} that still sets it must not turn on the Spring CSRF filter for the API.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ContextConfiguration(classes = CsrfKeyConfig.class)
class CsrfTokenModeRemovedTest extends AuthenticationApiTestBase {

  static class CsrfKeyConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      H2DhisConfigurationProvider provider = new H2DhisConfigurationProvider();
      Properties properties = new Properties();
      properties.put("http.security.csrf.enabled", "on");
      provider.addProperties(properties);
      return provider;
    }
  }

  @Autowired
  @Qualifier("springSecurityFilterChain")
  private FilterChainProxy springSecurityFilterChain;

  @Test
  void apiFilterChainHasNoCsrfFilter() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/constants");
    SecurityFilterChain chain =
        springSecurityFilterChain.getFilterChains().stream()
            .filter(c -> c.matches(request))
            .findFirst()
            .orElse(null);

    assertNotNull(chain, "no security filter chain matches POST /api/constants");
    assertFalse(
        chain.getFilters().stream().anyMatch(CsrfFilter.class::isInstance),
        "the API filter chain must not contain a CsrfFilter");
  }

  @Test
  void sameOriginPostWithoutTokenSucceeds() throws Exception {
    MockHttpServletResponse response =
        webRequestWithMvcResult(
                post("/api/constants")
                    .header("Origin", "http://localhost")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"csrfModeRemoved\",\"shortName\":\"csrfModeRemoved\",\"value\":1}"))
            .getResponse();

    assertEquals(201, response.getStatus(), response.getContentAsString());
    assertNull(response.getCookie("XSRF-TOKEN"));
    assertFalse(
        response.getHeaders("Set-Cookie").stream().anyMatch(h -> h.startsWith("XSRF-TOKEN")),
        "the response must not set an XSRF-TOKEN cookie");
  }
}
