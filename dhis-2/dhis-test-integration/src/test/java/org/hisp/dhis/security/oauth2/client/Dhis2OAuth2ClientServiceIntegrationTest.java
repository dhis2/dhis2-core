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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link Dhis2OAuth2ClientServiceImpl}. */
@Slf4j
@Transactional
public class Dhis2OAuth2ClientServiceIntegrationTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2ClientService clientRepository;
  @Autowired private Dhis2OAuth2ClientStore clientStore;

  @Test
  void testSaveAndFindById() {
    // Given
    RegisteredClient registeredClient = createTestClient();

    // When
    clientRepository.save(registeredClient);
    RegisteredClient foundClient = clientRepository.findById(registeredClient.getClientId());

    // Then
    assertNotNull(foundClient);
    assertEquals(registeredClient.getId(), foundClient.getId());
    assertEquals(registeredClient.getClientId(), foundClient.getClientId());
    assertEquals(registeredClient.getClientName(), foundClient.getClientName());
    assertEquals(registeredClient.getClientSecret(), foundClient.getClientSecret());
  }

  @Test
  void testFindByClientId() {
    // Given
    RegisteredClient registeredClient = createTestClient();
    clientRepository.save(registeredClient);

    // When
    RegisteredClient foundClient = clientRepository.findByClientId(registeredClient.getClientId());

    // Then
    assertNotNull(foundClient);
    assertEquals(registeredClient.getId(), foundClient.getId());
    assertEquals(registeredClient.getClientId(), foundClient.getClientId());
  }

  @Test
  void testComplexAttributesConversion() {
    // Given
    RegisteredClient registeredClient = createTestClientWithComplexAttributes();
    clientRepository.save(registeredClient);

    // When
    RegisteredClient foundClient = clientRepository.findById(registeredClient.getClientId());

    // Then
    assertNotNull(foundClient);

    // Verify client settings
    assertEquals(
        registeredClient.getClientSettings().isRequireProofKey(),
        foundClient.getClientSettings().isRequireProofKey());
    assertEquals(
        registeredClient.getClientSettings().isRequireAuthorizationConsent(),
        foundClient.getClientSettings().isRequireAuthorizationConsent());

    // Verify token settings
    assertEquals(
        registeredClient.getTokenSettings().getAccessTokenTimeToLive(),
        foundClient.getTokenSettings().getAccessTokenTimeToLive());
    assertEquals(
        registeredClient.getTokenSettings().getRefreshTokenTimeToLive(),
        foundClient.getTokenSettings().getRefreshTokenTimeToLive());
  }

  @Test
  void testMultiValuedAttributesConversion() {
    // Given
    RegisteredClient registeredClient = createTestClientWithMultiValuedAttributes();
    clientRepository.save(registeredClient);

    // When
    RegisteredClient foundClient = clientRepository.findById(registeredClient.getClientId());

    // Then
    assertNotNull(foundClient);

    // Check client authentication methods
    assertTrue(
        foundClient
            .getClientAuthenticationMethods()
            .contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
    assertTrue(
        foundClient
            .getClientAuthenticationMethods()
            .contains(ClientAuthenticationMethod.CLIENT_SECRET_POST));
    assertEquals(2, foundClient.getClientAuthenticationMethods().size());

    // Check authorization grant types
    assertTrue(
        foundClient
            .getAuthorizationGrantTypes()
            .contains(AuthorizationGrantType.AUTHORIZATION_CODE));
    assertTrue(
        foundClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
    assertTrue(
        foundClient
            .getAuthorizationGrantTypes()
            .contains(AuthorizationGrantType.CLIENT_CREDENTIALS));
    assertEquals(3, foundClient.getAuthorizationGrantTypes().size());

    // Check scopes
    assertTrue(foundClient.getScopes().contains("read"));
    assertTrue(foundClient.getScopes().contains("write"));
    assertTrue(foundClient.getScopes().contains("openid"));
    assertEquals(3, foundClient.getScopes().size());

    // Check redirect URIs
    assertTrue(foundClient.getRedirectUris().contains("https://client1.example.com/callback"));
    assertTrue(foundClient.getRedirectUris().contains("https://client1.example.com/callback2"));
    assertEquals(2, foundClient.getRedirectUris().size());
  }

  @Test
  void testTimestampConversion() {
    // Given
    Instant now = Instant.now();
    Instant future = now.plusSeconds(3600);

    RegisteredClient registeredClient =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("test-timestamps-client")
            .clientSecret("secret")
            .clientName("Test Timestamps Client")
            .clientIdIssuedAt(now)
            .clientSecretExpiresAt(future)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://example.com/callback")
            .scope("read")
            .build();

    // When
    clientRepository.save(registeredClient);
    RegisteredClient foundClient = clientRepository.findById(registeredClient.getClientId());

    // Then
    assertNotNull(foundClient);
    assertEquals(now.getEpochSecond(), foundClient.getClientIdIssuedAt().getEpochSecond());
    assertEquals(future.getEpochSecond(), foundClient.getClientSecretExpiresAt().getEpochSecond());
  }

  @Test
  void testEntityToDomainConversion() {
    // Given
    Dhis2OAuth2Client entity = new Dhis2OAuth2Client();
    entity.setUid(CodeGenerator.generateUid());
    entity.setName("Direct Entity Client");
    entity.setClientId("direct-entity-client");
    entity.setClientSecret("direct-entity-secret");
    entity.setClientAuthenticationMethods(
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue());
    entity.setAuthorizationGrantTypes(
        AuthorizationGrantType.AUTHORIZATION_CODE.getValue()
            + ","
            + AuthorizationGrantType.REFRESH_TOKEN.getValue());
    entity.setRedirectUris("https://direct.example.com/callback");
    entity.setScopes("read,write");
    entity.setClientSettings(
        "{\"@class\": \"java.util.Collections$UnmodifiableMap\",\"settings.client.require-authorization-consent\":true}");
    entity.setTokenSettings(
        "{\"@class\": \"java.util.Collections$UnmodifiableMap\",\"settings.token.access-token-time-to-live\":3600,\"settings.token.refresh-token-time-to-live\":7200}");

    // When
    clientStore.save(entity);
    RegisteredClient foundClient = clientRepository.findByClientId("direct-entity-client");

    // Then
    assertNotNull(foundClient);
    assertEquals(entity.getUid(), foundClient.getId());
    assertEquals(entity.getName(), foundClient.getClientName());
    assertEquals(entity.getClientId(), foundClient.getClientId());
    assertEquals(entity.getClientSecret(), foundClient.getClientSecret());

    assertTrue(
        foundClient
            .getClientAuthenticationMethods()
            .contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
    assertTrue(
        foundClient
            .getAuthorizationGrantTypes()
            .contains(AuthorizationGrantType.AUTHORIZATION_CODE));
    assertTrue(
        foundClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
    assertTrue(foundClient.getScopes().contains("read"));
    assertTrue(foundClient.getScopes().contains("write"));
    assertTrue(foundClient.getRedirectUris().contains("https://direct.example.com/callback"));

    ClientSettings clientSettings = foundClient.getClientSettings();

    assertTrue(clientSettings.isRequireAuthorizationConsent());
  }

  /** Creates a test client with basic attributes for testing. */
  private RegisteredClient createTestClient() {
    return RegisteredClient.withId(CodeGenerator.generateUid())
        .clientId("test-client-" + UUID.randomUUID())
        .clientName("Test Client")
        .clientSecret("test-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://example.com/callback")
        .scope("read")
        .build();
  }

  /** Creates a test client with complex attributes for testing. */
  private RegisteredClient createTestClientWithComplexAttributes() {
    return RegisteredClient.withId(CodeGenerator.generateUid())
        .clientId("complex-client-" + UUID.randomUUID())
        .clientName("Complex Client")
        .clientSecret("complex-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://complex.example.com/callback")
        .scope("read")
        .clientSettings(
            ClientSettings.builder()
                .requireProofKey(true)
                .requireAuthorizationConsent(true)
                .build())
        .tokenSettings(
            TokenSettings.builder()
                .accessTokenTimeToLive(java.time.Duration.ofHours(1))
                .refreshTokenTimeToLive(java.time.Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .build())
        .build();
  }

  /** Creates a test client with multi-valued attributes for testing. */
  private RegisteredClient createTestClientWithMultiValuedAttributes() {
    return RegisteredClient.withId(CodeGenerator.generateUid())
        .clientId("multi-client-" + UUID.randomUUID())
        .clientName("Multi-Valued Client")
        .clientSecret("multi-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .redirectUri("https://client1.example.com/callback")
        .redirectUri("https://client1.example.com/callback2")
        .scope("read")
        .scope("write")
        .scope("openid")
        .build();
  }
}
