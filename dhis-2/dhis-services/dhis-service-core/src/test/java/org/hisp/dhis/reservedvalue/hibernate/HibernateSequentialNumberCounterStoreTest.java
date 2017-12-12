package org.hisp.dhis.reservedvalue.hibernate;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.reservedvalue.SequentialNumberCounterStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class HibernateSequentialNumberCounterStoreTest
    extends DhisSpringTest
{
    @Autowired
    private SequentialNumberCounterStore store;

    @Test
    public void getNextValue()
    {
        // Same uid and key should increment value
        assertEquals( 1, store.getNextValue( "ABC", "ABC-#" ) );
        assertEquals( 2, store.getNextValue( "ABC", "ABC-#" ) );
        assertEquals( 3, store.getNextValue( "ABC", "ABC-#" ) );
        assertEquals( 4, store.getNextValue( "ABC", "ABC-#" ) );

        // Different uid or key should return new values
        assertEquals( 1, store.getNextValue( "DEF", "ABC-#" ) );
        assertEquals( 1, store.getNextValue( "DEF", "ABC-##" ) );
    }

    @Test
    public void getNextValues()
    {

        int[] result = store.getNextValues( "ABC", "ABC-#", 3 );

        assertEquals( 3, result.length );
        assertEquals( 1, result[0] );
        assertEquals( 2, result[1] );
        assertEquals( 3, result[2] );

        result = store.getNextValues( "ABC", "ABC-#", 50 );

        assertEquals( 50, result.length );
        assertEquals( 4, result[0] );
        assertEquals( 5, result[1] );
        assertEquals( 6, result[2] );
        assertEquals( 53, result[49] );

    }

    @Test
    public void deleteCounter()
    {
        assertEquals( 1, store.getNextValues( "ABC", "ABC-#", 3 )[0]);
        store.deleteCounter( "ABC" );
        assertEquals( 1, store.getNextValues( "ABC", "ABC-#", 3 )[0]);
        assertEquals( 1, store.getNextValues( "ABC", "ABC-##", 3 )[0]);
        assertEquals( 1, store.getNextValues( "ABC", "ABC-####", 3 )[0]);
        store.deleteCounter( "ABC" );
        assertEquals( 1, store.getNextValues( "ABC", "ABC-#", 3 )[0]);
        assertEquals( 1, store.getNextValues( "ABC", "ABC-##", 3 )[0]);
        assertEquals( 1, store.getNextValues( "ABC", "ABC-###", 3 )[0]);
    }
}