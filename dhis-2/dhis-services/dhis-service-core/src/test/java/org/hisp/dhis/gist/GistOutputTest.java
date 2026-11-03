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
package org.hisp.dhis.gist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.object.ObjectOutput;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GistOutput}, focusing on the JSON output paths that route through {@link
 * org.hisp.dhis.json.JsonStreamOutput}. CSV output is exercised separately via {@link
 * org.hisp.dhis.csv.CsvBuilder}'s own tests and is not covered here.
 *
 * <p>The core shapes tested:
 *
 * <ul>
 *   <li>{@link GistOutput#toJson(GistObject.Output, java.io.OutputStream)} — flat, nested,
 *       null-excluded, and the "unwrap" branch that extracts a single property value
 *   <li>{@link GistOutput#toJson(GistObjectList.Output, java.io.OutputStream)} — headless array,
 *       wrapped object with and without pager, and the unwrap branches for both
 * </ul>
 */
class GistOutputTest {

  private static final boolean HEADLESS = true;
  private static final boolean NO_HEADLESS = false;

  // ====== GistObject.Output.paths() ======

  @Test
  void testPaths_Empty() {
    GistObject.Output obj = new GistObject.Output(List.of(), new Object[0]);
    assertEquals(List.of(), obj.paths());
  }

  @Test
  void testPaths_Simple() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("id", String.class), prop("name", String.class)), new Object[2]);
    assertEquals(List.of("id", "name"), obj.paths());
  }

  @Test
  void testPaths_Nested() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("a.b", String.class), prop("c.d.e", String.class)), new Object[2]);
    assertEquals(List.of("a.b", "c.d.e"), obj.paths());
  }

  // ====== toJson(GistObject.Output) — normal branch ======

  @Test
  void testToJsonObject_Simple() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("id", String.class), prop("name", String.class)),
            new Object[] {"abc", "Hello"});
    assertEquals(
        """
      {"id":"abc","name":"Hello"}""",
        toJson(obj));
  }

  @Test
  void testToJsonObject_Numbers() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(
                prop("count", Integer.class),
                prop("total", Long.class),
                prop("ratio", Double.class)),
            new Object[] {42, 100L, 0.5});
    assertEquals(
        """
      {"count":42,"total":100,"ratio":0.5}""",
        toJson(obj));
  }

  @Test
  void testToJsonObject_Booleans() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("a", Boolean.class), prop("b", Boolean.class)),
            new Object[] {true, false});
    assertEquals(
        """
      {"a":true,"b":false}""",
        toJson(obj));
  }

  @Test
  void testToJsonObject_Nested() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(
                prop("a.x", String.class), prop("a.y", String.class), prop("b.z", Integer.class)),
            new Object[] {"x1", "y1", 42});
    assertEquals(
        """
      {"a":{"x":"x1","y":"y1"},"b":{"z":42}}""",
        toJson(obj));
  }

  @Test
  void testToJsonObject_NullsExcluded() {
    // FORMAT uses excludeNullMembers=true, so a null value drops the member entirely
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("a", String.class), prop("b", String.class), prop("c", String.class)),
            new Object[] {"value", null, "other"});
    assertEquals(
        """
      {"a":"value","c":"other"}""",
        toJson(obj));
  }

  @Test
  void testToJsonObject_Empty() {
    GistObject.Output obj = new GistObject.Output(List.of(), new Object[0]);
    assertEquals("{}", toJson(obj));
  }

  // ====== toJson(GistObject.Output) — unwrap branch ======

  @Test
  void testToJsonObject_Unwrap_SingleStringProperty() {
    GistObject.Output obj =
        new GistObject.Output(List.of(prop("name", String.class)), new Object[] {"Hello"});
    assertEquals("\"Hello\"", toJson(obj));
  }

  @Test
  void testToJsonObject_Unwrap_SingleNumberProperty() {
    GistObject.Output obj =
        new GistObject.Output(List.of(prop("count", Integer.class)), new Object[] {42});
    assertEquals("42", toJson(obj));
  }

  @Test
  void testToJsonObject_Unwrap_SingleNestedProperty() {
    // Path a.b — object is built as {"a":{"b":…}} then get("a.b") navigates to the value
    GistObject.Output obj =
        new GistObject.Output(List.of(prop("a.b", String.class)), new Object[] {"nested"});
    assertEquals("\"nested\"", toJson(obj));
  }

  @Test
  void testToJsonObject_Unwrap_WithTwoPropertiesFallsBackToObject() {
    // unwrap only applies when properties.size() == 1
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("a", String.class), prop("b", String.class)), new Object[] {"x", "y"});
    assertEquals(
        """
      {"a":"x","b":"y"}""",
        toJson(obj));
  }

  // ====== toJson(GistObjectList.Output) — headless branch ======

  @Test
  void testToJsonList_Headless_Empty() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS, null, "items", List.of(prop("id", String.class)), Stream.empty());
    assertEquals("[]", toJson(list));
  }

  @Test
  void testToJsonList_Headless_Single() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS,
            null,
            "items",
            List.of(prop("id", String.class), prop("name", String.class)),
            Stream.<Object[]>of(new Object[] {"abc", "Hello"}));
    assertEquals(
        """
      [{"id":"abc","name":"Hello"}]""",
        toJson(list));
  }

  @Test
  void testToJsonList_Headless_Multiple() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS,
            null,
            "items",
            List.of(prop("id", String.class)),
            Stream.<Object[]>of(new Object[] {"a"}, new Object[] {"b"}, new Object[] {"c"}));
    assertEquals(
        """
      ["a","b","c"]""",
        toJson(list));
  }

  @Test
  void testToJsonList_Headless_Nested() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS,
            null,
            "items",
            List.of(prop("a.x", String.class), prop("b.y", Integer.class)),
            Stream.<Object[]>of(new Object[] {"x1", 42}));
    assertEquals(
        """
      [{"a":{"x":"x1"},"b":{"y":42}}]""",
        toJson(list));
  }

  // ====== toJson(GistObjectList.Output) — wrapped branch ======

  @Test
  void testToJsonList_Wrapped_WithoutPager() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("id", String.class)),
            Stream.<Object[]>of(new Object[] {"abc"}));
    String json = toJson(list);
    // gist:true + collectionName array, no pager
    assertEquals(
        """
      {"items":["abc"]}""",
        json);
  }

  @Test
  void testToJsonList_Wrapped_WithPager() {
    GistPager pager = new GistPager(1, 50, 200, "prevURL", "nextURL");
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            pager,
            "items",
            List.of(prop("id", String.class)),
            Stream.<Object[]>of(new Object[] {"abc"}));
    String json = toJson(list);
    assertTrue(
        JsonMixed.of(
"""
{"pager":{"page":1,"pageSize":50,"total":200,"pageCount":4,"prevPage":"prevURL","nextPage":"nextURL"},"items":["abc"]}""")
            .equivalentTo(JsonMixed.of(json)));
  }

  @Test
  void testToJsonList_Wrapped_WithPager_NullPrevNext() {
    GistPager pager = new GistPager(1, 50, 10, null, null);
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            pager,
            "items",
            List.of(prop("id", String.class)),
            Stream.<Object[]>of(new Object[] {"a"}));
    String json = toJson(list);
    // FORMAT excludes null members, so prevPage/nextPage are absent from the output
    assertTrue(json.contains("\"page\":1"), json);
    assertFalse(json.contains("\"prevPage\""), json);
    assertFalse(json.contains("\"nextPage\""), json);
  }

  @Test
  void testToJsonList_Wrapped_Empty() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS, null, "items", List.of(prop("id", String.class)), Stream.empty());
    assertEquals(
        """
      {"items":[]}""",
        toJson(list));
  }

  // ====== toJson(GistObjectList.Output) — unwrap branches ======

  @Test
  void testToJsonList_Headless_Unwrap_SingleProperty() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS,
            null,
            "items",
            List.of(prop("name", String.class)),
            Stream.<Object[]>of(new Object[] {"a"}, new Object[] {"b"}, new Object[] {"c"}));
    assertEquals(
        """
      ["a","b","c"]""",
        toJson(list));
  }

  @Test
  void testToJsonList_Wrapped_Unwrap_SingleProperty() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("name", String.class)),
            Stream.<Object[]>of(new Object[] {"a"}, new Object[] {"b"}));
    assertEquals(
        """
      {"items":["a","b"]}""",
        toJson(list));
  }

  @Test
  void testToJsonList_Unwrap_WithTwoPropertiesFallsBackToObjects() {
    // unwrap only applies when properties.size() == 1
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS,
            null,
            "items",
            List.of(prop("a", String.class), prop("b", String.class)),
            Stream.<Object[]>of(new Object[] {"x", "y"}));
    assertEquals(
        """
      [{"a":"x","b":"y"}]""",
        toJson(list));
  }

  // ====== helpers ======

  /**
   * Helper to build an {@link ObjectOutput.Property}. Assumes:
   *
   * <ul>
   *   <li>{@code PropertyPath.of(String)} is the factory
   *   <li>{@code ObjectOutput.Type.of(Class)} is the factory
   *   <li>{@code Property} has a record component {@code (path, type, arrayAggregate)}
   * </ul>
   *
   * <p>If the actual API differs, this is the only method that needs adjustment — the test bodies
   * never construct properties directly.
   */
  private static ObjectOutput.Property prop(String path, Class<?> type) {
    return new ObjectOutput.Property(path, new ObjectOutput.Type(type), false);
  }

  private static String toJson(GistObject.Output obj) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GistOutput.toJson(obj, out);
    return out.toString();
  }

  private static String toJson(GistObjectList.Output list) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GistOutput.toJson(list, out);
    return out.toString();
  }
}
