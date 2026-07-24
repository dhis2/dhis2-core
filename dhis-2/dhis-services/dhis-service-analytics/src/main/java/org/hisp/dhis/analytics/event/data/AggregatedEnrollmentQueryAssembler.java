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

import static org.hisp.dhis.analytics.common.CteDefinition.ENROLLMENT_AGGR_BASE;
import static org.hisp.dhis.analytics.event.data.AbstractJdbcEventAnalyticsManager.COLUMN_ENROLLMENT_GEOMETRY_GEOJSON;
import static org.hisp.dhis.analytics.event.data.AbstractJdbcEventAnalyticsManager.COL_VALUE;
import static org.hisp.dhis.analytics.event.data.EnrollmentOrgUnitFilterHandler.isAggregateEnrollment;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getOrgUnitLevelColumns;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getPeriodColumns;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.aggregate.AggregatedEnrollmentDateHeaderResolver;
import org.hisp.dhis.analytics.event.data.aggregate.AggregatedEnrollmentHeaderColumnResolver;
import org.hisp.dhis.analytics.event.data.stage.StageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.analytics.util.sql.SqlColumnParser;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.AnalyticsType;
import org.springframework.stereotype.Component;

/**
 * Assembles SELECT-clause columns for enrollment analytics queries — both the aggregated path
 * (count + org unit + period + header columns) and the standard list path (enrollment standard
 * columns with optional shadow-CTE alias rewriting).
 */
@Component
public class AggregatedEnrollmentQueryAssembler {

  private static final AnalyticsType ANALYTICS_TYPE = AnalyticsType.ENROLLMENT;

  private static final String AX_ALIAS = "ax";

  private static final String ENROLLMENT_AGGR_BASE_ALIAS = "eb";

  private final AnalyticsSqlBuilder sqlBuilder;

  private final DateFieldPeriodBucketColumnResolver dateFieldPeriodBucketColumnResolver;

  private final StageHeaderClassifier stageHeaderClassifier = new StageHeaderClassifier();

  private final AggregatedEnrollmentDateHeaderResolver dateHeaderResolver =
      new AggregatedEnrollmentDateHeaderResolver();

  private final AggregatedEnrollmentHeaderColumnResolver headerColumnResolver;

  public AggregatedEnrollmentQueryAssembler(
      AnalyticsSqlBuilder sqlBuilder,
      DateFieldPeriodBucketColumnResolver dateFieldPeriodBucketColumnResolver,
      StageDatePeriodBucketSqlRenderer dateRenderer) {
    this.sqlBuilder = sqlBuilder;
    this.dateFieldPeriodBucketColumnResolver = dateFieldPeriodBucketColumnResolver;
    this.headerColumnResolver =
        new AggregatedEnrollmentHeaderColumnResolver(stageHeaderClassifier, dateRenderer);
  }

  /** A response key paired with the optional bucketed expression that produces it. */
  public record PeriodProjection(
      String responseKey,
      Optional<DateFieldPeriodBucketColumnResolver.ResolvedExpression> expression) {}

  /** Adds the single aggregated enrollment count column. */
  public void addAggregatedColumns(SelectBuilder sb) {
    sb.addColumn("count(eb.enrollment) as value");
  }

  /**
   * Adds standard enrollment columns. The shadow-CTE branch rewrites formula columns to their
   * {@link #getFormulaColumnAliases() alias}; otherwise formula columns are emitted verbatim.
   */
  public void addStandardColumns(
      SelectBuilder sb, CteContext cteContext, List<String> standardColumns) {
    boolean useShadowCte = cteContext.containsCte("top_enrollments");

    if (useShadowCte) {
      addStandardColumnsWithShadowCte(sb, standardColumns);
    } else {
      addStandardColumnsWithoutShadowCte(sb, standardColumns);
    }
  }

  private void addStandardColumnsWithShadowCte(SelectBuilder sb, List<String> standardColumns) {
    Map<String, String> formulaAliases = getFormulaColumnAliases();

    for (String column : standardColumns) {
      if (columnIsInFormula(column)) {
        String alias = formulaAliases.getOrDefault(column, createDefaultAlias(column));
        sb.addColumn(alias, AX_ALIAS);
      } else {
        sb.addColumn(column, AX_ALIAS);
      }
    }
  }

