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
package org.hisp.dhis.dxf2.geojson.job;

import java.io.IOException;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportReport;
import org.hisp.dhis.dxf2.geojson.GeoJsonService;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.GeoJsonImportJobParams;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class GeoJsonImportJob implements Job {

  private final GeoJsonService geoJsonService;
  private final FileResourceService fileResourceService;
  private final Notifier notifier;

  @Override
  public JobType getJobType() {
    return JobType.GEOJSON_IMPORT;
  }

  @Override
  public void execute(JobConfiguration jobConfig, JobProgress progress) {
    progress.startingProcess("GeoJSON import started");
    GeoJsonImportJobParams jobParams = (GeoJsonImportJobParams) jobConfig.getJobParameters();

    progress.startingStage("Loading file resource");
    FileResource data =
        progress.runStage(() -> fileResourceService.getFileResource(jobConfig.getUid()));
    progress.startingStage("Loading file content");

    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      progress.startingStage("Importing GeoJSON");
      GeoJsonImportReport report =
          progress.runStage(() -> geoJsonService.importGeoData(jobParams, input));
      if (report == null) {
        progress.failedProcess("Import failed, no report available");
        return;
      }
      if (report.hasConflicts()) {
        for (ImportConflict c : report.getConflicts())
          progress.addError(c.getErrorCode(), c.getObject(), c.getGroupingKey(), c.getArgs());
      }

      ImportCount count = report.getImportCount();
      progress.completedProcess(
          "GeoJSON import completed with status {}, {} created, {} updated, {} deleted, {} ignored",
          report.getStatus(),
          count.getImported(),
          count.getUpdated(),
          count.getDeleted(),
          count.getIgnored());

      notifier.addJobSummary(jobConfig, report, GeoJsonImportReport.class);
    } catch (IOException e) {
      progress.failedProcess(e);
    }
  }
}
