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

import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.hamcrest.Matchers;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
        Mockito.when( userService.getUser( "def2" ) ).thenReturn( user );

        final TypeReport typeReport = new TypeReport( User.class );
        typeReport.getStats().incUpdated();
        final ImportReport importReport = new ImportReport();
        importReport.setStatus( Status.OK );
        importReport.addTypeReport( typeReport );
        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        Mockito.verifyNoInteractions( currentUserService );
        Mockito.verify( userGroupService ).updateUserGroups( Mockito.same( user ),
            (Collection<String>) argThat( Matchers.containsInAnyOrder( "abc1", "abc2" ) ),
            Mockito.same( currentUser ) );
    }

    @Test
    public void updateUserGroupsNotOk()
    {
        final TypeReport typeReport = new TypeReport( User.class );
        typeReport.getStats().incUpdated();
        final ImportReport importReport = new ImportReport();
        importReport.setStatus( Status.ERROR );
        importReport.addTypeReport( typeReport );
        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        Mockito.verifyNoInteractions( currentUserService );
        Mockito.verifyNoInteractions( userService );
        Mockito.verifyNoInteractions( userGroupService );
    }

    @Test
    public void updateUserGroupsNotUpdated()
    {
        final TypeReport typeReport = new TypeReport( User.class );
        typeReport.getStats().incCreated();
        final ImportReport importReport = new ImportReport();
        importReport.setStatus( Status.OK );
        importReport.addTypeReport( typeReport );
        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        Mockito.verifyNoInteractions( currentUserService );
        Mockito.verifyNoInteractions( userService );
        Mockito.verifyNoInteractions( userGroupService );
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

        Mockito.when( userService.getUser( "def2" ) ).thenReturn( user );
        Mockito.when( currentUserService.getCurrentUser() ).thenReturn( currentUser2 );

        final TypeReport typeReport = new TypeReport( User.class );
        typeReport.getStats().incUpdated();
        final ImportReport importReport = new ImportReport();
        importReport.setStatus( Status.OK );
        importReport.addTypeReport( typeReport );
        if ( importReport.getStatus() == Status.OK && importReport.getStats().getUpdated() == 1 )
        {
            userController.updateUserGroups( "def2", parsedUser, currentUser );
        }

        Mockito.verify( currentUserService ).getCurrentUser();
        Mockito.verifyNoMoreInteractions( currentUserService );
        Mockito.verify( userGroupService ).updateUserGroups( Mockito.same( user ),
            (Collection<String>) argThat( Matchers.containsInAnyOrder( "abc1", "abc2" ) ),
            Mockito.same( currentUser2 ) );
    }
}