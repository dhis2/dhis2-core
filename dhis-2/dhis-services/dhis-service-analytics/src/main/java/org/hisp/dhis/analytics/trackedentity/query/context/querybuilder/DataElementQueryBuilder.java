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

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.fromValueType;
import static org.hisp.dhis.analytics.common.query.Field.ofUnquoted;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.system.grid.ListGrid.LEGEND;
import static org.hisp.dhis.util.TextUtils.doubleQuote;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.trackedentity.query.DataElementCondition;
import org.hisp.dhis.analytics.trackedentity.query.RenderableDataValue;
import org.hisp.dhis.analytics.trackedentity.query.SuffixedRenderableDataValue;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilders;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.ValueType;
import org.springframework.stereotype.Service;

/** Query builder for data elements. */
@Service
@Getter
@org.springframework.core.annotation.Order(999)
public class DataElementQueryBuilder implements SqlQueryBuilder {

  private final List<Predicate<DimensionIdentifier<DimensionParam>>> headerFilters =
      List.of(DimensionIdentifierHelper::isDataElement);

  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(DimensionIdentifierHelper::isDataElement);

  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(DataElementQueryBuilder::isDataElementOrder);

  private static final Map<ValueType, IdScheme> DEFAULT_ID_SCHEMES_BY_VALUE_TYPE =
      Map.of(ORGANISATION_UNIT, IdScheme.NAME);

  private static final Map<IdScheme, String> SUFFIX_BY_ID_SCHEME =
      Map.of(
          IdScheme.NAME, "_name",
          IdScheme.CODE, "_code");

  @Override
  public RenderableSqlQuery buildSqlQuery(
      @Nonnull QueryContext queryContext,
      @Nonnull List<DimensionIdentifier<DimensionParam>> acceptedHeaders,
      @Nonnull List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
      @Nonnull List<AnalyticsSortingParams> acceptedSortingParams) {
    RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

    // Select fields are the union of headers, dimensions and sorting params
    GroupedDimensions groupedDimensions =
        getGroupedDimensions(acceptedHeaders, acceptedDimensions, acceptedSortingParams);

    getValueFields(groupedDimensions).forEach(builder::selectField);

    // Groupable conditions comes from dimensions
    acceptedDimensions.stream()
        .filter(SqlQueryBuilders::hasRestrictions)
        .map(
            dimId ->
                GroupableCondition.of(
                    dimId.getGroupId(),
                    SqlQueryHelper.buildExistsValueSubquery(
                        dimId, DataElementCondition.of(queryContext, dimId))))
        .forEach(builder::groupableCondition);

    // Order clause comes from sorting params
    acceptedSortingParams.forEach(
        analyticsSortingParams ->
            builder.orderClause(
                IndexedOrder.of(
                    analyticsSortingParams.getIndex(),
                    Order.of(
                        SqlQueryHelper.buildOrderSubQuery(
                            analyticsSortingParams.getOrderBy(),
                            RenderableDataValue.of(
                                EMPTY,
                                analyticsSortingParams.getOrderBy().getDimension().getUid(),
                                fromValueType(
                                    analyticsSortingParams
                                        .getOrderBy()
                                        .getDimension()
                                        .getValueType()))),
                        analyticsSortingParams.getSortDirection()))));

    return builder.build();
  }

  /**
   * Returns the fields holding the value of data elements.
   *
   * @param groupedDimensions the list of groupedDimensions.
   * @return the stream of fields holding the value of data elements.
   */
  @Nonnull
  private static Stream<Field> getValueFields(GroupedDimensions groupedDimensions) {
    return groupedDimensions.getGroupsByKey().stream()
        .map(DimensionGroup::dimensions)
        .flatMap(Collection::stream)
        .map(DataElementQueryBuilder::toField)
        .map(Field::asVirtual);
  }

  /**
   * Converts the given dimension identifier to a field.
   *
   * @param dimensionIdentifier the dimension identifier to convert.
   * @return the field.
   */
  private static Field toField(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    String alias = dimensionIdentifier.getKey();
    if (dimensionIdentifier.hasLegendSet()) {
      alias += LEGEND;
    }
    Renderable renderableDataValue = getRenderableDataValue(dimensionIdentifier);
    return ofUnquoted(EMPTY, renderableDataValue, alias);
  }

  /**
   * Returns the renderable data value for the given dimension identifier, applying the necessary
   * logic based on id scheme and value type.
   *
   * @param dimensionIdentifier the dimension identifier.
   * @return the renderable data value.
   */
  private static Renderable getRenderableDataValue(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {

    ValueType valueType = dimensionIdentifier.getDimension().getValueType();

    IdScheme idScheme =
        Optional.of(dimensionIdentifier)
            .map(DimensionIdentifier::getDimension)
            .map(DimensionParam::getIdScheme)
            .orElse(nonNull(valueType) ? DEFAULT_ID_SCHEMES_BY_VALUE_TYPE.get(valueType) : null);

    if (nonNull(valueType)
        && nonNull(idScheme)
        && DEFAULT_ID_SCHEMES_BY_VALUE_TYPE.containsKey(valueType)
        && SUFFIX_BY_ID_SCHEME.containsKey(idScheme)) {
      return SuffixedRenderableDataValue.of(
          doubleQuote(dimensionIdentifier.getPrefix()),
          dimensionIdentifier.getDimension().getUid(),
          fromValueType(dimensionIdentifier.getDimension().getValueType()),
          SUFFIX_BY_ID_SCHEME.get(idScheme));
    }
    return withMaybeLegendSet(dimensionIdentifier);
  }

  /**
   * Returns the renderable data value for the given dimension identifier, applying the necessary
   * logic based on whether the dimension identifier has a legend set.
   *
   * @param dimensionIdentifier the dimension identifier.
   * @return the renderable data value.
   */
  private static Renderable withMaybeLegendSet(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    RenderableDataValue renderableDataValue =
        RenderableDataValue.of(
            doubleQuote(dimensionIdentifier.getPrefix()),
            dimensionIdentifier.getDimension().getUid(),
            fromValueType(dimensionIdentifier.getDimension().getValueType()));
    if (dimensionIdentifier.hasLegendSet()) {
      return RenderableDataValue.withLegendSet(
          renderableDataValue, dimensionIdentifier.getDimension().getQueryItem().getLegendSet());
    } else {
      return renderableDataValue.transformedIfNecessary();
    }
  }

  /**
   * Checks if the given sorting parameter is of type data element.
   *
   * @param analyticsSortingParams the sorting parameter to check.
   * @return true if the sorting parameter is of type data element, false otherwise.
   */
  private static boolean isDataElementOrder(AnalyticsSortingParams analyticsSortingParams) {
    return DimensionIdentifierHelper.isDataElement(analyticsSortingParams.getOrderBy());
  }
}
