/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.user;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.function.Consumer;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.Stats;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Unit tests for {@link UserController}.
 *
 * @author Volker Schmidt
 */
public class UserControllerTest
{
    @Mock
    private UserService userService;

    @Mock
    private UserGroupService userGroupService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AclService aclService;

    @InjectMocks
    private UserController userController;

    private UserGroup userGroup1;

    private UserGroup userGroup2;

    private User currentUser;

    private User user;

    private User parsedUser;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        userGroup1 = new UserGroup();
        userGroup1.setUid( "abc1" );

        userGroup2 = new UserGroup();
        userGroup2.setUid( "abc2" );

        currentUser = new User();
        currentUser.setId( 1000 );
        currentUser.setUid( "def1" );

        user = new User();
        user.setId( 1001 );
        user.setUid( "def2" );

        parsedUser = new User();
        parsedUser.setUid( "def2" );
        parsedUser.setGroups( new HashSet<>( Arrays.asList( userGroup1, userGroup2 ) ) );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void updateUserGroups()
    {
        when( userService.getUser( "def2" ) ).thenReturn( user );

        if ( isInStatusUpdatedOK( createReportWith( Status.OK, Stats::incUpdated ) ) )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        verifyNoInteractions( currentUserService );
        verify( userGroupService ).updateUserGroups( same( user ),
            (Collection<String>) argThat( containsInAnyOrder( "abc1", "abc2" ) ),
            same( currentUser ) );
    }

    @Test
    public void updateUserGroupsNotOk()
    {
        if ( isInStatusUpdatedOK( createReportWith( Status.ERROR, Stats::incUpdated ) ) )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        verifyNoInteractions( currentUserService );
        verifyNoInteractions( userService );
        verifyNoInteractions( userGroupService );
    }

