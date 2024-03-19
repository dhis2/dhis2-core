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

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService;
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
public class CompleteDataSetRegistrationSynchronization
    implements DataSynchronizationWithoutPaging {
  private final SystemSettingManager settings;

  private final RestTemplate restTemplate;

  private final CompleteDataSetRegistrationService completeDataSetRegistrationService;

  private final CompleteDataSetRegistrationExchangeService
      completeDataSetRegistrationExchangeService;

  @Getter
  private static final class CompleteDataSetRegistrationSynchronizationContext
      extends DataSynchronizationContext {
    private final Date lastUpdatedAfter;

    CompleteDataSetRegistrationSynchronizationContext() {
      this(null, 0, null, null);
    }

    public CompleteDataSetRegistrationSynchronizationContext(
        Date skipChangedBefore,
        int objectsToSynchronize,
        SystemInstance instance,
        Date lastUpdatedAfter) {
      super(skipChangedBefore, objectsToSynchronize, instance);
      this.lastUpdatedAfter = lastUpdatedAfter;
    }
  }

  @Override
  public SynchronizationResult synchronizeData(JobProgress progress) {
    progress.startingProcess("Starting Complete data set registration synchronization job.");
    if (!SyncUtils.testServerAvailability(settings, restTemplate).isAvailable()) {
      String msg =
          "Complete data set registration synchronization failed. Remote server is unavailable.";
      progress.failedProcess(msg);
      return SynchronizationResult.failure(msg);
    }

    progress.startingStage("Counting complete data sets");
    CompleteDataSetRegistrationSynchronizationContext context =
        progress.runStage(
            new CompleteDataSetRegistrationSynchronizationContext(),
            ctx ->
                "CompleteDataSetRegistrations last changed before "
                    + ctx.getSkipChangedBefore()
                    + " will not be synchronized.",
            this::createContext);

    if (context.getObjectsToSynchronize() == 0) {
      SyncUtils.setLastSyncSuccess(
          settings,
          SettingKey.LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC,
          context.getStartTime());
      String msg = "Skipping completeness synchronization, no new or updated data";
      progress.completedProcess(msg);
      return SynchronizationResult.success(msg);
    }

    if (runSync(context, progress)) {
      SyncUtils.setLastSyncSuccess(
          settings,
          SettingKey.LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC,
          context.getStartTime());
      String msg = "Complete data set registration synchronization is done.";
      progress.completedProcess(msg);
      return SynchronizationResult.success(msg);
    }

    String msg = "Complete data set registration synchronization failed.";
    progress.failedProcess(msg);
    return SynchronizationResult.failure(msg);
  }

  private CompleteDataSetRegistrationSynchronizationContext createContext() {
    Date lastSuccessTime =
        SyncUtils.getLastSyncSuccess(
            settings, SettingKey.LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC);
    Date skipChangedBefore =
        settings.getDateSetting(SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE);
    Date lastUpdatedAfter =
        lastSuccessTime.after(skipChangedBefore) ? lastSuccessTime : skipChangedBefore;
    int objectsToSynchronize =
        completeDataSetRegistrationService.getCompleteDataSetCountLastUpdatedAfter(
            lastUpdatedAfter);

    if (objectsToSynchronize != 0) {
      SystemInstance instance =
          SyncUtils.getRemoteInstance(settings, SyncEndpoint.COMPLETE_DATA_SET_REGISTRATIONS);

      return new CompleteDataSetRegistrationSynchronizationContext(
          skipChangedBefore, objectsToSynchronize, instance, lastUpdatedAfter);
    }
    return new CompleteDataSetRegistrationSynchronizationContext(
        skipChangedBefore, 0, null, lastUpdatedAfter);
  }

  private boolean runSync(
      CompleteDataSetRegistrationSynchronizationContext context, JobProgress progress) {
    String msg =
        context.getObjectsToSynchronize()
            + " completed data set registrations to synchronize were found.\n";
    msg +=
        "Remote server URL for completeness POST synchronization: "
            + context.getInstance().getUrl();

    progress.startingStage(msg);
    return progress.runStage(false, () -> sendSyncRequest(context));
  }

  private boolean sendSyncRequest(CompleteDataSetRegistrationSynchronizationContext context) {
    SystemInstance instance = context.getInstance();
    Date lastUpdatedAfter = context.getLastUpdatedAfter();
    final RequestCallback requestCallback =
        request -> {
          request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          request
              .getHeaders()
              .add(
                  SyncUtils.HEADER_AUTHORIZATION,
                  CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));

          completeDataSetRegistrationExchangeService.writeCompleteDataSetRegistrationsJson(
              lastUpdatedAfter, request.getBody(), new IdSchemes());
        };

    return SyncUtils.sendSyncRequest(
        settings,
        restTemplate,
        requestCallback,
        instance,
        SyncEndpoint.COMPLETE_DATA_SET_REGISTRATIONS);
  }
}
