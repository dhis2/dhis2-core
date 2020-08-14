package org.hisp.dhis.reservedvalue.hibernate;

import java.util.List;

import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luciano Fiandesio
 */
public class DummyService
{
    private SequentialNumberCounterStore sequentialNumberCounterStore;

    public DummyService( SequentialNumberCounterStore hibernateSequentialNumberCounterStore )
    {
        this.sequentialNumberCounterStore = hibernateSequentialNumberCounterStore;
    }

    @Transactional
    public List<Integer> getNextValues( String uid, String key, int length )
    {
        return sequentialNumberCounterStore.getNextValues( uid, key, length );
    }
    
    @Transactional
    public void deleteCounter( String uid )
    {
        sequentialNumberCounterStore.deleteCounter( uid );
    }
}
