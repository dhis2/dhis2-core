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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.encode;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.getCoalesce;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.removeLastOr;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.util.DateUtils;
import org.locationtech.jts.util.Assert;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
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

  private static final String ORDER_BY_EXECUTION_DATE = "order by executiondate ";

  private static final String LIMIT_1 = "limit 1";

  private static final String IS_NOT_NULL = " is not null ";

  private static final List<String> COLUMNS =
      List.of(
          "pi",
          "tei",
          "enrollmentdate",
          "incidentdate",
          "storedby",
          "createdbydisplayname",
          "lastupdatedbydisplayname",
          "lastupdated",
          "ST_AsGeoJSON(pigeometry)",
          "longitude",
          "latitude",
          "ouname",
          "ounamehierarchy",
          "oucode",
          "enrollmentstatus");

  public JdbcEnrollmentAnalyticsManager(
      JdbcTemplate jdbcTemplate,
      ProgramIndicatorService programIndicatorService,
      ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder,
      EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer,
      ExecutionPlanStore executionPlanStore) {
    super(
        jdbcTemplate, programIndicatorService, programIndicatorSubqueryBuilder, executionPlanStore);
    this.timeFieldSqlRenderer = timeFieldSqlRenderer;
  }

  @Override
  public void getEnrollments(EventQueryParams params, Grid grid, int maxLimit) {
    String sql = getEventsOrEnrollmentsSql(params, maxLimit);

    if (params.analyzeOnly()) {
      executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql);
    } else {
      withExceptionHandling(() -> getEnrollments(params, grid, sql, maxLimit == 0));
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
    log.debug(String.format("Analytics enrollment query SQL: %s", sql));

    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

    int rowsRed = 0;

    grid.setLastDataRow(true);

    while (rowSet.next()) {
      if (++rowsRed > params.getPageSizeWithDefault()
          && !params.isTotalPages()
          && !unlimitedPaging) {
        grid.setLastDataRow(false);

        continue;
      }

      grid.addRow();

      // columnOffset is synchronization aid for <<grid headers>> and <<rowSet columns>> indexes.
      // The amount of headers must not match to amount of columns due the additional ones
      // describing the repeating of repeatable stage.
      int columnOffset = 0;

      for (int i = 0; i < grid.getHeaders().size(); ++i) {
        addGridValue(grid, grid.getHeaders().get(i), i + 1 + columnOffset, rowSet, params);

        if (params.isRowContext()
            && addValueMetaInfo(grid, rowSet, grid.getHeaders().get(i).getName())) {
          ++columnOffset;
        }
      }
    }
  }

  /**
   * Add value meta info into the grid. Value meta info is information about origin of the
   * repeatable stage value.
   *
   * @param grid the {@link Grid}.
   * @param rowSet the {@link SqlRowSet}.
   * @param columnName the {@link String}.
   * @return true when ValueMetaInfo added
   */
  private boolean addValueMetaInfo(Grid grid, SqlRowSet rowSet, String columnName) {
    int gridRowIndex = grid.getRows().size() - 1;

    Optional<String> valueMetaInfoColumnName =
        Arrays.stream(rowSet.getMetaData().getColumnNames())
            .filter((columnName + ".exists")::equalsIgnoreCase)
            .findFirst();

    if (valueMetaInfoColumnName.isPresent()) {
      try {
        boolean isDefined = rowSet.getBoolean(valueMetaInfoColumnName.get());

        boolean isSet = rowSet.getObject(columnName) != null;

        ValueStatus valueStatus = ValueStatus.of(isDefined, isSet);

        if (valueStatus != ValueStatus.NOT_DEFINED) {
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

    try {
      log.debug("Analytics enrollment count SQL: " + sql);

      if (params.analyzeOnly()) {
        executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql);
      } else {
        count = jdbcTemplate.queryForObject(sql, Long.class);
      }
    } catch (BadSqlGrammarException ex) {
      log.info(AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING, ex);
    } catch (DataAccessResourceFailureException ex) {
      log.warn(ErrorCode.E7131.getMessage(), ex);
      throw new QueryRuntimeException(ErrorCode.E7131);
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
    // Organisation unit group sets
    // ---------------------------------------------------------------------

    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.CATEGORY));

    for (DimensionalObject dim : dynamicDimensions) {
      String col = quoteAlias(dim.getDimensionName());

      sql += "and " + col + " in (" + getQuotedCommaDelimitedString(getUids(dim.getItems())) + ") ";
    }

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

    if (params.hasProgramStatus()) {
      sql +=
          "and enrollmentstatus in ("
              + params.getProgramStatus().stream().map(p -> encode(p.name())).collect(joining(","))
              + ") ";
    }

    if (params.isCoordinatesOnly()) {
      sql += "and (longitude is not null and latitude is not null) ";
    }

    if (params.isGeometryOnly()) {
      sql +=
          "and "
              + getCoalesce(
                  params.getCoordinateFields(), FallbackCoordinateFieldType.PI_GEOMETRY.getValue())
              + IS_NOT_NULL;
    }

    if (params.isCompletedOnly()) {
      sql += "and completeddate is not null ";
    }

    if (params.hasBbox()) {
      sql +=
          "and "
              + getCoalesce(
                  params.getCoordinateFields(), FallbackCoordinateFieldType.PI_GEOMETRY.getValue())
              + " && ST_MakeEnvelope("
              + params.getBbox()
              + ",4326) ";
    }

    return sql;
  }

  @Override
  protected String getSelectClause(EventQueryParams params) {
    List<String> selectCols = ListUtils.distinctUnion(COLUMNS, getSelectColumns(params, false));

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
              + ".pi = "
              + ANALYTICS_TBL_ALIAS
              + ".pi "
              + "and "
              + colName
              + IS_NOT_NULL
              + psCondition
              + ORDER_BY_EXECUTION_DATE
              + createOrderTypeAndOffset(item.getProgramStageOffset())
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

      if (item.getProgramStage().getRepeatable()
          && item.hasRepeatableStageParams()
          && !item.getRepeatableStageParams().simpleStageValueExpected()) {
        return "(select json_agg(t1) from (select "
            + colName
            + ", incidentdate, duedate, executiondate "
            + " from "
            + eventTableName
            + " where "
            + eventTableName
            + ".pi = "
            + ANALYTICS_TBL_ALIAS
            + ".pi "
            + "and ps = '"
            + item.getProgramStage().getUid()
            + "'"
            + getExecutionDateFilter(
                item.getRepeatableStageParams().getStartDate(),
                item.getRepeatableStageParams().getEndDate())
            + ORDER_BY_EXECUTION_DATE
            + createOrderTypeAndOffset(item.getProgramStageOffset())
            + getLimit(item.getRepeatableStageParams().getCount())
            + " ) as t1)";
      }

      if (item.getProgramStage().getRepeatable() && item.hasRepeatableStageParams()) {
        return "(select "
            + colName
            + " from "
            + eventTableName
            + " where "
            + eventTableName
            + ".pi = "
            + ANALYTICS_TBL_ALIAS
            + ".pi "
            + "and ps = '"
            + item.getProgramStage().getUid()
            + "' "
            + getExecutionDateFilter(
                item.getRepeatableStageParams().getStartDate(),
                item.getRepeatableStageParams().getEndDate())
            + ORDER_BY_EXECUTION_DATE
            + createOrderTypeAndOffset(item.getProgramStageOffset())
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
          + eventTableName
          + ".pi = "
          + ANALYTICS_TBL_ALIAS
          + ".pi "
          + "and "
          + colName
          + IS_NOT_NULL
          + "and ps = '"
          + item.getProgramStage().getUid()
          + "' "
          + ORDER_BY_EXECUTION_DATE
          + createOrderTypeAndOffset(item.getProgramStageOffset())
          + " "
          + LIMIT_1
          + " )";
    } else {
      return quoteAlias(colName);
    }
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

  private static String getExecutionDateFilter(Date startDate, Date endDate) {
    StringBuilder sb = new StringBuilder();

    if (startDate != null) {
      sb.append(" and executiondate >= ");

      sb.append(
          String.format("%s ", SqlUtils.singleQuote(DateUtils.getMediumDateString(startDate))));
    }

    if (endDate != null) {
      sb.append(" and executiondate <= ");

      sb.append(String.format("%s ", SqlUtils.singleQuote(DateUtils.getMediumDateString(endDate))));
    }

    return sb.toString();
  }

  private static String getLimit(int count) {
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

  private String createOrderTypeAndOffset(int offset) {
    if (offset == 0) {
      return "desc";
    }
    if (offset < 0) {
      return "desc offset " + (-1 * offset);
    } else {
      return "asc offset " + (offset - 1);
    }
  }
}
