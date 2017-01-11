package org.hisp.dhis.commons.util;
/*
 * Copyright (c) 2004-2017, University of Oslo
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

/**
 * @author Stian Sandvold
 */
public class CronUtils
{
    public enum Weekday
    {
        SUNDAY( "SUN" ),
        MONDAY( "MON" ),
        TUESDAY( "TUE" ),
        WEDNESDAY( "WED" ),
        THURSDAY( "THU" ),
        FRIDAY( "FRI" ),
        SATURDAY( "SAT" );

        private final String name;

        Weekday( String name )
        {
            this.name = name;
        }
    }

    /**
     * Generates a cron pattern that will execute every day at the given hour:minute
     *
     * @param minutes
     * @param hours
     * @return a cron pattern
     */
    public static String getDailyCronExpression( int minutes, int hours )
    {
        return getCronExpression(
            "0",
            String.valueOf( minutes ),
            String.valueOf( hours ),
            "*/1",
            null,
            null
        );
    }

    /**
     * Generates a cron pattern that will execute every week at the dayOfWeek at hour:minute
     *
     * @param minutes
     * @param hours
     * @param dayOfWeek can be 0-7. 0 and 7 both resolve to sunday
     * @return a cron pattern
     */
    public static String getWeeklyCronExpression( int minutes, int hours, int dayOfWeek )
    {
        return getCronExpression(
            "0",
            String.valueOf( minutes ),
            String.valueOf( hours ),
            null,
            null,
            Weekday.values()[(dayOfWeek % 7)].name // both 0 and 7 are valid as Sunday in crontab patterns
        );
    }

    /**
     * Generates a cron pattern that will execute every month at the dayOfMonth at hour:minute
     *
     * @param minutes
     * @param hours
     * @param dayOfMonth
     * @return a cron pattern
     */
    public static String getMonthlyCronExpression( int minutes, int hours, int dayOfMonth )
    {
        return getCronExpression(
            "0",
            String.valueOf( minutes ),
            String.valueOf( hours ),
            String.valueOf( dayOfMonth ),
            "*/1",
            null
        );
    }

    /**
     * Joins together each segment of a cron pattern into a complete cron pattern.
     *
     * @param seconds a valid cron segment
     * @param minutes a valid cron segment
     * @param hours   a valid cron segment
     * @param days    a valid cron segment
     * @param months  a valid cron segment
     * @param weekday a valid cron segment (MON-SUN)
     * @return a cron pattern
     */
    public static String getCronExpression( String seconds, String minutes, String hours, String days, String months,
        String weekday )
    {
        return String.join( " ",
            (seconds == null ? "*" : seconds),
            (minutes == null ? "*" : minutes),
            (hours == null ? "*" : hours),
            (days == null ? "*" : days),
            (months == null ? "*" : months),
            (weekday == null ? "*" : weekday)
        );
    }
}
