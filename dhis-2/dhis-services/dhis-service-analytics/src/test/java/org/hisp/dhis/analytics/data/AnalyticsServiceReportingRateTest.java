/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsServiceReportingRateTest extends AnalyticsServiceBaseTest
{

    @Test
    public void verifyReportingRatesValueWhenPeriodIsFilter()
    {
        int timeUnit = 10;
        double expectedReports = 100D;

        DataSet dataSetA = createDataSet( 'A' );
        ReportingRate reportingRateA = new ReportingRate( dataSetA );
        reportingRateA.setMetric( ReportingRateMetric.REPORTING_RATE );
        ReportingRate reportingRateB = new ReportingRate( dataSetA );
        reportingRateB.setMetric( ReportingRateMetric.ACTUAL_REPORTS );
        ReportingRate reportingRateC = new ReportingRate( dataSetA );
        reportingRateC.setMetric( ReportingRateMetric.EXPECTED_REPORTS );

        List<DimensionalItemObject> periods = new ArrayList<>();

        Stream.iterate( 1, i -> i + 1 ).limit( timeUnit ).forEach(
            x -> periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, x, 1, 0, 0 ).toDate() ) ) );

        OrganisationUnit ou = new OrganisationUnit( "aaaa" );
        
        DataQueryParams params = DataQueryParams.newBuilder().withOrganisationUnit( ou )
            // DATA ELEMENTS
            .withDataElements( newArrayList( reportingRateA, reportingRateB, reportingRateC ) ).withIgnoreLimit( true )
            // FILTERS (OU)
            .withFilters(
                Collections.singletonList( new BaseDimensionalObject( "pe", DimensionType.PERIOD, periods ) ) )
            .build();

        initMock( params );

        Map<String, Object> actualReports = new HashMap<>();
        actualReports.put( dataSetA.getUid() + "-" + ou.getUid(), 500D );

        when( analyticsManager.getAggregatedDataValues( any( DataQueryParams.class ),
            eq( AnalyticsTableType.COMPLETENESS ), eq( 0 ) ) )
                .thenReturn( CompletableFuture.completedFuture( actualReports ) );

        Map<String, Object> reportingRate = new HashMap<>();
        reportingRate.put( dataSetA.getUid() + "-" + ou.getUid(), expectedReports );

        when( analyticsManager.getAggregatedDataValues( any( DataQueryParams.class ),
            eq( AnalyticsTableType.COMPLETENESS_TARGET ), eq( 0 ) ) )
                .thenReturn( CompletableFuture.completedFuture( reportingRate ) );

        Grid grid = target.getAggregatedDataValues( params );

        assertEquals( expectedReports * timeUnit,
            getValueFromGrid( grid.getRows(), makeKey( dataSetA, ReportingRateMetric.EXPECTED_REPORTS ) ), 0 );
        assertEquals( 50D, getValueFromGrid( grid.getRows(), makeKey( dataSetA, ReportingRateMetric.REPORTING_RATE ) ), 0 );
        assertEquals( 500D, getValueFromGrid( grid.getRows(), makeKey( dataSetA, ReportingRateMetric.ACTUAL_REPORTS ) ), 0 );
    }

    private double getValueFromGrid(List<List<Object>> rows, String key )
    {
        for ( List<Object> row : rows )
        {
            if ( row.get( 0 ).equals( key ) )
            {
                return (Double) row.get( 2 );
            }
        }
        return 0;
    }
    
    private String makeKey( DataSet dataSet, ReportingRateMetric reportingRateMetric )
    {
        return dataSet.getUid() + "." + reportingRateMetric.name();
    }
}
