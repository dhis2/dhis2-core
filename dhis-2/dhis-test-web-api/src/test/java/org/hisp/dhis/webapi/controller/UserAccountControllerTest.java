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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.auth.SelfRegistrationForm;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.message.FakeMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class UserAccountControllerTest extends DhisControllerIntegrationTest {

  @Autowired private MessageSender messageSender;
  @Autowired private SystemSettingManager systemSettingManager;
  @Autowired private PasswordManager passwordEncoder;

  private OrganisationUnit selfRegOrgUnit;

  //  @BeforeAll
  //  public static void before() {}

  @Test
  @Disabled(
      "This test is only failing in Jenkins, temp disable in master while debugging on separate Jenkins only branch")
  void testResetPasswordOk() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);

    User test = switchToNewUser("test");

    clearSecurityContext();
    String token = sendForgotPasswordRequest(test);

    String newPassword = "Abxf123###...";

    HttpResponse response =
        POST(
            "/auth/passwordReset",
            "{'newPassword':'%s', 'resetToken':'%s'}".formatted(newPassword, token));

    JsonMixed jsonValues = response.contentUnchecked();

    log.error("jsonValues: {}", jsonValues);

    response.content(HttpStatus.OK);

    User updatedUser = userService.getUserByUsername(test.getUsername());

    boolean passwordMatch = passwordEncoder.matches(newPassword, updatedUser.getPassword());

    assertTrue(passwordMatch);
  }

  @Test
  @DisplayName("Self registration is allowed when no errors")
  void selfRegIsAllowed() {
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
  @DisplayName("Self registration error when username is null")
  void selfRegUsernameNull() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Username is not specified or invalid",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationFormNullUsername()))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when username exists")
  void selfRegUsernameExists() {
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
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Last name is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithSurname(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when first name too long")
  void selfRegSurnameTooLong() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Last name is not specified or invalid",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithSurname(
                        "abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz,abcdefghijklmnopqrstuvwxyz")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password null")
  void selfRegPasswordNull() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Password is not specified",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithPassword(null)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password must have 1 digit")
  void selfRegPasswordNoDigit() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Password must have at least one digit",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithPassword("tester-dhis")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password has no uppercase")
  void selfRegPasswordNoUppercase() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Password must have at least one upper case",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithPassword("samewisegamgee1")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password has no special char")
  void selfRegPasswordCNoSpecialChar() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Password must have at least one special character",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithPassword("samewisegamgeE1")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password contains username")
  void selfRegPasswordContainsUsername() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Username/Email must not be a part of password",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithPassword("samewisegamgeE1@")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password contains email")
  void selfRegPasswordContainsEmail() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Username/Email must not be a part of password",
        POST(
                "/auth/register",
                renderService.toJsonAsString(
                    getSelfRegistrationFormWithPassword("samewise@dhis2.orG1@")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password too short")
  void selfRegPasswordTooShort() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Password must have at least 8, and at most 72 characters",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithPassword("sA1@")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when password contains key word")
  void selfRegPasswordKeyWord() {
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Password must not have any generic word",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getSelfRegistrationFormWithPassword("sAdmin1@")))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when null email")
  void selfRegNullEmail() {
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
    systemSettingManager.saveSystemSetting(SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.TRUE);

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "User self registration is not allowed",
        POST("/auth/register", renderService.toJsonAsString(getSelfRegistrationForm()))
            .content(HttpStatus.BAD_REQUEST));
  }

  // TODO add test to login as new user and check default self reg org unit

  private void enableSelfRegistration() {
    OrganisationUnit selfRegOrgUnit = createOrganisationUnit("test org 123");
    manager.save(selfRegOrgUnit);
    superUser.addOrganisationUnit(selfRegOrgUnit);

    String superUserRoleUid = superUser.getUserRoles().iterator().next().getUid();
    systemSettingManager.saveSystemSetting(SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.TRUE);
    POST("/configuration/selfRegistrationRole", superUserRoleUid).content(HttpStatus.NO_CONTENT);
    POST("/configuration/selfRegistrationOrgUnit", selfRegOrgUnit.getUid())
        .content(HttpStatus.NO_CONTENT);
  }

  private String sendForgotPasswordRequest(User test) {
    POST("/auth/forgotPassword", "{'username':'%s'}".formatted(test.getUsername()))
        .content(HttpStatus.OK);

    OutboundMessage message = assertMessageSendTo(test.getEmail());

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

  private SelfRegistrationForm getSelfRegistrationForm() {
    return SelfRegistrationForm.builder()
        .username("samewisegamgee")
        .firstName("samewise")
        .surname("gamgee")
        .password("Test123!")
        .email("samewise@dhis2.org")
        .phoneNumber("1234566-99")
        .recaptchaResponse("recaptcha response")
        .build();
  }

  private SelfRegistrationForm getSelfRegistrationFormNullUsername() {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setUsername(null);
    return selfRegistrationFormBuilder;
  }

  private SelfRegistrationForm getSelfRegistrationFormWithUsername(String username) {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setUsername(username);
    return selfRegistrationFormBuilder;
  }

  private SelfRegistrationForm getSelfRegistrationFormWithFirstName(String firstName) {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setFirstName(firstName);
    return selfRegistrationFormBuilder;
  }

  private SelfRegistrationForm getSelfRegistrationFormWithSurname(String surname) {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setSurname(surname);
    return selfRegistrationFormBuilder;
  }

  private SelfRegistrationForm getSelfRegistrationFormWithPassword(String password) {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setPassword(password);
    return selfRegistrationFormBuilder;
  }

  private SelfRegistrationForm getSelfRegistrationFormWithEmail(String email) {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setEmail(email);
    return selfRegistrationFormBuilder;
  }

  private SelfRegistrationForm getSelfRegistrationFormWithPhone(String phone) {
    SelfRegistrationForm selfRegistrationFormBuilder = getSelfRegistrationForm();
    selfRegistrationFormBuilder.setPhoneNumber(phone);
    return selfRegistrationFormBuilder;
  }
}
