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
      SessionFactoryImplementor sessionFactoryImplementor =
          entityManagerFactory.unwrap(SessionFactoryImplementor.class);
      ServiceRegistry serviceRegistry = sessionFactoryImplementor.getServiceRegistry();
      RegionFactory regionFactory = serviceRegistry.getService(RegionFactory.class);

      if (regionFactory == null) {
        log.debug("No RegionFactory found, assuming no second-level cache configured.");
        return;
      }

      javax.cache.CacheManager jsr107CacheManager = getEhCacheManager(regionFactory);
      if (jsr107CacheManager == null) {
        log.warn("Could not retrieve JSR-107 CacheManager via reflection. Cache metrics disabled.");
        return;
      }

      CacheManager ehCacheManager = jsr107CacheManager.unwrap(CacheManager.class);
      if (ehCacheManager == null) {
        log.warn(
            "Could not unwrap JSR-107 CacheManager ({}) to native Ehcache CacheManager. Cache metrics disabled.",
            jsr107CacheManager.getClass().getName());
        return;
      }

      log.debug("Successfully retrieved native Ehcache CacheManager. Registering metrics...");
      registerCacheMetrics(jsr107CacheManager, ehCacheManager, registry);

    } catch (PersistenceException | ClassCastException | IllegalArgumentException ex) {
      log.warn(
          "Failed to bind EHCache metrics due to an unexpected error during setup: {}",
          ex.getMessage());
    } catch (Exception ex) {
      // Catch broader exceptions during reflection/unwrapping
      log.warn(
          "An unexpected error occurred while binding EHCache metrics: {}", ex.getMessage(), ex);
    }
  }

  /**
   * Attempts to extract the EHCache CacheManager from the Hibernate RegionFactory using reflection.
   * Returns the JSR-107 CacheManager wrapper.
   */
  private javax.cache.CacheManager getEhCacheManager(RegionFactory regionFactory) {
    try {
      // Common field name for JCacheRegionFactory implementations
      java.lang.reflect.Field field = regionFactory.getClass().getDeclaredField("cacheManager");
      field.setAccessible(true);
      Object cacheManagerObj = field.get(regionFactory);

      if (cacheManagerObj instanceof javax.cache.CacheManager cacheManager) {
        log.debug(
            "Retrieved JSR-107 CacheManager ({}) via reflection from RegionFactory ({}).",
            cacheManagerObj.getClass().getName(),
            regionFactory.getClass().getName());
        return cacheManager;
      } else {
        log.warn(
            "Object retrieved via reflection from field 'cacheManager' in {} is not a javax.cache.CacheManager. Type: {}",
            regionFactory.getClass().getName(),
            cacheManagerObj != null ? cacheManagerObj.getClass().getName() : "null");
        return null;
      }
    } catch (NoSuchFieldException nsfe) {
      log.warn(
          "Reflection failed: Field 'cacheManager' not found in {}. Cannot monitor EHCache.",
          regionFactory.getClass().getName());
      return null;
    } catch (Exception e) {
      log.warn(
          "Could not access CacheManager via reflection on {}: {}",
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
    if (cacheNames.isEmpty()) {
      log.info("No EHCache regions found for monitoring.");
      return;
    }
    log.info("Found {} EHCache regions for monitoring.", cacheNames.size());

    for (String cacheName : cacheNames) {
      try {
        javax.cache.Cache<?, ?> jsr107Cache = jsr107CacheManager.getCache(cacheName);
        if (jsr107Cache == null) {
          log.warn("JSR-107 CacheManager returned null for cache name: {}", cacheName);
          continue; // Skip this cache
        }

        CacheStatistics cacheStats = getCacheStatisticsViaReflection(jsr107Cache);
        if (cacheStats != null) {
          new EhCacheDirectStatisticsMetrics(cacheStats, cacheName).bindTo(registry);
          log.debug("Registered metrics for cache: {}", cacheName);
        } else {
          log.warn(
              "Could not retrieve CacheStatistics for cache '{}' via reflection. Metrics not registered for this cache.",
              cacheName);
        }
      } catch (Exception e) {
        // Catch exceptions during individual cache processing
        log.warn("Failed to register metrics for cache '{}': {}", cacheName, e.getMessage(), e);
      }
    }
  }

  /**
   * Uses reflection to extract the CacheStatistics object from a JSR-107 Cache instance, assuming
   * it's an Ehcache implementation with a statisticsBean containing cacheStatistics.
   */
  private CacheStatistics getCacheStatisticsViaReflection(javax.cache.Cache<?, ?> jsr107Cache) {
    try {
      java.lang.reflect.Field statsBeanField =
          jsr107Cache.getClass().getDeclaredField("statisticsBean");
      statsBeanField.setAccessible(true);
      Object statsBean = statsBeanField.get(jsr107Cache);

      if (statsBean == null) {
        log.debug(
            "Reflection returned null for 'statisticsBean' field in cache: {}",
            jsr107Cache.getName());
        return null;
      }

      java.lang.reflect.Field cacheStatsField =
          statsBean.getClass().getDeclaredField("cacheStatistics");
      cacheStatsField.setAccessible(true);
      Object cacheStatsObj = cacheStatsField.get(statsBean);

      if (cacheStatsObj instanceof CacheStatistics cacheStatistics) {
        return cacheStatistics;
      } else {
        log.warn(
            "Field 'cacheStatistics' in statisticsBean is not of expected type CacheStatistics for cache: {}. Type: {}",
            jsr107Cache.getName(),
            cacheStatsObj != null ? cacheStatsObj.getClass().getName() : "null");
        return null;
      }
    } catch (NoSuchFieldException nsfe) {
      log.warn(
          "Reflection failed to find 'statisticsBean' or 'cacheStatistics' field for cache '{}': {}. Cannot retrieve stats.",
          jsr107Cache.getName(),
          nsfe.getMessage());
      return null;
    } catch (Exception e) {
      log.warn(
          "Error accessing statistics via reflection for cache '{}': {}",
          jsr107Cache.getName(),
          e.getMessage(),
          e);
      return null;
    }
  }

  /** Binds metrics using a CacheStatistics object obtained via reflection. */
  private static class EhCacheDirectStatisticsMetrics implements MeterBinder {
    public static final Tag L_2 = Tag.of("type", "L2");
    private final CacheStatistics stats;
    private final String cacheName; // Renamed from 'name' for clarity

    public EhCacheDirectStatisticsMetrics(CacheStatistics stats, String cacheName) {
      this.stats = stats;
      this.cacheName = cacheName;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      // Derive a shorter metric name part from the full cache name
      // Example: org.hisp.dhis.user.User -> User
      // Example: org.hisp.dhis.user.OrganisationUnit.categoryOptions ->
      // OrganisationUnit.categoryOptions
      // Example: org.hibernate.cache.internal.StandardQueryCache -> StandardQueryCache
      String namePart = deriveNamePart(cacheName);

      Tags cacheTags = Tags.of(Tag.of("cache", namePart), L_2);

      FunctionCounter.builder("ehcache_gets_total", stats, CacheStatistics::getCacheGets)
          .tags(cacheTags)
          .description("The total number of get requests made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_puts_total", stats, CacheStatistics::getCachePuts)
          .tags(cacheTags)
          .description("The total number of put requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_removals_total", stats, CacheStatistics::getCacheRemovals)
          .tags(cacheTags)
          .description("The total number of removal requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_evictions_total", stats, CacheStatistics::getCacheEvictions)
          .tags(cacheTags)
          .description("The total number of evictions from the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_hits_total", stats, CacheStatistics::getCacheHits)
          .tags(cacheTags)
          .description(
              "The total number of times cache lookup methods found a requested entry in the cache")
          .register(registry);

      FunctionCounter.builder("ehcache_misses_total", stats, CacheStatistics::getCacheMisses)
          .tags(cacheTags)
          .description(
              "The total number of times cache lookup methods did not find a requested entry in the cache")
          .register(registry);
    }

    /**
     * Derives a simplified name part for the metric from the full cache name. It finds the first
     * segment starting with an uppercase letter and includes all subsequent segments.
     *
     * <p>Examples: - org.hisp.dhis.user.User -> User - org.hisp.dhis.user.User.roles -> User.roles
     * - org.hibernate.cache.internal.StandardQueryCache -> StandardQueryCache
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
          // Append subsequent parts after the first capitalized one is found
          metricPartBuilder.append(".").append(part);
        } else if (Character.isUpperCase(part.charAt(0))) {
          // Found the first capitalized part
          metricPartBuilder.append(part);
          foundCapitalized = true;
        }
      }
      // If a capitalized part was found, return the built string; otherwise, fallback to the
      // original name
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
