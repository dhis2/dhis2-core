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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
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
          sqlBuilder, new DateFieldPeriodBucketColumnResolver(sqlBuilder));

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
    EventQueryParams params = mock(EventQueryParams.class);

    assembler.addStandardColumns(sb, cteContext, List.of("enrollment", "trackedentity"));

    String sql = sb.build();
    assertThat(sql, containsString("ax.enrollment"));
    assertThat(sql, containsString("ax.trackedentity"));
  }

  @Test
  void addStandardColumnsFormulaColumnInPlainPathIsAddedVerbatim() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);
    EventQueryParams params = mock(EventQueryParams.class);
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
    EventQueryParams params = mock(EventQueryParams.class);

    assembler.addStandardColumns(sb, cteContext, List.of("enrollment", "trackedentity"));

    String sql = sb.build();
    assertThat(sql, containsString("ax.enrollment"));
    assertThat(sql, containsString("ax.trackedentity"));
  }

  @Test
  void addStandardColumnsWithShadowCteRewritesGeometryFormulaToAlias() {
    SelectBuilder sb = new SelectBuilder().from("ax_table", "ax");
    CteContext cteContext = shadowCteContext();
    EventQueryParams params = mock(EventQueryParams.class);
    String formula = "ST_AsGeoJSON(enrollmentgeometry)";

    assembler.addStandardColumns(sb, cteContext, List.of(formula));

    String sql = sb.build();
    // Shadow path swaps the formula for the registered alias and ax-prefixes it.
    assertThat(sql, containsString("ax.enrollmentgeometry_geojson"));
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
  void resolveAggregatePeriodProjectionsReturnsEmptyWhenNoPeriodDimension() {
    EventQueryParams params = mock(EventQueryParams.class);

    List<AggregatedEnrollmentQueryAssembler.PeriodProjection> projections =
        assembler.resolveAggregatePeriodProjections(params);

    assertTrue(projections.isEmpty());
  }

  @Test
  void collectPeriodDateFieldKeysReturnsEmptyWhenNoPeriodDimension() {
    EventQueryParams params = mock(EventQueryParams.class);

    assertTrue(assembler.collectPeriodDateFieldKeys(params).isEmpty());
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
}
