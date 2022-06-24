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
package org.hisp.dhis.reservedvalue;

import static java.util.Calendar.DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternGenerationException;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

@Commit
class ReservedValueServiceIntegrationTest extends TransactionalIntegrationTest
{

    @Autowired
    private ReservedValueService reservedValueService;

    @Autowired
    private ReservedValueStore reservedValueStore;

    // Preset values
    private static Date future;

    private static TrackedEntityAttribute simpleTextPattern;

    private static TrackedEntityAttribute simpleSequentialTextPattern;

    private static TrackedEntityAttribute simpleRandomTextPattern;

    private static TrackedEntityAttribute simpleRandomTextNumericPattern;

    private static TrackedEntityAttribute simpleStringPattern;

    @BeforeAll
    static void setUpClass()
    {
        // Set up future Date
        Calendar calendar = Calendar.getInstance();
        calendar.add( DATE, 10 );
        future = calendar.getTime();
        // Set up dummy TrackedEntityAttribute
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'A' );
        // Set up text patterns
        simpleTextPattern = createTextPattern( tea, "\"FOOBAR\"" );
        simpleSequentialTextPattern = createTextPattern( tea, "\"TEST-\"+SEQUENTIAL(##)" );
        simpleRandomTextPattern = createTextPattern( tea, "\"TEST-\"+RANDOM(XXX)" );
        simpleRandomTextNumericPattern = createTextPattern( tea, "\"EPI_\"+RANDOM(######)" );
        simpleStringPattern = createTextPattern( tea, "\"TEST-\"+ORG_UNIT_CODE(..)" );
    }

    @Test
    void testReserveReserveASingleSimpleValueWhenNotUsed()
        throws Exception
    {
        List<ReservedValue> res = reservedValueService.reserve( simpleTextPattern, 1, new HashMap<>(), future );
        assertEquals( "FOOBAR", res.get( 0 ).getValue() );
        assertEquals( 1, reservedValueStore.getCount() );
    }

    @Test
    void testReserveReserveASingleSimpleValueWhenUsed()
        throws TextPatternGenerationException,
        ReserveValueException
    {
        reservedValueService.reserve( simpleTextPattern, 1, new HashMap<>(), future );
        assertThrows( ReserveValueException.class,
            () -> reservedValueService.reserve( simpleTextPattern, 1, new HashMap<>(), future ) );
        assertEquals( 1, reservedValueStore.getCount() );
    }

    @Test
    void testReserveReserveATwoSimpleValuesShouldFail()
    {
        assertThrows( ReserveValueException.class,
            () -> reservedValueService.reserve( simpleTextPattern, 2, new HashMap<>(), future ) );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    void testReserveReserveMultipleRandomValues()
        throws Exception
    {
        reservedValueService.reserve( simpleRandomTextPattern, 3, new HashMap<>(), future );
        List<ReservedValue> all = reservedValueStore.getAll();
        assertEquals( 3, all.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 8 ).count() );
        assertEquals( 3, all.size() );
    }

    @Test
    void testReserveReserveRandomValuesWithExistingGenerationAndAlphaNumericPattern()
        throws Exception
    {
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'A' );

        simpleRandomTextNumericPattern = createTextPattern( tea, "\"EPI_\"+RANDOM(######)" );

        ReservedValue reservedValue = new ReservedValue();
        reservedValue.setTrackedEntityAttributeId( tea.getId() );
        reservedValue.setCreated( new Date() );
        reservedValue.setExpiryDate( new Date() );
        reservedValue.setOwnerObject( simpleRandomTextNumericPattern.getTextPattern().getOwnerObject().toString() );
        reservedValue.setOwnerUid( tea.getUid() );
        reservedValue.setKey( "EPI_RANDOM(######)" );
        reservedValue.setValue( "EPI_000000" );

        reservedValueStore.save( reservedValue );

        reservedValueService.reserve( simpleRandomTextNumericPattern, 3, new HashMap<>(), future );

        List<ReservedValue> all = reservedValueStore.getAll();
        assertEquals( 4, all.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "EPI_" ) == 0 && rv.getValue().length() == 10 )
            .count() );
        assertEquals( 4, all.size() );
    }

    @Test
    void testReserveReserveASequentialValueWhenNotUsed()
        throws Exception
    {
        List<ReservedValue> res = reservedValueService.reserve( simpleSequentialTextPattern, 1, new HashMap<>(),
            future );
        assertEquals( 1, res.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    void testReserveReserveMultipleSequentialValueWhenNotUsed()
        throws Exception
    {
        List<ReservedValue> res = reservedValueService.reserve( simpleSequentialTextPattern, 50, new HashMap<>(),
            future );
        assertEquals( 50, res.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    void testReserveReserveMultipleSequentialValueWhenSomeExists()
        throws Exception
    {
        List<ReservedValue> reserved = reservedValueService.reserve( simpleSequentialTextPattern, 50, new HashMap<>(),
            future );
        assertEquals( 50, reserved.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 0, reservedValueStore.getCount() );
        List<ReservedValue> res = reservedValueService.reserve( simpleSequentialTextPattern, 25, new HashMap<>(),
            future );
        assertTrue( ListUtils.intersection( reserved, res ).isEmpty() );
        assertEquals( 25, res.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    void testReserveReserveTooManySequentialValuesWhenNoneExists()
    {
        assertThrows(
            ReserveValueException.class,
            () -> reservedValueService.reserve( simpleSequentialTextPattern, 101, new HashMap<>(), future ),
            "Could not reserve value: Not enough values left to reserve 101 values." );
    }

    @Test
    void testReserveReserveTooManySequentialValuesWhenSomeExists()
        throws Exception
    {
        assertEquals( 99,
            reservedValueService.reserve( simpleSequentialTextPattern, 99, new HashMap<>(), future ).size() );
        assertThrows(
            ReserveValueException.class,
            () -> reservedValueService.reserve( simpleSequentialTextPattern, 1, new HashMap<>(), future ),
            "Could not reserve value: Not enough values left to reserve 1 values." );
    }

    @Test
    void testReserveReserveStringValueWithValues()
        throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put( "ORG_UNIT_CODE", "OSLO" );
        List<ReservedValue> result = reservedValueService.reserve( simpleStringPattern, 1, map, future );
        assertEquals( 1, result.size() );
        assertEquals( "TEST-OS", result.get( 0 ).getValue() );
    }

    @Test
    void testUseReservationWhenReserved()
        throws TextPatternGenerationException,
        ReserveValueException
    {
        reservedValueService.reserve( simpleTextPattern, 1, new HashMap<>(), future );
        assertTrue( reservedValueService.useReservedValue( simpleTextPattern.getTextPattern(), "FOOBAR" ) );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    void testUseReservationWhenNotReserved()
    {
        assertFalse( reservedValueService.useReservedValue( simpleTextPattern.getTextPattern(), "FOOBAR" ) );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    private static TrackedEntityAttribute createTextPattern( IdentifiableObject owner, String pattern )
    {
        try
        {
            TextPattern textPattern = TextPatternParser.parse( pattern );
            textPattern.setOwnerObject( Objects.fromClass( owner.getClass() ) );
            textPattern.setOwnerUid( owner.getUid() );
            TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
            trackedEntityAttribute.setTextPattern( textPattern );
            trackedEntityAttribute.setGenerated( true );
            return trackedEntityAttribute;
        }
        catch ( TextPatternParser.TextPatternParsingException | IllegalAccessException e )
        {
            e.printStackTrace();
        }
        return null;
    }
}
