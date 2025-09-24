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

import java.util.Optional;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ColumnAliasUtils {

  /**
   * Splits a column reference into its qualifier and column name when the reference is fully
   * qualified.
   *
   * <p>The method accepts identifiers written as {@code qualifier.column}, including quoted
   * variants, and returns an {@link Optional} containing both pieces. If the input is blank,
   * contains leading or trailing whitespace, or represents something other than a plain qualified
   * column, {@link Optional#empty()} is returned.
   *
   * @param columnRef textual column reference, e.g. {@code analytics.value}
   * @return optional qualifier/column pair when the reference is qualified; empty otherwise
   */
  public static Optional<QualifiedRef> splitQualified(String columnRef) {
    Column col = parseColumnOrNull(columnRef);
    if (col == null) return Optional.empty();

    Table table = col.getTable();
    String q = (table != null) ? table.getFullyQualifiedName() : null;
    if (q == null || q.isBlank()) return Optional.empty();

    // Preserve quotes in both qualifier and column name
    return Optional.of(new QualifiedRef(q, col.getColumnName()));
  }

  private static Column parseColumnOrNull(String text) {
    if (StringUtils.isBlank(text)) return null;
    try {
      // Parse as an expression and check it's a Column
      Expression expr = CCJSqlParserUtil.parseExpression(text);
      if (expr instanceof Column) {
        return (Column) expr;
      }
      // Not a plain column reference (could be a function, arithmetic, etc.)
      return null;
    } catch (JSQLParserException e) {
      // Not parseable as an expression — treat as not-a-column
      return null;
    }
  }

  public record QualifiedRef(String qualifier, String columnName) {}
}
