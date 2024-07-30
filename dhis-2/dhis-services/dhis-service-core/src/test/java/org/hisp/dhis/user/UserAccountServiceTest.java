package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
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
  @Mock private SystemSettingManager systemSettingManager;
  @Mock private PasswordValidationService passwordValidationService;

  @BeforeEach
  public void init() {
    userAccountService =
        new DefaultUserAccountService(
            userService,
            configService,
            twoFactorAuthProvider,
            systemSettingManager,
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

    when(systemSettingManager.selfRegistrationNoRecaptcha()).thenReturn(false);
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

    when(systemSettingManager.selfRegistrationNoRecaptcha()).thenReturn(false);
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
