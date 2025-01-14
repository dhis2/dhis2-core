package org.hisp.dhis.analytics.util.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlColumnParserTest {
    @Test
    void testRemoveTableAlias_WithDoubleQuotes() throws Exception {
        String result = SqlColumnParser.removeTableAlias("ax.\"uidlevel2\"");
        assertEquals("uidlevel2", result);
    }

    @Test
    void testRemoveTableAlias_WithBackticks() throws Exception {
        String result = SqlColumnParser.removeTableAlias("cc.`alfa`");
        assertEquals("alfa", result);
    }

    @Test
    void testRemoveTableAlias_WithoutQuotes() throws Exception {
        String result = SqlColumnParser.removeTableAlias("test1.uidlevel2");
        assertEquals("uidlevel2", result);
    }

    @Test
    void testRemoveTableAlias_NoAlias() throws Exception {
        String result = SqlColumnParser.removeTableAlias("uidlevel2");
        assertEquals("uidlevel2", result);
    }

    @Test
    void testRemoveTableAlias_EmptyString() throws Exception {
        String result = SqlColumnParser.removeTableAlias("");
        assertEquals("", result);
    }

    @Test
    void testRemoveTableAlias_NullInput() throws Exception {
        String result = SqlColumnParser.removeTableAlias(null);
        assertNull(result);
    }


    @Test
    void testRemoveTableAlias_ComplexColumnName() throws Exception {
        String result = SqlColumnParser.removeTableAlias("schema.table.\"complex.column.name\"");
        assertEquals("complex.column.name", result);
    }

    @Test
    void testRemoveTableAlias_MultipleDots() throws Exception {
        String result = SqlColumnParser.removeTableAlias("schema.table.column");
        assertEquals("column", result);
    }
}