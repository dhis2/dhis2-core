/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.period;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Value;
import org.hisp.dhis.i18n.I18n;

public record PeriodTypeResponse(@JsonProperty List<PeriodTypeEntry> periodTypes) {
  /** A {@link PeriodType} as exposed in the web API with all its display properties joined in. */
  @Value
  public static class PeriodTypeEntry {
    @JsonProperty String name;
    @JsonProperty String displayName;
    @JsonProperty String isoDuration;
    @JsonProperty String isoFormat;
    @JsonProperty int frequencyOrder;
    @JsonProperty String label;
    @JsonProperty String displayLabel;

    public PeriodTypeEntry(@Nonnull PeriodType type, PeriodTypeLabels labels, I18n i18n) {
      this.name = type.getName();
      this.displayName = type.getDisplayName(i18n);
      this.frequencyOrder = type.getFrequencyOrder();
      this.isoDuration = type.getIso8601Duration();
      this.isoFormat = type.getIsoFormat();
      this.label = labels == null ? null : labels.label();
      this.displayLabel = labels == null ? null : labels.displayLabel();
    }
  }
}
