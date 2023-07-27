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
package org.hisp.dhis.dxf2.datavalueset.tasks;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.adx.AdxDataService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.stereotype.Component;

/**
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class DataValueSetImportJob implements Job {

  private final FileResourceService fileResourceService;
  private final DataValueSetService dataValueSetService;
  private final AdxDataService adxDataService;

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
        progress.runStage(() -> fileResourceService.getFileResource(jobId.getUid()));
    progress.startingStage("Loading file content");
    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      String contentType = data.getContentType();
      progress.startingStage("Importing data...");
      ImportSummary summary =
          switch (contentType) {
            case "application/json" -> progress.runStage(
                () -> dataValueSetService.importDataValueSetJson(input, options, jobId));
            case "application/csv" -> progress.runStage(
                () -> dataValueSetService.importDataValueSetCsv(input, options, jobId));
            case "application/pdf" -> progress.runStage(
                () -> dataValueSetService.importDataValueSetPdf(input, options, jobId));
            case "application/adx+xml" -> progress.runStage(
                () -> adxDataService.saveDataValueSet(input, options, jobId));
            case "application/xml" -> progress.runStage(
                () -> dataValueSetService.importDataValueSetXml(input, options, jobId));
            default -> progress.runStage(
                () -> {
                  progress.failedProcess("Import failed, no summary available");
                  throw new UnsupportedOperationException("Unknown format: " + contentType);
                });
          };
      ImportCount count = summary.getImportCount();
      progress.completedProcess(
          "Import complete with status %s, %d created, %d updated, %d deleted, %d ignored"
              .formatted(
                  summary.getStatus(),
                  count.getImported(),
                  count.getUpdated(),
                  count.getDeleted(),
                  count.getIgnored()));
    } catch (IOException ex) {
      progress.failedProcess(ex);
    }
  }
}
