package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.util.optimizer.cte.CteOptimizerException;

/**
 * Generic SQL parser for CTE optimization.
 */
public class CteOptimizerParser implements SqlOptimizationStep {

    /**
     * Parse the given SQL statement into a {@link Statement}.
     * The statement is an AST (Abstract syntax tree) representation of the SQL.
     *
     * @param sql the SQL statement to parse
     * @return the parsed {@link Statement}
     * @throws CteOptimizerException if an error occurs during parsing
     */
    public Statement parse(String sql) {
        if (StringUtils.isEmpty(sql)) {
            throw new CteOptimizerException("SQL is empty");
        }
        try {
            return CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new CteOptimizerException("Error parsing SQL: " + sql, e);
        }
    }
}
