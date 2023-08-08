/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.DIMENSION_SEPARATOR;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getPrefix;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.ENROLLMENT_STATUS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EVENT_STATUS;
import static org.hisp.dhis.commons.util.TextUtils.doubleQuote;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.StatusCondition;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilderAdaptor;
import org.springframework.stereotype.Service;

@Service
public class StatusQueryBuilder extends SqlQueryBuilderAdaptor {
  /** The supported status dimensions. */
  private static final Collection<DimensionParam.StaticDimension> SUPPORTED_STATUS_DIMENSIONS =
      List.of(ENROLLMENT_STATUS, EVENT_STATUS);

  @Getter
  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(StatusQueryBuilder::isStatusDimension);

  @Getter
  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(sortingParams -> isStatusDimension(sortingParams.getOrderBy()));

  private static boolean isStatusDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return Optional.of(dimensionIdentifier)
        .map(DimensionIdentifier::getDimension)
        .map(DimensionParam::getStaticDimension)
        .filter(SUPPORTED_STATUS_DIMENSIONS::contains)
        .isPresent();
  }

  @Override
  public RenderableSqlQuery buildSqlQuery(
      QueryContext ctx,
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      List<AnalyticsSortingParams> acceptedSortingParams) {
    RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

    Stream.concat(
            acceptedDimensions.stream(),
            acceptedSortingParams.stream().map(AnalyticsSortingParams::getOrderBy))
        .map(
            dimensionIdentifier -> {
              String field =
                  dimensionIdentifier.getDimension().getStaticDimension().getColumnName();
              String prefix = getPrefix(dimensionIdentifier, false);

              return Field.ofUnquoted(
                  doubleQuote(prefix), () -> field, prefix + DIMENSION_SEPARATOR + field);
            })
        .forEach(builder::selectField);

    acceptedDimensions.stream()
        .map(dimensionIdentifier -> StatusCondition.of(dimensionIdentifier, ctx))
        .map(GroupableCondition::ofUngroupedCondition)
        .forEach(builder::groupableCondition);

    acceptedSortingParams.forEach(
        sortingParam -> {
          DimensionIdentifier<DimensionParam> dimensionIdentifier = sortingParam.getOrderBy();
          String fieldName =
              dimensionIdentifier.getDimension().getStaticDimension().getColumnName();

          Field field =
              Field.ofUnquoted(
                  getPrefix(sortingParam.getOrderBy()), () -> fieldName, StringUtils.EMPTY);
          builder.orderClause(
              IndexedOrder.of(
                  sortingParam.getIndex(), Order.of(field, sortingParam.getSortDirection())));
        });

    return builder.build();
  }
}
