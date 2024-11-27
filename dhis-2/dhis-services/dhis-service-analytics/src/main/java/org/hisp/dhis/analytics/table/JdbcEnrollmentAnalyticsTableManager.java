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

import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsDimensionType;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
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
      DatabaseInfoProvider databaseInfoProvider,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      AnalyticsTableSettings analyticsExportSettings,
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
        databaseInfoProvider,
        jdbcTemplate,
        analyticsExportSettings,
        periodDataProvider,
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

    String fromClause =
        replaceQualify(
            """
            \s from ${enrollment} en \
            inner join ${program} pr on en.programid=pr.programid \
            left join ${trackedentity} te on en.trackedentityid=te.trackedentityid \
            and te.deleted = false \
            left join ${organisationunit} registrationou on te.organisationunitid=registrationou.organisationunitid \
            inner join ${organisationunit} ou on en.organisationunitid=ou.organisationunitid \
            left join analytics_rs_orgunitstructure ous on en.organisationunitid=ous.organisationunitid \
            left join analytics_rs_organisationunitgroupsetstructure ougs on en.organisationunitid=ougs.organisationunitid \
            and (cast(${enrollmentDateMonth} as date)=ougs.startdate or ougs.startdate is null) \
            left join analytics_rs_dateperiodstructure dps on cast(en.enrollmentdate as date)=dps.dateperiod \
            where pr.programid=${programId}  \
            and en.organisationunitid is not null \
            and en.lastupdated <= '${startTime}' \
            and en.occurreddate is not null \
            and en.deleted = false\s""",
            Map.of(
                "enrollmentDateMonth", sqlBuilder.dateTrunc("month", "en.enrollmentdate"),
                "programId", String.valueOf(program.getId()),
                "startTime", toLongDate(params.getStartTime())));

    populateTableInternal(partition, fromClause);
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
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    for (TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes()) {
      DataType dataType = getColumnType(attribute.getValueType(), isSpatialSupport());
      String dataClause =
          attribute.isNumericType()
              ? getNumericClause()
              : attribute.isDateType() ? getDateClause() : "";
      String select = getSelectExpressionForAttribute(attribute.getValueType(), "value");
      Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

      String sql =
          replaceQualify(
              """
              (select ${select} from ${trackedentityattributevalue} \
              where trackedentityid=en.trackedentityid \
              and trackedentityattributeid=${attributeId}\
              ${dataClause})${closingParentheses} as ${attributeUid}""",
              Map.of(
                  "select",
                  select,
                  "attributeId",
                  String.valueOf(attribute.getId()),
                  "dataClause",
                  dataClause,
                  "closingParentheses",
                  getClosingParentheses(select),
                  "attributeUid",
                  quote(attribute.getUid())));
      columns.add(
          AnalyticsTableColumn.builder()
              .name(attribute.getUid())
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(dataType)
              .selectExpression(sql)
              .skipIndex(skipIndex)
              .build());

      if (attribute.getValueType().isOrganisationUnit()) {
        String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select value";
        String ouNameSql = getSelectSubquery(attribute, fromTypeSql, dataClause);

        columns.add(
            AnalyticsTableColumn.builder()
                .name((attribute.getUid() + OU_NAME_COL_SUFFIX))
                .dimensionType(AnalyticsDimensionType.DYNAMIC)
                .dataType(TEXT)
                .selectExpression(ouNameSql)
                .skipIndex(SKIP)
                .build());
      }
    }
    return columns;
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
