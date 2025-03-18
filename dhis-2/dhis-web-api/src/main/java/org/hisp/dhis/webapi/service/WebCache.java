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
package org.hisp.dhis.webapi.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.common.cache.Cacheability.PRIVATE;
import static org.hisp.dhis.common.cache.Cacheability.PUBLIC;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.CacheControl.noCache;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.cache.AnalyticsCacheSettings;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;

/**
 * This component encapsulates the caching settings and object definitions related to the caching at
 * the HTTP level.
 */
@Component
@RequiredArgsConstructor
public class WebCache {

  private final SystemSettingsProvider settingsProvider;
  private final AnalyticsCacheSettings analyticsCacheSettings;

  /**
   * Defines and return a CacheControl object with the correct expiration time and cacheability
   * based on the internal system settings defined by the user. The expiration time is defined
   * through the Enum {@link CacheStrategy}
   *
   * @param cacheStrategy
   * @return a CacheControl object configured based on current system settings.
   */
  public CacheControl getCacheControlFor(CacheStrategy cacheStrategy) {
    CacheControl cacheControl;

    if (RESPECT_SYSTEM_SETTING == cacheStrategy) {
      cacheStrategy = settingsProvider.getCurrentSettings().getCacheStrategy();
    }

    boolean cacheStrategyHasExpirationTimeSet = cacheStrategy != null && cacheStrategy != NO_CACHE;

    if (cacheStrategyHasExpirationTimeSet) {
      cacheControl = maxAge(cacheStrategy.toSeconds(), SECONDS);

      setCacheabilityFor(cacheControl);
    } else {
      cacheControl = noCache();
    }

    return cacheControl;
  }

  /**
   * Defines and return a CacheControl object with the correct expiration time and cacheability,
   * based on a provided date, in SECONDS.
   *
   * @param latestEndDate
   * @return a CacheControl object configured based on current cacheability settings and the
   *     provided time to live.
   */
  public CacheControl getCacheControlFor(Date latestEndDate) {
    CacheControl cacheControl;

    long timeToLive = analyticsCacheSettings.progressiveExpirationTimeOrDefault(latestEndDate);

    if (timeToLive > 0) {
      cacheControl = maxAge(timeToLive, SECONDS);

      setCacheabilityFor(cacheControl);
    } else {
      cacheControl = noCache();
    }

    return cacheControl;
  }

  /**
   * See {@link AnalyticsCacheSettings#isProgressiveCachingEnabled()}
   *
   * @return true if progressive caching is enabled, false otherwise
   */
  public boolean isProgressiveCachingEnabled() {
    return analyticsCacheSettings.isProgressiveCachingEnabled();
  }

  /**
   * Sets the cacheability (defined as system setting) into the given CacheControl.
   *
   * @see SystemSettings#getCacheability()
   * @param cacheControl where cacheability will be set.
   */
  private void setCacheabilityFor(CacheControl cacheControl) {
    Cacheability cacheability = settingsProvider.getCurrentSettings().getCacheability();

    if (PUBLIC == cacheability) {
      cacheControl.cachePublic();
    } else if (PRIVATE == cacheability) {
      cacheControl.cachePrivate();
    }
  }
}
