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

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.jcache.internal.JCacheDomainDataRegionImpl;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;

/**
 * JCache domain data region whose NONSTRICT_READ_WRITE entity and collection accesses are the
 * guarded ones, {@link GuardedEntityNonStrictReadWriteAccess} and {@link
 * GuardedCollectionNonStrictReadWriteAccess}. Every other access type is left exactly as Hibernate
 * builds it.
 *
 * <p>Extends {@link JCacheDomainDataRegionImpl} rather than {@code DomainDataRegionTemplate}
 * directly, because that is the region {@code JCacheRegionFactory} builds itself: it adds the
 * TRANSACTIONAL access implementations of {@code DomainDataRegionImpl} plus the non standard access
 * type warning. Extending the plain template instead would silently turn a TRANSACTIONAL mapping
 * from a warning into an {@code UnsupportedOperationException}.
 *
 * <p>Natural id access is deliberately not overridden: nothing in the NONSTRICT_READ_WRITE bucket
 * of this code base uses natural id caching, and a guarded natural id access with no caller would
 * be untested code.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GuardedDomainDataRegion extends JCacheDomainDataRegionImpl {

  /**
   * One guard per region instance, shared by every access object this region generates, because
   * sibling accesses of one region share one storage: a clear recorded through one of them must bar
   * stale puts arriving through any of the others.
   *
   * <p>The scope is the region instance and not the storage, which is worth stating because the two
   * are not the same thing: a JCache cache is identified by its name, so two region instances built
   * over one CacheManager URI would share storage while holding separate guards. DHIS2 builds one
   * SessionFactory, and closing a SessionFactory closes its CacheManager with it, so in practice a
   * guard and the storage it protects are created and discarded together.
   *
   * <p>Not assigned in the constructor, and a field initializer is equally forbidden here: both run
   * only after {@code DomainDataRegionTemplate}'s constructor has already generated every access
   * object through {@code completeInstantiation}, so either one would hand out accesses built with
   * a null guard and then overwrite the field behind them. Created on first use instead. The
   * unsynchronized lazy init is safe because Hibernate builds a region entirely on the thread that
   * calls {@code buildDomainDataRegion} during SessionFactory bootstrap, and no access object of
   * this region can be reached by another thread before that build has returned.
   */
  private EvictionGuard guard;

  public GuardedDomainDataRegion(
      DomainDataRegionConfig regionConfig,
      RegionFactoryTemplate regionFactory,
      DomainDataStorageAccess storageAccess,
      CacheKeysFactory defaultKeysFactory,
      DomainDataRegionBuildingContext buildingContext) {
    super(regionConfig, regionFactory, storageAccess, defaultKeysFactory, buildingContext);
  }

  @Override
  protected EntityDataAccess generateNonStrictReadWriteEntityAccess(
      EntityDataCachingConfig accessConfig) {
    return new GuardedEntityNonStrictReadWriteAccess(
        this, getEffectiveKeysFactory(), getCacheStorageAccess(), accessConfig, guard());
  }

  @Override
  public CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig accessConfig) {
    // the collection counterpart of generateNonStrictReadWriteEntityAccess is private in the
    // template, so this switch is the only interception point. The configured access type is the
    // one to test: the collection access object built for a NONSTRICT_READ_WRITE mapping reports
    // READ_WRITE from its own getAccessType() in Hibernate 5.6.
    if (accessConfig.getAccessType() == AccessType.NONSTRICT_READ_WRITE) {
      return new GuardedCollectionNonStrictReadWriteAccess(
          this, getEffectiveKeysFactory(), getCacheStorageAccess(), accessConfig, guard());
    }
    return super.generateCollectionAccess(accessConfig);
  }

  private EvictionGuard guard() {
    if (guard == null) {
      guard = new EvictionGuard();
    }
    return guard;
  }
}
