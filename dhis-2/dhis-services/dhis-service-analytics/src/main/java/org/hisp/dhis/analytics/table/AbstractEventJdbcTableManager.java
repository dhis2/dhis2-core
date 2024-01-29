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

import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.ColumnDataType;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableExportSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Markus Bekken
 */
public abstract class AbstractEventJdbcTableManager extends AbstractJdbcTableManager {
  public AbstractEventJdbcTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingManager systemSettingManager,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      DatabaseInfoProvider databaseInfoProvider,
      JdbcTemplate jdbcTemplate,
      AnalyticsTableExportSettings analyticsExportSettings,
      PeriodDataProvider periodDataProvider) {
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
        periodDataProvider);
  }

  protected final String getNumericClause() {
    return " and value ~* '" + NUMERIC_LENIENT_REGEXP + "'";
  }

  protected final String getDateClause() {
    return " and value ~* '" + DATE_REGEXP + "'";
  }

  protected Skip skipIndex(ValueType valueType, boolean hasOptionSet) {
    boolean skipIndex = NO_INDEX_VAL_TYPES.contains(valueType) && !hasOptionSet;
    return skipIndex ? Skip.SKIP : Skip.INCLUDE;
  }

  @Override
  public String validState() {
    // Data values might be '{}' / empty object if data values existed
    // and were removed later

    String sql = "select eventid " + "from event " + "where eventdatavalues != '{}' limit 1;";

    boolean hasData = jdbcTemplate.queryForRowSet(sql).next();

    if (!hasData) {
      return "No events exist, not updating event analytics tables";
    }

    return null;
  }

  @Override
  protected boolean hasUpdatedLatestData(Date startDate, Date endDate) {
    throw new IllegalStateException("This method should never be invoked");
  }

  /**
   * Populates the given analytics table partition using the given columns and join statement.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   * @param fromClause the SQL from clause.
   */
  protected void populateTableInternal(AnalyticsTablePartition partition, String fromClause) {
    String tableName = partition.getTempTableName();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getColumns();

    String sql = "insert into " + partition.getTempTableName() + " (";

    for (AnalyticsTableColumn col : columns) {
      sql += col.getName() + ",";
    }

    sql = TextUtils.removeLastComma(sql) + ") select ";

    for (AnalyticsTableColumn col : columns) {
      sql += col.getSelectExpression() + ",";
    }

    sql = TextUtils.removeLastComma(sql) + " ";

    sql += fromClause;

    invokeTimeAndLog(sql, String.format("Populate %s", tableName));
  }

  protected List<AnalyticsTableColumn> addTrackedEntityAttributes(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    for (TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes()) {
      ColumnDataType dataType = getColumnType(attribute.getValueType(), isSpatialSupport());
      String dataClause =
          attribute.isNumericType()
              ? getNumericClause()
              : attribute.isDateType() ? getDateClause() : "";
      String select = getSelectClause(attribute.getValueType(), "value");
      Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

      String sql =
          "(select "
              + select
              + " "
              + "from trackedentityattributevalue where trackedentityid=pi.trackedentityid "
              + "and trackedentityattributeid="
              + attribute.getId()
              + dataClause
              + ")"
              + getClosingParentheses(select)
              + " as "
              + quote(attribute.getUid());

      columns.add(new AnalyticsTableColumn(quote(attribute.getUid()), dataType, sql, skipIndex));
    }

    return columns;
  }
}
