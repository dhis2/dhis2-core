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
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.system.util.SqlUtils.singleQuote;

import java.util.stream.Collectors;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;

public class PostgreSqlBuilder extends AbstractSqlBuilder {
  // Data types

  @Override
  public String typeSmallInt() {
    return "smallint";
  }

  @Override
  public String typeInteger() {
    return "integer";
  }

  @Override
  public String typeBigInt() {
    return "bigint";
  }

  @Override
  public String typeNumeric() {
    return "numeric(18,6)";
  }

  @Override
  public String typeReal() {
    return "real";
  }

  @Override
  public String typeDouble() {
    return "double precision";
  }

  @Override
  public String typeBoolean() {
    return "boolean";
  }

  @Override
  public String typeCharacter(int length) {
    return String.format("char(%d)", length);
  }

  @Override
  public String typeVarchar(int length) {
    return String.format("varchar(%d)", length);
  }

  @Override
  public String typeText() {
    return "text";
  }

  @Override
  public String typeDate() {
    return "date";
  }

  @Override
  public String typeTimestamp() {
    return "timestamp";
  }

  @Override
  public String typeTimestampTz() {
    return "timestamptz";
  }

  @Override
  public String typeTime() {
    return "time";
  }

  @Override
  public String typeTimeTz() {
    return "timetz";
  }

  @Override
  public String typeGeometry() {
    return "geometry";
  }

  @Override
  public String typeGeometryPoint() {
    return "geometry(Point, 4326)";
  }

  @Override
  public String typeJsonb() {
    return "jsonb";
  }

  // Capabilities

  @Override
  public boolean supportsAnalyze() {
    return true;
  }

  @Override
  public boolean supportsVacuum() {
    return true;
  }

  // Statements

  @Override
  public String createTable(Table table) {
    String unlogged = table.getLogged() == Logged.UNLOGGED ? "unlogged " : "";

    String sql = "create " + unlogged + "table " + quote(table.getName()) + " (";

    // Columns

    for (Column column : table.getColumns()) {
      String dataType = getDataTypeName(column.getDataType());
      String nullable = column.getNullable() == Nullable.NOT_NULL ? "not null" : "null";
      sql += quote(column.getName()) + " " + dataType + " " + nullable + ", ";
    }

    // Primary key

    if (table.hasPrimaryKey()) {
      sql += "primary key (";

      for (String columnName : table.getPrimaryKey()) {
        sql += quote(columnName) + ", ";
      }

      sql = removeLastComma(sql) + "),";
    }

    sql = removeLastComma(sql);

    return sql + ");";
  }

  @Override
  public String analyzeTable(Table table) {
    return String.format("analyze %s;", quote(table.getName()));
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
  public String dropTableIfExists(Table table) {
    return String.format("drop table if exists %s;", quote(table.getName()));
  }

  @Override
  public String dropTableIfExists(String name) {
    return String.format("drop table if exists %s;", quote(name));
  }

  @Override
  public String tableExists(Table table) {
    return String.format(
        "select t.table_name from information_schema.tables t "
            + "where t.table_schema = 'public' and t.table_name = %s;",
        singleQuote(table.getName()));
  }

  @Override
  public String createIndex(Table table, Index index) {
    String unique = index.getUnique() == Unique.UNIQUE ? "unique " : "";

    String columns =
        index.getColumns().stream().map(c -> quote(c)).collect(Collectors.joining(", "));

    return String.format(
        "create %sindex %s on %s (%s);",
        unique, quote(index.getName()), quote(table.getName()), columns);
  }
}
