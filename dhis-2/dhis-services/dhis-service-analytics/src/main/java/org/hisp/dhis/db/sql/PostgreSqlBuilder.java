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

import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;

import org.hisp.dhis.db.model.Collation;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;

/**
 * Implementation of {@link SqlBuilder} for PostgreSQL.
 *
 * @author Lars Helge Overland
 */
public class PostgreSqlBuilder extends AbstractSqlBuilder {

  // Constants

  private static final String QUOTE = "\"";

  // Data types

  @Override
  public String dataTypeSmallInt() {
    return "smallint";
  }

  @Override
  public String dataTypeInteger() {
    return "integer";
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
    return "double precision";
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
    return "text";
  }

  @Override
  public String dataTypeDate() {
    return "date";
  }

  @Override
  public String dataTypeTimestamp() {
    return "timestamp";
  }

  @Override
  public String dataTypeTimestampTz() {
    return "timestamptz";
  }

  @Override
  public String dataTypeGeometry() {
    return "geometry";
  }

  @Override
  public String dataTypeGeometryPoint() {
    return "geometry(Point, 4326)";
  }

  @Override
  public String dataTypeJson() {
    return "jsonb";
  }

  // Index types

  @Override
  public String indexTypeBtree() {
    return "btree";
  }

  @Override
  public String indexTypeGist() {
    return "gist";
  }

  @Override
  public String indexTypeGin() {
    return "gin";
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
    return true;
  }

  /**
   * PostgreSQL supports declarative partitioning, but table inheritance is used as query
   * performance is better.
   */
  @Override
  public boolean supportsDeclarativePartitioning() {
    return false;
  }

  @Override
  public boolean supportsAnalyze() {
    return true;
  }

  @Override
  public boolean supportsVacuum() {
    return true;
  }

  @Override
  public boolean requiresIndexesForAnalytics() {
    return true;
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
    return quote(name);
  }

  @Override
  public String dateTrunc(String text, String timestamp) {
    return String.format("date_trunc(%s, %s)", singleQuote(text), timestamp);
  }

  // Statements

  @Override
  public String createTable(Table table) {
    String unlogged = table.isUnlogged() ? " unlogged" : "";

    StringBuilder sql =
        new StringBuilder("create")
            .append(unlogged)
            .append(" table ")
            .append(quote(table.getName()))
            .append(" (");

    // Columns

    if (table.hasColumns()) {
      for (Column column : table.getColumns()) {
        String dataType = getDataTypeName(column.getDataType());
        String nullable = column.getNullable() == Nullable.NOT_NULL ? " not null" : " null";
        String collation = column.getCollation() == Collation.C ? (" collate " + quote("C")) : "";

        sql.append(quote(column.getName()) + " ")
            .append(dataType)
            .append(nullable)
            .append(collation)
            .append(COMMA);
      }
    }

    // Primary key

    if (table.hasPrimaryKey()) {
      sql.append("primary key (");

      for (String columnName : table.getPrimaryKey()) {
        sql.append(quote(columnName)).append(COMMA);
      }

      removeLastComma(sql).append(")").append(COMMA);
    }

    // Checks

    if (table.hasChecks()) {
      for (String check : table.getChecks()) {
        sql.append("check(" + check + ")").append(COMMA);
      }
    }

    removeLastComma(sql).append(")");

    // Parent

    if (table.hasParent()) {
      sql.append(" inherits (").append(quote(table.getParent().getName())).append(")");
    }

    return sql.append(";").toString();
  }

  @Override
  public String analyzeTable(String name) {
    return String.format("analyze %s;", quote(name));
  }

  @Override
  public String vacuumTable(Table table) {
    return String.format("vacuum %s;", quote(table.getName()));
  }

  @Override
  public String renameTable(Table table, String newName) {
    return String.format("alter table %s rename to %s;", quote(table.getName()), quote(newName));
  }

  @Override
  public String dropTableIfExists(String name) {
    return String.format("drop table if exists %s;", quote(name));
  }

  @Override
  public String dropTableIfExistsCascade(Table table) {
    return dropTableIfExistsCascade(table.getName());
  }

  @Override
  public String dropTableIfExistsCascade(String name) {
    return String.format("drop table if exists %s cascade;", quote(name));
  }

  @Override
  public String setParentTable(Table table, String parentName) {
    return String.format("alter table %s inherit %s;", quote(table.getName()), quote(parentName));
  }

  @Override
  public String removeParentTable(Table table, String parentName) {
    return String.format(
        "alter table %s no inherit %s;", quote(table.getName()), quote(parentName));
  }

  @Override
  public String tableExists(String name) {
    return String.format(
        """
        select t.table_name from information_schema.tables t \
        where t.table_schema = 'public' and t.table_name = %s;""",
        singleQuote(name));
  }

  @Override
  public String createIndex(Index index) {
    String unique = index.getUnique() == Unique.UNIQUE ? "unique " : "";
    String tableName = index.getTableName();
    String typeName = getIndexTypeName(index.getIndexType());
    String columns = toCommaSeparated(index.getColumns(), col -> toIndexColumn(index, col));
    String sortOrder = index.getSortOrder();

    return sortOrder == null
        ? String.format(
            "create %sindex %s on %s using %s(%s);",
            unique, quote(index.getName()), quote(tableName), typeName, columns)
        : String.format(
            "create %sindex %s on %s using %s(%s %s);",
            unique, quote(index.getName()), quote(tableName), typeName, columns, sortOrder);
  }

  @Override
  public String createCatalog(String connectionUrl, String username, String password) {
    return notSupported();
  }

  @Override
  public String dropCatalogIfExists() {
    return notSupported();
  }
}
