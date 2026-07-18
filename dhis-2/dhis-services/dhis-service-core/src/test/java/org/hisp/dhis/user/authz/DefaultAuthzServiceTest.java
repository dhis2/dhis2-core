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
import static org.mockito.Mockito.mock;
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
    UserDetails first = details("alice", "u1");
    when(userService.createUserDetailsByUsername("alice")).thenReturn(first);

    UserDetails a = authzService.getFreshUserDetails("alice");
    UserDetails b = authzService.getFreshUserDetails("alice");

    assertSame(first, a);
    assertSame(a, b);
    verify(userService, times(1)).createUserDetailsByUsername("alice");
  }

  @Test
  void bumpRoleAuthzForcesRebuild() {
    UserDetails first = details("alice", "u1");
    UserDetails second = details("alice", "u1");
    when(userService.createUserDetailsByUsername("alice")).thenReturn(first, second);

    UserDetails a = authzService.getFreshUserDetails("alice");
    authzService.bumpRoleAuthz("r1");
    UserDetails b = authzService.getFreshUserDetails("alice");

    assertSame(first, a);
    assertSame(second, b);
    assertNotSame(a, b);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
  }

  @Test
  void bumpUserAuthzForcesRebuild() {
    UserDetails first = details("alice", "u1");
    UserDetails second = details("alice", "u1");
    when(userService.createUserDetailsByUsername("alice")).thenReturn(first, second);

    UserDetails a = authzService.getFreshUserDetails("alice");
    authzService.bumpUserAuthz("u1");
    UserDetails b = authzService.getFreshUserDetails("alice");

    assertSame(first, a);
    assertSame(second, b);
    assertNotSame(a, b);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
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
    // Entry was stamped with the pre-build epoch, so the bump during build must force a rebuild.
    UserDetails second = authzService.getFreshUserDetails("alice");
    assertNotNull(second);
    assertNotSame(first, second);
    verify(userService, times(2)).createUserDetailsByUsername("alice");
  }

  @Test
  void effectiveGenIsMaxOfUserAndRoleGens() {
    versionStore.bumpUserGen("u1"); // 1
    versionStore.bumpRoleGen("r1"); // 1
    versionStore.bumpRoleGen("r2"); // 1
    versionStore.bumpRoleGen("r2"); // 2

    UserDetails principal =
        UserDetails.empty().username("alice").uid("u1").userRoleIds(Set.of("r1", "r2")).build();

    assertEquals(2L, authzService.effectiveGen(principal));
  }

  @Test
  void unknownUserReturnsNullAndDoesNotCacheNull() {
    when(userService.createUserDetailsByUsername("ghost")).thenReturn(null);

    assertNull(authzService.getFreshUserDetails("ghost"));
    assertNull(authzService.getFreshUserDetails("ghost"));
    verify(userService, times(2)).createUserDetailsByUsername("ghost");
  }

  @Test
  void currentEpochDelegates() {
    assertEquals(0L, authzService.currentEpoch());
    authzService.bumpUserAuthz("u1");
    assertEquals(1L, authzService.currentEpoch());
  }

  private static UserDetails details(String username, String uid) {
    return UserDetails.empty().username(username).uid(uid).userRoleIds(Set.of()).build();
  }
}
