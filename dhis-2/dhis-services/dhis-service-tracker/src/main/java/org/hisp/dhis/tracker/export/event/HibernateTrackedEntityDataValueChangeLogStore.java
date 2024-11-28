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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.springframework.stereotype.Component;

// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component("org.hisp.dhis.tracker.export.event.TrackedEntityDataValueChangeLogStore")
class HibernateTrackedEntityDataValueChangeLogStore {
  private static final String PROP_EVENT = "event";

  private static final String PROP_ORGANISATION_UNIT = "organisationUnit";

  private static final String PROP_CREATED = "created";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final EntityManager entityManager;

  public HibernateTrackedEntityDataValueChangeLogStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void addTrackedEntityDataValueChangeLog(
      TrackedEntityDataValueChangeLog trackedEntityDataValueChangeLog) {
    entityManager.unwrap(Session.class).save(trackedEntityDataValueChangeLog);
  }

  @SuppressWarnings("unchecked")
  public List<TrackedEntityDataValueChangeLog> getTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<TrackedEntityDataValueChangeLog> criteria =
        builder.createQuery(TrackedEntityDataValueChangeLog.class);
    Root<TrackedEntityDataValueChangeLog> tedvcl =
        criteria.from(TrackedEntityDataValueChangeLog.class);
    Join<TrackedEntityDataValueChangeLog, Event> event = tedvcl.join(PROP_EVENT);
    Join<Event, OrganisationUnit> ou = event.join(PROP_ORGANISATION_UNIT);
    criteria.select(tedvcl);

    List<Predicate> predicates =
        getTrackedEntityDataValueAuditCriteria(params, builder, tedvcl, event, ou);
    criteria.where(predicates.toArray(Predicate[]::new));
    criteria.orderBy(builder.desc(tedvcl.get(PROP_CREATED)));

    Query query = entityManager.createQuery(criteria);

    if (params.hasPaging()) {
      query
          .setFirstResult(params.getPager().getOffset())
          .setMaxResults(params.getPager().getPageSize());
    }

    return query.getResultList();
  }

  public int countTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
    Root<TrackedEntityDataValueChangeLog> tedvcl =
        criteria.from(TrackedEntityDataValueChangeLog.class);
    Join<TrackedEntityDataValueChangeLog, Event> event = tedvcl.join(PROP_EVENT);
    Join<Event, OrganisationUnit> ou = event.join(PROP_ORGANISATION_UNIT);
    criteria.select(builder.countDistinct(tedvcl.get("id")));

    List<Predicate> predicates =
        getTrackedEntityDataValueAuditCriteria(params, builder, tedvcl, event, ou);
    criteria.where(predicates.toArray(Predicate[]::new));

    return entityManager.createQuery(criteria).getSingleResult().intValue();
  }

  public void deleteTrackedEntityDataValueChangeLog(DataElement dataElement) {
    String hql = "delete from TrackedEntityDataValueChangeLog d where d.dataElement = :de";

    entityManager.createQuery(hql).setParameter("de", dataElement).executeUpdate();
  }

  public void deleteTrackedEntityDataValueChangeLog(Event event) {
    String hql = "delete from TrackedEntityDataValueChangeLog d where d.event = :event";

    entityManager.createQuery(hql).setParameter(PROP_EVENT, event).executeUpdate();
  }

  private List<Predicate> getTrackedEntityDataValueAuditCriteria(
      TrackedEntityDataValueChangeLogQueryParams params,
      CriteriaBuilder builder,
      Root<TrackedEntityDataValueChangeLog> tedvcl,
      Join<TrackedEntityDataValueChangeLog, Event> event,
      Join<Event, OrganisationUnit> ou) {
    List<Predicate> predicates = new ArrayList<>();

    if (!params.getDataElements().isEmpty()) {
      predicates.add(tedvcl.get("dataElement").in(params.getDataElements()));
    }

    if (!params.getOrgUnits().isEmpty()) {
      if (DESCENDANTS == params.getOuMode()) {
        List<Predicate> orgUnitPredicates = new ArrayList<>();

        for (OrganisationUnit orgUnit : params.getOrgUnits()) {
          orgUnitPredicates.add(builder.like(ou.get("path"), (orgUnit.getPath() + "%")));
        }

        predicates.add(builder.or(orgUnitPredicates.toArray(Predicate[]::new)));
      } else if (SELECTED == params.getOuMode() || !params.hasOuMode()) {
        predicates.add(event.get(PROP_ORGANISATION_UNIT).in(params.getOrgUnits()));
      }
    }

    if (!params.getEvents().isEmpty()) {
      predicates.add(tedvcl.get(PROP_EVENT).in(params.getEvents()));
    }

    if (!params.getProgramStages().isEmpty()) {
      predicates.add(event.get("programStage").in(params.getProgramStages()));
    }

    if (params.getStartDate() != null) {
      predicates.add(builder.greaterThanOrEqualTo(tedvcl.get(PROP_CREATED), params.getStartDate()));
    }

    if (params.getEndDate() != null) {
      predicates.add(builder.lessThanOrEqualTo(tedvcl.get(PROP_CREATED), params.getEndDate()));
    }

    if (!params.getAuditTypes().isEmpty()) {
      predicates.add(tedvcl.get("auditType").in(params.getAuditTypes()));
    }

    return predicates;
  }
}
