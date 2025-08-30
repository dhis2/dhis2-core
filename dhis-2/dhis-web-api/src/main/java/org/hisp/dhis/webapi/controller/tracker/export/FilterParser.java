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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.common.DimensionConstants.DIMENSION_NAME_SEP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;

/**
 * Parser for the <code>filter</code> (<code>filterAttribute</code>) request parameter.
 *
 * <p><code>/changeLogs?filter</code> validation is implemented in {@link
 * org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator} and its parsing in its mapper.
 * The requirements for these endpoints are slightly different. We should use this parser for
 * changeLogs as soon as we need to support escaped values like dates which contain the segment
 * separator ':'.
 */
public class FilterParser {

  private FilterParser() {
    throw new IllegalStateException("Utility class");
  }

  /** Slash, comma and colon have a width of 1 in the input string. */
  public static final int SEPARATOR_CHAR_COUNT = 1;

  private static final char ESCAPE = '/';
  private static final String ESCAPE_STRING = Character.toString(ESCAPE);
  private static final String ESCAPED_ESCAPE = ESCAPE + ESCAPE_STRING;
  private static final char COMMA = ',';
  private static final String COMMA_STRING = Character.toString(COMMA);
  private static final String ESCAPED_COMMA = ESCAPE + COMMA_STRING;
  private static final char COLON = ':';
  private static final String COLON_STRING = Character.toString(COLON);
  private static final String ESCAPED_COLON = ESCAPE + COLON_STRING;

  /**
   * Parse given {@code input} string representing one or more filters for objects referenced by a
   * UID like a tracked entity attribute or data element.
   *
   * <p>The expected input format is <br>
   * <code>{uid}[:{operator}[:{value}]][,{uid}[:{operator}[:{value}]]]</code> <br>
   * The value is mandatory and only allowed for binary operators. Multiple operator or
   * operator:value pairs are allowed. A {@link QueryFilter} for each operator[:value] pair is added
   * to the corresponding UID in the input order.
   *
   * <p>This method assumes that the input
   *
   * <ul>
   *   <li>has been percent-decoded already
   * </ul>
   *
   * @return accumulated {@link QueryFilter}s per UID
   * @throws BadRequestException if the input is invalid
   */
  public static Map<UID, List<QueryFilter>> parseFilters(
      @Nonnull String parameterName, @CheckForNull String input) throws BadRequestException {
    try {
      return parseFilters(input);
    } catch (Exception e) {
      throw new BadRequestException(parameterName + "=" + input + " is invalid. " + e.getMessage());
    }
  }

  public static Map<UID, List<QueryFilter>> parseFilters(String input) throws BadRequestException {
    Map<UID, List<QueryFilter>> result = new HashMap<>();
    if (StringUtils.isBlank(input)) {
      return result;
    }

    int start = 0;
    int end = 0;
    UID uid = null;
    String operator = null;
    String valueOrOperator = null;
    int length = input.length();
    while (end < length) {
      int curChar = input.codePointAt(end);
      int charCount =
          Character.charCount(
              curChar); // so we iterate characters spanning more than char(16-bytes)

      // skip escaped slash, colon and comma which allow users to pass them as part of values
      if (curChar == ESCAPE && end + SEPARATOR_CHAR_COUNT < length) {
        int nextChar = input.codePointAt(end + SEPARATOR_CHAR_COUNT);
        if (nextChar == ESCAPE || nextChar == COLON || nextChar == COMMA) {
          end += charCount + Character.charCount(nextChar);
          continue;
        }
      }

      // get next segment
      if (curChar == COLON || curChar == COMMA) {
        if (uid == null
            && (curChar == COLON || end - start > 0)) { // empty commas like "," ",," are ignored
          uid = uid(input.substring(start, end));
        } else if (operator == null) {
          operator = input.substring(start, end);
        } else {
          valueOrOperator = input.substring(start, end);
        }
        start = end + charCount;
      }

      // state transitions
      if (curChar == COMMA) { // transition back to initial state
        addFilter(result, uid, operator, valueOrOperator);

        uid = null;
        operator = null;
        valueOrOperator = null;
      } else if (curChar == COLON && valueOrOperator != null) {
        // we only keep track of two segments after the uid so we need to consume at least the first
        // if it is a unary operator or both if its a binary operator
        QueryOperator parsedOperator = getQueryOperator(uid, operator);
        if (parsedOperator.isUnary()) {
          // ensure the next segment (valueOrOperator) is a valid operator as the unary operator
          // cannot have a value
          validateUnaryOperatorHasNoValue(uid, operator, valueOrOperator);
          addFilter(result, uid, operator, null);

          operator = valueOrOperator;
        } else {
          addFilter(result, uid, operator, valueOrOperator);

          operator = null;
        }

        // the uid is not reset as it might get another operator or operator:value pair
        valueOrOperator = null;
      }

      end += charCount;
    }

    if (start < end) { // consume remaining input
      if (uid == null) {
        uid = uid(input.substring(start, end));
      } else if (operator == null) {
        operator = input.substring(start, end);
      } else {
        valueOrOperator = input.substring(start, end);
      }
    }
    addFilter(result, uid, operator, valueOrOperator);

    return result;
  }

