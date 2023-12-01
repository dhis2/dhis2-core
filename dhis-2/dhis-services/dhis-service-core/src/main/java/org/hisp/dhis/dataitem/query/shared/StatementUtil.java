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
package org.hisp.dhis.dataitem.query.shared;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.replaceEach;

import lombok.NoArgsConstructor;

/**
 * This class keeps basic SQL statements/keywords/constants so they can be reused by the queries.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class StatementUtil {
  public static final String SPACED_SELECT = " select ";

  public static final String SPACED_WHERE = " where ";

  public static final String SPACED_OR = " or ";

  public static final String SPACED_AND = " and ";

  public static final String EQUAL = " = :";

  public static final String ILIKE = " ilike :";

  public static final String SPACED_LEFT_PARENTHESIS = " ( ";

  public static final String SPACED_RIGHT_PARENTHESIS = " ) ";

  public static final String SPACED_FROM = " from ";

  /**
   * This method is specific for strings used in "ilike" filters where some non accepted characters
   * will fail at querying time. It will only replace common characters by the form accepted in SQL
   * ilike queries.
   *
   * @param value the value where characters will ve replaced.
   * @return the input value with the characters replaced.
   */
  public static String addIlikeReplacingCharacters(String value) {
    return replaceEach(value, new String[] {"%", ","}, new String[] {"\\%", "\\,"});
  }

  /**
   * Creates a "in" SQL statement isolated by parenthesis, ie.: "( column in (:param) )"
   *
   * @param column
   * @param namedParam
   * @return the SQL string
   */
  public static String inFiltering(String column, String namedParam) {
    return SPACED_LEFT_PARENTHESIS
        + column
        + " in (:"
        + namedParam
        + ")"
        + SPACED_RIGHT_PARENTHESIS;
  }

  /**
   * Creates a "ilike" SQL statement isolated by parenthesis, ie.: "( column ilike :param )"
   *
   * @param column
   * @param namedParam
   * @return the SQL string
   */
  public static String ilikeFiltering(String column, String namedParam) {
    return SPACED_LEFT_PARENTHESIS + column + ILIKE + namedParam + SPACED_RIGHT_PARENTHESIS;
  }

  /**
   * Creates a "ilike" SQL statement isolated by parenthesis. It consider two different columns and
   * uses "or" as junction, ie.: "( columnOne ilike :param or columnTwo ilike :param)"
   *
   * @param columnOne
   * @param columnTwo
   * @param namedParam
   * @return the SQL string
   */
  public static String ilikeOrFiltering(String columnOne, String columnTwo, String namedParam) {
    return SPACED_LEFT_PARENTHESIS
        + columnOne
        + ILIKE
        + namedParam
        + " or "
        + columnTwo
        + ILIKE
        + namedParam
        + SPACED_RIGHT_PARENTHESIS;
  }

  /**
   * Creates a "equal" SQL statement isolated by parenthesis, ie.: "( column = :param )"
   *
   * @param column
   * @param namedParam
   * @return the SQL string
   */
  public static String equalsFiltering(String column, String namedParam) {
    return SPACED_LEFT_PARENTHESIS + column + EQUAL + namedParam + SPACED_RIGHT_PARENTHESIS;
  }
}
