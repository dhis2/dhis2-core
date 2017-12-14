package org.hisp.dhis.reservedvalue;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultReservedValueService
    implements ReservedValueService
{

    @Autowired
    private SequentialNumberCounterStore sequentialNumberCounterStore;

    @Override
    public List<String> generateAndReserveRandomValues( String uid, String key, String pattern, int num )
    {
        return null;
    }

    @Override
    public List<String> generateAndReserveSequentialValues( String uid, String key, String pattern, int num )
    {

        // TODO: Validate that there are available numbers left.
        List<String> values = sequentialNumberCounterStore.getNextValues( uid, key, num )
            .stream()
            .map( ( value ) ->
                (pattern.length() == 1 ?
                    value.toString() :
                    String.format( "%0" + (pattern.length() - value.toString().length()) + "d%s", 0, value ))
            )
            .collect( Collectors.toList() );

        reserveValues( uid, key, values );

        return values;
    }

    private void reserveValues( String uid, String key, List<String> values )
    {



    }

}
