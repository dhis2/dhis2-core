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

import java.util.Objects;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.CollectionNonStrictReadWriteAccess;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * NONSTRICT_READ_WRITE collection access that refuses stale late puts: the collection twin of
 * {@link GuardedEntityNonStrictReadWriteAccess}, which states the two ordering rules, why every
 * storage-reaching superclass path is overridden, the shared-guard-per-region requirement and the
 * precedent. All of it applies here unchanged.
 *
 * <p>Collection specifics: there is no update or insert entry point at all. Hibernate signals a
 * changed collection through {@code unlockItem} and a deleted one through {@code remove}, both
 * guarded.
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
