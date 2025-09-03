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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.nowMinusDuration;
import static org.hisp.dhis.util.DateUtils.toLongDateWithMillis;
import static org.hisp.dhis.util.DateUtils.toLongGmtDate;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component("org.hisp.dhis.tracker.export.enrollment.EnrollmentStore")
class HibernateEnrollmentStore extends SoftDeleteHibernateObjectStore<Enrollment> {

  private static final String DEFAULT_ORDER = "en.id desc";

  private static final String STATUS = "status";

  /**
   * Enrollments can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.program.Enrollment}.
   */
  private static final Set<String> ORDERABLE_FIELDS =
      Set.of(
          "completedDate",
          "created",
          "createdAtClient",
          "enrollmentDate",
          "lastUpdated",
          "lastUpdatedAtClient");

  public HibernateEnrollmentStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Enrollment.class, aclService, true);
  }

  private String buildCountEnrollmentHql(EnrollmentQueryParams params) {
    return buildEnrollmentHql(params)
        .getQuery()
        .replaceFirst(
            "select en\\s+from Enrollment en", "select count(distinct en.uid) from Enrollment en");
  }

  public List<Enrollment> getEnrollments(EnrollmentQueryParams params) {
    String hql = buildEnrollmentHql(params).getFullQuery();
    Query<Enrollment> query = getQuery(hql);
    return query.list();
  }

  public Page<Enrollment> getEnrollments(EnrollmentQueryParams params, PageParams pageParams) {
    String hql = buildEnrollmentHql(params).getFullQuery();
    Query<Enrollment> query = getQuery(hql);

    query.setFirstResult(pageParams.getOffset());
    query.setMaxResults(
        pageParams.getPageSize() + 1); // get extra enrollment to determine if there is a nextPage

    return new Page<>(query.list(), pageParams, () -> countEnrollments(params));
  }

  private long countEnrollments(EnrollmentQueryParams params) {
    String hql = buildCountEnrollmentHql(params);
    Query<Long> query = getTypedQuery(hql);
    return query.getSingleResult();
  }

  private QueryWithOrderBy buildEnrollmentHql(EnrollmentQueryParams params) {
    String hql =
        """
        select en
        from Enrollment en
        join TrackedEntityProgramOwner tepo
        on en.program = tepo.program
        and en.trackedEntity = tepo.trackedEntity
      """;

    SqlHelper hlp = new SqlHelper(true);

    if (params.hasEnrollmentUids()) {
      hql +=
          hlp.whereAnd()
              + "en.uid in ("
              + getQuotedCommaDelimitedString(UID.toValueList(params.getEnrollments()))
              + ")";
    }

    if (params.hasLastUpdatedDuration()) {
      hql +=
          hlp.whereAnd()
              + "en.lastUpdated >= '"
              + toLongGmtDate(nowMinusDuration(params.getLastUpdatedDuration()))
              + "'";
    } else if (params.hasLastUpdated()) {
      hql +=
          hlp.whereAnd()
              + "en.lastUpdated >= '"
              + toLongDateWithMillis(params.getLastUpdated())
              + "'";
    }

    if (params.hasTrackedEntity()) {
      hql += hlp.whereAnd() + "en.trackedEntity.uid = '" + params.getTrackedEntity().getUid() + "'";
    }

    if (params.hasTrackedEntityType()) {
      hql +=
          hlp.whereAnd()
              + "en.trackedEntity.trackedEntityType.uid = '"
              + params.getTrackedEntityType().getUid()
              + "'";
    }

    if (params.hasOrganisationUnits()) {
      if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)) {
        hql += hlp.whereAnd() + getDescendantsQuery(params.getOrganisationUnits());
      } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
        hql += hlp.whereAnd() + getChildrenQuery(hlp, params.getOrganisationUnits());

      } else {
        hql += hlp.whereAnd() + getSelectedQuery(params.getOrganisationUnits());
      }
    }

    if (params.hasProgram()) {
      hql += hlp.whereAnd() + "en.program.uid = '" + params.getProgram().getUid() + "'";
    }

    // TODO(DHIS2-17961) This will be removed when dummy enrollments will not exist anymore
    hql += hlp.whereAnd() + "en.program.programType = '" + ProgramType.WITH_REGISTRATION + "'";

    if (params.hasEnrollmentStatus()) {
      hql += hlp.whereAnd() + "en." + STATUS + " = '" + params.getEnrollmentStatus() + "'";
    }

    if (params.hasFollowUp()) {
      hql += hlp.whereAnd() + "en.followup = " + params.getFollowUp();
    }

    if (params.hasProgramStartDate()) {
      hql +=
          hlp.whereAnd()
              + "en.enrollmentDate >= '"
              + toLongDateWithMillis(params.getProgramStartDate())
              + "'";
    }

    if (params.hasProgramEndDate()) {
      hql +=
          hlp.whereAnd()
              + "en.enrollmentDate <= '"
              + toLongDateWithMillis(params.getProgramEndDate())
              + "'";
    }

    if (!params.isIncludeDeleted()) {
      hql += hlp.whereAnd() + " en.deleted is false ";
    }

    return QueryWithOrderBy.builder().query(hql).orderBy(orderBy(params.getOrder())).build();
  }

  private String getDescendantsQuery(Set<OrganisationUnit> organisationUnits) {
    StringBuilder ouClause = new StringBuilder();
    ouClause.append("(");

    SqlHelper orHlp = new SqlHelper(true);

    for (OrganisationUnit organisationUnit : organisationUnits) {
      ouClause
          .append(orHlp.or())
          .append("tepo.organisationUnit.path LIKE '")
          .append(organisationUnit.getStoredPath())
          .append("%'");
    }

    ouClause.append(")");

    return ouClause.toString();
  }

  private String getChildrenQuery(SqlHelper hlp, Set<OrganisationUnit> organisationUnits) {
    StringBuilder orgUnits = new StringBuilder();
    for (OrganisationUnit organisationUnit : organisationUnits) {
      orgUnits
          .append(hlp.or())
          .append("tepo.organisationUnit.path LIKE '")
          .append(organisationUnit.getStoredPath())
          .append("%'")
          .append(" AND (tepo.organisationUnit.hierarchyLevel = ")
          .append(organisationUnit.getHierarchyLevel())
          .append(" OR tepo.organisationUnit.hierarchyLevel = ")
          .append((organisationUnit.getHierarchyLevel() + 1))
          .append(")");
    }
    return orgUnits.toString();
  }

  private String getSelectedQuery(Set<OrganisationUnit> organisationUnits) {
    return "tepo.organisationUnit.uid in ("
        + getQuotedCommaDelimitedString(getUids(organisationUnits))
        + ")";
  }

  private static String orderBy(List<Order> orders) {
    if (orders.isEmpty()) {
      return " order by " + DEFAULT_ORDER;
    }

    StringJoiner orderJoiner = new StringJoiner(", ");
    for (Order order : orders) {
      orderJoiner.add(
          order.getField() + " " + (order.getDirection().isAscending() ? "asc" : "desc"));
    }
    return " order by " + orderJoiner + ", " + DEFAULT_ORDER;
  }

  @Getter
  @Builder(toBuilder = true)
  static class QueryWithOrderBy {
    private final String query;

    private final String orderBy;

    String getFullQuery() {
      return Stream.of(query, orderBy)
          .map(StringUtils::trimToEmpty)
          .filter(Objects::nonNull)
          .collect(Collectors.joining(" "));
    }
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS;
  }

  @Override
  public void delete(@Nonnull Enrollment enrollment) {
    enrollment.setStatus(EnrollmentStatus.CANCELLED);
    super.delete(enrollment);
  }
}
