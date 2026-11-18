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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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

/** Tests for the OAuth2ClientStore implementation. */
@Transactional
class Dhis2OAuth2ClientStoreTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2ClientStore oauth2ClientStore;

  @Autowired private EntityManager entityManager;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

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

  @Test
  void shouldMapClientToOauth2ClientTable() {
    AbstractEntityPersister persister = persister();

    assertEquals("oauth2_client", persister.getTableName());
    assertArrayEquals(new String[] {"id"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());

    Map<String, String> columns =
        Map.ofEntries(
            Map.entry("uid", "uid"),
            Map.entry("code", "code"),
            Map.entry("created", "created"),
            Map.entry("lastUpdated", "lastupdated"),
            Map.entry("lastUpdatedBy", "lastupdatedby"),
            Map.entry("createdBy", "createdby"),
            Map.entry("name", "name"),
            Map.entry("clientId", "client_id"),
            Map.entry("clientIdIssuedAt", "client_id_issued_at"),
            Map.entry("clientSecret", "client_secret"),
            Map.entry("clientSecretExpiresAt", "client_secret_expires_at"),
            Map.entry("clientAuthenticationMethods", "client_authentication_methods"),
            Map.entry("authorizationGrantTypes", "authorization_grant_types"),
            Map.entry("redirectUris", "redirect_uris"),
            Map.entry("postLogoutRedirectUris", "post_logout_redirect_uris"),
            Map.entry("scopes", "scopes"),
            Map.entry("clientSettings", "client_settings"),
            Map.entry("tokenSettings", "token_settings"));
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
  void shouldMapClientPropertyTypesAndNullability() {
    AbstractEntityPersister persister = persister();

    for (String property : List.of("uid", "name", "clientId", "created", "lastUpdated")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of(
            "code",
            "clientSecret",
            "clientIdIssuedAt",
            "clientSecretExpiresAt",
            "clientAuthenticationMethods",
            "authorizationGrantTypes",
            "redirectUris",
            "postLogoutRedirectUris",
            "scopes",
            "clientSettings",
            "tokenSettings",
            "lastUpdatedBy",
            "createdBy")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    for (String property :
        List.of("created", "lastUpdated", "clientIdIssuedAt", "clientSecretExpiresAt")) {
      assertInstanceOf(TimestampType.class, persister.getPropertyType(property), property);
    }
    for (String property :
        List.of(
            "uid",
            "code",
            "name",
            "clientId",
            "clientSecret",
            "clientAuthenticationMethods",
            "authorizationGrantTypes",
            "redirectUris",
            "postLogoutRedirectUris",
            "scopes")) {
      assertInstanceOf(StringType.class, persister.getPropertyType(property), property);
    }
    for (String property : List.of("clientSettings", "tokenSettings")) {
      assertInstanceOf(TextType.class, persister.getPropertyType(property), property);
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
  void shouldCacheClientReadWrite() {
    AbstractEntityPersister persister = persister();

    assertTrue(persister.canWriteToCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void shouldWriteAndReadEveryClientColumn() {
    User admin = getAdminUser();
    Date issuedAt = new Date(1_700_000_000_000L);
    Date expiresAt = new Date(1_800_000_000_000L);
    String clientSettings = "{\"settings.client.require-proof-key\":true}";
    Dhis2OAuth2Client client = newClient("round-trip-client");
    client.setCode("CLIENT_CODE");
    client.setName("Round trip client");
    client.setCreatedBy(admin);
    client.setLastUpdatedBy(admin);
    client.setClientSecret("{noop}secret");
    client.setClientIdIssuedAt(issuedAt);
    client.setClientSecretExpiresAt(expiresAt);
    client.setClientAuthenticationMethods("client_secret_basic,client_secret_post");
    client.setAuthorizationGrantTypes("authorization_code,refresh_token");
    client.setRedirectUris("https://example.com/a,https://example.com/b");
    client.setPostLogoutRedirectUris("https://example.com/logout");
    client.setScopes("openid,profile");
    client.setClientSettings(clientSettings);
    client.setTokenSettings("{\"settings.token.reuse-refresh-tokens\":true}");
    oauth2ClientStore.save(client);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, name, client_id, client_secret, client_id_issued_at,"
                + " client_secret_expires_at, client_authentication_methods,"
                + " authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes,"
                + " client_settings, token_settings, createdby, lastupdatedby"
                + " from oauth2_client where id = ?",
            client.getId());
    assertEquals(client.getUid(), row.get("uid"));
    assertEquals("CLIENT_CODE", row.get("code"));
    assertEquals("Round trip client", row.get("name"));
    assertEquals("round-trip-client", row.get("client_id"));
    assertEquals("{noop}secret", row.get("client_secret"));
    assertEquals(issuedAt.getTime(), ((Date) row.get("client_id_issued_at")).getTime());
    assertEquals(expiresAt.getTime(), ((Date) row.get("client_secret_expires_at")).getTime());
    assertEquals(
        "client_secret_basic,client_secret_post", row.get("client_authentication_methods"));
    assertEquals("authorization_code,refresh_token", row.get("authorization_grant_types"));
    assertEquals("https://example.com/a,https://example.com/b", row.get("redirect_uris"));
    assertEquals("https://example.com/logout", row.get("post_logout_redirect_uris"));
    assertEquals("openid,profile", row.get("scopes"));
    assertEquals(clientSettings, row.get("client_settings"));
    assertEquals("{\"settings.token.reuse-refresh-tokens\":true}", row.get("token_settings"));
    assertEquals(admin.getId(), ((Number) row.get("createdby")).longValue());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());

    Dhis2OAuth2Client loaded = reload(client);
    assertEquals("CLIENT_CODE", loaded.getCode());
    assertEquals("Round trip client", loaded.getRawName());
    assertEquals("{noop}secret", loaded.getClientSecret());
    assertEquals(issuedAt.getTime(), loaded.getClientIdIssuedAt().getTime());
    assertEquals(expiresAt.getTime(), loaded.getClientSecretExpiresAt().getTime());
    assertEquals("https://example.com/logout", loaded.getPostLogoutRedirectUris());
    assertEquals(clientSettings, loaded.getClientSettings());
    assertEquals(admin.getUid(), loaded.getCreatedBy().getUid());
    assertEquals(admin.getUid(), loaded.getLastUpdatedBy().getUid());
  }

  @Test
  void shouldWriteClientIdFallbackToNameColumnWhenNameIsMissing() {
    Dhis2OAuth2Client client = newClient("name-fallback-client");
    oauth2ClientStore.save(client);
    entityManager.flush();

    assertNull(client.getRawName());
    assertEquals(
        "name-fallback-client",
        jdbcTemplate.queryForObject(
            "select name from oauth2_client where id = ?", String.class, client.getId()));
  }

  @Test
  void shouldReadNameColumnThroughNameSetter() {
    Dhis2OAuth2Client client = newClient("name-column-client");
    client.setName("Stored name");
    oauth2ClientStore.save(client);
    entityManager.flush();
    jdbcTemplate.update(
        "update oauth2_client set name = 'Changed in DB' where id = ?", client.getId());

    Dhis2OAuth2Client loaded = reload(client);

    assertEquals("Changed in DB", loaded.getRawName());
    assertEquals("Changed in DB", loaded.getName());
  }

  @Test
  void shouldEqualUninitializedProxyOfSameClient() {
    Dhis2OAuth2Client client = newClient("proxy-client");
    client.setCode("PROXY_CODE");
    client.setName("Proxy client");
    oauth2ClientStore.save(client);
    entityManager.flush();
    clearSession();
    entityManagerFactory.getCache().evict(Dhis2OAuth2Client.class);

    Dhis2OAuth2Client proxy = entityManager.getReference(Dhis2OAuth2Client.class, client.getId());

    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(proxy, client);
    assertEquals(client, proxy);
    assertEquals(client.hashCode(), proxy.hashCode());
  }

  private Dhis2OAuth2Client newClient(String clientId) {
    Dhis2OAuth2Client client = new Dhis2OAuth2Client();
    client.setAutoFields();
    client.setClientId(clientId);
    return client;
  }

  private Dhis2OAuth2Client reload(Dhis2OAuth2Client client) {
    entityManager.flush();
    clearSession();
    entityManagerFactory.getCache().evict(Dhis2OAuth2Client.class);
    return oauth2ClientStore.get(client.getId());
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
            .entityPersister(Dhis2OAuth2Client.class);
  }
}
