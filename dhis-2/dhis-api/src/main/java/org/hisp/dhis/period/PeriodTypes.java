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

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.input.Fields;
import org.hisp.dhis.translation.JsonTranslations;
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

  @OpenApi.Shared(name = "PeriodTypes")
  public record Output(@Nonnull Locale locale, @Nonnull List<PeriodTypeEntry.Output> entries) {

    public Output {
      requireNonNull(locale);
      requireNonNull(entries);
    }
  }

  /** A {@link PeriodType} as exposed in the web API with all its display properties joined in. */
  public record PeriodTypeEntry(
      @Nonnull PeriodTypeEnum type,
      @Nonnull String isoDuration,
      @Nonnull String isoFormat,
      int frequencyOrder,
      @Nonnull Labels labels,
      @Nonnull Map<RelativePeriodEnum, Labels> relativePeriods) {

    public PeriodTypeEntry {
      requireNonNull(type);
      requireNonNull(isoDuration);
      requireNonNull(isoFormat);
      requireNonNull(labels);
      requireNonNull(relativePeriods);
    }

    @OpenApi.Shared(name = "PeriodTypeEntry")
    public record Output(
        @CheckForNull PeriodTypeEnum type,
        @CheckForNull String displayName,
        @CheckForNull String isoDuration,
        @CheckForNull String isoFormat,
        @CheckForNull Integer frequencyOrder,
        @CheckForNull String defaultName,
        @CheckForNull String label,
        @CheckForNull String displayLabel,
        @CheckForNull List<Translation> translations,
        @CheckForNull Map<RelativePeriodEnum, Labels> relativePeriods) {}

    public Output select(Fields fields) {
      boolean all = fields.fields().isEmpty() || fields.contains(":all");
      if (all)
        return new Output(
            type,
            labels.displayName,
            isoDuration,
            isoFormat,
            frequencyOrder,
            labels.defaultName,
            labels.label,
            labels.displayLabel,
            labels.translations,
            relativePeriods);
      return new Output(
          fields.contains("name") ? type : null,
          fields.contains("displayName") ? labels.displayName : null,
          fields.contains("isoDuration") ? isoDuration : null,
          fields.contains("isoFormat") ? isoFormat : null,
          fields.contains("frequencyOrder") ? frequencyOrder : null,
          fields.contains("defaultName") ? labels.defaultName : null,
          fields.contains("label") ? labels.label : null,
          fields.contains("displayLabel") ? labels.displayLabel : null,
          fields.contains("translations") ? labels.translations : null,
          fields.contains("relativePeriods") ? relativePeriods : null);
    }
  }

  public record Labels(
      @CheckForNull String defaultName,
      @CheckForNull String label,
      @CheckForNull List<Translation> translations,
      @CheckForNull String displayLabel,
      @Nonnull String displayName) {

    public static Labels of(
        @Nonnull Locale locale,
        @Nonnull String defaultName,
        @CheckForNull String label,
        @CheckForNull JsonTranslations translations) {
      String displayLabel = null;
      List<Translation> trans = List.of();
      if (translations != null) {
        trans = translations.translationsList();
        displayLabel = translations.translationValue(locale);
      }
      if (displayLabel == null) displayLabel = label;
      String displayName = displayLabel;
      if (displayName == null) displayName = defaultName;
      return new Labels(defaultName, label, trans, displayLabel, displayName);
    }
  }
}
