package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;

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
public class LastSchedMatcher extends AbstractLastValueMatcher {
    @Override
    protected boolean validateColumn(Column col, PlainSelect plain) {
        return "scheduleddate".equalsIgnoreCase(col.getColumnName());
    }

    @Override
    protected String getCteName(Column col) {
        return "last_sched";
    }
}
