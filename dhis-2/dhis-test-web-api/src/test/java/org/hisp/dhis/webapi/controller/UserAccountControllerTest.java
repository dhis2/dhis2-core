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

import static org.hisp.dhis.utils.Assertions.assertContains;
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
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserInviteParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
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
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
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
  @Autowired private DhisConfigurationProvider configurationProvider;

  private String superUserRoleUid;

  @BeforeEach
  final void setupHere() throws Exception {
    ((FakeMessageSender) messageSender).clearMessages();
    configurationProvider
        .getProperties()
        .put(ConfigurationKey.SERVER_BASE_URL.getKey(), "http://localhost:8080");
  }

  @Test
  @DisplayName("Happy path for forgot password with username as input")
  void testResetPasswordOkUsername() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    User user = switchToNewUser("testA");
    clearSecurityContext();
    sendForgotPasswordRequest(user.getUsername());
    doAndCheckPasswordResetWithUser(user);
  }

  @Test
  @DisplayName("Happy path for forgot password with email as input")
  void testResetPasswordOkEmail() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    User user = switchToNewUser("testB");
    clearSecurityContext();
    sendForgotPasswordRequest(user.getEmail());
    doAndCheckPasswordResetWithUser(user);
  }

  @Test
  @DisplayName(
      "Send wrong/non-existent email, should return OK to avoid email enumeration and not send any email")
  void testResetPasswordWrongEmail() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    clearSecurityContext();
    sendForgotPasswordRequest("wrong@email.com");
    assertTrue(((FakeMessageSender) messageSender).getAllMessages().isEmpty());
  }

  @Test
  @DisplayName(
      "Send wrong/non-existent username, should return OK to avoid username enumeration and not send any email")
  void testResetPasswordWrongUsername() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    clearSecurityContext();
    sendForgotPasswordRequest("wrong");
    List<OutboundMessage> allMessages = ((FakeMessageSender) messageSender).getAllMessages();
    assertTrue(allMessages.isEmpty());
  }

  @Test
  @DisplayName(
      "Send non-unique email, should return OK to avoid username enumeration and not send any email")
  void testResetPasswordNonUniqueEmail() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);

    switchContextToUser(superUser);
    User userA = createUserWithAuth("userA");
    userA.setEmail("same@email.no");
    User userB = createUserWithAuth("userB");
    userB.setEmail("same@email.no");

    clearSecurityContext();
    sendForgotPasswordRequest("wrong");
    List<OutboundMessage> allMessages = ((FakeMessageSender) messageSender).getAllMessages();
    assertTrue(allMessages.isEmpty());
  }

  @Test
  @DisplayName(
      "Try to reset password for external auth user, should return OK to avoid username enumeration and not send any email")
  void testResetPasswordExternalAuthUser() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    clearSecurityContext();
    User user = switchToNewUser("testC");
    user.setExternalAuth(true);
    userService.updateUser(user);

    sendForgotPasswordRequest("testC");
    List<OutboundMessage> allMessages = ((FakeMessageSender) messageSender).getAllMessages();
    assertTrue(allMessages.isEmpty());
  }

  @Test
  void testResetPasswordNoBaseUrl() {
    configurationProvider.getProperties().put(ConfigurationKey.SERVER_BASE_URL.getKey(), "");
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);
    clearSecurityContext();
    POST("/auth/forgotPassword", "{'emailOrUsername':'%s'}".formatted("userA"))
        .content(HttpStatus.CONFLICT);
  }

  private void doAndCheckPasswordResetWithUser(User user) {
    String token = fetchTokenInSentEmail(user);
    String newPassword = "Abxf123###...";
    POST("/auth/passwordReset", "{'newPassword':'%s', 'token':'%s'}".formatted(newPassword, token))
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
        "Created",
        201,
        "OK",
        "Account created",
        POST(
                "/auth/registration",
                renderService.toJsonAsString(getUserRegParams(new UserInviteParams())))
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
                "/auth/registration",
                renderService.toJsonAsString(getUserRegParams(new UserInviteParams())))
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
        POST("/auth/registration", renderService.toJsonAsString(getRegParamsWithUsername(null)))
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
                "/auth/registration",
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
                "/auth/registration",
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
                "/auth/registration",
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
                "/auth/registration",
                renderService.toJsonAsString(
                    getRegParamsWithFirstName(StringUtils.repeat('a', 81), RegType.SELF_REG)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration succeeds with single character first name")
  void selfRegSingleCharFirstName() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Created",
        201,
        "OK",
        "Account created",
        POST(
                "/auth/registration",
                renderService.toJsonAsString(getRegParamsWithFirstName("A", RegType.SELF_REG)))
            .content(HttpStatus.CREATED));
  }

  @Test
  @DisplayName("Self registration succeeds with single character surname")
  void selfRegSingleCharSurname() {
    disableRecaptcha();
    enableSelfRegistration();

    assertWebMessage(
        "Created",
        201,
        "OK",
        "Account created",
        POST(
                "/auth/registration",
                renderService.toJsonAsString(getRegParamsWithSurname("A", RegType.SELF_REG)))
            .content(HttpStatus.CREATED));
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
                "/auth/registration",
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
                "/auth/registration",
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
                "/auth/registration",
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
        POST("/auth/registration", renderService.toJsonAsString(getRegParamsWithEmail(null)))
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
                "/auth/registration",
                renderService.toJsonAsString(getRegParamsWithEmail("invalidEmail")))
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
                "/auth/registration",
                renderService.toJsonAsString(getUserRegParams(new UserInviteParams())))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Self registration error when invalid recaptcha input")
  void selfRegInvalidRecaptchaInput() {
    systemSettingManager.saveSystemSetting(
        SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.FALSE);

    JsonWebMessage response =
        POST(
                "/auth/registration",
                renderService.toJsonAsString(getRegParamsWithRecaptcha("secret", RegType.SELF_REG)))
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class);

    assertContains("Recaptcha validation failed", response.getMessage());
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
                "/auth/registration",
                renderService.toJsonAsString(getRegParamsWithRecaptcha(null, RegType.SELF_REG)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration completes and user can check 'me' endpoint")
  void inviteCompleteAndUsernameOk() {
    disableRecaptcha();
    // setup user as admin
    User adminCreatedUser = getAdminCreatedUser();
    POST("/users", renderService.toJsonAsString(adminCreatedUser)).content(HttpStatus.CREATED);

    switchContextToUser(adminCreatedUser);
    GET("/me").content(HttpStatus.OK);

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Account updated",
        POST("/auth/invite", renderService.toJsonAsString(getInviteRegistrationForm()))
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
                "/auth/invite",
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
                "/auth/invite",
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
                "/auth/invite",
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
                "/auth/invite",
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
                "/auth/invite",
                renderService.toJsonAsString(getRegParamsWithPassword(password, RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  @DisplayName("Invite registration error when invalid recaptcha input")
  void inviteRegInvalidRecaptchaInput() {
    systemSettingManager.saveSystemSetting(
        SettingKey.SELF_REGISTRATION_NO_RECAPTCHA, Boolean.FALSE);

    JsonWebMessage response =
        POST(
                "/auth/invite",
                renderService.toJsonAsString(getRegParamsWithRecaptcha("secret", RegType.INVITE)))
            .content(HttpStatus.BAD_REQUEST)
            .as(JsonWebMessage.class);

    assertContains("Recaptcha validation failed", response.getMessage());
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
                "/auth/invite",
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

  private RegistrationParams getUserRegParams(RegistrationParams userParams) {
    userParams.setUsername("samewisegamgee");
    userParams.setFirstName("samewise");
    userParams.setSurname("gamgee");
    userParams.setPassword("Test123!");
    userParams.setEmail("samewise@dhis2.org");
    userParams.setRecaptchaResponse("recaptcha response");
    return userParams;
  }

  private UserInviteParams getInviteRegistrationForm() {
    UserInviteParams regParams = (UserInviteParams) getUserRegParams(new UserInviteParams());
    // this Base64 encoded string needs to start with 'ID' for this invited user test
    // it's part of the recaptcha validation in RestoreOptions#getRestoreOptions
    // this unencoded string is 'idToken:IDrestoreToken'
    regParams.setToken("aWRUb2tlbjpJRHJlc3RvcmVUb2tlbg==");
    return regParams;
  }

  private RegistrationParams getRegParamsWithUsername(String username) {
    RegistrationParams selfRegistrationParams = getUserRegParams(new UserRegistrationParams());
    selfRegistrationParams.setUsername(username);
    return selfRegistrationParams;
  }

  private RegistrationParams getRegParamsWithFirstName(String firstName, RegType regType) {
    RegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setFirstName(firstName);
    return regParams;
  }

  private RegistrationParams getRegParamsWithSurname(String surname, RegType regType) {
    RegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setSurname(surname);
    return regParams;
  }

  private RegistrationParams getRegParamsWithPassword(String password, RegType regType) {
    RegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
    regParams.setPassword(password);
    return regParams;
  }

  private RegistrationParams getRegParamsWithEmail(String email) {
    RegistrationParams regParams = getUserRegParams(new UserRegistrationParams());
    regParams.setEmail(email);
    return regParams;
  }

  private RegistrationParams getRegParamsWithRecaptcha(String recaptcha, RegType regType) {
    RegistrationParams regParams = getUserRegParams(getParamsFromType(regType));
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

  private RegistrationParams getParamsFromType(RegType regParamsType) {
    return switch (regParamsType) {
      case SELF_REG -> new UserRegistrationParams();
      case INVITE -> new UserInviteParams();
    };
  }

  enum RegType {
    SELF_REG,
    INVITE
  }
}
