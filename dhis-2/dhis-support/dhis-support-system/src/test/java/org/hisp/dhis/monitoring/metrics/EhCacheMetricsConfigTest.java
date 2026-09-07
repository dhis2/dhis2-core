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
package org.hisp.dhis.monitoring.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hisp.dhis.cache.guard.GuardedJCacheRegionFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests the reflective {@code cacheManager} lookup of {@link EhCacheMetricsConfig}: it has to find
 * the field when the configured region factory is a subclass of {@link JCacheRegionFactory}, which
 * is exactly what {@link GuardedJCacheRegionFactory} makes it. The eviction guard counters are
 * bound by {@link EvictionGuardMetricsConfig} and tested there.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EhCacheMetricsConfigTest {

  /**
   * Stands in for {@code GuardedJCacheRegionFactory}: any subclass at all is enough to break a
   * lookup that only looks at the concrete class.
   */
  private static class SubclassedRegionFactory extends JCacheRegionFactory {}

  @Test
  void findsCacheManagerFieldDeclaredBySuperclass() {
    Field field = EhCacheMetricsConfig.findCacheManagerField(SubclassedRegionFactory.class);

    assertNotNull(field, "cacheManager field inherited from JCacheRegionFactory must be found");
    assertEquals(JCacheRegionFactory.class, field.getDeclaringClass());
  }

  @Test
  void findsCacheManagerFieldOnTheConfiguredGuardedRegionFactory() {
    Field field = EhCacheMetricsConfig.findCacheManagerField(GuardedJCacheRegionFactory.class);

    assertNotNull(field, "the region factory DHIS actually configures must not lose its metrics");
    assertEquals(JCacheRegionFactory.class, field.getDeclaringClass());
  }

  @Test
  void readsCacheManagerFromSubclassedRegionFactory() throws Exception {
    SubclassedRegionFactory regionFactory = new SubclassedRegionFactory();
    javax.cache.CacheManager cacheManager = mock(javax.cache.CacheManager.class);
    Field field = JCacheRegionFactory.class.getDeclaredField("cacheManager");
    field.setAccessible(true);
    field.set(regionFactory, cacheManager);

    assertSame(cacheManager, new EhCacheMetricsConfig().getEhCacheManager(regionFactory));
  }

  @Test
  void returnsNullWhenNoCacheManagerFieldExistsAnywhere() {
    RegionFactory regionFactory = mock(RegionFactory.class);

    assertNull(EhCacheMetricsConfig.findCacheManagerField(regionFactory.getClass()));
    assertNull(new EhCacheMetricsConfig().getEhCacheManager(regionFactory));
  }
}
