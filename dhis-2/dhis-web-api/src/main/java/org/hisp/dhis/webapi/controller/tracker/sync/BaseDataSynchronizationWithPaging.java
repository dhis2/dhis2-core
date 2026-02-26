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
import static org.hisp.dhis.webapi.controller.tracker.export.MappingErrors.ensureNoMappingErrors;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.sync.DataSynchronizationWithPaging;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.webapi.controller.tracker.export.MappingErrors;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * Base abstract class providing common functionality for data synchronization operations with
 * pagination support. This class handles the core synchronization logic including HTTP
 * communication, progress tracking, error handling, and entity partitioning.
 *
 * <p>Concrete implementations should extend this class and provide entity-specific behavior for
 * data fetching, mapping, and timestamp updates.
 *
 * <p>This class follows a template method pattern where the abstract methods define the specific
 * behavior that concrete classes must implement.
 *
 * @param <V> The type of entity DTO/View object used for serialization and HTTP communication
 * @param <D> The type of domain entity being synchronized, must extend {@link SoftDeletableEntity}
 * @see DataSynchronizationWithPaging
 * @see TrackerSynchronizationContext
 */
@Slf4j
abstract class BaseDataSynchronizationWithPaging<V, D extends SoftDeletableEntity>
    implements DataSynchronizationWithPaging {

  private final RenderService renderService;
  private final RestTemplate restTemplate;

  protected BaseDataSynchronizationWithPaging(
      RenderService renderService, RestTemplate restTemplate) {
    this.renderService = renderService;
    this.restTemplate = restTemplate;
  }

  public SynchronizationResult endProcess(JobProgress progress, String message) {
    String fullMessage = format("%s %s", getProcessName(), message);
    progress.completedProcess(fullMessage);
    return SynchronizationResult.success(fullMessage);
  }

  public SynchronizationResult failProcess(JobProgress progress, String reason) {
    String fullMessage = format("%s failed. %s", getProcessName(), reason);
    progress.failedProcess(fullMessage);
    return SynchronizationResult.failure(fullMessage);
  }

  public SynchronizationResult validatePreconditions(
      SystemSettings settings, JobProgress progress) {
    if (!testServerAvailability(settings, restTemplate).isAvailable()) {
      return failProcess(progress, "Remote server unavailable");
    }

    return null;
  }

  public void synchronizePage(
      int page, TrackerSynchronizationContext context, SystemSettings settings)
      throws ForbiddenException, BadRequestException, NotFoundException, WebMessageException {
    List<D> entities = fetchEntitiesForPage(page, context);

    Map<Boolean, List<D>> partitionedTrackedEntities = partitionEntitiesByDeletionStatus(entities);
    List<D> deletedTrackedEntities = partitionedTrackedEntities.get(true);
    List<D> activeTrackedEntities = partitionedTrackedEntities.get(false);

    syncEntitiesByDeletionStatus(activeTrackedEntities, deletedTrackedEntities, context, settings);
  }

  public ImportSummary sendHttpRequest(
      List<V> entities, SystemInstance instance, SystemSettings settings, String url) {
    RequestCallback requestCallback =
        request -> {
          request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          request
              .getHeaders()
              .set(
                  SyncUtils.HEADER_AUTHORIZATION,
                  CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));

          renderService.toJson(request.getBody(), Map.of(getJsonRootName(), entities));
        };

    Optional<WebMessageResponse> response =
        runSyncRequest(
            restTemplate,
            requestCallback,
            SyncEndpoint.TRACKER_IMPORT.getKlass(),
            url,
            settings.getSyncMaxAttempts());

    return response.map(ImportSummary.class::cast).orElse(null);
  }

  public void syncAndUpdateEntities(
      List<V> entities,
      SystemInstance instance,
      SystemSettings settings,
      Date syncTime,
      TrackerImportStrategy importStrategy) {
    String url = instance.getUrl() + "?importStrategy=" + importStrategy;

    ImportSummary summary = sendHttpRequest(entities, instance, settings, url);

    if (summary == null || summary.getStatus() != ImportStatus.SUCCESS) {
      throw new MetadataSyncServiceException(
          format("%s sync failed for importStrategy=%s", getEntityName(), importStrategy));
    }

    log.info(
        "{} sync successful for importStrategy={}. Entity count: {}",
        getEntityName(),
        importStrategy,
        entities.size());

    updateEntitySyncTimeStamp(entities, syncTime);
  }

  public boolean executeSynchronizationWithPaging(
      TrackerSynchronizationContext context, JobProgress progress, SystemSettings settings) {

    final int pages = context.getPages();
    final int pageSize = context.getPageSize();
    final String entityName = getJsonRootName();
    final String remoteUrl = context.getInstance().getUrl();

    String stageDescription =
        format(
            "Found %d %s. Remote: %s. Pages: %d (size %d)",
            context.getObjectsToSynchronize(), entityName, remoteUrl, pages, pageSize);

    progress.startingStage(stageDescription, pages, SKIP_ITEM);

    progress.runStage(
        IntStream.range(1, pages + 1).boxed(),
        page -> format("Syncing page %d (size %d)", page, pageSize),
        page -> synchronizePageSafely(page, context, settings));

    return !progress.isSkipCurrentStage();
  }

  public void syncEntitiesByDeletionStatus(
      List<D> activeEntities,
      List<D> deletedEntities,
      TrackerSynchronizationContext context,
      SystemSettings settings)
      throws WebMessageException {
    Date syncTime = context.getStartTime();
    SystemInstance instance = context.getInstance();

    TrackerIdSchemeParams idSchemeParam =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.UID).build();
    MappingErrors errors = new MappingErrors(idSchemeParam);

    if (!activeEntities.isEmpty()) {
      List<V> activeEventDtos =
          activeEntities.stream().map(ev -> getMappedEntities(ev, idSchemeParam, errors)).toList();
      ensureNoMappingErrors(errors);
      syncAndUpdateEntities(
          activeEventDtos, instance, settings, syncTime, TrackerImportStrategy.CREATE_AND_UPDATE);
    }

    if (!deletedEntities.isEmpty()) {
      List<V> deletedEventDtos = deletedEntities.stream().map(this::toMinimalEntity).toList();
      syncAndUpdateEntities(
          deletedEventDtos, instance, settings, syncTime, TrackerImportStrategy.DELETE);
    }
  }

  public abstract V toMinimalEntity(D entity);

  public abstract String getJsonRootName();

  public abstract String getEntityName();

  public abstract String getProcessName();

  public abstract void updateEntitySyncTimeStamp(List<V> entities, Date syncTime);

  public abstract boolean isDeleted(D entity);

  public abstract V getMappedEntities(
      D ev, TrackerIdSchemeParams idSchemeParam, MappingErrors errors);

  public abstract long countEntitiesForSynchronization(Date skipChangedBefore)
      throws ForbiddenException, BadRequestException;

  public abstract List<D> fetchEntitiesForPage(int page, TrackerSynchronizationContext context)
      throws BadRequestException, ForbiddenException, NotFoundException;

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

  private Map<Boolean, List<D>> partitionEntitiesByDeletionStatus(List<D> entities) {
    return entities.stream().collect(Collectors.partitioningBy(this::isDeleted));
  }
}
