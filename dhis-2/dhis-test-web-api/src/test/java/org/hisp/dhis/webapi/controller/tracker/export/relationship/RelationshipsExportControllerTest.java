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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.http.HttpStatus.FORBIDDEN;
import static org.hisp.dhis.http.HttpStatus.NOT_FOUND;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.Assertions.*;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContains;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertEnrollmentWithinRelationshipItem;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertEventWithinRelationshipItem;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertFirstRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyUid;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertNoRelationships;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertProgramOwners;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertTrackedEntityWithinRelationshipItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.JsonProgramOwner;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.JsonUser;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipsExportControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private Event relationship1To;
  private Relationship relationship1;
  private TrackedEntity relationship1From;
  private Relationship relationship2;
  private TrackedEntity relationship2From;
  private Enrollment relationship2To;
  private Relationship deletedTEToEnrollmentRelationship;
  private Relationship deletedTEToEventRelationship;
  private TrackerObjects trackerObjects;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    trackerObjects = testSetup.importTrackerData();

    manager.flush();
    manager.clear();

    relationship1 = getRelationship(UID.of("oLT07jKRu9e"));
    relationship1From = getTrackedEntity(relationship1.getFrom().getTrackedEntity());
    assertNotNull(relationship1From, "test expects 'from' to be a tracked entity");
    relationship1To = getEvent(relationship1.getTo().getEvent());
    assertNotNull(relationship1To.getUid(), "test expects 'to' to be an event");

    relationship2 = getRelationship(UID.of("p53a6314631"));
    relationship2From = getTrackedEntity(relationship2.getFrom().getTrackedEntity());
    assertNotNull(relationship2From, "test expects 'from' to be a tracked entity");
    relationship2To = getEnrollment(relationship2.getTo().getEnrollment());
    assertNotNull(relationship2To, "test expects 'to' to be an enrollment");

    deletedTEToEnrollmentRelationship = getRelationship(UID.of("rSXvGDlJRBT"));

    deletedTEToEventRelationship = getRelationship(UID.of("rq1MGCJ8hlq"));

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder()
                .relationships(
                    List.of(deletedTEToEventRelationship, deletedTEToEnrollmentRelationship))
                .build()));
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @Test
  void shouldGetRelationshipsById() {
    JsonRelationship jsonRelationship =
        GET("/tracker/relationships/{uid}", relationship1.getUid())
            .content(HttpStatus.OK)
            .as(JsonRelationship.class);

    assertHasOnlyMembers(
        jsonRelationship, "relationship", "relationshipType", "createdAtClient", "from", "to");
    assertRelationship(relationship1, jsonRelationship);
    assertHasOnlyUid(
        relationship1From.getUid(), "trackedEntity", jsonRelationship.getObject("from"));
    assertHasOnlyUid(relationship1To.getUid(), "event", jsonRelationship.getObject("to"));
  }

  @Test
  void shouldGetRelationshipsByIdWithFieldsAll() {
    JsonRelationship jsonRelationship =
        GET("/tracker/relationships/{uid}?fields=*", relationship1.getUid())
            .content(HttpStatus.OK)
            .as(JsonRelationship.class);

    assertRelationship(relationship1, jsonRelationship);
    assertTrackedEntityWithinRelationshipItem(relationship1From, jsonRelationship.getFrom());
    assertEventWithinRelationshipItem(relationship1To, jsonRelationship.getTo());
  }

  @Test
  void shouldGetRelationshipsByIdWithFields() {
    JsonRelationship jsonRelationship =
        GET("/tracker/relationships/{uid}?fields=relationship,to[event]", relationship1.getUid())
            .content(HttpStatus.OK)
            .as(JsonRelationship.class);

    assertHasOnlyMembers(jsonRelationship, "relationship", "to");
    assertEquals(
        relationship1.getUid().getValue(), jsonRelationship.getRelationship(), "relationship UID");
    assertHasOnlyMembers(jsonRelationship.getObject("to"), "event");
    assertEquals(
        relationship1To.getUid().getValue(),
        jsonRelationship.getTo().getEvent().getEvent(),
        "event UID");
  }

  @Test
  void shouldReturnNotFoundWhenGettingANonExistingRelationshipById() {
    assertEquals(
        "Relationship with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/relationships/Hq3Kc6HK4OZ").error(NOT_FOUND).getMessage());
  }

  @Test
  void shouldReturnBadRequestWhenGettingRelationshipsWithoutMandatoryParams() {
    assertEquals(
        "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
        GET("/tracker/relationships").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void shouldReturnBadRequestWhenGettingRelationshipsWithMultipleParams() {
    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        GET("/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ&enrollment=Hq3Kc6HK4OZ&event=Hq3Kc6HK4OZ")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGetRelationshipsByEvent() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={uid}", relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertRelationship(relationship1, jsonRelationship);
    assertHasOnlyUid(relationship1From.getUid(), "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship1To.getUid(), "event", jsonRelationship.getTo());
  }

  @Test
  void shouldGetNoRelationshipsByEventWhenRelationshipTypeIsUnidirectionalAndEventIsOnTheToSide() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    relationshipType.setBidirectional(false);
    manager.update(relationshipType);

    Relationship relationship = getRelationship(UID.of("x8919212736"));

    org.hisp.dhis.program.Event to = manager.get(org.hisp.dhis.program.Event.class, "QRYjLTiJTrA");
    assertHasSize(
        1,
        to.getRelationshipItems(),
        "test expects relationship to have 'to' event with uid " + to.getUid());

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={uid}", to.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship.getUid());

    assertRelationship(relationship, jsonRelationship);
    assertHasOnlyUid(UID.of(to), "event", jsonRelationship.getTo());
  }

  @Test
  void shouldGetRelationshipsByEventWithAllFields() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={uid}&fields=*", relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertRelationship(relationship1, jsonRelationship);
    assertTrackedEntityWithinRelationshipItem(relationship1From, jsonRelationship.getFrom());
    assertEventWithinRelationshipItem(relationship1To, jsonRelationship.getTo());
  }

  @Test
  void shouldGetRelationshipsByEventWithFields() {
    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/relationships?event={uid}&fields=relationship,to[event]",
                relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertHasOnlyMembers(jsonRelationship, "relationship", "to");
    assertHasOnlyMembers(jsonRelationship.getTo(), "event");
    assertEquals(
        relationship1To.getUid().getValue(),
        jsonRelationship.getTo().getEvent().getEvent(),
        "event UID");
  }

  @Test
  void shouldGetRelationshipsByEventWithAssignedUser() {
    JsonList<JsonRelationship> relationships =
        GET("/tracker/relationships?event={uid}&fields=to[event[assignedUser]]", "QRYjLTiJTrA")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonUser user = relationships.get(0).getTo().getEvent().getAssignedUser();
    assertEquals("lPaILkLkgOM", user.getUid());
    assertEquals("testuser", user.getUsername());
  }

  @Test
  void shouldGetRelationshipsByEventWithDataValues() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?event={uid}&fields=to[event[dataValues[dataElement,value]]]",
                relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonList<JsonDataValue> dataValues = relationships.get(0).getTo().getEvent().getDataValues();
    dataValues.forEach(dv -> assertHasOnlyMembers(dv, "dataElement", "value"));
  }

  @Test
  void shouldGetRelationshipsByEventWithNotes() {
    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/relationships?event={uid}&fields=relationship,to[event[notes]]",
                relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    JsonList<JsonNote> notes = jsonRelationship.getTo().getEvent().getNotes();
    notes.forEach(
        note -> assertHasOnlyMembers(note, "note", "value", "storedAt", "storedBy", "createdBy"));
  }

  @Test
  void shouldReturnNotFoundWhenGettingRelationshipsByNonExistingEvent() {
    assertStartsWith(
        "Event with id Hq3Kc6HK4OZ",
        GET("/tracker/relationships?event=Hq3Kc6HK4OZ").error(NOT_FOUND).getMessage());
  }

  @Test
  void shouldGetRelationshipsByEnrollment() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment=" + relationship2To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = assertFirstRelationship(relationship2, jsonRelationships);
    assertHasOnlyMembers(
        jsonRelationship, "relationship", "relationshipType", "from", "to", "createdAtClient");
    assertHasOnlyUid(relationship2From.getUid(), "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship2To.getUid(), "enrollment", jsonRelationship.getTo());
  }

  @Test
  void
      shouldGetNoRelationshipsByEnrollmentWhenRelationshipTypeIsUnidirectionalAndEnrollmentIsOnTheToSide() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "xLmPUYJX8Ks");
    relationshipType.setBidirectional(false);
    manager.update(relationshipType);

    org.hisp.dhis.program.Enrollment to =
        manager.get(org.hisp.dhis.program.Enrollment.class, "nxP7UnKhomJ");
    assertHasSize(
        1,
        to.getRelationshipItems(),
        "test expects relationship to have 'to' enrollment with uid " + to.getUid());

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment=" + to.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = assertFirstRelationship(relationship2, jsonRelationships);
    assertHasOnlyMembers(
        jsonRelationship, "relationship", "relationshipType", "from", "to", "createdAtClient");
    assertHasOnlyUid(relationship2From.getUid(), "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship2To.getUid(), "enrollment", jsonRelationship.getTo());
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWithFieldsAll() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment={uid}&fields=*", relationship2To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = assertFirstRelationship(relationship2, jsonRelationships);
    assertTrackedEntityWithinRelationshipItem(relationship2From, jsonRelationship.getFrom());
    assertEnrollmentWithinRelationshipItem(relationship2To, jsonRelationship.getTo());
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWithEvents() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?enrollment={uid}&fields=to[enrollment[events[enrollment,event]]]",
                relationship2To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationshipItem.JsonEvent event =
        relationships.get(0).getTo().getEnrollment().getEvents().get(0);
    assertEquals(relationship2To.getUid().getValue(), event.getEnrollment());
    assertEquals(
        getEventsByEnrollment(UID.of(event.getEnrollment())).get(0).getEvent().getValue(),
        event.getEvent());
    assertHasOnlyMembers(event, "enrollment", "event");
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWithAttributes() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=to[enrollment[enrollment,trackedEntity,attributes[attribute,value]]]",
                "woitxQbWYNq")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    Map<String, JsonAttribute> jsonAttributes =
        relationships.get(0).getTo().getEnrollment().getAttributes().stream()
            .collect(Collectors.toMap(JsonAttribute::getAttribute, Function.identity()));

    List<Attribute> expectedEnrollmentAttributes =
        getEnrollment(UID.of(relationships.get(0).getTo().getEnrollment().getEnrollment()))
            .getAttributes();
    List<Attribute> expectedTETAttributes =
        getTrackedEntity(UID.of(relationships.get(0).getTo().getEnrollment().getTrackedEntity()))
            .getAttributes();

    Map<String, Attribute> expectedAttributes =
        Stream.concat(expectedEnrollmentAttributes.stream(), expectedTETAttributes.stream())
            .distinct()
            .collect(
                Collectors.toMap(att -> att.getAttribute().getIdentifier(), Function.identity()));

    assertContainsOnly(expectedAttributes.keySet(), jsonAttributes.keySet());

    for (Attribute expectedAttribute : expectedAttributes.values()) {
      String expectedValue = expectedAttribute.getValue();

      JsonAttribute matchingJsonAttribute =
          jsonAttributes.get(expectedAttribute.getAttribute().getIdentifier());

      assertEquals(
          expectedValue,
          matchingJsonAttribute.getValue(),
          "Value mismatch for attribute " + expectedAttribute);
    }
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWithAttributesWithTrackerDataViewDefined() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "xLmPUYJX8Ks");
    TrackerDataView trackerDataView = new TrackerDataView();
    String expectedAttribute = "dIVt4l5vIOa";
    trackerDataView.getAttributes().add(expectedAttribute);
    relationshipType.getToConstraint().setTrackerDataView(trackerDataView);
    manager.save(relationshipType, false);

    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=to[enrollment[attributes[attribute,value]]]",
                "woitxQbWYNq")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    List<String> jsonAttributes =
        relationships.get(0).getTo().getEnrollment().getAttributes().stream()
            .map(JsonAttribute::getAttribute)
            .toList();

    assertContainsOnly(List.of(expectedAttribute), jsonAttributes);
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWithNotes() {
    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/relationships?enrollment={uid}&fields=relationship,to[enrollment[notes]]",
                relationship2To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship2.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship2.getUid());

    JsonList<JsonNote> notes = jsonRelationship.getTo().getEnrollment().getNotes();
    notes.forEach(
        note -> assertHasOnlyMembers(note, "note", "value", "storedAt", "storedBy", "createdBy"));
  }

  @Test
  void shouldReturnNotFoundWhenGettingRelationshipsByNonExistingEnrollment() {
    assertStartsWith(
        "Enrollment with id Hq3Kc6HK4OZ",
        GET("/tracker/relationships?enrollment=Hq3Kc6HK4OZ").error(NOT_FOUND).getMessage());
  }

  @Test
  void shouldGetRelationshipsByTrackedEntity() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?trackedEntity={trackedEntity}", relationship1From.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertRelationship(relationship1, jsonRelationship);
    assertHasOnlyMembers(
        jsonRelationship, "relationship", "relationshipType", "createdAtClient", "from", "to");
    assertHasOnlyUid(relationship1From.getUid(), "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship1To.getUid(), "event", jsonRelationship.getTo());
  }

  @Test
  void
      shouldGetNoRelationshipsByTrackedEntityWhenRelationshipTypeIsUnidirectionalAndTrackedEntityIsOnTheToSide() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "m1575931405");
    relationshipType.setBidirectional(false);
    manager.update(relationshipType);

    Relationship relationship = getRelationship(UID.of("N8800829a58"));

    org.hisp.dhis.trackedentity.TrackedEntity to =
        manager.get(org.hisp.dhis.trackedentity.TrackedEntity.class, "QesgJkTyTCk");
    assertHasSize(
        1,
        to.getRelationshipItems(),
        "test expects relationship to have 'to' tracked entity with uid " + to.getUid());

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?trackedEntity={trackedEntity}", to.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship.getUid().getValue().equals(rel.getRelationship()),
            "expected to find relationship " + relationship.getUid());

    assertRelationship(relationship, jsonRelationship);
    assertHasOnlyUid(UID.of(to), "trackedEntity", jsonRelationship.getTo());
  }

  @Test
  void shouldNotGetRelationshipsByTrackedEntityWhenRelationshipIsDeleted() {
    assertNoRelationships(
        GET("/tracker/relationships?trackedEntity={te}", "guVNoAerxWo").content(HttpStatus.OK));
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWhenRelationshipIsDeleted() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?trackedEntity={te}&includeDeleted=true", "guVNoAerxWo")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);
    assertFirstRelationship(deletedTEToEnrollmentRelationship, jsonRelationships);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWhenTrackedEntityIsDeletedAndIncludeDeletedIsTrue() {
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder()
                .trackedEntities(List.of(getTrackedEntity(UID.of("guVNoAerxWo"))))
                .build()));

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?trackedEntity={te}&fields=*&includeDeleted=true", "guVNoAerxWo")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);
    assertFirstRelationship(deletedTEToEnrollmentRelationship, jsonRelationships);
    assertTrackedEntityWithinRelationshipItem(
        getTrackedEntity(UID.of("guVNoAerxWo")), jsonRelationships.get(0).getFrom());
  }

  @Test
  void
      shouldReturnNotFoundWhenGettingRelationshipsByTrackedEntityAndTrackedEntityIsDeletedAndIncludeDeletedIsFalse() {
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder()
                .trackedEntities(List.of(getTrackedEntity(UID.of("guVNoAerxWo"))))
                .build()));

    GET("/tracker/relationships?trackedEntity={te}&includeDeleted=false", "guVNoAerxWo")
        .error(NOT_FOUND);
  }

  @Test
  void shouldNotGetRelationshipsByEnrollmentWhenRelationshipIsDeleted() {
    assertNoRelationships(
        GET("/tracker/relationships?enrollment={en}", "ipBifypAQTo").content(HttpStatus.OK));
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWhenRelationshipIsDeleted() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment={en}&includeDeleted=true", "ipBifypAQTo")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);
    assertFirstRelationship(deletedTEToEnrollmentRelationship, jsonRelationships);
  }

  @Test
  void shouldGetRelationshipsByEnrollmentWhenEnrollmentIsDeletedAndIncludeDeletedIsTrue() {
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder()
                .enrollments(List.of(getEnrollment(UID.of("ipBifypAQTo"))))
                .build()));

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment={en}&includeDeleted=true&fields=*", "ipBifypAQTo")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);
    assertFirstRelationship(deletedTEToEnrollmentRelationship, jsonRelationships);
    assertEnrollmentWithinRelationshipItem(
        getEnrollment(UID.of("ipBifypAQTo")), jsonRelationships.get(0).getTo());
  }

  @Test
  void
      shouldGetNotFoundWhenGettingRelationshipsByEnrollmentAndEnrollmentIsDeletedAndIncludeDeletedIsFalse() {
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder()
                .enrollments(List.of(getEnrollment(UID.of("ipBifypAQTo"))))
                .build()));

    GET("/tracker/relationships?enrollment={en}&includeDeleted=false", "ipBifypAQTo")
        .error(NOT_FOUND);
  }

  @Test
  void shouldNotGetRelationshipsByEventWhenRelationshipIsDeleted() {
    assertNoRelationships(
        GET("/tracker/relationships?event={ev}", "LCSfHnurnNB").content(HttpStatus.OK));
  }

  @Test
  void shouldGetRelationshipsByEventWhenRelationshipIsDeletedAndIncludeDeletedIsTrue() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={ev}&includeDeleted=true", "LCSfHnurnNB")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);
    assertFirstRelationship(deletedTEToEventRelationship, jsonRelationships);
  }

  @Test
  void shouldGetRelationshipsByEventWhenEventIsDeletedAndIncludeDeletedIsTrue() {
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder().events(List.of(getEvent(UID.of("LCSfHnurnNB")))).build()));

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={ev}&includeDeleted=true&fields=*", "LCSfHnurnNB")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);
    assertFirstRelationship(deletedTEToEventRelationship, jsonRelationships);
    assertEventWithinRelationshipItem(
        getEvent(UID.of("LCSfHnurnNB")), jsonRelationships.get(0).getTo());
  }

  @Test
  void shouldGetNotFoundWhenGettingRelationshipsByEventAndEventIsDeletedAndIncludeDeletedIsFalse() {
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            TrackerObjects.builder().events(List.of(getEvent(UID.of("LCSfHnurnNB")))).build()));

    GET("/tracker/relationships?event={ev}&includeDeleted=false", "LCSfHnurnNB").error(NOT_FOUND);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithEnrollments() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=from[trackedEntity[enrollments[enrollment,trackedEntity]]",
                relationship1From.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonList<JsonRelationshipItem.JsonEnrollment> enrollments =
        relationships.get(0).getFrom().getTrackedEntity().getEnrollments();
    List<Enrollment> enrollmentsByTrackedEntity =
        getEnrollmentsByTrackedEntity(UID.of(enrollments.get(0).getTrackedEntity()));

    assertEquals(relationship1From.getUid().getValue(), enrollments.get(0).getTrackedEntity());
    assertContainsOnly(
        enrollmentsByTrackedEntity.stream().map(e -> e.getEnrollment().getValue()).toList(),
        enrollments.stream().map(JsonRelationshipItem.JsonEnrollment::getEnrollment).toList());
    assertHasOnlyMembers(enrollments.get(0), "trackedEntity", "enrollment");
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithAttributes() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=from[trackedEntity[attributes[attribute,value]]]",
                "mHWCacsGYYn")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    List<String> jsonAttributes =
        relationships.get(0).getFrom().getTrackedEntity().getAttributes().stream()
            .map(JsonAttribute::getAttribute)
            .toList();

    List<String> expectedAttributes =
        getTrackedEntity(UID.of("mHWCacsGYYn")).getAttributes().stream()
            .map(Attribute::getAttribute)
            .map(MetadataIdentifier::getIdentifier)
            .toList();

    assertContainsOnly(expectedAttributes, jsonAttributes);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithAttributesWithTrackerDataViewDefined() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    TrackerDataView trackerDataView = new TrackerDataView();
    String expectedAttribute = "integerAttr";
    trackerDataView.getAttributes().add(expectedAttribute);
    relationshipType.getFromConstraint().setTrackerDataView(trackerDataView);
    manager.save(relationshipType, false);

    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=from[trackedEntity[attributes[attribute,value]]]",
                "mHWCacsGYYn")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    List<String> jsonAttributes =
        relationships.get(0).getFrom().getTrackedEntity().getAttributes().stream()
            .map(JsonAttribute::getAttribute)
            .toList();

    assertContainsOnly(List.of(expectedAttribute), jsonAttributes);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithNoEnrollmentAttributes() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=relationshipType,from[trackedEntity[enrollment[attributes[attribute,value]]]]",
                "dUE514NMOlo")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    List<String> jsonAttributes =
        relationships
            .get(0)
            .getTo()
            .getTrackedEntity()
            .getEnrollments()
            .get(0)
            .getAttributes()
            .stream()
            .map(JsonAttribute::getAttribute)
            .toList();
    assertIsEmpty(jsonAttributes);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithDataElements() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=relationshipType,to[event[event,dataValues[dataElement,value]]]",
                "mHWCacsGYYn")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    List<String> jsonDataValues =
        relationships.get(0).getTo().getEvent().getDataValues().stream()
            .map(JsonDataValue::getDataElement)
            .toList();

    List<String> expectedDataElements =
        getEvent(UID.of(relationships.get(0).getTo().getEvent().getEvent()))
            .getDataValues()
            .stream()
            .map(DataValue::getDataElement)
            .map(MetadataIdentifier::getIdentifier)
            .toList();

    assertContainsOnly(expectedDataElements, jsonDataValues);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithDataElementsWithTrackerDataViewDefined() {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    TrackerDataView trackerDataView = new TrackerDataView();
    String expectedDataElement = "GieVkTxp4HH";
    trackerDataView.getDataElements().add(expectedDataElement);
    relationshipType.getToConstraint().setTrackerDataView(trackerDataView);
    manager.save(relationshipType, false);

    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=relationshipType,to[event[dataValues[dataElement,value]]]",
                "mHWCacsGYYn")
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    List<String> jsonDataValues =
        relationships.get(0).getTo().getEvent().getDataValues().stream()
            .map(JsonDataValue::getDataElement)
            .toList();

    assertContainsOnly(List.of(expectedDataElement), jsonDataValues);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityWithProgramOwners() {
    JsonList<JsonRelationship> relationships =
        GET(
                "/tracker/relationships?trackedEntity={trackedEntity}&fields=from[trackedEntity[programOwners]",
                relationship1From.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonList<JsonProgramOwner> jsonProgramOwners =
        relationships.get(0).getFrom().getTrackedEntity().getProgramOwners();

    assertProgramOwners(
        getEnrollmentsByTrackedEntity(relationship1From.getUid()), jsonProgramOwners);
  }

  @Test
  void shouldGetRelationshipsByTrackedEntityRelationshipsNoAccessToRelationshipType() {
    User user = userService.getUser("Z7870757a75");
    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    relationshipType.getSharing().setUserAccesses(Set.of());
    relationshipType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(relationshipType, false);

    switchContextToUser(user);

    assertNoRelationships(
        GET("/tracker/relationships?trackedEntity={trackedEntity}", relationship1From.getUid())
            .content(HttpStatus.OK));
  }

  @Test
  void shouldRetrieveNoRelationshipsWhenUserHasNoAccessToRelationshipItemTo() {
    User user = userService.getUser("Z7870757a75");
    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    UserAccess userAccess = new UserAccess();
    userAccess.setUser(user);
    userAccess.setAccess(AccessStringHelper.DATA_READ);
    relationshipType.getSharing().setUserAccesses(Set.of(userAccess));
    relationshipType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);

    Program program = manager.get(Program.class, "BFcipDERJnf");
    program.getSharing().setUserAccesses(Set.of());
    program.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);

    manager.save(relationshipType, false);
    manager.save(program, false);

    switchContextToUser(user);

    assertNoRelationships(
        GET("/tracker/relationships?trackedEntity={trackedEntity}", relationship1From.getUid())
            .content(HttpStatus.OK));
  }

  @Test
  void shouldReturnForbiddenWhenUserHasNoAccessToRelationshipItemFrom() {
    User user = userService.getUser("Z7870757a75");
    TrackedEntityType trackedEntityType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    trackedEntityType.getSharing().setUserAccesses(Set.of());
    trackedEntityType.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);

    manager.save(trackedEntityType, false);

    switchContextToUser(user);

    GET("/tracker/relationships?trackedEntity={trackedEntity}", relationship1From.getUid())
        .error(FORBIDDEN);
  }

  @Test
  void shouldReturnNotFoundWhenGettingRelationshipsByNonExistingTrackedEntity() {
    assertStartsWith(
        "TrackedEntity with id Hq3Kc6HK4OZ",
        GET("/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ").error(NOT_FOUND).getMessage());
  }

  private TrackedEntity getTrackedEntity(UID trackedEntity) {
    return trackerObjects.getTrackedEntities().stream()
        .filter(te -> te.getTrackedEntity().equals(trackedEntity))
        .findFirst()
        .get();
  }

  private Enrollment getEnrollment(UID enrollment) {
    return trackerObjects.getEnrollments().stream()
        .filter(en -> en.getEnrollment().equals(enrollment))
        .findFirst()
        .get();
  }

  private List<Enrollment> getEnrollmentsByTrackedEntity(UID trackedEntity) {
    return trackerObjects.getEnrollments().stream()
        .filter(en -> en.getTrackedEntity().equals(trackedEntity))
        .toList();
  }

  private Event getEvent(UID event) {
    return trackerObjects.getEvents().stream()
        .filter(ev -> ev.getEvent().equals(event))
        .findFirst()
        .get();
  }

  private List<Event> getEventsByEnrollment(UID enrollment) {
    return trackerObjects.getEvents().stream()
        .filter(ev -> ev.getEnrollment().equals(enrollment))
        .toList();
  }

  private Relationship getRelationship(UID relationship) {
    return trackerObjects.getRelationships().stream()
        .filter(r -> r.getRelationship().equals(relationship))
        .findFirst()
        .get();
  }
}
