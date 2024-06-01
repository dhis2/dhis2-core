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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.Session;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
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
  public List<ProgramRule> getProgramRulesLinkedToTeaOrDe() {

    String jql =
        "SELECT distinct pr FROM ProgramRule pr, Program p "
            + "JOIN FETCH pr.programRuleActions pra "
            + "WHERE p.uid = pr.program.uid AND "
            + "(pra.dataElement IS NOT NULL OR pra.attribute IS NOT NULL)";
    Session session = getSession();
    return session.createQuery(jql, ProgramRule.class).getResultList();
  }

  @Override
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> actionTypes) {
    final String hql =
        "SELECT distinct pr FROM ProgramRule pr JOIN FETCH pr.programRuleActions pra "
            + "WHERE pr.program = :program AND pra.programRuleActionType IN ( :actionTypes ) ";

    return getQuery(hql)
        .setParameter("program", program)
        .setParameter("actionTypes", actionTypes)
        .getResultList();
  }

  @Override
  public List<ProgramRule> getProgramRulesByActionTypes(
      Program program, Set<ProgramRuleActionType> types, String programStageUid) {
    List<String> actionTypeNames = types.stream().map(Enum::name).toList();
    String sql =
        """

                SELECT distinct pr.* FROM programrule pr
                LEFT JOIN programruleaction pra on pra.programruleid=pr.programruleid
                LEFT JOIN programstage prs on prs.programstageid=pr.programstageid
                WHERE pr.programid =:programId  AND pra.actiontype IN ( :implementableTypes )
                AND (pr.programstageid IS NULL OR prs.uid = cast(:programStageUid as text))
                """;

    List<ProgramRule> programRules =
        nativeSynchronizedTypedQuery(sql)
            .setParameter("programId", program.getId())
            .setParameter("implementableTypes", actionTypeNames)
            .setParameter("programStageUid", programStageUid)
            .getResultList();

    if (programRules.isEmpty()) {
      return programRules;
    }

    sql =
        """
                SELECT distinct pra.* FROM programruleaction pra
                WHERE pra.programruleid in ( :programRuleIds ) AND pra.actiontype IN ( :implementableTypes )
                """;

    Map<ProgramRule, Set<ProgramRuleAction>> ruleActions =
        getSession()
            .createNativeQuery(sql, ProgramRuleAction.class)
            .addSynchronizedEntityClass(ProgramRuleAction.class)
            .setParameter(
                "programRuleIds", programRules.stream().map(BaseIdentifiableObject::getId).toList())
            .setParameter("implementableTypes", actionTypeNames)
            .getResultList()
            .stream()
            .collect(Collectors.groupingBy(ProgramRuleAction::getProgramRule, Collectors.toSet()));

    if (ruleActions.isEmpty()) {
      return programRules;
    }

    for (ProgramRule programRule : programRules) {
      if (ruleActions.containsKey(programRule)) {
        programRule.getProgramRuleActions().clear();
        programRule.getProgramRuleActions().addAll(ruleActions.get(programRule));
      }
    }

    return List.copyOf(programRules);
  }
}
