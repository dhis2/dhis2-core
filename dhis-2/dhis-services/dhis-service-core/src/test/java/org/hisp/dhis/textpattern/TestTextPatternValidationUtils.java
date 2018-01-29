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

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class TestTextPatternValidationUtils
{

    private TextPatternSegment textSegment = new TextPatternSegment( TextPatternMethod.TEXT, "\"FOOBAR\"" );

    private TextPatternSegment randomSegment = new TextPatternSegment( TextPatternMethod.RANDOM, "RANDOM(XXxx##)" );

    private TextPatternSegment sequentialSegment = new TextPatternSegment( TextPatternMethod.SEQUENTIAL,
        "SEQUENTIAL(###)" );

    private TextPatternSegment orgUnitCodeSegment = new TextPatternSegment( TextPatternMethod.ORG_UNIT_CODE,
        "ORG_UNIT_CODE(...)" );

    private TextPatternSegment currentDateSegment = new TextPatternSegment( TextPatternMethod.CURRENT_DATE,
        "CURRENT_DATE(dd/mm/yyyy)" );

    @Test
    public void testValidationUtilsValidateSegmentValue()
    {

        assertTrue( TextPatternValidationUtils.validateSegmentValue( textSegment, "FOOBAR" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( textSegment, "FOBAR" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( textSegment, "" ) );

        assertTrue( TextPatternValidationUtils.validateSegmentValue( randomSegment, "AAaa11" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( randomSegment, "11AAaa" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( randomSegment, "AAaa111" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( randomSegment, "Aa1" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( randomSegment, "" ) );

        assertTrue( TextPatternValidationUtils.validateSegmentValue( sequentialSegment, "001" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( sequentialSegment, "1234" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( sequentialSegment, "01" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( sequentialSegment, "asd" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( sequentialSegment, "" ) );

        assertTrue( TextPatternValidationUtils.validateSegmentValue( orgUnitCodeSegment, "ABC" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( orgUnitCodeSegment, "ABCD" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( orgUnitCodeSegment, "AB" ) );
        assertFalse( TextPatternValidationUtils.validateSegmentValue( orgUnitCodeSegment, "" ) );

        // TODO: We only validate that there is <something> , not that it follows the format.
        assertTrue( TextPatternValidationUtils.validateSegmentValue( currentDateSegment, "22/10/1990" ) );

    }

    @Test
    public void testValidationUtilsValidateTextPatternValue()
        throws TextPatternParser.TextPatternParsingException
    {
        TextPattern tp = TextPatternParser.parse( "\"FOOBAR\"+RANDOM(xxx)+\"-\"+SEQUENTIAL(##)+ORG_UNIT_CODE(...)+CURRENT_DATE(yyyy)" );

        assertTrue( TextPatternValidationUtils.validateTextPatternValue( tp, "FOOBARabc-01OSL1990" ) );
        assertFalse( TextPatternValidationUtils.validateTextPatternValue( tp, "FOOBAR abc - 01 OSL 1990" ) );
        assertFalse( TextPatternValidationUtils.validateTextPatternValue( tp, "FOOBARabc-01 OSL 1990" ) );
        assertFalse( TextPatternValidationUtils.validateTextPatternValue( tp, "FOOBARabc-01OSL 1990" ) );
        assertFalse( TextPatternValidationUtils.validateTextPatternValue( tp, "" ) );


    }

}
