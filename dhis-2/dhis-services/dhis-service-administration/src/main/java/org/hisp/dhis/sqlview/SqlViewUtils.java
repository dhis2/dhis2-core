/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.sqlview;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lars Helge Overland
 */
public class SqlViewUtils {
  private SqlViewUtils() {
    // hide
  }

  private static final String VARIABLE_EXPRESSION = "\\$\\{([^}]+)}";

  private static final Pattern VARIABLE_PATTERN =
      Pattern.compile(VARIABLE_EXPRESSION, Pattern.DOTALL);

  /**
   * Returns the variables contained in the given SQL string.
   *
   * @param sql the SQL query string.
   * @return a set of variable keys.
   */
  public static Set<String> getVariables(String sql) {
    Set<String> variables = new HashSet<>();

    Matcher matcher = VARIABLE_PATTERN.matcher(sql);

    while (matcher.find()) {
      variables.add(matcher.group(1));
    }

    return variables;
  }

  /**
   * Substitutes the given SQL query string with the given user-supplied variables. SQL variables
   * are of the format ${key}.
   *
   * @param sql the SQL string.
   * @param variables the variables.
   * @return the substituted SQL.
   */
  public static String substituteSqlVariables(String sql, Map<String, String> variables) {
    String sqlQuery = sql;

    if (variables != null) {
      for (Entry<String, String> param : variables.entrySet()) {
        String name = param.getKey();
        if (SqlView.isValidQueryParam(name)) {
          sqlQuery = substituteSqlVariable(sqlQuery, name, param.getValue());
        }
      }
    }

    return sqlQuery;
  }

  /**
   * Substitutes the given SQL query string with the given, single variable. SQL variables are of
   * the format ${key}.
   *
   * @param sql the SQL string.
   * @param name the variable name.
   * @param value the variable value.
   * @return the substituted SQL.
   */
  public static String substituteSqlVariable(String sql, String name, String value) {
    if (SqlView.isValidQueryValue(value)) {
      return sql.replace("${" + name + "}", value);
    }

    return sql;
  }

  /**
   * Removes all query separators ";" in the given SQL.
   *
   * @param sql the SQL.
   * @return the replaced SQL.
   */
  public static String removeQuerySeparator(String sql) {
    return sql.replace(";", "");
  }
}
