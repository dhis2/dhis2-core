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
package org.hisp.dhis.common.cache;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hisp.dhis.util.DateUtils.getSecondsUntilTomorrow;

/**
 * CacheStrategies express web request caching settings. Note that {@link #RESPECT_SYSTEM_SETTING}
 * should only be used on a per-object-basis (i.e. never as a system wide setting).
 *
 * @author Halvdan Hoem Grelland
 */
public enum CacheStrategy {
  NO_CACHE,
  CACHE_1_MINUTE,
  CACHE_5_MINUTES,
  CACHE_10_MINUTES,
  CACHE_15_MINUTES,
  CACHE_30_MINUTES,
  CACHE_1_HOUR,
  CACHE_6AM_TOMORROW,
  CACHE_TWO_WEEKS,
  RESPECT_SYSTEM_SETTING;

  public Long toSeconds() {
    switch (this) {
      case CACHE_1_MINUTE:
        return MINUTES.toSeconds(1);
      case CACHE_5_MINUTES:
        return MINUTES.toSeconds(5);
      case CACHE_10_MINUTES:
        return MINUTES.toSeconds(10);
      case CACHE_15_MINUTES:
        return MINUTES.toSeconds(15);
      case CACHE_30_MINUTES:
        return MINUTES.toSeconds(30);
      case CACHE_1_HOUR:
        return HOURS.toSeconds(1);
      case CACHE_TWO_WEEKS:
        return DAYS.toSeconds(14);
      case CACHE_6AM_TOMORROW:
        return getSecondsUntilTomorrow(6);
      case NO_CACHE:
        return 0l;
      case RESPECT_SYSTEM_SETTING:
      default:
        throw new UnsupportedOperationException();
    }
  }

  public boolean hasExpirationTimeSet() {
    return this != NO_CACHE && this != RESPECT_SYSTEM_SETTING;
  }
}
