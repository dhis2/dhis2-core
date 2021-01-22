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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class SecurityServiceTest
    extends DhisSpringTest
{
    private UserCredentials credentials;

    private UserCredentials otherCredentials;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordManager passwordManager;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    public void setUpTest()
    {
        credentials = new UserCredentials();
        credentials.setUsername( "johndoe" );
        credentials.setPassword( "" );
        credentials.setAutoFields();

        User userA = createUser( 'A' );
        userA.setEmail( "validA@email.com" );
        userA.setUserCredentials( credentials );

        credentials.setUserInfo( userA );
        userService.addUserCredentials( credentials );

        otherCredentials = new UserCredentials();
        otherCredentials.setUsername( "janesmith" );
        otherCredentials.setPassword( "" );

        User userB = createUser( 'B' );
        userB.setEmail( "validB@email.com" );
        userB.setUserCredentials( otherCredentials );
        otherCredentials.setUserInfo( userB );
        userService.addUserCredentials( otherCredentials );
    }

    @Test
    public void testUserAuthenticationLockout()
    {
        systemSettingManager.saveSystemSetting(
            SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE );

        String username = "dr_evil";

        securityService.registerFailedLogin( username );
        assertFalse( securityService.isLocked( username ) );

        securityService.registerFailedLogin( username );
        assertFalse( securityService.isLocked( username ) );

        securityService.registerFailedLogin( username );
        assertFalse( securityService.isLocked( username ) );

        securityService.registerFailedLogin( username );
        assertFalse( securityService.isLocked( username ) );

        securityService.registerFailedLogin( username );
        assertFalse( securityService.isLocked( username ) );

        securityService.registerFailedLogin( username );
        assertTrue( securityService.isLocked( username ) );

        securityService.registerFailedLogin( username );
        assertTrue( securityService.isLocked( username ) );

        securityService.registerSuccessfulLogin( username );
        assertFalse( securityService.isLocked( username ) );

        systemSettingManager.saveSystemSetting(
            SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE );
    }

    @Test
    public void testRecoveryAttemptLocked()
    {
        systemSettingManager.saveSystemSetting(
            SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE );

        String username = "dr_evil";

        securityService.registerRecoveryAttempt( username );
        assertFalse( securityService.isRecoveryLocked( username ) );

        securityService.registerRecoveryAttempt( username );
        assertFalse( securityService.isRecoveryLocked( username ) );

        securityService.registerRecoveryAttempt( username );
        assertFalse( securityService.isRecoveryLocked( username ) );

        securityService.registerRecoveryAttempt( username );
        assertFalse( securityService.isRecoveryLocked( username ) );

        securityService.registerRecoveryAttempt( username );
        assertFalse( securityService.isRecoveryLocked( username ) );

        securityService.registerRecoveryAttempt( username );
        assertTrue( securityService.isRecoveryLocked( username ) );

        systemSettingManager.saveSystemSetting(
            SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE );
    }

    @Test
    public void testRestoreRecoverPassword()
    {
        String encodedTokens = securityService
            .generateAndPersistTokens( credentials, RestoreOptions.RECOVER_PASSWORD_OPTION );

        assertNotNull( encodedTokens );
        assertNotNull( credentials.getRestoreToken() );
        assertNotNull( credentials.getIdToken() );
        assertNotNull( credentials.getRestoreExpiry() );

        String[] idAndHashedToken = securityService.decodeEncodedTokens( encodedTokens );
        String idToken = idAndHashedToken[0];
        String restoreToken = idAndHashedToken[1];

        UserCredentials credentials = userService.getUserCredentialsByIdToken( idToken );
        assertNotNull( credentials );
        assertEquals( credentials, this.credentials );

        RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );
        assertEquals( RestoreOptions.RECOVER_PASSWORD_OPTION, restoreOptions );
        assertEquals( RestoreType.RECOVER_PASSWORD, restoreOptions.getRestoreType() );
        assertFalse( restoreOptions.isUsernameChoice() );

        //
        // verifyToken()
        //
        assertNotNull(
            securityService.verifyRestoreToken( otherCredentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        assertNotNull( securityService.verifyRestoreToken( credentials, "wrongToken", RestoreType.RECOVER_PASSWORD ) );

        assertNotNull( securityService.verifyRestoreToken( credentials, restoreToken, RestoreType.INVITE ) );

        assertNull( securityService.verifyRestoreToken( credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        //
        // canRestoreNow()
        //
        assertFalse( securityService.canRestore( otherCredentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        assertFalse( securityService.canRestore( credentials, "wrongToken", RestoreType.RECOVER_PASSWORD ) );

        assertFalse( securityService.canRestore( credentials, restoreToken, RestoreType.INVITE ) );

        assertTrue( securityService.canRestore( credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        //
        // restore()
        //
        String password = "NewPassword1";

        assertFalse( securityService.restore( otherCredentials, restoreToken, password, RestoreType.INVITE ) );

        assertFalse( securityService.restore( credentials, "wrongToken", password, RestoreType.INVITE ) );

        assertFalse( securityService.restore( credentials, restoreToken, password, RestoreType.INVITE ) );

        assertTrue( securityService.restore( credentials, restoreToken, password, RestoreType.RECOVER_PASSWORD ) );

        //
        // check password
        //
        assertTrue( passwordManager.matches( password, credentials.getPassword() ) );
    }

    @Test
    public void testRestoreInvite()
    {
        String encodedTokens = securityService
            .generateAndPersistTokens( credentials, RestoreOptions.INVITE_WITH_DEFINED_USERNAME );

        assertNotNull( encodedTokens );
        assertNotNull( credentials.getRestoreToken() );
        assertNotNull( credentials.getIdToken() );
        assertNotNull( credentials.getRestoreExpiry() );

        String[] idAndHashedToken = securityService.decodeEncodedTokens( encodedTokens );
        String idToken = idAndHashedToken[0];
        String restoreToken = idAndHashedToken[1];

        RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );
        assertEquals( RestoreOptions.INVITE_WITH_DEFINED_USERNAME, restoreOptions );
        assertEquals( RestoreType.INVITE, restoreOptions.getRestoreType() );
        assertFalse( restoreOptions.isUsernameChoice() );

        UserCredentials credentials = userService.getUserCredentialsByIdToken( idToken );
        assertNotNull( credentials );
        assertEquals( credentials, this.credentials );

        //
        // verifyToken()
        //
        assertNotNull( securityService.verifyRestoreToken( otherCredentials, restoreToken, RestoreType.INVITE ) );

        assertNotNull( securityService.verifyRestoreToken( this.credentials, "wrongToken", RestoreType.INVITE ) );

        assertNotNull(
            securityService.verifyRestoreToken( this.credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        assertNull( securityService.verifyRestoreToken( this.credentials, restoreToken, RestoreType.INVITE ) );

        //
        // canRestoreNow()
        //
        assertFalse( securityService.canRestore( otherCredentials, restoreToken, RestoreType.INVITE ) );

        assertFalse( securityService.canRestore( this.credentials, "wrongToken", RestoreType.INVITE ) );

        assertFalse( securityService.canRestore( this.credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        assertTrue( securityService.canRestore( this.credentials, restoreToken, RestoreType.INVITE ) );

        //
        // restore()
        //
        String password = "NewPassword1";

        assertFalse( securityService.restore( otherCredentials, restoreToken, password, RestoreType.INVITE ) );

        assertFalse( securityService.restore( this.credentials, "wrongToken", password, RestoreType.INVITE ) );

        assertFalse(
            securityService.restore( this.credentials, restoreToken, password, RestoreType.RECOVER_PASSWORD ) );

        assertTrue( securityService.restore( this.credentials, restoreToken, password, RestoreType.INVITE ) );

        //
        // check password
        //
        assertTrue( passwordManager.matches( password, this.credentials.getPassword() ) );
    }

    @Test
    public void testRestoreInviteWithUsernameChoice()
    {
        String encodedTokens = securityService
            .generateAndPersistTokens( credentials, RestoreOptions.INVITE_WITH_USERNAME_CHOICE );
        String[] idAndHashedToken = securityService.decodeEncodedTokens( encodedTokens );
        String restoreToken = idAndHashedToken[1];

        RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );

        assertEquals( RestoreOptions.INVITE_WITH_USERNAME_CHOICE, restoreOptions );
        assertEquals( RestoreType.INVITE, restoreOptions.getRestoreType() );
        assertTrue( restoreOptions.isUsernameChoice() );
    }

    @Test
    public void testIsInviteUsername()
    {
        assertTrue( securityService.isInviteUsername( "invite-johndoe@gmail.com-OsTci1JyHRU" ) );
        assertTrue( securityService.isInviteUsername( "invite-fr37@abc.gov-OsTci1JyHRU" ) );
        assertTrue( securityService.isInviteUsername( null ) );
        assertFalse( securityService.isInviteUsername( "inv1te-mark@gmail.com-OsTci1JyHRU" ) );
        assertFalse( securityService.isInviteUsername( "invite-tomjohnson@yahoo.com-OsTci1JyHRUC" ) );
        assertFalse( securityService.isInviteUsername( "invite-johnthomson@gmail.com-OsTci1yHRU" ) );
    }
}
