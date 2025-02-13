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
import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContainsAll;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntitiesExportControllerTest extends PostgresControllerIntegrationTestBase {
  // Used to generate unique chars for creating test objects like TEA, ...
  private static final String UNIQUE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  protected ObjectBundle setUpMetadata(String path) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    assertNoErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
    return bundle;
  }

  protected TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
  }

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));

    manager.flush();
    manager.clear();
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @Autowired private FileResourceService fileResourceService;

  private OrganisationUnit orgUnit;

  private OrganisationUnit anotherOrgUnit;

  private Program program;

  private ProgramStage programStage;

  private TrackedEntityType trackedEntityType;

  private User owner;

  private User user;

  private TrackedEntity softDeletedTrackedEntity;

  // Used to generate unique chars for creating TEA in test setup
  private int uniqueAttributeCharCounter = 0;

  @BeforeEach
  void setUpToBeMigrated() {
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
    program.getSharing().addUserAccess(userAccess());
    manager.save(program, false);

    programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(owner);
    programStage.getSharing().addUserAccess(userAccess());
    manager.save(programStage, false);

    trackedEntityType = trackedEntityTypeAccessible();
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    softDeletedTrackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    softDeletedTrackedEntity.setDeleted(true);
    manager.save(softDeletedTrackedEntity, false);
  }

  @Test
  void getTrackedEntitiesNeedsProgramOrType() {
    assertEquals(
        "Either `program`, `trackedEntityType` or `trackedEntities` should be specified",
        GET("/tracker/trackedEntities").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void getTrackedEntitiesNeedsProgramOrTrackedEntityType() {
    assertEquals(
        "Either `program`, `trackedEntityType` or `trackedEntities` should be specified",
        GET("/tracker/trackedEntities?orgUnit={ou}", orgUnit.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldReturnEmptyListWhenGettingTrackedEntitiesWithNoMatchingParams() {
    LocalDate futureDate = LocalDate.now().plusYears(1);
    JsonList<JsonTrackedEntity> trackedEntities =
        GET("/tracker/trackedEntities?trackedEntityType="
                + trackedEntityType.getUid()
                + "&ouMode=ALL"
                + "&trackedEntities=AbjwFr5o9IT"
                + "&updatedAfter="
                + futureDate)
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    assertEquals(0, trackedEntities.size());
  }

  @Test
  void getTrackedEntityById() {
    TrackedEntity te = get(TrackedEntity.class, "QS6w44flWAf");

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
    TrackedEntity te = get(TrackedEntity.class, "QS6w44flWAf");

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
    TrackedEntity te = get(TrackedEntity.class, "dUE514NMOlo");
    // TETA
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "numericAttr");
    TrackedEntityAttribute tea2 = get(TrackedEntityAttribute.class, "toUpdate000");

    JsonList<JsonAttribute> attributes =
        GET("/tracker/trackedEntities/{id}?fields=attributes[attribute,value]", te.getUid())
            .content(HttpStatus.OK)
            .getList("attributes", JsonAttribute.class);

    assertContainsAll(
        List.of(tea1.getUid(), tea2.getUid()), attributes, JsonAttribute::getAttribute);
    assertContainsAll(List.of("rainy day", "70"), attributes, JsonAttribute::getValue);
  }

  @Test
  void getTrackedEntityByIdWithAttributesReturnsAllAttributes() {
    TrackedEntity te = get(TrackedEntity.class, "dUE514NMOlo");
    assertNotEmpty(te.getEnrollments(), "test expects a tracked entity with an enrollment");
    String program = te.getEnrollments().iterator().next().getProgram().getUid();
    // TETA
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "numericAttr");
    TrackedEntityAttribute tea2 = get(TrackedEntityAttribute.class, "toUpdate000");
    // PTEA
    TrackedEntityAttribute tea3 = get(TrackedEntityAttribute.class, "dIVt4l5vIOa");

    JsonList<JsonAttribute> attributes =
        GET(
                "/tracker/trackedEntities/{id}?program={id}&fields=attributes[attribute,value]",
                te.getUid(),
                program)
            .content(HttpStatus.OK)
            .getList("attributes", JsonAttribute.class);

    assertContainsAll(
        List.of(tea1.getUid(), tea2.getUid(), tea3.getUid()),
        attributes,
        JsonAttribute::getAttribute);
    assertContainsAll(
        List.of("rainy day", "70", "Frank PTEA"), attributes, JsonAttribute::getValue);
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationships() {
    TrackedEntity from = get(TrackedEntity.class, "mHWCacsGYYn");
    assertHasSize(
        1, from.getRelationshipItems(), "test expects a tracked entity with one relationship");
    RelationshipItem relItem = from.getRelationshipItems().iterator().next();
    Relationship r = get(Relationship.class, relItem.getRelationship().getUid());
    Event to = r.getTo().getEvent();

    JsonList<JsonRelationship> rels =
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship relationship = assertFirstRelationship(r, rels);
    assertTrackedEntityWithinRelationship(from, relationship.getFrom());
    assertTrackedEntityWithinRelationship(to, relationship.getTo());
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationshipsFromTEToEnrollment() {
    TrackedEntity from = get(TrackedEntity.class, "guVNoAerxWo");
    assertHasSize(
        1, from.getRelationshipItems(), "test expects a tracked entity with one relationship");
    RelationshipItem relItem = from.getRelationshipItems().iterator().next();
    Relationship r = get(Relationship.class, relItem.getRelationship().getUid());
    Enrollment to = r.getTo().getEnrollment();

    JsonList<JsonRelationship> rels =
        GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship relationship = assertFirstRelationship(r, rels);
    assertTrackedEntityWithinRelationship(from, relationship.getFrom());
    assertTrackedEntityWithinRelationship(to, relationship.getTo());
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemTo() {
    TrackedEntity from = get(TrackedEntity.class, "mHWCacsGYYn");
    assertNotEmpty(
        from.getRelationshipItems(),
        "test expects a tracked entity with at least one relationship");

    User user = userService.getUser("Z7870757a75");
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
  void getTrackedEntityByIdWithFieldsRelationshipsNoAccessToRelationshipItemFrom() {
    TrackedEntity to = get(TrackedEntity.class, "QesgJkTyTCk");
    assertNotEmpty(
        to.getRelationshipItems(), "test expects a tracked entity with at least one relationship");

    User user = userService.getUser("Z7870757a75");
    this.switchContextToUser(user);

    JsonList<JsonRelationship> relationships =
        GET("/tracker/trackedEntities/{id}?fields=relationships", to.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    assertEquals(
        0,
        relationships.size(),
        "user needs access to from and to items to access the relationship");
  }

  @Test
  void getTrackedEntityByIdyWithFieldsRelationshipsNoAccessToTrackedEntityType() {
    TrackedEntityType type = trackedEntityTypeNotAccessible();
    TrackedEntity from = trackedEntity(type);
    TrackedEntity to = trackedEntity(type);
    relationship(from, to);
    this.switchContextToUser(user);

    GET("/tracker/trackedEntities/{id}?fields=relationships", from.getUid())
        .error(HttpStatus.FORBIDDEN);
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
    Program program = get(Program.class, "BFcipDERJnf");

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
    TrackedEntity te = get(TrackedEntity.class, "QS6w44flWAf");
    List<TrackedEntityAttributeValue> trackedEntityTypeAttributeValues =
        te.getTrackedEntityAttributeValues().stream()
            .filter(teav -> !"toDelete000".equals(teav.getAttribute().getUid()))
            .toList();
    assertHasSize(
        2,
        trackedEntityTypeAttributeValues,
        "test expects the tracked entity to have 2 tracked entity type attribute values");

    HttpResponse response =
        GET("/tracker/trackedEntities/{id}", te.getUid(), Accept(ContextUtils.CONTENT_TYPE_CSV));

    String csvResponse = response.content(ContextUtils.CONTENT_TYPE_CSV);

    assertTrue(response.header("content-type").contains(ContextUtils.CONTENT_TYPE_CSV));
    assertTrue(response.header("content-disposition").contains("filename=trackedEntity.csv"));
    assertStartsWith(
        """
trackedEntity,trackedEntityType,createdAt,createdAtClient,updatedAt,updatedAtClient,orgUnit,inactive,deleted,potentialDuplicate,geometry,latitude,longitude,storedBy,createdBy,updatedBy,attrCreatedAt,attrUpdatedAt,attribute,displayName,value,valueType
""",
        csvResponse);
    // TEAV order is not deterministic
    assertContains(trackedEntityToCsv(te, trackedEntityTypeAttributeValues.get(0)), csvResponse);
    assertContains(trackedEntityToCsv(te, trackedEntityTypeAttributeValues.get(1)), csvResponse);
  }

  String trackedEntityToCsv(TrackedEntity te, TrackedEntityAttributeValue attributeValue) {
    String value = attributeValue.getValue();
    if (attributeValue.getAttribute().getValueType() == ValueType.TEXT) {
      value = "\"" + value + "\"";
    }
    return String.join(
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
            ",,,",
            importUser.getUsername(),
            importUser.getUsername())
        + ","
        + String.join(
            ",",
            DateUtils.instantFromDate(attributeValue.getCreated()).toString(),
            DateUtils.instantFromDate(attributeValue.getLastUpdated()).toString(),
            attributeValue.getAttribute().getUid(),
            attributeValue.getAttribute().getDisplayName(),
            value,
            attributeValue.getAttribute().getValueType().name());
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
    TrackedEntity te = get(TrackedEntity.class, "dUE514NMOlo");
    assertHasSize(1, te.getEnrollments(), "test expects a tracked entity with one enrollment");
    Enrollment enrollment = te.getEnrollments().iterator().next();

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?fields=enrollments", te.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    assertDefaultEnrollmentResponse(json, enrollment);
  }

  @Test
  void shouldGetNoEventRelationshipsWhenEventsHasNoRelationshipsAndFieldsIncludeAll() {
    TrackedEntity te = get(TrackedEntity.class, "mHWCacsGYYn");
    assertHasSize(1, te.getEnrollments(), "test expects a tracked entity with one enrollment");
    Enrollment enrollment = te.getEnrollments().iterator().next();
    assertHasSize(1, enrollment.getEvents(), "test expects an enrollment with one event");
    Event event = enrollment.getEvents().iterator().next();
    assertIsEmpty(event.getRelationshipItems(), "test expects an event with no relationships");

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?fields=enrollments", te.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    JsonEnrollment jsonEnrollment = assertDefaultEnrollmentResponse(json, enrollment);
    JsonEvent jsonEvent = assertDefaultEventResponse(jsonEnrollment, event);
    assertTrue(jsonEvent.getRelationships().isEmpty());
  }

  @Test
  void shouldGetEventRelationshipsWhenEventHasRelationshipsAndFieldsIncludeEventRelationships() {
    TrackedEntity te = get(TrackedEntity.class, "QS6w44flWAf");
    Enrollment enrollment = get(Enrollment.class, "nxP7UnKhomJ");
    assertHasSize(1, enrollment.getEvents(), "test expects an enrollment with one event");
    Event event = enrollment.getEvents().iterator().next();
    assertNotEmpty(
        event.getRelationshipItems(), "test expects an event with at least one relationship");
    RelationshipItem relItem = te.getRelationshipItems().iterator().next();
    Relationship r = get(Relationship.class, relItem.getRelationship().getUid());

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?fields=enrollments", te.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    List<JsonEnrollment> enrollments =
        json.stream().filter(en -> "nxP7UnKhomJ".equals(en.getEnrollment())).toList();
    assertNotEmpty(
        enrollments,
        () -> String.format("Expected enrollment \"nxP7UnKhomJ\" instead got %s", enrollments));
    JsonEnrollment jsonEnrollment = enrollments.get(0);
    assertDefaultEnrollmentResponse(enrollment, jsonEnrollment);

    JsonEvent jsonEvent = assertDefaultEventResponse(jsonEnrollment, event);
    assertTrue(
        jsonEvent
            .getRelationships()
            .contains(JsonRelationship::getRelationship, actual -> r.getUid().equals(actual)),
        () ->
            String.format(
                "Expected event to have relationship to TE instead got %s",
                jsonEvent.getRelationships().toJson()));
  }

  @Test
  void shouldGetNoEventRelationshipsWhenEventHasRelationshipsAndFieldsExcludeEventRelationships() {
    TrackedEntity te = get(TrackedEntity.class, "QS6w44flWAf");
    Enrollment enrollment = get(Enrollment.class, "nxP7UnKhomJ");
    assertHasSize(1, enrollment.getEvents(), "test expects an enrollment with one event");
    Event event = enrollment.getEvents().iterator().next();
    assertNotEmpty(
        event.getRelationshipItems(), "test expects an event with at least one relationship");

    JsonList<JsonEnrollment> json =
        GET(
                "/tracker/trackedEntities/{id}?fields=enrollments[*,events[!relationships]]",
                te.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    List<JsonEnrollment> enrollments =
        json.stream().filter(en -> "nxP7UnKhomJ".equals(en.getEnrollment())).toList();
    assertNotEmpty(
        enrollments,
        () -> String.format("Expected enrollment \"nxP7UnKhomJ\" instead got %s", enrollments));
    JsonEnrollment jsonEnrollment = enrollments.get(0);
    assertDefaultEnrollmentResponse(enrollment, jsonEnrollment);

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

  private JsonEnrollment assertDefaultEnrollmentResponse(
      JsonList<JsonEnrollment> enrollments, Enrollment enrollment) {
    assertFalse(enrollments.isEmpty());
    JsonEnrollment jsonEnrollment = enrollments.get(0);

    assertDefaultEnrollmentResponse(enrollment, jsonEnrollment);

    return jsonEnrollment;
  }

  private static void assertDefaultEnrollmentResponse(
      Enrollment enrollment, JsonEnrollment jsonEnrollment) {
    assertHasMember(jsonEnrollment, "enrollment");
    assertEquals(enrollment.getUid(), jsonEnrollment.getEnrollment());
    assertEquals(enrollment.getTrackedEntity().getUid(), jsonEnrollment.getTrackedEntity());
    assertEquals(enrollment.getProgram().getUid(), jsonEnrollment.getProgram());
    assertEquals(enrollment.getStatus().name(), jsonEnrollment.getStatus());
    assertEquals(enrollment.getOrganisationUnit().getUid(), jsonEnrollment.getOrgUnit());
    assertFalse(jsonEnrollment.getBoolean("deleted").booleanValue());
    assertHasMember(jsonEnrollment, "enrolledAt");
    assertHasMember(jsonEnrollment, "occurredAt");
    assertHasMember(jsonEnrollment, "createdAt");
    assertHasMember(jsonEnrollment, "createdAtClient");
    assertHasMember(jsonEnrollment, "updatedAt");
    assertHasMember(jsonEnrollment, "notes");
    assertHasMember(jsonEnrollment, "followUp");
  }

  private JsonEvent assertDefaultEventResponse(JsonEnrollment enrollment, Event event) {
    assertTrue(enrollment.isObject());
    assertFalse(enrollment.isEmpty());

    JsonEvent jsonEvent = enrollment.getEvents().get(0);

    assertEquals(event.getUid(), jsonEvent.getEvent());
    assertEquals(event.getProgramStage().getUid(), jsonEvent.getProgramStage());
    assertEquals(event.getEnrollment().getUid(), jsonEvent.getEnrollment());
    assertEquals(event.getProgramStage().getProgram().getUid(), jsonEvent.getProgram());
    assertEquals(event.getStatus().name(), jsonEvent.getStatus());
    assertEquals(event.getOrganisationUnit().getUid(), jsonEvent.getOrgUnit());
    assertFalse(jsonEvent.getDeleted());
    assertHasMember(jsonEvent, "createdAt");
    assertHasMember(jsonEvent, "occurredAt");
    assertHasMember(jsonEvent, "updatedAt");
    assertHasMember(jsonEvent, "notes");
    assertHasMember(jsonEvent, "followUp");

    return jsonEvent;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().setOwner(owner);
    type.getSharing().addUserAccess(userAccess());
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
    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(owner);
    return te;
  }

  private Enrollment enroll(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    Enrollment enrollment = createEnrollment(program, trackedEntity, orgUnit);
    manager.save(enrollment);
    trackedEntity.getEnrollments().add(enrollment);
    manager.update(trackedEntity);

    return enrollment;
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
    assertEquals(
        expected.getTrackedEntityAttributeValues().isEmpty(),
        jsonTe.getArray("attributes").isEmpty());
  }

  private void assertTrackedEntityWithinRelationship(
      Enrollment expected, JsonRelationshipItem json) {
    JsonRelationshipItem.JsonEnrollment jsonEnrollment = json.getEnrollment();
    assertFalse(jsonEnrollment.isEmpty(), "enrollment should not be empty");
    assertEquals(expected.getUid(), jsonEnrollment.getEnrollment());
    assertHasMember(jsonEnrollment, "events");
    assertHasNoMember(json, "trackedEntityType");
    assertHasNoMember(json, "orgUnit");
    assertHasNoMember(json, "relationships");
    assertHasNoMember(jsonEnrollment, "relationships");
    assertHasNoMember(jsonEnrollment.getEvents().get(0), "relationships"); // relationships are not
    // returned within
    // relationships
  }

  private void assertTrackedEntityWithinRelationship(Event expected, JsonRelationshipItem json) {
    JsonRelationshipItem.JsonEvent jsonEvent = json.getEvent();
    assertFalse(jsonEvent.isEmpty(), "event should not be empty");
    assertEquals(expected.getUid(), jsonEvent.getEvent());
    assertHasNoMember(json, "trackedEntityType");
    assertHasNoMember(json, "orgUnit");
    assertHasNoMember(json, "relationships"); // relationships are not
    // returned within
    // relationships
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

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(
        t,
        () ->
            String.format(
                "'%s' with uid '%s' should have been created", type.getSimpleName(), uid));
    return t;
  }

  public static void assertNoErrors(ImportReport report) {
    assertNotNull(report);
    assertEquals(
        Status.OK,
        report.getStatus(),
        errorMessage(
            "Expected import with status OK, instead got:%n", report.getValidationReport()));
  }

  private static Supplier<String> errorMessage(String errorTitle, ValidationReport report) {
    return () -> {
      StringBuilder msg = new StringBuilder(errorTitle);
      report
          .getErrors()
          .forEach(
              e -> {
                msg.append(e.getErrorCode());
                msg.append(": ");
                msg.append(e.getMessage());
                msg.append('\n');
              });
      return msg.toString();
    };
  }

  public static void assertNoErrors(ObjectBundleValidationReport report) {
    assertNotNull(report);
    List<String> errors = new ArrayList<>();
    report.forEachErrorReport(err -> errors.add(err.toString()));
    assertFalse(
        report.hasErrorReports(), String.format("Expected no errors, instead got: %s%n", errors));
  }

  public static JsonRelationship assertFirstRelationship(
      Relationship expected, JsonList<JsonRelationship> actual) {
    assertFalse(actual.isEmpty(), "relationships should not be empty");
    assertTrue(
        actual.size() >= 0,
        String.format("element %d does not exist in %d relationships elements", 0, actual.size()));
    JsonRelationship jsonRelationship = actual.get(0);
    assertRelationship(expected, jsonRelationship);
    return jsonRelationship;
  }

  public static void assertRelationship(Relationship expected, JsonRelationship actual) {
    assertFalse(actual.isEmpty(), "relationship should not be empty");
    assertEquals(expected.getUid(), actual.getRelationship(), "relationship UID");
    assertEquals(
        DateUtils.toIso8601NoTz(expected.getCreatedAtClient()),
        actual.getCreatedAtClient(),
        "createdAtClient date");
    assertEquals(
        expected.getRelationshipType().getUid(),
        actual.getRelationshipType(),
        "relationshipType UID");
  }
}
