/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.security.twofa;

import static org.hisp.dhis.common.CodeGenerator.generateSecureRandomBytes;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.user.UserService.DEFAULT_APPLICATION_TITLE;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs
 */
@Slf4j
@Service
@AllArgsConstructor
public class TwoFactorAuthService {

  public static final String TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME = "R_ENABLE_2FA";
  public static final long TWOFA_EMAIL_CODE_EXPIRY_MILLIS = 3600000;

  private final UserService userService;
  private final SystemSettingsProvider settingsProvider;
  private final MessageSender emailMessageSender;
  private final UserSettingsService userSettingsService;
  private final I18nManager i18nManager;
  private final AclService aclService;

  /**
   * Verify the 2FA code for the user.
   *
   * @param user The user that is being verified.
   * @param code The 2FA code that is being verified.
   * @return A boolean value.
   */
  public boolean isInvalid2FACode(User user, String code) {
    return !TwoFactorAuthUtils.isValid2FACode(code, user.getTwoFactorType(), user.getSecret());
  }

  /**
   * Enroll user in time-based one-time password (TOTP) based two-factor authentication.
   *
   * @param user The user object that is being enrolled.
   */
  @Transactional
  public void enrollTOTP2FA(User user) {
    if (user.isTwoFactorEnabled()) {
      throw new IllegalStateException(
          "User has 2FA enabled already, disable first to enroll again");
    }
    user.setTwoFactorType(TwoFactorType.ENROLLING_TOTP);
    String totpSeed = Base32.encode(generateSecureRandomBytes(20));
    user.setSecret(totpSeed);
    userService.updateUser(user);
  }

  /**
   * Enroll user in email based two-factor authentication.
   *
   * @param user The user object that is being enrolled.
   */
  @Transactional
  public void enrollEmail2FA(User user) {
    if (user.isTwoFactorEnabled()) {
      throw new IllegalStateException(
          "User has 2FA enabled already, disable first to enroll again");
    }
    if (!userService.isEmailVerified(user)) {
      throw new IllegalStateException("User's email is not verified, verify email first");
    }
    user.setTwoFactorType(TwoFactorType.ENROLLING_EMAIL);
    Email2FACode email2FACode = generateEmail2FACode();
    user.setSecret(email2FACode.encodedCode());

    send2FACodeWithEmailSender(user, email2FACode.code());

    userService.updateUser(user);
  }

  /**
   * Enable 2FA authentication for the user, if the user is in the 2FA enrolling state.
   *
   * @param user The user to enable 2FA authentication.
   * @param code The TOTP code that the user generated with the authenticator app, or the email code
   *     that was sent to the user.
   */
  @Transactional
  public void enable2FA(User user, String code) {
    if (user.getTwoFactorType().isEnabled()) {
      throw new IllegalStateException(
          "User has 2FA enabled already, disable first to enroll and enable again");
    }
    if (!user.getTwoFactorType().isEnrolling()) {
      throw new IllegalStateException("User has not enrolled in 2FA, call enrollment first");
    }

    if (isInvalid2FACode(user, code)) {
      throw new IllegalStateException("Invalid 2FA code");
    }

    setEnabled2FA(user, CurrentUserUtil.getCurrentUserDetails());
  }

  public void setEnabled2FA(User user, UserDetails actingUser) {
    user.setTwoFactorType(user.getTwoFactorType().getEnabledType());
    userService.updateUser(user, actingUser);
  }

  public void setEnabled2FA(UserDetails userDetails, UserDetails actingUser) {
    User user = userService.getUserByUsername(userDetails.getUsername());
    if (!user.getTwoFactorType().isEnrolling()) {
      throw new IllegalStateException("Two factor type is not enrolling");
    }
    user.setTwoFactorType(user.getTwoFactorType().getEnabledType());
    userService.updateUser(user, actingUser);
  }

  /**
   * If the user has 2FA authentication enabled, and the code is valid, then disable 2FA
   * authentication.
   *
   * @param user The user that you want to disable 2FA authentication
   * @param code The 2FA code
   */
  @Transactional
  public void disable2FA(User user, String code) {
    if (!user.isTwoFactorEnabled()) {
      throw new IllegalStateException("Two factor is not enabled, enable first");
    }
    if (userService.is2FADisableEndpointLocked(user.getUsername())) {
      throw new IllegalStateException("Too many failed disable attempts, try again later");
    }

    if (isInvalid2FACode(user, code)) {
      throw new IllegalStateException("Invalid 2FA code");
    }

    reset2FA(user, CurrentUserUtil.getCurrentUserDetails());

    userService.registerSuccess2FADisable(user.getUsername());
  }

  /**
   * Disable 2FA authentication for the user.
   *
   * @param user The user that you want to reset.
   */
  @Transactional
  public void reset2FA(User user, UserDetails actingUser) {
    user.setSecret(null);
    user.setTwoFactorType(null);
    userService.updateUser(user, actingUser);
  }

  @Transactional
  public void reset2FA(UserDetails userDetails, UserDetails actingUser) {
    User user = userService.getUserByUsername(userDetails.getUsername());
    user.setSecret(null);
    user.setTwoFactorType(null);
    userService.updateUser(user, actingUser);
  }

