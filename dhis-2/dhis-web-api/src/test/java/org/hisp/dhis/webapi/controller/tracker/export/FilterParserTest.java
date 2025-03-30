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

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.export.FilterParser.parseFilters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests {@link FilterParser}. */
class FilterParserTest {
  @Test
  void shouldParseBinaryOperatorWithUnicodeCodePointsOutsideAsciiRange()
      throws BadRequestException {
    String value = "𞸀𞸁";
    assertTrue(
        value.codePoints().anyMatch(cp -> cp > 0xFFFF),
        "String contains characters exceeding 16-bit range");

    Map<UID, List<QueryFilter>> filters = parseFilters("TvjwTPToKHO:like:" + value);

    assertEquals(
        Map.of(UID.of("TvjwTPToKHO"), List.of(new QueryFilter(QueryOperator.LIKE, value))),
        filters);
  }

  @Test
  void shouldParseFiltersWithMultipleDistinctIdentifiersAndOperators() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters("TvjwTPToKHO:lt:20:gt:10,cy2oRh2sNr6:like:foo");

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(
                new QueryFilter(QueryOperator.LT, "20"), new QueryFilter(QueryOperator.GT, "10")),
            UID.of("cy2oRh2sNr6"),
            List.of(new QueryFilter(QueryOperator.LIKE, "foo"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithMultipleRepeatedIdentifiers() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters("TvjwTPToKHO:lt:20,cy2oRh2sNr6:like:foo,TvjwTPToKHO:gt:10");

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(
                new QueryFilter(QueryOperator.LT, "20"), new QueryFilter(QueryOperator.GT, "10")),
            UID.of("cy2oRh2sNr6"),
            List.of(new QueryFilter(QueryOperator.LIKE, "foo"))),
        filters);
  }

  @ValueSource(
      strings = {
        "TvjwTPToKHO",
        "TvjwTPToKHO:",
        "TvjwTPToKHO,",
      })
  @ParameterizedTest
  void shouldParseFiltersWithIdentifierOnly(String input) throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(input);

    assertEquals(Map.of(UID.of("TvjwTPToKHO"), List.of()), filters);
  }

  @Test
  void shouldParseFiltersWithIdentifiersOnly() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters("TvjwTPToKHO,cy2oRh2sNr6");

    assertEquals(
        Map.of(UID.of("TvjwTPToKHO"), List.of(), UID.of("cy2oRh2sNr6"), List.of()), filters);
  }

  @Test
  void shouldParseFiltersWithBlankInput() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(" ");

    assertTrue(filters.isEmpty());
  }

  @ValueSource(
      strings = {
        ",", ",,",
      })
  @ParameterizedTest
  void shouldParseFiltersWithJustCommas(String input) throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(input);

    assertTrue(filters.isEmpty());
  }

  @Test
  void shouldParseFiltersWithValueContainingEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters("cy2oRh2sNr6:like:project/:x/:eq/:2");

    assertEquals(
        Map.of(
            UID.of("cy2oRh2sNr6"), List.of(new QueryFilter(QueryOperator.LIKE, "project:x:eq:2"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithValueContainingEscapedComma() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters("cy2oRh2sNr6:like:project/,x/:eq/:2");

    assertEquals(
        Map.of(
            UID.of("cy2oRh2sNr6"), List.of(new QueryFilter(QueryOperator.LIKE, "project,x:eq:2"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithValueContainingEscapedSlash() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters("cy2oRh2sNr6:like:project//x/:eq/:2");

    assertEquals(
        Map.of(
            UID.of("cy2oRh2sNr6"), List.of(new QueryFilter(QueryOperator.LIKE, "project/x:eq:2"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithDateRangeContainingEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters(
            "TvjwTPToKHO:ge:2020-01-01T00/:00/:00.001 +05/:30:le:2021-01-01T00/:00/:00.001 +05/:30");

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(
                new QueryFilter(QueryOperator.GE, "2020-01-01T00:00:00.001 +05:30"),
                new QueryFilter(QueryOperator.LE, "2021-01-01T00:00:00.001 +05:30"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithTextRangeContainingEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters("TvjwTPToKHO:sw:project/:x:ew:project/:le/:");

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(
                new QueryFilter(QueryOperator.SW, "project:x"),
                new QueryFilter(QueryOperator.EW, "project:le:"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithEscapedSlashAndComma() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters(
"""
TvjwTPToKHO:eq:project///,/,//,\
cy2oRh2sNr6:eq:project//,\
cy2oRh2sNr7:eq:project//""");

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(new QueryFilter(QueryOperator.EQ, "project/,,/")),
            UID.of("cy2oRh2sNr6"),
            List.of(new QueryFilter(QueryOperator.EQ, "project/")),
            UID.of("cy2oRh2sNr7"),
            List.of(new QueryFilter(QueryOperator.EQ, "project/"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithMultipleOperatorsAndEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters("TvjwTPToKHO:like:value1/::like:value2");

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(
                new QueryFilter(QueryOperator.LIKE, "value1:"),
                new QueryFilter(QueryOperator.LIKE, "value2"))),
        filters);
  }

  @ValueSource(strings = {"TvjwTPToKHO:!null", "TvjwTPToKHO:!null:", "TvjwTPToKHO:!null,"})
  @ParameterizedTest
  void shouldParseFiltersWithSingleUnaryOperator(String input) throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(input);

    assertEquals(
        Map.of(UID.of("TvjwTPToKHO"), List.of(new QueryFilter(QueryOperator.NNULL))), filters);
  }

  @ValueSource(
      strings = {
        "TvjwTPToKHO:!null:null",
        "TvjwTPToKHO:!null:null:",
        "TvjwTPToKHO:!null:null,",
      })
  @ParameterizedTest
  void shouldParseFiltersWithSingleIdentifierAndMultipleUnaryOperators(String input)
      throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(input);

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NULL))),
        filters);
  }

  @ValueSource(strings = {"TvjwTPToKHO:null:gt:10", "TvjwTPToKHO:null,TvjwTPToKHO:gt:10"})
  @ParameterizedTest
  void shouldParseFiltersWithCombinedUnaryAndBinaryOperators(String input)
      throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(input);

    assertEquals(
        Map.of(
            UID.of("TvjwTPToKHO"),
            List.of(new QueryFilter(QueryOperator.NULL), new QueryFilter(QueryOperator.GT, "10"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithBinaryOperatorValueBeingNull() throws BadRequestException {
    // null is not a reserved keyword like in Java so it will be used as a value if it follows a
    // binary operator
    Map<UID, List<QueryFilter>> filters = parseFilters("TvjwTPToKHO:eq:null");

    assertEquals(
        Map.of(UID.of("TvjwTPToKHO"), List.of(new QueryFilter(QueryOperator.EQ, "null"))), filters);
  }

  @Test
  void shouldFailWhenOperatorDoesNotExist() {
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> parseFilters("TvjwTPToKHO:lke:value"));

    assertContains("'lke' is not a valid operator", exception.getMessage());
  }

  @ValueSource(
      strings = {
        "TvjwTPToKHO::null,cy2oRh2sNr6:!null",
        "TvjwTPToKHO::gt:10",
        "TvjwTPToKHO::gt:10,",
        "TvjwTPToKHO::gt:10:",
      })
  @ParameterizedTest
  void shouldFailWhenOperatorIsEmpty(String input) {
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> parseFilters(input));

    assertContains("UID 'TvjwTPToKHO' is missing an operator", exception.getMessage());
  }

  @Test
  void shouldFailReportingTheParameterNameAndInput() {
    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> parseFilters("filterAttributes", "nouid:eq:2"));

    assertContains("filterAttributes=nouid:eq:2 is invalid", exception.getMessage());
  }

  @ValueSource(strings = {"nouid:eq:2", ":", "::", ",:", " ,", ", ,"})
  @ParameterizedTest
  void shouldFailWhenUIDIsInvalid(String input) {
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> parseFilters(input));

    assertContains("UID must be an alphanumeric string", exception.getMessage());
  }

  @ValueSource(
      strings = {
        "TvjwTPToKHO:lt",
        "TvjwTPToKHO:lt:",
        "TvjwTPToKHO:lt::",
        "TvjwTPToKHO:lt,",
        "TvjwTPToKHO:lt::gt:10",
        "TvjwTPToKHO:lt::gt:10:",
        "TvjwTPToKHO:gt:10:lt",
        "TvjwTPToKHO:null:lt",
        "TvjwTPToKHO:null:lt,",
        "TvjwTPToKHO:null:lt:",
      })
  @ParameterizedTest
  void shouldFailWhenBinaryOperatorIsMissingAValue(String input) {
    Exception exception = assertThrows(BadRequestException.class, () -> parseFilters(input));

    assertContains("Binary operator 'lt' must have a value", exception.getMessage());
  }

  @ValueSource(
      strings = {
        "TvjwTPToKHO:null:!null:value",
        "TvjwTPToKHO:!null:value:eq:2",
        "TvjwTPToKHO:!null:value",
        "TvjwTPToKHO:null:!null:value:",
        "TvjwTPToKHO:!null:value:eq:2:",
        "TvjwTPToKHO:gt:10:!null:value",
        "TvjwTPToKHO:!null:value:",
        "TvjwTPToKHO:null:!null:value,",
        "TvjwTPToKHO:!null:value:eq:2,",
        "TvjwTPToKHO:!null:value,"
      })
  @ParameterizedTest
  void shouldFailWhenUnaryOperatorHasAValue(String input) {
    Exception exception = assertThrows(BadRequestException.class, () -> parseFilters(input));

    assertContains("Unary operator '!null' cannot have a value", exception.getMessage());
  }

  @ValueSource(
      strings = {
        "TvjwTPToKHO:gt:10:null:!null:null",
        "TvjwTPToKHO:gt:10:null:!null:null:",
        "TvjwTPToKHO:gt:10:null,TvjwTPToKHO:!null:null",
      })
  @ParameterizedTest
  void shouldFailParsingFiltersWithMoreThanThreeOperatorsForASingleIdentifier(String input) {
    Exception exception = assertThrows(BadRequestException.class, () -> parseFilters(input));

    assertStartsWith(
        "A maximum of three operators can be used in a filter", exception.getMessage());
  }
}
