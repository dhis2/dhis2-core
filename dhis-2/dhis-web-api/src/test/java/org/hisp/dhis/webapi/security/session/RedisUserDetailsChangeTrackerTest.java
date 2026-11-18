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
package org.hisp.dhis.webapi.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit test of {@link RedisUserDetailsChangeTracker}, with Redis replaced by a mock as in the other
 * Redis tests of the repository.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class RedisUserDetailsChangeTrackerTest {

  private StringRedisTemplate redisTemplate;

  private ValueOperations<String, String> values;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(values);
  }

  @Test
  void aChangeStoresOneNewTokenPerUserThatExpiresWithTheSessions() {
    Duration timeToLive = Duration.ofHours(1);
    RedisUserDetailsChangeTracker tracker =
        new RedisUserDetailsChangeTracker(redisTemplate, timeToLive);

    tracker.markChanged(List.of("alice", "bob"));
    tracker.markChanged(List.of("alice"));

    ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
    verify(values, times(2))
        .set(eq("dhis2:userDetailsChange:alice"), tokens.capture(), eq(timeToLive));
    verify(values)
        .set(eq("dhis2:userDetailsChange:bob"), eq(tokens.getAllValues().get(0)), eq(timeToLive));
    assertEquals(2, Set.copyOf(tokens.getAllValues()).size());
  }

  /** Redis refuses an expiry that is not positive, which made every change fail. */
  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void aChangeIsStoredWithoutExpiryWhenTheSessionTimeoutIsNotPositive(long seconds) {
    RedisUserDetailsChangeTracker tracker =
        new RedisUserDetailsChangeTracker(redisTemplate, Duration.ofSeconds(seconds));

    tracker.markChanged(List.of("alice"));

    verify(values).set(eq("dhis2:userDetailsChange:alice"), anyString());
    verify(values, never()).set(anyString(), anyString(), any(Duration.class));
  }
}
