/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.event.data.aggregate;

import static org.hisp.dhis.analytics.event.data.AbstractJdbcEventAnalyticsManager.LATEST_EVENTS_CTE_PREFIX;
import static org.hisp.dhis.analytics.util.RepeatableStageParamsHelper.removeRepeatableStageParams;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.data.stage.StageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier.StageHeaderType;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.analytics.util.sql.SqlColumnParser;
import org.hisp.dhis.common.QueryItem;

/**
 * Resolves aggregated enrollment header columns into SQL select and group-by expressions, handling
 * stage-specific dimensions via per-stage filter CTEs.
 */
public final class AggregatedEnrollmentHeaderColumnResolver {

  private final StageHeaderClassifier stageHeaderClassifier;

  private final StageDatePeriodBucketSqlRenderer dateRenderer;

  /**
   * Creates a resolver that uses the provided classifier to detect stage-specific headers and the
   * renderer to bucket stage date columns by the requested period.
   *
   * @param stageHeaderClassifier classifier for stage-specific header types
   * @param dateRenderer renderer that buckets a stage date column by the requested period
   */
  public AggregatedEnrollmentHeaderColumnResolver(
      StageHeaderClassifier stageHeaderClassifier, StageDatePeriodBucketSqlRenderer dateRenderer) {
    this.stageHeaderClassifier = stageHeaderClassifier;
    this.dateRenderer = dateRenderer;
  }

  /**
   * Adds header columns to the {@link SelectBuilder}, preserving header order and applying the
   * appropriate CTE mappings for stage-specific dimensions.
   *
   * @param headerColumns header column names
   * @param cteContext CTE context with available CTE definitions
   * @param sb select builder to append columns and group-by entries to
   * @param cteDefinitionMap map of header keys to their CTE definitions
   * @param stageDateItems map of stage date header keys (e.g. {@code stageUid.eventdate}) to their
   *     {@link QueryItem}, used to bucket the column by the requested period
   * @param quote operator used to quote column aliases
   */
  public void addHeaderColumns(
      Set<String> headerColumns,
      CteContext cteContext,
      SelectBuilder sb,
      Map<String, CteDefinition> cteDefinitionMap,
      Map<String, QueryItem> stageDateItems,
      UnaryOperator<String> quote) {

    for (String headerColumn : headerColumns) {
      String colName = SqlColumnParser.removeTableAlias(headerColumn);
      String quotedCol = quote.apply(colName);
      StageHeaderType stageHeaderType = stageHeaderClassifier.classify(headerColumn);

      if (stageHeaderType != StageHeaderType.NOT_STAGE_SPECIFIC) {
        CteDefinition filterCte = findFilterCte(headerColumn, cteContext);
        if (filterCte != null) {
          String cteCol = resolveCteColumn(filterCte, stageHeaderType, colName, stageDateItems);
          sb.addColumn(cteCol, "", quotedCol);
          sb.groupBy(cteCol);
          continue;
        }
      }

      Map.Entry<String, CteDefinition> matchingEntry = findMatchingCte(cteDefinitionMap, colName);
      if (matchingEntry != null) {
        sb.addColumn(matchingEntry.getValue().getAlias() + ".value", "", quotedCol);
        sb.groupBy(quotedCol);
      } else {
        sb.addColumn(quotedCol);
        sb.groupBy(quotedCol);
      }
    }
  }

  /**
   * Finds the filter CTE for a stage-specific header by extracting the stage UID and looking up the
   * corresponding CTE definition.
   */
  private CteDefinition findFilterCte(String header, CteContext cteContext) {
    String stageUid = extractStageUid(header);
    if (stageUid == null) {
      return null;
    }
    return cteContext.getDefinitionByItemUid(LATEST_EVENTS_CTE_PREFIX + stageUid);
  }

  /**
   * Resolves the CTE column expression for a given stage header type. Uses the {@code ev_<column>}
   * alias convention established by the aggregate filter CTE.
   */
  private String resolveCteColumn(
      CteDefinition filterCte,
      StageHeaderType stageHeaderType,
      String colName,
      Map<String, QueryItem> stageDateItems) {
    String alias = filterCte.getAlias();
    String bareCol = extractColumnName(colName);
    return switch (stageHeaderType) {
      case EVENT_DATE -> resolveStageDateColumn(alias, "ev_occurreddate", colName, stageDateItems);
      case SCHEDULED_DATE ->
          resolveStageDateColumn(alias, "ev_scheduleddate", colName, stageDateItems);
      case EVENT_STATUS -> alias + ".ev_eventstatus";
      case OU_NAME -> alias + ".ev_ouname";
      case OU_CODE -> alias + ".ev_oucode";
      case OU, GENERIC_STAGE_ITEM -> alias + ".ev_" + bareCol;
      case NOT_STAGE_SPECIFIC -> alias + "." + bareCol;
    };
  }

  /**
   * Resolves a stage date column to a period-bucket expression when the matching {@link QueryItem}
   * carries period dimension values; otherwise returns the raw {@code <alias>.<dateColumn>}
   * reference (ranges and no-value cases are unaffected).
   */
  private String resolveStageDateColumn(
      String alias, String dateColumn, String colName, Map<String, QueryItem> stageDateItems) {
    String rawColumn = alias + "." + dateColumn;
    QueryItem item = stageDateItems.get(colName);
    if (item == null) {
      return rawColumn;
    }
    Optional<String> periodBucket = dateRenderer.resolvePeriodBucketColumn(item);
    return periodBucket
        .map(bucket -> dateRenderer.renderPeriodBucketExpression(rawColumn, bucket))
        .orElse(rawColumn);
  }

  /**
   * Extracts the column name from a potentially stage-prefixed column (e.g. "stageUid.col" →
   * "col").
   */
  private String extractColumnName(String colName) {
    int dotIndex = colName.indexOf('.');
    return dotIndex >= 0 ? colName.substring(dotIndex + 1) : colName;
  }

  /**
   * Extracts the stage UID from a header in {@code "stageUid.column"} format. Strips surrounding
   * quotes if present.
   */
  private String extractStageUid(String header) {
    String cleaned = header.replace("\"", "").replace("`", "");
    int dotIndex = cleaned.indexOf('.');
    if (dotIndex <= 0) {
      return null;
    }
    return cleaned.substring(0, dotIndex);
  }

  private Map.Entry<String, CteDefinition> findMatchingCte(
      Map<String, CteDefinition> cteDefinitionMap, String colName) {
    String normalizedColName =
        removeRepeatableStageParams(colName.replace("\"", "").replace("`", ""));
    for (Map.Entry<String, CteDefinition> entry : cteDefinitionMap.entrySet()) {
      String normalizedKey =
          removeRepeatableStageParams(entry.getKey().replace("\"", "").replace("`", ""));
      if (normalizedKey.contains(normalizedColName)) {
        return entry;
      }
    }
    return null;
  }
}
