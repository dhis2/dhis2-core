package org.hisp.dhis.textpattern;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
