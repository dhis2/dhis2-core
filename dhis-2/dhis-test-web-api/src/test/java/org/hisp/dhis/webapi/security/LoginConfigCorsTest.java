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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.filter.LoginConfigCorsFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Web test for CORS on the security-ignored {@code /api/**&#47;loginConfig} endpoint (DHIS2-21909):
 * a whitelisted browser origin must receive CORS headers even though the Spring Security filter
 * chain (and its CorsFilter) is skipped for this path.
 *
 * <p>The production servlet registration in {@code DhisWebApiWebAppInitializer#setupServlets} is
 * not active in MockMvc, so the filter is added explicitly with its production {@code /api/*}
 * mapping; the loginConfig path gate lives inside the filter itself. The end-to-end registration
 * order is covered by manual verification against a running server.
 *
 * @author Morten Svanæs
 */
@Transactional
class LoginConfigCorsTest extends H2ControllerIntegrationTestBase {

  private static final String WHITELISTED_ORIGIN = "http://localhost:3000";

  @Autowired private LoginConfigCorsFilter loginConfigCorsFilter;

  @Autowired private ConfigurationService configurationService;

  @BeforeEach
  void setUpCors() {
    mvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilter(loginConfigCorsFilter, "/api/*")
            .build();
    configurationService.setCorsWhitelist(Set.of(WHITELISTED_ORIGIN));
  }

  @Test
  void preflightOnLoginConfigReturnsCorsHeaders() throws Exception {
    mvc.perform(
            options("/api/loginConfig")
                .header("Origin", WHITELISTED_ORIGIN)
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Access-Control-Allow-Origin", WHITELISTED_ORIGIN))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
        .andExpect(header().string("Access-Control-Allow-Methods", "GET"));
  }

  @Test
  void getLoginConfigWithWhitelistedOriginReturnsBodyAndCorsHeaders() throws Exception {
    mvc.perform(get("/api/loginConfig").header("Origin", WHITELISTED_ORIGIN))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", WHITELISTED_ORIGIN))
        .andExpect(jsonPath("$.apiVersion").exists());
  }

  @Test
  void getLoginConfigWithNonWhitelistedOriginReturnsNoCorsHeaders() throws Exception {
    mvc.perform(get("/api/loginConfig").header("Origin", "http://evil.example"))
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void otherApiPathIsNotTouchedByLoginConfigCorsFilter() throws Exception {
    mvc.perform(get("/api/me").header("Origin", WHITELISTED_ORIGIN))
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }
}
