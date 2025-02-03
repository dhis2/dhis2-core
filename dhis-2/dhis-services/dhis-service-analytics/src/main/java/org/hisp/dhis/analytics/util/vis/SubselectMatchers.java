package org.hisp.dhis.analytics.util.vis;

import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class SubselectMatchers {

    /**
     * Checks if the given subselect matches the expected pattern:
     *   SELECT scheduleddate
     *   FROM <some_table>
     *   WHERE <some_table>.enrollment = subax.enrollment
     *     AND scheduleddate IS NOT NULL
     *   ORDER BY occurreddate DESC
     *   LIMIT 1
     *
     * @param subSelect the subselect to check.
     */
    public static Optional<FoundSubSelect> matchesLastSchedPattern(SubSelect subSelect) {
        if (!(subSelect.getSelectBody() instanceof PlainSelect plain)) {
            return Optional.empty();
        }
        List<SelectItem> selectItems = plain.getSelectItems();
        if (selectItems == null || selectItems.size() != 1) {
            return Optional.empty();
        }
        SelectItem item = selectItems.get(0);
        if (!(item instanceof SelectExpressionItem sei)) {
            return Optional.empty();
        }
        Expression selectExpr = sei.getExpression();
        if (!(selectExpr instanceof Column col)) {
            return Optional.empty();
        }
        if (!"scheduleddate".equalsIgnoreCase(col.getColumnName())) {
            return Optional.empty();
        }
        // Check that the FROM clause is a table.
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table)) {
            return Optional.empty();
        }
        // Check that the WHERE clause contains "subax.enrollment"
        Expression where = plain.getWhere();
        if (where == null) {
            return Optional.empty();
        }
        String whereStr = where.toString().toLowerCase();
        if (!whereStr.contains("subax.enrollment")) {
            return Optional.empty();
        }
        // Check that ORDER BY and LIMIT are present.
        if (plain.getOrderByElements() == null || plain.getOrderByElements().isEmpty()) {
            return Optional.empty();
        }
        if (plain.getLimit() == null) {
            return Optional.empty();
        }
        return Optional.of(new FoundSubSelect("last_sched", subSelect, "scheduleddate"));
    }

    /**
     * Checks if the given subselect (from an EXISTS expression) matches the expected pattern for last_created.
     *
     * Expected pattern:
     *   SELECT created
     *   FROM <table>
     *   WHERE <table>.enrollment = subax.enrollment
     *     AND created IS NOT NULL
     *   ORDER BY occurreddate DESC
     *   LIMIT 1
     *
     * @param subSelect the subselect to check.
     */
    public static Optional<FoundSubSelect> matchesLastCreatedExistsPattern(SubSelect subSelect) {
        if (!(subSelect.getSelectBody() instanceof PlainSelect plain)) {
            return Optional.empty();
        }
        List<SelectItem> selectItems = plain.getSelectItems();
        if (selectItems == null || selectItems.size() != 1){
            return Optional.empty();
        }
        SelectItem item = selectItems.get(0);
        if (!(item instanceof SelectExpressionItem sei)) {
            return Optional.empty();
        }
        Expression selectExpr = sei.getExpression();
        if (!(selectExpr instanceof Column col)) {
            return Optional.empty();
        }
        if (!"created".equalsIgnoreCase(col.getColumnName())) {
            return Optional.empty();
        }
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table)) {
            return Optional.empty();
        }
        Expression where = plain.getWhere();
        if (where == null) {
            return Optional.empty();
        }
        String whereStr = where.toString().toLowerCase();
        if (!(whereStr.contains("subax.enrollment") && whereStr.contains("created") && whereStr.contains("is not null"))) {
            return Optional.empty();
        }
        if (plain.getOrderByElements() == null || plain.getOrderByElements().isEmpty()) {
            return Optional.empty();
        }
        if (plain.getLimit() == null) {
            return Optional.empty();
        }
        return Optional.of(new FoundSubSelect("last_created", subSelect, "created"));
    }

    /**
     * Checks if the given subselect matches the pattern for selecting the last value of a specific column:
     *   SELECT "columnName"
     *   FROM <table>
     *   WHERE <table>.enrollment = subax.enrollment
     *     AND "columnName" IS NOT NULL
     *     AND ps = 'programStageId'
     *   ORDER BY occurreddate DESC
     *   LIMIT 1
     *
     * @param subSelect the subselect to check.
     * @return Optional containing FoundSubSelect if it matches, empty Optional otherwise.
     */
    public static Optional<FoundSubSelect> matchesLastEventValuePattern(SubSelect subSelect) {
        if (!(subSelect.getSelectBody() instanceof PlainSelect plain)) {
            return Optional.empty();
        }

        // Check SELECT items
        List<SelectItem> selectItems = plain.getSelectItems();
        if (selectItems == null || selectItems.size() != 1) {
            return Optional.empty();
        }

        SelectItem item = selectItems.get(0);
        if (!(item instanceof SelectExpressionItem sei)) {
            return Optional.empty();
        }

        Expression selectExpr = sei.getExpression();
        if (!(selectExpr instanceof Column col)) {
            return Optional.empty();
        }

        String columnName = col.getColumnName();
        // Skip if it's one of our already handled special cases
        if ("scheduleddate".equalsIgnoreCase(columnName) ||
                "created".equalsIgnoreCase(columnName)) {
            return Optional.empty();
        }

        // Check FROM clause is a table
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table)) {
            return Optional.empty();
        }

        // Check WHERE clause
        Expression where = plain.getWhere();
        if (where == null) {
            return Optional.empty();
        }

        String whereStr = where.toString().toLowerCase();
        if (!whereStr.contains("subax.enrollment")) {
            return Optional.empty();
        }

        // Check for columnName IS NOT NULL condition
        if (!whereStr.contains(columnName.toLowerCase() + " is not null")) {
            return Optional.empty();
        }

        // Check for program stage condition (ps = 'someId')
        if (!whereStr.contains("ps =") && !whereStr.contains("ps is")) {
            return Optional.empty();
        }

        // Check ORDER BY and LIMIT
        if (plain.getOrderByElements() == null || plain.getOrderByElements().isEmpty()) {
            return Optional.empty();
        }
        if (plain.getLimit() == null) {
            return Optional.empty();
        }
        // Generate a unique name for this last-value CTE
        String cteName = "last_value_" + preserveLettersAndNumbers(preserveLettersAndNumbers(columnName.toLowerCase()));
        return Optional.of(new FoundSubSelect(cteName, subSelect, columnName));
    }

    /**
     * Checks if the given subselect matches one of the relationship count patterns:
     * Pattern 1 (without relationship ID):
     *   SELECT sum(relationship_count)
     *   FROM analytics_rs_relationship arr
     *   WHERE arr.trackedentityid = ax.trackedentity
     *
     * Pattern 2 (with relationship ID):
     *   SELECT relationship_count
     *   FROM analytics_rs_relationship arr
     *   WHERE arr.trackedentityid = ax.trackedentity
     *   AND relationshiptypeuid = 'specific_uid'
     *
     * @param subSelect the subselect to check
     * @return Optional containing FoundSubSelect if it matches, empty Optional otherwise
     */
    public static Optional<FoundSubSelect> matchesRelationshipCountPattern(SubSelect subSelect) {
        if (!(subSelect.getSelectBody() instanceof PlainSelect plain)) {
            return Optional.empty();
        }

        // Check FROM clause: should be analytics_rs_relationship
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table table) ||
                !table.getName().toLowerCase().contains("analytics_rs_relationship")) {
            return Optional.empty();
        }

        // Check SELECT clause
        List<SelectItem> selectItems = plain.getSelectItems();
        if (selectItems == null || selectItems.size() != 1) {
            return Optional.empty();
        }

        SelectItem item = selectItems.get(0);
        if (!(item instanceof SelectExpressionItem sei)) {
            return Optional.empty();
        }

        Expression selectExpr = sei.getExpression();
        boolean isAggregated = false;

        // Check for either sum(relationship_count) or just relationship_count
        if (selectExpr instanceof Function func) {
            if (!"sum".equalsIgnoreCase(func.getName()) ||
                    func.getParameters() == null ||
                    func.getParameters().getExpressions().size() != 1) {
                return Optional.empty();
            }

            Expression sumParam = func.getParameters().getExpressions().get(0);
            if (!(sumParam instanceof Column col) ||
                    !"relationship_count".equalsIgnoreCase(col.getColumnName())) {
                return Optional.empty();
            }
            isAggregated = true;
        } else if (selectExpr instanceof Column col) {
            if (!"relationship_count".equalsIgnoreCase(col.getColumnName())) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

        // Check WHERE clause
        Expression where = plain.getWhere();
        if (where == null) {
            return Optional.empty();
        }

        // Handle both cases: simple equality or equality AND relationship type
        if (where instanceof EqualsTo equals) {
            // Case without relationship ID
            if (!isValidTrackedEntityComparison(equals)) {
                return Optional.empty();
            }
        } else if (where instanceof AndExpression and) {
            // Case with relationship ID
            Expression left = and.getLeftExpression();
            Expression right = and.getRightExpression();

            // Check for trackedentityid comparison
            if (!(left instanceof EqualsTo equals && isValidTrackedEntityComparison(equals))) {
                return Optional.empty();
            }

            // Check for relationshiptypeuid comparison
            if (!(right instanceof EqualsTo relationshipTypeEquals)) {
                return Optional.empty();
            }

            Expression typeLeft = relationshipTypeEquals.getLeftExpression();
            if (!(typeLeft instanceof Column typeCol) ||
                    !"relationshiptypeuid".equalsIgnoreCase(typeCol.getColumnName())) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

        // If we get here, the pattern matches
        // Use a different name for the CTE based on whether it's aggregated
        String cteName = "relationship_count";
        return Optional.of(new FoundSubSelect(cteName, subSelect, "relationship_count"));
    }

    /**
     * Helper method to validate the trackedentityid = ax.trackedentity comparison
     */
    private boolean isValidTrackedEntityComparison(EqualsTo equals) {
        Expression left = equals.getLeftExpression();
        Expression right = equals.getRightExpression();

        if (!(left instanceof Column leftCol && right instanceof Column rightCol)) {
            return false;
        }

        return "trackedentityid".equalsIgnoreCase(leftCol.getColumnName()) &&
                rightCol.getTable() != null &&
                ("ax".equalsIgnoreCase(rightCol.getTable().getName()) ||
                        "subax".equalsIgnoreCase(rightCol.getTable().getName())) &&
                "trackedentity".equalsIgnoreCase(rightCol.getColumnName());
    }
    private String preserveLettersAndNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}