  private void addStandardColumnsWithoutShadowCte(SelectBuilder sb, List<String> standardColumns) {
    standardColumns.forEach(
        column -> {
          if (columnIsInFormula(column)) {
            sb.addColumn(column);
          } else {
            sb.addColumn(column, AX_ALIAS);
          }
        });
  }

  /**
   * Adds organisation-unit columns. When level columns are configured they replace the default
   * {@code ou} column; otherwise the default is added unless the params describe an aggregate
   * enrollment without dimensions.
   */
  public void addOrgUnitAggregateColumns(SelectBuilder sb, EventQueryParams params) {
    Set<String> orgColumns = getOrgUnitLevelColumns(params);

    if (orgColumns.isEmpty() && !isAggregateEnrollment(params)) {
      sb.addColumn(ORGUNIT_DIM_ID);
      sb.groupBy(ORGUNIT_DIM_ID);
      return;
    }

    for (String col : orgColumns) {
      String trimmed = col.trim();
      sb.addColumn(trimmed);
      sb.groupBy(trimmed);
    }
  }

  /**
   * Adds period columns. When date-field period projections are present they take precedence over
   * the default {@code pe} column and bring along their resolved expressions.
   */
  public void addPeriodAggregateColumns(
      EventQueryParams params, SelectBuilder sb, List<PeriodProjection> projections) {
    if (projections.isEmpty()) {
      Set<String> periodColumns = getPeriodColumns(params);
      for (String periodColumn : periodColumns) {
        var col = SqlColumnParser.removeTableAlias(periodColumn.trim());
        sb.addColumn(col);
        sb.groupBy(col);
      }
      return;
    }

    for (PeriodProjection projection : projections) {
      if (projection.expression().isPresent()) {
        DateFieldPeriodBucketColumnResolver.ResolvedExpression expr = projection.expression().get();
        sb.addColumn(expr.selectExpression());
        sb.groupBy(expr.groupByExpression());
      } else {
        String column = quote(projection.responseKey());
        sb.addColumn(column);
        sb.groupBy(column);
      }
    }
  }

  /**
   * Adds the columns specified in the headers to the SelectBuilder. The columns are added in the
   * order specified in the headers and are based on existing CTE definitions. Stage-specific
   * prefixes (e.g. "stageUid.eventdate") are preserved so the resolver can look up the correct
   * per-stage filter CTE.
   */
  public void addHeaderAggregateColumns(
      List<GridHeader> headers,
      EventQueryParams params,
      CteContext cteContext,
      SelectBuilder sb,
      List<PeriodProjection> projections) {
    Set<String> periodKeys =
        projections.stream().map(PeriodProjection::responseKey).collect(Collectors.toSet());

    // Also skip headers that match the date field key of a period projection source column
    // (e.g. "eventdate" when EVENT_DATE is the date field for the period dimension)
    periodKeys.addAll(collectPeriodDateFieldKeys(params));

    Set<String> headerColumns = new LinkedHashSet<>();

    for (GridHeader header : headers) {
      String name = dateHeaderResolver.normalizeHeaderKey(header.getName());

      if (isInfrastructureHeader(name)) {
        continue;
      }
      if (periodKeys.stream().anyMatch(name::equalsIgnoreCase)) {
        continue;
      }
      headerColumns.add(resolveHeaderColumn(name));
    }

    Map<String, CteDefinition> cteDefinitionMap = collectCteDefinitions(cteContext);
    Map<String, QueryItem> stageDateItems = collectStageDateItems(params);
    headerColumnResolver.addHeaderColumns(
        headerColumns, cteContext, sb, cteDefinitionMap, stageDateItems, this::quote);
  }

