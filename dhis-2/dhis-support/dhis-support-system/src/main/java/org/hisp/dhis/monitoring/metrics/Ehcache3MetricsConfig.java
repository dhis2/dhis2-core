package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_EHCACHE_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.spi.service.StatisticsService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Consolidated configuration for wiring up Ehcache3 metrics.
 * This class both configures and binds the Ehcache metrics to the MeterRegistry.
 */
@Configuration
@Conditional(Ehcache3MetricsConfig.EhcacheMetricsEnabledCondition.class)
public class Ehcache3MetricsConfig implements MeterBinder {

    private final CacheManager cacheManager;
    private final StatisticsService statisticsService;

    @Autowired
    public Ehcache3MetricsConfig(CacheManager cacheManager, StatisticsService statisticsService) {
        this.cacheManager = cacheManager;
        this.statisticsService = statisticsService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Iterate over the names of configured caches
        for (String cacheName : cacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet()) {
            // Optionally check that the cache exists
            Cache<?, ?> cache = cacheManager.getCache(cacheName, Object.class, Object.class);
            if (cache != null) {
                CacheStatistics stats = statisticsService.getCacheStatistics(cacheName);
                List<Tag> tags = List.of(Tag.of("cache", cacheName));
                // Bind gauges for various metrics
                registry.gauge("cache.gets", tags, stats, CacheStatistics::getCacheGets);
                registry.gauge("cache.puts", tags, stats, CacheStatistics::getCachePuts);
                registry.gauge("cache.evictions", tags, stats, CacheStatistics::getCacheEvictions);
                registry.gauge("cache.removals", tags, stats, CacheStatistics::getCacheRemovals);
                registry.gauge("cache.hit.count", tags, stats, CacheStatistics::getCacheHits);
                registry.gauge("cache.miss.count", tags, stats, CacheStatistics::getCacheMisses);
                registry.gauge("cache.hit.ratio", tags, stats, CacheStatistics::getCacheHitPercentage);
                registry.gauge("cache.miss.ratio", tags, stats, CacheStatistics::getCacheMissPercentage);
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
