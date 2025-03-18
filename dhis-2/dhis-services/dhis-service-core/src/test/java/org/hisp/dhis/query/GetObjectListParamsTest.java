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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the parameter processing implemented in {@link GetObjectListParams}.
 *
 * @author Jan Bernitt
 */
class GetObjectListParamsTest {

  @Test
  void testSplitFilters_empty() {
    assertFilters(null, "");
    assertFilters(null, null);
  }

  @Test
  void testSplitFilters_2parts() {
    assertFilters(List.of("name:empty"), "name:empty");
    assertFilters(List.of("organisationUnits:!empty"), "organisationUnits:!empty");
    assertFilters(
        List.of("name:empty", "organisationUnits:!empty"), "name:empty,organisationUnits:!empty");
  }

  @Test
  void testSplitFilters_3parts() {
    assertFilters(List.of("name:eq:Peter"), "name:eq:Peter");
    assertFilters(List.of("parent.id:eq:a1234t6u8o"), "parent.id:eq:a1234t6u8o");
    assertFilters(
        List.of("name:eq:Peter", "parent.id:eq:a1234t6u8o"),
        "name:eq:Peter,parent.id:eq:a1234t6u8o");
  }

  @Test
  void testSplitFilters_in() {
    assertFilters(List.of("name:in:[Peter,Paul,Mary]"), "name:in:[Peter,Paul,Mary]");
    assertFilters(
        List.of("age:eq:20", "name:in:[Peter,Paul,Mary]", "points:lt:200"),
        "age:eq:20,name:in:[Peter,Paul,Mary],points:lt:200");
  }

  @Test
  void testSplitFilters_allOperators() {
    List<String> ops =
        List.of(
            "eq",
            "ieq",
            "!eq",
            "neq",
            "ne",
            "gt",
            "lt",
            "gte",
            "ge",
            "lte",
            "le",
            "like",
            "!like",
            "$like",
            "!$like",
            "like$",
            "!like$",
            "ilike",
            "!ilike",
            "startsWith",
            "$ilike",
            "!$ilike",
            "token",
            "!token",
            "endsWith",
            "ilkike$",
            "!ilike$",
            "in",
            "!in",
            "null",
            "empty",
            "!null");
    for (String op : ops) {
      String filter = "name:%s:value".formatted(op);
      assertFilters(List.of(filter), filter);
      assertFilters(List.of("foo:eq:bar", filter), "foo:eq:bar," + filter);
      assertFilters(List.of("foo:eq:bar", filter, "x:empty"), "foo:eq:bar," + filter + ",x:empty");
    }
  }

  private static void assertFilters(List<String> expected, String actual) {
    assertEquals(expected, GetObjectListParams.splitFilters(actual));
  }
}
