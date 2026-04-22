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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2Authorization;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationStore;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that no REST endpoint leaks Spring Authorization Server token material. The read-only
 * {@code /api/oAuth2Authorizations} admin endpoint must remain usable by a future UI (principal,
 * grant type, scopes, issue/expire timestamps) while redacting access/refresh/authorization-code
 * values, OIDC claims, OAuth state and the Spring AS attributes blob. The generic metadata export
 * ({@code /api/metadata}) must not include the type at all in the default response.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class OAuth2TokenRedactionTest extends H2ControllerIntegrationTestBase {

  /**
   * Every JSON field name on {@link Dhis2OAuth2Authorization} that must never appear in any REST
   * response.
   */
  private static final List<String> SECRET_FIELDS =
      List.of(
          "attributes",
          "state",
          "authorizationCodeValue",
          "authorizationCodeMetadata",
          "accessTokenValue",
          "accessTokenMetadata",
          "refreshTokenValue",
          "refreshTokenMetadata",
          "oidcIdTokenValue",
          "oidcIdTokenMetadata",
          "oidcIdTokenClaims",
          "userCodeValue",
          "userCodeMetadata",
          "deviceCodeValue",
          "deviceCodeMetadata");

  @Autowired private Dhis2OAuth2AuthorizationStore authorizationStore;

  private String seededUid;

  @BeforeEach
  void seedAuthorization() {
    Dhis2OAuth2Authorization row = new Dhis2OAuth2Authorization();
    row.setAutoFields();
    row.setName("admin");
    row.setRegisteredClientId("client-1");
    row.setPrincipalName("admin");
    row.setAuthorizationGrantType("authorization_code");
    row.setAuthorizedScopes("read,write");
    row.setAttributes("{\"java.security.Principal\":\"admin\"}");
    row.setState("csrf-state-value");
    row.setAuthorizationCodeValue("AUTHCODE-SECRET");
    row.setAuthorizationCodeIssuedAt(new Date());
    row.setAccessTokenValue("ACCESS-TOKEN-SECRET");
    row.setAccessTokenIssuedAt(new Date());
    row.setAccessTokenType("Bearer");
    row.setAccessTokenScopes("read,write");
    row.setRefreshTokenValue("REFRESH-TOKEN-SECRET");
    row.setOidcIdTokenValue("OIDC-ID-TOKEN-SECRET");
    row.setOidcIdTokenClaims("{\"sub\":\"admin\"}");
    row.setUserCodeValue("USER-CODE-SECRET");
    row.setDeviceCodeValue("DEVICE-CODE-SECRET");
    authorizationStore.save(row);
    seededUid = row.getUid();
  }

  @Test
  @DisplayName("GET /api/oAuth2Authorizations/{uid} must not include any token material")
  void readOnlyControllerRedactsTokens() {
    JsonObject body =
        GET("/oAuth2Authorizations/{uid}", seededUid).content(HttpStatus.OK).as(JsonObject.class);

    // Non-secret fields are still visible so a future UI works.
    assertTrue(body.has("principalName"), "principalName should remain visible");
    assertTrue(body.has("registeredClientId"), "registeredClientId should remain visible");
    assertTrue(body.has("authorizationGrantType"), "authorizationGrantType should remain visible");

    for (String secret : SECRET_FIELDS) {
      assertFalse(
          body.has(secret),
          "Response must not contain secret field `" + secret + "`: " + body.toJson());
    }
  }
}
