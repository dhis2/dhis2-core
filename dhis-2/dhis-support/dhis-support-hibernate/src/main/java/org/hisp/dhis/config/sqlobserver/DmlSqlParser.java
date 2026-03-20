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
package org.hisp.dhis.config.sqlobserver;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.statement.values.ValuesStatement;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.audit.DmlEvent.DmlOperation;

/**
 * Utility for parsing DML SQL statements and extracting table name, operation type, and
 * parameter-to-column position mappings for PK extraction.
 */
@Slf4j
public class DmlSqlParser {

  /**
   * Bounded LRU cache for parse results keyed by SQL string. Hibernate typically uses ~200-300
   * distinct SQL templates, so 512 entries provides ample headroom.
   */
  private static final int PARSE_CACHE_MAX_SIZE = 512;

  private static final Cache<String, Optional<DmlParseResult>> PARSE_CACHE =
      new Cache2kBuilder<String, Optional<DmlParseResult>>() {}.entryCapacity(PARSE_CACHE_MAX_SIZE)
          .eternal(true)
          .permitNullValues(true)
          .build();

  private DmlSqlParser() {}

  @Value
  @Builder
  public static class DmlParseResult {
    DmlOperation operation;
    String tableName;

    /**
     * Maps column names in WHERE clause (for UPDATE/DELETE) or INSERT column list to 1-based JDBC
     * parameter position. Only populated for simple {@code column = ?} equality predicates.
     */
    Map<String, Integer> columnToParamIndex;

    /** Column names from the SET clause of an UPDATE statement. Empty for INSERT/DELETE. */
    @Builder.Default Set<String> updatedColumns = Set.of();
  }

  /**
   * Returns {@code true} if the query is possibly a DML statement (INSERT/UPDATE/DELETE). Strips a
   * leading block comment but not line comments.
   */
  public static boolean isPossibleDml(String query) {
    if (query == null || query.isEmpty()) {
      return false;
    }

    int i = 0;
    int len = query.length();

    // Skip leading whitespace
    while (i < len && Character.isWhitespace(query.charAt(i))) {
      i++;
    }

    // Skip leading /* MDC comment */
    if (i + 1 < len && query.charAt(i) == '/' && query.charAt(i + 1) == '*') {
      int endComment = query.indexOf("*/", i + 2);
      if (endComment < 0) {
        return false;
      }
      i = endComment + 2;
      // Skip whitespace after comment
      while (i < len && Character.isWhitespace(query.charAt(i))) {
        i++;
      }
    }

    if (i + 2 >= len) {
      return false;
    }

    // Check first 3 characters to distinguish INSERT/UPDATE/DELETE from DROP/DESCRIBE/IF etc.
    String prefix = query.substring(i, Math.min(i + 3, len)).toUpperCase(java.util.Locale.ROOT);
    return prefix.startsWith("INS") || prefix.startsWith("UPD") || prefix.startsWith("DEL");
  }

  /**
   * Parses a DML SQL statement and extracts operation, table name, and column-to-parameter
   * mappings. Returns empty for non-DML or unparseable statements.
   */
  public static Optional<DmlParseResult> parse(String query) {
    if (query == null || query.isEmpty()) {
      return Optional.empty();
    }

    // Check cache first — Hibernate reuses the same SQL template for prepared statements
    if (PARSE_CACHE.containsKey(query)) {
      return PARSE_CACHE.peek(query);
    }

    // Strip leading MDC comment for parsing
    String sqlToParse = stripLeadingComment(query);

    Optional<DmlParseResult> result;
    try {
      Statement stmt = CCJSqlParserUtil.parse(sqlToParse);

      if (stmt instanceof Insert insert) {
        result = parseInsert(insert);
      } else if (stmt instanceof Update update) {
        result = parseUpdate(update);
      } else if (stmt instanceof Delete delete) {
        result = parseDelete(delete);
      } else {
        result = Optional.empty();
      }
    } catch (Exception e) {
      log.trace("Failed to parse SQL: {}", query, e);
      result = Optional.empty();
    }

    PARSE_CACHE.put(query, result);
    return result;
  }

