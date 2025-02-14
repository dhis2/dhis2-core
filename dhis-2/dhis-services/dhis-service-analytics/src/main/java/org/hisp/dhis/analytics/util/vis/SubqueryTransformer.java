package org.hisp.dhis.analytics.util.vis;

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

import java.util.ArrayList;
import java.util.List;
public class SubqueryTransformer {

    /**
     * Transforms a given SubSelect query by:
     * <ul>
     *   <li>Removing the enrollment join condition between dynamicTableName.enrollment and subax.enrollment.</li>
     *   <li>Adding the 'enrollment' column to the SELECT clause.</li>
     *   <li>Renaming the count function alias to "fcxk_count".</li>
     *   <li>Adding a GROUP BY clause on 'enrollment'.</li>
     * </ul>
     *
     * @param subSelect   The subselect to transform.
     * @param columnAlias
     * @return The transformed SubSelect.
     * @throws IllegalArgumentException if the select body is not a PlainSelect.
     */
    public static String transformSubSelect(SubSelect subSelect, String columnAlias)  {
        if (!(subSelect.getSelectBody() instanceof PlainSelect plainSelect)) {
            throw new IllegalArgumentException("Only PlainSelect is supported in this transformation.");
        }

        // Dynamically extract the table name from the FROM clause.
        String dynamicTableName = null;
        if (plainSelect.getFromItem() instanceof Table) {
            Table table = (Table) plainSelect.getFromItem();
            dynamicTableName = table.getName();
        }

        // Remove the enrollment join condition comparing dynamicTableName.enrollment and subax.enrollment.
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
            if (item instanceof SelectExpressionItem sei) {
                if (sei.getExpression() instanceof Function func) {
                    if ("count".equalsIgnoreCase(func.getName())) {
                        sei.setAlias(new Alias(columnAlias));
                    }
                }
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
     * Recursively removes the condition that compares dynamicTableName.enrollment with subax.enrollment.
     * Supports both orders (i.e., dynamicTableName.enrollment = subax.enrollment or vice versa).
     *
     * @param expr             The expression to process.
     * @param dynamicTableName The dynamic table name.
     * @return The modified expression with the enrollment join condition removed, or null if the condition was isolated.
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
                if ((isColumnMatch(leftCol, dynamicTableName, "enrollment") &&
                        isColumnMatch(rightCol, "subax", "enrollment")) ||
                        (isColumnMatch(leftCol, "subax", "enrollment") &&
                                isColumnMatch(rightCol, dynamicTableName, "enrollment"))) {
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
     * @param column    The column to check.
     * @param tableName The expected table name.
     * @param columnName The expected column name.
     * @return true if the column matches; false otherwise.
     */
    private static boolean isColumnMatch(Column column, String tableName, String columnName) {
        if (column == null || column.getColumnName() == null) {
            return false;
        }
        String colName = column.getColumnName().replaceAll("\"", "");
        if (!colName.equalsIgnoreCase(columnName)) {
            return false;
        }
        if (column.getTable() != null && column.getTable().getName() != null) {
            return column.getTable().getName().equalsIgnoreCase(tableName);
        }
        return false;
    }
}
