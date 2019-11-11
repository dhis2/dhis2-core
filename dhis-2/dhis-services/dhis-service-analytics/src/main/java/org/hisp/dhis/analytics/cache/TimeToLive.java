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
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hisp.dhis.analytics.cache.TimeToLive.IntervalOfMilliseconds.FIFTEEN_MINUTES;
import static org.hisp.dhis.analytics.cache.TimeToLive.IntervalOfMilliseconds.FIVE_MINUTES;
import static org.hisp.dhis.analytics.cache.TimeToLive.IntervalOfMilliseconds.FORTY_MINUTES;
import static org.hisp.dhis.analytics.cache.TimeToLive.IntervalOfMilliseconds.SIXTY_MINUTES;
import static org.hisp.dhis.analytics.cache.TimeToLive.IntervalOfMilliseconds.TWO_MINUTES;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.MONTHLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.QUARTERLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.SIX_MONTHS;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.WEEKLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.YEARLY;
import static org.springframework.util.Assert.notNull;

import java.time.Instant;
import java.util.Map;

import org.hisp.dhis.analytics.DataQueryParams;

import com.google.common.collect.ImmutableMap;

public class TimeToLive
    implements
    Computable
{

    private final DataQueryParams params;

    static final Map<Periods, IntervalOfMilliseconds> EXPIRATION_TIME_TABLE = ImmutableMap.of(
        WEEKLY, TWO_MINUTES,
        MONTHLY, FIVE_MINUTES,
        QUARTERLY, FIFTEEN_MINUTES,
        SIX_MONTHS, FORTY_MINUTES,
        YEARLY, SIXTY_MINUTES );

    public TimeToLive( final DataQueryParams params )
    {
        notNull( params.getEarliestStartDate(), "Object params.getEarliestStartDate() must not be null" );
        notNull( params.getLatestEndDate(), "Object params.getLatestEndDate() must not be null" );
        this.params = params;
    }

    /**
     * Calculates the time to live based on specific internal rules.
     *
     * @return the computed TTL value in MILLISECONDS.
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
     * @return a time to live value in MILLISECONDS.
     */
    long calculationFor( final long daysDiff, final Instant mostRecentDate )
    {
        /*
         * If the difference between the most recent date and NOW is 0 (zero) it means
         * the current year, so we increment the multiplier by 1 (one) avoiding
         * multiplying by 0 (zero).
         */
        final int ttlMultiplierOrOne = (int) YEARS.between( ofInstant( mostRecentDate, UTC ), now() ) + 1;
        final IntervalOfMilliseconds ttlInMilliseconds;

        if ( daysDiff <= WEEKLY.value() )
        {
            ttlInMilliseconds = EXPIRATION_TIME_TABLE.get( WEEKLY );
        }
        else if ( daysDiff <= MONTHLY.value() )
        {
            ttlInMilliseconds = EXPIRATION_TIME_TABLE.get( MONTHLY );
        }
        else if ( daysDiff <= QUARTERLY.value() )
        {
            ttlInMilliseconds = EXPIRATION_TIME_TABLE.get( QUARTERLY );
        }
        else if ( daysDiff <= SIX_MONTHS.value() )
        {
            ttlInMilliseconds = EXPIRATION_TIME_TABLE.get( SIX_MONTHS );
        }
        else
        {
            ttlInMilliseconds = EXPIRATION_TIME_TABLE.get( YEARLY );
        }
        return ttlInMilliseconds.value() * ttlMultiplierOrOne;
    }

    enum Periods
    {
        WEEKLY( 7 ), MONTHLY( 30 ), QUARTERLY( 120 ), SIX_MONTHS( 180 ), YEARLY( 365 );

        final int days;

        Periods( int days )
        {
            this.days = days;
        }

        int value()
        {
            return days;
        }
    }

    enum IntervalOfMilliseconds
    {
        TWO_MINUTES( MINUTES.toMillis( 2 ) ),
        FIVE_MINUTES( MINUTES.toMillis( 5 ) ),
        FIFTEEN_MINUTES( MINUTES.toMillis( 15 ) ),
        FORTY_MINUTES( MINUTES.toMillis( 40 ) ),
        SIXTY_MINUTES( MINUTES.toMillis( 60 ) );

        final long milliseconds;

        IntervalOfMilliseconds(long milliseconds )
        {
            this.milliseconds = milliseconds;
        }

        long value()
        {
            return milliseconds;
        }
    }
}
