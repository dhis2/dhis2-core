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
package org.hisp.dhis.i18n.ui.locale;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManagerException;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserSettingsService;
import org.springframework.stereotype.Component;

/**
 * @author Torgeir Lorange Ostby
 */
@Component("org.hisp.dhis.i18n.locale.LocaleManager")
@RequiredArgsConstructor
public class UserSettingLocaleManager implements LocaleManager {

  private final UserSettingsService userSettingsService;
  private final ResourceBundleManager resourceBundleManager;

  @Override
  public Locale getCurrentLocale() {

    // TODO: MAS: Called by interceptor for old web ui only
    if (CurrentUserUtil.hasCurrentUser()) {
      Locale locale = getUserSelectedLocale();

      if (locale != null) {
        return locale;
      }
    }

    return DEFAULT_LOCALE;
  }

  @Override
  public void setCurrentLocale(Locale locale) {
    try {
      userSettingsService.put("keyUiLocale", locale);
    } catch (NotFoundException | BadRequestException ex) {
      // this should never happen as this key-value combination is valid
      throw new IllegalArgumentException(ex);
    }
  }

  @Override
  public List<Locale> getLocalesOrderedByPriority() {
    List<Locale> locales = new ArrayList<>();
    Locale userLocale = getUserSelectedLocale();

    if (userLocale != null) {
      locales.add(userLocale);
    }

    locales.add(DEFAULT_LOCALE);
    return locales;
  }

  @CheckForNull
  public Locale getUserSelectedLocale() {
    return UserSettings.getCurrentSettings().getUserUiLocale();
  }

  @Override
  public Locale getFallbackLocale() {
    return DEFAULT_LOCALE;
  }

  @Override
  public List<Locale> getAvailableLocales() {
    try {
      return resourceBundleManager.getAvailableLocales();
    } catch (ResourceBundleManagerException ex) {
      throw new RuntimeException(ex);
    }
  }
}
