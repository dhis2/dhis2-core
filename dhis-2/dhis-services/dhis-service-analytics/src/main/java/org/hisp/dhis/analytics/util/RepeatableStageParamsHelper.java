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
package org.hisp.dhis.analytics.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.RepeatableStageParams;

@NoArgsConstructor(access = PRIVATE)
public final class RepeatableStageParamsHelper {
  /** Matches indexes like [-1]. */
  private static final String PS_INDEX_REGEX = "\\[-?\\d+\\]";

  private static final Pattern PATTERN_INDEX_REGEX = Pattern.compile(PS_INDEX_REGEX);

  /**
   * Returns a {@link RepeatableStageParams} instance based on the given dimension.
   *
   * @param dimension the dimension param. ie: EPEcjy3FWmI[0].fCXKBdc27Bt.
   * @return the {@link RepeatableStageParams}.
   */
  public static RepeatableStageParams getRepeatableStageParams(@Nonnull String dimension) {
    Matcher matcher = PATTERN_INDEX_REGEX.matcher(dimension);
    boolean patternMatches = matcher.find();

    if (patternMatches) {
      String index = matcher.group(0).replace("[", "").replace("]", "");
      return RepeatableStageParams.of(Integer.parseInt(index), dimension);
    }

    return RepeatableStageParams.of(dimension);
  }

  /**
   * Removes the offset of the repeatable stage, if any.
   *
   * @param dimension ie: EPEcjy3FWmI[0].fCXKBdc27Bt.
   * @return dimension without params like edqlbukwRfQ[1].vANAXwtLwcT -> edqlbukwRfQ.vANAXwtLwcT
   */
  public static String removeRepeatableStageParams(@Nonnull String dimension) {
    Matcher matcher = PATTERN_INDEX_REGEX.matcher(dimension);

    if (matcher.find()) {
      return dimension.replace(matcher.group(0), "");
    }

    return dimension;
  }
}
