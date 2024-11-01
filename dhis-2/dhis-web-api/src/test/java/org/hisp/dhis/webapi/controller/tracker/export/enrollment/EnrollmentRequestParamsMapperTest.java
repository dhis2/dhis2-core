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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.export.enrollment.EnrollmentRequestParams.DEFAULT_FIELDS_PARAM;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class EnrollmentRequestParamsMapperTest {

  private static final UID ORG_UNIT_1_UID = UID.of("lW0T2U7gZUi");

  private static final UID ORG_UNIT_2_UID = UID.of("TK4KA0IIWqa");

  private static final UID PROGRAM_UID = UID.of("XhBYIraw7sv");

  private static final UID TRACKED_ENTITY_TYPE_UID = UID.of("Dp8baZYrLtr");

  private static final UID TRACKED_ENTITY_UID = UID.of("DGbr8GHG4li");

  @Mock private EnrollmentFieldsParamMapper fieldsParamMapper;

  @InjectMocks private EnrollmentRequestParamsMapper mapper;

  @BeforeEach
  void setUp() {
    when(fieldsParamMapper.map(anyList())).thenReturn(EnrollmentParams.FALSE);
  }

  @Test
  void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();

    mapper.map(enrollmentRequestParams);

    verify(fieldsParamMapper, times(1)).map(FieldFilterParser.parse(DEFAULT_FIELDS_PARAM));
  }

  @Test
  void testMappingOrgUnit() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrgUnit(ORG_UNIT_1_UID.getValue() + ";" + ORG_UNIT_2_UID.getValue());
    enrollmentRequestParams.setProgram(PROGRAM_UID);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertContainsOnly(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID), params.getOrgUnits());
  }

  @Test
  void testMappingOrgUnits() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrgUnits(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID));
    enrollmentRequestParams.setProgram(PROGRAM_UID);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertContainsOnly(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID), params.getOrgUnits());
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrgUnitMode(OrganisationUnitSelectionMode.CAPTURE);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertEquals(OrganisationUnitSelectionMode.CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeGivenOuModeParam() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOuMode(OrganisationUnitSelectionMode.CAPTURE);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertEquals(OrganisationUnitSelectionMode.CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldThrowIfDeprecatedAndNewOrgUnitModeParameterIsSet() {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOuMode(OrganisationUnitSelectionMode.SELECTED);
    enrollmentRequestParams.setOrgUnitMode(OrganisationUnitSelectionMode.SELECTED);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(enrollmentRequestParams));

    assertStartsWith("Only one parameter of 'ouMode' and 'orgUnitMode'", exception.getMessage());
  }

  @Test
  void shouldFailIfDeprecatedAndNewStatusParameterIsSet() {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);
    enrollmentRequestParams.setStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(enrollmentRequestParams));

    assertStartsWith("Only one parameter of 'programStatus' and 'status'", exception.getMessage());
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setProgram(PROGRAM_UID);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertEquals(PROGRAM_UID, params.getProgram());
  }

  @Test
  void testMappingTrackedEntityType() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setTrackedEntityType(TRACKED_ENTITY_TYPE_UID);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertEquals(TRACKED_ENTITY_TYPE_UID, params.getTrackedEntityType());
  }

  @Test
  void testMappingTrackedEntity() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setTrackedEntity(TRACKED_ENTITY_UID);

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertEquals(TRACKED_ENTITY_UID, params.getTrackedEntity());
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrder(
        OrderCriteria.fromOrderString("enrolledAt:desc,createdAt:asc"));

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertEquals(
        List.of(
            new Order("enrollmentDate", SortDirection.DESC),
            new Order("created", SortDirection.ASC)),
        params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(enrollmentRequestParams));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();

    EnrollmentOperationParams params = mapper.map(enrollmentRequestParams);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldFailWhenProgramAndTrackedEntityTypeProvided() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setProgram(UID.of("madeUpUid01"));
    requestParams.setTrackedEntityType(UID.of("madeUpUid02"));

    Exception badRequestException =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "`program` and `trackedEntityType` cannot be specified simultaneously",
        badRequestException.getMessage());
  }

  @Test
  void shouldFailIfProgramStatusIsSetWithoutProgram() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setProgramStatus(EnrollmentStatus.ACTIVE);

    Exception exception =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertStartsWith("`program` must be defined when `programStatus`", exception.getMessage());
  }

  @Test
  void shouldFailIfEnrollmentStatusIsSetWithoutProgram() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setStatus(EnrollmentStatus.ACTIVE);

    Exception exception =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertStartsWith("`program` must be defined when `status`", exception.getMessage());
  }

  @Test
  void shouldFailWhenFollowUpProvidedAndProgramNotPresent() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setFollowUp(true);

    Exception badRequestException =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "`program` must be defined when `followUp` status is defined",
        badRequestException.getMessage());
  }

  @Test
  void shouldFailWhenEnrolledAfterProvidedAndProgramNotPresent() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setEnrolledAfter(StartDateTime.of("2020-01-01"));

    Exception badRequestException =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "`program` must be defined when `enrolledAfter` is specified",
        badRequestException.getMessage());
  }

  @Test
  void shouldFailWhenEnrolledBeforeProvidedAndProgramNotPresent() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setEnrolledBefore(EndDateTime.of("2020-01-01"));

    Exception badRequestException =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "`program` must be defined when `enrolledBefore` is specified",
        badRequestException.getMessage());
  }

  @Test
  void shouldFailWhenUpdatedWithinAndUpdatedAfterProvided() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setUpdatedAfter(StartDateTime.of("2020-01-01"));
    requestParams.setUpdatedWithin("2h");

    Exception badRequestException =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "`updatedAfter` and `updatedWithin` cannot be specified simultaneously",
        badRequestException.getMessage());
  }

  @Test
  void shouldFailWhenUpdatedWithinProvidedButNotValid() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setUpdatedWithin("invalid value");

    Exception badRequestException =
        Assertions.assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        String.format("`updatedWithin` is not valid: %s", requestParams.getUpdatedWithin()),
        badRequestException.getMessage());
  }
}
