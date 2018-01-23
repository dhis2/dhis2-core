package org.hisp.dhis.textpattern;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stian Sandvold
 */
public class TextPatternMethodUtils
{

    /**
     * Returns a random String based on the format. The format (As specified in TextPatternMethod) can contain '#' digits,
     * 'X' capital letters and 'x' lower case letters.
     * @param format the format (as specified in TextPatternMethod)
     * @return the string generated
     */
    public static String generateRandom( String format )
    {
        Random random = new Random();
        StringBuilder result = new StringBuilder();

        for ( char c : format.toCharArray() )
        {
            switch ( c )
            {
            case '#':
                result.append( random.nextInt( 10 ) );
                break;
            case 'X':
                result.append( (char) (random.nextInt( 26 ) + 'A') );
                break;
            case 'x':
                result.append( (char) (random.nextInt( 26 ) + 'a') );
            }
        }

        return result.toString();
    }

    /**
     * Takes a format (as specified in TextPatternMethod) and attempts to apply it to the text.
     * If there is no match, the method returns null. This can happen if the text don't fit the format:
     * There are more '.' characters than there are characters in the text
     * Both '^' (start) and '$' end characters is present, but there is not an equal amount of '.' as characters in the text
     *
     * @param format the format defined (As specified in TextPatternMethod)
     * @param text the text to perform the format on.
     * @return the formatted text, or null if no match was found.
     */
    public static String formatText( String format, String text )
    {
        Matcher m = Pattern.compile( "(" + format + ")" ).matcher( text );

        if ( m.lookingAt() )
        {
            return m.group( 0 );
        }

        return null;
    }
}
