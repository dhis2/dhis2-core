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
package org.hisp.dhis.analytics.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.IndexFunction.LOWER;
import static org.hisp.dhis.analytics.IndexType.BTREE;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.createIndexStatement;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexName;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexes;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.AnalyticsIndex;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link org.hisp.dhis.analytics.AnalyticsIndex}
 *
 * @author maikel arabori
 */
class AnalyticsIndexHelperTest
{
    @Test
    void testGetIndexes()
    {
        List<AnalyticsTablePartition> stubPartitions = List.of( stubAnalyticsTablePartition() );

        List<AnalyticsIndex> indexes = getIndexes( stubPartitions );

        assertThat( indexes, hasSize( 1 ) );
        assertThat( indexes.get( 0 ).getTable(), is( equalTo( "analytics_event_temp_2022" ) ) );
        assertThat( indexes.get( 0 ).getColumns(), hasSize( 1 ) );
        assertThat( indexes.get( 0 ).getType(), is( equalTo( BTREE ) ) );
    }

    @Test
    void testCreateIndexStatement()
    {
        AnalyticsIndex someAnalyticsIndex = new AnalyticsIndex( "table", List.of( "column" ), BTREE );

        String statement = createIndexStatement( someAnalyticsIndex, EVENT );

        assertThat( statement, containsString( "create index \"in_column_table" ) );
        assertThat( statement, containsString( "on table using" ) );
        assertThat( statement, containsString( "btree (column)" ) );
    }

    @Test
    void testGetIndexName()
    {
        AnalyticsIndex someAnalyticsIndex = new AnalyticsIndex( "table", List.of( "column" ), BTREE );

        String statement = getIndexName( someAnalyticsIndex, EVENT );

        assertThat( statement, containsString( "\"in_column_table" ) );
    }

    @Test
    void testGetIndexNameWithFunction()
    {
        AnalyticsIndex someAnalyticsIndex = new AnalyticsIndex( "table", List.of( "column" ), BTREE, LOWER );

        String statement = getIndexName( someAnalyticsIndex, EVENT );

        assertThat( statement, containsString( "\"in_column_table" ) );
        assertThat( statement, containsString( "_lower\"" ) );
    }

    private AnalyticsTablePartition stubAnalyticsTablePartition()
    {
        AnalyticsTablePartition analyticsTablePartitionStub = new AnalyticsTablePartition( stubAnalyticsTable(),
            2022, new Date(), new Date(), false );

        return analyticsTablePartitionStub;
    }

    private AnalyticsTable stubAnalyticsTable()
    {
        List<AnalyticsTableColumn> dimensionColumns = List.of( stubAnalyticsTableColumn() );
        List<AnalyticsTableColumn> valueColumns = List.of( stubAnalyticsTableColumn() );

        return new AnalyticsTable( EVENT, dimensionColumns, valueColumns );
    }

    private AnalyticsTableColumn stubAnalyticsTableColumn()
    {
        return new AnalyticsTableColumn( "column", TEXT, "c" ).withIndexType( BTREE );
    }
}
