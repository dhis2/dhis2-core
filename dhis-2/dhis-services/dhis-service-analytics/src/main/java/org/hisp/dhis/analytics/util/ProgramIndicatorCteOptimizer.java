package org.hisp.dhis.analytics.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hisp.dhis.analytics.util.vis.AppendExtractedCtesHelper;
import org.hisp.dhis.analytics.util.vis.ExpressionTransformer;
import org.hisp.dhis.analytics.util.vis.FoundSubSelect;
import org.hisp.dhis.analytics.util.vis.GeneratedCte;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimizes SQL queries containing Program Indicator Common Table Expressions (CTEs) by
 * transforming correlated subqueries into more efficient JOIN operations.
 *
 * <p>This optimizer specifically targets CTEs whose names start with "pi_" and contain
 * correlated subqueries in their WHERE clauses. It performs the following optimizations:
 * <ul>
 *   <li>Identifies and extracts correlated subqueries that fetch the most recent scheduled dates
 *       and created dates from event tables</li>
 *   <li>Transforms these subqueries into separate CTEs using window functions for better performance</li>
 *   <li>Replaces the original correlated subqueries with LEFT JOIN operations to these new CTEs</li>
 *   <li>Maintains the original query semantics while improving execution efficiency</li>
 * </ul>
 *
 * <p>For example, it transforms queries of the form:
 * <pre>{@code
 * WITH pi_example AS (
 *   SELECT enrollment,
 *     (SELECT scheduleddate
 *      FROM analytics_event_table
 *      WHERE enrollment = subax.enrollment
 *      AND scheduleddate IS NOT NULL
 *      ORDER BY occurreddate DESC
 *      LIMIT 1) as scheduled_date
 *   FROM analytics_enrollment_table subax
 *   WHERE ...
 * )
 * }</pre>
 *
 * into:
 * <pre>{@code
 * WITH last_sched AS (
 *   SELECT enrollment, scheduleddate
 *   FROM (
 *     SELECT enrollment, scheduleddate,
 *            ROW_NUMBER() OVER (PARTITION BY enrollment ORDER BY occurreddate DESC) AS rn
 *     FROM analytics_event_table
 *     WHERE scheduleddate IS NOT NULL
 *   ) t
 *   WHERE rn = 1
 * ),
 * pi_example AS (
 *   SELECT subax.enrollment, ls.scheduleddate as scheduled_date
 *   FROM analytics_enrollment_table subax
 *   LEFT JOIN last_sched ls ON ls.enrollment = subax.enrollment
 *   WHERE ...
 * )
 * }</pre>
 *
 * <p>The optimization process includes:
 * <ul>
 *   <li>Parsing the input SQL into an Abstract Syntax Tree (AST)</li>
 *   <li>Identifying PI CTEs that contain correlated subqueries</li>
 *   <li>Extracting and transforming the subqueries into efficient CTEs</li>
 *   <li>Rebuilding the original CTEs with appropriate JOINs</li>
 *   <li>Maintaining column aliases and GROUP BY clauses</li>
 *   <li>Preserving the original query semantics</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * ProgramIndicatorCteOptimizer optimizer = new ProgramIndicatorCteOptimizer();
 * String optimizedSql = optimizer.transformSQL(originalSql);
 * }</pre>
 *
 * @see WithItem
 * @see ExpressionTransformer
 * @see GeneratedCte
 * @see FoundSubSelect
 *
 */
@Slf4j
public class ProgramIndicatorCteOptimizer {

    // Collection to hold generated CTE definitions.
    private final Map<String, GeneratedCte> generatedCtes = new LinkedHashMap<>();

    public String transformSQL(String sql) {
        try {
            // Parse the SQL into an AST
            Statement statement = CCJSqlParserUtil.parse(sql);
            // Ignore non-SELECT statements.
            if (statement instanceof Select select) {
                // Process each WITH item that is a PI CTE.
                if (select.getWithItemsList() != null) {
                    for (WithItem withItem : select.getWithItemsList()) {
                        String cteName = withItem.getName();
                        if (cteName != null && cteName.startsWith("pi_") && containsWhereClauseWithSubaxCorrelatedSubSelect(withItem)) {
                            log.debug("Processing PI CTE: {}",  cteName);
                            processCTE(withItem);
                        }
                    }
                }

                // Append the new (generated) CTEs before the existing ones.
                AppendExtractedCtesHelper.appendExtractedCtes(select, this.generatedCtes);

                // Deparse the AST back into a SQL string.
                StringBuilder buffer = new StringBuilder();
                StatementDeParser deparser = new StatementDeParser(buffer);
                select.accept(deparser);
                return buffer.toString();
            } else {
                return sql;
            }
        } catch (Exception e) {
            log.error("Error transforming SQL", e);
            throw new IllegalQueryException(ErrorCode.E7150, e.getMessage(), e);
        }
    }

