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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Stats;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class TrackerDataSynchronizationServiceTest {

  private static final String REMOTE_URL = "http://remote";
  private static final String PING_RESPONSE = "true";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .configure(
              com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
              false);

  @Mock private TrackedEntityService trackedEntityService;
  @Mock private ProgramStageDataElementService programStageDataElementService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private RenderService renderService;

  private MockRestServiceServer mockServer;
  private TrackerDataSynchronizationService service;

  @BeforeEach
  void setUp() throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    service =
        new TrackerDataSynchronizationService(
            trackedEntityService,
            programStageDataElementService,
            systemSettingsService,
            restTemplate,
            renderService);

    // renderService is mocked, so its fromJson() call inside the service (which parses the
    // remote's ImportReport response) must be wired to actually parse, or it silently returns
    // null and the service aborts with "Tracker sync returned null response".
    when(renderService.fromJson(any(java.io.InputStream.class), eq(ImportReport.class)))
        .thenAnswer(
            invocation ->
                OBJECT_MAPPER.readValue(
                    (java.io.InputStream) invocation.getArgument(0), ImportReport.class));

    mockSettings();
  }

  @Test
  void shouldSendDeleteBeforeCreateAndUpdateWhenEnrollmentHasDeletedEvent() throws Exception {
    stubTrackedEntityService(buildTeWithEnrollmentContaining("ActEvtUidAB", "DelEvtUidAB"));
    expectDeleteBeforeCreateAndUpdate(successReport(), successReport());

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
  }

  @Test
  void shouldOnlySendCreateAndUpdateWhenNoDeletedChildren() throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("ActEvtUidAB", null);
    stubTrackedEntityService(te);

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(successReport()));

    service.synchronizeTrackerData(100, JobProgress.noop());

    verify(trackedEntityService).getTrackedEntityCount(any());
    verify(trackedEntityService).findTrackedEntities(any(), any());
    mockServer.verify();
  }

  @Test
  void shouldStripDeletedEventFromCreateAndUpdatePayload() throws Exception {
    stubTrackedEntityService(buildTeWithEnrollmentContaining("ActEvtUidAB", "DelEvtUidAB"));
    expectDeleteBeforeCreateAndUpdate(successReport(), successReport());

    List<Map<?, ?>> payloads = synchronizeAndCapturePayloads();

    Map<?, ?> deleteBody = payloads.get(0);
    assertEquals(
        Set.of("trackedEntities", "enrollments", "events", "relationships"), deleteBody.keySet());
    assertEquals(0, ((List<?>) deleteBody.get("trackedEntities")).size());
    assertEquals(1, ((List<?>) deleteBody.get("events")).size());

    List<?> teList = (List<?>) payloads.get(1).get("trackedEntities");
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity teDto =
        (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity) teList.get(0);
    List<org.hisp.dhis.webapi.controller.tracker.view.Enrollment> enrollments =
        teDto.getEnrollments();
    assertEquals(1, enrollments.size());
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> events =
        enrollments.get(0).getEvents();
    assertEquals(1, events.size());
    assertEquals("ActEvtUidAB", events.get(0).getEvent().getValue());
  }

  @Test
  void shouldStripDeletedEnrollmentFromCreateAndUpdatePayload() throws Exception {
    stubTrackedEntityService(buildTeWithDeletedAndActiveEnrollment());
    expectDeleteBeforeCreateAndUpdate(successReport(), successReport());

    List<Map<?, ?>> payloads = synchronizeAndCapturePayloads();

    Map<?, ?> deleteBody = payloads.get(0);
    assertEquals(
        Set.of("trackedEntities", "enrollments", "events", "relationships"), deleteBody.keySet());
    assertEquals(0, ((List<?>) deleteBody.get("trackedEntities")).size());
    List<?> deletedEnrollments = (List<?>) deleteBody.get("enrollments");
    assertEquals(1, deletedEnrollments.size());
    assertEquals(
        "DelEnrlUiAB",
        ((org.hisp.dhis.webapi.controller.tracker.view.Enrollment) deletedEnrollments.get(0))
            .getEnrollment()
            .getValue());
    assertEquals(0, ((List<?>) deleteBody.get("events")).size());

    List<?> teList = (List<?>) payloads.get(1).get("trackedEntities");
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity teDto =
        (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity) teList.get(0);
    assertEquals(1, teDto.getEnrollments().size());
    assertEquals("ActEnrlUiAB", teDto.getEnrollments().get(0).getEnrollment().getValue());
  }

  @Test
  void shouldStripDeletedEnrollmentRelationshipFromCreateAndUpdatePayload() throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("ActEvtUidAB", null);
    Enrollment enrollment = te.getEnrollments().iterator().next();
    attachDeletedRelationshipToEnrollment(enrollment, "DelRelUidAB");
    stubTrackedEntityService(te);
    expectDeleteBeforeCreateAndUpdate(successReport(), successReport());

    List<Map<?, ?>> payloads = synchronizeAndCapturePayloads();

    Map<?, ?> deleteBody = payloads.get(0);
    List<?> deletedRelationships = (List<?>) deleteBody.get("relationships");
    assertEquals(1, deletedRelationships.size());
    assertEquals(
        "DelRelUidAB",
        ((org.hisp.dhis.webapi.controller.tracker.view.Relationship) deletedRelationships.get(0))
            .getRelationship()
            .getValue());

    List<?> teList = (List<?>) payloads.get(1).get("trackedEntities");
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity teDto =
        (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity) teList.get(0);
    assertEquals(0, teDto.getEnrollments().get(0).getRelationships().size());
  }

  @Test
  void shouldStripSkipSyncAttributesAndDataElementsFromCreateAndUpdatePayload() throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("ActEvtUidAB", null);

    TrackedEntityAttribute skipAttribute = new TrackedEntityAttribute();
    skipAttribute.setUid("SkipAttrUAB");
    skipAttribute.setSkipSynchronization(true);
    TrackedEntityAttribute keepAttribute = new TrackedEntityAttribute();
    keepAttribute.setUid("KeepAttrUAB");
    te.setTrackedEntityAttributeValues(
        new LinkedHashSet<>(
            List.of(
                new TrackedEntityAttributeValue(skipAttribute, te, "skip-me"),
                new TrackedEntityAttributeValue(keepAttribute, te, "keep-me"))));

    Event event = te.getEnrollments().iterator().next().getEvents().iterator().next();
    event.setEventDataValues(
        new java.util.HashSet<>(
            List.of(
                new EventDataValue("SkipDataElAB", "skip-value"),
                new EventDataValue("KeepDataElAB", "keep-value"))));

    when(trackedEntityService.getTrackedEntityCount(any())).thenReturn(1L);
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(List.of(te), 1, 100, 1L, null, null));
    when(programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue())
        .thenReturn(Map.of("ProgStageAB", Set.of("SkipDataElAB")));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(successReport()));

    ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
    service.synchronizeTrackerData(100, JobProgress.noop());
    verify(renderService, times(1)).toJson(any(), bodyCaptor.capture());
    mockServer.verify();

    Map<?, ?> payload = (Map<?, ?>) bodyCaptor.getValue();
    List<?> teList = (List<?>) payload.get("trackedEntities");
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity teDto =
        (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity) teList.get(0);

    assertEquals(
        List.of("KeepAttrUAB"),
        teDto.getAttributes().stream()
            .map(org.hisp.dhis.webapi.controller.tracker.view.Attribute::getAttribute)
            .toList());

    List<org.hisp.dhis.webapi.controller.tracker.view.DataValue> dataValues =
        teDto.getEnrollments().get(0).getEvents().get(0).getDataValues().stream().toList();
    assertEquals(
        List.of("KeepDataElAB"),
        dataValues.stream()
            .map(org.hisp.dhis.webapi.controller.tracker.view.DataValue::getDataElement)
            .toList());
  }

  @Test
  void shouldNotMarkTrackedEntitySyncedWhenChildEventFailedRemotely() throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("FailEvtUidA", null);
    stubTrackedEntityService(te);

    ImportReport reportWithFailedEvent =
        reportWith(
            successEntity(TrackerType.TRACKED_ENTITY, "TeUid1234AB"),
            failedEntity(TrackerType.EVENT, "FailEvtUidA", "E1032", "Event validation failed"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(reportWithFailedEvent));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(trackedEntityService, times(0)).updateTrackedEntitiesSyncTimestamp(any(), any());
  }

  @Test
  void shouldNotMarkTrackedEntitySyncedWhenChildRelationshipFailedRemotely() throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("OkEvtUidAB1", null);
    attachRelationship(te, "FailRelUidA");
    stubTrackedEntityService(te);

    ImportReport reportWithFailedRelationship =
        reportWith(
            successEntity(TrackerType.TRACKED_ENTITY, "TeUid1234AB"),
            failedEntity(
                TrackerType.RELATIONSHIP,
                "FailRelUidA",
                "E4009",
                "Relationship validation failed"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(reportWithFailedRelationship));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(trackedEntityService, times(0)).updateTrackedEntitiesSyncTimestamp(any(), any());
  }

  @Test
  void shouldMarkOnlyTrackedEntityWithAllChildrenSucceededWhenSyncingMultipleTrackedEntities()
      throws Exception {
    TrackedEntity succeedingTe = buildTeWithEnrollmentContaining("OkEvtUidAB1", null);
    succeedingTe.setUid("SucceedTeAB");
    succeedingTe.getEnrollments().forEach(e -> e.setUid("SucceedEnAB"));

    TrackedEntity failingTe = buildTeWithEnrollmentContaining("FailEvtUidA", null);
    failingTe.setUid("FailingTeAB");

    when(trackedEntityService.getTrackedEntityCount(any())).thenReturn(2L);
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(List.of(succeedingTe, failingTe), 1, 100, 2L, null, null));

    ImportReport report =
        reportWith(
            successEntity(TrackerType.TRACKED_ENTITY, "SucceedTeAB"),
            successEntity(TrackerType.TRACKED_ENTITY, "FailingTeAB"),
            failedEntity(TrackerType.EVENT, "FailEvtUidA", "E1032", "Event validation failed"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(report));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    ArgumentCaptor<Set<UID>> uidsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(trackedEntityService).updateTrackedEntitiesSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(Set.of(UID.of("SucceedTeAB")), uidsCaptor.getValue());
  }

  @Test
  void shouldExcludeAlreadyAttemptedTrackedEntitiesFromSubsequentPageFetches() throws Exception {
    TrackedEntity firstTe = buildTeWithEnrollmentContaining("FailEvtUidA", null);
    firstTe.setUid("FrstTeUidAB");
    firstTe.getEnrollments().forEach(e -> e.setUid("FrstEnUidAB"));

    TrackedEntity secondTe = buildTeWithEnrollmentContaining("OkEvtUidAB2", null);
    secondTe.setUid("ScndTeUidAB");
    secondTe.getEnrollments().forEach(e -> e.setUid("ScndEnUidAB"));

    when(trackedEntityService.getTrackedEntityCount(any())).thenReturn(2L);
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(List.of(firstTe), 1, 1, 2L, null, null))
        .thenReturn(new Page<>(List.of(secondTe), 1, 1, 2L, null, null));
    when(programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue())
        .thenReturn(Map.of());

    ImportReport firstPageReport =
        reportWith(
            successEntity(TrackerType.TRACKED_ENTITY, "FrstTeUidAB"),
            failedEntity(TrackerType.EVENT, "FailEvtUidA", "E1032", "Event validation failed"));
    ImportReport secondPageReport =
        reportWith(successEntity(TrackerType.TRACKED_ENTITY, "ScndTeUidAB"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(firstPageReport));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(secondPageReport));

    ArgumentCaptor<org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams>
        paramsCaptor =
            ArgumentCaptor.forClass(
                org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams.class);

    service.synchronizeTrackerData(1, JobProgress.noop());

    mockServer.verify();
    verify(trackedEntityService, times(2)).findTrackedEntities(paramsCaptor.capture(), any());
    List<org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams> capturedParams =
        paramsCaptor.getAllValues();
    // firstTe failed remotely (its event errored out), so the second "page 1" fetch must exclude
    // it, or it would be refetched forever instead of ever reaching secondTe.
    assertEquals(Set.of(), capturedParams.get(0).getExcludedTrackedEntities());
    assertEquals(Set.of(UID.of("FrstTeUidAB")), capturedParams.get(1).getExcludedTrackedEntities());
  }

  @Test
  void shouldMarkDeletedTrackedEntitySyncedOnlyWhenDeleteSucceededRemotely() throws Exception {
    TrackedEntity deletedTe = new TrackedEntity();
    deletedTe.setUid("DeletedTeAB");
    deletedTe.setDeleted(true);

    when(trackedEntityService.getTrackedEntityCount(any())).thenReturn(1L);
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(List.of(deletedTe), 1, 100, 1L, null, null));

    ImportReport deleteFailedReport =
        reportWith(
            failedEntity(
                TrackerType.TRACKED_ENTITY, "DeletedTeAB", "E1063", "Entity does not exist"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(deleteFailedReport));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(trackedEntityService, times(0)).updateTrackedEntitiesSyncTimestamp(any(), any());
  }

  @Test
  void shouldNotMarkTrackedEntitySyncedWhenDeletedChildEventFailedForRealReason() throws Exception {
    stubTrackedEntityService(buildTeWithEnrollmentContaining("ActEvtUidAB", "DelEvtUidAB"));
    ImportReport deleteReport =
        reportWith(failedEntity(TrackerType.EVENT, "DelEvtUidAB", "E0000", "validation failed"));
    expectDeleteBeforeCreateAndUpdate(
        deleteReport, reportWith(successEntity(TrackerType.TRACKED_ENTITY, "TeUid1234AB")));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(trackedEntityService, times(0)).updateTrackedEntitiesSyncTimestamp(any(), any());
  }

  @Test
  void shouldMarkTrackedEntitySyncedWhenDeletedChildEventAlreadyDeletedRemotely() throws Exception {
    stubTrackedEntityService(buildTeWithEnrollmentContaining("ActEvtUidAB", "DelEvtUidAB"));
    ImportReport deleteReport =
        reportWith(failedEntity(TrackerType.EVENT, "DelEvtUidAB", "E1082", "already deleted"));
    expectDeleteBeforeCreateAndUpdate(
        deleteReport, reportWith(successEntity(TrackerType.TRACKED_ENTITY, "TeUid1234AB")));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    ArgumentCaptor<Set<UID>> uidsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(trackedEntityService).updateTrackedEntitiesSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(Set.of(UID.of("TeUid1234AB")), uidsCaptor.getValue());
  }

  @Test
  void shouldNotMarkTrackedEntitySyncedWhenDeletedRelationshipFailedForRealReason()
      throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("ActEvtUidAB", null);
    attachDeletedRelationshipToEnrollment(te.getEnrollments().iterator().next(), "DelRelUidAB");
    stubTrackedEntityService(te);
    ImportReport deleteReport =
        reportWith(
            failedEntity(TrackerType.RELATIONSHIP, "DelRelUidAB", "E0000", "validation failed"));
    expectDeleteBeforeCreateAndUpdate(
        deleteReport, reportWith(successEntity(TrackerType.TRACKED_ENTITY, "TeUid1234AB")));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(trackedEntityService, times(0)).updateTrackedEntitiesSyncTimestamp(any(), any());
  }

  @Test
  void shouldMarkTrackedEntitySyncedWhenDeletedRelationshipAlreadyDeletedRemotely()
      throws Exception {
    TrackedEntity te = buildTeWithEnrollmentContaining("ActEvtUidAB", null);
    attachDeletedRelationshipToEnrollment(te.getEnrollments().iterator().next(), "DelRelUidAB");
    stubTrackedEntityService(te);
    ImportReport deleteReport =
        reportWith(
            failedEntity(TrackerType.RELATIONSHIP, "DelRelUidAB", "E4017", "already deleted"));
    expectDeleteBeforeCreateAndUpdate(
        deleteReport, reportWith(successEntity(TrackerType.TRACKED_ENTITY, "TeUid1234AB")));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    ArgumentCaptor<Set<UID>> uidsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(trackedEntityService).updateTrackedEntitiesSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(Set.of(UID.of("TeUid1234AB")), uidsCaptor.getValue());
  }

  @Test
  void shouldMarkDeletedTrackedEntitySyncedWhenItsOwnDeleteAlreadyHappenedRemotely()
      throws Exception {
    TrackedEntity deletedTe = new TrackedEntity();
    deletedTe.setUid("DeletedTeAB");
    deletedTe.setDeleted(true);

    when(trackedEntityService.getTrackedEntityCount(any())).thenReturn(1L);
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(List.of(deletedTe), 1, 100, 1L, null, null));

    ImportReport alreadyDeletedReport =
        reportWith(
            failedEntity(TrackerType.TRACKED_ENTITY, "DeletedTeAB", "E1114", "already deleted"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(alreadyDeletedReport));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    ArgumentCaptor<Set<UID>> uidsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(trackedEntityService).updateTrackedEntitiesSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(Set.of(UID.of("DeletedTeAB")), uidsCaptor.getValue());
  }

  private void expectDeleteBeforeCreateAndUpdate(
      ImportReport deleteReport, ImportReport createReport) {
    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(deleteReport));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(createReport));
  }

  private List<Map<?, ?>> synchronizeAndCapturePayloads() throws Exception {
    ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
    service.synchronizeTrackerData(100, JobProgress.noop());
    verify(renderService, times(2)).toJson(any(), bodyCaptor.capture());
    return bodyCaptor.getAllValues().stream().<Map<?, ?>>map(b -> (Map<?, ?>) b).toList();
  }

  private void mockSettings() {
    SystemSettings settings = mock(SystemSettings.class);
    when(systemSettingsService.getCurrentSettings()).thenReturn(settings);
    when(settings.getRemoteInstanceUrl()).thenReturn(REMOTE_URL);
    when(settings.getRemoteInstanceUsername()).thenReturn("admin");
    when(settings.getRemoteInstancePassword()).thenReturn("district");
    when(settings.getSyncMaxRemoteServerAvailabilityCheckAttempts()).thenReturn(1);
    when(settings.getSyncDelayBetweenRemoteServerAvailabilityCheckAttempts()).thenReturn(500);
    when(settings.getSyncSkipSyncForDataChangedBefore()).thenReturn(new Date(0));
    when(settings.getSyncMaxAttempts()).thenReturn(1);
  }

  private void stubTrackedEntityService(TrackedEntity te) throws Exception {
    doReturn(1L).when(trackedEntityService).getTrackedEntityCount(any());
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(List.of(te), 1, 100, 1L, null, null));
    when(programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue())
        .thenReturn(Map.of());
  }

  private static String toJson(ImportReport report) {
    try {
      return OBJECT_MAPPER.writeValueAsString(report);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Mirrors TrackerImportController#importSync: a synchronous /api/tracker import responds with
  // 409 CONFLICT whenever the report status is ERROR, and 200 otherwise. MockRestServiceServer
  // doesn't do this for us, so tests must reproduce it or they don't exercise the real
  // HttpClientErrorException path the service has to handle.
  private static ResponseCreator respondWith(ImportReport report) {
    String json = toJson(report);
    return report.getStatus() == Status.ERROR
        ? withStatus(HttpStatus.CONFLICT).body(json).contentType(MediaType.APPLICATION_JSON)
        : withSuccess(json, MediaType.APPLICATION_JSON);
  }

  private static ImportReport successReport() {
    return ImportReport.builder().status(Status.OK).build();
  }

  private static ImportReport reportWith(Entity... entities) {
    Map<TrackerType, TrackerTypeReport> typeReportMap = new java.util.HashMap<>();
    boolean hasErrors = false;
    for (Entity entity : entities) {
      TrackerTypeReport typeReport =
          typeReportMap.computeIfAbsent(entity.getTrackerType(), TrackerTypeReport::new);
      typeReport.addEntity(entity);
      hasErrors = hasErrors || !entity.getErrorReports().isEmpty();
    }
    return ImportReport.builder()
        .status(hasErrors ? Status.ERROR : Status.OK)
        .validationReport(ValidationReport.emptyReport())
        .persistenceReport(new PersistenceReport(typeReportMap))
        .stats(new Stats())
        .build();
  }

  private static Entity successEntity(TrackerType type, String uid) {
    return new Entity(type, UID.of(uid), List.of());
  }

  private static Entity failedEntity(
      TrackerType type, String uid, String errorCode, String message) {
    return new Entity(
        type,
        UID.of(uid),
        List.of(
            Error.builder()
                .message(message)
                .errorCode(errorCode)
                .trackerType(type.name())
                .uid(UID.of(uid))
                .args(List.of())
                .build()));
  }

  private TrackedEntity buildTeWithEnrollmentContaining(
      String activeEventUid, String deletedEventUid) {
    TrackedEntityType tet = new TrackedEntityType();
    tet.setUid("TrackedEtAB");

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("OrgUnitUiAB");

    Program program = new Program();
    program.setUid("ProgramUiAB");

    ProgramStage programStage = new ProgramStage();
    programStage.setUid("ProgStageAB");

    TrackedEntity te = new TrackedEntity();
    te.setUid("TeUid1234AB");
    te.setTrackedEntityType(tet);
    te.setOrganisationUnit(orgUnit);

    Enrollment enrollment = new Enrollment();
    enrollment.setUid("EnrollUidAB");
    enrollment.setDeleted(false);
    enrollment.setTrackedEntity(te);
    enrollment.setProgram(program);
    enrollment.setOrganisationUnit(orgUnit);

    Event activeEvent = new Event();
    activeEvent.setUid(activeEventUid);
    activeEvent.setDeleted(false);
    activeEvent.setEnrollment(enrollment);
    activeEvent.setProgramStage(programStage);
    activeEvent.setOrganisationUnit(orgUnit);

    Set<Event> events = new java.util.HashSet<>();
    events.add(activeEvent);

    if (deletedEventUid != null) {
      Event deletedEvent = new Event();
      deletedEvent.setUid(deletedEventUid);
      deletedEvent.setDeleted(true);
      deletedEvent.setEnrollment(enrollment);
      deletedEvent.setProgramStage(programStage);
      deletedEvent.setOrganisationUnit(orgUnit);
      events.add(deletedEvent);
    }

    enrollment.setEvents(events);
    te.setEnrollments(Set.of(enrollment));
    return te;
  }

  private void attachRelationship(TrackedEntity te, String relationshipUid) {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid("RelTypeUiAB");
    relationshipType.setName("Relationship type");
    relationshipType.setBidirectional(false);

    TrackedEntity other = new TrackedEntity();
    other.setUid("OtherTeUiAB");

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setTrackedEntity(te);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setTrackedEntity(other);

    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(fromItem);
    relationship.setTo(toItem);
    relationship.setAutoFields();
    relationship.setUid(relationshipUid);

    fromItem.setRelationship(relationship);
    toItem.setRelationship(relationship);

    te.getRelationshipItems().add(fromItem);
  }

  private void attachDeletedRelationshipToEnrollment(
      Enrollment enrollment, String relationshipUid) {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid("RelTypeUiCD");
    relationshipType.setName("Enrollment relationship type");
    relationshipType.setBidirectional(false);

    Enrollment other = new Enrollment();
    other.setUid("OthrEnrlUAB");

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setEnrollment(enrollment);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setEnrollment(other);

    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(fromItem);
    relationship.setTo(toItem);
    relationship.setAutoFields();
    relationship.setUid(relationshipUid);
    relationship.setDeleted(true);

    fromItem.setRelationship(relationship);
    toItem.setRelationship(relationship);

    enrollment.getRelationshipItems().add(fromItem);
  }

  private TrackedEntity buildTeWithDeletedAndActiveEnrollment() {
    TrackedEntityType tet = new TrackedEntityType();
    tet.setUid("TrackedEtAB");

    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("OrgUnitUiAB");

    Program program = new Program();
    program.setUid("ProgramUiAB");

    ProgramStage programStage = new ProgramStage();
    programStage.setUid("ProgStageAB");

    TrackedEntity te = new TrackedEntity();
    te.setUid("TeUid1234AB");
    te.setTrackedEntityType(tet);
    te.setOrganisationUnit(orgUnit);

    Enrollment activeEnrollment = new Enrollment();
    activeEnrollment.setUid("ActEnrlUiAB");
    activeEnrollment.setDeleted(false);
    activeEnrollment.setTrackedEntity(te);
    activeEnrollment.setProgram(program);
    activeEnrollment.setOrganisationUnit(orgUnit);
    activeEnrollment.setEnrollmentDate(new Date(1000));

    Event activeEvent = new Event();
    activeEvent.setUid("ActEvtUidAB");
    activeEvent.setDeleted(false);
    activeEvent.setEnrollment(activeEnrollment);
    activeEvent.setProgramStage(programStage);
    activeEvent.setOrganisationUnit(orgUnit);
    activeEnrollment.setEvents(Set.of(activeEvent));

    Enrollment deletedEnrollment = new Enrollment();
    deletedEnrollment.setUid("DelEnrlUiAB");
    deletedEnrollment.setDeleted(true);
    deletedEnrollment.setTrackedEntity(te);
    deletedEnrollment.setProgram(program);
    deletedEnrollment.setOrganisationUnit(orgUnit);
    deletedEnrollment.setEnrollmentDate(new Date(2000));

    Event eventOfDeletedEnrollment = new Event();
    eventOfDeletedEnrollment.setUid("EvtDelEnrAB");
    eventOfDeletedEnrollment.setDeleted(false);
    eventOfDeletedEnrollment.setEnrollment(deletedEnrollment);
    eventOfDeletedEnrollment.setProgramStage(programStage);
    eventOfDeletedEnrollment.setOrganisationUnit(orgUnit);
    deletedEnrollment.setEvents(Set.of(eventOfDeletedEnrollment));

    te.setEnrollments(Set.of(activeEnrollment, deletedEnrollment));
    return te;
  }
}
