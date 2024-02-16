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

import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;

public class DorisSqlBuilder extends AbstractSqlBuilder {
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
    return "datetime";  // to do
  }

  @Override
  public String dataTypeGeometry() {
    return "string";
  }

  @Override
  public String dataTypeGeometryPoint() {
    return null;
  }

  @Override
  public String dataTypeJson() {
    return "json";
  }

  // Index types

  @Override
  public String indexTypeBtree() {
    return notSupported();
  }

  @Override
  public String indexTypeGist() {
    return notSupported();
  }

  @Override
  public String indexTypeGin() {
    return notSupported();
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
  public boolean supportsAnalyze() {
    return false;
  }

  @Override
  public boolean supportsVacuum() {
    return false;
  }

  // Utilities

  @Override
  public String quote(String relation) {
    return null;
  }

  @Override
  public String quote(String alias, String relation) {
    return null;
  }

  @Override
  public String quoteAx(String relation) {
    return null;
  }

  @Override
  public String singleQuote(String value) {
    return SINGLE_QUOTE + escape(value) + SINGLE_QUOTE;
  }

  @Override
  public String escape(String value) {
    return value
        .replace(SINGLE_QUOTE, (SINGLE_QUOTE + SINGLE_QUOTE))
        .replace(BACKSLASH, (BACKSLASH + BACKSLASH));
  }

  // Statements

  @Override
  public String createTable(Table table) {
    return null;
  }

  @Override
  public String analyzeTable(Table table) {
    return null;
  }

  @Override
  public String analyzeTable(String name) {
    return null;
  }

  @Override
  public String vacuumTable(Table table) {
    return null;
  }

  @Override
  public String vacuumTable(String name) {
    return null;
  }

  @Override
  public String renameTable(Table table, String newName) {
    return null;
  }

  @Override
  public String dropTableIfExists(Table table) {
    return null;
  }

  @Override
  public String dropTableIfExists(String name) {
    return null;
  }

  @Override
  public String dropTableIfExistsCascade(Table table) {
    return null;
  }

  @Override
  public String dropTableIfExistsCascade(String name) {
    return null;
  }

  @Override
  public String swapTable(Table table, String newName) {
    return null;
  }

  @Override
  public String setParentTable(Table table, String parentName) {
    return null;
  }

  @Override
  public String removeParentTable(Table table, String parentName) {
    return null;
  }

  @Override
  public String swapParentTable(Table table, String parentName, String newParentName) {
    return null;
  }

  @Override
  public String tableExists(String name) {
    return null;
  }

  @Override
  public String createIndex(Index index) {
    return null;
  }
}
