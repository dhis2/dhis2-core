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

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.alreadyDeletedOrSucceededUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.blockingFailedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.failedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.formatFailedUids;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.sendTrackerRequest;
import static org.hisp.dhis.webapi.controller.tracker.sync.TrackerSyncReportUtils.successfullyProcessedUids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityFields;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.trackedentity.TrackedEntityMapper;
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
@RequiredArgsConstructor
public class TrackerDataSynchronizationService extends TrackerDataSynchronizationWithPaging {
  private static final String PROCESS_NAME = "Tracker data synchronization";
  private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER =
      Mappers.getMapper(TrackedEntityMapper.class);

  private record DeleteSyncResult(Set<UID> syncedTeUids, Set<UID> blockingFailedChildUids) {}

  private final TrackedEntityService trackedEntityService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final SystemSettingsService systemSettingsService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;

  @Getter
  private static final class TrackerSynchronizationContext extends PagedDataSynchronisationContext {
    private final Map<String, Set<String>> skipSyncDataElementsByProgramStage;
    private final Set<UID> failedTrackedEntityUids = new HashSet<>();
    // Distinct tracked entity uids fetched across all pages this run, so the run's completion can
    // report how many entities in the backlog were never attempted at all (see
    // executeSynchronizationWithPaging), e.g. because repeatedly failing entities kept occupying
    // page 1 slots and crowded out entities further back in the queue.
    private final Set<UID> attemptedTrackedEntityUids = new HashSet<>();

