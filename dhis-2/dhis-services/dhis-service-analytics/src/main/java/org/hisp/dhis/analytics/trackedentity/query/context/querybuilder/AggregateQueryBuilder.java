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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.analytics.trackedentity.query.context.QueryContextConstants.TRACKED_ENTITY_ALIAS;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionAliases;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.EventValue;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.RenderableDataValue;
import org.hisp.dhis.analytics.trackedentity.query.context.TrackedEntityStaticField;
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

  /**
   * Alias of the collapsed program-stage event table joined when aggregating over an event value.
   */
  private static final String EVENT_VALUE_ALIAS = "ev";

  /** The static columns the tracked entity table carries, by the header name of the dimension. */
  private static final Set<String> TRACKED_ENTITY_COLUMNS =
      Arrays.stream(TrackedEntityStaticField.values())
          .map(TrackedEntityStaticField::getAlias)
          .collect(toUnmodifiableSet());

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

    // A program-stage data element value is aggregated from the collapsed event row, joined at
    // tracked-entity grain so the GROUP BY counts tracked entities, not events.
    EventValue eventValue = queryContext.getContextParams().getTypedParsed().getEventValue();
    if (eventValue != null) {
      builder.leftJoin(
          SqlQueryHelper.buildEventValueLeftJoin(
              eventValue, queryContext.getTetTableSuffix(), EVENT_VALUE_ALIAS));
    }

    // The aggregate value column is the last select column and is not grouped.
    builder.selectField(
        Field.ofUnquoted("", () -> valueExpression(queryContext.getContextParams()), "value"));

    return builder.build();
  }

  /**
   * Returns the SQL expression of the aggregate value column. Without a value the query counts
   * TEIs. Over a tracked entity attribute the function is applied to the attribute column; over a
   * program-stage data element it is applied to the value extracted from the collapsed event row.
   * An explicit COUNT then counts non-null values, matching the event/enrollment aggregate
   * contract.
   */
  private static String valueExpression(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    TrackedEntityQueryParams params = contextParams.getTypedParsed();

    EventValue eventValue = params.getEventValue();
    if (eventValue != null) {
      String value =
          RenderableDataValue.of(
                  EVENT_VALUE_ALIAS,
                  eventValue.dataElement().getUid(),
                  ValueTypeMapping.fromValueType(eventValue.dataElement().getValueType()))
              .render();
      return params.getAggregationType().getValue() + "(" + value + ")";
    }

    if (params.getAttributeValue() == null) {
      return "count(1)";
    }

    return params.getAggregationType().getValue()
        + "("
        + TRACKED_ENTITY_ALIAS
        + ".\""
        + params.getAttributeValue().getUid()
        + "\")";
  }

  /**
   * Returns the keys of the dimensions the query groups by: the ones asked for in the {@code
   * dimension} param that can also be grouped on, which are the registration org unit, the tracked
   * entity static fields and the attributes. The raw request is needed because the parsed
   * dimensions also contain the attributes the mapper adds for row level display, and those must
   * stay out of the GROUP BY. What is grouped here is also what can be sorted on.
   *
   * <p>A dimension can be grouped on only when the tracked entity table has a column for it, which
   * takes both its scope and its own level into account. A program or stage scoped org unit is the
   * enrollment or event org unit and lives in the enrollment and event tables; an enrollment or
   * event level field such as {@code enrollmentdate} has no tracked entity column either, even
   * without a program or stage prefix. Both can restrict the query but neither can be a group by
   * key.
   */
  public static Set<String> getGroupedDimensionKeys(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    Set<String> requestedKeys =
        contextParams.getCommonRaw().getDimension().stream()
            .map(AggregateQueryBuilder::canonicalDimensionKey)
            .collect(toSet());

    return contextParams.getCommonParsed().getDimensionIdentifiers().stream()
        .filter(DimensionIdentifier::isTeDimension)
        .filter(
            dimension -> OrgUnitQueryBuilder.isOu(dimension) || hasTrackedEntityColumn(dimension))
        .map(DimensionIdentifier::getKey)
        .filter(requestedKeys::contains)
        .collect(toSet());
  }

  /**
   * Whether the dimension has a column on the tracked entity table to group on. A static dimension
   * has one when it is one of the tracked entity fields the table flattens, so an enrollment or
   * event level field such as {@code enrollmentdate} or {@code eventstatus} has none even when it
   * is requested without a program or stage prefix. An attribute always has its own column. A
   * period dimension has none: the table carries no period column.
   */
  private static boolean hasTrackedEntityColumn(DimensionIdentifier<DimensionParam> dimension) {
    DimensionParam dimensionParam = dimension.getDimension();

    if (dimensionParam.isStaticDimension()) {
      return TRACKED_ENTITY_COLUMNS.contains(dimensionParam.getStaticDimension().getHeaderName());
    }

    return TrackedEntityQueryBuilder.isTrackedEntity(dimension)
        && !dimensionParam.isPeriodDimension();
  }

  /**
   * Returns the key a raw {@code dimension} parameter is parsed into, so that a request can be
   * matched against the parsed dimensions. Two resolutions are applied to the dimension id, in the
   * order the request parsing applies them: a keyword alias is replaced by the dimension it stands
   * for, e.g. {@code ENROLLMENT_OU} by {@code ou}, and a static dimension is resolved to its
   * canonical header name, e.g. {@code LAST_UPDATED} to {@code lastupdated}. Any program and stage
   * prefix is kept.
   *
   * @see DimensionAliases#canonicalize(String)
   */
  public static String canonicalDimensionKey(String rawDimension) {
    String key = getDimensionFromParam(rawDimension);
    int separator = key.lastIndexOf(DIMENSION_IDENTIFIER_SEP);
    String prefix = separator < 0 ? "" : key.substring(0, separator + 1);
    String dimension = DimensionAliases.canonicalize(key.substring(separator + 1));

    return prefix
        + StaticDimension.of(dimension).map(StaticDimension::getHeaderName).orElse(dimension);
  }

  @Override
  public boolean alwaysRun() {
    return true;
  }
}
