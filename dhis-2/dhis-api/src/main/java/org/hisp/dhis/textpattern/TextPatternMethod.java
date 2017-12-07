package org.hisp.dhis.textpattern;

import java.util.regex.Pattern;

import static org.hisp.dhis.textpattern.MethodType.RequiredStatus.*;

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
    TEXT( new TextMethodType( Pattern.compile( "\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"" ), NONE ) ),

    /**
     * Generator methods has a required param, that needs to be between 1 and 12 characters.
     * SEQUENTIAL only accepts #'s while RANDOM accepts #Xx's
     */
    RANDOM( new GeneratedMethodType( Pattern.compile( "RANDOM\\(([#Xx]{1,12})\\)" ), OPTIONAL ) ),
    SEQUENTIAL( new GeneratedMethodType( Pattern.compile( "SEQUENTIAL\\(([#]{1,12})\\)" ), OPTIONAL ) ),

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
    ORG_UNIT_CODE( new StringMethodType( Pattern.compile( "ORG_UNIT_CODE\\((.{0}|[\\^]?[.]+?[$]?)\\)" ), REQUIRED ) ),

    /**
     * Date methods has a required param that will be used to format the date.
     * The regex will match any sequence of characters for now.
     * <p>
     * The param will be used directly as the format in SimpleDateFormat:
     * https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
     */
    CURRENT_DATE( new DateMethodType( Pattern.compile( "CURRENT_DATE\\((.+?)\\)" ), OPTIONAL ) );

    private MethodType type;

    TextPatternMethod( MethodType type )
    {
        this.type = type;
    }

    public boolean isText()
    {
        return this.type instanceof TextMethodType;
    }

    public boolean isGenerated()
    {
        return this.type instanceof GeneratedMethodType;
    }

    public boolean hasTextFormat()
    {
        return this.type instanceof StringMethodType;
    }

    public boolean hasDateFormat()
    {
        return this.type instanceof DateMethodType;
    }

    public MethodType getType()
    {
        return type;
    }
}
