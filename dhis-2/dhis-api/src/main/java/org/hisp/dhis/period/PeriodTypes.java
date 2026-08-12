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

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.translation.Translation;

/**
 * List of (all) {@link PeriodType}s with their labels as exposed in the web API.
 *
 * @param locale the locale for which labels were resolved
 * @param periodTypes the list of period types
 * @since 2.44
 */
public record PeriodTypes(
    @JsonProperty @Nonnull Locale locale, @JsonProperty @Nonnull List<Entry> periodTypes) {

  public PeriodTypes {
    requireNonNull(locale);
    requireNonNull(periodTypes);
  }

  /** A {@link PeriodType} as exposed in the web API with all its display properties joined in. */
  public record Entry(
      @OpenApi.Description("The ID of the period type") @Nonnull @JsonProperty String name,
      @JsonProperty String isoDuration,
      @JsonProperty String isoFormat,
      @JsonProperty Integer frequencyOrder,
      @OpenApi.Description("The i18n translation of `name` or the custom override for it")
          @JsonProperty
          String label,
      @JsonProperty List<Translation> translations,
      @OpenApi.Description("An optional translation for `label` resolved for a specific `locale`")
          @JsonProperty
          String displayLabel,
      @OpenApi.Description(
              "The name to display in the UI computed from all sources resolved for a specific `locale`")
          @JsonProperty
          String displayName) {

    public Entry {
      requireNonNull(name);
      // all other properties may be null due to fields-filtering
    }
  }
}
