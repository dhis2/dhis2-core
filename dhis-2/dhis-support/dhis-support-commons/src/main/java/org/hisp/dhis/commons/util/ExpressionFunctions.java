package org.hisp.dhis.commons.util;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Class for functions to be used in JEXL expression evaluation.
 * 
 * @author Lars Helge Overland
 */
public class ExpressionFunctions
{
    public static final String NAMESPACE = "d2";
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );
    
    /**
     * Function which will return zero if the argument is a negative number.
     * 
     * @param value the value, must be a number.
     * @return a Double.
     */
    public static Double zing( Number value )
    {
        if ( value == null )
        {
            return null;
        }
        
        return Math.max( 0d, value.doubleValue() );
    }

    /**
     * Function which will return one if the argument is zero or a positive 
     * number, and zero if not.
     * 
     * @param value the value, must be a number.
     * @return a Double.
     */
    public static Double oizp( Number value )
    {
        if ( value == null )
        {
            return null;
        }
        
        return ( value.doubleValue() >= 0d ) ? 1d : 0d;
    }
    
    /**
     * Function which will return the count of zero or positive values among the
     * given argument values.
     * 
     * @param values the arguments.
     * @return an Integer.
     */
    public static Integer zpvc( Number... values )
    {
        if ( values == null || values.length == 0 )
        {
            throw new IllegalArgumentException( "Argument is null or empty" );
        }
        
        int count = 0;
        
        for ( Number value : values )
        {
            if ( value != null && value.doubleValue() >= 0d )
            {
                count++;
            }
        }
        
        return count;        
    }

    /**
     * Functions which will return the true value if the condition is true, false
     * value if not.
     * 
     * @param condititon the condition.
     * @param trueValue the true value.
     * @param falseValue the false value.
     * @return a String.
     */
    public static Object condition( String condititon, Object trueValue, Object falseValue )
    {
        return ExpressionUtils.isTrue( condititon, null ) ? trueValue : falseValue;        
    }
    
    /**
     * Function which will return the number of days between the two given dates.
     * 
     * @param start the start date. 
     * @param end the end date.
     * @return number of days between dates.
     * @throws ParseException if start or end could not be parsed.
     */
    public static Long daysBetween( String start, String end )
    {
        LocalDate st = LocalDate.parse( start, DATE_FORMAT );
        LocalDate en = LocalDate.parse( end, DATE_FORMAT );
        
        return ChronoUnit.DAYS.between( st, en );
    }
}
