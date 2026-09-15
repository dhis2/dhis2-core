/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.http.HttpMethod;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.config.PostgresTestConfigOverride;
import org.hisp.dhis.test.config.SlowQueryDataSourceProxy;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.hisp.dhis.webapi.controller.tracker.export.ExportTimeoutTest.DhisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests that tracker export requests are bounded by {@code tracker.export.timeout} and that
 * exceeding the budget returns 504 and leaves no PostgreSQL backend running.
 *
 * <p>Real export SQL is made slow by {@link SlowQueryDataSourceProxy}, which wraps the matched
 * statement in a {@code pg_sleep} at the datasource boundary so production queries stay untouched.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {DhisConfig.class, SlowQueryDataSourceProxy.class})
class ExportTimeoutTest extends PostgresControllerIntegrationTestBase {

  /**
   * Budget the export endpoints get in this test. Every test here runs until it is cancelled, so
   * each costs roughly this much and the budget is what drives the runtime.
   *
   * <p>Not lower because the shared budget test needs the first statement to fit and still leave a
   * remainder that {@code remainingSecondsCeil} does not round away. At 2s the fixture's own query
   * time was enough to spend the whole budget on the first statement.
   */
  private static final Duration BUDGET = Duration.ofSeconds(5);

  static class DhisConfig {
    @Bean
    public PostgresTestConfigOverride postgresTestConfigOverride() {
      PostgresTestConfigOverride override = new PostgresTestConfigOverride();
      override.put("tracker.export.timeout", String.valueOf(BUDGET.toSeconds()));
      return override;
    }
  }

  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private JdbcTemplate jdbcTemplate;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();

    manager.flush();
    manager.clear();
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @AfterEach
  void tearDownProxy() {
    SlowQueryDataSourceProxy.reset();
  }

  /**
   * Outer projection of the tracked entity id query ({@code JdbcTrackedEntityStore}). Matching on
   * this rather than on a table name keeps the sleep off the framework's own setup, auth and
   * metadata queries, which would otherwise hold every pooled connection in a sleep.
   */
  private static final String ID_QUERY = "select te.trackedentityid, te.uid";

  /**
   * Projection unique to {@code TrackedEntityStore}, run by {@code TrackedEntityAggregate} after
   * the id query.
   */
  private static final String AGGREGATE_QUERY = "te.uid as te_uid";

  /**
   * HEAD as well as GET: Spring dispatches it to the same {@code @GetMapping} handler, so it runs
   * the whole export and only drops the body on the way out. Unbounded, it would be a cheap request
   * that costs a full export and that nothing cancels.
   */
  @EnumSource(names = {"GET", "HEAD"})
  @ParameterizedTest
  void shouldReturnGatewayTimeoutAndCancelTheQueryWhenTheBudgetIsExceeded(HttpMethod method) {
    // sleep well past the budget so the deadline must fire on the id query
    SlowQueryDataSourceProxy.sleepBefore(ID_QUERY, BUDGET.multipliedBy(3));

    HttpStatus status;
    try {
      status = perform(method, "/api/tracker/trackedEntities?program=BFcipDERJnf").status();
    } finally {
      SlowQueryDataSourceProxy.disarm();
    }

    assertEquals(HttpStatus.GATEWAY_TIMEOUT, status);
    assertTrue(
        SlowQueryDataSourceProxy.matches() > 0,
        "no statement was slowed down, so this test would pass without any timeout being enforced");
    // A silently failed Statement.cancel() is a documented pgjdbc mode, so asserting only on the
    // HTTP status would pass while leaving an orphaned backend still running the query.
    assertEquals(
        0,
        countBackendsRunningSleep(),
        "a PostgreSQL backend is still running the cancelled query");
  }

  @Test
  void shouldShareOneBudgetAcrossSequentialStatementsOfOneRequest() {
    // Either statement fits the budget alone, the two together cannot, so this covers one budget
    // being shared across the id query and the aggregate's fetches.
    Duration overHalfTheBudget = BUDGET.dividedBy(2).plusSeconds(1);
    SlowQueryDataSourceProxy.sleepBefore(ID_QUERY, overHalfTheBudget);
    SlowQueryDataSourceProxy.sleepBefore(AGGREGATE_QUERY, overHalfTheBudget);

    long startNanos = System.nanoTime();
    HttpStatus status = getTrackedEntities("/tracker/trackedEntities?program={program}&fields=*");
    Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

    assertEquals(HttpStatus.GATEWAY_TIMEOUT, status);
    assertTrue(
        SlowQueryDataSourceProxy.matches() > 1,
        "only one statement was slowed down, so this does not exercise a shared budget");
    assertTrue(
        elapsed.compareTo(BUDGET.plus(overHalfTheBudget)) < 0,
        "request took "
            + elapsed
            + ", so each statement got its own timeout instead of sharing the budget of "
            + BUDGET);
  }

