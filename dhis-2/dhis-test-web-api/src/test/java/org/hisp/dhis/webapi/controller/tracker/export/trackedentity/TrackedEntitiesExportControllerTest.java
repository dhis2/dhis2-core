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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.webapi.Assertions.assertNoDiff;
import static org.hisp.dhis.webapi.controller.tracker.Assertions.assertNoErrors;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonDiff.Mode;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntitiesExportControllerTest extends PostgresControllerIntegrationTestBase {
  // Used to generate unique chars for creating test objects like TEA, ...
  private static final String UNIQUE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String TEA_MULTI_TEXT = "multitxtAtr";
  private static final String TE_UID_RBG = "QS6w44flWSS"; // "red,blue,Green" as multi text value
  private static final String TE_UID_RWY = "QS6w44flWTT"; // "red,white,yellow" as multi text value
  private static final String TE_UID_EMPTY_STRING = "QS6w44flWUU"; // "" as multi text value
  public static final List<String> TE_UID_NULL =
      List.of("mHWCacsGYYn", "dUE514NMOlo", "QS6w44flWAf"); // no value or null as multi text value

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  @Autowired private IdentifiableObjectManager manager;

  private TrackerObjects trackerObjects;

  private User importUser;

  @Autowired private TestSetup testSetup;

  @BeforeEach
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
    setUpToBeMigrated();

    manager.flush();
    manager.clear();
    testSetup.importTrackerData("tracker/tracker_multi_text_attribute_data.json");

    manager.flush();
    manager.clear();
    trackerObjects = testSetup.importTrackerData();
    manager.flush();
    manager.clear();

    deleteTrackedEntity(UID.of("woitxQbWYNq"));
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

  // Used to generate unique chars for creating TEA in test setup
  private int uniqueAttributeCharCounter = 0;

  private void setUpToBeMigrated() {
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
  void getTrackedEntityByPathIsIdenticalToQueryParam() {
    TrackedEntity trackedEntity = get(TrackedEntity.class, "QS6w44flWAf");

    JsonTrackedEntity pathTrackedEntity =
        GET("/tracker/trackedEntities/{id}?fields=*", trackedEntity.getUid())
            .content(HttpStatus.OK)
            .as(JsonTrackedEntity.class);
    JsonList<JsonTrackedEntity> queryTrackedEntity =
        GET(
                "/tracker/trackedEntities?fields=*&trackedEntities={id}&trackedEntityType={type}",
                trackedEntity.getUid(),
                trackedEntity.getTrackedEntityType().getUid())
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    assertHasSize(1, queryTrackedEntity.stream().toList());
    assertNoDiff(pathTrackedEntity, queryTrackedEntity.get(0), Mode.LENIENT);
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
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "integerAttr");
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
    TrackedEntityAttribute tea1 = get(TrackedEntityAttribute.class, "integerAttr");
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
  void
      shouldGetTrackedEntityWithoutRelationshipsWhenRelationshipIsDeletedAndIncludeDeletedIsFalse() {
    TrackedEntity from = get(TrackedEntity.class, "mHWCacsGYYn");
    assertHasSize(
        1, from.getRelationshipItems(), "test expects a tracked entity with one relationship");
    RelationshipItem relItem = from.getRelationshipItems().iterator().next();
    Relationship r = get(Relationship.class, relItem.getRelationship().getUid());
    manager.delete(r);

    JsonList<JsonRelationship> rels =
        GET(
                "/tracker/trackedEntities?trackedEntities={id}&fields=relationships&includeDeleted=false",
                from.getUid())
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    assertIsEmpty(rels.stream().toList());
  }

  @Test
  void shouldGetTrackedEntityWithRelationshipsWhenRelationshipIsDeletedAndIncludeDeletedIsTrue() {
    TrackedEntity from = get(TrackedEntity.class, "mHWCacsGYYn");
    assertHasSize(
        1, from.getRelationshipItems(), "test expects a tracked entity with one relationship");
    RelationshipItem relItem = from.getRelationshipItems().iterator().next();
    Relationship r = get(Relationship.class, relItem.getRelationship().getUid());
    manager.delete(r);
    SingleEvent to = r.getTo().getSingleEvent();

    JsonList<JsonRelationship> rels =
        GET(
                "/tracker/trackedEntities?trackedEntities={id}&fields=relationships&includeDeleted=true",
                from.getUid())
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship relationship = assertFirstRelationship(r, rels);
    assertTrackedEntityWithinRelationship(from, relationship.getFrom());
    assertTrackedEntityWithinRelationship(to, relationship.getTo());
  }

  @Test
  void
      shouldGetTrackedEntityWithNoRelationshipsWhenTrackedEntityIsOnTheToSideOfAUnidirectionalRelationship() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "m1575931405");
    relationshipType.setBidirectional(false);
    manager.update(relationshipType);

    TrackedEntity to = get(TrackedEntity.class, "QesgJkTyTCk");
    assertHasSize(
        1, to.getRelationshipItems(), "test expects a tracked entity with one relationship");

    JsonList<JsonRelationship> rels =
        GET("/tracker/trackedEntities?trackedEntities={id}&fields=relationships", to.getUid())
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    assertIsEmpty(rels.stream().toList());
  }

  @Test
  void getTrackedEntityByIdWithFieldsRelationships() {
    TrackedEntity from = get(TrackedEntity.class, "mHWCacsGYYn");
    assertHasSize(
        1, from.getRelationshipItems(), "test expects a tracked entity with one relationship");
    RelationshipItem relItem = from.getRelationshipItems().iterator().next();
    Relationship r = get(Relationship.class, relItem.getRelationship().getUid());
    SingleEvent to = r.getTo().getSingleEvent();

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
        .error(HttpStatus.NOT_FOUND);
  }

  @Test
  void getTrackedEntityByIdNotFound() {
    assertEquals(
        "TrackedEntity with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/trackedEntities/Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void shouldReturnNotFoundWhenGettingASoftDeletedTrackedEntityById() {
    assertEquals(HttpStatus.NOT_FOUND, GET("/tracker/trackedEntities/" + "woitxQbWYNq").status());
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
  void shouldGetTrackedEntitiesDisregardingSearchScope() {
    // Create a user with a distinct search scope, separate from capture scope
    User testUser = createAndAddUser("testingUser");
    testUser.setOrganisationUnits(Set.of(get(OrganisationUnit.class, "RojfDTBhoGC")));
    testUser.setTeiSearchOrganisationUnits(Set.of(get(OrganisationUnit.class, "h4w96yEMlzO")));
    testUser.setUserRoles(Set.of(get(UserRole.class, "nJ4Ml8ads4M")));

    this.switchContextToUser(testUser);

    Program program = get(Program.class, "BFcipDERJnf");

    HttpResponse response =
        GET(
            "/tracker/trackedEntities?program={programId}&orgUnitMode={orgUnitMode}",
            program.getUid(),
            CAPTURE);

    assertEquals(HttpStatus.OK, response.status());
  }

  @Test
  void getTrackedEntityCsvById() {
    TrackedEntity te = get(TrackedEntity.class, "QS6w44flWAf");
    List<TrackedEntityAttributeValue> trackedEntityTypeAttributeValues =
        te.getTrackedEntityAttributeValues().stream()
            .filter(teav -> !"toDelete000".equals(teav.getAttribute().getUid()))
            .toList();
    assertHasSize(
        3,
        trackedEntityTypeAttributeValues,
        "test expects the tracked entity to have 3 tracked entity type attribute values");

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
  void shouldGetSoftDeletedEnrollmentsWhenIncludeDeletedIsTrue() {
    JsonList<JsonTrackedEntity> json =
        GET(
                "/tracker/trackedEntities?trackedEntities={id}&fields=enrollments&includeDeleted=true",
                "woitxQbWYNq")
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    JsonList<JsonEnrollment> enrollments = json.get(0).getEnrollments();
    enrollments.forEach(
        en -> {
          assertEquals(EnrollmentStatus.CANCELLED.name(), en.getStatus());
          assertTrue(en.getDeleted());
        });
    enrollments.stream()
        .flatMap(en -> en.getEvents().stream())
        .forEach(ev -> assertTrue(ev.getDeleted()));
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
    String program = "shPjYNifvMK";
    TrackedEntity te = get(TrackedEntity.class, "mHWCacsGYYn");
    assertHasSize(2, te.getEnrollments(), "test expects a tracked entity with one enrollment");
    assertContains(
        program,
        te.getEnrollments().stream().map(e -> e.getProgram().getUid()).collect(joining(",")));
    Enrollment enrollment =
        te.getEnrollments().stream()
            .filter(e -> e.getProgram().getUid().equals(program))
            .findFirst()
            .get();
    assertHasSize(1, enrollment.getEvents(), "test expects an enrollment with one event");
    TrackerEvent event = enrollment.getEvents().iterator().next();
    assertIsEmpty(event.getRelationshipItems(), "test expects an event with no relationships");

    JsonList<JsonEnrollment> json =
        GET("/tracker/trackedEntities/{id}?program={id}&fields=enrollments", te.getUid(), program)
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
    TrackerEvent event = enrollment.getEvents().iterator().next();
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
    TrackerEvent event = enrollment.getEvents().iterator().next();
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

    HttpResponse response =
        GET(
            "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
            trackedEntity.getUid(),
            tetTea.getUid(),
            program.getUid());

    assertEquals(HttpStatus.OK, response.status());
    assertEquals("\"" + file1.getUid() + "\"", response.header("Etag"));
    assertEquals("no-cache, private", response.header("Cache-Control"));
    assertEquals(Long.toString(file1.getContentLength()), response.header("Content-Length"));
    assertEquals("filename=" + file1.getName(), response.header("Content-Disposition"));
    assertEquals("file content", response.content("text/plain"));
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
    this.switchContextToUser(user);
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
    this.switchContextToUser(user);

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
    this.switchContextToUser(user);
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
        "User is not authorized to read data from selected program's tracked entity type",
        GET(
                "/tracker/trackedEntities/{trackedEntityUid}/attributes/{attributeUid}/file?program={programUid}",
                trackedEntity.getUid(),
                tea.getUid(),
                program.getUid())
            .error(HttpStatus.FORBIDDEN)
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
    this.switchContextToUser(user);
    assertStartsWith(
        "TrackedEntityAttribute with id " + tea.getUid(),
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
    this.switchContextToUser(user);
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
    FileResource file = createFileResource('B', "file content".getBytes());
    manager.save(file, false);
    TrackedEntityAttribute tea =
        addProgramAttributeValue(trackedEntity, program, ValueType.FILE_RESOURCE, file.getUid());
    manager.delete(file);
    this.switchContextToUser(user);
    assertStartsWith(
        "FileResource with id " + file.getUid(),
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

  @ParameterizedTest
  @MethodSource("provideMultiTextFilterTestCases")
  void shouldReturnExpectedTrackedEntitiesForMultiTextFilter(
      String filter, List<String> expectedDataValues, List<String> expectedTrackedEntities) {
    switchContextToUser(get(User.class, "tTgjgobT1oS"));

    JsonList<JsonTrackedEntity> jsonTrackedEntities =
        GET(
                "/tracker/trackedEntities?filter={attr}:{filter}&program={program}&fields=trackedEntity,attributes",
                TEA_MULTI_TEXT,
                filter,
                "BFcipDERJnf")
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    assertContainsOnly(
        expectedTrackedEntities,
        jsonTrackedEntities.stream().map(JsonTrackedEntity::getTrackedEntity).toList());
    assertContainsOnly(
        expectedDataValues,
        jsonTrackedEntities.stream()
            .map(
                te ->
                    te.getAttributes().stream()
                        .filter(attr -> "multitxtAtr".equals(attr.getAttribute()))
                        .findFirst()
                        .map(JsonAttribute::getValue)
                        .orElse(null))
            .toList());
  }

  @Test
  void shouldReturnWhenFetchingTrackedEntitiesUsingNullOperators() {
    switchContextToUser(get(User.class, "tTgjgobT1oS"));

    JsonList<JsonTrackedEntity> jsonTrackedEntities =
        GET(
                "/tracker/trackedEntities?filter={attr}:null&program={program}&fields=trackedEntity,attributes",
                TEA_MULTI_TEXT,
                "BFcipDERJnf")
            .content(HttpStatus.OK)
            .getList("trackedEntities", JsonTrackedEntity.class);

    assertContainsAll(
        Stream.concat(Stream.of(TE_UID_EMPTY_STRING), TE_UID_NULL.stream()).toList(),
        jsonTrackedEntities,
        JsonTrackedEntity::getTrackedEntity);
  }

  @Test
  void shouldReturnErrorWhenFetchingTrackedEntitiesUsingUnsupportedOperators() {
    switchContextToUser(get(User.class, "tTgjgobT1oS"));

    assertEquals(
        String.format(
            "Invalid filter: Operator 'GT' is not supported for multi-text TrackedEntityAttribute : '%s'.",
            TEA_MULTI_TEXT),
        GET(
                "/tracker/trackedEntities?filter={attribute}:GT:bl&program={program}&fields=trackedEntity,attributes",
                TEA_MULTI_TEXT,
                "BFcipDERJnf")
            .error(BAD_REQUEST)
            .getMessage());
  }

  private TrackedEntity deleteTrackedEntity(UID uid) {
    TrackedEntity trackedEntity = get(TrackedEntity.class, uid.getValue());
    org.hisp.dhis.tracker.imports.domain.TrackedEntity deletedTrackedEntity =
        trackerObjects.getTrackedEntities().stream()
            .filter(te -> te.getTrackedEntity().equals(uid))
            .findFirst()
            .get();

    TrackerObjects deleteTrackerObjects =
        TrackerObjects.builder().trackedEntities(List.of(deletedTrackedEntity)).build();
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            deleteTrackerObjects));
    manager.clear();
    manager.flush();
    return trackedEntity;
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

  private JsonEvent assertDefaultEventResponse(JsonEnrollment enrollment, TrackerEvent event) {
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
    TrackedEntity te = trackedEntity(orgUnit, trackedEntityType);
    te.setTrackedEntityType(trackedEntityType);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(TrackedEntityType trackedEntityType) {
    TrackedEntity te = trackedEntity(orgUnit, trackedEntityType);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit, trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.READ);
    te.getSharing().setOwner(owner);
    return te;
  }

  private Enrollment enroll(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    Enrollment enrollment = createEnrollment(program, trackedEntity, orgUnit);
    manager.save(enrollment);
    trackedEntity.getEnrollments().add(enrollment);
    manager.update(trackedEntity);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(
        trackedEntity, program, orgUnit);

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

  private void assertTrackedEntityWithinRelationship(
      SingleEvent expected, JsonRelationshipItem json) {
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
    TrackedEntityAttributeValue attributeValue = attributeValue(tea, trackedEntity, value);
    trackedEntity.addAttributeValue(attributeValue);
    manager.save(trackedEntity, false);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(attributeValue);
    manager.flush();
    manager.clear();
    return tea;
  }

  private TrackedEntityAttribute addProgramAttributeValue(
      TrackedEntity trackedEntity, Program program, ValueType type, String value) {
    TrackedEntityAttribute tea = programAttribute(program, type);
    TrackedEntityAttributeValue attributeValue = attributeValue(tea, trackedEntity, value);
    trackedEntity.addAttributeValue(attributeValue);
    manager.save(trackedEntity, false);
    trackedEntityAttributeValueService.addTrackedEntityAttributeValue(attributeValue);
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

  /**
   * Provides test cases for multi-text tracked entity attribute filtering using different
   * operators. Each test case defines: - The filter expression to be tested - The expected matching
   * multi-text tracked entity attribute values - The expected matching TrackedEntity UIDs
   */
  private static Stream<Arguments> provideMultiTextFilterTestCases() {
    return Stream.of(
        Arguments.of("sw:bl", List.of("red,blue,Green"), List.of(TE_UID_RBG)),
        Arguments.of("ew:een", List.of("red,blue,Green"), List.of(TE_UID_RBG)),
        Arguments.of("like:ellow", List.of("red,white,yellow"), List.of(TE_UID_RWY)),
        Arguments.of("like:ello", List.of("red,white,yellow"), List.of(TE_UID_RWY)),
        Arguments.of("ew:bl", List.of(), List.of()), // no match
        Arguments.of("ilike:green", List.of("red,blue,Green"), List.of(TE_UID_RBG)),
        Arguments.of(
            "in:red",
            List.of("red,blue,Green", "red,white,yellow"),
            List.of(TE_UID_RBG, TE_UID_RWY)),
        Arguments.of(
            "like:red",
            List.of("red,blue,Green", "red,white,yellow"),
            List.of(TE_UID_RBG, TE_UID_RWY)),
        Arguments.of(
            "!null",
            List.of("red,blue,Green", "red,white,yellow"),
            List.of(TE_UID_RBG, TE_UID_RWY)));
  }
}
