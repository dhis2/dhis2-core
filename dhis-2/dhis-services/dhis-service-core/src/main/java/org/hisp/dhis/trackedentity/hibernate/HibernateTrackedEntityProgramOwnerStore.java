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
package org.hisp.dhis.trackedentity.hibernate;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Ameen Mohamed
 */
@Repository("org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerStore")
public class HibernateTrackedEntityProgramOwnerStore
    extends HibernateGenericStore<TrackedEntityProgramOwner>
    implements TrackedEntityProgramOwnerStore {
  public HibernateTrackedEntityProgramOwnerStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher) {
    super(sessionFactory, jdbcTemplate, publisher, TrackedEntityProgramOwner.class, false);
  }

  @Override
  public TrackedEntityProgramOwner getTrackedEntityProgramOwner(long teiId, long programId) {
    Query<TrackedEntityProgramOwner> query =
        getQuery(
            "from TrackedEntityProgramOwner tepo where "
                + "tepo.trackedEntity.id= :teiId and "
                + "tepo.program.id= :programId");

    query.setParameter("teiId", teiId);
    query.setParameter("programId", programId);
    return query.uniqueResult();
  }

  @Override
  public List<TrackedEntityProgramOwner> getTrackedEntityProgramOwners(List<Long> teiIds) {
    String hql = "from TrackedEntityProgramOwner tepo where tepo.trackedEntity.id in (:teiIds)";
    Query<TrackedEntityProgramOwner> q = getQuery(hql);
    q.setParameterList("teiIds", teiIds);
    return q.list();
  }

  @Override
  public List<TrackedEntityProgramOwner> getTrackedEntityProgramOwners(
      List<Long> teiIds, long programId) {
    String hql =
        "from TrackedEntityProgramOwner tepo where tepo.trackedEntity.id in (:teiIds) and tepo.program.id=(:programId) ";
    Query<TrackedEntityProgramOwner> q = getQuery(hql);
    q.setParameterList("teiIds", teiIds);
    q.setParameter("programId", programId);
    return q.list();
  }

  @Override
  public List<TrackedEntityProgramOwnerOrgUnit> getTrackedEntityProgramOwnerOrgUnits(
      Set<Long> teiIds) {
    List<TrackedEntityProgramOwnerOrgUnit> trackedEntityProgramOwnerOrgUnits = new ArrayList<>();

    if (teiIds == null || teiIds.size() == 0) {
      return trackedEntityProgramOwnerOrgUnits;
    }

    Iterable<List<Long>> teiIdsPartitions = Iterables.partition(teiIds, 20000);

    String hql =
        "select new org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit( tepo.trackedEntity.uid, tepo.program.uid, tepo.organisationUnit) from TrackedEntityProgramOwner tepo where tepo.trackedEntity.id in (:teiIds)";

    Query<TrackedEntityProgramOwnerOrgUnit> q =
        getQuery(hql, TrackedEntityProgramOwnerOrgUnit.class);

    teiIdsPartitions.forEach(
        partition -> {
          q.setParameterList("teiIds", partition);
          trackedEntityProgramOwnerOrgUnits.addAll(q.list());
        });

    return trackedEntityProgramOwnerOrgUnits;
  }
}
