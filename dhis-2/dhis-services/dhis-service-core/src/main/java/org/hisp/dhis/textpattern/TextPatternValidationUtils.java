package org.hisp.dhis.textpattern;

public class TextPatternValidationUtils
{

    public static boolean validateSegmentValue( TextPatternSegment segment, String value )
    {
        return segment.getMethod().getType().validateText( segment.getParameter(), value );
    }

    public static boolean validateTextPatternValue( TextPattern textPattern, String value )
    {
        // TODO!
        return false;
    }

}
