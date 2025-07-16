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
import static org.hisp.dhis.analytics.AnalyticsStringUtils.toCommaSeparated;
import static org.hisp.dhis.analytics.table.model.AnalyticsValueType.FACT;
import static org.hisp.dhis.analytics.table.util.PartitionUtils.getLatestTablePartition;
import static org.hisp.dhis.db.model.DataType.BOOLEAN;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.util.DateUtils.SECONDS_PER_DAY;
import static org.hisp.dhis.util.DateUtils.toLongDate;
import static org.hisp.dhis.util.TextUtils.emptyIfTrue;
import static org.hisp.dhis.util.TextUtils.format;
import static org.hisp.dhis.util.TextUtils.replace;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.CompletenessTableManager")
public class JdbcCompletenessTableManager extends AbstractJdbcTableManager {
  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("dx")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("ds.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("year")
              .dataType(INTEGER)
              .nullable(NOT_NULL)
              .selectExpression("ps.year")
              .build());

  private static final List<String> SORT_KEY = List.of("dx");

  public JdbcCompletenessTableManager(
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
  }

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return AnalyticsTableType.COMPLETENESS;
  }

  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    AnalyticsTable table =
        params.isLatestUpdate()
            ? getLatestAnalyticsTable(params, getColumns())
            : getRegularAnalyticsTable(params, getDataYears(params), getColumns(), SORT_KEY);

    return table.hasTablePartitions() ? List.of(table) : List.of();
  }

  @Override
  public Set<String> getExistingDatabaseTables() {
    return Set.of(getTableName());
  }

  @Override
  public boolean validState() {
    return tableIsNotEmpty("completedatasetregistration");
  }

  @Override
  public boolean hasUpdatedLatestData(Date startDate, Date endDate) {
    String sql =
        replaceQualify(
            sqlBuilder,
            """
            select cdr.datasetid \
            from ${completedatasetregistration} cdr \
            where cdr.lastupdated >= '${startDate}' \
            and cdr.lastupdated < '${endDate}' \
            limit 1;""",
            Map.of("startDate", toLongDate(startDate), "endDate", toLongDate(endDate)));

    return !jdbcTemplate.queryForList(sql).isEmpty();
  }

  @Override
  public void removeUpdatedData(List<AnalyticsTable> tables) {
    AnalyticsTablePartition partition = getLatestTablePartition(tables);
    String sql =
        replaceQualify(
            sqlBuilder,
            """
            delete from ${tableName} ax \
            where ax.id in ( \
            select concat(ds.uid,'-',ps.iso,'-',ous.organisationunituid,'-',acs.categoryoptioncombouid) as id \
            from ${completedatasetregistration} cdr \
            inner join ${dataset} ds on cdr.datasetid=ds.datasetid \
            inner join analytics_rs_periodstructure ps on cdr.periodid=ps.periodid \
            inner join analytics_rs_orgunitstructure ous on cdr.sourceid=ous.organisationunitid \
            inner join analytics_rs_categorystructure acs on cdr.attributeoptioncomboid=acs.categoryoptioncomboid \
            where cdr.lastupdated >= '${startDate}' \
            and cdr.lastupdated < '${endDate}');""",
            Map.of(
                "tableName", quote(getAnalyticsTableType().getTableName()),
                "startDate", toLongDate(partition.getStartDate()),
                "endDate", toLongDate(partition.getEndDate())));

    invokeTimeAndLog(sql, "Remove updated data values");
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    Objects.requireNonNull(year);
    return List.of("year = " + year + "");
  }

  @Override
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String tableName = partition.getName();
    String partitionClause = getPartitionClause(partition);

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    String sql = "insert into " + tableName + " (";
    sql += toCommaSeparated(columns, col -> quote(col.getName()));
    sql += ") select ";
    sql += toCommaSeparated(columns, AnalyticsTableColumn::getSelectExpression);
    sql += " ";
    // Database legacy fix
    sql = sql.replace("organisationunitid", "sourceid");

    sql +=
        replaceQualify(
            sqlBuilder,
            """
            from ${completedatasetregistration} cdr \
            inner join ${dataset} ds on cdr.datasetid=ds.datasetid \
            inner join analytics_rs_periodstructure ps on cdr.periodid=ps.periodid \
            inner join analytics_rs_organisationunitgroupsetstructure ougs on cdr.sourceid=ougs.organisationunitid \
            left join analytics_rs_orgunitstructure ous on cdr.sourceid=ous.organisationunitid \
            inner join analytics_rs_categorystructure acs on cdr.attributeoptioncomboid=acs.categoryoptioncomboid \
            where cdr.date is not null \
            ${partitionClause} \
            and (ougs.startdate is null or ps.monthstartdate=ougs.startdate) \
            and cdr.lastupdated < '${startTime}' \
            and ${completedClause}""",
            Map.of(
                "partitionClause", partitionClause,
                "startTime", toLongDate(params.getStartTime()),
                "completedClause", sqlBuilder.isTrue("cdr", "completed")));

    invokeTimeAndLog(sql, "Populating table: '{}'", tableName);
  }

  /**
   * Returns a partition SQL clause.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   * @return a partition SQL clause.
   */
  private String getPartitionClause(AnalyticsTablePartition partition) {
    String latestFilter =
        format("and cdr.lastupdated >= '{}' ", toLongDate(partition.getStartDate()));
    String partitionFilter = format("and ps.year = {} ", partition.getYear());

    return partition.isLatestPartition()
        ? latestFilter
        : emptyIfTrue(partitionFilter, sqlBuilder.supportsDeclarativePartitioning());
  }

  private List<AnalyticsTableColumn> getColumns() {
    String idColAlias =
        "concat(ds.uid,'-',ps.iso,'-',ous.organisationunituid,'-',acs.categoryoptioncombouid) as id ";
    String diffInSeconds = sqlBuilder.differenceInSeconds("cdr.date", "ps.enddate");
    String timelyDateDiff = diffInSeconds + " / (" + SECONDS_PER_DAY + ")";
    // Since the end date is reported with a time of 00:00:00, we add 1 day to the timely days
    // so that the calculations starts at 00:00:00 of the day after the end date.
    String timelyAlias = "((" + timelyDateDiff + ") <= (ds.timelydays + 1)) as timely";

    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_COLS);
    columns.add(
        AnalyticsTableColumn.builder()
            .name("id")
            .dataType(TEXT)
            .selectExpression(idColAlias)
            .build());
    columns.addAll(getOrganisationUnitGroupSetColumns());
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.addAll(getAttributeCategoryOptionGroupSetColumns());
    columns.addAll(getAttributeCategoryColumns());
    columns.addAll(getPeriodTypeColumns("ps"));
    columns.add(
        AnalyticsTableColumn.builder()
            .name("timely")
            .dataType(BOOLEAN)
            .selectExpression(timelyAlias)
            .build());
    columns.add(
        AnalyticsTableColumn.builder()
            .name("value")
            .dataType(TIMESTAMP)
            .valueType(FACT)
            .selectExpression("cdr.date as value")
            .build());

    return filterDimensionColumns(columns);
  }

  private List<Integer> getDataYears(AnalyticsTableUpdateParams params) {
    String sql =
        replaceQualify(
            sqlBuilder,
            """
            select distinct(extract(year from ps.startdate)) \
            from ${completedatasetregistration} cdr \
            inner join analytics_rs_periodstructure ps on cdr.periodid=ps.periodid \
            where ps.startdate is not null \
            and cdr.date < '${startTime}'""",
            Map.of("startTime", toLongDate(params.getStartTime())));

    if (params.getFromDate() != null) {
      sql +=
          replace(
              "and ps.startdate >= '${fromDate}'",
              Map.of("fromDate", DateUtils.toLongDate(params.getFromDate())));
    }

    return jdbcTemplate.queryForList(sql, Integer.class);
  }
}
