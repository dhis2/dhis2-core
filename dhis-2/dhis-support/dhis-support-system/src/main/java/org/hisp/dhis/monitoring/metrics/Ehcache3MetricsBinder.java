package org.hisp.dhis.monitoring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.spi.service.StatisticsService;
import org.springframework.context.annotation.Configuration;

@Configuration

public class Ehcache3MetricsBinder implements MeterBinder {

    private final String cacheName;
    private final StatisticsService statisticsService;
    private final Iterable<Tag> tags;

    public Ehcache3MetricsBinder(String cacheName, StatisticsService statisticsService, Iterable<Tag> tags) {
        this.cacheName = cacheName;
        this.statisticsService = statisticsService;
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        CacheStatistics stats = statisticsService.getCacheStatistics(cacheName);

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
