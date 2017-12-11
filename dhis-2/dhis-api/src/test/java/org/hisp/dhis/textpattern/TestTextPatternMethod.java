package org.hisp.dhis.textpattern;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestTextPatternMethod
{

    @Test
    public void testValidateText()
    {
        String[] valid = {
            "\"Hello world!\"",
            "\"Hello \\\"world\\\"\""
        };

        String[] invalid = {
            "Hello world",
            "Hello \" world",
            "\"Hello world",
            "Hello world\""
        };

        testSyntax( TextPatternMethod.TEXT, valid, true );
        testSyntax( TextPatternMethod.TEXT, invalid, false );
    }

    @Test
    public void testValidateRandom()
    {
        String[] valid = {
            "RANDOM(#)",
            "RANDOM(X)",
            "RANDOM(x)",
            "RANDOM(Xx#)",
            "RANDOM(xX#)",
            "RANDOM(##XXxx)",
            "RANDOM(#X#xXx#xX#XX)" // 12 characters
        };

        String[] invalid = {
            "RAND(#)",
            "RANDOM()",
            "RANDOM(1)",
            "RANDOM(#############)", // 13 characters
        };

        testSyntax( TextPatternMethod.RANDOM, valid, true );
        testSyntax( TextPatternMethod.RANDOM, invalid, false );
    }

    @Test
    public void testValidateSequential()
    {
        String[] valid = {
            "SEQUENTIAL(#)",
            "SEQUENTIAL(############)", // 12 characters
        };

        String[] invalid = {
            "SEQ(#)",
            "SEQUENTIAL()",
            "SEQUENTIAL(1)",
            "SEQUENTIAL(x)",
            "SEQUENTIAL(X)",
            "SEQUENTIAL(#############)", // 13 characters
        };

        testSyntax( TextPatternMethod.SEQUENTIAL, valid, true );
        testSyntax( TextPatternMethod.SEQUENTIAL, invalid, false );
    }

    @Test
    public void testValidateOrgUnitCode()
    {
        String[] valid = {
            "ORG_UNIT_CODE()",
            "ORG_UNIT_CODE(.)",
            "ORG_UNIT_CODE(...)",
            "ORG_UNIT_CODE(^.)",
            "ORG_UNIT_CODE(.$)",
        };

        String[] invalid = {
            "ORG_UNIT_CODE(1)",
            "ORG_UNIT_CODE(.^)",
            "ORG_UNIT_CODE($.)",
            "ORG_UNIT_CODE(.^.)",
            "ORG_UNIT_CODE(.$.)",
            "ORG_UNIT_CODE(^$)",
            "ORG_UNIT_CODE(^^^)",
            "ORG_UNIT_CODE(.$$)"
        };

        testSyntax( TextPatternMethod.ORG_UNIT_CODE, valid, true );
        testSyntax( TextPatternMethod.ORG_UNIT_CODE, invalid, false );
    }

    @Test
    public void testValidateCurrentDate()
    {
        String[] valid = {
            "CURRENT_DATE(.)",
            "CURRENT_DATE(yy/mm/dd)",
            "CURRENT_DATE(DDMMYYYY)",
            "CURRENT_DATE(DD-MM-HH)",
            "CURRENT_DATE(Hello world)",
        };

        String[] invalid = {
            "CURRENT()",
            "CURRENT_DATE()"
        };

        testSyntax( TextPatternMethod.CURRENT_DATE, valid, true );
        testSyntax( TextPatternMethod.CURRENT_DATE, invalid, false );
    }

    @Test
    public void testGetParamText()
    {
        assertEquals( "Hello world", TextPatternMethod.TEXT.getType().getParam( "\"Hello world\"" ) );
    }

    @Test
    public void testGetParamRandom()
    {
        assertEquals( "##XXxx", TextPatternMethod.RANDOM.getType().getParam( "RANDOM(##XXxx)" ) );
    }

    @Test
    public void testGetParamSequential()
    {
        assertEquals( "###", TextPatternMethod.SEQUENTIAL.getType().getParam( "SEQUENTIAL(###)" ) );
    }

    @Test
    public void testGetParamOrgUnitCode()
    {
        assertEquals( "...", TextPatternMethod.ORG_UNIT_CODE.getType().getParam( "ORG_UNIT_CODE(...)" ) );
    }

    @Test
    public void testGetParamCurrentDate()
    {
        assertEquals( "DDMMYYYY", TextPatternMethod.CURRENT_DATE.getType().getParam( "CURRENT_DATE(DDMMYYYY)" ) );
    }

    @Test
    public void testGetParamTextFails()
    {
        assertNull( TextPatternMethod.TEXT.getType().getParam( "Hello world" ) );
    }

    @Test
    public void testGetParamRandomFails()
    {
        assertNull( TextPatternMethod.RANDOM.getType().getParam( "INVALID(##XXxx)" ) );
    }

    @Test
    public void testGetParamSequentialFails()
    {
        assertNull( TextPatternMethod.SEQUENTIAL.getType().getParam( "INVALID(###)" ) );
    }

    @Test
    public void testGetParamOrgUnitCodeFails()
    {
        assertNull( TextPatternMethod.ORG_UNIT_CODE.getType().getParam( "INVALID(...)" ) );
    }

    @Test
    public void testGetParamCurrentDateFails()
    {
        assertNull( TextPatternMethod.CURRENT_DATE.getType().getParam( "INVALID(DDMMYYYY)" ) );
    }

    private void testSyntax( TextPatternMethod method, String[] input, boolean expected )
    {
        for ( String s : input )
        {
            assertEquals( expected, method.getType().validatePattern( s ) );
        }
    }
}
