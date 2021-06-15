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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternMethodUtils;
import org.hisp.dhis.textpattern.TextPatternSegment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValueGeneratorService
{

    private final SequentialNumberCounterStore sequentialNumberCounterStore;

    public List<String> generateValues( TextPatternSegment segment, TextPattern textPattern, String key,
        int numberOfValues )
        throws ReserveValueException
    {
        List<String> generatedValues = new ArrayList<>();

        switch ( segment.getMethod() )
        {

        case SEQUENTIAL:

            BigInteger maxValue = BigInteger.TEN.pow( segment.getParameter().length() );
            List<Integer> generatedNumbers = sequentialNumberCounterStore
                .getNextValues( textPattern.getOwnerUid(), key, numberOfValues );

            boolean outOfValues = generatedNumbers.stream()
                .anyMatch( n -> maxValue.intValue() <= n );

            if ( outOfValues )
            {
                throw new ReserveValueException( "Unable to reserve value, no new values available." );
            }

            generatedValues.addAll(
                generatedNumbers
                    .stream()
                    .map( n -> String.format( "%0" + segment.getParameter().length() + "d", n ) )
                    .collect( Collectors.toList() ) );

            break;

        case RANDOM:

            for ( int i = 0; i < numberOfValues; i++ )
            {
                generatedValues
                    .add( TextPatternMethodUtils.generateRandom( new SecureRandom(), segment.getParameter() ) );
            }

            break;

        default:
            break;

        }

        return generatedValues;
    }
}
