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

import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSynchronizationContext.forEntities;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SynchronizationStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.webapi.controller.tracker.view.Attribute;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class TrackerDataSynchronizationServiceTest {

  @Mock private TrackedEntityService trackedEntityService;
  @Mock private ProgramStageDataElementService programStageDataElementService;
  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private RestTemplate restTemplate;
  @Mock private RenderService renderService;
  @Mock private SystemSettings settings;

  private TrackerDataSynchronizationService service;

  @BeforeEach
  void setUp() {
    service =
        new TrackerDataSynchronizationService(
            trackedEntityService,
            programStageDataElementService,
            trackedEntityAttributeService,
            systemSettingsService,
            restTemplate,
            renderService);
  }

  @Test
  void shouldStripDeletedEnrollmentEventAndRelationshipFromActiveTrackedEntity() {
    Relationship deletedTeRelationship =
        Relationship.builder().relationship(UID.of("Rel00000001")).deleted(true).build();
    Relationship activeTeRelationship =
        Relationship.builder().relationship(UID.of("Rel00000002")).deleted(false).build();

    Event deletedEvent = Event.builder().event(UID.of("Event000001")).deleted(true).build();
    Event activeEvent =
        Event.builder()
            .event(UID.of("Event000002"))
            .deleted(false)
            .relationships(List.of())
            .build();

    Enrollment deletedEnrollment =
        Enrollment.builder().enrollment(UID.of("Enrol000001")).deleted(true).build();
    Enrollment activeEnrollment =
        Enrollment.builder()
            .enrollment(UID.of("Enrol000002"))
            .deleted(false)
            .events(List.of(deletedEvent, activeEvent))
            .relationships(List.of())
            .build();

    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te =
        org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity.builder()
            .trackedEntity(UID.of("TrackedEnt1"))
            .enrollments(List.of(deletedEnrollment, activeEnrollment))
            .relationships(List.of(deletedTeRelationship, activeTeRelationship))
            .build();

    BaseDataSynchronizationWithPaging.NestedDeletion<
            org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity>
        result = service.splitDeletedChildren(List.of(te));

    assertEquals(1, result.deletedCountByType().get(TrackerType.ENROLLMENT));
    assertEquals(1, result.deletedCountByType().get(TrackerType.EVENT));
    assertEquals(1, result.deletedCountByType().get(TrackerType.RELATIONSHIP));

    // the TE and its still-active enrollment/relationship/event survive, stripped of their
    // deleted children
    assertEquals(List.of(activeEnrollment), te.getEnrollments());
    assertEquals(List.of(activeTeRelationship), te.getRelationships());
    assertEquals(List.of(activeEvent), activeEnrollment.getEvents());

    Set<UID> ownedDeletedChildUids = result.deletedChildUidsByParent().get(te.getTrackedEntity());
    assertEquals(
        Set.of(
            deletedEnrollment.getEnrollment(),
            deletedEvent.getEvent(),
            deletedTeRelationship.getRelationship()),
        ownedDeletedChildUids);
  }

  @Test
  void shouldReportFailedChildWhenNestedEnrollmentFailed() {
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te =
        org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity.builder()
            .trackedEntity(UID.of("TrackedEnt1"))
            .enrollments(List.of(Enrollment.builder().enrollment(UID.of("Enrol000001")).build()))
            .relationships(List.of())
            .build();

    boolean hasFailedChild =
        service.hasFailedChild(te, Map.of(TrackerType.ENROLLMENT, Set.of(UID.of("Enrol000001"))));

    assertTrue(hasFailedChild);
  }

  @Test
  void shouldNotReportFailedChildWhenNoNestedChildFailed() {
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te =
        org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity.builder()
            .trackedEntity(UID.of("TrackedEnt1"))
            .enrollments(List.of(Enrollment.builder().enrollment(UID.of("Enrol000001")).build()))
            .relationships(List.of())
            .build();

    boolean hasFailedChild =
        service.hasFailedChild(te, Map.of(TrackerType.ENROLLMENT, Set.of(UID.of("SomeOtherOn"))));

    assertFalse(hasFailedChild);
  }

  @Test
  void shouldStripAttributesAndDataValuesFlaggedSkipSynchronization() {
    Event event =
        Event.builder()
            .event(UID.of("Event000001"))
            .programStage("ProgStage01")
            .dataValues(
                Set.of(
                    DataValue.builder().dataElement("SkipDe00001").build(),
                    DataValue.builder().dataElement("KeepDe00001").build()))
            .build();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(UID.of("Enrol000001"))
            .events(List.of(event))
            .attributes(
                List.of(
                    Attribute.builder().attribute("SkipAttr001").build(),
                    Attribute.builder().attribute("SkipAttr002").build(),
                    Attribute.builder().attribute("KeepAttr001").build()))
            .build();
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity teDto =
        org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity.builder()
            .trackedEntity(UID.of("TrackedEnt1"))
            .enrollments(List.of(enrollment))
            .attributes(
                List.of(
                    Attribute.builder().attribute("SkipAttr001").build(),
                    Attribute.builder().attribute("KeepAttr001").build()))
            .build();

    TrackerSynchronizationContext context =
        forEntities(
            null,
            1,
            null,
            50,
            Map.of("ProgStage01", Set.of("SkipDe00001")),
            Set.of(UID.of("SkipAttr001"), UID.of("SkipAttr002")));

    service.stripSkipSyncFields(List.of(), List.of(teDto), context);

    assertEquals(
        List.of("KeepAttr001"),
        teDto.getAttributes().stream().map(Attribute::getAttribute).toList());
    assertEquals(
        List.of("KeepAttr001"),
        enrollment.getAttributes().stream().map(Attribute::getAttribute).toList());
    assertEquals(
        Set.of("KeepDe00001"),
        event.getDataValues().stream()
            .map(DataValue::getDataElement)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void shouldStampSyncTimestampWhenActiveTrackedEntitySyncSucceeds() throws Exception {
    UID teUid = UID.of("TrackedEnt1");
    stubHappyPathSyncInfrastructure(List.of(activeTrackedEntity(teUid)));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(reportFor(TrackerType.TRACKED_ENTITY, Set.of(teUid), Set.of()));

    SynchronizationResult result = service.synchronizeData(50, JobProgress.noop());

    assertEquals(SynchronizationStatus.SUCCESS, result.status);
    verify(trackedEntityService)
        .updateTrackedEntitiesSyncTimestamp(eq(Set.of(teUid)), any(Date.class));
  }

  @Test
  void shouldNotStampSyncTimestampWhenActiveTrackedEntitySyncFails() throws Exception {
    UID teUid = UID.of("TrackedEnt1");
    stubHappyPathSyncInfrastructure(List.of(activeTrackedEntity(teUid)));
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(reportFor(TrackerType.TRACKED_ENTITY, Set.of(), Set.of(teUid)));

    service.synchronizeData(50, JobProgress.noop());

    verify(trackedEntityService, never())
        .updateTrackedEntitiesSyncTimestamp(any(), any(Date.class));
  }

  @Test
  void shouldStampSyncTimestampWhenDeletedTrackedEntityIsAlreadyDeletedOnRemote() throws Exception {
    UID teUid = UID.of("TrackedEnt1");
    org.hisp.dhis.tracker.model.TrackedEntity deletedTe = activeTrackedEntity(teUid);
    deletedTe.setDeleted(true);
    stubHappyPathSyncInfrastructure(List.of(deletedTe));

    Entity alreadyDeleted = new Entity(TrackerType.TRACKED_ENTITY, teUid);
    alreadyDeleted
        .getErrorReports()
        .add(
            new Error(
                "Tracked entity already deleted",
                "E1114",
                TrackerType.TRACKED_ENTITY.name(),
                teUid,
                List.of()));
    TrackerTypeReport typeReport = new TrackerTypeReport(TrackerType.TRACKED_ENTITY);
    typeReport.setEntityReport(new ArrayList<>(List.of(alreadyDeleted)));
    TrackerTypeReport empty = new TrackerTypeReport(TrackerType.TRACKED_ENTITY);
    ImportReport report =
        ImportReport.builder()
            .status(Status.ERROR)
            .persistenceReport(new PersistenceReport(typeReport, empty, empty, empty, empty))
            .build();
    when(restTemplate.<ImportReport>execute(anyString(), eq(HttpMethod.POST), any(), any()))
        .thenReturn(report);

    service.synchronizeData(50, JobProgress.noop());

    verify(trackedEntityService)
        .updateTrackedEntitiesSyncTimestamp(eq(Set.of(teUid)), any(Date.class));
  }

  /** Stubs the availability check, {@code createContext}, and page fetch for a happy-path run. */
  private void stubHappyPathSyncInfrastructure(List<org.hisp.dhis.tracker.model.TrackedEntity> page)
      throws Exception {
    when(systemSettingsService.getCurrentSettings()).thenReturn(settings);
    when(settings.getRemoteInstanceUrl()).thenReturn("http://remote");
    when(settings.getRemoteInstanceUsername()).thenReturn("admin");
    when(settings.getRemoteInstancePassword()).thenReturn("district");
    when(settings.getSyncMaxAttempts()).thenReturn(1);
    when(settings.getSyncMaxRemoteServerAvailabilityCheckAttempts()).thenReturn(2);
    when(settings.getSyncSkipSyncForDataChangedBefore()).thenReturn(new Date(0));
    when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    when(trackedEntityService.getTrackedEntityCount(any())).thenReturn((long) page.size());
    when(trackedEntityService.findTrackedEntities(any(), any()))
        .thenReturn(new Page<>(page, PageParams.of(1, 50, false)));
  }

  private org.hisp.dhis.tracker.model.TrackedEntity activeTrackedEntity(UID uid) {
    org.hisp.dhis.tracker.model.TrackedEntity te = new org.hisp.dhis.tracker.model.TrackedEntity();
    te.setUid(uid.getValue());
    TrackedEntityType type = new TrackedEntityType();
    type.setUid("TeType0001");
    te.setTrackedEntityType(type);
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("OrgUnit001");
    te.setOrganisationUnit(orgUnit);
    te.setCreated(new Date());
    te.setLastUpdated(new Date());
    return te;
  }

  /**
   * An {@link ImportReport} with {@code succeededUids} reported as synced and {@code failedUids} as
   * failed.
   */
  private ImportReport reportFor(TrackerType type, Set<UID> succeededUids, Set<UID> failedUids) {
    List<Entity> entities = new ArrayList<>();
    succeededUids.forEach(uid -> entities.add(new Entity(type, uid)));
    failedUids.forEach(
        uid -> {
          Entity failed = new Entity(type, uid);
          failed
              .getErrorReports()
              .add(new Error("Validation failed", "E1000", type.name(), uid, List.of()));
          entities.add(failed);
        });
    TrackerTypeReport typeReport = new TrackerTypeReport(type);
    typeReport.setEntityReport(entities);
    TrackerTypeReport empty = new TrackerTypeReport(type);
    PersistenceReport persistenceReport =
        type == TrackerType.TRACKED_ENTITY
            ? new PersistenceReport(typeReport, empty, empty, empty, empty)
            : type == TrackerType.ENROLLMENT
                ? new PersistenceReport(empty, typeReport, empty, empty, empty)
                : type == TrackerType.EVENT
                    ? new PersistenceReport(empty, empty, typeReport, empty, empty)
                    : new PersistenceReport(empty, empty, empty, empty, typeReport);
    return ImportReport.builder()
        .status(failedUids.isEmpty() ? Status.OK : Status.ERROR)
        .persistenceReport(persistenceReport)
        .build();
  }
}