    /**
     * Checks if a PI CTE has a WHERE clause with a subselect that references the "subax" alias.
     * This is a simple heuristic to determine if the CTE is a PI CTE.
     *
     * @param withItem the WITH item to check.
     * @return true if the CTE contains a subselect that references "subax", false otherwise.
     */
    private boolean containsWhereClauseWithSubaxCorrelatedSubSelect(WithItem withItem) {
        SelectBody body = withItem.getSubSelect().getSelectBody();
        if (body instanceof PlainSelect ps) {
            return ps.getWhere() != null && ps.getWhere().toString().contains("subax");
        }
        return false;
    }

    /**
     * Processes a PI CTE (one whose name starts with "pi_") by detecting a predictable subselect
     * (the one that selects scheduleddate from an event table), then:
     * - Extracts the event table name,
     * - Generates a new CTE SQL (e.g. for "last_scheduled"),
     * - Stores that generated CTE, and
     * - Rebuilds the PI CTE's SELECT body so that it uses a LEFT JOIN to the new CTE.
     * <p>
     * For example, transforms:
     * <p>
     *   WITH pi_hgtnuhsqbml AS (
     *       SELECT enrollment,
     *              (SELECT scheduleddate
     *               FROM analytics_event_ur1edk5oe2n
     *               WHERE enrollment = subax.enrollment
     *               ORDER BY occurreddate DESC
     *               LIMIT 1) AS sched_date
     *       FROM analytics_enrollment_ur1edk5oe2n subax
     *   )
     * <p>
     * into:
     * <p>
     *   WITH last_scheduled AS ( ... ),
     *   pi_hgtnuhsqbml AS (
     *       SELECT subax.enrollment,
     *              ls.scheduleddate AS sched_date
     *       FROM analytics_enrollment_ur1edk5oe2n subax
     *       LEFT JOIN last_scheduled ls ON ls.enrollment = subax.enrollment
     *   )
     *
     * @param withItem the PI CTE to process.
     */
    public void processCTE(WithItem withItem) {
        try {
            SelectBody body = withItem.getSubSelect().getSelectBody();
            if (!(body instanceof PlainSelect oldSelect)) return;

            // Get the WHERE expression
            Expression whereExpr = oldSelect.getWhere();
            if (whereExpr == null) return;

            // Create and apply the transformer
            ExpressionTransformer transformer = new ExpressionTransformer();
            whereExpr.accept(transformer);

            // Get the transformed expression and extracted subselects
            Expression transformedWhere = transformer.getTransformedExpression();
            if (transformedWhere == null) {
                // If transformation failed, keep original
                transformedWhere = whereExpr;
            }

            Map<SubSelect, FoundSubSelect> extractedSubSelects = transformer.getExtractedSubSelects();
            if (!extractedSubSelects.isEmpty()) {
                // Process extracted subselects
                for (Map.Entry<SubSelect, FoundSubSelect> entry : extractedSubSelects.entrySet()) {
                    if (!generatedCtes.containsKey(entry.getValue().name())) {
                        String eventTable = extractTableName(entry.getKey());
                        if (eventTable != null) {
                            FoundSubSelect found = entry.getValue();
                            String name = found.name();

                            if (name.equals("last_sched")) {
                                String newCteSql = lastScheduledCte(eventTable);
                                generatedCtes.put(name,
                                        new GeneratedCte(name, newCteSql, "ls"));
                            } else if (name.equals("last_created")) {
                                String newCteSql = lastCreatedCte(eventTable);
                                generatedCtes.put(name,
                                        new GeneratedCte(name, newCteSql, "lc"));
                            } else if(name.startsWith("relationship_count")) {
                                boolean isAggregated = Boolean.parseBoolean(found.metadata().getOrDefault("isAggregated", "false"));
                                String relationshipTypeUid = found.metadata().get("relationshipTypeUid");
                                String newCteSql = relationshipCountCte(isAggregated, relationshipTypeUid);
                                generatedCtes.put(name,
                                        new GeneratedCte(name, newCteSql, "rlc", ImmutablePair.of("trackedentity", "trackedentityid")));
                            } else if (name.startsWith("last_value_")) {
                                String newCteSql = lastEventValueCte(eventTable, found.columnReference());
                                generatedCtes.put(name,
                                        new GeneratedCte(name, newCteSql, "lv_" + preserveLettersAndNumbers(found.columnReference())));
                            }
                        }
                    }
                }

                // Update the WHERE clause
                oldSelect.setWhere(transformedWhere);

                // Rebuild the CTE body
                rebuildPiCteBody(oldSelect);
            }
        } catch (Exception e) {
            // Log the error but don't throw it
            e.printStackTrace();
        }
    }

