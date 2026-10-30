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
package org.hisp.dhis.tracker.imports;

import javax.annotation.Nonnull;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.RecordingJobProgress;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackerImportService {
  /**
   * Import objects using provided params. Takes the objects through all phases of the importer from
   * preheating to validation, and then finished with a commit (unless the {@link
   * TrackerImportParams#getImportMode()} is {@link
   * org.hisp.dhis.tracker.imports.bundle.TrackerBundleMode#VALIDATE} only).
   *
   * <p>{@link TrackerObjects} need to be flat. Each entity needs to be in the top level field like
   * {@link TrackerObjects#getEnrollments()} for an enrollment, even though you could put its' data
   * onto its tracked entitys' enrollment field in {@link TrackerObjects#getTrackedEntities()}.
   *
   * @param params import parameters
   * @param trackerObjects the objects to import
   * @return report giving status of import (and any errors)
   */
  @Nonnull
  default ImportReport importTracker(
      @Nonnull TrackerImportParams params, @Nonnull TrackerObjects trackerObjects) {
    return importTracker(params, trackerObjects, RecordingJobProgress.transitory());
  }

  /**
   * Import object using provided params. Takes the objects through all phases of the importer from
   * preheating to validation, and then finished with a commit (unless its validate only)
   *
   * @param params Parameters for import
   * @param trackerObjects the objects to import
   * @param jobProgress to track import progress
   * @return Report giving status of import (and any errors)
   */
  @Nonnull
  ImportReport importTracker(
      @Nonnull TrackerImportParams params,
      @Nonnull TrackerObjects trackerObjects,
      @Nonnull JobProgress jobProgress);

  /**
   * Build the report based on the mode selected by the client.
   *
   * @param importReport report with all the data collected during import
   * @return TrackerImportReport report with filtered data based on reportMode
   */
  @Nonnull
  ImportReport buildImportReport(
      @Nonnull ImportReport importReport, @Nonnull TrackerBundleReportMode reportMode);
}