  private static UID uid(String uid) throws BadRequestException {
    try {
      return UID.of(uid);
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException(exception.getMessage());
    }
  }

  @Nonnull
  private static QueryOperator getQueryOperator(@Nonnull UID uid, String operator)
      throws BadRequestException {
    try {
      QueryOperator parsedOperator = QueryOperator.fromString(operator);
      if (parsedOperator == null) {
        throw new BadRequestException(
            "UID '" + uid + "' is missing an operator (or has too many ':').");
      }
      return parsedOperator;
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException("'" + operator + "' is not a valid operator.");
    }
  }

  private static Optional<QueryOperator> validateUnaryOperatorHasNoValue(
      @Nonnull UID uid, @Nonnull String operator, @CheckForNull String valueOrOperator)
      throws BadRequestException {
    QueryOperator parsedOperator = getQueryOperator(uid, operator);
    Optional<QueryOperator> nextOperator = findQueryOperator(valueOrOperator);
    if (parsedOperator.isUnary()
        && StringUtils.isNotEmpty(valueOrOperator)
        && nextOperator.isEmpty()) {
      throw new BadRequestException("Unary operator '" + operator + "' cannot have a value.");
    }
    return nextOperator;
  }

  private static Optional<QueryOperator> findQueryOperator(String operator) {
    try {
      return Optional.ofNullable(QueryOperator.fromString(operator));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  private static void addFilter(
      @Nonnull Map<UID, List<QueryFilter>> result,
      @CheckForNull UID uid,
      @CheckForNull String operator,
      @CheckForNull String valueOrOperator)
      throws BadRequestException {
    if (uid == null) {
      return;
    }

    result.putIfAbsent(uid, new ArrayList<>());

    if (operator == null) {
      return;
    }

    QueryOperator parsedOperator = getQueryOperator(uid, operator);
    Optional<QueryOperator> nextOperator =
        validateUnaryOperatorHasNoValue(uid, operator, valueOrOperator);
    if (parsedOperator.isUnary()) {
      result.get(uid).add(new QueryFilter(parsedOperator));
      if (nextOperator.isPresent() && nextOperator.get().isUnary()) {
        result.get(uid).add(new QueryFilter(nextOperator.get()));
      } else if (nextOperator.isPresent() && nextOperator.get().isBinary()) {
        throw new BadRequestException(
            "Binary operator '" + valueOrOperator + "' must have a value.");
      }
    } else {
      if (StringUtils.isEmpty(valueOrOperator)) {
        throw new BadRequestException("Binary operator '" + operator + "' must have a value.");
      }

      result.get(uid).add(new QueryFilter(parsedOperator, removeEscapeCharacters(valueOrOperator)));
    }

    if (result.get(uid).size() > 3) {
      throw new BadRequestException("A maximum of three operators can be used in a filter.");
    }
  }

  /** Remove escape character '/' from escaped comma or colon */
  private static String removeEscapeCharacters(String value) {
    return value
        .replace(ESCAPED_ESCAPE, ESCAPE_STRING)
        .replace(ESCAPED_COMMA, COMMA_STRING)
        .replace(ESCAPED_COLON, DIMENSION_NAME_SEP);
  }
}
