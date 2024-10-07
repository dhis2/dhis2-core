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
package org.hisp.dhis.tracker.export.trackedentity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityAttributeValueChangeLogStore")
public class HibernateTrackedEntityAttributeValueChangeLogStore
    implements TrackedEntityAttributeValueChangeLogStore {
  private EntityManager entityManager;
  private Session session;

  public HibernateTrackedEntityAttributeValueChangeLogStore(EntityManager entityManager) {
    this.entityManager = entityManager;
    this.session = entityManager.unwrap(Session.class);
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  public void addTrackedEntityAttributeValueChangeLog(
      TrackedEntityAttributeValueChangeLog attributeValueChangeLog) {
    session.save(attributeValueChangeLog);
  }

  @Override
  public List<TrackedEntityAttributeValueChangeLog> getTrackedEntityAttributeValueChangeLogs(
      TrackedEntityAttributeValueChangeLogQueryParams params) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<TrackedEntityAttributeValueChangeLog> criteria =
        builder.createQuery(TrackedEntityAttributeValueChangeLog.class);

    Root<TrackedEntityAttributeValueChangeLog> root =
        criteria.from(TrackedEntityAttributeValueChangeLog.class);

    List<Predicate> predicates = getTrackedEntityAttributeValueCriteria(root, params);

    criteria.where(predicates.toArray(new Predicate[0])).orderBy(builder.desc(root.get("created")));

    TypedQuery<TrackedEntityAttributeValueChangeLog> query = entityManager.createQuery(criteria);

    if (params.hasPager()) {
      query
          .setFirstResult(params.getPager().getOffset())
          .setMaxResults(params.getPager().getPageSize());
    }

    return query.getResultList();
  }

  @Override
  public int countTrackedEntityAttributeValueChangeLogs(
      TrackedEntityAttributeValueChangeLogQueryParams params) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> query = builder.createQuery(Long.class);

    Root<TrackedEntityAttributeValueChangeLog> root =
        query.from(TrackedEntityAttributeValueChangeLog.class);

    List<Predicate> predicates = getTrackedEntityAttributeValueCriteria(root, params);

    query.select(builder.countDistinct(root.get("id"))).where(predicates.toArray(new Predicate[0]));

    return (entityManager.createQuery(query).getSingleResult()).intValue();
  }

  @Override
  public void deleteTrackedEntityAttributeValueChangeLogs(TrackedEntity trackedEntity) {
    Query<?> query =
        session.createQuery(
            "delete TrackedEntityAttributeValueChangeLog where trackedEntity = :trackedEntity");
    query.setParameter("trackedEntity", trackedEntity);
    query.executeUpdate();
  }

  private List<Predicate> getTrackedEntityAttributeValueCriteria(
      Root<TrackedEntityAttributeValueChangeLog> root,
      TrackedEntityAttributeValueChangeLogQueryParams params) {
    List<Predicate> predicates = new ArrayList<>();

    if (!params.getTrackedEntityAttributes().isEmpty()) {
      predicates.add(root.get("attribute").in(params.getTrackedEntityAttributes()));
    }

    if (!params.getTrackedEntities().isEmpty()) {
      predicates.add(root.get("trackedEntity").in(params.getTrackedEntities()));
    }

    if (!params.getAuditTypes().isEmpty()) {
      predicates.add(root.get("auditType").in(params.getAuditTypes()));
    }

    return predicates;
  }
}
