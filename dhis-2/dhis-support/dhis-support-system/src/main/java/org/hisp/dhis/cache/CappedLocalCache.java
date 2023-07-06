/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.cache;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hisp.dhis.cache.CacheInfo.CacheBurdenInfo;
import org.hisp.dhis.cache.CacheInfo.CacheCapInfo;
import org.hisp.dhis.cache.CacheInfo.CacheGroupInfo;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The {@link CappedLocalCache} is a multi-region cache that tries to estimate the memory usage of
 * cache entries and prevent all cache regions from together use over a configured cap limit.
 *
 * <p>The cap is in relation to the maximum heap memory available to the JVM.
 *
 * <p>Main configuration is the heap cap percentage. This is how much of the heap the cache is going
 * to use at most. One has to remember that this is based on estimated sizes of cached objects.
 *
 * <p>Secondary cap settings are the soft and hard caps which are in relation to the maximum memory
 * the cache is using.
 *
 * <p>When entries occupy memory beyond the hard cap the cache aggressively tries to free memory by
 * invalidating the entries it deems the most burden (the least useful).
 *
 * <p>When entries occupy memory beyond the soft cap the cache tries to free memory each time a new
 * entry is added by invalidating some entries deemed a burden.
 *
 * <p>The cache is organised by a map of regions, each having a map of entries. Both regions and
 * entries record metadata to distinguish entries that are a burden from those that are useful.
 *
 * <p>A watcher thread uses the metadata to compose a list of high burden entries in a regular
 * interval. This list is used in case memory should be freed after entries are inserted or overall
 * memory of the JVM is reaching its maximum.
 *
 * @author Jan Bernitt
 */
@Slf4j
@Component
public class CappedLocalCache {
  /** Only used to measure the {@link Sizeof} overhead of the {@link CacheEntry} itself. */
  public static final Object EMPTY = new CacheEntry<>(null, null, null, 0L, 0L, 0L);

  /**
   * A {@link CacheRegion} works like a {@link Cache} facade for the underlying {@link
   * CappedLocalCache} where all values share a single space.
   *
   * @param <V> type of values stored.
   */
  private static final class CacheRegion<V> implements Cache<V> {
    private final String region;

    private final V defaultValue;

    private final long defaultTtlInSeconds;

    private final ConcurrentMap<String, CacheEntry<V>> entries = new ConcurrentHashMap<>();

    private final LongConsumer sizeDeltaListener;

    private final Sizeof sizeof;

    private final long emptyEntrySize;

    private final AtomicLong totalRegionSize = new AtomicLong();

    private final AtomicLong hits = new AtomicLong();

    private final AtomicLong misses = new AtomicLong();

    CacheRegion(final CacheBuilder<V> builder, Sizeof sizeof, LongConsumer sizeDeltaListener) {
      this.region = builder.getRegion();
      log.info("Local capped cache instance created for region: '{}'", region);
      this.defaultValue = builder.getDefaultValue();
      this.defaultTtlInSeconds = builder.getExpiryInSeconds();
      this.sizeof = sizeof;
      this.emptyEntrySize = sizeof.sizeof(EMPTY);
      this.sizeDeltaListener = sizeDeltaListener;
    }

    long getHits() {
      return hits.get();
    }

    long getMisses() {
      return misses.get();
    }

    @Override
    public Optional<V> getIfPresent(String key) {
      return getOrDefault(key, value -> value);
    }

    @Override
    public Optional<V> get(String key) {
      return getOrDefault(key, value -> value == null ? defaultValue : value);
    }

    private Optional<V> getOrDefault(String key, UnaryOperator<V> value) {
      CacheEntry<V> entry = entries.get(key);
      if (entry == null) {
        misses.incrementAndGet();
        return Optional.empty();
      }
      if (entry.isExpired(currentTimeMillis())) {
        invalidate(entry, true);
        misses.incrementAndGet();
        return Optional.empty();
      }
      hits.incrementAndGet();
      return Optional.ofNullable(value.apply(entry.read()));
    }

