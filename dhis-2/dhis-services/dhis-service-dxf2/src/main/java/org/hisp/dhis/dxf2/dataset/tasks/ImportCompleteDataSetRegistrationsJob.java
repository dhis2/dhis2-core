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
package org.hisp.dhis.dxf2.dataset.tasks;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportCompleteDataSetRegistrationsJob implements Job {

  private final CompleteDataSetRegistrationExchangeService registrationService;
  private final FileResourceService fileResourceService;
  private final Notifier notifier;

  @Override
  public JobType getJobType() {
    return JobType.COMPLETE_DATA_SET_REGISTRATION_IMPORT;
  }

  @Override
  public void execute(JobConfiguration jobConfig, JobProgress progress) {
    progress.startingProcess("Complete data set registration import");
    ImportOptions options = (ImportOptions) jobConfig.getJobParameters();

    progress.startingStage("Loading file resource");
    FileResource data =
        progress.runStage(() -> fileResourceService.getFileResource(jobConfig.getUid()));

    progress.startingStage("Loading file content");
    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      String contentType = data.getContentType();
      progress.startingStage("Importing data...");
      ImportSummary summary =
          switch (contentType) {
            case "application/json" -> progress.runStage(
                () ->
                    registrationService.saveCompleteDataSetRegistrationsJson(
                        input, options, jobConfig));
            case "application/xml" -> progress.runStage(
                () ->
                    registrationService.saveCompleteDataSetRegistrationsXml(
                        input, options, jobConfig));
            default -> {
              progress.failedStage("Unknown format: " + contentType);
              yield null;
            }
          };
      if (summary == null) {
        progress.failedProcess("Import failed, no summary available");
        return;
      }
      ImportCount count = summary.getImportCount();
      progress.completedProcess(
          "Import complete with status %s, %d created, %d updated, %d deleted, %d ignored"
              .formatted(
                  summary.getStatus(),
                  count.getImported(),
                  count.getUpdated(),
                  count.getDeleted(),
                  count.getIgnored()));
      notifier.addJobSummary(jobConfig, summary, ImportSummary.class);
    } catch (IOException ex) {
      progress.failedProcess(ex);
    }
  }
}
