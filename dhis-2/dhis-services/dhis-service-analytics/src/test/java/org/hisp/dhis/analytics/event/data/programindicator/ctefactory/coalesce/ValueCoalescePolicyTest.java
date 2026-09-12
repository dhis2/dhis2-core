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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory.coalesce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.model.DataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verifies that the coalesce default rendered for a value type is compatible with the analytics
 * column type that {@link AnalyticsUtils#getColumnType} creates for that same value type.
 */
class ValueCoalescePolicyTest {

  @Test
  void trueOnlyUsesNumericDefault() {
    assertEquals("coalesce(a.value, 0)", ValueCoalescePolicy.from(ValueType.TRUE_ONLY).render("a"));
  }

  @Test
  void booleanUsesNumericDefault() {
    assertEquals("coalesce(a.value, 0)", ValueCoalescePolicy.from(ValueType.BOOLEAN).render("a"));
  }

  @Test
  void ageUsesNoDefault() {
    assertEquals("a.value", ValueCoalescePolicy.from(ValueType.AGE).render("a"));
  }

  @Test
  void dateUsesNoDefault() {
    assertEquals("a.value", ValueCoalescePolicy.from(ValueType.DATE).render("a"));
    assertEquals("a.value", ValueCoalescePolicy.from(ValueType.DATETIME).render("a"));
  }

  @Test
  void numericTypesUseZeroDefault() {
    assertEquals("coalesce(a.value, 0)", ValueCoalescePolicy.from(ValueType.INTEGER).render("a"));
    assertEquals("coalesce(a.value, 0)", ValueCoalescePolicy.from(ValueType.NUMBER).render("a"));
    assertEquals(
        "coalesce(a.value, 0)", ValueCoalescePolicy.from(ValueType.PERCENTAGE).render("a"));
    assertEquals(
        "coalesce(a.value, 0)", ValueCoalescePolicy.from(ValueType.UNIT_INTERVAL).render("a"));
  }

  @Test
  void textTypesUseEmptyStringDefault() {
    assertEquals("coalesce(a.value, '')", ValueCoalescePolicy.from(ValueType.TEXT).render("a"));
    assertEquals(
        "coalesce(a.value, '')", ValueCoalescePolicy.from(ValueType.LONG_TEXT).render("a"));
  }

  /**
   * The coalesce default must never be a text literal when the analytics column is numeric, nor a
   * numeric literal when the column is text, otherwise the database rejects the query at bind time.
   */
  @ParameterizedTest
  @EnumSource(ValueType.class)
  void defaultMatchesAnalyticsColumnType(ValueType valueType) {
    DataType columnType = AnalyticsUtils.getColumnType(valueType, false);
    String rendered = ValueCoalescePolicy.from(valueType).render("a");

    switch (columnType) {
      case INTEGER, BIGINT, DOUBLE -> assertEquals("coalesce(a.value, 0)", rendered);
      case TIMESTAMP -> assertEquals("a.value", rendered);
      case TEXT -> assertEquals("coalesce(a.value, '')", rendered);
      default -> {
        // no expectation for column types not produced by scalar value types
      }
    }
  }
}
