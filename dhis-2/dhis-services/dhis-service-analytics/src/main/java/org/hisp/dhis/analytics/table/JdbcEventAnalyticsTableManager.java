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
package org.hisp.dhis.analytics.table;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.commons.util.TextUtils.emptyIfTrue;
import static org.hisp.dhis.commons.util.TextUtils.format;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;
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
import java.util.Set;
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

  static final String[] EXPORTABLE_EVENT_STATUSES = {"'COMPLETED'", "'ACTIVE'", "'SCHEDULE'"};

  public JdbcEventAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      @Qualifier("postgresSqlBuilder") SqlBuilder sqlBuilder) {
    super(
        idObjectManager,
        organisationUnitService,
        categoryService,
        settingsProvider,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        partitionManager,
        jdbcTemplate,
        analyticsTableSettings,
        periodDataProvider,
        sqlBuilder);
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
        isGeospatialSupport());

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
            "Added latest event analytics partition for program: '{}', start: '{}' and end: '{}'",
            program.getUid(),
            toLongDate(startDate),
            toLongDate(endDate));
      } else {
        log.info(
            "No updated latest event data found for program: '{}', start: '{}' and end: '{}",
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
    String attributeJoinClause = getAttributeValueJoinClause(program);

    String fromClause =
        replaceQualify(
            """
            \sfrom ${event} ev \
            inner join ${enrollment} en on ev.enrollmentid=en.enrollmentid \
            inner join ${programstage} ps on ev.programstageid=ps.programstageid \
            inner join ${program} pr on en.programid=pr.programid and ${enDeletedClause} \
            left join ${trackedentity} te on en.trackedentityid=te.trackedentityid and ${teDeletedClause} \
            left join ${organisationunit} registrationou on te.organisationunitid=registrationou.organisationunitid \
            inner join ${organisationunit} ou on ev.organisationunitid=ou.organisationunitid \
            left join analytics_rs_dateperiodstructure dps on cast(${eventDateExpression} as date)=dps.dateperiod \
            left join analytics_rs_orgunitstructure ous on ev.organisationunitid=ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on ev.organisationunitid=ougs.organisationunitid \
            left join ${organisationunit} enrollmentou on en.organisationunitid=enrollmentou.organisationunitid \
            inner join analytics_rs_categorystructure acs on ev.attributeoptioncomboid=acs.categoryoptioncomboid \
            ${attributeJoinClause}\
            where ev.lastupdated < '${startTime}' ${partitionClause} \
            and pr.programid = ${programId} \
            and ev.organisationunitid is not null \
            and (${eventDateExpression}) is not null \
            and (ougs.startdate is null or dps.monthstartdate=ougs.startdate) \
            and dps.year >= ${firstDataYear} \
            and dps.year <= ${latestDataYear} \
            and ev.status in (${exportableEventStatues}) \
            and ev.deleted = false""",
            Map.of(
                "eventDateExpression", eventDateExpression,
                "partitionClause", partitionClause,
                "attributeJoinClause", attributeJoinClause,
                "startTime", toLongDate(params.getStartTime()),
                "programId", String.valueOf(program.getId()),
                "enDeletedClause", sqlBuilder.isFalse("en", "deleted"),
                "teDeletedClause", sqlBuilder.isFalse("te", "deleted"),
                "firstDataYear", String.valueOf(firstDataYear),
                "latestDataYear", String.valueOf(latestDataYear),
                "exportableEventStatues", join(",", EXPORTABLE_EVENT_STATUSES)));

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
    List<AnalyticsTableColumn> columns =
        new ArrayList<>(EventAnalyticsColumn.getColumns(sqlBuilder, useCentroidForOuColumns()));
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
    Set<DataElement> dataElements =
        program.getAnalyticsDataElements().stream().filter(Objects::nonNull).collect(toSet());

    columns.addAll(
        dataElements.stream()
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

    DataType dataType = getColumnType(dataElement.getValueType(), isGeospatialSupport());
    String jsonExpression =
        sqlBuilder.jsonExtract("eventdatavalues", dataElement.getUid(), "value");
    String columnExpression = getColumnExpression(dataElement.getValueType(), jsonExpression);
    String dataFilterClause = getDataFilterClause(dataElement);
    String selectExpression = getSelectExpression(dataElement, columnExpression);
    Skip skipIndex = skipIndex(dataElement.getValueType(), dataElement.hasOptionSet());

    if (withLegendSet) {
      return getColumnFromDataElementWithLegendSet(dataElement, columnExpression, dataFilterClause);
    }

    if (dataElement.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnForOrgUnitDataElement(dataElement));
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(dataElement.getUid())
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(selectExpression)
            .skipIndex(skipIndex)
            .build());

    return columns;
  }

  /**
   * Returns a select expression.
   *
   * @param dataElement the {@link DataElement}.
   * @param columnExpression the column expression.
   * @return a select expression.
   */
  private String getSelectExpression(DataElement dataElement, String columnExpression) {
    return String.format("%s as %s", columnExpression, quote(dataElement.getUid()));
  }

  /**
   * Returns a list of columns.
   *
   * @param dataElement the {@link DataElement}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForOrgUnitDataElement(DataElement dataElement) {
    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return List.of();
    }

    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (isGeospatialSupport()) {
      columns.add(
          AnalyticsTableColumn.builder()
              .name((dataElement.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(getOrgUnitSelectSubquery(dataElement, "geometry"))
              .indexType(IndexType.GIST)
              .build());
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name((dataElement.getUid() + OU_NAME_COL_SUFFIX))
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(getOrgUnitSelectSubquery(dataElement, "name"))
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * Returns a org unit select query.
   *
   * @param dataElement the {@link DataElement}.
   * @param column the column name.
   * @return an org unit select query.
   */
  private String getOrgUnitSelectSubquery(DataElement dataElement, String column) {
    String format =
        """
        (select ou.${column} from ${organisationunit} ou \
        where ou.uid = ${columnExpression}) as ${alias}""";
    String columnExpression =
        sqlBuilder.jsonExtract("eventdatavalues", dataElement.getUid(), "value");
    String alias = quote(dataElement.getUid());

    return replaceQualify(
        format,
        Map.of(
            "column", column,
            "columnExpression", columnExpression,
            "alias", alias));
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
   * Returns a list of columns based on the given attribute with legend set.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForAttributeWithLegendSet(
      TrackedEntityAttribute attribute) {
    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return List.of();
    }

    String columnExpression = getColumnExpression(attribute.getValueType(), "value");
    String numericClause = getNumericClause("value");
    String query =
        """
        \s(select l.uid from ${maplegend} l \
        inner join trackedentityattributevalue av on l.startvalue <= ${selectClause} \
        and l.endvalue > ${selectClause} \
        and l.maplegendsetid=${legendSetId} \
        and av.trackedentityid=en.trackedentityid \
        and av.trackedentityattributeid=${attributeId} ${numericClause}) as ${column}""";

    return attribute.getLegendSets().stream()
        .map(
            ls -> {
              String column = attribute.getUid() + PartitionUtils.SEP + ls.getUid();
              String selectExpression =
                  replaceQualify(
                      query,
                      Map.of(
                          "selectClause", columnExpression,
                          "legendSetId", String.valueOf(ls.getId()),
                          "column", column,
                          "attributeId", String.valueOf(attribute.getId()),
                          "numericClause", numericClause));

              return AnalyticsTableColumn.builder()
                  .name(column)
                  .dataType(CHARACTER_11)
                  .selectExpression(selectExpression)
                  .build();
            })
        .toList();
  }

  /**
   * Returns a list of columns.
   *
   * @param dataElement the {@link DataElement}.
   * @param selectExpression the select expression.
   * @param dataFilterClause the data filter clause.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet(
      DataElement dataElement, String selectExpression, String dataFilterClause) {
    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return List.of();
    }

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
   * @return a data filter clause.
   */
  private String getDataFilterClause(DataElement dataElement) {
    ValueType valueType = dataElement.getValueType();

    if (valueType.isNumeric() || valueType.isDate()) {
      String jsonExpression =
          sqlBuilder.jsonExtract("eventdatavalues", dataElement.getUid(), "value");
      String regex = valueType.isNumeric() ? NUMERIC_REGEXP : DATE_REGEXP;

      return " and " + sqlBuilder.regexpMatch(jsonExpression, regex);
    }

    return EMPTY;
  }

  /**
   * Returns a list of years for which data exist.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param program the {@link Program}.
   * @param firstDataYear the first year to include.
   * @param lastDataYear the last data year to include.
   * @return a list of years for which data exists.
   */
  private List<Integer> getDataYears(
      AnalyticsTableUpdateParams params,
      Program program,
      Integer firstDataYear,
      Integer lastDataYear) {
    String fromDate = toMediumDate(params.getFromDate());
    String fromDateClause =
        params.getFromDate() != null
            ? replace(
                "and (${eventDateExpression}) >= '${fromDate}'",
                Map.of(
                    "eventDateExpression", eventDateExpression,
                    "fromDate", fromDate))
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
   * Returns a numeric regexp match expression for the given value.
   *
   * @param value the value.
   * @return a numeric regexp match expression.
   */
  private String getNumericClause(String value) {
    return " and " + sqlBuilder.regexpMatch(value, "'" + NUMERIC_LENIENT_REGEXP + "'");
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

  private boolean useCentroidForOuColumns() {
    return settingsProvider.getCurrentSettings().getOrgUnitCentroidsInEventsAnalytics();
  }
}
