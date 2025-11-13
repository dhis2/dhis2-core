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
package org.hisp.dhis.tracker.export.singleevent;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.Assertions.assertNotes;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
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
class SingleEventExporterTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private SingleEventService singleEventService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private User importUser;

  private SingleEventOperationParams.SingleEventOperationParamsBuilder operationParamsBuilder;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");

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
        SingleEventOperationParams.builderForProgram(UID.of("iS7eutanDry"))
            .orgUnit(orgUnit)
            .orgUnitMode(SELECTED);
  }

  @Test
  void shouldThrowBadRequestWhenWhenProgramIsWithRegistration() {
    SingleEventOperationParams params =
        operationParamsBuilder.program(UID.of("shPjYNifvMK")).build();

    assertThrows(BadRequestException.class, () -> singleEventService.findEvents(params));
  }

  @Test
  void shouldExportEventAndMapAssignedUserWhenAssignedUserIsNotNull()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder.assignedUsers(Set.of(UID.of("lPaILkLkgOM"))).build();

    List<SingleEvent> events = singleEventService.findEvents(params);

    assertNotNull(events.get(0).getAssignedUser());
    assertEquals("lPaILkLkgOM", events.get(0).getAssignedUser().getUid());
  }

  @Test
  void shouldReturnEventsWithRelationships() throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder
            .events(Set.of(UID.of("QRYjLTiJTrA")))
            .fields(org.hisp.dhis.tracker.export.singleevent.SingleEventFields.all())
            .build();

    List<SingleEvent> events = singleEventService.findEvents(params);

    assertContainsOnly(List.of("QRYjLTiJTrA"), uids(events));
    List<Relationship> relationships =
        events.get(0).getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .toList();
    assertContainsOnly(List.of("x8919212736"), uids(relationships));
  }

  @Test
  void shouldReturnEventsWithNotes() throws ForbiddenException, BadRequestException {
    SingleEvent event = get(SingleEvent.class, "QRYjLTiJTrA");
    SingleEventOperationParams params =
        operationParamsBuilder.events(Set.of(UID.of("QRYjLTiJTrA"))).build();

    List<SingleEvent> events = singleEventService.findEvents(params);

    assertContainsOnly(List.of("QRYjLTiJTrA"), uids(events));
    assertNotes(event.getNotes(), events.get(0).getNotes());
  }

  @Test
  void shouldExportSingleEventsWhenTheyOccurredAfterAndBeforePassedDates()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder
            .occurredAfter(getDate(2022, 4, 20))
            .occurredBefore(getDate(2022, 4, 22))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("cadc5eGj0j7", "lumVtWwwy0O", "QRYjLTiJTrA"), events);
  }

  @Test
  void testExportEventsWithLastUpdateDuration() throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder.eventStatus(EventStatus.COMPLETED).updatedWithin("1d").build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("cadc5eGj0j7"), events);
  }

  @Test
  void shouldReturnEventsIfItOccurredAtTheSameDateAndTimeOfOccurredBeforePassed()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder.occurredBefore(DateUtils.getDate(2022, 4, 20, 5, 0)).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("cadc5eGj0j7"), events);
  }

  @Test
  void shouldReturnEventsIfItOccurredAtTheSameDateAndTimeOfOccurredAfterPassed()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder.occurredAfter(getDate(2022, 4, 24)).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("ck7DzdxqLqA"), events);
  }

  @Test
  void testExportEventsWithLastUpdateDates() throws ForbiddenException, BadRequestException {
    Date date = new Date();
    SingleEventOperationParams params =
        operationParamsBuilder
            .eventStatus(EventStatus.COMPLETED)
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

    assertContainsOnly(List.of("cadc5eGj0j7"), events);
  }

  @Test
  void testExportEventsWithDatesIncludingTimeStamp()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of(UID.of("cadc5eGj0j7")))
            .build();

    List<SingleEvent> events = singleEventService.findEvents(params);
    SingleEvent event = events.get(0);

    assertAll(
        "All dates should include timestamp",
        () ->
            assertEquals(
                "2022-04-20T03:00:38.343",
                DateUtils.toIso8601NoTz(event.getOccurredDate()),
                () ->
                    String.format(
                        "Expected %s to be in %s",
                        event.getOccurredDate(), "2022-04-20T03:00:38.343")),
        () ->
            assertEquals(
                "2022-07-15T00:00:50.743",
                DateUtils.toIso8601NoTz(event.getCompletedDate()),
                () ->
                    String.format(
                        "Expected %s to be in %s",
                        event.getCompletedDate(), "2022-07-15T00:00:50.743")));
  }

  @Test
  void shouldReturnEventsGivenCategoryOptionCombo() throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        operationParamsBuilder
            .orgUnit(UID.of("DiszpKrYNg8"))
            .orgUnitMode(SELECTED)
            .attributeCategoryCombo(UID.of("O4VaNks6tta"))
            .attributeCategoryOptions(UID.of("xwZ2u3WyQR0", "M58XdOfhiJ7"))
            .build();

    List<SingleEvent> events = singleEventService.findEvents(params);

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

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(
        t,
        () ->
            String.format(
                "'%s' with uid '%s' should have been created", type.getSimpleName(), uid));
    return t;
  }

  private List<String> getEvents(SingleEventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(singleEventService.findEvents(params));
  }

  private static List<String> uids(List<? extends IdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(IdentifiableObject::getUid).toList();
  }
}
