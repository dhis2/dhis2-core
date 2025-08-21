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
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsColumnType;
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
import org.hisp.dhis.db.sql.SqlBuilder;
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

  private static final Pattern ALIAS_PATTERN = Pattern.compile("\\s+as\\s+\\S+$");

  public static final String OU_NAME_COL_SUFFIX = "_name";

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

  /**
   * Returns the select clause, potentially with a cast statement, based on the given value type.
   *
   * @param valueType the value type to represent as database column type.
   */
  protected String getSelectClause(ValueType valueType, String columnName) {
    if (valueType.isDecimal()) {
      return "cast(" + columnName + " as double precision)";
    } else if (valueType.isInteger()) {
      return "cast(" + columnName + " as bigint)";
    } else if (valueType.isBoolean()) {
      return "case when "
          + columnName
          + " = 'true' then 1 when "
          + columnName
          + " = 'false' then 0 else null end";
    } else if (valueType.isDate()) {
      return "cast(" + columnName + " as timestamp)";
    } else if (valueType.isGeo() && isSpatialSupport()) {
      return "ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || ("
          + columnName
          + ") || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}')";
    } else if (valueType.isOrganisationUnit()) {
      return "ou.uid from organisationunit ou where ou.uid = (select " + columnName;
    } else {
      return columnName;
    }
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

  protected List<AnalyticsTableColumn> getTrackedEntityAttributeColumns(Program program) {
    List<AnalyticsTableColumn> columns = new ArrayList<>();

    for (TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes()) {
      DataType dataType = getColumnType(attribute.getValueType(), isSpatialSupport());
      String dataClause =
          attribute.isNumericType()
              ? getNumericClause()
              : attribute.isDateType() ? getDateClause() : "";
      String select = getSelectClause(attribute.getValueType(), "value");
      Skip skipIndex = skipIndex(attribute.getValueType(), attribute.hasOptionSet());

      String sql =
          replace(
              """
              (select ${select} from trackedentityattributevalue \
              where trackedentityid=pi.trackedentityid \
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
              .columnType(AnalyticsColumnType.DYNAMIC)
              .dataType(dataType)
              .selectExpression(sql)
              .skipIndex(skipIndex)
              .build());

      if (attribute.getValueType().isOrganisationUnit()) {
        String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select value";
        String ouNameSql = selectForInsert(attribute, fromTypeSql, dataClause);

        columns.add(
            AnalyticsTableColumn.builder()
                .name((attribute.getUid() + OU_NAME_COL_SUFFIX))
                .columnType(AnalyticsColumnType.DYNAMIC)
                .dataType(TEXT)
                .selectExpression(ouNameSql)
                .skipIndex(SKIP)
                .build());
      }
    }
    return columns;
  }

  /**
   * The select statement used by the table population.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param fromType the sql snippet related to "from" part
   * @param dataClause the data type related clause like "NUMERIC"
   * @return
   */
  protected String selectForInsert(
      TrackedEntityAttribute attribute, String fromType, String dataClause) {
    return replace(
        """
            (select ${fromType} from trackedentityattributevalue \
            where trackedentityid=pi.trackedentityid \
            and trackedentityattributeid=${attributeId}\
            ${dataClause})\
            ${closingParentheses} as ${attributeUid}""",
        Map.of(
            "fromType", fromType,
            "dataClause", dataClause,
            "attributeId", String.valueOf(attribute.getId()),
            "closingParentheses", getClosingParentheses(fromType),
            "attributeUid", quote(attribute.getUid())));
  }

  /**
   * The select statement used by the table population, without the alias part.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @param fromType the sql snippet related to "from" part
   * @param dataClause the data type related clause like "NUMERIC"
   * @return the select statement without alias
   */
  protected String selectForInsertWithoutAlias(
      TrackedEntityAttribute attribute, String fromType, String dataClause) {
    String sqlWithAlias = selectForInsert(attribute, fromType, dataClause);
    // Remove the alias part (everything from " as " to the end)
    return ALIAS_PATTERN.matcher(sqlWithAlias).replaceAll("");
  }
}
