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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Abyot Asalefew
 */
@Repository("org.hisp.dhis.program.ProgramStageInstanceStore")
public class HibernateProgramStageInstanceStore
    extends SoftDeleteHibernateObjectStore<ProgramStageInstance>
    implements ProgramStageInstanceStore {
  private static final String PSI_HQL_BY_UIDS =
      "from ProgramStageInstance as psi where psi.uid in (:uids)";

  private static final Set<NotificationTrigger> SCHEDULED_PROGRAM_STAGE_INSTANCE_TRIGGERS =
      Sets.intersection(
          NotificationTrigger.getAllApplicableToProgramStageInstance(),
          NotificationTrigger.getAllScheduledTriggers());

  public HibernateProgramStageInstanceStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        ProgramStageInstance.class,
        currentUserService,
        aclService,
        false);
  }

  @Override
  public List<ProgramStageInstance> get(
      Collection<ProgramInstance> programInstances, EventStatus status) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("status"), status))
            .addPredicate(root -> root.get("programInstance").in(programInstances)));
  }

  @Override
  public List<ProgramStageInstance> get(TrackedEntityInstance entityInstance, EventStatus status) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("status"), status))
            .addPredicate(
                root ->
                    builder.equal(
                        root.join("programInstance").get("entityInstance"), entityInstance)));
  }

  @Override
  public long getProgramStageInstanceCountLastUpdatedAfter(Date time) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.greaterThanOrEqualTo(root.get("lastUpdated"), time))
            .count(builder::countDistinct));
  }

  @Override
  public boolean exists(String uid) {
    if (uid == null) {
      return false;
    }

    Query query =
        getSession()
            .createNativeQuery(
                "select exists(select 1 from programstageinstance where uid=:uid and deleted is false)");
    query.setParameter("uid", uid);

    return ((Boolean) query.getSingleResult()).booleanValue();
  }

  @Override
  public boolean existsIncludingDeleted(String uid) {
    if (uid == null) {
      return false;
    }

    Query query =
        getSession()
            .createNativeQuery("select exists(select 1 from programstageinstance where uid=:uid)");
    query.setParameter("uid", uid);

    return ((Boolean) query.getSingleResult()).booleanValue();
  }

  @Override
  public List<String> getUidsIncludingDeleted(List<String> uids) {
    final String hql = "select psi.uid " + PSI_HQL_BY_UIDS;
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
  public List<ProgramStageInstance> getIncludingDeleted(List<String> uids) {
    List<ProgramStageInstance> programStageInstances = new ArrayList<>();
    List<List<String>> uidsPartitions = Lists.partition(Lists.newArrayList(uids), 20000);

    for (List<String> uidsPartition : uidsPartitions) {
      if (!uidsPartition.isEmpty()) {
        programStageInstances.addAll(
            getSession()
                .createQuery(PSI_HQL_BY_UIDS, ProgramStageInstance.class)
                .setParameter("uids", uidsPartition)
                .list());
      }
    }

    return programStageInstances;
  }

  @Override
  public void updateProgramStageInstancesSyncTimestamp(
      List<String> programStageInstanceUIDs, Date lastSynchronized) {
    String hql =
        "update ProgramStageInstance set lastSynchronized = :lastSynchronized WHERE uid in :programStageInstances";

    getQuery(hql)
        .setParameter("lastSynchronized", lastSynchronized)
        .setParameter("programStageInstances", programStageInstanceUIDs)
        .executeUpdate();
  }

  @Override
  public List<ProgramStageInstance> getWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate) {
    if (notificationDate == null
        || !SCHEDULED_PROGRAM_STAGE_INSTANCE_TRIGGERS.contains(template.getNotificationTrigger())) {
      return Lists.newArrayList();
    }

    if (template.getRelativeScheduledDays() == null) {
      return Lists.newArrayList();
    }

    Date targetDate = DateUtils.addDays(notificationDate, template.getRelativeScheduledDays() * -1);

    String hql =
        "select distinct psi from ProgramStageInstance as psi "
            + "inner join psi.programStage as ps "
            + "where :notificationTemplate in elements(ps.notificationTemplates) "
            + "and psi.dueDate is not null "
            + "and psi.executionDate is null "
            + "and psi.status != :skippedEventStatus "
            + "and cast(:targetDate as date) = psi.dueDate "
            + "and psi.deleted is false";

    return getQuery(hql)
        .setParameter("notificationTemplate", template)
        .setParameter("skippedEventStatus", EventStatus.SKIPPED)
        .setParameter("targetDate", targetDate)
        .list();
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<ProgramStageInstance>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected ProgramStageInstance postProcessObject(ProgramStageInstance programStageInstance) {
    return (programStageInstance == null || programStageInstance.isDeleted())
        ? null
        : programStageInstance;
  }
}
