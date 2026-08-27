/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.GridAdaptor;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreator;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

/**
 * Service responsible exclusively for querying. Methods present on this class must not change any
 * state.
 *
 * @author maikel arabori
 */
@Service
@RequiredArgsConstructor
public class TrackedEntityAnalyticsQueryService {

  private final QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor;

  private final GridAdaptor gridAdaptor;

  private final SqlQueryCreatorService sqlQueryCreatorService;

  private final ExecutionPlanStore executionPlanStore;

  private final CommonParamsSecurityManager securityManager;

  private final UserService userService;

  /**
   * This method will create a query, based on the teiParams, and execute it against the underline
   * data provider and return. The results found will be added to the {@link Grid} object returned.
   *
   * @param contextParams the {@link ContextParams}.
   * @return the populated {@link Grid} object.
   * @throws IllegalArgumentException if the given queryParams is null.
   */
  public Grid getGrid(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    CommonParsedParams commonParams = contextParams.getCommonParsed();
    TrackedEntityQueryParams trackedEntityQueryParams = contextParams.getTypedParsed();

    securityManager.decideAccess(
        contextParams.getCommonParsed(),
        singleton(trackedEntityQueryParams.getTrackedEntityType()));
    securityManager.applyOrganisationUnitConstraint(commonParams);
    securityManager.applyDimensionConstraints(commonParams);

    SqlQueryCreator queryCreator = sqlQueryCreatorService.getSqlQueryCreator(contextParams);

    Optional<SqlQueryResult> result =
        withExceptionHandling(() -> queryExecutor.find(queryCreator.createForSelect()));

    long rowsCount = 0;

    AnalyticsPagingParams pagingParams = commonParams.getPagingParams();

    if (pagingParams.showTotalPages()) {
      rowsCount =
          withExceptionHandling(() -> queryExecutor.count(queryCreator.createForCount()))
              .orElse(0l);
    }

    List<Field> fields = queryCreator.getRenderableSqlQuery().getSelectFields();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    return gridAdaptor.createGrid(result, rowsCount, contextParams, fields, currentUser);
  }

  /**
   * This method will only return the data/analysis generated by the "explain" Postgres tool. The
   * result of the analysis will be returned inside a {@link Grid} object.
   *
   * @param contextParams the {@link ContextParams}.
   * @return the populated {@link Grid} object.
   * @throws IllegalArgumentException if the given queryParams is null.
   */
  public Grid getGridExplain(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    CommonParsedParams commonParams = contextParams.getCommonParsed();

    Grid grid = new ListGrid();
    String explainId = randomUUID().toString();

    SqlQueryCreator sqlQueryCreator = sqlQueryCreatorService.getSqlQueryCreator(contextParams);

    withExceptionHandling(
        () -> executionPlanStore.addExecutionPlan(explainId, sqlQueryCreator.createForSelect()));

    if (commonParams.getPagingParams().showTotalPages()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(explainId, sqlQueryCreator.createForCount()));
    }

    List<ExecutionPlan> executionPlans = executionPlanStore.getExecutionPlans(explainId);

    grid.addPerformanceMetrics(executionPlans);

    return grid;
  }
}
