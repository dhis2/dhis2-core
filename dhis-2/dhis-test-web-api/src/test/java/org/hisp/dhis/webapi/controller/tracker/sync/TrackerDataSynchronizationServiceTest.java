/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.sync;

import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Stats;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

/**
 * Integration-tier tests for {@link TrackerDataSynchronizationService}: real Postgres, real tracker
 * importer creating the fixtures, real pagination/timestamp persistence.
 *
 * <p>The remote instance is stood in for by {@link MockRestServiceServer} wrapping the app's real,
 * shared {@link RestTemplate} bean. The mocked responses are plain, static {@link ImportReport}s
 * built from UIDs the test already knows (since the test created the fixtures) — no request
 * parsing, no capturing, no replaying. That's a deliberate simplicity trade-off: it doesn't verify
 * the exact shape of what the service sends, only how it behaves given a certain response. If a
 * payload field gets renamed or dropped, these tests won't catch it — the unit tests in {@code
 * TrackerDataSynchronizationServiceTest} cover the decision logic, and this class is kept simple on
 * purpose rather than trying to cover everything.
 *
 * <p>Deliberately NOT {@code @Transactional}: {@code TrackedEntityFields.all()} makes {@link
 * TrackerDataSynchronizationService} fetch enrollments via {@code EnrollmentAggregate}, which runs
 * on a separate thread with its own DB connection. A per-test uncommitted transaction would be
 * invisible to that thread, so fixtures must actually commit.
 */
class TrackerDataSynchronizationServiceTest extends PostgresControllerIntegrationTestBase {

  private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
      new com.fasterxml.jackson.databind.ObjectMapper();

  @Autowired private TrackerDataSynchronizationService syncService;
  @Autowired private RestTemplate restTemplate;
  @Autowired private SystemSettingsService systemSettingsService;

  private MockRestServiceServer mockServer;
  private String orgUnitUid;
  private String programUid;
  private String programStageUid;
  private String trackedEntityTypeUid;

  @BeforeEach
  void setUpFixturesAndRemote() {
    mockServer = MockRestServiceServer.createServer(restTemplate);

    // explicit fresh UIDs: the test framework truncates all tables after every non-@Transactional
    // test method (see SpringIntegrationTestExtension#tearDown), so there's no actual collision
    // risk between methods here — this just avoids relying on that framework detail and on the
    // fixed char-derived UIDs some createXxx('S') helpers (e.g. createOrganisationUnit) default
    // to, which are shared by convention across many other test classes in the codebase.
    TrackedEntityType type = createTrackedEntityType('S');
    type.setUid(CodeGenerator.generateUid());
    manager.save(type);
    trackedEntityTypeUid = type.getUid();

    OrganisationUnit orgUnit = createOrganisationUnit('S');
    orgUnit.setUid(CodeGenerator.generateUid());
    manager.save(orgUnit);
    orgUnitUid = orgUnit.getUid();
    getAdminUser().addOrganisationUnit(orgUnit);
    getAdminUser().setTeiSearchOrganisationUnits(java.util.Set.of(orgUnit));
    userService.updateUser(getAdminUser());
    switchToAdminUser();

    Program program = createProgram('S');
    program.setUid(CodeGenerator.generateUid());
    program.setOrganisationUnits(java.util.Set.of(orgUnit));
    program.setTrackedEntityType(type);
    manager.save(program);
    programUid = program.getUid();

    ProgramStage programStage = createProgramStage('S', program);
    programStage.setUid(CodeGenerator.generateUid());
    manager.save(programStage);
    programStageUid = programStage.getUid();

    systemSettingsService.put("keyRemoteInstanceUrl", "http://remote");
    systemSettingsService.put("keyRemoteInstanceUsername", "admin");
    systemSettingsService.put("keyRemoteInstancePassword", "district");
    systemSettingsService.put("syncSkipSyncForDataChangedBefore", new Date(0));
    systemSettingsService.clearCurrentSettings();

    expectPing();
  }

