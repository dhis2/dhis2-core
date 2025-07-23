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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.test.utils.Assertions.assertHasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.test.webapi.Assertions.assertNoDiff;
import static org.hisp.dhis.webapi.controller.tracker.Assertions.*;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContains;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonDiff.Mode;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentsExportControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String DELETE_TRACKED_ENTITY_UID = "mHWCacsGYYn";

  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private TrackerObjects trackerObjects;

  private User importUser;

  private Program program;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    trackerObjects = testSetup.importTrackerData();

    manager.flush();
    manager.clear();

    deleteTrackedEntity(UID.of(DELETE_TRACKED_ENTITY_UID));

    program = get(Program.class, "BFcipDERJnf");
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @Test
  void getEnrollmentByPathIsIdenticalToQueryParam() {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");

    JsonEnrollment pathEnrollment =
        GET("/tracker/enrollments/{id}?fields=*", enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);
    JsonList<JsonEnrollment> queryEnrollment =
        GET(
                "/tracker/enrollments?fields=*&enrollments={id}&program={id}",
                enrollment.getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    assertHasSize(1, queryEnrollment.stream().toList());
    assertNoDiff(pathEnrollment, queryEnrollment.get(0), Mode.LENIENT);
  }

  @Test
  void getEnrollmentById() {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");

    JsonEnrollment jsonEnrollment =
        GET("/tracker/enrollments/{id}", enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    assertDefaultResponse(enrollment, jsonEnrollment);
  }

  @ParameterizedTest
  @MethodSource("getEnrollment")
  void getEnrollmentByIdWithFields(BiFunction<Enrollment, String, JsonEnrollment> getEnrollment) {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");

    JsonEnrollment jsonEnrollment = getEnrollment.apply(enrollment, "orgUnit,status");

    assertHasOnlyMembers(jsonEnrollment, "orgUnit", "status");
    assertEquals(enrollment.getOrganisationUnit().getUid(), jsonEnrollment.getOrgUnit());
    assertEquals(enrollment.getStatus().toString(), jsonEnrollment.getStatus());
  }

  @ParameterizedTest
  @MethodSource("getEnrollment")
  void shouldGetEnrollmentWithNotes(BiFunction<Enrollment, String, JsonEnrollment> getEnrollment) {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");
    assertNotEmpty(enrollment.getNotes(), "test expects an enrollment with notes");

    JsonEnrollment jsonEnrollment = getEnrollment.apply(enrollment, "notes");

    JsonNote note = jsonEnrollment.getNotes().get(0);
    assertEquals("f9423652692", note.getNote());
    assertEquals("enrollment comment value", note.getValue());
  }

  @ParameterizedTest
  @MethodSource("getEnrollment")
  void shouldGetEnrollmentsWithAttributes(
      BiFunction<Enrollment, String, JsonEnrollment> getEnrollment) {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");
    assertNotEmpty(
        enrollment.getTrackedEntity().getTrackedEntityAttributeValues(),
        "test expects an enrollment with attribute values");
    TrackedEntityAttribute ptea = get(TrackedEntityAttribute.class, "dIVt4l5vIOa");

    JsonEnrollment jsonEnrollment = getEnrollment.apply(enrollment, "attributes");
    assertHasOnlyMembers(jsonEnrollment, "attributes");
    JsonAttribute attribute = jsonEnrollment.getAttributes().get(0);
    assertEquals(ptea.getUid(), attribute.getAttribute());
    assertEquals("Frank PTEA", attribute.getValue());
    assertEquals(ValueType.TEXT.name(), attribute.getValueType());
    assertHasMember(attribute, "createdAt");
    assertHasMember(attribute, "updatedAt");
    assertHasMember(attribute, "displayName");
    assertHasMember(attribute, "code");
  }

  @Test
  void shouldGetEnrollmentWithoutRelationshipsWhenRelationshipIsDeletedAndIncludeDeletedIsFalse() {
    Relationship relationship = get(Relationship.class, "p53a6314631");
    manager.delete(relationship);

    assertNotNull(
        relationship.getTo().getEnrollment(),
        "test expects relationship to have a 'to' enrollment");
    Enrollment enrollment = relationship.getTo().getEnrollment();

    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/enrollments?enrollments={id}&program={id}&fields=*&includeDeleted=false",
                enrollment.getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    assertIsEmpty(jsonRelationships.stream().toList());
  }

  @Test
  void shouldGetEnrollmentWithRelationshipsWhenRelationshipIsDeletedAndIncludeDeletedIsTrue() {
    Relationship relationship = get(Relationship.class, "p53a6314631");
    manager.delete(relationship);

    assertNotNull(
        relationship.getTo().getEnrollment(),
        "test expects relationship to have a 'to' enrollment");
    Enrollment enrollment = relationship.getTo().getEnrollment();

    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/enrollments?enrollments={id}&program={id}&fields=*&includeDeleted=true",
                enrollment.getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            re -> relationship.getUid().equals(re.getRelationship()),
            relationship.getUid());

    assertAll(
        "relationship JSON",
        () ->
            assertEquals(
                relationship.getFrom().getTrackedEntity().getUid(),
                jsonRelationship.getFrom().getTrackedEntity().getTrackedEntity()),
        () -> assertHasNoMember(jsonRelationship.getFrom().getTrackedEntity(), "relationships"),
        () -> assertHasMember(jsonRelationship.getFrom().getTrackedEntity(), "enrollments"),
        () ->
            assertHasNoMember(
                jsonRelationship.getFrom().getTrackedEntity().getEnrollments().get(0),
                "relationships"),
        () ->
            assertHasMember(
                jsonRelationship.getFrom().getTrackedEntity().getEnrollments().get(0), "events"),
        () ->
            assertHasNoMember(
                jsonRelationship
                    .getFrom()
                    .getTrackedEntity()
                    .getEnrollments()
                    .get(0)
                    .getEvents()
                    .get(0),
                "relationships"),
        () ->
            assertEquals(
                relationship.getTo().getEnrollment().getUid(),
                jsonRelationship.getTo().getEnrollment().getEnrollment()),
        () -> assertHasNoMember(jsonRelationship.getTo().getEnrollment(), "relationships"),
        () -> assertHasMember(jsonRelationship.getTo().getEnrollment(), "events"),
        () ->
            assertHasNoMember(
                jsonRelationship.getTo().getEnrollment().getEvents().get(0), "relationships"),
        () -> assertHasMember(jsonRelationship, "relationshipName"),
        () -> assertHasMember(jsonRelationship, "relationshipType"),
        () -> assertHasMember(jsonRelationship, "createdAt"),
        () -> assertHasMember(jsonRelationship, "updatedAt"),
        () -> assertHasMember(jsonRelationship, "bidirectional"));
  }

  @Test
  void
      shouldGetEnrollmentWithNoRelationshipsWhenEnrollmentIsOnTheToSideOfAUnidirectionalRelationship() {
    Relationship relationship = get(Relationship.class, "p53a6314631");

    assertNotNull(
        relationship.getTo().getEnrollment(),
        "test expects relationship to have a 'to' enrollment");
    RelationshipType relationshipType = relationship.getRelationshipType();
    relationshipType.setBidirectional(false);
    manager.save(relationshipType);

    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/enrollments?enrollments={id}&program={id}&fields=*&includeDeleted=true",
                relationship.getTo().getEnrollment().getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class)
            .get(0)
            .getList("relationships", JsonRelationship.class);

    assertIsEmpty(jsonRelationships.stream().toList());
  }

  @ParameterizedTest
  @MethodSource("getEnrollment")
  void shouldGetEnrollmentWithRelationshipsFields(
      BiFunction<Enrollment, String, JsonEnrollment> getEnrollment) {
    Relationship relationship = get(Relationship.class, "p53a6314631");
    assertNotNull(
        relationship.getTo().getEnrollment(),
        "test expects relationship to have a 'to' enrollment");
    Enrollment enrollment = relationship.getTo().getEnrollment();

    JsonList<JsonRelationship> jsonRelationships =
        getEnrollment
            .apply(enrollment, "relationships")
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            re -> relationship.getUid().equals(re.getRelationship()),
            relationship.getUid());

    assertAll(
        "relationship JSON",
        () ->
            assertEquals(
                relationship.getFrom().getTrackedEntity().getUid(),
                jsonRelationship.getFrom().getTrackedEntity().getTrackedEntity()),
        () -> assertHasNoMember(jsonRelationship.getFrom().getTrackedEntity(), "relationships"),
        () -> assertHasMember(jsonRelationship.getFrom().getTrackedEntity(), "enrollments"),
        () ->
            assertHasNoMember(
                jsonRelationship.getFrom().getTrackedEntity().getEnrollments().get(0),
                "relationships"),
        () ->
            assertHasMember(
                jsonRelationship.getFrom().getTrackedEntity().getEnrollments().get(0), "events"),
        () ->
            assertHasNoMember(
                jsonRelationship
                    .getFrom()
                    .getTrackedEntity()
                    .getEnrollments()
                    .get(0)
                    .getEvents()
                    .get(0),
                "relationships"),
        () ->
            assertEquals(
                relationship.getTo().getEnrollment().getUid(),
                jsonRelationship.getTo().getEnrollment().getEnrollment()),
        () -> assertHasNoMember(jsonRelationship.getTo().getEnrollment(), "relationships"),
        () -> assertHasMember(jsonRelationship.getTo().getEnrollment(), "events"),
        () ->
            assertHasNoMember(
                jsonRelationship.getTo().getEnrollment().getEvents().get(0), "relationships"),
        () -> assertHasMember(jsonRelationship, "relationshipName"),
        () -> assertHasMember(jsonRelationship, "relationshipType"),
        () -> assertHasMember(jsonRelationship, "createdAt"),
        () -> assertHasMember(jsonRelationship, "updatedAt"),
        () -> assertHasMember(jsonRelationship, "bidirectional"));
  }

  @ParameterizedTest
  @MethodSource("getEnrollment")
  void shouldGetEnrollmentWithEventsFields(
      BiFunction<Enrollment, String, JsonEnrollment> getEnrollment) {
    TrackerEvent event = get(TrackerEvent.class, "pTzf9KYMk72");
    assertNotNull(event.getEnrollment(), "test expects an event with an enrollment");
    assertNotEmpty(event.getEventDataValues(), "test expects an event with data values");
    EventDataValue eventDataValue = event.getEventDataValues().iterator().next();

    JsonList<JsonEvent> jsonEvents =
        getEnrollment.apply(event.getEnrollment(), "events").getList("events", JsonEvent.class);

    JsonEvent jsonEvent = jsonEvents.get(0);
    assertAll(
        "event JSON",
        () -> assertEquals(event.getUid(), jsonEvent.getEvent()),
        () -> assertEquals(event.getEnrollment().getUid(), jsonEvent.getEnrollment()),
        () ->
            assertEquals(
                event.getEnrollment().getTrackedEntity().getUid(), jsonEvent.getTrackedEntity()),
        () -> assertEquals(event.getProgramStage().getProgram().getUid(), jsonEvent.getProgram()),
        () -> assertEquals(event.getOrganisationUnit().getUid(), jsonEvent.getOrgUnit()),
        () -> {
          JsonDataValue jsonDataValue =
              assertContains(
                  jsonEvent.getDataValues(),
                  dv -> eventDataValue.getDataElement().equals(dv.getDataElement()),
                  eventDataValue.getDataElement());
          assertEquals(
              eventDataValue.getValue(),
              jsonDataValue.getValue(),
              "data value for data element " + eventDataValue.getDataElement());
        },
        () -> assertHasMember(jsonEvent, "status"),
        () -> assertHasMember(jsonEvent, "followUp"),
        () -> assertEquals(event.isDeleted(), jsonEvent.getDeleted()));
  }

  @Test
  void shouldGetSoftDeletedEnrollmentWithEventsWhenIncludeDeletedIsTrue() {
    TrackerEvent event = get(TrackerEvent.class, "pTzf9KYMk72");
    assertNotNull(event.getEnrollment(), "test expects an event with an enrollment");
    manager.delete(get(Enrollment.class, event.getEnrollment().getUid()));

    JsonList<JsonEvent> jsonEvents =
        GET(
                "/tracker/enrollments?enrollments={id}&program={id}&fields=events[event]&includeDeleted=true",
                event.getEnrollment().getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class)
            .get(0)
            .getList("events", JsonEvent.class);

    JsonEvent jsonEvent = jsonEvents.get(0);
    assertEquals(event.getUid(), jsonEvent.getEvent());
  }

  @Test
  void shouldGetEnrollmentsByTrackedEntityWhenTrackedEntityIsDeletedAndIncludeDeletedIsTrue() {
    Program program = get(Program.class, "shPjYNifvMK");
    JsonList<JsonEnrollment> enrollments =
        GET(
                "/tracker/enrollments?trackedEntity={te}&program={id}&includeDeleted=true&fields=deleted,trackedEntity",
                DELETE_TRACKED_ENTITY_UID,
                program.getUid())
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class);

    enrollments.forEach(
        en -> {
          assertTrue(en.getDeleted());
          assertEquals(DELETE_TRACKED_ENTITY_UID, en.getTrackedEntity());
        });
  }

  @Test
  void
      shouldGetNotFoundWhenGettingEnrollmentsByTrackedEntityAndTrackedEntityIsDeletedAndIncludeDeletedIsFalse() {
    GET("/tracker/enrollments?trackedEntity={te}&includeDeleted=false", DELETE_TRACKED_ENTITY_UID)
        .error(BAD_REQUEST);
  }

  @Test
  void getEnrollmentByIdWithExcludedFields() {
    TrackerEvent event = get(TrackerEvent.class, "pTzf9KYMk72");
    assertNotNull(event.getEnrollment(), "test expects an event with an enrollment");
    assertNotNull(
        event.getRelationshipItems(), "test expects an event with at least one relationship");

    assertTrue(
        (GET(
                    "/tracker/enrollments/{id}?fields=!attributes,!relationships,!events",
                    event.getEnrollment().getUid())
                .content(HttpStatus.OK))
            .isEmpty());
  }

  @Test
  void getEnrollmentByIdNotFound() {
    assertEquals(
        "Enrollment with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/enrollments/Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  private Stream<Arguments> getEnrollment() {
    return Stream.of(
        Arguments.of(getEnrollmentFromSingleEnrollmentEndpoint()),
        Arguments.of(getEnrollmentFromEnrollmentsEndpoint()));
  }

  private BiFunction<Enrollment, String, JsonEnrollment> getEnrollmentFromEnrollmentsEndpoint() {
    return (Enrollment enrollment, String fields) ->
        GET(
                "/tracker/enrollments?enrollments={id}&program={id}&fields={fields}",
                enrollment.getUid(),
                program.getUid(),
                fields)
            .content(HttpStatus.OK)
            .getList("enrollments", JsonEnrollment.class)
            .get(0);
  }

  private BiFunction<Enrollment, String, JsonEnrollment>
      getEnrollmentFromSingleEnrollmentEndpoint() {
    return (Enrollment enrollment, String fields) ->
        GET("/tracker/enrollments/{id}?fields={fields}", enrollment.getUid(), fields)
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);
  }

  private void assertDefaultResponse(Enrollment expected, JsonEnrollment actual) {
    assertFalse(actual.isEmpty());
    assertEquals(expected.getUid(), actual.getEnrollment());
    assertEquals(expected.getTrackedEntity().getUid(), actual.getTrackedEntity());
    assertEquals(expected.getProgram().getUid(), actual.getProgram());
    assertEquals(expected.getStatus().name(), actual.getStatus());
    assertEquals(expected.getOrganisationUnit().getUid(), actual.getOrgUnit());
    assertEquals(expected.getFollowup(), actual.getBoolean("followUp").bool());
    assertEquals(expected.isDeleted(), actual.getBoolean("deleted").bool());
    assertHasMember(actual, "enrolledAt");
    assertHasMember(actual, "occurredAt");
    assertHasMember(actual, "createdAt");
    assertHasMember(actual, "createdAtClient");
    assertHasMember(actual, "updatedAt");
    assertHasMember(actual, "notes");
    assertHasNoMember(actual, "relationships");
    assertHasNoMember(actual, "events");
    assertHasNoMember(actual, "attributes");
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
