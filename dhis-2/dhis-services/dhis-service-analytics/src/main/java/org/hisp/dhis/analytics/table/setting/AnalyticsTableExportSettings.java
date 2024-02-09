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
package org.hisp.dhis.analytics.table.setting;

import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_ORDERING;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_UNLOGGED;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_PERIOD_YEARS_OFFSET;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

/**
 * Component responsible for exposing analytics table export settings. Can hold settings living in
 * configuration files (ie. dhis.conf) or in system settings.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class AnalyticsTableExportSettings {
  private final DhisConfigurationProvider dhisConfigurationProvider;

  private final SystemSettingManager systemSettingManager;

  /**
   * Returns the setting indicating whether resource and analytics tables should be logged or
   * unlogged.
   *
   * @return the {@link Logged} parameter.
   */
  public Logged getTableLogged() {
    if (dhisConfigurationProvider.isEnabled(ANALYTICS_TABLE_UNLOGGED)) {
      return Logged.UNLOGGED;
    }

    return Logged.LOGGED;
  }

  public boolean isTableOrdering() {
    return dhisConfigurationProvider.isEnabled(ANALYTICS_TABLE_ORDERING);
  }

  /**
   * Returns the years' offset defined for the period generation. See {@link
   * ANALYTICS_MAX_PERIOD_YEARS_OFFSET}.
   *
   * @return the offset defined in system settings, or null if nothing is set.
   */
  public Integer getMaxPeriodYearsOffset() {
    return systemSettingManager.getIntSetting(ANALYTICS_MAX_PERIOD_YEARS_OFFSET) < 0
        ? null
        : systemSettingManager.getIntSetting(ANALYTICS_MAX_PERIOD_YEARS_OFFSET);
  }
}
