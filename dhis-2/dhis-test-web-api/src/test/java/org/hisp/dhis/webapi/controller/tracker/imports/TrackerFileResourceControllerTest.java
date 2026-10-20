/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.tracker.JsonImportReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * A file value of a tracker object binds the file resource to that object, after which the tracker
 * API serves the file content to everyone who can read the object.
 */
class TrackerFileResourceControllerTest extends DhisControllerIntegrationTest {
  @PersistenceContext private EntityManager entityManager;

  private static final String ORG_UNIT = "OuFileVal01";
  private static final String DATA_ELEMENT = "DeFileVal01";
  private static final String PROGRAM = "PrFileVal01";
  private static final String PROGRAM_STAGE = "PsFileVal01";
  private static final String ATTRIBUTE = "TaFileVal01";
  private static final String TRACKED_ENTITY_TYPE = "TtFileVal01";
  private static final String TRACKER_PROGRAM = "PrFileVal02";
  private static final String TEXT_DATA_ELEMENT = "DeTextVal01";
  private static final String TEXT_ATTRIBUTE = "TaTextVal01";

  private User writer;

  private User reader;

  @BeforeEach
  void setUp() {
    HttpResponse metadataResponse =
        POST(
            "/metadata",
            """
            {
             "organisationUnits": [{"id": "%1$s", "name": "File OU", "shortName": "File OU",
               "openingDate": "2020-01-01"}],
             "dataElements": [{"id": "%2$s", "name": "File value", "shortName": "File value",
               "valueType": "FILE_RESOURCE", "domainType": "TRACKER", "aggregationType": "NONE"},
               {"id": "%8$s", "name": "Text value", "shortName": "Text value",
               "valueType": "TEXT", "domainType": "TRACKER", "aggregationType": "NONE"}],
             "programs": [{"id": "%3$s", "name": "File program", "shortName": "File program",
               "programType": "WITHOUT_REGISTRATION", "organisationUnits": [{"id": "%1$s"}],
               "programStages": [{"id": "%4$s"}]},
               {"id": "%7$s", "name": "File tracker program", "shortName": "File tracker program",
               "programType": "WITH_REGISTRATION", "trackedEntityType": {"id": "%6$s"},
               "organisationUnits": [{"id": "%1$s"}],
               "programTrackedEntityAttributes": [{"trackedEntityAttribute": {"id": "%5$s"}}]}],
             "programStages": [{"id": "%4$s", "name": "File stage", "program": {"id": "%3$s"},
               "programStageDataElements": [{"dataElement": {"id": "%2$s"}},
                 {"dataElement": {"id": "%8$s"}}]}],
             "trackedEntityAttributes": [{"id": "%5$s", "name": "File attribute",
               "shortName": "File attribute", "valueType": "FILE_RESOURCE",
               "aggregationType": "NONE"},
               {"id": "%9$s", "name": "Text attribute", "shortName": "Text attribute",
               "valueType": "TEXT", "aggregationType": "NONE"}],
             "trackedEntityTypes": [{"id": "%6$s", "name": "File person", "shortName": "File person",
               "trackedEntityTypeAttributes": [{"trackedEntityAttribute": {"id": "%5$s"}},
                 {"trackedEntityAttribute": {"id": "%9$s"}}]}]
            }
            """
                .formatted(
                    ORG_UNIT,
                    DATA_ELEMENT,
                    PROGRAM,
                    PROGRAM_STAGE,
                    ATTRIBUTE,
                    TRACKED_ENTITY_TYPE,
                    TRACKER_PROGRAM,
                    TEXT_DATA_ELEMENT,
                    TEXT_ATTRIBUTE));
    assertStatus(HttpStatus.OK, metadataResponse);
    shareDataWrite("program", PROGRAM);
    shareDataWrite("programStage", PROGRAM_STAGE);
    shareDataWrite("trackedEntityType", TRACKED_ENTITY_TYPE);
    // a tracked entity is readable through a tracker program of its type
    shareDataWrite("program", TRACKER_PROGRAM);

    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, ORG_UNIT);
    writer = createAndAddUser("fileValueWriter", orgUnit);
    reader = createAndAddUser("fileValueReader", orgUnit);
  }

  @Test
  @DisplayName("A data reader reads a file value uploaded and imported by another user")
  void shouldServeOwnDataValueFileToReadersOfTheEvent() {
    switchToNewUser(writer);
    String fileUid = upload(FileResourceDomain.DATA_VALUE, "own content");
    assertImported(event("EvFileVal01", fileUid));
    // clients resend the unchanged value when they update another field
    assertImported(event("EvFileVal01", fileUid));

    switchToNewUser(reader);
    assertFileContent("own content", eventFilePath("EvFileVal01"));
    assertEquals(
        "DATA_VALUE",
        GET("/fileResources/{uid}", fileUid).content(HttpStatus.OK).getString("domain").string());
    assertEquals(HttpStatus.FORBIDDEN, GET("/fileResources/{uid}/data", fileUid).status());
  }

  @Test
  @DisplayName("An event cannot be created with a file resource of another domain")
  void shouldRejectFileOfAnotherDomainOnEventCreate() {
    String fileUid = upload(FileResourceDomain.DOCUMENT, "document content");

    switchToNewUser(writer);
    assertImportError(event("EvFileVal02", fileUid), "E1004");
    assertEquals(HttpStatus.NOT_FOUND, GET(eventFilePath("EvFileVal02")).status());

    switchToNewUser(superUser);
    assertFalse(fileResource(fileUid).isAssigned());
  }

  @Test
  @DisplayName("An event cannot be created with a data value file uploaded by another user")
  void shouldRejectDataValueFileOfAnotherUserOnEventCreate() {
    String fileUid = upload(FileResourceDomain.DATA_VALUE, "other content");

    switchToNewUser(writer);
    assertImportError(event("EvFileVal03", fileUid), "E1004");

    switchToNewUser(superUser);
    assertFalse(fileResource(fileUid).isAssigned());
  }

  @Test
  @DisplayName("An event cannot be updated to a file resource bound to a document")
  void shouldRejectDocumentFileOnEventUpdate() {
    String documentFileUid = upload(FileResourceDomain.DOCUMENT, "document content");
    String documentUid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/documents",
                "{'name':'Private','external':false,'type':'UPLOAD_FILE','attachment':false,'url':'"
                    + documentFileUid
                    + "'}"));

    try {
      switchToNewUser(writer);
      String ownFileUid = upload(FileResourceDomain.DATA_VALUE, "own content");
      assertImported(event("EvFileVal04", ownFileUid));

      assertImportError(event("EvFileVal04", documentFileUid), "E1004");
      assertFileContent("own content", eventFilePath("EvFileVal04"));
    } finally {
      // the per-test database cleanup does not remove a document before its file resource
      switchToNewUser(superUser);
      assertStatus(HttpStatus.OK, DELETE("/documents/" + documentUid));
    }
  }

  @Test
  @DisplayName("An event cannot be updated to a file value that belongs to another event")
  void shouldRejectFileOfAnotherEventOnEventUpdate() {
    switchToNewUser(writer);
    String firstFileUid = upload(FileResourceDomain.DATA_VALUE, "first content");
    String secondFileUid = upload(FileResourceDomain.DATA_VALUE, "second content");
    assertImported(event("EvFileVal05", firstFileUid));
    assertImported(event("EvFileVal06", secondFileUid));

    assertImportError(event("EvFileVal06", firstFileUid), "E1009");
    assertFileContent("second content", eventFilePath("EvFileVal06"));
  }

  @Test
  @DisplayName("A file value assigned before file owners were recorded can be resent unchanged")
  void shouldAcceptUnchangedFileValueWithoutRecordedOwner() {
    switchToNewUser(writer);
    String fileUid = upload(FileResourceDomain.DATA_VALUE, "legacy content");
    assertImported(event("EvFileVal07", fileUid));

    FileResource fileResource = fileResource(fileUid);
    fileResource.setFileResourceOwner(null);
    manager.update(fileResource);

    switchToNewUser(reader);
    assertImported(event("EvFileVal07", fileUid));
  }

  @Test
  @DisplayName("The tracker API only serves a file value of the DATA_VALUE domain")
  void shouldNotServeFileValueOfAnotherDomain() {
    switchToNewUser(writer);
    String fileUid = upload(FileResourceDomain.DATA_VALUE, "own content");
    assertImported(event("EvFileVal08", fileUid));

    FileResource fileResource = fileResource(fileUid);
    fileResource.setDomain(FileResourceDomain.DOCUMENT);
    manager.update(fileResource);

    assertEquals(HttpStatus.NOT_FOUND, GET(eventFilePath("EvFileVal08")).status());
    assertEquals(
        HttpStatus.NOT_FOUND,
        GET("/tracker/events/EvFileVal08/dataValues/" + DATA_ELEMENT + "/image").status());
  }

  @Test
  @DisplayName("A tracked entity attribute only takes an own unassigned data value file")
  void shouldOnlyBindOwnDataValueFileToTrackedEntityAttribute() {
    String documentFileUid = upload(FileResourceDomain.DOCUMENT, "document content");
    String otherFileUid = upload(FileResourceDomain.DATA_VALUE, "other content");

    switchToNewUser(writer);
    assertImportError(trackedEntity("TeFileVal01", documentFileUid), "E1004");
    assertImportError(trackedEntity("TeFileVal01", otherFileUid), "E1004");

    String ownFileUid = upload(FileResourceDomain.DATA_VALUE, "own content");
    assertImported(trackedEntity("TeFileVal01", ownFileUid));

    switchToNewUser(reader);
    assertFileContent("own content", attributeFilePath("TeFileVal01"));

    FileResource fileResource = fileResource(ownFileUid);
    fileResource.setDomain(FileResourceDomain.DOCUMENT);
    manager.update(fileResource);
    newRequest();
    assertEquals(HttpStatus.NOT_FOUND, GET(attributeFilePath("TeFileVal01")).status());
  }

  @Test
  @DisplayName("An enrollment attribute only takes an own data value file or the one already held")
  void shouldOnlyBindOwnDataValueFileToEnrollmentAttribute() {
    String documentFileUid = upload(FileResourceDomain.DOCUMENT, "document content");

    switchToNewUser(writer);
    String ownFileUid = upload(FileResourceDomain.DATA_VALUE, "own content");
    assertImported(trackedEntity("TeFileVal02", ownFileUid));

    assertImportError(enrollment("EnFileVal01", "TeFileVal02", documentFileUid), "E1004");
    // the value the tracked entity already holds is not a new binding
    assertImported(enrollment("EnFileVal01", "TeFileVal02", ownFileUid));
    assertFileContent("own content", attributeFilePath("TeFileVal02"));
  }

  @Test
  @DisplayName("A file uid held by a text data value of the event is not a held file value")
  void shouldRejectFileUidHeldByTextDataValueOnEventUpdate() {
    switchToNewUser(reader);
    String assignedFileUid = upload(FileResourceDomain.DATA_VALUE, "other event content");
    assertImported(event("EvFileVal10", assignedFileUid));
    String unassignedFileUid = upload(FileResourceDomain.DATA_VALUE, "other content");

    switchToNewUser(writer);
    assertImported(eventValue("EvFileVal11", TEXT_DATA_ELEMENT, assignedFileUid));
    assertImportError(event("EvFileVal11", assignedFileUid), "E1009");
    assertEquals(HttpStatus.NOT_FOUND, GET(eventFilePath("EvFileVal11")).status());

    assertImported(eventValue("EvFileVal12", TEXT_DATA_ELEMENT, unassignedFileUid));
    assertImportError(event("EvFileVal12", unassignedFileUid), "E1004");
    assertEquals(HttpStatus.NOT_FOUND, GET(eventFilePath("EvFileVal12")).status());

    switchToNewUser(superUser);
    assertEquals("EvFileVal10", fileResource(assignedFileUid).getFileResourceOwner());
    assertFalse(fileResource(unassignedFileUid).isAssigned());
  }

  @Test
  @DisplayName("A file uid held by a text attribute of the tracked entity is not a held file value")
  void shouldRejectFileUidHeldByTextAttributeOnTrackedEntityUpdate() {
    switchToNewUser(reader);
    String assignedFileUid = upload(FileResourceDomain.DATA_VALUE, "other entity content");
    assertImported(trackedEntity("TeFileVal03", assignedFileUid));
    String unassignedFileUid = upload(FileResourceDomain.DATA_VALUE, "other content");

    switchToNewUser(writer);
    assertImported(trackedEntityValue("TeFileVal04", TEXT_ATTRIBUTE, assignedFileUid));
    assertImportError(trackedEntity("TeFileVal04", assignedFileUid), "E1009");
    assertImportError(enrollment("EnFileVal02", "TeFileVal04", assignedFileUid), "E1009");
    assertEquals(HttpStatus.NOT_FOUND, GET(attributeFilePath("TeFileVal04")).status());

    assertImported(trackedEntityValue("TeFileVal05", TEXT_ATTRIBUTE, unassignedFileUid));
    assertImportError(trackedEntity("TeFileVal05", unassignedFileUid), "E1004");
    assertEquals(HttpStatus.NOT_FOUND, GET(attributeFilePath("TeFileVal05")).status());

    switchToNewUser(superUser);
    assertEquals("TeFileVal03", fileResource(assignedFileUid).getFileResourceOwner());
    assertFalse(fileResource(unassignedFileUid).isAssigned());
  }

  private void shareDataWrite(String type, String uid) {
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/sharing?type=" + type + "&id=" + uid,
            "{'object':{'publicAccess':'rwrw----','externalAccess':false,'userAccesses':[],"
                + "'userGroupAccesses':[]}}"));
  }

  private String upload(FileResourceDomain domain, String content) {
    MockMultipartFile file =
        new MockMultipartFile("file", "file.txt", "text/plain", content.getBytes());
    return POST_MULTIPART("/fileResources?domain=" + domain.name(), file)
        .content(HttpStatus.ACCEPTED)
        .getObject("response")
        .getObject("fileResource")
        .getString("id")
        .string();
  }

  private FileResource fileResource(String uid) {
    newRequest();
    return manager.get(FileResource.class, uid);
  }

  private static String eventFilePath(String event) {
    return "/tracker/events/" + event + "/dataValues/" + DATA_ELEMENT + "/file";
  }

  private static String event(String event, String fileUid) {
    return eventValue(event, DATA_ELEMENT, fileUid);
  }

  private static String eventValue(String event, String dataElement, String value) {
    return """
        {"events": [{"event": "%s", "program": "%s", "programStage": "%s", "orgUnit": "%s",
          "occurredAt": "2024-01-01", "status": "ACTIVE",
          "dataValues": [{"dataElement": "%s", "value": "%s"}]}]}
        """
        .formatted(event, PROGRAM, PROGRAM_STAGE, ORG_UNIT, dataElement, value);
  }

  private static String attributeFilePath(String trackedEntity) {
    return "/tracker/trackedEntities/" + trackedEntity + "/attributes/" + ATTRIBUTE + "/file";
  }

  private static String enrollment(String enrollment, String trackedEntity, String fileUid) {
    return """
        {"enrollments": [{"enrollment": "%s", "trackedEntity": "%s", "program": "%s",
          "orgUnit": "%s", "enrolledAt": "2024-01-01", "occurredAt": "2024-01-01",
          "status": "ACTIVE", "attributes": [{"attribute": "%s", "value": "%s"}]}]}
        """
        .formatted(enrollment, trackedEntity, TRACKER_PROGRAM, ORG_UNIT, ATTRIBUTE, fileUid);
  }

  private static String trackedEntity(String trackedEntity, String fileUid) {
    return trackedEntityValue(trackedEntity, ATTRIBUTE, fileUid);
  }

  private static String trackedEntityValue(String trackedEntity, String attribute, String value) {
    return """
        {"trackedEntities": [{"trackedEntity": "%s", "trackedEntityType": "%s", "orgUnit": "%s",
          "attributes": [{"attribute": "%s", "value": "%s"}]}]}
        """
        .formatted(trackedEntity, TRACKED_ENTITY_TYPE, ORG_UNIT, attribute, value);
  }

  /**
   * Requests of this test share one thread-bound persistence context, while each request of a
   * running server has its own. The tracker importer writes file assignments with JDBC, so a file
   * resource loaded by an earlier request would otherwise be read back unchanged.
   */
  private void newRequest() {
    entityManager.flush();
    entityManager.clear();
  }

  private void assertFileContent(String expected, String path) {
    newRequest();
    HttpResponse response = GET(path);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toString());
    assertEquals(expected, response.content("text/plain"));
  }

  private void assertImported(String payload) {
    newRequest();
    JsonImportReport report =
        POST("/tracker?async=false", payload).content(HttpStatus.OK).as(JsonImportReport.class);
    assertEquals("OK", report.getStatus());
  }

  private void assertImportError(String payload, String errorCode) {
    newRequest();
    JsonImportReport report =
        POST("/tracker?async=false", payload)
            .content(HttpStatus.CONFLICT)
            .as(JsonImportReport.class);
    assertEquals(
        errorCode,
        report.getString("validationReport.errorReports[0].errorCode").string(),
        report.toString());
  }
}
