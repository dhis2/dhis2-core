/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.common.CteContext.ENROLLMENT_AGGR_BASE;
import static org.hisp.dhis.analytics.common.CteUtils.computeKey;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.CteUtils;
import org.hisp.dhis.analytics.common.InQueryCteFilter;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.AbstractJdbcTableManager;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.util.sql.Condition;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.analytics.util.sql.SqlAliasReplacer;
import org.hisp.dhis.analytics.util.sql.SqlColumnParser;
import org.hisp.dhis.analytics.util.sql.SqlWhereClauseExtractor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.system.util.ListBuilder;
import org.locationtech.jts.util.Assert;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

/**
 * @author Markus Bekken
 */
@Slf4j
@Service("org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager")
public class JdbcEnrollmentAnalyticsManager extends AbstractJdbcEventAnalyticsManager
    implements EnrollmentAnalyticsManager {
  private final EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer;

  private static final String ANALYTICS_EVENT = "analytics_event_";

  private static final String DIRECTION_PLACEHOLDER = "#DIRECTION_PLACEHOLDER";

  private static final String ORDER_BY_EXECUTION_DATE =
      "order by occurreddate " + DIRECTION_PLACEHOLDER + ", created " + DIRECTION_PLACEHOLDER;

  private static final String LIMIT_1 = "limit 1";

  private static final String IS_NOT_NULL = " is not null ";

  private static final String COLUMN_ENROLLMENT_GEOMETRY_GEOJSON =
      String.format(
          "ST_AsGeoJSON(%s)", EnrollmentAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME);

  public JdbcEnrollmentAnalyticsManager(
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      ProgramIndicatorService programIndicatorService,
      ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder,
      EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer,
      ExecutionPlanStore executionPlanStore,
      SqlBuilder sqlBuilder) {
    super(
        jdbcTemplate,
        programIndicatorService,
        programIndicatorSubqueryBuilder,
        executionPlanStore,
        sqlBuilder);
    this.timeFieldSqlRenderer = timeFieldSqlRenderer;
  }

  @Override
  public void getEnrollments(EventQueryParams params, Grid grid, int maxLimit) {
    String sql;
    if (params.isAggregatedEnrollments()) {
      // LUCIANO //
      sql = buildAggregatedEnrollmentQueryWithCte(grid.getHeaders(), params);
      // sql = getAggregatedEnrollmentsSql(grid.getHeaders(), params);
    } else {
      sql = buildEnrollmentQueryWithCte(params);
    }

    System.out.println("SQL: " + sql); // FIXME: Remove debug line

    if (params.analyzeOnly()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql));
    } else {
      withExceptionHandling(
          () -> getEnrollments(params, grid, sql, maxLimit == 0), params.isMultipleQueries());
    }
  }

  /**
   * Adds enrollments to the given grid based on the given parameters and SQL statement.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid}.
   * @param sql the SQL statement used to retrieve events.
   */
  private void getEnrollments(
      EventQueryParams params, Grid grid, String sql, boolean unlimitedPaging) {
    log.debug("Analytics enrollment query SQL: '{}'", sql);

    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

    int rowsRed = 0;

    grid.setLastDataRow(true);

    while (rowSet.next()) {
      if (params.isComingFromQuery()) {
        rowsRed++;
        if (isLastRowAfterPageSize(params, unlimitedPaging, rowsRed)) {
          grid.setLastDataRow(false);
          continue; // skips the last row in n+1 query scenario
        }
      }

      grid.addRow();

      // columnOffset is synchronization aid for <<grid headers>> and <<rowSet columns>> indexes.
      // The amount of headers must not match to amount of columns due the additional ones
      // describing the repeating of repeatable stage.
      int columnOffset = 0;

      for (int i = 0; i < grid.getHeaders().size(); ++i) {
        addGridValue(grid, grid.getHeaders().get(i), i + 1 + columnOffset, rowSet, params);

        if (params.isRowContext()) {
          addValueOriginInfo(grid, rowSet, grid.getHeaders().get(i).getName());
          columnOffset += getRowSetOriginItems(rowSet, grid.getHeaders().get(i).getName());
        }
      }
    }
  }

  /**
   * Retrieves the amount of the supportive columns in database result set.
   *
   * @param rowSet {@link SqlRowSet}.
   * @param columnName The name of the investigated column.
   * @return if the investigated column has some supportive columns like .exists or .status, the
   *     count of the columns is returned.
   */
  private long getRowSetOriginItems(SqlRowSet rowSet, String columnName) {
    return Arrays.stream(rowSet.getMetaData().getColumnNames())
        .filter(
            c ->
                c.equalsIgnoreCase(columnName + ".exists")
                    || c.equalsIgnoreCase(columnName + ".status"))
        .count();
  }

  /**
   * Adds value meta info into the grid. Value meta info is information about origin of the
   * repeatable stage value.
   *
   * @param grid the {@link Grid}.
   * @param rowSet the {@link SqlRowSet}.
   * @param columnName the {@link String}.
   * @return int, the amount of written info items
   */
  private boolean addValueOriginInfo(Grid grid, SqlRowSet rowSet, String columnName) {
    int gridRowIndex = grid.getRows().size() - 1;

    Optional<String> existsMetaInfoColumnName =
        Arrays.stream(rowSet.getMetaData().getColumnNames())
            .filter((columnName + ".exists")::equalsIgnoreCase)
            .findFirst();

    if (existsMetaInfoColumnName.isPresent()) {
      try {
        Optional<String> statusMetaInfoColumnName =
            Arrays.stream(rowSet.getMetaData().getColumnNames())
                .filter((columnName + ".status")::equalsIgnoreCase)
                .findFirst();

        boolean isDefined = rowSet.getBoolean(existsMetaInfoColumnName.get());

        boolean isSet = rowSet.getObject(columnName) != null;

        boolean isScheduled = false;

        if (statusMetaInfoColumnName.isPresent()) {
          String status = rowSet.getString(statusMetaInfoColumnName.get());
          isScheduled = "schedule".equalsIgnoreCase(status);
        }

        ValueStatus valueStatus = ValueStatus.of(isDefined, isSet, isScheduled);

        if (valueStatus == ValueStatus.SET) {
          return true;
        }

        Map<Integer, Map<String, Object>> rowContext = grid.getRowContext();

        Map<String, Object> row = rowContext.get(gridRowIndex);

        if (row == null) {
          row = new HashMap<>();
        }

        Map<String, String> colValueType = new HashMap<>();

        colValueType.put("valueStatus", valueStatus.getValue());

        row.put(columnName, colValueType);

        rowContext.put(gridRowIndex, row);

        return true;
      } catch (InvalidResultSetAccessException ignored) {
        // when .exists extension of column name does not indicate boolean flag,
        // value will not be added and method returns false
      }
    }

    return false;
  }

  @Override
  public long getEnrollmentCount(EventQueryParams params) {
    String sql = "select count(pi) ";

    sql += getFromClause(params);

    sql += getWhereClause(params);
    sql += addFiltersToWhereClause(params);

    long count = 0;

    log.debug("Analytics enrollment count SQL: '{}'", sql);

    final String finalSqlValue = sql;

    if (params.analyzeOnly()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(params.getExplainOrderId(), finalSqlValue));
    } else {
      count =
          withExceptionHandling(
                  () -> jdbcTemplate.queryForObject(finalSqlValue, Long.class),
                  params.isMultipleQueries())
              .orElse(0L);
    }

    return count;
  }

  /**
   * Returns a from SQL clause for the given analytics table partition.
   *
   * @param params the {@link EventQueryParams}.
   */
  @Override
  protected String getFromClause(EventQueryParams params) {
    return " from "
        + params.getTableName()
        + " as "
        + ANALYTICS_TBL_ALIAS
        + " "
        + joinOrgUnitTables(params, getAnalyticsType());
  }

  /**
   * Returns a from and where SQL clause. If this is a program indicator with non-default
   * boundaries, the relationship with the reporting period is specified with where conditions on
   * the enrollment or incident dates. If the default boundaries is used, or the params does not
   * include program indicators, the periods are joined in from the analytics tables the normal way.
   * A where clause can never have a mix of indicators with non-default boundaries and regular
   * analytics table periods.
   *
   * @param params the {@link EventQueryParams}.
   */
  @Override
  protected String getWhereClause(EventQueryParams params) {
    String sql = "";
    SqlHelper hlp = new SqlHelper();

    // ---------------------------------------------------------------------
    // Periods
    // ---------------------------------------------------------------------

    String timeFieldSql = timeFieldSqlRenderer.renderPeriodTimeFieldSql(params);

    if (StringUtils.isNotBlank(timeFieldSql)) {
      sql += hlp.whereAnd() + " " + timeFieldSql;
    }

    // ---------------------------------------------------------------------
    // Organisation units
    // ---------------------------------------------------------------------

    if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)) {
      sql +=
          hlp.whereAnd()
              + " ou in ("
              + getQuotedCommaDelimitedString(
                  getUids(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID)))
              + ") ";
    } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
      sql +=
          hlp.whereAnd()
              + " ou in ("
              + getQuotedCommaDelimitedString(getUids(params.getOrganisationUnitChildren()))
              + ") ";
    } else // Descendants
    {
      sql += hlp.whereAnd() + " (";

      for (DimensionalItemObject object : params.getDimensionOrFilterItems(ORGUNIT_DIM_ID)) {
        OrganisationUnit unit = (OrganisationUnit) object;
        sql +=
            params.getOrgUnitField().getOrgUnitLevelCol(unit.getLevel(), getAnalyticsType())
                + " = '"
                + unit.getUid()
                + "' or ";
      }

      sql = removeLastOr(sql) + ") ";
    }

    // ---------------------------------------------------------------------
    // Categories (enrollments don't have attribute categories)
    // ---------------------------------------------------------------------

    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.CATEGORY));

    for (DimensionalObject dim : dynamicDimensions) {
      if (!isAttributeCategory(dim)) {
        String col = quoteAlias(dim.getDimensionName());

        sql +=
            "and " + col + " in (" + getQuotedCommaDelimitedString(getUids(dim.getItems())) + ") ";
      }
    }

    // ---------------------------------------------------------------------
    // Organisation unit group sets
    // ---------------------------------------------------------------------

    dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.ORGANISATION_UNIT_GROUP_SET));

    for (DimensionalObject dim : dynamicDimensions) {
      if (!dim.isAllItems()) {
        String col = quoteAlias(dim.getDimensionName());

        sql +=
            "and " + col + " in (" + getQuotedCommaDelimitedString(getUids(dim.getItems())) + ") ";
      }
    }

    // ---------------------------------------------------------------------
    // Program stage
    // ---------------------------------------------------------------------

    if (params.hasProgramStage()) {
      sql += "and ps = '" + params.getProgramStage().getUid() + "' ";
    }

    // ---------------------------------------------------------------------
    // Filter expression
    // ---------------------------------------------------------------------

    if (params.hasProgramIndicatorDimension() && params.getProgramIndicator().hasFilter()) {
      String filter =
          programIndicatorService.getAnalyticsSql(
              params.getProgramIndicator().getFilter(),
              BOOLEAN,
              params.getProgramIndicator(),
              params.getEarliestStartDate(),
              params.getLatestEndDate());

      String sqlFilter = ExpressionUtils.asSql(filter);

      sql += "and (" + sqlFilter + ") ";
    }

    // ---------------------------------------------------------------------
    // Various filters
    // ---------------------------------------------------------------------

    if (params.hasEnrollmentStatuses()) {
      sql +=
          "and enrollmentstatus in ("
              + params.getEnrollmentStatus().stream()
                  .map(p -> singleQuote(p.name()))
                  .collect(joining(","))
              + ") ";
    }

    if (params.isCoordinatesOnly()) {
      sql += "and (longitude is not null and latitude is not null) ";
    }

    if (params.isGeometryOnly()) {
      sql +=
          "and "
              + getCoalesce(
                  params.getCoordinateFields(),
                  FallbackCoordinateFieldType.ENROLLMENT_GEOMETRY.getValue())
              + IS_NOT_NULL;
    }

    if (params.isCompletedOnly()) {
      sql += "and completeddate is not null ";
    }

    if (params.hasBbox()) {
      sql +=
          "and "
              + getCoalesce(
                  params.getCoordinateFields(),
                  FallbackCoordinateFieldType.ENROLLMENT_GEOMETRY.getValue())
              + " && ST_MakeEnvelope("
              + params.getBbox()
              + ",4326) ";
    }

    return sql;
  }

  private String addFiltersToWhereClause(EventQueryParams params) {
    return getQueryItemsAndFiltersWhereClause(params, new SqlHelper());
  }

  private String addCteFiltersToWhereClause(EventQueryParams params, CteContext cteContext) {
    StringBuilder cteWhereClause = new StringBuilder();
    Set<QueryItem> processedItems = new HashSet<>(); // Track processed items

    // Get all filters from the query items and item filters
    List<QueryItem> filters =
        Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
            .filter(QueryItem::hasFilter)
            .toList();
    // Iterate over each filter and apply the correct condition
    for (QueryItem item : filters) {
      String cteName = CteUtils.computeKey(item);

      if (cteContext.containsCte(cteName)) {
        processedItems.add(item); // Mark item as processed
        CteDefinition cteDef = cteContext.getDefinitionByItemUid(cteName);
        for (QueryFilter filter : item.getFilters()) {
          if (IN.equals(filter.getOperator())) {
            InQueryCteFilter inQueryCteFilter =
                new InQueryCteFilter("value", filter.getFilter(), item.isText(), cteDef);
            cteWhereClause
                .append(" and ")
                .append(
                    inQueryCteFilter.getSqlFilter(
                        computeRowNumberOffset(item.getProgramStageOffset())));
          } else {
            String value = getSqlFilterValue(filter, item);

            cteWhereClause
                .append(" and ")
                .append(cteDef.getAlias())
                .append(".value ")
                .append("NULL".equals(value) ? "is" : filter.getSqlOperator())
                .append(" ")
                .append(value);
          }
        }
      }
    }
    // Add filters for items that are not part of the CTE
    String nonCteWhereClause =
        getQueryItemsAndFiltersWhereClause(params, processedItems, new SqlHelper())
            .replace("where", "");
    if (nonCteWhereClause.isEmpty()) return cteWhereClause.toString();

    String currentWhereClause = cteWhereClause.toString().toLowerCase().trim();
    cteWhereClause.append(
        currentWhereClause.endsWith("and") ? nonCteWhereClause : " and " + nonCteWhereClause);

    return cteWhereClause.toString();
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
                      ? "AND ps = '" + item.getProgramStage().getUid() + "'"
                      : ""; // Add program stage filter if available

              return """
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
                                  %s
                              ) ranked
                          where
                              rn = 1
                      """
                  .formatted(columnName, columnName, tableName, programStageCondition);
            })
        .collect(Collectors.joining("\nUNION ALL\n"));
  }

  @Override
  protected String getSelectClause(EventQueryParams params) {
    List<String> selectCols =
        ListUtils.distinctUnion(
            params.isAggregatedEnrollments() ? List.of("enrollment") : getStandardColumns(),
            getSelectColumns(params, false));

    return "select " + StringUtils.join(selectCols, ",") + " ";
  }

  /**
   * Returns an encoded column name respecting the geometry/coordinate format. The given QueryItem
   * must be of type COORDINATE.
   *
   * @param item the {@link QueryItem}
   * @return the column selector (SQL query) or EMPTY if the item valueType is not COORDINATE.
   * @throws NullPointerException if item is null
   */
  @Override
  protected ColumnAndAlias getCoordinateColumn(QueryItem item) {
    return getCoordinateColumn(item, null);
  }

  /**
   * Returns an encoded column name respecting the geometry/coordinate format. The given QueryItem
   * can be of type COORDINATE or ORGANISATION_UNIT.
   *
   * @param suffix is currently ignored. Not currently used for enrollments
   * @param item the {@link QueryItem}
   * @return the column selector (SQL query) or EMPTY if the item valueType is not COORDINATE.
   * @throws NullPointerException if item is null
   */
  @Override
  protected ColumnAndAlias getCoordinateColumn(QueryItem item, String suffix) {
    if (item.getProgram() != null) {
      String eventTableName = ANALYTICS_EVENT + item.getProgram().getUid();
      String colName = quote(item.getItemId());

      String psCondition = "";

      if (item.hasProgramStage()) {
        assertProgram(item);

        psCondition = "and ps = '" + item.getProgramStage().getUid() + "' ";
      }

      String stCentroidFunction = "";

      if (ValueType.ORGANISATION_UNIT == item.getValueType()) {
        stCentroidFunction = "ST_Centroid";
      }

      String alias = getAlias(item).orElse(null);

      return ColumnAndAlias.ofColumnAndAlias(
          "(select '[' || round(ST_X("
              + stCentroidFunction
              + "("
              + colName
              + "))::numeric, 6) || ',' || round(ST_Y("
              + stCentroidFunction
              + "("
              + colName
              + "))::numeric, 6) || ']' as "
              + colName
              + " from "
              + eventTableName
              + " where "
              + eventTableName
              + ".enrollment = "
              + ANALYTICS_TBL_ALIAS
              + ".enrollment "
              + "and "
              + colName
              + IS_NOT_NULL
              + psCondition
              + " "
              + createOrderType(item.getProgramStageOffset())
              + " "
              + createOffset(item.getProgramStageOffset())
              + " "
              + LIMIT_1
              + " )",
          alias);
    }

    return ColumnAndAlias.EMPTY;
  }

  @Override
  protected String getColumnWithCte(QueryItem item, String suffix, CteContext cteContext) {
    List<String> columns = new ArrayList<>();

    CteDefinition cteDef = cteContext.getDefinitionByItemUid(computeKey(item));
    int programStageOffset = computeRowNumberOffset(item.getProgramStageOffset());
    String alias = getAlias(item).orElse(null);
    columns.add("%s.value as %s".formatted(cteDef.getAlias(programStageOffset), quote(alias)));
    if (cteDef.isRowContext()) {
      // Add additional status and exists columns for row context
      columns.add(
          "COALESCE(%s.rn = %s, false) as %s"
              .formatted(
                  cteDef.getAlias(programStageOffset),
                  programStageOffset + 1,
                  quote(alias + ".exists")));
      columns.add(
          "%s.eventstatus as %s"
              .formatted(cteDef.getAlias(programStageOffset), quote(alias + ".status")));
    }
    return String.join(",\n", columns);
  }

  /**
   * Creates a column "selector" for the given item name. The suffix will be appended as part of the
   * item name. The column selection is based on events analytics tables.
   *
   * @param item the {@link QueryItem}
   * @param suffix to be appended to the item name (column)
   * @return when there is a program stage: returns the column select statement for the given item
   *     and suffix, otherwise returns the item name quoted and prefixed with the table prefix. ie.:
   *     ax."enrollmentdate"
   */
  @Override
  protected String getColumn(QueryItem item, String suffix) {
    String colName = item.getItemName();
    String alias = EMPTY;

    if (item.hasProgramStage()) {
      assertProgram(item);

      colName = quote(colName + suffix);

      String eventTableName = ANALYTICS_EVENT + item.getProgram().getUid();
      String excludingScheduledCondition =
          eventTableName + ".eventstatus != '" + EventStatus.SCHEDULE + "' and ";

      if (item.getProgramStage().getRepeatable() && item.hasRepeatableStageParams()) {
        return "(select "
            + colName
            + " from "
            + eventTableName
            + " where "
            + excludingScheduledCondition
            + eventTableName
            + ".enrollment = "
            + ANALYTICS_TBL_ALIAS
            + ".enrollment "
            + "and ps = '"
            + item.getProgramStage().getUid()
            + "' "
            + getExecutionDateFilter(
                item.getRepeatableStageParams().getStartDate(),
                item.getRepeatableStageParams().getEndDate())
            + createOrderType(item.getProgramStageOffset())
            + " "
            + createOffset(item.getProgramStageOffset())
            + " "
            + LIMIT_1
            + " )";
      }

      if (item.getItem().getDimensionItemType() == DATA_ELEMENT && item.getProgramStage() != null) {
        alias = " as " + quote(item.getProgramStage().getUid() + "." + item.getItem().getUid());
      }

      return "(select "
          + colName
          + alias
          + " from "
          + eventTableName
          + " where "
          + excludingScheduledCondition
          + eventTableName
          + ".enrollment = "
          + ANALYTICS_TBL_ALIAS
          + ".enrollment "
          + "and "
          + colName
          + IS_NOT_NULL
          + "and ps = '"
          + item.getProgramStage().getUid()
          + "' "
          + createOrderType(item.getProgramStageOffset())
          + " "
          + createOffset(item.getProgramStageOffset())
          + " "
          + LIMIT_1
          + " )";
    } else if (isOrganizationUnitProgramAttribute(item)) {
      return quoteAlias(colName + suffix);
    } else {
      return quoteAlias(colName);
    }
  }

  /**
   * Returns a list of names of standard columns.
   *
   * @return a list of names of standard columns.
   */
  private List<String> getStandardColumns() {
    ListBuilder<String> columns = new ListBuilder<>();

    columns.add(
        EnrollmentAnalyticsColumnName.ENROLLMENT_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.TRACKED_ENTITY_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.STORED_BY_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.CREATED_BY_DISPLAY_NAME_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_DISPLAY_NAME_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME);

    if (sqlBuilder.supportsGeospatialData()) {
      columns.add(
          COLUMN_ENROLLMENT_GEOMETRY_GEOJSON,
          EnrollmentAnalyticsColumnName.LONGITUDE_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.LATITUDE_COLUMN_NAME);
    }

    columns.add(
        EnrollmentAnalyticsColumnName.OU_NAME_COLUMN_NAME,
        AbstractJdbcTableManager.OU_NAME_HIERARCHY_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.OU_CODE_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME);

    return columns.build();
  }

  /**
   * Returns true if the item is a program attribute and the value type is an organizational unit.
   *
   * @param item the {@link QueryItem}.
   */
  private boolean isOrganizationUnitProgramAttribute(QueryItem item) {
    return item.getValueType() == ValueType.ORGANISATION_UNIT
        && item.getItem().getDimensionItemType() == DimensionItemType.PROGRAM_ATTRIBUTE;
  }

  /**
   * Returns an encoded column name wrapped in lower directive if not numeric or boolean.
   *
   * @param item the {@link QueryItem}.
   */
  @Override
  protected String getColumn(QueryItem item) {
    return getColumn(item, "");
  }

  /**
   * Is a category dimension an attribute category (rather than a disaggregation category)?
   * Attribute categories are not included in enrollment tables, so category user dimension
   * restrictions (which use attribute categories) do not apply.
   */
  private boolean isAttributeCategory(DimensionalObject categoryDim) {
    return ((CategoryOption) categoryDim.getItems().get(0))
            .getCategories()
            .iterator()
            .next()
            .getDataDimensionType()
        == ATTRIBUTE;
  }

  private String getExecutionDateFilter(Date startDate, Date endDate) {
    StringBuilder sb = new StringBuilder();

    if (startDate != null) {
      sb.append(" and occurreddate >= ");

      sb.append(String.format("%s ", sqlBuilder.singleQuote(toMediumDate(startDate))));
    }

    if (endDate != null) {
      sb.append(" and occurreddate <= ");

      sb.append(String.format("%s ", sqlBuilder.singleQuote(toMediumDate(endDate))));
    }

    return sb.toString();
  }

  private void assertProgram(QueryItem item) {
    Assert.isTrue(
        item.hasProgram(),
        "Can not query item with program stage but no program:" + item.getItemName());
  }

  @Override
  protected AnalyticsType getAnalyticsType() {
    return AnalyticsType.ENROLLMENT;
  }

  private String createOffset(int offset) {
    if (offset == 0) {
      return EMPTY;
    }

    if (offset < 0) {
      return "offset " + (-1 * offset);
    } else {
      return "offset " + (offset - 1);
    }
  }

  private String createOrderType(int offset) {
    if (offset == 0) {
      return ORDER_BY_EXECUTION_DATE.replace(DIRECTION_PLACEHOLDER, "desc");
    }
    if (offset < 0) {
      return ORDER_BY_EXECUTION_DATE.replace(DIRECTION_PLACEHOLDER, "desc");
    } else {
      return ORDER_BY_EXECUTION_DATE.replace(DIRECTION_PLACEHOLDER, "asc");
    }
  }

  // New methods //

  private void handleProgramIndicatorCte(
      QueryItem item, CteContext cteContext, EventQueryParams params) {
    ProgramIndicator pi = (ProgramIndicator) item.getItem();
    if (item.hasRelationshipType()) {
      programIndicatorSubqueryBuilder.contributeCte(
          pi,
          item.getRelationshipType(),
          getAnalyticsType(),
          params.getEarliestStartDate(),
          params.getLatestEndDate(),
          cteContext);
    } else {
      programIndicatorSubqueryBuilder.contributeCte(
          pi,
          getAnalyticsType(),
          params.getEarliestStartDate(),
          params.getLatestEndDate(),
          cteContext);
    }
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
  private CteContext getCteDefinitions(EventQueryParams params, CteContext cteContext) {
    if (cteContext == null) {
      cteContext = new CteContext();
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

  private CteContext getCteDefinitions(EventQueryParams params) {
    return getCteDefinitions(params, null);
  }

  private List<String> addBaseAggregationCte(CteContext cteContext, EventQueryParams params) {
    // create base enrollment context
    List<String> columns = new ArrayList<>();
    List<String> rootColumns;
    columns.add("enrollment");

    addDimensionSelectColumns(columns, params, true);

    SelectBuilder sb = new SelectBuilder();
    for (String column : Sets.newHashSet(columns)) {
      sb.addColumn(SqlColumnParser.removeTableAlias(column));
    }
    // return the name of the columns that are part
    // of the original params, no need to return also
    // the columns that are part of the filters
    rootColumns = sb.getColumnNames();
    sb.from(getFromClause(params));
    sb.where(
        Condition.and(
            Condition.raw(getWhereClause(params)), Condition.raw(addFiltersToWhereClause(params))));

    // Extract the columns from the where clause
    List<String> cols = SqlWhereClauseExtractor.extractWhereColumns(sb.build());
    for (String col : cols) {
      sb.addColumnIfNotExist(col);
    }
    // Add the base aggregate CTE along with the original where
    // condition, that have to be propagated in every other CTE
    cteContext.addBaseAggregateCte(
        sb.build(), SqlAliasReplacer.replaceTableAliases(sb.getWhereClause(), cols));

    return rootColumns;
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

      CteDefinition baseAggregatedCte = cteContext.getBaseAggregatedCte();
      assert baseAggregatedCte != null;

      String cteSql =
          """
              select
                    evt.enrollment,
                    evt.%s as value
              from (
              select
                  evt.enrollment,
                  evt.%s,
                  row_number() over (
                      partition by evt.enrollment
                      order by occurreddate desc, created desc
                  ) as rn
              from %s evt
              join %s eb ON eb.enrollment = evt.enrollment
              where evt.eventstatus != 'SCHEDULE'
                and evt.ps = '%s' and %s) evt
                where evt.rn = 1
              """
              .formatted(
                  colName,
                  colName,
                  eventTableName,
                  ENROLLMENT_AGGR_BASE,
                  item.getProgramStage().getUid(),
                  baseAggregatedCte.getAggregateWhereClause().replaceAll("%s", "eb"));

      System.out.println(cteSql);

      cteContext.addCte(
          item.getProgramStage(),
          item,
          cteSql,
          computeRowNumberOffset(item.getProgramStageOffset()),
          false);
      return;
    }

    // Determine if row context is needed (repeatable stage + rowContextAllowed).
    boolean hasRowContext = rowContextAllowedAndNeeded(params, item);

    // Build the main CTE SQL.
    // If hasRowContext == true, we'll also include the eventstatus column.
    String cteSql =
        """
        select
            enrollment,
            %s as value,%s
            row_number() over (
                partition by enrollment
                order by occurreddate desc, created desc
            ) as rn
        from %s
        where eventstatus != 'SCHEDULE'
          and ps = '%s'
        """
            .formatted(
                colName,
                hasRowContext ? " eventstatus," : "",
                eventTableName,
                item.getProgramStage().getUid());

    // Register this CTE in the context.
    // The createOffset2(...) method calculates the row offset based on
    // item.getProgramStageOffset().
    cteContext.addCte(
        item.getProgramStage(),
        item,
        cteSql,
        computeRowNumberOffset(item.getProgramStageOffset()),
        hasRowContext);

    // If row context is needed, we add an extra "exists" CTE for event checks.
    if (hasRowContext) {
      String existCte =
          """
            select distinct
                enrollment
            from
                %s
            where
                eventstatus != 'SCHEDULE'
                and ps = '%s'
            """
              .formatted(eventTableName, item.getProgramStage().getUid());

      cteContext.addExistsCte(item.getProgramStage(), item, existCte);
    }
  }

  private void addCteJoins(SelectBuilder builder, CteContext cteContext) {
    for (String itemUid : cteContext.getCteKeys()) {
      CteDefinition cteDef = cteContext.getDefinitionByItemUid(itemUid);

      // Handle Program Stage CTE (potentially with multiple offsets)
      if (cteDef.isProgramStage()) {
        for (Integer offset : cteDef.getOffsets()) {
          String alias = cteDef.getAlias(offset);
          builder.leftJoin(
              itemUid,
              alias,
              tableAlias ->
                  tableAlias
                      + ".enrollment = ax.enrollment AND "
                      + tableAlias
                      + ".rn = "
                      + (offset + 1));
        }
      }

      // Handle 'Exists' type CTE
      if (cteDef.isExists()) {
        builder.leftJoin(itemUid, "ee", tableAlias -> tableAlias + ".enrollment = ax.enrollment");
      }

      // Handle Program Indicator CTE
      if (cteDef.isProgramIndicator()) {
        String alias = cteDef.getAlias();
        builder.leftJoin(itemUid, alias, tableAlias -> tableAlias + ".enrollment = ax.enrollment");
      }

      // Handle Filter CTE
      if (cteDef.isFilter()) {
        String alias = cteDef.getAlias();
        builder.leftJoin(itemUid, alias, tableAlias -> tableAlias + ".enrollment = ax.enrollment");
      }
    }
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
  private int computeRowNumberOffset(int offset) {
    if (offset == 0) {
      return 0;
    }

    if (offset < 0) {
      return (-1 * offset);
    } else {
      return (offset - 1);
    }
  }

  private void generateFilterCTEs(EventQueryParams params, CteContext cteContext) {
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
              // TODO is this correct? items.get(0)
              cteContext.addCteFilter(items.get(0), cteSql);
            });

    // Process non-repeatable stage filters
    itemsByRepeatableFlag
        .getOrDefault(false, List.of())
        .forEach(
            queryItem -> {
              if (queryItem.hasProgram() && queryItem.hasProgramStage()) {
                String cteSql = buildFilterCteSql(List.of(queryItem), params);
                cteContext.addCteFilter(queryItem, cteSql);
              }
            });
  }

  private String buildAggregatedEnrollmentQueryWithCte(
      List<GridHeader> headers, EventQueryParams params) {
    CteContext cteContext = new CteContext();
    // add base aggregation CTE
    List<String> rootQueryColumns = addBaseAggregationCte(cteContext, params);

    // Add CTE definitions for program indicators, program stages, etc.
    getCteDefinitions(params, cteContext);

    SelectBuilder sb = new SelectBuilder();
    // add the CTE with clause based on the CTE definitions accumulated so far
    addCteClause(sb, cteContext);

    // add select clause
    sb.addColumn("count(eb.enrollment) as value");
    // add the columns from the root CTE query
    rootQueryColumns.stream()
        .filter(col -> !col.equals("enrollment"))
        .peek(sb::groupBy)
        .forEach(sb::addColumn);
    // add the columns from the CTE definitions
    cteContext
        .getCteKeys(ENROLLMENT_AGGR_BASE)
        .forEach(
            itemUid -> {
              CteDefinition cteDef = cteContext.getDefinitionByItemUid(itemUid);
              if (cteDef.isProgramStage()) {
                String columnAlias = quote(cteDef.getProgramStageUid() + "." + cteDef.getItemId());
                sb.addColumn(cteDef.getAlias() + ".value", "", columnAlias);
                sb.groupBy(columnAlias);
              }
            });

    // add from
    sb.from(ENROLLMENT_AGGR_BASE, "eb");

    // Add join statements for each CTE definition
    for (String itemUid : cteContext.getCteKeys(ENROLLMENT_AGGR_BASE)) {
      CteDefinition cteDef = cteContext.getDefinitionByItemUid(itemUid);
      sb.leftJoin(
          itemUid, cteDef.getAlias(), tableAlias -> tableAlias + ".enrollment = eb.enrollment");
    }

    return sb.build();
  }

  private String buildEnrollmentQueryWithCte(EventQueryParams params) {
    // LUCIANO //

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

    // 3.3: Append the FROM clause (the main enrollment analytics table)
    addFromClause(sb, params);

    // 3.4: Append LEFT JOINs for each relevant CTE definition
    addCteJoins(sb, cteContext);

    // 3.5: Collect and append WHERE conditions (including filters from CTE)
    addWhereClause(sb, params, cteContext);

    // 3.6: Append ORDER BY and paging
    addSortingAndPaging(sb, params);

    return sb.build();
  }

  /**
   * Appends the WITH clause using the CTE definitions from cteContext. If there are no CTE
   * definitions, nothing is appended.
   */
  private void addCteClause(SelectBuilder sb, CteContext cteContext) {
    cteContext.getCteDefinitions().forEach(sb::withCTE);
  }

  private boolean columnIsInFormula(String col) {
    return col.contains("(") && col.contains(")");
  }

  /**
   * Appends the SELECT clause, including both the standard enrollment columns (or aggregated
   * columns) and columns derived from the CTE definitions.
   */
  private void addSelectClause(SelectBuilder sb, EventQueryParams params, CteContext cteContext) {

    // Append standard columns or aggregated columns
    if (params.isAggregatedEnrollments()) {
      sb.addColumn("count(eb.enrollment) as value");
    } else {
      getStandardColumns()
          .forEach(
              column -> {
                if (columnIsInFormula(column)) {
                  sb.addColumn(column);
                } else {
                  sb.addColumn(column, "ax");
                }
              });
    }

    // Append columns from CTE definitions
    getSelectColumnsWithCTE(params, cteContext).forEach(sb::addColumn);
  }

  /** Appends the FROM clause, i.e. the main table name and alias. */
  private void addFromClause(SelectBuilder sb, EventQueryParams params) {
    sb.from(params.getTableName(), "ax");
  }

  /**
   * Collects the WHERE conditions from both the base enrollment table and the CTE-based filters,
   * then appends them to the SQL.
   */
  private void addWhereClause(SelectBuilder sb, EventQueryParams params, CteContext cteContext) {
    Condition baseConditions = Condition.raw(getWhereClause(params));
    Condition cteConditions = Condition.raw(addCteFiltersToWhereClause(params, cteContext));
    sb.where(Condition.and(baseConditions, cteConditions));
  }

  private void addSortingAndPaging(SelectBuilder builder, EventQueryParams params) {
    if (params.isSorting()) {
      // Assuming getSortFields returns List<OrderByClause>
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

  protected String getSortClause(EventQueryParams params) {
    if (params.isSorting()) {
      return super.getSortClause(params);
    }
    return "";
  }
}
