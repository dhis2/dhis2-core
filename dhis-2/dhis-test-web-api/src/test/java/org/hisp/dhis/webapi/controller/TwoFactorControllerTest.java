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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.security.TwoFactorController;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link TwoFactorController}
 *
 * @author Jan Bernitt
 * @author Morten Svanæs
 */
@Transactional
class TwoFactorControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private TwoFactorAuthService twoFactorAuthService;
  @Autowired private SystemSettingsService systemSettingsService;

  @Test
  void testEnrollTOTP2FA() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);

    switchToNewUser(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enrollTOTP2FA"));

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    assertNotNull(enrolledUser.getSecret());
    assertTrue(enrolledUser.getSecret().matches("^[a-zA-Z0-9]{32}$"));
    assertSame(TwoFactorType.ENROLLING_TOTP, enrolledUser.getTwoFactorType());

    HttpResponse res = GET("/2fa/showQRCodeAsJson");
    assertStatus(HttpStatus.OK, res);
    assertNotNull(res.content());

    JsonMixed content = res.content();
    JsonValue base32Secret = content.get("base32Secret");
    JsonValue base64QRImage = content.get("base64QRImage");

    assertTrue(base32Secret.isString());
    assertTrue(base64QRImage.isString());
  }

  @Test
  void testEnrollEmail2FA() {
    systemSettingsService.put("email2FAEnabled", "true");

    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.addUser(user);

    switchToNewUser(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enrollEmail2FA"));

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    assertNotNull(enrolledUser.getSecret());
    assertTrue(enrolledUser.getSecret().matches("^[0-9]{6}\\|\\d+$"));
    assertSame(TwoFactorType.ENROLLING_EMAIL, enrolledUser.getTwoFactorType());
  }

  @Test
  void testEnableTOTP2FA() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user);

    switchToNewUser(user);

    String code = generateTOTP2FACodeFromUserSecret(user);
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnableEmail2FA() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollEmail2FA(user);

    switchToNewUser(user);

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String secret = enrolledUser.getSecret();
    assertNotNull(secret);
    String[] codeAndTTL = secret.split("\\|");
    String code = codeAndTTL[0];

    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnableTOTP2FAWrongCode() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user);

    switchToNewUser(user);

    assertEquals(
        "Invalid 2FA code",
        POST("/2fa/enabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testQr2FAConflictMustDisableFirst() {
    assertNull(getCurrentUser().getSecret());

    User user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    twoFactorAuthService.enrollTOTP2FA(user);

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());

    String code = generateTOTP2FACodeFromUserSecret(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());
  }

  @Test
  void testEnable2FANotEnrolledFirst() {
    assertEquals(
        "User must start 2FA enrollment first",
        POST("/2fa/enable", "{'code':'wrong'}").error(HttpStatus.Series.CLIENT_ERROR).getMessage());
  }

  @Test
  void testDisableTOTP2FA() {
    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");

    userService.addUser(newUser);
    twoFactorAuthService.enrollTOTP2FA(newUser);
    twoFactorAuthService.setUserToEnabled2FA(newUser, new SystemUser());

    switchToNewUser(newUser);

    String code = generateTOTP2FACodeFromUserSecret(newUser);

    assertStatus(HttpStatus.OK, POST("/2fa/disable", "{'code':'" + code + "'}"));
  }

  @Test
  void testDisableEmail2FA() {
    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");
    newUser.setVerifiedEmail("valid.y@email.com");

    userService.addUser(newUser);
    twoFactorAuthService.enrollEmail2FA(newUser);
    twoFactorAuthService.setUserToEnabled2FA(newUser, new SystemUser());

    switchToNewUser(newUser);

    User enrolledUser = userService.getUserByUsername(newUser.getUsername());
    String secretAndTTL = enrolledUser.getSecret();
    String code = secretAndTTL.split("\\|")[0];

    assertStatus(HttpStatus.OK, POST("/2fa/disable", "{'code':'" + code + "'}"));

    User disabledUser = userService.getUserByUsername(newUser.getUsername());
    assertNull(disabledUser.getSecret());
  }

  @Test
  void testDisable2FANotEnabled() {
    assertEquals(
        "Two factor authentication is not enabled",
        POST("/2fa/disable", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testDisable2FATooManyTimes() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user);

    switchToNewUser(user);

    String code = generateTOTP2FACodeFromUserSecret(user);
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));

    assertStatus(HttpStatus.FORBIDDEN, POST("/2fa/disable", "{'code':'333333'}"));

    for (int i = 0; i < 3; i++) {
      assertWebMessage(
          "Forbidden",
          403,
          "ERROR",
          "Invalid 2FA code",
          POST("/2fa/disable", "{'code':'333333'}").content(HttpStatus.FORBIDDEN));
    }

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Too many failed disable attempts. Please try again later",
        POST("/2fa/disable", "{'code':'333333'}").content(HttpStatus.CONFLICT));
  }

  private static String generateTOTP2FACodeFromUserSecret(User newUser) {
    return new Totp(newUser.getSecret()).now();
  }
}
