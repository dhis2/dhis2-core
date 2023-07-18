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
package org.hisp.dhis.gist;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link GistQuery} API methods to compose a {@link GistQuery} object.
 *
 * @author Jan Bernitt
 */
class GistQueryTest {

  @Test
  void testFilterParse_SimpleValue() {
    assertFilterEquals("name:eq:2", -1, "name", Comparison.EQ, "2");
    assertFilterEquals("name::eq::2", -1, "name", Comparison.EQ, "2");
    assertFilterEquals("name~eq~2", -1, "name", Comparison.EQ, "2");
    assertFilterEquals("name@eq@2", -1, "name", Comparison.EQ, "2");
  }

  @Test
  void testFilterParse_SimpleValueWithDelimiter() {
    assertFilterEquals("name:eq:2:3", -1, "name", Comparison.EQ, "2:3");
  }

  @Test
  void testFilterParse_GroupSimpleValue() {
    assertFilterEquals("1:name:eq:2", 1, "name", Comparison.EQ, "2");
    assertFilterEquals("1~name~eq~2", 1, "name", Comparison.EQ, "2");
    assertFilterEquals("1@name@eq@2", 1, "name", Comparison.EQ, "2");
  }

  @Test
  void testFilterParse_GroupValueWithDelimiter() {
    assertFilterEquals("1:name:eq:2:3", 1, "name", Comparison.EQ, "2:3");
  }

  @Test
  void testFilterParse_ArrayValue() {
    assertFilterEquals("name:eq:[1,2]", -1, "name", Comparison.EQ, "1", "2");
  }

  @Test
  void testFilterParse_ArrayValueWithDelimiters() {
    assertFilterEquals("name:eq:[1:1,2:2]", -1, "name", Comparison.EQ, "1:1", "2:2");
  }

  @Test
  void testFilterParse_GroupArray() {
    assertFilterEquals("1:name:eq:[1,2]", 1, "name", Comparison.EQ, "1", "2");
  }

  @Test
  void testFilterParse_GroupArrayValueWithDelimiters() {
    assertFilterEquals("2:name:eq:[1:1,2:2]", 2, "name", Comparison.EQ, "1:1", "2:2");
  }

  private void assertFilterEquals(
      String filter, int group, String name, Comparison op, String... value) {
    assertFilterEquals(Filter.parse(filter), group, name, op, value);
  }

  private void assertFilterEquals(
      Filter actual, int group, String name, Comparison op, String... value) {
    assertEquals(group, actual.getGroup());
    assertEquals(name, actual.getPropertyPath());
    assertEquals(op, actual.getOperator());
    assertArrayEquals(value, actual.getValue());
  }
}
