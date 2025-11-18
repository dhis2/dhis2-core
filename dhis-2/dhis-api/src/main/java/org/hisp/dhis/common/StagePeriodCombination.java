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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.TimeField;

/**
 * Represents a combination of program stage and period for event analytics queries.
 *
 * <p>Used to support the eventDate=stage.period syntax, e.g., eventDate=Zj7UnCAulEk.LAST_12_MONTHS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StagePeriodCombination {
  /** The program stage UID */
  private String stageUid;

  /** The period expression (e.g., LAST_12_MONTHS, THIS_YEAR, 202201) */
  private String period;

  /** The time field to apply the period filter to */
  private TimeField timeField;

  /**
   * Validates that this combination is well-formed.
   *
   * @throws IllegalQueryException if validation fails
   */
  public void validate() {
    if (StringUtils.isBlank(stageUid)) {
      throw new IllegalQueryException("Stage UID cannot be blank in stage.period combination");
    }
    if (StringUtils.isBlank(period)) {
      throw new IllegalQueryException("Period cannot be blank in stage.period combination");
    }
    if (timeField == null) {
      throw new IllegalQueryException("Time field cannot be null in stage.period combination");
    }
  }
}
