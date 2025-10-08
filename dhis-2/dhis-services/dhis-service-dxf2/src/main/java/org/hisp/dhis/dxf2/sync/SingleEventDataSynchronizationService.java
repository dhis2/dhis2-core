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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.events.Event;
import org.hisp.dhis.dxf2.events.Events;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Component
@AllArgsConstructor
public class SingleEventDataSynchronizationService implements DataSynchronizationWithPaging {

  private final EventService eventService;
  private final SystemSettingsService settingService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final UserService userService;

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
    if (!SyncUtils.testServerAvailability(settingService.getCurrentSettings(), restTemplate)
        .isAvailable()) {
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
    initSyncUserIfNoUserLoggedIn();

    Date skipChangedBefore =
        settingService.getCurrentSettings().getSyncSkipSyncForDataChangedBefore();
    int objectsToSynchronize =
        eventService.getAnonymousEventReadyForSynchronizationCount(skipChangedBefore);

    if (objectsToSynchronize != 0) {
      SystemInstance instance =
          SyncUtils.getRemoteInstanceWithSyncImportStrategy(
              settingService.getCurrentSettings(), SyncEndpoint.EVENTS);

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

  /**
   * Ensures a valid {@link User} is available during event synchronization.
   *
   * <p>If a {@link User} is already logged in, no action is taken. A logged-in user is required for
   * {@link EventSynchronization}, since user checks are performed in several downstream calls, such
   * as {@link org.hisp.dhis.dxf2.events.event.JdbcEventStore#getEventCount(EventQueryParams)}.
   * Without an authenticated user, a {@link NullPointerException} may occur, causing the sync to
   * fail.
   *
   * <p>The {@link User} is initialized and unproxied to prevent {@link
   * org.hibernate.LazyInitializationException} issues.
   *
   * <p>If no {@link User} is logged in, the synchronization user will be injected. This user must
   * have sufficient authorities to perform synchronization. At this stage, a successful connection
   * to the remote server ensures valid user credentials are available.
   *
   * <p>Note: This scheduling behavior has been corrected in version 2.41, where a valid user is
   * always associated with the job at both setup and execution time.
   */
  private void initSyncUserIfNoUserLoggedIn() {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    if (currentUser == null) {
      log.info(
          "CurrentUser is null, before performing EVENT_PROGRAMS_DATA_SYNC, the remote sync user will be injected");

      String remoteUsername = settingService.getCurrentSettings().getRemoteInstanceUsername();
      User user = userService.getUserByUsername(remoteUsername);

      HibernateProxyUtils.unproxy(user);

      UserDetails currentUserDetails = userService.createUserDetails(user);

      Authentication authentication =
          new UsernamePasswordAuthenticationToken(
              currentUserDetails, "", currentUserDetails.getAuthorities());
      SecurityContext secContext = SecurityContextHolder.createEmptyContext();
      secContext.setAuthentication(authentication);
      SecurityContextHolder.setContext(secContext);
    }
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
      return;
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
        settingService.getCurrentSettings(),
        restTemplate,
        requestCallback,
        instance,
        SyncEndpoint.EVENTS);
  }
}
