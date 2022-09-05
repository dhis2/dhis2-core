/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.system.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 * @version $Id: MathUtil.java 4712 2008-03-12 10:32:45Z larshelg $
 */
class MathUtilsTest
{

    private static final double DELTA = 0.0001;

    @Test
    void testGetMin()
    {
        double[] array = { 5.0, 2.0, 6.0, 12.0 };
        assertEquals( MathUtils.getMin( array ), DELTA, 2.0 );
    }

    @Test
    void testGetMax()
    {
        double[] array = { 5.0, 2.0, 12.0, 6.0 };
        assertEquals( MathUtils.getMax( array ), DELTA, 12.0 );
    }

    @Test
    void testDoubleIsEqual()
    {
        assertTrue( MathUtils.isEqual( 100.0, 100.0 ) );
        assertTrue( MathUtils.isEqual( 100.0, 99.992 ) );

        assertFalse( MathUtils.isEqual( 100.0, 99.9 ) );
        assertFalse( MathUtils.isEqual( 100.0, 99.0 ) );
    }

    @Test
    void testDoubleObjectIsEqual()
    {
        assertTrue( MathUtils.isEqual( Double.valueOf( 100.0 ), Double.valueOf( 100.0 ) ) );
        assertTrue( MathUtils.isEqual( Double.valueOf( 100.0 ), Double.valueOf( 99.992 ) ) );

        assertFalse( MathUtils.isEqual( Double.valueOf( 100.0 ), Double.valueOf( 99.9 ) ) );
        assertFalse( MathUtils.isEqual( Double.valueOf( 100.0 ), Double.valueOf( 99.0 ) ) );
    }

    @Test
    void testIsNumeric()
    {
        assertTrue( MathUtils.isNumeric( "123" ) );
        assertTrue( MathUtils.isNumeric( "0" ) );
        assertTrue( MathUtils.isNumeric( "1.2" ) );
        assertTrue( MathUtils.isNumeric( "12.34" ) );
        assertTrue( MathUtils.isNumeric( "0.0" ) );
        assertTrue( MathUtils.isNumeric( "1.234" ) );
        assertTrue( MathUtils.isNumeric( "-1234" ) );
        assertTrue( MathUtils.isNumeric( "-12.34" ) );
        assertTrue( MathUtils.isNumeric( "-0.34" ) );
        assertTrue( MathUtils.isNumeric( "6.34" ) );
        assertTrue( MathUtils.isNumeric( "3.34" ) );
        assertTrue( MathUtils.isNumeric( "2.43" ) );
        assertFalse( MathUtils.isNumeric( "Hey" ) );
        assertFalse( MathUtils.isNumeric( "45 Perinatal Condition" ) );
        assertFalse( MathUtils.isNumeric( "Long street 2" ) );
        assertFalse( MathUtils.isNumeric( "1.2f" ) );
        assertFalse( MathUtils.isNumeric( "1 234" ) );
        assertFalse( MathUtils.isNumeric( "." ) );
        assertFalse( MathUtils.isNumeric( "1." ) );
        assertFalse( MathUtils.isNumeric( ".1" ) );
        assertFalse( MathUtils.isNumeric( "" ) );
        assertFalse( MathUtils.isNumeric( " " ) );
        assertFalse( MathUtils.isNumeric( "+1234  " ) );
        assertFalse( MathUtils.isNumeric( "1234  " ) );
        assertFalse( MathUtils.isNumeric( "  1234" ) );
        assertFalse( MathUtils.isNumeric( "1,234" ) );
        assertFalse( MathUtils.isNumeric( "0,1" ) );
        assertFalse( MathUtils.isNumeric( "0," ) );
        assertFalse( MathUtils.isNumeric( "0." ) );
        assertFalse( MathUtils.isNumeric( "01" ) );
        assertFalse( MathUtils.isNumeric( "001" ) );
        assertFalse( MathUtils.isNumeric( "00.23" ) );
        assertFalse( MathUtils.isNumeric( "01.23" ) );
        assertFalse( MathUtils.isNumeric( "4.23E" ) );
        assertFalse( MathUtils.isNumeric( "4.23Ef" ) );
        assertFalse( MathUtils.isNumeric( "E5" ) );
        assertFalse( MathUtils.isNumeric( null ) );
    }

