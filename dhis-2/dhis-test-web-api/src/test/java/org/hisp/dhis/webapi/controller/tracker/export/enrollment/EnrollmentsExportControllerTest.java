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

import static org.hisp.dhis.test.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonDataValue;
import org.hisp.dhis.webapi.controller.tracker.JsonEnrollment;
import org.hisp.dhis.webapi.controller.tracker.JsonEvent;
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
class EnrollmentsExportControllerTest extends PostgresControllerIntegrationTestBase {

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

  @Test
  void getEnrollmentById() {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");

    JsonEnrollment jsonEnrollment =
        GET("/tracker/enrollments/{id}", enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    assertDefaultResponse(enrollment, jsonEnrollment);
  }

  @Test
  void getEnrollmentByIdWithFields() {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");

    JsonEnrollment jsonEnrollment =
        GET("/tracker/enrollments/{id}?fields=orgUnit,status", enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    assertHasOnlyMembers(jsonEnrollment, "orgUnit", "status");
    assertEquals(enrollment.getOrganisationUnit().getUid(), jsonEnrollment.getOrgUnit());
    assertEquals(enrollment.getStatus().toString(), jsonEnrollment.getStatus());
  }

  @Test
  void getEnrollmentByIdWithNotes() {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");
    assertNotEmpty(enrollment.getNotes(), "test expects an enrollment with notes");

    JsonEnrollment jsonEnrollment =
        GET("/tracker/enrollments/{uid}?fields=notes", enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

    JsonNote note = jsonEnrollment.getNotes().get(0);
    assertEquals("f9423652692", note.getNote());
    assertEquals("enrollment comment value", note.getValue());
  }

  @Test
  void getEnrollmentByIdWithAttributes() {
    Enrollment enrollment = get(Enrollment.class, "TvctPPhpD8z");
    assertNotEmpty(
        enrollment.getTrackedEntity().getTrackedEntityAttributeValues(),
        "test expects an enrollment with attribute values");
    TrackedEntityAttribute ptea = get(TrackedEntityAttribute.class, "dIVt4l5vIOa");

    JsonEnrollment jsonEnrollment =
        GET("/tracker/enrollments/{id}?fields=attributes", enrollment.getUid())
            .content(HttpStatus.OK)
            .as(JsonEnrollment.class);

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
  void getEnrollmentByIdWithRelationshipsFields() {
    Relationship relationship = get(Relationship.class, "p53a6314631");
    assertNotNull(
        relationship.getTo().getEnrollment(),
        "test expects relationship to have a 'to' enrollment");
    Enrollment enrollment = relationship.getTo().getEnrollment();

    JsonList<JsonRelationship> jsonRelationships =
        GET("/tracker/enrollments/{id}?fields=relationships", enrollment.getUid())
            .content(HttpStatus.OK)
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
        () ->
            assertEquals(
                relationship.getTo().getEnrollment().getUid(),
                jsonRelationship.getTo().getEnrollment().getEnrollment()),
        () -> assertHasMember(jsonRelationship, "relationshipName"),
        () -> assertHasMember(jsonRelationship, "relationshipType"),
        () -> assertHasMember(jsonRelationship, "createdAt"),
        () -> assertHasMember(jsonRelationship, "updatedAt"),
        () -> assertHasMember(jsonRelationship, "bidirectional"));
  }

  @Test
  void getEnrollmentByIdWithEventsFields() {
    Event event = get(Event.class, "pTzf9KYMk72");
    assertNotNull(event.getEnrollment(), "test expects an event with an enrollment");
    assertNotEmpty(event.getEventDataValues(), "test expects an event with data values");
    EventDataValue eventDataValue = event.getEventDataValues().iterator().next();

    JsonList<JsonEvent> jsonEvents =
        GET("/tracker/enrollments/{id}?fields=events", event.getEnrollment().getUid())
            .content(HttpStatus.OK)
            .getList("events", JsonEvent.class);

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
        () -> assertHasMember(jsonEvent, "followup"),
        () -> assertEquals(event.isDeleted(), jsonEvent.getDeleted()));
  }

  @Test
  void getEnrollmentByIdWithExcludedFields() {
    Event event = get(Event.class, "pTzf9KYMk72");
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

  @Test
  void getEnrollmentsFailsIfGivenEnrollmentAndEnrollmentsParameters() {
    assertStartsWith(
        "Only one parameter of 'enrollment' (deprecated",
        GET("/tracker/enrollments?enrollment=IsdLBTOBzMi&enrollments=IsdLBTOBzMi")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
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
