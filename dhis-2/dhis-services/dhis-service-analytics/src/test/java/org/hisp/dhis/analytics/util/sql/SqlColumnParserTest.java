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
package org.hisp.dhis.analytics.util.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqlColumnParserTest {
  @Test
  void testRemoveTableAlias_WithDoubleQuotes() {
    String result = SqlColumnParser.removeTableAlias("ax.\"uidlevel2\"");
    assertEquals("uidlevel2", result);
  }

  @Test
  void testRemoveTableAlias_WithBackticks() {
    String result = SqlColumnParser.removeTableAlias("cc.`alfa`");
    assertEquals("alfa", result);
  }

  @Test
  void testRemoveTableAlias_WithoutQuotes() {
    String result = SqlColumnParser.removeTableAlias("test1.uidlevel2");
    assertEquals("uidlevel2", result);
  }

  @Test
  void testRemoveTableAlias_NoAlias() {
    String result = SqlColumnParser.removeTableAlias("uidlevel2");
    assertEquals("uidlevel2", result);
  }

  @Test
  void testRemoveTableAlias_EmptyString() {
    String result = SqlColumnParser.removeTableAlias("");
    assertEquals("", result);
  }

  @Test
  void testRemoveTableAlias_NullInput() {
    String result = SqlColumnParser.removeTableAlias(null);
    assertNull(result);
  }

  @Test
  void testRemoveTableAlias_ComplexColumnName() {
    String result = SqlColumnParser.removeTableAlias("schema.table.\"complex.column.name\"");
    assertEquals("complex.column.name", result);
  }

  @Test
  void testRemoveTableAlias_MultipleDots() {
    String result = SqlColumnParser.removeTableAlias("schema.table.column");
    assertEquals("column", result);
  }
}
