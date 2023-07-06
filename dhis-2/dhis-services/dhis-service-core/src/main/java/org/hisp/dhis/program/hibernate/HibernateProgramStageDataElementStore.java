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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Viet Nguyen
 */
@Repository("org.hisp.dhis.program.ProgramStageDataElementStore")
public class HibernateProgramStageDataElementStore
    extends HibernateIdentifiableObjectStore<ProgramStageDataElement>
    implements ProgramStageDataElementStore {
  public HibernateProgramStageDataElementStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        ProgramStageDataElement.class,
        currentUserService,
        aclService,
        false);
  }

  @Override
  public ProgramStageDataElement get(ProgramStage programStage, DataElement dataElement) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("programStage"), programStage))
            .addPredicate(root -> builder.equal(root.get("dataElement"), dataElement)));
  }

  @Override
  public List<ProgramStageDataElement> getProgramStageDataElements(DataElement dataElement) {
    CriteriaBuilder builder = getCriteriaBuilder();
    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("dataElement"), dataElement)));
  }

  @Override
  public Map<String, Set<String>> getProgramStageDataElementsWithSkipSynchronizationSetToTrue() {
    final String sql =
        "select ps.uid as ps_uid, de.uid as de_uid from programstagedataelement psde "
            + "join programstage ps on psde.programstageid = ps.programstageid "
            + "join dataelement de on psde.dataelementid = de.dataelementid "
            + "where psde.programstageid in (select distinct ( programstageid ) from event psi where psi.lastupdated > psi.lastsynchronized) "
            + "and psde.skipsynchronization = true";

    final Map<String, Set<String>> psdesWithSkipSync = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          String programStageUid = rs.getString("ps_uid");
          String dataElementUid = rs.getString("de_uid");

          psdesWithSkipSync
              .computeIfAbsent(programStageUid, p -> new HashSet<>())
              .add(dataElementUid);
        });

    return psdesWithSkipSync;
  }
}