    @Override
    public V get(String key, Function<String, V> fetcher) {
      if (null == fetcher) {
        throw new IllegalArgumentException("MappingFunction cannot be null");
      }
      CacheEntry<V> entry = entries.get(key);
      long now = currentTimeMillis();
      V value = entry == null || entry.isExpired(now) ? null : entry.read();
      if (value != null) {
        hits.incrementAndGet();
        return value;
      }
      misses.incrementAndGet();
      value = fetcher.apply(key);
      if (value == null && entry != null && !entry.isExpired(now)) {
        // still null, no entry update needed
        return defaultValue;
      }
      // need new entry...
      put(key, value);
      return value == null ? defaultValue : value;
    }

    @Override
    public Stream<V> getAll() {
      return entries.values().stream().map(CacheEntry::read);
    }

    @Override
    public Iterable<String> keys() {
      return entries.keySet();
    }

    @Override
    public void put(String key, V value) {
      put(key, value, defaultTtlInSeconds);
    }

    @Override
    public void put(String key, V value, long ttlInSeconds) {
      long entrySize = emptyEntrySize + sizeof.sizeof(key) + sizeof.sizeof(value);
      long now = currentTimeMillis();
      CacheEntry<V> oldEntry =
          entries.put(
              key,
              new CacheEntry<>(region, key, value, now, now + (ttlInSeconds * 1000L), entrySize));
      long sizeDelta = entrySize - (oldEntry == null ? 0L : oldEntry.size);
      totalRegionSize.addAndGet(sizeDelta);
      sizeDeltaListener.accept(sizeDelta);
    }

    @Override
    public boolean putIfAbsent(String key, V value) {
      long entrySize = emptyEntrySize + sizeof.sizeof(key) + sizeof.sizeof(value);
      long now = currentTimeMillis();
      CacheEntry<V> newEntry =
          new CacheEntry<>(region, key, value, now, now + (defaultTtlInSeconds * 1000L), entrySize);
      var oldEntry = entries.putIfAbsent(key, newEntry);
      long sizeDelta = entrySize - (oldEntry == null ? 0L : oldEntry.size);
      totalRegionSize.addAndGet(sizeDelta);
      sizeDeltaListener.accept(sizeDelta);
      return oldEntry != newEntry;
    }

    @Override
    public void invalidate(String key) {
      invalidate(entries.remove(key), false);
    }

    /**
     * @param entry the entry to invalidate
     * @param remove true, if the entry needs removing, false if the passed entry already was
     *     removed
     * @return true, if the entry wasn't already removed, else false
     */
    boolean invalidate(CacheEntry<?> entry, boolean remove) {
      if (entry == null) {
        return false; // already removed by another thread
      }
      if (remove && entries.remove(entry.key) != entry) {
        return false; // already removed
      }
      long sizeDelta = -entry.size;
      totalRegionSize.addAndGet(sizeDelta);
      sizeDeltaListener.accept(sizeDelta);
      return true;
    }

    @Override
    public void invalidateAll() {
      long sizeDelta = -totalRegionSize.get();
      entries.clear();
      totalRegionSize.set(0L);
      sizeDeltaListener.accept(sizeDelta);
    }

    @Override
    public CacheType getCacheType() {
      return CacheType.IN_MEMORY;
    }

    @Override
    public String toString() {
      return region
          + "["
          + CacheInfo.humanReadableSize(totalRegionSize.get())
          + " for "
          + entries.size()
          + " entries]";
    }
  }

  private static final class CacheEntry<V> {

    final String region;

    final String key;

    private final V value;

    /** System time when this entry was cached */
    final long created;

    /** System time when this entry expires */
    final long expires;

    /** Number of bytes this entry is estimated to use in memory */
    final long size;

    /** Number of read accesses since creation */
    private int reads;

    CacheEntry(String region, String key, V value, long created, long expires, long size) {
      this.region = region;
      this.key = key;
      this.value = value;
      this.created = created;
      this.expires = expires;
      this.size = size;
    }

    V read() {
      reads++;
      return value;
    }

    public boolean isExpired(long now) {
      return now >= expires;
    }

    /**
     * Is a number that expresses how much an entry contributes to memory usage. Higher is worse.
     * Lower is better.
     *
     * <p>We want to get rid of the entries with highest burden.
     *
     * <p>Burden increases the less a value is accessed and the closer it gets to the end of it
     * maximum lifespan - its expiry.
     *
     * @param now what to consider system time of now
     * @return burden this entry causes for memory
     */
    public long burden(long now) {
      if (isExpired(now)) {
        return Long.MAX_VALUE;
      }
      long avgAccessTime = (now - created) / max(1, reads);
      double avgReadsPerSec = 1000d / avgAccessTime;
      double lifespanLeftRatio = getRelativeBurden(expires - now, expires - created);
      return (long)
          (size / avgReadsPerSec / (lifespanLeftRatio == 0d ? 0.0001 : lifespanLeftRatio));
    }