    @Test
    void testIsNumericLenient()
    {
        assertTrue( MathUtils.isNumericLenient( "0123" ) );
        assertTrue( MathUtils.isNumericLenient( "123" ) );
        assertTrue( MathUtils.isNumericLenient( "0" ) );
        assertTrue( MathUtils.isNumericLenient( "1.2" ) );
        assertTrue( MathUtils.isNumericLenient( "012.34" ) );
        assertTrue( MathUtils.isNumericLenient( "12.34" ) );
        assertTrue( MathUtils.isNumericLenient( "0.0" ) );
        assertTrue( MathUtils.isNumericLenient( "1.234" ) );
        assertTrue( MathUtils.isNumericLenient( "-1234" ) );
        assertTrue( MathUtils.isNumericLenient( "-12.34" ) );
        assertTrue( MathUtils.isNumericLenient( "-0.34" ) );
        assertTrue( MathUtils.isNumericLenient( "6.34" ) );
        assertTrue( MathUtils.isNumericLenient( "3.342" ) );
        assertTrue( MathUtils.isNumericLenient( "2.43" ) );
        assertFalse( MathUtils.isNumericLenient( "Hey" ) );
        assertFalse( MathUtils.isNumericLenient( "45 Perinatal Condition" ) );
        assertFalse( MathUtils.isNumericLenient( "Long street 2" ) );
        assertFalse( MathUtils.isNumericLenient( "1.2f" ) );
        assertFalse( MathUtils.isNumericLenient( "1 234" ) );
        assertFalse( MathUtils.isNumericLenient( ".1" ) );
        assertFalse( MathUtils.isNumericLenient( ".4543" ) );
        assertFalse( MathUtils.isNumericLenient( "." ) );
        assertFalse( MathUtils.isNumericLenient( "1." ) );
        assertFalse( MathUtils.isNumericLenient( "" ) );
        assertFalse( MathUtils.isNumericLenient( " " ) );
        assertFalse( MathUtils.isNumericLenient( "+6575  " ) );
        assertFalse( MathUtils.isNumericLenient( "5643  " ) );
        assertFalse( MathUtils.isNumericLenient( "  3243" ) );
        assertFalse( MathUtils.isNumericLenient( "1,877" ) );
        assertFalse( MathUtils.isNumericLenient( "0,1" ) );
        assertFalse( MathUtils.isNumericLenient( "0," ) );
        assertFalse( MathUtils.isNumericLenient( "0." ) );
        assertFalse( MathUtils.isNumericLenient( "4.23E" ) );
        assertFalse( MathUtils.isNumericLenient( "4.23Ef" ) );
        assertFalse( MathUtils.isNumericLenient( "E5" ) );
        assertFalse( MathUtils.isNumericLenient( null ) );
    }

    @Test
    void testIsUnitInterval()
    {
        assertTrue( MathUtils.isUnitInterval( "0" ) );
        assertTrue( MathUtils.isUnitInterval( "0.2" ) );
        assertTrue( MathUtils.isUnitInterval( "0.876" ) );
        assertTrue( MathUtils.isUnitInterval( "1" ) );
        assertFalse( MathUtils.isUnitInterval( "2" ) );
        assertFalse( MathUtils.isUnitInterval( "-1" ) );
        assertFalse( MathUtils.isUnitInterval( "abc" ) );
        assertFalse( MathUtils.isUnitInterval( "1.01" ) );
    }

    @Test
    void testIsPercentage()
    {
        assertTrue( MathUtils.isPercentage( "0" ) );
        assertTrue( MathUtils.isPercentage( "15" ) );
        assertTrue( MathUtils.isPercentage( "100" ) );
        assertFalse( MathUtils.isPercentage( "abc" ) );
        assertFalse( MathUtils.isPercentage( "-1" ) );
        assertTrue( MathUtils.isPercentage( "12.5" ) );
        assertFalse( MathUtils.isPercentage( "17,8" ) );
        assertFalse( MathUtils.isPercentage( "101" ) );
    }

    @Test
    void testIsInteger()
    {
        assertTrue( MathUtils.isInteger( "1" ) );
        assertTrue( MathUtils.isInteger( "123" ) );
        assertTrue( MathUtils.isInteger( "-2" ) );
        assertTrue( MathUtils.isInteger( "0" ) );
        assertFalse( MathUtils.isInteger( "1.1" ) );
        assertFalse( MathUtils.isInteger( "+4" ) );
        assertFalse( MathUtils.isInteger( "-0" ) );
        assertFalse( MathUtils.isInteger( "Hey" ) );
        assertFalse( MathUtils.isInteger( " 1" ) );
        assertFalse( MathUtils.isInteger( "1 " ) );
        assertFalse( MathUtils.isInteger( "1.2345" ) );
        assertFalse( MathUtils.isInteger( "12147483647" ) );
    }

