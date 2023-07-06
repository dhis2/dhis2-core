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
package org.hisp.dhis.tracker.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.tracker.TrackerType;

/**
 * The Bundle Report is responsible for aggregating the outcome of the persistence stage of the
 * Tracker Import.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerBundleReport {
  @JsonProperty @Builder.Default private TrackerStatus status = TrackerStatus.OK;

  @JsonProperty @Builder.Default
  private Map<TrackerType, TrackerTypeReport> typeReportMap = new HashMap<>();

  @JsonProperty
  public TrackerStats getStats() {
    TrackerStats stats = new TrackerStats();
    typeReportMap.values().forEach(tr -> stats.merge(tr.getStats()));

    return stats;
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  /**
   * Are there any errors present?
   *
   * @return true or false depending on any errors found in type reports
   */
  public boolean isEmpty() {
    return typeReportMap.keySet().stream().allMatch(k -> typeReportMap.get(k).isEmpty());
  }
}