  private static Optional<DmlParseResult> parseInsert(Insert insert) {
    String tableName = extractTableName(insert.getTable());
    Map<String, Integer> columnToParam = new HashMap<>();

    List<Column> columns = insert.getColumns();
    if (columns != null && insert.getSelect() != null) {
      // Map columns to JDBC param positions. Mixed literal/parameter VALUES means column
      // position may differ from param position.
      List<Expression> valuesExpressions = extractValuesExpressions(insert);
      if (!valuesExpressions.isEmpty() && valuesExpressions.size() == columns.size()) {
        int paramIndex = 0;
        for (int i = 0; i < columns.size(); i++) {
          if (valuesExpressions.get(i) instanceof JdbcParameter) {
            paramIndex++;
            columnToParam.put(columns.get(i).getColumnName().toLowerCase(), paramIndex);
          }
        }
      } else {
        // Fallback: assume all columns are parameterized
        for (int i = 0; i < columns.size(); i++) {
          columnToParam.put(columns.get(i).getColumnName().toLowerCase(), i + 1);
        }
      }
    }

    return Optional.of(
        DmlParseResult.builder()
            .operation(DmlOperation.INSERT)
            .tableName(tableName)
            .columnToParamIndex(columnToParam)
            .build());
  }

  /**
   * Extracts expressions from the first VALUES row of an INSERT. Returns an empty list if the
   * structure cannot be navigated (e.g., INSERT ... SELECT).
   */
  private static List<Expression> extractValuesExpressions(Insert insert) {
    try {
      Select sel = insert.getSelect();
      if (sel == null) return List.of();
      SelectBody body = sel.getSelectBody();
      if (!(body instanceof SetOperationList sol)) return List.of();
      for (SelectBody sb : sol.getSelects()) {
        if (sb instanceof ValuesStatement vs) {
          ItemsList items = vs.getExpressions();
          if (items instanceof ExpressionList el && !el.getExpressions().isEmpty()) {
            Expression first = el.getExpressions().get(0);
            if (first instanceof RowConstructor rc && rc.getExprList() != null) {
              return rc.getExprList().getExpressions();
            }
          }
        }
      }
    } catch (Exception e) {
      log.trace("Could not extract VALUES expressions from INSERT", e);
    }
    return List.of();
  }

  private static Optional<DmlParseResult> parseUpdate(Update update) {
    String tableName = extractTableName(update.getTable());

    // Count SET clause params first to offset WHERE params correctly
    int setParamCount = countSetParams(update);
    Map<String, Integer> columnToParam = extractWhereColumnParams(update.getWhere(), setParamCount);
    Set<String> updatedColumns = extractSetColumns(update);

    return Optional.of(
        DmlParseResult.builder()
            .operation(DmlOperation.UPDATE)
            .tableName(tableName)
            .columnToParamIndex(columnToParam)
            .updatedColumns(updatedColumns)
            .build());
  }

  private static Optional<DmlParseResult> parseDelete(Delete delete) {
    String tableName = extractTableName(delete.getTable());
    Map<String, Integer> columnToParam = extractWhereColumnParams(delete.getWhere(), 0);

    return Optional.of(
        DmlParseResult.builder()
            .operation(DmlOperation.DELETE)
            .tableName(tableName)
            .columnToParamIndex(columnToParam)
            .build());
  }

  /** Extracts the table name, stripping any schema prefix. */
  static String extractTableName(Table table) {
    return table.getName().toLowerCase();
  }

