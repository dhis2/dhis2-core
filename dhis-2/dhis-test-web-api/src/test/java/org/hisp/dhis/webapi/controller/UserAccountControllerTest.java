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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.auth.CompleteRegistrationParams;
import org.hisp.dhis.common.auth.SelfRegistrationParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.message.FakeMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
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
  void testResetPasswordOk() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);

    User test = switchToNewUser("test");

    clearSecurityContext();
    String token = sendForgotPasswordRequest(test);

    String newPassword = "Abxf123###...";

    POST(
            "/auth/passwordReset",
            "{'newPassword':'%s', 'resetToken':'%s'}".formatted(newPassword, token))
        .content(HttpStatus.OK);

    User updatedUser = userService.getUserByUsername(test.getUsername());

    boolean passwordMatch = passwordEncoder.matches(newPassword, updatedUser.getPassword());

    assertTrue(passwordMatch);
  }

  @Test
  @DisplayName("Self registration is allowed when no errors")
  void selfRegIsAllowed() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Created",
        201,
        "OK",
        "Account created",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getUserRegParams(new CompleteRegistrationParams())))
            .content(HttpStatus.CREATED));
  }

  @Test
  @DisplayName("Self registration completes and user org unit is correct")
  void regCompleteAndUserConfigOk() {
    disableRecaptcha();
    OrganisationUnit organisationUnit = enableSelfRegistration();

    assertWebMessage(
        "Created",
        201,
        "OK",
        "Account created",
        POST(
                "/auth/register",
                renderService.toJsonAsString(getUserRegParams(new CompleteRegistrationParams())))
            .content(HttpStatus.CREATED));

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
        POST("/auth/register", renderService.toJsonAsString(getRegParamsWithUsername(null)))
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
                renderService.toJsonAsString(getRegParamsWithUsername(superUser.getUsername())))
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
                renderService.toJsonAsString(getRegParamsWithUsername("..invalid username ..")))
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
                renderService.toJsonAsString(getRegParamsWithFirstName(null, RegType.SELF_REG)))
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
                    getRegParamsWithFirstName(StringUtils.repeat('a', 81), RegType.SELF_REG)))
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
                renderService.toJsonAsString(getRegParamsWithSurname(null, RegType.SELF_REG)))
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
                    getRegParamsWithSurname(StringUtils.repeat('a', 81), RegType.SELF_REG)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @ParameterizedTest
  @MethodSource("passwordData")
  @DisplayName("Self registration error when invalid password data")
  void selfRegInvalidPassword(String input, String expectedError) {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        expectedError,
        POST(
                "/auth/register",
                renderService.toJsonAsString(getRegParamsWithPassword(input, RegType.SELF_REG)))
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
        POST("/auth/register", renderService.toJsonAsString(getRegParamsWithEmail(null)))
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
        POST("/auth/register", renderService.toJsonAsString(getRegParamsWithEmail("invalidEmail")))
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
        POST(
                "/auth/register",
                renderService.toJsonAsString(getRegParamsWithPhone(null, RegType.SELF_REG)))
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
                    getRegParamsWithPhone(
                        "12345678910, 12345678910, 12345678910", RegType.SELF_REG)))
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
        POST(
                "/auth/register",
                renderService.toJsonAsString(getUserRegParams(new CompleteRegistrationParams())))
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
                renderService.toJsonAsString(getRegParamsWithRecaptcha("secret", RegType.SELF_REG)))
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
                renderService.toJsonAsString(getRegParamsWithRecaptcha(null, RegType.SELF_REG)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration completes and user name is correct")
  void inviteCompleteAndUsernameOk() {
    disableRecaptcha();
    enableSelfRegistration();
    // setup user as admin
    User adminCreatedUser = getAdminCreatedUser();
    POST("/users", renderService.toJsonAsString(adminCreatedUser)).content(HttpStatus.CREATED);

    switchContextToUser(adminCreatedUser);
    GET("/me").content(HttpStatus.OK);

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Account created",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getInviteRegistrationForm()))
            .content(HttpStatus.OK));

    JsonUser user = GET("/me").content(HttpStatus.OK).as(JsonUser.class);
    assertEquals("samewisegamgee", user.getUsername());
  }

  @Test
  @DisplayName("Invite registration error when first name is null")
  void inviteRegFirstNameNull() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "First name is not specified or invalid",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getRegParamsWithFirstName(null, RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when surname is null")
  void inviteRegSurnameNull() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Surname is not specified or invalid",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getRegParamsWithSurname(null, RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when first name too long")
  void inviteRegFirstNameTooLong() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "First name is not specified or invalid",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(
                    getRegParamsWithFirstName(StringUtils.repeat('a', 81), RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when surname name too long")
  void inviteRegSurnameTooLong() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Surname is not specified or invalid",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(
                    getRegParamsWithSurname(StringUtils.repeat('a', 81), RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @ParameterizedTest
  @MethodSource("passwordData")
  @DisplayName("Invite registration error when invalid password data")
  void inviteRegInvalidPassword(String password, String expectedError) {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        expectedError,
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getRegParamsWithPassword(password, RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when null phone number")
  void inviteRegInvalidPhoneNumber() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Phone number is not specified or invalid",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getRegParamsWithPhone(null, RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when phone number too long")
  void inviteRegPhoneNumberTooLong() {
    disableRecaptcha();

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Phone number is not specified or invalid",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(
                    getRegParamsWithPhone("12345678910, 12345678910, 12345678910", RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when invalid recaptcha input")
  void inviteRegInvalidRecaptchaInput() {
    systemSettingManager.saveSystemSetting(
        SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.FALSE);

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Recaptcha validation failed: [invalid-input-secret]",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getRegParamsWithRecaptcha("secret", RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when recaptcha enabled and null input")
  void inviteRegRecaptcha() {
    systemSettingManager.saveSystemSetting(
        SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.FALSE);

    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Recaptcha validation failed.",
        POST(
                "/auth/completeRegistration",
                renderService.toJsonAsString(getRegParamsWithRecaptcha(null, RegType.INVITE)))
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

  private UserRegistrationParams getUserRegParams(UserRegistrationParams userParams) {
    userParams.setUsername("samewisegamgee");
    userParams.setFirstName("samewise");
    userParams.setSurname("gamgee");
    userParams.setPassword("Test123!");
    userParams.setEmail("samewise@dhis2.org");
    userParams.setPhoneNumber("1234566-99");
    userParams.setRecaptchaResponse("recaptcha response");
    return userParams;
  }

  private CompleteRegistrationParams getInviteRegistrationForm() {
    CompleteRegistrationParams regParams =
        (CompleteRegistrationParams) getUserRegParams(new CompleteRegistrationParams());
    // this Base64 encoded string needs to start with 'ID' for this invited user test
    // it's part of the recaptcha validation in RestoreOptions#getRestoreOptions
    // this unencoded string is 'idToken:IDrestoreToken'
    regParams.setToken("aWRUb2tlbjpJRHJlc3RvcmVUb2tlbg==");
    return regParams;
  }

  private UserRegistrationParams getRegParamsWithUsername(String username) {
    UserRegistrationParams selfRegistrationParams = getUserRegParams(new SelfRegistrationParams());
    selfRegistrationParams.setUsername(username);
    return selfRegistrationParams;
  }

  private UserRegistrationParams getRegParamsWithFirstName(String firstName, RegType regType) {
    UserRegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setFirstName(firstName);
    return regParams;
  }

  private UserRegistrationParams getRegParamsWithSurname(String surname, RegType regType) {
    UserRegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setSurname(surname);
    return regParams;
  }

  private UserRegistrationParams getRegParamsWithPassword(String password, RegType regType) {
    UserRegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setPassword(password);
    return regParams;
  }

  private UserRegistrationParams getRegParamsWithEmail(String email) {
    UserRegistrationParams regParams = getUserRegParams(new SelfRegistrationParams());
    regParams.setEmail(email);
    return regParams;
  }

  private UserRegistrationParams getRegParamsWithPhone(String phone, RegType regType) {
    UserRegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setPhoneNumber(phone);
    return regParams;
  }

  private UserRegistrationParams getRegParamsWithRecaptcha(String recaptcha, RegType regType) {
    UserRegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setRecaptchaResponse(recaptcha);
    return regParams;
  }

  private User getAdminCreatedUser() {
    User user = new User();
    user.setUid("Uid00000001");
    user.setName("samewise gamgee");
    user.setUsername("samewisegamgee");
    user.setFirstName("samwise");
    user.setSurname("gamgee");
    user.setPassword("Test123!");
    user.setUserRoles(superUser.getUserRoles());
    user.setIdToken("idToken");
    // This hashed string (when matched with raw password) needs to match the password that the
    // invited user will pass when completing their invited registration.
    // Use passwordEncoder.encode("Test123!") in debug to get the expected hash
    user.setRestoreToken("$2a$10$fScYIKiJx6sBWBm/U0QgR.fPlLJeMXOu0CmuharO7v5XVOSZRZ.p.");
    user.setRestoreExpiry(DateUtils.getDate(2040, 11, 22, 4, 20));
    return user;
  }

  private UserRegistrationParams getParamsFromType(RegType regParamsType) {
    return switch (regParamsType) {
      case SELF_REG -> new SelfRegistrationParams();
      case INVITE -> new CompleteRegistrationParams();
    };
  }

  enum RegType {
    SELF_REG,
    INVITE
  }
}
