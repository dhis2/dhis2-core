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
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.AnalyticsConstants.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.common.ColumnHeader.LATITUDE;
import static org.hisp.dhis.analytics.common.ColumnHeader.LONGITUDE;
import static org.hisp.dhis.analytics.common.CteUtils.computeKey;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.analytics.table.ColumnSuffix.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.feedback.ErrorCode.E7131;
import static org.hisp.dhis.feedback.ErrorCode.E7132;
import static org.hisp.dhis.feedback.ErrorCode.E7133;
import static org.hisp.dhis.util.DateUtils.toMediumDate;
import static org.postgresql.util.PSQLState.DIVISION_BY_ZERO;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.OrgUnitFieldType;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.AbstractJdbcTableManager;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.ListBuilder;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * TODO could use row_number() and filtering for paging.
 *
 * <p>TODO introduce dedicated "year" partition column.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service("org.hisp.dhis.analytics.event.EventAnalyticsManager")
public class JdbcEventAnalyticsManager extends AbstractJdbcEventAnalyticsManager
    implements EventAnalyticsManager {
  protected static final String OPEN_IN = " in (";

  private final EventTimeFieldSqlRenderer timeFieldSqlRenderer;

  public JdbcEventAnalyticsManager(
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      ProgramIndicatorService programIndicatorService,
      ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder,
      PiDisagInfoInitializer piDisagInfoInitializer,
      PiDisagQueryGenerator piDisagQueryGenerator,
      EventTimeFieldSqlRenderer timeFieldSqlRenderer,
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
  public Grid getEvents(EventQueryParams params, Grid grid, int maxLimit) {
    String sql =
        useExperimentalAnalyticsQueryEngine()
            ? buildAnalyticsQuery(params)
            : getAggregatedEnrollmentsSql(params, maxLimit);

    if (params.analyzeOnly()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql));
    } else {
      withExceptionHandling(
          () -> getEvents(params, grid, sql, maxLimit == 0), params.isMultipleQueries());
    }

    return grid;
  }

  /**
   * Adds event to the given grid based on the given parameters and SQL statement.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid}.
   * @param sql the SQL statement used to retrieve events.
   */
  private void getEvents(EventQueryParams params, Grid grid, String sql, boolean unlimitedPaging) {
    log.debug("Analytics event query SQL: '{}'", sql);

    SqlRowSet rowSet = queryForRows(sql);

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

      int index = 1;

      for (GridHeader header : grid.getHeaders()) {
        if (LONGITUDE.getItem().equals(header.getName())
            || LATITUDE.getItem().equals(header.getName())) {
          double val = rowSet.getDouble(index);
          grid.addValue(Precision.round(val, COORD_DEC));
        } else {
          addGridValue(grid, header, index, rowSet, params);
        }

        index++;
      }
    }
  }

  @Override
  public Grid getEventClusters(EventQueryParams params, Grid grid, int maxLimit) {
    List<String> clusterFields = params.getCoordinateFields();
    String sqlClusterFields =
        getCoalesce(clusterFields, FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue());

    List<String> columns =
        Lists.newArrayList(
            "count(event) as count", "ST_Extent(" + sqlClusterFields + ") as extent");

    columns.add(
        "case when count(event) = 1 then ST_AsGeoJSON(array_to_string(array_agg("
            + sqlClusterFields
            + "), ','), 6) "
            + "else ST_AsGeoJSON(ST_Centroid(ST_Collect("
            + sqlClusterFields
            + ")), 6) end as center");

    columns.add(
        params.isIncludeClusterPoints()
            ? "array_to_string(array_agg(event), ',') as points"
            : "case when count(event) = 1 then array_to_string(array_agg(event), ',') end as points");

    String sql = "select " + StringUtils.join(columns, ",") + " ";

    sql += getFromClause(params);

    sql += getWhereClause(params);

    sql +=
        "group by ST_SnapToGrid(ST_Transform(ST_SetSRID(ST_Centroid("
            + sqlClusterFields
            + "), 4326), 3785), "
            + params.getClusterSize()
            + ") ";

    log.debug("Analytics event cluster SQL: '{}'", sql);

    SqlRowSet rowSet = queryForRows(sql);

    while (rowSet.next()) {
      grid.addRow()
          .addValue(rowSet.getLong("count"))
          .addValue(rowSet.getString("center"))
          .addValue(rowSet.getString("extent"))
          .addValue(rowSet.getString("points"));
    }

    return grid;
  }

  @Override
  public long getEventCount(EventQueryParams params) {
    String sql = "select count(1) ";

    sql += getFromClause(params);

    sql += getWhereClause(params);

    long count = 0;

    log.debug("Analytics event count SQL: '{}'", sql);

    final String finalSqlValue = sql;

    if (params.analyzeOnly()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(params.getExplainOrderId(), finalSqlValue),
          params.isMultipleQueries());
    } else {
      count =
          withExceptionHandling(() -> jdbcTemplate.queryForObject(finalSqlValue, Long.class))
              .orElse(0L);
    }

    return count;
  }

  @Override
  public Rectangle getRectangle(EventQueryParams params) {
    String coalesceClause =
        getCoalesce(
            params.getCoordinateFields(), FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue());

    String sql =
        "select count(event) as "
            + COL_COUNT
            + ", ST_Extent("
            + coalesceClause
            + ") as "
            + COL_EXTENT
            + " ";

    sql += getFromClause(params);

    sql += getWhereClause(params);

    log.debug("Analytics event count and extent SQL: '{}'", sql);

    Rectangle rectangle = new Rectangle();

    final String finalSqlValue = sql;

    SqlRowSet rowSet = withExceptionHandling(() -> queryForRows(finalSqlValue)).get();

    if (rowSet.next()) {
      Object extent = rowSet.getObject(COL_EXTENT);

      rectangle.setCount(rowSet.getLong(COL_COUNT));
      rectangle.setExtent(extent != null ? String.valueOf(rowSet.getObject(COL_EXTENT)) : null);
    }

    return rectangle;
  }

  private SqlRowSet queryForRows(String sql) {
    try {
      return jdbcTemplate.queryForRowSet(sql);
    } catch (DataAccessResourceFailureException ex) {
      log.warn(E7131.getMessage(), ex);
      throw new QueryRuntimeException(E7131);
    } catch (DataIntegrityViolationException ex) {
      return ExceptionHandler.handle(ex);
    }
  }

  @Override
  protected AnalyticsType getAnalyticsType() {
    return AnalyticsType.EVENT;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Returns a select SQL clause for the given query.
   *
   * @param params the {@link EventQueryParams}.
   */
  @Override
  protected String getSelectClause(EventQueryParams params) {
    List<String> standardColumns = getStandardColumns(params);

    List<String> selectCols =
        ListUtils.distinctUnion(standardColumns, getSelectColumns(params, false));

    return "select " + StringUtils.join(selectCols, ",") + " ";
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

  @Override
  void addFromClause(SelectBuilder sb, EventQueryParams params) {

    // FIXME: use same logic from `getFromClause` method
    sb.from(params.getTableName(), "ax");
  }

  /**
   * Returns a list of names of standard columns.
   *
   * @param params the {@link EventQueryParams}.
   * @return a list of names of standard columns.
   */
  @Override
  List<String> getStandardColumns(EventQueryParams params) {
    ListBuilder<String> columns = new ListBuilder<>();

    columns.add(
        EventAnalyticsColumnName.EVENT_COLUMN_NAME,
        EventAnalyticsColumnName.PS_COLUMN_NAME,
        EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME,
        EventAnalyticsColumnName.STORED_BY_COLUMN_NAME,
        EventAnalyticsColumnName.CREATED_BY_DISPLAYNAME_COLUMN_NAME,
        EventAnalyticsColumnName.LAST_UPDATED_BY_DISPLAYNAME_COLUMN_NAME,
        EventAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME,
        EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);

    if (params.getProgram().isRegistration()) {
      columns.add(
          EventAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME,
          EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME,
          EventAnalyticsColumnName.TRACKED_ENTITY_COLUMN_NAME,
          EventAnalyticsColumnName.ENROLLMENT_COLUMN_NAME);
    }

    if (sqlBuilder.supportsGeospatialData()) {
      columns.add(
          getCoordinateSelectExpression(params),
          EventAnalyticsColumnName.LONGITUDE_COLUMN_NAME,
          EventAnalyticsColumnName.LATITUDE_COLUMN_NAME);
    }

    columns.add(
        EventAnalyticsColumnName.OU_NAME_COLUMN_NAME,
        AbstractJdbcTableManager.OU_NAME_HIERARCHY_COLUMN_NAME,
        EventAnalyticsColumnName.OU_CODE_COLUMN_NAME,
        EventAnalyticsColumnName.ENROLLMENT_STATUS_COLUMN_NAME,
        EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME);

    return columns.build();
  }

  /**
   * Returns a coordinate coalesce select expression.
   *
   * @param params the {@link EventQueryParams}.
   * @return a coordinate coalesce select expression.
   */
  private String getCoordinateSelectExpression(EventQueryParams params) {
    String field =
        getCoalesce(
            params.getCoordinateFields(), FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue());

    return String.format("ST_AsGeoJSON(%s, 6) as geometry", field);
  }

  /**
   * Returns a from SQL clause for the given analytics table partition. If the query has a
   * non-default time field specified, a join with the {@code date period structure} resource table
   * in that field is included.
   *
   * @param params the {@link EventQueryParams}.
   */
  @Override
  protected String getFromClause(EventQueryParams params) {
    String sql = " from ";

    if (params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()) {
      sql += getFirstOrLastValueSubquerySql(params);
    } else {
      sql += params.getTableName();
    }

    sql += " as " + ANALYTICS_TBL_ALIAS + " ";

    if (params.hasTimeField()) {
      String joinCol = quoteAlias(params.getTimeFieldAsField(AnalyticsType.EVENT));
      sql +=
          "left join analytics_rs_dateperiodstructure as "
              + DATE_PERIOD_STRUCT_ALIAS
              + " on cast("
              + joinCol
              + " as date) = "
              + DATE_PERIOD_STRUCT_ALIAS
              + "."
              + quote("dateperiod")
              + " ";
    }

    return sql + joinOrgUnitTables(params, getAnalyticsType());
  }

  private List<DimensionalObject> getPeriods(EventQueryParams params) {
    return params.getDimensions().stream().filter(d -> d.getDimensionType() == PERIOD).toList();
  }

  /**
   * Returns a from and where SQL clause. If this is a program indicator with non-default
   * boundaries, the relationship with the reporting period is specified with where conditions on
   * the enrollment or incident dates. If the default boundaries is used, or the query does not
   * include program indicators, the periods are joined in from the analytics tables the normal way.
   * A where clause can never have a mix of indicators with non-default boundaries and regular
   * analytics table periods.
   *
   * <p>If the query has a non-default time field specified, the query will use the period type
   * columns from the {@code date period structure} resource table through an alias to reflect the
   * period aggregation.
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

    if (!params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()) {
      String timeFieldSql = timeFieldSqlRenderer.renderPeriodTimeFieldSql(params);
      if (StringUtils.isNotBlank(timeFieldSql)) {
        sql += hlp.whereAnd() + " " + timeFieldSql;
      }
    }

    // ---------------------------------------------------------------------
    // Organisation units
    // ---------------------------------------------------------------------

    OrgUnitField orgUnitField = params.getOrgUnitField();

    if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)) {
      String orgUnitCol = orgUnitField.getOrgUnitWhereCol(getAnalyticsType());

      sql +=
          hlp.whereAnd()
              + " "
              + orgUnitCol
              + OPEN_IN
              + sqlBuilder.singleQuotedCommaDelimited(
                  getUids(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID)))
              + ") ";
    } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
      String orgUnitCol = orgUnitField.getOrgUnitWhereCol(getAnalyticsType());

      sql +=
          hlp.whereAnd()
              + " "
              + orgUnitCol
              + OPEN_IN
              + sqlBuilder.singleQuotedCommaDelimited(getUids(params.getOrganisationUnitChildren()))
              + ") ";
    } else // Descendants
    {
      String sqlSnippet =
          getOrgUnitDescendantsClause(
              orgUnitField, params.getDimensionOrFilterItems(ORGUNIT_DIM_ID));

      if (isNotEmpty(sqlSnippet)) {
        sql += hlp.whereAnd() + " " + sqlSnippet;
      }
    }

    // ---------------------------------------------------------------------
    // Categories, category option group sets, organisation unit group sets
    // ---------------------------------------------------------------------

    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(
            Set.of(DimensionType.CATEGORY, DimensionType.CATEGORY_OPTION_GROUP_SET));

    for (DimensionalObject dim : dynamicDimensions) {
      String dimName = dim.getDimensionName();
      String col =
          params.isPiDisagDimension(dimName)
              ? piDisagQueryGenerator.getColumnForWhereClause(params, dimName)
              : quoteAlias(dimName);

      sql +=
          hlp.whereAnd()
              + " "
              + col
              + OPEN_IN
              + sqlBuilder.singleQuotedCommaDelimited(getUids(dim.getItems()))
              + ") ";
    }

    StringBuilder sb = new StringBuilder();
    for (String condition : piDisagQueryGenerator.getCocWhereConditions(params)) {
      sb.append(hlp.whereAnd()).append(condition);
    }
    sql += sb.toString();

    List<DimensionalObject> orgUnitGroupSetDimension =
        params.getDimensionsAndFilters(Set.of(DimensionType.ORGANISATION_UNIT_GROUP_SET));

    for (DimensionalObject dim : orgUnitGroupSetDimension) {
      if (!dim.isAllItems()) {
        String col = quoteAlias(dim.getDimensionName());

        sql +=
            hlp.whereAnd()
                + " "
                + col
                + OPEN_IN
                + sqlBuilder.singleQuotedCommaDelimited(getUids(dim.getItems()))
                + ") ";
      }
    }

    // ---------------------------------------------------------------------
    // Program stage
    // ---------------------------------------------------------------------

    if (params.hasProgramStage()) {
      sql +=
          hlp.whereAnd()
              + " "
              + quoteAlias("ps")
              + " = '"
              + params.getProgramStage().getUid()
              + "' ";
    }

    // ---------------------------------------------------------------------
    // Query items and filters
    // ---------------------------------------------------------------------

    sql += getQueryItemsAndFiltersWhereClause(params, hlp);

    sql += getOptionFilter(params, hlp);

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

      sql += hlp.whereAnd() + " (" + sqlFilter + ") ";
    }

    if (params.hasProgramIndicatorDimension()) {
      String anyValueFilter =
          programIndicatorService.getAnyValueExistsClauseAnalyticsSql(
              params.getProgramIndicator().getExpression(),
              params.getProgramIndicator().getAnalyticsType());

      if (anyValueFilter != null) {
        sql += hlp.whereAnd() + " (" + anyValueFilter + ") ";
      }
    }

    // ---------------------------------------------------------------------
    // Various filters
    // ---------------------------------------------------------------------

    if (params.hasEnrollmentStatuses()) {
      sql +=
          hlp.whereAnd()
              + " enrollmentstatus in ("
              + params.getEnrollmentStatus().stream()
                  .map(p -> singleQuote(p.name()))
                  .collect(joining(","))
              + ") ";
    }

    if (params.hasEventStatus()) {
      sql +=
          hlp.whereAnd()
              + " eventstatus in ("
              + params.getEventStatus().stream()
                  .map(e -> singleQuote(e.name()))
                  .collect(joining(","))
              + ") ";
    }

    if (params.isCoordinatesOnly() || params.isGeometryOnly()) {
      sql +=
          hlp.whereAnd()
              + " "
              + getCoalesce(
                  resolveCoordinateFieldsColumnNames(params.getCoordinateFields(), params),
                  FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue())
              + " is not null ";
    }

    if (params.isCompletedOnly()) {
      sql += hlp.whereAnd() + " completeddate is not null ";
    }

    if (params.hasBbox()) {
      sql +=
          hlp.whereAnd()
              + " "
              + getCoalesce(
                  params.getCoordinateFields(),
                  FallbackCoordinateFieldType.EVENT_GEOMETRY.getValue())
              + " && ST_MakeEnvelope("
              + params.getBbox()
              + ",4326) ";
    }

    // ---------------------------------------------------------------------
    // Partitions restriction to allow constraint exclusion
    // ---------------------------------------------------------------------

    if (!params.isSkipPartitioning()
        && params.hasPartitions()
        && !params.hasTimeDateRanges()
        && !params.hasNonDefaultBoundaries()
        && !params.hasTimeField()
        && !params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()) {
      sql +=
          hlp.whereAnd()
              + " "
              + quoteAlias("yearly")
              + OPEN_IN
              + TextUtils.getQuotedCommaDelimitedString(
                  params.getPartitions().getPartitionsAsString())
              + ") ";
    }

    // ---------------------------------------------------------------------
    // Period rank restriction to get first or last value only
    // ---------------------------------------------------------------------

    if (params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()) {
      sql += hlp.whereAnd() + " " + quoteAlias("pe_rank") + " = 1 ";
    }

    return sql;
  }

  /**
   * Checks if the query requires ownership. This is determined by checking if the {@link
   * OrgUnitFieldType} of the organization unit field is an ownership type.
   *
   * @param params the {@link EventQueryParams} to check.
   * @return true if the query requires ownership, false otherwise.
   */
  private boolean queryRequiresOwnership(EventQueryParams params) {
    return params.getOrgUnitField().getType().isOwnership();
  }

  /**
   * Adds a filtering condition into the "where" statement if an {@Option} is defined.
   *
   * @param params the {@link EventQueryParams}.
   * @param sqlHelper the {@link SqlHelper}.
   * @return the SQL condition for the {@link Option}, if any.
   */
  private String getOptionFilter(EventQueryParams params, SqlHelper sqlHelper) {
    if (params.hasOption() && params.hasValue()) {
      DimensionalItemObject dimensionalItemObject = params.getValue();
      Option option = params.getOption();

      return new StringBuilder()
          .append(SPACE)
          .append(sqlHelper.whereAnd())
          .append(SPACE)
          .append(quote(dimensionalItemObject.getUid()))
          .append(" in ('")
          .append(option.getCode())
          .append("') ")
          .toString();
    }

    return EMPTY;
  }

  /** Generates a sub query which provides a filter by organisation descendant level. */
  private String getOrgUnitDescendantsClause(
      OrgUnitField orgUnitField, List<DimensionalItemObject> dimensionOrFilterItems) {
    Map<String, List<OrganisationUnit>> collect =
        dimensionOrFilterItems.stream()
            .map(object -> (OrganisationUnit) object)
            .collect(
                Collectors.groupingBy(
                    unit ->
                        orgUnitField
                            .withSqlBuilder(sqlBuilder)
                            .getOrgUnitLevelCol(unit.getLevel(), getAnalyticsType())));

    return collect.keySet().stream()
        .map(org -> toInCondition(org, collect.get(org)))
        .collect(joining(" and "));
  }

  /**
   * Generates an in condition.
   *
   * @param orgUnit the org unit identifier.
   * @param organisationUnits the list of {@link OrganisationUnit}.
   * @return a SQL in condition.
   */
  private String toInCondition(String orgUnit, List<OrganisationUnit> organisationUnits) {
    return organisationUnits.stream()
        .filter(unit -> isNotEmpty(unit.getUid()))
        .map(unit -> "'" + unit.getUid() + "'")
        .collect(joining(",", orgUnit + OPEN_IN, ") "));
  }

  /**
   * Generates a sub query which provides a view of the data where each row is ranked by the
   * execution date, ascending or descending. The events are partitioned by org unit and attribute
   * option combo. A column {@code pe_rank} defines the rank. Only data for the last 10 years
   * relative to the period end date is included.
   *
   * @param params the {@link EventQueryParams}.
   */
  private String getFirstOrLastValueSubquerySql(EventQueryParams params) {
    Assert.isTrue(
        params.hasValueDimension() || params.hasProgramIndicatorDimension(),
        "Last value aggregation type query must have value dimension or a program indicator");

    String timeCol = quoteAlias(params.getTimeFieldAsFieldFallback());
    String createdCol = quoteAlias(TimeField.CREATED.getEventColumnName());
    String partitionByClause = getFirstOrLastValuePartitionByClause(params);
    String order =
        params.getAggregationTypeFallback().isFirstPeriodAggregationType() ? "asc" : "desc";

    String columns;
    String timeTest;
    String nullTest;

    if (params.hasProgramIndicatorDimension()) {
      columns = "*," + getProgramIndicatorSql(params) + " as value";
      timeTest = timeFieldSqlRenderer.renderPeriodTimeFieldSql(params);
      nullTest = "";
    } else {
      String valueItem = quoteAlias(params.getValue().getDimensionItem());
      columns =
          quote("event") + "," + valueItem + "," + getFirstOrLastValueSubqueryQuotedColumns(params);

      Date latest = params.getLatestEndDate();
      Date earliest = addYears(latest, LAST_VALUE_YEARS_OFFSET);
      timeTest =
          timeCol
              + " >= '"
              + toMediumDate(earliest)
              + "' "
              + "and "
              + timeCol
              + " <= '"
              + toMediumDate(latest)
              + "'";

      nullTest = " and " + valueItem + " is not null";
    }

    return "(select "
        + columns
        + ",row_number() over ("
        + partitionByClause
        + " "
        + "order by "
        + timeCol
        + " "
        + order
        + ", "
        + createdCol
        + " "
        + order
        + ") as pe_rank "
        + "from "
        + params.getTableName()
        + " as "
        + ANALYTICS_TBL_ALIAS
        + " "
        + "where "
        + timeTest
        + nullTest
        + ")";
  }

  /**
   * Returns the partition by clause for the first or last event sub query. If the aggregation type
   * of the given parameters specifies "first" or "last" as the general aggregation type, the
   * partition by clause will use the dimensions from the analytics query as columns in order to
   * rank events in all dimensions. In this case, the outer query will perform no aggregation and
   * simply filter by the top ranked events.
   *
   * <p>If the aggregation type specifies another aggregation type (i.e. "sum" or "average") as the
   * general aggregation type, the partition by clause will use the "ou" and "ao" columns to rank
   * events in the time dimension only, and have the outer query perform the aggregation in the
   * other dimensions, as well as filter by the top ranked events.
   *
   * @param params the {@link EventQueryParams}.
   * @return the partition by clause.
   */
  private String getFirstOrLastValuePartitionByClause(EventQueryParams params) {
    if (params.isAnyAggregationType(AggregationType.FIRST, AggregationType.LAST)) {
      return getFirstOrLastValuePartitionByColumns(params.getNonPeriodDimensions());
    } else {
      return "partition by " + quoteAliasCommaDelimited(List.of("ou", "ao"));
    }
  }

  /**
   * Returns the partition by clause for the first or last event sub query. The columns to partition
   * by are based on the given list of dimensions.
   *
   * @param dimensions the list of {@link DimensionalObject}.
   * @return the partition by clause.
   */
  private String getFirstOrLastValuePartitionByColumns(List<DimensionalObject> dimensions) {
    String partitionColumns =
        dimensions.stream()
            .map(DimensionalObject::getDimensionName)
            .map(sqlBuilder::quoteAx)
            .collect(Collectors.joining(","));

    String sql = "";

    if (isNotEmpty(partitionColumns)) {
      sql += "partition by " + partitionColumns;
    }

    return sql;
  }

  /**
   * Returns quoted names of columns for the {@link AggregationType#FIRST} or {@link
   * AggregationType#LAST} sub query (not for program indicators).
   *
   * @param params the {@link EventQueryParams}.
   */
  private String getFirstOrLastValueSubqueryQuotedColumns(EventQueryParams params) {
    return params.getDimensionsAndFilters().stream()
        .map(dim -> quote(dim.getDimensionName()))
        .collect(joining(","));
  }

  /** Returns the program indicator SQL from the query parameters. */
  private String getProgramIndicatorSql(EventQueryParams params) {
    return programIndicatorService.getAnalyticsSql(
        params.getProgramIndicator().getExpression(),
        NUMERIC,
        params.getProgramIndicator(),
        params.getEarliestStartDate(),
        params.getLatestEndDate());
  }

  /**
   * If the coordinateField points to an Item of type ORG UNIT, add the "_geom" suffix to the field
   * name.
   */
  private List<String> resolveCoordinateFieldsColumnNames(
      List<String> coordinateFields, EventQueryParams params) {
    List<String> coors = new ArrayList<>(coordinateFields);

    for (int i = 0; i < coors.size(); ++i) {
      for (QueryItem queryItem : params.getItems()) {
        if (queryItem.getItem().getUid().equals(coordinateFields.get(i))
            && queryItem.getValueType() == ValueType.ORGANISATION_UNIT) {
          coors.set(
              i, coors.get(i).replaceAll(coors.get(i), coors.get(i) + OU_GEOMETRY_COL_SUFFIX));
        }
      }
    }

    return coors;
  }

  @Override
  void addSelectClause(SelectBuilder sb, EventQueryParams params, CteContext cteContext) {

    List<String> columns = new ArrayList<>(getStandardColumns(params));
    addDimensionSelectColumns(columns, params, false);
    addEventsItemSelectColumns(columns, params, cteContext);

    columns.forEach(
        column -> {
          if (columnIsInFormula(column) || hasColunnPrefix(column, "ax")) {
            sb.addColumn(column);
          } else {
            sb.addColumn(column, "ax");
          }
        });
    if (cteContext.hasCteDefinitions()) {
      // if there are no CTEs, we can use the standard columns
      getSelectColumnsWithCTE(params, cteContext).forEach(sb::addColumn);
    }
  }

  private void addEventsItemSelectColumns(
      List<String> columns, EventQueryParams params, CteContext cteContext) {
    for (QueryItem queryItem : params.getItems()) {
      ColumnAndAlias columnAndAlias = getColumnAndAlias(queryItem, params, false, false);

      if (columnAndAlias != null && !cteContext.containsCte(columnAndAlias.alias)) {
        columns.add(columnAndAlias.asSql());
      }

      // asked for row context if allowed and needed based on column and its alias
      handleRowContext(columns, params, queryItem, columnAndAlias);
    }
  }

  private boolean hasColunnPrefix(String column, String prefix) {
    return column.startsWith(prefix + ".");
  }

  @Override
  protected CteContext getCteDefinitions(EventQueryParams params) {
    return getCteDefinitions(params, null);
  }

  @Override
  CteContext getCteDefinitions(EventQueryParams params, CteContext cteContext) {
    if (cteContext == null) {
      cteContext = new CteContext(EndpointItem.EVENT);
    }

    for (QueryItem item : params.getItems()) {
      if (item.isProgramIndicator()) {
        ProgramIndicator programIndicator = (ProgramIndicator) item.getItem();
        // Handle any program indicator CTE logic.
        if (programIndicator.getAnalyticsType().equals(AnalyticsType.ENROLLMENT)) {
          // CTE needed only for Enrollment type
          handleProgramIndicatorCte(item, cteContext, params);
        }
      }
    }

    return cteContext;
  }

  protected static class ExceptionHandler {
    private ExceptionHandler() {}

    protected static SqlRowSet handle(DataIntegrityViolationException ex) {
      if (ex != null
          && ex.getCause() instanceof PSQLException
          && DIVISION_BY_ZERO.getState().equals(((PSQLException) ex.getCause()).getSQLState())) {
        log.warn(E7132.getMessage(), ex);
        throw new QueryRuntimeException(E7132);
      } else {
        log.warn(E7133.getMessage(), ex);
        throw new QueryRuntimeException(E7133);
      }
    }
  }
}
