package org.hisp.dhis.mock;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;

/**
 * @author Adrian Quintana
 */
public class MockUserService
    implements
    UserService
{

    private List<User> users;

    public MockUserService( List<User> users )
    {
        this.users = users;
    }

    @Override
    public int addUser( User user )
    {
        this.users.add( user );
        return user.getId();
    }

    @Override
    public void updateUser( User user )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public User getUser( int id )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getUser( String uid )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<User> getAllUsers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<User> getAllUsersBetweenByName( String name, int first, int max )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteUser( User user )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<User> getUsersByUid( List<String> uids )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLastSuperUser( UserCredentials userCredentials )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLastSuperRole( UserAuthorityGroup userAuthorityGroup )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<User> getManagedUsers( User user )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getManagedUserCount( User user )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<User> getUsers( UserQueryParams params )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getUserCount( UserQueryParams params )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getUserCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<User> getUsersByPhoneNumber( String phoneNumber )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canAddOrUpdateUser( Collection<String> userGroups )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAddOrUpdateUser( Collection<String> userGroups, User currentUser )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int addUserCredentials( UserCredentials userCredentials )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateUserCredentials( UserCredentials userCredentials )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public UserCredentials getUserCredentialsByUsername( String username )
    {
        for (User user: users) {
            if (user.getUsername().equals( username )) {
                return user.getUserCredentials();
            }
        }
        return null;
    }

    @Override
    public UserCredentials getUserCredentialsByOpenId( String openId )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserCredentials getUserCredentialsByLdapId( String ldapId )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UserCredentials> getAllUserCredentials()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encodeAndSetPassword( User user, String rawPassword )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void encodeAndSetPassword( UserCredentials userCredentials, String rawPassword )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLastLogin( String username )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public int getActiveUsersCount( int days )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getActiveUsersCount( Date since )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean credentialsNonExpired( UserCredentials credentials )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int addUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void updateUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public UserAuthorityGroup getUserAuthorityGroup( int id )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAuthorityGroup getUserAuthorityGroup( String uid )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAuthorityGroup getUserAuthorityGroupByName( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<UserAuthorityGroup> getAllUserAuthorityGroups()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UserAuthorityGroup> getUserRolesByUid( Collection<String> uids )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UserAuthorityGroup> getUserRolesBetween( int first, int max )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UserAuthorityGroup> getUserRolesBetweenByName( String name, int first, int max )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int countDataSetUserAuthorityGroups( DataSet dataSet )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getUserRoleCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getUserRoleCountByName( String name )
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void canIssueFilter( Collection<UserAuthorityGroup> userRoles )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<ErrorReport> validateUser( User user, User currentUser )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<User> getExpiringUsers()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
