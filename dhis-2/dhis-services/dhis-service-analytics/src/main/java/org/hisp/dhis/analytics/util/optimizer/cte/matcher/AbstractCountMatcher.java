package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

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
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCountMatcher {

    protected abstract Optional<FoundSubSelect> match(SubSelect subSelect);

    protected Optional<PlainSelect> asPlainSelect(SubSelect subSelect) {
        SelectBody selectBody = subSelect.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return Optional.empty();
        }
        return Optional.of((PlainSelect) selectBody);
    }

    protected Optional<Expression> hasSingleExpression(PlainSelect select) {
        List<SelectItem> selectItems = select.getSelectItems();
        if (selectItems == null || selectItems.size() != 1) {
            return Optional.empty();
        }
        SelectItem item = selectItems.get(0);
        if (!(item instanceof SelectExpressionItem sei)) {
            return Optional.empty();
        }

        return Optional.of(sei.getExpression());
    }

    /**
     * Removes all characters from the input string that are not letters or numbers.
     *
     * @param str the input string.
     * @return a cleaned string containing only letters and numbers.
     */
    protected String preserveLettersAndNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Extracts conditions from a complex WHERE clause by flattening AND expressions.
     *
     * @param whereExpr     the WHERE clause expression.
     * @param dataElementId the data element id to check against.
     * @return a WhereClauseConditions record containing flags and extracted values.
     */
    protected WhereClauseConditions extractWhereConditions(Expression whereExpr, String dataElementId) {
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

    /**
     * Recursively flattens an AND expression into individual conditions.
     *
     * @param expr       the expression to flatten.
     * @param conditions the list to accumulate conditions.
     */
    private void flattenAndConditions(Expression expr, List<Expression> conditions) {
        if (expr instanceof AndExpression and) {
            flattenAndConditions(and.getLeftExpression(), conditions);
            flattenAndConditions(and.getRightExpression(), conditions);
        } else {
            conditions.add(expr);
        }
    }

    /**
     * Checks if the EqualsTo expression is the enrollment condition:
     *   <code>enrollment = subax.enrollment</code>
     *
     * @param equals the EqualsTo expression.
     * @return true if it matches the enrollment condition.
     */
    private boolean isEnrollmentCondition(EqualsTo equals) {
        Expression left = equals.getLeftExpression();
        Expression right = equals.getRightExpression();

        return left instanceof Column leftCol &&
                right instanceof Column rightCol &&
                "enrollment".equals(leftCol.getColumnName()) &&
                rightCol.getTable() != null &&
                "subax".equals(rightCol.getTable().getName()) &&
                "enrollment".equals(rightCol.getColumnName());
    }

    /**
     * Checks if the EqualsTo expression is a program stage condition (ps = ...).
     *
     * @param equals the EqualsTo expression.
     * @return true if the left-hand side is column "ps".
     */
    private boolean isProgramStageCondition(EqualsTo equals) {
        Expression left = equals.getLeftExpression();
        return left instanceof Column col && "ps".equals(col.getColumnName());
    }

    /**
     * Checks if the EqualsTo expression is a condition comparing the data element value.
     *
     * @param equals        the EqualsTo expression.
     * @param dataElementId the expected data element id.
     * @return true if the left-hand side matches the data element id.
     */
    private boolean isDataElementValueCondition(EqualsTo equals, String dataElementId) {
        Expression left = equals.getLeftExpression();
        return left instanceof Column col &&
                col.getColumnName().replaceAll("\"", "").equals(dataElementId) &&
                equals.getRightExpression() != null;
    }

    /**
     * Checks if the IsNullExpression represents a NOT NULL condition for the given data element.
     *
     * @param isNull        the IsNullExpression.
     * @param dataElementId the data element id to check.
     * @return true if the condition is "dataElementId IS NOT NULL".
     */
    private boolean isDataElementNotNullCondition(IsNullExpression isNull, String dataElementId) {
        Expression left = isNull.getLeftExpression();
        return left instanceof Column col &&
                col.getColumnName().replaceAll("\"", "").equals(dataElementId) &&
                isNull.isNot();
    }

    /**
     * Extracts the string value from an expression if it is a StringValue.
     *
     * @param expr the expression to extract from.
     * @return the string value or null.
     */
    private static String extractStringValue(Expression expr) {
        if (expr instanceof StringValue sv) {
            return sv.getValue();
        }
        return null;
    }

    /**
     * Record holding the extracted conditions from the WHERE clause for a data element count.
     */
    public record WhereClauseConditions(
            boolean hasEnrollmentCondition,
            boolean hasIsNotNullCondition,
            boolean hasValueCondition,
            boolean hasProgramStageCondition,
            String programStageId,
            String dataElementValue
    ) {
        /**
         * Checks if all required conditions for a valid data element count are met.
         *
         * @return true if valid; false otherwise.
         */
        boolean isValid() {
            return hasEnrollmentCondition &&
                    hasIsNotNullCondition &&
                    hasValueCondition &&
                    dataElementValue != null;
        }
    }
}

