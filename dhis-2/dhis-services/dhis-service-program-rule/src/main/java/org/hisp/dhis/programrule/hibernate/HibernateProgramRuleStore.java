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

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
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
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        ProgramRule.class,
        currentUserService,
        aclService,
        false);
  }

  @Override
  public List<ProgramRule> get(Program program) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters().addPredicate(root -> builder.equal(root.get("program"), program)));
  }

  @Override
  public ProgramRule getByName(String name, Program program) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getSingleResult(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("name"), name))
            .addPredicate(root -> builder.equal(root.get("program"), program)));
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
    final String hql =
        "SELECT distinct pr FROM ProgramRule pr JOIN FETCH pr.programRuleActions pra "
            + "LEFT JOIN FETCH pr.programStage ps "
            + "WHERE pr.program = :programId AND pra.programRuleActionType IN ( :implementableTypes ) "
            + "AND (pr.programStage IS NULL OR ps.uid = :programStageUid )";

    return getQuery(hql)
        .setParameter("programId", program)
        .setParameter("implementableTypes", types)
        .setParameter("programStageUid", programStageUid)
        .getResultList();
  }

  @Override
  public List<ProgramRule> get(Program program, String key) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder,
        newJpaParameters()
            .addPredicate(root -> builder.equal(root.get("program"), program))
            .addPredicate(
                root ->
                    JpaQueryUtils.stringPredicateIgnoreCase(
                        builder, root.get("name"), key, JpaQueryUtils.StringSearchMode.ANYWHERE))
            .addOrder(root -> builder.asc(root.get("name"))));
  }

  @Override
  public List<ProgramRule> getProgramRulesWithNoCondition() {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getList(
        builder, newJpaParameters().addPredicate(root -> builder.isNull(root.get("condition"))));
  }

  @Override
  public List<ProgramRule> getProgramRulesWithNoPriority() {
    final String jql =
        "FROM ProgramRule pr JOIN FETCH pr.programRuleActions pra "
            + "WHERE pr.priority IS NULL AND pra.programRuleActionType = :actionType";

    return getQuery(jql).setParameter("actionType", ProgramRuleActionType.ASSIGN).getResultList();
  }

  @Override
  public List<ProgramRule> getProgramRulesByEvaluationTime(
      ProgramRuleActionEvaluationTime evaluationTime) {
    Session session = getSession();
    session.clear(); // TODO Why?

    final String jql =
        "SELECT distinct pr FROM ProgramRule pr JOIN FETCH pr.programRuleActions pra "
            + "WHERE pra.programRuleActionEvaluationTime = :defaultEvaluationTime OR pra.programRuleActionEvaluationTime = :evaluationTime";

    return getQuery(jql)
        .setParameter("defaultEvaluationTime", ProgramRuleActionEvaluationTime.getDefault())
        .setParameter("evaluationTime", evaluationTime)
        .getResultList();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ProgramRule> getProgramRulesByEvaluationEnvironment(
      ProgramRuleActionEvaluationEnvironment environment) {
    List<BigInteger> bigIntegerList =
        getSession()
            .createNativeQuery(
                "select pra.programruleactionid from programrule pr JOIN programruleaction pra ON pr.programruleid=pra.programruleid "
                    + "where environments@> '[\""
                    + environment
                    + "\"]';")
            .list();
    List<Long> idList =
        bigIntegerList.stream()
            .map(item -> Long.valueOf(item.longValue()))
            .collect(Collectors.toList());

    Session session = getSession();
    session.clear();
    return session
        .createQuery(
            "SELECT distinct pr FROM ProgramRule pr JOIN FETCH pr.programRuleActions pra WHERE pra.id in (:ids)",
            ProgramRule.class)
        .setParameterList("ids", idList)
        .getResultList();
  }

  @Override
  public List<ProgramRule> getProgramRulesWithNoAction() {
    final String jql = "FROM ProgramRule pr WHERE pr.programRuleActions IS EMPTY";

    return getQuery(jql).getResultList();
  }
}
