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
import org.hisp.dhis.analytics.table.model.Skip;
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
              .name("pi")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("pi.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("enrollmentdate")
              .dataType(TIMESTAMP)
              .selectExpression("pi.enrollmentdate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("incidentdate")
              .dataType(TIMESTAMP)
              .selectExpression("pi.occurreddate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("completeddate")
              .dataType(TIMESTAMP)
              .selectExpression("case pi.status when 'COMPLETED' then pi.completeddate end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdated")
              .dataType(TIMESTAMP)
              .selectExpression("pi.lastupdated")
              .build(),
          AnalyticsTableColumn.builder()
              .name("storedby")
              .dataType(VARCHAR_255)
              .selectExpression("pi.storedby")
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbyusername")
              .dataType(VARCHAR_255)
              .selectExpression("pi.createdbyuserinfo ->> 'username' as createdbyusername")
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbyname")
              .dataType(VARCHAR_255)
              .selectExpression("pi.createdbyuserinfo ->> 'firstName' as createdbyname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbylastname")
              .dataType(VARCHAR_255)
              .selectExpression("pi.createdbyuserinfo ->> 'surname' as createdbylastname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbydisplayname")
              .dataType(VARCHAR_255)
              .selectExpression(getDisplayName("createdbyuserinfo", "pi", "createdbydisplayname"))
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbyusername")
              .dataType(VARCHAR_255)
              .selectExpression("pi.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbyname")
              .dataType(VARCHAR_255)
              .selectExpression("pi.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbylastname")
              .dataType(VARCHAR_255)
              .selectExpression("pi.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbydisplayname")
              .dataType(VARCHAR_255)
              .selectExpression(
                  getDisplayName("lastupdatedbyuserinfo", "pi", "lastupdatedbydisplayname"))
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("enrollmentstatus")
              .dataType(VARCHAR_50)
              .selectExpression("pi.status")
              .build(),
          AnalyticsTableColumn.builder()
              .name("longitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "CASE WHEN 'POINT' = GeometryType(pi.geometry) THEN ST_X(pi.geometry) ELSE null END")
              .build(),
          AnalyticsTableColumn.builder()
              .name("latitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "CASE WHEN 'POINT' = GeometryType(pi.geometry) THEN ST_Y(pi.geometry) ELSE null END")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ou")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("ou.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ouname")
              .dataType(TEXT)
              .nullable(NOT_NULL)
              .selectExpression("ou.name")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oucode")
              .dataType(TEXT)
              .selectExpression("ou.code")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oulevel")
              .dataType(INTEGER)
              .selectExpression("ous.level")
              .build(),
          AnalyticsTableColumn.builder()
              .name("pigeometry")
              .dataType(GEOMETRY)
              .selectExpression("pi.geometry")
              .indexType(IndexType.GIST)
              .build(),
          AnalyticsTableColumn.builder()
              .name("registrationou")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("coalesce(registrationou.uid,ou.uid)")
              .build());

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
}
