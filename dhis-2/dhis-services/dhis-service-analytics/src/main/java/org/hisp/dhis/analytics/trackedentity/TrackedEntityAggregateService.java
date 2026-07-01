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
package org.hisp.dhis.analytics.trackedentity;

import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.trackedentity.query.TrackedEntityFields.getAggregateGridHeaders;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreator;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

/** Executes tracked-entity aggregate (grouped) queries and assembles the grouped grid. */
@Service
@RequiredArgsConstructor
public class TrackedEntityAggregateService {

  private static final GridHeader VALUE_HEADER =
      new GridHeader("value", "Value", ValueType.NUMBER, false, false);

  private final QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor;

  private final SqlQueryCreatorService sqlQueryCreatorService;

  private final ExecutionPlanStore executionPlanStore;

  private final CommonParamsSecurityManager securityManager;

  private final MetadataParamsHandler metadataParamsHandler;

  private final UserService userService;

  public Grid getGrid(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    CommonParsedParams commonParams = contextParams.getCommonParsed();
    TrackedEntityQueryParams teParams = contextParams.getTypedParsed();

    securityManager.decideAccess(commonParams, singleton(teParams.getTrackedEntityType()));
    securityManager.applyOrganisationUnitConstraint(commonParams);
    securityManager.applyDimensionConstraints(commonParams);

    SqlQueryCreator queryCreator = sqlQueryCreatorService.getSqlQueryCreator(contextParams);

    Optional<SqlQueryResult> result =
        withExceptionHandling(() -> queryExecutor.find(queryCreator.createForSelect()));

    long rowsCount = 0;
    AnalyticsPagingParams paging = commonParams.getPagingParams();
    if (paging.showTotalPages()) {
      rowsCount =
          withExceptionHandling(() -> queryExecutor.count(queryCreator.createForCount()))
              .orElse(0L);
    }

    Grid grid = new ListGrid();
    getAggregateGridHeaders(contextParams).forEach(grid::addHeader);
    grid.addHeader(VALUE_HEADER);

    result.ifPresent(r -> addGroupedRows(grid, r.result()));

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    metadataParamsHandler.handle(grid, contextParams, currentUser, rowsCount);
    return grid;
  }

  private void addGroupedRows(Grid grid, SqlRowSet rs) {
    List<String> columns = grid.getHeaders().stream().map(GridHeader::getName).collect(toList());
    while (rs.next()) {
      grid.addRow();
      columns.forEach(col -> grid.addValue(rs.getObject(col)));
    }
  }

  public Grid getGridExplain(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    Grid grid = new ListGrid();
    String explainId = randomUUID().toString();
    SqlQueryCreator queryCreator = sqlQueryCreatorService.getSqlQueryCreator(contextParams);

    withExceptionHandling(
        () -> executionPlanStore.addExecutionPlan(explainId, queryCreator.createForSelect()));
    if (contextParams.getCommonParsed().getPagingParams().showTotalPages()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(explainId, queryCreator.createForCount()));
    }
    List<ExecutionPlan> plans = executionPlanStore.getExecutionPlans(explainId);
    grid.addPerformanceMetrics(plans);
    return grid;
  }
}
