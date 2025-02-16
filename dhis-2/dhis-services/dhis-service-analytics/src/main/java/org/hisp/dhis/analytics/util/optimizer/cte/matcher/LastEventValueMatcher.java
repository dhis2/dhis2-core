package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;

public /**
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
