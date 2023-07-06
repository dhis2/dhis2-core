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
package org.hisp.dhis.dataintegrity.jobs;

import java.util.Set;
import lombok.AllArgsConstructor;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataintegrity.DataIntegrityService;
import org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters.DataIntegrityReportType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Component("dataIntegrityJob")
@AllArgsConstructor
public class DataIntegrityJob implements Job {
  private final DataIntegrityService dataIntegrityService;

  private final Notifier notifier;

  @Override
  public JobType getJobType() {
    return JobType.DATA_INTEGRITY;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    DataIntegrityJobParameters parameters = (DataIntegrityJobParameters) config.getJobParameters();
    Set<String> checks = parameters == null ? Set.of() : parameters.getChecks();

    DataIntegrityReportType type = parameters == null ? null : parameters.getType();
    if (type == null || type == DataIntegrityReportType.REPORT) {
      runReport(config, progress, checks);
    } else if (type == DataIntegrityReportType.SUMMARY) {
      dataIntegrityService.runSummaryChecks(checks, progress);
    } else {
      dataIntegrityService.runDetailsChecks(checks, progress);
    }
  }

  private void runReport(JobConfiguration config, JobProgress progress, Set<String> checks) {
    Timer timer = new SystemTimer().start();
    notifier.notify(config, NotificationLevel.INFO, "Starting data integrity job", false);

    FlattenedDataIntegrityReport report = dataIntegrityService.getReport(checks, progress);

    timer.stop();

    notifier
        .notify(
            config,
            NotificationLevel.INFO,
            "Data integrity checks completed in " + timer + ".",
            true)
        .addJobSummary(config, report, FlattenedDataIntegrityReport.class);
  }
}
