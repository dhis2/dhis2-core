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
package org.hisp.dhis.cache.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Deterministic interleaving tests for {@link GuardedEntityNonStrictReadWriteAccess} and {@link
 * GuardedCollectionNonStrictReadWriteAccess}. No threads and no timing: the schedules are executed
 * step by step in the test thread, which is exactly what makes the outcome an assertion about the
 * guard's ordering rules rather than about a lucky run.
 *
 * <p>Hibernate cache timestamps are scripted through a mocked {@link RegionFactory} handing out an
 * {@link AtomicLong}, and reader transactions get their caching timestamp from a mocked {@link
 * CacheTransactionSynchronization}. Storage is a {@link HashMap} behind a {@link
 * DomainDataStorageAccess} stub whose put can run a callback first, which is how a writer landing
 * in the middle of a reader's put is simulated without a race.
 *
 * <p>The guard is created by the test and injected, mirroring how a region creates one guard and
 * hands it to every access object it builds. Every test uses its own {@link EvictionGuardStats}
 * region name, because the stats registry is process wide and has no reset.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GuardedNonStrictInterleavingTest {
  private static final Object KEY = "Entity#1";
  private static final Object OTHER_KEY = "Entity#2";
  private static final String STALE = "value read before the write";
  private static final String FRESH = "value read after the write";
  private static final String WRITTEN = "value written";

  /**
   * Steps of an interleaving: {@code W1} is the writer's update (records the eviction and evicts),
   * {@code C} is the writer's post commit {@code unlockItem}, {@code P} is the reader's {@code
   * putFromLoad} carrying a transaction timestamp captured before {@code W1}.
   */
  enum Step {
    W1,
    C,
    P
  }

  /** One writer entry point, so every overridden eviction path can be asserted with one test. */
  private interface WriterPath {
    void run(
        GuardedEntityNonStrictReadWriteAccess access, SharedSessionContractImplementor session);
  }

  /** Source of Hibernate cache timestamps, shared by the region factory and the test sessions. */
  private final AtomicLong cacheClock = new AtomicLong(1000);

  private final MapStorage storage = new MapStorage();

  /** One guard per region, created here the way the region creates it in production. */
  private final EvictionGuard guard = new EvictionGuard();

  static Stream<Arguments> schedules() {
    return Stream.of(
        // put after both evicts: refused up front, the guard already knows about the write
        arguments(List.of(Step.W1, Step.C, Step.P), 1L, 0L),
        // put between the two evicts: still refused, the first evict was already recorded
        arguments(List.of(Step.W1, Step.P, Step.C), 1L, 0L),
        // put before the write: stored, then removed by the write's own evicts
        arguments(List.of(Step.P, Step.W1, Step.C), 0L, 1L));
  }

  @ParameterizedTest
  @MethodSource("schedules")
  void stalePutNeverSurvives(List<Step> schedule, long expectedRefused, long expectedStoredPuts) {
    String regionName =
        "t3-schedule-" + schedule.stream().map(Enum::name).collect(Collectors.joining("-"));
    GuardedEntityNonStrictReadWriteAccess access = entityAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    // the reader's transaction started before any of the writer's steps
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    for (Step step : schedule) {
      switch (step) {
        case W1 -> access.update(writer, KEY, WRITTEN, null, null);
        case C -> access.unlockItem(writer, KEY, null);
        case P -> access.putFromLoad(reader, KEY, STALE, null);
      }
    }

    assertFalse(storage.contains(KEY), "stale value survived schedule " + schedule);
    assertNull(storage.getFromCache(KEY, reader));
    assertEquals(expectedRefused, stats.getRefused());
    // a refused put is only refused: it never reaches the store, so it is not a stored put either
    assertEquals(expectedStoredPuts, stats.getStoredPuts());
  }

  @Test
  void freshPutAfterWriteIsAccepted() {
    String regionName = "t3-fresh-put";
    GuardedEntityNonStrictReadWriteAccess access = entityAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);

    access.update(writer, KEY, WRITTEN, null, null);
    access.unlockItem(writer, KEY, null);
    // a reader transaction started after the write reads the new row, so its put is not stale
    SharedSessionContractImplementor reader = sessionAt(cacheClock.incrementAndGet());

    assertTrue(access.putFromLoad(reader, KEY, FRESH, null));
    assertEquals(FRESH, storage.getFromCache(KEY, reader));
    assertEquals(0, stats.getRefused());
    assertEquals(0, stats.getSelfEvicted());
    assertEquals(1, stats.getStoredPuts());
  }

  @Test
  void writeLandingMidPutIsSelfEvicted() {
    String regionName = "t3-mid-put-race";
    GuardedEntityNonStrictReadWriteAccess access = entityAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());
    // the writer records and evicts after the reader's guard check but before its value is stored,
    // which is the one window the up front check cannot cover
    storage.beforeStore = () -> access.update(writer, KEY, WRITTEN, null, null);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null));

    assertFalse(storage.contains(KEY), "stale value stranded by a write landing mid put");
    assertEquals(1, stats.getSelfEvicted());
    // the up front check passed, so this must not also be counted as a refusal
    assertEquals(0, stats.getRefused());
    // nor as a stored put: the value did not stay, so the three outcomes stay mutually exclusive
    assertEquals(0, stats.getStoredPuts());
  }

  @Test
  void refusalAndSelfEvictionAreCountedPerRegion() {
    String regionName = "t3-counters";
    GuardedEntityNonStrictReadWriteAccess access = entityAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    // first key: the write is already recorded when the reader puts, so the put is refused up front
    access.update(writer, KEY, WRITTEN, null, null);
    assertFalse(access.putFromLoad(reader, KEY, STALE, null));

    // second key: the write lands between the reader's guard check and its storage put
    storage.beforeStore = () -> access.update(writer, OTHER_KEY, WRITTEN, null, null);
    assertFalse(access.putFromLoad(reader, OTHER_KEY, STALE, null));

    assertEquals(1, stats.getRefused());
    assertEquals(1, stats.getSelfEvicted());
    assertFalse(storage.contains(KEY));
    assertFalse(storage.contains(OTHER_KEY));
  }

  static Stream<Arguments> writerPaths() {
    return Stream.of(
        arguments(
            "update",
            (WriterPath) (access, session) -> access.update(session, KEY, WRITTEN, null, null)),
        // afterUpdate delegates to unlockItem in the superclass, so unlockItem is the guarded one
        // of the two. This entry pins that delegation.
        arguments(
            "afterUpdate",
            (WriterPath)
                (access, session) -> access.afterUpdate(session, KEY, WRITTEN, null, null, null)),
        arguments(
            "unlockItem", (WriterPath) (access, session) -> access.unlockItem(session, KEY, null)),
        arguments("remove", (WriterPath) (access, session) -> access.remove(session, KEY)),
        arguments("removeAll", (WriterPath) (access, session) -> access.removeAll(session)),
        arguments("evict", (WriterPath) (access, session) -> access.evict(KEY)),
        arguments("evictAll", (WriterPath) (access, session) -> access.evictAll()),
        // unlockRegion reaches storage through clearCache, not through evictAll, so it needs an
        // override of its own. Hibernate calls it post commit for bulk HQL update and delete
        // statements.
        arguments("unlockRegion", (WriterPath) (access, session) -> access.unlockRegion(null)),
        arguments("destroy", (WriterPath) (access, session) -> access.destroy()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("writerPaths")
  void everyEvictionPathBlocksOlderPuts(String name, WriterPath writerPath) {
    String regionName = "t3-writer-" + name;
    GuardedEntityNonStrictReadWriteAccess access = entityAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    writerPath.run(access, writer);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null), name + " left the door open");
    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getRefused());
  }

  @Test
  void fiveArgumentPutFromLoadIsGuardedThroughDelegation() {
    // pins an upstream assumption: the five argument overload of the superclass routes to the four
    // argument one virtually, so guarding the four argument one guards both. An upgrade that stops
    // delegating fails here instead of silently reopening the window.
    String regionName = "t3-five-arg-put";
    GuardedEntityNonStrictReadWriteAccess access = entityAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    access.update(writer, KEY, WRITTEN, null, null);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null, true));
    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getRefused());
  }

  @Test
  void collectionAccessRefusesStalePut() {
    String regionName = "t3-collection";
    GuardedCollectionNonStrictReadWriteAccess access = collectionAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    // a collection has no update path: Hibernate evicts it on transaction completion
    access.unlockItem(writer, KEY, null);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null));
    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getRefused());
  }

  @Test
  void collectionAccessSelfEvictsWriteLandingMidPut() {
    String regionName = "t3-collection-mid-put";
    GuardedCollectionNonStrictReadWriteAccess access = collectionAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());
    storage.beforeStore = () -> access.unlockItem(writer, KEY, null);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null));

    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getSelfEvicted());
  }

  /** One collection writer entry point, the collection twin of {@link WriterPath}. */
  private interface CollectionWriterPath {
    void run(
        GuardedCollectionNonStrictReadWriteAccess access, SharedSessionContractImplementor session);
  }

  static Stream<Arguments> collectionWriterPaths() {
    return Stream.of(
        arguments(
            "unlockItem",
            (CollectionWriterPath) (access, session) -> access.unlockItem(session, KEY, null)),
        arguments(
            "remove", (CollectionWriterPath) (access, session) -> access.remove(session, KEY)),
        arguments(
            "removeAll", (CollectionWriterPath) (access, session) -> access.removeAll(session)),
        arguments("evict", (CollectionWriterPath) (access, session) -> access.evict(KEY)),
        arguments("evictAll", (CollectionWriterPath) (access, session) -> access.evictAll()),
        arguments(
            "unlockRegion", (CollectionWriterPath) (access, session) -> access.unlockRegion(null)),
        arguments("destroy", (CollectionWriterPath) (access, session) -> access.destroy()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("collectionWriterPaths")
  void everyCollectionEvictionPathBlocksOlderPuts(String name, CollectionWriterPath writerPath) {
    String regionName = "t3-collection-writer-" + name;
    GuardedCollectionNonStrictReadWriteAccess access = collectionAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    writerPath.run(access, writer);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null), name + " left the door open");
    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getRefused());
  }

  @Test
  void collectionFiveArgumentPutFromLoadIsGuardedThroughDelegation() {
    String regionName = "t3-collection-five-arg-put";
    GuardedCollectionNonStrictReadWriteAccess access = collectionAccess(regionName);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    access.unlockItem(writer, KEY, null);

    assertFalse(access.putFromLoad(reader, KEY, STALE, null, true));
    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getRefused());
  }

  @Test
  void aMissingGuardFailsAtConstruction() {
    // a Task 4 wiring mistake must surface when the region builds the access, not on the first
    // write
    DomainDataRegion region = region("t3-null-guard");

    assertThrows(
        NullPointerException.class,
        () ->
            new GuardedEntityNonStrictReadWriteAccess(
                region,
                mock(CacheKeysFactory.class),
                storage,
                mock(EntityDataCachingConfig.class),
                null));
    assertThrows(
        NullPointerException.class,
        () ->
            new GuardedCollectionNonStrictReadWriteAccess(
                region,
                mock(CacheKeysFactory.class),
                storage,
                mock(CollectionDataCachingConfig.class),
                null));
  }

  @Test
  void regionClearThroughOneAccessBarsPutThroughSibling() {
    // two entity types sharing one region: one region, one storage, one guard, two access objects
    String regionName = "t3-shared-region";
    DomainDataRegion sharedRegion = region(regionName);
    GuardedEntityNonStrictReadWriteAccess clearedThrough = entityAccess(sharedRegion);
    GuardedEntityNonStrictReadWriteAccess sibling = entityAccess(sharedRegion);
    EvictionGuardStats stats = EvictionGuardStats.forRegion(regionName);
    SharedSessionContractImplementor writer = mock(SharedSessionContractImplementor.class);
    SharedSessionContractImplementor reader = sessionAt(cacheClock.get());

    // the clear wipes the shared storage, the sibling's keys included
    clearedThrough.removeAll(writer);

    assertFalse(sibling.putFromLoad(reader, KEY, STALE, null));
    assertFalse(storage.contains(KEY));
    assertEquals(1, stats.getRefused());
  }

  private GuardedEntityNonStrictReadWriteAccess entityAccess(String regionName) {
    return entityAccess(region(regionName));
  }

  private GuardedEntityNonStrictReadWriteAccess entityAccess(DomainDataRegion region) {
    return new GuardedEntityNonStrictReadWriteAccess(
        region, mock(CacheKeysFactory.class), storage, mock(EntityDataCachingConfig.class), guard);
  }

  private GuardedCollectionNonStrictReadWriteAccess collectionAccess(String regionName) {
    return new GuardedCollectionNonStrictReadWriteAccess(
        region(regionName),
        mock(CacheKeysFactory.class),
        storage,
        mock(CollectionDataCachingConfig.class),
        guard);
  }

  private DomainDataRegion region(String regionName) {
    RegionFactory regionFactory = mock(RegionFactory.class);
    when(regionFactory.nextTimestamp()).thenAnswer(invocation -> cacheClock.incrementAndGet());
    DomainDataRegion region = mock(DomainDataRegion.class);
    when(region.getName()).thenReturn(regionName);
    when(region.getRegionFactory()).thenReturn(regionFactory);
    return region;
  }

  /** A session whose transaction started at the given Hibernate cache timestamp. */
  private static SharedSessionContractImplementor sessionAt(long cachingTimestamp) {
    CacheTransactionSynchronization synchronization = mock(CacheTransactionSynchronization.class);
    when(synchronization.getCachingTimestamp()).thenReturn(cachingTimestamp);
    SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
    when(session.getCacheTransactionSynchronization()).thenReturn(synchronization);
    return session;
  }

  /** Cache provider replaced by a {@link HashMap}, single threaded on purpose. */
  private static final class MapStorage implements DomainDataStorageAccess {
    private final Map<Object, Object> data = new HashMap<>();

    /** Runs before a loaded value is stored, which is how a mid put write is scripted. */
    Runnable beforeStore = () -> {};

    @Override
    public void putFromLoad(Object key, Object value, SharedSessionContractImplementor session) {
      beforeStore.run();
      putIntoCache(key, value, session);
    }

    @Override
    public Object getFromCache(Object key, SharedSessionContractImplementor session) {
      return data.get(key);
    }

    @Override
    public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
      data.put(key, value);
    }

    @Override
    public boolean contains(Object key) {
      return data.containsKey(key);
    }

    @Override
    public void evictData() {
      data.clear();
    }

    @Override
    public void evictData(Object key) {
      data.remove(key);
    }

    @Override
    public void release() {
      data.clear();
    }
  }
}
