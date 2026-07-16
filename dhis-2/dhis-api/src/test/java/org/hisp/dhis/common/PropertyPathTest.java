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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.Text;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link PropertyPath} value class.
 *
 * @author Jan Bernitt
 */
class PropertyPathTest {

  @Test
  void testOf() {
    assertPropertyPath("foo");
    assertPropertyPath("foo.bar");
    assertPropertyPath("foo.bar.baz");
    // more uncommon cases with special 1st character
    assertPropertyPath(":all");
    assertPropertyPath("-foo");
    assertPropertyPath("!foo");
    assertPropertyPath("-foo.bar");
    assertPropertyPath("!foo.bar");
  }

  @Test
  void testOf_Star() {
    assertEquals(":all", PropertyPath.of("*").toString(), "* should become :all");
  }

  @Test
  void testOf_Varagrs() {
    assertEquals(PropertyPath.of("foo.bar"), PropertyPath.of("foo", "bar"));
    assertEquals(PropertyPath.of("foo.bar.baz"), PropertyPath.of("foo", "bar", "baz"));
  }

  @Test
  void testOf_Illegal() {
    assertIllegalPath("");
    assertIllegalPath("**");
    assertIllegalPath("!!foo");
    assertIllegalPath("::all");
    assertIllegalPath("foo..bar");
    assertIllegalPath("-foo.-bar");
    assertIllegalPath(":foo.:bar");
    assertIllegalPath("!foo.!bar");
    assertIllegalPath("foo.:bar.baz");
  }

  @Test
  void testOf_IllegalEmpty() {
    assertThrowsExactly(IllegalArgumentException.class, PropertyPath::of);
  }

  @Test
  void testCompareTo() {
    assertEquals(
        List.of(
            PropertyPath.of("a"),
            PropertyPath.of("abc"),
            PropertyPath.of("b"),
            PropertyPath.of("a.a"),
            PropertyPath.of("a.b"),
            PropertyPath.of("a.a.c"),
            PropertyPath.of("a.b.b")),
        Stream.of(
                PropertyPath.of("b"),
                PropertyPath.of("a.a.c"),
                PropertyPath.of("abc"),
                PropertyPath.of("a.b"),
                PropertyPath.of("a.a"),
                PropertyPath.of("a.b.b"),
                PropertyPath.of("a"))
            .sorted()
            .toList());
  }

  @Test
  void testIsNested() {
    assertTrue(PropertyPath.of("a.b").isNested());
    assertTrue(PropertyPath.of("!a.b").isNested());

    assertFalse(PropertyPath.of("foo").isNested());
    assertFalse(PropertyPath.of("!foo").isNested());

    assertFalse(PropertyPath.of(":foo").isNested());
  }

  @Test
  void testIsExclude() {
    assertFalse(PropertyPath.of("a.b").isExclude());
    assertFalse(PropertyPath.of("foo").isExclude());

    assertTrue(PropertyPath.of("!a.b").isExclude());
    assertTrue(PropertyPath.of("!foo").isExclude());

    assertTrue(PropertyPath.of("-a.b").isExclude());
    assertTrue(PropertyPath.of("-foo").isExclude());
    assertTrue(PropertyPath.of("foo.-bar").isExclude());

    assertFalse(PropertyPath.of(":foo").isExclude());
  }

  @Test
  void testIsPreset() {
    assertFalse(PropertyPath.of("a.b").isPreset());
    assertFalse(PropertyPath.of("foo").isPreset());

    assertFalse(PropertyPath.of("!a.b").isPreset());
    assertFalse(PropertyPath.of("!foo").isPreset());

    assertFalse(PropertyPath.of("-a.b").isPreset());
    assertFalse(PropertyPath.of("-foo").isPreset());

    assertTrue(PropertyPath.of(":foo").isPreset());
  }

  @Test
  void testIsAll() {
    assertFalse(PropertyPath.of("a.b").isAll());
    assertFalse(PropertyPath.of("foo").isAll());

    assertFalse(PropertyPath.of("!a.b").isAll());
    assertFalse(PropertyPath.of("!foo").isAll());

    assertFalse(PropertyPath.of("-a.b").isAll());
    assertFalse(PropertyPath.of("-foo").isAll());

    assertFalse(PropertyPath.of(":foo").isAll());

    assertTrue(PropertyPath.of(":all").isAll());
    assertTrue(PropertyPath.of("*").isAll());
  }

  @Test
  void testIsUID() {
    assertFalse(PropertyPath.of("a.b").isUID());
    assertFalse(PropertyPath.of("foo").isUID());

    assertFalse(PropertyPath.of("!a.b").isUID());
    assertFalse(PropertyPath.of("!foo").isUID());

    assertFalse(PropertyPath.of("-a.b").isUID());
    assertFalse(PropertyPath.of("-foo").isUID());

    assertFalse(PropertyPath.of(":foo").isUID());
    assertFalse(PropertyPath.of(":all").isUID());
    assertFalse(PropertyPath.of("*").isUID());

    assertTrue(PropertyPath.of("ou123456789").isUID());
  }

  @Test
  void testLength() {
    assertEquals(1, PropertyPath.of("foo").length());
    assertEquals(2, PropertyPath.of("foo.bar").length());
    assertEquals(3, PropertyPath.of("foo.bar.baz").length());

    assertEquals(2, PropertyPath.of("!a.b").length());
    assertEquals(1, PropertyPath.of("!foo").length());
    assertEquals(2, PropertyPath.of("-a.b").length());
    assertEquals(1, PropertyPath.of("-foo").length());
  }

