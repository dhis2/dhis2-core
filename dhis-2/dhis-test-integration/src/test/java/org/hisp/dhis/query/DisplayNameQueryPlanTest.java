/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the query plans of the translated display-name filter on PostgreSQL. pgjdbc
 * server-prepares a statement after five executions, and PostgreSQL may then reuse a generic plan,
 * which is several times slower for this filter. The session here prefers generic plans, which is
 * what PostgreSQL chooses on its own once it has cached one.
 *
 * @author Morten Svanæs
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DisplayNameQueryPlanTest extends PostgresIntegrationTestBase {

  private static final int SEARCHES = 8;

  @Autowired private QueryService queryService;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  private final List<OrganisationUnit> units = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ThreadUserSettings.put(Map.of("keyDbLocale", "fr"));
    units.clear();
    for (int i = 0; i < SEARCHES; i++) {
      OrganisationUnit unit = createOrganisationUnit((char) ('A' + i));
      unit.setName("Base name " + i);
      unit.setTranslations(Set.of(new Translation(Locale.FRENCH, "NAME", "Nom traduit " + i)));
      identifiableObjectManager.save(unit);
      units.add(unit);
    }
    entityManager.flush();
    setPlanCacheMode("force_generic_plan");
  }

  @AfterEach
  void clearUserSettings() {
    ThreadUserSettings.clear();
  }

  @Test
  void displayNameListAndCountNeverRunOnAGenericPlan() {
    // Prepared statements live as long as the pooled connection, and the ordering statements of
    // other tests contain the same expression, so check only the statements that ran here.
    Map<String, PreparedPlan> before = preparedPlans();
    // Distinct terms keep the list query out of the query cache, so every search reaches the DB.
    for (int i = 0; i < SEARCHES; i++) {
      Query<OrganisationUnit> query =
          Query.of(OrganisationUnit.class)
              .add(Filters.ilike("displayName", "traduit " + i, MatchMode.ANYWHERE));
      query.setFirstResult(0);
      query.setMaxResults(50);
      assertEquals(
          List.of(units.get(i).getUid()),
          queryService.query(query).stream().map(OrganisationUnit::getUid).toList());
      assertEquals(1, queryService.count(query));
    }

    Map<String, PreparedPlan> executed = executedSince(before);
    assertFalse(
        executed.isEmpty(), "pgjdbc should have server-prepared the display-name statements");
    executed.forEach(
        (statement, plan) ->
            assertEquals(
                before.containsKey(statement) ? before.get(statement).generic() : 0,
                plan.generic(),
                "generic plans of " + statement));
    assertEquals("force_generic_plan", planCacheMode(), "the session setting is restored");
  }

  @Test
  void genericDisplayNamePlanEvaluatesTheTranslationPathOncePerExecution() {
    Map<String, PreparedPlan> before = preparedPlans();
    for (int i = 0; i < SEARCHES; i++) {
      queryService.count(
          Query.of(OrganisationUnit.class)
              .add(Filters.ilike("displayName", "traduit " + i, MatchMode.ANYWHERE)));
    }
    Map<String, PreparedPlan> executed = executedSince(before);
    assertFalse(
        executed.isEmpty(), "pgjdbc should have server-prepared the display-name statement");
    PreparedPlan count = executed.values().iterator().next();

    // Should the custom plan not be forced, the generic plan must still evaluate the bound JSONPath
    // and its variables once per execution (InitPlans), not once per row. The session prefers
    // generic plans, and a generic plan does not depend on the parameter values.
    String nulls = String.join(", ", Collections.nCopies(count.parameters(), "null"));
    List<?> plan =
        entityManager
            .createNativeQuery("explain execute \"" + count.name() + "\"(" + nulls + ")")
            .getResultList();
    // PostgreSQL 17 also names an InitPlan where its value is used, so count only plan nodes.
    long initPlans =
        plan.stream()
            .map(line -> String.valueOf(line).strip())
            .filter(line -> line.startsWith("InitPlan "))
            .count();
    assertEquals(2, initPlans, String.valueOf(plan));
  }

  private record PreparedPlan(String name, int parameters, long generic, long custom) {
    long executions() {
      return generic + custom;
    }
  }

  private Map<String, PreparedPlan> executedSince(Map<String, PreparedPlan> before) {
    Map<String, PreparedPlan> executed = new HashMap<>();
    preparedPlans()
        .forEach(
            (statement, plan) -> {
              PreparedPlan previous = before.get(statement);
              if (previous == null || previous.executions() < plan.executions()) {
                executed.put(statement, plan);
              }
            });
    return executed;
  }

  @SuppressWarnings("unchecked")
  private Map<String, PreparedPlan> preparedPlans() {
    List<Object[]> rows =
        entityManager
            .createNativeQuery(
                "select statement, name, cardinality(parameter_types), generic_plans, custom_plans"
                    + " from pg_prepared_statements"
                    + " where statement like '%jsonb_path_query_first%'")
            .getResultList();
    Map<String, PreparedPlan> plans = new HashMap<>();
    for (Object[] row : rows) {
      plans.put(
          (String) row[0],
          new PreparedPlan(
              (String) row[1],
              ((Number) row[2]).intValue(),
              ((Number) row[3]).longValue(),
              ((Number) row[4]).longValue()));
    }
    return plans;
  }

  private void setPlanCacheMode(String mode) {
    entityManager
        .createNativeQuery("select set_config('plan_cache_mode', :mode, false)")
        .setParameter("mode", mode)
        .getSingleResult();
  }

  private String planCacheMode() {
    return (String)
        entityManager
            .createNativeQuery("select current_setting('plan_cache_mode')")
            .getSingleResult();
  }
}
