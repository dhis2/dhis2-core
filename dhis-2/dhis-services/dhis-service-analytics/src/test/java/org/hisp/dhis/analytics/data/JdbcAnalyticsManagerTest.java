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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class JdbcAnalyticsManagerTest
{
    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private PartitionManager partitionManager;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SqlRowSet rowSet;

    @Mock
    private NestedIndicatorCyclicDependencyInspector nestedIndicatorCyclicDependencyInspector;

    @Captor
    private ArgumentCaptor<String> sql;

    private JdbcAnalyticsManager subject;

    @Mock
    private ExecutionPlanStore executionPlanStore;

    @BeforeEach
    public void setUp()
    {
        QueryPlanner queryPlanner = new DefaultQueryPlanner( partitionManager );

        mockRowSet();

        when( jdbcTemplate.queryForRowSet( sql.capture() ) ).thenReturn( rowSet );

        subject = new JdbcAnalyticsManager( queryPlanner, jdbcTemplate, executionPlanStore );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasLastAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.LAST );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        assertExpectedLastSql( "desc" );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasLastAvgOrgUnitAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.LAST_AVERAGE_ORG_UNIT );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        assertExpectedLastSql( "desc" );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasLastLastOrgUnitAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.LAST_LAST_ORG_UNIT );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        String subquery = "(select \"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\",\"daysno\"," +
            "\"value\",\"textvalue\",\"dx\",cast('201501' as text) as \"pe\",\"ou\"," +
            "row_number() over (partition by ax.\"dx\" order by peenddate desc, pestartdate desc) as pe_rank " +
            "from analytics as ax where ax.\"pestartdate\" >= '2005-01-31' and ax.\"peenddate\" <= '2015-01-31' " +
            "and (ax.\"value\" is not null or ax.\"textvalue\" is not null))";

        assertThat( sql.getValue(), containsString( subquery ) );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasLastInPeriodAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.LAST_IN_PERIOD );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        assertExpectedLastInPeriodSql( "desc" );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasLastInPeriodAvgOrgUnitAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.LAST_IN_PERIOD_AVERAGE_ORG_UNIT );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        assertExpectedLastInPeriodSql( "desc" );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasMaxSumOrgUnitAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.MAX_SUM_ORG_UNIT );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        assertExpectedMaxMinSumOrgUnitSql( "max" );
    }

    @Test
    void verifyQueryGeneratedWhenDataElementHasMinSumOrgUnitAggregationType()
    {
        DataQueryParams params = createParams( AggregationType.MIN_SUM_ORG_UNIT );

        subject.getAggregatedDataValues( params, AnalyticsTableType.DATA_VALUE, 20000 );

        assertExpectedMaxMinSumOrgUnitSql( "min" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void mockRowSet()
    {
        // Simulate no rows
        when( rowSet.next() ).thenReturn( false );
    }

    private DataQueryParams createParams( AggregationType aggregationType )
    {
        DataElement deA = createDataElement( 'A', ValueType.INTEGER, aggregationType );
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        Period peA = PeriodType.getPeriodFromIsoString( "201501" );

        return DataQueryParams.newBuilder()
            .withDataType( DataType.NUMERIC )
            .withTableName( "analytics" )
            .withAggregationType( AnalyticsAggregationType.fromAggregationType( aggregationType ) )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, getList( deA ) ) )
            .addFilter( new BaseDimensionalObject( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList( ouA ) ) )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, getList( peA ) ) ).build();
    }

    private void assertExpectedLastSql( String sortOrder )
    {
        String lastAggregationTypeSql = "(select \"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\","
            + "\"daysno\",\"value\",\"textvalue\",\"dx\",cast('201501' as text) as \"pe\",\"ou\","
            + "row_number() over (partition by ax.\"dx\",ax.\"ou\",ax.\"co\",ax.\"ao\" order by peenddate " +
            sortOrder + ", pestartdate " + sortOrder + ") as pe_rank "
            + "from analytics as ax where ax.\"pestartdate\" >= '2005-01-31' and ax.\"peenddate\" <= '2015-01-31' "
            + "and (ax.\"value\" is not null or ax.\"textvalue\" is not null))";

        assertThat( sql.getValue(), containsString( lastAggregationTypeSql ) );
    }

    private void assertExpectedLastInPeriodSql( String sortOrder )
    {
        String lastAggregationTypeSql = "(select \"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\","
            + "\"daysno\",\"value\",\"textvalue\",\"dx\",cast('201501' as text) as \"pe\",\"ou\","
            + "row_number() over (partition by ax.\"dx\",ax.\"ou\",ax.\"co\",ax.\"ao\" order by peenddate " +
            sortOrder + ", pestartdate " + sortOrder + ") as pe_rank "
            + "from analytics as ax where ax.\"pestartdate\" >= '2015-01-01' and ax.\"peenddate\" <= '2015-01-31' "
            + "and (ax.\"value\" is not null or ax.\"textvalue\" is not null))";

        assertThat( sql.getValue(), containsString( lastAggregationTypeSql ) );
    }

    private void assertExpectedMaxMinSumOrgUnitSql( String maxOrMin )
    {
        String maxMinTypeSql = "(select ax.\"ou\",ax.\"dx\",ax.\"pe\","
            + maxOrMin + "(\"daysxvalue\") as \"daysxvalue\"," + maxOrMin + "(\"daysno\") as \"daysno\","
            + maxOrMin + "(\"value\") as \"value\"," + maxOrMin + "(\"textvalue\") as \"textvalue\" "
            + "from analytics as ax "
            + "where ax.\"dx\" in ('deabcdefghA') and ax.\"pe\" in ('201501') and ( ax.\"ou\" in ('ouabcdefghA') ) "
            + "group by ax.\"ou\",ax.\"dx\",ax.\"pe\")";

        assertThat( sql.getValue(), containsString( maxMinTypeSql ) );
    }
}
