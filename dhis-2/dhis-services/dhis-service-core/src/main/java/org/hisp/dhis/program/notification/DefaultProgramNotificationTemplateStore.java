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
package org.hisp.dhis.program.notification;

import static org.hisp.dhis.program.notification.BaseNotificationParam.DEFAULT_PAGE;
import static org.hisp.dhis.program.notification.BaseNotificationParam.DEFAULT_PAGE_SIZE;

import java.math.BigInteger;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Created by zubair@dhis2.org on 16.11.17. */
@Repository("org.hisp.dhis.program.ProgramNotificationTemplateStore")
public class DefaultProgramNotificationTemplateStore
    extends HibernateIdentifiableObjectStore<ProgramNotificationTemplate>
    implements ProgramNotificationTemplateStore {
  private static final String DEFAULT_ORDER = " programnotificationtemplateid desc ";
  private static final String NOTIFICATION_RECIPIENT = "recipient";

  private static final String PROGRAM_ID = "pid";

  private static final String PROGRAM_STAGE_ID = "psid";

  public DefaultProgramNotificationTemplateStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(
        entityManager,
        jdbcTemplate,
        publisher,
        ProgramNotificationTemplate.class,
        aclService,
        true);
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramNotificationByTriggerType(
      NotificationTrigger trigger) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("notificationtrigger"), trigger)));
  }

  @Override
  public boolean isProgramLinkedToWebHookNotification(Long pId) {
    NativeQuery<BigInteger> query =
        nativeSynchronizedQuery(
            "select count(*) from programnotificationtemplate where programid = :pid and notificationrecipienttype = :recipient");
    query.setParameter(PROGRAM_ID, pId);
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getSingleResult().longValue() > 0;
  }

  @Override
  public boolean isProgramStageLinkedToWebHookNotification(Long psId) {
    NativeQuery<BigInteger> query =
        nativeSynchronizedQuery(
            "select count(*) from programnotificationtemplate where programstageid = :psid and notificationrecipienttype = :recipient");
    query.setParameter(PROGRAM_STAGE_ID, psId);
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getSingleResult().longValue() > 0;
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramLinkedToWebHookNotifications(Program program) {
    NativeQuery<ProgramNotificationTemplate> query =
        nativeSynchronizedTypedQuery(
            "select * from programnotificationtemplate where programid = :pid and notificationrecipienttype = :recipient");
    query.setParameter(PROGRAM_ID, program.getId());
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getResultList();
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramStageLinkedToWebHookNotifications(
      ProgramStage programStage) {
    NativeQuery<ProgramNotificationTemplate> query =
        nativeSynchronizedTypedQuery(
            "select * from programnotificationtemplate where programstageid = :psid and notificationrecipienttype = :recipient");
    query.setParameter(PROGRAM_STAGE_ID, programStage.getId());
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getResultList();
  }

  @Override
  public int countProgramNotificationTemplates(ProgramNotificationTemplateParam param) {
    StringBuilder sql = new StringBuilder("select count(*) from programnotificationtemplate ");
    SqlHelper sqlHelper = new SqlHelper();

    if (param.hasProgram()) {
      sql.append(sqlHelper.whereAnd()).append(" programid = :programId");
    }

    if (param.hasProgramStage()) {
      sql.append(sqlHelper.whereAnd()).append(" programstageid = :programStageId");
    }

    NativeQuery<Number> query = nativeSynchronizedQuery(sql.toString());

    if (param.hasProgram()) {
      query.setParameter("programId", param.getProgram().getId());
    }

    if (param.hasProgramStage()) {
      query.setParameter("programStageId", param.getProgramStage().getId());
    }
    return query.getSingleResult().intValue();
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramNotificationTemplates(
      ProgramNotificationTemplateParam param) {
    SqlHelper sqlHelper = new SqlHelper();

    StringBuilder sql = new StringBuilder("select * from programnotificationtemplate ");

    if (param.hasProgram()) {
      sql.append(sqlHelper.whereAnd()).append(" programid = :programId ");
    }

    if (param.hasProgramStage()) {
      sql.append(sqlHelper.whereAnd()).append(" programstageid = :programStageId ");
    }

    sql.append(" ORDER BY ").append(DEFAULT_ORDER);

    NativeQuery<ProgramNotificationTemplate> query = nativeSynchronizedTypedQuery(sql.toString());

    if (param.hasProgram()) {
      query.setParameter("programId", param.getProgram().getId());
    }

    if (param.hasProgramStage()) {
      query.setParameter("programStageId", param.getProgramStage().getId());
    }

    if (!param.isSkipPaging()) {
      int page = param.getPage() != null ? param.getPage() : DEFAULT_PAGE;
      int pageSize = param.getPageSize() != null ? param.getPageSize() : DEFAULT_PAGE_SIZE;

      query.setFirstResult((page - 1) * pageSize);
      query.setMaxResults(pageSize);
    }
    return query.getResultList();
  }
}
