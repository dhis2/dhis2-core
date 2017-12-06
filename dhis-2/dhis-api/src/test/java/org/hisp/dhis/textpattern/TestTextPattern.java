package org.hisp.dhis.textpattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testValidate()
    {
        TextPattern _1 = new TextPattern();
        _1.addSegment( "ABC-", TextPatternMethod.TEXT );
        _1.addSegment( "^..$", TextPatternMethod.ORG_UNIT_CODE );
        _1.addSegment( "yyyy", TextPatternMethod.CURRENT_DATE );

        assertTrue( _1.validate( "ABC-ab1234" ) );
    }

}
