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
package org.hisp.dhis.user.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * @author Morten Svanæs
 */
class DefaultAuthzServiceTest {

  private InMemoryAuthzVersionStore versionStore;
  private UserService userService;
  private DefaultAuthzService authzService;

  @BeforeEach
  void setUp() {
    versionStore = new InMemoryAuthzVersionStore();
    userService = mock(UserService.class);
    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.createUserDetailsAuthzCache())
        .thenAnswer(
            (Answer<Cache<?>>)
                invocation ->
                    new SimpleCacheBuilder<>()
                        .forRegion("userDetailsAuthzCache-test")
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .withInitialCapacity(16)
                        .withMaximumSize(100)
                        .build());
    authzService = new DefaultAuthzService(versionStore, userService, cacheProvider);
  }

  @Test
  void sameEpochReturnsSameInstanceAndLoadsOnce() {
    when(userService.createUserDetailsByUsername("alice")).thenReturn(details("alice", "u1"));

    UserDetails a = authzService.getFreshUserDetails("alice");
    UserDetails b = authzService.getFreshUserDetails("alice");

    assertNotNull(a);
    assertSame(a, b);
    assertEquals(0L, a.getAuthzCheckedEpoch());
    assertEquals(0L, a.getAuthzGen());
    verify(userService, times(1)).createUserDetailsByUsername("alice");
  }

  @Test
  void bumpOfOwnRoleForcesRebuild() {
    when(userService.createUserDetailsByUsername("alice"))
        .thenReturn(details("alice", "u1", Set.of("r1")), details("alice", "u1", Set.of("r1")));

    UserDetails a = authzService.getFreshUserDetails("alice");
    authzService.bumpRoleAuthz("r1");
    UserDetails b = authzService.getFreshUserDetails("alice");

    assertNotSame(a, b);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
  }

  @Test
  void bumpUserAuthzForcesRebuild() {
    when(userService.createUserDetailsByUsername("alice"))
        .thenReturn(details("alice", "u1"), details("alice", "u1"));

    UserDetails a = authzService.getFreshUserDetails("alice");
    authzService.bumpUserAuthz("u1");
    UserDetails b = authzService.getFreshUserDetails("alice");

    assertNotSame(a, b);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
  }

  @Test
  void unrelatedBumpTakesGenFastPathWithoutRebuild() {
    when(userService.createUserDetailsByUsername("alice")).thenReturn(details("alice", "u1"));

    UserDetails a = authzService.getFreshUserDetails("alice");
    authzService.bumpUserAuthz("someone-else");
    UserDetails b = authzService.getFreshUserDetails("alice");

    // Epoch moved but alice's gens did not: re-stamped copy, no rebuild.
    assertNotSame(a, b);
    assertEquals("alice", b.getUsername());
    assertEquals(1L, b.getAuthzCheckedEpoch());
    assertEquals(0L, b.getAuthzGen());
    verify(userService, times(1)).createUserDetailsByUsername("alice");

    // And the re-stamped entry serves the epoch fast path afterwards.
    assertSame(b, authzService.getFreshUserDetails("alice"));
  }

  @Test
  void stampBeforeBuildInvariant_entryUsesPreReadEpoch() {
    AtomicInteger calls = new AtomicInteger();
    when(userService.createUserDetailsByUsername("alice"))
        .thenAnswer(
            invocation -> {
              int n = calls.incrementAndGet();
              // Simulate a concurrent authz change observed during snapshot build.
              versionStore.bumpUserGen("u1");
              return details("alice-" + n, "u1");
            });

    UserDetails first = authzService.getFreshUserDetails("alice");
    assertNotNull(first);
    // The entry is stamped with the pre-build epoch and an "unknown" gen (the epoch moved during
    // the build), so the bump during build must force a rebuild on the next check.
    assertEquals(0L, first.getAuthzGen());
    UserDetails second = authzService.getFreshUserDetails("alice");
    assertNotNull(second);
    assertNotSame(first, second);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
  }

  @Test
  void unknownUserReturnsNullAndDoesNotCacheNull() {
    when(userService.createUserDetailsByUsername("ghost")).thenReturn(null);

    assertNull(authzService.getFreshUserDetails("ghost"));
    assertNull(authzService.getFreshUserDetails("ghost"));
    verify(userService, times(2)).createUserDetailsByUsername("ghost");
  }

  @Test
  void refreshIfStaleWithCurrentStampReturnsSameInstance() {
    UserDetails principal = details("alice", "u1");

    assertSame(principal, authzService.refreshIfStale(principal));
    verify(userService, never()).createUserDetailsByUsername(any());
  }

  @Test
  void refreshIfStaleEpochMovedGenUnchangedReturnsRestampedCopy() {
    versionStore.bumpUserGen("u1"); // gen 1, epoch 1
    versionStore.bumpRoleGen("r1"); // gen 1, epoch 2
    versionStore.bumpRoleGen("r2"); // gen 1, epoch 3
    versionStore.bumpRoleGen("r2"); // gen 2, epoch 4

    // Stamp matches the CURRENT effective gen = max(u1, r1, r2) = 2, but an old epoch.
    UserDetails principal =
        UserDetails.empty()
            .username("alice")
            .uid("u1")
            .userRoleIds(Set.of("r1", "r2"))
            .authzCheckedEpoch(3L)
            .authzGen(2L)
            .build();

    UserDetails result = authzService.refreshIfStale(principal);

    assertNotSame(principal, result);
    assertEquals("alice", result.getUsername());
    assertEquals(4L, result.getAuthzCheckedEpoch());
    assertEquals(2L, result.getAuthzGen());
    verify(userService, never()).createUserDetailsByUsername(any());
  }

  @Test
  void refreshIfStaleGenMovedRebuilds() {
    UserDetails fresh = details("alice", "u1");
    when(userService.createUserDetailsByUsername("alice")).thenReturn(fresh);
    versionStore.bumpUserGen("u1"); // gen 1, epoch 1

    UserDetails principal = details("alice", "u1"); // stamps (0, 0)
    UserDetails result = authzService.refreshIfStale(principal);

    assertNotSame(principal, result);
    assertEquals(1L, result.getAuthzCheckedEpoch());
    assertEquals(1L, result.getAuthzGen());
    verify(userService, times(1)).createUserDetailsByUsername("alice");
  }

  @Test
  void refreshIfStaleUnknownUserReturnsArgument() {
    when(userService.createUserDetailsByUsername("alice")).thenReturn(null);
    versionStore.bumpUserGen("u1");

    UserDetails principal = details("alice", "u1");

    assertSame(principal, authzService.refreshIfStale(principal));
  }

  private static UserDetails details(String username, String uid) {
    return details(username, uid, Set.of());
  }

  private static UserDetails details(String username, String uid, Set<String> roleIds) {
    return UserDetails.empty().username(username).uid(uid).userRoleIds(roleIds).build();
  }
}
