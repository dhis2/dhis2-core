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
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
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
public class DataValueSynchronization extends DataSynchronizationWithPaging {
  private final DataValueService dataValueService;

  private final DataValueSetService dataValueSetService;

  private final SystemSettingManager systemSettingManager;

  private final RestTemplate restTemplate;

  private Date lastUpdatedAfter;

  public DataValueSynchronization(
      DataValueService dataValueService,
      DataValueSetService dataValueSetService,
      SystemSettingManager systemSettingManager,
      RestTemplate restTemplate) {
    checkNotNull(dataValueService);
    checkNotNull(dataValueSetService);
    checkNotNull(systemSettingManager);
    checkNotNull(restTemplate);

    this.dataValueService = dataValueService;
    this.dataValueSetService = dataValueSetService;
    this.systemSettingManager = systemSettingManager;
    this.restTemplate = restTemplate;
  }

  @Override
  public SynchronizationResult synchronizeData(final int pageSize) {
    if (!SyncUtils.testServerAvailability(systemSettingManager, restTemplate).isAvailable()) {
      return SynchronizationResult.newFailureResultWithMessage(
          "DataValueSynchronization failed. Remote server is unavailable.");
    }

    log.info("Starting DataValueSynchronization job.");

    initializeSyncVariables(pageSize);

    if (objectsToSynchronize == 0) {
      log.info("Skipping synchronization, no new or updated DataValues");
      return SynchronizationResult.newSuccessResultWithMessage(
          "Skipping synchronization, no new or updated DataValues");
    }

    runSyncWithPaging(pageSize);

    if (syncResult) {
      clock.logTime("SUCCESS! DataValueSynchronization job is done. It took");
      SyncUtils.setLastSyncSuccess(
          systemSettingManager,
          SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC,
          new Date(clock.getStartTime()));
      return SynchronizationResult.newSuccessResultWithMessage(
          "DataValueSynchronization done. It took " + clock.getTime() + " ms.");
    }

    return SynchronizationResult.newFailureResultWithMessage("DataValueSynchronization failed.");
  }

  private void initializeSyncVariables(final int pageSize) {
    clock = new Clock(log).startClock().logTime("Starting DataValueSynchronization job");
    final Date lastSuccessTime =
        SyncUtils.getLastSyncSuccess(
            systemSettingManager, SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC);
    final Date skipChangedBefore =
        systemSettingManager.getDateSetting(
            SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE);
    lastUpdatedAfter =
        lastSuccessTime.after(skipChangedBefore) ? lastSuccessTime : skipChangedBefore;

    objectsToSynchronize =
        dataValueService.getDataValueCountLastUpdatedAfter(lastUpdatedAfter, true);

    log.info("DataValues last changed before " + skipChangedBefore + " will not be synchronized.");

    if (objectsToSynchronize != 0) {
      instance = SyncUtils.getRemoteInstance(systemSettingManager, SyncEndpoint.DATA_VALUE_SETS);

      // Using this approach as (int) Match.ceil doesn't work until I cast
      // int to double
      pages = (objectsToSynchronize / pageSize) + ((objectsToSynchronize % pageSize == 0) ? 0 : 1);

      log.info(objectsToSynchronize + " DataValues to synchronize were found.");
      log.info("Remote server URL for DataValues POST sync: " + instance.getUrl());
      log.info(
          "DataValueSynchronization job has "
              + pages
              + " pages to sync. With page size: "
              + pageSize);
    }
  }

  protected void synchronizePage(int page, int pageSize) {
    log.info(String.format("Synchronizing page %d with page size %d", page, pageSize));

    if (!sendSyncRequest(pageSize, page)) {
      syncResult = false;
    }
  }

  private boolean sendSyncRequest(int syncPageSize, int page) {
    final RequestCallback requestCallback =
        request -> {
          request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          request
              .getHeaders()
              .add(
                  SyncUtils.HEADER_AUTHORIZATION,
                  CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));

          dataValueSetService.exportDataValueSetJson(
              lastUpdatedAfter, request.getBody(), new IdSchemes(), syncPageSize, page);
        };

    return SyncUtils.sendSyncRequest(
        systemSettingManager,
        restTemplate,
        requestCallback,
        instance,
        SyncEndpoint.DATA_VALUE_SETS);
  }
}
