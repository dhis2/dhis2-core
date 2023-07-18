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
package org.hisp.dhis.trackedentityattributevalue.hibernate;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditQueryParams;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditStore;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository("org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditStore")
public class HibernateTrackedEntityAttributeValueAuditStore
    implements TrackedEntityAttributeValueAuditStore {
  private SessionFactory sessionFactory;

  public HibernateTrackedEntityAttributeValueAuditStore(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  public void addTrackedEntityAttributeValueAudit(
      TrackedEntityAttributeValueAudit trackedEntityAttributeValueAudit) {
    Session session = sessionFactory.getCurrentSession();
    session.save(trackedEntityAttributeValueAudit);
  }

  @Override
  public List<TrackedEntityAttributeValueAudit> getTrackedEntityAttributeValueAudits(
      TrackedEntityAttributeValueAuditQueryParams params) {
    CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();

    CriteriaQuery<TrackedEntityAttributeValueAudit> criteria =
        builder.createQuery(TrackedEntityAttributeValueAudit.class);

    Root<TrackedEntityAttributeValueAudit> root =
        criteria.from(TrackedEntityAttributeValueAudit.class);

    List<Predicate> predicates = getTrackedEntityAttributeValueAuditCriteria(root, params);

    criteria.where(predicates.toArray(new Predicate[0])).orderBy(builder.desc(root.get("created")));

    Query<TrackedEntityAttributeValueAudit> query =
        sessionFactory.getCurrentSession().createQuery(criteria);

    if (params.hasPager()) {
      query
          .setFirstResult(params.getPager().getOffset())
          .setMaxResults(params.getPager().getPageSize());
    }

    return query.getResultList();
  }

  @Override
  public int countTrackedEntityAttributeValueAudits(
      TrackedEntityAttributeValueAuditQueryParams params) {
    CriteriaBuilder builder = sessionFactory.getCurrentSession().getCriteriaBuilder();

    CriteriaQuery<Long> query = builder.createQuery(Long.class);

    Root<TrackedEntityAttributeValueAudit> root =
        query.from(TrackedEntityAttributeValueAudit.class);

    List<Predicate> predicates = getTrackedEntityAttributeValueAuditCriteria(root, params);

    query.select(builder.countDistinct(root.get("id"))).where(predicates.toArray(new Predicate[0]));

    return (sessionFactory.getCurrentSession().createQuery(query).uniqueResult()).intValue();
  }

  @Override
  public void deleteTrackedEntityAttributeValueAudits(TrackedEntityInstance entityInstance) {
    Session session = sessionFactory.getCurrentSession();
    Query<?> query =
        session.createQuery(
            "delete TrackedEntityAttributeValueAudit where entityInstance = :entityInstance");
    query.setParameter("entityInstance", entityInstance);
    query.executeUpdate();
  }

  private List<Predicate> getTrackedEntityAttributeValueAuditCriteria(
      Root<TrackedEntityAttributeValueAudit> root,
      TrackedEntityAttributeValueAuditQueryParams params) {
    List<Predicate> predicates = new ArrayList<>();

    if (!params.getTrackedEntityAttributes().isEmpty()) {
      predicates.add(root.get("attribute").in(params.getTrackedEntityAttributes()));
    }

    if (!params.getTrackedEntityInstances().isEmpty()) {
      predicates.add(root.get("entityInstance").in(params.getTrackedEntityInstances()));
    }

    if (!params.getAuditTypes().isEmpty()) {
      predicates.add(root.get("auditType").in(params.getAuditTypes()));
    }

    return predicates;
  }
}
