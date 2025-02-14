package org.hisp.dhis.analytics.util.vis;

import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        String relationshipTypeUid = null;

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
            Expression typeRight = relationshipTypeEquals.getRightExpression();

            if (!(typeLeft instanceof Column typeCol) ||
                    !"relationshiptypeuid".equalsIgnoreCase(typeCol.getColumnName())) {
                return Optional.empty();
            }

            // Extract the relationship type UID from the comparison
            if (typeRight instanceof StringValue stringValue) {
                relationshipTypeUid = stringValue.getValue();
            }
        } else {
            return Optional.empty();
        }

        // If we get here, the pattern matches
        // Use a different name for the CTE based on whether it's aggregated
        String cteName = isAggregated ? "relationship_count_agg" : "relationship_count";

        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        if (relationshipTypeUid != null) {
            metadata.put("relationshipTypeUid", relationshipTypeUid);
        }
        metadata.put("isAggregated", String.valueOf(isAggregated));

        return Optional.of(new FoundSubSelect(cteName, subSelect, "relationship_count", metadata));
    }

    /**
     * Checks if the given subselect matches the data element count pattern:
     *   SELECT count("dataElementId")
     *   FROM analytics_event_*
     *   WHERE analytics_event_*.enrollment = subax.enrollment
     *     AND "dataElementId" IS NOT NULL
     *     AND "dataElementId" = 1
     *     AND ps = 'programStageId'
     *
     * @param subSelect the subselect to check
     * @return Optional containing FoundSubSelect if it matches, empty Optional otherwise
     */
    public static Optional<FoundSubSelect> matchesDataElementCountPattern(SubSelect subSelect) {
        if (!(subSelect.getSelectBody() instanceof PlainSelect plain)) {
            return Optional.empty();
        }

        // Check SELECT clause - should be count of a data element
        List<SelectItem> selectItems = plain.getSelectItems();
        if (selectItems == null || selectItems.size() != 1) {
            return Optional.empty();
        }

        SelectItem item = selectItems.get(0);
        if (!(item instanceof SelectExpressionItem sei)) {
            return Optional.empty();
        }

        Expression selectExpr = sei.getExpression();
        if (!(selectExpr instanceof Function func) ||
                !"count".equalsIgnoreCase(func.getName()) ||
                func.getParameters() == null ||
                func.getParameters().getExpressions().size() != 1) {
            return Optional.empty();
        }

        Expression countParam = func.getParameters().getExpressions().get(0);
        if (!(countParam instanceof Column col)) {
            return Optional.empty();
        }

        // Remove quotes from the column name if present
        String dataElementId = col.getColumnName().replaceAll("\"", "");

        // Check FROM clause - should be analytics_event_*
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table table) ||
                !table.getName().toLowerCase().contains("analytics_event")) {
            return Optional.empty();
        }

        // Check WHERE clause conditions
        Expression where = plain.getWhere();
        if (!(where instanceof AndExpression)) {
            return Optional.empty();
        }

        // Parse the WHERE clause to verify all required conditions
        WhereClauseConditions conditions = extractWhereConditions(where, dataElementId);

        if (!conditions.isValid()) {
            return Optional.empty();
        }

        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        metadata.put("dataElementId", dataElementId);
        metadata.put("programStageId", conditions.programStageId());
        metadata.put("value", conditions.dataElementValue());

        // Generate CTE name using the data element ID
        String cteName = "de_count_" + preserveLettersAndNumbers(dataElementId);

        return Optional.of(new FoundSubSelect(cteName, subSelect, "de_count", metadata));
    }

    private record WhereClauseConditions(
            boolean hasEnrollmentCondition,
            boolean hasIsNotNullCondition,
            boolean hasValueCondition,
            boolean hasProgramStageCondition,
            String programStageId,
            String dataElementValue  // This could be any value, not just "1"
    ) {
        boolean isValid() {
            return hasEnrollmentCondition &&
                    hasIsNotNullCondition &&
                    hasValueCondition &&
                    dataElementValue != null;  // Remove programStage requirement
        }
    }


    private static WhereClauseConditions extractWhereConditions(Expression whereExpr, String dataElementId) {
        boolean hasEnrollmentCondition = false;
        boolean hasIsNotNullCondition = false;
        boolean hasValueCondition = false;
        boolean hasProgramStageCondition = false;
        String programStageId = null;
        String dataElementValue = null;

        List<Expression> conditions = new ArrayList<>();
        flattenAndConditions(whereExpr, conditions);

        for (Expression condition : conditions) {
            if (condition instanceof EqualsTo equals) {
                if (isEnrollmentCondition(equals)) {
                    hasEnrollmentCondition = true;
                } else if (isProgramStageCondition(equals)) {
                    hasProgramStageCondition = true;
                    programStageId = extractStringValue(equals.getRightExpression());
                } else if (isDataElementValueCondition(equals, dataElementId)) {
                    hasValueCondition = true;
                    dataElementValue = equals.getRightExpression().toString();
                }
            } else if (condition instanceof IsNullExpression isNull) {
                if (isDataElementNotNullCondition(isNull, dataElementId)) {
                    hasIsNotNullCondition = true;
                }
            }
        }

        return new WhereClauseConditions(
                hasEnrollmentCondition,
                hasIsNotNullCondition,
                hasValueCondition,
                hasProgramStageCondition,
                programStageId,
                dataElementValue
        );
    }

    private static void flattenAndConditions(Expression expr, List<Expression> conditions) {
        if (expr instanceof AndExpression and) {
            flattenAndConditions(and.getLeftExpression(), conditions);
            flattenAndConditions(and.getRightExpression(), conditions);
        } else {
            conditions.add(expr);
        }
    }

    private static boolean isEnrollmentCondition(EqualsTo equals) {
        Expression left = equals.getLeftExpression();
        Expression right = equals.getRightExpression();

        return left instanceof Column leftCol &&
                right instanceof Column rightCol &&
                leftCol.getColumnName().equals("enrollment") &&
                rightCol.getTable() != null &&
                rightCol.getTable().getName().equals("subax") &&
                rightCol.getColumnName().equals("enrollment");
    }

    private static boolean isProgramStageCondition(EqualsTo equals) {
        Expression left = equals.getLeftExpression();
        return left instanceof Column col && col.getColumnName().equals("ps");
    }

    private static boolean isDataElementValueCondition(EqualsTo equals, String dataElementId) {
        Expression left = equals.getLeftExpression();
        return left instanceof Column col &&
                col.getColumnName().replaceAll("\"", "").equals(dataElementId) &&
                equals.getRightExpression() != null;  // Accept any non-null value
    }

    private static boolean isDataElementNotNullCondition(IsNullExpression isNull, String dataElementId) {
        Expression left = isNull.getLeftExpression();
        return left instanceof Column col &&
                col.getColumnName().replaceAll("\"", "").equals(dataElementId) &&
                isNull.isNot();
    }

    private static String extractStringValue(Expression expr) {
        if (expr instanceof StringValue sv) {
            return sv.getValue();
        }
        return null;
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
