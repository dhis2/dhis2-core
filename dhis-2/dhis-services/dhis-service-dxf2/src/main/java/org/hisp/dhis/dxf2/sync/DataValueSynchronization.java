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
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.synch.SystemInstance;
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
@Component
@AllArgsConstructor
public class DataValueSynchronization implements DataSynchronizationWithPaging {
  private final DataValueService dataValueService;

  private final DataValueSetService dataValueSetService;

  private final SystemSettingManager settings;

  private final RestTemplate restTemplate;

  @Getter
  private static final class DataValueSynchronisationContext
      extends PagedDataSynchronisationContext {
    private final Date lastUpdatedAfter;

    public DataValueSynchronisationContext(Date skipChangedBefore, int pageSize) {
      this(skipChangedBefore, 0, null, pageSize, null);
    }

    public DataValueSynchronisationContext(
        Date skipChangedBefore,
        int objectsToSynchronize,
        SystemInstance instance,
        int pageSize,
        Date lastUpdatedAfter) {
      super(skipChangedBefore, objectsToSynchronize, instance, pageSize);
      this.lastUpdatedAfter = lastUpdatedAfter;
    }
  }

  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    progress.startingProcess("Starting DataValueSynchronization job");
    if (!SyncUtils.testServerAvailability(settings, restTemplate).isAvailable()) {
      String msg = "DataValueSynchronization failed. Remote server is unavailable.";
      progress.failedProcess(msg);
      return SynchronizationResult.failure(msg);
    }

    progress.startingStage("Counting data values");
    DataValueSynchronisationContext context =
        progress.runStage(
            new DataValueSynchronisationContext(null, pageSize),
            ctx ->
                "DataValues last changed before "
                    + ctx.getSkipChangedBefore()
                    + " will not be synchronized.",
            () -> createContext(pageSize));

    if (context.getObjectsToSynchronize() == 0) {
      String msg = "Skipping synchronization, no new or updated DataValues";
      progress.completedProcess(msg);
      return SynchronizationResult.success(msg);
    }

    if (runSyncWithPaging(context, progress)) {
      progress.completedProcess("SUCCESS! DataValueSynchronization job is done.");
      SyncUtils.setLastSyncSuccess(
          settings, SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC, context.getStartTime());
      return SynchronizationResult.success("DataValueSynchronization done.");
    }

    String msg = "DataValueSynchronization failed. Not all pages were synchronised successfully.";
    progress.failedProcess(msg);
    return SynchronizationResult.failure(msg);
  }

  private DataValueSynchronisationContext createContext(final int pageSize) {
    final Date lastSuccessTime =
        SyncUtils.getLastSyncSuccess(settings, SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC);
    final Date skipChangedBefore =
        settings.getDateSetting(SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE);
    Date lastUpdatedAfter =
        lastSuccessTime.after(skipChangedBefore) ? lastSuccessTime : skipChangedBefore;

    int objectsToSynchronize =
        dataValueService.getDataValueCountLastUpdatedAfter(lastUpdatedAfter, true);

    if (objectsToSynchronize != 0) {
      SystemInstance instance = SyncUtils.getRemoteInstance(settings, SyncEndpoint.DATA_VALUE_SETS);
      return new DataValueSynchronisationContext(
          skipChangedBefore, objectsToSynchronize, instance, pageSize, lastUpdatedAfter);
    }
    return new DataValueSynchronisationContext(
        skipChangedBefore, 0, null, pageSize, lastUpdatedAfter);
  }

  private boolean runSyncWithPaging(DataValueSynchronisationContext context, JobProgress progress) {
    String msg = context.getObjectsToSynchronize() + " DataValues to synchronize were found.\n";
    msg += "Remote server URL for DataValues POST sync: " + context.getInstance().getUrl() + "\n";
    msg +=
        "DataValueSynchronization job has "
            + context.getPages()
            + " pages to sync. With page size: "
            + context.getPageSize();

    progress.startingStage(msg, context.getPages(), SKIP_ITEM);
    progress.runStage(
        IntStream.range(1, context.getPages() + 1).boxed(),
        page -> format("Synchronizing page %d with page size %d", page, context.getPageSize()),
        page -> synchronizePage(page, context));
    return !progress.isSkipCurrentStage();
  }

  protected void synchronizePage(int page, DataValueSynchronisationContext context) {
    if (!sendSyncRequest(page, context)) {
      throw new MetadataSyncServiceException(format("Page %d synchronisation failed.", page));
    }
  }

  private boolean sendSyncRequest(int page, DataValueSynchronisationContext context) {
    SystemInstance instance = context.getInstance();
    Date lastUpdatedAfter = context.getLastUpdatedAfter();
    int syncPageSize = context.getPageSize();

    RequestCallback requestCallback =
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
        settings, restTemplate, requestCallback, instance, SyncEndpoint.DATA_VALUE_SETS);
  }
}
