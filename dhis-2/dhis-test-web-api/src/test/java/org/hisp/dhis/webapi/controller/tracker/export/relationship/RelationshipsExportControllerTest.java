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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertContains;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertEnrollmentWithinRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertEventWithinRelationshipItem;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertFirstRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyUid;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertRelationship;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertTrackedEntityWithinRelationshipItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipsExportControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private Event relationship1To;
  private Relationship relationship1;
  private TrackedEntity relationship1From;
  private Relationship relationship2;
  private TrackedEntity relationship2From;
  private Enrollment relationship2To;

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

    relationship1 = get(Relationship.class, "oLT07jKRu9e");
    relationship1From = relationship1.getFrom().getTrackedEntity();
    assertNotNull(relationship1From, "test expects 'from' to be a tracked entity");
    relationship1To = relationship1.getTo().getEvent();
    assertNotNull(relationship1To, "test expects 'to' to be an event");

    relationship2 = get(Relationship.class, "p53a6314631");
    relationship2From = relationship2.getFrom().getTrackedEntity();
    assertNotNull(relationship2From, "test expects 'from' to be a tracked entity");
    relationship2To = relationship2.getTo().getEnrollment();
    assertNotNull(relationship2To, "test expects 'to' to be an enrollment");
    // for some reason we get a LazyInit exception in an assertion when running all tests if we
    // don't eagerly fetch like we do here
    relationship2From.getUid();
    relationship2To.getUid();
    relationship2.getRelationshipType().getUid();
  }

  @BeforeEach
  void setUpUser() {
    switchContextToUser(importUser);
  }

  @Test
  void getRelationshipsById() {
    JsonRelationship jsonRelationship =
        GET("/tracker/relationships/{uid}", relationship1.getUid())
            .content(HttpStatus.OK)
            .as(JsonRelationship.class);

    assertHasOnlyMembers(
        jsonRelationship, "relationship", "relationshipType", "createdAtClient", "from", "to");
    assertRelationship(relationship1, jsonRelationship);
    assertHasOnlyUid(relationship1From, "trackedEntity", jsonRelationship.getObject("from"));
    assertHasOnlyUid(relationship1To, "event", jsonRelationship.getObject("to"));
  }

  @Test
  void getRelationshipsByIdWithFieldsAll() {
    JsonRelationship jsonRelationship =
        GET("/tracker/relationships/{uid}?fields=*", relationship1.getUid())
            .content(HttpStatus.OK)
            .as(JsonRelationship.class);

    assertRelationship(relationship1, jsonRelationship);
    assertTrackedEntityWithinRelationshipItem(relationship1From, jsonRelationship.getFrom());
    assertEventWithinRelationshipItem(relationship1To, jsonRelationship.getTo());
  }

  @Test
  void getRelationshipsByIdWithFields() {
    JsonRelationship jsonRelationship =
        GET("/tracker/relationships/{uid}?fields=relationship,to[event]", relationship1.getUid())
            .content(HttpStatus.OK)
            .as(JsonRelationship.class);

    assertHasOnlyMembers(jsonRelationship, "relationship", "to");
    assertEquals(relationship1.getUid(), jsonRelationship.getRelationship(), "relationship UID");
    assertHasOnlyMembers(jsonRelationship.getObject("to"), "event");
    assertEquals(
        relationship1To.getUid(), jsonRelationship.getTo().getEvent().getEvent(), "event UID");
  }

  @Test
  void getRelationshipsByIdNotFound() {
    assertEquals(
        "Relationship with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/relationships/Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void getRelationshipsMissingParam() {
    assertEquals(
        "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
        GET("/tracker/relationships").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void getRelationshipsBadRequestWithMultipleParams() {
    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        GET("/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ&enrollment=Hq3Kc6HK4OZ&event=Hq3Kc6HK4OZ")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void getRelationshipsByEvent() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={uid}", relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertRelationship(relationship1, jsonRelationship);
    assertHasOnlyUid(relationship1From, "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship1To, "event", jsonRelationship.getTo());
  }

  @Test
  void getRelationshipsByEventWithAllFields() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?event={uid}&fields=*", relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertRelationship(relationship1, jsonRelationship);
    assertTrackedEntityWithinRelationshipItem(relationship1From, jsonRelationship.getFrom());
    assertEventWithinRelationshipItem(relationship1To, jsonRelationship.getTo());
  }

  @Test
  void getRelationshipsByEventWithFields() {
    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/relationships?event={uid}&fields=relationship,to[event]",
                relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertHasOnlyMembers(jsonRelationship, "relationship", "to");
    assertHasOnlyMembers(jsonRelationship.getTo(), "event");
    assertEquals(
        relationship1To.getUid(), jsonRelationship.getTo().getEvent().getEvent(), "event UID");
  }

  // TODO(DHIS2-18883) migrate these tests
  //  @Test
  //  void getRelationshipsByEventWithAssignedUser() {
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?event={uid}&fields=from[event[assignedUser]]",
  // from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonUser user = relationships.get(0).getFrom().getEvent().getAssignedUser();
  //    assertEquals(owner.getUid(), user.getUid());
  //    assertEquals(owner.getUsername(), user.getUsername());
  //  }
  //  @Test
  //  void getRelationshipsByEventWithDataValues() {
  //    TrackedEntity to = trackedEntity();
  //    Event from = event(enrollment(to));
  //    from.setEventDataValues(Set.of(new EventDataValue(dataElement.getUid(), "12")));
  //    Relationship relationship = relationship(from, to);
  //    RelationshipType type = relationship.getRelationshipType();
  //
  //    RelationshipConstraint toConstraint = new RelationshipConstraint();
  //
  //    TrackerDataView trackerDataView = new TrackerDataView();
  //    trackerDataView.setDataElements(new LinkedHashSet<>(Set.of(dataElement.getUid())));
  //
  //    toConstraint.setTrackerDataView(trackerDataView);
  //
  //    type.setFromConstraint(toConstraint);
  //
  //    manager.update(type);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET(
  //
  // "/tracker/relationships?event={uid}&fields=from[event[dataValues[dataElement,value]]]",
  //                from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonDataValue dataValue = relationships.get(0).getFrom().getEvent().getDataValues().get(0);
  //    assertEquals(dataElement.getUid(), dataValue.getDataElement());
  //    assertEquals("12", dataValue.getValue());
  //  }

  @Test
  void getRelationshipsByEventWithNotes() {
    JsonList<JsonRelationship> jsonRelationships =
        GET(
                "/tracker/relationships?event={uid}&fields=relationship,to[event[notes]]",
                relationship1To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    JsonList<JsonNote> notes = jsonRelationship.getTo().getEvent().getNotes();
    notes.forEach(
        note -> {
          assertHasOnlyMembers(note, "note", "value", "storedAt", "storedBy", "createdBy");
        });
  }

  @Test
  void getRelationshipsByEventNotFound() {
    assertNull(manager.get(Event.class, "Hq3Kc6HK4OZ"), "test expects event not to exist");

    assertStartsWith(
        "Event with id Hq3Kc6HK4OZ",
        GET("/tracker/relationships?event=Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void getRelationshipsByEnrollment() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment=" + relationship2To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = assertFirstRelationship(relationship2, jsonRelationships);
    assertHasOnlyMembers(jsonRelationship, "relationship", "relationshipType", "from", "to");
    assertHasOnlyUid(relationship2From, "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship2To, "enrollment", jsonRelationship.getTo());
  }

  @Test
  void getRelationshipsByEnrollmentWithFieldsAll() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?enrollment={uid}&fields=*", relationship2To.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = assertFirstRelationship(relationship2, jsonRelationships);
    assertTrackedEntityWithinRelationshipItem(relationship2From, jsonRelationship.getFrom());
    assertEnrollmentWithinRelationship(relationship2To, jsonRelationship.getTo());
  }

  // TODO(DHIS2-18883) migrate these tests
  //  @Test
  //  void getRelationshipsByEnrollmentWithEvents() {
  //    Enrollment from = enrollment(trackedEntity());
  //    Event to = event(from);
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET(
  //
  // "/tracker/relationships?enrollment={uid}&fields=from[enrollment[events[enrollment,event]]]",
  //                from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonRelationshipItem.JsonEvent event =
  //        relationships.get(0).getFrom().getEnrollment().getEvents().get(0);
  //    assertEquals(from.getUid(), event.getEnrollment());
  //    assertEquals(to.getUid(), event.getEvent());
  //  }
  //
  //  @Test
  //  void getRelationshipsByEnrollmentWithAttributes() {
  //    TrackedEntity to = trackedEntity();
  //    to.setTrackedEntityAttributeValues(Set.of(attributeValue(tea, to, "12")));
  //    program.setProgramAttributes(List.of(createProgramTrackedEntityAttribute(program, tea)));
  //
  //    Enrollment from = enrollment(to);
  //    Relationship relationship = relationship(from, to);
  //
  //    RelationshipType type = relationship.getRelationshipType();
  //
  //    RelationshipConstraint constraint = new RelationshipConstraint();
  //
  //    TrackerDataView trackerDataView = new TrackerDataView();
  //    trackerDataView.setAttributes(new LinkedHashSet<>(Set.of(tea.getUid())));
  //
  //    constraint.setTrackerDataView(trackerDataView);
  //
  //    type.setFromConstraint(constraint);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET(
  //
  // "/tracker/relationships?enrollment={uid}&fields=from[enrollment[attributes[attribute,value]]]",
  //                from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonAttribute attribute =
  // relationships.get(0).getFrom().getEnrollment().getAttributes().get(0);
  //    assertEquals(tea.getUid(), attribute.getAttribute());
  //    assertEquals("12", attribute.getValue());
  //  }
  //
  //  @Test
  //  void getRelationshipsByEnrollmentWithNotes() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    from.setNotes(List.of(note("oqXG28h988k", "my notes", owner.getUid())));
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?enrollment={uid}&fields=from[enrollment[notes]]",
  // from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonNote note = relationships.get(0).getFrom().getEnrollment().getNotes().get(0);
  //    assertEquals("oqXG28h988k", note.getNote());
  //    assertEquals("my notes", note.getValue());
  //    assertEquals(owner.getUid(), note.getStoredBy());
  //  }

  @Test
  void getRelationshipsByEnrollmentNotFound() {
    assertNull(manager.get(Enrollment.class, "Hq3Kc6HK4OZ"), "test expects event not to exist");

    assertStartsWith(
        "Enrollment with id Hq3Kc6HK4OZ",
        GET("/tracker/relationships?enrollment=Hq3Kc6HK4OZ")
            .error(HttpStatus.NOT_FOUND)
            .getMessage());
  }

  @Test
  void getRelationshipsByTrackedEntity() {
    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/relationships?trackedEntity={trackedEntity}", relationship1From.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship =
        assertContains(
            jsonRelationships,
            rel -> relationship1.getUid().equals(rel.getRelationship()),
            "expected to find relationship " + relationship1.getUid());

    assertRelationship(relationship1, jsonRelationship);
    assertHasOnlyMembers(
        jsonRelationship, "relationship", "relationshipType", "createdAtClient", "from", "to");
    assertHasOnlyUid(relationship1From, "trackedEntity", jsonRelationship.getFrom());
    assertHasOnlyUid(relationship1To, "event", jsonRelationship.getTo());
  }

  //
  //  @Test
  //  void shouldNotGetRelationshipsByTrackedEntityWhenRelationshipIsDeleted() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    Relationship r = relationship(from, to);
  //
  //    r.setDeleted(true);
  //    manager.update(r);
  //    switchContextToUser(user);
  //
  //    assertNoRelationships(
  //        GET("/tracker/relationships?trackedEntity={te}", to.getUid()).content(HttpStatus.OK));
  //  }
  //
  //  @Test
  //  void shouldNotGetRelationshipsByEnrollmentWhenRelationshipIsDeleted() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    Relationship r = relationship(from, to);
  //
  //    r.setDeleted(true);
  //    manager.update(r);
  //    switchContextToUser(user);
  //
  //    assertNoRelationships(
  //        GET("/tracker/relationships?enrollment={en}", from.getUid()).content(HttpStatus.OK));
  //  }
  //
  //  @Test
  //  void shouldNotGetRelationshipsByEventWhenRelationshipIsDeleted() {
  //    TrackedEntity to = trackedEntity();
  //    Event from = event(enrollment(to));
  //    Relationship r = relationship(from, to);
  //
  //    r.setDeleted(true);
  //    manager.update(r);
  //    switchContextToUser(user);
  //
  //    assertNoRelationships(
  //        GET("/tracker/relationships?event={ev}", from.getUid()).content(HttpStatus.OK));
  //  }
  //
  //  @Test
  //  void shouldGetRelationshipsByTrackedEntityWhenRelationshipIsDeleted() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    Relationship r = relationship(from, to);
  //
  //    r.setDeleted(true);
  //    manager.update(r);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?trackedEntity={te}&includeDeleted=true", to.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    assertFirstRelationship(r, relationships);
  //  }
  //
  //  @Test
  //  void shouldGetRelationshipsByEventWhenRelationshipIsDeleted() {
  //    TrackedEntity to = trackedEntity();
  //    Event from = event(enrollment(to));
  //    Relationship r = relationship(from, to);
  //
  //    r.setDeleted(true);
  //    manager.update(r);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?event={ev}&includeDeleted=true", from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    assertFirstRelationship(r, relationships);
  //  }
  //
  //  @Test
  //  void shouldGetRelationshipsByEnrollmentWhenRelationshipIsDeleted() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    Relationship r = relationship(from, to);
  //
  //    r.setDeleted(true);
  //    manager.update(r);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?enrollment={en}&includeDeleted=true", from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    assertFirstRelationship(r, relationships);
  //  }
  //
  //  @Test
  //  void getRelationshipsByDeprecatedTei() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    Relationship r = relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?tei=" + to.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonObject relationship = assertFirstRelationship(r, relationships);
  //    assertHasOnlyMembers(relationship, "relationship", "relationshipType", "from", "to");
  //    assertHasOnlyUid(from.getUid(), "enrollment", relationship.getObject("from"));
  //    assertHasOnlyUid(to.getUid(), "trackedEntity", relationship.getObject("to"));
  //  }
  //
  //  @Test
  //  void getRelationshipsByTrackedEntityWithEnrollments() {
  //    TrackedEntity to = trackedEntity();
  //    Enrollment from = enrollment(to);
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET(
  //
  // "/tracker/relationships?trackedEntity={trackedEntity}&fields=to[trackedEntity[enrollments[enrollment,trackedEntity]]",
  //                to.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonRelationshipItem.JsonEnrollment enrollment =
  //        relationships.get(0).getTo().getTrackedEntity().getEnrollments().get(0);
  //    assertEquals(from.getUid(), enrollment.getEnrollment());
  //    assertEquals(to.getUid(), enrollment.getTrackedEntity());
  //  }
  //
  //  @Test
  //  void getRelationshipsByTrackedEntityAndEnrollmentWithAttributesIsEmpty() {
  //    // Tracked entity attribute values are owned by the tracked entity and only mapped onto the
  //    // enrollment on export. Program tracked entity attributes are only returned by the
  // underlying
  //    // TE service if a program is
  //    // provided which is not possible on the relationship endpoint.
  //    TrackedEntity to = trackedEntity(orgUnit);
  //    to.setTrackedEntityAttributeValues(
  //        Set.of(attributeValue(tea, to, "12"), attributeValue(tea2, to, "24")));
  //    program.setProgramAttributes(List.of(createProgramTrackedEntityAttribute(program, tea2)));
  //    Enrollment from = enrollment(to);
  //    Relationship relationship = relationship(from, to);
  //
  //    RelationshipType type = relationship.getRelationshipType();
  //
  //    RelationshipConstraint fromConstraint = new RelationshipConstraint();
  //
  //    TrackerDataView trackerDataView = new TrackerDataView();
  //    trackerDataView.setAttributes(new LinkedHashSet<>(Set.of(tea2.getUid())));
  //
  //    fromConstraint.setTrackerDataView(trackerDataView);
  //
  //    RelationshipConstraint toConstraint = new RelationshipConstraint();
  //
  //    TrackerDataView dataView = new TrackerDataView();
  //    dataView.setAttributes(new LinkedHashSet<>(Set.of(tea.getUid(), tea2.getUid())));
  //
  //    toConstraint.setTrackerDataView(dataView);
  //
  //    type.setFromConstraint(fromConstraint);
  //    type.setToConstraint(toConstraint);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET(
  //
  // "/tracker/relationships?trackedEntity={trackedEntity}&fields=from[enrollment[attributes[attribute,value]]],to[trackedEntity[attributes[attribute,value]]]",
  //                to.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonList<JsonAttribute> enrollmentAttr =
  //        relationships.get(0).getFrom().getEnrollment().getAttributes();
  //    assertIsEmpty(
  //        enrollmentAttr.toList(JsonAttribute::getAttribute),
  //        "program attributes should not be returned as no program can be provided");
  //    JsonList<JsonAttribute> teAttributes =
  //        relationships.get(0).getTo().getTrackedEntity().getAttributes();
  //    assertContainsAll(List.of(tea.getUid()), teAttributes, JsonAttribute::getAttribute);
  //    assertContainsAll(List.of("12"), teAttributes, JsonAttribute::getValue);
  //  }
  //
  //  @Test
  //  void getRelationshipsByTrackedEntityWithProgramOwners() {
  //    TrackedEntity to = trackedEntity(orgUnit);
  //    Enrollment from = enrollment(to);
  //    to.setProgramOwners(Set.of(new TrackedEntityProgramOwner(to, from.getProgram(), orgUnit)));
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET(
  //
  // "/tracker/relationships?trackedEntity={trackedEntity}&fields=to[trackedEntity[programOwners]",
  //                to.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonProgramOwner jsonProgramOwner =
  //        relationships.get(0).getTo().getTrackedEntity().getProgramOwners().get(0);
  //    assertEquals(orgUnit.getUid(), jsonProgramOwner.getOrgUnit());
  //    assertEquals(to.getUid(), jsonProgramOwner.getTrackedEntity());
  //    assertEquals(from.getProgram().getUid(), jsonProgramOwner.getProgram());
  //  }
  //
  //  @Test
  //  void getRelationshipsByTrackedEntityRelationshipTeToTe() {
  //    TrackedEntity from = trackedEntity();
  //    TrackedEntity to = trackedEntity();
  //    Relationship r = relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?trackedEntity={trackedEntity}", from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonObject relationship = assertFirstRelationship(r, relationships);
  //    assertHasOnlyMembers(relationship, "relationship", "relationshipType", "from", "to");
  //    assertHasOnlyUid(from.getUid(), "trackedEntity", relationship.getObject("from"));
  //    assertHasOnlyUid(to.getUid(), "trackedEntity", relationship.getObject("to"));
  //  }
  //
  //  @Test
  //  void shouldRetrieveRelationshipWhenUserHasAccessToRelationship() {
  //    TrackedEntity from = trackedEntity();
  //    TrackedEntity to = trackedEntity();
  //    Relationship r = relationship(from, to);
  //    switchContextToUser(user);
  //
  //    JsonList<JsonRelationship> relationships =
  //        GET("/tracker/relationships?trackedEntity={trackedEntity}", from.getUid())
  //            .content(HttpStatus.OK)
  //            .getList("relationships", JsonRelationship.class);
  //
  //    JsonObject relationship = assertFirstRelationship(r, relationships);
  //    assertHasOnlyMembers(relationship, "relationship", "relationshipType", "from", "to");
  //    assertHasOnlyUid(from.getUid(), "trackedEntity", relationship.getObject("from"));
  //    assertHasOnlyUid(to.getUid(), "trackedEntity", relationship.getObject("to"));
  //  }
  //
  //  @Test
  //  void getRelationshipsByTrackedEntityRelationshipsNoAccessToRelationshipType() {
  //    TrackedEntity from = trackedEntity();
  //    TrackedEntity to = trackedEntity();
  //    relationship(relationshipTypeNotAccessible(), from, to);
  //    switchContextToUser(user);
  //
  //    assertNoRelationships(
  //        GET("/tracker/relationships?trackedEntity={trackedEntity}", from.getUid())
  //            .content(HttpStatus.OK));
  //  }
  //
  //  @Test
  //  void shouldRetrieveNoRelationshipsWhenUserHasNoAccessToRelationshipItemTo() {
  //    TrackedEntity from = trackedEntity();
  //    TrackedEntity to = trackedEntityNotInSearchScope();
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    assertNoRelationships(
  //        GET("/tracker/relationships?trackedEntity={trackedEntity}", from.getUid())
  //            .content(HttpStatus.OK));
  //  }
  //
  //  @Test
  //  void shouldReturnForbiddenWhenUserHasNoAccessToRelationshipItemFrom() {
  //    TrackedEntity from = trackedEntityNotInSearchScope();
  //    TrackedEntity to = trackedEntity();
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    assertEquals(
  //        HttpStatus.FORBIDDEN,
  //        GET("/tracker/relationships?trackedEntity={trackedEntity}", from.getUid()).status());
  //  }
  //
  //  @Test
  //  void
  //
  // shouldReturnForbiddenWhenGetRelationshipsByTrackedEntityWithNotAccessibleTrackedEntityType() {
  //    TrackedEntityType type = trackedEntityTypeNotAccessible();
  //    TrackedEntity from = trackedEntity(type);
  //    TrackedEntity to = trackedEntity(type);
  //    relationship(from, to);
  //    switchContextToUser(user);
  //
  //    assertEquals(
  //        HttpStatus.FORBIDDEN,
  //        GET("/tracker/relationships?trackedEntity={trackedEntity}", from.getUid()).status());
  //  }
  //
  //  @Test
  //  void getRelationshipsByTrackedEntityNotFound() {
  //    assertStartsWith(
  //        "TrackedEntity with id Hq3Kc6HK4OZ",
  //        GET("/tracker/relationships?trackedEntity=Hq3Kc6HK4OZ")
  //            .error(HttpStatus.NOT_FOUND)
  //            .getMessage());
  //  }

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
}
