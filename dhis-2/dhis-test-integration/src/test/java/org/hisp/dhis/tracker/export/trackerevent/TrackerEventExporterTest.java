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
package org.hisp.dhis.tracker.export.trackerevent;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.Assertions.assertHasTimeStamp;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Enrico Colasante
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerEventExporterTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerEventService trackerEventService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private ProgramStage programStage;

  private TrackedEntity trackedEntity;
  private User importUser;

  private TrackerEventOperationParams.TrackerEventOperationParamsBuilder operationParamsBuilder;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    programStage = get(ProgramStage.class, "NpsdDv6kKSO");
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

    operationParamsBuilder =
        TrackerEventOperationParams.builder().orgUnit(orgUnit).orgUnitMode(SELECTED);
  }

  @Test
  void shouldExportEventAndMapAssignedUserWhenAssignedUserIsNotNull()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder.assignedUsers(Set.of(UID.of("M5zQapPyTZI"))).build();

    List<TrackerEvent> events = trackerEventService.findEvents(params);

    assertNotNull(events.get(0).getAssignedUser());
    assertEquals("M5zQapPyTZI", events.get(0).getAssignedUser().getUid());
  }

  @Test
  void shouldReturnEventsWithRelationships() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .events(Set.of(UID.of("pTzf9KYMk72")))
            .fields(TrackerEventFields.all())
            .build();

    List<TrackerEvent> events = trackerEventService.findEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), uids(events));
    List<Relationship> relationships =
        events.get(0).getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .toList();
    assertContainsOnly(List.of("oLT07jKRu9e", "yZxjxJli9mO"), uids(relationships));
  }

  @Test
  void shouldReturnEventsWithNotes() throws ForbiddenException, BadRequestException {
    TrackerEvent pTzf9KYMk72 = get(TrackerEvent.class, "pTzf9KYMk72");
    TrackerEventOperationParams params =
        operationParamsBuilder.events(Set.of(UID.of("pTzf9KYMk72"))).build();

    List<TrackerEvent> events = trackerEventService.findEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), uids(events));
    assertNotes(pTzf9KYMk72.getNotes(), events.get(0).getNotes());
  }

  @Test
  void testExportEvents() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params = operationParamsBuilder.programStage(programStage).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByEnrollment() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
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
    TrackerEventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of(UID.of("pTzf9KYMk72")))
            .build();

    List<TrackerEvent> events = trackerEventService.findEvents(params);

    TrackerEvent event = events.get(0);

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
  void shouldReturnEventsGivenCategoryOptionCombo() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .orgUnit(UID.of("uoNW0E3xXUy"))
            .orgUnitMode(SELECTED)
            .attributeCategoryCombo(UID.of("O4VaNks6tta"))
            .attributeCategoryOptions(UID.of("xwZ2u3WyQR0", "M58XdOfhiJ7"))
            .build();

    List<TrackerEvent> events = trackerEventService.findEvents(params);

    assertContainsOnly(List.of("jxgFyJEMUPf"), uids(events));
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
  void testEnrollmentEnrolledBeforeSetToBeforeFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void testEnrollmentEnrolledBeforeEqualToFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledBeforeSetToAfterFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledAfterSetToBeforeLastEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledAfter(parseDate("2021-03-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledAfterEqualToLastEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledAfter(parseDate("2021-03-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentEnrolledAfterSetToAfterLastEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledAfter(parseDate("2021-03-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void testEnrollmentOccurredBeforeSetToBeforeFirstOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredBefore(parseDate("2021-02-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void testEnrollmentOccurredBeforeEqualToFirstOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredBefore(parseDate("2021-02-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentOccurredBeforeSetToAfterFirstOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredBefore(parseDate("2021-02-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("nxP7UnKhomJ"), enrollments);
  }

  @Test
  void testEnrollmentOccurredAfterSetToBeforeLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentOccurredAfterEqualToLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-28T12:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentOccurredAfterSetToAfterLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        trackerEventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertIsEmpty(enrollments);
  }

  @Test
  void shouldReturnNoEventsWhenParamStartDueDateLaterThanEventDueDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder.scheduledAfter(parseDate("2021-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldReturnEventsWhenParamStartDueDateEarlierThanEventsDueDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder.scheduledAfter(parseDate("2018-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldReturnNoEventsWhenParamEndDueDateEarlierThanEventDueDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder.scheduledBefore(parseDate("2018-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldReturnEventsWhenParamEndDueDateLaterThanEventsDueDate()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        operationParamsBuilder.scheduledBefore(parseDate("2021-02-28T13:05:00.000")).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
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

  private List<String> getEvents(TrackerEventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(trackerEventService.findEvents(params));
  }

  private static List<String> uids(List<? extends IdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(IdentifiableObject::getUid).toList();
  }
}
