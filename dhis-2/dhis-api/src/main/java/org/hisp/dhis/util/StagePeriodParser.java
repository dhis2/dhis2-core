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
package org.hisp.dhis.util;

import static org.hisp.dhis.feedback.ErrorCode.E7242;
import static org.hisp.dhis.feedback.ErrorCode.E7243;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.StagePeriodCombination;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Utility class for parsing stage.period syntax in event analytics queries.
 *
 * <p>Supports formats like:
 *
 * <ul>
 *   <li>Single: eventDate=stageUid.LAST_12_MONTHS
 *   <li>Multiple: eventDate=stageUid1.THIS_YEAR;stageUid2.LAST_MONTH
 * </ul>
 *
 * @author Luciano Fiandesio
 */
public class StagePeriodParser {

  private static final String STAGE_PERIOD_SEPARATOR = ".";
  private static final String COMBINATION_SEPARATOR = ";";

  private StagePeriodParser() {}

  /**
   * Parses a stage.period string into a list of StagePeriodCombination objects.
   *
   * @param value the value to parse (e.g., "stageUid.LAST_12_MONTHS" or
   *     "stageUid1.THIS_YEAR;stageUid2.LAST_MONTH")
   * @param timeField the time field to associate with the combinations
   * @return list of parsed StagePeriodCombination objects
   * @throws IllegalQueryException if the format is invalid or duplicate stages are found
   */
  public static List<StagePeriodCombination> parse(String value, TimeField timeField) {
    if (StringUtils.isBlank(value)) {
      return List.of();
    }

    List<StagePeriodCombination> combinations = new ArrayList<>();
    Set<String> seenStages = new HashSet<>();

    // Split by semicolon for multiple combinations
    String[] parts = value.split(COMBINATION_SEPARATOR);

    for (String part : parts) {
      part = part.trim();

      if (StringUtils.isBlank(part)) {
        continue;
      }

      // Each part must be in format "stageUid.period"
      if (!part.contains(STAGE_PERIOD_SEPARATOR)) {
        throw new IllegalQueryException(new ErrorMessage(E7243));
      }

      String[] stagePeriod = part.split("\\" + STAGE_PERIOD_SEPARATOR, 2);

      if (stagePeriod.length != 2) {
        throw new IllegalQueryException(new ErrorMessage(E7243));
      }

      String stageUid = stagePeriod[0].trim();
      String period = stagePeriod[1].trim();

      if (StringUtils.isBlank(stageUid) || StringUtils.isBlank(period)) {
        throw new IllegalQueryException(new ErrorMessage(E7243));
      }

      // Check for duplicate stages
      if (seenStages.contains(stageUid)) {
        throw new IllegalQueryException(new ErrorMessage(E7242, stageUid));
      }

      seenStages.add(stageUid);

      StagePeriodCombination combination = new StagePeriodCombination(stageUid, period, timeField);
      combination.validate();
      combinations.add(combination);
    }

    if (combinations.isEmpty()) {
      throw new IllegalQueryException(new ErrorMessage(E7243));
    }

    return combinations;
  }

  /**
   * Checks if the given value has the stage.period format.
   *
   * @param value the value to check
   * @return true if the value contains a period separator indicating stage.period format
   */
  public static boolean hasStagePeriodFormat(String value) {
    return StringUtils.isNotBlank(value) && value.contains(STAGE_PERIOD_SEPARATOR);
  }
}
