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
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getEventLevelName;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getEventLevelRequestKey;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.isDataElement;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.isOrgUnitObject;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.ENROLLMENTDATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.ENROLLMENT_STATUS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EVENT_DATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EVENT_STATUS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.INCIDENTDATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.OCCURREDDATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.OU;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.OUNAME;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.PROGRAM_STATUS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.SCHEDULED_DATE;
import static org.hisp.dhis.analytics.trackedentity.query.context.QueryContextConstants.TRACKED_ENTITY_ALIAS;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_IDENTIFIER_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionAliases;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.event.data.stage.StageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.trackedentity.EventValue;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.DataElementCondition;
import org.hisp.dhis.analytics.trackedentity.query.EventOrgUnitCondition;
import org.hisp.dhis.analytics.trackedentity.query.OrganisationUnitCondition;
import org.hisp.dhis.analytics.trackedentity.query.PeriodStaticDimensionCondition;
import org.hisp.dhis.analytics.trackedentity.query.RenderableDataValue;
import org.hisp.dhis.analytics.trackedentity.query.ScopedColumnResolver;
import org.hisp.dhis.analytics.trackedentity.query.StatusCondition;
import org.hisp.dhis.analytics.trackedentity.query.context.TrackedEntityStaticField;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilders;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Builds the SELECT and GROUP BY of a tracked entity aggregate (grouped) query. Owns the SELECT in
 * aggregate mode: the requested dimensions become both select columns and group-by keys, plus a
 * trailing aggregate value column.
 */
@Service
@Order(0)
@RequiredArgsConstructor
public class AggregateQueryBuilder implements SqlQueryBuilder {

  /**
   * Renders the period bucket a grouped date is reduced to. Only {@link
   * StageDatePeriodBucketSqlRenderer#renderPeriodBucketExpression} is used, which is a pure
   * function over a date expression; the period items are read off the dimension by {@link
   * PeriodBucketColumn}, since this stack keeps them as the request wrote them.
   */
  private final StageDatePeriodBucketSqlRenderer periodBucketRenderer;

  /**
   * Alias of the collapsed program-stage event table joined when aggregating over an event value.
   */
  private static final String EVENT_VALUE_ALIAS = "ev";

  /** The static columns the tracked entity table carries, by the header name of the dimension. */
  private static final Set<String> TRACKED_ENTITY_COLUMNS =
      Arrays.stream(TrackedEntityStaticField.values())
          .map(TrackedEntityStaticField::getAlias)
          .collect(toUnmodifiableSet());

  /**
   * The event table column behind each event scoped dimension that can be grouped on, and whether
   * it holds a date grouped by period bucket. An org unit reaches this map only as a static
   * dimension; once its items have been resolved it arrives as a dimensional object and is
   * recognised by {@link
   * org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper#isOrgUnitObject}.
   */
  private static final Map<StaticDimension, GroupableColumn> GROUPABLE_EVENT_COLUMNS =
      Map.of(
          OU, new GroupableColumn("ou", false),
          EVENT_DATE, new GroupableColumn("occurreddate", true),
          SCHEDULED_DATE, new GroupableColumn("scheduleddate", true),
          EVENT_STATUS, new GroupableColumn("status", false));

  /**
   * The enrollment table column behind each enrollment scoped dimension that can be grouped on. The
   * set is explicit rather than taken from {@link StaticDimension#getColumnName()}, because not
   * every static dimension names a column the enrollment table has: an end date is {@code
   * completeddate} there, and an event status has no enrollment column at all. Both are left out,
   * so they are rejected instead of reaching the database.
   */
  private static final Map<StaticDimension, GroupableColumn> GROUPABLE_ENROLLMENT_COLUMNS =
      Map.of(
          OU, new GroupableColumn("ou", false),
          OUNAME, new GroupableColumn("ouname", false),
          ENROLLMENTDATE, new GroupableColumn("enrollmentdate", true),
          INCIDENTDATE, new GroupableColumn("occurreddate", true),
          OCCURREDDATE, new GroupableColumn("occurreddate", true),
          ENROLLMENT_STATUS, new GroupableColumn("enrollmentstatus", false),
          PROGRAM_STATUS, new GroupableColumn("enrollmentstatus", false));

