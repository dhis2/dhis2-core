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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen
 */
class FieldFilterParserTest {

  @Test
  void testDepth0Filters() {
    assertFieldsEquals(
        List.of(new FieldPath("id"), new FieldPath("name"), new FieldPath("abc")),
        "id, name,    abc");
  }

  @Test
  void testDepth1Filters() {
    assertFieldsEquals(
        List.of(
            FieldPath.ofPath("id"),
            FieldPath.ofPath("name"),
            FieldPath.ofPath("group"),
            FieldPath.ofPath("group.id"),
            FieldPath.ofPath("group.name")),
        "id,name,group[id,name]");
  }

  @Test
  void testDepthXFilters() {
    assertFieldsEquals(
        List.of(
            FieldPath.ofPath("id"),
            FieldPath.ofPath("name"),
            FieldPath.ofPath("group"),
            FieldPath.ofPath("group.id"),
            FieldPath.ofPath("group.name"),
            FieldPath.ofPath("group.group"),
            FieldPath.ofPath("group.group.id"),
            FieldPath.ofPath("group.group.name"),
            FieldPath.ofPath("group.group.group"),
            FieldPath.ofPath("group.group.group.id"),
            FieldPath.ofPath("group.group.group.name")),
        "id,name,group[id,name],group[id,name,group[id,name,group[id,name]]]");
  }

  @Test
  void testOnlyBlockFilters() {
    assertFieldsEquals(
        List.of(
            FieldPath.ofPath("group"),
            FieldPath.ofPath("group.id"),
            FieldPath.ofPath("group.name")),
        "group[id,name]");
  }

  @Test
  void testOnlySpringBlockFilters() {
    assertFieldsEquals(
        List.of(
            FieldPath.ofPath("group"),
            FieldPath.ofPath("group.id"),
            FieldPath.ofPath("group.name")),
        "group[id,name]");
  }

  @Test
  void testParseWithTransformer1() {
    assertFieldsEquals(
        List.of(
            new FieldPath(
                "name", List.of(), List.of(new FieldPathTransformer("x", List.of("a", "b")))),
            new FieldPath(
                "id", List.of(), List.of(new FieldPathTransformer("y", List.of("a", "b", "c")))),
            new FieldPath("code", List.of(), List.of(new FieldPathTransformer("z", List.of("t"))))),
        "name::x(a;b),id~y(a;b;c),code|z(t)");
  }

  @Test
  void testParseWithTransformer2() {
    assertFieldsEquals(
        List.of(
            new FieldPath("groups"),
            new FieldPath(
                "name",
                List.of("groups"),
                List.of(new FieldPathTransformer("x", List.of("a", "b"))))),
        "groups[name::x(a;b)]");
  }

  @Test
  void testParseWithTransformer3() {
    assertFieldsEquals(
        List.of(
            new FieldPath("groups"),
            new FieldPath(
                "name",
                List.of("groups"),
                List.of(new FieldPathTransformer("x", List.of("a", "b")))),
            new FieldPath(
                "code", List.of("groups"), List.of(new FieldPathTransformer("y", List.of("a"))))),
        "groups[name::x(a;b), code~y(a)]");
  }

  @Test
  void testParseWithTransformer4() {
    assertFieldsEquals(
        List.of(
            new FieldPath(
                "name", List.of(), List.of(new FieldPathTransformer("rename", List.of("n")))),
            new FieldPath("groups"),
            new FieldPath("name", List.of("groups"))),
        "name::rename(n),groups[name]");
  }

  @Test
  void testParseWithTransformer5() {
    assertFieldsEquals(
        List.of(
            new FieldPath(
                "name", List.of(), List.of(new FieldPathTransformer("rename", List.of("n")))),
            new FieldPath(
                "groups", List.of(), List.of(new FieldPathTransformer("rename", List.of("g")))),
            new FieldPath(
                "name",
                List.of("groups"),
                List.of(new FieldPathTransformer("rename", List.of("n"))))),
        "name::rename(n),groups::rename(g)[name::rename(n)]");
  }

