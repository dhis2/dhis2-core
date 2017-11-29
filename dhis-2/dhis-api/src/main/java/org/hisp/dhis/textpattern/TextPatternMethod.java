package org.hisp.dhis.textpattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TextPatternMethod
{

    /**
     * Text method is just a fixed text that is a part of the pattern. It starts and ends with a quotation mark: "
     * A Text can contain quotation marks, but they need to be escaped.
     * Example usage:
     * "Hello world"
     * "Hello \"world\""
     * <p>
     * This is the only method that has no keyword associated with it.
     */
    TEXT( Pattern.compile( "\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"" ) ),

    /**
     * Generator methods has a required param, that needs to be between 1 and 12 characters.
     * SEQUENTIAL only accepts #'s while RANDOM accepts #Xx's
     */
    RANDOM( Pattern.compile( "RANDOM\\(([#Xx]{1,12})\\)" ) ),
    SEQUENTIAL( Pattern.compile( "SEQUENTIAL\\(([#]{1,12})\\)" ) ),

    /**
     * Variable methods has an optional param, that can:
     * start with ^
     * have 1 or more . (representing a character)
     * end with $
     * <p>
     * ^ will start the format form the start of the resolved value
     * $ will start the format from the end of the resolved value
     * . will match a single character. At least 1 is required if a param is supplied
     * <p>
     * Alternatively, an empty param means the entire resolved value will be returned.
     * <p>
     * Example usage assuming ORG_UNIT_CODE resolved to "Hello world":
     * ORG_UNIT_CODE() = "Hello world"
     * ORG_UNIT_CODE(..) = "He"
     * ORG_UNIT_CODE(^..) = "He"
     * ORG_UNIT_CODE(..$) = "ld"
     */

    // OrgUnit methods
    ORG_UNIT_CODE( Pattern.compile( "ORG_UNIT_CODE\\((.{0}|[\\^]?[.]+?[$]?)\\)" ) ),

    /**
     * Date methods has a required param that will be used to format the date.
     * The regex will match any sequence of characters for now.
     * <p>
     * The param will be used directly as the format in SimpleDateFormat:
     * https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
     */

    // Date methods
    CURRENT_DATE( Pattern.compile( "CURRENT_DATE\\((.+?)\\)" ) );

    private Pattern regex;

    TextPatternMethod( Pattern regex )
    {
        this.regex = regex;
    }

    public boolean isSyntaxValid( String raw )
    {
        return regex.matcher( raw ).matches();
    }

    public String getParam( String raw )
    {
        Matcher m = regex.matcher( raw );

        if ( m.matches() )
        {
            return m.group( 1 );
        }

        return null;
    }

}
