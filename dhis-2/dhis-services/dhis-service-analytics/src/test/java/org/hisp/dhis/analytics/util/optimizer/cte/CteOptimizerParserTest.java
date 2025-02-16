package org.hisp.dhis.analytics.util.optimizer.cte;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.hisp.dhis.analytics.util.optimizer.cte.pipeline.CteOptimizerParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CteOptimizerParserTest {

    @Test
    void testParse_ValidSelect() throws CteOptimizerException {
        CteOptimizerParser parser = new CteOptimizerParser();
        String sql = "SELECT * FROM employees;";
        Statement statement = parser.parse(sql);
        assertNotNull(statement, "Statement should not be null for valid SQL");
        assertInstanceOf(Select.class, statement, "Statement should be a Select statement");
    }

    @Test
    void testParse_InvalidSQL() {
        CteOptimizerParser parser = new CteOptimizerParser();
        String sql = "SELECT FROM invalid_table"; // Invalid SQL
        assertThrows(CteOptimizerException.class, () -> {
            parser.parse(sql);
        }, "Invalid SQL should throw CteOptimizerException");
    }

    @Test
    void testParse_EmptySQL() throws CteOptimizerException{
        CteOptimizerParser parser = new CteOptimizerParser();
        String sql = "";
        assertThrows(CteOptimizerException.class, () -> {
            parser.parse(sql);
        }, "Invalid SQL should throw CteOptimizerException");
    }

}