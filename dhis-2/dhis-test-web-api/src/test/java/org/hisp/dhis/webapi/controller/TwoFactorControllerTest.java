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

import static org.hisp.dhis.user.UserService.TWO_FACTOR_CODE_APPROVAL_PREFIX;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.security.TwoFactorController;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link TwoFactorController} sing (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class TwoFactorControllerTest extends DhisControllerConvenienceTest {
  @Test
  void testQr2FaConflictMustDisableFirst() {
    assertNull(getCurrentUser().getSecret());

    User user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    userService.generateTwoFactorOtpSecretForApproval(user);

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());

    String code = getCode(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());
  }

  @Test
  void testEnable2Fa() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    userService.generateTwoFactorOtpSecretForApproval(user);

    switchToNewUser(user);

    String code = getCode(user);
    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnable2FaWrongCode() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    userService.generateTwoFactorOtpSecretForApproval(user);

    switchToNewUser(user);

    assertEquals(
        "Invalid 2FA code",
        POST("/2fa/enabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testEnable2FaNotCalledQrFirst() {
    assertEquals(
        "User must call the /qrCode endpoint first",
        POST("/2fa/enabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testDisable2Fa() {
    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");

    userService.addUser(newUser);
    userService.generateTwoFactorOtpSecretForApproval(newUser);
    userService.approveTwoFactorSecret(newUser);

    switchToNewUser(newUser);

    String code = getCode(newUser);

    assertStatus(HttpStatus.OK, POST("/2fa/disabled", "{'code':'" + code + "'}"));
  }

  @Test
  void testDisable2FaNotEnabled() {
    assertEquals(
        "Two factor authentication is not enabled",
        POST("/2fa/disabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  private static String replaceApprovalPartOfTheSecret(User user) {
    return user.getSecret().replace(TWO_FACTOR_CODE_APPROVAL_PREFIX, "");
  }

  private static String getCode(User newUser) {
    return new Totp(replaceApprovalPartOfTheSecret(newUser)).now();
  }
}
