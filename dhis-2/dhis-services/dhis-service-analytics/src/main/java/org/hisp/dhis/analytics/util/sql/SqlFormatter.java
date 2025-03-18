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

import java.util.Set;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SqlFormatter {
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  private static final Set<String> MAIN_CLAUSES =
      Set.of(
          "with",
          "select",
          "from",
          "left join",
          "where",
          "group by",
          "having",
          "order by",
          "limit",
          "offset");

  private static final Set<String> SQL_KEYWORDS =
      Set.of(
          "WITH",
          "AS",
          "SELECT",
          "FROM",
          "LEFT JOIN",
          "ON",
          "WHERE",
          "GROUP BY",
          "HAVING",
          "ORDER BY",
          "LIMIT",
          "OFFSET",
          "AND",
          "OR",
          "NOT",
          "DESC",
          "ASC",
          "NULLS FIRST",
          "NULLS LAST",
          "CASE",
          "WHEN",
          "THEN",
          "ELSE",
          "END");

  public static String prettyPrint(String sql) {
    // First lowercase all SQL keywords
    String formattedSql = lowercase(sql);

    // Add newlines before main clauses
    for (String clause : MAIN_CLAUSES) {
      formattedSql = formattedSql.replace(" " + clause + " ", "\n" + clause + " ");
    }

    // Handle subqueries and CTEs
    formattedSql = formatParentheses(formattedSql);

    // Indent lines
    String[] lines = formattedSql.split("\n");
    StringBuilder result = new StringBuilder();
    int indent = 0;

    for (String line : lines) {
      String trimmedLine = line.trim();
      // Decrease indent if line starts with closing parenthesis
      if (trimmedLine.startsWith(")")) {
        indent--;
      }
      // Add indentation
      result.append("  ".repeat(Math.max(0, indent))).append(trimmedLine).append("\n");
      // Increase indent if line ends with opening parenthesis
      if (trimmedLine.endsWith("(")) {
        indent++;
      }
    }

    return result.toString().trim();
  }

  /**
   * Converts SQL keywords to lowercase and formats the SQL string into a single line. Preserves
   * single spaces between words and removes extra whitespace.
   *
   * @param sql the SQL string to format
   * @return formatted SQL string in a single line with lowercase keywords
   */
  public static String lowercase(String sql) {
    String result = sql;

    // Convert keywords to lowercase
    for (String keyword : SQL_KEYWORDS) {
      // Use word boundaries to only replace complete words
      result = result.replaceAll("\\b" + keyword + "\\b", keyword.toLowerCase());
    }

    // Replace all whitespace sequences (including newlines) with a single space
    result = WHITESPACE_PATTERN.matcher(result).replaceAll(" ");

    return result.trim();
  }

  private static String formatParentheses(String sql) {
    StringBuilder result = new StringBuilder();
    int indent = 0;
    boolean inString = false;
    char[] chars = sql.toCharArray();

    for (char c : chars) {
      // Handle string literals
      if (c == '\'') {
        inString = !inString;
        result.append(c);
        continue;
      }

      if (!inString) {
        if (c == '(') {
          // Add newline and indent after opening parenthesis
          result.append("(\n").append("  ".repeat(++indent));
          continue;
        } else if (c == ')') {
          // Add newline and indent before closing parenthesis
          result.append("\n").append("  ".repeat(--indent)).append(")");
          continue;
        } else if (c == ',') {
          // Add newline after comma (for lists of columns, etc.)
          result.append(",\n").append("  ".repeat(indent));
          continue;
        }
      }

      result.append(c);
    }

    return result.toString();
  }
}
