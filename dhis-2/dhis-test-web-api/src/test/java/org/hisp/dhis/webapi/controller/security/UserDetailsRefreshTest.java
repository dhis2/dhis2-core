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

import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * Verifies through the real security filter chain that a logged-in user sees a change of their user
 * roles on the next request of the same session, and that the changes that must log the user out
 * still do.
 *
 * <p>Changes are only picked up after they have committed, so each change is committed via {@link
 * TestTransaction} and the after-commit work drained before the next request. {@link
 * TestInstance.Lifecycle#PER_CLASS} makes the framework empty the database after the class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDetailsRefreshTest extends AuthenticationApiTestBase {

  private static final String AUTHORITY = "F_REFRESH_CHECK";

  @Autowired private SessionRegistry sessionRegistry;

  @Autowired
  @Qualifier("userDetailsChangeTaskExecutor")
  private ThreadPoolTaskExecutor userDetailsChangeExecutor;

  @Test
  void newUserRoleIsVisibleOnTheNextRequestOfTheSameSession() throws Exception {
    User user = createUserWithAuth("refreshuser");
    UserRole role = createRole('R');
    MockHttpSession userSession = loggedInSession(user);
    commit();
    assertEquals("false", hasAuthority(userSession).getContentAsString());

    addRole(user, role);

    MockHttpServletResponse response = hasAuthority(userSession);
    assertEquals(200, response.getStatus());
    assertEquals("true", response.getContentAsString());
    assertFalse(userSession.isInvalid());
  }

  @Test
  void disablingUserStillEndsTheSession() {
    User user = createUserWithAuth("disableduser");
    MockHttpSession userSession = loggedInSession(user);
    UserDetails principal = principal(userSession);
    sessionRegistry.registerNewSession(userSession.getId(), principal);
    commit();
    assertEquals(200, hasAuthority(userSession).getStatus());

    TestTransaction.start();
    PATCH("/users/" + user.getUid(), "[{'op':'replace','path':'/disabled','value':true}]")
        .content(OK);
    commit();

    assertTrue(sessionRegistry.getAllSessions(principal, false).isEmpty());
    assertNotEquals(200, hasAuthority(userSession).getStatus());
  }

  @Test
  void changingImpersonatedUserEndsTheSession() throws Exception {
    User user = createUserWithAuth("impersonateduser");
    UserRole role = createRole('I');
    MockHttpSession userSession = impersonatingSession(user);
    commit();
    assertEquals("false", hasAuthority(userSession).getContentAsString());

    addRole(user, role);

    assertNotEquals(200, hasAuthority(userSession).getStatus());
    assertTrue(userSession.isInvalid());
  }

  private UserRole createRole(char uniqueCharacter) {
    UserRole role = createUserRole(uniqueCharacter, AUTHORITY);
    userService.addUserRole(role);
    return role;
  }

  private void addRole(User user, UserRole role) {
    TestTransaction.start();
    PATCH(
            "/users/" + user.getUid(),
            "[{'op':'add','path':'/userRoles','value':[{'id':'" + role.getUid() + "'}]}]")
        .content(OK);
    commit();
  }

  private MockHttpSession loggedInSession(User user) {
    UserDetails principal = userService.createUserDetails(user);
    return sessionWith(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
  }

  private MockHttpSession impersonatingSession(User user) {
    UserDetails principal = userService.createUserDetails(user);
    Authentication impersonator = SecurityContextHolder.getContext().getAuthentication();
    List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
    authorities.add(new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", impersonator));
    return sessionWith(
        UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
  }

  private static MockHttpSession sessionWith(Authentication authentication) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        new SecurityContextImpl(authentication));
    return session;
  }

  private static UserDetails principal(MockHttpSession session) {
    SecurityContextImpl context =
        (SecurityContextImpl)
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    return (UserDetails) context.getAuthentication().getPrincipal();
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
