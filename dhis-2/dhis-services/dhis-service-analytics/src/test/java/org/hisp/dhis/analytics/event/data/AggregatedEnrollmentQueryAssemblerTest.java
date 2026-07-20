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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

/**
 * Direct unit tests for {@link AggregatedEnrollmentQueryAssembler}. Pin the SQL fragments produced
 * by the column-building helpers extracted from {@code JdbcEnrollmentAnalyticsManager} so the move
 * preserves byte-identical output. End-to-end coverage of the orchestrator path is left to {@code
 * EnrollmentAnalyticsManagerCteTest}.
 */
class AggregatedEnrollmentQueryAssemblerTest {

  private final PostgreSqlAnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  private final AggregatedEnrollmentQueryAssembler assembler =
      new AggregatedEnrollmentQueryAssembler(
          sqlBuilder,
          new DateFieldPeriodBucketColumnResolver(sqlBuilder),
          new DefaultStageDatePeriodBucketSqlRenderer(sqlBuilder));

  @Test
  void addAggregatedColumnsEmitsCountValue() {
    SelectBuilder sb = new SelectBuilder().from("eb_table", "eb");

    assembler.addAggregatedColumns(sb);

    assertThat(sb.build(), containsString("count(eb.enrollment) as value"));
  }

  @Test
  void isInfrastructureHeaderRecognisesValuePeriodAndOrgUnit() {
    assertTrue(assembler.isInfrastructureHeader("value"));
    assertTrue(assembler.isInfrastructureHeader("VALUE"));
    assertTrue(assembler.isInfrastructureHeader("pe"));
    assertTrue(assembler.isInfrastructureHeader("ou"));
  }

  @Test
  void isInfrastructureHeaderRejectsOtherHeaders() {
    assertFalse(assembler.isInfrastructureHeader("enrollmentdate"));
    assertFalse(assembler.isInfrastructureHeader("eventdate"));
    assertFalse(assembler.isInfrastructureHeader("oucode"));
  }

  @Test
  void resolveHeaderColumnQuotesPlainNames() {
    assertEquals("\"created\"", assembler.resolveHeaderColumn("created"));
  }

  @Test
  void resolveHeaderColumnMapsProgramStatusToEnrollmentStatus() {
    // PROGRAM_STATUS is an alias for ENROLLMENT_STATUS — the resolver collapses to the actual
    // physical column "enrollmentstatus".
    String resolved = assembler.resolveHeaderColumn("programstatus");

    assertThat(resolved, containsString("enrollmentstatus"));
  }

