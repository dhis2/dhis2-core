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

import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.MARCH;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DATE;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.ENDDATE;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.OU;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.STARTDATE;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.TEIUID;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.StatementDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * {@see JdbcOwnershipAnalyticsTableManager} Tester.
 *
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class JdbcOwnershipAnalyticsTableManagerTest
    extends DhisConvenienceTest
{
    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private DataApprovalLevelService dataApprovalLevelService;

    @Mock
    private ResourceTableService resourceTableService;

    @Mock
    private AnalyticsTableHookService tableHookService;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private PartitionManager partitionManager;

    @Mock
    private DatabaseInfo databaseInfo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private JdbcConfiguration jdbcConfiguration;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private SqlRowSet rowSet;

    @Mock
    private JdbcOwnershipWriter writer;

    private static final Program programA = createProgram( 'A' );

    private static final Program programB = createProgramWithoutRegistration( 'B' );

    private static AnalyticsTable tableA;

    private static AnalyticsTable tableB;

    private static AnalyticsTablePartition partitionA;

    private JdbcOwnershipAnalyticsTableManager target;

    @BeforeEach
    public void setUp()
    {
        target = new JdbcOwnershipAnalyticsTableManager( idObjectManager, organisationUnitService, categoryService,
            systemSettingManager, dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder,
            partitionManager, databaseInfo, jdbcTemplate, jdbcConfiguration );

        tableA = new AnalyticsTable( AnalyticsTableType.OWNERSHIP, target.getFixedColumns(),
            emptyList(), programA );

        tableB = new AnalyticsTable( AnalyticsTableType.OWNERSHIP, target.getFixedColumns(),
            emptyList(), programB );

        partitionA = new AnalyticsTablePartition( tableA, 1, new Date(), new Date(), true );
    }

    @Test
    void testGetAnalyticsTableType()
    {
        assertEquals( AnalyticsTableType.OWNERSHIP, target.getAnalyticsTableType() );
    }

    @Test
    void testGetAnalyticsTables()
    {
        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( List.of( programA, programB ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .build();

        assertEquals( List.of( tableA, tableB ), target.getAnalyticsTables( params ) );

        params = AnalyticsTableUpdateParams.newBuilder()
            .withLastYears( AnalyticsTablePartition.LATEST_PARTITION )
            .build();

        assertEquals( emptyList(), target.getAnalyticsTables( params ) );
    }

    @Test
    void testGetPartitionChecks()
    {
        assertEquals( emptyList(), target.getPartitionChecks( partitionA ) );
    }

    @Test
    void testGetPartitionColumn()
    {
        assertNull( target.getPartitionColumn() );
    }

    @Test
    void testPopulateTable()
        throws SQLException
    {
        String tei1 = "teiUid00001";
        String tei2 = "teiUid00002";

        String ou1 = "orgUnit0001";
        String ou2 = "orgUnit0002";

        Date start1 = new GregorianCalendar( 2022, JANUARY, 1 ).getTime();
        Date end1 = new GregorianCalendar( 2022, JANUARY, 31 ).getTime();
        Date start2 = new GregorianCalendar( 2022, FEBRUARY, 1 ).getTime();
        Date end2 = new GregorianCalendar( 2022, FEBRUARY, 28 ).getTime();
        Date start3 = new GregorianCalendar( 2022, MARCH, 1 ).getTime();
        Date end3 = new GregorianCalendar( 2022, MARCH, 31 ).getTime();

        when( jdbcConfiguration.getDialect() ).thenReturn( StatementDialect.POSTGRESQL );
        when( jdbcConfiguration.getDataSource() ).thenReturn( dataSource );
        when( dataSource.getConnection() ).thenReturn( connection );
        when( connection.createStatement() ).thenReturn( statement );

        when( jdbcTemplate.queryForRowSet( anyString() ) ).thenReturn( rowSet );

        // Mock RowSet will return 5 successive rows:
        when( rowSet.next() ).thenReturn( true, true, true, true, true, false );

        // TEI uid:
        when( rowSet.getObject( 1 ) ).thenReturn( tei1, tei1, tei1, tei2, tei2 );

        // Start date:
        when( rowSet.getObject( 2 ) ).thenReturn( start1, start2, start3, start1, start2 );

        // End date (always null):
        when( rowSet.getObject( 3 ) ).thenReturn( end1, end2, end3, end1, end2 );

        // OrgUnit:
        when( rowSet.getObject( 4 ) ).thenReturn( ou1, ou2, ou1, ou1, ou2 );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .build();

        try ( MockedStatic<JdbcOwnershipWriter> mocked = mockStatic( JdbcOwnershipWriter.class ) )
        {
            mocked.when( () -> JdbcOwnershipWriter.getInstance( any() ) )
                .thenReturn( writer );

            target.populateTable( params, partitionA );
        }

        List<Invocation> jdbcInvocations = getInvocations( jdbcTemplate );
        assertEquals( 1, jdbcInvocations.size() );
        assertEquals( "queryForRowSet", jdbcInvocations.get( 0 ).getMethod().getName() );

        String sql = (String) jdbcInvocations.get( 0 ).getArgument( 0 );
        String sqlMasked = sql.replaceAll( "lastupdated <= '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}'",
            "lastupdated <= 'yyyy-mm-ddThh:mm:ss'" );
        assertEquals( "select tei.uid,o.ownatstart,null,ou.uid from ( " +
            "select trackedentityinstanceid, owndate+1 as ownatstart, right(max(owns),-23)::bigint " +
            "as organisationunitid from ( " +
            "select pi.trackedentityinstanceid, pi.enrollmentdate::date as owndate, " +
            "rpad(pi.enrollmentdate::text,23) || pi.organisationunitid::text as owns " +
            "from programinstance pi " +
            "where pi.programid=0 and pi.trackedentityinstanceid is not null " +
            "and pi.organisationunitid is not null " +
            "and pi.lastupdated <= 'yyyy-mm-ddThh:mm:ss' " +
            "union select poh.trackedentityinstanceid, poh.startdate::date as owndate, " +
            "rpad(poh.startdate::text,23) || poh.organisationunitid::text as owns " +
            "from programownershiphistory poh where poh.programid=0 and poh.trackedentityinstanceid is not null " +
            "and poh.organisationunitid is not null ) o2 " +
            "group by trackedentityinstanceid, owndate ) o " +
            "inner join trackedentityinstance tei on o.trackedentityinstanceid=tei.trackedentityinstanceid " +
            "and tei.deleted is false " +
            "inner join organisationunit ou on o.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on o.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on o.organisationunitid=ougs.organisationunitid " +
            "order by tei.uid, o.ownatstart",
            sqlMasked );

        List<Invocation> writerInvocations = getInvocations( writer );
        assertEquals( 6, writerInvocations.size() );

        assertEquals( "write", writerInvocations.get( 0 ).getMethod().getName() );
        assertEquals( "write", writerInvocations.get( 1 ).getMethod().getName() );
        assertEquals( "write", writerInvocations.get( 2 ).getMethod().getName() );
        assertEquals( "write", writerInvocations.get( 3 ).getMethod().getName() );
        assertEquals( "write", writerInvocations.get( 4 ).getMethod().getName() );
        assertEquals( "flush", writerInvocations.get( 5 ).getMethod().getName() );

        Map<String, Object> map0 = writerInvocations.get( 0 ).getArgument( 0 );
        Map<String, Object> map1 = writerInvocations.get( 1 ).getArgument( 0 );
        Map<String, Object> map2 = writerInvocations.get( 2 ).getArgument( 0 );
        Map<String, Object> map3 = writerInvocations.get( 3 ).getArgument( 0 );
        Map<String, Object> map4 = writerInvocations.get( 4 ).getArgument( 0 );

        assertEquals( Map.of( TEIUID, tei1, STARTDATE, start1, ENDDATE, end1, OU, ou1 ), map0 );
        assertEquals( Map.of( TEIUID, tei1, STARTDATE, start2, ENDDATE, end2, OU, ou2 ), map1 );
        assertEquals( Map.of( TEIUID, tei1, STARTDATE, start3, ENDDATE, end3, OU, ou1 ), map2 );
        assertEquals( Map.of( TEIUID, tei2, STARTDATE, start1, ENDDATE, end1, OU, ou1 ), map3 );
        assertEquals( Map.of( TEIUID, tei2, STARTDATE, start2, ENDDATE, end2, OU, ou2 ), map4 );
    }

    @Test
    void testGetFixedColumns()
    {
        List<AnalyticsTableColumn> expected = List.of(
            new AnalyticsTableColumn( quote( "teiuid" ), CHARACTER_11, "tei.uid" ),
            new AnalyticsTableColumn( quote( "startdate" ), DATE, "o.ownatstart" ),
            new AnalyticsTableColumn( quote( "enddate" ), DATE, "null" ),
            new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NOT_NULL, "ou.uid" ) );

        assertEquals( expected, target.getFixedColumns() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets a list of invocations of a mocked object
     */
    private List<Invocation> getInvocations( Object mock )
    {
        return new ArrayList<>( mockingDetails( mock ).getInvocations() );
    }
}
