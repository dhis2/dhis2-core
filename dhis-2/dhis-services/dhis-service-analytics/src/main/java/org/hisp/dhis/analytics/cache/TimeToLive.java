/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.MONTHLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.QUARTERLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.SIX_MONTHS;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.WEEKLY;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS;
import static org.springframework.util.Assert.notNull;

import java.time.Instant;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;

public class TimeToLive
    implements
    Computable
{

    static final long DEFAULT_TO_30_SECONDS = 30;

    private final DataQueryParams params;

    private SystemSettingManager systemSettingManager;

    public TimeToLive( final DataQueryParams params, final SystemSettingManager systemSettingManager )
    {
        notNull( params, "Object params must not be null" );
        notNull( params.getEarliestStartDate(), "Object params.getEarliestStartDate() must not be null" );
        notNull( params.getLatestEndDate(), "Object params.getLatestEndDate() must not be null" );
        notNull( systemSettingManager, "Object systemSettingManager must not be null" );
        this.params = params;
        this.systemSettingManager = systemSettingManager;
    }

    /**
     * Calculates the time to live based on specific internal rules.
     *
     * @return the computed TTL value in SECONDS.
     */
    @Override
    public long compute()
    {
        final Instant oldestDate = params.getEarliestStartDate().toInstant();
        final Instant mostRecentDate = params.getLatestEndDate().toInstant();
        final long daysDiff = DAYS.between( oldestDate, mostRecentDate );
        return calculationFor( daysDiff, mostRecentDate );
    }

    /**
     * Execute the internal rules in order to calculate a TTL for the given
     * parameters.
     *
     * @param daysDiff the difference of days between the oldest and most recent
     *        date in the period.
     * @param mostRecentDate the most recent date of the period.
     * @return a time to live value in SECONDS.
     */
    long calculationFor( final long daysDiff, final Instant mostRecentDate )
    {
        /*
         * If the difference between the most recent date and NOW is 0 (zero) it means
         * the current year, so we increment the multiplier by 1 (one) avoiding
         * multiplying by 0 (zero).
         */
        final long ttlMultiplierOrOne = YEARS.between( ofInstant( mostRecentDate, UTC ), now() ) + 1;
        final long ttlInSeconds;

        if ( daysDiff <= WEEKLY.value() )
        {
            ttlInSeconds = preDefinedTtlValueForKey(ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS);
        }
        else if ( daysDiff <= MONTHLY.value() )
        {
            ttlInSeconds = preDefinedTtlValueForKey(ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS);
        }
        else if ( daysDiff <= QUARTERLY.value() )
        {
            ttlInSeconds = preDefinedTtlValueForKey(ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS);
        }
        else if ( daysDiff <= SIX_MONTHS.value() )
        {
            ttlInSeconds = preDefinedTtlValueForKey(ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS);
        }
        else
        {
            ttlInSeconds = preDefinedTtlValueForKey(ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS);
        }
        return ttlInSeconds * ttlMultiplierOrOne;
    }

    private Long preDefinedTtlValueForKey( final SettingKey ttlSettingKey )
    {
        final Long ttlInSeconds = (Long) systemSettingManager.getSystemSetting( ttlSettingKey );
        final boolean ttlNotNullAndPositive = ttlInSeconds != null && ttlInSeconds > 0;

        return ttlNotNullAndPositive ? ttlInSeconds : DEFAULT_TO_30_SECONDS;
    }

    enum Periods
    {
        WEEKLY( 7 ), MONTHLY( 30 ), QUARTERLY( 120 ), SIX_MONTHS( 180 ), YEARLY( 365 );

        final int days;

        Periods( final int days )
        {
            this.days = days;
        }

        int value()
        {
            return days;
        }
    }
}
