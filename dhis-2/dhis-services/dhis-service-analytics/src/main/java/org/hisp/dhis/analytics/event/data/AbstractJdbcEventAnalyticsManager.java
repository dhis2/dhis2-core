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
package org.hisp.dhis.analytics.event.data;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.math.NumberUtils.createDouble;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import static org.hisp.dhis.analytics.AggregationType.CUSTOM;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.AnalyticsConstants.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.AnalyticsConstants.NULL;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.analytics.SortOrder.DESC;
import static org.hisp.dhis.analytics.common.CteContext.ENROLLMENT_AGGR_BASE;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.PROGRAM_INDICATOR_ENROLLMENT;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.SHADOW_ENROLLMENT_TABLE;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.SHADOW_EVENT_TABLE;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.TOP_ENROLLMENTS;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getHeaderColumns;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getOrgUnitLevelColumns;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getPeriodColumns;
import static org.hisp.dhis.analytics.table.ColumnSuffix.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.table.ColumnSuffix.OU_NAME_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValue;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.replaceStringBetween;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.common.ValueType.REFERENCE;
import static org.hisp.dhis.commons.collection.ListUtils.union;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.feedback.ErrorCode.E7149;
import static org.hisp.dhis.system.util.MathUtils.getRounded;
import static org.hisp.dhis.system.util.MathUtils.getRoundedObject;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.MeasureFilter;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.CteUtils;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.common.InQueryCteFilter;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagDataHandler;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.analytics.util.sql.Condition;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.analytics.util.sql.SqlConditionJoiner;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.InQueryFilter;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.Reference;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Markus Bekken
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractJdbcEventAnalyticsManager {
  protected static final String COL_COUNT = "count";

  protected static final String COL_EXTENT = "extent";

  protected static final int COORD_DEC = 6;

  protected static final int LAST_VALUE_YEARS_OFFSET = -10;

  static final String COL_VALUE = "value";

  static final String OUTER_SQL_ALIAS = "t1";

  private static final String AND = " and ";

  private static final String OR = " or ";

  private static final String LIMIT = "limit";

  private static final Collector<CharSequence, ?, String> OR_JOINER = joining(OR, "(", ")");

  private static final Collector<CharSequence, ?, String> AND_JOINER = joining(AND);

  @Qualifier("analyticsReadOnlyJdbcTemplate")
  protected final JdbcTemplate jdbcTemplate;

  protected final ProgramIndicatorService programIndicatorService;

  protected final ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;

  protected final PiDisagInfoInitializer piDisagInfoInitializer;

  protected final PiDisagQueryGenerator piDisagQueryGenerator;

  protected final ExecutionPlanStore executionPlanStore;

  protected final AnalyticsSqlBuilder sqlBuilder;

  protected final SystemSettingsService settingsService;

  private final DhisConfigurationProvider config;

  private final OrganisationUnitResolver organisationUnitResolver;

  protected final ColumnMapper columnMapper;

  static final String ANALYTICS_EVENT = "analytics_event_";

  static final String COLUMN_ENROLLMENT_GEOMETRY_GEOJSON =
      String.format(
          "ST_AsGeoJSON(%s)", EnrollmentAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME);

  /**
   * Returns a SQL paging clause.
   *
   * @param params the {@link EventQueryParams}.
   * @param maxLimit the configurable max limit of records.
   */
  protected String getPagingClause(EventQueryParams params, int maxLimit) {
    String sql = "";

    if (params.isPaging()) {
      int limit =
          params.isTotalPages()
              ? params.getPageSizeWithDefault()
              : params.getPageSizeWithDefault() + 1;

      sql += LIMIT + " " + limit + " offset " + params.getOffset();
    } else if (maxLimit > 0) {
      sql += LIMIT + " " + (maxLimit + 1);
    }

    return sql;
  }

  /**
   * Returns a SQL sort clause.
   *
   * @param params the {@link EventQueryParams}.
   */
  protected String getSortClause(EventQueryParams params) {
    String sql = "";

    if (params.isSorting()) {
      sql += "order by " + getSortColumns(params, ASC) + getSortColumns(params, DESC);

      sql = TextUtils.removeLastComma(sql) + " ";
    }

    return sql;
  }

  private String getSortColumns(EventQueryParams params, SortOrder order) {
    String sql = "";

    for (QueryItem item : order == ASC ? params.getAsc() : params.getDesc()) {
      if (item.getItem().getDimensionItemType() == PROGRAM_INDICATOR) {
        sql += quote(item.getItem().getUid());
      } else if (item.getItem().getDimensionItemType() == DATA_ELEMENT) {
        sql += getSortColumnForDataElementDimensionType(item);
      } else {
        // Query returns UIDs but we want sorting on name or shortName
        // depending on the display property for OUGS and COGS
        sql +=
            Optional.ofNullable(extract(params.getDimensions(), item.getItem()))
                .filter(this::isSupported)
                .filter(DimensionalObject::hasItems)
                .map(
                    dim -> toCase(dim, quote(item.getItem().getUid()), params.getDisplayProperty()))
                .orElse(quote(item.getItem().getUid()));
      }

      sql += order == ASC ? " asc nulls last," : " desc nulls last,";
    }

    return sql;
  }

  private String getSortColumnForDataElementDimensionType(QueryItem item) {
    if (ValueType.ORGANISATION_UNIT == item.getValueType()) {
      return quote(item.getItemName() + OU_NAME_COL_SUFFIX);
    }

    if (item.hasRepeatableStageParams()) {
      return quote(item.getRepeatableStageParams().getDimension());
    }

    if (item.getProgramStage() != null) {
      return quote(item.getProgramStage().getUid() + "." + item.getItem().getUid());
    }

    return quote(item.getItem().getUid());
  }

  /**
   * Builds a CASE statement to use in sorting, mapping each OUGS and COGS identifiers into its name
   * or short name.
   */
  private String toCase(
      DimensionalObject dimension, String quotedAlias, DisplayProperty displayProperty) {
    return dimension.getItems().stream()
        .map(dio -> toWhenEntry(dio, quotedAlias, displayProperty))
        .collect(Collectors.joining(" ", "(CASE ", " ELSE '' END)"));
  }

  /** Builds a WHEN statement based on the given {@link DimensionalItemObject}. */
  private String toWhenEntry(DimensionalItemObject item, String quotedAlias, DisplayProperty dp) {
    return "WHEN "
        + quotedAlias
        + "="
        + singleQuote(item.getUid())
        + " THEN "
        + (dp == DisplayProperty.NAME
            ? singleQuote(item.getName())
            : singleQuote(item.getShortName()));
  }

  private boolean isSupported(DimensionalObject dimension) {
    return dimension.getDimensionType() == DimensionType.ORGANISATION_UNIT_GROUP_SET
        || dimension.getDimensionType() == DimensionType.CATEGORY_OPTION_GROUP_SET;
  }

  private DimensionalObject extract(
      List<DimensionalObject> dimensions, DimensionalItemObject item) {
    return dimensions.stream()
        .filter(dimensionalObject -> dimensionalObject.getUid().equals(item.getUid()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns the dynamic select column names to use in a group by clause. Dimensions come first and
   * query items second. Program indicator expressions are converted to SQL expressions. When
   * grouping with non-default analytics period boundaries, all periods are skipped in the group
   * clause, as non default boundaries is defining their own period groups within their where
   * clause.
   */
  protected List<String> getGroupByColumnNames(EventQueryParams params, boolean isAggregated) {
    return getSelectColumns(params, true, isAggregated);
  }

  /**
   * Returns the dynamic select columns. Dimensions come first and query items second. Program
   * indicator expressions are converted to SQL expressions. In the case of non-default boundaries
   * {@link EventQueryParams#hasNonDefaultBoundaries}, the period is hard-coded into the select
   * statement with "(isoPeriod) as (periodType)".
   */
  protected List<String> getSelectColumns(EventQueryParams params, boolean isAggregated) {
    return getSelectColumns(params, false, isAggregated);
  }

  /**
   * Returns the dynamic select columns. Dimensions come first and query items second.
   *
   * @param params the {@link EventQueryParams}.
   * @param isGroupByClause used to avoid grouping by period when using non-default boundaries where
   *     the column content would be fixed. Used by the group by calls.
   */
  private List<String> getSelectColumns(
      EventQueryParams params, boolean isGroupByClause, boolean isAggregated) {
    List<String> columns = new ArrayList<>();

    addDimensionSelectColumns(columns, params, isGroupByClause);
    addItemSelectColumns(columns, params, isGroupByClause, isAggregated);

    return columns;
  }

  /**
   * Adds the dynamic dimension select columns. Program indicator expressions are converted to SQL
   * expressions. In the case of non-default boundaries {@link
   * EventQueryParams#hasNonDefaultBoundaries}, the period is hard-coded into the select statement
   * with "(isoPeriod) as (periodType)".
   *
   * <p>If the first/last subquery is used then one query will be done for each period, and the
   * period will not be present in the query, so add it to the select columns and skip it in the
   * group by columns.
   */
  protected void addDimensionSelectColumns(
      List<String> columns, EventQueryParams params, boolean isGroupByClause) {
    params
        .getDimensions()
        .forEach(
            dimension -> {
              if (params.isAggregatedEnrollments()
                  && dimension.getDimensionType() == DimensionType.PERIOD) {
                for (DimensionalItemObject it : dimension.getItems()) {
                  columns.add(((Period) it).getPeriodType().getPeriodTypeEnum().getName());
                }
                return;
              }

              if (isGroupByClause
                  && dimension.getDimensionType() == DimensionType.PERIOD
                  && params.hasNonDefaultBoundaries()) {
                return;
              }

              if (dimension.getDimensionType() == DimensionType.PERIOD
                  && params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()) {
                if (!isGroupByClause) {
                  String alias = quote(dimension.getDimensionName());
                  columns.add(
                      "cast('"
                          + params.getLatestPeriod().getDimensionItem()
                          + "' as text) as "
                          + alias);
                }
              } else if (!params.hasNonDefaultBoundaries()
                  || dimension.getDimensionType() != DimensionType.PERIOD) {
                columns.add(getTableAndColumn(params, dimension, isGroupByClause));
              } else if (params.hasSinglePeriod()) {
                Period period = (Period) params.getPeriods().get(0);
                columns.add(
                    singleQuote(period.getIsoDate()) + " as " + period.getPeriodType().getName());
              } else if (!params.hasPeriods() && params.hasFilterPeriods()) {
                // Assuming same period type for all period filters, as the
                // query planner splits into one query per period type

                Period period = (Period) params.getFilterPeriods().get(0);
                columns.add(
                    singleQuote(period.getIsoDate()) + " as " + period.getPeriodType().getName());
              } else {
                throw new IllegalStateException(
                    """
                    Program indicator non-default boundary query must have \"
                    exactly one period, or no periods and a period filter""");
              }
            });
  }

  private void addItemSelectColumns(
      List<String> columns,
      EventQueryParams params,
      boolean isGroupByClause,
      boolean isAggregated) {
    for (QueryItem queryItem : params.getItems()) {
      ColumnAndAlias columnAndAlias =
          getColumnAndAlias(queryItem, params, isGroupByClause, isAggregated);

      columns.add(columnAndAlias.asSql());

      // asked for row context if allowed and needed based on column and its alias
      handleRowContext(columns, params, queryItem, columnAndAlias);
    }
  }

  protected void handleRowContext(
      List<String> columns,
      EventQueryParams params,
      QueryItem queryItem,
      ColumnAndAlias columnAndAlias) {
    if (rowContextAllowedAndNeeded(params, queryItem)
        && columnAndAlias != null
        && !isEmpty(columnAndAlias.alias)) {
      String columnForExists = " exists (" + columnAndAlias.column + ")";
      String aliasForExists = columnAndAlias.alias + ".exists";
      columns.add((new ColumnAndAlias(columnForExists, aliasForExists)).asSql());
      String columnForStatus =
          replaceStringBetween(columnAndAlias.column, "select", "from", " eventstatus ");
      String aliasForStatus = columnAndAlias.alias + ".status";
      columns.add((new ColumnAndAlias(columnForStatus, aliasForStatus)).asSql());
    }
  }

  /**
   * Eligibility of enrollment request for grid row context
   *
   * @param params
   * @param queryItem
   * @return true when eligible for row context
   */
  protected boolean rowContextAllowedAndNeeded(EventQueryParams params, QueryItem queryItem) {
    return params.getEndpointItem() == ENROLLMENT
        && params.isRowContext()
        && queryItem.hasProgramStage()
        && queryItem.getProgramStage().getRepeatable()
        && queryItem.hasRepeatableStageParams();
  }

  protected ColumnAndAlias getColumnAndAlias(
      QueryItem queryItem, EventQueryParams params, boolean isGroupByClause, boolean isAggregated) {
    if (queryItem.isProgramIndicator()) {
      ProgramIndicator in = (ProgramIndicator) queryItem.getItem();

      String asClause = in.getUid();
      String programIndicatorSubquery;

      if (queryItem.hasRelationshipType()) {
        programIndicatorSubquery =
            programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator(
                in,
                queryItem.getRelationshipType(),
                getAnalyticsType(),
                params.getEarliestStartDate(),
                params.getLatestEndDate());
      } else {
        programIndicatorSubquery =
            programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator(
                in, getAnalyticsType(), params.getEarliestStartDate(), params.getLatestEndDate());
      }

      return ColumnAndAlias.ofColumnAndAlias(programIndicatorSubquery, asClause);
    } else if (ValueType.COORDINATE == queryItem.getValueType()) {
      return getCoordinateColumn(queryItem);
    } else if (ValueType.ORGANISATION_UNIT == queryItem.getValueType()) {
      if (params.getCoordinateFields().stream()
          .anyMatch(f -> queryItem.getItem().getUid().equals(f))) {
        return getCoordinateColumn(queryItem, OU_GEOMETRY_COL_SUFFIX);
      } else {
        return getOrgUnitQueryItemColumnAndAlias(params, queryItem);
      }
    } else if (queryItem.getValueType() == ValueType.NUMBER && !isGroupByClause) {
      ColumnAndAlias columnAndAlias =
          getColumnAndAlias(queryItem, isAggregated, queryItem.getItemName());
      return ColumnAndAlias.ofColumnAndAlias(
          columnAndAlias.getColumn(),
          defaultIfNull(columnAndAlias.getAlias(), queryItem.getItemName()));
    } else if (queryItem.isText()
        && !isGroupByClause
        && hasOrderByClauseForQueryItem(queryItem, params)) {
      return getColumnAndAliasWithNullIfFunction(queryItem);
    } else {
      return getColumnAndAlias(queryItem, isGroupByClause, "");
    }
  }

  /**
   * The method create a ColumnAndAlias object for query item with repeatable stage and organization
   * unit value type, will return f.e. select 'w75KJ2mc4zz' from..., '') as 'w75KJ2mc4zz'
   *
   * @param params the {@link EventQueryParams}.
   * @param queryItem the {@link QueryItem}.
   * @return the {@link ColumnAndAlias}
   */
  private ColumnAndAlias getOrgUnitQueryItemColumnAndAlias(
      EventQueryParams params, QueryItem queryItem) {
    return rowContextAllowedAndNeeded(params, queryItem)
        ? ColumnAndAlias.ofColumnAndAlias(
            getColumn(queryItem, OU_NAME_COL_SUFFIX),
            getAlias(queryItem).orElse(queryItem.getItemName()))
        : ColumnAndAlias.ofColumn(getColumn(queryItem, OU_NAME_COL_SUFFIX));
  }

  /**
   * The method create a ColumnAndAlias object with nullif sql function. toSql function of class
   * will return f.e. nullif(select 'w75KJ2mc4zz' from..., '') as 'w75KJ2mc4zz'
   *
   * @param queryItem the {@link QueryItem}.
   * @return the {@link ColumnAndAlias} {@link ColumnWithNullIfAndAlias}
   */
  private ColumnAndAlias getColumnAndAliasWithNullIfFunction(QueryItem queryItem) {
    String column = getColumn(queryItem);

    if (queryItem.hasProgramStage() && queryItem.getItem().getDimensionItemType() == DATA_ELEMENT) {
      return ColumnWithNullIfAndAlias.ofColumnWithNullIfAndAlias(
          column, queryItem.getProgramStage().getUid() + "." + queryItem.getItem().getUid());
    }

    return ColumnWithNullIfAndAlias.ofColumnWithNullIfAndAlias(
        column, queryItem.getItem().getUid());
  }

  private boolean hasOrderByClauseForQueryItem(QueryItem queryItem, EventQueryParams params) {
    List<QueryItem> orderByColumns = getDistinctOrderByColumns(params);

    return orderByColumns.contains(queryItem);
  }

  private ColumnAndAlias getColumnAndAlias(
      QueryItem queryItem, boolean isGroupByClause, String aliasIfMissing) {
    String column = getColumn(queryItem);

    if (!isGroupByClause) {
      return ColumnAndAlias.ofColumnAndAlias(column, getAlias(queryItem).orElse(aliasIfMissing));
    }

    return ColumnAndAlias.ofColumn(column);
  }

  protected Optional<String> getAlias(QueryItem queryItem) {
    return Optional.of(queryItem)
        .filter(QueryItem::hasProgramStage)
        .filter(QueryItem::hasRepeatableStageParams)
        .map(QueryItem::getRepeatableStageParams)
        .map(RepeatableStageParams::getDimension);
  }

  @Transactional(readOnly = true, propagation = REQUIRES_NEW)
  public Grid getAggregatedEventData(EventQueryParams passedParams, Grid grid, int maxLimit) {
    EventQueryParams params = piDisagInfoInitializer.getParamsWithDisaggregationInfo(passedParams);
    String aggregateClause = getAggregateClause(params);
    List<String> columns =
        union(getSelectColumns(params, true), piDisagQueryGenerator.getCocSelectColumns(params));

    String sql =
        TextUtils.removeLastComma(
            "select " + aggregateClause + " as value," + StringUtils.join(columns, ",") + " ");

    // ---------------------------------------------------------------------
    // Criteria
    // ---------------------------------------------------------------------

    sql += getFromClause(params);

    sql += getWhereClause(params);

    sql += getGroupByClause(params);

    // ---------------------------------------------------------------------
    // Sort order
    // ---------------------------------------------------------------------

    if (params.hasSortOrder()) {
      sql += "order by value " + params.getSortOrder().toString().toLowerCase() + " ";
    }

    // ---------------------------------------------------------------------
    // Filtering criteria
    // ---------------------------------------------------------------------
    if (params.hasMeasureCriteria()) {
      sql += getMeasureCriteriaSql(params, aggregateClause);
    }

    // ---------------------------------------------------------------------
    // Limit, add one to max to enable later check against max limit
    // ---------------------------------------------------------------------

    if (params.hasLimit()) {
      sql += LIMIT + " " + params.getLimit();
    } else if (maxLimit > 0) {
      sql += LIMIT + " " + (maxLimit + 1);
    }

    // ---------------------------------------------------------------------
    // Grid
    // ---------------------------------------------------------------------

    final String finalSqlValue = sql;
    if (params.analyzeOnly()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(params.getExplainOrderId(), finalSqlValue));
    } else {
      withExceptionHandling(
          () -> getAggregatedEventData(grid, params, finalSqlValue), params.isMultipleQueries());
    }

    return grid;
  }

  /**
   * Returns a group by SQL clause.
   *
   * @param params the {@link EventQueryParams}.
   * @return a group by SQL clause.
   */
  private String getGroupByClause(EventQueryParams params) {
    String sql = "";

    if (params.isAggregation()) {
      List<String> selectColumnNames =
          union(
              getGroupByColumnNames(params, true),
              piDisagQueryGenerator.getCocColumnsForGroupBy(params));

      if (isNotEmpty(selectColumnNames)) {
        sql += "group by " + getCommaDelimitedString(selectColumnNames) + " ";
      }
    }

    return sql;
  }

  private void getAggregatedEventData(Grid grid, EventQueryParams params, String sql) {
    log.debug("Event analytics aggregate SQL: '{}'", sql);

    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

    while (rowSet.next()) {
      List<Object> row = new ArrayList<>();

      if (params.isAggregateData()) {
        if (params.hasValueDimension()) {
          row.add(getItemId(params));
        } else if (params.hasProgramIndicatorDimension()) {
          row.add(params.getProgramIndicator().getUid());
        }
      } else {
        for (QueryItem queryItem : params.getItems()) {
          ColumnAndAlias columnAndAlias = getColumnAndAlias(queryItem, params, false, true);
          String alias = columnAndAlias.getAlias();

          if (isEmpty(alias)) {
            alias = queryItem.getItemName();
          }

          String itemName = rowSet.getString(alias);
          String itemValue =
              params.isCollapseDataDimensions()
                  ? QueryItemHelper.getCollapsedDataItemValue(queryItem, itemName)
                  : itemName;

          if (params.getOutputIdScheme() == null || params.getOutputIdScheme() == IdScheme.NAME) {
            row.add(itemValue);
          } else {
            String value = null;

            String itemOptionValue = QueryItemHelper.getItemOptionValue(itemValue, params);

            if (itemOptionValue != null && !itemOptionValue.trim().isEmpty()) {
              value = itemOptionValue;
            } else {
              String legendItemValue = QueryItemHelper.getItemLegendValue(itemValue, params);

              if (legendItemValue != null && !legendItemValue.trim().isEmpty()) {
                value = legendItemValue;
              }
            }

            row.add(value == null ? itemValue : value);
          }
        }
      }

      for (DimensionalObject dimension : params.getDimensions()) {
        String dimensionValue = rowSet.getString(dimension.getDimensionName());
        row.add(dimensionValue);
      }

      if (params.hasValueDimension()) {
        if (params.hasTextValueDimension()) {
          String value = rowSet.getString(COL_VALUE);
          row.add(value);
        } else // Numeric
        {
          double value = rowSet.getDouble(COL_VALUE);
          row.add(params.isSkipRounding() ? value : getRounded(value));
        }
      } else if (params.hasProgramIndicatorDimension()) {
        double value = rowSet.getDouble(COL_VALUE);
        ProgramIndicator indicator = params.getProgramIndicator();
        row.add(getRoundedValue(params, indicator.getDecimals(), value));
      } else {
        int value = rowSet.getInt(COL_VALUE);
        row.add(value);
      }

      if (params.isIncludeNumDen()) {
        for (int i = 0; i < NUMERATOR_DENOMINATOR_PROPERTIES_COUNT; i++) {
          row.add(null);
        }
      }

      if (PiDisagDataHandler.addCocAndAoc(params, grid, row, rowSet)) {
        grid.addRow();
        grid.addValuesAsList(row);
      }
    }
  }

  /**
   * Builds the item identifier, so it can be identifiable in the response/row object.
   *
   * @param params the current {@link EventQueryParams}.
   * @return the item identifier.
   */
  private String getItemId(@Nonnull EventQueryParams params) {
    String programUid = params.getProgram().getUid();
    String dimensionUid = params.getValue().getUid();
    String optionUid = params.getOption() == null ? EMPTY : params.getOption().getUid();
    String itemId = programUid + COMPOSITE_DIM_OBJECT_PLAIN_SEP + dimensionUid;

    if (isNotBlank(optionUid)) {
      itemId += COMPOSITE_DIM_OBJECT_PLAIN_SEP + optionUid;
    }

    return itemId;
  }

  /**
   * Returns the aggregate clause based on value dimension and output type.
   *
   * @param params the {@link EventQueryParams}.
   */
  protected String getAggregateClause(EventQueryParams params) {
    // TODO include output type if aggregation type is count

    // If no aggregation type is set for this event data item and no override aggregation type is
    // set
    // no need to continue and skip aggregation all together by returning NULL
    if (hasNoAggregationType(params)) {
      return "null";
    }

    EventOutputType outputType = params.getOutputType();

    AggregationType aggregationType = params.getAggregationTypeFallback().getAggregationType();

    String function =
        (aggregationType == NONE || aggregationType == CUSTOM) ? "" : aggregationType.getValue();

    if (!params.isAggregation()) {
      return quoteAlias(params.getValue().getUid());
    } else if (params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()
        && params.hasEventProgramIndicatorDimension()) {
      return function + "(value)";
    } else if (params.hasNumericValueDimension() || params.hasBooleanValueDimension()) {
      String expression = quoteAlias(params.getValue().getUid());

      return function + "(" + expression + ")";
    } else if (params.hasProgramIndicatorDimension()) {
      String expression =
          programIndicatorService.getAnalyticsSql(
              params.getProgramIndicator().getExpression(),
              NUMERIC,
              params.getProgramIndicator(),
              params.getEarliestStartDate(),
              params.getLatestEndDate());

      return function + "(" + expression + ")";
    } else {
      if (params.hasEnrollmentProgramIndicatorDimension()) {
        if (EventOutputType.TRACKED_ENTITY_INSTANCE.equals(outputType)
            && params.isProgramRegistration()) {
          return "count(distinct trackedentity)";
        } else // ENROLLMENT
        {
          return "count(enrollment)";
        }
      } else {
        if (EventOutputType.TRACKED_ENTITY_INSTANCE.equals(outputType)
            && params.isProgramRegistration()) {
          return "count(distinct " + quoteAlias("trackedentity") + ")";
        } else if (EventOutputType.ENROLLMENT.equals(outputType)) {
          if (params.hasEnrollmentProgramIndicatorDimension()) {
            return "count(" + quoteAlias("enrollment") + ")";
          }
          return "count(distinct " + quoteAlias("enrollment") + ")";
        } else // EVENT
        {
          return "count(" + quoteAlias("event") + ")";
        }
      }
    }
  }

  /**
   * Creates a coordinate base column "selector" for the given item name. The item is expected to be
   * of type Coordinate.
   *
   * @param item the {@link QueryItem}.
   * @return the column select statement for the given item.
   */
  protected ColumnAndAlias getCoordinateColumn(QueryItem item) {
    String colName = item.getItemName();

    return ColumnAndAlias.ofColumnAndAlias(
        "'[' || round(ST_X("
            + quote(colName)
            + ")::numeric, 6) || ',' || round(ST_Y("
            + quote(colName)
            + ")::numeric, 6) || ']'",
        getAlias(item).orElse(colName));
  }

  /**
   * Creates a coordinate base column "selector" for the given item name. The item is expected to be
   * of type Coordinate.
   *
   * @param item the {@link QueryItem}.
   * @param suffix the suffix to append to the item id.
   * @return the column select statement for the given item.
   */
  protected ColumnAndAlias getCoordinateColumn(QueryItem item, String suffix) {
    String colName = item.getItemId() + suffix;

    String stCentroidFunction = "";

    if (ValueType.ORGANISATION_UNIT == item.getValueType()) {
      stCentroidFunction = "ST_Centroid";
    }

    return ColumnAndAlias.ofColumnAndAlias(
        "'[' || round(ST_X("
            + stCentroidFunction
            + "("
            + quote(colName)
            + "))::numeric, 6) || ',' || round(ST_Y("
            + stCentroidFunction
            + "("
            + quote(colName)
            + "))::numeric, 6) || ']'",
        colName);
  }

  /**
   * Creates a column selector for the given item name. The suffix will be appended as part of the
   * item name.
   *
   * @param item the {@link QueryItem}.
   * @param suffix the suffix.
   * @return the column select statement for the given item.
   */
  protected String getColumn(QueryItem item, String suffix) {
    return quote(item.getItemName() + suffix);
  }

  /**
   * Returns an encoded column name.
   *
   * @param item the {@link QueryItem}.
   */
  protected String getColumn(QueryItem item) {
    return quoteAlias(item.getItemName());
  }

  /**
   * Returns a SQL statement to select the expression or column of the item. If the item is a
   * program indicator, the program indicator expression is returned; if the item is a data element,
   * the item column name is returned.
   *
   * @param filter the {@link QueryFilter}.
   * @param item the {@link QueryItem}.
   * @param startDate the start date.
   * @param endDate the end date.
   */
  protected String getSelectSql(QueryFilter filter, QueryItem item, Date startDate, Date endDate) {
    if (item.isProgramIndicator()) {
      ProgramIndicator programIndicator = (ProgramIndicator) item.getItem();

      return programIndicatorService.getAnalyticsSql(
          programIndicator.getExpression(), NUMERIC, programIndicator, startDate, endDate);
    } else {
      return filter.getSqlFilterColumn(getColumn(item), item.getValueType());
    }
  }

  protected String getSelectSql(QueryFilter filter, QueryItem item, EventQueryParams params) {
    if (item.isProgramIndicator()) {
      return getColumnAndAlias(item, params, false, false).getColumn();
    } else {
      return filter.getSqlFilterColumn(getColumn(item), item.getValueType());
    }
  }

  /**
   * Returns a filter string.
   *
   * @param filter the filter string.
   * @param item the {@link QueryItem}.
   * @return a filter string.
   */
  private String getFilter(String filter, QueryItem item) {
    try {
      if (!NV.equals(filter) && item.getValueType() == ValueType.DATETIME) {
        return DateFormatUtils.format(
            DateUtils.parseDate(
                filter,
                // known formats
                "yyyy-MM-dd'T'HH.mm",
                "yyyy-MM-dd'T'HH.mm.ss"),
            // postgres format
            "yyyy-MM-dd HH:mm:ss");
      }
    } catch (ParseException pe) {
      throwIllegalQueryEx(ErrorCode.E7135, filter);
    }

    return filter;
  }

  /**
   * Returns the queryFilter value for the given query item.
   *
   * @param queryFilter the {@link QueryFilter}.
   * @param item the {@link QueryItem}.
   */
  protected String getSqlFilter(QueryFilter queryFilter, QueryItem item) {
    String filter = getFilter(queryFilter.getFilter(), item);

    return item.getSqlFilter(queryFilter, sqlBuilder.escape(filter), true);
  }

  /**
   * Returns the analytics table alias and column.
   *
   * @param params the {@link EventQueryParams}.
   * @param dimension the {@link DimensionalObject}.
   * @param isGroupByClause don't add a column alias if present.
   */
  private String getTableAndColumn(
      EventQueryParams params, DimensionalObject dimension, boolean isGroupByClause) {
    String col = dimension.getDimensionName();

    if (params.hasTimeField() && DimensionType.PERIOD == dimension.getDimensionType()) {
      return sqlBuilder.quote(DATE_PERIOD_STRUCT_ALIAS, col);
    } else if (DimensionType.ORGANISATION_UNIT == dimension.getDimensionType()) {
      return params
          .getOrgUnitField()
          .withSqlBuilder(sqlBuilder)
          .getOrgUnitStructCol(col, getAnalyticsType(), isGroupByClause);
    } else if (DimensionType.ORGANISATION_UNIT_GROUP_SET == dimension.getDimensionType()) {
      return params
          .getOrgUnitField()
          .withSqlBuilder(sqlBuilder)
          .getOrgUnitGroupSetCol(col, getAnalyticsType(), isGroupByClause);
    } else if (params.isPiDisagDimension(col)) {
      return piDisagQueryGenerator.getColumnForSelectOrGroupBy(params, col, isGroupByClause);
    } else {
      return quoteAlias(col);
    }
  }

  /**
   * Template method that generates a SQL query for retrieving events or enrollments.
   *
   * @param params the {@link EventQueryParams} to drive the query generation.
   * @param maxLimit max number of records to return.
   * @return a SQL query.
   */
  protected String getAggregatedEnrollmentsSql(EventQueryParams params, int maxLimit) {
    String sql = getSelectClause(params);

    sql += getFromClause(params);

    sql += getWhereClause(params);

    sql += getSortClause(params);

    sql += getPagingClause(params, maxLimit);

    return sql;
  }

  /**
   * Template method that generates a SQL query for retrieving aggregated enrollments.
   *
   * @param params the {@link List<GridHeader>} to drive the query generation.
   * @param params the {@link EventQueryParams} to drive the query generation.
   * @return a SQL query.
   */
  protected String getAggregatedEnrollmentsSql(List<GridHeader> headers, EventQueryParams params) {
    String sql = getSelectClause(params);

    sql += getFromClause(params);

    String whereClause = getWhereClause(params);
    String filterWhereClause = getQueryItemsAndFiltersWhereClause(params, new SqlHelper());
    sql += SqlConditionJoiner.joinSqlConditions(whereClause, filterWhereClause);

    String headerColumns = getHeaderColumns(headers, sql).stream().collect(joining(","));
    String orgColumns = getOrgUnitLevelColumns(params).stream().collect(joining(","));
    String periodColumns = getPeriodColumns(params).stream().collect(joining(","));

    String columns =
        (!isBlank(orgColumns) ? orgColumns : "," + ORGUNIT_DIM_ID)
            + (!isBlank(periodColumns) ? "," + periodColumns : EMPTY)
            + (!isBlank(headerColumns) ? "," + headerColumns : EMPTY);

    sql =
        "select count("
            + OUTER_SQL_ALIAS
            + ".enrollment) as "
            + COL_VALUE
            + ", "
            + columns
            + " from ("
            + sql
            + ") "
            + OUTER_SQL_ALIAS
            + " group by "
            + columns;

    return sql;
  }

  /**
   * Adds a value from the given row set to the grid.
   *
   * @param grid the {@link Grid}.
   * @param header the {@link GridHeader}.
   * @param index the row set index.
   * @param sqlRowSet the {@link SqlRowSet}.
   * @param params the {@link EventQueryParams}.
   */
  protected void addGridValue(
      Grid grid, GridHeader header, int index, SqlRowSet sqlRowSet, EventQueryParams params) {
    if (header.isDoubleWithoutLegendSet()) {
      Object value = sqlRowSet.getObject(index);

      boolean isNumber = value instanceof Number;

      if (value == null) {
        grid.addValue(EMPTY);
      } else if (isNumber) {
        addNumberValue(grid, header, params, (Number) value);
      } else {
        grid.addValue(trimToNull(sqlRowSet.getString(index)));
      }
    } else if (header.hasValueType(REFERENCE)) {
      addReferenceValue(grid, sqlRowSet.getString(index));
    } else if (headerHasValueType(header, ValueType.DATETIME, ValueType.DATE)) {
      grid.addValue(sqlBuilder.renderTimestamp(sqlRowSet.getString(index)));
    } else {
      // If the object is not a Number (e.g., a string from the DB),
      // treat it as a default string value.
      grid.addValue(trimToNull(sqlRowSet.getString(index)));
    }
  }

  /**
   * Adds the given number to the given {@link Grid}, respecting internal rules based on the
   * arguments.
   *
   * @param grid the current {@link Grid}.
   * @param header the current {@link GridHeader}.
   * @param params the current {@link EventQueryParams}.
   * @param number the number to be added.
   */
  private void addNumberValue(
      Grid grid, GridHeader header, EventQueryParams params, Number number) {
    Double doubleValue = number.doubleValue();

    // NaN (Not-a-Number) is also treated as an empty/invalid value in this context.
    if (!Double.isNaN(doubleValue)) {
      addGridDoubleTypeValue(doubleValue, grid, header, params);
    } else {
      grid.addValue(EMPTY);
    }
  }

  /**
   * Extracts the "uid" and "reference" from the given JSON string and adds them to the current
   * {@link Grid}. If something goes wrong, it adds the full JSON string to the grid.
   *
   * @param grid the current {@link Grid}.
   * @param json the JSON string.
   */
  private void addReferenceValue(Grid grid, String json) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      JsonNode jsonNode = mapper.readTree(json);
      String uid = UUID.randomUUID().toString();
      Reference referenceNode = new Reference(uid, jsonNode);

      grid.addValue(uid);
      grid.addReference(referenceNode);
    } catch (Exception e) {
      grid.addValue(json);
    }
  }

  /**
   * Double value type will be added into the grid. There is special handling for Option Set (Type
   * numeric)/Option. The code in grid/meta info and related value in row has to be the same (FE
   * request) if possible. The string interpretation of code coming from Option/Code can vary from
   * Option/value (double) fetched from database ("1" vs "1.0") By the equality (both are converted
   * to double) of both the Option/Code is used as a value.
   *
   * @param number the value.
   * @param grid the {@link Grid}.
   * @param header the {@link GridHeader}.
   * @param params the {@link EventQueryParams}.
   */
  private void addGridDoubleTypeValue(
      Double number, Grid grid, GridHeader header, EventQueryParams params) {
    Optional<QueryItem> programIndicatorItem =
        params.getItems().stream()
            .filter(
                item -> item.isProgramIndicator() && header.getName().equals(item.getItemName()))
            .findFirst();

    if (header.hasOptionSet()) {
      Optional<Option> option =
          header.getOptionSetObject().getOptions().stream()
              .filter(
                  o ->
                      isCreatable(o.getCode())
                          && MathUtils.isEqual(createDouble(o.getCode()), number))
              .findFirst();

      if (option.isPresent()) {
        grid.addValue(option.get().getCode());
      } else {
        grid.addValue(round(number, params.isSkipRounding()));
      }
    } else if (programIndicatorItem.isPresent()) {
      ProgramIndicator programIndicator = (ProgramIndicator) programIndicatorItem.get().getItem();

      grid.addValue(round(number, params, programIndicator.getDecimals()));
    } else {
      grid.addValue(round(number, params.isSkipRounding()));
    }
  }

  /**
   * Based on the given number and arguments, this method will round respecting the number of
   * decimals, or an internal pre-defined scale. It strips the trailing zeros from the final number.
   *
   * @param number the value to be rounded.
   * @param params the current {@link EventQueryParams} object.
   * @param decimals the number of decimals digits output.
   * @return the rounded number, without trailing zeros.
   */
  private String round(Double number, EventQueryParams params, Integer decimals) {
    double roundedNumber = getRoundedValue(params, decimals, number).doubleValue();
    String noTrailingZerosValue =
        BigDecimal.valueOf(roundedNumber).stripTrailingZeros().toPlainString();

    return noTrailingZerosValue;
  }

  /**
   * Based on the given number and boolean flag, this method will round the number to default
   * internal scale, or a pre-defined "larger" scale. It strips the trailing zeros from the final
   * number.
   *
   * @param number the value to be rounded.
   * @param skipDefaultRounding skip the default rounding, if true.
   * @return the rounded number, without trailing zeros.
   */
  private String round(Double number, boolean skipDefaultRounding) {
    final int largerScale = 10;
    Double roundedValue =
        skipDefaultRounding ? getRoundedObject(number, largerScale) : getRoundedObject(number);
    String noTrailingZerosValue =
        BigDecimal.valueOf(roundedValue).stripTrailingZeros().toPlainString();

    return noTrailingZerosValue;
  }

  protected String getQueryItemsAndFiltersWhereClause(EventQueryParams params, SqlHelper helper) {
    return getQueryItemsAndFiltersWhereClause(params, Set.of(), helper);
  }

  /**
   * Returns a SQL where clause string for query items and query item filters.
   *
   * @param params the {@link EventQueryParams}.
   * @param helper the {@link SqlHelper}.
   */
  protected String getQueryItemsAndFiltersWhereClause(
      EventQueryParams params, Set<QueryItem> exclude, SqlHelper helper) {
    if (params.isEnhancedCondition()) {
      return getItemsSqlForEnhancedConditions(params, helper);
    }

    // Creates a map grouping query items referring to repeatable stages and
    // those referring to non-repeatable stages. This is for enrollment
    // only, event query items are treated as non-repeatable.
    Map<Boolean, List<QueryItem>> itemsByRepeatableFlag =
        Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
            .filter(QueryItem::hasFilter)
            .filter(queryItem -> !exclude.contains(queryItem))
            .collect(
                groupingBy(
                    queryItem ->
                        queryItem.hasRepeatableStageParams()
                            && params.getEndpointItem() == ENROLLMENT));

    // Groups repeatable conditions based on PSI.DEID
    Map<String, List<String>> repeatableConditionsByIdentifier =
        asSqlCollection(itemsByRepeatableFlag.get(true), params)
            .collect(
                groupingBy(
                    IdentifiableSql::getIdentifier, mapping(IdentifiableSql::getSql, toList())));

    // Joins each group with OR
    List<String> orConditions =
        repeatableConditionsByIdentifier.values().stream()
            .map(sameGroup -> joinSql(sameGroup, OR_JOINER))
            .toList();

    // Non-repeatable conditions
    List<String> andConditions =
        asSqlCollection(itemsByRepeatableFlag.get(false), params)
            .map(IdentifiableSql::getSql)
            .toList();

    if (orConditions.isEmpty() && andConditions.isEmpty()) {
      return StringUtils.EMPTY;
    }

    return helper.whereAnd()
        + " "
        + joinSql(Stream.concat(orConditions.stream(), andConditions.stream()), AND_JOINER);
  }

  /**
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double quoted relation.
   */
  protected String quote(String relation) {
    return sqlBuilder.quote(relation);
  }

  /**
   * @param relation the relation to quote.
   * @return an "ax" aliased and double quoted relation.
   */
  protected String quoteAlias(String relation) {
    return sqlBuilder.quoteAx(relation);
  }

  /**
   * @param value the value to quote.
   * @return a single quoted value.
   */
  protected String singleQuote(String value) {
    return sqlBuilder.singleQuote(value);
  }

  /**
   * Returns a concatenated string of the given items separated by comma where each item is quoted
   * and aliased.
   *
   * @param items the collection of items.
   * @return a string.
   */
  protected String quoteAliasCommaDelimited(Collection<String> items) {
    return items.stream().map(this::quoteAlias).collect(Collectors.joining(","));
  }

  /**
   * Joins a stream of conditions using given join function. Returns empty string if collection is
   * empty.
   */
  private String joinSql(Stream<String> conditions, Collector<CharSequence, ?, String> joiner) {
    return joinSql(conditions.collect(toList()), joiner);
  }

  protected String getItemsSqlForEnhancedConditions(EventQueryParams params, SqlHelper hlp) {
    Map<UUID, String> sqlConditionByGroup =
        Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
            .filter(QueryItem::hasFilter)
            .collect(
                groupingBy(
                    QueryItem::getGroupUUID,
                    mapping(queryItem -> toSql(queryItem, params), OR_JOINER)));

    if (sqlConditionByGroup.values().isEmpty()) {
      return "";
    }

    return hlp.whereAnd() + " " + String.join(AND, sqlConditionByGroup.values());
  }

  /**
   * Joins a collection of conditions using given join function, returns empty string if collection
   * is empty
   */
  private String joinSql(Collection<String> conditions, Collector<CharSequence, ?, String> joiner) {
    if (!conditions.isEmpty()) {
      return conditions.stream().collect(joiner);
    }

    return "";
  }

  /**
   * Returns a collection of IdentifiableSql, each representing SQL for given queryItems together
   * with its identifier.
   */
  private Stream<IdentifiableSql> asSqlCollection(
      List<QueryItem> queryItems, EventQueryParams params) {
    return emptyIfNull(queryItems).stream().map(queryItem -> toIdentifiableSql(queryItem, params));
  }

  /** Converts given queryItem into {@link IdentifiableSql} joining its filters using AND. */
  private IdentifiableSql toIdentifiableSql(QueryItem queryItem, EventQueryParams params) {
    return IdentifiableSql.builder()
        .identifier(getIdentifier(queryItem))
        .sql(toSql(queryItem, params))
        .build();
  }

  /** Converts given queryItem into SQL joining its filters using AND. */
  private String toSql(QueryItem queryItem, EventQueryParams params) {
    return queryItem.getFilters().stream()
        .map(filter -> toSql(queryItem, filter, params))
        .collect(joining(AND));
  }

  /** Returns PSID.ITEM_ID of given queryItem. */
  private String getIdentifier(QueryItem queryItem) {
    String programStageId =
        Optional.of(queryItem)
            .map(QueryItem::getProgramStage)
            .map(IdentifiableObject::getUid)
            .orElse("");
    return programStageId + "." + queryItem.getItem().getUid();
  }

  @Getter
  @Builder
  public static class IdentifiableSql {
    private final String identifier;

    private final String sql;
  }

  /**
   * Creates a SQL statement for a single filter inside a query item. Made public for testing
   * purposes.
   *
   * @param item the {@link QueryItem}.
   * @param filter the {@link QueryFilter}.
   * @param params the {@link EventQueryParams}.
   */
  public String toSql(QueryItem item, QueryFilter filter, EventQueryParams params) {
    String field =
        item.hasAggregationType()
            ? getSelectSql(filter, item, params)
            : getSelectSql(filter, item, params.getEarliestStartDate(), params.getLatestEndDate());

    String filterString =
        item.getValueType() == ValueType.ORGANISATION_UNIT
            ? organisationUnitResolver.resolveOrgUnits(filter, params.getUserOrgUnits())
            : filter.getFilter();

    if (IN.equals(filter.getOperator())) {
      InQueryFilter inQueryFilter =
          new InQueryFilter(field, sqlBuilder.escape(filterString), !item.isNumeric());

      return inQueryFilter.getSqlFilter();
    } else {
      // NV filter has its own specific logic, so skip values
      // comparisons when NV is set as filter
      if (!NV.equals(filter.getFilter())) {
        // Specific handling for null and empty values
        switch (filter.getOperator()) {
          case NEQ:
          case NE:
          case NIEQ:
          case NLIKE:
          case NILIKE:
            return nullAndEmptyMatcher(item, filter, field);
          default:
            break;
        }
      }

      return field
          + SPACE
          + filter.getSqlOperator(true)
          + SPACE
          + getSqlFilter(filter, item)
          + SPACE;
    }
  }

  /**
   * Ensures that null/empty values will always match.
   *
   * @param item the {@link QueryItem}.
   * @param filter the {@link QueryFilter}.
   * @param field the field.
   * @return the respective SQL statement matcher.
   */
  private String nullAndEmptyMatcher(QueryItem item, QueryFilter filter, String field) {
    if (item.getValueType() != null && item.getValueType().isText()) {
      return "(coalesce("
          + field
          + ", '') = '' or "
          + field
          + SPACE
          + filter.getSqlOperator(true)
          + SPACE
          + getSqlFilter(filter, item)
          + ") ";
    } else {
      return "("
          + field
          + " is null or "
          + field
          + SPACE
          + filter.getSqlOperator(true)
          + SPACE
          + getSqlFilter(filter, item)
          + ") ";
    }
  }

  /**
   * Method responsible for merging query items based on sorting parameters
   *
   * @param params the {@link EventQueryParams} to drive the query item list generation.
   * @return the distinct {@link List<QueryItem>} relevant for order by DML.
   */
  private List<QueryItem> getDistinctOrderByColumns(EventQueryParams params) {
    List<QueryItem> orderByAscColumns = new ArrayList<>();

    List<QueryItem> orderByDescColumns = new ArrayList<>();

    if (params.getAsc() != null && !params.getAsc().isEmpty()) {
      orderByAscColumns.addAll(params.getAsc());
    }

    if (params.getDesc() != null && !params.getDesc().isEmpty()) {
      orderByDescColumns.addAll(params.getDesc());
    }

    return ListUtils.distinctUnion(orderByAscColumns, orderByDescColumns);
  }

  /**
   * returns true if the amount of rows red is greater than the page size and the query is not
   * unlimited.
   *
   * @param params the {@link EventQueryParams}.
   * @param unlimitedPaging the analytics unlimited paging setting.
   * @param rowsRed the amount of rows red.
   * @return true if the amount of rows red is greater than the page size and the query is not
   *     unlimited.
   */
  protected boolean isLastRowAfterPageSize(
      EventQueryParams params, boolean unlimitedPaging, int rowsRed) {
    return rowsRed > params.getPageSizeWithDefault()
        && !params.isTotalPages()
        && !isUnlimitedQuery(params, unlimitedPaging);
  }

  /**
   * Returns true if the given query is unlimited. This is the case when the page size is not set
   * and unlimited paging is enabled.
   *
   * @param params the {@link EventQueryParams}.
   * @param unlimitedPaging the analytics unlimited paging setting.
   * @return true if the given query is unlimited.
   */
  protected boolean isUnlimitedQuery(EventQueryParams params, boolean unlimitedPaging) {
    return unlimitedPaging && (Objects.isNull(params.getPageSize()) || params.getPageSize() == 0);
  }

  /**
   * Returns a coalesce expression for coordinates fallback.
   *
   * @param fields Collection of coordinate fields.
   * @param defaultColumnName Default coordinate field
   * @return a coalesce expression for coordinates fallback.
   */
  protected String getCoalesce(List<String> fields, String defaultColumnName) {
    if (fields == null) {
      return defaultColumnName;
    }

    String args =
        fields.stream()
            .filter(StringUtils::isNotBlank)
            .map(sqlBuilder::quoteAx)
            .collect(Collectors.joining(","));

    String sql = String.format("coalesce(%s)", args);

    return args.isEmpty() ? defaultColumnName : sql;
  }

  protected List<String> getSelectColumnsWithCTE(EventQueryParams params, CteContext cteContext) {
    List<String> columns = new ArrayList<>();

    // Add dimension columns only when the analytics query
    // is for enrollments
    if (!cteContext.isEventsAnalytics()) {
      addDimensionSelectColumns(columns, params, false);
    }

    // Process query items with CTE references
    for (QueryItem queryItem : params.getItems()) {
      if (queryItem.isProgramIndicator()) {
        // For program indicators, use CTE reference
        String piUid = queryItem.getItem().getUid();
        CteDefinition cteDef = cteContext.getDefinitionByItemUid(piUid);
        if (cteDef == null) {
          continue;
        }
        String col =
            cteDef.isRequiresCoalesce()
                ? "coalesce(%s.value, 0) as %s".formatted(cteDef.getAlias(), piUid)
                : "%s.value as %s".formatted(cteDef.getAlias(), piUid);
        columns.add(col);
      } else if (ValueType.COORDINATE == queryItem.getValueType()) {
        // Handle coordinates
        columns.add(getCoordinateColumn(queryItem).asSql());
      } else if (ValueType.ORGANISATION_UNIT == queryItem.getValueType()) {
        // Handle org units
        if (params.getCoordinateFields().stream()
            .anyMatch(f -> queryItem.getItem().getUid().equals(f))) {
          columns.add(getCoordinateColumn(queryItem, OU_GEOMETRY_COL_SUFFIX).asSql());
        } else {
          columns.add(getOrgUnitQueryItemColumnAndAlias(params, queryItem).asSql());
        }
      } else if (!cteContext.isEventsAnalytics() && queryItem.hasProgramStage()) {
        // Handle program stage items with CTE (only when NOT in events analytics)
        columns.add(getColumnWithCte(queryItem, cteContext));
      } else {
        // Handle other types as before
        ColumnAndAlias columnAndAlias = getColumnAndAlias(queryItem, false, "");
        columns.add(columnAndAlias.asSql());
      }
    }

    // Remove duplicates
    return columns.stream().distinct().toList();
  }

  /**
   * Determines if the experimental analytics query engine should be used. The experimental
   * analytics query engine is used when the analytics database is set to Doris or when the setting
   * is enabled. When the experimental analytics query engine is used, all enrollment and event
   * queries are constructed using CTE (Common Table Expressions) instead of subqueries.
   *
   * @return true if the experimental analytics query engine should be used, false otherwise.
   */
  protected boolean useExperimentalAnalyticsQueryEngine() {
    String analyticsDatabase = config.getPropertyOrDefault(ANALYTICS_DATABASE, "").trim();
    return "doris".equalsIgnoreCase(analyticsDatabase)
        || "clickhouse".equalsIgnoreCase(analyticsDatabase)
        || this.settingsService.getCurrentSettings().getUseExperimentalAnalyticsQueryEngine();
  }

  /**
   * Returns the "having" clause for the aggregated query. The "having" clause is calculated based
   * on the measure criteria in the {@link EventQueryParams} and the existing aggregate clause. The
   * expression has to be first cast to a numeric type and then rounded to 10 decimal places,
   * otherwise the comparison may fail due to floating point precision issues in Postgres.
   *
   * @param params the {@link EventQueryParams}
   * @param aggregateClause the aggregate clause to use in the SQL
   * @return the "having" clause
   */
  protected String getMeasureCriteriaSql(EventQueryParams params, String aggregateClause) {
    SqlHelper sqlHelper = new SqlHelper();
    StringBuilder builder = new StringBuilder();

    for (MeasureFilter filter : params.getMeasureCriteria().keySet()) {
      Double criterion = params.getMeasureCriteria().get(filter);

      String sqlFilter =
          String.format(
              " round(%s, 10) %s %s ",
              sqlBuilder.cast(aggregateClause, org.hisp.dhis.analytics.DataType.NUMERIC),
              getOperatorByMeasureFilter(filter),
              criterion);

      builder.append(sqlHelper.havingAnd()).append(sqlFilter);
    }

    return builder.toString();
  }

  private String getOperatorByMeasureFilter(MeasureFilter filter) {
    QueryOperator qo = QueryOperator.fromString(filter.toString());
    if (qo != null) {
      return qo.getValue();
    }
    throw new IllegalQueryException(E7149, filter.toString());
  }

  /**
   * Transforms the query item filters into an "and" separated SQL string. For instance, if the
   * query item has filters with values "a" and "b" and the operator is "eq", the resulting SQL
   * string will be "column = 'a' and column = 'b'". If the query item has no filters, an empty
   * string is returned.
   *
   * @param item the {@link QueryItem}.
   * @param columnName the column name.
   * @return the SQL string.
   */
  protected String extractFiltersAsSql(QueryItem item, String columnName) {
    return item.getFilters().stream()
        .map(
            f ->
                "%s %s %s".formatted(columnName, f.getOperator().getValue(), getSqlFilter(f, item)))
        .collect(Collectors.joining(" and "));
  }

  /**
   * Returns a select SQL clause for the given query.
   *
   * @param params the {@link EventQueryParams}.
   */
  protected abstract String getSelectClause(EventQueryParams params);

  /** Returns the column name associated with the CTE */
  protected abstract String getColumnWithCte(QueryItem item, CteContext cteContext);

  protected abstract CteContext getCteDefinitions(EventQueryParams params);

  /**
   * Generates the SQL for the from-clause. Generally this means which analytics table to get data
   * from.
   *
   * @param params the {@link EventQueryParams} that define what is going to be queried.
   * @return SQL to add to the analytics query.
   */
  protected abstract String getFromClause(EventQueryParams params);

  /**
   * Generates the SQL for the where-clause. Generally this means adding filters, grouping and
   * ordering to the SQL.
   *
   * @param params the {@link EventQueryParams} that defines the details of the filters, grouping
   *     and ordering.
   * @return SQL to add to the analytics query.
   */
  protected abstract String getWhereClause(EventQueryParams params);

  /**
   * Returns the relevant {@link AnalyticsType}.
   *
   * @return the {@link AnalyticsType}.
   */
  protected abstract AnalyticsType getAnalyticsType();

  /**
   * Check if the aggregation type is NONE on both the param value's aggregation type and the
   * EventQueryParams aggregation type (in case of aggregation type override).
   *
   * @param params the {@link EventQueryParams}.
   * @return true if the aggregation type is NONE on both the param value's aggregation type
   */
  private boolean hasNoAggregationType(EventQueryParams params) {
    if (params.getValue() == null) {
      return false;
    }

    // Check if there's an explicit aggregation type override
    if (params.getAggregationType() != null) {
      // If the override is NOT NONE, return false
      return params.getAggregationType().getAggregationType() == AggregationType.NONE;
    }

    // No override exists, so check the value's aggregation type
    return params.getValue().getAggregationType() == AggregationType.NONE;
  }

  /**
   * Check if the header has a value type that matches any of the provided types.
   *
   * @param header the {@link GridHeader}.
   * @param types the {@link ValueType} to check against.
   * @return true if the header has a matching value type.
   */
  private boolean headerHasValueType(GridHeader header, ValueType... types) {
    return Stream.of(types).anyMatch(type -> type == header.getValueType());
  }

  /**
   * Builds the full SQL query for enrollment or events analytics, including CTEs, SELECT, FROM,
   * JOINs, WHERE, and ORDER BY clauses.
   *
   * @param params the {@link EventQueryParams} to drive the query generation.
   * @return a complete SQL query string.
   */
  String buildAnalyticsQuery(EventQueryParams params) {

    // 1. Create the CTE context (collect all CTE definitions for program indicators, program
    // stages, etc.)
    CteContext cteContext = getCteDefinitions(params);

    // 2. Generate any additional CTE filters that might be needed
    generateFilterCTEs(params, cteContext);

    // 3. Build up the final SQL using dedicated sub-steps
    SelectBuilder sb = new SelectBuilder();

    // 3.1: Append the WITH clause if needed
    addCteClause(sb, cteContext);

    // 3.2: Append the SELECT clause, including columns from the CTE context
    addSelectClause(sb, params, cteContext);
    // Retain the columns of the main SELECT statement as a hint for the shadow CTEs
    List<String> selectColumns = sb.getColumnNames();

    // 3.3: Append the "FROM" clause (the main enrollment analytics table)
    addFromClause(sb, params);

    // 3.4: Append LEFT JOINs for each relevant CTE definition
    addCteJoins(sb, cteContext);

    // 3.5: Collect and append WHERE conditions (including filters from CTE)
    addWhereClause(sb, params, cteContext);

    // 3.6: Append ORDER BY and paging
    addSortingAndPaging(sb, params);

    if (needsOptimizedCtes(params, cteContext)) {
      // 3.7 : Add shadow CTEs for optimized query
      addShadowCtes(params, cteContext, selectColumns);
    }

    return sb.build();
  }

  /**
   * Checks if the query needs optimized CTEs based on a number of conditions: - If there are no
   * program indicators in the query, it returns false.
   *
   * @param params the {@link EventQueryParams} to check.
   * @return true if optimized CTEs are needed, false otherwise.
   */
  private boolean needsOptimizedCtes(EventQueryParams params, CteContext cteContext) {
    if (params.getEndpointItem() != ENROLLMENT) {
      // If the endpoint is not ENROLLMENT, we don't need optimized CTEs
      // TODO for events analytics, we probably need to enable shadow ctes only
      // if there are program indicators of type enrollment
      return false;
    }
    if (!cteContext.hasCteDefinitions()) {
      // If there are CTE definitions, we need to use optimized CTEs
      return false;
    }
    // if no PI -> return false
    return params.getItems().stream().anyMatch(QueryItem::isProgramIndicator);
  }

  /**
   * Appends the SELECT clause, including both the standard enrollment columns (or aggregated
   * columns) and columns derived from the CTE definitions.
   */
  abstract void addSelectClause(SelectBuilder sb, EventQueryParams params, CteContext cteContext);

  abstract void addFromClause(SelectBuilder sb, EventQueryParams params);

  abstract List<String> getStandardColumns(EventQueryParams params);

  /**
   * Adds a sequence of "shadow" Common Table Expressions (CTEs) to the CteContext to optimize
   * enrollment analytics queries, particularly when pagination is used.
   *
   * <p>The core rationale is to apply pagination and base filters as early as possible,
   * significantly reducing the data processed by subsequent Program Indicator and event-related
   * CTEs. This avoids performance issues where multiple CTEs would otherwise scan entire underlying
   * tables before a final LIMIT clause is applied. This optimization works by:
   *
   * <ol>
   *   <li>Creating a {@code top_enrollments} CTE: This selects the target page of enrollments from
   *       the main analytics enrollment table (e.g., {@code analytics_enrollment_PROGRAMUID}) after
   *       applying necessary enrollment-level filters and pagination (LIMIT/OFFSET).
   *   <li>Creating a shadow {@code analytics_enrollment_PROGRAMUID} CTE: This CTE is named
   *       <em>identically</em> to the actual enrollment analytics table. It simply selects all
   *       columns from {@code top_enrollments}.
   *   <li>Creating a shadow {@code analytics_event_PROGRAMUID} CTE: This CTE is named
   *       <em>identically</em> to the actual event analytics table for the program. It joins the
   *       real event table with {@code top_enrollments} and applies relevant event-level filters,
   *       effectively pre-filtering events.
   * </ol>
   *
   * <p>By defining these shadow CTEs with the same names as the original tables, all subsequent
   * CTEs that reference these table names will transparently use these pre-filtered, optimized
   * versions without needing any modification themselves.
   *
   * @param params The {@link EventQueryParams} driving the query.
   * @param cteContext The {@link CteContext} to which these shadow CTEs will be added.
   * @param selectColumns The list of columns computed for the main SELECT statement. This is used
   *     when computing the main {@code top_enrollments} CTE to ensure that any missing column is
   *     used in the shadow CTEs.
   */
  void addShadowCtes(EventQueryParams params, CteContext cteContext, List<String> selectColumns) {

    addTopEnrollmentsCte(params, cteContext, selectColumns);
    addShadowEnrollmentTableCte(params, cteContext);
    addShadowEventTableCte(params, cteContext);
  }

  /**
   * Creates and adds the {@code top_enrollments} Common Table Expression (CTE) to the provided
   * {@link CteContext}.
   *
   * <p>This CTE is the main step in the shadow CTE optimization strategy. Its primary purpose is to
   * select a paginated and filtered subset of enrollments from the main analytics enrollment table
   * (e.g., {@code analytics_enrollment_PROGRAMUID}).
   *
   * <p>The {@code top_enrollments} CTE includes:
   *
   * <ul>
   *   <li>All standard enrollment columns (from {@code getStandardColumns()}), ensuring that any
   *       columns defined as SQL formulas (like {@code ST_AsGeoJSON(...)}) are given appropriate
   *       aliases for consistent referencing.
   *   <li>Relevant organization unit level columns based on the query parameters.
   *   <li>Any additional enrollment-level attributes or data elements that are identified as being
   *       required by downstream program indicator calculations or other CTEs that operate on
   *       enrollment data.
   *   <li>Enrollment-level filters derived from the {@link EventQueryParams}.
   *   <li>Pagination (LIMIT and OFFSET) and sorting clauses to retrieve only the specific page of
   *       enrollments requested.
   * </ul>
   *
   * <p>This early filtering and pagination massively reduce the number of enrollment records that
   * subsequent shadow CTEs (like the shadow event table) and program indicator CTEs need to
   * process.
   *
   * @param params The {@link EventQueryParams} containing filters, pagination, sorting, and
   *     dimension information.
   * @param cteContext The {@link CteContext} to which the generated {@code top_enrollments} CTE
   *     definition will be added. It's added with a specific type ({@code CteType.TOP_ENROLLMENTS})
   *     to ensure correct ordering in the final SQL query.
   */
  void addTopEnrollmentsCte(
      EventQueryParams params, CteContext cteContext, List<String> selectColumns) {
    SelectBuilder topEnrollments = new SelectBuilder();
    Map<String, String> formulaAliases = getFormulaColumnAliases();

    // Add all standard enrollment columns
    for (String column : getStandardColumns(params)) {
      if (columnIsInFormula(column)) {
        String alias = formulaAliases.getOrDefault(column, createDefaultAlias(column));
        topEnrollments.addColumn(column, null, alias); // null tablePrefix, explicit alias
      } else {
        topEnrollments.addColumn(column, "ax"); // existing logic for regular columns
      }
    }

    for (DimensionalItemObject object : params.getDimensionOrFilterItems(ORGUNIT_DIM_ID)) {
      OrganisationUnit unit = (OrganisationUnit) object;
      topEnrollments.addColumnIfNotExist(
          params
              .getOrgUnitField()
              .withSqlBuilder(sqlBuilder)
              .getOrgUnitLevelCol(unit.getLevel(), getAnalyticsType()));
    }
    // add the `ou` column
    topEnrollments.addColumn("ou", "ax");

    Set<String> enrollmentColumns = getEnrollmentColumnsFromProgramIndicators(params);
    for (String column : enrollmentColumns) {
      topEnrollments.addColumn(quote(column), "ax");
    }
    // Add ORGANISATION_UNIT_GROUP_SET columns
    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.ORGANISATION_UNIT_GROUP_SET));

    for (DimensionalObject dim : dynamicDimensions) {
      if (!dim.isAllItems()) {
        String col = quoteAlias(dim.getDimensionName());
        topEnrollments.addColumnIfNotExist(col);
      }
    }
    for (String selectColumn : selectColumns) {
      topEnrollments.addColumnIfNotExist(selectColumn);
    }

    // Build from clause and where clauses
    addFromClause(topEnrollments, params);
    topEnrollments.where(Condition.raw(getWhereClause(params)));

    // Apply pagination
    addPagingToBuilder(topEnrollments, params);

    // Add to CTE context with special name and type
    cteContext.addShadowCte("top_enrollments", topEnrollments.build(), TOP_ENROLLMENTS);
  }

  /**
   * Creates and adds a "shadow" Common Table Expression (CTE) for the main analytics enrollment
   * table to the provided {@link CteContext}.
   *
   * <p>This shadow CTE is a critical component of the shadow CTE optimization strategy. It is named
   * <em>identically</em> to the actual underlying analytics enrollment table (e.g., {@code
   * analytics_enrollment_PROGRAMUID}.
   *
   * <p>The definition of this shadow CTE is straightforward:
   *
   * <pre>{@code
   * -- Assuming enrollmentTableName is "analytics_enrollment_PROGRAMUID"
   * analytics_enrollment_PROGRAMUID AS (
   *   SELECT * FROM top_enrollments
   * )
   * }</pre>
   *
   * It selects all columns from the previously defined {@code top_enrollments} CTE, which already
   * contains the paginated and filtered set of target enrollments.
   *
   * <p>By naming this CTE the same as the real table, any subsequent CTEs (like those for program
   * indicators or data elements that query enrollment attributes) or parts of the main query that
   * reference the enrollment analytics table by its original name will transparently query this
   * much smaller, pre-filtered shadow version instead of the full physical table. This is due to
   * SQL's behavior where CTEs with the same name as a table take precedence within the scope of the
   * query.
   *
   * <p>This effectively "replaces" the original enrollment table with a paginated and filtered
   * version for the rest of the query construction, without requiring modifications to the SQL
   * generation logic of those subsequent CTEs.
   *
   * @param params The {@link EventQueryParams} used to determine the original enrollment analytics
   *     table name (via {@code params.getTableName()}).
   * @param cteContext The {@link CteContext} to which the generated shadow enrollment table CTE
   *     definition will be added. It's added with a specific type (e.g., {@code
   *     CteType.SHADOW_ENROLLMENT_TABLE}) to ensure correct ordering.
   */
  private void addShadowEnrollmentTableCte(EventQueryParams params, CteContext cteContext) {
    // Create a shadow CTE with the EXACT same name as the real enrollment table
    String enrollmentTableName = params.getTableName(); // e.g., "analytics_enrollment_iphinat79uw"

    String shadowEnrollmentSql = "select * from top_enrollments";

    cteContext.addShadowCte(enrollmentTableName, shadowEnrollmentSql, SHADOW_ENROLLMENT_TABLE);
  }

  /**
   * Creates and adds a "shadow" Common Table Expression (CTE) for the event analytics table
   * associated with the program in the {@link EventQueryParams}.
   *
   * <p>This shadow CTE is a key part of the shadow CTE optimization strategy, designed to work in
   * tandem with the {@code top_enrollments} CTE and the shadow enrollment table CTE. It is named
   * <em>identically</em> to the actual underlying event analytics table (e.g., {@code
   * analytics_event_PROGRAMUID}, where PROGRAMUID is the specific program's UID).
   *
   * <p>The construction of this shadow event CTE involves:
   *
   * <ol>
   *   <li>Selecting all necessary columns (often {@code ae.*}) from the <em>actual</em> physical
   *       event analytics table (aliased, e.g., as {@code ae}).
   *   <li>Performing an {@code INNER JOIN} with the {@code top_enrollments} CTE. This is the
   *       primary filtering mechanism, ensuring that only events belonging to the pre-selected
   *       (paginated and filtered) enrollments are included.
   *   <li>Optionally, it can include a WHERE clause that aggregates relevant event-level filters.
   *       These filters might be derived from conditions originally intended for individual
   *       event-based CTEs (like those for program stage data elements or event-based program
   *       indicators). This further refines the set of events before they are used by subsequent
   *       CTEs.
   * </ol>
   *
   * <p>Example structure:
   *
   * <pre>{@code
   * -- Assuming eventTableName is "analytics_event_PROGRAMUID"
   * analytics_event_PROGRAMUID AS (
   *   SELECT ae.*
   *   FROM analytics_event_PROGRAMUID ae -- Real event table
   *   INNER JOIN top_enrollments te ON te.enrollment = ae.enrollment
   *   WHERE -- Optional aggregated event-level conditions --
   *     (ae.ps = 'stageUid1' AND ae."dataElementUid1" > 10) OR
   *     (ae.ps = 'stageUid2' AND ae."dataElementUid2" = 'true')
   * )
   * }</pre>
   *
   * <p>By defining this shadow CTE with the same name as the actual event table, any subsequent
   * CTEs (e.g., for program stage data elements or event-scoped program indicators) that reference
   * the event table by its original name will transparently query this significantly smaller,
   * pre-filtered shadow version. This avoids scanning the entire event table multiple times,
   * leading to substantial performance improvements.
   *
   * @param params The {@link EventQueryParams} used to determine the program UID (for constructing
   *     the event table name) and to extract potential event-level filters.
   * @param cteContext The {@link CteContext} to which the generated shadow event table CTE
   *     definition will be added. It is typically added with a specific type (e.g., {@code
   *     CteType.SHADOW_EVENT_TABLE}) to ensure correct ordering after {@code top_enrollments} and
   *     the shadow enrollment table.
   */
  private void addShadowEventTableCte(EventQueryParams params, CteContext cteContext) {
    // Create a shadow CTE with the EXACT same name as the real event table
    String eventTableName = "analytics_event_" + params.getProgram().getUid();

    SelectBuilder shadowEvents = new SelectBuilder();

    // Select all columns from the real event table
    shadowEvents
        .addColumn("ae.*")
        .from(eventTableName, "ae") // Reference the REAL table here
        .innerJoin("top_enrollments", "te", alias -> alias + ".enrollment = ae.enrollment");

    // Add aggregated WHERE conditions from all event-level CTEs
    String eventFilters = aggregateEventFiltersFromCtes(params);
    if (!eventFilters.isEmpty()) {
      shadowEvents.where(Condition.raw(eventFilters));
    }

    cteContext.addShadowCte(eventTableName, shadowEvents.build(), SHADOW_EVENT_TABLE);
  }

  private String aggregateEventFiltersFromCtes(EventQueryParams params) {
    List<String> conditions = new ArrayList<>();

    // Collect conditions that should be applied to event-level filtering
    // This includes conditions from program stage items, data elements, etc.

    for (QueryItem item : params.getItems()) {
      if (item.hasProgramStage() && !item.isProgramIndicator()) {
        // Add conditions like: ps = 'XYZ' for program stages
        conditions.add("ps = '" + item.getProgramStage().getUid() + "'");
      }
    }

    for (QueryItem item : params.getItemFilters()) {
      if (item.hasProgramStage() && item.hasFilter()) {
        // Add program stage condition
        conditions.add("ps = '" + item.getProgramStage().getUid() + "'");

        // Add data element filters
        String filterConditions = extractFiltersAsSql(item, quote(item.getItemName()));
        if (!filterConditions.isEmpty()) {
          conditions.add(filterConditions);
        }
      }
    }

    // Combine with OR within program stages, AND between different program stages
    return combineEventConditions(conditions);
  }

  private String combineEventConditions(List<String> conditions) {
    if (conditions.isEmpty()) {
      return "";
    }

    // For now, simple AND combination - might need more sophisticated logic
    return "(" + String.join(" OR ", conditions) + ")";
  }

  private Set<String> getEnrollmentColumnsFromProgramIndicators(EventQueryParams params) {
    Set<String> enrollmentColumns = new HashSet<>();

    Set<String> columns =
        params.getProgram().getNonConfidentialTrackedEntityAttributes().stream()
            .map(columnMapper::getColumnsForAttribute)
            .flatMap(Collection::stream)
            .map(AnalyticsTableColumn::getName)
            .collect(Collectors.toSet());

    String expressions =
        params.getItems().stream()
            .filter(QueryItem::isProgramIndicator)
            // do we need to filter by analytics type?
            .map(item -> ((ProgramIndicator) item.getItem()).getExpression())
            .collect(Collectors.joining("|"));
    // check if any of the columns is part of the expression
    if (!expressions.isEmpty() && !columns.isEmpty()) {
      for (String column : columns) {
        if (expressions.contains(column)) {
          enrollmentColumns.add(column);
        }
      }
    }

    return enrollmentColumns;
  }

  private void addPagingToBuilder(SelectBuilder builder, EventQueryParams params) {
    if (params.isPaging()) {
      if (params.isTotalPages()) {
        builder.limitWithMax(params.getPageSizeWithDefault(), 5000).offset(params.getOffset());
      } else {
        builder
            .limitWithMaxPlusOne(params.getPageSizeWithDefault(), 5000)
            .offset(params.getOffset());
      }
    } else {
      builder.limitPlusOne(5000);
    }
  }

  protected Map<String, String> getFormulaColumnAliases() {
    Map<String, String> aliases = new HashMap<>();
    aliases.put(COLUMN_ENROLLMENT_GEOMETRY_GEOJSON, "enrollmentgeometry_geojson");

    return aliases;
  }

  protected String createDefaultAlias(String formula) {
    // Create a safe alias from the formula
    return "formula_" + (formula.hashCode() & Integer.MAX_VALUE);
  }

  /**
   * Add to the {@link CteContext} the CTE definitions that are specified in the filters of {@link
   * EventQueryParams}.
   *
   * @param params the {@link EventQueryParams} object
   * @param cteContext the {@link CteContext} object
   * @param isAggregateQuery a boolean indicating the filter CTE is generated in the context of an
   *     aggregated query
   */
  void generateFilterCTEs(
      EventQueryParams params, CteContext cteContext, boolean isAggregateQuery) {
    // Combine items and item filters
    List<QueryItem> queryItems =
        Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
            .filter(QueryItem::hasFilter)
            .toList();

    // Group query items by repeatable and non-repeatable stages
    Map<Boolean, List<QueryItem>> itemsByRepeatableFlag =
        queryItems.stream()
            .collect(
                groupingBy(
                    queryItem ->
                        queryItem.hasRepeatableStageParams()
                            && params.getEndpointItem()
                                == RequestTypeAware.EndpointItem.ENROLLMENT));

    // Process repeatable stage filters
    itemsByRepeatableFlag.getOrDefault(true, List.of()).stream()
        .collect(groupingBy(CteUtils::getIdentifier))
        .forEach(
            (identifier, items) -> {
              String cteSql = buildFilterCteSql(items, params);
              if (isAggregateQuery) {
                cteContext.addCteFilter("latest_events", items.get(0), cteSql);
              } else {
                cteContext.addCteFilter(items.get(0), cteSql);
              }
            });

    // Process non-repeatable stage filters
    itemsByRepeatableFlag
        .getOrDefault(false, List.of())
        .forEach(
            queryItem -> {
              if (queryItem.hasProgram() && queryItem.hasProgramStage()) {
                String cteSql = buildFilterCteSql(List.of(queryItem), params);
                if (isAggregateQuery) {
                  cteContext.addCteFilter("latest_events", queryItem, cteSql);
                } else {
                  cteContext.addCteFilter(queryItem, cteSql);
                }
              }
            });
  }

  /**
   * Builds the CTE definitions for the given {@link EventQueryParams}.
   *
   * <p>For each {@link QueryItem} in {@code params}, this method:
   *
   * <ul>
   *   <li>Identifies if the item is a {@link ProgramIndicator} and delegates to {@link
   *       #handleProgramIndicatorCte(QueryItem, CteContext, EventQueryParams)}.
   *   <li>Identifies if the item has a {@link org.hisp.dhis.program.ProgramStage} and generates the
   *       appropriate CTE SQL, including any row-context details if the stage is repeatable.
   *   <li>Adds each resulting CTE (and optional "exists" CTE) to the provided {@link CteContext}.
   * </ul>
   *
   * @param params the {@link EventQueryParams} describing what data is being queried
   * @return a {@link CteContext} instance containing all relevant CTE definitions
   */
  CteContext getCteDefinitions(EventQueryParams params, CteContext cteContext) {
    if (cteContext == null) {
      cteContext = new CteContext(EndpointItem.ENROLLMENT);
    }

    for (QueryItem item : params.getItems()) {
      if (item.isProgramIndicator()) {
        // Handle any program indicator CTE logic.
        handleProgramIndicatorCte(item, cteContext, params);
      } else if (item.hasProgramStage()) {
        // Build CTE for program-stage-based items (including repeatable logic).
        buildProgramStageCte(cteContext, item, params);
      }
    }

    return cteContext;
  }

  void handleProgramIndicatorCte(QueryItem item, CteContext cteContext, EventQueryParams params) {
    ProgramIndicator pi = (ProgramIndicator) item.getItem();
    if (item.hasRelationshipType()) {
      programIndicatorSubqueryBuilder.addCte(
          pi,
          item.getRelationshipType(),
          getAnalyticsType(),
          params.getEarliestStartDate(),
          params.getLatestEndDate(),
          cteContext);
    } else {
      programIndicatorSubqueryBuilder.addCte(
          pi,
          getAnalyticsType(),
          params.getEarliestStartDate(),
          params.getLatestEndDate(),
          cteContext);
    }
  }

  /**
   * Collects the WHERE conditions from both the base enrollment table and the CTE-based filters,
   * then appends them to the SQL.
   */
  private void addWhereClause(SelectBuilder sb, EventQueryParams params, CteContext cteContext) {
    Condition baseConditions = Condition.raw(getWhereClause(params));
    Condition cteConditions = addCteFiltersToWhereClause(params, cteContext);
    sb.where(Condition.and(baseConditions, cteConditions));
  }

  private void addSortingAndPaging(SelectBuilder builder, EventQueryParams params) {
    if (params.isSorting()) {
      builder.orderBy(getSortClause(params));
    }

    // Paging with max limit of 5000
    if (params.isPaging()) {
      if (params.isTotalPages()) {
        builder.limitWithMax(params.getPageSizeWithDefault(), 5000).offset(params.getOffset());
      } else {
        builder
            .limitWithMaxPlusOne(params.getPageSizeWithDefault(), 5000)
            .offset(params.getOffset());
      }
    } else {
      builder.limitPlusOne(5000);
    }
  }

  private void addCteJoins(SelectBuilder builder, CteContext cteContext) {
    for (String itemUid : cteContext.getCteKeys()) {
      CteDefinition cteDef = cteContext.getDefinitionByItemUid(itemUid);

      if (cteDef.isProgramStage()) {
        addProgramStageJoins(builder, itemUid, cteDef);
      } else if (cteDef.isExists()) {
        addExistsJoin(builder, cteDef);
      } else if (cteDef.isProgramIndicator()) {
        addProgramIndicatorJoin(builder, itemUid, cteDef, cteContext);
      } else if (cteDef.isFilter()) {
        addFilterJoin(builder, itemUid, cteDef);
      }
    }
  }

  private void addProgramStageJoins(SelectBuilder builder, String itemUid, CteDefinition cteDef) {
    for (Integer offset : cteDef.getOffsets()) {
      String alias = cteDef.getAlias(offset);
      String joinCondition =
          alias + ".enrollment = ax.enrollment and " + alias + ".rn = " + (offset + 1);
      builder.leftJoin(itemUid, alias, tableAlias -> joinCondition);
    }
  }

  private void addExistsJoin(SelectBuilder builder, CteDefinition cteDef) {
    builder.leftJoin(
        cteDef.getAlias(), "ee", tableAlias -> tableAlias + ".enrollment = ax.enrollment");
  }

  private void addProgramIndicatorJoin(
      SelectBuilder builder, String itemUid, CteDefinition cteDef, CteContext cteContext) {
    if (cteContext.isEventsAnalytics() && cteDef.getCteType() == PROGRAM_INDICATOR_ENROLLMENT) {
      builder.crossJoin(itemUid, cteDef.getAlias());
    } else {
      String alias = cteDef.getAlias();
      builder.leftJoin(itemUid, alias, tableAlias -> tableAlias + ".enrollment = ax.enrollment");
    }
  }

  private void addFilterJoin(SelectBuilder builder, String itemUid, CteDefinition cteDef) {
    String alias = cteDef.getAlias();
    builder.leftJoin(itemUid, alias, tableAlias -> tableAlias + ".enrollment = ax.enrollment");
  }

  /**
   * Add to the {@link CteContext} the CTE definitions that are specified in the filters of {@link
   * EventQueryParams}.
   *
   * @param params the {@link EventQueryParams} object
   * @param cteContext the {@link CteContext} object
   */
  private void generateFilterCTEs(EventQueryParams params, CteContext cteContext) {
    generateFilterCTEs(params, cteContext, false);
  }

  /**
   * Builds and registers a CTE definition for the given {@link QueryItem} (which must have a {@link
   * org.hisp.dhis.program.ProgramStage}). This covers both repeatable and non-repeatable program
   * stages, optionally adding row-context CTEs if needed.
   *
   * @param cteContext the {@link CteContext} to which the new CTE definition(s) will be added
   * @param item the {@link QueryItem} containing program-stage details
   * @param params the {@link EventQueryParams}, used for checking row-context eligibility, offsets,
   *     etc.
   */
  private void buildProgramStageCte(
      CteContext cteContext, QueryItem item, EventQueryParams params) {
    // The event table name, e.g. "analytics_event_XYZ".
    String eventTableName = ANALYTICS_EVENT + item.getProgram().getUid();

    // Quoted column name for the item (e.g. "ax"."my_column").
    String colName = quote(item.getItemName());

    if (params.isAggregatedEnrollments()) {
      handleAggregatedEnrollments(cteContext, item, eventTableName, colName);
      return;
    }

    // Determine if row context is needed (repeatable stage + rowContextAllowed).
    boolean hasRowContext = rowContextAllowedAndNeeded(params, item);

    // Build the main CTE SQL.
    String cteSql = buildMainCteSql(eventTableName, colName, item, hasRowContext);

    // Register this CTE in the context.
    cteContext.addCte(
        item.getProgramStage(),
        item,
        cteSql,
        computeRowNumberOffset(item.getProgramStageOffset()),
        hasRowContext);

    // If row context is needed, we add an extra "exists" CTE for event checks.
    if (hasRowContext) {
      addExistsCte(cteContext, item, eventTableName);
    }
  }

  /**
   * Appends the WITH clause using the CTE definitions from cteContext. If there are no CTE
   * definitions, nothing is appended.
   */
  void addCteClause(SelectBuilder sb, CteContext cteContext) {
    if (!cteContext.hasCteDefinitions()) {
      return;
    }
    Map<String, CteContext.SqlWithCteType> cteSqlWithTypeMap =
        cteContext.getAliasAndDefinitionSqlMap();
    for (Map.Entry<String, CteContext.SqlWithCteType> entry : cteSqlWithTypeMap.entrySet()) {
      if (entry.getValue().cteType() == CteDefinition.CteType.BASE_AGGREGATION) {
        sb.withCTE(ENROLLMENT_AGGR_BASE, entry.getValue().cteDefinitionSql());
      } else {
        sb.withCTE(entry.getKey(), entry.getValue().cteDefinitionSql());
      }
    }
  }

  protected boolean columnIsInFormula(String col) {
    return col.contains("(") && col.contains(")");
  }

  /**
   * Computes a zero-based offset for use with the SQL <em>row_number()</em> function in CTEs that
   * partition and order events by date (e.g., most recent first).
   *
   * <p>In this context, an {@code offset} of 0 typically means “the most recent event” (row_number
   * = 1), a positive offset means “the Nth future event after the most recent” (for example, offset
   * = 1 means row_number = 2), and a negative offset means “the Nth older event before the most
   * recent”.
   *
   * <p>Internally, this method transforms the supplied {@code offset} into a
   * <strong>zero-based</strong> index, suitable for comparing against the row_number output. For
   * instance:
   *
   * <ul>
   *   <li>If {@code offset == 0}, returns {@code 0}.
   *   <li>If {@code offset > 0}, returns {@code offset - 1} (i.e., offset 1 becomes 0-based 0).
   *   <li>If {@code offset < 0}, returns the absolute value ({@code -offset}).
   * </ul>
   *
   * @param offset an integer specifying how many positions away from the most recent event
   *     (row_number = 1) you want to select. A positive offset selects a future row_number, a
   *     negative offset selects a past row_number, and zero selects the most recent.
   * @return an integer representing the zero-based offset to use in a {@code row_number} comparison
   */
  int computeRowNumberOffset(int offset) {
    if (offset == 0) {
      return 0;
    }

    if (offset < 0) {
      return (-1 * offset);
    } else {
      return (offset - 1);
    }
  }

  // ---------------------------------------------------------------------
  // PRIVATE METHODS
  // ---------------------------------------------------------------------

  /**
   * Builds a WHERE clause by combining CTE filters and non-CTE filters for event queries.
   *
   * @param params The event query parameters containing items and filters
   * @param cteContext The CTE context containing CTE definitions
   * @return A Condition representing the combined WHERE clause
   * @throws IllegalArgumentException if params or cteContext is null
   */
  private Condition addCteFiltersToWhereClause(EventQueryParams params, CteContext cteContext) {
    if (params == null || cteContext == null) {
      throw new IllegalArgumentException("Query parameters and CTE context cannot be null");
    }

    Set<QueryItem> processedItems = new HashSet<>();

    // Build CTE conditions
    Condition cteConditions = buildCteConditions(params, cteContext, processedItems);

    // Get non-CTE conditions
    String nonCteWhereClause =
        getQueryItemsAndFiltersWhereClause(params, processedItems, new SqlHelper())
            .replace("where", "");

    // Combine conditions
    if (!nonCteWhereClause.isEmpty()) {
      return cteConditions != null
          ? Condition.and(cteConditions, Condition.raw(nonCteWhereClause))
          : Condition.raw(nonCteWhereClause);
    }

    return cteConditions;
  }

  /**
   * Builds conditions for CTE filters.
   *
   * @param params The event query parameters
   * @param cteContext The CTE context
   * @param processedItems Set to track processed items
   * @return Combined condition for CTE filters
   */
  private Condition buildCteConditions(
      EventQueryParams params, CteContext cteContext, Set<QueryItem> processedItems) {

    List<Condition> conditions = new ArrayList<>();

    List<QueryItem> filters =
        Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
            .filter(QueryItem::hasFilter)
            .toList();

    for (QueryItem item : filters) {
      String cteName = CteUtils.computeKey(item);

      if (cteContext.containsCte(cteName)) {
        processedItems.add(item);
        conditions.addAll(buildItemConditions(item, cteContext.getDefinitionByItemUid(cteName)));
      }
    }

    return conditions.isEmpty() ? null : Condition.and(conditions.toArray(new Condition[0]));
  }

  /**
   * Builds conditions for a single query item.
   *
   * @param item The query item
   * @param cteDef The CTE definition
   * @return List of conditions for the item
   */
  private List<Condition> buildItemConditions(QueryItem item, CteDefinition cteDef) {
    return item.getFilters().stream()
        .map(filter -> buildFilterCondition(filter, item, cteDef))
        .toList();
  }

  /**
   * Builds a condition for a single filter.
   *
   * @param filter The query filter
   * @param item The query item
   * @param cteDef The CTE definition
   * @return Condition for the filter
   */
  private Condition buildFilterCondition(QueryFilter filter, QueryItem item, CteDefinition cteDef) {
    return IN.equals(filter.getOperator())
        ? buildInFilterCondition(filter, item, cteDef)
        : buildStandardFilterCondition(filter, item, cteDef);
  }

  /**
   * Builds a condition for an IN filter.
   *
   * @param filter The IN query filter
   * @param item The query item
   * @param cteDef The CTE definition
   * @return Condition for the IN filter
   */
  private Condition buildInFilterCondition(
      QueryFilter filter, QueryItem item, CteDefinition cteDef) {
    InQueryCteFilter inQueryCteFilter =
        new InQueryCteFilter("value", filter.getFilter(), !item.isNumeric(), cteDef);
    // Compute the offset for the row number if applicable
    Integer offset =
        cteDef.getOffsets().isEmpty() ? null : computeRowNumberOffset(item.getProgramStageOffset());

    return Condition.raw(inQueryCteFilter.getSqlFilter(offset));
  }

  /**
   * Builds a condition for a standard (non-IN) filter.
   *
   * @param filter The query filter
   * @param item The query item
   * @param cteDef The CTE definition
   * @return Condition for the standard filter
   */
  private Condition buildStandardFilterCondition(
      QueryFilter filter, QueryItem item, CteDefinition cteDef) {
    String value = getSqlFilterValue(filter, item);
    String operator = resolveOperator(filter, value);
    return Condition.raw(String.format("%s.value %s %s", cteDef.getAlias(), operator, value));
  }

  private String resolveOperator(QueryFilter filter, String value) {
    String operator = filter.getSqlOperator();

    if (value.trim().equalsIgnoreCase(NULL)) {
      if (isEqualityOperator(filter.getOperator())) {
        operator = "is";
      } else if (filter.getOperator().isNotEqualTo()) {
        operator = "is not";
      } else {
        throw new IllegalQueryException(ErrorCode.E7239, filter.getOperator().getValue());
      }
    }
    return operator;
  }

  private boolean isEqualityOperator(QueryOperator operator) {
    return operator == QueryOperator.EQ || operator == QueryOperator.IEQ;
  }

  private String getSqlFilterValue(QueryFilter filter, QueryItem item) {
    if ("NV".equals(filter.getFilter())) {
      return "NULL"; // Special case for 'null' filters
    }

    // Handle IN operator: wrap the value(s) in parentheses
    if (filter.getOperator() == QueryOperator.IN) {
      String[] values = filter.getFilter().split(","); // Support multiple values
      String quotedValues =
          Arrays.stream(values)
              .map(value -> item.isNumeric() ? value : sqlBuilder.singleQuote(value))
              .collect(Collectors.joining(", "));
      return "(" + quotedValues + ")";
    }

    // Handle text and numeric values
    return item.isNumeric()
        ? filter.getSqlBindFilter()
        : sqlBuilder.singleQuote(filter.getSqlBindFilter());
  }

  private String buildFilterCteSql(List<QueryItem> queryItems, EventQueryParams params) {
    final String filterSql =
        """
                select
                  enrollment,
                  %s as value
                from
                    (select
                        enrollment,
                        %s,
                        row_number() over (
                            partition by enrollment
                            order by
                                occurreddate desc,
                                created desc
                        ) as rn
                    from
                        %s
                    where
                        eventstatus != 'SCHEDULE'
                        %s %s
                    ) ranked
                where
                    rn = 1
                """;

    return queryItems.stream()
        .map(
            item -> {
              // Determine the correct table: event table or enrollment table
              String tableName =
                  item.hasProgramStage()
                      ? "analytics_event_"
                          + item.getProgram()
                              .getUid()
                              .toLowerCase() // Event table for program stage
                      : params.getTableName(); // Enrollment table

              String columnName = quote(item.getItemName()); // Raw column name without alias
              String programStageCondition =
                  item.hasProgramStage()
                      ? "and ps = '" + item.getProgramStage().getUid() + "'"
                      : ""; // Add program stage filter if available

              // Collect the filter on the item
              String filterConditions = extractFiltersAsSql(item, columnName);

              return filterSql.formatted(
                  columnName,
                  columnName,
                  tableName,
                  programStageCondition,
                  StringUtils.isEmpty(filterConditions) ? "" : " and " + filterConditions);
            })
        .collect(Collectors.joining("\nunion all\n"));
  }

  /**
   * Handles the case when aggregated enrollments are enabled.
   *
   * @param cteContext the {@link CteContext} to which the new CTE definition(s) will be added
   * @param item the {@link QueryItem} containing program-stage details
   * @param eventTableName the event table name
   * @param colName the quoted column name for the item
   */
  private void handleAggregatedEnrollments(
      CteContext cteContext, QueryItem item, String eventTableName, String colName) {
    CteDefinition baseAggregatedCte = cteContext.getBaseAggregatedCte();
    assert baseAggregatedCte != null;

    String cteSql = buildAggregatedCteSql(eventTableName, colName, item, baseAggregatedCte);

    cteContext.addCte(
        item.getProgramStage(),
        item,
        cteSql,
        computeRowNumberOffset(item.getProgramStageOffset()),
        false);
  }

  /**
   * Builds the aggregated CTE SQL.
   *
   * @param eventTableName the event table name
   * @param colName the quoted column name for the item
   * @param item the {@link QueryItem} containing program-stage details
   * @param baseAggregatedCte the base aggregated CTE
   * @return the aggregated CTE SQL
   */
  private String buildAggregatedCteSql(
      String eventTableName, String colName, QueryItem item, CteDefinition baseAggregatedCte) {
    String template =
        """
            select
                evt.enrollment,
                evt.${colName} as value
            from (
                select
                    evt.enrollment,
                    evt.${colName},
                    row_number() over (
                        partition by evt.enrollment
                        order by occurreddate desc, created desc
                    ) as rn
                from ${eventTableName} evt
                join ${enrollmentAggrBase} eb on eb.enrollment = evt.enrollment
                where evt.eventstatus != 'SCHEDULE'
                  and evt.ps = '${programStageUid}' and ${aggregateWhereClause}) evt
            where evt.rn = 1
            """;

    Map<String, String> values = new HashMap<>();
    values.put("colName", colName);
    values.put("eventTableName", eventTableName);
    values.put("enrollmentAggrBase", ENROLLMENT_AGGR_BASE);
    values.put("programStageUid", item.getProgramStage().getUid());
    values.put(
        "aggregateWhereClause",
        baseAggregatedCte
            .getAggregateWhereClause()
            // Replace the "ax." alias (from subqueries) with empty string
            .replace("ax.", "")
            .replace("%s", "eb"));

    return new StringSubstitutor(values).replace(template);
  }

  /**
   * Adds an "exists" CTE for event checks.
   *
   * @param cteContext the {@link CteContext} to which the new CTE definition(s) will be added
   * @param item the {@link QueryItem} containing program-stage details
   * @param eventTableName the event table name
   */
  private void addExistsCte(CteContext cteContext, QueryItem item, String eventTableName) {
    String template =
        """
            select distinct
                enrollment
            from
                ${eventTableName}
            where
                eventstatus != 'SCHEDULE'
                and ps = '${programStageUid}'
            """;

    Map<String, String> values = new HashMap<>();
    values.put("eventTableName", eventTableName);
    values.put("programStageUid", item.getProgramStage().getUid());

    String existCte = new StringSubstitutor(values).replace(template);

    cteContext.addExistsCte(item.getProgramStage(), item, existCte);
  }

  /**
   * Builds the main CTE SQL.
   *
   * @param eventTableName the event table name
   * @param colName the quoted column name for the item
   * @param item the {@link QueryItem} containing program-stage details
   * @param hasRowContext whether row context is needed
   * @return the main CTE SQL
   */
  private String buildMainCteSql(
      String eventTableName, String colName, QueryItem item, boolean hasRowContext) {
    String template =
        """
            select
                enrollment,
                ${colName} as value,${rowContext}
                row_number() over (
                    partition by enrollment
                    order by occurreddate desc, created desc
                ) as rn
            from ${eventTableName}
            where eventstatus != 'SCHEDULE'
              and ps = '${programStageUid}'
            """;

    Map<String, String> values = new HashMap<>();
    values.put("colName", colName);
    values.put("rowContext", hasRowContext ? " eventstatus," : "");
    values.put("eventTableName", eventTableName);
    values.put("programStageUid", item.getProgramStage().getUid());

    return new StringSubstitutor(values).replace(template);
  }
}
