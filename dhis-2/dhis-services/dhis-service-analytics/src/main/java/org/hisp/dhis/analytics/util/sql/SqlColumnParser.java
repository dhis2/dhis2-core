/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;

public class SqlColumnParser {

  /**
   * Removes table alias from a SQL column reference using JSqlParser. Handles quoted column names
   * and complex SQL expressions.
   *
   * @param columnReference The SQL column reference (e.g., "ax.uidlevel2", "test1.`alfa`")
   * @return The column name without the table alias (e.g., "uidlevel2", "alfa")
   */
  public static String removeTableAlias(String columnReference) {
    if (columnReference == null || columnReference.isEmpty()) {
      return columnReference;
    }

    try {
      // Parse the column reference using JSqlParser
      Expression expression = CCJSqlParserUtil.parseExpression(columnReference);

      // Ensure the parsed expression is a Column
      if (!(expression instanceof Column column)) {
        throw new IllegalArgumentException(
            "Input is not a valid SQL column reference: " + columnReference);
      }

      // Extract the column name
      return unquote(column.getColumnName());
    } catch (Exception e) {
      throw new RuntimeException("Error parsing SQL: " + e.getMessage(), e);
    }
  }

  // FIXME - this method is duplicated in SqlWhereClauseExtractor
  private static String unquote(String quoted) {
    // Handle null or empty
    if (quoted == null || quoted.isEmpty()) {
      return "";
    }

    // Check minimum length (needs at least 2 chars for quotes)
    if (quoted.length() < 2) {
      return quoted;
    }

    char firstChar = quoted.charAt(0);
    char lastChar = quoted.charAt(quoted.length() - 1);

    // Check if quotes match
    if ((firstChar == '"' && lastChar == '"') || (firstChar == '`' && lastChar == '`')) {
      return quoted.substring(1, quoted.length() - 1);
    }

    return quoted;
  }
}
