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
package org.hisp.dhis.user;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Chau Thu Tran
 */
public interface UserService {
  Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2([ayb])?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");

  String PW_NO_INTERNAL_LOGIN = "--[##no_internal_login##]--";

  String TWO_FACTOR_CODE_APPROVAL_PREFIX = "APPROVAL_";

  String TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME = "R_ENABLE_2FA";

  String RESTORE_PATH = "/dhis-web-login/index.html#/";

  String TBD_NAME = "(TBD)";

  String DEFAULT_APPLICATION_TITLE = "DHIS 2";

  int LOGIN_MAX_FAILED_ATTEMPTS = 4;

  int RECOVERY_LOCKOUT_MINS = 15;

  int RECOVER_MAX_ATTEMPTS = 5;

  String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

  /**
   * If the user's secret starts with the prefix `APPROVAL_`, then return true
   *
   * @param user The user to check.
   * @return A boolean value.
   */
  static boolean hasTwoFactorSecretForApproval(User user) {
    return user.getSecret().startsWith(TWO_FACTOR_CODE_APPROVAL_PREFIX);
  }

  /**
   * Adds a User.
   *
   * @param user the User to add.
   * @return the generated identifier.
   */
  long addUser(User user);

  long addUser(User user, UserDetails actingUser);

  /**
   * Updates a User.
   *
   * @param user the User to update.
   */
  void updateUser(User user);

  void updateUser(User user, UserDetails actingUser);

  /**
   * Retrieves the User with the given identifier.
   *
   * @param id the identifier of the User to retrieve.
   * @return the User.
   */
  User getUser(long id);

  /**
   * Retrieves the User with the given unique identifier.
   *
   * @param uid the identifier of the User to retrieve.
   * @return the User.
   */
  User getUser(String uid);

  /**
   * Retrieves the User with the given UUID.
   *
   * @param uuid the UUID of the User to retrieve.
   * @return the User.
   */
  User getUserByUuid(UUID uuid);

  /**
   * Retrieves the User with the given username. Returns null if no user is found.
   *
   * @param username the username of the User to retrieve.
   * @return the User.
   */
  User getUserByUsername(String username);

  /**
   * Retrieves the User with the given username. Ignores case when checking the username.
   *
   * @param username the username of the User to retrieve.
   * @return the User.
   */
  User getUserByUsernameIgnoreCase(String username);

  /**
   * Retrieves the User by attempting to look up by various identifiers in the following order:
   *
   * <ul>
   *   <li>UID
   *   <li>UUID
   *   <li>Username
   * </ul>
   *
   * @param id the User identifier.
   * @return the User, or null if not found.
   */
  User getUserByIdentifier(String id);

  /**
   * Retrieves the User with the given email.
   *
   * @param email the email of the User to retrieve.
   * @return the User.
   */
  User getUserByEmail(String email);

  User getUserByVerifiedEmail(String email);

  /**
   * Retrieves a collection of User with the given unique identifiers.
   *
   * @param uids the identifiers of the collection of Users to retrieve.
   * @return the User.
   */
  List<User> getUsers(@Nonnull Collection<String> uids);

  /**
   * Retrieves a collection of User with the given usernames.
   *
   * @param usernames the usernames of the collection of Users to retrieve.
   * @return the User.
   */
  List<User> getUsersByUsernames(Collection<String> usernames);

  /**
   * Returns a List of all Users.
   *
   * @return a Collection of Users.
   */
  List<User> getAllUsers();

  /**
   * Retrieves all Users with first name, surname or user name like the given name.
   *
   * @param name the name.
   * @param first the first item to return.
   * @param max the max number of item to return.
   * @return a list of Users.
   */
  List<User> getAllUsersBetweenByName(String name, int first, int max);

  /**
   * Deletes a User.
   *
   * @param user the User to delete.
   */
  void deleteUser(User user);

  /**
   * Checks if the given user represents the last user with ALL authority.
   *
   * @param user the user.
   * @return true if the given user represents the last user with ALL authority.
   */
  boolean isLastSuperUser(User user);

  /**
   * Returns a list of users based on the given query parameters. The default order of last name and
   * first name will be applied.
   *
   * @param params the user query parameters.
   * @return a List of users.
   */
  List<User> getUsers(UserQueryParams params);

