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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsDimensionType;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
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
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      DatabaseInfoProvider databaseInfoProvider,
      JdbcTemplate jdbcTemplate,
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
  }

  public static final String OU_GEOMETRY_COL_SUFFIX = "_geom";

  public static final String OU_NAME_COL_SUFFIX = "_name";

  protected final String getNumericClause() {
    return " and " + sqlBuilder.regexpMatch("value", "'" + NUMERIC_LENIENT_REGEXP + "'");
  }

  protected final String getDateClause() {
    return " and " + sqlBuilder.regexpMatch("value", DATE_REGEXP);
  }

  /**
   * Indicates whether creating an index should be skipped.
   *
   * @param valueType the {@link ValueType}.
   * @param hasOptionSet whether an option set exists.
   * @return a {@link Skip}.
   */
  protected Skip skipIndex(ValueType valueType, boolean hasOptionSet) {
    boolean skipIndex = NO_INDEX_VAL_TYPES.contains(valueType) && !hasOptionSet;
    return skipIndex ? Skip.SKIP : Skip.INCLUDE;
  }

  /**
   * Returns a select expression for a data element value, handling casting to the appropriate data
   * type based on the given value type.
   *
   * @param valueType the {@link ValueType}.
   * @param columnName the column name.
   * @return a select expression.
   */
  protected String getSelectExpression(ValueType valueType, String columnName) {
    return getSelectExpression(valueType, columnName, false);
  }

  /**
   * Returns a select expression for a tracked entity attribute, handling casting to the appropriate
   * data type based on the given value type.
   *
   * @param valueType the {@link ValueType}.
   * @param columnName the column name.
   * @return a select expression.
   */
  protected String getSelectExpressionForAttribute(ValueType valueType, String columnName) {
    return getSelectExpression(valueType, columnName, true);
  }

  /**
   * Returns a select expression, potentially with a cast statement, based on the given value type.
   * Handles data element and tracked entity attribute select expressions.
   *
   * @param valueType the {@link ValueType} to represent as database column type.
   * @param columnExpression the expression or name of the column to be selected.
   * @param isTea whether the selection is in the context of a tracked entity attribute. When true,
   *     organisation unit selections will include an additional subquery wrapper.
   * @return a select expression appropriate for the given value type and context.
   */
  private String getSelectExpression(ValueType valueType, String columnExpression, boolean isTea) {
    if (valueType.isDecimal()) {
      return getCastExpression(columnExpression, NUMERIC_REGEXP, sqlBuilder.dataTypeDouble());
    } else if (valueType.isInteger()) {
      return getCastExpression(columnExpression, NUMERIC_REGEXP, sqlBuilder.dataTypeBigInt());
    } else if (valueType.isBoolean()) {
      return String.format(
          "case when %1$s = 'true' then 1 when %1$s = 'false' then 0 else null end",
          columnExpression);
    } else if (valueType.isDate()) {
      return getCastExpression(columnExpression, DATE_REGEXP, sqlBuilder.dataTypeTimestamp());
    } else if (valueType.isGeo() && isSpatialSupport()) {
      return String.format(
          """
          ST_GeomFromGeoJSON('{"type":"Point", "coordinates":' || (%s) || ', "crs":{"type":"name", "properties":{"name":"EPSG:4326"}}}')""",
          columnExpression);
    } else if (valueType.isOrganisationUnit()) {
      String ouClause =
          isTea
              ? "ou.uid from ${organisationunit} ou where ou.uid = (select ${columnName}"
              : "ou.uid from ${organisationunit} ou where ou.uid = ${columnName}";
      return replaceQualify(ouClause, Map.of("columnName", columnExpression));
    } else {
      return columnExpression;
    }
  }

  /**
   * For numeric and date value types, returns a data filter clause for checking whether the value
   * is valid according to the value type. For other value types, returns the empty string.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a data filter clause.
   */
  protected String getDataFilterClause(TrackedEntityAttribute attribute) {
    if (attribute.isNumericType()) {
      return getNumericClause();
    } else if (attribute.isDateType()) {
      return getDateClause();
    }
    return EMPTY;
  }

  /**
   * Returns a cast expression which includes a value filter for the given value type.
   *
   * @param columnExpression the column expression.
   * @param filterRegex the value type filter regular expression.
   * @param dataType the SQL data type.
   * @return a cast and validate expression.
   */
  protected String getCastExpression(String columnExpression, String filterRegex, String dataType) {
    String filter = sqlBuilder.regexpMatch(columnExpression, filterRegex);
    return String.format(
        "case when %s then cast(%s as %s) else null end", filter, columnExpression, dataType);
  }

  @Override
  public boolean validState() {
    return tableIsNotEmpty("event");
  }

  /**
   * Populates the given analytics table partition using the given columns and join statement.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   * @param fromClause the SQL from clause.
   */
  protected void populateTableInternal(AnalyticsTablePartition partition, String fromClause) {
    String tableName = partition.getName();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    String sql = "insert into " + tableName + " (";

    for (AnalyticsTableColumn col : columns) {
      sql += quote(col.getName()) + ",";
    }

    sql = TextUtils.removeLastComma(sql) + ") select ";

    for (AnalyticsTableColumn col : columns) {
      sql += col.getSelectExpression() + ",";
    }

    sql = TextUtils.removeLastComma(sql) + " ";

    sql += fromClause;

    invokeTimeAndLog(sql, "Populating table: '{}'", tableName);
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnaylyticsTableColumn}.
   */
  protected List<AnalyticsTableColumn> getColumnForAttribute(TrackedEntityAttribute attribute) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    DataType dataType = getColumnType(attribute.getValueType(), isSpatialSupport());
    String selectExpression = getSelectExpressionForAttribute(attribute.getValueType(), "value");
    String dataFilterClause = getDataFilterClause(attribute);
    String sql = getSelectSubquery(attribute, selectExpression, dataFilterClause);
    Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

    if (attribute.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnForOrgUnitTrackedEntityAttribute(attribute, dataFilterClause));
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(attribute.getUid())
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(sql)
            .skipIndex(skipIndex)
            .build());

    return columns;
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param dataFilterClause the data filter clause.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForOrgUnitTrackedEntityAttribute(
      TrackedEntityAttribute attribute, String dataFilterClause) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String fromClause =
        qualifyVariables("from ${organisationunit} ou where ou.uid = (select value");

    if (isSpatialSupport()) {
      String selectExpression = "ou.geometry " + fromClause;
      String ouGeoSql = getSelectSubquery(attribute, selectExpression, dataFilterClause);
      columns.add(
          AnalyticsTableColumn.builder()
              .name((attribute.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(ouGeoSql)
              .indexType(IndexType.GIST)
              .build());
    }

    String selectExpression = "ou.name " + fromClause;
    String ouNameSql = getSelectSubquery(attribute, selectExpression, dataFilterClause);

    columns.add(
        AnalyticsTableColumn.builder()
            .name((attribute.getUid() + OU_NAME_COL_SUFFIX))
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(ouNameSql)
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * The select subquery statement.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param selectExpression the select expression.
   * @param dataFilterClause the data filter clause.
   * @return a select statement.
   */
  private String getSelectSubquery(
      TrackedEntityAttribute attribute, String selectExpression, String dataFilterClause) {
    return replaceQualify(
        """
        (select ${selectExpression} from ${trackedentityattributevalue} \
        where trackedentityid=en.trackedentityid \
        and trackedentityattributeid=${attributeId}${dataFilterClause})\
        ${closingParentheses} as ${attributeUid}""",
        Map.of(
            "selectExpression", selectExpression,
            "dataFilterClause", dataFilterClause,
            "attributeId", String.valueOf(attribute.getId()),
            "closingParentheses", getClosingParentheses(selectExpression),
            "attributeUid", quote(attribute.getUid())));
  }
}
