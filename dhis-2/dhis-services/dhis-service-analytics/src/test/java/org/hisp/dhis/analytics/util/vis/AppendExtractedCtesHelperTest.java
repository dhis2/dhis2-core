package org.hisp.dhis.analytics.util.vis;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hisp.dhis.analytics.util.vis.AppendExtractedCtesHelper.appendExtractedCtes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppendExtractedCtesHelperTest {
    private Select baseSelect;

    @BeforeEach
    void setUp() throws JSQLParserException {
        // Create a basic select statement for testing
        baseSelect = (Select) CCJSqlParserUtil.parse("SELECT * FROM test_table");
    }

    @Test
    void appendExtractedCtes_SingleCte_NoExistingWith() throws JSQLParserException {

        Map<String, GeneratedCte> generatedCtes = new HashMap<>();
        generatedCtes.put("cte1", new GeneratedCte(
                "cte1",
                "SELECT id, name FROM source_table",
                "join_alias"
        ));

        appendExtractedCtes(baseSelect, generatedCtes);

        assertNotNull(baseSelect.getWithItemsList());
        assertEquals(1, baseSelect.getWithItemsList().size());
        assertEquals("cte1", baseSelect.getWithItemsList().get(0).getName());
    }

    @Test
    void appendExtractedCtes_WithExistingCtes() throws JSQLParserException {

        Select selectWithCte = (Select) CCJSqlParserUtil.parse(
                "WITH existing_cte AS (SELECT * FROM existing_table) SELECT * FROM test_table"
        );

        Map<String, GeneratedCte> generatedCtes = new HashMap<>();
        generatedCtes.put("new_cte", new GeneratedCte(
                "new_cte",
                "SELECT id FROM new_table",
                "join_alias"
        ));

        appendExtractedCtes(selectWithCte, generatedCtes);

        assertEquals(2, selectWithCte.getWithItemsList().size());
        assertEquals("new_cte", selectWithCte.getWithItemsList().get(0).getName());
        assertEquals("existing_cte", selectWithCte.getWithItemsList().get(1).getName());
    }

    @Test
    void appendExtractedCtes_EmptyMap() {

        Map<String, GeneratedCte> emptyCtes = new HashMap<>();

        appendExtractedCtes(baseSelect, emptyCtes);

        assertTrue(baseSelect.getWithItemsList().isEmpty());
    }

    @Test
    void appendExtractedCtes_NullSelect() {

        Map<String, GeneratedCte> generatedCtes = new HashMap<>();
        generatedCtes.put("cte1", new GeneratedCte(
                "cte1",
                "SELECT id FROM table",
                "join_alias"
        ));

        assertThrows(NullPointerException.class, () ->
                appendExtractedCtes(null, generatedCtes)
        );
    }

    @Test
    void appendExtractedCtes_InvalidSqlSyntax() {

        Map<String, GeneratedCte> generatedCtes = new HashMap<>();
        generatedCtes.put("invalid_cte", new GeneratedCte(
                "invalid_cte",
                "INVALID SQL SYNTAX",
                "join_alias"
        ));

        IllegalQueryException exception = assertThrows(IllegalQueryException.class, () ->
                appendExtractedCtes(baseSelect, generatedCtes)
        );
        assertTrue(exception.getErrorCode().equals(ErrorCode.E7149));
    }

    @Test
    void appendExtractedCtes_ComplexCte() throws JSQLParserException {

        Map<String, GeneratedCte> generatedCtes = new HashMap<>();
        String complexQuery = """
                SELECT t1.id, t2.name, COUNT(*) as count 
                FROM table1 t1 
                JOIN table2 t2 ON t1.id = t2.id 
                GROUP BY t1.id, t2.name 
                HAVING COUNT(*) > 1
                """;

        generatedCtes.put("complex_cte", new GeneratedCte(
                "complex_cte",
                complexQuery,
                "join_alias"
        ));


        appendExtractedCtes(baseSelect, generatedCtes);

        // Assert
        assertNotNull(baseSelect.getWithItemsList());
        assertEquals(1, baseSelect.getWithItemsList().size());
        assertEquals("complex_cte", baseSelect.getWithItemsList().get(0).getName());
    }

    @Test
    void appendExtractedCtes_MultipleCtes() throws JSQLParserException {

        Map<String, GeneratedCte> generatedCtes = new HashMap<>();
        generatedCtes.put("cte1", new GeneratedCte(
                "cte1",
                "SELECT id FROM table1",
                "join_alias1"
        ));
        generatedCtes.put("cte2", new GeneratedCte(
                "cte2",
                "SELECT name FROM table2",
                "join_alias2"
        ));

        appendExtractedCtes(baseSelect, generatedCtes);

        assertEquals(2, baseSelect.getWithItemsList().size());
        // Note: The order might depend on HashMap iteration order
        assertTrue(baseSelect.getWithItemsList().stream()
                .map(WithItem::getName)
                .anyMatch(name -> name.equals("cte1")));
        assertTrue(baseSelect.getWithItemsList().stream()
                .map(WithItem::getName)
                .anyMatch(name -> name.equals("cte2")));
    }
}