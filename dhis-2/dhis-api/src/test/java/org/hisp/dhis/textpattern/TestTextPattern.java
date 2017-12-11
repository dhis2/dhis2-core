package org.hisp.dhis.textpattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestTextPattern
{

    @Test
    public void testAddSegment()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT.getType() );

        assertEquals( 1, tp.getSegments().size() );
    }

    @Test
    public void testGetSegment()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT.getType() );
        tp.addSegment( "DEF", TextPatternMethod.TEXT.getType() );

        assertEquals( 2, tp.getSegments().size() );
    }

    @Test
    public void testSegmentGetValue()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT.getType() );

        assertEquals( "ABC", tp.getSegments().get( 0 ).getSegment() );
    }

    @Test
    public void testSegmentGetMethod()
    {
        TextPattern tp = new TextPattern();

        tp.addSegment( "ABC", TextPatternMethod.TEXT.getType() );

        assertEquals( TextPatternMethod.TEXT.getType(), tp.getSegments().get( 0 ).getType() );
    }
}
