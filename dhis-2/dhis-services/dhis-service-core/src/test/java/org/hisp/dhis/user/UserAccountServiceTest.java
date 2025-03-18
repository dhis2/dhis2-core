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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.feedback.BadRequestException;
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

  @BeforeEach
  public void init() {
    userAccountService =
        new DefaultUserAccountService(
            userService,
            configService,
            twoFactorAuthProvider,
            settingsService,
            passwordValidationService);
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
