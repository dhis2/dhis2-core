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

import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
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
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
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
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Markus Bekken
 */
@Service("org.hisp.dhis.analytics.EnrollmentAnalyticsTableManager")
public class JdbcEnrollmentAnalyticsTableManager extends AbstractEventJdbcTableManager {
  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .build()
              .withName("pi")
              .withDataType(CHARACTER_11)
              .withNullable(NOT_NULL)
              .withSelectExpression("pi.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("enrollmentdate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("pi.enrollmentdate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("incidentdate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("pi.occurreddate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("completeddate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("case pi.status when 'COMPLETED' then pi.completeddate end"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("lastupdated")
              .withDataType(TIMESTAMP)
              .withSelectExpression("pi.lastupdated"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("storedby")
              .withDataType(VARCHAR_255)
              .withSelectExpression("pi.storedby"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("createdbyusername")
              .withDataType(VARCHAR_255)
              .withSelectExpression("pi.createdbyuserinfo ->> 'username' as createdbyusername"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("createdbyname")
              .withDataType(VARCHAR_255)
              .withSelectExpression("pi.createdbyuserinfo ->> 'firstName' as createdbyname"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("createdbylastname")
              .withDataType(VARCHAR_255)
              .withSelectExpression("pi.createdbyuserinfo ->> 'surname' as createdbylastname"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("createdbydisplayname")
              .withDataType(VARCHAR_255)
              .withSelectExpression(
                  getDisplayName("createdbyuserinfo", "pi", "createdbydisplayname")),
          AnalyticsTableColumn.builder()
              .build()
              .withName("lastupdatedbyusername")
              .withDataType(VARCHAR_255)
              .withSelectExpression(
                  "pi.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("lastupdatedbyname")
              .withDataType(VARCHAR_255)
              .withSelectExpression(
                  "pi.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("lastupdatedbylastname")
              .withDataType(VARCHAR_255)
              .withSelectExpression(
                  "pi.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("lastupdatedbydisplayname")
              .withDataType(VARCHAR_255)
              .withSelectExpression(
                  getDisplayName("lastupdatedbyuserinfo", "pi", "lastupdatedbydisplayname")),
          AnalyticsTableColumn.builder()
              .build()
              .withName("enrollmentstatus")
              .withDataType(VARCHAR_50)
              .withSelectExpression("pi.status"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("longitude")
              .withDataType(DOUBLE)
              .withSelectExpression(
                  "CASE WHEN 'POINT' = GeometryType(pi.geometry) THEN ST_X(pi.geometry) ELSE null END"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("latitude")
              .withDataType(DOUBLE)
              .withSelectExpression(
                  "CASE WHEN 'POINT' = GeometryType(pi.geometry) THEN ST_Y(pi.geometry) ELSE null END"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("ou")
              .withDataType(CHARACTER_11)
              .withNullable(NOT_NULL)
              .withSelectExpression("ou.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("ouname")
              .withDataType(TEXT)
              .withNullable(NOT_NULL)
              .withSelectExpression("ou.name"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("oucode")
              .withDataType(TEXT)
              .withSelectExpression("ou.code"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("oulevel")
              .withDataType(INTEGER)
              .withSelectExpression("ous.level"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("pigeometry")
              .withDataType(GEOMETRY)
              .withSelectExpression("pi.geometry")
              .withIndexType(IndexType.GIST),
          AnalyticsTableColumn.builder()
              .build()
              .withName("registrationou")
              .withDataType(CHARACTER_11)
              .withNullable(NOT_NULL)
              .withSelectExpression("coalesce(registrationou.uid,ou.uid)"));

  public JdbcEnrollmentAnalyticsTableManager(
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
    return AnalyticsTableType.ENROLLMENT;
  }

  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    return params.isLatestUpdate() ? List.of() : getRegularAnalyticsTables(params);
  }

  /**
   * Creates a list of {@link AnalyticsTable} for each program.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return a list of {@link AnalyticsTableUpdateParams}.
   */
  private List<AnalyticsTable> getRegularAnalyticsTables(AnalyticsTableUpdateParams params) {
    List<AnalyticsTable> tables = new UniqueArrayList<>();

    Logged logged = analyticsTableSettings.getTableLogged();
    List<Program> programs = idObjectManager.getAllNoAcl(Program.class);

    for (Program program : programs) {
      AnalyticsTable table =
          new AnalyticsTable(getAnalyticsTableType(), getColumns(program), logged, program);

      tables.add(table);
    }

    return tables;
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    return List.of();
  }

  @Override
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    Program program = partition.getMasterTable().getProgram();

    String fromClause =
        replace(
            """
            \s from enrollment pi \
            inner join program pr on pi.programid=pr.programid \
            left join trackedentity tei on pi.trackedentityid=tei.trackedentityid \
            and tei.deleted = false \
            left join organisationunit registrationou on tei.organisationunitid=registrationou.organisationunitid \
            inner join organisationunit ou on pi.organisationunitid=ou.organisationunitid \
            left join analytics_rs_orgunitstructure ous on pi.organisationunitid=ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on pi.organisationunitid=ougs.organisationunitid \
            and (cast(${piEnrollmentDateMonth} as date)=ougs.startdate or ougs.startdate is null) \
            left join analytics_rs_dateperiodstructure dps on cast(pi.enrollmentdate as date)=dps.dateperiod \
            where pr.programid=${programId}  \
            and pi.organisationunitid is not null \
            and pi.lastupdated <= '${startTime}' \
            and pi.occurreddate is not null \
            and pi.deleted = false\s""",
            Map.of(
                "piEnrollmentDateMonth", sqlBuilder.dateTrunc("month", "pi.enrollmentdate"),
                "programId", String.valueOf(program.getId()),
                "startTime", toLongDate(params.getStartTime())));

    populateTableInternal(partition, fromClause);
  }

  private List<AnalyticsTableColumn> getColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_COLS);
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(getOrganisationUnitGroupSetColumns());
    columns.addAll(getPeriodTypeColumns("dps"));
    columns.addAll(getTrackedEntityAttributeColumns(program));

    if (program.isRegistration()) {
      columns.add(
          AnalyticsTableColumn.builder()
              .build()
              .withName("tei")
              .withDataType(CHARACTER_11)
              .withSelectExpression("tei.uid"));
      columns.add(
          AnalyticsTableColumn.builder()
              .build()
              .withName("teigeometry")
              .withDataType(GEOMETRY)
              .withSelectExpression("tei.geometry"));
    }

    return filterDimensionColumns(columns);
  }
}
