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

public enum OutlierExpressionHelper {
  /**
   * Regex pattern to identify strings that are valid for casting to PostgreSQL `double precision`
   * (i.e., plain numeric values).
   *
   * <p>Pattern: ^-?[0-9]+(\.[0-9]+)?$
   *
   * <p>Matches: - "42" - "-3.14" - "0.5" - "0001.00"
   *
   * <p>Does not match: - "+5" - ".5" - "42." - "1e5" - " 5.5 " - "1,000.00"
   *
   * <p>This pattern is used to pre-filter text-based numeric values to avoid runtime casting
   * exceptions when converting to double precision. Since data values are stored as strings in the
   * database, there is no guarantee that the string representation of a number is valid for casting
   * to double precision. Some edge cases may not be covered by this pattern, but integrity checks
   * can help to identify such cases and fix them.
   */
  NUMERIC_PATTERN("^-?[0-9]+(\\.[0-9]+)?$");
  private String key;

  OutlierExpressionHelper(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