  /**
   * Returns a list of users based on the given query parameters. If the specified list of orders
   * are empty, default order of last name and first name will be applied.
   *
   * @param params the user query parameters.
   * @param orders the already validated order strings (e.g. email:asc).
   * @return a List of users.
   */
  List<User> getUsers(UserQueryParams params, @Nullable List<String> orders);

  /**
   * Returns the number of users based on the given query parameters.
   *
   * @param params the user query parameters.
   * @return number of users.
   */
  int getUserCount(UserQueryParams params);

  /**
   * Returns number of all users
   *
   * @return number of users
   */
  int getUserCount();

  @Nonnull
  List<User> getUsersByPhoneNumber(String phoneNumber);

  /**
   * Tests whether the current user is allowed to create a user associated with the given user group
   * identifiers. Returns true if current user has the F_USER_ADD authority. Returns true if the
   * current user has the F_USER_ADD_WITHIN_MANAGED_GROUP authority and can manage any of the given
   * user groups. Returns false otherwise.
   *
   * @param userGroups the user group identifiers.
   * @return true if the current user can create user, false if not.
   */
  boolean canAddOrUpdateUser(Collection<String> userGroups);

  boolean canAddOrUpdateUser(Collection<String> userGroups, User currentUser);

  /**
   * Retrieves the User associated with the User with the given id token.
   *
   * @param token the id token of the User.
   * @return the User.
   */
  User getUserByIdToken(String token);

  /**
   * Retrieves the User associated with the User with the given OpenID.
   *
   * @param openId the openId of the User.
   * @return the User or null if there is no match
   */
  @CheckForNull
  User getUserByOpenId(@Nonnull String openId);

  /**
   * Retrieves the User associated with the User with the given LDAP ID.
   *
   * @param ldapId the ldapId of the User.
   * @return the User.
   */
  User getUserByLdapId(String ldapId);

  /**
   * Encodes and sets the password of the User. Due to business logic required on password updates
   * the password for a user should only be changed using this method or {@link
   * #encodeAndSetPassword(User, String) encodeAndSetPassword} and not directly on the User or User
   * object.
   *
   * <p>Note that the changes made to the User object are not persisted.
   *
   * @param user the User
   * @param rawPassword the raw password.
   */
  void encodeAndSetPassword(User user, String rawPassword);

  /**
   * Updates the last login date of User with the given username with the current date.
   *
   * @param username the username of the User.
   */
  void setLastLogin(String username);

  int getActiveUsersCount(int days);

  int getActiveUsersCount(Date since);

  /**
   * If the user's password has not expired, return true
   *
   * @param user The user object that is being checked.
   */
  boolean userNonExpired(User user);

  /**
   * Adds a UserRole.
   *
   * @param userRole the UserRole.
   * @return the generated identifier.
   */
  long addUserRole(UserRole userRole);

  long addUserRole(UserRole userRole, UserDetails actingUser);

  /**
   * Updates a UserRole.
   *
   * @param userRole the UserRole.
   */
  void updateUserRole(UserRole userRole);

  /**
   * Retrieves the UserRole with the given identifier.
   *
   * @param id the identifier of the UserRole to retrieve.
   * @return the UserRole.
   */
  UserRole getUserRole(long id);

  /**
   * Retrieves the UserRole with the given identifier.
   *
   * @param uid the identifier of the UserRole to retrieve.
   * @return the UserRole.
   */
  UserRole getUserRole(String uid);

  /**
   * Retrieves the UserRole with the given name.
   *
   * @param name the name of the UserRole to retrieve.
   * @return the UserRole.
   */
  UserRole getUserRoleByName(String name);

  /**
   * Deletes a UserRole.
   *
   * @param userRole the UserRole to delete.
   */
  void deleteUserRole(UserRole userRole);

  /**
   * Retrieves all UserRole.
   *
   * @return a List of UserRole.
   */
  List<UserRole> getAllUserRoles();

  /**
   * Retrieves UserRole with the given UIDs.
   *
   * @param uids the UIDs.
   * @return a List of UserRolea.
   */
  List<UserRole> getUserRolesByUid(@Nonnull Collection<String> uids);

  /**
   * Retrieves all UserRole.
   *
   * @return a List of UserRole.
   */
  List<UserRole> getUserRolesBetween(int first, int max);

