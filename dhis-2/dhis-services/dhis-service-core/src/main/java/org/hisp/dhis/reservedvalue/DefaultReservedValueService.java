package org.hisp.dhis.reservedvalue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import java.util.stream.Collectors;

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
        throws Exception
    {
        int attemptsLeft = 10;

        List<String> resultList = new ArrayList<>();

        String key = textPatternService.resolvePattern( textPattern, values );

        ReservedValue reservedValue = new ReservedValue( textPattern.getClazz(), textPattern.getOwnerUID(), key, null,
            expires );

        TextPatternSegment generatedSegment = getGeneratedSegment( textPattern );

        if ( hasEnoughValuesLeft( reservedValue, TextPatternValidationUtils.getTotalValuesPotential( generatedSegment ),
            numberOfReservations ) )
        {
            throw new Exception( "Not enough values left to reserve " + numberOfReservations + " values." );
        }

        if ( generatedSegment == null )
        {
            if ( numberOfReservations == 1 )
            {
                reservedValue.setValue( key );
                reservedValueStore.reserveValues( reservedValue, Lists.newArrayList( key ) );

                return Lists.newArrayList( key );
            }
            else
            {
                throw new Exception( "Trying to reserve multiple values based on pattern with no generated segment." );
            }
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
                .map( Object::toString )
                .collect( Collectors.toList() ) );
        }
        else if ( segment.getMethod().equals( TextPatternMethod.RANDOM ) )
        {
            for ( int i = 0; i < numberOfValues; i++ )
            {
                generatedValues.add( TextPatternMethodUtils.generateRandom( segment.getParameter() ) );
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
