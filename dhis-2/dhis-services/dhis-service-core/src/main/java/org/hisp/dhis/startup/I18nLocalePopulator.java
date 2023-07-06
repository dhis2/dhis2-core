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
package org.hisp.dhis.startup;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.i18n.I18nLocaleService;
import org.hisp.dhis.i18n.locale.I18nLocale;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;

/**
 * Populates default I18nLocales if none exists.
 *
 * @author Lars Helge Overland
 */
@Slf4j
public class I18nLocalePopulator extends TransactionContextStartupRoutine {
  private final I18nLocaleService localeService;

  public I18nLocalePopulator(I18nLocaleService localeService) {
    checkNotNull(localeService);

    this.localeService = localeService;
  }

  private static final ImmutableSet<String> DEFAULT_LOCALES =
      ImmutableSet.of(
          "af", "ar", "bi", "am", "de", "dz", "en", "es", "fa", "fr", "gu", "hi", "id", "it", "km",
          "lo", "my", "ne", "nl", "no", "ps", "pt", "ru", "rw", "sw", "tg", "vi", "zh");

  @Override
  public void executeInTransaction() {
    int count = localeService.getI18nLocaleCount();

    if (count > 0) {
      return;
    }

    for (String locale : DEFAULT_LOCALES) {
      localeService.saveI18nLocale(new I18nLocale(new Locale(locale)));
    }

    log.info("Populated default locales");
  }
}