    @Override
    public String toString() {
      return region + ":" + key + "[" + CacheInfo.humanReadableSize(size) + "]";
    }
  }

  private final ConcurrentMap<String, CacheRegion<?>> regions = new ConcurrentHashMap<>();

  private final Sizeof sizeof;

  private final AtomicLong totalSize = new AtomicLong();

  private final AtomicReference<Deque<CacheEntry<?>>> highBurdenEntries =
      new AtomicReference<>(new ConcurrentLinkedDeque<>());

  private final AtomicBoolean guardRunning = new AtomicBoolean();

  private final Runtime runtime;
  /*
   * Settings and Statistics
   */

  private final AtomicReference<CacheInfo> info = new AtomicReference<>();

  private volatile long highBurdenThreshold = Long.MAX_VALUE;

  /**
   * In reference to the JVM max heap that is the absolute cap for the memory used by this cache.
   * For example 50 means half the JVM heap is used max.
   */
  private volatile int capPercent;

  /** {@link #capPercent} of the {@link Runtime#maxMemory()}. */
  private volatile long capSize;

  /**
   * In reference to {@link #capSize}. If more than this percentage of the reference size is used
   * entries are freed aggressively.
   */
  private volatile int hardCapPercentage = 80;

  /** {@link #hardCapPercentage} of {@link #capSize} */
  private volatile long hardCapSize;

  /**
   * In reference to {@link #capSize}. If more than this percentage of the reference size is used
   * adding entries tries to compensate by freeing burden entries of the same or larger size.
   */
  private volatile int softCapPercentage = 60;

  /** {@link #softCapPercentage} of {@link #capSize} */
  private volatile long softCapSize;

  @Autowired
  public CappedLocalCache(DhisConfigurationProvider config) {
    this(
        new GenericSizeof(20L, Hibernate::unproxy),
        parseInt(config.getPropertyOrDefault(ConfigurationKey.SYSTEM_CACHE_CAP_PERCENTAGE, "50")));
  }

  public CappedLocalCache(Sizeof sizeof, int capPercent) {
    this.sizeof = sizeof;
    this.runtime = Runtime.getRuntime();
    setCapPercent(capPercent);
  }

  public CacheInfo getInfo() {
    return info.get();
  }

  public Set<String> getRegions() {
    return unmodifiableSet(regions.keySet());
  }

  public void setCapPercent(int capPercent) {
    this.capPercent = capPercent;
    this.capSize = Runtime.getRuntime().maxMemory() / 100 * capPercent;
    setHardCapPercentage(hardCapPercentage);
    setSoftCapPercentage(softCapPercentage);
  }

  public void setHardCapPercentage(int hardCapPercentage) {
    this.hardCapPercentage = hardCapPercentage;
    this.hardCapSize = capSize / 100 * hardCapPercentage;
  }

  public void setSoftCapPercentage(int softCapPercentage) {
    this.softCapPercentage = softCapPercentage;
    this.softCapSize = capSize / 100 * softCapPercentage;
  }

  public void invalidate() {
    regions.values().forEach(CacheRegion::invalidateAll);
  }

  public void invalidateRegion(String region) {
    CacheRegion<?> cacheRegion = regions.get(region);
    if (cacheRegion != null) {
      cacheRegion.invalidateAll();
    }
  }

  @SuppressWarnings("unchecked")
  public <V> Cache<V> createRegion(CacheBuilder<V> builder) {
    return (Cache<V>)
        regions.computeIfAbsent(
            builder.getRegion(), region -> new CacheRegion<>(builder, sizeof, this::sizeUpdate));
  }

  /**
   * We do start the thread lazily by intention so that it is only active if this cache is actually
   * used with at least one {@link CacheRegion}.
   */
  private void ensureGuardThreadRunningWhenNeeded() {
    if (totalSize.get() > 0L && capPercent > 0L && guardRunning.compareAndSet(false, true)) {
      log.info("Starting capped cache cap guard.");
      Thread watcher = new Thread(this::runCacheGuard);
      watcher.setDaemon(true);
      watcher.setName("Capped Local Cache Guard");
      watcher.start();
    }
  }

