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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
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
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableExportSettings;
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
  public static final String OU_NAME_COL_SUFFIX = "_name";

  public static final String OU_GEOMETRY_COL_SUFFIX = "_geom";

  static final String[] EXPORTABLE_EVENT_STATUSES = {"'COMPLETED'", "'ACTIVE'", "'SCHEDULE'"};

  protected static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          new AnalyticsTableColumn("psi", CHARACTER_11, NOT_NULL, "psi.uid"),
          new AnalyticsTableColumn("pi", CHARACTER_11, NOT_NULL, "pi.uid"),
          new AnalyticsTableColumn("ps", CHARACTER_11, NOT_NULL, "ps.uid"),
          new AnalyticsTableColumn("ao", CHARACTER_11, NOT_NULL, "ao.uid"),
          new AnalyticsTableColumn("enrollmentdate", TIMESTAMP, "pi.enrollmentdate"),
          new AnalyticsTableColumn("incidentdate", TIMESTAMP, "pi.occurreddate"),
          new AnalyticsTableColumn("occurreddate", TIMESTAMP, "psi.occurreddate"),
          new AnalyticsTableColumn("scheduleddate", TIMESTAMP, "psi.scheduleddate"),
          new AnalyticsTableColumn("completeddate", TIMESTAMP, "psi.completeddate"),
          /*
           * DHIS2-14981: Use the client-side timestamp if available, otherwise
           * the server-side timestamp. Applies to both created and lastupdated.
           */
          new AnalyticsTableColumn(
              "created", TIMESTAMP, firstIfNotNullOrElse("psi.createdatclient", "psi.created")),
          new AnalyticsTableColumn(
              "lastupdated",
              TIMESTAMP,
              firstIfNotNullOrElse("psi.lastupdatedatclient", "psi.lastupdated")),
          new AnalyticsTableColumn("storedby", VARCHAR_255, "psi.storedby"),
          new AnalyticsTableColumn(
              "createdbyusername",
              VARCHAR_255,
              "psi.createdbyuserinfo ->> 'username' as createdbyusername"),
          new AnalyticsTableColumn(
              "createdbyname",
              VARCHAR_255,
              "psi.createdbyuserinfo ->> 'firstName' as createdbyname"),
          new AnalyticsTableColumn(
              "createdbylastname",
              VARCHAR_255,
              "psi.createdbyuserinfo ->> 'surname' as createdbylastname"),
          new AnalyticsTableColumn(
              "createdbydisplayname",
              VARCHAR_255,
              getDisplayName("createdbyuserinfo", "psi", "createdbydisplayname")),
          new AnalyticsTableColumn(
              "lastupdatedbyusername",
              VARCHAR_255,
              "psi.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername"),
          new AnalyticsTableColumn(
              "lastupdatedbyname",
              VARCHAR_255,
              "psi.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname"),
          new AnalyticsTableColumn(
              "lastupdatedbylastname",
              VARCHAR_255,
              "psi.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname"),
          new AnalyticsTableColumn(
              "lastupdatedbydisplayname",
              VARCHAR_255,
              getDisplayName("lastupdatedbyuserinfo", "psi", "lastupdatedbydisplayname")),
          new AnalyticsTableColumn("pistatus", VARCHAR_50, "pi.status"),
          new AnalyticsTableColumn("psistatus", VARCHAR_50, "psi.status"),
          new AnalyticsTableColumn("psigeometry", GEOMETRY, "psi.geometry", IndexType.GIST),
          // TODO latitude and longitude deprecated in 2.30, remove in 2.33
          new AnalyticsTableColumn(
              "longitude",
              DOUBLE,
              "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_X(psi.geometry) ELSE null END"),
          new AnalyticsTableColumn(
              "latitude",
              DOUBLE,
              "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_Y(psi.geometry) ELSE null END"),
          new AnalyticsTableColumn("ou", CHARACTER_11, NOT_NULL, "ou.uid"),
          new AnalyticsTableColumn("ouname", TEXT, NOT_NULL, "ou.name"),
          new AnalyticsTableColumn("oucode", TEXT, "ou.code"),
          new AnalyticsTableColumn("oulevel", INTEGER, "ous.level"),
          new AnalyticsTableColumn("ougeometry", GEOMETRY, "ou.geometry", IndexType.GIST),
          new AnalyticsTableColumn("pigeometry", GEOMETRY, "pi.geometry", IndexType.GIST),
          new AnalyticsTableColumn(
              "registrationou", CHARACTER_11, NOT_NULL, "coalesce(registrationou.uid,ou.uid)"),
          new AnalyticsTableColumn(
              "enrollmentou", CHARACTER_11, NOT_NULL, "coalesce(enrollmentou.uid,ou.uid)"));

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
      AnalyticsTableExportSettings analyticsExportSettings,
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
            analyticsExportSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);

    return params.isLatestUpdate()
        ? getLatestAnalyticsTables(params)
        : getRegularAnalyticsTables(params, availableDataYears);
  }

  /**
   * This method encapsulates the SQL logic to get the correct date column based on the
   * event(program stage instance) status. If new statuses need to be loaded into the analytics
   * events tables, they have to be supported/added into this logic.
   *
   * @return a statement that returns the date column related to the event(program stage instance)
   *     status
   */
  static String getDateLinkedToStatus() {
    return "CASE WHEN 'SCHEDULE' = psi.status THEN psi.scheduleddate ELSE psi.occurreddate END";
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
    Logged logged = analyticsExportSettings.getTableLogged();

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

    Logged logged = analyticsExportSettings.getTableLogged();

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
            getLongDateString(startDate),
            getLongDateString(endDate));
      } else {
        log.info(
            "No updated latest event data found for program: '{}' with start: '{}' and end: '{}",
            program.getUid(),
            getLongDateString(lastAnyTableUpdate),
            getLongDateString(endDate));
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
        "select psi.eventid "
            + "from event psi "
            + "inner join enrollment pi on psi.enrollmentid=pi.enrollmentid "
            + "where pi.programid = "
            + program.getId()
            + " "
            + "and psi.lastupdated >= '"
            + getLongDateString(startDate)
            + "' "
            + "and psi.lastupdated < '"
            + getLongDateString(endDate)
            + "' "
            + "limit 1";

    return !jdbcTemplate.queryForList(sql).isEmpty();
  }

  @Override
  public void removeUpdatedData(List<AnalyticsTable> tables) {
    for (AnalyticsTable table : tables) {
      AnalyticsTablePartition partition = table.getLatestTablePartition();

      String sql =
          "delete from "
              + quote(table.getName())
              + " ax "
              + "where ax.psi in ("
              + "select psi.uid "
              + "from event psi "
              + "inner join enrollment pi on psi.enrollmentid=pi.enrollmentid "
              + "where pi.programid = "
              + table.getProgram().getId()
              + " "
              + "and psi.lastupdated >= '"
              + getLongDateString(partition.getStartDate())
              + "' "
              + "and psi.lastupdated < '"
              + getLongDateString(partition.getEndDate())
              + "')";

      invokeTimeAndLog(sql, format("Remove updated events for table: '%s'", table.getName()));
    }
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    Objects.requireNonNull(year);
    return List.of("yearly = '" + year + "'");
  }

  @Override
  protected void populateTable(
      AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    List<Integer> availableDataYears =
        periodDataProvider.getAvailableYears(
            analyticsExportSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);
    Integer firstDataYear = availableDataYears.get(0);
    Integer latestDataYear = availableDataYears.get(availableDataYears.size() - 1);

    Program program = partition.getMasterTable().getProgram();
    String start = DateUtils.getLongDateString(partition.getStartDate());
    String end = DateUtils.getLongDateString(partition.getEndDate());
    String partitionClause =
        partition.isLatestPartition()
            ? "and psi.lastupdated >= '" + start + "' "
            : "and "
                + "("
                + getDateLinkedToStatus()
                + ") >= '"
                + start
                + "' "
                + "and "
                + "("
                + getDateLinkedToStatus()
                + ") < '"
                + end
                + "' ";

    String fromClause =
        "from event psi "
            + "inner join enrollment pi on psi.enrollmentid=pi.enrollmentid "
            + "inner join programstage ps on psi.programstageid=ps.programstageid "
            + "inner join program pr on pi.programid=pr.programid and pi.deleted is false "
            + "inner join categoryoptioncombo ao on psi.attributeoptioncomboid=ao.categoryoptioncomboid "
            + "left join trackedentity tei on pi.trackedentityid=tei.trackedentityid "
            + "and tei.deleted is false "
            + "left join organisationunit registrationou on tei.organisationunitid=registrationou.organisationunitid "
            + "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid "
            + "left join _orgunitstructure ous on psi.organisationunitid=ous.organisationunitid "
            + "left join _organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid "
            + "and (cast(date_trunc('month', "
            + getDateLinkedToStatus()
            + ") as date)"
            + "=ougs.startdate or ougs.startdate is null) "
            + "left join organisationunit enrollmentou on pi.organisationunitid=enrollmentou.organisationunitid "
            + "inner join _categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid "
            + "left join _dateperiodstructure dps on cast("
            + getDateLinkedToStatus()
            + " as date)=dps.dateperiod "
            + "where psi.lastupdated < '"
            + getLongDateString(params.getStartTime())
            + "' "
            + partitionClause
            + "and pr.programid="
            + program.getId()
            + " "
            + "and psi.organisationunitid is not null "
            + "and ("
            + getDateLinkedToStatus()
            + ") is not null "
            + "and dps.year >= "
            + firstDataYear
            + " "
            + "and dps.year <= "
            + latestDataYear
            + " "
            + "and psi.status in ("
            + String.join(",", EXPORTABLE_EVENT_STATUSES)
            + ")"
            + "and psi.deleted is false ";

    populateTableInternal(partition, fromClause);
  }

  /**
   * Returns dimensional analytics table columns.
   *
   * @param program the program.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (program.hasNonDefaultCategoryCombo()) {
      List<Category> categories = program.getCategoryCombo().getCategories();

      for (Category category : categories) {
        if (category.isDataDimension()) {
          columns.add(
              new AnalyticsTableColumn(
                  category.getUid(),
                  CHARACTER_11,
                  "acs." + quote(category.getUid()),
                  category.getCreated()));
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
            .collect(Collectors.toList()));

    columns.addAll(
        program.getAnalyticsDataElementsWithLegendSet().stream()
            .map(de -> getColumnFromDataElement(de, true))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
    ;

    columns.addAll(
        program.getNonConfidentialTrackedEntityAttributes().stream()
            .map(
                tea ->
                    getColumnFromTrackedEntityAttribute(
                        tea, getNumericClause(), getDateClause(), false))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

    columns.addAll(
        program.getNonConfidentialTrackedEntityAttributesWithLegendSet().stream()
            .map(
                tea ->
                    getColumnFromTrackedEntityAttribute(
                        tea, getNumericClause(), getDateClause(), true))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));

    columns.addAll(FIXED_COLS);

    if (program.isRegistration()) {
      columns.add(new AnalyticsTableColumn("tei", CHARACTER_11, "tei.uid"));
      columns.add(new AnalyticsTableColumn("teigeometry", GEOMETRY, "tei.geometry"));
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

    columns.add(new AnalyticsTableColumn(attribute.getUid(), dataType, sql, skipIndex));

    return withLegendSet
        ? getColumnFromTrackedEntityAttributeWithLegendSet(attribute, numericClause)
        : columns;
  }

  private List<AnalyticsTableColumn> getColumnFromTrackedEntityAttributeWithLegendSet(
      TrackedEntityAttribute attribute, String numericClause) {
    String select = getSelectClause(attribute.getValueType(), "value");

    return attribute.getLegendSets().stream()
        .map(
            ls -> {
              String column = attribute.getUid() + PartitionUtils.SEP + ls.getUid();

              String sql =
                  "(select l.uid from maplegend l "
                      + "inner join trackedentityattributevalue av on l.startvalue <= "
                      + select
                      + " "
                      + "and l.endvalue > "
                      + select
                      + " "
                      + "and l.maplegendsetid="
                      + ls.getId()
                      + " "
                      + "and av.trackedentityid=pi.trackedentityid "
                      + "and av.trackedentityattributeid="
                      + attribute.getId()
                      + numericClause
                      + ") as "
                      + column;

              return new AnalyticsTableColumn(column, CHARACTER_11, sql);
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

    columns.add(new AnalyticsTableColumn(dataElement.getUid(), dataType, sql, skipIndex));

    return withLegendSet
        ? getColumnFromDataElementWithLegendSet(dataElement, select, dataClause)
        : columns;
  }

  private List<AnalyticsTableColumn> getColumnsFromOrgUnitTrackedEntityAttribute(
      TrackedEntityAttribute attribute, String dataClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (isSpatialSupport()) {
      String geoSql =
          selectForInsert(
              attribute,
              "ou.geometry from organisationunit ou where ou.uid = (select value",
              dataClause);
      columns.add(
          new AnalyticsTableColumn(
              (attribute.getUid() + OU_GEOMETRY_COL_SUFFIX), GEOMETRY, geoSql, IndexType.GIST));
    }

    // Add org unit name column
    String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select value";
    String ouNameSql = selectForInsert(attribute, fromTypeSql, dataClause);

    columns.add(
        new AnalyticsTableColumn((attribute.getUid() + OU_NAME_COL_SUFFIX), TEXT, ouNameSql, SKIP));

    return columns;
  }

  private List<AnalyticsTableColumn> getColumnFromOrgUnitDataElement(
      DataElement dataElement, String dataClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";

    if (isSpatialSupport()) {
      String geoSql =
          selectForInsert(
              dataElement,
              "ou.geometry from organisationunit ou where ou.uid = (select " + columnName,
              dataClause);

      columns.add(
          new AnalyticsTableColumn(
              (dataElement.getUid() + OU_GEOMETRY_COL_SUFFIX), GEOMETRY, geoSql, IndexType.GIST));
    }

    // Add org unit name column
    String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select " + columnName;
    String ouNameSql = selectForInsert(dataElement, fromTypeSql, dataClause);

    columns.add(
        new AnalyticsTableColumn(
            (dataElement.getUid() + OU_NAME_COL_SUFFIX), TEXT, ouNameSql, SKIP));

    return columns;
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
    return "CASE WHEN " + first + " IS NOT NULL THEN " + first + " ELSE " + second + " END";
  }

  private String selectForInsert(DataElement dataElement, String fromType, String dataClause) {
    return format(
        "(select %s from event where eventid=psi.eventid "
            + dataClause
            + ")"
            + getClosingParentheses(fromType)
            + " as "
            + quote(dataElement.getUid()),
        fromType);
  }

  private String selectForInsert(
      TrackedEntityAttribute attribute, String fromType, String dataClause) {
    return format(
        "(select %s"
            + " from trackedentityattributevalue where trackedentityid=pi.trackedentityid "
            + "and trackedentityattributeid="
            + attribute.getId()
            + dataClause
            + ")"
            + getClosingParentheses(fromType)
            + " as "
            + quote(attribute.getUid()),
        fromType);
  }

  private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet(
      DataElement dataElement, String select, String dataClause) {
    return dataElement.getLegendSets().stream()
        .map(
            ls -> {
              String column = dataElement.getUid() + PartitionUtils.SEP + ls.getUid();

              String sql =
                  "(select l.uid from maplegend l "
                      + "inner join event on l.startvalue <= "
                      + select
                      + " "
                      + "and l.endvalue > "
                      + select
                      + " "
                      + "and l.maplegendsetid="
                      + ls.getId()
                      + " "
                      + "and eventid=psi.eventid "
                      + dataClause
                      + ") as "
                      + column;
              return new AnalyticsTableColumn(column, CHARACTER_11, sql);
            })
        .collect(toList());
  }

  private String getDataClause(String uid, ValueType valueType) {
    if (valueType.isNumeric() || valueType.isDate()) {
      String regex = valueType.isNumeric() ? NUMERIC_LENIENT_REGEXP : DATE_REGEXP;

      return " and eventdatavalues #>> '{" + uid + ",value}' ~* '" + regex + "'";
    }

    return "";
  }

  private List<Integer> getDataYears(
      AnalyticsTableUpdateParams params,
      Program program,
      Integer firstDataYear,
      Integer latestDataYear) {
    String sql =
        "select temp.supportedyear from "
            + "(select distinct extract(year from "
            + getDateLinkedToStatus()
            + ") as supportedyear "
            + "from event psi "
            + "inner join enrollment pi on psi.enrollmentid = pi.enrollmentid "
            + "where psi.lastupdated <= '"
            + getLongDateString(params.getStartTime())
            + "' "
            + "and pi.programid = "
            + program.getId()
            + " "
            + "and ("
            + getDateLinkedToStatus()
            + ") is not null "
            + "and ("
            + getDateLinkedToStatus()
            + ") > '1000-01-01' "
            + "and psi.deleted is false ";

    if (params.getFromDate() != null) {
      sql +=
          "and ("
              + getDateLinkedToStatus()
              + ") >= '"
              + DateUtils.getMediumDateString(params.getFromDate())
              + "'";
    }

    sql +=
        ") as temp where temp.supportedyear >= "
            + firstDataYear
            + " and temp.supportedyear <= "
            + latestDataYear;

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
}
