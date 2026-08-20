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
package org.hisp.dhis.analytics.event.data.registrationou;

import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;

class RegistrationOuSqlCoordinatorTest {

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private final OrganisationUnit root = levelOne('A');
  private final OrganisationUnit districtA = childOf(root, 'B');
  private final OrganisationUnit districtB = childOf(root, 'C');

  private static OrganisationUnit levelOne(char c) {
    OrganisationUnit ou = createOrganisationUnit(c);
    ou.updatePath();
    return ou;
  }

  private static OrganisationUnit childOf(OrganisationUnit parent, char c) {
    OrganisationUnit ou = createOrganisationUnit(c);
    ou.setParent(parent);
    ou.updatePath();
    return ou;
  }

  // -------------------------------------------------------------------------
  // Join
  // -------------------------------------------------------------------------

  @Test
  void testNoJoinWithoutRegistrationOu() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendLegacyJoin(
        sql, new EventQueryParams.Builder().build(), sqlBuilder);

    assertEquals("", sql.toString());
  }

  @Test
  void testJoinAddedForDimension() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendLegacyJoin(sql, dimensionParams(districtA), sqlBuilder);

    assertTrue(sql.toString().contains("inner join analytics_rs_orgunitstructure as regous"));
  }

  @Test
  void testJoinAddedForFilterOnly() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendLegacyJoin(sql, filterParams(districtA), sqlBuilder);

    assertTrue(sql.toString().contains("inner join analytics_rs_orgunitstructure as regous"));
  }

  /** A bare dimension still needs the join, because the query endpoint projects the OU name. */
  @Test
  void testJoinAddedForBareDimension() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendLegacyJoin(sql, dimensionParams(), sqlBuilder);

    assertTrue(sql.toString().contains("inner join analytics_rs_orgunitstructure as regous"));
  }

  @Test
  void testSelectBuilderJoinAddedForDimension() {
    SelectBuilder sb = new SelectBuilder().addColumn("1").from("analytics_event_x", "ax");

    RegistrationOuSqlCoordinator.addJoinIfNeeded(sb, dimensionParams(districtA), sqlBuilder);

    assertTrue(
        sb.build()
            .contains(
                "analytics_rs_orgunitstructure regous "
                    + "on regous.\"organisationunituid\" = ax.\"registrationou\""),
        sb.build());
  }

  @Test
  void testSelectBuilderJoinOmittedWithoutRegistrationOu() {
    SelectBuilder sb = new SelectBuilder().addColumn("1").from("analytics_event_x", "ax");

    RegistrationOuSqlCoordinator.addJoinIfNeeded(
        sb, new EventQueryParams.Builder().build(), sqlBuilder);

    assertTrue(!sb.build().contains("analytics_rs_orgunitstructure"), sb.build());
  }

  // -------------------------------------------------------------------------
  // Where predicate
  // -------------------------------------------------------------------------

  @Test
  void testNoPredicateWithoutItems() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendWherePredicateIfNeeded(
        sql, new SqlHelper(), dimensionParams(), sqlBuilder);

    assertEquals("", sql.toString());
  }

  @Test
  void testPredicateForSingleLevel() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendWherePredicateIfNeeded(
        sql, new SqlHelper(), dimensionParams(districtA, districtB), sqlBuilder);

    assertEquals(
        "where (regous.\"uidlevel2\" in ('"
            + districtA.getUid()
            + "','"
            + districtB.getUid()
            + "')) ",
        sql.toString());
  }

  /** Items spanning levels union their subtrees, so the per-level predicates are OR-ed. */
  @Test
  void testPredicateForMixedLevelsIsOred() {
    StringBuilder sql = new StringBuilder();

    RegistrationOuSqlCoordinator.appendWherePredicateIfNeeded(
        sql, new SqlHelper(), dimensionParams(root, districtA), sqlBuilder);

    assertEquals(
        "where (regous.\"uidlevel1\" in ('"
            + root.getUid()
            + "') or regous.\"uidlevel2\" in ('"
            + districtA.getUid()
            + "')) ",
        sql.toString());
  }

  /** A dimension and a filter both restrict, so they are AND-ed rather than OR-ed. */
  @Test
  void testDimensionAndFilterAreAnded() {
    StringBuilder sql = new StringBuilder();
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of(districtA))
            .withRegistrationOuFilter(List.of(districtB))
            .build();

    RegistrationOuSqlCoordinator.appendWherePredicateIfNeeded(
        sql, new SqlHelper(), params, sqlBuilder);

    assertEquals(
        "where (regous.\"uidlevel2\" in ('"
            + districtA.getUid()
            + "')) and (regous.\"uidlevel2\" in ('"
            + districtB.getUid()
            + "')) ",
        sql.toString());
  }

  // -------------------------------------------------------------------------
  // Aggregate select / group by
  // -------------------------------------------------------------------------

  @Test
  void testNoAggregateColumnWhenNotAggregated() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addDimensionSelectColumns(
        columns, dimensionParams(districtA), false, false, sqlBuilder);

    assertTrue(columns.isEmpty());
  }

  @Test
  void testNoAggregateColumnForFilterOnly() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addDimensionSelectColumns(
        columns, filterParams(districtA), false, true, sqlBuilder);

    assertTrue(columns.isEmpty());
  }

  @Test
  void testAggregateGroupByColumn() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addDimensionSelectColumns(
        columns, dimensionParams(districtA), true, true, sqlBuilder);

    assertEquals(List.of("regous.\"uidlevel2\""), columns);
  }

  @Test
  void testAggregateProjectionColumn() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addDimensionSelectColumns(
        columns, dimensionParams(districtA), false, true, sqlBuilder);

    assertEquals(List.of("regous.\"uidlevel2\" as registrationou"), columns);
  }

  /**
   * One group-by column cannot represent org units at two levels, and an event below both is
   * genuinely ambiguous, so this is rejected rather than silently resolved.
   */
  @Test
  void testAggregateRejectsMixedLevels() {
    List<String> columns = new ArrayList<>();
    EventQueryParams params = dimensionParams(root, districtA);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                RegistrationOuSqlCoordinator.addDimensionSelectColumns(
                    columns, params, false, true, sqlBuilder));

    assertEquals(ErrorCode.E7261, exception.getErrorCode());
  }

  // -------------------------------------------------------------------------
  // Query select
  // -------------------------------------------------------------------------

  @Test
  void testNoQueryColumnsWithoutDimension() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addQuerySelectColumns(
        columns, filterParams(districtA), sqlBuilder);

    assertTrue(columns.isEmpty());
  }

  @Test
  void testQueryColumnsForDimension() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addQuerySelectColumns(
        columns, dimensionParams(districtA), sqlBuilder);

    assertEquals(
        List.of(
            "regous.\"organisationunituid\" as registrationou",
            "regous.\"name\" as registrationouname"),
        columns);
  }

  @Test
  void testQueryColumnsForBareDimension() {
    List<String> columns = new ArrayList<>();

    RegistrationOuSqlCoordinator.addQuerySelectColumns(columns, dimensionParams(), sqlBuilder);

    assertEquals(2, columns.size());
  }

  private EventQueryParams dimensionParams(OrganisationUnit... items) {
    return new EventQueryParams.Builder().withRegistrationOuDimension(List.of(items)).build();
  }

  private EventQueryParams filterParams(OrganisationUnit... items) {
    return new EventQueryParams.Builder().withRegistrationOuFilter(List.of(items)).build();
  }
}