  /**
   * "If the current user is not the user being modified, and the current user has the authority to
   * modify the user, then disable two-factor authentication for the user."
   *
   * @param currentUser The user who is making the request.
   * @param userUid The user UID of the user to disable 2FA for.
   * @param errors A Consumer<ErrorReport> object that will be called if there is an error.
   */
  @Transactional
  public void privileged2FADisable(User currentUser, String userUid, Consumer<ErrorReport> errors)
      throws ForbiddenException {
    User user = userService.getUser(userUid);
    if (user == null) {
      throw new IllegalArgumentException("User not found");
    }

    if (currentUser.getUid().equals(user.getUid())
        || !userService.canCurrentUserCanModify(currentUser, user, errors)) {
      throw new ForbiddenException(ErrorCode.E3021.getMessage());
    }

    reset2FA(user, UserDetails.fromUser(currentUser));
  }

  /**
   * Email the user with a new 2FA code.
   *
   * @param userDetails The user that is being sent the 2FA code.
   */
  @Transactional
  public void sendEmail2FACode(UserDetails userDetails) {
    User user = userService.getUserByUsername(userDetails.getUsername());
    if (user == null) {
      throw new IllegalArgumentException("User not found");
    }
    if (!user.isTwoFactorEnabled()) {
      throw new IllegalStateException(
          "User has not enabled 2FA , enable before trying to send code");
    }
    if (!user.getTwoFactorType().equals(TwoFactorType.EMAIL)) {
      throw new IllegalStateException("User has not email 2FA enabled");
    }
    if (!userService.isEmailVerified(user)) {
      throw new IllegalStateException("User's email is not verified");
    }

    Email2FACode email2FACode = generateEmail2FACode();
    user.setSecret(email2FACode.encodedCode());

    send2FACodeWithEmailSender(user, email2FACode.code());

    userService.updateUser(user);
  }

  private record Email2FACode(String code, String encodedCode) {}

  @Nonnull
  private static Email2FACode generateEmail2FACode() {
    String code = new String(CodeGenerator.generateSecureRandomNumber(6));
    String encodedCode = code + "|" + (System.currentTimeMillis() + TWOFA_EMAIL_CODE_EXPIRY_MILLIS);
    return new Email2FACode(code, encodedCode);
  }

  private void send2FACodeWithEmailSender(User user, String code) {
    String applicationTitle = settingsProvider.getCurrentSettings().getApplicationTitle();
    if (applicationTitle == null || applicationTitle.isEmpty()) {
      applicationTitle = DEFAULT_APPLICATION_TITLE;
    }

    Map<String, Object> vars = new HashMap<>();
    vars.put("applicationTitle", applicationTitle);
    vars.put("code", code);
    vars.put("username", user.getUsername());
    vars.put("email", user.getEmail());
    vars.put("fullName", user.getName());

    I18n i18n =
        i18nManager.getI18n(
            userSettingsService.getUserSettings(user.getUsername(), true).getUserUiLocale());
    vars.put("i18n", i18n);

    VelocityManager vm = new VelocityManager();
    String messageBody = vm.render(vars, "twofa_email_body_template_v1");
    String messageSubject = i18n.getString("email_2fa_subject") + " " + applicationTitle;

    OutboundMessageResponse status =
        emailMessageSender.sendMessage(messageSubject, messageBody, null, null, Set.of(user), true);

    boolean success = status.getResponseObject() == EmailResponse.SENT;

    if (!success) {
      throw new IllegalStateException("Sending 2FA code with email failed");
    }
  }

  /**
   * If the user has a role with the 2FA authentication required restriction, return true.
   *
   * @param userDetails The user object that is being checked for the role.
   * @return A boolean value.
   */
  public boolean hasTwoFactorRoleRestriction(UserDetails userDetails) {
    return userDetails.hasAnyRestrictions(Set.of(TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME));
  }

  /**
   * If the user is not the same as the user to modify, and the user has the proper acl permissions
   * to modify the user, then the user can modify the user.
   *
   * @param before The state before the update.
   * @param after The state after the update.
   * @param userToModify The user object that is being updated.
   */
  @Transactional
  public void validateTwoFactorUpdate(boolean before, boolean after, User userToModify)
      throws ForbiddenException {

    if (before == after) {
      return;
    }

    if (!before) {
      throw new ForbiddenException("You can not enable 2FA with this API endpoint, only disable.");
    }

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();

    // Current user can not update their own 2FA settings, must use
    // /2fa/enable or disable API, even if they are admin.
    if (currentUserDetails.getUid().equals(userToModify.getUid())) {
      throw new ForbiddenException(ErrorCode.E3030.getMessage());
    }

    // If current user has access to manage this user, they can disable 2FA.
    if (!aclService.canUpdate(currentUserDetails, userToModify)) {
      throw new ForbiddenException(
          String.format(
              "User `%s` is not allowed to update object `%s`.",
              currentUserDetails.getUsername(), userToModify));
    }

    User currentUser = userService.getUserByUsername(currentUserDetails.getUsername());
    if (!userService.canAddOrUpdateUser(getUids(userToModify.getGroups()), currentUser)
        || !currentUserDetails.canModifyUser(userToModify)) {
      throw new ForbiddenException("You don't have the proper permissions to update this user.");
    }
  }
}
