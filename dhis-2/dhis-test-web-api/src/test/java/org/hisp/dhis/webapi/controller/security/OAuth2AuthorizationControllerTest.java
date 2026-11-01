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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2Authorization;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2AuthorizationStore;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the read-only {@code /api/oAuth2Authorizations} endpoint and its schema. */
@Transactional
class OAuth2AuthorizationControllerTest extends PostgresControllerIntegrationTestBase {

  private static final Date ISSUED_AT = new Date(1_700_000_000_000L);

  private static final Date EXPIRES_AT = new Date(1_800_000_000_000L);

  @Autowired private Dhis2OAuth2AuthorizationStore authorizationStore;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testGetAuthorizationReturnsPersistedAndRuntimePropertiesWithoutTokenValues() {
    Dhis2OAuth2Authorization authorization = saveAuthorization("authorization-user");

    JsonObject json =
        GET("/oAuth2Authorizations/{uid}?fields=*", authorization.getUid()).content(HttpStatus.OK);

    assertEquals(
        Set.of(
            "access",
            "accessTokenExpiresAt",
            "accessTokenIssuedAt",
            "accessTokenScopes",
            "accessTokenType",
            "attributeValues",
            "authorizationCodeExpiresAt",
            "authorizationCodeIssuedAt",
            "authorizationGrantType",
            "authorizedScopes",
            "code",
            "created",
            "createdBy",
            "deviceCodeExpiresAt",
            "deviceCodeIssuedAt",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "oidcIdTokenExpiresAt",
            "oidcIdTokenIssuedAt",
            "principalName",
            "refreshTokenExpiresAt",
            "refreshTokenIssuedAt",
            "registeredClientId",
            "sharing",
            "translations",
            "user",
            "userCodeExpiresAt",
            "userCodeIssuedAt"),
        Set.copyOf(json.names()));
    assertEquals(authorization.getUid(), json.getString("id").string());
    assertEquals("CODE_authorization-user", json.getString("code").string());
    assertEquals("authorization-user", json.getString("name").string());
    assertEquals("authorization-user", json.getString("displayName").string());
    assertEquals("authorization-client", json.getString("registeredClientId").string());
    assertEquals("authorization-user", json.getString("principalName").string());
    assertEquals("authorization_code", json.getString("authorizationGrantType").string());
    assertEquals("openid,profile", json.getString("authorizedScopes").string());
    assertEquals("Bearer", json.getString("accessTokenType").string());
    assertEquals("openid", json.getString("accessTokenScopes").string());
    for (String token :
        List.of(
            "authorizationCode",
            "accessToken",
            "refreshToken",
            "oidcIdToken",
            "userCode",
            "deviceCode")) {
      assertEquals("2023-11-14T22:13:20.000", json.getString(token + "IssuedAt").string(), token);
      assertEquals("2027-01-15T08:00:00.000", json.getString(token + "ExpiresAt").string(), token);
    }
    assertEquals(getAdminUid(), json.getObject("createdBy").getString("id").string());
    assertEquals(getAdminUid(), json.getObject("user").getString("id").string());
    assertEquals(getAdminUid(), json.getObject("lastUpdatedBy").getString("id").string());
    assertTrue(json.getArray("translations").isEmpty());
    assertTrue(json.getArray("attributeValues").isEmpty());
  }

  @Test
  void testListFilteredByRegisteredClientAndOrderedByDisplayName() {
    saveAuthorization("authorization-c");
    saveAuthorization("authorization-a");
    saveAuthorization("authorization-b");
    Dhis2OAuth2Authorization otherClient = newAuthorization("authorization-0");
    otherClient.setRegisteredClientId("other-client");
    authorizationStore.save(otherClient);
    startNewRequestSessionWithoutSecondLevelCache();

    JsonArray authorizations =
        GET("/oAuth2Authorizations?filter=registeredClientId:eq:authorization-client"
                + "&order=displayName:asc&fields=displayName,principalName")
            .content(HttpStatus.OK)
            .getArray("oAuth2Authorizations");

    assertEquals(
        List.of("authorization-a", "authorization-b", "authorization-c"),
        values(authorizations, "displayName"));
    assertEquals(
        List.of("authorization-a", "authorization-b", "authorization-c"),
        values(authorizations, "principalName"));
  }

  @Test
  void testGistListOrderedByAccessTokenExpiry() {
    Dhis2OAuth2Authorization later = newAuthorization("authorization-later");
    later.setAccessTokenExpiresAt(new Date(EXPIRES_AT.getTime() + 1000));
    authorizationStore.save(later);
    saveAuthorization("authorization-sooner");

    JsonArray authorizations =
        GET("/oAuth2Authorizations/gist?fields=principalName,accessTokenExpiresAt"
                + "&order=accessTokenExpiresAt:desc&headless=true")
            .content(HttpStatus.OK)
            .as(JsonArray.class);

    assertEquals(
        List.of("authorization-later", "authorization-sooner"),
        values(authorizations, "principalName"));
  }

