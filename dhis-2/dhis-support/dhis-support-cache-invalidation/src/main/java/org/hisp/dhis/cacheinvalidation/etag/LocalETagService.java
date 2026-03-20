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
package org.hisp.dhis.cacheinvalidation.etag;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * In-memory implementation of {@link ETagService} backed by a ConcurrentHashMap. Independent of
 * Redis; suitable for single-instance deployments.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
@Conditional(value = ETagCacheEnabledCondition.class)
public class LocalETagService implements ETagService, InitializingBean {

  /** Entity type versions — bounded by the number of Hibernate-mapped entity classes (~200). */
  private final ConcurrentHashMap<String, AtomicLong> entityTypeVersions =
      new ConcurrentHashMap<>();

  private final AtomicLong allCacheVersion = new AtomicLong(0);

  @Autowired private DhisConfigurationProvider configurationProvider;

  @Override
  public void afterPropertiesSet() {
    validateTtlMinutes();

    if (!configurationProvider.isEnabled(ConfigurationKey.SQL_DML_OBSERVER_ENABLED)) {
      log.warn(
          "ETag cache is enabled but SQL DML observer is disabled. "
              + "Cache invalidation will rely solely on the TTL safety window ({} min). "
              + "Enable {} for real-time invalidation.",
          getTtlMinutes(),
          ConfigurationKey.SQL_DML_OBSERVER_ENABLED.getKey());
    }
  }

  private void validateTtlMinutes() {
    String raw =
        configurationProvider.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES,
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getDefaultValue());
    int ttl;
    try {
      ttl = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Invalid ETag cache TTL value '"
              + raw
              + "' for key "
              + ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getKey()
              + ". Must be a positive integer.");
    }
    if (ttl <= 0) {
      throw new IllegalStateException(
          "ETag cache TTL must be > 0, got "
              + ttl
              + " for key "
              + ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getKey());
    }
  }

  @Override
  public long getAllCacheVersion() {
    return allCacheVersion.get();
  }

  @Override
  public long incrementAllCacheVersion() {
    return allCacheVersion.incrementAndGet();
  }

  @Override
  public long getEntityTypeVersion(@Nonnull Class<?> entityType) {
    AtomicLong version = entityTypeVersions.get(entityType.getName());
    return version != null ? version.get() : 0L;
  }

  @Override
  public long incrementEntityTypeVersion(@Nonnull Class<?> entityType) {
    String key = entityType.getName();
    long newVersion =
        entityTypeVersions.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    log.debug("Incremented entity type version for {} to {}", key, newVersion);
    return newVersion;
  }

  @Override
  public boolean isEnabled() {
    return configurationProvider.isEnabled(ConfigurationKey.CACHE_API_ETAG_ENABLED);
  }

  @Override
  public int getTtlMinutes() {
    return Integer.parseInt(
        configurationProvider.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES,
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getDefaultValue()));
  }
}
