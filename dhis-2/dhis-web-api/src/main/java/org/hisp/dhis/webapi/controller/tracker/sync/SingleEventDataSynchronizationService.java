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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.event.EventFields;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.webapi.controller.tracker.export.event.EventMapper;
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
public class SingleEventDataSynchronizationService extends TrackerDataSynchronizationWithPaging {
  private static final String PROCESS_NAME = "Single event programs data synchronization";
  private static final EventMapper EVENT_MAPPER = Mappers.getMapper(EventMapper.class);

  private record DeleteSyncResult(Set<UID> syncedEventUids, Set<UID> blockingFailedChildUids) {}

  private final EventService eventService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final SystemSettingsService systemSettingsService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;

  @Getter
  private static final class EventSynchronizationContext extends PagedDataSynchronisationContext {
    private final Map<String, Set<String>> skipSyncDataElementsByProgramStage;
    // Distinct event uids fetched across all pages this run, so the run's completion can report
    // how many events in the backlog were never attempted at all (see
    // executeSynchronizationWithPaging), e.g. because repeatedly failing events kept occupying
    // page 1 slots and crowded out events further back in the queue.
    private final Set<UID> attemptedEventUids = new HashSet<>();

    public EventSynchronizationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize, Map.of());
    }

    public EventSynchronizationContext(
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

    EventSynchronizationContext context = initializeContext(pageSize, progress, settings);

    if (context.hasNoObjectsToSynchronize()) {
      return endProcess(progress, "No events to synchronize", PROCESS_NAME);
    }

    boolean success = executeSynchronizationWithPaging(context, progress, settings);

    return success
        ? endProcess(progress, "Completed successfully", PROCESS_NAME)
        : failProcess(progress, "Page-level synchronization failed", PROCESS_NAME);
  }

  private EventSynchronizationContext initializeContext(
      int pageSize, JobProgress progress, SystemSettings settings) {
    return progress.runStage(
        new EventSynchronizationContext(null, pageSize),
        ctx -> format("Single events changed before %s will not sync", ctx.getSkipChangedBefore()),
        () -> createContext(pageSize, settings));
  }

  private EventSynchronizationContext createContext(int pageSize, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();

    long eventCount = countEventsForSynchronization(skipChangedBefore);

    if (eventCount == 0) {
      return new EventSynchronizationContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);
    Map<String, Set<String>> skipSyncProgramStageDataElements =
        getSkipSyncProgramStageDataElements();

    return new EventSynchronizationContext(
        skipChangedBefore, eventCount, instance, pageSize, skipSyncProgramStageDataElements);
  }

  private long countEventsForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    return eventService.countEvents(
        EventOperationParams.builder()
            .programType(ProgramType.WITHOUT_REGISTRATION)
            .skipChangedBefore(skipChangedBefore)
            .includeDeleted(true)
            .synchronizationQuery(true)
            .build());
  }

  private Map<String, Set<String>> getSkipSyncProgramStageDataElements() {
    return programStageDataElementService
        .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();
  }

  private boolean executeSynchronizationWithPaging(
      EventSynchronizationContext context, JobProgress progress, SystemSettings settings) {
    String stageDescription =
        format(
            "Found %d single events. Remote: %s. Pages: %d (size %d)",
            context.getObjectsToSynchronize(),
            context.getInstance().getUrl(),
            context.getPages(),
            context.getPageSize());

    progress.startingStage(stageDescription, context.getPages(), SKIP_ITEM);

    progress.runStage(
        IntStream.range(1, context.getPages() + 1).boxed(),
        page -> format("Syncing page %d (size %d)", page, context.getPageSize()),
        page -> synchronizePageSafely(page, context, settings));

    long unprocessed = context.getObjectsToSynchronize() - context.getAttemptedEventUids().size();
    if (unprocessed > 0) {
      log.info(
          "Single event data synchronization: {} of {} events were never attempted this run",
          unprocessed,
          context.getObjectsToSynchronize());
    }

    return !progress.isSkipCurrentStage();
  }

  private void synchronizePageSafely(
      int page, EventSynchronizationContext context, SystemSettings settings) {
    try {
      synchronizePage(context, settings);
    } catch (Exception ex) {
      log.error("Failed to synchronize page {}", page, ex);
      throw new RuntimeException(
          format("Page %d synchronization failed: %s", page, ex.getMessage()), ex);
    }
  }

  private void synchronizePage(EventSynchronizationContext context, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    List<Event> events = fetchEventsForPage(context);

    Map<Boolean, List<Event>> partitionedEvents = partitionEventsByDeletionStatus(events);
    List<Event> deletedEvents = partitionedEvents.get(true);
    List<Event> activeEvents = partitionedEvents.get(false);

    syncEventsByDeletionStatus(activeEvents, deletedEvents, context, settings);
  }

  private List<Event> fetchEventsForPage(EventSynchronizationContext context)
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .programType(ProgramType.WITHOUT_REGISTRATION)
            .skipChangedBefore(context.getSkipChangedBefore())
            .synchronizationQuery(true)
            .includeDeleted(true)
            .fields(EventFields.all())
            .withSkipSyncDataElements(context.getSkipSyncDataElementsByProgramStage())
            .build();
    // Always query page 1: events synced by a prior iteration drop out of the synchronization
    // filter on their own, so the next unsynced batch is always at the front of the result set.
    // Using an offset here would skip events, since the filtered set shrinks between iterations.
    // Events that failed to sync are not excluded, so they are retried on every subsequent
    // page 1 fetch this run, potentially crowding out backlog events that never get attempted at
    // all.
    return eventService
        .findEvents(params, PageParams.of(1, context.getPageSize(), false))
        .getItems();
  }

  private Map<Boolean, List<Event>> partitionEventsByDeletionStatus(List<Event> events) {
    return events.stream().collect(Collectors.partitioningBy(Event::isDeleted));
  }

  private record SplitActiveEvents(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> activeEvents,
      Map<UID, Relationship> deletedRelationshipsByUid,
      Map<UID, Set<UID>> deletedChildUidsByEvent) {}

  private void syncEventsByDeletionStatus(
      List<Event> active,
      List<Event> deleted,
      EventSynchronizationContext context,
      SystemSettings settings) {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    active.forEach(event -> context.getAttemptedEventUids().add(UID.of(event.getUid())));
    deleted.forEach(event -> context.getAttemptedEventUids().add(UID.of(event.getUid())));

    SplitActiveEvents splitActiveEvents = splitActiveEvents(active);
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> deletedEventDtos =
        deleted.stream().map(this::toMinimalEvent).toList();

    DeleteSyncResult deleteResult =
        syncDeletedIfNeeded(deletedEventDtos, splitActiveEvents, instance, settings);

    Set<UID> activeSyncCandidateUids =
        splitActiveEvents.activeEvents().isEmpty()
            ? Set.of()
            : syncActive(splitActiveEvents.activeEvents(), instance, settings);

    Set<UID> syncedActiveEventUids =
        resolveSyncedActiveEventUids(
            activeSyncCandidateUids, splitActiveEvents.deletedChildUidsByEvent(), deleteResult);
    Set<UID> syncedDeletedEventUids =
        deleteResult == null ? Set.of() : deleteResult.syncedEventUids();

    stampSyncTimestamp(deletedEventDtos, syncedDeletedEventUids, syncTime);
    stampSyncTimestamp(splitActiveEvents.activeEvents(), syncedActiveEventUids, syncTime);
  }

  private SplitActiveEvents splitActiveEvents(List<Event> active) {
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> activeEvents =
        active.stream().map(EVENT_MAPPER::map).toList();

    Map<UID, Relationship> deletedRelationshipsByUid = new LinkedHashMap<>();
    // Which deleted relationship UIDs belong to which active event, so a blocking (non idempotent)
    // failure to delete one of them can withhold that event's sync timestamp later.
    Map<UID, Set<UID>> deletedChildUidsByEvent = new HashMap<>();
    for (org.hisp.dhis.webapi.controller.tracker.view.Event event : activeEvents) {
      Set<UID> ownedDeletedChildUids =
          deletedChildUidsByEvent.computeIfAbsent(event.getEvent(), k -> new HashSet<>());
      event.setRelationships(
          stripDeletedRelationships(
              event.getRelationships(), deletedRelationshipsByUid, ownedDeletedChildUids));
    }

    return new SplitActiveEvents(activeEvents, deletedRelationshipsByUid, deletedChildUidsByEvent);
  }

  private DeleteSyncResult syncDeletedIfNeeded(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> deletedEventDtos,
      SplitActiveEvents prepared,
      SystemInstance instance,
      SystemSettings settings) {
    if (deletedEventDtos.isEmpty() && prepared.deletedRelationshipsByUid().isEmpty()) {
      return null;
    }
    return syncDeleted(
        deletedEventDtos, prepared.deletedRelationshipsByUid().values(), instance, settings);
  }

  private Set<UID> resolveSyncedActiveEventUids(
      Set<UID> activeSyncCandidateUids,
      Map<UID, Set<UID>> deletedChildUidsByEvent,
      DeleteSyncResult deleteResult) {
    Set<UID> blockingFailedChildUids =
        deleteResult == null ? Set.of() : deleteResult.blockingFailedChildUids();
    return activeSyncCandidateUids.stream()
        .filter(
            eventUid ->
                Collections.disjoint(
                    deletedChildUidsByEvent.getOrDefault(eventUid, Set.of()),
                    blockingFailedChildUids))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private void stampSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> candidates,
      Set<UID> syncedUids,
      Date syncTime) {
    if (syncedUids.isEmpty()) {
      return;
    }
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> syncedEvents =
        candidates.stream().filter(event -> syncedUids.contains(event.getEvent())).toList();
    updateEventsSyncTimestamp(syncedEvents, syncTime);
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

  private DeleteSyncResult syncDeleted(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> deletedEventDtos,
      Collection<Relationship> deletedRelationships,
      SystemInstance instance,
      SystemSettings settings) {
    String url = instance.getUrl() + "?importStrategy=" + DELETE + "&async=false&atomicMode=OBJECT";

    Map<String, List<?>> payload =
        Map.of(
            "events",
            deletedEventDtos,
            "relationships",
            deletedRelationships.stream().map(this::toMinimalRelationship).toList());

    ImportReport report =
        sendTrackerRequest(restTemplate, renderService, payload, instance, settings, url);

    Set<UID> syncedEventUids = alreadyDeletedOrSucceededUids(report, TrackerType.EVENT);
    Set<UID> syncedRelationshipUids =
        alreadyDeletedOrSucceededUids(report, TrackerType.RELATIONSHIP);
    List<org.hisp.dhis.webapi.controller.tracker.view.Event> syncedEvents =
        deletedEventDtos.stream()
            .filter(event -> syncedEventUids.contains(event.getEvent()))
            .toList();

    Set<UID> failedEventUids = blockingFailedUids(report, TrackerType.EVENT);
    Set<UID> failedRelationshipUids = blockingFailedUids(report, TrackerType.RELATIONSHIP);

    log.info(
        "Single Event delete sync: events={}/{} synced{}, relationships={}/{} synced{}",
        syncedEvents.size(),
        deletedEventDtos.size(),
        formatFailedUids(failedEventUids),
        syncedRelationshipUids.size(),
        deletedRelationships.size(),
        formatFailedUids(failedRelationshipUids));

    return new DeleteSyncResult(syncedEventUids, failedRelationshipUids);
  }

  private Set<UID> syncActive(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events,
      SystemInstance instance,
      SystemSettings settings) {
    String url =
        instance.getUrl()
            + "?importStrategy="
            + CREATE_AND_UPDATE
            + "&async=false&atomicMode=OBJECT";

    ImportReport report =
        sendTrackerRequest(
            restTemplate, renderService, Map.of("events", events), instance, settings, url);

    Set<UID> syncedEventUids = getEventsWithAllRelationshipsSynchronized(report, events);

    List<org.hisp.dhis.webapi.controller.tracker.view.Event> syncedEvents =
        events.stream().filter(event -> syncedEventUids.contains(event.getEvent())).toList();

    Set<UID> relationshipUids = getAllRelationshipUids(events);
    Set<UID> failedRelationshipUids = failedUids(report, TrackerType.RELATIONSHIP);

    log.info(
        "Single Event create/update sync: events={}/{} synced, relationships={}/{} synced{}",
        syncedEvents.size(),
        events.size(),
        successfullyProcessedUids(report, TrackerType.RELATIONSHIP).size(),
        relationshipUids.size(),
        formatFailedUids(failedRelationshipUids));

    return syncedEventUids;
  }

  private Set<UID> getAllRelationshipUids(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events) {
    // Deduplicate by UID because a relationship is attached to every entity that is one of its
    // from/to parties, so the same relationship can appear under two different events.
    Set<UID> relationshipUids = new HashSet<>();
    for (org.hisp.dhis.webapi.controller.tracker.view.Event event : events) {
      event.getRelationships().forEach(r -> relationshipUids.add(r.getRelationship()));
    }
    return relationshipUids;
  }

  private Set<UID> getEventsWithAllRelationshipsSynchronized(
      ImportReport report, List<org.hisp.dhis.webapi.controller.tracker.view.Event> events) {
    Set<UID> succeededUids = successfullyProcessedUids(report, TrackerType.EVENT);
    if (succeededUids.isEmpty()) {
      return succeededUids;
    }

    Set<UID> failedRelationships = failedUids(report, TrackerType.RELATIONSHIP);
    if (failedRelationships.isEmpty()) {
      return succeededUids;
    }

    return events.stream()
        .filter(event -> succeededUids.contains(event.getEvent()))
        .filter(event -> !hasFailedRelationship(event.getRelationships(), failedRelationships))
        .map(org.hisp.dhis.webapi.controller.tracker.view.Event::getEvent)
        .collect(Collectors.toCollection(HashSet::new));
  }

  private boolean hasFailedRelationship(
      List<Relationship> relationships, Set<UID> failedRelationships) {
    return relationships.stream().anyMatch(r -> failedRelationships.contains(r.getRelationship()));
  }

  private void updateEventsSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events, Date syncTime) {
    List<String> eventUids =
        events.stream()
            .map(org.hisp.dhis.webapi.controller.tracker.view.Event::getEvent)
            .map(UID::getValue)
            .toList();
    eventService.updateEventsSyncTimestamp(eventUids, syncTime);
  }

  private org.hisp.dhis.webapi.controller.tracker.view.Event toMinimalEvent(Event event) {
    org.hisp.dhis.webapi.controller.tracker.view.Event minimalEvent =
        new org.hisp.dhis.webapi.controller.tracker.view.Event();
    minimalEvent.setEvent(UID.of(event.getUid()));
    return minimalEvent;
  }

  private Relationship toMinimalRelationship(Relationship relationship) {
    Relationship minimal = new Relationship();
    minimal.setRelationship(relationship.getRelationship());
    return minimal;
  }
}
