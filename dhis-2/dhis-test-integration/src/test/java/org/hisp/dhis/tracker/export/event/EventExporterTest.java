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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.Assertions.assertHasTimeStamp;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.Assertions.assertNotes;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Enrico Colasante
 */
class EventExporterTest extends TrackerTest {

  @Autowired private EventService eventService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DataElementService dataElementService;

  private OrganisationUnit orgUnit;

  private ProgramStage programStage;

  private Program program;

  private TrackedEntity trackedEntity;
  private User importUser;

  private EventOperationParams.EventOperationParamsBuilder operationParamsBuilder;

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    programStage = get(ProgramStage.class, "NpsdDv6kKSO");
    program = programStage.getProgram();
    trackedEntity = get(TrackedEntity.class, "dUE514NMOlo");

    // to test that events are only returned if the user has read access to ALL COs of an events COC
    CategoryOption categoryOption = get(CategoryOption.class, "yMj2MnmNI8L");
    categoryOption.getSharing().setOwner("o1HMTIzBGo7");
    manager.update(categoryOption);
    manager.flush();
  }

  @BeforeEach
  void setUpUserAndParams() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by the importUser
    injectSecurityContextUser(importUser);

    operationParamsBuilder = EventOperationParams.builder().eventParams(EventParams.FALSE);
    operationParamsBuilder.orgUnit(orgUnit).orgUnitMode(SELECTED);
  }

  @Test
  void shouldExportEventAndMapAssignedUserWhenAssignedUserIsNotNull()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .trackedEntity(trackedEntity)
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .build();

    List<Event> events = eventService.getEvents(params);

    assertEquals(
        get(Event.class, "D9PbzJY8bJM").getAssignedUser(), events.get(0).getAssignedUser());
  }

  @Test
  void shouldReturnEventsWithRelationships() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .events(Set.of(UID.of("pTzf9KYMk72")))
            .eventParams(EventParams.TRUE)
            .build();

    List<Event> events = eventService.getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), uids(events));
    List<Relationship> relationships =
        events.get(0).getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .toList();
    assertContainsOnly(List.of("oLT07jKRu9e", "yZxjxJli9mO"), uids(relationships));
  }

  @Test
  void shouldReturnEventsWithNotes() throws ForbiddenException, BadRequestException {
    Event pTzf9KYMk72 = get(Event.class, "pTzf9KYMk72");
    EventOperationParams params =
        operationParamsBuilder.events(Set.of(UID.of("pTzf9KYMk72"))).build();

    List<Event> events = eventService.getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), uids(events));
    assertNotes(pTzf9KYMk72.getNotes(), events.get(0).getNotes());
  }

  @Test
  void testExportEvents() throws ForbiddenException, BadRequestException {
    EventOperationParams params = operationParamsBuilder.programStage(programStage).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByEnrollment() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .trackedEntity(trackedEntity)
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithExecutionAndUpdateDates()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .occurredAfter(getDate(2018, 1, 1))
            .occurredBefore(getDate(2020, 1, 29))
            .skipChangedBefore(getDate(2018, 1, 1))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithLastUpdateDuration() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .updatedWithin("1d")
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldReturnEventsIfItOccurredBetweenPassedDateAndTime()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .occurredBefore(Date.from(getDate(2020, 1, 28).toInstant().plus(1, ChronoUnit.HOURS)))
            .occurredAfter(Date.from(getDate(2020, 1, 28).toInstant().minus(1, ChronoUnit.HOURS)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldReturnEventsIfItOccurredAtTheSameDateAndTimeOfOccurredBeforePassed()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .occurredBefore(getDate(2020, 1, 28))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldReturnEventsIfItOccurredAtTheSameDateAndTimeOfOccurredAfterPassed()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .occurredAfter(getDate(2020, 1, 28))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithLastUpdateDates() throws ForbiddenException, BadRequestException {
    Date date = new Date();
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .updatedAfter(
                Date.from(
                    date.toInstant()
                        .minus(1, ChronoUnit.DAYS)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()))
            .updatedBefore(
                Date.from(
                    date.toInstant()
                        .plus(1, ChronoUnit.DAYS)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithDatesIncludingTimeStamp()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of(UID.of("pTzf9KYMk72")))
            .build();

    List<Event> events = eventService.getEvents(params);

    Event event = events.get(0);

    assertAll(
        "All dates should include timestamp",
        () ->
            assertEquals(
                "2019-01-25T12:10:38.100",
                DateUtils.toIso8601NoTz(event.getOccurredDate()),
                () ->
                    String.format(
                        "Expected %s to be in %s",
                        event.getOccurredDate(), "2019-01-25T12:10:38.100")),
        () ->
            assertEquals(
                "2019-01-28T12:32:38.100",
                DateUtils.toIso8601NoTz(event.getScheduledDate()),
                () ->
                    String.format(
                        "Expected %s to be in %s",
                        event.getScheduledDate(), "2019-01-28T12:32:38.100")),
        () -> assertHasTimeStamp(event.getCompletedDate()));
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "%val%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);

    new QueryItem(dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(), null, null);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithStatusFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .enrollmentStatus(EnrollmentStatus.ACTIVE)
            .dataElementFilters(
                Map.of(UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "%val%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithProgramTypeFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .programType(ProgramType.WITH_REGISTRATION)
            .dataElementFilters(
                Map.of(UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "%val%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsEqual()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(
                    UID.of(dataElement),
                    List.of(new QueryFilter(QueryOperator.LIKE, "%value00001%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(
                    UID.of(dataElement),
                    List.of(new QueryFilter(QueryOperator.IN, "value00001;value00002"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithCategoryOptionSuperUser()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .program(program)
            .attributeCategoryCombo(UID.of("bjDvmb4bfuf"))
            .attributeCategoryOptions(Set.of(UID.of("xYerKDKCefk")))
            .dataElementFilters(
                Map.of(
                    UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "value00001"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldReturnEventsGivenCategoryOptionCombo() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(UID.of("DiszpKrYNg8"))
            .orgUnitMode(SELECTED)
            .attributeCategoryCombo(UID.of("O4VaNks6tta"))
            .attributeCategoryOptions(UID.of("xwZ2u3WyQR0", "M58XdOfhiJ7"))
            .build();

    List<Event> events = eventService.getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "OTmjvJDn0Fu"), uids(events));
    List<Executable> executables =
        events.stream()
            .map(
                e ->
                    (Executable)
                        () ->
                            assertAll(
                                "category options and combo of event " + e.getUid(),
                                () ->
                                    assertEquals(
                                        "cr89ebDZrac", e.getAttributeOptionCombo().getUid()),
                                () ->
                                    assertContainsOnly(
                                        Set.of("xwZ2u3WyQR0", "M58XdOfhiJ7"),
                                        e.getAttributeOptionCombo().getCategoryOptions().stream()
                                            .map(CategoryOption::getUid)
                                            .collect(Collectors.toSet()))))
            .toList();
    assertAll("all events should have the same category option combo and options", executables);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithCategoryOptionNotSuperUser()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(
        createAndAddUser(false, "user", Set.of(orgUnit), Set.of(orgUnit), "F_EXPORT_DATA"));
    DataElement dataElement = dataElement(UID.of("DATAEL00002"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .program(program)
            .attributeCategoryCombo(UID.of("bjDvmb4bfuf"))
            .attributeCategoryOptions(Set.of(UID.of("xYerKDKCefk")))
            .dataElementFilters(
                Map.of(
                    UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "value00002"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithOptionSetEqual()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "option1"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithOptionSetIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(
                    UID.of(dataElement),
                    List.of(new QueryFilter(QueryOperator.IN, "option1;option2"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithOptionSetLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "%opt%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByNumericDataElements()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00006"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .dataElementFilters(
                Map.of(
                    UID.of(dataElement),
                    List.of(
                        new QueryFilter(QueryOperator.LT, "77"),
                        new QueryFilter(QueryOperator.GT, "8"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testEnrollmentEnrolledBeforeSetToBeforeFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void testEnrollmentEnrolledBeforeEqualToFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledBeforeSetToAfterFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledAfterSetToBeforeLastEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledAfter(parseDate("2021-03-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledAfterEqualToLastEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledAfter(parseDate("2021-03-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledAfterSetToAfterLastEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledAfter(parseDate("2021-03-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void testEnrollmentOccurredBeforeSetToBeforeFirstOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredBefore(parseDate("2021-02-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void testEnrollmentOccurredBeforeEqualToFirstOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredBefore(parseDate("2021-02-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentOccurredBeforeSetToAfterFirstOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredBefore(parseDate("2021-02-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentOccurredAfterSetToBeforeLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentOccurredAfterEqualToLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void
      shouldFilterOutEventsWithATrackedEntityWithoutThatAttributeWhenFilterAttributeHasNoQueryFilter()
          throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .attributeFilters(Map.of(UID.of("notUpdated0"), List.of()))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterNumericAttributes() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .attributeFilters(
                Map.of(
                    UID.of("numericAttr"),
                    List.of(
                        new QueryFilter(QueryOperator.LT, "77"),
                        new QueryFilter(QueryOperator.GT, "8"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributes() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .attributeFilters(
                Map.of(
                    UID.of("toUpdate000"),
                    List.of(new QueryFilter(QueryOperator.EQ, "summer day"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("QS6w44flWAf"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributesWithMultipleFiltersOnDifferentAttributes()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .attributeFilters(
                Map.of(
                    UID.of("toUpdate000"),
                    List.of(new QueryFilter(QueryOperator.EQ, "rainy day")),
                    UID.of("notUpdated0"),
                    List.of(new QueryFilter(QueryOperator.EQ, "winter day"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributesWithMultipleFiltersOnTheSameAttribute()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .attributeFilters(
                Map.of(
                    UID.of("toUpdate000"),
                    List.of(
                        new QueryFilter(QueryOperator.LIKE, "day"),
                        new QueryFilter(QueryOperator.LIKE, "in"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentOccurredAfterSetToAfterLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void shouldReturnNoEventsWhenParamStartDueDateLaterThanEventDueDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.scheduledAfter(parseDate("2021-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldReturnEventsWhenParamStartDueDateEarlierThanEventsDueDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.scheduledAfter(parseDate("2018-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldReturnNoEventsWhenParamEndDueDateEarlierThanEventDueDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.scheduledBefore(parseDate("2018-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldReturnEventsWhenParamEndDueDateLaterThanEventsDueDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.scheduledBefore(parseDate("2021-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  private DataElement dataElement(UID uid) {
    return dataElementService.getDataElement(uid.getValue());
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(
        t,
        () ->
            String.format(
                "'%s' with uid '%s' should have been created", type.getSimpleName(), uid));
    return t;
  }

  private List<String> getEvents(EventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(eventService.getEvents(params));
  }

  private static List<String> uids(List<? extends BaseIdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(BaseIdentifiableObject::getUid).toList();
  }
}
