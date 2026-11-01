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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimestampType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests for the OAuth2AuthorizationStore implementation. */
@Transactional
class Dhis2OAuth2AuthorizationStoreTest extends PostgresIntegrationTestBase {

  private static final List<String> TOKENS =
      List.of(
          "authorizationCode",
          "accessToken",
          "refreshToken",
          "oidcIdToken",
          "userCode",
          "deviceCode");

  private static final Map<String, String> TOKEN_COLUMNS =
      Map.of(
          "authorizationCode", "authorization_code",
          "accessToken", "access_token",
          "refreshToken", "refresh_token",
          "oidcIdToken", "oidc_id_token",
          "userCode", "user_code",
          "deviceCode", "device_code");

  @Autowired private Dhis2OAuth2AuthorizationStore oauth2AuthorizationStore;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

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

  @Test
  void shouldMapAuthorizationToOauth2AuthorizationTable() {
    AbstractEntityPersister persister = persister();

    assertEquals("oauth2_authorization", persister.getTableName());
    assertArrayEquals(new String[] {"id"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());

    Map<String, String> columns = new LinkedHashMap<>();
    columns.put("uid", "uid");
    columns.put("code", "code");
    columns.put("created", "created");
    columns.put("lastUpdated", "lastupdated");
    columns.put("lastUpdatedBy", "lastupdatedby");
    columns.put("createdBy", "createdby");
    columns.put("name", "name");
    columns.put("registeredClientId", "registered_client_id");
    columns.put("principalName", "principal_name");
    columns.put("authorizationGrantType", "authorization_grant_type");
    columns.put("authorizedScopes", "authorized_scopes");
    columns.put("attributes", "attributes");
    columns.put("state", "state");
    columns.put("accessTokenType", "access_token_type");
    columns.put("accessTokenScopes", "access_token_scopes");
    columns.put("oidcIdTokenClaims", "oidc_id_token_claims");
    for (String token : TOKENS) {
      String column = TOKEN_COLUMNS.get(token);
      columns.put(token + "Value", column + "_value");
      columns.put(token + "IssuedAt", column + "_issued_at");
      columns.put(token + "ExpiresAt", column + "_expires_at");
      columns.put(token + "Metadata", column + "_metadata");
    }
    assertEquals(
        columns.keySet().stream().sorted().toList(),
        List.of(persister.getPropertyNames()).stream().sorted().toList());
    columns.forEach(
        (property, column) ->
            assertEquals(
                column,
                persister.getPropertyColumnNames(property)[0].toLowerCase(),
                property + " column"));
  }

  @Test
  void shouldMapAuthorizationPropertyTypesAndNullability() {
    AbstractEntityPersister persister = persister();

    List<String> notNull =
        List.of(
            "uid",
            "created",
            "lastUpdated",
            "name",
            "registeredClientId",
            "principalName",
            "authorizationGrantType");
    for (String property : persister.getPropertyNames()) {
      assertEquals(!notNull.contains(property), isNullable(property), property + " nullability");
    }
    for (String property :
        List.of(
            "uid",
            "code",
            "name",
            "registeredClientId",
            "principalName",
            "authorizationGrantType",
            "authorizedScopes",
            "state",
            "accessTokenType",
            "accessTokenScopes")) {
      assertInstanceOf(StringType.class, persister.getPropertyType(property), property);
    }
    assertInstanceOf(TextType.class, persister.getPropertyType("attributes"));
    assertInstanceOf(TextType.class, persister.getPropertyType("oidcIdTokenClaims"));
    assertInstanceOf(TimestampType.class, persister.getPropertyType("created"));
    assertInstanceOf(TimestampType.class, persister.getPropertyType("lastUpdated"));
    for (String token : TOKENS) {
      assertInstanceOf(StringType.class, persister.getPropertyType(token + "Value"), token);
      assertInstanceOf(TimestampType.class, persister.getPropertyType(token + "IssuedAt"), token);
      assertInstanceOf(TimestampType.class, persister.getPropertyType(token + "ExpiresAt"), token);
      assertInstanceOf(TextType.class, persister.getPropertyType(token + "Metadata"), token);
    }
    for (String property : List.of("lastUpdatedBy", "createdBy")) {
      ManyToOneType type =
          assertInstanceOf(ManyToOneType.class, persister.getPropertyType(property));
      assertEquals(User.class.getName(), type.getAssociatedEntityName(), property);
      assertFalse(type.isEager(null), property + " is eager");
    }
    assertFalse(
        persister
            .getPropertyUpdateability()[
            persister.getEntityMetamodel().getPropertyIndex("created")]);
  }

  @Test
  void shouldCacheAuthorizationReadWrite() {
    AbstractEntityPersister persister = persister();

    assertTrue(persister.canWriteToCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void shouldWriteAndReadEveryAuthorizationColumn() {
    User admin = getAdminUser();
    String longMetadata = "{\"metadata.token.claims\":\"" + "x".repeat(5000) + "\"}";
    Dhis2OAuth2Authorization authorization = newAuthorization("round-trip-principal");
    authorization.setCode("AUTH_CODE");
    authorization.setCreatedBy(admin);
    authorization.setLastUpdatedBy(admin);
    authorization.setAuthorizedScopes("openid,profile");
    authorization.setAttributes("{\"java.security.Principal\":\"admin\"}");
    authorization.setState("round-trip-state");
    authorization.setAccessTokenType("Bearer");
    authorization.setAccessTokenScopes("openid");
    authorization.setOidcIdTokenClaims("{\"sub\":\"admin\"}");
    for (int i = 0; i < TOKENS.size(); i++) {
      setToken(authorization, TOKENS.get(i), i);
    }
    authorization.setDeviceCodeMetadata(longMetadata);
    oauth2AuthorizationStore.save(authorization);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select * from oauth2_authorization where id = ?", authorization.getId());
    assertEquals(authorization.getUid(), row.get("uid"));
    assertEquals("AUTH_CODE", row.get("code"));
    assertEquals("round-trip-principal", row.get("name"));
    assertEquals("round-trip-client", row.get("registered_client_id"));
    assertEquals("round-trip-principal", row.get("principal_name"));
    assertEquals("authorization_code", row.get("authorization_grant_type"));
    assertEquals("openid,profile", row.get("authorized_scopes"));
    assertEquals("{\"java.security.Principal\":\"admin\"}", row.get("attributes"));
    assertEquals("round-trip-state", row.get("state"));
    assertEquals("Bearer", row.get("access_token_type"));
    assertEquals("openid", row.get("access_token_scopes"));
    assertEquals("{\"sub\":\"admin\"}", row.get("oidc_id_token_claims"));
    assertEquals(admin.getId(), ((Number) row.get("createdby")).longValue());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());
    for (int i = 0; i < TOKENS.size(); i++) {
      String column = TOKEN_COLUMNS.get(TOKENS.get(i));
      String expectedMetadata =
          "deviceCode".equals(TOKENS.get(i)) ? longMetadata : "{\"token\":" + i + "}";
      assertEquals("value-" + i, row.get(column + "_value"), column);
      assertEquals(issuedAt(i).getTime(), ((Date) row.get(column + "_issued_at")).getTime());
      assertEquals(expiresAt(i).getTime(), ((Date) row.get(column + "_expires_at")).getTime());
      assertEquals(expectedMetadata, row.get(column + "_metadata"), column);
    }

    Dhis2OAuth2Authorization loaded = reload(authorization);
    assertEquals("AUTH_CODE", loaded.getCode());
    assertEquals("round-trip-principal", loaded.getName());
    assertEquals("round-trip-client", loaded.getRegisteredClientId());
    assertEquals("round-trip-principal", loaded.getPrincipalName());
    assertEquals("authorization_code", loaded.getAuthorizationGrantType());
    assertEquals("openid,profile", loaded.getAuthorizedScopes());
    assertEquals("{\"java.security.Principal\":\"admin\"}", loaded.getAttributes());
    assertEquals("round-trip-state", loaded.getState());
    assertEquals("Bearer", loaded.getAccessTokenType());
    assertEquals("openid", loaded.getAccessTokenScopes());
    assertEquals("{\"sub\":\"admin\"}", loaded.getOidcIdTokenClaims());
    assertEquals(admin.getUid(), loaded.getCreatedBy().getUid());
    assertEquals(admin.getUid(), loaded.getLastUpdatedBy().getUid());
    assertEquals("value-0", loaded.getAuthorizationCodeValue());
    assertEquals(issuedAt(0).getTime(), loaded.getAuthorizationCodeIssuedAt().getTime());
    assertEquals(expiresAt(0).getTime(), loaded.getAuthorizationCodeExpiresAt().getTime());
    assertEquals("{\"token\":0}", loaded.getAuthorizationCodeMetadata());
    assertEquals("value-1", loaded.getAccessTokenValue());
    assertEquals(issuedAt(1).getTime(), loaded.getAccessTokenIssuedAt().getTime());
    assertEquals(expiresAt(1).getTime(), loaded.getAccessTokenExpiresAt().getTime());
    assertEquals("{\"token\":1}", loaded.getAccessTokenMetadata());
    assertEquals("value-2", loaded.getRefreshTokenValue());
    assertEquals(issuedAt(2).getTime(), loaded.getRefreshTokenIssuedAt().getTime());
    assertEquals(expiresAt(2).getTime(), loaded.getRefreshTokenExpiresAt().getTime());
    assertEquals("{\"token\":2}", loaded.getRefreshTokenMetadata());
    assertEquals("value-3", loaded.getOidcIdTokenValue());
    assertEquals(issuedAt(3).getTime(), loaded.getOidcIdTokenIssuedAt().getTime());
    assertEquals(expiresAt(3).getTime(), loaded.getOidcIdTokenExpiresAt().getTime());
    assertEquals("{\"token\":3}", loaded.getOidcIdTokenMetadata());
    assertEquals("value-4", loaded.getUserCodeValue());
    assertEquals(issuedAt(4).getTime(), loaded.getUserCodeIssuedAt().getTime());
    assertEquals(expiresAt(4).getTime(), loaded.getUserCodeExpiresAt().getTime());
    assertEquals("{\"token\":4}", loaded.getUserCodeMetadata());
    assertEquals("value-5", loaded.getDeviceCodeValue());
    assertEquals(issuedAt(5).getTime(), loaded.getDeviceCodeIssuedAt().getTime());
    assertEquals(expiresAt(5).getTime(), loaded.getDeviceCodeExpiresAt().getTime());
    assertEquals(longMetadata, loaded.getDeviceCodeMetadata());
  }

  @Test
  void shouldDeleteAuthorizationByUidWithEntityNameQuery() {
    Dhis2OAuth2Authorization kept = newAuthorization("kept-principal");
    Dhis2OAuth2Authorization deleted = newAuthorization("deleted-principal");
    oauth2AuthorizationStore.save(kept);
    oauth2AuthorizationStore.save(deleted);
    entityManager.flush();

    oauth2AuthorizationStore.deleteByUID(deleted.getUid());

    assertEquals(
        List.of(kept.getUid()),
        jdbcTemplate.queryForList(
            "select uid from oauth2_authorization where uid in (?, ?)",
            String.class,
            kept.getUid(),
            deleted.getUid()));
  }

  @Test
  void shouldEqualUninitializedProxyOfSameAuthorization() {
    Dhis2OAuth2Authorization authorization = newAuthorization("proxy-principal");
    authorization.setCode("PROXY_CODE");
    oauth2AuthorizationStore.save(authorization);
    entityManager.flush();
    clearSession();
    entityManagerFactory.getCache().evict(Dhis2OAuth2Authorization.class);

    Dhis2OAuth2Authorization proxy =
        entityManager.getReference(Dhis2OAuth2Authorization.class, authorization.getId());

    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(proxy, authorization);
    assertEquals(authorization, proxy);
    assertEquals(authorization.hashCode(), proxy.hashCode());
  }

  private static Date issuedAt(int token) {
    return new Date(1_700_000_000_000L + token * 1000L);
  }

  private static Date expiresAt(int token) {
    return new Date(1_800_000_000_000L + token * 1000L);
  }

  private static void setToken(Dhis2OAuth2Authorization authorization, String token, int i) {
    String value = "value-" + i;
    String metadata = "{\"token\":" + i + "}";
    switch (token) {
      case "authorizationCode" -> {
        authorization.setAuthorizationCodeValue(value);
        authorization.setAuthorizationCodeIssuedAt(issuedAt(i));
        authorization.setAuthorizationCodeExpiresAt(expiresAt(i));
        authorization.setAuthorizationCodeMetadata(metadata);
      }
      case "accessToken" -> {
        authorization.setAccessTokenValue(value);
        authorization.setAccessTokenIssuedAt(issuedAt(i));
        authorization.setAccessTokenExpiresAt(expiresAt(i));
        authorization.setAccessTokenMetadata(metadata);
      }
      case "refreshToken" -> {
        authorization.setRefreshTokenValue(value);
        authorization.setRefreshTokenIssuedAt(issuedAt(i));
        authorization.setRefreshTokenExpiresAt(expiresAt(i));
        authorization.setRefreshTokenMetadata(metadata);
      }
      case "oidcIdToken" -> {
        authorization.setOidcIdTokenValue(value);
        authorization.setOidcIdTokenIssuedAt(issuedAt(i));
        authorization.setOidcIdTokenExpiresAt(expiresAt(i));
        authorization.setOidcIdTokenMetadata(metadata);
      }
      case "userCode" -> {
        authorization.setUserCodeValue(value);
        authorization.setUserCodeIssuedAt(issuedAt(i));
        authorization.setUserCodeExpiresAt(expiresAt(i));
        authorization.setUserCodeMetadata(metadata);
      }
      case "deviceCode" -> {
        authorization.setDeviceCodeValue(value);
        authorization.setDeviceCodeIssuedAt(issuedAt(i));
        authorization.setDeviceCodeExpiresAt(expiresAt(i));
        authorization.setDeviceCodeMetadata(metadata);
      }
      default -> throw new IllegalArgumentException(token);
    }
  }

  private Dhis2OAuth2Authorization newAuthorization(String principalName) {
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setAutoFields();
    authorization.setName(principalName);
    authorization.setRegisteredClientId("round-trip-client");
    authorization.setPrincipalName(principalName);
    authorization.setAuthorizationGrantType("authorization_code");
    return authorization;
  }

  private Dhis2OAuth2Authorization reload(Dhis2OAuth2Authorization authorization) {
    entityManager.flush();
    clearSession();
    entityManagerFactory.getCache().evict(Dhis2OAuth2Authorization.class);
    return oauth2AuthorizationStore.get(authorization.getId());
  }

  private boolean isNullable(String property) {
    AbstractEntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(Dhis2OAuth2Authorization.class);
  }
}
