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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrderCriteria;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SingleEventRequestParamsMapperTest {

  private static final UID DE_1_UID = UID.of("OBzmpRP6YUh");

  private static final UID DE_2_UID = UID.of("KSd4PejqBf9");

  private static final UID PROGRAM_UID = UID.of("PlZSBEN7iZd");

  private static final UID ORGUNIT_UID = UID.of("DiszpKrYNg8");

  private TrackerIdSchemeParams idSchemeParams;

  @BeforeEach
  void setUp() {
    idSchemeParams = TrackerIdSchemeParams.builder().build();
  }

  @Test
  void shouldFailMappingParamsWithoutMandatoryProgram() {
    EventRequestParams eventRequestParams = new EventRequestParams();

    assertThrows(
        BadRequestException.class,
        () -> SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams));
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(PROGRAM_UID, params.getProgram());
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrgUnit(ORGUNIT_UID);
    eventRequestParams.setOrgUnitMode(SELECTED);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(SELECTED, params.getOrgUnitMode());
  }

  @Test
  void shouldReturnOrgUnitWhenCorrectOrgUnitMapped() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrgUnit(ORGUNIT_UID);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(ORGUNIT_UID, params.getOrgUnit());
  }

  @Test
  void testMappingOccurredAfterBefore() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    StartDateTime occurredAfter = StartDateTime.of("2020-01-01");
    eventRequestParams.setOccurredAfter(occurredAfter);
    EndDateTime occurredBefore = EndDateTime.of("2020-09-12");
    eventRequestParams.setOccurredBefore(occurredBefore);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(occurredAfter.toDate(), params.getOccurredAfter());
    assertEquals(occurredBefore.toDate(), params.getOccurredBefore());
  }

  @Test
  void shouldMapAfterAndBeforeDatesWhenSupplied() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    StartDateTime updatedAfter = StartDateTime.of("2022-01-01");
    eventRequestParams.setUpdatedAfter(updatedAfter);
    EndDateTime updatedBefore = EndDateTime.of("2022-09-12");
    eventRequestParams.setUpdatedBefore(updatedBefore);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(updatedAfter.toDate(), params.getUpdatedAfter());
    assertEquals(updatedBefore.toDate(), params.getUpdatedBefore());
  }

  @Test
  void shouldMapUpdatedWithinDateWhenSupplied() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    String updatedWithin = "6m";
    eventRequestParams.setUpdatedWithin(updatedWithin);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(updatedWithin, params.getUpdatedWithin());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenTryingToMapAllUpdateDatesTogether() {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    StartDateTime updatedAfter = StartDateTime.of("2022-01-01");
    eventRequestParams.setUpdatedAfter(updatedAfter);
    EndDateTime updatedBefore = EndDateTime.of("2022-09-12");
    eventRequestParams.setUpdatedBefore(updatedBefore);
    String updatedWithin = "P6M";
    eventRequestParams.setUpdatedWithin(updatedWithin);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams));

    assertEquals(
        "Last updated from and/or to and last updated duration cannot be specified simultaneously",
        exception.getMessage());
  }

  @Test
  void testMappingEvents() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setEvents(UID.of("XKrcfuM4Hcw", "M4pNmLabtXl"));

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(UID.of("XKrcfuM4Hcw", "M4pNmLabtXl"), params.getEvents());
  }

  @Test
  void testMappingEventIsNull() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertIsEmpty(params.getEvents());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setAssignedUsers(UID.of("IsdLBTOBzMi", "l5ab8q5skbB"));
    eventRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertContainsOnly(UID.of("IsdLBTOBzMi", "l5ab8q5skbB"), params.getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserMode());
  }

  @Test
  void testMutualExclusionOfEventsAndFilter() {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setFilter(DE_1_UID + ":ge:1:le:2");
    eventRequestParams.setEvents(UID.of("XKrcfuM4Hcw", "M4pNmLabtXl"));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams));
    assertEquals(
        "Event UIDs and filters can not be specified at the same time", exception.getMessage());
  }

  @Test
  void shouldMapDataElementFilters() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setFilter(DE_1_UID + ":eq:2," + DE_2_UID + ":like:foo");

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();
    assertNotNull(dataElementFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(
            DE_1_UID,
            List.of(new QueryFilter(QueryOperator.EQ, "2")),
            DE_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo")));
    assertEquals(expected, dataElementFilters);
  }

  @Test
  void shouldMapDataElementFiltersWhenDataElementHasMultipleFilters() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setFilter(DE_1_UID + ":gt:10:lt:20");

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();
    assertNotNull(dataElementFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(
            DE_1_UID,
            List.of(
                new QueryFilter(QueryOperator.GT, "10"), new QueryFilter(QueryOperator.LT, "20")));
    assertEquals(expected, dataElementFilters);
  }

  @Test
  void shouldMapDataElementFiltersWhenQueryFilterHasUIDOnly() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setFilter(DE_1_UID.getValue());

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();
    assertNotNull(dataElementFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(DE_1_UID, List.of(new QueryFilter(QueryOperator.NNULL)));
    assertEquals(expected, dataElementFilters);
  }

  @Test
  void shouldMapDataElementFiltersToDefaultIfNoneSet() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();

    assertNotNull(dataElementFilters);
    assertTrue(dataElementFilters.isEmpty());
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrder(
        OrderCriteria.fromOrderString(
            "createdAt:asc,zGlzbfreTOH,programStage:desc,scheduledAt:asc"));

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(UID.of("zGlzbfreTOH"), SortDirection.ASC),
            new Order("programStage.uid", SortDirection.DESC),
            new Order("scheduledDate", SortDirection.ASC)),
        params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void shouldMapSelectedOrgUnitModeWhenOrgUnitModeNotProvided() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrgUnit(ORGUNIT_UID);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(SELECTED, params.getOrgUnitMode());
  }

  @Test
  void shouldMapAccessibleOrgUnitModeWhenOrgUnitModeNorOrgUnitProvided()
      throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    SingleEventOperationParams params =
        SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams);

    assertEquals(ACCESSIBLE, params.getOrgUnitMode());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeCannotHaveOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrgUnit(ORGUNIT_UID);
    eventRequestParams.setOrgUnitMode(orgUnitMode);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams));

    assertStartsWith(
        "orgUnitMode " + orgUnitMode + " cannot be used with orgUnits.", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeNeedsOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);
    eventRequestParams.setOrgUnitMode(orgUnitMode);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> SingleEventRequestParamsMapper.map(eventRequestParams, idSchemeParams));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: " + orgUnitMode,
        exception.getMessage());
  }
}
