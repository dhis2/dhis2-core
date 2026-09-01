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

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.JsonTranslations;
import org.hisp.dhis.translation.Translation;

/**
 * @apiNote extracted from {@link PeriodStore}
 * @since 2.44
 */
public interface PeriodTypeStore {

  /**
   * Adds a PeriodType.
   *
   * @param periodType the PeriodType to add.
   */
  void addPeriodType(@Nonnull PeriodType periodType);

  /**
   * Updates the label of the given period type name.
   *
   * @param name the {@link PeriodType}'s name.
   * @param label the new label, null or empty to erase
   * @param locale when null label is the override for the name not associated with a locale,
   *     otherwise it is a translation for the given locale
   */
  boolean updatePeriodTypeLabel(
      @Nonnull String name, @CheckForNull String label, @CheckForNull Locale locale);

  /**
   * Replaces the period type's translation labels with the given ones
   *
   * @param name of the type (key)
   * @param translations labels in different languages
   * @return true, if a change occurred, false if no row was affected
   */
  boolean updatePeriodTypeLabel(
      @Nonnull String name, @Nonnull Collection<Translation> translations);

  /**
   * Returns all PeriodTypes.
   *
   * @return a list of all PeriodTypes, or an empty list if there are no PeriodTypes.
   */
  @Nonnull
  List<PeriodType> getAllPeriodTypes();

  /**
   * @return the label information for all periods
   */
  List<PeriodTypeLabels> getAllPeriodTypeLabels();

  /**
   * Label information for a period type that is stored in the DB
   *
   * @param name the key
   * @param label override on the properties based i18n translation
   * @param translations local specific translations
   */
  record PeriodTypeLabels(
      @Nonnull String name, @CheckForNull String label, @Nonnull JsonTranslations translations) {

    public PeriodTypeLabels {
      requireNonNull(name);
      requireNonNull(translations);
    }
  }
}
