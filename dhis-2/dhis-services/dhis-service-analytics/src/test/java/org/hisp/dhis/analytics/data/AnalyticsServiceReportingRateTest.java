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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class AnalyticsServiceReportingRateTest extends AnalyticsServiceBaseTest {
  @Test
  void verifyReportingRatesValueWhenPeriodIsFilter() {
    int timeUnit = 10;
    double expectedReports = 100D;

    DataSet dataSetA = createDataSet('A');
    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);
    ReportingRate reportingRateB = new ReportingRate(dataSetA);
    reportingRateB.setMetric(ReportingRateMetric.ACTUAL_REPORTS);
    ReportingRate reportingRateC = new ReportingRate(dataSetA);
    reportingRateC.setMetric(ReportingRateMetric.EXPECTED_REPORTS);

    List<DimensionalItemObject> periods = new ArrayList<>();

    Stream.iterate(1, i -> i + 1)
        .limit(timeUnit)
        .forEach(
            x ->
                periods.add(
                    new MonthlyPeriodType().createPeriod(new DateTime(2014, x, 1, 0, 0).toDate())));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            // DATA ELEMENTS
            .withDataElements(newArrayList(reportingRateA, reportingRateB, reportingRateC))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(
                singletonList(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
            .build();

    initMock(params);

    Map<String, Object> actualReports = new HashMap<>();
    actualReports.put(dataSetA.getUid() + "-" + ou.getUid(), 500D);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(actualReports));

    Map<String, Object> reportingRate = new HashMap<>();
    reportingRate.put(dataSetA.getUid() + "-" + ou.getUid(), expectedReports);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(reportingRate));

    Grid grid = target.getAggregatedDataValueGrid(params);

    assertEquals(
        expectedReports * timeUnit,
        (Long)
            getValueFromGrid(
                    grid.getRows(), makeKey(dataSetA, ReportingRateMetric.EXPECTED_REPORTS))
                .get(),
        0);
    assertEquals(
        50D,
        (Long)
            getValueFromGrid(grid.getRows(), makeKey(dataSetA, ReportingRateMetric.REPORTING_RATE))
                .get(),
        0);
    assertEquals(
        500D,
        (Long)
            getValueFromGrid(grid.getRows(), makeKey(dataSetA, ReportingRateMetric.ACTUAL_REPORTS))
                .get(),
        0);
  }

  @Test
  void verifyNullValueIsZeroForReportingRate() {
    double expectedReports = 100D;
    DataSet dataSetA = createDataSet('A');
    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate()));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            // DATA ELEMENTS
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(
                singletonList(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
            .build();

    initMock(params);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(null)); // NO
    // VALUES
    Map<String, Object> reportingRate = new HashMap<>();
    reportingRate.put(dataSetA.getUid() + "-" + ou.getUid(), expectedReports);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(reportingRate));

    Grid grid = target.getAggregatedDataValueGrid(params);

    assertEquals(
        0,
        (Long)
            getValueFromGrid(grid.getRows(), makeKey(dataSetA, ReportingRateMetric.REPORTING_RATE))
                .get(),
        0);
  }

  @Test
  void verifyNullTargetIsNullForReportingRate() {
    DataSet dataSetA = createDataSet('A');
    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(new MonthlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate()));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            // DATA ELEMENTS
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(
                singletonList(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
            .build();

    initMock(params);
    Map<String, Object> actualReports = new HashMap<>();
    actualReports.put(dataSetA.getUid() + "-" + ou.getUid(), 500D);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(actualReports));

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(null)); // NO
    // TARGET
    // RETURNED

    Grid grid = target.getAggregatedDataValueGrid(params);

    assertNull(
        getValueFromGrid(grid.getRows(), makeKey(dataSetA, ReportingRateMetric.REPORTING_RATE))
            .orElse(null));
  }

  @Test
  void verifyReportingRatesForMonthsWithLessThen30DaysAreComputedCorrectly() {
    // Create a Dataset with a Daily period type
    DataSet dataSetA = createDataSet('A');
    dataSetA.setPeriodType(PeriodType.getPeriodTypeByName(DailyPeriodType.NAME));

    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    // Set a period for a month with less then 30 days (Feb)
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(PeriodType.getPeriodFromIsoString("201902"));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    // Create request
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            .withPeriods(periods)
            .withFilters(
                singletonList(
                    new BaseDimensionalObject(
                        "ou", DimensionType.ORGANISATION_UNIT, singletonList(ou))))
            .build();

    initMock(params);

    // Response for COMPLETENESS_TARGET
    Map<String, Object> targets = new HashMap<>();
    targets.put(dataSetA.getUid() + "-" + "201902", 1D);

    // Response for COMPLETENESS - set the completeness value to the same
    // number of
    // days of the selected month
    Map<String, Object> actuals = new HashMap<>();
    actuals.put(dataSetA.getUid() + "-" + "201902", 28D);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(targets));

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(actuals));

    Grid grid = target.getAggregatedDataValueGrid(params);
    assertReportingRatesGrid(grid, dataSetA, "201902");
  }

  @Test
  void verifyReportingRatesForMonthsWithMoreThen30DaysAreComputedCorrectly() {
    // Create a Dataset with a Daily period type
    DataSet dataSetA = createDataSet('A');
    dataSetA.setPeriodType(PeriodType.getPeriodTypeByName(DailyPeriodType.NAME));

    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    // Set a period for a month with more then 30 days (Jan)
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(PeriodType.getPeriodFromIsoString("201901"));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    // Create request
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            .withPeriods(periods)
            .withFilters(
                singletonList(
                    new BaseDimensionalObject(
                        "ou", DimensionType.ORGANISATION_UNIT, singletonList(ou))))
            .build();

    initMock(params);

    // Response for COMPLETENESS_TARGET
    Map<String, Object> targets = new HashMap<>();
    targets.put(dataSetA.getUid() + "-" + "201901", 1D);

    // Response for COMPLETENESS - set the completeness value to the same
    // number of
    // days of the selected month
    Map<String, Object> actuals = new HashMap<>();
    actuals.put(dataSetA.getUid() + "-" + "201901", 31D);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(targets));

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(actuals));

    Grid grid = target.getAggregatedDataValueGrid(params);
    assertReportingRatesGrid(grid, dataSetA, "201901");
  }

  private void assertReportingRatesGrid(Grid grid, DataSet dataset, String period) {
    assertThat(grid.getRows(), hasSize(1));
    assertThat(grid.getRow(0), hasSize(3));
    assertThat(grid.getHeaders(), hasSize(3));

    assertThat(
        grid.getRow(0).get(getDimensionIndex(grid.getHeaders(), "dx")),
        is(dataset.getUid() + ".REPORTING_RATE"));
    assertThat(grid.getRow(0).get(getDimensionIndex(grid.getHeaders(), "pe")), is(period));
    assertThat(grid.getRow(0).get(getDimensionIndex(grid.getHeaders(), "value")), is(100L));
  }

  private int getDimensionIndex(List<GridHeader> headers, String dimension) {
    int index = 0;
    for (GridHeader header : headers) {
      if (header.getName().equals(dimension)) {
        return index;
      }
      index++;
    }
    return -1;
  }

  private Optional<Number> getValueFromGrid(List<List<Object>> rows, String key) {
    for (List<Object> row : rows) {
      if (row.get(0).equals(key)) {
        return Optional.of((Number) row.get(2));
      }
    }
    return Optional.empty();
  }

  private String makeKey(DataSet dataSet, ReportingRateMetric reportingRateMetric) {
    return dataSet.getUid() + "." + reportingRateMetric.name();
  }
}
