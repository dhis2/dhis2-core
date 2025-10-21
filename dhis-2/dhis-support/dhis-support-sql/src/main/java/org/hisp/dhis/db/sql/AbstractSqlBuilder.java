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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.common.RegexUtils;
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
  protected static final Pattern IS_SINGLE_QUOTED = Pattern.compile("^'.*'$");

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

  @Override
  public String safeConcat(String... columns) {
    return "concat(" + String.join(", ", columns) + ")";
  }

  @Override
  public String concat(List<String> columns) {
    return "concat(" + String.join(", ", columns) + ")";
  }

  @Override
  public String trim(String expression) {
    return "trim(" + expression + ")";
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
  public String dropTableIfExists(String name) {
    return String.format("drop table if exists %s;", quote(name));
  }

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

  @Override
  public String insertIntoSelectFrom(Table intoTable, String fromTable) {
    String columns = toCommaSeparated(intoTable.getColumns(), col -> quote(col.getName()));

    StringBuilder sql =
        new StringBuilder().append("insert into ").append(quote(intoTable.getName())).append(" ");

    if (intoTable.hasColumns()) {
      sql.append("(").append(columns).append(") ");
    }

    sql.append("select ");

    if (intoTable.hasColumns()) {
      sql.append(columns).append(" ");
    }

    return sql.append("from ").append(fromTable).append(";").toString();
  }

  @Override
  public String getDatabaseName() {
    return EMPTY;
  }

  @Override
  public String aggrDecimal(String aggregateExpr, int precision, int scale) {
    ParsedAgg a = parseAggregateCall(aggregateExpr);

    // COUNT variants should not be decimalized
    if ("count".equalsIgnoreCase(a.name())) {
      return aggregateExpr; // unchanged
    }

    String inner = a.inner().trim();
    String maybeDistinct = a.distinct() ? "DISTINCT " : "";

    // Decimalize inner argument for numeric aggregates
    String decimalizedInner = castDecimal(inner, precision, scale);

    return String.format("%s(%s%s)", a.name(), maybeDistinct, decimalizedInner);
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
              String.format("Unsupported data type: %s", dataType));
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
              String.format("Unsupported index function: %s", indexFunction));
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
              String.format("Unsupported index type: %s", indexType));
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
   * @throws UnsupportedOperationException if the feature or syntax is not supported.
   */
  protected String notSupported() {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts the given collection to a comma-separated string, using the given mapping function to
   * convert each item in the collection to a string.
   *
   * @param <T> the type of the collection.
   * @param collection the {@link Collection}.
   * @param mapper the string mapping {@link Function}.
   * @return a comma-separated string.
   */
  protected <T> String toCommaSeparated(Collection<T> collection, Function<T, String> mapper) {
    return collection.stream().map(mapper).collect(Collectors.joining(","));
  }

  /**
   * Checks if the given input is single quoted.
   *
   * @param input the input string.
   * @return true if the input is quoted, false otherwise.
   */
  protected static boolean isSingleQuoted(String input) {
    return RegexUtils.matches(IS_SINGLE_QUOTED, input);
  }

  protected record ParsedAgg(String name, boolean distinct, String inner) {}

  /**
   * Parses an SQL aggregate function call of the form:
   *
   * <pre>
   *   FUNC(expression)
   *   FUNC(DISTINCT expression)
   * </pre>
   *
   * where {@code FUNC} is an aggregate function such as {@code AVG}, {@code SUM}, {@code MIN},
   * {@code MAX}, {@code COUNT}, {@code STDDEV}, or {@code VARIANCE}.
   *
   * <p>The parser extracts three logical components:
   *
   * <ul>
   *   <li><b>name</b> — the function name (upper-cased, e.g. {@code "AVG"})
   *   <li><b>distinct</b> — whether the call contains the {@code DISTINCT} keyword
   *       (case-insensitive)
   *   <li><b>inner</b> — the expression inside the parentheses after removing {@code DISTINCT},
   *       e.g. {@code "x + y"}
   * </ul>
   *
   * <p>This method assumes the input is a well-formed aggregate expression emitted by the SQL
   * builder, but provides a lenient fallback: if the input does not contain parentheses (i.e. is
   * not a function call), it treats the entire string as the inner expression and defaults the
   * function name to {@code AVG}. This allows callers such as {@link #aggrDecimal(String, int,
   * int)} to work even when provided with a raw expression instead of an aggregate.
   *
   * <p><b>Examples:</b>
   *
   * <pre>
   * parseAggregateCall("AVG(value)")
   *   → name="AVG", distinct=false, inner="value"
   *
   * parseAggregateCall("sum(DISTINCT price)")
   *   → name="SUM", distinct=true, inner="price"
   *
   * parseAggregateCall("value")   // not an aggregate
   *   → name="AVG", distinct=false, inner="value"
   * </pre>
   *
   * @param expr a SQL aggregate function call or a raw expression
   * @return a {@code ParsedAgg} object containing the function name, distinct flag, and inner
   *     expression
   */
  protected ParsedAgg parseAggregateCall(String expr) {
    String s = expr.trim();
    int open = s.indexOf('(');
    int close = s.lastIndexOf(')');
    if (open < 0 || close < open) {
      // The expression isn't an aggregate function like AVG(x) or SUM(x)
      // Fall back to treating it as a plain expression, using AVG as the default aggregate
      return new ParsedAgg("avg", false, s);
    }

    String name = s.substring(0, open).trim().toUpperCase();
    String inner = s.substring(open + 1, close).trim();

    boolean distinct = false;
    if (inner.regionMatches(true, 0, "distinct", 0, "distinct".length())) {
      distinct = true;
      inner = inner.substring("DISTINCT".length()).trim();
    }
    return new ParsedAgg(name, distinct, inner);
  }
}
