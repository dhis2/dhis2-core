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
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.common.ColumnHeader.ENROLLMENT_OU_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.ENROLLMENT_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.ENROLLMENT_OU_COLUMN_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.event.data.ou.OrgUnitSqlConstants;
import org.hisp.dhis.analytics.event.data.ou.OrgUnitSqlFragments;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.junit.jupiter.api.Test;

class OrgUnitSqlFragmentsTest {

  private final SqlBuilder pgSqlBuilder = new PostgreSqlBuilder();
  private final SqlBuilder dorisSqlBuilder = new DorisSqlBuilder("pg_dhis", "postgresql.jar");

  @Test
  void testJoinCondition() {
    assertThat(
        OrgUnitSqlFragments.joinCondition("enrl", pgSqlBuilder),
        is("ax.\"enrollment\" = enrl.\"enrollment\""));
  }

  @Test
  void testJoinConditionDoris() {
    assertThat(
        OrgUnitSqlFragments.joinCondition("enrl", dorisSqlBuilder),
        is("ax.`enrollment` = enrl.`enrollment`"));
  }

  @Test
  void testInnerJoinClause() {
    assertEquals(
        "inner join analytics_enrollment_abcdefghijk as enrl on ax.\"enrollment\" = enrl.\"enrollment\" ",
        OrgUnitSqlFragments.innerJoinClause("analytics_enrollment_abcdefghijk", pgSqlBuilder));
  }

  @Test
  void testInnerJoinClauseDoris() {
    assertEquals(
        "inner join analytics_enrollment_abcdefghijk as enrl on ax.`enrollment` = enrl.`enrollment` ",
        OrgUnitSqlFragments.innerJoinClause("analytics_enrollment_abcdefghijk", dorisSqlBuilder));
  }

  @Test
  void testSelectEnrollmentOuUid() {
    assertEquals(
        "enrl.\"ou\" as enrollmentou",
        OrgUnitSqlFragments.selectEnrollmentOuUid(false, pgSqlBuilder));
    assertEquals("enrl.\"ou\"", OrgUnitSqlFragments.selectEnrollmentOuUid(true, pgSqlBuilder));
  }

  @Test
  void testSelectEnrollmentOuUidDoris() {
    assertEquals(
        "enrl.`ou` as enrollmentou",
        OrgUnitSqlFragments.selectEnrollmentOuUid(false, dorisSqlBuilder));
    assertEquals("enrl.`ou`", OrgUnitSqlFragments.selectEnrollmentOuUid(true, dorisSqlBuilder));
  }

  @Test
  void testSelectEnrollmentOuName() {
    assertEquals(
        "enrl.\"ouname\" as enrollmentouname",
        OrgUnitSqlFragments.selectEnrollmentOuName(pgSqlBuilder));
  }

  @Test
  void testSelectEnrollmentOuNameDoris() {
    assertEquals(
        "enrl.`ouname` as enrollmentouname",
        OrgUnitSqlFragments.selectEnrollmentOuName(dorisSqlBuilder));
  }

  @Test
  void testPredicateByUids() {
    assertEquals(
        " enrl.\"ou\" in ('a','b') ", OrgUnitSqlFragments.predicateByUids("'a','b'", pgSqlBuilder));
  }

  @Test
  void testPredicateByUidsDoris() {
    assertEquals(
        " enrl.`ou` in ('a','b') ",
        OrgUnitSqlFragments.predicateByUids("'a','b'", dorisSqlBuilder));
  }

  @Test
  void testPredicateByLevels() {
    assertEquals(
        " enrl.\"oulevel\" in (2,4) ", OrgUnitSqlFragments.predicateByLevels("2,4", pgSqlBuilder));
  }

  @Test
  void testPredicateByLevelsDoris() {
    assertEquals(
        " enrl.`oulevel` in (2,4) ", OrgUnitSqlFragments.predicateByLevels("2,4", dorisSqlBuilder));
  }

  @Test
  void testPredicateByUidLevel() {
    assertEquals(
        "enrl.\"uidlevel3\" in ('uid1','uid2')",
        OrgUnitSqlFragments.predicateByUidLevel(3, "'uid1','uid2'", pgSqlBuilder));
  }

  @Test
  void testPredicateByUidLevelDoris() {
    assertEquals(
        "enrl.`uidlevel3` in ('uid1','uid2')",
        OrgUnitSqlFragments.predicateByUidLevel(3, "'uid1','uid2'", dorisSqlBuilder));
  }

  @Test
  void testConstantsAreMappedFromSharedIdentifiers() {
    assertEquals(ANALYTICS_TBL_ALIAS, OrgUnitSqlConstants.EVENT_TABLE_ALIAS);
    assertEquals(ENROLLMENT_COLUMN_NAME, OrgUnitSqlConstants.ENROLLMENT_JOIN_COLUMN);
    assertEquals(ENROLLMENT_OU_COLUMN_NAME, OrgUnitSqlConstants.EVENT_ENROLLMENT_OU_COLUMN);
    assertEquals(OrgUnitSqlConstants.ENROLLMENT_OU_NAME_RESULT_ALIAS, ENROLLMENT_OU_NAME.getItem());
  }
}
