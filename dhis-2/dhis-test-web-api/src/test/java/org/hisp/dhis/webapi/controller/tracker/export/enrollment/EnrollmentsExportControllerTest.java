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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasOnlyMembers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
import org.hisp.dhis.webapi.controller.tracker.JsonNote;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationship;
import org.hisp.dhis.webapi.controller.tracker.JsonRelationshipItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentsExportControllerTest extends DhisControllerConvenienceTest {

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private User owner;

  private Program program;

  private TrackedEntity te;

  private Enrollment enrollment;

  private TrackedEntityAttribute tea;

  private Relationship relationship;

  private ProgramStage programStage;

  private Event event;

  private DataElement dataElement;

  private static final String ATTRIBUTE_VALUE = "value";

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  private EventDataValue eventDataValue;

  @BeforeEach
  void setUp() {
    owner = makeUser("o");
    manager.save(owner, false);

    orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);

    User user = createUserWithId("tester", CodeGenerator.generateUid());
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    this.userService.updateUser(user);

    program = createProgram('A');
    manager.save(program);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);

    tea = createTrackedEntityAttribute('A');
    tea.getSharing().setOwner(owner);
    manager.save(tea, false);

    te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    manager.save(te);

    trackedEntityAttributeValue = new TrackedEntityAttributeValue();
    trackedEntityAttributeValue.setAttribute(tea);
    trackedEntityAttributeValue.setTrackedEntity(te);
    trackedEntityAttributeValue.setStoredBy("user");
    trackedEntityAttributeValue.setValue(ATTRIBUTE_VALUE);
    te.setTrackedEntityAttributeValues(Set.of(trackedEntityAttributeValue));
    manager.update(te);

    program.setProgramAttributes(List.of(createProgramTrackedEntityAttribute(program, tea)));

    programStage = createProgramStage('A', program);
    manager.save(programStage);

    enrollment = enrollment(te);
    event = event();
    enrollment.setEvents(Set.of(event));
    manager.update(enrollment);

    manager.save(relationship(enrollment, te));
  }

  @Test
  void getEnrollmentById() {
    JsonEnrollment enrollment =
        GET("/tracker/enrollments/{id}", this.enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    assertDefaultResponse(enrollment);
  }

  @Test
  void getEnrollmentByIdWithFields() {
    JsonEnrollment enrollment =
        GET("/tracker/enrollments/{id}?fields=orgUnit,status", this.enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    assertHasOnlyMembers(enrollment, "orgUnit", "status");
    assertEquals(this.enrollment.getOrganisationUnit().getUid(), enrollment.getOrgUnit());
    assertEquals(this.enrollment.getStatus().toString(), enrollment.getStatus());
  }

  @Test
  void getEnrollmentByIdWithNotes() {
    enrollment.setComments(List.of(note("oqXG28h988k", "my notes", owner.getUid())));

    JsonEnrollment enrollment =
        GET("/tracker/enrollments/{uid}?fields=notes", this.enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    JsonNote note = enrollment.getNotes().get(0);
    assertEquals("oqXG28h988k", note.getNote());
    assertEquals("my notes", note.getValue());
    assertEquals(owner.getUid(), note.getStoredBy());
  }

  @Test
  void getEnrollmentByIdWithAttributes() {
    JsonEnrollment enrollment =
        GET("/tracker/enrollments/{id}?fields=attributes", this.enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    assertHasOnlyMembers(enrollment, "attributes");
    JsonAttribute attribute = enrollment.getAttributes().get(0);
    assertEquals(tea.getUid(), attribute.getAttribute());
    TrackedEntityAttribute expected = trackedEntityAttributeValue.getAttribute();
    assertEquals(trackedEntityAttributeValue.getValue(), attribute.getValue());
    assertEquals(expected.getValueType().toString(), attribute.getValueType());
    assertHasMember(attribute, "createdAt");
    assertHasMember(attribute, "updatedAt");
    assertHasMember(attribute, "displayName");
    assertHasMember(attribute, "code");
    assertHasMember(attribute, "storedBy");
  }

  @Test
  void getEnrollmentByIdWithRelationshipsFields() {
    JsonList<JsonRelationship> relationships =
        GET("/tracker/enrollments/{id}?fields=relationships", enrollment.getUid())
            .content(HttpStatus.OK)
            .getList("relationships", JsonRelationship.class);

    JsonRelationship jsonRelationship = relationships.get(0);
    assertEquals(relationship.getUid(), jsonRelationship.getRelationship());

    JsonRelationshipItem.JsonEnrollment enrollment = jsonRelationship.getFrom().getEnrollment();
    assertEquals(relationship.getFrom().getEnrollment().getUid(), enrollment.getEnrollment());
    assertEquals(
        relationship.getFrom().getEnrollment().getTrackedEntity().getUid(),
        enrollment.getTrackedEntity());

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
  void getEnrollmentByIdWithEventsFields() {
    JsonList<JsonEvent> events =
        GET("/tracker/enrollments/{id}?fields=events", enrollment.getUid())
            .content(HttpStatus.OK)
            .getList("events", JsonEvent.class);

    JsonEvent event = events.get(0);
    assertEquals(this.event.getUid(), event.getEvent());
    assertEquals(enrollment.getUid(), event.getEnrollment());
    assertEquals(te.getUid(), event.getTrackedEntity());
    assertEquals(dataElement.getUid(), event.getDataValues().get(0).getDataElement());
    assertEquals(eventDataValue.getValue(), event.getDataValues().get(0).getValue());
    assertEquals(program.getUid(), event.getProgram());

    assertHasMember(event, "status");
    assertHasMember(event, "followup");
    assertEquals(program.getUid(), event.getProgram());
    assertEquals(orgUnit.getUid(), event.getOrgUnit());
    assertEquals(orgUnit.getName(), event.getOrgUnitName());
    assertFalse(event.getDeleted());
  }

  @Test
  void getEnrollmentByIdWithExcludedFields() {
    assertTrue(
        (GET(
                    "/tracker/enrollments/{id}?fields=!attributes,!relationships,!events",
                    enrollment.getUid())
                .content(HttpStatus.OK))
            .isEmpty());
  }

  @Test
  void getEnrollmentByIdNotFound() {
    assertEquals(
        "Enrollment with id Hq3Kc6HK4OZ could not be found.",
        GET("/tracker/enrollments/Hq3Kc6HK4OZ").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void getEnrollmentsFailsIfGivenEnrollmentAndEnrollmentsParameters() {
    assertStartsWith(
        "Only one parameter of 'enrollment' (deprecated",
        GET("/tracker/enrollments?enrollment=IsdLBTOBzMi&enrollments=IsdLBTOBzMi")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  private Event event() {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit());
    event.setAutoFields();

    eventDataValue = new EventDataValue();
    eventDataValue.setValue("value");
    dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.TEXT);
    manager.save(dataElement);
    eventDataValue.setDataElement(dataElement.getUid());
    Set<EventDataValue> eventDataValues = Set.of(eventDataValue);
    event.setEventDataValues(eventDataValues);
    manager.save(event);
    return event;
  }

  private Relationship relationship(Enrollment from, TrackedEntity to) {
    relationship = new Relationship();

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setEnrollment(from);
    from.getRelationshipItems().add(fromItem);
    relationship.setFrom(fromItem);
    fromItem.setRelationship(relationship);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntity(to);
    to.getRelationshipItems().add(toItem);
    relationship.setTo(toItem);
    toItem.setRelationship(relationship);

    RelationshipType type = createRelationshipType('A');
    type.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    type.getToConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(type, false);

    relationship.setRelationshipType(type);
    relationship.setKey(type.getUid());
    relationship.setInvertedKey(type.getUid());
    relationship.setAutoFields();

    manager.save(relationship, false);
    return relationship;
  }

  private void assertDefaultResponse(JsonEnrollment enrollment) {
    assertFalse(enrollment.isEmpty());
    assertEquals(this.enrollment.getUid(), enrollment.getEnrollment());
    assertEquals(te.getUid(), enrollment.getTrackedEntity());
    assertEquals(program.getUid(), enrollment.getProgram());
    assertEquals("COMPLETED", enrollment.getStatus());
    assertEquals(orgUnit.getUid(), enrollment.getOrgUnit());
    assertEquals(orgUnit.getName(), enrollment.getOrgUnitName());
    assertTrue(enrollment.getBoolean("followUp").bool());
    assertFalse(enrollment.getBoolean("deleted").bool());
    assertHasMember(enrollment, "enrolledAt");
    assertHasMember(enrollment, "occurredAt");
    assertHasMember(enrollment, "createdAt");
    assertHasMember(enrollment, "createdAtClient");
    assertHasMember(enrollment, "updatedAt");
    assertHasMember(enrollment, "notes");
    assertHasNoMember(enrollment, "relationships");
    assertHasNoMember(enrollment, "events");
    assertHasNoMember(enrollment, "attributes");
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, orgUnit);
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setIncidentDate(new Date());
    enrollment.setStatus(ProgramStatus.COMPLETED);
    enrollment.setFollowup(true);
    manager.save(enrollment, false);
    te.setEnrollments(Set.of(enrollment));
    manager.save(te, false);
    return enrollment;
  }

  private TrackedEntityComment note(String note, String value, String storedBy) {
    TrackedEntityComment comment = new TrackedEntityComment(value, storedBy);
    comment.setUid(note);
    manager.save(comment, false);
    return comment;
  }
}
