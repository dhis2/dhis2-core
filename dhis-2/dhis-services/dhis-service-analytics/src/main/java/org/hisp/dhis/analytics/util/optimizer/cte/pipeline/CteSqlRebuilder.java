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
package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.hisp.dhis.analytics.util.optimizer.cte.AppendExtractedCtesHelper;
import org.hisp.dhis.analytics.util.optimizer.cte.data.DecomposedCtes;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

/**
 * The `CteSqlRebuilder` class is responsible for reconstructing the original SQL query after the
 * CTE decomposition process. It takes the original parsed SQL statement and a list of {@link
 * GeneratedCte} objects (produced by the {@link
 * org.hisp.dhis.analytics.util.optimizer.cte.pipeline.CteDecomposer}) and integrates the generated
 * CTEs into the SQL, replacing the original correlated subqueries with JOINs to the new CTEs.
 */
public class CteSqlRebuilder implements SqlOptimizationStep {

  /**
   * Rebuilds the SQL query by incorporating the generated CTEs and replacing the original
   * correlated subqueries with JOINs.
   *
   * @param originalCte The original parsed SQL statement (a Select statement).
   * @param decomposedCtes The {@link DecomposedCtes} object containing the generated CTEs and other
   *     information from the decomposition process.
   * @return The modified {@link Select} statement with the CTEs added and subqueries replaced.
   */
  public Select rebuildSql(Select originalCte, DecomposedCtes decomposedCtes) {
    PlainSelect oldSelect = decomposedCtes.originalSelect();
    // Get the original FROM item
    FromItem fromItem = oldSelect.getFromItem();
    String fromAlias = getFromAlias(fromItem);

    // Build new SELECT items and joins
    List<SelectItem> newSelectItems = buildSelectItems(oldSelect, fromAlias);
    List<Join> joins = buildJoins(decomposedCtes, fromAlias);

    handleGroupBy(oldSelect, fromAlias);

    // Create the new SELECT
    PlainSelect newSelect = new PlainSelect();
    newSelect.setSelectItems(newSelectItems);
    newSelect.setFromItem(fromItem);
    newSelect.setJoins(joins);
    newSelect.setWhere(oldSelect.getWhere());

    // Update all the elements of the old select with the new ones
    oldSelect.setSelectItems(newSelect.getSelectItems());
    oldSelect.setFromItem(newSelect.getFromItem());
    oldSelect.setJoins(newSelect.getJoins());
    oldSelect.setWhere(newSelect.getWhere());

    // Append the extracted CTEs to the original CTE
    AppendExtractedCtesHelper.appendExtractedCtes(originalCte, toMap(decomposedCtes.ctes()));
    return originalCte;
  }

  private List<Join> buildJoins(DecomposedCtes decomposedCtes, String fromAlias) {
    List<Join> joins = new ArrayList<>();
    for (GeneratedCte cte : decomposedCtes.ctes()) {
      addJoinItem(joins, fromAlias, cte);
    }
    return joins;
  }

  private List<SelectItem> buildSelectItems(PlainSelect select, String fromAlias) {
    List<SelectItem> newSelectItems = new ArrayList<>();
    for (SelectItem item : select.getSelectItems()) {
      if (item instanceof SelectExpressionItem sei) {
        Expression expr = sei.getExpression();
        // If it's a column without a table alias, add the table alias
        if (expr instanceof Column col
            && (col.getTable() == null || col.getTable().getName() == null)) {
          col = new Column(new Table(fromAlias), col.getColumnName());
          SelectExpressionItem newSei = new SelectExpressionItem(col);
          // Preserve the alias if it existed
          if (sei.getAlias() != null) {
            newSei.setAlias(sei.getAlias());
          }
          newSelectItems.add(newSei);
        } else {
          // For other expressions, preserve as is
          newSelectItems.add(sei);
        }
      } else {
        // For other types of SelectItems (like AllColumns), preserve as is
        newSelectItems.add(item);
      }
    }
    return newSelectItems;
  }

  /**
   * Converts a list of {@link GeneratedCte} objects to a map, keyed by CTE name. This method uses a
   * LinkedHashMap to preserve the order of insertion.
   *
   * @param ctes the list of generated CTE
   * @return a map with name and GeneratedCte object.
   */
  private Map<String, GeneratedCte> toMap(List<GeneratedCte> ctes) {
    return ctes.stream()
        .collect(
            Collectors.toMap(
                GeneratedCte::name,
                Function.identity(),
                (existing, replacement) -> existing,
                LinkedHashMap::new // Use LinkedHashMap to preserve order
                ));
  }

