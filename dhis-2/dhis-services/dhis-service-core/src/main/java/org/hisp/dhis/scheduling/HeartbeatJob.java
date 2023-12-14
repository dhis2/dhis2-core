/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.scheduling;

import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Performs small general signal calls to perform updates that need to be maintained every n
 * seconds.
 *
 * <p>The reason this is better placed in this job than to execute it directly with an {@link
 * java.util.concurrent.ExecutorService} is to have it in sync with the general scheduling loop.
 *
 * <p>The second good reason is that using jobs can utilise the {@link JobProgress} tracking to
 * improve error handling and visibility without making it complicated.
 *
 * @author Jan Bernitt
 * @since 2.41
 */
@Component
@RequiredArgsConstructor
public class HeartbeatJob implements Job {

  private final JobSchedulerLoopService jobSchedulerService;
  private final JobConfigurationService jobConfigurationService;

  @Override
  public JobType getJobType() {
    return JobType.HOUSEKEEPING;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    progress.startingProcess("Heartbeat");

    progress.startingStage("Apply job cancellation", SKIP_STAGE);
    progress.runStage(
        0, "%d jobs were cancelled"::formatted, jobSchedulerService::applyCancellation);

    progress.startingStage("Auto spawn default jobs when missing", SKIP_STAGE);
    progress.runStage(
        0,
        "%d default jobs were created"::formatted, //
        jobConfigurationService::createDefaultJobs);

    progress.startingStage("Update statue to DISABLED for non enabled jobs", SKIP_STAGE);
    progress.runStage(
        0,
        "%d jobs were switched to state DISABLED"::formatted,
        jobConfigurationService::updateDisabledJobs);

    progress.startingStage("Cleanup finished ONCE_ASAP jobs", SKIP_STAGE);
    progress.runStage(
        0,
        "%d jobs were deleted"::formatted, //
        () -> jobConfigurationService.deleteFinishedJobs(-1));

    progress.startingStage("Reschedule stale jobs", SKIP_STAGE);
    progress.runStage(
        0,
        "%d jobs were rescheduled"::formatted,
        () -> jobConfigurationService.rescheduleStaleJobs(-1));

    progress.completedProcess(null);
  }
}
