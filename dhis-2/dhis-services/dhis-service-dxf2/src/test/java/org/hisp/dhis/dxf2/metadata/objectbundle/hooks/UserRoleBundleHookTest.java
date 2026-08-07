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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserRoleAuthoritiesChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit test of {@link UserRoleBundleHook}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class UserRoleBundleHookTest {

  private static final String ROLE_UID = "Rab1234abcd";

  @Mock private ApplicationEventPublisher eventPublisher;

  private UserRoleBundleHook hook;

  private ObjectBundle bundle;

  @BeforeEach
  void setUp() {
    hook = new UserRoleBundleHook(eventPublisher);
    bundle = new ObjectBundle(new ObjectBundleParams(), new Preheat(), Map.of());
  }

  @Test
  void postUpdatePublishesEventWhenAuthoritiesChanged() {
    UserRole existing = createUserRole(Set.of("F_USER_VIEW"));
    UserRole update = spy(createUserRole(Set.of("F_USER_VIEW", "F_USER_ADD")));

    hook.preUpdate(update, existing, bundle);
    hook.postUpdate(update, bundle);

    verify(eventPublisher).publishEvent(new UserRoleAuthoritiesChangedEvent(UID.of(ROLE_UID)));
    // the members of the role must never be loaded on the update path
    verify(update, never()).getUsers();
    verify(update, never()).getMembers();
  }

  @Test
  void postUpdateDoesNotPublishWhenAuthoritiesUnchanged() {
    UserRole existing = createUserRole(Set.of("F_USER_VIEW"));
    UserRole update = createUserRole(Set.of("F_USER_VIEW"));

    hook.preUpdate(update, existing, bundle);
    hook.postUpdate(update, bundle);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void postUpdateDoesNotPublishWithoutPrecedingPreUpdate() {
    hook.postUpdate(createUserRole(Set.of("F_USER_VIEW")), bundle);

    verifyNoInteractions(eventPublisher);
  }

  private static UserRole createUserRole(Set<String> authorities) {
    UserRole role = new UserRole();
    role.setUid(ROLE_UID);
    role.setAuthorities(authorities);
    return role;
  }
}
