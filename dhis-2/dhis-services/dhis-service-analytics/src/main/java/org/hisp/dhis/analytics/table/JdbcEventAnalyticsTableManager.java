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
import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
import static org.hisp.dhis.commons.util.TextUtils.format;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.SYSTEM_DEFINED;
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
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsColumnType;
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
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
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

  public JdbcEventAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingManager systemSettingManager,
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
        systemSettingManager,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        partitionManager,
        databaseInfoProvider,
        jdbcTemplate,
        analyticsExportSettings,
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
        isSpatialSupport());

    List<Integer> availableDataYears =
        periodDataProvider.getAvailableYears(
            analyticsTableSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);

    return params.isLatestUpdate()
        ? getLatestAnalyticsTables(params)
        : getRegularAnalyticsTables(params, availableDataYears);
  }

  /**
   * Creates a list of {@link AnalyticsTable} for each program. The tables contain a partition for
   * each year for which events exist.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
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
                .toList();

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
    Date lastFullTableUpdate =
        systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE);
    Date lastLatestPartitionUpdate =
        systemSettingManager.getDateSetting(
            SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE);
    Date lastAnyTableUpdate = DateUtils.getLatest(lastLatestPartitionUpdate, lastFullTableUpdate);

    Assert.notNull(
        lastFullTableUpdate,
        "A full analytics table update process must be run prior to a latest partition update process");

    Date startDate = lastFullTableUpdate;
    Date endDate = params.getStartTime();

    List<AnalyticsTable> tables = new ArrayList<>();

    Logged logged = analyticsTableSettings.getTableLogged();

    List<Program> programs =
        params.isSkipPrograms()
            ? idObjectManager.getAllNoAcl(Program.class).stream()
                .filter(p -> !params.getSkipPrograms().contains(p.getUid()))
                .toList()
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
        replace(
            """
            select psi.eventid \
            from event psi \
            inner join enrollment pi on psi.enrollmentid=pi.enrollmentid \
            where pi.programid = ${programId} \
            and psi.lastupdated >= '${startDate}' \
            and psi.lastupdated < '${endDate}' \
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
          replace(
              """
              delete from ${tableName} ax \
              where ax.psi in ( \
              select psi.uid \
              from event psi inner join enrollment pi on psi.enrollmentid=pi.enrollmentid \
              where pi.programid = ${programId} \
              and psi.lastupdated >= '${startDate}' \
              and psi.lastupdated < '${endDate}');""",
              Map.of(
                  "tableName", quote(table.getName()),
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
        periodDataProvider.getAvailableYears(
            analyticsTableSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);
    Integer firstDataYear = availableDataYears.get(0);
    Integer latestDataYear = availableDataYears.get(availableDataYears.size() - 1);
    Program program = partition.getMasterTable().getProgram();
    String partitionClause = getPartitionClause(partition);

    String fromClause =
        replace(
            """
            \sfrom event psi \
            inner join enrollment pi on psi.enrollmentid=pi.enrollmentid \
            inner join programstage ps on psi.programstageid=ps.programstageid \
            inner join program pr on pi.programid=pr.programid and pi.deleted = false \
            inner join categoryoptioncombo ao on psi.attributeoptioncomboid=ao.categoryoptioncomboid \
            left join trackedentity tei on pi.trackedentityid=tei.trackedentityid \
            and tei.deleted = false \
            left join organisationunit registrationou on tei.organisationunitid=registrationou.organisationunitid \
            inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid \
            left join analytics_rs_orgunitstructure ous on psi.organisationunitid=ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid \
            and (cast(${eventDateMonth} as date)=ougs.startdate or ougs.startdate is null) \
            left join organisationunit enrollmentou on pi.organisationunitid=enrollmentou.organisationunitid \
            inner join analytics_rs_categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid \
            left join analytics_rs_dateperiodstructure dps on cast(${eventDateExpression} as date)=dps.dateperiod \
            where psi.lastupdated < '${startTime}' ${partitionClause} \
            and pr.programid=${programId} \
            and psi.organisationunitid is not null \
            and (${eventDateExpression}) is not null \
            and dps.year >= ${firstDataYear} \
            and dps.year <= ${latestDataYear} \
            and psi.status in (${exportableEventStatues}) \
            and psi.deleted = false""",
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
    String latestFilter = format("and psi.lastupdated >= '{}' ", start);
    String partitionFilter =
        format("and ({}) >= '{}' and ({}) < '{}' ", statusDate, start, statusDate, end);

    return partition.isLatestPartition() ? latestFilter : partitionFilter;
  }

  public List<AnalyticsTableColumn> getFixedColumns() {
    List<AnalyticsTableColumn> fixedCols = new ArrayList<>();

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("psi")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("psi.uid")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("pi")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("pi.uid")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("ps")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ps.uid")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("ao")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ao.uid")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("enrollmentdate")
            .dataType(TIMESTAMP)
            .selectExpression("pi.enrollmentdate")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("incidentdate")
            .dataType(TIMESTAMP)
            .selectExpression("pi.occurreddate")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("occurreddate")
            .dataType(TIMESTAMP)
            .selectExpression("psi.occurreddate")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("scheduleddate")
            .dataType(TIMESTAMP)
            .selectExpression("psi.scheduleddate")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("completeddate")
            .dataType(TIMESTAMP)
            .selectExpression("psi.completeddate")
            .build());

    /*
     * DHIS2-14981: Use the client-side timestamp if available, otherwise
     * the server-side timestamp. Applies to both created and lastupdated.
     */
    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("created")
            .dataType(TIMESTAMP)
            .selectExpression(firstIfNotNullOrElse("psi.createdatclient", "psi.created"))
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("lastupdated")
            .dataType(TIMESTAMP)
            .selectExpression(firstIfNotNullOrElse("psi.lastupdatedatclient", "psi.lastupdated"))
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("storedby")
            .dataType(VARCHAR_255)
            .selectExpression("psi.storedby")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("createdbyusername")
            .dataType(VARCHAR_255)
            .selectExpression("psi.createdbyuserinfo ->> 'username' as createdbyusername")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("createdbyname")
            .dataType(VARCHAR_255)
            .selectExpression("psi.createdbyuserinfo ->> 'firstName' as createdbyname")
            .skipIndex(Skip.SKIP)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("createdbylastname")
            .dataType(VARCHAR_255)
            .selectExpression("psi.createdbyuserinfo ->> 'surname' as createdbylastname")
            .skipIndex(Skip.SKIP)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("createdbydisplayname")
            .dataType(VARCHAR_255)
            .selectExpression(getDisplayName("createdbyuserinfo", "psi", "createdbydisplayname"))
            .skipIndex(Skip.SKIP)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("lastupdatedbyusername")
            .dataType(VARCHAR_255)
            .selectExpression("psi.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("lastupdatedbyname")
            .dataType(VARCHAR_255)
            .selectExpression("psi.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname")
            .skipIndex(Skip.SKIP)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("lastupdatedbylastname")
            .dataType(VARCHAR_255)
            .selectExpression("psi.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname")
            .skipIndex(Skip.SKIP)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("lastupdatedbydisplayname")
            .dataType(VARCHAR_255)
            .selectExpression(
                getDisplayName("lastupdatedbyuserinfo", "psi", "lastupdatedbydisplayname"))
            .skipIndex(Skip.SKIP)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("psistatus")
            .dataType(VARCHAR_50)
            .selectExpression("psi.status")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("pistatus")
            .dataType(VARCHAR_50)
            .selectExpression("pi.status")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("psigeometry")
            .dataType(GEOMETRY)
            .selectExpression("psi.geometry")
            .indexType(IndexType.GIST)
            .build());

    // TODO latitude and longitude deprecated in 2.30, remove in 2.33
    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("longitude")
            .dataType(DOUBLE)
            .selectExpression(
                "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_X(psi.geometry) ELSE null END")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("latitude")
            .dataType(DOUBLE)
            .selectExpression(
                "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_Y(psi.geometry) ELSE null END")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("ou")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("ou.uid")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("ouname")
            .dataType(TEXT)
            .nullable(NOT_NULL)
            .selectExpression("ou.name")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("oucode")
            .dataType(TEXT)
            .selectExpression("ou.code")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("oulevel")
            .dataType(INTEGER)
            .selectExpression("ous.level")
            .build());
    String ouGeometryExpression =
        useCentroidForOuAttribute() ? "ST_Centroid(ou.geometry)" : "ou.geometry";
    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("ougeometry")
            .dataType(GEOMETRY)
            .selectExpression(ouGeometryExpression)
            .indexType(IndexType.GIST)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("pigeometry")
            .dataType(GEOMETRY)
            .selectExpression("pi.geometry")
            .indexType(IndexType.GIST)
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("registrationou")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("coalesce(registrationou.uid,ou.uid)")
            .build());

    fixedCols.add(
        AnalyticsTableColumn.builder()
            .name("enrollmentou")
            .dataType(CHARACTER_11)
            .nullable(NOT_NULL)
            .selectExpression("coalesce(enrollmentou.uid,ou.uid)")
            .build());

    return fixedCols;
  }

  /**
   * Returns dimensional analytics table columns.
   *
   * @param program the program.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(getFixedColumns());

    if (program.hasNonDefaultCategoryCombo()) {
      List<Category> categories = program.getCategoryCombo().getCategories();

      for (Category category : categories) {
        if (category.isDataDimension()) {
          columns.add(
              AnalyticsTableColumn.builder()
                  .name(category.getUid())
                  .columnType(AnalyticsColumnType.DYNAMIC)
                  .dataType(CHARACTER_11)
                  .selectExpression("acs." + quote(category.getUid()))
                  .created(category.getCreated())
                  .build());
        }
      }
    }

    columns.addAll(getOrganisationUnitLevelColumns());
    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(getOrganisationUnitGroupSetColumns());
    columns.addAll(getAttributeCategoryOptionGroupSetColumns());
    columns.addAll(getPeriodTypeColumns("dps"));

    columns.addAll(
        program.getAnalyticsDataElements().stream()
            .map(de -> getColumnFromDataElement(de, false))
            .flatMap(Collection::stream)
            .toList());

    columns.addAll(
        program.getAnalyticsDataElementsWithLegendSet().stream()
            .map(de -> getColumnFromDataElement(de, true))
            .flatMap(Collection::stream)
            .toList());

    columns.addAll(
        program.getNonConfidentialTrackedEntityAttributes().stream()
            .map(
                tea ->
                    getColumnFromTrackedEntityAttribute(
                        tea, getNumericClause(), getDateClause(), false))
            .flatMap(Collection::stream)
            .toList());

    columns.addAll(
        program.getNonConfidentialTrackedEntityAttributesWithLegendSet().stream()
            .map(
                tea ->
                    getColumnFromTrackedEntityAttribute(
                        tea, getNumericClause(), getDateClause(), true))
            .flatMap(Collection::stream)
            .toList());

    if (program.isRegistration()) {
      columns.add(
          AnalyticsTableColumn.builder()
              .name("tei")
              .dataType(CHARACTER_11)
              .selectExpression("tei.uid")
              .build());
      columns.add(
          AnalyticsTableColumn.builder()
              .name("teigeometry")
              .dataType(GEOMETRY)
              .selectExpression("tei.geometry")
              .build());
    }

    return filterDimensionColumns(columns);
  }

  private List<AnalyticsTableColumn> getColumnFromTrackedEntityAttribute(
      TrackedEntityAttribute attribute,
      String numericClause,
      String dateClause,
      boolean withLegendSet) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    DataType dataType = getColumnType(attribute.getValueType(), isSpatialSupport());
    String dataClause =
        attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
    String select = getSelectClause(attribute.getValueType(), "value");
    String sql = selectForInsert(attribute, select, dataClause);
    Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

    if (attribute.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnsFromOrgUnitTrackedEntityAttribute(attribute, dataClause));
    }
    columns.add(
        AnalyticsTableColumn.builder()
            .name(attribute.getUid())
            .columnType(AnalyticsColumnType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(sql)
            .skipIndex(skipIndex)
            .build());

    return withLegendSet
        ? getColumnFromTrackedEntityAttributeWithLegendSet(attribute, numericClause)
        : columns;
  }

  private List<AnalyticsTableColumn> getColumnFromTrackedEntityAttributeWithLegendSet(
      TrackedEntityAttribute attribute, String numericClause) {
    String selectClause = getSelectClause(attribute.getValueType(), "value");
    String query =
        """
          \s(select l.uid from maplegend l \
          inner join trackedentityattributevalue av on l.startvalue <= ${selectClause} \
          and l.endvalue > ${selectClause} \
          and l.maplegendsetid=${legendSetId} \
          and av.trackedentityid=pi.trackedentityid \
          and av.trackedentityattributeid=${attributeId} ${numericClause}) as ${column}""";

    return attribute.getLegendSets().stream()
        .map(
            ls -> {
              String column = attribute.getUid() + PartitionUtils.SEP + ls.getUid();
              String sql =
                  replace(
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
        .collect(toList());
  }

  private List<AnalyticsTableColumn> getColumnFromDataElement(
      DataElement dataElement, boolean withLegendSet) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    DataType dataType = getColumnType(dataElement.getValueType(), isSpatialSupport());
    String dataClause = getDataClause(dataElement.getUid(), dataElement.getValueType());
    String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";
    String select = getSelectClause(dataElement.getValueType(), columnName);
    String sql = selectForInsert(dataElement, select, dataClause);
    Skip skipIndex = skipIndex(dataElement.getValueType(), dataElement.hasOptionSet());

    if (dataElement.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnFromOrgUnitDataElement(dataElement, dataClause));
    }
    columns.add(
        AnalyticsTableColumn.builder()
            .name(dataElement.getUid())
            .columnType(AnalyticsColumnType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(sql)
            .skipIndex(skipIndex)
            .build());

    return withLegendSet
        ? getColumnFromDataElementWithLegendSet(dataElement, select, dataClause)
        : columns;
  }

  private List<AnalyticsTableColumn> getColumnsFromOrgUnitTrackedEntityAttribute(
      TrackedEntityAttribute attribute, String dataClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (isSpatialSupport()) {
      String geoSql =
          selectForInsertWithoutAlias(
              attribute,
              "ou.geometry from organisationunit ou where ou.uid = (select value",
              dataClause);
      // wraps the expression with ST_Centroid if configured to do so
      String finalSqlSnippet = wrapWithCentroid(geoSql) + " as " + quote(attribute.getUid());
      columns.add(
          AnalyticsTableColumn.builder()
              .name((attribute.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .columnType(AnalyticsColumnType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(finalSqlSnippet)
              .indexType(IndexType.GIST)
              .build());
    }

    String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select value";
    String ouNameSql = selectForInsert(attribute, fromTypeSql, dataClause);

    columns.add(
        AnalyticsTableColumn.builder()
            .name((attribute.getUid() + OU_NAME_COL_SUFFIX))
            .columnType(AnalyticsColumnType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(ouNameSql)
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  private List<AnalyticsTableColumn> getColumnFromOrgUnitDataElement(
      DataElement dataElement, String dataClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";

    if (isSpatialSupport()) {
      String geoSql =
          selectForInsertWithoutAlias(
              dataElement,
              "ou.geometry from organisationunit ou where ou.uid = (select " + columnName,
              dataClause);
      // wraps the expression with ST_Centroid if configured to do so
      String finalSqlSnippet = wrapWithCentroid(geoSql) + " as " + quote(dataElement.getUid());
      columns.add(
          AnalyticsTableColumn.builder()
              .name((dataElement.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .columnType(AnalyticsColumnType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(finalSqlSnippet)
              .indexType(IndexType.GIST)
              .build());
    }

    String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select " + columnName;
    String ouNameSql = selectForInsert(dataElement, fromTypeSql, dataClause);

    columns.add(
        AnalyticsTableColumn.builder()
            .name((dataElement.getUid() + OU_NAME_COL_SUFFIX))
            .columnType(AnalyticsColumnType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(ouNameSql)
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * Wraps the base query format with ST_Centroid if the current settings allow it.
   *
   * @param baseFormat the base SQL format string.
   * @return the format string, optionally wrapped with ST_Centroid.
   */
  private String wrapWithCentroid(String baseFormat) {
    return useCentroidForOuAttribute() ? "ST_Centroid(" + baseFormat + ")" : baseFormat;
  }

  /**
   * Returns a SQL expression that returns the first argument if it is not null, otherwise the
   * second argument.
   *
   * @param first the first argument
   * @param second the second argument
   * @return a SQL expression
   */
  private static String firstIfNotNullOrElse(String first, String second) {
    return "case when " + first + " is not null then " + first + " else " + second + " end";
  }

  private String selectForInsert(DataElement dataElement, String fromType, String dataClause) {
    return replace(
        """
        (select ${fromType} from event \
        where eventid=psi.eventid ${dataClause})${closingParentheses} as ${dataElementUid}""",
        Map.of(
            "fromType",
            fromType,
            "dataClause",
            dataClause,
            "closingParentheses",
            getClosingParentheses(fromType),
            "dataElementUid",
            quote(dataElement.getUid())));
  }

  private String selectForInsertWithoutAlias(
      DataElement dataElement, String fromType, String dataClause) {
    String sqlWithAlias = selectForInsert(dataElement, fromType, dataClause);
    return ALIAS_PATTERN.matcher(sqlWithAlias).replaceAll("");
  }

  private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet(
      DataElement dataElement, String select, String dataClause) {
    String query =
        """
        (select l.uid from maplegend l
        inner join event on l.startvalue <= ${select}
        and l.endvalue > ${select}
        and l.maplegendsetid=${legendSetId}
        and eventid=psi.eventid ${dataClause}) as ${column}""";
    return dataElement.getLegendSets().stream()
        .map(
            ls -> {
              String column = dataElement.getUid() + PartitionUtils.SEP + ls.getUid();
              String sql =
                  replace(
                      query,
                      Map.of(
                          "select", select,
                          "legendSetId", String.valueOf(ls.getId()),
                          "dataClause", dataClause,
                          "column", column));

              return AnalyticsTableColumn.builder()
                  .name(column)
                  .dataType(CHARACTER_11)
                  .selectExpression(sql)
                  .build();
            })
        .collect(toList());
  }

  private String getDataClause(String uid, ValueType valueType) {
    if (valueType.isNumeric() || valueType.isDate()) {
      String regex = valueType.isNumeric() ? NUMERIC_LENIENT_REGEXP : DATE_REGEXP;

      return replace(
          " and eventdatavalues #>> '{${uid},value}' ~* '${regex}'",
          Map.of("uid", uid, "regex", regex));
    }

    return "";
  }

  private List<Integer> getDataYears(
      AnalyticsTableUpdateParams params,
      Program program,
      Integer firstDataYear,
      Integer latestDataYear) {
    String fromDateClause =
        params.getFromDate() != null
            ? replace(
                "and (${eventDateExpression}) >= '${fromDate}'",
                Map.of(
                    "eventDateExpression",
                    eventDateExpression,
                    "fromDate",
                    toMediumDate(params.getFromDate())))
            : "";
    String sql =
        replace(
            """
            select temp.supportedyear from \
            (select distinct extract(year from ${eventDateExpression}) as supportedyear \
            from event psi \
            inner join enrollment pi on psi.enrollmentid = pi.enrollmentid \
            where psi.lastupdated <= '${startTime}' \
            and pi.programid = ${programId} \
            and (${eventDateExpression}) is not null \
            and (${eventDateExpression}) > '1000-01-01' \
            and psi.deleted = false \
            ${fromDateClause}) as temp \
            where temp.supportedyear >= ${firstDataYear} \
            and temp.supportedyear <= ${latestDataYear}""",
            Map.of(
                "eventDateExpression", eventDateExpression,
                "startTime", toLongDate(params.getStartTime()),
                "programId", String.valueOf(program.getId()),
                "fromDateClause", fromDateClause,
                "firstDataYear", String.valueOf(firstDataYear),
                "latestDataYear", String.valueOf(latestDataYear)));

    return jdbcTemplate.queryForList(sql, Integer.class);
  }

  /**
   * Retrieve years for partition tables. Year will become a partition key. The default return value
   * is the list with the recent year.
   *
   * @param dataYears list of years coming from inner join of event and enrollment tables
   * @return list of partition key values
   */
  private List<Integer> getYearsForPartitionTable(List<Integer> dataYears) {
    return ListUtils.mutableCopy(!dataYears.isEmpty() ? dataYears : List.of(Year.now().getValue()));
  }

  /**
   * Check the relevant system setting to determine whether TEA OU attribute should be rendered as
   * ST_CENTROID.
   *
   * @return true if the setting is enabled, false otherwise.
   * @see SettingKey#ANALYTICS_EVENTS_OU_CENTROID
   */
  private boolean useCentroidForOuAttribute() {
    return systemSettingManager.getBooleanSetting(SettingKey.ANALYTICS_EVENTS_OU_CENTROID);
  }
}
