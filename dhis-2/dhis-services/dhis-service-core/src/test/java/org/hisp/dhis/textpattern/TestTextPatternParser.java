package org.hisp.dhis.textpattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestTextPatternParser
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Class<TextPatternParser.TextPatternParsingException> ParsingException = TextPatternParser.TextPatternParsingException.class;

    private final String EXAMPLE_TEXT_SEGMENT = "\"Hello world!\"";

    private final String EXAMPLE_TEXT_SEGMENT_WITH_ESCAPED_QUOTES = "\"This is an \\\"escaped\\\" text\"";

    private final String EXAMPLE_SEQUENTIAL_SEGMENT = "SEQUENTIAL(#)";

    private final String EXAMPLE_RANDOM_SEGMENT = "RANDOM(#Xx)";

    @Test
    public void testParseNullExpressionThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        TextPatternParser.parse( null );
    }

    @Test
    public void testParseEmptyExpressionThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        TextPatternParser.parse( "" );
    }

    @Test
    public void testParseWhitespaceOnlyExpressionThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        TextPatternParser.parse( "   " );
    }

    @Test
    public void testParseWithUnexpectedPlusThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        TextPatternParser.parse( "+" );

    }

    @Test
    public void testParseWithInvalidInputThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        TextPatternParser.parse( "Z" );

    }

    @Test
    public void testParseBadTextSegment()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );

        TextPatternParser.parse( "\"This segment has no end" );
    }

    @Test
    public void testParseTextSegment()
        throws TextPatternParser.TextPatternParsingException
    {

        testParseOK( EXAMPLE_TEXT_SEGMENT, TextPatternMethod.TEXT );
    }

    @Test
    public void testParseTextWithEscapedQuotes()
        throws TextPatternParser.TextPatternParsingException
    {

        testParseOK( EXAMPLE_TEXT_SEGMENT_WITH_ESCAPED_QUOTES, TextPatternMethod.TEXT );
    }

    @Test
    public void testParseSequentialSegment()
        throws TextPatternParser.TextPatternParsingException
    {
        testParseOK( EXAMPLE_SEQUENTIAL_SEGMENT, TextPatternMethod.SEQUENTIAL );
    }

    @Test
    public void testParseSequentialSegmentInvalidPatternThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        testParseOK( "SEQUENTIAL(X)", TextPatternMethod.SEQUENTIAL );
    }

    @Test
    public void testParseSequentialSegmentWithNoEndThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        testParseOK( "SEQUENTIAL(#", TextPatternMethod.SEQUENTIAL );
    }

    @Test
    public void testParseSequentialSegmentWithNoPatternThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        testParseOK( "SEQUENTIAL()", TextPatternMethod.SEQUENTIAL );
    }

    @Test
    public void testParseRandomSegment()
        throws TextPatternParser.TextPatternParsingException
    {
        testParseOK( EXAMPLE_RANDOM_SEGMENT, TextPatternMethod.RANDOM );
    }

    @Test
    public void testParseRandomSegmentInvalidPatternThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        testParseOK( "RANDOM(S)", TextPatternMethod.RANDOM );
    }

    @Test
    public void testParseRandomSegmentWithNoEndThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        testParseOK( "RANDOM(#", TextPatternMethod.RANDOM );
    }

    @Test
    public void testParseRandomSegmentWithNoPatternThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        thrown.expect( ParsingException );
        testParseOK( "RANDOM()", TextPatternMethod.RANDOM );
    }

    @Test
    public void testParseFullValidExpression()
        throws TextPatternParser.TextPatternParsingException
    {
        String TEXT_1 = "\"ABC\"";
        String SEPARATOR = "\"-\"";
        String SEQUENTIAL = "SEQUENTIAL(###)";
        String expression = String.format( " %s + %s + %s", TEXT_1, SEPARATOR, SEQUENTIAL );

        TextPattern textPattern = TextPatternParser.parse( expression );
        assertNotNull( textPattern );

        List<TextPattern.Segment> segments = textPattern.getSegments();
        assertEquals( segments.size(), 3 );

        assertEquals( segments.get( 0 ).getSegment(), TEXT_1 );
        assertEquals( segments.get( 1 ).getSegment(), SEPARATOR );
        assertEquals( segments.get( 2 ).getSegment(), SEQUENTIAL );
    }

    @Test
    public void testParseMultipleSequentialAndRandomThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        String pattern_1 = "RANDOM(#) + SEQUENTIAL(#)";
        String pattern_2 = "SEQUENTIAL(#) + SEQUENTIAL(#)";

        thrown.expect( ParsingException );
        TextPatternParser.parse( pattern_1 );

        thrown.expect( ParsingException );
        TextPatternParser.parse( pattern_2 );
    }

    @Test
    public void testParsePatternEndWithJoinThrowsException()
        throws TextPatternParser.TextPatternParsingException
    {
        String pattern = "RANDOM(#) + ";

        thrown.expect( ParsingException );
        TextPatternParser.parse( pattern );
    }

    @Test
    public void testCompletePatternOK()
        throws TextPatternParser.TextPatternParsingException
    {
        String pattern = "ORG_UNIT_CODE() + CURRENT_DATE(yyyy) + RANDOM(###) + \"-OK\"";

        TextPattern textPattern = TextPatternParser.parse( pattern );
        List<TextPattern.Segment> segments = textPattern.getSegments();

        assertEquals( 4, segments.size());

        assertEquals( segments.get(0).getType(), TextPatternMethod.ORG_UNIT_CODE.getType() );
        assertEquals( segments.get(1).getType(), TextPatternMethod.CURRENT_DATE.getType() );
        assertEquals( segments.get(2).getType(), TextPatternMethod.RANDOM.getType() );
        assertEquals( segments.get(3).getType(), TextPatternMethod.TEXT.getType() );

    }


    private void testParseOK( String input, TextPatternMethod method )
        throws TextPatternParser.TextPatternParsingException
    {
        TextPattern result = TextPatternParser.parse( input );
        assertNotNull( result );

        List<TextPattern.Segment> segments = result.getSegments();
        assertEquals( segments.size(), 1 );

        assertEquals( segments.get( 0 ).getSegment(), input );
        assertEquals( segments.get( 0 ).getType(), method.getType() );
    }
}
