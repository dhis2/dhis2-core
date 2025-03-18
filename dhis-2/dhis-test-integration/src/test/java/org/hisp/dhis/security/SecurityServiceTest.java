/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import java.util.Set;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.RestoreOptions;
import org.hisp.dhis.user.RestoreType;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SecurityServiceTest extends PostgresIntegrationTestBase {
  @Autowired private PasswordManager passwordManager;

  @Autowired private SystemSettingsService settingsService;

  private User userA;

  private User userB;

  @BeforeEach
  void setUp() {
    userA = createAndAddUser("johndoe");
    userB = createAndAddUser("janesmith");
  }

  @Test
  void testUserAuthenticationLockout() {
    settingsService.put("keyLockMultipleFailedLogins", true);
    settingsService.clearCurrentSettings();
    String username = "dr_evil";
    userService.registerFailedLogin(username);
    assertFalse(userService.isLocked(username));
    userService.registerFailedLogin(username);
    assertFalse(userService.isLocked(username));
    userService.registerFailedLogin(username);
    assertFalse(userService.isLocked(username));
    userService.registerFailedLogin(username);
    assertTrue(userService.isLocked(username));
    userService.registerFailedLogin(username);
    assertTrue(userService.isLocked(username));
    userService.registerSuccessfulLogin(username);
    assertFalse(userService.isLocked(username));
    settingsService.deleteAll(Set.of("keyLockMultipleFailedLogins"));
  }

  @Test
  void testRecoveryAttemptLocked() {
    settingsService.put("keyLockMultipleFailedLogins", true);
    settingsService.clearCurrentSettings();
    String username = "dr_evil";
    userService.registerRecoveryAttempt(username);
    assertFalse(userService.isRecoveryLocked(username));
    userService.registerRecoveryAttempt(username);
    assertFalse(userService.isRecoveryLocked(username));
    userService.registerRecoveryAttempt(username);
    assertFalse(userService.isRecoveryLocked(username));
    userService.registerRecoveryAttempt(username);
    assertFalse(userService.isRecoveryLocked(username));
    userService.registerRecoveryAttempt(username);
    assertFalse(userService.isRecoveryLocked(username));
    userService.registerRecoveryAttempt(username);
    assertTrue(userService.isRecoveryLocked(username));
    settingsService.deleteAll(Set.of("keyLockMultipleFailedLogins"));
  }

  @Test
  void testRestoreRecoverPassword() {
    String encodedTokens =
        userService.generateAndPersistTokens(userA, RestoreOptions.RECOVER_PASSWORD_OPTION);
    assertNotNull(encodedTokens);
    assertNotNull(userA.getRestoreToken());
    assertNotNull(userA.getIdToken());
    assertNotNull(userA.getRestoreExpiry());
    String[] idAndHashedToken = userService.decodeEncodedTokens(encodedTokens);
    String idToken = idAndHashedToken[0];
    String restoreToken = idAndHashedToken[1];
    User credentials = userService.getUserByIdToken(idToken);
    assertNotNull(credentials);
    assertEquals(credentials, userA);
    RestoreOptions restoreOptions = userService.getRestoreOptions(restoreToken);
    assertEquals(RestoreOptions.RECOVER_PASSWORD_OPTION, restoreOptions);
    assertEquals(RestoreType.RECOVER_PASSWORD, restoreOptions.getRestoreType());
    assertFalse(restoreOptions.isUsernameChoice());

    // verifyToken()

    assertNotNull(
        userService.validateRestoreToken(userB, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertNotNull(
        userService.validateRestoreToken(credentials, "badToken", RestoreType.RECOVER_PASSWORD));
    assertNotNull(userService.validateRestoreToken(credentials, restoreToken, RestoreType.INVITE));
    assertNull(
        userService.validateRestoreToken(credentials, restoreToken, RestoreType.RECOVER_PASSWORD));

    // canRestoreNow()

    assertFalse(userService.canRestore(userB, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertFalse(userService.canRestore(credentials, "badToken", RestoreType.RECOVER_PASSWORD));
    assertFalse(userService.canRestore(credentials, restoreToken, RestoreType.INVITE));
    assertTrue(userService.canRestore(credentials, restoreToken, RestoreType.RECOVER_PASSWORD));

    // restore()

    String password = "NewPassword1";
    assertFalse(userService.restore(userB, restoreToken, password, RestoreType.INVITE));
    assertFalse(userService.restore(credentials, "badToken", password, RestoreType.INVITE));
    assertFalse(userService.restore(credentials, restoreToken, password, RestoreType.INVITE));
    assertTrue(
        userService.restore(credentials, restoreToken, password, RestoreType.RECOVER_PASSWORD));

    // check password

    assertTrue(passwordManager.matches(password, credentials.getPassword()));
  }

  @Test
  void testRestoreInvite() {
    String encodedTokens =
        userService.generateAndPersistTokens(userA, RestoreOptions.INVITE_WITH_DEFINED_USERNAME);
    assertNotNull(encodedTokens);
    assertNotNull(userA.getRestoreToken());
    assertNotNull(userA.getIdToken());
    assertNotNull(userA.getRestoreExpiry());
    String[] idAndHashedToken = userService.decodeEncodedTokens(encodedTokens);
    String idToken = idAndHashedToken[0];
    String restoreToken = idAndHashedToken[1];
    RestoreOptions restoreOptions = userService.getRestoreOptions(restoreToken);
    assertEquals(RestoreOptions.INVITE_WITH_DEFINED_USERNAME, restoreOptions);
    assertEquals(RestoreType.INVITE, restoreOptions.getRestoreType());
    assertFalse(restoreOptions.isUsernameChoice());
    User credentials = userService.getUserByIdToken(idToken);
    assertNotNull(credentials);
    assertEquals(credentials, userA);

    // verifyToken()

    assertNotNull(userService.validateRestoreToken(userB, restoreToken, RestoreType.INVITE));
    assertNotNull(userService.validateRestoreToken(userA, "badToken", RestoreType.INVITE));
    assertNotNull(
        userService.validateRestoreToken(userA, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertNull(userService.validateRestoreToken(userA, restoreToken, RestoreType.INVITE));

    // canRestoreNow()

    assertFalse(userService.canRestore(userB, restoreToken, RestoreType.INVITE));
    assertFalse(userService.canRestore(userA, "badToken", RestoreType.INVITE));
    assertFalse(userService.canRestore(userA, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertTrue(userService.canRestore(userA, restoreToken, RestoreType.INVITE));

    // restore()

    String password = "NewPassword1";
    assertFalse(userService.restore(userB, restoreToken, password, RestoreType.INVITE));
    assertFalse(userService.restore(userA, "badToken", password, RestoreType.INVITE));
    assertFalse(userService.restore(userA, restoreToken, password, RestoreType.RECOVER_PASSWORD));
    assertTrue(userService.restore(userA, restoreToken, password, RestoreType.INVITE));

    // check password

    assertTrue(passwordManager.matches(password, userA.getPassword()));
  }

  @Test
  public void testValidateRestoreError() {
    User user = new User();
    user.setUsername("username");

    assertEquals(ErrorCode.E6201, userService.validateRestore(null));
    assertEquals(ErrorCode.E6202, userService.validateRestore(user));
  }

  @Test
  public void testValidateInviteError() {
    User user = new User();
    user.setUsername("username");

    assertEquals(ErrorCode.E6201, userService.validateInvite(null));
    assertEquals(ErrorCode.E6202, userService.validateInvite(user));
  }

  @Test
  public void testValidateRestoreTokenError() {
    User user = new User();
    user.setUsername("username");

    assertEquals(ErrorCode.E6205, userService.validateRestoreToken(user, null, RestoreType.INVITE));
    assertEquals(ErrorCode.E6206, userService.validateRestoreToken(user, "XTOKEN", null));
    assertEquals(
        ErrorCode.E6207, userService.validateRestoreToken(user, "XTOKEN", RestoreType.INVITE));
  }
}
