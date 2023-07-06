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
package org.hisp.dhis.tracker.validation;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;

/**
 * Convenience class for creating a tracker bundle and calling validation and commit.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
@Builder
@Slf4j
public class ValidateAndCommitTestUnit {
  private TrackerValidationService trackerValidationService;

  private TrackerBundleService trackerBundleService;

  private TrackerBundle trackerBundle;

  private TrackerValidationReport validationReport;

  private TrackerBundleReport commitReport;

  private final TrackerImportParams trackerImportParams;

  private Exception commitException;

  private boolean forceCommit;

  @Builder.Default
  private final TrackerImportStrategy trackerImportStrategy =
      TrackerImportStrategy.CREATE_AND_UPDATE;

  /**
   * Runs the work
   *
   * @return an instance of it self to retrieve the commit and validation results from later.
   */
  public ValidateAndCommitTestUnit invoke() {
    trackerImportParams.setImportStrategy(trackerImportStrategy);

    trackerBundle = trackerBundleService.create(trackerImportParams);

    validationReport = trackerValidationService.validate(trackerBundle);

    if (validationReport.getErrors().isEmpty() || forceCommit) {
      try {
        commitReport = trackerBundleService.commit(trackerBundle);
      } catch (Exception e) {
        log.info("Failed to commit. Exception:" + e.getMessage());
        commitException = e;
      }
    }

    return this;
  }
}