  /**
   * Retrieves all UserRole.
   *
   * @return a List of UserRoles.
   */
  List<UserRole> getUserRolesBetweenByName(String name, int first, int max);

  /**
   * Returns the number of UserRoles which are associated with the given DataSet.
   *
   * @param dataSet the DataSet.
   * @return number of UserRoles.
   */
  int countDataSetUserRoles(DataSet dataSet);

  /**
   * Filters the given collection of user roles based on whether the current user is allowed to
   * issue it.
   *
   * @param userRoles the collection of user roles.
   */
  void canIssueFilter(Collection<UserRole> userRoles);

  /**
   * Validate that the current user are allowed to create or modify properties of the given user.
   *
   * @param user
   * @param currentUser
   * @return
   */
  List<ErrorReport> validateUserCreateOrUpdateAccess(User user, User currentUser);

  List<ErrorReport> validateUserRoleCreateOrUpdate(UserRole user, User currentUser);

  /**
   * @param inDays number of days to include
   * @return list of those users that are about to expire in the provided number of days (or less)
   *     and which have an email configured
   */
  List<UserAccountExpiryInfo> getExpiringUserAccounts(int inDays);

  /**
   * Sets {@link User#setDisabled(boolean)} to {@code true} for all users where the {@link
   * User#getLastLogin()} is before or equal to the provided pivot {@link Date}.
   *
   * @param inactiveSince the most recent point in time that is considered inactive together with
   *     accounts only active further in the past.#
   * @return number of users disabled
   */
  int disableUsersInactiveSince(Date inactiveSince);

  /**
   * Selects all not disabled users where the {@link User#getLastLogin()} is within the given
   * time-frame and which have an email address.
   *
   * @param from start of the selected time-frame (inclusive)
   * @param to end of the selected time-frame (exclusive)
   * @return user emails having a last login within the given time-frame as keys and if available
   *     their preferred locale as value
   */
  Map<String, Optional<Locale>> findNotifiableUsersWithLastLoginBetween(Date from, Date to);

  /**
   * Selects all not disabled users where the {@link User#getPasswordLastUpdated()} ()} is within
   * the given time-frame and which have an email address.
   *
   * @param from start of the selected time-frame (inclusive)
   * @param to end of the selected time-frame (exclusive)
   * @return user emails having a password last updated within the given time-frame as keys and if
   *     available their preferred locale as value
   */
  Map<String, Optional<Locale>> findNotifiableUsersWithPasswordLastUpdatedBetween(
      Date from, Date to);

  /**
   * Find users with email that are members of a user-group and return them by username.
   *
   * @param userGroupId a user group ID
   * @return a map of user emails by username for all users in the group that have an email
   *     configured
   */
  Map<String, String> getUserGroupUserEmailsByUsername(String userGroupId);

  /** Get user display name by concat( firstname,' ', surname ) Return null if User doesn't exist */
  String getDisplayName(String userUid);

  /** Given an Authority's name, retrieves a list of users that has that authority. */
  List<User> getUsersWithAuthority(String authority);

  /**
   * Use this method instead of {@link #createUserDetails(User)} if no {@link User} instance is
   * available or if the one available is not fully loaded or connected to a session.
   *
   * @param userUid UID of the {@link UserDetails} to create
   * @return the implementation object
   * @see #createUserDetails(User)
   */
  UserDetails createUserDetails(String userUid) throws NotFoundException;

  /**
   * It creates a CurrentUserDetailsImpl object from a User object. It also fetches the users locked
   * and credentials expired status.
   *
   * @param user The user object that is being authenticated.
   * @return A CurrentUserDetailsImpl object.
   */
  UserDetails createUserDetails(User user);

  /**
   * "If the current user is not the user being modified, and the current user has the authority to
   * modify the user, then disable two-factor authentication for the user."
   *
   * <p>The first thing we do is get the user object from the database. If the user doesn't exist,
   * we throw an exception
   *
   * @param currentUser The user who is making the request.
   * @param userUid The user UID of the user to disable 2FA for.
   * @param errors A Consumer<ErrorReport> object that will be called if there is an error.
   */
  void privilegedTwoFactorDisable(User currentUser, String userUid, Consumer<ErrorReport> errors)
      throws ForbiddenException;

