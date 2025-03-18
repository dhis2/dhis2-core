/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.attribute;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link LazyAttributeValues} implementation.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
class AttributeValuesTest {

  @Test
  void testOf() {
    assertThrows(IllegalArgumentException.class, () -> AttributeValues.of("1"));
    assertThrows(IllegalArgumentException.class, () -> AttributeValues.of("true"));
    assertThrows(IllegalArgumentException.class, () -> AttributeValues.of("\"hello\""));
  }

  @Test
  void testIsEmpty() {
    assertTrue(AttributeValues.empty().isEmpty());
    assertTrue(AttributeValues.of("{}").isEmpty());
    assertTrue(AttributeValues.of(Map.of()).isEmpty());
    assertFalse(AttributeValues.of("{\"a\": {\"value\":\"b\"}}").isEmpty());
    assertFalse(AttributeValues.of(Map.of("a", "b")).isEmpty());
  }

  @Test
  void testSize() {
    assertEquals(0, AttributeValues.empty().size());
    assertEquals(0, AttributeValues.of(Map.of()).size());
    assertEquals(1, AttributeValues.of(Map.of("a", "b")).size());
    assertEquals(2, AttributeValues.of(Map.of("a", "b", "c", "d")).size());
    assertEquals(1, AttributeValues.of("{\"a\": {\"value\":\"b\"}}").size());
  }

  @Test
  void testKeys() {
    assertEquals(Set.of(), AttributeValues.empty().keys());
    assertEquals(Set.of(), AttributeValues.of(Map.of()).keys());
    assertEquals(Set.of("a"), AttributeValues.of(Map.of("a", "b")).keys());
    assertEquals(Set.of("a", "c"), AttributeValues.of(Map.of("a", "b", "c", "d")).keys());
    assertEquals(Set.of("a"), AttributeValues.of("{\"a\": {\"value\":\"b\"}}").keys());
  }

  @Test
  void testValues() {
    assertEquals(Set.of(), AttributeValues.empty().values());
    assertEquals(Set.of(), AttributeValues.of(Map.of()).values());
    assertEquals(Set.of("b"), AttributeValues.of(Map.of("a", "b")).values());
    assertEquals(Set.of("b", "d"), AttributeValues.of(Map.of("a", "b", "c", "d")).values());
    assertEquals(Set.of("b"), AttributeValues.of("{\"a\": {\"value\":\"b\"}}").values());
  }

  @Test
  void testGet() {
    assertNull(AttributeValues.empty().get("a"));
    assertNull(AttributeValues.of(Map.of()).get("a"));
    assertNull(AttributeValues.of(Map.of("a", "b")).get("x"));
    assertEquals("b", AttributeValues.of(Map.of("a", "b")).get("a"));
    assertEquals("d", AttributeValues.of(Map.of("a", "b", "c", "d")).get("c"));
    assertEquals("b", AttributeValues.of("{\"a\": {\"value\":\"b\"}}").get("a"));
  }

  @Test
  void testContains() {
    assertFalse(AttributeValues.empty().contains("a"));
    assertFalse(AttributeValues.of("{}").contains("x"));
    assertFalse(AttributeValues.of(Map.of()).contains("y"));
    assertTrue(AttributeValues.of("{\"a\": {\"value\":\"b\"}}").contains("a"));
    assertTrue(AttributeValues.of(Map.of("a", "b")).contains("a"));
    assertTrue(AttributeValues.of(Map.of("a", "b", "c", "d")).contains("c"));
  }

  @Test
  void testAdd() {
    AttributeValues by = AttributeValues.empty().added("b", "y");
    assertEquals(AttributeValues.of(Map.of("b", "y")), by);
    AttributeValues axby = by.added("a", "x");
    assertEquals(AttributeValues.of(Map.of("a", "x", "b", "y")), axby);
    assertEquals(AttributeValues.of(Map.of("c", "z", "b", "y")), by.added("c", "z"));
    assertEquals(AttributeValues.of(Map.of("a", "x", "b", "y", "c", "z")), axby.added("c", "z"));
    assertEquals(AttributeValues.of(Map.of("a", "7", "b", "y")), axby.added("a", "7"));
    Map<String, String> expected = new HashMap<>();
    AttributeValues actual = AttributeValues.empty();
    for (int i = -10; i < 10; i++) {
      String key = "" + ('a' + i);
      String value = "" + i;
      expected.put(key, value);
      actual = actual.added(key, value);
      assertEquals(AttributeValues.of(expected), actual);
    }
  }

  @Test
  void testRemove() {
    assertEquals(AttributeValues.empty(), AttributeValues.empty().removed("a"));
    assertEquals(
        AttributeValues.of(Map.of("a", "b")), AttributeValues.of(Map.of("a", "b")).removed("c"));
    assertEquals(AttributeValues.empty(), AttributeValues.of(Map.of("a", "b")).removed("a"));
    AttributeValues before = AttributeValues.of(Map.of("a", "x", "b", "y", "c", "z"));
    assertEquals(AttributeValues.of(Map.of("b", "y", "c", "z")), before.removed("a"));
    assertEquals(AttributeValues.of(Map.of("a", "x", "c", "z")), before.removed("b"));
    assertEquals(AttributeValues.of(Map.of("a", "x", "b", "y")), before.removed("c"));
    assertSame(before, before.removed("r"));
  }

