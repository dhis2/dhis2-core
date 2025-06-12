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
import static java.lang.String.valueOf;
import static org.hisp.dhis.analytics.AnalyticsStringUtils.replaceQualify;
import static org.hisp.dhis.analytics.AnalyticsStringUtils.toCommaSeparated;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TrackedEntityEnrollmentsAnalyticsTableManager")
public class JdbcTrackedEntityEnrollmentsAnalyticsTableManager extends AbstractJdbcTableManager {
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
              .name("enrollmentdate")
              .dataType(TIMESTAMP)
              .selectExpression("en.enrollmentdate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("completeddate")
              .dataType(TIMESTAMP)
              .selectExpression("en.completeddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("occurreddate")
              .dataType(TIMESTAMP)
              .selectExpression("en.occurreddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("enrollmentstatus")
              .dataType(VARCHAR_50)
              .selectExpression("en.status")
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

  public JdbcTrackedEntityEnrollmentsAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      TrackedEntityTypeService trackedEntityTypeService,
      AnalyticsTableSettings analyticsTableSettings,
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
        jdbcTemplate,
        analyticsTableSettings,
        periodDataProvider,
        sqlBuilder);
    this.trackedEntityTypeService = trackedEntityTypeService;
  }

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
    Logged logged = analyticsTableSettings.getTableLogged();
    return trackedEntityTypeService.getAllTrackedEntityType().stream()
        .map(tet -> new AnalyticsTable(getAnalyticsTableType(), getColumns(), logged, tet))
        .collect(Collectors.toList());
  }

  private List<AnalyticsTableColumn> getColumns() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(getFixedCols());
    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(getOrganisationUnitLevelColumns());
    if (sqlBuilder.supportsDeclarativePartitioning()) {
      columns.add(getPartitionColumn());
    }
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
    long tetId = partition.getMasterTable().getTrackedEntityType().getId();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");
    sql.append(toCommaSeparated(columns, col -> quote(col.getName())));
    sql.append(") select ");
    sql.append(toCommaSeparated(columns, AnalyticsTableColumn::getSelectExpression));

    sql.append(
        replaceQualify(
            sqlBuilder,
            """
            \sfrom ${enrollment} en \
            inner join ${trackedentity} te on en.trackedentityid=te.trackedentityid \
            and ${teDeletedClause} and te.trackedentitytypeid = ${trackedEntityTypeId} \
            and te.lastupdated < '${startTime}' \
            left join ${program} p on en.programid=p.programid \
            left join analytics_rs_orgunitstructure ous on en.organisationunitid=ous.organisationunitid \
            where en.occurreddate is not null \
            and ${enDeletedClause}""",
            Map.of(
                "trackedEntityTypeId", valueOf(tetId),
                "teDeletedClause", sqlBuilder.isFalse("te", "deleted"),
                "startTime", toLongDate(params.getStartTime()),
                "statuses", join(",", EXPORTABLE_EVENT_STATUSES),
                "enDeletedClause", sqlBuilder.isFalse("en", "deleted"))));

    invokeTimeAndLog(sql.toString(), "Populating table: '{}'", tableName);
  }

  /**
   * Returns a list of fixed columns.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getFixedCols() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_COLS);
    if (sqlBuilder.supportsGeospatialData()) {
      columns.addAll(getGeospatialCols());
    }
    return columns;
  }

  /**
   * Returns a list of geospatial columns.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getGeospatialCols() {

    return List.of(
        AnalyticsTableColumn.builder()
            .name("enrollmentgeometry")
            .dataType(GEOMETRY)
            .selectExpression("en.geometry")
            .indexType(IndexType.GIST)
            .build(),
        AnalyticsTableColumn.builder()
            .name("enrollmentlongitude")
            .dataType(DOUBLE)
            .selectExpression(
                "case when 'POINT' = GeometryType(en.geometry) then ST_X(en.geometry) end")
            .build(),
        AnalyticsTableColumn.builder()
            .name("enrollmentlatitude")
            .dataType(DOUBLE)
            .selectExpression(
                "case when 'POINT' = GeometryType(en.geometry) then ST_Y(en.geometry) end")
            .build());
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
        .nullable(NOT_NULL)
        .selectExpression("extract(year from en.occurreddate)")
        .skipIndex(Skip.SKIP)
        .build();
  }
}
