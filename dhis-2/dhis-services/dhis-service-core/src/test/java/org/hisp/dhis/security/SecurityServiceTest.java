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
package org.hisp.dhis.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class SecurityServiceTest extends DhisSpringTest
{

    private User user;

    private User otherUser;

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
        user = new User();
        user.setUsername( "johndoe" );
        user.setPassword( "" );
        user.setAutoFields();
        User userA = makeUser( "A" );
        userA.setEmail( "validA@email.com" );

        userService.addUser( user );
        otherUser = new User();
        otherUser.setUsername( "janesmith" );
        otherUser.setPassword( "" );
        User userB = makeUser( "B" );
        userB.setEmail( "validB@email.com" );
        userService.addUser( otherUser );
    }

    @Test
    void testUserAuthenticationLockout()
    {
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE );
        String username = "dr_evil";
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
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE );
    }

    @Test
    void testRecoveryAttemptLocked()
    {
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE );
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
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE );
    }

    @Test
    void testRestoreRecoverPassword()
    {
        String encodedTokens = securityService.generateAndPersistTokens( user,
            RestoreOptions.RECOVER_PASSWORD_OPTION );
        assertNotNull( encodedTokens );
        assertNotNull( user.getRestoreToken() );
        assertNotNull( user.getIdToken() );
        assertNotNull( user.getRestoreExpiry() );
        String[] idAndHashedToken = securityService.decodeEncodedTokens( encodedTokens );
        String idToken = idAndHashedToken[0];
        String restoreToken = idAndHashedToken[1];
        User credentials = userService.getUserByIdToken( idToken );
        assertNotNull( credentials );
        assertEquals( credentials, this.user );
        RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );
        assertEquals( RestoreOptions.RECOVER_PASSWORD_OPTION, restoreOptions );
        assertEquals( RestoreType.RECOVER_PASSWORD, restoreOptions.getRestoreType() );
        assertFalse( restoreOptions.isUsernameChoice() );
        //
        // verifyToken()
        //
        assertNotNull(
            securityService.verifyRestoreToken( otherUser, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertNotNull( securityService.verifyRestoreToken( credentials, "wrongToken", RestoreType.RECOVER_PASSWORD ) );
        assertNotNull( securityService.verifyRestoreToken( credentials, restoreToken, RestoreType.INVITE ) );
        assertNull( securityService.verifyRestoreToken( credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        //
        // canRestoreNow()
        //
        assertFalse( securityService.canRestore( otherUser, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertFalse( securityService.canRestore( credentials, "wrongToken", RestoreType.RECOVER_PASSWORD ) );
        assertFalse( securityService.canRestore( credentials, restoreToken, RestoreType.INVITE ) );
        assertTrue( securityService.canRestore( credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        //
        // restore()
        //
        String password = "NewPassword1";
        assertFalse( securityService.restore( otherUser, restoreToken, password, RestoreType.INVITE ) );
        assertFalse( securityService.restore( credentials, "wrongToken", password, RestoreType.INVITE ) );
        assertFalse( securityService.restore( credentials, restoreToken, password, RestoreType.INVITE ) );
        assertTrue( securityService.restore( credentials, restoreToken, password, RestoreType.RECOVER_PASSWORD ) );
        //
        // check password
        //
        assertTrue( passwordManager.matches( password, credentials.getPassword() ) );
    }

    @Test
    void testRestoreInvite()
    {
        String encodedTokens = securityService.generateAndPersistTokens( user,
            RestoreOptions.INVITE_WITH_DEFINED_USERNAME );
        assertNotNull( encodedTokens );
        assertNotNull( user.getRestoreToken() );
        assertNotNull( user.getIdToken() );
        assertNotNull( user.getRestoreExpiry() );
        String[] idAndHashedToken = securityService.decodeEncodedTokens( encodedTokens );
        String idToken = idAndHashedToken[0];
        String restoreToken = idAndHashedToken[1];
        RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );
        assertEquals( RestoreOptions.INVITE_WITH_DEFINED_USERNAME, restoreOptions );
        assertEquals( RestoreType.INVITE, restoreOptions.getRestoreType() );
        assertFalse( restoreOptions.isUsernameChoice() );
        User credentials = userService.getUserByIdToken( idToken );
        assertNotNull( credentials );
        assertEquals( credentials, this.user );
        //
        // verifyToken()
        //
        assertNotNull( securityService.verifyRestoreToken( otherUser, restoreToken, RestoreType.INVITE ) );
        assertNotNull( securityService.verifyRestoreToken( this.user, "wrongToken", RestoreType.INVITE ) );
        assertNotNull(
            securityService.verifyRestoreToken( this.user, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertNull( securityService.verifyRestoreToken( this.user, restoreToken, RestoreType.INVITE ) );
        //
        // canRestoreNow()
        //
        assertFalse( securityService.canRestore( otherUser, restoreToken, RestoreType.INVITE ) );
        assertFalse( securityService.canRestore( this.user, "wrongToken", RestoreType.INVITE ) );
        assertFalse( securityService.canRestore( this.user, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertTrue( securityService.canRestore( this.user, restoreToken, RestoreType.INVITE ) );
        //
        // restore()
        //
        String password = "NewPassword1";
        assertFalse( securityService.restore( otherUser, restoreToken, password, RestoreType.INVITE ) );
        assertFalse( securityService.restore( this.user, "wrongToken", password, RestoreType.INVITE ) );
        assertFalse(
            securityService.restore( this.user, restoreToken, password, RestoreType.RECOVER_PASSWORD ) );
        assertTrue( securityService.restore( this.user, restoreToken, password, RestoreType.INVITE ) );
        //
        // check password
        //
        assertTrue( passwordManager.matches( password, this.user.getPassword() ) );
    }
}
