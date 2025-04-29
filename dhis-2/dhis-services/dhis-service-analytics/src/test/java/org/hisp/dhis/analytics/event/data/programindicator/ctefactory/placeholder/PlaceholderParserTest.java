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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceholderParserTest {

  // ------------------------------------------------------------------
  // PS / DE placeholders
  // ------------------------------------------------------------------

  @Nested
  class PsDe {

    @Test
    void parse_valid_returnsFields() {
      String p =
          "__PSDE_CTE_PLACEHOLDER__(psUid='PS1', deUid='DE1', offset='3', "
              + "boundaryHash='abc123', piUid='PI1')";
      Optional<PlaceholderParser.PsDeFields> opt = PlaceholderParser.parsePsDe(p);

      assertTrue(opt.isPresent());
      PlaceholderParser.PsDeFields f = opt.get();
      assertEquals("PS1", f.psUid());
      assertEquals("DE1", f.deUid());
      assertEquals(3, f.offset());
      assertEquals("abc123", f.boundaryHash());
      assertEquals("PI1", f.piUid());
    }

    @Test
    void parse_malformed_returnsEmpty() {
      String missingParen = "__PSDE_CTE_PLACEHOLDER__(psUid='PS1', deUid='DE1'";
      assertTrue(PlaceholderParser.parsePsDe(missingParen).isEmpty());
    }

    @Test
    void pattern_findsMultipleInSqlBlob() {
      String sql =
          "x "
              + "__PSDE_CTE_PLACEHOLDER__(psUid='PS1', deUid='A', offset='0', boundaryHash='h', piUid='PI')"
              + " + y + "
              + "__PSDE_CTE_PLACEHOLDER__(psUid='PS2', deUid='B', offset='1', boundaryHash='h', piUid='PI')";
      Matcher m = PlaceholderParser.psDePattern().matcher(sql);
      int count = 0;
      while (m.find()) count++;
      assertEquals(2, count);
    }
  }

  // ------------------------------------------------------------------
  // Simple filter placeholders
  // ------------------------------------------------------------------

  @Nested
  class Filter {

    @Test
    void parse_valid_returnsFields() {
      String expr = "V{event_date} >= '2025-01-01'";
      Optional<PlaceholderParser.FilterFields> opt = PlaceholderParser.parseFilter(expr);

      assertTrue(opt.isPresent());
      PlaceholderParser.FilterFields f = opt.get();
      assertEquals("event_date", f.variableName());
      assertEquals(">=", f.operator());
      assertEquals("2025-01-01", f.literal());
    }

    @Test
    void parse_invalid_returnsEmpty() {
      assertTrue(PlaceholderParser.parseFilter("event_date >= '2025'").isEmpty());
    }
  }

  // ------------------------------------------------------------------
  // D2 function placeholders
  // ------------------------------------------------------------------

  @Nested
  class D2Func {

    @Test
    void parse_valid_returnsFields() {
      String value = "42";
      String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
      String placeholder =
          "__D2FUNC__(func='countIfValue', ps='PS', de='DE', argType='val64', "
              + "arg64='"
              + encoded
              + "', hash='h1', pi='PI')__";

      Optional<PlaceholderParser.D2FuncFields> opt = PlaceholderParser.parseD2Func(placeholder);

      assertTrue(opt.isPresent());
      PlaceholderParser.D2FuncFields f = opt.get();

      assertEquals("countIfValue", f.func());
      assertEquals("PS", f.psUid());
      assertEquals("DE", f.deUid());
      assertEquals(encoded, f.valueSql());
      assertEquals("h1", f.boundaryHash());
      assertEquals("PI", f.piUid());
    }

    @Test
    void parse_badBase64_returnsEmpty() {
      String bad =
          "__D2FUNC__(func='countIfValue', ps='P', de='D', " + "val64='***', hash='h', pi='PI')__";
      assertTrue(PlaceholderParser.parseD2Func(bad).isEmpty());
    }

    @Test
    void parse_nonMatching_returnsEmpty() {
      assertTrue(PlaceholderParser.parseD2Func("__D2FUNC__(missing)__").isEmpty());
    }
  }

  // ------------------------------------------------------------------
  // Variable placeholders
  // ------------------------------------------------------------------

  @Nested
  class Variable {

    @Test
    void parse_valid_noPsUid_returnsFields() {
      String p =
          "FUNC_CTE_VAR(type='vEventDate', column='occurreddate', "
              + "piUid='PI', psUid='null', offset='0')";
      Optional<PlaceholderParser.VariableFields> opt = PlaceholderParser.parseVariable(p);

      assertTrue(opt.isPresent());
      PlaceholderParser.VariableFields f = opt.get();
      assertEquals("vEventDate", f.type());
      assertEquals("occurreddate", f.column());
      assertEquals("PI", f.piUid());
      assertNull(f.psUid());
      assertEquals(0, f.offset());
    }

    @Test
    void parse_valid_withPsUid_returnsFields() {
      String p =
          "FUNC_CTE_VAR(type='vCompletedDate', column='compdate', "
              + "piUid='PI', psUid='PS1', offset='-2')";
      Optional<PlaceholderParser.VariableFields> opt = PlaceholderParser.parseVariable(p);

      assertTrue(opt.isPresent());
      PlaceholderParser.VariableFields f = opt.get();
      assertEquals("PS1", f.psUid());
      assertEquals(-2, f.offset());
    }

    @Test
    void parse_invalid_returnsEmpty() {
      assertTrue(PlaceholderParser.parseVariable("FUNC_CTE_VAR(broken)").isEmpty());
    }
  }
}
