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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.LIKE;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackedEntityRequestParamsMapperTest {
  public static final UID TEA_1_UID = UID.of("TvjwTPToKHO");

  public static final UID TEA_2_UID = UID.of("cy2oRh2sNr6");

  private static final UID PROGRAM_UID = UID.of("XhBYIraw7sv");

  private static final UID PROGRAM_STAGE_UID = UID.of("RpCr2u2pFqw");

  private static final UID TRACKED_ENTITY_TYPE_UID = UID.of("Dp8baZYrLtr");

  private TrackedEntityRequestParams trackedEntityRequestParams;

  UserDetails user = new SystemUser();

  @BeforeEach
  public void setUp() {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
  }

  @Test
  void shouldMapCorrectlyWhenProgramAndSpecificUpdateDatesSupplied() throws BadRequestException {
    trackedEntityRequestParams.setOrgUnitMode(CAPTURE);
    trackedEntityRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);
    trackedEntityRequestParams.setProgram(PROGRAM_UID);
    trackedEntityRequestParams.setProgramStage(PROGRAM_STAGE_UID);
    trackedEntityRequestParams.setFollowUp(true);
    trackedEntityRequestParams.setUpdatedAfter(StartDateTime.of("2019-01-01"));
    trackedEntityRequestParams.setUpdatedBefore(EndDateTime.of("2020-01-01"));
    trackedEntityRequestParams.setEnrollmentOccurredAfter(StartDateTime.of("2019-05-05"));
    trackedEntityRequestParams.setEnrollmentOccurredBefore(EndDateTime.of("2020-05-05"));
    trackedEntityRequestParams.setEventStatus(EventStatus.COMPLETED);
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2019-07-07"));
    trackedEntityRequestParams.setEventOccurredBefore(EndDateTime.of("2020-07-07"));
    trackedEntityRequestParams.setIncludeDeleted(true);

    final TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertThat(params.getProgram(), is(PROGRAM_UID));
    assertThat(params.getProgramStage(), is(PROGRAM_STAGE_UID));
    assertThat(params.getFollowUp(), is(true));
    assertThat(
        params.getLastUpdatedStartDate(),
        is(trackedEntityRequestParams.getUpdatedAfter().toDate()));
    assertThat(
        params.getLastUpdatedEndDate(), is(trackedEntityRequestParams.getUpdatedBefore().toDate()));
    assertThat(
        params.getProgramIncidentStartDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredAfter().toDate()));
    assertThat(
        params.getProgramIncidentEndDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredBefore().toDate()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(
        params.getEventStartDate(),
        is(trackedEntityRequestParams.getEventOccurredAfter().toDate()));
    assertThat(
        params.getEventEndDate(), is(trackedEntityRequestParams.getEventOccurredBefore().toDate()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
  }

  @Test
  void shouldMapCorrectlyWhenTrackedEntityAndSpecificUpdatedRangeSupplied()
      throws BadRequestException {
    trackedEntityRequestParams.setOrgUnitMode(CAPTURE);
    trackedEntityRequestParams.setUpdatedWithin("20h");
    trackedEntityRequestParams.setTrackedEntityType(TRACKED_ENTITY_TYPE_UID);
    trackedEntityRequestParams.setEventStatus(EventStatus.COMPLETED);
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2019-07-07"));
    trackedEntityRequestParams.setEventOccurredBefore(EndDateTime.of("2020-07-07"));
    trackedEntityRequestParams.setIncludeDeleted(true);

    final TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertThat(params.getTrackedEntityType(), is(TRACKED_ENTITY_TYPE_UID));
    assertThat(params.getLastUpdatedStartDate(), is(trackedEntityRequestParams.getUpdatedAfter()));
    assertThat(params.getLastUpdatedEndDate(), is(trackedEntityRequestParams.getUpdatedBefore()));
    assertThat(
        params.getProgramIncidentStartDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredAfter()));
    assertThat(
        params.getProgramIncidentEndDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredBefore()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(
        params.getEventStartDate(),
        is(trackedEntityRequestParams.getEventOccurredAfter().toDate()));
    assertThat(
        params.getEventEndDate(), is(trackedEntityRequestParams.getEventOccurredBefore().toDate()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOrgUnitMode(CAPTURE);
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, Fields.ALL, user);

    assertEquals(CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeToDefaultGivenNoOrgUnitModeParamIsSet() throws BadRequestException {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, Fields.ALL, user);

    assertEquals(ACCESSIBLE, params.getOrgUnitMode());
  }

  @Test
  void shouldFailIfDeprecatedAndNewEnrollmentStatusParameterIsSet() {
    trackedEntityRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);
    trackedEntityRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, Fields.ALL, user));

    assertStartsWith(
        "Only one parameter of 'programStatus' and 'enrollmentStatus'", exception.getMessage());
  }

  @Test
  void testMappingProgramEnrollmentStartDate() throws BadRequestException {
    StartDateTime startDate = StartDateTime.of("2022-12-13");
    trackedEntityRequestParams.setEnrollmentEnrolledAfter(startDate);
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertEquals(startDate.toDate(), params.getProgramEnrollmentStartDate());
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertEquals(PROGRAM_UID, params.getProgram());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException {
    trackedEntityRequestParams.setProgram(PROGRAM_UID);
    trackedEntityRequestParams.setProgramStage(PROGRAM_STAGE_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertEquals(PROGRAM_STAGE_UID, params.getProgramStage());
  }

  @Test
  void testMappingTrackedEntityType() throws BadRequestException {
    trackedEntityRequestParams.setTrackedEntityType(TRACKED_ENTITY_TYPE_UID);
    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertEquals(TRACKED_ENTITY_TYPE_UID, params.getTrackedEntityType());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException {
    trackedEntityRequestParams.setAssignedUsers(UID.of("IsdLBTOBzMi", "l5ab8q5skbB"));
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertContainsOnly(
        UID.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldFailIfProgramStatusIsSetWithoutProgram() {
    trackedEntityRequestParams.setTrackedEntityType(TRACKED_ENTITY_TYPE_UID);
    trackedEntityRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, Fields.ALL, user));

    assertStartsWith("`program` must be defined when `programStatus`", exception.getMessage());
  }

  @Test
  void shouldFailIfEnrollmentStatusIsSetWithoutProgram() {
    trackedEntityRequestParams.setTrackedEntityType(TRACKED_ENTITY_TYPE_UID);
    trackedEntityRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, Fields.ALL, user));

    assertStartsWith("`program` must be defined when `enrollmentStatus`", exception.getMessage());
  }

  @Test
  void shouldFailIfGivenStatusAndNotOccurredEventDates() {
    trackedEntityRequestParams.setEventStatus(EventStatus.ACTIVE);

    assertThrows(
        BadRequestException.class,
        () -> TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenStatusAndOccurredAfterEventDateButNoOccurredBeforeEventDate() {
    trackedEntityRequestParams.setEventStatus(EventStatus.ACTIVE);
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2020-10-10"));

    assertThrows(
        BadRequestException.class,
        () -> TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenOccurredEventDatesAndNotEventStatus() {
    trackedEntityRequestParams.setEventOccurredBefore(EndDateTime.of("2020-11-11"));
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2020-10-10"));

    assertThrows(
        BadRequestException.class,
        () -> TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    trackedEntityRequestParams.setOrder(
        OrderCriteria.fromOrderString("createdAt:asc,zGlzbfreTOH,enrolledAt:desc"));
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(UID.of("zGlzbfreTOH"), SortDirection.ASC),
            new Order("enrollment.enrollmentDate", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    TrackedEntityOperationParams params =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    trackedEntityRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void shouldMapFilterParameter() throws BadRequestException {
    trackedEntityRequestParams.setOrgUnitMode(ACCESSIBLE);
    trackedEntityRequestParams.setFilter(TEA_1_UID + ":like:value1," + TEA_2_UID + ":like:value2");
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    Map<UID, List<QueryFilter>> filters =
        TrackedEntityRequestParamsMapper.map(trackedEntityRequestParams, user).getFilters();

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(new QueryFilter(LIKE, "value1")),
            TEA_2_UID,
            List.of(new QueryFilter(LIKE, "value2"))),
        filters);
  }

  @Test
  void shouldMapFiltersWithIVariantOperatorsToTrackerOperators() throws BadRequestException {
    trackedEntityRequestParams.setFilter(TEA_1_UID + ":ilike:value1," + TEA_2_UID + ":ieq:value2");
    trackedEntityRequestParams.setProgram(PROGRAM_UID);

    Map<UID, List<QueryFilter>> filters = mapper.map(trackedEntityRequestParams, user).getFilters();

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(new QueryFilter(LIKE, "value1")),
            TEA_2_UID,
            List.of(new QueryFilter(EQ, "value2"))),
        filters);
  }
}
