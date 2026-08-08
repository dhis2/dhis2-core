/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.StatisticsGateway;
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for monitoring Hibernate's second-level EHCache implementation.
 *
 * <p>This class creates metrics for cache hits, misses, and other statistics for each region in the
 * Hibernate second-level cache. Ehcache2 variant (2.41 runs {@code net.sf.ehcache} / Hibernate's
 * {@link EhcacheRegionFactory}, not Ehcache3/JSR-107 like current master), adapted from master's
 * {@code EhCacheMetricsConfig} (PR #20398) so it targets the native Ehcache2 {@link
 * CacheManager}/{@link StatisticsGateway} API directly instead of reflecting into a JSR-107
 * wrapper.
 */
@Slf4j
@Configuration
@Conditional(EhCacheMetricsConfig.EhCacheMetricsEnabledCondition.class)
public class EhCacheMetricsConfig {

  @Autowired
  public void bindEhCacheToRegistry(
      EntityManagerFactory entityManagerFactory, MeterRegistry registry) {
    try {
      SessionFactoryImplementor sessionFactoryImplementor =
          entityManagerFactory.unwrap(SessionFactoryImplementor.class);
      ServiceRegistry serviceRegistry = sessionFactoryImplementor.getServiceRegistry();
      RegionFactory regionFactory = serviceRegistry.getService(RegionFactory.class);

      if (!(regionFactory instanceof EhcacheRegionFactory ehcacheRegionFactory)) {
        log.debug("No Ehcache RegionFactory found, assuming no second-level cache configured.");
        return;
      }

      CacheManager cacheManager = ehcacheRegionFactory.getCacheManager();
      if (cacheManager == null) {
        log.warn("EhcacheRegionFactory has no CacheManager. Cache metrics disabled.");
        return;
      }

      log.debug("Successfully retrieved native Ehcache CacheManager. Registering metrics...");
      registerCacheMetrics(cacheManager, registry);

    } catch (PersistenceException | ClassCastException | IllegalArgumentException ex) {
      log.warn(
          "Failed to bind EHCache metrics due to an unexpected error during setup: {}",
          ex.getMessage());
    } catch (Exception ex) {
      log.warn(
          "An unexpected error occurred while binding EHCache metrics: {}", ex.getMessage(), ex);
    }
  }

  private void registerCacheMetrics(CacheManager cacheManager, MeterRegistry registry) {
    String[] cacheNames = cacheManager.getCacheNames();
    if (cacheNames.length == 0) {
      log.info("No EHCache regions found for monitoring.");
      return;
    }
    log.info("Found {} EHCache regions for monitoring.", cacheNames.length);

    for (String cacheName : cacheNames) {
      try {
        Ehcache cache = cacheManager.getEhcache(cacheName);
        if (cache == null) {
          log.warn("CacheManager returned null for cache name: {}", cacheName);
          continue;
        }
        new EhCacheDirectStatisticsMetrics(cache.getStatistics(), cacheName).bindTo(registry);
        log.debug("Registered metrics for cache: {}", cacheName);
      } catch (Exception e) {
        log.warn("Failed to register metrics for cache '{}': {}", cacheName, e.getMessage(), e);
      }
    }
  }

  /** Binds metrics using a {@link StatisticsGateway} obtained directly from the native cache. */
  private static class EhCacheDirectStatisticsMetrics implements MeterBinder {
    public static final Tag L_2 = Tag.of("type", "L2");
    private final StatisticsGateway stats;
    private final String cacheName;

    public EhCacheDirectStatisticsMetrics(StatisticsGateway stats, String cacheName) {
      this.stats = stats;
      this.cacheName = cacheName;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      // Derive a shorter metric name part from the full cache name
      // Example: org.hisp.dhis.option.Option -> Option
      // Example: org.hisp.dhis.option.OptionSet.options -> OptionSet.options
      // Example: org.hibernate.cache.internal.StandardQueryCache -> StandardQueryCache
      String namePart = deriveNamePart(cacheName);

      Tags cacheTags = Tags.of(Tag.of("cache", namePart), L_2);

      // Ehcache2's StatisticsGateway has no single "total gets" counter; it's hits + misses.
      FunctionCounter.builder(
              "ehcache_gets_total", stats, s -> s.cacheHitCount() + s.cacheMissCount())
          .tags(cacheTags)
          .description("The total number of get requests made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_puts_total", stats, StatisticsGateway::cachePutCount)
          .tags(cacheTags)
          .description("The total number of put requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_removals_total", stats, StatisticsGateway::cacheRemoveCount)
          .tags(cacheTags)
          .description("The total number of removal requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder(
              "ehcache_evictions_total", stats, StatisticsGateway::cacheEvictedCount)
          .tags(cacheTags)
          .description("The total number of evictions from the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_hits_total", stats, StatisticsGateway::cacheHitCount)
          .tags(cacheTags)
          .description(
              "The total number of times cache lookup methods found a requested entry in the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_misses_total", stats, StatisticsGateway::cacheMissCount)
          .tags(cacheTags)
          .description(
              "The total number of times cache lookup methods did not find a requested entry in the cache")
          .register(registry);

      // Deliberately no ehcache_local_heap_size_bytes gauge: StatisticsGateway.
      // getLocalHeapSizeInBytes() -> MemoryStore.getInMemorySizeInBytes() only returns a cheap,
      // incrementally-maintained value when the cache has byte-based sizing (maxBytesLocalHeap)
      // configured. None of our regions do (ehcache.xml only uses maxElementsInMemory), so it
      // falls back to walking every element's object graph with Ehcache's SizeOfEngine on every
      // single call -- i.e. on every Prometheus scrape, per region. Confirmed live: "configured
      // limit of 1,000 object references was reached" warnings from ObjectGraphWalker, and this
      // is O(cache size) work repeating every scrape, not a one-off cost.
    }

    /**
     * Derives a simplified name part for the metric from the full cache name. It finds the first
     * segment starting with an uppercase letter and includes all subsequent segments.
     *
     * @param fullCacheName The full name of the cache region.
     * @return A simplified name based on the first capitalized segment, or the original name if no
     *     capitalized segment is found.
     */
    private String deriveNamePart(String fullCacheName) {
      if (fullCacheName == null || fullCacheName.isEmpty()) {
        return "unknown";
      }

      String[] parts = fullCacheName.split("\\.");
      StringBuilder metricPartBuilder = new StringBuilder();
      boolean foundCapitalized = false;

      for (String part : parts) {
        if (part.isEmpty()) {
          continue;
        }
        if (foundCapitalized) {
          metricPartBuilder.append(".").append(part);
        } else if (Character.isUpperCase(part.charAt(0))) {
          metricPartBuilder.append(part);
          foundCapitalized = true;
        }
      }
      return foundCapitalized ? metricPartBuilder.toString() : fullCacheName;
    }
  }

  static class EhCacheMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_EHCACHE_ENABLED;
    }
  }
}
