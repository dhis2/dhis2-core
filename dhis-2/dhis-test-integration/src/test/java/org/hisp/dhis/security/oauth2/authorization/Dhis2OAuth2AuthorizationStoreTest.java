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
package org.hisp.dhis.security.oauth2.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests for the OAuth2AuthorizationStore implementation. */
@Transactional
class Dhis2OAuth2AuthorizationStoreTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2AuthorizationStore oauth2AuthorizationStore;

  @Test
  void testSaveAndGetByUid() {
    // Create a test authorization
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setAutoFields();
    authorization.setCreatedBy(getAdminUser());
    authorization.setLastUpdatedBy(getAdminUser());

    authorization.setName("Test OAuth2 Authorization");
    authorization.setRegisteredClientId("test-client-id");
    authorization.setPrincipalName("test-user");
    authorization.setAuthorizationGrantType("authorization_code");
    authorization.setAuthorizedScopes("openid,profile");
    authorization.setState("test-state");
    authorization.setAttributes("{\"attr\":\"value\"}");

    // Save the authorization
    oauth2AuthorizationStore.save(authorization);

    // Get the authorization by UID
    Dhis2OAuth2Authorization savedAuth = oauth2AuthorizationStore.getByUid(authorization.getUid());
    assertNotNull(savedAuth);
    assertEquals("Test OAuth2 Authorization", savedAuth.getName());
    assertEquals("test-client-id", savedAuth.getRegisteredClientId());
    assertEquals("test-user", savedAuth.getPrincipalName());
    assertEquals("authorization_code", savedAuth.getAuthorizationGrantType());
    assertEquals("openid,profile", savedAuth.getAuthorizedScopes());
    assertEquals("test-state", savedAuth.getState());
    assertEquals("{\"attr\":\"value\"}", savedAuth.getAttributes());
  }

  @Test
  void testGetByState() {
    // Create a test authorization with state
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setAutoFields();
    authorization.setCreatedBy(getAdminUser());
    authorization.setLastUpdatedBy(getAdminUser());
    authorization.setName("State Test Auth");
    authorization.setRegisteredClientId("client-1");
    authorization.setPrincipalName("user-1");
    authorization.setAuthorizationGrantType("authorization_code");
    authorization.setState("specific-state-value");

    // Save the authorization
    oauth2AuthorizationStore.save(authorization);

    // Get the authorization by state
    Dhis2OAuth2Authorization foundAuth =
        oauth2AuthorizationStore.getByState("specific-state-value");
    assertNotNull(foundAuth);
    assertEquals("State Test Auth", foundAuth.getName());
    assertEquals("specific-state-value", foundAuth.getState());

    // Try to get a non-existent state
    Dhis2OAuth2Authorization nonExistentAuth =
        oauth2AuthorizationStore.getByState("non-existent-state");
    assertNull(nonExistentAuth);
  }

  @Test
  void testGetByAuthorizationCode() {
    // Create a test authorization with authorization code
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setAutoFields();
    authorization.setCreatedBy(getAdminUser());
    authorization.setLastUpdatedBy(getAdminUser());
    authorization.setName("Auth Code Test");
    authorization.setRegisteredClientId("client-2");
    authorization.setPrincipalName("user-2");
    authorization.setAuthorizationGrantType("authorization_code");
    authorization.setAuthorizationCodeValue("test-auth-code");
    authorization.setAuthorizationCodeIssuedAt(new Date());
    authorization.setAuthorizationCodeExpiresAt(Date.from(Instant.now().plusSeconds(600)));

    // Save the authorization
    oauth2AuthorizationStore.save(authorization);

    // Get the authorization by authorization code
    Dhis2OAuth2Authorization foundAuth =
        oauth2AuthorizationStore.getByAuthorizationCode("test-auth-code");
    assertNotNull(foundAuth);
    assertEquals("Auth Code Test", foundAuth.getName());
    assertEquals("test-auth-code", foundAuth.getAuthorizationCodeValue());
  }

  @Test
  void testGetByAccessToken() {
    // Create a test authorization with access token
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setAutoFields();
    authorization.setCreatedBy(getAdminUser());
    authorization.setLastUpdatedBy(getAdminUser());
    authorization.setName("Access Token Test");
    authorization.setRegisteredClientId("client-3");
    authorization.setPrincipalName("user-3");
    authorization.setAuthorizationGrantType("authorization_code");
    authorization.setAccessTokenValue("test-access-token");
    authorization.setAccessTokenIssuedAt(new Date());
    authorization.setAccessTokenExpiresAt(Date.from(Instant.now().plusSeconds(3600)));

    // Save the authorization
    oauth2AuthorizationStore.save(authorization);

    // Get the authorization by access token
    Dhis2OAuth2Authorization foundAuth =
        oauth2AuthorizationStore.getByAccessToken("test-access-token");
    assertNotNull(foundAuth);
    assertEquals("Access Token Test", foundAuth.getName());
    assertEquals("test-access-token", foundAuth.getAccessTokenValue());
  }

  @Test
  void testGetByRefreshToken() {
    // Create a test authorization with refresh token
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setAutoFields();
    authorization.setCreatedBy(getAdminUser());
    authorization.setLastUpdatedBy(getAdminUser());
    authorization.setName("Refresh Token Test");
    authorization.setRegisteredClientId("client-4");
    authorization.setPrincipalName("user-4");
    authorization.setAuthorizationGrantType("authorization_code");
    authorization.setRefreshTokenValue("test-refresh-token");
    authorization.setRefreshTokenIssuedAt(new Date());
    authorization.setRefreshTokenExpiresAt(Date.from(Instant.now().plusSeconds(86400)));

    // Save the authorization
    oauth2AuthorizationStore.save(authorization);

    // Get the authorization by refresh token
    Dhis2OAuth2Authorization foundAuth =
        oauth2AuthorizationStore.getByRefreshToken("test-refresh-token");
    assertNotNull(foundAuth);
    assertEquals("Refresh Token Test", foundAuth.getName());
    assertEquals("test-refresh-token", foundAuth.getRefreshTokenValue());
  }

  @Test
  void testGetByToken() {
    // Create test authorizations with different token types
    Dhis2OAuth2Authorization auth1 = new Dhis2OAuth2Authorization();
    auth1.setAutoFields();
    auth1.setCreatedBy(getAdminUser());
    auth1.setLastUpdatedBy(getAdminUser());
    auth1.setName("Token Test 1");
    auth1.setRegisteredClientId("client-5");
    auth1.setPrincipalName("user-5");
    auth1.setAuthorizationGrantType("authorization_code");
    auth1.setAccessTokenValue("multi-token-test-1");

    Dhis2OAuth2Authorization auth2 = new Dhis2OAuth2Authorization();
    auth2.setAutoFields();
    auth2.setCreatedBy(getAdminUser());
    auth2.setLastUpdatedBy(getAdminUser());
    auth2.setName("Token Test 2");
    auth2.setRegisteredClientId("client-6");
    auth2.setPrincipalName("user-6");
    auth2.setAuthorizationGrantType("authorization_code");
    auth2.setRefreshTokenValue("multi-token-test-2");

    // Save the authorizations
    oauth2AuthorizationStore.save(auth1);
    oauth2AuthorizationStore.save(auth2);

    // Get the authorizations by token
    Dhis2OAuth2Authorization foundAuth1 = oauth2AuthorizationStore.getByToken("multi-token-test-1");
    assertNotNull(foundAuth1);
    assertEquals("Token Test 1", foundAuth1.getName());

    Dhis2OAuth2Authorization foundAuth2 = oauth2AuthorizationStore.getByToken("multi-token-test-2");
    assertNotNull(foundAuth2);
    assertEquals("Token Test 2", foundAuth2.getName());

    // Try to get a non-existent token
    Dhis2OAuth2Authorization nonExistentAuth =
        oauth2AuthorizationStore.getByToken("non-existent-token");
    assertNull(nonExistentAuth);
  }
}
