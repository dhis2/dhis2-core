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
package org.hisp.dhis.security;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface SecurityService {
  /**
   * Register a account recovery attempt for the given user account.
   *
   * @param username the username of the user account.
   */
  void registerRecoveryAttempt(String username);

  /**
   * Indicates whether the recovery of the user account is locked due to too many recovery attempts
   * within a specific time span. The max number of attempts is 5 and the time span is 15 minutes.
   *
   * @param username the username of the user account.
   */
  boolean isRecoveryLocked(String username);

  /**
   * Register a failed login attempt for the given user account.
   *
   * @param username the username of the user account.
   */
  void registerFailedLogin(String username);

  /**
   * Register a successful login attempt for the given user account.
   *
   * @param username the username of the user account.
   */
  void registerSuccessfulLogin(String username);

  /**
   * Indicates whether the given user account is locked out due to too many successive failed login
   * attempts within a specific time span. The max number of attempts is 5 and the time span is 15
   * minutes.
   *
   * @param username the username of the user account.
   */
  boolean isLocked(String username);

  /**
   * Sets information for a user who will be invited by email to finish setting up their user
   * account.
   *
   * @param user the user to invite.
   */
  void prepareUserForInvite(User user);

  /**
   * Indicates whether a restore/invite is allowed for the given user. Returns an error code if the
   * restore cannot be performed, null otherwise.
   *
   * @param user the user.
   * @return an {@link ErrorCode} if restore cannot be performed, null otherwise.
   */
  ErrorCode validateRestore(User user);

  /**
   * Indicates whether an invite is allowed for the given user. Delegates to validateRestore( User
   * ).
   *
   * @param user the user.
   * @return a string if invite cannot be performed, null otherwise.
   */
  ErrorCode validateInvite(User user);

  /**
   * Invokes the initRestore method and dispatches email messages with restore information to the
   * user, or sends an invite email.
   *
   * @param user the user to send restore message.
   * @param rootPath the root path of the request.
   * @param restoreOptions restore options, including type of restore.
   * @return false if any of the arguments are null or if the user identified by the user name does
   *     not exist, true otherwise.
   */
  boolean sendRestoreOrInviteMessage(User user, String rootPath, RestoreOptions restoreOptions);

  /**
   * Populates the restoreToken property and idToken of the given user with a hashed version of
   * auto-generated values. Sets the restoreExpiry property with a date time some interval from now
   * depending on the restore type. Changes are persisted.
   *
   * @param user the user.
   * @param restoreOptions restore options, including type of restore.
   * @return an encoded string containing both id token and hashed/restoreToken, delimited with :
   *     clear-text code.
   */
  String generateAndPersistTokens(User user, RestoreOptions restoreOptions);

  /**
   * Decodes the id and hashed/restore token used for restore or invite.
   *
   * @param encodedTokens a Base64 encoded string containing the id token and hashed/restoreToken,
   *     delimited with :
   * @return an array containing the decoded tokens
   */
  String[] decodeEncodedTokens(String encodedTokens);

  /**
   * Gets the restore options by parsing them from a restore token string.
   *
   * @param token the restore token.
   * @return the restore options.
   */
  RestoreOptions getRestoreOptions(String token);

  /**
   * Tests whether the given token is valid for the given username. If true, it will update the user
   * identified by the given username with the new password. In order to succeed, the given token
   * must match the ones on the user, and the current date must be before the expiry date time of
   * the user.
   *
   * @param user the user.
   * @param token the token.
   * @param newPassword the proposed new password.
   * @param restoreType type of restore operation (e.g. pw recovery, invite).
   * @return true or false.
   */
  boolean restore(User user, String token, String newPassword, RestoreType restoreType);

  /**
   * Tests whether the given token and code are valid for the given username. In order to succeed,
   * the given token and code must match the ones on the user, and the current date must be before
   * the expiry date time of the user.
   *
   * @param user the user.
   * @param token the token.
   * @param restoreType type of restore operation (e.g. pw recovery, invite).
   * @return true or false.
   */
  boolean canRestore(User user, String token, RestoreType restoreType);

  /**
   * Tests whether the given token in combination with the given user name is valid, i.e. whether
   * the hashed version of the token matches the one on the user identified by the given username.
   *
   * @param user the user.
   * @param token the token.
   * @return error code if any of the arguments are null or if the user identified by the username
   *     does not exist, null if the arguments are valid.
   */
  ErrorCode validateRestoreToken(User user, String token, RestoreType restoreType);

  /**
   * Checks whether current user has read access to object.
   *
   * @param identifiableObject Object to check for read access.
   * @return true of false depending on outcome of read check
   */
  boolean canRead(IdentifiableObject identifiableObject);

  /**
   * Checks whether current user has create access to object.
   *
   * @param identifiableObject Object to check for write access.
   * @return true of false depending on outcome of write check
   */
  boolean canWrite(IdentifiableObject identifiableObject);

  /**
   * Checks whether current user can create public instances of the object.
   *
   * @param identifiableObject Object to check for write access.
   * @return true of false depending on outcome of write check
   */
  boolean canCreatePublic(IdentifiableObject identifiableObject);

  /**
   * Checks whether current user can create public instances of the object.
   *
   * @param type Type to check for write access.
   * @return true of false depending on outcome of write check
   */
  boolean canCreatePublic(String type);

  /**
   * Checks whether current user can create private instances of the object.
   *
   * @param identifiableObject Object to check for write access.
   * @return true of false depending on outcome of write check
   */
  boolean canCreatePrivate(IdentifiableObject identifiableObject);

  /**
   * Checks whether current user can create private instances of the object.
   *
   * @param type Type to check for write access.
   * @return true of false depending on outcome of write check
   */
  boolean canCreatePrivate(String type);

  /**
   * Checks whether current user can view instances of the object. Depends on system setting for
   * require add to view objects.
   *
   * @param type Type to check for view access.
   * @return true of false depending on outcome of check
   */
  boolean canView(String type);

  /**
   * Checks whether current user has update access to object.
   *
   * @param identifiableObject Object to check for update access.
   * @return true of false depending on outcome of update check
   */
  boolean canUpdate(IdentifiableObject identifiableObject);

  /**
   * Checks whether current user has delete access to object.
   *
   * @param identifiableObject Object to check for delete access.
   * @return true of false depending on outcome of delete check
   */
  boolean canDelete(IdentifiableObject identifiableObject);

  /**
   * Checks whether current user has manage access to object.
   *
   * @param identifiableObject Object to check for manage access.
   * @return true of false depending on outcome of manage check
   */
  boolean canManage(IdentifiableObject identifiableObject);

  /**
   * Indicates whether the current user has been granted any of the given authorities.
   *
   * @param authorities the authorities.
   * @return true if the current user has any of the given authorities.
   */
  boolean hasAnyAuthority(String... authorities);

  /**
   * Verify reCaptcha V2 key against Google API.
   *
   * @param key the key to check
   * @return the response from Google reCaptcha API.
   */
  RecaptchaResponse verifyRecaptcha(String key, String remoteIp) throws IOException;

  /**
   * Check if current user has DATA_WRITE access for given object.
   *
   * @param identifiableObject Object to check for data write access.
   * @return true of false depending on outcome of DATA_WRITE check
   */
  boolean canDataWrite(IdentifiableObject identifiableObject);

  /**
   * Check if current user has DATA_READ for given object.
   *
   * @param identifiableObject Object to check for data read access.
   * @return true of false depending on outcome of DATA_READ check
   */
  boolean canDataRead(IdentifiableObject identifiableObject);

  /**
   * If the user is not a superuser, and the user is not updating their own 2FA settings, then the
   * user must have the proper permissions to update the user
   *
   * @param before The user object before the update.
   * @param after The user object that is being updated.
   */
  void validate2FAUpdate(boolean before, boolean after, User user);
}
