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
package org.hisp.dhis.fieldfiltering.better;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.Fields.Transformation;
import org.hisp.dhis.schema.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the better {@link FieldsParser} and its backwards compatibility with the current {@link
 * FieldFilterParser}. Some tests are ported over from {@code FieldFilterParserTest}. Comments
 * indicate where the tests differ.
 */
class FieldsParserTest {

  record ExpectField(boolean included, String dotPath, Transformation... transformations) {}

  @ParameterizedTest
  @MethodSource("providerEqualBehavior")
  void testBetterParser(String input, List<ExpectField> expectFields) {
    Fields fields = FieldsParser.parse(input);

    assertFields(expectFields, fields);
  }

  @ParameterizedTest
  @MethodSource("providerEqualBehavior")
  void testCurrentParser(String input, List<ExpectField> expectFields) {
    List<FieldPath> fieldPaths = FieldFilterParser.parse(input);

    assertFields(expectFields, fieldPaths);
  }

  // Tests with a comment that looks like a method name are ported from FieldFilterParserTest to
  // ensure the current and better parser behave the same. Some more tests were added.
  static Stream<Arguments> providerEqualBehavior() {
    return Stream.of(
        // testDepth0Filters
        Arguments.of(
            "id, name,    abc",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "abc"))),

        // testDepth1Filters
        Arguments.of(
            "id,name,group[id,name]",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"))),

        // testDepthXFilters
        Arguments.of(
            "id,name,group[id,name],group[id,name,group[id,name,group[id,name]]]",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"),
                new ExpectField(true, "group.group.id"),
                new ExpectField(true, "group.group.name"),
                new ExpectField(true, "group.group.group.id"),
                new ExpectField(true, "group.group.group.name"))),

        // testOnlyBlockFilters
        Arguments.of(
            "group[id,name]",
            List.of(
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"))),

        // missing closing brackets are ignored
        Arguments.of(
            "id,name,group[id,name],group[id,name,group[id,name,group[id,name[",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"),
                new ExpectField(true, "group.group.id"),
                new ExpectField(true, "group.group.name"),
                new ExpectField(true, "group.group.group.id"),
                new ExpectField(true, "group.group.group.name"))),

        // () is treated like [] (might have special meaning in a transformer waiting on answer from
        // platform)
        Arguments.of(
            "group(id,name)",
            List.of(
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"))),

        // brackets and parentheses can be mixed :joy:
        Arguments.of(
            "group(id,name]",
            List.of(
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"))),
        Arguments.of(
            "group[id,name)",
            List.of(
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"))),

        // testMixedBlockSingleFields
        Arguments.of(
            "id,name,group[id,name],code",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"),
                new ExpectField(true, "code"))),

        // testIgnoreWhitespace
        Arguments.of(
            " id,name  , group[ id , name  ], code  ",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"),
                new ExpectField(true, "code"))),

        // ignore empty fields and blocks
        Arguments.of(
            " id, ,, group[ , , id ,  ], code  ,",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "code"))),

        // testBlockSpreadOut
        Arguments.of(
            "id,group[id],name,group[name],code",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"),
                new ExpectField(true, "code"))),

        // TODO(ivo) bug or wanted? this is the behavior of the current FieldFilterParser not sure
        // I replicated it for backwards compatibility but am unsure if we want to trim whitespace
        // inside of a field name
        Arguments.of(
            " id  ,name  code, gro  up [ id , name  ]  ",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "namecode"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.id"),
                new ExpectField(true, "group.name"))),
        Arguments.of(
            "id,name,!code",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(false, "code"))),

