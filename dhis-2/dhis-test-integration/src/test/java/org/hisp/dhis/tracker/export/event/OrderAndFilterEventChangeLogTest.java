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
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class OrderAndFilterEventChangeLogTest extends TrackerTest {

  @Autowired private EventChangeLogService eventChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private TrackerImportParams importParams;

  private final PageParams defaultPageParams = new PageParams(null, null, false);

  private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private TrackerObjects trackerObjects;

  @BeforeAll
  void setUp() throws IOException {
    injectSecurityContextUser(getAdminUser());
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    importParams = TrackerImportParams.builder().build();
    trackerObjects = fromJson("tracker/event_and_enrollment.json");

    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  private static Stream<Arguments> provideDateAndUsernameOrderParams() {
    return Stream.of(
        Arguments.of("createdAt", SortDirection.DESC),
        Arguments.of("username", SortDirection.DESC),
        Arguments.of("username", SortDirection.ASC));
  }

  @ParameterizedTest
  @MethodSource("provideDateAndUsernameOrderParams")
  void shouldSortChangeLogsByCreatedAtDescWhenOrderingByDateOrUsername(
      String field, SortDirection sortDirection) throws ForbiddenException, NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy(field, sortDirection).build();
    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = getFirstDataElement(event);

    updateDataValues(event, dataElementUid, "20", "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("QRYjLTiJTrA"), params, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(0)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "15", "20", changeLogs.get(1)),
        () -> assertDataElementCreate("GieVkTxp4HH", "15", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsWhenOrderingByCreatedAtAsc()
      throws ForbiddenException, NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("createdAt", SortDirection.ASC).build();

    Event event = getEvent("QRYjLTiJTrA");
    String dataElementUid = getFirstDataElement(event);

    updateDataValues(event, dataElementUid, "20", "25");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("QRYjLTiJTrA"), params, defaultPageParams));

    assertNumberOfChanges(3, changeLogs);
    assertAll(
        () -> assertDataElementCreate(dataElementUid, "15", changeLogs.get(0)),
        () -> assertDataElementUpdate(dataElementUid, "15", "20", changeLogs.get(1)),
        () -> assertDataElementUpdate(dataElementUid, "20", "25", changeLogs.get(2)));
  }

  @Test
  void shouldSortChangeLogsWhenOrderingByDataElementAsc()
      throws ForbiddenException, NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("dataElement", SortDirection.ASC).build();
    Event event = getEvent("kWjSezkXHVp");

    updateDataValues(event, "GieVkTxp4HH", "20", "25");
    updateDataValues(event, "GieVkTxp4HG", "20");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("kWjSezkXHVp"), params, defaultPageParams));

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () -> assertDataElementUpdate("GieVkTxp4HG", "10", "20", changeLogs.get(0)),
        () -> assertDataElementCreate("GieVkTxp4HG", "10", changeLogs.get(1)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(2)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "15", "20", changeLogs.get(3)),
        () -> assertDataElementCreate("GieVkTxp4HH", "15", changeLogs.get(4)));
  }

  @Test
  void shouldSortChangeLogsWhenOrderingByDataElementDesc()
      throws ForbiddenException, NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("dataElement", SortDirection.DESC).build();
    Event event = getEvent("kWjSezkXHVp");

    updateDataValues(event, "GieVkTxp4HH", "20", "25");
    updateDataValues(event, "GieVkTxp4HG", "20");

    List<EventChangeLog> changeLogs =
        getDataElementChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("kWjSezkXHVp"), params, defaultPageParams));

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () -> assertDataElementUpdate("GieVkTxp4HH", "20", "25", changeLogs.get(0)),
        () -> assertDataElementUpdate("GieVkTxp4HH", "15", "20", changeLogs.get(1)),
        () -> assertDataElementCreate("GieVkTxp4HH", "15", changeLogs.get(2)),
        () -> assertDataElementUpdate("GieVkTxp4HG", "10", "20", changeLogs.get(3)),
        () -> assertDataElementCreate("GieVkTxp4HG", "10", changeLogs.get(4)));
  }

  @Test
  void shouldSortChangeLogsWhenOrderingByPropertyAsc()
      throws ForbiddenException, NotFoundException, IOException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("property", SortDirection.ASC).build();
    UID event = UID.of("QRYjLTiJTrA");

    LocalDateTime currentTime = LocalDateTime.now();
    updateEventDates(event, currentTime.toDate().toInstant());

    List<EventChangeLog> changeLogs =
        getAllPropertyChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("QRYjLTiJTrA"), params, defaultPageParams));

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () -> assertPropertyCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(0)),
        () ->
            assertPropertyUpdate(
                "occurredAt",
                "2022-04-20 06:00:38.343",
                currentTime.toString(formatter),
                changeLogs.get(1)),
        () -> assertPropertyCreate("occurredAt", "2022-04-20 06:00:38.343", changeLogs.get(2)),
        () ->
            assertPropertyUpdate(
                "scheduledAt",
                "2022-04-22 06:00:38.343",
                currentTime.toString(formatter),
                changeLogs.get(3)),
        () -> assertPropertyCreate("scheduledAt", "2022-04-22 06:00:38.343", changeLogs.get(4)));
  }

  @Test
  void shouldSortChangeLogsWhenOrderingByPropertyDesc()
      throws ForbiddenException, NotFoundException, IOException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder().orderBy("property", SortDirection.DESC).build();
    UID event = UID.of("QRYjLTiJTrA");

    LocalDateTime currentTime = LocalDateTime.now();
    updateEventDates(event, currentTime.toDate().toInstant());

    List<EventChangeLog> changeLogs =
        getAllPropertyChangeLogs(
            eventChangeLogService.getEventChangeLog(
                UID.of("QRYjLTiJTrA"), params, defaultPageParams));

    assertNumberOfChanges(5, changeLogs);
    assertAll(
        () ->
            assertPropertyUpdate(
                "scheduledAt",
                "2022-04-22 06:00:38.343",
                currentTime.toString(formatter),
                changeLogs.get(0)),
        () -> assertPropertyCreate("scheduledAt", "2022-04-22 06:00:38.343", changeLogs.get(1)),
        () ->
            assertPropertyUpdate(
                "occurredAt",
                "2022-04-20 06:00:38.343",
                currentTime.toString(formatter),
                changeLogs.get(2)),
        () -> assertPropertyCreate("occurredAt", "2022-04-20 06:00:38.343", changeLogs.get(3)),
        () -> assertPropertyCreate("geometry", "(-11.419700, 8.103900)", changeLogs.get(4)));
  }

  @Test
  void shouldFilterChangeLogsWhenFilteringByUser() throws ForbiddenException, NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("username", new QueryFilter(QueryOperator.EQ, importUser.getUsername()))
            .build();

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"), params, defaultPageParams);

    Set<String> changeLogUsers =
        changeLogs.getItems().stream()
            .map(cl -> cl.getCreatedBy().getUsername())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(importUser.getUsername()), changeLogUsers);
  }

  @Test
  void shouldFilterChangeLogsWhenFilteringByDataElement()
      throws ForbiddenException, NotFoundException {
    Event event = getEvent("kWjSezkXHVp");
    String dataElement = getFirstDataElement(event);
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("dataElement", new QueryFilter(QueryOperator.EQ, dataElement))
            .build();

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of(event.getUid()), params, defaultPageParams);

    Set<String> changeLogDataElements =
        changeLogs.getItems().stream()
            .map(cl -> cl.getDataElement().getUid())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(dataElement), changeLogDataElements);
  }

  private Stream<Arguments> provideEventProperties() {
    return Stream.of(
        Arguments.of("occurredAt"), Arguments.of("scheduledAt"), Arguments.of("geometry"));
  }

  @ParameterizedTest
  @MethodSource("provideEventProperties")
  void shouldFilterChangeLogsWhenFilteringByOccurredAt(String filterValue)
      throws ForbiddenException, NotFoundException {
    EventChangeLogOperationParams params =
        EventChangeLogOperationParams.builder()
            .filterBy("property", new QueryFilter(QueryOperator.EQ, filterValue))
            .build();

    Page<EventChangeLog> changeLogs =
        eventChangeLogService.getEventChangeLog(UID.of("QRYjLTiJTrA"), params, defaultPageParams);

    Set<String> changeLogOccurredAtProperties =
        changeLogs.getItems().stream()
            .map(EventChangeLog::getEventProperty)
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(filterValue), changeLogOccurredAtProperties);
  }

  private void updateDataValue(String event, String dataElementUid, String newValue) {
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().getValue().equalsIgnoreCase(event))
        .findFirst()
        .flatMap(
            e ->
                e.getDataValues().stream()
                    .filter(
                        dv -> dv.getDataElement().getIdentifier().equalsIgnoreCase(dataElementUid))
                    .findFirst())
        .ifPresent(dv -> dv.setValue(newValue));
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  private void updateEventDates(UID event, Instant newDate) throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equals(event))
        .findFirst()
        .ifPresent(
            e -> {
              e.setOccurredAt(newDate);
              e.setScheduledAt(newDate);
            });
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
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
        () -> assertEquals("CREATE", changeLog.getChangeLogType().name()),
        () -> assertDataElementChange(dataElement, null, currentValue, changeLog));
  }

  private void assertPropertyCreate(
      String property, String currentValue, EventChangeLog changeLog) {
    assertAll(
        () -> assertUser(importUser, changeLog),
        () -> assertEquals("CREATE", changeLog.getChangeLogType().name()),
        () -> assertPropertyChange(property, null, currentValue, changeLog));
  }

  private void assertDataElementUpdate(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(dataElement, null, previousValue, currentValue, changeLog, importUser);
  }

  private void assertPropertyUpdate(
      String property, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertUpdate(null, property, previousValue, currentValue, changeLog, importUser);
  }

  private void assertUpdate(
      String dataElement,
      String property,
      String previousValue,
      String currentValue,
      EventChangeLog changeLog,
      User user) {
    assertAll(
        () -> assertUser(user, changeLog),
        () -> assertEquals("UPDATE", changeLog.getChangeLogType().name()),
        () -> {
          if (dataElement != null) {
            assertDataElementChange(dataElement, previousValue, currentValue, changeLog);
          } else {
            assertPropertyChange(property, previousValue, currentValue, changeLog);
          }
        });
  }

  private static void assertDataElementChange(
      String dataElement, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(
        dataElement,
        changeLog.getDataElement() != null ? changeLog.getDataElement().getUid() : null);
    assertEquals(previousValue, changeLog.getPreviousValue());
    assertEquals(currentValue, changeLog.getCurrentValue());
  }

  private static void assertPropertyChange(
      String property, String previousValue, String currentValue, EventChangeLog changeLog) {
    assertEquals(property, changeLog.getEventProperty());
    assertEquals(previousValue, changeLog.getPreviousValue());
    assertEquals(currentValue, changeLog.getCurrentValue());
  }

  private static void assertUser(User user, EventChangeLog changeLog) {
    assertAll(
        () -> assertEquals(user.getUsername(), changeLog.getCreatedBy().getUsername()),
        () ->
            assertEquals(
                user.getFirstName(),
                changeLog.getCreatedBy() == null ? null : changeLog.getCreatedBy().getFirstName()),
        () ->
            assertEquals(
                user.getSurname(),
                changeLog.getCreatedBy() == null ? null : changeLog.getCreatedBy().getSurname()),
        () ->
            assertEquals(
                user.getUid(),
                changeLog.getCreatedBy() == null ? null : changeLog.getCreatedBy().getUid()));
  }

  private List<EventChangeLog> getDataElementChangeLogs(Page<EventChangeLog> changeLogs) {
    return changeLogs.getItems().stream().filter(cl -> cl.getDataElement() != null).toList();
  }

  private List<EventChangeLog> getAllPropertyChangeLogs(Page<EventChangeLog> changeLogs) {
    return changeLogs.getItems().stream().filter(cl -> cl.getEventProperty() != null).toList();
  }

  private void updateDataValues(Event event, String dataElementUid, String... values) {
    for (String value : values) {
      updateDataValue(event.getUid(), dataElementUid, value);
    }
  }

  private String getFirstDataElement(Event event) {
    return event.getEventDataValues().iterator().next().getDataElement();
  }
}
