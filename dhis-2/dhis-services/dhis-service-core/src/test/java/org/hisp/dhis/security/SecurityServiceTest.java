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

class SecurityServiceTest extends DhisSpringTest
{
    private User userA;

    private User userB;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordManager passwordManager;

    @Autowired
    private SecurityService service;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    public void setUpTest()
    {
        userA = new User();
        userA.setUsername( "johndoe" );
        userA.setPassword( "" );
        userA.setAutoFields();
        userService.addUser( userA );

        userB = new User();
        userB.setUsername( "janesmith" );
        userB.setPassword( "" );
        userService.addUser( userB );
    }

    @Test
    void testUserAuthenticationLockout()
    {
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE );
        String username = "dr_evil";
        service.registerFailedLogin( username );
        assertFalse( service.isLocked( username ) );
        service.registerFailedLogin( username );
        assertFalse( service.isLocked( username ) );
        service.registerFailedLogin( username );
        assertFalse( service.isLocked( username ) );
        service.registerFailedLogin( username );
        assertTrue( service.isLocked( username ) );
        service.registerFailedLogin( username );
        assertTrue( service.isLocked( username ) );
        service.registerSuccessfulLogin( username );
        assertFalse( service.isLocked( username ) );
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE );
    }

    @Test
    void testRecoveryAttemptLocked()
    {
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE );
        String username = "dr_evil";
        service.registerRecoveryAttempt( username );
        assertFalse( service.isRecoveryLocked( username ) );
        service.registerRecoveryAttempt( username );
        assertFalse( service.isRecoveryLocked( username ) );
        service.registerRecoveryAttempt( username );
        assertFalse( service.isRecoveryLocked( username ) );
        service.registerRecoveryAttempt( username );
        assertFalse( service.isRecoveryLocked( username ) );
        service.registerRecoveryAttempt( username );
        assertFalse( service.isRecoveryLocked( username ) );
        service.registerRecoveryAttempt( username );
        assertTrue( service.isRecoveryLocked( username ) );
        systemSettingManager.saveSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE );
    }

    @Test
    void testRestoreRecoverPassword()
    {
        String encodedTokens = service.generateAndPersistTokens( userA,
            RestoreOptions.RECOVER_PASSWORD_OPTION );
        assertNotNull( encodedTokens );
        assertNotNull( userA.getRestoreToken() );
        assertNotNull( userA.getIdToken() );
        assertNotNull( userA.getRestoreExpiry() );
        String[] idAndHashedToken = service.decodeEncodedTokens( encodedTokens );
        String idToken = idAndHashedToken[0];
        String restoreToken = idAndHashedToken[1];
        User credentials = userService.getUserByIdToken( idToken );
        assertNotNull( credentials );
        assertEquals( credentials, userA );
        RestoreOptions restoreOptions = service.getRestoreOptions( restoreToken );
        assertEquals( RestoreOptions.RECOVER_PASSWORD_OPTION, restoreOptions );
        assertEquals( RestoreType.RECOVER_PASSWORD, restoreOptions.getRestoreType() );
        assertFalse( restoreOptions.isUsernameChoice() );

        // verifyToken()

        assertNotNull( service.validateRestoreToken( userB, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertNotNull( service.validateRestoreToken( credentials, "badToken", RestoreType.RECOVER_PASSWORD ) );
        assertNotNull( service.validateRestoreToken( credentials, restoreToken, RestoreType.INVITE ) );
        assertNull( service.validateRestoreToken( credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        // canRestoreNow()

        assertFalse( service.canRestore( userB, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertFalse( service.canRestore( credentials, "badToken", RestoreType.RECOVER_PASSWORD ) );
        assertFalse( service.canRestore( credentials, restoreToken, RestoreType.INVITE ) );
        assertTrue( service.canRestore( credentials, restoreToken, RestoreType.RECOVER_PASSWORD ) );

        // restore()

        String password = "NewPassword1";
        assertFalse( service.restore( userB, restoreToken, password, RestoreType.INVITE ) );
        assertFalse( service.restore( credentials, "badToken", password, RestoreType.INVITE ) );
        assertFalse( service.restore( credentials, restoreToken, password, RestoreType.INVITE ) );
        assertTrue( service.restore( credentials, restoreToken, password, RestoreType.RECOVER_PASSWORD ) );

        // check password

        assertTrue( passwordManager.matches( password, credentials.getPassword() ) );
    }

    @Test
    void testRestoreInvite()
    {
        String encodedTokens = service.generateAndPersistTokens( userA,
            RestoreOptions.INVITE_WITH_DEFINED_USERNAME );
        assertNotNull( encodedTokens );
        assertNotNull( userA.getRestoreToken() );
        assertNotNull( userA.getIdToken() );
        assertNotNull( userA.getRestoreExpiry() );
        String[] idAndHashedToken = service.decodeEncodedTokens( encodedTokens );
        String idToken = idAndHashedToken[0];
        String restoreToken = idAndHashedToken[1];
        RestoreOptions restoreOptions = service.getRestoreOptions( restoreToken );
        assertEquals( RestoreOptions.INVITE_WITH_DEFINED_USERNAME, restoreOptions );
        assertEquals( RestoreType.INVITE, restoreOptions.getRestoreType() );
        assertFalse( restoreOptions.isUsernameChoice() );
        User credentials = userService.getUserByIdToken( idToken );
        assertNotNull( credentials );
        assertEquals( credentials, userA );

        // verifyToken()

        assertNotNull( service.validateRestoreToken( userB, restoreToken, RestoreType.INVITE ) );
        assertNotNull( service.validateRestoreToken( userA, "badToken", RestoreType.INVITE ) );
        assertNotNull( service.validateRestoreToken( userA, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertNull( service.validateRestoreToken( userA, restoreToken, RestoreType.INVITE ) );

        // canRestoreNow()

        assertFalse( service.canRestore( userB, restoreToken, RestoreType.INVITE ) );
        assertFalse( service.canRestore( userA, "badToken", RestoreType.INVITE ) );
        assertFalse( service.canRestore( userA, restoreToken, RestoreType.RECOVER_PASSWORD ) );
        assertTrue( service.canRestore( userA, restoreToken, RestoreType.INVITE ) );

        // restore()

        String password = "NewPassword1";
        assertFalse( service.restore( userB, restoreToken, password, RestoreType.INVITE ) );
        assertFalse( service.restore( userA, "badToken", password, RestoreType.INVITE ) );
        assertFalse( service.restore( userA, restoreToken, password, RestoreType.RECOVER_PASSWORD ) );
        assertTrue( service.restore( userA, restoreToken, password, RestoreType.INVITE ) );

        // check password

        assertTrue( passwordManager.matches( password, userA.getPassword() ) );
    }
}
