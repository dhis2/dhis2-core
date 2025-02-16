package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.analytics.util.optimizer.cte.data.DecomposedCtes;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CteDecomposerTest {

    @Test
    void t1() throws JSQLParserException {
        String withItemSql = """
                WITH pi_inputcte AS (
                    SELECT subax.enrollment
                    FROM analytics_enrollment_ur1edk5oe2n AS subax
                    WHERE (
                        SELECT scheduleddate
                        FROM analytics_event_ur1edk5oe2n
                        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                        AND scheduleddate IS NOT NULL
                        ORDER BY occurreddate DESC
                        LIMIT 1
                    ) IS NOT NULL
                )
                SELECT * FROM analytics_enrollment_ur1edk5oe2n;
                """;

        Select select = (Select) CCJSqlParserUtil.parse(withItemSql);

        // Get the WITH items from the statement
        WithItem withItem = select.getWithItemsList().get(0);

        CteDecomposer decomposer = new CteDecomposer();

        // Process the WITH item
        DecomposedCtes result = decomposer.processCTE(List.of(withItem));

        // Verify the result
        assertNotNull(result, "Result should not be null");

        assertEquals(1, result.ctes().size(), "Should find two subquery");
    }

    @Test
    void t4() throws JSQLParserException {
        String originalQuery = """
                    WITH pi_hgtnuhsqbml AS (
                        SELECT
                            enrollment
                        FROM analytics_enrollment_ur1edk5oe2n AS subax
                        WHERE ((
                            date_part('year',age(cast(
                                                 (
                                                 SELECT   scheduleddate
                                                 FROM     analytics_event_ur1edk5oe2n
                                                 WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                                 AND      scheduleddate IS NOT NULL
                                                 ORDER BY occurreddate DESC
                                                 LIMIT    1 ) AS date), cast(coalesce(completeddate,
                                      (
                                               SELECT   created
                                               FROM     analytics_event_ur1edk5oe2n
                                               WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                               AND      created IS NOT NULL
                                               ORDER BY occurreddate DESC
                                               LIMIT    1 )) AS date)))) * 12 + date_part('month',age(cast(
                                                        (
                                                        SELECT   scheduleddate
                                                        FROM     analytics_event_ur1edk5oe2n
                                                        WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                                        AND      scheduleddate IS NOT NULL
                                                        ORDER BY occurreddate DESC
                                                        LIMIT    1 ) AS date), cast(coalesce(completeddate,
                             (
                                      SELECT   created
                                      FROM     analytics_event_ur1edk5oe2n
                                      WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                      AND      created IS NOT NULL
                                      ORDER BY occurreddate DESC
                                      LIMIT    1 )) AS date)))) > 1
                    GROUP BY enrollment
                )
                SELECT
                    ax.enrollment
                FROM analytics_enrollment_ur1edk5oe2n AS ax
                LEFT JOIN pi_hgtnuhsqbml kektm ON kektm.enrollment = ax.enrollment
                WHERE (lastupdated >= '2015-01-01' AND lastupdated < '2025-01-01')
                AND ax."uidlevel1" = 'ImspTQPwCqd'
                LIMIT 101
                OFFSET 0
                """;

        Select select = (Select) CCJSqlParserUtil.parse(originalQuery);

        // Get the WITH items from the statement
        WithItem withItem = select.getWithItemsList().get(0);

        CteDecomposer decomposer = new CteDecomposer();

        // Process the WITH item
        DecomposedCtes result = decomposer.processCTE(List.of(withItem));

        // Verify the result
        assertNotNull(result, "Result should not be null");

        assertEquals(2, result.ctes().size(), "Should find two subquery");
    }

    @Test
    void shouldDecomposeTwoSubqueries() throws JSQLParserException {
        String withItemSql = """
                WITH pi_inputcte AS (
                    SELECT subax.enrollment
                    FROM analytics_enrollment_ur1edk5oe2n AS subax
                    WHERE (
                        SELECT scheduleddate
                        FROM analytics_event_ur1edk5oe2n
                        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                        AND scheduleddate IS NOT NULL
                        ORDER BY occurreddate DESC
                        LIMIT 1
                    ) IS NOT NULL
                    AND (
                        SELECT "de_value"
                        FROM analytics_event_ur1edk5oe2n
                        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                        AND "de_value" IS NOT NULL
                        AND ps = 'stage1'
                        ORDER BY occurreddate DESC
                        LIMIT 1
                    ) IS NOT NULL
                )
                SELECT * FROM analytics_enrollment_ur1edk5oe2n;
                """;

        Select select = (Select) CCJSqlParserUtil.parse(withItemSql);
        WithItem withItem = select.getWithItemsList().get(0);
        CteDecomposer decomposer = new CteDecomposer();
        DecomposedCtes result = decomposer.processCTE(List.of(withItem));

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.ctes().size(), "Should find two subqueries");

        // Verify specific patterns were found
        List<String> cteNames = result.ctes().stream()
                .map(GeneratedCte::name)
                .toList();

        assertTrue(cteNames.contains("last_sched"),
                "Should contain last_sched CTE");
        assertTrue(cteNames.stream().anyMatch(name -> name.startsWith("last_value_")),
                "Should contain last_value CTE");
    }

    @Test
    void t3() throws JSQLParserException {
        String withItemSql = """
                WITH pi_inputcte AS (
                    SELECT subax.enrollment
                    FROM analytics_enrollment_ur1edk5oe2n AS subax
                    WHERE (
                        (select
                            sum(relationship_count)
                        from
                            analytics_rs_relationship arr
                        where
                            arr.trackedentityid = subax.trackedentity) > 10
                    )
                )
                SELECT * FROM analytics_enrollment_ur1edk5oe2n;
                """;

        Select select = (Select) CCJSqlParserUtil.parse(withItemSql);

        // Get the WITH items from the statement
        WithItem withItem = select.getWithItemsList().get(0);

        CteDecomposer decomposer = new CteDecomposer();

        // Process the WITH item
        DecomposedCtes result = decomposer.processCTE(List.of(withItem));

        // Verify the result
        assertNotNull(result, "Result should not be null");

        assertEquals(1, result.ctes().size(), "Should find two subquery");
    }

    @ParameterizedTest
    @MethodSource("comparisonOperatorTestCases")
    void shouldHandleComparisonOperators(String operator) throws JSQLParserException {
        String sql = String.format("""
        WITH pi_inputcte AS (
            SELECT subax.enrollment
            FROM analytics_enrollment_ur1edk5oe2n AS subax
            WHERE (
                SELECT relationship_count
                FROM analytics_rs_relationship arr
                WHERE arr.trackedentityid = subax.trackedentity
            ) %s 10
        )
        SELECT * FROM analytics_enrollment_ur1edk5oe2n;
        """, operator);

        assertCteDecomposition(sql, 1, "relationship_count");
    }

    static Stream<String> comparisonOperatorTestCases() {
        return Stream.of(">", ">=", "<", "<=", "=", "!=");
    }

    @ParameterizedTest
    @MethodSource("logicalOperatorTestCases")
    void shouldHandleLogicalOperators(String operator) throws JSQLParserException {
        String sql = String.format("""
        WITH pi_inputcte AS (
            SELECT subax.enrollment
            FROM analytics_enrollment_ur1edk5oe2n AS subax
            WHERE (
                SELECT scheduleddate
                FROM analytics_event_ur1edk5oe2n
                WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND scheduleddate IS NOT NULL
                ORDER BY occurreddate DESC
                LIMIT 1
            ) IS NOT NULL
            %s (
                SELECT "de_value"
                FROM analytics_event_ur1edk5oe2n
                WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "de_value" IS NOT NULL
                AND ps = 'stage1'
                ORDER BY occurreddate DESC
                LIMIT 1
            ) IS NOT NULL
        )
        SELECT * FROM analytics_enrollment_ur1edk5oe2n;
        """, operator);

        assertCteDecomposition(sql, 2, "last_sched", "last_value_");
    }

    static Stream<String> logicalOperatorTestCases() {
        return Stream.of("AND", "OR");
    }

    @ParameterizedTest
    @MethodSource("specialOperatorTestCases")
    void shouldHandleSpecialOperators(SpecialOperatorTestCase testCase) throws JSQLParserException {
        assertCteDecomposition(testCase.sql(), testCase.expectedCtes(), testCase.expectedPatterns());
    }

    static Stream<SpecialOperatorTestCase> specialOperatorTestCases() {
        return Stream.of(
                // IS NULL case
                new SpecialOperatorTestCase(
                        """
                        WITH pi_inputcte AS (
                            SELECT subax.enrollment
                            FROM analytics_enrollment_ur1edk5oe2n AS subax
                            WHERE (
                                SELECT scheduleddate
                                FROM analytics_event_ur1edk5oe2n
                                WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                AND scheduleddate IS NOT NULL
                                ORDER BY occurreddate DESC
                                LIMIT 1
                            ) IS NOT NULL
                        )
                        SELECT * FROM analytics_enrollment_ur1edk5oe2n;
                        """,
                        1,
                        new String[]{"last_sched"}
                ),
                // IN case
                new SpecialOperatorTestCase(
                        """
                        WITH pi_inputcte AS (
                            SELECT subax.enrollment
                            FROM analytics_enrollment_ur1edk5oe2n AS subax
                            WHERE subax.enrollment IN (
                                SELECT scheduleddate
                                FROM analytics_event_ur1edk5oe2n
                                WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                AND scheduleddate IS NOT NULL
                                ORDER BY occurreddate DESC
                                LIMIT 1
                            )
                        )
                        SELECT * FROM analytics_enrollment_ur1edk5oe2n;
                        """,
                        1,
                        new String[]{"last_sched"}
                ),
                // BETWEEN case
                new SpecialOperatorTestCase(
                        """
                        WITH pi_inputcte AS (
                            SELECT subax.enrollment
                            FROM analytics_enrollment_ur1edk5oe2n AS subax
                            WHERE 5 BETWEEN (
                                SELECT relationship_count
                                FROM analytics_rs_relationship arr
                                WHERE arr.trackedentityid = subax.trackedentity
                            ) AND 10
                        )
                        SELECT * FROM analytics_enrollment_ur1edk5oe2n;
                        """,
                        1,
                        new String[]{"relationship_count"}
                )
        );
    }

    private record SpecialOperatorTestCase(String sql, int expectedCtes, String[] expectedPatterns) {}

    private void assertCteDecomposition(String sql, int expectedCtes, String... expectedPatterns)
            throws JSQLParserException {
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        WithItem withItem = select.getWithItemsList().get(0);
        CteDecomposer decomposer = new CteDecomposer();
        DecomposedCtes result = decomposer.processCTE(List.of(withItem));

        assertNotNull(result, "Result should not be null");
        assertEquals(expectedCtes, result.ctes().size(),
                "Should find " + expectedCtes + " subqueries");

        // Verify the specific patterns were found
        List<String> cteNames = result.ctes().stream()
                .map(GeneratedCte::name)
                .toList();

        for (String pattern : expectedPatterns) {
            assertTrue(
                    cteNames.stream().anyMatch(name -> name.equals(pattern) || name.startsWith(pattern)),
                    "Should contain pattern: " + pattern
            );
        }
    }
}