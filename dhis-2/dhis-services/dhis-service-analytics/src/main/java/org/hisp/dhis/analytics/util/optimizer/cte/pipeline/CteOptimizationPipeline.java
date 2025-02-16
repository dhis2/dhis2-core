package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.hisp.dhis.analytics.util.optimizer.cte.data.DecomposedCtes;

import java.util.List;

@Slf4j
public class CteOptimizationPipeline {

    private final CteOptimizerParser parser;
    private final CteSubqueryIdentifier subqueryIdentifier;
    private final CteDecomposer cteDecomposer;
    private final CteSqlRebuilder sqlRebuilder;

    public CteOptimizationPipeline() {
        this.parser = new CteOptimizerParser();
        this.subqueryIdentifier = new CteSubqueryIdentifier();
        this.cteDecomposer = new CteDecomposer();
        this.sqlRebuilder = new CteSqlRebuilder();
    }

    public String optimize(String sql) {
        Select statement = (Select) parser.parse(sql);
        if (statement == null) {
            return sql; // TODO throw exception?
        }

        List<WithItem> withItems = subqueryIdentifier.findQualifyingPiCtes(statement);
        log.debug("Found {} qualifying CTEs", withItems.size());
        if (withItems.isEmpty()) {
            log.debug("No qualifying CTEs found, skipping optimization");
            return sql;
        }
        DecomposedCtes generatedCtes = cteDecomposer.processCTE(withItems);
        log.debug("Generated CTEs: {}", generatedCtes);
        Select rebuiltSql = sqlRebuilder.rebuildSql(statement, generatedCtes);

        StringBuilder buffer = new StringBuilder();
        StatementDeParser deparser = new StatementDeParser(buffer);
        rebuiltSql.accept(deparser);
        return buffer.toString();
    }
}
