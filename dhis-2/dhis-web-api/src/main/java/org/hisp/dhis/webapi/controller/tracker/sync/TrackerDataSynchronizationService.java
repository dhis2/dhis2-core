/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webapi.controller.tracker.export.trackedentity.TrackedEntityMapper;
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
public class TrackerDataSynchronizationService extends TrackerDataSynchronizationWithPaging {
  private static final String PROCESS_NAME = "Tracker data synchronization";
  private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER =
      Mappers.getMapper(TrackedEntityMapper.class);

  private final TrackedEntityService trackedEntityService;
  private final SystemSettingsService systemSettingsService;
  private final RestTemplate restTemplate;
  private final RenderService renderService;

  @Getter
  private static final class TrackerSynchronizationContext extends PagedDataSynchronisationContext {
    public TrackerSynchronizationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize);
    }

    public TrackerSynchronizationContext(
        Date skipChangedBefore, long objectsToSynchronize, SystemInstance instance, int pageSize) {
      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
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
        skipChangedBefore, trackedEntityCount, instance, pageSize);
  }

  private long countTrackedEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.buildForDataSync(skipChangedBefore).build();
    return trackedEntityService.getTrackedEntityCount(params);
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

    return !progress.isSkipCurrentStage();
  }

  private void synchronizePageSafely(
      int page, TrackerSynchronizationContext context, SystemSettings settings) {
    try {
      synchronizePage(page, context, settings);
    } catch (Exception ex) {
      log.error("Failed to synchronize page {}", page, ex);
      throw new RuntimeException(
          format("Page %d synchronization failed: %s", page, ex.getMessage()), ex);
    }
  }

  private void synchronizePage(
      int page, TrackerSynchronizationContext context, SystemSettings settings)
      throws ForbiddenException, BadRequestException, NotFoundException {
    List<TrackedEntity> trackedEntities = fetchTrackedEntitiesForPage(page, context);

    Map<Boolean, List<TrackedEntity>> partitionedTrackedEntities =
        partitionTrackedEntitiesByDeletionStatus(trackedEntities);
    List<TrackedEntity> deletedTrackedEntities = partitionedTrackedEntities.get(true);
    List<TrackedEntity> activeTrackedEntities = partitionedTrackedEntities.get(false);

    syncTrackedEntitiesByDeletionStatus(
        activeTrackedEntities, deletedTrackedEntities, context, settings);
  }

  private List<TrackedEntity> fetchTrackedEntitiesForPage(
      int page, TrackerSynchronizationContext context)
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.buildForDataSync(context.getSkipChangedBefore()).build();
    return trackedEntityService
        .findTrackedEntities(params, PageParams.of(page, context.getPageSize(), false))
        .getItems();
  }

  private Map<Boolean, List<TrackedEntity>> partitionTrackedEntitiesByDeletionStatus(
      List<TrackedEntity> trackedEntities) {
    return trackedEntities.stream().collect(Collectors.partitioningBy(TrackedEntity::isDeleted));
  }

  private void syncTrackedEntitiesByDeletionStatus(
      List<TrackedEntity> activeTrackedEntities,
      List<TrackedEntity> deletedTrackedEntities,
      TrackerSynchronizationContext context,
      SystemSettings settings) {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    TrackerIdSchemeParams idSchemeParams =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.UID).build();
    MappingErrors errors = new MappingErrors(idSchemeParams);

    if (!activeTrackedEntities.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> activeTrackedEntityDtos =
          activeTrackedEntities.stream()
              .map(te -> TRACKED_ENTITY_MAPPER.map(idSchemeParams, errors, te))
              .toList();
      syncTrackedEntities(
          activeTrackedEntityDtos,
          instance,
          settings,
          syncTime,
          TrackerImportStrategy.CREATE_AND_UPDATE);
    }

    if (!deletedTrackedEntities.isEmpty()) {
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> deletedTrackedEntityDtos =
          deletedTrackedEntities.stream().map(this::toMinimalTrackedEntity).toList();
      syncTrackedEntities(
          deletedTrackedEntityDtos, instance, settings, syncTime, TrackerImportStrategy.DELETE);
    }
  }

  private void syncTrackedEntities(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      SystemInstance instance,
      SystemSettings settings,
      Date syncTime,
      TrackerImportStrategy importStrategy) {
    String url = instance.getUrl() + "?importStrategy=" + importStrategy;

    ImportSummary summary = sendTrackerRequest(trackedEntities, instance, settings, url);

    if (summary == null || summary.getStatus() != ImportStatus.SUCCESS) {
      throw new MetadataSyncServiceException(
          format("Tracked Entity sync failed for importStrategy=%s", importStrategy));
    }

    log.info(
        "Tracked Entity sync successful for importStrategy={}. Tracked entities count: {}",
        importStrategy,
        trackedEntities.size());

    updateTrackedEntitiesSyncTimestamp(trackedEntities, syncTime);
  }

  private ImportSummary sendTrackerRequest(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      SystemInstance instance,
      SystemSettings settings,
      String url) {
    RequestCallback requestCallback = createRequestCallback(trackedEntities, instance);

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
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      SystemInstance instance) {
    return request -> {
      request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      request
          .getHeaders()
          .set(
              SyncUtils.HEADER_AUTHORIZATION,
              CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));

      renderService.toJson(request.getBody(), Map.of("trackedEntities", trackedEntities));
    };
  }

  private void updateTrackedEntitiesSyncTimestamp(
      List<org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity> trackedEntities,
      Date syncTime) {
    List<String> trackedEntityUids =
        trackedEntities.stream().map(te -> te.getTrackedEntity().getValue()).toList();

    trackedEntityService.updateTrackedEntitiesSyncTimestamp(UID.of(trackedEntityUids), syncTime);
  }

  private org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity toMinimalTrackedEntity(
      TrackedEntity trackedEntity) {
    org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity minimalTrackedEntity =
        new org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity();
    minimalTrackedEntity.setTrackedEntity(UID.of(trackedEntity.getUid()));
    return minimalTrackedEntity;
  }
}
