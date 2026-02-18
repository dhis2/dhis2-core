/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.cacheinvalidation.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * Redis-based implementation of {@link ETagVersionService} that stores ETag versions in Redis for
 * fast lookups. This implementation uses Redis INCR for atomic version increments and GET for fast
 * reads.
 *
 * <p>Keys are stored with the prefix "etag:version:" followed by user UID or "global" for the
 * global version.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
@Conditional(value = CacheInvalidationEnabledCondition.class)
public class ETagVersionServiceImpl implements ETagVersionService {

  private static final String ETAG_VERSION_PREFIX = "etag:version:";
  private static final String GLOBAL_VERSION_KEY = ETAG_VERSION_PREFIX + "global";

  @Autowired
  @Qualifier("redisConnection")
  private StatefulRedisConnection<String, String> redisConnection;

  @Autowired private DhisConfigurationProvider configurationProvider;

  @Override
  public long getVersion(@Nonnull String userUid) {
    if (!isEnabled()) {
      return 0L;
    }

    try {
      RedisCommands<String, String> commands = redisConnection.sync();
      String value = commands.get(ETAG_VERSION_PREFIX + userUid);
      return value != null ? Long.parseLong(value) : 0L;
    } catch (Exception e) {
      log.warn("Failed to get ETag version for user {}: {}", userUid, e.getMessage());
      return 0L;
    }
  }

  @Override
  public long incrementVersion(@Nonnull String userUid) {
    if (!isEnabled()) {
      return 0L;
    }

    try {
      RedisCommands<String, String> commands = redisConnection.sync();
      Long newVersion = commands.incr(ETAG_VERSION_PREFIX + userUid);
      log.debug("Incremented ETag version for user {} to {}", userUid, newVersion);
      return newVersion;
    } catch (Exception e) {
      log.warn("Failed to increment ETag version for user {}: {}", userUid, e.getMessage());
      return 0L;
    }
  }

  @Override
  public long incrementGlobalVersion() {
    if (!isEnabled()) {
      return 0L;
    }

    try {
      RedisCommands<String, String> commands = redisConnection.sync();
      Long newVersion = commands.incr(GLOBAL_VERSION_KEY);
      log.debug("Incremented global ETag version to {}", newVersion);
      return newVersion;
    } catch (Exception e) {
      log.warn("Failed to increment global ETag version: {}", e.getMessage());
      return 0L;
    }
  }

  @Override
  public long getGlobalVersion() {
    if (!isEnabled()) {
      return 0L;
    }

    try {
      RedisCommands<String, String> commands = redisConnection.sync();
      String value = commands.get(GLOBAL_VERSION_KEY);
      return value != null ? Long.parseLong(value) : 0L;
    } catch (Exception e) {
      log.warn("Failed to get global ETag version: {}", e.getMessage());
      return 0L;
    }
  }

  @Override
  public long getEntityTypeVersion(@Nonnull Class<?> entityType) {
    if (!isEnabled()) {
      return 0L;
    }

    try {
      RedisCommands<String, String> commands = redisConnection.sync();
      String key = ETAG_VERSION_PREFIX + entityType.getSimpleName();
      String value = commands.get(key);
      return value != null ? Long.parseLong(value) : 0L;
    } catch (Exception e) {
      log.warn(
          "Failed to get ETag version for entity type {}: {}",
          entityType.getSimpleName(),
          e.getMessage());
      return 0L;
    }
  }

  @Override
  public long incrementEntityTypeVersion(@Nonnull Class<?> entityType) {
    if (!isEnabled()) {
      return 0L;
    }

    try {
      RedisCommands<String, String> commands = redisConnection.sync();
      String key = ETAG_VERSION_PREFIX + entityType.getSimpleName();
      Long newVersion = commands.incr(key);
      log.debug(
          "Incremented ETag version for entity type {} to {}",
          entityType.getSimpleName(),
          newVersion);
      return newVersion;
    } catch (Exception e) {
      log.warn(
          "Failed to increment ETag version for entity type {}: {}",
          entityType.getSimpleName(),
          e.getMessage());
      return 0L;
    }
  }

  @Override
  public boolean isEnabled() {
    return configurationProvider.isEnabled(ConfigurationKey.ETAG_CACHE_ENABLED);
  }

  @Override
  public int getTtlMinutes() {
    return Integer.parseInt(
        configurationProvider.getPropertyOrDefault(
            ConfigurationKey.ETAG_CACHE_TTL_MINUTES,
            ConfigurationKey.ETAG_CACHE_TTL_MINUTES.getDefaultValue()));
  }
}
