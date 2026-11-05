/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 * ... (license header unchanged) ...
 */
package org.hisp.dhis.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.hisp.dhis.jsontree.Text;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DirectiveTemplate}. Each test renders an inline template and asserts
 * the exact output (or the exact error message). Templates are intentionally short so the
 * input/output pair is fully visible in the test body.
 */
class DirectiveTemplateTest {

  // ============================== Harness ==============================

  private static Predicate<Text> isDirective() {
    return line -> {
      String s = line.toString().stripLeading();
      return s.startsWith("--") && s.indexOf('@') >= 0;
    };
  }

  private static String render(String template) {
    return render(template, Set.of(), Map.of(), Map.of());
  }

  private static String render(String template, Set<String> trueConditions) {
    return render(template, trueConditions, Map.of(), Map.of());
  }

  private static String render(
      String template, Set<String> trueConditions, Map<String, String> inserts) {
    return render(template, trueConditions, inserts, Map.of());
  }

  private static String render(
      String template,
      Set<String> trueConditions,
      Map<String, String> inserts,
      Map<String, Integer> orders) {
    return new DirectiveTemplate(
            isDirective(),
            trueConditions::contains,
            name -> inserts.containsKey(name) ? Text.of(inserts.get(name)) : null,
            name -> orders.getOrDefault(name, 0))
        .render(Text.of(template))
        .toString();
  }

  // ============================== Pass-through ==============================

  @Nested
  class PassThrough {

    @Test
    void emptyTemplate() {
      assertEquals("", render(""));
    }

    @Test
    void plainTextIsUnchanged() {
      assertEquals("SELECT *\nFROM x\n", render("SELECT *\nFROM x"));
    }

    @Test
    void ordinarySqlCommentIsKept() {
      assertEquals("-- just a comment\nSELECT 1\n", render("-- just a comment\nSELECT 1"));
    }

    @Test
    void dashDashAtWithNoNameIsOrdinaryComment() {
      assertEquals("-- @\nSELECT 1\n", render("-- @\nSELECT 1"));
    }

    @Test
    void blankLinesArePreserved() {
      assertEquals("A\n\nB\n", render("A\n\nB"));
    }
  }

  // ============================== @insert ==============================

  @Nested
  class Insert {

    @Test
    void insertsRegisteredText() {
      assertEquals(
          "A\nINSERTED\nB\n",
          render("A\n-- @insert :x\nB", Set.of(), Map.of("x", "INSERTED")));
    }

    @Test
    void unknownNameEmitsNothing() {
      assertEquals("A\nB\n", render("A\n-- @insert :x\nB"));
    }

    @Test
    void emptyStringInsertEmitsBlankLine() {
      assertEquals("A\n\nB\n", render("A\n-- @insert :x\nB", Set.of(), Map.of("x", "")));
    }

    @Test
    void insertInsideFalseIfIsSkipped() {
      assertEquals(
          "",
          render(
              "-- @if :flag\n-- @insert :x\n-- @end",
              Set.of(),
              Map.of("x", "INSERTED")));
    }

