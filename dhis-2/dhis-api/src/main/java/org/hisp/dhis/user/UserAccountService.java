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

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserInviteParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.HiddenNotFoundException;

/**
 * Service that handles user account activities, e.g. create/update account. The Validation can be
 * moved somewhere more appropriate when this is revisited.
 *
 * @author david mackessy
 */
public interface UserAccountService {

  /**
   * Create a self registered user using some self reg defaults (org unit and user role)
   *
   * @param params used to populate the new User
   * @param request used in the authentication process
   */
  void registerUser(UserRegistrationParams params, HttpServletRequest request);

  /**
   * Create an invited user using the restore flow
   *
   * @param params used to populate the updated User
   * @param request used in the authentication process
   */
  void confirmUserInvite(UserInviteParams params, HttpServletRequest request)
      throws BadRequestException;

  /**
   * Validates a self registered user. Uses specific validation for the self registration flow.
   *
   * @param params to validate
   * @param remoteIpAddress address for recaptcha checking
   * @throws BadRequestException when validation error
   * @throws IOException possible when validating recaptcha
   */
  void validateUserRegistration(RegistrationParams params, String remoteIpAddress)
      throws BadRequestException, IOException;

  /**
   * Validates an invited registered user. Uses specific validation for the invite registration
   * flow.
   *
   * @param params to validate
   * @param remoteIpAddress address for recaptcha checking
   * @throws BadRequestException when validation error
   * @throws IOException possible when validating recaptcha
   */
  void validateInvitedUser(RegistrationParams params, String remoteIpAddress)
      throws BadRequestException, IOException;

  /**
   * Initiates the account-recovery flow by sending a restore email to the user, if account recovery
   * is enabled. Attempts are throttled per account via the account-recovery lockout.
   *
   * @param emailOrUsername email address or username identifying the account
   * @throws HiddenNotFoundException when the user does not exist or cannot be restored
   * @throws ConflictException when recovery is disabled, unconfigured, or sending fails
   * @throws ForbiddenException when the account is temporarily locked due to too many attempts
   */
  void forgotPassword(String emailOrUsername)
      throws HiddenNotFoundException, ConflictException, ForbiddenException;

  /**
   * Completes the account-recovery flow by setting a new password for the account identified by the
   * restore token.
   *
   * @param token encoded id and restore token pair from the recovery email
   * @param newPassword new password, subject to the password policy
   * @throws ConflictException when recovery is disabled or the token does not resolve to a user
   * @throws BadRequestException when input is missing or the new password is not acceptable
   */
  void resetPassword(String token, String newPassword)
      throws ConflictException, BadRequestException;

  /**
   * Self-service change of an <b>expired</b> password. The caller is not logged in, so the method
   * is self-guarding: it only proceeds for an expired account whose current password is supplied
   * correctly. It deliberately does not establish a session; once the password is changed the
   * account is no longer expired, and the caller logs in through the regular login endpoint.
   *
   * <p>The current-password check runs <b>before</b> the expiry check, so "Account is not expired"
   * can only be observed by a caller who already knows the password (no username enumeration), and
   * repeated attempts against one account are throttled via the shared account-recovery lockout
   * (mirrors {@link #forgotPassword(String)}).
   *
   * @param username username identifying the account
   * @param oldPassword the current (expired) password
   * @param newPassword new password, subject to the password policy
   * @throws BadRequestException when input is missing, credentials are wrong, the account is not
   *     expired, or the new password is not acceptable
   * @throws ForbiddenException when the account is temporarily locked due to too many attempts
   */
  void updateExpiredPassword(String username, String oldPassword, String newPassword)
      throws BadRequestException, ForbiddenException;
}
