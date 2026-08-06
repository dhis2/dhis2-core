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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRoleBundleHookTest {

  @Mock private UserService userService;
  @Mock private ObjectBundle bundle;

  @Test
  @DisplayName("Members are read as a username projection, never off the members collection")
  void invalidatesUsingTheUsernameProjection() {
    UserRoleBundleHook hook = new UserRoleBundleHook(userService);
    UserRole role = roleWhoseMembersMustNotBeRead();

    when(bundle.getExtras(role, UserRoleBundleHook.INVALIDATE_SESSION_KEY))
        .thenReturn(Boolean.TRUE);
    when(userService.getUsernamesByUserRole(UID.of(role))).thenReturn(List.of("alice", "bob"));

    hook.postUpdate(role, bundle);

    verify(userService).invalidateUserSessions(List.of("alice", "bob"));
    verify(userService, never()).invalidateUserSessions(anyString());
  }

  @Test
  @DisplayName("Nothing is invalidated when the authorities did not change")
  void doesNothingWhenAuthoritiesUnchanged() {
    UserRoleBundleHook hook = new UserRoleBundleHook(userService);
    UserRole role = roleWhoseMembersMustNotBeRead();

    when(bundle.getExtras(role, UserRoleBundleHook.INVALIDATE_SESSION_KEY))
        .thenReturn(Boolean.FALSE);

    hook.postUpdate(role, bundle);

    verify(userService, never()).getUsernamesByUserRole(any());
    verify(userService, never()).invalidateUserSessions(any(Collection.class));
  }

  /**
   * Stands in for a role loaded from the database: reading {@code members} there initialises the
   * lazy collection, hydrating one {@link User} entity per member. That is what the hook must not
   * do, so here it fails the test instead.
   */
  private static UserRole roleWhoseMembersMustNotBeRead() {
    UserRole role =
        new UserRole() {
          @Override
          public List<User> getUsers() {
            fail("the hook must not initialise the members collection");
            return List.of();
          }
        };
    role.setName("Role");
    role.setAutoFields();
    return role;
  }
}
