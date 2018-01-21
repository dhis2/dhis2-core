package org.hisp.dhis.reservedvalue;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DefaultReservedValueService
    implements ReservedValueService
{


    @Autowired
    private SequentialNumberCounterStore sequentialNumberCounterStore;

    @Autowired
    private ReservedValueStore reservedValueStore;

    @Override
    public List<String> generateAndReserveRandomValues( String uid, String key, String pattern, int num )
    {
        return null;
    }

    @Override
    public List<String> generateAndReserveSequentialValues( String uid, String key, String pattern, int num )
    {

        return null;
    }
}
