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
package org.hisp.dhis.analytics.table.setting;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.db.model.Logged.LOGGED;
import static org.hisp.dhis.db.model.Logged.UNLOGGED;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_SKIP_COLUMN;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_SKIP_INDEX;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_UNLOGGED;
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.SYSTEM_DEFINED;

import com.google.common.collect.Lists;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.period.PeriodDataProvider.PeriodSource;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.stereotype.Component;

/**
 * Component responsible for exposing analytics table settings. Provides settings living in
 * configuration files (dhis.conf) and in system settings.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class AnalyticsTableSettings {
  private final DhisConfigurationProvider config;

  private final SystemSettingsProvider settingsProvider;

  /**
   * Returns the setting indicating whether resource and analytics tables should be logged or
   * unlogged.
   *
   * @return the {@link Logged} parameter.
   */
  public Logged getTableLogged() {
    if (config.isEnabled(ANALYTICS_TABLE_UNLOGGED)) {
      return UNLOGGED;
    }

    return LOGGED;
  }

  /**
   * Returns the years' offset defined for the period generation configured by {@link
   * SystemSettings#getAnalyticsPeriodYearsOffset()}
   *
   * @return the offset defined in system settings, or null if nothing is set.
   */
  public Integer getMaxPeriodYearsOffset() {
    int yearsOffset = settingsProvider.getCurrentSettings().getAnalyticsPeriodYearsOffset();
    return yearsOffset < 0 ? null : yearsOffset;
  }

  /**
   * Returns the {@link PeriodSource} based on the max years offset.
   *
   * @return the {@link PeriodSource}.
   */
  public PeriodSource getPeriodSource() {
    return getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE;
  }

  /**
   * Indicates whether an analytics database instance is configured.
   *
   * @return true if an analytics database instance is configured.
   */
  public boolean isAnalyticsDatabase() {
    return config.isAnalyticsDatabaseConfigured();
  }

  /**
   * Returns a set of dimension identifiers for which to skip building indexes for columns on
   * analytics tables.
   *
   * @return a set of dimension identifiers.
   */
  public Set<String> getSkipIndexDimensions() {
    return toSet(config.getProperty(ANALYTICS_TABLE_SKIP_INDEX));
  }

  /**
   * Returns a set of dimension identifiers for which to skip creating columns for analytics tables.
   *
   * @return a set of dimension identifiers.
   */
  public Set<String> getSkipColumnDimensions() {
    return toSet(config.getProperty(ANALYTICS_TABLE_SKIP_COLUMN));
  }

  /**
   * Splits the given value on comma, and returns the values as a set.
   *
   * @param value the value.
   * @return a set of values.
   */
  Set<String> toSet(String value) {
    if (isBlank(value)) {
      return Set.of();
    }

    return Lists.newArrayList(value.split(",")).stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .collect(Collectors.toSet());
  }

  /**
   * Converts the boolean enabled flag to a {@link Skip} value.
   *
   * @param enabled the boolean enabled flag.
   * @return a {@link Skip} value.
   */
  Skip toSkip(boolean enabled) {
    return enabled ? Skip.INCLUDE : Skip.SKIP;
  }
}
