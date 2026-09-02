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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.input.Fields;
import org.hisp.dhis.translation.Translation;

/**
 * List of (all) {@link PeriodType}s with their labels as exposed in the web API.
 *
 * @param locale the locale for which labels were resolved
 * @param entries the list of period types
 * @since 2.44
 */
public record PeriodTypes(@Nonnull Locale locale, @Nonnull List<PeriodTypeEntry> entries) {

  public PeriodTypes {
    requireNonNull(locale);
    requireNonNull(entries);
  }

  /** A {@link PeriodType} as exposed in the web API with all its display properties joined in. */
  public record PeriodTypeEntry(
      @JsonProperty @Nonnull @OpenApi.Description("The ID of the period type") String name,
      @JsonProperty @Nonnull String isoDuration,
      @JsonProperty @Nonnull String isoFormat,
      @JsonProperty int frequencyOrder,
      @JsonProperty @CheckForNull @OpenApi.Description("The i18n translation of `name`")
          String defaultName,
      @JsonProperty
          @CheckForNull
          @OpenApi.Description(
              "An optional override for the `name` taking precedence over the `defaultName`")
          String label,
      @JsonProperty @Nonnull List<Translation> translations,
      @JsonProperty
          @CheckForNull
          @OpenApi.Description(
              "An optional translation for `label` resolved for a specific `locale` (falls back to `label` when no translation exists)")
          String displayLabel,
      @JsonProperty
          @Nonnull
          @OpenApi.Description(
              "The name to display in the UI computed from all sources resolved for a specific `locale`")
          String displayName) {

    public PeriodTypeEntry {
      requireNonNull(name);
      requireNonNull(isoDuration);
      requireNonNull(isoFormat);
      requireNonNull(translations);
    }
  }

  public record Output(
      @Nonnull Locale locale, @Nonnull List<PeriodTypeEntry> entries, @Nonnull Fields fields) {

    public Output {
      requireNonNull(locale);
      requireNonNull(entries);
      requireNonNull(fields);
    }
  }
}
