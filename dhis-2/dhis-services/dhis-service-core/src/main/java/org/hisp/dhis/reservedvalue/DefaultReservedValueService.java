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

import static org.hisp.dhis.util.Constants.RESERVED_VALUE_GENERATION_ATTEMPT;
import static org.hisp.dhis.util.Constants.RESERVED_VALUE_GENERATION_TIMEOUT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternGenerationException;
import org.hisp.dhis.textpattern.TextPatternMethod;
import org.hisp.dhis.textpattern.TextPatternSegment;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;

/**
 * @author Stian Sandvold
 */
@Slf4j
@Service( "org.hisp.dhis.reservedvalue.ReservedValueService" )
@RequiredArgsConstructor
public class DefaultReservedValueService
    implements ReservedValueService
{

    private final TextPatternService textPatternService;

    private final ReservedValueStore reservedValueStore;

    private final ValueGeneratorService valueGeneratorService;

    @Override
    @Transactional
    public List<ReservedValue> reserve( TrackedEntityAttribute trackedEntityAttribute, int numberOfReservations,
        Map<String, String> values,
        Date expires )
        throws ReserveValueException,
        TextPatternGenerationException
    {
        long startTime = System.currentTimeMillis();
        int attemptsLeft = RESERVED_VALUE_GENERATION_ATTEMPT;

        List<ReservedValue> resultList = new ArrayList<>();

        TextPattern textPattern = trackedEntityAttribute.getTextPattern();

        TextPatternSegment generatedSegment = textPattern.getSegments()
            .stream()
            .filter( ( tp ) -> tp.getMethod().isGenerated() )
            .findFirst()
            .orElse( null );

        String key = textPatternService.resolvePattern( textPattern, values );

        // Used for searching value tables
        String valueKey = (generatedSegment != null
            ? key.replaceAll( Pattern.quote( generatedSegment.getRawSegment() ), "%" )
            : key);

        ReservedValue reservedValue = ReservedValue.builder().created( new Date() )
            .ownerObject( textPattern.getOwnerObject().name() )
            .ownerUid( textPattern.getOwnerUid() ).key( key ).value( valueKey ).expiryDate( expires ).build();

        if ( (generatedSegment == null || !TextPatternMethod.SEQUENTIAL.equals( generatedSegment.getMethod() ))
            && !hasEnoughValuesLeft( reservedValue,
                TextPatternValidationUtils.getTotalValuesPotential( generatedSegment ),
                numberOfReservations ) )
        {
            throw new ReserveValueException( "Not enough values left to reserve " + numberOfReservations + " values." );
        }

        if ( generatedSegment == null )
        {
            if ( numberOfReservations == 1 )
            {
                List<ReservedValue> reservedValues = Collections
                    .singletonList( reservedValue.toBuilder().value( key ).build() );

                reservedValueStore.reserveValues( reservedValues );

                return reservedValues;
            }
        }
        else
        {
            if ( !trackedEntityAttribute.isGenerated() )
                throw new ReserveValueException( "Tracked Entity Attribute must use a generated pattern method" );

            List<String> generatedValues = new ArrayList<>();

            int numberOfValuesLeftToGenerate = numberOfReservations;

            boolean isPersistable = generatedSegment.getMethod().isPersistable();

            reservedValue.setTrackedentityattributeid( trackedEntityAttribute.getId() );

            try
            {
                while ( attemptsLeft-- > 0 && numberOfValuesLeftToGenerate > 0 )
                {
                    if ( System.currentTimeMillis() - startTime >= RESERVED_VALUE_GENERATION_TIMEOUT )
                    {
                        throw new TimeoutException( "Generation and reservation of values took too long" );
                    }

                    List<String> resolvedPatterns = new ArrayList<>();

                    generatedValues
                        .addAll( valueGeneratorService.generateValues( generatedSegment, textPattern, key,
                            numberOfReservations - resultList.size() ) );

                    // Get a list of resolved patterns
                    for ( String generatedValue : generatedValues )
                    {
                        resolvedPatterns.add( textPatternService.resolvePattern( textPattern,
                            ImmutableMap.<String, String> builder()
                                .putAll( values )
                                .put( generatedSegment.getMethod().name(), generatedValue )
                                .build() ) );
                    }

                    if ( isPersistable )
                    {
                        List<ReservedValue> availableValues = reservedValueStore.getAvailableValues( reservedValue,
                            resolvedPatterns.stream().distinct().collect( Collectors.toList() ),
                            textPattern.getOwnerObject().name() );

                        List<ReservedValue> requiredValues = availableValues.subList( 0,
                            Math.min( availableValues.size(), numberOfReservations ) );

                        reservedValueStore.bulkInsertReservedValues(
                            requiredValues );

                        resultList.addAll( requiredValues );
                    }
                    else
                    {
                        resultList.addAll(
                            resolvedPatterns.stream().map( value -> reservedValue.toBuilder().value( value ).build() )
                                .collect( Collectors.toList() ) );
                    }

                    numberOfValuesLeftToGenerate = numberOfReservations - resultList.size();

                    generatedValues = new ArrayList<>();
                }

            }
            catch ( TimeoutException ex )
            {
                log.warn( String.format(
                    "Generation and reservation of values for %s wih uid %s timed out. %s values was reserved. You might be running low on available values",
                    textPattern.getOwnerObject().name(), textPattern.getOwnerUid(), resultList.size() ) );
            }
            catch ( ExecutionException e )
            {
                log.error( String.format(
                    "Generation and reservation of values error %s : ", e.getMessage() ) );
            }
            catch ( InterruptedException e )
            {
                log.error( String.format(
                    "Generation and reservation of values error %s : ", e.getMessage() ) );

                Thread.currentThread().interrupt();
            }

        }

        return resultList;
    }

    private boolean hasEnoughValuesLeft( ReservedValue reservedValue, long totalValues, int valuesRequired )
    {
        int used = reservedValueStore.getNumberOfUsedValues( reservedValue );

        return totalValues >= valuesRequired + used;
    }

    @Override
    @Transactional
    public boolean useReservedValue( TextPattern textPattern, String value )
    {
        return reservedValueStore.useReservedValue( textPattern.getOwnerUid(), value );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isReserved( TextPattern textPattern, String value )
    {
        return reservedValueStore.isReserved( textPattern.getOwnerObject().name(), textPattern.getOwnerUid(), value );
    }

    @Override
    @Transactional
    public void deleteReservedValueByUid( String uid )
    {
        reservedValueStore.deleteReservedValueByUid( uid );
    }

    @Override
    @Transactional
    public void removeUsedOrExpiredReservations()
    {
        reservedValueStore.removeUsedOrExpiredReservations();
    }
}
