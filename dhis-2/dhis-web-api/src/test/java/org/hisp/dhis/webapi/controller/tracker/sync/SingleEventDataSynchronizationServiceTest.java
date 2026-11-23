/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Stats;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.tracker.model.Relationship;
import org.hisp.dhis.tracker.model.RelationshipItem;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SingleEventDataSynchronizationServiceTest {

  @Mock private SingleEventService singleEventService;
  @Mock private ProgramStageDataElementService programStageDataElementService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private RestTemplate restTemplate;
  @Mock private RenderService renderService;
  @Mock private SystemSettings settings;

  private SingleEventDataSynchronizationService service;

  @BeforeEach
  void setUp() {
    service =
        new SingleEventDataSynchronizationService(
            singleEventService,
            systemSettingsService,
            programStageDataElementService,
            restTemplate,
            renderService);
  }

  @Test
  void shouldStripDeletedRelationshipsFromActiveEvent() {
    org.hisp.dhis.webapi.controller.tracker.view.Relationship deletedRelationship =
        org.hisp.dhis.webapi.controller.tracker.view.Relationship.builder()
            .relationship(UID.of("Rel00000001"))
            .deleted(true)
            .build();
    org.hisp.dhis.webapi.controller.tracker.view.Relationship activeRelationship =
        org.hisp.dhis.webapi.controller.tracker.view.Relationship.builder()
            .relationship(UID.of("Rel00000002"))
            .deleted(false)
            .build();

    Event event =
        Event.builder()
            .event(UID.of("Event000001"))
            .relationships(List.of(deletedRelationship, activeRelationship))
            .build();

    BaseDataSynchronizationWithPaging.NestedDeletion<Event> result =
        service.splitDeletedChildren(List.of(event));

    assertEquals(1, result.deletedCountByType().get(TrackerType.RELATIONSHIP));
    assertEquals(List.of(activeRelationship), event.getRelationships());
    assertEquals(
        Set.of(deletedRelationship.getRelationship()),
        result.deletedChildUidsByParent().get(event.getEvent()));
  }

  @Test
  void shouldReportFailedChildWhenNestedRelationshipFailed() {
    org.hisp.dhis.webapi.controller.tracker.view.Relationship relationship =
        org.hisp.dhis.webapi.controller.tracker.view.Relationship.builder()
            .relationship(UID.of("Rel00000001"))
            .build();
    Event event =
        Event.builder().event(UID.of("Event000001")).relationships(List.of(relationship)).build();

    boolean hasFailedChild =
        service.hasFailedChild(
            event, Map.of(TrackerType.RELATIONSHIP, Set.of(UID.of("Rel00000001"))));

    assertTrue(hasFailedChild);
  }

  @Test
  void shouldNotReportFailedChildWhenNoRelationshipFailed() {
    org.hisp.dhis.webapi.controller.tracker.view.Relationship relationship =
        org.hisp.dhis.webapi.controller.tracker.view.Relationship.builder()
            .relationship(UID.of("Rel00000001"))
            .build();
    Event event =
        Event.builder().event(UID.of("Event000001")).relationships(List.of(relationship)).build();

    boolean hasFailedChild =
        service.hasFailedChild(
            event, Map.of(TrackerType.RELATIONSHIP, Set.of(UID.of("SomeOtherOn"))));

    assertFalse(hasFailedChild);
  }

  @Test
  void shouldStampSyncTimestampWhenActiveEventSyncSucceeds() throws Exception {
    UID eventUid = UID.of("Event000001");
    stubHappyPathSyncInfrastructure(List.of(activeSingleEvent(eventUid)));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(reportWith(successEntity(TrackerType.EVENT, eventUid)));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService)
        .updateEventsSyncTimestamp(eq(List.of(eventUid.getValue())), any(Date.class));
  }

  @Test
  void shouldNotStampSyncTimestampWhenActiveEventSyncFails() throws Exception {
    UID eventUid = UID.of("Event000001");
    stubHappyPathSyncInfrastructure(List.of(activeSingleEvent(eventUid)));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(reportWith(failedEntity(TrackerType.EVENT, eventUid, "E1000", "failed")));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService, never()).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldNotStampSyncTimestampWhenChildRelationshipFailedRemotely() throws Exception {
    UID eventUid = UID.of("Event000001");
    UID relationshipUid = UID.of("Rel00000001");
    SingleEvent event = activeSingleEvent(eventUid);
    attachRelationship(event, relationshipUid, false);
    stubHappyPathSyncInfrastructure(List.of(event));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(
            reportWith(
                successEntity(TrackerType.EVENT, eventUid),
                failedEntity(
                    TrackerType.RELATIONSHIP, relationshipUid, "E4009", "validation failed")));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService, never()).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldOnlyStampSyncTimestampForEventsThatFullySucceeded() throws Exception {
    UID succeedingEventUid = UID.of("SucceedEvAB");
    UID failingEventUid = UID.of("FailingEvAB");
    UID relationshipUid = UID.of("Rel00000001");
    SingleEvent failingEvent = activeSingleEvent(failingEventUid);
    attachRelationship(failingEvent, relationshipUid, false);
    stubHappyPathSyncInfrastructure(List.of(activeSingleEvent(succeedingEventUid), failingEvent));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(
            reportWith(
                successEntity(TrackerType.EVENT, succeedingEventUid),
                successEntity(TrackerType.EVENT, failingEventUid),
                failedEntity(
                    TrackerType.RELATIONSHIP, relationshipUid, "E4009", "validation failed")));

    service.synchronizeData(100, JobProgress.noop());

    ArgumentCaptor<List<String>> uidsCaptor = ArgumentCaptor.forClass(List.class);
    verify(singleEventService).updateEventsSyncTimestamp(uidsCaptor.capture(), any(Date.class));
    assertEquals(List.of(succeedingEventUid.getValue()), uidsCaptor.getValue());
  }

  @Test
  void shouldStampSyncTimestampWhenDeletedEventIsSyncedSuccessfully() throws Exception {
    UID eventUid = UID.of("DeletedEvAB");
    SingleEvent deletedEvent = activeSingleEvent(eventUid);
    deletedEvent.setDeleted(true);
    stubHappyPathSyncInfrastructure(List.of(deletedEvent));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(reportWith(successEntity(TrackerType.EVENT, eventUid)));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService)
        .updateEventsSyncTimestamp(eq(List.of(eventUid.getValue())), any(Date.class));
  }

  @Test
  void shouldNotStampSyncTimestampWhenDeletedEventSyncFails() throws Exception {
    UID eventUid = UID.of("DeletedEvAB");
    SingleEvent deletedEvent = activeSingleEvent(eventUid);
    deletedEvent.setDeleted(true);
    stubHappyPathSyncInfrastructure(List.of(deletedEvent));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(
            reportWith(failedEntity(TrackerType.EVENT, eventUid, "E1032", "does not exist")));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService, never()).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldNotStampSyncTimestampWhenDeletedRelationshipFailedForRealReason() throws Exception {
    UID eventUid = UID.of("Event000001");
    UID relationshipUid = UID.of("Rel00000001");
    SingleEvent event = activeSingleEvent(eventUid);
    attachRelationship(event, relationshipUid, true);
    stubHappyPathSyncInfrastructure(List.of(event));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(
            reportWith(
                failedEntity(TrackerType.RELATIONSHIP, relationshipUid, "E0000", "failed"),
                successEntity(TrackerType.EVENT, eventUid)));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService, never()).updateEventsSyncTimestamp(any(), any());
  }

  @Test
  void shouldStampSyncTimestampWhenDeletedRelationshipAlreadyDeletedRemotely() throws Exception {
    UID eventUid = UID.of("Event000001");
    UID relationshipUid = UID.of("Rel00000001");
    SingleEvent event = activeSingleEvent(eventUid);
    attachRelationship(event, relationshipUid, true);
    stubHappyPathSyncInfrastructure(List.of(event));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(
            reportWith(
                failedEntity(TrackerType.RELATIONSHIP, relationshipUid, "E4017", "already deleted"),
                successEntity(TrackerType.EVENT, eventUid)));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService)
        .updateEventsSyncTimestamp(eq(List.of(eventUid.getValue())), any(Date.class));
  }

  @Test
  void shouldStampSyncTimestampWhenDeletedEventIsAlreadyDeletedOnRemote() throws Exception {
    UID eventUid = UID.of("DeletedEvAB");
    SingleEvent deletedEvent = activeSingleEvent(eventUid);
    deletedEvent.setDeleted(true);
    stubHappyPathSyncInfrastructure(List.of(deletedEvent));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(
            reportWith(failedEntity(TrackerType.EVENT, eventUid, "E1082", "already deleted")));

    service.synchronizeData(100, JobProgress.noop());

    verify(singleEventService)
        .updateEventsSyncTimestamp(eq(List.of(eventUid.getValue())), any(Date.class));
  }

  /** Stubs the availability check, {@code createContext}, and page fetch for a happy-path run. */
  private void stubHappyPathSyncInfrastructure(List<SingleEvent> page) throws Exception {
    when(systemSettingsService.getCurrentSettings()).thenReturn(settings);
    when(settings.getRemoteInstanceUrl()).thenReturn("http://remote");
    when(settings.getRemoteInstanceUsername()).thenReturn("admin");
    when(settings.getRemoteInstancePassword()).thenReturn("district");
    when(settings.getSyncMaxAttempts()).thenReturn(1);
    when(settings.getSyncMaxRemoteServerAvailabilityCheckAttempts()).thenReturn(2);
    when(settings.getSyncSkipSyncForDataChangedBefore()).thenReturn(new Date(0));
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    when(programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue())
        .thenReturn(Map.of());
    when(singleEventService.countEvents(any())).thenReturn((long) page.size());
    when(singleEventService.findEvents(any(), any()))
        .thenReturn(new Page<>(page, PageParams.of(1, 100, false)));
  }

  private SingleEvent activeSingleEvent(UID uid) {
    Program program = new Program();
    program.setUid("ProgramUiAB");
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("ProgStageAB");
    programStage.setProgram(program);
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("OrgUnitUiAB");

    SingleEvent event = new SingleEvent();
    event.setUid(uid.getValue());
    event.setProgramStage(programStage);
    event.setOrganisationUnit(orgUnit);
    event.setOccurredDate(new Date());
    event.setCreated(new Date());
    event.setLastUpdated(new Date());
    return event;
  }

  private void attachRelationship(SingleEvent event, UID relationshipUid, boolean deleted) {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setUid("RelTypeUiAB");
    relationshipType.setName("Relationship type");

    Relationship relationship = new Relationship();
    relationship.setUid(relationshipUid.getValue());
    relationship.setDeleted(deleted);
    relationship.setRelationshipType(relationshipType);
    relationship.setCreated(new Date());
    relationship.setLastUpdated(new Date());

    RelationshipItem item = new RelationshipItem();
    item.setRelationship(relationship);
    item.setSingleEvent(event);
    relationship.setFrom(item);
    relationship.setTo(item);

    event.getRelationshipItems().add(item);
  }

  /**
   * An {@link ImportReport} combining every given {@link Entity}, grouped by {@link
   * Entity#getTrackerType()}. {@code status} is {@code ERROR} if any entity carries an error.
   */
  private ImportReport reportWith(Entity... entities) {
    Map<TrackerType, TrackerTypeReport> typeReportMap = new EnumMap<>(TrackerType.class);
    boolean hasErrors = false;
    for (Entity entity : entities) {
      TrackerTypeReport typeReport =
          typeReportMap.computeIfAbsent(entity.getTrackerType(), TrackerTypeReport::new);
      typeReport.addEntity(entity);
      hasErrors = hasErrors || !entity.getErrorReports().isEmpty();
    }
    TrackerTypeReport empty = new TrackerTypeReport(TrackerType.EVENT);
    PersistenceReport persistenceReport =
        new PersistenceReport(
            typeReportMap.getOrDefault(
                TrackerType.TRACKED_ENTITY, new TrackerTypeReport(TrackerType.TRACKED_ENTITY)),
            typeReportMap.getOrDefault(
                TrackerType.ENROLLMENT, new TrackerTypeReport(TrackerType.ENROLLMENT)),
            empty,
            typeReportMap.getOrDefault(TrackerType.EVENT, new TrackerTypeReport(TrackerType.EVENT)),
            typeReportMap.getOrDefault(
                TrackerType.RELATIONSHIP, new TrackerTypeReport(TrackerType.RELATIONSHIP)));
    return ImportReport.builder()
        .status(hasErrors ? Status.ERROR : Status.OK)
        .persistenceReport(persistenceReport)
        .stats(new Stats())
        .build();
  }

  private Entity successEntity(TrackerType type, UID uid) {
    return new Entity(type, uid, List.of());
  }

  private Entity failedEntity(TrackerType type, UID uid, String errorCode, String message) {
    return new Entity(
        type,
        uid,
        List.of(
            Error.builder()
                .message(message)
                .errorCode(errorCode)
                .trackerType(type.name())
                .uid(uid)
                .args(List.of())
                .build()));
  }
}
