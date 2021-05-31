/*
 * Copyright (c) 2004-2021, University of Oslo
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
import static org.hisp.dhis.util.Constants.RESERVED_VALUE_GENERATION_ATTEMPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.IntStream;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.textpattern.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class ReservedValueServiceTest
{

    private ReservedValueService reservedValueService;

    private final TextPatternService textPatternService = new DefaultTextPatternService();

    @Mock
    private ReservedValueStore reservedValueStore;

    @Mock
    private SequentialNumberCounterStore sequentialNumberCounterStore;

    @Captor
    private ArgumentCaptor<ReservedValue> reservedValue;

    private static final String simpleText = "\"FOO\"";

    private static final String sequentialText = "\"TEST-\"+SEQUENTIAL(##)";

    private static final String randomText = "\"TEST-\"+RANDOM(XXX)";

    private static Date futureDate;

    private static final String ownerUid = "uid";

    @Before
    public void setUpClass()
    {
        reservedValueService = new DefaultReservedValueService( textPatternService, reservedValueStore,
            new ValueGeneratorService( sequentialNumberCounterStore ) );

        Calendar calendar = Calendar.getInstance();
        calendar.add( DATE, 1 );
        futureDate = calendar.getTime();
    }

    @Test
    public void shouldReserveSimpleTextPattern()
        throws TextPatternParser.TextPatternParsingException,
        TextPatternGenerationException,
        ReserveValueException
    {
        assertEquals( 1,
            reservedValueService.reserve( createTextPattern( Objects.TRACKEDENTITYATTRIBUTE, ownerUid, simpleText ), 1,
                new HashMap<>(), futureDate ).size() );

        verify( reservedValueStore, times( 1 ) ).reserveValues( any() );
    }

    @Test
    public void shouldNotReserveSimpleTextPatternAlreadyReserved()
    {
        when( reservedValueStore.getNumberOfUsedValues( reservedValue.capture() ) ).thenReturn( 1 );

        assertThrows( ReserveValueException.class,
            () -> reservedValueService.reserve(
                createTextPattern( Objects.TRACKEDENTITYATTRIBUTE, ownerUid, simpleText ),
                1, new HashMap<>(), futureDate ) );

        assertEquals( Objects.TRACKEDENTITYATTRIBUTE.name(), reservedValue.getValue().getOwnerObject() );
        assertEquals( ownerUid, reservedValue.getValue().getOwnerUid() );
        verify( reservedValueStore, times( 0 ) ).reserveValues( any() );
    }

    @Test
    public void shouldNotReserveValuesSequentialPattern()
        throws TextPatternParser.TextPatternParsingException,
        TextPatternGenerationException,
        ReserveValueException
    {
        List<Integer> generatedValues = new ArrayList<>();

        IntStream.range( 1, RESERVED_VALUE_GENERATION_ATTEMPT + 1 ).forEach( generatedValues::add );

        when( sequentialNumberCounterStore.getNextValues( any(), any(), anyInt() ) )
            .thenReturn( generatedValues );

        assertEquals( RESERVED_VALUE_GENERATION_ATTEMPT,
            reservedValueService
                .reserve( createTextPattern( Objects.TRACKEDENTITYATTRIBUTE, ownerUid, sequentialText ), 10,
                    new HashMap<>(), futureDate )
                .size() );

        verify( reservedValueStore, times( 0 ) ).reserveValues( any() );
    }

    @Test
    public void shouldReserveValuesRandomPattern()
        throws TextPatternParser.TextPatternParsingException,
        TextPatternGenerationException,
        ReserveValueException
    {
        when( reservedValueStore.reserveValuesAndCheckUniqueness( any(), any() ) )
            .thenReturn( Arrays.asList( ReservedValue.builder().build(), ReservedValue.builder().build() ) );

        assertEquals( 2,
            reservedValueService
                .reserve( createTextPattern( Objects.TRACKEDENTITYATTRIBUTE, ownerUid, randomText ), 2,
                    new HashMap<>(), futureDate )
                .size() );

        verify( reservedValueStore, times( 1 ) ).reserveValuesAndCheckUniqueness( any(), any() );
    }

    private static TextPattern createTextPattern( Objects objects, String uid, String pattern )
        throws TextPatternParser.TextPatternParsingException
    {
        TextPattern tp = TextPatternParser.parse( pattern );
        tp.setOwnerObject( objects );
        tp.setOwnerUid( uid );
        return tp;
    }

}
