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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
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
import java.util.Set;
import org.hisp.dhis.common.AssignedUserSelectionMode;
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
class TrackedEntityRequestParamsMapperTest {
  private static final String PROGRAM_UID = "XhBYIraw7sv";

  private static final String PROGRAM_STAGE_UID = "RpCr2u2pFqw";

  private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

  private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

  @Mock private TrackedEntityFieldsParamMapper fieldsParamMapper;

  @InjectMocks private TrackedEntityRequestParamsMapper mapper;

  private User user;

  private RequestParams requestParams;

  @BeforeEach
  public void setUp() {
    user = new User();
    requestParams = new RequestParams();
    requestParams.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
  }

  @Test
  void testMapping() throws BadRequestException {
    requestParams.setQuery("query-test");
    requestParams.setOuMode(DESCENDANTS);
    requestParams.setProgramStatus(ProgramStatus.ACTIVE);
    requestParams.setProgram(UID.of(PROGRAM_UID));
    requestParams.setProgramStage(UID.of(PROGRAM_STAGE_UID));
    requestParams.setFollowUp(true);
    requestParams.setUpdatedAfter(getDate(2019, 1, 1));
    requestParams.setUpdatedBefore(getDate(2020, 1, 1));
    requestParams.setUpdatedWithin("20");
    requestParams.setEnrollmentOccurredAfter(getDate(2019, 5, 5));
    requestParams.setEnrollmentOccurredBefore(getDate(2020, 5, 5));
    requestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));
    requestParams.setEventStatus(EventStatus.COMPLETED);
    requestParams.setEventOccurredAfter(getDate(2019, 7, 7));
    requestParams.setEventOccurredBefore(getDate(2020, 7, 7));
    requestParams.setSkipMeta(true);
    requestParams.setPage(1);
    requestParams.setPageSize(50);
    requestParams.setTotalPages(false);
    requestParams.setSkipPaging(false);
    requestParams.setIncludeDeleted(true);
    requestParams.setIncludeAllAttributes(true);

    final TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertThat(params.getQuery().getFilter(), is("query-test"));
    assertThat(params.getQuery().getOperator(), is(QueryOperator.EQ));
    assertThat(params.getProgramUid(), is(PROGRAM_UID));
    assertThat(params.getProgramStageUid(), is(PROGRAM_STAGE_UID));
    assertThat(params.getTrackedEntityTypeUid(), is(TRACKED_ENTITY_TYPE_UID));
    assertThat(params.getPageSize(), is(50));
    assertThat(params.getPage(), is(1));
    assertThat(params.isTotalPages(), is(false));
    assertThat(params.getProgramStatus(), is(ProgramStatus.ACTIVE));
    assertThat(params.getFollowUp(), is(true));
    assertThat(params.getLastUpdatedStartDate(), is(requestParams.getUpdatedAfter()));
    assertThat(params.getLastUpdatedEndDate(), is(requestParams.getUpdatedBefore()));
    assertThat(
        params.getProgramIncidentStartDate(), is(requestParams.getEnrollmentOccurredAfter()));
    assertThat(params.getProgramIncidentEndDate(), is(requestParams.getEnrollmentOccurredBefore()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(params.getEventStartDate(), is(requestParams.getEventOccurredAfter()));
    assertThat(params.getEventEndDate(), is(requestParams.getEventOccurredBefore()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
    assertThat(params.isIncludeAllAttributes(), is(true));
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setOrgUnitMode(SELECTED);

    TrackedEntityOperationParams params = mapper.map(requestParams, null);

    assertEquals(SELECTED, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeGivenOuModeParam() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setOuMode(SELECTED);

    TrackedEntityOperationParams params = mapper.map(requestParams, null);

    assertEquals(SELECTED, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeToDefaultGivenNoOrgUnitModeParamIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();

    TrackedEntityOperationParams params = mapper.map(requestParams, null);

    assertEquals(DESCENDANTS, params.getOrgUnitMode());
  }

  @Test
  void shouldThrowIfDeprecatedAndNewOrgUnitModeParameterIsSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setOuMode(SELECTED);
    requestParams.setOrgUnitMode(SELECTED);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams, null));

    assertStartsWith("Only one parameter of 'ouMode' and 'orgUnitMode'", exception.getMessage());
  }

  @Test
  void testMappingProgramEnrollmentStartDate() throws BadRequestException {
    Date date = parseDate("2022-12-13");
    requestParams.setEnrollmentEnrolledAfter(date);

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertEquals(date, params.getProgramEnrollmentStartDate());
  }

  @Test
  void shouldFailWithBadExceptionWhenBadFormattedQueryProvided() {
    String queryWithBadFormat = "wrong-query:";

    requestParams.setQuery(queryWithBadFormat);

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams, user));

    assertEquals("Query item or filter is invalid: " + queryWithBadFormat, e.getMessage());
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    requestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertEquals(PROGRAM_UID, params.getProgramUid());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException {
    requestParams.setProgram(UID.of(PROGRAM_UID));
    requestParams.setProgramStage(UID.of(PROGRAM_STAGE_UID));

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertEquals(PROGRAM_STAGE_UID, params.getProgramStageUid());
  }

  @Test
  void testMappingTrackedEntityType() throws BadRequestException {
    requestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertEquals(TRACKED_ENTITY_TYPE_UID, params.getTrackedEntityTypeUid());
  }

  @Test
  void testMappingAssignedUser() throws BadRequestException {
    requestParams.setAssignedUser("IsdLBTOBzMi;l5ab8q5skbB");
    requestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException {
    requestParams.setAssignedUsers(Set.of(UID.of("IsdLBTOBzMi"), UID.of("l5ab8q5skbB")));
    requestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldFailIfGivenOrgUnitAndOrgUnits() {
    requestParams.setOrgUnit("IsdLBTOBzMi");
    requestParams.setOrgUnits(Set.of(UID.of("IsdLBTOBzMi")));

    assertThrows(BadRequestException.class, () -> mapper.map(requestParams, user));
  }

  @Test
  void shouldFailIfGivenTrackedEntityAndTrackedEntities() {
    requestParams.setTrackedEntity("IsdLBTOBzMi");
    requestParams.setTrackedEntities(Set.of(UID.of("IsdLBTOBzMi")));

    assertThrows(BadRequestException.class, () -> mapper.map(requestParams, user));
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setOrder(
        OrderCriteria.fromOrderString("createdAt:asc,zGlzbfreTOH,enrolledAt:desc"));

    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(UID.of("zGlzbfreTOH"), SortDirection.ASC),
            new Order("enrollment.enrollmentDate", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    TrackedEntityOperationParams params = mapper.map(requestParams, user);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    requestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams, user));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }
}
