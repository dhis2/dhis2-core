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
package org.hisp.dhis.security.oauth2.consent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consent changes made through the service must invalidate the second-level cache entry of a
 * consent read before. The service methods run in committed transactions here (the test class is
 * not {@code @Transactional}), so the {@code READ_WRITE} cache sees the transaction completion.
 */
class Dhis2OAuth2AuthorizationConsentCacheTest extends PostgresIntegrationTestBase {

  private static final String PRINCIPAL = "cached-user";

  @Autowired private Dhis2OAuth2AuthorizationConsentService authorizationConsentService;

  @Autowired private RegisteredClientRepository clientRepository;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TransactionTemplate transactionTemplate;

  private RegisteredClient registeredClient;

  @BeforeEach
  void setUp() {
    registeredClient =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("cache-test-client-" + UUID.randomUUID())
            .clientName("Cache Test Client")
            .clientSecret("cache-test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://cache-test.example.com/callback")
            .scope("profile")
            .build();
    clientRepository.save(registeredClient);
    entityManagerFactory.getCache().evictAll();
  }

  /** The consent and client tables are not emptied between tests and reference userinfo. */
  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(
        status -> {
          jdbcTemplate.update(
              "delete from oauth2_authorization_consent where registered_client_id = ?",
              registeredClient.getId());
          jdbcTemplate.update("delete from oauth2_client where uid = ?", registeredClient.getId());
        });
  }

  @Test
  void testRemoveInvalidatesCachedConsent() {
    save("SCOPE_profile");
    long id = readIntoCache();

    authorizationConsentService.remove(consent("SCOPE_profile"));
    entityManager.clear();

    // READ_WRITE replaces a deleted entry with a soft lock instead of removing the key, so
    // Cache.contains stays true; what matters is that no read is served from the old entry
    assertNull(entityManager.find(Dhis2OAuth2AuthorizationConsent.class, id));
    assertNull(authorizationConsentService.findById(registeredClient.getId(), PRINCIPAL));
  }

  @Test
  void testUpdateReplacesCachedAuthorities() {
    save("SCOPE_profile");
    long id = readIntoCache();

    save("SCOPE_email", "SCOPE_address");
    entityManager.clear();

    assertEquals(
        Set.of("SCOPE_email", "SCOPE_address"),
        Set.of(
            entityManager
                .find(Dhis2OAuth2AuthorizationConsent.class, id)
                .getAuthorities()
                .split(",")));
    assertEquals(
        Set.of("SCOPE_email", "SCOPE_address"),
        authorityNames(authorizationConsentService.findById(registeredClient.getId(), PRINCIPAL)));
  }

  /** Reads the consent through the service so that it lands in the second-level cache. */
  private long readIntoCache() {
    entityManager.clear();
    assertNotNull(authorizationConsentService.findById(registeredClient.getId(), PRINCIPAL));
    entityManager.clear();
    long id =
        jdbcTemplate.queryForObject(
            "select id from oauth2_authorization_consent"
                + " where registered_client_id = ? and principal_name = ?",
            Long.class,
            registeredClient.getId(),
            PRINCIPAL);
    assertTrue(isCached(id));
    return id;
  }

  private boolean isCached(long id) {
    return entityManagerFactory.getCache().contains(Dhis2OAuth2AuthorizationConsent.class, id);
  }

  private void save(String... authorities) {
    authorizationConsentService.save(consent(authorities));
  }

  private OAuth2AuthorizationConsent consent(String... authorities) {
    OAuth2AuthorizationConsent.Builder builder =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), PRINCIPAL);
    for (String authority : authorities) {
      builder.authority(new SimpleGrantedAuthority(authority));
    }
    return builder.build();
  }

  private static Set<String> authorityNames(OAuth2AuthorizationConsent consent) {
    return consent.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }
}
