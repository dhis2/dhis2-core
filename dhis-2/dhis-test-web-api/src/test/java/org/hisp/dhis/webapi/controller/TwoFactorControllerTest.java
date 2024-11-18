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
import static org.hisp.dhis.user.UserService.TWO_FACTOR_CODE_APPROVAL_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.security.TwoFactorController;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link TwoFactorController}
 *
 * @author Jan Bernitt
 * @author Morten Svanæs
 */
@Transactional
class TwoFactorControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testEnableTOTP2FA() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    userService.enrollTOTP2FA(user);

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
    userService.enrollEmail2FA(user);

    switchToNewUser(user);

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String secret = enrolledUser.getSecret();
    assertNotNull(secret);
    String codeTTL = replaceApprovalPartOfTheSecret(secret);
    String[] codeAndTTL = codeTTL.split("\\|");
    String code = codeAndTTL[0];

    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnableTOTP2FAWrongCode() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    userService.enrollTOTP2FA(user);

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
    userService.enrollTOTP2FA(user);

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());

    String code = generateTOTP2FACodeFromUserSecret(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());
  }

  @Test
  void testEnable2FANotCalledQrFirst() {
    assertEquals(
        "User must call the /qrCode endpoint first",
        POST("/2fa/enabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testDisable2FA() {
    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");

    userService.addUser(newUser);
    userService.enrollTOTP2FA(newUser);
    userService.approveTwoFactorSecret(newUser, new SystemUser());

    switchToNewUser(newUser);

    String code = generateTOTP2FACodeFromUserSecret(newUser);

    assertStatus(HttpStatus.OK, POST("/2fa/disabled", "{'code':'" + code + "'}"));
  }

  @Test
  void testDisable2FANotEnabled() {
    assertEquals(
        "Two factor authentication is not enabled",
        POST("/2fa/disabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testDisable2FATooManyTimes() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    userService.enrollTOTP2FA(user);

    switchToNewUser(user);

    String code = generateTOTP2FACodeFromUserSecret(user);
    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));

    assertStatus(HttpStatus.UNAUTHORIZED, POST("/2fa/disabled", "{'code':'333333'}"));

    for (int i = 0; i < 3; i++) {
      assertWebMessage(
          "Unauthorized",
          401,
          "ERROR",
          "Invalid 2FA code",
          POST("/2fa/disabled", "{'code':'333333'}").content(HttpStatus.UNAUTHORIZED));
    }

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Too many failed disable attempts. Please try again later",
        POST("/2fa/disabled", "{'code':'333333'}").content(HttpStatus.CONFLICT));
  }

  private static String replaceApprovalPartOfTheSecret(String secret) {
    return secret.replace(TWO_FACTOR_CODE_APPROVAL_PREFIX, "");
  }

  private static String generateTOTP2FACodeFromUserSecret(User newUser) {
    return new Totp(replaceApprovalPartOfTheSecret(newUser.getSecret())).now();
  }
}
