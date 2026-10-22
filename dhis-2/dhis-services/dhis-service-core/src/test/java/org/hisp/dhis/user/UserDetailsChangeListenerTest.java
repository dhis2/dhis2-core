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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link UserDetailsChangeListener}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserDetailsChangeListenerTest {

  private static final UID ROLE_UID = UID.of("Rab1234abcd");

  private UserService userService;

  private UserDetailsChangeTracker tracker;

  private UserDetailsChangeListener listener;

  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.getIntProperty(ConfigurationKey.SYSTEM_SESSION_TIMEOUT)).thenReturn(3600);
    tracker = new InMemoryUserDetailsChangeTracker(config);
    listener = new UserDetailsChangeListener(userService, tracker);
  }

  @Test
  void marksAllRoleMembersAcrossBatchesAsChanged() {
    List<String> usernames =
        IntStream.range(0, UserDetailsChangeListener.BATCH_SIZE + 1)
            .mapToObj(i -> "user" + i)
            .toList();
    when(userService.getUsernamesByUserRole(ROLE_UID)).thenReturn(usernames);

    listener.onUserRoleAuthoritiesChanged(new UserRoleAuthoritiesChangedEvent(ROLE_UID));

    assertAll(usernames.stream().map(name -> () -> assertNotNull(tracker.getChangeToken(name))));
    verify(userService, never()).invalidateUserSessions(anyString());
  }

  @Test
  void marksOnlyUsersWhoseUserDetailsChanged() {
    when(userService.getUsernamesByIds(anyList())).thenReturn(List.of("alice", "bob"));

    listener.onUserDetailsChanged(new UserDetailsChangedEvent(Set.of(1L, 2L)));

    assertNotNull(tracker.getChangeToken("alice"));
    assertNotNull(tracker.getChangeToken("bob"));
    assertNull(tracker.getChangeToken("carol"));
    verify(userService, never()).invalidateUserSessions(anyString());
  }

  @Test
  void resolvesUsernamesOfChangedUsersInBatches() {
    Set<Long> userIds =
        LongStream.range(0, UserDetailsChangeListener.BATCH_SIZE + 1)
            .boxed()
            .collect(Collectors.toSet());
    List<Integer> batchSizes = new ArrayList<>();
    when(userService.getUsernamesByIds(anyList()))
        .thenAnswer(
            call -> {
              List<Long> batch = call.getArgument(0);
              batchSizes.add(batch.size());
              return batch.stream().map(id -> "user" + id).toList();
            });

    listener.onUserDetailsChanged(new UserDetailsChangedEvent(userIds));

    assertTrue(batchSizes.stream().allMatch(size -> size <= UserDetailsChangeListener.BATCH_SIZE));
    assertAll(userIds.stream().map(id -> () -> assertNotNull(tracker.getChangeToken("user" + id))));
  }
}
