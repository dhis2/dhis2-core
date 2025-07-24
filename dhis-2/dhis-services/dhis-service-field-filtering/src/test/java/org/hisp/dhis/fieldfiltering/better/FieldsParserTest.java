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
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPathTransformer;
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
  record ExpectField(boolean included, String dotPath) {}

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
            "id,name,group[id,name],group[id,name,group[id,name,group[id,name[[[",
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

        // based on testParseWithPresetAndExclude without the presets as I will need to test them separately
        // FieldFilterParser.parse("id,name,!code,:owner,group[:owner,:all,!code,hello]");
        Arguments.of(
            "code,id,name,!code,group[!code,hello,code]",
            List.of(
                new ExpectField(true, "id"),
                new ExpectField(true, "name"),
                new ExpectField(false, "code"),
                new ExpectField(true, "group"),
                new ExpectField(false, "group.code"),
                new ExpectField(true, "group.hello"))));
  }

  // The following tests show where the current and better implementations differ. Some differences
  // are due to an improved API in the better parser, some are due to what I think are bugs in the
  // current implementation which we need to go through case by case.

  // TODO(ivo) better API compared to current:
  // /api/organisationUnits?fields=dataSets[name],!dataSets will return an empty object
  // The better parser clearly shows that negation has precedence over inclusion.
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

  // TODO(ivo): this is a bug IMHO opening a group without a field
  // http://localhost:8080/api/organisationUnits?pageSize=1&fields=[id]
  // I get empty objects

  // TODO(ivo) this is a bug IMHO as it leads to an HTTP 500 instead of 400
  @Test
  void bugInCurrentParserUnbalancedClosingParen() {
    assertThrows(
        java.util.EmptyStackException.class, () -> FieldFilterParser.parse("group[name]]"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "]", ")",
        "group[name]]", "group[name))",
        // TODO old parser throws EmptyStackException which leads to a 500 error
        "group[name],id]", "group[name],id)",
      })
  void betterParserFailsOnUnbalancedClosingParen(String input) {
    assertThrows(IllegalArgumentException.class, () -> FieldsParser.parse(input));
  }

  // TODO(ivo) create extra tests for * only for my parser?
  // I might have to separate the two or create a separate preset test with different assertions
  //    @Test
  //    void testParseWithAsterisk1() {
  //      List<FieldPath> fieldPaths = FieldFilterParser.parse("*,!code");
  //
  //      FieldPath asterisk = getFieldPath(fieldPaths, "all");
  //      assertNotNull(asterisk);
  //      assertFalse(asterisk.isExclude());
  //      assertTrue(asterisk.isPreset());
  //      FieldPath code = getFieldPath(fieldPaths, "code");
  //      assertNotNull(code);
  //      assertTrue(code.isExclude());
  //      assertFalse(code.isPreset());
  //    }
  @Test
  void testBetterParserStar() {
    Fields fields = FieldsParser.parse("*,!code");

    assertFields(
        List.of(new ExpectField(false, "code"),new ExpectField(true, "group"), new ExpectField(true, "group.code")), fields);
  }

  // TODO implement: group(id) is equivalent to group[id] but () is also used for transformers
  // TODO(ivo) presets: org.hisp.dhis.fieldfiltering.FieldPathHelper.applyPresets does rely on the
  // schema. Make a provision for this that allows passing in a Map<String, Set<String>> presets
  // into the parser.
  // :all should be a preset mapped to * (maybe a default preset that users of the parser cannot
  // override)
  // on the other hand FieldPreset is a static mapping of presets to fields. Why do we still need
  // the schema then? Is it due to his approach of computing all paths of an object instead of only
  // what we need to know?
  // TODO(ivo) add test for negating presets which is ignored so leads to preset inclusion
  // TODO(ivo) only used by metadata: fields=parent on orgUnits only shows the parent.id and fields=parent[:all] shows all. This is done by the FieldFilterService, the FieldFilterParser only parses the presets without expanding them.
  // fields
  // TODO(ivo) support transformers: I think I first need to investigate all their intricacies and what a
  // more efficient way is for Jackson
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

  // TODO(ivo) double-check my ported tests are equivalent, make them fail, look at assertion errors
  @Test
  void testParseWithPresetAndExclude1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id,name,!code,:owner");

    FieldPath id = getFieldPath(fieldPaths, "id");
    assertNotNull(id);
    assertFalse(id.isExclude());
    assertFalse(id.isPreset());
    FieldPath name = getFieldPath(fieldPaths, "name");
    assertNotNull(name);
    assertFalse(name.isExclude());
    assertFalse(name.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
    FieldPath owner = getFieldPath(fieldPaths, "owner");
    assertNotNull(owner);
    assertFalse(owner.isExclude());
    assertTrue(owner.isPreset());
  }

  @Test
  void testParseWithPresetAndExclude() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse("id,name,!code,:owner,group[:owner,:all,!code,hello]");

    FieldPath id = getFieldPath(fieldPaths, "id");
    assertNotNull(id);
    assertFalse(id.isExclude());
    assertFalse(id.isPreset());
    FieldPath name = getFieldPath(fieldPaths, "name");
    assertNotNull(name);
    assertFalse(name.isExclude());
    assertFalse(name.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
    FieldPath owner = getFieldPath(fieldPaths, "owner");
    assertNotNull(owner);
    assertFalse(owner.isExclude());
    assertTrue(owner.isPreset());
    FieldPath groupOwner = getFieldPath(fieldPaths, "group.owner");
    assertNotNull(groupOwner);
    assertFalse(groupOwner.isExclude());
    assertTrue(groupOwner.isPreset());
    FieldPath groupAll = getFieldPath(fieldPaths, "group.all");
    assertNotNull(groupAll);
    assertFalse(groupAll.isExclude());
    assertTrue(groupAll.isPreset());
    FieldPath groupCode = getFieldPath(fieldPaths, "group.code");
    assertNotNull(groupCode);
    assertTrue(groupCode.isExclude());
    assertFalse(groupCode.isPreset());
    FieldPath groupHello = getFieldPath(fieldPaths, "group.hello");
    assertNotNull(groupHello);
    assertFalse(groupHello.isExclude());
    assertFalse(groupHello.isPreset());
  }

  @Test
  void testParseWithAsterisk1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("*,!code");

    FieldPath asterisk = getFieldPath(fieldPaths, "all");
    assertNotNull(asterisk);
    assertFalse(asterisk.isExclude());
    assertTrue(asterisk.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
  }

  @Test
  void testParseWithAsterisk2() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("*,!code,group[*]");

    FieldPath asterisk = getFieldPath(fieldPaths, "all");
    assertNotNull(asterisk);
    assertFalse(asterisk.isExclude());
    assertTrue(asterisk.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
    FieldPath groupAsterisk = getFieldPath(fieldPaths, "group.all");
    assertNotNull(groupAsterisk);
    assertFalse(groupAsterisk.isExclude());
    assertTrue(groupAsterisk.isPreset());
  }

  @Test
  void testMixedBlockSingleFields() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id,name,group[id,name],code");

    assertEquals(6, fieldPaths.size());
    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "group");
    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
    assertFieldPathContains(fieldPaths, "code");
  }

  // TODO(ivo) need to implement transformers
  @Test
  void testParseWithTransformer1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::x(a;b),id~y(a;b;c),code|z(t)");

    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "code");
  }

  @Test
  void testParseWithTransformer2() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("groups[name::x(a;b)]");

    assertFieldPathContains(fieldPaths, "groups");
    assertFieldPathContains(fieldPaths, "groups.name");
  }

  @Test
  void testParseWithTransformer3() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("groups[name::x(a;b), code~y(a)]");

    assertFieldPathContains(fieldPaths, "groups");
    assertFieldPathContains(fieldPaths, "groups.name");
    assertFieldPathContains(fieldPaths, "groups.code");
  }

  @Test
  void testParseWithTransformer4() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::rename(n),groups[name]");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "groups");
    assertFieldPathContains(fieldPaths, "groups.name", false);
  }

  @Test
  void testParseWithTransformer5() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse("name::rename(n),groups::rename(g)[name::rename(n)]");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "groups", true);
    assertFieldPathContains(fieldPaths, "groups.name", true);
  }

  @Test
  void testParseWithTransformer6() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::rename(n),groups::rename(g)[name]");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "groups", true);
    assertFieldPathContains(fieldPaths, "groups.name", false);
  }

  @Test
  void testParseWithTransformer7() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::size,group::isEmpty");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "group", true);
  }

  @Test
  void testParseWithTransformer8() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::rename(n)");

    assertFieldPathContains(fieldPaths, "name", true);
    FieldPathTransformer fieldPathTransformer = fieldPaths.get(0).getTransformers().get(0);
    assertEquals("rename", fieldPathTransformer.getName());
  }

  @Test
  void testParseWithMultipleTransformers() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::size::rename(n)");

    assertFieldPathContains(fieldPaths, "name", true);
    FieldPathTransformer fieldPathTransformer = fieldPaths.get(0).getTransformers().get(0);
    assertEquals("size", fieldPathTransformer.getName());
    fieldPathTransformer = fieldPaths.get(0).getTransformers().get(1);
    assertEquals("rename", fieldPathTransformer.getName());
  }

  @Test
  void testMixedBlockWithTransformation() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse("id,categoryCombo[categoryOptionCombos~size],displayName");

    assertEquals(4, fieldPaths.size());
    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "categoryCombo");
    assertFieldPathContains(fieldPaths, "categoryCombo.categoryOptionCombos");
    assertFieldPathContains(fieldPaths, "displayName");
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
    String what = expected.included ? "includes" : "exclude";
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
      assertTrue(
          actual.stream().anyMatch(fp -> !fp.isExclude()),
          () ->
              fieldPaths.stream().map(FieldPath::toFullPath).collect(Collectors.toSet())
                  + " should includes "
                  + expected.dotPath
                  + " but it does not contain the path with an inclusion");
    } else {
      assertTrue(
          actual.stream().anyMatch(FieldPath::isExclude),
          () ->
              fieldPaths.stream().map(FieldPath::toFullPath).collect(Collectors.toSet())
                  + " should exclude "
                  + expected.dotPath
                  + " but it does not contain the path with an exclusion");
    }
  }

  private void assertFieldPathContains(
      List<FieldPath> fieldPaths, String expected, boolean isTransformer) {
    boolean condition = false;
    for (FieldPath fieldPath : fieldPaths) {
      String path = fieldPath.toFullPath();
      if (path.equals(expected)) {
        condition = fieldPath.isTransformer() == isTransformer;
        break;
      }
    }
    assertTrue(condition);
  }

  private void assertFieldPathContains(List<FieldPath> fieldPaths, String expected) {
    Set<String> actual =
        fieldPaths.stream().map(FieldPath::toFullPath).collect(toUnmodifiableSet());
    assertTrue(actual.contains(expected), () -> actual + " does not contain " + expected);
  }

  private FieldPath getFieldPath(List<FieldPath> fieldPaths, String path) {
    for (FieldPath fieldPath : fieldPaths) {
      String fullPath = fieldPath.toFullPath();
      if (path.equals(fullPath)) {
        return fieldPath;
      }
    }
    return null;
  }
}
