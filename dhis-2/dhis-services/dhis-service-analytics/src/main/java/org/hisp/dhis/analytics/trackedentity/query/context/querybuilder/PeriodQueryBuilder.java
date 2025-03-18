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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static java.util.function.Predicate.not;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.DIMENSION_SEPARATOR;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getPrefix;
import static org.hisp.dhis.commons.util.TextUtils.doubleQuote;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.trackedentity.query.PeriodCondition;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilderAdaptor;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.Period;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for adding period conditions to the SQL query. By design, "Period"
 * conditions are grouped together in their own single group. This means that the period conditions
 * are not combined with other conditions and are rendered as a single group of "OR" conditions.
 */
@Service
@Getter
@org.springframework.core.annotation.Order(3)
public class PeriodQueryBuilder extends SqlQueryBuilderAdaptor {

  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(d -> d.getDimension().isPeriodDimension());

  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(sortingParams -> sortingParams.getOrderBy().getDimension().isPeriodDimension());

  @Override
  public RenderableSqlQuery buildSqlQuery(
      QueryContext ctx,
      List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      List<AnalyticsSortingParams> acceptedSortingParams) {
    RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

    streamDimensions(acceptedHeaders, acceptedDimensions, acceptedSortingParams)
        .filter(DimensionIdentifier::isTeDimension)
        .map(PeriodQueryBuilder::asField)
        .forEach(builder::selectField);

    streamDimensions(acceptedHeaders, acceptedDimensions, acceptedSortingParams)
        .filter(not(DimensionIdentifier::isTeDimension))
        .map(PeriodQueryBuilder::asField)
        // non TE periods are virtual fields, since those will be extracted from JSON
        .map(Field::asVirtual)
        .forEach(builder::selectField);

    acceptedDimensions.stream()
        .map(
            dimensionIdentifier ->
                GroupableCondition.of(
                    getGroupId(dimensionIdentifier),
                    SqlQueryHelper.buildExistsValueSubquery(
                        dimensionIdentifier, PeriodCondition.of(dimensionIdentifier, ctx))))
        .forEach(builder::groupableCondition);

    acceptedSortingParams.forEach(
        sortingParam -> {
          DimensionIdentifier<DimensionParam> dimensionIdentifier = sortingParam.getOrderBy();
          String fieldName = getTimeField(dimensionIdentifier, StaticDimension::getColumnName);

          builder.orderClause(
              IndexedOrder.of(
                  sortingParam.getIndex(),
                  Order.of(
                      SqlQueryHelper.buildOrderSubQuery(sortingParam.getOrderBy(), () -> fieldName),
                      sortingParam.getSortDirection())));
        });

    return builder.build();
  }

  /**
   * Gets the group id for the period condition based on the dimension identifier and the time
   * field.
   *
   * @param dimensionIdentifier the dimension identifier.
   * @return the group id.
   */
  private String getGroupId(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return dimensionIdentifier.getGroupId() + ":" + getTimeField(dimensionIdentifier, Enum::name);
  }

  private static Field asField(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    String field = getTimeField(dimensionIdentifier, StaticDimension::getColumnName);
    String alias = getTimeField(dimensionIdentifier, StaticDimension::getHeaderName);

    String prefix = getPrefix(dimensionIdentifier, false);

    return Field.ofUnquoted(doubleQuote(prefix), () -> field, prefix + DIMENSION_SEPARATOR + alias);
  }

  /**
   * Extracts the time field from the dimension identifier. If the dimension identifier is a period
   * dimension, the time field is extracted from the period object. If the dimension identifier is a
   * static dimension, the time field is extracted from the static dimension.
   *
   * @param dimensionIdentifier the dimension identifier.
   * @param staticDimensionNameExtractor the static dimension name extractor.
   * @return the time field.
   */
  private static String getTimeField(
      DimensionIdentifier<DimensionParam> dimensionIdentifier,
      Function<StaticDimension, String> staticDimensionNameExtractor) {
    return Optional.of(dimensionIdentifier)
        .map(DimensionIdentifier::getDimension)
        .map(DimensionParam::getDimensionalObject)
        .filter(DimensionalObject::hasItems)
        .map(d -> d.getItems().get(0))
        .map(Period.class::cast)
        .map(Period::getDateField)
        .map(TimeField::valueOf)
        .map(TimeField::getTrackedEntityColumnName)
        .orElseGet(
            () ->
                staticDimensionNameExtractor.apply(
                    dimensionIdentifier.getDimension().getStaticDimension()));
  }
}