  @Test
  void testSchemaPropertiesMatchMapping() {
    JsonObject schema = GET("/schemas/oAuth2Authorization").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Map<String, String> actual = new TreeMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String propertyType = property.getString("propertyType").string();
      actual.put(
          property.getString("name").string(),
          property.getBoolean("persisted").booleanValue()
              + " "
              + propertyType
              + " required="
              + property.getBoolean("required").booleanValue()
              + ("TEXT".equals(propertyType) && property.has("length")
                  ? " length=" + property.getNumber("length").intValue()
                  : ""));
    }

    Map<String, String> expected = new TreeMap<>();
    expected.put("access", "false COMPLEX required=false");
    expected.put("accessTokenExpiresAt", "true DATE required=false");
    expected.put("accessTokenIssuedAt", "true DATE required=false");
    expected.put("accessTokenScopes", "true TEXT required=false length=1000");
    expected.put("accessTokenType", "true TEXT required=false length=255");
    expected.put("attributeValues", "false COMPLEX required=false");
    expected.put("authorizationCodeExpiresAt", "true DATE required=false");
    expected.put("authorizationCodeIssuedAt", "true DATE required=false");
    expected.put("authorizationGrantType", "true TEXT required=true length=255");
    expected.put("authorizedScopes", "true TEXT required=false length=1000");
    expected.put("code", "true IDENTIFIER required=false");
    expected.put("created", "true DATE required=false");
    expected.put("createdBy", "true REFERENCE required=false");
    expected.put("deviceCodeExpiresAt", "true DATE required=false");
    expected.put("deviceCodeIssuedAt", "true DATE required=false");
    expected.put("displayName", "false TEXT required=false");
    expected.put("href", "false URL required=false");
    expected.put("id", "true IDENTIFIER required=false");
    expected.put("lastUpdated", "true DATE required=false");
    expected.put("lastUpdatedBy", "true REFERENCE required=false");
    expected.put("name", "true TEXT required=true length=230");
    expected.put("oidcIdTokenExpiresAt", "true DATE required=false");
    expected.put("oidcIdTokenIssuedAt", "true DATE required=false");
    expected.put("principalName", "true TEXT required=true length=255");
    expected.put("refreshTokenExpiresAt", "true DATE required=false");
    expected.put("refreshTokenIssuedAt", "true DATE required=false");
    expected.put("registeredClientId", "true TEXT required=true length=255");
    expected.put("sharing", "false COMPLEX required=false");
    expected.put("translation", "false COLLECTION required=false");
    expected.put("user", "false REFERENCE required=false");
    expected.put("userCodeExpiresAt", "true DATE required=false");
    expected.put("userCodeIssuedAt", "true DATE required=false");
    assertEquals(expected, actual);
    assertEquals("oauth2Authorization", schema.getString("name").string());
    assertTrue(schema.getBoolean("metadata").booleanValue());
    assertTrue(schema.getBoolean("secondaryMetadata").booleanValue());
    assertFalse(schema.getBoolean("shareable").booleanValue());
  }

  private static List<String> values(JsonArray objects, String property) {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < objects.size(); i++) {
      values.add(objects.getObject(i).getString(property).string());
    }
    return values;
  }

  /** Saves an authorization and starts a new request session, so the GET reads the row. */
  private Dhis2OAuth2Authorization saveAuthorization(String principalName) {
    Dhis2OAuth2Authorization authorization = newAuthorization(principalName);
    authorizationStore.save(authorization);
    startNewRequestSessionWithoutSecondLevelCache();
    return authorization;
  }

  private Dhis2OAuth2Authorization newAuthorization(String principalName) {
    Dhis2OAuth2Authorization authorization = new Dhis2OAuth2Authorization();
    authorization.setCode("CODE_" + principalName);
    authorization.setName(principalName);
    authorization.setRegisteredClientId("authorization-client");
    authorization.setPrincipalName(principalName);
    authorization.setAuthorizationGrantType("authorization_code");
    authorization.setAuthorizedScopes("openid,profile");
    authorization.setAccessTokenType("Bearer");
    authorization.setAccessTokenScopes("openid");
    authorization.setAuthorizationCodeIssuedAt(ISSUED_AT);
    authorization.setAuthorizationCodeExpiresAt(EXPIRES_AT);
    authorization.setAccessTokenIssuedAt(ISSUED_AT);
    authorization.setAccessTokenExpiresAt(EXPIRES_AT);
    authorization.setRefreshTokenIssuedAt(ISSUED_AT);
    authorization.setRefreshTokenExpiresAt(EXPIRES_AT);
    authorization.setOidcIdTokenIssuedAt(ISSUED_AT);
    authorization.setOidcIdTokenExpiresAt(EXPIRES_AT);
    authorization.setUserCodeIssuedAt(ISSUED_AT);
    authorization.setUserCodeExpiresAt(EXPIRES_AT);
    authorization.setDeviceCodeIssuedAt(ISSUED_AT);
    authorization.setDeviceCodeExpiresAt(EXPIRES_AT);
    authorization.setLastUpdatedBy(getAdminUser());
    return authorization;
  }

  /** Flushes and clears the session and the second-level cache, as between two requests. */
  private void startNewRequestSessionWithoutSecondLevelCache() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
  }
}