  /**
   * Indexes query items carrying a custom stage header (e.g. {@code stageUid.EVENT_DATE}) by their
   * analytics header key (e.g. {@code stageUid.eventdate}) so the header-column resolver can bucket
   * a stage date column by the item's requested period.
   */
  private Map<String, QueryItem> collectStageDateItems(EventQueryParams params) {
    Map<String, QueryItem> stageDateItems = new HashMap<>();
    for (QueryItem item : params.getItems()) {
      if (item.hasCustomHeader()) {
        AnalyticsCustomHeader customHeader = item.getCustomHeader();
        stageDateItems.put(customHeader.headerKey(customHeader.key()), item);
      }
    }
    return stageDateItems;
  }

  /**
   * Returns the per-period date-field projections derived from the {@code pe} dimension. Empty when
   * the dimension is absent.
   */
  public List<PeriodProjection> resolveAggregatePeriodProjections(EventQueryParams params) {
    DimensionalObject periodDimension = params.getDimension(PERIOD_DIM_ID);
    if (periodDimension == null) {
      return List.of();
    }

    return PeriodDimensionSplitter.splitPeriodDimension(periodDimension).stream()
        .map(
            dim ->
                new PeriodProjection(
                    dim.getDimensionName(),
                    dateFieldPeriodBucketColumnResolver.resolve(
                        ANALYTICS_TYPE, dim, ENROLLMENT_AGGR_BASE_ALIAS)))
        .toList();
  }

  /** Returns true for headers whose columns are added by dedicated sibling methods. */
  public boolean isInfrastructureHeader(String name) {
    return name.equalsIgnoreCase(COL_VALUE)
        || name.equalsIgnoreCase(PERIOD_DIM_ID)
        || name.equalsIgnoreCase(ORGUNIT_DIM_ID);
  }

  /**
   * Collects the date field keys from all period dimension items that have a non-default date
   * field. For example, period items with date field EVENT_DATE produce "eventdate".
   */
  public Set<String> collectPeriodDateFieldKeys(EventQueryParams params) {
    DimensionalObject periodDimension = params.getDimension(PERIOD_DIM_ID);
    if (periodDimension == null) {
      return Set.of();
    }
    return periodDimension.getItems().stream()
        .filter(PeriodDimension.class::isInstance)
        .map(PeriodDimension.class::cast)
        .map(PeriodDimension::getDateField)
        .filter(Objects::nonNull)
        .map(PeriodDimensionSplitter::toDateFieldKey)
        .collect(Collectors.toSet());
  }

  /** Maps header name to the corresponding database column name. */
  public String resolveHeaderColumn(String name) {
    if (name.equalsIgnoreCase(StaticDimension.PROGRAM_STATUS.getHeaderName())) {
      return StaticDimension.PROGRAM_STATUS.getColumnName();
    }
    return quote(name);
  }

  /**
   * Indexes program-stage and program-indicator CTE definitions by their quoted header key so the
   * header-column resolver can look up which CTE supplies a given column.
   */
  public Map<String, CteDefinition> collectCteDefinitions(CteContext cteContext) {
    Map<String, CteDefinition> cteDefinitionMap = new HashMap<>();

    Set<String> cteKeys = cteContext.getCteKeysExcluding(ENROLLMENT_AGGR_BASE);

    for (String key : cteKeys) {
      CteDefinition def = cteContext.getDefinitionByItemUid(key);

      if (def.isProgramStage() || def.isProgramIndicator()) {
        String mapKey =
            quote(
                def.isProgramIndicator()
                    ? def.getProgramIndicatorUid()
                    : def.getProgramStageUid() + "." + def.getItemId());

        cteDefinitionMap.put(mapKey, def);
      }
    }
    return cteDefinitionMap;
  }

  private String quote(String relation) {
    return sqlBuilder.quote(relation);
  }

  private Map<String, String> getFormulaColumnAliases() {
    Map<String, String> aliases = new HashMap<>();
    aliases.put(COLUMN_ENROLLMENT_GEOMETRY_GEOJSON, "enrollmentgeometry_geojson");
    return aliases;
  }

  private boolean columnIsInFormula(String col) {
    return col.contains("(") && col.contains(")");
  }

  private String createDefaultAlias(String formula) {
    return "formula_" + (formula.hashCode() & Integer.MAX_VALUE);
  }
}
