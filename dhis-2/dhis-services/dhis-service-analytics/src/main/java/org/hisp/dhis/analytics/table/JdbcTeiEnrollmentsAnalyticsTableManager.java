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
import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_32;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_50;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NULL;
import static org.hisp.dhis.analytics.IndexType.GIST;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.util.DateUtils.getLongDateString;
import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TeiEnrollmentsAnalyticsTableManager")
public class JdbcTeiEnrollmentsAnalyticsTableManager extends AbstractJdbcTableManager {
  private final TrackedEntityTypeService trackedEntityTypeService;

  public JdbcTeiEnrollmentsAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingManager systemSettingManager,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      StatementBuilder statementBuilder,
      PartitionManager partitionManager,
      DatabaseInfo databaseInfo,
      JdbcTemplate jdbcTemplate,
      TrackedEntityTypeService trackedEntityTypeService,
      AnalyticsExportSettings settings,
      PeriodDataProvider periodDataProvider) {
    super(
        idObjectManager,
        organisationUnitService,
        categoryService,
        systemSettingManager,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        statementBuilder,
        partitionManager,
        databaseInfo,
        jdbcTemplate,
        settings,
        periodDataProvider);

    notNull(trackedEntityTypeService, "trackedEntityTypeService cannot be null");
    this.trackedEntityTypeService = trackedEntityTypeService;
  }

  private static final List<AnalyticsTableColumn> FIXED_COLUMNS =
      List.of(
          new AnalyticsTableColumn(
              quote("trackedentityinstanceuid"), CHARACTER_11, NOT_NULL, "tei.uid"),
          new AnalyticsTableColumn(quote("programuid"), CHARACTER_11, NULL, "p.uid"),
          new AnalyticsTableColumn(quote("programinstanceuid"), CHARACTER_11, NULL, "pi.uid"),
          new AnalyticsTableColumn(quote("enrollmentdate"), TIMESTAMP, "pi.enrollmentdate"),
          new AnalyticsTableColumn(quote("enddate"), TIMESTAMP, "pi.enddate"),
          new AnalyticsTableColumn(quote("incidentdate"), TIMESTAMP, "pi.incidentdate"),
          new AnalyticsTableColumn(quote("enrollmentstatus"), VARCHAR_50, "pi.status"),
          new AnalyticsTableColumn(quote("pigeometry"), GEOMETRY, "pi.geometry")
              .withIndexType(GIST),
          new AnalyticsTableColumn(
              quote("pilongitude"),
              DOUBLE,
              "case when 'POINT' = GeometryType(pi.geometry) then ST_X(pi.geometry) end"),
          new AnalyticsTableColumn(
              quote("pilatitude"),
              DOUBLE,
              "case when 'POINT' = GeometryType(pi.geometry) then ST_Y(pi.geometry) end"),
          new AnalyticsTableColumn(quote("uidlevel1"), CHARACTER_11, NULL, "ous.uidlevel1"),
          new AnalyticsTableColumn(quote("uidlevel2"), CHARACTER_11, NULL, "ous.uidlevel2"),
          new AnalyticsTableColumn(quote("uidlevel3"), CHARACTER_11, NULL, "ous.uidlevel3"),
          new AnalyticsTableColumn(quote("uidlevel4"), CHARACTER_11, NULL, "ous.uidlevel4"),
          new AnalyticsTableColumn(quote("ou"), CHARACTER_11, NULL, "ou.uid"),
          new AnalyticsTableColumn(quote("ouname"), VARCHAR_255, NULL, "ou.name"),
          new AnalyticsTableColumn(quote("oucode"), CHARACTER_32, NULL, "ou.code"),
          new AnalyticsTableColumn(quote("oulevel"), INTEGER, NULL, "ous.level"));

  /**
   * Returns the {@link AnalyticsTableType} of analytics table which this manager handles.
   *
   * @return type of analytics table.
   */
  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
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
    return trackedEntityTypeService.getAllTrackedEntityType().stream()
        .map(
            tet -> new AnalyticsTable(getAnalyticsTableType(), getTableColumns(), emptyList(), tet))
        .collect(Collectors.toList());
  }

  private List<AnalyticsTableColumn> getTableColumns() {
    List<AnalyticsTableColumn> analyticsTableColumnList = new ArrayList<>(getFixedColumns());
    analyticsTableColumnList.add(getOrganisationUnitNameHierarchyColumn());

    return analyticsTableColumnList;
  }

  /**
   * Checks if the database content is in valid state for analytics table generation.
   *
   * @return null if valid, a descriptive string if invalid.
   */
  @Override
  public String validState() {
    return null;
  }

  /**
   * Returns a list of non-dynamic {@link AnalyticsTableColumn}.
   *
   * @return a List of {@link AnalyticsTableColumn}.
   */
  @Override
  public List<AnalyticsTableColumn> getFixedColumns() {
    return FIXED_COLUMNS;
  }

  /**
   * Returns a list of table checks (constraints) for the given analytics table partition.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   */
  @Override
  protected List<String> getPartitionChecks(AnalyticsTablePartition partition) {
    return emptyList();
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
    List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();
    List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

    validateDimensionColumns(columns);

    StringBuilder sql = new StringBuilder("insert into " + partition.getTempTableName() + " (");

    for (AnalyticsTableColumn col : ListUtils.union(columns, values)) {
      if (col.isVirtual()) {
        continue;
      }

      sql.append(col.getName() + ",");
    }

    removeLastComma(sql).append(") select ");

    for (AnalyticsTableColumn col : columns) {
      if (col.isVirtual()) {
        continue;
      }

      sql.append(col.getAlias() + ",");
    }

    removeLastComma(sql)
        .append(" from enrollment pi")
        .append(" inner join trackedentity tei " + "on pi.trackedentityid = tei.trackedentityid")
        .append(" and tei.deleted is false ")
        .append(
            " and tei.trackedentitytypeid = "
                + partition.getMasterTable().getTrackedEntityType().getId())
        .append(" and tei.lastupdated < '" + getLongDateString(params.getStartTime()) + "'")
        .append(" left join program p on p.programid = pi.programid")
        .append(" left join organisationunit ou on pi.organisationunitid = ou.organisationunitid")
        .append(
            " left join _orgunitstructure ous on ous.organisationunitid = ou.organisationunitid")
        .append(
            " where exists ( select 1 from event psi where psi.deleted is false"
                + " and psi.enrollmentid = pi.enrollmentid"
                + " and psi.status in ("
                + join(",", EXPORTABLE_EVENT_STATUSES)
                + "))")
        .append(" and pi.incidentdate is not null ")
        .append(" and pi.deleted is false");

    invokeTimeAndLog(sql.toString(), partition.getTempTableName());
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
