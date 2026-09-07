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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.ClickHouseAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventItemSelectColumnResolverTest {
  private final Program program = program();

  private final ProgramStage programStage = programStage();

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @Test
  void stageOuItemEmitsValueNameAndCodeColumnsWhenHeadersRequestThem() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(stageOuItem())
            .withHeaders(Set.of("stageA.ouname", "stageA.oucode"))
            .build();
    when(organisationUnitResolver.buildStageOuCteContext(
            same(params.getItems().get(0)), same(params), same("enrl")))
        .thenReturn(new OrganisationUnitResolver.StageOuCteContext("enrl.\"uidlevel1\"", "", ""));

    List<String> columns =
        resolver(
                new PostgreSqlAnalyticsSqlBuilder(),
                (item, queryParams) -> ColumnAndAlias.ofColumnAndAlias("unused", "unused"),
                (target, queryParams, item, columnAndAlias) -> {},
                queryParams -> "enrl")
            .resolve(params, new CteContext(EndpointItem.EVENT));

    assertEquals(
        List.of(
            "enrl.\"uidlevel1\" as \"stageA.ou\"",
            "enrl.\"ouname\" as \"stageA.ouname\"",
            "enrl.\"oucode\" as \"stageA.oucode\""),
        columns);
    verify(organisationUnitResolver)
        .buildStageOuCteContext(same(params.getItems().get(0)), same(params), same("enrl"));
  }

  @Test
  void clickHouseProgramIndicatorBackedByCteKeepsRequestOrder() {
    QueryItem piItem = programIndicatorItem("piFirst");
    QueryItem inlineItem = dataElementItem("deSecond");
    EventQueryParams params =
        new EventQueryParams.Builder().addItem(piItem).addItem(inlineItem).build();
    CteContext cteContext = new CteContext(EndpointItem.EVENT);
    cteContext.addProgramIndicatorCte(
        (ProgramIndicator) piItem.getItem(), "select 1", false, "event");
    CteDefinition cteDef = cteContext.getDefinitionByItemUid("piFirst");

    List<String> columns =
        clickHouseResolver(item -> item == piItem ? "piFirst" : "deSecond")
            .resolve(params, cteContext);

    assertEquals(
        List.of(cteDef.getAlias() + ".value as piFirst", "\"deSecond\" as \"deSecond\""), columns);
    verifyNoInteractions(organisationUnitResolver);
  }

  @Test
  void clickHouseProgramIndicatorBackedByCoalescedCteUsesCoalesce() {
    QueryItem piItem = programIndicatorItem("piCount");
    EventQueryParams params = new EventQueryParams.Builder().addItem(piItem).build();
    CteContext cteContext = new CteContext(EndpointItem.EVENT);
    cteContext.addProgramIndicatorCte(
        (ProgramIndicator) piItem.getItem(), "select 1", true, "event");
    CteDefinition cteDef = cteContext.getDefinitionByItemUid("piCount");

    List<String> columns = clickHouseResolver(item -> "piCount").resolve(params, cteContext);

    assertEquals(List.of("coalesce(" + cteDef.getAlias() + ".value, 0) as piCount"), columns);
  }

  @Test
  void clickHouseNonProgramIndicatorFallsBackToInlineColumnAndAppendsRowContext() {
    QueryItem item = dataElementItem("deInline");
    EventQueryParams params = new EventQueryParams.Builder().addItem(item).build();

    List<String> columns =
        clickHouseResolver(
                queryItem -> {
                  assertEquals(item, queryItem);
                  return "deInline";
                },
                (target, queryParams, queryItem, columnAndAlias) ->
                    target.add(columnAndAlias.getAlias() + ".exists"))
            .resolve(params, new CteContext(EndpointItem.EVENT));

    assertEquals(List.of("\"deInline\" as \"deInline\"", "deInline.exists"), columns);
  }

  @Test
  void correlatedSubqueryPathSkipsCteBackedColumnsAndStillAppendsRowContext() {
    QueryItem item = programIndicatorItem("piCte");
    EventQueryParams params = new EventQueryParams.Builder().addItem(item).build();
    CteContext cteContext = new CteContext(EndpointItem.EVENT);
    cteContext.addProgramIndicatorCte(
        (ProgramIndicator) item.getItem(), "select 1", false, "event");

    List<String> columns =
        resolver(
                new PostgreSqlAnalyticsSqlBuilder(),
                (queryItem, queryParams) -> ColumnAndAlias.ofColumnAndAlias("select 1", "piCte"),
                (target, queryParams, queryItem, columnAndAlias) ->
                    target.add(columnAndAlias.getAlias() + ".status"),
                queryParams -> "ax")
            .resolve(params, cteContext);

    assertEquals(List.of("piCte.status"), columns);
  }

  private EventItemSelectColumnResolver clickHouseResolver(ItemAliasResolver itemAliasResolver) {
    return clickHouseResolver(
        itemAliasResolver, (target, queryParams, queryItem, columnAndAlias) -> {});
  }

  private EventItemSelectColumnResolver clickHouseResolver(
      ItemAliasResolver itemAliasResolver,
      EventItemSelectColumnResolver.RowContextAppender appender) {
    return resolver(
        new ClickHouseAnalyticsSqlBuilder("dhis2"),
        (item, queryParams) ->
            ColumnAndAlias.ofColumnAndAlias(
                '"' + itemAliasResolver.resolve(item) + '"', itemAliasResolver.resolve(item)),
        appender,
        queryParams -> "ax");
  }

  private EventItemSelectColumnResolver resolver(
      AnalyticsSqlBuilder sqlBuilder,
      EventItemSelectColumnResolver.ColumnResolver columnResolver,
      EventItemSelectColumnResolver.RowContextAppender rowContextAppender,
      EventItemSelectColumnResolver.StageOuTableAliasResolver stageOuTableAliasResolver) {
    return new EventItemSelectColumnResolver(
        sqlBuilder,
        organisationUnitResolver,
        stageOuTableAliasResolver,
        columnResolver,
        rowContextAppender);
  }

  private QueryItem stageOuItem() {
    QueryItem queryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME),
            program,
            null,
            ValueType.ORGANISATION_UNIT,
            AggregationType.NONE,
            null);
    queryItem.setProgramStage(programStage);
    return queryItem;
  }

  private QueryItem dataElementItem(String uid) {
    DataElement dataElement = new DataElement(uid);
    dataElement.setUid(uid);
    dataElement.setValueType(ValueType.INTEGER);
    dataElement.setAggregationType(AggregationType.SUM);
    return new QueryItem(dataElement, program, null, ValueType.INTEGER, AggregationType.SUM, null);
  }

  private QueryItem programIndicatorItem(String uid) {
    ProgramIndicator programIndicator = new ProgramIndicator();
    programIndicator.setUid(uid);
    programIndicator.setProgram(program);
    programIndicator.setAnalyticsType(AnalyticsType.EVENT);
    programIndicator.setAggregationType(AggregationType.SUM);
    return new QueryItem(
        programIndicator, program, null, ValueType.NUMBER, AggregationType.SUM, null);
  }

  @FunctionalInterface
  private interface ItemAliasResolver {
    String resolve(QueryItem item);
  }

  private static Program program() {
    Program program = new Program("programA");
    program.setUid("programA");
    return program;
  }

  private ProgramStage programStage() {
    ProgramStage stage = new ProgramStage("stageA", program);
    stage.setUid("stageA");
    return stage;
  }
}
