package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.ProgramIndicator;

import java.util.Date;
import java.util.Map;

/**
 * Strategy for recognising a placeholder family and producing the
 * substituted SQL *plus* the necessary CTE side-effects.
 *
 * Implementations must be stateless and thread-safe.
 */
public interface CteSqlFactory {

    /** Returns {@code true} when {@code rawSql} contains this factory’s placeholder grammar. */
    boolean supports(String rawSql);

    /**
     * Replaces placeholders with “alias.value/coalesce(…)” and
     * registers the required CTEs in {@code cteContext}.
     *
     * @param aliasMap caller-supplied map; implementation must add one entry per placeholder.
     */
    String process(String rawSql,
                   ProgramIndicator programIndicator,
                   Date earliestStart,
                   Date latestEnd,
                   CteContext cteContext,
                   Map<String,String> aliasMap,
                   SqlBuilder sqlBuilder);
}