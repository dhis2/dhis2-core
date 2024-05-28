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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.common.ObjectDeletionRequestedEvent;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.EnrollmentStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
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
  private static final String PI_HQL_BY_UIDS = "from Enrollment as en where en.uid in (:uids)";

  private static final String STATUS = "status";

  private static final Set<NotificationTrigger> SCHEDULED_ENROLLMENT_TRIGGERS =
      Sets.intersection(
          NotificationTrigger.getAllApplicableToEnrollment(),
          NotificationTrigger.getAllScheduledTriggers());

  public HibernateEnrollmentStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, Enrollment.class, aclService, true);
  }

  @Override
  public List<Enrollment> get(Program program) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("program"), program)));
  }

  @Override
  public List<Enrollment> get(Program program, EnrollmentStatus status) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("program"), program))
            .addPredicate(root -> builder.equal(root.get(STATUS), status)));
  }

  @Override
  public List<Enrollment> get(
      TrackedEntity trackedEntity, Program program, EnrollmentStatus status) {
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
        nativeSynchronizedQuery(
            "select exists(select 1 from enrollment where uid=:uid and deleted is false)");
    query.setParameter("uid", uid);

    return ((Boolean) query.getSingleResult()).booleanValue();
  }

  @Override
  public boolean existsIncludingDeleted(String uid) {
    if (uid == null) {
      return false;
    }

    Query<?> query =
        nativeSynchronizedQuery("select exists(select 1 from enrollment where uid=:uid)");
    query.setParameter("uid", uid);

    return ((Boolean) query.getSingleResult()).booleanValue();
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
        "select distinct en from Enrollment as en "
            + "inner join en.program as p "
            + "where :notificationTemplate in elements(p.notificationTemplates) "
            + "and en."
            + dateProperty
            + " is not null "
            + "and en.status = :activeEnrollmentStatus "
            + "and cast(:targetDate as date) = en."
            + dateProperty;

    return getQuery(hql)
        .setParameter("notificationTemplate", template)
        .setParameter("activeEnrollmentStatus", EnrollmentStatus.ACTIVE)
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
  public void hardDelete(Enrollment enrollment) {
    publisher.publishEvent(new ObjectDeletionRequestedEvent(enrollment));
    getSession().delete(enrollment);
  }

  private String toDateProperty(NotificationTrigger trigger) {
    if (trigger == NotificationTrigger.SCHEDULED_DAYS_ENROLLMENT_DATE) {
      return "enrollmentDate";
    } else if (trigger == NotificationTrigger.SCHEDULED_DAYS_INCIDENT_DATE) {
      return "occurredDate";
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
