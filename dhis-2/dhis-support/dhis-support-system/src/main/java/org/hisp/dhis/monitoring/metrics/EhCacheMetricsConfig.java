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
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.hibernate.SessionFactory;
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
      SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
      SessionFactoryImplementor sessionFactoryImplementor =
          (SessionFactoryImplementor) sessionFactory;

      // Try to access the CacheManager
      ServiceRegistry serviceRegistry = sessionFactoryImplementor.getServiceRegistry();
      RegionFactory regionFactory = serviceRegistry.getService(RegionFactory.class);

      if (regionFactory == null) {
        log.warn("No RegionFactory found for second-level cache monitoring");
        return;
      }

      Object cacheManager = getEhCacheManager(regionFactory);
      if (cacheManager instanceof CacheManager) {
        registerCacheMetrics((CacheManager) cacheManager, registry);
      } else {
        log.warn("Unable to access EHCache CacheManager for monitoring");
      }
    } catch (PersistenceException | ClassCastException ex) {
      log.warn("Failed to bind EHCache metrics: {}", ex.getMessage());
    }
  }

  /**
   * Attempts to extract the EHCache CacheManager from the Hibernate RegionFactory. This uses
   * reflection as there isn't a standardized way to access it directly.
   */
  private Object getEhCacheManager(RegionFactory regionFactory) {
    try {
      // Try to access the cacheManager field via reflection
      java.lang.reflect.Field field = regionFactory.getClass().getDeclaredField("cacheManager");
      field.setAccessible(true);
      return field.get(regionFactory);
    } catch (Exception e) {
      log.warn("Could not access CacheManager via reflection: {}", e.getMessage());
      return null;
    }
  }

  /** Registers metrics for each cache in the CacheManager. */
  private void registerCacheMetrics(CacheManager cacheManager, MeterRegistry registry) {
    Set<String> cacheNames =
        cacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet();
    log.info("Found {} EHCache regions for monitoring", cacheNames.size());

    // Try to get the statistics service
    StatisticsService statisticsService = null;
    try {
      Object serviceObj =
          cacheManager.getClass().getMethod("getStatisticsService").invoke(cacheManager);
      if (serviceObj instanceof StatisticsService) {
        statisticsService = (StatisticsService) serviceObj;
      }
    } catch (Exception e) {
      log.warn("Could not access EHCache StatisticsService: {}", e.getMessage());
    }

    for (String cacheName : cacheNames) {
      try {
        // Since EhCache 3 is generic, we need Object types when we don't know the exact types
        Cache<Object, Object> cache = cacheManager.getCache(cacheName, Object.class, Object.class);
        if (cache != null) {
          Tags tags =
              Tags.of(Tag.of("name", cacheName), Tag.of("type", "hibernate-second-level-cache"));

          if (statisticsService != null) {
            // Register all available metrics using the statistics service
            new EhCache3Metrics(cache, cacheName, statisticsService, tags).bindTo(registry);
            log.debug("Registered metrics for cache: {}", cacheName);
          } else {
            // Fallback with limited metrics if statistics service is not available
            new EhCache3FallbackMetrics(cache, cacheName, tags).bindTo(registry);
            log.debug("Registered limited metrics for cache: {}", cacheName);
          }
        }
      } catch (Exception e) {
        log.warn("Failed to register metrics for cache {}: {}", cacheName, e.getMessage());
      }
    }
  }

  /** Custom metrics binder for EhCache 3.x with full statistics. */
  private static class EhCache3Metrics implements MeterBinder {
    private final Cache<?, ?> cache;
    private final String cacheName;
    private final StatisticsService statisticsService;
    private final Iterable<Tag> tags;

    public EhCache3Metrics(
        Cache<?, ?> cache,
        String cacheName,
        StatisticsService statisticsService,
        Iterable<Tag> tags) {
      this.cache = cache;
      this.cacheName = cacheName;
      this.statisticsService = statisticsService;
      this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      CacheStatistics stats = statisticsService.getCacheStatistics(cacheName);

      FunctionCounter.builder("cache.gets", stats, CacheStatistics::getCacheGets)
          .tags(tags)
          .description("The number of get requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("cache.puts", stats, CacheStatistics::getCachePuts)
          .tags(tags)
          .description("The number of put requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("cache.removals", stats, CacheStatistics::getCacheRemovals)
          .tags(tags)
          .description("The number of removal requests that were made to the cache")
          .register(registry);

      FunctionCounter.builder("cache.evictions", stats, CacheStatistics::getCacheEvictions)
          .tags(tags)
          .description("The number of evictions from the cache")
          .register(registry);

      FunctionCounter.builder("cache.hits", stats, CacheStatistics::getCacheHits)
          .tags(tags)
          .description(
              "The number of times cache lookup methods found a requested entry in the cache")
          .register(registry);

      FunctionCounter.builder("cache.misses", stats, CacheStatistics::getCacheMisses)
          .tags(tags)
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

  /**
   * Fallback metrics binder with limited statistics when the StatisticsService is not available.
   */
  private static class EhCache3FallbackMetrics implements MeterBinder {
    private final Cache<?, ?> cache;
    private final String cacheName;
    private final Iterable<Tag> tags;

    public EhCache3FallbackMetrics(Cache<?, ?> cache, String cacheName, Iterable<Tag> tags) {
      this.cache = cache;
      this.cacheName = cacheName;
      this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      // Only register metrics that we can calculate without the statistics service
      Gauge.builder(
              "cache.size",
              cache,
              c -> {
                try {
                  return c.iterator().hasNext() ? 1 : 0; // We can only check if it's empty
                } catch (Exception ex) {
                  return 0;
                }
              })
          .tags(tags)
          .description("An estimate of the number of entries in the cache")
          .register(registry);
    }
  }

  static class EhCacheMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_EHCACHE_ENABLED;
    }
  }
}
