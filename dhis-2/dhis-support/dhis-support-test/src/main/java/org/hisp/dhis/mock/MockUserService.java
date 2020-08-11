package org.hisp.dhis.mock;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Adrian Quintana
 */
public class MockUserService
    implements UserService
{

    private List<User> users;

    public MockUserService( List<User> users )
    {
        this.users = users;
    }

    @Override
    public long addUser( User user )
    {
        this.users.add( user );
        return user.getId();
    }

    @Override
    public void updateUser( User user )
    {
    }

    @Override
    public User getUser( long id )
    {
        return null;
    }

    @Override
    public User getUser( String uid )
    {
        return null;
    }

    @Override
    public User getUserByUuid( UUID uuid )
    {
        return null;
    }

    @Override
    public List<User> getUsers( Collection<String> uid )
    {
        return this.users;
    }

    @Override
    public List<User> getAllUsers()
    {
        return null;
    }

    @Override
    public List<User> getAllUsersBetweenByName( String name, int first, int max )
    {
        return null;
    }

    @Override
    public void deleteUser( User user )
    {
    }

    @Override
    public boolean isLastSuperUser( UserCredentials userCredentials )
    {
        return false;
    }

    @Override
    public boolean isLastSuperRole( UserAuthorityGroup userAuthorityGroup )
    {
        return false;
    }

    @Override
    public List<User> getUsers( UserQueryParams params )
    {
        return null;
    }

    @Override
    public List<User> getUsers( UserQueryParams params, @Nullable List<String> orders )
    {
        return null;
    }

    @Override
    public int getUserCount( UserQueryParams params )
    {
        return 0;
    }

    @Override
    public int getUserCount()
    {
        return 0;
    }

    @Override
    public List<User> getUsersByPhoneNumber( String phoneNumber )
    {
        return null;
    }

    @Override
    public boolean canAddOrUpdateUser( Collection<String> userGroups )
    {
        return false;
    }

    @Override
    public boolean canAddOrUpdateUser( Collection<String> userGroups, User currentUser )
    {
        return false;
    }

    @Override
    public long addUserCredentials( UserCredentials userCredentials )
    {
        return 0;
    }

    @Override
    public void updateUserCredentials( UserCredentials userCredentials )
    {
    }

    @Override
    public UserCredentials getUserCredentialsByUsername( String username )
    {
        for ( User user : users )
        {
            if ( user.getUsername().equals( username ) )
            {
                return user.getUserCredentials();
            }
        }
        return null;
    }

    @Override
    public UserCredentials getUserCredentialsWithEagerFetchAuthorities( String username )
    {
        for ( User user : users )
        {
            if ( user.getUsername().equals( username ) )
            {
                UserCredentials userCredentials = user.getUserCredentials();
                userCredentials.getAllAuthorities();
                return userCredentials;
            }
        }
        return null;
    }

    @Override
    public UserCredentials getUserCredentialsByOpenId( String openId )
    {
        return null;
    }

    @Override
    public UserCredentials getUserCredentialsByLdapId( String ldapId )
    {
        return null;
    }

    @Override
    public List<UserCredentials> getAllUserCredentials()
    {
        return null;
    }

    @Override
    public void encodeAndSetPassword( User user, String rawPassword )
    {
    }

    @Override
    public void encodeAndSetPassword( UserCredentials userCredentials, String rawPassword )
    {
    }

    @Override
    public void setLastLogin( String username )
    {
    }

    @Override
    public int getActiveUsersCount( int days )
    {
        return 0;
    }

    @Override
    public int getActiveUsersCount( Date since )
    {
        return 0;
    }

    @Override
    public boolean credentialsNonExpired( UserCredentials credentials )
    {
        return false;
    }

    @Override
    public long addUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        return 0;
    }

    @Override
    public void updateUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
    }

    @Override
    public UserAuthorityGroup getUserAuthorityGroup( long id )
    {
        return null;
    }

    @Override
    public UserAuthorityGroup getUserAuthorityGroup( String uid )
    {
        return null;
    }

    @Override
    public UserAuthorityGroup getUserAuthorityGroupByName( String name )
    {
        return null;
    }

    @Override
    public void deleteUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
    }

    @Override
    public List<UserAuthorityGroup> getAllUserAuthorityGroups()
    {
        return null;
    }

    @Override
    public List<UserAuthorityGroup> getUserRolesByUid( Collection<String> uids )
    {
        return null;
    }

    @Override
    public List<UserAuthorityGroup> getUserRolesBetween( int first, int max )
    {
        return null;
    }

    @Override
    public List<UserAuthorityGroup> getUserRolesBetweenByName( String name, int first, int max )
    {
        return null;
    }

    @Override
    public int countDataSetUserAuthorityGroups( DataSet dataSet )
    {
        return 0;
    }

    @Override
    public void canIssueFilter( Collection<UserAuthorityGroup> userRoles )
    {
    }

    @Override
    public List<ErrorReport> validateUser( User user, User currentUser )
    {
        return null;
    }

    @Override
    public List<User> getExpiringUsers()
    {
        return null;
    }

    @Override
    public void set2FA( User user, Boolean twoFA )
    {
    }

    @Override
    public void expireActiveSessions( UserCredentials credentials )
    {
    }

    @Override
    public User getUserByUsername( String username )
    {
        return null;
    }

    @Override
    public User getUserByIdentifier( String id )
    {
        return null;
    }
}
