package org.hisp.dhis.datastatistics;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
public class DataStatisticsStoreTest
    extends DhisSpringTest
{
    @Autowired
    private DataStatisticsStore dataStatisticsStore;

    private DataStatistics ds1;
    private DataStatistics ds2;
    private DataStatistics ds3;
    private DataStatistics ds4;
    private DataStatistics ds5;

    private int ds1Id;
    private int ds2Id;

    private Date date;

    @Override
    public void setUpTest() throws Exception
    {
        ds1 = new DataStatistics();
        ds2 = new DataStatistics( 2.0, 3.0, 4.0, 5.0, 6.0, 10.0, 8.0, 11.0, 12.0, 13.0, 14.0, 11.0, 15.0, 16.0, 17.0, 11.0, 10, 18 );
        ds3 = new DataStatistics( 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 11.0, 12.0, 13.0, 14.0, 15.0, 12.0, 16.0, 17.0, 18.0, 11.0, 10, 19 );
        ds4 = new DataStatistics( 2.0, 1.0, 6.0, 5.0, 4.0, 8.0, 10.0, 4.0, 4.0, 5.0, 9.0, 7.0, 14.0, 6.0, 4.0, 11.9, 3, 2 );
        ds5 = new DataStatistics( 6.0, 4.0, 3.0, 5.0, 7.0, 8.0, 10.0, 1.6, 5.5, 6.4, 8.3, 8.2, 16.0, 9.4, 9.6, 11.0, 5, 9 );

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
    public void saveSnapshotTest() throws Exception
    {
        dataStatisticsStore.save( ds1 );
        ds1Id = ds1.getId();
        dataStatisticsStore.save( ds2 );
        ds2Id = ds2.getId();

        assertTrue( ds1Id != 0 );
        assertTrue( ds2Id != 0 );
    }

    @Test
    public void getSnapshotsInIntervalGetInDAYTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );

        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.DAY, getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }

    @Test
    public void getSnapshotsInIntervalGetInDAY_DifferenDayesSavedTest() throws Exception
    {
        date = getDate( 2016, 3, 20 );
        ds2.setCreated( date );

        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );

        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.DAY, getDate( 2015, 3, 19 ), getDate( 2016, 3, 21 ) );
        assertEquals( 2, asList.size() );
    }

    @Test
    public void getSnapshotsInIntervalGetInDAY_GEDatesTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );

        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.DAY, getDate( 2017, 3, 21 ), getDate( 2017, 3, 22 ) );
        assertEquals( 0, asList.size() );
    }

    @Test
    public void getSnapshotsInIntervalGetInWEEKTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );

        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.WEEK, getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }

    @Test
    public void getSnapshotsInIntervalGetInMONTHTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );

        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.MONTH, getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }

    @Test
    public void getSnapshotsInIntervalGetInYEARTest()
    {
        dataStatisticsStore.save( ds2 );
        dataStatisticsStore.save( ds3 );
        dataStatisticsStore.save( ds4 );
        dataStatisticsStore.save( ds5 );

        List<AggregatedStatistics> asList = dataStatisticsStore.getSnapshotsInInterval( EventInterval.YEAR, getDate( 2015, 3, 21 ), getDate( 2016, 3, 21 ) );
        assertEquals( 1, asList.size() );
    }
}
