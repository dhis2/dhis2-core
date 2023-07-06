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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Slf4j
@Component
public class TrackerSynchronization extends DataSynchronizationWithPaging {
  private final TrackedEntityInstanceService teiService;

  private final SystemSettingManager systemSettingManager;

  private final RestTemplate restTemplate;

  private final RenderService renderService;

  public TrackerSynchronization(
      TrackedEntityInstanceService teiService,
      SystemSettingManager systemSettingManager,
      RestTemplate restTemplate,
      RenderService renderService) {
    checkNotNull(teiService);
    checkNotNull(systemSettingManager);
    checkNotNull(restTemplate);
    checkNotNull(renderService);

    this.teiService = teiService;
    this.systemSettingManager = systemSettingManager;
    this.restTemplate = restTemplate;
    this.renderService = renderService;
  }

  @Override
  public SynchronizationResult synchronizeData(final int pageSize) {
    if (!SyncUtils.testServerAvailability(systemSettingManager, restTemplate).isAvailable()) {
      return SynchronizationResult.newFailureResultWithMessage(
          "Tracker programs data synchronization failed. Remote server is unavailable.");
    }

    TrackedEntityInstanceQueryParams queryParams = initializeQueryParams();
    initializeSyncVariables(queryParams, pageSize);

    if (objectsToSynchronize == 0) {
      log.info("Skipping synchronization. No new tracker data to synchronize were found.");
      return SynchronizationResult.newSuccessResultWithMessage(
          "Tracker programs data synchronization skipped. No new or updated TEIs found.");
    }

    runSyncWithPaging(queryParams, pageSize);

    if (syncResult) {
      clock.logTime(
          "SUCCESS! Tracker programs data synchronization was successfully done! It took ");
      return SynchronizationResult.newSuccessResultWithMessage(
          "Tracker programs data synchronization done. It took " + clock.getTime() + " ms.");
    }

    return SynchronizationResult.newFailureResultWithMessage(
        "Tracker programs data synchronization failed.");
  }

  private void initializeSyncVariables(
      TrackedEntityInstanceQueryParams queryParams, final int pageSize) {
    clock =
        new Clock(log).startClock().logTime("Starting Tracker programs data synchronization job.");
    final Date skipChangedBefore =
        systemSettingManager.getDateSetting(
            SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE);
    queryParams.setSkipChangedBefore(skipChangedBefore);
    objectsToSynchronize = teiService.getTrackedEntityInstanceCount(queryParams, true, true);

    log.info(
        "TrackedEntityInstances last changed before "
            + skipChangedBefore
            + " will not be synchronized.");

    if (objectsToSynchronize != 0) {
      instance =
          SyncUtils.getRemoteInstanceWithSyncImportStrategy(
              systemSettingManager, SyncEndpoint.TRACKED_ENTITY_INSTANCES);
      pages =
          (objectsToSynchronize / pageSize)
              + ((objectsToSynchronize % pageSize == 0) ? 0 : 1); // Have
      // to
      // use
      // this
      // as
      // (int)
      // Match.ceil
      // doesn't
      // work
      // until
      // I
      // am
      // casting
      // int
      // to
      // double

      log.info(objectsToSynchronize + " TEIs to sync were found.");
      log.info("Remote server URL for Tracker programs POST synchronization: " + instance.getUrl());
      log.info(
          "Tracker programs data synchronization job has "
              + pages
              + " pages to synchronize. With page size: "
              + pageSize);

      queryParams.setPageSize(pageSize);
    }
  }

  private TrackedEntityInstanceQueryParams initializeQueryParams() {
    TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
    queryParams.setIncludeDeleted(true);
    queryParams.setSynchronizationQuery(true);

    return queryParams;
  }

  private void runSyncWithPaging(TrackedEntityInstanceQueryParams queryParams, int pageSize) {
    syncResult = true;

    for (int page = 1; page <= pages; page++) {
      synchronizePage(queryParams, page, pageSize);
    }
  }

  private void synchronizePage(
      TrackedEntityInstanceQueryParams queryParams, int page, int pageSize) {
    queryParams.setPage(page);

    List<TrackedEntityInstance> dtoTeis =
        teiService.getTrackedEntityInstances(
            queryParams, TrackedEntityInstanceParams.DATA_SYNCHRONIZATION, true, true);
    log.info(String.format("Synchronizing page %d with page size %d", page, pageSize));

    if (log.isDebugEnabled()) {
      log.debug("TEIs that are going to be synchronized are: " + dtoTeis);
    }

    if (sendSyncRequest(dtoTeis)) {
      List<String> teiUIDs =
          dtoTeis.stream()
              .map(TrackedEntityInstance::getTrackedEntityInstance)
              .collect(Collectors.toList());
      log.info("The lastSynchronized flag of these TEIs will be updated: " + teiUIDs);
      teiService.updateTrackedEntityInstancesSyncTimestamp(teiUIDs, new Date(clock.getStartTime()));
    } else {
      syncResult = false;
    }
  }

  private boolean sendSyncRequest(List<TrackedEntityInstance> dtoTeis) {
    TrackedEntityInstances teis = new TrackedEntityInstances();
    teis.setTrackedEntityInstances(dtoTeis);

    final RequestCallback requestCallback =
        request -> {
          request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          request
              .getHeaders()
              .add(
                  SyncUtils.HEADER_AUTHORIZATION,
                  CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));
          renderService.toJson(request.getBody(), teis);
        };

    return SyncUtils.sendSyncRequest(
        systemSettingManager,
        restTemplate,
        requestCallback,
        instance,
        SyncEndpoint.TRACKED_ENTITY_INSTANCES);
  }

  @Override
  protected void runSyncWithPaging(int pageSize) {
    throw new IllegalStateException(
        "Method runSyncWithPaging(int pageSize) is not supported by TrackerSynchronization");
  }

  @Override
  protected void synchronizePage(int page, int pageSize) {
    throw new IllegalStateException(
        "Method synchronizePage(int page, int pageSize) is not supported by TrackerSynchronization");
  }
}
