package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.ProgramIndicator;

import java.util.Date;
import java.util.List;
import java.util.Map;


public class CteSqlFactoryRegistry {
    private final List<CteSqlFactory> factories;
    private final DataElementService dataElementService;

    public CteSqlFactoryRegistry(DataElementService dataElementService) {
        this.dataElementService = dataElementService;
        factories = List.of(
                new VariableCteFactory(),
                new FilterCteFactory(),
                new PsDeCteFactory(dataElementService),
                new D2FunctionCteFactory());
    }

    /**
     * Fallback that performs no parsing and leaves the SQL untouched.
     */
    private static final CteSqlFactory NOOP_FACTORY = new CteSqlFactory() {
        @Override
        public boolean supports(String rawSql) {
            return false;
        }

        @Override
        public String process(String rawSql,
                              ProgramIndicator pi,
                              Date start,
                              Date end,
                              CteContext ctx,
                              Map<String, String> aliasMap,
                              SqlBuilder qb) {
            // keep incoming SQL, do not touch context or alias map
            return rawSql;
        }
    };


    public CteSqlFactory factoryFor(String rawSql) {
        return factories.stream()
                .filter(f -> f.supports(rawSql))
                .findFirst()
                .orElse(NOOP_FACTORY);
    }
}
