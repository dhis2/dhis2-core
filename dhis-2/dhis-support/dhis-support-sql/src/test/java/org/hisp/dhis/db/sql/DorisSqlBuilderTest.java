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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DorisSqlBuilderTest {
  private final DorisSqlBuilder sqlBuilder = new DorisSqlBuilder("dhis2", "driver.jar");

  @Test
  void testQuote() {
    assertEquals("`quarterly`", sqlBuilder.quote("quarterly"));
    assertEquals("`Fully immunized`", sqlBuilder.quote("Fully immunized"));
    // Internal backticks must be doubled
    assertEquals("`Tre``ated`", sqlBuilder.quote("Tre`ated"));
  }

  @Test
  void testUnquote() {
    // Null and empty inputs
    assertEquals("", sqlBuilder.unquote(null));
    assertEquals("", sqlBuilder.unquote(""));

    // Strip surrounding backticks
    assertEquals("hello", sqlBuilder.unquote("`hello`"));
    assertEquals("hello world", sqlBuilder.unquote("`hello world`"));

    // Unescape doubled inner backticks
    assertEquals("he`llo", sqlBuilder.unquote("`he``llo`"));

    // Return as-is when not wrapped in backticks
    assertEquals("not quoted", sqlBuilder.unquote("not quoted"));

    // Empty content between quotes
    assertEquals("", sqlBuilder.unquote("``"));

    // Round-trip property: unquote(quote(x)) == x
    assertEquals("simple", sqlBuilder.unquote(sqlBuilder.quote("simple")));
    assertEquals("has `inner` tick", sqlBuilder.unquote(sqlBuilder.quote("has `inner` tick")));
  }

  @Test
  void testQualifyTable() {
    assertEquals("dhis2.public.`organisationunit`", sqlBuilder.qualifyTable("organisationunit"));
  }
}
