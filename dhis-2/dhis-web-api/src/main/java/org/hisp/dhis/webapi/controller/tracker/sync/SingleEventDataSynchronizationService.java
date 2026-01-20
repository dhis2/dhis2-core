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
import static org.hisp.dhis.webapi.controller.tracker.export.MappingErrors.ensureNoMappingErrors;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.DataSynchronizationWithPaging;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.event.EventMapper;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
public class SingleEventDataSynchronizationService
    extends TrackerDataSynchronizationWithPaging<Event> {
  private static final String PROCESS_NAME = "Single event programs data synchronization";
  private static final EventMapper EVENT_MAPPER = Mappers.getMapper(EventMapper.class);

  private final SingleEventService singleEventService;
  private final SystemSettingsService systemSettingsService;
  private final ProgramStageDataElementService programStageDataElementService;

  public SingleEventDataSynchronizationService(
      SingleEventService singleEventService,
      SystemSettingsService systemSettingsService,
      ProgramStageDataElementService programStageDataElementService,
      RestTemplate restTemplate,
      RenderService renderService) {
    super(renderService, restTemplate);
    this.singleEventService = singleEventService;
    this.systemSettingsService = systemSettingsService;
    this.programStageDataElementService = programStageDataElementService;
  }

  @Getter
  private static final class EventSynchronizationContext
      extends DataSynchronizationWithPaging.PagedDataSynchronisationContext {
    private final Map<String, Set<String>> skipSyncDataElementsByProgramStage;

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

    SynchronizationResult validationResult = validatePreconditions(settings, progress);
    if (validationResult != null) {
      return validationResult;
    }

    EventSynchronizationContext context = initializeContext(pageSize, progress, settings);

    if (context.hasNoObjectsToSynchronize()) {
      return endProcess(progress, "No events to synchronize");
    }

    boolean success = executeSynchronizationWithPaging(context, progress, settings);

    return success
        ? endProcess(progress, "Completed successfully")
        : failProcess(progress, "Page-level synchronization failed");
  }

  @Override
  public void updateEntitySyncTimeStamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events, Date syncTime) {
    List<String> eventUids = events.stream().map(event -> event.getEvent().getValue()).toList();
    singleEventService.updateEventsSyncTimestamp(eventUids, syncTime);
  }

  @Override
  public String getEntityName() {
    return "events";
  }

  @Override
  public String getProcessName() {
    return PROCESS_NAME;
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
    return singleEventService.countEvents(
        SingleEventOperationParams.builderForDataSync(skipChangedBefore, Map.of()).build());
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

    return !progress.isSkipCurrentStage();
  }

  private void synchronizePageSafely(
      int page, EventSynchronizationContext context, SystemSettings settings) {
    try {
      synchronizePage(page, context, settings);
    } catch (Exception ex) {
      log.error("Failed to synchronize page {}", page, ex);
      throw new RuntimeException(
          format("Page %d synchronization failed: %s", page, ex.getMessage()), ex);
    }
  }

  private void synchronizePage(
      int page, EventSynchronizationContext context, SystemSettings settings)
      throws ForbiddenException, BadRequestException, WebMessageException {
    List<SingleEvent> events = fetchEventsForPage(page, context);

    Map<Boolean, List<SingleEvent>> partitionedEvents = partitionEventsByDeletionStatus(events);
    List<SingleEvent> deletedEvents = partitionedEvents.get(true);
    List<SingleEvent> activeEvents = partitionedEvents.get(false);

    syncEventsByDeletionStatus(activeEvents, deletedEvents, context, settings);
  }

  private List<SingleEvent> fetchEventsForPage(int page, EventSynchronizationContext context)
      throws ForbiddenException, BadRequestException {
    return singleEventService
        .findEvents(
            SingleEventOperationParams.builderForDataSync(
                    context.getSkipChangedBefore(), context.getSkipSyncDataElementsByProgramStage())
                .build(),
            PageParams.of(page, context.getPageSize(), false))
        .getItems();
  }

  private Map<Boolean, List<SingleEvent>> partitionEventsByDeletionStatus(
      List<SingleEvent> events) {
    return events.stream().collect(Collectors.partitioningBy(SingleEvent::isDeleted));
  }

  private void syncEventsByDeletionStatus(
      List<SingleEvent> activeEvents,
      List<SingleEvent> deletedEvents,
      EventSynchronizationContext context,
      SystemSettings settings)
      throws WebMessageException {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    TrackerIdSchemeParams idSchemeParam =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.UID).build();
    MappingErrors errors = new MappingErrors(idSchemeParam);

    if (!activeEvents.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> activeEventDtos =
          activeEvents.stream().map(ev -> EVENT_MAPPER.map(idSchemeParam, errors, ev)).toList();
      ensureNoMappingErrors(errors);
      syncEntities(
          activeEventDtos, instance, settings, syncTime, TrackerImportStrategy.CREATE_AND_UPDATE);
    }

    if (!deletedEvents.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> deletedEventDtos =
          deletedEvents.stream().map(this::toMinimalEvent).toList();
      syncEntities(deletedEventDtos, instance, settings, syncTime, TrackerImportStrategy.DELETE);
    }
  }

  private org.hisp.dhis.webapi.controller.tracker.view.Event toMinimalEvent(
      SingleEvent singleEvent) {
    org.hisp.dhis.webapi.controller.tracker.view.Event minimalEvent =
        new org.hisp.dhis.webapi.controller.tracker.view.Event();
    minimalEvent.setEvent(UID.of(singleEvent.getUid()));
    return minimalEvent;
  }
}
