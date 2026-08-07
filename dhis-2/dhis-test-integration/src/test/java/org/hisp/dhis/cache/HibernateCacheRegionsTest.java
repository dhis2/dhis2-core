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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Effect tests for the second level cache configuration that Postgres integration tests (and CI)
 * actually run with ({@code postgresTestDhis.conf}: cache on, query cache off, no ehcache.xml): the
 * region factory Hibernate resolved, the cache regions that exist at boot, and the fact that the
 * JCache {@link CacheManager} runs on the ehcache provider default configuration, not on
 * ehcache.xml.
 *
 * @author Morten Svanæs
 */
class HibernateCacheRegionsTest extends PostgresIntegrationTestBase {

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void secondLevelCacheIsOnWithJCacheRegionFactory() {
    assertInstanceOf(JCacheRegionFactory.class, regionFactory());
  }

  @Test
  void allCachedEntityAndCollectionRegionsExistAtBoot() {
    Set<String> regions = sessionFactory().getCache().getCacheRegionNames();

    assertNotNull(regions);
    assertTrue(regions.contains(User.class.getName()), "User entity region must exist: " + regions);
    assertTrue(
        regions.contains(OrganisationUnit.class.getName()),
        "OrganisationUnit entity region must exist: " + regions);
    // The cached-region inventory is ~235 declarations: 33 annotated entities plus ~202
    // cached collections in hbm.xml mappings. Guard the order of magnitude so a mapping
    // change that silently drops caching for whole groups of regions is caught.
    assertTrue(
        regions.size() >= 200,
        "expected the full cached-region inventory (~235), but found: " + regions.size());
  }

  @Test
  void cacheManagerRunsOnProviderDefaultsWithoutEhcacheConfigFile() {
    JCacheRegionFactory regionFactory =
        assertInstanceOf(JCacheRegionFactory.class, regionFactory());
    CacheManager cacheManager = regionFactory.getCacheManager();

    assertNotNull(cacheManager);
    assertEquals(
        Caching.getCachingProvider().getDefaultURI(),
        cacheManager.getURI(),
        "with a blank cache.ehcache.config.file the CacheManager must run on the ehcache"
            + " provider default configuration");
  }

  private SessionFactoryImplementor sessionFactory() {
    // Unwrap to the implementor directly: the Spring-managed EntityManagerFactory is a
    // proxy, and unwrapping to plain SessionFactory returns a proxy that cannot be cast
    return entityManagerFactory.unwrap(SessionFactoryImplementor.class);
  }

  private RegionFactory regionFactory() {
    return sessionFactory().getServiceRegistry().getService(RegionFactory.class);
  }
}
