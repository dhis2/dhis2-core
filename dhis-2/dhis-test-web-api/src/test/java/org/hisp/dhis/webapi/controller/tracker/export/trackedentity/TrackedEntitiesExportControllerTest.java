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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.utils.Assertions.assertHasSize;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.web.WebClient.Accept;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContainsAll;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertFirstRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntitiesExportControllerTest extends DhisControllerConvenienceTest {
  // Used to generate unique chars for creating test objects like TEA, ...
  private static final String UNIQUE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String EVENT_OCCURRED_AT = "2023-03-23T12:23:00.000";

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private FileResourceService fileResourceService;

  @Autowired private EnrollmentService enrollmentService;

  private OrganisationUnit orgUnit;

  private OrganisationUnit anotherOrgUnit;

  private Program program;

  private ProgramStage programStage;

  private TrackedEntityType trackedEntityType;

  private DataElement dataElement;

  private User owner;

  private User user;
  private User userWithDifferentScopes;

  private TrackedEntity softDeletedTrackedEntity;

  // Used to generate unique chars for creating TEA in test setup
  private int uniqueAttributeCharCounter = 0;

  @BeforeEach
  void setUp() {
    owner = makeUser("owner");

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

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(owner);
    program.getSharing().addUserAccess(userAccess(user));
    manager.save(program, false);

    programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(owner);
    programStage.getSharing().addUserAccess(userAccess(user));
    manager.save(programStage, false);

    trackedEntityType = trackedEntityTypeAccessible();
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    softDeletedTrackedEntity = createTrackedEntity(orgUnit);
    softDeletedTrackedEntity.setDeleted(true);
    manager.save(softDeletedTrackedEntity, false);
  }

  @Test
  void getTrackedEntitiesNeedsProgramOrType() {
    injectSecurityContextUser(user);

    assertEquals(
        "Either `program`, `trackedEntityType` or `trackedEntities` should be specified",
        GET("/tracker/trackedEntities").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void getTrackedEntitiesNeedsProgramOrTrackedEntityType() {
    this.switchContextToUser(user);

    assertEquals(
        "Either `program`, `trackedEntityType` or `trackedEntities` should be specified",
        GET("/tracker/trackedEntities?orgUnit={ou}", orgUnit.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldReturnEmptyListWhenGettingTrackedEntitiesWithNoMatchingParams() {
    LocalDate futureDate = LocalDate.now().plusYears(1);
    JsonList<JsonTrackedEntity> instances =
        GET("/tracker/trackedEntities?trackedEntityType="
                + trackedEntityType.getUid()
                + "&ouMode=ALL"
                + "&trackedEntities=AbjwFr5o9IT"
                + "&updatedAfter="
                + futureDate)
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    assertEquals(0, instances.size());
  }

  @Test
  void shouldGetTrackedEntitiesWhenCaptureScopeProvidedAndDisregardingSearchScope() {
    userWithDifferentScopes =
        createUserWithId("testerWithDiffSearchScope", CodeGenerator.generateUid());
    userWithDifferentScopes.addOrganisationUnit(orgUnit);
    userWithDifferentScopes.setTeiSearchOrganisationUnits(Set.of(anotherOrgUnit));

    program.getSharing().addUserAccess(userAccess(userWithDifferentScopes));
    manager.save(program, false);

    trackedEntityType.getSharing().setUserAccesses(Set.of(userAccess(userWithDifferentScopes)));
    manager.save(trackedEntityType, false);

    this.userService.updateUser(userWithDifferentScopes);

    this.switchContextToUser(userWithDifferentScopes);
    HttpResponse response =
        GET(
            "/tracker/trackedEntities?program={programId}&orgUnitMode={orgUnitMode}",
            program.getUid(),
            CAPTURE);

    assertEquals(HttpStatus.OK, response.status());
  }

  @Test
  void getTrackedEntityById() {
    TrackedEntity te = trackedEntity();
    this.switchContextToUser(user);

    JsonTrackedEntity json =
        GET("/tracker/trackedEntities/{id}", te.getUid())
            .content(HttpStatus.OK)
            .as(JsonTrackedEntity.class);

    assertFalse(json.isEmpty());
    assertEquals(te.getUid(), json.getTrackedEntity());
    assertEquals(te.getTrackedEntityType().getUid(), json.getTrackedEntityType());
    assertEquals(te.getOrganisationUnit().getUid(), json.getOrgUnit());
    assertHasMember(json, "createdAt");
    assertHasMember(json, "createdAtClient");
    assertHasMember(json, "updatedAtClient");
    assertHasNoMember(json, "relationships");
    assertHasNoMember(json, "enrollments");
    assertHasNoMember(json, "events");
    assertHasNoMember(json, "programOwners");
  }

  @Test
  void getTrackedEntityByIdWithFields() {
    TrackedEntity te = trackedEntity();
    this.switchContextToUser(user);

    JsonTrackedEntity json =
        GET("/tracker/trackedEntities/{id}?fields=trackedEntityType,orgUnit", te.getUid())
            .content(HttpStatus.OK)
            .as(JsonTrackedEntity.class);

    assertHasOnlyMembers(json, "trackedEntityType", "orgUnit");
    assertEquals(te.getTrackedEntityType().getUid(), json.getTrackedEntityType());
    assertEquals(te.getOrganisationUnit().getUid(), json.getOrgUnit());
  }

  @Test
  void getTrackedEntityByIdWithAttributesReturnsTrackedEntityTypeAttributesOnly() {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    TrackedEntityAttribute tea =
        addTrackedEntityTypeAttributeValue(trackedEntity, ValueType.NUMBER, "12");
    addProgramAttributeValue(trackedEntity, program, ValueType.NUMBER, "24");

    JsonList<JsonAttribute> attributes =
        GET(
                "/tracker/trackedEntities/{id}?fields=attributes[attribute,value]",
                trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("attributes", JsonAttribute.class);

    assertAll(
        "include tracked entity type attributes only if no program query param is given",
        () ->
            assertEquals(
                1,
                attributes.size(),
                () -> String.format("expected 1 attribute instead got %s", attributes)),
        () -> assertEquals(tea.getUid(), attributes.get(0).getAttribute()),
        () -> assertEquals("12", attributes.get(0).getValue()));
  }

  @Test
  void getTrackedEntityByIdWithAttributesReturnsAllAttributes() {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    TrackedEntityAttribute tea =
        addTrackedEntityTypeAttributeValue(trackedEntity, ValueType.NUMBER, "12");
    TrackedEntityAttribute tea2 =
        addProgramAttributeValue(trackedEntity, program, ValueType.NUMBER, "24");

    JsonList<JsonAttribute> attributes =
        GET(
                "/tracker/trackedEntities/{id}?program={id}&fields=attributes[attribute,value]",
                trackedEntity.getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("attributes", JsonAttribute.class);

    assertContainsAll(
        List.of(tea.getUid(), tea2.getUid()), attributes, JsonAttribute::getAttribute);
    assertContainsAll(List.of("12", "24"), attributes, JsonAttribute::getValue);
  }

  @Test
  void
      shouldGetTrackedEntityWithNoRelationshipsWhenTrackedEntityIsOnTheToSideOfAUnidirectionalRelationship() {
    TrackedEntity to = trackedEntity(trackedEntityType);
    relationship(trackedEntity(), to);
    this.switchContextToUser(user);

    assertHasSize(
        1, to.getRelationshipItems(), "test expects a tracked entity with one relationship");

    JsonList<JsonRelationship> rels =
        GET("/tracker/trackedEntities/{id}?fields=relationships", to.getUid())
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    assertIsEmpty(rels.stream().toList());
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationships() {
    TrackedEntity from = trackedEntity();
    TrackedEntity to = trackedEntity();
    Relationship r = relationship(from, to);
    this.switchContextToUser(user);

    JsonList<JsonRelationship> rels =
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(1, rels.size());
    JsonRelationship relationship = assertFirstRelationship(r, rels);
    assertTrackedEntityWithinRelationship(from, relationship.getFrom());
    assertTrackedEntityWithinRelationship(to, relationship.getTo());
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipType() {
    TrackedEntity from = trackedEntity();
    TrackedEntity to = trackedEntity();
    relationship(relationshipTypeNotAccessible(), fromTrackedEntity(from), toTrackedEntity(to));
    this.switchContextToUser(user);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(
        0,
        relationships.size(),
        "user needs access to relationship type to access the relationship");
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemTo() {
    TrackedEntity from = trackedEntity();
    TrackedEntity to = trackedEntityNotInSearchScope();
    relationship(from, to);
    this.switchContextToUser(user);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(
        0,
        relationships.size(),
        "user needs access to from and to items to access the relationship");
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToBothRelationshipItems() {
    TrackedEntity from = trackedEntityNotInSearchScope();
    TrackedEntity to = trackedEntityNotInSearchScope();
    relationship(from, to);
    this.switchContextToUser(user);

    assertTrue(
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage()
            .contains("User has no access to TrackedEntity"));
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemFrom() {
    TrackedEntity from = trackedEntityNotInSearchScope();
    TrackedEntity to = trackedEntity();
    relationship(from, to);
    this.switchContextToUser(user);

    assertTrue(
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage()
            .contains("User has no access to TrackedEntity"));
  }

  @Test
  void getTrackedEntityByIdyWithFieldsRelationshipsNoAccessToTrackedEntityType() {
    TrackedEntityType type = trackedEntityTypeNotAccessible();
    TrackedEntity from = trackedEntity(type);
    TrackedEntity to = trackedEntity(type);
    relationship(from, to);
    this.switchContextToUser(user);

    assertTrue(
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .error(HttpStatus.FORBIDDEN)
            .getMessage()
            .contains("User has no access to TrackedEntity"));
  }

  @Test
  void getTrackedEntityByIdNotFound() {
    assertEquals(
        "TrackedEntity with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/trackedEntities/Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenGettingASoftDeletedTrackedEntityById() {
    assertEquals(
        HttpStatus.NOT_FOUND,
        GET("/tracker/trackedEntities/" + softDeletedTrackedEntity.getUid()).status());
  }

  @Test
  void getTrackedEntityReturnsCsvFormat() {
    injectSecurityContextUser(user);

    HttpResponse response =
        GET(
            "/tracker/trackedEntities.csv?program={programId}&orgUnitMode={orgUnitMode}",
            program.getUid(),
            ACCESSIBLE);

    assertEquals(HttpStatus.OK, response.status());

    assertAll(
        () -> assertTrue(response.header("content-type").contains(ContextUtils.CONTENT_TYPE_CSV)),
        () ->
            assertTrue(
                response.header("content-disposition").contains("filename=trackedEntities.csv")),
        () ->
            assertTrue(response.content().toString().contains("trackedEntity,trackedEntityType")));
  }

  @Test
  void getTrackedEntityCsvById() {
    TrackedEntity te = trackedEntity();
    this.switchContextToUser(user);

    HttpResponse response =
        GET("/tracker/trackedEntities/{id}", te.getUid(), Accept(ContextUtils.CONTENT_TYPE_CSV));

    String csvResponse = response.content(ContextUtils.CONTENT_TYPE_CSV);

    assertTrue(response.header("content-type").contains(ContextUtils.CONTENT_TYPE_CSV));
    assertTrue(response.header("content-disposition").contains("filename=trackedEntity.csv"));
    assertEquals(trackedEntityToCsv(te), csvResponse);
  }

  String trackedEntityToCsv(TrackedEntity te) {
    return """
       trackedEntity,trackedEntityType,createdAt,createdAtClient,updatedAt,updatedAtClient,orgUnit,inactive,deleted,potentialDuplicate,geometry,latitude,longitude,storedBy,createdBy,updatedBy,attrCreatedAt,attrUpdatedAt,attribute,displayName,value,valueType
       """
        .concat(
            String.join(
                ",",
                te.getUid(),
                te.getTrackedEntityType().getUid(),
                DateUtils.instantFromDate(te.getCreated()).toString(),
                DateUtils.instantFromDate(te.getCreatedAtClient()).toString(),
                DateUtils.instantFromDate(te.getLastUpdated()).toString(),
                DateUtils.instantFromDate(te.getLastUpdatedAtClient()).toString(),
                te.getOrganisationUnit().getUid(),
                Boolean.toString(te.isInactive()),
                Boolean.toString(te.isDeleted()),
                Boolean.toString(te.isPotentialDuplicate()),
                ",,,,,,,,,,," + "\n"));
  }

  @Test
  void getTrackedEntityReturnsCsvZipFormat() {
    injectSecurityContextUser(user);

    HttpResponse response =
        GET(
            "/tracker/trackedEntities.csv.zip?program={programId}&orgUnitMode={orgUnitMode}",
            program.getUid(),
            ACCESSIBLE);

    assertEquals(HttpStatus.OK, response.status());

    assertAll(
        () ->
            assertTrue(response.header("content-type").contains(ContextUtils.CONTENT_TYPE_CSV_ZIP)),
        () ->
            assertTrue(
                response
                    .header("content-disposition")
                    .contains("filename=trackedEntities.csv.zip")),
        () -> assertNotNull(response.content(ContextUtils.CONTENT_TYPE_CSV_ZIP)));
  }

  @Test
  void getTrackedEntityReturnsCsvGZipFormat() {
    injectSecurityContextUser(user);

    HttpResponse response =
        GET(
            "/tracker/trackedEntities.csv.gz?program={programId}&orgUnitMode={orgUnitMode}",
            program.getUid(),
            ACCESSIBLE);

    assertEquals(HttpStatus.OK, response.status());

    assertAll(
        () ->
            assertTrue(
                response.header("content-type").contains(ContextUtils.CONTENT_TYPE_CSV_GZIP)),
        () ->
            assertTrue(
                response.header("content-disposition").contains("filename=trackedEntities.csv.gz")),
        () -> assertNotNull(response.content(ContextUtils.CONTENT_TYPE_CSV_GZIP)));
  }

  @Test
  void shouldGetEnrollmentWhenFieldsHasEnrollments() {
    TrackedEntity trackedEntity = trackedEntity();
    Enrollment enrollment = enroll(trackedEntity, program, orgUnit);

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?fields=enrollments", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse(json, enrollment);

    assertTrue(jsonEnrollment.getArray("relationships").isEmpty());
    assertTrue(jsonEnrollment.getAttributes().isEmpty());
    assertTrue(jsonEnrollment.getEvents().isEmpty());
  }

  @Test
  void shouldGetNoEventRelationshipsWhenEventsHasNoRelationshipsAndFieldsIncludeAll() {
    TrackedEntity trackedEntity = trackedEntity();

    Enrollment enrollment = enroll(trackedEntity, program, orgUnit);

    Event event = eventWithDataValue(enrollment);

    enrollment.getEvents().add(event);
    manager.update(enrollment);

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?fields=enrollments", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse(json, enrollment);
    assertTrue(jsonEnrollment.getArray("relationships").isEmpty());
    assertTrue(jsonEnrollment.getAttributes().isEmpty());

    JsonEvent jsonEvent = assertDefaultEventResponse(jsonEnrollment, event);

    assertTrue(jsonEvent.getRelationships().isEmpty());
  }

  @Test
  void shouldGetEventRelationshipsWhenEventHasRelationshipsAndFieldsIncludeEventRelationships() {
    TrackedEntity trackedEntity = trackedEntity();

    Enrollment enrollment = enroll(trackedEntity, program, orgUnit);

    Event event = eventWithDataValue(enrollment);
    enrollment.getEvents().add(event);
    manager.update(enrollment);

    Relationship teToEventRelationship = relationship(trackedEntity, event);

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?fields=enrollments", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse(json, enrollment);
    assertTrue(jsonEnrollment.getAttributes().isEmpty());
    assertTrue(jsonEnrollment.getArray("relationships").isEmpty());

    JsonEvent jsonEvent = assertDefaultEventResponse(jsonEnrollment, event);

    JsonRelationship relationship = jsonEvent.getRelationships().get(0);

    assertEquals(teToEventRelationship.getUid(), relationship.getRelationship());
    assertEquals(
        trackedEntity.getUid(), relationship.getFrom().getTrackedEntity().getTrackedEntity());
    assertEquals(event.getUid(), relationship.getTo().getEvent().getEvent());
  }

  @Test
  void shouldGetNoEventRelationshipsWhenEventHasRelationshipsAndFieldsExcludeEventRelationships() {
    TrackedEntity trackedEntity = trackedEntity();

    Enrollment enrollment = enroll(trackedEntity, program, orgUnit);

    Event event = eventWithDataValue(enrollment);

    enrollment.getEvents().add(event);
    manager.update(enrollment);

    relationship(trackedEntity, event);

    JsonList<JsonEnrollment> json =
        GET(
                "/tracker/trackedEntities/{id}?fields=enrollments[*,events[!relationships]]",
                trackedEntity.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse(json, enrollment);
    assertTrue(jsonEnrollment.getAttributes().isEmpty());
    assertTrue(jsonEnrollment.getArray("relationships").isEmpty());

    JsonEvent jsonEvent = assertDefaultEventResponse(jsonEnrollment, event);

    assertHasNoMember(jsonEvent, "relationships");
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramGivenProgramAttribute() throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("text/plain", "file content");
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());

    this.switchContextToUser(user);

    HttpResponse response =
        GET(
            "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
            trackedEntity.getUid(),
            tea.getUid(),
            program.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals("file content", response.content("text/plain"));
  }

  @Test
  void getAttributeValuesFileByAttributeGivenTrackedEntityTypeAttribute() throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();

    FileResource file = storeFile("text/plain", "file content");
    TrackedEntityAttribute tea =
        addTrackedEntityTypeAttributeValue(trackedEntity, ValueType.FILE_RESOURCE, file.getUid());

    this.switchContextToUser(user);

    HttpResponse response =
        GET(
            "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file",
            trackedEntity.getUid(),
            tea.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals("file content", response.content("text/plain"));
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramGivenTrackedEntityTypeAttribute()
      throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file1 = storeFile("text/plain", "file content");
    TrackedEntityAttribute tetTea =
        addTrackedEntityTypeAttributeValue(trackedEntity, ValueType.FILE_RESOURCE, file1.getUid());

    FileResource file2 = storeFile("text/plain", "file content", 'B');
    addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file2.getUid());

    this.switchContextToUser(user);

    assertStartsWith(
        "TrackedEntityAttribute with id " + tetTea.getUid(),
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tetTea.getUid(),
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeGivenProgramAttribute() throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file1 = storeFile("text/plain", "file content");
    addTrackedEntityTypeAttributeValue(trackedEntity, ValueType.FILE_RESOURCE, file1.getUid());

    FileResource file2 = storeFile("text/plain", "file content", 'B');
    TrackedEntityAttribute programTea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file2.getUid());

    this.switchContextToUser(user);

    assertStartsWith(
        "TrackedEntityAttribute with id " + programTea.getUid(),
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file",
                trackedEntity.getUid(),
                programTea.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeIfGivenParameterDimension() {
    assertStartsWith(
        "Request parameter 'dimension'",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?dimension=small",
                CodeGenerator.generateUid(),
                CodeGenerator.generateUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfAttributeIsNotFound() throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("text/plain", "file content");
    addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());

    String attributeUid = CodeGenerator.generateUid();

    assertStartsWith(
        "TrackedEntityAttribute with id " + attributeUid,
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                attributeUid,
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfUserDoesNotHaveDataReadAccessToProgram()
      throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("text/plain", "file content");
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());

    // remove access
    program.getSharing().setUserAccesses(Set.of());
    program.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(program, false);

    this.switchContextToUser(user);

    assertStartsWith(
        "Program",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfAttributeIsNotAFile() {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.BOOLEAN, "true");

    assertStartsWith(
        "Tracked entity attribute " + tea.getUid() + " is not a file",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfProgramDoesNotExist() throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("text/plain", "file content");
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());

    String programUid = CodeGenerator.generateUid();

    assertStartsWith(
        "Program",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                programUid)
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void
      getAttributeValuesFileByAttributeAndProgramIfUserDoesNotHaveDataReadAccessToTrackedEntityType()
          throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("text/plain", "file content");
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());

    // remove public access
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    trackedEntityType.getSharing().setUserAccesses(Set.of());
    manager.save(trackedEntityType, false);

    this.switchContextToUser(user);

    assertStartsWith(
        "TrackedEntity ",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeIfUserDoesNotHaveDataReadAccessToTrackedEntityType()
      throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("text/plain", "file content");
    TrackedEntityAttribute tea =
        addTrackedEntityTypeAttributeValue(trackedEntity, ValueType.FILE_RESOURCE, file.getUid());

    // remove public access
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    trackedEntityType.getSharing().setUserAccesses(Set.of());
    manager.save(trackedEntityType, false);

    this.switchContextToUser(user);

    assertStartsWith(
        "TrackedEntity ",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file",
                trackedEntity.getUid(),
                tea.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfNoAttributeValueExists() {
    TrackedEntity trackedEntity = trackedEntity();

    TrackedEntityAttribute tea = programAttribute(program, ValueType.FILE_RESOURCE);

    enroll(trackedEntity, program, orgUnit);

    assertStartsWith(
        "Attribute value for tracked entity attribute " + tea.getUid(),
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfFileResourceIsInDbButNotInTheStore() {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = createFileResource('A', "file content".getBytes());
    manager.save(file, false);
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());

    GET(
            "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
            trackedEntity.getUid(),
            tea.getUid(),
            program.getUid())
        .error(HttpStatus.CONFLICT);
  }

  @Test
  void getAttributeValuesFileByAttributeAndProgramIfFileIsNotFound() {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    String fileUid = CodeGenerator.generateUid();
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, fileUid);

    assertStartsWith(
        "FileResource with id " + fileUid,
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                program.getUid())
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getAttributeValuesImageByProgramAttribute() throws ConflictException {
    TrackedEntity trackedEntity = trackedEntity();
    enroll(trackedEntity, program, orgUnit);

    FileResource file = storeFile("image/png", "file content");
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.IMAGE, file.getUid());

    this.switchContextToUser(user);

    HttpResponse response =
        GET(
            "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/image?program={programUid}",
            trackedEntity.getUid(),
            tea.getUid(),
            program.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals("filename=" + file.getName(), response.header("Content-Disposition"));
    assertEquals(Long.toString(file.getContentLength()), response.header("Content-Length"));
    assertEquals("file content", response.content("image/png"));
  }

  private Event eventWithDataValue(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit());
    event.setAutoFields();
    event.setOccurredDate(DateUtils.parseDate(EVENT_OCCURRED_AT));

    dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.TEXT);
    manager.save(dataElement);

    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setValue("value");
    eventDataValue.setDataElement(dataElement.getUid());
    eventDataValue.setCreatedByUserInfo(UserInfoSnapshot.from(user));
    eventDataValue.setLastUpdatedByUserInfo(UserInfoSnapshot.from(user));
    Set<EventDataValue> eventDataValues = Set.of(eventDataValue);
    event.setEventDataValues(eventDataValues);

    manager.save(event);
    return event;
  }

  private JsonEnrollment assertDefaultEnrollmentResponse(
      JsonList<JsonEnrollment> enrollments, Enrollment enrollment) {
    assertFalse(enrollments.isEmpty());
    JsonEnrollment jsonEnrollment = enrollments.get(0);

    assertHasMember(jsonEnrollment, "enrollment");

    assertEquals(enrollment.getUid(), jsonEnrollment.getEnrollment());
    assertEquals(enrollment.getTrackedEntity().getUid(), jsonEnrollment.getTrackedEntity());
    assertEquals(program.getUid(), jsonEnrollment.getProgram());
    assertEquals("ACTIVE", jsonEnrollment.getStatus());
    assertEquals(orgUnit.getUid(), jsonEnrollment.getOrgUnit());
    assertFalse(jsonEnrollment.getBoolean("deleted").booleanValue());
    assertHasMember(jsonEnrollment, "enrolledAt");
    assertHasMember(jsonEnrollment, "occurredAt");
    assertHasMember(jsonEnrollment, "createdAt");
    assertHasMember(jsonEnrollment, "createdAtClient");
    assertHasMember(jsonEnrollment, "updatedAt");
    assertHasMember(jsonEnrollment, "notes");
    assertHasMember(jsonEnrollment, "followUp");

    return jsonEnrollment;
  }

  private JsonEvent assertDefaultEventResponse(JsonEnrollment enrollment, Event event) {
    assertTrue(enrollment.isObject());
    assertFalse(enrollment.isEmpty());

    JsonEvent jsonEvent = enrollment.getEvents().get(0);

    assertEquals(event.getUid(), jsonEvent.getEvent());
    assertEquals(event.getProgramStage().getUid(), jsonEvent.getProgramStage());
    assertEquals(event.getEnrollment().getUid(), jsonEvent.getEnrollment());
    assertEquals(program.getUid(), jsonEvent.getProgram());
    assertEquals("ACTIVE", jsonEvent.getStatus());
    assertEquals(orgUnit.getUid(), jsonEvent.getOrgUnit());
    assertFalse(jsonEvent.getDeleted());
    assertHasMember(jsonEvent, "createdAt");
    assertHasMember(jsonEvent, "occurredAt");
    assertEquals(EVENT_OCCURRED_AT, jsonEvent.getString("occurredAt").string());
    assertHasMember(jsonEvent, "createdAtClient");
    assertHasMember(jsonEvent, "updatedAt");
    assertHasMember(jsonEvent, "notes");
    assertHasMember(jsonEvent, "followUp");
    assertHasMember(jsonEvent, "followup");

    JsonDataValue dataValue = jsonEvent.getDataValues().get(0);

    assertEquals(dataElement.getUid(), dataValue.getDataElement());
    assertEquals(event.getEventDataValues().iterator().next().getValue(), dataValue.getValue());
    assertHasMember(dataValue, "createdAt");
    assertHasMember(dataValue, "updatedAt");
    assertHasMember(dataValue, "createdBy");
    assertHasMember(dataValue, "updatedBy");

    return jsonEvent;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().setOwner(owner);
    type.getSharing().addUserAccess(userAccess(user));
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
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

  private Enrollment enroll(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    return enrollmentService.enrollTrackedEntity(
        trackedEntity, program, new Date(), new Date(), orgUnit);
  }

  private UserAccess userAccess(User user) {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private RelationshipType relationshipTypeAccessible(
      RelationshipEntity from, RelationshipEntity to) {
    RelationshipType type = relationshipType(from, to);
    type.getSharing().addUserAccess(userAccess(user));
    manager.save(type, false);
    return type;
  }

  private RelationshipType relationshipTypeNotAccessible() {
    return relationshipType(
        RelationshipEntity.TRACKED_ENTITY_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE);
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

  private Relationship relationship(TrackedEntity from, TrackedEntity to) {
    RelationshipType type =
        relationshipTypeAccessible(
            RelationshipEntity.TRACKED_ENTITY_INSTANCE, RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    return relationship(type, fromTrackedEntity(from), toTrackedEntity(to));
  }

  private Relationship relationship(TrackedEntity from, Event to) {
    RelationshipType type =
        relationshipTypeAccessible(
            RelationshipEntity.TRACKED_ENTITY_INSTANCE, RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    RelationshipItem fromItem = fromTrackedEntity(from);
    RelationshipItem toItem = toEvent(to);
    Relationship relationship = relationship(type, fromItem, toItem);
    fromItem.setRelationship(relationship);
    toItem.setRelationship(relationship);
    to.getRelationshipItems().add(toItem);
    manager.save(to, false);
    manager.save(relationship, false);
    return relationship;
  }

  private Relationship relationship(
      RelationshipType type, RelationshipItem fromItem, RelationshipItem toItem) {
    Relationship r = new Relationship();

    r.setTo(toItem);
    toItem.setRelationship(r);

    r.setFrom(fromItem);
    fromItem.setRelationship(r);

    r.setRelationshipType(type);
    r.setKey(type.getUid());
    r.setInvertedKey(type.getUid());
    r.setAutoFields();
    r.getSharing().setOwner(owner);
    manager.save(r, false);
    return r;
  }

  private RelationshipItem fromTrackedEntity(TrackedEntity from) {
    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setTrackedEntity(from);
    from.getRelationshipItems().add(fromItem);
    return fromItem;
  }

  private RelationshipItem toTrackedEntity(TrackedEntity to) {
    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntity(to);
    to.getRelationshipItems().add(toItem);
    return toItem;
  }

  private RelationshipItem toEvent(Event to) {
    RelationshipItem toItem = new RelationshipItem();
    toItem.setEvent(to);
    to.getRelationshipItems().add(toItem);
    return toItem;
  }

  private void assertTrackedEntityWithinRelationship(
      TrackedEntity expected, JsonRelationshipItem json) {
    JsonRelationshipItem.JsonTrackedEntity jsonTe = json.getTrackedEntity();
    assertFalse(jsonTe.isEmpty(), "trackedEntity should not be empty");
    assertEquals(expected.getUid(), jsonTe.getTrackedEntity());
    assertHasNoMember(json, "trackedEntityType");
    assertHasNoMember(json, "orgUnit");
    assertHasNoMember(json, "relationships"); // relationships are not
    // returned within
    // relationships
    assertTrue(jsonTe.getArray("attributes").isEmpty());
  }

  private TrackedEntityAttribute addTrackedEntityTypeAttributeValue(
      TrackedEntity trackedEntity, ValueType type, String value) {
    TrackedEntityAttribute tea =
        trackedEntityTypeAttribute(trackedEntity.getTrackedEntityType(), type);
    trackedEntity.addAttributeValue(attributeValue(tea, trackedEntity, value));
    manager.save(trackedEntity, false);
    return tea;
  }

  private TrackedEntityAttribute addProgramAttributeValue(
      TrackedEntity trackedEntity, Program program, ValueType type, String value) {
    TrackedEntityAttribute tea = programAttribute(program, type);
    trackedEntity.addAttributeValue(attributeValue(tea, trackedEntity, value));
    manager.save(trackedEntity, false);
    return tea;
  }

  private TrackedEntityAttributeValue attributeValue(
      TrackedEntityAttribute tea, TrackedEntity te, String value) {
    return new TrackedEntityAttributeValue(tea, te, value);
  }

  private TrackedEntityAttribute programAttribute(Program program, ValueType type) {
    TrackedEntityAttribute tea = trackedEntityAttribute(type);
    program.getProgramAttributes().add(createProgramTrackedEntityAttribute(program, tea));
    manager.save(program, false);
    return tea;
  }

  private TrackedEntityAttribute trackedEntityTypeAttribute(
      TrackedEntityType trackedEntityType, ValueType type) {
    TrackedEntityAttribute tea = trackedEntityAttribute(type);
    TrackedEntityTypeAttribute teta = new TrackedEntityTypeAttribute(trackedEntityType, tea);
    manager.save(teta, false);
    trackedEntityType.getTrackedEntityTypeAttributes().add(teta);
    manager.save(trackedEntityType, false);
    return tea;
  }

  private TrackedEntityAttribute trackedEntityAttribute(ValueType type) {
    TrackedEntityAttribute tea =
        createTrackedEntityAttribute(UNIQUE_CHARS.charAt(uniqueAttributeCharCounter++));
    tea.setValueType(type);
    manager.save(tea, false);
    return tea;
  }

  private FileResource storeFile(String contentType, String content) throws ConflictException {
    return storeFile(contentType, content, 'A');
  }

  private FileResource storeFile(String contentType, String content, char uniqueChar)
      throws ConflictException {
    byte[] data = content.getBytes();
    FileResource fr = createFileResource(uniqueChar, data);
    fr.setContentType(contentType);
    fileResourceService.syncSaveFileResource(fr, data);
    fr.setStorageStatus(FileResourceStorageStatus.STORED);
    return fr;
  }
}
