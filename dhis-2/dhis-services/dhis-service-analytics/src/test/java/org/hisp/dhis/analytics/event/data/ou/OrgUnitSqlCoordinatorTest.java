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
package org.hisp.dhis.analytics.event.data.ou;

import static org.hisp.dhis.analytics.event.data.ou.TrackerOrgUnitDimension.ENROLLMENT_OU;
import static org.hisp.dhis.analytics.event.data.ou.TrackerOrgUnitDimension.REGISTRATION_OU;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrgUnitSqlCoordinatorTest {

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private final OrganisationUnit root = levelOne('A');
  private final OrganisationUnit districtA = childOf(root, 'B');
  private final OrganisationUnit districtB = childOf(root, 'C');
  private final OrganisationUnit chiefdom = childOf(districtA, 'D');
  private final OrganisationUnit facility = childOf(chiefdom, 'E');

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

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testNoJoinWhenUnused(TrackerOrgUnitDimension dimension) {
    assertEquals(
        "",
        OrgUnitSqlCoordinator.joinClause(
            dimension, new EventQueryParams.Builder().build(), sqlBuilder));
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testJoinAddedForDimension(TrackerOrgUnitDimension dimension) {
    assertEquals(
        "inner join analytics_rs_orgunitstructure as "
            + dimension.getStructAlias()
            + " on "
            + dimension.getStructAlias()
            + ".\"organisationunituid\" = ax.\""
            + dimension.getAnalyticsColumn()
            + "\" ",
        OrgUnitSqlCoordinator.joinClause(
            dimension, dimensionParams(dimension, districtA), sqlBuilder));
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testJoinAddedForFilterOnly(TrackerOrgUnitDimension dimension) {
    assertTrue(
        OrgUnitSqlCoordinator.joinClause(dimension, filterParams(dimension, districtA), sqlBuilder)
            .contains("inner join analytics_rs_orgunitstructure as " + dimension.getStructAlias()));
  }

  /** A bare dimension still needs the join, because the query endpoint projects the OU name. */
  @Test
  void testJoinAddedForBareRegistrationOuDimension() {
    assertTrue(
        OrgUnitSqlCoordinator.joinClause(
                REGISTRATION_OU, dimensionParams(REGISTRATION_OU), sqlBuilder)
            .contains("inner join analytics_rs_orgunitstructure as regous"));
  }

  /** Each dimension joins only for itself, so one does not drag in the other's join. */
  @Test
  void testJoinIsPerDimension() {
    EventQueryParams params = dimensionParams(ENROLLMENT_OU, districtA);

    assertEquals("", OrgUnitSqlCoordinator.joinClause(REGISTRATION_OU, params, sqlBuilder));
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testSelectBuilderJoinAddedForDimension(TrackerOrgUnitDimension dimension) {
    SelectBuilder sb = new SelectBuilder().addColumn("1").from("analytics_event_x", "ax");

    OrgUnitSqlCoordinator.addJoinIfNeeded(
        dimension, sb, dimensionParams(dimension, districtA), sqlBuilder);

    String alias = dimension.getStructAlias();
    assertTrue(
        sb.build()
            .contains(
                "analytics_rs_orgunitstructure "
                    + alias
                    + " on "
                    + alias
                    + ".\"organisationunituid\" = ax.\""
                    + dimension.getAnalyticsColumn()
                    + "\""),
        sb.build());
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testSelectBuilderJoinOmittedWhenUnused(TrackerOrgUnitDimension dimension) {
    SelectBuilder sb = new SelectBuilder().addColumn("1").from("analytics_event_x", "ax");

    OrgUnitSqlCoordinator.addJoinIfNeeded(
        dimension, sb, new EventQueryParams.Builder().build(), sqlBuilder);

    assertFalse(sb.build().contains("analytics_rs_orgunitstructure"), sb.build());
  }

  // -------------------------------------------------------------------------
  // Where predicate
  // -------------------------------------------------------------------------

  @Test
  void testNoPredicateWithoutItems() {
    assertEquals(
        "",
        OrgUnitSqlCoordinator.wherePredicate(
            REGISTRATION_OU, dimensionParams(REGISTRATION_OU), new SqlHelper(), sqlBuilder));
  }

  /** A district matches its whole subtree, not only events recorded at the district itself. */
  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testPredicateForSingleOrgUnitMatchesSubtree(TrackerOrgUnitDimension dimension) {
    assertEquals(
        "where ("
            + dimension.getStructAlias()
            + ".\"uidlevel2\" in ('"
            + districtA.getUid()
            + "')) ",
        OrgUnitSqlCoordinator.wherePredicate(
            dimension, dimensionParams(dimension, districtA), new SqlHelper(), sqlBuilder));
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testPredicateForSingleLevel(TrackerOrgUnitDimension dimension) {
    String alias = dimension.getStructAlias();

    assertEquals(
        "where ("
            + alias
            + ".\"uidlevel2\" in ('"
            + districtA.getUid()
            + "','"
            + districtB.getUid()
            + "')) ",
        OrgUnitSqlCoordinator.wherePredicate(
            dimension,
            dimensionParams(dimension, districtA, districtB),
            new SqlHelper(),
            sqlBuilder));
  }

  /** Items spanning levels union their subtrees, so the per-level predicates are OR-ed. */
  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testPredicateForMixedLevelsIsOred(TrackerOrgUnitDimension dimension) {
    String alias = dimension.getStructAlias();

    assertEquals(
        "where ("
            + alias
            + ".\"uidlevel1\" in ('"
            + root.getUid()
            + "') or "
            + alias
            + ".\"uidlevel2\" in ('"
            + districtA.getUid()
            + "')) ",
        OrgUnitSqlCoordinator.wherePredicate(
            dimension, dimensionParams(dimension, root, districtA), new SqlHelper(), sqlBuilder));
  }

  /** A district and a facility in a filter match either subtree. */
  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testFilterWithMixedLevelsIsOred(TrackerOrgUnitDimension dimension) {
    String alias = dimension.getStructAlias();

    assertEquals(
        "where ("
            + alias
            + ".\"uidlevel2\" in ('"
            + districtB.getUid()
            + "') or "
            + alias
            + ".\"uidlevel4\" in ('"
            + facility.getUid()
            + "')) ",
        OrgUnitSqlCoordinator.wherePredicate(
            dimension, filterParams(dimension, facility, districtB), new SqlHelper(), sqlBuilder));
  }

  /** A dimension and a filter both restrict, so they are AND-ed rather than OR-ed. */
  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testDimensionAndFilterAreAnded(TrackerOrgUnitDimension dimension) {
    String alias = dimension.getStructAlias();

    assertEquals(
        "where ("
            + alias
            + ".\"uidlevel4\" in ('"
            + facility.getUid()
            + "')) and ("
            + alias
            + ".\"uidlevel2\" in ('"
            + districtB.getUid()
            + "')) ",
        OrgUnitSqlCoordinator.wherePredicate(
            dimension,
            params(dimension, List.of(facility), List.of(districtB)),
            new SqlHelper(),
            sqlBuilder));
  }

  /** Each dimension restricts on its own alias only. */
  @Test
  void testPredicateIsPerDimension() {
    EventQueryParams params = dimensionParams(ENROLLMENT_OU, districtA);

    assertEquals(
        "",
        OrgUnitSqlCoordinator.wherePredicate(REGISTRATION_OU, params, new SqlHelper(), sqlBuilder));
  }

  // -------------------------------------------------------------------------
  // Aggregate select / group by
  // -------------------------------------------------------------------------

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testNoAggregateColumnWhenNotAggregated(TrackerOrgUnitDimension dimension) {
    assertTrue(
        OrgUnitSqlCoordinator.dimensionSelectColumn(
                dimension, dimensionParams(dimension, districtA), false, false, sqlBuilder)
            .isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testNoAggregateColumnForFilterOnly(TrackerOrgUnitDimension dimension) {
    assertTrue(
        OrgUnitSqlCoordinator.dimensionSelectColumn(
                dimension, filterParams(dimension, districtA), false, true, sqlBuilder)
            .isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testAggregateGroupByColumn(TrackerOrgUnitDimension dimension) {
    assertEquals(
        Optional.of(dimension.getStructAlias() + ".\"uidlevel2\""),
        OrgUnitSqlCoordinator.dimensionSelectColumn(
            dimension, dimensionParams(dimension, districtA, districtB), true, true, sqlBuilder));
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testAggregateProjectionColumn(TrackerOrgUnitDimension dimension) {
    assertEquals(
        Optional.of(
            dimension.getStructAlias() + ".\"uidlevel2\" as " + dimension.getUidHeader().getItem()),
        OrgUnitSqlCoordinator.dimensionSelectColumn(
            dimension, dimensionParams(dimension, districtA, districtB), false, true, sqlBuilder));
  }

  /** Items below a district group by the level of the items, not of the district. */
  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testAggregateColumnUsesLevelOfItems(TrackerOrgUnitDimension dimension) {
    assertEquals(
        Optional.of(dimension.getStructAlias() + ".\"uidlevel3\""),
        OrgUnitSqlCoordinator.dimensionSelectColumn(
            dimension, dimensionParams(dimension, chiefdom), true, true, sqlBuilder));
  }

  /**
   * One group-by column cannot represent org units at two levels, and an event below both is
   * genuinely ambiguous, so this is rejected rather than silently resolved.
   */
  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testAggregateRejectsMixedLevels(TrackerOrgUnitDimension dimension) {
    EventQueryParams params = dimensionParams(dimension, districtA, facility);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                OrgUnitSqlCoordinator.dimensionSelectColumn(
                    dimension, params, false, true, sqlBuilder));

    assertEquals(ErrorCode.E7261, exception.getErrorCode());
    assertTrue(exception.getMessage().contains(dimension.getDimensionName()));
  }

  // -------------------------------------------------------------------------
  // Query select
  // -------------------------------------------------------------------------

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testNoQueryColumnsWithoutDimension(TrackerOrgUnitDimension dimension) {
    assertTrue(
        OrgUnitSqlCoordinator.querySelectColumns(
                dimension, filterParams(dimension, districtA), sqlBuilder)
            .isEmpty());
  }

  @ParameterizedTest
  @EnumSource(TrackerOrgUnitDimension.class)
  void testQueryColumnsForDimension(TrackerOrgUnitDimension dimension) {
    String alias = dimension.getStructAlias();

    assertEquals(
        List.of(
            alias + ".\"organisationunituid\" as " + dimension.getUidHeader().getItem(),
            alias + ".\"name\" as " + dimension.getNameHeader().getItem()),
        OrgUnitSqlCoordinator.querySelectColumns(
            dimension, dimensionParams(dimension, districtA), sqlBuilder));
  }

  @Test
  void testQueryColumnsForBareRegistrationOuDimension() {
    assertEquals(
        2,
        OrgUnitSqlCoordinator.querySelectColumns(
                REGISTRATION_OU, dimensionParams(REGISTRATION_OU), sqlBuilder)
            .size());
  }

  private EventQueryParams dimensionParams(
      TrackerOrgUnitDimension dimension, OrganisationUnit... items) {
    return params(dimension, List.of(items), List.of());
  }

  private EventQueryParams filterParams(
      TrackerOrgUnitDimension dimension, OrganisationUnit... items) {
    return params(dimension, null, List.of(items));
  }

  /**
   * Builds params with the given dimension and filter items. A null dimension list leaves the
   * dimension out entirely, which differs from an empty one for REGISTRATION_OU.
   */
  private EventQueryParams params(
      TrackerOrgUnitDimension dimension,
      List<OrganisationUnit> dimensionItems,
      List<OrganisationUnit> filterItems) {
    EventQueryParams.Builder builder = new EventQueryParams.Builder();

    if (dimension == REGISTRATION_OU) {
      if (dimensionItems != null) {
        builder.withRegistrationOuDimension(dimensionItems);
      }
      return builder.withRegistrationOuFilter(filterItems).build();
    }

    if (dimensionItems != null) {
      builder.withEnrollmentOuDimension(dimensionItems);
    }
    return builder.withEnrollmentOuFilter(filterItems).build();
  }
}
