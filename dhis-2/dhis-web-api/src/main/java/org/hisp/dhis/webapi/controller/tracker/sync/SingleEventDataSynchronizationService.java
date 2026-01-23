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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
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
    extends BaseDataSynchronizationWithPaging<Event, SingleEvent> {
  private static final String PROCESS_NAME = "Single Event Data Synchronization";
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

  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    progress.startingProcess(PROCESS_NAME);

    SystemSettings settings = systemSettingsService.getCurrentSettings();

    SynchronizationResult validationResult = validatePreconditions(settings, progress);
    if (validationResult != null) {
      return validationResult;
    }

    TrackerSynchronizationContext context = initializeContext(pageSize, progress, settings);

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
  public long countEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    return singleEventService.countEvents(
        SingleEventOperationParams.builderForDataSync(skipChangedBefore, Map.of()).build());
  }

  @Override
  public List<SingleEvent> fetchEntitiesForPage(int page, TrackerSynchronizationContext context)
      throws ForbiddenException, BadRequestException {
    return singleEventService
        .findEvents(
            SingleEventOperationParams.builderForDataSync(
                    context.getSkipChangedBefore(), context.getSkipSyncDataElementsByProgramStage())
                .build(),
            PageParams.of(page, context.getPageSize(), false))
        .getItems();
  }

  @Override
  public Event getMappedEntities(
      SingleEvent ev, TrackerIdSchemeParams idSchemeParam, MappingErrors errors) {
    return EVENT_MAPPER.map(idSchemeParam, errors, ev);
  }

  @Override
  public String getJsonRootName() {
    return "events";
  }

  @Override
  public String getProcessName() {
    return PROCESS_NAME;
  }

  @Override
  public boolean isDeleted(SingleEvent entity) {
    return entity.isDeleted();
  }

  @Override
  public org.hisp.dhis.webapi.controller.tracker.view.Event toMinimalEntity(
      SingleEvent singleEvent) {
    org.hisp.dhis.webapi.controller.tracker.view.Event minimalEvent =
        new org.hisp.dhis.webapi.controller.tracker.view.Event();
    minimalEvent.setEvent(UID.of(singleEvent.getUid()));
    return minimalEvent;
  }

  private Map<String, Set<String>> getSkipSyncProgramStageDataElements() {
    return programStageDataElementService
        .getProgramStageDataElementsWithSkipSynchronizationSetToTrue();
  }

  private TrackerSynchronizationContext initializeContext(
      int pageSize, JobProgress progress, SystemSettings settings) {
    return progress.runStage(
        TrackerSynchronizationContext.emptyContext(null, pageSize),
        ctx -> format("Single events changed before %s will not sync", ctx.getSkipChangedBefore()),
        () -> createContext(pageSize, settings));
  }

  private TrackerSynchronizationContext createContext(int pageSize, SystemSettings settings)
      throws ForbiddenException, BadRequestException {
    Date skipChangedBefore = settings.getSyncSkipSyncForDataChangedBefore();

    long eventCount = countEntitiesForSynchronization(skipChangedBefore);

    if (eventCount == 0) {
      return TrackerSynchronizationContext.emptyContext(skipChangedBefore, pageSize);
    }

    SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.TRACKER_IMPORT);
    Map<String, Set<String>> skipSyncProgramStageDataElements =
        getSkipSyncProgramStageDataElements();

    return TrackerSynchronizationContext.forEvents(
        skipChangedBefore, eventCount, instance, pageSize, skipSyncProgramStageDataElements);
  }
}
