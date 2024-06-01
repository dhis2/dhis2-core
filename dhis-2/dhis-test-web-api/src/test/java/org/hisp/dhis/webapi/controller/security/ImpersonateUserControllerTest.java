/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.ImpersonateUserControllerBaseTest;
import org.hisp.dhis.webapi.json.domain.JsonImpersonateUserResponse;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles({"test-h2", "impersonate-user-test"})
class ImpersonateUserControllerTest extends ImpersonateUserControllerBaseTest {

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
    assertEquals(usernameToImpersonate, me.getUsername());

    JsonImpersonateUserResponse responseExit =
        POST("/auth/impersonateExit").content(HttpStatus.OK).as(JsonImpersonateUserResponse.class);

    assertEquals("IMPERSONATION_EXIT_SUCCESS", responseExit.getLoginStatus());
    assertEquals("admin", responseExit.getImpersonatedUsername());
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
    assertEquals("Forbidden, requires authority [F_IMPERSONATE_USER]", response.getMessage());
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
