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
package org.hisp.dhis.analytics.table;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.commons.util.TextUtils.emptyIfTrue;
import static org.hisp.dhis.commons.util.TextUtils.format;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.util.DateUtils.toLongDate;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsDimensionType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.util.PartitionUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service("org.hisp.dhis.analytics.EventAnalyticsTableManager")
public class JdbcEventAnalyticsTableManager extends AbstractEventJdbcTableManager {

  public static final String OU_GEOMETRY_COL_SUFFIX = "_geom";

  static final String[] EXPORTABLE_EVENT_STATUSES = {"'COMPLETED'", "'ACTIVE'", "'SCHEDULE'"};

  protected final List<AnalyticsTableColumn> fixedColumns;

  public JdbcEventAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      DatabaseInfoProvider databaseInfoProvider,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      AnalyticsTableSettings analyticsExportSettings,
      PeriodDataProvider periodDataProvider,
      SqlBuilder sqlBuilder) {
    super(
        idObjectManager,
        organisationUnitService,
        categoryService,
        settingsProvider,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        partitionManager,
        databaseInfoProvider,
        jdbcTemplate,
        analyticsExportSettings,
        periodDataProvider,
        sqlBuilder);
    fixedColumns = EventAnalyticsColumn.getColumns(sqlBuilder);
  }

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return AnalyticsTableType.EVENT;
  }

  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    log.info(
        "Get tables using earliest: {}, spatial support: {}",
        params.getFromDate(),
        isSpatialSupport());

    List<Integer> availableDataYears =
        periodDataProvider.getAvailableYears(analyticsTableSettings.getPeriodSource());

    return params.isLatestUpdate()
        ? getLatestAnalyticsTables(params)
        : getRegularAnalyticsTables(params, availableDataYears);
  }

  /**
   * Creates a list of {@link AnalyticsTable} for each program. The tables contain a partition for
   * each year for which events exist.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param availableDataYears
   * @return a list of {@link AnalyticsTableUpdateParams}.
   */
  private List<AnalyticsTable> getRegularAnalyticsTables(
      AnalyticsTableUpdateParams params, List<Integer> availableDataYears) {
    Calendar calendar = PeriodType.getCalendar();
    List<AnalyticsTable> tables = new ArrayList<>();
    Logged logged = analyticsTableSettings.getTableLogged();

    List<Program> programs =
        params.isSkipPrograms()
            ? idObjectManager.getAllNoAcl(Program.class)
            : idObjectManager.getAllNoAcl(Program.class).stream()
                .filter(p -> !params.getSkipPrograms().contains(p.getUid()))
                .collect(toList());

    Integer firstDataYear = availableDataYears.get(0);
    Integer latestDataYear = availableDataYears.get(availableDataYears.size() - 1);

    for (Program program : programs) {
      List<Integer> yearsForPartitionTables =
          getYearsForPartitionTable(getDataYears(params, program, firstDataYear, latestDataYear));

      Collections.sort(yearsForPartitionTables);

      AnalyticsTable table =
          new AnalyticsTable(getAnalyticsTableType(), getColumns(program), logged, program);

      for (Integer year : yearsForPartitionTables) {
        List<String> checks = getPartitionChecks(year, PartitionUtils.getEndDate(calendar, year));
        table.addTablePartition(
            checks,
            year,
            PartitionUtils.getStartDate(calendar, year),
            PartitionUtils.getEndDate(calendar, year));
      }

      if (table.hasTablePartitions()) {
        tables.add(table);
      }
    }

    return tables;
  }

  /**
   * Creates a list of {@link AnalyticsTable} with a partition each or the "latest" data. The start
   * date of the partition is the time of the last successful full analytics table update. The end
   * date of the partition is the start time of this analytics table update process.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return a list of {@link AnalyticsTableUpdateParams}.
   */
  private List<AnalyticsTable> getLatestAnalyticsTables(AnalyticsTableUpdateParams params) {
    SystemSettings settings = settingsProvider.getCurrentSettings();
    Date lastFullTableUpdate = settings.getLastSuccessfulAnalyticsTablesUpdate();
    Date lastLatestPartitionUpdate = settings.getLastSuccessfulLatestAnalyticsPartitionUpdate();
    Date lastAnyTableUpdate = DateUtils.getLatest(lastLatestPartitionUpdate, lastFullTableUpdate);

    Assert.isTrue(
        lastFullTableUpdate.getTime() > 0L,
        "A full analytics table update process must be run prior to a latest partition update");

    Date startDate = lastFullTableUpdate;
    Date endDate = params.getStartTime();

    List<AnalyticsTable> tables = new ArrayList<>();

    Logged logged = analyticsTableSettings.getTableLogged();

    List<Program> programs =
        params.isSkipPrograms()
            ? idObjectManager.getAllNoAcl(Program.class).stream()
                .filter(p -> !params.getSkipPrograms().contains(p.getUid()))
                .collect(toList())
            : idObjectManager.getAllNoAcl(Program.class);

    for (Program program : programs) {
      boolean hasUpdatedData = hasUpdatedLatestData(lastAnyTableUpdate, endDate, program);

      if (hasUpdatedData) {
        AnalyticsTable table =
            new AnalyticsTable(getAnalyticsTableType(), getColumns(program), logged, program);
        table.addTablePartition(
            List.of(), AnalyticsTablePartition.LATEST_PARTITION, startDate, endDate);
        tables.add(table);

        log.info(
            "Added latest event analytics partition for program: '{}' with start: '{}' and end: '{}'",
            program.getUid(),
            toLongDate(startDate),
            toLongDate(endDate));
      } else {
        log.info(
            "No updated latest event data found for program: '{}' with start: '{}' and end: '{}",
            program.getUid(),
            toLongDate(lastAnyTableUpdate),
            toLongDate(endDate));
      }
    }

    return tables;
  }

  /**
   * Indicates whether event data stored between the given start and end date and for the given
   * program exists.
   *
   * @param startDate the start date.
   * @param endDate the end date.
   * @param program the program.
   * @return whether event data exists.
   */
  private boolean hasUpdatedLatestData(Date startDate, Date endDate, Program program) {
    String sql =
        replaceQualify(
            """
            select ev.eventid \
            from ${event} ev \
            inner join ${enrollment} en on ev.enrollmentid=en.enrollmentid \
            where en.programid = ${programId} \
            and ev.lastupdated >= '${startDate}' \
            and ev.lastupdated < '${endDate}' \
            limit 1;""",
            Map.of(
                "programId", String.valueOf(program.getId()),
                "startDate", toLongDate(startDate),
                "endDate", toLongDate(endDate)));

    return !jdbcTemplate.queryForList(sql).isEmpty();
  }

  @Override
  public void removeUpdatedData(List<AnalyticsTable> tables) {
    for (AnalyticsTable table : tables) {
      AnalyticsTablePartition partition = table.getLatestTablePartition();

      String sql =
          replaceQualify(
              """
              delete from ${tableName} ax \
              where ax.event in ( \
              select ev.uid \
              from ${event} ev \
              inner join ${enrollment} en on ev.enrollmentid=en.enrollmentid \
              where en.programid = ${programId} \
              and ev.lastupdated >= '${startDate}' \
              and ev.lastupdated < '${endDate}');""",
              Map.of(
                  "tableName", qualify(table.getName()),
                  "programId", String.valueOf(table.getProgram().getId()),
                  "startDate", toLongDate(partition.getStartDate()),
                  "endDate", toLongDate(partition.getEndDate())));

      invokeTimeAndLog(sql, "Remove updated events for table: '{}'", table.getName());
    }
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    Objects.requireNonNull(year);
    return List.of("yearly = '" + year + "'");
  }

  @Override
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    List<Integer> availableDataYears =
        periodDataProvider.getAvailableYears(analyticsTableSettings.getPeriodSource());
    Integer firstDataYear = availableDataYears.get(0);
    Integer latestDataYear = availableDataYears.get(availableDataYears.size() - 1);
    Program program = partition.getMasterTable().getProgram();
    String partitionClause = getPartitionClause(partition);

    String fromClause =
        replaceQualify(
            """
            \sfrom ${event} ev \
            inner join ${enrollment} en on ev.enrollmentid=en.enrollmentid \
            inner join ${programstage} ps on ev.programstageid=ps.programstageid \
            inner join ${program} pr on en.programid=pr.programid and en.deleted = false \
            left join ${trackedentity} te on en.trackedentityid=te.trackedentityid and te.deleted = false \
            left join ${organisationunit} registrationou on te.organisationunitid=registrationou.organisationunitid \
            inner join ${organisationunit} ou on ev.organisationunitid=ou.organisationunitid \
            left join analytics_rs_orgunitstructure ous on ev.organisationunitid=ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on ev.organisationunitid=ougs.organisationunitid \
            and (cast(${eventDateMonth} as date)=ougs.startdate or ougs.startdate is null) \
            left join ${organisationunit} enrollmentou on en.organisationunitid=enrollmentou.organisationunitid \
            inner join analytics_rs_categorystructure acs on ev.attributeoptioncomboid=acs.categoryoptioncomboid \
            left join analytics_rs_dateperiodstructure dps on cast(${eventDateExpression} as date)=dps.dateperiod \
            where ev.lastupdated < '${startTime}' ${partitionClause} \
            and pr.programid=${programId} \
            and ev.organisationunitid is not null \
            and (${eventDateExpression}) is not null \
            and dps.year >= ${firstDataYear} \
            and dps.year <= ${latestDataYear} \
            and ev.status in (${exportableEventStatues}) \
            and ev.deleted = false""",
            Map.of(
                "eventDateMonth", sqlBuilder.dateTrunc("month", eventDateExpression),
                "eventDateExpression", eventDateExpression,
                "partitionClause", partitionClause,
                "startTime", toLongDate(params.getStartTime()),
                "programId", String.valueOf(program.getId()),
                "firstDataYear", String.valueOf(firstDataYear),
                "latestDataYear", String.valueOf(latestDataYear),
                "exportableEventStatues", String.join(",", EXPORTABLE_EVENT_STATUSES)));

    populateTableInternal(partition, fromClause);
  }

  /**
   * Returns a partition SQL clause.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   * @return a partition SQL clause.
   */
  private String getPartitionClause(AnalyticsTablePartition partition) {
    String start = toLongDate(partition.getStartDate());
    String end = toLongDate(partition.getEndDate());
    String statusDate = eventDateExpression;
    String latestFilter = format("and ev.lastupdated >= '{}' ", start);
    String partitionFilter =
        format("and ({}) >= '{}' and ({}) < '{}' ", statusDate, start, statusDate, end);

    return partition.isLatestPartition()
        ? latestFilter
        : emptyIfTrue(partitionFilter, sqlBuilder.supportsDeclarativePartitioning());
  }

  /**
   * Returns dimensional analytics table columns.
   *
   * @param program the program.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>(fixedColumns);
    columns.addAll(getAttributeCategoryColumns(program));
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(getOrganisationUnitGroupSetColumns());
    columns.addAll(getAttributeCategoryOptionGroupSetColumns());
    columns.addAll(getPeriodTypeColumns("dps"));
    columns.addAll(getDataElementColumns(program));
    columns.addAll(getAttributeColumns(program));

    if (program.isRegistration()) {
      columns.add(EventAnalyticsColumn.TRACKED_ENTITY);
      if (sqlBuilder.supportsGeospatialData()) {
        columns.add(EventAnalyticsColumn.TRACKED_ENTITY_GEOMETRY);
      }
    }
    if (sqlBuilder.supportsDeclarativePartitioning()) {
      columns.add(getPartitionColumn());
    }

    return filterDimensionColumns(columns);
  }

  /**
   * Returns a partition column.
   *
   * @return an {@link AnalyticsTableColumn}.
   */
  private AnalyticsTableColumn getPartitionColumn() {
    return AnalyticsTableColumn.builder()
        .name("year")
        .dataType(INTEGER)
        .selectExpression("dps.year")
        .build();
  }

  /**
   * Returns columns for attribute categories of the given program.
   *
   * @param program the {@link Program}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getAttributeCategoryColumns(Program program) {
    if (program.hasNonDefaultCategoryCombo()) {
      List<Category> categories = program.getCategoryCombo().getDataDimensionCategories();
      return categories.stream()
          .map(
              category ->
                  AnalyticsTableColumn.builder()
                      .name(category.getUid())
                      .dimensionType(AnalyticsDimensionType.DYNAMIC)
                      .dataType(CHARACTER_11)
                      .selectExpression("acs." + quote(category.getUid()))
                      .created(category.getCreated())
                      .build())
          .toList();
    }

    return List.of();
  }

  /**
   * Returns columns for data elements of the given program.
   *
   * @param program the {@link Program}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getDataElementColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(
        program.getAnalyticsDataElements().stream()
            .map(de -> getColumnForDataElement(de, false))
            .flatMap(Collection::stream)
            .toList());
    columns.addAll(
        program.getAnalyticsDataElementsWithLegendSet().stream()
            .map(de -> getColumnForDataElement(de, true))
            .flatMap(Collection::stream)
            .toList());
    return columns;
  }

  /**
   * Returns a column for the given data element. If the value type of the data element is {@link
   * ValueType#ORGANISATION_UNIT}, an extra column will be included.
   *
   * @param dataElement the {@link DataElement}.
   * @param withLegendSet indicates
   * @return
   */
  private List<AnalyticsTableColumn> getColumnForDataElement(
      DataElement dataElement, boolean withLegendSet) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    DataType dataType = getColumnType(dataElement.getValueType(), isSpatialSupport());
    String columnExpression =
        sqlBuilder.jsonExtractNested("eventdatavalues", dataElement.getUid(), "value");
    String selectExpression = getSelectExpression(dataElement.getValueType(), columnExpression);
    String dataFilterClause = getDataFilterClause(dataElement);
    String sql = getSelectForInsert(dataElement, selectExpression, dataFilterClause);
    Skip skipIndex = skipIndex(dataElement.getValueType(), dataElement.hasOptionSet());

    if (withLegendSet) {
      return getColumnFromDataElementWithLegendSet(dataElement, selectExpression, dataFilterClause);
    }

    if (dataElement.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnForOrgUnitDataElement(dataElement, dataFilterClause));
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(dataElement.getUid())
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(sql)
            .skipIndex(skipIndex)
            .build());

    return columns;
  }

  /**
   * Returns a list of columns.
   *
   * @param dataElement the {@link DataElement}.
   * @param dataFilterClause the data filter SQL clause.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForOrgUnitDataElement(
      DataElement dataElement, String dataFilterClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String columnExpression =
        sqlBuilder.jsonExtractNested("eventdatavalues", dataElement.getUid(), "value");
    String fromClause =
        qualifyVariables("from ${organisationunit} ou where ou.uid = " + columnExpression);

    if (isSpatialSupport()) {
      String fromType = "ou.geometry " + fromClause;
      String geoSql = getSelectForInsert(dataElement, fromType, dataFilterClause);

      columns.add(
          AnalyticsTableColumn.builder()
              .name((dataElement.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(geoSql)
              .indexType(IndexType.GIST)
              .build());
    }

    String fromTypeSql = "ou.name " + fromClause;
    String ouNameSql = getSelectForInsert(dataElement, fromTypeSql, dataFilterClause);

    columns.add(
        AnalyticsTableColumn.builder()
            .name((dataElement.getUid() + OU_NAME_COL_SUFFIX))
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(ouNameSql)
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * Returns columns for attributes of the given program.
   *
   * @param program the {@link Program}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getAttributeColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(
        program.getNonConfidentialTrackedEntityAttributes().stream()
            .map(this::getColumnForAttribute)
            .flatMap(Collection::stream)
            .toList());
    columns.addAll(
        program.getNonConfidentialTrackedEntityAttributesWithLegendSet().stream()
            .map(this::getColumnForAttributeWithLegendSet)
            .flatMap(Collection::stream)
            .toList());
    return columns;
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param withLegendSet indicates whether the attribute has a legend set.
   * @return a list of {@link AnaylyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForAttribute(TrackedEntityAttribute attribute) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    DataType dataType = getColumnType(attribute.getValueType(), isSpatialSupport());
    String selectExpression = getSelectExpressionForAttribute(attribute.getValueType(), "value");
    String dataExpression = getDataFilterClause(attribute);
    String sql = getSelectSubquery(attribute, selectExpression, dataExpression);
    Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

    if (attribute.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnsForOrgUnitTrackedEntityAttribute(attribute, dataExpression));
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(attribute.getUid())
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(sql)
            .skipIndex(skipIndex)
            .build());

    return columns;
  }

  /**
   * Returns a list of columns based on the given attribute with legend set.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForAttributeWithLegendSet(
      TrackedEntityAttribute attribute) {
    String selectClause = getSelectExpression(attribute.getValueType(), "value");
    String numericClause = getNumericClause();
    String query =
        """
        \s(select l.uid from ${maplegend} l \
        inner join ${trackedentityattributevalue} av on l.startvalue <= ${selectClause} \
        and l.endvalue > ${selectClause} \
        and l.maplegendsetid=${legendSetId} \
        and av.trackedentityid=en.trackedentityid \
        and av.trackedentityattributeid=${attributeId} ${numericClause}) as ${column}""";

    return attribute.getLegendSets().stream()
        .map(
            ls -> {
              String column = attribute.getUid() + PartitionUtils.SEP + ls.getUid();
              String sql =
                  replaceQualify(
                      query,
                      Map.of(
                          "selectClause", selectClause,
                          "legendSetId", String.valueOf(ls.getId()),
                          "column", column,
                          "attributeId", String.valueOf(attribute.getId()),
                          "numericClause", numericClause));

              return AnalyticsTableColumn.builder()
                  .name(column)
                  .dataType(CHARACTER_11)
                  .selectExpression(sql)
                  .build();
            })
        .toList();
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param dataFilterClause the data filter clause.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnsForOrgUnitTrackedEntityAttribute(
      TrackedEntityAttribute attribute, String dataFilterClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String fromClause =
        qualifyVariables("from ${organisationunit} ou where ou.uid = (select value");

    if (isSpatialSupport()) {
      String fromType = "ou.geometry " + fromClause;
      String geoSql = getSelectSubquery(attribute, fromType, dataFilterClause);
      columns.add(
          AnalyticsTableColumn.builder()
              .name((attribute.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(geoSql)
              .indexType(IndexType.GIST)
              .build());
    }

    String fromTypeSql = "ou.name " + fromClause;
    String ouNameSql = getSelectSubquery(attribute, fromTypeSql, dataFilterClause);

    columns.add(
        AnalyticsTableColumn.builder()
            .name((attribute.getUid() + OU_NAME_COL_SUFFIX))
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(ouNameSql)
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * Creates a select statement for the given select expression.
   *
   * @param dataElement the data element to create the select statement for.
   * @param selectExpression the select expression.
   * @param dataFilterClause the data filter clause.
   * @return A SQL select expression for the data element.
   */
  private String getSelectForInsert(
      DataElement dataElement, String selectExpression, String dataFilterClause) {
    String sqlTemplate =
        dataElement.getValueType().isOrganisationUnit()
            ? "(select ${selectExpression} ${dataClause})${closingParentheses} as ${uid}"
            : "${selectExpression}${closingParentheses} as ${uid}";

    return replaceQualify(
        sqlTemplate,
        Map.of(
            "selectExpression",
            selectExpression,
            "dataClause",
            dataFilterClause,
            "closingParentheses",
            getClosingParentheses(selectExpression),
            "uid",
            quote(dataElement.getUid())));
  }

  /**
   * Returns a list of columns.
   *
   * @param dataElement the {@link DataElement}.
   * @param selectExpression the select expression.
   * @param dataFilterClause the data filter clause.
   * @return a list of {@link AnayticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet(
      DataElement dataElement, String selectExpression, String dataFilterClause) {
    String query =
        """
        (select l.uid from ${maplegend} l \
        inner join ${event} on l.startvalue <= ${select} \
        and l.endvalue > ${select} \
        and l.maplegendsetid=${legendSetId} \
        ${dataClause} where eventid = ev.eventid) as ${column}""";

    return dataElement.getLegendSets().stream()
        .map(
            ls -> {
              String column = dataElement.getUid() + PartitionUtils.SEP + ls.getUid();
              String sql =
                  replaceQualify(
                      query,
                      Map.of(
                          "select", selectExpression,
                          "legendSetId", String.valueOf(ls.getId()),
                          "dataClause", dataFilterClause,
                          "column", column));

              return AnalyticsTableColumn.builder()
                  .name(column)
                  .dataType(CHARACTER_11)
                  .selectExpression(sql)
                  .build();
            })
        .toList();
  }

  /**
   * For numeric and date value types, returns a data filter clause for checking whether the value
   * is valid according to the value type. For other value types, returns the empty string.
   *
   * @param dataElement the {@link DataElement}.
   * @return an filter expression.
   */
  private String getDataFilterClause(DataElement dataElement) {
    String uid = dataElement.getUid();
    ValueType valueType = dataElement.getValueType();

    if (valueType.isNumeric() || valueType.isDate()) {
      String jsonExpression = sqlBuilder.jsonExtractNested("eventdatavalues", uid, "value");
      String regex = valueType.isNumeric() ? NUMERIC_REGEXP : DATE_REGEXP;

      return " and " + sqlBuilder.regexpMatch(jsonExpression, regex);
    }

    return EMPTY;
  }

  /**
   * For numeric and date value types, returns a data filter clause for checking whether the value
   * is valid according to the value type. For other value types, returns the empty string.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return an filter expression.
   */
  private String getDataFilterClause(TrackedEntityAttribute attribute) {
    if (attribute.isNumericType()) {
      return getNumericClause();
    }
    return attribute.isDateType() ? getDateClause() : EMPTY;
  }

  /**
   * Returns a list of years for which data exist.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param program the {@link Program}.
   * @param firstDataYear the first year to include.
   * @param lastDataYear the last data year to include.
   * @return a list of years for which data exist.
   */
  private List<Integer> getDataYears(
      AnalyticsTableUpdateParams params,
      Program program,
      Integer firstDataYear,
      Integer lastDataYear) {
    String fromDateClause =
        params.getFromDate() != null
            ? replace(
                "and (${eventDateExpression}) >= '${fromDate}'",
                Map.of(
                    "eventDateExpression",
                    eventDateExpression,
                    "fromDate",
                    toMediumDate(params.getFromDate())))
            : EMPTY;

    String sql =
        replaceQualify(
            """
            select temp.supportedyear from \
            (select distinct extract(year from ${eventDateExpression}) as supportedyear \
            from ${event} ev \
            inner join ${enrollment} en on ev.enrollmentid = en.enrollmentid \
            where ev.lastupdated <= '${startTime}' and en.programid = ${programId} \
            and (${eventDateExpression}) is not null \
            and (${eventDateExpression}) > '1000-01-01' \
            and ev.deleted = false \
            ${fromDateClause}) as temp \
            where temp.supportedyear >= ${firstDataYear} \
            and temp.supportedyear <= ${latestDataYear}""",
            Map.of(
                "eventDateExpression", eventDateExpression,
                "startTime", toLongDate(params.getStartTime()),
                "programId", String.valueOf(program.getId()),
                "fromDateClause", fromDateClause,
                "firstDataYear", String.valueOf(firstDataYear),
                "latestDataYear", String.valueOf(lastDataYear)));

    return jdbcTemplate.queryForList(sql, Integer.class);
  }

  /**
   * Retrieve years for partition tables. Year will become a partition key. The default return value
   * is the list with the recent year.
   *
   * @param dataYears the list of years coming from inner join of event and enrollment tables.
   * @return list of partition key values.
   */
  private List<Integer> getYearsForPartitionTable(List<Integer> dataYears) {
    return ListUtils.mutableCopy(!dataYears.isEmpty() ? dataYears : List.of(Year.now().getValue()));
  }
}
