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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.trackedentity.TrackedEntityMapper;
import org.hisp.dhis.webapi.controller.tracker.view.Attribute;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class TrackerDataSynchronizationService
    extends BaseDataSynchronizationWithPaging<
        org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity, TrackedEntity> {
  private static final String PROCESS_NAME = "Tracker Data Synchronization";
  private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER =
      Mappers.getMapper(TrackedEntityMapper.class);

  private final TrackedEntityService trackedEntityService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final TrackedEntityAttributeService trackedEntityAttributeService;

  public TrackerDataSynchronizationService(
      TrackedEntityService trackedEntityService,
      ProgramStageDataElementService programStageDataElementService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      SystemSettingsService systemSettingsService,
      RestTemplate restTemplate,
      RenderService renderService) {
    super(renderService, restTemplate, systemSettingsService);
    this.trackedEntityService = trackedEntityService;
    this.programStageDataElementService = programStageDataElementService;
    this.trackedEntityAttributeService = trackedEntityAttributeService;
  }

  @Override
  public void updateEntitySyncTimeStamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      Date syncTime) {
    List<String> trackedEntityUids =
        trackedEntities.stream().map(te -> te.getTrackedEntity().getValue()).toList();

    trackedEntityService.updateTrackedEntitiesSyncTimestamp(UID.of(trackedEntityUids), syncTime);
  }

  @Override
  public List<TrackedEntity> fetchEntitiesForPage(TrackerSynchronizationContext context)
      throws BadRequestException, ForbiddenException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.buildForDataSync(context.getSkipChangedBefore()).build();
    return trackedEntityService
        .findTrackedEntities(params, PageParams.of(1, context.getPageSize(), false))
        .getItems();
  }

  @Override
  public long countEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.buildForDataSync(skipChangedBefore).build();
    return trackedEntityService.getTrackedEntityCount(params);
  }

  @Override
  public boolean isDeleted(TrackedEntity entity) {
    return entity.isDeleted();
  }

  @Override
  public org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity getMappedEntities(
      TrackedEntity ev, TrackerIdSchemeParams idSchemeParam, MappingErrors errors) {
    return TRACKED_ENTITY_MAPPER.map(idSchemeParam, errors, ev);
  }

  @Override
  public String getJsonRootName() {
    return "trackedEntities";
  }

  @Override
  public String getEntityName() {
    return "Tracked Entities";
  }

  @Override
  public String getProcessName() {
    return PROCESS_NAME;
  }

  @Override
  public TrackerType getTrackerType() {
    return TrackerType.TRACKED_ENTITY;
  }

  @Override
  public UID getUid(org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity entity) {
    return entity.getTrackedEntity();
  }

  @Override
  public org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity toMinimalEntity(
      TrackedEntity trackedEntity) {
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity minimalTrackedEntity =
        new org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity();
    minimalTrackedEntity.setTrackedEntity(UID.of(trackedEntity.getUid()));
    return minimalTrackedEntity;
  }

  @Override
  public TrackerSynchronizationContext createContext(int pageSize, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();

    long trackedEntityCount = countEntitiesForSynchronization(skipChangedBefore);

    if (trackedEntityCount == 0) {
      return TrackerSynchronizationContext.emptyContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);

    return TrackerSynchronizationContext.forEntities(
        skipChangedBefore,
        trackedEntityCount,
        instance,
        pageSize,
        getSkipSyncDataElementsByProgramStage(),
        getSkipSyncAttributeUids());
  }

  private Map<String, Set<String>> getSkipSyncDataElementsByProgramStage() {
    return programStageDataElementService
        .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();
  }

  private Set<UID> getSkipSyncAttributeUids() {
    return trackedEntityAttributeService
        .getTrackedEntityAttributeUidsWithSkipSynchronizationSetToTrue();
  }

  @Override
  protected void stripSkipSyncFields(
      List<TrackedEntity> activeDomainEntities,
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> activeDtos,
      TrackerSynchronizationContext context) {
    Set<UID> skipSyncAttributeUids = context.getSkipSyncAttributeUids();
    Map<String, Set<String>> skipSyncDataElementsByProgramStage =
        context.getSkipSyncDataElementsByProgramStage();

    for (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te : activeDtos) {
      te.setAttributes(stripSkipSyncAttributes(te.getAttributes(), skipSyncAttributeUids));

      for (Enrollment enrollment : te.getEnrollments()) {
        enrollment.setAttributes(
            stripSkipSyncAttributes(enrollment.getAttributes(), skipSyncAttributeUids));

        for (Event event : enrollment.getEvents()) {
          Set<String> skipDataElements =
              skipSyncDataElementsByProgramStage.getOrDefault(event.getProgramStage(), Set.of());
          if (!skipDataElements.isEmpty()) {
            event.setDataValues(
                event.getDataValues().stream()
                    .filter(dv -> !skipDataElements.contains(dv.getDataElement()))
                    .collect(Collectors.toSet()));
          }
        }
      }
    }
  }

  private List<Attribute> stripSkipSyncAttributes(
      List<Attribute> attributes, Set<UID> skipSyncAttributeUids) {
    if (skipSyncAttributeUids.isEmpty()) {
      return attributes;
    }
    return attributes.stream()
        .filter(a -> !skipSyncAttributeUids.contains(UID.of(a.getAttribute())))
        .toList();
  }

  @Override
  protected NestedDeletion<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity>
      splitDeletedChildren(
          List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> activeEntities) {
    List<Enrollment> deletedEnrollments = new ArrayList<>();
    List<Event> deletedEvents = new ArrayList<>();
    Map<UID, Relationship> deletedRelationshipsByUid = new LinkedHashMap<>();
    // Maps each active TE to the enrollment/event/relationship UIDs it owns. If deleting one of
    // those really fails (not just "already deleted"), we must not save a sync timestamp for
    // that TE later, or it would never be retried.
    Map<UID, Set<UID>> deletedChildUidsByTe = new HashMap<>();

    for (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te : activeEntities) {
      Set<UID> ownedDeletedChildUids =
          deletedChildUidsByTe.computeIfAbsent(te.getTrackedEntity(), k -> new HashSet<>());

      List<Enrollment> enrollments = te.getEnrollments();
      List<Enrollment> deletedEnrollmentsForTe =
          enrollments.stream().filter(Enrollment::isDeleted).toList();
      deletedEnrollments.addAll(deletedEnrollmentsForTe);
      deletedEnrollmentsForTe.forEach(e -> ownedDeletedChildUids.add(e.getEnrollment()));
      List<Enrollment> activeEnrollments =
          enrollments.stream().filter(e -> !e.isDeleted()).toList();
      te.setEnrollments(activeEnrollments);

      for (Enrollment enrollment : activeEnrollments) {
        List<Event> events = enrollment.getEvents();
        List<Event> deletedEventsForEnrollment = events.stream().filter(Event::isDeleted).toList();
        deletedEvents.addAll(deletedEventsForEnrollment);
        deletedEventsForEnrollment.forEach(e -> ownedDeletedChildUids.add(e.getEvent()));
        List<Event> activeEvents = events.stream().filter(e -> !e.isDeleted()).toList();
        enrollment.setEvents(activeEvents);

        enrollment.setRelationships(
            stripDeletedRelationships(
                enrollment.getRelationships(), deletedRelationshipsByUid, ownedDeletedChildUids));
        for (Event event : activeEvents) {
          event.setRelationships(
              stripDeletedRelationships(
                  event.getRelationships(), deletedRelationshipsByUid, ownedDeletedChildUids));
        }
      }

      te.setRelationships(
          stripDeletedRelationships(
              te.getRelationships(), deletedRelationshipsByUid, ownedDeletedChildUids));
    }

    Map<String, List<?>> deletedPayloadByJsonKey =
        Map.of(
            "enrollments", deletedEnrollments.stream().map(this::toMinimalEnrollment).toList(),
            "events", deletedEvents.stream().map(this::toMinimalEvent).toList(),
            "relationships",
                deletedRelationshipsByUid.values().stream()
                    .map(r -> toMinimalRelationship(r))
                    .toList());
    Map<TrackerType, Integer> deletedCountByType =
        Map.of(
            TrackerType.ENROLLMENT, deletedEnrollments.size(),
            TrackerType.EVENT, deletedEvents.size(),
            TrackerType.RELATIONSHIP, deletedRelationshipsByUid.size());

    return new NestedDeletion<>(
        activeEntities, deletedPayloadByJsonKey, deletedCountByType, deletedChildUidsByTe);
  }

  @Override
  protected boolean hasFailedChild(
      org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te,
      Map<TrackerType, Set<UID>> failedChildUidsByType) {
    Set<UID> failedEnrollments = failedChildUidsByType.get(TrackerType.ENROLLMENT);
    Set<UID> failedEvents = failedChildUidsByType.get(TrackerType.EVENT);
    Set<UID> failedRelationships = failedChildUidsByType.get(TrackerType.RELATIONSHIP);

    if (hasFailedRelationship(te.getRelationships(), failedRelationships)) {
      return true;
    }
    for (Enrollment enrollment : te.getEnrollments()) {
      if (failedEnrollments.contains(enrollment.getEnrollment())
          || hasFailedRelationship(enrollment.getRelationships(), failedRelationships)) {
        return true;
      }
      for (Event event : enrollment.getEvents()) {
        if (failedEvents.contains(event.getEvent())
            || hasFailedRelationship(event.getRelationships(), failedRelationships)) {
          return true;
        }
      }
    }
    return false;
  }

  private Enrollment toMinimalEnrollment(Enrollment enrollment) {
    Enrollment minimal = new Enrollment();
    minimal.setEnrollment(enrollment.getEnrollment());
    return minimal;
  }

  private Event toMinimalEvent(Event event) {
    Event minimal = new Event();
    minimal.setEvent(event.getEvent());
    return minimal;
  }
}
