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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

// ABOUTME: Handles stage-level static dimensions (EVENT_DATE, SCHEDULED_DATE, OU, EVENT_STATUS)
// ABOUTME: for tracked entity analytics queries.

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.DIMENSION_SEPARATOR;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getPrefix;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.isEventLevelStaticDimension;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EVENT_DATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EVENT_STATUS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.OU;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.SCHEDULED_DATE;
import static org.hisp.dhis.commons.util.TextUtils.doubleQuote;

import java.util.List;
import java.util.function.Predicate;
import lombok.Getter;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.trackedentity.query.EventOrgUnitCondition;
import org.hisp.dhis.analytics.trackedentity.query.PeriodStaticDimensionCondition;
import org.hisp.dhis.analytics.trackedentity.query.StatusCondition;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilderAdaptor;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilders;
import org.springframework.stereotype.Service;

/** Query builder for stage-level static dimensions. */
@Service
@Getter
@org.springframework.core.annotation.Order(5)
public class EventAttributeQueryBuilder extends SqlQueryBuilderAdaptor {

  private static final List<StaticDimension> SUPPORTED_EVENT_ATTRIBUTES =
      List.of(EVENT_DATE, SCHEDULED_DATE, OU, EVENT_STATUS);

  private final List<Predicate<DimensionIdentifier<DimensionParam>>> headerFilters =
      List.of(EventAttributeQueryBuilder::isEventAttribute);

  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(EventAttributeQueryBuilder::isEventAttribute);

  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(sortingParams -> isEventAttribute(sortingParams.getOrderBy()));

  private static boolean isEventAttribute(DimensionIdentifier<DimensionParam> dimId) {
    return isEventLevelStaticDimension(dimId, SUPPORTED_EVENT_ATTRIBUTES);
  }

  @Override
  public RenderableSqlQuery buildSqlQuery(
      QueryContext ctx,
      List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      List<AnalyticsSortingParams> acceptedSortingParams) {
    RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

    // All stage-specific static dimensions should be virtual fields.
    // The data is extracted from the enrollments JSON column by SqlRowSetJsonExtractorDelegator.
    streamDimensions(acceptedHeaders, acceptedDimensions, acceptedSortingParams)
        .map(EventAttributeQueryBuilder::toField)
        .map(Field::asVirtual)
        .forEach(builder::selectField);

    // Build conditions for dimensions with restrictions
    acceptedDimensions.stream()
        .filter(SqlQueryBuilders::hasRestrictions)
        .map(dimId -> GroupableCondition.of(dimId.getGroupId(), buildCondition(dimId, ctx)))
        .forEach(builder::groupableCondition);

    acceptedSortingParams.forEach(
        sortingParam -> {
          DimensionIdentifier<DimensionParam> dimId = sortingParam.getOrderBy();
          String fieldName = dimId.getDimension().getStaticDimension().getColumnName();

          builder.orderClause(
              IndexedOrder.of(
                  sortingParam.getIndex(),
                  org.hisp.dhis.analytics.common.query.Order.of(
                      SqlQueryHelper.buildOrderSubQuery(sortingParam.getOrderBy(), () -> fieldName),
                      sortingParam.getSortDirection())));
        });

    return builder.build();
  }

  private static Field toField(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    StaticDimension staticDimension = dimensionIdentifier.getDimension().getStaticDimension();
    String prefix = getPrefix(dimensionIdentifier, false);

    return Field.ofUnquoted(
        doubleQuote(prefix),
        staticDimension::getColumnName,
        prefix + DIMENSION_SEPARATOR + staticDimension.getHeaderName());
  }

  private Renderable buildCondition(DimensionIdentifier<DimensionParam> dimId, QueryContext ctx) {
    StaticDimension staticDimension = dimId.getDimension().getStaticDimension();

    return switch (staticDimension) {
      case EVENT_DATE ->
          SqlQueryHelper.buildExistsValueSubquery(
              dimId, PeriodStaticDimensionCondition.of(dimId, ctx));
      case SCHEDULED_DATE ->
          SqlQueryHelper.buildExistsValueSubqueryIncludeSchedule(
              dimId, PeriodStaticDimensionCondition.of(dimId, ctx));
      case OU ->
          SqlQueryHelper.buildExistsValueSubquery(dimId, EventOrgUnitCondition.of(dimId, ctx));
      case EVENT_STATUS ->
          SqlQueryHelper.buildExistsValueSubqueryIncludeSchedule(
              dimId, StatusCondition.of(dimId, ctx));
      default ->
          throw new IllegalArgumentException("Unsupported event attribute: " + staticDimension);
    };
  }
}
