package org.hisp.dhis.analytics.util.vis;

import org.junit.jupiter.api.Test;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SubSelectMatchersTest {

    @Test
    void shouldMatchValidDataElementCountPattern() throws Exception {
        // given
        String sql = """
            SELECT count("fCXKBdc27Bt")
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
                AND "fCXKBdc27Bt" = 1
                AND ps = 'EPEcjy3FWmI'
            """;
        SubSelect subSelect = createSubSelect(sql);

        // when
        Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

        // then
        assertTrue(result.isPresent());
        FoundSubSelect found = result.get();
        assertEquals("de_count_fCXKBdc27Bt", found.name());
        assertEquals("count", found.columnReference());

        // verify metadata
        assertNotNull(found.metadata());
        assertEquals("fCXKBdc27Bt", found.metadata().get("dataElementId"));
        assertEquals("EPEcjy3FWmI", found.metadata().get("programStageId"));
        assertEquals("1", found.metadata().get("value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Missing count function
            """
            SELECT "fCXKBdc27Bt"
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
                AND "fCXKBdc27Bt" = 1
            """,
            // Wrong table name
            """
            SELECT count("fCXKBdc27Bt")
            FROM wrong_table
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
                AND "fCXKBdc27Bt" = 1
            """,
            // Missing enrollment condition
            """
            SELECT count("fCXKBdc27Bt")
            FROM analytics_event_ur1edk5oe2n
            WHERE "fCXKBdc27Bt" IS NOT NULL
                AND "fCXKBdc27Bt" = 1
            """,
            // Missing IS NOT NULL condition
            """
            SELECT count("fCXKBdc27Bt")
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" = 1
            """,
            // Missing value comparison
            """
            SELECT count("fCXKBdc27Bt")
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
            """,
            // Empty count parameter
            """
            SELECT count()
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
                AND "fCXKBdc27Bt" = 1
            """
    })
    void shouldNotMatchInvalidPatterns(String sql) throws Exception {
        // given
        SubSelect subSelect = createSubSelect(sql);

        // when
        Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleVariationsInWhereClauseOrder() throws Exception {
        // given
        String sql = """
            SELECT count("fCXKBdc27Bt")
            FROM analytics_event_ur1edk5oe2n
            WHERE ps = 'EPEcjy3FWmI'
                AND "fCXKBdc27Bt" = 1
                AND analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
            """;
        SubSelect subSelect = createSubSelect(sql);

        // when
        Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

        // then
        assertTrue(result.isPresent());
        assertEquals("de_count_fCXKBdc27Bt", result.get().name());
    }

    @Test
    void shouldHandleDifferentDataElementIds() throws Exception {
        // given
        String sql = """
            SELECT count("different123")
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "different123" IS NOT NULL
                AND "different123" = 1
                AND ps = 'EPEcjy3FWmI'
            """;
        SubSelect subSelect = createSubSelect(sql);

        // when
        Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

        // then
        assertTrue(result.isPresent());
        assertEquals("de_count_different123", result.get().name());
        assertEquals("different123", result.get().metadata().get("dataElementId"));
    }

    @Test
    void shouldHandleDifferentProgramStageIds() throws Exception {
        // given
        String sql = """
            SELECT count("fCXKBdc27Bt")
            FROM analytics_event_ur1edk5oe2n
            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                AND "fCXKBdc27Bt" IS NOT NULL
                AND "fCXKBdc27Bt" = 1
                AND ps = 'DifferentStage'
            """;
        SubSelect subSelect = createSubSelect(sql);

        // when
        Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

        // then
        assertTrue(result.isPresent());
        assertEquals("DifferentStage", result.get().metadata().get("programStageId"));
    }

    @Test
    void shouldHandleDifferentDataElementValues() throws Exception {
        // given
        String[] testQueries = {
                """
        SELECT count("fCXKBdc27Bt")
        FROM analytics_event_ur1edk5oe2n
        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
            AND "fCXKBdc27Bt" IS NOT NULL
            AND "fCXKBdc27Bt" = 1
        """,
                """
        SELECT count("fCXKBdc27Bt")
        FROM analytics_event_ur1edk5oe2n
        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
            AND "fCXKBdc27Bt" IS NOT NULL
            AND "fCXKBdc27Bt" = 0
        """,
                """
        SELECT count("fCXKBdc27Bt")
        FROM analytics_event_ur1edk5oe2n
        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
            AND "fCXKBdc27Bt" IS NOT NULL
            AND "fCXKBdc27Bt" = 'YES'
        """,
                """
        SELECT count("fCXKBdc27Bt")
        FROM analytics_event_ur1edk5oe2n
        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
            AND "fCXKBdc27Bt" IS NOT NULL
            AND "fCXKBdc27Bt" = 42
        """
        };

        for (String sql : testQueries) {
            // when
            SubSelect subSelect = createSubSelect(sql);
            Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

            // then
            assertTrue(result.isPresent(), "Should match pattern for query: " + sql);
            FoundSubSelect found = result.get();
            assertEquals("de_count_fCXKBdc27Bt", found.name());
            assertEquals("fCXKBdc27Bt", found.metadata().get("dataElementId"));
            assertNotNull(found.metadata().get("value"), "Value should be captured in metadata");
        }
    }

    @Test
    void shouldHandleWithoutProgramStage() throws Exception {
        // given
        String sql = """
        SELECT count("fCXKBdc27Bt")
        FROM analytics_event_ur1edk5oe2n
        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
            AND "fCXKBdc27Bt" IS NOT NULL
            AND "fCXKBdc27Bt" = 1
        """;
        SubSelect subSelect = createSubSelect(sql);

        // when
        Optional<FoundSubSelect> result = SubselectMatchers.matchesDataElementCountPattern(subSelect);

        // then
        assertTrue(result.isPresent());
        assertEquals("de_count_fCXKBdc27Bt", result.get().name());
        assertNull(result.get().metadata().get("programStageId"), "Program stage ID should be null");
    }

    private SubSelect createSubSelect(String sql) throws Exception {
        String wrappedSql = "SELECT * FROM (" + sql + ") AS t";
        net.sf.jsqlparser.statement.Statement stmt = CCJSqlParserUtil.parse(wrappedSql);
        return (SubSelect) ((net.sf.jsqlparser.statement.select.PlainSelect)
                ((net.sf.jsqlparser.statement.select.Select) stmt).getSelectBody())
                .getFromItem();
    }


}