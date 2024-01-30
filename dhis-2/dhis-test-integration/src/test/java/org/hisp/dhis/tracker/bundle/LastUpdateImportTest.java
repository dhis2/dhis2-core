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
package org.hisp.dhis.tracker.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LastUpdateImportTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private SessionFactory sessionFactory;
  private org.hisp.dhis.tracker.domain.TrackedEntity trackedEntity;
  private org.hisp.dhis.tracker.domain.Enrollment enrollment;
  private org.hisp.dhis.tracker.domain.Event event;

  private OrganisationUnit organisationUnit;

  @Autowired protected UserService _userService;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");
    injectAdminUser();

    TrackerImportParams trackerImportParams = fromJson("tracker/single_tei.json");
    assertNoErrors(trackerImportService.importTracker(trackerImportParams));

    trackedEntity = trackerImportParams.getTrackedEntities().get(0);

    trackerImportParams = fromJson("tracker/single_enrollment.json");
    assertNoErrors(trackerImportService.importTracker(trackerImportParams));

    enrollment = trackerImportParams.getEnrollments().get(0);

    trackerImportParams = fromJson("tracker/single_event.json");
    assertNoErrors(trackerImportService.importTracker(trackerImportParams));

    event = trackerImportParams.getEvents().get(0);

    organisationUnit =
        manager.get(OrganisationUnit.class, trackedEntity.getOrgUnit().getIdentifier());
  }

  @Test
  void shouldUpdateTeiWhenTeiIsUpdated() throws IOException {

    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    clearSession();

    TrackerImportParams trackerImportParams = fromJson("tracker/single_tei.json");
    trackerImportParams.setImportStrategy(TrackerImportStrategy.UPDATE);

    assertNoErrors(trackerImportService.importTracker(trackerImportParams));

    clearSession();

    Date lastUpdateAfter = getTrackedEntity().getLastUpdated();

    assertTrue(
        lastUpdateAfter.getTime() > entityBeforeUpdate.getLastUpdated().getTime(),
        String.format(
            "Data integrity error for tracked entity %s. The lastUpdated date has not been updated after the import",
            trackedEntity.getUid()));
  }

  @Test
  void shouldUpdateTeiWhenEventIsUpdated() throws IOException {
    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    clearSession();

    User user = user();

    clearSession();

    TrackerImportParams trackerImportParams =
        fromJson("tracker/event_with_updated_data_values.json");
    trackerImportParams.setImportStrategy(TrackerImportStrategy.UPDATE);
    trackerImportParams.setUserId(user.getUid());

    assertNoErrors(trackerImportService.importTracker(trackerImportParams));

    clearSession();

    TrackedEntityInstance entityAfterUpdate = getTrackedEntity();

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

    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    User user = user();

    clearSession();

    enrollment.setStatus(EnrollmentStatus.COMPLETED);

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.UPDATE)
            .enrollments(List.of(enrollment))
            .userId(user.getUid())
            .build();

    assertNoErrors(trackerImportService.importTracker(params));

    clearSession();

    TrackedEntityInstance entityAfterUpdate = getTrackedEntity();

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

    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    clearSession();

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.DELETE)
            .trackedEntities(List.of(trackedEntity))
            .build();

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
    assertEquals(1, importReport.getStats().getDeleted());

    clearSession();

    TrackedEntityInstance entityAfterDeletion = getTrackedEntity();

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
    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    ProgramInstance enrollmentBeforeDelete = getEnrollment();

    enrollTrackerEntity();

    clearSession();

    // delete cascade
    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.DELETE)
            .trackedEntities(List.of(trackedEntity))
            .build();

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);
    assertEquals(1, importReport.getStats().getDeleted());

    clearSession();

    TrackedEntityInstance entityAfterDelete = getTrackedEntity();
    ProgramInstance enrollmentAfterDelete = getEnrollment();

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
    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    ProgramInstance enrollmentBeforeDeletion = getEnrollment();

    ProgramStageInstance eventBeforeDeletion = getEvent();

    User user = createAndAddUser("userDelete", organisationUnit, "ALL");

    clearSession();

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.DELETE)
            .enrollments(List.of(enrollment))
            .userId(user.getUid())
            .build();

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
    assertEquals(1, importReport.getStats().getDeleted());

    clearSession();

    TrackedEntityInstance entityAfterDeletion = getTrackedEntity();
    ProgramInstance enrollmentAfterDeletion = getEnrollment();
    ProgramStageInstance eventAfterDeletion = getEvent();

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
    TrackedEntityInstance entityBeforeUpdate = getTrackedEntity();

    ProgramInstance enrollmentBeforeDeletion = getEnrollment();

    ProgramStageInstance eventBeforeDeletion = getEvent();

    User user = user();

    clearSession();

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.DELETE)
            .events(List.of(event))
            .userId(user.getUid())
            .build();

    assertNoErrors(trackerImportService.importTracker(params));

    clearSession();

    TrackedEntityInstance entityAfterDeletion = getTrackedEntity();
    ProgramInstance enrollmentAfterDeletion = getEnrollment();

    ProgramStageInstance eventAfterDeletion = getEvent();

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

    org.hisp.dhis.tracker.domain.Event ev = importEventProgram();

    ProgramStageInstance eventBeforeDeletion = getEvent(ev.getUid());

    User user = user();

    clearSession();

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.DELETE)
            .events(List.of(ev))
            .userId(user.getUid())
            .build();

    assertNoErrors(trackerImportService.importTracker(params));

    clearSession();

    ProgramStageInstance eventAfterDeletion = getEvent(ev.getUid());

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

  private User user() {
    return createAndAddUser(CodeGenerator.generateUid(), organisationUnit, "ALL");
  }

  private org.hisp.dhis.tracker.domain.Event importEventProgram() throws IOException {
    TrackerImportParams trackerObjects = fromJson("tracker/single_event.json");
    org.hisp.dhis.tracker.domain.Event ev = trackerObjects.getEvents().get(0);
    ev.setEnrollment(null);
    ev.setEvent(CodeGenerator.generateUid());
    // set event program and program stage
    ev.setProgramStage(MetadataIdentifier.of(TrackerIdScheme.UID, "NpsdDv6kKSe", null));
    ev.setProgram(MetadataIdentifier.of(TrackerIdScheme.UID, "BFcipDERJne", null));

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().events(List.of(ev)).build()));

    return ev;
  }

  void enrollTrackerEntity() {
    trackedEntity.setEnrollments(List.of(enrollment));

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().trackedEntities(List.of(trackedEntity)).build()));
  }

  /**
   * Flush the session to synchronize the hibernate objects with the database and clear the
   * references used during the import, so we can compare an object before and after
   */
  void clearSession() {
    dbmsManager.clearSession();
  }

  ProgramInstance getEnrollment() {
    return getEntityJpql(ProgramInstance.class.getSimpleName(), enrollment.getUid());
  }

  ProgramStageInstance getEvent() {
    return getEntityJpql(ProgramStageInstance.class.getSimpleName(), event.getUid());
  }

  ProgramStageInstance getEvent(String uid) {
    return getEntityJpql(ProgramStageInstance.class.getSimpleName(), uid);
  }

  TrackedEntityInstance getTrackedEntity() {
    return getEntityJpql(TrackedEntityInstance.class.getSimpleName(), trackedEntity.getUid());
  }

  /**
   * Get with the current session because some Store exclude deleted and {@link
   * TrackedEntityInstanceService#getTrackedEntityInstances} ies} use async Spring jdbc Template. So
   * we use the same query for all the entities
   */
  @SuppressWarnings("unchecked")
  public <T extends SoftDeletableObject> T getEntityJpql(String entity, String uid) {

    return (T)
        sessionFactory
            .getCurrentSession()
            .createQuery("SELECT e FROM " + entity + " e WHERE e.uid = :uid")
            .setParameter("uid", uid)
            .getSingleResult();
  }
}
