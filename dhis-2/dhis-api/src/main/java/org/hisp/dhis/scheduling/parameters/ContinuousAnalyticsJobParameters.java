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
package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@NoArgsConstructor
public class ContinuousAnalyticsJobParameters implements JobParameters {
  /** The hour of day at which the full analytics table update will be invoked. */
  @JsonProperty private Integer fullUpdateHourOfDay = 0;

  /** The number of last years of data to include in the full analytics table update. */
  @JsonProperty private Integer lastYears;

  /** The types of analytics tables for which to skip update. */
  @JsonProperty private Set<AnalyticsTableType> skipTableTypes = new HashSet<>();

  public ContinuousAnalyticsJobParameters(
      Integer fullUpdateHourOfDay, Integer lastYears, Set<AnalyticsTableType> skipTableTypes) {
    this.fullUpdateHourOfDay = fullUpdateHourOfDay;
    this.lastYears = lastYears;
    this.skipTableTypes = skipTableTypes;
  }
}
