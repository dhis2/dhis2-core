/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

public class SqlWhereClauseExtractor {

  // GIUSEPPE/MAIKEL: can we use a different approach to avoid using jsqlparser?
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
      throw new RuntimeException("Error parsing SQL: " + e.getMessage(), e);
    }
    return columns;
  }

  private static void extractColumnsFromExpression(Expression expression, Set<String> columns) {
    if (expression instanceof net.sf.jsqlparser.schema.Column column) {
      // Add the column name without table alias to the set
      String columnName = column.getColumnName();
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
    }
  }
}
