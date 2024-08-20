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

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew Gizaw
 */
@Slf4j
@Repository("org.hisp.dhis.trackedentity.TrackedEntityStore")
public class HibernateTrackedEntityStore extends SoftDeleteHibernateObjectStore<TrackedEntity>
    implements TrackedEntityStore {

  // TODO too many arguments in constructor. This needs to be refactored.
  public HibernateTrackedEntityStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntity.class, aclService, false);
  }

  @Override
  public void updateTrackedEntityLastUpdated(
      Set<String> trackedEntityUIDs, Date lastUpdated, String infoSnapshot) {
    List<List<String>> uidsPartitions =
        Lists.partition(Lists.newArrayList(trackedEntityUIDs), 20000);

    uidsPartitions.stream()
        .filter(trackedEntities -> !trackedEntities.isEmpty())
        .forEach(
            trackedEntities ->
                getSession()
                    .getNamedQuery("updateTrackedEntitiesLastUpdated")
                    .setParameter("trackedEntities", trackedEntities)
                    .setParameter("lastUpdated", lastUpdated)
                    .setParameter("lastupdatedbyuserinfo", infoSnapshot)
                    .executeUpdate());
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<TrackedEntity>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected TrackedEntity postProcessObject(TrackedEntity trackedEntity) {
    return (trackedEntity == null || trackedEntity.isDeleted()) ? null : trackedEntity;
  }
}
