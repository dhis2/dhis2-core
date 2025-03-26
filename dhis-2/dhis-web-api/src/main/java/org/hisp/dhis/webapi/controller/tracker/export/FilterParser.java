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

  private enum state {
    UID,
    OPERATOR,
    VALUE
  }

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
    state currentState = state.UID;
    UID currentUid = null;
    QueryOperator currentOperator = null;
    while (end < input.length()) {
      // skip escaped slash and segment/filter separators
      if (input.charAt(end) == '/'
          && end + 1 < input.length()
          && (input.charAt(end + 1) == '/'
              || input.charAt(end + 1) == ':'
              || input.charAt(end + 1) == ',')) {
        end = end + 2;
        continue;
      }

      if (input.charAt(end) == ':') {
        if (currentState == state.UID) {
          currentUid = UID.of(input.substring(start, end));

          // an operator or operator:value pair might follow but only UIDs are legal as well
          currentState = state.OPERATOR;
        } else if (currentState == state.OPERATOR) {
          // TODO we seem to have some logic mapping our "!null" or "null" to the QueryOperators
          currentOperator = QueryOperator.fromString(input.substring(start, end));
          // TODO only binary operators should move to the value state; use the unary set we have to
          // determine if we now expect a value or not
          currentState = state.VALUE;
        } else {
          consumeFilter(result, currentUid, currentOperator, input, start, end);

          // another operator or operator:value pair for the currentUid might follow
          currentOperator = null;
          currentState = state.OPERATOR;
        }

        // TODO be careful not to index out of bounds
        start = end + 1;
      } else if (input.charAt(end) == ',') {
        // TODO error handling depending on the state we are in; what could happen? make sure we err
        // if we have not consumed some input
        if (currentState == state.VALUE) {
          consumeFilter(result, currentUid, currentOperator, input, start, end);
        }

        // comma transitions back to the initial state
        currentState = state.UID;
        currentUid = null;
        currentOperator = null;

        // TODO be careful not to index out of bounds
        start = end + 1;
      }
      end++;
    }

    // TODO check currentState and non-consumed input like in the only UID case, there might be
    // other cases
    if (start < end) {
      if (currentState == state.UID) {
        currentUid = UID.of(input.substring(start, end));
        result.putIfAbsent(currentUid, new ArrayList<>());
        // TODO we might also still need to consume an operator and a value right?
      } else {
        consumeFilter(result, currentUid, currentOperator, input, start, end);
      }
      // TODO cases for trailing :
      // is that correct
    } else if (currentUid != null) {
      result.putIfAbsent(currentUid, new ArrayList<>());
    }

    return result;
  }

  private static void consumeFilter(
      Map<UID, List<QueryFilter>> result,
      UID currentUid,
      QueryOperator currentOperator,
      String input,
      int start,
      int end) {
    String value = removeEscapeCharacters(input.substring(start, end));
    result.putIfAbsent(currentUid, new ArrayList<>());
    result.get(currentUid).add(new QueryFilter(currentOperator, value));
  }

  /** Remove escape character '/' from escaped comma or colon */
  private static String removeEscapeCharacters(String value) {
    return value
        .replace(ESCAPED_ESCAPE, ESCAPE_STRING)
        .replace(ESCAPED_COMMA, COMMA_STRING)
        .replace(ESCAPED_COLON, DIMENSION_NAME_SEP);
  }
}
