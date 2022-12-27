/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class AnalyticsTableTest
{
    @Test
    void testGetTableName()
    {
        Program program = new Program( "ProgramA", "DescriptionA" );
        program.setUid( "UIDA" );
        AnalyticsTable tableA = new AnalyticsTable( AnalyticsTableType.EVENT, List.of(),
            List.of(), program );
        assertEquals( "analytics_event_uida", tableA.getTableName() );
    }

    @Test
    void testGetTablePartitionName()
    {
        Program program = new Program( "ProgramA", "DescriptionA" );
        program.setUid( "UIDA" );
        Period periodA = new YearlyPeriodType().createPeriod( new DateTime( 2014, 1, 1, 0, 0 ).toDate() );
        Period periodB = new YearlyPeriodType().createPeriod( new DateTime( 2015, 1, 1, 0, 0 ).toDate() );
        AnalyticsTable tableA = new AnalyticsTable( AnalyticsTableType.EVENT, List.of(),
            List.of(), program );
        tableA.addPartitionTable( 2014, periodA.getStartDate(), periodA.getEndDate() );
        tableA.addPartitionTable( 2015, periodB.getStartDate(), periodB.getEndDate() );
        AnalyticsTablePartition partitionA = tableA.getTablePartitions().get( 0 );
        AnalyticsTablePartition partitionB = tableA.getTablePartitions().get( 1 );
        assertNotNull( partitionA );
        assertNotNull( partitionB );
        assertEquals( "analytics_event_uida_2014", partitionA.getTableName() );
        assertEquals( "analytics_event_uida_2015", partitionB.getTableName() );
        assertEquals( "analytics_event_temp_uida_2014", partitionA.getTempTableName() );
        assertEquals( "analytics_event_temp_uida_2015", partitionB.getTempTableName() );
    }

    @Test
    void testEquals()
    {
        AnalyticsTable tableA = new AnalyticsTable( AnalyticsTableType.DATA_VALUE, List.of(), List.of() );
        AnalyticsTable tableB = new AnalyticsTable( AnalyticsTableType.DATA_VALUE, List.of(), List.of() );
        List<AnalyticsTable> uniqueList = new UniqueArrayList<>();
        uniqueList.add( tableA );
        uniqueList.add( tableB );
        assertEquals( 1, uniqueList.size() );
    }
}
