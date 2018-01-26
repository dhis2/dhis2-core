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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sun.javafx.binding.StringFormatter;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternMethod;
import org.hisp.dhis.textpattern.TextPatternMethodUtils;
import org.hisp.dhis.textpattern.TextPatternSegment;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author Stian Sandvold
 */
public class DefaultReservedValueService
    implements ReservedValueService
{

    @Autowired
    private TextPatternService textPatternService;

    @Autowired
    private ReservedValueStore reservedValueStore;

    @Autowired
    private SequentialNumberCounterStore sequentialNumberCounterStore;

    @Override
    public List<String> reserve( TextPattern textPattern, int numberOfReservations, Map<String, String> values,
        Date expires )
        throws ReserveValueException, TextPatternService.TextPatternGenerationException
    {
        int attemptsLeft = 10;

        List<String> resultList = new ArrayList<>();

        TextPatternSegment generatedSegment = getGeneratedSegment( textPattern );

        String key = textPatternService.resolvePattern( textPattern, values );

        // Used for searching value tables
        String valueKey = (generatedSegment != null ? key.replaceAll( generatedSegment.getRawSegment(), "%" ) : key);

        ReservedValue reservedValue = new ReservedValue( textPattern.getOwnerObject(), textPattern.getOwnerUID(), key,
            valueKey,
            expires );

        if ( !hasEnoughValuesLeft( reservedValue,
            TextPatternValidationUtils.getTotalValuesPotential( generatedSegment ),
            numberOfReservations ) )
        {
            throw new ReserveValueException( "Not enough values left to reserve " + numberOfReservations + " values." );
        }

        if ( generatedSegment == null && numberOfReservations == 1 )
        {
            reservedValue.setValue( key );
            reservedValueStore.reserveValues( reservedValue, Lists.newArrayList( key ) );

            return Lists.newArrayList( key );
        }

        while ( attemptsLeft-- > 0 && resultList.size() < numberOfReservations )
        {
            List<String> resolvedPatterns = new ArrayList<>();

            List<String> generatedValues = generateValues( textPattern, numberOfReservations - resultList.size() );

            // Get a list of resolved patterns.
            for ( int i = 0; i < numberOfReservations - resultList.size(); i++ )
            {
                resolvedPatterns.add( textPatternService.resolvePattern( textPattern,
                    ImmutableMap.<String, String>builder()
                        .putAll( values )
                        .put( generatedSegment.getRawSegment(), generatedValues.get( i ) )
                        .build() ) );
            }

            resultList.addAll( reservedValueStore.reserveValues( reservedValue, resolvedPatterns ).stream()
                .map( ReservedValue::getValue )
                .collect( Collectors.toList() ) );
        }

        return resultList;
    }

    @Override
    public boolean useReservedValue( TextPattern textPattern, String value )
    {
        return reservedValueStore.useReservedValue( textPattern.getOwnerUID(), value );
    }

    // Helper methods

    private TextPatternSegment getGeneratedSegment( TextPattern textPattern )
    {
        return textPattern.getSegments()
            .stream()
            .filter( ( tp ) -> tp.getMethod().isGenerated() )
            .findFirst()
            .orElse( null );
    }

    private List<String> generateValues( TextPattern textPattern, int numberOfValues )
    {
        List<String> generatedValues = new ArrayList<>();
        TextPatternSegment segment = getGeneratedSegment( textPattern );

        if ( segment.getMethod().equals( TextPatternMethod.SEQUENTIAL ) )
        {
            generatedValues.addAll( sequentialNumberCounterStore
                .getNextValues( textPattern.getOwnerUID(), segment.getParameter(), numberOfValues )
                .stream()
                .map( ( n ) -> StringFormatter.format( "%0" + segment.getParameter().length() + "d", n ).getValue() )
                .collect( Collectors.toList() ) );
        }
        else if ( segment.getMethod().equals( TextPatternMethod.RANDOM ) )
        {
            for ( int i = 0; i < numberOfValues; i++ )
            {
                generatedValues.add( TextPatternMethodUtils.generateRandom( new Random(), segment.getParameter() ) );
            }
        }

        return generatedValues;
    }

    private boolean hasEnoughValuesLeft( ReservedValue reservedValue, int totalValues, int valuesRequired )
    {
        int used = reservedValueStore.getNumberOfUsedValues( reservedValue );

        return (totalValues - used) >= valuesRequired;
    }
}
