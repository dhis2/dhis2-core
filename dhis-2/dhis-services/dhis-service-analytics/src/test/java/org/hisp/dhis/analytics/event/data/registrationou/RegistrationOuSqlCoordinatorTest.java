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

import java.util.List;
import java.util.Optional;
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
    assertEquals(
        "",
        RegistrationOuSqlCoordinator.joinClause(
            new EventQueryParams.Builder().build(), sqlBuilder));
  }

  @Test
  void testJoinAddedForDimension() {
    assertTrue(
        RegistrationOuSqlCoordinator.joinClause(dimensionParams(districtA), sqlBuilder)
            .contains("inner join analytics_rs_orgunitstructure as regous"));
  }

  @Test
  void testJoinAddedForFilterOnly() {
    assertTrue(
        RegistrationOuSqlCoordinator.joinClause(filterParams(districtA), sqlBuilder)
            .contains("inner join analytics_rs_orgunitstructure as regous"));
  }

  /** A bare dimension still needs the join, because the query endpoint projects the OU name. */
  @Test
  void testJoinAddedForBareDimension() {
    assertTrue(
        RegistrationOuSqlCoordinator.joinClause(dimensionParams(), sqlBuilder)
            .contains("inner join analytics_rs_orgunitstructure as regous"));
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
    assertEquals(
        "",
        RegistrationOuSqlCoordinator.wherePredicate(
            dimensionParams(), new SqlHelper(), sqlBuilder));
  }

  @Test
  void testPredicateForSingleLevel() {
    assertEquals(
        "where (regous.\"uidlevel2\" in ('"
            + districtA.getUid()
            + "','"
            + districtB.getUid()
            + "')) ",
        RegistrationOuSqlCoordinator.wherePredicate(
            dimensionParams(districtA, districtB), new SqlHelper(), sqlBuilder));
  }

  /** Items spanning levels union their subtrees, so the per-level predicates are OR-ed. */
  @Test
  void testPredicateForMixedLevelsIsOred() {
    assertEquals(
        "where (regous.\"uidlevel1\" in ('"
            + root.getUid()
            + "') or regous.\"uidlevel2\" in ('"
            + districtA.getUid()
            + "')) ",
        RegistrationOuSqlCoordinator.wherePredicate(
            dimensionParams(root, districtA), new SqlHelper(), sqlBuilder));
  }

  /** A dimension and a filter both restrict, so they are AND-ed rather than OR-ed. */
  @Test
  void testDimensionAndFilterAreAnded() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of(districtA))
            .withRegistrationOuFilter(List.of(districtB))
            .build();

    assertEquals(
        "where (regous.\"uidlevel2\" in ('"
            + districtA.getUid()
            + "')) and (regous.\"uidlevel2\" in ('"
            + districtB.getUid()
            + "')) ",
        RegistrationOuSqlCoordinator.wherePredicate(params, new SqlHelper(), sqlBuilder));
  }

  // -------------------------------------------------------------------------
  // Aggregate select / group by
  // -------------------------------------------------------------------------

  @Test
  void testNoAggregateColumnWhenNotAggregated() {
    assertTrue(
        RegistrationOuSqlCoordinator.dimensionSelectColumn(
                dimensionParams(districtA), false, false, sqlBuilder)
            .isEmpty());
  }

  @Test
  void testNoAggregateColumnForFilterOnly() {
    assertTrue(
        RegistrationOuSqlCoordinator.dimensionSelectColumn(
                filterParams(districtA), false, true, sqlBuilder)
            .isEmpty());
  }

  @Test
  void testAggregateGroupByColumn() {
    assertEquals(
        Optional.of("regous.\"uidlevel2\""),
        RegistrationOuSqlCoordinator.dimensionSelectColumn(
            dimensionParams(districtA), true, true, sqlBuilder));
  }

  @Test
  void testAggregateProjectionColumn() {
    assertEquals(
        Optional.of("regous.\"uidlevel2\" as registrationou"),
        RegistrationOuSqlCoordinator.dimensionSelectColumn(
            dimensionParams(districtA), false, true, sqlBuilder));
  }

  /**
   * One group-by column cannot represent org units at two levels, and an event below both is
   * genuinely ambiguous, so this is rejected rather than silently resolved.
   */
  @Test
  void testAggregateRejectsMixedLevels() {
    EventQueryParams params = dimensionParams(root, districtA);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                RegistrationOuSqlCoordinator.dimensionSelectColumn(
                    params, false, true, sqlBuilder));

    assertEquals(ErrorCode.E7261, exception.getErrorCode());
  }

  // -------------------------------------------------------------------------
  // Query select
  // -------------------------------------------------------------------------

  @Test
  void testNoQueryColumnsWithoutDimension() {
    assertTrue(
        RegistrationOuSqlCoordinator.querySelectColumns(filterParams(districtA), sqlBuilder)
            .isEmpty());
  }

  @Test
  void testQueryColumnsForDimension() {
    assertEquals(
        List.of(
            "regous.\"organisationunituid\" as registrationou",
            "regous.\"name\" as registrationouname"),
        RegistrationOuSqlCoordinator.querySelectColumns(dimensionParams(districtA), sqlBuilder));
  }

  @Test
  void testQueryColumnsForBareDimension() {
    assertEquals(
        2, RegistrationOuSqlCoordinator.querySelectColumns(dimensionParams(), sqlBuilder).size());
  }

  private EventQueryParams dimensionParams(OrganisationUnit... items) {
    return new EventQueryParams.Builder().withRegistrationOuDimension(List.of(items)).build();
  }

  private EventQueryParams filterParams(OrganisationUnit... items) {
    return new EventQueryParams.Builder().withRegistrationOuFilter(List.of(items)).build();
  }
}
