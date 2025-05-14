/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests cover: • supports(..) happy / negative • replacement rules for DATE, TEXT, NUMBER •
 * malformed placeholder is left untouched • alias map is populated
 */
@ExtendWith(MockitoExtension.class)
class VariableCteFactoryTest extends TestBase {

  private VariableCteFactory factory;

  @Mock private CteContext cteContext;
  @Mock private CteDefinition cteDefinition;
  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private final Map<String, String> aliasMap = new HashMap<>();

  @BeforeEach
  void setUp() {
    factory = new VariableCteFactory();
  }

  // ------------------------------------------------------------------
  // supports(..)
  // ------------------------------------------------------------------

  @Test
  void supports_returnsTrueWhenPatternPresent() {
    String sql = "select FUNC_CTE_VAR(type='x', column='c', piUid='PI', psUid='null', offset='0')";
    assertTrue(factory.supports(sql));
    assertFalse(factory.supports("select * from x"));
  }

  // ------------------------------------------------------------------
  // Replacement by variable type
  // ------------------------------------------------------------------

  @Nested
  class ReplacementPolicy {

    @Test
    void dateType_returnsDirectValue() {

      Program program = createProgram('A');
      ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

      when(cteDefinition.getAlias()).thenReturn("v1");
      when(cteContext.containsCte(anyString())).thenReturn(true);
      when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);

      String placeholder =
          "FUNC_CTE_VAR(type='vEventDate', column='occurreddate', piUid='PI', psUid='null', offset='0')";
      String rawSql = "select " + placeholder + " as col";

      String result =
          factory.process(
              rawSql, programIndicator, new Date(), new Date(), cteContext, aliasMap, sqlBuilder);

      assertEquals("select v1.value as col", result.trim());
      assertEquals("v1", aliasMap.get(placeholder));
    }

    @Test
    void textType_returnsCoalesceEmptyString() {
      Program program = createProgram('A');
      ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

      when(cteDefinition.getAlias()).thenReturn("v1");
      when(cteContext.containsCte(anyString())).thenReturn(true);
      when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);

      String placeholder =
          "FUNC_CTE_VAR(type='vTextAttribute', column='txt', piUid='PI', psUid='null', offset='0')";
      String rawSql = "select " + placeholder;

      String result =
          factory.process(
              rawSql, programIndicator, new Date(), new Date(), cteContext, aliasMap, sqlBuilder);

      assertTrue(result.contains("coalesce(v1.value, '')"));
    }

    @Test
    void numericDefault_returnsCoalesceZero() {
      Program program = createProgram('A');
      ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

      when(cteDefinition.getAlias()).thenReturn("v1");
      when(cteContext.containsCte(anyString())).thenReturn(true);
      when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);

      String placeholder =
          "FUNC_CTE_VAR(type='vSomeNumber', column='val', piUid='PI', psUid='null', offset='0')";
      String rawSql = "select " + placeholder;

      String result =
          factory.process(
              rawSql, programIndicator, new Date(), new Date(), cteContext, aliasMap, sqlBuilder);

      assertTrue(result.contains("coalesce(v1.value, 0)"));
    }
  }

  // ------------------------------------------------------------------
  // Error / malformed handling
  // ------------------------------------------------------------------

  @Test
  void malformedPlaceholder_isLeftUntouched() {
    String bad = "FUNC_CTE_VAR(type='vEventDate', column='col'"; // missing closing parts
    String sql = "select " + bad;
    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

    String out =
        factory.process(
            sql, programIndicator, new Date(), new Date(), cteContext, aliasMap, sqlBuilder);

    assertEquals(sql, out);
    assertTrue(aliasMap.isEmpty());
  }
}