  // TrackerDataSynchronizationWithPaging#validatePreconditions checks the remote is reachable
  // before doing any real work, so every synchronizeTrackerData() call does one of these first.
  private void expectPing() {
    mockServer
        .expect(requestTo(containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("true", MediaType.TEXT_PLAIN));
  }

  @Test
  void shouldUpdateLastSynchronizedWhenTrackedEntitySyncedSuccessfully() {
    String teUid = importTrackedEntity();

    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertTrue(te.getLastSynchronized().getTime() > 0, "lastSynchronized should have been updated");
  }

  @Test
  void shouldNotUpdateLastSynchronizedWhenChildEventFailedRemotely() {
    String teUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(teUid);
    String eventUid = importEvent(enrollmentUid);

    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeededWithChildFailure(teUid, TrackerType.EVENT, eventUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertEquals(
        new Date(0),
        te.getLastSynchronized(),
        "TE must stay eligible for the next sync since its child event failed remotely");
  }

  @Test
  void shouldNotUpdateLastSynchronizedWhenChildEnrollmentFailedRemotely() {
    String teUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(teUid);

    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeededWithChildFailure(teUid, TrackerType.ENROLLMENT, enrollmentUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertEquals(
        new Date(0),
        te.getLastSynchronized(),
        "TE must stay eligible for the next sync since its child enrollment failed remotely");
  }

  @Test
  void shouldOnlyUpdateLastSynchronizedForTrackedEntitiesThatFullySucceeded() {
    String healthyTeUid = importTrackedEntity();
    String failingTeUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(failingTeUid);
    String eventUid = importEvent(enrollmentUid);

    TrackerTypeReport teReport = new TrackerTypeReport(TrackerType.TRACKED_ENTITY);
    teReport.addEntity(new Entity(TrackerType.TRACKED_ENTITY, UID.of(healthyTeUid), List.of()));
    teReport.addEntity(new Entity(TrackerType.TRACKED_ENTITY, UID.of(failingTeUid), List.of()));

    TrackerTypeReport eventReport = new TrackerTypeReport(TrackerType.EVENT);
    eventReport.addEntity(
        new Entity(
            TrackerType.EVENT,
            UID.of(eventUid),
            List.of(
                Error.builder()
                    .message("validation failed")
                    .errorCode("E0000")
                    .trackerType(TrackerType.EVENT.name())
                    .uid(UID.of(eventUid))
                    .args(List.of())
                    .build())));

    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            jsonResponse(
                Status.ERROR,
                Map.of(TrackerType.TRACKED_ENTITY, teReport, TrackerType.EVENT, eventReport)));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity healthyTe = manager.get(TrackedEntity.class, healthyTeUid);
    TrackedEntity failingTe = manager.get(TrackedEntity.class, failingTeUid);
    assertTrue(
        healthyTe.getLastSynchronized().getTime() > 0,
        "the fully-synced TE should have been marked synchronized");
    assertEquals(
        new Date(0),
        failingTe.getLastSynchronized(),
        "the TE with a failed child must stay eligible for the next sync");
  }

  @Test
  void shouldSyncAllTrackedEntitiesAcrossMultiplePagesWithoutSkippingAny() {
    List<String> teUids = new java.util.ArrayList<>();
    for (int i = 0; i < 6; i++) {
      teUids.add(importTrackedEntity());
    }

    // pageSize 2 forces 3 round-trips (6 TEs / page size 2 = 3 pages)
    mockServer
        .expect(times(3), requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUids.toArray(new String[0])));

    syncService.synchronizeTrackerData(2, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    for (String teUid : teUids) {
      TrackedEntity te = manager.get(TrackedEntity.class, teUid);
      assertTrue(
          te.getLastSynchronized().getTime() > 0,
          "TE " + teUid + " should have been synced across the paged run");
    }
  }

  @Test
  void shouldUpdateLastSynchronizedWhenDeletedEventIsSyncedSuccessfully() {
    String teUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(teUid);
    String eventUid = importEvent(enrollmentUid);
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"events\":[{\"event\":\"" + eventUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.EVENT, eventUid));
    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertTrue(
        te.getLastSynchronized().getTime() > 0,
        "lastSynchronized should have been updated after the delete was synced");
  }

  @Test
  void shouldUpdateLastSynchronizedWhenDeletedTrackedEntityIsSyncedSuccessfully() {
    String teUid = importTrackedEntity();
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"trackedEntities\":[{\"trackedEntity\":\"" + teUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertTrue(
        te.getLastSynchronized().getTime() > 0,
        "lastSynchronized should have been updated after the TE delete was synced");
  }

