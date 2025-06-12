/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link AccountController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class AccountControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private SystemSettingsService settingsService;
  @Autowired private MessageSender emailMessageSender;

  @AfterEach
  void afterEach() {
    emailMessageSender.clearMessages();
  }

  @Test
  void testRecoverAccount_NotEnabled() {
    settingsService.put("keyAccountRecovery", false);
    User test = switchToNewUser("test");
    switchToAdminUser();
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
    settingsService.put("keyAccountRecovery", true);
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
    settingsService.put("keyAccountRecovery", true);
    clearSecurityContext();
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "User account does not have a valid email address",
        POST("/account/recovery?username=" + getAdminUser().getUsername())
            .content(HttpStatus.CONFLICT));
  }

  @Test
  void testRecoverAccount_OK() {
    switchToNewUser("test");
    settingsService.put("keyAccountRecovery", true);
    clearSecurityContext();
    POST("/account/recovery?username=test").content(HttpStatus.OK);
  }

  @Test
  void testCreateAccount() {
    settingsService.put("keySelfRegistrationNoRecaptcha", true);
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
    POST("/systemSettings/maxPasswordLength", "72").content(HttpStatus.OK);
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
    POST("/systemSettings/maxPasswordLength", "72").content(HttpStatus.OK);
    assertMessage(
        "response",
        "error",
        "Password must have at least 8, and at most 72 characters",
        POST("/account/validatePassword?password=xyz").content(HttpStatus.OK));
  }

  @Test
  void testGetLinkedAccounts() {
    createUserWithAuth("usera");
    createUserWithAuth("userb");

    String openId = "email@provider.com";
    List<User> allUsers = userService.getAllUsers();
    for (User user : allUsers) {
      user.setOpenId(openId);
      userService.updateUser(user);
    }

    JsonMixed response = GET("/account/linkedAccounts").content(HttpStatus.OK);
    JsonList<JsonObject> users = response.getList("users", JsonObject.class);
    assertEquals(3, users.size());
  }

  @Test
  void testVerifyEmailWithTokenTwice() {
    settingsService.put("keyEmailHostName", "mail.example.com");
    settingsService.put("keyEmailUsername", "mailer");

    User user = switchToNewUser("kent");

    String emailAddress = user.getEmail();
    assertStatus(HttpStatus.CREATED, POST("/account/sendEmailVerification"));
    OutboundMessage emailMessage = assertMessageSendTo(emailAddress);
    String token = extractTokenFromEmailText(emailMessage.getText());
    assertNotNull(token);

    HttpResponse success = GET("/account/verifyEmail?token=" + token);
    assertStatus(HttpStatus.FOUND, success);
    assertEquals(
        "http://localhost/login/#/email-verification-success", success.header("Location"));
    user = userService.getUser(user.getId());
    assertTrue(userService.isEmailVerified(user));

    HttpResponse failure = GET("/account/verifyEmail?token=" + token);
    assertStatus(HttpStatus.FOUND, failure);
    assertEquals(
        "http://localhost/login/#/email-verification-failure", failure.header("Location"));
  }

  @Test
  void testSendEmailVerification() {
    settingsService.put("keyEmailHostName", "mail.example.com");
    settingsService.put("keyEmailUsername", "mailer");

    User user = switchToNewUser("clark");

    String emailAddress = user.getEmail();
    assertStatus(HttpStatus.CREATED, POST("/account/sendEmailVerification"));
    OutboundMessage emailMessage = assertMessageSendTo(emailAddress);
    String token = extractTokenFromEmailText(emailMessage.getText());
    assertNotNull(token);

    user = userService.getUser(user.getId());
    assertFalse(userService.isEmailVerified(user));
  }

  @Test
  void testVerifyEmailWithToken() {
    settingsService.put("keyEmailHostName", "mail.example.com");
    settingsService.put("keyEmailUsername", "mailer");

    User user = switchToNewUser("lex");

    String emailAddress = user.getEmail();
    assertStatus(HttpStatus.CREATED, POST("/account/sendEmailVerification"));
    OutboundMessage emailMessage = assertMessageSendTo(emailAddress);
    String token = extractTokenFromEmailText(emailMessage.getText());
    assertValidEmailVerificationToken(token);

    HttpResponse success = GET("/account/verifyEmail?token=" + token);
    assertStatus(HttpStatus.FOUND, success);
    assertEquals(
        "http://localhost/login/#/email-verification-success", success.header("Location"));
    user = userService.getUser(user.getId());
    assertTrue(userService.isEmailVerified(user));
  }

  @Test
  void testVerifyWithBadToken() {
    switchToNewUser("zod");
    HttpResponse response = GET("/account/verifyEmail?token=WRONGTOKEN");
    assertStatus(HttpStatus.FOUND, response);
    String location = response.header("Location");
    assertEquals("http://localhost/login/#/email-verification-failure", location);
  }

  @Test
  void testSendEmailWithoutEmail() {
    User user = switchToNewUser("metallo");
    user.setEmail(null);
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "User has no email set",
        POST("/account/sendEmailVerification").content(HttpStatus.CONFLICT));
  }

  @Test
  void testSendEmailIsAlreadyVerifiedBySameUser() {
    User user = switchToNewUser("brainiac");
    user.setVerifiedEmail(user.getEmail());
    userService.updateUser(user);
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "User has already verified the email address",
        POST("/account/sendEmailVerification").content(HttpStatus.CONFLICT));
  }

  @Test
  void testSendEmailIsAlreadyVerifiedByAnotherUser() {
    User user = switchToNewUser("doomsday");

    user.setVerifiedEmail(user.getEmail());
    userService.updateUser(user);

    User anotherUser = switchToNewUser("ultraman");
    anotherUser.setEmail(user.getEmail());

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "The email the user is trying to verify is already verified by another account",
        POST("/account/sendEmailVerification").content(HttpStatus.CONFLICT));
  }

  private void assertValidEmailVerificationToken(String token) {
    User user = userService.getUserByEmailVerificationToken(token);
    assertNotNull(user);
  }

  private OutboundMessage assertMessageSendTo(String email) {
    List<OutboundMessage> messagesByEmail = emailMessageSender.getMessagesByEmail(email);
    assertFalse(messagesByEmail.isEmpty());
    return messagesByEmail.get(0);
  }

  private String extractTokenFromEmailText(String message) {
    int tokenPos = message.indexOf("?token=");
    return message.substring(tokenPos + 7, message.indexOf('\n', tokenPos)).trim();
  }

  private static void assertMessage(String key, String value, String message, JsonMixed response) {
    assertContainsOnly(Set.of(key, "message"), response.names());
    assertEquals(value, response.getString(key).string());
    assertEquals(message, response.getString("message").string());
  }
}
