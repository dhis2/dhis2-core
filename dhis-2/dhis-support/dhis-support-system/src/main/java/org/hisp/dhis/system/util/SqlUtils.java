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
package org.hisp.dhis.system.util;

import com.google.common.collect.Sets;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;

/**
 * Utilities for SQL operations, compatible with PostgreSQL and H2 database platforms.
 *
 * @author Lars Helge Overland
 */
public class SqlUtils {
  public static final String QUOTE = "\"";

  public static final String SINGLE_QUOTE = "'";

  public static final String SEPARATOR = ".";

  public static final String OPTION_SEP = ".";

  private static final String BACKSLASH = "\\";

  /**
   * Double quotes the given relation (typically a column). Quotes part of the given relation are
   * escaped (replaced by two double quotes).
   *
   * @param relation the relation (typically a column).
   * @return the quoted relation.
   */
  public static String quote(String relation) {
    String rel = relation.replace(QUOTE, (QUOTE + QUOTE));

    return QUOTE + rel + QUOTE;
  }

  /**
   * Double quotes and qualifies the given relation (typically a column). Quotes part of the given
   * relation are escaped (replaced by double quotes). The column name is qualified by the given
   * alias.
   *
   * @param relation the relation (typically a column).
   * @param alias the alias.
   * @return the quoted relation.
   */
  public static String quote(String alias, String relation) {
    Objects.requireNonNull(alias);

    return alias + SEPARATOR + quote(relation);
  }

  /**
   * Single quotes the given relation (typically a value). Escapes characters including single quote
   * and backslash.
   *
   * @param value the value.
   * @return the single quoted relation.
   */
  public static String singleQuote(String value) {
    return SINGLE_QUOTE + escape(value) + SINGLE_QUOTE;
  }

  /**
   * Escapes the given value. Replaces single quotes with two single quotes. Replaces backslash with
   * two backslashes.
   *
   * @param value the value to escape.
   * @return the escaped value.
   */
  public static String escape(String value) {
    return value
        .replace(SINGLE_QUOTE, (SINGLE_QUOTE + SINGLE_QUOTE))
        .replace(BACKSLASH, (BACKSLASH + BACKSLASH));
  }

  /**
   * Appends an underscore and five character random suffix to the given relation.
   *
   * @param relation the relation.
   * @return the appended relation.
   */
  public static String appendRandom(String relation) {
    return String.format("%s_%s", relation, CodeGenerator.generateCode(5));
  }

  /**
   * Returns a string set for the given result set and column. Assumes that the SQL type is an array
   * of text values.
   *
   * @param rs the result set.
   * @param columnLabel the column label.
   * @return a string set.
   */
  public static Set<String> getArrayAsSet(ResultSet rs, String columnLabel) throws SQLException {
    Array sqlArray = rs.getArray(columnLabel);
    String[] array = (String[]) sqlArray.getArray();
    return Sets.newHashSet(array);
  }

  /**
   * Cast the given value to numeric (cast(X as numeric).
   *
   * @param value the value.
   * @return a string with the numeric cast statement.
   */
  public static String castToNumber(String value) {
    return "cast (" + value + " as numeric)";
  }

  /**
   * Lowers the given value.
   *
   * @param value the value.
   * @return a string with the lower function.
   */
  public static String lower(String value) {
    return "lower(" + value + ")";
  }
}
