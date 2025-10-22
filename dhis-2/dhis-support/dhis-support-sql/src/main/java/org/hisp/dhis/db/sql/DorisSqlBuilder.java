/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.sql;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.db.model.DateUnit;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.TablePartition;
import org.hisp.dhis.db.model.constraint.Nullable;

/**
 * Implementation of {@link SqlBuilder} for Apache Doris.
 *
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DorisSqlBuilder extends AbstractSqlBuilder {

  private final String catalog;

  private final String driverFilename;

  // Constants

  private static final String QUOTE = "`";

  // Database

  @Override
  public Database getDatabase() {
    return Database.DORIS;
  }

  // Data types

  @Override
  public String dataTypeSmallInt() {
    return "smallint";
  }

  @Override
  public String dataTypeInteger() {
    return "int";
  }

  @Override
  public String dataTypeBigInt() {
    return "bigint";
  }

  @Override
  public String dataTypeDecimal() {
    return "decimal(18,6)";
  }

  @Override
  public String dataTypeFloat() {
    return "float";
  }

  @Override
  public String dataTypeDouble() {
    return "double";
  }

  @Override
  public String dataTypeBoolean() {
    return "boolean";
  }

  @Override
  public String dataTypeCharacter(int length) {
    return String.format("char(%d)", length);
  }

  @Override
  public String dataTypeVarchar(int length) {
    return String.format("varchar(%d)", length);
  }

  @Override
  public String dataTypeText() {
    return "string";
  }

  @Override
  public String dataTypeDate() {
    return "date";
  }

  @Override
  public String dataTypeTimestamp() {
    return "datetime(3)";
  }

  @Override
  public String dataTypeTimestampTz() {
    return "datetime(3)";
  }

  @Override
  public String dataTypeGeometry() {
    return "string";
  }

  @Override
  public String dataTypeGeometryPoint() {
    return "string";
  }

  @Override
  public String dataTypeJson() {
    return "json";
  }

  // Index functions

  @Override
  public String indexFunctionUpper() {
    return "upper";
  }

  @Override
  public String indexFunctionLower() {
    return "lower";
  }

  // Capabilities

  @Override
  public boolean supportsGeospatialData() {
    return false;
  }

  @Override
  public boolean supportsDeclarativePartitioning() {
    return true;
  }

  @Override
  public boolean supportsAnalyze() {
    return false;
  }

  @Override
  public boolean supportsVacuum() {
    return false;
  }

  @Override
  public boolean supportsCorrelatedSubquery() {
    return true;
  }

  @Override
  public boolean supportsMultiStatements() {
    return true;
  }

  @Override
  public boolean supportsUpdateForMultiKeyTable() {
    return false;
  }

  @Override
  public boolean requiresIndexesForAnalytics() {
    return false;
  }

  @Override
  public boolean supportsPercentileCont() {
    return false;
  }

  // Utilities

  @Override
  public String quote(String relation) {
    String escapedRelation = relation.replace(QUOTE, (QUOTE + QUOTE));
    return QUOTE + escapedRelation + QUOTE;
  }

  @Override
  public String escape(String value) {
    return value
        .replace(SINGLE_QUOTE, (SINGLE_QUOTE + SINGLE_QUOTE))
        .replace(BACKSLASH, (BACKSLASH + BACKSLASH));
  }

  @Override
  public String qualifyTable(String name) {
    return String.format("%s.%s.%s", catalog, SCHEMA, quote(name));
  }

  @Override
  public String dateTrunc(String text, String timestamp) {
    return String.format("date_trunc(%s, %s)", timestamp, singleQuote(text));
  }

  @Override
  public String safeConcat(String... columns) {
    return "concat("
        + Arrays.stream(columns)
            .map(this::wrapTrimNullIf) // Adjust wrapping logic
            .collect(Collectors.joining(", "))
        + ")";
  }

  @Override
  public String differenceInSeconds(String columnA, String columnB) {
    return String.format("(unix_timestamp(%s) - unix_timestamp(%s))", columnA, columnB);
  }

  @Override
  public String regexpMatch(String value, String pattern) {
    return String.format("%s regexp %s", value, pattern);
  }

  @Override
  public String coalesce(String expression, String defaultValue) {
    return "coalesce(" + expression + ", " + defaultValue + ")";
  }

  @Override
  public String jsonExtract(String json, String property) {
    return String.format("json_unquote(json_extract(%s, '$.%s'))", json, property);
  }

  @Override
  public String jsonExtract(String json, String key, String property) {
    String path = "$." + String.join(".", key, property);
    return String.format("json_unquote(json_extract(%s, '%s'))", json, path);
  }

  @Override
  public String cast(String column, DataType dataType) {
    return switch (dataType) {
      case NUMERIC -> String.format("CAST(%s AS DECIMAL)", column);
      case BOOLEAN -> String.format("CAST(%s AS DECIMAL) != 0", column);
      case TEXT -> String.format("CAST(%s AS CHAR)", column);
    };
  }

  @Override
  public String dateDifference(String startDate, String endDate, DateUnit dateUnit) {
    return switch (dateUnit) {
      case DAYS -> String.format("DATEDIFF(%s, %s)", endDate, startDate);
      case MINUTES -> String.format("TIMESTAMPDIFF(MINUTE, %s, %s)", startDate, endDate);
      case MONTHS -> String.format("TIMESTAMPDIFF(MONTH, %s, %s)", startDate, endDate);
      case YEARS -> String.format("TIMESTAMPDIFF(YEAR, %s, %s)", startDate, endDate);
      case WEEKS -> String.format("TIMESTAMPDIFF(WEEK, %s, %s)", startDate, endDate);
    };
  }

  @Override
  public String isTrue(String alias, String column) {
    return String.format("%s.%s = true", alias, quote(column));
  }

  @Override
  public String isFalse(String alias, String column) {
    return String.format("%s.%s = false", alias, quote(column));
  }

  @Override
  public String ifThen(String condition, String result) {
    return String.format("case when %s then %s end", condition, result);
  }

  @Override
  public String ifThenElse(String condition, String thenResult, String elseResult) {
    return String.format("case when %s then %s else %s end", condition, thenResult, elseResult);
  }

  @Override
  public String ifThenElse(
      String conditionA,
      String thenResultA,
      String conditionB,
      String thenResultB,
      String elseResult) {
    return String.format(
        "case when %s then %s when %s then %s else %s end",
        conditionA, thenResultA, conditionB, thenResultB, elseResult);
  }

  /**
   * For more information, see <a
   * href="https://doris.apache.org/docs/3.0/sql-manual/sql-functions/scalar-functions/numeric-functions/log">Apache
   * Doris Log function</a>.
   */
  @Override
  public String log10(String expression) {
    return String.format("log(10, %s)", expression);
  }

  @Override
  public String stddev(String expression) {
    return String.format("stddev(%s)", expression);
  }

  @Override
  public String variance(String expression) {
    return String.format("variance(%s)", expression);
  }

  // Statements

  @Override
  public String createTable(Table table) {
    Validate.isTrue(table.hasPrimaryKey() || table.hasColumns());

    StringBuilder sql =
        new StringBuilder("create table ").append(quote(table.getName())).append(" ");

    // Columns

    if (table.hasColumns()) {
      String columns = toCommaSeparated(table.getColumns(), this::toColumnString);

      sql.append("(").append(columns).append(") engine = olap ");
    }

    // Primary key

    if (table.hasPrimaryKey()) {
      // If primary key exists, use it as keys with the unique model
      String keys = toCommaSeparated(table.getPrimaryKey(), this::quote);

      sql.append("unique key (").append(keys).append(") ");
    } else if (table.hasColumns()) {
      // If columns exist, use first as key with the duplicate model
      String key = quote(table.getFirstColumn().getName());

      sql.append("duplicate key (").append(key).append(") ");
    }

    // Partitions

    if (table.hasPartitions()) {
      sql.append(generatePartitionClause(table.getPartitions()));
    }

    // Distribution

    if (table.hasPrimaryKey() || table.hasColumns()) {
      String distKey = getDistKey(table);

      sql.append("distributed by hash(")
          .append(quote(distKey))
          .append(") ")
          .append("buckets 10 "); // Verify this
    }

    // Properties

    sql.append("properties (\"replication_num\" = \"1\")");

    return sql.append(";").toString();
  }

  /**
   * Generates the partition clause for the table creation SQL.
   *
   * @param partitions the list of table partitions
   * @return the partition clause string
   */
  private String generatePartitionClause(List<TablePartition> partitions) {
    StringBuilder partitionClause =
        new StringBuilder("partition by range(year) ("); // Make configurable

    List<TablePartition> sortedPartitions;
    try {
      sortedPartitions =
          partitions.stream()
              .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.getValue().toString())))
              .toList();
    } catch (NumberFormatException e) {
      sortedPartitions = partitions;
    }

    for (int i = 0; i < sortedPartitions.size(); i++) {
      if (i == sortedPartitions.size() - 1) {
        // Handle last partition with MAXVALUE
        partitionClause
            .append("partition ")
            .append(quote(sortedPartitions.get(i).getName()))
            .append(" values less than(MAXVALUE),");
      } else {
        partitionClause.append(toPartitionString(sortedPartitions.get(i))).append(",");
      }
    }
    return partitionClause.substring(0, partitionClause.length() - 1) + ") ";
  }

  /**
   * Returns a column definition string.
   *
   * @param column the {@link Column}.
   * @return a column clause.
   */
  private String toColumnString(Column column) {
    String dataType = getDataTypeName(column.getDataType());
    String nullable = column.getNullable() == Nullable.NOT_NULL ? " not null" : " null";
    return quote(column.getName()) + " " + dataType + nullable;
  }

  /**
   * Returns a partition definition string.
   *
   * @param partition the {@link TablePartition}.
   * @return a partition definition string.
   */
  private String toPartitionString(TablePartition partition) {
    String condition = "values less than(\"" + partition.getValue() + "\")";
    return "partition " + quote(partition.getName()) + " " + condition;
  }

  /**
   * Returns the distribution key. Uses the first primary key column name if any exists, or the
   * first column name if any exists, otherwise null.
   *
   * @param table {@link Table}.
   */
  private String getDistKey(Table table) {
    if (table.hasPrimaryKey()) {
      return table.getFirstPrimaryKey();
    } else if (table.hasColumns()) {
      return table.getFirstColumn().getName();
    }

    return null;
  }

  @Override
  public String renameTable(Table table, String newName) {
    return String.format("alter table %s rename %s;", quote(table.getName()), quote(newName));
  }

  @Override
  public String dropTableIfExistsCascade(Table table) {
    return dropTableIfExists(table);
  }

  @Override
  public String dropTableIfExistsCascade(String name) {
    return dropTableIfExists(name);
  }

  @Override
  public String tableExists(String name) {
    return String.format(
        """
        select t.table_name from information_schema.tables t \
        where t.table_schema = 'public' \
        and t.table_name = %s;""",
        singleQuote(name));
  }

  /**
   * Doris supports indexes but relies on concurrency and compression for query performance instead
   * of indexes on arbitrary columns. Read more at {@link https://t.ly/AHhJ1}.
   */
  @Override
  public String createIndex(Index index) {
    return notSupported();
  }

  // Normalization

  // Catalog

  /**
   * Returns a create catalog statement.
   *
   * @param connectionUrl the JDBC connection URL.
   * @param username the JDBC connection username.
   * @param password the JDBC connection password.
   * @return a create catalog statement.
   */
  public String createCatalog(String connectionUrl, String username, String password) {
    return replace(
        """
        create catalog ${catalog} \
        properties (\
        "type" = "jdbc", \
        "user" = "${username}", \
        "password" = "${password}", \
        "jdbc_url" = "${connection_url}", \
        "driver_url" = "${driver_filename}", \
        "driver_class" = "org.postgresql.Driver");""",
        Map.of(
            "catalog", quote(catalog),
            "username", username,
            "password", password,
            "connection_url", connectionUrl,
            "driver_filename", driverFilename));
  }

  /**
   * Ensures {@code trim(nullif(...))} regardless of incoming column formatting.
   *
   * @param column the column to be wrapped.
   * @return the wrapped column.
   */
  private String wrapTrimNullIf(String column) {
    // If the column is a literal, return it as is
    if (isSingleQuoted(column)) {
      return column;
    }

    // If the column already contains 'trim', insert 'nullif' inside 'trim'
    if (column.startsWith("trim(")) {
      String innerValue = column.substring(5, column.length() - 1);
      return "trim(nullif('', " + innerValue + "))";
    }

    // For other cases, apply both 'trim' and 'nullif'
    return "trim(nullif('', " + column + "))";
  }

  /**
   * @return a drop catalog if exists statement.
   */
  public String dropCatalogIfExists() {
    return String.format("drop catalog if exists %s;", quote(catalog));
  }
}
