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

import java.util.Collection;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.IndexFunction;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Table;

/**
 * Provides methods for generation of SQL statements and queries.
 *
 * @author Lars Helge Overland
 */
public interface SqlBuilder {
  // Data types

  /**
   * @return the name of the small integer data type.
   */
  String dataTypeSmallInt();

  /**
   * @return the name of the integer data type.
   */
  String dataTypeInteger();

  /**
   * @return the name of the big integer data type.
   */
  String dataTypeBigInt();

  /**
   * @return the name of the numeric data type.
   */
  String dataTypeNumeric();

  /**
   * @return the name of the real data type.
   */
  String dataTypeReal();

  /**
   * @return the name of the double data type.
   */
  String dataTypeDouble();

  /**
   * @return the name of the boolean data type.
   */
  String dataTypeBoolean();

  /**
   * @param length the character length.
   * @return the name of the character data type.
   */
  String dataTypeCharacter(int length);

  /**
   * @param length the character length.
   * @return the name of the character varying data type.
   */
  String dataTypeVarchar(int length);

  /**
   * @return the name of the text data type.
   */
  String dataTypeText();

  /**
   * @return the name of the date data type.
   */
  String dataTypeDate();

  /**
   * @return the name of the timestamp data type.
   */
  String dataTypeTimestamp();

  /**
   * @return the name of the timestamp with time zone data type.
   */
  String dataTypeTimestampTz();

  /**
   * @return the name of the time data type.
   */
  String dataTypeTime();

  /**
   * @return the name of the time with time zone data type.
   */
  String dataTypeTimeTz();

  /**
   * @return the name of the geometry data type.
   */
  String dataTypeGeometry();

  /**
   * @return the name of the geometry point data type.
   */
  String dataTypeGeometryPoint();

  /**
   * @return the name of the JSONB data type.
   */
  String dataTypeJsonb();

  /**
   * @param dataType the {@link DataType}.
   * @return the data type name.
   */
  String getDataTypeName(DataType dataType);

  // Index types

  /**
   * @return the name of the B-Tree index type.
   */
  String indexTypeBtree();

  /**
   * @return the name of the GiST index type.
   */
  String indexTypeGist();

  /**
   * @return the name of the GIN index type.
   */
  String indexTypeGin();

  /**
   * @param indexType the {@link IndexType}.
   * @return the index type name.
   */
  String getIndexTypeName(IndexType indexType);

  // Index functions

  /**
   * @return the name of the upper index function.
   */
  String indexFunctionUpper();

  /**
   * @return the name of the lower index function.
   */
  String indexFunctionLower();

  /**
   * @param indexFunction the {@link IndexFunction}.
   * @return the index function name.
   */
  String getIndexFunctionName(IndexFunction indexFunction);

  // Capabilities

  /**
   * @return true if the DBMS supports table analysis.
   */
  boolean supportsAnalyze();

  /**
   * @return true if the DBMS supports table vacuuming.
   */
  boolean supportsVacuum();

  // Utilities

  /**
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double quoted relation.
   */
  String quote(String relation);

  /**
   * @param relation the relation to quote, e.g. a column name.
   * @return an aliased and double quoted relation.
   */
  String quote(String alias, String relation);

  /**
   * @param relation the relation to quote.
   * @return an "ax" aliased and double quoted relation.
   */
  String quoteAx(String relation);

  /**
   * @param value the value to quote.
   * @return a single quoted value.
   */
  String singleQuote(String value);

  /**
   * @param value the value to escape.
   * @return the escaped value, with single quotes doubled up.
   */
  String escape(String value);

  /**
   * @param items the items to join.
   * @return a string representing the comma delimited and single quoted item values.
   */
  String singleQuotedCommaDelimited(Collection<String> items);

  // Statements

  /**
   * @param table the {@link Table}.
   * @return a create table statement.
   */
  String createTable(Table table);

  /**
   * @param table the {@link Table}.
   * @return an analyze table statement.
   */
  String analyzeTable(Table table);

  /**
   * @param name the table name.
   * @return an analyze table statement.
   */
  String analyzeTable(String name);

  /**
   * @param table the {@link Table}.
   * @return a vacuum table statement.
   */
  String vacuumTable(Table table);

  /**
   * @param name the table name.
   * @return a vacuum table statement.
   */
  String vacuumTable(String name);

  /**
   * @param table the {@link Table}.
   * @param newName the new name for the table.
   * @return a rename table statement.
   */
  String renameTable(Table table, String newName);

  /**
   * @param table the {@link Table}.
   * @return a drop table if exists statement.
   */
  String dropTableIfExists(Table table);

  /**
   * @param name the table name.
   * @return a drop table if exists statement.
   */
  String dropTableIfExists(String name);

  /**
   * @param table the {@link Table}.
   * @return a drop table if exists cascade statement.
   */
  String dropTableIfExistsCascade(Table table);

  /**
   * @param name the table name.
   * @return a drop table if exists cascade statement.
   */
  String dropTableIfExistsCascade(String name);

  /**
   * @param table the {@link Table}.
   * @param newName the new name for the table.
   * @return a combined drop table if exists cascade and rename table statement.
   */
  String swapTable(Table table, String newName);

  /**
   * @param table the {@link Table}.
   * @param parentName the parent table name.
   * @return a table inherit statement.
   */
  String setParentTable(Table table, String parentName);

  /**
   * @param table the {@link Table}.
   * @param parentName the parent table name.
   * @return a table no inherit statement.
   */
  String removeParentTable(Table table, String parentName);

  /**
   * @param table the {@link Table}.
   * @param parentName the name of the current parent table.
   * @param newParentName the name of the new parent table.
   * @return a combined table inherit and table no inherit statement.
   */
  String swapParentTable(Table table, String parentName, String newParentName);

  /**
   * @param name the table name.
   * @return a statement which will return a single row with a single column with the table name if
   *     the table exists.
   */
  String tableExists(String name);

  /**
   * @param index the {@link Index}.
   * @return a create index statement.
   */
  String createIndex(Index index);
}
