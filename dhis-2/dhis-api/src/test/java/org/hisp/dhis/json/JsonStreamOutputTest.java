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
package org.hisp.dhis.json;

import static org.hisp.dhis.json.JsonStreamOutput.addArrayElements;
import static org.hisp.dhis.json.JsonStreamOutput.addObjectMembers;
import static org.hisp.dhis.json.JsonStreamOutput.reorderNoParentTearing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.junit.jupiter.api.Test;

class JsonStreamOutputTest {

  // ====== reorderNoParentTearing ======

  @Test
  void testReorderNoParentTearing_Empty() {
    assertSameNoTearing(List.of());
  }

  @Test
  void testReorderNoParentTearing_One() {
    assertSameNoTearing(List.of("x"));
    assertSameNoTearing(List.of("x.y"));
    assertSameNoTearing(List.of("x.y.z"));
  }

  @Test
  void testReorderNoParentTearing_1Level() {
    assertEqualsNoTearing(List.of("a", "b", "c", "d"), List.of("a", "b", "c", "d"));
  }

  @Test
  void testReorderNoParentTearing_2Levels() {
    assertEqualsNoTearing(List.of("a.x", "a.y", "b.x", "b.z"), List.of("a.x", "b.x", "a.y", "b.z"));
    assertEqualsNoTearing(
        List.of("a.x", "a.y", "b.x", "b.z", "c"), List.of("a.x", "b.x", "a.y", "c", "b.z"));
    assertEqualsNoTearing(
        List.of("foo", "a.x", "a.y", "b.x", "b.z", "c"),
        List.of("foo", "a.x", "b.x", "a.y", "c", "b.z"));
  }

  @Test
  void testReorderNoParentTearing_3Levels() {
    assertEqualsNoTearing(
        List.of("a.b.x", "a.b.y", "a.c.z", "d"), List.of("a.b.x", "a.c.z", "a.b.y", "d"));
  }

  // ====== addArrayElements — existing test kept as-is ======

