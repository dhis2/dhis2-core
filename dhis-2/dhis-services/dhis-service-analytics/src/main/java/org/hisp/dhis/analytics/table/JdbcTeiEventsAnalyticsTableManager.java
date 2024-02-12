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
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.getDateLinkedToStatus;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getEndDate;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getStartDate;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
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
import static org.hisp.dhis.util.DateUtils.getLongDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableExportSettings;
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
      AnalyticsTableExportSettings settings,
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
        settings,
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
    Logged logged = analyticsExportSettings.getTableLogged();

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
    StringBuilder sql =
        new StringBuilder("select temp.supportedyear from")
            .append(
                " (select distinct extract(year from "
                    + getDateLinkedToStatus()
                    + ") as supportedyear ")
            .append(" from trackedentity tei ")
            .append(
                " inner join trackedentitytype tet on tet.trackedentitytypeid = tei.trackedentitytypeid ")
            .append(" inner join enrollment pi on pi.trackedentityid = tei.trackedentityid ")
            .append(" inner join event psi on psi.enrollmentid = pi.enrollmentid")
            .append(" where psi.lastupdated <= '" + getLongDateString(params.getStartTime()) + "' ")
            .append(" and tet.trackedentitytypeid = " + tet.getId() + " ")
            .append(AND + getDateLinkedToStatus() + ") is not null ")
            .append(AND + getDateLinkedToStatus() + ") > '1000-01-01' ")
            .append(" and psi.deleted is false ")
            .append(" and tei.deleted is false");

    if (params.getFromDate() != null) {
      sql.append(
          AND
              + getDateLinkedToStatus()
              + ") >= '"
              + getMediumDateString(params.getFromDate())
              + "'");
    }

    List<Integer> availableDataYears =
        periodDataProvider.getAvailableYears(
            analyticsExportSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);
    Integer firstDataYear = availableDataYears.get(0);
    Integer latestDataYear = availableDataYears.get(availableDataYears.size() - 1);

    sql.append(
        " ) as temp where temp.supportedyear >= "
            + firstDataYear
            + " and temp.supportedyear <= "
            + latestDataYear);

    return jdbcTemplate.queryForList(sql.toString(), Integer.class);
  }

  private List<AnalyticsTableColumn> getColumns() {
    List<AnalyticsTableColumn> analyticsTableColumnList = new ArrayList<>(FIXED_COLS);
    analyticsTableColumnList.add(getOrganisationUnitNameHierarchyColumn());

    return analyticsTableColumnList;
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
  protected void populateTable(
      AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String tableName = partition.getName();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    String start = getLongDateString(partition.getStartDate());
    String end = getLongDateString(partition.getEndDate());
    String partitionClause =
        partition.isLatestPartition()
            ? "psi.lastupdated >= '" + start + "' "
            : "("
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

    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");

    for (AnalyticsTableColumn col : columns) {
      sql.append(quote(col.getName()) + ",");
    }

    removeLastComma(sql).append(") select distinct ");

    for (AnalyticsTableColumn col : columns) {
      sql.append(col.getSelectExpression() + ",");
    }

    removeLastComma(sql)
        .append(" from event psi")
        .append(
            " inner join enrollment pi on pi.enrollmentid = psi.enrollmentid"
                + " and pi.deleted is false")
        .append(
            " inner join trackedentity tei on tei.trackedentityid = pi.trackedentityid"
                + " and tei.deleted is false"
                + " and tei.trackedentitytypeid = "
                + partition.getMasterTable().getTrackedEntityType().getId()
                + " and tei.lastupdated < '"
                + getLongDateString(params.getStartTime())
                + "'")
        .append(" left join programstage ps on ps.programstageid = psi.programstageid")
        .append(" left join program p on p.programid = ps.programid")
        .append(" left join organisationunit ou on psi.organisationunitid = ou.organisationunitid")
        .append(
            " left join _orgunitstructure ous on ous.organisationunitid = ou.organisationunitid")
        .append(" where psi.status in (" + join(",", EXPORTABLE_EVENT_STATUSES) + ")")
        .append(" and " + partitionClause)
        .append(" and psi.deleted is false ");

    invokeTimeAndLog(sql.toString(), tableName);
  }

  /**
   * Indicates whether data was created or updated for the given time range since last successful
   * "latest" table partition update.
   *
   * @param startDate the start date.
   * @param endDate the end date.
   * @return true if updated data exists.
   */
  @Override
  protected boolean hasUpdatedLatestData(Date startDate, Date endDate) {
    return false;
  }
}
