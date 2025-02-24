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
        """
            with pi_inputcte as (
                select subax.enrollment
                from analytics_enrollment_ur1edk5oe2n as subax
                where (
                    select scheduleddate
                    from analytics_event_ur1edk5oe2n
                    where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    and scheduleddate is not null
                    order by occurreddate desc
                    limit 1
                ) is not null
            )
            select * from pi_inputcte;
            """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(1, withItems.size(), "Should find one qualifying WithItem");
    assertEquals(
        "pi_inputcte", withItems.get(0).getName(), "The WithItem should have the correct name");
  }

  @Test
  void testFindQualifyingPiCtes_NoPiCte() throws Exception {
    String sql =
        """
            with regular_cte as (
                select subax.enrollment
                from analytics_enrollment_ur1edk5oe2n as subax
                where (
                    select scheduleddate
                    from analytics_event_ur1edk5oe2n
                    where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    and scheduleddate is not null
                    order by occurreddate desc
                    limit 1
                ) is not null
            )
            select * from regular_cte;
            """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should find no WithItems (no pi_ CTE)");
  }

  @Test
  void testFindQualifyingPiCtes_NoWithClause() throws Exception {
    String sql =
        """
            select * from employees;
            """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should return an empty list when there's no WITH clause");
  }

  @Test
  void testFindQualifyingPiCtes_SubqueryInSelectOfPiCte() throws Exception {
    String sql =
        """
            with pi_inputcte as (
                select subax.enrollment,
                       (select scheduleddate
                        from analytics_event_ur1edk5oe2n
                        where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                        and scheduleddate is not null
                        order by occurreddate desc
                        limit 1) as some_alias
                from analytics_enrollment_ur1edk5oe2n as subax
            )
            select * from pi_inputcte;
            """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertEquals(1, withItems.size(), "Should find one qualifying WithItem with SELECT subquery");
    assertEquals(
        "pi_inputcte", withItems.get(0).getName(), "Correct WithItem should be identified");
  }

  @Test
  void testFindQualifyingPiCtes_MultiplePiCtes_OneQualifying() throws Exception {
    String sql =
        """
        with pi_first as (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n as subax
            where (
                select scheduleddate
                from analytics_event_ur1edk5oe2n
                where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                and scheduleddate is not null
                order by occurreddate desc
                limit 1
            ) is not null
        ), pi_second as (
            select subax.id
            from another_table as subax
        ), non_pi_cte as (
            select * from some_table
        )
        select * from pi_first, pi_second;
        """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(1, withItems.size(), "Should find only one qualifying WithItem");
    assertEquals("pi_first", withItems.get(0).getName(), "Correct WithItem should be identified");
  }

  @Test
  void testFindQualifyingPiCtes_SubqueryInWhereOfAnotherCte() throws Exception {
    String sql =
        """
        with pi_inputcte as (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n as subax
        ), another_cte as (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n as subax
            where (
                select scheduleddate
                from analytics_event_ur1edk5oe2n
                where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                and scheduleddate is not null
                order by occurreddate desc
                limit 1
            ) is not null
        )
        select * from pi_inputcte;
        """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(0, withItems.size(), "Should find zero WithItems (wrong CTE)");
  }

  @Test
  void testFindQualifyingPiCtes_NoCorrelatedSubquery() throws Exception {
    String sql =
        """
        with pi_inputcte as (
            select enrollment
            from analytics_enrollment_ur1edk5oe2n
            where (
                select scheduleddate
                from analytics_event_ur1edk5oe2n
                where enrollment = 'some_value'
                and scheduleddate is not null
                order by occurreddate desc
                limit 1
            ) is not null
        )
        select * from pi_inputcte;
        """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);
    assertTrue(withItems.isEmpty(), "Should find no WithItems (no correlation)");
  }

  @Test
  void testFindQualifyingPiCtes_MultiplePiCtes_AllQualifying() throws Exception {
    String sql =
        """
        with pi_first as (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n as subax
            where (
                select scheduleddate
                from analytics_event_ur1edk5oe2n
                where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                and scheduleddate is not null
                order by occurreddate desc
                limit 1
            ) is not null
        ), pi_second as (
            select subax.id
            from another_table as subax
            where (
                select date
                from yet_another_table
                where yet_another_table.id = subax.id
                and date is not null
                order by date desc
                limit 1
            ) is not null
        )
        select * from pi_first, pi_second;
        """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(2, withItems.size(), "Should find two qualifying WithItems");
    // You might want to add checks for the specific names, depending on your requirements
  }

  @Test
  void testFindQualifyingPiCtes_WithSubqueryInSelectClause() throws Exception {
    String sql =
        """
            with pi_select_subquery as (
                select
                    subax.enrollment,
                    (
                        select count(*)
                        from analytics_event_ur1edk5oe2n
                        where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    ) as event_count
                from analytics_enrollment_ur1edk5oe2n as subax
            )
            select * from pi_select_subquery;
            """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(1, withItems.size(), "Should find one qualifying WithItem with SELECT subquery");
    assertEquals(
        "pi_select_subquery", withItems.get(0).getName(), "Correct WithItem should be identified");
  }

  @Test
  void testFindQualifyingPiCtes_WithSubqueriesInBothClauses() throws Exception {
    String sql =
        """
            with pi_both_clauses as (
                select
                    subax.enrollment,
                    (
                        select avg(cast(`value` as DECIMAL))
                        from analytics_event_ur1edk5oe2n
                        where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                        and `value` is not null
                    ) as avg_value
                from analytics_enrollment_ur1edk5oe2n as subax
                where (
                    select count(*)
                    from analytics_event_ur1edk5oe2n
                    where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                ) > 0
            ), pi_no_subquery as (
                select enrollment, enrollmentdate
                from analytics_enrollment_ur1edk5oe2n
            )
            select * from pi_both_clauses, pi_no_subquery;
            """;

    Statement statement = CCJSqlParserUtil.parse(sql);
    List<WithItem> withItems = identifier.findQualifyingPiCtes(statement);

    assertEquals(
        1, withItems.size(), "Should find one qualifying WithItem with subqueries in both clauses");
    assertEquals(
        "pi_both_clauses", withItems.get(0).getName(), "Correct WithItem should be identified");
  }

  @Test
  void testFindQualifyingPiCtes_InvalidSQL() {
    String sql =
        """
            select * form invalid
            """;

    // Expect a JSQLParserException
    assertThrows(
        JSQLParserException.class,
        () -> {
          CCJSqlParserUtil.parse(sql);
        });
  }
}
