/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_EHCACHE_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Consolidated configuration for wiring up Ehcache3 metrics. This class both configures and binds
 * the Ehcache metrics to the MeterRegistry.
 */
@Configuration
@Conditional(Ehcache3MetricsConfig.EhcacheMetricsEnabledCondition.class)
@Slf4j
public class Ehcache3MetricsConfig {

  @Bean
  public EhcacheMeterBinder ehcacheMeterBinder() {
    log.info("Creating EhcacheMeterBinder bean");
    EhcacheCachingProvider provider =
        (EhcacheCachingProvider)
            Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
    CacheManager cacheManager = provider.getCacheManager();
    return new EhcacheMeterBinder(cacheManager);
  }

  @Autowired
  public void bindToRegistry(MeterRegistry registry, EhcacheMeterBinder meterBinder) {
    log.info("Explicitly binding Ehcache metrics to registry");
    meterBinder.bindTo(registry);
  }

  // Inner class that provides the metrics binding logic
  public static class EhcacheMeterBinder implements MeterBinder {
    private final CacheManager cacheManager;

    public EhcacheMeterBinder(CacheManager cacheManager) {
      this.cacheManager = cacheManager;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      log.info("Binding Ehcache metrics to registry");

      if (this.cacheManager == null) {
        log.warn("JSR-107 CacheManager bean not found. Cannot bind JCache metrics.");
        return;
      }

      log.info(
          "Attempting to bind metrics for JCache caches managed by: {}",
          this.cacheManager.getClass().getName());

      int boundCount = 0;
      for (String cacheName : this.cacheManager.getCacheNames()) {
        Cache<?, ?> cache = cacheManager.getCache(cacheName, Object.class, Object.class);
        if (cache != null) {
          Iterable<Tag> tags = Tags.of("cache", cacheName);
          new JCacheMetrics<>(cache, tags).bindTo(registry);
          boundCount++;
        }
      }

      if (boundCount > 0) {
        log.info("Successfully bound metrics for {} JCache caches.", boundCount);
      } else {
        log.warn(
            "No JCache caches found or bound via CacheManager: {}",
            this.cacheManager.getClass().getName());
      }
    }
  }

  static class EhcacheMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_EHCACHE_ENABLED;
    }
  }
}
