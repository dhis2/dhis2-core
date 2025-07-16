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
import static org.hisp.dhis.analytics.AnalyticsStringUtils.replaceQualify;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getEndDate;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getStartDate;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.JSONB;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.SYSTEM_DEFINED;
import static org.hisp.dhis.util.DateUtils.toLongDate;
import static org.hisp.dhis.util.DateUtils.toMediumDate;
import static org.hisp.dhis.util.TextUtils.emptyIfTrue;
import static org.hisp.dhis.util.TextUtils.format;
import static org.hisp.dhis.util.TextUtils.replace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsStringUtils;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TrackedEntityEventsAnalyticsTableManager")
public class JdbcTrackedEntityEventsAnalyticsTableManager extends AbstractJdbcTableManager {

  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("trackedentity")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("te.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("program")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("p.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("enrollment")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("en.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("programstage")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("ps.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("event")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("ev.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("occurreddate")
              .dataType(TIMESTAMP)
              .selectExpression("ev.occurreddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdated")
              .dataType(TIMESTAMP)
              .selectExpression("ev.lastupdated")
              .build(),
          AnalyticsTableColumn.builder()
              .name("created")
              .dataType(TIMESTAMP)
              .selectExpression("ev.created")
              .build(),
          AnalyticsTableColumn.builder()
              .name("scheduleddate")
              .dataType(TIMESTAMP)
              .selectExpression("ev.scheduleddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("status")
              .dataType(VARCHAR_50)
              .selectExpression("ev.status")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ou")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("ous.organisationunituid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ouname")
              .dataType(VARCHAR_255)
              .nullable(NULL)
              .selectExpression("ous.name")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oucode")
              .dataType(VARCHAR_50)
              .nullable(NULL)
              .selectExpression("ous.code")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oulevel")
              .dataType(INTEGER)
              .nullable(NULL)
              .selectExpression("ous.level")
              .build());

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final AnalyticsSqlBuilder analyticsSqlBuilder;

  public JdbcTrackedEntityEventsAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      @Qualifier("analyticsPostgresJdbcTemplate") JdbcTemplate jdbcTemplate,
      TrackedEntityTypeService trackedEntityTypeService,
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
    this.trackedEntityTypeService = trackedEntityTypeService;
    this.analyticsSqlBuilder = new PostgreSqlAnalyticsSqlBuilder();
  }

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return TRACKED_ENTITY_INSTANCE_EVENTS;
  }

  /**
   * Returns a {@link AnalyticsTable} with a list of yearly {@link AnalyticsTablePartition}.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return the analytics table with partitions.
   */
  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    Calendar calendar = PeriodType.getCalendar();
    List<TrackedEntityType> trackedEntityTypes = trackedEntityTypeService.getAllTrackedEntityType();
    List<AnalyticsTable> tables = new ArrayList<>();
    Logged logged = analyticsTableSettings.getTableLogged();

    for (TrackedEntityType tet : trackedEntityTypes) {
      List<Integer> dataYears = getDataYears(params, tet);

      Collections.sort(dataYears);

      AnalyticsTable table = new AnalyticsTable(getAnalyticsTableType(), getColumns(), logged, tet);

      for (Integer year : dataYears) {
        table.addTablePartition(
            List.of(), year, getStartDate(calendar, year), getEndDate(calendar, year));
      }

      if (table.hasTablePartitions()) {
        tables.add(table);
      }
    }

    return tables;
  }

