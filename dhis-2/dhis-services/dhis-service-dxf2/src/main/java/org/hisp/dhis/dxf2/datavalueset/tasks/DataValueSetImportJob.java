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
package org.hisp.dhis.dxf2.datavalueset.tasks;

import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.datavalue.DataEntryPipeline;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class DataValueSetImportJob implements Job {

  private final FileResourceService fileResourceService;
  private final Notifier notifier;
  private final DataEntryPipeline dataEntryPipeline;

  @Override
  public JobType getJobType() {
    return JobType.DATAVALUE_IMPORT;
  }

  @Override
  public void execute(JobConfiguration jobId, JobProgress progress) {
    progress.startingProcess("Data value set import");
    ImportOptions options = (ImportOptions) jobId.getJobParameters();
    progress.startingStage("Loading file resource");
    FileResource data =
        progress.nonNullStagePostCondition(
            progress.runStage(() -> fileResourceService.getFileResource(jobId.getUid())));
    progress.startingStage("Loading file content");
    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      String contentType = data.getContentType();
      boolean unknownFormat = false;
      ImportSummary summary =
          switch (contentType) {
            case "application/json" -> dataEntryPipeline.importJson(input, options, progress);
            case "application/csv" -> dataEntryPipeline.importCsv(input, options, progress);
            case "application/pdf" -> dataEntryPipeline.importPdf(input, options, progress);
            case "application/xml", "application/adx+xml" ->
                dataEntryPipeline.importXml(input, options, progress);
            default -> {
              unknownFormat = true;
              yield null;
            }
          };
      if (summary == null) {
        String error =
            unknownFormat
                ? "Unknown format: " + contentType
                : "Import failed, no summary available";
        progress.failedProcess(error);
        return;
      }

      if (summary.hasConflicts()) {
        for (ImportConflict c : summary.getConflicts())
          progress.addError(c.getErrorCode(), c.getObject(), c.getGroupingKey(), c.getArgs());
      }

      ImportCount count = summary.getImportCount();
      progress.completedProcess(
          "Import complete with status {}, {} created, {} updated, {} deleted, {} ignored",
          summary.getStatus(),
          count.getImported(),
          count.getUpdated(),
          count.getDeleted(),
          count.getIgnored());

      NotificationLevel level = options == null ? INFO : options.getNotificationLevel(INFO);
      notifier.addJobSummary(jobId, level, summary, ImportSummary.class);
    } catch (Exception ex) {
      progress.failedProcess(ex);
      // TODO add error summary
    }
  }
}
