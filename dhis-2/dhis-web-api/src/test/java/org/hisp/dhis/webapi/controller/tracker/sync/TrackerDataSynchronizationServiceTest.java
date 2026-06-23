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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class TrackerDataSynchronizationServiceTest {

  private static final String REMOTE_URL = "http://remote";
  private static final String IMPORT_SUCCESS_JSON =
      "{\"status\":\"SUCCESS\",\"importCount\":{\"imported\":0,\"updated\":0,\"ignored\":0,\"deleted\":0}}";
  private static final String PING_RESPONSE = "true";

  @Mock private TrackedEntityService trackedEntityService;
  @Mock private EventService eventService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private RenderService renderService;

  private MockRestServiceServer mockServer;
  private TrackerDataSynchronizationService service;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    service =
        new TrackerDataSynchronizationService(
            trackedEntityService, eventService, systemSettingsService, restTemplate, renderService);
  }

  @Test
  void shouldSendDeleteBeforeCreateAndUpdateWhenEnrollmentHasDeletedEvent() throws Exception {
    mockSettings();
    stubTrackedEntityService(buildTeWithEnrollmentContaining("ActEvtUidAB", "DelEvtUidAB", true));
    expectDeleteBeforeCreateAndUpdate();

    service.synchronizeTrackerData(100, JobProgress.noop());

    mockServer.verify();
    verify(eventService).updateEventsSyncTimestamp(eq(List.of("DelEvtUidAB")), any(Date.class));
  }

  @Test
  void shouldOnlySendCreateAndUpdateWhenNoDeletedChildren() throws Exception {
    mockSettings();
    TrackedEntity te = buildTeWithEnrollmentContaining("ActEvtUidAB", null, false);
    stubTrackedEntityService(te);

    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(IMPORT_SUCCESS_JSON, MediaType.APPLICATION_JSON));

    service.synchronizeTrackerData(100, JobProgress.noop());

    verify(trackedEntityService).getTrackedEntityCount(any());
    verify(trackedEntityService).findTrackedEntities(any(), any());
    mockServer.verify();
  }

  @Test
  void shouldStripDeletedEventFromCreateAndUpdatePayload() throws Exception {
    mockSettings();
    stubTrackedEntityService(buildTeWithEnrollmentContaining("ActEvtUidAB", "DelEvtUidAB", true));
    expectDeleteBeforeCreateAndUpdate();

    List<Map<?, ?>> payloads = synchronizeAndCapturePayloads();

    Map<?, ?> deleteBody = payloads.get(0);
    assertEquals(Set.of("enrollments", "events"), deleteBody.keySet());
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
    mockSettings();
    stubTrackedEntityService(buildTeWithDeletedAndActiveEnrollment());
    expectDeleteBeforeCreateAndUpdate();

    List<Map<?, ?>> payloads = synchronizeAndCapturePayloads();

    Map<?, ?> deleteBody = payloads.get(0);
    assertEquals(Set.of("enrollments", "events"), deleteBody.keySet());
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

  private void expectDeleteBeforeCreateAndUpdate() {
    mockServer
        .expect(requestTo(Matchers.containsString("/api/system/ping")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(PING_RESPONSE, MediaType.TEXT_PLAIN));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=DELETE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(IMPORT_SUCCESS_JSON, MediaType.APPLICATION_JSON));
    mockServer
        .expect(requestTo(Matchers.containsString("importStrategy=CREATE_AND_UPDATE")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(IMPORT_SUCCESS_JSON, MediaType.APPLICATION_JSON));
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
  }

  private TrackedEntity buildTeWithEnrollmentContaining(
      String activeEventUid, String deletedEventUid, boolean withDeletedEvent) {
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

    Set<Event> events = new HashSet<>();
    events.add(activeEvent);

    if (withDeletedEvent) {
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
