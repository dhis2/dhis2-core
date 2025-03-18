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
package org.hisp.dhis.security.oauth2.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests for the OAuth2ClientStore implementation. */
@Transactional
class Dhis2OAuth2ClientStoreTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2ClientStore oauth2ClientStore;

  @Test
  void testSaveAndGet() {
    // Create a test client
    Dhis2OAuth2Client client = new Dhis2OAuth2Client();
    client.setAutoFields();
    client.setCreatedBy(getAdminUser());
    client.setLastUpdatedBy(getAdminUser());

    client.setName("Test OAuth2 Client");
    client.setClientId("test-client-id");
    client.setClientSecret("test-client-secret");
    client.setClientIdIssuedAt(new Date());
    client.setClientSecretExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
    client.setClientAuthenticationMethods("client_secret_basic,client_secret_post");
    client.setAuthorizationGrantTypes("authorization_code,refresh_token");
    client.setRedirectUris("https://example.com/callback");
    client.setPostLogoutRedirectUris("https://example.com/logout");
    client.setScopes("openid,profile,email");
    client.setClientSettings("{\"setting1\":\"value1\"}");
    client.setTokenSettings("{\"token_setting\":\"token_value\"}");

    // Save the client
    oauth2ClientStore.save(client);

    // Get the client by UID
    Dhis2OAuth2Client savedClient = oauth2ClientStore.getByUid(client.getUid());
    assertNotNull(savedClient);
    assertEquals("Test OAuth2 Client", savedClient.getName());
    assertEquals("test-client-id", savedClient.getClientId());
    assertEquals("test-client-secret", savedClient.getClientSecret());
    assertEquals(
        "client_secret_basic,client_secret_post", savedClient.getClientAuthenticationMethods());
    assertEquals("authorization_code,refresh_token", savedClient.getAuthorizationGrantTypes());
    assertEquals("https://example.com/callback", savedClient.getRedirectUris());
    assertEquals("https://example.com/logout", savedClient.getPostLogoutRedirectUris());
    assertEquals("openid,profile,email", savedClient.getScopes());
    assertEquals("{\"setting1\":\"value1\"}", savedClient.getClientSettings());
    assertEquals("{\"token_setting\":\"token_value\"}", savedClient.getTokenSettings());
  }

  @Test
  void testGetByClientId() {
    // Create a test client
    Dhis2OAuth2Client client = new Dhis2OAuth2Client();
    client.setAutoFields();
    client.setCreatedBy(getAdminUser());
    client.setLastUpdatedBy(getAdminUser());
    client.setName("Client By ID Test");
    client.setClientId("specific-client-id");
    client.setClientSecret("test-client-secret");
    client.setClientIdIssuedAt(new Date());
    client.setClientSecretExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
    client.setClientAuthenticationMethods("client_secret_basic");
    client.setAuthorizationGrantTypes("authorization_code");
    client.setRedirectUris("https://example.org/callback");
    client.setScopes("openid");

    // Save the client
    oauth2ClientStore.save(client);

    // Get the client by client ID
    Dhis2OAuth2Client foundClient = oauth2ClientStore.getByClientId("specific-client-id");
    assertNotNull(foundClient);
    assertEquals("Client By ID Test", foundClient.getName());
    assertEquals("specific-client-id", foundClient.getClientId());

    // Try to get a non-existent client ID
    Dhis2OAuth2Client nonExistentClient = oauth2ClientStore.getByClientId("non-existent-id");
    assertNull(nonExistentClient);
  }
}
