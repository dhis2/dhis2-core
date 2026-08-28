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

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;

/**
 * Domain data region whose NONSTRICT_READ_WRITE entity and collection accesses are {@link
 * GuardedEntityNonStrictReadWriteAccess} and {@link GuardedCollectionNonStrictReadWriteAccess}.
 * Every other access type is left exactly as Hibernate builds it.
 *
 * <p>Extends {@link DomainDataRegionImpl}, the region {@code EhcacheRegionFactory} builds itself.
 * Natural-id access is not overridden: nothing in the NONSTRICT_READ_WRITE bucket uses it, and a
 * guarded access with no caller would be untested code.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GuardedDomainDataRegion extends DomainDataRegionImpl {

  /**
   * One guard per region instance, shared by every access object the region generates: sibling
   * accesses share one storage, so a clear recorded through one must bar stale puts arriving
   * through the others. DHIS2 builds one SessionFactory, and closing it closes its CacheManager, so
   * a guard and the storage it protects live and die together.
   *
   * <p>Created lazily on first use. Constructor assignment and a field initializer are both
   * forbidden here: they run only after the superclass constructor has already generated every
   * access object through {@code completeInstantiation}, which would hand out accesses holding a
   * null guard. The unsynchronized lazy init is safe because Hibernate builds a region entirely on
   * the bootstrap thread, and no access object is reachable by another thread before the build
   * returns.
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
