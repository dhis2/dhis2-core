/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Tests the correct object nesting for {@link JsonWriter#writeEntries(List, Stream)}.
 *
 * @author Jan Bernitt
 */
class JsonWriterTest {
  @Test
  void testWriteEntries_SingleFlatMember() {
    assertWrites("[{'key':'x','a':42}]", List.of("a"), Map.of("x", List.of("42")));
    assertWrites(
        "[{'key':'x','a':42,'b':true}]", List.of("a", "b"), Map.of("x", List.of("42", "true")));
  }

  @Test
  void testWriteEntries_SingleDeepMember() {
    assertWrites("[{'key':'x','a':{'b':42}}]", List.of("a.b"), Map.of("x", List.of("42")));
  }

  @Test
  void testWriteEntries_MultiFlatMember() {
    assertWrites(
        "[{'key':'x','a':42},{'key':'y','a':13}]",
        List.of("a"),
        mapOf("x", List.of("42"), "y", List.of("13")));
    assertWrites(
        "[{'key':'x','a':42,'b':false},{'key':'y','a':13,'b':true}]",
        List.of("a", "b"),
        mapOf("x", List.of("42", "false"), "y", List.of("13", "true")));
  }

  @Test
  void testWriteEntries_MultiDeepMember() {
    assertWrites(
        "[{'key':'x','a':{'b':42}},{'key':'y','a':{'b':13}}]",
        List.of("a.b"),
        mapOf("x", List.of("42"), "y", List.of("13")));
    assertWrites(
        "[{'key':'x','a':{'b':42,'c':false}},{'key':'y','a':{'b':13,'c':true}}]",
        List.of("a.b", "a.c"),
        mapOf("x", List.of("42", "false"), "y", List.of("13", "true")));
  }

  @Test
  void testWriteEntries_MixedDeepMember() {
    assertWrites(
        "[{'key':'x','a':1,'b':{'A':2,'B':{'c':3}},'z':4},{'key':'y','a':5,'b':{'A':6,'B':{'c':7}},'z':8}]",
        List.of("a", "b.A", "b.B.c", "z"),
        mapOf("x", List.of("1", "2", "3", "4"), "y", List.of("5", "6", "7", "8")));
  }

  private static void assertWrites(
      String expected, List<String> members, Map<String, List<String>> entries) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonWriter writer = new JsonWriter(new PrintWriter(out))) {
      writer.writeEntries(members, new LinkedHashMap<>(entries).entrySet().stream());
    }
    assertEquals(expected.replace('\'', '"'), out.toString());
  }

  /**
   * {@link Map#of(Object, Object, Object, Object)} would not preserve order, so we need to do
   * this...
   *
   * @return map with the given key value pairs preserving their order
   */
  private static <K, V> Map<K, V> mapOf(K key1, V value1, K key2, V value2) {
    LinkedHashMap<K, V> map = new LinkedHashMap<>();
    map.put(key1, value1);
    map.put(key2, value2);
    return map;
  }
}