    private void rebuildPiCteBody(PlainSelect oldSelect) {
        // Get the original FROM item
        FromItem fromItem = oldSelect.getFromItem();
        String fromAlias = getFromAlias(fromItem);

        // Build new SELECT items and joins
        List<SelectItem> newSelectItems = new ArrayList<>();
        List<Join> joins = new ArrayList<>();

        // Preserve all original SELECT items
        for (SelectItem item : oldSelect.getSelectItems()) {
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

        // Add joins and select items for each generated CTE
        for (GeneratedCte cte : generatedCtes.values()) {
            addJoinItem(joins, newSelectItems, fromAlias, cte);
        }

        // Create the new SELECT
        PlainSelect newSelect = new PlainSelect();
        newSelect.setSelectItems(newSelectItems);
        newSelect.setFromItem(fromItem);
        newSelect.setJoins(joins);
        newSelect.setWhere(oldSelect.getWhere());

        // Handle GROUP BY by adding table alias to columns
        GroupByElement groupBy = oldSelect.getGroupBy();
        if (groupBy != null) {
            List<Expression> groupByExpressions = groupBy.getGroupByExpressions();
            if (groupByExpressions != null) {
                List<Expression> newGroupByExpressions = new ArrayList<>();
                for (Expression expr : groupByExpressions) {
                    if (expr instanceof Column col) {
                        if (col.getTable() == null || col.getTable().getName() == null) {
                            // Add the table alias to the column
                            col = new Column(new Table(fromAlias), col.getColumnName());
                        }
                        newGroupByExpressions.add(col);
                    } else {
                        newGroupByExpressions.add(expr);
                    }
                }
                groupBy.setGroupByExpressions(newGroupByExpressions);
            }
        }

        // Update all the elements of the old select with the new ones
        oldSelect.setSelectItems(newSelect.getSelectItems());
        oldSelect.setFromItem(newSelect.getFromItem());
        oldSelect.setJoins(newSelect.getJoins());
        oldSelect.setWhere(newSelect.getWhere());
    }

    private String getFromAlias(FromItem fromItem) {
        return fromItem.getAlias() != null && fromItem.getAlias().getName() != null
                ? fromItem.getAlias().getName()
                : "subax";
    }

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

    /**
     * Extracts the table name from the subselect's FROM clause.
     *
     * @param subSelect the subselect.
     * @return the table name as a String.
     */
    private String extractTableName(SubSelect subSelect) {
        if (subSelect.getSelectBody() instanceof PlainSelect plain) {
            FromItem fromItem = plain.getFromItem();
            if (fromItem instanceof Table table) {
                return table.getName();
            }
        }
        return null;
    }

    /**
     * Generates the new CTE SQL for the "last scheduled" data based on the event table name.
     *
     * @param table the event table name.
     * @return the CTE SQL snippet.
     */
    private String lastScheduledCte(String table) {
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
               """.formatted(table);
    }

    private String lastCreatedCte(String table) {
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
               """.formatted(table);
    }

    private String relationshipCountCte(boolean isAggregated, String relationshipTypeUid) {
        if (isAggregated) {
            return """
               SELECT trackedentityid,
                      sum(relationship_count) as relationship_count
               FROM analytics_rs_relationship
               GROUP BY trackedentityid
               """;
        } else {
            String whereClause = relationshipTypeUid != null ?
                    "WHERE relationshiptypeuid = '%s'".formatted(relationshipTypeUid) : "";
            return """
               SELECT trackedentityid,
                      relationship_count
               FROM analytics_rs_relationship
               %s
               """.formatted(whereClause);
        }
    }

    private String lastEventValueCte(String table, String columnName) {
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
           WHERE rn = 1
           """.formatted(columnName, columnName, table, columnName);
    }

    private String preserveLettersAndNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}