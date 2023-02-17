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

import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.EMAIL;
import static org.hisp.dhis.common.ValueType.FILE_RESOURCE;
import static org.hisp.dhis.common.ValueType.GEOJSON;
import static org.hisp.dhis.common.ValueType.IMAGE;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.INTEGER_NEGATIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_POSITIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_ZERO_OR_POSITIVE;
import static org.hisp.dhis.common.ValueType.LETTER;
import static org.hisp.dhis.common.ValueType.LONG_TEXT;
import static org.hisp.dhis.common.ValueType.MULTI_TEXT;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.PERCENTAGE;
import static org.hisp.dhis.common.ValueType.PHONE_NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.common.ValueType.TIME;
import static org.hisp.dhis.common.ValueType.TRUE_ONLY;
import static org.hisp.dhis.common.ValueType.UNIT_INTERVAL;
import static org.hisp.dhis.common.ValueType.URL;
import static org.hisp.dhis.common.ValueType.USERNAME;
import static org.hisp.dhis.system.util.ValidationUtils.bboxIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.coordinateIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;
import static org.hisp.dhis.system.util.ValidationUtils.emailIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.expressionIsValidSQl;
import static org.hisp.dhis.system.util.ValidationUtils.getLatitude;
import static org.hisp.dhis.system.util.ValidationUtils.getLongitude;
import static org.hisp.dhis.system.util.ValidationUtils.isPhoneNumber;
import static org.hisp.dhis.system.util.ValidationUtils.isValidHexColor;
import static org.hisp.dhis.system.util.ValidationUtils.isValidLetter;
import static org.hisp.dhis.system.util.ValidationUtils.normalizeBoolean;
import static org.hisp.dhis.system.util.ValidationUtils.passwordIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.valueIsComparable;
import static org.hisp.dhis.system.util.ValidationUtils.valueIsValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.FileTypeValueOptions;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ValidationUtils}.
 *
 * @author Lars Helge Overland
 */
class ValidationUtilsTest
{
    @Test
    void testCoordinateIsValid()
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
    void testBboxIsValid()
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
    void testGetLongitude()
    {
        assertEquals( "+37.99034", getLongitude( "[+37.99034,-28.94221]" ) );
        assertEquals( "37.99034", getLongitude( "[37.99034,28.94221]" ) );
        assertNull( getLongitude( "23.34343,56.3232" ) );
        assertNull( getLongitude( null ) );
    }

    @Test
    void testGetLatitude()
    {
        assertEquals( "-28.94221", getLatitude( "[+37.99034,-28.94221]" ) );
        assertEquals( "28.94221", getLatitude( "[37.99034,28.94221]" ) );
        assertNull( getLatitude( "23.34343,56.3232" ) );
        assertNull( getLatitude( null ) );
    }

    @Test
    void testPasswordIsValid()
    {
        assertFalse( passwordIsValid( "Johnd1" ) );
        assertFalse( passwordIsValid( "johndoe1" ) );
        assertFalse( passwordIsValid( "Johndoedoe" ) );
        assertTrue( passwordIsValid( "Johndoe1" ) );
    }

    @Test
    void testEmailIsValid()
    {
        assertFalse( emailIsValid( "john@doe" ) );
        assertTrue( emailIsValid( "john@doe.com" ) );
    }

    @Test
    void testUuidIsValid()
    {
        assertTrue( uuidIsValid( "0b976c48-4577-437b-bba6-794d0e7ebde0" ) );
        assertTrue( uuidIsValid( "38052fd0-8c7a-4330-ac45-2c53b3a41a78" ) );
        assertTrue( uuidIsValid( "50be5898-2413-465f-91b9-aced950fc3ab" ) );
        assertFalse( uuidIsValid( "Jjg3j3-412-1435-342-jajg8234f" ) );
        assertFalse( uuidIsValid( "6cafdc73_2ca4_4c52-8a0a-d38adec33b24" ) );
        assertFalse( uuidIsValid( "e1809673dbf3482d8f84e493c65f74d9" ) );
    }

