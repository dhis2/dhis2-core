package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractCountMatcher implements SubselectMatcher {

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
     * <code>enrollment = subax.enrollment</code>
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



