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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventFields;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventDataValueTest extends PostgresIntegrationTestBase {

  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerEventService trackerEventService;

  @Autowired private JdbcTemplate jdbcTemplate;

  private User userA;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    userA = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(userA);

    testSetup.importTrackerData("tracker/one_te.json");
    testSetup.importTrackerData("tracker/one_enrollment.json");
    manager.flush();
  }

  @Test
  void successWhenEventHasNoProgramAndHasProgramStage() throws IOException {
    testSetup.importTrackerData("tracker/validations/events-with_no_program.json");
  }

  @Test
  void testEventDataValue() throws IOException {
    testSetup.importTrackerData("tracker/event_with_data_values.json");

    List<TrackerEvent> events = manager.getAll(TrackerEvent.class);
    assertEquals(1, events.size());
    TrackerEvent event = events.get(0);
    Set<EventDataValue> eventDataValues = event.getEventDataValues();
    assertEquals(4, eventDataValues.size());
  }

  @Test
  void testEventDataValueUpdate() throws IOException {
    testSetup.importTrackerData("tracker/event_with_data_values.json");

    List<TrackerEvent> events = manager.getAll(TrackerEvent.class);
    assertEquals(1, events.size());
    TrackerEvent event = events.get(0);
    Set<EventDataValue> eventDataValues = event.getEventDataValues();
    assertEquals(4, eventDataValues.size());
    // update

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    testSetup.importTrackerData("tracker/event_with_updated_data_values.json", params);

    clearSession();

    List<TrackerEvent> updatedEvents = manager.getAll(TrackerEvent.class);
    assertEquals(1, updatedEvents.size());
    TrackerEvent updatedEvent = manager.get(TrackerEvent.class, updatedEvents.get(0).getUid());
    assertEquals(3, updatedEvent.getEventDataValues().size());
    List<String> values =
        updatedEvent.getEventDataValues().stream()
            .map(EventDataValue::getValue)
            .collect(Collectors.toList());
    assertThat(values, hasItem("First"));
    assertThat(values, hasItem("Second"));
    assertThat(values, hasItem("Fourth updated"));

    Map<String, EventDataValue> dataValueMap =
        eventDataValues.stream()
            .collect(Collectors.toMap(EventDataValue::getDataElement, ev -> ev));
    Map<String, EventDataValue> updatedDataValueMap =
        updatedEvent.getEventDataValues().stream()
            .collect(Collectors.toMap(EventDataValue::getDataElement, ev -> ev));

    String updatedDataElementId = "DATAEL00004";
    assertEquals(
        dataValueMap.get(updatedDataElementId).getCreated(),
        updatedDataValueMap.get(updatedDataElementId).getCreated());
    assertEquals("Fourth updated", updatedDataValueMap.get(updatedDataElementId).getValue());
  }

  @Test
  void shouldExportUsersOfDataValuesChangedByAnotherUser() throws IOException, NotFoundException {
    injectSecurityContextUser(userA);
    testSetup.importTrackerData("tracker/event_with_data_values.json");
    User userB = otherUser();
    updateEventAsUserB();

    assertAll(
        () -> assertStoredUsers("DATAEL00002", false, false),
        () -> assertStoredUsers("DATAEL00004", false, true),
        () -> assertStoredUsers("DATAEL00006", true, false));

    Map<String, EventDataValue> byUid = exportDataValues(TrackerIdSchemeParams.builder().build());
    assertAll(
        () -> assertUsers(byUid.get("DATAEL00002"), userA, userA),
        () -> assertUsers(byUid.get("DATAEL00004"), userA, userB),
        () -> assertUsers(byUid.get("DATAEL00006"), userB, userB));

    Map<String, EventDataValue> byName =
        exportDataValues(
            TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.NAME).build());
    assertAll(
        () -> assertUsers(byName.get("test-dataelement2"), userA, userA),
        () -> assertUsers(byName.get("test-dataelement4"), userA, userB),
        () -> assertUsers(byName.get("test-dataelement6"), userB, userB));
  }

  @Test
  void shouldExportUnknownUsersOfDataValuesStoredWithoutUsers()
      throws IOException, NotFoundException {
    injectSecurityContextUser(userA);
    testSetup.importTrackerData("tracker/event_with_data_values.json");
    // as stored by 2.36.0 to 2.36.6 and marked by V2_44_27
    jdbcTemplate.update(
        "update trackerevent set eventdatavalues = jsonb_set(eventdatavalues, '{DATAEL00002}',"
            + " '{\"value\": \"Second\", \"createdByUserInfo\": {}, \"lastUpdatedByUserInfo\":"
            + " {}}') where uid = 'D9PbzJY8bJO'");
    User userB = otherUser();
    updateEventAsUserB();

    Map<String, EventDataValue> byUid = exportDataValues(TrackerIdSchemeParams.builder().build());
    assertAll(
        () -> assertUsers(byUid.get("DATAEL00002"), null, null),
        () -> assertUsers(byUid.get("DATAEL00004"), userA, userB));
  }

  /** User B updates DATAEL00004, adds DATAEL00006 and leaves DATAEL00002 as it is. */
  private void updateEventAsUserB() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/event_with_data_values.json");
    org.hisp.dhis.tracker.imports.domain.TrackerEvent event = trackerObjects.getEvents().get(0);
    event.setDataValues(
        Set.of(
            DataValue.builder()
                .dataElement(MetadataIdentifier.ofUid("DATAEL00002"))
                .value("Second")
                .build(),
            DataValue.builder()
                .dataElement(MetadataIdentifier.ofUid("DATAEL00004"))
                .value("Fourth updated")
                .build(),
            DataValue.builder()
                .dataElement(MetadataIdentifier.ofUid("DATAEL00006"))
                .value("6")
                .build()));
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .build(),
            trackerObjects));
    clearSession();
  }

  private User otherUser() {
    injectAdminIntoSecurityContext();
    User user =
        createAndAddUser(
            CodeGenerator.generateUid(), manager.get(OrganisationUnit.class, "h4w96yEMlzO"), "ALL");
    injectSecurityContextUser(user);
    return user;
  }

  private Map<String, EventDataValue> exportDataValues(TrackerIdSchemeParams idSchemeParams)
      throws NotFoundException {
    return trackerEventService
        .getEvent(UID.of("D9PbzJY8bJO"), idSchemeParams, TrackerEventFields.none())
        .getEventDataValues()
        .stream()
        .collect(Collectors.toMap(EventDataValue::getDataElement, dv -> dv));
  }

  /** Asserts which users of a data value are stored in the eventdatavalues column. */
  private void assertStoredUsers(
      String dataElement, boolean storesCreatedBy, boolean storesLastUpdatedBy) {
    String json =
        jdbcTemplate.queryForObject(
            "select eventdatavalues -> ? from trackerevent where uid = 'D9PbzJY8bJO'",
            String.class,
            dataElement);
    JsonObject value = JsonMixed.of(json).asObject();
    assertAll(
        () -> assertEquals(storesCreatedBy, value.has("createdByUserInfo"), json),
        () -> assertEquals(storesLastUpdatedBy, value.has("lastUpdatedByUserInfo"), json));
  }

  private static void assertUsers(EventDataValue value, User createdBy, User lastUpdatedBy) {
    assertAll(
        () -> assertUser(createdBy, value.getCreatedByUserInfo(), "createdByUserInfo"),
        () -> assertUser(lastUpdatedBy, value.getLastUpdatedByUserInfo(), "lastUpdatedByUserInfo"));
  }

  private static void assertUser(User expected, UserInfoSnapshot actual, String property) {
    if (expected == null) {
      assertNull(actual, property);
      return;
    }
    assertNotNull(actual, property);
    assertAll(
        property,
        () -> assertEquals(expected.getUid(), actual.getUid()),
        () -> assertEquals(expected.getUsername(), actual.getUsername()),
        () -> assertEquals(expected.getFirstName(), actual.getFirstName()),
        () -> assertEquals(expected.getSurname(), actual.getSurname()));
  }
}
