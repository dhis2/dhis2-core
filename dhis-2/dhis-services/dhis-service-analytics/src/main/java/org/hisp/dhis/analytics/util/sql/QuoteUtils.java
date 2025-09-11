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
package org.hisp.dhis.analytics.util.sql;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class QuoteUtils {

  /**
   * Removes surrounding quotes (double quotes <code>"</code> or backticks <code>`</code>) from a
   * string. If the string is not quoted or the quotes do not match, the original string is returned
   * unchanged.
   *
   * <p><b>Behavior:</b>
   *
   * <ul>
   *   <li>Returns an empty string if the input is <code>null</code> or empty.
   *   <li>Returns the original string if it is too short to have matching quotes (less than 2
   *       characters).
   *   <li>Removes quotes only if the first and last characters are matching quotes (either <code>"
   *       </code> or <code>`</code>).
   *   <li>Returns the original string if the quotes do not match or if the string is not quoted.
   * </ul>
   *
   * <p><b>Examples:</b>
   *
   * <ul>
   *   <li><code>unquote("\"quoted\"")</code> returns <code>"quoted"</code>
   *   <li><code>unquote("`backticked`")</code> returns <code>"backticked"</code>
   *   <li><code>unquote("not quoted")</code> returns <code>"not quoted"</code>
   *   <li><code>unquote("\"mismatch`")</code> returns <code>"\"mismatch`"</code>
   *   <li><code>unquote("")</code> returns <code>""</code>
   *   <li><code>unquote(null)</code> returns <code>""</code>
   * </ul>
   *
   * @param quoted The string from which to remove quotes. Can be <code>null</code> or empty.
   * @return The string without surrounding quotes, or the original string if it is not quoted.
   *     Returns an empty string if the input is <code>null</code> or empty.
   */
  public static String unquote(String quoted) {
    if (StringUtils.isBlank(quoted)) {
      return "";
    }

    String betweenDoubleQuotes = StringUtils.substringBetween(quoted, "\"", "\"");
    if (betweenDoubleQuotes != null) {
      return betweenDoubleQuotes;
    }

    String betweenBackticks = StringUtils.substringBetween(quoted, "`", "`");
    if (betweenBackticks != null) {
      return betweenBackticks;
    }

    return quoted;
  }
}
