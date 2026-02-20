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
import static org.hisp.dhis.analytics.AnalyticsConstants.ORG_UNIT_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.common.ColumnHeader.ENROLLMENT_OU_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.ENROLLMENT_OU_COLUMN_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.event.data.ou.OrgUnitSqlConstants;
import org.hisp.dhis.analytics.event.data.ou.OrgUnitSqlFragments;
import org.junit.jupiter.api.Test;

class OrgUnitSqlFragmentsTest {

  @Test
  void testJoinCondition() {
    assertThat(
        OrgUnitSqlFragments.joinCondition("ous"),
        is("ax.\"enrollmentou\" = ous.\"organisationunituid\""));
  }

  @Test
  void testInnerJoinClause() {
    assertEquals(
        "inner join analytics_rs_orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" ",
        OrgUnitSqlFragments.innerJoinClause());
  }

  @Test
  void testSelectEnrollmentOuUid() {
    assertEquals(
        "ous.\"organisationunituid\" as enrollmentou",
        OrgUnitSqlFragments.selectEnrollmentOuUid(false));
    assertEquals("ous.\"organisationunituid\"", OrgUnitSqlFragments.selectEnrollmentOuUid(true));
  }

  @Test
  void testSelectEnrollmentOuName() {
    assertEquals("ous.\"name\" as enrollmentouname", OrgUnitSqlFragments.selectEnrollmentOuName());
  }

  @Test
  void testPredicates() {
    assertEquals(
        " ous.\"organisationunituid\" in ('a','b') ",
        OrgUnitSqlFragments.predicateByUids("'a','b'"));
    assertEquals(" ous.\"level\" in (2,4) ", OrgUnitSqlFragments.predicateByLevels("2,4"));
  }

  @Test
  void testConstantsAreMappedFromSharedIdentifiers() {
    assertEquals(ANALYTICS_TBL_ALIAS, OrgUnitSqlConstants.EVENT_TABLE_ALIAS);
    assertEquals(ORG_UNIT_STRUCT_ALIAS, OrgUnitSqlConstants.ORG_UNIT_STRUCTURE_ALIAS);
    assertEquals(ENROLLMENT_OU_COLUMN_NAME, OrgUnitSqlConstants.EVENT_ENROLLMENT_OU_COLUMN);
    assertEquals(ENROLLMENT_OU_NAME.getItem(), OrgUnitSqlConstants.ENROLLMENT_OU_NAME_RESULT_ALIAS);
  }
}
