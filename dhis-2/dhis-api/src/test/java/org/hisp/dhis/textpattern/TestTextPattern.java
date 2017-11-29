package org.hisp.dhis.textpattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestTextPattern
{

    @Test
    public void testAddSegment()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT );

        assertEquals( 1, tp.getSegments().size() );
    }

    @Test
    public void testGetSegment()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT );
        tp.addSegment( "DEF", TextPatternMethod.TEXT );

        assertEquals( 2, tp.getSegments().size() );
    }

    @Test
    public void testSegmentGetValue()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT );

        assertEquals( "ABC", tp.getSegments().get( 0 ).getValue() );
    }

    @Test
    public void testSegmentGetMethod()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT );

        assertEquals( TextPatternMethod.TEXT, tp.getSegments().get( 0 ).getMethod() );
    }
}
