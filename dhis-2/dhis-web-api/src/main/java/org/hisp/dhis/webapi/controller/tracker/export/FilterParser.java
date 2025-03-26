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
    UID uid = null;
    QueryOperator operator = null;
    String valueOrOperator = null;
    while (end < input.length()) {
      char curChar = input.charAt(end);
      // skip escaped slash, colon and comma which allow users to pass them as part of values
      if (curChar == '/'
          && end + 1 < input.length()
          && (input.charAt(end + 1) == '/'
              || input.charAt(end + 1) == ':'
              || input.charAt(end + 1) == ',')) {
        end = end + 2;
        continue;
      }

      // get next segment
      if (curChar == ':' || curChar == ',') {
        if (uid == null) {
          uid = UID.of(input.substring(start, end));
        } else if (operator == null) {
          operator = getQueryOperator(input, input.substring(start, end));
        } else {
          valueOrOperator = input.substring(start, end);
        }
        start = end + 1;
      }

      // state transitions
      if (curChar == ',') { // transition back to initial state
        addFilter(input, result, uid, operator, valueOrOperator);

        uid = null;
        operator = null;
        valueOrOperator = null;
      } else if (curChar == ':' && valueOrOperator != null) {
        // uid is not reset as it might get another operator or operator:value pair
        Optional<QueryOperator> nextOperator =
            validateUnaryOperator(input, operator, valueOrOperator);
        if (operator.isUnary() && nextOperator.isPresent()) {
          addFilter(input, result, uid, operator, null);

          operator = nextOperator.get();
        } else {
          addFilter(input, result, uid, operator, valueOrOperator);

          operator = null;
        }
        valueOrOperator = null;
      }
      end++;
    }

    if (start < end) { // consume remaining input
      if (uid == null) {
        uid = UID.of(input.substring(start, end));
      } else if (operator == null) {
        operator = getQueryOperator(input, input.substring(start, end));
      } else {
        valueOrOperator = input.substring(start, end);
      }
      // can I join this with the case below somehow? or make addFilter null safe and just call it
      // regardless
      addFilter(input, result, uid, operator, valueOrOperator);
      // TODO cases for trailing :
      // is that correct, are there more cases? could we have a trailing value here? or not as it
      // would then need to be consumed in the above start < end case
      // can there ever be a valueOrOperator trailing here? add test
    } else if (uid != null) {
      addFilter(input, result, uid, operator, valueOrOperator);
    }

    return result;
  }

  private static void addFilter(
      @Nonnull String input,
      @Nonnull Map<UID, List<QueryFilter>> result,
      @Nonnull UID uid,
      @CheckForNull QueryOperator operator,
      @CheckForNull String valueOrOperator)
      throws BadRequestException {
    result.putIfAbsent(uid, new ArrayList<>());

    if (operator == null) {
      return;
    }

    if (operator.isBinary() && StringUtils.isEmpty(valueOrOperator)) {
      throw new BadRequestException(
          "filter " + input + " is invalid. Binary operator " + operator + " must have a value.");
    }

    Optional<QueryOperator> nextOperator = validateUnaryOperator(input, operator, valueOrOperator);
    if (operator.isUnary()) {
      result.get(uid).add(new QueryFilter(operator));
      if (nextOperator.isPresent() && nextOperator.get().isUnary()) {
        result.get(uid).add(new QueryFilter(nextOperator.get()));
      } else if (nextOperator.isPresent() && nextOperator.get().isBinary()) {
        throw new BadRequestException(
            "filter "
                + input
                + " is invalid. Binary operator "
                + nextOperator.get()
                + " must have a value.");
      }
    } else {
      result.get(uid).add(new QueryFilter(operator, removeEscapeCharacters(valueOrOperator)));
    }

    if (result.get(uid).size() > 2) {
      throw new BadRequestException(
          String.format("A maximum of two operators can be used in a filter: %s", input));
    }
  }

  private static Optional<QueryOperator> validateUnaryOperator(
      @Nonnull String input, @Nonnull QueryOperator operator, @CheckForNull String valueOrOperator)
      throws BadRequestException {
    Optional<QueryOperator> nextOperator = findQueryOperator(valueOrOperator);
    if (operator.isUnary() && StringUtils.isNotEmpty(valueOrOperator) && nextOperator.isEmpty()) {
      throw new BadRequestException(
          "filter " + input + " is invalid. Unary operator " + operator + " cannot have a value.");
    }
    return nextOperator;
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
