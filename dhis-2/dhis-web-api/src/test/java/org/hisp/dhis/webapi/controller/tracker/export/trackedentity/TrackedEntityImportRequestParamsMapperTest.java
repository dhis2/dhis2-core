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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntityImportRequestParamsMapperTest {
  public static final String TEA_1_UID = "TvjwTPToKHO";

  public static final String TEA_2_UID = "cy2oRh2sNr6";

  public static final String TEA_3_UID = "cy2oRh2sNr7";

  private static final String PROGRAM_UID = "XhBYIraw7sv";

  private static final String PROGRAM_STAGE_UID = "RpCr2u2pFqw";

  private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

  private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

  @Mock private TrackedEntityFieldsParamMapper fieldsParamMapper;

  @InjectMocks private TrackedEntityRequestParamsMapper mapper;

  private User user;

  private TrackedEntityRequestParams trackedEntityRequestParams;

  @BeforeEach
  public void setUp() {
    user = new User();
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
  }

  @Test
  void testMapping() throws BadRequestException {
    trackedEntityRequestParams.setOuMode(CAPTURE);
    trackedEntityRequestParams.setProgramStatus(ProgramStatus.ACTIVE);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));
    trackedEntityRequestParams.setProgramStage(UID.of(PROGRAM_STAGE_UID));
    trackedEntityRequestParams.setFollowUp(true);
    trackedEntityRequestParams.setUpdatedAfter(getDate(2019, 1, 1));
    trackedEntityRequestParams.setUpdatedBefore(getDate(2020, 1, 1));
    trackedEntityRequestParams.setUpdatedWithin("20");
    trackedEntityRequestParams.setEnrollmentOccurredAfter(getDate(2019, 5, 5));
    trackedEntityRequestParams.setEnrollmentOccurredBefore(getDate(2020, 5, 5));
    trackedEntityRequestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));
    trackedEntityRequestParams.setEventStatus(EventStatus.COMPLETED);
    trackedEntityRequestParams.setEventOccurredAfter(getDate(2019, 7, 7));
    trackedEntityRequestParams.setEventOccurredBefore(getDate(2020, 7, 7));
    trackedEntityRequestParams.setIncludeDeleted(true);

    final TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertThat(params.getProgramUid(), is(PROGRAM_UID));
    assertThat(params.getProgramStageUid(), is(PROGRAM_STAGE_UID));
    assertThat(params.getTrackedEntityTypeUid(), is(TRACKED_ENTITY_TYPE_UID));
    assertThat(params.getProgramStatus(), is(ProgramStatus.ACTIVE));
    assertThat(params.getFollowUp(), is(true));
    assertThat(params.getLastUpdatedStartDate(), is(trackedEntityRequestParams.getUpdatedAfter()));
    assertThat(params.getLastUpdatedEndDate(), is(trackedEntityRequestParams.getUpdatedBefore()));
    assertThat(
        params.getProgramIncidentStartDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredAfter()));
    assertThat(
        params.getProgramIncidentEndDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredBefore()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(params.getEventStartDate(), is(trackedEntityRequestParams.getEventOccurredAfter()));
    assertThat(params.getEventEndDate(), is(trackedEntityRequestParams.getEventOccurredBefore()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    TrackedEntityRequestParams trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOrgUnitMode(CAPTURE);

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, null);

    assertEquals(CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeGivenOuModeParam() throws BadRequestException {
    TrackedEntityRequestParams trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOuMode(CAPTURE);

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, null);

    assertEquals(CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeToDefaultGivenNoOrgUnitModeParamIsSet() throws BadRequestException {
    TrackedEntityRequestParams trackedEntityRequestParams = new TrackedEntityRequestParams();

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, null);

    assertEquals(ACCESSIBLE, params.getOrgUnitMode());
  }

  @Test
  void shouldThrowIfDeprecatedAndNewOrgUnitModeParameterIsSet() {
    TrackedEntityRequestParams trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOuMode(SELECTED);
    trackedEntityRequestParams.setOrgUnitMode(SELECTED);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, null));

    assertStartsWith("Only one parameter of 'ouMode' and 'orgUnitMode'", exception.getMessage());
  }

  @Test
  void testMappingProgramEnrollmentStartDate() throws BadRequestException {
    Date date = parseDate("2022-12-13");
    trackedEntityRequestParams.setEnrollmentEnrolledAfter(date);

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(date, params.getProgramEnrollmentStartDate());
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(PROGRAM_UID, params.getProgramUid());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException {
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));
    trackedEntityRequestParams.setProgramStage(UID.of(PROGRAM_STAGE_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(PROGRAM_STAGE_UID, params.getProgramStageUid());
  }

  @Test
  void testMappingTrackedEntityType() throws BadRequestException {
    trackedEntityRequestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(TRACKED_ENTITY_TYPE_UID, params.getTrackedEntityTypeUid());
  }

  @Test
  void testMappingAssignedUser() throws BadRequestException {
    trackedEntityRequestParams.setAssignedUser("IsdLBTOBzMi;l5ab8q5skbB");
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException {
    trackedEntityRequestParams.setAssignedUsers(
        Set.of(UID.of("IsdLBTOBzMi"), UID.of("l5ab8q5skbB")));
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldFailIfGivenOrgUnitAndOrgUnits() {
    trackedEntityRequestParams.setOrgUnit("IsdLBTOBzMi");
    trackedEntityRequestParams.setOrgUnits(Set.of(UID.of("IsdLBTOBzMi")));

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenTrackedEntityAndTrackedEntities() {
    trackedEntityRequestParams.setTrackedEntity("IsdLBTOBzMi");
    trackedEntityRequestParams.setTrackedEntities(Set.of(UID.of("IsdLBTOBzMi")));

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenRemovedQueryParameter() {
    trackedEntityRequestParams.setQuery("query");

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenRemovedAttributeParameter() {
    trackedEntityRequestParams.setAttribute("IsdLBTOBzMi");

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenRemovedIncludeAllAttributesParameter() {
    trackedEntityRequestParams.setIncludeAllAttributes("true");

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    TrackedEntityRequestParams trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOrder(
        OrderCriteria.fromOrderString("createdAt:asc,zGlzbfreTOH,enrolledAt:desc"));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(UID.of("zGlzbfreTOH"), SortDirection.ASC),
            new Order("enrollment.enrollmentDate", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    trackedEntityRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void shouldMapFilterParameter() throws BadRequestException {
    TrackedEntityRequestParams trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOrgUnitMode(ACCESSIBLE);
    trackedEntityRequestParams.setFilter(TEA_1_UID + ":like:value1," + TEA_2_UID + ":like:value2");

    Map<String, List<QueryFilter>> filters =
        mapper.map(trackedEntityRequestParams, user).getFilters();

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "value1")),
            TEA_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "value2"))),
        filters);
  }
}
