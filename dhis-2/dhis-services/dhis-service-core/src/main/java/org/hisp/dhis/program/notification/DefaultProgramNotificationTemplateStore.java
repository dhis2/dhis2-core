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

import java.math.BigInteger;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Created by zubair@dhis2.org on 16.11.17. */
@Repository("org.hisp.dhis.program.ProgramNotificationTemplateStore")
public class DefaultProgramNotificationTemplateStore
    extends HibernateIdentifiableObjectStore<ProgramNotificationTemplate>
    implements ProgramNotificationTemplateStore {
  private static final String NOTIFICATION_RECIPIENT = "recipient";

  private static final String PROGRAM_ID = "pid";

  private static final String PROGRAM_STAGE_ID = "psid";

  public DefaultProgramNotificationTemplateStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        ProgramNotificationTemplate.class,
        currentUserService,
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
        getSession()
            .createNativeQuery(
                "select count(*) from programnotificationtemplate where programid = :pid and notificationrecipienttype = :recipient");
    query.setParameter(PROGRAM_ID, pId);
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getSingleResult().longValue() > 0;
  }

  @Override
  public boolean isProgramStageLinkedToWebHookNotification(Long psId) {
    NativeQuery<BigInteger> query =
        getSession()
            .createNativeQuery(
                "select count(*) from programnotificationtemplate where programstageid = :psid and notificationrecipienttype = :recipient");
    query.setParameter(PROGRAM_STAGE_ID, psId);
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getSingleResult().longValue() > 0;
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramLinkedToWebHookNotifications(Program program) {
    NativeQuery<ProgramNotificationTemplate> query =
        getSession()
            .createNativeQuery(
                "select * from programnotificationtemplate where programid = :pid and notificationrecipienttype = :recipient",
                ProgramNotificationTemplate.class);
    query.setParameter(PROGRAM_ID, program.getId());
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getResultList();
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramStageLinkedToWebHookNotifications(
      ProgramStage programStage) {
    NativeQuery<ProgramNotificationTemplate> query =
        getSession()
            .createNativeQuery(
                "select * from programnotificationtemplate where programstageid = :psid and notificationrecipienttype = :recipient",
                ProgramNotificationTemplate.class);
    query.setParameter(PROGRAM_STAGE_ID, programStage.getId());
    query.setParameter(NOTIFICATION_RECIPIENT, ProgramNotificationRecipient.WEB_HOOK.name());

    return query.getResultList();
  }

  @Override
  public int countProgramNotificationTemplates(ProgramNotificationTemplateParam param) {
    Query query =
        getSession()
            .createNativeQuery(
                "select count(*) from programnotificationtemplate where programstageid = :psid or  programid = :pid");
    query.setParameter(
        PROGRAM_STAGE_ID, param.hasProgramStage() ? param.getProgramStage().getId() : 0);
    query.setParameter(PROGRAM_ID, param.hasProgram() ? param.getProgram().getId() : 0);

    return ((Number) query.getSingleResult()).intValue();
  }

  @Override
  public List<ProgramNotificationTemplate> getProgramNotificationTemplates(
      ProgramNotificationTemplateParam param) {
    NativeQuery<ProgramNotificationTemplate> query =
        getSession()
            .createNativeQuery(
                "select * from programnotificationtemplate where programstageid = :psid or  programid = :pid",
                ProgramNotificationTemplate.class);

    query.setParameter(
        PROGRAM_STAGE_ID, param.hasProgramStage() ? param.getProgramStage().getId() : 0);
    query.setParameter(PROGRAM_ID, param.hasProgram() ? param.getProgram().getId() : 0);

    return query.getResultList();
  }
}
