package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RelationshipCountMatcher extends AbstractCountMatcher {

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