    @Test
    void testIsPositiveInteger()
    {
        assertTrue( MathUtils.isPositiveInteger( "1" ) );
        assertTrue( MathUtils.isPositiveInteger( "123" ) );
        assertFalse( MathUtils.isPositiveInteger( "0" ) );
        assertFalse( MathUtils.isPositiveInteger( "+2" ) );
        assertFalse( MathUtils.isPositiveInteger( "-2" ) );
        assertFalse( MathUtils.isPositiveInteger( "-2232" ) );
        assertFalse( MathUtils.isPositiveInteger( "-2.17" ) );
        assertFalse( MathUtils.isPositiveInteger( "1.1" ) );
        assertFalse( MathUtils.isPositiveInteger( "-0" ) );
        assertFalse( MathUtils.isPositiveInteger( "Hey" ) );
        assertFalse( MathUtils.isPositiveInteger( "1 " ) );
        assertFalse( MathUtils.isPositiveInteger( "1.2345" ) );
    }

    @Test
    void testIsNegativeInteger()
    {
        assertTrue( MathUtils.isNegativeInteger( "-1" ) );
        assertTrue( MathUtils.isNegativeInteger( "-123" ) );
        assertFalse( MathUtils.isNegativeInteger( "0" ) );
        assertFalse( MathUtils.isNegativeInteger( "+2" ) );
        assertFalse( MathUtils.isNegativeInteger( "2" ) );
        assertFalse( MathUtils.isNegativeInteger( "2232" ) );
        assertFalse( MathUtils.isNegativeInteger( "2.17" ) );
        assertFalse( MathUtils.isNegativeInteger( "1.1" ) );
        assertFalse( MathUtils.isNegativeInteger( "-0" ) );
        assertFalse( MathUtils.isNegativeInteger( "Hey" ) );
        assertFalse( MathUtils.isNegativeInteger( "2 " ) );
        assertFalse( MathUtils.isNegativeInteger( "6.1345" ) );
    }

    @Test
    void testIsZeroOrPositiveInteger()
    {
        assertTrue( MathUtils.isZeroOrPositiveInteger( "0" ) );
        assertTrue( MathUtils.isZeroOrPositiveInteger( "123" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "012" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "+20" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "-2" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "-2232" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "-2.17" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "1.1" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "-0" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "Hey" ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "1 " ) );
        assertFalse( MathUtils.isZeroOrPositiveInteger( "1.2345" ) );
    }

    @Test
    void testIsCoordinate()
    {
        assertTrue( MathUtils.isCoordinate( "[0.0,0.0]" ) );
        assertTrue( MathUtils.isCoordinate( "[18, 65]" ) );
        assertTrue( MathUtils.isCoordinate( "[18.56, 65.342]" ) );
        assertTrue( MathUtils.isCoordinate( "[18.56,65.342]" ) );
        assertTrue( MathUtils.isCoordinate( "[-18.56,-65.342]" ) );
        assertTrue( MathUtils.isCoordinate( "   [18.56 ,  65.342   ]    " ) );
        assertTrue( MathUtils.isCoordinate( "   [  -180 ,  -90]    " ) );
        assertTrue( MathUtils.isCoordinate( "   [  12.30 ,  45.67    ]    " ) );
        assertFalse( MathUtils.isCoordinate( "" ) );
        assertFalse( MathUtils.isCoordinate( null ) );
        assertFalse( MathUtils.isCoordinate( "18.56a, 65.342b" ) );
        assertFalse( MathUtils.isCoordinate( "0" ) );
        assertFalse( MathUtils.isCoordinate( "-0" ) );
        assertFalse( MathUtils.isCoordinate( "Hey" ) );
        assertFalse( MathUtils.isCoordinate( " 1" ) );
        assertFalse( MathUtils.isCoordinate( "1 " ) );
        assertFalse( MathUtils.isCoordinate( "1.2345,123, 123" ) );
        assertFalse( MathUtils.isCoordinate( "12147483647" ) );
        assertFalse( MathUtils.isCoordinate( "-181 ,-90" ) );
        assertFalse( MathUtils.isCoordinate( "-180 , 91" ) );
        assertFalse( MathUtils.isCoordinate( "12,34" ) );
        assertFalse( MathUtils.isCoordinate( "[,]" ) );
        assertFalse( MathUtils.isCoordinate( "[12,  ]" ) );
    }

