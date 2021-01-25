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
package org.hisp.dhis.security;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Tests the effects of {@link UserCredentials#setDisabled(boolean)} or
 * {@link UserCredentials#setAccountExpiry(Date)} on the {@link UserDetails}
 * ability to log in.
 *
 * @author Jan Bernitt
 */
public class UserDetailsServiceTest extends DhisSpringTest
{

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsService userDetailsService;

    private User user;

    private UserCredentials credentials;

    @Override
    protected void setUpTest()
        throws Exception
    {
        user = createUser( 'A' );
        credentials = createUserCredentials( 'A', user );
        userService.addUser( user );
        userService.addUserCredentials( credentials );
    }

    @Test
    public void baseline()
    {
        // a vanilla user should be able to log in
        assertCanLogin( getUserDetails() );
    }

    @Test
    public void disabledUserCanNotLogIn()
    {
        credentials.setDisabled( true );
        userService.updateUserCredentials( credentials );
        assertCanNotLogin( getUserDetails() );
    }

    @Test
    public void enabledUserCanLogIn()
    {
        credentials.setDisabled( true );
        userService.updateUserCredentials( credentials );
        assertCanNotLogin( getUserDetails() );

        credentials.setDisabled( false );
        userService.updateUserCredentials( credentials );
        assertCanLogin( getUserDetails() );
    }

    @Test
    public void expiredUserAccountCanNotLogIn()
    {
        // expired 1000s in past
        credentials.setAccountExpiry( new Date( currentTimeMillis() - 1000 ) );
        userService.updateUserCredentials( credentials );
        assertCanNotLogin( getUserDetails() );
    }

    @Test
    public void notYetExpiredUserAccountCanStillLogIn()
    {
        credentials.setAccountExpiry( new Date( currentTimeMillis() + 10000 ) );
        userService.updateUserCredentials( credentials );
        assertCanLogin( getUserDetails() );
    }

    private UserDetails getUserDetails()
    {
        return userDetailsService.loadUserByUsername( user.getUsername() );
    }

    private static void assertCanLogin( UserDetails details )
    {
        assertTrue( details.isEnabled() );
        assertTrue( details.isAccountNonExpired() );
        assertTrue( details.isAccountNonLocked() );
        assertTrue( details.isCredentialsNonExpired() );
    }

    private static void assertCanNotLogin( UserDetails details )
    {
        assertFalse( details.isEnabled()
            && details.isAccountNonExpired()
            && details.isAccountNonLocked()
            && details.isCredentialsNonExpired() );
    }
}
