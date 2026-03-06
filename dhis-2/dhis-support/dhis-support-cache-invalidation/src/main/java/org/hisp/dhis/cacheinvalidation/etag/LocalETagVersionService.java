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
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * Local in-memory implementation of {@link ETagVersionService} that stores ETag versions in a
 * ConcurrentHashMap for fast, thread-safe lookups. This implementation is independent of Redis and
 * suitable for single-instance deployments or when used with a DML observer layer for cache
 * invalidation.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
@Conditional(value = ETagCacheEnabledCondition.class)
public class LocalETagVersionService implements ETagVersionService {

  private final ConcurrentHashMap<String, AtomicLong> versions = new ConcurrentHashMap<>();

  private final AtomicLong globalVersion = new AtomicLong(0);

  @Autowired private DhisConfigurationProvider configurationProvider;

  @Override
  public long getVersion(@Nonnull String userUid) {
    AtomicLong version = versions.get(userUid);
    return version != null ? version.get() : 0L;
  }

  @Override
  public long incrementVersion(@Nonnull String userUid) {
    return versions.computeIfAbsent(userUid, k -> new AtomicLong(0)).incrementAndGet();
  }

  @Override
  public long getGlobalVersion() {
    return globalVersion.get();
  }

  @Override
  public long incrementGlobalVersion() {
    return globalVersion.incrementAndGet();
  }

  @Override
  public long getEntityTypeVersion(@Nonnull Class<?> entityType) {
    AtomicLong version = versions.get(entityType.getSimpleName());
    return version != null ? version.get() : 0L;
  }

  @Override
  public long incrementEntityTypeVersion(@Nonnull Class<?> entityType) {
    // incr anything for now.
    incrementGlobalVersion();

    String key = entityType.getSimpleName();
    long newVersion = versions.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    log.debug("Incremented entity type version for {} to {}", key, newVersion);
    return newVersion;
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
