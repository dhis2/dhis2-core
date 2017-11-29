package org.hisp.dhis.idgenerator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class IDExpressionParserTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Class<IDExpressionParser.IDExpressionParseException> ParsingException = IDExpressionParser.IDExpressionParseException.class;

    private final String EXAMPLE_TEXT_SEGMENT = "\"Hello world!\"";

    private final String EXAMPLE_TEXT_SEGMENT_WITH_ESCAPED_QUOTES = "\"This is an \\\"escaped\\\" text\"";

    private final String EXAMPLE_SEQUENTIAL_SEGMENT = "SEQUENTIAL(#)";

    private final String EXAMPLE_RANDOM_SEGMENT = "RANDOM(#Xx)";

    @Test
    public void testParseNullExpressionThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        IDExpressionParser.parse( null );
    }

    @Test
    public void testParseEmptyExpressionThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        IDExpressionParser.parse( "" );
    }

    @Test
    public void testParseWhitespaceOnlyExpressionThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        IDExpressionParser.parse( "   " );
    }

    @Test
    public void testParseWithUnexpectedPlusThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        IDExpressionParser.parse( "+" );

    }

    @Test
    public void testParseWithInvalidInputThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        IDExpressionParser.parse( "Z" );

    }

    @Test
    public void testParseBadTextSegment()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );

        IDExpressionParser.parse( "\"This segment has no end" );
    }

    @Test
    public void testParseTextSegment()
        throws IDExpressionParser.IDExpressionParseException
    {

        testParseOK( EXAMPLE_TEXT_SEGMENT, IDExpressionType.TEXT );
    }

    @Test
    public void testParseTextWithEscapedQuotes()
        throws IDExpressionParser.IDExpressionParseException
    {

        testParseOK( EXAMPLE_TEXT_SEGMENT_WITH_ESCAPED_QUOTES, IDExpressionType.TEXT );
    }

    @Test
    public void testParseSequentialSegment()
        throws IDExpressionParser.IDExpressionParseException
    {
        testParseOK( EXAMPLE_SEQUENTIAL_SEGMENT, IDExpressionType.SEQUENTIAL );
    }

    @Test
    public void testParseSequentialSegmentInvalidPatternThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        testParseOK( "SEQUENTIAL(X)", IDExpressionType.SEQUENTIAL );
    }

    @Test
    public void testParseSequentialSegmentWithNoEndThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        testParseOK( "SEQUENTIAL(#", IDExpressionType.SEQUENTIAL );
    }

    @Test
    public void testParseSequentialSegmentWithNoPatternThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        testParseOK( "SEQUENTIAL()", IDExpressionType.SEQUENTIAL );
    }

    @Test
    public void testParseRandomSegment()
        throws IDExpressionParser.IDExpressionParseException
    {
        testParseOK( EXAMPLE_RANDOM_SEGMENT, IDExpressionType.RANDOM );
    }

    @Test
    public void testParseRandomSegmentInvalidPatternThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        testParseOK( "RANDOM(S)", IDExpressionType.RANDOM );
    }

    @Test
    public void testParseRandomSegmentWithNoEndThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        testParseOK( "RANDOM(#", IDExpressionType.RANDOM );
    }

    @Test
    public void testParseRandomSegmentWithNoPatternThrowsException()
        throws IDExpressionParser.IDExpressionParseException
    {
        thrown.expect( ParsingException );
        testParseOK( "RANDOM()", IDExpressionType.RANDOM );
    }

    @Test
    public void testParseFullValidExpression()
        throws IDExpressionParser.IDExpressionParseException
    {
        String TEXT_1 = "\"ABC\"";
        String SEPARATOR = "\"-\"";
        String SEQUENTIAL = "SEQUENTIAL(###)";
        String expression = String.format( " %s + %s + %s", TEXT_1, SEPARATOR, SEQUENTIAL );

        IDExpression idExpression = IDExpressionParser.parse( expression );
        assertNotNull( idExpression );

        List<Segment> segments = idExpression.getSegments();
        assertEquals( segments.size(), 3 );

        assertEquals( segments.get( 0 ).getValue(), TEXT_1 );
        assertEquals( segments.get( 1 ).getValue(), SEPARATOR );
        assertEquals( segments.get( 2 ).getValue(), SEQUENTIAL );
    }

    private void testParseOK( String input, IDExpressionType type )
        throws IDExpressionParser.IDExpressionParseException
    {
        IDExpression result = IDExpressionParser.parse( input );
        assertNotNull( result );

        List<Segment> segments = result.getSegments();
        assertEquals( segments.size(), 1 );

        assertEquals( segments.get( 0 ).getValue(), input );
        assertEquals( segments.get( 0 ).getType(), type );
    }
}
