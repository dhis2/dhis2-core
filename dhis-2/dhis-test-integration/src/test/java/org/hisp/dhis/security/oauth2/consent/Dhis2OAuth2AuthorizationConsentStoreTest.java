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

import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests for the OAuth2AuthorizationConsentStore implementation. */
@Transactional
class Dhis2OAuth2AuthorizationConsentStoreTest extends PostgresIntegrationTestBase {

  @Autowired private Dhis2OAuth2AuthorizationConsentStore oauth2AuthorizationConsentStore;

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
}
