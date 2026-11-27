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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQueryItemClassifier;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StageDatePeriodTypePlannerTest extends TestBase {
  private final StageDatePeriodTypePlanner subject =
      new StageDatePeriodTypePlanner(new DefaultStageQueryItemClassifier());

  private Program program;
  private ProgramStage stage;
  private OrganisationUnit orgUnit;

  @BeforeEach
  void setUp() {
    program = createProgram('A');
    stage = createProgramStage('A', program);
    orgUnit = createOrganisationUnit('A');
  }

  @ParameterizedTest
  @ValueSource(strings = {"occurreddate", "scheduleddate"})
  void shouldPlanOneQueryPerPeriodTypeOfStageDateDimension(String dateColumn) {
    QueryItem dateItem = stageDateItem(dateColumn, "2021-01-01", "2022-03-31", "2021", "2022Q1");
    EventQueryParams params = params().addItem(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(
        List.of("[2021] GE:2021-01-01 LE:2021-12-31", "[2022Q1] GE:2022-01-01 LE:2022-03-31"),
        queries.stream().map(query -> describe(query.getItems().get(0))).toList());
  }

  @Test
  void shouldPlanOverlappingPeriodsOfDifferentTypesInSeparateQueries() {
    QueryItem dateItem =
        stageDateItem("occurreddate", "2022-01-01", "2022-12-31", "2022", "2022Q1");
    EventQueryParams params = params().addItem(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(
        List.of("[2022] GE:2022-01-01 LE:2022-12-31", "[2022Q1] GE:2022-01-01 LE:2022-03-31"),
        queries.stream().map(query -> describe(query.getItems().get(0))).toList());
  }

  @Test
  void shouldGroupPeriodsOfTheSameTypeInOneQuery() {
    QueryItem dateItem =
        stageDateItem(
            "occurreddate", "2021-11-01", "2022-12-31", "202111", "202112", "202201", "2022");
    EventQueryParams params = params().addItem(dateItem).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(
        List.of(
            "[202111, 202112, 202201] GE:2021-11-01 LE:2022-01-31",
            "[2022] GE:2022-01-01 LE:2022-12-31"),
        queries.stream().map(query -> describe(query.getItems().get(0))).toList());
  }

  @Test
  void shouldKeepTheRequestedStageDateItemUnchanged() {
    QueryItem dateItem =
        stageDateItem("occurreddate", "2021-01-01", "2022-03-31", "2021", "2022Q1");
    EventQueryParams params = params().addItem(dateItem).build();

    subject.plan(params);

    assertEquals("[2021, 2022Q1] GE:2021-01-01 LE:2022-03-31", describe(dateItem));
    assertSame(dateItem, params.getItems().get(0));
  }

  @Test
  void shouldKeepTheItemPositionAmongOtherItems() {
    DataElement dataElement = createDataElement('A');
    QueryItem before = new QueryItem(dataElement);
    QueryItem after = new QueryItem(createDataElement('B'));
    QueryItem dateItem =
        stageDateItem("occurreddate", "2021-01-01", "2022-03-31", "2021", "2022Q1");
    EventQueryParams params = params().addItem(before).addItem(dateItem).addItem(after).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(2, queries.size());
    for (EventQueryParams query : queries) {
      assertEquals(3, query.getItems().size());
      assertSame(before, query.getItems().get(0));
      assertEquals(dateItem, query.getItems().get(1));
      assertSame(after, query.getItems().get(2));
    }
  }

  @Test
  void shouldPlanEachCombinationOfPeriodTypesOfTwoStageDateDimensions() {
    QueryItem eventDate =
        stageDateItem("occurreddate", "2021-01-01", "2022-03-31", "2021", "2022Q1");
    QueryItem scheduledDate =
        stageDateItem("scheduleddate", "2022-01-01", "2022-12-31", "2022", "202201");
    EventQueryParams params = params().addItem(eventDate).addItem(scheduledDate).build();

    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(
        List.of(
            List.of("[2021] GE:2021-01-01 LE:2021-12-31", "[2022] GE:2022-01-01 LE:2022-12-31"),
            List.of("[2021] GE:2021-01-01 LE:2021-12-31", "[202201] GE:2022-01-01 LE:2022-01-31"),
            List.of("[2022Q1] GE:2022-01-01 LE:2022-03-31", "[2022] GE:2022-01-01 LE:2022-12-31"),
            List.of(
                "[2022Q1] GE:2022-01-01 LE:2022-03-31", "[202201] GE:2022-01-01 LE:2022-01-31")),
        queries.stream()
            .map(query -> query.getItems().stream().map(this::describe).toList())
            .toList());
  }

  @Test
  void shouldKeepQueryWhenStageDateHasPeriodsOfOneType() {
    QueryItem dateItem = stageDateItem("occurreddate", "2021-01-01", "2022-12-31", "2021", "2022");
    EventQueryParams params = params().addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryWhenStageDateHasNoPeriods() {
    QueryItem dateItem = stageDateItem("occurreddate", "2021-01-01", "2021-03-31");
    EventQueryParams params = params().addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  /** A filter restricts without a group-by column, so its periods stay in one query. */
  @Test
  void shouldKeepQueryWhenStageDateFilterHasPeriodsOfDifferentTypes() {
    QueryItem dateItem =
        stageDateItem("occurreddate", "2021-01-01", "2022-03-31", "2021", "2022Q1");
    EventQueryParams params = params().addItemFilter(dateItem).build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryWhenStageDateHasValueThatIsNotAPeriod() {
    QueryItem dateItem =
        stageDateItem("occurreddate", "2021-01-01", "2022-03-31", "2021", "2022Q1", "notAPeriod");
    EventQueryParams params = params().addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  @Test
  void shouldKeepQueryWhenDateItemHasNoStage() {
    QueryItem dateItem =
        stageDateItem("occurreddate", "2021-01-01", "2022-03-31", "2021", "2022Q1");
    dateItem.setProgramStage(null);
    EventQueryParams params = params().addItem(dateItem).build();

    assertKeepsQuery(params);
  }

  private void assertKeepsQuery(EventQueryParams params) {
    List<EventQueryParams> queries = subject.plan(params);

    assertEquals(1, queries.size());
    assertSame(params, queries.get(0));
  }

  private String describe(QueryItem item) {
    return item.getDimensionValues()
        + " "
        + String.join(
            " ",
            item.getFilters().stream()
                .map(filter -> filter.getOperator() + ":" + filter.getFilter())
                .toList());
  }

  /** Creates a stage date item as parsed from {@code stageUid.EVENT_DATE:<periods>}. */
  private QueryItem stageDateItem(String dateColumn, String start, String end, String... periods) {
    QueryItem item =
        new QueryItem(
            new BaseDimensionalItemObject(dateColumn),
            program,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    item.setProgramStage(stage);
    item.addFilter(new QueryFilter(QueryOperator.GE, start));
    item.addFilter(new QueryFilter(QueryOperator.LE, end));
    for (String period : periods) {
      item.addDimensionValue(period);
    }
    return item;
  }

  private EventQueryParams.Builder params() {
    return new EventQueryParams.Builder()
        .withProgram(program)
        .withOrganisationUnits(List.of(orgUnit));
  }
}
