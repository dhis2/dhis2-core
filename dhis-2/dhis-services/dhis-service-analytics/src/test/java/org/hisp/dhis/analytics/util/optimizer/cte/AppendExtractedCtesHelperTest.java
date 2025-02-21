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

import static org.hisp.dhis.analytics.util.optimizer.cte.AppendExtractedCtesHelper.appendExtractedCtes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppendExtractedCtesHelperTest {
  private Select baseSelect;

  @BeforeEach
  void setUp() throws JSQLParserException {
    // Create a basic select statement for testing
    baseSelect = (Select) CCJSqlParserUtil.parse("SELECT * FROM test_table");
  }

  @Test
  void appendExtractedCtes_SingleCte_NoExistingWith() {

    Map<String, GeneratedCte> generatedCtes = new HashMap<>();
    generatedCtes.put(
        "cte1", new GeneratedCte("cte1", "SELECT id, name FROM source_table", "join_alias"));

    appendExtractedCtes(baseSelect, generatedCtes);

    assertNotNull(baseSelect.getWithItemsList());
    assertEquals(1, baseSelect.getWithItemsList().size());
    assertEquals("cte1", baseSelect.getWithItemsList().get(0).getName());
  }

  @Test
  void appendExtractedCtes_WithExistingCtes() throws JSQLParserException {

    Select selectWithCte =
        (Select)
            CCJSqlParserUtil.parse(
                "WITH existing_cte AS (SELECT * FROM existing_table) SELECT * FROM test_table");

    Map<String, GeneratedCte> generatedCtes = new HashMap<>();
    generatedCtes.put(
        "new_cte", new GeneratedCte("new_cte", "SELECT id FROM new_table", "join_alias"));

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
  void appendExtractedCtes_InvalidSqlSyntax() {

    Map<String, GeneratedCte> generatedCtes = new HashMap<>();
    generatedCtes.put(
        "invalid_cte", new GeneratedCte("invalid_cte", "INVALID SQL SYNTAX", "join_alias"));

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> appendExtractedCtes(baseSelect, generatedCtes));
    assertEquals(ErrorCode.E7149, exception.getErrorCode());
  }

  @Test
  void appendExtractedCtes_ComplexCte() {

    Map<String, GeneratedCte> generatedCtes = new HashMap<>();
    String complexQuery =
        """
                SELECT t1.id, t2.name, COUNT(*) as count
                FROM table1 t1
                JOIN table2 t2 ON t1.id = t2.id
                GROUP BY t1.id, t2.name
                HAVING COUNT(*) > 1
                """;

    generatedCtes.put("complex_cte", new GeneratedCte("complex_cte", complexQuery, "join_alias"));

    appendExtractedCtes(baseSelect, generatedCtes);

    // Assert
    assertNotNull(baseSelect.getWithItemsList());
    assertEquals(1, baseSelect.getWithItemsList().size());
    assertEquals("complex_cte", baseSelect.getWithItemsList().get(0).getName());
  }

  @Test
  void appendExtractedCtes_MultipleCtes() {

    Map<String, GeneratedCte> generatedCtes = new HashMap<>();
    generatedCtes.put("cte1", new GeneratedCte("cte1", "SELECT id FROM table1", "join_alias1"));
    generatedCtes.put("cte2", new GeneratedCte("cte2", "SELECT name FROM table2", "join_alias2"));

    appendExtractedCtes(baseSelect, generatedCtes);

    assertEquals(2, baseSelect.getWithItemsList().size());
    // Note: The order might depend on HashMap iteration order
    assertTrue(
        baseSelect.getWithItemsList().stream()
            .map(WithItem::getName)
            .anyMatch(name -> name.equals("cte1")));
    assertTrue(
        baseSelect.getWithItemsList().stream()
            .map(WithItem::getName)
            .anyMatch(name -> name.equals("cte2")));
  }
}
