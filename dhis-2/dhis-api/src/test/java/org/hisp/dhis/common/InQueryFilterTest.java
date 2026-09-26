/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InQueryFilterTest {

  @Test
  void verifyInWithTextParameters() {
    executeTest("aFilter1;aFilter2", true, false, "aField in ('aFilter1','aFilter2') ");
  }

  @Test
  void verifyInWithNumberParameters() {
    executeTest("1;2;3", false, false, "aField in (1,2,3) ");
  }

  @Test
  @DisplayName("Non-option-set: NV is the no-value keyword (mixed with text)")
  void verifyNonOptionSetNvAndText() {
    executeTest("NV;aFilter1", true, false, "(aField in ('aFilter1') or aField is null ) ");
  }

  @Test
  @DisplayName("Non-option-set: NV is the no-value keyword (mixed with number)")
  void verifyNonOptionSetNvAndNumber() {
    executeTest("NV;1", false, false, "(aField in (1) or aField is null ) ");
  }

  @Test
  @DisplayName("Non-option-set: NV only")
  void verifyNonOptionSetNvOnly() {
    executeTest("NV", true, false, "aField is null ");
  }

  @Test
  @DisplayName("Option-set: D2__NOVALUE is the no-value keyword (mixed with code)")
  void verifyOptionSetNoValueAndText() {
    executeTest("D2__NOVALUE;aFilter1", true, true, "(aField in ('aFilter1') or aField is null ) ");
  }

  @Test
  @DisplayName("Option-set: D2__NOVALUE only")
  void verifyOptionSetNoValueOnly() {
    executeTest("D2__NOVALUE", true, true, "aField is null ");
  }

  @Test
  @DisplayName("Option-set: NV is a literal option code, not no-value")
  void verifyOptionSetNvIsLiteral() {
    executeTest("NV;aFilter1", true, true, "aField in ('NV','aFilter1') ");
  }

  @Test
  void verifyNestedSqlStmtInFieldWithNullOnly() {
    String field = "(select * from xy)";
    executeTest(
        field, "NV", true, false, "(" + field + " is null and exists((select * from xy))) ");
  }

  private void executeTest(
      String filterValue, boolean shouldQuote, boolean isOptionSet, String expected) {
    executeTest("aField", filterValue, shouldQuote, isOptionSet, expected);
  }

  private void executeTest(
      String field, String filterValue, boolean shouldQuote, boolean isOptionSet, String expected) {
    assertEquals(
        new InQueryFilter(field, filterValue, shouldQuote, isOptionSet).getSqlFilter(), expected);
  }
}
