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

import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory;
import org.hibernate.cache.spi.DomainDataRegion;

/**
 * Ehcache 2 region factory that builds {@link GuardedDomainDataRegion} instead of Hibernate's own
 * region, so NONSTRICT_READ_WRITE entities and collections refuse stale late puts. See {@link
 * GuardedEntityNonStrictReadWriteAccess} for the race and the ordering rules, {@link EvictionGuard}
 * for the bookkeeping.
 *
 * <p>2.41 still runs {@code hibernate-ehcache} / Ehcache 2 rather than master's JCache stack, so
 * this is the 2.41 counterpart of {@code GuardedJCacheRegionFactory}.
 *
 * <p>Hibernate instantiates this class reflectively from {@code
 * hibernate.cache.region.factory_class}, so it must stay public with a public no-argument
 * constructor. Only the domain data region build is replaced; query results and timestamps regions
 * stay inherited.
 *
 * @author Morten Svanæs &lt;msvanaes@dhis2.org&gt;
 */
public class GuardedEhcacheRegionFactory extends EhcacheRegionFactory {

  /**
   * Mirrors {@code EhcacheRegionFactory.buildDomainDataRegion} with the region type swapped. {@code
   * getImplicitCacheKeysFactory()} returns exactly the keys factory the superclass passes to its
   * own region, and the {@code verifyStarted()} call is the guard {@code RegionFactoryTemplate}
   * puts in front of every region build.
   */
  @Override
  public DomainDataRegion buildDomainDataRegion(
      DomainDataRegionConfig regionConfig, DomainDataRegionBuildingContext buildingContext) {
    verifyStarted();
    return new GuardedDomainDataRegion(
        regionConfig,
        this,
        createDomainDataStorageAccess(regionConfig, buildingContext),
        getImplicitCacheKeysFactory(),
        buildingContext);
  }
}
