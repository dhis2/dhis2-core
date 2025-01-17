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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;

@UtilityClass
public class SqlAliasReplacer {

  public static String replaceTableAliases(String whereClause, List<String> columns) {
    if (whereClause == null || columns == null) {
      throw new IllegalArgumentException("Where clause and columns list cannot be null");
    }

    if (whereClause.isEmpty() || columns.isEmpty()) {
      return whereClause;
    }

    try {
      Expression expr = CCJSqlParserUtil.parseCondExpression(whereClause);
      ColumnReplacementVisitor visitor = new ColumnReplacementVisitor(columns);
      expr.accept(visitor);
      return expr.toString();
    } catch (Exception e) {
      throw new RuntimeException("Error parsing SQL where clause: " + e.getMessage(), e);
    }
  }

  private static class ColumnReplacementVisitor extends ExpressionVisitorAdapter {
    private final Set<String> columns;
    private static final Table PLACEHOLDER_TABLE = new Table("%s");
    private boolean inSubQuery = false;

    public ColumnReplacementVisitor(List<String> columns) {
      this.columns =
          columns.stream()
              .map(String::toLowerCase)
              .map(this::stripQuotes)
              .collect(Collectors.toSet());
    }

    @Override
    public void visit(Column column) {
      String columnName = column.getColumnName();
      String rawColumnName = stripQuotes(columnName);

      if (columns.contains(rawColumnName.toLowerCase())) {
        String quoteType = getQuoteType(columnName);
        if (!quoteType.isEmpty()) {
          column.setColumnName(quoteType + rawColumnName + quoteType);
        }
        if (!inSubQuery) {
          column.setTable(PLACEHOLDER_TABLE);
        } else {
          column.setTable(null);
        }
      }
    }

    @Override
    public void visit(SubSelect subSelect) {
      boolean wasInSubQuery = inSubQuery;
      inSubQuery = true;

      if (subSelect.getSelectBody() instanceof PlainSelect) {
        PlainSelect plainSelect = (PlainSelect) subSelect.getSelectBody();
        plainSelect
            .getSelectItems()
            .forEach(
                selectItem -> {
                  if (selectItem instanceof SelectExpressionItem) {
                    SelectExpressionItem sei = (SelectExpressionItem) selectItem;
                    Expression expression = sei.getExpression();
                    if (expression instanceof Function) {
                      Function function = (Function) expression;
                      function.accept(this);
                    }
                  }
                });
      }

      inSubQuery = wasInSubQuery;
    }

    private String stripQuotes(String identifier) {
      if (identifier == null) return "";

      if ((identifier.startsWith("\"") && identifier.endsWith("\""))
          || (identifier.startsWith("`") && identifier.endsWith("`"))
          || (identifier.startsWith("'") && identifier.endsWith("'"))) {
        return identifier.substring(1, identifier.length() - 1);
      }
      return identifier;
    }

    private String getQuoteType(String identifier) {
      if (identifier == null) return "";

      if (identifier.startsWith("\"")) return "\"";
      if (identifier.startsWith("`")) return "`";
      if (identifier.startsWith("'")) return "'";
      return "";
    }
  }
}
