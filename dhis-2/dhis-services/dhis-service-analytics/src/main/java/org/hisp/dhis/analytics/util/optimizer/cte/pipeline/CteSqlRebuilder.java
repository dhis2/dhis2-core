package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The `CteSqlRebuilder` class is responsible for reconstructing the original SQL query
 * after the CTE decomposition process. It takes the original parsed SQL statement
 * and a list of {@link GeneratedCte} objects (produced by the {@link org.hisp.dhis.analytics.util.optimizer.cte.pipeline.CteDecomposer})
 * and integrates the generated CTEs into the SQL, replacing the original correlated
 * subqueries with JOINs to the new CTEs.
 */
public class CteSqlRebuilder implements SqlOptimizationStep {

    /**
     * Rebuilds the SQL query by incorporating the generated CTEs and replacing the
     * original correlated subqueries with JOINs.
     *
     * @param originalCte The original parsed SQL statement (a Select statement).
     * @param decomposedCtes The {@link DecomposedCtes} object containing the generated CTEs and other information
     *                       from the decomposition process.
     * @return The modified {@link Select} statement with the CTEs added and subqueries replaced.
     */
    public Select rebuildSql(Select originalCte, DecomposedCtes decomposedCtes) {
        PlainSelect oldSelect = decomposedCtes.originalSelect();
        // Get the original FROM item
        FromItem fromItem = oldSelect.getFromItem();
        String fromAlias = getFromAlias(fromItem);

        // Build new SELECT items and joins
        List<SelectItem> newSelectItems = buildSelectItems(oldSelect, fromAlias);
        List<Join> joins  = buildJoins(decomposedCtes, fromAlias, newSelectItems);

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

    private List<Join> buildJoins(DecomposedCtes decomposedCtes, String fromAlias, List<SelectItem> newSelectItems) {
        List<Join> joins = new ArrayList<>();
        for (GeneratedCte cte : decomposedCtes.ctes()) {
            addJoinItem(joins, newSelectItems, fromAlias, cte);
        }
        return joins;
    }

    private List<SelectItem> buildSelectItems(PlainSelect select, String fromAlias) {
        List<SelectItem> newSelectItems = new ArrayList<>();
        for (SelectItem item : select.getSelectItems()) {
            if (item instanceof SelectExpressionItem sei) {
                Expression expr = sei.getExpression();
                // If it's a column without a table alias, add the table alias
                if (expr instanceof Column col && (col.getTable() == null || col.getTable().getName() == null)) {
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
     * Converts a list of {@link GeneratedCte} objects to a map, keyed by CTE name.
     * This method uses a LinkedHashMap to preserve the order of insertion.
     * @param ctes the list of generated CTE
     * @return a map with name and GeneratedCte object.
     */
    private Map<String, GeneratedCte> toMap(List<GeneratedCte> ctes) {
        return ctes.stream().collect(Collectors.toMap(
                GeneratedCte::name,
                Function.identity(),
                (existing, replacement) -> existing,
                LinkedHashMap::new // Use LinkedHashMap to preserve order
        ));
    }

    /**
     * Retrieves the alias of a FromItem. If the FromItem does not have
     * an alias, and if is an instance of table, returns its name.
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
     * Adds a JOIN clause to the list of joins and a corresponding SELECT item
     * for the given generated CTE.  The JOIN is a LEFT JOIN, and the ON condition
     * is an equality comparison between the `enrollment` column of the main table
     * (aliased by `fromAlias`) and the `enrollment` column of the CTE (aliased by
     * the CTE's generated alias).
     *
     * @param joins The list of Join objects to which the new JOIN clause will be added.
     * @param selectItems The list of SelectItem objects to which a new select item might be added (currently unused).
     * @param fromAlias The alias of the main table in the original query (usually "subax").
     * @param cte The GeneratedCte object representing the CTE to be joined.
     */
    private void addJoinItem(List<Join> joins, List<SelectItem> selectItems,
                             String fromAlias, GeneratedCte cte) {
        // Create LEFT JOIN
        Join join = new Join();
        join.setLeft(true);
        Table joinTable = new Table(cte.name());
        joinTable.setAlias(new Alias(cte.joinAlias()));
        join.setRightItem(joinTable);

        // Set join condition
        EqualsTo joinCond = new EqualsTo();
        joinCond.setLeftExpression(new Column(new Table(fromAlias), getLeftExpression(cte, "enrollment")));
        joinCond.setRightExpression(new Column(new Table(cte.joinAlias()), getRightExpression(cte, "enrollment")));
        join.addOnExpression(joinCond);
        joins.add(join);
    }

    /**
     * Handles the GROUP BY clause of the original query, adding table aliases
     * to columns if necessary.
     */
    private void handleGroupBy(PlainSelect oldSelect, String fromAlias) {
        GroupByElement groupBy = oldSelect.getGroupBy();
        if (groupBy != null) {
            List<Expression> groupByExpressions = groupBy.getGroupByExpressions();
            if (groupByExpressions != null) {
                List<Expression> newGroupByExpressions = new ArrayList<>();
                for (Expression expr : groupByExpressions) {
                    if (expr instanceof Column col) {
                        if (col.getTable() == null || col.getTable().getName() == null) {
                            col = new Column(new Table(fromAlias), col.getColumnName()); // Add alias
                        }
                        newGroupByExpressions.add(col);
                    } else {
                        newGroupByExpressions.add(expr); // Keep other expressions
                    }
                }
                groupBy.setGroupByExpressions(newGroupByExpressions);
            }
        }
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
