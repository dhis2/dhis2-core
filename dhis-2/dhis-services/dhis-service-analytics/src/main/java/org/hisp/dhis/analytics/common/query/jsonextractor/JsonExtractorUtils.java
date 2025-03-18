/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.common.query.jsonextractor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.util.DateUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonExtractorUtils {

  private static final Pattern TRAILING_ZEROES = Pattern.compile("0*$");
  private static final Pattern ENDING_WITH_DOT = Pattern.compile("\\.$");

  public static String getFormattedDate(LocalDateTime date) {
    if (date == null) {
      return null;
    }

    return withOutTrailingZeroes(
        DateUtils.toLongDateNoT(Date.from(date.atZone(ZoneId.systemDefault()).toInstant())));
  }

  /**
   * Removes trailing zeroes from the date string Examples: - "2020-01-01T00:00:00.000" ->
   * "2020-01-01T00:00:00.0" - "2020-01-01T00:00:00.100" -> "2020-01-01T00:00:00.1" -
   * "2020-01-01T00:00:00.010" -> "2020-01-01T00:00:00.01" - "2020-01-01T00:00:00.001" ->
   * "2020-01-01T00:00:00.001"
   *
   * @param date date string
   * @return date string without trailing zeroes
   */
  private static String withOutTrailingZeroes(String date) {
    return ENDING_WITH_DOT.matcher(TRAILING_ZEROES.matcher(date).replaceAll("")).replaceAll(".0");
  }
}
