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

import static org.hisp.dhis.analytics.table.model.Skip.SKIP;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.TEXT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.analytics.AnalyticsStringUtils;
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
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.beans.factory.annotation.Qualifier;
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
      JdbcTemplate jdbcTemplate,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      @Qualifier("postgresSqlBuilder") SqlBuilder sqlBuilder) {
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

  public static final String OU_GEOMETRY_COL_SUFFIX = "_geom";

  public static final String OU_NAME_COL_SUFFIX = "_name";

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
   * Returns a column expression, potentially with a cast statement, based on the given value type.
   *
   * @param valueType the {@link ValueType} to represent as database column type.
   * @param columnExpression the expression or name of the column to be selected.
   * @return a select expression appropriate for the given value type and context.
   */
  protected String getColumnExpression(ValueType valueType, String columnExpression) {
    if (valueType.isDecimal()) {
      return getCastExpression(columnExpression, NUMERIC_REGEXP, sqlBuilder.dataTypeDouble());
    } else if (valueType.isInteger()) {
      return getCastExpression(columnExpression, NUMERIC_REGEXP, sqlBuilder.dataTypeBigInt());
    } else if (valueType.isBoolean()) {
      return sqlBuilder.ifThenElse(
          columnExpression + " = 'true'", "1", columnExpression + " = 'false'", "0", "null");
    } else if (valueType.isDate()) {
      return getCastExpression(columnExpression, DATE_REGEXP, sqlBuilder.dataTypeTimestamp());
    } else if (valueType.isGeo() && isGeospatialSupport()) {
      return String.format(
          """
          ST_GeomFromGeoJSON('{"type":"Point", "coordinates":' || (%s) || \
          ', "crs":{"type":"name", "properties":{"name":"EPSG:4326"}}}')""",
          columnExpression);
    } else {
      return columnExpression;
    }
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
    String result = String.format("cast(%s as %s)", columnExpression, dataType);

    return sqlBuilder.ifThen(filter, result);
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
    sql += toCommaSeparated(columns, col -> quote(col.getName()));
    sql += ") select ";
    sql += toCommaSeparated(columns, AnalyticsTableColumn::getSelectExpression);
    sql += " " + fromClause;

    invokeTimeAndLog(sql, "Populating table: '{}'", tableName);
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  protected List<AnalyticsTableColumn> getColumnForAttribute(TrackedEntityAttribute attribute) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    String valueColumn = getValueColumn(attribute);
    DataType dataType = getColumnType(attribute.getValueType(), isGeospatialSupport());
    String selectExpression = getColumnExpression(attribute.getValueType(), valueColumn);
    Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

    if (attribute.getValueType().isOrganisationUnit()) {
      columns.addAll(getColumnForOrgUnitAttribute(attribute));
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name(attribute.getUid())
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(dataType)
            .selectExpression(selectExpression)
            .skipIndex(skipIndex)
            .build());

    return columns;
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getColumnForOrgUnitAttribute(
      TrackedEntityAttribute attribute) {
    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return List.of();
    }

    Validate.isTrue(attribute.getValueType().isOrganisationUnit());
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    if (isGeospatialSupport()) {
      columns.add(
          AnalyticsTableColumn.builder()
              .name((attribute.getUid() + OU_GEOMETRY_COL_SUFFIX))
              .dimensionType(AnalyticsDimensionType.DYNAMIC)
              .dataType(GEOMETRY)
              .selectExpression(getOrgUnitSelectSubquery(attribute, "geometry"))
              .indexType(IndexType.GIST)
              .build());
    }

    columns.add(
        AnalyticsTableColumn.builder()
            .name((attribute.getUid() + OU_NAME_COL_SUFFIX))
            .dimensionType(AnalyticsDimensionType.DYNAMIC)
            .dataType(TEXT)
            .selectExpression(getOrgUnitSelectSubquery(attribute, "name"))
            .skipIndex(SKIP)
            .build());

    return columns;
  }

  /**
   * Returns the value column with alias.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return the vlaue column with alias.
   */
  private String getValueColumn(TrackedEntityAttribute attribute) {
    return String.format("%s.%s", quote(attribute.getUid()), "value");
  }

  /**
   * Returns a org unit select query.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param column the column name.
   * @return an org unit select query.
   */
  private String getOrgUnitSelectSubquery(TrackedEntityAttribute attribute, String column) {
    String valueColumn = getValueColumn(attribute);
    String columnExpression = getColumnExpression(attribute.getValueType(), valueColumn);
    String alias = quote(attribute.getUid());

    return buildOrgUnitSelectSubquery(column, columnExpression, alias);
  }

  private String buildOrgUnitSelectSubquery(String column, String columnExpression, String alias) {
    String baseFormat =
        "(select ou.${column} from ${organisationunit} ou where ou.uid = ${columnExpression})";
    String finalFormat = wrapWithCentroid(column, baseFormat) + " as ${alias}";

    return AnalyticsStringUtils.replaceQualify(
        sqlBuilder,
        finalFormat,
        Map.of(
            "column", column,
            "columnExpression", columnExpression,
            "alias", alias));
  }

  /**
   * Wraps the base query format with ST_Centroid if the column is geometry and the current settings
   * allow it.
   *
   * @param column the column name.
   * @param baseFormat the base SQL format string.
   * @return the format string, optionally wrapped with ST_Centroid.
   */
  protected String wrapWithCentroid(String column, String baseFormat) {
    return column.equals("geometry")
            && this.settingsProvider.getCurrentSettings().getOrgUnitCentroidsInEventsAnalytics()
        ? "ST_Centroid(" + baseFormat + ")"
        : baseFormat;
  }

  /**
   * Returns a join clause for attribute value for every attribute of the given program.
   *
   * @param program the {@link Program}.
   * @return a join clause.
   */
  protected String getAttributeValueJoinClause(Program program) {
    String template =
        """
        left join trackedentityattributevalue as ${uid} \
        on en.trackedentityid=${uid}.trackedentityid \
        and ${uid}.trackedentityattributeid = ${id}\s""";

    return program.getNonConfidentialTrackedEntityAttributes().stream()
        .map(attribute -> replaceQualify(template, toVariableMap(attribute)))
        .collect(Collectors.joining());
  }
}
