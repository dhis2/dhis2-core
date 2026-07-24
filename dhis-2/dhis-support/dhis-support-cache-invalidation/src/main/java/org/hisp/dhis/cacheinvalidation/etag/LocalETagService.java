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
package org.hisp.dhis.cacheinvalidation.etag;

import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_ENTITY_VERSIONS_SIZE;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_NAMED_VERSIONS_SIZE;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.external.conf.ApiCacheEnabledCondition;
import org.hisp.dhis.external.conf.ApiETagCacheActivation;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * In-memory implementation of {@link ETagService} backed by ConcurrentHashMaps. Process-local only;
 * not registered when multi-node force-off applies (DHIS2 clustering or Redis cache invalidation;
 * see {@link ApiETagCacheActivation}).
 *
 * <p>Memory is bounded by design: one {@link AtomicLong} per observed entity type name and per
 * registered named key (apps, static content, …), not per row or per request.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
@Conditional(value = ApiCacheEnabledCondition.class)
public class LocalETagService implements ETagService, InitializingBean {

  /** Entity type versions — keys are FQCNs; bounded by mapped/observed entity types. */
  private final ConcurrentHashMap<String, AtomicLong> entityTypeVersions =
      new ConcurrentHashMap<>();

  /** Named versions — keys like {@code installedApps}; small fixed set of named endpoints. */
  private final ConcurrentHashMap<String, AtomicLong> namedVersions = new ConcurrentHashMap<>();

  private final AtomicLong allCacheVersion = new AtomicLong(0);

  @Autowired private DhisConfigurationProvider configurationProvider;

  @Autowired(required = false)
  private MeterRegistry meterRegistry;

  @Override
  public void afterPropertiesSet() {
    validateTtlMinutes();
    registerMemoryGauges();
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

  private void registerMemoryGauges() {
    if (meterRegistry == null
        || !configurationProvider.isEnabled(ConfigurationKey.MONITORING_CACHE_ETAG_ENABLED)) {
      return;
    }
    // Idempotent: Spring should call afterPropertiesSet once, but re-entry must not throw.
    if (meterRegistry.find(ETAG_ENTITY_VERSIONS_SIZE).gauge() == null) {
      Gauge.builder(ETAG_ENTITY_VERSIONS_SIZE, entityTypeVersions, ConcurrentHashMap::size)
          .description("Number of entity-type version keys in LocalETagService")
          .register(meterRegistry);
    }
    if (meterRegistry.find(ETAG_NAMED_VERSIONS_SIZE).gauge() == null) {
      Gauge.builder(ETAG_NAMED_VERSIONS_SIZE, namedVersions, ConcurrentHashMap::size)
          .description("Number of named version keys in LocalETagService")
          .register(meterRegistry);
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
  public long getNamedVersion(@Nonnull String key) {
    AtomicLong version = namedVersions.get(key);
    return version != null ? version.get() : 0L;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The named-version map is bounded only by convention: production call sites must use a small
   * fixed set of compile-time string constants ({@code "installedApps"}, {@code "staticContent"}).
   * This method does not validate keys at runtime; a new call site is a conscious change and should
   * update the pinned key-set test in {@code LocalETagServiceCardinalityTest}.
   */
  @Override
  public long incrementNamedVersion(@Nonnull String key) {
    long newVersion = namedVersions.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    log.debug("Incremented named version for {} to {}", key, newVersion);
    return newVersion;
  }

  @Override
  public boolean isEnabled() {
    return ApiETagCacheActivation.isEffectivelyEnabled(configurationProvider);
  }

  @Override
  public int getTtlMinutes() {
    return Integer.parseInt(
        configurationProvider.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES,
            ConfigurationKey.CACHE_API_ETAG_TTL_MINUTES.getDefaultValue()));
  }

  @Override
  public int getStaleWhileRevalidateSeconds() {
    return Integer.parseInt(
        configurationProvider.getPropertyOrDefault(
            ConfigurationKey.CACHE_API_ETAG_STALE_SECONDS,
            ConfigurationKey.CACHE_API_ETAG_STALE_SECONDS.getDefaultValue()));
  }

  /** Package-private for unit tests / cardinality checks. */
  int entityTypeVersionMapSize() {
    return entityTypeVersions.size();
  }

  /** Package-private for unit tests / cardinality checks. */
  int namedVersionMapSize() {
    return namedVersions.size();
  }

  /** Package-private for unit tests. */
  Set<String> entityTypeVersionKeys() {
    return Set.copyOf(entityTypeVersions.keySet());
  }

  /** Package-private for unit tests. */
  Set<String> namedVersionKeys() {
    return Set.copyOf(namedVersions.keySet());
  }
}
