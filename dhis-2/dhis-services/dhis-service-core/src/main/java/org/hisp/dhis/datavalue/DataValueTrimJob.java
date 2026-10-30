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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.stereotype.Component;

/**
 * A job that takes care of updating data value related soft transitions such as linking and
 * unlinking file resources based on usage as well as marking data values deleted that are 0 but not
 * significant.
 *
 * <p>This should run regularly to make the data eventually consistent.
 *
 * @author Jan Bernitt
 * @since 2.43
 */
@Component
@RequiredArgsConstructor
public class DataValueTrimJob implements Job {

  private final DataValueTrimService service;

  @Override
  public JobType getJobType() {
    return JobType.DATA_VALUE_TRIM;
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    progress.startingProcess("Data value trim");

    progress.startingStage(
        "Marking file resources assigned referenced by a data value", SKIP_STAGE);
    progress.runStage(
        0,
        "Marked %d file resources as assigned"::formatted,
        service::updateFileResourcesAssignedToAnyDataValue);

    progress.startingStage(
        "Marking file resourced as not assigned not referenced by any data value", SKIP_STAGE);
    progress.runStage(
        0,
        "Marked %d file resources as not assigned"::formatted,
        service::updateFileResourcesNotAssignedToAnyDataValue);

    progress.startingStage(
        "Marking data values as deleted that are 0 and not significant", SKIP_STAGE);
    progress.runStage(
        0,
        "Marked %d data values as deleted"::formatted,
        service::updateDeletedIfNotZeroIsSignificant);

    progress.completedProcess(null);
  }
}