  /**
   * Counts the number of JDBC parameter placeholders in UPDATE SET clauses. Uses {@code
   * getUpdateSets()} to iterate over each SET assignment. This is needed to correctly offset WHERE
   * clause parameter positions.
   */
  private static int countSetParams(Update update) {
    int count = 0;
    List<UpdateSet> updateSets = update.getUpdateSets();
    if (updateSets != null) {
      for (UpdateSet updateSet : updateSets) {
        if (updateSet.getExpressions() != null) {
          for (Expression expr : updateSet.getExpressions()) {
            if (expr instanceof JdbcParameter) {
              count++;
            }
          }
        }
      }
    }
    return count;
  }

  /** Extracts the column names from the SET clause of an UPDATE statement. */
  private static Set<String> extractSetColumns(Update update) {
    Set<String> columns = new LinkedHashSet<>();
    List<UpdateSet> updateSets = update.getUpdateSets();
    if (updateSets != null) {
      for (UpdateSet updateSet : updateSets) {
        if (updateSet.getColumns() != null) {
          for (Column col : updateSet.getColumns()) {
            columns.add(col.getColumnName().toLowerCase());
          }
        }
      }
    }
    return Set.copyOf(columns);
  }

  /**
   * Extracts column-to-parameter-index mappings from WHERE clause. Only handles simple {@code
   * column = ?} predicates joined by AND. Parameter positions are 1-based and offset by any
   * preceding SET clause params.
   */
  private static Map<String, Integer> extractWhereColumnParams(Expression where, int paramOffset) {
    Map<String, Integer> result = new HashMap<>();
    if (where == null) {
      return result;
    }
    collectEqualsParams(where, result, new int[] {paramOffset});
    return result;
  }

  /**
   * Recursively walks the expression tree collecting {@code column = ?} bindings. Tracks a running
   * JDBC parameter counter in {@code counter[0]}. Non-equality comparisons consume parameter slots
   * but are not mapped.
   */
  private static void collectEqualsParams(
      Expression expr, Map<String, Integer> result, int[] counter) {
    if (expr instanceof EqualsTo equalsTo) {
      handleEquals(equalsTo, result, counter);
    } else if (expr instanceof ComparisonOperator comp) {
      // Non-equality comparison — count ? params but don't map columns.
      countJdbcParams(comp.getLeftExpression(), counter);
      countJdbcParams(comp.getRightExpression(), counter);
    } else if (expr instanceof BinaryExpression binExpr) {
      // Handle AND/OR by recursing into both sides
      collectEqualsParams(binExpr.getLeftExpression(), result, counter);
      collectEqualsParams(binExpr.getRightExpression(), result, counter);
    }
  }

  /** Counts JdbcParameter instances in an expression, incrementing the running counter. */
  private static void countJdbcParams(Expression expr, int[] counter) {
    if (expr instanceof JdbcParameter) {
      counter[0]++;
    }
  }

  private static void handleEquals(EqualsTo equalsTo, Map<String, Integer> result, int[] counter) {
    Expression left = equalsTo.getLeftExpression();
    Expression right = equalsTo.getRightExpression();

    if (left instanceof Column col && right instanceof JdbcParameter) {
      counter[0]++;
      result.put(col.getColumnName().toLowerCase(), counter[0]);
    } else if (right instanceof Column col && left instanceof JdbcParameter) {
      counter[0]++;
      result.put(col.getColumnName().toLowerCase(), counter[0]);
    }
  }

  /** Strips a leading SQL block comment (e.g. MDC context) from the query. */
  static String stripLeadingComment(String query) {
    int i = 0;
    int len = query.length();

    while (i < len && Character.isWhitespace(query.charAt(i))) {
      i++;
    }

    if (i + 1 < len && query.charAt(i) == '/' && query.charAt(i + 1) == '*') {
      int endComment = query.indexOf("*/", i + 2);
      if (endComment >= 0) {
        i = endComment + 2;
        while (i < len && Character.isWhitespace(query.charAt(i))) {
          i++;
        }
      }
    }

    return i > 0 ? query.substring(i) : query;
  }
}