  @Test
  void testParseWithTransformer6() {
    assertFieldsEquals(
        List.of(
            new FieldPath(
                "name", List.of(), List.of(new FieldPathTransformer("rename", List.of("n")))),
            new FieldPath(
                "groups", List.of(), List.of(new FieldPathTransformer("rename", List.of("g")))),
            new FieldPath("name", List.of("groups"))),
        "name::rename(n),groups::rename(g)[name]");
  }

  @Test
  void testParseWithTransformer7() {
    assertFieldsEquals(
        List.of(
            new FieldPath("name", List.of(), List.of(new FieldPathTransformer("size"))),
            new FieldPath("group", List.of(), List.of(new FieldPathTransformer("isEmpty")))),
        "name::size,group::isEmpty");
  }

  @Test
  void testParseWithTransformer8() {
    assertFieldsEquals(
        List.of(
            new FieldPath(
                "name", List.of(), List.of(new FieldPathTransformer("rename", List.of("n"))))),
        "name::rename(n)");
  }

  @Test
  void testParseWithMultipleTransformers() {
    assertFieldsEquals(
        List.of(
            new FieldPath(
                "name",
                List.of(),
                List.of(
                    new FieldPathTransformer("size"),
                    new FieldPathTransformer("rename", List.of("n"))))),
        "name::size::rename(n)");
  }

  @Test
  void testParseWithPresetAndExclude1() {
    assertFieldsEquals(
        List.of(
            new FieldPath("id"),
            new FieldPath("name"),
            new FieldPath("code", List.of(), true, false),
            new FieldPath("owner", List.of(), false, true)),
        "id,name,!code,:owner");
  }

  @Test
  void testParseWithPresetAndExclude() {
    assertFieldsEquals(
        List.of(
            new FieldPath("id"),
            new FieldPath("name"),
            new FieldPath("code", List.of(), true, false),
            new FieldPath("owner", List.of(), false, true),
            new FieldPath("group"),
            new FieldPath("owner", List.of("group"), false, true),
            new FieldPath("all", List.of("group"), false, true),
            new FieldPath("code", List.of("group"), true, false),
            new FieldPath("hello", List.of("group"))),
        "id,name,!code,:owner,group[:owner,:all,!code,hello]");
  }

  @Test
  void testParseWithAsterisk1() {
    assertFieldsEquals(
        List.of(
            new FieldPath("all", List.of(), false, true),
            new FieldPath("code", List.of(), true, false)),
        "*,!code");
  }

  @Test
  void testParseWithAsterisk2() {
    assertFieldsEquals(
        List.of(
            new FieldPath("all", List.of(), false, true),
            new FieldPath("code", List.of(), true, false),
            new FieldPath("group"),
            new FieldPath("all", List.of("group"), false, true)),
        "*,!code,group[*]");
  }

  @Test
  void testMixedBlockSingleFields() {
    assertFieldsEquals(
        List.of(
            new FieldPath("id"),
            new FieldPath("name"),
            new FieldPath("group"),
            new FieldPath("id", List.of("group")),
            new FieldPath("name", List.of("group")),
            new FieldPath("code")),
        "id,name,group[id,name],code");
  }

  @Test
  void testMixedBlockWithTransformation() {
    assertFieldsEquals(
        List.of(
            new FieldPath("id"),
            new FieldPath("categoryCombo"),
            new FieldPath(
                "categoryOptionCombos",
                List.of("categoryCombo"),
                List.of(new FieldPathTransformer("size"))),
            new FieldPath("displayName")),
        "id,categoryCombo[categoryOptionCombos~size],displayName");
  }

  private void assertFieldsEquals(List<FieldPath> expected, String fields) {
    List<FieldPath> actual = FieldFilterParser.parse(fields);
    assertEquals(expected.size(), actual.size(), list(expected) + " vs " + list(actual));
    for (int i = 0; i < expected.size(); i++)
      assertEquals(expected.get(i), actual.get(i), "%d field is different: ".formatted(i));
  }

  private static String list(List<FieldPath> fields) {
    return fields.stream().map(FieldPath::getFullPath).collect(joining(","));
  }
}
