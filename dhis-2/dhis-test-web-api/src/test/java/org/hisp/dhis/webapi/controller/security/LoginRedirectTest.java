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
package org.hisp.dhis.webapi.controller.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.test.webapi.ControllerWithJwtTokenAuthTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unauthenticated browser requests are redirected to the login page with a context-relative {@code
 * Location}. Behind a TLS-terminating proxy the servlet request is plain http, so an absolute URL
 * would send the browser to {@code http://}; a relative one keeps the scheme and host the client
 * used.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles("oauth2-authorization-server-test")
class LoginRedirectTest extends ControllerWithJwtTokenAuthTestBase {

  /** S256 code_challenge from the RFC 7636 appendix B example. */
  private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

  @Autowired private Dhis2OAuth2ClientService oAuth2ClientService;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void unauthenticatedRequestRedirectsToLoginPageWithRelativeLocation() throws Exception {
    mvc.perform(get("/api/me").accept(MediaType.TEXT_HTML))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "/login/"));
  }

  @Test
  void unauthenticatedAuthorizationRequestRedirectsToLoginPageWithRelativeLocation()
      throws Exception {
    String clientId = "login-redirect-test-client-" + CodeGenerator.generateUid();
    String redirectUri = "https://app.example.org/callback";
    oAuth2ClientService.save(
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode("login-redirect-test-secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(redirectUri)
            .scope("openid")
            .clientSettings(ClientSettings.builder().requireProofKey(true).build())
            .build());

    // The authorization endpoint only reads GET parameters that are in the query string
    mvc.perform(
            get(
                    "/oauth2/authorize?response_type=code&client_id={clientId}"
                        + "&redirect_uri={redirectUri}&scope=openid&state=state"
                        + "&code_challenge={codeChallenge}&code_challenge_method=S256",
                    clientId,
                    redirectUri,
                    CODE_CHALLENGE)
                .accept(MediaType.TEXT_HTML))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "/login/"));
  }
}