        // exclusion has higher precedence
        // exclusion before inclusion
        Arguments.of(
            "!code,id,name,code",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(false, "code"))),

        // exclusion after inclusion
        Arguments.of(
            "code,id,name,!code",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(false, "code"))),

        // exclusion only affects its level
        Arguments.of(
            "id,name,!code,group[code]",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(false, "code"),
                new ExpectField(true, "group"),
                new ExpectField(true, "group.code"))),

        // based on testParseWithPresetAndExclude without the presets which are tested separately
        // below
        Arguments.of(
            "code,id,name,!code,group[!code,hello,code]",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(false, "code"),
                new ExpectField(true, "group"),
                new ExpectField(false, "group.code"),
                new ExpectField(true, "group.hello"))),

        // testParseWithTransformer1
        Arguments.of(
            "name::x(a;b),id~y(a;b;c),code|z(t)",
            List.of(
                new ExpectField(true, "name", new Transformation("x", "a", "b")),
                new ExpectField(true, "id", new Transformation("y", "a", "b", "c")),
                new ExpectField(true, "code", new Transformation("z", "t")))),

        // testParseWithTransformer2
        Arguments.of(
            "groups[name::x(a;b)]",
            List.of(
                new ExpectField(true, "groups"),
                new ExpectField(true, "groups.name", new Transformation("x", "a", "b")))),

        // testParseWithTransformer3
        Arguments.of(
            "groups[name::x(a;b), code~y(a)]",
            List.of(
                new ExpectField(true, "groups"),
                new ExpectField(true, "groups.name", new Transformation("x", "a", "b")),
                new ExpectField(true, "groups.code", new Transformation("y", "a")))),

        // testParseWithTransformer4
        Arguments.of(
            "name::rename(n),groups[name]",
            List.of(
                new ExpectField(true, "name", new Transformation("rename", "n")),
                new ExpectField(true, "groups"),
                new ExpectField(true, "groups.name"))),

        // testParseWithTransformer5
        Arguments.of(
            "name::rename(n),groups::rename(g)[name::rename(n)]",
            List.of(
                new ExpectField(true, "name", new Transformation("rename", "n")),
                new ExpectField(true, "groups", new Transformation("rename", "g")),
                new ExpectField(true, "groups.name", new Transformation("rename", "n")))),

        // testParseWithTransformer6
        Arguments.of(
            "name::rename(n),groups::rename(g)[name]",
            List.of(
                new ExpectField(true, "name", new Transformation("rename", "n")),
                new ExpectField(true, "groups", new Transformation("rename", "g")),
                new ExpectField(true, "groups.name"))),

        // testParseWithTransformer7
        Arguments.of(
            "name::size,group::isEmpty",
            List.of(
                new ExpectField(true, "name", new Transformation("size")),
                new ExpectField(true, "group", new Transformation("isEmpty")))),

        // testParseWithTransformer8
        Arguments.of(
            "name::rename(n)",
            List.of(new ExpectField(true, "name", new Transformation("rename", "n")))),

        // testParseWithMultipleTransformers
        Arguments.of(
            "name::size::rename(n)",
            List.of(
                new ExpectField(
                    true, "name", new Transformation("size"), new Transformation("rename", "n")))),

        // testMixedBlockWithTransformation
        Arguments.of(
            "id,categoryCombo[categoryOptionCombos~size],displayName",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "categoryCombo"),
                new ExpectField(
                    true, "categoryCombo.categoryOptionCombos", new Transformation("size")),
                new ExpectField(true, "displayName"))));
  }

  // The following tests show where the current and better implementations differ. Some differences
  // are due to an improved API in the better parser, some are due to what I think are bugs in the
  // current implementation which we need to go through case by case.

  // TODO(ivo) better API compared to current:
  // /api/organisationUnits?fields=dataSets[name],!dataSets will return an empty object
  // The better parser clearly shows that excluding has precedence over inclusion.
  // The current parser does not as exclusions are handled in the FieldFilterService. This makes it
  // confusion to read the expectations of the current parser.
  @Test
  void testBetterParserIncludeChildOfExcludedParent() {
    Fields fields = FieldsParser.parse("group[code],!group");

    assertFields(
        List.of(new ExpectField(false, "group"), new ExpectField(false, "group.code")), fields);
  }

  @Test
  void testCurrentParserIncludeChildOfExcludedParent() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("group[code],!group");

    assertFields(
        List.of(new ExpectField(false, "group"), new ExpectField(true, "group.code")), fieldPaths);
  }

  // TODO(ivo) this is a bug IMHO as it leads to an HTTP 500 instead of 400
  @Test
  void bugInCurrentParserUnbalancedClosingParen() {
    assertThrows(
        java.util.EmptyStackException.class, () -> FieldFilterParser.parse("group[name]]"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // TODO old parser ignores this, I think this should be invalid
        "[value]"
      })
  void betterParserFailsOnBlockWithoutName(String input) {
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> FieldsParser.parse(input));

    assertContains("Block must have a field name", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // TODO old parser throws EmptyStackException which leads to a 500 error
        "]",
        ")",
        "group[name]]",
        "group[group[name)]),code",
        "group[name))",
        "group[name],id]",
        "group[name],id)",
      })
  void betterParserFailsOnUnbalancedClosingParen(String input) {
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> FieldsParser.parse(input));

    assertContains("Unbalanced", exception.getMessage());
  }

  private static final Map<String, Function<Schema, Set<String>>> PRESETS =
      Map.of(":all", FieldsParser.PRESET_ALL);

  record Any() {}

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*", ":all", "!*", "!:all",
      })
  void testBetterParserGivenRootStar(String input) {
    Schema schema = new Schema(Any.class, "any", "any");
    Fields fields = FieldsParser.parse(input, schema, (s, f) -> schema, PRESETS);

    assertFields(
        List.of(
            new ExpectField(true, "code"),
            new ExpectField(true, "group"),
            new ExpectField(true, "group.code"),
            new ExpectField(true, "group.group.code")),
        fields);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*,!code",
        ":all,!code",
        "!*,!code",
        "!:all,!code",
      })
  void testBetterParserGivenRootStarAndExclusion(String input) {
    Schema schema = new Schema(Any.class, "any", "any");
    Fields fields = FieldsParser.parse(input, schema, (s, f) -> schema, PRESETS);

    assertFields(
        List.of(
            new ExpectField(false, "code"),
            new ExpectField(true, "group"),
            new ExpectField(true, "group.code"),
            new ExpectField(true, "group.group.code")),
        fields);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*,group[!code]",
        ":all,group[!code]",
        "!*,group[!code]",
        "!:all,group[!code]",
      })
  void testBetterParserGivenRootStarAndChildExclusion(String input) {
    Schema schema = new Schema(Any.class, "any", "any");
    Fields fields = FieldsParser.parse(input, schema, (s, f) -> schema, PRESETS);

    assertFields(
        List.of(
            new ExpectField(true, "code"),
            new ExpectField(true, "code.name"),
            new ExpectField(true, "group"),
            new ExpectField(false, "group.code"),
            new ExpectField(false, "group.code.name")),
        fields);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "code,group[*,!code],names[list[*]],names[list[!first]]",
        "code,group[:all,!code],names[list[:all]],names[list[!first]]",
        "code,group[!*,!code],names[list[!*]],names[list[!first]]",
        "code,group[!:all,!code],names[list[!:all]],names[list[!first]]",
      })
  void testBetterParserGivenChildStarAndChildExclusion(String input) {
    Schema schema = new Schema(Any.class, "any", "any");
    Fields fields = FieldsParser.parse(input, schema, (s, f) -> schema, PRESETS);

    assertFields(
        List.of(
            new ExpectField(true, "code"),
            new ExpectField(true, "code.name"),
            new ExpectField(true, "group"),
            new ExpectField(true, "group.name"),
            new ExpectField(false, "group.code"),
            new ExpectField(false, "group.code.name"),
            new ExpectField(true, "names"),
            new ExpectField(true, "names.list"),
            new ExpectField(true, "names.list.second"),
            new ExpectField(false, "names.list.first"),
            new ExpectField(false, "names.list.first.deep")),
        fields);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "!group",
        "!group[]",
        "!group[code]", // TODO(ivo) bug in old field filter NPE Cannot invoke
        // "org.hisp.dhis.fieldfiltering.FieldPath.getPath()" because "fieldPath" is
        // null
      })
  void bugInCurrentParserExcludingParent(String input) {
    Fields fields = FieldsParser.parse(input);

    assertFields(
        List.of(
            new ExpectField(false, "none"),
            new ExpectField(false, "group"),
            new ExpectField(false, "group.code"),
            new ExpectField(false, "group.code.name")),
        fields);
  }

  @Test
  void testGetChildrenAlignsWithIncludes() {
    Fields fields = FieldsParser.parse("relationships[!from]");

    assertTrue(fields.test("relationships"));
    assertFalse(fields.getChildren("relationships").test("from"));
    assertTrue(fields.getChildren("relationships").test("to"));
    assertFalse(fields.getChildren("relationships").getChildren("from").test("value"));

    assertTrue(fields.includes("relationships"));
    assertTrue(fields.includes("relationships.to"));
    assertFalse(fields.includes("relationships.from"));
    assertFalse(fields.includes("relationships.from.value"));
  }

  // TODO(ivo) transformers: error handling, like I think we should not allow duplicate transformers
  // TODO(ivo) transformers: test what happens with transformers on presets

  // TODO(ivo) only used in tests: FieldFilterParser.parseWithPrefix can be removed
  @Test
  void testParseWithPrefix1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parseWithPrefix("a,b", "prefix");

    assertFieldPathContains(fieldPaths, "prefix.a");
    assertFieldPathContains(fieldPaths, "prefix.b");
  }

  @Test
  void testParseWithPrefix2() {
    List<FieldPath> fieldPaths = FieldFilterParser.parseWithPrefix("aaa[a],bbb[b]", "prefix");

    assertFieldPathContains(fieldPaths, "prefix.aaa.a");
    assertFieldPathContains(fieldPaths, "prefix.bbb.b");
  }

  public static void assertFields(List<ExpectField> expectFields, Fields fields) {
    for (ExpectField expectField : expectFields) {
      assertField(expectField, fields);
    }
  }

  /**
   * Tests if the field represented by the full path as used by the current FieldFilterParser is
   * included in the parsed fields predicate.
   */
  private static void assertField(ExpectField expected, Fields fields) {
    String what = expected.included ? "includes" : "exclude";

    assertEquals(
        expected.included,
        fields.includes(expected.dotPath),
        "fields " + fields + " does not " + what + " " + expected.dotPath);
    assertTransformations(expected, fields);
  }

  private static void assertTransformations(ExpectField expected, Fields fields) {
    String[] segments = expected.dotPath.split("\\.");
    Fields current = fields;
    for (int i = 0; i < segments.length - 1; i++) {
      current = current.getChildren(segments[i]);
    }
    String lastSegment = segments[segments.length - 1];
    List<Transformation> actual = current.getTransformations(lastSegment);

    if (expected.transformations.length == 0) {
      assertIsEmpty(
          actual == null ? List.of() : actual,
          "Expected no transformations for field " + expected.dotPath);
    } else {
      assertNotEmpty(actual, "Expected transformations for field " + expected.dotPath);
      assertEquals(
          List.of(expected.transformations),
          actual,
          "Transformations mismatch for field " + expected.dotPath);
    }
  }

  private static void assertFields(
      List<FieldsParserTest.ExpectField> expectFields, List<FieldPath> fieldPaths) {
    for (ExpectField expectField : expectFields) {
      assertField(expectField, fieldPaths);
    }
  }

  /**
   * Tests if the field represented by the full path as used by the current FieldFilterParser is
   * included in the parsed fields predicate.
   */
  private static void assertField(ExpectField expected, List<FieldPath> fieldPaths) {
    String what = expected.included ? "include" : "exclude";
    List<FieldPath> actual =
        fieldPaths.stream().filter(fp -> expected.dotPath.equals(fp.toFullPath())).toList();
    assertNotEmpty(
        actual,
        () ->
            fieldPaths.stream().map(FieldPath::toFullPath).collect(Collectors.toSet())
                + " should contain "
                + expected.dotPath
                + " and "
                + what
                + " it");

    Predicate<FieldPath> predicate;
    if (expected.included) {
      // exclusion has higher precedence over inclusion, a field can thus only be considered
      // included if there is not also an exclusion
      assertFalse(
          actual.stream().anyMatch(FieldPath::isExclude),
          () ->
              fieldPaths.stream().map(FieldPath::toFullPath).collect(Collectors.toSet())
                  + " should includes "
                  + expected.dotPath
                  + " but it contains the path with an exclusion");
      predicate = fp -> !fp.isExclude();
    } else {
      predicate = FieldPath::isExclude;
    }
    Optional<FieldPath> path = actual.stream().filter(predicate).findAny();
    assertTrue(
        path.isPresent(),
        () ->
            fieldPaths.stream().map(FieldPath::toFullPath).collect(Collectors.toSet())
                + " should contain "
                + expected.dotPath
                + " and "
                + what
                + " it");

    assertTransformations(expected, path.get());
  }

  private static void assertTransformations(ExpectField expected, FieldPath actual) {
    if (expected.transformations.length == 0) {
      assertIsEmpty(
          actual.getTransformers(), "Expected no transformations for field " + expected.dotPath);
    } else {
      assertNotEmpty(
          actual.getTransformers(),
          "Expected transformations "
              + Arrays.toString(expected.transformations)
              + " for field "
              + expected.dotPath);
      List<Transformation> actualTransformations =
          actual.getTransformers().stream()
              .map(t -> new Transformation(t.getName(), t.getParameters().toArray(new String[0])))
              .toList();
      assertEquals(
          List.of(expected.transformations),
          actualTransformations,
          "Transformations mismatch for field " + expected.dotPath);
    }
  }

  private void assertFieldPathContains(List<FieldPath> fieldPaths, String expected) {
    Set<String> actual =
        fieldPaths.stream().map(FieldPath::toFullPath).collect(toUnmodifiableSet());
    assertTrue(actual.contains(expected), () -> actual + " does not contain " + expected);
  }

  record Token(String type, String value, int start, int end) {}

  @ParameterizedTest
  @MethodSource("providerEqualBehavior")
  void testRegexTokenization(String input, List<ExpectField> expectFields) {
    Pattern pattern =
        Pattern.compile(
            "(!?\\w+(?:\\s*\\w+)*(?:(?:[~|]|::)\\w+(?:\\([^)]*\\))?)*\\s*)|(,)|(\\[|\\()|(\\]|\\))");

    List<Token> tokens = tokenize(input, pattern);

    // Print tokens for debugging
    System.out.println("Input: \"" + input + "\"");
    for (Token token : tokens) {
      System.out.println("  " + token);
    }
    System.out.println();
  }

  private List<Token> tokenize(String input, Pattern pattern) {
    List<Token> tokens = new ArrayList<>();
    Matcher matcher = pattern.matcher(input);

    while (matcher.find()) {
      String tokenType = null;
      String value = matcher.group();

      if (matcher.group(1) != null) {
        tokenType = "NAME";
      } else if (matcher.group(2) != null) {
        tokenType = "SEPARATOR";
      } else if (matcher.group(3) != null) {
        tokenType = "PAREN_OPEN";
      } else if (matcher.group(4) != null) {
        tokenType = "PAREN_CLOSE";
      }

      if (tokenType != null) {
        tokens.add(new Token(tokenType, value, matcher.start(), matcher.end()));
      }
    }

    return tokens;
  }
}
