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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.security.oauth2.consent.Dhis2OAuth2AuthorizationConsent;
import org.hisp.dhis.security.oauth2.consent.Dhis2OAuth2AuthorizationConsentStore;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the read-only {@code /api/oAuth2AuthorizationConsents} endpoint and its schema. */
@Transactional
class OAuth2AuthorizationConsentControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private Dhis2OAuth2AuthorizationConsentStore consentStore;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testGetConsentReturnsPersistedAndRuntimeProperties() {
    Dhis2OAuth2AuthorizationConsent consent = new Dhis2OAuth2AuthorizationConsent();
    consent.setCode("CONSENT_A");
    consent.setName("consent-user");
    consent.setRegisteredClientId("consent-client");
    consent.setPrincipalName("consent-user");
    consent.setAuthorities("SCOPE_openid,SCOPE_profile");
    consent.setLastUpdatedBy(getAdminUser());
    consentStore.save(consent);
    startNewRequestSessionWithoutSecondLevelCache();

    JsonObject json = GET("/oAuth2AuthorizationConsents/{uid}", consent.getUid()).content();

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "authorities",
            "code",
            "created",
            "createdBy",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "principalName",
            "registeredClientId",
            "sharing",
            "translations",
            "user"),
        Set.copyOf(json.names()));
    assertEquals(consent.getUid(), json.getString("id").string());
    assertEquals("CONSENT_A", json.getString("code").string());
    assertEquals("consent-user", json.getString("name").string());
    assertEquals("consent-user", json.getString("displayName").string());
    assertEquals("consent-client", json.getString("registeredClientId").string());
    assertEquals("consent-user", json.getString("principalName").string());
    assertEquals("SCOPE_openid,SCOPE_profile", json.getString("authorities").string());
    assertEquals(getAdminUid(), json.getObject("createdBy").getString("id").string());
    assertEquals(getAdminUid(), json.getObject("user").getString("id").string());
    assertEquals(getAdminUid(), json.getObject("lastUpdatedBy").getString("id").string());
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(json.getObject("sharing").names()));
    assertEquals(
        Set.of("manage", "write", "read", "update", "delete"),
        Set.copyOf(json.getObject("access").names()));
    assertTrue(json.getArray("translations").isEmpty());
    assertTrue(json.getArray("attributeValues").isEmpty());
  }

  @Test
  void testSchemaPropertiesMatchMapping() {
    JsonObject schema = GET("/schemas/oAuth2AuthorizationConsent").content();
    JsonArray properties = schema.getArray("properties");
    Map<String, String> actual = new TreeMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      actual.put(
          property.getString("name").string(),
          property.getBoolean("persisted").booleanValue()
              + " "
              + property.getString("propertyType").string()
              + " required="
              + property.getBoolean("required").booleanValue()
              + " unique="
              + property.getBoolean("unique").booleanValue()
              + " length="
              + (property.has("length") ? property.getNumber("length").intValue() : null));
    }

    Map<String, String> expected = new TreeMap<>();
    expected.put("access", "false COMPLEX required=false unique=false length=null");
    expected.put("attributeValues", "false COMPLEX required=false unique=false length=null");
    expected.put("authorities", "true TEXT required=false unique=false length=1000");
    expected.put("code", "true IDENTIFIER required=false unique=true length=50");
    expected.put("created", "true DATE required=false unique=false length=255");
    expected.put("createdBy", "true REFERENCE required=false unique=false length=255");
    expected.put("displayName", "false TEXT required=false unique=false length=null");
    expected.put("href", "false URL required=false unique=false length=null");
    expected.put("id", "true IDENTIFIER required=false unique=true length=11");
    expected.put("lastUpdated", "true DATE required=false unique=false length=255");
    expected.put("lastUpdatedBy", "true REFERENCE required=false unique=false length=255");
    expected.put("name", "true TEXT required=true unique=false length=230");
    expected.put("principalName", "true TEXT required=true unique=false length=255");
    expected.put("registeredClientId", "true TEXT required=true unique=false length=255");
    expected.put("sharing", "false COMPLEX required=false unique=false length=null");
    expected.put("translation", "false COLLECTION required=false unique=false length=null");
    expected.put("user", "false REFERENCE required=false unique=false length=null");
    assertEquals(expected, actual);
    assertTrue(schema.getBoolean("metadata").booleanValue());
    assertTrue(schema.getBoolean("secondaryMetadata").booleanValue());
    assertFalse(schema.getBoolean("shareable").booleanValue());
  }

  /** Flushes and clears the session and the second-level cache, as between two requests. */
  private void startNewRequestSessionWithoutSecondLevelCache() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
  }
}
