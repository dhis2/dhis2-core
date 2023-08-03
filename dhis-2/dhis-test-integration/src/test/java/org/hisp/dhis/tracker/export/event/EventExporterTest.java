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
import static org.hisp.dhis.tracker.Assertions.assertHasTimeStamp;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.event.EventOperationParams.EventOperationParamsBuilder;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
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

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");
    importUser = userService.getUser("M5zQapPyTZI");
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/event_and_enrollment.json", importUser.getUid())));
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
  void setUp() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by admin
    injectAdminUser();

    operationParamsBuilder = EventOperationParams.builder();
    operationParamsBuilder.orgUnitUid(orgUnit.getUid()).orgUnitMode(SELECTED);
  }

  @Test
  void shouldExportEventAndMapAssignedUserWhenAssignedUserIsNotNull()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .trackedEntityUid(trackedEntity.getUid())
            .enrollments(Set.of("TvctPPhpD8z"))
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertEquals(
        get(Event.class, "D9PbzJY8bJM").getAssignedUser(), events.get(0).getAssignedUser());
  }

  @Test
  void shouldReturnEventsWithRelationships() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.events(Set.of("pTzf9KYMk72")).includeRelationships(true).build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertContainsOnly(List.of("pTzf9KYMk72"), uids(events));
    List<Relationship> relationships =
        events.get(0).getRelationshipItems().stream()
            .map(RelationshipItem::getRelationship)
            .toList();
    assertContainsOnly(List.of("oLT07jKRu9e", "yZxjxJli9mO"), uids(relationships));
  }

  @Test
  void shouldReturnEventsWithNotes() throws ForbiddenException, BadRequestException {
    EventOperationParams params = operationParamsBuilder.events(Set.of("pTzf9KYMk72")).build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertContainsOnly(List.of("pTzf9KYMk72"), uids(events));
    List<TrackedEntityComment> notes = events.get(0).getComments();
    assertContainsOnly(List.of("SGuCABkhpgn", "DRKO4xUVrpr"), uids(notes));
    assertAll(
        () -> assertNote(importUser, "comment value", notes.get(0)),
        () -> assertNote(importUser, "comment value", notes.get(1)));
  }

  @Test
  void shouldReturnPaginatedEventsWithNotesGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    EventOperationParamsBuilder paramsBuilder =
        operationParamsBuilder
            .events(Set.of("pTzf9KYMk72", "D9PbzJY8bJM"))
            .orderBy("executionDate", SortDirection.DESC);

    EventOperationParams params = paramsBuilder.page(1).pageSize(1).build();

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage),
        () -> assertEquals(List.of("D9PbzJY8bJM"), eventUids(firstPage)));

    params = paramsBuilder.page(2).pageSize(1).build();

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage),
        () -> assertEquals(List.of("pTzf9KYMk72"), eventUids(secondPage)));

    params = paramsBuilder.page(3).pageSize(3).build();

    assertIsEmpty(getEvents(params));
  }

  @Test
  void testExportEvents() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.programStageUid(programStage.getUid()).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWithTotalPages() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.programStageUid(programStage.getUid()).totalPages(true).build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByEnrollment() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .trackedEntityUid(trackedEntity.getUid())
            .enrollments(Set.of("TvctPPhpD8z"))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithExecutionAndUpdateDates()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
            .startDate(getDate(2018, 1, 1))
            .endDate(getDate(2020, 1, 29))
            .skipChangedBefore(getDate(2018, 1, 1))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithLastUpdateDuration() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
            .updatedWithin("1d")
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWithLastUpdateDates() throws ForbiddenException, BadRequestException {
    Date date = new Date();
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
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
        EventOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of("pTzf9KYMk72"))
            .build();

    Events events = eventService.getEvents(params);

    assertNotEmpty(events.getEvents());

    Event event = events.getEvents().get(0);

    assertAll(
        "All dates should include timestamp",
        () ->
            assertEquals(
                "2019-01-25T12:10:38.100",
                DateUtils.getIso8601NoTz(event.getExecutionDate()),
                () ->
                    String.format(
                        "Expected %s to be in %s",
                        event.getExecutionDate(), "2019-01-25T12:10:38.100")),
        () ->
            assertEquals(
                "2019-01-28T12:32:38.100",
                DateUtils.getIso8601NoTz(event.getDueDate()),
                () ->
                    String.format(
                        "Expected %s to be in %s", event.getDueDate(), "2019-01-28T12:32:38.100")),
        () -> assertHasTimeStamp(event.getCompletedDate()));
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00001");
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of("DATAEL00001", List.of(new QueryFilter(QueryOperator.LIKE, "%val%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);

    new QueryItem(dataElement, QueryOperator.LIKE, "val", dataElement.getValueType(), null, null);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithStatusFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00001");

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .programStatus(ProgramStatus.ACTIVE)
            .dataElementFilters(
                Map.of(dataElement.getUid(), List.of(new QueryFilter(QueryOperator.LIKE, "%val%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithProgramTypeFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00001");

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .programType(ProgramType.WITH_REGISTRATION)
            .dataElementFilters(
                Map.of(dataElement.getUid(), List.of(new QueryFilter(QueryOperator.LIKE, "%val%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsEqual()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00001");

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of(
                    dataElement.getUid(),
                    List.of(new QueryFilter(QueryOperator.LIKE, "%value00001%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsIn()
      throws ForbiddenException, BadRequestException {
    DataElement datael00001 = dataElement("DATAEL00001");

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of(
                    datael00001.getUid(),
                    List.of(new QueryFilter(QueryOperator.IN, "value00001;value00002"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithCategoryOptionSuperUser()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00001");

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .programUid(program.getUid())
            .attributeCategoryCombo("bjDvmb4bfuf")
            .attributeCategoryOptions(Set.of("xYerKDKCefk"))
            .dataElementFilters(
                Map.of(
                    dataElement.getUid(), List.of(new QueryFilter(QueryOperator.EQ, "value00001"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldReturnPaginatedPublicEventsWithMultipleCategoryOptionsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventOperationParamsBuilder paramsBuilder =
        operationParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programUid(program.getUid())
            .orderBy("executionDate", SortDirection.DESC);

    EventOperationParams params = paramsBuilder.page(1).pageSize(3).build();

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 3, false, firstPage),
        () ->
            assertEquals(
                List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp"), eventUids(firstPage)));

    params = paramsBuilder.page(2).pageSize(3).build();

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 3, true, secondPage),
        () ->
            assertEquals(
                List.of("lumVtWwwy0O", "QRYjLTiJTrA", "cadc5eGj0j7"), eventUids(secondPage)));

    params = paramsBuilder.page(3).pageSize(3).build();

    assertIsEmpty(getEvents(params));
  }

  @Test
  void shouldReturnPaginatedEventsWithMultipleCategoryOptionsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventOperationParams params =
        operationParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programUid(program.getUid())
            .orderBy("executionDate", SortDirection.DESC)
            .page(1)
            .pageSize(2)
            .totalPages(true)
            .build();

    Events events = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertPager(1, 2, 6, events),
        () -> assertEquals(List.of("ck7DzdxqLqA", "OTmjvJDn0Fu"), eventUids(events)));
  }

  @Test
  void shouldReturnEventsGivenCategoryOptionCombo() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid("DiszpKrYNg8")
            .orgUnitMode(SELECTED)
            .attributeCategoryCombo("O4VaNks6tta")
            .attributeCategoryOptions(Set.of("xwZ2u3WyQR0", "M58XdOfhiJ7"))
            .build();

    Events events = eventService.getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "OTmjvJDn0Fu"), eventUids(events));
    List<Executable> executables =
        events.getEvents().stream()
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
            .collect(Collectors.toList());
    assertAll("all events should have the same category option combo and options", executables);
  }

  @Test
  void shouldFailIfCategoryOptionComboOfGivenEventDoesNotHaveAValueForGivenIdScheme() {
    IdSchemes idSchemes = new IdSchemes();
    idSchemes.setCategoryOptionComboIdScheme("ATTRIBUTE:GOLswS44mh8");
    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid("DiszpKrYNg8")
            .orgUnitMode(SELECTED)
            .idSchemes(idSchemes)
            .events(Set.of("kWjSezkXHVp"))
            .build();

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> eventService.getEvents(params));
    assertStartsWith("CategoryOptionCombo", ex.getMessage());
    assertContains("not have a value assigned for idScheme ATTRIBUTE:GOLswS44mh8", ex.getMessage());
  }

  @Test
  void shouldReturnEventsGivenIdSchemeCode() throws ForbiddenException, BadRequestException {
    IdSchemes idSchemes = new IdSchemes();
    idSchemes.setProgramIdScheme("code");
    idSchemes.setProgramStageIdScheme("code");
    idSchemes.setOrgUnitIdScheme("code");
    idSchemes.setCategoryOptionComboIdScheme("code");

    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid("DiszpKrYNg8")
            .orgUnitMode(SELECTED)
            .idSchemes(idSchemes)
            .attributeCategoryCombo("O4VaNks6tta")
            .attributeCategoryOptions(Set.of("xwZ2u3WyQR0", "M58XdOfhiJ7"))
            .build();

    Events events = eventService.getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "OTmjvJDn0Fu"), eventUids(events));
    List<Executable> executables =
        events.getEvents().stream()
            .map(
                e ->
                    (Executable)
                        () ->
                            assertAll(
                                "event " + e.getUid(),
                                () ->
                                    assertEquals(
                                        "multi-program", e.getEnrollment().getProgram().getUid()),
                                () -> assertEquals("multi-stage", e.getProgramStage().getUid()),
                                () ->
                                    assertEquals(
                                        "DiszpKrYNg8",
                                        e.getOrganisationUnit()
                                            .getUid()), // TODO(DHIS2-14968): this might be a bug
                                // caused by
                                // https://github.com/dhis2/dhis2-core/pull/12518
                                () ->
                                    assertEquals(
                                        "COC_1153452", e.getAttributeOptionCombo().getUid()),
                                () ->
                                    assertContainsOnly(
                                        Set.of("xwZ2u3WyQR0", "M58XdOfhiJ7"),
                                        e.getAttributeOptionCombo().getCategoryOptions().stream()
                                            .map(CategoryOption::getUid)
                                            .collect(Collectors.toSet()))))
            .collect(Collectors.toList());
    assertAll("all events should have the same category option combo and options", executables);
  }

  @Test
  void shouldReturnEventsGivenIdSchemeAttribute() throws ForbiddenException, BadRequestException {
    IdSchemes idSchemes = new IdSchemes();
    idSchemes.setProgramIdScheme("ATTRIBUTE:j45AR9cBQKc");
    idSchemes.setProgramStageIdScheme("ATTRIBUTE:j45AR9cBQKc");
    idSchemes.setOrgUnitIdScheme("ATTRIBUTE:j45AR9cBQKc");
    idSchemes.setCategoryOptionComboIdScheme("ATTRIBUTE:j45AR9cBQKc");
    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid("DiszpKrYNg8")
            .orgUnitMode(SELECTED)
            .idSchemes(idSchemes)
            .attributeCategoryCombo("O4VaNks6tta")
            .attributeCategoryOptions(Set.of("xwZ2u3WyQR0", "M58XdOfhiJ7"))
            .build();

    Events events = eventService.getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "OTmjvJDn0Fu"), eventUids(events));
    List<Executable> executables =
        events.getEvents().stream()
            .map(
                e ->
                    (Executable)
                        () ->
                            assertAll(
                                "event " + e.getUid(),
                                () ->
                                    assertEquals(
                                        "multi-program-attribute",
                                        e.getEnrollment().getProgram().getUid()),
                                () ->
                                    assertEquals(
                                        "multi-program-stage-attribute",
                                        e.getProgramStage().getUid()),
                                () ->
                                    assertEquals(
                                        "DiszpKrYNg8",
                                        e.getOrganisationUnit()
                                            .getUid()), // TODO(DHIS2-14968): this might be a bug
                                // caused by
                                // https://github.com/dhis2/dhis2-core/pull/12518
                                () ->
                                    assertEquals(
                                        "COC_1153452-attribute",
                                        e.getAttributeOptionCombo().getUid()),
                                () ->
                                    assertContainsOnly(
                                        Set.of("xwZ2u3WyQR0", "M58XdOfhiJ7"),
                                        e.getAttributeOptionCombo().getCategoryOptions().stream()
                                            .map(CategoryOption::getUid)
                                            .collect(Collectors.toSet()))))
            .collect(Collectors.toList());
    assertAll("all events should have the same category option combo and options", executables);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithCategoryOptionNotSuperUser()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(
        createAndAddUser(false, "user", Set.of(orgUnit), Set.of(orgUnit), "F_EXPORT_DATA"));
    DataElement dataElement = dataElement("DATAEL00002");

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
            .programUid(program.getUid())
            .attributeCategoryCombo("bjDvmb4bfuf")
            .attributeCategoryOptions(Set.of("xYerKDKCefk"))
            .dataElementFilters(
                Map.of(
                    dataElement.getUid(), List.of(new QueryFilter(QueryOperator.EQ, "value00002"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithOptionSetEqual()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00005");
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of(dataElement.getUid(), List.of(new QueryFilter(QueryOperator.EQ, "option1"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithOptionSetIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00005");
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of(
                    dataElement.getUid(),
                    List.of(new QueryFilter(QueryOperator.IN, "option1;option2"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByDataElementsWithOptionSetLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00005");
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of(dataElement.getUid(), List.of(new QueryFilter(QueryOperator.LIKE, "%opt%"))))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void testExportEventsWhenFilteringByNumericDataElements()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement("DATAEL00006");
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStageUid(programStage.getUid())
            .dataElementFilters(
                Map.of(
                    dataElement.getUid(),
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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

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
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

    assertContainsOnly(List.of("TvctPPhpD8z"), enrollments);
  }

  @Test
  void testEnrollmentFilterNumericAttributes() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .attributeFilters(
                Map.of(
                    "numericAttr",
                    List.of(
                        new QueryFilter(QueryOperator.LT, "77"),
                        new QueryFilter(QueryOperator.GT, "8"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributes() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
    operationParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .attributeFilters(
                Map.of("toUpdate000", List.of(new QueryFilter(QueryOperator.EQ, "summer day"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());

    assertContainsOnly(List.of("QS6w44flWAf"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributesWithMultipleFiltersOnDifferentAttributes()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
    operationParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .attributeFilters(
                Map.of(
                    "toUpdate000",
                    List.of(new QueryFilter(QueryOperator.EQ, "rainy day")),
                    "notUpdated0",
                    List.of(new QueryFilter(QueryOperator.EQ, "winter day"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void testEnrollmentFilterAttributesWithMultipleFiltersOnTheSameAttribute()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
operationParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .attributeFilters(
                Map.of(
                    "toUpdate000",
                    List.of(
                        new QueryFilter(QueryOperator.LIKE, "day"),
                        new QueryFilter(QueryOperator.LIKE, "in"))))
            .build();

    List<String> trackedEntities =
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderEventsOnPrimaryKeyDescByDefault() throws ForbiddenException, BadRequestException {
    Event d9PbzJY8bJM = get(Event.class, "D9PbzJY8bJM");
    Event pTzf9KYMk72 = get(Event.class, "pTzf9KYMk72");
    List<String> expected =
        Stream.of(d9PbzJY8bJM, pTzf9KYMk72)
            .sorted(Comparator.comparing(Event::getId).reversed()) // reversed = desc
            .map(Event::getUid)
            .toList();

    EventOperationParams params =
        EventOperationParams.builder().orgUnitUid(orgUnit.getUid()).build();

    List<String> events = getEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderEventsByEnrollmentProgramUIDAsc() throws ForbiddenException, BadRequestException {
    Event pTzf9KYMk72 =
        get(Event.class, "pTzf9KYMk72"); // enrolled in program BFcipDERJnf with registration
    Event QRYjLTiJTrA =
        get(Event.class, "QRYjLTiJTrA"); // enrolled in program BFcipDERJne without registration
    List<String> expected =
        Stream.of(pTzf9KYMk72, QRYjLTiJTrA)
            .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
            .map(Event::getUid)
            .toList();
    System.out.println(expected);

    EventOperationParams params =
        EventOperationParams.builder()
            .events(Set.of("pTzf9KYMk72", "QRYjLTiJTrA"))
            .orderBy("enrollment.program.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderEventsByAttributeAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orderBy(UID.of("toUpdate000"), SortDirection.ASC).build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByAttributeDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orderBy(UID.of("toUpdate000"), SortDirection.DESC).build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), uids(events));
    List<String> trackedEntities =
        events.stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());
    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesAndPaginateWhenGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    EventOperationParamsBuilder paramsBuilder =
        operationParamsBuilder
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC);

    EventOperationParams params = paramsBuilder.page(1).pageSize(1).build();

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage),
        () -> assertEquals(List.of("D9PbzJY8bJM"), eventUids(firstPage)));

    params = paramsBuilder.page(2).pageSize(1).build();

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage),
        () -> assertEquals(List.of("pTzf9KYMk72"), eventUids(secondPage)));

    params = paramsBuilder.page(3).pageSize(3).build();

    assertIsEmpty(getEvents(params));
  }

  @Test
  void testEnrollmentOccurredAfterSetToAfterLastOccurredAtDate()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .enrollmentOccurredAfter(parseDate("2021-03-28T13:05:00.000"))
            .build();

    List<String> enrollments =
        eventService.getEvents(params).getEvents().stream()
            .map(event -> event.getEnrollment().getUid())
            .collect(Collectors.toList());

    assertIsEmpty(enrollments);
  }

  @Test
  void testOrderByEnrolledAtDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orderBy("enrollment.enrollmentDate", SortDirection.DESC).build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testOrderByEnrolledAtAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orderBy("enrollment.enrollmentDate", SortDirection.ASC).build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void testOrderByOccurredAtDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orderBy("executionDate", SortDirection.DESC).build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void testOrderByOccurredAtAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orderBy("executionDate", SortDirection.ASC).build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
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

  @Test
  void shouldSortEntitiesRespectingOrderWhenAttributeOrderSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeAttributeOrder()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenDataElementSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orderBy("dueDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldSortEntitiesRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  private void assertNote(
      User expectedLastUpdatedBy, String expectedNote, TrackedEntityComment actual) {
    assertEquals(expectedNote, actual.getCommentText());
    assertEquals(expectedLastUpdatedBy, actual.getLastUpdatedBy());
  }

  private DataElement dataElement(String uid) {
    return dataElementService.getDataElement(uid);
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

  private List<String> getEvents(EventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(eventService.getEvents(params).getEvents());
  }

  private static List<String> eventUids(Events events) {
    return uids(events.getEvents());
  }

  private static List<String> uids(List<? extends BaseIdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(BaseIdentifiableObject::getUid).toList();
  }
}
