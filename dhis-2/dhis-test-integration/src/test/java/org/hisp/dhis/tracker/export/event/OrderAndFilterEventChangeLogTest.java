/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLogService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLogService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderAndFilterEventChangeLogTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private SingleEventChangeLogService singleEventChangeLogService;

  @Autowired private TrackerEventChangeLogService trackerEventChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private final PageParams defaultPageParams;

  private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private TrackerObjects trackerObjects;

  OrderAndFilterEventChangeLogTest() throws BadRequestException {
    defaultPageParams = PageParams.of(1, 50, false);
  }

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    trackerObjects = testSetup.importTrackerData();
  }

  private static Stream<Arguments> provideDateAndUsernameOrderParams() {
    return Stream.of(
        Arguments.of("createdAt", SortDirection.DESC),
        Arguments.of("username", SortDirection.DESC),
        Arguments.of("username", SortDirection.ASC));
  }

  @ParameterizedTest
  @MethodSource("provideDateAndUsernameOrderParams")
  void shouldSortChangeLogsForSingleEventByCreatedAtDescWhenOrderingByDateOrUsername(
      String field, SortDirection sortDirection) throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy(field, sortDirection).build();
    Event event = getEvent("OTmjvJDn0Fu");
    String dataElementUid = getFirstDataElement(event);

    updateDataValues(event, dataElementUid, "20", "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of("OTmjvJDn0Fu"), params, defaultPageParams),
            dataElementUid);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(0)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "13", "20", changeLogs.get(1)),
        () -> assertDataElementCreate("GieVkTxp4HH", "13", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsForSingleEventWhenOrderingByCreatedAtAsc() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("createdAt", SortDirection.ASC).build();

    Event event = getEvent("OTmjvJDn0Fu");
    String dataElementUid = getFirstDataElement(event);

    updateDataValues(event, dataElementUid, "20", "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of("OTmjvJDn0Fu"), params, defaultPageParams),
            dataElementUid);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementCreate(dataElementUid, "13", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElementUid, "13", "20", changeLogs.get(1)),
        () -> assertDataElementUpdate(dataElementUid, "20", "25", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsForSingleEventWhenOrderingByDataElementAsc() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.ASC).build();
    Event event = getEvent("OTmjvJDn0Fu");

    updateDataValues(event, "GieVkTxp4HH", "20", "25");

    List<EventChangeLog> changeLogs =
        singleEventChangeLogService
            .getEventChangeLog(UID.of("OTmjvJDn0Fu"), params, defaultPageParams)
            .getItems();

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(0)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(1)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "13", "20", changeLogs.get(2)),
        () -> assertDataElementCreate("GieVkTxp4HH", "13", changeLogs.get(3)),
        () -> assertFieldCreate("occurredAt", "2022-04-23 06:00:38.343", changeLogs.get(4)));
  }

  @Test
  void shouldSortChangeLogsForSingleEventWhenOrderingByChangeDesc() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.DESC).build();
    Event event = getEvent("OTmjvJDn0Fu");

    updateDataValues(event, "GieVkTxp4HH", "20", "25");

    List<EventChangeLog> changeLogs =
        singleEventChangeLogService
            .getEventChangeLog(UID.of("OTmjvJDn0Fu"), params, defaultPageParams)
            .getItems();

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () -> assertFieldCreate("occurredAt", "2022-04-23 06:00:38.343", changeLogs.get(0)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(1)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "13", "20", changeLogs.get(2)),
        () -> assertDataElementCreate("GieVkTxp4HH", "13", changeLogs.get(3)),
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(4)));
  }

  @Test
  void shouldSortChangeLogsForSingleEventWhenOrderingByChangeAscAndChangesOnlyToEventFields()
      throws NotFoundException, IOException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.ASC).build();
    UID event = UID.of("OTmjvJDn0Fu");

    LocalDateTime currentTime = LocalDateTime.now();
    updateEventDates(event, currentTime.toDate().toInstant());

    List<EventChangeLog> changeLogs =
        getAllFieldChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of("OTmjvJDn0Fu"), params, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(0)),
        () ->
            assertFieldUpdate(
                "occurredAt",
                "2022-04-23 06:00:38.343",
                currentTime.toString(formatter),
                changeLogs.get(1)),
        () -> assertFieldCreate("occurredAt", "2022-04-23 06:00:38.343", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsForSingleEventWhenOrderingByChangeDescAndChangesOnlyToEventFields()
      throws NotFoundException, IOException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.DESC).build();
    UID event = UID.of("OTmjvJDn0Fu");

    LocalDateTime currentTime = LocalDateTime.now();
    updateEventDates(event, currentTime.toDate().toInstant());

    List<EventChangeLog> changeLogs =
        getAllFieldChangeLogs(
            singleEventChangeLogService.getEventChangeLog(
                UID.of("OTmjvJDn0Fu"), params, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () ->
            assertFieldUpdate(
                "occurredAt",
                "2022-04-23 06:00:38.343",
                currentTime.toString(formatter),
                changeLogs.get(0)),
        () -> assertFieldCreate("occurredAt", "2022-04-23 06:00:38.343", changeLogs.get(1)),
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(2)));
  }

  @Test
  void
      shouldSortChangeLogsForSingleEventByNameWhenOrderingByChangeAndDataElementDoesNotHaveFormName()
          throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.DESC).build();
    Event event = getEvent("OTmjvJDn0Fu");

    updateDataValues(event, "DATAEL00001", "value00002", "value00003");

    List<String> changeLogs =
        singleEventChangeLogService
            .getEventChangeLog(UID.of("OTmjvJDn0Fu"), params, defaultPageParams)
            .getItems()
            .stream()
            .map(this::getDisplayName)
            .toList();

    assertEquals(List.of("occurredAt", "height in cm", "height in cm", "geometry"), changeLogs);
  }

  @Test
  void shouldFilterChangeLogsForSingleEventWhenFilteringByUser() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("username", new QueryFilter(QueryOperator.EQ, importUser.getUsername()))
            .build();

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            UID.of("OTmjvJDn0Fu"), params, defaultPageParams);

    Set<String> changeLogUsers =
        changeLogs.getItems().stream()
            .map(cl -> cl.createdBy().getUsername())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(importUser.getUsername()), changeLogUsers);
  }

  @Test
  void shouldFilterChangeLogsForSingleEventWhenFilteringByDataElement() throws NotFoundException {
    Event event = getEvent("kWjSezkXHVp");
    String dataElement = getFirstDataElement(event);
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("dataElement", new QueryFilter(QueryOperator.EQ, dataElement))
            .build();

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            UID.of(event.getUid()), params, defaultPageParams);

    Set<String> changeLogDataElements =
        changeLogs.getItems().stream()
            .map(cl -> cl.dataElement().getUid())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(dataElement), changeLogDataElements);
  }

  private Stream<Arguments> provideSingleEventField() {
    return Stream.of(Arguments.of("occurredAt"), Arguments.of("geometry"));
  }

  @ParameterizedTest
  @MethodSource("provideSingleEventField")
  void shouldFilterChangeLogsForSingleEventWhenFilteringByField(String filterValue)
      throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("field", new QueryFilter(QueryOperator.EQ, filterValue))
            .build();

    Page<EventChangeLog> changeLogs =
        singleEventChangeLogService.getEventChangeLog(
            UID.of("OTmjvJDn0Fu"), params, defaultPageParams);

    Set<String> changeLogOccurredAtFields =
        changeLogs.getItems().stream().map(EventChangeLog::eventField).collect(Collectors.toSet());
    assertContainsOnly(List.of(filterValue), changeLogOccurredAtFields);
  }

  @ParameterizedTest
  @MethodSource("provideDateAndUsernameOrderParams")
  void shouldSortChangeLogsForTrackerEventByCreatedAtDescWhenOrderingByDateOrUsername(
      String field, SortDirection sortDirection) throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy(field, sortDirection).build();
    Event event = getEvent("pTzf9KYMk72");
    String dataElementUid = "DATAEL00001";

    updateDataValues(event, dataElementUid, "20", "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            trackerEventChangeLogService.getEventChangeLog(
                UID.of("pTzf9KYMk72"), params, defaultPageParams),
            dataElementUid);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementUpdate(dataElementUid, "20", "25", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElementUid, "value00001", "20", changeLogs.get(1)),
        () -> assertDataElementCreate(dataElementUid, "value00001", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsForTrackerEventWhenOrderingByCreatedAtAsc() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("createdAt", SortDirection.ASC).build();

    Event event = getEvent("pTzf9KYMk72");
    String dataElementUid = "DATAEL00001";

    updateDataValues(event, dataElementUid, "20", "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            trackerEventChangeLogService.getEventChangeLog(
                UID.of("pTzf9KYMk72"), params, defaultPageParams),
            dataElementUid);

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementCreate(dataElementUid, "value00001", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElementUid, "value00001", "20", changeLogs.get(1)),
        () -> assertDataElementUpdate(dataElementUid, "20", "25", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsForTrackerEventWhenOrderingByDataElementAsc() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.ASC).build();
    Event event = getEvent("D9PbzJY8bJM");

    updateDataValues(event, "GieVkTxp4HH", "20", "25");

    List<EventChangeLog> changeLogs =
        trackerEventChangeLogService
            .getEventChangeLog(UID.of("D9PbzJY8bJM"), params, defaultPageParams)
            .getItems();

    assertNumberOfChanges(11, changeLogs);
    assertAll(
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(0)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(1)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "15", "20", changeLogs.get(2)),
        () -> assertDataElementCreate("GieVkTxp4HH", "15", changeLogs.get(3)),
        () -> assertFieldCreate("occurredAt", "2020-01-28 00:00:00.000", changeLogs.get(4)),
        () -> assertFieldCreate("scheduledAt", "2019-01-28 12:10:38.100", changeLogs.get(5)),
        () -> assertDataElementCreate("DATAEL00002", "value00002", changeLogs.get(6)),
        () -> assertDataElementCreate("DATAEL00006", "70", changeLogs.get(7)),
        () -> assertDataElementCreate("DATAEL00007", "70", changeLogs.get(8)),
        () -> assertDataElementCreate("DATAEL00001", "value00002", changeLogs.get(9)),
        () -> assertDataElementCreate("DATAEL00005", "option2", changeLogs.get(10)));
  }

  @Test
  void shouldSortChangeLogsForTrackerEventWhenOrderingByChangeDesc() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.DESC).build();
    Event event = getEvent("D9PbzJY8bJM");

    updateDataValues(event, "GieVkTxp4HH", "20", "25");

    List<EventChangeLog> changeLogs =
        trackerEventChangeLogService
            .getEventChangeLog(UID.of("D9PbzJY8bJM"), params, defaultPageParams)
            .getItems();

    assertNumberOfChanges(11, changeLogs);
    assertAll(
        () -> assertDataElementCreate("DATAEL00005", "option2", changeLogs.get(0)),
        () -> assertDataElementCreate("DATAEL00001", "value00002", changeLogs.get(1)),
        () -> assertDataElementCreate("DATAEL00007", "70", changeLogs.get(2)),
        () -> assertDataElementCreate("DATAEL00006", "70", changeLogs.get(3)),
        () -> assertDataElementCreate("DATAEL00002", "value00002", changeLogs.get(4)),
        () -> assertFieldCreate("scheduledAt", "2019-01-28 12:10:38.100", changeLogs.get(5)),
        () -> assertFieldCreate("occurredAt", "2020-01-28 00:00:00.000", changeLogs.get(6)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(7)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "15", "20", changeLogs.get(8)),
        () -> assertDataElementCreate("GieVkTxp4HH", "15", changeLogs.get(9)),
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(10)));
  }

  @Test
  void shouldSortChangeLogsForTrackerEventWhenOrderingByChangeAscAndChangesOnlyToEventFields()
      throws NotFoundException, IOException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.ASC).build();
    UID event = UID.of("D9PbzJY8bJM");

    LocalDateTime currentTime = LocalDateTime.now();
    updateEventDates(event, currentTime.toDate().toInstant());

    List<EventChangeLog> changeLogs =
        getAllFieldChangeLogs(
            trackerEventChangeLogService.getEventChangeLog(
                UID.of("D9PbzJY8bJM"), params, defaultPageParams));

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(0)),
        () ->
            assertFieldUpdate(
                "occurredAt",
                "2020-01-28 00:00:00.000",
                currentTime.toString(formatter),
                changeLogs.get(1)),
        () -> assertFieldCreate("occurredAt", "2020-01-28 00:00:00.000", changeLogs.get(2)),
        () ->
            assertFieldUpdate(
                "scheduledAt",
                "2019-01-28 12:10:38.100",
                currentTime.toString(formatter),
                changeLogs.get(3)),
        () -> assertFieldCreate("scheduledAt", "2019-01-28 12:10:38.100", changeLogs.get(4)));
  }

  @Test
  void shouldSortChangeLogsForTrackerEventWhenOrderingByChangeDescAndChangesOnlyToEventFields()
      throws NotFoundException, IOException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.DESC).build();
    UID event = UID.of("D9PbzJY8bJM");

    LocalDateTime currentTime = LocalDateTime.now();
    updateEventDates(event, currentTime.toDate().toInstant());

    List<EventChangeLog> changeLogs =
        getAllFieldChangeLogs(
            trackerEventChangeLogService.getEventChangeLog(
                UID.of("D9PbzJY8bJM"), params, defaultPageParams));

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () ->
            assertFieldUpdate(
                "scheduledAt",
                "2019-01-28 12:10:38.100",
                currentTime.toString(formatter),
                changeLogs.get(0)),
        () -> assertFieldCreate("scheduledAt", "2019-01-28 12:10:38.100", changeLogs.get(1)),
        () ->
            assertFieldUpdate(
                "occurredAt",
                "2020-01-28 00:00:00.000",
                currentTime.toString(formatter),
                changeLogs.get(2)),
        () -> assertFieldCreate("occurredAt", "2020-01-28 00:00:00.000", changeLogs.get(3)),
        () -> assertFieldCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(4)));
  }

  @Test
  void
      shouldSortChangeLogsForTrackerEventByNameWhenOrderingByChangeAndDataElementDoesNotHaveFormName()
          throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("change", SortDirection.DESC).build();
    Event event = getEvent("pTzf9KYMk72");

    updateDataValues(event, "DATAEL00001", "value00002", "value00003");

    List<String> changeLogs =
        trackerEventChangeLogService
            .getEventChangeLog(UID.of("pTzf9KYMk72"), params, defaultPageParams)
            .getItems()
            .stream()
            .map(this::getDisplayName)
            .toList();

    assertEquals(
        List.of(
            "with-option-set",
            "test-dataelement9",
            "test-dataelement9",
            "test-dataelement9",
            "test-dataelement6",
            "scheduledAt",
            "occurredAt"),
        changeLogs);
  }

  @Test
  void shouldFilterChangeLogsForTrackerEventWhenFilteringByUser() throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("username", new QueryFilter(QueryOperator.EQ, importUser.getUsername()))
            .build();

    Page<EventChangeLog> changeLogs =
        trackerEventChangeLogService.getEventChangeLog(
            UID.of("pTzf9KYMk72"), params, defaultPageParams);

    Set<String> changeLogUsers =
        changeLogs.getItems().stream()
            .map(cl -> cl.createdBy().getUsername())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(importUser.getUsername()), changeLogUsers);
  }

  @Test
  void shouldFilterChangeLogsForTrackerEventWhenFilteringByDataElement() throws NotFoundException {
    Event event = getEvent("pTzf9KYMk72");
    String dataElement = getFirstDataElement(event);
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("dataElement", new QueryFilter(QueryOperator.EQ, dataElement))
            .build();

    Page<EventChangeLog> changeLogs =
        trackerEventChangeLogService.getEventChangeLog(
            UID.of(event.getUid()), params, defaultPageParams);

    Set<String> changeLogDataElements =
        changeLogs.getItems().stream()
            .map(cl -> cl.dataElement().getUid())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(dataElement), changeLogDataElements);
  }

  private Stream<Arguments> provideTrackerEventField() {
    return Stream.of(
        Arguments.of("occurredAt"), Arguments.of("scheduledAt"), Arguments.of("geometry"));
  }

  @ParameterizedTest
  @MethodSource("provideTrackerEventField")
  void shouldFilterChangeLogsForTrackerEventWhenFilteringByField(String filterValue)
      throws NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("field", new QueryFilter(QueryOperator.EQ, filterValue))
            .build();

    Page<EventChangeLog> changeLogs =
        trackerEventChangeLogService.getEventChangeLog(
            UID.of("D9PbzJY8bJM"), params, defaultPageParams);

    Set<String> changeLogOccurredAtFields =
        changeLogs.getItems().stream().map(EventChangeLog::eventField).collect(Collectors.toSet());
    assertContainsOnly(List.of(filterValue), changeLogOccurredAtFields);
  }

  private void updateDataValue(String event, String dataElementUid, String newValue) {
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().getValue().equalsIgnoreCase(event))
        .findFirst()
        .ifPresent(
            e -> {
              e.getDataValues().stream()
                  .filter(
                      dv -> dv.getDataElement().getIdentifier().equalsIgnoreCase(dataElementUid))
                  .findFirst()
                  .ifPresent(dataValue -> dataValue.setValue(newValue));

              assertNoErrors(
                  trackerImportService.importTracker(
                      TrackerImportParams.builder().build(),
                      TrackerObjects.builder().events(List.of(e)).build()));
            });
  }

  private void updateEventDates(UID event, Instant newDate) throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/base_data.json");

    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              org.hisp.dhis.tracker.imports.domain.Event ev =
                  TrackerEvent.builderFromEvent(e).occurredAt(newDate).scheduledAt(newDate).build();

              assertNoErrors(
                  trackerImportService.importTracker(
                      TrackerImportParams.builder().build(),
                      TrackerObjects.builder().events(List.of(ev)).build()));
            });
  }

  private Event getEvent(String uid) {
    Event event = manager.get(Event.class, uid);
    assertNotNull(event);
    return event;
  }

  private static void assertNumberOfChanges(int expected, List<EventChangeLog> changeLogs) {
    assertNotNull(changeLogs);
    assertEquals(
        expected,
        changeLogs.size(),
        String.format(
            "Expected to find %s elements in the event change log list, found %s instead: %s",
            expected, changeLogs.size(), changeLogs));
  }

  private void assertDataElementCreate(
      String dataElement, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.changeLogType().name()),
        () -> assertDataElementChange(dataElement, null, currentValue, changeLog));
  }

  private void assertFieldCreate(String field, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.changeLogType().name()),
        () -> assertFieldChange(field, null, currentValue, changeLog));
  }

  private void assertDataElementUpdate(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(dataElement, null, previousValue, currentValue, changeLog, importUser);
  }

  private void assertFieldUpdate(
      String field, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(null, field, previousValue, currentValue, changeLog, importUser);
  }

  private void assertUpdate(
      String dataElement,
      String field,
      String previousValue,
      String currentValue,
      EventChangeLog changeLog,
      User user) {
    assertAll(
        "asserting update to field " + field,
        () -> assertUser(user, changeLog),
        () -> assertEquals("UPDATE", changeLog.changeLogType().name()),
        () -> {
          if (dataElement != null) {
            assertDataElementChange(dataElement, previousValue, currentValue, changeLog);
          } else {
            assertFieldChange(field, previousValue, currentValue, changeLog);
          }
        });
  }

  private static void assertDataElementChange(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(
        dataElement, changeLog.dataElement() != null ? changeLog.dataElement().getUid() : null);
    assertEquals(previousValue, changeLog.previousValue());
    assertEquals(currentValue, changeLog.currentValue());
  }

  private static void assertFieldChange(
      String field, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(field, changeLog.eventField());
    assertEquals(previousValue, changeLog.previousValue());
    assertEquals(currentValue, changeLog.currentValue());
  }

  private static void assertUser(User user, EventChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.createdBy().getUsername()),
        () ->
            assertEquals(
                user.getFirstName(),
                changeLog.createdBy() == null ? null : changeLog.createdBy().getFirstName()),
        () ->
            assertEquals(
                user.getSurname(),
                changeLog.createdBy() == null ? null : changeLog.createdBy().getSurname()),
        () ->
            assertEquals(
                user.getUid(),
                changeLog.createdBy() == null ? null : changeLog.createdBy().getUid()));
  }

  private List<EventChangeLog> getDataElementChangeLogs(
      Page<EventChangeLog> changeLogs, String dataElementUid) {
    return changeLogs.getItems().stream()
        .filter(cl -> cl.dataElement() != null)
        .filter(cl -> cl.dataElement().getUid().equals(dataElementUid))
        .toList();
  }

  private List<EventChangeLog> getAllFieldChangeLogs(Page<EventChangeLog> changeLogs) {
    return changeLogs.getItems().stream().filter(cl -> cl.eventField() != null).toList();
  }

  private void updateDataValues(Event event, String dataElementUid, String... values) {
    for (String value : values) {
      updateDataValue(event.getUid(), dataElementUid, value);
    }
  }

  private String getFirstDataElement(Event event) {
    return event.getEventDataValues().iterator().next().getDataElement();
  }

  private String getDisplayName(EventChangeLog cl) {
    if (cl.eventField() != null) {
      return cl.eventField();
    } else if (cl.dataElement().getFormName() != null) {
      return cl.dataElement().getFormName();
    } else {
      return cl.dataElement().getName();
    }
  }
}