class RelationshipCountMatcher extends AbstractCountMatcher {

    public Optional<FoundSubSelect> match(SubSelect subSelect) {
        Optional<PlainSelect> maybePlain = asPlainSelect(subSelect);
        if (maybePlain.isEmpty()) {
            return Optional.empty();
        }
        PlainSelect plain = maybePlain.get();

        // FROM clause must be a table and contain "analytics_rs_relationship"
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table table) ||
                !table.getName().toLowerCase().contains("analytics_rs_relationship")) {
            return Optional.empty();
        }

        Optional<Expression> selectExpr = hasSingleExpression(plain);
        if (selectExpr.isEmpty()) {
            return Optional.empty();
        }

        boolean isAggregated = false;
        // Accept either sum(relationship_count) or relationship_count.
        if (selectExpr.get() instanceof Function func) {
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
        } else if (selectExpr.get() instanceof Column col) {
            if (!"relationship_count".equalsIgnoreCase(col.getColumnName())) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

        // Check WHERE clause.
        Expression where = plain.getWhere();
        if (where == null) {
            return Optional.empty();
        }
        String relationshipTypeUid = null;
        if (where instanceof EqualsTo equals) {
            // Pattern without relationship type condition.
            if (!isValidTrackedEntityComparison(equals)) {
                return Optional.empty();
            }
        } else if (where instanceof AndExpression and) {
            // Pattern with relationship type condition.
            Expression left = and.getLeftExpression();
            Expression right = and.getRightExpression();
            if (!(left instanceof EqualsTo equalsLeft && isValidTrackedEntityComparison(equalsLeft))) {
                return Optional.empty();
            }
            if (!(right instanceof EqualsTo relationshipTypeEquals)) {
                return Optional.empty();
            }
            Expression typeLeft = relationshipTypeEquals.getLeftExpression();
            Expression typeRight = relationshipTypeEquals.getRightExpression();
            if (!(typeLeft instanceof Column typeCol) ||
                    !"relationshiptypeuid".equalsIgnoreCase(typeCol.getColumnName())) {
                return Optional.empty();
            }
            if (typeRight instanceof StringValue stringValue) {
                relationshipTypeUid = stringValue.getValue();
            }
        } else {
            return Optional.empty();
        }

        String cteName = isAggregated ? "relationship_count_agg" : "relationship_count";

        Map<String, String> metadata = new HashMap<>();
        if (relationshipTypeUid != null) {
            metadata.put("relationshipTypeUid", relationshipTypeUid);
        }
        metadata.put("isAggregated", String.valueOf(isAggregated));

        return Optional.of(new FoundSubSelect(cteName, subSelect, "relationship_count", metadata));
    }

    /**
     * Validates the tracked entity comparison condition.
     * Expected: trackedentityid = ax.trackedentity OR trackedentityid = subax.trackedentity.
     *
     * @param equals the EqualsTo expression.
     * @return true if the expression matches the expected comparison.
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
}

class DataElementCountMatcher extends AbstractCountMatcher {

    public Optional<FoundSubSelect> match(SubSelect subSelect) {
        Optional<PlainSelect> maybePlain = asPlainSelect(subSelect);
        if (maybePlain.isEmpty()) {
            return Optional.empty();
        }
        PlainSelect plain = maybePlain.get();

        Optional<Expression> selectExpr = hasSingleExpression(plain);
        if (selectExpr.isEmpty()) {
            return Optional.empty();
        }

        if (!(selectExpr.get() instanceof Function func) ||
                !"count".equalsIgnoreCase(func.getName()) ||
                func.getParameters() == null ||
                func.getParameters().getExpressions().size() != 1) {
            return Optional.empty();
        }
        Expression countParam = func.getParameters().getExpressions().get(0);
        if (!(countParam instanceof Column col)) {
            return Optional.empty();
        }
        String dataElementId = col.getColumnName().replaceAll("\"", "");

        // FROM clause must be a table containing "analytics_event"
        FromItem fromItem = plain.getFromItem();
        if (!(fromItem instanceof Table table) ||
                !table.getName().toLowerCase().contains("analytics_event")) {
            return Optional.empty();
        }

        // WHERE clause must be an AndExpression with required conditions.
        Expression where = plain.getWhere();
        if (!(where instanceof AndExpression)) {
            return Optional.empty();
        }
        WhereClauseConditions conditions = extractWhereConditions(where, dataElementId);
        if (!conditions.isValid()) {
            return Optional.empty();
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("dataElementId", dataElementId);
        metadata.put("programStageId", conditions.programStageId());
        metadata.put("value", conditions.dataElementValue());

        String cteName = "de_count_" + preserveLettersAndNumbers(dataElementId);
        return Optional.of(new FoundSubSelect(cteName, subSelect, "de_count", metadata));
    }
}


