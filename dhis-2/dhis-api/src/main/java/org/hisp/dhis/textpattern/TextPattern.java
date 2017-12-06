package org.hisp.dhis.textpattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TextPattern
{
    private ArrayList<Segment> segments;

    private String valuePattern = "^";

    TextPattern()
    {
        this.segments = new ArrayList<>();
    }

    class Segment
    {
        private String value;

        private TextPatternMethod method;

        Segment( String value, TextPatternMethod method )
        {
            this.value = value;
            this.method = method;
        }

        public String getValue()
        {
            return value;
        }

        public TextPatternMethod getMethod()
        {
            return method;
        }
    }

    public void addSegment( String value, TextPatternMethod type )
    {
        this.segments.add( new Segment( value, type ) );

        valuePattern += "(" + getValuePattern( value, type ) + ")";
    }

    private String getValuePattern( String value, TextPatternMethod type )
    {
        if ( type.equals( TextPatternMethod.TEXT ) )
        {
            return value;
        }
        else if ( type.equals( TextPatternMethod.RANDOM ) )
        {
            value = value.replaceAll( "#", "[0-9]" );
            value = value.replaceAll( "X", "[A-Z]" );
            value = value.replaceAll( "x", "[a-z]" );

            return value;
        }
        else if ( type.equals( TextPatternMethod.SEQUENTIAL ))
        {
            return value.replaceAll( "#", "[0-9]" );
        }
        else if ( type.equals( TextPatternMethod.ORG_UNIT_CODE ))
        {
            System.out.println("value: " + value);
            value = value.replaceAll( "\\^", "" );
            System.out.println("value: " + value);
            value = value.replaceAll( "\\$", "" );
            System.out.println("value: " + value);
            return value;
        }
        else if ( type.equals( TextPatternMethod.CURRENT_DATE ))
        {
            return ".*?";
        }

        return "";
    }

    public List<Segment> getSegments()
    {
        return segments;
    }

    public boolean validate( String text )
    {

        System.out.println("Regex: " + valuePattern + "$");
        return Pattern.compile( valuePattern + "$" ).matcher( text ).matches();

    }

}
