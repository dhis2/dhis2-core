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

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.ValueType;

@RequiredArgsConstructor
public enum ValueCoalescePolicy {
  NUMBER("0"),
  // use 0 for all boolean values, since "yes/no" data type is converted to 0/1 in
  // analytics tables
  BOOLEAN("0"),
  TEXT("''"),
  DATE(null);

  private final String defaultSqlLiteral;

  /**
   * Renders either <code>alias.value</code> or <code>coalesce(alias.value,&nbsp;default)</code>.
   */
  public String render(String alias) {
    if (defaultSqlLiteral == null) {
      return alias + ".value";
    }
    return "coalesce(" + alias + ".value, " + defaultSqlLiteral + ")";
  }

  /** Maps DHIS2 {@link ValueType} → policy. */
  public static ValueCoalescePolicy from(ValueType vt) {
    return switch (vt) {
      case INTEGER,
          NUMBER,
          INTEGER_POSITIVE,
          INTEGER_NEGATIVE,
          INTEGER_ZERO_OR_POSITIVE,
          PERCENTAGE,
          UNIT_INTERVAL ->
          NUMBER;
      case BOOLEAN -> BOOLEAN;
      case DATE, DATETIME -> DATE;
      default -> TEXT;
    };
  }
}
