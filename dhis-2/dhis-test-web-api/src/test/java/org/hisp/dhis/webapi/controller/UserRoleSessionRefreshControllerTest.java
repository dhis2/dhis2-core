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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that users are marked as changed, instead of being logged out, when the authorities of
 * one of their {@link UserRole}s change or when their user roles change. The marking happens after
 * the transaction has committed, off the request thread.
 *
 * <p>Because the marking runs {@code AFTER_COMMIT} on a separate thread, the change has to be
 * committed (via {@link TestTransaction}) and the executor drained before asserting. {@link
 * TestInstance.Lifecycle#PER_CLASS} makes the framework empty the database after the class, so the
 * committed data does not leak into other tests.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRoleSessionRefreshControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private SessionRegistry sessionRegistry;

  @Autowired private UserDetailsChangeTracker userDetailsChangeTracker;

  @Autowired
  @Qualifier("userDetailsChangeTaskExecutor")
  private ThreadPoolTaskExecutor userDetailsChangeExecutor;

  @Test
  void authorityChangeMarksRoleMembersAsChanged() {
    User member = createUserWithAuth("authoritymember");
    UserDetails principal = userService.createUserDetails(member);
    UserRole role = createUserRole('B', "F_DATAVALUE_ADD");
    userService.addUserRole(role);
    assignRole(member, role);
    commit();
    String tokenBefore = changeToken(member);

    sessionRegistry.registerNewSession("authoritySession", principal);
    TestTransaction.start();
    PATCH("/userRoles/" + role.getUid(), "[{'op':'add','path':'/authorities','value':['NONE']}]")
        .content(OK);
    commit();

    String tokenAfter = changeToken(member);
    assertNotNull(tokenAfter);
    assertNotEquals(tokenBefore, tokenAfter);
    assertFalse(sessionRegistry.getAllSessions(principal, false).isEmpty());
  }

  @Test
  void userRoleChangeMarksUserAsChangedAfterCommit() {
    User member = createUserWithAuth("assignedmember");
    UserRole role = createUserRole('C', "F_DATAVALUE_ADD");
    userService.addUserRole(role);

    assignRole(member, role);
    assertNull(changeToken(member), "the change must only be visible after the commit");
    commit();

    assertNotNull(changeToken(member));
  }

  private void assignRole(User user, UserRole role) {
    PATCH(
            "/users/" + user.getUid(),
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + role.getUid() + "'}]}]")
        .content(OK);
  }

  private String changeToken(User user) {
    return userDetailsChangeTracker.getChangeToken(user.getUsername());
  }

  /** Commits the test transaction and waits for the after-commit marking to have run. */
  private void commit() {
    TestTransaction.flagForCommit();
    TestTransaction.end();
    try {
      userDetailsChangeExecutor.submit(() -> {}).get(10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new IllegalStateException("User details changes were not processed", ex);
    }
  }
}
