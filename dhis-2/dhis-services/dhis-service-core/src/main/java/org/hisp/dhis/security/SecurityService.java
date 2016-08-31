package org.hisp.dhis.security;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;

/**
 * @author Lars Helge Overland
 */
public interface SecurityService
{
    /**
     * Sets information for a user who will be invited by email to finish
     * setting up their user account.
     *
     * @param user the user to invite.
     * @return true if the invitation was sent, otherwise false.
     */
    boolean prepareUserForInvite( User user );

    /**
     * Indicates whether a restore/invite is allowed for the given user. The
     * requirements are:</p>
     * <p/>
     * <ul>
     * <li>email_not_configured_for_system</li>
     * <li>no_user_credentials</li>
     * <li>user_does_not_have_valid_email</li>
     * <li>user_has_critical_authorities</li>
     * </ul>
     *
     * @param credentials the user credentials.
     * @return a string if restore cannot be performed, null otherwise.
     */
    String validateRestore( UserCredentials credentials );

    /**
     * Indicates whether an invite is allowed for the given user. Delegates to
     * validateRestore( UserCredentials ). The requirements are.
     * <p/>
     * <ul>
     * <li>no_user_credentials</li>
     * <li>username_taken</li>
     * </ul>
     *
     * @param credentials the user credentials.
     * @return a string if invite cannot be performed, null otherwise.
     */
    String validateInvite( UserCredentials credentials );

    /**
     * Invokes the initRestore method and dispatches email messages with
     * restore information to the user.
     * <p/>
     * In the case of inviting a user to finish setting up an account,
     * the user account must already be configured with the profile desired
     * for the user (e.g., locale, organisation unit(s), role(s), etc.)
     *
     * @param credentials    the credentials for the user to send restore message.
     * @param rootPath       the root path of the request.
     * @param restoreOptions restore options, including type of restore.
     * @return false if any of the arguments are null or if the user credentials
     * identified by the user name does not exist, true otherwise.
     */
    boolean sendRestoreMessage( UserCredentials credentials, String rootPath, RestoreOptions restoreOptions );

    /**
     * Populates the restoreToken and restoreCode property of the given
     * credentials with a hashed version of auto-generated values. Sets the
     * restoreExpiry property with a date time some interval from now depending
     * on the restore type. Changes are persisted.
     *
     * @param credentials    the user credentials.
     * @param restoreOptions restore options, including type of restore.
     * @return an array where index 0 is the clear-text token and index 1 the
     * clear-text code.
     */
    String[] initRestore( UserCredentials credentials, RestoreOptions restoreOptions );

    /**
     * Gets the restore options by parsing them from a restore token string.
     *
     * @param token the restore token.
     * @return the restore options.
     */
    RestoreOptions getRestoreOptions( String token );

    /**
     * Tests whether the given token and code are valid for the given user name.
     * If true, it will update the user credentials identified by the given user
     * name with the new password. In order to succeed, the given token and code
     * must match the ones on the credentials, and the current date must be before
     * the expiry date time of the credentials.
     *
     * @param credentials the user credentials.
     * @param token       the token.
     * @param code        the code.
     * @param newPassword the proposed new password.
     * @param restoreType type of restore operation (e.g. pw recovery, invite).
     * @return true or false.
     */
    boolean restore( UserCredentials credentials, String token, String code, String newPassword, RestoreType restoreType );

    /**
     * Tests whether the given token and code are valid for the given user name.
     * In order to succeed, the given token and code must match the ones on the
     * credentials, and the current date must be before the expiry date time of
     * the credentials.
     *
     * @param credentials the user credentials.
     * @param token       the token.
     * @param code        the code.
     * @param restoreType type of restore operation (e.g. pw recovery, invite).
     * @return true or false.
     */
    boolean canRestore( UserCredentials credentials, String token, String code, RestoreType restoreType );

    /**
     * Tests whether the given token in combination with the given user name is
     * valid, i.e. whether the hashed version of the token matches the one on the
     * user credentials identified by the given user name.
     *
     * @param credentials the user credentials.
     * @param token       the token.
     * @return error message if any of the arguments are null or if the user
     * credentials identified by the user name does not exist, null if
     * the arguments are valid.
     */
    String verifyToken( UserCredentials credentials, String token, RestoreType restoreType );

    /**
     * Indicates whether the given username is an invite. The username is
     * considered an invite if it is null or matches the invite username pattern
     * of invite-<email>-<uid>.
     *
     * @param username the username.
     * @return true if the username represents an account invitation.
     */
    boolean isInviteUsername( String username );

    /**
     * Checks whether current user has read access to object.
     *
     * @param identifiableObject Object to check for read access.
     * @return true of false depending on outcome of read check
     */
    boolean canRead( IdentifiableObject identifiableObject );

    /**
     * Checks whether current user has create access to object.
     *
     * @param identifiableObject Object to check for write access.
     * @return true of false depending on outcome of write check
     */
    boolean canWrite( IdentifiableObject identifiableObject );

    /**
     * Checks whether current user can create public instances of the object.
     *
     * @param identifiableObject Object to check for write access.
     * @return true of false depending on outcome of write check
     */
    boolean canCreatePublic( IdentifiableObject identifiableObject );

    /**
     * Checks whether current user can create public instances of the object.
     *
     * @param type Type to check for write access.
     * @return true of false depending on outcome of write check
     */
    boolean canCreatePublic( String type );

    /**
     * Checks whether current user can create private instances of the object.
     *
     * @param identifiableObject Object to check for write access.
     * @return true of false depending on outcome of write check
     */
    boolean canCreatePrivate( IdentifiableObject identifiableObject );

    /**
     * Checks whether current user can create private instances of the object.
     *
     * @param type Type to check for write access.
     * @return true of false depending on outcome of write check
     */
    boolean canCreatePrivate( String type );

    /**
     * Checks whether current user can view instances of the object. Depends on
     * system setting for require add to view objects.
     *
     * @param type Type to check for view access.
     * @return true of false depending on outcome of check
     */
    boolean canView( String type );

    /**
     * Checks whether current user has update access to object.
     *
     * @param identifiableObject Object to check for update access.
     * @return true of false depending on outcome of update check
     */
    boolean canUpdate( IdentifiableObject identifiableObject );

    /**
     * Checks whether current user has delete access to object.
     *
     * @param identifiableObject Object to check for delete access.
     * @return true of false depending on outcome of delete check
     */
    boolean canDelete( IdentifiableObject identifiableObject );

    /**
     * Checks whether current user has manage access to object.
     *
     * @param identifiableObject Object to check for manage access.
     * @return true of false depending on outcome of manage check
     */
    boolean canManage( IdentifiableObject identifiableObject );

    boolean hasAnyAuthority( String... authorities );
}
