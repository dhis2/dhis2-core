/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterUtilsTest {

  @Test
  public void testFromFilterWithNull() {
    // Test with null input
    List<String> result = FilterUtils.fromFilter(null);
    assertTrue(result.isEmpty(), "Result should be empty for null input");
  }

  @Test
  public void testFromFilterWithEmptyString() {
    // Test with empty string
    List<String> result = FilterUtils.fromFilter("");
    assertTrue(result.isEmpty(), "Result should be empty for empty string input");
  }

  @Test
  public void testFromFilterWithNoOperation() {
    // Test with input that doesn't contain a colon (no operation)
    String filter = "abc123";
    List<String> result = FilterUtils.fromFilter(filter);

    assertEquals(1, result.size(), "Should return a list with one element");
    assertEquals(filter, result.get(0), "The element should be the input string");
  }

  @Test
  public void testFromFilterWithInOperation() {
    // Test with IN operation and multiple identifiers
    String filter = "IN:id1;id2;id3";
    List<String> result = FilterUtils.fromFilter(filter);

    List<String> expected = Arrays.asList("id1", "id2", "id3");
    assertEquals(expected, result, "Should return a list with all identifiers");
  }

  @Test
  public void testFromFilterWithEqOperation() {
    // Test with EQ operation or any other operation besides IN
    String filter = "EQ:id1";
    List<String> result = FilterUtils.fromFilter(filter);

    assertEquals(1, result.size(), "Should return a list with one element");
    assertEquals("id1", result.get(0), "The element should be the identifier");
  }

  @Test
  public void testFromFilterWithEmptyIdentifiers() {
    // Test with IN operation but empty identifiers
    String filter = "IN:";
    List<String> result = FilterUtils.fromFilter(filter);

    assertEquals(1, result.size(), "Should return a list with one element");
    assertEquals("", result.get(0), "The element should be an empty string");
  }

  @Test
  public void testFromFilterWithInOperationAndEmptyElements() {
    // Test with IN operation and empty elements in the list
    String filter = "IN:id1;;id3";
    List<String> result = FilterUtils.fromFilter(filter);

    List<String> expected = Arrays.asList("id1", "", "id3");
    assertEquals(expected, result, "Should return a list including empty elements");
  }

  @Test
  public void testFromFilterWithMultipleColons() {
    // Test with multiple colons in the filter
    String filter = "EQ:prefix:value";
    List<String> result = FilterUtils.fromFilter(filter);

    assertEquals(1, result.size(), "Should return a list with one element");
    assertEquals(
        "prefix:value",
        result.get(0),
        "The element should include everything after the first colon");
  }

  @Test
  public void testFromFilterWithColonOnly() {
    // Test with just a colon
    String filter = ":";
    List<String> result = FilterUtils.fromFilter(filter);

    assertEquals(1, result.size(), "Should return a list with one element");
    assertEquals("", result.get(0), "The element should be an empty string");
  }
}
