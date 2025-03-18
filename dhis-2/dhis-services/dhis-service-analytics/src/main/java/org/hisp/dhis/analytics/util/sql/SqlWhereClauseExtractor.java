/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

/**
 * A utility class for extracting column names from the WHERE clause of a SQL query. This class uses
 * the JSqlParser library to parse SQL statements and recursively traverses the WHERE clause to
 * identify and extract column names.
 *
 * <p><b>Supported SQL Features:</b>
 *
 * <ul>
 *   <li>Simple equality conditions: <code>column1 = 'value'</code>
 *   <li>Multiple conditions with <code>AND</code> and <code>OR</code>: <code>
 *       column1 = 'value' AND column2 = 10</code>
 *   <li><code>IN</code> conditions: <code>column1 IN (1, 2, 3)</code>
 *   <li>Nested parentheses: <code>(column1 = 'value' AND (column2 = 10))</code>
 *   <li><code>LIKE</code> operator: <code>column1 LIKE '%test%'</code>
 *   <li><code>BETWEEN</code> operator: <code>column1 BETWEEN 1 AND 10</code>
 *   <li><code>IS NULL</code> conditions: <code>column1 IS NULL</code>
 *   <li>Function calls: <code>UPPER(column1) = 'TEST'</code>
 *   <li>Subqueries in <code>IN</code> conditions: <code>column1 IN (SELECT id FROM other_table)
 *       </code>
 *   <li>Special characters in column names: <code>"Special!@#$%^&*()" = 'value'</code>
 *   <li>Case-sensitive column names: <code>COLUMN1 = 'value'</code> and <code>column1 = 'value'
 *       </code> are treated as distinct
 *   <li>Duplicate column references: Duplicates are removed from the result
 *   <li>Complex mixed conditions: <code>UPPER(column1) LIKE '%TEST%' AND (column2 BETWEEN 1 AND 10)
 *       </code>
 *   <li>Nested function calls: <code>UPPER(TRIM(column1)) = 'TEST'</code>
 *   <li>Functions with multiple parameters: <code>CONCAT(column1, column2) = 'TEST'</code>
 *   <li>Column references in <code>BETWEEN</code>: <code>column1 BETWEEN column2 AND column3</code>
 * </ul>
 */
@UtilityClass
public class SqlWhereClauseExtractor {

  /**
   * Extracts the column names used in the WHERE clause of the provided SQL query.
   *
   * <p><b>Behavior:</b>
   *
   * <ul>
   *   <li>Returns an empty list if the SQL query does not contain a WHERE clause.
   *   <li>Throws <code>IllegalArgumentException</code> if the SQL query is null, empty, or invalid.
   *   <li>Handles nested parentheses, function calls, and complex conditions.
   *   <li>Removes duplicate column names from the result.
   *   <li>Preserves case sensitivity in column names.
   * </ul>
   *
   * @param sql The SQL query string from which to extract column names.
   * @return A list of column names found in the WHERE clause. The list is empty if no WHERE clause
   *     is present or if no columns are found.
   * @throws RuntimeException If the SQL query is null, empty, or cannot be parsed.
   */
  public static List<String> extractWhereColumns(String sql) {
    List<String> columns = new ArrayList<>();
    try {
      // Parse the SQL string
      Statement statement = CCJSqlParserUtil.parse(sql);
      if (statement instanceof Select) {
        PlainSelect plainSelect = (PlainSelect) ((Select) statement).getSelectBody();

        // Get the WHERE clause
        Expression whereExpression = plainSelect.getWhere();

        // Extract columns from the WHERE clause
        if (whereExpression != null) {
          Set<String> columnSet = new HashSet<>();
          extractColumnsFromExpression(whereExpression, columnSet);
          columns.addAll(columnSet);
        }
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Error parsing SQL: " + e.getMessage(), e);
    }
    return columns;
  }

  /**
   * Recursively extracts column names from a given SQL expression and adds them to the provided
   * set. This method handles various types of expressions, including:
   *
   * <ul>
   *   <li>Simple column references: <code>column1 = 'value'</code>
   *   <li>Binary expressions: <code>column1 = column2</code>
   *   <li><code>IN</code> conditions: <code>column1 IN (1, 2, 3)</code>
   *   <li>Parentheses: <code>(column1 = 'value')</code>
   *   <li><code>IS NULL</code> conditions: <code>column1 IS NULL</code>
   *   <li>Function calls: <code>UPPER(column1)</code>
   *   <li><code>BETWEEN</code> conditions: <code>column1 BETWEEN 1 AND 10</code>
   * </ul>
   *
   * @param expression The SQL expression to process.
   * @param columns A set to store the extracted column names.
   */
  private static void extractColumnsFromExpression(Expression expression, Set<String> columns) {
    if (expression instanceof net.sf.jsqlparser.schema.Column column) {
      // Add the column name without table alias to the set
      String columnName = column.getColumnName();
      // Remove surrounding quotes if present and handle escaped quotes
      if (columnName.startsWith("\"") && columnName.endsWith("\"")) {
        columnName =
            columnName
                .substring(1, columnName.length() - 1)
                .replace("\"\"", "\""); // Handle escaped quotes
      }
      columns.add(columnName);
    } else if (expression instanceof BinaryExpression binaryExpression) {
      // Recursively process left and right expressions
      extractColumnsFromExpression(binaryExpression.getLeftExpression(), columns);
      extractColumnsFromExpression(binaryExpression.getRightExpression(), columns);
    } else if (expression instanceof InExpression inExpression) {
      // Process the left expression of an IN clause
      extractColumnsFromExpression(inExpression.getLeftExpression(), columns);
    } else if (expression instanceof Parenthesis parenthesis) {
      // Process the expression inside parentheses
      extractColumnsFromExpression(parenthesis.getExpression(), columns);
    } else if (expression instanceof IsNullExpression isNullExpression) {
      // Process IS NULL expressions
      extractColumnsFromExpression(isNullExpression.getLeftExpression(), columns);
    } else if (expression instanceof Function function) {
      // Process function parameters to extract column names
      ExpressionList parameters = function.getParameters();
      if (parameters != null) {
        for (Expression parameter : parameters.getExpressions()) {
          extractColumnsFromExpression(parameter, columns);
        }
      }
    } else if (expression instanceof Between between) {
      // Process BETWEEN expressions
      extractColumnsFromExpression(between.getLeftExpression(), columns);
      extractColumnsFromExpression(between.getBetweenExpressionStart(), columns);
      extractColumnsFromExpression(between.getBetweenExpressionEnd(), columns);
    }
  }
}
