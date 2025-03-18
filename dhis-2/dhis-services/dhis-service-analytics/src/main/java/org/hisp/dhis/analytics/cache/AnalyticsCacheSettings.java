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
package org.hisp.dhis.analytics.cache;

import static java.lang.Math.max;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.FIXED;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.PROGRESSIVE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_TWO_WEEKS;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.stereotype.Component;

/** Holds the configuration settings for the analytics caching. */
@Component
@RequiredArgsConstructor
public class AnalyticsCacheSettings {

  private final SystemSettingsProvider settingsProvider;

  /**
   * Returns true if the analytics cache mode, at application level, is set to PROGRESSIVE. If
   * enabled, it overrides the fixed predefined settings.
   *
   * @see AnalyticsCacheTtlMode#PROGRESSIVE
   * @return true if the current cache is enabled and set to PROGRESSIVE, false otherwise.
   */
  public boolean isProgressiveCachingEnabled() {
    return PROGRESSIVE == settingsProvider.getCurrentSettings().getAnalyticsCacheTtlMode();
  }

  /**
   * Returns true if the analytics cache mode, at application level, is correctly set to FIXED.
   *
   * @see AnalyticsCacheTtlMode#FIXED
   * @return true if the current cache mode is set to FIXED, false otherwise.
   */
  public boolean isFixedCachingEnabled() {
    SystemSettings settings = settingsProvider.getCurrentSettings();
    return FIXED == settings.getAnalyticsCacheTtlMode()
        && settings.getCacheStrategy().hasExpirationTimeSet();
  }

  /**
   * Encapsulates the calculation of the progressive expiration time for the analytics caching at
   * application level, if the PROGRESSIVE mode is set.
   *
   * @param dateBeforeToday the date to be used during the calculation of the progressive expiration
   *     time.
   * @return the expiration time computed based on the given "dateBeforeToday".
   */
  public long progressiveExpirationTimeOrDefault(Date dateBeforeToday) {
    return new TimeToLive(dateBeforeToday, getProgressiveTtlFactorOrDefault()).compute();
  }

  /**
   * Retrieves the expiration time in seconds based on the system settings based on the {@link
   * SystemSettings#getCacheStrategy()}. If it says not to cache, return 0 so no caching will take
   * place. Otherwise return a long time. This is because we flush the analytics cache after on
   * analytics rebuild. For this purpose, two weeks is considered to be "a long time". Two weeks is
   * likely to be longer than until the next analytics rebuild, and if it isn't, this ensures that
   * all cache entries will eventually be aged out.
   *
   * @see CacheStrategy
   * @return the predefined expiration time set or 0 (ZERO) if nothing is set.
   */
  public long fixedExpirationTimeOrDefault() {
    return settingsProvider.getCurrentSettings().getCacheStrategy() == NO_CACHE
        ? NO_CACHE.toSeconds()
        : CACHE_TWO_WEEKS.toSeconds();
  }

  /**
   * Checks if any the caching feature (PROGRESSIVE or FIXED) is enabled.
   *
   * @return true if there is any expiration time set, false otherwise.
   */
  public boolean isCachingEnabled() {
    return isFixedCachingEnabled() || isProgressiveCachingEnabled();
  }

  /**
   * Returns the TTL factor set in system settings or 1 (when the factor is set to ZERO or
   * negative).
   *
   * @return the ttl factor
   */
  private int getProgressiveTtlFactorOrDefault() {
    return max(settingsProvider.getCurrentSettings().getAnalyticsCacheProgressiveTtlFactor(), 1);
  }
}
