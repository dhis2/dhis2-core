/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.data;

import static java.util.Calendar.JANUARY;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.program.AnalyticsType.EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

/**
 * {@see OrgUnitTableJoiner} Tester.
 *
 * @author Jim Grace
 */
class OrgUnitTableJoinerTest extends DhisConvenienceTest {
  private static final OrgUnitField DEFAULT = new OrgUnitField(null);

  private static final OrgUnitField ATTRIBUTE = new OrgUnitField("AttributeId");

  private static final OrgUnitField OWNER_AT_START = new OrgUnitField("OWNER_AT_START");

  private static final OrgUnitField OWNER_AT_END = new OrgUnitField("OWNER_AT_END");

  private static final Program programA = createProgram('A');

  private static final Period periodDaily = PeriodType.getPeriodFromIsoString("20230101");

  private static final Period periodMonthly = PeriodType.getPeriodFromIsoString("202201");

  private static final Period periodQuarterly = PeriodType.getPeriodFromIsoString("2022Q1");

  private static final Date dateA = new GregorianCalendar(2022, JANUARY, 1).getTime();

  private static final Date dateB = new GregorianCalendar(2023, JANUARY, 1).getTime();

  private static final DimensionalItemObject ouA = createOrganisationUnit('A');

  private static final DimensionalObject ouGroupSetA =
      new BaseDimensionalObject(
          "OrgUnitGrSe", DimensionType.ORGANISATION_UNIT_GROUP_SET, emptyList());

  @Test
  void testJoinOrgUnitTablesDefault() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrgUnitField(DEFAULT)
            .withPeriods(
                List.of(periodMonthly), periodMonthly.getPeriodType().getName().toLowerCase())
            .addDimension(ouGroupSetA)
            .build();

    assertEquals("", joinOrgUnitTables(params, EVENT));

    assertEquals("", joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesAttribute() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(ATTRIBUTE)
            .withPeriods(
                List.of(periodMonthly), periodMonthly.getPeriodType().getName().toLowerCase())
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join _orgunitstructure as ous on ax.\"AttributeId\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join _orgunitstructure as ous on ax.\"AttributeId\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesOwnerAtStartWithDailyPeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(OWNER_AT_START)
            .withPeriods(List.of(periodDaily), periodDaily.getPeriodType().getName().toLowerCase())
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2023-01-01' between own.\"startdate\" and own.\"enddate\" "
            + "left join _orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2023-01-01' between own.\"startdate\" and own.\"enddate\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesOwnerAtStartWithNonDailyPeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(OWNER_AT_START)
            .withPeriods(
                List.of(periodMonthly), periodMonthly.getPeriodType().getName().toLowerCase())
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-01-01' between own.\"startdate\" and own.\"enddate\" "
            + "left join _orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-01-01' between own.\"startdate\" and own.\"enddate\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesOwnerAtStartWithStartAndEndDates() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(OWNER_AT_START)
            .withStartDate(dateA)
            .withEndDate(dateB)
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-01-01' between own.\"startdate\" and own.\"enddate\" "
            + "left join _orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-01-01' between own.\"startdate\" and own.\"enddate\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesOwnerAtEndWithDailyPeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(OWNER_AT_END)
            .withPeriods(List.of(periodDaily), periodDaily.getPeriodType().getName().toLowerCase())
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2023-01-02' between own.\"startdate\" and own.\"enddate\" "
            + "left join _orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2023-01-02' between own.\"startdate\" and own.\"enddate\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesOwnerAtEndWithNonDailyPeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(OWNER_AT_END)
            .withPeriods(
                List.of(periodMonthly), periodMonthly.getPeriodType().getName().toLowerCase())
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-02-01' between own.\"startdate\" and own.\"enddate\" "
            + "left join _orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-02-01' between own.\"startdate\" and own.\"enddate\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }

  @Test
  void testJoinOrgUnitTablesOwnerAtEndWithDateRangeByDateFilter() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(programA)
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(OWNER_AT_END)
            .withPeriods(
                List.of(periodQuarterly), periodQuarterly.getPeriodType().getName().toLowerCase())
            .withStartEndDatesForPeriods()
            .addDimension(ouGroupSetA)
            .build();

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-04-01' between own.\"startdate\" and own.\"enddate\" "
            + "left join _orgunitstructure as ous on ax.\"enrollmentou\" = ous.\"organisationunituid\" "
            + "left join _organisationunitgroupsetstructure as ougs on ous.\"organisationunitid\" = ougs.\"organisationunitid\" ",
        joinOrgUnitTables(params, EVENT));

    assertEquals(
        "left join analytics_ownership_prabcdefgha as own on ax.\"tei\" = own.\"teiuid\" "
            + "and '2022-04-01' between own.\"startdate\" and own.\"enddate\" ",
        joinOrgUnitTables(params, ENROLLMENT));
  }
}
