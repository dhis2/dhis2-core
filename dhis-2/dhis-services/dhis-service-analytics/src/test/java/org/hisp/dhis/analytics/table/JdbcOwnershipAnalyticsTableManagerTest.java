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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AnalyticsExportSettings;
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
import org.springframework.jdbc.core.RowCallbackHandler;

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
    private JdbcOwnershipWriter writer;

    @Mock
    private AnalyticsExportSettings analyticsExportSettings;

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
            partitionManager, databaseInfo, jdbcTemplate, jdbcConfiguration, analyticsExportSettings );

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

        // Mock the jdbcTemplate callback handler to return the mocked ResultSet
        // object:

        ResultSet resultSet1 = mock( ResultSet.class );
        ResultSet resultSet2 = mock( ResultSet.class );
        ResultSet resultSet3 = mock( ResultSet.class );

        doAnswer( invocation -> {
            RowCallbackHandler callbackHandler = invocation.getArgument( 1 );
            callbackHandler.processRow( resultSet1 );
            callbackHandler.processRow( resultSet2 );
            callbackHandler.processRow( resultSet3 );
            return null;
        } ).when( jdbcTemplate ).query( anyString(), any( RowCallbackHandler.class ) );

        // TEI uid:
        when( resultSet1.getObject( 1 ) ).thenReturn( tei1 );
        when( resultSet2.getObject( 1 ) ).thenReturn( tei2 );
        when( resultSet3.getObject( 1 ) ).thenReturn( tei2 );

        // Start date:
        when( resultSet1.getObject( 2 ) ).thenReturn( start1 );
        when( resultSet2.getObject( 2 ) ).thenReturn( start2 );
        when( resultSet3.getObject( 2 ) ).thenReturn( start3 );

        // End date (always null):
        when( resultSet1.getObject( 3 ) ).thenReturn( end1 );
        when( resultSet2.getObject( 3 ) ).thenReturn( end2 );
        when( resultSet3.getObject( 3 ) ).thenReturn( end3 );

        // OrgUnit:
        when( resultSet1.getObject( 4 ) ).thenReturn( ou1 );
        when( resultSet2.getObject( 4 ) ).thenReturn( ou2 );
        when( resultSet3.getObject( 4 ) ).thenReturn( ou2 );

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
        assertEquals( "query", jdbcInvocations.get( 0 ).getMethod().getName() );

        String sql = jdbcInvocations.get( 0 ).getArgument( 0 );
        String sqlMasked = sql.replaceAll( "lastupdated <= '\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}'",
            "lastupdated <= 'yyyy-mm-ddThh:mm:ss'" );
        assertEquals( "select tei.uid,a.startdate,a.enddate,ou.uid from (" +
            "select h.trackedentityinstanceid, '1001-01-01' as startdate, h.enddate as enddate, h.organisationunitid " +
            "from programownershiphistory h " +
            "where h.programid=0 " +
            "union " +
            "select o.trackedentityinstanceid, '2002-02-02' as startdate, null as enddate, o.organisationunitid " +
            "from trackedentityprogramowner o " +
            "where o.programid=0 " +
            "and exists (select programid from programownershiphistory p " +
            "where o.trackedentityinstanceid = p.trackedentityinstanceid " +
            "and p.programid=0)" +
            ") a " +
            "inner join trackedentityinstance tei on a.trackedentityinstanceid = tei.trackedentityinstanceid " +
            "inner join organisationunit ou on a.organisationunitid = ou.organisationunitid " +
            "left join _orgunitstructure ous on a.organisationunitid = ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on a.organisationunitid = ougs.organisationunitid " +
            "order by tei.uid, a.startdate, a.enddate",
            sqlMasked );

        List<Invocation> writerInvocations = getInvocations( writer );
        assertEquals( 3, writerInvocations.size() );

        assertEquals( "write", writerInvocations.get( 0 ).getMethod().getName() );
        assertEquals( "write", writerInvocations.get( 1 ).getMethod().getName() );
        assertEquals( "write", writerInvocations.get( 2 ).getMethod().getName() );

        Map<String, Object> map0 = writerInvocations.get( 0 ).getArgument( 0 );
        Map<String, Object> map1 = writerInvocations.get( 1 ).getArgument( 0 );
        Map<String, Object> map2 = writerInvocations.get( 2 ).getArgument( 0 );

        assertEquals( Map.of( TEIUID, tei1, STARTDATE, start1, ENDDATE, end1, OU, ou1 ), map0 );
        assertEquals( Map.of( TEIUID, tei2, STARTDATE, start2, ENDDATE, end2, OU, ou2 ), map1 );
        assertEquals( Map.of( TEIUID, tei2, STARTDATE, start3, ENDDATE, end3, OU, ou2 ), map2 );
    }

    @Test
    void testGetFixedColumns()
    {
        List<AnalyticsTableColumn> expected = List.of(
            new AnalyticsTableColumn( quote( "teiuid" ), CHARACTER_11, "tei.uid" ),
            new AnalyticsTableColumn( quote( "startdate" ), DATE, "a.startdate" ),
            new AnalyticsTableColumn( quote( "enddate" ), DATE, "a.enddate" ),
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
