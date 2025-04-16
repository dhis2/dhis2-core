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

import static org.hisp.dhis.system.util.SqlUtils.singleQuoteAndEscape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLog;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLogQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityChangeLogStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@Repository("org.hisp.dhis.trackedentity.TrackedEntityChangeLogStore")
public class HibernateTrackedEntityChangeLogStore
    extends HibernateGenericStore<TrackedEntityChangeLog> implements TrackedEntityChangeLogStore {

  public HibernateTrackedEntityChangeLogStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntityChangeLog.class, false);
  }

  // -------------------------------------------------------------------------
  // TrackedEntityAuditService implementation
  // -------------------------------------------------------------------------

  @Override
  public void addTrackedEntityChangeLog(TrackedEntityChangeLog trackedEntityChangeLog) {
    getSession().save(trackedEntityChangeLog);
  }

  @Override
  public void addTrackedEntityChangeLog(List<TrackedEntityChangeLog> trackedEntityChangeLog) {
    final String sql =
        "INSERT INTO trackedentityaudit ("
            + "trackedentityauditid, "
            + "trackedentity, "
            + "created, "
            + "accessedby, "
            + "audittype, "
            + "comment ) VALUES ";

    Function<TrackedEntityChangeLog, String> mapToString =
        audit -> {
          StringBuilder sb = new StringBuilder();
          sb.append("(");
          sb.append("nextval('trackedentityinstanceaudit_sequence'), ");
          sb.append(singleQuoteAndEscape(audit.getTrackedEntity())).append(",");
          sb.append("now()").append(",");
          sb.append(singleQuoteAndEscape(audit.getAccessedBy())).append(",");
          sb.append(singleQuoteAndEscape(audit.getAuditType().name())).append(",");
          sb.append(
              StringUtils.isNotEmpty(audit.getComment())
                  ? SqlUtils.singleQuoteAndEscape(audit.getComment())
                  : "''");
          sb.append(")");
          return sb.toString();
        };

    final String values =
        trackedEntityChangeLog.stream().map(mapToString).collect(Collectors.joining(","));

    nativeSynchronizedQuery(sql + values).executeUpdate();
  }

  @Override
  public List<TrackedEntityChangeLog> getTrackedEntityChangeLogs(
      TrackedEntityChangeLogQueryParams params) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<TrackedEntityChangeLog> jpaParameters =
        newJpaParameters()
            .addPredicates(getTrackedEntityPredicates(params, builder))
            .addOrder(root -> builder.desc(root.get("created")));

    if (params.hasPaging()) {
      jpaParameters
          .setFirstResult(params.getPager().getOffset())
          .setMaxResults(params.getPager().getPageSize());
    }

    return getList(builder, jpaParameters);
  }

  @Override
  public int getTrackedEntityChangeLogsCount(TrackedEntityChangeLogQueryParams params) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
            builder,
            newJpaParameters()
                .addPredicates(getTrackedEntityPredicates(params, builder))
                .count(root -> builder.countDistinct(root.get("id"))))
        .intValue();
  }

  private List<Function<Root<TrackedEntityChangeLog>, Predicate>> getTrackedEntityPredicates(
      TrackedEntityChangeLogQueryParams params, CriteriaBuilder builder) {
    List<Function<Root<TrackedEntityChangeLog>, Predicate>> predicates = new ArrayList<>();

    if (params.hasTrackedEntities()) {
      predicates.add(root -> root.get("trackedEntity").in(params.getTrackedEntities()));
    }

    if (params.hasUsers()) {
      predicates.add(root -> root.get("accessedBy").in(params.getUsers()));
    }

    if (params.hasAuditTypes()) {
      predicates.add(root -> root.get("auditType").in(params.getAuditTypes()));
    }

    if (params.hasStartDate()) {
      predicates.add(
          root -> builder.greaterThanOrEqualTo(root.get("created"), params.getStartDate()));
    }

    if (params.hasEndDate()) {
      predicates.add(root -> builder.lessThanOrEqualTo(root.get("created"), params.getEndDate()));
    }

    return predicates;
  }
}
