/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Morten Olav Hansen
 */
class FieldFilterParserTest {
  record ExpectField(boolean expected, String dotPath) {}

  @ParameterizedTest
  @MethodSource("testParserProvider")
  void testParser(String input, List<ExpectField> expectFields) {
    List<FieldPath> fieldPaths = FieldFilterParser.parse(input);

    assertFields(expectFields, fieldPaths);
  }

  static Stream<Arguments> testParserProvider() {
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
            List.of(new ExpectField(true, "group.id"), new ExpectField(true, "group.name"))),

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

        // TODO(ivo) bug or wanted?
        Arguments.of(
            " id,name  group ",
            List.of(new ExpectField(true, "id"), new ExpectField(true, "namegroup"))),

        // TODO(ivo) not sure if code is part of the :owner preset. Figure that out and also how to
        // test current preset behavior with the new one as this parser does not take care of preset
        // expansion. The logic is spread into the service or some helper.
        // testExclude1
        //        Arguments.of("id,name,!code,:owner",
        Arguments.of(
            "id,name,!code",
            List.of(new ExpectField(true, "id"), new ExpectField(true, "name")),
            new ExpectField(false, "code")));
  }

  @Test
  void testDepth0Filters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id, name,    abc");

    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "abc");
  }

  @Test
  void testDepth1Filters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id,name,group[id,name]");

    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
  }

  @Test
  void testDepthXFilters() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse(
            "id,name,group[id,name],group[id,name,group[id,name,group[id,name]]]");

    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
    assertFieldPathContains(fieldPaths, "group.group.id");
    assertFieldPathContains(fieldPaths, "group.group.name");
    assertFieldPathContains(fieldPaths, "group.group.group.id");
    assertFieldPathContains(fieldPaths, "group.group.group.name");
  }

  @Test
  void testOnlyBlockFilters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("group[id,name]");

    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
  }

  @Test
  void testOnlySpringBlockFilters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("group[id,name]");

    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
  }

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

  private static void assertFields(List<ExpectField> expectFields, List<FieldPath> fieldPaths) {
    for (ExpectField expectField : expectFields) {
      assertField(expectField.expected, expectField.dotPath, fieldPaths);
    }
  }

  /**
   * Tests if the field represented by the full path as used by the current FieldFilterParser is
   * included in the parsed fields predicate.
   */
  private static void assertField(
      boolean expected, String expectedDotPath, List<FieldPath> fieldPaths) {
    assertFieldPathContains(expected, expectedDotPath, fieldPaths);
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

  private static void assertFieldPathContains(
      boolean expected, String expectedDotPath, List<FieldPath> fieldPaths) {
    Set<String> actual =
        fieldPaths.stream().map(FieldPath::toFullPath).collect(toUnmodifiableSet());
    String what = expected ? "include" : "exclude";
    assertEquals(
        expected,
        actual.contains(expectedDotPath),
        () -> actual + " does not " + what + " " + expectedDotPath);
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
