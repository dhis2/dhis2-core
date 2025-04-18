/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.hibernate;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookStore;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Repository("org.hisp.dhis.analytics.AnalyticsTableHookStore")
public class HibernateAnalyticsTableHookStore
    extends HibernateIdentifiableObjectStore<AnalyticsTableHook>
    implements AnalyticsTableHookStore {
  public HibernateAnalyticsTableHookStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, AnalyticsTableHook.class, aclService, false);
  }

  @Override
  public List<AnalyticsTableHook> getByPhase(AnalyticsTablePhase phase) {

    return getQuery("from AnalyticsTableHook h where h.phase = :phase")
        .setParameter("phase", phase)
        .getResultList();
  }

  @Override
  public List<AnalyticsTableHook> getByPhaseAndResourceTableType(
      AnalyticsTablePhase phase, ResourceTableType resourceTableType) {
    return getQuery(
            "from AnalyticsTableHook h where h.phase = :phase and h.resourceTableType = :resourceTableType")
        .setParameter("phase", phase)
        .setParameter("resourceTableType", resourceTableType)
        .getResultList();
  }

  @Override
  public List<AnalyticsTableHook> getByPhaseAndAnalyticsTableType(
      AnalyticsTablePhase phase, AnalyticsTableType analyticsTableType) {
    return getQuery(
            "from AnalyticsTableHook h where h.phase = :phase and h.analyticsTableType = :analyticsTableType")
        .setParameter("phase", phase)
        .setParameter("analyticsTableType", analyticsTableType)
        .getResultList();
  }

  @Override
  public void executeAnalyticsTableSqlHooks(List<AnalyticsTableHook> hooks) {
    for (AnalyticsTableHook hook : hooks) {
      log.info("Executing analytics table hook: '{}', '{}'", hook.getUid(), hook.getName());
      jdbcTemplate.execute(hook.getSql());
    }
  }
}
