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

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ColumnUtils {

  /**
   * Splits a string containing SQL column definitions separated by newlines into a list of
   * individual columns.
   *
   * <p>This method processes legacy SQL column strings where each column definition is on a
   * separate line, potentially ending with a comma. The method performs the following operations:
   *
   * <ul>
   *   <li>Splits the input string on newline characters (\n)
   *   <li>Trims leading and trailing whitespace from each line
   *   <li>Removes trailing commas from each column definition
   *   <li>Filters out empty lines
   * </ul>
   *
   * @param columns the string containing column definitions separated by newlines, may be null or
   *     empty
   * @return a list of trimmed column definitions without trailing commas; returns an empty list if
   *     input is null or empty
   *     <pre>
   * String input = "column_a,\ncolumn_b,\ncolumn_c";
   * List&lt;String&gt; result = ColumnUtils.splitColumns(input);
   * // result: ["column_a", "column_b", "column_c"]
   * </pre>
   */
  public static List<String> splitColumns(String columns) {
    if (columns == null || columns.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> result = new ArrayList<>();
    String[] lines = columns.split("\n");

    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        // Remove trailing comma if present
        if (trimmed.endsWith(",")) {
          trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        // Only add if still not empty after comma removal
        if (!trimmed.isEmpty()) {
          result.add(trimmed);
        }
      }
    }

    return result;
  }
}
