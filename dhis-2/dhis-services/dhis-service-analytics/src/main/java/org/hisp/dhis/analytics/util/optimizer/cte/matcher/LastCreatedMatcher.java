package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;

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
public class LastCreatedMatcher extends AbstractLastValueMatcher {
    @Override
    protected boolean validateColumn(Column col, PlainSelect plain) {
        return "created".equalsIgnoreCase(col.getColumnName());
    }

    @Override
    protected String getCteName(Column col) {
        return "last_created";
    }
}