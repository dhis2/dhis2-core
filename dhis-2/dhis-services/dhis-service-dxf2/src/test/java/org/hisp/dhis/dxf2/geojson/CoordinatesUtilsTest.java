/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.geojson;

import static org.hisp.dhis.dxf2.geojson.CoordinatesUtils.coordinateDimensions;
import static org.hisp.dhis.dxf2.geojson.CoordinatesUtils.coordinatesAsPairs;
import static org.hisp.dhis.dxf2.geojson.CoordinatesUtils.coordinatesEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonValue;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link CoordinatesUtils}
 *
 * @author Jan Bernitt
 */
class CoordinatesUtilsTest {
  @Test
  void testCoordinatesEmpty() {
    assertTrue(coordinatesEmpty(JsonValue.of("{}").asObject().get("coordinates")));
    assertTrue(coordinatesEmpty(JsonValue.of("null")));
    assertTrue(coordinatesEmpty(JsonValue.of("[]")));
    assertTrue(coordinatesEmpty(JsonValue.of("[[]]")));
    assertTrue(coordinatesEmpty(JsonValue.of("[[[]]]")));
    assertTrue(coordinatesEmpty(JsonValue.of("[[[[]]]]")));

    assertFalse(coordinatesEmpty(JsonValue.of("[1]")));
    assertFalse(coordinatesEmpty(JsonValue.of("[[1]]")));
    assertFalse(coordinatesEmpty(JsonValue.of("[[[1]]]")));
    assertFalse(coordinatesEmpty(JsonValue.of("[[[[1]]]]")));
  }

  @Test
  void testCoordinateDimensions() {
    assertEquals(0, coordinateDimensions(JsonValue.of("null")));
    assertEquals(0, coordinateDimensions(JsonValue.of("23")));
    assertEquals(1, coordinateDimensions(JsonValue.of("[23]")));
    assertEquals(2, coordinateDimensions(JsonValue.of("[[23],[42]]")));
    assertEquals(3, coordinateDimensions(JsonValue.of("[[[23],[42]],[[], []]]")));
    assertEquals(4, coordinateDimensions(JsonValue.of("[ [[[23],[42]],[[], []]] ]")));
  }

  @Test
  void testCoordinatesAsPairs_1Dimension() {
    assertEquals("[1]", coordinatesAsPairs("[1]"));
    assertEquals("[1,2]", coordinatesAsPairs("[1,2]"));
    assertEquals("[1,2]", coordinatesAsPairs("[1,2,3]"));
  }

  @Test
  void testCoordinatesAsPairs_2Dimensions() {
    assertEquals("[[1]]", coordinatesAsPairs("[[1]]"));
    assertEquals("[[1,2]]", coordinatesAsPairs("[[1,2]]"));
    assertEquals("[[1,2]]", coordinatesAsPairs("[[1,2,3]]"));
    assertEquals("[[1,2],[4,5],[7,8]]", coordinatesAsPairs("[[1,2,3],[4,5,6],[7,8,9]]"));
  }

  @Test
  void testCoordinatesAsPairs_3Dimensions() {
    assertEquals("[[[1]]]", coordinatesAsPairs("[[[1]]]"));
    assertEquals("[[[1,2]]]", coordinatesAsPairs("[[[1,2]]]"));
    assertEquals("[[[1,2]]]", coordinatesAsPairs("[[[1,2,3]]]"));
    assertEquals("[[[1,2],[4,5],[7,8]]]", coordinatesAsPairs("[[[1,2,3],[4,5,6],[7,8,9]]]"));
    assertEquals(
        "[[[1,2],[4,5],[7,8]],[[3,4],[6,7],[9,0]]]",
        coordinatesAsPairs("[[[1,2,3],[4,5,6],[7,8,9]],[[3,4,5],[6,7,8],[9,0,1]]]"));
  }

  @Test
  void testCoordinatesAsPairs_4Dimensions() {
    assertEquals("[[[[1]]]]", coordinatesAsPairs("[[[[1]]]]"));
    assertEquals("[[[[1,2]]]]", coordinatesAsPairs("[[[[1,2]]]]"));
    assertEquals("[[[[1,2]]]]", coordinatesAsPairs("[[[[1,2,3]]]]"));
    assertEquals("[[[[1,2],[4,5],[7,8]]]]", coordinatesAsPairs("[[[[1,2,3],[4,5,6],[7,8,9]]]]"));
    assertEquals(
        "[[[[1,2],[4,5],[7,8]],[[3,4],[6,7],[9,0]]]]",
        coordinatesAsPairs("[[[[1,2,3],[4,5,6],[7,8,9]],[[3,4,5],[6,7,8],[9,0,1]]]]"));
  }
}
