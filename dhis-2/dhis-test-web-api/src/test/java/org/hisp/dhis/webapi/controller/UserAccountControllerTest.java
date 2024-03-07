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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.auth.SelfRegistrationParams;
import org.hisp.dhis.message.FakeMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class UserAccountControllerTest extends DhisControllerConvenienceTest {

  @Autowired private MessageSender messageSender;
  @Autowired private SystemSettingManager systemSettingManager;
  @Autowired private PasswordManager passwordEncoder;

  private String superUserRoleUid;

  @Test
  @DisplayName("Happy path for forgot password with username as input")
  void testResetPasswordOkUsername() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    User user = switchToNewUser("test");
    clearSecurityContext();
    sendForgotPasswordRequest(user.getUsername());
    doAndCheckPasswordResetWithUser(user);
  }

  @Test
  @DisplayName("Happy path for forgot password with email as input")
  void testResetPasswordOkEmail() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    User user = switchToNewUser("test");
    clearSecurityContext();
    sendForgotPasswordRequest(user.getEmail());
    doAndCheckPasswordResetWithUser(user);
  }

  @Test
  @DisplayName("Send wrong/non-existent email, should return OK to avoid email enumeration")
  void testResetPasswordWrongEmail() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    clearSecurityContext();
    sendForgotPasswordRequest("wrong@email.com");
    assertTrue(((FakeMessageSender) messageSender).getAllMessages().isEmpty());
  }

  @Test
  @DisplayName("Send wrong/non-existent username, should return OK to avoid username enumeration")
  void testResetPasswordWrongUsername() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    clearSecurityContext();
    sendForgotPasswordRequest("wrong");
    assertTrue(((FakeMessageSender) messageSender).getAllMessages().isEmpty());
  }

  private void doAndCheckPasswordResetWithUser(User user) {
    String token = fetchTokenInSentEmail(user);
    String newPassword = "Abxf123###...";
    POST(
            "/auth/passwordReset",
            "{'newPassword':'%s', 'resetToken':'%s'}".formatted(newPassword, token))
        .content(HttpStatus.OK);

    User updatedUser = userService.getUserByUsername(user.getUsername());
    boolean passwordMatch = passwordEncoder.matches(newPassword, updatedUser.getPassword());

    assertTrue(passwordMatch);
  }

  @Test
  @DisplayName("Self registration is allowed when no errors")
  void selfRegIsAllowed() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Account created",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationForm()))
            .content(HttpStatus.OK));
  }

  @Test
  @DisplayName("Self registration completes and user org unit is correct")
  void regCompleteAndUserConfigOk() {
    disableRecaptcha();
    OrganisationUnit organisationUnit = enableSelfRegistration();

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Account created",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationForm()))
            .content(HttpStatus.OK));

    JsonUser user = GET("/me").content(HttpStatus.OK).as(JsonUser.class);
    assertEquals(organisationUnit.getUid(), user.getOrganisationUnits().get(0).getId());
    assertEquals(
        superUserRoleUid, user.getArray("userRoles").getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("Self registration error when username is null")
  void selfRegUsernameNull() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Username is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithUsername(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when username exists")
  void selfRegUsernameExists() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Username is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithUsername(superUser.getUsername())))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when username invalid")
  void selfRegUsernameInvalid() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Username is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithUsername("..invalid username ..")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when first name null")
  void selfRegFirstNameNull() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "First name is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithFirstName(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when first name too long")
  void selfRegFirstNameTooLong() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "First name is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithFirstName(
                        "abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when surname null")
  void selfRegSurnameNull() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Surname is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithSurname(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when surname name too long")
  void selfRegSurnameTooLong() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Surname is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithSurname(
                        "abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @ParameterizedTest
  @MethodSource("passwordData")
  @DisplayName("Self registration error when invalid password data")
  void selfRegPasswordNull(String input, String expectedError) {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        expectedError,
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithPassword(input)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when null email")
  void selfRegNullEmail() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Email is not specified or invalid",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationFormWithEmail(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when invalid email")
  void selfRegInvalidEmail() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Email is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithEmail("invalidEmail")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when null phone number")
  void selfRegInvalidPhoneNumber() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Phone number is not specified or invalid",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationFormWithPhone(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when phone number too long")
  void selfRegPhoneNumberTooLong() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Phone number is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithPhone("12345678910, 12345678910, 12345678910")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when not enabled")
  void selfRegNotEnabled() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "User self registration is not allowed",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationForm()))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when invalid recaptcha input")
  void selfRegInvalidRecaptchaInput() {
    systemSettingManager.saveSystemSetting(
        SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.FALSE);

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Recaptcha validation failed: [invalid-input-secret]",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithRecaptcha("secret")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when recaptcha enabled and null input")
  void selfRegRecaptcha() {
    systemSettingManager.saveSystemSetting(
        SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.FALSE);

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Recaptcha validation failed.",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithRecaptcha(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  private OrganisationUnit enableSelfRegistration() {
    OrganisationUnit selfRegOrgUnit = createOrganisationUnit("test org 123");
    manager.save(selfRegOrgUnit);
    superUser.addOrganisationUnit(selfRegOrgUnit);

    superUserRoleUid = superUser.getUserRoles().iterator().next().getUid();
    POST("/configuration/selfRegistrationRole", superUserRoleUid).content(HttpStatus.NO_CONTENT);
    POST("/configuration/selfRegistrationOrgUnit", selfRegOrgUnit.getUid())
        .content(HttpStatus.NO_CONTENT);
    return selfRegOrgUnit;
  }

  private void disableRecaptcha() {
    systemSettingManager.saveSystemSetting(SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.TRUE);
  }

  private static Stream<Arguments> passwordData() {
    return Stream.of(
        arguments(null, "Password is not specified"),
        arguments("tester-dhis", "Password must have at least one digit"),
        arguments("samewisegamgee1", "Password must have at least one upper case"),
        arguments("samewisegamgeE1", "Password must have at least one special character"),
        arguments("samewisegamgeE1@", "Username/Email must not be a part of password"),
        arguments("samewise@dhis2.orG1@", "Username/Email must not be a part of password"),
        arguments("sA1@", "Password must have at least 8, and at most 72 characters"),
        arguments("sAdmin1@", "Password must not have any generic word"));
  }

  private void sendForgotPasswordRequest(String emailOrUsername) {
    POST("/auth/forgotPassword", "{'emailOrUsername':'%s'}".formatted(emailOrUsername))
        .content(HttpStatus.OK);
  }

  private String fetchTokenInSentEmail(User user) {
    OutboundMessage message = assertMessageSendTo(user.getEmail());
    Pattern pattern = Pattern.compile("\\?token=(.*?)\\n");
    Matcher matcher = pattern.matcher(message.getText());
    String token = "";
    if (matcher.find()) {
      token = matcher.group(1);
    }
    assertFalse(token.isEmpty());
    return token;
  }

  private OutboundMessage assertMessageSendTo(String email) {
    List<OutboundMessage> messagesByEmail =
        ((FakeMessageSender) messageSender).getMessagesByEmail(email);
    assertFalse(messagesByEmail.isEmpty());
    return messagesByEmail.get(0);
  }

  private SelfRegistrationParams getSelfRegistrationForm() {
    return SelfRegistrationParams.builder()
        .username("samewisegamgee")
        .firstName("samewise")
        .surname("gamgee")
        .password("Test123!")
        .email("samewise@dhis2.org")
        .phoneNumber("1234566-99")
        .recaptchaResponse("recaptcha response")
        .build();
  }

  private SelfRegistrationParams getSelfRegistrationFormWithUsername(String username) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setUsername(username);
    return selfRegistrationParams;
  }

  private SelfRegistrationParams getSelfRegistrationFormWithFirstName(String firstName) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setFirstName(firstName);
    return selfRegistrationParams;
  }

  private SelfRegistrationParams getSelfRegistrationFormWithSurname(String surname) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setSurname(surname);
    return selfRegistrationParams;
  }

  private SelfRegistrationParams getSelfRegistrationFormWithPassword(String password) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setPassword(password);
    return selfRegistrationParams;
  }

  private SelfRegistrationParams getSelfRegistrationFormWithEmail(String email) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setEmail(email);
    return selfRegistrationParams;
  }

  private SelfRegistrationParams getSelfRegistrationFormWithPhone(String phone) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setPhoneNumber(phone);
    return selfRegistrationParams;
  }

  private SelfRegistrationParams getSelfRegistrationFormWithRecaptcha(String recaptcha) {
    SelfRegistrationParams selfRegistrationParams = getSelfRegistrationForm();
    selfRegistrationParams.setRecaptchaResponse(recaptcha);
    return selfRegistrationParams;
  }
}
