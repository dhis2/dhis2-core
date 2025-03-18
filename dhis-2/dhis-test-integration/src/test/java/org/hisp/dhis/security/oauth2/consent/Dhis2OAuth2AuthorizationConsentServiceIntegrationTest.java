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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link Dhis2OAuth2AuthorizationConsentServiceImpl}. */
@Transactional
public class Dhis2OAuth2AuthorizationConsentServiceIntegrationTest
    extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2AuthorizationConsentService authorizationConsentService;

  @Autowired private Dhis2OAuth2AuthorizationConsentStore authorizationConsentStore;

  @Autowired private RegisteredClientRepository clientRepository;

  @Autowired private Dhis2OAuth2ClientStore clientStore;

  private RegisteredClient registeredClient;

  @BeforeEach
  void setUp() {
    // Create and save a test client
    registeredClient =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("consent-test-client-" + UUID.randomUUID())
            .clientName("Consent Test Client")
            .clientSecret("consent-test-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://consent-test.example.com/callback")
            .scope("profile")
            .scope("email")
            .build();

    clientRepository.save(registeredClient);
  }

  @Test
  void testSaveAndFindById() {
    // Given
    OAuth2AuthorizationConsent authorizationConsent =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user1")
            .authority(new SimpleGrantedAuthority("SCOPE_profile"))
            .authority(new SimpleGrantedAuthority("SCOPE_email"))
            .build();

    // When
    authorizationConsentService.save(authorizationConsent);
    OAuth2AuthorizationConsent foundConsent =
        authorizationConsentService.findById(registeredClient.getId(), "user1");

    // Then
    assertNotNull(foundConsent);
    assertEquals(
        authorizationConsent.getRegisteredClientId(), foundConsent.getRegisteredClientId());
    assertEquals(authorizationConsent.getPrincipalName(), foundConsent.getPrincipalName());

    Set<String> actualAuthorities = new HashSet<>();
    foundConsent
        .getAuthorities()
        .forEach(authority -> actualAuthorities.add(authority.getAuthority()));

    assertEquals(2, actualAuthorities.size());
    assertTrue(actualAuthorities.contains("SCOPE_profile"));
    assertTrue(actualAuthorities.contains("SCOPE_email"));
  }

  @Test
  void testRemoveAuthorizationConsent() {
    // Given
    OAuth2AuthorizationConsent authorizationConsent =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user2")
            .authority(new SimpleGrantedAuthority("SCOPE_profile"))
            .build();

    // When
    authorizationConsentService.save(authorizationConsent);
    OAuth2AuthorizationConsent foundBeforeRemove =
        authorizationConsentService.findById(registeredClient.getId(), "user2");

    authorizationConsentService.remove(authorizationConsent);
    OAuth2AuthorizationConsent foundAfterRemove =
        authorizationConsentService.findById(registeredClient.getId(), "user2");

    // Then
    assertNotNull(foundBeforeRemove);
    assertNull(foundAfterRemove);
  }

  @Test
  void testUpdateExistingConsent() {
    // Given - create initial consent with one authority
    OAuth2AuthorizationConsent initialConsent =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user3")
            .authority(new SimpleGrantedAuthority("SCOPE_profile"))
            .build();

    authorizationConsentService.save(initialConsent);

    // When - update with modified authorities
    OAuth2AuthorizationConsent updatedConsent =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user3")
            .authority(new SimpleGrantedAuthority("SCOPE_profile"))
            .authority(new SimpleGrantedAuthority("SCOPE_email"))
            .authority(new SimpleGrantedAuthority("SCOPE_address"))
            .build();

    authorizationConsentService.save(updatedConsent);

    // Then
    OAuth2AuthorizationConsent foundConsent =
        authorizationConsentService.findById(registeredClient.getId(), "user3");

    assertNotNull(foundConsent);

    Set<String> authorities = new HashSet<>();
    foundConsent.getAuthorities().forEach(authority -> authorities.add(authority.getAuthority()));

    assertEquals(3, authorities.size());
    assertTrue(authorities.contains("SCOPE_profile"));
    assertTrue(authorities.contains("SCOPE_email"));
    assertTrue(authorities.contains("SCOPE_address"));
  }

  @Test
  void testMultipleConsentsForDifferentUsers() {
    // Given - create consents for different users
    OAuth2AuthorizationConsent consent1 =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user4")
            .authority(new SimpleGrantedAuthority("SCOPE_profile"))
            .build();

    OAuth2AuthorizationConsent consent2 =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user5")
            .authority(new SimpleGrantedAuthority("SCOPE_email"))
            .build();

    // When
    authorizationConsentService.save(consent1);
    authorizationConsentService.save(consent2);

    OAuth2AuthorizationConsent foundConsent1 =
        authorizationConsentService.findById(registeredClient.getId(), "user4");

    OAuth2AuthorizationConsent foundConsent2 =
        authorizationConsentService.findById(registeredClient.getId(), "user5");

    // Then
    assertNotNull(foundConsent1);
    assertNotNull(foundConsent2);

    assertEquals("user4", foundConsent1.getPrincipalName());
    assertEquals("user5", foundConsent2.getPrincipalName());

    Set<String> authorities1 = new HashSet<>();
    foundConsent1.getAuthorities().forEach(authority -> authorities1.add(authority.getAuthority()));

    Set<String> authorities2 = new HashSet<>();
    foundConsent2.getAuthorities().forEach(authority -> authorities2.add(authority.getAuthority()));

    assertTrue(authorities1.contains("SCOPE_profile"));
    assertTrue(authorities2.contains("SCOPE_email"));
  }

  @Test
  void testConsentsForMultipleClients() {
    // Given - create another client
    RegisteredClient anotherClient =
        RegisteredClient.withId(CodeGenerator.generateUid())
            .clientId("another-consent-client-" + UUID.randomUUID())
            .clientName("Another Consent Test Client")
            .clientSecret("another-consent-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://another-consent.example.com/callback")
            .scope("api")
            .build();

    clientRepository.save(anotherClient);

    // Create consents for different clients
    OAuth2AuthorizationConsent consent1 =
        OAuth2AuthorizationConsent.withId(registeredClient.getId(), "user6")
            .authority(new SimpleGrantedAuthority("SCOPE_profile"))
            .build();

    OAuth2AuthorizationConsent consent2 =
        OAuth2AuthorizationConsent.withId(anotherClient.getId(), "user6")
            .authority(new SimpleGrantedAuthority("SCOPE_api"))
            .build();

    // When
    authorizationConsentService.save(consent1);
    authorizationConsentService.save(consent2);

    OAuth2AuthorizationConsent foundConsent1 =
        authorizationConsentService.findById(registeredClient.getId(), "user6");

    OAuth2AuthorizationConsent foundConsent2 =
        authorizationConsentService.findById(anotherClient.getId(), "user6");

    // Then
    assertNotNull(foundConsent1);
    assertNotNull(foundConsent2);

    assertEquals(registeredClient.getId(), foundConsent1.getRegisteredClientId());
    assertEquals(anotherClient.getId(), foundConsent2.getRegisteredClientId());

    Set<String> authorities1 = new HashSet<>();
    foundConsent1.getAuthorities().forEach(authority -> authorities1.add(authority.getAuthority()));

    Set<String> authorities2 = new HashSet<>();
    foundConsent2.getAuthorities().forEach(authority -> authorities2.add(authority.getAuthority()));

    assertTrue(authorities1.contains("SCOPE_profile"));
    assertTrue(authorities2.contains("SCOPE_api"));
  }
}
