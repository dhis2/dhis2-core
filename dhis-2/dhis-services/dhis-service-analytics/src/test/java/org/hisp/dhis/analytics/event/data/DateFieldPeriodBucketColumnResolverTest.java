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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionConstants;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DateFieldPeriodBucketColumnResolverTest {
  private final DateFieldPeriodBucketColumnResolver subject =
      new DateFieldPeriodBucketColumnResolver(new PostgreSqlBuilder());

  @Test
  void shouldResolveSelectExpressionForEnrollmentDateYearlyBucket() {
    String sql =
        subject
            .resolve(
                AnalyticsType.EVENT,
                periodDimension("2021", TimeField.ENROLLMENT_DATE.name()),
                false)
            .orElseThrow();

    assertEquals(
        "(select \"yearly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('year', ax.\"enrollmentdate\")::date) as \"pe\"",
        sql);
  }

  @ParameterizedTest
  @MethodSource("groupByCases")
  void shouldResolveGroupByExpression(String isoPeriod, String dateField, String expectedSql) {
    String sql =
        subject
            .resolve(AnalyticsType.EVENT, periodDimension(isoPeriod, dateField), true)
            .orElseThrow();

    assertEquals(expectedSql, sql);
  }

  private static Stream<Arguments> groupByCases() {
    return Stream.of(
        Arguments.of(
            "2022W1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"weekly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\")::date)"),
        Arguments.of(
            "202301",
            TimeField.ENROLLMENT_DATE.name(),
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"enrollmentdate\")::date)"),
        Arguments.of(
            "2022",
            TimeField.INCIDENT_DATE.name(),
            "(select \"yearly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('year', ax.\"occurreddate\")::date)"),
        Arguments.of(
            "2022W1",
            TimeField.LAST_UPDATED.name(),
            "(select \"weekly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"lastupdated\")::date)"),
        Arguments.of(
            "202301",
            TimeField.CREATED.name(),
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"created\")::date)"),
        Arguments.of(
            "20230115",
            TimeField.ENROLLMENT_DATE.name(),
            "(select \"daily\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = ax.\"enrollmentdate\"::date)"),
        Arguments.of(
            "202301",
            DimensionConstants.COMPLETED,
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"completeddate\")::date)"),
        Arguments.of(
            "202701B",
            TimeField.INCIDENT_DATE.name(),
            "(select \"bimonthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( extract(year from ax.\"occurreddate\")::int, ((extract(month from ax.\"occurreddate\")::int - 1) / 2) * 2 + 1, 1 ))"),
        Arguments.of(
            "2027S1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"sixmonthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( extract(year from ax.\"occurreddate\")::int, case when extract(month from ax.\"occurreddate\") <= 6 then 1 else 7 end, 1 ))"),
        Arguments.of(
            "2027BiW1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"biweekly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\")::date - ((extract(week from ax.\"occurreddate\")::int - 1) % 2) * interval '7 days')"),
        Arguments.of(
            "2027Q1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"quarterly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('quarter', ax.\"occurreddate\")::date)"),
        Arguments.of(
            "2027NovQ1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"quarterlynov\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('quarter', ax.\"occurreddate\" - interval '1 month')::date + interval '1 month')"),
        Arguments.of(
            "2027WedW1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"weeklywednesday\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\" + interval '5 days')::date - interval '5 days')"),
        Arguments.of(
            "2027ThuW1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"weeklythursday\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\" + interval '4 days')::date - interval '4 days')"),
        Arguments.of(
            "2027FriW1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"weeklyfriday\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\" + interval '3 days')::date - interval '3 days')"),
        Arguments.of(
            "2027SatW1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"weeklysaturday\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\" + interval '2 days')::date - interval '2 days')"),
        Arguments.of(
            "2027SunW1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"weeklysunday\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('week', ax.\"occurreddate\" + interval '1 day')::date - interval '1 day')"),
        Arguments.of(
            "2021AprilS1",
            TimeField.CREATED.name(),
            "(select \"sixmonthlyapril\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = case when extract(month from ax.\"created\") between 4 and 9 then make_date(extract(year from ax.\"created\")::int, 4, 1) when extract(month from ax.\"created\") >= 10 then make_date(extract(year from ax.\"created\")::int, 10, 1) else make_date(extract(year from ax.\"created\")::int - 1, 10, 1) end)"),
        Arguments.of(
            "2027NovS1",
            TimeField.INCIDENT_DATE.name(),
            "(select \"sixmonthlynov\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = case when extract(month from ax.\"occurreddate\") between 5 and 10 then make_date(extract(year from ax.\"occurreddate\")::int, 5, 1) when extract(month from ax.\"occurreddate\") >= 11 then make_date(extract(year from ax.\"occurreddate\")::int, 11, 1) else make_date(extract(year from ax.\"occurreddate\")::int - 1, 11, 1) end)"),
        Arguments.of(
            "2027Feb",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialfeb\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 2 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 2, 1 ))"),
        Arguments.of(
            "2027April",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialapril\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 4 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 4, 1 ))"),
        Arguments.of(
            "2027July",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialjuly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 7 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 7, 1 ))"),
        Arguments.of(
            "2027Aug",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialaug\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 8 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 8, 1 ))"),
        Arguments.of(
            "2027Sep",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialsep\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 9 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 9, 1 ))"),
        Arguments.of(
            "2027Oct",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialoct\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 10 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 10, 1 ))"),
        Arguments.of(
            "2027Nov",
            TimeField.INCIDENT_DATE.name(),
            "(select \"financialnov\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = make_date( case when extract(month from ax.\"occurreddate\") >= 11 then extract(year from ax.\"occurreddate\")::int else extract(year from ax.\"occurreddate\")::int - 1 end, 11, 1 ))"),
        Arguments.of(
            "2021AprilS1",
            TimeField.ENROLLMENT_DATE.name(),
            "(select \"sixmonthlyapril\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = case when extract(month from ax.\"enrollmentdate\") between 4 and 9 then make_date(extract(year from ax.\"enrollmentdate\")::int, 4, 1) when extract(month from ax.\"enrollmentdate\") >= 10 then make_date(extract(year from ax.\"enrollmentdate\")::int, 10, 1) else make_date(extract(year from ax.\"enrollmentdate\")::int - 1, 10, 1) end)"));
  }

  // --- Empty-result guard clause tests ---

  @Test
  void shouldReturnEmptyForNonPeriodDimension() {
    DimensionalObject dimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of());

    assertTrue(subject.resolve(AnalyticsType.EVENT, dimension, true).isEmpty());
  }

  @Test
  void shouldReturnEmptyForOccurredDateField() {
    DimensionalObject dimension = periodDimension("202301", TimeField.OCCURRED_DATE.name());

    assertTrue(subject.resolve(AnalyticsType.EVENT, dimension, true).isEmpty());
  }

  @Test
  void shouldReturnEmptyWhenDateFieldIsNull() {
    PeriodDimension period = PeriodDimension.of("202301").setDateField(null);
    PeriodType periodType = PeriodType.getPeriodTypeFromIsoString("202301");
    period.getPeriod().setPeriodType(periodType);
    DimensionalObject dimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(period));

    assertTrue(subject.resolve(AnalyticsType.EVENT, dimension, true).isEmpty());
  }

  @Test
  void shouldReturnEmptyWhenMultipleDateFieldsPresent() {
    PeriodDimension p1 =
        PeriodDimension.of("202301").setDateField(TimeField.ENROLLMENT_DATE.name());
    p1.getPeriod().setPeriodType(PeriodType.getPeriodTypeFromIsoString("202301"));

    PeriodDimension p2 = PeriodDimension.of("202302").setDateField(TimeField.LAST_UPDATED.name());
    p2.getPeriod().setPeriodType(PeriodType.getPeriodTypeFromIsoString("202302"));

    DimensionalObject dimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(p1, p2));

    assertTrue(subject.resolve(AnalyticsType.EVENT, dimension, true).isEmpty());
  }

  @Test
  void shouldReturnEmptyWhenMultiplePeriodTypesPresent() {
    PeriodDimension p1 =
        PeriodDimension.of("202301").setDateField(TimeField.ENROLLMENT_DATE.name());
    p1.getPeriod().setPeriodType(PeriodType.getPeriodTypeFromIsoString("202301"));

    PeriodDimension p2 = PeriodDimension.of("2023").setDateField(TimeField.ENROLLMENT_DATE.name());
    p2.getPeriod().setPeriodType(PeriodType.getPeriodTypeFromIsoString("2023"));

    DimensionalObject dimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(p1, p2));

    assertTrue(subject.resolve(AnalyticsType.EVENT, dimension, true).isEmpty());
  }

  @Test
  void shouldReturnEmptyForUnresolvableTimeField() {
    PeriodDimension period = PeriodDimension.of("202301").setDateField("BOGUS_FIELD");
    PeriodType periodType = PeriodType.getPeriodTypeFromIsoString("202301");
    period.getPeriod().setPeriodType(periodType);
    DimensionalObject dimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(period));

    assertTrue(subject.resolve(AnalyticsType.EVENT, dimension, true).isEmpty());
  }

  // --- AnalyticsType.ENROLLMENT tests ---

  @Test
  void shouldUseEnrollmentColumnNameForEnrollmentAnalyticsType() {
    String sql =
        subject
            .resolve(
                AnalyticsType.ENROLLMENT,
                periodDimension("202301", TimeField.ENROLLMENT_DATE.name()),
                true)
            .orElseThrow();

    assertEquals(
        "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period"
            + " where dps_period.\"dateperiod\" = date_trunc('month', ax.\"enrollmentdate\")::date)",
        sql);
  }

  @Test
  void shouldUseEnrollmentOccurredDateColumnForIncidentDateWithEnrollmentType() {
    String sql =
        subject
            .resolve(
                AnalyticsType.ENROLLMENT,
                periodDimension("2022", TimeField.INCIDENT_DATE.name()),
                true)
            .orElseThrow();

    assertEquals(
        "(select \"yearly\" from analytics_rs_dateperiodstructure as dps_period"
            + " where dps_period.\"dateperiod\" = date_trunc('year', ax.\"occurreddate\")::date)",
        sql);
  }

  // --- Custom table alias test ---

  @Test
  void shouldResolveWithCustomTableAlias() {
    DateFieldPeriodBucketColumnResolver.ResolvedExpression resolved =
        subject
            .resolve(
                AnalyticsType.EVENT,
                periodDimension("202301", TimeField.ENROLLMENT_DATE.name()),
                "t1")
            .orElseThrow();

    assertTrue(resolved.selectExpression().contains("t1.\"enrollmentdate\""));
    assertTrue(resolved.groupByExpression().contains("t1.\"enrollmentdate\""));
    assertEquals("enrollmentdate", resolved.sourceColumn());
  }

  // --- resolveSourceColumn tests ---

  @Test
  void shouldResolveSourceColumnForEventType() {
    String column =
        subject
            .resolveSourceColumn(
                AnalyticsType.EVENT, periodDimension("202301", TimeField.ENROLLMENT_DATE.name()))
            .orElseThrow();

    assertEquals("enrollmentdate", column);
  }

  @Test
  void shouldResolveSourceColumnForEnrollmentType() {
    String column =
        subject
            .resolveSourceColumn(
                AnalyticsType.ENROLLMENT, periodDimension("202301", TimeField.LAST_UPDATED.name()))
            .orElseThrow();

    assertEquals("lastupdated", column);
  }

  @Test
  void shouldReturnEmptySourceColumnForNonPeriodDimension() {
    DimensionalObject dimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of());

    assertTrue(subject.resolveSourceColumn(AnalyticsType.EVENT, dimension).isEmpty());
  }

  @Test
  void shouldReturnEmptySourceColumnForOccurredDate() {
    Optional<String> result =
        subject.resolveSourceColumn(
            AnalyticsType.EVENT, periodDimension("202301", TimeField.OCCURRED_DATE.name()));

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptySourceColumnWhenNoDateField() {
    PeriodDimension period = PeriodDimension.of("202301").setDateField(null);
    PeriodType periodType = PeriodType.getPeriodTypeFromIsoString("202301");
    period.getPeriod().setPeriodType(periodType);
    DimensionalObject dimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(period));

    assertTrue(subject.resolveSourceColumn(AnalyticsType.EVENT, dimension).isEmpty());
  }

  // --- Select expression coverage for non-YEARLY period types ---

  @Test
  void shouldResolveSelectExpressionForMonthlyBucket() {
    String sql =
        subject
            .resolve(
                AnalyticsType.EVENT,
                periodDimension("202301", TimeField.ENROLLMENT_DATE.name()),
                false)
            .orElseThrow();

    assertEquals(
        "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period"
            + " where dps_period.\"dateperiod\" = date_trunc('month', ax.\"enrollmentdate\")::date)"
            + " as \"pe\"",
        sql);
  }

  private static DimensionalObject periodDimension(String isoPeriod, String dateField) {
    PeriodDimension period = PeriodDimension.of(isoPeriod).setDateField(dateField);
    PeriodType periodType = PeriodType.getPeriodTypeFromIsoString(isoPeriod);
    period.getPeriod().setPeriodType(periodType);

    return new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of(period));
  }
}
