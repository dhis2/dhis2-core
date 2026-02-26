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
package org.hisp.dhis.analytics.event.data.enrollmentou;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ou.OrgUnitSqlCoordinator;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

class OrgUnitSqlCoordinatorTest {

  private final AnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  @Test
  void testAddJoinIfNeededNoOpWhenNoEnrollmentOu() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('A')).build();
    SelectBuilder sb = new SelectBuilder();
    sb.from("analytics_event_test", "ax");

    OrgUnitSqlCoordinator.addJoinIfNeeded(sb, params);

    assertThat(sb.build(), not(containsString("enrl")));
  }

  @Test
  void testAddJoinIfNeededWhenEnrollmentOuPresent() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    Program program = createProgram('A');
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .withEnrollmentOuFilter(List.of(ouA))
            .build();
    SelectBuilder sb = new SelectBuilder();
    sb.from("analytics_event_test", "ax");

    OrgUnitSqlCoordinator.addJoinIfNeeded(sb, params);

    String sql = sb.build();
    assertThat(sql, containsString("analytics_enrollment_" + program.getUid().toLowerCase()));
    assertThat(sql, containsString("ax.\"enrollment\" = enrl.\"enrollment\""));
  }

  @Test
  void testAddDimensionSelectColumnsAggregateEventOnly() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEnrollmentOuDimension(List.of(ouA))
            .build();
    List<String> columns = new ArrayList<>();

    OrgUnitSqlCoordinator.addDimensionSelectColumns(
        columns, params, false, true, AnalyticsType.EVENT);

    assertThat(columns, hasSize(1));
    assertThat(columns.get(0), is("enrl.\"ou\" as enrollmentou"));
  }

  @Test
  void testAddDimensionSelectColumnsNoOpWhenNotEventAggregate() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEnrollmentOuDimension(List.of(ouA))
            .build();
    List<String> columns = new ArrayList<>();

    OrgUnitSqlCoordinator.addDimensionSelectColumns(
        columns, params, false, false, AnalyticsType.EVENT);
    OrgUnitSqlCoordinator.addDimensionSelectColumns(
        columns, params, false, true, AnalyticsType.ENROLLMENT);

    assertThat(columns, hasSize(0));
  }

  @Test
  void testAddQuerySelectColumnsWhenEnrollmentOuDimension() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEnrollmentOuDimension(List.of(ouA))
            .build();
    List<String> columns = new ArrayList<>();

    OrgUnitSqlCoordinator.addQuerySelectColumns(columns, params);

    assertThat(columns, hasSize(2));
    assertThat(columns.get(0), is("enrl.\"ou\" as enrollmentou"));
    assertThat(columns.get(1), is("enrl.\"ouname\" as enrollmentouname"));
  }

  @Test
  void testAppendWherePredicateIfNeededNoOpWhenNoEnrollmentOu() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(createProgram('A')).build();
    StringBuilder sql = new StringBuilder();

    OrgUnitSqlCoordinator.appendWherePredicateIfNeeded(sql, new SqlHelper(), params, sqlBuilder);

    assertThat(sql.toString(), is(""));
  }

  @Test
  void testAppendWherePredicateIfNeededWithUidAndLevelConstraints() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B', ouA);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEnrollmentOuFilter(List.of(ouA, ouB))
            .withEnrollmentOuFilterLevels(new LinkedHashSet<>(List.of(2, 4)))
            .build();

    StringBuilder sql = new StringBuilder();
    OrgUnitSqlCoordinator.appendWherePredicateIfNeeded(sql, new SqlHelper(), params, sqlBuilder);

    String where = sql.toString();
    assertThat(where, containsString("where ("));
    assertThat(where, containsString("enrl.\"uidlevel1\" in ('" + ouA.getUid() + "')"));
    assertThat(where, containsString("enrl.\"uidlevel2\" in ('" + ouB.getUid() + "')"));
    assertThat(where, containsString("enrl.\"oulevel\" in (2,4)"));
    assertThat(where, containsString(" or "));
  }

  @Test
  void testAppendWherePredicateIfNeededGroupsOusByLevel() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(createProgram('A'))
            .withEnrollmentOuFilter(List.of(ouA, ouB))
            .build();

    StringBuilder sql = new StringBuilder();
    OrgUnitSqlCoordinator.appendWherePredicateIfNeeded(sql, new SqlHelper(), params, sqlBuilder);

    String where = sql.toString();
    assertThat(where, containsString("enrl.\"uidlevel1\" in ("));
    assertThat(where, containsString("'" + ouA.getUid() + "'"));
    assertThat(where, containsString("'" + ouB.getUid() + "'"));
  }

  @Test
  void testAppendLegacyJoinWhenEnrollmentOuPresent() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    Program program = createProgram('A');
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .withEnrollmentOuFilter(List.of(ouA))
            .build();
    StringBuilder sql = new StringBuilder();

    OrgUnitSqlCoordinator.appendLegacyJoin(sql, params);

    String result = sql.toString();
    assertThat(result, containsString("analytics_enrollment_" + program.getUid().toLowerCase()));
    assertThat(result, containsString("enrl"));
    assertThat(result, containsString("ax.\"enrollment\" = enrl.\"enrollment\""));
  }
}
