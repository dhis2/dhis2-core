/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataRetryContext;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataSyncJob;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.sync.CompleteDataSetRegistrationSynchronization;
import org.hisp.dhis.dxf2.sync.DataValueSynchronization;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SynchronizationStatus;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * Performs the tasks before metadata sync happens
 *
 * @author aamerm
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Component
@RequiredArgsConstructor
public class MetadataSyncPreProcessor {
  private final SystemSettingsService settingsService;

  private final MetadataVersionService metadataVersionService;

  private final MetadataVersionDelegate metadataVersionDelegate;

  private final DataValueSynchronization dataValueSync;

  private final CompleteDataSetRegistrationSynchronization completeDataSetRegistrationSync;

  public void setUp(MetadataRetryContext context, JobProgress progress) {
    progress.startingProcess("Setting up metadata synchronisation");
    progress.runStage(() -> settingsService.put("keyVersionEnabled", true));
    progress.completedProcess("finished setting up metadata synchronisation");
  }

  public void handleDataValuePush(
      MetadataRetryContext context, MetadataSyncJobParameters syncParams, JobProgress progress) {
    SynchronizationResult result =
        dataValueSync.synchronizeData(syncParams.getDataValuesPageSize(), progress);

    if (result.status == SynchronizationStatus.FAILURE) {
      context.updateRetryContext(MetadataSyncJob.DATA_PUSH_SUMMARY, result.message, null, null);
      throw new MetadataSyncServiceException(result.message);
    }
  }

  public List<MetadataVersion> handleMetadataVersionsList(
      MetadataRetryContext context, MetadataVersion version, JobProgress progress) {
    progress.startingProcess(
        "Fetching the list of remote versions"
            + (version == null ? "There is no initial version in the system" : ""));
    try {
      List<MetadataVersion> versions = metadataVersionDelegate.getMetaDataDifference(version);

      if (isRemoteVersionEmpty(version, versions)) {
        progress.completedProcess("There are no metadata versions created in the remote instance");
        return versions;
      }

      if (isUsingLatestVersion(version, versions)) {
        progress.completedProcess("Your instance is already using the latest version:" + version);
        return versions;
      }

      MetadataVersion latestVersion = getLatestVersion(versions);
      assert latestVersion != null;

      settingsService.put("keyRemoteMetadataVersion", latestVersion.getName());
      progress.completedProcess("Remote system is at version: " + latestVersion.getName());
      return versions;
    } catch (MetadataVersionServiceException e) {
      String message = setVersionListErrorInfoInContext(context, version, e);
      progress.failedStage(message);
      throw new MetadataSyncServiceException(message, e);
    } catch (Exception ex) {
      if (ex instanceof MetadataSyncServiceException) {
        progress.failedStage(ex);
        throw ex;
      }

      String message = setVersionListErrorInfoInContext(context, version, ex);
      progress.failedStage(ex);
      throw new MetadataSyncServiceException(message, ex);
    }
  }

  private String setVersionListErrorInfoInContext(
      MetadataRetryContext context, MetadataVersion version, Exception e) {
    String message =
        "Exception happened while trying to get remote metadata versions difference "
            + e.getMessage();
    context.updateRetryContext(
        MetadataSyncJob.GET_METADATAVERSIONSLIST, e.getMessage(), version, null);
    return message;
  }

  private boolean isUsingLatestVersion(
      MetadataVersion metadataVersion, List<MetadataVersion> versions) {
    return metadataVersion != null && versions.isEmpty();
  }

  private boolean isRemoteVersionEmpty(
      MetadataVersion metadataVersion, List<MetadataVersion> versions) {
    return metadataVersion == null && versions.isEmpty();
  }

  public MetadataVersion handleCurrentMetadataVersion(
      MetadataRetryContext context, JobProgress progress) {
    progress.startingProcess("Getting the current version of the system");

    try {
      MetadataVersion version = metadataVersionService.getCurrentVersion();
      progress.completedProcess("Current Metadata Version of the system: {}", version);
      return version;
    } catch (MetadataVersionServiceException ex) {
      context.updateRetryContext(MetadataSyncJob.GET_METADATAVERSION, ex.getMessage(), null, null);
      progress.failedStage(ex);
      throw new MetadataSyncServiceException(ex.getMessage(), ex);
    }
  }

  // ----------------------------------------------------------------------------------------
  // Private Methods
  // ----------------------------------------------------------------------------------------

  private MetadataVersion getLatestVersion(List<MetadataVersion> metadataVersionList) {
    Collection<Date> dateCollection = new ArrayList<>();

    for (MetadataVersion metadataVersion : metadataVersionList) {
      dateCollection.add(metadataVersion.getCreated());
    }

    Date maxDate = DateUtils.max(dateCollection);

    for (MetadataVersion metadataVersion : metadataVersionList) {
      if (metadataVersion.getCreated().equals(maxDate)) {
        return metadataVersion;
      }
    }
    return null;
  }

  public void handleCompleteDataSetRegistrationDataPush(
      MetadataRetryContext context, JobProgress progress) {
    SynchronizationResult result = completeDataSetRegistrationSync.synchronizeData(progress);

    if (result.status == SynchronizationStatus.FAILURE) {
      context.updateRetryContext(MetadataSyncJob.DATA_PUSH_SUMMARY, result.message, null, null);
      throw new MetadataSyncServiceException(result.message);
    }
  }
}
