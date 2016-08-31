package org.hisp.dhis.user;

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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.dataset.DataSet;

/**
 * @author Chau Thu Tran
 */
public interface UserService
{
    String ID = UserService.class.getName();
    String PW_NO_INTERNAL_LOGIN = "--[##no_internal_login##]--";

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    /**
     * Adds a User.
     *
     * @param user the User to add.
     * @return the generated identifier.
     */
    int addUser( User user );

    /**
     * Updates a User.
     *
     * @param user the User to update.
     */
    void updateUser( User user );

    /**
     * Retrieves the User with the given identifier.
     *
     * @param id the identifier of the User to retrieve.
     * @return the User.
     */
    User getUser( int id );

    /**
     * Retrieves the User with the given unique identifier.
     *
     * @param uid the identifier of the User to retrieve.
     * @return the User.
     */
    User getUser( String uid );

    /**
     * Returns a List of all Users.
     *
     * @return a Collection of Users.
     */
    List<User> getAllUsers();

    /**
     * Retrieves all Users with first name, surname or user name like the given
     * name.
     * 
     * @param name the name.
     * @param first the first item to return.
     * @param max the max number of item to return.
     * @return a list of Users.
     */
    List<User> getAllUsersBetweenByName( String name, int first, int max );

    /**
     * Deletes a User.
     *
     * @param user the User to delete.
     */
    void deleteUser( User user );

    List<User> getUsersByUid( List<String> uids );

    boolean isLastSuperUser( UserCredentials userCredentials );

    boolean isLastSuperRole( UserAuthorityGroup userAuthorityGroup );

    /**
     * Returns all users which are managed by the given user through its managed
     * groups association.
     * 
     * @param user the user.
     * @return a List of users.
     */
    List<User> getManagedUsers( User user );

    /**
     * Returns the number of users which are managed by the given user through its 
     * managed groups association.
     * 
     * @param user the user.
     * @return number of users.
     */
    int getManagedUserCount( User user );

    /**
     * Returns a list of users based on the given query parameters.
     * 
     * @param params the user query parameters.
     * @return a List of users.
     */
    List<User> getUsers( UserQueryParams params );

    /**
     * Returns the number of users based on the given query parameters.
     * 
     * @param params the user query parameters.
     * @return number of users.
     */
    int getUserCount( UserQueryParams params );

    /**
    * Returns number of all users
    * @return number of users
    */
    int getUserCount();
    
    List<User> getUsersByPhoneNumber( String phoneNumber );
    
    /**
     * Tests whether the current user is allowed to create a user associated
     * with the given user group identifiers. Returns true if current user has 
     * the F_USER_ADD authority. Returns true if the current user has the 
     * F_USER_ADD_WITHIN_MANAGED_GROUP authority and can manage any of the given
     * user groups. Returns false otherwise.
     * 
     * @param userGroups the user group identifiers.
     * @return true if the current user can create user, false if not.
     */
    boolean canAddOrUpdateUser( Collection<String> userGroups );
    
    // -------------------------------------------------------------------------
    // UserCredentials
    // -------------------------------------------------------------------------

    /**
     * Adds a UserCredentials.
     *
     * @param userCredentials the UserCredentials to add.
     * @return the User which the UserCredentials is associated with.
     */
    int addUserCredentials( UserCredentials userCredentials );

    /**
     * Updates a UserCredentials.
     *
     * @param userCredentials the UserCredentials to update.
     */
    void updateUserCredentials( UserCredentials userCredentials );


    /**
     * Retrieves the UserCredentials associated with the User with the given
     * name.
     *
     * @param username the name of the User.
     * @return the UserCredentials.
     */
    UserCredentials getUserCredentialsByUsername( String username );

    /**
     * Retrieves the UserCredentials associated with the User with the given
     * OpenID.
     *
     * @param openId the openId of the User.
     * @return the UserCredentials.
     */
    UserCredentials getUserCredentialsByOpenId( String openId );

    /**
     * Retrieves the UserCredentials associated with the User with the given
     * LDAP ID.
     *
     * @param ldapId the ldapId of the User.
     * @return the UserCredentials.
     */
    UserCredentials getUserCredentialsByLdapId( String ldapId );

    /**
     * Retrieves all UserCredentials.
     *
     * @return a List of UserCredentials.
     */
    List<UserCredentials> getAllUserCredentials();

