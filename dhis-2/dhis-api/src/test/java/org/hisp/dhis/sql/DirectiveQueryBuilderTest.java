package org.hisp.dhis.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Unit tests for {@link DirectiveQueryBuilder}. Each test assembles a query via
 * {@link SQL#spy} and asserts on the exact produced SQL. Tests are grouped by directive so
 * the directive vocabulary is easy to audit.
 */
class DirectiveQueryBuilderTest {

  // ============================== Harness ==============================

  private static String fetchSql( String template) {
    return fetchSql(template, b -> {});
  }

  private static String fetchSql( String template, Consumer<DirectiveQueryBuilder> configure) {
    List<String> captured = new ArrayList<>();
    DirectiveQueryBuilder b = SQL.ofDirective(template, SQL.spy(captured::add, (n, p) -> {}));
    configure.accept(b);
    b.stream().toList();
    assertEquals(1, captured.size());
    return captured.get(0);
  }

  private static String countSql( String template, Consumer<DirectiveQueryBuilder> configure) {
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
          fetchSql("-- @if :name\nAND x = :name\n-- @end", b -> b.setParameter("name", (String) null))
              .isEmpty());
    }

    @Test
    void emptyArrayParameterIsFalsy() {
      assertTrue(
          fetchSql("-- @if :ids\nAND x IN (:ids)\n-- @end", b -> b.setParameter("ids", new long[0])).isEmpty());
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
      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fetchSql(template));
      assertEquals("@else block did not close with @end but with ELSE: -- @if :a", ex.getMessage());
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
      assertEquals("A\nB\n", fetchSql(TEMPLATE, b -> b.setCondition("a", true).setCondition("b", true)));
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
      assertEquals("@if block did not close with @end but with EOF: -- @if :a", ex.getMessage());
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
      assertEquals("@replace block did not close with @end but with EOF: -- @replace :count count(*)", ex.getMessage());
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
      assertEquals(
          "count(*)\nSELECT 1\n",
          countSql(template, b -> b.setCondition("flag", true)));
    }
  }

  // ============================== @order ==============================

  @Nested
  class Order {

    private static final String TEMPLATE =
        """
        SELECT *
        FROM x
        -- @order
        ORDER BY
        -- @order :name
        x.name
        -- @order :created
        x.created
        -- @order
        """;

    @Test
    void sectionIsDroppedWhenNoOrdersSelected() {
      assertEquals("SELECT *\nFROM x\n", fetchSql(TEMPLATE));
    }

    @Test
    void emitsSingleSelectedItem() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name\n", fetchSql(TEMPLATE, b -> b.setOrder("name", 1)));
    }

    @Test
    void sortsByPositionNotTemplateOrder() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created,\nx.name\n",
          fetchSql(TEMPLATE, b -> b.setOrder("name", 2).setOrder("created", 1)));
    }

    @Test
    void tiesBreakByTemplateOrder() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name,\nx.created\n",
          fetchSql(TEMPLATE, b -> b.setOrder("created", 1).setOrder("name", 1)));
    }

    @Test
    void templateDirectionWinsOverSetter() {
      String template =
          """
          SELECT *
          FROM x
          -- @order
          ORDER BY
          -- @order :created
          x.created DESC
          -- @order
          """;
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.created DESC\n",
          fetchSql(template, b -> b.setOrder("created", 1, true)));
    }

    @Test
    void setterDirectionAppliesWhenTemplateHasNone() {
      assertEquals(
          "SELECT *\nFROM x\nORDER BY\nx.name DESC\n",
          fetchSql(TEMPLATE, b -> b.setOrder("name", 1, false)));
    }

    @Test
    void unknownOrderNameIsIgnored() {
      assertEquals("SELECT *\nFROM x\n", fetchSql(TEMPLATE, b -> b.setOrder("doesNotExist", 1)));
    }

    @Test
    void duplicateItemNamesFail() {
      String template =
          """
          -- @order
          ORDER BY
          -- @order :name
          x.name
          -- @order :name
          y.name
          -- @order
          """;
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> fetchSql(template, b -> b.setOrder("name", 1)));
      assertTrue(ex.getMessage().contains("Duplicate @order :name"));
    }

    @Test
    void unclosedSectionFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> fetchSql("-- @order\nORDER BY\n-- @order :name\nx.name"));
      assertTrue(ex.getMessage().contains("Unclosed @order"));
    }

    @Test
    void orderItemOutsideSectionFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> fetchSql("-- @order :name\nx.name"));
      assertTrue(ex.getMessage().contains("outside @order section"));
    }

    @Test
    void nonOrderDirectiveInsideSectionFails() {
      String template =
          """
          -- @order
          ORDER BY
          -- @if :x
          x.name
          -- @end
          -- @order
          """;
      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fetchSql(template));
      assertTrue(ex.getMessage().contains("Only @order directives may appear inside @order"));
    }

    @Test
    void sectionInsideTrueIfIsEmitted() {
      String template =
          """
          -- @if :sort
          -- @order
          ORDER BY
          -- @order :name
          x.name
          -- @order
          -- @end
          SELECT 1
          """;
      assertEquals(
          "ORDER BY\nx.name\nSELECT 1\n",
          fetchSql(template, b -> b.setOrder("name", 1).setCondition("sort", true)));
    }

    @Test
    void sectionInsideFalseIfIsErased() {
      String template =
          """
          -- @if :sort
          -- @order
          ORDER BY
          -- @order :name
          x.name
          -- @order
          -- @end
          SELECT 1
          """;
      assertEquals("SELECT 1\n", fetchSql(template, b -> b.setOrder("name", 1)));
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

      SQL.ofDirective("SELECT * FROM x WHERE a = :a\n-- @if :flag\nAND b = 1\n-- @end", api)
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

      SQL.ofDirective("SELECT 1\n-- @if :flag\nWHERE 1=1\n-- @end", api)
          .setCondition("flag", true)
          .stream()
          .toList();

      assertTrue(sent.isEmpty());
    }

    @Test
    void setConditionOverridesPreviousParameterOfSameName() {
      assertTrue(
          fetchSql(
              "-- @if :x\nA\n-- @end",
              b -> b.setParameter("x", "v").setCondition("x", false))
              .isEmpty());
    }

    @Test
    void setParameterOverridesPreviousConditionOfSameName() {
      assertEquals(
          "A\n",
          fetchSql(
              "-- @if :x\nA\n-- @end",
              b -> b.setCondition("x", false).setParameter("x", "v")));
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
      assertEquals("@if block did not close with @end but with EOF: -- @if :x", ex.getMessage());
    }

    @Test
    void orphanElseFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @else\nA"));
      assertEquals("Unbalanced directive at top level: else", ex.getMessage());
    }

    @Test
    void orphanEndifFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> fetchSql("-- @end\nA"));
      assertTrue(ex.getMessage().contains("Unbalanced directive at top level"));
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
          assertThrows(
              IllegalStateException.class, () -> fetchSql("-- @if\nA\n-- @end"));
      assertTrue(ex.getMessage().contains("requires ':name' argument"));
    }

    @Test
    void ifWithNonColonArgumentFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> fetchSql("-- @if flag\nA\n-- @end"));
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