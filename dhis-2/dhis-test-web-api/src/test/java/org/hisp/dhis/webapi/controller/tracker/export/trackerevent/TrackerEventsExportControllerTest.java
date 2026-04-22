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
package org.hisp.dhis.webapi.controller.tracker.export.trackerevent;

import static org.hisp.dhis.http.HttpClientAdapter.Header;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.webapi.Assertions.assertNoDiff;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonDiff;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerEventsExportControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String USER_UID = "tTgjgobT1oS";
  private static final String EVENT_UID = "pTzf9KYMk72";
  private static final String EVENT_UID_2 = "D9PbzJY8bJM";
  private static final String EVENT_UID_3 = "jxgFyJEMUPf";
  private static final String EVENT_UID_4 = "JaRDIvcEcEx";
  private static final String EVENT_UID_5 = "gvULMgNiAfM";
  private static final String EVENT_UID_6 = "gvULMgNiAfN";
  private static final String PROGRAM_UID = "BFcipDERJnf";
  private static final String PROGRAM_STAGE_UID = "NpsdDv6kKSO";
  private static final String ORG_UNIT_UID = "h4w96yEMlzO";
  private static final String TRACKED_ENTITY_UID = "QS6w44flWAf";
  private static final String ENROLLMENT_UID = "nxP7UnKhomJ";
  private static final String DATA_ELEMENT_UID = "GieVkTxp4HH";
  private static final String SINGLE_EVENT_PROGRAM_UID = "iS7eutanDry";
  private static final String SINGLE_EVENT_UID = "QRYjLTiJTrA";

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private FileResourceService fileResourceService;

  @Autowired private FileResourceContentStore fileResourceContentStore;

  @Autowired private TestSetup testSetup;

  private User user;
  private User owner;
  private DataElement dataElement;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    user = userService.getUser(USER_UID);
    owner = userService.getUser("M5zQapPyTZI");

    dataElement = manager.get(DataElement.class, DATA_ELEMENT_UID);

    injectSecurityContextUser(user);
    testSetup.importTrackerData();
  }

  @BeforeEach
  void setupUser() {
    injectSecurityContextUser(user);
  }

  @Test
  void shouldReturnTrackerEventsWhenRequestingWithProgramParameter() {
    JsonMixed content =
        GET("/tracker/trackerEvents?program={program}", PROGRAM_UID).content(HttpStatus.OK);

    JsonList<JsonEvent> events = content.getList("trackerEvents", JsonEvent.class);
    assertFalse(events.isEmpty(), "Should have at least one tracker event");

    JsonEvent event = events.get(0);
    assertHasMember(event, "event");
    assertHasMember(event, "program");
    assertHasMember(event, "programStage");
    assertHasMember(event, "enrollment");
    assertHasMember(event, "orgUnit");
    assertHasMember(event, "status");
  }

  @Test
  void shouldReturnTrackerEventWhenFilteringByEventUid() {
    JsonMixed content =
        GET("/tracker/trackerEvents?program={program}&events={event}", PROGRAM_UID, EVENT_UID)
            .content(HttpStatus.OK);

    JsonList<JsonEvent> events = content.getList("trackerEvents", JsonEvent.class);
    assertHasSize(1, events.stream().toList());
    assertEquals(EVENT_UID, events.get(0).getEvent());
  }

  @Test
  void shouldReturnTrackerEventsWhenFilteringByOrgUnit() {
    JsonMixed content =
        GET("/tracker/trackerEvents?program={program}&orgUnit={orgUnit}", PROGRAM_UID, ORG_UNIT_UID)
            .content(HttpStatus.OK);

    JsonList<JsonEvent> events = content.getList("trackerEvents", JsonEvent.class);
    assertFalse(events.isEmpty());
    events.forEach(
        event -> assertEquals(ORG_UNIT_UID, event.getOrgUnit(), "OrgUnit should match filter"));
  }

  @Test
  void shouldReturnTrackerEventsWhenFilteringByTrackedEntity() {
    JsonMixed content =
        GET(
                "/tracker/trackerEvents?program={program}&trackedEntity={te}",
                PROGRAM_UID,
                TRACKED_ENTITY_UID)
            .content(HttpStatus.OK);

    JsonList<JsonEvent> events = content.getList("trackerEvents", JsonEvent.class);
    assertFalse(events.isEmpty());
  }

  @Test
  void shouldReturnTrackerEventsWhenFilteringByEnrollment() {
    JsonMixed content =
        GET(
                "/tracker/trackerEvents?program={program}&enrollments={enrollment}",
                PROGRAM_UID,
                ENROLLMENT_UID)
            .content(HttpStatus.OK);

    JsonList<JsonEvent> events = content.getList("trackerEvents", JsonEvent.class);
    assertFalse(events.isEmpty());
  }

  @Test
  void shouldReturnFilteredFieldsWhenRequestingWithFieldsParameter() {
    JsonMixed content =
        GET(
                "/tracker/trackerEvents?program={program}&events={event}&fields=event,status",
                PROGRAM_UID,
                EVENT_UID)
            .content(HttpStatus.OK);

    JsonEvent event = content.getList("trackerEvents", JsonEvent.class).get(0);
    assertHasOnlyMembers(event, "event", "status");
    assertEquals(EVENT_UID, event.getEvent());
  }

  @Test
  void shouldReturnEmptyListWhenNoEventsMatchFilters() {
    JsonMixed content =
        GET(
                "/tracker/trackerEvents?program={program}&events={event}",
                PROGRAM_UID,
                CodeGenerator.generateUid())
            .content(HttpStatus.OK);

    JsonList<JsonEvent> events = content.getList("trackerEvents", JsonEvent.class);
    assertTrue(events.isEmpty());
  }

  @Test
  void shouldReturnBadRequestWhenProgramParameterIsMissing() {
    assertEquals(
        "Program is mandatory", GET("/tracker/trackerEvents").error(BAD_REQUEST).getMessage());
  }

  @Test
  void shouldReturnTrackerEventWhenRequestingByUid() {
    JsonEvent event =
        GET("/tracker/trackerEvents/{uid}", EVENT_UID).content(HttpStatus.OK).as(JsonEvent.class);

    assertEquals(EVENT_UID, event.getEvent());
    assertEquals(PROGRAM_UID, event.getProgram());
    assertEquals(PROGRAM_STAGE_UID, event.getProgramStage());
    assertEquals(ORG_UNIT_UID, event.getOrgUnit());
    assertHasMember(event, "enrollment");
    assertHasMember(event, "status");
    assertHasMember(event, "occurredAt");
    assertHasMember(event, "createdAt");
    assertHasMember(event, "updatedAt");
    assertHasMember(event, "dataValues");
    assertHasNoMember(event, "relationships");
  }

  @Test
  void shouldReturnTrackerEventWithDataValuesWhenRequestingByUid() {
    JsonEvent event =
        GET("/tracker/trackerEvents/{uid}?fields=dataValues", EVENT_UID)
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertHasOnlyMembers(event, "dataValues");
    assertFalse(event.getDataValues().isEmpty());

    JsonDataValue dataValue = event.getDataValues().get(0);
    assertHasMember(dataValue, "dataElement");
    assertHasMember(dataValue, "value");
  }

  @Test
  void shouldReturnTrackerEventWithSelectedFieldsWhenRequestingWithFieldsParameter() {
    JsonEvent event =
        GET("/tracker/trackerEvents/{uid}?fields=orgUnit,status", EVENT_UID)
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertHasOnlyMembers(event, "orgUnit", "status");
    assertEquals(ORG_UNIT_UID, event.getOrgUnit());
    assertHasMember(event, "status");
  }

  @Test
  void shouldReturnNotFoundWhenRequestingNonExistentEvent() {
    String nonExistentUid = CodeGenerator.generateUid();
    assertEquals(
        "Event with id " + nonExistentUid + " could not be found.",
        GET("/tracker/trackerEvents/{uid}", nonExistentUid)
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenRequestingSingleEventByUid() {
    assertEquals(
        "Event with id " + SINGLE_EVENT_UID + " could not be found.",
        GET("/tracker/trackerEvents/{uid}", SINGLE_EVENT_UID)
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnNoTrackerEventsWhenProgramIsWithoutRegistration() {
    assertEquals(
        "Program specified is not a tracker program: " + SINGLE_EVENT_PROGRAM_UID,
        GET("/tracker/trackerEvents?program={program}", SINGLE_EVENT_PROGRAM_UID)
            .error(BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldReturnUserInfoWhenEventHasCreatedByAndUpdatedBy() {
    JsonObject jsonEvent = GET("/tracker/trackerEvents/{id}", EVENT_UID).content(HttpStatus.OK);

    assertEquals(user.getUsername(), jsonEvent.getString("createdBy.username").string());
    assertEquals(user.getUsername(), jsonEvent.getString("updatedBy.username").string());

    assertFalse(jsonEvent.getArray("dataValues").isEmpty());
    assertEquals(
        user.getUsername(),
        jsonEvent.getArray("dataValues").getObject(0).getString("createdBy.username").string());
    assertEquals(
        user.getUsername(),
        jsonEvent.getArray("dataValues").getObject(0).getString("updatedBy.username").string());
  }

  @Test
  void shouldReturnFileWhenRequestingDataValueFile() throws ConflictException {
    DataElement de = createDataElementWithValueType(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_3);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    HttpResponse response =
        GET(
            "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, must-revalidate, private", response.header("Cache-Control"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertContains("script-src 'none';", response.header("Content-Security-Policy"));
    assertEquals("file content", response.content("text/plain"));
  }

  @Test
  void shouldReturnFileWhenRequestingDataValueFileAndFileIsAnImage() throws ConflictException {
    DataElement de = createDataElementWithValueType(ValueType.IMAGE);
    FileResource file = storeFile("image/png", "image content");

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_4);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    HttpResponse response =
        GET(
            "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals("image content", response.content("image/png"));
  }

  @Test
  void shouldReturnNotModifiedWhenRequestingFileWithMatchingETag() throws ConflictException {
    DataElement de = createDataElementWithValueType(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_5);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    HttpResponse response =
        GET(
            "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
            event.getUid(),
            de.getUid(),
            Header("If-None-Match", "\"" + file.getUid() + "\""));

    assertEquals(HttpStatus.NOT_MODIFIED, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertContains("script-src 'none';", response.header("Content-Security-Policy"));
    assertFalse(response.hasBody());
  }

  @Test
  void shouldReturnImageWhenRequestingDataValueImage() throws ConflictException {
    DataElement de = createDataElementWithValueType(ValueType.IMAGE);
    FileResource file = storeFile("image/png", "image content");

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_6);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    HttpResponse response =
        GET(
            "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/image",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals("image content", response.content("image/png"));
  }

  @Test
  void shouldGetEventByPathIdenticalToQueryParamWhenRequestingBothWays() {
    JsonEvent pathEvent =
        GET("/tracker/trackerEvents/{id}?fields=*,!relationships", EVENT_UID)
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    JsonMixed content =
        GET(
                "/tracker/trackerEvents?fields=*&events={id}&program={programUid}",
                EVENT_UID,
                PROGRAM_UID)
            .content(HttpStatus.OK);
    JsonList<JsonEvent> queryEvents = content.getList("trackerEvents", JsonEvent.class);

    assertHasSize(1, queryEvents.stream().toList());
    assertNoDiff(pathEvent, queryEvents.get(0), JsonDiff.Mode.LENIENT);
  }

  @Test
  void shouldReturnTrackerEventWithNotesWhenEventHasNotes() {
    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID);

    JsonEvent jsonEvent =
        GET("/tracker/trackerEvents/{uid}?fields=notes", event.getUid())
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertHasOnlyMembers(jsonEvent, "notes");
    assertFalse(jsonEvent.getNotes().isEmpty());
    assertContainsOnly(
        List.of("SGuCABkhpgn", "DRKO4xUVrpr"),
        jsonEvent.getNotes().stream().map(JsonNote::getNote).toList());
    assertContainsOnly(
        List.of("comment value", "comment value"),
        jsonEvent.getNotes().stream().map(JsonNote::getValue).toList());
  }

  @Test
  void shouldReturnBadRequestWhenRequestingFileWithDimensionParameter() {
    assertStartsWith(
        "Request parameter 'dimension'",
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file?dimension=small",
                CodeGenerator.generateUid(),
                CodeGenerator.generateUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenRequestingFileForNonExistentDataElement() {
    String deUid = CodeGenerator.generateUid();

    assertStartsWith(
        "DataElement with id " + deUid,
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
                EVENT_UID_6,
                deUid)
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenRequestingFileForDataElementThatIsNotAFile() {
    assertStartsWith(
        "Data element " + dataElement.getUid() + " is not a file",
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
                EVENT_UID,
                dataElement.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenNoDataValueExists() {
    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_2);
    DataElement de = createDataElementWithValueType(ValueType.FILE_RESOURCE);

    assertEquals(
        "Event " + event.getUid() + " with data element " + de.getUid() + " could not be found.",
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenDataValueIsNull() {
    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_3);
    DataElement de = createDataElementWithValueType(ValueType.IMAGE);

    event.getEventDataValues().add(createDataValue(de, null));
    manager.update(event);
    manager.flush();

    assertEquals(
        "DataValue for data element " + de.getUid() + " could not be found.",
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnConflictWhenFileResourceInDbButNotInStore() {
    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_4);
    DataElement de = createDataElementWithValueType(ValueType.FILE_RESOURCE);

    FileResource file = createFileResource('A', "file content".getBytes());
    manager.save(file, false);

    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    assertStartsWith(
        "Content is being processed and is not available yet, try again later",
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.CONFLICT)
            .getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenFileResourceDoesNotExist() {
    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_5);
    DataElement de = createDataElementWithValueType(ValueType.FILE_RESOURCE);
    String fileUid = CodeGenerator.generateUid();

    event.getEventDataValues().add(createDataValue(de, fileUid));
    manager.update(event);
    manager.flush();

    assertStartsWith(
        "FileResource with id " + fileUid,
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void shouldReturnSmallImageWhenRequestingWithDimensionParameter(@TempDir Path tempDir)
      throws ConflictException, IOException {
    DataElement de = createDataElementWithValueType(ValueType.IMAGE);

    FileResource file = storeFile("image/png", "original image");
    file.setHasMultipleStorageFiles(true);
    manager.update(file);

    Path smallImage = tempDir.resolve("small.png");
    String smallFileContent = "small file";
    Files.writeString(smallImage, smallFileContent);
    fileResourceContentStore.saveFileResourceContent(
        file, Map.of(ImageFileDimension.SMALL, smallImage.toFile()));

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_2);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    HttpResponse response =
        GET(
            "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/image?dimension=small",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals(
        Long.toString(smallFileContent.getBytes().length), response.header("Content-Length"));
    assertEquals(smallFileContent, response.content("image/png"));
  }

  @Test
  void shouldReturnBadRequestWhenRequestingImageWithInvalidDimension() throws ConflictException {
    DataElement de = createDataElementWithValueType(ValueType.IMAGE);
    FileResource file = storeFile("image/png", "image content");

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_3);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    String message =
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/image?dimension=tiny",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage();

    assertStartsWith("Value 'tiny' is not valid", message);
  }

  @Test
  void shouldReturnBadRequestWhenRequestingImageForNonImageDataElement() throws ConflictException {
    DataElement de = createDataElementWithValueType(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_4);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    String message =
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/image",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage();

    assertStartsWith("File is not an image", message);
  }

  @Test
  void shouldReturnBadRequestWhenRequestingDimensionForImageWithoutMultipleFiles(
      @TempDir Path tempDir) throws ConflictException, IOException {
    DataElement de = createDataElementWithValueType(ValueType.IMAGE);

    FileResource file = storeFile("image/png", "original image");
    file.setHasMultipleStorageFiles(false);
    manager.update(file);

    Path smallImage = tempDir.resolve("small.png");
    Files.writeString(smallImage, "small file");
    fileResourceContentStore.saveFileResourceContent(
        file, Map.of(ImageFileDimension.SMALL, smallImage.toFile()));

    TrackerEvent event = manager.get(TrackerEvent.class, EVENT_UID_5);
    event.getEventDataValues().add(createDataValue(de, file.getUid()));
    manager.update(event);
    manager.flush();

    String message =
        GET(
                "/tracker/trackerEvents/{eventUid}/dataValues/{dataElementUid}/image?dimension=small",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage();

    assertStartsWith("Image is not stored using multiple dimensions", message);
  }

  private DataElement createDataElementWithValueType(ValueType type) {
    DataElement result = createDataElement('B');
    result.setValueType(type);
    result.getSharing().setOwner(owner);
    manager.save(result, false);
    return result;
  }

  private EventDataValue createDataValue(DataElement de, String value) {
    EventDataValue result = new EventDataValue();
    result.setDataElement(de.getUid());
    result.setValue(value);
    return result;
  }

  static Stream<Arguments> callEventsEndpoint() {
    return Stream.of(
        arguments(
            ".json.zip",
            "application/json+zip",
            "attachment; filename=trackerEvents.json.zip",
            "binary"),
        arguments(
            ".json.gz",
            "application/json+gzip",
            "attachment; filename=trackerEvents.json.gz",
            "binary"),
        arguments(
            ".csv",
            "application/csv; charset=UTF-8",
            "attachment; filename=trackerEvents.csv",
            null),
        arguments(
            ".csv.gz",
            "application/csv+gzip",
            "attachment; filename=trackerEvents.csv.gz",
            "binary"),
        arguments(
            ".csv.zip",
            "application/csv+zip",
            "attachment; filename=trackerEvents.csv.zip",
            "binary"));
  }

  @ParameterizedTest
  @MethodSource("callEventsEndpoint")
  void
      shouldMatchContentTypeAndAttachment_whenEndpointForCompressedEventJsonIsInvokedForTrackerEvent(
          String extension,
          String expectedContentType,
          String expectedAttachment,
          String encoding) {
    HttpResponse res = GET("/tracker/trackerEvents" + extension + "?program=" + PROGRAM_UID);

    assertEquals(HttpStatus.OK, res.status());
    assertEquals(expectedContentType, res.header("Content-Type"));
    assertEquals(expectedAttachment, res.header(ContextUtils.HEADER_CONTENT_DISPOSITION));
    assertEquals(encoding, res.header(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING));
    assertNotNull(res.content(expectedContentType));
  }

  private FileResource storeFile(String contentType, String content) throws ConflictException {
    byte[] data = content.getBytes();
    FileResource fr = createFileResource('A', data);
    fr.setContentType(contentType);
    fileResourceService.syncSaveFileResource(fr, data);
    fr.setStorageStatus(FileResourceStorageStatus.STORED);
    return fr;
  }
}
