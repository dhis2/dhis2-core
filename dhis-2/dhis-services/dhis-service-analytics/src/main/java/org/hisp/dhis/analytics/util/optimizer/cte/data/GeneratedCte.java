package org.hisp.dhis.analytics.util.optimizer.cte.data;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @param name            the name of the CTE
 * @param cteString       the SQL string of the CTE
 * @param joinAlias       the name of the alias to join the CTE with
 *                        for instance, if the value is "rlc", the join will be
 *                        left join relationship_count as rlc on ...
 */
public record GeneratedCte(String name,
                           String cteString,
                           String joinAlias,
                           ImmutablePair<String, String> joinColumns) {

    public GeneratedCte(String name, String cteString, String joinAlias) {
        this(name, cteString, joinAlias, null);
    }
}