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

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.RestoreOptions;
import org.hisp.dhis.user.RestoreType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecurityServiceTest extends TransactionalIntegrationTest {
  @Autowired private UserService _userService;

  @Autowired private PasswordManager passwordManager;

  @Autowired private SystemSettingManager systemSettingManager;

  private User userA;

  private User userB;

  @Override
  public void setUpTest() {
    this.userService = _userService;
    userA = createAndAddUser("johndoe");
    userB = createAndAddUser("janesmith");
  }

  @Test
  void testUserAuthenticationLockout() {
    systemSettingManager.saveSystemSetting(SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE);
    String username = "dr_evil";
    _userService.registerFailedLogin(username);
    assertFalse(_userService.isLocked(username));
    _userService.registerFailedLogin(username);
    assertFalse(_userService.isLocked(username));
    _userService.registerFailedLogin(username);
    assertFalse(_userService.isLocked(username));
    _userService.registerFailedLogin(username);
    assertTrue(_userService.isLocked(username));
    _userService.registerFailedLogin(username);
    assertTrue(_userService.isLocked(username));
    _userService.registerSuccessfulLogin(username);
    assertFalse(_userService.isLocked(username));
    systemSettingManager.saveSystemSetting(SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE);
  }

  @Test
  void testRecoveryAttemptLocked() {
    systemSettingManager.saveSystemSetting(SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.TRUE);
    String username = "dr_evil";
    _userService.registerRecoveryAttempt(username);
    assertFalse(_userService.isRecoveryLocked(username));
    _userService.registerRecoveryAttempt(username);
    assertFalse(_userService.isRecoveryLocked(username));
    _userService.registerRecoveryAttempt(username);
    assertFalse(_userService.isRecoveryLocked(username));
    _userService.registerRecoveryAttempt(username);
    assertFalse(_userService.isRecoveryLocked(username));
    _userService.registerRecoveryAttempt(username);
    assertFalse(_userService.isRecoveryLocked(username));
    _userService.registerRecoveryAttempt(username);
    assertTrue(_userService.isRecoveryLocked(username));
    systemSettingManager.saveSystemSetting(SettingKey.LOCK_MULTIPLE_FAILED_LOGINS, Boolean.FALSE);
  }

  @Test
  void testRestoreRecoverPassword() {
    String encodedTokens =
        _userService.generateAndPersistTokens(userA, RestoreOptions.RECOVER_PASSWORD_OPTION);
    assertNotNull(encodedTokens);
    assertNotNull(userA.getRestoreToken());
    assertNotNull(userA.getIdToken());
    assertNotNull(userA.getRestoreExpiry());
    String[] idAndHashedToken = _userService.decodeEncodedTokens(encodedTokens);
    String idToken = idAndHashedToken[0];
    String restoreToken = idAndHashedToken[1];
    User credentials = _userService.getUserByIdToken(idToken);
    assertNotNull(credentials);
    assertEquals(credentials, userA);
    RestoreOptions restoreOptions = _userService.getRestoreOptions(restoreToken);
    assertEquals(RestoreOptions.RECOVER_PASSWORD_OPTION, restoreOptions);
    assertEquals(RestoreType.RECOVER_PASSWORD, restoreOptions.getRestoreType());
    assertFalse(restoreOptions.isUsernameChoice());

    // verifyToken()

    assertNotNull(
        _userService.validateRestoreToken(userB, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertNotNull(
        _userService.validateRestoreToken(credentials, "badToken", RestoreType.RECOVER_PASSWORD));
    assertNotNull(_userService.validateRestoreToken(credentials, restoreToken, RestoreType.INVITE));
    assertNull(
        _userService.validateRestoreToken(credentials, restoreToken, RestoreType.RECOVER_PASSWORD));

    // canRestoreNow()

    assertFalse(_userService.canRestore(userB, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertFalse(_userService.canRestore(credentials, "badToken", RestoreType.RECOVER_PASSWORD));
    assertFalse(_userService.canRestore(credentials, restoreToken, RestoreType.INVITE));
    assertTrue(_userService.canRestore(credentials, restoreToken, RestoreType.RECOVER_PASSWORD));

    // restore()

    String password = "NewPassword1";
    assertFalse(_userService.restore(userB, restoreToken, password, RestoreType.INVITE));
    assertFalse(_userService.restore(credentials, "badToken", password, RestoreType.INVITE));
    assertFalse(_userService.restore(credentials, restoreToken, password, RestoreType.INVITE));
    assertTrue(
        _userService.restore(credentials, restoreToken, password, RestoreType.RECOVER_PASSWORD));

    // check password

    assertTrue(passwordManager.matches(password, credentials.getPassword()));
  }

  @Test
  void testRestoreInvite() {
    String encodedTokens =
        _userService.generateAndPersistTokens(userA, RestoreOptions.INVITE_WITH_DEFINED_USERNAME);
    assertNotNull(encodedTokens);
    assertNotNull(userA.getRestoreToken());
    assertNotNull(userA.getIdToken());
    assertNotNull(userA.getRestoreExpiry());
    String[] idAndHashedToken = _userService.decodeEncodedTokens(encodedTokens);
    String idToken = idAndHashedToken[0];
    String restoreToken = idAndHashedToken[1];
    RestoreOptions restoreOptions = _userService.getRestoreOptions(restoreToken);
    assertEquals(RestoreOptions.INVITE_WITH_DEFINED_USERNAME, restoreOptions);
    assertEquals(RestoreType.INVITE, restoreOptions.getRestoreType());
    assertFalse(restoreOptions.isUsernameChoice());
    User credentials = _userService.getUserByIdToken(idToken);
    assertNotNull(credentials);
    assertEquals(credentials, userA);

    // verifyToken()

    assertNotNull(_userService.validateRestoreToken(userB, restoreToken, RestoreType.INVITE));
    assertNotNull(_userService.validateRestoreToken(userA, "badToken", RestoreType.INVITE));
    assertNotNull(
        _userService.validateRestoreToken(userA, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertNull(_userService.validateRestoreToken(userA, restoreToken, RestoreType.INVITE));

    // canRestoreNow()

    assertFalse(_userService.canRestore(userB, restoreToken, RestoreType.INVITE));
    assertFalse(_userService.canRestore(userA, "badToken", RestoreType.INVITE));
    assertFalse(_userService.canRestore(userA, restoreToken, RestoreType.RECOVER_PASSWORD));
    assertTrue(_userService.canRestore(userA, restoreToken, RestoreType.INVITE));

    // restore()

    String password = "NewPassword1";
    assertFalse(_userService.restore(userB, restoreToken, password, RestoreType.INVITE));
    assertFalse(_userService.restore(userA, "badToken", password, RestoreType.INVITE));
    assertFalse(_userService.restore(userA, restoreToken, password, RestoreType.RECOVER_PASSWORD));
    assertTrue(_userService.restore(userA, restoreToken, password, RestoreType.INVITE));

    // check password

    assertTrue(passwordManager.matches(password, userA.getPassword()));
  }

  @Test
  public void testValidateRestoreError() {
    User user = new User();
    user.setUsername("username");

    assertEquals(ErrorCode.E6201, _userService.validateRestore(null));
    assertEquals(ErrorCode.E6202, _userService.validateRestore(user));
  }

  @Test
  public void testValidateInviteError() {
    User user = new User();
    user.setUsername("username");

    assertEquals(ErrorCode.E6201, _userService.validateInvite(null));
    assertEquals(ErrorCode.E6202, _userService.validateInvite(user));
  }

  @Test
  public void testValidateRestoreTokenError() {
    User user = new User();
    user.setUsername("username");

    assertEquals(
        ErrorCode.E6205, _userService.validateRestoreToken(user, null, RestoreType.INVITE));
    assertEquals(ErrorCode.E6206, _userService.validateRestoreToken(user, "XTOKEN", null));
    assertEquals(
        ErrorCode.E6207, _userService.validateRestoreToken(user, "XTOKEN", RestoreType.INVITE));
  }
}
