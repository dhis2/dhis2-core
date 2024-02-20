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

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.TablePartition;
import org.hisp.dhis.db.model.constraint.Nullable;

public class DorisSqlBuilder extends AbstractSqlBuilder {

  @Override
  public String dataTypeSmallInt() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeInteger() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeBigInt() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeDecimal() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeFloat() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeDouble() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeBoolean() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeCharacter(int length) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeVarchar(int length) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeText() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeDate() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeTimestamp() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeTimestampTz() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeGeometry() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeGeometryPoint() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dataTypeJson() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String indexTypeBtree() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String indexTypeGist() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String indexTypeGin() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String indexFunctionUpper() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean supportsDeclarativePartitioning() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String indexFunctionLower() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean supportsAnalyze() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean supportsVacuum() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String createTable(Table table) {
    Validate.isTrue(table.hasPrimaryKey()); // ?

    StringBuilder sql =
        new StringBuilder("create table ").append(quote(table.getName())).append(" ");

    // Columns

    if (table.hasColumns()) {
      sql.append("(");

      for (Column column : table.getColumns()) {
        String dataType = getDataTypeName(column.getDataType());
        String nullable = column.getNullable() == Nullable.NOT_NULL ? " not null" : " null";

        sql.append(quote(column.getName()) + " ").append(dataType).append(nullable).append(", ");
      }

      removeLastComma(sql).append(") engine=olap ");
    }

    // Primary key

    if (table.hasPrimaryKey()) {
      sql.append("duplicate key (");

      for (String columnName : table.getPrimaryKey()) {
        sql.append(quote(columnName) + ", ");
      }

      removeLastComma(sql).append(") ");
    }

    // Partitions

    if (table.hasPartitions()) {
      sql.append("partition by range(year) (");

      for (TablePartition partition : table.getPartitions()) {
        sql.append("partition ")
            .append(quote(partition.getName()))
            .append(" values less than(\"")
            .append(partition.getValue())
            .append("\"),");
      }

      removeLastComma(sql).append(") ");
    }

    // Distribution

    if (table.hasPrimaryKey()) {
      sql.append("distributed by hash(")
          .append(quote(table.getFirstPrimaryKey()))
          .append(") ")
          .append("buckets = 10 "); // TODO check
    }

    // Properties

    sql.append("properties (\"replication_num\" = \"1\")");

    return sql.append(";").toString();
  }

  @Override
  public String analyzeTable(Table table) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String analyzeTable(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String vacuumTable(Table table) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String renameTable(Table table, String newName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dropTableIfExists(Table table) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dropTableIfExists(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dropTableIfExistsCascade(Table table) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String dropTableIfExistsCascade(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String swapTable(Table table, String newName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String setParentTable(Table table, String parentName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String removeParentTable(Table table, String parentName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String swapParentTable(Table table, String parentName, String newParentName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String tableExists(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String createIndex(Index index) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean supportsGeospatialData() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean requiresIndexesForAnalytics() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String quote(String relation) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String quote(String alias, String relation) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String singleQuote(String value) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String escape(String value) {
    // TODO Auto-generated method stub
    return null;
  }
}