  private void sizeUpdate(long sizeDelta) {
    long newTotalSize = totalSize.addAndGet(sizeDelta);
    if (sizeDelta > 0L && newTotalSize > softCapSize) {
      free(sizeDelta); // try compensate
    }
    ensureGuardThreadRunningWhenNeeded();
  }

  private void freeProtectRuntime() {
    // remember: this can be off as `sizeof` is only an estimate
    long estimatedUsedSize = totalSize.get();
    if (estimatedUsedSize > hardCapSize) {
      long overflow = free(estimatedUsedSize - hardCapSize);
      if (overflow > 0L) {
        log.info("Cache overflew {}.", CacheInfo.humanReadableSize(overflow));
      }
    }

    // this is about actual memory
    // if there is little actual memory left cache should free entries
    long freeHeapSize = runtime.freeMemory();
    // less then 10% heap left?
    long tenthHeapSize = runtime.maxMemory() / 10;
    if (freeHeapSize < tenthHeapSize && estimatedUsedSize > tenthHeapSize) {
      free(tenthHeapSize);
    }
  }

  /**
   * @param freeSize number of bytes to try to free by invalidating high burden entries
   * @return the size that could not be freed
   */
  private long free(long freeSize) {
    if (freeSize <= 0L) {
      return 0L; // we freed memory :)
    }
    Deque<CacheEntry<?>> highBurdens = highBurdenEntries.get();
    if (highBurdens.isEmpty()) {
      return 0L;
    }
    long now = currentTimeMillis();
    List<CacheEntry<?>> secondQualityHighBurdens = new ArrayList<>();
    long sizeLeft = free(freeSize, highBurdens, secondQualityHighBurdens);
    // do we still want to free memory?
    if (sizeLeft <= 0L) {
      return 0;
    }
    // only sort if numbers aren't to high, otherwise we just take first
    if (secondQualityHighBurdens.size() < 500) {
      secondQualityHighBurdens.sort((a, b) -> Long.compare(b.burden(now), a.burden(now)));
    }
    long burdenThreshold = highBurdenThreshold;
    for (CacheEntry<?> e : secondQualityHighBurdens) {
      if (sizeLeft <= 0L) {
        return 0; // done
      }
      if (e.burden(now) > burdenThreshold && regions.get(e.region).invalidate(e, true)) {
        sizeLeft -= e.size;
      }
    }
    return freeSize - sizeLeft;
  }

  /**
   * Tries to free memory by invalidating
   *
   * @param freeSize number of bytes to try to free by invalidating high burden entries
   * @param highBurdens a list of candidates to invalidate
   * @param secondQualityHighBurdens a target (output) list of those entries in highBurdens list
   *     that were skipped so far
   * @return the number of bytes left to free after invalidating first quality entries in high
   *     burden list
   */
  private long free(
      long freeSize,
      Deque<CacheEntry<?>> highBurdens,
      List<CacheEntry<?>> secondQualityHighBurdens) {
    long now = currentTimeMillis();
    long sizeLeft = freeSize;
    long burdenThreshold = highBurdenThreshold;
    while (sizeLeft > 0L && !highBurdens.isEmpty()) {
      CacheEntry<?> e = highBurdens.removeFirst();
      if (e.burden(now) > burdenThreshold) {
        if (regions.get(e.region).invalidate(e, true)) {
          sizeLeft -= e.size;
        }
      } else {
        secondQualityHighBurdens.add(e);
      }
    }
    return sizeLeft;
  }

  private void runCacheGuard() {
    while (totalSize.get() > 0L) {
      try {
        long start = currentTimeMillis();
        long duration = currentTimeMillis() - start;
        updateHighBurdenState();
        freeProtectRuntime();
        long delay = 10_000L - duration;
        if (delay > 0) {
          waitFor(delay);
        }
      } catch (RuntimeException ex) {
        log.error("Cache guard threw exception", ex);
      }
    }
    guardRunning.compareAndSet(true, false);
  }

