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
package org.hisp.dhis.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;
import org.hisp.dhis.common.UID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of {@link UserRoleSessionInvalidationListener}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class UserRoleSessionInvalidationListenerTest {

  private static final UID ROLE_UID = UID.of("Rab1234abcd");

  @Mock private UserService userService;

  @InjectMocks private UserRoleSessionInvalidationListener listener;

  @Test
  void invalidatesSessionsOfAllRoleMembersInOrder() {
    List<String> usernames = List.of("alice", "bob", "carol");
    when(userService.getUsernamesByUserRole(ROLE_UID)).thenReturn(usernames);

    listener.onUserRoleAuthoritiesChanged(new UserRoleAuthoritiesChangedEvent(ROLE_UID));

    InOrder order = inOrder(userService);
    usernames.forEach(username -> order.verify(userService).invalidateUserSessions(username));
  }

  @Test
  void invalidatesSessionsOfAllRoleMembersAcrossBatches() {
    List<String> usernames =
        IntStream.range(0, UserRoleSessionInvalidationListener.BATCH_SIZE + 1)
            .mapToObj(i -> "user" + i)
            .toList();
    when(userService.getUsernamesByUserRole(ROLE_UID)).thenReturn(usernames);

    listener.onUserRoleAuthoritiesChanged(new UserRoleAuthoritiesChangedEvent(ROLE_UID));

    verify(userService, times(usernames.size())).invalidateUserSessions(anyString());
  }

  @Test
  void invalidatesNothingWhenRoleHasNoMembers() {
    when(userService.getUsernamesByUserRole(ROLE_UID)).thenReturn(List.of());

    listener.onUserRoleAuthoritiesChanged(new UserRoleAuthoritiesChangedEvent(ROLE_UID));

    verify(userService, never()).invalidateUserSessions(anyString());
  }
}