  @Test
  void addStandardColumnsWithoutShadowCteEmitsAxAlias() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);

    assembler.addStandardColumns(sb, cteContext, List.of("enrollment", "trackedentity"));

    String sql = sb.build();
    assertThat(sql, containsString("ax.enrollment"));
    assertThat(sql, containsString("ax.trackedentity"));
  }

  @Test
  void addStandardColumnsFormulaColumnInPlainPathIsAddedVerbatim() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    String formula = "ST_AsGeoJSON(enrollmentgeometry)";

    assembler.addStandardColumns(sb, cteContext, List.of(formula));

    String sql = sb.build();
    // Formula columns are emitted verbatim (no ax. prefix) in the plain path.
    assertThat(sql, containsString(formula));
    assertThat(sql, not(containsString("ax." + formula)));
  }

  @Test
  void addStandardColumnsWithShadowCtePrefixesPlainColumnsWithAx() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = shadowCteContext();

    assembler.addStandardColumns(sb, cteContext, List.of("enrollment", "trackedentity"));

    String sql = sb.build();
    assertThat(sql, containsString("ax.enrollment"));
    assertThat(sql, containsString("ax.trackedentity"));
  }

  @Test
  void addStandardColumnsWithShadowCteRewritesGeometryFormulaToAlias() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = shadowCteContext();
    String formula = "ST_AsGeoJSON(enrollmentgeometry)";

    assembler.addStandardColumns(sb, cteContext, List.of(formula));

    String sql = sb.build();
    // Shadow path swaps the formula for the registered alias and ax-prefixes it.
    assertThat(sql, containsString("ax.enrollmentgeometry_geojson"));
    assertThat(sql, not(containsString(formula)));
  }

  @Test
  void addStandardColumnsWithShadowCteRewritesUnknownFormulaToStableDefaultAlias() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = shadowCteContext();
    String formula = "coalesce(enrollmentdate, created)";

    assembler.addStandardColumns(sb, cteContext, List.of(formula));

    String sql = sb.build();
    String expectedAlias = "formula_" + (formula.hashCode() & Integer.MAX_VALUE);
    assertThat(sql, containsString("ax." + expectedAlias));
    assertThat(sql, not(containsString(formula)));
  }

  @Test
  void addOrgUnitAggregateColumnsEmitsDefaultOuWhenNoLevelColumnsAndNotAggregateEnrollment() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    EventQueryParams params = mock(EventQueryParams.class);

    assembler.addOrgUnitAggregateColumns(sb, params);

    String sql = sb.build();
    assertThat(sql, containsString("ou"));
    assertThat(sql, containsString("group by"));
  }

  @Test
  void addOrgUnitAggregateColumnsEmitsLevelColumnsWhenOuModeRequiresThem() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    OrganisationUnit organisationUnit = new OrganisationUnit("OrgTest");
    organisationUnit.setPath("/Level1/OrgTest");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnitMode(CAPTURE)
            .addDimension(
                new BaseDimensionalObject(
                    ORGUNIT_DIM_ID, ORGANISATION_UNIT, List.of(organisationUnit)))
            .build();

    assembler.addOrgUnitAggregateColumns(sb, params);

    String sql = sb.build();
    assertThat(sql, containsString("uidlevel2"));
    assertThat(sql, containsString("group by uidlevel2"));
    assertThat(sql, not(containsString("group by ou")));
  }

  @Test
  void addOrgUnitAggregateColumnsSkipsDefaultOuForAggregateEnrollmentWithoutOuDimensions() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withEndpointAction(AGGREGATE)
            .withEndpointItem(org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT)
            .build();

    assembler.addOrgUnitAggregateColumns(sb, params);

    String sql = sb.build();
    assertThat(sql, not(containsString("ou")));
    assertThat(sql, not(containsString("group by")));
  }

  @Test
  void addPeriodAggregateColumnsUsesPeriodColumnsWhenNoProjectionExists() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, List.of(month("202301"))))
            .build();

    assembler.addPeriodAggregateColumns(params, sb, List.of());

    String sql = sb.build();
    assertThat(sql, containsString("monthly"));
    assertThat(sql, containsString("group by monthly"));
    assertThat(sql, not(containsString("t1.monthly")));
  }

  @Test
  void addPeriodAggregateColumnsAddsResolvedExpressions() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    EventQueryParams params = mock(EventQueryParams.class);
    DateFieldPeriodBucketColumnResolver.ResolvedExpression expression =
        new DateFieldPeriodBucketColumnResolver.ResolvedExpression(
            "date_trunc('month', eb.\"enrollmentdate\") as \"enrollmentdate\"",
            "date_trunc('month', eb.\"enrollmentdate\")",
            "enrollmentdate",
            Optional.empty());

    assembler.addPeriodAggregateColumns(
        params,
        sb,
        List.of(
            new AggregatedEnrollmentQueryAssembler.PeriodProjection(
                "enrollmentdate", Optional.of(expression))));

    String sql = sb.build();
    assertThat(
        sql, containsString("date_trunc('month', eb.\"enrollmentdate\") as \"enrollmentdate\""));
    assertThat(sql, containsString("group by date_trunc('month', eb.\"enrollmentdate\")"));
  }

  @Test
  void addPeriodAggregateColumnsQuotesProjectionKeysWithoutResolvedExpressions() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    EventQueryParams params = mock(EventQueryParams.class);

    assembler.addPeriodAggregateColumns(
        params,
        sb,
        List.of(
            new AggregatedEnrollmentQueryAssembler.PeriodProjection(
                "scheduleddate", Optional.empty())));

    String sql = sb.build();
    assertThat(sql, containsString("\"scheduleddate\""));
    assertThat(sql, containsString("group by \"scheduleddate\""));
  }

  @Test
  void addHeaderAggregateColumnsSkipsInfrastructureAndPeriodHeaders() {
    SelectBuilder sb = new SelectBuilder().from("enrollment_aggr_base", "eb");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject(
                    PERIOD_DIM_ID,
                    PERIOD,
                    "monthly",
                    "Period",
                    List.of(month("202301").setDateField(TimeField.ENROLLMENT_DATE.name()))))
            .build();
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);

    assembler.addHeaderAggregateColumns(
        List.of(
            new GridHeader("value"),
            new GridHeader("ou"),
            new GridHeader("pe"),
            new GridHeader("enrollmentdate"),
            new GridHeader("plain")),
        params,
        cteContext,
        sb,
        List.of(
            new AggregatedEnrollmentQueryAssembler.PeriodProjection("monthly", Optional.empty())));

    String sql = sb.build();
    assertThat(sql, containsString("\"plain\""));
    assertThat(sql, not(containsString("\"enrollmentdate\"")));
    assertThat(sql, not(containsString("\"value\"")));
    assertThat(sql, not(containsString("\"ou\"")));
    assertThat(sql, not(containsString("\"pe\"")));
  }

  @Test
  void addHeaderAggregateColumnsUsesStageFilterCteWhenPresent() {
    SelectBuilder sb = new SelectBuilder().from("enrollment_aggr_base", "eb");
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_stage1", "select 1");

    assembler.addHeaderAggregateColumns(
        List.of(new GridHeader("stage1.eventdate")),
        mock(EventQueryParams.class),
        cteContext,
        sb,
        List.of());

    String sql = sb.build();
    assertThat(sql, containsString("ev_occurreddate as \"stage1.eventdate\""));
    assertThat(sql, containsString("group by"));
    assertThat(sql, containsString("ev_occurreddate"));
  }

  @Test
  void addHeaderAggregateColumnsBucketsStageDateViaRealQueryItemKey() {
    // Proves the real collectStageDateItems key path: a QueryItem carrying a stage EVENT_DATE
    // custom header is indexed under the same analytics header key (stageUid.eventdate) that the
    // resolver derives from the GridHeader, so its period (202205) buckets the filter-CTE date
    // column end-to-end through the assembler — no hand-built header->item map.
    ProgramStage stage = new ProgramStage();
    stage.setUid("A03MvHHogjR");
    stage.setName("Birth");

    QueryItem eventDate =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    eventDate.setValueType(ValueType.DATE);
    eventDate.setProgramStage(stage);
    eventDate.withCustomHeader(AnalyticsCustomHeader.forEventDate(stage));
    eventDate.addDimensionValue("202205");

    EventQueryParams params = new EventQueryParams.Builder().addItem(eventDate).build();

    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    cteContext.addFilterCte("latest_events_A03MvHHogjR", "select 1");
    String alias = cteContext.getDefinitionByItemUid("latest_events_A03MvHHogjR").getAlias();

    SelectBuilder sb = new SelectBuilder().from("enrollment_aggr_base", "eb");

    assembler.addHeaderAggregateColumns(
        List.of(new GridHeader("A03MvHHogjR.eventdate")), params, cteContext, sb, List.of());

    String sql = sb.build();
    String expected =
        "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_stage where dps_stage."
            + "\"dateperiod\" = cast("
            + alias
            + ".ev_occurreddate as date))";

    assertThat(sql, containsString(expected + " as \"A03MvHHogjR.eventdate\""));
    assertThat(sql, containsString("group by " + expected));
    // not the raw (unbucketed) select column that the bug produced
    assertThat(sql, not(containsString(alias + ".ev_occurreddate as \"A03MvHHogjR.eventdate\"")));
  }

  @Test
  void addHeaderAggregateColumnsSkipsPeriodProjectionHeadersCaseInsensitively() {
    SelectBuilder sb = new SelectBuilder().from("enrollment_aggr_base", "eb");

    assembler.addHeaderAggregateColumns(
        List.of(new GridHeader("ScheduledDate"), new GridHeader("plain")),
        mock(EventQueryParams.class),
        new CteContext(EndpointItem.ENROLLMENT),
        sb,
        List.of(
            new AggregatedEnrollmentQueryAssembler.PeriodProjection(
                "scheduleddate", Optional.empty())));

    String sql = sb.build();
    assertThat(sql, containsString("\"plain\""));
    assertThat(sql, not(containsString("\"ScheduledDate\"")));
    assertThat(sql, not(containsString("\"scheduleddate\"")));
  }

  @Test
  void addHeaderAggregateColumnsUsesProgramIndicatorCteWhenHeaderMatchesIndicatorUid() {
    SelectBuilder sb = new SelectBuilder().from("enrollment_aggr_base", "eb");
    CteContext cteContext = mock(CteContext.class);
    CteDefinition piDef = mock(CteDefinition.class);
    when(cteContext.getCteKeysExcluding(CteDefinition.ENROLLMENT_AGGR_BASE))
        .thenReturn(Set.of("PiUid"));
    when(cteContext.getDefinitionByItemUid("PiUid")).thenReturn(piDef);
    when(piDef.isProgramStage()).thenReturn(false);
    when(piDef.isProgramIndicator()).thenReturn(true);
    when(piDef.getProgramIndicatorUid()).thenReturn("PiUid");
    when(piDef.getAlias()).thenReturn("pi_cte");

    assembler.addHeaderAggregateColumns(
        List.of(new GridHeader("PiUid")), mock(EventQueryParams.class), cteContext, sb, List.of());

    String sql = sb.build();
    assertThat(sql, containsString("pi_cte.value as \"PiUid\""));
    assertThat(sql, containsString("group by \"PiUid\""));
  }

  @Test
  void resolveAggregatePeriodProjectionsReturnsEmptyWhenNoPeriodDimension() {
    EventQueryParams params = mock(EventQueryParams.class);

    List<AggregatedEnrollmentQueryAssembler.PeriodProjection> projections =
        assembler.resolveAggregatePeriodProjections(params);

    assertTrue(projections.isEmpty());
  }

  @Test
  void resolveAggregatePeriodProjectionsSplitsMixedDateFieldPeriods() {
    PeriodDimension enrollmentDate = month("202301").setDateField(TimeField.ENROLLMENT_DATE.name());
    PeriodDimension lastUpdated = month("202302").setDateField(TimeField.LAST_UPDATED.name());
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject(
                    PERIOD_DIM_ID,
                    PERIOD,
                    "monthly",
                    "Period",
                    List.of(enrollmentDate, lastUpdated)))
            .build();

    List<AggregatedEnrollmentQueryAssembler.PeriodProjection> projections =
        assembler.resolveAggregatePeriodProjections(params);

    assertEquals(2, projections.size());
    assertTrue(
        projections.stream()
            .anyMatch(
                projection ->
                    "enrollmentdate".equals(projection.responseKey())
                        && projection.expression().isPresent()));
    assertTrue(
        projections.stream()
            .anyMatch(
                projection ->
                    "lastupdated".equals(projection.responseKey())
                        && projection.expression().isPresent()));
  }

  @Test
  void resolveAggregatePeriodProjectionsKeepsDefaultGroupWhenMixedWithNonDefaultDateField() {
    PeriodDimension defaultPeriod = month("202301");
    PeriodDimension enrollmentDate = month("202302").setDateField(TimeField.ENROLLMENT_DATE.name());
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject(
                    PERIOD_DIM_ID,
                    PERIOD,
                    "monthly",
                    "Period",
                    List.of(defaultPeriod, enrollmentDate)))
            .build();

    List<AggregatedEnrollmentQueryAssembler.PeriodProjection> projections =
        assembler.resolveAggregatePeriodProjections(params);

    assertEquals(2, projections.size());
    assertTrue(
        projections.stream()
            .anyMatch(
                projection ->
                    "monthly".equals(projection.responseKey())
                        && projection.expression().isEmpty()));
    assertTrue(
        projections.stream()
            .anyMatch(
                projection ->
                    "enrollmentdate".equals(projection.responseKey())
                        && projection.expression().isPresent()));
  }

  @Test
  void collectPeriodDateFieldKeysReturnsEmptyWhenNoPeriodDimension() {
    EventQueryParams params = mock(EventQueryParams.class);

    assertTrue(assembler.collectPeriodDateFieldKeys(params).isEmpty());
  }

  @Test
  void collectPeriodDateFieldKeysReturnsNormalizedNonNullDateFields() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(
                new BaseDimensionalObject(
                    PERIOD_DIM_ID,
                    PERIOD,
                    "monthly",
                    "Period",
                    List.of(
                        month("202301").setDateField(TimeField.ENROLLMENT_DATE.name()),
                        month("202302"),
                        month("202303").setDateField(TimeField.LAST_UPDATED.name()))))
            .build();

    assertEquals(
        Set.of("enrollmentdate", "lastupdated"), assembler.collectPeriodDateFieldKeys(params));
  }

  @Test
  void collectCteDefinitionsReturnsEmptyForEmptyContext() {
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);

    assertTrue(assembler.collectCteDefinitions(cteContext).isEmpty());
  }

  @Test
  void collectCteDefinitionsKeysProgramStageByStageUidAndItemId() {
    CteContext cteContext = mock(CteContext.class);
    CteDefinition stageDef = mock(CteDefinition.class);
    when(cteContext.getCteKeysExcluding(CteDefinition.ENROLLMENT_AGGR_BASE))
        .thenReturn(Set.of("stagecte"));
    when(cteContext.getDefinitionByItemUid("stagecte")).thenReturn(stageDef);
    when(stageDef.isProgramStage()).thenReturn(true);
    when(stageDef.isProgramIndicator()).thenReturn(false);
    when(stageDef.getProgramStageUid()).thenReturn("PStageUid");
    when(stageDef.getItemId()).thenReturn("itemA");

    Map<String, CteDefinition> result = assembler.collectCteDefinitions(cteContext);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("\"PStageUid.itemA\""));
    assertEquals(stageDef, result.get("\"PStageUid.itemA\""));
  }

  @Test
  void collectCteDefinitionsKeysProgramIndicatorByIndicatorUid() {
    CteContext cteContext = mock(CteContext.class);
    CteDefinition piDef = mock(CteDefinition.class);
    when(cteContext.getCteKeysExcluding(CteDefinition.ENROLLMENT_AGGR_BASE))
        .thenReturn(Set.of("picte"));
    when(cteContext.getDefinitionByItemUid("picte")).thenReturn(piDef);
    when(piDef.isProgramStage()).thenReturn(false);
    when(piDef.isProgramIndicator()).thenReturn(true);
    when(piDef.getProgramIndicatorUid()).thenReturn("PiUid");

    Map<String, CteDefinition> result = assembler.collectCteDefinitions(cteContext);

    assertEquals(1, result.size());
    assertTrue(result.containsKey("\"PiUid\""));
    assertEquals(piDef, result.get("\"PiUid\""));
  }

  @Test
  void collectCteDefinitionsSkipsDefinitionsThatAreNeitherStageNorIndicator() {
    CteContext cteContext = mock(CteContext.class);
    CteDefinition otherDef = mock(CteDefinition.class);
    when(cteContext.getCteKeysExcluding(CteDefinition.ENROLLMENT_AGGR_BASE))
        .thenReturn(Set.of("otherCte"));
    when(cteContext.getDefinitionByItemUid("otherCte")).thenReturn(otherDef);
    when(otherDef.isProgramStage()).thenReturn(false);
    when(otherDef.isProgramIndicator()).thenReturn(false);

    assertTrue(assembler.collectCteDefinitions(cteContext).isEmpty());
  }

  private static CteContext shadowCteContext() {
    CteContext context = new CteContext(EndpointItem.ENROLLMENT);
    context.addShadowCte("top_enrollments", "select 1", CteDefinition.CteType.TOP_ENROLLMENTS);
    return context;
  }

  private static PeriodDimension month(String isoPeriod) {
    PeriodDimension period = PeriodDimension.of(isoPeriod);
    period.getPeriod().setPeriodType(PeriodType.getPeriodTypeFromIsoString(isoPeriod));
    return period;
  }
}
