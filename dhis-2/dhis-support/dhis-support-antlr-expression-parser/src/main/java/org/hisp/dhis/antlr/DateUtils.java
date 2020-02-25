package org.hisp.dhis.antlr;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.joda.time.IllegalInstantException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import java.util.Date;

/**
 * @author Lars Helge Overland
 */
public class DateUtils
{
    private static final DateTimeParser[] SUPPORTED_DATE_FORMAT_PARSERS = {
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSS" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ssZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mmZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HHZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ssZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM" ).getParser(),
        DateTimeFormat.forPattern( "yyyy" ).getParser()
    };

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
        .append( null, SUPPORTED_DATE_FORMAT_PARSERS ).toFormatter();

    /**
     * Parses the given string into a Date using the supported date formats.
     * Returns null if the string cannot be parsed.
     *
     * @param dateString the date string.
     * @return a date.
     */
    public static Date parseDate( final String dateString )
    {
        return safeParseDateTime( dateString, DATE_FORMATTER );
    }

    /**
     * Parses the given string into a Date object. In case the date parsed falls in a
     * daylight savings transition, the date is parsed via a local date and converted to the
     * first valid time after the DST gap. When the fallback is used, any timezone offset in the given
     * format would be ignored.
     *
     * @param dateString The string to parse
     * @param formatter The formatter to use for parsing
     * @return Parsed Date object. Null if the supplied dateString is empty.
     */
    private static Date safeParseDateTime( final String dateString, final DateTimeFormatter formatter )
    {
        if ( StringUtils.isEmpty( dateString ) )
        {
            return null;
        }

        try
        {
            return formatter.parseDateTime( dateString ).toDate();
        }
        catch( IllegalInstantException e )
        {
            return formatter.parseLocalDateTime( dateString ).toDate();
        }
    }
}
