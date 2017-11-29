package org.hisp.dhis.textpattern;

import java.util.ArrayList;
import java.util.List;

public class TextPattern
{
    private ArrayList<Segment> segments;

    TextPattern()
    {
        this.segments = new ArrayList<>();
    }

    class Segment
    {
        String value;

        TextPatternMethod method;

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
    }

    public List<Segment> getSegments()
    {
        return segments;
    }

}
