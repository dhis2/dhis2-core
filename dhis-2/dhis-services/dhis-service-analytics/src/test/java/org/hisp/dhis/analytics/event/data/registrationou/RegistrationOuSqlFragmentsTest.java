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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.junit.jupiter.api.Test;

class RegistrationOuSqlFragmentsTest {

  private final SqlBuilder pgSqlBuilder = new PostgreSqlBuilder();
  private final SqlBuilder dorisSqlBuilder = new DorisSqlBuilder("pg_dhis", "postgresql.jar");

  @Test
  void testJoinCondition() {
    assertEquals(
        "regous.\"organisationunituid\" = ax.\"registrationou\"",
        RegistrationOuSqlFragments.joinCondition("regous", pgSqlBuilder));
  }

  @Test
  void testJoinConditionDoris() {
    assertEquals(
        "regous.`organisationunituid` = ax.`registrationou`",
        RegistrationOuSqlFragments.joinCondition("regous", dorisSqlBuilder));
  }

  @Test
  void testInnerJoinClause() {
    assertEquals(
        "inner join analytics_rs_orgunitstructure as regous "
            + "on regous.\"organisationunituid\" = ax.\"registrationou\" ",
        RegistrationOuSqlFragments.innerJoinClause(pgSqlBuilder));
  }

  @Test
  void testInnerJoinClauseDoris() {
    assertEquals(
        "inner join analytics_rs_orgunitstructure as regous "
            + "on regous.`organisationunituid` = ax.`registrationou` ",
        RegistrationOuSqlFragments.innerJoinClause(dorisSqlBuilder));
  }

  @Test
  void testPredicateByUidLevel() {
    assertEquals(
        "regous.\"uidlevel2\" in ('abcdefghij1','abcdefghij2')",
        RegistrationOuSqlFragments.predicateByUidLevel(
            2, "'abcdefghij1','abcdefghij2'", pgSqlBuilder));
  }

  @Test
  void testPredicateByUidLevelDoris() {
    assertEquals(
        "regous.`uidlevel2` in ('abcdefghij1')",
        RegistrationOuSqlFragments.predicateByUidLevel(2, "'abcdefghij1'", dorisSqlBuilder));
  }

  @Test
  void testSelectUidLevelForGroupBy() {
    assertEquals(
        "regous.\"uidlevel3\"", RegistrationOuSqlFragments.selectUidLevel(3, true, pgSqlBuilder));
  }

  @Test
  void testSelectUidLevelForProjection() {
    assertEquals(
        "regous.\"uidlevel3\" as registrationou",
        RegistrationOuSqlFragments.selectUidLevel(3, false, pgSqlBuilder));
  }

  @Test
  void testSelectRegistrationOuUid() {
    assertEquals(
        "regous.\"organisationunituid\" as registrationou",
        RegistrationOuSqlFragments.selectRegistrationOuUid(pgSqlBuilder));
  }

  @Test
  void testSelectRegistrationOuName() {
    assertEquals(
        "regous.\"name\" as registrationouname",
        RegistrationOuSqlFragments.selectRegistrationOuName(pgSqlBuilder));
  }

  @Test
  void testSelectRegistrationOuNameDoris() {
    assertEquals(
        "regous.`name` as registrationouname",
        RegistrationOuSqlFragments.selectRegistrationOuName(dorisSqlBuilder));
  }
}
