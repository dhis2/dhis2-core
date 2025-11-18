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

  private static final String PROCESS_NAME = "Event programs data synchronization";
  private static final EventMapper EVENT_MAPPER = Mappers.getMapper(EventMapper.class);

  private final EventService eventService;
  private final SystemSettingsService systemSettingsService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final ProgramService programService;

  @Getter
  private static final class EventSynchronizationContext extends PagedDataSynchronisationContext {
    private final Map<String, Set<String>> skipSyncPSDEs;

    public EventSynchronizationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize, Map.of());
    }

    public EventSynchronizationContext(
        Date skipChangedBefore,
        long objectsToSynchronize,
        SystemInstance instance,
        int pageSize,
        Map<String, Set<String>> skipSyncPSDEs) {

      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
      this.skipSyncPSDEs = skipSyncPSDEs;
    }

    public boolean hasNoObjectsToSynchronize() {
      return getObjectsToSynchronize() == 0;
    }
  }

  @Override
  public SynchronizationResult synchronizeTrackerData(
      int pageSize, JobProgress progress, String programUid) {

    progress.startingProcess("Starting " + PROCESS_NAME);

    SystemSettings settings = systemSettingsService.getCurrentSettings();

    if (!testServerAvailability(settings, restTemplate).isAvailable()) {
      return failProcess(progress, PROCESS_NAME + " failed. Remote server unavailable.");
    }

    EventSynchronizationContext context =
        initializeContext(pageSize, progress, settings, programUid);

    if (context.hasNoObjectsToSynchronize()) {
      return endProcess(progress, PROCESS_NAME + " ended. No events to synchronize.");
    }

    boolean success = executeSynchronizationWithPaging(context, progress, settings);

    return success
        ? endProcess(progress, PROCESS_NAME + " completed successfully.")
        : failProcess(progress, PROCESS_NAME + " failed. Page-level synchronization errors.");
  }

  private EventSynchronizationContext initializeContext(
      int pageSize, JobProgress progress, SystemSettings settings, String programUid) {

    return progress.runStage(
        new EventSynchronizationContext(null, pageSize),
        ctx -> "Events changed before " + ctx.getSkipChangedBefore() + " will not sync.",
        () -> createContext(pageSize, settings, programUid));
  }

  private EventSynchronizationContext createContext(
      int pageSize, SystemSettings settings, String programUid)
      throws ForbiddenException, BadRequestException {

    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();
    Program program = programService.getProgram(programUid);

    long count =
        eventService.countEvents(
            EventOperationParams.builder()
                .programType(ProgramType.WITHOUT_REGISTRATION)
                .program(program)
                .skipChangedBefore(skipChangedBefore)
                .includeDeleted(true)
                .synchronizationQuery(true)
                .build());

    if (count == 0) {
      return new EventSynchronizationContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);

    Map<String, Set<String>> skipSyncPSDEs =
        programStageDataElementService.getProgramStageDataElementsWithSkipSynchronizationSetToTrue(
            program);

    return new EventSynchronizationContext(
        skipChangedBefore, count, instance, pageSize, skipSyncPSDEs);
  }

  private boolean executeSynchronizationWithPaging(
      EventSynchronizationContext ctx, JobProgress progress, SystemSettings settings) {

    progress.startingStage(
        format(
            "Found %d events. Remote: %s. Pages: %d (size %d)",
            ctx.getObjectsToSynchronize(),
            ctx.getInstance().getUrl(),
            ctx.getPages(),
            ctx.getPageSize()),
        ctx.getPages(),
        SKIP_ITEM);

    progress.runStage(
        IntStream.range(1, ctx.getPages() + 1).boxed(),
        page -> format("Syncing page %d (size %d)", page, ctx.getPageSize()),
        page -> {
          try {
            synchronizePage(page, ctx, settings);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        });

    return !progress.isSkipCurrentStage();
  }

  private void synchronizePage(int page, EventSynchronizationContext ctx, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    Date skipChangedBefore = ctx.getSkipChangedBefore();
    Map<String, Set<String>> skipSyncPSDEs = ctx.getSkipSyncPSDEs();
    SystemInstance instance = ctx.getInstance();
    Date syncStart = ctx.getStartTime();

    List<Event> events =
        eventService.findEvents(
            EventOperationParams.builder()
                .programType(ProgramType.WITHOUT_REGISTRATION)
                .skipChangedBefore(skipChangedBefore)
                .synchronizationQuery(true)
                .includeDeleted(true)
                .build(),
            skipSyncPSDEs,
            PageParams.of(page, ctx.getPageSize(), false));

    Map<Boolean, List<Event>> partitioned =
        events.stream().collect(Collectors.partitioningBy(Event::isDeleted));

    List<Event> deleted = partitioned.get(true);
    List<Event> active = partitioned.get(false);

    if (!active.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> dtoEvents =
          events.stream().map(EVENT_MAPPER::map).toList();
      syncEvents(dtoEvents, instance, settings, syncStart, false);
    }

    if (!deleted.isEmpty()) {
      syncEvents(
          deleted.stream().map(this::toMinimalEvent).toList(), instance, settings, syncStart, true);
    }
  }

  private void syncEvents(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events,
      SystemInstance instance,
      SystemSettings settings,
      Date syncTime,
      boolean isDelete) {
    String url = instance.getUrl();

    if (isDelete) {
      url += "?importStrategy=DELETE";
    }

    ImportSummary summary = sendTrackerRequest(events, instance, settings, url);

    if (summary == null || summary.getStatus() != ImportStatus.SUCCESS) {
      throw new MetadataSyncServiceException(
          "Event sync failed (importStrategy=" + (isDelete ? "DELETE" : "CREATE_AND_UPDATE") + ")");
    } else {
      log.info(
          "Event sync successful for importStrategy={}. Events count: {}",
          isDelete ? "DELETE" : "CREATE_AND_UPDATE",
          events.size());
    }

    updateEventsSyncTimestamp(events, syncTime);
  }

  private ImportSummary sendTrackerRequest(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events,
      SystemInstance instance,
      SystemSettings settings,
      String url) {

    RequestCallback requestCallback =
        req -> {
          req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          req.getHeaders()
              .set(
                  SyncUtils.HEADER_AUTHORIZATION,
                  CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));

          renderService.toJson(req.getBody(), Map.of("events", events));
        };

    Optional<WebMessageResponse> response =
        runSyncRequest(
            restTemplate,
            requestCallback,
            SyncEndpoint.TRACKER_IMPORT.getKlass(),
            url,
            settings.getSyncMaxAttempts());

    ImportSummary summary = null;
    if (response.isPresent()) {
      summary = (ImportSummary) response.get();
    }
    return summary;
  }

  private void updateEventsSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.Event> events, Date syncTime) {
    List<String> uids = events.stream().map(event -> event.getEvent().getValue()).toList();
    eventService.updateEventsSyncTimestamp(uids, syncTime);
  }

  private SynchronizationResult endProcess(JobProgress progress, String msg) {
    progress.completedProcess(msg);
    return SynchronizationResult.success(msg);
  }

  private SynchronizationResult failProcess(JobProgress progress, String msg) {
    progress.failedProcess(msg);
    return SynchronizationResult.failure(msg);
  }

  private org.hisp.dhis.webapi.controller.tracker.view.Event toMinimalEvent(Event ev) {
    org.hisp.dhis.webapi.controller.tracker.view.Event e =
        new org.hisp.dhis.webapi.controller.tracker.view.Event();
    e.setEvent(UID.of(ev.getUid()));
    return e;
  }
}
