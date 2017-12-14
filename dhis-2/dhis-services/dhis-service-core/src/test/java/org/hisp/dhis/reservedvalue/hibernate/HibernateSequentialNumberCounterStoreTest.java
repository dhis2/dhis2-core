package org.hisp.dhis.reservedvalue.hibernate;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HibernateSequentialNumberCounterStoreTest
    extends DhisSpringTest
{
    @Autowired
    private SequentialNumberCounterStore store;

    @Test
    public void getNextValues()
    {

        List<Integer> result = store.getNextValues( "ABC", "ABC-#", 3 );

        assertEquals( 3, result.size() );
        assertTrue( result.contains( 1 ) );
        assertTrue( result.contains( 2 ) );
        assertTrue( result.contains( 3 ) );

        result = store.getNextValues( "ABC", "ABC-#", 50 );

        assertEquals( 50, result.size() );
        assertTrue( result.contains( 4 ) );
        assertTrue( result.contains( 5 ) );
        assertTrue( result.contains( 52 ) );
        assertTrue( result.contains( 53 ) );

    }

    @Test
    public void deleteCounter()
    {
        assertTrue( store.getNextValues( "ABC", "ABC-#", 3 ).contains( 1 ) );

        store.deleteCounter( "ABC" );

        assertTrue( store.getNextValues( "ABC", "ABC-#", 3 ).contains( 1 ) );
        assertTrue( store.getNextValues( "ABC", "ABC-##", 3 ).contains( 1 ) );
        assertTrue( store.getNextValues( "ABC", "ABC-###", 3 ).contains( 1 ) );

        store.deleteCounter( "ABC" );

        assertTrue( store.getNextValues( "ABC", "ABC-#", 3 ).contains( 1 ) );
        assertTrue( store.getNextValues( "ABC", "ABC-##", 3 ).contains( 1 ) );
        assertTrue( store.getNextValues( "ABC", "ABC-###", 3 ).contains( 1 ) );
    }
}