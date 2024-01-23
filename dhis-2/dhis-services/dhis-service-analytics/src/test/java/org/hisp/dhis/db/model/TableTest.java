package org.hisp.dhis.db.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TableTest
{
    @Test
    void testToStagingTable()
    {
        assertEquals( "_categorystructure_staging", Table.toStaging( "_categorystructure" ) );
        assertEquals( "analytics_staging", Table.toStaging( "analytics" ) );
    }

    @Test
    void testFromStagingTable()
    {
        assertEquals( "_categorystructure", Table.fromStaging( "_categorystructure_staging" ) );
        assertEquals( "analytics", Table.fromStaging( "analytics_staging" ) );
    }
}
