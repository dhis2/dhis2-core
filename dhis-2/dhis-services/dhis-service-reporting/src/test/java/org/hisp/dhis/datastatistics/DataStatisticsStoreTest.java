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
package org.hisp.dhis.datastatistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
class DataStatisticsStoreTest extends DhisSpringTest
{
    @Autowired
    private DataStatisticsStore dataStatisticsStore;

    private DataStatistics ds1;

    private DataStatistics ds2;

    private DataStatistics ds3;

    private DataStatistics ds4;

    private DataStatistics ds5;

    private long ds1Id;

    private long ds2Id;

    private Date date;

    @Override
    public void setUpTest()
    {
        ds1 = new DataStatistics();
        ds2 = new DataStatistics( 1.0, 3.0, 4.0, 5.0, 3.0, 6.0, 17.0, 10.0, 8.0, 11.0, 14.0, 11.0, 15.0, 20.0, 16.0,
            17.0, 11.0, 1, 18 );
        ds3 = new DataStatistics( 1.0, 4.0, 5.0, 6.0, 4.0, 7.0, 17.0, 8.0, 11.0, 12.0, 15.0, 12.0, 16.0, 21.0, 17.0,
            18.0, 11.0, 2, 19 );
        ds4 = new DataStatistics( 1.0, 1.0, 6.0, 5.0, 5.0, 4.0, 16.0, 8.0, 10.0, 4.0, 9.0, 7.0, 14.0, 22.0, 6.0, 4.0,
            11.9, 3, 2 );
        ds5 = new DataStatistics( 3.0, 4.0, 3.0, 5.0, 6.0, 7.0, 16.0, 8.0, 10.0, 1.6, 8.0, 8.2, 16.0, 23.0, 9.4, 9.6,
            11.0, 2, 9 );
        ds1Id = 0;
        ds2Id = 0;
        date = getDate( 2016, 3, 21 );
        ds1.setCreated( date );
        ds2.setCreated( date );
        ds3.setCreated( date );
        ds4.setCreated( date );
        ds5.setCreated( date );
    }

    @Test
    void saveSnapshotTest()
    {
        dataStatisticsStore.save( ds1 );
        ds1Id = ds1.getId();
        dataStatisticsStore.save( ds2 );
        ds2Id = ds2.getId();
        assertTrue( ds1Id != 0 );
        assertTrue( ds2Id != 0 );
    }

    @Test
    void getSnapshotsInIntervalGetInDAYTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );
        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.DAY,
            getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }

    @Test
    void getSnapshotsInIntervalGetInDAY_DifferenDayesSavedTest()
    {
        date = getDate( 2016, 3, 20 );
        ds2.setCreated( date );
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );
        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.DAY,
            getDate( 2015, 3, 19 ), getDate( 2016, 3, 21 ) );
        assertEquals( 2, asList.size() );
    }

    @Test
    void getSnapshotsInIntervalGetInDAY_GEDatesTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );
        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.DAY,
            getDate( 2017, 3, 21 ), getDate( 2017, 3, 22 ) );
        assertEquals( 0, asList.size() );
    }

    @Test
    void getSnapshotsInIntervalGetInWEEKTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );
        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.WEEK,
            getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }

    @Test
    void getSnapshotsInIntervalGetInMONTHTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );
        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.MONTH,
            getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }

    @Test
    void getSnapshotsInIntervalGetInYEARTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );
        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.YEAR,
            getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
        AggregatedStatistics as = asList.get( 0 );
        assertEquals( 6, as.getMapViews() );
        assertEquals( 12, as.getVisualizationViews() );
        assertEquals( 18, as.getEventReportViews() );
        assertEquals( 21, as.getEventChartViews() );
        assertEquals( 18, as.getEventVisualizationViews() );
        assertEquals( 24, as.getDashboardViews() );
        assertEquals( 66, as.getPassiveDashboardViews() );
        assertEquals( 34, as.getDataSetReportViews() );
        assertEquals( 39, as.getTotalViews() );
        assertEquals( 13, as.getAverageViews() );
        assertEquals( 2, as.getAverageMapViews() );
        assertEquals( 4, as.getAverageVisualizationViews() );
        assertEquals( 6, as.getAverageEventReportViews() );
        assertEquals( 7, as.getAverageEventChartViews() );
        assertEquals( 6, as.getAverageEventVisualizationViews() );
        assertEquals( 8, as.getAverageDashboardViews() );
        assertEquals( 22, as.getAveragePassiveDashboardViews() );
        assertEquals( 29, as.getSavedMaps() );
        assertEquals( 46, as.getSavedVisualizations() );
        assertEquals( 38, as.getSavedEventReports() );
        assertEquals( 61, as.getSavedEventCharts() );
        assertEquals( 86, as.getSavedEventVisualizations() );
        assertEquals( 48, as.getSavedDashboards() );
        assertEquals( 49, as.getSavedIndicators() );
        assertEquals( 45, as.getSavedDataValues() );
        assertEquals( 3, as.getActiveUsers() );
        assertEquals( 19, as.getUsers() );
    }
}
