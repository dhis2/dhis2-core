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

import java.util.Objects;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.CollectionNonStrictReadWriteAccess;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * NONSTRICT_READ_WRITE collection access that refuses stale late puts, the collection twin of
 * {@link GuardedEntityNonStrictReadWriteAccess}. Plain NONSTRICT_READ_WRITE evicts the collection
 * when its owning transaction completes and then accepts any {@code putFromLoad} that arrives
 * afterwards, so a reader that loaded the collection before the write and stores it after the write
 * strands the pre write element list in the L2 cache until something else evicts that key. This
 * subclass closes that window with two rules. Writers record the eviction in an {@link
 * EvictionGuard} before they evict storage, never after, so a recorded eviction is always at least
 * as old as the storage change it describes. Readers check the guard, put, then check again and
 * remove their own entry if the second check fails, so a write that landed between the check and
 * the put cannot leave the reader's value behind.
 *
 * <p>The guard only protects a put whose eviction was recorded before that put's guard check, which
 * is exactly the ordering the two rules above supply: record before evict on the writer side means
 * an eviction that is visible in storage is already visible in the guard, and the check then put
 * then recheck loop on the reader side means a put that slipped past the first check is caught by
 * the second one. Any future caller that evicts region storage directly, without recording into the
 * guard first, breaks this ordering and reopens the window, which is why every path of the
 * superclass that reaches storage is overridden here rather than only the ones current DHIS2 code
 * happens to call. Two of them do not route through {@code evictAll} and are easy to miss: {@code
 * unlockRegion}, which the superclass sends straight to {@code clearCache}, and {@code destroy}. A
 * collection has no update or insert entry point at all: Hibernate signals a changed collection
 * through {@code unlockItem} and a deleted one through {@code remove}, both of which are guarded.
 *
 * <p>The guard is supplied by the caller and its scope is the region, not this access object. Every
 * access object built over one {@link DomainDataRegion} must be given the same guard instance,
 * because sibling access objects of a shared region share one storage: a region wide clear recorded
 * through one of them has to bar stale puts arriving through any of the others, and it only does so
 * if they all consult the same guard.
 *
 * <p>The design is not novel, only missing from Hibernate's own NONSTRICT implementation.
 * Infinispan carries the same rule in its NonStrictAccessDelegate and PutFromLoadValidator, which
 * refuse a putFromLoad from a transaction that started before the last invalidation. Hibernate's
 * UpdateTimestampsCache applies the same timestamp comparison to query result caching, and
 * READ_WRITE achieves the same effect with a SoftLock whose unlock timestamp bars older puts. This
 * class buys that guarantee for NONSTRICT_READ_WRITE without READ_WRITE's per key locking cost.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GuardedCollectionNonStrictReadWriteAccess extends CollectionNonStrictReadWriteAccess {
  private final EvictionGuard guard;
  private final EvictionGuardStats stats;

  public GuardedCollectionNonStrictReadWriteAccess(
      DomainDataRegion region,
      CacheKeysFactory keysFactory,
      DomainDataStorageAccess storageAccess,
      CollectionDataCachingConfig config,
      EvictionGuard guard) {
    super(region, keysFactory, storageAccess, config);
    this.guard = Objects.requireNonNull(guard, "guard");
    this.stats = EvictionGuardStats.forRegion(region.getName());
  }

  @Override
  public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
    // record before evicting, so a concurrent put can never see the eviction without the record
    guard.recordEviction(key, nextTimestamp());
    super.unlockItem(session, key, lock);
  }

  @Override
  public void remove(SharedSessionContractImplementor session, Object key) {
    // record before evicting
    guard.recordEviction(key, nextTimestamp());
    super.remove(session, key);
  }

  @Override
  public void removeAll(SharedSessionContractImplementor session) {
    // record before clearing, same ordering rule applied to the whole region
    guard.recordClearAll(nextTimestamp());
    super.removeAll(session);
  }

  @Override
  public void evict(Object key) {
    // record before evicting
    guard.recordEviction(key, nextTimestamp());
    super.evict(key);
  }

  @Override
  public void evictAll() {
    // record before clearing
    guard.recordClearAll(nextTimestamp());
    super.evictAll();
  }

  @Override
  public void unlockRegion(SoftLock lock) {
    // the superclass routes this to clearCache, which wipes storage without passing evictAll,
    // so the record has to happen here. Reached after a bulk HQL update or delete commits.
    guard.recordClearAll(nextTimestamp());
    super.unlockRegion(lock);
  }

  @Override
  public void destroy() {
    // releasing the storage also drops its content, so record before it happens
    guard.recordClearAll(nextTimestamp());
    super.destroy();
  }

  /**
   * The five argument {@code putFromLoad} of the superclass delegates to this one, so guarding this
   * overload guards both.
   *
   * <p>The take back on a failed re-check is a blind delete: it removes whatever sits under the
   * key, which may be a fresher value that another reader stored between this store and this
   * re-check. The cost of that is one extra cache miss, never staleness, so an unconditional delete
   * is a better trade than reading the entry back to compare it.
   */
  @Override
  public boolean putFromLoad(
      SharedSessionContractImplementor session, Object key, Object value, Object version) {
    long txTimestamp = session.getCacheTransactionSynchronization().getCachingTimestamp();
    if (!guard.isPutAllowed(key, txTimestamp)) {
      stats.countRefused();
      return false;
    }
    boolean stored = super.putFromLoad(session, key, value, version);
    if (!stored) {
      return false;
    }
    // Re-validate: a writer may have recorded and evicted between our check and our put.
    // Writer order is record THEN evict, so if its evict raced past our put, the record
    // is already visible here and we remove our own stale entry.
    if (!guard.isPutAllowed(key, txTimestamp)) {
      getStorageAccess().evictData(key);
      stats.countSelfEvicted();
      return false;
    }
    stats.countStoredPut();
    return true;
  }

  private long nextTimestamp() {
    return getRegion().getRegionFactory().nextTimestamp();
  }
}