  /**
   * The export paths reading through Hibernate rather than {@code JdbcTemplate}, which are bounded
   * by the {@code jakarta.persistence.query.timeout} hint instead of by the deadline aware
   * template.
   *
   * <p>Each case is the SQL the sleep is injected into and the request that runs it:
   *
   * <ul>
   *   <li>relationships, the list query
   *   <li>relationships with {@code totalPages}, the count query. It returns a single result rather
   *       than a list, and an unbounded one surfaces as a bare {@code PersistenceException} that
   *       the advice turns into 409 rather than 504
   *   <li>tracked entity change logs, a sub path of an export controller, so the interceptor
   *       already arms a deadline for it
   * </ul>
   */
  private static Stream<Arguments> hibernateExportPaths() {
    return Stream.of(
        arguments("from relationship", "/tracker/relationships?trackedEntity={te}"),
        arguments("count(", "/tracker/relationships?trackedEntity={te}&totalPages=true"),
        arguments("from trackedentitychangelog", "/tracker/trackedEntities/{te}/changeLogs"));
  }

  @MethodSource("hibernateExportPaths")
  @ParameterizedTest
  void shouldReturnGatewayTimeoutWhenAHibernateExportQueryExceedsTheBudget(
      String sqlPattern, String url) {
    SlowQueryDataSourceProxy.sleepBefore(sqlPattern, BUDGET.multipliedBy(3));

    HttpStatus status;
    try {
      status = GET(url, "QS6w44flWAf").status();
    } finally {
      SlowQueryDataSourceProxy.disarm();
    }

    assertEquals(HttpStatus.GATEWAY_TIMEOUT, status);
    assertTrue(
        SlowQueryDataSourceProxy.matches() > 0,
        "no statement was slowed down, so this would pass without any timeout being enforced");
  }

  /**
   * A Hibernate query reached with under a second of budget left must still be bounded.
   *
   * <p>Hibernate takes its timeout in whole seconds, so a sub second remainder has to round up to 1
   * rather than down to 0, which JDBC reads as no timeout at all. Slowing the tracked entity lookup
   * that {@code RelationshipOperationParamsMapper} runs first leaves exactly that remainder for the
   * relationship query behind it.
   *
   * <p>Asserting on elapsed time rather than only on the 504: an unbounded relationship query still
   * ends in a 504, since the budget is already spent by the time it returns, but it runs its sleep
   * out in full first. That is the failure this guards, so the status alone would not catch it.
   */
  @Test
  void shouldBoundAHibernateQueryReachedWithLessThanASecondOfBudgetLeft() {
    Duration justUnderTheBudget = BUDGET.minusMillis(400);
    SlowQueryDataSourceProxy.sleepBefore("from trackedentity", justUnderTheBudget);
    SlowQueryDataSourceProxy.sleepBefore("from relationship", BUDGET.multipliedBy(3));

    long startNanos = System.nanoTime();
    HttpStatus status;
    try {
      status = GET("/tracker/relationships?trackedEntity={te}", "QS6w44flWAf").status();
    } finally {
      SlowQueryDataSourceProxy.disarm();
    }
    Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

    assertEquals(HttpStatus.GATEWAY_TIMEOUT, status);
    assertTrue(
        SlowQueryDataSourceProxy.matches() > 1,
        "only one statement was slowed down, so the relationship query did not run with a sub"
            + " second remainder");
    assertTrue(
        elapsed.compareTo(BUDGET.multipliedBy(2)) < 0,
        "request took "
            + elapsed
            + ", so the relationship query ran unbounded rather than being cancelled with the "
            + "remainder of the budget of "
            + BUDGET);
    assertEquals(
        0,
        countBackendsRunningSleep(),
        "a PostgreSQL backend is still running the cancelled query");
  }

  /**
   * Issues the request and disarms the proxy the moment it returns, so no sleep can bleed into the
   * assertions or the per-test teardown.
   */
  private HttpStatus getTrackedEntities(String url) {
    try {
      return GET(url, "BFcipDERJnf").status();
    } finally {
      SlowQueryDataSourceProxy.disarm();
    }
  }

  /** Number of PostgreSQL backends still executing the injected sleep. */
  private int countBackendsRunningSleep() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*) from pg_stat_activity
            where state = 'active'
              and query like '%pg_sleep%'
              and pid <> pg_backend_pid()""",
            Integer.class);
    return count == null ? 0 : count;
  }
}
