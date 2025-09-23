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

import static org.hisp.dhis.analytics.AnalyticsStringUtils.replaceQualify;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.Collection;
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
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Markus Bekken
 */
@Service("org.hisp.dhis.analytics.EnrollmentAnalyticsTableManager")
public class JdbcEnrollmentAnalyticsTableManager extends AbstractEventJdbcTableManager {

  private final List<AnalyticsTableColumn> fixedColumns;

  public JdbcEnrollmentAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      ColumnMapper columnMapper,
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
        columnMapper,
        sqlBuilder);
    fixedColumns = EnrollmentAnalyticsColumn.getColumns(sqlBuilder);
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
    String attributeJoinClause = getAttributeValueJoinClause(program);

    String fromClause =
        replaceQualify(
            sqlBuilder,
            """
            \sfrom ${enrollment} en \
            inner join ${program} pr on en.programid=pr.programid \
            left join ${trackedentity} te on en.trackedentityid=te.trackedentityid and ${teDeletedClause} \
            left join ${organisationunit} registrationou on te.organisationunitid=registrationou.organisationunitid \
            inner join ${organisationunit} ou on en.organisationunitid=ou.organisationunitid \
            left join analytics_rs_dateperiodstructure dps on cast(en.enrollmentdate as date)=dps.dateperiod \
            left join analytics_rs_orgunitstructure ous on en.organisationunitid=ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on en.organisationunitid=ougs.organisationunitid \
            ${attributeJoinClause}\
            where pr.programid = ${programId} \
            and en.organisationunitid is not null \
            and (ougs.startdate is null or dps.monthstartdate=ougs.startdate) \
            and en.lastupdated <= '${startTime}' \
            and en.occurreddate is not null \
            and ${enDeletedClause} """,
            Map.of(
                "attributeJoinClause", attributeJoinClause,
                "programId", String.valueOf(program.getId()),
                "startTime", toLongDate(params.getStartTime()),
                "teDeletedClause", sqlBuilder.isFalse("te", "deleted"),
                "enDeletedClause", sqlBuilder.isFalse("en", "deleted")));

    String tableName = partition.getName();
    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    populateTableInternal(tableName, columns, fromClause);
  }

  /**
   * Returns a list of columns for the given program.
   *
   * @param program the {@link Program}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(fixedColumns);
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(getOrganisationUnitGroupSetColumns());
    columns.addAll(getPeriodTypeColumns("dps"));
    columns.addAll(getTrackedEntityAttributeColumns(program));
    columns.addAll(getTrackedEntityColumns(program));

    return filterDimensionColumns(columns);
  }

  /**
   * Returns a list of tracked entity attribute {@link AnalyticsTableColumn}.
   *
   * @param program the {@link Program}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getTrackedEntityAttributeColumns(Program program) {
    return program.getNonConfidentialTrackedEntityAttributes().stream()
        .map(this::getColumnForAttribute)
        .flatMap(Collection::stream)
        .toList();
  }

  /**
   * Returns a list of tracked entity {@link AnalyticsTableColumn}.
   *
   * @param program the {@link Program}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getTrackedEntityColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (program.isRegistration()) {
      columns.add(EnrollmentAnalyticsColumn.TRACKED_ENTITY);
      if (sqlBuilder.supportsGeospatialData()) {
        columns.add(EnrollmentAnalyticsColumn.TRACKED_ENTITY_GEOMETRY);
      }
    }

    return columns;
  }
}
