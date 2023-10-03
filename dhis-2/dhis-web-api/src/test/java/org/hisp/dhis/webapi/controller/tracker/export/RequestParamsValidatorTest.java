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
package org.hisp.dhis.webapi.controller.tracker.export;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria.fromOrderString;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.parseFilters;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrgUnitMode;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validatePaginationParameters;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Data;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests {@link RequestParamsValidator}. */
class RequestParamsValidatorTest {

  private static final String TEA_1_UID = "TvjwTPToKHO";

  private static final String TEA_2_UID = "cy2oRh2sNr6";

  public static final String TEA_3_UID = "cy2oRh2sNr7";

  private static final OrganisationUnit orgUnit = new OrganisationUnit();

  @Test
  void shouldPassOrderParamsValidationWhenGivenOrderIsOrderable() throws BadRequestException {
    Set<String> supportedFieldNames = Set.of("createdAt", "scheduledAt");

    validateOrderParams(fromOrderString("createdAt:asc,scheduledAt:asc"), supportedFieldNames, "");
  }

  @Test
  void shouldFailOrderParamsValidationWhenGivenInvalidOrderComponents() {
    Set<String> supportedFieldNames = Set.of("enrolledAt");
    String invalidUID = "Cogn34Del";
    assertFalse(CodeGenerator.isValidUid(invalidUID));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                validateOrderParams(
                    fromOrderString(
                        "unsupportedProperty1:asc,enrolledAt:asc,"
                            + invalidUID
                            + ",unsupportedProperty2:desc"),
                    supportedFieldNames,
                    "data element and attribute"));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () ->
            assertContains(
                "Supported are data element and attribute UIDs and fields", exception.getMessage()),
        // order of fields might not always be the same; therefore using contains
        () -> assertContains(invalidUID, exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()),
        () -> assertContains("unsupportedProperty2", exception.getMessage()));
  }

  @Test
  void shouldPassOrderParamsValidationWhenGivenInvalidOrderNameWhichIsAValidUID()
      throws BadRequestException {
    Set<String> supportedFieldNames = Set.of("enrolledAt");
    // This test case shows that some field names are valid UIDs. We can thus not rule out all
    // invalid field names and UIDs at this stage as we do not have access to data element/attribute
    // services. Such invalid order values will be caught in the service (mapper).
    assertTrue(CodeGenerator.isValidUid("lastUpdated"));

    validateOrderParams(fromOrderString("lastUpdated:desc"), supportedFieldNames, "");
  }

  @Test
  void shouldFailOrderParamsValidationWhenGivenRepeatedOrderComponents() {
    Set<String> supportedFieldNames = Set.of("createdAt", "enrolledAt");

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                validateOrderParams(
                    fromOrderString(
                        "zGlzbfreTOH,createdAt:asc,enrolledAt:asc,enrolledAt,zGlzbfreTOH"),
                    supportedFieldNames,
                    ""));

    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        // order of fields might not always be the same; therefore using contains
        () -> assertContains("repeated", exception.getMessage()),
        () -> assertContains("enrolledAt", exception.getMessage()),
        () -> assertContains("zGlzbfreTOH", exception.getMessage()));
  }

  @Test
  void shouldParseFilters() throws BadRequestException {
    Map<String, List<QueryFilter>> filters =
        parseFilters(TEA_1_UID + ":lt:20:gt:10," + TEA_2_UID + ":like:foo");

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(
                new QueryFilter(QueryOperator.LT, "20"), new QueryFilter(QueryOperator.GT, "10")),
            TEA_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo"))),
        filters);
  }

  @Test
  void shouldParseFiltersGivenRepeatedUID() throws BadRequestException {
    Map<String, List<QueryFilter>> filters =
        parseFilters(TEA_1_UID + ":lt:20," + TEA_2_UID + ":like:foo," + TEA_1_UID + ":gt:10");

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(
                new QueryFilter(QueryOperator.LT, "20"), new QueryFilter(QueryOperator.GT, "10")),
            TEA_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo"))),
        filters);
  }

  @Test
  void shouldParseFiltersOnlyContainingAnIdentifier() throws BadRequestException {
    Map<String, List<QueryFilter>> filters = parseFilters(TEA_1_UID);

    assertEquals(Map.of(TEA_1_UID, List.of()), filters);
  }

  @Test
  void shouldParseFiltersWithIdentifierAndTrailingColon() throws BadRequestException {
    Map<String, List<QueryFilter>> filters = parseFilters(TEA_1_UID + ":");

    assertEquals(Map.of(TEA_1_UID, List.of()), filters);
  }

  @Test
  void shouldParseFiltersGivenBlankInput() throws BadRequestException {
    Map<String, List<QueryFilter>> filters = parseFilters(" ");

    assertTrue(filters.isEmpty());
  }

  @Test
  void shouldFailParsingFiltersMissingAValue() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(TEA_1_UID + ":lt"));
    assertEquals("Query item or filter is invalid: " + TEA_1_UID + ":lt", exception.getMessage());
  }

  @Test
  void shouldFailParsingFiltersWithMissingValueAndTrailingColon() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> parseFilters(TEA_1_UID + ":lt:"));
    assertEquals("Query item or filter is invalid: " + TEA_1_UID + ":lt:", exception.getMessage());
  }

  @Test
  void shouldParseFiltersWithFilterNameHasSeparationCharInIt() throws BadRequestException {
    Map<String, List<QueryFilter>> filters = parseFilters(TEA_2_UID + ":like:project/:x/:eq/:2");

    assertEquals(
        Map.of(TEA_2_UID, List.of(new QueryFilter(QueryOperator.LIKE, "project:x:eq:2"))), filters);
  }

  @Test
  void shouldThrowBadRequestWhenFilterHasOperatorInWrongFormat() {
    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> parseFilters(TEA_1_UID + ":lke:value"));
    assertEquals(
        "Query item or filter is invalid: " + TEA_1_UID + ":lke:value", exception.getMessage());
  }

  @Test
  void shouldParseFilterWhenFilterHasDatesFormatDateWithMilliSecondsAndTimeZone()
      throws BadRequestException {
    Map<String, List<QueryFilter>> filters =
        parseFilters(
            TEA_1_UID
                + ":ge:2020-01-01T00/:00/:00.001 +05/:30:le:2021-01-01T00/:00/:00.001 +05/:30");

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(
                new QueryFilter(QueryOperator.GE, "2020-01-01T00:00:00.001 +05:30"),
                new QueryFilter(QueryOperator.LE, "2021-01-01T00:00:00.001 +05:30"))),
        filters);
  }

  @Test
  void shouldParseFilterWhenFilterHasMultipleOperatorAndTextRange() throws BadRequestException {
    Map<String, List<QueryFilter>> filters =
        parseFilters(TEA_1_UID + ":sw:project/:x:ew:project/:le/:");

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(
                new QueryFilter(QueryOperator.SW, "project:x"),
                new QueryFilter(QueryOperator.EW, "project:le:"))),
        filters);
  }

  @Test
  void shouldParseFilterWhenMultipleFiltersAreMixedCommaAndSlash() throws BadRequestException {
    Map<String, List<QueryFilter>> filters =
        parseFilters(
            TEA_1_UID
                + ":eq:project///,/,//"
                + ","
                + TEA_2_UID
                + ":eq:project//"
                + ","
                + TEA_3_UID
                + ":eq:project//");

    assertEquals(
        Map.of(
            TEA_1_UID, List.of(new QueryFilter(QueryOperator.EQ, "project/,,/")),
            TEA_2_UID, List.of(new QueryFilter(QueryOperator.EQ, "project/")),
            TEA_3_UID, List.of(new QueryFilter(QueryOperator.EQ, "project/"))),
        filters);
  }

  @Test
  void shouldParseFilterWhenFilterHasMultipleOperatorWithFinalColon() throws BadRequestException {
    Map<String, List<QueryFilter>> filters = parseFilters(TEA_1_UID + ":like:value1/::like:value2");

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(
                new QueryFilter(QueryOperator.LIKE, "value1:"),
                new QueryFilter(QueryOperator.LIKE, "value2"))),
        filters);
  }

  private TrackedEntityAttribute trackedEntityAttribute(String uid) {
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setUid(uid);
    return tea;
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"CAPTURE", "ACCESSIBLE", "ALL"})
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeDoesNotRequireOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitMode(Set.of(UID.of(orgUnit)), orgUnitMode));

    assertStartsWith(
        String.format("orgUnitMode %s cannot be used with orgUnits.", orgUnitMode),
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"CAPTURE", "ACCESSIBLE", "ALL"})
  void shouldPassWhenNoOrgUnitSuppliedAndOrgUnitModeDoesNotRequireOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    assertDoesNotThrow(() -> validateOrgUnitMode(emptySet(), orgUnitMode));
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit)), orgUnitMode));
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), orgUnitMode));

    assertStartsWith(
        String.format("At least one org unit is required for orgUnitMode: %s", orgUnitMode),
        exception.getMessage());
  }

  @Data
  private static class PaginationParameters implements PageRequestParams {
    private Integer page;
    private Integer pageSize;
    private Boolean totalPages;
    private Boolean skipPaging;
  }

  private static Stream<Arguments> mutuallyExclusivePaginationParameters() {
    return Stream.of(
        arguments(null, 1, null, true),
        arguments(null, 1, false, true),
        arguments(null, 1, false, true),
        arguments(1, 1, false, true),
        arguments(1, 1, true, true),
        arguments(null, null, true, true));
  }

  @MethodSource("mutuallyExclusivePaginationParameters")
  @ParameterizedTest
  void shouldFailWhenGivenMutuallyExclusivePaginationParameters(
      Integer page, Integer pageSize, Boolean totalPages, Boolean skipPaging) {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPage(page);
    paginationParameters.setPageSize(pageSize);
    paginationParameters.setTotalPages(totalPages);
    paginationParameters.setSkipPaging(skipPaging);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> validatePaginationParameters(paginationParameters));

    assertStartsWith("Paging cannot be skipped with", exception.getMessage());
  }

  private static Stream<Arguments> validPaginationParameters() {
    return Stream.of(
        arguments(null, null, null, null),
        arguments(null, null, null, false),
        arguments(null, 1, true, null),
        arguments(null, 1, false, null),
        arguments(null, 1, false, false),
        arguments(null, null, true, false),
        arguments(1, 1, false, false),
        arguments(null, null, true, null),
        arguments(null, 1, true, false),
        arguments(null, null, null, true),
        arguments(null, null, false, true));
  }

  @MethodSource("validPaginationParameters")
  @ParameterizedTest
  void shouldPassWhenGivenValidPaginationParameters(
      Integer page, Integer pageSize, Boolean totalPages, Boolean skipPaging)
      throws BadRequestException {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPage(page);
    paginationParameters.setPage(pageSize);
    paginationParameters.setTotalPages(totalPages);
    paginationParameters.setSkipPaging(skipPaging);

    validatePaginationParameters(paginationParameters);
  }

  @ValueSource(ints = {-1, 0})
  @ParameterizedTest
  void shouldFailWhenGivenPageLessThanOrEqualToZero(int page) {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPage(page);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> validatePaginationParameters(paginationParameters));

    assertStartsWith("page must be greater", exception.getMessage());
  }

  @ValueSource(ints = {1, 2})
  @ParameterizedTest
  void shouldPassWhenGivenPageGreaterThanOrEqualToOne(int page) throws BadRequestException {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPage(page);

    validatePaginationParameters(paginationParameters);
  }

  @ValueSource(ints = {-1, 0})
  @ParameterizedTest
  void shouldFailWhenGivenPageSizeLessThanOrEqualToZero(int pageSize) {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPageSize(pageSize);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> validatePaginationParameters(paginationParameters));

    assertStartsWith("pageSize must be greater", exception.getMessage());
  }

  @ValueSource(ints = {1, 2})
  @ParameterizedTest
  void shouldPassWhenGivenPageSizeGreaterThanOrEqualToOne(int pageSize) throws BadRequestException {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPageSize(pageSize);

    validatePaginationParameters(paginationParameters);
  }
}
