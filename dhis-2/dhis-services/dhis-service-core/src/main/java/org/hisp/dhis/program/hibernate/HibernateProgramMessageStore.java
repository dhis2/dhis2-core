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

import java.util.List;
import javax.persistence.EntityManager;
import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Repository("org.hisp.dhis.program.ProgramMessageStore")
public class HibernateProgramMessageStore extends HibernateIdentifiableObjectStore<ProgramMessage>
    implements ProgramMessageStore {
  public HibernateProgramMessageStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, ProgramMessage.class, aclService, true);
  }

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public List<ProgramMessage> getProgramMessages(ProgramMessageQueryParams params) {
    Query<ProgramMessage> query = getHqlQuery(params);

    if (params.hasPaging()) {
      query.setFirstResult(params.getPage());
      query.setMaxResults(params.getPageSize());
    }

    return query.list();
  }

  // -------------------------------------------------------------------------
  // Supportive Methods
  // -------------------------------------------------------------------------

  private Query<ProgramMessage> getHqlQuery(ProgramMessageQueryParams params) {
    SqlHelper helper = new SqlHelper(true);

    String hql = " select distinct pm from ProgramMessage pm ";

    if (params.hasEnrollment()) {
      hql += helper.whereAnd() + "pm.enrollment = :enrollment";
    }

    if (params.hasEvent()) {
      hql += helper.whereAnd() + "pm.event = :event";
    }

    hql +=
        params.getMessageStatus() != null
            ? helper.whereAnd() + "pm.messageStatus = :messageStatus"
            : "";

    hql +=
        params.getAfterDate() != null
            ? helper.whereAnd() + "pm.processeddate > :processeddate"
            : "";

    hql +=
        params.getBeforeDate() != null
            ? helper.whereAnd() + "pm.processeddate < :processeddate"
            : "";

    Query<ProgramMessage> query = getQuery(hql);

    if (params.hasEnrollment()) {
      query.setParameter("enrollment", params.getEnrollment());
    }

    if (params.hasEvent()) {
      query.setParameter("event", params.getEvent());
    }

    if (params.getMessageStatus() != null) {
      query.setParameter("messageStatus", params.getMessageStatus());
    }

    if (params.getAfterDate() != null) {
      query.setParameter("processeddate", params.getAfterDate());
    }

    if (params.getBeforeDate() != null) {
      query.setParameter("processeddate", params.getBeforeDate());
    }

    return query;
  }
}
