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
package org.hisp.dhis.tracker.export.event;

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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
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
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
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
class EventExporterTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private EventService eventService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private ProgramStage programStage;

  private Program program;

  private TrackedEntity trackedEntity;
  private User importUser;

  private EventOperationParams.EventOperationParamsBuilder operationParamsBuilder;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
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

    List<Event> events = eventService.findEvents(params);

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

    List<Event> events = eventService.findEvents(params);

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

    List<Event> events = eventService.findEvents(params);

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

    List<Event> events = eventService.findEvents(params);

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
  void testExportEventsWhenFilteringByNumberAttributesUsingEq()
      throws ForbiddenException, BadRequestException {
    // shows number semantics are applied since 170.0 == 170, this would fail if we were to treat
    // value type number as text "170.0" != "170"
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "V66aa7a2122");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.EQ, "170")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByIntegerAttributesUsingIn()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.IN, "70;72")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "jxgFyJEMUPf"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextAttributesUsingIn()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.IN, "Summer day;rainy Day")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(
        List.of(
            "YKmfzHdjUDL",
            "LCSfHnurnNB",
            "SbUJzkxKYAG",
            "gvULMgNiAfM",
            "JaRDIvcEcEx",
            "jxgFyJEMUPf",
            "D9PbzJY8bJM",
            "pTzf9KYMk72"),
        events);
  }

  @Test
  void testExportEventsWhenFilteringByIntegerAttributesUsingStartsWith()
      throws ForbiddenException, BadRequestException {
    // Operators sw is based on SQL like which is supported for numeric value types by not casting
    // values i.e. treating them as strings
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.SW, "7")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "jxgFyJEMUPf"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextAttributesUsingStartsWith()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.SW, "RAiny")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextAttributesUsingLike()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "notUpdated0");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.LIKE, "0% Winter's")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByIntegerAttributesUsingLessThan()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.LT, "71")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextAttributesUsingLessThan()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.LT, "summer day")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextAttributesUsingNotNull()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "notUpdated0");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByIntegerAttributesUsingNotNull()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .trackedEntity(UID.of("dUE514NMOlo"))
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextDataElementsUsingLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "VaL")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextDataElementsUsingSWWithStatusFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .enrollmentStatus(EnrollmentStatus.ACTIVE)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.SW, "val")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextDataElementsUsingEWWithProgramTypeFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .programType(ProgramType.WITH_REGISTRATION)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EW, "001")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextDataElementsUsingIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement),
                List.of(new QueryFilter(QueryOperator.IN, "Value00001;Value00002")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByTextDataElementsUsingEqWithCategoryOptionSuperUser()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .program(program)
            .attributeCategoryCombo(UID.of("bjDvmb4bfuf"))
            .attributeCategoryOptions(Set.of(UID.of("xYerKDKCefk")))
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "value00001")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsUsingEqWithCategoryOptionNotSuperUser()
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
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "value00002")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsUsingOptionSetEqual()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "option1")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsUsingOptionSetIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.IN, "option1;option2")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsUsingOptionSetLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "opt")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByNumericDataElementsUsingComparisonRange()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00006"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement),
                List.of(
                    new QueryFilter(QueryOperator.LT, "77"),
                    new QueryFilter(QueryOperator.GT, "8")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByNumericDataElementsUsingSWAndEW()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00006"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.SW, "7")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldFilterByEventsNumberDataValueUsingEq() throws ForbiddenException, BadRequestException {
    // shows number semantics are applied since 15.0 == 15, this would fail if we were to treat
    // value type number as text "15.0" != "15"
    EventOperationParams params =
        EventOperationParams.builder()
            .eventParams(EventParams.FALSE)
            .filterByDataElement(
                UID.of("GieVkTxp4HH"), List.of(new QueryFilter(QueryOperator.EQ, "15.0")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "QRYjLTiJTrA"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenDataValuesWhenFilteringByNonNullDataValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .eventParams(EventParams.FALSE)
            .filterByDataElement(
                UID.of("GieVkTxp4HH"), List.of(new QueryFilter(QueryOperator.NNULL)))
            .filterByDataElement(
                UID.of("GieVkTxp4HG"), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "QRYjLTiJTrA"), events);
  }

  @Test
  void shouldFilterByEventsNotContainingGivenDataValueWhenFilteringByNullDataValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .eventParams(EventParams.FALSE)
            .filterByDataElement(
                UID.of("DATAEL00002"), List.of(new QueryFilter(QueryOperator.NULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenDataValueWhenCombiningUnaryAndBinaryOperatorsInFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement),
                List.of(
                    new QueryFilter(QueryOperator.IN, "option2"),
                    new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenDataValueWhenCombiningTwoUnaryOperatorsInFilter()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .eventParams(EventParams.FALSE)
            .filterByDataElement(
                UID.of("DATAEL00002"),
                List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
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

    List<Event> events = eventService.findEvents(params);

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
  void testEnrollmentEnrolledBeforeSetToBeforeFirstEnrolledAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentEnrolledBefore(parseDate("2021-02-27T12:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
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
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getUid())
            .toList();

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void shouldOnlyIncludeEventsWithGivenAttributeWhenFilterAttributeHasNoQueryFilter()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orgUnit(orgUnit).filterByAttribute(UID.of("notUpdated0")).build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterNumericAttributes() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .filterByAttribute(
                UID.of("integerAttr"),
                List.of(
                    new QueryFilter(QueryOperator.LT, "77"),
                    new QueryFilter(QueryOperator.GT, "8")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributes() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .filterByAttribute(
                UID.of("toUpdate000"), List.of(new QueryFilter(QueryOperator.EQ, "summer day")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
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
            .filterByAttribute(
                UID.of("toUpdate000"), List.of(new QueryFilter(QueryOperator.EQ, "rainy day")))
            .filterByAttribute(
                UID.of("integerAttr"), List.of(new QueryFilter(QueryOperator.EQ, "70")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
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
            .filterByAttribute(
                UID.of("toUpdate000"),
                List.of(
                    new QueryFilter(QueryOperator.LIKE, "day"),
                    new QueryFilter(QueryOperator.LIKE, "in")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldFilterByEventsContainingGivenAttributeValueWhenCombiningTwoUnaryOperators()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .eventParams(EventParams.FALSE)
            .filterByAttribute(
                UID.of("dIVt4l5vIOa"),
                List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenAttributeValueWhenCombiningUnaryAndBinaryOperators()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .eventParams(EventParams.FALSE)
            .filterByAttribute(
                UID.of("toUpdate000"),
                List.of(
                    new QueryFilter(QueryOperator.NNULL),
                    new QueryFilter(QueryOperator.EQ, "rainy day")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldFilterByEventsNotContainingGivenAttributeValueWhenFilteringByNullValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .eventParams(EventParams.FALSE)
            .filterByAttribute(UID.of("toDelete000"), List.of(new QueryFilter(QueryOperator.NULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("H0PbzJY8bJG"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenAttributeValueWhenFilteringByNonNullValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .programStage(programStage)
            .eventParams(EventParams.FALSE)
            .filterByAttribute(UID.of("dIVt4l5vIOa"), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testEnrollmentOccurredAfterSetToAfterLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.findEvents(params).stream()
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
    return get(DataElement.class, uid.getValue());
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
    return uids(eventService.findEvents(params));
  }

  private static List<String> uids(List<? extends BaseIdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(BaseIdentifiableObject::getUid).toList();
  }
}
