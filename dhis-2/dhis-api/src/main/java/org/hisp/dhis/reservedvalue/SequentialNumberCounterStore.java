package org.hisp.dhis.reservedvalue;

public interface SequentialNumberCounterStore
{
    int getNextValue( String uid, String key );

    int[] getNextValues( String uid, String key, int length );

    void deleteCounter( String uid );

}