  @Test
  void testAddAsJsonObjects() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.JsonObjectBuilder.AddMember<Object> adder =
        (obj, name, val) -> obj.addNumber(name, (Number) val);
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("a.x.z", "a.y", "c", "a.x.t", "d"),
                List.of(adder, adder, adder, adder, adder),
                Stream.of(List.of(1, 2, 3, 4, 5), List.of(6, 7, 8, 9, 10)).map(l -> l::get)));
    assertEquals(
        """
        [{"a":{"x":{"z":1,"t":4},"y":2},"c":3,"d":5},{"a":{"x":{"z":6,"t":9},"y":7},"c":8,"d":10}]""",
        str.toString());
  }

  // ====== addArrayElements — new coverage ======

  @Test
  void testAddArrayElements_Empty() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str, arr -> addArrayElements(arr, List.of(), List.of(), Stream.empty()));
    assertEquals("[]", str.toString());
  }

  @Test
  void testAddArrayElements_SingleFlat() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("x", "y"),
                List.of(numberAdder(), numberAdder()),
                Stream.of(idx -> idx == 0 ? 1 : 2)));
    assertEquals(
        """
      [{"x":1,"y":2}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_MultipleElementsPreserveOrder() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("a", "b"),
                List.of(numberAdder(), numberAdder()),
                Stream.of(
                    idx -> idx == 0 ? 1 : 2, idx -> idx == 0 ? 3 : 4, idx -> idx == 0 ? 5 : 6)));
    assertEquals(
        """
      [{"a":1,"b":2},{"a":3,"b":4},{"a":5,"b":6}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_MixedFlatAndNested() {
    // Exercises root branch (flat + nested side by side) — the ObjectLayoutNode.add call
    // has to interleave a PropertyLayoutNode and an ObjectLayoutNode at the top level.
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("flat", "nest.x", "nest.y", "after"),
                List.of(numberAdder(), numberAdder(), numberAdder(), numberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> 1;
                          case 1 -> 2;
                          case 2 -> 3;
                          case 3 -> 4;
                          default -> null;
                        })));
    assertEquals(
        """
      [{"flat":1,"nest":{"x":2,"y":3},"after":4}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_SharedParentPreservedAcrossSiblings() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("shared.a", "shared.b", "shared.c"),
                List.of(numberAdder(), numberAdder(), numberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> 1;
                          case 1 -> 2;
                          case 2 -> 3;
                          default -> null;
                        })));
    assertEquals(
        """
      [{"shared":{"a":1,"b":2,"c":3}}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_SiblingNestedParents() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("a.x", "a.y", "b.x", "b.z"),
                List.of(numberAdder(), numberAdder(), numberAdder(), numberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> 1;
                          case 1 -> 2;
                          case 2 -> 3;
                          case 3 -> 4;
                          default -> null;
                        })));
    assertEquals(
        """
      [{"a":{"x":1,"y":2},"b":{"x":3,"z":4}}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_InsideObjectMember() {
    // Mirrors the actual DHIS2 usage: the array is a member of an enclosing object
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamObject(
        str,
        obj ->
            obj.addArray(
                "items",
                arr ->
                    addArrayElements(
                        arr,
                        List.of("x", "y"),
                        List.of(numberAdder(), numberAdder()),
                        Stream.of(idx -> idx == 0 ? 1 : 2))));
    assertEquals(
        """
      {"items":[{"x":1,"y":2}]}""",
        str.toString());
  }

  // ====== ArrayOfObjectsLayoutNode — the transposition branch ======

  @Test
  void testAddArrayElements_ArrayAggregate() {
    // [sub].a + [sub].b => transposed into an array of objects under "sub"
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("[sub].a", "[sub].b"),
                List.of(numberAdder(), numberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> new Object[] {1, 2, 3};
                          case 1 -> new Object[] {10, 20, 30};
                          default -> null;
                        })));
    assertEquals(
        """
      [{"sub":[{"a":1,"b":10},{"a":2,"b":20},{"a":3,"b":30}]}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_ArrayAggregate_SingleRow() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("[sub].a", "[sub].b"),
                List.of(numberAdder(), numberAdder()),
                Stream.of(idx -> idx == 0 ? new Object[] {1} : new Object[] {10})));
    assertEquals(
        """
      [{"sub":[{"a":1,"b":10}]}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_ArrayAggregate_EmptyArrays() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("[sub].a", "[sub].b"),
                List.of(numberAdder(), numberAdder()),
                Stream.of(idx -> new Object[0])));
    assertEquals(
        """
      [{"sub":[]}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_ArrayAggregate_DifferentLengths() {
    // Shorter array gets padded with nulls — exercises the null-check in the inner loop
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("[sub].a", "[sub].b"),
                List.of(nullSafeNumberAdder(), nullSafeNumberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> new Object[] {1, 2, 3};
                          case 1 -> new Object[] {10, 20}; // shorter than a
                          default -> null;
                        })));
    assertEquals(
        """
      [{"sub":[{"a":1,"b":10},{"a":2,"b":20},{"a":3,"b":null}]}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_ArrayAggregate_NullArrayThrows() {
    // Documents current behaviour — a null array value causes NPE inside the transposition
    // because `value.length` is read without a null check on the array itself.
    assertThrows(
        NullPointerException.class,
        () ->
            JsonBuilder.streamArray(
                new ByteArrayOutputStream(),
                arr ->
                    addArrayElements(
                        arr,
                        List.of("[sub].a", "[sub].b"),
                        List.of(nullSafeNumberAdder(), nullSafeNumberAdder()),
                        Stream.of(idx -> idx == 0 ? new Object[] {1, 2} : null))));
  }

  @Test
  void testAddArrayElements_ArrayAggregate_NestedParent() {
    // parent.[items].a — the array aggregate is nested inside a plain object
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("parent.[items].a", "parent.[items].b", "other"),
                List.of(numberAdder(), numberAdder(), numberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> new Object[] {1, 2};
                          case 1 -> new Object[] {10, 20};
                          case 2 -> 99;
                          default -> null;
                        })));
    assertEquals(
        """
        [{"parent":{"items":[{"a":1,"b":10},{"a":2,"b":20}]},"other":99}]""",
        str.toString());
  }

  @Test
  void testAddArrayElements_ArrayAggregatePlusFlat() {
    // Array aggregate and flat path siblings at the top level — interleaves
    // ArrayOfObjectsLayoutNode and PropertyLayoutNode in the same parent.
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamArray(
        str,
        arr ->
            addArrayElements(
                arr,
                List.of("[sub].a", "flat", "[sub].b"),
                List.of(numberAdder(), numberAdder(), numberAdder()),
                Stream.of(
                    idx ->
                        switch (idx) {
                          case 0 -> new Object[] {1, 2};
                          case 1 -> 99;
                          case 2 -> new Object[] {10, 20};
                          default -> null;
                        })));
    assertEquals(
        """
        [{"sub":[{"a":1,"b":10},{"a":2,"b":20}],"flat":99}]""",
        str.toString());
  }

  // ====== addObjectMembers ======

  @Test
  void testAddObjectMembers_Empty() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamObject(str, obj -> addObjectMembers(obj, List.of(), List.of(), idx -> null));
    assertEquals("{}", str.toString());
  }

  @Test
  void testAddObjectMembers_SingleFlat() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamObject(
        str, obj -> addObjectMembers(obj, List.of("x"), List.of(numberAdder()), idx -> 42));
    assertEquals(
        """
      {"x":42}""",
        str.toString());
  }

  @Test
  void testAddObjectMembers_MultipleFlat() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamObject(
        str,
        obj ->
            addObjectMembers(
                obj,
                List.of("a", "b", "c"),
                List.of(numberAdder(), numberAdder(), numberAdder()),
                idx ->
                    switch (idx) {
                      case 0 -> 1;
                      case 1 -> 2;
                      case 2 -> 3;
                      default -> null;
                    }));
    assertEquals(
        """
      {"a":1,"b":2,"c":3}""",
        str.toString());
  }

  @Test
  void testAddObjectMembers_Nested() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamObject(
        str,
        obj ->
            addObjectMembers(
                obj,
                List.of("a.x", "a.y", "b.z"),
                List.of(numberAdder(), numberAdder(), numberAdder()),
                idx ->
                    switch (idx) {
                      case 0 -> 1;
                      case 1 -> 2;
                      case 2 -> 3;
                      default -> null;
                    }));
    assertEquals(
        """
      {"a":{"x":1,"y":2},"b":{"z":3}}""",
        str.toString());
  }

  @Test
  void testAddObjectMembers_ArrayAggregate() {
    ByteArrayOutputStream str = new ByteArrayOutputStream();
    JsonBuilder.streamObject(
        str,
        obj ->
            addObjectMembers(
                obj,
                List.of("[sub].a", "[sub].b"),
                List.of(numberAdder(), numberAdder()),
                idx ->
                    switch (idx) {
                      case 0 -> new Object[] {1, 2, 3};
                      case 1 -> new Object[] {10, 20, 30};
                      default -> null;
                    }));
    assertEquals(
        """
        {"sub":[{"a":1,"b":10},{"a":2,"b":20},{"a":3,"b":30}]}""",
        str.toString());
  }

  // ====== Helpers ======

  private static JsonBuilder.JsonObjectBuilder.AddMember<Object> numberAdder() {
    return (obj, name, val) -> obj.addNumber(name, (Number) val);
  }

  private static JsonBuilder.JsonObjectBuilder.AddMember<Object> nullSafeNumberAdder() {
    return (obj, name, val) -> {
      if (val == null) obj.addBoolean(name, null);
      else obj.addNumber(name, (Number) val);
    };
  }

  private void assertEqualsNoTearing(List<String> expected, List<String> input) {
    assertEquals(expected, reorderNoParentTearing(input));
  }

  private void assertSameNoTearing(List<String> expected) {
    assertSame(expected, reorderNoParentTearing(expected));
  }
}
