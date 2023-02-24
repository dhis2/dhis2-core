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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class JdbcAnalyticsTableManagerTest
{
    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AnalyticsExportSettings analyticsExportSettings;

    @Mock
    private PeriodDataProvider periodDataProvider;

    private AnalyticsTableManager subject;

    @BeforeEach
    public void setUp()
    {
        subject = new JdbcAnalyticsTableManager( mock( IdentifiableObjectManager.class ),
            mock( OrganisationUnitService.class ),
            mock( CategoryService.class ), systemSettingManager, mock( DataApprovalLevelService.class ),
            mock( ResourceTableService.class ), mock( AnalyticsTableHookService.class ), mock( StatementBuilder.class ),
            mock( PartitionManager.class ), mock( DatabaseInfo.class ), jdbcTemplate, analyticsExportSettings,
            periodDataProvider );
    }

    @Test
    void testGetRegularAnalyticsTable()
    {
        Date startTime = new DateTime( 2019, 3, 1, 10, 0 ).toDate();
        List<Integer> dataYears = List.of( 2018, 2019 );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .withStartTime( startTime )
            .build();

        when( jdbcTemplate.queryForList( Mockito.anyString(), ArgumentMatchers.<Class<Integer>> any() ) )
            .thenReturn( dataYears );

        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertEquals( 1, tables.size() );

        AnalyticsTable table = tables.get( 0 );

        assertNotNull( table );
        assertNotNull( table.getTablePartitions() );
        assertEquals( 2, table.getTablePartitions().size() );

        AnalyticsTablePartition partitionA = table.getTablePartitions().get( 0 );
        AnalyticsTablePartition partitionB = table.getTablePartitions().get( 1 );

        assertNotNull( partitionA );
        assertNotNull( partitionA.getStartDate() );
        assertNotNull( partitionA.getEndDate() );
        assertEquals( partitionA.getYear().intValue(), new DateTime( partitionA.getStartDate() ).getYear() );

        assertNotNull( partitionB );
        assertNotNull( partitionB.getStartDate() );
        assertNotNull( partitionB.getEndDate() );
        assertEquals( partitionB.getYear().intValue(), new DateTime( partitionB.getStartDate() ).getYear() );
    }

    @Test
    void testGetLatestAnalyticsTable()
    {
        Date lastFullTableUpdate = new DateTime( 2019, 3, 1, 2, 0 ).toDate();
        Date lastLatestPartitionUpdate = new DateTime( 2019, 3, 1, 9, 0 ).toDate();
        Date startTime = new DateTime( 2019, 3, 1, 10, 0 ).toDate();

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .withStartTime( startTime )
            .withLatestPartition()
            .build();

        List<Map<String, Object>> queryResp = new ArrayList<>();
        queryResp.add( Map.of( "dataelementid", 1 ) );

        when( systemSettingManager.getDateSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE ) )
            .thenReturn( lastFullTableUpdate );
        when( systemSettingManager.getDateSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE ) )
            .thenReturn( lastLatestPartitionUpdate );
        when( jdbcTemplate.queryForList( Mockito.anyString() ) ).thenReturn( queryResp );

        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertEquals( 1, tables.size() );

        AnalyticsTable table = tables.get( 0 );

        assertNotNull( table );
        assertNotNull( table.getTablePartitions() );
        assertEquals( 1, table.getTablePartitions().size() );

        AnalyticsTablePartition partition = table.getLatestPartition();

        assertNotNull( partition );
        assertTrue( partition.isLatestPartition() );
        assertEquals( lastFullTableUpdate, partition.getStartDate() );
        assertEquals( startTime, partition.getEndDate() );
    }

    @Test
    void testGetLatestAnalyticsTableNoFullTableUpdate()
    {
        Date lastLatestPartitionUpdate = new DateTime( 2019, 3, 1, 9, 0 ).toDate();
        Date startTime = new DateTime( 2019, 3, 1, 10, 0 ).toDate();

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .withStartTime( startTime )
            .withLatestPartition()
            .build();

        when( systemSettingManager.getDateSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE ) )
            .thenReturn( null );
        when( systemSettingManager.getDateSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE ) )
            .thenReturn( null );
        when( systemSettingManager.getDateSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE ) )
            .thenReturn( lastLatestPartitionUpdate );
        assertThrows( IllegalArgumentException.class, () -> subject.getAnalyticsTables( params ) );
    }
}
