/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.parser.expression.antlr.ExpressionBaseListener;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies that lifting simple V{...} comparisons out of a filter leaves a parseable remainder. */
@ExtendWith(MockitoExtension.class)
class FilterCteFactoryTest extends TestBase {

  private static final String DE_CONDITION = "isNotNull(#{StAgEuId001.DaTaElEm001})";
  private static final String STATUS_CONDITION = "V{event_status} == 'COMPLETED'";

  private FilterCteFactory factory;

  @Mock private CteContext cteContext;
  @Mock private CteDefinition cteDefinition;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();
  private final Map<String, String> aliasMap = new HashMap<>();
  private ProgramIndicator programIndicator;

  @BeforeEach
  void setUp() {
    factory = new FilterCteFactory();

    Program program = createProgram('A');
    programIndicator = createProgramIndicator('A', program, "1", "");
    programIndicator.setUid("PiUid000001");

    lenient().when(cteContext.containsCte(anyString())).thenReturn(false);
    lenient().when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);
    lenient().when(cteDefinition.getAlias()).thenReturn("fcte");
  }

  /** One comparison for every variable {@link FilterCteFactory} lifts into a filter CTE. */
  private static Stream<String> liftedComparisons() {
    return Stream.of(
        "V{event_status} == 'COMPLETED'",
        "V{event_date} >= '2024-01-01'",
        "V{creation_date} >= '2024-01-01'",
        "V{due_date} <= '2024-12-31'",
        "V{scheduled_date} != '2024-06-01'");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("liftedComparisons")
  void liftedComparisonAloneLeavesEmptyRemainder(String comparison) {
    assertEquals("", process(comparison));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("liftedComparisons")
  void liftedComparisonAfterOtherConditionLeavesParseableRemainder(String comparison) {
    String remaining = process(DE_CONDITION + " && " + comparison);

    assertEquals(DE_CONDITION + " && true", remaining);
    assertParseable(remaining);
  }

  @Test
  void repeatedStatusConditionsLeaveEmptyRemainder() {
    assertEquals("", process(STATUS_CONDITION + " && " + STATUS_CONDITION));
  }

  @Test
  void statusConditionBeforeOtherConditionLeavesParseableRemainder() {
    String remaining = process(STATUS_CONDITION + " && " + DE_CONDITION);

    assertEquals("true && " + DE_CONDITION, remaining);
    assertParseable(remaining);
  }

  @Test
  void statusConditionBetweenOtherConditionsLeavesParseableRemainder() {
    String remaining = process(DE_CONDITION + " && " + STATUS_CONDITION + " && " + DE_CONDITION);

    assertEquals(DE_CONDITION + " && true && " + DE_CONDITION, remaining);
    assertParseable(remaining);
  }

  @Test
  void wordOperatorLeavesParseableRemainder() {
    String remaining = process(DE_CONDITION + " and " + STATUS_CONDITION);

    assertEquals(DE_CONDITION + " and true", remaining);
    assertParseable(remaining);
  }

  @Test
  void filterWithoutSimpleComparisonIsUntouched() {
    String filter = "V{enrollment_status} == 'COMPLETED' && " + DE_CONDITION;

    assertEquals(filter, process(filter));
  }

  @ParameterizedTest
  @MethodSource("liftedComparisons")
  void comparisonsUnderOrOrNegationStayInTheExpression(String comparison) {
    for (String filter :
        new String[] {
          DE_CONDITION + " || " + comparison,
          comparison + " or " + DE_CONDITION,
          "!(" + comparison + ")",
          "not (" + comparison + ")"
        }) {
      assertEquals(filter, process(filter));
    }
    assertTrue(aliasMap.isEmpty());
    verifyNoInteractions(cteContext);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "V{event_status} == 'ACTIVE' || V{event_status} == 'COMPLETED'",
        "true || (V{event_status} == 'ACTIVE' && V{event_status} == 'COMPLETED')",
        "!(true && V{event_status} == 'COMPLETED')",
        "if(V{event_status} == 'COMPLETED', true, false)",
        "(V{event_status} == 'COMPLETED') == false",
        "\"V{event_status} == 'COMPLETED'\" == 'text'"
      })
  void comparisonsOutsideRequiredConjunctsAreUntouched(String filter) {
    assertEquals(filter, process(filter));
    assertTrue(aliasMap.isEmpty());
    verifyNoInteractions(cteContext);
  }

  @ParameterizedTest
  @ValueSource(strings = {"(true || false)", "not false", "'😀' == '😀'"})
  void unrelatedBooleanContextsDoNotPreventLifting(String condition) {
    String remaining = process(condition + " && (" + STATUS_CONDITION + ")");

    assertEquals(condition + " && (true)", remaining);
    assertEquals(Map.of(STATUS_CONDITION, "fcte"), aliasMap);
    assertParseable(remaining);
  }

  @Test
  void identicalComparisonsAreLiftedOnlyAtRequiredOccurrences() {
    String filter = STATUS_CONDITION + " && (" + STATUS_CONDITION + " || " + DE_CONDITION + ")";

    assertEquals("true && (" + STATUS_CONDITION + " || " + DE_CONDITION + ")", process(filter));
    assertEquals(Map.of(STATUS_CONDITION, "fcte"), aliasMap);
    verify(cteContext).addFilterCte(anyString(), anyString());
  }

  @Test
  void groupedConjunctionOfLiftedComparisonsLeavesEmptyRemainder() {
    assertEquals("", process("((" + STATUS_CONDITION + ") and (" + STATUS_CONDITION + "))"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "(V{event_status} == 'COMPLETED' && )",
        "V{event_status} == 'COMPLETED' ||",
        "V{event_status} == 'COMPLETED' && && true",
        "(V{event_status} == 'COMPLETED'",
        "V{event_status} == 'COMPLETED' AND true"
      })
  void invalidOriginalFiltersFailBeforeRegisteringCtes(String filter) {
    assertThrows(ParserException.class, () -> process(filter));
    assertTrue(aliasMap.isEmpty());
    verifyNoInteractions(cteContext);
  }

  private String process(String filter) {
    return factory.process(
        filter, programIndicator, new Date(), new Date(), cteContext, aliasMap, sqlBuilder);
  }

  private void assertParseable(String expression) {
    assertDoesNotThrow(
        () -> Parser.listen(expression, new ExpressionBaseListener()),
        () -> "Remaining filter is not a valid expression: '" + expression + "'");
  }
}
