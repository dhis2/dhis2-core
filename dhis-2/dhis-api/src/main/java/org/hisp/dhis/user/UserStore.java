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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.hisp.dhis.common.IdentifiableObjectStore;

/**
 * @author Nguyen Hong Duc
 */
public interface UserStore
    extends IdentifiableObjectStore<User>
{
    String ID = UserStore.class.getName();

    /**
     * Returns a list of users based on the given query parameters.
     *
     *
     * @param params the user query parameters.
     * @return a List of users.
     */
    List<User> getUsers( UserQueryParams params );

    /**
     * Returns a list of users based on the given query parameters. If the
     * specified list of orders are empty, default order of last name and first
     * name will be applied.
     *
     * @param params the user query parameters.
     * @param orders the already validated order strings (e.g. email:asc).
     * @return a List of users.
     */
    List<User> getUsers( UserQueryParams params, @Nullable List<String> orders );

    /**
     * Returns the number of users based on the given query parameters.
     *
     * @param params the user query parameters.
     * @return number of users.
     */
    int getUserCount( UserQueryParams params );

    /**
     * Returns number of all users
     *
     * @return number of users
     */
    int getUserCount();

    List<User> getExpiringUsers( UserQueryParams userQueryParams );

    /**
     * @param inDays number of days to include
     * @return list of those users that are about to expire in the provided
     *         number of days (or less) and which have an email configured
     */
    List<UserAccountExpiryInfo> getExpiringUserAccounts( int inDays );

    /**
     * Returns User for given username.
     *
     * @param username username for which the User will be returned
     * @return User for given username or null
     */
    User getUserByUsername( String username );

    /**
     * Returns User with given userId.
     *
     * @param userId UserId
     * @return User with given userId
     */
    User getUser( long userId );

    /**
     * Return CurrentUserGroupInfo used for ACL check in
     * {@link IdentifiableObjectStore}
     *
     * @param user
     * @return
     */
    CurrentUserGroupInfo getCurrentUserGroupInfo( User user );

    /**
     * Sets {@link User#setDisabled(boolean)} to {@code true} for all users
     * where the {@link User#getLastLogin()} is before or equal to the provided
     * pivot {@link Date}.
     *
     * @param inactiveSince the most recent point in time that is considered
     *        inactive together with accounts only active further in the past.
     * @return number of users disabled
     */
    int disableUsersInactiveSince( Date inactiveSince );

    /**
     * Selects all not disabled users where the {@link User#getLastLogin()} is
     * within the given time-frame and which have an email address.
     *
     * @param from start of the selected time-frame (inclusive)
     * @param to end of the selected time-frame (exclusive)
     * @return user emails having a last login within the given time-frame as
     *         keys and if available their preferred locale as value
     */
    Map<String, Optional<Locale>> findNotifiableUsersWithLastLoginBetween( Date from, Date to );

    String getDisplayName( String userUid );

    /**
     * Retrieves a collection of User with the given usernames.
     *
     * @param usernames the usernames of the collection of Users to retrieve.
     * @return the User.
     */
    List<User> getUserByUsernames( Collection<String> usernames );

    /**
     * Retrieves the User associated with the User with the given open ID.
     *
     * @param openId open ID.
     * @return the User or null if there is no match.
     */
    User getUserByOpenId( String openId );

    /**
     * Retrieves the User associated with the User with the given LDAP ID.
     *
     * @param ldapId LDAP ID.
     * @return the User.
     */
    User getUserByLdapId( String ldapId );

    /**
     * Retrieves the User associated with the User with the given id token.
     *
     * @param token the restore token of the User.
     * @return the User.
     */
    User getUserByIdToken( String token );

    /**
     * Retrieves the User with the given UUID.
     *
     * @param uuid UUID.
     * @return the User.
     */
    User getUserByUuid( UUID uuid );

    List<User> getHasAuthority( String authority );
}
