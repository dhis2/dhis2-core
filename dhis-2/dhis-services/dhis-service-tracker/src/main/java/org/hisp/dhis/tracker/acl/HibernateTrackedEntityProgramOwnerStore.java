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
package org.hisp.dhis.tracker.acl;

import static org.hisp.dhis.tracker.acl.OwnershipCacheUtils.getOwnershipCacheKey;

import com.google.common.collect.Iterables;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.query.Query;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Ameen Mohamed
 */
@Repository("org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerStore")
public class HibernateTrackedEntityProgramOwnerStore
    extends HibernateGenericStore<TrackedEntityProgramOwner>
    implements TrackedEntityProgramOwnerStore {

  private final Cache<OrganisationUnit> ownerCache;

  public HibernateTrackedEntityProgramOwnerStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CacheProvider cacheProvider) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntityProgramOwner.class, false);
    this.ownerCache = cacheProvider.createProgramOwnerCache();
  }

  @Override
  public TrackedEntityProgramOwner getTrackedEntityProgramOwner(TrackedEntity te, Program program) {
    Query<TrackedEntityProgramOwner> query =
        getQuery(
            "from TrackedEntityProgramOwner tepo where "
                + "tepo.trackedEntity.uid= :teUid and "
                + "tepo.program.id= :programId");

    query.setParameter("teUid", te.getUid());
    query.setParameter("programId", program.getId());
    return query.uniqueResult();
  }

  @Override
  public List<TrackedEntityProgramOwnerOrgUnit> getTrackedEntityProgramOwnerOrgUnits(
      Set<Long> teIds) {
    List<TrackedEntityProgramOwnerOrgUnit> trackedEntityProgramOwnerOrgUnits = new ArrayList<>();

    if (teIds == null || teIds.isEmpty()) {
      return trackedEntityProgramOwnerOrgUnits;
    }

    Iterable<List<Long>> teIdsPartitions = Iterables.partition(teIds, 20000);

    String hql =
        "select new org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit( tepo.trackedEntity.uid, tepo.program.uid, tepo.organisationUnit) from TrackedEntityProgramOwner tepo where tepo.trackedEntity.id in (:teIds)";

    Query<TrackedEntityProgramOwnerOrgUnit> q =
        getQuery(hql, TrackedEntityProgramOwnerOrgUnit.class);

    teIdsPartitions.forEach(
        partition -> {
          q.setParameterList("teIds", partition);
          trackedEntityProgramOwnerOrgUnits.addAll(q.list());
        });

    return trackedEntityProgramOwnerOrgUnits;
  }

  @Override
  public void save(@NotNull TrackedEntityProgramOwner trackedEntityProgramOwner) {
    super.save(trackedEntityProgramOwner);
    ownerCache.invalidate(
        getOwnershipCacheKey(
            trackedEntityProgramOwner.getTrackedEntity(), trackedEntityProgramOwner.getProgram()));
  }

  @Override
  public void update(@NotNull TrackedEntityProgramOwner trackedEntityProgramOwner) {
    super.update(trackedEntityProgramOwner);
    ownerCache.invalidate(
        getOwnershipCacheKey(
            trackedEntityProgramOwner.getTrackedEntity(), trackedEntityProgramOwner.getProgram()));
  }
}
