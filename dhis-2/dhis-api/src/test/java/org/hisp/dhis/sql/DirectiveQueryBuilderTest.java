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
package org.hisp.dhis.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DirectiveQueryBuilder}. Each test assembles a query via {@link SQL#spy} and
 * asserts on the exact produced SQL. Tests are grouped by directive so the directive vocabulary is
 * easy to audit.
 */
class DirectiveQueryBuilderTest {

  // ============================== Harness ==============================

  private static String fetchSql(String template) {
    return fetchSql(template, b -> {});
  }

  private static String fetchSql(String template, Consumer<DirectiveQueryBuilder> configure) {
    List<String> captured = new ArrayList<>();
    DirectiveQueryBuilder b = SQL.ofDirective(template, SQL.spy(captured::add, (n, p) -> {}));
    configure.accept(b);
    assertNotNull(b.stream().toList()); // capture
    assertEquals(1, captured.size());
    return captured.get(0);
  }

  private static String countSql(String template, Consumer<DirectiveQueryBuilder> configure) {
    List<String> captured = new ArrayList<>();
    DirectiveQueryBuilder b = SQL.ofDirective(template, SQL.spy(captured::add, (n, p) -> {}));
    configure.accept(b);
    b.count();
    assertEquals(1, captured.size());
    return captured.get(0);
  }

  // ============================== No directives ==============================

  @Nested
  class NoDirectives {

    @Test
    void plainSqlIsUnchanged() {
      assertEquals("SELECT *\nFROM x\n", fetchSql("SELECT *\nFROM x"));
    }

    @Test
    void ordinaryCommentsAreKept() {
      assertEquals(
          "SELECT *\n-- ordinary comment\nFROM x\n",
          fetchSql("SELECT *\n-- ordinary comment\nFROM x"));
    }

    @Test
    void multipleOrdinaryLinesArePreservedIncludingBlankOnes() {
      String sql = "SELECT *\n\nFROM x\n\nWHERE y = 1";
      assertEquals(sql + "\n", fetchSql(sql));
    }
  }

  // ============================== @if ==============================

  @Nested
  class If {

    private static final String TEMPLATE =
        """
        -- @if :flag
        SELECT 1
        -- @end
        """;

    @Test
    void trueEmitsBlock() {
      assertEquals("SELECT 1\n", fetchSql(TEMPLATE, b -> b.setCondition("flag", true)));
    }

    @Test
    void falseErasesBlock() {
      assertTrue(fetchSql(TEMPLATE, b -> b.setCondition("flag", false)).isEmpty());
    }

    @Test
    void unsetConditionErasesBlock() {
      assertTrue(fetchSql(TEMPLATE).isEmpty());
    }

    @Test
    void boundNonNullParameterIsTruthy() {
      assertEquals(
          "AND x = :name\n",
          fetchSql("-- @if :name\nAND x = :name\n-- @end", b -> b.setParameter("name", "abc")));
    }

    @Test
    void nullBoundParameterIsFalsy() {
      assertTrue(
          fetchSql(
                  "-- @if :name\nAND x = :name\n-- @end",
                  b -> b.setParameter("name", (String) null))
              .isEmpty());
    }

    @Test
    void emptyArrayParameterIsFalsy() {
      assertTrue(
          fetchSql("-- @if :ids\nAND x IN (:ids)\n-- @end", b -> b.setParameter("ids", new long[0]))
              .isEmpty());
    }

    @Test
    void nonEmptyArrayParameterIsTruthy() {
      assertEquals(
          "AND x IN (:ids)\n",
          fetchSql("-- @if :ids\nAND x IN (:ids)\n-- @end", b -> b.setParameter("ids", "a", "b")));
    }
  }

  // ============================== @else ==============================

  @Nested
  class Else {

    private static final String TEMPLATE =
        """
        -- @if :flag
        A
        -- @else
        B
        -- @end
        """;

    @Test
    void trueEmitsFirstBranch() {
      assertEquals("A\n", fetchSql(TEMPLATE, b -> b.setCondition("flag", true)));
    }

    @Test
    void falseEmitsSecondBranch() {
      assertEquals("B\n", fetchSql(TEMPLATE, b -> b.setCondition("flag", false)));
    }

    @Test
    void duplicateElseFails() {
      String template =
          """
          -- @if :a
          A
          -- @else
          B
          -- @else
          C
          -- @end
          """;
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql(template));
      assertEquals(
          "@else block did not close with @end but with @else: -- @if :a", ex.getMessage());
    }
  }

  // ============================== Nesting ==============================

  @Nested
  class Nesting {

    private static final String TEMPLATE =
        """
        -- @if :outer
        O
        -- @if :inner
        I
        -- @end
        O2
        -- @end
        """;

    @Test
    void bothTrueEmitsEverything() {
      assertEquals(
          "O\nI\nO2\n",
          fetchSql(TEMPLATE, b -> b.setCondition("outer", true).setCondition("inner", true)));
    }

    @Test
    void outerTrueInnerFalseEmitsOnlyOuter() {
      assertEquals(
          "O\nO2\n",
          fetchSql(TEMPLATE, b -> b.setCondition("outer", true).setCondition("inner", false)));
    }

    @Test
    void outerFalseSuppressesInnerRegardless() {
      assertTrue(
          fetchSql(TEMPLATE, b -> b.setCondition("outer", false).setCondition("inner", true))
              .isEmpty());
    }
  }

  // ============================== @end --@if ==============================

  @Nested
  class EndifIf {

    private static final String TEMPLATE =
        """
        -- @if :a
        A
        -- @end --@if :b
        B
        -- @end
        """;

    @Test
    void firstTrueEmitsFirstBlock() {
      assertEquals("A\n", fetchSql(TEMPLATE, b -> b.setCondition("a", true)));
    }

    @Test
    void secondTrueEmitsSecondBlock() {
      assertEquals("B\n", fetchSql(TEMPLATE, b -> b.setCondition("b", true)));
    }

    @Test
    void bothTrueEmitsBothBlocks() {
      assertEquals(
          "A\nB\n", fetchSql(TEMPLATE, b -> b.setCondition("a", true).setCondition("b", true)));
    }

    @Test
    void neitherTrueEmitsNothing() {
      assertTrue(fetchSql(TEMPLATE).isEmpty());
    }

    @Test
    void chainOfThree() {
      String template =
          """
          -- @if :a
          A
          -- @end --@if :b
          B
          -- @end --@if :c
          C
          -- @end
          """;
      assertEquals("C\n", fetchSql(template, b -> b.setCondition("c", true)));
    }

    @Test
    void endifFollowedByEmbeddedIfArgFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  fetchSql(
                      """
          -- @if :a
          A
          -- @if :b --@if :c
          B
          -- @end
          """));
      assertEquals("@if block did not close with @end but with null: -- @if :a", ex.getMessage());
    }
  }

  // ============================== @replace ==============================

  @Nested
  class Insert {

    private static final String TEMPLATE =
        """
        A
        -- @insert :mid
        B
        """;

    @Test
    void emitsSubstitutionWhenSet() {
      assertEquals("A\nX\nB\n", fetchSql(TEMPLATE, b -> b.setInsert("mid", "X")));
    }

    @Test
    void erasesLineWhenUnset() {
      assertEquals("A\nB\n", fetchSql(TEMPLATE));
    }

    @Test
    void erasesLineWhenSetToEmptyString() {
      assertEquals("A\nB\n", fetchSql(TEMPLATE, b -> b.setInsert("mid", "")));
    }

    @Test
    void insideFalseIfIsAlsoErased() {
      String template =
          """
          -- @if :flag
          -- @insert :mid
          -- @end
          """;
      assertTrue(fetchSql(template, b -> b.setInsert("mid", "X")).isEmpty());
    }
  }

  // ============================== @count ==============================

  @Nested
  class Replace {

    private static final String TEMPLATE =
        """
        SELECT
        -- @replace :count count(*)
          a, b
        -- @end
        FROM x
        """;

    @Test
    void markersAreDroppedInFetchMode() {
      assertEquals("SELECT\n  a, b\nFROM x\n", fetchSql(TEMPLATE));
    }

    @Test
    void markersInsertSelectListInCountMode() {
      assertEquals("SELECT\ncount(*)\nFROM x\n", countSql(TEMPLATE, b -> {}));
    }

    @Test
    void oddNumberOfMarkersFailsInCountMode() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> countSql("SELECT\n-- @replace :count count(*)\n  a, b\nFROM x\n", b -> {}));
      assertEquals(
          "@replace block did not close with @end but with null: -- @replace :count count(*)",
          ex.getMessage());
    }

    @Test
    void replaceCanBeNestedWithinIf() {
      String template =
          """
          -- @if :flag
          -- @replace :count count(*)
          A
          -- @end
          -- @end
          SELECT 1
          """;
      assertEquals("count(*)\nSELECT 1\n", countSql(template, b -> b.setCondition("flag", true)));
    }
  }

  // ============================== @reorder ==============================

  @Nested
  class Reorder {

    private static final String TEMPLATE =
        """
        SELECT *
        FROM x
        ORDER BY
        -- @reorder :name
        x.name
        -- @end
        -- @reorder :created
        x.created
        -- @end
        """;

    @Test
    void emitsAllBlocksInTemplateOrderWhenUnset() {
      assertEquals("SELECT *\nFROM x\nORDER BY\nx.name\nx.created\n", fetchSql(TEMPLATE));
    }

    @Test
    void sortsByPosition() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nx.name\n",
          fetchSql(TEMPLATE, b -> b.setOrderBy("name", 2).setOrderBy("created", 1)));
    }

    @Test
    void tiesBreakByTemplateOrder() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\nx.created\n",
          fetchSql(TEMPLATE, b -> b.setOrderBy("created", 1).setOrderBy("name", 1)));
    }

    @Test
    void fixedDirectionInTemplate() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name DESC
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name DESC\n",
          fetchSql(template, b -> b.setOrderBy("name", 1)));
    }

    @Test
    void dynamicDirectionViaInsert() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :created
          x.created
          -- @insert :createdDir
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nDESC\n",
          fetchSql(template, b -> b.setOrderBy("created", 1).setInsert("createdDir", "DESC")));
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nASC\n",
          fetchSql(template, b -> b.setOrderBy("created", 1).setInsert("createdDir", "ASC")));
    }

    @Test
    void orderSectionDroppedWhenWrappedInFalseIf() {
      String template =
          """
          SELECT *
          FROM x
          -- @if :sorted
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          -- @reorder :created
          x.created
          -- @end
          -- @end
          """;
      assertEquals("SELECT *\nFROM x\n", fetchSql(template));
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\nx.created\n",
          fetchSql(template, b -> b.setCondition("sorted", true)));
    }

    @Test
    void unknownOrderNameDoesNotAffectOutput() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\nx.created\n",
          fetchSql(TEMPLATE, b -> b.setOrderBy("doesNotExist", 1)));
    }

    @Test
    void reorderInsideTrueIfIsSorted() {
      String template =
          """
          -- @if :sorted
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          -- @reorder :created
          x.created
          -- @end
          -- @end
          """;
      assertEquals(
          "ORDER BY\nx.created\nx.name\n",
          fetchSql(
              template,
              b -> b.setCondition("sorted", true).setOrderBy("name", 2).setOrderBy("created", 1)));
    }

    @Test
    void unclosedReorderFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @reorder :name\nx.name"));
      assertEquals(
          "@reorder block 'name' did not close with @end but with null: -- @reorder :name",
          ex.getMessage());
    }

    @Test
    void reorderWithoutArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @reorder\nx"));
      assertEquals("@reorder requires ':name' argument: -- @reorder", ex.getMessage());
    }
  }

  // ============================== setOrderBy helpers ==============================

  @Nested
  class SetOrderByHelpers {

    record OrderBy(String field, boolean asc) {}

    // ---------------- setOrderBy(CharSequence...) ----------------

    @Test
    void varargsOrdersBlocksByArgumentOrder() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          -- @reorder :created
          x.created
          -- @end
          -- @reorder :updated
          x.updated
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.updated\nx.name\nx.created\n",
          fetchSql(template, b -> b.setOrderBy("updated", "name", "created")));
    }

    @Test
    void varargsSingleNameKeepsBlockAtPositionZero() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          -- @reorder :created
          x.created
          -- @end
          """;
      // 'name' at position 0, 'created' has no entry → MAX_VALUE → last.
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\nx.created\n",
          fetchSql(template, b -> b.setOrderBy("name")));
    }

    @Test
    void varargsEmptyIsNoOp() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\n",
          fetchSql(template, DirectiveQueryBuilder::setOrderBy));
    }

    // ---------------- setOrderBy(List, Function, Predicate) ----------------

    @Test
    void listOrdersByListIndexNotTemplateOrder() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @insert :name
          -- @end
          -- @reorder :created
          x.created
          -- @insert :created
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nx.name\n",
          fetchSql(
              template,
              b ->
                  b.setOrderBy(
                      List.of(new OrderBy("created", true), new OrderBy("name", true)),
                      OrderBy::field,
                      OrderBy::asc)));
    }

    @Test
    void listDescendingRegistersDescInsert() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :created
          x.created
          -- @insert :created
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nDESC\n",
          fetchSql(
              template,
              b ->
                  b.setOrderBy(
                      List.of(new OrderBy("created", false)), OrderBy::field, OrderBy::asc)));
    }

    @Test
    void listMixedDirectionsOnlyDescendingGetInsert() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @insert :name
          -- @end
          -- @reorder :created
          x.created
          -- @insert :created
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nDESC\nx.name\n",
          fetchSql(
              template,
              b ->
                  b.setOrderBy(
                      List.of(new OrderBy("created", false), new OrderBy("name", true)),
                      OrderBy::field,
                      OrderBy::asc)));
    }

    @Test
    void listLeavesUnlistedBlocksAtEnd() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          -- @reorder :created
          x.created
          -- @end
          """;
      // Only 'created' gets a position; 'name' keeps MAX_VALUE and sorts to the end.
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created\nx.name\n",
          fetchSql(
              template,
              b ->
                  b.setOrderBy(
                      List.of(new OrderBy("created", true)), OrderBy::field, OrderBy::asc)));
    }

    @Test
    void listEmptyIsNoOp() {
      String template =
          """
          SELECT *
          FROM x
          ORDER BY
          -- @reorder :name
          x.name
          -- @end
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\n",
          fetchSql(template, b -> b.setOrderBy(List.<OrderBy>of(), OrderBy::field, OrderBy::asc)));
    }
  }

  // ============================== Parameters vs. conditions ==============================

  @Nested
  class ParametersAndConditions {

    @Test
    void boundParametersReachTheDriver() {
      List<String> sqls = new ArrayList<>();
      Map<String, SQL.Param> sent = new LinkedHashMap<>();
      SQL.QueryAPI api = SQL.spy(sqls::add, sent::put);

      SQL
          .ofDirective("SELECT * FROM x WHERE a = :a\n-- @if :flag\nAND b = 1\n-- @end", api)
          .setParameter("a", "hello")
          .setCondition("flag", true)
          .stream()
          .toList();

      assertEquals(Set.of("a"), sent.keySet());
      assertEquals("hello", sent.get("a").value());
    }

    @Test
    void conditionsAreNotSentToTheDriver() {
      List<String> sqls = new ArrayList<>();
      Map<String, SQL.Param> sent = new LinkedHashMap<>();
      SQL.QueryAPI api = SQL.spy(sqls::add, sent::put);

      SQL
          .ofDirective("SELECT 1\n-- @if :flag\nWHERE 1=1\n-- @end", api)
          .setCondition("flag", true)
          .stream()
          .toList();

      assertTrue(sent.isEmpty());
    }

    @Test
    void setConditionOverridesPreviousParameterOfSameName() {
      assertTrue(
          fetchSql("-- @if :x\nA\n-- @end", b -> b.setParameter("x", "v").setCondition("x", false))
              .isEmpty());
    }

    @Test
    void setParameterOverridesPreviousConditionOfSameName() {
      assertEquals(
          "A\n",
          fetchSql(
              "-- @if :x\nA\n-- @end", b -> b.setCondition("x", false).setParameter("x", "v")));
    }

    @Test
    void isNullParameterIsTrueForNullBoundParam() {
      DirectiveQueryBuilder b = SQL.ofDirective("SELECT 1", SQL.spy(s -> {}, (n, p) -> {}));
      b.setParameter("x", (String) null);
      assertTrue(b.isNullParameter("x"));
    }

    @Test
    void isNullParameterIsFalseForSetCondition() {
      DirectiveQueryBuilder b = SQL.ofDirective("SELECT 1", SQL.spy(s -> {}, (n, p) -> {}));
      b.setCondition("x", true);
      assertFalse(b.isNullParameter("x"));
    }
  }

  // ============================== Structural errors ==============================

  @Nested
  class StructuralErrors {

    @Test
    void unclosedIfFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @if :x\nA"));
      assertEquals("@if block did not close with @end but with null: -- @if :x", ex.getMessage());
    }

    @Test
    void orphanElseFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @else\nA"));
      assertEquals("Closing @end is missing for: @else", ex.getMessage());
    }

    @Test
    void orphanEndifFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @end\nA"));
      assertEquals("@end without opening directive", ex.getMessage());
    }

    @Test
    void unknownDirectiveFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @bogus\nA"));
      assertTrue(ex.getMessage().contains("Unknown directive @bogus"));
    }

    @Test
    void ifWithoutArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @if\nA\n-- @end"));
      assertTrue(ex.getMessage().contains("requires ':name' argument"));
    }

    @Test
    void ifWithNonColonArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @if flag\nA\n-- @end"));
      assertEquals("@if requires ':name' argument: -- @if flag", ex.getMessage());
    }

    @Test
    void elseWithArgumentFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> fetchSql("-- @if :a\nA\n-- @else :x\nB\n-- @end"));
      assertEquals("@else takes no argument: -- @else :x", ex.getMessage());
    }

    @Test
    void insertWithoutArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @insert\nA"));
      assertTrue(ex.getMessage().contains("@insert requires ':name' argument"));
    }
  }
}
