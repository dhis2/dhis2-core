/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventQueryParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class OrderAndPaginationExporterTest extends TrackerTest {

  @Autowired private EventService eventService;

  @Autowired private TrackedEntityInstanceService trackedEntityService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private TrackedEntityType trackedEntityType;

  private User importUser;

  private Program program;

  final Function<EventQueryParams, List<String>> eventsFunction =
      (params) ->
          eventService.getEvents(params).getEvents().stream()
              .map(Event::getEvent)
              .collect(Collectors.toList());

  private ProgramStageInstance pTzf9KYMk72;

  private ProgramStageInstance D9PbzJY8bJM;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");
    importUser = userService.getUser("M5zQapPyTZI");
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/event_and_enrollment.json", importUser.getUid())));
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");

    pTzf9KYMk72 = get(ProgramStageInstance.class, "pTzf9KYMk72");
    D9PbzJY8bJM = get(ProgramStageInstance.class, "D9PbzJY8bJM");
    trackedEntityType = get(TrackedEntityType.class, "ja8NY4PW7Xm");
    program = get(Program.class, "BFcipDERJnf");

    // to test that events are only returned if the user has read access to ALL COs of an events COC
    CategoryOption categoryOption = get(CategoryOption.class, "yMj2MnmNI8L");
    categoryOption.getSharing().setOwner("o1HMTIzBGo7");
    manager.update(categoryOption);

    manager.flush();
  }

  @BeforeEach
  void setUp() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by admin
    injectAdminUser();
  }

  @Test
  void shouldOrderEventsByStatusAndByDefaultOrder() {
    List<String> expected =
        Stream.of(
                get(ProgramStageInstance.class, "ck7DzdxqLqA"),
                get(ProgramStageInstance.class, "kWjSezkXHVp"),
                get(ProgramStageInstance.class, "OTmjvJDn0Fu"))
            .sorted(Comparator.comparing(ProgramStageInstance::getId).reversed()) // reversed = desc
            .map(ProgramStageInstance::getUid)
            .collect(Collectors.toList());

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, "DiszpKrYNg8"));
    params.setEvents(Set.of("ck7DzdxqLqA", "kWjSezkXHVp", "OTmjvJDn0Fu"));
    params.addOrders(List.of(new OrderParam("status", SortDirection.DESC)));

    List<String> actual = getEvents(params);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnPaginatedEventsWithNotesGivenNonDefaultPageSize() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(orgUnit);
    params.setEvents(Set.of("pTzf9KYMk72", "D9PbzJY8bJM"));
    params.addOrders(List.of(new OrderParam("occurredAt", SortDirection.DESC)));

    params.setPage(1);
    params.setPageSize(1);

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage),
        () -> assertEquals(List.of("D9PbzJY8bJM"), eventUids(firstPage)));

    params.setPage(2);

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage),
        () -> assertEquals(List.of("pTzf9KYMk72"), eventUids(secondPage)));

    params.setPage(2);
    params.setPageSize(3);

    assertIsEmpty(getEvents(params));
  }

  @Test
  void shouldReturnPaginatedPublicEventsWithMultipleCategoryOptionsGivenNonDefaultPageSize() {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.setProgram(program);

    params.addOrders(List.of(new OrderParam("occurredAt", SortDirection.DESC)));
    params.setPage(1);
    params.setPageSize(3);

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 3, false, firstPage),
        () ->
            assertEquals(
                List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp"), eventUids(firstPage)));

    params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.setProgram(program);

    params.addOrders(List.of(new OrderParam("occurredAt", SortDirection.DESC)));
    params.setPage(2);
    params.setPageSize(3);

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 3, true, secondPage),
        () ->
            assertEquals(
                List.of("lumVtWwwy0O", "QRYjLTiJTrA", "cadc5eGj0j7"), eventUids(secondPage)));

    params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.setProgram(program);

    params.addOrders(List.of(new OrderParam("occurredAt", SortDirection.DESC)));
    params.setPage(3);
    params.setPageSize(3);

    assertIsEmpty(eventsFunction.apply(params));
  }

  @Test
  void
      shouldReturnPaginatedEventsWithMultipleCategoryOptionsGivenNonDefaultPageSizeAndTotalPages() {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.setProgram(program);

    params.addOrders(List.of(new OrderParam("occurredAt", SortDirection.DESC)));
    params.setPage(1);
    params.setPageSize(2);
    params.setTotalPages(true);

    Events events = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertPager(1, 2, 6, events),
        () -> assertEquals(List.of("ck7DzdxqLqA", "OTmjvJDn0Fu"), eventUids(events)));
  }

  @Test
  void testOrderEventsOnAttributeAsc() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addFilterAttributes(queryItem(tea));
    params.addAttributeOrders(List.of(new OrderParam(tea.getUid(), SortDirection.ASC)));
    params.addOrders(params.getAttributeOrders());

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void testOrderEventsOnAttributeDesc() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addFilterAttributes(queryItem(tea));
    params.addAttributeOrders(List.of(new OrderParam(tea.getUid(), SortDirection.DESC)));
    params.addOrders(params.getAttributeOrders());

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testOrderEventsOnMultipleAttributesDesc() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "toDelete000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addFilterAttributes(List.of(queryItem(tea), queryItem(tea1)));
    params.addAttributeOrders(
        List.of(
            new OrderParam(tea1.getUid(), SortDirection.DESC),
            new OrderParam(tea.getUid(), SortDirection.DESC)));
    params.addOrders(params.getAttributeOrders());

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testOrderEventsOnMultipleAttributesAsc() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "toDelete000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addFilterAttributes(List.of(queryItem(tea), queryItem(tea1)));
    params.addAttributeOrders(
        List.of(
            new OrderParam(tea1.getUid(), SortDirection.DESC),
            new OrderParam(tea.getUid(), SortDirection.ASC)));
    params.addOrders(params.getAttributeOrders());

    Events events = eventService.getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), eventUids(events));
    List<String> trackedEntities =
        events.getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesAndPaginateWhenGivenNonDefaultPageSize() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "toDelete000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(orgUnit);
    params.setOrgUnitSelectionMode(SELECTED);
    params.addFilterAttributes(List.of(queryItem(tea), queryItem(tea1)));
    params.addAttributeOrders(
        List.of(
            new OrderParam(tea1.getUid(), SortDirection.DESC),
            new OrderParam(tea.getUid(), SortDirection.ASC)));
    params.addOrders(params.getAttributeOrders());
    params.setEvents(Set.of("D9PbzJY8bJM", "pTzf9KYMk72"));

    params.setPage(1);
    params.setPageSize(1);

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage),
        () -> assertEquals(List.of("D9PbzJY8bJM"), eventUids(firstPage)));

    params.setPage(2);
    params.setPageSize(1);

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage),
        () -> assertEquals(List.of("pTzf9KYMk72"), eventUids(secondPage)));

    params.setPage(3);
    params.setPageSize(3);

    assertIsEmpty(getEvents(params));
  }

  @Test
  void testOrderByEnrolledAtDesc() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("enrolledAt", SortDirection.DESC)));

    List<String> enrollments =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getEnrollment)
            .collect(Collectors.toList());

    assertEquals(List.of("TvctPPhpD8z", "nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testOrderByEnrolledAtAsc() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("enrolledAt", SortDirection.ASC)));

    List<String> enrollments =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getEnrollment)
            .collect(Collectors.toList());

    assertEquals(List.of("nxP7UnKhomJ", "TvctPPhpD8z"), enrollments);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenAttributeOrderSuppliedBeforeOrderParam() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addFilterAttributes(List.of(queryItem(tea)));
    params.addAttributeOrders(List.of(new OrderParam("toUpdate000", SortDirection.ASC)));
    params.addOrders(
        List.of(
            new OrderParam(tea.getUid(), SortDirection.ASC),
            new OrderParam("enrolledAt", SortDirection.ASC)));

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeAttributeOrder() {
    TrackedEntityAttribute tea = get(TrackedEntityAttribute.class, "toUpdate000");

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addFilterAttributes(List.of(queryItem(tea)));
    params.addAttributeOrders(List.of(new OrderParam(tea.getUid(), SortDirection.DESC)));
    params.addOrders(
        List.of(
            new OrderParam("enrolledAt", SortDirection.DESC),
            new OrderParam(tea.getUid(), SortDirection.DESC)));

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenDataElementSuppliedBeforeOrderParam() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addDataElements(List.of(queryItem("DATAEL00006")));
    params.addGridOrders(List.of(new OrderParam("DATAEL00006", SortDirection.DESC)));

    params.addOrders(
        List.of(
            new OrderParam("dueDate", SortDirection.DESC),
            new OrderParam("DATAEL00006", SortDirection.DESC),
            new OrderParam("enrolledAt", SortDirection.DESC)));

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeDataElement() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addDataElements(List.of(queryItem("DATAEL00006")));
    params.addGridOrders(List.of(new OrderParam("DATAEL00006", SortDirection.DESC)));

    params.addOrders(
        List.of(
            new OrderParam("enrolledAt", SortDirection.DESC),
            new OrderParam("DATAEL00006", SortDirection.DESC)));

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(Event::getTrackedEntityInstance)
            .collect(Collectors.toList());

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  private static Stream<Arguments> orderByFieldInDescendingOrderWhenModeSelected() {
    return Stream.of(
        Arguments.of("enrollment", "pTzf9KYMk72", "D9PbzJY8bJM"),
        Arguments.of("occurredAt", "D9PbzJY8bJM", "pTzf9KYMk72"),
        Arguments.of("enrollmentStatus", "D9PbzJY8bJM", "pTzf9KYMk72"),
        Arguments.of("event", "pTzf9KYMk72", "D9PbzJY8bJM"));
  }

  private static Stream<Arguments> orderByFieldInAscendingOrderWhenModeSelected() {
    return Stream.of(
        Arguments.of("enrollment", "D9PbzJY8bJM", "pTzf9KYMk72"),
        Arguments.of("occurredAt", "pTzf9KYMk72", "D9PbzJY8bJM"),
        Arguments.of("enrollmentStatus", "pTzf9KYMk72", "D9PbzJY8bJM"),
        Arguments.of("event", "D9PbzJY8bJM", "pTzf9KYMk72"));
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInDescendingOrderWhenModeSelected")
  void shouldOrderByFieldInDescendingOrderWhenModeSelected(
      String field, String firstEvent, String secondEvent) {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam(field, SortDirection.DESC)));

    Events events = eventService.getEvents(params);

    assertEquals(List.of(firstEvent, secondEvent), eventUids(events));
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInAscendingOrderWhenModeSelected")
  void shouldOrderByFieldInAscendingOrderWhenModeSelected(
      String field, String firstEvent, String secondEvent) {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam(field, SortDirection.ASC)));

    Events events = eventService.getEvents(params);

    assertEquals(List.of(firstEvent, secondEvent), eventUids(events));
  }

  private static Stream<Arguments> orderByFieldInDescendingOrderWhenModeDescendants() {
    return Stream.of(
        Arguments.of("orgUnit", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("program", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("programStage", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("dueDate", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("status", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("storedBy", "SbUJzkxKYAG", "gvULMgNiAfM"));
  }

  private static Stream<Arguments> orderByFieldInAscendingOrderWhenModeDescendants() {
    return Stream.of(
        Arguments.of("orgUnit", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("program", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("programStage", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("dueDate", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("status", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("storedBy", "gvULMgNiAfM", "SbUJzkxKYAG"));
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInDescendingOrderWhenModeDescendants")
  void shouldOrderByFieldInDescendingOrderWhenModeDescendants(
      String field, String firstEvent, String secondEvent) {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(DESCENDANTS);
    params.setOrgUnit(get(OrganisationUnit.class, "RojfDTBhoGC"));
    params.addOrders(List.of(new OrderParam(field, SortDirection.DESC)));
    Events events = eventService.getEvents(params);

    assertEquals(List.of(firstEvent, secondEvent), eventUids(events));
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInAscendingOrderWhenModeDescendants")
  void shouldOrderByFieldInAscendingOrderWhenModeDescendants(
      String field, String firstEvent, String secondEvent) {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(DESCENDANTS);
    params.setOrgUnit(get(OrganisationUnit.class, "RojfDTBhoGC"));
    params.addOrders(List.of(new OrderParam(field, SortDirection.ASC)));

    Events events = eventService.getEvents(params);

    assertEquals(List.of(firstEvent, secondEvent), eventUids(events));
  }

  @Test
  void shouldOrderEventsByCompletedDateInDescendingOrderWhenCompletedDateDescSupplied() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("completedDate", SortDirection.DESC)));

    Events events = eventService.getEvents(params);

    boolean isSameCompletedDate =
        pTzf9KYMk72.getCompletedDate().equals(D9PbzJY8bJM.getCompletedDate());
    if (isSameCompletedDate) {
      // the order is non-deterministic if the completed date is the same. we can then only assert
      // the correct events are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), eventUids(events));
    } else {
      List<String> expected =
          reverseSortEventsByDate(pTzf9KYMk72, D9PbzJY8bJM, ProgramStageInstance::getCompletedDate);
      assertEquals(expected, eventUids(events));
    }
  }

  @Test
  void shouldOrderEventsByCompletedDateInAscendingOrderWhenCompletedDateAscSupplied() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("completedDate", SortDirection.ASC)));

    Events events = eventService.getEvents(params);

    boolean isSameCompletedDate =
        pTzf9KYMk72.getCompletedDate().equals(D9PbzJY8bJM.getCompletedDate());
    if (isSameCompletedDate) {
      // the order is non-deterministic if the completed date is the same. we can then only assert
      // the correct events are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), eventUids(events));
    } else {
      List<String> expected =
          sortEventsByDate(pTzf9KYMk72, D9PbzJY8bJM, ProgramStageInstance::getCompletedDate);
      assertEquals(expected, eventUids(events));
    }
  }

  @Test
  void shouldOrderEventsByCreatedInAscendingOrderWhenCreatedAscSupplied() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("created", SortDirection.ASC)));

    Events events = eventService.getEvents(params);

    boolean isSameCompletedDate = pTzf9KYMk72.getCreated().equals(D9PbzJY8bJM.getCreated());
    if (isSameCompletedDate) {
      // the order is non-deterministic if the completed date is the same. we can then only assert
      // the correct events are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), eventUids(events));
    } else {
      List<String> expected =
          sortEventsByDate(pTzf9KYMk72, D9PbzJY8bJM, ProgramStageInstance::getCreated);
      assertEquals(expected, eventUids(events));
    }
  }

  @Test
  void shouldOrderEventsByCreatedInDescendingOrderWhenCreatedDescSupplied() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("created", SortDirection.DESC)));

    Events events = eventService.getEvents(params);

    boolean isSameCompletedDate = pTzf9KYMk72.getCreated().equals(D9PbzJY8bJM.getCreated());
    if (isSameCompletedDate) {
      // the order is non-deterministic if the completed date is the same. we can then only assert
      // the correct events are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), eventUids(events));
    } else {
      List<String> expected =
          reverseSortEventsByDate(pTzf9KYMk72, D9PbzJY8bJM, ProgramStageInstance::getCreated);
      assertEquals(expected, eventUids(events));
    }
  }

  @Test
  void shouldOrderEventsByLastUpdatedInDescendingOrderWhenLastUpdatedDescSupplied() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("lastUpdated", SortDirection.DESC)));

    Events events = eventService.getEvents(params);

    boolean isSameCompletedDate = pTzf9KYMk72.getLastUpdated().equals(D9PbzJY8bJM.getLastUpdated());
    if (isSameCompletedDate) {
      // the order is non-deterministic if the completed date is the same. we can then only assert
      // the correct events are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), eventUids(events));
    } else {
      List<String> expected =
          reverseSortEventsByDate(pTzf9KYMk72, D9PbzJY8bJM, ProgramStageInstance::getLastUpdated);
      assertEquals(expected, eventUids(events));
    }
  }

  @Test
  void shouldOrderEventsByLastUpdatedInAscendingOrderWhenLastUpdatedAscSupplied() {
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(SELECTED);
    params.setOrgUnit(orgUnit);
    params.addOrders(List.of(new OrderParam("lastUpdated", SortDirection.ASC)));

    Events events = eventService.getEvents(params);

    boolean isSameCompletedDate = pTzf9KYMk72.getCreated().equals(D9PbzJY8bJM.getLastUpdated());
    if (isSameCompletedDate) {
      // the order is non-deterministic if the completed date is the same. we can then only assert
      // the correct events are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), eventUids(events));
    } else {
      List<String> expected =
          sortEventsByDate(pTzf9KYMk72, D9PbzJY8bJM, ProgramStageInstance::getLastUpdated);
      assertEquals(expected, eventUids(events));
    }
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtAsc() {
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

    params.addOrganisationUnits(Set.of(orgUnit));
    params.setOrganisationUnitMode(SELECTED);
    params.setTrackedEntityInstanceUids(Set.of("QS6w44flWAf", "dUE514NMOlo"));
    params.setTrackedEntityType(trackedEntityType);
    params.setUser(importUser);
    params.setOrders(List.of(new OrderParam("enrolledAt", SortDirection.ASC)));

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtDescWithNoProgramInParams() {
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

    params.addOrganisationUnits(Set.of(orgUnit));
    params.setOrganisationUnitMode(SELECTED);
    params.setTrackedEntityInstanceUids(Set.of("QS6w44flWAf", "dUE514NMOlo"));
    params.setTrackedEntityType(trackedEntityType);
    params.setUser(importUser);
    params.setOrders(List.of(new OrderParam("enrolledAt", SortDirection.DESC)));

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(
        List.of("QS6w44flWAf", "dUE514NMOlo"),
        trackedEntities); // QS6w44flWAf has 2 enrollments, one of which has an enrollment with
    // enrolled date greater than the enrollment in dUE514NMOlo
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtDescWithProgramInParams() {

    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

    params.setProgram(program);
    params.addOrganisationUnits(Set.of(orgUnit));
    params.setOrganisationUnitMode(SELECTED);
    params.setTrackedEntityInstanceUids(Set.of("QS6w44flWAf", "dUE514NMOlo"));
    params.setUser(importUser);
    params.setOrders(List.of(new OrderParam("enrolledAt", SortDirection.DESC)));

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByInactiveDesc() {

    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

    params.addOrganisationUnits(Set.of(orgUnit));
    params.setOrganisationUnitMode(SELECTED);
    params.setTrackedEntityInstanceUids(Set.of("QS6w44flWAf", "dUE514NMOlo"));
    params.setTrackedEntityType(trackedEntityType);
    params.setUser(importUser);
    params.setOrders(List.of(new OrderParam("inactive", SortDirection.DESC)));

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByInactiveAsc() {

    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

    params.addOrganisationUnits(Set.of(orgUnit));
    params.setOrganisationUnitMode(SELECTED);
    params.setTrackedEntityInstanceUids(Set.of("QS6w44flWAf", "dUE514NMOlo"));
    params.setTrackedEntityType(trackedEntityType);
    params.setUser(importUser);
    params.setOrders(List.of(new OrderParam("inactive", SortDirection.ASC)));

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  private List<String> getTrackedEntities(TrackedEntityInstanceQueryParams params) {
    return trackedEntityService
        .getTrackedEntityInstances(params, TrackedEntityInstanceParams.FALSE, false, false)
        .stream()
        .map(TrackedEntityInstance::getTrackedEntityInstance)
        .collect(Collectors.toList());
  }

  private static QueryItem queryItem(TrackedEntityAttribute tea) {
    return new QueryItem(
        tea,
        null,
        tea.getValueType(),
        tea.getAggregationType(),
        tea.getOptionSet(),
        tea.isUnique());
  }

  private static QueryItem queryItem(String teaUid) {
    return queryItem(teaUid, ValueType.TEXT);
  }

  private static QueryItem queryItem(String teaUid, ValueType valueType) {
    TrackedEntityAttribute at = new TrackedEntityAttribute();
    at.setUid(teaUid);
    at.setValueType(valueType);
    at.setAggregationType(AggregationType.NONE);
    return new QueryItem(
        at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(), at.isUnique());
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(t, () -> String.format("metadata with uid '%s' should have been created", uid));
    return t;
  }

  private static void assertSlimPager(int pageNumber, int pageSize, boolean isLast, Events events) {
    assertInstanceOf(
        SlimPager.class, events.getPager(), "SlimPager should be returned if totalPages=false");
    SlimPager pager = (SlimPager) events.getPager();
    assertAll(
        "pagination details",
        () -> assertEquals(pageNumber, pager.getPage(), "number of current page"),
        () -> assertEquals(pageSize, pager.getPageSize(), "page size"),
        () ->
            assertEquals(
                isLast,
                pager.isLastPage(),
                isLast ? "should be the last page" : "should NOT be the last page"));
  }

  private static void assertPager(int pageNumber, int pageSize, int totalCount, Events events) {
    Pager pager = events.getPager();
    assertAll(
        "pagination details",
        () -> assertEquals(pageNumber, pager.getPage(), "number of current page"),
        () -> assertEquals(pageSize, pager.getPageSize(), "page size"),
        () -> assertEquals(totalCount, pager.getTotal(), "total page count"));
  }

  private List<String> getEvents(EventQueryParams params) {
    return eventUids(eventService.getEvents(params));
  }

  private static List<String> eventUids(Events events) {
    return events.getEvents().stream().map(Event::getEvent).collect(Collectors.toList());
  }

  private static List<String> reverseSortEventsByDate(
      ProgramStageInstance pTzf9KYMk72,
      ProgramStageInstance D9PbzJY8bJM,
      Function<ProgramStageInstance, Date> eventDate) {

    return Stream.of(pTzf9KYMk72, D9PbzJY8bJM)
        .sorted(Comparator.comparing(eventDate).reversed()) // reversed = desc
        .map(ProgramStageInstance::getUid)
        .collect(Collectors.toList());
  }

  private static List<String> sortEventsByDate(
      ProgramStageInstance pTzf9KYMk72,
      ProgramStageInstance D9PbzJY8bJM,
      Function<ProgramStageInstance, Date> eventDate) {

    return Stream.of(pTzf9KYMk72, D9PbzJY8bJM)
        .sorted(Comparator.comparing(eventDate))
        .map(ProgramStageInstance::getUid)
        .collect(Collectors.toList());
  }
}
