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
package org.hisp.dhis.dataexchange.aggregate;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.AggregateDataExchangeJobParameters;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Component;

/**
 * Job implementation for aggregate data exchange.
 *
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class AggregateDataExchangeJob implements Job {
  private final AggregateDataExchangeService dataExchangeService;

  private final Notifier notifier;

  @Override
  public JobType getJobType() {
    return JobType.AGGREGATE_DATA_EXCHANGE;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    notifier.clear(config);
    AggregateDataExchangeJobParameters params =
        (AggregateDataExchangeJobParameters) config.getJobParameters();

    List<String> dataExchangeIds = params.getDataExchangeIds();
    progress.startingProcess(
        "Aggregate data exchange with {} exchange(s) started", dataExchangeIds.size());
    ImportSummaries allSummaries = new ImportSummaries();
    for (String dataExchangeId : dataExchangeIds) {
      AggregateDataExchange exchange;
      try {
        exchange = dataExchangeService.loadByUid(dataExchangeId);
      } catch (IllegalQueryException ex) {
        progress.startingStage("exchange aggregate data for exchange with ID " + dataExchangeId);
        progress.failedStage(ex);
        allSummaries.addImportSummary(new ImportSummary(ImportStatus.ERROR, ex.getMessage()));
        continue;
      }
      allSummaries.addImportSummaries(
          dataExchangeService.exchangeData(
              CurrentUserUtil.getCurrentUserDetails(), exchange, progress));
    }
    notifier.addJobSummary(config, NotificationLevel.INFO, allSummaries, ImportSummaries.class);
    ImportStatus status = allSummaries.getStatus();
    if (status == ImportStatus.ERROR) {
      String errors =
          allSummaries.getImportSummaries().stream()
              .map(ImportSummary::getDescription)
              .collect(Collectors.joining(","));
      progress.failedProcess("Aggregate data exchange completed with errors: {}", errors);
    } else {
      progress.completedProcess("Aggregate data exchange completed with status: {}", status);
    }
  }
}
