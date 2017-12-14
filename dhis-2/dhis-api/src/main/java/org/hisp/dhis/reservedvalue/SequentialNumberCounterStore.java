package org.hisp.dhis.reservedvalue;

import java.util.List;

public interface SequentialNumberCounterStore
{
    List<Integer> getNextValues( String uid, String key, int length );

    void deleteCounter( String uid );

}
