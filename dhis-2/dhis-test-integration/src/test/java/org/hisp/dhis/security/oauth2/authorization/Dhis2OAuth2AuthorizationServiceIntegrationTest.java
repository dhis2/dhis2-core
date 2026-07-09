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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientStore;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link Dhis2OAuth2AuthorizationServiceImpl}. */
@Transactional
public class Dhis2OAuth2AuthorizationServiceIntegrationTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2AuthorizationService authorizationService;

  @Autowired private Dhis2OAuth2AuthorizationStore authorizationStore;

  @Autowired private RegisteredClientRepository clientRepository;

  @Autowired private Dhis2OAuth2ClientStore clientStore;

  private RegisteredClient registeredClient;

  @BeforeEach
  void setUp() {
    // Create and save a test client
    registeredClient =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("auth-test-client-" + UUID.randomUUID())
            .clientName("Auth Test Client")
            .clientSecret("auth-test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("https://auth-test.example.com/callback")
            .scope("read")
            .scope("write")
            .build();

    clientRepository.save(registeredClient);
  }

  @Test
  void testSaveAndFindById() {
    // Given
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute("custom_attribute", "value1")
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization foundAuthorization = authorizationService.findById(authorization.getId());

    // Then
    assertNotNull(foundAuthorization);
    assertEquals(authorization.getId(), foundAuthorization.getId());
    assertEquals(authorization.getPrincipalName(), foundAuthorization.getPrincipalName());
    assertEquals(authorization.getRegisteredClientId(), foundAuthorization.getRegisteredClientId());
    assertEquals("value1", foundAuthorization.getAttribute("custom_attribute"));
  }

  @Test
  void testSaveDerivesNameFromPrincipalName() {
    // The Spring OAuth2Authorization domain object has no name field; the DHIS2
    // entity derives `name` from `principalName` to satisfy the NOT NULL
    // constraint required by BaseIdentifiableObject.
    String uid = CodeGenerator.generateUid();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("principal-with-derived-name")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id(uid)
            .build();

    authorizationService.save(authorization);

    Dhis2OAuth2Authorization entity = authorizationStore.getByUidNoAcl(uid);
    assertNotNull(entity);
    assertEquals("principal-with-derived-name", entity.getName());
  }

  @Test
  void testSaveTruncatesLongPrincipalNameToNameColumnLength() {
    // principal_name is varchar(255); name is varchar(230). Verify truncation.
    String longPrincipal = "p".repeat(255);
    String uid = CodeGenerator.generateUid();
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(longPrincipal)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id(uid)
            .build();

    authorizationService.save(authorization);

    Dhis2OAuth2Authorization entity = authorizationStore.getByUidNoAcl(uid);
    assertNotNull(entity);
    assertEquals(230, entity.getName().length());
    assertEquals(longPrincipal, entity.getPrincipalName());
  }

  @Test
  void testFindByAuthorizationCode() {
    // Given
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(300);

    OAuth2AuthorizationCode authorizationCode =
        new OAuth2AuthorizationCode("code-value", now, expiresAt);

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(authorizationCode)
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);

    OAuth2Authorization foundAuthorization =
        authorizationService.findByToken(
            "code-value", new OAuth2TokenType(OAuth2ParameterNames.CODE));

    // Then
    assertNotNull(foundAuthorization);
    assertEquals(authorization.getId(), foundAuthorization.getId());
    assertNotNull(foundAuthorization.getToken(OAuth2AuthorizationCode.class));
    assertEquals(
        "code-value",
        foundAuthorization.getToken(OAuth2AuthorizationCode.class).getToken().getTokenValue());
  }

  @Test
  void testFindByAccessToken() {
    // Given
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(3600);

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token-value",
            now,
            expiresAt,
            Set.of("read", "write"));

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(accessToken)
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization foundAuthorization =
        authorizationService.findByToken("access-token-value", OAuth2TokenType.ACCESS_TOKEN);

    // Then
    assertNotNull(foundAuthorization);
    assertEquals(authorization.getId(), foundAuthorization.getId());
    assertNotNull(foundAuthorization.getToken(OAuth2AccessToken.class));
    assertEquals(
        "access-token-value",
        foundAuthorization.getToken(OAuth2AccessToken.class).getToken().getTokenValue());
    assertEquals(
        Set.of("read", "write"),
        foundAuthorization.getToken(OAuth2AccessToken.class).getToken().getScopes());
  }

  @Test
  void testFindByRefreshToken() {
    // Given
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(86400);

    OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh-token-value", now, expiresAt);

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(refreshToken)
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization foundAuthorization =
        authorizationService.findByToken("refresh-token-value", OAuth2TokenType.REFRESH_TOKEN);

    // Then
    assertNotNull(foundAuthorization);
    assertEquals(authorization.getId(), foundAuthorization.getId());
    assertNotNull(foundAuthorization.getToken(OAuth2RefreshToken.class));
    assertEquals(
        "refresh-token-value",
        foundAuthorization.getToken(OAuth2RefreshToken.class).getToken().getTokenValue());
  }

  @Test
  void testFindByState() {
    // Given
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute(OAuth2ParameterNames.STATE, "state-value")
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization foundAuthorization =
        authorizationService.findByToken(
            "state-value", new OAuth2TokenType(OAuth2ParameterNames.STATE));

    // Then
    assertNotNull(foundAuthorization);
    assertEquals(authorization.getId(), foundAuthorization.getId());
    assertEquals("state-value", foundAuthorization.getAttribute(OAuth2ParameterNames.STATE));
  }

  @Test
  void testTokenMetadataPersistence() {
    // Given
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(3600);

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "metadata-token-value", now, expiresAt);

    Map<String, Object> metadata =
        Map.of("metadata_key1", "metadata_value1", "metadata_key2", 42, "metadata_key3", true);

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(accessToken, m -> m.putAll(metadata))
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization foundAuthorization =
        authorizationService.findByToken("metadata-token-value", OAuth2TokenType.ACCESS_TOKEN);

    // Then
    assertNotNull(foundAuthorization);
    Map<String, Object> savedMetadata =
        foundAuthorization.getToken(OAuth2AccessToken.class).getMetadata();
    assertEquals("metadata_value1", savedMetadata.get("metadata_key1"));
    assertEquals(42, savedMetadata.get("metadata_key2"));
    assertEquals(true, savedMetadata.get("metadata_key3"));
  }

  @Test
  void testRemoveAuthorization() {
    // Given
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization foundBeforeRemove = authorizationService.findById(authorization.getId());
    authorizationService.remove(authorization);
    OAuth2Authorization foundAfterRemove = authorizationService.findById(authorization.getId());

    // Then
    assertNotNull(foundBeforeRemove);
    assertNull(foundAfterRemove);
  }

  @Test
  void testFindByAuthorizationCodeWithOidcUserPrincipal() {
    // Regression test for the /oauth2/token 500: an external-OIDC login presents an
    // OAuth2AuthenticationToken whose principal is a DhisOidcUser (wrapping a UserDetailsImpl). The
    // persistence layer must store a lean, Spring-native principal so the read side (the token
    // exchange) never trips Spring Security's Jackson deserialization allowlist.
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(300);

    Map<String, Object> claims =
        Map.of(IdTokenClaimNames.SUB, currentUser.getUsername(), "email", "oidc@example.com");
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("oidc-id-token-value")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(currentUser.getUsername())
            .claims(c -> c.putAll(claims))
            .build();
    DhisOidcUser oidcPrincipal =
        new DhisOidcUser(currentUser, claims, IdTokenClaimNames.SUB, idToken);
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(oidcPrincipal, oidcPrincipal.getAuthorities(), "google");

    OAuth2AuthorizationCode authorizationCode =
        new OAuth2AuthorizationCode("oidc-code-value", now, expiresAt);
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(currentUser.getUsername())
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(authorizationCode)
            .attribute(Principal.class.getName(), authentication)
            .id(CodeGenerator.generateUid())
            .build();

    // When
    authorizationService.save(authorization);
    OAuth2Authorization found =
        authorizationService.findByToken(
            "oidc-code-value", new OAuth2TokenType(OAuth2ParameterNames.CODE));

    // Then: the heavyweight OIDC principal was persisted as a lean, Spring-native
    // UsernamePasswordAuthenticationToken (no DHIS2-custom types in the row), carrying the DHIS2
    // username and authorities. getName() is the DHIS2 username — what the token customizer emits
    // as
    // the username claim and what the resource server resolves the user by.
    assertNotNull(found);
    Object principalAttribute = found.getAttribute(Principal.class.getName());
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, principalAttribute);
    UsernamePasswordAuthenticationToken roundTripped =
        (UsernamePasswordAuthenticationToken) principalAttribute;
    assertEquals(currentUser.getUsername(), roundTripped.getName());
    assertTrue(roundTripped.isAuthenticated());
    assertEquals(
        currentUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet()),
        roundTripped.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet()));
  }
}
