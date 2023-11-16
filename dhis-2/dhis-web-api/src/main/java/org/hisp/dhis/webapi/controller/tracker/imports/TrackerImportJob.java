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
package org.hisp.dhis.webapi.controller.tracker.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Stats;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackerImportJob implements Job {
  private final TrackerImportService trackerImportService;
  private final FileResourceService fileResourceService;
  private final Notifier notifier;

  @Override
  public JobType getJobType() {
    return JobType.TRACKER_IMPORT_JOB;
  }

  @Override
  public void execute(JobConfiguration config, JobProgress progress) {
    progress.startingProcess("Tracker import started");
    TrackerImportParams params = (TrackerImportParams) config.getJobParameters();
    progress.startingStage("Loading file resource");
    FileResource data =
        progress.runStage(() -> fileResourceService.getExistingFileResource(config.getUid()));
    progress.startingStage("Loading file content");
    try (InputStream input =
        progress.runStage(() -> fileResourceService.getFileResourceContent(data))) {
      ImportReport report =
          trackerImportService.importTracker(params, toTrackerObjects(input), progress);
      if (report == null) {
        progress.failedProcess("Import failed, no summary available");
        return;
      }
      notifier.addJobSummary(config, report, ImportReport.class);

      if (report.getValidationReport().hasErrors()) {
        report
            .getValidationReport()
            .getErrors()
            .forEach(
                e ->
                    progress.addError(
                        ValidationCode.valueOf(e.getErrorCode()), e.getUid(), e.getTrackerType()));
        // TODO args
      }

      Stats stats = report.getStats();
      Consumer<String> endProcess =
          report.getStatus() == Status.ERROR ? progress::failedProcess : progress::completedProcess;
      endProcess.accept(
          "Import complete with status %s, %d created, %d updated, %d deleted, %d ignored"
              .formatted(
                  report.getStatus(),
                  stats.getCreated(),
                  stats.getUpdated(),
                  stats.getDeleted(),
                  stats.getIgnored()));
    } catch (Exception ex) {
      progress.failedProcess(ex);
    }
  }

  private TrackerObjects toTrackerObjects(InputStream input)
      throws IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(input);
    return (TrackerObjects) ois.readObject();
  }
}
