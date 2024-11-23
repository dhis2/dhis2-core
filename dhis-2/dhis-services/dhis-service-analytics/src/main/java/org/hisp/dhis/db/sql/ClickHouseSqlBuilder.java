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
import java.util.Map.Entry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;

@Getter
@RequiredArgsConstructor
public class ClickHouseSqlBuilder extends AbstractSqlBuilder {

  // Constants

  public static final String NAMED_COLLECTION = "pg_dhis";

  private static final String QUOTE = "\"";

  // Data types

  @Override
  public String dataTypeSmallInt() {
    return "Int16";
  }

  @Override
  public String dataTypeInteger() {
    return "Int32";
  }

  @Override
  public String dataTypeBigInt() {
    return "Int64";
  }

  @Override
  public String dataTypeDecimal() {
    return "Decimal(10,6)";
  }

  @Override
  public String dataTypeFloat() {
    return "Float32";
  }

  @Override
  public String dataTypeDouble() {
    return "Float64";
  }

  @Override
  public String dataTypeBoolean() {
    return "Bool";
  }

  @Override
  public String dataTypeCharacter(int length) {
    return "String";
  }

  @Override
  public String dataTypeVarchar(int length) {
    return "String";
  }

  @Override
  public String dataTypeText() {
    return "String";
  }

  @Override
  public String dataTypeDate() {
    return "Date";
  }

  @Override
  public String dataTypeTimestamp() {
    return "DateTime64(3)";
  }

  @Override
  public String dataTypeTimestampTz() {
    return "DateTime64(3)";
  }

  @Override
  public String dataTypeGeometry() {
    return "String";
  }

  @Override
  public String dataTypeGeometryPoint() {
    return "String";
  }

  @Override
  public String dataTypeJson() {
    return "JSON";
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
    return value.replace(SINGLE_QUOTE, (SINGLE_QUOTE + SINGLE_QUOTE));
  }

  /** Uses the <code>postgresql</code> table function to query DHIS 2 PostgreSQL server. */
  @Override
  public String qualifyTable(String name) {
    return String.format("postgresql(%s, table=%s)", quote(NAMED_COLLECTION), singleQuote(name));
  }

  @Override
  public String dateTrunc(String text, String timestamp) {
    return String.format("date_trunc(%s, %s)", singleQuote(text), timestamp);
  }

  @Override
  public String differenceInSeconds(String columnA, String columnB) {
    return String.format("(toUnixTimestamp(%s) - toUnixTimestamp(%s))", columnA, columnB);
  }

  @Override
  public String regexpMatch(String value, String pattern) {
    return String.format("match(%s, %s)", value, pattern);
  }

  @Override
  public String concat(String... columns) {
    return "concat(" + String.join(", ", columns) + ")";
  }

  @Override
  public String trim(String expression) {
    return "trim(" + expression + ")";
  }

  @Override
  public String coalesce(String expression, String defaultValue) {
    return "coalesce(" + expression + ", " + defaultValue + ")";
  }

  @Override
  public String jsonExtract(String column, String property) {
    return "JSONExtractRaw(" + column + ", '" + property + "')";
  }

  @Override
  public String jsonExtract(String tablePrefix, String column, String jsonPath) {
    return String.format("JSONExtractRaw(%s.%s, '%s')", tablePrefix, column, jsonPath);
  }

  @Override
  public String jsonExtractNested(String column, String... jsonPath) {
    String path = String.join(".", jsonPath);
    return String.format("JSONExtract(%s, '%s')", column, path);
  }

  // Statements

  @Override
  public String createTable(Table table) {
    Validate.notEmpty(table.getColumns());

    StringBuilder sql =
        new StringBuilder("create table ").append(quote(table.getName())).append(" ");

    // Columns

    if (table.hasColumns()) {
      String columns = toCommaSeparated(table.getColumns(), this::toColumnString);

      sql.append("(").append(columns).append(") engine = MergeTree() ");
    }

    // Order by

    sql.append(getOrderByClause(table));

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
   * Returns an order by clause. The primary key columns will be used for ordering. ClickHouse will
   * use order by columns as primary key when the primary key clause is omitted. The primary key
   * does not have to uniquely identify each row.
   *
   * @param table the {@link Table}.
   * @return the order by clause.
   */
  private String getOrderByClause(Table table) {
    String keys = null;

    if (table.hasSortKey()) {
      keys = toCommaSeparated(table.getSortKey(), this::quote);
    } else if (table.hasPrimaryKey()) {
      keys = toCommaSeparated(table.getPrimaryKey(), this::quote);
    } else {
      keys = quote(table.getFirstColumn().getName());
    }

    return String.format("order by (%s)", keys);
  }

  @Override
  public String renameTable(Table table, String newName) {
    return String.format("rename table %s to %s;", quote(table.getName()), quote(newName));
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
        select t.name as table_name \
        from system.tables t \
        where t.database = 'default' \
        and t.name = %s \
        and engine not in ('View', 'Materialized View');""",
        singleQuote(name));
  }

  @Override
  public String createIndex(Index index) {
    return notSupported();
  }

  @Override
  public String createCatalog(String connectionUrl, String username, String password) {
    return notSupported();
  }

  @Override
  public String dropCatalogIfExists() {
    return notSupported();
  }

  /**
   * @param name the collection name.
   * @param keyValues the map of key value pairs.
   * @return a create named collection statement.
   */
  public String createNamedCollection(String name, Map<String, Object> keyValues) {
    String pairs = toCommaSeparated(keyValues.entrySet(), this::toPairString);
    return String.format("create named collection %s as %s;", quote(name), pairs);
  }

  /**
   * @param name the collection name.
   * @return a drop named collection if exists statement.
   */
  public String dropNamedCollectionIfExists(String name) {
    return String.format("drop named collection if exists %s;", quote(name));
  }

  /**
   * Converts the given {@link Map} {@link Entry} to a key value pair string.
   *
   * @param pair the {@link Entry}.
   * @return a key value pair string.
   */
  private String toPairString(Entry<String, Object> pair) {
    return String.format(
        "%s = %s", quote(pair.getKey()), singleQuote(String.valueOf(pair.getValue())));
  }
}
