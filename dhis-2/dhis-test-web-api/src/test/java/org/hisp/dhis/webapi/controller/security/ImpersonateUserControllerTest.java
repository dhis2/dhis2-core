/*
 * Copyright (c) 2004-2024, University of Oslo
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

import java.util.Properties;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.config.H2DhisConfigurationProvider;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonImpersonateUserResponse;
import org.hisp.dhis.test.webapi.json.domain.JsonUser;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.security.ImpersonateUserControllerTest.DhisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ContextConfiguration(
    classes = {
      DhisConfig.class,
    })
@ActiveProfiles("impersonate-user-test")
@Transactional
class ImpersonateUserControllerTest extends H2ControllerIntegrationTestBase {

  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      H2DhisConfigurationProvider provider = new H2DhisConfigurationProvider();

      Properties properties = new Properties();
      properties.put(ConfigurationKey.SWITCH_USER_FEATURE_ENABLED.getKey(), "true");
      provider.addProperties(properties);

      return provider;
    }
  }

  @Test
  void testImpersonateUserOKAsRoot() {
    String usernameToImpersonate = "usera";
    createUserWithAuth(usernameToImpersonate, "ALL");

    JsonImpersonateUserResponse response =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.OK)
            .as(JsonImpersonateUserResponse.class);

    assertEquals("IMPERSONATION_SUCCESS", response.getLoginStatus());
    assertEquals(usernameToImpersonate, response.getImpersonatedUsername());

    JsonUser me = GET("/me?fields=username").content(HttpStatus.OK).as(JsonUser.class);

    String username = me.getUsername();
    assertEquals(usernameToImpersonate, username);
  }

  @Test
  void testImpersonateUserOKAndExit() {
    String usernameToImpersonate = "usera";
    createUserWithAuth(usernameToImpersonate, "NONE");

    JsonImpersonateUserResponse response =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.OK)
            .as(JsonImpersonateUserResponse.class);

    assertEquals("IMPERSONATION_SUCCESS", response.getLoginStatus());
    assertEquals(usernameToImpersonate, response.getImpersonatedUsername());

    JsonUser me = GET("/me?fields=username").content(HttpStatus.OK).as(JsonUser.class);

    String username = me.getUsername();
    assertEquals(usernameToImpersonate, username);

    JsonImpersonateUserResponse responseExit =
        POST("/auth/impersonateExit").content(HttpStatus.OK).as(JsonImpersonateUserResponse.class);

    assertEquals("IMPERSONATION_EXIT_SUCCESS", responseExit.getLoginStatus());
    assertEquals("admin", responseExit.getImpersonatedUsername());
  }

  @Test
  void testImpersonateExitNotImpersonating() {
    JsonWebMessage response =
        POST("/auth/impersonateExit").content(HttpStatus.BAD_REQUEST).as(JsonWebMessage.class);

    assertEquals(400, response.getHttpStatusCode());
    assertEquals("User not impersonating anyone, user: admin", response.getMessage());
    assertEquals("Bad Request", response.getHttpStatus());
    assertEquals("ERROR", response.getStatus());
  }

  @Test
  void testImpersonateUserOKWithAuth() {
    User userWithAuth = createUserWithAuth("authuser", "F_IMPERSONATE_USER");

    String usernameToImpersonate = "usera";
    createUserWithAuth(usernameToImpersonate, "SOME_AUTHORITY");

    injectSecurityContextUser(userWithAuth);
    JsonImpersonateUserResponse responseB =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.OK)
            .as(JsonImpersonateUserResponse.class);

    assertEquals("IMPERSONATION_SUCCESS", responseB.getLoginStatus());
    assertEquals(usernameToImpersonate, responseB.getImpersonatedUsername());
  }

  @Test
  void testImpersonateUserNonExistent() {
    String usernameToImpersonate = "dontexist";

    JsonWebMessage response =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.NOT_FOUND)
            .as(JsonWebMessage.class);

    assertEquals(404, response.getHttpStatusCode());
    assertEquals("Username not found: dontexist", response.getMessage());
    assertEquals("Not Found", response.getHttpStatus());
    assertEquals("ERROR", response.getStatus());
  }

  @Test
  void testImpersonateRoot() {
    String usernameToImpersonate = "admin";

    JsonWebMessage response =
        POST("/auth/impersonate?username=%s".formatted(usernameToImpersonate))
            .content(HttpStatus.FORBIDDEN)
            .as(JsonWebMessage.class);

    assertEquals(403, response.getHttpStatusCode());
    assertEquals("Forbidden, reason: User can not impersonate itself", response.getMessage());
    assertEquals("Forbidden", response.getHttpStatus());
    assertEquals("ERROR", response.getStatus());
  }

  @Test
  void testImpersonateNoAuthority() {
    User guestUser = createUserWithAuth("guestuser", "NONE");
    injectSecurityContextUser(guestUser);

    JsonWebMessage response =
        POST("/auth/impersonate?username=%s".formatted("admin"))
            .content(HttpStatus.FORBIDDEN)
            .as(JsonWebMessage.class);

    assertEquals(403, response.getHttpStatusCode());
    assertEquals(
        "Access is denied, requires one Authority from [F_IMPERSONATE_USER]",
        response.getMessage());
    assertEquals("Forbidden", response.getHttpStatus());
    assertEquals("ERROR", response.getStatus());
  }

  @Test
  void testImpersonateWithAuthorityNotAllowedToBecomeSuperuser() {
    User userWithAuth = createUserWithAuth("userwithauth", "F_IMPERSONATE_USER");
    injectSecurityContextUser(userWithAuth);

    JsonWebMessage response =
        POST("/auth/impersonate?username=%s".formatted("admin"))
            .content(HttpStatus.FORBIDDEN)
            .as(JsonWebMessage.class);

    assertEquals(403, response.getHttpStatusCode());
    assertEquals(
        "Forbidden, reason: User is not authorized to impersonate super user",
        response.getMessage());
    assertEquals("Forbidden", response.getHttpStatus());
    assertEquals("ERROR", response.getStatus());
  }
}