    @Test
    public void updateUserGroupsNotUpdated()
    {
        if ( isInStatusUpdatedOK( createReportWith( Status.OK, Stats::incCreated ) ) )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        verifyNoInteractions( currentUserService );
        verifyNoInteractions( userService );
        verifyNoInteractions( userGroupService );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void updateUserGroupsSameUser()
    {
        currentUser.setId( 1001 );
        currentUser.setUid( "def2" );

        User currentUser2 = new User();
        currentUser2.setId( 1001 );
        currentUser2.setUid( "def2" );

        when( userService.getUser( "def2" ) ).thenReturn( user );
        when( currentUserService.getCurrentUser() ).thenReturn( currentUser2 );

        if ( isInStatusUpdatedOK( createReportWith( Status.OK, Stats::incUpdated ) ) )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        verify( currentUserService ).getCurrentUser();
        verifyNoMoreInteractions( currentUserService );
        verify( userGroupService ).updateUserGroups( same( user ),
            (Collection<String>) argThat( containsInAnyOrder( "abc1", "abc2" ) ), same( currentUser2 ) );
    }

    private ImportReport createReportWith( Status status, Consumer<Stats> operation )
    {
        TypeReport typeReport = new TypeReport( User.class );
        operation.accept( typeReport.getStats() );
        ImportReport report = new ImportReport();
        report.setStatus( status );
        report.addTypeReport( typeReport );
        return report;
    }

    private boolean isInStatusUpdatedOK( ImportReport report )
    {
        return report.getStatus() == Status.OK && report.getStats().getUpdated() == 1;
    }

    private void setUpUserExpireScenarios()
    {
        addUserCredentialsTo( user );
        addUserCredentialsTo( currentUser );
        // make current user have ALL authority
        setUpUserAuthority( currentUser, UserAuthorityGroup.AUTHORITY_ALL );
        // allow any change
        when( aclService.canUpdate( any(), any() ) ).thenReturn( true );
        when( userService.canAddOrUpdateUser( any(), any() ) ).thenReturn( true );
        // link user and current user to service methods
        when( userService.getUser( eq( user.getUid() ) ) ).thenReturn( user );
        when( currentUserService.getCurrentUser() ).thenReturn( currentUser );
    }

    @Test
    public void expireUserInTheFutureDoesNotExpireSession()
        throws Exception
    {
        setUpUserExpireScenarios();

        Date inTheFuture = new Date( System.currentTimeMillis() + 1000 );
        userController.expireUser( user.getUid(), inTheFuture );

        assertUserCredentialsUpdatedWithAccountExpiry( inTheFuture );
        verify( userService, never() ).expireActiveSessions( any() );
    }

    @Test
    public void expireUserNowDoesExpireSession()
        throws Exception
    {
        setUpUserExpireScenarios();
        when( userService.isAccountExpired( same( user.getUserCredentials() ) ) ).thenReturn( true );

        Date now = new Date();
        userController.expireUser( user.getUid(), now );

        assertUserCredentialsUpdatedWithAccountExpiry( now );
        verify( userService, atLeastOnce() ).expireActiveSessions( same( user.getUserCredentials() ) );
    }

    @Test
    public void unexpireUserDoesUpdateUserCredentials()
        throws Exception
    {
        setUpUserExpireScenarios();

        userController.unexpireUser( user.getUid() );

        assertUserCredentialsUpdatedWithAccountExpiry( null );
    }

    @Test
    public void updateUserExpireRequiresUserCredentialBasedAuthority()
    {
        setUpUserExpireScenarios();
        // executing user has no authorities
        currentUser.getUserCredentials().setUserAuthorityGroups( emptySet() );
        // changed user does have an authority
        setUpUserAuthority( user, "whatever" );

        WebMessageException ex = assertThrows( WebMessageException.class,
            () -> userController.expireUser( user.getUid(), new Date() ) );
        assertEquals(
            "You must have permissions to create user, or ability to manage at least one user group for the user.",
            ex.getWebMessage().getMessage() );
    }

    @Test
    public void updateUserExpireRequiresGroupBasedAuthority()
    {
        setUpUserExpireScenarios();
        when( userService.canAddOrUpdateUser( any(), any() ) ).thenReturn( false );

        WebMessageException ex = assertThrows( WebMessageException.class,
            () -> userController.expireUser( user.getUid(), new Date() ) );
        assertEquals(
            "You must have permissions to create user, or ability to manage at least one user group for the user.",
            ex.getWebMessage().getMessage() );
    }

    @Test
    public void updateUserExpireRequiresShareBasedAuthority()
    {
        setUpUserExpireScenarios();
        when( aclService.canUpdate( currentUser, user ) ).thenReturn( false );

        Exception ex = assertThrows( UpdateAccessDeniedException.class,
            () -> userController.expireUser( user.getUid(), new Date() ) );
        assertEquals( "You don't have the proper permissions to update this object.", ex.getMessage() );
    }

    private void setUpUserAuthority( User user, String authority )
    {
        UserAuthorityGroup suGroup = new UserAuthorityGroup();
        suGroup.setAuthorities( singleton( authority ) );
        user.getUserCredentials().setUserAuthorityGroups( singleton( suGroup ) );
    }

    private void assertUserCredentialsUpdatedWithAccountExpiry( Date accountExpiry )
    {
        ArgumentCaptor<UserCredentials> credentials = ArgumentCaptor.forClass( UserCredentials.class );
        verify( userService ).updateUserCredentials( credentials.capture() );
        UserCredentials actual = credentials.getValue();
        assertSame( "no user credentials update occurred", actual, user.getUserCredentials() );
        assertEquals( "date was not updated", accountExpiry, actual.getAccountExpiry() );
        verify( userService ).isAccountExpired( same( actual ) );
    }

    private static void addUserCredentialsTo( User user )
    {
        UserCredentials credentials = new UserCredentials();
        credentials.setUser( user );
        credentials.setUid( user.getUid() );
        user.setUserCredentials( credentials );
    }
}
