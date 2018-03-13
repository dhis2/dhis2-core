package org.hisp.dhis.reservedvalue;

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

import com.google.common.collect.Lists;
import org.apache.commons.collections.ListUtils;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Calendar.DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultReservedValueServiceTest
    extends DhisTest
{
    @Autowired
    private DefaultReservedValueService reservedValueService;

    @Autowired
    private ReservedValueStore reservedValueStore;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // Preset values
    private static Date future;

    private static TextPattern simpleTextPattern;

    private static TextPattern simpleSequentialTextPattern;

    private static TextPattern simpleRandomTextPattern;

    private static TextPattern simpleStringPattern;

    private static ReservedValue simpleReservedValue;

    @BeforeClass
    public static void setUpClass()
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
        simpleStringPattern = createTextPattern( tea, "\"TEST-\"+ORG_UNIT_CODE(..)" );

        // Set up reserved values
        simpleReservedValue = createReservedValue( tea, "FOOBAR" );
    }

    @Before
    public void setUpTest()
        throws Exception
    {
        reservedValueStore.getAll().forEach( reservedValueStore::delete );
    }

    @After
    public void tearDown()
    {
        reservedValueStore.getAll().forEach( reservedValueStore::delete );
    }

    @Override
    protected boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testReserveReserveASingleSimpleValueWhenNotUsed()
        throws Exception
    {
        List<ReservedValue> res = reservedValueService.reserve( simpleTextPattern, 1, new HashMap<>(), future );

        assertEquals( "FOOBAR", res.get( 0 ).getValue() );
        assertEquals( 1, reservedValueStore.getCount() );
    }

    @Test
    public void testReserveReserveASingleSimpleValueWhenUsed()
    {
        reservedValueStore.reserveValues( simpleReservedValue, Lists.newArrayList( "FOOBAR" ) );

        List<ReservedValue> res = null;

        try
        {
            res = reservedValueService.reserve( simpleTextPattern, 1, new HashMap<>(), future );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof ReservedValueService.ReserveValueException );
            assertEquals( "Could not reserve value: Not enough values left to reserve 1 values.", e.getMessage() );
        }

        assertNull( res );
        assertEquals( 1, reservedValueStore.getCount() );
    }

    @Test
    public void testReserveReserveATwoSimpleValuesShouldFail()
    {
        List<ReservedValue> res = null;

        try
        {
            res = reservedValueService.reserve( simpleTextPattern, 2, new HashMap<>(), future );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof ReservedValueService.ReserveValueException );
            assertEquals( "Could not reserve value: Not enough values left to reserve 2 values.", e.getMessage() );
        }

        assertNull( res );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    public void testReserveReserveMultipleRandomValues()
        throws TextPatternService.TextPatternGenerationException, ReservedValueService.ReserveValueException
    {
        reservedValueService.reserve( simpleRandomTextPattern, 3, new HashMap<>(), future );

        List<ReservedValue> all = reservedValueStore.getAll();
        assertEquals( 3, all.stream()
            .filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 8 )
            .count() );
        assertEquals( 3, all.size() );
    }

    @Test
    public void testReserveReserveASequentialValueWhenNotUsed()
        throws Exception
    {
        List<ReservedValue> res = reservedValueService.reserve( simpleSequentialTextPattern, 1, new HashMap<>(), future );

        assertEquals( 1, res.stream().filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 1, reservedValueStore.getCount() );

    }

    @Test
    public void testReserveReserveMultipleSequentialValueWhenNotUsed()
        throws Exception
    {
        List<ReservedValue> res = reservedValueService.reserve( simpleSequentialTextPattern, 50, new HashMap<>(), future );

        assertEquals( 50, res.stream().filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 50, reservedValueStore.getCount() );

    }

    @Test
    public void testReserveReserveMultipleSequentialValueWhenSomeExists()
        throws Exception
    {
        List<ReservedValue> reserved = reservedValueService
            .reserve( simpleSequentialTextPattern, 50, new HashMap<>(), future );

        // Make sure they where added successfully
        assertEquals( 50,
            reserved.stream().filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 50, reservedValueStore.getCount() );

        List<ReservedValue> res = reservedValueService.reserve( simpleSequentialTextPattern, 25, new HashMap<>(), future );

        assertTrue( ListUtils.intersection( reserved, res ).isEmpty() );
        assertEquals( 25, res.stream().filter( ( rv ) -> rv.getValue().indexOf( "TEST-" ) == 0 && rv.getValue().length() == 7 ).count() );
        assertEquals( 75, reservedValueStore.getCount() );

    }

    @Test
    public void testReserveReserveTooManySequentialValuesWhenNoneExists()
        throws Exception
    {
        thrown.expect( ReservedValueService.ReserveValueException.class );
        thrown.expectMessage( "Could not reserve value: Not enough values left to reserve 101 values." );

        reservedValueService.reserve( simpleSequentialTextPattern, 101, new HashMap<>(), future );
    }

    @Test
    public void testReserveReserveTooManySequentialValuesWhenSomeExists()
        throws Exception
    {
        assertEquals( 99,
            reservedValueService.reserve( simpleSequentialTextPattern, 99, new HashMap<>(), future ).size() );

        thrown.expect( ReservedValueService.ReserveValueException.class );
        thrown.expectMessage( "Could not reserve value: Not enough values left to reserve 1 values." );

        reservedValueService.reserve( simpleSequentialTextPattern, 1, new HashMap<>(), future );
    }

    @Test
    public void testReserveReserveStringValueWithValues()
        throws TextPatternService.TextPatternGenerationException, ReservedValueService.ReserveValueException
    {
        Map<String, String> map = new HashMap<>();
        map.put( "ORG_UNIT_CODE", "OSLO" );

        List<ReservedValue> result = reservedValueService.reserve( simpleStringPattern, 1, map, future );

        assertEquals( 1, result.size() );
        assertEquals( "TEST-OS", result.get( 0 ).getValue() );
    }

    @Test
    public void testUseReservationWhenReserved()
    {
        reservedValueStore.reserveValues( simpleReservedValue, Lists.newArrayList( "FOOBAR" ) );

        boolean res = reservedValueService.useReservedValue( simpleTextPattern, "FOOBAR" );

        assertTrue( res );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    @Test
    public void testUseReservationWhenNotReserved()
    {
        boolean res = reservedValueService.useReservedValue( simpleTextPattern, "FOOBAR" );

        assertFalse( res );
        assertEquals( 0, reservedValueStore.getCount() );
    }

    // Helpers

    private static TextPattern createTextPattern( IdentifiableObject owner, String pattern )
    {
        try
        {
            TextPattern tp = TextPatternParser.parse( pattern );
            tp.setOwnerObject( Objects.fromClass( owner.getClass() ) );
            tp.setOwnerUid( owner.getUid() );

            return tp;
        }
        catch ( TextPatternParser.TextPatternParsingException | IllegalAccessException e )
        {
            e.printStackTrace();
        }

        return null;
    }

    private static ReservedValue createReservedValue( IdentifiableObject owner, String key )
    {
        try
        {
            return new ReservedValue( Objects.fromClass( owner.getClass() ).name(), owner.getUid(), key, "",
                new Date() );
        }
        catch ( IllegalAccessException e )
        {
            e.printStackTrace();
            return null;
        }
    }
}