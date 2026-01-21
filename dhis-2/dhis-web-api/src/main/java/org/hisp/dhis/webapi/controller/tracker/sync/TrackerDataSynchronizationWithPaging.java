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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for tracker data synchronization jobs that require paging support and an associated
 * Program UID context. Extends {@link DataSynchronizationWithPaging} to add tracker-specific
 * synchronization behavior.
 */
@Slf4j
@Component
public abstract class TrackerDataSynchronizationWithPaging<E, V extends SoftDeletableEntity>
    implements DataSynchronizationWithPaging {

  private final RenderService renderService;
  private final RestTemplate restTemplate;

  protected TrackerDataSynchronizationWithPaging(
      RenderService renderService, RestTemplate restTemplate) {
    this.renderService = renderService;
    this.restTemplate = restTemplate;
  }

  /**
   * Synchronize tracker data (events, enrollments, tracked entities etc.) for a specific program.
   *
   * @param pageSize number of records per page
   * @param progress job progress reporter
   * @return result of synchronization
   */
  public abstract SynchronizationResult synchronizeTrackerData(int pageSize, JobProgress progress);

  /**
   * This method from {@link DataSynchronizationWithPaging} is not directly used here.
   * Implementations should invoke {@link #synchronizeTrackerData(int, JobProgress)} instead when a
   * program context is available.
   */
  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    throw new UnsupportedOperationException(
        "Use synchronizeTrackerData(pageSize, progress, programUid) instead.");
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

  public ImportSummary sendHttpRequest(
      List<E> entities, SystemInstance instance, SystemSettings settings, String url) {
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

  public void syncEntities(
      List<E> entities,
      SystemInstance instance,
      SystemSettings settings,
      Date syncTime,
      TrackerImportStrategy importStrategy) {
    String url = instance.getUrl() + "?importStrategy=" + importStrategy;

    ImportSummary summary = sendHttpRequest(entities, instance, settings, url);

    if (summary == null || summary.getStatus() != ImportStatus.SUCCESS) {
      throw new MetadataSyncServiceException(
          format("Single Event sync failed for importStrategy=%s", importStrategy));
    }

    log.info(
        "Single Event sync successful for importStrategy={}. Events count: {}",
        importStrategy,
        entities.size());

    updateEntitySyncTimeStamp(entities, syncTime);
  }

  public Map<Boolean, List<V>> partitionEntitiesByDeletionStatus(List<V> entities) {
    return entities.stream().collect(Collectors.partitioningBy(this::isDeleted));
  }

  public abstract String getJsonRootName();

  public abstract String getProcessName();

  public abstract void updateEntitySyncTimeStamp(List<E> entities, Date syncTime);

  public abstract boolean isDeleted(V entity);
}