  /**
   * Stands for a stage data element, whose value is read out of the event's data values rather than
   * from a column of its own.
   */
  private static final String DATA_VALUES_COLUMN = "eventdatavalues";

  /**
   * The table column a groupable scoped dimension reads, and whether it holds a date grouped by
   * period bucket rather than as a raw timestamp.
   */
  private record GroupableColumn(String column, boolean dateBucketed) {}

  /**
   * A sort on a scoped dimension is claimed here so that the order clause is built from the same
   * expression the query groups on. Whether the dimension really is grouped can only be decided
   * with the query context, so it is checked again in {@link #buildSqlQuery}.
   */
  @Getter
  private final List<Predicate<AnalyticsSortingParams>> sortingFilters =
      List.of(sortingParams -> groupableScopedColumn(sortingParams.getOrderBy()).isPresent());

  @Getter
  private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters =
      List.of(
          dimension ->
              OrgUnitQueryBuilder.isOu(dimension)
                  || TrackedEntityQueryBuilder.isTrackedEntity(dimension)
                  || groupableScopedColumn(dimension).isPresent());

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
    // The expression of each grouped dimension is built once here and reused by its select column,
    // its group by key and any order clause on it. A sorting parameter carries no items of its own,
    // so it cannot rebuild an identical expression: a bucketed date would order by the raw date
    // while the query grouped by the bucket, which is invalid grouped SQL.
    Map<String, Renderable> groupedExpressions = new LinkedHashMap<>();

    acceptedDimensions.stream()
        .filter(dimension -> groupedKeys.contains(dimension.getKey()))
        .forEach(
            dimension ->
                groupedExpressions.put(
                    dimension.getKey(), addGroupedDimension(queryContext, builder, dimension)));

    // A program-stage data element value is aggregated from the collapsed event row, joined at
    // tracked-entity grain so the GROUP BY counts tracked entities, not events.
    EventValue eventValue = queryContext.getContextParams().getTypedParsed().getEventValue();
    if (eventValue != null) {
      builder.leftJoin(
          SqlQueryHelper.buildEventValueLeftJoin(
              eventValue, queryContext.getTetTableSuffix(), EVENT_VALUE_ALIAS));
    }

    acceptedSortingParams.stream()
        .filter(sortingParam -> groupedExpressions.containsKey(sortingParam.getOrderBy().getKey()))
        .forEach(
            sortingParam ->
                builder.orderClause(
                    IndexedOrder.of(
                        sortingParam.getIndex(),
                        org.hisp.dhis.analytics.common.query.Order.of(
                            groupedExpressions.get(sortingParam.getOrderBy().getKey()),
                            sortingParam.getSortDirection()))));

    // The aggregate value column is the last select column and is not grouped.
    builder.selectField(
        Field.ofUnquoted("", () -> valueExpression(queryContext.getContextParams()), "value"));

