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
package org.hisp.dhis.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.util.TextUtils;

@UtilityClass
public class AnalyticsStringUtils {

  /**
   * Converts the given list of items to a comma-separated string, using the given mapping function
   * to map the object to string.
   *
   * @param <T> the type.
   * @param list the list.
   * @param mapper the mapping function.
   * @return a comma-separated string.
   */
  public static <T> String toCommaSeparated(List<T> list, Function<T, String> mapper) {
    return list.stream().map(mapper).collect(Collectors.joining(","));
  }

  /**
   * Replaces variables in the given template string.
   *
   * <p>Variables which are present in the given template and in the given map of variables are
   * replaced with the corresponding map value.
   *
   * <p>Variables which are present in the given template but not present in the given map of
   * variables will be qualified using <code>qualify(String)</code>.
   *
   * @param template the template string.
   * @param variables the map of variable names and values.
   * @return a string with replaced variables.
   */
  public static String replaceQualify(
      SqlBuilder sqlBuilder, String template, Map<String, String> variables) {
    Map<String, String> map = new HashMap<>(variables);
    Set<String> variableNames = TextUtils.getVariableNames(template);
    variableNames.forEach(name -> map.putIfAbsent(name, sqlBuilder.qualifyTable(name)));

    return TextUtils.replace(template, map);
  }

  /**
   * Qualifies variables in the given template string using the variable name as table name.
   *
   * @param template the template string.
   * @return a string with qualified table names.
   */
  public static String qualifyVariables(SqlBuilder sqlBuilder, String template) {
    return replaceQualify(sqlBuilder, template, Map.of());
  }
}
