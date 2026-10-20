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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.hisp.dhis.common.ValueType.FILE_RESOURCE;
import static org.hisp.dhis.common.ValueType.IMAGE;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.fileresource.FileResourceDomain.DATA_VALUE;
import static org.hisp.dhis.fileresource.FileResourceDomain.DOCUMENT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

class DeprecatedTrackerFileResourceControllerTest extends DhisControllerIntegrationTest {
  private static final String TEI_UID = "TeLegacyF01";
  private static final String ENROLLMENT_UID = "EnLegacyF01";
  private static final String EVENT_UID = "EvLegacyF01";
  private static final String OTHER_OWNER_UID = "EvLegacyF02";
  private static final byte[] FILE_BYTES =
      "legacy tracker file content".getBytes(StandardCharsets.UTF_8);

  @Autowired private FileResourceService fileResourceService;

  private final EnumMap<ValueType, DataElement> dataElements = new EnumMap<>(ValueType.class);
  private final EnumMap<ValueType, TrackedEntityAttribute> attributes =
      new EnumMap<>(ValueType.class);
  private OrganisationUnit orgUnit;
  private TrackedEntityType trackedEntityType;
  private Program program;
  private ProgramStage stage;
  private User writer;
  private User editor;

  private enum ImportTarget {
    EVENT("/events", "events"),
    TRACKED_ENTITY("/trackedEntityInstances", "trackedEntityInstances"),
    ENROLLMENT("/enrollments", "enrollments");

    private final String path;
    private final String collection;

    ImportTarget(String path, String collection) {
      this.path = path;
      this.collection = collection;
    }
  }

  @BeforeEach
  void setUpFileMetadata() {
    orgUnit = createOrganisationUnit('F');
    manager.save(orgUnit);
    writer = createAndAddUser("legacyFileWriter", orgUnit);
    editor = createAndAddUser("legacyFileEditor", orgUnit);
    writer.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    editor.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    userService.updateUser(writer);
    userService.updateUser(editor);

    trackedEntityType = createTrackedEntityType('F');
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.FULL);
    manager.save(trackedEntityType, false);

    program = createProgram('F');
    program.setTrackedEntityType(trackedEntityType);
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setPublicAccess(AccessStringHelper.FULL);
    manager.save(program, false);

