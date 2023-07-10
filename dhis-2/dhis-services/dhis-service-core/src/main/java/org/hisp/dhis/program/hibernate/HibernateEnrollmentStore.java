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
package org.hisp.dhis.program.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.hisp.dhis.util.DateUtils.nowMinusDuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.program.EnrollmentStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew
 * @author Lars Helge Overland
 */
@Repository("org.hisp.dhis.program.EnrollmentStore")
public class HibernateEnrollmentStore extends SoftDeleteHibernateObjectStore<Enrollment>
    implements EnrollmentStore {
  private static final String PI_HQL_BY_UIDS = "from Enrollment as pi where pi.uid in (:uids)";

  private static final String STATUS = "status";

  private static final Set<NotificationTrigger> SCHEDULED_ENROLLMENT_TRIGGERS =
      Sets.intersection(
          NotificationTrigger.getAllApplicableToEnrollment(),
          NotificationTrigger.getAllScheduledTriggers());

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
        .replaceFirst("from Enrollment pi", "select count(distinct uid) from Enrollment pi");
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
    String hql = "from Enrollment pi";
    SqlHelper hlp = new SqlHelper(true);

    if (params.hasLastUpdatedDuration()) {
      hql +=
          hlp.whereAnd()
              + "pi.lastUpdated >= '"
              + getLongGmtDateString(nowMinusDuration(params.getLastUpdatedDuration()))
              + "'";
    } else if (params.hasLastUpdated()) {
      hql +=
          hlp.whereAnd()
              + "pi.lastUpdated >= '"
              + getMediumDateString(params.getLastUpdated())
              + "'";
    }

    if (params.hasTrackedEntity()) {
      hql += hlp.whereAnd() + "pi.trackedEntity.uid = '" + params.getTrackedEntity().getUid() + "'";
    }

    if (params.hasTrackedEntityType()) {
      hql +=
          hlp.whereAnd()
              + "pi.trackedEntity.trackedEntityType.uid = '"
              + params.getTrackedEntityType().getUid()
              + "'";
    }

    if (params.hasOrganisationUnits()) {
      if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)) {
        String ouClause = "(";
        SqlHelper orHlp = new SqlHelper(true);

        for (OrganisationUnit organisationUnit : params.getOrganisationUnits()) {
          ouClause +=
              orHlp.or() + "pi.organisationUnit.path LIKE '" + organisationUnit.getPath() + "%'";
        }

        ouClause += ")";

        hql += hlp.whereAnd() + ouClause;
      } else {
        hql +=
            hlp.whereAnd()
                + "pi.organisationUnit.uid in ("
                + getQuotedCommaDelimitedString(getUids(params.getOrganisationUnits()))
                + ")";
      }
    }

    if (params.hasProgram()) {
      hql += hlp.whereAnd() + "pi.program.uid = '" + params.getProgram().getUid() + "'";
    }

    if (params.hasProgramStatus()) {
      hql += hlp.whereAnd() + "pi." + STATUS + " = '" + params.getProgramStatus() + "'";
    }

    if (params.hasFollowUp()) {
      hql += hlp.whereAnd() + "pi.followup = " + params.getFollowUp();
    }

    if (params.hasProgramStartDate()) {
      hql +=
          hlp.whereAnd()
              + "pi.enrollmentDate >= '"
              + getMediumDateString(params.getProgramStartDate())
              + "'";
    }

    if (params.hasProgramEndDate()) {
      hql +=
          hlp.whereAnd()
              + "pi.enrollmentDate <= '"
              + getMediumDateString(params.getProgramEndDate())
              + "'";
    }

    if (!params.isIncludeDeleted()) {
      hql += hlp.whereAnd() + " pi.deleted is false ";
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
  public List<Enrollment> get(Program program) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("program"), program)));
  }

  @Override
  public List<Enrollment> get(Program program, ProgramStatus status) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("program"), program))
            .addPredicate(root -> builder.equal(root.get(STATUS), status)));
  }

  @Override
  public List<Enrollment> get(TrackedEntity trackedEntity, Program program, ProgramStatus status) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("trackedEntity"), trackedEntity))
            .addPredicate(root -> builder.equal(root.get("program"), program))
            .addPredicate(root -> builder.equal(root.get(STATUS), status)));
  }

  @Override
  public boolean exists(String uid) {
    if (uid == null) {
      return false;
    }

    Query<?> query =
        getSession()
            .createNativeQuery(
                "select exists(select 1 from programinstance where uid=:uid and deleted is false)");
    query.setParameter("uid", uid);

    return ((Boolean) query.getSingleResult()).booleanValue();
  }

  @Override
  public boolean existsIncludingDeleted(String uid) {
    if (uid == null) {
      return false;
    }

    Query<?> query =
        getSession()
            .createNativeQuery("select exists(select 1 from programinstance where uid=:uid)");
    query.setParameter("uid", uid);

    return ((Boolean) query.getSingleResult()).booleanValue();
  }

  @Override
  public List<String> getUidsIncludingDeleted(List<String> uids) {
    String hql = "select pi.uid " + PI_HQL_BY_UIDS;
    List<String> resultUids = new ArrayList<>();
    List<List<String>> uidsPartitions = Lists.partition(Lists.newArrayList(uids), 20000);

    for (List<String> uidsPartition : uidsPartitions) {
      if (!uidsPartition.isEmpty()) {
        resultUids.addAll(
            getSession().createQuery(hql, String.class).setParameter("uids", uidsPartition).list());
      }
    }

    return resultUids;
  }

  @Override
  public List<Enrollment> getIncludingDeleted(List<String> uids) {
    List<Enrollment> enrollments = new ArrayList<>();
    List<List<String>> uidsPartitions = Lists.partition(Lists.newArrayList(uids), 20000);

    for (List<String> uidsPartition : uidsPartitions) {
      if (!uidsPartition.isEmpty()) {
        enrollments.addAll(
            getSession()
                .createQuery(PI_HQL_BY_UIDS, Enrollment.class)
                .setParameter("uids", uidsPartition)
                .list());
      }
    }

    return enrollments;
  }

  @Override
  public List<Enrollment> getWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate) {
    if (notificationDate == null
        || !SCHEDULED_ENROLLMENT_TRIGGERS.contains(template.getNotificationTrigger())) {
      return Lists.newArrayList();
    }

    String dateProperty = toDateProperty(template.getNotificationTrigger());

    if (dateProperty == null) {
      return Lists.newArrayList();
    }

    Date targetDate = DateUtils.addDays(notificationDate, template.getRelativeScheduledDays() * -1);

    String hql =
        "select distinct pi from Enrollment as pi "
            + "inner join pi.program as p "
            + "where :notificationTemplate in elements(p.notificationTemplates) "
            + "and pi."
            + dateProperty
            + " is not null "
            + "and pi.status = :activeEnrollmentStatus "
            + "and cast(:targetDate as date) = pi."
            + dateProperty;

    return getQuery(hql)
        .setParameter("notificationTemplate", template)
        .setParameter("activeEnrollmentStatus", ProgramStatus.ACTIVE)
        .setParameter("targetDate", targetDate)
        .list();
  }

  @Override
  public List<Enrollment> getByPrograms(List<Program> programs) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.in(root.get("program")).value(programs)));
  }

  @Override
  public List<Enrollment> getByType(ProgramType type) {
    String hql = "select pi from Enrollment pi join fetch pi.program p where p.programType = :type";

    Query<Enrollment> query = getQuery(hql);
    query.setParameter("type", type);

    return query.list();
  }

  @Override
  public void hardDelete(Enrollment enrollment) {
    publisher.publishEvent(new ObjectDeletionRequestedEvent(enrollment));
    getSession().delete(enrollment);
  }

  @Override
  public List<Enrollment> getByProgramAndTrackedEntity(
      List<Pair<Program, TrackedEntity>> programTeiPair, ProgramStatus programStatus) {
    checkNotNull(programTeiPair);

    if (programTeiPair.isEmpty()) {
      return new ArrayList<>();
    }

    CriteriaBuilder cb = sessionFactory.getCurrentSession().getCriteriaBuilder();
    CriteriaQuery<Enrollment> cr = cb.createQuery(Enrollment.class);
    Root<Enrollment> enrollment = cr.from(Enrollment.class);

    // Constructing list of parameters
    List<Predicate> predicates = new ArrayList<>();

    // TODO we may have potentially thousands of events here, so, it's
    // better to
    // partition the list
    for (Pair<Program, TrackedEntity> pair : programTeiPair) {
      predicates.add(
          cb.and(
              cb.equal(enrollment.get("program"), pair.getLeft()),
              cb.equal(enrollment.get("trackedEntity"), pair.getRight()),
              cb.equal(enrollment.get(STATUS), programStatus)));
    }

    cr.select(enrollment).where(cb.or(predicates.toArray(new Predicate[] {})));

    return sessionFactory.getCurrentSession().createQuery(cr).getResultList();
  }

  private String toDateProperty(NotificationTrigger trigger) {
    if (trigger == NotificationTrigger.SCHEDULED_DAYS_ENROLLMENT_DATE) {
      return "enrollmentDate";
    } else if (trigger == NotificationTrigger.SCHEDULED_DAYS_INCIDENT_DATE) {
      return "incidentDate";
    }

    return null;
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