    return builder.build();
  }

  /**
   * Contributes a dimension as both a select column and a group by key. A tracked entity dimension
   * is a column on the main table, so one alias free field serves both clauses. An event scoped
   * dimension is a subquery instead, and needs two fields: the select column is aliased to the
   * header name, because the grid reads each row by header name, while the group by repeats the
   * bare expression, since an alias is not a valid group by key.
   */
  private Renderable addGroupedDimension(
      QueryContext queryContext,
      RenderableSqlQuery.RenderableSqlQueryBuilder builder,
      DimensionIdentifier<DimensionParam> dimension) {
    Optional<GroupableColumn> scopedColumn = groupableScopedColumn(dimension);

    if (scopedColumn.isEmpty()) {
      Field field = Field.ofDimensionIdentifier(dimension);
      builder.selectField(field);
      builder.groupByField(field);
      return field;
    }

    Renderable expression = groupedScopedExpression(dimension, scopedColumn.get());
    builder.selectField(Field.ofUnquoted(expression, groupedDimensionName(dimension)));
    builder.groupByField(Field.ofUnquoted(expression, ""));

    if (SqlQueryBuilders.hasRestrictions(dimension)) {
      builder.groupableCondition(
          GroupableCondition.of(
              dimension.getGroupId(),
              scopedRestriction(queryContext, dimension, scopedColumn.get())));
    }
    return expression;
  }

  /**
   * Returns the restriction of a grouped scoped dimension, read from the same row the dimension is
   * grouped on. The builders that own the restriction of a dimension the query does not group by
   * step aside for one it does, see {@link #isGroupedInAggregate}, so that a request does not
   * filter on one event and group on another.
   *
   * <p>A date is restricted on the date itself rather than on its period bucket, so an explicit
   * period and a date range restrict the same way.
   */
  private Renderable scopedRestriction(
      QueryContext queryContext,
      DimensionIdentifier<DimensionParam> dimension,
      GroupableColumn column) {
    ScopedColumnResolver columnResolver = name -> scopedExpression(dimension, name);

    if (isOrgUnitUid(dimension)) {
      return dimension.isEventDimension()
          ? EventOrgUnitCondition.of(dimension, queryContext, columnResolver)
          : OrganisationUnitCondition.of(dimension, queryContext, columnResolver);
    }

    if (isGroupableDataElement(dimension)) {
      return DataElementCondition.of(
          queryContext,
          dimension,
          valueTypeMapping -> dataElementExpression(dimension, valueTypeMapping));
    }

    if (column.dateBucketed()) {
      return PeriodStaticDimensionCondition.of(dimension, queryContext, columnResolver);
    }

    return StatusCondition.of(dimension, queryContext, columnResolver);
  }

  /**
   * Whether the dimension holds an org unit uid, which is the {@code ou} dimension in either of the
   * shapes it is parsed into. The other org unit dimensions, such as {@code ouname}, hold a name
   * rather than a uid.
   *
   * @param dimension the dimension identifier.
   * @return true when the dimension holds an org unit uid.
   */
  public static boolean isOrgUnitUid(DimensionIdentifier<DimensionParam> dimension) {
    return isOrgUnitObject(dimension) || isStaticOrgUnit(dimension);
  }

  private static boolean isStaticOrgUnit(DimensionIdentifier<DimensionParam> dimension) {
    return dimension.getDimension().isStaticDimension()
        && dimension.getDimension().getStaticDimension() == OU;
  }

  /**
   * Whether this builder owns the given dimension's restriction, select column and ordering, which
   * is the case only for a scoped dimension the aggregate query groups by. A builder that would
   * otherwise contribute any of the three must step aside when this holds, so that the query
   * filters, groups and orders on the same event or enrollment.
   *
   * <p>A tracked entity dimension is deliberately excluded even when grouped: this builder does not
   * produce its restriction, so claiming it would drop the restriction altogether and answer over
   * every item rather than the requested ones.
   */
  public static boolean isGroupedInAggregate(
      QueryContext queryContext, DimensionIdentifier<DimensionParam> dimension) {
    return queryContext.isAggregate()
        && groupableScopedColumn(dimension).isPresent()
        && getGroupedDimensionKeys(queryContext.getContextParams()).contains(dimension.getKey());
  }

  /**
   * Returns the expression an event scoped dimension is grouped on: the column of the single event
   * chosen for each tracked entity, reduced to a period bucket when the dimension is a date the
   * request asked for by period. A date without a resolvable period is grouped as it is, which is
   * how the enrollment aggregate endpoint answers the same request.
   */
  private Renderable groupedScopedExpression(
      DimensionIdentifier<DimensionParam> dimension, GroupableColumn column) {
    Renderable scopedExpression = scopedExpression(dimension, column.column());

    if (!column.dateBucketed()) {
      return scopedExpression;
    }

    return PeriodBucketColumn.of(dimension)
        .map(
            bucket ->
                (Renderable)
                    () ->
                        periodBucketRenderer.renderPeriodBucketExpression(
                            scopedExpression.render(), bucket))
        .orElse(scopedExpression);
  }

  /**
   * Returns the expression reading the given column from the single event or enrollment chosen for
   * each tracked entity. The enrollment expression is the one the row level endpoint already sorts
   * and filters on, so a grouped enrollment dimension agrees with a sort on the same dimension.
   *
   * <p>Every grouped field of a program stage reads the same event, so a grouped row always
   * describes one real event. That event is the one {@code value=} aggregates over, which is why
   * scheduled events are excluded: a scheduled event carries no data values, so letting one win the
   * offset would empty the aggregated value.
   *
   * <p>The consequence is that {@code EVENT_STATUS} never groups a tracked entity under {@code
   * SCHEDULE}, since a scheduled event is never the chosen row.
   */
  private static Renderable scopedExpression(
      DimensionIdentifier<DimensionParam> dimension, String column) {
    if (isGroupableDataElement(dimension)) {
      return dataElementExpression(
          dimension, ValueTypeMapping.fromValueType(dimension.getDimension().getValueType()));
    }

    return dimension.isEventDimension()
        ? SqlQueryHelper.buildCollapsedEventSubquery(dimension, column)
        : SqlQueryHelper.buildOrderSubQuery(dimension, () -> column);
  }

  /**
   * Returns the expression reading a stage data element's value out of the single event chosen for
   * each tracked entity.
   */
  private static Renderable dataElementExpression(
      DimensionIdentifier<DimensionParam> dimension, ValueTypeMapping valueTypeMapping) {
    return SqlQueryHelper.buildCollapsedEventValueSubquery(
        dimension,
        RenderableDataValue.of(
            SqlQueryHelper.COLLAPSED_EVENT_ALIAS,
            dimension.getDimension().getUid(),
            valueTypeMapping));
  }

  /**
   * Whether the dimension is a stage data element the query can group on. A legend set is not
   * grouped: the row level path renders the legend rather than the value, and a grouped legend is
   * not part of this contract.
   */
  private static boolean isGroupableDataElement(DimensionIdentifier<DimensionParam> dimension) {
    return isDataElement(dimension) && !dimension.hasLegendSet();
  }

  /**
   * Returns the table column a program or stage scoped dimension groups on, or empty when the
   * dimension is not one this query can group on. An event scoped dimension reads the event table
   * and an enrollment scoped one the enrollment table, so the two have their own supported sets.
   */
  static Optional<GroupableColumn> groupableScopedColumn(
      DimensionIdentifier<DimensionParam> dimension) {
    if (isGroupableDataElement(dimension)) {
      return Optional.of(new GroupableColumn(DATA_VALUES_COLUMN, false));
    }

    if (dimension.isEventDimension()) {
      return scopedColumn(dimension, GROUPABLE_EVENT_COLUMNS);
    }

    if (dimension.isEnrollmentDimension()) {
      return scopedColumn(dimension, GROUPABLE_ENROLLMENT_COLUMNS);
    }

    return Optional.empty();
  }

  private static Optional<GroupableColumn> scopedColumn(
      DimensionIdentifier<DimensionParam> dimension,
      Map<StaticDimension, GroupableColumn> columns) {
    if (isOrgUnitObject(dimension)) {
      return Optional.ofNullable(columns.get(OU));
    }

    if (!dimension.getDimension().isStaticDimension()) {
      return Optional.empty();
    }

    return Optional.ofNullable(columns.get(dimension.getDimension().getStaticDimension()));
  }

  /**
   * Returns the name a grouped dimension is reported under, which is also the alias its select
   * column carries. An event scoped dimension is named by its stage scoped form so that the
   * response matches the enrollment aggregate endpoint. Every other dimension keeps its resolved
   * key.
   *
   * <p>This name drops the stage offset, so two offsets of one stage would be reported under one
   * name; such a request is rejected rather than answered ambiguously. Request identity keeps the
   * offset and is answered by {@link
   * org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper#getEventLevelRequestKey}.
   */
  public static String groupedDimensionName(DimensionIdentifier<DimensionParam> dimension) {
    return getEventLevelName(dimension).orElseGet(dimension::getKey);
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
   * dimension} param that can also be grouped on. Those are the registration org unit, the tracked
   * entity static fields, the attributes, and the program or stage scoped dimensions that {@link
   * #groupableScopedColumn} names a column for. The raw request is needed because the parsed
   * dimensions also contain the attributes the mapper adds for row level display, and those must
   * stay out of the GROUP BY. What is grouped here is also what can be sorted on.
   *
   * <p>A scoped dimension is read from the enrollment or event table rather than from a tracked
   * entity column, so its group by key is the collapsed subquery that picks one row per tracked
   * entity. A dimension neither the tracked entity table nor that collapse can produce, such as an
   * unscoped {@code enrollmentdate} or a program scoped event status, is rejected rather than
   * silently applied as a restriction.
   */
  public static Set<String> getGroupedDimensionKeys(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    return groupedDimensions(contextParams).map(DimensionIdentifier::getKey).collect(toSet());
  }

  /**
   * Returns the {@code dimension} parameters the query groups by, in the form the request wrote
   * them. A request addresses an event scoped dimension by its stage, {@code A03MvHHogjR.ou}, while
   * the parsed dimension also carries the program the stage belongs to, so the two forms differ and
   * {@link #getGroupedDimensionKeys} cannot answer for the raw request.
   */
  public static Set<String> getGroupedRequestKeys(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    Set<String> requestedKeys = requestedKeys(contextParams);

    return groupedDimensions(contextParams)
        .flatMap(dimension -> requestKeysOf(dimension).stream())
        .filter(requestedKeys::contains)
        .collect(toSet());
  }

  /**
   * Returns the parsed dimensions the query groups by: those asked for in the {@code dimension}
   * param that can also be grouped on.
   */
  private static Stream<DimensionIdentifier<DimensionParam>> groupedDimensions(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    Set<String> requestedKeys = requestedKeys(contextParams);

    return contextParams.getCommonParsed().getDimensionIdentifiers().stream()
        .filter(AggregateQueryBuilder::isGroupable)
        .filter(dimension -> requestedKeys.stream().anyMatch(requestKeysOf(dimension)::contains));
  }

  /**
   * Whether the dimension can be a group by key: a tracked entity dimension with a column on the
   * tracked entity table, or an event scoped dimension whose value the query can read from the
   * single event chosen for each tracked entity.
   */
  private static boolean isGroupable(DimensionIdentifier<DimensionParam> dimension) {
    if (dimension.isTeDimension()) {
      return OrgUnitQueryBuilder.isOu(dimension) || hasTrackedEntityColumn(dimension);
    }

    return groupableScopedColumn(dimension).isPresent();
  }

  /**
   * Returns the keys a request can address the dimension by: its resolved key, and for an event
   * scoped dimension also the shorter stage scoped form.
   */
  private static Set<String> requestKeysOf(DimensionIdentifier<DimensionParam> dimension) {
    return getEventLevelRequestKey(dimension)
        .map(key -> Set.of(dimension.getKey(), key))
        .orElseGet(() -> Set.of(dimension.getKey()));
  }

  private static Set<String> requestedKeys(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    return contextParams.getCommonRaw().getDimension().stream()
        .map(AggregateQueryBuilder::canonicalDimensionKey)
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
