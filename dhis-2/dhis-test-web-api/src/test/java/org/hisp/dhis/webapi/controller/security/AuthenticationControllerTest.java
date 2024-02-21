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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Calendar;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetailsImpl;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisAuthenticationApiTest;
import org.hisp.dhis.webapi.json.domain.JsonLoginResponse;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class AuthenticationControllerTest extends DhisAuthenticationApiTest {

  @Autowired SystemSettingManager systemSettingManager;
  @Autowired private SessionRegistry sessionRegistry;

  @Test
  void testSuccessfulLogin() {
    JsonLoginResponse response =
        POST("/auth/login", "{'username':'admin','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("SUCCESS", response.getLoginStatus());
    assertEquals("/dhis-web-dashboard", response.getRedirectUrl());
  }

  @Test
  void testWrongUsernameOrPassword() {
    User userA = createUserWithAuth("userb", "ALL");
    injectSecurityContextUser(userA);

    clearSecurityContext();
    JsonWebMessage response =
        POST("/auth/login", "{'username':'userb','password':'district9'}")
            .content(HttpStatus.UNAUTHORIZED)
            .as(JsonWebMessage.class);

    assertEquals("Bad credentials", response.getMessage());
    assertEquals("Unauthorized", response.getHttpStatus());
    assertEquals(401, response.getHttpStatusCode());
    assertEquals("ERROR", response.getStatus());
  }

  @Test
  void testLoginWith2FAEnrolmentUser() throws Exception {
    User userA = createUserWithAuth("usera", "ALL");
    injectSecurityContextUser(userA);

    mvc.perform(
            get("/2fa/qrCode")
                .contentType("application/octet-stream")
                .accept("application/octet-stream"))
        .andExpect(status().isAccepted());

    JsonLoginResponse wrong2FaCodeResponse =
        POST("/auth/login", "{'username':'usera','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("REQUIRES_TWO_FACTOR_ENROLMENT", wrong2FaCodeResponse.getLoginStatus());
    assertNull(wrong2FaCodeResponse.getRedirectUrl());
  }

  @Test
  void testLoginWith2FAEnabledUser() {
    User admin = userService.getUserByUsername("admin");
    String secret = Base32.random();
    admin.setSecret(secret);
    userService.updateUser(admin);

    JsonLoginResponse wrong2FaCodeResponse =
        POST("/auth/login", "{'username':'admin','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("INCORRECT_TWO_FACTOR_CODE", wrong2FaCodeResponse.getLoginStatus());
    Assertions.assertNull(wrong2FaCodeResponse.getRedirectUrl());

    validateTOTP(secret);
  }

  @Test
  void testLoginWithLockedUser() {
    systemSettingManager.saveSystemSetting(SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, true);

    User admin = userService.getUserByUsername("admin");
    userService.updateUser(admin);
    userService.registerFailedLogin(admin.getUsername());
    userService.registerFailedLogin(admin.getUsername());
    userService.registerFailedLogin(admin.getUsername());
    userService.registerFailedLogin(admin.getUsername());
    userService.registerFailedLogin(admin.getUsername());

    JsonLoginResponse loginResponse =
        POST("/auth/login", "{'username':'admin','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("ACCOUNT_LOCKED", loginResponse.getLoginStatus());
    assertNull(loginResponse.getRedirectUrl());
  }

  @Test
  void testLoginWithDisabledUser() {
    User admin = userService.getUserByUsername("admin");
    admin.setDisabled(true);
    userService.updateUser(admin);

    JsonLoginResponse loginResponse =
        POST("/auth/login", "{'username':'admin','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("ACCOUNT_DISABLED", loginResponse.getLoginStatus());
    assertNull(loginResponse.getRedirectUrl());
  }

  @Test
  void testLoginWithCredentialsExpiredUser() {
    systemSettingManager.saveSystemSetting(SettingKey.CREDENTIALS_EXPIRES, 1);

    User admin = userService.getUserByUsername("admin");

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(admin.getPasswordLastUpdated());
    calendar.add(Calendar.MONTH, -2);

    admin.setPasswordLastUpdated(calendar.getTime());
    userService.updateUser(admin);

    JsonLoginResponse loginResponse =
        POST("/auth/login", "{'username':'admin','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("PASSWORD_EXPIRED", loginResponse.getLoginStatus());
    assertNull(loginResponse.getRedirectUrl());
  }

  @Test
  void testLoginWithAccountExpiredUser() {
    User admin = userService.getUserByUsername("admin");

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(admin.getPasswordLastUpdated());
    calendar.add(Calendar.MONTH, -2);

    admin.setAccountExpiry(calendar.getTime());
    userService.updateUser(admin);

    JsonLoginResponse loginResponse =
        POST("/auth/login", "{'username':'admin','password':'district'}")
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);

    assertEquals("ACCOUNT_EXPIRED", loginResponse.getLoginStatus());
    assertNull(loginResponse.getRedirectUrl());
  }

  @Test
  void testSessionGetsCreated() {
    clearSecurityContext();

    HttpResponse response = POST("/auth/login", "{'username':'admin','password':'district'}");
    assertNotNull(response);

    assertEquals(1, sessionRegistry.getAllPrincipals().size());
    UserDetailsImpl actual = (UserDetailsImpl) sessionRegistry.getAllPrincipals().get(0);

    assertNotNull(actual);
    assertEquals("admin", actual.getUsername());
  }

  // test redirect to login page when not logged in, remember url befire login...

  private void validateTOTP(String secret) {
    Totp totp = new Totp(secret);
    String code = totp.now();
    JsonLoginResponse ok2FaCodeResponse =
        POST(
                "/auth/login",
                "{'username':'admin','password':'district','twoFactorCode':'%s'}".formatted(code))
            .content(HttpStatus.OK)
            .as(JsonLoginResponse.class);
    assertEquals("SUCCESS", ok2FaCodeResponse.getLoginStatus());
    assertEquals("/dhis-web-dashboard", ok2FaCodeResponse.getRedirectUrl());
  }
}
