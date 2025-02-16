package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DataElementCountMatcher extends AbstractCountMatcher {

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