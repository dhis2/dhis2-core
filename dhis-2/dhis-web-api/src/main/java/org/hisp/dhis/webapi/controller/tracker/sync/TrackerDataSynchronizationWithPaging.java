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
import static org.hisp.dhis.dxf2.sync.SyncUtils.testServerAvailability;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.sync.DataSynchronizationWithPaging;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for tracker data synchronization jobs that require paging support and an associated
 * Program UID context. Extends {@link DataSynchronizationWithPaging} to add tracker-specific
 * synchronization behavior.
 */
public abstract class TrackerDataSynchronizationWithPaging
    implements DataSynchronizationWithPaging {

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
   * Implementations should invoke {@link #synchronizeTrackerData(int, JobProgress, UID)} instead
   * when a program context is available.
   */
  @Override
  public SynchronizationResult synchronizeData(int pageSize, JobProgress progress) {
    throw new UnsupportedOperationException(
        "Use synchronizeTrackerData(pageSize, progress, programUid) instead.");
  }

  public SynchronizationResult endProcess(
      JobProgress progress, String message, String processName) {
    String fullMessage = format("%s %s", processName, message);
    progress.completedProcess(fullMessage);
    return SynchronizationResult.success(fullMessage);
  }

  public SynchronizationResult failProcess(
      JobProgress progress, String reason, String processName) {
    String fullMessage = format("%s failed. %s", processName, reason);
    progress.failedProcess(fullMessage);
    return SynchronizationResult.failure(fullMessage);
  }

  public SynchronizationResult validatePreconditions(
      SystemSettings settings,
      JobProgress progress,
      RestTemplate restTemplate,
      String processName) {
    if (!testServerAvailability(settings, restTemplate).isAvailable()) {
      return failProcess(progress, "Remote server unavailable", processName);
    }

    return null;
  }
}
