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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationService;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.test.webapi.ControllerWithJwtTokenAuthTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end test for FEDERATED OIDC login through the OAuth2 Authorization Server.
 *
 * <p>Simulates the "log in with Google" path where the user is authenticated as a {@link
 * DhisOidcUser} (whose {@code getName()} is the IdP {@code sub}), then exchanges the resulting
 * authorization code at {@code /oauth2/token} and calls the API with the issued DHIS2 JWT. Verifies
 * that the lean-principal persistence makes the issued token carry the DHIS2 <em>username</em> (not
 * the IdP {@code sub}) and that the resource server resolves it to the correct DHIS2 user.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles("oauth2-authorization-server-test")
class FederatedOidcTokenControllerTest extends ControllerWithJwtTokenAuthTestBase {

  @Autowired private Dhis2OAuth2ClientService oAuth2ClientService;
  @Autowired private Dhis2OAuth2AuthorizationService authorizationService;
  @Autowired private AuthorizationServerSettings authorizationServerSettings;
  @Autowired private Dhis2JwtAuthenticationManagerResolver jwtAuthenticationManagerResolver;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    // Make the resource-server resolver validate tokens with the AS's own signing keys.
    jwtAuthenticationManagerResolver.setJwtDecoder(jwtDecoder);
  }

  @Test
  @DisplayName("Federated OIDC login: token carries the DHIS2 username and resolves the user")
  void federatedOidcLoginIssuesTokenThatResolvesToDhis2User() throws Exception {
    // Given: a local DHIS2 user matched by an external OIDC login (external auth).
    User user = createUserWithAuth("feduser", "ALL");
    UserDetails userDetails = userService.createUserDetails(user);

    // And: the Spring principal that the OIDC login produces — a DhisOidcUser whose getName() is
    // the
    // IdP 'sub', not the DHIS2 username.
    Instant now = Instant.now();
    Map<String, Object> claims =
        Map.of(IdTokenClaimNames.SUB, "google-sub-0001", "email", "feduser@dhis2.org");
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("id-token")
            .issuedAt(now)
            .expiresAt(now.plus(5, ChronoUnit.MINUTES))
            .subject("google-sub-0001")
            .claims(c -> c.putAll(claims))
            .build();
    DhisOidcUser oidcPrincipal =
        new DhisOidcUser(userDetails, claims, IdTokenClaimNames.SUB, idToken);
    OAuth2AuthenticationToken oidcAuthentication =
        new OAuth2AuthenticationToken(oidcPrincipal, oidcPrincipal.getAuthorities(), "google");

    // And: a confidential authorization_code client (the native app's server-side equivalent).
    String clientId = "federated-test-client";
    String clientSecret = "federated-secret";
    String redirectUri = "https://app.example.org/callback";
    RegisteredClient client =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(clientSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(redirectUri)
            .scope("openid")
            .scope("profile")
            .scope("username")
            .scope("email")
            .build();
    oAuth2ClientService.save(client);

    // And: the authorization the /oauth2/authorize step would have stored after the OIDC login.
    String issuer = authorizationServerSettings.getIssuer();
    String codeValue = "fed-code-value";
    OAuth2AuthorizationCode authorizationCode =
        new OAuth2AuthorizationCode(codeValue, now, now.plus(5, ChronoUnit.MINUTES));
    OAuth2AuthorizationRequest authorizationRequest =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(issuer + "oauth2/authorize")
            .clientId(clientId)
            .redirectUri(redirectUri)
            .scopes(Set.of("openid", "profile", "username", "email"))
            .state("state-0001")
            .build();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(client)
            .id(CodeGenerator.generateUid())
            .principalName(oidcPrincipal.getName()) // the IdP sub, as Spring AS would set it
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("openid", "profile", "username", "email"))
            .token(authorizationCode)
            .attribute(java.security.Principal.class.getName(), oidcAuthentication)
            .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationRequest)
            .build();
    authorizationService.save(authorization); // <-- lean-principal swap happens here

    // When: the client exchanges the code for a token.
    String basicAuth =
        Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    String tokenResponse =
        mvc.perform(
                post("/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", "authorization_code")
                    .param("code", codeValue)
                    .param("redirect_uri", redirectUri))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonValue.of(tokenResponse).asObject().getString("access_token").string();
    assertNotNull(accessToken);

    // Then: the issued JWT carries the DHIS2 username (NOT the IdP sub) as the username claim. The
    // sub and the DHIS2 username deliberately differ, so this is a real regression guard: if the
    // lean-principal swap were removed, getName() would be the sub and these would fail (or
    // /oauth2/token would 500 on the Jackson allowlist).
    assertNotEquals(
        oidcPrincipal.getName(),
        user.getUsername(),
        "the IdP sub must differ from the DHIS2 username, else this test proves nothing");
    Jwt decoded = jwtDecoder.decode(accessToken);
    assertEquals(user.getUsername(), decoded.getClaimAsString("username"));
    assertNotEquals("google-sub-0001", decoded.getClaimAsString("username"));
    // And the email-scope branch of the token customizer also resolves off the lean username.
    assertEquals(user.getEmail(), decoded.getClaimAsString("email"));

    // And: an API call with that token resolves to the correct DHIS2 user.
    mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(user.getUsername()));
  }
}
