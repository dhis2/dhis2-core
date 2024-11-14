/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.db.sql;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.TablePartition;
import org.hisp.dhis.db.model.constraint.Nullable;

@RequiredArgsConstructor
public class DorisSqlBuilder extends AbstractSqlBuilder {

  private final String catalog;

  private final String driverFilename;

  // Constants

  private static final String QUOTE = "`";

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
    return "datetime";
  }

  @Override
  public String dataTypeTimestampTz() {
    return "datetime";
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
  public boolean requiresIndexesForAnalytics() {
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
      String partitions = toCommaSeparated(table.getPartitions(), this::toPartitionString);

      sql.append("partition by range(year) (").append(partitions).append(") "); // Make configurable
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
  public String dropTableIfExists(String name) {
    return String.format("drop table if exists %s;", quote(name));
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
   * of indexes on arbitrary columns. Read more at {@link https://t.ly/uNK5T}.
   */
  @Override
  public String createIndex(Index index) {
    return notSupported();
  }

  @Override
  public String createCatalog(String connectionUrl, String username, String password) {
    return replace(
        """
        create catalog ${catalog} \
        properties (
        "type" = "jdbc", \
        "user" = "${username}", \
        "password" = "${password}", \
        "jdbc_url" = "${connection_url}", \
        "driver_url" = "${driver_filename}", \
        "driver_class" = "org.postgresql.Driver"
        );""",
        Map.of(
            "catalog", quote(catalog),
            "username", username,
            "password", password,
            "connection_url", connectionUrl,
            "driver_filename", driverFilename));
  }

  @Override
  public String dropCatalogIfExists() {
    return String.format("drop catalog if exists %s;", quote(catalog));
  }
}
