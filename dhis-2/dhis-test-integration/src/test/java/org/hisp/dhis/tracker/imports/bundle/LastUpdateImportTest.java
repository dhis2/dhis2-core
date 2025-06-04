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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerIdScheme;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LastUpdateImportTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity;
  private org.hisp.dhis.tracker.imports.domain.TrackedEntity anotherTrackedEntity;
  private org.hisp.dhis.tracker.imports.domain.Enrollment enrollment;
  private org.hisp.dhis.tracker.imports.domain.Event event;

  private OrganisationUnit organisationUnit;

  @Autowired protected UserService _userService;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;

    setUpMetadata("tracker/simple_metadata.json");

    injectAdminUser();

    TrackerObjects trackerObjects = fromJson("tracker/single_tei.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));

    trackedEntity = trackerObjects.getTrackedEntities().get(0);

    trackerObjects = fromJson("tracker/another_single_te.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));

    anotherTrackedEntity = trackerObjects.getTrackedEntities().get(0);

    trackerObjects = fromJson("tracker/single_enrollment.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));

    enrollment = trackerObjects.getEnrollments().get(0);

    trackerObjects = fromJson("tracker/single_event.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));

    event = trackerObjects.getEvents().get(0);

    organisationUnit =
        manager.get(OrganisationUnit.class, trackedEntity.getOrgUnit().getIdentifier());
  }

  @Test
  void shouldUpdateTeiWhenTeiIsUpdated() throws IOException {

    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    clearSession();

    TrackerObjects trackerObjects = fromJson("tracker/single_tei.json");
    TrackerImportParams trackerImportParams = new TrackerImportParams();
    trackerImportParams.setImportStrategy(TrackerImportStrategy.UPDATE);

    assertNoErrors(trackerImportService.importTracker(trackerImportParams, trackerObjects));

    Date lastUpdateAfter = getTrackedEntity().getLastUpdated();

    assertTrue(
        lastUpdateAfter.getTime() > entityBeforeUpdate.getLastUpdated().getTime(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
            trackedEntity.getUid()));
  }

  @Test
  void shouldUpdateOnlyFromTrackedEntityWhenUnidirectionalRelationshipIsCreated()
      throws IOException {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "m1575931405");
    relationshipType.setBidirectional(false);
    manager.update(relationshipType);
    TrackedEntity fromEntityBeforeUpdate = getTrackedEntity();
    TrackedEntity toEntityBeforeUpdate = getTrackedEntity(anotherTrackedEntity.getUid());

    TrackerObjects trackerObjects = fromJson("tracker/relationshipTEtoTE.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));
    clearSession();

    TrackedEntity fromEntityAfterUpdate = getTrackedEntity();
    TrackedEntity toEntityAfterUpdate = getTrackedEntity(anotherTrackedEntity.getUid());

    assertTrackedEntityUpdated(fromEntityBeforeUpdate, fromEntityAfterUpdate, getCurrentUser());
    assertTrackedEntityNotUpdated(toEntityBeforeUpdate, toEntityAfterUpdate);
  }

  @Test
  void shouldUpdateFromAndToTrackedEntitiesWhenBidirectionalRelationshipIsCreated()
      throws IOException {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "m1575931405");
    relationshipType.setBidirectional(true);
    manager.update(relationshipType);

    TrackedEntity fromEntityBeforeUpdate = getTrackedEntity();
    TrackedEntity toEntityBeforeUpdate = getTrackedEntity(anotherTrackedEntity.getUid());

    TrackerObjects trackerObjects = fromJson("tracker/relationshipTEtoTE.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));
    clearSession();

    TrackedEntity fromEntityAfterUpdate = getTrackedEntity();
    TrackedEntity toEntityAfterUpdate = getTrackedEntity(anotherTrackedEntity.getUid());

    assertTrackedEntityUpdated(fromEntityBeforeUpdate, fromEntityAfterUpdate, getCurrentUser());
    assertTrackedEntityUpdated(toEntityBeforeUpdate, toEntityAfterUpdate, getCurrentUser());
  }

  @Test
  void shouldUpdateOnlyFromTrackedEntityWhenUnidirectionalRelationshipIsDeleted()
      throws IOException {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "m1575931405");
    relationshipType.setBidirectional(false);
    manager.update(relationshipType);

    TrackerObjects trackerObjects = fromJson("tracker/relationshipTEtoTE.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));
    clearSession();

    TrackedEntity fromEntityBeforeUpdate = getTrackedEntity();
    TrackedEntity toEntityBeforeUpdate = getTrackedEntity(anotherTrackedEntity.getUid());
    clearSession();

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            trackerObjects));

    TrackedEntity fromEntityAfterUpdate = getTrackedEntity();
    TrackedEntity toEntityAfterUpdate = getTrackedEntity(anotherTrackedEntity.getUid());

    assertTrackedEntityUpdated(fromEntityBeforeUpdate, fromEntityAfterUpdate, getCurrentUser());
    assertTrackedEntityNotUpdated(toEntityBeforeUpdate, toEntityAfterUpdate);
  }

  @Test
  void shouldUpdateFromAndToTrackedEntitiesWhenBidirectionalRelationshipIsDeleted()
      throws IOException {
    RelationshipType relationshipType = manager.get(RelationshipType.class, "m1575931405");
    relationshipType.setBidirectional(true);
    manager.update(relationshipType);

    TrackerObjects trackerObjects = fromJson("tracker/relationshipTEtoTE.json");
    assertNoErrors(trackerImportService.importTracker(new TrackerImportParams(), trackerObjects));
    clearSession();

    TrackedEntity fromEntityBeforeUpdate = getTrackedEntity();
    TrackedEntity toEntityBeforeUpdate = getTrackedEntity(anotherTrackedEntity.getUid());
    clearSession();

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build(),
            trackerObjects));

    TrackedEntity fromEntityAfterUpdate = getTrackedEntity();
    TrackedEntity toEntityAfterUpdate = getTrackedEntity(anotherTrackedEntity.getUid());

    assertTrackedEntityUpdated(fromEntityBeforeUpdate, fromEntityAfterUpdate, getCurrentUser());
    assertTrackedEntityUpdated(toEntityBeforeUpdate, toEntityAfterUpdate, getCurrentUser());
  }

  @Test
  void shouldUpdateTeiWhenEventIsUpdated() throws IOException {
    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    User user = user();

    clearSession();

    TrackerObjects trackerObjects = fromJson("tracker/event_with_data_values.json");
    TrackerImportParams trackerImportParams = new TrackerImportParams();
    assertNoErrors(trackerImportService.importTracker(trackerImportParams, trackerObjects));

    trackerObjects = fromJson("tracker/event_with_updated_data_values.json");
    trackerImportParams.setImportStrategy(TrackerImportStrategy.UPDATE);
    trackerImportParams.setUserId(user.getUid());

    assertNoErrors(trackerImportService.importTracker(trackerImportParams, trackerObjects));

    clearSession();

    TrackedEntity entityAfterUpdate = getTrackedEntity();

    assertTrue(
        entityAfterUpdate.getLastUpdated().getTime()
            > entityBeforeUpdate.getLastUpdated().getTime(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
            trackedEntity.getUid()));
    assertEquals(
        entityAfterUpdate.getLastUpdatedByUserInfo().getUid(),
        UserInfoSnapshot.from(user).getUid(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdatedByUserinfo has not been saved during the import",
            trackedEntity.getUid()));
  }

  @Test
  void shouldUpdateTeiWhenEnrollmentIsUpdated() {

    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    User user = user();

    clearSession();

    TrackerImportParams trackerImportParams = new TrackerImportParams();
    trackerImportParams.setUserId(user.getUid());
    enrollment.setStatus(EnrollmentStatus.COMPLETED);
    trackerImportParams.setImportStrategy(TrackerImportStrategy.UPDATE);

    assertNoErrors(
        trackerImportService.importTracker(
            trackerImportParams,
            TrackerObjects.builder().enrollments(List.of(enrollment)).build()));

    clearSession();

    TrackedEntity entityAfterUpdate = getTrackedEntity();

    assertTrue(
        entityAfterUpdate.getLastUpdated().getTime()
            > entityBeforeUpdate.getLastUpdated().getTime(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
            trackedEntity.getUid()));
    assertEquals(
        entityAfterUpdate.getLastUpdatedByUserInfo().getUid(),
        UserInfoSnapshot.from(user).getUid(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdatedByUserinfo has not been saved during the import",
            trackedEntity.getUid()));
  }

  @Test
  void shouldUpdateAndDeleteTrackedEntityWhenTeIsDeleted() {

    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    clearSession();

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReport =
        trackerImportService.importTracker(
            params, TrackerObjects.builder().trackedEntities(List.of(trackedEntity)).build());
    assertNoErrors(importReport);
    assertEquals(1, importReport.getStats().getDeleted());

    TrackedEntity entityAfterDeletion = getTrackedEntity();

    assertAll(
        () -> assertTrue(entityAfterDeletion.isDeleted(), "Tracked Entity %s has not been deleted"),
        () ->
            assertTrue(
                entityAfterDeletion.getLastUpdated().getTime()
                    > entityBeforeUpdate.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
                    trackedEntity.getUid())));
  }

  @Test
  void shouldUpdateAndDeleteTrackedEntityCascadeWhenTeWithEnrollmentIsDeleted() {
    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    Enrollment enrollmentBeforeDelete = getEnrollment();

    enrollTrackerEntity();

    clearSession();

    // delete cascade
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReport =
        trackerImportService.importTracker(
            params, TrackerObjects.builder().trackedEntities(List.of(trackedEntity)).build());

    assertNoErrors(importReport);
    assertEquals(1, importReport.getStats().getDeleted());

    clearSession();

    TrackedEntity entityAfterDelete = getTrackedEntity();
    Enrollment enrollmentAfterDelete = getEnrollment();

    assertAll(
        () -> assertTrue(entityAfterDelete.isDeleted()),
        () -> assertTrue(enrollmentAfterDelete.isDeleted()),
        () ->
            assertTrue(
                entityAfterDelete.getLastUpdated().getTime()
                    > entityBeforeUpdate.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
                    trackedEntity.getUid())),
        () -> assertTrue(enrollmentAfterDelete.isDeleted()),
        () ->
            assertTrue(
                enrollmentAfterDelete.getLastUpdated().getTime()
                    > enrollmentBeforeDelete.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for enrollment %s. The lastUpdated date has not been updated after the import",
                    enrollment.getUid())));
  }

  @Test
  void shouldUpdateTrackedEntityAndDeleteEnrollmentWhenEnrollmentIsDeleted() {
    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    Enrollment enrollmentBeforeDeletion = getEnrollment();

    Event eventBeforeDeletion = getEvent();

    User user = createAndAddUser("userDelete", organisationUnit, "F_ENROLLMENT_CASCADE_DELETE");

    clearSession();

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    params.setUserId(user.getUid());

    ImportReport importReport =
        trackerImportService.importTracker(
            params, TrackerObjects.builder().enrollments(List.of(enrollment)).build());
    assertNoErrors(importReport);
    assertEquals(1, importReport.getStats().getDeleted());

    clearSession();

    TrackedEntity entityAfterDeletion = getTrackedEntity();
    Enrollment enrollmentAfterDeletion = getEnrollment();
    Event eventAfterDeletion = getEvent();

    assertAll(
        () ->
            assertTrue(
                entityAfterDeletion.getLastUpdated().getTime()
                    > entityBeforeUpdate.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
                    trackedEntity.getUid())),
        () -> assertTrue(enrollmentAfterDeletion.isDeleted()),
        () ->
            assertEquals(
                entityAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for tracked entity %s. The lastUpdatedByUserinfo has not been saved during the import",
                    trackedEntity.getUid())),
        () ->
            assertTrue(
                enrollmentAfterDeletion.getLastUpdated().getTime()
                    > enrollmentBeforeDeletion.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for enrollment %s. The lastUpdated date has not been updated after the import",
                    enrollment.getUid())),
        () ->
            assertEquals(
                enrollmentAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for enrollment %s. The lastUpdatedByUserinfo has not been saved during the import",
                    enrollment.getUid())),
        () -> assertTrue(eventAfterDeletion.isDeleted()),
        () ->
            assertTrue(
                eventAfterDeletion.getLastUpdated().getTime()
                    > eventBeforeDeletion.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for event %s. The lastUpdated date has not been updated after the import",
                    event.getUid())),
        () ->
            assertEquals(
                eventAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for event %s. The lastUpdatedByUserinfo has not been saved during the import",
                    event.getUid())));
  }

  @Test
  void shouldUpdateTrackedEntityAndDeleteEnrolledEventWhenEventIsDeleted() {
    TrackedEntity entityBeforeUpdate = getTrackedEntity();

    Enrollment enrollmentBeforeDeletion = getEnrollment();

    Event eventBeforeDeletion = getEvent();

    User user = user();

    clearSession();

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    params.setUserId(user.getUid());

    assertNoErrors(
        trackerImportService.importTracker(
            params, TrackerObjects.builder().events(List.of(event)).build()));

    clearSession();

    TrackedEntity entityAfterDeletion = getTrackedEntity();
    Enrollment enrollmentAfterDeletion = getEnrollment();

    Event eventAfterDeletion = getEvent();

    assertAll(
        () ->
            assertTrue(
                entityAfterDeletion.getLastUpdated().getTime()
                    > entityBeforeUpdate.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
                    trackedEntity.getUid())),
        () ->
            assertEquals(
                entityAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for tracked entity %s. The lastUpdatedByUserinfo has not been saved during the import",
                    trackedEntity.getUid())),
        () ->
            assertTrue(
                enrollmentAfterDeletion.getLastUpdated().getTime()
                    > enrollmentBeforeDeletion.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for enrollment %s. The lastUpdated date has not been updated after the import",
                    enrollment.getUid())),
        () ->
            assertEquals(
                enrollmentAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for enrollment %s. The lastUpdatedByUserinfo has not been saved during the import",
                    enrollment.getUid())),
        () -> assertTrue(eventAfterDeletion.isDeleted()),
        () ->
            assertTrue(
                eventAfterDeletion.getLastUpdated().getTime()
                    > eventBeforeDeletion.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for event %s. The lastUpdated date has not been updated after the import",
                    event.getUid())),
        () ->
            assertEquals(
                eventAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for event %s. The lastUpdatedByUserinfo has not been saved during the import",
                    event.getUid())));
  }

  @Test
  void shouldUpdatedEventProgramWhenEventIsDeleted() throws IOException {

    org.hisp.dhis.tracker.imports.domain.Event ev = importEventProgram();

    Event eventBeforeDeletion = getEvent(ev.getUid());

    User user = user();

    clearSession();

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    params.setUserId(user.getUid());

    assertNoErrors(
        trackerImportService.importTracker(
            params, TrackerObjects.builder().events(List.of(ev)).build()));

    clearSession();

    Event eventAfterDeletion = getEvent(ev.getUid());

    assertAll(
        () -> assertTrue(eventAfterDeletion.isDeleted()),
        () ->
            assertTrue(
                eventAfterDeletion.getLastUpdated().getTime()
                    > eventBeforeDeletion.getLastUpdated().getTime(),
                String.format(
                    "Data integrity error for event %s. The lastUpdated date has not been updated after the import",
                    event.getUid())),
        () ->
            assertEquals(
                eventAfterDeletion.getLastUpdatedByUserInfo().getUid(),
                UserInfoSnapshot.from(user).getUid(),
                String.format(
                    "Data integrity error for event %s. The lastUpdatedByUserinfo has not been saved during the import",
                    event.getUid())));
  }

  private void assertTrackedEntityUpdated(
      TrackedEntity entityBeforeUpdate, TrackedEntity entityAfterUpdate, User user) {
    assertTrue(
        entityAfterUpdate.getLastUpdated().getTime()
            > entityBeforeUpdate.getLastUpdated().getTime(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdated date has not been updated"
                + " after the import",
            entityAfterUpdate.getUid()));
    assertEquals(
        user.getUid(),
        entityAfterUpdate.getLastUpdatedByUserInfo().getUid(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdatedByUserinfo has not been"
                + " updated during the import",
            entityAfterUpdate.getUid()));
  }

  private void assertTrackedEntityNotUpdated(
      TrackedEntity entityBeforeUpdate, TrackedEntity entityAfterUpdate) {
    assertEquals(
        entityBeforeUpdate.getLastUpdated().getTime(),
        entityAfterUpdate.getLastUpdated().getTime(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdated date has been updated after the import",
            entityAfterUpdate.getUid()));
    assertEquals(
        entityBeforeUpdate.getLastUpdatedByUserInfo().getUid(),
        entityAfterUpdate.getLastUpdatedByUserInfo().getUid(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdatedByUserinfo has been updated during the import",
            entityAfterUpdate.getUid()));
  }

  private User user() {
    return createAndAddUser(CodeGenerator.generateUid(), organisationUnit);
  }

  private org.hisp.dhis.tracker.imports.domain.Event importEventProgram() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/single_event.json");
    org.hisp.dhis.tracker.imports.domain.Event ev = trackerObjects.getEvents().get(0);
    ev.setEnrollment(null);
    ev.setEvent(CodeGenerator.generateUid());
    // set event program and program stage
    ev.setProgramStage(MetadataIdentifier.of(TrackerIdScheme.UID, "NpsdDv6kKSe", null));
    ev.setProgram(MetadataIdentifier.of(TrackerIdScheme.UID, "BFcipDERJne", null));

    assertNoErrors(
        trackerImportService.importTracker(
            new TrackerImportParams(), TrackerObjects.builder().events(List.of(ev)).build()));

    return ev;
  }

  void enrollTrackerEntity() {
    trackedEntity.setEnrollments(List.of(enrollment));

    assertNoErrors(
        trackerImportService.importTracker(
            new TrackerImportParams(),
            TrackerObjects.builder().trackedEntities(List.of(trackedEntity)).build()));
  }

  /**
   * Flush the session to synchronize the hibernate objects with the database and clear the
   * references used during the import, so we can compare an object before and after
   */
  void clearSession() {
    dbmsManager.clearSession();
  }

  Enrollment getEnrollment() {
    return getEntityJpql(Enrollment.class.getSimpleName(), enrollment.getUid());
  }

  Event getEvent() {
    return getEntityJpql(Event.class.getSimpleName(), event.getUid());
  }

  Event getEvent(String uid) {
    return getEntityJpql(Event.class.getSimpleName(), uid);
  }

  TrackedEntity getTrackedEntity() {
    return getEntityJpql(TrackedEntity.class.getSimpleName(), trackedEntity.getUid());
  }

  TrackedEntity getTrackedEntity(String uid) {
    return getEntityJpql(TrackedEntity.class.getSimpleName(), uid);
  }

  /**
   * Get with the entity manager because some Store exclude deleted and {@link
   * TrackedEntityService#getTrackedEntities} use async Spring jdbc Template. So we use the same
   * query for all the entities
   */
  @SuppressWarnings("unchecked")
  public <T extends SoftDeletableObject> T getEntityJpql(String entity, String uid) {

    return (T)
        entityManager
            .createQuery("SELECT e FROM " + entity + " e WHERE e.uid = :uid")
            .setParameter("uid", uid)
            .getSingleResult();
  }
}
