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
import org.hibernate.Session;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Effect tests for the second level cache configuration that Postgres integration tests (and CI)
 * actually run with ({@code postgresTestDhis.conf}: cache on, query cache off, no ehcache.xml): the
 * region factory Hibernate resolved, the cache regions that exist at boot, and the fact that the
 * JCache {@link CacheManager} runs on the ehcache provider default configuration, not on
 * ehcache.xml. Also proves the per-flow region behavior on real production mappings: a second
 * entity load by id is a region hit, and a collection region hit rehydrates every element
 * individually through the entity region (F5, N+1 on hit).
 *
 * @author Morten Svanæs
 */
class HibernateCacheRegionsTest extends PostgresIntegrationTestBase {

  @Autowired private EntityManagerFactory entityManagerFactory;

  /** Number of children of the parent organisation unit in the collection flow test. */
  private static final int CHILD_COUNT = 3;

  @Autowired private OrganisationUnitService organisationUnitService;

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

  @Test
  void secondEntityLoadByIdIsARegionHit() {
    OrganisationUnit unit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(unit);

    Statistics statistics = enableStatistics();
    sessionFactory().getCache().evictEntityData(OrganisationUnit.class);
    statistics.clear();

    loadOrganisationUnitInNewSession(unit.getId());
    CacheRegionStatistics regionStatistics = entityRegionStatistics(statistics);
    assertEquals(1, regionStatistics.getMissCount(), "first load must miss the region");
    assertEquals(1, regionStatistics.getPutCount(), "first load must populate the region");
    assertEquals(0, regionStatistics.getHitCount(), "first load cannot hit the region");

    loadOrganisationUnitInNewSession(unit.getId());
    regionStatistics = entityRegionStatistics(statistics);
    assertEquals(1, regionStatistics.getHitCount(), "second load must hit the region");
    assertEquals(1, regionStatistics.getMissCount(), "second load must not miss the region");
  }

  /**
   * Encodes F5 on a real production mapping ({@code OrganisationUnit.children}): collection regions
   * store element ids only, so a collection region HIT with a cold element region rehydrates every
   * element individually through the entity region and, on miss, the database. The exact SQL cost
   * of this flow (one SELECT per element) is pinned by HibernateCacheEffectTest in
   * dhis-support-hibernate.
   */
  @Test
  void collectionCacheHitRehydratesEachElementThroughTheEntityRegion() {
    OrganisationUnit parent = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(parent);
    for (int i = 0; i < CHILD_COUNT; i++) {
      organisationUnitService.addOrganisationUnit(createOrganisationUnit((char) ('C' + i), parent));
    }

    Statistics statistics = enableStatistics();
    sessionFactory().getCache().evictAllRegions();
    // warm the parent and element entity regions and the children collection region
    readChildrenInNewSession(parent.getId());
    // cold entity region, warm collection region: the steady state after any entity region
    // eviction (bounded heap, TTL, write) while the collection entry survives
    sessionFactory().getCache().evictEntityData(OrganisationUnit.class);
    statistics.clear();

    readChildrenInNewSession(parent.getId());

    CacheRegionStatistics collectionStatistics =
        statistics.getDomainDataRegionStatistics(OrganisationUnit.class.getName() + ".children");
    assertNotNull(collectionStatistics);
    assertEquals(1, collectionStatistics.getHitCount(), "children collection region must hit");
    assertEquals(
        1 + CHILD_COUNT,
        entityRegionStatistics(statistics).getMissCount(),
        "the parent and every collection element must be rehydrated individually");
  }

  private SessionFactoryImplementor sessionFactory() {
    // Unwrap to the implementor directly: the Spring-managed EntityManagerFactory is a
    // proxy, and unwrapping to plain SessionFactory returns a proxy that cannot be cast
    return entityManagerFactory.unwrap(SessionFactoryImplementor.class);
  }

  private RegionFactory regionFactory() {
    return sessionFactory().getServiceRegistry().getService(RegionFactory.class);
  }

  private Statistics enableStatistics() {
    Statistics statistics = sessionFactory().getStatistics();
    statistics.setStatisticsEnabled(true);
    return statistics;
  }

  private CacheRegionStatistics entityRegionStatistics(Statistics statistics) {
    CacheRegionStatistics regionStatistics =
        statistics.getDomainDataRegionStatistics(OrganisationUnit.class.getName());
    assertNotNull(regionStatistics);
    return regionStatistics;
  }

  private void loadOrganisationUnitInNewSession(long id) {
    try (Session session = sessionFactory().openSession()) {
      assertNotNull(session.get(OrganisationUnit.class, id));
    }
  }

  private void readChildrenInNewSession(long parentId) {
    try (Session session = sessionFactory().openSession()) {
      OrganisationUnit parent = session.get(OrganisationUnit.class, parentId);
      assertNotNull(parent);
      int read = 0;
      for (OrganisationUnit child : parent.getChildren()) {
        assertNotNull(child.getName());
        read++;
      }
      assertEquals(CHILD_COUNT, read, "all children must be readable");
    }
  }
}
