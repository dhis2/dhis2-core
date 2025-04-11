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
package org.hisp.dhis.outlierdetection.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Jason P. Pickering
 */
class OutlierExpressionHelperTest {

  private static final Pattern NUMERIC_PATTERN =
      Pattern.compile(OutlierExpressionHelper.NUMERIC_PATTERN.getKey());

  @Test
  void shouldMatchValidNumbers() {
    assertTrue(matches("42"));
    assertTrue(matches("0"));
    assertTrue(matches("-123"));
    assertTrue(matches("3.14"));
    assertTrue(matches("0.0"));
    assertTrue(matches("0001.00"));
    assertTrue(matches("-0.5"));
  }

  @Test
  void shouldNotMatchInvalidNumbers() {
    assertFalse(matches("+42"));
    assertFalse(matches(".5"));
    assertFalse(matches("42."));
    assertFalse(matches("1e5"));
    assertFalse(matches("1,000"));
    assertFalse(matches("abc"));
    assertFalse(matches(""));
    assertFalse(matches(null));
  }

  private boolean matches(String value) {
    if (value == null) return false;
    return NUMERIC_PATTERN.matcher(value.trim()).matches();
  }
}
