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
package org.hisp.dhis.resourcetable.util;

import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnNameUtils {
  private static final Pattern PAT_COL_NAME_CHARS =
      Pattern.compile("[.a-zA-Z0-9 _+\\-/?@#$%^&\\*\",:]");
  private static final int MAX_COL_NAME_LENGTH = 128;
  private static final char REPLACEMENT_CHAR = '_';

  /**
   * Sanitizes the given input by replacing characters which are not matching any of the valid
   * characters as specified by the given pattern with the given replacement character. Consecutive
   * replacement characters are merged into a single character. Any leading replacement characters
   * are removed.
   *
   * @param input the column name.
   */
  public static String toValidColumnName(String input) {
    if (StringUtils.isEmpty(input)) {
      return input;
    }

    String string = TextUtils.sanitize(PAT_COL_NAME_CHARS, input, REPLACEMENT_CHAR);
    return StringUtils.substring(string, 0, MAX_COL_NAME_LENGTH);
  }
}
