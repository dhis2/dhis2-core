/*
 * Copyright (c) 2004-2022, University of Oslo
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

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.CacheManager;
import org.ehcache.core.statistics.CacheStatistics;
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
 * Hibernate second-level cache.
 */
@Slf4j
@Configuration
@Conditional(EhCacheMetricsConfig.EhCacheMetricsEnabledCondition.class)
public class EhCacheMetricsConfig {

  @Autowired
  public void bindEhCacheToRegistry(
      EntityManagerFactory entityManagerFactory, MeterRegistry registry) {
    try {
      // Directly unwrap to the needed type, handling potential proxies
      SessionFactoryImplementor sessionFactoryImplementor =
          entityManagerFactory.unwrap(SessionFactoryImplementor.class);

      if (sessionFactoryImplementor == null) {
        log.warn(
            "Could not unwrap EntityManagerFactory to SessionFactoryImplementor. Cache metrics disabled.");
        return; // Exit if unwrapping fails
      }

      // Try to access the CacheManager
      ServiceRegistry serviceRegistry = sessionFactoryImplementor.getServiceRegistry();
      RegionFactory regionFactory = serviceRegistry.getService(RegionFactory.class);

      if (regionFactory == null) {
        log.warn("No RegionFactory found for second-level cache monitoring");
        return;
      }

      // Get the JSR-107 CacheManager wrapper (likely Eh107CacheManager)
      javax.cache.CacheManager jsr107CacheManager = getEhCacheManager(regionFactory);
      if (jsr107CacheManager == null) {
        log.warn("Failed to retrieve JSR-107 CacheManager via reflection.");
        return;
      }

      try {
        // Unwrap to get the native Ehcache CacheManager to retrieve cache names
        CacheManager ehCacheManager = jsr107CacheManager.unwrap(CacheManager.class);
        if (ehCacheManager != null) {
          log.info("Successfully unwrapped to native Ehcache CacheManager.");
          // Pass both managers to register metrics
          registerCacheMetrics(jsr107CacheManager, ehCacheManager, registry);
        } else {
          log.warn(
              "Unwrapping JSR-107 CacheManager returned null for native Ehcache CacheManager.");
        }
      } catch (IllegalArgumentException e) {
        log.warn(
            "Failed to unwrap JSR-107 CacheManager ({}) to native Ehcache CacheManager: {}",
            jsr107CacheManager.getClass().getName(),
            e.getMessage());
      }
    } catch (PersistenceException | ClassCastException ex) {
      log.warn("Failed to bind EHCache metrics: {}", ex.getMessage());
    }
  }

  /**
   * Attempts to extract the EHCache CacheManager from the Hibernate RegionFactory. This uses
   * reflection. Returns the JSR-107 CacheManager wrapper.
   */
  private javax.cache.CacheManager getEhCacheManager(RegionFactory regionFactory) {
    log.info(
        "[Metrics] Attempting reflection on RegionFactory: {}", regionFactory.getClass().getName());
    try {
      // The field name might vary, but "cacheManager" is common for JCacheRegionFactory
      java.lang.reflect.Field field = regionFactory.getClass().getDeclaredField("cacheManager");
      field.setAccessible(true);
      Object cacheManagerObj = field.get(regionFactory);
      log.info(
          "[Metrics] Object returned by reflection ('cacheManager' field): {}",
          cacheManagerObj != null ? cacheManagerObj.getClass().getName() : "null");

      if (cacheManagerObj instanceof javax.cache.CacheManager) {
        return (javax.cache.CacheManager) cacheManagerObj;
      } else {
        log.warn(
            "[Metrics] Object retrieved via reflection is not a javax.cache.CacheManager. Type:"
                + " {}",
            cacheManagerObj != null ? cacheManagerObj.getClass().getName() : "null");
        return null;
      }
    } catch (NoSuchFieldException nsfe) {
      log.error(
          "[Metrics] Reflection failed: Field 'cacheManager' not found in {}",
          regionFactory.getClass().getName(),
          nsfe);
      return null;
    } catch (Exception e) {
      log.error(
          "[Metrics] Could not access CacheManager via reflection on {}: {}",
          regionFactory.getClass().getName(),
          e.getMessage(),
          e);
      return null;
    }
  }

  /**
   * Registers metrics for each cache using reflection to access statistics directly from the
   * JSR-107 cache wrapper.
   */
  private void registerCacheMetrics(
      javax.cache.CacheManager jsr107CacheManager,
      CacheManager ehCacheManager, // Native manager used to get cache names
      MeterRegistry registry) {

    Set<String> cacheNames =
        ehCacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet();
    log.info("[Metrics] Found {} EHCache regions for monitoring", cacheNames.size());

    for (String cacheName : cacheNames) {
      try {
        javax.cache.Cache<?, ?> jsr107Cache = jsr107CacheManager.getCache(cacheName);
        if (jsr107Cache != null) {

          // Use reflection to get cacheStatistics via statisticsBean
          CacheStatistics cacheStats = getCacheStatisticsViaReflection(jsr107Cache);

          if (cacheStats != null) {
            // Register metrics using the directly obtained CacheStatistics
            new EhCacheDirectStatisticsMetrics(cacheStats, null, cacheName).bindTo(registry);
            log.debug("[Metrics] Registered metrics for cache: {}", cacheName);
          } else {
            log.warn(
                "[Metrics] Could not retrieve CacheStatistics for cache '{}' via reflection.",
                cacheName);
            // Optionally, register fallback metrics if needed, but for now, we just warn
          }
        } else {
          log.warn("[Metrics] JSR-107 CacheManager returned null for cache name: {}", cacheName);
        }
      } catch (Exception e) {
        log.warn(
            "[Metrics] Failed to register metrics for cache {}: {}", cacheName, e.getMessage(), e);
      }
    }
  }

