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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link AccountController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class AccountControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private SystemSettingManager systemSettingManager;

  @Test
  void testRecoverAccount_NotEnabled() {
    User test = switchToNewUser("test");
    switchToSuperuser();
    clearSecurityContext();
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Account recovery is not enabled",
        POST("/account/recovery?username=" + test.getUsername()).content(HttpStatus.CONFLICT));
  }

  @Test
  void testRestoreAccount_InvalidTokenPassword() {
    clearSecurityContext();
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Account recovery failed",
        POST("/account/restore?token=xyz&password=secret").content(HttpStatus.CONFLICT));
  }

  @Test
  void testRecoverAccount_UsernameNotExist() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, Boolean.TRUE);
    clearSecurityContext();
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "User does not exist: BART",
        POST("/account/recovery?username=BART").content(HttpStatus.CONFLICT));
  }

  @Test
  void testRecoverAccount_NotValidEmail() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, Boolean.TRUE);
    clearSecurityContext();
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "User account does not have a valid email address",
        POST("/account/recovery?username=" + superUser.getUsername()).content(HttpStatus.CONFLICT));
  }

  @Test
  void testRecoverAccount_OK() {
    switchToNewUser("test");
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, Boolean.TRUE);
    clearSecurityContext();
    POST("/account/recovery?username=test").content(HttpStatus.OK);
  }

  @Test
  void testCreateAccount() {
    systemSettingManager.saveSystemSetting(SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.TRUE);
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "User self registration is not allowed",
        POST("/account?username=test&firstName=fn&surname=sn&password=Pass%23%23%23123663&email=test@example.com&phoneNumber=0123456789&employer=em")
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testUpdatePassword_NonExpired() {
    assertMessage(
        "status",
        "NON_EXPIRED",
        "Account is not expired, redirecting to login.",
        POST("/account/password?username=admin&oldPassword=xyz&password=abc")
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testValidateUserNameGet_UserNameAvailable() {
    assertMessage(
        "response", "success", "", GET("/account/username?username=abcd").content(HttpStatus.OK));
  }

  @Test
  void testValidateUserNameGet_UserNameTaken() {
    assertMessage(
        "response",
        "error",
        "Username is already taken",
        GET("/account/username?username=admin").content(HttpStatus.OK));
  }

  @Test
  void testValidateUserNamePost_UserNameAvailable() {
    assertMessage(
        "response",
        "success",
        "",
        POST("/account/validateUsername?username=abcd").content(HttpStatus.OK));
  }

  @Test
  void testValidateUserNamePost_UserNameTaken() {
    assertMessage(
        "response",
        "error",
        "Username is already taken",
        POST("/account/validateUsername?username=admin").content(HttpStatus.OK));
  }

  @Test
  void testValidatePasswordGet_PasswordValid() {
    assertMessage(
        "response",
        "success",
        "",
        GET("/account/password?password=Sup€rSecre1").content(HttpStatus.OK));
  }

  @Test
  void testValidatePasswordGet_PasswordNotValid() {
    assertMessage(
        "response",
        "error",
        "Password must have at least 8, and at most 72 characters",
        GET("/account/password?password=xyz").content(HttpStatus.OK));
  }

  @Test
  void testValidatePasswordPost_PasswordValid() {
    assertMessage(
        "response",
        "success",
        "",
        POST("/account/validatePassword?password=Sup€rSecre1").content(HttpStatus.OK));
  }

  @Test
  void testValidatePasswordPost_PasswordNotValid() {
    assertMessage(
        "response",
        "error",
        "Password must have at least 8, and at most 72 characters",
        POST("/account/validatePassword?password=xyz").content(HttpStatus.OK));
  }

  @Test
  void testGetLinkedAccounts() {
    String openId = "email@provider.com";
    List<User> allUsers = userService.getAllUsers();
    for (User user : allUsers) {
      user.setOpenId(openId);
      userService.updateUser(user);
    }

    JsonMixed response = GET("/account/linkedAccounts").content(HttpStatus.OK);
    JsonList<JsonObject> users = response.getList("users", JsonObject.class);
    assertEquals(2, users.size());
  }

  private static void assertMessage(String key, String value, String message, JsonMixed response) {
    assertContainsOnly(Set.of(key, "message"), response.names());
    assertEquals(value, response.getString(key).string());
    assertEquals(message, response.getString("message").string());
  }
}
