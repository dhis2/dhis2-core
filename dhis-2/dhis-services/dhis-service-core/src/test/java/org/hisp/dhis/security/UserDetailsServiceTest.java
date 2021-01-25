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
