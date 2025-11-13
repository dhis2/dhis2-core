/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.sql;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SqlQueryCreatorService {
  private final List<SqlQueryBuilder> providers;

  /**
   * Builds a SqlQueryCreator from the given TrackedEntityQueryParams.
   *
   * @param contextParams the {@link ContextParams} to build the SqlQueryCreator from.
   * @return a SqlQueryCreator
   */
  public SqlQueryCreator getSqlQueryCreator(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(contextParams, sqlParameterManager);
    CommonParsedParams parsedParams = contextParams.getCommonParsed();

    RenderableSqlQuery renderableSqlQuery =
        RenderableSqlQuery.builder().countRequested(false).build();

    for (SqlQueryBuilder provider : providers) {
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions =
          parsedParams.getDimensionIdentifiers().stream()
              .filter(provider.getDimensionFilters().stream().reduce(x -> true, Predicate::and))
              .toList();

      List<AnalyticsSortingParams> acceptedSortingParams =
          parsedParams.getOrderParams().stream()
              .filter(provider.getSortingFilters().stream().reduce(x -> true, Predicate::and))
              .toList();

      List<DimensionIdentifier<DimensionParam>> acceptedHeaders =
          parsedParams.getParsedHeaders().stream()
              .filter(provider.getHeaderFilters().stream().reduce(x -> true, Predicate::and))
              .filter(parsedHeader -> notContains(acceptedDimensions, parsedHeader))
              .toList();

      if (provider.alwaysRun()
          || !CollectionUtils.isEmpty(acceptedHeaders)
          || !CollectionUtils.isEmpty(acceptedDimensions)
          || !CollectionUtils.isEmpty(acceptedSortingParams)) {
        renderableSqlQuery =
            mergeQueries(
                renderableSqlQuery,
                provider.buildSqlQuery(
                    queryContext, acceptedHeaders, acceptedDimensions, acceptedSortingParams));
      }
    }

    return SqlQueryCreator.of(queryContext, renderableSqlQuery);
  }

  /**
   * Checks if the acceptedDimensions list does not contain the parsedHeader. As an exception,
   * duplicates are allowed if the dimension has a legendSet.
   *
   * @param acceptedDimensions the list of accepted dimensions
   * @param parsedHeader the parsed header
   * @return true if the acceptedDimensions list does not contain the parsedHeader
   */
  private boolean notContains(
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      DimensionIdentifier<DimensionParam> parsedHeader) {
    return acceptedDimensions.stream()
        .filter(not(DimensionIdentifier::hasLegendSet))
        .noneMatch(
            dimensionIdentifier ->
                Strings.CI.equals(dimensionIdentifier.toString(), parsedHeader.toString()));
  }

  private RenderableSqlQuery mergeQueries(
      RenderableSqlQuery initial, RenderableSqlQuery contribution) {
    RenderableSqlQuery.RenderableSqlQueryBuilder sqlQueryContextBuilder = initial.toBuilder();
    contribution.getSelectFields().forEach(sqlQueryContextBuilder::selectField);
    contribution.getLeftJoins().forEach(sqlQueryContextBuilder::leftJoin);
    contribution.getGroupableConditions().forEach(sqlQueryContextBuilder::groupableCondition);
    contribution.getOrderClauses().forEach(sqlQueryContextBuilder::orderClause);

    if (contribution.getMainTable() != null) {
      sqlQueryContextBuilder.mainTable(contribution.getMainTable());
    }

    if (contribution.getLimitOffset() != null) {
      sqlQueryContextBuilder.limitOffset(contribution.getLimitOffset());
    }

    return sqlQueryContextBuilder.build();
  }
}
