package org.hisp.dhis.monitoring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.JCacheMetrics;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_EHCACHE_ENABLED;

/**
 * Consolidated configuration for wiring up Ehcache3 metrics.
 * This class both configures and binds the Ehcache metrics to the MeterRegistry.
 */
@Configuration
@Conditional(Ehcache3MetricsConfig.EhcacheMetricsEnabledCondition.class)
@Slf4j
public class Ehcache3MetricsConfig  {

    @Bean
    public EhcacheMeterBinder ehcacheMeterBinder() {
        log.info("Creating EhcacheMeterBinder bean");
        EhcacheCachingProvider provider = (EhcacheCachingProvider)
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

            log.info("Attempting to bind metrics for JCache caches managed by: {}",
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
                log.warn("No JCache caches found or bound via CacheManager: {}",
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
