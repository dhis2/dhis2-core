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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
public class DataStatisticsEventStoreTest
    extends DhisSpringTest
{
    @Autowired
    private DataStatisticsEventStore dataStatisticsEventStore;

    private DataStatisticsEvent dse1;
    private DataStatisticsEvent dse2;
    private DataStatisticsEvent dse3;
    private DataStatisticsEvent dse4;

    private int dse1Id;
    private int dse2Id;

    private Date start;
    private Date end;

    @Override
    public void setUpTest()
    {
        end = getDate( 2016, 3, 21 );
        start = getDate( 2016, 3, 19 );
        Date endDate = getDate( 2016, 3, 20 );
        Date testDate = getDate( 2016, 3, 16 );

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.REPORT_TABLE_VIEW, endDate, "Testuser" );
        dse2 = new DataStatisticsEvent( DataStatisticsEventType.EVENT_CHART_VIEW, endDate, "TestUser" );
        dse3 = new DataStatisticsEvent( DataStatisticsEventType.CHART_VIEW, testDate, "Testuser" );
        dse4 = new DataStatisticsEvent( DataStatisticsEventType.DASHBOARD_VIEW, endDate, "TestUser" );
    }

    @Test
    public void addDataStatisticsEventTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dse1Id = dse1.getId();
        dataStatisticsEventStore.save( dse2 );
        dse2Id = dse2.getId();

        assertTrue( dse1Id != 0 );
        assertTrue( dse2Id != 0 );
    }

    @Test
    public void getDataStatisticsEventCountTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );

        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore.getDataStatisticsEventCount( start, end );

        //Test for 3 objects because TOTAL_VIEWS is always present
        assertTrue( dsList.size() == 3 );
    }

    @Test
    public void getDataStatisticsEventCountCorrectContentTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );

        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore.getDataStatisticsEventCount( start, end );
        double expected = 1.0;
        double firstActual = dsList.get( DataStatisticsEventType.REPORT_TABLE_VIEW );
        double secondActual = dsList.get( DataStatisticsEventType.DASHBOARD_VIEW );

        assertEquals( expected, firstActual, 0.0 );
        assertEquals( expected, secondActual, 0.0 );
    }

    @Test
    public void getDataStatisticsEventCountCorrectDatesTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );
        dataStatisticsEventStore.save( dse2 );

        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore.getDataStatisticsEventCount( start, end );
        //Test for 4 objects, because TOTAL_VIEW is always present
        assertTrue( dsList.size() == 4 );
    }

    @Test
    public void getDataStatisticsEventCountWrongDatesTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );
        dataStatisticsEventStore.save( dse3 );

        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore.getDataStatisticsEventCount( start, end );
        //Test for 3 objects because TOTAL_VIEW is always present
        assertTrue( dsList.size() == 3 );
    }
}
