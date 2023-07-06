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
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_LATITUDE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_LONGITUDE;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.OU_GEOMETRY_COL_SUFFIX;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.ORG_UNIT_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.encode;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAlias;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quoteAliasCommaSeparate;
import static org.hisp.dhis.common.AnalyticsDateFilter.SCHEDULED_DATE;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.feedback.ErrorCode.E7131;
import static org.hisp.dhis.feedback.ErrorCode.E7132;
import static org.hisp.dhis.feedback.ErrorCode.E7133;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.postgresql.util.PSQLState.DIVISION_BY_ZERO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.util.AnalyticsSqlUtils;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * TODO could use row_number() and filtering for paging. TODO introduce dedicated "year" partition
 * column.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component("org.hisp.dhis.analytics.event.EventAnalyticsManager")
public class JdbcEventAnalyticsManager extends AbstractJdbcEventAnalyticsManager
    implements EventAnalyticsManager {
  protected static final String OPEN_IN = " in (";

  private static final String ORG_UNIT_UID_LEVEL_COLUMN_PREFIX = "uidlevel";

  private final EventTimeFieldSqlRenderer timeFieldSqlRenderer;

  public JdbcEventAnalyticsManager(
      JdbcTemplate jdbcTemplate,
      StatementBuilder statementBuilder,
      ProgramIndicatorService programIndicatorService,
      ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder,
      EventTimeFieldSqlRenderer timeFieldSqlRenderer,
      ExecutionPlanStore executionPlanStore) {
    super(
        jdbcTemplate,
        statementBuilder,
        programIndicatorService,
        programIndicatorSubqueryBuilder,
        executionPlanStore);
    this.timeFieldSqlRenderer = timeFieldSqlRenderer;
  }

  @Override
  public Grid getEvents(EventQueryParams params, Grid grid, int maxLimit) {
    String sql = getEventsOrEnrollmentsSql(params, maxLimit);

    if (params.analyzeOnly()) {
      executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql);
    } else {
      withExceptionHandling(() -> getEvents(params, grid, sql, maxLimit == 0));
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
      if (++rowsRed > params.getPageSizeWithDefault()
          && !params.isTotalPages()
          && !unlimitedPaging) {
        grid.setLastDataRow(false);

        continue;
      }

      grid.addRow();

      int index = 1;

      for (GridHeader header : grid.getHeaders()) {
        if (ITEM_LONGITUDE.equals(header.getName()) || ITEM_LATITUDE.equals(header.getName())) {
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
    String clusterField = params.getCoordinateField();
    String quotedClusterField = quoteAlias(clusterField);

    List<String> columns =
        Lists.newArrayList(
            "count(psi) as count", "ST_Extent(" + quotedClusterField + ") as extent");

    columns.add(
        "case when count(psi) = 1 then ST_AsGeoJSON(array_to_string(array_agg("
            + quotedClusterField
            + "), ','), 6) "
            + "else ST_AsGeoJSON(ST_Centroid(ST_Collect("
            + quotedClusterField
            + ")), 6) end as center");

    columns.add(
        params.isIncludeClusterPoints()
            ? "array_to_string(array_agg(psi), ',') as points"
            : "case when count(psi) = 1 then array_to_string(array_agg(psi), ',') end as points");

    String sql = "select " + StringUtils.join(columns, ",") + " ";

    sql += getFromClause(params);

    sql += getWhereClause(params);

    sql +=
        "group by ST_SnapToGrid(ST_Transform(ST_SetSRID(ST_Centroid("
            + quotedClusterField
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

    try {
      log.debug("Analytics event count SQL: '{}'", sql);

      if (params.analyzeOnly()) {
        executionPlanStore.addExecutionPlan(params.getExplainOrderId(), sql);
      } else {
        count = jdbcTemplate.queryForObject(sql, Long.class);
      }
    } catch (BadSqlGrammarException ex) {
      log.info(AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING, ex);
    } catch (DataAccessResourceFailureException ex) {
      log.warn(E7131.getMessage(), ex);
      throw new QueryRuntimeException(E7131, ex);
    } catch (DataIntegrityViolationException ex) {
      log.warn(E7132.getMessage(), ex);
      throw new QueryRuntimeException(E7132, ex);
    }

    return count;
  }

  @Override
  public Rectangle getRectangle(EventQueryParams params) {
    String fallback = params.getFallbackCoordinateField();
    String quotedClusterFieldFraction;
    if (fallback == null || !params.isCoordinateOuFallback()) {
      quotedClusterFieldFraction = quoteAlias(params.getCoordinateField());
    } else {
      quotedClusterFieldFraction =
          "coalesce(" + quoteAlias(params.getCoordinateField()) + "," + quoteAlias(fallback) + ")";
    }

    String sql =
        "select count(psi) as "
            + COL_COUNT
            + ", ST_Extent("
            + quotedClusterFieldFraction
            + ") as "
            + COL_EXTENT
            + " ";

    sql += getFromClause(params);

    sql += getWhereClause(params);

    log.debug("Analytics event count and extent SQL: '{}'", sql);

    Rectangle rectangle = new Rectangle();

    SqlRowSet rowSet = queryForRows(sql);

    if (rowSet.next()) {
      Object extent = rowSet.getObject(COL_EXTENT);

      rectangle.setCount(rowSet.getLong(COL_COUNT));
      rectangle.setExtent(extent != null ? String.valueOf(rowSet.getObject(COL_EXTENT)) : null);
    }

    return rectangle;
  }

  private SqlRowSet queryForRows(final String sql) {
    try {
      return jdbcTemplate.queryForRowSet(sql);
    } catch (DataAccessResourceFailureException ex) {
      log.warn(E7131.getMessage(), ex);
      throw new QueryRuntimeException(E7131, ex);
    } catch (DataIntegrityViolationException ex) {
      return ExceptionHandler.handle(ex);
    }
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
    ImmutableList.Builder<String> cols =
        new ImmutableList.Builder<String>()
            .add(
                "psi",
                "ps",
                "executiondate",
                "storedby",
                "createdbydisplayname",
                "lastupdatedbydisplayname",
                "lastupdated");

    if (params.containsScheduledDatePeriod()) {
      cols.add(SCHEDULED_DATE.getTimeField().getField());
    }

    if (params.getProgram().isRegistration()) {
      cols.add("enrollmentdate", "incidentdate", "tei", "pi");
    }

    cols.add(
        "ST_AsGeoJSON(psigeometry, 6) as geometry",
        "longitude",
        "latitude",
        "ouname",
        "oucode",
        "pistatus",
        "psistatus");

    List<String> selectCols =
        ListUtils.distinctUnion(cols.build(), getSelectColumns(params, false));

    return "select " + StringUtils.join(selectCols, ",") + " ";
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
      String joinCol = quoteAlias(params.getTimeFieldAsField());
      sql +=
          "left join _dateperiodstructure as "
              + DATE_PERIOD_STRUCT_ALIAS
              + " on cast("
              + joinCol
              + " as date) = "
              + DATE_PERIOD_STRUCT_ALIAS
              + "."
              + quote("dateperiod")
              + " ";
    }

    if (params.hasOrgUnitField()) {
      String joinCol = quoteAlias(params.getOrgUnitField());
      sql +=
          "left join _orgunitstructure as "
              + ORG_UNIT_STRUCT_ALIAS
              + " on "
              + joinCol
              + " = "
              + ORG_UNIT_STRUCT_ALIAS
              + "."
              + quote("organisationunituid")
              + " ";
    }

    return sql;
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

    sql += hlp.whereAnd() + " " + timeFieldSqlRenderer.renderTimeFieldSql(params);

    // ---------------------------------------------------------------------
    // Organisation units
    // ---------------------------------------------------------------------

    if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)) {
      String orgUnitCol = quoteAlias(params.getOrgUnitFieldFallback());

      sql +=
          hlp.whereAnd()
              + " "
              + orgUnitCol
              + OPEN_IN
              + getQuotedCommaDelimitedString(
                  getUids(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID)))
              + ") ";
    } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
      String orgUnitCol = quoteAlias(params.getOrgUnitFieldFallback());

      sql +=
          hlp.whereAnd()
              + " "
              + orgUnitCol
              + OPEN_IN
              + getQuotedCommaDelimitedString(getUids(params.getOrganisationUnitChildren()))
              + ") ";
    } else // Descendants
    {
      String orgUnitAlias = getOrgUnitAlias(params);

      String sqlSnippet =
          getOrgDescendantsSqlSnippet(
              orgUnitAlias, params.getDimensionOrFilterItems(ORGUNIT_DIM_ID));

      if (sqlSnippet != null && !sqlSnippet.trim().isEmpty()) {
        sql += hlp.whereAnd() + " " + sqlSnippet;
      }
    }

    // ---------------------------------------------------------------------
    // Organisation unit group sets
    // ---------------------------------------------------------------------

    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(
            Sets.newHashSet(DimensionType.CATEGORY, DimensionType.CATEGORY_OPTION_GROUP_SET));

    for (DimensionalObject dim : dynamicDimensions) {
      String col = quoteAlias(dim.getDimensionName());

      sql +=
          hlp.whereAnd()
              + " "
              + col
              + OPEN_IN
              + getQuotedCommaDelimitedString(getUids(dim.getItems()))
              + ") ";
    }

    dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.ORGANISATION_UNIT_GROUP_SET));

    for (DimensionalObject dim : dynamicDimensions) {
      if (!dim.isAllItems()) {
        String col = quoteAlias(dim.getDimensionName());

        sql +=
            hlp.whereAnd()
                + " "
                + col
                + OPEN_IN
                + getQuotedCommaDelimitedString(getUids(dim.getItems()))
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

    sql += getStatementForDimensionsAndFilters(params, hlp);

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

    if (params.hasProgramStatus()) {
      sql +=
          hlp.whereAnd()
              + " pistatus in ("
              + params.getProgramStatus().stream()
                  .map(p -> encode(p.name(), true))
                  .collect(joining(","))
              + ") ";
    }

    if (params.hasEventStatus()) {
      sql +=
          hlp.whereAnd()
              + " psistatus in ("
              + params.getEventStatus().stream()
                  .map(e -> encode(e.name(), true))
                  .collect(joining(","))
              + ") ";
    }

    if (params.isCoordinatesOnly() || params.isGeometryOnly()) {
      if (params.isCoordinateOuFallback()) {
        sql +=
            hlp.whereAnd()
                + " ("
                + quoteAlias(resolveCoordinateFieldColumnName(params.getCoordinateField(), params))
                + " is not null or "
                + quoteAlias(
                    resolveCoordinateFieldColumnName(params.getFallbackCoordinateField(), params))
                + " is not null )";
      } else {
        sql +=
            hlp.whereAnd()
                + " "
                + quoteAlias(resolveCoordinateFieldColumnName(params.getCoordinateField(), params))
                + " is not null ";
      }
    }

    if (params.isCompletedOnly()) {
      sql += hlp.whereAnd() + " completeddate is not null ";
    }

    if (params.hasBbox()) {
      sql +=
          hlp.whereAnd()
              + " "
              + quoteAlias(params.getCoordinateField())
              + " && ST_MakeEnvelope("
              + params.getBbox()
              + ",4326) ";
    }

    // ---------------------------------------------------------------------
    // Partitions restriction to allow constraint exclusion
    // ---------------------------------------------------------------------

    if (!params.isSkipPartitioning()
        && params.hasPartitions()
        && !params.hasNonDefaultBoundaries()
        && !params.hasTimeField()
        && !params.getAggregationTypeFallback().isFirstOrLastPeriodAggregationType()) {
      sql +=
          hlp.whereAnd()
              + " "
              + quoteAlias("yearly")
              + OPEN_IN
              + TextUtils.getQuotedCommaDelimitedString(params.getPartitions().getPartitions())
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

  /** Generates a sub query which provides a filter by organisation - descendant level */
  private String getOrgDescendantsSqlSnippet(
      String orgUnitAlias, List<DimensionalItemObject> dimensionOrFilterItems) {
    Map<String, List<OrganisationUnit>> collect =
        dimensionOrFilterItems.stream()
            .map(object -> (OrganisationUnit) object)
            .collect(
                Collectors.groupingBy(
                    unit ->
                        quote(orgUnitAlias, ORG_UNIT_UID_LEVEL_COLUMN_PREFIX + unit.getLevel())));

    return collect.keySet().stream()
        .map(org -> toInCondition(org, collect.get(org)))
        .collect(joining(" and "));
  }

  private String toInCondition(String org, List<OrganisationUnit> organisationUnits) {
    return organisationUnits.stream()
        .filter(unit -> unit.getUid() != null && !unit.getUid().trim().isEmpty())
        .map(unit -> "'" + unit.getUid() + "'")
        .collect(joining(",", org + OPEN_IN, ") "));
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
    String createdCol = quoteAlias(TimeField.CREATED.getField());
    String partitionByClause = getFirstOrLastValuePartitionByClause(params);
    String order =
        params.getAggregationTypeFallback().isFirstPeriodAggregationType() ? "asc" : "desc";

    String columns;
    String timeTest;
    String nullTest;

    if (params.hasProgramIndicatorDimension()) {
      columns = "*," + getProgramIndicatorSql(params) + " as value";
      timeTest = timeFieldSqlRenderer.renderTimeFieldSql(params);
      nullTest = "";
    } else {
      String valueItem = quoteAlias(params.getValue().getDimensionItem());
      columns =
          quote("psi") + "," + valueItem + "," + getFirstOrLastValueSubqueryQuotedColumns(params);

      Date latest = params.getLatestEndDate();
      Date earliest = addYears(latest, LAST_VALUE_YEARS_OFFSET);
      timeTest =
          timeCol
              + " >= '"
              + getMediumDateString(earliest)
              + "' "
              + "and "
              + timeCol
              + " <= '"
              + getMediumDateString(latest)
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
      return "partition by " + quoteAliasCommaSeparate(List.of("ou", "ao"));
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
            .map(AnalyticsSqlUtils::quoteAlias)
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

  @Override
  protected AnalyticsType getAnalyticsType() {
    return AnalyticsType.EVENT;
  }

  /**
   * If the coordinateField points to an Item of type ORG UNIT, add the "_geom" suffix to the field
   * name.
   */
  private String resolveCoordinateFieldColumnName(String coordinateField, EventQueryParams params) {
    for (QueryItem queryItem : params.getItems()) {
      if (queryItem.getItem().getUid().equals(coordinateField)
          && queryItem.getValueType() == ValueType.ORGANISATION_UNIT) {
        return coordinateField + OU_GEOMETRY_COL_SUFFIX;
      }
    }
    return coordinateField;
  }

  protected static class ExceptionHandler {
    private ExceptionHandler() {}

    protected static SqlRowSet handle(final DataIntegrityViolationException ex) {
      if (ex != null
          && ex.getCause() instanceof PSQLException
          && DIVISION_BY_ZERO.getState().equals(((PSQLException) ex.getCause()).getSQLState())) {
        log.warn(E7132.getMessage(), ex);
        throw new QueryRuntimeException(E7132, ex);
      } else {
        log.warn(E7133.getMessage(), ex);
        throw new QueryRuntimeException(E7133, ex);
      }
    }
  }
}
