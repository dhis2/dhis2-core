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

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed {@link UserDetailsChangeTracker} for the Redis session stack, so that the change
 * tokens are shared by all nodes of a cluster, like the sessions themselves. Each token is stored
 * under its own key that expires after the session timeout.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class RedisUserDetailsChangeTracker implements UserDetailsChangeTracker {

  private static final String KEY_PREFIX = "dhis2:userDetailsChange:";

  private final StringRedisTemplate redisTemplate;

  private final Duration timeToLive;

  public RedisUserDetailsChangeTracker(StringRedisTemplate redisTemplate, Duration timeToLive) {
    this.redisTemplate = redisTemplate;
    this.timeToLive = timeToLive;
  }

  @Override
  public void markChanged(@Nonnull Collection<String> usernames) {
    String token = UUID.randomUUID().toString();
    usernames.forEach(
        username -> redisTemplate.opsForValue().set(KEY_PREFIX + username, token, timeToLive));
  }

  @Override
  @CheckForNull
  public String getChangeToken(@Nonnull String username) {
    return redisTemplate.opsForValue().get(KEY_PREFIX + username);
  }
}
