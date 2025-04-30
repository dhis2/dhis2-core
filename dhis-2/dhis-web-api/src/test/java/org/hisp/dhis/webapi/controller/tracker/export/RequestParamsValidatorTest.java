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

import static java.util.Collections.emptySet;
import static org.hisp.dhis.common.OrderCriteria.fromOrderString;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateFilter;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrgUnitModeForEnrollmentsAndEvents;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validateOrgUnitModeForTrackedEntities;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.stream.Stream;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.webapi.controller.tracker.PageRequestParams;
import org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests {@link RequestParamsValidator}. */
class RequestParamsValidatorTest {

  private static final UID TEA_1_UID = UID.of("TvjwTPToKHO");

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
  void shouldValidateFilterWhenFormatIsCorrect() throws BadRequestException {
    Set<Pair<String, Class<?>>> supportedFields =
        Set.of(Pair.of("field1", String.class), Pair.of("field2", String.class));

    validateFilter("field2:eq:value", supportedFields);
  }

  @Test
  void shouldFailWhenChangeLogFilterFieldNotSupported() {
    Set<Pair<String, Class<?>>> supportedFields =
        Set.of(Pair.of("field1", String.class), Pair.of("field2", String.class));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> validateFilter("unknownField:eq:value", supportedFields));
    assertStartsWith(
        String.format(
            "Invalid filter field. Supported fields are '%s'.",
            String.join(", ", supportedFields.stream().map(Pair::getKey).sorted().toList())),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenChangeLogFilterOperatorNotSupported() {
    Set<Pair<String, Class<?>>> supportedFields =
        Set.of(Pair.of("field1", String.class), Pair.of("field2", String.class));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> validateFilter("field1:sw:value", supportedFields));
    assertStartsWith(
        "Invalid filter operator. The only supported operator is 'eq'.", exception.getMessage());
  }

  @Test
  void shouldFailWhenChangeLogFilterDoesNotHaveCorrectFormat() {
    Set<Pair<String, Class<?>>> supportedFields =
        Set.of(Pair.of("field1", String.class), Pair.of("field2", String.class));
    String invalidFilter = "field1:eq";

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> validateFilter(invalidFilter, supportedFields));
    assertStartsWith(
        String.format(
            "Invalid filter => %s. Expected format is [field]:eq:[value].", invalidFilter),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenChangeLogFilterDoesNotHaveCorrectUidFormat() {
    Set<Pair<String, Class<?>>> supportedFields = Set.of(Pair.of("field1", UID.class));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> validateFilter("field1:eq:QRYjLTiJTr", supportedFields));
    assertStartsWith("Incorrect filter value provided as UID: QRYjLTiJTr", exception.getMessage());
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
            () -> validateOrgUnitModeForEnrollmentsAndEvents(Set.of(UID.of(orgUnit)), orgUnitMode));

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
    assertDoesNotThrow(() -> validateOrgUnitModeForEnrollmentsAndEvents(emptySet(), orgUnitMode));
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    assertDoesNotThrow(
        () -> validateOrgUnitModeForEnrollmentsAndEvents(Set.of(UID.of(orgUnit)), orgUnitMode));
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldPassWhenTrackedEntitySuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    assertDoesNotThrow(
        () -> validateOrgUnitModeForTrackedEntities(emptySet(), orgUnitMode, Set.of(TEA_1_UID)));
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitModeForEnrollmentsAndEvents(emptySet(), orgUnitMode));

    assertStartsWith(
        String.format("At least one org unit is required for orgUnitMode: %s", orgUnitMode),
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenNoOrgUnitNorTrackedEntitySuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitModeForTrackedEntities(emptySet(), orgUnitMode, emptySet()));

    assertStartsWith(
        String.format(
            "At least one org unit or tracked entity is required for orgUnitMode: %s", orgUnitMode),
        exception.getMessage());
  }

  @Data
  private static class PaginationParameters implements PageRequestParams {
    private Integer page;
    private Integer pageSize;
    private boolean totalPages;
    private boolean paging;
  }

  private static Stream<Arguments> mutuallyExclusivePaginationParameters() {
    return Stream.of(
        arguments(null, null, true),
        arguments(null, 1, false),
        arguments(null, 1, true),
        arguments(1, null, false),
        arguments(1, null, true),
        arguments(1, 1, true));
  }

  @MethodSource("mutuallyExclusivePaginationParameters")
  @ParameterizedTest
  void shouldFailWhenGivenMutuallyExclusivePaginationParameters(
      Integer page, Integer pageSize, boolean totalPages) {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPage(page);
    paginationParameters.setPageSize(pageSize);
    paginationParameters.setTotalPages(totalPages);
    paginationParameters.setPaging(false);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> validatePaginationParameters(paginationParameters));

    assertStartsWith("Paging cannot be disabled with", exception.getMessage());
  }

  private static Stream<Arguments> validPaginationParameters() {
    return Stream.of(
        arguments(null, null, false, true),
        arguments(null, null, true, true),
        arguments(null, 1, false, true),
        arguments(null, 1, true, true),
        arguments(1, null, false, true),
        arguments(1, null, true, true),
        arguments(1, 1, false, true),
        arguments(1, 1, true, true),
        arguments(null, null, false, false));
  }

  @MethodSource("validPaginationParameters")
  @ParameterizedTest
  void shouldPassWhenGivenValidPaginationParameters(
      Integer page, Integer pageSize, boolean totalPages, boolean paging)
      throws BadRequestException {
    PaginationParameters paginationParameters = new PaginationParameters();
    paginationParameters.setPage(page);
    paginationParameters.setPage(pageSize);
    paginationParameters.setTotalPages(totalPages);
    paginationParameters.setPaging(paging);

    validatePaginationParameters(paginationParameters);
  }
}
