package org.hisp.dhis.analytics.util.optimizer.cte.data;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.List;

public record DecomposedCtes(List<GeneratedCte> ctes,
                             Expression transformedWhere,
                             PlainSelect originalSelect) {

    public static DecomposedCtes empty() {
        return new DecomposedCtes(List.of(), null, null);
    }
}
