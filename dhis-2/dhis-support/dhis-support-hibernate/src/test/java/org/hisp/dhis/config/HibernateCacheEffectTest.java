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
package org.hisp.dhis.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.CACHE_EHCACHE_CONFIG_FILE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_QUERY_CACHE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_SECOND_LEVEL_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.ResourceType;
import org.ehcache.config.SizedResourcePool;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.dialect.DhisH2Dialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Effect tests for the second level cache and query cache configuration: instead of inspecting the
 * JPA property map (see {@link HibernateConfigTest}), these tests build a real {@link
 * SessionFactory} from {@link HibernateConfig#getAdditionalProperties(DhisConfigurationProvider)}
 * and assert what Hibernate actually resolved:
 *
 * <ul>
 *   <li>the resolved {@link RegionFactory} service ({@link NoCachingRegionFactory} when off, {@link
 *       JCacheRegionFactory} when on),
 *   <li>the actual cache region names created at boot,
 *   <li>second level cache and query cache behavior via {@code hibernate.generate_statistics}
 *       counters,
 *   <li>the exact SQL statement count per representative flow (entity load by id, cached collection
 *       access, cacheable query) via a {@link StatementInspector}, proving with numbers that
 *       collection and query cache hits rehydrate element ids one SELECT at a time (N+1 on hit)
 *       while the element region is cold,
 *   <li>that the {@code cache.ehcache.config.file} location resolves and the settings from
 *       ehcache.xml (bounded heap sizes) are in effect on regions, so a silent fallback to
 *       unbounded provider default caches becomes a test failure.
 * </ul>
 *
 * @author Morten Svanæs
 */
class HibernateCacheEffectTest {

  private static final String ENTITY_REGION = CachedTestEntity.class.getName();

  private static final String COLLECTION_REGION =
      CachedParentTestEntity.class.getName() + ".children";

  /** Number of collection elements / query result rows used by the statement count tests. */
  private static final int CHILD_COUNT = 5;

  private static final String QUERY_RESULTS_REGION = "default-query-results-region";

  private static final String UPDATE_TIMESTAMPS_REGION = "default-update-timestamps-region";

  /** Heap bound for the update timestamps region declared in ehcache.xml. */
  private static final long EHCACHE_XML_TIMESTAMPS_HEAP_ENTRIES = 5_000;

  /** Heap bound from the ehcache.xml default cache template (jsr107:defaults). */
  private static final long EHCACHE_XML_TEMPLATE_HEAP_ENTRIES = 1_000_000;

  private SessionFactory sessionFactory;

  private final RecordingStatementInspector statementInspector = new RecordingStatementInspector();

  @AfterEach
  void tearDown() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }

  @Test
  void secondLevelCacheOffResolvesNoCachingRegionFactory() {
    sessionFactory = buildSessionFactory(dhisConfig("false", "true", ""));

    assertInstanceOf(NoCachingRegionFactory.class, regionFactory());
    Set<String> regions = regionNames();
    // DisabledCaching in Hibernate 5.6 returns null instead of an empty set
    assertTrue(
        regions == null || regions.isEmpty(),
        "no cache regions must exist when the cache is off: " + regions);
  }

  @Test
  void secondLevelCacheOffNeverTouchesTheCache() {
    sessionFactory = buildSessionFactory(dhisConfig("false", "true", ""));
    persistEntity();

    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
    loadEntityInNewSession();
    loadEntityInNewSession();

    assertEquals(0, statistics.getSecondLevelCachePutCount());
    assertEquals(0, statistics.getSecondLevelCacheHitCount());
    assertEquals(0, statistics.getSecondLevelCacheMissCount());
  }

  @Test
  void secondLevelCacheOnResolvesJCacheRegionFactoryAndCreatesRegions() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false", ""));

    assertInstanceOf(JCacheRegionFactory.class, regionFactory());
    Set<String> regions = regionNames();
    assertTrue(regions.contains(ENTITY_REGION), "entity region must exist: " + regions);
    assertTrue(
        regions.stream().noneMatch(QUERY_RESULTS_REGION::equals),
        "query results region must not exist when the query cache is off: " + regions);
  }

  @Test
  void secondLevelCacheOnServesSecondLoadFromCache() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false", ""));
    persistEntity();
    sessionFactory.getCache().evictAllRegions();

    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
    loadEntityInNewSession();

    assertEquals(1, statistics.getSecondLevelCacheMissCount(), "first load must miss");
    assertEquals(1, statistics.getSecondLevelCachePutCount(), "first load must populate");

    loadEntityInNewSession();

    assertEquals(1, statistics.getSecondLevelCacheHitCount(), "second load must hit");
    assertEquals(1, statistics.getSecondLevelCacheMissCount(), "second load must not miss");
  }

  @Test
  void queryCacheOnServesRepeatedQueryFromCache() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "true", ""));
    persistEntity();

    assertTrue(
        regionNames().contains(QUERY_RESULTS_REGION),
        "query results region must exist when the query cache is on: " + regionNames());

    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
    queryEntitiesInNewSession();
    queryEntitiesInNewSession();

    assertEquals(1, statistics.getQueryCachePutCount(), "first query must populate");
    assertEquals(1, statistics.getQueryCacheHitCount(), "second query must hit");
    assertEquals(1, statistics.getQueryExecutionCount(), "second query must not hit the database");
  }

  @Test
  void queryCacheOffIgnoresCacheableQueries() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false", ""));
    persistEntity();

    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
    queryEntitiesInNewSession();
    queryEntitiesInNewSession();

    assertEquals(0, statistics.getQueryCachePutCount());
    assertEquals(0, statistics.getQueryCacheHitCount());
    assertEquals(2, statistics.getQueryExecutionCount(), "both queries must hit the database");
  }

  // -------------------------------------------------------------------------
  // Deterministic per-flow statement counts (Phase 2)
  // -------------------------------------------------------------------------

  @Test
  void secondLevelCacheOnSecondEntityLoadIssuesNoSql() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false", ""));
    persistEntity();
    sessionFactory.getCache().evictAllRegions();

    statementInspector.clear();
    loadEntityInNewSession();
    assertEquals(1, statementInspector.selectCount(), "first load must query the database once");

    statementInspector.clear();
    loadEntityInNewSession();
    assertEquals(0, statementInspector.selectCount(), "second load must be served from the cache");
  }

  @Test
  void secondLevelCacheOffEveryEntityLoadQueriesTheDatabase() {
    sessionFactory = buildSessionFactory(dhisConfig("false", "true", ""));
    persistEntity();

    statementInspector.clear();
    loadEntityInNewSession();
    loadEntityInNewSession();
    assertEquals(2, statementInspector.selectCount(), "every load must query the database");
  }

  /**
   * Encodes F5, the worst case of the collection cache: collection regions store element ids only,
   * so a collection region HIT with a cold element region rehydrates every element with its own
   * SELECT by id, N statements for N elements. The uncached baseline for the same access is a
   * single SELECT for all elements (see {@link
   * #collectionAccessWithCacheOffLoadsAllElementsInOneSelect()}), so a collection cache "hit" can
   * cost N times the SQL of no cache at all.
   */
  @Test
  void collectionCacheHitWithColdElementRegionRehydratesEachElementById() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false", ""));
    persistParentWithChildren(CHILD_COUNT);
    sessionFactory.getCache().evictAllRegions();
    // warm the parent, collection and element regions
    readParentAndChildrenInNewSession();
    // cold element region, warm collection region: the production steady state after any
    // element region eviction (bounded heap, TTL) while the collection entry survives
    sessionFactory.getCache().evictEntityData(CachedTestEntity.class);

    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
    statementInspector.clear();
    readParentAndChildrenInNewSession();

    CacheRegionStatistics collectionStatistics =
        statistics.getDomainDataRegionStatistics(COLLECTION_REGION);
    assertNotNull(collectionStatistics);
    assertEquals(1, collectionStatistics.getHitCount(), "collection region must hit");
    assertEquals(
        CHILD_COUNT,
        statementInspector.selectCount(),
        "a collection cache hit must issue one SELECT per element when the element region is"
            + " cold");
  }

  @Test
  void collectionAccessWithCacheOffLoadsAllElementsInOneSelect() {
    sessionFactory = buildSessionFactory(dhisConfig("false", "true", ""));
    persistParentWithChildren(CHILD_COUNT);

    statementInspector.clear();
    readParentAndChildrenInNewSession();

    assertEquals(
        2,
        statementInspector.selectCount(),
        "without the cache the same access is one SELECT for the parent and one SELECT for all"
            + " elements");
  }

  @Test
  void collectionCacheHitWithWarmElementRegionIssuesNoSql() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false", ""));
    persistParentWithChildren(CHILD_COUNT);
    sessionFactory.getCache().evictAllRegions();
    // warm the parent, collection and element regions
    readParentAndChildrenInNewSession();

    statementInspector.clear();
    readParentAndChildrenInNewSession();

    assertEquals(
        0, statementInspector.selectCount(), "fully warm regions must serve without any SQL");
  }

  /**
   * Encodes the query cache flavor of the same mechanism (see PR #22711): cached query results
   * store entity ids only, so a query cache HIT with a cold entity region issues one SELECT per
   * result row, N statements where the original query was one.
   */
  @Test
  void queryCacheHitWithColdEntityRegionRehydratesEachRowById() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "true", ""));
    persistEntities(CHILD_COUNT);

    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();
    statementInspector.clear();
    queryEntitiesInNewSession(CHILD_COUNT);
    assertEquals(1, statementInspector.selectCount(), "first query must be a single SELECT");

    sessionFactory.getCache().evictEntityData(CachedTestEntity.class);
    statementInspector.clear();
    queryEntitiesInNewSession(CHILD_COUNT);

    assertEquals(1, statistics.getQueryCacheHitCount(), "second query must hit the query cache");
    assertEquals(1, statistics.getQueryExecutionCount(), "the query itself must not run again");
    assertEquals(
        CHILD_COUNT,
        statementInspector.selectCount(),
        "a query cache hit must issue one SELECT per result row when the entity region is cold");
  }

  @Test
  void queryCacheHitWithWarmEntityRegionIssuesNoSql() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "true", ""));
    persistEntities(CHILD_COUNT);

    queryEntitiesInNewSession(CHILD_COUNT);
    statementInspector.clear();
    queryEntitiesInNewSession(CHILD_COUNT);

    assertEquals(
        0,
        statementInspector.selectCount(),
        "a query cache hit with a warm entity region must not issue any SQL");
  }

  @Test
  void noEhcacheConfigFileFallsBackToUnboundedProviderDefaultCaches() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "true", ""));

    CacheManager cacheManager = cacheManager();
    assertEquals(Caching.getCachingProvider().getDefaultURI(), cacheManager.getURI());
    assertEquals(
        Long.MAX_VALUE,
        heapEntries(cacheManager, ENTITY_REGION),
        "provider default caches are unbounded");
  }

  /**
   * Encodes the {@code cache.ehcache.config.file} default value bug: Hibernate's
   * ClassLoaderServiceImpl only understands the nonstandard 'classpath://' scheme, so the
   * documented 'classpath:' spelling (which is also the ConfigurationKey default) must be
   * normalized before it is handed to Hibernate, otherwise the SessionFactory fails to boot. Both
   * spellings must load ehcache.xml.
   */
  @ParameterizedTest
  @ValueSource(strings = {"classpath:ehcache.xml", "classpath://ehcache.xml"})
  void ehcacheConfigFileLoadsForAllClasspathSpellings(String location) {
    assertEhcacheXmlIsInEffect(location);
  }

  @Test
  void ehcacheConfigFileDefaultValueLoads() {
    assertEhcacheXmlIsInEffect(CACHE_EHCACHE_CONFIG_FILE.getDefaultValue());
  }

  @Test
  void ehcacheConfigFileLoadsFromFileUrl(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("ehcache.xml");
    try (InputStream in =
        Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("ehcache.xml"),
            "ehcache.xml must be on the classpath")) {
      Files.copy(in, file);
    }

    assertEhcacheXmlIsInEffect(file.toUri().toString());
  }

  private void assertEhcacheXmlIsInEffect(String location) {
    sessionFactory = buildSessionFactory(dhisConfig("true", "true", location));

    CacheManager cacheManager = cacheManager();
    assertNotEquals(
        Caching.getCachingProvider().getDefaultURI(),
        cacheManager.getURI(),
        "CacheManager must not run on the provider default configuration");
    assertTrue(
        cacheManager.getURI().toString().contains("ehcache.xml"),
        "CacheManager must be configured from ehcache.xml: " + cacheManager.getURI());
    assertEquals(
        EHCACHE_XML_TIMESTAMPS_HEAP_ENTRIES,
        heapEntries(cacheManager, UPDATE_TIMESTAMPS_REGION),
        "update timestamps region must carry the heap bound declared in ehcache.xml");
    assertEquals(
        EHCACHE_XML_TEMPLATE_HEAP_ENTRIES,
        heapEntries(cacheManager, ENTITY_REGION),
        "entity region must carry the heap bound of the ehcache.xml default template");
    assertEquals(
        EHCACHE_XML_TEMPLATE_HEAP_ENTRIES,
        heapEntries(cacheManager, QUERY_RESULTS_REGION),
        "query results region must carry the heap bound declared in ehcache.xml");
  }

  // -------------------------------------------------------------------------
  // Test plumbing
  // -------------------------------------------------------------------------

  private static DhisConfigurationProvider dhisConfig(
      String secondLevelCache, String queryCache, String ehcacheConfigFile) {
    DhisConfigurationProvider dhisConfig = mock(DhisConfigurationProvider.class);
    when(dhisConfig.isEnabled(any())).thenCallRealMethod();
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn(secondLevelCache);
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn(queryCache);
    when(dhisConfig.getProperty(CACHE_EHCACHE_CONFIG_FILE)).thenReturn(ehcacheConfigFile);
    return dhisConfig;
  }

  private SessionFactory buildSessionFactory(DhisConfigurationProvider dhisConfig) {
    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
    properties.forEach((key, value) -> registryBuilder.applySetting((String) key, value));
    registryBuilder.applySetting(AvailableSettings.DIALECT, DhisH2Dialect.class.getName());
    registryBuilder.applySetting(AvailableSettings.DRIVER, "org.h2.Driver");
    registryBuilder.applySetting(
        AvailableSettings.URL,
        "jdbc:h2:mem:cache-effect-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    registryBuilder.applySetting(AvailableSettings.USER, "sa");
    registryBuilder.applySetting(AvailableSettings.PASS, "");
    // The DHIS2 schema is owned by Flyway, but this test schema only exists in memory
    registryBuilder.applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop");
    registryBuilder.applySetting(AvailableSettings.GENERATE_STATISTICS, "true");
    registryBuilder.applySetting(AvailableSettings.STATEMENT_INSPECTOR, statementInspector);

    return new MetadataSources(registryBuilder.build())
        .addAnnotatedClass(CachedTestEntity.class)
        .addAnnotatedClass(CachedParentTestEntity.class)
        .buildMetadata()
        .buildSessionFactory();
  }

  private RegionFactory regionFactory() {
    return ((SessionFactoryImplementor) sessionFactory)
        .getServiceRegistry()
        .getService(RegionFactory.class);
  }

  private Set<String> regionNames() {
    return ((SessionFactoryImplementor) sessionFactory).getCache().getCacheRegionNames();
  }

  private CacheManager cacheManager() {
    JCacheRegionFactory regionFactory =
        assertInstanceOf(JCacheRegionFactory.class, regionFactory());
    return regionFactory.getCacheManager();
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

  private void persistEntity() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(new CachedTestEntity(1L, "cached"));
      transaction.commit();
    }
  }

  private void persistEntities(int count) {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      for (long id = 1; id <= count; id++) {
        session.persist(new CachedTestEntity(id, "cached" + id));
      }
      transaction.commit();
    }
  }

  private void persistParentWithChildren(int count) {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      CachedParentTestEntity parent = new CachedParentTestEntity(1L, "parent");
      for (long id = 1; id <= count; id++) {
        parent.getChildren().add(new CachedTestEntity(id, "child" + id));
      }
      session.persist(parent);
      transaction.commit();
    }
  }

  private void readParentAndChildrenInNewSession() {
    try (Session session = sessionFactory.openSession()) {
      CachedParentTestEntity parent = session.get(CachedParentTestEntity.class, 1L);
      assertNotNull(parent);
      int read = 0;
      for (CachedTestEntity child : parent.getChildren()) {
        assertNotNull(child.getName());
        read++;
      }
      assertEquals(CHILD_COUNT, read, "all collection elements must be readable");
    }
  }

  private void loadEntityInNewSession() {
    try (Session session = sessionFactory.openSession()) {
      assertNotNull(session.get(CachedTestEntity.class, 1L));
    }
  }

  private void queryEntitiesInNewSession() {
    queryEntitiesInNewSession(1);
  }

  private void queryEntitiesInNewSession(int expectedCount) {
    try (Session session = sessionFactory.openSession()) {
      assertEquals(
          expectedCount,
          session
              .createQuery("from CachedTestEntity", CachedTestEntity.class)
              .setCacheable(true)
              .getResultList()
              .size());
    }
  }

  /** Records every SQL statement Hibernate issues so tests can assert exact statement counts. */
  private static final class RecordingStatementInspector implements StatementInspector {

    private final List<String> statements = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
      statements.add(sql);
      return sql;
    }

    long selectCount() {
      return statements.stream()
          .filter(sql -> sql.toLowerCase(Locale.ROOT).startsWith("select"))
          .count();
    }

    void clear() {
      statements.clear();
    }
  }
}
