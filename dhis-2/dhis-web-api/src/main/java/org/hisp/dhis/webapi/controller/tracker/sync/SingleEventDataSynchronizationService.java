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
import static org.hisp.dhis.dxf2.sync.SyncUtils.runSyncRequest;
import static org.hisp.dhis.dxf2.sync.SyncUtils.testServerAvailability;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.webapi.controller.tracker.export.event.EventMapper;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
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

  private final EventService eventService;
  private final SystemSettingsService systemSettingsService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final ProgramService programService;

  @Getter
  private static final class EventSynchronizationContext extends PagedDataSynchronisationContext {
    private final Map<String, Set<String>> skipSyncDataElementsByProgramStage;
    private final Program program;

    public EventSynchronizationContext(Date skipChangedBefore, int pageSize, Program program) {
      this(skipChangedBefore, 0, null, pageSize, Map.of(), program);
    }

    public EventSynchronizationContext(
        Date skipChangedBefore,
        long objectsToSynchronize,
        SystemInstance instance,
        int pageSize,
        Map<String, Set<String>> skipSyncDataElementsByProgramStage,
        Program program) {
      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
      this.skipSyncDataElementsByProgramStage = skipSyncDataElementsByProgramStage;
      this.program = program;
    }

    public boolean hasNoObjectsToSynchronize() {
      return getObjectsToSynchronize() == 0;
    }
  }

  @Override
  public SynchronizationResult synchronizeTrackerData(int pageSize, JobProgress progress) {
    return null;
  }

  @Override
  public SynchronizationResult synchronizeSingleEventData(
      int pageSize, JobProgress progress, @Nonnull String programUid) {
    progress.startingProcess(PROCESS_NAME);

    SystemSettings settings = systemSettingsService.getCurrentSettings();

    SynchronizationResult validationResult = validatePreconditions(settings, programUid, progress);
    if (validationResult != null) {
      return validationResult;
    }

    EventSynchronizationContext context =
        initializeContext(pageSize, progress, settings, programUid);

    if (context.hasNoObjectsToSynchronize()) {
      return endProcess(progress, "No events to synchronize");
    }

    boolean success = executeSynchronizationWithPaging(context, progress, settings);

    return success
        ? endProcess(progress, "Completed successfully")
        : failProcess(progress, "Page-level synchronization failed");
  }

  private EventSynchronizationContext initializeContext(
      int pageSize, JobProgress progress, SystemSettings settings, String programUid) {
    return progress.runStage(
        new EventSynchronizationContext(null, pageSize, null),
        ctx -> format("Single events changed before %s will not sync", ctx.getSkipChangedBefore()),
        () -> createContext(pageSize, settings, programUid));
  }

  private SynchronizationResult validatePreconditions(
      SystemSettings settings, String programUid, JobProgress progress) {
    if (!testServerAvailability(settings, restTemplate).isAvailable()) {
      return failProcess(progress, "Remote server unavailable");
    }

    Program program = programService.getProgram(programUid);
    if (program == null) {
      return failProcess(progress, "Program %s not found".formatted(programUid));
    }

    if (program.getProgramType() != ProgramType.WITHOUT_REGISTRATION) {
      return failProcess(
          progress, "Program %s must be of type WITHOUT_REGISTRATION".formatted(programUid));
    }

    return null;
  }

  private EventSynchronizationContext createContext(
      int pageSize, SystemSettings settings, String programUid)
      throws ForbiddenException, BadRequestException {
    Program program = programService.getProgram(programUid);
    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();

    long eventCount = countEventsForSynchronization(program, skipChangedBefore);

    if (eventCount == 0) {
      return new EventSynchronizationContext(skipChangedBefore, pageSize, program);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);
    Map<String, Set<String>> skipSyncProgramStageDataElements =
        getSkipSyncProgramStageDataElements(program);

    return new EventSynchronizationContext(
        skipChangedBefore,
        eventCount,
        instance,
        pageSize,
        skipSyncProgramStageDataElements,
        program);
  }

  private long countEventsForSynchronization(Program program, Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    return eventService.countEvents(
        EventOperationParams.builder()
            .program(program)
            .skipChangedBefore(skipChangedBefore)
            .includeDeleted(true)
            .synchronizationQuery(true)
            .build());
  }

  private Map<String, Set<String>> getSkipSyncProgramStageDataElements(Program program) {
    return programStageDataElementService
        .getProgramStageDataElementsWithSkipSynchronizationSetToTrue(program);
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
      throws ForbiddenException, BadRequestException {
    List<Event> events = fetchEventsForPage(page, context);

    Map<Boolean, List<Event>> partitionedEvents = partitionEventsByDeletionStatus(events);
    List<Event> deletedEvents = partitionedEvents.get(true);
    List<Event> activeEvents = partitionedEvents.get(false);

    syncEventsByDeletionStatus(activeEvents, deletedEvents, context, settings);
  }

  private List<Event> fetchEventsForPage(int page, EventSynchronizationContext context)
      throws ForbiddenException, BadRequestException {
    return eventService
        .findEvents(
            EventOperationParams.builder()
                .programType(ProgramType.WITHOUT_REGISTRATION)
                .program(context.getProgram())
                .skipChangedBefore(context.getSkipChangedBefore())
                .synchronizationQuery(true)
                .includeDeleted(true)
                .withSkipSyncDataElements(context.getSkipSyncDataElementsByProgramStage())
                .build(),
            PageParams.of(page, context.getPageSize(), false))
        .getItems();
  }

  private Map<Boolean, List<Event>> partitionEventsByDeletionStatus(List<Event> events) {
    return events.stream().collect(Collectors.partitioningBy(Event::isDeleted));
  }

  private void syncEventsByDeletionStatus(
      List<Event> activeEvents,
      List<Event> deletedEvents,
      EventSynchronizationContext context,
      SystemSettings settings) {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    if (!activeEvents.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> activeEventDtos =
          activeEvents.stream().map(EVENT_MAPPER::map).toList();
      syncEvents(
          activeEventDtos, instance, settings, syncTime, TrackerImportStrategy.CREATE_AND_UPDATE);
    }

    if (!deletedEvents.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> deletedEventDtos =
          deletedEvents.stream().map(this::toMinimalEvent).toList();
      syncEvents(deletedEventDtos, instance, settings, syncTime, TrackerImportStrategy.DELETE);
    }
  }

  private void syncEvents(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events,
      SystemInstance instance,
      SystemSettings settings,
      Date syncTime,
      TrackerImportStrategy importStrategy) {
    String url = instance.getUrl() + "?importStrategy=" + importStrategy;

    ImportSummary summary = sendTrackerRequest(events, instance, settings, url);

    if (summary == null || summary.getStatus() != ImportStatus.SUCCESS) {
      throw new MetadataSyncServiceException(
          format("Single Event sync failed for importStrategy=%s", importStrategy));
    }

    log.info(
        "Single Event sync successful for importStrategy={}. Events count: {}",
        importStrategy,
        events.size());

    updateEventsSyncTimestamp(events, syncTime);
  }

  private ImportSummary sendTrackerRequest(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events,
      SystemInstance instance,
      SystemSettings settings,
      String url) {
    RequestCallback requestCallback = createRequestCallback(events, instance);

    Optional<WebMessageResponse> response =
        runSyncRequest(
            restTemplate,
            requestCallback,
            SyncEndpoint.TRACKER_IMPORT.getKlass(),
            url,
            settings.getSyncMaxAttempts());

    return response.map(ImportSummary.class::cast).orElse(null);
  }

  private RequestCallback createRequestCallback(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events, SystemInstance instance) {
    return request -> {
      request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      request
          .getHeaders()
          .set(
              SyncUtils.HEADER_AUTHORIZATION,
              CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));

      renderService.toJson(request.getBody(), Map.of("events", events));
    };
  }

  private void updateEventsSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events, Date syncTime) {
    List<String> eventUids = events.stream().map(event -> event.getEvent().getValue()).toList();
    eventService.updateEventsSyncTimestamp(eventUids, syncTime);
  }

  private SynchronizationResult endProcess(JobProgress progress, String message) {
    String fullMessage = format("%s %s", PROCESS_NAME, message);
    progress.completedProcess(fullMessage);
    return SynchronizationResult.success(fullMessage);
  }

  private SynchronizationResult failProcess(JobProgress progress, String reason) {
    String fullMessage = format("%s failed. %s", PROCESS_NAME, reason);
    progress.failedProcess(fullMessage);
    return SynchronizationResult.failure(fullMessage);
  }

  private org.hisp.dhis.webapi.controller.tracker.view.Event toMinimalEvent(Event event) {
    org.hisp.dhis.webapi.controller.tracker.view.Event minimalEvent =
        new org.hisp.dhis.webapi.controller.tracker.view.Event();
    minimalEvent.setEvent(UID.of(event.getUid()));
    return minimalEvent;
  }
}
