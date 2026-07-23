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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.analytics.trackedentity.query.context.QueryContextConstants.TRACKED_ENTITY_ALIAS;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Builds the SELECT and GROUP BY of a tracked entity aggregate (grouped) query. Owns the SELECT in
 * aggregate mode: the requested dimensions become both select columns and group-by keys, plus a
 * trailing aggregate value column.
 */
@Service
@Order(0)
public class AggregateQueryBuilder implements SqlQueryBuilder {

  @Getter
  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(
          dimension ->
              OrgUnitQueryBuilder.isOu(dimension)
                  || TrackedEntityQueryBuilder.isTrackedEntity(dimension));

  @Override
  public RenderableSqlQuery buildSqlQuery(
      QueryContext queryContext,
      List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      List<AnalyticsSortingParams> acceptedSortingParams) {
    if (!queryContext.isAggregate()) {
      return RenderableSqlQuery.builder().build();
    }

    RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

    Set<String> groupedKeys = getGroupedDimensionKeys(queryContext.getContextParams());

    // Only dimensions explicitly requested by the user (present in the raw request's
    // `dimension` param) become select columns and group-by keys. `acceptedDimensions` also
    // carries dimensions injected upstream for row-level display purposes, which must not
    // affect grouping. The same alias-free field is used for both select and group-by: an
    // "as <alias>" suffix is invalid inside a GROUP BY, and the column name already identifies
    // the dimension item.
    acceptedDimensions.stream()
        .filter(dimension -> groupedKeys.contains(dimension.getKey()))
        .forEach(
            dimension -> {
              Field field = Field.ofDimensionIdentifier(dimension);
              builder.selectField(field);
              builder.groupByField(field);
            });

    // The aggregate value column is the last select column and is not grouped.
    builder.selectField(
        Field.ofUnquoted("", () -> valueExpression(queryContext.getContextParams()), "value"));

    return builder.build();
  }

  /**
   * Returns the SQL expression of the aggregate value column. Without a value attribute the query
   * counts TEIs; with one, the aggregation function is applied to the attribute column — including
   * an explicit COUNT, which then counts non-null attribute values, matching the event/enrollment
   * aggregate contract.
   */
  private static String valueExpression(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    TrackedEntityQueryParams params = contextParams.getTypedParsed();

    if (params.getValue() == null) {
      return "count(1)";
    }

    return params.getAggregationType().getValue()
        + "("
        + TRACKED_ENTITY_ALIAS
        + ".\""
        + params.getValue().getUid()
        + "\")";
  }

  /**
   * Returns the keys of the dimensions an aggregate query groups by: the dimensions the user
   * explicitly requested (present in the raw {@code dimension} param) that are also groupable in
   * aggregate mode (registration org unit or a tracked entity / program attribute). Only {@code
   * commonRaw} distinguishes explicitly-requested dimensions from those injected upstream for
   * row-level display; the parsed dimensions merge both. This is the single source of truth for
   * what the aggregate query groups by, and therefore what may be sorted on.
   */
  public static Set<String> getGroupedDimensionKeys(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    Set<String> requestedKeys =
        contextParams.getCommonRaw().getDimension().stream()
            .map(param -> getDimensionFromParam(param))
            .collect(toSet());

    return contextParams.getCommonParsed().getDimensionIdentifiers().stream()
        .filter(
            dimension ->
                OrgUnitQueryBuilder.isOu(dimension)
                    || TrackedEntityQueryBuilder.isTrackedEntity(dimension))
        .map(DimensionIdentifier::getKey)
        .filter(requestedKeys::contains)
        .collect(toSet());
  }

  @Override
  public boolean alwaysRun() {
    return true;
  }
}