  private void updateHighBurdenState() {
    int totalEntryCount = 0;
    long totalBurden = 0L;
    long now = currentTimeMillis();
    List<CacheGroupInfo> regionsInfo = new ArrayList<>();
    for (CacheRegion<?> region : regions.values()) {
      int regionEntryCount = 0;
      long regionBurden = 0L;
      long regionSize = 0L;
      for (CacheEntry<?> e : region.entries.values()) {
        if (!e.isExpired(now)) {
          regionEntryCount++;
          regionBurden += e.burden(now);
          regionSize += e.size;
        }
      }
      regionsInfo.add(
          new CacheGroupInfo(
              region.region,
              region.entries.size(),
              region.getHits(),
              region.getMisses(),
              regionSize,
              getRelativeBurden(regionBurden, regionSize)));
      totalEntryCount += regionEntryCount;
      totalBurden += regionBurden;
    }

    long avgBurden = totalEntryCount == 0 ? 0L : totalBurden / totalEntryCount;
    // upper third entries are considered high burden
    long newHighBurdenThreshold = avgBurden * 2 / 3;

    Deque<CacheEntry<?>> newHighBurdenEntries = new ConcurrentLinkedDeque<>();

    CacheGroupInfo total =
        findHighBurdens(regionsInfo, newHighBurdenEntries, newHighBurdenThreshold, totalBurden);
    info.set(
        new CacheInfo(
            new CacheCapInfo(capPercent, softCapPercentage, hardCapPercentage),
            new CacheBurdenInfo(
                newHighBurdenEntries.size(),
                newHighBurdenEntries.stream().mapToLong(e -> e.size).sum(),
                getRelativeBurden(newHighBurdenThreshold, newHighBurdenEntries.size())),
            total,
            regionsInfo));

    highBurdenThreshold = newHighBurdenThreshold;
    highBurdenEntries.set(newHighBurdenEntries);
  }

  private CacheGroupInfo findHighBurdens(
      List<CacheGroupInfo> regionsInfo,
      Deque<CacheEntry<?>> newHighBurdenEntries,
      long newHighBurdenThreshold,
      long totalBurden) {
    // OBS! we want to process regions high => low burden
    regionsInfo.sort((a, b) -> Double.compare(b.getBurden(), a.getBurden()));
    for (CacheGroupInfo regionInfo : regionsInfo) {
      findHighBurdensInRegion(
          regions.get(regionInfo.getName()),
          regionInfo,
          newHighBurdenEntries,
          newHighBurdenThreshold);
    }
    long totalNonExpiredSize = regionsInfo.stream().mapToLong(CacheGroupInfo::getSize).sum();
    CacheGroupInfo total =
        new CacheGroupInfo(
            "total",
            regionsInfo.stream().mapToInt(CacheGroupInfo::getEntries).sum(),
            regionsInfo.stream().mapToLong(CacheGroupInfo::getHits).sum(),
            regionsInfo.stream().mapToLong(CacheGroupInfo::getMisses).sum(),
            totalNonExpiredSize,
            getRelativeBurden(totalBurden, totalNonExpiredSize));
    total.setHighBurdenEntries(
        regionsInfo.stream().mapToInt(CacheGroupInfo::getHighBurdenEntries).sum());
    return total;
  }

  private void findHighBurdensInRegion(
      CacheRegion<?> region,
      CacheGroupInfo info,
      Deque<CacheEntry<?>> newHighBurdenEntries,
      long currentHighBurdenThreshold) {
    long now = currentTimeMillis();
    int regionHighBurdenCount = 0;
    for (CacheEntry<?> e : region.entries.values()) {
      if (e.isExpired(now)) {
        region.invalidate(e, true);
      } else {
        long burden = e.burden(now);
        if (burden > currentHighBurdenThreshold) {
          if (newHighBurdenEntries.isEmpty()
              || burden >= newHighBurdenEntries.peekFirst().burden(now)) {
            newHighBurdenEntries.addFirst(e);
          } else {
            newHighBurdenEntries.addLast(e);
          }
          regionHighBurdenCount++;
        }
      }
    }
    info.setHighBurdenEntries(regionHighBurdenCount);
  }

  private static double getRelativeBurden(long burden, long size) {
    return round(size == 0 ? 0d : burden / (double) size);
  }

  private static double round(double value) {
    return Math.round(value * 100d) / 100d;
  }

  public static void waitFor(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
