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

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.condition.RedisDisabledCondition;
import org.hisp.dhis.condition.RedisEnabledCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires {@link AuthzVersionStore}: Redis when Redis is enabled (required for multi-node session
 * deployments), JDBC otherwise.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Configuration
public class AuthzVersionStoreConfig {

  @Bean
  @Conditional(RedisEnabledCondition.class)
  public AuthzVersionStore redisAuthzVersionStore(
      @Autowired(required = false) @Qualifier("stringRedisTemplate")
          StringRedisTemplate stringRedisTemplate,
      DhisConfigurationProvider config) {
    if (stringRedisTemplate == null) {
      // Fail closed: Redis sessions without a usable template must not silently go local-only.
      throw new IllegalStateException(
          "redis.enabled=true but stringRedisTemplate is unavailable; cannot start AuthzVersionStore");
    }
    if (!config.isEnabled(ConfigurationKey.REDIS_ENABLED)) {
      throw new IllegalStateException("Redis AuthzVersionStore requires redis.enabled=true");
    }
    log.info("Using Redis AuthzVersionStore for UserDetails soft-refresh");
    return new RedisAuthzVersionStore(stringRedisTemplate);
  }

  @Bean
  @Conditional(RedisDisabledCondition.class)
  public AuthzVersionStore jdbcAuthzVersionStore(JdbcTemplate jdbcTemplate) {
    log.info("Using JDBC AuthzVersionStore for UserDetails soft-refresh (Redis disabled)");
    return new JdbcAuthzVersionStore(jdbcTemplate);
  }
}
