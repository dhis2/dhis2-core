/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.sync;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Events;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.synch.SystemInstance;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 * @author Jan Bernitt (job progress tracking refactoring)
 */
@Slf4j
@Component
@AllArgsConstructor
public class EventSynchronization implements DataSynchronizationWithPaging {
  private final EventService eventService;

  private final SystemSettingManager settings;

  private final RestTemplate restTemplate;

  private final RenderService renderService;

  private final ProgramStageDataElementService programStageDataElementService;

  @Getter
  private static final class EventSynchronisationContext extends PagedDataSynchronisationContext {
    private final Map<String, Set<String>> psdesWithSkipSyncTrue;

    public EventSynchronisationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize, Map.of());
    }

    public EventSynchronisationContext(
        Date skipChangedBefore,
        int objectsToSynchronize,
        SystemInstance instance,
        int pageSize,
        Map<String, Set<String>> psdesWithSkipSyncTrue) {
      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
      this.psdesWithSkipSyncTrue = psdesWithSkipSyncTrue;
    }
  }

  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    progress.startingProcess("Starting Event programs data synchronization job.");
    if (!SyncUtils.testServerAvailability(settings, restTemplate).isAvailable()) {
      String msg = "Event programs data synchronization failed. Remote server is unavailable.";
      progress.failedProcess(msg);
      return SynchronizationResult.failure(msg);
    }

    progress.startingStage("Counting anonymous events ready to be synchronised.");
    EventSynchronisationContext context =
        progress.runStage(
            new EventSynchronisationContext(null, pageSize),
            ctx ->
                "Events last changed before "
                    + ctx.getSkipChangedBefore()
                    + " will not be synchronized.",
            () -> createContext(pageSize));

    if (context.getObjectsToSynchronize() == 0) {
      String msg = "Event programs data synchronization skipped. No new or updated events found.";
      progress.completedProcess(msg);
      return SynchronizationResult.success(msg);
    }

    if (runSyncWithPaging(context, progress)) {
      progress.completedProcess(
          "SUCCESS! Event programs data sync was successfully done! It took ");
      return SynchronizationResult.success("Event programs data synchronization done.");
    }

    String msg =
        "Event programs data synchronization failed. Not all pages were synchronised successfully.";
    progress.failedProcess(msg);
    return SynchronizationResult.failure(msg);
  }

  private EventSynchronisationContext createContext(final int pageSize) {
    Date skipChangedBefore =
        settings.getDateSetting(SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE);
    int objectsToSynchronize =
        eventService.getAnonymousEventReadyForSynchronizationCount(skipChangedBefore);

    if (objectsToSynchronize != 0) {
      SystemInstance instance =
          SyncUtils.getRemoteInstanceWithSyncImportStrategy(settings, SyncEndpoint.EVENTS);

      return new EventSynchronisationContext(
          skipChangedBefore,
          objectsToSynchronize,
          instance,
          pageSize,
          programStageDataElementService
              .getProgramStageDataElementsWithSkipSynchronizationSetToTrue());
    }
    return new EventSynchronisationContext(skipChangedBefore, pageSize);
  }

  private boolean runSyncWithPaging(EventSynchronisationContext context, JobProgress progress) {
    String msg =
        context.getObjectsToSynchronize() + " anonymous Events to synchronize were found.\n";
    msg +=
        "Remote server URL for Event programs POST synchronization: "
            + context.getInstance().getUrl()
            + "\n";
    msg +=
        "Event programs data synchronization job has "
            + context.getPages()
            + " pages to synchronize. With page size: "
            + context.getPageSize();
    progress.startingStage(msg, context.getPages(), SKIP_ITEM);
    progress.runStage(
        IntStream.range(1, context.getPages() + 1).boxed(),
        page -> format("Synchronizing page %d with page size %d", page, context.getPageSize()),
        page -> synchronizePage(page, context));
    return !progress.isSkipCurrentStage();
  }

  protected void synchronizePage(int page, EventSynchronisationContext context) {
    Events events =
        eventService.getAnonymousEventsForSync(
            context.getPageSize(),
            context.getSkipChangedBefore(),
            context.getPsdesWithSkipSyncTrue());
    filterOutDataValuesMarkedWithSkipSynchronizationFlag(events);

    if (log.isDebugEnabled()) {
      log.debug("Events that are going to be synchronized are: " + events);
    }

    if (sendSyncRequest(events, context.getInstance())) {
      List<String> eventsUIDs =
          events.getEvents().stream().map(Event::getEvent).collect(Collectors.toList());
      log.info("The lastSynchronized flag of these Events will be updated: " + eventsUIDs);
      eventService.updateEventsSyncTimestamp(eventsUIDs, context.getStartTime());
    }
    throw new MetadataSyncServiceException(format("Page %d synchronisation failed.", page));
  }

  private void filterOutDataValuesMarkedWithSkipSynchronizationFlag(Events events) {
    for (Event event : events.getEvents()) {
      event.setDataValues(
          event.getDataValues().stream()
              .filter(dv -> !dv.isSkipSynchronization())
              .collect(Collectors.toSet()));
    }
  }

  private boolean sendSyncRequest(Events events, SystemInstance instance) {
    RequestCallback requestCallback =
        request -> {
          request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          request
              .getHeaders()
              .add(
                  SyncUtils.HEADER_AUTHORIZATION,
                  CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));
          renderService.toJson(request.getBody(), events);
        };

    return SyncUtils.sendSyncRequest(
        settings, restTemplate, requestCallback, instance, SyncEndpoint.EVENTS);
  }
}
