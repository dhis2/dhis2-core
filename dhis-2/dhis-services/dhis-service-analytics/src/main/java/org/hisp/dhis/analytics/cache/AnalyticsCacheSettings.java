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
package org.hisp.dhis.analytics.cache;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.max;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.FIXED;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.PROGRESSIVE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_TWO_WEEKS;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TTL_MODE;
import static org.hisp.dhis.setting.SettingKey.CACHE_STRATEGY;

import java.util.Date;

import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

/**
 * Holds the configuration settings for the analytics caching.
 */
@Component
public class AnalyticsCacheSettings
{
    private final SystemSettingManager systemSettingManager;

    public AnalyticsCacheSettings( final SystemSettingManager systemSettingManager )
    {
        checkNotNull( systemSettingManager );
        this.systemSettingManager = systemSettingManager;
    }

    /**
     * Returns true if the analytics cache mode, at application level, is set to
     * PROGRESSIVE. If enabled, it overrides the fixed predefined settings.
     *
     * @see AnalyticsCacheTtlMode#PROGRESSIVE
     *
     * @return true if the current cache is enabled and set to PROGRESSIVE,
     *         false otherwise.
     */
    public boolean isProgressiveCachingEnabled()
    {
        final AnalyticsCacheTtlMode analyticsCacheMode = systemSettingManager
            .getSystemSetting( ANALYTICS_CACHE_TTL_MODE, AnalyticsCacheTtlMode.class );

        return PROGRESSIVE == analyticsCacheMode;
    }

    /**
     * Returns true if the analytics cache mode, at application level, is
     * correctly set to FIXED.
     *
     * @see AnalyticsCacheTtlMode#FIXED
     *
     * @return true if the current cache mode is set to FIXED, false otherwise.
     */
    public boolean isFixedCachingEnabled()
    {
        final AnalyticsCacheTtlMode analyticsCacheMode = systemSettingManager
            .getSystemSetting( ANALYTICS_CACHE_TTL_MODE, AnalyticsCacheTtlMode.class );

        final CacheStrategy cacheStrategy = systemSettingManager.getSystemSetting( CACHE_STRATEGY,
            CacheStrategy.class );

        return FIXED == analyticsCacheMode && cacheStrategy != null && cacheStrategy.hasExpirationTimeSet();
    }

    /**
     * Encapsulates the calculation of the progressive expiration time for the
     * analytics caching at application level, if the PROGRESSIVE mode is set.
     *
     * @param dateBeforeToday the date to be used during the calculation of the
     *        progressive expiration time.
     *
     * @return the expiration time computed based on the given
     *         "dateBeforeToday".
     */
    public long progressiveExpirationTimeOrDefault( final Date dateBeforeToday )
    {
        return new TimeToLive( dateBeforeToday, getProgressiveTtlFactorOrDefault() ).compute();
    }

    /**
     * Retrieves the expiration time in seconds based on the system settings
     * based on the {@link org.hisp.dhis.setting.SettingKey#CACHE_STRATEGY}. If
     * it says not to cache, return 0 so no caching will take place. Otherwise
     * return a long time. This is because we flush the analytics cache after on
     * analytics rebuild. For this purpose, two weeks is considered to be "a
     * long time". Two weeks is likely to be longer than until the next
     * analytics rebuild, and if it isn't, this ensures that all cache entries
     * will eventually be aged out.
     *
     * @see CacheStrategy
     *
     * @return the predefined expiration time set or 0 (ZERO) if nothing is set.
     */
    public long fixedExpirationTimeOrDefault()
    {
        final CacheStrategy cacheStrategy = systemSettingManager.getSystemSetting( CACHE_STRATEGY,
            CacheStrategy.class );

        return (NO_CACHE.equals( cacheStrategy ))
            ? NO_CACHE.toSeconds()
            : CACHE_TWO_WEEKS.toSeconds();
    }

    /**
     * Checks if any the caching feature (PROGRESSIVE or FIXED) is enabled.
     *
     * @return true if there is any expiration time set, false otherwise.
     */
    public boolean isCachingEnabled()
    {
        return isFixedCachingEnabled() || isProgressiveCachingEnabled();
    }

    /**
     * Returns the TTL factor set in system settings or 1 (when the factor is
     * set to ZERO or negative).
     *
     * @return the ttl factor
     */
    private int getProgressiveTtlFactorOrDefault()
    {
        final Integer ttlFactor = systemSettingManager.getIntegerSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR );

        return max( ttlFactor, 1 );
    }
}
