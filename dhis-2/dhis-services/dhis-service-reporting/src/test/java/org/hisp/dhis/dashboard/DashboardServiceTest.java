package org.hisp.dhis.dashboard;

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
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class DashboardServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ChartService chartService;

    @Autowired
    private DocumentService documentService;

    private Dashboard dA;
    private Dashboard dB;

    private DashboardItem diA;
    private DashboardItem diB;
    private DashboardItem diC;
    private DashboardItem diD;

    private Chart chartA;
    private Chart chartB;

    @Override
    public void setUpTest()
    {
        chartA = createChart( 'A' );
        chartB = createChart( 'B' );

        chartService.addChart( chartA );
        chartService.addChart( chartB );

        Document docA = new Document( "A", "url", false, null );
        Document docB = new Document( "B", "url", false, null );
        Document docC = new Document( "C", "url", false, null );
        Document docD = new Document( "D", "url", false, null );

        documentService.saveDocument( docA );
        documentService.saveDocument( docB );
        documentService.saveDocument( docC );
        documentService.saveDocument( docD );

        diA = new DashboardItem();
        diA.setAutoFields();
        diA.setChart( chartA );

        diB = new DashboardItem();
        diB.setAutoFields();
        diB.setChart( chartB );

        diC = new DashboardItem();
        diC.setAutoFields();
        diC.getResources().add( docA );
        diC.getResources().add( docB );

        diD = new DashboardItem();
        diD.setAutoFields();
        diD.getResources().add( docC );
        diD.getResources().add( docD );

        dA = new Dashboard( "A" );
        dA.setAutoFields();
        dA.getItems().add( diA );
        dA.getItems().add( diB );
        dA.getItems().add( diC );

        dB = new Dashboard( "B" );
        dB.setAutoFields();
        dB.getItems().add( diD );
    }

    @Test
    public void testAddGet()
    {
        int dAId = dashboardService.saveDashboard( dA );
        int dBId = dashboardService.saveDashboard( dB );

        assertEquals( dA, dashboardService.getDashboard( dAId ) );
        assertEquals( dB, dashboardService.getDashboard( dBId ) );

        assertEquals( 3, dashboardService.getDashboard( dAId ).getItems().size() );
        assertEquals( 1, dashboardService.getDashboard( dBId ).getItems().size() );
    }

    @Test
    public void testUpdate()
    {
        int dAId = dashboardService.saveDashboard( dA );

        assertEquals( "A", dashboardService.getDashboard( dAId ).getName() );

        dA.setName( "B" );

        dashboardService.updateDashboard( dA );

        assertEquals( "B", dashboardService.getDashboard( dAId ).getName() );
    }

    @Test
    public void testDelete()
    {
        int dAId = dashboardService.saveDashboard( dA );
        int dBId = dashboardService.saveDashboard( dB );

        assertNotNull( dashboardService.getDashboard( dAId ) );
        assertNotNull( dashboardService.getDashboard( dBId ) );

        dashboardService.deleteDashboard( dA );

        assertNull( dashboardService.getDashboard( dAId ) );
        assertNotNull( dashboardService.getDashboard( dBId ) );

        dashboardService.deleteDashboard( dB );

        assertNull( dashboardService.getDashboard( dAId ) );
        assertNull( dashboardService.getDashboard( dBId ) );
    }

    @Test
    public void testAddItemContent()
    {
        dashboardService.saveDashboard( dA );
        dashboardService.saveDashboard( dB );

        DashboardItem itemA = dashboardService.addItemContent( dA.getUid(), DashboardItemType.CHART, chartA.getUid() );

        assertNotNull( itemA );
        assertNotNull( itemA.getUid() );
    }
}
