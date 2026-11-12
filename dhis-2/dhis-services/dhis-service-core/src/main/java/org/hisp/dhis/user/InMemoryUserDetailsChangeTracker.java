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
package org.hisp.dhis.user;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.condition.RedisDisabledCondition;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Map-backed {@link UserDetailsChangeTracker} for the in-memory (non-Redis) session stack, where
 * the sessions live on this node only. A change is remembered for the session timeout, or without
 * expiry when the session timeout is not positive.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@Conditional(RedisDisabledCondition.class)
public class InMemoryUserDetailsChangeTracker implements UserDetailsChangeTracker {

  private final Map<String, Change> changes = new ConcurrentHashMap<>();

  /** Time to live of a change, zero for no expiry. */
  private final Duration timeToLive;

  private final LongSupplier clock;

  @Autowired
  public InMemoryUserDetailsChangeTracker(DhisConfigurationProvider config) {
    this(UserDetailsRefreshSettings.changeTokenTimeToLive(config), System::currentTimeMillis);
  }

  InMemoryUserDetailsChangeTracker(Duration timeToLive, LongSupplier clock) {
    this.timeToLive = timeToLive;
    this.clock = clock;
  }

  @Override
  public void markChanged(@Nonnull Collection<String> usernames) {
    long now = clock.getAsLong();
    changes.values().removeIf(change -> change.isExpired(now));
    long expiresAt = timeToLive.isZero() ? Long.MAX_VALUE : now + timeToLive.toMillis();
    Change change = new Change(UUID.randomUUID().toString(), expiresAt);
    usernames.forEach(username -> changes.put(username, change));
  }

  @Override
  @CheckForNull
  public String getChangeToken(@Nonnull String username) {
    Change change = changes.get(username);
    if (change == null || change.isExpired(clock.getAsLong())) {
      return null;
    }
    return change.token();
  }

  @Override
  @Nonnull
  public Duration getChangeTokenTimeToLive() {
    return timeToLive;
  }

  private record Change(String token, long expiresAtMillis) {
    boolean isExpired(long now) {
      return now >= expiresAtMillis;
    }
  }
}
