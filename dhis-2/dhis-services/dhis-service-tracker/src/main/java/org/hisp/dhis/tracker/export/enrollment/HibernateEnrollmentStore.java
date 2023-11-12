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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.getLongDateString;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.nowMinusDuration;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.enrollment.EnrollmentStore")
class HibernateEnrollmentStore extends SoftDeleteHibernateObjectStore<Enrollment>
    implements EnrollmentStore {

  private static final String DEFAULT_ORDER = "id desc";

  private static final String STATUS = "status";

  /**
   * Enrollments can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.program.Enrollment}.
   */
  private static final Set<String> ORDERABLE_FIELDS =
      Set.of(
          "endDate",
          "created",
          "createdAtClient",
          "enrollmentDate",
          "lastUpdated",
          "lastUpdatedAtClient");

  public HibernateEnrollmentStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        Enrollment.class,
        currentUserService,
        aclService,
        true);
  }

  @Override
  public int countEnrollments(EnrollmentQueryParams params) {
    String hql = buildCountEnrollmentHql(params);

    Query<Long> query = getTypedQuery(hql);

    return query.getSingleResult().intValue();
  }

  private String buildCountEnrollmentHql(EnrollmentQueryParams params) {
    return buildEnrollmentHql(params)
        .getQuery()
        .replaceFirst("from Enrollment en", "select count(distinct uid) from Enrollment en");
  }

  @Override
  public List<Enrollment> getEnrollments(EnrollmentQueryParams params) {
    String hql = buildEnrollmentHql(params).getFullQuery();

    Query<Enrollment> query = getQuery(hql);

    return query.list();
  }

  @Override
  public Page<Enrollment> getEnrollments(EnrollmentQueryParams params, PageParams pageParams) {
    String hql = buildEnrollmentHql(params).getFullQuery();

    Query<Enrollment> query = getQuery(hql);
    query.setFirstResult((pageParams.getPage() - 1) * pageParams.getPageSize());
    query.setMaxResults(pageParams.getPageSize());

    IntSupplier enrollmentCount = () -> countEnrollments(params);
    return getPage(pageParams, query.list(), enrollmentCount);
  }

  private Page<Enrollment> getPage(
      PageParams pageParams, List<Enrollment> enrollments, IntSupplier enrollmentCount) {
    if (pageParams.isPageTotal()) {
      Pager pager =
          new Pager(pageParams.getPage(), enrollmentCount.getAsInt(), pageParams.getPageSize());
      return Page.of(enrollments, pager);
    }

    Pager pager = new Pager(pageParams.getPage(), 0, pageParams.getPageSize());
    pager.force(pageParams.getPage(), pageParams.getPageSize());
    return Page.of(enrollments, pager);
  }

  private QueryWithOrderBy buildEnrollmentHql(EnrollmentQueryParams params) {
    String hql = "from Enrollment en";
    SqlHelper hlp = new SqlHelper(true);

    if (params.hasEnrollmentUids()) {
      hql +=
          hlp.whereAnd()
              + "en.uid in ("
              + getQuotedCommaDelimitedString(params.getEnrollmentUids())
              + ")";
    }

    if (params.hasLastUpdatedDuration()) {
      hql +=
          hlp.whereAnd()
              + "en.lastUpdated >= '"
              + getLongGmtDateString(nowMinusDuration(params.getLastUpdatedDuration()))
              + "'";
    } else if (params.hasLastUpdated()) {
      hql +=
          hlp.whereAnd() + "en.lastUpdated >= '" + getLongDateString(params.getLastUpdated()) + "'";
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

    if (params.hasProgramStatus()) {
      hql += hlp.whereAnd() + "en." + STATUS + " = '" + params.getProgramStatus() + "'";
    }

    if (params.hasFollowUp()) {
      hql += hlp.whereAnd() + "en.followup = " + params.getFollowUp();
    }

    if (params.hasProgramStartDate()) {
      hql +=
          hlp.whereAnd()
              + "en.enrollmentDate >= '"
              + getLongDateString(params.getProgramStartDate())
              + "'";
    }

    if (params.hasProgramEndDate()) {
      hql +=
          hlp.whereAnd()
              + "en.enrollmentDate <= '"
              + getLongDateString(params.getProgramEndDate())
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
          .append("en.organisationUnit.path LIKE '")
          .append(organisationUnit.getPath())
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
          .append("en.organisationUnit.path LIKE '")
          .append(organisationUnit.getPath())
          .append("%'")
          .append(" AND (en.organisationUnit.hierarchyLevel = ")
          .append(organisationUnit.getHierarchyLevel())
          .append(" OR en.organisationUnit.hierarchyLevel = ")
          .append((organisationUnit.getHierarchyLevel() + 1))
          .append(")");
    }
    return orgUnits.toString();
  }

  private String getSelectedQuery(Set<OrganisationUnit> organisationUnits) {
    return "en.organisationUnit.uid in ("
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
    return " order by " + orderJoiner;
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

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<Enrollment>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected Enrollment postProcessObject(Enrollment enrollment) {
    return (enrollment == null || enrollment.isDeleted()) ? null : enrollment;
  }

  @Override
  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS;
  }
}
