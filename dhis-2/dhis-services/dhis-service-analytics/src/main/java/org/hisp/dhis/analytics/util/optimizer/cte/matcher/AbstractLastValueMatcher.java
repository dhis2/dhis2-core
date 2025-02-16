package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.List;
import java.util.Optional;

/**
 * An abstract matcher that implements the common logic for matching last-value subselects.
 */
public abstract class AbstractLastValueMatcher {

    /**
     * Attempts to match the provided SubSelect.
     *
     * @param subSelect the subselect to check.
     * @return an Optional FoundSubSelect if the pattern matches; empty otherwise.
     */
    public Optional<FoundSubSelect> match(SubSelect subSelect) {
        Optional<PlainSelect> maybePlain = asPlainSelect(subSelect);
        if (maybePlain.isEmpty()) {
            return Optional.empty();
        }
        PlainSelect plain = maybePlain.get();

        // Validate that the SELECT clause is a single expression item.
        Optional<SelectExpressionItem> seiOpt = extractSingleSelectExpressionItem(plain);
        if (seiOpt.isEmpty()) {
            return Optional.empty();
        }
        SelectExpressionItem sei = seiOpt.get();
        Expression selectExpr = sei.getExpression();
        if (!(selectExpr instanceof Column col)) {
            return Optional.empty();
        }

        // Delegate column-specific validation to the subclass.
        if (!validateColumn(col, plain)) {
            return Optional.empty();
        }

        // Validate FROM clause is a table.
        if (!isTable(plain.getFromItem())) {
            return Optional.empty();
        }

        // Validate that the WHERE clause exists and contains "subax.enrollment".
        Expression where = plain.getWhere();
        if (where == null || !where.toString().toLowerCase().contains("subax.enrollment")) {
            return Optional.empty();
        }

        // Allow subclasses to perform additional validations.
        if (!additionalValidation(plain, col)) {
            return Optional.empty();
        }

        // Validate that ORDER BY and LIMIT exist.
        if (!hasOrderByAndLimit(plain)) {
            return Optional.empty();
        }

        // Create the CTE name based on the matched column.
        String cteName = getCteName(col);
        return Optional.of(new FoundSubSelect(cteName, subSelect, col.getColumnName()));
    }

    /**
     * Validates the column from the SELECT clause.
     *
     * @param col   the column from the SELECT clause.
     * @param plain the PlainSelect statement.
     * @return true if the column meets the expected criteria.
     */
    protected abstract boolean validateColumn(Column col, PlainSelect plain);

    /**
     * Provides the CTE name for the matched subselect.
     *
     * @param col the column from the SELECT clause.
     * @return the CTE name.
     */
    protected abstract String getCteName(Column col);

    /**
     * Allows subclasses to perform any additional validation after the common checks.
     * Defaults to returning true.
     *
     * @param plain the PlainSelect statement.
     * @param col   the matched column.
     * @return true if additional validation passes.
     */
    protected boolean additionalValidation(PlainSelect plain, Column col) {
        return true;
    }

    // -------------------- Helper Methods --------------------

    protected Optional<PlainSelect> asPlainSelect(SubSelect subSelect) {
        if (subSelect.getSelectBody() instanceof PlainSelect plain) {
            return Optional.of(plain);
        }
        return Optional.empty();
    }

    protected Optional<SelectExpressionItem> extractSingleSelectExpressionItem(PlainSelect plain) {
        List<SelectItem> items = plain.getSelectItems();
        if (items == null || items.size() != 1) {
            return Optional.empty();
        }
        SelectItem item = items.get(0);
        if (item instanceof SelectExpressionItem sei) {
            return Optional.of(sei);
        }
        return Optional.empty();
    }

    protected boolean isTable(FromItem fromItem) {
        return fromItem instanceof Table;
    }

    protected boolean hasOrderByAndLimit(PlainSelect plain) {
        return plain.getOrderByElements() != null && !plain.getOrderByElements().isEmpty() && plain.getLimit() != null;
    }

    /**
     * Utility method to remove all characters except letters and numbers.
     */
    protected String preserveLettersAndNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}

/**
 * Matcher for the "last_sched" pattern:
 *
 * <pre>
 *   SELECT scheduleddate
 *   FROM &lt;some_table&gt;
 *   WHERE &lt;some_table&gt;.enrollment = subax.enrollment
 *     AND scheduleddate IS NOT NULL
 *   ORDER BY occurreddate DESC
 *   LIMIT 1
 * </pre>
 */
class LastSchedMatcher extends AbstractLastValueMatcher {
    @Override
    protected boolean validateColumn(Column col, PlainSelect plain) {
        return "scheduleddate".equalsIgnoreCase(col.getColumnName());
    }

    @Override
    protected String getCteName(Column col) {
        return "last_sched";
    }
}

/**
 * Matcher for the "last_created" pattern:
 *
 * <pre>
 *   SELECT created
 *   FROM &lt;table&gt;
 *   WHERE &lt;table&gt;.enrollment = subax.enrollment
 *     AND created IS NOT NULL
 *   ORDER BY occurreddate DESC
 *   LIMIT 1
 * </pre>
 */
class LastCreatedMatcher extends AbstractLastValueMatcher {
    @Override
    protected boolean validateColumn(Column col, PlainSelect plain) {
        return "created".equalsIgnoreCase(col.getColumnName());
    }

    @Override
    protected String getCteName(Column col) {
        return "last_created";
    }
}

/**
 * Matcher for the "last event value" pattern:
 *
 * <pre>
 *   SELECT "columnName"
 *   FROM &lt;table&gt;
 *   WHERE &lt;table&gt;.enrollment = subax.enrollment
 *     AND "columnName" IS NOT NULL
 *     AND ps = 'programStageId'
 *   ORDER BY occurreddate DESC
 *   LIMIT 1
 * </pre>
 *
 * This matcher applies only if the column is not "scheduleddate" or "created".
 */
class LastEventValueMatcher extends AbstractLastValueMatcher {
    @Override
    protected boolean validateColumn(Column col, PlainSelect plain) {
        String columnName = col.getColumnName();
        // Exclude columns handled by other matchers.
        return !("scheduleddate".equalsIgnoreCase(columnName) || "created".equalsIgnoreCase(columnName));
    }

    @Override
    protected boolean additionalValidation(PlainSelect plain, Column col) {
        String columnName = col.getColumnName();
        String whereStr = plain.getWhere().toString().toLowerCase();
        // Ensure that the WHERE clause contains the column IS NOT NULL condition
        // and a program stage condition.
        return whereStr.contains(columnName.toLowerCase() + " is not null")
                && (whereStr.contains("ps =") || whereStr.contains("ps is"));
    }

    @Override
    protected String getCteName(Column col) {
        // The original code uses double preservation of letters/numbers.
        String cleaned = preserveLettersAndNumbers(preserveLettersAndNumbers(col.getColumnName().toLowerCase()));
        return "last_value_" + cleaned;
    }
}