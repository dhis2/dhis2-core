/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.cache.guard;

import static org.hisp.dhis.external.conf.ConfigurationKey.USE_QUERY_CACHE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_SECOND_LEVEL_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.config.HibernateConfig;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.hibernate.dialect.DhisH2Dialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Boot-level proof that the eviction guard is wired and refuses a stale late put, asserted against
 * a real {@link SessionFactory} over H2 and a real ehcache-backed CacheManager built from {@link
 * HibernateConfig#getAdditionalProperties(DhisConfigurationProvider)}. Everything below the test is
 * production wiring.
 *
 * <p>Wiring assertions: a NONSTRICT_READ_WRITE mapping resolves to the guarded access classes while
 * a READ_WRITE mapping in the same SessionFactory stays on Hibernate's own, so dropping the region
 * factory, the region or the NONSTRICT dispatch fails here instead of silently reverting.
 * Behavioural assertion: the stranding interleaving is scripted, not raced. A reader session opened
 * before the write keeps its caching timestamp older than the eviction, and its late {@code
 * putFromLoad} is replayed directly on the {@link EntityDataAccess} SPI, so the only reason the put
 * is refused is the guard's timestamp comparison. {@link GuardedNonStrictInterleavingTest} covers
 * every eviction path against a stub; this test proves the rules survive real wiring.
 *
 * <p>{@link NonstrictEntity} (NONSTRICT_READ_WRITE, owns a NONSTRICT_READ_WRITE collection) and its
 * element type {@link ReadWriteEntity} (the READ_WRITE control) share one SessionFactory, so
 * guarding is shown to follow the mapping, not the region factory. Region names are the nested
 * class FQNs, unique to this test, because the {@link EvictionGuardStats} registry has no reset.
 * {@code dhisConfig} and {@code buildSessionFactory} are copied from {@code
 * org.hisp.dhis.config.HibernateCacheEffectTest} (private there) minus its statement inspector and
 * statistics settings. Isolation between test methods comes from {@code sessionFactory.close()},
 * which closes the CacheManager and with it every region's storage.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GuardedNonStrictCacheEffectTest {

  private static final long ENTITY_ID = 1L;

  private static final String NONSTRICT_ENTITY_REGION = NonstrictEntity.class.getName();

  private static final String NONSTRICT_COLLECTION_ROLE =
      NonstrictEntity.class.getName() + ".children";

  private static final String NAME_BEFORE_WRITE = "before the write";

  private static final String NAME_AFTER_WRITE = "written by W";

  /**
   * Stands in for the cache entry the reader's load would have put. The guard refuses before the
   * value is looked at, so any non-null value is enough, and a value that is obviously not a cache
   * entry makes it plain that the refusal cannot depend on it.
   */
  private static final String STALE_PLACEHOLDER = "value read before the write";

  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = buildSessionFactory(dhisConfig("true", "false"));
  }

  @AfterEach
  void tearDown() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
  }

  @Test
  void nonstrictEntityRegionGetsGuardedAccess() {
    EntityDataAccess access = entityAccess(NonstrictEntity.class);

    assertInstanceOf(GuardedEntityNonStrictReadWriteAccess.class, access);
    assertEquals(
        NONSTRICT_ENTITY_REGION,
        access.getRegion().getName(),
        "the region name is the key of this test's guard counters and must stay unique to it");
  }

  @Test
  void nonstrictCollectionRegionGetsGuardedAccess() {
    CollectionPersister persister =
        sessionFactoryImplementor().getMetamodel().collectionPersister(NONSTRICT_COLLECTION_ROLE);
    CollectionDataAccess access = persister.getCacheAccessStrategy();
    assertNotNull(
        access, "the NONSTRICT collection mapping must be cached: " + persister.getRole());

    // deliberately not an assertion on access.getAccessType(): the collection access built for a
    // NONSTRICT_READ_WRITE mapping reports READ_WRITE in Hibernate 5.6, which is why the region
    // dispatches on the configured access type instead
    assertInstanceOf(GuardedCollectionNonStrictReadWriteAccess.class, access);
  }

  @Test
  void readWriteRegionStaysStock() {
    EntityDataAccess access = entityAccess(ReadWriteEntity.class);

    assertInstanceOf(AbstractReadWriteAccess.class, access);
    assertFalse(
        access instanceof GuardedEntityNonStrictReadWriteAccess,
        "a READ_WRITE mapping must keep Hibernate's own access, it already bars stale puts with a"
            + " SoftLock");
  }

  /**
   * Ivo's interleaving, executed step by step against real storage: a reader whose transaction
   * started before a write completes its load after that write, and its {@code putFromLoad} would
   * strand the pre write value in the region under plain NONSTRICT_READ_WRITE. The guard must
   * refuse it, and must refuse it without wedging the region for later readers.
   */
  @Test
  void scriptedStrandingLosesAgainstRealStorage() {
    persistEntity();
    EntityPersister persister = entityPersister(NonstrictEntity.class);
    EntityDataAccess access = persister.getCacheAccessStrategy();
    EvictionGuardStats stats = EvictionGuardStats.forRegion(access.getRegion().getName());
    // the counters are process wide and never reset, and every test method builds a new
    // SessionFactory over the same region name, so only the delta over this method is deterministic
    long refusedBefore = stats.getRefused();
    long selfEvictedBefore = stats.getSelfEvicted();

    // R opens before the write and stays open across it, so its caching timestamp keeps predating
    // the eviction. That is the whole setup: no thread and no sleep is involved.
    try (Session reader = sessionFactory.openSession()) {
      SharedSessionContractImplementor readerSession = (SharedSessionContractImplementor) reader;
      long readerTimestamp = cachingTimestamp(readerSession);
      assertEquals(NAME_BEFORE_WRITE, loadName(reader), "R must read the pre write row");
      assertTrue(
          cachedEntity(),
          "R's own load must populate the region, so the stranding scenario has something to"
              + " strand");

      updateEntityInNewSession();

      assertFalse(
          cachedEntity(),
          "the write must evict the entity, which is plain NONSTRICT_READ_WRITE behaviour and the"
              + " precondition of the stale put");
      assertEquals(
          readerTimestamp,
          cachingTimestamp(readerSession),
          "holding R open must keep its pre write caching timestamp");

      Object cacheKey =
          access.generateCacheKey(ENTITY_ID, persister, sessionFactoryImplementor(), null);
      // R's late put, replayed on the SPI exactly as R's load would have completed it after the
      // write landed
      assertFalse(
          access.putFromLoad(readerSession, cacheKey, STALE_PLACEHOLDER, null),
          "a put from a transaction older than the eviction must be refused");
      assertFalse(access.contains(cacheKey), "the refused value must not reach region storage");
      assertFalse(cachedEntity(), "the region must still hold nothing for the key");
      assertEquals(
          1, stats.getRefused() - refusedBefore, "the refusal must be counted for the region");
      assertEquals(
          0,
          stats.getSelfEvicted() - selfEvictedBefore,
          "the up front check must refuse, so no value was stored and none had to be taken back");
    }

    // and the refusal must not wedge the region: a transaction that starts after the write caches
    // the written value again
    try (Session fresh = sessionFactory.openSession()) {
      assertEquals(NAME_AFTER_WRITE, loadName(fresh), "a fresh load must see the written value");
    }
    assertTrue(
        cachedEntity(),
        "a put from a transaction newer than the eviction must repopulate the region");
  }

  // -------------------------------------------------------------------------
  // Test plumbing
  // -------------------------------------------------------------------------

  /** Copied from the private helper of {@code HibernateCacheEffectTest}. */
  private static DhisConfigurationProvider dhisConfig(String secondLevelCache, String queryCache) {
    DhisConfigurationProvider dhisConfig = mock(DhisConfigurationProvider.class);
    when(dhisConfig.isEnabled(any())).thenCallRealMethod();
    when(dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE)).thenReturn(secondLevelCache);
    when(dhisConfig.getProperty(USE_QUERY_CACHE)).thenReturn(queryCache);
    return dhisConfig;
  }

  /**
   * Copied from the private helper of {@code HibernateCacheEffectTest}, minus its statement
   * inspector and statistics settings, which nothing here asserts on.
   */
  private SessionFactory buildSessionFactory(DhisConfigurationProvider dhisConfig) {
    Properties properties = HibernateConfig.getAdditionalProperties(dhisConfig);

    StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
    properties.forEach((key, value) -> registryBuilder.applySetting((String) key, value));
    registryBuilder.applySetting(AvailableSettings.DIALECT, DhisH2Dialect.class.getName());
    registryBuilder.applySetting(AvailableSettings.DRIVER, "org.h2.Driver");
    registryBuilder.applySetting(
        AvailableSettings.URL,
        "jdbc:h2:mem:guarded-nonstrict-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    registryBuilder.applySetting(AvailableSettings.USER, "sa");
    registryBuilder.applySetting(AvailableSettings.PASS, "");
    // The DHIS2 schema is owned by Flyway, but this test schema only exists in memory
    registryBuilder.applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop");

    return new MetadataSources(registryBuilder.build())
        .addAnnotatedClass(NonstrictEntity.class)
        .addAnnotatedClass(ReadWriteEntity.class)
        .buildMetadata()
        .buildSessionFactory();
  }

  private SessionFactoryImplementor sessionFactoryImplementor() {
    return (SessionFactoryImplementor) sessionFactory;
  }

  private EntityPersister entityPersister(Class<?> entityClass) {
    return sessionFactoryImplementor().getMetamodel().entityPersister(entityClass);
  }

  private EntityDataAccess entityAccess(Class<?> entityClass) {
    EntityPersister persister = entityPersister(entityClass);
    EntityDataAccess access = persister.getCacheAccessStrategy();
    assertNotNull(access, "the mapping must be cached: " + persister.getEntityName());
    return access;
  }

  private static long cachingTimestamp(SharedSessionContractImplementor session) {
    return session.getCacheTransactionSynchronization().getCachingTimestamp();
  }

  /** Whether the real region storage currently holds the test entity. */
  private boolean cachedEntity() {
    return sessionFactory.getCache().containsEntity(NonstrictEntity.class, ENTITY_ID);
  }

  private void persistEntity() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      session.persist(new NonstrictEntity(ENTITY_ID, NAME_BEFORE_WRITE));
      transaction.commit();
    }
  }

  /** Writer W: a real update in its own transaction, so the real eviction paths fire. */
  private void updateEntityInNewSession() {
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.beginTransaction();
      NonstrictEntity entity = session.get(NonstrictEntity.class, ENTITY_ID);
      assertNotNull(entity);
      entity.setName(NAME_AFTER_WRITE);
      transaction.commit();
    }
  }

  private static String loadName(Session session) {
    NonstrictEntity entity = session.get(NonstrictEntity.class, ENTITY_ID);
    assertNotNull(entity);
    return entity.getName();
  }

  /**
   * The guarded case: NONSTRICT_READ_WRITE on the entity and on its cached collection, the mapping
   * shape this whole change exists for.
   */
  @Entity
  @Table(name = "t5_nonstrict_entity")
  @Cacheable
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  public static class NonstrictEntity {

    @Id private Long id;

    @Column private String name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "parent_id")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<ReadWriteEntity> children = new HashSet<>();

    public NonstrictEntity() {}

    public NonstrictEntity(Long id, String name) {
      this.id = id;
      this.name = name;
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Set<ReadWriteEntity> getChildren() {
      return children;
    }
  }

  /**
   * The control: READ_WRITE in the same SessionFactory as the guarded mapping, so the guarding is
   * shown to follow the mapping rather than the region factory. It doubles as the element type of
   * the guarded collection, which is why one region of that collection's own SessionFactory is
   * guarded while the element region beside it is not.
   */
  @Entity
  @Table(name = "t5_read_write_entity")
  @Cacheable
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  public static class ReadWriteEntity {

    @Id private Long id;

    @Column private String name;

    public ReadWriteEntity() {}

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }
  }
}
