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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.http.HttpClientAdapter.Header;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
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
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EventsExportControllerByIdTest extends H2ControllerIntegrationTestBase {
  private static final String DATA_ELEMENT_VALUE = "value";

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private FileResourceService fileResourceService;

  @Autowired private FileResourceContentStore fileResourceContentStore;

  @Autowired private CategoryService categoryService;

  private CategoryOptionCombo coc;

  private OrganisationUnit orgUnit;

  private OrganisationUnit anotherOrgUnit;

  private Program program;

  private ProgramStage programStage;

  private User owner;

  private User user;

  private TrackedEntityType trackedEntityType;

  private EventDataValue dv;

  private DataElement de;

  @BeforeEach
  void setUp() {
    owner = makeUser("owner");

    coc = categoryService.getDefaultCategoryOptionCombo();

    orgUnit = createOrganisationUnit('A');
    orgUnit.getSharing().setOwner(owner);
    manager.save(orgUnit, false);

    anotherOrgUnit = createOrganisationUnit('B');
    anotherOrgUnit.getSharing().setOwner(owner);
    manager.save(anotherOrgUnit, false);

    user = createUserWithId("tester", CodeGenerator.generateUid());
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    this.userService.updateUser(user);

    trackedEntityType = trackedEntityTypeAccessible();

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(owner);
    program.getSharing().addUserAccess(userAccess());
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    de = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    de.getSharing().setOwner(owner);
    manager.save(de, false);

    programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(owner);
    programStage.getSharing().addUserAccess(userAccess());
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(programStage, de, 1, false);
    programStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElement));
    manager.save(programStage, false);

    dv = new EventDataValue();
    dv.setDataElement(de.getUid());
    dv.setStoredBy("user");
    dv.setValue(DATA_ELEMENT_VALUE);
  }

  @Test
  void getEventById() {
    Event event = event(enrollment(trackedEntity()));

    JsonEvent json =
        GET("/tracker/events/{id}", event.getUid()).content(HttpStatus.OK).as(JsonEvent.class);

    assertDefaultResponse(json, event);
  }

  @Test
  void getEventByIdWithFields() {
    Event event = event(enrollment(trackedEntity()));

    JsonEvent jsonEvent =
        GET("/tracker/events/{id}?fields=orgUnit,status", event.getUid())
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertHasOnlyMembers(jsonEvent, "orgUnit", "status");
    assertEquals(event.getOrganisationUnit().getUid(), jsonEvent.getOrgUnit());
    assertEquals(event.getStatus().toString(), jsonEvent.getStatus());
  }

  @Test
  void getEventByIdWithNotes() {
    Event event = event(enrollment(trackedEntity()));
    event.setNotes(List.of(note("oqXG28h988k", "my notes", owner.getUid())));
    manager.update(event);

    JsonEvent jsonEvent =
        GET("/tracker/events/{uid}?fields=notes", event.getUid())
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    JsonNote note = jsonEvent.getNotes().get(0);
    assertEquals("oqXG28h988k", note.getNote());
    assertEquals("my notes", note.getValue());
    assertEquals(owner.getUid(), note.getStoredBy());
  }

  @Test
  void getEventByIdWithDataValues() {
    Event event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dv);
    manager.update(event);

    JsonEvent eventJson =
        GET("/tracker/events/{id}?fields=dataValues", event.getUid())
            .content(HttpStatus.OK)
            .as(JsonEvent.class);

    assertHasOnlyMembers(eventJson, "dataValues");
    JsonDataValue dataValue = eventJson.getDataValues().get(0);
    assertEquals(de.getUid(), dataValue.getDataElement());
    assertEquals(dv.getValue(), dataValue.getValue());
    assertHasMember(dataValue, "createdAt");
    assertHasMember(dataValue, "updatedAt");
    assertHasMember(dataValue, "storedBy");
  }

  @Test
  void getEventByIdWithFieldsRelationships() {
    TrackedEntity to = trackedEntity();
    Event from = event(enrollment(to));
    Relationship relationship = relationship(from, to);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/events/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = relationships.get(0);
    assertEquals(relationship.getUid(), jsonRelationship.getRelationship());

    JsonRelationshipItem.JsonEvent event = jsonRelationship.getFrom().getEvent();
    assertEquals(relationship.getFrom().getEvent().getUid(), event.getEvent());
    assertEquals(relationship.getFrom().getEvent().getEnrollment().getUid(), event.getEnrollment());

    JsonRelationshipItem.JsonTrackedEntity trackedEntity =
        jsonRelationship.getTo().getTrackedEntity();
    assertEquals(
        relationship.getTo().getTrackedEntity().getUid(), trackedEntity.getTrackedEntity());

    assertHasMember(jsonRelationship, "relationshipName");
    assertHasMember(jsonRelationship, "relationshipType");
    assertHasMember(jsonRelationship, "createdAt");
    assertHasMember(jsonRelationship, "updatedAt");
    assertHasMember(jsonRelationship, "bidirectional");
  }

  @Test
  void getEventByIdRelationshipsNoAccessToRelationshipType() {
    TrackedEntity to = trackedEntity();
    Event from = event(enrollment(to));
    relationship(relationshipTypeNotAccessible(), from, to);
    this.switchContextToUser(user);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/events/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(0, relationships.size());
  }

  @Test
  void getEventByIdRelationshipsNoAccessToRelationshipItemTo() {
    TrackedEntityType type = trackedEntityTypeNotAccessible();
    TrackedEntity to = trackedEntity(type);
    Event from = event(enrollment(to));
    relationship(from, to);
    this.switchContextToUser(user);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/events/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(0, relationships.size());
  }

  @Test
  void getEventByIdRelationshipsNoAccessToBothRelationshipItems() {
    TrackedEntity to = trackedEntityNotInSearchScope();
    Event from = event(enrollment(to));
    relationship(from, to);
    this.switchContextToUser(user);

    assertTrue(
        GET("/tracker/events/{id}", from.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage()
            .contains("OWNERSHIP_ACCESS_DENIED"));
  }

  @Test
  void getEventByIdRelationshipsNoAccessToRelationshipItemFrom() {
    TrackedEntityType type = trackedEntityTypeNotAccessible();
    TrackedEntity from = trackedEntity(type);
    Event to = event(enrollment(from));
    relationship(from, to);
    this.switchContextToUser(user);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/events/{id}?fields=relationships", to.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(0, relationships.size());
  }

  @Test
  void getEventByIdContainsCreatedByAndUpdateByAndAssignedUserInDataValues() {
    TrackedEntity te = trackedEntity();
    Enrollment enrollment = enrollment(te);
    Event event = event(enrollment);
    event.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    event.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    event.setAssignedUser(user);
    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setValue("6");

    eventDataValue.setDataElement(de.getUid());
    eventDataValue.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    eventDataValue.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    Set<EventDataValue> eventDataValues = Set.of(eventDataValue);
    event.setEventDataValues(eventDataValues);
    manager.save(event);

    JsonObject jsonEvent = GET("/tracker/events/{id}", event.getUid()).content(HttpStatus.OK);

    assertTrue(jsonEvent.isObject());
    assertFalse(jsonEvent.isEmpty());
    assertEquals(event.getUid(), jsonEvent.getString("event").string());
    assertEquals(enrollment.getUid(), jsonEvent.getString("enrollment").string());
    assertEquals(orgUnit.getUid(), jsonEvent.getString("orgUnit").string());
    assertEquals(user.getUsername(), jsonEvent.getString("createdBy.username").string());
    assertEquals(user.getUsername(), jsonEvent.getString("updatedBy.username").string());
    assertEquals(user.getDisplayName(), jsonEvent.getString("assignedUser.displayName").string());
    assertFalse(jsonEvent.getArray("dataValues").isEmpty());
    assertEquals(
        user.getUsername(),
        jsonEvent.getArray("dataValues").getObject(0).getString("createdBy.username").string());
    assertEquals(
        user.getUsername(),
        jsonEvent.getArray("dataValues").getObject(0).getString("updatedBy.username").string());
  }

  @Test
  void getEventByIdNotFound() {
    assertEquals(
        "Event with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/events/Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void getDataValuesFileByDataElement() throws ConflictException {
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    Event event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    HttpResponse response =
        GET(
            "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertContains("script-src 'none';", response.header("Content-Security-Policy"));
    assertEquals("file content", response.content("text/plain"));
  }

  @Test
  void getDataValuesFileByDataElementIfFileIsAnImage() throws ConflictException {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.IMAGE);
    FileResource file = storeFile("image/png", "file content");

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    HttpResponse response =
        GET(
            "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("file content", response.content("image/png"));
  }

  @Test
  void getDataValuesFileByDataElementShouldReturnNotModified() throws ConflictException {
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    Event event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    HttpResponse response =
        GET(
            "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file",
            event.getUid(),
            de.getUid(),
            Header("If-None-Match", "\"" + file.getUid() + "\""));

    assertEquals(HttpStatus.NOT_MODIFIED, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertContains("script-src 'none';", response.header("Content-Security-Policy"));
    assertFalse(response.hasBody());
  }

  @Test
  void getDataValuesFileByDataElementIfGivenDimensionParameter() {
    assertStartsWith(
        "Request parameter 'dimension'",
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file?dimension=small",
                CodeGenerator.generateUid(),
                CodeGenerator.generateUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void getDataValuesFileByDataElementIfDataElementIsNotFound() {
    Event event = event(enrollment(trackedEntity()));

    String deUid = CodeGenerator.generateUid();

    assertStartsWith(
        "DataElement with id " + deUid,
        GET("/tracker/events/{eventUid}/dataValues/{dataElementUid}/file", event.getUid(), deUid)
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getDataValuesFileByDataElementIfUserDoesNotHaveReadAccessToDataElement()
      throws ConflictException {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    // remove public access
    de.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(de, false);
    FileResource file = storeFile("text/plain", "file content");

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    this.switchContextToUser(user);

    GET("/tracker/events/{eventUid}/dataValues/{dataElementUid}/file", event.getUid(), de.getUid())
        .error(HttpStatus.NOT_FOUND);
  }

  @Test
  void getDataValuesFileByDataElementIfDataElementIsNotAFile() {
    DataElement de = dataElement(ValueType.BOOLEAN);
    Event event = event(enrollment(trackedEntity()));

    event.getEventDataValues().add(dataValue(de, "true"));
    manager.update(event);

    assertStartsWith(
        "Data element " + de.getUid() + " is not a file",
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getDataValuesFileByDataElementIfNoDataValueExists() {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.FILE_RESOURCE);

    assertEquals(
        "DataValue for data element " + de.getUid() + " could not be found.",
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getDataValuesFileByDataElementIfFileResourceIsInDbButNotInTheStore() {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    FileResource file = createFileResource('A', "file content".getBytes());
    manager.save(file, false);

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    GET("/tracker/events/{eventUid}/dataValues/{dataElementUid}/file", event.getUid(), de.getUid())
        .error(HttpStatus.CONFLICT);
  }

  @Test
  void getDataValuesFileByDataElementIfFileIsNotFound() {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    String fileUid = CodeGenerator.generateUid();

    event.getEventDataValues().add(dataValue(de, fileUid));
    manager.update(event);

    assertStartsWith(
        "FileResource with id " + fileUid,
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/file",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getDataValuesFileByDataElementIfUserDoesNotHaveReadAccessToTrackedentity()
      throws ConflictException {
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    Event event = event(enrollment(trackedEntityNotInSearchScope()));
    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    this.switchContextToUser(user);

    GET("/tracker/events/{eventUid}/dataValues/{dataElementUid}/file", event.getUid(), de.getUid())
        .error(HttpStatus.FORBIDDEN);
  }

  @Test
  void getDataValuesImageByDataElement() throws ConflictException {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.IMAGE);
    FileResource file = storeFile("image/png", "file content");

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    HttpResponse response =
        GET(
            "/tracker/events/{eventUid}/dataValues/{dataElementUid}/image",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("file content", response.content("image/png"));
  }

  @Test
  void getDataValuesImageByDataElementUsingAnotherDimension(@TempDir Path tempDir)
      throws ConflictException, IOException {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.IMAGE);
    // simulating the work of the ImageResizingJob
    // original "image"
    FileResource file = storeFile("image/png", "original image");
    file.setHasMultipleStorageFiles(true);
    manager.update(file);
    // small "image"
    Path smallImage = tempDir.resolve("small.png");
    String smallFileContent = "small file";
    Files.writeString(smallImage, smallFileContent);
    fileResourceContentStore.saveFileResourceContent(
        file, Map.of(ImageFileDimension.SMALL, smallImage.toFile()));

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    HttpResponse response =
        GET(
            "/tracker/events/{eventUid}/dataValues/{dataElementUid}/image?dimension=small",
            event.getUid(),
            de.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals(
        Long.toString(smallFileContent.getBytes().length), response.header("Content-Length"));
    assertEquals(smallFileContent, response.content("image/png"));
  }

  @Test
  void getDataValuesImageByDataElementWithInvalidDimension() throws ConflictException {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.IMAGE);
    FileResource file = storeFile("image/png", "file content");

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    String message =
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/image?dimension=tiny",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage();

    assertStartsWith("Value 'tiny' is not valid", message);
  }

  @Test
  void getDataValuesImageByDataElementIfDataElementIsNotAnImage() throws ConflictException {
    DataElement de = dataElement(ValueType.FILE_RESOURCE);
    FileResource file = storeFile("text/plain", "file content");

    Event event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    String message =
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/image",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage();

    assertStartsWith("File is not an image", message);
  }

  @Test
  void getDataValuesImageByDataElementUsingAnotherDimensionIfDoesNotHaveMultipleStoredFiles(
      @TempDir Path tempDir) throws ConflictException, IOException {
    Event event = event(enrollment(trackedEntity()));
    DataElement de = dataElement(ValueType.IMAGE);
    // simulating the work of the ImageResizingJob
    // original "image"
    FileResource file = storeFile("image/png", "original image");
    file.setHasMultipleStorageFiles(false);
    manager.update(file);
    // small "image"
    Path smallImage = tempDir.resolve("small.png");
    String smallFileContent = "small file";
    Files.writeString(smallImage, smallFileContent);
    fileResourceContentStore.saveFileResourceContent(
        file, Map.of(ImageFileDimension.SMALL, smallImage.toFile()));

    event.getEventDataValues().add(dataValue(de, file.getUid()));
    manager.update(event);

    String message =
        GET(
                "/tracker/events/{eventUid}/dataValues/{dataElementUid}/image?dimension=small",
                event.getUid(),
                de.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage();

    assertStartsWith("Image is not stored using multiple dimensions", message);
  }

  private FileResource storeFile(String contentType, String content) throws ConflictException {
    byte[] data = content.getBytes();
    FileResource fr = createFileResource('A', data);
    fr.setContentType(contentType);
    fileResourceService.syncSaveFileResource(fr, data);
    fr.setStorageStatus(FileResourceStorageStatus.STORED);
    return fr;
  }

  private DataElement dataElement(ValueType type) {
    DataElement de = createDataElement('B');
    de.setValueType(type);
    de.getSharing().setOwner(owner);
    manager.save(de, false);
    return de;
  }

  private EventDataValue dataValue(DataElement de, String value) {
    EventDataValue dv = new EventDataValue();
    dv.setDataElement(de.getUid());
    dv.setValue(value);
    return dv;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().addUserAccess(userAccess());
    manager.save(type, false);
    return type;
  }

  private TrackedEntityType trackedEntityTypeNotAccessible() {
    TrackedEntityType type = trackedEntityType('B');
    manager.save(type, false);
    return type;
  }

  private TrackedEntityType trackedEntityType(char uniqueChar) {
    TrackedEntityType type = createTrackedEntityType(uniqueChar);
    type.getSharing().setOwner(owner);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntityNotInSearchScope() {
    TrackedEntity te = trackedEntity(anotherOrgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(TrackedEntityType trackedEntityType) {
    TrackedEntity te = trackedEntity(orgUnit, trackedEntityType);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(OrganisationUnit orgUnit) {
    return trackedEntity(orgUnit, trackedEntityType);
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(owner);
    return te;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    manager.save(enrollment);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit(), coc);
    event.setAutoFields();
    manager.save(event);
    return event;
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private RelationshipType relationshipTypeAccessible(
      RelationshipEntity from, RelationshipEntity to) {
    RelationshipType type = relationshipType(from, to);
    type.getSharing().addUserAccess(userAccess());
    manager.save(type, false);
    return type;
  }

  private RelationshipType relationshipTypeNotAccessible() {
    return relationshipType(
        RelationshipEntity.PROGRAM_STAGE_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE);
  }

  private RelationshipType relationshipType(RelationshipEntity from, RelationshipEntity to) {
    RelationshipType type = createRelationshipType('A');
    type.getFromConstraint().setRelationshipEntity(from);
    type.getToConstraint().setRelationshipEntity(to);
    type.getSharing().setOwner(owner);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(type, false);
    return type;
  }

  private Relationship relationship(Event from, TrackedEntity to) {
    return relationship(
        relationshipTypeAccessible(
            RelationshipEntity.PROGRAM_STAGE_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE),
        from,
        to);
  }

  private Relationship relationship(RelationshipType type, Event from, TrackedEntity to) {
    Relationship r = new Relationship();

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setEvent(from);
    from.getRelationshipItems().add(fromItem);
    fromItem.setRelationship(r);
    r.setFrom(fromItem);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntity(to);
    to.getRelationshipItems().add(toItem);
    r.setTo(toItem);
    toItem.setRelationship(r);

    r.setRelationshipType(type);
    r.setKey(type.getUid());
    r.setInvertedKey(type.getUid());
    r.setAutoFields();
    r.getSharing().setOwner(owner);
    manager.save(r, false);
    return r;
  }

  private void relationship(TrackedEntity from, Event to) {
    Relationship r = new Relationship();

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setTrackedEntity(from);
    from.getRelationshipItems().add(fromItem);
    r.setFrom(fromItem);
    fromItem.setRelationship(r);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setEvent(to);
    to.getRelationshipItems().add(toItem);
    r.setTo(toItem);
    toItem.setRelationship(r);

    RelationshipType type =
        relationshipTypeAccessible(
            RelationshipEntity.PROGRAM_STAGE_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    r.setRelationshipType(type);
    r.setKey(type.getUid());
    r.setInvertedKey(type.getUid());

    r.setAutoFields();
    r.getSharing().setOwner(owner);
    manager.save(r, false);
  }

  private Note note(String uid, String value, String storedBy) {
    Note note = new Note(value, storedBy);
    note.setUid(uid);
    manager.save(note, false);
    return note;
  }

  private void assertDefaultResponse(JsonObject json, Event event) {
    // note that some fields are not included in the response because they
    // are not part of the setup
    // i.e. attributeOptionCombo, ...
    assertTrue(json.isObject());
    assertFalse(json.isEmpty());
    assertEquals(event.getUid(), json.getString("event").string(), "event UID");
    assertEquals("ACTIVE", json.getString("status").string());
    assertEquals(program.getUid(), json.getString("program").string());
    assertEquals(programStage.getUid(), json.getString("programStage").string());
    assertEquals(event.getEnrollment().getUid(), json.getString("enrollment").string());
    assertEquals(orgUnit.getUid(), json.getString("orgUnit").string());
    assertFalse(json.getBoolean("followUp").booleanValue());
    assertFalse(json.getBoolean("followup").booleanValue());
    assertFalse(json.getBoolean("deleted").booleanValue());
    assertHasMember(json, "createdAt");
    assertHasMember(json, "createdAtClient");
    assertHasMember(json, "updatedAt");
    assertHasMember(json, "updatedAtClient");
    assertHasMember(json, "dataValues");
    assertHasMember(json, "notes");
    assertHasMember(json, "attributeOptionCombo");
    assertHasMember(json, "attributeCategoryOptions");
    assertHasNoMember(json, "relationships");
  }
}
