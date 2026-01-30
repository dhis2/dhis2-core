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
package org.hisp.dhis.analytics.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/** Renders SQL statements with named parameters replaced by their values. */
@UtilityClass
public final class SqlRenderer {

  private static final Pattern PARAM_PATTERN = Pattern.compile(":(\\w*|\\d+)");

  /**
   * Renders a SQL statement with named parameters replaced by their values.
   *
   * @param sql the SQL statement with named parameters
   * @param params the parameter values
   * @return the SQL statement with parameters substituted
   */
  public static String render(String sql, Map<String, Object> params) {
    Matcher matcher = PARAM_PATTERN.matcher(sql);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String paramName = matcher.group(1);
      if (!params.containsKey(paramName)) {
        matcher.appendReplacement(sb, Matcher.quoteReplacement(":" + paramName));
        continue;
      }
      Object value = params.get(paramName);
      String replacement = value == null ? "null" : formatValue(value);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static String formatValue(Object value) {
    if (value instanceof String) {
      return "'" + value + "'";
    }
    if (value instanceof LocalDate localDate) {
      return "'" + localDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "'";
    }
    if (value instanceof LocalDateTime localDateTime) {
      return "'" + localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "'";
    }
    if (value instanceof java.util.Date date) {
      Instant instant = date.toInstant();
      LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
      return "'" + ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "'";
    }
    if (value instanceof Iterable) {
      return value.toString();
    }
    return String.valueOf(value);
  }
}
