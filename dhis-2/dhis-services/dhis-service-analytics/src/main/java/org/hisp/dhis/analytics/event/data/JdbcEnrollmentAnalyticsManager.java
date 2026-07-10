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
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.common.CteDefinition.ENROLLMENT_AGGR_BASE;
import static org.hisp.dhis.analytics.event.data.EnrollmentQueryHelper.getHeaderColumns;
import static org.hisp.dhis.analytics.event.data.OrgUnitTableJoiner.joinOrgUnitTables;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.analytics.util.EventQueryParamsUtils.getProgramIndicators;
import static org.hisp.dhis.analytics.util.EventQueryParamsUtils.withoutProgramStageItems;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.aggregate.AggregatedEnrollmentDateHeaderResolver;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.event.data.stage.StageHeaderClassifier;
import org.hisp.dhis.analytics.event.data.stage.StageQuerySqlFacade;
import org.hisp.dhis.analytics.table.AbstractJdbcTableManager;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.analytics.util.sql.Condition;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.analytics.util.sql.SqlAliasReplacer;
import org.hisp.dhis.analytics.util.sql.SqlColumnParser;
import org.hisp.dhis.analytics.util.sql.SqlWhereClauseExtractor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.util.AnalyticsTableNames;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.ListBuilder;
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
  private final EnrollmentEventSubqueryBuilder eventSubqueryBuilder;
  private final AggregatedEnrollmentQueryAssembler aggregatedAssembler;
  private final StageHeaderClassifier stageHeaderClassifier = new StageHeaderClassifier();
  private final AggregatedEnrollmentDateHeaderResolver dateHeaderResolver =
      new AggregatedEnrollmentDateHeaderResolver();

  private static final String IS_NOT_NULL = " is not null ";

  private static final String ENROLLMENT_COL = "enrollment";
  private static final Set<TimeField> EVENT_TIME_FIELDS =
      EnumSet.of(TimeField.EVENT_DATE, TimeField.SCHEDULED_DATE);

  private static final String ENROLLMENT_AGGR_BASE_ALIAS = "eb";

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
      ColumnMapper columnMapper,
      QueryItemFilterBuilder filterBuilder,
      StageQuerySqlFacade stageQuerySqlFacade,
      DateFieldPeriodBucketColumnResolver dateFieldPeriodBucketColumnResolver,
      EnrollmentEventSubqueryBuilder eventSubqueryBuilder,
      AggregatedEnrollmentQueryAssembler aggregatedAssembler) {
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
        columnMapper,
        filterBuilder,
        stageQuerySqlFacade,
        dateFieldPeriodBucketColumnResolver);
    this.timeFieldSqlRenderer = timeFieldSqlRenderer;
    this.eventSubqueryBuilder = eventSubqueryBuilder;
    this.aggregatedAssembler = aggregatedAssembler;
  }

  private enum AggregateEnrollmentHeaderType {
    VALUE,
    ORG_UNIT,
    PERIOD,
    OTHER
  }

  @Override
  public void getEnrollments(EventQueryParams params, Grid grid, int maxLimit) {
    String sql;
    if (params.isAggregatedEnrollments()) {
      sql = buildAggregatedEnrollmentQueryWithCte(grid.getHeaders(), params);
    } else {
      sql = buildAnalyticsQuery(params, maxLimit);
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
    Optional<ProgramIndicatorCteSql> programIndicatorCte = getProgramIndicatorCteSql(params);
    String sql = "select count(pi) ";

    sql += getFromClause(params);
    sql += programIndicatorCte.map(ProgramIndicatorCteSql::joinClause).orElse("");

    sql += getWhereClause(params);
    sql += addFiltersToWhereClause(params);

    if (programIndicatorCte.isPresent()) {
      sql = programIndicatorCte.get().withClause() + sql;
    }

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

  private String addFiltersToWhereClause(EventQueryParams params) {
    return getQueryItemsAndFiltersWhereClause(params, new SqlHelper());
  }

  /**
   * Returns a from SQL clause for the given analytics table partition.
   *
   * @param params the {@link EventQueryParams}.
   */
  @Override
  protected String getFromClause(EventQueryParams params) {
    StringBuilder sql =
        new StringBuilder(" from ")
            .append(params.getTableName())
            .append(" as ")
            .append(ANALYTICS_TBL_ALIAS)
            .append(" ");

    resolveDateFieldPeriodBucketJoins(params, ANALYTICS_TBL_ALIAS)
        .forEach(join -> sql.append(join.toSql()).append(" "));

    return sql.append(joinOrgUnitTables(params, getAnalyticsType())).toString();
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

    {
      EventQueryParams timeFilterParams =
          EventPeriodUtils.sanitizeTimeFiltersForStageDateItems(params);
      String timeFieldSql = timeFieldSqlRenderer.renderPeriodTimeFieldSql(timeFilterParams);

      if (StringUtils.isNotBlank(timeFieldSql)) {
        sql += hlp.whereAnd() + " " + timeFieldSql;
      }
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
      sql += getDescendantsCondition(params, hlp);
    }

    // ---------------------------------------------------------------------
    // Categories and category option group sets (enrollments don't have attribute categories)
    // ---------------------------------------------------------------------

    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(
            Sets.newHashSet(
                DimensionType.CATEGORY,
                DimensionType.CATEGORY_OPTION_GROUP_SET,
                DimensionType.PROGRAM_STATUS));

    for (DimensionalObject dim : dynamicDimensions) {
      // PROGRAM_STATUS without items means group-by only — the column comes from the generic
      // dimension SELECT loop; there is no IN-list to filter on.
      DimensionType type = dim.getDimensionType();
      if (type == DimensionType.PROGRAM_STATUS && dim.getItems().isEmpty()) {
        continue;
      }

      String dimName = dim.getDimensionName();
      String col =
          params.isPiDisagDimension(dimName)
              ? piDisagQueryGenerator.getColumnForWhereClause(params, dimName)
              : quoteAlias(dimName);

      String condition =
          "and " + col + " in (" + getQuotedCommaDelimitedString(getUids(dim.getItems())) + ") ";

      // For stage-specific categories/COGS, add program stage filter
      if (dim.getProgramStage() != null) {
        condition =
            "and ("
                + col
                + " in ("
                + getQuotedCommaDelimitedString(getUids(dim.getItems()))
                + ") and ps = '"
                + dim.getProgramStage().getUid()
                + "') ";
      }

      sql += condition;
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

  /**
   * It gets the OU items and its respective levels, and generates a SQL condition/filter that
   * checks if the respective level matches any descendant.
   *
   * @param params the {@link EventQueryParams} where OU items are retrieved from.
   * @param hlp the {@link SqlHelper} used to append the right operator.
   * @return the SQL statement.
   */
  String getDescendantsCondition(EventQueryParams params, SqlHelper hlp) {
    List<DimensionalItemObject> orgUnitItems = params.getDimensionOrFilterItems(ORGUNIT_DIM_ID);
    Map<Integer, Set<String>> levelsOus = new HashMap<>();
    String condition = "";
    String orClause = "";

    for (DimensionalItemObject orgUnitItem : orgUnitItems) {
      OrganisationUnit orgUnit = (OrganisationUnit) orgUnitItem;
      Set<String> ouUids = levelsOus.get(orgUnit.getLevel());

      if (ouUids == null) {
        ouUids = new HashSet<>();
      }

      ouUids.add(orgUnit.getUid());
      levelsOus.put(orgUnit.getLevel(), ouUids);
    }

    boolean or = false;

    for (Entry<Integer, Set<String>> levelOu : levelsOus.entrySet()) {
      String column =
          params
              .getOrgUnitField()
              .withSqlBuilder(sqlBuilder)
              .getOrgUnitLevelCol(levelOu.getKey(), getAnalyticsType());
      orClause =
          orClause.concat(
              (or ? " or " : EMPTY)
                  + column
                  + " in ("
                  + getQuotedCommaDelimitedString(levelOu.getValue())
                  + ")");

      or = true;
    }

    if (!orClause.isEmpty()) {
      condition = hlp.whereAnd() + " (" + orClause + ") ";
    }

    return condition;
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
    return eventSubqueryBuilder.renderCoordinateSubquery(item);
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
    if (item.hasProgramStage()) {
      return eventSubqueryBuilder.renderValueSubquery(item, suffix);
    }
    if (isOrganizationUnitProgramAttribute(item)) {
      return quoteAlias(item.getItemName() + suffix);
    }
    return quoteAlias(item.getItemName());
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

  @Override
  protected AnalyticsType getAnalyticsType() {
    return AnalyticsType.ENROLLMENT;
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
    addDimensionSelectColumns(columns, params, true, true);
    removeLegacyPeriodDimensionColumns(columns, params);

    SelectBuilder sb = new SelectBuilder();
    sb.addColumn(ENROLLMENT_COL, "ax", ENROLLMENT_COL);
    for (String column : Sets.newHashSet(columns)) {
      sb.addColumn(SqlColumnParser.removeTableAlias(column));
    }

    addNonDefaultPeriodSourceColumns(sb, params);

    List<String> programIndicators =
        getProgramIndicators(params).stream().map(QueryItem::getItemId).toList();

    // Add the columns from the headers (only the ones that are not program indicators
    // and not stage-specific dimensions which are fetched from the filter CTE)
    for (String column : getHeaderColumns(headers, params)) {
      // Skip stage-specific headers (e.g., stageUid.eventdate, stageUid.ou) since
      // they are resolved from the per-stage filter CTE, not the base enrollment table
      if (stageHeaderClassifier.isStageSpecific(column)) {
        continue;
      }
      String colToAdd =
          dateHeaderResolver.normalizeHeaderKey(SqlColumnParser.removeTableAlias(column));
      if (!programIndicators.contains(colToAdd)) {
        Optional<AggregatedEnrollmentDateHeaderResolver.BaseAggregationHeaderProjection>
            headerProjection =
                dateHeaderResolver.resolveBaseProjection(colToAdd, usesAggregateEventJoin(params));
        if (headerProjection.isPresent()) {
          AggregatedEnrollmentDateHeaderResolver.BaseAggregationHeaderProjection projection =
              headerProjection.get();
          sb.addColumn(
              projection.sourceColumn(), projection.tableAlias(), quote(projection.alias()));
          continue;
        }
        sb.addColumnIfNotExist(quote(colToAdd));
      }
    }

    sb.from(getFromClause(params));

    addBaseAggregatedCteJoins(sb, cteContext, params);

    FilteredWhereContext filteredWhere = buildEnrollmentWhereClause(sb, params);
    // Add the base aggregate CTE along with the original where
    // condition, that have to be propagated in every other CTE for
    // performance reasons
    cteContext.addBaseAggregateCte(
        filteredWhere.sql(),
        SqlAliasReplacer.replaceTableAliases(sb.getWhereClause(), filteredWhere.columns()));
  }

  private void addNonDefaultPeriodSourceColumns(SelectBuilder sb, EventQueryParams params) {
    DimensionalObject periodDimension = params.getDimension(PERIOD_DIM_ID);
    if (periodDimension == null) {
      return;
    }

    for (DimensionalObject splitDim :
        PeriodDimensionSplitter.splitPeriodDimension(periodDimension)) {
      resolveDateFieldPeriodSourceColumn(splitDim).ifPresent(sb::addColumnIfNotExist);
    }
  }

  /**
   * Removes physical period columns from the base enrollment CTE when the requested period
   * dimension is derived from a non-default date field.
   *
   * <p>In that case, the final period value is resolved later through {@link
   * DateFieldPeriodBucketColumnResolver}. Keeping the physical period column in the base CTE would
   * duplicate the dimension and can produce conflicting projections in the outer aggregate query.
   */
  private void removeLegacyPeriodDimensionColumns(List<String> columns, EventQueryParams params) {
    DimensionalObject periodDimension = params.getDimension(PERIOD_DIM_ID);
    if (periodDimension == null) {
      return;
    }

    for (DimensionalObject splitDim :
        PeriodDimensionSplitter.splitPeriodDimension(periodDimension)) {
      if (resolveDateFieldPeriodBucket(splitDim, ANALYTICS_TBL_ALIAS).isPresent()) {
        splitDim.getItems().stream()
            .map(PeriodDimension.class::cast)
            .map(period -> period.getPeriodType().getPeriodTypeEnum().getName())
            .forEach(
                periodColumn ->
                    columns.removeIf(column -> periodColumn.equalsIgnoreCase(column.trim())));
      }
    }
  }

  /**
   * Builds the WHERE clause for the base enrollment CTE, keeping only predicates that require the
   * enrollment prefix while ensuring the referenced columns remain part of the projection.
   *
   * @param sb the {@link SelectBuilder} used to compose the base query.
   * @param params the {@link EventQueryParams} defining the current request.
   * @return a {@link FilteredWhereContext} containing the final SQL statement and the columns found
   *     in the WHERE clause.
   */
  private FilteredWhereContext buildEnrollmentWhereClause(
      SelectBuilder sb, EventQueryParams params) {
    Condition baseWhereCondition = Condition.raw(getBaseAggregationWhereClause(params));

    EventQueryParams paramsWithoutProgramStageItems = withoutProgramStageItems(params);
    Set<QueryItem> aggregateEventDateFilters =
        new LinkedHashSet<>(getAggregateEventDateFilters(paramsWithoutProgramStageItems));

    Set<QueryItem> filtersWithoutEnrollmentPrefix =
        Stream.concat(
                paramsWithoutProgramStageItems.getItems().stream(),
                paramsWithoutProgramStageItems.getItemFilters().stream())
            .filter(QueryItem::hasFilter)
            .filter(
                item ->
                    aggregateEventDateFilters.contains(item)
                        || !needsEnrollmentPrefix(item.getItem(), params))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    String filterClauseWithEnrollmentPrefixOnly =
        getQueryItemsAndFiltersWhereClause(
            paramsWithoutProgramStageItems, filtersWithoutEnrollmentPrefix, new SqlHelper());

    sb.where(
        Condition.and(
            baseWhereCondition,
            // Keep only the predicates that require an enrollment prefix
            Condition.raw(filterClauseWithEnrollmentPrefixOnly)));

    List<String> whereColumns = SqlWhereClauseExtractor.extractWhereColumns(sb.build());

    Map<String, QueryItem> queryItemsByUid =
        Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
            .collect(
                Collectors.toMap(
                    item -> item.getItem().getUid(),
                    Function.identity(),
                    (existing, replacement) -> existing));

    for (String column : whereColumns) {
      QueryItem queryItem = queryItemsByUid.get(column);

      if (queryItem != null) {
        sb.addColumnIfNotExist(column);
      }
    }

    String finalSql = sb.build();
    List<String> finalWhereColumns = SqlWhereClauseExtractor.extractWhereColumns(finalSql);

    return new FilteredWhereContext(finalSql, finalWhereColumns);
  }

  private record FilteredWhereContext(String sql, List<String> columns) {}

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

  private void addBaseAggregatedCteJoins(
      SelectBuilder sb, CteContext cteContext, EventQueryParams params) {
    addBaseAggregatedCteJoins(sb, cteContext);

    List<QueryItem> aggregateEventDateFilters = getAggregateEventDateFilters(params);
    Map<TimeField, List<DateRange>> eventTimeDateRanges = getAggregateEventTimeDateRanges(params);

    if (aggregateEventDateFilters.isEmpty() && eventTimeDateRanges.isEmpty()) {
      return;
    }

    List<String> eventFilters = new ArrayList<>();

    String eventFilterSql =
        aggregateEventDateFilters.stream()
            .map(item -> extractFiltersAsSql(item, "ev." + quote(item.getItemName()), params))
            .filter(StringUtils::isNotBlank)
            .collect(joining(" and "));

    if (!eventFilterSql.isBlank()) {
      eventFilters.add(eventFilterSql);
    }

    eventTimeDateRanges.forEach(
        (timeField, dateRanges) ->
            dateRanges.stream()
                .map(dateRange -> buildEventDateRangeSql(timeField, dateRange))
                .filter(StringUtils::isNotBlank)
                .forEach(eventFilters::add));

    if (eventFilters.isEmpty()) {
      return;
    }

    String eventTableName = AnalyticsTableNames.eventTable(params.getProgram());
    String eventEnrollmentFilterSql =
        """
        (
            select
                ev.enrollment as enrollment,
                max(ev.%s) as %s
            from %s ev
            where ev.eventstatus != 'SCHEDULE'
              and %s
            group by ev.enrollment
        )
        """
            .formatted(
                quote(TimeField.EVENT_DATE.getEventColumnName()),
                AggregatedEnrollmentDateHeaderResolver.EVENT_DATE_JOIN_COLUMN,
                eventTableName,
                String.join(" and ", eventFilters));

    sb.innerJoin(
        eventEnrollmentFilterSql,
        AggregatedEnrollmentDateHeaderResolver.EVENT_DATE_JOIN_ALIAS,
        tableAlias -> tableAlias + ".%s = ax.%s".formatted(ENROLLMENT_COL, ENROLLMENT_COL));
  }

  private List<QueryItem> getAggregateEventDateFilters(EventQueryParams params) {
    return Stream.concat(params.getItems().stream(), params.getItemFilters().stream())
        .filter(QueryItem::hasFilter)
        .filter(item -> !item.hasProgramStage())
        .filter(
            item ->
                EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME.equals(item.getItemId()))
        .toList();
  }

  private Map<TimeField, List<DateRange>> getAggregateEventTimeDateRanges(EventQueryParams params) {
    return params.getTimeDateRanges(EVENT_TIME_FIELDS);
  }

  private String buildEventDateRangeSql(TimeField timeField, DateRange dateRange) {
    String eventColumn = "ev." + quote(timeField.getEventColumnName());
    return "%s >= '%s' and %s < '%s'"
        .formatted(
            eventColumn,
            toMediumDate(dateRange.getStartDate()),
            eventColumn,
            toMediumDate(dateRange.getEndDatePlusOneDay()));
  }

  private String getBaseAggregationWhereClause(EventQueryParams params) {
    EventQueryParams sanitizedParams =
        EventPeriodUtils.sanitizeTimeFiltersForStageDateItems(params);
    sanitizedParams = withoutProgramStageItems(sanitizedParams);
    Set<QueryItem> aggregateEventDateFilters =
        new LinkedHashSet<>(getAggregateEventDateFilters(params));

    if (!aggregateEventDateFilters.isEmpty()) {
      sanitizedParams =
          withoutQueryItems(sanitizedParams, item -> aggregateEventDateFilters.contains(item));
    }

    if (sanitizedParams.hasTimeDateRanges()) {
      sanitizedParams =
          new EventQueryParams.Builder(sanitizedParams)
              .withoutTimeDateRanges(EVENT_TIME_FIELDS)
              .build();
    }

    return getWhereClause(sanitizedParams);
  }

  private EventQueryParams withoutQueryItems(
      EventQueryParams params, Predicate<QueryItem> predicate) {
    EventQueryParams.Builder builder = new EventQueryParams.Builder(params);
    List<QueryItem> filteredItems = params.getItems().stream().filter(predicate.negate()).toList();
    List<QueryItem> filteredItemFilters =
        params.getItemFilters().stream().filter(predicate.negate()).toList();

    builder.removeItems().removeItemFilters();
    filteredItems.forEach(builder::addItem);
    filteredItemFilters.forEach(builder::addItemFilter);

    return builder.build();
  }

  private boolean usesAggregateEventJoin(EventQueryParams params) {
    return !getAggregateEventDateFilters(params).isEmpty()
        || !getAggregateEventTimeDateRanges(params).isEmpty();
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
    List<AggregatedEnrollmentQueryAssembler.PeriodProjection> periodProjections =
        aggregatedAssembler.resolveAggregatePeriodProjections(params);

    // 3.1: Append the WITH clause if needed
    addCteClause(sb, cteContext);

    addAggregateEnrollmentSelectColumnsInHeaderOrder(
        sb, headers, params, cteContext, periodProjections);

    // 3.3: Append the FROM clause (the main enrollment analytics table)
    sb.from(ENROLLMENT_AGGR_BASE, ENROLLMENT_AGGR_BASE_ALIAS);
    appendPeriodBucketJoins(sb, periodProjections);

    // 3.4: Append JOINs for each relevant CTE definition
    for (String itemUid : cteContext.getCteKeysExcluding(ENROLLMENT_AGGR_BASE)) {
      CteDefinition cteDef = cteContext.getDefinitionByItemUid(itemUid);
      String tableName = useItemUidForTable(cteDef) ? itemUid : cteDef.getAlias();
      sb.leftJoin(
          tableName,
          cteDef.getAlias(),
          tableAlias -> tableAlias + ".enrollment = " + ENROLLMENT_AGGR_BASE_ALIAS + ".enrollment");
    }
    return sb.build();
  }

  private void addAggregateEnrollmentSelectColumnsInHeaderOrder(
      SelectBuilder sb,
      List<GridHeader> headers,
      EventQueryParams params,
      CteContext cteContext,
      List<AggregatedEnrollmentQueryAssembler.PeriodProjection> periodProjections) {
    if (headers.isEmpty()) {
      aggregatedAssembler.addAggregatedColumns(sb);
      aggregatedAssembler.addOrgUnitAggregateColumns(sb, params);
      aggregatedAssembler.addPeriodAggregateColumns(params, sb, periodProjections);
      aggregatedAssembler.addHeaderAggregateColumns(
          headers, params, cteContext, sb, periodProjections);
      return;
    }

    Set<String> periodHeaderNames =
        periodProjections.stream()
            .map(AggregatedEnrollmentQueryAssembler.PeriodProjection::responseKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    periodHeaderNames.add(PERIOD_DIM_ID);
    periodHeaderNames.addAll(aggregatedAssembler.collectPeriodDateFieldKeys(params));

    Set<AggregateEnrollmentHeaderType> addedInfrastructureColumns =
        EnumSet.noneOf(AggregateEnrollmentHeaderType.class);

    for (GridHeader header : headers) {
      String headerName = header.getName();
      AggregateEnrollmentHeaderType headerType =
          classifyAggregateEnrollmentHeader(headerName, periodHeaderNames);

      switch (headerType) {
        case VALUE -> {
          if (addedInfrastructureColumns.add(headerType)) {
            aggregatedAssembler.addAggregatedColumns(sb);
          }
        }
        case ORG_UNIT -> {
          if (addedInfrastructureColumns.add(headerType)) {
            aggregatedAssembler.addOrgUnitAggregateColumns(sb, params);
          }
        }
        case PERIOD -> {
          if (addedInfrastructureColumns.add(headerType)) {
            aggregatedAssembler.addPeriodAggregateColumns(params, sb, periodProjections);
          }
        }
        case OTHER ->
            aggregatedAssembler.addHeaderAggregateColumns(
                List.of(header), params, cteContext, sb, periodProjections);
      }
    }
  }

  private AggregateEnrollmentHeaderType classifyAggregateEnrollmentHeader(
      String headerName, Set<String> periodHeaderNames) {
    if (COL_VALUE.equalsIgnoreCase(headerName)) {
      return AggregateEnrollmentHeaderType.VALUE;
    }

    if (ORGUNIT_DIM_ID.equalsIgnoreCase(headerName)) {
      return AggregateEnrollmentHeaderType.ORG_UNIT;
    }

    if (periodHeaderNames.stream().anyMatch(headerName::equalsIgnoreCase)) {
      return AggregateEnrollmentHeaderType.PERIOD;
    }

    return AggregateEnrollmentHeaderType.OTHER;
  }

  private boolean useItemUidForTable(CteDefinition cteDefinition) {
    return cteDefinition.getCteType() == CteDefinition.CteType.PROGRAM_INDICATOR_ENROLLMENT
        || cteDefinition.getCteType() == CteDefinition.CteType.PROGRAM_INDICATOR_EVENT
        || cteDefinition.getCteType() == CteDefinition.CteType.PROGRAM_STAGE
        || cteDefinition.getCteType() == CteDefinition.CteType.FILTER;
  }

  @Override
  protected String getSortClause(EventQueryParams params) {
    if (params.isSorting()) {
      return super.getSortClause(params);
    }
    return "";
  }

  private void appendPeriodBucketJoins(
      SelectBuilder sb, List<AggregatedEnrollmentQueryAssembler.PeriodProjection> projections) {
    projections.stream()
        .map(AggregatedEnrollmentQueryAssembler.PeriodProjection::expression)
        .flatMap(Optional::stream)
        .map(DateFieldPeriodBucketColumnResolver.ResolvedExpression::joinClause)
        .flatMap(Optional::stream)
        .forEach(join -> sb.leftJoin(join.table(), join.alias(), tableAlias -> join.condition()));
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
      aggregatedAssembler.addAggregatedColumns(sb);
    } else {
      aggregatedAssembler.addStandardColumns(sb, cteContext, getStandardColumns(params));
    }

    // Append columns from CTE definitions
    getSelectColumnsWithCTE(params, cteContext).forEach(sb::addColumn);
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
        EnrollmentAnalyticsColumnName.CREATED_BY_DISPLAY_NAME_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.LAST_UPDATED_BY_DISPLAY_NAME_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME,
        EventAnalyticsColumnName.CREATED_COLUMN_NAME,
        EnrollmentAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME);

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
