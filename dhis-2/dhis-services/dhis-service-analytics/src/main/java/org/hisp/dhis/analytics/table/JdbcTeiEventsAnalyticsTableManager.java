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
import static java.lang.String.join;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getEndDate;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getStartDate;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.CHARACTER_32;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.JSONB;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.SYSTEM_DEFINED;
import static org.hisp.dhis.util.DateUtils.toLongDate;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TeiEventsAnalyticsTableManager")
public class JdbcTeiEventsAnalyticsTableManager extends AbstractJdbcTableManager {
  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          new AnalyticsTableColumn("trackedentityinstanceuid", CHARACTER_11, NOT_NULL, "tei.uid"),
          new AnalyticsTableColumn("programuid", CHARACTER_11, NULL, "p.uid"),
          new AnalyticsTableColumn("programinstanceuid", CHARACTER_11, NULL, "pi.uid"),
          new AnalyticsTableColumn("programstageuid", CHARACTER_11, NULL, "ps.uid"),
          new AnalyticsTableColumn("programstageinstanceuid", CHARACTER_11, NULL, "psi.uid"),
          new AnalyticsTableColumn("occurreddate", TIMESTAMP, "psi.occurreddate"),
          new AnalyticsTableColumn("lastupdated", TIMESTAMP, "psi.lastupdated"),
          new AnalyticsTableColumn("created", TIMESTAMP, "psi.created"),
          new AnalyticsTableColumn("scheduleddate", TIMESTAMP, "psi.scheduleddate"),
          new AnalyticsTableColumn("status", VARCHAR_50, "psi.status"),
          new AnalyticsTableColumn("psigeometry", GEOMETRY, "psi.geometry", IndexType.GIST),
          new AnalyticsTableColumn(
              "psilongitude",
              DOUBLE,
              "case when 'POINT' = GeometryType(psi.geometry) then ST_X(psi.geometry) end"),
          new AnalyticsTableColumn(
              "psilatitude",
              DOUBLE,
              "case when 'POINT' = GeometryType(psi.geometry) then ST_Y(psi.geometry) end"),
          new AnalyticsTableColumn("uidlevel1", CHARACTER_11, NULL, "ous.uidlevel1"),
          new AnalyticsTableColumn("uidlevel2", CHARACTER_11, NULL, "ous.uidlevel2"),
          new AnalyticsTableColumn("uidlevel3", CHARACTER_11, NULL, "ous.uidlevel3"),
          new AnalyticsTableColumn("uidlevel4", CHARACTER_11, NULL, "ous.uidlevel4"),
          new AnalyticsTableColumn("ou", CHARACTER_11, NULL, "ou.uid"),
          new AnalyticsTableColumn("ouname", VARCHAR_255, NULL, "ou.name"),
          new AnalyticsTableColumn("oucode", CHARACTER_32, NULL, "ou.code"),
          new AnalyticsTableColumn("oulevel", INTEGER, NULL, "ous.level"),
          new AnalyticsTableColumn("eventdatavalues", JSONB, "psi.eventdatavalues"));

  private static final String AND = " and (";

  private final TrackedEntityTypeService trackedEntityTypeService;

  public JdbcTeiEventsAnalyticsTableManager(
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
      TrackedEntityTypeService trackedEntityTypeService,
      AnalyticsTableSettings analyticsTableSettings,
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
        analyticsTableSettings,
        periodDataProvider,
        sqlBuilder);
    this.trackedEntityTypeService = trackedEntityTypeService;
  }

  /**
   * Returns the {@link AnalyticsTableType} of analytics table which this manager handles.
   *
   * @return type of analytics table.
   */
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

  private List<Integer> getDataYears(AnalyticsTableUpdateParams params, TrackedEntityType tet) {
    StringBuilder sql = new StringBuilder();
    sql.append(
        replace(
            """
        select temp.supportedyear from \
        (select distinct extract(year from ${eventDateExpression}) as supportedyear \
        from trackedentity tei \
        inner join trackedentitytype tet on tet.trackedentitytypeid = tei.trackedentitytypeid \
        inner join enrollment pi on pi.trackedentityid = tei.trackedentityid \
        inner join event psi on psi.enrollmentid = pi.enrollmentid \
        where psi.lastupdated <= '${startTime}' \
        and tet.trackedentitytypeid = ${tetId} \
        and (${eventDateExpression}) is not null \
        and (${eventDateExpression}) > '1000-01-01' \
        and psi.deleted = false \
        and tei.deleted = false\s""",
            Map.of(
                "eventDateExpression", eventDateExpression,
                "startTime", toLongDate(params.getStartTime()),
                "tetId", String.valueOf(tet.getId()))));

    if (params.getFromDate() != null) {
      sql.append(AND + eventDateExpression + ") >= '" + toMediumDate(params.getFromDate()) + "'");
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
    columns.addAll(FIXED_COLS);
    columns.add(getOrganisationUnitNameHierarchyColumn());

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
    String tableName = partition.getName();
    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();
    String partitionClause = getPartitionClause(partition);

    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");

    for (AnalyticsTableColumn col : columns) {
      sql.append(quote(col.getName()) + ",");
    }

    removeLastComma(sql).append(") select distinct ");

    for (AnalyticsTableColumn col : columns) {
      sql.append(col.getSelectExpression() + ",");
    }

    removeLastComma(sql)
        .append(
            replace(
                """
        \s from event psi \
        inner join enrollment pi on pi.enrollmentid = psi.enrollmentid \
        and pi.deleted = false \
        inner join trackedentity tei on tei.trackedentityid = pi.trackedentityid \
        and tei.deleted = false \
        and tei.trackedentitytypeid = ${tetId} \
        and tei.lastupdated < '${startTime}' \
        left join programstage ps on ps.programstageid = psi.programstageid \
        left join program p on p.programid = ps.programid \
        left join organisationunit ou on psi.organisationunitid = ou.organisationunitid \
        left join analytics_rs_orgunitstructure ous on ous.organisationunitid = ou.organisationunitid \
        where psi.status in (${statuses}) \
        ${partitionClause} \
        and psi.deleted = false\s""",
                Map.of(
                    "tetId",
                        String.valueOf(partition.getMasterTable().getTrackedEntityType().getId()),
                    "startTime", toLongDate(params.getStartTime()),
                    "statuses", join(",", EXPORTABLE_EVENT_STATUSES),
                    "partitionClause", partitionClause)));

    invokeTimeAndLog(sql.toString(), tableName);
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
    String latestFilter = format("and psi.lastupdated >= '%s' ", start);
    String partitionFilter =
        replace(
            "and (${eventDateExpression}) >= '${start}' and (${eventDateExpression}) < '${end}' ",
            Map.of(
                "eventDateExpression", eventDateExpression,
                "start", start,
                "end", end));

    return partition.isLatestPartition() ? latestFilter : partitionFilter;
  }
}
