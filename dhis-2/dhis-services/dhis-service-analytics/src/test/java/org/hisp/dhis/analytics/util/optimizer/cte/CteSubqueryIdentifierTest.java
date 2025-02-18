/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.util.optimizer.cte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.analytics.util.optimizer.cte.pipeline.CteSubqueryIdentifier;
import org.junit.jupiter.api.Test;

class CteSubqueryIdentifierTest {

  private final CteSubqueryIdentifier identifier = new CteSubqueryIdentifier();

  @Test
  void testFindQualifyingPiCtes_SingleQualifyingCte() throws Exception {
    String sql =
        "WITH pi_inputcte AS (\n"
            + "    SELECT subax.enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + "    WHERE (\n"
            + "        SELECT scheduleddate\n"
            + "        FROM analytics_event_ur1edk5oe2n\n"
            + "        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment\n"
            + // Correct correlation
            "        AND scheduleddate IS NOT NULL\n"
            +
            // No pi_ function check needed here!
            "        ORDER BY occurreddate DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + ")\n"
            + "SELECT * FROM pi_inputcte;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(1, withItems.size(), "Should find one qualifying WithItem");
    assertEquals(
        "pi_inputcte", withItems.get(0).getName(), "The WithItem should have the correct name");
  }

  @Test
  void testFindQualifyingPiCtes_NoPiCte() throws Exception {
    String sql =
        "WITH regular_cte AS (\n"
            + // No pi_ prefix
            "    SELECT subax.enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + "    WHERE (\n"
            + "        SELECT scheduleddate\n"
            + "        FROM analytics_event_ur1edk5oe2n\n"
            + "        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment\n"
            + // Correct correlation
            "        AND scheduleddate IS NOT NULL\n"
            + "        ORDER BY occurreddate DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + ")\n"
            + "SELECT * FROM regular_cte;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should find no WithItems (no pi_ CTE)");
  }

  @Test
  void testFindQualifyingPiCtes_NoWithClause() throws Exception {
    String sql = "SELECT * FROM employees;"; // No WITH clause at all

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should return an empty list when there's no WITH clause");
  }

  @Test
  void testFindQualifyingPiCtes_SubqueryInSelectOfPiCte() throws Exception {
    String sql =
        "WITH pi_inputcte AS (\n"
            + "    SELECT subax.enrollment, \n"
            + "           (SELECT scheduleddate\n"
            + // Subquery in SELECT, not WHERE
            "            FROM analytics_event_ur1edk5oe2n\n"
            + "            WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment\n"
            + "            AND scheduleddate IS NOT NULL\n"
            + "            ORDER BY occurreddate DESC\n"
            + "            LIMIT 1) as some_alias\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + ")\n"
            + "SELECT * FROM pi_inputcte;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should find NO WithItems (subquery in SELECT)");
  }

  @Test
  void testFindQualifyingPiCtes_MultiplePiCtes_OneQualifying() throws Exception {
    String sql =
        "WITH pi_first AS (\n"
            + "    SELECT subax.enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + "    WHERE (\n"
            + "        SELECT scheduleddate\n"
            + "        FROM analytics_event_ur1edk5oe2n\n"
            + "        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment\n"
            + // Correct correlation
            "        AND scheduleddate IS NOT NULL\n"
            + "        ORDER BY occurreddate DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + // Qualifying CTE
            "), pi_second AS (\n"
            + "    SELECT subax.id\n"
            + "    FROM another_table AS subax\n"
            + // No WHERE clause
            "), non_pi_cte AS (\n"
            + "    SELECT * FROM some_table\n"
            + ")\n"
            + "SELECT * FROM pi_first, pi_second;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(1, withItems.size(), "Should find only one qualifying WithItem");
    assertEquals("pi_first", withItems.get(0).getName(), "Correct WithItem should be identified");
  }

  @Test
  void testFindQualifyingPiCtes_SubqueryInWhereOfAnotherCte() throws Exception {
    String sql =
        "WITH pi_inputcte AS (\n"
            + "    SELECT subax.enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + "), another_cte AS (\n"
            + "    SELECT subax.enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + "    WHERE (\n"
            + "        SELECT scheduleddate\n"
            + "        FROM analytics_event_ur1edk5oe2n\n"
            + "        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment\n"
            + // Correct correlation
            "        AND scheduleddate IS NOT NULL\n"
            + "        ORDER BY occurreddate DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + ")\n"
            + "SELECT * FROM pi_inputcte;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(0, withItems.size(), "Should find zero WithItems (wrong CTE)");
  }

  @Test
  void testFindQualifyingPiCtes_NoCorrelatedSubquery() throws Exception {
    String sql =
        "WITH pi_inputcte AS (\n"
            + "    SELECT enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n\n"
            + "    WHERE (\n"
            + "        SELECT scheduleddate\n"
            + "        FROM analytics_event_ur1edk5oe2n\n"
            + "        WHERE enrollment = 'some_value'\n"
            + // No correlation
            "        AND scheduleddate IS NOT NULL\n"
            + "        ORDER BY occurreddate DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + ")\n"
            + "SELECT * FROM pi_inputcte;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should find no WithItems (no correlation)");
  }

  @Test
  void testFindQualifyingPiCtes_MultiplePiCtes_AllQualifying() throws Exception {
    String sql =
        "WITH pi_first AS (\n"
            + "    SELECT subax.enrollment\n"
            + "    FROM analytics_enrollment_ur1edk5oe2n AS subax\n"
            + "    WHERE (\n"
            + "        SELECT scheduleddate\n"
            + "        FROM analytics_event_ur1edk5oe2n\n"
            + "        WHERE analytics_event_ur1edk5oe2n.enrollment = subax.enrollment\n"
            + // Correct correlation
            "        AND scheduleddate IS NOT NULL\n"
            + "        ORDER BY occurreddate DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + "), pi_second AS (\n"
            + "    SELECT subax.id\n"
            + "    FROM another_table AS subax\n"
            + "    WHERE (\n"
            + // Added WHERE clause with correlated subquery
            "        SELECT date\n"
            + "        FROM yet_another_table\n"
            + "        WHERE yet_another_table.id = subax.id\n"
            + // Correct correlation
            "        AND date IS NOT NULL\n"
            + "        ORDER BY date DESC\n"
            + "        LIMIT 1\n"
            + "    ) IS NOT NULL\n"
            + ")\n"
            + "SELECT * FROM pi_first, pi_second;";

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(2, withItems.size(), "Should find two qualifying WithItems");
    // You might want to add checks for the specific names, depending on your requirements
  }

  @Test
  void testFindQualifyingPiCtes_InvalidSQL() {
    String sql = "SELECT * FORM invalid"; // Invalid SQL

    // Expect a JSQLParserException
    assertThrows(
        JSQLParserException.class,
        () -> {
          CCJSqlParserUtil.parse(sql);
        });
  }
}
