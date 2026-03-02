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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.hisp.dhis.audit.DmlEvent.DmlOperation;

/**
 * Stateless utility for parsing DML SQL statements and extracting table name, operation type, and
 * parameter-to-column position mappings for PK extraction.
 */
@Slf4j
public class DmlSqlParser {

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
  }

  /**
   * Fast pre-check: returns true if the query is possibly a DML statement (INSERT/UPDATE/DELETE).
   * Strips any leading SQL comment (MDC context). Runs in under 5 microseconds.
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

    if (i >= len) {
      return false;
    }

    char first = Character.toUpperCase(query.charAt(i));
    return first == 'I' || first == 'U' || first == 'D';
  }

  /**
   * Parses a DML SQL statement and extracts operation, table name, and column-to-parameter
   * mappings. Returns empty for non-DML or unparseable statements.
   */
  public static Optional<DmlParseResult> parse(String query) {
    if (query == null || query.isEmpty()) {
      return Optional.empty();
    }

    // Strip leading MDC comment for parsing
    String sqlToParse = stripLeadingComment(query);

    try {
      Statement stmt = CCJSqlParserUtil.parse(sqlToParse);

      if (stmt instanceof Insert insert) {
        return parseInsert(insert);
      } else if (stmt instanceof Update update) {
        return parseUpdate(update);
      } else if (stmt instanceof Delete delete) {
        return parseDelete(delete);
      }

      return Optional.empty();
    } catch (Exception e) {
      log.trace("Failed to parse SQL: {}", query, e);
      return Optional.empty();
    }
  }

  private static Optional<DmlParseResult> parseInsert(Insert insert) {
    String tableName = extractTableName(insert.getTable());
    Map<String, Integer> columnToParam = new HashMap<>();

    List<Column> columns = insert.getColumns();
    if (columns != null && insert.getSelect() != null) {
      // For INSERT INTO t (c1, c2) VALUES (?, ?)
      // Map column index to 1-based param position
      for (int i = 0; i < columns.size(); i++) {
        columnToParam.put(columns.get(i).getColumnName().toLowerCase(), i + 1);
      }
    }

    return Optional.of(
        DmlParseResult.builder()
            .operation(DmlOperation.INSERT)
            .tableName(tableName)
            .columnToParamIndex(columnToParam)
            .build());
  }

  private static Optional<DmlParseResult> parseUpdate(Update update) {
    String tableName = extractTableName(update.getTable());

    // Count SET clause params first to offset WHERE params correctly
    int setParamCount = countSetParams(update);
    Map<String, Integer> columnToParam =
        extractWhereColumnParams(update.getWhere(), setParamCount);

    return Optional.of(
        DmlParseResult.builder()
            .operation(DmlOperation.UPDATE)
            .tableName(tableName)
            .columnToParamIndex(columnToParam)
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

  /**
   * Extracts column-to-parameter-index mappings from WHERE clause. Only handles simple {@code
   * column = ?} predicates joined by AND. Parameter positions are 1-based and offset by any
   * preceding SET clause params.
   */
  private static Map<String, Integer> extractWhereColumnParams(
      Expression where, int paramOffset) {
    Map<String, Integer> result = new HashMap<>();
    if (where == null) {
      return result;
    }
    collectEqualsParams(where, result, new int[] {paramOffset});
    return result;
  }

  /**
   * Recursively walks an expression tree collecting {@code column = ?} bindings. Tracks a running
   * JDBC parameter counter in {@code counter[0]}.
   */
  private static void collectEqualsParams(
      Expression expr, Map<String, Integer> result, int[] counter) {
    if (expr instanceof EqualsTo equalsTo) {
      handleEquals(equalsTo, result, counter);
    } else if (expr instanceof BinaryExpression binExpr) {
      // Handle AND/OR by recursing into both sides
      collectEqualsParams(binExpr.getLeftExpression(), result, counter);
      collectEqualsParams(binExpr.getRightExpression(), result, counter);
    }
  }

  private static void handleEquals(
      EqualsTo equalsTo, Map<String, Integer> result, int[] counter) {
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
