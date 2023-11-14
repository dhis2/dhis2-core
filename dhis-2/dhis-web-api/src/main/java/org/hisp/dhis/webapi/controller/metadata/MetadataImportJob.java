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
package org.hisp.dhis.webapi.controller.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.csv.CsvImportOptions;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.dxf2.gml.GmlImportService;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.Stats;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

/**
 * @author Jan Bernitt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataImportJob implements Job {

  private final MetadataImportService metadataImportService;
  private final GmlImportService gmlImportService;
  private final FileResourceService fileResourceService;
  private final Notifier notifier;
  private final RenderService renderService;
  private final CsvImportService csvImportService;
  private final SchemaService schemaService;

  @Override
  public JobType getJobType() {
    return JobType.METADATA_IMPORT;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    progress.startingProcess("Metadata import started");
    MetadataImportParams params = (MetadataImportParams) config.getJobParameters();
    progress.startingStage("Loading file resource");
    FileResource data =
        progress.runStage(() -> fileResourceService.getExistingFileResource(config.getUid()));
    progress.startingStage("Loading file content");
    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      String contentType = data.getContentType();
      ImportReport report =
          switch (contentType) {
            case "application/json" -> metadataImportService.importMetadata(
                params, jsonToMetadataObjects(input), progress);
            case "application/csv" -> metadataImportService.importMetadata(
                params, csvToMetadataObjects(params, input), progress);
            case "application/xml" -> gmlImportService.importGml(input, params, progress);
            default -> null;
          };
      if (report == null) {
        progress.failedProcess("Import failed, no summary available");
        return;
      }

      if (report.hasErrorReports()) {
        report.forEachErrorReport(
            r ->
                progress.addError(
                    r.getErrorCode(),
                    r.getMainId(),
                    r.getMainKlass().getSimpleName(),
                    null,
                    r.getArgs()));
      }

      notifier.addJobSummary(config, report, ImportReport.class);
      Stats count = report.getStats();
      Consumer<String> endProcess =
          report.getStatus() == Status.ERROR ? progress::failedProcess : progress::completedProcess;
      endProcess.accept(
          "Import complete with status %s, %d created, %d updated, %d deleted, %d ignored"
              .formatted(
                  report.getStatus(),
                  count.getCreated(),
                  count.getUpdated(),
                  count.getDeleted(),
                  count.getIgnored()));
    } catch (IOException ex) {
      progress.failedProcess(ex);
    }
  }

  private MetadataObjects csvToMetadataObjects(MetadataImportParams params, InputStream input)
      throws IOException {
    Metadata metadata =
        csvImportService.fromCsv(
            input,
            new CsvImportOptions()
                .setImportClass(params.getCsvImportClass())
                .setFirstRowIsHeader(params.isFirstRowIsHeader()));
    return new MetadataObjects().addMetadata(schemaService.getMetadataSchemas(), metadata);
  }

  private MetadataObjects jsonToMetadataObjects(InputStream input) throws IOException {
    return new MetadataObjects(
        renderService.fromMetadata(
            StreamUtils.wrapAndCheckCompressionFormat(input), RenderFormat.JSON));
  }
}
