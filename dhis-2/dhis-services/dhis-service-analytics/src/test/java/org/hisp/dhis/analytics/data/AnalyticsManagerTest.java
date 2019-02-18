package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Lars Helge Overland
 */
@RunWith(Enclosed.class)
public class AnalyticsManagerTest
    extends DhisConvenienceTest
{

    @RunWith(Parameterized.class)
    public static class Parametrized {

        private AnalyticsManager analyticsManager;

        @Parameterized.Parameter
        public String financialYear;

        @Parameterized.Parameter(1)
        public Double weightedAverage;
        
        @Parameterized.Parameters
        public static Collection<Object[]> data()
        {
            return Arrays.asList( new Object[][] { { "2017April", 115.75D }, { "2017July", 77.5D }, { "2017Oct", 39.25 },
                    { "2017Nov", 26.5D } } );
        }

        @Before
        public void setUp()
        {
            analyticsManager = new JdbcAnalyticsManager();
        }

        @Test
        public void testWeightedAverage() {

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
                    .withAggregationType( aggregationType ).build();


            analyticsManager.replaceDataPeriodsWithAggregationPeriods( dataValueMap, params, dataPeriodAggregationPeriodMap );

            assertEquals( 1, dataValueMap.size() );

            assertThat( dataValueMap.get( BASE_UID + "-" + finYear2017.getIsoDate() ), is( weightedAverage ) );
        }
    }

    public static class SingleExecution {

        @Test
        public void testReplaceDataPeriodsWithAggregationPeriods()
        {
            AnalyticsManager analyticsManager = new JdbcAnalyticsManager();
            Period y2012 = createPeriod( "2012" );

            AnalyticsAggregationType aggregationType = new AnalyticsAggregationType(
                    AggregationType.SUM, AggregationType.AVERAGE, DataType.NUMERIC, true );

            DataQueryParams params = DataQueryParams.newBuilder()
                    .withDataElements( getList( createDataElement( 'A' ), createDataElement( 'B' ) ) )
                    .withPeriods( getList( y2012 ) )
                    .withOrganisationUnits( getList( createOrganisationUnit( 'A' ) ) )
                    .withDataPeriodType( new YearlyPeriodType() )
                    .withAggregationType( aggregationType ).build();

            Map<String, Object> dataValueMap = new HashMap<>();
            dataValueMap.put( BASE_UID + "A-2012-" + BASE_UID + "A", 1d );
            dataValueMap.put( BASE_UID + "B-2012-" + BASE_UID + "A", 1d );

            ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap = new ListMap<>();
            dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q1" ) );
            dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q2" ) );
            dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q3" ) );
            dataPeriodAggregationPeriodMap.putValue( y2012, createPeriod( "2012Q4" ) );

            analyticsManager.replaceDataPeriodsWithAggregationPeriods( dataValueMap, params, dataPeriodAggregationPeriodMap );

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

}
