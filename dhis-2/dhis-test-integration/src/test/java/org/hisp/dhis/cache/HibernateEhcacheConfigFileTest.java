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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.ResourceType;
import org.ehcache.config.SizedResourcePool;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hisp.dhis.cache.HibernateEhcacheConfigFileTest.DhisConfig;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.test.config.PostgresTestConfigOverride;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

/**
 * Proves that the {@code cache.ehcache.config.file} default value ({@code classpath:ehcache.xml})
 * loads against the full production {@link SessionFactory}: the JCache {@link CacheManager} is
 * configured from ehcache.xml and the regions carry its settings (bounded heaps) instead of the
 * unbounded ehcache provider defaults. Until the classpath: spelling was normalized in
 * HibernateConfig, this exact configuration failed the SessionFactory boot with "Couldn't load URI
 * from classpath:ehcache.xml".
 *
 * @author Morten Svanæs
 */
@ContextConfiguration(classes = {DhisConfig.class})
class HibernateEhcacheConfigFileTest extends PostgresIntegrationTestBase {

  static class DhisConfig {
    @Bean
    public PostgresTestConfigOverride postgresTestConfigOverride() {
      PostgresTestConfigOverride override = new PostgresTestConfigOverride();
      override.put(
          ConfigurationKey.CACHE_EHCACHE_CONFIG_FILE.getKey(),
          ConfigurationKey.CACHE_EHCACHE_CONFIG_FILE.getDefaultValue());
      return override;
    }
  }

  /** Heap bound for the update timestamps region declared in ehcache.xml. */
  private static final long EHCACHE_XML_TIMESTAMPS_HEAP_ENTRIES = 5_000;

  /** Heap bound from the ehcache.xml default cache template (jsr107:defaults). */
  private static final long EHCACHE_XML_TEMPLATE_HEAP_ENTRIES = 1_000_000;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void cacheManagerIsConfiguredFromEhcacheXml() {
    CacheManager cacheManager = cacheManager();

    assertNotEquals(
        Caching.getCachingProvider().getDefaultURI(),
        cacheManager.getURI(),
        "CacheManager must not run on the provider default configuration");
    assertTrue(
        cacheManager.getURI().toString().contains("ehcache.xml"),
        "CacheManager must be configured from ehcache.xml: " + cacheManager.getURI());
  }

  @Test
  void regionsCarryTheEhcacheXmlHeapBounds() {
    CacheManager cacheManager = cacheManager();

    assertEquals(
        EHCACHE_XML_TEMPLATE_HEAP_ENTRIES,
        heapEntries(cacheManager, User.class.getName()),
        "entity regions must carry the heap bound of the ehcache.xml default template");
    assertEquals(
        EHCACHE_XML_TIMESTAMPS_HEAP_ENTRIES,
        heapEntries(cacheManager, "default-update-timestamps-region"),
        "update timestamps region must carry the heap bound declared in ehcache.xml");
  }

  private CacheManager cacheManager() {
    // Unwrap to the implementor directly: the Spring-managed EntityManagerFactory is a
    // proxy, and unwrapping to plain SessionFactory returns a proxy that cannot be cast
    RegionFactory regionFactory =
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getServiceRegistry()
            .getService(RegionFactory.class);
    JCacheRegionFactory jCacheRegionFactory =
        assertInstanceOf(JCacheRegionFactory.class, regionFactory);
    CacheManager cacheManager = jCacheRegionFactory.getCacheManager();
    assertNotNull(cacheManager);
    return cacheManager;
  }

  private static long heapEntries(CacheManager cacheManager, String region) {
    javax.cache.Cache<Object, Object> cache = cacheManager.getCache(region);
    assertNotNull(cache, "cache region must exist: " + region);
    Eh107Configuration<?, ?> configuration = cache.getConfiguration(Eh107Configuration.class);
    CacheRuntimeConfiguration<?, ?> runtimeConfiguration =
        configuration.unwrap(CacheRuntimeConfiguration.class);
    SizedResourcePool heap =
        runtimeConfiguration.getResourcePools().getPoolForResource(ResourceType.Core.HEAP);
    assertNotNull(heap, "cache region must have a heap resource pool: " + region);
    return heap.getSize();
  }
}
