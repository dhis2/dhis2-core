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
package org.hisp.dhis.programrule.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleStore;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author markusbekken
 */
@Repository("org.hisp.dhis.programrule.ProgramRuleStore")
public class HibernateProgramRuleStore extends HibernateIdentifiableObjectStore<ProgramRule>
    implements ProgramRuleStore {

  public HibernateProgramRuleStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, ProgramRule.class, aclService, false);
  }

  @Override
  public List<ProgramRule> get(Program program) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("program").get("uid"), program.getUid())));
  }

  @Override
  public List<String> getDataElementsPresentInProgramRules(Set<ProgramRuleActionType> actionTypes) {
    String sql =
        """
            SELECT distinct de.uid
            FROM ProgramRuleAction pra JOIN pra.dataElement de
            WHERE pra.programRuleActionType in (:types)
        """;
    return getQuery(sql, String.class).setParameter("types", actionTypes).getResultList();
  }

  @Override
  public List<String> getTrackedEntityAttributesPresentInProgramRules(
      Set<ProgramRuleActionType> actionTypes) {
    String sql =
        """
                SELECT distinct att.uid
                FROM ProgramRuleAction pra JOIN pra.attribute att
                WHERE pra.programRuleActionType in (:types)
            """;
    return getQuery(sql, String.class).setParameter("types", actionTypes).getResultList();
  }

  @Override
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> actionTypes) {
    final String hql =
        "SELECT distinct pr FROM ProgramRule pr JOIN pr.programRuleActions pra "
            + "WHERE pr.program = :program AND pra.programRuleActionType IN ( :actionTypes ) ";

    return getQuery(hql)
        .setParameter("program", program)
        .setParameter("actionTypes", actionTypes)
        .getResultList();
  }

  @Override
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> types, String programStageUid) {
    final String hql =
        "SELECT distinct pr FROM ProgramRule pr JOIN pr.programRuleActions pra "
            + "LEFT JOIN pr.programStage ps "
            + "WHERE pr.program = :programId AND pra.programRuleActionType IN ( :implementableTypes ) "
            + "AND (pr.programStage IS NULL OR ps.uid = :programStageUid )";

    return getQuery(hql)
        .setParameter("programId", program)
        .setParameter("implementableTypes", types)
        .setParameter("programStageUid", programStageUid)
        .getResultList();
  }
}