  @Test
  void shouldUpdateLastSynchronizedWhenDeletedEnrollmentIsSyncedSuccessfully() {
    String teUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(teUid);
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"enrollments\":[{\"enrollment\":\"" + enrollmentUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.ENROLLMENT, enrollmentUid));
    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertTrue(
        te.getLastSynchronized().getTime() > 0,
        "lastSynchronized should have been updated after the enrollment delete was synced");
  }

  @Test
  void shouldUpdateLastSynchronizedWhenDeletedRelationshipIsSyncedSuccessfully() {
    String relationshipTypeUid = createPersonToPersonRelationshipTypeFor(trackedEntityTypeUid);
    String motherUid = importTrackedEntity();
    String childUid = importTrackedEntity();
    String relationshipUid = importRelationship(motherUid, childUid, relationshipTypeUid);
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"relationships\":[{\"relationship\":\"" + relationshipUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.RELATIONSHIP, relationshipUid));
    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, motherUid, childUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity mother = manager.get(TrackedEntity.class, motherUid);
    TrackedEntity child = manager.get(TrackedEntity.class, childUid);
    assertTrue(
        mother.getLastSynchronized().getTime() > 0,
        "lastSynchronized should have been updated for the TE owning the deleted relationship"
            + " after the delete was synced");
    assertTrue(
        child.getLastSynchronized().getTime() > 0,
        "lastSynchronized should have been updated for the other end of the deleted relationship"
            + " too");
  }

  @Test
  void shouldUpdateLastSynchronizedWhenTrackedEntityWithRelationshipSyncedSuccessfully() {
    String relationshipTypeUid = createPersonToPersonRelationshipTypeFor(trackedEntityTypeUid);
    String motherUid = importTrackedEntity();
    String childUid = importTrackedEntity();
    importRelationship(motherUid, childUid, relationshipTypeUid);
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, motherUid, childUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity mother = manager.get(TrackedEntity.class, motherUid);
    assertTrue(
        mother.getLastSynchronized().getTime() > 0,
        "lastSynchronized should have been updated for a TE carrying a relationship");
  }

  @Test
  void shouldUpdateLastSynchronizedWhenDeletedChildFailedBecauseAlreadyDeletedRemotely() {
    String teUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(teUid);
    String eventUid = importEvent(enrollmentUid);
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"events\":[{\"event\":\"" + eventUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(failedWithCode(TrackerType.EVENT, eventUid, "E1082"));
    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertTrue(
        te.getLastSynchronized().getTime() > 0,
        "TE should be marked synced when its deleted child's only failure means the delete had"
            + " already happened");
  }

  @Test
  void shouldNotUpdateLastSynchronizedWhenDeletedChildFailedForRealReason() {
    String teUid = importTrackedEntity();
    String enrollmentUid = importEnrollment(teUid);
    String eventUid = importEvent(enrollmentUid);
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"events\":[{\"event\":\"" + eventUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(failedWithCode(TrackerType.EVENT, eventUid, "E0000"));
    mockServer
        .expect(requestTo(containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(succeeded(TrackerType.TRACKED_ENTITY, teUid));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertEquals(
        new Date(0),
        te.getLastSynchronized(),
        "TE must stay eligible for the next sync since its deleted child failed to delete for a"
            + " real reason, not because it was already deleted");
  }

  @Test
  void shouldUpdateLastSynchronizedWhenTrackedEntityDeleteAlreadyHappenedRemotely() {
    String teUid = importTrackedEntity();
    entityManager.clear();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?importStrategy=DELETE&async=false",
            "{\"trackedEntities\":[{\"trackedEntity\":\"" + teUid + "\"}]}"));
    entityManager.clear();

    mockServer
        .expect(requestTo(containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(failedWithCode(TrackerType.TRACKED_ENTITY, teUid, "E1114"));

    syncService.synchronizeTrackerData(50, JobProgress.noop());
    mockServer.verify();

    entityManager.clear();
    TrackedEntity te = manager.get(TrackedEntity.class, teUid);
    assertTrue(
        te.getLastSynchronized().getTime() > 0,
        "a TE's own delete should count as achieved when the remote reports it as already"
            + " deleted, so it stops being resent forever");
  }

  private String createPersonToPersonRelationshipTypeFor(String trackedEntityTypeUid) {
    TrackedEntityType type = manager.get(TrackedEntityType.class, trackedEntityTypeUid);
    RelationshipType relationshipType =
        createPersonToPersonRelationshipType('R', null, type, false);
    relationshipType.setUid(CodeGenerator.generateUid());
    manager.save(relationshipType);
    return relationshipType.getUid();
  }

  private String importTrackedEntity() {
    String teUid = CodeGenerator.generateUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?async=false",
            "{\"trackedEntities\":[{\"trackedEntity\":\""
                + teUid
                + "\",\"trackedEntityType\":\""
                + trackedEntityTypeUid
                + "\",\"orgUnit\":\""
                + orgUnitUid
                + "\"}]}"));
    return teUid;
  }

  private String importEnrollment(String teUid) {
    String enrollmentUid = CodeGenerator.generateUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?async=false",
            "{\"enrollments\":[{\"enrollment\":\""
                + enrollmentUid
                + "\",\"trackedEntity\":\""
                + teUid
                + "\",\"program\":\""
                + programUid
                + "\",\"orgUnit\":\""
                + orgUnitUid
                + "\",\"status\":\"ACTIVE\",\"enrolledAt\":\"2024-01-01\",\"occurredAt\":\"2024-01-01\"}]}"));
    return enrollmentUid;
  }

  private String importEvent(String enrollmentUid) {
    String eventUid = CodeGenerator.generateUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?async=false",
            "{\"events\":[{\"event\":\""
                + eventUid
                + "\",\"enrollment\":\""
                + enrollmentUid
                + "\",\"program\":\""
                + programUid
                + "\",\"programStage\":\""
                + programStageUid
                + "\",\"orgUnit\":\""
                + orgUnitUid
                + "\",\"status\":\"ACTIVE\",\"occurredAt\":\"2024-01-01\"}]}"));
    return eventUid;
  }

  private String importRelationship(String fromTeUid, String toTeUid, String relationshipTypeUid) {
    String relationshipUid = CodeGenerator.generateUid();
    assertStatus(
        HttpStatus.OK,
        POST(
            "/tracker?async=false",
            "{\"relationships\":[{\"relationship\":\""
                + relationshipUid
                + "\",\"relationshipType\":\""
                + relationshipTypeUid
                + "\",\"from\":{\"trackedEntity\":{\"trackedEntity\":\""
                + fromTeUid
                + "\"}},\"to\":{\"trackedEntity\":{\"trackedEntity\":\""
                + toTeUid
                + "\"}}}]}"));
    return relationshipUid;
  }

  private static ResponseCreator succeeded(TrackerType type, String... uids) {
    TrackerTypeReport typeReport = new TrackerTypeReport(type);
    for (String uid : uids) {
      typeReport.addEntity(new Entity(type, UID.of(uid), List.of()));
    }
    return jsonResponse(Status.OK, Map.of(type, typeReport));
  }

  private static ResponseCreator succeededWithChildFailure(
      String teUid, TrackerType childType, String failedChildUid) {
    TrackerTypeReport teReport = new TrackerTypeReport(TrackerType.TRACKED_ENTITY);
    teReport.addEntity(new Entity(TrackerType.TRACKED_ENTITY, UID.of(teUid), List.of()));

    TrackerTypeReport childReport = new TrackerTypeReport(childType);
    childReport.addEntity(
        new Entity(
            childType,
            UID.of(failedChildUid),
            List.of(
                Error.builder()
                    .message("validation failed")
                    .errorCode("E0000")
                    .trackerType(childType.name())
                    .uid(UID.of(failedChildUid))
                    .args(List.of())
                    .build())));

    return jsonResponse(
        Status.ERROR, Map.of(TrackerType.TRACKED_ENTITY, teReport, childType, childReport));
  }

  private static ResponseCreator failedWithCode(TrackerType type, String uid, String errorCode) {
    TrackerTypeReport typeReport = new TrackerTypeReport(type);
    typeReport.addEntity(
        new Entity(
            type,
            UID.of(uid),
            List.of(
                Error.builder()
                    .message("validation failed")
                    .errorCode(errorCode)
                    .trackerType(type.name())
                    .uid(UID.of(uid))
                    .args(List.of())
                    .build())));
    return jsonResponse(Status.ERROR, Map.of(type, typeReport));
  }

  // Mirrors TrackerImportController#importSync: a synchronous /api/tracker import responds with
  // 409 CONFLICT whenever the report status is ERROR, and 200 otherwise. MockRestServiceServer
  // doesn't do this for us, so tests must reproduce it or they don't exercise the real
  // HttpClientErrorException path the service has to handle.
  private static ResponseCreator jsonResponse(
      Status status, Map<TrackerType, TrackerTypeReport> typeReports) {
    ImportReport report =
        ImportReport.builder()
            .status(status)
            .persistenceReport(new PersistenceReport(new EnumMap<>(typeReports)))
            .stats(new Stats())
            .build();
    try {
      String json = OBJECT_MAPPER.writeValueAsString(report);
      return status == Status.ERROR
          ? withStatus(org.springframework.http.HttpStatus.CONFLICT)
              .body(json)
              .contentType(MediaType.APPLICATION_JSON)
          : withSuccess(json, MediaType.APPLICATION_JSON);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
