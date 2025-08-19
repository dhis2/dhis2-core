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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.common.CteContext.ENROLLMENT_AGGR_BASE;
import static org.hisp.dhis.analytics.common.CteUtils.computeKey;
import static org.hisp.dhis.analytics.event.data.EnrollmentOrgUnitFilterHandler.hasEnrollmentOrgUnitFilter;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getHeaderColumns;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getOrgUnitLevelColumns;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getPeriodColumns;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.analytics.util.EventQueryParamsUtils.getProgramIndicators;
import static org.hisp.dhis.analytics.util.EventQueryParamsUtils.withoutProgramStageItems;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.AbstractJdbcTableManager;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
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
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.ListBuilder;
import org.locationtech.jts.util.Assert;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

/**
 * @author Markus Bekken
 */
@Slf4j
@Service("org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager")
public class JdbcEnrollmentAnalyticsManager extends AbstractJdbcEventAnalyticsManager
    implements EnrollmentAnalyticsManager {
  private final EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer;

  private static final String DIRECTION_PLACEHOLDER = "#DIRECTION_PLACEHOLDER";

  private static final String ORDER_BY_EXECUTION_DATE =
      "order by occurreddate " + DIRECTION_PLACEHOLDER + ", created " + DIRECTION_PLACEHOLDER;

  private static final String LIMIT_1 = "limit 1";

  private static final String IS_NOT_NULL = " is not null ";

  private static final String ENROLLMENT_COL = "enrollment";

  public JdbcEnrollmentAnalyticsManager(
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      ProgramIndicatorService programIndicatorService,
      ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder,
      PiDisagInfoInitializer piDisagInfoInitializer,
      PiDisagQueryGenerator piDisagQueryGenerator,
      EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer,
      ExecutionPlanStore executionPlanStore,
      SystemSettingsService settingsService,
      DhisConfigurationProvider config,
      AnalyticsSqlBuilder sqlBuilder,
      OrganisationUnitResolver organisationUnitResolver,
      ColumnMapper columnMapper) {
    super(
        jdbcTemplate,
        programIndicatorService,
        programIndicatorSubqueryBuilder,
        piDisagInfoInitializer,
        piDisagQueryGenerator,
        executionPlanStore,
        sqlBuilder,
        settingsService,
        config,
        organisationUnitResolver,
        columnMapper);
    this.timeFieldSqlRenderer = timeFieldSqlRenderer;
  }

  @Override
  public void getEnrollments(EventQueryParams params, Grid grid, int maxLimit) {
    String sql;
    if (params.isAggregatedEnrollments()) {
      sql =
          useExperimentalAnalyticsQueryEngine()
              ? buildAggregatedEnrollmentQueryWithCte(grid.getHeaders(), params)
              : getAggregatedEnrollmentsSql(grid.getHeaders(), params);
    } else {
      sql =
          useExperimentalAnalyticsQueryEngine()
              ? buildAnalyticsQuery(params)
              : getAggregatedEnrollmentsSql(params, maxLimit);
    }
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
    List<String> columnLabels = getColumnLabels(rowSet);

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
          columnOffset += getRowSetOriginItems(columnLabels, grid.getHeaders().get(i).getName());
        }
      }
    }
  }

  /**
   * Retrieves the amount of the supportive columns in database result set.
   *
   * @param columns List of columns from a {@link SqlRowSet}.
   * @param columnName The name of the investigated column.
   * @return if the investigated column has some supportive columns like .exists or .status, the
   *     count of the columns is returned.
   */
  private int getRowSetOriginItems(List<String> columns, String columnName) {
    return (int)
        columns.stream()
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

  /** Appends the FROM clause, i.e. the main table name and alias. */
  @Override
  void addFromClause(SelectBuilder sb, EventQueryParams params) {
    sb.from(params.getTableName(), "ax");
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
        String dimName = dim.getDimensionName();
        String col =
            params.isPiDisagDimension(dimName)
                ? piDisagQueryGenerator.getColumnForWhereClause(params, dimName)
                : quoteAlias(dimName);

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
    // Query items and filters
    // ---------------------------------------------------------------------
    if (!useExperimentalAnalyticsQueryEngine()) {
      sql += getQueryItemsAndFiltersWhereClause(params, hlp);
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

  @Override
  protected String getSelectClause(EventQueryParams params) {
    List<String> selectCols =
        ListUtils.distinctUnion(
            params.isAggregatedEnrollments() ? List.of("enrollment") : getStandardColumns(params),
            getSelectColumns(params, false));

    // Needs event prefix as we will join with the event table for filtering DataElement of type
    // Org. Unit.
    if (hasEnrollmentOrgUnitFilter(params)) {
      selectCols = selectCols.stream().map(this::addEnrollmentPrefix).toList();
    }

    return "select " + StringUtils.join(selectCols, ",") + " ";
  }

  /**
   * This method switches or add a new prefix to the given column if needed. It takes into
   * consideration columns used in functions as well as regular columns and columns with aliases.
   *
   * <p>ie:
   *
   * <ul>
   *   <li>ax.value as "A03MvHHogjR" -> ax.value as "A03MvHHogjR"
   *   <li>ev.value as "A03MvHHogjR" -> ev.value as "A03MvHHogjR"
   *   <li>value as "A03MvHHogjR" -> ax.value as "A03MvHHogjR"
   *   <li>count() as value -> count() as value"
   *   <li>ST_Y(ax.geometry) -> ST_Y(ax.geometry)
   * </ul>
   *
   * @param column to be prefixed.
   * @return the prefixed column (if required).
   */
  String addEnrollmentPrefix(String column) {
    String functionColumn = substringBetween(column, "(", ")");
    boolean hasFunction = functionColumn != null;
    boolean hasPrefix = column.contains("ax.") || column.contains("ev.");

    if (!hasFunction && !hasPrefix) {
      column = "ax." + column;
    } else if (hasFunction && functionColumn.length() > 0 && !hasPrefix) {
      column = column.replace(functionColumn, "ax." + functionColumn);
    }

    return column;
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
  protected String getColumnWithCte(QueryItem item, CteContext cteContext) {
    Set<String> columns = new LinkedHashSet<>();

    // Get the CTE definition for the item
    CteDefinition cteDef = cteContext.getDefinitionByItemUid(computeKey(item));
    if (cteDef == null) {
      throw new IllegalQueryException(ErrorCode.E7148, item.getItemId());
    }
    int programStageOffset = computeRowNumberOffset(item.getProgramStageOffset());
    // calculate the alias for the column
    // if the item is not a repeatable stage, the alias is the program stage + item name
    String alias =
        getAlias(item).orElse("%s.%s".formatted(item.getProgramStage().getUid(), item.getItemId()));
    columns.add("%s.value as %s".formatted(cteDef.getAlias(programStageOffset), quote(alias)));
    if (cteDef.isRowContext()) {
      // Add additional status and exists columns for row context
      columns.add(
          "coalesce(%s.rn = %s, false) as %s"
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

  /**
   * Constructs the SQL query for the `enrollment_aggr_base` Common Table Expression (CTE).
   *
   * <p>The `enrollment_aggr_base` CTE is a foundational component of an analytical query. It
   * extracts a filtered subset of enrollment data from the target `analytics_enrollment_*` table
   * based on specific criteria.
   *
   * <p>This CTE serves as the "base" dataset for subsequent operations in the query, such as
   * event-level processing and aggregations. By applying these filters early, the CTE ensures that
   * only relevant records are passed to downstream processes, improving query efficiency.
   *
   * <h3>Purpose</h3>
   *
   * The primary purpose of this CTE is to:
   *
   * <ul>
   *   <li>Reduce the size of the dataset by applying restrictive filters.
   *   <li>Serve as a starting point for analytics queries requiring enrollment-specific data.
   *   <li>Facilitate joins with event data while minimizing unnecessary computation.
   * </ul>
   *
   * @param cteContext the {@link CteContext} containing all CTE definitions
   * @param params the {@link EventQueryParams} describing the query parameters
   * @param headers the {@link GridHeader} list defining the query columns
   */
  private void addBaseAggregationCte(
      CteContext cteContext, EventQueryParams params, List<GridHeader> headers) {

    // create base enrollment context
    List<String> columns = new ArrayList<>();

    // Add base column
    addDimensionSelectColumns(columns, params, true);

    SelectBuilder sb = new SelectBuilder();
    sb.addColumn(ENROLLMENT_COL, "ax");
    for (String column : Sets.newHashSet(columns)) {
      sb.addColumn(SqlColumnParser.removeTableAlias(column));
    }

    List<String> programIndicators =
        getProgramIndicators(params).stream().map(QueryItem::getItemId).toList();

    // Add the columns from the headers (only the ones that are not program indicators)
    for (String column : getHeaderColumns(headers, params)) {
      String colToAdd = SqlColumnParser.removeTableAlias(column);
      if (!programIndicators.contains(colToAdd)) {
        sb.addColumnIfNotExist(quote(colToAdd));
      }
    }

    sb.from(getFromClause(params));

    addBaseAggregatedCteJoins(sb, cteContext);

    // Add where clause
    sb.where(
        Condition.and(
            // Add base where condition
            Condition.raw(getWhereClause(params)),
            // Add conditions from filters (remove program stages item to avoid sub-queries in the
            // filter)
            Condition.raw(addFiltersToWhereClause(withoutProgramStageItems(params)))));

    // Extract the columns from the where clause
    List<String> cols = SqlWhereClauseExtractor.extractWhereColumns(sb.build());
    for (String col : cols) {
      sb.addColumnIfNotExist(col);
    }

    // Add the base aggregate CTE along with the original where
    // condition, that have to be propagated in every other CTE for
    // performance reasons
    cteContext.addBaseAggregateCte(
        sb.build(), SqlAliasReplacer.replaceTableAliases(sb.getWhereClause(), cols));
  }

  private void addBaseAggregatedCteJoins(SelectBuilder sb, CteContext cteContext) {
    if (!cteContext.getCteDefinitions().isEmpty()) {
      for (String cteKey : cteContext.getCteKeys()) {
        CteDefinition def = cteContext.getDefinitionByItemUid(cteKey);
        sb.innerJoin(
            cteKey,
            def.getAlias(),
            tableAlias -> tableAlias + ".%s = ax.%s".formatted(ENROLLMENT_COL, ENROLLMENT_COL));
      }
    }
  }

  private String buildAggregatedEnrollmentQueryWithCte(
      List<GridHeader> headers, EventQueryParams params) {

    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);

    // 1. Generate CTE definitions for filters
    generateFilterCTEs(params, cteContext, true);

    // 2. Add base aggregation CTE
    addBaseAggregationCte(cteContext, params, headers);

    // 3. Add CTE definitions for program indicators, program stages, etc.
    getCteDefinitions(params, cteContext);

    // 3. Build up the final SQL using dedicated sub-steps
    SelectBuilder sb = new SelectBuilder();

    // 3.1: Append the WITH clause if needed
    addCteClause(sb, cteContext);

    // 3.2: Append SELECT columns in following order:
    //    1) count(eb.enrollment) as value
    //    2) org unit columns (orgColumns)
    //    3) period columns (periodColumns)
    //    4) header columns (headerColumns)
    sb.addColumn("count(eb.enrollment) as value");
    addOrgUnitAggregateColumns(sb, params);
    addPeriodAggregateColumns(params, sb);
    addHeaderAggregateColumns(headers, cteContext, sb);

    // 3.3: Append the FROM clause (the main enrollment analytics table)
    sb.from(ENROLLMENT_AGGR_BASE, "eb");

    // 3.4: Append JOINs for each relevant CTE definition
    for (String itemUid : cteContext.getCteKeysExcluding(ENROLLMENT_AGGR_BASE)) {
      CteDefinition cteDef = cteContext.getDefinitionByItemUid(itemUid);
      String tableName = useItemUidForTable(cteDef) ? itemUid : cteDef.getAlias();
      sb.leftJoin(
          tableName, cteDef.getAlias(), tableAlias -> tableAlias + ".enrollment = eb.enrollment");
    }

    return sb.build();
  }

  private boolean useItemUidForTable(CteDefinition cteDefinition) {
    return cteDefinition.getCteType() == CteDefinition.CteType.PROGRAM_INDICATOR_ENROLLMENT
        || cteDefinition.getCteType() == CteDefinition.CteType.PROGRAM_INDICATOR_EVENT
        || cteDefinition.getCteType() == CteDefinition.CteType.PROGRAM_STAGE
        || cteDefinition.getCteType() == CteDefinition.CteType.FILTER;
  }

  /**
   * Add the columns specified in the headers to the SelectBuilder. The columns are added in the
   * order specified in the headers and are based on existing CTE definitions.
   *
   * @param headers List of GridHeader objects
   * @param cteContext CteContext object containing all CTE definitions
   * @param sb SelectBuilder object to which the columns are added
   */
  private void addHeaderAggregateColumns(
      List<GridHeader> headers, CteContext cteContext, SelectBuilder sb) {
    // Collect all columns from the headers
    Set<String> headerColumns = getHeaderColumns(headers, "");
    // Collect all CTE definitions for program indicators and program stages
    Map<String, CteDefinition> cteDefinitionMap = collectCteDefinitions(cteContext);

    // Iterate over headerColumns and add the columns to SelectBuilder based on the order specified
    // in the original GridHeader list
    headerColumns.forEach(
        headerColumn -> {
          boolean foundMatch = false;
          String columnWithoutAlias = SqlColumnParser.removeTableAlias(headerColumn);

          // First, check if there's any match in the CTE definitions
          // If there is a match, the column is added with the alias from the CTE definition
          for (Map.Entry<String, CteDefinition> entry : cteDefinitionMap.entrySet()) {
            if (entry.getKey().contains(columnWithoutAlias)) {
              CteDefinition cteDef = entry.getValue();
              sb.addColumn(cteDef.getAlias() + ".value", "", entry.getKey());
              sb.groupBy(entry.getKey());
              foundMatch = true;
              break;
            }
          }

          if (!foundMatch) {
            // Otherwise, add the column as is
            sb.addColumn(quote(columnWithoutAlias));
            sb.groupBy(quote(columnWithoutAlias));
          }
        });
  }

  private Map<String, CteDefinition> collectCteDefinitions(CteContext cteContext) {

    Map<String, CteDefinition> cteDefinitionMap = new HashMap<>();

    // Get all CTE keys excluding ENROLLMENT_AGGR_BASE
    Set<String> cteKeys = cteContext.getCteKeysExcluding(ENROLLMENT_AGGR_BASE);

    for (String key : cteKeys) {
      CteDefinition def = cteContext.getDefinitionByItemUid(key);

      // Only process if it's a program stage or program indicator
      if (def.isProgramStage() || def.isProgramIndicator()) {
        String mapKey =
            quote(
                def.isProgramIndicator()
                    ? def.getProgramIndicatorUid()
                    : def.getProgramStageUid() + "." + def.getItemId());

        cteDefinitionMap.put(mapKey, def);
      }
    }
    return cteDefinitionMap;
  }

  @Override
  protected String getSortClause(EventQueryParams params) {
    if (params.isSorting()) {
      return super.getSortClause(params);
    }
    return "";
  }

  private void addOrgUnitAggregateColumns(SelectBuilder sb, EventQueryParams params) {
    Set<String> orgColumns = getOrgUnitLevelColumns(params);
    if (!orgColumns.isEmpty()) {
      // Add them *exactly in the old order*, then group by them
      for (String orgColumn : orgColumns) {
        sb.addColumn(orgColumn.trim());
        sb.groupBy(orgColumn.trim());
      }
    } else {
      // The old code always ensures we at least include ORGUNIT_DIM_ID if orgColumns is blank
      sb.addColumn(ORGUNIT_DIM_ID);
      sb.groupBy(ORGUNIT_DIM_ID);
    }
  }

  private static void addPeriodAggregateColumns(EventQueryParams params, SelectBuilder sb) {
    Set<String> periodColumns = getPeriodColumns(params);
    if (!periodColumns.isEmpty()) {
      for (String periodColumn : periodColumns) {
        var col = SqlColumnParser.removeTableAlias(periodColumn.trim());
        sb.addColumn(col);
        sb.groupBy(col);
      }
    }
  }

  /**
   * Returns a list of column labels from the given {@link SqlRowSet}. The JDBC-compliant way of
   * getting the alias of a column, is by calling ResultSetMetaData.getColumnLabel().
   *
   * @param rowSet the {@link SqlRowSet} to extract column labels from
   * @return a list of column labels
   */
  private List<String> getColumnLabels(SqlRowSet rowSet) {
    SqlRowSetMetaData metaData = rowSet.getMetaData();
    int columnCount = metaData.getColumnCount();

    List<String> columnLabels = new ArrayList<>();
    for (int i = 1; i <= columnCount; i++) {
      columnLabels.add(metaData.getColumnLabel(i));
    }

    return columnLabels;
  }

  @Override
  void addSelectClause(SelectBuilder sb, EventQueryParams params, CteContext cteContext) {
    if (params.isAggregatedEnrollments()) {
      addAggregatedColumns(sb);
    } else {
      addStandardColumns(sb, params, cteContext);
    }

    // Append columns from CTE definitions
    getSelectColumnsWithCTE(params, cteContext).forEach(sb::addColumn);
  }

  /** Adds aggregated enrollment count column. */
  private void addAggregatedColumns(SelectBuilder sb) {
    sb.addColumn("count(eb.enrollment) as value");
  }

  /** Adds standard columns based on whether shadow CTE is used. */
  private void addStandardColumns(
      SelectBuilder sb, EventQueryParams params, CteContext cteContext) {
    boolean useShadowCte = cteContext.containsCte("top_enrollments");

    if (useShadowCte) {
      addStandardColumnsWithShadowCte(sb, params);
    } else {
      addStandardColumnsWithoutShadowCte(sb, params);
    }
  }

  /** Adds standard columns when using shadow CTE with formula aliases. */
  private void addStandardColumnsWithShadowCte(SelectBuilder sb, EventQueryParams params) {
    Map<String, String> formulaAliases = getFormulaColumnAliases();

    for (String column : getStandardColumns(params)) {
      if (columnIsInFormula(column)) {
        String alias = formulaAliases.getOrDefault(column, createDefaultAlias(column));
        sb.addColumn(alias, "ax");
      } else {
        sb.addColumn(column, "ax");
      }
    }
  }

  /** Adds standard columns without shadow CTE using original logic. */
  private void addStandardColumnsWithoutShadowCte(SelectBuilder sb, EventQueryParams params) {
    getStandardColumns(params)
        .forEach(
            column -> {
              if (columnIsInFormula(column)) {
                sb.addColumn(column);
              } else {
                sb.addColumn(column, "ax");
              }
            });
  }

  /**
   * Returns a list of names of standard columns.
   *
   * @return a list of names of standard columns.
   */
  @Override
  List<String> getStandardColumns(EventQueryParams params) {
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

  @Override
  protected CteContext getCteDefinitions(EventQueryParams params) {
    return getCteDefinitions(params, null);
  }
}
