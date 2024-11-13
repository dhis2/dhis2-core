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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.IndexFunction;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Table;

/**
 * Abstract SQL builder class.
 *
 * @author Lars Helge Overland
 */
public abstract class AbstractSqlBuilder implements SqlBuilder {

  // Constants

  protected static final String SINGLE_QUOTE = "'";
  protected static final String BACKSLASH = "\\";
  protected static final String COMMA = ", ";
  protected static final String DOT = ".";
  protected static final String EMPTY = "";
  protected static final String ALIAS_AX = "ax";
  protected static final String SCHEMA = "public";

  // Utilities

  @Override
  public String quote(String alias, String relation) {
    return alias + DOT + quote(relation);
  }

  @Override
  public String quoteAx(String relation) {
    return ALIAS_AX + DOT + quote(relation);
  }

  @Override
  public String singleQuote(String value) {
    return SINGLE_QUOTE + escape(value) + SINGLE_QUOTE;
  }

  @Override
  public String singleQuotedCommaDelimited(Collection<String> items) {
    return isEmpty(items)
        ? EMPTY
        : items.stream().map(this::singleQuote).collect(Collectors.joining(COMMA));
  }

  // Statements

  @Override
  public String analyzeTable(Table table) {
    return analyzeTable(table.getName());
  }

  @Override
  public String dropTableIfExists(Table table) {
    return dropTableIfExists(table.getName());
  }

  @Override
  public String swapTable(Table table, String newName) {
    return String.join(" ", dropTableIfExistsCascade(newName), renameTable(table, newName));
  }

  @Override
  public String swapParentTable(Table table, String parentName, String newParentName) {
    return String.join(
        " ", removeParentTable(table, parentName), setParentTable(table, newParentName));
  }

  @Override
  public String tableExists(Table table) {
    return tableExists(table.getName());
  }

  @Override
  public String countRows(Table table) {
    return String.format("select count(*) as row_count from %s;", quote(table.getName()));
  }

  // Table

  @Override
  public String analyzeTable(String name) {
    return notSupported();
  }

  @Override
  public String vacuumTable(Table table) {
    return notSupported();
  }

  @Override
  public String setParentTable(Table table, String parentName) {
    return notSupported();
  }

  @Override
  public String removeParentTable(Table table, String parentName) {
    return notSupported();
  }

  // Mapping

  /**
   * Returns the database name of the given data type.
   *
   * @param dataType the {@link DataType}.
   * @return the database name of the given data type.
   */
  protected String getDataTypeName(DataType dataType) {
    return switch (dataType) {
      case SMALLINT -> dataTypeSmallInt();
      case INTEGER -> dataTypeInteger();
      case BIGINT -> dataTypeBigInt();
      case DECIMAL -> dataTypeDecimal();
      case FLOAT -> dataTypeFloat();
      case DOUBLE -> dataTypeDouble();
      case BOOLEAN -> dataTypeBoolean();
      case CHARACTER_11 -> dataTypeCharacter(11);
      case CHARACTER_32 -> dataTypeCharacter(32);
      case VARCHAR_50 -> dataTypeVarchar(50);
      case VARCHAR_255 -> dataTypeVarchar(255);
      case TEXT -> dataTypeText();
      case DATE -> dataTypeDate();
      case TIMESTAMP -> dataTypeTimestamp();
      case TIMESTAMPTZ -> dataTypeTimestampTz();
      case GEOMETRY -> dataTypeGeometry();
      case GEOMETRY_POINT -> dataTypeGeometryPoint();
      case JSONB -> dataTypeJson();
      default ->
          throw new UnsupportedOperationException(
              String.format("Unsuported data type: %s", dataType));
    };
  }

  /**
   * Returns the database name of the given index function.
   *
   * @param indexFunction the {@link IndexFunction}.
   * @return the database name of the given index function.
   */
  protected String getIndexFunctionName(IndexFunction indexFunction) {
    return switch (indexFunction) {
      case UPPER -> indexFunctionUpper();
      case LOWER -> indexFunctionLower();
      default ->
          throw new UnsupportedOperationException(
              String.format("Unsuported index function: %s", indexFunction));
    };
  }

  /**
   * Returns the database name of the given index type.
   *
   * @param indexType the {@link IndexType}.
   * @return the database name of the given index type.
   */
  protected String getIndexTypeName(IndexType indexType) {
    return switch (indexType) {
      case BTREE -> indexTypeBtree();
      case GIST -> indexTypeGist();
      case GIN -> indexTypeGin();
      default ->
          throw new UnsupportedOperationException(
              String.format("Unsuported index type: %s", indexType));
    };
  }

  // Supportive

  /**
   * Replaces variables in the given template string with the given variable values.
   *
   * @param template the template string.
   * @param variables the map of variables and values.
   * @return a resolved string.
   */
  protected String replace(String template, Map<String, String> variables) {
    return new StringSubstitutor(variables).replace(template);
  }

  /**
   * Returns a quoted column string. If the index has a function, the quoted column is wrapped in
   * the function call.
   *
   * @param index the {@link Index}.
   * @param column the column name.
   * @return an index column string.
   */
  protected String toIndexColumn(Index index, String column) {
    String functionName = index.hasFunction() ? getIndexFunctionName(index.getFunction()) : null;
    String indexColumn = quote(column);
    return index.hasFunction() ? String.format("%s(%s)", functionName, indexColumn) : indexColumn;
  }

  /**
   * Indicates that the feature or syntax is not supported by throwing an {@link
   * UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException
   */
  protected String notSupported() {
    throw new UnsupportedOperationException();
  }
}
