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

/** Tests {@link FilterParser}. */
class FilterParserTest {

  private static final UID UID_1 = UID.of("TvjwTPToKHO");

  private static final UID UID_2 = UID.of("cy2oRh2sNr6");

  private static final UID UID_3 = UID.of("cy2oRh2sNr7");

  @Test
  void shouldParseFiltersWithMultipleDistinctIdentifiersAndOperators() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters(UID_1 + ":lt:20:gt:10," + UID_2 + ":like:foo");

    assertEquals(
        Map.of(
            UID_1,
            List.of(
                new QueryFilter(QueryOperator.LT, "20"), new QueryFilter(QueryOperator.GT, "10")),
            UID_2,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithMultipleRepeatedIdentifiers() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters(UID_1 + ":lt:20," + UID_2 + ":like:foo," + UID_1 + ":gt:10");

    assertEquals(
        Map.of(
            UID_1,
            List.of(
                new QueryFilter(QueryOperator.LT, "20"), new QueryFilter(QueryOperator.GT, "10")),
            UID_2,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithIdentifierOnly() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1.getValue());

    assertEquals(Map.of(UID_1, List.of()), filters);
  }

  @Test
  void shouldParseFiltersWithIdentifierAndTrailingColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1.getValue() + ":");

    assertEquals(Map.of(UID_1, List.of()), filters);
  }

  @Test
  void shouldParseFiltersWithBlankInput() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(" ");

    assertTrue(filters.isEmpty());
  }

  @Test
  void shouldFailWhenOperatorIsMissingAValue() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":lt"));
    assertEquals(
        "Operator in filter must be be used with a value: " + UID_1 + ":lt",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenOperatorHasATrailingColonAndIsMissingAValue() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":lt:"));
    assertEquals(
        "Operator in filter must be be used with a value: " + UID_1 + ":lt:",
        exception.getMessage());
  }

  @Test
  void shouldParseFiltersWithValueContainingEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_2 + ":like:project/:x/:eq/:2");

    assertEquals(
        Map.of(UID_2, List.of(new QueryFilter(QueryOperator.LIKE, "project:x:eq:2"))), filters);
  }

  @Test
  void shouldParseFiltersWithValueContainingEscapedComma() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_2 + ":like:project/,x/:eq/:2");

    assertEquals(
        Map.of(UID_2, List.of(new QueryFilter(QueryOperator.LIKE, "project,x:eq:2"))), filters);
  }

  @Test
  void shouldParseFiltersWithValueContainingEscapedSlash() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_2 + ":like:project//x/:eq/:2");

    assertEquals(
        Map.of(UID_2, List.of(new QueryFilter(QueryOperator.LIKE, "project/x:eq:2"))), filters);
  }

  @Test
  void shouldFailWhenOperatorDoesNotExist() {
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":lke:value"));
    assertEquals("'lke' is not a valid operator: " + UID_1 + ":lke:value", exception.getMessage());
  }

  @Test
  void shouldParseFiltersWithDateRangeContainingEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters(
            UID_1 + ":ge:2020-01-01T00/:00/:00.001 +05/:30:le:2021-01-01T00/:00/:00.001 +05/:30");

    assertEquals(
        Map.of(
            UID_1,
            List.of(
                new QueryFilter(QueryOperator.GE, "2020-01-01T00:00:00.001 +05:30"),
                new QueryFilter(QueryOperator.LE, "2021-01-01T00:00:00.001 +05:30"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithTextRangeContainingEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1 + ":sw:project/:x:ew:project/:le/:");

    assertEquals(
        Map.of(
            UID_1,
            List.of(
                new QueryFilter(QueryOperator.SW, "project:x"),
                new QueryFilter(QueryOperator.EW, "project:le:"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithEscapedSlashAndComma() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters =
        parseFilters(
            UID_1
                + ":eq:project///,/,//"
                + ","
                + UID_2
                + ":eq:project//"
                + ","
                + UID_3
                + ":eq:project//");

    assertEquals(
        Map.of(
            UID_1, List.of(new QueryFilter(QueryOperator.EQ, "project/,,/")),
            UID_2, List.of(new QueryFilter(QueryOperator.EQ, "project/")),
            UID_3, List.of(new QueryFilter(QueryOperator.EQ, "project/"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithMultipleOperatorsAndEscapedColon() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1 + ":like:value1/::like:value2");

    assertEquals(
        Map.of(
            UID_1,
            List.of(
                new QueryFilter(QueryOperator.LIKE, "value1:"),
                new QueryFilter(QueryOperator.LIKE, "value2"))),
        filters);
  }

  @Test
  void shouldParseFiltersWithSingleUnaryOperator() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1 + ":!null");

    assertEquals(Map.of(UID_1, List.of(new QueryFilter(QueryOperator.NNULL))), filters);
  }

  @Test
  void shouldParseFiltersWithSingleIdentifierAndMultipleUnaryOperators()
      throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1 + ":!null:null");

    assertEquals(
        Map.of(
            UID_1,
            List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NULL))),
        filters);
  }

  @Test
  void shouldParseFiltersWithCombinedUnaryAndBinaryOperators() throws BadRequestException {
    Map<UID, List<QueryFilter>> filters = parseFilters(UID_1 + ":null:gt:10");

    assertEquals(
        Map.of(
            UID_1,
            List.of(new QueryFilter(QueryOperator.NULL), new QueryFilter(QueryOperator.GT, "10"))),
        filters);
  }

  @Test
  void shouldFailParsingFiltersWithUnaryOperatorHavingAValue() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":!null:value"));
    assertEquals(
        "Operator '!null' in filter can't be used with a value: " + UID_1 + ":!null:value",
        exception.getMessage());
  }

  @Test
  void shouldFailParsingFiltersWithUnaryAndBinaryOperatorsCombinedAndUnaryOperatorHavingAValue() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":gt:10:null:value"));
    assertEquals(
        "Operator 'null' in filter can't be used with a value: " + UID_1 + ":gt:10:null:value",
        exception.getMessage());
  }

  @Test
  void shouldFailParsingFiltersWithMoreThanTwoOperatorsForASingleIdentifier() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":gt:10:null:!null"));
    assertEquals(
        "A maximum of two operators can be used in a filter: " + UID_1 + ":gt:10:null:!null",
        exception.getMessage());
  }

  @Test
  void shouldFailParsingFiltersWithMultipleBinaryOperatorsAndOneHasNoValue() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(UID_1 + ":gt:10:lt"));
    assertEquals("Query item or filter is invalid: " + UID_1 + ":gt:10:lt", exception.getMessage());
  }
}
