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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.Test;

class EnrollmentRequestParamsMapperTest {

  private static final UID ORG_UNIT_1_UID = UID.of("lW0T2U7gZUi");

  private static final UID ORG_UNIT_2_UID = UID.of("TK4KA0IIWqa");

  private static final UID PROGRAM_UID = UID.of("XhBYIraw7sv");

  private static final UID TRACKED_ENTITY_UID = UID.of("DGbr8GHG4li");

  @Test
  void testMappingOrgUnits() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrgUnits(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID));
    enrollmentRequestParams.setProgram(PROGRAM_UID);

    EnrollmentOperationParams params = EnrollmentRequestParamsMapper.map(enrollmentRequestParams);

    assertContainsOnly(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID), params.getOrgUnits());
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrgUnitMode(OrganisationUnitSelectionMode.CAPTURE);
    enrollmentRequestParams.setProgram(UID.generate());

    EnrollmentOperationParams params = EnrollmentRequestParamsMapper.map(enrollmentRequestParams);

    assertEquals(OrganisationUnitSelectionMode.CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldFailIfDeprecatedAndNewStatusParameterIsSet() {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);
    enrollmentRequestParams.setStatus(EnrollmentStatus.ACTIVE);
    enrollmentRequestParams.setProgram(UID.generate());

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> EnrollmentRequestParamsMapper.map(enrollmentRequestParams));

    assertStartsWith("Only one parameter of 'programStatus' and 'status'", exception.getMessage());
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setProgram(PROGRAM_UID);

    EnrollmentOperationParams params = EnrollmentRequestParamsMapper.map(enrollmentRequestParams);

    assertEquals(PROGRAM_UID, params.getProgram());
  }

  @Test
  void testMappingTrackedEntity() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setTrackedEntity(TRACKED_ENTITY_UID);
    enrollmentRequestParams.setProgram(UID.generate());

    EnrollmentOperationParams params = EnrollmentRequestParamsMapper.map(enrollmentRequestParams);

    assertEquals(TRACKED_ENTITY_UID, params.getTrackedEntity());
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setOrder(
        OrderCriteria.fromOrderString("enrolledAt:desc,createdAt:asc"));
    enrollmentRequestParams.setProgram(UID.generate());

    EnrollmentOperationParams params = EnrollmentRequestParamsMapper.map(enrollmentRequestParams);

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
    enrollmentRequestParams.setProgram(UID.generate());

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> EnrollmentRequestParamsMapper.map(enrollmentRequestParams));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    EnrollmentRequestParams enrollmentRequestParams = new EnrollmentRequestParams();
    enrollmentRequestParams.setProgram(UID.generate());

    EnrollmentOperationParams params = EnrollmentRequestParamsMapper.map(enrollmentRequestParams);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldFailWhenProgramNotProvided() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();

    Exception badRequestException =
        assertThrows(
            BadRequestException.class, () -> EnrollmentRequestParamsMapper.map(requestParams));

    assertEquals("Program is mandatory", badRequestException.getMessage());
  }

  @Test
  void shouldFailWhenUpdatedWithinAndUpdatedAfterProvided() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setUpdatedAfter(StartDateTime.of("2020-01-01"));
    requestParams.setUpdatedWithin("2h");
    requestParams.setProgram(UID.generate());

    Exception badRequestException =
        assertThrows(
            BadRequestException.class, () -> EnrollmentRequestParamsMapper.map(requestParams));

    assertEquals(
        "`updatedAfter` and `updatedWithin` cannot be specified simultaneously",
        badRequestException.getMessage());
  }

  @Test
  void shouldFailWhenUpdatedWithinProvidedButNotValid() {
    EnrollmentRequestParams requestParams = new EnrollmentRequestParams();
    requestParams.setUpdatedWithin("invalid value");
    requestParams.setProgram(UID.generate());

    Exception badRequestException =
        assertThrows(
            BadRequestException.class, () -> EnrollmentRequestParamsMapper.map(requestParams));

    assertEquals(
        String.format("`updatedWithin` is not valid: %s", requestParams.getUpdatedWithin()),
        badRequestException.getMessage());
  }
}
