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

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;

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

/** Parser for the filter request parameter. */
public class FilterParser {
  private FilterParser() {
    throw new IllegalStateException("Utility class");
  }

  private static final String INVALID_FILTER = "Query item or filter is invalid: ";

  private static final char COMMA_SEPARATOR = ',';

  private static final char ESCAPE = '/';

  private static final String ESCAPE_STRING = Character.toString(ESCAPE);
  private static final String COMMA_STRING = Character.toString(COMMA_SEPARATOR);

  private static final String ESCAPED_ESCAPE = ESCAPE + ESCAPE_STRING;
  private static final String ESCAPED_COMMA = ESCAPE + COMMA_STRING;
  private static final String ESCAPED_COLON = ESCAPE + DIMENSION_NAME_SEP;

  /**
   * Parse given {@code input} string representing a filter for an object referenced by a UID like a
   * tracked entity attribute.
   *
   * <p>Accumulate {@link QueryFilter}s per UID by parsing given input string of format
   * {uid}[:{operator}:{value}]. Only the UID is mandatory. Multiple operator:value pairs are
   * allowed. A {@link QueryFilter} for each operator:value pair is added to the corresponding UID.
   *
   * @throws BadRequestException filter is neither multiple nor single operator:value format
   */
  public static Map<UID, List<QueryFilter>> parseFilters(String input) throws BadRequestException {
    Map<UID, List<QueryFilter>> result = new HashMap<>();
    if (StringUtils.isBlank(input)) {
      return result;
    }

    int start = 0, end = 0;
    UID currentUid = null;
    QueryOperator currentOperator = null;
    while (end < input.length()) {
      char curChar = input.charAt(end);
      // skip escaped slash and segment/filter separators
      if (curChar == '/'
          && end + 1 < input.length()
          && (input.charAt(end + 1) == '/'
              || input.charAt(end + 1) == ':'
              || input.charAt(end + 1) == ',')) {
        end = end + 2;
        continue;
      }

      if (curChar == ':') { // new segment causes state transition
        if (currentUid == null) {
          currentUid = UID.of(input.substring(start, end));
        } else if (currentOperator == null) {
          currentOperator = getQueryOperator(input, input.substring(start, end));
        } else if (currentOperator.isUnary()) { // consume unary operator
          addFilter(input, result, currentUid, currentOperator, null);

          // the current segment might be the next operator or be an invalid attempt at providing a
          // value to a unary operator
          // TODO what test is currently targeting this case? could we provide a better error
          // message?
          currentOperator = getQueryOperator(input, input.substring(start, end));
        } else { // consume binary operator:value
          addFilter(input, result, currentUid, currentOperator, input.substring(start, end));

          // currentUid might get another operator or operator:value pair
          currentOperator = null;
        }

        start = end + 1;
      } else if (curChar == ',') { // new filter causes state transition to initial state
        String valueOrOperator = null;
        if (currentUid == null) {
          currentUid = UID.of(input.substring(start, end));
        } else if (currentOperator == null) {
          currentOperator = getQueryOperator(input, input.substring(start, end));
        } else {
          valueOrOperator = input.substring(start, end);
        }
        addFilter(input, result, currentUid, currentOperator, valueOrOperator);

        currentUid = null;
        currentOperator = null;

        start = end + 1;
      }
      end++;
    }

    if (start < end) { // consume remaining input
      String valueOrOperator = null;
      if (currentUid == null) {
        currentUid = UID.of(input.substring(start, end));
      } else if (currentOperator == null) {
        currentOperator = getQueryOperator(input, input.substring(start, end));
      } else {
        valueOrOperator = input.substring(start, end);
      }
      addFilter(input, result, currentUid, currentOperator, valueOrOperator);
      // TODO cases for trailing :
      // is that correct, are there more cases? could we have a trailing value here? or not as it
      // would then need to be consumed in the above start < end case
    } else if (currentUid != null) {
      addFilter(input, result, currentUid, currentOperator, null);
    }

    return result;
  }

  private static void addFilter(
      @Nonnull String input,
      @Nonnull Map<UID, List<QueryFilter>> result,
      @Nonnull UID currentUid,
      @CheckForNull QueryOperator currentOperator,
      @CheckForNull String valueOrOperator)
      throws BadRequestException {
    result.putIfAbsent(currentUid, new ArrayList<>());

    if (currentOperator == null) {
      return;
    }

    if (currentOperator.isBinary() && StringUtils.isEmpty(valueOrOperator)) {
      throw new BadRequestException(
          "filter "
              + input
              + " is invalid. Binary operator "
              + currentOperator
              + " must have a value.");
    }

    Optional<QueryOperator> nextOperator = findQueryOperator(valueOrOperator);
    if (currentOperator.isUnary()
        && StringUtils.isNotEmpty(valueOrOperator)
        && nextOperator.isEmpty()) {
      throw new BadRequestException(
          "filter "
              + input
              + " is invalid. Unary operator "
              + currentOperator
              + " cannot have a value.");
    }

    if (currentOperator.isUnary()) {
      result.get(currentUid).add(new QueryFilter(currentOperator));
      if (nextOperator.isPresent() && nextOperator.get().isUnary()) {
        result.get(currentUid).add(new QueryFilter(nextOperator.get()));
      }
      // TODO(ivo) I need to validate the nextOperator is not a binary one as it would not have a
      // value
    } else {
      result
          .get(currentUid)
          .add(new QueryFilter(currentOperator, removeEscapeCharacters(valueOrOperator)));
    }

    if (result.get(currentUid).size() > 2) {
      throw new BadRequestException(
          String.format("A maximum of two operators can be used in a filter: %s", input));
    }
  }

  private static QueryOperator getQueryOperator(String input, String operator)
      throws BadRequestException {
    try {
      return QueryOperator.fromString(operator);
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException(
          "filter " + input + " is invalid. '" + operator + "' is not a valid operator.");
    }
  }

  private static Optional<QueryOperator> findQueryOperator(String operator) {
    try {
      return Optional.ofNullable(QueryOperator.fromString(operator));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
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
