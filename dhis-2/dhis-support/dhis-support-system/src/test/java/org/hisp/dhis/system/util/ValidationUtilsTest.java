package org.hisp.dhis.system.util;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Test;

import static org.hisp.dhis.system.util.ValidationUtils.*;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class ValidationUtilsTest
{
    @Test
    public void testCoordinateIsValid()
    {
        assertTrue( coordinateIsValid( "[+37.99034,-28.94221]" ) );
        assertTrue( coordinateIsValid( "[37.99034,-28.94221]" ) );
        assertTrue( coordinateIsValid( "[+37.99034,28.94221]" ) );
        assertTrue( coordinateIsValid( "[170.99034,78.94221]" ) );
        assertTrue( coordinateIsValid( "[-167,-28.94221]" ) );
        assertTrue( coordinateIsValid( "[37.99034,28]" ) );

        assertFalse( coordinateIsValid( "23.34343,56.3232" ) );
        assertFalse( coordinateIsValid( "23.34343 56.3232" ) );
        assertFalse( coordinateIsValid( "[23.34f43,56.3232]" ) );
        assertFalse( coordinateIsValid( "23.34343,56.323.2" ) );
        assertFalse( coordinateIsValid( "[23.34343,56..3232]" ) );
        assertFalse( coordinateIsValid( "[++37,-28.94221]" ) );
        assertFalse( coordinateIsValid( "S-0.27726 E37.08472" ) );
        assertFalse( coordinateIsValid( null ) );

        assertFalse( coordinateIsValid( "-185.12345,45.45423" ) );
        assertFalse( coordinateIsValid( "192.56789,-45.34332" ) );
        assertFalse( coordinateIsValid( "140.34,92.23323" ) );
        assertFalse( coordinateIsValid( "123.34,-94.23323" ) );
        assertFalse( coordinateIsValid( "000.34,-94.23323" ) );
        assertFalse( coordinateIsValid( "123.34,-00.23323" ) );
    }
    
    @Test
    public void testBboxIsValid()
    {
        assertTrue( bboxIsValid( "-13.2682125,7.3721619,-10.4261178,9.904012" ) );
        assertTrue( bboxIsValid( "12.26821,-23.3721,13.4261,-21.904" ) );
        assertTrue( bboxIsValid( "4,-23.37,5,-24.904" ) );
        assertTrue( bboxIsValid( "2.23, -23.37, 5.22, -24.90" ) );
        assertTrue( bboxIsValid( "-179.234,-89.342,178.323,88.135" ) );
        
        assertFalse( bboxIsValid( "[12.23,14.41,34.12,12.45]" ) );
        assertFalse( bboxIsValid( "22,23,14,41,34,11,11,41" ) );
        assertFalse( bboxIsValid( "22,23.14,41.34,11.11,41" ) );
        assertFalse( bboxIsValid( "-181.234,-89.342,178.323,88.135" ) );
        assertFalse( bboxIsValid( "-179.234,-92.342,178.323,88.135" ) );
        assertFalse( bboxIsValid( "-179.234,-89.342,185.323,88.135" ) );
        assertFalse( bboxIsValid( "-179.234,-89.342,178.323,94.135" ) );
    }

    @Test
    public void testGetLongitude()
    {
        assertEquals( "+37.99034", getLongitude( "[+37.99034,-28.94221]" ) );
        assertEquals( "37.99034", getLongitude( "[37.99034,28.94221]" ) );
        assertNull( getLongitude( "23.34343,56.3232" ) );
        assertNull( getLongitude( null ) );
    }

    @Test
    public void testGetLatitude()
    {
        assertEquals( "-28.94221", getLatitude( "[+37.99034,-28.94221]" ) );
        assertEquals( "28.94221", getLatitude( "[37.99034,28.94221]" ) );
        assertNull( getLatitude( "23.34343,56.3232" ) );
        assertNull( getLatitude( null ) );
    }

    @Test
    public void testPasswordIsValid()
    {
        assertFalse( passwordIsValid( "Johnd1" ) );
        assertFalse( passwordIsValid( "johndoe1" ) );
        assertFalse( passwordIsValid( "Johndoedoe" ) );
        assertTrue( passwordIsValid( "Johndoe1" ) );
    }

    @Test
    public void testEmailIsValid()
    {
        assertFalse( emailIsValid( "john@doe" ) );
        assertTrue( emailIsValid( "john@doe.com" ) );
    }

    @Test
    public void testDataValueIsZeroAndInsignificant()
    {
        DataElement de = new DataElement( "DEA" );
        de.setValueType( ValueType.INTEGER );
        de.setAggregationType( AggregationType.SUM );

        assertTrue( dataValueIsZeroAndInsignificant( "0", de ) );

        de.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        assertFalse( dataValueIsZeroAndInsignificant( "0", de ) );
    }

    @Test
    public void testDataValueIsValid()
    {
        DataElement de = new DataElement( "DEA" );
        de.setValueType( ValueType.INTEGER );

        assertNull( dataValueIsValid( null, de ) );
        assertNull( dataValueIsValid( "", de ) );

        assertNull( dataValueIsValid( "34", de ) );
        assertNotNull( dataValueIsValid( "Yes", de ) );

        de.setValueType( ValueType.NUMBER );

        assertNull( dataValueIsValid( "3.7", de ) );
        assertNotNull( dataValueIsValid( "No", de ) );

        de.setValueType( ValueType.INTEGER_POSITIVE );

        assertNull( dataValueIsValid( "3", de ) );
        assertNotNull( dataValueIsValid( "-4", de ) );

        de.setValueType( ValueType.INTEGER_ZERO_OR_POSITIVE );

        assertNull( dataValueIsValid( "3", de ) );
        assertNotNull( dataValueIsValid( "-4", de ) );

        de.setValueType( ValueType.INTEGER_NEGATIVE );

        assertNull( dataValueIsValid( "-3", de ) );
        assertNotNull( dataValueIsValid( "4", de ) );

        de.setValueType( ValueType.TEXT );

        assertNull( dataValueIsValid( "0", de ) );

        de.setValueType( ValueType.BOOLEAN );

        assertNull( dataValueIsValid( "true", de ) );
        assertNotNull( dataValueIsValid( "yes", de ) );

        de.setValueType( ValueType.TRUE_ONLY );

        assertNull( dataValueIsValid( "true", de ) );
        assertNotNull( dataValueIsValid( "false", de ) );

        de.setValueType( ValueType.DATE );
        assertNull( dataValueIsValid( "2013-04-01", de ) );
        assertNotNull( dataValueIsValid( "2012304-01", de ) );
        assertNotNull( dataValueIsValid( "Date", de ) );

        de.setValueType( ValueType.DATETIME );
        assertNull( dataValueIsValid( "2013-04-01T11:00:00.000Z", de ) );
        assertNotNull( dataValueIsValid( "2013-04-01", de ) );
        assertNotNull( dataValueIsValid( "abcd", de ) );
    }

    @Test
    public void testIsValidHexColor()
    {
        assertFalse( isValidHexColor( "abcpqr" ) );
        assertFalse( isValidHexColor( "#qwerty" ) );
        assertFalse( isValidHexColor( "FFAB#O" ) );

        assertTrue( isValidHexColor( "#FF0" ) );
        assertTrue( isValidHexColor( "#FF0000" ) );
        assertTrue( isValidHexColor( "FFFFFF" ) );
        assertTrue( isValidHexColor( "ffAAb4" ) );
        assertTrue( isValidHexColor( "#4a6" ) );
        assertTrue( isValidHexColor( "abc" ) );
    }

    @Test
    public void testExpressionIsValidSQl()
    {
        assertFalse( expressionIsValidSQl( "10 == 10; delete from table" ) );
        assertFalse( expressionIsValidSQl( "select from table" ) );

        assertTrue( expressionIsValidSQl( "\"abcdef12345\" < 30" ) );
        assertTrue( expressionIsValidSQl( "\"abcdef12345\" >= \"bcdefg23456\"" ) );
        assertTrue( expressionIsValidSQl( "\"DO0v7fkhUNd\" > -30000 and \"DO0v7fkhUNd\" < 30000" ) );
        assertTrue( expressionIsValidSQl( "\"oZg33kd9taw\" == 'Female'" ) );
        assertTrue( expressionIsValidSQl( "\"oZg33kd9taw\" == 'Female' and \"qrur9Dvnyt5\" <= 5" ) );
    }
}
