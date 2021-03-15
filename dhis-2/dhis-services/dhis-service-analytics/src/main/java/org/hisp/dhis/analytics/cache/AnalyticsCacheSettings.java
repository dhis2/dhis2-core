/*
 * Copyright (c) 2004-2021, University of Oslo
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
        final AnalyticsCacheTtlMode analyticsCacheMode = (AnalyticsCacheTtlMode) systemSettingManager
            .getSystemSetting( ANALYTICS_CACHE_TTL_MODE );

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
        final AnalyticsCacheTtlMode analyticsCacheMode = (AnalyticsCacheTtlMode) systemSettingManager
            .getSystemSetting( ANALYTICS_CACHE_TTL_MODE );

        final CacheStrategy cacheStrategy = (CacheStrategy) systemSettingManager.getSystemSetting( CACHE_STRATEGY );

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
     * defined by the {@link org.hisp.dhis.setting.SettingKey#CACHE_STRATEGY}
     *
     * @see CacheStrategy
     *
     * @return the predefined expiration time set or 0 (ZERO) if nothing is set.
     */
    public long fixedExpirationTimeOrDefault()
    {
        final CacheStrategy cacheStrategy = (CacheStrategy) systemSettingManager.getSystemSetting( CACHE_STRATEGY );

        if ( cacheStrategy != null && cacheStrategy.hasExpirationTimeSet() )
        {
            return cacheStrategy.toSeconds();
        }
        else
        {
            // Try to get a default value
            final CacheStrategy defaultExpirationTime = (CacheStrategy) CACHE_STRATEGY.getDefaultValue();

            if ( defaultExpirationTime.hasExpirationTimeSet() )
            {
                return defaultExpirationTime.toSeconds();
            }
            else
            {
                // Return ZERO (always expire)
                return NO_CACHE.toSeconds();
            }
        }
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
        final Integer ttlFactor = (Integer) systemSettingManager
            .getSystemSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR );

        return max( ttlFactor, 1 );
    }
}
