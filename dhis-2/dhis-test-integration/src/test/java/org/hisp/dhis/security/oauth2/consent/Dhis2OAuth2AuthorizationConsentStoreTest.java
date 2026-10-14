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
package org.hisp.dhis.security.oauth2.consent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests for the OAuth2AuthorizationConsentStore implementation. */
@Transactional
class Dhis2OAuth2AuthorizationConsentStoreTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2AuthorizationConsentStore oauth2AuthorizationConsentStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testSaveAndGetByUid() {
    // Create a test authorization consent
    Dhis2OAuth2AuthorizationConsent consent = new Dhis2OAuth2AuthorizationConsent();
    consent.setAutoFields();
    consent.setCreatedBy(getAdminUser());
    consent.setLastUpdatedBy(getAdminUser());
    consent.setName("Test OAuth2 Authorization Consent");
    consent.setRegisteredClientId("test-client-id");
    consent.setPrincipalName("test-user");
    consent.setAuthorities("SCOPE_read,SCOPE_write");

    // Save the consent
    oauth2AuthorizationConsentStore.save(consent);

    // Get the consent by UID
    Dhis2OAuth2AuthorizationConsent savedConsent =
        oauth2AuthorizationConsentStore.getByUid(consent.getUid());
    assertNotNull(savedConsent);
    assertEquals("Test OAuth2 Authorization Consent", savedConsent.getName());
    assertEquals("test-client-id", savedConsent.getRegisteredClientId());
    assertEquals("test-user", savedConsent.getPrincipalName());
    assertEquals("SCOPE_read,SCOPE_write", savedConsent.getAuthorities());
  }

  @Test
  void testGetByRegisteredClientIdAndPrincipalName() {
    // Create a test authorization consent
    Dhis2OAuth2AuthorizationConsent consent = new Dhis2OAuth2AuthorizationConsent();
    consent.setAutoFields();
    consent.setCreatedBy(getAdminUser());
    consent.setLastUpdatedBy(getAdminUser());
    consent.setName("Client-Principal Test");
    consent.setRegisteredClientId("specific-client-id");
    consent.setPrincipalName("specific-user");
    consent.setAuthorities("SCOPE_profile,SCOPE_email");

    // Save the consent
    oauth2AuthorizationConsentStore.save(consent);

    // Get the consent by registered client ID and principal name
    Dhis2OAuth2AuthorizationConsent foundConsent =
        oauth2AuthorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            "specific-client-id", "specific-user");

    assertNotNull(foundConsent);
    assertEquals("Client-Principal Test", foundConsent.getName());
    assertEquals("SCOPE_profile,SCOPE_email", foundConsent.getAuthorities());

    // Try to get a non-existent combination
    Dhis2OAuth2AuthorizationConsent nonExistentConsent =
        oauth2AuthorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            "non-existent-client", "non-existent-user");

    assertNull(nonExistentConsent);
  }

  @Test
  void testDeleteByRegisteredClientIdAndPrincipalName() {
    // Create a test authorization consent
    Dhis2OAuth2AuthorizationConsent consent = new Dhis2OAuth2AuthorizationConsent();
    consent.setAutoFields();
    consent.setCreatedBy(getAdminUser());
    consent.setLastUpdatedBy(getAdminUser());
    consent.setName("Deletion Test");
    consent.setRegisteredClientId("delete-client-id");
    consent.setPrincipalName("delete-user");
    consent.setAuthorities("SCOPE_delete");

    // Save the consent
    oauth2AuthorizationConsentStore.save(consent);

    // Verify it was saved
    Dhis2OAuth2AuthorizationConsent savedConsent =
        oauth2AuthorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            "delete-client-id", "delete-user");

    assertNotNull(savedConsent);

    // Delete the consent
    oauth2AuthorizationConsentStore.deleteByRegisteredClientIdAndPrincipalName(
        "delete-client-id", "delete-user");

    // Verify it was deleted
    Dhis2OAuth2AuthorizationConsent deletedConsent =
        oauth2AuthorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            "delete-client-id", "delete-user");

    assertNull(deletedConsent);
  }

  @Test
  void testMappingMatchesLegacyHbm() {
    AbstractEntityPersister persister = persister();

    assertEquals("oauth2_authorization_consent", persister.getTableName());
    assertArrayEquals(new String[] {"id"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());

    assertEquals(
        List.of(
            "authorities",
            "code",
            "created",
            "createdBy",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "principalName",
            "registeredClientId",
            "uid"),
        List.of(persister.getPropertyNames()).stream().sorted().toList());
    Map<String, String> columns =
        Map.of(
            "uid", "uid",
            "code", "code",
            "created", "created",
            "lastUpdated", "lastupdated",
            "lastUpdatedBy", "lastupdatedby",
            "createdBy", "createdby",
            "name", "name",
            "registeredClientId", "registered_client_id",
            "principalName", "principal_name",
            "authorities", "authorities");
    columns.forEach(
        (property, column) ->
            assertEquals(
                column,
                persister.getPropertyColumnNames(property)[0].toLowerCase(),
                property + " column"));

    for (String property : List.of("uid", "name", "registeredClientId", "principalName")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property : List.of("created", "lastUpdated")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
      assertInstanceOf(TimestampType.class, persister.getPropertyType(property), property);
    }
    for (String property : List.of("code", "authorities", "lastUpdatedBy", "createdBy")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    for (String property :
        List.of("uid", "code", "name", "registeredClientId", "principalName", "authorities")) {
      assertInstanceOf(StringType.class, persister.getPropertyType(property), property);
    }
    for (String property : List.of("lastUpdatedBy", "createdBy")) {
      ManyToOneType type =
          assertInstanceOf(ManyToOneType.class, persister.getPropertyType(property));
      assertEquals(User.class.getName(), type.getAssociatedEntityName(), property);
      assertFalse(type.isEager(null), property + " is eager");
    }

    boolean[] updateability = persister.getPropertyUpdateability();
    assertFalse(updateability[persister.getEntityMetamodel().getPropertyIndex("created")]);
    for (String property :
        List.of("lastUpdated", "createdBy", "registeredClientId", "principalName", "authorities")) {
      assertTrue(
          updateability[persister.getEntityMetamodel().getPropertyIndex(property)],
          property + " is not updatable");
    }

    assertTrue(persister.canWriteToCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void testColumnsRoundTrip() {
    User admin = getAdminUser();
    Date created = new Date(1_700_000_000_000L);
    Dhis2OAuth2AuthorizationConsent consent =
        newConsent("round-trip-client", "round-trip-user", "SCOPE_openid,SCOPE_profile");
    consent.setCode("CONSENT_CODE");
    consent.setName("Round trip consent");
    consent.setCreated(created);
    consent.setLastUpdatedBy(admin);
    save(consent);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, name, registered_client_id, principal_name, authorities, created,"
                + " lastupdated, lastupdatedby, createdby from oauth2_authorization_consent"
                + " where id = ?",
            consent.getId());
    assertEquals(consent.getUid(), row.get("uid"));
    assertEquals("CONSENT_CODE", row.get("code"));
    assertEquals("Round trip consent", row.get("name"));
    assertEquals("round-trip-client", row.get("registered_client_id"));
    assertEquals("round-trip-user", row.get("principal_name"));
    assertEquals("SCOPE_openid,SCOPE_profile", row.get("authorities"));
    assertEquals(created.getTime(), ((Date) row.get("created")).getTime());
    assertEquals(consent.getLastUpdated().getTime(), ((Date) row.get("lastupdated")).getTime());
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());
    assertEquals(admin.getId(), ((Number) row.get("createdby")).longValue());

    Dhis2OAuth2AuthorizationConsent reloaded = reload(consent);
    assertEquals(consent.getId(), reloaded.getId());
    assertEquals(consent.getUid(), reloaded.getUid());
    assertEquals("CONSENT_CODE", reloaded.getCode());
    assertEquals("Round trip consent", reloaded.getName());
    assertEquals("round-trip-client", reloaded.getRegisteredClientId());
    assertEquals("round-trip-user", reloaded.getPrincipalName());
    assertEquals("SCOPE_openid,SCOPE_profile", reloaded.getAuthorities());
    assertEquals(created.getTime(), reloaded.getCreated().getTime());
    assertEquals(consent.getLastUpdated().getTime(), reloaded.getLastUpdated().getTime());
    assertEquals(admin.getUid(), reloaded.getLastUpdatedBy().getUid());
    assertEquals(admin.getUid(), reloaded.getCreatedBy().getUid());
  }

  @Test
  void testAuthoritiesRoundTripAsStoredString() {
    String authorities = "SCOPE_openid,SCOPE_profile,SCOPE_email,ROLE_x y";
    Dhis2OAuth2AuthorizationConsent many = newConsent("auth-client", "many-user", authorities);
    Dhis2OAuth2AuthorizationConsent none = newConsent("auth-client", "none-user", null);
    Dhis2OAuth2AuthorizationConsent empty = newConsent("auth-client", "empty-user", "");
    save(many);
    save(none);
    save(empty);

    assertEquals(authorities, reload(many).getAuthorities());
    assertNull(reload(none).getAuthorities());
    assertEquals("", reload(empty).getAuthorities());
    assertNull(
        jdbcTemplate.queryForObject(
            "select authorities from oauth2_authorization_consent where id = ?",
            String.class,
            none.getId()));

    Dhis2OAuth2AuthorizationConsent updated = reload(many);
    updated.setAuthorities("SCOPE_openid");
    oauth2AuthorizationConsentStore.update(updated);
    assertEquals("SCOPE_openid", reload(many).getAuthorities());
  }

  @Test
  void testLookupMatchesBothKeyColumnsAfterReload() {
    save(newConsent("key-client-a", "key-user-a", "SCOPE_a"));
    save(newConsent("key-client-a", "key-user-b", "SCOPE_b"));
    save(newConsent("key-client-b", "key-user-a", "SCOPE_c"));
    clearSessionAndCache();

    assertEquals(
        "SCOPE_a",
        oauth2AuthorizationConsentStore
            .getByRegisteredClientIdAndPrincipalName("key-client-a", "key-user-a")
            .getAuthorities());
    assertEquals(
        "SCOPE_b",
        oauth2AuthorizationConsentStore
            .getByRegisteredClientIdAndPrincipalName("key-client-a", "key-user-b")
            .getAuthorities());
    assertEquals(
        "SCOPE_c",
        oauth2AuthorizationConsentStore
            .getByRegisteredClientIdAndPrincipalName("key-client-b", "key-user-a")
            .getAuthorities());
    assertNull(
        oauth2AuthorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            "key-client-b", "key-user-b"));

    oauth2AuthorizationConsentStore.deleteByRegisteredClientIdAndPrincipalName(
        "key-client-a", "key-user-a");
    clearSessionAndCache();

    assertNull(
        oauth2AuthorizationConsentStore.getByRegisteredClientIdAndPrincipalName(
            "key-client-a", "key-user-a"));
    assertEquals(
        2,
        jdbcTemplate.queryForObject(
            "select count(*) from oauth2_authorization_consent where registered_client_id like"
                + " 'key-client-%'",
            Integer.class));
  }

  private Dhis2OAuth2AuthorizationConsent newConsent(
      String registeredClientId, String principalName, String authorities) {
    Dhis2OAuth2AuthorizationConsent consent = new Dhis2OAuth2AuthorizationConsent();
    consent.setName(principalName);
    consent.setRegisteredClientId(registeredClientId);
    consent.setPrincipalName(principalName);
    consent.setAuthorities(authorities);
    return consent;
  }

  private void save(Dhis2OAuth2AuthorizationConsent consent) {
    oauth2AuthorizationConsentStore.save(consent);
    entityManager.flush();
  }

  /** Reloads the consent from the database, bypassing the session and the second-level cache. */
  private Dhis2OAuth2AuthorizationConsent reload(Dhis2OAuth2AuthorizationConsent consent) {
    clearSessionAndCache();
    return oauth2AuthorizationConsentStore.get(consent.getId());
  }

  private void clearSessionAndCache() {
    clearSession();
    entityManagerFactory.getCache().evictAll();
  }

  private AbstractEntityPersister persister() {
    EntityPersister persister =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(Dhis2OAuth2AuthorizationConsent.class);
    return (AbstractEntityPersister) persister;
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }
}
