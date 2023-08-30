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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.hibernate.JpaQueryParameters;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTempOwnershipAudit;
import org.hisp.dhis.program.ProgramTempOwnershipAuditQueryParams;
import org.hisp.dhis.program.ProgramTempOwnershipAuditStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Repository("org.hisp.dhis.program.ProgramTempOwnershipAuditStore")
public class HibernateProgramTempOwnershipAuditStore
    extends HibernateGenericStore<ProgramTempOwnershipAudit>
    implements ProgramTempOwnershipAuditStore {
  public HibernateProgramTempOwnershipAuditStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher) {
    super(sessionFactory, jdbcTemplate, publisher, ProgramTempOwnershipAudit.class, false);
  }

  // -------------------------------------------------------------------------
  // ProgramTempOwnershipAuditStore implementation
  // -------------------------------------------------------------------------

  @Override
  public void addProgramTempOwnershipAudit(ProgramTempOwnershipAudit programTempOwnershipAudit) {
    sessionFactory.getCurrentSession().save(programTempOwnershipAudit);
  }

  @Override
  public void deleteProgramTempOwnershipAudit(Program program) {
    String hql = "delete ProgramTempOwnershipAudit where program = :program";
    sessionFactory
        .getCurrentSession()
        .createQuery(hql)
        .setParameter("program", program)
        .executeUpdate();
  }

  @Override
  public List<ProgramTempOwnershipAudit> getProgramTempOwnershipAudits(
      ProgramTempOwnershipAuditQueryParams params) {
    CriteriaBuilder builder = getCriteriaBuilder();

    JpaQueryParameters<ProgramTempOwnershipAudit> jpaParameters =
        newJpaParameters()
            .addPredicates(getProgramTempOwnershipAuditPredicates(params, builder))
            .addOrder(root -> builder.desc(root.get("created")));

    if (!params.isSkipPaging()) {
      jpaParameters.setFirstResult(params.getFirst()).setMaxResults(params.getMax());
    }

    return getList(builder, jpaParameters);
  }

  @Override
  public int getProgramTempOwnershipAuditsCount(ProgramTempOwnershipAuditQueryParams params) {
    CriteriaBuilder builder = getCriteriaBuilder();

    return getCount(
            builder,
            newJpaParameters()
                .addPredicates(getProgramTempOwnershipAuditPredicates(params, builder))
                .count(root -> builder.countDistinct(root.get("id"))))
        .intValue();
  }

  private List<Function<Root<ProgramTempOwnershipAudit>, Predicate>>
      getProgramTempOwnershipAuditPredicates(
          ProgramTempOwnershipAuditQueryParams params, CriteriaBuilder builder) {
    List<Function<Root<ProgramTempOwnershipAudit>, Predicate>> predicates = new ArrayList<>();

    if (params.hasUsers()) {
      predicates.add(root -> root.get("accessedBy").in(params.getUsers()));
    }

    if (params.hasStartDate()) {
      predicates.add(
          root -> builder.greaterThanOrEqualTo(root.get("created"), params.getStartDate()));
    }

    if (params.hasEndDate()) {
      predicates.add(root -> builder.lessThanOrEqualTo(root.get("created"), params.getEndDate()));
    }

    if (params.hasPrograms()) {
      predicates.add(root -> root.get("program").in(params.getPrograms()));
    }

    return predicates;
  }
}
