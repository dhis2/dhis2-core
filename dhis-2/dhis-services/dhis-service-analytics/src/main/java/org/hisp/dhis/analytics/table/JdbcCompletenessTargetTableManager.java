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

import static org.hisp.dhis.analytics.table.model.AnalyticsValueType.FACT;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DATE;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableExportSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.CompletenessTargetTableManager")
public class JdbcCompletenessTargetTableManager extends AbstractJdbcTableManager {
  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          new AnalyticsTableColumn("ouopeningdate", DATE, "ou.openingdate"),
          new AnalyticsTableColumn("oucloseddate", DATE, "ou.closeddate"),
          new AnalyticsTableColumn("costartdate", DATE, "doc.costartdate"),
          new AnalyticsTableColumn("coenddate", DATE, "doc.coenddate"),
          new AnalyticsTableColumn("dx", CHARACTER_11, NOT_NULL, "ds.uid"),
          new AnalyticsTableColumn("ao", CHARACTER_11, NOT_NULL, "ao.uid"));

  public JdbcCompletenessTargetTableManager(
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
      AnalyticsTableExportSettings analyticsExportSettings,
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
    return AnalyticsTableType.COMPLETENESS_TARGET;
  }

  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    Logged logged = analyticsExportSettings.getTableLogged();
    return params.isLatestUpdate()
        ? List.of()
        : List.of(new AnalyticsTable(getAnalyticsTableType(), getColumns(), logged));
  }

  @Override
  public Set<String> getExistingDatabaseTables() {
    return Set.of(getTableName());
  }

  @Override
  protected boolean hasUpdatedLatestData(Date startDate, Date endDate) {
    return false;
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    return List.of();
  }

  @Override
  protected void populateTable(
      AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String tableName = partition.getName();

    String sql = "insert into " + tableName + " (";

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    for (AnalyticsTableColumn col : columns) {
      sql += quote(col.getName()) + ",";
    }

    sql = TextUtils.removeLastComma(sql) + ") select ";

    for (AnalyticsTableColumn col : columns) {
      sql += col.getSelectExpression() + ",";
    }

    sql = TextUtils.removeLastComma(sql) + " ";

    sql +=
        "from _datasetorganisationunitcategory doc "
            + "inner join dataset ds on doc.datasetid=ds.datasetid "
            + "inner join organisationunit ou on doc.organisationunitid=ou.organisationunitid "
            + "left join _orgunitstructure ous on doc.organisationunitid=ous.organisationunitid "
            + "left join _organisationunitgroupsetstructure ougs on doc.organisationunitid=ougs.organisationunitid "
            + "left join categoryoptioncombo ao on doc.attributeoptioncomboid=ao.categoryoptioncomboid "
            + "left join _categorystructure acs on doc.attributeoptioncomboid=acs.categoryoptioncomboid ";

    invokeTimeAndLog(sql, String.format("Populate %s", tableName));
  }

  private List<AnalyticsTableColumn> getColumns() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    columns.addAll(getOrganisationUnitGroupSetColumns());
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.addAll(getAttributeCategoryOptionGroupSetColumns());
    columns.addAll(getAttributeCategoryColumns());
    columns.addAll(FIXED_COLS);
    columns.add(new AnalyticsTableColumn("value", DOUBLE, NULL, FACT, "1 as value"));

    return filterDimensionColumns(columns);
  }
}