    public TrackerSynchronizationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize, Map.of());
    }

    public TrackerSynchronizationContext(
        Date skipChangedBefore,
        long objectsToSynchronize,
        SystemInstance instance,
        int pageSize,
        Map<String, Set<String>> skipSyncDataElementsByProgramStage) {
      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
      this.skipSyncDataElementsByProgramStage = skipSyncDataElementsByProgramStage;
    }

    public boolean hasNoObjectsToSynchronize() {
      return getObjectsToSynchronize() == 0;
    }
  }

  @Override
  public SynchronizationResult synchronizeTrackerData(int pageSize, JobProgress progress) {
    progress.startingProcess(PROCESS_NAME);

    SystemSettings settings = systemSettingsService.getCurrentSettings();

    SynchronizationResult validationResult =
        validatePreconditions(settings, progress, restTemplate, PROCESS_NAME);
    if (validationResult != null) {
      return validationResult;
    }

    TrackerSynchronizationContext context = initializeContext(pageSize, progress, settings);

    if (context.hasNoObjectsToSynchronize()) {
      return endProcess(progress, "No tracked entities to synchronize", PROCESS_NAME);
    }

    boolean success = executeSynchronizationWithPaging(context, progress, settings);

    return success
        ? endProcess(progress, "Completed successfully", PROCESS_NAME)
        : failProcess(progress, "Page-level synchronization failed", PROCESS_NAME);
  }

  private TrackerSynchronizationContext initializeContext(
      int pageSize, JobProgress progress, SystemSettings settings) {
    return progress.runStage(
        new TrackerSynchronizationContext(null, pageSize),
        ctx ->
            format("Tracked entities changed before %s will not sync", ctx.getSkipChangedBefore()),
        () -> createContext(pageSize, settings));
  }

  private TrackerSynchronizationContext createContext(int pageSize, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();

    long trackedEntityCount = countTrackedEntitiesForSynchronization(skipChangedBefore);

    if (trackedEntityCount == 0) {
      return new TrackerSynchronizationContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);

    return new TrackerSynchronizationContext(
        skipChangedBefore,
        trackedEntityCount,
        instance,
        pageSize,
        getSkipSyncDataElementsByProgramStage());
  }

  private Map<String, Set<String>> getSkipSyncDataElementsByProgramStage() {
    return programStageDataElementService
        .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();
  }

  private long countTrackedEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    return trackedEntityService.getTrackedEntityCount(
        TrackedEntityOperationParams.builder()
            .skipChangedBefore(skipChangedBefore)
            .includeDeleted(true)
            .synchronizationQuery(true)
            .build());
  }

  private boolean executeSynchronizationWithPaging(
      TrackerSynchronizationContext context, JobProgress progress, SystemSettings settings) {
    String stageDescription =
        format(
            "Found %d tracked entities. Remote: %s. Pages: %d (size %d)",
            context.getObjectsToSynchronize(),
            context.getInstance().getUrl(),
            context.getPages(),
            context.getPageSize());

    progress.startingStage(stageDescription, context.getPages(), SKIP_ITEM);

    progress.runStage(
        IntStream.range(1, context.getPages() + 1).boxed(),
        page -> format("Syncing page %d (size %d)", page, context.getPageSize()),
        page -> synchronizePageSafely(page, context, settings));

    long unprocessed =
        context.getObjectsToSynchronize() - context.getAttemptedTrackedEntityUids().size();
    if (unprocessed > 0) {
      log.info(
          "Tracker data synchronization: {} of {} tracked entities were never attempted this run",
          unprocessed,
          context.getObjectsToSynchronize());
    }

    return !progress.isSkipCurrentStage();
  }

  private void synchronizePageSafely(
      int page, TrackerSynchronizationContext context, SystemSettings settings) {
    try {
      synchronizePage(context, settings);
    } catch (Exception ex) {
      log.error("Failed to synchronize page {}", page, ex);
      throw new RuntimeException(
          format("Page %d synchronization failed: %s", page, ex.getMessage()), ex);
    }
  }

  private void synchronizePage(TrackerSynchronizationContext context, SystemSettings settings)
      throws ForbiddenException, BadRequestException, NotFoundException {
    List<TrackedEntity> trackedEntities = fetchTrackedEntitiesForPage(context);

    Map<Boolean, List<TrackedEntity>> partitionedTrackedEntities =
        partitionTrackedEntitiesByDeletionStatus(trackedEntities);
    List<TrackedEntity> deletedTrackedEntities = partitionedTrackedEntities.get(true);
    List<TrackedEntity> activeTrackedEntities = partitionedTrackedEntities.get(false);

    syncTrackedEntitiesByDeletionStatus(
        activeTrackedEntities, deletedTrackedEntities, context, settings);
  }

  private List<TrackedEntity> fetchTrackedEntitiesForPage(TrackerSynchronizationContext context)
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .skipChangedBefore(context.getSkipChangedBefore())
            .synchronizationQuery(true)
            .includeDeleted(true)
            .fields(TrackedEntityFields.all())
            .build();
    // Always query page 1: entities synced by a prior iteration drop out of the synchronization
    // filter on their own, so the next unsynced batch is always at the front of the result set.
    // Using an offset here would skip entities, since the filtered set shrinks between iterations.
    // Entities that failed to sync are not excluded, so they are retried on every subsequent
    // page 1 fetch this run, potentially crowding out backlog entities that never get attempted
    // at all.
    return trackedEntityService
        .findTrackedEntities(params, PageParams.of(1, context.getPageSize(), false))
        .getItems();
  }

  private Map<Boolean, List<TrackedEntity>> partitionTrackedEntitiesByDeletionStatus(
      List<TrackedEntity> trackedEntities) {
    return trackedEntities.stream().collect(Collectors.partitioningBy(TrackedEntity::isDeleted));
  }

  private record SplitActiveTrackedEntities(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> activeTrackedEntities,
      List<Enrollment> deletedEnrollments,
      List<Event> deletedEvents,
      Map<UID, Relationship> deletedRelationshipsByUid,
      Map<UID, Set<UID>> deletedChildUidsByTe) {}

  private void syncTrackedEntitiesByDeletionStatus(
      List<TrackedEntity> active,
      List<TrackedEntity> deleted,
      TrackerSynchronizationContext context,
      SystemSettings settings) {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    Set<UID> attemptedThisPage = new HashSet<>();
    active.forEach(te -> attemptedThisPage.add(UID.of(te.getUid())));
    deleted.forEach(te -> attemptedThisPage.add(UID.of(te.getUid())));
    context.getAttemptedTrackedEntityUids().addAll(attemptedThisPage);

    SplitActiveTrackedEntities splitActiveEntities = splitActiveTrackedEntities(active, context);
    List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> deletedTrackedEntities =
        deleted.stream().map(this::toMinimalTrackedEntity).toList();

    DeleteSyncResult deleteResult =
        syncDeletedIfNeeded(deletedTrackedEntities, splitActiveEntities, instance, settings);

    Set<UID> activeSyncCandidateUids =
        splitActiveEntities.activeTrackedEntities().isEmpty()
            ? Set.of()
            : syncActive(splitActiveEntities.activeTrackedEntities(), instance, settings);

    Set<UID> syncedActiveTeUids =
        resolveSyncedActiveTeUids(
            activeSyncCandidateUids, splitActiveEntities.deletedChildUidsByTe(), deleteResult);
    Set<UID> syncedDeletedTeUids = deleteResult == null ? Set.of() : deleteResult.syncedTeUids();

    stampSyncTimestamp(deletedTrackedEntities, syncedDeletedTeUids, syncTime);
    stampSyncTimestamp(splitActiveEntities.activeTrackedEntities(), syncedActiveTeUids, syncTime);
  }

  private SplitActiveTrackedEntities splitActiveTrackedEntities(
      List<TrackedEntity> active, TrackerSynchronizationContext context) {
    TrackerIdSchemeParams idSchemeParams =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.UID).build();
    MappingErrors errors = new MappingErrors(idSchemeParams);

    List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> activeTrackedEntities =
        active.stream().map(te -> TRACKED_ENTITY_MAPPER.map(idSchemeParams, errors, te)).toList();

    List<Enrollment> deletedEnrollments = new ArrayList<>();
    List<Event> deletedEvents = new ArrayList<>();
    Map<UID, Relationship> deletedRelationshipsByUid = new LinkedHashMap<>();
    // Which deleted enrollment/event/relationship UIDs belong to which active TE, so a blocking
    // (non idempotent) failure to delete one of them can withhold that TE's timestamp later.
    Map<UID, Set<UID>> deletedChildUidsByTe = new HashMap<>();
    if (!activeTrackedEntities.isEmpty()) {
      stripDeletedChildren(
          activeTrackedEntities,
          deletedEnrollments,
          deletedEvents,
          deletedRelationshipsByUid,
          deletedChildUidsByTe);
      stripSkipSyncFields(
          activeTrackedEntities,
          skipSyncAttributeUids(active),
          context.getSkipSyncDataElementsByProgramStage());
    }

    return new SplitActiveTrackedEntities(
        activeTrackedEntities,
        deletedEnrollments,
        deletedEvents,
        deletedRelationshipsByUid,
        deletedChildUidsByTe);
  }

  private DeleteSyncResult syncDeletedIfNeeded(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> deletedTrackedEntities,
      SplitActiveTrackedEntities prepared,
      SystemInstance instance,
      SystemSettings settings) {
    if (deletedTrackedEntities.isEmpty()
        && prepared.deletedEnrollments().isEmpty()
        && prepared.deletedEvents().isEmpty()
        && prepared.deletedRelationshipsByUid().isEmpty()) {
      return null;
    }
    return syncDeleted(
        deletedTrackedEntities,
        prepared.deletedEnrollments(),
        prepared.deletedEvents(),
        prepared.deletedRelationshipsByUid().values(),
        instance,
        settings);
  }

  private Set<UID> resolveSyncedActiveTeUids(
      Set<UID> activeSyncCandidateUids,
      Map<UID, Set<UID>> deletedChildUidsByTe,
      DeleteSyncResult deleteResult) {
    Set<UID> blockingFailedChildUids =
        deleteResult == null ? Set.of() : deleteResult.blockingFailedChildUids();
    return activeSyncCandidateUids.stream()
        .filter(
            teUid ->
                Collections.disjoint(
                    deletedChildUidsByTe.getOrDefault(teUid, Set.of()), blockingFailedChildUids))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private void stampSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> candidates,
      Set<UID> syncedUids,
      Date syncTime) {
    if (syncedUids.isEmpty()) {
      return;
    }
    List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> syncedTes =
        candidates.stream().filter(te -> syncedUids.contains(te.getTrackedEntity())).toList();
    updateTrackedEntitiesSyncTimestamp(syncedTes, syncTime);
  }

  private void stripDeletedChildren(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      List<Enrollment> deletedEnrollments,
      List<Event> deletedEvents,
      Map<UID, Relationship> deletedRelationshipsByUid,
      Map<UID, Set<UID>> deletedChildUidsByTe) {
    for (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te : trackedEntities) {
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
  }

  private List<Relationship> stripDeletedRelationships(
      List<Relationship> relationships,
      Map<UID, Relationship> deletedRelationshipsByUid,
      Set<UID> ownedDeletedChildUids) {
    relationships.stream()
        .filter(Relationship::isDeleted)
        .forEach(
            r -> {
              deletedRelationshipsByUid.put(r.getRelationship(), r);
              ownedDeletedChildUids.add(r.getRelationship());
            });
    return relationships.stream().filter(r -> !r.isDeleted()).toList();
  }

  private Set<String> skipSyncAttributeUids(List<TrackedEntity> trackedEntities) {
    return trackedEntities.stream()
        .flatMap(te -> te.getTrackedEntityAttributeValues().stream())
        .map(TrackedEntityAttributeValue::getAttribute)
        .filter(a -> Boolean.TRUE.equals(a.getSkipSynchronization()))
        .map(BaseIdentifiableObject::getUid)
        .collect(Collectors.toSet());
  }

  private void stripSkipSyncFields(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      Set<String> skipSyncAttributeUids,
      Map<String, Set<String>> skipSyncDataElementsByProgramStage) {
    for (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te : trackedEntities) {
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

  private List<org.hisp.dhis.webapi.controller.tracker.view.Attribute> stripSkipSyncAttributes(
      List<org.hisp.dhis.webapi.controller.tracker.view.Attribute> attributes,
      Set<String> skipSyncAttributeUids) {
    if (skipSyncAttributeUids.isEmpty()) {
      return attributes;
    }
    return attributes.stream()
        .filter(a -> !skipSyncAttributeUids.contains(a.getAttribute()))
        .toList();
  }

  private DeleteSyncResult syncDeleted(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> deletedTrackedEntities,
      List<Enrollment> deletedEnrollments,
      List<Event> deletedEvents,
      Collection<Relationship> deletedRelationships,
      SystemInstance instance,
      SystemSettings settings) {
    String url = instance.getUrl() + "?importStrategy=" + DELETE + "&async=false&atomicMode=OBJECT";

    Map<String, List<?>> payload =
        Map.of(
            "trackedEntities", deletedTrackedEntities,
            "enrollments", deletedEnrollments.stream().map(this::toMinimalEnrollment).toList(),
            "events", deletedEvents.stream().map(this::toMinimalEvent).toList(),
            "relationships",
                deletedRelationships.stream().map(this::toMinimalRelationship).toList());

    ImportReport report =
        sendTrackerRequest(restTemplate, renderService, payload, instance, settings, url);

    // An entity whose own delete came back "already deleted" achieved its goal, so it is treated as
    // synced here too.
    Set<UID> syncedTeUids = alreadyDeletedOrSucceededUids(report, TrackerType.TRACKED_ENTITY);
    Set<UID> syncedEnrollmentUids = alreadyDeletedOrSucceededUids(report, TrackerType.ENROLLMENT);
    Set<UID> syncedEventUids = alreadyDeletedOrSucceededUids(report, TrackerType.EVENT);
    Set<UID> syncedRelationshipUids =
        alreadyDeletedOrSucceededUids(report, TrackerType.RELATIONSHIP);
    List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> syncedTes =
        deletedTrackedEntities.stream()
            .filter(te -> syncedTeUids.contains(te.getTrackedEntity()))
            .toList();

    Set<UID> failedTeUids = blockingFailedUids(report, TrackerType.TRACKED_ENTITY);
    Set<UID> failedEnrollmentUids = blockingFailedUids(report, TrackerType.ENROLLMENT);
    Set<UID> failedEventUids = blockingFailedUids(report, TrackerType.EVENT);
    Set<UID> failedRelationshipUids = blockingFailedUids(report, TrackerType.RELATIONSHIP);

    Set<UID> blockingFailedChildUids = new HashSet<>();
    Stream.of(failedEnrollmentUids, failedEventUids, failedRelationshipUids)
        .forEach(blockingFailedChildUids::addAll);

    log.info(
        "Tracker delete sync: TEs={}/{} synced{}, enrollments={}/{} synced{},"
            + " events={}/{} synced{}, relationships={}/{} synced{}",
        syncedTes.size(),
        deletedTrackedEntities.size(),
        formatFailedUids(failedTeUids),
        syncedEnrollmentUids.size(),
        deletedEnrollments.size(),
        formatFailedUids(failedEnrollmentUids),
        syncedEventUids.size(),
        deletedEvents.size(),
        formatFailedUids(failedEventUids),
        syncedRelationshipUids.size(),
        deletedRelationships.size(),
        formatFailedUids(failedRelationshipUids));

    return new DeleteSyncResult(syncedTeUids, blockingFailedChildUids);
  }

  private Set<UID> syncActive(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      SystemInstance instance,
      SystemSettings settings) {
    String url =
        instance.getUrl()
            + "?importStrategy="
            + CREATE_AND_UPDATE
            + "&async=false&atomicMode=OBJECT";

    ImportReport report =
        sendTrackerRequest(
            restTemplate,
            renderService,
            Map.of("trackedEntities", trackedEntities),
            instance,
            settings,
            url);

    Set<UID> syncedTeUids = getTrackedEntitiesWithAllChildrenSynchronized(report, trackedEntities);

    List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> syncedTes =
        trackedEntities.stream()
            .filter(te -> syncedTeUids.contains(te.getTrackedEntity()))
            .toList();

    List<Enrollment> enrollments =
        trackedEntities.stream().flatMap(te -> te.getEnrollments().stream()).toList();
    List<Event> events = enrollments.stream().flatMap(e -> e.getEvents().stream()).toList();
    Set<UID> relationshipUids = getAllRelationshipUids(trackedEntities);
    Set<UID> failedEnrollmentUids = failedUids(report, TrackerType.ENROLLMENT);
    Set<UID> failedEventUids = failedUids(report, TrackerType.EVENT);
    Set<UID> failedRelationshipUids = failedUids(report, TrackerType.RELATIONSHIP);

    log.info(
        "Tracker create/update sync: TEs={}/{} synced, enrollments={}/{} synced{},"
            + " events={}/{} synced{}, relationships={}/{} synced{}",
        syncedTes.size(),
        trackedEntities.size(),
        successfullyProcessedUids(report, TrackerType.ENROLLMENT).size(),
        enrollments.size(),
        formatFailedUids(failedEnrollmentUids),
        successfullyProcessedUids(report, TrackerType.EVENT).size(),
        events.size(),
        formatFailedUids(failedEventUids),
        successfullyProcessedUids(report, TrackerType.RELATIONSHIP).size(),
        relationshipUids.size(),
        formatFailedUids(failedRelationshipUids));

    return syncedTeUids;
  }

  private Set<UID> getAllRelationshipUids(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities) {
    // Deduplicate by UID because a relationship is attached to every entity that is one of its
    // from/to parties, so the same
    // relationship can appear under two different tracked entities.
    Set<UID> relationshipUids = new HashSet<>();
    for (org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te : trackedEntities) {
      te.getRelationships().forEach(r -> relationshipUids.add(r.getRelationship()));
      for (Enrollment enrollment : te.getEnrollments()) {
        enrollment.getRelationships().forEach(r -> relationshipUids.add(r.getRelationship()));
        for (Event event : enrollment.getEvents()) {
          event.getRelationships().forEach(r -> relationshipUids.add(r.getRelationship()));
        }
      }
    }
    return relationshipUids;
  }

  private Set<UID> getTrackedEntitiesWithAllChildrenSynchronized(
      ImportReport report,
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities) {
    Set<UID> succeededUids = successfullyProcessedUids(report, TrackerType.TRACKED_ENTITY);
    if (succeededUids.isEmpty()) {
      return succeededUids;
    }

    Set<UID> failedEnrollments = failedUids(report, TrackerType.ENROLLMENT);
    Set<UID> failedEvents = failedUids(report, TrackerType.EVENT);
    Set<UID> failedRelationships = failedUids(report, TrackerType.RELATIONSHIP);
    if (failedEnrollments.isEmpty() && failedEvents.isEmpty() && failedRelationships.isEmpty()) {
      return succeededUids;
    }

    return trackedEntities.stream()
        .filter(te -> succeededUids.contains(te.getTrackedEntity()))
        .filter(te -> !hasFailedChild(te, failedEnrollments, failedEvents, failedRelationships))
        .map(org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity::getTrackedEntity)
        .collect(Collectors.toCollection(HashSet::new));
  }

  private boolean hasFailedChild(
      org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity te,
      Set<UID> failedEnrollments,
      Set<UID> failedEvents,
      Set<UID> failedRelationships) {
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

  private boolean hasFailedRelationship(
      List<Relationship> relationships, Set<UID> failedRelationships) {
    return relationships.stream().anyMatch(r -> failedRelationships.contains(r.getRelationship()));
  }

  private void updateTrackedEntitiesSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      Date syncTime) {
    Set<UID> trackedEntityUids =
        trackedEntities.stream()
            .map(org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity::getTrackedEntity)
            .collect(Collectors.toSet());

    trackedEntityService.updateTrackedEntitiesSyncTimestamp(trackedEntityUids, syncTime);
  }

  private org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity toMinimalTrackedEntity(
      TrackedEntity trackedEntity) {
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity minimalTrackedEntity =
        new org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity();
    minimalTrackedEntity.setTrackedEntity(UID.of(trackedEntity.getUid()));
    return minimalTrackedEntity;
  }

  private Enrollment toMinimalEnrollment(Enrollment enrollment) {
    Enrollment minimal = new Enrollment();
    minimal.setEnrollment(enrollment.getEnrollment());
    return minimal;
  }

  private Relationship toMinimalRelationship(Relationship relationship) {
    Relationship minimal = new Relationship();
    minimal.setRelationship(relationship.getRelationship());
    return minimal;
  }

  private Event toMinimalEvent(Event event) {
    Event minimal = new Event();
    minimal.setEvent(event.getEvent());
    return minimal;
  }
}