    @Test
    void testUsernameIsValid()
    {
        assertTrue( usernameIsValid( "johnmichaeldoe", false ) );
        assertTrue( usernameIsValid( "ted@johnson.com", false ) );
        assertTrue( usernameIsValid( "harry@gmail.com", false ) );
        assertTrue( usernameIsValid( "har_ry@gmail.com", false ) );
        assertTrue( usernameIsValid( "Harry@gmail.com", false ) );
        assertTrue( usernameIsValid( "TeD@johnSon.com", false ) );
        assertTrue( usernameIsValid( "Harry-JohnSon", false ) );
        assertFalse( usernameIsValid( "_harry@gmail.com", false ) );
        assertFalse( usernameIsValid( "harry@gmail.com_", false ) );
        assertFalse( usernameIsValid( ".harry@gmail.com", false ) );
        assertFalse( usernameIsValid( "harry@gmail.com.", false ) );
        assertFalse( usernameIsValid( "@harry@gmail.com", false ) );
        assertFalse( usernameIsValid( "harry@gmail.com@", false ) );
        assertFalse( usernameIsValid( "harry_@gmail.com", false ) );
        assertFalse( usernameIsValid( "har__ry@gmail.com", false ) );
        assertFalse( usernameIsValid( "harry@@gmail.com", false ) );
        assertFalse( usernameIsValid( "harry..gmail.com", false ) );
        assertFalse( usernameIsValid( "harry--gmail.com", false ) );
        assertFalse( usernameIsValid( "-harry-gmail.com", false ) );
        assertFalse( usernameIsValid( "harry-gmail.com-", false ) );

        assertFalse( usernameIsValid( null, false ) );
        assertFalse( usernameIsValid( CodeGenerator.generateCode( 400 ), false ) );
    }

    @Test
    void testDataValueIsZeroAndInsignificant()
    {
        DataElement de = new DataElement( "DEA" );

        de.setValueType( INTEGER );
        de.setAggregationType( AggregationType.SUM );
        assertTrue( dataValueIsZeroAndInsignificant( "0", de ) );

        de.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        assertFalse( dataValueIsZeroAndInsignificant( "0", de ) );
    }