  /**
   * Retrieves the alias of a FromItem. If the FromItem does not have an alias, and if is an
   * instance of table, returns its name.
   *
   * @param fromItem The FromItem to retrieve the alias from.
   * @return The alias of the FromItem as a String, if present; otherwise, "subax".
   */
  private String getFromAlias(FromItem fromItem) {
    return fromItem.getAlias() != null && fromItem.getAlias().getName() != null
        ? fromItem.getAlias().getName()
        : "subax";
  }

  /**
   * Adds a JOIN clause to the list of joins and a corresponding SELECT item for the given generated
   * CTE. The JOIN is a LEFT JOIN, and the ON condition is an equality comparison between the
   * `enrollment` column of the main table (aliased by `fromAlias`) and the `enrollment` column of
   * the CTE (aliased by the CTE's generated alias).
   *
   * @param joins The list of Join objects to which the new JOIN clause will be added.
   * @param fromAlias The alias of the main table in the original query (usually "subax").
   * @param cte The GeneratedCte object representing the CTE to be joined.
   */
  private void addJoinItem(List<Join> joins, String fromAlias, GeneratedCte cte) {
    // Create LEFT JOIN
    Join join = new Join();
    join.setLeft(true);
    Table joinTable = new Table(cte.name());
    joinTable.setAlias(new Alias(cte.joinAlias()));
    join.setRightItem(joinTable);

    // Set join condition
    EqualsTo joinCond = new EqualsTo();
    joinCond.setLeftExpression(
        new Column(new Table(fromAlias), getLeftExpression(cte, "enrollment")));
    joinCond.setRightExpression(
        new Column(new Table(cte.joinAlias()), getRightExpression(cte, "enrollment")));
    join.addOnExpression(joinCond);
    joins.add(join);
  }

  /**
   * Handles the GROUP BY clause of the original query by adding table aliases to columns where
   * necessary.
   *
   * @param oldSelect the original PlainSelect to modify
   * @param fromAlias the alias to use for columns without a table reference
   */
  private void handleGroupBy(PlainSelect oldSelect, String fromAlias) {
    GroupByElement groupBy = oldSelect.getGroupBy();
    if (groupBy == null || groupBy.getGroupByExpressions() == null) {
      return;
    }

    List<Expression> newGroupByExpressions =
        transformGroupByExpressions(groupBy.getGroupByExpressions(), fromAlias);
    groupBy.setGroupByExpressions(newGroupByExpressions);
  }

  /**
   * Transforms the list of GROUP BY expressions by adding table aliases to column references where
   * needed.
   *
   * @param expressions the original GROUP BY expressions
   * @param fromAlias the alias to use for columns without a table reference
   * @return the transformed list of expressions
   */
  private List<Expression> transformGroupByExpressions(
      List<Expression> expressions, String fromAlias) {
    return expressions.stream()
        .map(expr -> transformGroupByExpression(expr, fromAlias))
        .collect(Collectors.toList());
  }

  /**
   * Transforms a single GROUP BY expression by adding a table alias if it's a column without one.
   *
   * @param expr the expression to transform
   * @param fromAlias the alias to use for columns without a table reference
   * @return the transformed expression
   */
  private Expression transformGroupByExpression(Expression expr, String fromAlias) {
    if (!(expr instanceof Column column)) {
      return expr;
    }

    return needsTableAlias(column)
        ? new Column(new Table(fromAlias), column.getColumnName())
        : column;
  }

  /**
   * Determines if a column needs a table alias added.
   *
   * @param column the column to check
   * @return true if the column needs a table alias, false otherwise
   */
  private boolean needsTableAlias(Column column) {
    return column.getTable() == null || column.getTable().getName() == null;
  }

  private String getLeftExpression(GeneratedCte cte, String defaultExpression) {
    if (cte.joinColumns() != null) {
      return cte.joinColumns().getLeft();
    }
    return defaultExpression;
  }

  private String getRightExpression(GeneratedCte cte, String defaultExpression) {
    if (cte.joinColumns() != null) {
      return cte.joinColumns().getRight();
    }
    return defaultExpression;
  }
}
