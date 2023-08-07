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
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.hisp.dhis.util.DateUtils.nowMinusDuration;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
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
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.enrollment.EnrollmentStore")
class HibernateEnrollmentStore extends SoftDeleteHibernateObjectStore<Enrollment>
    implements EnrollmentStore {
  private static final String STATUS = "status";

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

    if (!params.isSkipPaging()) {
      query.setFirstResult(params.getOffset());
      query.setMaxResults(params.getPageSizeWithDefault());
    }

    // When the clients choose to not show the total of pages.
    if (!params.isTotalPages() && !params.isSkipPaging()) {
      // Get pageSize + 1, so we are able to know if there is another
      // page available. It adds one additional element into the list,
      // as consequence. The caller needs to remove the last element.
      query.setMaxResults(params.getPageSizeWithDefault() + 1);
    }

    return query.list();
  }

  private QueryWithOrderBy buildEnrollmentHql(EnrollmentQueryParams params) {
    String hql = "from Enrollment en";
    SqlHelper hlp = new SqlHelper(true);

    if (params.hasLastUpdatedDuration()) {
      hql +=
          hlp.whereAnd()
              + "en.lastUpdated >= '"
              + getLongGmtDateString(nowMinusDuration(params.getLastUpdatedDuration()))
              + "'";
    } else if (params.hasLastUpdated()) {
      hql +=
          hlp.whereAnd()
              + "en.lastUpdated >= '"
              + getMediumDateString(params.getLastUpdated())
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
        String ouClause = "(";
        SqlHelper orHlp = new SqlHelper(true);

        for (OrganisationUnit organisationUnit : params.getOrganisationUnits()) {
          ouClause +=
              orHlp.or() + "en.organisationUnit.path LIKE '" + organisationUnit.getPath() + "%'";
        }

        ouClause += ")";

        hql += hlp.whereAnd() + ouClause;
      } else {
        hql +=
            hlp.whereAnd()
                + "en.organisationUnit.uid in ("
                + getQuotedCommaDelimitedString(getUids(params.getOrganisationUnits()))
                + ")";
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
              + getMediumDateString(params.getProgramStartDate())
              + "'";
    }

    if (params.hasProgramEndDate()) {
      hql +=
          hlp.whereAnd()
              + "en.enrollmentDate <= '"
              + getMediumDateString(params.getProgramEndDate())
              + "'";
    }

    if (!params.isIncludeDeleted()) {
      hql += hlp.whereAnd() + " en.deleted is false ";
    }

    QueryWithOrderBy query = QueryWithOrderBy.builder().query(hql).build();

    if (params.isSorting()) {
      query =
          query.toBuilder()
              .orderBy(
                  " order by "
                      + params.getOrder().stream()
                          .map(
                              orderParam ->
                                  orderParam.getField()
                                      + " "
                                      + (orderParam.getDirection().isAscending() ? "asc" : "desc"))
                          .collect(Collectors.joining(", ")))
              .build();
    }

    return query;
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
}
