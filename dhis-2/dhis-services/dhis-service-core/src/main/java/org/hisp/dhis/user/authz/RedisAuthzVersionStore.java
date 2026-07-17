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

import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Shared dual-generation store for multi-node Redis session deployments.
 *
 * @author Morten Svanæs
 */
@RequiredArgsConstructor
public class RedisAuthzVersionStore implements AuthzVersionStore {

  static final String USER_KEY_PREFIX = "dhis2:authz:user:";
  static final String ROLE_KEY_PREFIX = "dhis2:authz:role:";

  private final StringRedisTemplate redisTemplate;

  @Override
  public long getUserGen(@Nonnull String username) {
    return getGen(USER_KEY_PREFIX + username);
  }

  @Override
  public long getRoleGen(@Nonnull String roleUid) {
    return getGen(ROLE_KEY_PREFIX + roleUid);
  }

  @Override
  public long bumpUserGen(@Nonnull String username) {
    Long value = redisTemplate.opsForValue().increment(USER_KEY_PREFIX + username);
    return value == null ? 0L : value;
  }

  @Override
  public long bumpRoleGen(@Nonnull String roleUid) {
    Long value = redisTemplate.opsForValue().increment(ROLE_KEY_PREFIX + roleUid);
    return value == null ? 0L : value;
  }

  @Override
  public void bumpUserGens(@Nonnull Collection<String> usernames) {
    for (String username : usernames) {
      if (username != null && !username.isBlank()) {
        bumpUserGen(username);
      }
    }
  }

  private long getGen(String key) {
    String value = redisTemplate.opsForValue().get(key);
    if (value == null || value.isBlank()) {
      return 0L;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return 0L;
    }
  }
}