    @Test
    void insertWithoutArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @insert"));
      assertEquals("@insert requires ':name' argument: -- @insert", ex.getMessage());
    }
  }

  // ============================== @if ==============================

  @Nested
  class If {

    @Test
    void trueEmitsBody() {
      assertEquals("A\n", render("-- @if :x\nA\n-- @end", Set.of("x")));
    }

    @Test
    void falseErasesBody() {
      assertEquals("", render("-- @if :x\nA\n-- @end"));
    }

    @Test
    void textBeforeAndAfterIsKept() {
      assertEquals("pre\nA\npost\n", render("pre\n-- @if :x\nA\n-- @end\npost", Set.of("x")));
    }

    @Test
    void nestedIfBothTrue() {
      assertEquals(
          "O\nI\n",
          render("-- @if :o\nO\n-- @if :i\nI\n-- @end\n-- @end", Set.of("o", "i")));
    }

    @Test
    void nestedIfOuterFalseSuppressesInner() {
      assertEquals(
          "", render("-- @if :o\nO\n-- @if :i\nI\n-- @end\n-- @end", Set.of("i")));
    }

    @Test
    void nestedIfOuterTrueInnerFalse() {
      assertEquals(
          "O\n", render("-- @if :o\nO\n-- @if :i\nI\n-- @end\n-- @end", Set.of("o")));
    }

    @Test
    void ifWithoutArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @if"));
      assertEquals("@if requires ':name' argument: -- @if", ex.getMessage());
    }

    @Test
    void unclosedIfFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @if :x\nA"));
      assertEquals("@if block did not close with @end but with null: -- @if :x", ex.getMessage());
    }
  }

  // ============================== @else ==============================

  @Nested
  class Else {

    @Test
    void trueEmitsFirstBranch() {
      assertEquals(
          "A\n", render("-- @if :x\nA\n-- @else\nB\n-- @end", Set.of("x")));
    }

    @Test
    void falseEmitsSecondBranch() {
      assertEquals("B\n", render("-- @if :x\nA\n-- @else\nB\n-- @end"));
    }

    @Test
    void nestedElsePairsWithInnermostIf() {
      assertEquals(
          "O\nI\n",
          render(
              "-- @if :o\nO\n-- @if :i\nI\n-- @else\nJ\n-- @end\n-- @end",
              Set.of("o", "i")));
      assertEquals(
          "O\nJ\n",
          render(
              "-- @if :o\nO\n-- @if :i\nI\n-- @else\nJ\n-- @end\n-- @end",
              Set.of("o")));
    }

    @Test
    void duplicateElseFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> render("-- @if :x\nA\n-- @else\nB\n-- @else\nC\n-- @end"));
      assertEquals(
          "@else block did not close with @end but with @else: -- @if :x", ex.getMessage());
    }

    @Test
    void elseWithArgumentFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> render("-- @else :x\nA"));
      assertEquals("@else takes no argument: -- @else :x", ex.getMessage());
    }
  }

  // ============================== @end chains ==============================

  @Nested
  class EndChains {

    @Test
    void endFollowedByIfOnSameLineChainsSiblings() {
      assertEquals(
          "B\n",
          render(
              "-- @if :a\nA\n-- @end --@if :b\nB\n-- @end",
              Set.of("b")));
    }

    @Test
    void chainOfThree() {
      String template =
          "-- @if :a\nA\n-- @end --@if :b\nB\n-- @end --@if :c\nC\n-- @end";
      assertEquals("C\n", render(template, Set.of("c")));
      assertEquals("A\nB\nC\n", render(template, Set.of("a", "b", "c")));
      assertEquals("", render(template));
    }

    @Test
    void endFollowedByElseIsElseOfParent() {
      String template = "-- @if :outer\n-- @if :a\nA\n-- @end --@else\nE\n-- @end";

      // outer=true, a=true: outer then, inner then → A emitted, else not taken
      assertEquals("A\n", render(template, Set.of("outer", "a")));

      // outer=true, a=false: outer then, inner not → nothing, else not taken (outer=true)
      assertEquals("", render(template, Set.of("outer")));

      // outer=false, a=true: outer else → E emitted (inner never processed)
      assertEquals("E\n", render(template, Set.of("a")));

      // outer=false, a=false: outer else → E emitted
      assertEquals("E\n", render(template, Set.of()));
    }

    @Test
    void elseOnNextLineBehavesTheSame() {
      String template = "-- @if :outer\n-- @if :a\nA\n-- @end\n--@else\nE\n-- @end";
      assertEquals("A\n", render(template, Set.of("outer", "a")));
      assertEquals("", render(template, Set.of("outer")));
      assertEquals("E\n", render(template, Set.of("a")));
      assertEquals("E\n", render(template, Set.of()));
    }

    @Test
    void endWithArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @end :x"));
      assertEquals("@end without opening directive", ex.getMessage());
    }

    @Test
    void endifIsAcceptedAsAlias() {
      assertEquals("A\n", render("-- @if :x\nA\n-- @endif", Set.of("x")));
    }

    @Test
    void topLevelEndFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @end\nA"));
      assertEquals("@end without opening directive", ex.getMessage());
    }

    @Test
    void topLevelElseFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @else\nA"));
      assertEquals("Closing @end is missing for: @else", ex.getMessage());
    }
  }

  // ============================== @replace ==============================

  @Nested
  class Replace {

    @Test
    void trueWithInlineTextReplacesBody() {
      assertEquals(
          "REPLACEMENT\n",
          render("-- @replace :x REPLACEMENT\nOLD\n-- @end", Set.of("x")));
    }

    @Test
    void trueWithNoInlineUsesInsertByName() {
      assertEquals(
          "INSERTED\n",
          render(
              "-- @replace :x\nOLD\n-- @end",
              Set.of("x"),
              Map.of("x", "INSERTED")));
    }

    @Test
    void trueWithNoInlineAndNoInsertEmitsBlankLine() {
      assertEquals("\n", render("-- @replace :x\nOLD\n-- @end", Set.of("x")));
    }

    @Test
    void falseKeepsBody() {
      assertEquals("OLD\n", render("-- @replace :x REPLACEMENT\nOLD\n-- @end"));
    }

    @Test
    void falseKeepsNestedDirectives() {
      assertEquals(
          "A\n",
          render(
              "-- @replace :x REPLACEMENT\n-- @if :y\nA\n-- @end\n-- @end",
              Set.of("y")));
    }

    @Test
    void trueDiscardsNestedDirectives() {
      assertEquals(
          "REPLACEMENT\n",
          render(
              "-- @replace :x REPLACEMENT\n-- @if :y\nA\n-- @end\n-- @end",
              Set.of("x", "y")));
    }

    @Test
    void nestedReplaceInsideFalseOuterIsProcessed() {
      assertEquals(
          "B\n",
          render(
              "-- @replace :outer A\n-- @replace :inner B\n-- @end\n-- @end",
              Set.of("inner")));
    }

    @Test
    void nestedReplaceInsideTrueOuterIsDiscarded() {
      assertEquals(
          "A\n",
          render(
              "-- @replace :outer A\n-- @replace :inner B\n-- @end\n-- @end",
              Set.of("outer", "inner")));
    }

    @Test
    void bareReplaceFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @replace\nOLD"));
      assertEquals("@replace requires ':name' argument: -- @replace", ex.getMessage());
    }

    @Test
    void unclosedReplaceFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> render("-- @replace :x REPLACEMENT\nOLD"));
      assertEquals(
          "@replace block did not close with @end but with null: -- @replace :x REPLACEMENT",
          ex.getMessage());
    }

    @Test
    void replaceClosedByEndifAliasAlsoWorks() {
      assertEquals("NEW\n", render("-- @replace :x NEW\nOLD\n-- @endif", Set.of("x")));
    }
  }

  // ============================== @reorder ==============================

  @Nested
  class Reorder {

    @Test
    void singleBlockEmitsUnchanged() {
      assertEquals("A\n", render("-- @reorder :a\nA\n-- @end", Set.of(), Map.of(), Map.of()));
    }

    @Test
    void sortsBlocksByOrder() {
      String template = "-- @reorder :a\nA\n-- @end\n-- @reorder :b\nB\n-- @end";
      assertEquals(
          "B\nA\n", render(template, Set.of(), Map.of(), Map.of("a", 2, "b", 1)));
      assertEquals(
          "A\nB\n", render(template, Set.of(), Map.of(), Map.of("a", 1, "b", 2)));
    }

    @Test
    void threeBlocksSorted() {
      String template =
          "-- @reorder :a\nA\n-- @end\n-- @reorder :b\nB\n-- @end\n-- @reorder :c\nC\n-- @end";
      assertEquals(
          "C\nA\nB\n", render(template, Set.of(), Map.of(), Map.of("a", 2, "b", 3, "c", 1)));
    }

    @Test
    void tiesKeepTemplateOrder() {
      String template = "-- @reorder :a\nA\n-- @end\n-- @reorder :b\nB\n-- @end";
      assertEquals(
          "A\nB\n", render(template, Set.of(), Map.of(), Map.of("a", 1, "b", 1)));
    }

    @Test
    void textBeforeAndAfterIsPreserved() {
      String template =
          "pre\n-- @reorder :a\nA\n-- @end\n-- @reorder :b\nB\n-- @end\npost";
      assertEquals(
          "pre\nB\nA\npost\n", render(template, Set.of(), Map.of(), Map.of("a", 2, "b", 1)));
    }

    @Test
    void nonReorderLineEndsTheSequence() {
      String template =
          "-- @reorder :a\nA\n-- @end\n-- ordinary\n-- @reorder :b\nB\n-- @end";
      assertEquals(
          "A\n-- ordinary\nB\n",
          render(template, Set.of(), Map.of(), Map.of("a", 2, "b", 1)));
    }

    @Test
    void reorderInsideFalseIfIsErased() {
      String template =
          "-- @if :flag\n-- @reorder :a\nA\n-- @end\n-- @reorder :b\nB\n-- @end\n-- @end";
      assertEquals("", render(template, Set.of(), Map.of(), Map.of("a", 2, "b", 1)));
    }

    @Test
    void reorderInsideTrueIfIsEmitted() {
      String template =
          "-- @if :flag\n-- @reorder :a\nA\n-- @end\n-- @reorder :b\nB\n-- @end\n-- @end";
      assertEquals(
          "B\nA\n",
          render(template, Set.of("flag"), Map.of(), Map.of("a", 2, "b", 1)));
    }

    @Test
    void reorderBodyWithNestedIfIsProcessedPerBlock() {
      String template =
          "-- @reorder :a\n-- @if :x\nX\n-- @end\n-- @end\n"
              + "-- @reorder :b\nB\n-- @end";
      assertEquals(
          "B\nX\n", render(template, Set.of("x"), Map.of(), Map.of("a", 2, "b", 1)));
      assertEquals(
          "B\n", render(template, Set.of(), Map.of(), Map.of("a", 2, "b", 1)));
    }

    @Test
    void reorderWithoutArgumentFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @reorder\nA\n-- @end"));
      assertEquals("@reorder requires ':name' argument: -- @reorder", ex.getMessage());
    }

    @Test
    void unclosedReorderFails() {
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class, () -> render("-- @reorder :a\nA"));
      assertEquals(
          "@reorder block 'a' did not close with @end but with null: -- @reorder :a",
          ex.getMessage());
    }
  }

  // ============================== Unknown directive ==============================

  @Nested
  class Unknown {

    @Test
    void unknownDirectiveFails() {
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> render("-- @bogus\nA"));
      assertEquals("Unknown directive @bogus", ex.getMessage());
    }
  }
}