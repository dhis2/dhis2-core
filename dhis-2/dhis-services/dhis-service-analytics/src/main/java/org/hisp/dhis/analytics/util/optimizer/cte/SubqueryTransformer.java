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
package org.hisp.dhis.analytics.util.optimizer.cte;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

@UtilityClass
public class SubqueryTransformer {

  /**
   * Transforms a given SubSelect query by:
   *
   * <ul>
   *   <li>Removing the enrollment join condition between dynamicTableName.enrollment and
   *       subax.enrollment.
   *   <li>Adding the 'enrollment' column to the SELECT clause.
   *   <li>Renaming the count function alias to "fcxk_count".
   *   <li>Adding a GROUP BY clause on 'enrollment'.
   * </ul>
   *
   * @param subSelect The subselect to transform.
   * @param columnAlias The alias for the count function.
   * @return The transformed SubSelect.
   * @throws IllegalArgumentException if the select body is not a PlainSelect.
   */
  public static String dataElementCountCte(SubSelect subSelect, String columnAlias) {
    if (!(subSelect.getSelectBody() instanceof PlainSelect plainSelect)) {
      throw new IllegalArgumentException("Only PlainSelect is supported in this transformation.");
    }

    // Dynamically extract the table name from the FROM clause.
    String dynamicTableName = null;
    if (plainSelect.getFromItem() instanceof Table table) {
      dynamicTableName = table.getName();
    }

    // Remove the enrollment join condition comparing dynamicTableName.enrollment and
    // subax.enrollment.
    Expression modifiedWhere = removeEnrollmentCondition(plainSelect.getWhere(), dynamicTableName);
    plainSelect.setWhere(modifiedWhere);

    // Add the 'enrollment' column to the SELECT list.
    SelectExpressionItem enrollmentItem = new SelectExpressionItem();
    enrollmentItem.setExpression(new Column("enrollment"));
    List<SelectItem> selectItems = new ArrayList<>(plainSelect.getSelectItems());
    selectItems.add(0, enrollmentItem); // Prepend enrollment to the select items.
    plainSelect.setSelectItems(selectItems);

    // Rename the count function alias to "de_count".
    for (SelectItem item : selectItems) {
      if (item instanceof SelectExpressionItem sei
          && sei.getExpression() instanceof Function func
          && "count".equalsIgnoreCase(func.getName())) {
        sei.setAlias(new Alias(columnAlias));
      }
    }

    // Add a GROUP BY clause on 'enrollment'.
    GroupByElement groupBy = new GroupByElement();
    List<Expression> groupByExpressions = new ArrayList<>();
    groupByExpressions.add(new Column("enrollment"));
    groupBy.setGroupByExpressions(groupByExpressions);
    plainSelect.setGroupByElement(groupBy);

    // Convert the subselect to SQL string.
    String sql = subSelect.toString().trim();

    // Remove the outer parentheses if they exist.
    if (sql.startsWith("(") && sql.endsWith(")")) {
      sql = sql.substring(1, sql.length() - 1).trim();
    }
    return sql;
  }

  /**
   * Generates the new CTE SQL for the "last scheduled" data based on the event table name.
   *
   * @param table the event table name.
   * @return the CTE SQL snippet.
   */
  public static String lastScheduledCte(String table) {
    return """
           select enrollment,
                  scheduleddate
           from (
             select enrollment,
                    scheduleddate,
                    row_number() over (partition by enrollment order by occurreddate desc) as rn
             from %s
             where scheduleddate is not null
           ) t
           where rn = 1
           """
        .formatted(table);
  }

  public static String lastCreatedCte(String table) {
    return """
           select enrollment,
                  created
           from (
             select enrollment,
                    created,
                    row_number() over (partition by enrollment order by occurreddate desc) as rn
             from %s
             where created is not null
           ) t
           where rn = 1
           """
        .formatted(table);
  }

  public static String relationshipCountCte(boolean isAggregated, String relationshipTypeUid) {
    if (isAggregated) {
      return """
             select trackedentityid,
                    sum(relationship_count) as relationship_count
             from analytics_rs_relationship
             group by trackedentityid
             """;
    } else {
      String whereClause =
          relationshipTypeUid != null
              ? "where relationshiptypeuid = '%s'".formatted(relationshipTypeUid)
              : "";
      return """
             select trackedentityid,
                    relationship_count
             from analytics_rs_relationship
             %s
             """
          .formatted(whereClause);
    }
  }

  public static String lastEventValueCte(String table, String columnName) {
    return """
           select enrollment,
                  %s
           from (
             select enrollment,
                    %s,
                    row_number() over (partition by enrollment order by occurreddate desc) as rn
             from %s
             where %s is not null
           ) t
           where rn = 1
           """
        .formatted(columnName, columnName, table, columnName);
  }

  /**
   * Recursively removes the condition that compares dynamicTableName.enrollment with
   * subax.enrollment. Supports both orders (i.e., dynamicTableName.enrollment = subax.enrollment or
   * vice versa).
   *
   * @param expr The expression to process.
   * @param dynamicTableName The dynamic table name.
   * @return The modified expression with the enrollment join condition removed, or null if the
   *     condition was isolated.
   */
  private static Expression removeEnrollmentCondition(Expression expr, String dynamicTableName) {
    if (expr == null) {
      return null;
    }
    if (expr instanceof AndExpression andExpr) {
      Expression left = removeEnrollmentCondition(andExpr.getLeftExpression(), dynamicTableName);
      Expression right = removeEnrollmentCondition(andExpr.getRightExpression(), dynamicTableName);

      if (left == null) {
        return right;
      }
      if (right == null) {
        return left;
      }
      return new AndExpression(left, right);
    } else if (expr instanceof EqualsTo equalsExpr) {
      if (equalsExpr.getLeftExpression() instanceof Column leftCol
          && equalsExpr.getRightExpression() instanceof Column rightCol) {

        // Check for both orders.
        if ((isColumnMatch(leftCol, dynamicTableName, "enrollment")
                && isColumnMatch(rightCol, "subax", "enrollment"))
            || (isColumnMatch(leftCol, "subax", "enrollment")
                && isColumnMatch(rightCol, dynamicTableName, "enrollment"))) {
          return null; // Remove this condition.
        }
      }
    }
    // If no match, return the expression unchanged.
    return expr;
  }

  /**
   * Utility method to verify that a given column matches the expected table and column name.
   *
   * @param column The column to check.
   * @param tableName The expected table name.
   * @param columnName The expected column name.
   * @return true if the column matches; false otherwise.
   */
  private static boolean isColumnMatch(Column column, String tableName, String columnName) {
    if (column == null || column.getColumnName() == null) {
      return false;
    }
    String colName = column.getColumnName().replace("\"", "");
    if (!colName.equalsIgnoreCase(columnName)) {
      return false;
    }
    if (column.getTable() != null && column.getTable().getName() != null) {
      return column.getTable().getName().equalsIgnoreCase(tableName);
    }
    return false;
  }
}