  /**
   * Uses reflection to extract the CacheStatistics object from a JSR-107 Cache instance, assuming
   * it's an Ehcache implementation with a statisticsBean field.
   */
  private CacheStatistics getCacheStatisticsViaReflection(javax.cache.Cache<?, ?> jsr107Cache) {
    try {
      // Get the statisticsBean field from the JSR-107 Cache object
      java.lang.reflect.Field statsBeanField =
          jsr107Cache.getClass().getDeclaredField("statisticsBean");
      statsBeanField.setAccessible(true);
      Object statsBean = statsBeanField.get(jsr107Cache);

      if (statsBean == null) {
        log.warn(
            "[Metrics] Reflection returned null for 'statisticsBean' field in cache: {}",
            jsr107Cache.getName());
        return null;
      }

      // Get the cacheStatistics field from the statisticsBean object
      java.lang.reflect.Field cacheStatsField =
          statsBean.getClass().getDeclaredField("cacheStatistics");
      cacheStatsField.setAccessible(true);
      Object cacheStatsObj = cacheStatsField.get(statsBean);

      if (cacheStatsObj instanceof CacheStatistics) {
        return (CacheStatistics) cacheStatsObj;
      } else {
        log.warn(
            "[Metrics] Field 'cacheStatistics' in statisticsBean is not of expected type"
                + " CacheStatistics for cache: {}. Type: {}",
            jsr107Cache.getName(),
            cacheStatsObj != null ? cacheStatsObj.getClass().getName() : "null");
        return null;
      }
    } catch (NoSuchFieldException nsfe) {
      log.warn(
          "[Metrics] Reflection failed to find 'statisticsBean' or 'cacheStatistics' field for"
              + " cache {}: {}",
          jsr107Cache.getName(),
          nsfe.getMessage());
      return null;
    } catch (Exception e) {
      log.warn(
          "[Metrics] Error accessing statistics via reflection for cache {}: {}",
          jsr107Cache.getName(),
          e.getMessage(),
          e);
      return null;
    }
  }

  /** Binds metrics using a CacheStatistics object obtained via reflection. */
  private static class EhCacheDirectStatisticsMetrics implements MeterBinder {
    private final CacheStatistics stats;
    private final Iterable<Tag> tags;
    private final String name;

    public EhCacheDirectStatisticsMetrics(CacheStatistics stats, Iterable<Tag> tags, String name) {
      this.stats = stats;
      this.tags = tags;
      this.name = name;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      Tag l2 = Tag.of("type", "L2");
      String[] parts = name.split("\\.");
      StringBuilder lastPartBuilder = new StringBuilder();
      boolean foundCapitalized = false;
      for (String part : parts) {
        if (foundCapitalized) {
          lastPartBuilder.append(".").append(part);
        } else if (Character.isUpperCase(part.charAt(0))) {
          lastPartBuilder.append(part);
          foundCapitalized = true;
        }
      }
      String lastPart = foundCapitalized ? lastPartBuilder.toString() : name;

      log.info("[Metrics] Last part: {}", lastPart);

      FunctionCounter.builder("ehcache." + lastPart, stats, CacheStatistics::getCacheGets)
          .tags(Tags.of(Tag.of("name", "cache.gets"), l2))
          .description("The number of get requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache." + lastPart, stats, CacheStatistics::getCachePuts)
          .tags(Tags.of(Tag.of("name", "cache.puts"), l2))
          .description("The number of put requests that were made to the cache")
          .register(registry);
      FunctionCounter.builder("ehcache." + lastPart, stats, CacheStatistics::getCacheRemovals)
          .tags(Tags.of(Tag.of("name", "cache.removals"), l2))
          .description("The number of removal requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache." + lastPart, stats, CacheStatistics::getCacheEvictions)
          .tags(Tags.of(Tag.of("name", "cache.evictions"), l2))
          .description("The number of evictions from the cache")
          .register(registry);

      FunctionCounter.builder("ehcache." + lastPart, stats, CacheStatistics::getCacheHits)
          .tags(Tags.of(Tag.of("name", "cache.hits"), l2))
          .description(
              "The number of times cache lookup methods found a requested entry in the cache")
          .register(registry);

      FunctionCounter.builder("ehcache." + lastPart, stats, CacheStatistics::getCacheMisses)
          .tags(Tags.of(Tag.of("name", "cache.misses"), l2))
          .description(
              "The number of times cache lookup methods did not find a requested entry in the cache")
          .register(registry);

      Gauge.builder(
              "cache.hit.ratio",
              stats,
              s -> {
                long gets = s.getCacheGets();
                return gets == 0 ? 0 : (double) s.getCacheHits() / gets;
              })
          .tags(tags)
          .description("The ratio of cache requests which were hits")
          .register(registry);
    }
  }

  // Removed EhCache3Metrics and EhCache3FallbackMetrics as they are no longer used

  static class EhCacheMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_EHCACHE_ENABLED;
    }
  }
}
