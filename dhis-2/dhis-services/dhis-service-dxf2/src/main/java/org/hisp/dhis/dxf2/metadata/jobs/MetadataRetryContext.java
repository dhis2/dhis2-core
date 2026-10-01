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
package org.hisp.dhis.dxf2.metadata.jobs;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Holds per-run state for metadata sync retries (attributes + last failure).
 *
 * <p>Spring Framework 7 core retry no longer provides a mutable {@code RetryContext} attribute map,
 * so this class owns that state itself.
 *
 * @author aamerm
 */
@Slf4j
@Component("metadataRetryContext")
@Scope("prototype")
public class MetadataRetryContext {
  private final Map<String, Object> attributes = new HashMap<>();

  private int attempt;

  private Throwable lastThrowable;

  /** Clears attributes/attempt state before a new retry loop. */
  public void reset() {
    attributes.clear();
    attempt = 0;
    lastThrowable = null;
  }

  /** Called at the start of each attempt inside {@code RetryTemplate.execute}. */
  public void beginAttempt() {
    attempt++;
    log.info("Now trying. Current count: " + attempt);
  }

  public int getAttempt() {
    return attempt;
  }

  public Object getAttribute(String key) {
    return attributes.get(key);
  }

  public Throwable getLastThrowable() {
    return lastThrowable;
  }

  public void setLastThrowable(Throwable lastThrowable) {
    this.lastThrowable = lastThrowable;
  }

  public void updateRetryContext(String stepKey, String message, MetadataVersion version) {
    attributes.put(stepKey, message);

    if (version != null) {
      attributes.put(MetadataSyncJob.VERSION_KEY, version);
    }
  }

  public void updateRetryContext(
      String stepKey, String message, MetadataVersion version, MetadataSyncSummary summary) {
    updateRetryContext(stepKey, message, version);

    if (summary != null) {
      setupImportReport(summary.getImportReport());
    }
  }

  // ----------------------------------------------------------------------------------------
  // Private Methods
  // ----------------------------------------------------------------------------------------

  private void setupImportReport(ImportReport importReport) {
    Status status = importReport.getStatus();

    if (Status.ERROR == status) {
      StringBuilder report = new StringBuilder();
      importReport.forEachErrorReport(errorReport -> report.append(errorReport.toString() + "\n"));
      attributes.put(MetadataSyncJob.METADATA_SYNC_REPORT, report.toString());
    }
  }
}
