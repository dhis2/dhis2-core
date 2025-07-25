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
package org.hisp.dhis.webapi.controller.user;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link UserController}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {
  @Mock private UserService userService;

  @Mock private UserGroupService userGroupService;

  @Mock private AclService aclService;

  @InjectMocks private UserController userController;

  private User currentUser;

  private User user;

  private User parsedUser;

  @BeforeEach
  public void setUp() {
    UserGroup userGroup1 = new UserGroup();
    userGroup1.setUid("abc1");

    UserGroup userGroup2 = new UserGroup();
    userGroup2.setUid("abc2");

    currentUser = new User();
    currentUser.setId(1000);
    currentUser.setUid("def1");

    user = new User();
    user.setId(1001);
    user.setUid("def2");

    parsedUser = new User();
    parsedUser.setUid("def2");
    parsedUser.setGroups(new HashSet<>(Arrays.asList(userGroup1, userGroup2)));
  }

  @Test
  void updateUserGroupsNotOk() {
    if (isInStatusUpdatedOK(createReportWith(Status.ERROR, report -> report.updatedInc(1)))) {
      userController.updateUserGroups("def2", parsedUser, UserDetails.fromUser(currentUser));
    }

    verifyNoInteractions(userService);
    verifyNoInteractions(userGroupService);
  }

  @Test
  void updateUserGroupsNotUpdated() {
    if (isInStatusUpdatedOK(createReportWith(Status.OK, report -> report.createdInc(1)))) {
      userController.updateUserGroups("def2", parsedUser, UserDetails.fromUser(currentUser));
    }

    verifyNoInteractions(userService);
    verifyNoInteractions(userGroupService);
  }

  @Test
  void updateUserGroupsSameUser() {
    currentUser.setId(1001);
    currentUser.setUid("def2");

    User currentUser2 = new User();
    currentUser2.setId(1001);
    currentUser2.setUid("def2");

    when(userService.getUser("def2")).thenReturn(user);

    if (isInStatusUpdatedOK(createReportWith(Status.OK, report -> report.updatedInc(1)))) {
      userController.updateUserGroups("def2", parsedUser, UserDetails.fromUser(currentUser));
    }

    verify(userGroupService)
        .updateUserGroups(user, Set.of("abc1", "abc2"), UserDetails.fromUser(currentUser2));
  }

  private ImportReport createReportWith(Status status, Consumer<TypeReport> operation) {
    TypeReport typeReport = new TypeReport(User.class);
    operation.accept(typeReport);
    ImportReport report = new ImportReport();
    report.setStatus(status);
    report.addTypeReport(typeReport);
    return report;
  }

  private boolean isInStatusUpdatedOK(ImportReport report) {
    return report.getStatus() == Status.OK && report.getStats().updated() == 1;
  }

  public static void injectSecurityContext(UserDetails currentUserDetails) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            currentUserDetails, "", currentUserDetails.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  private void setUpUserExpireScenarios() {
    addUserTo(user);
    addUserTo(currentUser);
    // make current user have ALL authority
    setUpUserAuthority(currentUser, Authorities.ALL.toString());

    injectSecurityContext(UserDetails.fromUser(currentUser));

    // allow any change
    when(aclService.canUpdate(any(UserDetails.class), any())).thenReturn(true);

    lenient().when(userService.canAddOrUpdateUser(any(), any())).thenReturn(true);
    // link user and current user to service methods
    when(userService.getUser(user.getUid())).thenReturn(user);
  }

  @Test
  void expireUserInTheFutureDoesNotExpireSession() throws Exception {
    setUpUserExpireScenarios();
    when(userService.canAddOrUpdateUser(any())).thenReturn(true);

    Date inTheFuture = new Date(System.currentTimeMillis() + 1000);
    userController.expireUser(user.getUid(), inTheFuture);

    assertUserUpdatedWithAccountExpiry(inTheFuture);
    verify(userService, never()).invalidateUserSessions(any());
  }

  @Test
  void expireUserNowDoesExpireSession() throws Exception {
    setUpUserExpireScenarios();
    when(userService.canAddOrUpdateUser(any())).thenReturn(true);
    Date now = new Date();
    userController.expireUser(user.getUid(), now);

    assertUserUpdatedWithAccountExpiry(now);
    verify(userService, atLeastOnce()).invalidateUserSessions(same(user.getUsername()));
  }

  @Test
  void unexpireUserDoesUpdateUser() throws Exception {
    setUpUserExpireScenarios();

    when(userService.canAddOrUpdateUser(any())).thenReturn(true);
    userController.unexpireUser(user.getUid());

    assertUserUpdatedWithAccountExpiry(null);
  }

  @Test
  void updateUserExpireRequiresUserBasedAuthority() {
    setUpUserExpireScenarios();
    // executing user has no authorities
    currentUser.setUserRoles(emptySet());
    // changed user does have an authority
    setUpUserAuthority(user, "whatever");

    WebMessageException ex =
        assertThrows(
            WebMessageException.class, () -> userController.expireUser(user.getUid(), new Date()));
    assertEquals(
        "You must have permissions to create user, or ability to manage at least one user group for the user.",
        ex.getWebMessage().getMessage());
  }

  @Test
  void updateUserExpireRequiresGroupBasedAuthority() {
    setUpUserExpireScenarios();
    when(userService.canAddOrUpdateUser(any())).thenReturn(false);

    WebMessageException ex =
        assertThrows(
            WebMessageException.class, () -> userController.expireUser(user.getUid(), new Date()));
    assertEquals(
        "You must have permissions to create user, or ability to manage at least one user group for the user.",
        ex.getWebMessage().getMessage());
  }

  @Test
  void updateUserExpireRequiresShareBasedAuthority() {
    addUserTo(user);
    addUserTo(currentUser);
    setUpUserAuthority(currentUser, Authorities.ALL.toString());
    injectSecurityContext(UserDetails.fromUser(currentUser));
    when(aclService.canUpdate(any(UserDetails.class), any())).thenReturn(false);
    lenient().when(userService.canAddOrUpdateUser(any(), any())).thenReturn(true);
    when(userService.getUser(user.getUid())).thenReturn(user);

    Exception ex =
        assertThrows(
            ForbiddenException.class, () -> userController.expireUser(user.getUid(), new Date()));
    assertEquals("You don't have the proper permissions to update this object.", ex.getMessage());
  }

  private void setUpUserAuthority(User user, String authority) {
    UserRole suGroup = new UserRole();
    suGroup.setAuthorities(singleton(authority));
    user.setUserRoles(singleton(suGroup));
  }

  private void assertUserUpdatedWithAccountExpiry(Date accountExpiry) {
    ArgumentCaptor<User> credentials = ArgumentCaptor.forClass(User.class);
    verify(userService).updateUser(credentials.capture());
    User actual = credentials.getValue();
    assertSame(actual, user, "no user credentials update occurred");
    assertEquals(accountExpiry, actual.getAccountExpiry(), "date was not updated");
    assertEquals(user.isAccountNonExpired(), actual.isAccountNonExpired());
  }

  private static void addUserTo(User user) {
    User credentials = new User();
    credentials.setUser(user);
    credentials.setUid(user.getUid());
  }

  @Test
  @SuppressWarnings("unchecked")
  void updateUserGroups() {
    when(userService.getUser("def2")).thenReturn(user);
    UserDetails currentUserDetails = UserDetails.fromUser(currentUser);

    if (isInStatusUpdatedOK(createReportWith(Status.OK, report -> report.updatedInc(1)))) {
      userController.updateUserGroups("def2", parsedUser, currentUserDetails);
    }

    verify(userGroupService)
        .updateUserGroups(
            same(user),
            (Collection<String>) argThat(containsInAnyOrder("abc1", "abc2")),
            same(currentUserDetails));
  }
}