    @Test
    void testIsZero()
    {
        assertTrue( MathUtils.isZero( "0" ) );
        assertFalse( MathUtils.isZero( "+0" ) );
        assertFalse( MathUtils.isZero( "-0" ) );
        assertFalse( MathUtils.isZero( "2232" ) );
        assertFalse( MathUtils.isZero( "2.17" ) );
        assertFalse( MathUtils.isZero( "Hey" ) );
    }

    @Test
    void testGetAverage()
    {
        assertEquals( MathUtils.getAverage( Arrays.asList( 5.0, 5.0, 10.0, 10.0 ) ), DELTA, 7.5 );
    }

    @Test
    void testGetRounded()
    {
        assertEquals( 10, MathUtils.getRounded( 10.00 ), DELTA );
        assertEquals( 10, MathUtils.getRounded( 10 ), DELTA );
        assertEquals( 0.53, MathUtils.getRounded( 0.5281 ), DELTA );
        assertEquals( 0.5, MathUtils.getRounded( 0.5 ), DELTA );
        assertEquals( 0, MathUtils.getRounded( 0 ), DELTA );
        assertEquals( -0.43, MathUtils.getRounded( -0.43123 ), DELTA );
        assertEquals( -10, MathUtils.getRounded( -10.00 ), DELTA );
    }

    @Test
    void testRoundToSignificantDigits()
    {
        assertEquals( 0.1, MathUtils.roundToSignificantDigits( .1357, 1 ), DELTA );
        assertEquals( 0.14, MathUtils.roundToSignificantDigits( .1357, 2 ), DELTA );
        assertEquals( 0.136, MathUtils.roundToSignificantDigits( .1357, 3 ), DELTA );
        assertEquals( 0.1357, MathUtils.roundToSignificantDigits( .1357, 4 ), DELTA );

        assertEquals( -0.1, MathUtils.roundToSignificantDigits( -.1357, 1 ), DELTA );
        assertEquals( -0.14, MathUtils.roundToSignificantDigits( -.1357, 2 ), DELTA );
        assertEquals( -0.136, MathUtils.roundToSignificantDigits( -.1357, 3 ), DELTA );
        assertEquals( -0.1357, MathUtils.roundToSignificantDigits( -.1357, 4 ), DELTA );

        assertEquals( 0.14, MathUtils.roundToSignificantDigits( .1357, 2 ), DELTA );
        assertEquals( 1.4, MathUtils.roundToSignificantDigits( 1.357, 2 ), DELTA );
        assertEquals( 14.0, MathUtils.roundToSignificantDigits( 13.57, 2 ), DELTA );
        assertEquals( 140.0, MathUtils.roundToSignificantDigits( 135.7, 2 ), DELTA );

        assertEquals( -0.14, MathUtils.roundToSignificantDigits( -.1357, 2 ), DELTA );
        assertEquals( -1.4, MathUtils.roundToSignificantDigits( -1.357, 2 ), DELTA );
        assertEquals( -14.0, MathUtils.roundToSignificantDigits( -13.57, 2 ), DELTA );
        assertEquals( -140.0, MathUtils.roundToSignificantDigits( -135.7, 2 ), DELTA );
    }

    @Test
    void testRoundFraction()
    {
        assertEquals( 1.0, MathUtils.roundFraction( 1.357, 1 ), DELTA );
        assertEquals( 1.4, MathUtils.roundFraction( 1.357, 2 ), DELTA );
        assertEquals( 1.36, MathUtils.roundFraction( 1.357, 3 ), DELTA );

        assertEquals( -1.0, MathUtils.roundFraction( -1.357, 1 ), DELTA );
        assertEquals( -1.4, MathUtils.roundFraction( -1.357, 2 ), DELTA );
        assertEquals( -1.36, MathUtils.roundFraction( -1.357, 3 ), DELTA );

        assertEquals( 1.4, MathUtils.roundFraction( 1.357, 2 ), DELTA );
        assertEquals( 14.0, MathUtils.roundFraction( 13.57, 2 ), DELTA );
        assertEquals( 136.0, MathUtils.roundFraction( 135.7, 2 ), DELTA );

        assertEquals( -1.4, MathUtils.roundFraction( -1.357, 2 ), DELTA );
        assertEquals( -14.0, MathUtils.roundFraction( -13.57, 2 ), DELTA );
        assertEquals( -136.0, MathUtils.roundFraction( -135.7, 2 ), DELTA );
    }
}
