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

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier.StageHeaderType;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.analytics.util.sql.SqlColumnParser;

/**
 * Resolves aggregated enrollment header columns into SQL select and group-by expressions, handling
 * stage-specific dimensions and CTE-backed values (including the latest-events filter CTE).
 */
public final class AggregatedEnrollmentHeaderColumnResolver {
  private static final String LATEST_EVENTS_CTE = "latest_events";

  private final StageHeaderClassifier stageHeaderClassifier;

  /**
   * Creates a resolver that uses the provided classifier to detect stage-specific headers.
   *
   * @param stageHeaderClassifier classifier for stage-specific header types
   */
  public AggregatedEnrollmentHeaderColumnResolver(StageHeaderClassifier stageHeaderClassifier) {
    this.stageHeaderClassifier = stageHeaderClassifier;
  }

  /**
   * Adds header columns to the {@link SelectBuilder}, preserving header order and applying the
   * appropriate CTE mappings for stage-specific dimensions.
   *
   * @param headerColumns header column names
   * @param cteContext CTE context with available CTE definitions
   * @param sb select builder to append columns and group-by entries to
   * @param cteDefinitionMap map of header keys to their CTE definitions
   * @param quote function used to quote column aliases
   */
  public void addHeaderColumns(
      Set<String> headerColumns,
      CteContext cteContext,
      SelectBuilder sb,
      Map<String, CteDefinition> cteDefinitionMap,
      Function<String, String> quote) {
    CteDefinition filterCte = cteContext.getDefinitionByItemUid(LATEST_EVENTS_CTE);

    for (String headerColumn : headerColumns) {
      String colName = SqlColumnParser.removeTableAlias(headerColumn);
      String quotedCol = quote.apply(colName);
      StageHeaderType stageHeaderType = stageHeaderClassifier.classify(headerColumn);

      if (filterCte != null && usesFilterCteValue(stageHeaderType)) {
        String cteValueCol = filterCte.getAlias() + ".value";
        sb.addColumn(cteValueCol, "", quotedCol);
        sb.groupBy(cteValueCol);
        continue;
      }

      Map.Entry<String, CteDefinition> matchingEntry = findMatchingCte(cteDefinitionMap, colName);
      if (matchingEntry != null) {
        sb.addColumn(matchingEntry.getValue().getAlias() + ".value", "", matchingEntry.getKey());
        sb.groupBy(matchingEntry.getKey());
      } else if (filterCte != null && stageHeaderType == StageHeaderType.OU_NAME) {
        String cteCol = filterCte.getAlias() + ".ev_ouname";
        sb.addColumn(cteCol, "", quotedCol);
        sb.groupBy(cteCol);
      } else if (filterCte != null && stageHeaderType == StageHeaderType.OU_CODE) {
        String cteCol = filterCte.getAlias() + ".ev_oucode";
        sb.addColumn(cteCol, "", quotedCol);
        sb.groupBy(cteCol);
      } else if (filterCte != null && stageHeaderType == StageHeaderType.GENERIC_STAGE_ITEM) {
        String cteValueCol = filterCte.getAlias() + ".value";
        sb.addColumn(cteValueCol, "", quotedCol);
        sb.groupBy(cteValueCol);
      } else {
        sb.addColumn(quotedCol);
        sb.groupBy(quotedCol);
      }
    }
  }

  private Map.Entry<String, CteDefinition> findMatchingCte(
      Map<String, CteDefinition> cteDefinitionMap, String colName) {
    for (Map.Entry<String, CteDefinition> entry : cteDefinitionMap.entrySet()) {
      if (entry.getKey().contains(colName)) {
        return entry;
      }
    }
    return null;
  }

  private boolean usesFilterCteValue(StageHeaderType stageHeaderType) {
    return stageHeaderType == StageHeaderType.EVENT_DATE
        || stageHeaderType == StageHeaderType.SCHEDULED_DATE
        || stageHeaderType == StageHeaderType.OU
        || stageHeaderType == StageHeaderType.EVENT_STATUS;
  }
}