    stage = createProgramStage('F', program);
    stage.getSharing().setPublicAccess(AccessStringHelper.FULL);
    stage.setRepeatable(true);
    char name = 'F';
    for (ValueType type : new ValueType[] {FILE_RESOURCE, IMAGE, TEXT}) {
      DataElement dataElement =
          createDataElement(name, type, AggregationType.NONE, DataElementDomain.TRACKER);
      dataElement.getSharing().setPublicAccess(AccessStringHelper.FULL);
      manager.save(dataElement, false);
      dataElements.put(type, dataElement);
      stage
          .getProgramStageDataElements()
          .add(createProgramStageDataElement(stage, dataElement, 1, false));

      TrackedEntityAttribute attribute = createTrackedEntityAttribute(name++, type);
      attribute.getSharing().setPublicAccess(AccessStringHelper.FULL);
      manager.save(attribute, false);
      attributes.put(type, attribute);
      TrackedEntityTypeAttribute typeAttribute =
          new TrackedEntityTypeAttribute(trackedEntityType, attribute);
      manager.save(typeAttribute, false);
      trackedEntityType.getTrackedEntityTypeAttributes().add(typeAttribute);
      program.getProgramAttributes().add(createProgramTrackedEntityAttribute(program, attribute));
    }
    manager.update(trackedEntityType);
    manager.update(program);
    manager.save(stage, false);
    program.getProgramStages().add(stage);
    manager.update(program);
    flushAndClear();
    switchToNewUser(writer);
  }

  static Stream<Arguments> imports() {
    return Stream.of(ImportTarget.values())
        .flatMap(target -> Stream.of(FILE_RESOURCE, IMAGE).map(type -> Arguments.of(target, type)));
  }

  static Stream<Arguments> servingEndpoints() {
    return Stream.of(ImportTarget.EVENT, ImportTarget.TRACKED_ENTITY)
        .flatMap(target -> Stream.of(FILE_RESOURCE, IMAGE).map(type -> Arguments.of(target, type)));
  }

  @ParameterizedTest
  @MethodSource("imports")
  void shouldRejectDataValueUploadedByAnotherUser(ImportTarget target, ValueType type)
      throws Exception {
    prepareImport(target);
    FileResource file = storeFile(editor, DATA_VALUE, type);

    assertRejected(target, importValue(target, type, file.getUid()), false);

    assertNull(savedValue(target, type));
    assertFalse(reloadFile(file).isAssigned());
    assertEquals(DATA_VALUE, reloadFile(file).getDomain());
  }

  @ParameterizedTest
  @MethodSource("imports")
  void shouldRejectOwnFileOfAnotherDomain(ImportTarget target, ValueType type) throws Exception {
    prepareImport(target);
    FileResource file = storeFile(writer, DOCUMENT, type);

    assertRejected(target, importValue(target, type, file.getUid()), false);

    assertNull(savedValue(target, type));
    assertFalse(reloadFile(file).isAssigned());
    assertEquals(DOCUMENT, reloadFile(file).getDomain());
  }

  @ParameterizedTest
  @MethodSource("imports")
  void shouldImportOwnDataValue(ImportTarget target, ValueType type) throws Exception {
    prepareImport(target);
    FileResource file = storeFile(writer, DATA_VALUE, type);

    assertImported(target, importValue(target, type, file.getUid()), false);

    assertEquals(file.getUid(), savedValue(target, type));
    assertTrue(reloadFile(file).isAssigned());
  }

  @ParameterizedTest
  @MethodSource("imports")
  void shouldAllowAnotherEditorToResendUnchangedValueIncludingWithoutOwner(
      ImportTarget target, ValueType type) throws Exception {
    prepareImport(target);
    FileResource file = storeFile(writer, DATA_VALUE, type);
    assertImported(target, importValue(target, type, file.getUid()), false);
    FileResource recordedFile = reloadFile(file);
    recordedFile.setFileResourceOwner(target == ImportTarget.EVENT ? EVENT_UID : TEI_UID);
    fileResourceService.updateFileResource(recordedFile);
    flushAndClear();

    switchToNewUser(editor);
    assertImported(target, importValue(target, type, file.getUid()), true);
    assertEquals(file.getUid(), savedValue(target, type));

    FileResource legacyFile = reloadFile(file);
    legacyFile.setAssigned(true);
    legacyFile.setFileResourceOwner(null);
    fileResourceService.updateFileResource(legacyFile);
    flushAndClear();

    assertImported(target, importValue(target, type, file.getUid()), true);
    assertEquals(file.getUid(), savedValue(target, type));
    assertTrue(reloadFile(file).isAssigned());
  }

  @ParameterizedTest
  @MethodSource("imports")
  void shouldNotTreatUidInTextFieldAsHeldFile(ImportTarget target, ValueType type)
      throws Exception {
    prepareImport(target);
    FileResource file = storeFile(editor, DATA_VALUE, type);
    assertImported(target, importValue(target, TEXT, file.getUid()), false);

    assertRejected(target, importValue(target, type, file.getUid()), false);
    assertEquals(file.getUid(), savedValue(target, TEXT));
    assertNull(savedValue(target, type));
    assertFalse(reloadFile(file).isAssigned());

    FileResource assignedFile = reloadFile(file);
    assignedFile.setAssigned(true);
    assignedFile.setFileResourceOwner(OTHER_OWNER_UID);
    fileResourceService.updateFileResource(assignedFile);
    flushAndClear();

    assertRejected(target, importValue(target, type, file.getUid()), true);
    assertEquals(file.getUid(), savedValue(target, TEXT));
    assertNull(savedValue(target, type));
    assertTrue(reloadFile(file).isAssigned());
    assertEquals(OTHER_OWNER_UID, reloadFile(file).getFileResourceOwner());
    assertEquals(
        target == ImportTarget.EVENT ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND,
        GET(fileUrl(target, type)).status());
  }

  @ParameterizedTest
  @EnumSource(
      value = ValueType.class,
      names = {"FILE_RESOURCE", "IMAGE"})
  void shouldRejectUnassignableFileOnSingleValueUpdate(ValueType type) throws Exception {
    prepareImport(ImportTarget.EVENT);
    FileResource foreignFile = storeFile(editor, DATA_VALUE, type);
    assertImported(
        ImportTarget.EVENT, importValue(ImportTarget.EVENT, TEXT, foreignFile.getUid()), false);

    assertRejected(
        updateEventValue(type, foreignFile.getUid())
            .content(HttpStatus.CONFLICT)
            .getObject("response"),
        false);
    assertEquals(foreignFile.getUid(), savedValue(ImportTarget.EVENT, TEXT));
    assertNull(savedValue(ImportTarget.EVENT, type));
    assertFalse(reloadFile(foreignFile).isAssigned());

    FileResource ownDocument = storeFile(writer, DOCUMENT, type);
    assertRejected(
        updateEventValue(type, ownDocument.getUid())
            .content(HttpStatus.CONFLICT)
            .getObject("response"),
        false);
    assertNull(savedValue(ImportTarget.EVENT, type));
    assertFalse(reloadFile(ownDocument).isAssigned());
    assertEquals(DOCUMENT, reloadFile(ownDocument).getDomain());
  }

  @ParameterizedTest
  @EnumSource(
      value = ValueType.class,
      names = {"FILE_RESOURCE", "IMAGE"})
  void shouldAcceptOwnFileAndUnchangedResendOnSingleValueUpdate(ValueType type) throws Exception {
    prepareImport(ImportTarget.EVENT);
    assertImported(ImportTarget.EVENT, importValue(ImportTarget.EVENT, TEXT, "original"), false);
    FileResource file = storeFile(writer, DATA_VALUE, type);

    assertImported(
        updateEventValue(type, file.getUid()).content(HttpStatus.OK).getObject("response"), true);
    assertEquals(file.getUid(), savedValue(ImportTarget.EVENT, type));
    assertTrue(reloadFile(file).isAssigned());

    switchToNewUser(editor);
    assertImported(
        updateEventValue(type, file.getUid()).content(HttpStatus.OK).getObject("response"), true);
    FileResource legacyFile = reloadFile(file);
    legacyFile.setFileResourceOwner(null);
    fileResourceService.updateFileResource(legacyFile);
    flushAndClear();

    assertImported(
        updateEventValue(type, file.getUid()).content(HttpStatus.OK).getObject("response"), true);
    assertEquals(file.getUid(), savedValue(ImportTarget.EVENT, type));
    assertTrue(reloadFile(file).isAssigned());
  }

  @ParameterizedTest
  @EnumSource(
      value = ValueType.class,
      names = {"FILE_RESOURCE", "IMAGE"})
  void shouldClearFileWithDeletionMarkerOnSingleValueUpdate(ValueType type) throws Exception {
    prepareImport(ImportTarget.EVENT);
    assertImported(ImportTarget.EVENT, importValue(ImportTarget.EVENT, TEXT, "original"), false);
    FileResource file = storeFile(writer, DATA_VALUE, type);
    assertImported(
        updateEventValue(type, file.getUid()).content(HttpStatus.OK).getObject("response"), true);
    assertEquals(file.getUid(), savedValue(ImportTarget.EVENT, type));

    assertImported(
        updateEventValue(type, "null").content(HttpStatus.OK).getObject("response"), true);
    assertNull(savedValue(ImportTarget.EVENT, type));
  }

  @ParameterizedTest
  @MethodSource("servingEndpoints")
  void shouldServeDataValueButNotFileOfAnotherDomain(ImportTarget target, ValueType type)
      throws Exception {
    prepareImport(target);
    FileResource file = storeFile(writer, DATA_VALUE, type);
    assertImported(target, importValue(target, type, file.getUid()), false);

    MockHttpServletResponse response =
        webRequestWithMvcResult(get(fileUrl(target, type))).getResponse();
    assertEquals(200, response.getStatus());
    if (target == ImportTarget.TRACKED_ENTITY && type == IMAGE) {
      BufferedImage image =
          ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
      assertEquals(2, image.getWidth());
      assertEquals(2, image.getHeight());
      assertEquals(0xff123456, image.getRGB(0, 0));
    } else {
      assertArrayEquals(content(type), response.getContentAsByteArray());
    }

    FileResource existingFile = reloadFile(file);
    existingFile.setDomain(DOCUMENT);
    fileResourceService.updateFileResource(existingFile);
    flushAndClear();

    assertEquals(HttpStatus.NOT_FOUND, GET(fileUrl(target, type)).status());
    assertEquals(file.getUid(), savedValue(target, type));
    assertTrue(reloadFile(file).isAssigned());
    assertEquals(DOCUMENT, reloadFile(file).getDomain());
  }

  private void prepareImport(ImportTarget target) {
    if (target == ImportTarget.TRACKED_ENTITY) {
      return;
    }
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit);
    trackedEntity.setUid(TEI_UID);
    trackedEntity.setTrackedEntityType(
        manager.get(TrackedEntityType.class, trackedEntityType.getUid()));
    manager.save(trackedEntity);
    if (target == ImportTarget.EVENT) {
      Enrollment enrollment =
          createEnrollment(manager.get(Program.class, program.getUid()), trackedEntity, orgUnit);
      enrollment.setUid(ENROLLMENT_UID);
      manager.save(enrollment);
    }
    flushAndClear();
  }

  private FileResource storeFile(User uploader, FileResourceDomain domain, ValueType type)
      throws Exception {
    switchToNewUser(uploader);
    byte[] bytes = content(type);
    FileResource file = createFileResource('F', bytes);
    file.setName(type == IMAGE ? "legacy-image.png" : "legacy-file.txt");
    file.setContentType(type == IMAGE ? "image/png" : "text/plain");
    file.setDomain(domain);
    file.setCreatedBy(uploader);
    fileResourceService.syncSaveFileResource(file, bytes);
    file.setStorageStatus(FileResourceStorageStatus.STORED);
    fileResourceService.updateFileResource(file);
    flushAndClear();
    switchToNewUser(writer);
    return file;
  }

  private static byte[] content(ValueType type) throws Exception {
    if (type != IMAGE) {
      return FILE_BYTES;
    }
    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, 0xff123456);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return bytes.toByteArray();
  }

  private HttpResponse importValue(ImportTarget target, ValueType type, String uid) {
    String value =
        "{'value':'"
            + uid
            + "', '"
            + (target == ImportTarget.EVENT ? "dataElement" : "attribute")
            + "':'"
            + fieldUid(target, type)
            + "'}";
    String object;
    if (target == ImportTarget.EVENT) {
      object =
          "{'event':'"
              + EVENT_UID
              + "','program':'"
              + program.getUid()
              + "','programStage':'"
              + stage.getUid()
              + "','enrollment':'"
              + ENROLLMENT_UID
              + "','trackedEntityInstance':'"
              + TEI_UID
              + "','orgUnit':'"
              + orgUnit.getUid()
              + "','eventDate':'2020-01-01','status':'ACTIVE','dataValues':["
              + value
              + "]}";
    } else if (target == ImportTarget.TRACKED_ENTITY) {
      object =
          "{'trackedEntityInstance':'"
              + TEI_UID
              + "','trackedEntityType':'"
              + trackedEntityType.getUid()
              + "','orgUnit':'"
              + orgUnit.getUid()
              + "','attributes':["
              + value
              + "]}";
    } else {
      object =
          "{'enrollment':'"
              + ENROLLMENT_UID
              + "','program':'"
              + program.getUid()
              + "','trackedEntityInstance':'"
              + TEI_UID
              + "','orgUnit':'"
              + orgUnit.getUid()
              + "','enrollmentDate':'2020-01-01','incidentDate':'2020-01-01','status':'ACTIVE','attributes':["
              + value
              + "]}";
    }
    flushAndClear();
    HttpResponse response = POST(target.path, "{'" + target.collection + "':[" + object + "]}");
    flushAndClear();
    return response;
  }

  private HttpResponse updateEventValue(ValueType type, String uid) {
    flushAndClear();
    HttpResponse response =
        PUT(
            "/events/" + EVENT_UID + "/" + dataElements.get(type).getUid(),
            "{'dataValues':[{'dataElement':'"
                + dataElements.get(type).getUid()
                + "','value':'"
                + uid
                + "'}]}");
    flushAndClear();
    return response;
  }

  private void assertImported(ImportTarget target, HttpResponse response, boolean update) {
    assertImported(summary(target, response, false), update);
  }

  private void assertImported(JsonObject summary, boolean update) {
    assertEquals("SUCCESS", summary.getString("status").string(), summary.toString());
    assertEquals(
        1, summary.getObject("importCount").getNumber(update ? "updated" : "imported").intValue());
    assertEquals(0, summary.getObject("importCount").getNumber("ignored").intValue());
  }

  private void assertRejected(ImportTarget target, HttpResponse response, boolean assigned) {
    assertRejected(summary(target, response, true), assigned);
  }

  private void assertRejected(JsonObject summary, boolean assigned) {
    assertEquals("ERROR", summary.getString("status").string(), summary.toString());
    assertEquals(1, summary.getObject("importCount").getNumber("ignored").intValue());
    assertEquals(0, summary.getObject("importCount").getNumber("imported").intValue());
    assertEquals(0, summary.getObject("importCount").getNumber("updated").intValue());
    String conflicts = summary.getArray("conflicts").toString();
    assertTrue(conflicts.contains(assigned ? "assigned to" : "DATA_VALUE"), summary.toString());
  }

  private JsonObject summary(ImportTarget target, HttpResponse response, boolean rejected) {
    // This legacy enrollment controller uses 201 even for an ERROR import summary.
    HttpStatus status =
        target == ImportTarget.ENROLLMENT
            ? HttpStatus.CREATED
            : rejected ? HttpStatus.CONFLICT : HttpStatus.OK;
    return response.content(status).getObject("response").getArray("importSummaries").getObject(0);
  }

  private String fieldUid(ImportTarget target, ValueType type) {
    return target == ImportTarget.EVENT
        ? dataElements.get(type).getUid()
        : attributes.get(type).getUid();
  }

  private String savedValue(ImportTarget target, ValueType type) {
    if (target == ImportTarget.EVENT) {
      Event event = manager.get(Event.class, EVENT_UID);
      return event == null
          ? null
          : event.getEventDataValues().stream()
              .filter(value -> fieldUid(target, type).equals(value.getDataElement()))
              .map(value -> value.getValue())
              .findFirst()
              .orElse(null);
    }
    TrackedEntity trackedEntity = manager.get(TrackedEntity.class, TEI_UID);
    return trackedEntity == null
        ? null
        : trackedEntity.getTrackedEntityAttributeValues().stream()
            .filter(value -> fieldUid(target, type).equals(value.getAttribute().getUid()))
            .map(value -> value.getValue())
            .findFirst()
            .orElse(null);
  }

  private FileResource reloadFile(FileResource file) {
    return fileResourceService.getFileResource(file.getUid());
  }

  private String fileUrl(ImportTarget target, ValueType type) {
    return target == ImportTarget.EVENT
        ? "/events/files?eventUid=" + EVENT_UID + "&dataElementUid=" + fieldUid(target, type)
        : "/trackedEntityInstances/"
            + TEI_UID
            + "/"
            + fieldUid(target, type)
            + (type == IMAGE ? "/image" : "/file");
  }

  private void flushAndClear() {
    dbmsManager.flushSession();
    dbmsManager.clearSession();
  }
}
