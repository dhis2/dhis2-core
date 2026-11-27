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
package org.hisp.dhis.dataintegrity;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.scheduling.JobProgress;

/**
 * @author Jan Bernitt
 */
public interface DataIntegrityService {

  default @Nonnull Collection<DataIntegrityCheck> getDataIntegrityChecks() {
    return getDataIntegrityChecks(Set.of());
  }

  @Nonnull
  Collection<DataIntegrityCheck> getDataIntegrityChecks(Set<String> checks);

  /**
   * @param checks names of the checks, or empty for all checks
   * @param timeout milliseconds to wait for missing results
   * @param maxAge only give results that finished within this duration, or {@code null} for results
   *     of any age
   * @return the cached summary results by check name
   */
  @Nonnull
  Map<String, DataIntegritySummary> getSummaries(
      @Nonnull Set<String> checks, long timeout, @CheckForNull Duration maxAge);

  @Nonnull
  Map<String, DataIntegrityDetails> getDetails(@Nonnull Set<String> checks, long timeout);

  void runSummaryChecks(@Nonnull Set<String> checks, JobProgress progress);

  void runDetailsChecks(@Nonnull Set<String> checks, JobProgress progress);

  @Nonnull
  Set<String> getRunningSummaryChecks();

  @Nonnull
  Set<String> getRunningDetailsChecks();

  /**
   * @param maxAge only give checks with a result that finished within this duration, or {@code
   *     null} for results of any age
   * @return names of the checks that have a cached summary result
   */
  @Nonnull
  Set<String> getCompletedSummaryChecks(@CheckForNull Duration maxAge);

  @Nonnull
  Set<String> getCompletedDetailsChecks();
}
