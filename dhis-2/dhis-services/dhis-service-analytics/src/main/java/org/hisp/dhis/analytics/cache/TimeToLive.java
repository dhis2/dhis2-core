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
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_FACTOR;
import static org.springframework.util.Assert.notNull;

import java.time.Instant;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.setting.SystemSettingManager;

public class TimeToLive
    implements
    Computable
{

    static final int DEFAULT_TTL_FACTOR = 5;
    static final long DEFAULT_MULTIPLIER = 1;

    private final DataQueryParams params;

    private SystemSettingManager systemSettingManager;

    public TimeToLive( final DataQueryParams params, final SystemSettingManager systemSettingManager )
    {
        notNull( params, "Object params must not be null" );
        notNull( params.getLatestEndDate(), "Object params.getLatestEndDate() must not be null" );
        notNull( systemSettingManager, "Object systemSettingManager must not be null" );
        this.params = params;
        this.systemSettingManager = systemSettingManager;
    }

    /**
     * Execute the internal rules in order to calculate a TTL for the given
     * parameters. The current rules are based on a configurable timeout
     * "factor" (through SettingKey) which will be used in the calculation
     * of this time to live. Given the "factor" described above:
     *
     * Older the "endingDate", higher the "factor", longer the TTL.
     * The formula is basically: TTL = "factor" * (diff between now and endingDate)
     *
     * @return the computed TTL value in SECONDS.
     */
    @Override
    public long compute()
    {
        final Instant endingDate = params.getLatestEndDate().toInstant();

        /*
         * If the difference between the most recent date and NOW is 0 (zero) it means
         * the current day, so we increment the multiplier by 1 (one) avoiding
         * multiplying by 0 (zero).
         */
        final long diff = daysBetweenDateAndNow( endingDate );
        final long ttlMultiplier = diff > 0 ? diff : DEFAULT_MULTIPLIER;

        return ttlFactorOrDefault() * ttlMultiplier;
    }

    /**
     * Calculates the difference between now and the given date.
     * It has a the particularity of returning ZERO (0), if the
     * diff is negative, which means the date is ahead of now.
     *
     * @param date the date to subtract from now
     * @return the difference of days in MILLISECONDS
     */
    private long daysBetweenDateAndNow( final Instant date )
    {
        final long diff = DAYS.between( ofInstant( date, UTC ), now() );
        return diff >= 0 ? diff : 0;
    }

    /**
     * Returns the default TTL factor or a default one if none is defined.
     * @return the factor in
     */
    private int ttlFactorOrDefault()
    {
        final Integer ttlFactor = (Integer) systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR );
        final boolean ttlNotNullAndPositive = ttlFactor != null && ttlFactor > 0;

        return ttlNotNullAndPositive ? ttlFactor : DEFAULT_TTL_FACTOR;
    }
}
