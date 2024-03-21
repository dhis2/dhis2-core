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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.fromValueType;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType.DATA_ELEMENT;
import static org.hisp.dhis.analytics.common.query.Field.ofUnquoted;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.isOfType;
import static org.hisp.dhis.commons.util.TextUtils.doubleQuote;
import static org.hisp.dhis.system.grid.ListGrid.EXISTS;
import static org.hisp.dhis.system.grid.ListGrid.HAS_VALUE;
import static org.hisp.dhis.system.grid.ListGrid.STATUS;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.DataElementCondition;
import org.hisp.dhis.analytics.tei.query.RenderableDataValue;
import org.hisp.dhis.analytics.tei.query.StageExistsRenderable;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders;
import org.springframework.stereotype.Service;

/** Query builder for data elements. */
@Service
@org.springframework.core.annotation.Order(999)
public class DataElementQueryBuilder implements SqlQueryBuilder {

  @Getter
  private final List<Predicate<DimensionIdentifier<DimensionParam>>> headerFilters =
      List.of(DataElementQueryBuilder::isDataElement);

  @Getter
  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(DataElementQueryBuilder::isDataElement);

  @Getter
  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(DataElementQueryBuilder::isDataElementOrder);

  @Override
  public RenderableSqlQuery buildSqlQuery(
      QueryContext queryContext,
      List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
      List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      List<AnalyticsSortingParams> acceptedSortingParams) {
    RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

    // Select fields are the union of headers, dimensions and sorting params
    List<DimensionIdentifier<DimensionParam>> dimensions =
        streamDimensions(acceptedHeaders, acceptedDimensions, acceptedSortingParams).toList();

    Stream.of(
            // Fields holding the value of data elements
            getValueFields(dimensions),
            // Fields holding the "exists" flag of the stages
            getExistsFields(dimensions),
            // Fields holding the status of the stages (SCHEDULED, COMPLETE, etc)
            getStatusFields(dimensions),
            // Fields holding the "hasValue" flag of the data elements
            getHasValueFields(dimensions))
        .flatMap(Function.identity())
        .forEach(builder::selectField);

    // Groupable conditions comes from dimensions
    acceptedDimensions.stream()
        .filter(SqlQueryBuilders::hasRestrictions)
        .map(
            dimId ->
                GroupableCondition.of(
                    dimId.getGroupId(), DataElementCondition.of(queryContext, dimId)))
        .forEach(builder::groupableCondition);

    // Order clause comes from sorting params
    acceptedSortingParams.forEach(
        analyticsSortingParams ->
            builder.orderClause(
                IndexedOrder.of(
                    analyticsSortingParams.getIndex(),
                    Order.of(
                        Field.of(analyticsSortingParams.getOrderBy().toString()),
                        analyticsSortingParams.getSortDirection()))));

    return builder.build();
  }

  private Stream<Field> getHasValueFields(List<DimensionIdentifier<DimensionParam>> dimensions) {
    return dimensions.stream()
        .map(
            dimensionIdentifier -> {
              String prefix = dimensionIdentifier.getPrefix();
              return ofUnquoted(
                  EMPTY,
                  () ->
                      doubleQuote(prefix)
                          + ".eventdatavalues :: jsonb ?? '"
                          + dimensionIdentifier.getDimension().getUid()
                          + "'",
                  dimensionIdentifier + HAS_VALUE);
            });
  }

  private Stream<Field> getStatusFields(List<DimensionIdentifier<DimensionParam>> dimensions) {
    return dimensions.stream()
        .map(
            dimId ->
                ofUnquoted(
                    EMPTY,
                    () -> doubleQuote(dimId.getPrefix()) + STATUS,
                    dimId.toString() + STATUS));
  }

  private Stream<Field> getExistsFields(List<DimensionIdentifier<DimensionParam>> dimensions) {
    return dimensions.stream()
        .map(dim -> ofUnquoted(EMPTY, StageExistsRenderable.of(dim), dim.getKey() + EXISTS));
  }

  @Nonnull
  private static Stream<Field> getValueFields(
      List<DimensionIdentifier<DimensionParam>> dimensions) {
    return dimensions.stream()
        .map(
            dimensionIdentifier ->
                ofUnquoted(
                    EMPTY,
                    RenderableDataValue.of(
                            doubleQuote(dimensionIdentifier.getPrefix()),
                            dimensionIdentifier.getDimension().getUid(),
                            fromValueType(dimensionIdentifier.getDimension().getValueType()))
                        .transformedIfNecessary(),
                    dimensionIdentifier.toString()));
  }

  /**
   * Checks if the given sorting parameter is of type data element.
   *
   * @param analyticsSortingParams the sorting parameter to check.
   * @return true if the sorting parameter is of type data element, false otherwise.
   */
  private static boolean isDataElementOrder(AnalyticsSortingParams analyticsSortingParams) {
    return isDataElement(analyticsSortingParams.getOrderBy());
  }

  /**
   * Checks if the given dimension identifier is of type data element.
   *
   * @param dimensionIdentifier the dimension identifier to check.
   * @return true if the dimension identifier is of type data element, false otherwise.
   */
  private static boolean isDataElement(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return isOfType(dimensionIdentifier, DATA_ELEMENT) && dimensionIdentifier.isEventDimension();
  }
}
