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
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link StageQuerySqlFacade}.
 *
 * <p>Delegates classification and SQL rendering to specialized stage services and returns empty
 * results when a query item is not stage-specific.
 */
@Component
public class DefaultStageQuerySqlFacade implements StageQuerySqlFacade {
  private final StageQueryItemClassifier classifier;
  private final StageDatePeriodBucketSqlRenderer dateRenderer;
  private final StageOrgUnitSqlService stageOrgUnitSqlService;

  /**
   * Creates a stage SQL facade.
   *
   * @param classifier stage query item classifier
   * @param dateRenderer stage date period renderer
   * @param stageOrgUnitSqlService stage org unit SQL service
   */
  public DefaultStageQuerySqlFacade(
      StageQueryItemClassifier classifier,
      StageDatePeriodBucketSqlRenderer dateRenderer,
      StageOrgUnitSqlService stageOrgUnitSqlService) {
    this.classifier = classifier;
    this.dateRenderer = dateRenderer;
    this.stageOrgUnitSqlService = stageOrgUnitSqlService;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<ColumnAndAlias> resolveSelectColumn(
      QueryItem item, EventQueryParams params, boolean isGroupByClause, boolean isAggregated) {
    if (classifier.isStageOrgUnit(item)) {
      return Optional.of(stageOrgUnitSqlService.selectColumn(item, params, isGroupByClause));
    }

    if (isAggregated && classifier.isStageDate(item)) {
      Optional<String> periodBucket = dateRenderer.resolvePeriodBucketColumn(item);
      if (periodBucket.isPresent()) {
        String expression = dateRenderer.renderPeriodBucketExpression(item, periodBucket.get());
        return Optional.of(
            isGroupByClause
                ? ColumnAndAlias.ofColumn(expression)
                : ColumnAndAlias.ofColumnAndAlias(expression, item.getItemName()));
      }
    }

    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<String> resolveWhereClause(
      QueryItem item, EventQueryParams params, AnalyticsType analyticsType) {
    if (classifier.isStageOrgUnit(item)) {
      return Optional.of(stageOrgUnitSqlService.whereClause(item, params, analyticsType));
    }

    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStageDate(QueryItem item) {
    return classifier.isStageDate(item);
  }
}