  @Test
  void testForEach() {
    AttributeValues.empty().forEach((k, v) -> fail("should not be called"));
    AttributeValues.of("{}").forEach((k, v) -> fail("should not be called"));
    AttributeValues.of(Map.of()).forEach((k, v) -> fail("should not be called"));
    Map<String, String> res = new HashMap<>();
    AttributeValues.of("{\"a\": {\"value\":\"b\"}}").forEach(res::put);
    assertEquals(Map.of("a", "b"), res);
    res.clear();
    AttributeValues.of(Map.of("a", "b")).forEach(res::put);
    assertEquals(Map.of("a", "b"), res);
    res.clear();
    AttributeValues.of(Map.of("a", "b", "c", "d")).forEach(res::put);
    assertEquals(Map.of("a", "b", "c", "d"), res);
  }

  @Test
  void testStream() {
    assertEquals(List.of(), AttributeValues.empty().stream().toList());
    assertEquals(List.of(), AttributeValues.of("{}").stream().toList());
    assertEquals(List.of(), AttributeValues.of(Map.of()).stream().toList());
    assertEquals(
        Map.of("a", "b"),
        AttributeValues.of("{\"a\": {\"value\":\"b\"}}").stream()
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertTrue(AttributeValues.of(Map.of("a", "b")).contains("a"));
    assertEquals(
        Map.of("a", "b"),
        AttributeValues.of(Map.of("a", "b")).stream()
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
    assertEquals(
        Map.of("a", "b", "c", "d"),
        AttributeValues.of(Map.of("a", "b", "c", "d")).stream()
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  @Test
  void testToObjectJson() {
    assertEquals("{}", AttributeValues.empty().toObjectJson());
    assertEquals("{}", AttributeValues.of("{}").toObjectJson());
    assertEquals("{}", AttributeValues.of(Map.of()).toObjectJson());
    AttributeValues fromJson = AttributeValues.of("{\"a\": {\"value\":\"b\"}}");
    assertEquals("{\"a\": {\"value\":\"b\"}}", fromJson.toObjectJson());
    // note that the JSON is different in whitespace because when constructed from JSON the original
    // JSON is returned while when constructed from a map the JSON is composed
    assertEquals("{\"a\":{\"value\":\"b\"}}", AttributeValues.of(Map.of("a", "b")).toObjectJson());
    // however, once the JSON got parsed internally the toJson is computed again
    assertFalse(fromJson.isEmpty()); // force parse
    assertEquals("{\"a\":{\"value\":\"b\"}}", fromJson.toObjectJson());

    // note that the order of root properties is alphabetically sorted in JSON
    assertEquals(
        "{\"c\":{\"value\":\"d\"},\"x\":{\"value\":\"y\"}}",
        AttributeValues.of(Map.of("x", "y", "c", "d")).toObjectJson());
  }

  @Test
  void testToArrayJson() {
    assertEquals("[]", AttributeValues.empty().toArrayJson());
    assertEquals("[]", AttributeValues.of("{}").toArrayJson());
    assertEquals("[]", AttributeValues.of(Map.of()).toArrayJson());
    AttributeValues fromJson = AttributeValues.of("{\"a\": {\"value\":\"b\"}}");
    assertEquals("[{\"value\":\"b\",\"attribute\":{\"id\":\"a\"}}]", fromJson.toArrayJson());
    assertEquals(
        "[{\"value\":\"d\",\"attribute\":{\"id\":\"c\"}},{\"value\":\"y\",\"attribute\":{\"id\":\"x\"}}]",
        AttributeValues.of(Map.of("x", "y", "c", "d")).toArrayJson());
  }

  @Test
  void testMapKeys() {
    assertEquals(AttributeValues.empty(), AttributeValues.empty().mapKeys(v -> v.repeat(2)));
    assertEquals(
        AttributeValues.of(Map.of("aa", "b")),
        AttributeValues.of(Map.of("a", "b")).mapKeys(v -> v.repeat(2)));
  }

  @Test
  void testMapValues() {
    assertEquals(AttributeValues.empty(), AttributeValues.empty().mapValues(v -> v.repeat(2)));
    assertEquals(
        AttributeValues.of(Map.of("a", "bb")),
        AttributeValues.of(Map.of("a", "b")).mapValues(v -> v.repeat(2)));
  }

  @Test
  void testRemovedAll() {
    assertEquals(AttributeValues.empty(), AttributeValues.empty().removedAll(id -> true));
    assertEquals(AttributeValues.empty(), AttributeValues.empty().removedAll(id -> false));
    assertEquals(
        AttributeValues.of(Map.of("a", "b")),
        AttributeValues.of(Map.of("a", "b")).removedAll("c"::equals));
    assertEquals(
        AttributeValues.empty(), AttributeValues.of(Map.of("a", "b")).removedAll("a"::equals));
    AttributeValues before = AttributeValues.of(Map.of("a", "x", "b", "y", "c", "z"));
    assertEquals(AttributeValues.of(Map.of("b", "y", "c", "z")), before.removedAll("a"::equals));
    assertEquals(AttributeValues.of(Map.of("a", "x", "c", "z")), before.removedAll("b"::endsWith));
    assertEquals(
        AttributeValues.of(Map.of("a", "x", "b", "y")), before.removedAll("c"::startsWith));
    assertEquals(AttributeValues.of(Map.of("b", "y")), before.removedAll(id -> id.matches("[ac]")));
    assertSame(before, before.removedAll("r"::equals));
  }

  @Test
  void testToMap() {
    assertEquals(Map.of(), AttributeValues.empty().toMap());
    assertEquals(Map.of("a", "1"), AttributeValues.of(Map.of("a", "1")).toMap());
  }
}
