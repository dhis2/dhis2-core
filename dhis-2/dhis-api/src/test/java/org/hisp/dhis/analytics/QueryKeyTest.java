package org.hisp.dhis.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class QueryKeyTest
{
    @Test
    public void testAsPlainKey()
    {
        String key = new QueryKey()
            .add( "dimension", "dx" )
            .add( "dimension", "pe" )
            .add( "filter", "ou" )
            .add( "aggregationType", AggregationType.SUM )
            .add( "skipMeta", true )
            .asPlainKey();

        assertEquals( "dimension:dx-dimension:pe-filter:ou-aggregationType:SUM-skipMeta:true", key );
    }

    @Test
    public void testAsPlainKeyIgnoreNull()
    {
        String key = new QueryKey()
            .add( "dimension", "dx" )
            .add( "filter", "ou" )
            .addIgnoreNull( "valueType", null )
            .asPlainKey();

        assertEquals( "dimension:dx-filter:ou", key );
    }

    @Test
    public void testNoCollision()
    {
        String keyA = new QueryKey()
            .add( "dimension", "dx" )
            .add( "dimension", "aZASaK6ebLC" )
            .add( "filter", "ou" )
            .add( "aggregationType", AggregationType.SUM )
            .asPlainKey();

        String keyB = new QueryKey()
            .add( "dimension", "dx" )
            .add( "dimension", "aZASaK6ebLD" )
            .add( "filter", "ou" )
            .add( "aggregationType", AggregationType.SUM )
            .asPlainKey();

        assertNotEquals( keyA, keyB );
    }
}