  /**
   * Checks if the input user can modify the other input user.
   *
   * @param currentUser The user who is trying to modify the user
   * @param userToModify The user that is being modified
   * @param errors A Consumer<ErrorReport> object that will be called if the user cannot be
   *     modified.
   * @return Boolean
   */
  boolean canCurrentUserCanModify(
      User currentUser, User userToModify, Consumer<ErrorReport> errors);

  /**
   * Generate a new two factor (TOTP) secret for the user, but prefix it with a special string so
   * that we can tell the difference between a normal secret and an approval secret.
   *
   * @param user The user object that is being updated.
   */
  void generateTwoFactorOtpSecretForApproval(User user);

  /**
   * If the user has an OTP secret that starts with the approval prefix, remove the prefix and
   * update the user property.
   *
   * @param user The user object that is being updated.
   */
  void approveTwoFactorSecret(User user, UserDetails actingUser);

  /**
   * "Disable 2FA authentication for the input user, by setting the secret to null."
   *
   * @param user The user object that you want to reset the 2FA for.
   */
  void resetTwoFactor(User user, UserDetails actingUser);

  /**
   * If the user has a secret, and the secret has not been approved, and the code is valid, then
   * approve the secret and effectively enable 2FA.
   *
   * @param user The user object to enable 2FA authentication for.
   * @param code The code that the user entered into the app
   */
  void enableTwoFa(User user, String code);

  /**
   * If the user has 2FA authentication enabled, and the code is valid, then disable 2FA
   * authentication
   *
   * @param user The user object that you want to disable 2FA authentication for.
   * @param code The code that the user entered
   */
  void disableTwoFa(User user, String code);

  /**
   * Register a failed 2FA disable attempt for the given user account.
   *
   * @param username
   */
  void registerFailed2FADisableAttempt(String username);

  /**
   * If the user has a failed 2FA disable attempt more than 4 times in the last 15 minutes, return
   * true.
   *
   * @param username
   * @return
   */
  boolean twoFaDisableIsLocked(String username);

  /**
   * Register a successful 2FA disable attempt for the given user account, this will reset the
   * attempt cache.
   *
   * @param username
   */
  void registerSuccess2FADisable(String username);

  /**
   * If the user has a role with the 2FA authentication required restriction, return true.
   *
   * @param userDetails The user object that is being checked for the role.
   * @return A boolean value.
   */
  boolean hasTwoFactorRoleRestriction(UserDetails userDetails);

  /**
   * If the user is not the same as the user to modify, and the user has the proper acl permissions
   * to modify the user, then the user can modify the user.
   *
   * @param before The state before the update.
   * @param after The state after the update.
   * @param userToModify The user object that is being updated.
   */
  void validateTwoFactorUpdate(boolean before, boolean after, User userToModify)
      throws ForbiddenException;

  /**
   * Get linked user accounts for the given user
   *
   * @param actingUser the acting/current user
   * @return list of linked user accounts
   */
  @Nonnull
  List<UserLookup> getLinkedUserAccounts(@Nonnull User actingUser);

  void invalidateUserSessions(String uid);

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

  CurrentUserGroupInfo getCurrentUserGroupInfo(String userUID);

  /**
   * Generate a new email verification token for the user and set it on the user object.
   *
   * @param user the user
   * @return the generated token
   */
  String generateAndSetNewEmailVerificationToken(User user);

  /**
   * Send email verification token to the user's email address.
   *
   * @param user the user
   * @param token the verification token
   * @param requestUrl the request URL
   * @return true if the email was sent successfully, false otherwise
   */
  boolean sendEmailVerificationToken(User user, String token, String requestUrl);

  boolean verifyEmail(String token);

  boolean isEmailVerified(User currentUser);

  User getUserByVerificationToken(String token);

  /**
   * Method that retrieves all {@link User}s that have an entry for the {@link OrganisationUnit} in
   * the given table
   *
   * @param orgUnitProperty {@link UserOrgUnitProperty} used to search
   * @param uid {@link OrganisationUnit} {@link UID} to match on
   * @return matching {@link User}s
   */
  List<User> getUsersWithOrgUnit(@Nonnull UserOrgUnitProperty orgUnitProperty, @Nonnull UID uid);
}
