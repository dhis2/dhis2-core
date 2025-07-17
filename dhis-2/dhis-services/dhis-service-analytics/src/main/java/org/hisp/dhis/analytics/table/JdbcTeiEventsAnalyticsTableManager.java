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

import static java.lang.String.join;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getEndDate;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getStartDate;
import static org.hisp.dhis.commons.util.TextUtils.format;
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
import org.hisp.dhis.analytics.table.model.Skip;
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

  private static final String EVENT_DATA_VALUE_REBUILDER =
      """
            (select json_object_agg(l2.keys, l2.datavalue) as value
             from (select l1.uid,
                          l1.keys,
                          json_strip_nulls(json_build_object(
                                  'value', l1.eventdatavalues -> l1.keys ->> 'value',
                                  'created', l1.eventdatavalues -> l1.keys ->> 'created',
                                  'storedBy', l1.eventdatavalues -> l1.keys ->> 'storedBy',
                                  'lastUpdated', l1.eventdatavalues -> l1.keys ->> 'lastUpdated',
                                  'providedElsewhere', l1.eventdatavalues -> l1.keys -> 'providedElsewhere',
                                  'value_name', (select ou.name
                                                 from organisationunit ou
                                                 where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'),
                                  'value_code', (select ou.code
                                                 from organisationunit ou
                                                 where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'))) as datavalue
                   from (select inner_evt.*, jsonb_object_keys(inner_evt.eventdatavalues) keys
                         from event inner_evt) as l1) as l2
             where l2.uid = psi.uid
             group by l2.uid)::jsonb
      """;

  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("trackedentityinstanceuid")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("tei.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("programuid")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("p.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("programinstanceuid")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("pi.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("programstageuid")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("ps.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("programstageinstanceuid")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("psi.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("occurreddate")
              .dataType(TIMESTAMP)
              .selectExpression("psi.occurreddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdated")
              .dataType(TIMESTAMP)
              .selectExpression("psi.lastupdated")
              .build(),
          AnalyticsTableColumn.builder()
              .name("created")
              .dataType(TIMESTAMP)
              .selectExpression("psi.created")
              .build(),
          AnalyticsTableColumn.builder()
              .name("scheduleddate")
              .dataType(TIMESTAMP)
              .selectExpression("psi.scheduleddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("status")
              .dataType(VARCHAR_50)
              .selectExpression("psi.status")
              .build(),
          AnalyticsTableColumn.builder()
              .name("psigeometry")
              .dataType(GEOMETRY)
              .selectExpression("psi.geometry")
              .indexType(IndexType.GIST)
              .build(),
          AnalyticsTableColumn.builder()
              .name("psilongitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "case when 'POINT' = GeometryType(psi.geometry) then ST_X(psi.geometry) end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("psilatitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "case when 'POINT' = GeometryType(psi.geometry) then ST_Y(psi.geometry) end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ou")
              .dataType(CHARACTER_11)
              .nullable(NULL)
              .selectExpression("ou.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ouname")
              .dataType(VARCHAR_255)
              .nullable(NULL)
              .selectExpression("ou.name")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oucode")
              .dataType(VARCHAR_50)
              .nullable(NULL)
              .selectExpression("ou.code")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oulevel")
              .dataType(INTEGER)
              .nullable(NULL)
              .selectExpression("ous.level")
              .build(),
          AnalyticsTableColumn.builder()
              .name("eventdatavalues")
              .dataType(JSONB)
              .selectExpression(EVENT_DATA_VALUE_REBUILDER)
              .skipIndex(Skip.SKIP)
              .build());

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
    String latestFilter = format("and psi.lastupdated >= '{}' ", start);
    String partitionFilter =
        format(
            "and ({}) >= '{}' and ({}) < '{}' ",
            eventDateExpression,
            start,
            eventDateExpression,
            end);

    return partition.isLatestPartition() ? latestFilter : partitionFilter;
  }
}