    /**
     * Encodes and sets the password of the User.
     * Due to business logic required on password updates the password for a user
     * should only be changed using this method or {@link #encodeAndSetPassword(UserCredentials, String) encodeAndSetPassword}
     * and not directly on the User or UserCredentials object.
     *
     * Note that the changes made to the User object are not persisted.
     *
     * @param user the User.
     * @param rawPassword the raw password.
     */
    void encodeAndSetPassword( User user, String rawPassword );

    /**
     * Encodes and sets the password of the UserCredentials.
     * Due to business logic required on password updates the password for a user
     * should only be changed using this method or {@link #encodeAndSetPassword(User, String) encodeAndSetPassword}
     * and not directly on the User or UserCredentials object.
     *
     * Note that the changes made to the UserCredentials object are not persisted.
     *
     * @param userCredentials the UserCredentials.
     * @param rawPassword the raw password.
     */
    void encodeAndSetPassword( UserCredentials userCredentials, String rawPassword );

    /**
     * Updates the last login date of UserCredentials with the given username
     * with the current date.
     *
     * @param username the username of the UserCredentials.
     */
    void setLastLogin( String username );

    int getActiveUsersCount( int days );

    int getActiveUsersCount( Date since );

    boolean credentialsNonExpired( UserCredentials credentials );
    
    // -------------------------------------------------------------------------
    // UserAuthorityGroup
    // -------------------------------------------------------------------------

    /**
     * Adds a UserAuthorityGroup.
     *
     * @param userAuthorityGroup the UserAuthorityGroup.
     * @return the generated identifier.
     */
    int addUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup );

    /**
     * Updates a UserAuthorityGroup.
     *
     * @param userAuthorityGroup the UserAuthorityGroup.
     */
    void updateUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup );

    /**
     * Retrieves the UserAuthorityGroup with the given identifier.
     *
     * @param id the identifier of the UserAuthorityGroup to retrieve.
     * @return the UserAuthorityGroup.
     */
    UserAuthorityGroup getUserAuthorityGroup( int id );

    /**
     * Retrieves the UserAuthorityGroup with the given identifier.
     *
     * @param uid the identifier of the UserAuthorityGroup to retrieve.
     * @return the UserAuthorityGroup.
     */
    UserAuthorityGroup getUserAuthorityGroup( String uid );

    /**
     * Retrieves the UserAuthorityGroup with the given name.
     *
     * @param name the name of the UserAuthorityGroup to retrieve.
     * @return the UserAuthorityGroup.
     */
    UserAuthorityGroup getUserAuthorityGroupByName( String name );

    /**
     * Deletes a UserAuthorityGroup.
     *
     * @param userAuthorityGroup the UserAuthorityGroup to delete.
     */
    void deleteUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup );

    /**
     * Retrieves all UserAuthorityGroups.
     *
     * @return a List of UserAuthorityGroups.
     */
    List<UserAuthorityGroup> getAllUserAuthorityGroups();
    
    /**
     * Retrieves UserAuthorityGroups with the given UIDs.
     * 
     * @param uids the UIDs.
     * @return a List of UserAuthorityGroups.
     */
    List<UserAuthorityGroup> getUserRolesByUid( Collection<String> uids );

    /**
     * Retrieves all UserAuthorityGroups.
     *
     * @return a List of UserAuthorityGroups.
     */
    List<UserAuthorityGroup> getUserRolesBetween( int first, int max );

    /**
     * Retrieves all UserAuthorityGroups.
     *
     * @return a List of UserAuthorityGroups.
     */
    List<UserAuthorityGroup> getUserRolesBetweenByName( String name, int first, int max );

    /**
     * Returns the number of UserAuthorityGroups which are associated with the
     * given DataSet.
     *  
     * @param dataSet the DataSet.
     * @return number of UserAuthorityGroups.
     */
    int countDataSetUserAuthorityGroups( DataSet dataSet );

    /**
     * Returns the number of UserAuthorityGroups.
     * 
     * @return the number of UserAuthorityGroups.
     */
    int getUserRoleCount();

    /**
     * Returns the number of UserAuthorityGroups with the given name.
     * 
     * @return the number of UserAuthorityGroups with the given name.
     */
    int getUserRoleCountByName( String name );

    /**
     * Filters the given collection of user roles based on whether the current user
     * is allowed to issue it.
     * 
     * @param userRoles the collection of user roles.
     */
    void canIssueFilter( Collection<UserAuthorityGroup> userRoles );
}
