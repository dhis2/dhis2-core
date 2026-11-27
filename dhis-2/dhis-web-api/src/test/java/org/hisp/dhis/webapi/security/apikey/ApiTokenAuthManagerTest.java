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
package org.hisp.dhis.webapi.security.apikey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.test.cache.TestCache;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.Authentication;

/**
 * Unit test of the cache of {@link ApiTokenAuthManager}: a cached token must never outlive a change
 * of its owner, also after the {@link UserDetailsChangeTracker} has forgotten the change.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiTokenAuthManagerTest {

  private static final String KEY = "key";

  private static final String OWNER = "alice";

  private static final Duration CHANGE_TOKEN_TIME_TO_LIVE = Duration.ofMinutes(15);

  private final AtomicReference<String> changeToken = new AtomicReference<>();

  private final AtomicLong now = new AtomicLong(1_000_000L);

  private ApiTokenService apiTokenService;

  private UserService userService;

  private ApiTokenAuthManager manager;

  @BeforeEach
  void setUp() {
    apiTokenService = mock(ApiTokenService.class);
    userService = mock(UserService.class);
    UserDetailsChangeTracker tracker = mock(UserDetailsChangeTracker.class);
    when(tracker.getChangeToken(OWNER)).thenAnswer(call -> changeToken.get());
    when(tracker.getChangeTokenTimeToLive()).thenReturn(CHANGE_TOKEN_TIME_TO_LIVE);
    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.createApiKeyCache()).thenReturn(new TestCache<>());
    Clock clock = mock(Clock.class);
    when(clock.millis()).thenAnswer(call -> now.get());

    User owner = new User();
    owner.setUsername(OWNER);
    ApiToken apiToken = new ApiToken();
    apiToken.setCreatedBy(owner);
    apiToken.setExpire(Long.MAX_VALUE);
    when(apiTokenService.getByKey(KEY)).thenReturn(apiToken);
    ownerIs(owner("F_OLD", true));

    manager = new ApiTokenAuthManager(apiTokenService, cacheProvider, userService, tracker, clock);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void refusedReloadStaysRefusedAfterTheChangeTokenExpired(boolean timePassed) {
    authenticate();
    changeToken.set("T1");
    ownerIs(owner("F_NONE", false));
    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);

    changeToken.set(null);
    if (timePassed) {
      now.addAndGet(CHANGE_TOKEN_TIME_TO_LIVE.toMillis());
    }

    assertThrows(ApiTokenAuthenticationException.class, this::authenticate);
  }

  @Test
  void changeNoRequestSawIsPickedUpAfterItsTokenExpired() {
    authenticate();
    changeToken.set("T1");
    ownerIs(owner("F_NEW", true));
    now.addAndGet(CHANGE_TOKEN_TIME_TO_LIVE.toMillis());
    changeToken.set(null);

    UserDetails principal = (UserDetails) authenticate().getPrincipal();

    assertTrue(principal.isAuthorized("F_NEW"));
    assertFalse(principal.isAuthorized("F_OLD"));
  }

  @Test
  void unchangedOwnerIsServedFromTheCache() {
    Authentication first = authenticate();
    now.addAndGet(CHANGE_TOKEN_TIME_TO_LIVE.toMillis() - 1);

    assertSame(first, authenticate());
    verify(apiTokenService, times(1)).getByKey(KEY);
  }

  @Test
  void changedOwnerIsReloaded() {
    authenticate();
    changeToken.set("T1");
    ownerIs(owner("F_NEW", true));

    UserDetails principal = (UserDetails) authenticate().getPrincipal();

    assertTrue(principal.isAuthorized("F_NEW"));
    verify(apiTokenService, times(2)).getByKey(KEY);
  }

  private Authentication authenticate() {
    return manager.authenticate(new ApiTokenAuthenticationToken(KEY));
  }

  private void ownerIs(UserDetails owner) {
    when(userService.createUserDetailsByUsername(OWNER)).thenReturn(owner);
  }

  private static UserDetails owner(String authority, boolean enabled) {
    return UserDetails.empty()
        .uid("aliceUid001")
        .username(OWNER)
        .enabled(enabled)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .authorities(List.of())
        .allAuthorities(Set.of(authority))
        .build();
  }
}
