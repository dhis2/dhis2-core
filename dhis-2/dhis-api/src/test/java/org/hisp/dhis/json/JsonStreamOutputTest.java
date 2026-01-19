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
package org.hisp.dhis.json;

import static org.hisp.dhis.json.JsonStreamOutput.addArrayElements;
import static org.hisp.dhis.json.JsonStreamOutput.reorderNoParentTearing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.junit.jupiter.api.Test;

class JsonStreamOutputTest {

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

  private void assertEqualsNoTearing(List<String> expected, List<String> input) {
    assertEquals(expected, reorderNoParentTearing(input));
  }

  private void assertSameNoTearing(List<String> expected) {
    assertSame(expected, reorderNoParentTearing(expected));
  }
}
