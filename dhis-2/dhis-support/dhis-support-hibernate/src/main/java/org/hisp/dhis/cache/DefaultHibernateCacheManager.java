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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.springframework.context.event.EventListener;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class DefaultHibernateCacheManager implements HibernateCacheManager {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private SessionFactory sessionFactory;

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  // -------------------------------------------------------------------------
  // HibernateCacheManager implementation
  // -------------------------------------------------------------------------

  @Override
  public void clearObjectCache() {
    sessionFactory.getCache().evictEntityData();
    sessionFactory.getCache().evictCollectionData();
  }

  @Override
  public void clearQueryCache() {
    sessionFactory.getCache().evictDefaultQueryRegion();
    sessionFactory.getCache().evictQueryRegions();
  }

  @Override
  public void clearCache() {
    clearObjectCache();
    clearQueryCache();

    log.info("Hibernate caches cleared");
  }

  @Override
  @EventListener
  public void handleApplicationCachesCleared(ApplicationCacheClearedEvent event) {
    clearCache();
  }

  @Override
  public Statistics getStatistics() {
    return sessionFactory.getStatistics();
  }
}
