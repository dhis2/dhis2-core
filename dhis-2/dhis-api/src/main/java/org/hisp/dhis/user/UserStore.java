/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Nguyen Hong Duc
 */
public interface UserStore extends IdentifiableObjectStore<User> {
  /**
   * Returns a list of users based on the given query parameters.
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

  List<UID> getUserIds(UserQueryParams params, @Nullable List<String> orders);

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

  List<User> getExpiringUsers(UserQueryParams userQueryParams);

  /**
   * @param inDays number of days to include
   * @return list of those users that are about to expire in the provided number of days (or less)
   *     and which have an email configured
   */
  List<UserAccountExpiryInfo> getExpiringUserAccounts(int inDays);

  @CheckForNull
  User getUserByUsername(String username);

  /**
   * Returns User for given username. Returns null if no user is found.
   *
   * @param username username for which the User will be returned
   * @param ignoreCase match name ignoreing case
   * @return User for given username or null
   */
  @CheckForNull
  User getUserByUsername(String username, boolean ignoreCase);

  /**
   * Returns User with given userId.
   *
   * @param userId UserId
   * @return User with given userId
   */
  User getUser(long userId);

  /**
   * Sets {@link User#setDisabled(boolean)} to {@code true} for all users where the {@link
   * User#getLastLogin()} is before or equal to the provided pivot {@link Date}.
   *
   * @param inactiveSince the most recent point in time that is considered inactive together with
   *     accounts only active further in the past.
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
  Map<String, String> getActiveUserGroupUserEmailsByUsername(String userGroupId);

  String getDisplayName(String userUid);

  /**
   * Retrieves a collection of User with the given usernames.
   *
   * @param usernames the usernames of the collection of Users to retrieve.
   * @return the User.
   */
  List<User> getUserByUsernames(Collection<String> usernames);

  /**
   * Retrieves the most recently-used enabled User associated with the given open ID.
   *
   * <p>This only returns enabled users.
   *
   * <p>A newly-created User (last login is null) will only be returned if there is no enabled User
   * with the given open ID and a non-null last login.
   *
   * @param openId open ID.
   * @return the User or null if there is no enabled user match.
   */
  @CheckForNull
  User getUserByOpenId(@Nonnull String openId);

  /**
   * Retrieves the User associated with the User with the given LDAP ID.
   *
   * @param ldapId LDAP ID.
   * @return the User.
   */
  User getUserByLdapId(String ldapId);

  /**
   * Retrieves the User associated with the User with the given id token.
   *
   * @param token the restore token of the User.
   * @return the User.
   */
  User getUserByIdToken(String token);

  /**
   * Retrieves the User with the given UUID.
   *
   * @param uuid UUID.
   * @return the User.
   */
  User getUserByUuid(UUID uuid);

  /**
   * Retrieves the User associated with the User with the given email.
   *
   * @param email email.
   * @return the User.
   */
  User getUserByEmail(String email);

  List<User> getHasAuthority(String authority);

  List<User> getLinkedUserAccounts(User currentUser);

  /** Return CurrentUserGroupInfo used for ACL check in {@link IdentifiableObjectStore} */
  CurrentUserGroupInfo getCurrentUserGroupInfo(String userUid);

  /**
   * Sets the active account for the next login session.
   *
   * <p>This method updates the last login timestamp of the target account 'activeUsername', to one
   * hour in the future. This future timestamp ensures the account appears first when sorting linked
   * accounts by last login date, and hence the top of the list will be the 'active'.
   *
   * @param actingUser the acting/current user
   * @param activeUsername the username of the user to set as active
   */
  void setActiveLinkedAccounts(@Nonnull String actingUser, @Nonnull String activeUsername);

  User getUserByEmailVerificationToken(String token);

  User getUserByVerifiedEmail(String email);

  /**
   * Retrieves all {@link User}s that have an entry for the {@link OrganisationUnit}s in the given
   * table
   *
   * @param orgUnitProperty {@link UserOrgUnitProperty} used to search
   * @param uids {@link OrganisationUnit}s {@link UID}s to match on
   * @return matching {@link User}s
   */
  List<User> getUsersWithOrgUnit(
      @Nonnull UserOrgUnitProperty orgUnitProperty, @Nonnull Set<UID> uids);
}
