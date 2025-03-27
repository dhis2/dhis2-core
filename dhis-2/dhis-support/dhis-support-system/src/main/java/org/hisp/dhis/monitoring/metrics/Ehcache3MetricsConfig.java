package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_EHCACHE_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.core.spi.service.StatisticsService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Conditional(Ehcache3MetricsConfig.EhcacheMetricsEnabledCondition.class)
public class Ehcache3MetricsConfig {

    @Autowired
    public void bindEhcacheCachesToRegistry(
        CacheManager cacheManager,
        StatisticsService statisticsService,
        MeterRegistry registry) {

        for (String cacheName : cacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet()) {
            Cache<?, ?> cache = cacheManager.getCache(cacheName, Object.class, Object.class);
            if (cache != null) {
                List<Tag> tags = List.of(Tag.of("cache", cacheName));
                new Ehcache3MetricsBinder(cacheName, statisticsService, tags).bindTo(registry);
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
