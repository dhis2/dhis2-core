package org.hisp.dhis.textpattern;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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

    private final String EXAMPLE_RANDOM_SEGMENT = "RANDOM(#Xx*)";

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

        List<TextPatternSegment> segments = textPattern.getSegments();
        assertEquals( segments.size(), 3 );

        assertEquals( segments.get( 0 ).getMethod(), TextPatternMethod.TEXT );
        assertEquals( segments.get( 1 ).getMethod(), TextPatternMethod.TEXT );
        assertEquals( segments.get( 2 ).getMethod(), TextPatternMethod.SEQUENTIAL );
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
        List<TextPatternSegment> segments = textPattern.getSegments();

        assertEquals( 4, segments.size() );

        assertEquals( segments.get( 0 ).getMethod(), TextPatternMethod.ORG_UNIT_CODE );
        assertEquals( segments.get( 1 ).getMethod(), TextPatternMethod.CURRENT_DATE );
        assertEquals( segments.get( 2 ).getMethod(), TextPatternMethod.RANDOM );
        assertEquals( segments.get( 3 ).getMethod(), TextPatternMethod.TEXT );

    }

    private void testParseOK( String input, TextPatternMethod method )
        throws TextPatternParser.TextPatternParsingException
    {
        TextPattern result = TextPatternParser.parse( input );
        assertNotNull( result );

        List<TextPatternSegment> segments = result.getSegments();
        assertEquals( segments.size(), 1 );

        assertEquals( segments.get( 0 ).getRawSegment(), input );
        assertEquals( segments.get( 0 ).getMethod(), method );
    }
}
