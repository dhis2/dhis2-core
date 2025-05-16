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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertUser;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests tracker exporter idScheme support. */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdSchemeExportControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String METADATA_ATTRIBUTE = "j45AR9cBQKc";
  private static final String UNUSED_METADATA_ATTRIBUTE = "i57a0734128";

  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    // ensure these are created in the setup
    get(Attribute.class, METADATA_ATTRIBUTE);
    get(Attribute.class, UNUSED_METADATA_ATTRIBUTE);

    manager.flush();
    manager.clear();
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @ParameterizedTest
  @MethodSource(value = "shouldExportMetadataUsingGivenIdSchemeProvider")
  void shouldExportEventMetadataUsingGivenIdScheme(TrackerIdSchemeParam idSchemeParam) {
    Event event = get(Event.class, "QRYjLTiJTrA");
    assertNotEmpty(event.getEventDataValues(), "test expects an event with data values");

    List<String> idSchemeRequestParams =
        List.of(
            "orgUnit",
            "program",
            "programStage",
            "categoryOptionCombo",
            "categoryOption",
            "dataElement");
    String idSchemes =
        idSchemeRequestParams.stream()
            .map(p -> p + "IdScheme=" + idSchemeParam)
            .collect(Collectors.joining("&"));

    JsonEvent actual =
        GET(
                "/tracker/events/{id}?fields=orgUnit,program,programStage,attributeOptionCombo,attributeCategoryOptions,dataValues&{idSchemes}",
                event.getUid(),
                idSchemes)
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertAll(
        "event metadata assertions for idScheme=" + idSchemeParam,
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(event.getOrganisationUnit()),
                actual,
                idSchemeParam,
                "orgUnit"),
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(event.getProgramStage().getProgram()),
                actual,
                idSchemeParam,
                "program"),
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(event.getProgramStage()),
                actual,
                idSchemeParam,
                "programStage"),
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(event.getAttributeOptionCombo()),
                actual,
                idSchemeParam,
                "attributeOptionCombo"),
        () -> {
          String field = "attributeCategoryOptions";
          List<String> expected =
              event.getAttributeOptionCombo().getCategoryOptions().stream()
                  .map(co -> idSchemeParam.getIdentifier(co))
                  .toList();
          assertNotEmpty(
              expected,
              String.format(
                  "metadata corresponding to field \"%s\" has no value in test data for"
                      + " idScheme '%s'",
                  field, idSchemeParam));
          assertTrue(
              actual.has(field),
              () ->
                  String.format(
                      "field \"%s\" is not in response %s for idScheme '%s'",
                      field, actual, idSchemeParam));
          assertContainsOnly(expected, Arrays.asList(actual.getString(field).string().split(",")));
        },
        () -> assertDataValues(actual, event, idSchemeParam));
  }

  @Test
  void shouldExportEventUsingNonUIDDataElementIdSchemeEvenIfItHasNoDataValues() {
    Event event = get(Event.class, "jxgFyJEMUPf");
    assertIsEmpty(event.getEventDataValues(), "test expects an event with no data values");

    JsonEvent actual =
        GET("/tracker/events/{id}?fields=event,dataValues&dataElementIdScheme=NAME", event.getUid())
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertEquals("jxgFyJEMUPf", actual.getEvent());
  }

  @Test
  void shouldExportEventUsingNonUIDDataElementIdSchemeIfItHasRelationships() {
    Event event = get(Event.class, "pTzf9KYMk72");
    assertNotEmpty(event.getRelationshipItems(), "test expects an event with relationships");

    JsonEvent actual =
        GET(
                "/tracker/events/{id}?fields=event,relationships&dataElementIdScheme=NAME",
                event.getUid())
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertEquals("pTzf9KYMk72", actual.getEvent());
  }

  @Test
  void shouldExportEventsUsingNonUIDDataElementIdScheme() {
    Event event1 = get(Event.class, "QRYjLTiJTrA");
    Event event2 = get(Event.class, "kWjSezkXHVp");
    assertNotEmpty(
        CollectionUtils.intersection(
            event1.getEventDataValues().stream()
                .map(EventDataValue::getDataElement)
                .collect(Collectors.toSet()),
            event2.getEventDataValues().stream()
                .map(EventDataValue::getDataElement)
                .collect(Collectors.toSet())),
        "test expects both events to have at least one data value for the same data element");

    JsonList<JsonEvent> jsonEvents =
        GET("/tracker/events?events=QRYjLTiJTrA,kWjSezkXHVp&program=iS7eutanDry&fields=event,dataValues&dataElementIdScheme=NAME")
            .content(HttpStatus.OK)
            .getList("events", JsonEvent.class);

    Map<String, JsonEvent> events =
        jsonEvents.stream().collect(Collectors.toMap(JsonEvent::getEvent, Function.identity()));
    assertContainsOnly(List.of(event1.getUid(), event2.getUid()), events.keySet());

    TrackerIdSchemeParam idSchemeParam = TrackerIdSchemeParam.NAME;
    assertAll(
        () -> assertDataValues(events.get("QRYjLTiJTrA"), event1, idSchemeParam),
        () -> assertDataValues(events.get("kWjSezkXHVp"), event2, idSchemeParam));
  }

  @Test
  void shouldExportEventDataValuesEquallyWithIdSchemeUIDAndName() {
    // ensure the event data value JSON is identical when idScheme=UID than other idSchemes as
    // different code is used to map it due to it being stored as JSONB
    Event event = get(Event.class, "QRYjLTiJTrA");
    assertNotEmpty(event.getEventDataValues(), "test expects an event with data values");
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = get(DataElement.class, dataElementUid);

    JsonEvent uidJson =
        GET("/tracker/events/QRYjLTiJTrA?fields=event,dataValues&dataElementIdScheme=UID")
            .content(HttpStatus.OK)
            .as(JsonEvent.class);
    JsonEvent nameJson =
        GET("/tracker/events/QRYjLTiJTrA?fields=event,dataValues&dataElementIdScheme=NAME")
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    JsonDataValue uidDataValue =
        uidJson.getDataValues().stream()
            .filter(dv -> dataElementUid.equals(dv.getDataElement()))
            .findFirst()
            .orElse(null);
    assertNotNull(uidDataValue, "event should have dataValues");
    JsonDataValue nameDataValue =
        nameJson.getDataValues().stream()
            .filter(dv -> dataElement.getName().equals(dv.getDataElement()))
            .findFirst()
            .orElse(null);
    assertNotNull(nameDataValue, "event should have dataValues");
    // dataElement is asserted in other tests
    assertAll(
        "assert dataValue fields",
        () -> assertEquals(uidDataValue.getValue(), nameDataValue.getValue(), "value"),
        () -> assertEquals(uidDataValue.getCreatedAt(), nameDataValue.getCreatedAt(), "createdAt"),
        () -> assertEquals(uidDataValue.getUpdatedAt(), nameDataValue.getUpdatedAt(), "updatedAt"),
        () -> assertEquals(uidDataValue.getStoredBy(), nameDataValue.getStoredBy(), "storedBy"),
        () ->
            assertEquals(
                uidDataValue.getProvidedElsewhere(),
                nameDataValue.getProvidedElsewhere(),
                "providedElsewhere"),
        () -> assertUser(uidDataValue.getCreatedBy(), nameDataValue.getCreatedBy(), "createdBy"),
        () -> assertUser(uidDataValue.getUpdatedBy(), nameDataValue.getUpdatedBy(), "updatedBy"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/{id}?",
        "?events={id}&program=iS7eutanDry&paging=true&",
        "?events={id}&program=iS7eutanDry&paging=false&"
      })
  void shouldReportEventMetadataWhichDoesNotHaveAnIdentifierForGivenIdScheme(String urlPortion) {
    Event event = get(Event.class, "QRYjLTiJTrA");

    JsonWebMessage msg =
        assertWebMessage(
            HttpStatus.UNPROCESSABLE_ENTITY,
            GET(
                "/tracker/events"
                    + urlPortion
                    + "fields=orgUnit,program,programStage,attributeOptionCombo,attributeCategoryOptions,dataValues&idScheme=ATTRIBUTE:{attribute}",
                event.getUid(),
                UNUSED_METADATA_ATTRIBUTE));

    assertAll(
        () ->
            assertContains(
                "Not all metadata has an identifier for the requested idScheme", msg.getMessage()),
        () ->
            assertContains(
                "Program[ATTRIBUTE:" + UNUSED_METADATA_ATTRIBUTE + "]", msg.getDevMessage()));
  }

  @ParameterizedTest
  @MethodSource(value = "shouldExportMetadataUsingGivenIdSchemeProvider")
  void shouldExportTrackedEntityMetadataUsingGivenIdScheme(TrackerIdSchemeParam idSchemeParam) {
    TrackedEntity trackedEntity = get(TrackedEntity.class, "dUE514NMOlo");
    assertNotEmpty(
        trackedEntity.getTrackedEntityAttributeValues(),
        "test expects a tracked entity with attribute values");

    List<String> idSchemeRequestParams = List.of("orgUnit");
    String idSchemes =
        idSchemeRequestParams.stream()
            .map(p -> p + "IdScheme=" + idSchemeParam)
            .collect(Collectors.joining("&"));

    JsonTrackedEntity actual =
        GET(
                "/tracker/trackedEntities/{id}?fields=trackedEntity,trackedEntityType,orgUnit,attributes&{idSchemes}&idScheme={idScheme}",
                trackedEntity.getUid(),
                idSchemes,
                idSchemeParam.toString())
            .content(HttpStatus.OK)
            .as(JsonTrackedEntity.class);

    assertAll(
        "tracked entity metadata assertions for idScheme=" + idSchemeParam,
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(trackedEntity.getOrganisationUnit()),
                actual,
                idSchemeParam,
                "orgUnit"),
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(trackedEntity.getTrackedEntityType()),
                actual,
                idSchemeParam,
                "trackedEntityType"),
        () -> assertAttributes(actual, trackedEntity, idSchemeParam));
  }

  @ParameterizedTest
  @MethodSource(value = "shouldExportMetadataUsingGivenIdSchemeProvider")
  void shouldExportTrackedEntitiesMetadataUsingGivenIdScheme(TrackerIdSchemeParam idSchemeParam) {
    TrackedEntity trackedEntity = get(TrackedEntity.class, "dUE514NMOlo");
    assertNotEmpty(
        trackedEntity.getTrackedEntityAttributeValues(),
        "test expects a tracked entity with attribute values");

    List<String> idSchemeRequestParams = List.of("orgUnit");
    String idSchemes =
        idSchemeRequestParams.stream()
            .map(p -> p + "IdScheme=" + idSchemeParam)
            .collect(Collectors.joining("&"));

    JsonList<JsonTrackedEntity> jsonTrackedEntities =
        GET(
                "/tracker/trackedEntities?trackedEntities={id}&fields=trackedEntity,trackedEntityType,orgUnit,attributes&{idSchemes}&idScheme={idScheme}",
                trackedEntity.getUid(),
                idSchemes,
                idSchemeParam.toString())
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    JsonTrackedEntity actual =
        jsonTrackedEntities.first(te -> trackedEntity.getUid().equals(te.getTrackedEntity()));

    assertAll(
        "tracked entity metadata assertions for idScheme=" + idSchemeParam,
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(trackedEntity.getOrganisationUnit()),
                actual,
                idSchemeParam,
                "orgUnit"),
        () ->
            assertIdScheme(
                idSchemeParam.getIdentifier(trackedEntity.getTrackedEntityType()),
                actual,
                idSchemeParam,
                "trackedEntityType"),
        () -> assertAttributes(actual, trackedEntity, idSchemeParam));
  }

  public static Stream<TrackerIdSchemeParam> shouldExportMetadataUsingGivenIdSchemeProvider() {
    return Stream.of(
        TrackerIdSchemeParam.UID,
        TrackerIdSchemeParam.CODE,
        TrackerIdSchemeParam.NAME,
        TrackerIdSchemeParam.ofAttribute(METADATA_ATTRIBUTE));
  }

  private static void assertIdScheme(
      String expected, JsonObject actual, TrackerIdSchemeParam idSchemeParam, String field) {
    assertNotEmpty(
        expected,
        String.format(
            "metadata corresponding to field \"%s\" has no value in test data for idScheme '%s'",
            field, idSchemeParam));
    assertTrue(
        actual.has(field),
        () ->
            String.format(
                "field \"%s\" is not in response %s for idScheme '%s'",
                field, actual, idSchemeParam));
    assertEquals(
        expected,
        actual.getString(field).string(),
        () ->
            String.format(
                "field \"%s\" does not have required idScheme '%s' in response",
                field, idSchemeParam));
  }

  private void assertDataValues(
      JsonEvent actual, Event expected, TrackerIdSchemeParam idSchemeParam) {
    String field = "dataValues";
    List<String> expectedDataElement =
        expected.getEventDataValues().stream()
            .map(dv -> idSchemeParam.getIdentifier(get(DataElement.class, dv.getDataElement())))
            .toList();
    assertNotEmpty(
        expectedDataElement,
        String.format(
            "metadata corresponding to field \"%s\" has no value in test data for"
                + " idScheme '%s'",
            field, idSchemeParam));
    assertTrue(
        actual.has(field),
        () ->
            String.format(
                "field \"%s\" is not in response %s for idScheme '%s'",
                field, actual, idSchemeParam));
    List<String> actualDataElement =
        actual
            .getList(field, JsonObject.class)
            .toList(el -> el.getString("dataElement").string(""));
    assertContainsOnly(
        expectedDataElement,
        actualDataElement,
        "mismatch in data elements of event " + expected.getUid());
  }

  private void assertAttributes(
      JsonTrackedEntity actual, TrackedEntity expected, TrackerIdSchemeParam idSchemeParam) {
    String field = "attributes";

    Set<String> tetAttributes =
        expected.getTrackedEntityType().getTrackedEntityTypeAttributes().stream()
            .map(teta -> teta.getTrackedEntityAttribute().getUid())
            .collect(Collectors.toSet());
    // this assumes the request was made without the request parameter program
    List<String> expectedAttributes =
        expected.getTrackedEntityAttributeValues().stream()
            .filter(teav -> tetAttributes.contains(teav.getAttribute().getUid()))
            .map(
                tav ->
                    idSchemeParam.getIdentifier(
                        get(TrackedEntityAttribute.class, tav.getAttribute().getUid())))
            .toList();
    assertNotEmpty(
        expectedAttributes,
        String.format(
            "metadata corresponding to field \"%s\" has no value in test data for"
                + " idScheme '%s'",
            field, idSchemeParam));
    assertFalse(
        expectedAttributes.contains(null),
        String.format(
            "metadata corresponding to field \"%s\" contains null value in test data for idScheme '%s'",
            field, idSchemeParam));
    assertTrue(
        actual.has(field),
        () ->
            String.format(
                "field \"%s\" is not in response %s for idScheme '%s'",
                field, actual, idSchemeParam));
    List<String> actualAttributes =
        actual.getList(field, JsonObject.class).toList(el -> el.getString("attribute").string(""));
    assertContainsOnly(
        expectedAttributes,
        actualAttributes,
        "mismatch in attributes of tracked entity " + expected.getUid());
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
}
