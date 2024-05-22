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
              .build()
              .withName("trackedentityinstanceuid")
              .withDataType(CHARACTER_11)
              .withNullable(NOT_NULL)
              .withSelectExpression("tei.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("programuid")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("p.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("programinstanceuid")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("pi.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("programstageuid")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ps.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("programstageinstanceuid")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("psi.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("occurreddate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("psi.occurreddate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("lastupdated")
              .withDataType(TIMESTAMP)
              .withSelectExpression("psi.lastupdated"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("created")
              .withDataType(TIMESTAMP)
              .withSelectExpression("psi.created"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("scheduleddate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("psi.scheduleddate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("status")
              .withDataType(VARCHAR_50)
              .withSelectExpression("psi.status"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("psigeometry")
              .withDataType(GEOMETRY)
              .withSelectExpression("psi.geometry")
              .withIndexType(IndexType.GIST),
          AnalyticsTableColumn.builder()
              .build()
              .withName("psilongitude")
              .withDataType(DOUBLE)
              .withSelectExpression(
                  "case when 'POINT' = GeometryType(psi.geometry) then ST_X(psi.geometry) end"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("psilatitude")
              .withDataType(DOUBLE)
              .withSelectExpression(
                  "case when 'POINT' = GeometryType(psi.geometry) then ST_Y(psi.geometry) end"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel1")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel1"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel2")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel2"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel3")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel3"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel4")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel4"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("ou")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ou.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("ouname")
              .withDataType(VARCHAR_255)
              .withNullable(NULL)
              .withSelectExpression("ou.name"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("oucode")
              .withDataType(CHARACTER_32)
              .withNullable(NULL)
              .withSelectExpression("ou.code"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("oulevel")
              .withDataType(INTEGER)
              .withNullable(NULL)
              .withSelectExpression("ous.level"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("eventdatavalues")
              .withDataType(JSONB)
              .withSelectExpression(EVENT_DATA_VALUE_REBUILDER));

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
