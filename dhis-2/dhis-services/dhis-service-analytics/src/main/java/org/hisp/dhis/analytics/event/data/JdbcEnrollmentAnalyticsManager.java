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

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.AbstractJdbcTableManager;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.util.DateUtils;
import org.locationtech.jts.util.Assert;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

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

  private static final List<String> COLUMNS =
      List.of(
          "ax." + EnrollmentAnalyticsColumnName.ENROLLMENT_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.TRACKED_ENTITY_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.STORED_BY_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.CREATED_BY_DISPLAY_NAME_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_DISPLAY_NAME_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME,
          "ST_AsGeoJSON(" + EnrollmentAnalyticsColumnName.ENROLLMENT_GEOMETRY_COLUMN_NAME + ")",
          EnrollmentAnalyticsColumnName.LONGITUDE_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.LATITUDE_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.OU_NAME_COLUMN_NAME,
          AbstractJdbcTableManager.OU_NAME_HIERARCHY_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.OU_CODE_COLUMN_NAME,
          EnrollmentAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME);

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
      sql = getAggregatedEnrollmentsSql(grid.getHeaders(), params);
    } else if (!params.getItems().isEmpty() && shouldUseCTE(params)) {
      sql = buildEnrollmentQueryWithCTE(params, params.getItems().get(0));
    } else {
      sql = buildEnrollmentQueryWithoutCTE(params);
    }

    System.out.println("SQL: " + sql);

    if (params.analyzeOnly()) {
      withExceptionHandling(
              () -> executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql));
    } else {
      withExceptionHandling(
              () -> getEnrollments(params, grid, sql, maxLimit == 0),
              params.isMultipleQueries());
    }
  }

  private boolean shouldUseCTE(EventQueryParams params) {
    if (params.getItems().isEmpty()) {
      return false;
    }
    QueryItem item = params.getItems().get(0);
    return item.hasProgram() && item.hasProgramStage();
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
   * The method retrieves the amount of the supportive columns in database result set
   *
   * @param rowSet {@link SqlRowSet}.
   * @param columnName The name of the investigated column.
   * @return If the investigated column has some supportive columns lie .exists or .status, the
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
   * Add value meta info into the grid. Value meta info is information about origin of the
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
              .orElse(0l);
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
    // Query items and filters
    // ---------------------------------------------------------------------

    sql += getQueryItemsAndFiltersWhereClause(params, hlp);

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

  @Override
  protected String getSelectClause(EventQueryParams params) {
    List<String> selectCols =
        ListUtils.distinctUnion(
            params.isAggregatedEnrollments() ? List.of("enrollment") : COLUMNS,
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

      if (item.getProgramStage().getRepeatable()
          && item.hasRepeatableStageParams()
          && !item.getRepeatableStageParams().simpleStageValueExpected()) {
        return "(select json_agg(t1) from (select "
            + colName
            + ", "
            + String.join(
                ", ",
                EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME,
                EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME,
                EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME)
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
            + "'"
            + getExecutionDateFilter(
                item.getRepeatableStageParams().getStartDate(),
                item.getRepeatableStageParams().getEndDate())
            + createOrderType(item.getProgramStageOffset())
            + " "
            + createOffset(item.getProgramStageOffset())
            + " "
            + getLimit(item.getRepeatableStageParams().getCount())
            + " ) as t1)";
      }

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

  private String getLimit(int count) {
    if (count == Integer.MAX_VALUE) {
      return "";
    }

    return " LIMIT " + count;
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

  private String getLatestEventsCTE(EventQueryParams params, QueryItem item) {
    String eventTableName = ANALYTICS_EVENT + item.getProgram().getUid().toLowerCase();
    String columnName = quote(item.getItem().getUid());

    return String.format(
            "with LatestEvents as ( " +
                    "select " +
                    "  enrollment, " +
                    "  %s as value, " +
                    "  eventstatus, " +
                    "  row_number() over (partition by enrollment " +
                    "    order by occurreddate desc, created desc) as rn " +
                    "from %s " +
                    "where eventstatus != 'SCHEDULE' " +
                    "  and ps = '%s' " +
                    ") ",
            columnName,
            eventTableName,
            item.getProgramStage().getUid()
    );
  }

  private String buildEnrollmentQueryWithoutCTE(EventQueryParams params) {
    StringBuilder sql = new StringBuilder();

    // 1. Select clause
    sql.append(getSelectClause(params));

    // 2. From clause
    sql.append(" from ").append(params.getTableName()).append(" as ax");

    // 3. Where clause
    List<String> conditions = new ArrayList<>();

    // Add organization unit condition
    if (!params.getOrganisationUnits().isEmpty()) {
      String orgUnit = params.getOrganisationUnits().get(0).getUid();
      conditions.add(String.format("ax.\"uidlevel1\" = '%s'", orgUnit));
    }

    // Use TimeField and DateRange instead of the dimension approach
    if (!params.getTimeDateRanges().isEmpty()) {
      for (Map.Entry<TimeField, List<DateRange>> entry : params.getTimeDateRanges().entrySet()) {
        String column = getColumnForTimeField(entry.getKey());
        if (column != null) {
          List<String> dateConditions = new ArrayList<>();
          for (DateRange range : entry.getValue()) {
            dateConditions.add(String.format(
                    "(%s >= '%s' and %s < '%s')",
                    column, DateUtils.toMediumDate(range.getStartDate()),
                    column, DateUtils.toMediumDate(range.getEndDate())
            ));
          }
          conditions.add("(" + String.join(" or ", dateConditions) + ")");
        }
      }
    }

    if (!conditions.isEmpty()) {
      sql.append(" where ").append(String.join(" and ", conditions));
    }

    // 4. Order by
    if (params.getAsc() != null && !params.getAsc().isEmpty()) {
      sql.append(" order by");
      boolean first = true;
      for (QueryItem item : params.getAsc()) {
        if (!first) {
          sql.append(",");
        }
        String columnName = item.getItemId().equals("incidentdate") ?
                "occurreddate" : item.getItemId();
        sql.append(" ").append(quote(columnName)).append(" asc nulls last");
        first = false;
      }
    }

    // 5. Paging
    sql.append(" ").append(getPagingClause(params, 5000));

    return sql.toString();
  }

  private String getColumnForTimeField(TimeField timeField) {
    switch (timeField) {
      case ENROLLMENT_DATE:
        return "enrollmentdate";
      case INCIDENT_DATE:
        return "occurreddate";
      default:
        return null;
    }
  }

  private String buildEnrollmentQueryWithCTE(EventQueryParams params, QueryItem item) {
    StringBuilder sql = new StringBuilder();

    // 1. Build CTE if needed
    if (item != null && item.hasProgramStage()) {
      sql.append(getLatestEventsCTE(params, item));
    }

    // 2. Get select columns
    String selectClause = getSelectClause(params);
    sql.append(selectClause);

    // 3. From clause
    sql.append(" from ").append(params.getTableName()).append(" as ax");

    // 4. Join clause if needed
    if (item != null && item.hasProgramStage()) {
      int offset = item.getProgramStageOffset();
      sql.append(" left join LatestEvents le on ax.enrollment = le.enrollment")
              .append(" and le.rn = ").append(Math.abs(offset) + 1);
    }

    // 5. Where clause
    String whereClause = getWhereClauseWithCTE(params, item);
    if (!whereClause.isEmpty()) {
      sql.append(" where ").append(whereClause);
    }

    // 6. Order by - ensure proper spacing
    String sortClause = getSortClause(params);
    if (!sortClause.isEmpty()) {
      sql.append(" ").append(sortClause);
    }

    // 7. Limit clause
    sql.append(" ").append(getPagingClause(params, 5000));

    return sql.toString();
  }

  private String buildYearlyDateRangeCondition(String column, String[] years) {
    List<String> yearConditions = new ArrayList<>();
    for (String year : years) {
      yearConditions.add(String.format(
              "(%s >= '%s-01-01' and %s < '%s-02-01')",
              column, year,
              column, year
      ));
    }
    // Note: The entire set of year conditions should be wrapped in parentheses
    return "(" + String.join(" or ", yearConditions) + ")";
  }


  private String getOrderByColumn(String itemId) {
    // Map the column names exactly as they appear in the original query
    switch (itemId.toLowerCase()) {
      case "incidentdate":
        return "occurreddate";  // Note: maps to occurreddate
      case "ouname":
        return "ouname";
      default:
        return itemId;
    }
  }

  private String buildDateRangeCondition(String column, String[] years) {
    List<String> yearConditions = new ArrayList<>();
    for (String year : years) {
      yearConditions.add(String.format(
              "(%s >= '%s-01-01' and %s < '%s-02-01')",
              column, year,
              column, year
      ));
    }
    return String.join(" or ", yearConditions);
  }

  private String getWhereClauseWithoutCTE(EventQueryParams params) {
    List<String> conditions = new ArrayList<>();

    // Add date range conditions
    if (params.getStartDate() != null && params.getEndDate() != null) {
      conditions.add(String.format(
              "enrollmentdate >= '%s' and enrollmentdate < '%s'",
              DateUtils.toMediumDate(params.getStartDate()),
              DateUtils.toMediumDate(params.getEndDate())
      ));
    }

    // Add organization unit conditions
    if (!params.getOrganisationUnits().isEmpty()) {
      String orgUnit = params.getOrganisationUnits().get(0).getUid();
      conditions.add(String.format("ax.uidlevel1 = '%s'", orgUnit));
    }

    return String.join(" and ", conditions);
  }

  private String getWhereClauseWithCTE(EventQueryParams params, QueryItem item) {
    List<String> conditions = new ArrayList<>();

    // Add organization unit conditions
    if (!params.getOrganisationUnits().isEmpty()) {
      String orgUnit = params.getOrganisationUnits().get(0).getUid();
      conditions.add(String.format("ax.\"uidlevel1\" = '%s'", orgUnit));
    }

    // Handle date ranges using TimeField and DateRange
    if (!params.getTimeDateRanges().isEmpty()) {
      for (Map.Entry<TimeField, List<DateRange>> entry : params.getTimeDateRanges().entrySet()) {
        String column = getColumnForTimeField(entry.getKey());
        if (column != null) {
          List<String> dateConditions = new ArrayList<>();
          for (DateRange range : entry.getValue()) {
            dateConditions.add(String.format(
                    "(%s >= '%s' and %s < '%s')",
                    "ax." + column, DateUtils.toMediumDate(range.getStartDate()),
                    "ax." + column, DateUtils.toMediumDate(range.getEndDate())
            ));
          }
          conditions.add("(" + String.join(" or ", dateConditions) + ")");
        }
      }
    }

    // Handle NV (null value) filter
    if (hasNullValueFilter(params)) {
      conditions.add("le.value is null");
      conditions.add("le.enrollment is not null");
    }

    // Join conditions with AND
    return conditions.isEmpty() ? "" : conditions.stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" and "));
  }


  private boolean hasNullValueFilter(EventQueryParams params) {
    return params.getItems().stream()
            .anyMatch(item -> item.hasFilter() &&
                    item.getFilters().stream()
                            .anyMatch(filter -> "NV".equals(filter.getFilter())));
  }

  protected String getSortClause(EventQueryParams params) {
    if (params.isSorting()) {
      return super.getSortClause(params);
    }
    return "";
  }

  protected String getQueryItemsAndFiltersWhereClause(EventQueryParams params, SqlHelper hlp) {
    if (params.isEnhancedCondition()) {
      return getItemsSqlForEnhancedConditions(params, hlp);
    }

    List<String> conditions = new ArrayList<>();

    // Handle item filters using CTE
    for (QueryItem item : params.getItems()) {
      if (item.hasFilter()) {
        conditions.addAll(getItemConditions(item));
      }
    }

    // Handle item filters
    for (QueryItem item : params.getItemFilters()) {
      if (item.hasFilter()) {
        conditions.addAll(getItemConditions(item));
      }
    }

    return conditions.isEmpty() ? "" : String.join(" and ", conditions);
  }

  private List<String> getItemConditions(QueryItem item) {
    List<String> conditions = new ArrayList<>();

    for (QueryFilter filter : item.getFilters()) {
      if ("NV".equals(filter.getFilter())) {
        // Special handling for null values
        conditions.add("le.value is null");
      } else {
        String operator = filter.getSqlOperator();
        if (operator.equals("in")) {
          // Handle IN clause differently based on value type
          String value = getSqlFilter(filter, item);
          if (item.isNumeric()) {
            conditions.add("le.value in (" + value + ")");
          } else {
            // For text values, keep the quotes
            conditions.add("le.value in (" + value + ")");
          }
        } else {
          String value = getSqlFilter(filter, item);
          conditions.add(String.format("le.value %s %s",
                  operator,
                  value));
        }
      }
    }

    return conditions;
  }

  protected String getSqlFilter(QueryFilter filter, QueryItem item) {
    String value = filter.getFilter();

    if ("NV".equals(value)) {
      return "null";
    }

    if (item.isNumeric()) {
      return value; // Don't quote numeric values
    } else {
      return sqlBuilder.singleQuote(value); // Quote text values
    }
  }
}
