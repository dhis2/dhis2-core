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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.CodecUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Slf4j
@Component
public class EventSynchronization extends DataSynchronizationWithPaging {
  private final EventService eventService;

  private final SystemSettingManager systemSettingManager;

  private final RestTemplate restTemplate;

  private final RenderService renderService;

  private final ProgramStageDataElementService programStageDataElementService;

  private Date skipChangedBefore;

  private Map<String, Set<String>> psdesWithSkipSyncTrue;

  public EventSynchronization(
      EventService eventService,
      SystemSettingManager systemSettingManager,
      RestTemplate restTemplate,
      RenderService renderService,
      ProgramStageDataElementService programStageDataElementService) {
    checkNotNull(eventService);
    checkNotNull(systemSettingManager);
    checkNotNull(renderService);
    checkNotNull(programStageDataElementService);
    checkNotNull(restTemplate);

    this.eventService = eventService;
    this.systemSettingManager = systemSettingManager;
    this.restTemplate = restTemplate;
    this.renderService = renderService;
    this.programStageDataElementService = programStageDataElementService;
  }

  @Override
  public SynchronizationResult synchronizeData(final int pageSize) {
    if (!SyncUtils.testServerAvailability(systemSettingManager, restTemplate).isAvailable()) {
      return SynchronizationResult.newFailureResultWithMessage(
          "Event programs data synchronization failed. Remote server is unavailable.");
    }

    initializeSyncVariables(pageSize);

    if (objectsToSynchronize == 0) {
      log.info("Skipping synchronization, no new or updated events found");
      return SynchronizationResult.newSuccessResultWithMessage(
          "Event programs data synchronization skipped. No new or updated events found.");
    }

    runSyncWithPaging(pageSize);

    if (syncResult) {
      clock.logTime("SUCCESS! Event programs data sync was successfully done! It took ");
      return SynchronizationResult.newSuccessResultWithMessage(
          "Event programs data synchronization done. It took " + clock.getTime() + " ms.");
    }

    return SynchronizationResult.newFailureResultWithMessage(
        "Event programs data synchronization failed.");
  }

  private void initializeSyncVariables(final int pageSize) {
    clock =
        new Clock(log).startClock().logTime("Starting Event programs data synchronization job.");
    skipChangedBefore =
        systemSettingManager.getDateSetting(
            SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE);
    objectsToSynchronize =
        eventService.getAnonymousEventReadyForSynchronizationCount(skipChangedBefore);

    log.info("Events last changed before " + skipChangedBefore + " will not be synchronized.");

    if (objectsToSynchronize != 0) {
      instance =
          SyncUtils.getRemoteInstanceWithSyncImportStrategy(
              systemSettingManager, SyncEndpoint.EVENTS);
      // Have to use this as (int) Match.ceil doesn't work until I am
      // casting int to double
      pages = (objectsToSynchronize / pageSize) + ((objectsToSynchronize % pageSize == 0) ? 0 : 1);

      log.info(objectsToSynchronize + " anonymous Events to synchronize were found.");
      log.info("Remote server URL for Event programs POST synchronization: " + instance.getUrl());
      log.info(
          "Event programs data synchronization job has "
              + pages
              + " pages to synchronize. With page size: "
              + pageSize);

      psdesWithSkipSyncTrue =
          programStageDataElementService
              .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();
    }
  }

  protected void synchronizePage(int page, int pageSize) {
    Events events =
        eventService.getAnonymousEventsForSync(pageSize, skipChangedBefore, psdesWithSkipSyncTrue);
    filterOutDataValuesMarkedWithSkipSynchronizationFlag(events);
    log.info(String.format("Synchronizing page %d with page size %d", page, pageSize));

    if (log.isDebugEnabled()) {
      log.debug("Events that are going to be synchronized are: " + events);
    }

    if (sendSyncRequest(events)) {
      List<String> eventsUIDs =
          events.getEvents().stream().map(Event::getEvent).collect(Collectors.toList());
      log.info("The lastSynchronized flag of these Events will be updated: " + eventsUIDs);
      eventService.updateEventsSyncTimestamp(eventsUIDs, new Date(clock.getStartTime()));
    } else {
      syncResult = false;
    }
  }

  private void filterOutDataValuesMarkedWithSkipSynchronizationFlag(Events events) {
    for (Event event : events.getEvents()) {
      event.setDataValues(
          event.getDataValues().stream()
              .filter(dv -> !dv.isSkipSynchronization())
              .collect(Collectors.toSet()));
    }
  }

  private boolean sendSyncRequest(Events events) {
    final RequestCallback requestCallback =
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
        systemSettingManager, restTemplate, requestCallback, instance, SyncEndpoint.EVENTS);
  }
}
