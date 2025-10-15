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
package org.hisp.dhis.dxf2.sync;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.events.Event;
import org.hisp.dhis.dxf2.events.Events;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SingleEventDataSynchronizationService implements DataSynchronizationWithPaging {
  private static final String PROCESS_NAME = "Event programs data synchronization";

  private final EventService eventService;
  private final SystemSettingsService systemSettingsService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final UserService userService;
  private final SyncUtils syncUtils;

  @Getter
  private static final class EventSynchronizationContext extends PagedDataSynchronisationContext {
    private final Map<String, Set<String>> psdesWithSkipSyncTrue;

    public EventSynchronizationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize, Map.of());
    }

    public EventSynchronizationContext(
        Date skipChangedBefore,
        int objectsToSynchronize,
        SystemInstance instance,
        int pageSize,
        Map<String, Set<String>> psdesWithSkipSyncTrue) {
      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
      this.psdesWithSkipSyncTrue = psdesWithSkipSyncTrue;
    }

    public boolean hasNoObjectsToSynchronize() {
      return getObjectsToSynchronize() == 0;
    }
  }

  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    progress.startingProcess("Starting " + PROCESS_NAME);

    SystemSettings systemSettings = systemSettingsService.getCurrentSettings();

    if (!isRemoteServerAvailable(systemSettings)) {
      return failProcess(progress, PROCESS_NAME + " failed. Remote server is unavailable.");
    }

    EventSynchronizationContext context =
        initializeSynchronizationContext(pageSize, progress, systemSettings);
    if (context.hasNoObjectsToSynchronize()) {
      return skipProcess(progress, PROCESS_NAME + " skipped. No new or updated events found.");
    }

    boolean success = executeSynchronizationWithPaging(context, progress, systemSettings);

    return success
        ? success(progress, PROCESS_NAME + " completed successfully.")
        : failProcess(
            progress, PROCESS_NAME + " failed. Not all pages were synchronized successfully.");
  }

  private boolean isRemoteServerAvailable(SystemSettings systemSettings) {
    return syncUtils.testServerAvailability(systemSettings, restTemplate).isAvailable();
  }

  private EventSynchronizationContext initializeSynchronizationContext(
      int pageSize, JobProgress progress, SystemSettings systemSettings) {
    return progress.runStage(
        new EventSynchronizationContext(null, pageSize),
        ctx ->
            "Events last changed before "
                + ctx.getSkipChangedBefore()
                + " will not be synchronized.",
        () -> createSynchronizationContext(pageSize, systemSettings));
  }

  private EventSynchronizationContext createSynchronizationContext(
      int pageSize, SystemSettings systemSettings) throws ForbiddenException, BadRequestException {
    ensureAuthentication(systemSettings);

    Date skipChangedBefore = systemSettings.getSyncSkipSyncForDataChangedBefore();
    int objectsToSynchronize =
        eventService.findEvents(EventOperationParams.builder().skipChangedBefore(skipChangedBefore).build()).size();

    if (objectsToSynchronize == 0) {
      return new EventSynchronizationContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance =
        SyncUtils.getRemoteInstanceWithSyncImportStrategy(systemSettings, SyncEndpoint.EVENTS);

    Map<String, Set<String>> skipSyncElements =
        programStageDataElementService
            .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();

    return new EventSynchronizationContext(
        skipChangedBefore, objectsToSynchronize, instance, pageSize, skipSyncElements);
  }

  /** Ensures a valid {@link User} is available during event synchronization. */
  private void ensureAuthentication(SystemSettings systemSettings) {
      CurrentUserUtil.getCurrentUserDetails();
  }

  private void initializeSyncUser(SystemSettings systemSettings) {
    log.info("CurrentUser is null, initializing remote sync user for EVENT_PROGRAMS_DATA_SYNC");

    String remoteUsername = systemSettings.getRemoteInstanceUsername();
    User user = userService.getUserByUsername(remoteUsername);
    UserDetails userDetails = userService.createUserDetails(user);

    var authentication =
        new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());

    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  private boolean executeSynchronizationWithPaging(
      EventSynchronizationContext context, JobProgress progress, SystemSettings systemSettings) {
    String message =
        format(
            "Found %d anonymous Events to synchronize.%nRemote server: %s%nProcessing %d pages with size %d",
            context.getObjectsToSynchronize(),
            context.getInstance().getUrl(),
            context.getPages(),
            context.getPageSize());

    progress.startingStage(message, context.getPages(), SKIP_ITEM);

    progress.runStage(
        IntStream.range(1, context.getPages() + 1).boxed(),
        page -> format("Synchronizing page %d with size %d", page, context.getPageSize()),
        page -> synchronizePage(page, context, systemSettings));

    return !progress.isSkipCurrentStage();
  }

  protected void synchronizePage(
      int page, EventSynchronizationContext context, SystemSettings systemSettings) {
    Events events =
        eventService.getAnonymousEventsForSync(
            context.getPageSize(),
            context.getSkipChangedBefore(),
            context.getPsdesWithSkipSyncTrue());

    filterDataValuesWithSkipSyncFlag(events);

    log.debug("Events to be synchronized: {}", events);

    if (sendSynchronizationRequest(events, context.getInstance(), systemSettings)) {
      updateEventsSyncTimestamp(events, context.getStartTime());
    } else {
      throw new MetadataSyncServiceException(format("Page %d synchronization failed", page));
    }
  }

  private void filterDataValuesWithSkipSyncFlag(Events events) {
    events
        .getEvents()
        .forEach(
            event ->
                event.setDataValues(
                    event.getDataValues().stream()
                        .filter(dataValue -> !dataValue.isSkipSynchronization())
                        .collect(Collectors.toSet())));
  }

  private boolean sendSynchronizationRequest(
      Events events, SystemInstance instance, SystemSettings systemSettings) {
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

    return syncUtils.sendSyncRequest(
        systemSettings, restTemplate, requestCallback, instance, SyncEndpoint.EVENTS);
  }

  private void updateEventsSyncTimestamp(Events events, Date syncTime) {
    List<String> eventUids =
        events.getEvents().stream().map(Event::getEvent).collect(Collectors.toList());

    log.info("Updating lastSynchronized flag for events: {}", eventUids);
    eventService.updateEventsSyncTimestamp(eventUids, syncTime);
  }

  private SynchronizationResult success(JobProgress progress, String message) {
    progress.completedProcess(message);
    return SynchronizationResult.success(message);
  }

  private SynchronizationResult skipProcess(JobProgress progress, String message) {
    progress.completedProcess(message);
    return SynchronizationResult.success(message);
  }

  private SynchronizationResult failProcess(JobProgress progress, String message) {
    progress.failedProcess(message);
    return SynchronizationResult.failure(message);
  }
}
