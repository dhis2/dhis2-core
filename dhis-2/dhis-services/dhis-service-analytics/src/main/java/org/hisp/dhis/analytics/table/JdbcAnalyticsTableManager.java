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

import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.table.PartitionUtils.getLatestTablePartition;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.dataapproval.DataApprovalLevelService.APPROVAL_LEVEL_UNAPPROVED;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class manages the analytics tables. The analytics table is a denormalized table designed for
 * analysis which contains raw data values.
 *
 * <p>The analytics table is horizontally partitioned. The partition key is the start date of the
 * period of the data record. The table is partitioned according to time span with one partition per
 * calendar quarter.
 *
 * <p>The data records in this table are not aggregated. Typically, queries will aggregate in
 * organisation unit hierarchy dimension, in the period/time dimension, and the category dimensions,
 * as well as organisation unit group set dimensions.
 *
 * <p>This analytics table is partitioned by year.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service("org.hisp.dhis.analytics.AnalyticsTableManager")
public class JdbcAnalyticsTableManager extends AbstractJdbcTableManager {
  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          new AnalyticsTableColumn(quote("dx"), CHARACTER_11, NOT_NULL, "de.uid"),
          new AnalyticsTableColumn(quote("co"), CHARACTER_11, NOT_NULL, "co.uid")
              .withIndexColumns(List.of(quote("dx"), quote("co"))),
          new AnalyticsTableColumn(quote("ao"), CHARACTER_11, NOT_NULL, "ao.uid")
              .withIndexColumns(List.of(quote("dx"), quote("ao"))),
          new AnalyticsTableColumn(quote("pestartdate"), TIMESTAMP, "pe.startdate"),
          new AnalyticsTableColumn(quote("peenddate"), TIMESTAMP, "pe.enddate"),
          new AnalyticsTableColumn(quote("year"), INTEGER, NOT_NULL, "ps.year"),
          new AnalyticsTableColumn(quote("pe"), TEXT, NOT_NULL, "ps.iso"),
          new AnalyticsTableColumn(quote("ou"), CHARACTER_11, NOT_NULL, "ou.uid"),
          new AnalyticsTableColumn(quote("oulevel"), INTEGER, "ous.level"));

  public JdbcAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingManager systemSettingManager,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      StatementBuilder statementBuilder,
      PartitionManager partitionManager,
      DatabaseInfoProvider databaseInfoProvider,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      AnalyticsExportSettings analyticsExportSettings,
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
        databaseInfoProvider,
        jdbcTemplate,
        analyticsExportSettings,
        periodDataProvider);
  }

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return AnalyticsTableType.DATA_VALUE;
  }

  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    AnalyticsTable table =
        params.isLatestUpdate()
            ? getLatestAnalyticsTable(params, getDimensionColumns(), getValueColumns())
            : getRegularAnalyticsTable(
                params, getDataYears(params), getDimensionColumns(), getValueColumns());

    return table.hasPartitionTables() ? List.of(table) : List.of();
  }

  @Override
  public String validState() {
    boolean hasData =
        jdbcTemplate
            .queryForRowSet(
                "select dataelementid from datavalue dv where dv.deleted is false limit 1")
            .next();

    if (!hasData) {
      return "No data values exist, not updating aggregate analytics tables";
    }

    int orgUnitLevels = organisationUnitService.getNumberOfOrganisationalLevels();

    if (orgUnitLevels == 0) {
      return "No organisation unit levels exist, not updating aggregate analytics tables";
    }

    return null;
  }

  @Override
  protected boolean hasUpdatedLatestData(Date startDate, Date endDate) {
    String sql =
        "select dv.dataelementid "
            + "from datavalue dv "
            + "where dv.lastupdated >= '"
            + getLongDateString(startDate)
            + "' "
            + "and dv.lastupdated < '"
            + getLongDateString(endDate)
            + "' "
            + "limit 1";

    return !jdbcTemplate.queryForList(sql).isEmpty();
  }

  @Override
  public void preCreateTables(AnalyticsTableUpdateParams params) {
    if (isApprovalEnabled(null)) {
      resourceTableService.generateDataApprovalRemapLevelTable();
      resourceTableService.generateDataApprovalMinLevelTable();
    }
  }

  @Override
  public void removeUpdatedData(List<AnalyticsTable> tables) {
    AnalyticsTablePartition partition = getLatestTablePartition(tables);
    String sql =
        "delete from "
            + quote(getAnalyticsTableType().getTableName())
            + " ax "
            + "where ax.id in ("
            + "select (de.uid || '-' || ps.iso || '-' || ou.uid || '-' || co.uid || '-' || ao.uid) as id "
            + "from datavalue dv "
            + "inner join dataelement de on dv.dataelementid=de.dataelementid "
            + "inner join _periodstructure ps on dv.periodid=ps.periodid "
            + "inner join organisationunit ou on dv.sourceid=ou.organisationunitid "
            + "inner join categoryoptioncombo co on dv.categoryoptioncomboid=co.categoryoptioncomboid "
            + "inner join categoryoptioncombo ao on dv.attributeoptioncomboid=ao.categoryoptioncomboid "
            + "where dv.lastupdated >= '"
            + getLongDateString(partition.getStartDate())
            + "' "
            + "and dv.lastupdated < '"
            + getLongDateString(partition.getEndDate())
            + "')";

    invokeTimeAndLog(sql, "Remove updated data values");
  }

  @Override
  protected List<String> getPartitionChecks(AnalyticsTablePartition partition) {
    return partition.isLatestPartition()
        ? List.of()
        : List.of(
            "year = " + partition.getYear() + "",
            "pestartdate < '" + DateUtils.getMediumDateString(partition.getEndDate()) + "'");
  }

  @Override
  protected void populateTable(
      AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String dbl = statementBuilder.getDoubleColumnType();
    boolean skipDataTypeValidation =
        systemSettingManager.getBoolSetting(
            SettingKey.SKIP_DATA_TYPE_VALIDATION_IN_ANALYTICS_TABLE_EXPORT);
    boolean includeZeroValues =
        systemSettingManager.getBoolSetting(SettingKey.INCLUDE_ZERO_VALUES_IN_ANALYTICS);

    String numericClause =
        skipDataTypeValidation
            ? ""
            : ("and dv.value "
                + statementBuilder.getRegexpMatch()
                + " '"
                + MathUtils.NUMERIC_LENIENT_REGEXP
                + "' ");
    String zeroValueCondition = includeZeroValues ? " or de.zeroissignificant = true" : "";
    String zeroValueClause =
        "(dv.value != '0' or de.aggregationtype in ('"
            + AggregationType.AVERAGE
            + "','"
            + AggregationType.AVERAGE_SUM_ORG_UNIT
            + "')"
            + zeroValueCondition
            + ") ";
    String intClause = zeroValueClause + numericClause;

    populateTable(
        params,
        partition,
        "cast(dv.value as " + dbl + ")",
        "null",
        ValueType.NUMERIC_TYPES,
        intClause);
    populateTable(
        params,
        partition,
        "1",
        "null",
        Sets.newHashSet(ValueType.BOOLEAN, ValueType.TRUE_ONLY),
        "dv.value = 'true'");
    populateTable(
        params, partition, "0", "null", Sets.newHashSet(ValueType.BOOLEAN), "dv.value = 'false'");
    populateTable(
        params,
        partition,
        "null",
        "dv.value",
        Sets.union(ValueType.TEXT_TYPES, ValueType.DATE_TYPES),
        null);
  }

  /**
   * Populates the given analytics table.
   *
   * @param valueExpression numeric value expression.
   * @param textValueExpression textual value expression.
   * @param valueTypes data element value types to include data for.
   * @param whereClause where clause to constrain data query.
   */
  private void populateTable(
      AnalyticsTableUpdateParams params,
      AnalyticsTablePartition partition,
      String valueExpression,
      String textValueExpression,
      Set<ValueType> valueTypes,
      String whereClause) {
    String tableName = partition.getTempTableName();
    String valTypes = TextUtils.getQuotedCommaDelimitedString(ObjectUtils.asStringList(valueTypes));
    boolean respectStartEndDates =
        systemSettingManager.getBoolSetting(
            SettingKey.RESPECT_META_DATA_START_END_DATES_IN_ANALYTICS_TABLE_EXPORT);
    String approvalClause = getApprovalJoinClause(partition.getYear());
    String partitionClause =
        partition.isLatestPartition()
            ? "and dv.lastupdated >= '" + getLongDateString(partition.getStartDate()) + "' "
            : "and ps.year = " + partition.getYear() + " ";

    String sql = "insert into " + partition.getTempTableName() + " (";

    List<AnalyticsTableColumn> columns = getDimensionColumns(partition.getYear(), params);
    List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

    validateDimensionColumns(columns);

    for (AnalyticsTableColumn col : ListUtils.union(columns, values)) {
      sql += col.getName() + ",";
    }

    sql = TextUtils.removeLastComma(sql) + ") select ";

    for (AnalyticsTableColumn col : columns) {
      sql += col.getAlias() + ",";
    }

    sql +=
        valueExpression
            + " * ps.daysno as daysxvalue, "
            + "ps.daysno as daysno, "
            + valueExpression
            + " as value, "
            + textValueExpression
            + " as textvalue "
            + "from datavalue dv "
            + "inner join period pe on dv.periodid=pe.periodid "
            + "inner join _periodstructure ps on dv.periodid=ps.periodid "
            + "left join periodtype pt on pe.periodtypeid = pt.periodtypeid "
            + "inner join dataelement de on dv.dataelementid=de.dataelementid "
            + "inner join _dataelementstructure des on dv.dataelementid = des.dataelementid "
            + "inner join _dataelementgroupsetstructure degs on dv.dataelementid=degs.dataelementid "
            + "inner join organisationunit ou on dv.sourceid=ou.organisationunitid "
            + "left join _orgunitstructure ous on dv.sourceid=ous.organisationunitid "
            + "inner join _organisationunitgroupsetstructure ougs on dv.sourceid=ougs.organisationunitid "
            + "and (cast(date_trunc('month', pe.startdate) as date)=ougs.startdate or ougs.startdate is null) "
            + "inner join categoryoptioncombo co on dv.categoryoptioncomboid=co.categoryoptioncomboid "
            + "inner join categoryoptioncombo ao on dv.attributeoptioncomboid=ao.categoryoptioncomboid "
            + "inner join _categorystructure dcs on dv.categoryoptioncomboid=dcs.categoryoptioncomboid "
            + "inner join _categorystructure acs on dv.attributeoptioncomboid=acs.categoryoptioncomboid "
            + "inner join _categoryoptioncomboname aon on dv.attributeoptioncomboid=aon.categoryoptioncomboid "
            + "inner join _categoryoptioncomboname con on dv.categoryoptioncomboid=con.categoryoptioncomboid ";

    if (!skipOutliers(params)) {
      sql += getOutliersJoinStatement();
    }

    sql +=
        approvalClause
            + "where de.valuetype in ("
            + valTypes
            + ") "
            + "and de.domaintype = 'AGGREGATE' "
            + partitionClause
            + "and dv.lastupdated < '"
            + getLongDateString(params.getStartTime())
            + "' "
            + "and dv.value is not null "
            + "and dv.deleted is false ";

    if (respectStartEndDates) {
      sql +=
          "and (aon.startdate is null or aon.startdate <= pe.startdate) "
              + "and (aon.enddate is null or aon.enddate >= pe.enddate) "
              + "and (con.startdate is null or con.startdate <= pe.startdate) "
              + "and (con.enddate is null or con.enddate >= pe.enddate) ";
    }

    if (whereClause != null) {
      sql += "and " + whereClause;
    }

    invokeTimeAndLog(sql, String.format("Populate %s %s", tableName, valueTypes));
  }

  /**
   * Returns sub-query for approval level. First looks for approval level in data element resource
   * table which will indicate level 0 (highest) if approval is not required. Then looks for highest
   * level in dataapproval table.
   *
   * @param year the data year.
   */
  private String getApprovalJoinClause(Integer year) {
    if (isApprovalEnabled(year)) {
      String sql =
          "left join _dataapprovalminlevel da "
              + "on des.workflowid=da.workflowid and da.periodid=dv.periodid "
              + "and da.attributeoptioncomboid=dv.attributeoptioncomboid "
              + "and (";

      Set<OrganisationUnitLevel> levels =
          dataApprovalLevelService.getOrganisationUnitApprovalLevels();

      for (OrganisationUnitLevel level : levels) {
        sql += "ous.idlevel" + level.getLevel() + " = da.organisationunitid or ";
      }

      return TextUtils.removeLastOr(sql) + ") ";
    }

    return StringUtils.EMPTY;
  }

  private List<AnalyticsTableColumn> getDimensionColumns() {
    return getDimensionColumns(null, null);
  }

  private List<AnalyticsTableColumn> getDimensionColumns(
      Integer year, AnalyticsTableUpdateParams params) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String idColAlias =
        "(de.uid || '-' || ps.iso || '-' || ou.uid || '-' || co.uid || '-' || ao.uid) as id ";
    columns.add(new AnalyticsTableColumn(quote("id"), ColumnDataType.TEXT, idColAlias));

    List<DataElementGroupSet> dataElementGroupSets =
        idObjectManager.getDataDimensionsNoAcl(DataElementGroupSet.class);

    List<OrganisationUnitGroupSet> orgUnitGroupSets =
        idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class);

    List<CategoryOptionGroupSet> disaggregationCategoryOptionGroupSets =
        categoryService.getDisaggregationCategoryOptionGroupSetsNoAcl();

    List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
        categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

    List<Category> disaggregationCategories =
        categoryService.getDisaggregationDataDimensionCategoriesNoAcl();

    List<Category> attributeCategories = categoryService.getAttributeDataDimensionCategoriesNoAcl();

    List<OrganisationUnitLevel> levels = organisationUnitService.getFilledOrganisationUnitLevels();

    for (DataElementGroupSet groupSet : dataElementGroupSets) {
      columns.add(
          new AnalyticsTableColumn(
                  quote(groupSet.getUid()), CHARACTER_11, "degs." + quote(groupSet.getUid()))
              .withCreated(groupSet.getCreated()));
    }

    for (OrganisationUnitGroupSet groupSet : orgUnitGroupSets) {
      columns.add(
          new AnalyticsTableColumn(
                  quote(groupSet.getUid()), CHARACTER_11, "ougs." + quote(groupSet.getUid()))
              .withCreated(groupSet.getCreated()));
    }

    for (CategoryOptionGroupSet groupSet : disaggregationCategoryOptionGroupSets) {
      columns.add(
          new AnalyticsTableColumn(
                  quote(groupSet.getUid()), CHARACTER_11, "dcs." + quote(groupSet.getUid()))
              .withCreated(groupSet.getCreated()));
    }

    for (CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets) {
      columns.add(
          new AnalyticsTableColumn(
                  quote(groupSet.getUid()), CHARACTER_11, "acs." + quote(groupSet.getUid()))
              .withCreated(groupSet.getCreated()));
    }

    for (Category category : disaggregationCategories) {
      columns.add(
          new AnalyticsTableColumn(
                  quote(category.getUid()), CHARACTER_11, "dcs." + quote(category.getUid()))
              .withCreated(category.getCreated()));
    }

    for (Category category : attributeCategories) {
      columns.add(
          new AnalyticsTableColumn(
                  quote(category.getUid()), CHARACTER_11, "acs." + quote(category.getUid()))
              .withCreated(category.getCreated()));
    }

    for (OrganisationUnitLevel level : levels) {
      String column = quote(PREFIX_ORGUNITLEVEL + level.getLevel());
      columns.add(
          new AnalyticsTableColumn(column, CHARACTER_11, "ous." + column)
              .withCreated(level.getCreated()));
    }

    columns.addAll(addPeriodTypeColumns("ps"));

    String approvalCol =
        isApprovalEnabled(year)
            ? "coalesce(des.datasetapprovallevel, aon.approvallevel, da.minlevel, "
                + APPROVAL_LEVEL_UNAPPROVED
                + ") as approvallevel "
            : DataApprovalLevelService.APPROVAL_LEVEL_HIGHEST + " as approvallevel";

    columns.add(new AnalyticsTableColumn(quote("approvallevel"), INTEGER, approvalCol));
    columns.addAll(getFixedColumns());
    if (!skipOutliers(params)) {
      columns.addAll(getOutlierStatsColumns());
    }

    return filterDimensionColumns(columns);
  }

  /**
   * Returns a list of columns representing data value.
   *
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getValueColumns() {
    return List.of(
        new AnalyticsTableColumn(quote("daysxvalue"), DOUBLE, "daysxvalue"),
        new AnalyticsTableColumn(quote("daysno"), INTEGER, NOT_NULL, "daysno"),
        new AnalyticsTableColumn(quote("value"), DOUBLE, "value"),
        new AnalyticsTableColumn(quote("textvalue"), TEXT, "textvalue"));
  }

  private List<AnalyticsTableColumn> getOutlierStatsColumns() {
    return List.of(
        new AnalyticsTableColumn(quote("de_uid"), CHARACTER_11, NOT_NULL, "de.uid"),
        new AnalyticsTableColumn(quote("coc_uid"), CHARACTER_11, NOT_NULL, "co.uid"),
        new AnalyticsTableColumn(quote("aoc_uid"), CHARACTER_11, NOT_NULL, "ao.uid"),
        new AnalyticsTableColumn(quote("ou_uid"), CHARACTER_11, NOT_NULL, "ou.uid"),
        new AnalyticsTableColumn(quote("dataelementid"), INTEGER, NOT_NULL, "dv.dataelementid")
            .withIndexColumns(List.of(quote("dataelementid"))),
        new AnalyticsTableColumn(quote("sourceid"), INTEGER, NOT_NULL, "dv.sourceid"),
        new AnalyticsTableColumn(quote("periodid"), INTEGER, NOT_NULL, "dv.periodid"),
        new AnalyticsTableColumn(
            quote("categoryoptioncomboid"), INTEGER, NOT_NULL, "dv.categoryoptioncomboid"),
        new AnalyticsTableColumn(
            quote("attributeoptioncomboid"), INTEGER, NOT_NULL, "dv.attributeoptioncomboid"),
        new AnalyticsTableColumn(quote("de_name"), VARCHAR_255, "de.name"),
        new AnalyticsTableColumn(quote("ou_name"), VARCHAR_255, "ou.name"),
        new AnalyticsTableColumn(quote("coc_name"), VARCHAR_255, "co.name"),
        new AnalyticsTableColumn(quote("aoc_name"), VARCHAR_255, "ao.name"),
        new AnalyticsTableColumn(quote("pt_name"), VARCHAR_255, "pt.name"),
        new AnalyticsTableColumn(quote("path"), VARCHAR_255, "ou.path"),
        new AnalyticsTableColumn(quote("avg_middle_value"), DOUBLE, "stats.avg_middle_value"),
        new AnalyticsTableColumn(
            quote("percentile_middle_value"), DOUBLE, "stats.percentile_middle_value"),
        new AnalyticsTableColumn(quote("mad"), DOUBLE, "stats.mad"),
        new AnalyticsTableColumn(quote("std_dev"), DOUBLE, "stats.std_dev"));
  }

  /**
   * Returns the distinct years which contain data values, relative to the from date in the given
   * parameters, if it exists.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return a list of data years.
   */
  private List<Integer> getDataYears(AnalyticsTableUpdateParams params) {
    String sql =
        "select distinct(extract(year from pe.startdate)) "
            + "from datavalue dv "
            + "inner join period pe on dv.periodid=pe.periodid "
            + "where pe.startdate is not null "
            + "and dv.lastupdated < '"
            + getLongDateString(params.getStartTime())
            + "' ";

    if (params.getFromDate() != null) {
      sql += "and pe.startdate >= '" + DateUtils.getMediumDateString(params.getFromDate()) + "'";
    }

    return jdbcTemplate.queryForList(sql, Integer.class);
  }

  @Override
  public void applyAggregationLevels(
      AnalyticsTablePartition partition, Collection<String> dataElements, int aggregationLevel) {
    StringBuilder sql = new StringBuilder("update " + partition.getTempTableName() + " set ");

    for (int i = 0; i < aggregationLevel; i++) {
      int level = i + 1;

      String column = quote(DataQueryParams.LEVEL_PREFIX + level);

      sql.append(column + " = null,");
    }

    sql.deleteCharAt(sql.length() - ",".length());

    sql.append(" where oulevel > " + aggregationLevel);
    sql.append(" and dx in (" + getQuotedCommaDelimitedString(dataElements) + ")");

    log.debug("Aggregation level SQL: " + sql);

    jdbcTemplate.execute(sql.toString());
  }

  @Override
  public void vacuumTables(AnalyticsTablePartition partition) {
    String sql = statementBuilder.getVacuum(partition.getTempTableName());

    log.debug("Vacuum SQL: " + sql);

    jdbcTemplate.execute(sql);
  }

  @Override
  public List<AnalyticsTableColumn> getFixedColumns() {
    return FIXED_COLS;
  }

  /**
   * Indicates whether the system should ignore data which has not been approved in analytics
   * tables.
   *
   * @param year the year of the data partition.
   */
  private boolean isApprovalEnabled(Integer year) {
    boolean setting = systemSettingManager.hideUnapprovedDataInAnalytics();
    boolean levels = !dataApprovalLevelService.getAllDataApprovalLevels().isEmpty();
    Integer maxYears =
        systemSettingManager.getIntegerSetting(SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD);

    log.debug(
        String.format(
            "Hide approval setting: %b, approval levels exists: %b, max years threshold: %d",
            setting, levels, maxYears));

    if (year != null) {
      boolean periodOverMaxYears = AnalyticsUtils.periodIsOutsideApprovalMaxYears(year, maxYears);

      return setting && levels && !periodOverMaxYears;
    } else {
      return setting && levels;
    }
  }

  private String getOutliersJoinStatement() {
    return "left join (select t3.dataelementid, "
        + "                           t3.sourceid, "
        + "                           t3.categoryoptioncomboid, "
        + "                           t3.attributeoptioncomboid, "
        + "                           percentile_cont(0.5) "
        + "                           within group (order by abs(t3.value::double precision - t3.percentile_middle_value)) as MAD, "
        + "                           avg(t3.value::double precision)                                                 as avg_middle_value, "
        + "                           percentile_cont(0.5) "
        + "                           within group (order by t3.value::double precision)                              as percentile_middle_value, "
        + "                           stddev_pop(t3.value::double precision)                                          as std_dev "
        + "                    from (select t1.dataelementid, "
        + "                                 t1.sourceid, "
        + "                                 t1.categoryoptioncomboid, "
        + "                                 t1.attributeoptioncomboid, "
        + "                                 t1.percentile_middle_value, "
        + "                                 t2.value "
        + "                          from (select dv1.dataelementid                                   as dataelementid, "
        + "                                       dv1.sourceid                                        as sourceid, "
        + "                                       dv1.categoryoptioncomboid                           as categoryoptioncomboid, "
        + "                                       dv1.attributeoptioncomboid                          as attributeoptioncomboid, "
        + "                                       percentile_cont(0.5) "
        + "                                       within group (order by dv1.value::double precision) as percentile_middle_value "
        + "                                from datavalue dv1 "
        + "                                         inner join period pe on dv1.periodid = pe.periodid "
        + "                                         inner join organisationunit ou on dv1.sourceid = ou.organisationunitid "
        + "                                where dv1.value ~ '^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$' "
        + "                                group by dv1.dataelementid, dv1.sourceid, dv1.categoryoptioncomboid, "
        + "                                         dv1.attributeoptioncomboid) t1 "
        + "                                   join "
        + "                               (select dv1.dataelementid          as dataelementid, "
        + "                                       dv1.sourceid               as sourceid, "
        + "                                       dv1.categoryoptioncomboid  as categoryoptioncomboid, "
        + "                                       dv1.attributeoptioncomboid as attributeoptioncomboid, "
        + "                                       dv1.value, "
        + "                                       dv1.periodid "
        + "                                from datavalue dv1 "
        + "                                         inner join period pe on dv1.periodid = pe.periodid "
        + "                                         inner join organisationunit ou on dv1.sourceid = ou.organisationunitid "
        + "                                where dv1.value ~ '^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$' "
        + "                                group by dv1.dataelementid, dv1.sourceid, dv1.categoryoptioncomboid, "
        + "                                         dv1.attributeoptioncomboid, dv1.value, dv1.periodid) t2 "
        + "                               on t1.sourceid = t2.sourceid "
        + "                                   and t1.categoryoptioncomboid = t2.categoryoptioncomboid "
        + "                                   and t1.attributeoptioncomboid = t2.attributeoptioncomboid "
        + "                                   and t1.dataelementid = t2.dataelementid) as t3 "
        + "                    group by t3.dataelementid, t3.sourceid, t3.categoryoptioncomboid, "
        + "                             t3.attributeoptioncomboid) as stats "
        + "                   on dv.dataelementid = stats.dataelementid and dv.sourceid = stats.sourceid and "
        + "                      dv.categoryoptioncomboid = stats.categoryoptioncomboid and "
        + "                      dv.attributeoptioncomboid = stats.attributeoptioncomboid ";
  }

  private boolean skipOutliers(AnalyticsTableUpdateParams params) {
    return params != null && params.isSkipOutliers();
  }
}