  @Test
  void testProperty() {
    assertEquals("foo", PropertyPath.of("foo").property().toString());
    assertEquals("bar", PropertyPath.of("foo.bar").property().toString());
    assertEquals("baz", PropertyPath.of("foo.bar.baz").property().toString());
    assertEquals("foo", PropertyPath.of("!foo").property().toString());
    assertEquals("bar", PropertyPath.of("!foo.bar").property().toString());
    assertEquals("baz", PropertyPath.of("!foo.bar.baz").property().toString());
    assertEquals("baz", PropertyPath.of("foo.bar.!baz").property().toString());
    assertEquals("foo", PropertyPath.of("-foo").property().toString());
  }

  @Test
  void testProperties() {
    assertEquals(List.of("foo"), PropertyPath.of("foo").properties().map(Text::toString).toList());
    assertEquals(
        List.of("foo", "bar"),
        PropertyPath.of("foo.bar").properties().map(Text::toString).toList());
    assertEquals(
        List.of("foo", "bar"),
        PropertyPath.of("!foo.bar").properties().map(Text::toString).toList());
    assertEquals(
        List.of("foo", "bar"),
        PropertyPath.of("foo.!bar").properties().map(Text::toString).toList());
    assertEquals(
        List.of(":foo"), PropertyPath.of(":foo").properties().map(Text::toString).toList());
  }

  @Test
  void testHead() {
    assertEquals(Text.of("foo"), PropertyPath.of("foo").head());
    assertEquals(Text.of("foo"), PropertyPath.of("foo.bar").head());
    assertEquals(Text.of("foo"), PropertyPath.of("foo.bar.baz").head());
  }

  @Test
  void testConcat() {
    assertEquals(PropertyPath.of("foo.bar"), PropertyPath.of("foo").concat("bar"));
    assertEquals(PropertyPath.of("foo.:all"), PropertyPath.of("foo").concat("*"));
    assertEquals(
        PropertyPath.of("foo.bar.baz"), PropertyPath.of("foo").concat(PropertyPath.of("bar.baz")));

    assertEquals(PropertyPath.of("!foo.bar"), PropertyPath.of("!foo").concat("bar"));
  }

  @Test
  void testWithTail() {
    assertEquals(PropertyPath.of("y"), PropertyPath.of("foo").withTail("y"));
    assertEquals(PropertyPath.of("foo.y"), PropertyPath.of("foo.bar").withTail("y"));
    assertEquals(PropertyPath.of("foo.bar.y"), PropertyPath.of("foo.bar.baz").withTail("y"));
  }

  @Test
  void testRelativeTo() {
    assertNull(PropertyPath.of("foo").relativeTo("foo"));
    assertEquals(PropertyPath.of("bar"), PropertyPath.of("foo.bar").relativeTo("foo"));
    assertEquals(
        PropertyPath.of("a1234567890"),
        PropertyPath.of("attributeValues.attribute.a1234567890").relativeTo("attribute"));
  }

  @Test
  void testContains() {
    assertTrue(PropertyPath.of("foo.bar").contains("bar"));
    assertTrue(PropertyPath.of("foo.bar").contains("foo"));
    assertFalse(PropertyPath.of("foo.bar").contains("!bar"));
    assertFalse(PropertyPath.of("foo.bar").contains("!foo"));
    assertFalse(PropertyPath.of("foo.bar").contains("baz"));
    assertFalse(PropertyPath.of("foo.bar").contains("foo.bar"));
  }

  @Test
  void testIsExcluded() {
    assertTrue(PropertyPath.of("!code").isExcluded("code"));
    assertTrue(PropertyPath.of("!x").isExcluded("x.bar"));
    assertTrue(PropertyPath.of("x.!y").isExcluded("x.y"));
    assertTrue(PropertyPath.of("x.!y").isExcluded("x.y.z"));

    assertFalse(PropertyPath.of("code").isExcluded("code"));
    assertFalse(PropertyPath.of("!code").isExcluded("name"));
    assertFalse(PropertyPath.of("!foo.code").isExcluded("code"));
    assertFalse(PropertyPath.of("foo.!code").isExcluded("code"));
  }

  @Test
  void testDropHead() {
    assertNull(PropertyPath.of("foo").dropHead());
    assertEquals(PropertyPath.of("bar"), PropertyPath.of("foo.bar").dropHead());
    assertEquals(PropertyPath.of("bar"), PropertyPath.of("!foo.bar").dropHead());
    assertEquals(PropertyPath.of("!bar"), PropertyPath.of("foo.!bar").dropHead());
    assertEquals(PropertyPath.of("bar.baz"), PropertyPath.of("foo.bar.baz").dropHead());
  }

  private static void assertPropertyPath(String path) {
    PropertyPath actual = PropertyPath.of(path);
    assertEquals(path, actual.toString(), "toString() of a path should be the same as the input");
    String[] segments = path.split("\\.");
    assertEquals(
        segments.length,
        actual.length(),
        "number of segments should be the same as a regex split on dot");
    List<Text> actualSegments = actual.segments().toList();
    for (int i = 0; i < segments.length; i++) {
      assertEquals(segments[i], actualSegments.get(i).toString());
    }
  }

  private static void assertIllegalPath(String path) {
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> PropertyPath.of(path),
        "of %s should not be a valid path".formatted(path));
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> new PropertyPath(null, Text.of(path)),
        "new with %s should not be a valid path".formatted(path));
    String[] segments = path.split("\\.");
    assertThrowsExactly(IllegalArgumentException.class, () -> PropertyPath.of(segments));
  }
}
