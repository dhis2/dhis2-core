/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  private UserAccountService userAccountService;
  @Mock private UserService userService;
  @Mock private ConfigurationService configService;
  @Mock private TwoFactorAuthenticationProvider twoFactorAuthProvider;
  @Mock private SystemSettingsService settingsService;
  @Mock private PasswordValidationService passwordValidationService;
  @Mock private DhisConfigurationProvider configProvider;
  @Mock private PasswordManager passwordManager;

  @BeforeEach
  public void init() {
    userAccountService =
        new DefaultUserAccountService(
            userService,
            configService,
            twoFactorAuthProvider,
            settingsService,
            passwordValidationService,
            configProvider,
            passwordManager);
  }

  @Test
  @DisplayName("Failed recaptcha response during user registration throws an exception")
  void failedRecaptchaResponseUserRegTest() throws IOException {
    // given
    RegistrationParams regParams = UserRegistrationParams.builder().build();
    regParams.setRecaptchaResponse("invalid-key");

    RecaptchaResponse recaptchaResponse = new RecaptchaResponse();
    recaptchaResponse.setSuccess(false);
    recaptchaResponse.setErrorCodes(List.of("invalid challenge received"));

    when(settingsService.getCurrentSettings())
        .thenReturn(SystemSettings.of(Map.of("keySelfRegistrationNoRecaptcha", "false")));
    when(userService.verifyRecaptcha("invalid-key", "ip1")).thenReturn(recaptchaResponse);

    // then
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> userAccountService.validateUserRegistration(regParams, "ip1"));
    assertEquals(
        "Recaptcha validation failed: [invalid challenge received]", exception.getMessage());
  }

  @Test
  @DisplayName("updateExpiredPassword with unknown username is generic and burns one verification")
  void updateExpiredPasswordUnknownUserTest() {
    when(userService.getUserByUsername("ghost")).thenReturn(null);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> userAccountService.updateExpiredPassword("ghost", "Old_pw1!", "New_pw1!"));

    assertEquals("Invalid username or password", exception.getMessage());
    // The timing-equalization verification must run, so unknown and known usernames respond in
    // similar time; no recovery attempt is registered for a nonexistent account.
    verify(passwordManager).matches(eq("Old_pw1!"), anyString());
    verify(userService, never()).registerRecoveryAttempt(anyString());
  }

  @Test
  @DisplayName("updateExpiredPassword rejects a recovery-locked account before checking passwords")
  void updateExpiredPasswordLockedTest() {
    User user = expiredPasswordUser();
    when(userService.getUserByUsername("mia")).thenReturn(user);
    when(userService.isRecoveryLocked("mia")).thenReturn(true);

    assertThrows(
        ForbiddenException.class,
        () -> userAccountService.updateExpiredPassword("mia", "Old_pw1!", "New_pw1!"));

    verify(userService, never()).registerRecoveryAttempt(anyString());
    verify(passwordManager, never()).matches(anyString(), anyString());
  }

  @Test
  @DisplayName("updateExpiredPassword with wrong old password is generic and registers an attempt")
  void updateExpiredPasswordWrongOldPasswordTest() {
    User user = expiredPasswordUser();
    when(userService.getUserByUsername("mia")).thenReturn(user);
    when(userService.isRecoveryLocked("mia")).thenReturn(false);
    when(passwordManager.matches("Wrong_pw1!", "encoded-old")).thenReturn(false);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> userAccountService.updateExpiredPassword("mia", "Wrong_pw1!", "New_pw1!"));

    assertEquals("Invalid username or password", exception.getMessage());
    verify(userService).registerRecoveryAttempt("mia");
    // Guard order: expiry state must not be consulted before the password is verified, so
    // "Account is not expired" is only observable by a caller who knows the password.
    verify(userService, never()).userNonExpired(any(User.class));
  }

  @Test
  @DisplayName("updateExpiredPassword rejects a non-expired account")
  void updateExpiredPasswordNonExpiredTest() {
    User user = expiredPasswordUser();
    when(userService.getUserByUsername("mia")).thenReturn(user);
    when(userService.isRecoveryLocked("mia")).thenReturn(false);
    when(passwordManager.matches("Old_pw1!", "encoded-old")).thenReturn(true);
    when(userService.userNonExpired(user)).thenReturn(true);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> userAccountService.updateExpiredPassword("mia", "Old_pw1!", "New_pw1!"));

    assertEquals("Account is not expired", exception.getMessage());
  }

  @Test
  @DisplayName("updateExpiredPassword rejects a new password equal to the old one")
  void updateExpiredPasswordSameAsOldTest() {
    User user = expiredPasswordUser();
    when(userService.getUserByUsername("mia")).thenReturn(user);
    when(userService.isRecoveryLocked("mia")).thenReturn(false);
    when(passwordManager.matches("Old_pw1!", "encoded-old")).thenReturn(true);
    when(userService.userNonExpired(user)).thenReturn(false);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> userAccountService.updateExpiredPassword("mia", "Old_pw1!", "Old_pw1!"));

    assertEquals("New password must be different from the old password", exception.getMessage());
  }

  @Test
  @DisplayName("updateExpiredPassword sets and persists a valid new password")
  void updateExpiredPasswordOkTest() throws BadRequestException, ForbiddenException {
    User user = expiredPasswordUser();
    when(userService.getUserByUsername("mia")).thenReturn(user);
    when(userService.isRecoveryLocked("mia")).thenReturn(false);
    when(passwordManager.matches("Old_pw1!", "encoded-old")).thenReturn(true);
    when(userService.userNonExpired(user)).thenReturn(false);
    when(passwordValidationService.validate(any(CredentialsInfo.class)))
        .thenReturn(PasswordValidationResult.VALID);

    userAccountService.updateExpiredPassword("mia", "Old_pw1!", "New_pw1!");

    verify(userService).encodeAndSetPassword(user, "New_pw1!");
    verify(userService).updateUser(eq(user), any(SystemUser.class));
  }

  private User expiredPasswordUser() {
    User user = new User();
    user.setUsername("mia");
    user.setPassword("encoded-old");
    return user;
  }

  @Test
  @DisplayName("Failed recaptcha response during user invite throws an exception")
  void failedRecaptchaResponseUserInviteTest() throws IOException {
    // given
    RegistrationParams regParams = UserRegistrationParams.builder().build();
    regParams.setRecaptchaResponse("invalid-key");

    RecaptchaResponse recaptchaResponse = new RecaptchaResponse();
    recaptchaResponse.setSuccess(false);
    recaptchaResponse.setErrorCodes(List.of("invalid challenge received"));

    when(settingsService.getCurrentSettings())
        .thenReturn(SystemSettings.of(Map.of("keySelfRegistrationNoRecaptcha", "false")));
    when(userService.verifyRecaptcha("invalid-key", "ip1")).thenReturn(recaptchaResponse);

    // then
    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> userAccountService.validateInvitedUser(regParams, "ip1"));
    assertEquals(
        "Recaptcha validation failed: [invalid challenge received]", exception.getMessage());
  }
}
