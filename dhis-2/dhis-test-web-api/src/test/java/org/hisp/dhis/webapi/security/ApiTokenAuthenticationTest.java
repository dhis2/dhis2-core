/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.web.WebClient.ApiTokenHeader;
import static org.hisp.dhis.web.WebClient.Header;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.security.apikey.ApiTokenStore;
import org.hisp.dhis.security.apikey.ApiTokenType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerWithApiTokenAuthTest;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class ApiTokenAuthenticationTest extends DhisControllerWithApiTokenAuthTest {
  public static final String URI = "/me?fields=settings,id";

  @Autowired private ApiTokenService apiTokenService;

  @Autowired private ApiTokenStore apiTokenStore;

  private static class TokenAndKey {
    String key;

    ApiToken apiToken;

    public static TokenAndKey of(String key, ApiToken token) {
      final TokenAndKey tokenAndKey = new TokenAndKey();
      tokenAndKey.key = key;
      tokenAndKey.apiToken = token;
      return tokenAndKey;
    }
  }

  @BeforeAll
  static void setUpClass() {
    DhisWebApiWebSecurityConfig.setApiContextPath("");
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  private TokenAndKey createNewToken() {
    ApiToken token = new ApiToken();
    token.setOwner("M5zQapPyTZI");
    token.setType(ApiTokenType.PERSONAL_ACCESS_TOKEN);
    token = apiTokenService.initToken(token);
    apiTokenStore.save(token);
    final String key = token.getKey();
    final String hashedKey = apiTokenService.hashKey(key);
    token.setKey(hashedKey);
    apiTokenService.update(token);
    return TokenAndKey.of(key, token);
  }

  @Test
  void testApiTokenAuthentication() {
    final TokenAndKey tokenAndKey = createNewToken();
    JsonUser user = GET(URI, ApiTokenHeader(tokenAndKey.key)).content().as(JsonUser.class);
    assertEquals(adminUser.getUid(), user.getId());
    assertEquals(
        "The API token does not exists.",
        GET(URI, ApiTokenHeader("FAKE_KEY")).error(HttpStatus.UNAUTHORIZED).getMessage());
  }

  @Test
  void testInvalidApiTokenAuthentication() {
    final TokenAndKey tokenAndKey = createNewToken();
    JsonUser user = GET(URI, ApiTokenHeader(tokenAndKey.key)).content().as(JsonUser.class);
    assertEquals(adminUser.getUid(), user.getId());
  }

  @Test
  void testAllowedIpRule() {
    final TokenAndKey tokenAndKey = createNewToken();
    final String key = tokenAndKey.key;
    final ApiToken apiToken = tokenAndKey.apiToken;
    apiToken.addIpToAllowedList("192.168.2.1");
    apiTokenService.update(apiToken);
    assertEquals(
        "Failed to authenticate API token, request ip address is not allowed.",
        GET(URI, ApiTokenHeader(key)).error(HttpStatus.UNAUTHORIZED).getMessage());
    apiToken.addIpToAllowedList("127.0.0.1");
    apiTokenService.update(apiToken);
    JsonUser user = GET(URI, ApiTokenHeader(key)).content().as(JsonUser.class);
    assertEquals(adminUser.getUid(), user.getId());
  }

  @Test
  void testAllowedMethodRule() {
    final TokenAndKey tokenAndKey = createNewToken();
    final String key = tokenAndKey.key;
    final ApiToken apiToken = tokenAndKey.apiToken;
    apiToken.addMethodToAllowedList("POST");
    apiTokenService.update(apiToken);
    assertEquals(
        "Failed to authenticate API token, request http method is not allowed.",
        GET(URI, ApiTokenHeader(key)).error(HttpStatus.UNAUTHORIZED).getMessage());
    apiToken.addMethodToAllowedList("GET");
    apiTokenService.update(apiToken);
    JsonUser user = GET(URI, ApiTokenHeader(key)).content().as(JsonUser.class);
    assertEquals(adminUser.getUid(), user.getId());
  }

  @Test
  void testAllowedReferrerRule() {
    final TokenAndKey tokenAndKey = createNewToken();
    final String key = tokenAndKey.key;
    final ApiToken apiToken = tokenAndKey.apiToken;
    apiToken.addReferrerToAllowedList("https://one.io");
    apiTokenService.update(apiToken);
    assertEquals(
        "Failed to authenticate API token, request http referrer is missing or not allowed.",
        GET(URI, ApiTokenHeader(key)).error(HttpStatus.UNAUTHORIZED).getMessage());
    apiToken.addReferrerToAllowedList("https://two.io");
    apiTokenService.update(apiToken);
    JsonUser user =
        GET(URI, ApiTokenHeader(key), Header("referer", "https://two.io"))
            .content()
            .as(JsonUser.class);
    assertEquals(adminUser.getUid(), user.getId());
  }

  @Test
  void testExpiredToken() {
    final TokenAndKey tokenAndKey = createNewToken();
    final String key = tokenAndKey.key;
    final ApiToken apiToken = tokenAndKey.apiToken;
    apiToken.setExpire(System.currentTimeMillis() - 36000);
    assertEquals(
        "Failed to authenticate API token, token has expired.",
        GET(URI, ApiTokenHeader(key)).error(HttpStatus.UNAUTHORIZED).getMessage());
  }

  @Test
  void testAuthWithDisabledUser() {
    final TokenAndKey tokenAndKey = createNewToken();
    final String key = tokenAndKey.key;
    User user = adminUser;
    user.setDisabled(true);
    userService.updateUser(user);
    assertEquals(
        "The API token is disabled, locked or 2FA is enabled.",
        GET(URI, ApiTokenHeader(key)).error(HttpStatus.UNAUTHORIZED).getMessage());
  }
}
