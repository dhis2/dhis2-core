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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Stian Sandvold
 */
public class TextPatternMethodUtils
{
    /**
     * Returns a random String based on the format. The format (As specified in TextPatternMethod) can contain '#' digits,
     * 'X' capital letters and 'x' lower case letters.
     *
     * @param random a Random object to generate random numbers
     * @param format the format (as specified in TextPatternMethod)
     * @return the string generated
     */
    public static String generateRandom( Random random, String format )
    {
        StringBuilder result = new StringBuilder();

        List<Character> uppercase = IntStream.range( 0, 26 ).mapToObj( ( n ) -> (char) (n + 'A') )
            .collect( Collectors.toList() );
        List<Character> lowercase = IntStream.range( 0, 26 ).mapToObj( ( n ) -> (char) (n + 'a') )
            .collect( Collectors.toList() );
        List<Character> digits = IntStream.range( 0, 10 ).mapToObj( ( n ) -> (char) (n + '0') )
            .collect( Collectors.toList() );
        List<Character> all = new ArrayList<>();
        all.addAll( uppercase );
        all.addAll( lowercase );
        all.addAll( digits );

        for ( char c : format.toCharArray() )
        {
            switch ( c )
            {
            case '*':
                result.append( all.get( random.nextInt( all.size() ) ) );
                break;
            case '#':
                result.append( digits.get( random.nextInt( digits.size() ) ) );
                break;
            case 'X':
                result.append( uppercase.get( random.nextInt( uppercase.size() ) ) );
                break;
            case 'x':
                result.append( lowercase.get( random.nextInt( lowercase.size() ) ) );
                break;
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
     * @param text   the text to perform the format on.
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