  private List<AnalyticsTableColumn> getFixedCols() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_COLS);
    columns.add(getEventDataValueColumn());
    if (sqlBuilder.supportsGeospatialData()) {
      columns.addAll(getGeospatialCols());
    }
    return columns;
  }

  private AnalyticsTableColumn getEventDataValueColumn() {
    return AnalyticsTableColumn.builder()
        .name("eventdatavalues")
        .dataType(JSONB)
        .selectExpression(analyticsSqlBuilder.getEventDataValues())
        .skipIndex(Skip.SKIP)
        .build();
  }

  private List<AnalyticsTableColumn> getGeospatialCols() {

    return List.of(
        AnalyticsTableColumn.builder()
            .name("eventgeometry")
            .dataType(GEOMETRY)
            .selectExpression("ev.geometry")
            .indexType(IndexType.GIST)
            .build(),
        AnalyticsTableColumn.builder()
            .name("evlongitude")
            .dataType(DOUBLE)
            .selectExpression(
                "case when 'POINT' = GeometryType(ev.geometry) then ST_X(ev.geometry) end")
            .build(),
        AnalyticsTableColumn.builder()
            .name("evlatitude")
            .dataType(DOUBLE)
            .selectExpression(
                "case when 'POINT' = GeometryType(ev.geometry) then ST_Y(ev.geometry) end")
            .build());
  }

  private List<Integer> getDataYears(AnalyticsTableUpdateParams params, TrackedEntityType tet) {
    StringBuilder sql = new StringBuilder();
    sql.append(
        replaceQualify(
            sqlBuilder,
            """
            select temp.supportedyear from \
            (select distinct extract(year from ${eventDateExpression}) as supportedyear \
            from ${trackedentity} te \
            inner join ${enrollment} en on te.trackedentityid=en.trackedentityid \
            inner join ${event} ev on en.enrollmentid=ev.enrollmentid \
            where ev.lastupdated <= '${startTime}' \
            and te.trackedentitytypeid = ${tetId} \
            and (${eventDateExpression}) is not null \
            and (${eventDateExpression}) > '1000-01-01' \
            and ${evDeletedClause} \
            and ${teDeletedClause}""",
            Map.of(
                "eventDateExpression", eventDateExpression,
                "startTime", toLongDate(params.getStartTime()),
                "tetId", String.valueOf(tet.getId()),
                "evDeletedClause", sqlBuilder.isFalse("ev", "deleted"),
                "teDeletedClause", sqlBuilder.isFalse("te", "deleted"))));

    if (params.getFromDate() != null) {
      sql.append(" and (" + eventDateExpression + ") >= '")
          .append(toMediumDate(params.getFromDate()))
          .append("'");
    }

    List<Integer> availableDataYears =
        periodDataProvider.getAvailableYears(
            analyticsTableSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);
    Integer firstDataYear = availableDataYears.get(0);
    Integer latestDataYear = availableDataYears.get(availableDataYears.size() - 1);

    sql.append(
        replace(
            """
             ) as temp where temp.supportedyear >= ${firstDataYear} \
             and temp.supportedyear <= ${latestDataYear}\s""",
            Map.of(
                "firstDataYear", String.valueOf(firstDataYear),
                "latestDataYear", String.valueOf(latestDataYear))));

    return jdbcTemplate.queryForList(sql.toString(), Integer.class);
  }

  private List<AnalyticsTableColumn> getColumns() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(getFixedCols());

    if (sqlBuilder.supportsDeclarativePartitioning()) {
      columns.add(getPartitionColumn());
    }

    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(getOrganisationUnitLevelColumns());

    return columns;
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    return List.of();
  }

  /**
   * Populates the given analytics table.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partition the {@link AnalyticsTablePartition} to populate.
   */
  @Override
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    AnalyticsTable masterTable = partition.getMasterTable();
    String tableName = partition.getName();
    long tetId = masterTable.getTrackedEntityType().getId();
    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();
    String partitionClause =
        sqlBuilder.supportsDeclarativePartitioning() ? "" : getPartitionClause(partition);

    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");
    sql.append(AnalyticsStringUtils.toCommaSeparated(columns, col -> quote(col.getName())));
    sql.append(") select distinct ");
    sql.append(
        AnalyticsStringUtils.toCommaSeparated(columns, AnalyticsTableColumn::getSelectExpression));
    sql.append(" ");

    sql.append(
        replaceQualify(
            sqlBuilder,
            """
            from ${event} ev \
            inner join ${enrollment} en on en.enrollmentid=ev.enrollmentid and ${enDeletedClause} \
            inner join ${trackedentity} te on te.trackedentityid=en.trackedentityid \
            and ${teDeletedClause} and te.trackedentitytypeid = ${tetId} and te.lastupdated < '${startTime}' \
            left join ${programstage} ps on ev.programstageid=ps.programstageid \
            left join ${program} p on ps.programid=p.programid \
            left join analytics_rs_orgunitstructure ous on ev.organisationunitid=ous.organisationunitid \
            where ev.status in (${statuses}) \
            ${partitionClause} \
            and ${evDeletedClause}""",
            Map.of(
                "enDeletedClause", sqlBuilder.isFalse("en", "deleted"),
                "teDeletedClause", sqlBuilder.isFalse("te", "deleted"),
                "evDeletedClause", sqlBuilder.isFalse("ev", "deleted"),
                "tetId", String.valueOf(tetId),
                "startTime", toLongDate(params.getStartTime()),
                "statuses", join(",", EXPORTABLE_EVENT_STATUSES),
                "partitionClause", partitionClause)));

    invokeTimeAndLog(sql.toString(), "Populating table: '{}'", tableName);
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
    String latestFilter = format("and ev.lastupdated >= '{}' ", start);
    String partitionFilter =
        format(
            "and ({}) >= '{}' and ({}) < '{}' ",
            eventDateExpression,
            start,
            eventDateExpression,
            end);

    return partition.isLatestPartition()
        ? latestFilter
        : emptyIfTrue(partitionFilter, sqlBuilder.supportsDeclarativePartitioning());
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
        .selectExpression("ev.lastupdated")
        .build();
  }
}
