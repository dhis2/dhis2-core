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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.hisp.dhis.common.UID;
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
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.event.EventService;
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
class SingleEventDataSynchronizationServiceTest {

  private static final String REMOTE_URL = "http://remote";
  private static final String PING_RESPONSE = "true";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .configure(
              com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
              false);

  @Mock private EventService eventService;
  @Mock private ProgramStageDataElementService programStageDataElementService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private RenderService renderService;

  private MockRestServiceServer mockServer;
  private SingleEventDataSynchronizationService service;

  @BeforeEach
  void setUp() throws Exception {
    RestTemplate restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    service =
        new SingleEventDataSynchronizationService(
            eventService,
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
  void shouldOnlySendCreateAndUpdateWhenNoDeletedEvents() throws Exception {
    stubEventService(buildActiveEvent("ActEvtUidAB"));

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(respondWith(successReport()));

    service.synchronizeTrackerData(100, JobProgress.noop());

    verify(eventService).countEvents(any());
    verify(eventService).findEvents(any(), any());
    mockServer.verify();
  }

  @Test
  void shouldSendDeleteBeforeCreateAndUpdateWhenPageHasDeletedEvent() throws Exception {
    Event active = buildActiveEvent("ActEvtUidAB");
    Event deleted = buildActiveEvent("DelEvtUidAB");
    deleted.setDeleted(true);
    stubEventService(active, deleted);
    expectDeleteBeforeCreateAndUpdate(successReport(), successReport());

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
  }

  @Test
  void shouldStripDeletedRelationshipFromCreateAndUpdatePayloadAndSendItInDeleteRequest()
      throws Exception {
    Event event = buildActiveEvent("ActEvtUidAB");
    attachDeletedRelationship(event, "DelRelUidAB");
    stubEventService(event);
    expectDeleteBeforeCreateAndUpdate(successReport(), successReport());

    List<Map<?, ?>> payloads = synchronizeAndCapturePayloads();

    Map<?, ?> deleteBody = payloads.get(0);
    assertEquals(Set.of("events", "relationships"), deleteBody.keySet());
    assertEquals(0, ((List<?>) deleteBody.get("events")).size());
    List<?> deletedRelationships = (List<?>) deleteBody.get("relationships");
    assertEquals(1, deletedRelationships.size());
    assertEquals(
        "DelRelUidAB",
        ((org.hisp.dhis.webapi.controller.tracker.view.Relationship) deletedRelationships.get(0))
            .getRelationship()
            .getValue());

    List<?> eventList = (List<?>) payloads.get(1).get("events");
    org.hisp.dhis.webapi.controller.tracker.view.Event eventDto =
        (org.hisp.dhis.webapi.controller.tracker.view.Event) eventList.get(0);
    assertEquals(0, eventDto.getRelationships().size());
  }

  @Test
  void shouldNotMarkEventSyncedWhenChildRelationshipFailedRemotely() throws Exception {
    Event event = buildActiveEvent("OkEvtUidAB1");
    attachRelationship(event, "FailRelUidA");
    stubEventService(event);

    ImportReport reportWithFailedRelationship =
        reportWith(
            successEntity(TrackerType.EVENT, "OkEvtUidAB1"),
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
    verify(eventService, times(0)).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldMarkOnlyEventWithAllRelationshipsSucceededWhenSyncingMultipleEvents()
      throws Exception {
    Event succeedingEvent = buildActiveEvent("SucceedEvAB");
    Event failingEvent = buildActiveEvent("FailingEvAB");
    attachRelationship(failingEvent, "FailRelUidA");

    when(eventService.countEvents(any())).thenReturn(2L);
    when(eventService.findEvents(any(), any()))
        .thenReturn(new Page<>(List.of(succeedingEvent, failingEvent), 1, 100, 2L, null, null));
    when(programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue())
        .thenReturn(Map.of());

    ImportReport report =
        reportWith(
            successEntity(TrackerType.EVENT, "SucceedEvAB"),
            successEntity(TrackerType.EVENT, "FailingEvAB"),
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
        .andRespond(respondWith(report));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    ArgumentCaptor<List<String>> uidsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventService).updateEventsSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(List.of("SucceedEvAB"), uidsCaptor.getValue());
  }

  @Test
  void shouldMarkDeletedEventSyncedOnlyWhenDeleteSucceededRemotely() throws Exception {
    Event deletedEvent = buildActiveEvent("DeletedEvAB");
    deletedEvent.setDeleted(true);

    when(eventService.countEvents(any())).thenReturn(1L);
    when(eventService.findEvents(any(), any()))
        .thenReturn(new Page<>(List.of(deletedEvent), 1, 100, 1L, null, null));

    ImportReport deleteFailedReport =
        reportWith(failedEntity(TrackerType.EVENT, "DeletedEvAB", "E1032", "does not exist"));

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
    verify(eventService, times(0)).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldNotMarkEventSyncedWhenDeletedRelationshipFailedForRealReason() throws Exception {
    Event event = buildActiveEvent("ActEvtUidAB");
    attachDeletedRelationship(event, "DelRelUidAB");
    stubEventService(event);
    ImportReport deleteReport =
        reportWith(
            failedEntity(TrackerType.RELATIONSHIP, "DelRelUidAB", "E0000", "validation failed"));
    expectDeleteBeforeCreateAndUpdate(
        deleteReport, reportWith(successEntity(TrackerType.EVENT, "ActEvtUidAB")));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(eventService, times(0)).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldMarkEventSyncedWhenDeletedRelationshipAlreadyDeletedRemotely() throws Exception {
    Event event = buildActiveEvent("ActEvtUidAB");
    attachDeletedRelationship(event, "DelRelUidAB");
    stubEventService(event);
    ImportReport deleteReport =
        reportWith(
            failedEntity(TrackerType.RELATIONSHIP, "DelRelUidAB", "E4017", "already deleted"));
    expectDeleteBeforeCreateAndUpdate(
        deleteReport, reportWith(successEntity(TrackerType.EVENT, "ActEvtUidAB")));

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    ArgumentCaptor<List<String>> uidsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventService).updateEventsSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(List.of("ActEvtUidAB"), uidsCaptor.getValue());
  }

  @Test
  void shouldMarkDeletedEventSyncedWhenItsOwnDeleteAlreadyHappenedRemotely() throws Exception {
    Event deletedEvent = buildActiveEvent("DeletedEvAB");
    deletedEvent.setDeleted(true);

    when(eventService.countEvents(any())).thenReturn(1L);
    when(eventService.findEvents(any(), any()))
        .thenReturn(new Page<>(List.of(deletedEvent), 1, 100, 1L, null, null));

    ImportReport alreadyDeletedReport =
        reportWith(failedEntity(TrackerType.EVENT, "DeletedEvAB", "E1082", "already deleted"));

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
    ArgumentCaptor<List<String>> uidsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventService).updateEventsSyncTimestamp(uidsCaptor.capture(), any());
    assertEquals(List.of("DeletedEvAB"), uidsCaptor.getValue());
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

  private void stubEventService(Event... events) throws Exception {
    doReturn((long) events.length).when(eventService).countEvents(any());
    when(eventService.findEvents(any(), any()))
        .thenReturn(new Page<>(List.of(events), 1, 100, (long) events.length, null, null));
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
    Map<TrackerType, TrackerTypeReport> typeReportMap = new java.util.EnumMap<>(TrackerType.class);
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

  private Event buildActiveEvent(String eventUid) {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("OrgUnitUiAB");

    Program program = new Program();
    program.setUid("ProgramUiAB");

    ProgramStage programStage = new ProgramStage();
    programStage.setUid("ProgStageAB");

    Enrollment enrollment = new Enrollment();
    enrollment.setUid("EnrollUidAB");
    enrollment.setProgram(program);
    enrollment.setOrganisationUnit(orgUnit);

    Event event = new Event();
    event.setUid(eventUid);
    event.setDeleted(false);
    event.setEnrollment(enrollment);
    event.setProgramStage(programStage);
    event.setOrganisationUnit(orgUnit);
    return event;
  }

  private void attachRelationship(Event event, String relationshipUid) {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid("RelTypeUiAB");
    relationshipType.setName("Relationship type");
    relationshipType.setBidirectional(false);

    Event other = new Event();
    other.setUid("OthrEvtUiAB");

    RelationshipItem fromItem = new RelationshipItem();
    fromItem.setEvent(event);

    RelationshipItem toItem = new RelationshipItem();
    toItem.setEvent(other);

    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(fromItem);
    relationship.setTo(toItem);
    relationship.setAutoFields();
    relationship.setUid(relationshipUid);

    fromItem.setRelationship(relationship);
    toItem.setRelationship(relationship);

    event.getRelationshipItems().add(fromItem);
  }

  private void attachDeletedRelationship(Event event, String relationshipUid) {
    attachRelationship(event, relationshipUid);
    event.getRelationshipItems().stream()
        .map(RelationshipItem::getRelationship)
        .filter(r -> relationshipUid.equals(r.getUid()))
        .forEach(r -> r.setDeleted(true));
  }
}
