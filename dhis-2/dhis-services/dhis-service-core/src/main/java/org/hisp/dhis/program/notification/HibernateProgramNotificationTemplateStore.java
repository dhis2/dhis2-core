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
package org.hisp.dhis.program.notification;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Created by zubair@dhis2.org on 16.11.17. */
@Repository("org.hisp.dhis.program.ProgramNotificationTemplateStore")
public class HibernateProgramNotificationTemplateStore
    extends HibernateIdentifiableObjectStore<ProgramNotificationTemplate>
    implements ProgramNotificationTemplateStore {

  private static final String DEFAULT_ORDER = " programnotificationtemplateid desc ";

  public HibernateProgramNotificationTemplateStore(
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
  public int countProgramNotificationTemplates(ProgramNotificationTemplateQueryParams param) {
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
      ProgramNotificationTemplateQueryParams param) {
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

    if (param.isPaging()) {
      query.setFirstResult((param.getPage() - 1) * param.getPageSize());
      query.setMaxResults(param.getPageSize());
    }
    return query.getResultList();
  }

  @Override
  public List<ProgramNotificationTemplate> getByDataElement(Collection<DataElement> dataElements) {
    String hql =
        """
        from ProgramNotificationTemplate pnt
        where pnt.recipientDataElement in :dataElements
        """;

    return getQuery(hql).setParameter("dataElements", dataElements).list();
  }
}
