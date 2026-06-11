/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultEventQueryValidatorTest {

  private final DefaultEventQueryValidator validator = new DefaultEventQueryValidator(null);

  @Test
  @DisplayName("D2__NOVALUE on a non-option-set dimension is rejected")
  void rejectsNoValueKeywordOnNonOptionSet() {
    ErrorMessage error =
        validator.validateQueryFilter(new QueryFilter(IN, "D2__NOVALUE"), ValueType.NUMBER, false);
    assertEquals(ErrorCode.E7246, error.getErrorCode());
  }

  @Test
  @DisplayName("D2__NOVALUE with EQ on a non-option-set dimension is rejected")
  void rejectsNoValueKeywordWithEqOnNonOptionSet() {
    ErrorMessage error =
        validator.validateQueryFilter(new QueryFilter(EQ, "D2__NOVALUE"), ValueType.NUMBER, false);
    assertEquals(ErrorCode.E7246, error.getErrorCode());
  }

  @Test
  @DisplayName("D2__NOVALUE on an option-set dimension is allowed")
  void allowsNoValueKeywordOnOptionSet() {
    assertNull(
        validator.validateQueryFilter(new QueryFilter(IN, "D2__NOVALUE"), ValueType.TEXT, true));
  }

  @Test
  @DisplayName("NV on a non-option-set dimension is allowed (legacy no-value keyword)")
  void allowsNvOnNonOptionSet() {
    assertNull(validator.validateQueryFilter(new QueryFilter(IN, "NV"), ValueType.NUMBER, false));
  }
}
