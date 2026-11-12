/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.http.HttpAssertions.assertSeries;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * Verifies through the real security filter chain that changes made on write paths outside the
 * metadata import reach the session of the changed user on its next request: a user role revoked
 * through the role endpoint is gone, and a deleted or disabled user is logged out. A login, which
 * updates the user, does not mark the user as changed.
 *
 * <p>Changes are only picked up after they have committed, so each change is committed via {@link
 * TestTransaction} and the after-commit work drained before the next request. {@link
 * TestInstance.Lifecycle#PER_CLASS} makes the framework empty the database after the class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDetailsWritePathRefreshTest extends AuthenticationApiTestBase {

  private static final String AUTHORITY = "F_WRITE_PATH_CHECK";

  @Autowired private UserDetailsChangeTracker userDetailsChangeTracker;

  @Autowired
  @Qualifier("userDetailsChangeTaskExecutor")
  private ThreadPoolTaskExecutor userDetailsChangeExecutor;

  @Test
  void userRoleRevokedThroughRoleEndpointIsGoneOnTheNextRequest() throws Exception {
    User user = createUserWithAuth("revokeduser");
    UserRole role = createUserRole('V', AUTHORITY);
    userService.addUserRole(role);
    user.getUserRoles().add(role);
    userService.updateUser(user);
    MockHttpSession userSession = loggedInSession(user);
    commit();
    assertEquals("true", hasAuthority(userSession).getContentAsString());

    TestTransaction.start();
    assertSeries(
        HttpStatus.Series.SUCCESSFUL,
        DELETE("/userRoles/" + role.getUid() + "/users/" + user.getUid()));
    commit();

    MockHttpServletResponse response = hasAuthority(userSession);
    assertEquals(200, response.getStatus());
    assertEquals("false", response.getContentAsString());
    assertFalse(userSession.isInvalid());
  }

  @Test
  void deletingUserEndsTheSession() {
    User user = createUserWithAuth("deleteduser");
    MockHttpSession userSession = loggedInSession(user);
    commit();
    assertEquals(200, hasAuthority(userSession).getStatus());

    TestTransaction.start();
    DELETE("/users/" + user.getUid()).content(OK);
    commit();

    assertNotEquals(200, hasAuthority(userSession).getStatus());
    assertTrue(userSession.isInvalid());
  }

  @Test
  void disablingUserThroughPutEndsTheSession() {
    User user = createUserWithAuth("putdisableduser");
    String roles =
        user.getUserRoles().stream()
            .map(role -> "{'id':'" + role.getUid() + "'}")
            .collect(Collectors.joining(","));
    MockHttpSession userSession = loggedInSession(user);
    commit();
    assertEquals(200, hasAuthority(userSession).getStatus());

    TestTransaction.start();
    PUT(
            "/users/" + user.getUid(),
            "{'id':'%s','username':'%s','firstName':'%s','surname':'%s','userRoles':[%s],'disabled':true}"
                .formatted(
                    user.getUid(),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getSurname(),
                    roles))
        .content(OK);
    commit();

    assertNotEquals(200, hasAuthority(userSession).getStatus());
    assertTrue(userSession.isInvalid());
  }

  @Test
  void loginDoesNotMarkTheUserAsChanged() throws Exception {
    User user = createUserWithAuth("loginuser");
    commit();

    TestTransaction.start();
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
    MockHttpServletResponse response =
        mvc.perform(get("/api/me").with(httpBasic(user.getUsername(), DEFAULT_ADMIN_PASSWORD)))
            .andReturn()
            .getResponse();
    commit();

    assertEquals(200, response.getStatus());
    TestTransaction.start();
    assertNotNull(userService.getUser(user.getUid()).getLastLogin(), "the login updates the user");
    TestTransaction.end();
    assertNull(userDetailsChangeTracker.getChangeToken(user.getUsername()));
  }

  private MockHttpSession loggedInSession(User user) {
    UserDetails principal = userService.createUserDetails(user);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        new SecurityContextImpl(
            new DhisAuthenticationToken(
                principal, null, principal.getAuthorities(), LoginMethod.LOCAL)));
    return session;
  }

  /** Requests as the user of the given session only, not as the admin of the test. */
  private MockHttpServletResponse hasAuthority(MockHttpSession userSession) {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
    try {
      return mvc.perform(get("/api/me/authorities/" + AUTHORITY).session(userSession))
          .andReturn()
          .getResponse();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /** Commits the test transaction and waits for the after-commit work to have run. */
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
