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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.AggregationType.AVERAGE;
import static org.hisp.dhis.analytics.AggregationType.LAST;
import static org.hisp.dhis.analytics.AggregationType.MAX;
import static org.hisp.dhis.analytics.AggregationType.MIN;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.DataType.TEXT;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 */
@ExtendWith( MockitoExtension.class )
class AnalyticsManagerTest extends DhisConvenienceTest
{
    @Mock
    private QueryPlanner queryPlanner;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ExecutionPlanStore executionPlanStore;

    private JdbcAnalyticsManager analyticsManager;

    private static Stream<Arguments> data()
    {
        return Stream.of( arguments( "2017April", 115.75D ), arguments( "2017July", 77.5D ),
            arguments( "2017Oct", 39.25 ),
            arguments( "2017Nov", 26.5D ) );
    }

    @BeforeEach
    void before()
    {
        analyticsManager = new JdbcAnalyticsManager( queryPlanner, jdbcTemplate, executionPlanStore );
    }

    @ParameterizedTest
    @MethodSource( "data" )
    public void testWeightedAverage( String financialYear, Double weightedAverage )
    {
        AnalyticsAggregationType aggregationType = new AnalyticsAggregationType(
            AggregationType.SUM, AggregationType.AVERAGE, DataType.NUMERIC, true );

        Period y2017 = createPeriod( "2017" );
        Period y2018 = createPeriod( "2018" );
        Period finYear2017 = createPeriod( financialYear );

        Map<String, Object> dataValueMap = new HashMap<>();
        dataValueMap.put( BASE_UID + "-2018", 1.0 );
        dataValueMap.put( BASE_UID + "-2017", 154.0 );

        ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap = new ListMap<>();
        dataPeriodAggregationPeriodMap.putValue( y2017, finYear2017 );
        dataPeriodAggregationPeriodMap.putValue( y2018, finYear2017 );

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( getList( createDataElement( 'A' ), createDataElement( 'B' ) ) )
            .withPeriods( getList( y2017, y2018 ) )
            .withDataPeriodType( new YearlyPeriodType() )
            .withAggregationType( aggregationType )
            .build();

        analyticsManager.replaceDataPeriodsWithAggregationPeriods( dataValueMap, params,
            dataPeriodAggregationPeriodMap );

        assertEquals( 1, dataValueMap.size() );

        assertThat( dataValueMap.get( BASE_UID + "-" + finYear2017.getIsoDate() ), is( weightedAverage ) );
    }

    @Test
    void testGetValueClause()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder()
            .withPeriods( List.of( createPeriod( "202201" ) ) )
            .withAggregationType( new AnalyticsAggregationType( SUM, AVERAGE, NUMERIC, false ) )
            .withDataType( DataType.NUMERIC )
            .build();

        DataQueryParams paramsB = DataQueryParams.newBuilder()
            .withPeriods( List.of( createPeriod( "202201" ) ) )
            .withAggregationType( new AnalyticsAggregationType( MIN, MIN, NUMERIC, false ) )
            .withDataType( DataType.NUMERIC )
            .build();

        DataQueryParams paramsC = DataQueryParams.newBuilder()
            .withPeriods( List.of( createPeriod( "202201" ) ) )
            .withAggregationType( new AnalyticsAggregationType( NONE, NONE, NUMERIC, false ) )
            .withDataType( DataType.NUMERIC )
            .build();

        DataQueryParams paramsD = DataQueryParams.newBuilder()
            .withPeriods( List.of( createPeriod( "202201" ) ) )
            .withAggregationType( new AnalyticsAggregationType( LAST, LAST, TEXT, false ) )
            .withDataType( DataType.TEXT )
            .build();

        assertEquals( "sum(daysxvalue) / 31 as value ", analyticsManager.getValueClause( paramsA ) );
        assertEquals( "min(value) as value ", analyticsManager.getValueClause( paramsB ) );
        assertEquals( "value as value ", analyticsManager.getValueClause( paramsC ) );
        assertEquals( "value as value ", analyticsManager.getValueClause( paramsD ) );
    }

    @Test
    void testGetAggregateValueColumn()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder()
            .withPeriods( List.of( createPeriod( "202201" ) ) )
            .withAggregationType( new AnalyticsAggregationType( SUM, AVERAGE, NUMERIC, false ) )
            .build();

        DataQueryParams paramsB = DataQueryParams.newBuilder()
            .withPeriods( List.of( createPeriod( "202201" ) ) )
            .withAggregationType( new AnalyticsAggregationType( MAX, MAX, NUMERIC, false ) )
            .build();

        assertEquals( "sum(daysxvalue) / 31", analyticsManager.getAggregateValueColumn( paramsA ) );
        assertEquals( "max(value)", analyticsManager.getAggregateValueColumn( paramsB ) );
    }

    @Test
    void testReplaceDataPeriodsWithAggregationPeriods()
    {
        Period y2012 = createPeriod( "2012" );

        AnalyticsAggregationType aggregationType = new AnalyticsAggregationType(
            AggregationType.SUM, AggregationType.AVERAGE, DataType.NUMERIC, true );

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( getList( createDataElement( 'A' ), createDataElement( 'B' ) ) )
            .withPeriods( getList( y2012 ) )
            .withOrganisationUnits( getList( createOrganisationUnit( 'A' ) ) )
            .withDataPeriodType( new YearlyPeriodType() )
            .withAggregationType( aggregationType )
            .build();

        Map<String, Object> dataValueMap = new HashMap<>();
        dataValueMap.put( BASE_UID + "A-2012-" + BASE_UID + "A", 1d );
        dataValueMap.put( BASE_UID + "B-2012-" + BASE_UID + "A", 1d );

        ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap = new ListMap<>();
        dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q1" ) );
        dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q2" ) );
        dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q3" ) );
        dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q4" ) );

        analyticsManager.replaceDataPeriodsWithAggregationPeriods(
            dataValueMap, params, dataPeriodAggregationPeriodMap );

        assertEquals( 8, dataValueMap.size() );

        assertTrue( dataValueMap.keySet().contains( BASE_UID + "A-2012Q1-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "A-2012Q2-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "A-2012Q3-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "A-2012Q4-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "B-2012Q1-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "B-2012Q2-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "B-2012Q3-" + BASE_UID + "A" ) );
        assertTrue( dataValueMap.keySet().contains( BASE_UID + "B-2012Q4-" + BASE_UID + "A" ) );
    }
}