    @Test
    void testValueIsComparableForIntegerTypes()
    {
        ValueType valueType = INTEGER;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "-1 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = INTEGER_POSITIVE;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "-1 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = INTEGER_NEGATIVE;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "-1 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = INTEGER_ZERO_OR_POSITIVE;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "-1 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );
    }

    @Test
    void testValueIsComparableForDoubleTypes()
    {
        ValueType valueType;
        valueType = NUMBER;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "2.45", valueType ) );
        assertTrue( valueIsComparable( "-2.45", valueType ) );
        assertTrue( valueIsComparable( "-2.45 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = UNIT_INTERVAL;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "2.45", valueType ) );
        assertTrue( valueIsComparable( "-2.45", valueType ) );
        assertTrue( valueIsComparable( "-2.45 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = PERCENTAGE;
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "2.45", valueType ) );
        assertTrue( valueIsComparable( "-2.45", valueType ) );
        assertTrue( valueIsComparable( "-2.45 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );
    }

    @Test
    void testValueIsComparableForBooleanTypes()
    {
        ValueType valueType;
        valueType = BOOLEAN;
        assertTrue( valueIsComparable( "true", valueType ) );
        assertTrue( valueIsComparable( "false", valueType ) );
        assertTrue( valueIsComparable( "false ", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "1 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = TRUE_ONLY;
        assertTrue( valueIsComparable( "true", valueType ) );
        assertTrue( valueIsComparable( "false", valueType ) );
        assertTrue( valueIsComparable( "false ", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "1 ", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );
    }

    @Test
    void testValueIsComparableForDateTimeTypes()
    {
        ValueType valueType;
        valueType = DATE;
        assertTrue( valueIsComparable( "2013-04-01", valueType ) );
        assertFalse( valueIsComparable( "2013/04/01", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = TIME;
        assertTrue( valueIsComparable( "12:30", valueType ) );
        assertFalse( valueIsComparable( "12.30", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
        assertFalse( valueIsComparable( "a", valueType ) );

        valueType = DATETIME;
        assertTrue( valueIsComparable( "2021-08-30T13:53:33.767412Z", valueType ) );
        assertTrue( valueIsComparable( "2021-08-30T13:53:33.767412", valueType ) );
        assertTrue( valueIsComparable( "2013-04-01T11:12:05.5417Z", valueType ) );
        assertTrue( valueIsComparable( "2013-04-01T11:12:05.5417", valueType ) );
        assertTrue( valueIsComparable( "2013-04-01T11:12:02.541Z", valueType ) );
        assertTrue( valueIsComparable( "2021-08-30T13:53:33.741", valueType ) );
        assertTrue( valueIsComparable( "2013-04-01T11:12:00", valueType ) );
        assertFalse( valueIsComparable( "2021-08-30T13.53.33.767412Z", valueType ) );
        assertFalse( valueIsComparable( "2021-08-30T13.53.33.767412", valueType ) );
        assertFalse( valueIsComparable( "2013-04-01T11.12.05.5417Z", valueType ) );
        assertFalse( valueIsComparable( "2021-08-30T13.53.33.741", valueType ) );
        assertFalse( valueIsComparable( "2013-04-01T11.12.00", valueType ) );
        assertFalse( valueIsComparable( "2013-04-01", valueType ) );
        assertFalse( valueIsComparable( "abcd", valueType ) );
        assertFalse( valueIsComparable( "-", valueType ) );
    }

    @Test
    void testValueIsComparableForStringTypes()
    {
        ValueType valueType;
        valueType = LONG_TEXT;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );

        valueType = MULTI_TEXT;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );
        assertTrue( valueIsComparable( " ", valueType ) );

        valueType = PHONE_NUMBER;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "(+355)", valueType ) );
        assertTrue( valueIsComparable( "5-1234-5", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );

        valueType = EMAIL;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "5_1234_5", valueType ) );
        assertTrue( valueIsComparable( "5-1234-5", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );

        valueType = TEXT;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );
        assertTrue( valueIsComparable( " ", valueType ) );

        valueType = LETTER;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );
        assertTrue( valueIsComparable( " ", valueType ) );

        valueType = COORDINATE;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( ":", valueType ) );
        assertTrue( valueIsComparable( ".", valueType ) );

        valueType = URL;
        assertTrue( valueIsComparable( "http", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( ":", valueType ) );
        assertTrue( valueIsComparable( ".", valueType ) );

        valueType = FILE_RESOURCE;
        assertTrue( valueIsComparable( "file://", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );
        assertTrue( valueIsComparable( ":", valueType ) );
        assertTrue( valueIsComparable( ".", valueType ) );

        valueType = IMAGE;
        assertTrue( valueIsComparable( "file://", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );
        assertTrue( valueIsComparable( ":", valueType ) );
        assertTrue( valueIsComparable( ".", valueType ) );

        valueType = USERNAME;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "@", valueType ) );

        valueType = GEOJSON;
        assertTrue( valueIsComparable( "a", valueType ) );
        assertTrue( valueIsComparable( "abc", valueType ) );
        assertTrue( valueIsComparable( "1", valueType ) );
        assertTrue( valueIsComparable( "0", valueType ) );
        assertTrue( valueIsComparable( "-1", valueType ) );
        assertTrue( valueIsComparable( "[", valueType ) );
        assertTrue( valueIsComparable( ".", valueType ) );
        assertTrue( valueIsComparable( " ", valueType ) );

        // Applicable for all value types.
        assertFalse( valueIsComparable( "", valueType ) );
        assertFalse( valueIsComparable( null, valueType ) );
    }

    @Test
    void testValueIsValid()
    {
        DataElement de = new DataElement( "DEA" );

        de.setValueType( INTEGER );
        assertNull( valueIsValid( null, de ) );
        assertNull( valueIsValid( "", de ) );
        assertNull( valueIsValid( "34", de ) );
        assertNotNull( valueIsValid( "Yes", de ) );

        de.setValueType( NUMBER );
        assertNull( valueIsValid( "3.7", de ) );
        assertNotNull( valueIsValid( "No", de ) );

        de.setValueType( INTEGER_POSITIVE );
        assertNull( valueIsValid( "3", de ) );
        assertNotNull( valueIsValid( "-4", de ) );

        de.setValueType( INTEGER_ZERO_OR_POSITIVE );
        assertNull( valueIsValid( "3", de ) );
        assertNotNull( valueIsValid( "-4", de ) );

        de.setValueType( INTEGER_NEGATIVE );
        assertNull( valueIsValid( "-3", de ) );
        assertNotNull( valueIsValid( "4", de ) );

        de.setValueType( TEXT );
        assertNull( valueIsValid( "0", de ) );

        de.setValueType( BOOLEAN );
        assertNull( valueIsValid( "true", de ) );
        assertNull( valueIsValid( "false", de ) );
        assertNull( valueIsValid( "FALSE", de ) );
        assertNotNull( valueIsValid( "yes", de ) );

        de.setValueType( TRUE_ONLY );
        assertNull( valueIsValid( "true", de ) );
        assertNull( valueIsValid( "TRUE", de ) );
        assertNotNull( valueIsValid( "false", de ) );

        de.setValueType( DATE );
        assertNull( valueIsValid( "2013-04-01", de ) );
        assertNotNull( valueIsValid( "2012304-01", de ) );
        assertNotNull( valueIsValid( "Date", de ) );

        de.setValueType( DATETIME );
        assertNull( valueIsValid( "2021-08-30T13:53:33.767412Z", de ) );
        assertNull( valueIsValid( "2021-08-30T13:53:33.767412", de ) );
        assertNull( valueIsValid( "2013-04-01T11:12:05.5417Z", de ) );
        assertNull( valueIsValid( "2013-04-01T11:12:05.5417", de ) );
        assertNull( valueIsValid( "2013-04-01T11:12:02.541Z", de ) );
        assertNull( valueIsValid( "2021-08-30T13:53:33.741", de ) );
        assertNull( valueIsValid( "2013-04-01T11:12:00", de ) );
        assertNotNull( valueIsValid( "2013-04-01", de ) );
        assertNotNull( valueIsValid( "abcd", de ) );
    }

    @Test
    void testIsValidHexColor()
    {
        assertFalse( isValidHexColor( "abcpqr" ) );
        assertFalse( isValidHexColor( "#qwerty" ) );
        assertFalse( isValidHexColor( "FFAB#O" ) );
        assertFalse( isValidHexColor( "#aaee88ee" ) );
        assertTrue( isValidHexColor( "#FF0" ) );
        assertTrue( isValidHexColor( "#FF0000" ) );
        assertTrue( isValidHexColor( "FFFFFF" ) );
        assertTrue( isValidHexColor( "ffAAb4" ) );
        assertTrue( isValidHexColor( "#4a6" ) );
        assertTrue( isValidHexColor( "abc" ) );
    }

    @Test
    void testIsPhoneNumber()
    {
        assertTrue( isPhoneNumber( "+ 47 33 987 937" ) );
        assertTrue( isPhoneNumber( "+4733987937" ) );
        assertTrue( isPhoneNumber( "123456" ) );
        assertTrue( isPhoneNumber( "(+47) 3398 7937" ) );
        assertTrue( isPhoneNumber( "(47) 3398 7937" ) );
        assertTrue( isPhoneNumber( "(47) 3398 7937 ext 123" ) );
        assertTrue( isPhoneNumber( "(47) 3398 7937.123" ) );
        // 50 characters
        assertTrue( isPhoneNumber( "01234567890123456789012345678901234567890123456789" ) );
        // 51 characters
        assertFalse( isPhoneNumber( "012345678901234567890123456789012345678901234567890" ) );
        assertFalse( isPhoneNumber( "+AA4733987937" ) );
        assertFalse( isPhoneNumber( "+AA4733987937" ) );
        assertFalse( isPhoneNumber( "12345" ) );
        assertFalse( isPhoneNumber( "" ) );
        assertFalse( isPhoneNumber( " " ) );
    }

    @Test
    void testExpressionIsValidSQl()
    {
        assertFalse( expressionIsValidSQl( "10 == 10; delete from table" ) );
        assertFalse( expressionIsValidSQl( "select from table" ) );
        assertTrue( expressionIsValidSQl( "\"abcdef12345\" < 30" ) );
        assertTrue( expressionIsValidSQl( "\"abcdef12345\" >= \"bcdefg23456\"" ) );
        assertTrue( expressionIsValidSQl( "\"DO0v7fkhUNd\" > -30000 and \"DO0v7fkhUNd\" < 30000" ) );
        assertTrue( expressionIsValidSQl( "\"oZg33kd9taw\" == 'Female'" ) );
        assertTrue( expressionIsValidSQl( "\"oZg33kd9taw\" == 'Female' and \"qrur9Dvnyt5\" <= 5" ) );
    }

    @Test
    void testNormalizeBoolean()
    {
        assertEquals( "true", normalizeBoolean( "1", BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "T", BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "true", BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "TRUE", BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "t", BOOLEAN ) );
        assertEquals( "test", normalizeBoolean( "test", TEXT ) );
        assertEquals( "false", normalizeBoolean( "0", BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "f", BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "False", BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "FALSE", BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "F", BOOLEAN ) );
    }

    @Test
    void testFileValueTypeOptionValidation()
        throws IOException
    {
        long oneHundredMegaBytes = 1024 * (1024 * 100L);
        ValueType valueType = FILE_RESOURCE;

        FileTypeValueOptions options = new FileTypeValueOptions();
        options.setMaxFileSize( oneHundredMegaBytes );
        options.setAllowedContentTypes( Set.of( "jpg", "pdf" ) );

        FileResource fileResource = new FileResource( "name", "jpg", oneHundredMegaBytes, "md5sum",
            FileResourceDomain.DOCUMENT );
        assertNull( valueIsValid( fileResource, valueType, options ) );

        fileResource = new FileResource( "name", "jpg", 1024 * (1024 * 101L), "md5sum", FileResourceDomain.DOCUMENT );
        assertEquals( "not_valid_file_size_too_big", valueIsValid( fileResource, valueType, options ) );

        fileResource = new FileResource( "name", "exe", oneHundredMegaBytes, "md5sum", FileResourceDomain.DOCUMENT );
        assertEquals( "not_valid_file_content_type", valueIsValid( fileResource, valueType, options ) );
    }

    @Test
    void testIsValidLetter()
    {
        List<String> valid = List.of( "a", "A", "é", "â", "ß", "ä" );
        valid.forEach( letter -> assertTrue( isValidLetter( letter ) ) );

        List<String> invalid = List.of( "1", "", "aa", "=", "," );
        invalid.forEach( value -> assertFalse( isValidLetter( value ) ) );
    }
}
