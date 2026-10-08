/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.data;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.test.TestBase.createDataSet;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.user.SystemUser;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class AnalyticsServiceReportingRateTest extends AnalyticsServiceBaseTest {
  @BeforeEach
  void setUp() {
    injectSecurityContextNoSettings(new SystemUser());
  }

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
                    PeriodDimension.of(
                        new MonthlyPeriodType()
                            .createPeriod(new DateTime(2014, x, 1, 0, 0).toDate()))));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            // DATA ELEMENTS
            .withDataElements(List.of(reportingRateA, reportingRateB, reportingRateC))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(List.of(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
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
        (expectedReports * timeUnit),
        (Long)
            getValueFromGrid(
                    grid.getRows(), makeKey(dataSetA, ReportingRateMetric.EXPECTED_REPORTS))
                .get(),
        0);
    assertEquals(
        50L,
        (Long)
            getValueFromGrid(grid.getRows(), makeKey(dataSetA, ReportingRateMetric.REPORTING_RATE))
                .get(),
        0);
    assertEquals(
        500L,
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
    periods.add(
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate())));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            // DATA ELEMENTS
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(List.of(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
            .build();

    initMock(params);

    // NO VALUES
    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(null));

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
        0L);
  }

  @Test
  void verifyNullTargetIsNullForReportingRate() {
    DataSet dataSetA = createDataSet('A');
    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate())));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            // DATA ELEMENTS
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            // FILTERS (OU)
            .withFilters(List.of(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
            .build();

    initMock(params);
    Map<String, Object> actualReports = new HashMap<>();
    actualReports.put(dataSetA.getUid() + "-" + ou.getUid(), 500D);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(actualReports));

    // NO TARGET RETURNED
    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(null));

    Grid grid = target.getAggregatedDataValueGrid(params);

    assertNull(
        getValueFromGrid(grid.getRows(), makeKey(dataSetA, ReportingRateMetric.REPORTING_RATE))
            .orElse(null));
  }

  @Test
  void verifyReportingRatesForMonthsWithLessThen30DaysAreComputedCorrectly() {
    // Create a Dataset with a Daily period type
    DataSet dataSetA = createDataSet('A');
    dataSetA.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    // Set a period for a month with less then 30 days (Feb)
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(PeriodDimension.of("201902"));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    // Create request
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            .withPeriods(periods)
            .withFilters(
                List.of(
                    new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ou))))
            .build();

    initMock(params);

    // Response for COMPLETENESS_TARGET
    Map<String, Object> targets = new HashMap<>();
    targets.put(dataSetA.getUid() + "-" + "201902", 1D);

    // Response for COMPLETENESS - set the completeness value to the same
    // number of days of the selected month
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
    dataSetA.setPeriodType(PeriodType.getPeriodType(PeriodTypeEnum.DAILY));

    ReportingRate reportingRateA = new ReportingRate(dataSetA);
    reportingRateA.setMetric(ReportingRateMetric.REPORTING_RATE);

    // Set a period for a month with more then 30 days (Jan)
    List<DimensionalItemObject> periods = new ArrayList<>();
    periods.add(PeriodDimension.of("201901"));

    OrganisationUnit ou = new OrganisationUnit("aaaa");

    // Create request
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(newArrayList(reportingRateA))
            .withIgnoreLimit(true)
            .withPeriods(periods)
            .withFilters(
                List.of(
                    new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ou))))
            .build();

    initMock(params);

    // Response for COMPLETENESS_TARGET
    Map<String, Object> targets = new HashMap<>();
    targets.put(dataSetA.getUid() + "-" + "201901", 1D);

    // Response for COMPLETENESS - set the completeness value to the same
    // number of days of the selected month
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

  @Test
  void verifyNumDenForRateMetricsHasPercentMultiplierAndDivisor() {
    DataSet dataSetA = createDataSet('A');
    OrganisationUnit ou = new OrganisationUnit("aaaa");

    Grid grid =
        getNumDenGrid(
            dataSetA,
            ou,
            10,
            true,
            500D,
            100D,
            ReportingRateMetric.REPORTING_RATE,
            ReportingRateMetric.REPORTING_RATE_ON_TIME);

    for (ReportingRateMetric metric :
        List.of(ReportingRateMetric.REPORTING_RATE, ReportingRateMetric.REPORTING_RATE_ON_TIME)) {
      List<Object> row = getRowByDx(grid, makeKey(dataSetA, metric));

      assertEquals(50L, getValue(grid, row, "value"), metric.name());
      assertEquals(500D, getValue(grid, row, "numerator"), metric.name());
      assertEquals(1000D, getValue(grid, row, "denominator"), metric.name());
      assertEquals(100D, getValue(grid, row, "factor"), metric.name());
      assertEquals(100, getValue(grid, row, "multiplier"), metric.name());
      assertEquals(1, getValue(grid, row, "divisor"), metric.name());
    }
  }

  @Test
  void verifyNumDenForCountMetricsHasUnitFactorMultiplierAndDivisor() {
    DataSet dataSetA = createDataSet('A');
    OrganisationUnit ou = new OrganisationUnit("aaaa");

    Grid grid =
        getNumDenGrid(
            dataSetA,
            ou,
            10,
            true,
            500D,
            100D,
            ReportingRateMetric.ACTUAL_REPORTS,
            ReportingRateMetric.ACTUAL_REPORTS_ON_TIME,
            ReportingRateMetric.EXPECTED_REPORTS);

    for (ReportingRateMetric metric :
        List.of(
            ReportingRateMetric.ACTUAL_REPORTS,
            ReportingRateMetric.ACTUAL_REPORTS_ON_TIME,
            ReportingRateMetric.EXPECTED_REPORTS)) {
      List<Object> row = getRowByDx(grid, makeKey(dataSetA, metric));

      assertEquals(1D, getValue(grid, row, "factor"), metric.name());
      assertEquals(1, getValue(grid, row, "multiplier"), metric.name());
      assertEquals(1, getValue(grid, row, "divisor"), metric.name());
    }

    // Numerator and denominator are always the actual and the expected reports
    List<Object> expected =
        getRowByDx(grid, makeKey(dataSetA, ReportingRateMetric.EXPECTED_REPORTS));
    assertEquals(1000L, getValue(grid, expected, "value"));
    assertEquals(0D, getValue(grid, expected, "numerator"));
    assertEquals(1000D, getValue(grid, expected, "denominator"));

    List<Object> actual = getRowByDx(grid, makeKey(dataSetA, ReportingRateMetric.ACTUAL_REPORTS));
    assertEquals(500L, getValue(grid, actual, "value"));
    assertEquals(500D, getValue(grid, actual, "numerator"));
    assertEquals(1000D, getValue(grid, actual, "denominator"));
  }

  @Test
  void verifyNumDenWhenReportingRateIsCappedAt100() {
    DataSet dataSetA = createDataSet('A');
    OrganisationUnit ou = new OrganisationUnit("aaaa");

    // More actual reports than expected: the rate is capped, numerator and
    // denominator are not
    Grid grid =
        getNumDenGrid(dataSetA, ou, 1, true, 150D, 100D, ReportingRateMetric.REPORTING_RATE);

    List<Object> row = getRowByDx(grid, makeKey(dataSetA, ReportingRateMetric.REPORTING_RATE));

    assertEquals(100L, getValue(grid, row, "value"));
    assertEquals(150D, getValue(grid, row, "numerator"));
    assertEquals(100D, getValue(grid, row, "denominator"));
    assertEquals(100D, getValue(grid, row, "factor"));
    assertEquals(100, getValue(grid, row, "multiplier"));
    assertEquals(1, getValue(grid, row, "divisor"));
  }

  @Test
  void verifyNoNumDenColumnsWhenIncludeNumDenIsFalse() {
    DataSet dataSetA = createDataSet('A');
    OrganisationUnit ou = new OrganisationUnit("aaaa");

    Grid grid =
        getNumDenGrid(dataSetA, ou, 1, false, 50D, 100D, ReportingRateMetric.REPORTING_RATE);

    assertThat(grid.getHeaders(), hasSize(3));
    assertThat(grid.getRows(), hasSize(1));
    assertThat(grid.getRow(0), hasSize(3));
    assertEquals(-1, getDimensionIndex(grid.getHeaders(), "multiplier"));
    assertEquals(-1, getDimensionIndex(grid.getHeaders(), "divisor"));
  }

  /**
   * Runs an aggregated query for the given reporting rate metrics of a single data set and org
   * unit, using a period filter with the given number of monthly periods.
   */
  private Grid getNumDenGrid(
      DataSet dataSet,
      OrganisationUnit ou,
      int timeUnits,
      boolean includeNumDen,
      double actualReports,
      double expectedReportsPerPeriod,
      ReportingRateMetric... metrics) {
    List<DimensionalItemObject> reportingRates = new ArrayList<>();

    for (ReportingRateMetric metric : metrics) {
      ReportingRate reportingRate = new ReportingRate(dataSet);
      reportingRate.setMetric(metric);
      reportingRates.add(reportingRate);
    }

    List<DimensionalItemObject> periods = new ArrayList<>();

    Stream.iterate(1, i -> i + 1)
        .limit(timeUnits)
        .forEach(
            x ->
                periods.add(
                    PeriodDimension.of(
                        new MonthlyPeriodType()
                            .createPeriod(new DateTime(2014, x, 1, 0, 0).toDate()))));

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withOrganisationUnit(ou)
            .withDataElements(reportingRates)
            .withIgnoreLimit(true)
            .withIncludeNumDen(includeNumDen)
            .withFilters(List.of(new BaseDimensionalObject("pe", DimensionType.PERIOD, periods)))
            .build();

    initMock(params);

    Map<String, Object> actuals = new HashMap<>();
    actuals.put(dataSet.getUid() + "-" + ou.getUid(), actualReports);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(actuals));

    Map<String, Object> targets = new HashMap<>();
    targets.put(dataSet.getUid() + "-" + ou.getUid(), expectedReportsPerPeriod);

    when(analyticsManager.getAggregatedDataValues(
            any(DataQueryParams.class), eq(AnalyticsTableType.COMPLETENESS_TARGET), eq(0)))
        .thenReturn(CompletableFuture.completedFuture(targets));

    return target.getAggregatedDataValueGrid(params);
  }

  private List<Object> getRowByDx(Grid grid, String dx) {
    int dxIndex = getDimensionIndex(grid.getHeaders(), "dx");

    return grid.getRows().stream()
        .filter(row -> dx.equals(row.get(dxIndex)))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No row found for dx: " + dx));
  }

  private Object getValue(Grid grid, List<Object> row, String header) {
    int index = getDimensionIndex(grid.getHeaders(), header);
    assertNotEquals(-1, index, "Missing header: " + header);
    return row.get(index);
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
