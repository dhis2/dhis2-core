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
package org.hisp.dhis.dxf2.synch;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hisp.dhis.dxf2.sync.CompleteDataSetRegistrationSynchronization;
import org.hisp.dhis.dxf2.sync.DataValueSynchronization;
import org.hisp.dhis.dxf2.sync.SynchronizationJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Component("dataSyncJob")
public class DataSynchronizationJob extends SynchronizationJob {
  private final SynchronizationManager synchronizationManager;

  private final Notifier notifier;

  private final DataValueSynchronization dataValueSynchronization;

  private final CompleteDataSetRegistrationSynchronization completenessSynchronization;

  public DataSynchronizationJob(
      Notifier notifier,
      DataValueSynchronization dataValueSynchronization,
      CompleteDataSetRegistrationSynchronization completenessSynchronization,
      SynchronizationManager synchronizationManager) {
    checkNotNull(notifier);
    checkNotNull(dataValueSynchronization);
    checkNotNull(completenessSynchronization);
    checkNotNull(synchronizationManager);

    this.notifier = notifier;
    this.dataValueSynchronization = dataValueSynchronization;
    this.completenessSynchronization = completenessSynchronization;
    this.synchronizationManager = synchronizationManager;
  }

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public JobType getJobType() {
    return JobType.DATA_SYNC;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    DataSynchronizationJobParameters jobParameters =
        (DataSynchronizationJobParameters) jobConfiguration.getJobParameters();
    dataValueSynchronization.synchronizeData(jobParameters.getPageSize());
    notifier.notify(jobConfiguration, "Data value sync successful");

    completenessSynchronization.synchronizeData();
    notifier.notify(jobConfiguration, "Complete data set registration sync successful");

    notifier.notify(
        jobConfiguration, "Data value and Complete data set registration sync successful");
  }
}
