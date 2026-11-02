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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQueryItemClassifier;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FirstOrLastStagePeriodPlannerTest extends TestBase {
  private final FirstOrLastStagePeriodPlanner subject =
      new FirstOrLastStagePeriodPlanner(new DefaultStageQueryItemClassifier());

  private Program program;
  private ProgramStage stage;
  private DataElement dataElement;
  private OrganisationUnit orgUnit;

  @BeforeEach
  void setUp() {
    program = createProgram('A');
    stage = createProgramStage('A', program);
    dataElement = createDataElement('A');
    orgUnit = createOrganisationUnit('A');
  }

  @ParameterizedTest
  @EnumSource(
      value = AggregationType.class,
      names = {
        "FIRST", "FIRST_AVERAGE_ORG_UNIT", "FIRST_FIRST_ORG_UNIT",
        "LAST", "LAST_AVERAGE_ORG_UNIT", "LAST_LAST_ORG_UNIT"
      })
  void shouldPlanOneQueryPerStageDatePeriod(AggregationType aggregationType) {
    QueryItem dateItem = createStageEventDateItem(stage, "202601", "202602");
    dateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    dateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-02-28"));
    EventQueryParams params = createParams(aggregationType).addItem(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(
        List.of("202601", "202602"),
        queries.stream().map(q -> q.getFirstOrLastStagePeriod().isoPeriod()).toList());
    assertTrue(
        queries.stream().allMatch(q -> q.getFirstOrLastStagePeriod().dateItem() == dateItem));
    assertTrue(queries.stream().allMatch(EventQueryParams::isSkipPartitioning));
    assertNull(params.getFirstOrLastStagePeriod());
    assertNull(params.getStartDate());
    assertNull(params.getEndDate());
    assertEquals(List.of("202601", "202602"), dateItem.getDimensionValues());
    assertEquals(2, dateItem.getFilters().size());
  }

  @Test
  void shouldPlanRepeatedStageDatePeriodOnce() {
    QueryItem dateItem = createStageEventDateItem(stage, "202602", "202602");
    EventQueryParams params = createParams(AggregationType.LAST).addItem(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(1, queries.size());
    assertEquals("202602", queries.get(0).getFirstOrLastStagePeriod().isoPeriod());
  }

  @Test
  void shouldPlanOneQueryEndingAtLatestFilterPeriod() {
    QueryItem dateItem = createStageEventDateItem(stage, "202201", "202202");
    EventQueryParams params = createParams(AggregationType.LAST).addItemFilter(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(1, queries.size());
    assertEquals("202202", queries.get(0).getFirstOrLastStagePeriod().isoPeriod());
    assertSame(dateItem, queries.get(0).getFirstOrLastStagePeriod().dateItem());
    assertTrue(queries.get(0).isSkipPartitioning());
  }

  @Test
  void shouldIgnoreGapsBetweenFilterPeriods() {
    assertEquals("202204", planFilterPeriod("202201", "202204"));
  }

  @Test
  void shouldPickLatestEndingFilterPeriodRegardlessOfOrder() {
    assertEquals("202204", planFilterPeriod("202204", "202201"));
  }

  @Test
  void shouldPickLatestEndingFilterPeriodAcrossPeriodTypes() {
    assertEquals("2022", planFilterPeriod("2022", "202206"));
  }

  @ParameterizedTest
  @EnumSource(
      value = AggregationType.class,
      names = {"SUM", "AVERAGE", "COUNT", "LAST_IN_PERIOD", "LAST_IN_PERIOD_AVERAGE_ORG_UNIT"})
  void shouldKeepQueryForOtherAggregations(AggregationType aggregationType) {
    QueryItem dateItem = createStageEventDateItem(stage, "202601", "202602");
    EventQueryParams params = createParams(aggregationType).addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryForOtherEndpoints() {
    QueryItem dateItem = createStageEventDateItem(stage, "202601", "202602");
    EventQueryParams params =
        createParams(AggregationType.LAST)
            .addItem(dateItem)
            .withEndpointItem(EndpointItem.ENROLLMENT)
            .build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryWithoutValueDimension() {
    QueryItem dateItem = createStageEventDateItem(stage, "202601", "202602");
    EventQueryParams params =
        createParams(AggregationType.LAST).addItem(dateItem).withValue(null).build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryWhenStageDateHasNoPeriods() {
    QueryItem dateItem = createStageEventDateItem(stage);
    dateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-02-01"));
    dateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-02-28"));
    EventQueryParams params = createParams(AggregationType.LAST).addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryWhenStageDateBelongsToAnotherStage() {
    QueryItem dateItem =
        createStageEventDateItem(createProgramStage('B', program), "202601", "202602");
    EventQueryParams params = createParams(AggregationType.LAST).addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  private String planFilterPeriod(String... periods) {
    QueryItem dateItem = createStageEventDateItem(stage, periods);
    EventQueryParams params = createParams(AggregationType.LAST).addItemFilter(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(1, queries.size());
    return queries.get(0).getFirstOrLastStagePeriod().isoPeriod();
  }

  private void assertKeepsQuery(EventQueryParams params) {
    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(1, queries.size());
    assertSame(params, queries.get(0));
  }

  private QueryItem createStageEventDateItem(ProgramStage programStage, String... periods) {
    QueryItem item =
        new QueryItem(
            new BaseDimensionalItemObject("occurreddate"),
            program,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    item.setProgramStage(programStage);
    for (String period : periods) {
      item.addDimensionValue(period);
    }
    return item;
  }

  private EventQueryParams.Builder createParams(AggregationType aggregationType) {
    return new EventQueryParams.Builder()
        .withProgram(program)
        .withProgramStage(stage)
        .withValue(dataElement)
        .withEndpointItem(EndpointItem.EVENT)
        .withAggregationType(AnalyticsAggregationType.fromAggregationType(aggregationType))
        .withOrganisationUnits(List.of(orgUnit));
  }
}
