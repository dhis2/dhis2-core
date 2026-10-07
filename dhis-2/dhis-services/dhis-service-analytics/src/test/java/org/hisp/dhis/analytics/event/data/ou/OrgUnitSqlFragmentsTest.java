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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrgUnitSqlFragmentsTest {

  private final SqlBuilder pgSqlBuilder = new PostgreSqlBuilder();
  private final SqlBuilder dorisSqlBuilder = new DorisSqlBuilder("pg_dhis", "postgresql.jar");

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationou", "ENROLLMENT_OU,enrous,enrollmentou"})
  void testJoinCondition(TrackerOrgUnitDimension dimension, String alias, String column) {
    assertEquals(
        alias + ".\"organisationunituid\" = ax.\"" + column + "\"",
        OrgUnitSqlFragments.joinCondition(dimension, alias, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationou", "ENROLLMENT_OU,enrous,enrollmentou"})
  void testJoinConditionDoris(TrackerOrgUnitDimension dimension, String alias, String column) {
    assertEquals(
        alias + ".`organisationunituid` = ax.`" + column + "`",
        OrgUnitSqlFragments.joinCondition(dimension, alias, dorisSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationou", "ENROLLMENT_OU,enrous,enrollmentou"})
  void testInnerJoinClause(TrackerOrgUnitDimension dimension, String alias, String column) {
    assertEquals(
        "inner join analytics_rs_orgunitstructure as "
            + alias
            + " on "
            + alias
            + ".\"organisationunituid\" = ax.\""
            + column
            + "\" ",
        OrgUnitSqlFragments.innerJoinClause(dimension, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationou", "ENROLLMENT_OU,enrous,enrollmentou"})
  void testInnerJoinClauseDoris(TrackerOrgUnitDimension dimension, String alias, String column) {
    assertEquals(
        "inner join analytics_rs_orgunitstructure as "
            + alias
            + " on "
            + alias
            + ".`organisationunituid` = ax.`"
            + column
            + "` ",
        OrgUnitSqlFragments.innerJoinClause(dimension, dorisSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous", "ENROLLMENT_OU,enrous"})
  void testPredicateByUidLevel(TrackerOrgUnitDimension dimension, String alias) {
    assertEquals(
        alias + ".\"uidlevel2\" in ('abcdefghij1','abcdefghij2')",
        OrgUnitSqlFragments.predicateByUidLevel(
            dimension, 2, "'abcdefghij1','abcdefghij2'", pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous", "ENROLLMENT_OU,enrous"})
  void testPredicateByUidLevelDoris(TrackerOrgUnitDimension dimension, String alias) {
    assertEquals(
        alias + ".`uidlevel2` in ('abcdefghij1')",
        OrgUnitSqlFragments.predicateByUidLevel(dimension, 2, "'abcdefghij1'", dorisSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous", "ENROLLMENT_OU,enrous"})
  void testSelectUidLevelForGroupBy(TrackerOrgUnitDimension dimension, String alias) {
    assertEquals(
        alias + ".\"uidlevel3\"",
        OrgUnitSqlFragments.selectUidLevel(dimension, 3, true, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationou", "ENROLLMENT_OU,enrous,enrollmentou"})
  void testSelectUidLevelForProjection(
      TrackerOrgUnitDimension dimension, String alias, String header) {
    assertEquals(
        alias + ".\"uidlevel3\" as " + header,
        OrgUnitSqlFragments.selectUidLevel(dimension, 3, false, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationou", "ENROLLMENT_OU,enrous,enrollmentou"})
  void testSelectUid(TrackerOrgUnitDimension dimension, String alias, String header) {
    assertEquals(
        alias + ".\"organisationunituid\" as " + header,
        OrgUnitSqlFragments.selectUid(dimension, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationouname", "ENROLLMENT_OU,enrous,enrollmentouname"})
  void testSelectName(TrackerOrgUnitDimension dimension, String alias, String header) {
    assertEquals(
        alias + ".\"name\" as " + header, OrgUnitSqlFragments.selectName(dimension, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,regous,registrationouname", "ENROLLMENT_OU,enrous,enrollmentouname"})
  void testSelectNameDoris(TrackerOrgUnitDimension dimension, String alias, String header) {
    assertEquals(
        alias + ".`name` as " + header, OrgUnitSqlFragments.selectName(dimension, dorisSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({
    "REGISTRATION_OU,regous,registrationou,organisationunituid",
    "REGISTRATION_OU,regous,registrationouname,name",
    "ENROLLMENT_OU,enrous,enrollmentou,organisationunituid",
    "ENROLLMENT_OU,enrous,enrollmentouname,name"
  })
  void testSortColumn(TrackerOrgUnitDimension dimension, String alias, String item, String column) {
    assertEquals(
        Optional.of(alias + ".\"" + column + "\""),
        OrgUnitSqlFragments.sortColumn(dimension, item, pgSqlBuilder));
  }

  @ParameterizedTest
  @CsvSource({"REGISTRATION_OU,enrollmentou", "ENROLLMENT_OU,registrationou", "ENROLLMENT_OU,ou"})
  void testSortColumnIgnoresOtherItems(TrackerOrgUnitDimension dimension, String item) {
    assertEquals(Optional.empty(), OrgUnitSqlFragments.sortColumn(dimension, item, pgSqlBuilder));
  }
}
