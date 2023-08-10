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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.export.OperationParamUtils.parseQueryItem;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria.fromOrderString;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.parseFilters;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrgUnitMode;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.OperationParamUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests {@link RequestParamsValidator}. */
class RequestParamsValidatorTest {

  private static final String TEA_1_UID = "TvjwTPToKHO";

  private static final String TEA_2_UID = "cy2oRh2sNr6";

  private Map<String, TrackedEntityAttribute> attributes;

  private static final OrganisationUnit orgUnit = new OrganisationUnit();

  @BeforeEach
  void setUp() {
    attributes =
        Map.of(
            TEA_1_UID, trackedEntityAttribute(TEA_1_UID),
            TEA_2_UID, trackedEntityAttribute(TEA_2_UID));
  }

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
  void testParseQueryItem() throws BadRequestException {
    String param = TEA_1_UID + ":lt:20:gt:10";

    QueryItem item = parseQueryItem(param, id -> new QueryItem(attributes.get(id)));

    assertNotNull(item);
    assertAll(
        () -> assertEquals(attributes.get(TEA_1_UID), item.getItem()),
        // QueryItem equals() does not take the QueryFilter into account, so
        // we need to assert on filters separately
        () ->
            assertEquals(
                List.of(
                    new QueryFilter(QueryOperator.LT, "20"),
                    new QueryFilter(QueryOperator.GT, "10")),
                item.getFilters()));
  }

  @Test
  void testParseQueryItemWithOnlyIdentifier() throws BadRequestException {
    QueryItem item = parseQueryItem(TEA_1_UID, id -> new QueryItem(attributes.get(id)));

    assertNotNull(item);
    assertAll(
        () -> assertEquals(attributes.get(TEA_1_UID), item.getItem()),
        () -> assertIsEmpty(item.getFilters()));
  }

  @Test
  void testParseQueryItemWithIdentifierAndTrailingColon() throws BadRequestException {
    String param = TEA_1_UID + ":";

    QueryItem item = parseQueryItem(param, id -> new QueryItem(attributes.get(id)));

    assertNotNull(item);
    assertAll(
        () -> assertEquals(attributes.get(TEA_1_UID), item.getItem()),
        () -> assertIsEmpty(item.getFilters()));
  }

  @Test
  void testParseQueryItemWithMissingValue() {
    String param = TEA_1_UID + ":lt";

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> parseQueryItem(param, id -> new QueryItem(attributes.get(id))));
    assertEquals("Query item or filter is invalid: " + param, exception.getMessage());
  }

  @Test
  void testParseQueryItemWithMissingValueAndTrailingColon() {
    String param = TEA_1_UID + ":lt:";

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> parseQueryItem(param, id -> new QueryItem(attributes.get(id))));
    assertEquals("Query item or filter is invalid: " + param, exception.getMessage());
  }

  @Test
  void testParseAttributeQueryItemWhenNoTEAExist() {
    String param = TEA_1_UID + ":eq:2";
    Map<String, TrackedEntityAttribute> attributes = Collections.emptyMap();

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> OperationParamUtils.parseAttributeQueryItems(param, attributes));
    assertEquals("Attribute does not exist: " + TEA_1_UID, exception.getMessage());
  }

  @Test
  void testParseAttributeQueryWhenTEAInFilterDoesNotExist() {
    String param = "JM5zWuf1mkb:eq:2";

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> OperationParamUtils.parseAttributeQueryItems(param, attributes));
    assertEquals("Attribute does not exist: JM5zWuf1mkb", exception.getMessage());
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
  void shouldCreateQueryFiltersWhenQueryHasOperatorAndValueWithDelimiter()
      throws BadRequestException {
    assertEquals(
        new QueryFilter(QueryOperator.LIKE, "project:x"),
        OperationParamUtils.parseQueryFilter("like:project/:x"));
  }

  private TrackedEntityAttribute trackedEntityAttribute(String uid) {
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setUid(uid);
    return tea;
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeAccessible() {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), ACCESSIBLE));

    assertStartsWith(
        "orgUnitMode ACCESSIBLE cannot be used with orgUnits.", exception.getMessage());
  }

  @Test
  void shouldPassWhenNoOrgUnitSuppliedAndOrgUnitModeAccessible() {
    assertDoesNotThrow(() -> validateOrgUnitMode(emptySet(), ACCESSIBLE));
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeCapture() {
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), CAPTURE));

    assertStartsWith("orgUnitMode CAPTURE cannot be used with orgUnits.", exception.getMessage());
  }

  @Test
  void shouldPassWhenNoOrgUnitSuppliedAndOrgUnitModeCapture() {
    assertDoesNotThrow(() -> validateOrgUnitMode(emptySet(), CAPTURE));
  }

  @Test
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeSelected() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), SELECTED));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: SELECTED", exception.getMessage());
  }

  @Test
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeSelected() {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), SELECTED));
  }

  @Test
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeDescendants() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), DESCENDANTS));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: DESCENDANTS", exception.getMessage());
  }

  @Test
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeDescendants() {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), DESCENDANTS));
  }

  @Test
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeChildren() {
    Exception exception =
        assertThrows(BadRequestException.class, () -> validateOrgUnitMode(emptySet(), CHILDREN));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: CHILDREN", exception.getMessage());
  }

  @Test
  void shouldPassWhenOrgUnitSuppliedAndOrgUnitModeChildren() {
    assertDoesNotThrow(() -> validateOrgUnitMode(Set.of(UID.of(orgUnit.getUid())), CHILDREN));
  }
}
