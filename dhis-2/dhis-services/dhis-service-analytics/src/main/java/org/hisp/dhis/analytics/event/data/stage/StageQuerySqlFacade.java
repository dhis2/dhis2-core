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
package org.hisp.dhis.analytics.event.data.stage;

import java.util.Optional;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ColumnAndAlias;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.AnalyticsType;

/**
 * Facade for stage-specific SQL concerns used by event and enrollment analytics managers.
 *
 * <p>Provides a single integration point for stage-specific SELECT, WHERE, and item classification
 * behavior.
 */
public interface StageQuerySqlFacade {
  /**
   * Resolves a stage-specific SELECT/GROUP BY expression when applicable.
   *
   * @param item the query item
   * @param params query parameters
   * @param isGroupByClause true when expression is for GROUP BY
   * @param isAggregated true when query is aggregated
   * @return stage-specific column and alias, empty when generic manager logic should be used
   */
  Optional<ColumnAndAlias> resolveSelectColumn(
      QueryItem item, EventQueryParams params, boolean isGroupByClause, boolean isAggregated);

  /**
   * Resolves a stage-specific WHERE clause fragment when applicable.
   *
   * @param item the query item
   * @param params query parameters
   * @param analyticsType analytics type used for SQL rendering
   * @return stage-specific WHERE fragment, empty when generic manager logic should be used
   */
  Optional<String> resolveWhereClause(
      QueryItem item, EventQueryParams params, AnalyticsType analyticsType);

  /**
   * Checks whether an item is a stage-specific date item.
   *
   * @param item the query item
   * @return true if the item is stage event date or scheduled date
   */
  boolean isStageDate(QueryItem item);
}
